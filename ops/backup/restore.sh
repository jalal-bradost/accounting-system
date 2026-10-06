#!/usr/bin/env bash
# Restore the application database to any point in time from dumps + binary logs.
#
#   restore.sh list                                  what can be restored
#   restore.sh test [--at TIME] [--keep]             rebuild in a throwaway MySQL container and check it
#   restore.sh into-db NAME [--at TIME] [--replace]  rebuild into a separate database on the live server
#   restore.sh production --at TIME --yes-i-am-sure  replace the live database (keeps the old one)
#   restore.sh verify                                weekly self-test (test, at "now", strict checks)
#
# TIME is anything `date -d` understands, in the server's time zone unless an offset is given:
#   --at "2026-10-05 14:32"   --at "2026-10-05 14:32 +03:00"   --at "30 minutes ago"
# Without --at, the restore goes to "now" (latest dump + every binlog up to the present).

source "$(dirname "$(readlink -f "$0")")/lib.sh"
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

usage() { sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'; exit 2; }

MODE="${1:-}"; [ -n "$MODE" ] || usage; shift
AT=""; KEEP=0; REPLACE=0; CONFIRM=0; TARGET_NAME=""
if [ "$MODE" = into-db ]; then TARGET_NAME="${1:-}"; [ -n "$TARGET_NAME" ] || usage; shift; fi
while [ $# -gt 0 ]; do
    case "$1" in
        --at) AT="${2:-}"; shift 2 ;;
        --keep) KEEP=1; shift ;;
        --replace) REPLACE=1; shift ;;
        --yes-i-am-sure) CONFIRM=1; shift ;;
        *) usage ;;
    esac
done

case "$MODE" in
    list | test | into-db | production) begin_job restore ;;
    verify) begin_job verify ;;
    *) usage ;;
esac

TMP=""; TEST_CONTAINER=""; BACKEND_STOPPED=0
cleanup() {
    [ -n "$TMP" ] && rm -rf "$TMP"
    if [ "$BACKEND_STOPPED" -eq 1 ]; then
        log "restarting backend after an interrupted restore"
        docker compose -f "$COMPOSE_DIR/docker-compose.yml" start backend > /dev/null 2>&1 || true
    fi
    if [ -n "$TEST_CONTAINER" ] && [ "$KEEP" -eq 0 ]; then
        docker rm -f -v "$TEST_CONTAINER" > /dev/null 2>&1 || true
    fi
}
trap cleanup EXIT

# ---------------------------------------------------------------- catalogue

# All dumps, one line each: "<epoch> <path>", oldest first, de-duplicated across hard links.
all_dumps() {
    find "$BACKUP_ROOT/db" -mindepth 2 -maxdepth 2 -type f -name "${APP_NAME}_*.sql.gz" 2>/dev/null \
        | while read -r f; do echo "$(stamp_to_epoch "$(stamp_of "$f")") $(stat -c %i "$f") $f"; done \
        | sort -n | awk '!seen[$2]++ {print $1, $3}'
}

dump_position() {  # prints "<file> <pos>"
    zcat "$1" | head -n 80 | grep -Eo "SOURCE_LOG_FILE='[^']+', SOURCE_LOG_POS=[0-9]+" | head -1 \
        | sed -E "s/SOURCE_LOG_FILE='([^']+)', SOURCE_LOG_POS=([0-9]+)/\1 \2/" || true
}

binlog_number() { echo "${1##*.}" | sed 's/^0*//'; }

list() {
    echo "Dumps (UTC):"
    all_dumps | while read -r epoch path; do
        printf '  %s  %-12s %8s  %s\n' "$(date -u -d "@$epoch" '+%F %H:%M')" \
            "$(basename "$(dirname "$path")")" "$(numfmt --to=iec "$(file_size "$path")")" "$(basename "$path")"
    done
    local oldest newest
    oldest=$(find "$BACKUP_ROOT/binlog" -name 'binlog.*.gz' -printf '%f\n' 2>/dev/null | sort | head -1)
    newest=$(find "$BACKUP_ROOT/binlog" -name 'binlog.*.gz' -printf '%f\n' 2>/dev/null | sort | tail -1)
    echo "Binlog copies: ${oldest:-none} .. ${newest:-none}"
    echo "Any minute from the oldest dump whose binlogs are still kept up to now can be restored."
}

# ---------------------------------------------------------------- planning

resolve_at() {
    if [ -z "$AT" ]; then
        AT_EPOCH=$(date +%s)
        AT_IS_NOW=1
    else
        AT_EPOCH=$(date -d "$AT" +%s 2>/dev/null) || die "cannot understand --at '$AT'"
        AT_IS_NOW=0
        [ "$AT_EPOCH" -le "$(date +%s)" ] || die "--at is in the future"
    fi
    AT_UTC=$(date -u -d "@$AT_EPOCH" '+%F %T')
    log "restore point: $AT_UTC UTC ($(TZ=Asia/Baghdad date -d "@$AT_EPOCH" '+%F %T') Baghdad)"
}

choose_dump() {
    DUMP=$(all_dumps | awk -v at="$AT_EPOCH" '$1 <= at {f=$2} END {print f}')
    [ -n "$DUMP" ] || die "no dump at or before $AT_UTC UTC"
    verify_checksum "$DUMP" || die "checksum mismatch for $DUMP"
    read -r START_FILE START_POS < <(dump_position "$DUMP")
    [ -n "${START_FILE:-}" ] || die "$DUMP has no binlog position"
    log "base dump: $(basename "$DUMP") (binlogs from $START_FILE:$START_POS)"
}

# Gather binlogs START_FILE.. into $TMP/binlog, from backup copies or (for the newest ones) the live data dir.
collect_binlogs() {
    BINLOG_FILES=()
    mkdir -p "$TMP/binlog"
    # Close the active binlog so every event up to now is in a finished file.
    if docker inspect -f '{{.State.Running}}' "$MYSQL_CONTAINER" 2>/dev/null | grep -q true; then
        mysql_live -N -e 'FLUSH BINARY LOGS'
    fi
    local start last n name
    start=$(binlog_number "$START_FILE")
    last=$( { find "$BACKUP_ROOT/binlog" -name 'binlog.*.gz' -printf '%f\n' 2>/dev/null | sed 's/\.gz$//';
              find "$MYSQL_DATA_DIR" -maxdepth 1 -name 'binlog.[0-9]*' -printf '%f\n' 2>/dev/null; } \
            | sed 's/^binlog\.0*//' | sort -n | tail -1)
    [ -n "$last" ] || last="$start"
    for ((n = start; n <= last; n++)); do
        name=$(printf 'binlog.%06d' "$n")
        if [ -f "$BACKUP_ROOT/binlog/$name.gz" ]; then
            verify_checksum "$BACKUP_ROOT/binlog/$name.gz" || die "checksum mismatch for $name.gz"
            zcat "$BACKUP_ROOT/binlog/$name.gz" > "$TMP/binlog/$name"
        elif [ -f "$MYSQL_DATA_DIR/$name" ]; then
            cp "$MYSQL_DATA_DIR/$name" "$TMP/binlog/$name"
        else
            die "binlog $name is missing; cannot replay past it"
        fi
        BINLOG_FILES+=("/b/$name")
    done
    log "binlogs to replay: ${#BINLOG_FILES[@]} (binlog.$(printf '%06d' "$start") .. binlog.$(printf '%06d' "$last"))"
}

# SQL for every change to DB_NAME between the dump and AT, rewritten for the target database.
binlog_sql() {
    local target_db="$1"
    local args=(--start-position="$START_POS" --stop-datetime="$AT_UTC" --database="$target_db")
    [ "$target_db" = "$DB_NAME" ] || args+=(--rewrite-db="$DB_NAME->$target_db")
    docker run --rm --user 0 -e TZ=UTC -v "$TMP/binlog:/b:ro" --entrypoint mysqlbinlog "$MYSQLBINLOG_IMAGE" \
        "${args[@]}" "${BINLOG_FILES[@]}"
}

# ---------------------------------------------------------------- loading

# $1 = command prefix that runs the mysql client against the target server (reads SQL on stdin)
load_into() {
    local runner="$1" db="$2"
    log "loading dump into $db"
    { echo "SET SESSION sql_log_bin=0;"; zcat "$DUMP"; } | $runner "$db"
    if [ "${#BINLOG_FILES[@]}" -gt 0 ]; then
        log "replaying binlogs into $db up to $AT_UTC UTC"
        { echo "SET SESSION sql_log_bin=0;"; binlog_sql "$db"; } | $runner "$db"
    fi
}

TEST_PW=""
test_mysql() { docker exec -i -e MYSQL_PWD="$TEST_PW" "$TEST_CONTAINER" mysql -uroot "$@"; }

start_test_server() {
    TEST_PW=$(head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n')
    TEST_CONTAINER="${APP_NAME}-restore-$(date +%s)"
    log "starting throwaway MySQL container $TEST_CONTAINER"
    docker run -d --name "$TEST_CONTAINER" -e MYSQL_ROOT_PASSWORD="$TEST_PW" "$MYSQL_IMAGE" \
        --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --skip-log-bin > /dev/null
    local i
    for i in $(seq 1 90); do
        if docker logs "$TEST_CONTAINER" 2>&1 | grep -q 'ready for connections.*port: 3306' \
            && test_mysql -N -e 'SELECT 1' > /dev/null 2>&1; then
            test_mysql -e "CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            return 0
        fi
        sleep 2
    done
    die "throwaway MySQL did not start"
}

# ---------------------------------------------------------------- checks

# $1 runner for the restored server, $2 restored db name, $3 strict(1)/report(0)
run_checks() {
    local runner="$1" db="$2" strict="$3" failures=0
    q() { $runner -N "$db" -e "$1"; }
    live() { mysql_live -N "$DB_NAME" -e "$1"; }

    local tables_r tables_l
    tables_r=$(q "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()")
    tables_l=$(live "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()")
    log "check tables: restored $tables_r, live $tables_l"
    [ "$tables_r" -gt 0 ] || { log "FAIL: no tables restored"; failures=$((failures + 1)); }
    if [ "$strict" = 1 ] && [ "$tables_r" != "$tables_l" ]; then
        log "FAIL: table count differs"; failures=$((failures + 1))
    fi

    local ledger
    ledger=$(q "SELECT COALESCE(ROUND(SUM(debit) - SUM(credit), 4), 0) FROM journal_items")
    log "check ledger debit-credit: $ledger"
    [ "$(echo "$ledger" | tr -d '0.-')" = "" ] || { log "FAIL: ledger does not balance"; failures=$((failures + 1)); }

    local migrations_r migrations_l
    migrations_r=$(q "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1")
    migrations_l=$(live "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1")
    log "check migrations: restored $migrations_r, live $migrations_l"
    if [ "$strict" = 1 ] && [ "$migrations_r" != "$migrations_l" ]; then
        log "FAIL: migration history differs"; failures=$((failures + 1))
    fi

    local t r l
    for t in journal_entries journal_items acc_customer_invoice acc_customer_payment sal_sales_order \
             inv_stock_move inv_product contacts_partner platform_app_user; do
        r=$(q "SELECT COUNT(*) FROM $t" 2>/dev/null || echo "?")
        l=$(live "SELECT COUNT(*) FROM $t" 2>/dev/null || echo "?")
        log "rows $t: restored $r, live $l"
        if [ "$strict" = 1 ] && [ "$r" != "?" ] && [ "$l" != "?" ]; then
            # Live keeps changing while the test runs; allow a small window.
            local diff=$(( r > l ? r - l : l - r ))
            if [ "$diff" -gt $(( l / 100 + 20 )) ]; then
                log "FAIL: $t differs by $diff rows"; failures=$((failures + 1))
            fi
        fi
    done
    CHECK_FAILURES=$failures
}

# ---------------------------------------------------------------- modes

restore_test() {
    local strict="$1"
    require_disk_space
    resolve_at; choose_dump
    TMP=$(mktemp -d "$BACKUP_ROOT/.restore-XXXXXX")
    collect_binlogs
    start_test_server
    load_into test_mysql "$DB_NAME"
    run_checks test_mysql "$DB_NAME" "$strict"
    if [ "$KEEP" -eq 1 ]; then
        log "kept container $TEST_CONTAINER. Connect: docker exec -it -e MYSQL_PWD=$TEST_PW $TEST_CONTAINER mysql -uroot $DB_NAME"
        log "remove it when done: docker rm -f -v $TEST_CONTAINER"
    fi
    [ "$CHECK_FAILURES" -eq 0 ] || die "$CHECK_FAILURES check(s) failed"
    write_status ok "restored $(basename "$DUMP") + ${#BINLOG_FILES[@]} binlog(s) to $AT_UTC UTC" \
        "$(jq -n --arg dump "$DUMP" --arg at "$AT_UTC" --argjson binlogs "${#BINLOG_FILES[@]}" \
            '{dump:$dump, restoredTo:$at, binlogs:$binlogs}')"
}

restore_into_db() {
    local name="$1"
    [[ "$name" =~ ^[a-z0-9_]{1,48}$ ]] || die "database name must be lowercase letters, digits, underscore"
    case "$name" in "$DB_NAME" | mysql | sys | information_schema | performance_schema)
        die "refusing to restore over '$name'; use 'production' to replace the live database" ;;
    esac
    require_mysql; require_disk_space
    resolve_at; choose_dump
    TMP=$(mktemp -d "$BACKUP_ROOT/.restore-XXXXXX")
    collect_binlogs
    local exists
    exists=$(mysql_live -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$name'")
    if [ "$exists" != 0 ]; then
        [ "$REPLACE" -eq 1 ] || die "database $name already exists (add --replace to overwrite it)"
        mysql_live -e "DROP DATABASE \`$name\`"
    fi
    mysql_live -e "CREATE DATABASE \`$name\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    load_into mysql_live "$name"
    run_checks mysql_live "$name" 0
    [ "$CHECK_FAILURES" -eq 0 ] || die "$CHECK_FAILURES check(s) failed on $name"
    log "ok: $name holds the data as of $AT_UTC UTC"
    write_status ok "restored into database $name as of $AT_UTC UTC"
}

restore_production() {
    [ -n "$AT" ] || die "production restore needs an explicit --at"
    [ "$CONFIRM" -eq 1 ] || die "production restore needs --yes-i-am-sure"
    require_mysql
    local ts staging old tables_live tables_staging rename=""
    ts=$(utc_stamp)
    staging="${APP_DB}_restore_staging"
    old="${APP_DB}_before_restore_${ts,,}"

    log "safety backup of the current state"
    JOB=db-manual "$SCRIPT_DIR/backup-db.sh" manual "before-restore-$ts" > /dev/null
    JOB=binlog "$SCRIPT_DIR/backup-binlog.sh"

    REPLACE=1
    restore_into_db "$staging"

    log "stopping backend"
    BACKEND_STOPPED=1
    docker compose -f "$COMPOSE_DIR/docker-compose.yml" stop backend
    mysql_live -e "CREATE DATABASE \`$old\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    tables_live=$(mysql_live -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='$DB_NAME'")
    tables_staging=$(mysql_live -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='$staging'")
    local t
    for t in $tables_live; do rename+="\`$DB_NAME\`.\`$t\` TO \`$old\`.\`$t\`, "; done
    for t in $tables_staging; do rename+="\`$staging\`.\`$t\` TO \`$DB_NAME\`.\`$t\`, "; done
    log "swapping databases (old data kept in $old)"
    mysql_live -e "SET SESSION sql_log_bin=0; RENAME TABLE ${rename%, };"
    mysql_live -e "DROP DATABASE \`$staging\`"

    log "starting backend"
    docker compose -f "$COMPOSE_DIR/docker-compose.yml" start backend
    BACKEND_STOPPED=0
    local i
    for i in $(seq 1 60); do
        [ "$(docker inspect -f '{{.State.Health.Status}}' "$(docker compose -f "$COMPOSE_DIR/docker-compose.yml" ps -q backend)" 2>/dev/null)" = healthy ] && break
        sleep 5
    done
    JOB=db-manual "$SCRIPT_DIR/backup-db.sh" manual "after-restore-$ts" > /dev/null
    log "done. Live data is as of $AT_UTC UTC. Previous data is in database $old."
    log "to undo: run this script again with --at set to just before $(date -u '+%F %T') UTC, or swap $old back."
    write_status ok "production restored to $AT_UTC UTC (previous data in $old)"
}

case "$MODE" in
    list) list ;;
    test) restore_test 0 ;;
    verify) AT=""; KEEP=0; restore_test 1 ;;
    into-db) restore_into_db "$TARGET_NAME" ;;
    production) restore_production ;;
esac
