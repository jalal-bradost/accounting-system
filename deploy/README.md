# Deploying

One server, Docker Compose, nginx in front. The same files serve any project built on this template:
pick an `APP_NAME` (lowercase letters, digits, hyphens) and keep it consistent everywhere.

## Server layout

```
/opt/<app>/
  docker-compose.yml   <- deploy/docker-compose.yml
  .env                 <- from deploy/.env.example (chmod 600, never committed)
  backend/             <- this repo
  frontend/            <- the frontend repo
```

The folder name is the compose project name, so containers and volumes are called `<app>-mysql-1`,
`<app>_mysql_data`, `<app>_backend_data` ... `ops/backup` relies on exactly those names (`APP_NAME=<app>`).

## First time

1. Create the layout above, clone both repos, copy `deploy/docker-compose.yml` and `deploy/.env.example`
   (as `.env`) into `/opt/<app>/` and fill in the secrets.
2. nginx: render a config for your domain and enable it.
   ```bash
   sed 's/__DOMAIN__/erp.example.com/g' deploy/nginx-http-only.conf.template > /etc/nginx/sites-available/<app>.conf
   # after certbot has issued the certificate:
   sed 's/__DOMAIN__/erp.example.com/g' deploy/nginx.conf.template > /etc/nginx/sites-available/<app>.conf
   ```
3. `cd /opt/<app> && docker compose up -d --build`
4. Log in with the seeded admin and change the password.
5. Backups: `APP_NAME=<app> /opt/<app>/backend/ops/backup/install.sh` (see `ops/backup/README.md`).
   MySQL must run with binary logging on (the default for `mysql:8.4`).

## CI/CD

`.github/workflows/ci.yml` runs the H2 test suite plus a MySQL Flyway/boot smoke test on every push and PR.
`deploy.yml` (backend) and the frontend `deploy.yml` deploy over SSH. They are manual until you add a push
trigger. Set these in the GitHub `production` environment:

| Kind | Name | Value |
|---|---|---|
| secret | `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` | server access |
| variable | `APP_NAME` | the `<app>` name above (defaults to `erp`) |
| variable | `PUBLIC_URL` | frontend CI build only, e.g. `https://erp.example.com` |

The backend deploy takes a database dump first (when backups are installed) and stops if that fails.

## AI assistant

Off by default. Switch it on per company in Settings > Assistant. For Ollama either start the bundled one
(`docker compose --profile local-ollama up -d`) or point `APP_AI_OLLAMA_BASE_URL` at a GPU host.
Provider API keys entered in Settings are stored in the database (`platform_assistant_settings`) in plain text,
so restrict who can open that page and treat DB backups as sensitive.
