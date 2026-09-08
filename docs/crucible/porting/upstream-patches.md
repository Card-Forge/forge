# Upstream Patches

- **Status:** Active

Log of everything Crucible has placed outside its two reserved paths, `crucible/` and `docs/crucible/`. Required by
REV-1 ([../guidelines/05-commit-and-review.md](../guidelines/05-commit-and-review.md)) and ADR-0001
([../adr/0001-fork-layout-and-upstream-sync.md](../adr/0001-fork-layout-and-upstream-sync.md)).

Two kinds, and the distinction matters:

| Kind                                                   | Conflict risk on the next upstream sync                                  |
| ------------------------------------------------------ | ------------------------------------------------------------------------ |
| **edit** — a line changed in a file upstream owns      | **High.** Conflicts every time upstream touches the same region. Avoid   |
| **add** — a new file in a directory upstream also uses | **Low.** Only conflicts if upstream later adds a file with the same name |

Default is zero of both. Anything landing here needs a reason that survives review, and the row goes in the same commit
as the change.

## Log

| Date       | Kind | Path                                       | Why                                                                                                                                                            | Commit        |
| ---------- | ---- | ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------- |
| 2026-09-06 | add  | `.github/workflows/claude.yml`             | GitHub Actions only reads workflows from `.github/workflows/`. No alternative location exists. Created by `/install-github-app`                                | `9f0d3ad315d` |
| 2026-09-06 | add  | `.github/workflows/claude-code-review.yml` | Same. Scoped with a `paths:` filter so an upstream sync merge does not trigger a review of a 500-file Java diff                                                | `ca4135a3f1e` |
| 2026-09-06 | add  | `.prettierrc`, `.prettierignore`           | Prettier resolves config from the repo root only. `.prettierignore` is what keeps every upstream file unformatted (DOC-14)                                     | `ff8c555fdf0` |
| 2026-09-06 | add  | `.markdownlint-cli2.jsonc`                 | Same root-only resolution, and the VSCode extension reads the same file so editor and CI agree (DOC-15)                                                        | `ff8c555fdf0` |
| 2026-09-06 | add  | `CLAUDE.md`                                | Claude Code reads it from the repo root only                                                                                                                   | `ff8c555fdf0` |
| 2026-09-06 | add  | `.github/dependabot.yml`                   | Dependabot reads config from `.github/` only. Scoped to `github-actions`; a `maven` entry at the root would auto-generate PRs editing upstream `pom.xml` files | `pending`     |

### Edits

**One.** Everything else above is an `add`.

| Date       | Path        | Change            | Why                                                                                                                                                                | Conflict rule                                        |
| ---------- | ----------- | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------- |
| 2026-09-07 | `README.md` | Replaced entirely | A fork's front page has to describe the fork. Upstream's README describes Forge, which is misleading as the landing page of a repository whose purpose is Crucible | **Always keep ours.** Never merge upstream's version |

**The target is zero edits.** Accepting this one is a deliberate trade: the repository's front page is the single
most-read file and describing the wrong project there is a real cost, while `README.md` is a file whose merge conflicts
have exactly one correct resolution and no ambiguity.

`README.md` is absent from the ignore lists in `.prettierignore` and `.markdownlint-cli2.jsonc` for the same reason: it
is Crucible's file now, and subject to DOC-14 and DOC-15 like the rest.

The upstream version is preserved in git history at any commit before this one, and remains available at
[Card-Forge/forge](https://github.com/Card-Forge/forge). Attribution to Forge is kept prominent in the replacement,
along with the GPLv3 notice.

**If a second edit is ever proposed, the reason belongs in an ADR, not a table row.** One exception with a mechanical
resolution rule is manageable; a growing list is how a fork becomes unmergeable.

## Not logged here

Upstream workflows disabled through the GitHub API rather than by editing files — 13 of them, on 2026-09-06. That
changes no file and creates no conflict surface, so it is deliberately not a patch. Re-enable any of them with:

```bash
gh api -X PUT /repos/jczastkiewicz/crucible/actions/workflows/<id>/enable
```

`351717668` is `test-build.yaml`, which runs upstream's TestNG suite. Re-enable it around M1, when the Java oracle needs
to be known-buildable.
