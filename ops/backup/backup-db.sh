#!/usr/bin/env bash
# Full logical dump of the application database.
#
#   backup-db.sh hourly            scheduled; also promotes daily/weekly/monthly copies and prunes
#   backup-db.sh pre-deploy <tag>  called by the deploy workflow before new code runs
#   backup-db.sh manual [tag]      before risky manual work (imports, SQL fixes, ...)
#
# Each dump rotates the binary log (--flush-logs) and records the binlog position it starts from
# (--source-data=2), so restore.sh can replay binlogs on top of it to any point in time.
# Prints the path of the new dump on stdout.

source "$(dirname "$(readlink -f "$0")")/lib.sh"

KIND="${1:-hourly}"
TAG="${2:-}"
case "$KIND" in
    hourly) begin_job db ;;
    pre-deploy | manual) begin_job "db-$KIND" ;;
    *) echo "usage: $0 [hourly|pre-deploy|manual] [tag]" >&2; exit 2 ;;
esac

require_mysql
require_disk_space

ts=$(utc_stamp)
suffix=""
if [ "$KIND" != hourly ]; then
    clean_tag=$(printf '%s' "$TAG" | tr -c 'A-Za-z0-9._-' '-' | cut -c1-40)
    suffix="_${KIND}${clean_tag:+-$clean_tag}"
fi
dir="$BACKUP_ROOT/db/$KIND"
mkdir -p "$dir"
final="$dir/${APP_NAME}_${ts}${suffix}.sql.gz"
partial="$final.partial"
rm -f "$partial"

log "dumping $DB_NAME to $final"
mysql_tool mysqldump \
    --single-transaction --routines --triggers --events \
    --flush-logs --source-data=2 --set-gtid-purged=OFF \
    --no-tablespaces --hex-blob --default-character-set=utf8mb4 \
    "$DB_NAME" | gzip -6 > "$partial"

gzip -t "$partial" || die "gzip check failed for $partial"
zcat "$partial" | tail -n 1 | grep -q 'Dump completed' || die "dump is incomplete (no 'Dump completed' marker)"
position=$(zcat "$partial" | head -n 80 | grep -Eo "SOURCE_LOG_FILE='[^']+', SOURCE_LOG_POS=[0-9]+" | head -1 || true)
[ -n "$position" ] || die "dump has no binlog position; point-in-time restore would be impossible"

finalize "$partial" "$final"
size=$(file_size "$final")
log "ok: $(basename "$final") ($size bytes, $position)"

if [ "$KIND" = hourly ]; then
    name=$(basename "$final")
    day=${ts:0:8}
    month=${ts:0:6}
    week=$(date -u +%G-W%V)

    promote() {
        local target="$BACKUP_ROOT/db/$1"
        mkdir -p "$target"
        ln -f "$final" "$target/$name"
        ln -f "$final.sha256" "$target/$name.sha256"
        log "promoted to $1"
    }
    newest_in() {
        mkdir -p "$BACKUP_ROOT/db/$1"
        find "$BACKUP_ROOT/db/$1" -maxdepth 1 -name "${APP_NAME}_*.sql.gz" -printf '%f\n' | sort | tail -1
    }

    latest=$(newest_in daily)
    [ -n "$latest" ] && [ "$(stamp_of "$latest" | cut -c1-8)" = "$day" ] || promote daily

    latest=$(newest_in weekly)
    if [ -z "$latest" ] || [ "$(date -u -d "@$(stamp_to_epoch "$(stamp_of "$latest")")" +%G-W%V)" != "$week" ]; then
        promote weekly
    fi

    latest=$(newest_in monthly)
    [ -n "$latest" ] && [ "$(stamp_of "$latest" | cut -c1-6)" = "$month" ] || promote monthly

    # Hourly copies live 48 h; promoted copies are hard links, so pruning hourly never removes them.
    find "$BACKUP_ROOT/db/hourly" -maxdepth 1 -type f \( -name "${APP_NAME}_*.sql.gz" -o -name "${APP_NAME}_*.sql.gz.sha256" \) \
        -mmin +$((48 * 60)) -print -delete | sed 's/^/pruned /' >&2 || true
    keep_newest "$BACKUP_ROOT/db/daily" "${APP_NAME}_*.sql.gz" 30
    keep_newest "$BACKUP_ROOT/db/weekly" "${APP_NAME}_*.sql.gz" 12
    keep_newest "$BACKUP_ROOT/db/monthly" "${APP_NAME}_*.sql.gz" 12
elif [ "$KIND" = pre-deploy ]; then
    keep_newest "$dir" "${APP_NAME}_*.sql.gz" 20
else
    keep_newest "$dir" "${APP_NAME}_*.sql.gz" 30
fi

write_status ok "dump $(basename "$final")" \
    "$(jq -n --arg file "$final" --argjson size "$size" --arg position "$position" \
        '{file:$file, size:$size, position:$position}')"
echo "$final"
