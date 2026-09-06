# ADR-0003 — Go Project Layout

- **Status:** Proposed
- **Date:** 2026-09-06
- **Deciders:** `jc@archlab.pl`

## Context

Java tolerates package cycles. Go forbids them. That single difference decides this ADR, and it decides it much more
forcefully than expected.

Measured across `forge-game/src/main/java/forge/game`, 21 packages, counting only `forge.game.*` imports:

```text
direct two-package cycles: 82
```

Every significant package cycles with nearly every other: `card` with `game`, `player`, `zone`, `spellability`,
`combat`, `cost`, `mana`, `keyword`, `trigger`, `replacement`, `staticability`, `event`. The root `forge.game` package
cycles with all 17 of its subpackages.

The cause is ordinary object-oriented modelling. `Card` holds a reference to its `Game`; `Game` holds `Card`s. A
`SpellAbility` knows its host `Card`; a `Card` lists its `SpellAbility`s. A `Trigger` reads card properties and fires
into the game. None of this is bad Java. All of it is uncompilable Go.

**The package tree in the implementation plan, Section 3.1, is therefore wrong.** It listed
`internal/engine/{game,card,player,zone,sa,cost,effect,trigger,...}` as sibling packages mirroring Java. That tree
cannot build. This ADR replaces it.

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
   whole answer: it puts ~100k lines and the 203 effect implementations in a single package, and makes every future
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
    engine/              THE RECURSIVE CORE — one package, see below
    engine/effect/       203 API implementations      (imports engine)
    valid/  expr/        evaluate against game state  (imports engine)
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
ADR-0009). That is what makes `valid` and `expr` separate packages rather than core members.

**`internal/` for everything, `pkg/` only for `collect` and `javarand`.** Those two are genuinely general and have no
Crucible semantics. Everything else stays unimportable from outside, because there is no API stability promise while the
port is in progress.

## Consequences

**Good.** The layout compiles, which the mirrored alternative does not. Dependency direction is a single arrow into
`engine`, so a cycle can only ever be reintroduced by editing `engine` itself, and the compiler catches it immediately.
Effects, AI, telemetry, and the parsers are each independently testable against a stable core. Hot paths inside the core
use direct calls with no interface dispatch.

**Bad.** `internal/engine` will be large — plausibly 30k to 50k lines — and large packages resist navigation, slow
incremental compilation, and let unrelated internals reach each other with no compiler objection. Discipline replaces
enforcement inside that boundary, which is exactly the weakest kind of guarantee. Contributors arriving from the Java
source will repeatedly propose splitting it, and each proposal needs the cycle analysis re-run rather than a reflexive
no.

**Neutral.** One large core package means one large test unit under TEST-4, which sounds worse than it is: engine
behaviour is tested through fixtures (TEST-5), and fixtures subdivide by rule, not by package. Explicit registry wiring
adds a line per effect in `cmd/` that an `init()` would have hidden — verbose, but it keeps the dependency arrow
readable and makes partial registration possible in tests.

## Related

- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-5, GO-9, GO-10
- [03-testing-standards.md](../guidelines/03-testing-standards.md) — TEST-4, package as test unit
- [00-master-implementation-plan.md](../00-master-implementation-plan.md) — Section 3.1, superseded by this ADR
- ADR-0008 — effect dispatch registry
- ADR-0009 — handles and the arena, which is what keeps the core from growing
