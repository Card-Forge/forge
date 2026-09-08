# Crucible — Master Implementation Plan

**Automated MTG deck testing & optimization suite, built on a Go port of the Forge rules engine.**

- **Version:** 0.1
- **Status:** Draft
- **Owner:** `jc@archlab.pl`
- **Base repo:** fork of [Card-Forge/forge](https://github.com/Card-Forge/forge) @ `b05ee47a6fd`

---

## 0. Executive summary and one stated concern

Crucible runs batch simulations of a target deck against a meta gauntlet, captures per-game telemetry (dead cards, mana
health, per-card impact), and emits actionable deck-improvement reports. The engine will be Go, ported from Forge's Java
engine.

**Concern, stated once, then the plan proceeds as specified.** The Java surface to be ported is ~210,000 LOC across
`forge-core` + `forge-game` + `forge-ai`, driving the whole card corpus — roughly 34,000 scripts, 300,000 script lines —
and exposing 203 ability APIs, 203 keywords, 153 trigger types, 45 replacement types, and a ~2,100-line "valid string"
predicate matcher. A complete, card-for-card port is a multi-engineer-year effort. Two adjustments make it tractable
without violating either constraint:

1. **Scope the port by card corpus, not by feature completeness.** Crucible does not need every card in the corpus. It
   needs the target deck plus a gauntlet — realistically 400–900 distinct cards. The porting order is driven by a
   _coverage gate_ over that corpus (Section 3.2). Measured coverage curve (Section 1.5) shows the top 30 of 192 used
   APIs fully cover 78.5% of the entire card pool; a curated modern/standard corpus concentrates far harder than that.
2. **The Java engine stays as a test oracle, not as a runtime.** "No long-term hybrid" is satisfied: nothing Java is on
   Crucible's execution path. But Java remains a _CI dependency_ used to generate golden outputs for differential
   testing (Section 3.3). Deleting the oracle removes the only mechanism that proves rule accuracy. Keep it.

Everything below assumes those two adjustments. If either is rejected, Section 5's roadmap timing is invalid and needs
rework.

---

# Phase 1 — Codebase Mapping & Analysis

## 1.1 Module inventory (measured)

| Module                                       | Java files |     LOC | Port?                   | Notes                                                                                                                                 |
| -------------------------------------------- | ---------: | ------: | ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `forge-core`                                 |        152 |  26,385 | **Yes — first**         | Static card data, card-script reader, mana/color/type models, deck IO, collections                                                    |
| `forge-game`                                 |        807 | 126,676 | **Yes — second**        | The rules engine. Everything below hangs off it                                                                                       |
| `forge-ai`                                   |        191 |  56,815 | **Yes — third**         | `AiController`, `ComputerUtil*`, per-API `SpellAbilityAi`, lookahead simulator                                                        |
| `forge-gui`                                  |        441 |  77,033 | **Partial**             | Mostly UI-agnostic glue + resource bootstrap. Harvest `FModel`-equivalent load paths only                                             |
| `forge-gui-desktop`                          |        523 | 114,054 | **No** (harvest 1 file) | Swing. But `forge/view/SimulateMatch.java` is the existing headless batch entrypoint — read it as the reference for Crucible's runner |
| `forge-gui-mobile`                           |        367 | 100,889 | No                      | libGDX UI                                                                                                                             |
| `forge-gui-android` / `-ios` / `-mobile-dev` |         64 |   7,295 | No                      | Platform shells                                                                                                                       |
| `adventure-editor`                           |         39 |   5,989 | No                      | Adventure mode tooling                                                                                                                |
| `forge-lda`                                  |         23 |   1,933 | No                      | Latent Dirichlet deck analysis; interesting later, irrelevant now                                                                     |

Dependency direction is strictly one-way: `forge-core ← forge-game ← forge-ai ← forge-gui ← GUI shells`. Nothing in
`forge-game` imports `forge-ai`; the AI plugs in through the abstract `PlayerController`. **This is the single most
important structural fact for the port** — it means the Go engine can be built and tested with a scripted/replay
controller long before any AI exists.

Java target is 17 (`pom.xml:27`). Heavy Guava usage throughout (`FCollection`, `Multimap`, `Predicates`, `Lists`), plus
Apache Commons Lang.

## 1.2 `forge-game` internal weight (LOC by package)

| Package                           | Files |    LOC | Port priority                        |
| --------------------------------- | ----: | -----: | ------------------------------------ |
| `ability` (incl. 205 `effects/*`) |   215 | 32,650 | P5 — incremental, corpus-gated       |
| `card`                            |    42 | 22,632 | P3 — `Card.java` alone is 8,105 LOC  |
| `trigger`                         |   141 | 11,884 | P4                                   |
| `spellability`                    |    24 |  8,153 | P3/P4                                |
| `player`                          |    34 |  7,615 | P3                                   |
| `cost`                            |    52 |  7,916 | P2 (parser) + P4 (payment)           |
| `staticability`                   |    63 |  5,614 | P4 — continuous-effect layers        |
| `replacement`                     |    47 |  3,742 | P4                                   |
| `combat`                          |    10 |  3,119 | P4                                   |
| `keyword`                         |    38 |  2,479 | P3                                   |
| `phase`                           |     7 |  2,042 | P4 — `PhaseHandler` is the turn loop |
| `zone`                            |     8 |  1,735 | P3 + P4 (`MagicStack`, 1,019 LOC)    |
| `mana`                            |     6 |  1,386 | P4                                   |
| `event`                           |    63 |  1,512 | P3 — becomes the telemetry spine     |
| `mulligan`                        |     7 |    338 | P4                                   |

## 1.3 The core loops that must be ported first

Ranked by "nothing works until this works":

1. **`forge-game/src/main/java/forge/game/GameAction.java` (2,897 LOC)** — the beating heart. Owns `changeZone`/`moveTo`
   (every zone transition in the game funnels here), `checkStateEffects` (state-based actions + the continuous-effect
   layer application pass), destroy/sacrifice, and game-over detection. Every rules bug lives here or is visible from
   here.
2. **`forge/game/phase/PhaseHandler.java` (1,324 LOC)** — turn structure, step advance, the priority loop, `Untap` (264
   LOC), combat step sequencing.
3. **`forge/game/zone/MagicStack.java` (1,019 LOC)** — stack push/resolve, simultaneous-trigger ordering, replacement
   application at add-time.
4. **`forge/game/card/Card.java` (8,105 LOC) + `CardState.java` (1,136)** — the mutable card. Faces, characteristics,
   counters, keyword instances, attachments, damage, timestamps for layers, `Remembered`/`Imprinted` object lists.
5. **`forge/game/Game.java` (1,440) + `Match.java` (464)** — game/match lifecycle, player list, active-player rotation,
   `GameOutcome`.
6. **`forge/game/staticability/StaticAbilityContinuous.java` (1,096) + `StaticEffects.java`** — the layer system. Get
   this wrong and every P/T, type, and cost calculation is wrong.
7. **`forge/game/player/Player.java`** — life, zones, mana pool, land drops, `shuffle()` (`Player.java:1617`).

Notably **`PlayerController` (`forge/game/player/PlayerController.java:50`) has ~110 abstract methods.** Every point
where the game asks a decision goes through it. Port it as a Go interface _first_, with three implementations:
`ScriptedController` (fixture-driven), `ReplayController` (decision-log driven — see 3.3), and later `AIController`.

## 1.4 How Forge parses `.txt` card definitions — the full pipeline

Cards live at `forge-gui/res/cardsfolder/<letter>/<snake_case_name>.txt`, 29 subfolders, roughly 34,000 files.
`CardStorageReader` (`forge-core/src/main/java/forge/CardStorageReader.java`) walks the tree (or a `cardsfolder.zip`),
multi-threaded, and feeds each file to `CardRules.Reader`.

**Parsing is two-stage, and the split matters enormously for the Go design.**

### Stage 1 — load time, `forge-core`, static and immutable

`CardRules.Reader.parseLine` (`forge-core/src/main/java/forge/card/CardRules.java:691`) does a line-oriented `Key:Value`
parse into per-face `CardFace` objects, producing an immutable `CardRules` shared by every instance of that card across
every game. Measured key frequency across the corpus:

| Key                                                                                                                                               |  Count | Meaning                                                      |
| ------------------------------------------------------------------------------------------------------------------------------------------------- | -----: | ------------------------------------------------------------ |
| `SVar:`                                                                                                                                           | 59,405 | Named variable: sub-abilities, numeric expressions, AI hints |
| `Types:`                                                                                                                                          | 34,627 | Type line                                                    |
| `Oracle:`                                                                                                                                         | 34,627 | Rules text (display only)                                    |
| `ManaCost:`                                                                                                                                       | 34,626 | e.g. `1 U`, `no cost`                                        |
| `Name:`                                                                                                                                           | 34,618 |                                                              |
| `PT:`                                                                                                                                             | 19,198 | `2/1`                                                        |
| `A:`                                                                                                                                              | 18,449 | Ability (spell or activated)                                 |
| `K:`                                                                                                                                              | 18,245 | Keyword, optionally parameterized (`Dash:1 R`)               |
| `T:`                                                                                                                                              | 16,971 | Triggered ability                                            |
| `DeckHas:` / `DeckHints:` / `DeckNeeds:` / `DeckRule:`                                                                                            | 13,590 | Deckbuilder metadata                                         |
| `S:`                                                                                                                                              |  7,093 | Static ability                                               |
| `AI:`                                                                                                                                             |  4,952 | AI hints (e.g. `RemoveDeck:All`)                             |
| `R:`                                                                                                                                              |  1,693 | Replacement effect                                           |
| `AlternateMode:`                                                                                                                                  |    902 | Split / transform / adventure / meld / specialize            |
| `Colors:`, `Loyalty:`, `Defense:`, `Variant:`, `SPECIALIZE:`, `MeldPair:`, `CopyFaceFrom:`, `HandLifeModifier:`, `Text:`, `Draft:`, `SETCOLORID:` | <2,000 | Long tail — all must be handled, most are trivial            |

**Critical:** at stage 1 the `A:`, `T:`, `S:`, `R:`, and `SVar:` values are stored as **raw strings**. No ability
objects exist yet. Multi-face cards switch face via `AlternateMode:` and continue parsing into `faces[curFace]`.

### Stage 2 — game time, `forge-game`, mutable

`CardFactory.getCard(PaperCard, owner, game)` builds a mutable `Card`; `CardFactoryUtil.setupKeywordedAbilities` and
`AbilityFactory` then interpret the stored strings.

`AbilityFactory.getAbility(abString, CardState)`
(`forge-game/src/main/java/forge/game/ability/AbilityFactory.java:114`):

1. Split the string on `|` into `Key$ Value` pairs → `Map<String,String>`.
2. First key selects the record type and API: `SP$` = Spell, `AB$` = activated ability, `DB$` = sub-ability (drawback),
   `ST$` = static, `RE$` = replacement.
3. API name string → `ApiType` enum → effect class, resolved **by reflection** (`ApiType.java` maps 203 constants to 205
   `effects/*Effect` classes).
4. `Cost$` → `Cost` parser (52 files in `cost/`).
5. `ValidTgts$` → `TargetRestrictions`.
6. `SubAbility$ <svarName>` → recursive `getAbility` on the named SVar. This is how chains like Ragavan's
   `TrigTreasure → TrigExile → DBEffect → DBCleanup` are built.

### The five embedded sub-DSLs (each needs its own Go parser)

1. **Param map** — `Key$ Value | Key$ Value`. Trivial split, but the key _vocabulary_ is large and undocumented;
   enumerate it from the corpus (Section 3.2, M2 gate).
2. **SVar system** — a per-card namespace holding sub-ability strings, `Count$` expressions, and AI hints. Resolution is
   lexically scoped to the card state and can be inherited by copies.
3. **Valid strings** — `Creature.Green+attacking+YouCtrl` / `Instant.YouCtrl,Sorcery.YouCtrl`. Grammar:
   `TypeOrName [ . Prop (+ Prop)* ] [ , alternative ]*`, where `,` is OR and `+` is AND. Implemented by
   `CardProperty.java` (**2,135 LOC of predicate cases**) and `PlayerProperty.java` (517). This is the single largest
   and most error-prone port unit outside `GameAction`.
4. **X-expressions / counts** — `AbilityUtils.calculateAmount` and friends (**3,950 LOC**). Handles
   `Count$CardsInYourHand`, `SVar$X`, `Targeted$CardPower`, `Remembered$Amount`, plus arithmetic suffixes (`.Plus1`,
   `.Twice`, `.NMinus`, `.LimitMax3`).
5. **Cost strings** — `Cost$ 2 R T Sac<1/Creature> Discard<1/Card>`: mana symbols, tap/untap, sacrifice, discard, exile,
   pay-life, counter removal, and ~45 more cost part types.

### Design consequence for Go

> **Refined by [ADR-0007](adr/0007-card-dsl-representation.md).** The compile-once claim below is right about
> definitions and wrong about SVars: `forge-game` has 144 `setSVar`/`removeSVar` call sites, so SVars are also a runtime
> scratchpad. Definitions compile once and are shared; written values live in a per-card overlay.

Java re-interprets these strings **at runtime, per card instance, per game**. For a suite running 100k+ games this is
pure waste. **Crucible must compile scripts once at load into a typed, immutable AST** (ADR-0007), shared read-only
across all goroutines. Per-game `Card` objects hold only mutable state plus a pointer to the shared compiled definition.
This is both the largest performance win of the port and the biggest structural divergence from Java — flag it
prominently in the translation rules so nobody "faithfully" ports the string interpretation.

## 1.5 Coverage curve — the lever that makes this feasible

Measured across the whole corpus (192 distinct APIs actually used out of 203 defined):

| Ability instances |       | Cards **fully** covered |                      |
| ----------------- | ----- | ----------------------- | -------------------- |
| top 10 APIs       | 58.6% | top 10 APIs             | 50.2% (16,913 cards) |
| top 20 APIs       | 77.0% | top 20 APIs             | 69.6% (23,446)       |
| top 30 APIs       | 84.7% | top 30 APIs             | 78.5% (26,439)       |
| top 50 APIs       | 92.3% | top 50 APIs             | 88.6% (29,849)       |
| top 75 APIs       | 96.4% | top 75 APIs             | 94.2% (31,738)       |
| top 100 APIs      | 98.3% | top 100 APIs            | 97.3% (32,774)       |

4,495 cards (13.3%) have zero scripted abilities — vanilla or keyword-only.

Top APIs by instance count: `ChangeZone` (6,616), `Pump` (4,989), `Draw` (3,733), `Token` (3,560), `PutCounter` (3,293),
`Cleanup` (2,989), `DealDamage` (2,859), `Mana` (2,503), `Effect` (1,913), `GainLife` (1,777), `Destroy` (1,546), `Tap`
(1,471), `LoseLife` (1,214), `Discard` (1,100), `PumpAll` (1,048), `Animate` (1,025), `Dig` (989), `Sacrifice` (892),
`Charm` (793).

**Use this as the work-ordering function, intersected with the actual Crucible card corpus.** A tool
(`crucible corpus-coverage`) must exist from day one that takes a set of decklists and reports exactly which APIs,
keywords, triggers, costs, and card properties are required — and which are missing from the Go engine. That report is
the backlog.

## 1.6 Reusable assets already in the repo

- **`forge/game/GameState.java` (1,433 LOC)** — a text-format game-state serializer/deserializer used by dev mode and AI
  tests. Keys: `p1life`, `p1battlefield`, `p1hand`, `activeplayer`, `activephase`, `turn`, `removesummoningsickness`,
  etc. (`GameState.java:48-546`). **Adopt this format verbatim as Crucible's cross-engine scenario fixture format.** It
  is the cheapest possible bridge for differential testing.
- **`forge/view/SimulateMatch.java:39`** — existing headless CLI batch simulator. Read it as the functional spec for
  `crucible run`.
- **`forge-ai/.../simulation/`** — `GameCopier`, `GameSimulator`, `GameStateEvaluator`, `SpellAbilityPicker`, `Plan`.
  Forge's existing lookahead AI. `GameCopier` is slow in Java; in Go a value-oriented state clone is dramatically
  cheaper, which is a real quality upgrade for Crucible's AI, not just a speedup.
- **`forge/util/MyRandom.java:62` — `setRandom(Random)` exists.** Determinism is already supported upstream. Shuffles
  use `Collections.shuffle(list, MyRandom.getRandom())` (`Player.java:1617`).
- **`forge-gui/res/ai/*.ai`** — `Default`, `Cautious`, `Reckless`, `Experimental` AI profiles (tunable property files).
  Port the property schema, not just one profile.
- **`docs/File-Formats.md`** — upstream's own (incomplete) format notes. Starting point, not a spec.

---

# Phase 2 — Documentation & Architecture (executed BEFORE any Go code)

## 2.1 Where things live (and why)

Two reserved paths — `crucible/` for Go, `docs/crucible/` for documentation — and no edits outside them, because every
line touched elsewhere is a conflict on the next upstream merge. Decided in
[ADR-0001](adr/0001-fork-layout-and-upstream-sync.md), enforced by `REV-1`, and the exceptions are logged in
[`porting/upstream-patches.md`](porting/upstream-patches.md).

## 2.2 `/docs/crucible/` structure

[`README.md`](README.md) is the index and carries the directory table. Documents are written in the compressed style
[`guidelines/00-documentation-style.md`](guidelines/00-documentation-style.md) defines, and rules carry stable IDs
(`GO-2`, `TEST-1`, `DOC-16`) so review comments, code comments and commit bodies can cite them.

Doc-before-code is enforced by `crucible/tools/docgate` rather than by habit: a package with no module-map row, a ported
package with no port-log note, or an `ADR-nnnn` citation with no ADR file all fail the build (DOC-12, PORT-4, ADRP-4).

## 2.3 The ADR set, written before any Go

Fifteen ADRs, all `Accepted` before the first line of engine code, each in MADR format. The index, with the question
each one settles, is [`adr/README.md`](adr/README.md).

**Near-term subjects, deliberately unnumbered:**

Numbers are allocated when an ADR is written, never reserved in advance. Reserving them locks a subject to a number
decided before anyone knew what the ADR would say, and leaves a permanent hole if the subject turns out unnecessary —
ADRP-3 forbids reuse. Each subject below takes the next free number on the day it is written.

| Subject                                                                  | Needed before       |
| ------------------------------------------------------------------------ | ------------------- |
| Error handling & panic policy — engine invariants vs. recoverable errors | M5                  |
| AI port strategy & parity tolerance — statistical, not bit-exact         | M7                  |
| Causal attribution methodology for per-card impact metrics               | M9                  |
| Reporting output formats & CLI UX                                        | M9                  |
| Observability, logging, and profiling                                    | M8                  |
| Oracle-data licensing & redistribution posture (repo is GPLv3)           | before distribution |

## 2.4 Java → Go translation rules

Normative in exactly one place: [`guidelines/02-java-to-go-translation.md`](guidelines/02-java-to-go-translation.md)
(`PORT-1` to `PORT-7`), with the decision behind it in [ADR-0004](adr/0004-java-to-go-translation-patterns.md) and the
Go rules it leans on in [`guidelines/01-go-coding-standards.md`](guidelines/01-go-coding-standards.md) (`GO-1` to
`GO-16`).

The stance, in one line: **port behaviour, not structure.** Java's shape came from Java's constraints — reflection
dispatch, checked exceptions, mutable singletons, 8,000 lines of accessors — and none of them exist in Go.

---

# Phase 3 — Golang Porting Strategy

## 3.1 Go project layout

[ADR-0003](adr/0003-go-project-layout.md) decides it: an engine core sized by what must be mutually recursive, with
acyclic satellites around it — not a mirror of Java's packages, because `forge-game` has 82 direct two-package import
cycles and Go forbids them. [`architecture/module-map.md`](architecture/module-map.md) tracks which packages exist
today.

Paths the module carries that are not packages, and so appear in neither document:

| Path                  | Contents                                                                |
| --------------------- | ----------------------------------------------------------------------- |
| `testdata/scenarios/` | `GameState`-format fixtures, shared with the Java oracle (TEST-5)       |
| `testdata/golden/`    | Golden AST dumps, event streams, decision logs                          |
| `tools/gen/`          | `go:generate` sources — effect registry, typed param structs (ADR-0008) |
| `oracle-java/`        | Maven module: dumpers and recorders against Forge (ADR-0010)            |

## 3.2 Porting sequence — exact order, with exit gates

Each stage is independently verifiable and ships a usable artifact. **Do not start a stage until the previous stage's
gate is green.**

### P0 — Foundation

Repo scaffolding, `go.mod`, lint, CI, `pkg/collect`, `pkg/javarand`, `internal/mana`, `internal/cardtype`, ID types.
**Gate:** `javarand` reproduces every draw kind Forge uses — `nextInt()`, `nextInt(n)`, `nextLong`, `nextBoolean`,
`nextDouble`, `nextFloat`, `nextGaussian`, `Collections.shuffle` — against a golden emitted by a real JVM
(`crucible/oracle-java` `RandomDumper`, 1,199 records). Kind coverage is the gate, not draw count: the generator is an
LCG, so a sequence that matches at draw 1,000 matches at draw 10⁶ by construction, while an unexercised draw kind can be
wrong forever. `ManaCost` and type-line parsers round-trip every distinct value in the corpus.

### P1 — Static card database (`forge-core` port)

Port `CardStorageReader` + `CardRules.Reader` → `internal/carddb`. All top-level keys, all faces, all `AlternateMode`
variants, deck metadata keys. **Gate — the highest-value cheap gate in the whole project:** a Java dumper
(`oracle-java`) emits normalized JSON for every `CardRules` in the corpus; Go emits the same; **the diff must be
empty.** 100% corpus coverage, fully deterministic, catches parser divergence before it can hide behind rules bugs.

### P2 — DSL front-end (compile to typed AST)

Param-map splitter, SVar namespace resolution, `SubAbility$` recursion, cost strings, Valid strings, Count/X
expressions, keyword strings. **Gates:**

- **Vocabulary completeness:** a corpus scan reports zero unknown top-level keys, zero unknown `Key$` param names, zero
  unknown card/player properties, zero unknown cost parts, zero unknown `Count$` heads. Unknowns are a hard build
  failure; the allowlist of deliberately-unsupported items lives in `porting/parity-matrix.md`.
- **Golden AST:** compiled AST for every card serializes to a stable JSON golden file, reviewed once, then diff-gated
  forever.

### P3 — Core state model (no rules yet)

`Game`, `Player`, `Card`/`State`, zones, counters, keyword instances, mana pool, the `PlayerController` interface, the
event bus, and `GameState` fixture load/dump. **Gate:** every scenario fixture in `testdata/scenarios/` loads into Go
and dumps back to a byte-identical `GameState` text blob; Java loads and dumps the same fixtures identically.

### P4 — Rules kernel

Turn/phase/step loop, priority, stack + simultaneous trigger ordering, state-based actions, zone-change machinery, the
continuous-effect **layer system**, replacement effects, triggers, combat
(declare/order/damage/first-strike/trample/deathtouch), mana payment, mulligans. **Gate:** scenario-parity harness (3.3
layer 2) green on a growing fixture suite; ≥300 hand-written scenarios covering every step transition, every layer,
every SBA.

### P5 — Effects, corpus-gated

Implement `ApiType` effects in this order: (a) the ~30 APIs needed by the _Crucible corpus_, (b) top-30 global, (c)
top-50, (d) whatever the corpus coverage tool still reports missing. Same for keywords, triggers, replacements, cost
parts. **Gate:** `crucible corpus-coverage --decks meta/` reports 100% supported for the active gauntlet; scenario
parity green per API (minimum 3 scenarios per API: normal, edge, interaction).

### P6 — AI

Two tiers, in order:

1. **Rule-based:** `AiController`, `ComputerUtilMana` (1,737 LOC — mana payment planning; port early, the runner needs
   it), `ComputerUtilCombat` (2,617), `ComputerUtilCard`, `CreatureEvaluator`, per-API `SpellAbilityAi` classes, `.ai`
   profile loading.
2. **Lookahead:** `GameCopier`/`GameSimulator`/`GameStateEvaluator`/`SpellAbilityPicker`. Go's cheap state clone
   (ADR-0009) makes deeper search practical than in Java. **Gate:** statistical parity (3.3 layer 4), not bit-exact.

### P7 — Simulation runner + telemetry

Worker pool, deck loading (`.dck`), gauntlet config, seed management, event recording, aggregation, storage. **Gate:**
100k games on the reference gauntlet complete with zero panics, zero hangs (turn cap enforced), deterministic re-run
from seed produces byte-identical telemetry.

### P8 — Reporting

Metric computation, causal attribution modes, report rendering, CLI. **Gate:** reports for a known-good and a known-bad
deck produce the expected qualitative diagnosis on a blind test set.

## 3.3 Verifying the Go port against Java — the differential harness

Four layers, each removing a class of divergence. All Java-side tooling is additive and lives in
`crucible/oracle-java/`.

### Layer 1 — Static parity (P1–P2) — total, deterministic, cheap

Java `CardRulesDumper` and `CardAstDumper` emit canonical JSON for every card. Go emits the same. CI diffs them. Any
parser divergence — a missed key, a mis-split param, a wrong SVar resolution — is caught here across 100% of the corpus,
before any game is ever simulated. **Build this on day one of P1; it pays for itself immediately.**

### Layer 2 — Scenario parity (P3–P5) — targeted, deterministic

Reuse Forge's own `GameState` text format (`forge-game/src/main/java/forge/game/GameState.java:48`) as the shared
fixture format. A scenario is:

```text
setup.state   # initial GameState text
actions.log   # ordered, explicit decisions (no AI involved)
expected/     # post-state dump + canonical event stream (generated from Java)
```

Both engines run the fixture with a **scripted controller**; compare (a) the final `GameState` dump and (b) the
normalized event stream. Because no AI is involved, **any difference is a rules bug** — the harness never produces
ambiguous results.

### Layer 3 — Full-game replay parity (P4–P6) — the decisive technique

The problem: AI divergence swamps rules divergence in full games. The fix: **eliminate the AI from the comparison.**

1. Run a full Java game with a `RecordingPlayerController` that wraps the AI and logs **every** decision it returns, as
   a stable identifier (ability ID, target IDs, ordering indices) — one line per decision.
2. Export: the deck lists, the RNG seed, and the decision log.
3. Replay in Go with `ReplayController`, which answers each `PlayerController` query by consuming the next logged
   decision and asserting the _option set offered_ matches Java's.
4. Diff the full normalized event stream, turn by turn.

This converts a stochastic system into a deterministic one. Two failure modes are both diagnostic: a diverging event
stream is a rules bug; a mismatched option set is a legality bug (Go offered a different set of legal plays). The
option-set assertion is the more valuable of the two — it catches "Go thinks this is castable and Java doesn't"
directly.

Requires `pkg/javarand` for identical shuffles (P0 gate) and `MyRandom.setRandom` on the Java side (already present,
`MyRandom.java:62`).

Corpus: replay logs generated nightly from thousands of Java games across the gauntlet, stored as a growing regression
suite.

### Layer 4 — Statistical parity (P6) — for the AI, where bit-exactness is meaningless

Run N=10,000 games per matchup, Java vs. Go, same decks, same seed stream. Compare with confidence intervals:

- win rate (target: within ±1.5pp, i.e. inside sampling noise)
- mean/median game length in turns
- mulligan rate distribution
- mean cards cast per turn, mana spent per turn curve
- distribution of game-ending conditions

A drift in any of these is an AI-behaviour regression even when the rules are correct. Track them as a dashboard, not a
pass/fail gate.

### Cross-cutting: fuzz & soak

Random legal decks drawn from the supported corpus, run to completion in Go only. Assertions: no panics, no infinite
loops (hard turn cap), no illegal states (a `validate()` pass over the game state after every priority round in debug
builds), no memory growth across 10⁵ games.

## 3.4 What the Java side already tests — and what it doesn't

Measured: **roughly 490 `@Test` methods across ~120 files, ~26,000 LOC.** Framework is **TestNG** (not JUnit), with
Mockito and PowerMock for the card-DB tests.

**Placement is an accident of module layout, not intent.** Only 2 test files (3 tests) live in `forge-game` —
`ManaCostBeingPaidTest` (convoke payment) and `AbilityKeyTest`. Everything else sits in
`forge-gui-desktop/src/test/java`, because that is the only module where the full runtime (card DB + `FModel` + AI) is
assembled. Consequence: `mvn test` needs a display, and CI runs it under Xvfb.

**CI does run them.** `.github/workflows/test-build.yaml` runs `mvn -U -B clean test` on every push and PR, matrixed
over Java 17 and 21, inside a virtual framebuffer. The release/snapshot workflows (`maven-publish`, `snapshots-*`,
`publish-android`) all pass `-DskipTests` / `-Dmaven.test.skip=true` — so tests gate PRs, not releases.

### Test families, ranked by value to the port

| Family                       | Files | Tests | Value to Crucible                                                                                                                                                                                                                                                                                           |
| ---------------------------- | ----: | ----: | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `forge/ai/simulation/`       |     7 |   137 | **Highest.** `GameSimulationTest` (2,856 LOC, 73 tests) is real rules assertion — static abilities, layer interactions, equipment/aura grants, monstrous, triggers, combat. Plus `SpellAbilityPickerSimulationTest` (34), `GameStateEvaluatorTest`, `OnePlaySafetyCheckerTest`, `TerminalScoreDiscountTest` |
| `forge/card/`                |     8 |    70 | **High.** `CardDbCardMockTestCase` alone is 54 tests over card-DB lookup, editions, request strings, lazy loading. Directly portable to Crucible P1/P2                                                                                                                                                      |
| `forge/deck/`                |     5 |    91 | Deck construction/generation. Relevant at P7                                                                                                                                                                                                                                                                |
| `forge/ai/ability/`          |    13 |    38 | Per-API AI behaviour (`DamageDealAiTest`, `ChangeZoneAiTest`, `CountersProliferateAiTest`, `UnlockDoorAiTest`, …). The template for Go's per-effect test layout                                                                                                                                             |
| `forge/gamesimulationtests/` |    31 |    15 | **Right shape, wrong medium, almost empty.** A declarative builder DSL keyed to Comprehensive Rules numbers — but only sections 103 and 104 are covered                                                                                                                                                     |
| `forge/net/`                 |    18 |    16 | Network play harness. Irrelevant to Crucible                                                                                                                                                                                                                                                                |
| everything else              |    36 |    89 | Boosters, draft, conquest, GUI smoke, `FCollection`                                                                                                                                                                                                                                                         |

The `gamesimulationtests` DSL is worth reading before designing Crucible's fixtures — it is the correct idea:

```java
// ComprehensiveRulesSection103.test_103_7a_first_player_skips_draw_step_of_first_turn
new GameWrapper(
    new GameStateSpecificationBuilder()
        .addCard(new CardSpecificationBuilder("Plains").owner(PLAYER_1).library())
        .build(),
    new PlayerActions(
        new CardAssertAction(new CardSpecificationBuilder("Plains").owner(PLAYER_1).library())
            .when(new ActionPreCondition().turn(2).phase(PhaseType.END_OF_TURN)),
        new EndTestAction(PLAYER_1)))
```

Naming tests after CR rule numbers is exactly right. Encoding them as Java builder calls is not — every new case costs a
compile, and the fixtures cannot be shared with a second engine.

### Honest gap assessment

Fewer than five hundred tests for a 210,000-line engine driving tens of thousands of card scripts is thin, and it is
concentrated in _AI simulation_ rather than _rules_. There is no per-card regression suite, no `src/test/resources`
fixture directory anywhere in the repo, and no coverage of the vast majority of the 203 APIs, 153 trigger types, or the
layer system's ordering cases.

**So the Java suite is an acceptance floor, not a specification.** It is genuinely useful — hundreds of free,
already-debugged assertions about real card behaviour — but passing all of it proves far less than it sounds. The
differential harness in 3.3 remains the primary correctness mechanism; the ported tests are a fast inner loop that runs
in seconds without a JVM.

## 3.5 Go test architecture

Six layers, module-first, fixtures as data rather than Go functions. Normative in
[`guidelines/03-testing-standards.md`](guidelines/03-testing-standards.md) (`TEST-1` to `TEST-14`); `TEST-13` is the
layer table and `TEST-5` the fixture format.

What belongs here rather than there is when each layer has to exist:

| Milestone | Test work that is part of its exit gate                                     |
| --------- | --------------------------------------------------------------------------- |
| M1        | L1 unit tests + `javarand` parity; CI running `go test -race`               |
| M2        | L2 corpus golden diff; port the 70 `forge/card` tests                       |
| M3        | L1 tests for every sub-DSL; L5 fuzz targets on all parsers                  |
| M4        | L3 harness itself, plus `GameState` round-trip fixtures                     |
| M5        | ≥300 rules fixtures; test-port matrix green for rules-relevant Java tests   |
| M6        | ≥3 fixtures per implemented API; L4 replay parity live                      |
| M7        | Port the 137 `ai/simulation` + 38 `ai/ability` tests; statistical dashboard |
| M8        | L5 soak at 10⁵ games; determinism re-run test                               |

Porting an inherited Java test means converting it into a fixture directory, not into a Go test function. One row per
test in [`porting/test-port-matrix.md`](porting/test-port-matrix.md), which doubles as M5's exit gate.

---

# Phase 4 — Telemetry & Game Event Design (Go)

The engine **emits, never stores**: it writes flat events into a per-game buffer and knows nothing about what records
them. Everything this phase used to specify has since been decided or defined elsewhere, and is normative there.

| Subject                                                          | Owner                                                                                      |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Event struct, recording levels, schema versioning                | [ADR-0013](adr/0013-telemetry-event-bus.md)                                                |
| Shard format, and why DuckDB is invoked rather than imported     | [ADR-0014](adr/0014-telemetry-storage-format.md)                                           |
| Why v1 reports without DuckDB at all                             | [ADR-0016](adr/0016-reporting-without-duckdb.md)                                           |
| Dead cards, castability probe, mana health, per-card impact      | [`telemetry/metric-definitions.md`](telemetry/metric-definitions.md) — `MET-1` to `MET-25` |
| How an engine event becomes a figure in a report, stage by stage | [`design/telemetry-pipeline.md`](design/telemetry-pipeline.md)                             |

The one cross-milestone dependency worth repeating here, because it decides M5's scope: **the castability probe asks the
rules engine a question, and only the engine can answer it.** M5 ships that support for a consumer that does not exist
until M8 (`MET-5`, `MET-6`).

---

# Phase 5 — Step-by-Step Execution Roadmap

Estimates are engineering-weeks for one focused engineer, and are ranges because the effect long-tail (P5) is genuinely
open-ended. Sequence is firm; durations are not.

## Milestones

### M0 — Documentation & architecture foundation _(no Go code)_ — 1.5-2.5 wks

**Complete.**

| #   | Item                                                                             | State                                                                                                                                                                           |
| --- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `docs/crucible/` structure, `README.md`, `adr/README.md`                         | Done                                                                                                                                                                            |
| 2   | **ADR-0001 … ADR-0014**                                                          | Done — all fourteen `Accepted`. The gate asked for 0001-0011; 0012-0014 followed as ports-and-adapters and telemetry came into scope                                            |
| 3   | `guidelines/` — `DOC-n`, `GO-n`, `PORT-n`, `TEST-n`, `ADRP-n`, `REV-n`, `ARCH-n` | Done — seven documents plus index                                                                                                                                               |
| 4   | `/CLAUDE.md`                                                                     | Done — inlines the ten non-negotiables and the module-first testing rule                                                                                                        |
| 5   | `architecture/` and `design/`                                                    | Done — `system-overview` and `module-map` describe what exists; four design documents (ARCH-10) hold the target shape. `engine-state-model` omitted, covered by `engine-design` |
| 6   | DSL grammars under `porting/dsl/`                                                | Done — six grammars derived from the corpus                                                                                                                                     |
| 7   | `telemetry/metric-definitions.md`                                                | Done — 21 numbered definitions, versioned                                                                                                                                       |
| 8   | `research/meta-gauntlet.md`                                                      | Done — format, sources, refresh cadence, resulting corpus. The Modern decklists themselves need an external source and gate M6's backlog, not M0                                |

### M1 — Go foundation & the oracle harness skeleton — 1.5-2 wks

**Complete.**

| #   | Item                                                                    | State                                                               |
| --- | ----------------------------------------------------------------------- | ------------------------------------------------------------------- |
| 9   | `crucible/` module, CI (build/lint/test), `pkg/collect`, `pkg/javarand` | Done                                                                |
| 10  | `oracle-java/` Maven module wired into the build; first dumper          | Done — `RandomDumper`, standalone POM (ADR-0002)                    |
| 11  | `tools/javacycles`, `tools/enginelint`                                  | Done — both premises of ADR-0003 now machine-checked                |
| 12  | `internal/mana`, `internal/cardtype`                                    | Done — both corpus round-trips green, goldens committed             |
| 13  | `tools/docgate`                                                         | Done — DOC-12, PORT-4 and ADRP-4 now fail the build, not the review |

**Exit gate:** P0 gate (Section 3.2), green. `javarand` reproduces every draw kind over the 1,199-record golden; all 858
distinct `ManaCost` values and all 3,908 distinct `Types` values in the corpus parse and round-trip through their
printed form.

### M2 — Card script parser & static DB (foundation port) — 3–4 wks

**In progress.**

14. `internal/carddb` — top-level `Key:Value` parser, all faces, all variants. **Done**: every script in the corpus
    parses, every `CopyFaceFrom:` placeholder resolves, and the shape of the result is pinned by a summary golden.
15. `CardRulesDumper` in Java; canonical-JSON dumper in Go.
16. Deck (`.dck`) loading; `crucible corpus-coverage` first version. **Exit gate:** P1 gate — empty diff across the
    whole corpus.

### M3 — DSL compilation to typed AST — 3–5 wks

17. `internal/carddb/compile`: param maps, SVar resolution + `SubAbility$` recursion, cost strings, valid strings,
    count/X expressions, keyword strings.
18. `go:generate` pipeline for typed param structs + the effect registry (ADR-0008).
19. Vocabulary-completeness scanner (hard-fails on unknown keys/props/cost parts).
20. `porting/parity-matrix.md` generated from the registry. **Exit gate:** P2 gates — zero unknowns outside the explicit
    allowlist; golden AST diff clean.

### M4 — Core state model + controller interface + event bus — 2–3 wks

21. `internal/engine/{game,card,player,zone,event,control}`; the ~110-method `PlayerController` interface with
    `ScriptedController`.
22. `GameState` fixture load/dump in Go (byte-identical to Java's).
23. Event schema v1 implemented per ADR-0013. **Exit gate:** P3 gate — fixture round-trip parity.

### M5 — Rules kernel — 6–10 wks _(the largest single risk)_

24. Turn/phase/step loop + priority (`PhaseHandler` port).
25. Zone changes + state-based actions + game-over (`GameAction` port — budget the most time here).
26. Stack, simultaneous trigger ordering, replacement effects (`MagicStack`, `replacement/`).
27. Continuous effects & the layer system (`StaticAbilityContinuous`).
28. Combat (`combat/`), mana payment (`mana/`), mulligans (`mulligan/`).
29. Scenario-parity harness (Layer 2) + ≥300 fixtures. **Exit gate:** P4 gate — scenario suite green.

### M6 — Effects, corpus-gated — 6–12 wks _(parallelizable; the long tail)_

30. Implement APIs in corpus-first, then frequency order (Section 1.5). Keywords, triggers, replacements, cost parts
    alongside.
31. Three scenarios minimum per API. Parity matrix updated continuously.
32. Replay-parity harness (Layer 3) stood up as soon as full games run at all — do not defer this to the end. **Exit
    gate:** P5 gate — 100% corpus coverage for the active gauntlet; replay parity green over the nightly log corpus.

### M7 — AI port — 5–8 wks

33. `ComputerUtilMana` first (the runner cannot play a real game without mana planning), then `ComputerUtilCombat`,
    `ComputerUtilCard`, `CreatureEvaluator`.
34. `AiController` + per-API `SpellAbilityAi` decision logic; `.ai` profile loading.
35. Lookahead simulator on top of the cheap Go state clone. **Exit gate:** P6 gate — statistical parity within stated
    bounds vs. Java on the reference gauntlet.

### M8 — Simulation runner & telemetry — 2–3 wks

36. `internal/sim`: worker pool, seed management, gauntlet config, turn caps, crash isolation (a panicking game fails
    that game only).
37. `internal/telemetry`: recorder, castability probe, mana sampler, tenure tracking, aggregation.
38. `internal/store`: shard writers, `manifest.json`, and a streaming reader (ADR-0016). **Exit gate:** P7 gate — 100k
    games clean, deterministic re-run byte-identical.

### M9 — Reporting & the optimization loop — 2–4 wks

39. `internal/report`: matchup matrix w/ CIs, dead-card table, mana health, per-card impact (all four attribution modes,
    each labelled), opening-hand analysis, play/draw split.
40. Markdown + HTML + JSON output; `crucible report`.
41. **A/B swap optimizer**: propose candidate swaps from the dead-card and impact tables, run the confirmatory A/B
    gauntlet, rank by measured win-rate delta with CIs. This is the actual product. **Exit gate:** P8 gate — blind-test
    diagnosis matches expert assessment on known-good and known-bad decks.

### Continuous, from M0 onward

- Every ported unit gets a `port-log/` note before merge (doc-before-code applies per unit, not just per phase).
- Parity matrix and coverage report regenerate in CI.
- Nightly: replay-log generation from Java, full parity suite, fuzz/soak, statistical dashboard.

## 5.1 Top risks and their mitigations

| Risk                                                                                             | Mitigation                                                                                                                                                   |
| ------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Layer system divergence** (silently wrong P/T, types, costs — poisons every downstream metric) | Dedicated fixture family per layer and per timestamp-ordering case, built during M5 before any effect work                                                   |
| **Effect long tail is unbounded**                                                                | Corpus gate (ADR-0011). "Done" is defined by the gauntlet, not by the whole corpus. Unsupported card in a deck = hard error at load, never a silent misplay  |
| **AI divergence masking rules bugs**                                                             | Replay-parity harness (Layer 3) removes AI from rules comparison entirely. Build it in M6, not M7                                                            |
| **Upstream fork drift**                                                                          | ADR-0001: no edits to upstream Java; all patches logged. Upstream card-script changes are absorbed automatically, since Crucible reads the same `.txt` files |
| **Telemetry metrics that are subtly wrong** (worse than no metrics)                              | Normative, versioned definitions written in M0; thresholds in config and printed in every report header                                                      |
| **Confounded impact metrics mis-ranking cards**                                                  | Four labelled attribution modes; A/B swap as the confirmatory gate before any recommendation is emitted                                                      |
| **Estimate risk on M5/M6**                                                                       | These two milestones dominate. Re-baseline after M5's exit gate with real velocity data before committing to a date                                          |
