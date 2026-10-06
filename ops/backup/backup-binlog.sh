#!/usr/bin/env bash
# Copy MySQL binary logs into the backup area (every 15 minutes).
#
# Rotates the active binlog, then copies every closed binlog not yet copied, gzipped.
# Closed binlogs never change, so each one is copied once. Together with the hourly dumps
# these let restore.sh rebuild the database at any minute of the last BINLOG_KEEP_DAYS days.

source "$(dirname "$(readlink -f "$0")")/lib.sh"

BINLOG_KEEP_DAYS="${BINLOG_KEEP_DAYS:-35}"

begin_job binlog
require_mysql
require_disk_space

dir="$BACKUP_ROOT/binlog"
mkdir -p "$dir"

[ "$(mysql_live -N -e 'SELECT @@log_bin')" = 1 ] || die "binary logging is disabled on $MYSQL_CONTAINER"

mysql_live -N -e 'FLUSH BINARY LOGS'
active=$(mysql_live -N -e 'SHOW BINARY LOG STATUS' | awk '{print $1}')
[ -n "$active" ] || die "could not read the active binlog name"

copied=0
while read -r name size _; do
    [ -n "$name" ] || continue
    [ "$name" = "$active" ] && continue
    target="$dir/$name.gz"
    [ -f "$target" ] && continue
    source_file="$MYSQL_DATA_DIR/$name"
    [ -f "$source_file" ] || die "binlog $source_file not found on the host"
    [ "$(file_size "$source_file")" = "$size" ] || die "binlog $name size changed while closed"
    gzip -6 -c "$source_file" > "$target.partial"
    gzip -t "$target.partial"
    [ "$(zcat "$target.partial" | wc -c)" = "$size" ] || die "copy of $name is incomplete"
    finalize "$target.partial" "$target"
    copied=$((copied + 1))
done < <(mysql_live -N -e 'SHOW BINARY LOGS')

find "$dir" -maxdepth 1 -type f \( -name 'binlog.*.gz' -o -name 'binlog.*.gz.sha256' \) \
    -mtime +"$BINLOG_KEEP_DAYS" -print -delete | sed 's/^/pruned /' >&2 || true

newest=$(find "$dir" -maxdepth 1 -name 'binlog.*.gz' -printf '%f\n' | sort | tail -1)
log "ok: copied $copied new binlog(s); newest $newest; active $active"
write_status ok "copied $copied binlog(s)" \
    "$(jq -n --arg newest "$newest" --arg active "$active" --argjson copied "$copied" \
        '{newest:$newest, active:$active, copied:$copied}')"
