# ADR-0003 — Go Project Layout

- **Status:** Accepted; `engine/effect` placement and `cmd/` registry wiring superseded by
  [ADR-0017](0017-effects-inside-engine-generated-registry.md)
- **Date:** 2026-09-06
- **Deciders:** `jc@archlab.pl`

## Context

Java tolerates package cycles. Go forbids them. That single difference decides this ADR, and it decides it much more
forcefully than expected.

Measured across `forge-game/src/main/java/forge/game`, 22 packages, counting only `forge.game.*` imports.
`crucible/tools/javacycles` re-runs the count, so the premise can be checked after an upstream sync rather than trusted:

```console
$ cd crucible && go run ./tools/javacycles -root ../forge-game/src/main/java -prefix forge.game
packages: 22
direct two-package cycles: 87
```

Pass `-expect 87` to turn a change in the premise into a build failure.

The design-time measurement was 21 packages and 82 cycles. The five extra cycles come from the `forge.game.card.sticker`
package (Unfinity stickers, upstream `7e3b22b44f5`), which couples to `ability`, `card`, `player`, `staticability` and
`trigger`. The premise holds: one more package joins the same tangle. The 82 in the rest of this ADR is the design-time
measurement.

Every significant package cycles with nearly every other: `card` with `game`, `player`, `zone`, `spellability`,
`combat`, `cost`, `mana`, `keyword`, `trigger`, `replacement`, `staticability`, `event`. The root `forge.game` package
cycles with all 17 of its subpackages.

The cause is ordinary object-oriented modelling. `Card` holds a reference to its `Game`; `Game` holds `Card`s. A
`SpellAbility` knows its host `Card`; a `Card` lists its `SpellAbility`s. A `Trigger` reads card properties and fires
into the game. None of this is bad Java. All of it is uncompilable Go.

**So the tree a port reaches for by reflex is the one tree that cannot exist.**
`internal/engine/{game,card,player,zone,sa,cost,effect,trigger,...}` as sibling packages mirrors Java exactly and does
not build. This ADR is what the plan's layout section defers to.

## Decision Drivers

- Go's cycle prohibition is absolute and there is no escape hatch.
- Package boundaries are test boundaries (TEST-4), so they must fall where behaviour is observable, not where Java
  happened to put a directory.
- `internal/` gives compiler-enforced encapsulation. Nothing in the engine is a public API during a port.
- Hot paths run 10^5 to 10^6 games. Interface dispatch introduced purely to break a cycle is a permanent tax.
- Contributors will reach for the Java structure by reflex. The layout has to make the correct thing the easy thing.

## Considered Options

### Package structure

1. **Mirror Java's packages.** Rejected — does not compile. Not a judgement call.
2. **One package for all of `internal/engine`.** Compiles, because cycles inside a package are legal. Rejected as the
   whole answer: it puts the 50-65k-line core and the 203 effect implementations in one package, and makes every future
   split a breaking refactor.
3. **Interface at every boundary**, each package declaring the narrow interface it needs. Rejected — breaks cycles at
   the cost of dynamic dispatch on the priority loop and state-based-action check, and produces an interface per package
   pair with nothing gained in clarity.
4. **A deliberately large recursive core, with acyclic satellites.** **Chosen.**

## Decision

Standard Go layout, with the core sized by what genuinely must be mutually recursive rather than by Java's directories.

```text
crucible/
  cmd/crucible/          CLI: run, report, corpus-coverage, parity
  cmd/crucible-oracle/   differential-test driver
  internal/
    carddb/              .txt -> CardRules            (no engine import)
    carddb/compile/      script -> typed AST          (no engine import)
    mana/  cardtype/     value types                  (leaf)
    valid/  expr/        parse the value grammars     (leaf)
    engine/              THE RECURSIVE CORE — one package, see below
    engine/effect/       203 API implementations      (imports engine)
    ai/                  controllers, eval, lookahead (imports engine)
    sim/  telemetry/  store/  report/
  pkg/collect/  pkg/javarand/
  oracle-java/
```

**`internal/engine` is one package, and that is the point of this ADR.** It holds exactly the types whose references are
genuinely bidirectional: `Game`, `Card`, `CardState`, `Player`, `Zone`, `Stack`, `SpellAbility`, `Combat`, `Trigger`,
`ReplacementEffect`, `StaticAbility`, `Cost`, `ManaPool`, plus the `Effect` and `PlayerController` interfaces. Files are
organised by topic — `card.go`, `sba.go`, `layers.go`, `combat.go`, `stack.go` — so the source is navigable even though
the compilation unit is not subdivided.

**Everything that can be acyclic is a separate package, and the dependency arrow points one way: into `engine`, never
out.** `engine` declares the `Effect` interface; `engine/effect` implements it. The registry is populated by explicit
wiring from `cmd/`, not by `init()`, so the direction stays visible and test binaries can register a subset.

**Reducing the core is ongoing work, not a one-time decision.** Any type that stops needing a back-reference moves out.
Handles help here: a `Card` holds a `CardID`, never a `*Game`, and operations take `*Game` as a parameter (GO-9,
ADR-0009).

**`valid` and `expr` turned out to be parsers, not evaluators, which flips the arrow this ADR originally drew for
them.** The plan above had them importing `engine` to evaluate a parsed grammar against live game state. What actually
got built (M3, before `internal/engine` existed to import) is the opposite split: `valid`/`expr` parse a script's value
grammar into a plain value type (`valid.Spec`, `expr.Amount`) with zero `Game`/`Card` dependency — the same "leaf,
corpus-fuzzed pure function" category `mana` and `cardtype` are already in, and TEST-2's own table names them by name
for it. Evaluating a parsed `Spec` against a real game needs `Game`/`Card`, which only exist inside the core, so that
half (`engine.Matches`, `valid.go`) lives in `internal/engine` itself, importing `valid` for the types it walks — the
one direction that does not create a cycle, since the alternative (moving the evaluator into `valid`, which would then
import `engine`) would leave `internal/engine`'s own state-based-action code needing to import `valid` right back to
call it. `valid`/`expr` stay separate packages for the same handles-shaped reason this ADR already gives: they are
genuinely usable, and fuzz-tested, without a `Game` in scope at all.

**Handles do not dissolve the cycles, though, and neither does deleting the GUI.** The 82 cycles were measured on Java,
where cards hold `*Game`. [ADR-0009](0009-game-state-representation.md) removes exactly that, so the fair question is
whether the ported design still cycles. It does:

| Scenario                                                       | Cycles |
| -------------------------------------------------------------- | -----: |
| Java as-is                                                     |     82 |
| Minus GUI view-models, which Crucible never ports              |     82 |
| Minus shared base types and enums, extracted to a leaf package |     81 |
| Both                                                           | **81** |

Handles dissolve three cycles. Deleting the entire view layer dissolves none. Extracting `CardTraitBase`, `IHasSVars`,
`EvenOdd`, `Direction` and `GameStage` into a leaf package dissolves one. What remains is genuine domain coupling —
`card` with `zone`, `card` with `player`, `spellability` with `trigger`, `cost` with `mana`, `phase` with `combat`.
Those are Magic's relationships, not Java artefacts, and no arrangement of Go packages removes them.

**Inside the core, `crucible/tools/enginelint` replaces discipline with enforcement.** It groups the package's own files
and fails on a reference crossing a boundary the config does not allow. Go offers no sub-package visibility, so this is
the only way the arrangement can be checked rather than remembered.

**`internal/` for everything, `pkg/` only for `collect` and `javarand`.** Those two are genuinely general and have no
Crucible semantics. Everything else stays unimportable from outside, because there is no API stability promise while the
port is in progress.

## Consequences

**Good.** The layout compiles, which the mirrored alternative does not. Dependency direction is a single arrow into
`engine`, so a cycle can only ever be reintroduced by editing `engine` itself, and the compiler catches it immediately.
Effects, AI, telemetry, and the parsers are each independently testable against a stable core. Hot paths inside the core
use direct calls with no interface dispatch.

**Bad.** `internal/engine` will be large. Measured against what actually has to sit in the cyclic core:

```text
forge-game, src/main                    125,436
  GUI view-models, never ported          -3,974
  ability/effects -> engine/effect       -26,583
  ------------------------------------------------
  genuinely cyclic core                   94,879
  accessor declarations that vanish      -10,263   (3,421 declarations, GO-13)
  valid/ and expr/ leave the core         -6,085   (CardProperty + AbilityUtils)
  ------------------------------------------------
  subtotal                                78,531
  Go 20-35% denser than Java          51,000-63,000
```

This table is a pre-implementation estimate, not a measurement, and one line undercounts what actually landed in the
core: `valid/ and expr/ leave the core` assumed all of `CardProperty`/`AbilityUtils` moved out, but only the parsing
half did (above) — the evaluation half came back in as `internal/engine/valid.go`, 626 lines measured today, `expr`'s
own evaluator not built yet. Small next to the budget below, not worth re-deriving the Java-side split for, but the
`-6,085` is an overstatement by that much.

**Budget 50,000 to 65,000 lines in one package.** Packages that size resist navigation, slow incremental compilation,
and let unrelated internals reach each other with no compiler objection. `enginelint` covers the last of those and
nothing covers the first two. Contributors arriving from the Java source will repeatedly propose splitting it, and each
proposal needs the cycle analysis re-run rather than a reflexive no.

**Neutral.** One large core package means one large test unit under TEST-4, which sounds worse than it is: engine
behaviour is tested through fixtures (TEST-5), and fixtures subdivide by rule, not by package. Explicit registry wiring
adds a line per effect in `cmd/` that an `init()` would have hidden — verbose, but it keeps the dependency arrow
readable and makes partial registration possible in tests.

## Related

- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-5, GO-9, GO-10
- [03-testing-standards.md](../guidelines/03-testing-standards.md) — TEST-4, package as test unit
- ADR-0008 — effect dispatch registry
- ADR-0009 — handles and the arena, which is what keeps the core from growing
