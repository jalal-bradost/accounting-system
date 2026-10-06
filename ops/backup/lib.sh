#!/usr/bin/env bash
# Shared helpers for the backup scripts. Sourced, never executed directly.
# Secrets never live here: MySQL credentials are read inside the container from $MYSQL_ROOT_PASSWORD.

set -Eeuo pipefail
umask 077

# One name drives every path below. Use lowercase letters, digits and hyphens (e.g. APP_NAME=acme-erp).
# It must match the docker compose project name (the folder the compose file lives in).
APP_NAME="${APP_NAME:-erp}"
APP_DB="${APP_NAME//-/_}"
BACKUP_ROOT="${BACKUP_ROOT:-/var/backups/$APP_NAME}"
COMPOSE_DIR="${COMPOSE_DIR:-/opt/$APP_NAME}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-$APP_NAME-mysql-1}"
MYSQL_IMAGE="${MYSQL_IMAGE:-mysql:8.4}"
# The official mysql image has no mysqlbinlog; Percona Server 8.4 ships the same 8.4 tool.
MYSQLBINLOG_IMAGE="${MYSQLBINLOG_IMAGE:-percona/percona-server:8.4}"
DB_NAME="${DB_NAME:-$APP_DB}"
MYSQL_DATA_DIR="${MYSQL_DATA_DIR:-/var/lib/docker/volumes/${APP_NAME}_mysql_data/_data}"
UPLOADS_DIR="${UPLOADS_DIR:-/var/lib/docker/volumes/${APP_NAME}_backend_data/_data}"
MAX_DISK_USED_PCT="${MAX_DISK_USED_PCT:-90}"

JOB="${JOB:-backup}"

log() { printf '[%s] %s: %s\n' "$(date -u +%FT%TZ)" "$JOB" "$*" >&2; }

# status/<job>.last.json is written on every run; status/<job>.ok.json only on success.
write_status() {
    local result="$1" message="$2" extra="${3:-}"
    [ -n "$extra" ] || extra='{}'
    local dir="$BACKUP_ROOT/status"
    mkdir -p "$dir"
    jq -n --arg job "$JOB" --arg result "$result" --arg message "$message" \
        --arg at "$(date -u +%FT%TZ)" --argjson epoch "$(date +%s)" --argjson extra "$extra" \
        '{job:$job, result:$result, message:$message, at:$at, epoch:$epoch} + $extra' \
        > "$dir/$JOB.last.json.partial"
    mv "$dir/$JOB.last.json.partial" "$dir/$JOB.last.json"
    if [ "$result" = ok ]; then
        cp "$dir/$JOB.last.json" "$dir/$JOB.ok.json"
    fi
}

die() {
    log "ERROR: $*"
    write_status fail "$*" || true
    exit 1
}

on_error() {
    local code=$? line="${1:-?}"
    log "ERROR: failed at line $line (exit $code)"
    write_status fail "failed at line $line (exit $code)" || true
    exit "$code"
}

begin_job() {
    JOB="$1"
    trap 'on_error $LINENO' ERR
    [ "$(id -u)" -eq 0 ] || die "must run as root"
    mkdir -p "$BACKUP_ROOT"
    chmod 700 "$BACKUP_ROOT"
    exec 9>"$BACKUP_ROOT/.lock-$JOB"
    if ! flock -n 9; then
        log "another $JOB run is in progress; skipping"
        exit 0
    fi
}

require_disk_space() {
    local used
    used=$(df --output=pcent "$BACKUP_ROOT" | tail -1 | tr -dc '0-9')
    [ "$used" -lt "$MAX_DISK_USED_PCT" ] || die "disk ${used}% used (limit ${MAX_DISK_USED_PCT}%)"
}

require_mysql() {
    docker inspect -f '{{.State.Running}}' "$MYSQL_CONTAINER" 2>/dev/null | grep -q true \
        || die "container $MYSQL_CONTAINER is not running"
}

# Run a client tool inside the live MySQL container as root, without the password warning.
mysql_tool() {
    local tool="$1"; shift
    docker exec -i "$MYSQL_CONTAINER" sh -c \
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; export MYSQL_PWD; tool="$1"; shift; exec "$tool" -uroot "$@"' \
        sh "$tool" "$@"
}

mysql_live() { mysql_tool mysql "$@"; }

utc_stamp() { date -u +%Y%m%dT%H%M%SZ; }

# Dump file names carry their UTC time: <app>_20261005T040500Z[_tag].sql.gz
stamp_of() { basename "$1" | sed -nE 's/^[a-z0-9_-]+_([0-9]{8}T[0-9]{6}Z).*/\1/p'; }

stamp_to_epoch() { date -u -d "$(echo "$1" | sed -E 's/^(....)(..)(..)T(..)(..)(..)Z$/\1-\2-\3 \4:\5:\6/')" +%s; }

# Write checksum next to the file and move it into place only after it is complete.
finalize() {
    local partial="$1" final="$2"
    (cd "$(dirname "$partial")" && sha256sum "$(basename "$partial")" \
        | sed "s/$(basename "$partial")/$(basename "$final")/" > "$(basename "$final").sha256")
    mv "$partial" "$final"
    chmod 600 "$final" "$final.sha256"
}

verify_checksum() {
    local file="$1"
    [ -f "$file.sha256" ] || return 1
    (cd "$(dirname "$file")" && sha256sum --quiet -c "$(basename "$file").sha256")
}

# Keep the newest N files matching a glob in a directory (and their .sha256).
keep_newest() {
    local dir="$1" pattern="$2" keep="$3"
    [ -d "$dir" ] || return 0
    find "$dir" -maxdepth 1 -type f -name "$pattern" ! -name '*.sha256' ! -name '*.partial' -printf '%f\n' \
        | sort -r | tail -n +"$((keep + 1))" | while read -r f; do
            rm -f "$dir/$f" "$dir/$f.sha256"
            log "pruned $dir/$f"
        done
}

file_size() { stat -c %s "$1"; }
