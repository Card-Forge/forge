# Port Log — Game State: Pump `KW$` hidden lines, ChangeZone `TargetsWithDefinedController$` (R9)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `pumpeffect.go` (`pumpGrant`, `pumpKeywords`,
  `pumpPlayers`), `pumpalleffect.go`, `continuous.go` (`pumpHiddenKeywords`, `endNextUntapStepKeyword`,
  `hiddenKeywordRead`, `keywordTokenDynamic`), `game.go` (`pumpRecord.Hidden`), `turn.go` (`untapStep`), `card.go`
  (`untapNextStepKeyword`), `changezoneeffect.go` (`changeZoneShufflers`), `targeting.go` (`targetChoiceFor`,
  `resolveTargets`), `chaintargets.go`, `copyspellabilityeffect.go`
- **Java:** `PumpEffect.java:44-73`, `:94-123`, `:127-147`; `PumpAllEffect.java:28-93`; `Card.java:5358-5384`
  (`addHiddenExtrinsicKeywords`, `removeHiddenExtrinsicKeyword`), `Card.java:4812-4823` (`canUntap`);
  `Untap.java:157-161`; `ChangeZoneEffect.java:872-890`; `SpellAbility.java:1405-1416`; `TargetSelection.java:70-120`
  (forge-gui)

## What `KW$ "_"` refused

146 smoke-probe cards were refused with `Pump: KW$ "_" not resolvable yet` (136) or its `PumpAll` twin (10); the
top-causes table counts only the 94 refused at the `resolve` stage. Every one was a `KW$` line carrying a `HIDDEN`
token. No variable-keyword shape (Protection, Ward, Landwalk, ...) was among them: `pumpKeywords` passes those to Layer
6 unrefused (this batch checked the refusal, not how Layer 6 applies each one). Corpus count of the refused tokens,
`(AB|SP|DB)$ Pump`/`PumpAll` lines:

| Hidden line                                                                                       | Pump | PumpAll | Status                                       |
| ------------------------------------------------------------------------------------------------- | ---- | ------- | -------------------------------------------- |
| `CARDNAME can't block.`                                                                           | 112  | 4       | Resolved (`canBlockAtAll`)                   |
| `This card doesn't untap during your next untap step.`                                            | 94   | 11      | Resolved (`untapBlocked`, untap removal)     |
| `CARDNAME must be blocked if able.`                                                               | 15   | 1       | Resolved (block requirements)                |
| `All creatures able to block CARDNAME do so.`                                                     | 12   | 0       | Resolved (block requirements)                |
| `CARDNAME can't attack or block.`                                                                 | 9    | 0       | Resolved (both halves)                       |
| `CARDNAME can't attack.`                                                                          | 5    | 0       | Resolved (`canAttackAtAll`)                  |
| `During your next untap step, as you untap your permanents, return CARDNAME to its owner's hand.` | 1    | 0       | Refused: bounce at `Untap.java:93` not built |
| `Damage that would be dealt to CARDNAME can't be redirected.`                                     | 1    | 0       | Refused: no damage redirection               |
| `Unblockable`                                                                                     | 1    | 0       | Refused: read by no Java code (below)        |

## Hidden lines are pump-record state

Java splits `KW$` in `applyPump`: a token starting `HIDDEN` loses the prefix and goes to
`addHiddenExtrinsicKeywords(timestamp, 0, ...)`, the rest to `addChangedCardKeywords`. Both come off at cleanup unless
`Duration$ Permanent`. `pumpKeywords` returns the same split as a `pumpGrant` (plain keywords, hidden lines, the Layer
7d switch); mixed lines (`Trample & HIDDEN CARDNAME can't block.`) work.

| Piece                     | Does                                                                                           | Why                                                                                                     |
| ------------------------- | ---------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `pumpRecord.Hidden`       | The lines, prefix stripped                                                                     | The record is the state; `Card.hiddenKeywords` is rebuilt every pass (`clearHiddenKeywords`)            |
| `pumpHiddenKeywords`      | Puts every live record's lines back on its card, in `applyContinuousRules`                     | Same pass that rebuilds static `AddHiddenKeyword$` grants; a direct write would vanish on the next pass |
| `hiddenKeywordRead`       | One allow-list for statics and pumps; gains `CARDNAME can't attack.` (read by `attack.go`)     | A line nothing reads would be granted and ignored: refused, naming the line (GO-7)                      |
| `endNextUntapStepKeyword` | After the active player's untap, strips the untap line from records on permanents they control | `Untap.java:157-161`; a record left granting nothing is dropped                                         |

No `CARDNAME can't attack.` static line exists in the corpus, so widening the allow-list changes no static.

The untap line is removed whoever made the record and whether or not the permanent was tapped, as Java's loop over
`active.getCardsIn(Battlefield)` does: Frost Breath cast on the opponent's turn ends at that player's next untap step.
`endNextUntapStepKeyword` replaces a record's `Hidden` slice instead of editing it: `Game.Clone` copies `g.pumps` by
value, so a clone shares the slice (`TestPumpHiddenDoesNotUntapSkipsTheControllersNextUntapStep` clones before the untap
and checks the clone still waits).

A hidden line given to a player (`ValidTgts$ Player`) is refused before the effect changes anything (`pumpPlayers`):
Java passes the `HIDDEN ...` string itself to `Player.addChangedKeywords` (`PumpEffect.java:131-132`), which nothing
reads.

## ChangeZone `TargetsWithDefinedController$`

Java reads it twice. `SpellAbility.canTarget` (`:1405-1416`) keeps a card target only if its controller is one of the
defined players: `targetChoiceFor` (`targeting.go`) already did. `ChangeZoneEffect.java:876-878` also uses it for whose
library a `Shuffle$ True` move into a library shuffles — "it does need to work even without Targets" — with no fallback
when it names nobody. `changeZoneShufflers` ports that branch (each player once); the effect-side reject is gone.
Unblocked: Gaea's Blessing, Memory's Journey, Krosan Reclamation, Loaming Shaman, Rite of Renewal, Stream of
Consciousness, Witness the Future, Ink-Eyes, Scion of Darkness, Sigil of Sleep, Skullsnatcher, Zareth San, Zombie
Cannibal, Mortuary Mire, Deadwood Treefolk, Dukhara Scavenger, Dwell on the Past, Graven Abomination, Suffer the Past.

## "Up to" targets with no candidate

Gaea's Blessing over an empty graveyard could not be cast: `targetChoiceFor` answered "no legal target" for an empty
candidate pool whatever `TargetMin$` said. `TargetSelection.chooseTargets` cancels only when there are neither enough
candidates nor enough targets (`TargetSelection.java:82`, `:113-120`), and a minimum of 0 is always enough. Now:

| Site                    | `TargetMin$ 0`, no candidate                                                |
| ----------------------- | --------------------------------------------------------------------------- |
| `targetChoiceFor`       | ok, empty pool                                                              |
| `resolveTargets`        | no `ChooseTargets` question; the ability goes on with no target             |
| `resolveChainTargets`   | the link records an empty choice, no question                               |
| `copySpell` new targets | no `ConfirmEffect`/`ChooseTargets` question                                 |
| resolution              | no fizzle (no target to lose); `ValidTgts$` named, so no `Defined$` default |

`TestUpToTargetsWithNoCandidateStillResolves` pins the last row. Every smoke card that went plays→refused with this
change names `TargetMin$ 0`: the ability used to vanish silently, and now reaches its effect, which refuses a param it
does not resolve (`PutCounter: TargetMin$`, `ChangeZone: Duration$`, ...). Three refuse in `Attach`'s `Object$` with
`Defined$ ... no ancestor ability chose targets`: `definedCards`/`definedPlayers` fail closed on an empty
`parentTargets`, since they cannot tell "the ancestor chose zero" from "no ancestor targets"; Java's `ParentTarget` is
an empty list there.

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after this change:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 29,190 | 29,308 |
| refused      | 4,319  | 4,214  |
| inconclusive | 211    | 198    |
| judged play  | 87.1%  | 87.4%  |

| Flip                    | Cards | Cause                                                                                                                                                                            |
| ----------------------- | ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| refused → plays         | 149   | 128 `KW$`, 19 `TargetsWithDefinedController$`; Mindbreak Trap and Disorienting Choice play on the zero-target path only (`TargetMin$ 0`), their old refusals still stand         |
| refused, reason changed | 19    | 18 `KW$`: 16 lure pumps now refused at `turns` (below), Jet's Brainwashing (`GainControl: Condition$`), Searing Barb (`Pump: Defined$`); The Caves of Androzani (`TargetMin$ 0`) |
| plays → refused         | 41    | `TargetMin$ 0` abilities now reaching their effect                                                                                                                               |
| inconclusive → decided  | 13    | `TargetMin$ 0`: 10 play (zero-target path only), 3 refused                                                                                                                       |

The 16 lure pumps (Alluring Scent, Compelled Duel, Irresistible Prey, ...) now grant "must be blocked"; the probe's
block declarer does not satisfy block requirements, so the engine rejects its declaration (CR 509.1c), as it already did
for Lure, Gift of the Deity and 7 others.

## Not ported

| Shape                                                                | Lines | Reason                                                          |
| -------------------------------------------------------------------- | ----- | --------------------------------------------------------------- |
| `HIDDEN During your next untap step, ... return CARDNAME ...`        | 1     | Untap-step bounce (`Untap.java:93`); Undiscovered Paradise      |
| `HIDDEN Damage that would be dealt to CARDNAME can't be redirected.` | 1     | No damage redirection (`ReplaceDamage.java:100`); Whippoorwill  |
| `HIDDEN Unblockable`                                                 | 1     | Card-script defect: nothing reads it (`card-script-defects.md`) |
| a hidden line on a player                                            | 0     | Java's own result is an unread player keyword                   |
| `Defined$ ParentTarget` after an ancestor chose zero targets         | —     | Fails closed (`defined.go`); Java returns an empty list         |

## Tests

`pumphiddenkeywords_test.go` (can't block until cleanup, untap skip only at the controller's next untap step and the
clone, the rest of a record kept, PumpAll mixed line, can't attack, refused shapes),
`targetswithdefinedcontroller_test.go` (Gaea's Blessing with no card shuffles the target player's library, the targeting
restriction, an unknown spec, up-to targets with no candidate), `pumpeffect_test.go`
(`TestPumpEffectRejectsUnreadHiddenKeyword`). Scenarios `pump-hidden-mugging-target-cant-block`,
`pump-hidden-frost-breath-skips-the-controllers-next-untap`,
`pumpall-hidden-sleep-keeps-the-target-players-creatures-tapped`,
`changezone-mortuary-mire-puts-own-graveyard-creature-on-library`.
