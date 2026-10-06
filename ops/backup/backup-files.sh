#!/usr/bin/env bash
# Daily archive of uploaded files and server configuration.
#
#   files/   uploads volume (/app/data: product, partner, employee images, logos, attachments)
#   config/  .env, compose file, nginx, TLS certificates, WireGuard, backup timers
#
# Config archives contain secrets (.env, private keys); everything under BACKUP_ROOT is root-only.

source "$(dirname "$(readlink -f "$0")")/lib.sh"

begin_job files
require_disk_space

ts=$(utc_stamp)

archive() {
    local kind="$1"; shift
    local dir="$BACKUP_ROOT/$kind/daily"
    mkdir -p "$dir"
    local final="$dir/${kind}_${ts}.tar.gz"
    rm -f "$final.partial"
    tar -czf "$final.partial" "$@"
    gzip -t "$final.partial"
    tar -tzf "$final.partial" > /dev/null
    finalize "$final.partial" "$final"

    # First archive of each month is kept for a year.
    local month_dir="$BACKUP_ROOT/$kind/monthly"
    mkdir -p "$month_dir"
    if ! find "$month_dir" -maxdepth 1 -name "${kind}_${ts:0:6}*.tar.gz" | grep -q .; then
        ln -f "$final" "$month_dir/$(basename "$final")"
        ln -f "$final.sha256" "$month_dir/$(basename "$final").sha256"
    fi
    keep_newest "$dir" "${kind}_*.tar.gz" 30
    keep_newest "$month_dir" "${kind}_*.tar.gz" 12
    log "ok: $(basename "$final") ($(file_size "$final") bytes)"
    echo "$final"
}

[ -d "$UPLOADS_DIR" ] || die "uploads directory $UPLOADS_DIR not found"
uploads=$(archive files -C "$UPLOADS_DIR" .)

config_paths=()
for p in "${COMPOSE_DIR#/}/.env" "${COMPOSE_DIR#/}/docker-compose.yml" "${COMPOSE_DIR#/}"/nginx-*.conf \
         etc/nginx etc/letsencrypt etc/wireguard; do
    [ -e "/$p" ] && config_paths+=("$p")
done
for unit in /etc/systemd/system/${APP_NAME}-backup-*; do
    [ -e "$unit" ] && config_paths+=("${unit#/}")
done
config=$(archive config -C / "${config_paths[@]}")

write_status ok "archived uploads and config" \
    "$(jq -n --arg uploads "$uploads" --arg config "$config" \
        --argjson uploadsSize "$(file_size "$uploads")" --argjson configSize "$(file_size "$config")" \
        '{uploads:$uploads, uploadsSize:$uploadsSize, config:$config, configSize:$configSize}')"
