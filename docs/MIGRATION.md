# Migration: Thymeleaf `/web` UI → Next.js frontend

The Spring Boot backend originally served two surfaces:

1. **JSON REST** under `/api/v1/...` (`accounting-application` module)
2. **Thymeleaf MVC** under `/web/...` (`accounting-container` module)

The Thymeleaf UI is being replaced by a Next.js 16 frontend that lives in the
sibling project at `../accounting-system-frontend`. The new client calls only
the JSON REST API and ships an Odoo-style modular shell so additional modules
(inventory, sales, purchase, ...) can be added incrementally.

## Status

- All `*WebController` classes under
  `accounting-service/accounting-container/.../web/*` are annotated
  `@Deprecated(since = "0.2.0", forRemoval = true)`.
- They are intentionally **not deleted** yet so that the legacy UI keeps
  working until the Next.js client reaches full parity.
- Once parity is confirmed and any external links are migrated, the entire
  `web/` package, the `templates/` directory, the Thymeleaf dependency, and
  the seeder code that targets the legacy UI can be removed.

## Backend changes that support the new client

| Change                                                                     | Where                                                                                                  |
|----------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| CORS for `http://localhost:3000` on `/api/v1/**`                           | `accounting-container/.../WebMvcConfig.java`, `application.yml` (`accounting.cors.allowed-origins`)    |
| `GET /api/v1/journal-entries?companyId=` to list entries                   | `accounting-application/.../JournalEntryController.java`                                               |

No domain or persistence changes were necessary — the existing application
services already cover everything the new client needs.

## How to remove the Thymeleaf UI later

1. Delete every class under `accounting-container/.../web/*WebController.java`
   plus `WebExceptionHandler`, `WebControllerAdvice`, `WebCompanyContext`,
   `RootController`, `DashboardController`.
2. Delete `accounting-container/src/main/resources/templates/`.
3. Drop the `spring-boot-starter-thymeleaf` dependency from
   `accounting-container/pom.xml` (and `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`
   if present).
4. Optional: rename the `accounting-container` module if it now only hosts the
   REST entry point, or merge it into `accounting-application`.

## Frontend entry point

Run the Next.js client from `accounting-system-frontend`:

```bash
cd accounting-system-frontend
npm install
npm run dev
```

It defaults to `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`. Override via
an `.env.local` if your backend runs elsewhere.
