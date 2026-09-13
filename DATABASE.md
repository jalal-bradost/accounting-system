# Database configuration

This document describes how to run the accounting backend against H2 (local dev), PostgreSQL (optional dev/staging), or MySQL 8 (production).

## Profiles

| Profile | Database | Flyway location | Use case |
|---------|----------|-----------------|----------|
| dev (default) | H2 in-memory | classpath:db/migration | Local development |
| postgres | PostgreSQL | classpath:db/migration | Docker / persistent dev DB |
| prod | MySQL 8 | classpath:db/migration-mysql | VPS production |

Activate a profile with SPRING_PROFILES_ACTIVE (dev, postgres, or prod).

## Local development (H2)

No external database required. The default dev profile uses in-memory H2 in PostgreSQL compatibility mode.

```bash
cd accounting-system
mvn spring-boot:run -pl accounting-service/accounting-container
```

H2 console is enabled only in the dev profile.

## PostgreSQL (optional)

1. Start Postgres from infrastructure/docker-compose/postgres.yml
2. Run with postgres profile:

```bash
mvn spring-boot:run -pl accounting-service/accounting-container -Dspring-boot.run.profiles=postgres
```

Uses classpath:db/migration (PostgreSQL-native UUID columns).

## Production (MySQL 8)

### Environment variables

All five variables are **required** in production (no defaults in `application-prod.yml`):

| Variable | Description |
|----------|-------------|
| `DB_HOST` | MySQL host (e.g. `127.0.0.1`) |
| `DB_PORT` | MySQL port (e.g. `3306`) |
| `DB_NAME` | Database name (e.g. `accounting`) |
| `DB_USERNAME` | MySQL user (e.g. `accounting_user`) |
| `DB_PASSWORD` | MySQL password (never commit) |

Also set `SPRING_PROFILES_ACTIVE=prod`.

### Schema management

- Flyway: infrastructure/src/main/resources/db/migration-mysql/ (V1 through V15)
- Hibernate: ddl-auto=validate
- UUID columns: CHAR(36) with hibernate.type.preferred_uuid_jdbc_type=CHAR
- TEXT columns: hibernate.type.preferred_clob_mapping=TEXT

PostgreSQL migrations in db/migration/ are not used in production.

### Ubuntu 24.04 VPS example

```bash
sudo mysql -e "CREATE DATABASE accounting CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER 'accounting_user'@'127.0.0.1' IDENTIFIED BY 'your-password';"
sudo mysql -e "GRANT ALL PRIVILEGES ON accounting.* TO 'accounting_user'@'127.0.0.1';"
sudo mysql -e "FLUSH PRIVILEGES;"

cd accounting-system
mvn clean package -DskipTests -pl accounting-service/accounting-container -am

export DB_HOST='127.0.0.1'
export DB_PORT='3306'
export DB_NAME='accounting'
export DB_USERNAME='accounting_user'
export DB_PASSWORD='your-password'
export SPRING_PROFILES_ACTIVE=prod
java -jar accounting-service/accounting-container/target/accounting-container-1.0-SNAPSHOT.jar
```

Store secrets in /etc/accounting-service/env (chmod 600) for systemd.

### Production defaults

- H2 console: disabled
- Demo seed data: disabled
- RabbitMQ: disabled (enable with prod,messaging profile and a broker)

Override JWT secret in production (do not use the dev default).

### Known test note

`SettingsApiIntegrationTest.companyMe_returnsSeededDemoCompany` fails because the test expects `USD` while `PlatformRbacSeeder` seeds `IQD`. This is a **pre-existing failure** and is not caused by the MySQL implementation.

## Building

```bash
cd accounting-system
mvn clean test
mvn clean package -DskipTests -pl accounting-service/accounting-container -am
```

## Adding new migrations

1. Add V{n}__description.sql to db/migration/ (PostgreSQL/H2).
2. Add a MySQL-compatible copy to db/migration-mysql/ (UUID to CHAR(36), avoid PostgreSQL-only syntax).
