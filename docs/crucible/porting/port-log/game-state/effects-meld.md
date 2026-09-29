# Effects: Meld (ADR-0032)

One script-driven `ApiType` resolves, 183 of the corpus's 203. Implements
[ADR-0032](../../../adr/0032-meld-two-cards-one-permanent.md): two named cards exiled and returned as one permanent (CR
712), split back apart when it leaves the battlefield (CR 712.4c). Ported from `MeldEffect.java`'s `resolve`, the
spin-off in `GameAction.changeZone` (`GameAction.java:629-642`) and `PlayerZoneBattlefield.addToMelded`
(`PlayerZoneBattlefield.java:45-49`).

## Meld lands

**Corpus.** 7 real lines, one per meld pair. 6 resolve, 1 rejected.

| Card (primary + secondary)                          | Shape                                                                    | Status                                 |
| --------------------------------------------------- | ------------------------------------------------------------------------ | -------------------------------------- |
| Gisela, the Broken Blade + Bruna, the Fading Light  | End-step trigger, `IsPresent2$ Creature...+namedBruna; the Fading Light` | Resolves (`TestMeldGiselaAndBruna...`) |
| Graf Rats + Midnight Scavengers                     | Begin-combat trigger, same `IsPresent2$` shape                           | Resolves                               |
| Titania, Voice of Gaea + Argoth, Sanctum of Nature  | Upkeep trigger, `CheckSVar$`, `SecondaryType$ Land`                      | Resolves (`TestMeldTitania...`)        |
| Hanweir Battlements + Hanweir Garrison              | `AB$ Meld`, `Cost$ 3 R R T`, `ConditionPresent$` + `ConditionCheckSVar$` | Resolves (`TestMeldHanweir...`)        |
| Urza, Lord Protector + The Mightstone and Weakstone | `AB$ Meld`, `Cost$ 7`, `SecondaryType$ Artifact`, `SorcerySpeed$`        | Resolves (`TestMeldUrza...`)           |
| Mishra, Claimed by Gix + Phyrexian Dragon Engine    | `AttackersDeclared` chain, `Tapped$ True`, `Attacking$ True`             | Resolves (`TestMeldMishra...`)         |
| Vanille, Cheerful l'Cie + Fang, Fearless l'Cie      | Main1 trigger executing `AB$ Meld \| Cost$ 3 B G`                        | Rejected: triggered `Cost$` never paid |

**Resolution order** (`MeldEffect.java`): secondary candidates = activator's battlefield, owned by activator, name
`Secondary$`, type `SecondaryType$` (default `Creature`); none → no-op. One pick (`ChooseCardsForEffect`, 1..1). Host
must still be on the battlefield (below). Host then secondary exiled (`exileCards`: exiled triggers, one
`ChangesZoneAll` batch), both removed from combat. Gate: both in exile, names still `Primary$`/`Secondary$` (a copy
effect ended on the way out), neither a token (`cloneOrigin` is set only on token copies, `TokenEffectBase.java:176`).
Failing the gate leaves both exiled, as Java does. Then the meld face swaps in, the secondary is folded in, and the host
enters under the activator (`moveByEffect`: ETB replacements and triggers from the meld face, starting loyalty from it —
Urza, Planeswalker enters with 7).

**`Name$`** is inert: nothing in `MeldEffect.java` or `MeldAi.java` reads it; it repeats the meld face's name for the
description.

## Engine pieces

| Piece                            | Where                                          | Why                                                                                                                                                                 |
| -------------------------------- | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Card.MeldedWith`, `Card.Melded` | `card.go`                                      | ADR-0032's pair state; plain values, `Game.Clone`'s `copy` carries them                                                                                             |
| Meld face swap                   | `meldeffect.go`                                | `transform()`'s own idiom: synthetic back `compile.Card`, `frontDef` holds the front; `turnFrontFaceUp` reverts on leaving                                          |
| Split back apart                 | `Card.leaveMeld`/`Game.unmeld` (`game.go`)     | Hooked in `Move` and `MoveToLibraryTop`'s leave-battlefield branches; non-melded moves pay one `NoCard` compare                                                     |
| `named<Name>` valid property     | `valid.go` (`sharesName`)                      | `CardProperty.java:62-68`; every corpus line's trigger or condition reads it. Previously false for every card (Play rejected it)                                    |
| Live faces for trigger scans     | `liveFaces` (`trigger.go`)                     | `triggerFaces` read every face of `Def`: Mishra, Claimed by Gix attacking fired Mishra, Lost to Phyrexia's attack charm. See below                                  |
| Leave combat on meld             | `removeFromCombat` in `meldeffect.go`          | `GameAction.java:424-429`; `Move` does not, and `combatdamage.go:157` counts any attacker whose `Zone` is `Battlefield` -- a melded Dragon Engine would deal damage |
| `Attacking$ True`                | `attackByEffect` (`changecombatantseffect.go`) | `SpellAbilityEffect.addToCombat` (`SpellAbilityEffect.java:758-793`); extracted from `ChangeCombatants`, shared                                                     |

**Where the secondary lives.** Java's `addToMelded` removes the card from its zone, points its zone at the battlefield
and adds it to a separate `meldedCards` list, never to the zone's card list: `getCards()`, `contains()`, `size()` never
see it. Ported literally (ADR-0032, corrected in place to this mechanism): `Zone`/`ZoneOwner` read
`Battlefield`/activator, `Melded` is set, and the card is in no `Zone`'s set. Every battlefield enumeration (84
`Zone(Battlefield, ...)` walks, `CardsIncludingPhasedOut`, the fixture dump) excludes it with no per-walk check. Two
raw-arena scans that read every `CardID` ever allocated instead of a zone's own set (`RemoveFromMatch`'s `RemoveType$`,
`Intensify`'s `AllDefined$`, both predating this ADR) needed their own explicit `Melded` skip, found on rules review of
the merged commit -- the concrete instance of the "not present-and-skipped, simply absent" trap the ADR's own
Consequences section names. Only `MeldedWith` reaches the secondary otherwise. Stale references (targets) are cut off by
`zoneStamp`: the meld does not restamp the secondary after its exile.

**Split back (CR 712.4c).** `leaveMeld` runs after the LKI snapshot, so last-known information keeps `MeldedWith` and
the meld face (Brisela's dies triggers read it). After the primary's own `ZoneChanged` event, `unmeld` puts the
secondary into the same zone kind and owner, appended after the primary (graveyard, hand, exile, library bottom) or over
it on the library's top -- Java's `changeZone(null, zoneTo, unmeld, position, ...)` at the primary's own position. Java
moves it from no zone: no leave-the-battlefield cleanup, no LKI, no effect-card watch. Crucible's `ZoneChanged` for it
names `Battlefield` as `From`. A secondary moved on its own first (a stale reference) drops `Melded` in `leaveMeld`, so
the split leaves it where it went.

**Live faces.** `Card.getTriggers` reads the current state alone. This port's current face is `Faces[0]` for a
transforming, flipping, modal, melded, specialize or prepare card: a transformed or melded card's `Def` is its back
face, no engine path flips a flip card yet (`setstateeffect.go`'s own "Flip... not resolved" gap, so an unflipped card's
`Def` still carries both faces' real data) or puts a modal card's back face into play (`playCastGap` rejects its choice
of spells; `cloneDef` reads faces the same way), and Prepare joins for consistency with Java's own
`CardSplitType.java:7-16` classification (0 real corpus Prepare lines carry a trigger on their alternate face today, so
it is corpus-inert, not exempted on principle). So `triggerFaces` now yields only `Faces[0]` for those split types.
Split, adventure and omen cards, and a Room's both-doors view, keep every face. Before this, every meld front face
carried its meld face's triggers: Graf Rats entering fired Chittering Host's pump, Titania, Voice of Gaea entering fired
Titania, Gaea Incarnate's land return, Vanille dying fired Ragnarok's destroy -- and every transform DFC's back-face
triggers fired on its front face. Scenario `meld-face-trigger-not-live-on-front-face` pins it: Titania, Voice of Gaea is
cast with a Forest in the graveyard, and the Forest stays there. Flip was missed in the first pass (rules review on the
merged commit): a flip card's own back face carries a real, non-corpus-inert trigger in 4 of 20 real
`AlternateMode:Flip` lines, confirmed live via Nezumi Shortfang firing Stabwhisker the Odious's "each opponent's upkeep"
trigger unflipped; `TestFlipFaceTriggersAreNotLiveOnTheFrontFace` pins the fix.

## Rejected with an error before acting

| Shape                                       | Why                                                                                                                   |
| ------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `Cost$` on anything but the host's A:       | Vanille's trigger-executed `AB$ Meld \| Cost$ 3 B G`: no triggered ability's cost is asked for; free meld wrong       |
| `Attacking$` other than `True`, `Blocking$` | `addToCombat`'s defined-defender and blocker branches; 0 corpus lines                                                 |
| `Condition$`                                | `subAbilityConditionMet` would skip it silently                                                                       |
| No `Primary$`/`Secondary$`                  | Java's `sharesNameWith(null)` fails after exiling both                                                                |
| Host no longer on the battlefield           | Java rechecks a trigger's `IsPresent$` at resolution (CR 603.4); Crucible does not, and would meld from the graveyard |
| Any `Mode$ CantExile` static in play        | `Card.canExiledBy` not modeled (same stance as `heisteffect.go:23`); 1 corpus card                                    |

## Not ported

| Gap                                                                                                                                 | Java                                                                               |
| ----------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| Melded mana value = sum of both front faces; `CMC()` reads the meld face's `no cost`, 0                                             | `Card.java:7244-7250`                                                              |
| The secondary joins the caller's `ChangesZoneAll` batch on a split                                                                  | `storeChangesZoneAll`, `GameAction.java:641`                                       |
| `ChangeZone` `Remember$`/exiled-with and `WithCountersType$` reaching the melded card                                               | `ChangeZoneEffect.java:790-796`, `:1452-1485`                                      |
| Fixture `\|Meld:<Name>` dump and load; no scenario fixture of a melded permanent for that reason (Transformed is not ported either) | `GameState.java:322-330`, `:1322-1334`                                             |
| Commander-ness through the melded card                                                                                              | `Card.java:7282`, `:7304`                                                          |
| `sharesName`'s own `hasNonLegendaryCreatureNames()` tail (SpyKit's text-changing ability)                                           | `Card.java:5864-5868`; 0 real `named<Name>` lines reach a SpyKit-shaped card today |

**Forge bug** (`forge-java-defects.md`): `GameAction.java:635-640` computes `unmeldPosition` and passes `position`, so
the comment's "ask controller if it wants to be on top or bottom" never happens. Reproduced for oracle parity.
