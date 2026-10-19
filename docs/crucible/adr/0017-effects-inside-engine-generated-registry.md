# ADR-0017 — Effects Inside `internal/engine`, Registry Generated From Effect Files

- **Status:** Accepted
- **Date:** 2026-09-25
- **Deciders:** `jc@archlab.pl`
- **Supersedes:** ADR-0008 in full; ADR-0003's `engine/effect` placement and `cmd/` wiring (its layout tree and "the
  registry is populated by explicit wiring from `cmd/`")

## Context

ADR-0003 and ADR-0008 put the 203 API implementations in `internal/engine/effect`, wired by a generated
`effect.Register` called from `cmd/`. M6 built something else:

| Planned (ADR-0003, ADR-0008)                  | Built                                                                |
| --------------------------------------------- | -------------------------------------------------------------------- |
| Effects in `internal/engine/effect`           | 135 `*effect.go` files in `internal/engine` itself                   |
| Generated `Register`, called from `cmd/`      | Hand-written `NewRegistry()` in `internal/engine/castspell.go`       |
| Generator input: corpus API vocabulary scan   | No generator; unregistered APIs return `ErrUnimplemented` (ADR-0011) |
| `enginelint` groups declared in one JSON file | `enginelint.json` 1,680 lines; 133 one-file effect groups            |

Placement was not a slip. 134 of the 135 effect files call unexported engine functions (`moveByEffect`,
`targetedOrDefinedCards`, `subAbilityConditionMet`, the animate/layer records). A separate package would force all of
those to be exported, turning the engine's internals into an API for one caller.

The hand-written wiring now costs more than it saves. Every effect port edits the same three places — `NewRegistry()`
plus its batch-by-batch doc comment, `enginelint.json` (a group, an allow-list, the `castspell` allow-list), and the
resolved-API counts in `CLAUDE.md` and the plan — so two parallel ports always conflict, and the counts drift.

## Decision Drivers

- GO-4: no reflection; dispatch stays an array index into an interface value (ADR-0008's measurement still holds).
- Parallel effect ports must not conflict outside their own files.
- A stale registry or a wrong count must fail CI, not be found by reading.
- `enginelint` boundaries stay enforced for every effect file (ADR-0003).

## Considered Options

1. **Keep hand-written wiring.** Rejected: every port conflicts on `castspell.go`, `enginelint.json` and the counts.
2. **Move effects to `internal/engine/effect` as ADR-0008 planned.** Rejected: exports the unexported surface 134 of 135
   effect files use.
3. **`init()` self-registration per effect file.** Rejected for ADR-0008's reason: contents depend on imports, and
   package-level mutable state in the engine violates GO-2.
4. **Generate `NewRegistry()` from the effect files; effect files declare their own lint allow-list.** **Chosen.**

## Decision

**Effects live in `internal/engine`.** One `<api>effect.go` per API family, as built.

**`tools/genregistry` writes `internal/engine/registry_gen.go`.** It parses the package with `go/ast` and registers
every type whose name ends in `Effect` and that has a value-receiver `Resolve(*Game, *Ability, PlayerController) error`.
Type `xEffect` registers as `APIX` with `xEffect{}`. A type serving other or several APIs, or needing field values, says
so in its doc comment, one line per API:

```go
//crucible:register Manifest manifestEffect{api: "Manifest", remember: "RememberManifested"}
```

An API name with no `APIType` constant, or registered twice, fails the generator.
`go generate -run genregistry ./internal/engine` regenerates; `genregistry -check` fails CI when the committed file is
stale — the check ADR-0008 named as the condition for the whole guarantee.

**`NewRegistry()` stays an explicit constructor, not an `init()`.** A test that wants a subset still builds its own
`Registry`.

**Each effect file declares its own `enginelint` group** with a `//enginelint:allow <group> ...` line after the package
clause. The file is its own group, named after its base name. `enginelint.json` keeps the shared engine groups. Files
carrying the standard generated-code header are not checked as the source of a reference; they are wiring, and the
generator is what constrains them.

**Counts are checked, not maintained.** `genregistry -check` also verifies the resolved-API count written in
`CLAUDE.md`: registered APIs minus the three M5 casting entries (`PermanentCreature`, `PermanentNoncreature`, `Attach`).

**Unimplemented APIs stay explicit:** an empty slot returns `ErrUnimplemented` naming the API (ADR-0011 decides when
that is acceptable).

## Consequences

**Good.** A new effect touches its own file, a regenerated `registry_gen.go`, and the docs for that effect. Two parallel
ports conflict at most in the sorted generated file, and the fix is to rerun the generator rather than to merge by hand.
A forgotten registration, a stale registry and a wrong count all fail CI. Effects keep direct access to engine internals
with no exported surface created for them.

**Bad.** `internal/engine` stays one large package, so every boundary inside it rests on `enginelint`, now configured in
135 files plus one JSON file instead of one JSON file. Registration by naming convention is implicit: renaming a type
changes which API it registers, caught only because the generated file then differs and `-check` fails. The generator is
a build step that must run before commit.

**Neutral.** The count check pins one phrasing in two docs; rewording that sentence means updating the check.

## Related

- ADR-0003 — layout; its `engine/effect` placement is superseded here, everything else stands
- ADR-0008 — superseded; its reasoning on reflection and stateless effects carries over
- ADR-0011 — when an `ErrUnimplemented` slot is acceptable
- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-2, GO-4
