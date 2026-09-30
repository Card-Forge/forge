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

| Date       | Kind | Path                                | Why                                                                                                                                                                                                                | Commit        |
| ---------- | ---- | ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------- |
| 2026-09-06 | add  | `.github/workflows/claude.yml`      | GitHub Actions only reads workflows from `.github/workflows/`. No alternative location exists. Created by `/install-github-app`                                                                                    | `9f0d3ad315d` |
| 2026-09-06 | add  | `.prettierrc`, `.prettierignore`    | Prettier resolves config from the repo root only. `.prettierignore` is what keeps every upstream file unformatted (DOC-14)                                                                                         | `ff8c555fdf0` |
| 2026-09-06 | add  | `.markdownlint-cli2.jsonc`          | Same root-only resolution, and the VSCode extension reads the same file so editor and CI agree (DOC-15)                                                                                                            | `ff8c555fdf0` |
| 2026-09-06 | add  | `CLAUDE.md`                         | Claude Code reads it from the repo root only                                                                                                                                                                       | `ff8c555fdf0` |
| 2026-09-06 | add  | `.github/workflows/crucible-go.yml` | GitHub Actions only reads workflows from `.github/workflows/`. Crucible's own Go CI: build, race tests, and every gate. Added in M1 and logged here late — REV-1 requires the row in the same commit as the change | `b976078523d` |
| 2026-09-06 | add  | `.github/dependabot.yml`            | Dependabot reads config from `.github/` only. Scoped to `github-actions`; a `maven` entry at the root would auto-generate PRs editing upstream `pom.xml` files                                                     | `pending`     |

### Edits

**One permanent.** Everything else above is an `add`, and the card-script fixes below carry their own deletion
condition.

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

**A second permanent edit belongs in an ADR, not a table row.** One exception with a mechanical resolution rule is
manageable; a growing list is how a fork becomes unmergeable. An edit that mirrors an open pull request against upstream
is not permanent and is logged in the next section instead: it names the condition that deletes it, and a list that
empties itself is not the list that makes a fork unmergeable.

### Pending upstream fixes

An edit whose whole purpose is to disappear goes here: the same change open as a pull request against
[Card-Forge/forge](https://github.com/Card-Forge/forge), with the row and the local edit both deleted once upstream
merges it and a sync brings the identical content back.

| Date | Path | Change    | Upstream |
| ---- | ---- | --------- | -------- |
| —    | —    | none open | —        |

Carried edits exist because the corpus gates run against the fork's own tree: a card the parser rejects fails the build
whoever wrote it, and waiting for a merge would mean disabling a gate in the meantime.

`tools/apiscan` scans only `AB$`/`SP$`/`DB$` effect params, not `S:` static-ability params, so a static-ability param
written under the wrong name (Sanctum Lurker's `Affected$` where `StaticAbilityIgnoreZeroLoyalty.java` reads only
`ValidCard$`) passes every gate. Only reading a sync's diff to `forge-game/src/main/java` against the port finds one.

PORT-8: a param that never reaches its effect, a sub-ability chain broken by name, or a line that is not a script line
at all is a Forge bug, reported and fixed here rather than exempted from either gate.

Twenty rows have retired this way — #11846, #11848, #11850, #11851, #11852, #11854, #11859, #12035, #12038
(`the_disciple_of_vess.txt` and `ginger_queen_of_sweets.txt`, merged with the reviewers' edits: the card's reminder text
restored, a redundant `Controller$ You` dropped), and the 2026-09-19 batch (`clash_of_elements.txt` at upstream
`913081c68d6`, `dack_fayden_helping_hand.txt` at `#11990`, `living_library.txt` at `24546a4a121`,
`venser_fervent_forger.txt` at `45baeffbf21`, `sanctum_lurker.txt` at `#12035`). `nascent_metamorph.txt` and
`peace_talks.txt` retired without a pull request of ours: `#11958` (Command the Stage) fixed both in passing. In every
case a sync brought the identical content back, which is exactly the condition each row named. None has ever graduated
into a permanent edit, which is the outcome that would need an ADR. The list works by emptying itself.

**Conflict rule while one is open: always take upstream.** If upstream applies the identical change, git merges both
sides silently and there is nothing to resolve. If upstream fixes it differently, upstream's version wins without
discussion — the point of the edit was the fix, not the wording.

A rejected pull request stops being pending: the row moves up into **Edits**, and by the rule above that needs an ADR.

## Not logged here

Upstream workflows disabled through the GitHub API rather than by editing files — 13 of them, on 2026-09-06. That
changes no file and creates no conflict surface, so it is deliberately not a patch. Re-enable any of them with:

```bash
gh api -X PUT /repos/jczastkiewicz/crucible/actions/workflows/<id>/enable
```

`351717668` is `test-build.yaml`, which runs upstream's TestNG suite. Re-enable it around M1, when the Java oracle needs
to be known-buildable.
