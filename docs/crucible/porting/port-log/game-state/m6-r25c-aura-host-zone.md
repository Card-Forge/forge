# Port Log — Game State: an Aura whose host is a card outside the battlefield (R25c)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `castspell.go` (`enchantTargets`, `castAura`,
  `attachEffect`), `action.go` (`enchantSpec`, `enchantsOutsidePlay`, `attachmentHostInPlay`,
  `cleanupDanglingAttachments`), `targeting.go` (`auraTargetStillLegal`), `attachobject.go` (`attachDefined`,
  `canBeAttached`); [`internal/smoke`](../../../../../crucible/internal/smoke) (`tailor.go`)

Java: `CardState.getAuraSpell` (`CardState.java:665`), `Card.cantBeEnchantedByMsg` (`Card.java:6978`),
`GameEntity.cantBeAttachedMsg` (`GameEntity.java:236`), `Card.attachToEntity` (`Card.java:3936`), `AttachEffect.resolve`
(`AttachEffect.java:31`), `GameAction.stateBasedAction704_attach` (`GameAction.java:1777`).

Every Aura spell is `SP$ Attach | ValidTgts$ Card.CanBeEnchantedBy,... | TgtZone$ Battlefield,Graveyard`. The zone list
is fixed for all Auras; what stops Pacifism from enchanting a graveyard card is `cantBeEnchantedByMsg`: the host must
match the `Enchant` type **and** (the type contains `inZone` **or** the host is in play). Corpus: `K:Enchant:` with an
`inZone` type is Animate Dead and Dance of the Dead (`Creature.inZoneGraveyard`) and Spellweaver Volute; Daybreak
Coronet and Decomposition were on the R25c list but name battlefield hosts (a creature with another Aura; a black
creature), see "Two other shapes the R25c list held". A whole-corpus search for `^K:Enchant:.*inZone` finds exactly
those three cards.

| Piece                  | Go                                                                                         | Java                                           |
| ---------------------- | ------------------------------------------------------------------------------------------ | ---------------------------------------------- |
| Zone predicate         | `enchantsOutsidePlay(spec)`: the spec text contains `inZone`                               | `v.contains("inZone")`, `Card.java:6986`       |
| Cast-time candidates   | `enchantTargets`: battlefield, then every graveyard when the predicate holds               | `TgtZone$ Battlefield,Graveyard`               |
| Other `inZone` zones   | Match nothing searched (`inZoneExile` finds no host), the cast is refused                  | same: only two zones are searched              |
| Hexproof, protection   | `cardCantBeTargetedBy` as for a battlefield host (its own `Zone != Battlefield` early out) | `canBeTargetedBy`                              |
| Resolution re-check    | `auraTargetStillLegal`: battlefield, or graveyard under the predicate; host re-matches     | `Attach` target legality                       |
| Enters attached        | `attachEffect` unchanged: `Move` to the battlefield, `attachTo` the graveyard card         | `moveToPlay`, `attachToEntity`                 |
| Stays attached         | `attachmentHostInPlay` in `cleanupDanglingAttachments`; `Matches` then decides             | 704_attach via `canBeAttached(c, null, true)`  |
| Current `Enchant` type | `enchantSpec` reads `KeywordLines()`, not the printed face                                 | `aura.getKeywords(Keyword.ENCHANT)`            |
| Re-attach              | `DB$ Attach \| Defined$ X` (no `ValidTgts$`, no `Object$`): `attachDefined`                | `AttachEffect.resolve`, `Defined` branch       |
| Attach gate            | `canBeAttached` adds the in-play-or-`inZone` rule for an Aura                              | `attachToEntity` -> `canBeAttached` -> message |

## Animate Dead and Dance of the Dead end to end

The Aura enters attached to the graveyard card. Its `Mode$ ChangesZone` enters trigger runs `ChangeZone` on
`Defined$ Enchanted` (`GainControl$ True`, `RememberChanged$ True`), then `Animate` on itself with
`RemoveKeywords$ Enchant:Creature.inZoneGraveyard` and `Keywords$ Enchant:Creature.IsRemembered`, then `Attach` to
`Defined$ Remembered`. The Go card keeps its ID across the move, so the Aura is still attached when the creature reaches
the battlefield and the final `Attach` finds it already there (`attachTo` no-ops, so the `Attached` trigger does not run
a second time; Java attaches the new object, a gap with no corpus card relying on it). The state-based action pass runs
after the whole chain, by which time `enchantSpec` reads the swapped keyword.

Because the Aura is attached to a graveyard card between entering and the trigger resolving,
`cleanupDanglingAttachments` must not read "host not on the battlefield" as illegal for an Aura under the predicate; it
does for every other Aura. A host that leaves the graveyard (exiled in response) makes `Creature.inZoneGraveyard` fail
and the Aura goes to the graveyard, Java's outcome too.

## `attachDefined`

`DB$ Attach | Defined$ X` without `ValidTgts$` or `Object$` attaches the source itself. One host is no decision (Java
asks the chooser); several, a player host, `Optional$`, `Chooser$`, `Choices$`, `PlayerChoices$` and `Move$` return an
`error` (GO-7). The attachment must still be on the battlefield and pass `canBeAttached`. `RememberAttached$` remembers
the source on itself (`AttachEffect.java:149-151`).

## Leaving the battlefield

Animate Dead's delayed trigger (`Mode$ ChangesZone | Origin$ Battlefield | ValidCard$ Card.Self`, no `Destination$`)
sacrifices the remembered creature. `delayedWatchesLeaving` treated an absent `Destination$` as no match; Java accepts
every destination (`TriggerChangesZone.java:85`), so the trigger never fired and the creature stayed. Dance of the Dead
reads the creature through `Defined$ DirectRemembered` (`AbilityUtils.java:231`), now resolved as the host's remembered
cards; a delayed trigger whose host already ran its `ClearRemembered$` cleanup falls back to the delayed trigger's own
`RememberObjects$`, standing in for the host's last-known remembered list, which this port does not keep.

## Two other shapes the R25c list held

| Card                      | Cause                                                                                                      | Fix                                                                          |
| ------------------------- | ---------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| Daybreak Coronet          | `Creature.EnchantedBy Aura.Other`: the property name has a nested spec, which `propertyMatches` left false | `EnchantedBy <spec>` matches when one of the candidate's attachments matches |
| Decomposition, Lion Umbra | the fixed probe board has no black creature, no modified creature                                          | `tailor.go` (below)                                                          |

`EnchantedBy Imprinted` and `EnchantedBy Targeted` (`CardProperty.java:469-481`) stay false, as before.

## Probe

`internal/smoke/tailor.go` adds an Aura's `Enchant` spec to `targetSpecs`, so the tailored variant puts a matching card
on the battlefield (Decomposition: a black creature). A spec naming a zone is skipped: the fixed board already has a
creature card in each graveyard. A spec asking for an enchanted or modified creature (`EnchantedBy`, `modified`) gets a
Grizzly Bears wearing Pacifism on each side (`addWornAura`), because the tailor judges candidates in hand where none can
be enchanted. After the change Animate Dead, Dance of the Dead, Daybreak Coronet, Decomposition, Lion Umbra and
Spellweaver Volute all play. A whole-corpus run before the Coronet and Lion Umbra probe additions measured 28775 plays,
4654 refused and 293 inconclusive of 33429 judged (86.1%).

## Gaps

- An Aura that loses every ability (a lose-all-abilities static) loses its `Enchant` keyword; Java then reports "No
  Enchant Keyword" and the Aura is illegal (`Card.java:6979`). `cleanupDanglingAttachments` reads "no checkable
  `Enchant`" as legal, as it always did for an Aura with none (the unit tests build such Auras on purpose).
- Several `Enchant` keywords on one Aura: Java requires every one to hold, `enchantSpec` reads the first.
- Java attaches the reanimated creature's new object and fires `Attached` for it; here the Aura stays attached to the
  same ID, so that trigger does not run a second time.

## Tests

Scenarios `animate-dead-reanimates-the-creature-card-it-enchants`, `dance-of-the-dead-reanimates-its-host-tapped`,
`animate-dead-leaving-sacrifices-the-reanimated-creature`,
`dance-of-the-dead-leaving-sacrifices-the-reanimated-creature` and
`daybreak-coronet-enchants-a-creature-with-another-aura`; module tests in `auraoutsideplay_test.go` (graveyard cast,
choice across graveyards, no host for `Creature` / `inZoneExile`, fizzle when the host leaves, falls off when the host
leaves, nested `EnchantedBy`, `attachDefined` refusals and `RememberAttached$`).
