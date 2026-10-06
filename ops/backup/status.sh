#!/usr/bin/env bash
# Show when each backup job last succeeded. Used by the SSH login banner.
#   status.sh           human-readable, coloured
#   status.sh --check   exit 1 if any job is stale or failed (for scripts)

APP_NAME="${APP_NAME:-erp}"
BACKUP_ROOT="${BACKUP_ROOT:-/var/backups/$APP_NAME}"
COMPOSE_DIR="${COMPOSE_DIR:-/opt/$APP_NAME}"
STATUS="$BACKUP_ROOT/status"
CHECK=0; [ "${1:-}" = --check ] && CHECK=1

if { [ -t 1 ] || [ -n "${FORCE_COLOR:-}" ]; } && [ "$CHECK" -eq 0 ]; then RED=$'\e[31m'; GREEN=$'\e[32m'; RESET=$'\e[0m'; else RED=""; GREEN=""; RESET=""; fi

[ -d "$STATUS" ] || { echo "$APP_NAME backups: ${RED}not installed${RESET}"; exit 1; }

now=$(date +%s); bad=0
row() {  # job label max_age_seconds
    local job="$1" label="$2" max="$3" ok="$STATUS/$1.ok.json" last="$STATUS/$1.last.json" age when state
    if [ -f "$ok" ]; then
        age=$(( now - $(jq -r .epoch "$ok") ))
        when="$(date -d "@$(jq -r .epoch "$ok")" '+%a %d %b %H:%M') ($(( age / 3600 ))h $(( age % 3600 / 60 ))m ago)"
    else
        age=999999999; when="never"
    fi
    if [ -f "$last" ] && [ "$(jq -r .result "$last")" != ok ]; then
        state="${RED}FAILED: $(jq -r .message "$last")${RESET}"; bad=1
    elif [ "$age" -gt "$max" ]; then
        state="${RED}STALE${RESET}"; bad=1
    else
        state="${GREEN}ok${RESET}"
    fi
    printf '  %-26s %-30s %s\n' "$label" "$when" "$state"
}

echo "$APP_NAME backups ($BACKUP_ROOT, $(du -sh "$BACKUP_ROOT" 2>/dev/null | cut -f1)):"
row db       "database dump (hourly)"  $(( 2 * 3600 ))
row binlog   "binlogs (15 min)"        $(( 45 * 60 ))
row files    "uploads + config (daily)" $(( 26 * 3600 ))
row verify   "restore test (weekly)"   $(( 8 * 86400 ))
[ -f "$STATUS/db-pre-deploy.ok.json" ] && \
    printf '  %-26s %s\n' "last pre-deploy dump" "$(date -d "@$(jq -r .epoch "$STATUS/db-pre-deploy.ok.json")" '+%a %d %b %H:%M')"
echo "  restore help: $COMPOSE_DIR/backend/ops/backup/restore.sh (see README.md there)"

[ "$CHECK" -eq 1 ] && exit "$bad"
exit 0
