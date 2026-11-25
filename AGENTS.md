# AGENTS.md — wakey

Open source uptime monitor (self-host and EU cloud): Java/Spring Boot server, Go agent, one
React + Vite frontend for the panel and public status pages. One maintainer; long-lived project.

Domain: backend and frontend (the maintainer's generic rules live outside this repository; this file adds only local ones)

## Read when

| Task | Read |
|---|---|
| Any doc, spec or research | `docs/index.md` |
| Product behaviour | `docs/specs/product-features.md` |
| UI or status page | `docs/specs/ui-ux.md` |
| Theme, colours, sizes, mobile layout | `docs/decisions/0009-theme.md`, `docs/specs/theme.md` |
| Stack, library or database choice | `docs/decisions/0002-stack.md` |
| Check executor, HTTP client, DNS | `docs/decisions/0003-network-checks.md` |
| Frontend library | `docs/decisions/0004-frontend.md` |
| Server module, interface or cross-role flow | `docs/decisions/0005-server-architecture.md` |
| Endpoint, auth, errors, paging or frontend API client | `docs/decisions/0006-api-contract.md` |
| Build checks or lint rules | `docs/decisions/0007-static-checks.md` |
| Writing or changing tests | `docs/decisions/0007-static-checks.md` (section "Struktura testów") |
| SQL and PostgreSQL-only features | `docs/decisions/0008-postgresql-only.md` |
| Research task | `docs/research/backlog.md` |

## Local rules

- Dependencies: use a proven, long-supported library with a large community for anything non-trivial or security-sensitive; write trivial things yourself. Every new dependency needs the owner's approval.
- Keep server modules separable: roles (`wakey.roles`) talk only through the queue port and the database, never shared in-memory state (ADR 0005).
- Server conventions are tests, not review comments: controllers in `<module>.web`, no header parameters, errors as `ErrorCode` enums, interfaces only as ADR 0005 ports, `JdbcClient` only for SQL modules, Lombok constructors, no media type or header literals (`ServerConventionsTest`, Checkstyle; ADR 0005).
- Names: root package and `groupId` `dev.wakey`, configuration prefix `wakey.*` (`WAKEY_*`), license AGPL-3.0-or-later (ADR 0002).
- Simple arguments: a call argument carries at most one method call; extract a longer expression into a named variable (ADR 0007).
- A rule from an ADR that can be checked statically becomes an ArchUnit or lint rule, not a review comment.
- Colours, sizes, radius and typography only from `--app-*` tokens or the Mantine theme (lint guards hex and px); a state is always `Badge color="<state>"` + icon + text, never colour alone; UI text goes through i18n.
- A screen is built from its accepted mockup in `docs/specs/screens/`; with no mockup, the stage's mockup wave (see `docs/specs/roadmap.md`) comes first.
- Every screen works on mobile (< 768 px, 44 px touch targets); theme rules and mobile layout are in ADR 0009 and `docs/specs/theme.md`.
- PostgreSQL is the only database (ADR 0008). Every PostgreSQL-only place (SQL, Liquibase changeset, code, configuration) carries a `postgres-only: ...` comment and a row in the ADR 0008 table in the same pull request; `PostgresOnlyRegistryTest` enforces the row.
- Docs stay in Polish until backlog item 13 (English translation); new docs may be in Polish.
- `docs/research/` is a local doc type for research briefs and reports. A report recommends; only an ADR in `docs/decisions/` decides.
- Stage numbers (0–9) and their scope come from `docs/specs/roadmap.md`.
- The repository is public: no file, commit or pull request may contain the names of the owner's other projects, local paths, workspace layout or personal data.
- Pull requests come only from collaborators; settings and files follow the baseline of the public `CaffeinatedPinguin/templates` repository. Workflows avoid `pull_request_target`, `workflow_run` and `issue_comment`, and carry no secrets in pull requests.

## Verify

- `bash scripts/ci.sh` — checks the documentation links.
- the maintainer's workspace check `check-ai.ps1 -Root . -Kind repo` ends with `CHECK-OK`.

## Never

- Add a dependency without the owner's approval.
- Treat a research report as a decision.
