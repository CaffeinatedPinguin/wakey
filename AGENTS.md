# AGENTS.md — wakey

Open source uptime monitor (self-host and EU cloud): Java/Spring Boot server, Go agent, one
React + Vite frontend for the panel and public status pages. One maintainer; long-lived project.

Domain: ../../gitlab/ai/domains/backend/AGENTS.md and ../../gitlab/ai/domains/frontend/AGENTS.md (generic rules; this file adds only local ones)

## Read when

| Task | Read |
|---|---|
| Any doc, spec or research | `docs/index.md` |
| Product behaviour | `docs/specs/product-features.md` |
| UI or status page | `docs/specs/ui-ux.md` |
| Stack, library or database choice | `docs/decisions/0002-stack.md` |
| Check executor, HTTP client, DNS | `docs/decisions/0003-network-checks.md` |
| Frontend library | `docs/decisions/0004-frontend.md` |
| Server module, interface or cross-role flow | `docs/decisions/0005-server-architecture.md` |
| Endpoint, auth, errors, paging or frontend API client | `docs/decisions/0006-api-contract.md` |
| Build checks or lint rules | `docs/decisions/0007-static-checks.md` |
| SQL for both databases | `docs/decisions/0001-stack.md` |
| Research task | `docs/research/backlog.md` |

## Local rules

- Dependencies: use a proven, long-supported library with a large community for anything non-trivial or security-sensitive; write trivial things yourself. Every new dependency needs the owner's approval.
- Keep server modules separable: roles (`app.roles`) talk only through the queue port and the database, never shared in-memory state (ADR 0005).
- A rule from an ADR that can be checked statically becomes an ArchUnit or lint rule, not a review comment.
- Docs stay in Polish until backlog item 13 (English translation); new docs may be in Polish.
- `docs/research/` is a local doc type for research briefs and reports. A report recommends; only an ADR in `docs/decisions/` decides.
- Stage numbers (0–9) come from a lost plan v0.16; the roadmap is backlog item 0.

## Verify

- `powershell -NoProfile -ExecutionPolicy Bypass -File ../../gitlab/ai/tools/check-ai.ps1 -Root . -Kind repo` — must end with `CHECK-OK`.

## Never

- Add a dependency without the owner's approval.
- Treat a research report as a decision.
