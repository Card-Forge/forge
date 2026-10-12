# M6 R25a: Haunt (CR 702.55)

## Haunt lands

`K:Haunt:<SVar>` on 10 corpus cards: 3 spells (Benediction of Moons, Cry of Contrition, Seize the Soul), 7 creatures
(Absolver Thrull, Belfry Spirit, Blind Hunter, Exhumer Thrull, Graven Dominator, Orzhov Euthanist, Orzhov Pontiff). A
Haunt spell has no `A:SP$` line: its haunt SVar is the spell `castInstantOrSorcery` casts. Java builds the keyword in
two places: `CardFactoryUtil.java:1185-1272` (triggers) and `:3069-3083` (the spell).

| Piece               | Port                                                                                                                                                                                                                                                                                                                                                                                         |
| ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Compile (PORT-2)    | `compile/haunt.go` `expandHaunt`, called from `expandKeywords` in keyword order. Not in `keyword.Expand`: it reads the face's type and the SVar's text. Errors: `ErrBadHaunt` (no SVar named), `ErrMissingSVar`                                                                                                                                                                              |
| Spell               | Non-creature face: the SVar with `DB$` replaced by `SP$` (`:3076`), `Ability.Keyword` set. Java's `Cost$ 0` placeholder is dropped: `spellAdditionalCost` reads an `SP$` line's `Cost$` as an additional cost and would refuse `{0}` against `{W}`; no `Cost$` already means the card's mana cost (`setPayCosts`, `:3080`). `StackDescription$` is display only                              |
| Triggers            | Creature: ETB running the SVar (`:1243-1250`), then dies running `KWHaunt<n>` (`:1254-1265`). Spell: `Origin$ Stack \| Fizzle$ False \| Destination$ Graveyard` running `KWHaunt<n>`. Both: `TriggerZones$ Exile \| ValidCard$ Creature.HauntedBy` running the SVar (`:1206-1211`). `KWHaunt<n>` is `DB$ Haunt \| ValidTgts$ Creature \| TgtPrompt$ ...` (`:1262`)                           |
| Unhaunt             | Java's two `Static$` triggers (haunter leaves exile, haunted creature leaves the battlefield, `:1213-1237`) are not synthesized. `Card.haunting` (`hauntMark`, `card.go`) pins three values: the haunter's exile `zoneStamp`, the haunted `CardID` and its battlefield `zoneStamp`. `Card.haunts` stops matching once either object moves (CR 400.7). Same answer, no trigger, no extra pass |
| `HauntedBy`         | `valid.go`: `sourceCard.haunts(c)` (`CardProperty.java:553`). A creature on the battlefield matches by its `zoneStamp`; one that left matches by `battlefieldStamp`, which is how the dies trigger's LKI snapshot (taken after `put`, so its `zoneStamp` is already the graveyard one) still reads as haunted                                                                                |
| `hauntEffect`       | `haunteffect.go`, `HauntEffect.java`'s targeted branch: the host must be no token, in the graveyard, and the object the trigger fired for (`triggeredObjects.cardStamp`, `equalsWithGameTimestamp` at `:25`). Then `moveByEffect` to exile and set the mark. Untargeted line (Java's unhaunt) and any param past `ValidTgts$`/`TgtPrompt$` return an `error` (GO-7)                          |
| `Fizzle$`           | `moveResolvedSpellToGraveyard(controller, a, fizzled)` (`stack.go`) now gets the fizzle result from `resolveTop`. After the move, `checkSpellToGraveyardTriggers` fires the card's own `Origin$ Stack` / `Destination$ Graveyard` lines whose `Fizzle$` agrees (`TriggerChangesZone.java:151-158`, runParam set at `MagicStack.java:715-721`)                                                |
| Countered, replaced | A countered spell moves through `counterEffect`, never `moveResolvedSpellToGraveyard`: no `Fizzle$` line fires, as Java (no `AbilityKey.Fizzle`, `TriggerChangesZone.java:152`). A spell sent elsewhere (flashback, Rest in Peace) is not in the graveyard and fires nothing                                                                                                                 |
| Exile watcher       | `otherDiesTriggerMatches` walks each player's exile after `traitHosts`, for dies triggers whose `TriggerZones$` names `Exile` (effect cards excluded). The corpus has two such lines: Haunt's and suspended Nihilith's (`Origin$ Any`, so it now also fires on a death, its battlefield-origin case). Graveyard-zone dies watchers stay a gap (`game-state.md`, "Not ported yet")            |
| `cardStamp`         | New `triggeredObjects` field: the triggered card's `zoneStamp` as a "put into a graveyard" trigger fired. Set by `checkDiesTriggers`' own-card loop and `checkSpellToGraveyardTriggers`; read only by Haunt. A scalar, so `Game.Clone` copies it with the `Ability` value                                                                                                                    |

No new `PlayerController` method: the haunt target goes through `ChooseTargets` as any trigger's does. No `ADR`: the
mark extends the `exiledWith` stamp-identity model (`exiledWithMark`, `card.go`; stable `CardID`, ADR-0009) and the
compile-time keyword expansion (ADR-0038) rather than adding a mechanism.

Goldens regenerated: `compile/testdata/ast.golden` (exactly the 10 Haunt cards change),
`tools/apiscan/testdata/param-kinds.golden` (the synthesized lines add `Haunt` `DB`/`ValidTgts`/`TgtPrompt` and
`ChangesZone` `Fizzle`, and raise the counts of the SVars now compiled), hence `compile/params_gen.go` (new
`HauntParams`).

`card-smoke -cards` over the 10 cards: all 10 play.

Tests: `haunting_test.go` (cast, haunt and re-trigger for Benediction of Moons, Seize the Soul and Blind Hunter; death
by lethal damage through the state-based check; fizzle, no creature, exiled creature, haunter leaving exile, host
outside the graveyard, rejected lines). Every death path (destroy, `DestroyAll`, sacrifice, each state-based death)
reaches the exile watcher through `checkDiesTriggers`, `compile/hauntexpansion_test.go`. Fixture:
`haunt-benediction-of-moons-haunts-the-bears-and-gains-again-when-they-die`.

Not ported:

- The untargeted `DB$ Haunt` (unhaunt): no corpus line reaches it once the static triggers are not synthesized; refused.
- A haunt trigger whose controller answers no target is still pushed and fails at resolution with
  `engine: Haunt: no creature target chosen`, rather than leaving the stack silently.
