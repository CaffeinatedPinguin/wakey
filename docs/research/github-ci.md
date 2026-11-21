# GitHub, CI/CD i walidacja workflowów: co wakey bierze z `templates`

Raport (nie decyzja) przed założeniem repozytorium na GitHubie i etapem 0 ([roadmap.md](../specs/roadmap.md)). Źródło: repozytorium `github/templates` (GitHub `CaffeinatedPinguin/templates`, stan na commit `dbd8866`), skill `ai/skills/devops/github-new-repo` i działające repo `revu`. Zasady z `templates` czytamy stamtąd, a nie kopiujemy.

## Co już jest

| Element | Gdzie | Co daje |
|---|---|---|
| Bazowy zestaw ustawień | `templates/baseline/bin/apply.sh` + `config.env` | Merge tylko przez squash. Ruleset na `main` wymaga PR z zielonym `ci-required` i blokuje force-push. Lista dozwolonych akcji (`ACTION_PATTERNS`), 4 etykiety, Dependabot alerts. Skrypt jest idempotentny, a `--dry-run` pokazuje każdą zmianę. |
| Pliki repo | `templates/baseline/repo-files/` | `SECURITY.md`, `CONTRIBUTING.md`, formularze zgłoszeń (bug, feature, task), szablon PR, `dependabot.yml`, `dependabot-auto-merge.yml`, `stale.yml`, ogólny `ci.yml` i `scripts/ci.sh` |
| Workflowy wielokrotnego użytku | `templates/.github/workflows/` | `conventions.yml` (tytuł PR), `dependency-review.yml`, `docker.yml` (build, test dymny, publikacja w GHCR tylko z tagu `vX.Y.Z`), `release.yml` (wydanie z notatek), `node-ci.yml` (pnpm) |
| **Walidacja workflowów** | `templates/tools/check.sh` (`[dir]`), `tools/install-hooks.sh` | **actionlint i zizmor z przypiętych obrazów Dockera**, lokalnie i w CI jednakowo. Hook `pre-push` uruchamia to samo. Skrypt przyjmuje katalog, więc sprawdza też inne repo: `bash ../templates/tools/check.sh .` |
| Przypinanie akcji | `templates/baseline/bin/pin-actions.sh` | Zamienia `@LATEST` na pełny SHA z komentarzem `# vX.Y.Z`. Dependabot potem podbija SHA. |
| Zasady bezpieczeństwa | `templates/docs/security-principles.md` | `permissions: {}` na starcie, `persist-credentials: false`, PR bez sekretów, publikacja tylko z tagu, `timeout-minutes` w każdym jobie, bez `secrets: inherit` |
| Procedura nowego repo | `templates/docs/new-repository.md`, skill `github-new-repo` | 13 kroków od `gh repo create` do pierwszego wydania. Właściciel zatwierdza `apply.sh` po `--dry-run`. |
| Wzorzec wywołania | `revu/.github/workflows/ci.yml`, `templates/examples/caller.yml` | cienki `ci.yml` wołający workflowy po SHA i job `ci-required`, który zbiera wyniki |

## Czego brakuje dla wakey

1. **Brak workflowu dla Mavena.** `node-ci.yml` obsługuje tylko pnpm i nie ma `working-directory`, a wakey ma serwer w Mavenie i frontend w pnpm w jednym repo. Opcje:
   - **(a)** ogólny `ci.yml` z baseline i `scripts/ci.sh` (`setup-java` i `setup-node` to akcje GitHuba, więc są dozwolone bez zmian; `pnpm/action-setup` jest już na liście);
   - **(b)** nowy `maven-ci.yml` w `templates` (wymaga wpisu w `ACTION_PATTERNS` i opisu w `docs/workflows.md`).

   (a) jest prostsze na start, a (b) ma sens, gdy drugie repo w Javie będzie potrzebowało tego samego.
2. **Test dymny obrazu.** `docker.yml` sprawdza `http://127.0.0.1:8080/healthz` przez `wget` wewnątrz kontenera. Wakey musi:
   - wystawić `/healthz` na porcie 8080 (mapowanie health z Actuatora);
   - mieć `wget` w obrazie.

   Inaczej trzeba dodać do `docker.yml` wejście z ścieżką albo poleceniem.
3. **Zgodność tagu z wersją.** `node-ci.yml` porównuje tag z `package.json`. Dla wakey wersja żyje w `pom.xml`, więc porównanie tagu z wersją Mavena musi zrobić `scripts/ci.sh` albo `maven-ci.yml`.
4. **Czas.** Ogólny `ci.yml` daje `scripts/ci.sh` 12 minut, a job 15. Error Prone ([0007](../decisions/0007-static-checks.md)) i Testcontainers na PostgreSQL oraz HSQLDB mogą tego nie zmieścić. Do zmierzenia na etapie 0.
5. **Dependabot.** Do włączenia ekosystemy `maven`, `npm` (pnpm), `docker` i `github-actions`, każdy z grupą `security`, zgodnie z szablonem.
6. **Agent Go** (osobne repo, [0002](../decisions/0002-stack.md)): ogólny `ci.yml` z `setup-go` i `scripts/ci.sh`, bez nowych workflowów.

## Do decyzji przy zakładaniu repo

- **Publiczne od startu czy prywatne do etapu 9?** Na prywatnym repo w darmowym planie rulesety, code scanning i push protection nie działają (skill `github-new-repo`). Roadmapa przewiduje publikację open source w etapie 9, po nazwie, prawie i tłumaczeniu dokumentacji.
- **Kanban:** baseline zostawia tylko 4 etykiety (`bug`, `enhancement`, `needs-info`, `dependencies`), więc tablica nie powinna opierać się na etykietach. Naturalny wybór to GitHub Projects z polem statusu, a etapy roadmapy jako kamienie milowe albo pole „Etap”.
- Wariant (a) czy (b) z punktu 1.

## Źródła

- `github/templates`: `README.md`, `docs/new-repository.md`, `docs/workflows.md`, `docs/use-in-a-project.md`, `docs/releases.md`, `docs/dependencies.md`, `docs/security-principles.md`, `baseline/README.md`, `baseline/config.env`, `tools/check.sh`
- `github/revu/.github/workflows/ci.yml`
- `ai/skills/devops/github-new-repo/SKILL.md`
