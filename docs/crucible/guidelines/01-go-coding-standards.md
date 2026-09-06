# GO — Go Coding Standards

- **Status:** Active
- **Applies to:** all Go code under `crucible/`
- **Rule IDs:** cite as `GO-n`

Standard Go, plus rules that exist because this codebase is a port of a 210k-LOC Java engine. Java shapes leak. These
rules block the leaks.

---

## GO-1 — Toolchain and formatting

| Tool     | Setting                                                                                               |
| -------- | ----------------------------------------------------------------------------------------------------- |
| Go       | pinned in `go.mod`, bumped by ADR only                                                                |
| Format   | `gofmt -s`. Non-negotiable, CI-enforced                                                               |
| Lint     | `golangci-lint`: `errcheck`, `govet`, `staticcheck`, `revive`, `gocritic`, `ineffassign`, `unconvert` |
| `nolint` | Requires inline reason: `//nolint:gocritic // hot path, bounds check hoisted manually`                |

Bare `//nolint` fails review.

---

## GO-2 — No package-level mutable state in the engine

Hard ban inside `internal/engine/...` and `internal/carddb/...`.

**Bad** — direct translation of Java's `StaticData` / `FModel` / `MyRandom`:

```go
var cardDB *carddb.DB
var rng *rand.Rand

func LoadDB(path string) { cardDB = carddb.Load(path) }
```

**Good** — injected, immutable, carried on the game:

```go
type Game struct {
    db   *carddb.DB   // shared across goroutines, never mutated after load
    rand *javarand.Rand
}
```

Reason: engine runs goroutine-per-game (ADR-0005). One package-level var kills concurrency and produces a race that only
appears at 10k games. Java gets away with it because Forge runs one game per process; Crucible does not.

Custom lint rule enforces this. `var` at package scope must be `const`-like: typed constants, lookup tables,
`sync.Once`-guarded registries built at init and never written again.

---

## GO-3 — No mutexes in the engine

Games share only immutable data. A `sync.Mutex` inside `internal/engine` means the design is wrong.

Allowed: `internal/sim` (worker pool), `internal/store` (shard writers), `internal/telemetry` (merge step).

Reason: locks in the engine serialize the one thing the port exists to parallelize.

---

## GO-4 — No reflection in the engine

`reflect` banned in `internal/engine/...`.

Java resolves `ApiType` → effect class by reflection. Go uses a generated registry:

```go
//go:generate go run ../../tools/gen/effectregistry

var effectRegistry = [numAPITypes]EffectFactory{
    APIDealDamage: newDealDamageEffect,
    APIDraw:       newDrawEffect,
    // ...
}
```

Reason: compile-time checked dispatch, no init-order hazard, dead-code elimination works, and a missing API is a build
error instead of a runtime panic on card 12,004.

Allowed outside the engine: `encoding/json` internals, test helpers, `go-cmp`.

---

## GO-5 — Interfaces at the consumer, small

Define the interface where it is used, not where it is implemented. Keep it to the methods the consumer calls.

**Bad** — Java-style wide interface next to the implementation:

```go
package effect
type Effect interface {
    Resolve(*game.Game, *sa.SpellAbility) error
    CanPlayAI(...) bool
    Description() string
    ChooseTargets(...) error
    // 12 more
}
```

**Good:**

```go
package resolver
type Resolver interface {   // what the stack actually needs
    Resolve(*game.Game, *sa.SpellAbility) error
}
```

Exception: `control.PlayerController` is genuinely wide (~110 methods, mirrors the Java contract). Keep it in one place,
document why in the port-log.

---

## GO-6 — Embedding for shared behavior, never for "is-a"

**Good** — shared helpers:

```go
type BaseEffect struct{ params ParamSet }

func (b BaseEffect) TargetsOrDefault(...) []EntityID { ... }

type DealDamageEffect struct {
    BaseEffect
    amount expr.Count
}
```

**Bad** — modelling a Java hierarchy:

```go
type SpellAbility struct{ ... }
type Spell struct{ SpellAbility }          // "extends"
type AbilityActivated struct{ SpellAbility }
type AbilitySub struct{ SpellAbility }
```

Use one concrete type with a `Kind` discriminator instead. See
[02-java-to-go-translation](02-java-to-go-translation.md).

---

## GO-7 — Errors

```go
if err != nil {
    return fmt.Errorf("resolve %s on %s: %w", api, cardName, err)
}
```

- Wrap with `%w`, add context that names the card / API / fixture.
- Sentinel errors for conditions callers branch on: `var ErrUnknownAPI = errors.New("unknown api")`.
- Never `_ = err`. `errcheck` blocks it.

**Panic policy:**

| Condition                                                         | Response                              |
| ----------------------------------------------------------------- | ------------------------------------- |
| Malformed card script, unknown vocabulary                         | `error`, fail the load, name the file |
| Illegal game action requested by a controller                     | `error`, reject the action            |
| Engine invariant broken (card in two zones, negative arena index) | `panic`                               |

Panic is recovered at the game boundary in `internal/sim`, reported as one failed game with a full state dump. Never let
one bad card kill a 100k-game batch.

---

## GO-8 — No `any` in engine packages

`internal/engine/...` and `internal/carddb/...` must be fully typed.

Java's ability params are `Map<String,String>`. Go generates a typed param struct per API:

```go
type DealDamageParams struct {
    NumDmg     expr.Count
    ValidTgts  valid.Spec
    DamageSource card.Ref
}
```

Reason: `Map<String,String>` params are the #1 silent-bug source in the Java engine. Typing them is the point of
porting, not incidental.

Allowed: telemetry side-channel payloads (typed variants preferred), JSON decode boundaries, test helpers.

---

## GO-9 — Identity by ID, never by pointer

```go
type CardID uint32
type PlayerID uint8
type EntityID uint32
```

Cards get copied, LKI-snapshotted, exiled and returned. Pointer identity is unreliable even in Java.

- Compare `CardID`, never `*Card`.
- `equals`-style methods only on value types: `ManaCost`, `ColorSet`, `TypeLine`.
- Never define equality on `Card`, `Player`, `Game`.

---

## GO-10 — Package layout and naming

| Rule                                                 | Detail                                                        |
| ---------------------------------------------------- | ------------------------------------------------------------- |
| No `util`, `common`, `helpers`, `misc` packages      | Name by what it does: `pkg/collect`, `internal/mana`          |
| Package name = single lowercase word, no underscores | `carddb`, `staticability` → `static`                          |
| No stutter                                           | `carddb.CardDB` → `carddb.DB`; `mana.ManaCost` → `mana.Cost`  |
| `internal/` for everything engine-side               | Only `pkg/collect` and `pkg/javarand` are importable outside  |
| One package per coherent responsibility              | Package with no testable public API should be merged (TEST-4) |

---

## GO-11 — Zero values over `null` translation

Java `null` is a decision point. Do not translate it mechanically.

| Java                                       | Go                                       |
| ------------------------------------------ | ---------------------------------------- |
| `null` meaning "none, and that is normal"  | Zero value (`""`, `0`, empty slice)      |
| `null` meaning "absent vs present matters" | Pointer, or `(T, bool)`                  |
| `null` returned from a lookup              | `(T, bool)` — always                     |
| `null` meaning "not yet computed"          | Explicit `computed bool`, or `sync.Once` |

Each non-obvious choice gets a line in the unit's `port-log/` note.

---

## GO-12 — Collections

| Java                                     | Go                                                         |
| ---------------------------------------- | ---------------------------------------------------------- |
| Guava `FCollection<T>` (ordered set)     | `pkg/collect.OrderedSet[T comparable]` — slice + index map |
| `List<T>`                                | `[]T`                                                      |
| `Map<K,V>` where iteration order matters | `collect.OrderedMap[K,V]`, never bare `map`                |
| `Multimap`                               | `map[K][]V`                                                |
| `Iterables.filter` / `Predicates`        | `collect.Filter`, `collect.Any`, plain loops               |

**Iteration order is load-bearing.** Trigger ordering, simultaneous-ability ordering, and replacement selection all
depend on it. Go map iteration is randomized. Substituting a bare `map` for an `FCollection` produces non-deterministic
games and destroys replay parity.

---

## GO-13 — Fields over accessors

Java has ~8,000 lines of getter/setter. Do not translate them.

- No invariant → exported field.
- Invariant to enforce → unexported field + method that enforces it, with a comment saying what the invariant is.

```go
type Card struct {
    ID    CardID     // immutable after creation
    Owner PlayerID   // immutable after creation

    zone  zone.Type  // unexported: zone changes must go through the game so
                     // triggers and replacements fire. See GameAction port.
}

func (c *Card) Zone() zone.Type { return c.zone }
```

---

## GO-14 — Dependencies

Near-zero. Adding one requires an ADR.

Currently allowed:

| Dep                        | Scope      | Why                                    |
| -------------------------- | ---------- | -------------------------------------- |
| `github.com/google/go-cmp` | tests only | Readable diffs on ASTs and game states |
| stdlib                     | everywhere |                                        |

Anything else: ADR first. "It is convenient" is not a reason.

---

## GO-15 — Comments

- Every exported identifier in `internal/engine` and `internal/carddb` has a doc comment.
- Every ported unit carries provenance:

```go
// Ported from forge-game/src/main/java/forge/game/GameAction.java (checkStateEffects).
// Deviations recorded in docs/crucible/porting/port-log/game-action.md.
```

- Decisions cite their ADR: `// ADR-0009: arena handles, not pointers.`
- Comments explain **why**. `// increment i` is noise. `// Layer 7b applies before 7c; see CR 613.4` is not.
- Code comments use normal English, not compressed doc style (`DOC-7`).

---

## GO-16 — Performance rules that are not premature

Engine runs 10⁵–10⁶ games. These are load-bearing, not micro-optimization.

| Rule                                                                                 | Reason                                                                                    |
| ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------- |
| Compiled card definitions shared read-only; per-game `Card` holds only mutable state | Java re-parses strings per instance per game. That is the port's main perf win (ADR-0007) |
| Events are a flat struct in a preallocated per-game buffer, not an interface tree    | Interface-per-event allocates and escapes; millions per batch                             |
| No allocation in the priority loop or SBA check                                      | Profiled hot path                                                                         |
| Game state clone is value-oriented and benchmarked                                   | AI lookahead depth depends on it                                                          |

Benchmark required for any change to these. Regression threshold enforced in CI.

Everywhere else: write clear code first.

---

## Checklist before merging Go code

- [ ] `gofmt -s`, lint clean, no bare `//nolint`
- [ ] No package-level mutable state in engine packages (GO-2)
- [ ] No mutex, no reflection, no `any` in engine packages (GO-3, GO-4, GO-8)
- [ ] Identity by ID (GO-9)
- [ ] Errors wrapped with card/API context; panic only on invariant breach (GO-7)
- [ ] Provenance comment on ported units (GO-15)
- [ ] Doc landed in the same commit (DOC-12)
- [ ] Tests follow [03-testing-standards](03-testing-standards.md)

## Related

- [00-documentation-style](00-documentation-style.md)
- [02-java-to-go-translation](02-java-to-go-translation.md)
- [03-testing-standards](03-testing-standards.md)
