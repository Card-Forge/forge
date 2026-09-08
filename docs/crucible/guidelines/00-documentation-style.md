# DOC — Documentation Style

- **Status:** Active
- **Applies to:** every file under `docs/crucible/`, every code comment, every ADR, every commit body
- **Rule IDs:** cite as `DOC-n`

This document is written in the style it defines. Read it as the reference example.

---

## Why this style

Docs rot when long. Long docs get skimmed, then ignored, then wrong.

Compressed docs stay read. Reader is IT engineer — knows Go, knows Magic rules, knows what a parser is. Do not explain
those. Explain **decisions, constraints, reasons**.

Target: 60% fewer words, 0% less technical content.

---

## DOC-1 — Drop the fluff

**Drop:**

| Category        | Examples                                                           |
| --------------- | ------------------------------------------------------------------ |
| Articles        | a, an, the                                                         |
| Filler          | just, really, basically, simply, actually, essentially, quite      |
| Hedging         | maybe, perhaps, it seems, arguably, one might, you may want to     |
| Pleasantries    | note that, please be aware, it is worth mentioning, as you can see |
| Throat-clearing | In this section we will discuss… → delete, use heading             |

**Bad:**

> In this section, we will basically discuss the way that the card parser actually works. It is worth noting that you
> may want to be aware that the parser is essentially a two-stage process.

**Good:**

> Card parser runs two stages. Stage 1 = load time, immutable. Stage 2 = game time, mutable.

---

## DOC-2 — Never drop technical content

Compression applies to grammar. Not to facts.

**Keep exact, always:**

- Identifiers: `CardRules.Reader.parseLine`, `ApiType`, `--race`
- File paths + line refs: `forge-game/src/main/java/forge/game/GameAction.java:2897`
- Numbers with units: `33,682 scripts`, `6–10 weeks`, `±1.5pp`
- Error strings: verbatim, in backticks, never paraphrased
- Version/flag names: `go1.24`, `-DskipTests`

Fragment that loses a fact is not compression. It is a bug.

---

## DOC-3 — Sentence pattern

Default shape:

```text
[thing] [action] [reason]. [next step].
```

**Examples:**

```text
GameAction.checkStateEffects runs SBAs + layer pass. Port first — everything downstream depends on it.

Engine emits events, never stores. Keeps I/O out of hot path.

Mutex inside internal/engine = design bug. Games share only immutable data.
```

Fragments allowed. Verbless clauses allowed. Ambiguity not allowed.

---

## DOC-4 — Every rule carries a reason

Rule without reason gets ignored on the first deadline.

**Bad:**

```text
Do not use reflection.
```

**Good:**

```text
No reflection in engine packages. Reason: dispatch must be compile-time checkable,
and reflection blocks dead-code elimination. Use generated registry instead (ADR-0008).
```

Reason may be one clause. Must exist.

---

## DOC-5 — Table beats prose

Three or more parallel items → table. Always.

Prose list of parallel items is unscannable and hides gaps.

**Table needs:** header row, one row per item, no cell longer than ~2 lines.

Two items → inline is fine.

---

## DOC-6 — Structure every doc the same

```text
# AREA — Short Title

- **Status:** Draft | Active | Superseded
- **Applies to:** <scope>
- **Rule IDs:** cite as `AREA-n`

Short framing. 1-3 lines. What this governs, why it exists.

---

## Why this exists          <- only if non-obvious

## AREA-1 — <rule>          <- one heading per rule

## AREA-2 — <rule>

---

## Open questions           <- optional, honest

## Related

- [other-doc](other-doc.md)
```

**Metadata header is a bullet list, always.** Prettier joins consecutive lines that fit inside 120 columns, so a header
written as three bold lines collapses into one run-on line. List items survive.

Rule headings are stable IDs. **Never renumber.** Superseded rule keeps its number, gets a `**Superseded by AREA-9.**`
line.

---

## DOC-7 — Code blocks stay normal

Caveman style applies to prose only.

Inside fenced blocks: normal code, normal comments, real identifiers, compiling syntax. No compression, no fragments, no
dropped articles in code comments.

```go
// Cards are copied and LKI-snapshotted, so pointer identity is unreliable.
// Compare by CardID instead.
func (g *Game) Card(id CardID) *Card { ... }
```

---

## DOC-8 — Show bad before good

Rule sticks when reader sees the failure mode.

Format:

**Bad:**

```go
var cardDB *carddb.DB // package-level mutable state
```

**Good:**

```go
type Game struct {
    db *carddb.DB // injected, immutable
}
```

Bad example must be realistic — code someone would actually write, ideally the direct Java translation.

---

## DOC-9 — Expand where compression is dangerous

Drop caveman style, write full sentences, for:

| Case                                        | Why                               |
| ------------------------------------------- | --------------------------------- |
| Destructive/irreversible steps              | Misread costs data                |
| Ordered multi-step procedures               | Fragment order reads as ambiguous |
| Security or licensing constraints           | Precision is the whole point      |
| Anything with a legal or correctness gotcha | Ambiguity = defect                |

Resume compressed style after the risky part.

---

## DOC-10 — Length limits

| Unit                             | Limit      | Action if over                                  |
| -------------------------------- | ---------- | ----------------------------------------------- |
| Prose run before table/list/code | ~15 lines  | Break it up                                     |
| Single doc                       | ~400 lines | Split by topic, link with a relative link       |
| Rule body                        | ~20 lines  | Rule is doing two things — split into two rules |

---

## DOC-11 — Link, do not repeat

Fact lives in exactly one doc. Others link.

Use relative Markdown links: `[03-testing-standards](03-testing-standards.md)`. Repo-relative paths for code.

Reason: relative links resolve on GitHub, in IDEs, and in any Markdown viewer. Wiki-style `[[name]]` renders as literal
brackets outside Obsidian.

Duplicated fact = two facts that will disagree in three months.

---

## DOC-12 — Docs land before code

New Go package → entry in `architecture/module-map.md` in the **same** commit. New ADR-worthy decision → ADR merged
**before** the implementing PR. Ported Java unit → `porting/port-log/<unit>.md` note before merge.

CI enforces it. `crucible/tools/docgate` fails the build on a package with no module-map row, on a ported package whose
row links to no port-log note, and on an `ADR-nnnn` reference in Go code with no matching ADR file.

Reason: doc-after-code never happens.

---

## DOC-13 — Status honesty

Every doc header carries `Status:`.

- `Draft` — not binding, may be wrong
- `Active` — binding, cite it in review
- `Superseded` — first line says what replaced it

Never leave `Draft` on something the team already follows. Never leave `Active` on something wrong.

---

## DOC-14 — Prettier formats every Markdown file

Config: `/.prettierrc` — `printWidth: 120`, `proseWrap: "always"` for `*.md`. Pinned version: **prettier 3.8.3**
(ADR-0002). A local install at a different version is fine; CI is the authority.

```bash
prettier --write .      # respects .prettierignore
prettier --check .      # CI gate
```

`/.prettierignore` excludes all upstream Forge files. Only `CLAUDE.md` and `docs/crucible/**` are formatted. Reason:
reformatting upstream `docs/*.md` produces guaranteed rebase conflicts on the next sync (REV-1).

What prettier decides, stop arguing about:

| Concern                          | Prettier owns it        |
| -------------------------------- | ----------------------- |
| Table pipe alignment             | Yes — do not hand-align |
| Prose wrap column                | Yes — 120, hard-wrapped |
| List marker, indent, blank lines | Yes                     |
| Heading and fence style          | Yes                     |

What prettier does **not** touch, so it stays your job: word choice, rule IDs, table column order, whether a thing
should be a table at all (DOC-5).

Run before every commit. CI blocks on `prettier --check`.

---

## DOC-15 — markdownlint catches what Prettier cannot

Prettier formats. markdownlint checks semantics. Both run; neither replaces the other.

```bash
npx markdownlint-cli2 "CLAUDE.md" "docs/crucible/**/*.md"
```

Config: `/.markdownlint-cli2.jsonc`. The VSCode extension `DavidAnson.vscode-markdownlint` reads the same file, so
editor squiggles and CI agree. Pinned version: **markdownlint-cli2 0.23.2** (ADR-0002).

Two categories are disabled there, each with its reason in a comment:

| Category                   | Examples                                                          | Why off                                                                                              |
| -------------------------- | ----------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Prettier already owns it   | `MD004` list style, `MD013` line length, `MD047` trailing newline | Warning that `prettier --write` immediately re-introduces. Prettier wins (DOC-14)                    |
| Deliberate convention here | `MD025` single H1, `MD029` list numbering                         | Plan uses `# Phase n` as section separators; roadmap numbers steps continuously so they can be cited |

Everything else stays on and catches real defects: fenced blocks missing a language (`MD040`), bare URLs (`MD034`),
heading-level jumps (`MD001`), trailing punctuation in headings (`MD026`).

**Fence Markdown templates as `text`, not `markdown`** (DOC-7). Prettier formats embedded Markdown and will silently
rewrap the template you are trying to show — merging its `**Status:**` and `**Date:**` lines into one, for example.

---

## DOC-16 — Docs carry state, not history

Every document reads as if written today, in one pass, by someone who already knew the answer.

Banned: `Correction`, `Update`, `Changelog`, `Amendment` sections. Dated "as of" notes about the document itself.
Sentences like "this previously said", "originally we chose", "the old approach was". A superseded paragraph kept "for
context".

**Bad:**

```text
## Correction — 2026-09-07

The estimate above was low by 40%. A realistic figure is 50,000 to 65,000 lines.
```

**Good:**

```text
**Budget 50,000 to 65,000 lines in one package.**
```

Two exceptions, both about the subject rather than the document: a ledger whose whole purpose is a record
(`porting/upstream-patches.md`, `port-log/`), and a measurement dated because the thing measured moves — "corpus as of
2026-09-07" (ARCH-9).

Reason: a reader wants the current answer, and an amendment log makes them derive it by replaying edits. Git holds the
history, diffs it properly, and never disagrees with itself. Two statements of the same fact in one file is the failure
mode DOC-11 exists to prevent, and a correction section is exactly that.

---

## Checklist before merging a doc

- [ ] Header: Status + Applies to
- [ ] Every rule has an ID and a reason (DOC-4)
- [ ] Parallel items in tables, not prose (DOC-5)
- [ ] Identifiers, paths, numbers, error strings exact (DOC-2)
- [ ] Code blocks not compressed (DOC-7)
- [ ] At least one Bad/Good pair per non-obvious rule (DOC-8)
- [ ] Dangerous or ordered parts written out in full (DOC-9)
- [ ] No fact duplicated from another doc (DOC-11)
- [ ] No correction, update, or changelog section (DOC-16)
- [ ] `prettier --check .` clean (DOC-14)
- [ ] `markdownlint-cli2` clean (DOC-15)

## Related

- [01-go-coding-standards](01-go-coding-standards.md)
- [02-java-to-go-translation](02-java-to-go-translation.md)
- [03-testing-standards](03-testing-standards.md)
- [04-adr-process](04-adr-process.md)
