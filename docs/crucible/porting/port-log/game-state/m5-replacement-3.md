# Port Log — Game State: M5 batch L, replacement remainder 2

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `replacemententry.go`, `replacementevents.go`,
  `replacementfaces.go`, `replacement.go`

Siblings: [`m5-replacement.md`](m5-replacement.md), [`m5-replacement-2.md`](m5-replacement-2.md).

## Moved to the battlefield: the replacement runs before the move

Java runs `ReplacementHandler.run(Moved)` in `GameAction.changeZone` before the card leaves its zone. `Replaced` returns
at once (`GameAction.java:334-365`): no zone change, no table entry, no ETB trigger. The `ChangeZone` ability of the
`ReplaceWith$` chain moves the card through a second, full `changeZone`, and `ReplacementEffect.hasRun` keeps the same
line from applying to that nested move (`ReplacementHandler.java:137`, `:227-228`).

`Game.entryReplaced` (`replacemententry.go`) does the same. Each of the four entry sites (`permanentEffect`,
`attachEffect`, `playLandNow`, `moveByEffect`) calls it before `Game.Move`; a `true` skips the move, the enter
replacements and the ETB triggers, since the chain's own `ChangeZone` ran them. `runReplacements` keeps the applied line
in `Game.replacing` for the length of the chain, which is `hasRun`.

| Shape | Layer | Corpus lines |
| --- | --- | --- |
| `PayBeforeETB`/`SacBeforeETB` chain | Other | 9: Lake of the Dead, Mox Diamond, Balduvian Trading Post, Heart of Yavimaya, Lotus Vale, Scorched Ruins, Kjeldoran Outpost, Soldevi Excavations, Sheltered Valley |
| `ReplaceWith$ Exile` (`ChangeZone Defined$ ReplacedCard`) | Other | 2: Containment Priest, Primeval Spawn |
| `DBChooseOpp` + `ChangeZone GainControl$ ChosenPlayer` | Control | 3 of 3 `Layer$ Control` Moved lines on a card: Captive Audience, Pendant of Prosperity, Xantcha, Abby |
| `ReplacementResult$ Updated` + `LoseLife` | Other | 1: Lich |

- The entering card is matched as it would exist on the battlefield under the player it enters for (`card.controller`
  set for the check), so a `ValidCard$ Card.Self` line is read off a card in hand, on the stack or in a graveyard.
- Control runs before Other (`ReplacementLayer.java:9-13`); each layer is its own `runReplacements` walk.
- `Origin$ All` is every zone a card can be in (`anyZoneOrigin`); it no longer makes a `ChangeZone` hidden by itself.
- `PlayLand` counts the land drop and fires `LandPlayed` whether or not the land entered (`Player.java:1645-1650`): a
  Lake of the Dead sent to the graveyard still used the drop (PORT-7).
- `Discard` gained `Optional$`, `DiscardValid$` and `RememberDiscarded$` (`DiscardEffect.java:255-264`,
  `Player.java: 1441-1442`); `Sacrifice` gained `StrictAmount$` (`SacrificeEffect.java:137-144`); `LoseLife` and
  `Discard` default `Defined$` to You.
- `wasCast` and `CastSa Spell.ManaSpent <op><n>` valid properties: `Card.wasCast` is set when a cast is announced and
  cleared on every move except stack to battlefield (CR 400.7), `castManaSpent` is the shards, generic and X paid.

Layer order is Control, Copy, Other, all before the move: a Clone entering under Containment Priest copies first and is
exiled as the copy ([`layers-n-copy.md`](layers-n-copy.md#the-copy-layer-runs-before-the-move)).

Fixture verbs added: `queue cardchoice none` (an empty pick), `queue playerchoice <p>`, `queue colorchoice <c>`,
`queue option <n>`.

| Scenario | Proves |
| --- | --- |
| `replacement-moved-lake-of-the-dead-sacrifices-a-swamp` | sacrifice, then the chain's `ChangeZone` enters the land |
| `replacement-moved-lake-of-the-dead-no-swamp-goes-to-the-graveyard` | X is 0: graveyard, land drop still used |
| `replacement-moved-mox-diamond-discards-a-land` | `Optional$ Discard`, `RememberDiscarded$`, cast from stack |
| `replacement-moved-mox-diamond-declined-goes-to-the-graveyard` | empty pick: graveyard |
| `replacement-moved-scorched-ruins-sacrifices-two-untapped-lands` | `Amount$ 2`, the entering land is not a candidate |
| `replacement-moved-scorched-ruins-with-one-land-goes-to-the-graveyard` | `StrictAmount$`: nothing sacrificed |
| `replacement-moved-sheltered-valley-sacrifices-the-other-valley` | `SacrificeAll` before the new Valley is in play |
| `replacement-moved-containment-priest-exiles-a-reanimated-creature` | not cast: exiled instead of entering |
| `replacement-moved-containment-priest-lets-a-cast-creature-enter` | `!wasCast` fails for a cast spell |
| `replacement-moved-primeval-spawn-reanimated-is-exiled` | self line read from the graveyard |
| `replacement-moved-primeval-spawn-cast-for-mana-enters` | `CastSa Spell.ManaSpent EQ0` fails |
| `replacement-moved-captive-audience-enters-under-an-opponent` | Control layer, `GainControl$ ChosenPlayer` |
| `replacement-moved-pendant-of-prosperity-enters-under-an-opponent` | the same with `DBCleanup` |
| `replacement-moved-lich-loses-all-life-as-it-enters` | `Updated`: loses life, then enters, survives at 0 |
| `replacement-moved-lich-at-seven-life-enters-at-zero` | `LifeTotal` read when the line runs |

## Not landed from the Moved remainder

- `PayLife` (Minion of the Wastes, Nameless Race, Phyrexian Processor) is ported: see
  [`m5-q-attached.md`](m5-q-attached.md) (`payReplacementLifeX`).
- "Enters with counters" (`PutCounter | ETB$ True`) was already ported (`entercounters.go`, ADR-0038); the old row was
  stale.
- Xantcha, Abby: the same `Layer$ Control` shape as Captive Audience; no fixture of their own.
- Amulet-style `ChooseP`/`ChooseCT`: unchanged.

## Event$ RollDice

`Game.rollReplaced` (`replacementevents.go`) runs before the dice are rolled, as `RollDiceEffect.rollAction` does
(`RollDiceEffect.java:403-421`). The line's ability chain edits the dice count (`VarName$ Number`) and the lowest rolls
ignored (`VarName$ Ignore`); `replacementEvent.vars` carries the second number. `ValidPlayer$` and `ValidSides$` are
`ReplaceRollDice.canReplace`'s. Pixie Guide, Wyll, Barbarian Class resolve. Vedalken Squirrel-Whacker's
`DicePTExchanges` (`VarType$ CardSet`) is not modelled: `rollReplacementResolvable` is false for it, so
`diceModifierInPlay` still fails the roll closed.

Tests: `rolldicereplaced_test.go` (extra die and ignore, `ValidSides$`/`ValidPlayer$` gates, the exchange fails closed).

## Event$ Attached

`Game.attachTo` (`replacementfaces.go`) is `Card.attachToEntity` for a card host: attaching to the object already
attached to does nothing (`Card.java:3929`), otherwise `Game.Attach` and then `attachedReplaced` (`ReplaceAttached`:
`ValidCard$` against the attachment, `ValidTarget$` against the host; the result is not read). Every site that attaches
a card to a card calls it (`attachEffect`, `attachObject`, `attachActivated`, `tokenEffect`). Player hosts
(`AttachToPlayer`) are not run through it: no corpus `Attached` line has a player `ValidTarget$`.

| Scenario | Proves |
| --- | --- |
| `replacement-attached-sanctuary-blade-protects-from-the-chosen-color` | red chosen as it attaches: a Bolt on the Bears fizzles |
| `replacement-attached-sanctuary-blade-protects-only-from-the-chosen-color` | green chosen: the Bolt kills the 4/2 |

Psychic Paper is ported ([`m5-q-attached.md`](m5-q-attached.md)). Not done: Paleontologist's Pick-Axe (its back face,
Dinosaur Headdress, needs `ExiledWith` and craft).

## Transform and TurnFaceUp scenarios

The fixture format gained `|Transformed` (`GameState.java:360-366`, `:1346`): `Game.Transform` and
`Card.InTransformedState`, so a transformed permanent loads and dumps by its front face's name.

| Scenario | Proves |
| --- | --- |
| `replacement-transform-zenos-chooses-an-opponent-as-it-becomes-shinryu` | `Event$ Transform` `ChoosePlayer` runs once Zenos is Shinryu |
| `replacement-transform-zenos-does-not-transform-when-another-creature-dies` | `ChosenCardStrict`: another death does not transform |
| `replacement-turn-face-up-aquamorph-entity-chooses-one-power-five-toughness` | `GenericChoice` branch 0 (1/5) survives a Bolt |
| `replacement-turn-face-up-aquamorph-entity-chooses-five-power-one-toughness` | branch 1 (5/1) dies to it |

Not done:

- Sephiroth, Fabled SOLDIER transforms ([`m5-q-attached.md`](m5-q-attached.md), `Count$ResolvedThisTurn`). Curse of
  Leeches transforms through day/night, which fixtures cannot set; Ludevic needs `XMin1 ... ExileFromGrave<X>` and a
  `Clone` with `Choices$ Creature.ExiledWithSource`.
- Gift of Doom and Vesuvan Shapeshifter resolve ([`m5-t-replace-search.md`](m5-t-replace-search.md)).

## Draw, GainLife, DamageDone remainder

Every `ReplaceWith$` ability the hand-run shapes do not cover now resolves through the Registry with the affected player
as the replaced player (`Defined$ ReplacedPlayer`), `SubAbility$` chain included (`runReplacementChain`). Before, such a
line was skipped silently.

- `Draw`: Blood Scrivener's `DrawTwo` + `DBLoseLife`, Laboratory Maniac's `WinsGame` and Magus of the Chains' `Discard`
  have a test each. The other lines of the 29 resolve exactly when the Registry resolves their ability (Dig, ExileTop, a
  Treasure token, ...); none has its own test. A chain that errors records a pending error. Still skipped by
  `drawReplacementMatches`: `ValidCause$` (the cycling line), `FirstExtraCardDrawnThisTurn$` (Reed Richards) and the
  `Optional$ Prevent$` line.
- `GainLife`: `ValidSource$ SpellAbility` and `SourceController$ True` read the causing ability (`gainLife` takes
  `cause`; lifelink, SetLife and ExchangeLife pass nil). Rain of Gore resolves.
- `DamageDone`: a `ReplaceWith$` naming an API that is not one of the `Replace…` effects replaces the damage with the
  chain (the 17 "different `DB$` API" lines). A `Replace…` effect that cannot run keeps the old silent skip, since one
  that ran and changed nothing looks the same.

| Scenario | Proves |
| --- | --- |
| `replacement-draw-laboratory-maniac-wins-from-an-empty-library` | `WinsGame` replaces the draw before the 704.5b loss |
| `replacement-draw-laboratory-maniac-with-cards-left-draws-normally` | `IsPresent$ ... EQ0` gate |
| `replacement-draw-blood-scrivener-hellbent-draws-two-and-loses-life` | chained substitute, `hasRun` for the nested draws |
| `replacement-gain-life-rain-of-gore-turns-a-spell-gain-into-life-loss` | spell gain becomes life loss |
| `replacement-gain-life-rain-of-gore-leaves-lifelink-alone` | lifelink has no `SourceSA` |
