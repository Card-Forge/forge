# ADR-0008 — Effect Dispatch

- **Status:** Accepted
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

A compiled ability names an API — `DealDamage`, `ChangeZone`, `Pump` — and something has to turn that name into code
that resolves it. There are 203 API constants mapping to 205 effect classes.

**Java resolves this by reflection, once.** `ApiType` is an enum whose constructor takes a
`Class<? extends SpellAbilityEffect>` and calls `ReflectionUtil.makeDefaultInstanceOf`, which does
`cls.getConstructor().newInstance()` (`forge-core/src/main/java/forge/util/ReflectionUtil.java`).

**And every one of the 203 is stateless.** `ApiType` has a second constructor parameter, `isStateLess`, defaulting to
true; the count of constants passing `false` is **zero**. Every effect is instantiated once at enum initialisation and
cached in `instanceEffect`, then shared by every resolution in the process.

That measurement matters because it corrects the obvious argument. Reflection here is **not** a hot-path cost — it runs
203 times at class-init and never again. Removing it buys compile-time safety, not speed, and GO-4 should be read that
way. It also confirms the shape of the Go design: effects can be shared values with no per-resolution allocation.

The real problem with the Java arrangement is different. The enum is the only thing tying an API name to its
implementation, so a missing or misnamed entry surfaces as a `RuntimeException` from `makeDefaultInstanceOf` at startup,
or as an "unknown API" failure the first time some card in the corpus reaches it. Neither is a compile error.

## Decision Drivers

- No reflection in engine packages (GO-4).
- A missing effect must be a build failure, not a runtime discovery on card 12,004.
- The dependency arrow runs into `engine`, never out — `engine` declares the interface, `engine/effect` implements it
  (ADR-0003).
- Dispatch sits under the stack resolution loop, so it must not allocate.

## Considered Options

1. **Port the reflection.** Rejected — banned by GO-4, and Go reflection additionally defeats dead-code elimination, so
   every effect is linked into every binary whether reachable or not.
2. **A hand-written `map[APIType]Effect` in `engine`.** Rejected — inverts the dependency arrow, since `engine` would
   import `engine/effect`. It also drifts silently: adding an API constant without a map entry compiles.
3. **`init()` self-registration from each effect file.** Rejected — the registry's contents then depend on which
   packages happen to be imported, which is invisible at the call site and awkward to subset in tests.
4. **Generated registry, explicitly wired.** **Chosen.**

## Decision

**A generated array, indexed by API type, wired explicitly from `cmd/`.**

```go
// internal/engine — declares the contract, imports nothing downward
type APIType uint16
type Effect interface {
    Resolve(*Game, *SpellAbility) error
}
type Registry [numAPITypes]Effect

// internal/engine/effect — implements, generated wiring
//go:generate go run ../../../tools/gen/effectregistry
func Register(r *engine.Registry) {
    r[engine.APIDealDamage] = dealDamage{}
    r[engine.APIChangeZone] = changeZone{}
    // ... 203 entries, generated
}
```

**Effects are stateless shared values, matching what Java measured.** No per-resolution allocation, no per-call
instantiation. Anything an effect needs comes from `*Game` and `*SpellAbility`.

**The generator's input is the API vocabulary scan**, the same corpus scan that hard-fails on unknown `Key$` names
(ADR-0007, P2 gate). An API used by a card script with no registered effect fails the build; a registered effect with no
API constant fails to compile. Drift is not possible in either direction.

**Registration is a call, not an `init()`.** `cmd/crucible` calls `effect.Register(&reg)` during startup. A test binary
can register a subset, or a stub, without linking every effect — which matters while only part of the corpus is
implemented.

**Unimplemented APIs are explicit.** An unregistered slot holds a sentinel that returns an error naming the API and the
card, so a gap during the port is a clear diagnostic rather than a nil dereference. The corpus coverage gate (ADR-0011)
is what makes that state acceptable during M6 and unacceptable at release.

## Consequences

**Good.** A missing effect is caught at build time across the whole corpus rather than when some card first resolves.
Dispatch is an array index into an interface value with no allocation. Dead-code elimination works, so a binary built
for a restricted corpus does not carry effects it cannot reach. Explicit wiring keeps ADR-0003's dependency arrow
visible at the call site instead of hidden in import side effects.

**Bad.** Generated code is a build step that must run before compilation, and a stale generated file compiles happily
while being wrong — CI has to verify the generator's output matches the committed file, or the whole guarantee is
theatre. The explicit `Register` call is also one more thing to forget in a new entry point, and forgetting it produces
203 sentinel errors rather than an obvious startup failure.

**Neutral.** A single 203-entry generated function is unusual to read but never read by hand. The `Effect` interface
forces a small allocation-free indirection that a giant type switch would avoid, at the cost of a switch nobody could
maintain.

## Related

- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-4, read as safety rather than speed
- ADR-0003 — the dependency arrow this wiring preserves
- ADR-0007 — what a compiled ability carries into dispatch
- ADR-0011 — corpus scoping, which decides when a sentinel slot is acceptable
