# wakey docs index

One line per document: `file — what it owns — read when`. Documents are in Polish until the
English translation (backlog item 13).

| File | Owns | Read when |
|---|---|---|
| [specs/product-features.md](specs/product-features.md) | What the product does: components, roles, MVP/v1/later | Any product or feature work |
| [specs/ui-ux.md](specs/ui-ux.md) | UI behaviour, screens, components and design tokens | Frontend or UX work |
| [specs/roadmap.md](specs/roadmap.md) | Stages 0–9: order, scope, milestones and research needed per stage | Planning a stage or deciding when a feature lands |
| [decisions/0001-stack.md](decisions/0001-stack.md) | Superseded first stack decision; still owns the shared SQL subset rules | Writing SQL for both databases |
| [decisions/0002-stack.md](decisions/0002-stack.md) | Server, databases, result storage, leader/HA, agent, build and delivery | Choosing a library, tool or database feature |
| [decisions/0003-network-checks.md](decisions/0003-network-checks.md) | HTTP client, DNS, connections, HTTP/2 and ICMP for server checks | Building the check executor |
| [decisions/0004-frontend.md](decisions/0004-frontend.md) | Frontend libraries | Adding or changing a frontend dependency |
| [decisions/0005-server-architecture.md](decisions/0005-server-architecture.md) | Server modules, ArchUnit boundaries, ports, role-to-role queue, domain model | Adding a module, an interface or a cross-role flow |
| [decisions/0006-api-contract.md](decisions/0006-api-contract.md) | REST style, OpenAPI, errors, cursors, login and sessions, HTTP surfaces, limits, frontend API client | Adding or changing an endpoint, auth or the frontend API layer |
| [decisions/0007-static-checks.md](decisions/0007-static-checks.md) | Formatter, Error Prone/NullAway, Checkstyle and ESLint rules | Changing build checks or a lint rule |
| [research/backlog.md](research/backlog.md) | Open research topics and when each is needed | Planning the next stage or a research task |
| [research/ui-ux-brief.md](research/ui-ux-brief.md) | Brief that produced the UI/UX spec | Revisiting UI/UX research scope |
| [research/stack-server.md](research/stack-server.md) | Server language, framework and async model options (item 1) | Choosing or revisiting the server stack |
| [research/stack-db.md](research/stack-db.md) | Databases, result storage, DB access, migrations, backup, leader lock (item 1) | Database or storage decisions |
| [research/stack-ops.md](research/stack-ops.md) | Agent language, process model, build, repo, CI, distribution, releases (item 1) | Build, release or deployment decisions |
| [research/frontend.md](research/frontend.md) | Frontend options and minimal dependency set (item 1.5) | Choosing a frontend library or writing own UI code |
| [research/net-java.md](research/net-java.md) | Server network checks on JDK 25 (item 2a) | Building the server check executor |
| [research/net-go.md](research/net-go.md) | Go agent checks, distribution and parity (item 2b) | Building the agent or its release |

## Rules

- One line per file; no nested lists, no status.
- `research/` holds research briefs and reports; they inform decisions but are not decisions.
- Every link resolves in a fresh clone.
