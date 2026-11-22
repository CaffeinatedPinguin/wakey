# GitHub, CI/CD i walidacja workflowów: co wakey bierze z `templates`

Raport (nie decyzja) przed założeniem repozytorium na GitHubie i etapem 0 ([roadmap.md](../specs/roadmap.md)). Źródło: repozytorium `CaffeinatedPinguin/templates` (stan na commit `dbd8866`). Zasady z `templates` czytamy stamtąd, a nie kopiujemy.

## Co już jest

| Element | Gdzie | Co daje |
|---|---|---|
| Bazowy zestaw ustawień | `templates/baseline/bin/apply.sh` + `config.env` | Merge tylko przez squash. Ruleset na `main` wymaga PR z zielonym `ci-required` i blokuje force-push. Lista dozwolonych akcji (`ACTION_PATTERNS`), 4 etykiety, Dependabot alerts. Skrypt jest idempotentny, a `--dry-run` pokazuje każdą zmianę. |
| Pliki repo | `templates/baseline/repo-files/` | `SECURITY.md`, `CONTRIBUTING.md`, formularze zgłoszeń (bug, feature, task), szablon PR, `dependabot.yml`, `dependabot-auto-merge.yml`, `stale.yml`, ogólny `ci.yml` i `scripts/ci.sh` |
| Workflowy wielokrotnego użytku | `templates/.github/workflows/` | `conventions.yml` (tytuł PR), `dependency-review.yml`, `docker.yml` (build, test dymny, publikacja w GHCR tylko z tagu `vX.Y.Z`), `release.yml` (wydanie z notatek), `node-ci.yml` (pnpm) |
| **Walidacja workflowów** | `templates/tools/check.sh` (`[dir]`), `tools/install-hooks.sh` | **actionlint i zizmor z przypiętych obrazów Dockera**, lokalnie i w CI jednakowo. Hook `pre-push` uruchamia to samo. Skrypt przyjmuje katalog, więc sprawdza też inne repo: `bash ../templates/tools/check.sh .` |
| Przypinanie akcji | `templates/baseline/bin/pin-actions.sh` | Zamienia `@LATEST` na pełny SHA z komentarzem `# vX.Y.Z`. Dependabot potem podbija SHA. |
| Zasady bezpieczeństwa | `templates/docs/security-principles.md` | `permissions: {}` na starcie, `persist-credentials: false`, PR bez sekretów, publikacja tylko z tagu, `timeout-minutes` w każdym jobie, bez `secrets: inherit` |
| Procedura nowego repo | `templates/docs/new-repository.md` | 13 kroków od `gh repo create` do pierwszego wydania. Właściciel zatwierdza `apply.sh` po `--dry-run`. |
| Wzorzec wywołania | `templates/examples/caller.yml` | cienki `ci.yml` wołający workflowy po SHA i job `ci-required`, który zbiera wyniki |

## Czego brakuje dla wakey

1. **Brak workflowu dla Mavena.** `node-ci.yml` obsługuje tylko pnpm i nie ma `working-directory`, a wakey ma serwer w Mavenie i frontend w pnpm w jednym repo. Wybrano wariant (b): `maven-ci.yml` buduje tylko Mavena, frontend buduje `node-ci.yml` z `working-directory`, a `changes.yml` uruchamia joby tylko dla zmienionej części (`server`, `frontend`). Wariant (a) — ogólny `ci.yml` z baseline i `scripts/ci.sh` — pozostaje prostszą alternatywą dla repo bez drugiego stosu.
2. **Test dymny obrazu — rozwiązane.** `docker.yml` sprawdza `http://127.0.0.1:8080/healthz` przez `wget` wewnątrz kontenera. Wakey wystawia `/healthz` (mapowanie health z Actuatora), a obraz Alpine zawiera `wget` z busyboxa. Job `docker` uruchamia się po merge'u do `main` i przy ręcznym uruchomieniu, nie w PR.
3. **Zgodność tagu z wersją — rozwiązane.** `node-ci.yml` porównuje tag z `package.json`. Dla wakey wersja żyje w `pom.xml`, a porównanie tagu z wersją Mavena robi `maven-ci.yml`.
4. **Czas — zmierzony.** Pierwsze uruchomienie na GitHubie (2026-11-22, PR z backendem i frontendem, bez cache, bez testu dymnego): około 65 s od startu do zielonego `ci-required`. `server / verify` trwał 45 s, w tym Maven 37 s (Error Prone, Checkstyle i 13 testów, z PostgreSQL w Testcontainers). `frontend / verify` trwał 24 s. Joby `docs`, `changes`, `conventions` i `dependency-review` trwają po kilka sekund. Limit `maven-ci.yml` (25 min) ma duży zapas.
5. **Dependabot.** Włączone ekosystemy `maven`, `npm` (pnpm), `docker` i `github-actions`, każdy z grupą `minor-patch` i `security`, zgodnie z szablonem.
6. **Agent Go** (osobne repo, [0002](../decisions/0002-stack.md)): ogólny `ci.yml` z `setup-go` i `scripts/ci.sh`, bez nowych workflowów.

## Ustalone (2026-11-21/22, zapisane w [0002](../decisions/0002-stack.md))

- **Repo publiczne od 2026-11-22.** PR tylko od collaborators (Settings → Pull requests). Pełny baseline nałożony przez `apply.sh` (ruleset, push protection, CodeQL, zatwierdzanie uruchomień z forków).
- **Kanban na GitHub Projects** z polem statusu. Baseline zostawia tylko 4 etykiety (`bug`, `enhancement`, `needs-info`, `dependencies`), więc tablica nie opiera się na etykietach. Etapy roadmapy to kamienie milowe od „Etap 0 — Fundament techniczny” do „Etap 9 — Start” (2026-11-22).
- **Licencja AGPL-3.0-or-later** (ADR 0002). Krok 1 z `new-repository.md` używa `--license mit`, więc dla wakey trzeba `--license agpl-3.0`.
- **CI: wariant (b), `maven-ci.yml` w `templates`.** Workflow wielokrotnego użytku dla Mavena buduje tylko serwer; frontend w pnpm buduje `node-ci.yml` z `working-directory`. Wymagał:
  - opisu wejść i wyjść w `templates/docs/workflows.md`;
  - wpisu w `ACTION_PATTERNS`;
  - walidacji `tools/check.sh` (actionlint i zizmor).

  Rozwiązuje punkty 2 i 3: ścieżkę health w teście dymnym i porównanie tagu z wersją w `pom.xml`. Zmiana przeszła przez PR w `templates` przed pierwszym CI wakey.

## Źródła

- `CaffeinatedPinguin/templates`: `README.md`, `docs/new-repository.md`, `docs/workflows.md`, `docs/use-in-a-project.md`, `docs/releases.md`, `docs/dependencies.md`, `docs/security-principles.md`, `baseline/README.md`, `baseline/config.env`, `tools/check.sh`
