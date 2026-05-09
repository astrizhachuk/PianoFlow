# Contributing to PianoFlow

Thanks for your interest in contributing.

## Workflow

PianoFlow follows a **hybrid GitHub Flow**, fully documented in [`CLAUDE.md`](./CLAUDE.md#workflow-issues-and-pull-requests).

In short:

- **File an issue** for bugs, new features, ideas, and architectural changes.
- **Open a PR directly** for typos, dependency bumps, docs edits, refactors without behavior change, and small fixes (≤30 lines).

## Branch naming

- `feat/<short-desc>` — new feature
- `fix/<short-desc>` — bug fix
- `refactor/<short-desc>` — refactor without behavior change
- `docs/<short-desc>` — documentation
- `chore/<short-desc>` — tooling, deps, CI

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/). Examples:

```
feat(midi): support running-status messages
fix(piano-staff): correct ghost-note alignment for E5
docs(architecture): clarify Domain layer dependency rule
chore(deps): bump compose-bom to 2026.05.00
```

## Pull requests

- Link the issue with `Closes #N` or `Fixes #N` when applicable.
- Fill out the PR template (Summary + Test plan).
- Keep PRs focused — split unrelated changes.
- Self-review the diff before requesting review.
- Bilingual issue/PR descriptions (English first, `---`, then Russian) are appreciated but not required. Use English alone if you prefer — a translation may be added later.

## Code standards

See [`CLAUDE.md`](./CLAUDE.md) for build commands, Clean Architecture rules, logging strategy (Timber), and localization requirements (`values/strings.xml` + `values-ru/strings.xml`).

Architecture principles: [`docs/en/plans/ARCHITECTURE_PRINCIPLES.md`](./docs/en/plans/ARCHITECTURE_PRINCIPLES.md).
Testing strategy: [`docs/en/plans/TESTING_STRATEGY.md`](./docs/en/plans/TESTING_STRATEGY.md).

## Tests

Before opening a PR:

```bash
./gradlew test lint
```

UI changes must be verified manually on a device or emulator — type checks alone do not validate visual or interaction behavior.

## Documentation

The project's primary language is English. Documentation lives in `docs/en/` (source of truth). A Russian translation under `docs/ru/` is optional and maintained for Russian-speaking contributors — feel free to add or update it, but it is not required.

If you want to provide a Russian version without speaking Russian fluently, machine translation is fine — services like [DeepL](https://www.deepl.com/translator) or Google Translate produce usable drafts. Mark the file as auto-translated in a short note at the top so a native speaker can refine it later.
