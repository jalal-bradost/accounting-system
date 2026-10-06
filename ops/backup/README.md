# Backups

> **Naming:** every script reads `APP_NAME` (default `erp`, exported for manual runs; the systemd units get it from install.sh. Pass it
> inline: `APP_NAME=acme-erp ./install.sh`). It must equal the docker compose project name, i.e. the folder
> that holds `docker-compose.yml` (`/opt/<app>`). In this document `<app>` means that name and `<app_db>` the
> same name with hyphens turned into underscores (the default database name). The repo is expected at
> `/opt/<app>/backend`.

Automatic, local backups of the production server with point-in-time recovery.
Everything lives in `/var/backups/<app>` (root-only). Backup status is printed on every SSH login,
or run `/opt/<app>/backend/ops/backup/status.sh`.

> These backups are on the same server. They protect against mistakes (bad deletes, bad deploys,
> bad imports), **not** against losing the server or its disk. Add an off-server copy for that.

## What is backed up

| What | When | Kept | Where |
|---|---|---|---|
| Full database dump | hourly at :05 | 48 h hourly, 30 daily, 12 weekly, 12 monthly | `db/hourly`, `db/daily`, … |
| MySQL binary logs (every change) | every 15 min | 35 days | `binlog/` |
| Database dump before each backend deploy | each deploy | last 20 | `db/pre-deploy` |
| Uploaded files (`/app/data`) | daily 03:30 | 30 daily, 12 monthly | `files/` |
| Server config (`.env`, compose, nginx, TLS, WireGuard) | daily 03:30 | 30 daily, 12 monthly | `config/` |
| Restore test of the newest backup | Sundays 04:30 | status only | `status/verify.*.json` |

Dumps + binlogs mean the database can be rebuilt **as of any minute** in the last 35 days, and as of
any kept dump before that. Every file has a `.sha256` next to it and is checked before use.

## Restoring

All commands run as root on the server. `--at` is in server time (Europe/Berlin) unless you add an
offset, e.g. `"2026-10-05 14:32 +03:00"` for Baghdad time.

```bash
cd /opt/<app>/backend/ops/backup

./restore.sh list                                   # what is available

# Look at the data as it was, without touching anything (throwaway container, removed afterwards)
./restore.sh test --at "2026-10-05 14:30 +03:00"
./restore.sh test --at "2026-10-05 14:30 +03:00" --keep   # keep it to query; prints how to connect

# Recover a few records: rebuild into a side database on the live server, then compare/copy rows
./restore.sh into-db <app>_restore --at "2026-10-05 14:30 +03:00"
#   e.g. INSERT INTO <app_db>.acc_customer_invoice SELECT * FROM <app>_restore.acc_customer_invoice WHERE id='...';
#   DROP DATABASE <app>_restore;   when finished

# Roll the whole live database back (bad deploy, mass delete). Takes a safety dump first,
# stops the backend, swaps the data in, restarts it. The replaced data stays in
# database <app>_before_restore_<time> until you drop it.
./restore.sh production --at "2026-10-05 14:30 +03:00" --yes-i-am-sure
```

Restoring uploaded files or config: pick an archive in `files/daily` or `config/daily` and extract it,
e.g. `tar -xzf files_<time>.tar.gz -C /var/lib/docker/volumes/<app>_backend_data/_data ./product-images/<file>`.

## Before risky manual work

```bash
./backup-db.sh manual "before-price-import"     # prints the dump path
```

## Setup and maintenance

- Install or repair (safe to re-run): `./install.sh`
- Timers: `systemctl list-timers '<app>-backup-*'`; logs: `journalctl -u <app>-backup-db -n 50`
- Run a job now: `systemctl start <app>-backup-db` (or `-binlog`, `-files`, `-verify`)
- Scripts update with each backend deploy (`/opt/<app>/backend/ops/backup`); re-run `install.sh` only
  when files under `systemd/` change.
