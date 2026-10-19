# ADR-0023 — Granted and Removed Abilities: a Timestamped Overlay of Compiled Traits

- **Status:** Accepted
- **Date:** 2026-09-26
- **Deciders:** `mc@archlab.pl`

## Context

CR 613.1f (Layer 6): an effect can give a permanent an ability it does not print, or remove its abilities. The corpus
does this constantly, from two directions:

| Source                                          | Corpus lines                                                                                  |
| ----------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `S:Mode$ Continuous` grants                     | `AddAbility$` 341, `AddTrigger$` 249, `AddStaticAbility$` 54, `AddReplacementEffect$` 9       |
| `S:Mode$ Continuous` removal                    | `RemoveAllAbilities$` 49                                                                      |
| One-shot effects granting traits for a duration | `Animate` 308, `AnimateAll` 68, `Clone` 18, `CopyPermanent` 10 (`Abilities$`, `Triggers$`, …) |

Constructed examples: Urza's Saga (`DB$ Animate | Abilities$ ABMana`), Chromatic Lantern and Cryptolith Rite (static
`AddAbility$`).

Java keeps printed traits on the card's current `CardState` and layers two timestamp-keyed tables over them —
`changedCardTraitsByText` (Layer 3) and `changedCardTraits` (Layer 6), `Table<timestamp, staticAbilityId, changes>`
(`Card.java:141-142`) — merged in layer then timestamp order on every read (`Card.java:4913-4920`), with a Layer-4
land-trait slot between them (`getLandTraitChanges`, `:4917`) through which a basic land type grants or strips abilities
(CR 305.7). A grant's SVar is parsed into an ability object at apply time and cached per static ability and substituted
text (`Card.java:4716-4718`, `:4857`; `StaticAbilityContinuous.java:336`). `RemoveAllAbilities$` removes everything with
an earlier timestamp than the removing effect (`StaticAbilityContinuous.java:327-328`). Perpetual changes reuse the same
Layer-6 table (`PerpetualAbilities.java:14-16`).

This port reads traits straight off the compiled definition: `Def.Faces[…].Triggers` in 41 range loops, `.Statics` in
18, `.Replacements` in 15, `.Abilities` in 3. Layer 1 copies already swap `Def` for a per-game compiled definition
(`effects-clone.md` (git `624d87dba16`)), so copies are covered. Grants are not: `Animate` rejects every trait-granting
param because "each needs a runtime-parsed trait, PORT-2" (`animate.go:143-157`), and a static `AddAbility$` line has no
applier at all — the grant silently does not happen (`effects-manareflected.md` (git `624d87dba16`), "Caveat").

## Decision Drivers

- PORT-2: no script text is parsed after load. Java parses grant SVars at apply time; this port cannot.
- CR 613 timestamp order between grants and `RemoveAllAbilities$`, with Java's merge order as the parity reference.
- GO-7 and ADR-0011: a grant the engine cannot apply must fail loudly, never be silently absent.
- 77 read loops; the fix must not depend on each one remembering an overlay.

## Considered Options

1. **Build a fresh per-game `*compile.Card` for each granted permanent, as copies do.** Rejected: grants come and go
   every state-check pass and stack by timestamp; rebuilding a definition per pass per card is the wrong grain, and a
   removal would have to know which slices it owns.
2. **Parse the SVar at apply time and cache it, as Java does.** Rejected: PORT-2.
3. **Compile grant SVars at load; keep a per-card timestamped overlay of compiled traits; read all traits through
   accessors that merge definition and overlay.** Chosen.

## Decision

1. **`compile` resolves every SVar a grant names into compiled abilities at load** — `AddAbility$`, `AddTrigger$`,
   `AddStaticAbility$`, `AddReplacementEffect$` on `S:` lines and `Abilities$`/`Triggers$`/`staticAbilities$`/
   `Replacements$` on the granting effects — extending the `effectTraitKeys` precedent (`compile.go:570-582`). A value
   Java substitutes into grant text at apply time (a mana cost, a CMC) becomes a typed amount on the compiled ability,
   evaluated at use; nothing is re-tokenized.
2. **A card carries an overlay of trait changes keyed by (timestamp, source)**: compiled abilities added, and a removal
   predicate (`RemoveAllAbilities$` and its narrower forms). Continuous grants are rebuilt from scratch each state-check
   pass, like every other continuous effect today (`action.go:173-188`); one-shot grants carry their own duration, as
   `Game.pumps` does. `Game.Clone` copies the overlay.
3. **All trait reads go through accessors on `Card`** — abilities, triggers, statics, replacements — that return the
   definition's traits merged with the overlay in timestamp order, a removal hiding everything older than itself
   (`Card.java:4913-4920` order). Every one of the 77 loops moves to them. A loop left reading `Def.Faces` directly is a
   review finding.
4. **A grant the port cannot apply fails closed with an error when applied**, the way `Animate` rejects its trait params
   today — never a silent omission. The coverage command (`cmd/crucible/coverage.go`) checks card presence only;
   extending it to report unapplicable grants at load is part of the implementing work. The `S:` lines applied today
   stay applied.
5. **Layer 3 text-changing is its own decision** (`ChangeText` 17 lines, `GainTextOf$` 1):
   [ADR-0039](0039-layer3-word-substitution.md). Java stores it as a substitution on the ability object
   (`changeTextIntrinsic`), which fits this overlay without re-parsing.

## Consequences

**Good:** unblocks the 686 distinct static grant and removal lines and 404 effect-side grant lines above, including
Urza's Saga. Grants become visible to every trait scan at once, instead of per scan.

**Bad:** 77 loops change in the implementing commits, and every trait read pays for a merge. The merge is a no-op return
of the definition's slice when the overlay is empty, which is nearly every card.

**Neutral:** Layer 1 copy keeps its `Def`-swap model; the overlay applies over whichever definition is current, exactly
as Java's tables apply over the current `CardState`.

## Related

ADR-0007 (card DSL representation), ADR-0011 (coverage gate), ADR-0017, `effects-clone.md` (git `624d87dba16`),
`effects-manareflected.md` (git `624d87dba16`), [04-adr-process](../guidelines/04-adr-process.md)
