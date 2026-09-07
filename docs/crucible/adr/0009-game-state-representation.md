# ADR-0009 — Game State Representation

- **Status:** Proposed
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

Two things read game state constantly and want opposite properties from it. The rules engine wants cheap mutation. The
AI wants cheap _copies_, because lookahead means playing a line out on a throwaway state and throwing it away.

**Java's copy is a graph walk with pointer fixup.** `forge-ai/.../simulation/GameCopier.java` is 531 lines whose core is
`CopiedGameObjectMap.map(GameObject)` — every reference in the copied graph must be looked up and rewritten to point at
the corresponding object in the new graph. Cards reference their game, their controller, their attachments, their
`Remembered` lists; every one of those needs remapping.

That cost is why Forge's simulation AI searches shallowly. It is also the single largest opportunity in the port: the Go
engine's search depth is set by how cheap a copy is.

**Pointer identity is already unreliable in Java, so nothing is lost by abandoning it.** Cards are copied for the stack,
snapshotted as last-known-information, exiled and returned as new objects. Forge compares by identity in some places and
by timestamp in others precisely because the object is not stable across those transitions.

`Card.java` is 8,105 lines. Whatever representation is chosen has to be split into something navigable (ADR-0003 keeps
the package boundary, not the file).

## Decision Drivers

- Copy cost sets AI search depth, which sets the quality of every win-rate number Crucible reports.
- No shared mutable state between games (ADR-0005) — a copy must be independent, with no residual pointers.
- Copies happen inside a game's own goroutine, so nothing needs to be safe for concurrent mutation.
- Memory is the scaling limit under goroutine-per-game, so per-game footprint matters more than per-object elegance.

## Considered Options

1. **Port the pointer graph and `GameCopier`.** Rejected — carries the remapping cost that limits Java's search depth,
   and pointer-chasing defeats the cache locality that makes the Go version worth writing.
2. **Persistent/immutable structures with structural sharing.** Rejected — copies become nearly free, but every mutation
   allocates, and the rules engine mutates constantly. Wrong trade for a workload dominated by mutation.
3. **Copy-on-write at the card level.** Rejected — the bookkeeping to decide what to clone costs more than copying a
   contiguous slice of small structs.
4. **Arena of value types, addressed by integer handles.** **Chosen.**

## Decision

**Every game entity lives in a per-`Game` arena and is addressed by an integer handle.**

```go
type CardID uint32
type PlayerID uint8
type EntityID uint32   // card or player, for targeting

type Game struct {
    cards   []Card     // arena; CardID is the index
    players []Player
    zones   Zones
    db      *carddb.DB // shared, immutable (ADR-0005, ADR-0007)
    rand    Rand       // per-game streams (ADR-0006)
}
```

**A `Card` holds no back-reference.** No `*Game`, no `*Player` — a `PlayerID` for its controller, a `CardID` for
attachments. Operations take `*Game` as a parameter. This is what keeps `valid` and `expr` outside the engine core
(ADR-0003) and what makes a copy trivial.

**Copy is a slice copy.** Handles are indices, so they stay valid in the copy without rewriting anything. There is no
equivalent of `CopiedGameObjectMap`, because there are no pointers to remap.

**Handles are never reused within a game.** A destroyed card's slot stays allocated. Last-known-information snapshots,
`Remembered` lists, and delayed triggers all hold handles to objects that have left the battlefield, and reuse would
silently alias them to something else. Memory cost is bounded by cards created, which is small.

**Identity is the handle.** No `equals` on `Card` or `Player`; comparison is `==` on the ID (GO-9). Value types —
`ManaCost`, `ColorSet`, `TypeLine` — keep ordinary value equality.

**`Card` is split by concern**, since 8,105 lines of Java has to land somewhere: identity and zone on `Card`, with
`State`, `Counters`, `Attachments`, `Damage`, and `Memory` as separate types. Same package (ADR-0003), separate files
and separate tests.

**Copy cost is a benchmark with a regression threshold**, not an aspiration. It is the number that decides search depth,
so it belongs in CI alongside the priority loop and the state-based-action check (GO-16).

## Consequences

**Good.** Copying a game becomes a contiguous memory copy rather than a graph traversal with per-object lookup, which is
what lets the Go AI search deeper than Java's on the same hardware. Value types in slices give cache locality the
pointer graph cannot. Handles are stable across zone changes, copies, and snapshots, removing the identity ambiguity
Java works around. Serialising state for a fixture or a crash dump is straightforward, because the arena is the state.

**Bad.** Every access is an arena lookup, so code reads as `g.Card(id).Power` rather than `card.Power` — more verbose
everywhere, and a real ergonomic cost paid on every line of the engine. A stale handle is not a nil dereference but a
silent read of the wrong card, which is a worse failure mode than the one it replaces; only the no-reuse rule and
debug-build validation stand between the design and that bug. Never reusing slots also means a long game with heavy
token production grows its arena monotonically.

**Neutral.** Copies are cheap enough to be tempting, and nothing here limits how often the AI takes one — the memory
ceiling under goroutine-per-game (ADR-0005) is what actually bounds it, so worker count and search depth are coupled in
a way that needs tuning together rather than separately.

## Related

- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-9, GO-16
- ADR-0003 — why keeping back-references out is what keeps the core package small
- ADR-0005 — the memory ceiling that bounds copy frequency
- ADR-0007 — the shared immutable definitions a `Card` points at
