# ADR-0029 — Planechase: Active-Plane State

- **Status:** Accepted
- **Date:** 2026-09-27
- **Deciders:** Crucible session (M6 porter round)

## Context

Four `ApiType` effects are deferred on Planechase (CR 901): `Planeswalk` (30 corpus lines), `ChaosEnsues` (11),
`RunChaos` (1), `RollPlanarDice` (1) — 43 lines total. Each Java resolve gates on `Game.getActivePlanes() == null`
(`Game.java:80,1041-1046`) as "not a Planechase game"; Crucible's `*Game` has no equivalent state at all (`zone.go`'s
`PlanarDeck` constant is unused past its name; `effecthelpers.go:99` excludes it from `ChangeZone` destinations).

Forge-oracle research (full report cited throughout) found the surrounding mechanic simpler than expected:

- Plane/Phenomenon cards are ordinary compiled `Card` objects (`Card.fromPaperCard`, `Player.java:2968-2979`), with real
  `Types:Plane <subtype>` lines and real `S:`/`T:` ability scripts (`forge-gui/res/cardsfolder/b/bant.txt`). Their
  abilities are live only while sitting in the `Command` zone, gated by the same `TriggerZones$`/`EffectZone$`
  zone-scoping every other card already uses (`PlaneswalkEffect.java`, `ChaosEnsuesEffect.java` — no Planechase-specific
  check exists in Java's trigger/static dispatch). No new card-state concept is needed.
- `PlanarDice.roll` (`PlanarDice.java:44-49`) is `MyRandom.getRandom().nextInt(6)` mapped to 3 faces
  (Planeswalk/Chaos/Blank, 1/1/4 of 6) — the same RNG stream `rolldiceeffect.go:106` already uses via
  `g.rand.Int32n(6)`. No new randomness primitive needed.
- `PlaneswalkEffect.java:39-43` moves **every player's** current plane back to that player's own `PlanarDeck` bottom
  before the activator's new plane enters (`leaveCurrentPlane`, `Player.java:2669-2679`), not just the activator's.
- `Game.activePlanes` is typed `List<Card>` but every real code path (`initPlane`, `planeswalkTo`) drives it to exactly
  one card.

Corpus weight beyond the 4 APIs: 159 Plane + 14 Phenomenon card files (173 total) carry their own scripts once a
Planechase deck exists; scoping their full ability surface is out of scope for this ADR (Related, below).

## Decision Drivers

- GO-2: no package-level state — Planechase state lives on `*Game`, like every other engine field.
- GO-9: identity by `CardID`, never `*Card`.
- PORT-2: card scripts compile once; Plane/Phenomenon abilities need zero new dispatch machinery, only the same
  zone-scoped trigger/static checks every card already gets — confirmed no Java-side special case exists to port.
- Corpus-first (Plan Section 1.5): port the dominant, unconditional shape of each of the 4 APIs; reject rarer shapes
  (`Optional$`, the replacement-driven "roll extra dice, discard one" shape) as unresolved, matching every other M6
  effect's convention, rather than build for a shape no real corpus card exercises yet.

## Considered Options

**State shape (`Game.activePlanes`):**

1. `Game.ActivePlanes []CardID`, mirroring Java's `List<Card>` exactly.
2. `Game.ActivePlane CardID` (`NoCard` = none), `Game.PlanechaseActive bool` set once at construction.
3. Conflate "not Planechase" and "no active plane" into one nilable field, no separate bool.

**Whose plane is it (needed for `leaveCurrentPlane`'s per-player return zone):**

1. New `Player.CurrentPlane CardID` field, mirroring Java's `Player.currentPlanes`.
2. Derive it from the plane card's own `Card.ZoneOwner` — already the field that answers "whose Command zone is this
   card in" for every other Command-zone card.

**Extra planar-die rolls and mid-roll discard (`PlanarDice.roll`'s replacement-driven "ignore" step,
`PlayerController.choosePDRollToIgnore`):**

1. Port a `ChoosePDRollToIgnore`-equivalent `PlayerController` method now.
2. Defer: no real corpus card grants extra rolls via `ReplaceRollPlanarDice` today: reject that replacement shape as
   unresolved if a future script needs it, add the method then.

## Decision

**State shape: Option 2.** `Game.ActivePlane CardID` (`NoCard` sentinel) plus `Game.PlanechaseActive bool`. Every real
Java invocation drives the list to size ≤1 (Context); a single field is simpler and GO-9-idiomatic. `PlanechaseActive`
is set once at game construction, true when any player's `PlanarDeck` zone is non-empty after setup — this reproduces
Java's `hasAppliedVariant(GameType.Planechase)` gate (`GameAction.java:2358`) without inventing new variant-flag
plumbing Crucible's fixture-driven setup has no other use for (YAGNI): a deck with no planes behaves exactly like Java's
`null` case. Revisit to a slice only if a real corpus card needs concurrent multi-plane (none found).

**Plane ownership: Option 2.** No new `Player` field. `g.Card(activePlane).ZoneOwner` already answers "whose Command
zone is this plane in," reused by `leaveCurrentPlane`'s per-player-loop port. Avoids duplicating state two ways.

**Extra rolls: Option 2, defer.** `RollPlanarDice` ports the single-roll dominant shape only; `Ignore`/extra-`Number`
replacement shapes return `"not resolvable yet"` like every other port's unresolved-shape convention (PORT-8's sibling
discipline: reject before acting, never approximate).

**Trigger modes.** `PlaneswalkedFrom`, `PlaneswalkedTo`, `PlanarDice`, `ChaosEnsues` are new `Mode$` values
`trigger.go`'s `check*Triggers` family does not walk yet. Add all four following the existing 28 modes' own pattern (a
`checkXTriggers` function, dispatched through `pushTriggeredAbilities`'s CR 603.3b ordering) — required for any
Plane/Phenomenon card's own script to ever fire, not just the 4 APIs.

**Replacement types.** `ReplacementType.Planeswalk`/`RollPlanarDice`/`PlanarDiceResult` are not ported now: no real
corpus line drives them for the dominant shape being resolved. A script naming one fails closed via the same
unresolved-`Params` rejection every effect already uses.

## Consequences

**Good:** The state addition is small — one `CardID` field, one `bool`, four trigger modes reusing an existing dispatch
pattern — because Plane/Phenomenon card abilities need no new engine machinery, only the zone-scoping every card already
has. `RollPlanarDice`'s RNG reuses the existing per-`Game` stream with no new primitive. The four target APIs (43 corpus
lines) become portable without a larger redesign.

**Bad:** `RunChaos` and `RollPlanarDice` each cover exactly 1 real corpus line; this ADR's engine piece (trigger modes,
active-plane state) is sized for `Planeswalk`/`ChaosEnsues`'s 41 lines and the 173 Plane/Phenomenon cards' own scripts,
not amortized evenly across all 4 APIs. Deferring the replacement types and the extra-roll controller method means a
future Planechase card using either fails closed rather than resolving; each needs its own follow-up port when a real
corpus line needs it (PORT-8 discipline, not a gap left silently).

**Neutral:** `PlanechaseActive` being inferred from `PlanarDeck` occupancy rather than an explicit variant flag means a
fixture that wants Planechase-off behavior with cards physically sitting in `PlanarDeck` (e.g. a scenario testing the
zone in isolation) cannot express that combination; no real scenario need surfaced for it.

## Related

- ADR-0013 (event schema) — the 4 new trigger modes emit through the existing event pipeline, no new event kinds.
- ADR-0017 (generated registry) — `Planeswalk`/`ChaosEnsues`/`RunChaos`/`RollPlanarDice` register the normal way.
- `docs/crucible/porting/port-log/game-state/effects-batch-a.md`, `effects-batch-b.md`, `effects-batch-d.md` —
  `Planeswalk`, `ChaosEnsues`, `RunChaos`, `RollPlanarDice`'s prior deferred rows, superseded by this ADR unblocking
  them (those files stay closed per DOC-12's own discipline; the port-log index row for the landing batch says so).
- Follow-up, not scoped here: the 173 Plane/Phenomenon cards' own non-`ApiType` ability surface (their `S:`/`T:` lines
  may use APIs already ported for other reasons, or may not — unmeasured).
