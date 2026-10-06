#!/usr/bin/env bash
# One-time setup of the backup system on the server (safe to re-run).
#   APP_NAME=acme-erp ./install.sh   (default APP_NAME=erp; see lib.sh)
#   - creates /var/backups/$APP_NAME (root-only)
#   - installs the systemd timers and the SSH login status banner
#   - moves earlier ad-hoc backups ($COMPOSE_DIR/backups, /tmp/*.sql) into archive/
#   - runs every job once
# Scripts stay in $COMPOSE_DIR/backend/ops/backup and update with each backend deploy.

source "$(dirname "$(readlink -f "$0")")/lib.sh"
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
JOB=install

[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
for tool in docker jq flock gzip sha256sum; do
    command -v "$tool" > /dev/null || { echo "missing $tool" >&2; exit 1; }
done
require_mysql
[ "$(mysql_live -N -e 'SELECT @@log_bin')" = 1 ] || { echo "MySQL binary logging must be on" >&2; exit 1; }

chmod 755 "$SCRIPT_DIR"/*.sh
mkdir -p "$BACKUP_ROOT"/{db,binlog,files,config,archive,status}
chmod 700 "$BACKUP_ROOT"

# Earlier hand-made backups and one-off scripts: keep them, but in one root-only place.
if [ -d "$COMPOSE_DIR/backups" ] && [ ! -L "$COMPOSE_DIR/backups" ]; then
    mkdir -p "$BACKUP_ROOT/archive/previous-backups"
    find "$COMPOSE_DIR/backups" -mindepth 1 -maxdepth 1 -exec mv -n {} "$BACKUP_ROOT/archive/previous-backups/" \;
    rmdir "$COMPOSE_DIR/backups"
    ln -s "$BACKUP_ROOT" "$COMPOSE_DIR/backups"
    log "moved $COMPOSE_DIR/backups into archive/ (and linked $COMPOSE_DIR/backups to $BACKUP_ROOT)"
fi
shopt -s nullglob
tmp_sql=(/tmp/*.sql)
if [ "${#tmp_sql[@]}" -gt 0 ]; then
    mkdir -p "$BACKUP_ROOT/archive/tmp-sql"
    mv -n "${tmp_sql[@]}" "$BACKUP_ROOT/archive/tmp-sql/"
    log "moved ${#tmp_sql[@]} SQL script(s) from /tmp into archive/tmp-sql"
fi
shopt -u nullglob
chmod -R go-rwx "$BACKUP_ROOT/archive"

log "pulling $MYSQLBINLOG_IMAGE (provides mysqlbinlog for point-in-time restores)"
docker pull -q "$MYSQLBINLOG_IMAGE" > /dev/null
docker run --rm --entrypoint mysqlbinlog "$MYSQLBINLOG_IMAGE" --version

render() { sed -e "s#__APP__#$APP_NAME#g" -e "s#__COMPOSE_DIR__#$COMPOSE_DIR#g" "$1"; }
for unit in "$SCRIPT_DIR"/systemd/__APP__-backup-*; do
    render "$unit" > "/etc/systemd/system/$(basename "${unit/__APP__/$APP_NAME}")"
    chmod 644 "/etc/systemd/system/$(basename "${unit/__APP__/$APP_NAME}")"
done
render "$SCRIPT_DIR/systemd/99-__APP__-backup" > "/etc/update-motd.d/99-$APP_NAME-backup"
chmod 755 "/etc/update-motd.d/99-$APP_NAME-backup"
systemctl daemon-reload

log "running each job once"
"$SCRIPT_DIR/backup-binlog.sh"
"$SCRIPT_DIR/backup-db.sh" hourly > /dev/null
"$SCRIPT_DIR/backup-files.sh"

systemctl enable --now "$APP_NAME-backup-db.timer" "$APP_NAME-backup-binlog.timer" "$APP_NAME-backup-files.timer" "$APP_NAME-backup-verify.timer"
touch "$BACKUP_ROOT/.installed"
log "installed"
systemctl list-timers "$APP_NAME-backup-*" --no-pager
"$SCRIPT_DIR/status.sh"
