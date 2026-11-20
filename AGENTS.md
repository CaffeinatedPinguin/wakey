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
| Stack, library or database choice | `docs/decisions/0001-stack.md` |
| Research task | `docs/research/backlog.md` |

## Local rules

- KISS first: prefer the platform (JDK, Go stdlib, browser APIs) and own small code over a new library. Every new dependency needs the owner's approval.
- Docs stay in Polish until backlog item 13 (English translation); new docs may be in Polish.
- `docs/research/` is a local doc type for research briefs and reports. A report recommends; only an ADR in `docs/decisions/` decides.
- Stage numbers (0–9) come from a lost plan v0.16; the roadmap is backlog item 0.

## Verify

- `powershell -NoProfile -ExecutionPolicy Bypass -File ../../gitlab/ai/tools/check-ai.ps1 -Root . -Kind repo` — must end with `CHECK-OK`.

## Never

- Add a dependency without the owner's approval.
- Treat a research report as a decision.
