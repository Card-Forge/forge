# Meta Gauntlet

- **Status:** Active
- **Applies to:** the card corpus, and therefore the scope of M6
- **First format:** Modern
- **Measured:** 2026-09-07

Defines what Crucible plays against. [ADR-0011](../adr/0011-card-corpus-scoping.md) scopes the card corpus to the target
deck plus this gauntlet, so this document is what makes "done" checkable.

---

## Scope: all formats eventually, Modern first

The gauntlet is **format-parameterised**, not Modern-specific. Each format is a directory of decklists plus a manifest;
adding a format adds a directory and changes no code.

```text
docs/crucible/research/meta-gauntlet.md      this document — policy and structure
gauntlets/
  modern/
    gauntlet.toml                            format, source, snapshot date, deck weights
    <archetype>.dck                          one file per deck
  standard/  pioneer/  legacy/  pauper/      later, same shape
```

Modern is first because its meta barely moves, so a gauntlet stays valid for years and results do not age out between
one run and the next. The cost is that Modern leans on the mechanics that are hardest to port.

### What "all formats" does to ADR-0011

Worth stating plainly, because it changes how that ADR should be read.

| Corpus                       |    Cards | APIs required | Share of the 192 used corpus-wide |
| ---------------------------- | -------: | ------------: | --------------------------------: |
| A real ~15-deck gauntlet     | ~316–562 |         59–71 |                            31–37% |
| The entire Modern legal pool |   22,107 |       **148** |                           **77%** |
| All formats, in the limit    |  ~34,000 |           192 |                              100% |

```console
$ # Modern pool, resolved against card scripts
22107 card names, 22002 resolved to scripts (100%)
148 of 192 APIs
198 keywords
```

**ADR-0011 is a sequencing argument, not a permanent scope reduction.** Targeting all formats eventually means the
corpus approaches the whole card pool, and the port approaches complete. What the corpus gate buys is ordering and a
checkable definition of done _at each stage_ — not less total work in the limit.

That is the correct reading and it does not weaken the ADR. Without the gate there is no way to say M6 has finished;
with it, M6 finishes when Modern's gauntlet is covered, and each later format is an increment with its own measurable
cost.

---

## What a gauntlet is

A gauntlet is a set of decklists representing what a deck must beat to be considered good, plus the metadata needed to
interpret results.

| Field      | Purpose                                                                      |
| ---------- | ---------------------------------------------------------------------------- |
| `format`   | Which format's legality rules apply                                          |
| `snapshot` | The date the meta was sampled. Results are only comparable within a snapshot |
| `source`   | Where the decklists came from, with enough detail to re-derive them          |
| `weight`   | Optional share of the metagame per archetype                                 |

**Weights matter more than deck count.** An unweighted gauntlet reports the average matchup against a uniform field,
which no real tournament resembles. A weighted one reports expected win rate against the field as it actually is. Both
are useful; only one answers "should I play this deck".

## Sizing

8 to 15 archetypes. Beyond that, added decks are usually variants of ones already present and cost corpus surface
without adding information.

Measured against real decklists in this repository, a mixed 20-deck sample was 316 distinct cards needing 59 APIs, and
30 decks was 562 cards needing 71. A curated Modern gauntlet will sit in that range — an order of magnitude below the
format's full 22,107-card pool.

---

## Sourcing

**Not yet supplied.** The decklists themselves require an external source, and this document deliberately does not
invent them — a fabricated metagame would produce confident, wrong recommendations, which is the failure mode
[ADR-0011](../adr/0011-card-corpus-scoping.md) exists to prevent.

Requirements for whatever source is chosen:

- Recent competitive results, not casual or theorycrafted lists
- Enough events to be a metagame rather than one tournament
- Archetype labels, so decks can be weighted and matchups reported by name
- A date, recorded as `snapshot`

Candidate sources are public metagame trackers and tournament result aggregators. Whichever is used goes in
`gauntlet.toml` alongside the snapshot date, so a run's `manifest.json` identifies exactly what it was measured against.

Decks are stored in Forge's `.dck` format, which the engine already reads ([grammar](../porting/dsl/README.md),
`forge-gui/res` ships thousands of examples).

## Refresh cadence

| Format          | Cadence                                          | Why                                               |
| --------------- | ------------------------------------------------ | ------------------------------------------------- |
| Modern          | On each new set, plus after any ban announcement | Meta shifts are event-driven, not calendar-driven |
| Standard        | Quarterly, and at rotation                       | Rotation invalidates the gauntlet outright        |
| Pioneer, Legacy | Twice yearly                                     | Slow-moving                                       |

A refresh is a new snapshot, never an edit to an existing one. Results carry their snapshot, and results from different
snapshots are not comparable — the same rule `MetricsVersion` applies to metric definitions
([MET-1](../telemetry/metric-definitions.md)).

## Adding a format

1. Create `gauntlets/<format>/` with `gauntlet.toml` and decklists.
2. Run `crucible corpus-coverage --decks gauntlets/<format>/`.
3. The report lists every API, keyword, trigger, replacement, cost part and card property the engine lacks. That is the
   backlog for supporting the format.
4. Support is complete when the report is empty.

The order formats are added is a scheduling decision, made with that report in hand rather than in advance.

---

## Open questions

- **Decklists are not chosen yet.** Everything else here is structure. They need an external source: the `.dck` files in
  this repository are quest, adventure and AI-generated decks, not a competitive metagame. What they gate is M6's
  backlog — `crucible corpus-coverage --decks gauntlets/modern/` is what defines "done" for card support — not M1-M5,
  which need no gauntlet.
- Weights need a source too, and an unweighted first gauntlet is acceptable provided reports say so.
- Whether sideboards are simulated at all in M6, or deferred. Sideboarding needs `PlayerController` support for
  between-game decisions and is not in the plan's milestone list.

## Related

- [ADR-0011](../adr/0011-card-corpus-scoping.md) — corpus scoping, which this document parameterises
- [`../telemetry/metric-definitions.md`](../telemetry/metric-definitions.md) — MET-24, results split by play/draw
- [`../00-master-implementation-plan.md`](../00-master-implementation-plan.md) — M6, sized by this gauntlet
