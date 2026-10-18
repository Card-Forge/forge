# Port Log — Game State: cast-time timing restrictions on spells (R25d)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `playerproperty.go` (`BeenAttackedThisCombat`),
  `castrecord.go` (`recordAttacked`), `turn.go` (`beginStep`, `advanceStep`, `endCombat`, `cleanupStep`),
  `amountheads.go` (`FinishedUpkeepsThisTurn`), `valid.go` (`attackingYou`, `attackedYouThisTurn`,
  `DefendingPlayer[Ctrl]`), `player.go`, `card.go`, `game.go`;
  [`internal/smoke`](../../../../../crucible/internal/smoke) (`timing.go`, `probe.go`)

Java: `SpellAbilityRestriction.checkTimingRestrictions` (`SpellAbilityRestriction.java:282-318`),
`checkOtherRestrictions` `CheckSVar$` (`:508-515`), `setRestrictions` (`:111-175`), `PhaseType.parseRange`
(`PhaseType.java:110-127`), `PlayerProperty.java:477-483`, `Player.java:1835-1862`, `Combat.java:170`,
`CombatUtil.java:385-387`, `AbilityUtils.java:2213-2216`, `PhaseHandler.java:257,415,976-978`,
`CardProperty.java:231-238,1193-1196,1506-1514`, `CardDamageHistory.java:59-72,93-95,287-288`.

## The timing check was already right

The open-plan row listed 42 cards (Assassin's Blade, Blaze of Glory, Camouflage, Champion's Victory, ...) that the probe
could not cast. `timingRestrictionsMet` (`activateability.go`) matches `checkTimingRestrictions` line for line:
`PlayerTurn$`, `OpponentTurn$`, `ActivationPhases$` (`parsePhaseRange`, Java's `parseRange`), `ActivationFirstCombat$`,
`ActivationAfterBlockers$`. Nothing in it changed. The fixed board was the cause: it casts in the probe player's own
first main phase, which no "cast only during combat / an opponent's turn" spell accepts. Behind the board, two
`CheckSVar$` operands and three target properties never resolved, so the same spells stayed uncastable at the right
moment too. In the current baseline 38 inconclusive cards name `ActivationPhases$`, `PlayerTurn$`, `OpponentTurn$` or
`CheckSVar$` on their `SP$` line: 37 timing cards, and Suffocation, whose `CheckSVar$` reads a red-spell damage record
(another row). Hurkyl's Final Meditation (a `RaiseCost` static) and Titan's Presence (a `Reveal` cost part) name none
and belong to R25h and R25g.

## Engine pieces

| Piece                                    | Go                                                                                                                                                                            | Java                                                                                                                                           |
| ---------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `BeenAttackedThisCombat` player property | `playerProperty`: some player's `attackedPlayersThisCombat` names the candidate                                                                                               | `PlayerProperty.java:477-483`, any registered player's `getAttackedPlayersMyCombat`                                                            |
| The per-combat record                    | `Player.attackedPlayersThisCombat`, appended once per player by `recordAttacked`, cleared for the active player in `endCombat`                                                | `Player.addCreaturesAttackedThisTurn` (`Player.java:1839-1841`), only from `CombatUtil.checkDeclaredAttacker`; `Combat.endCombat` (`:170`)     |
| `Count$FinishedUpkeepsThisTurn`          | `Game.upkeepsThisTurn`, counted in `beginStep`'s Upkeep case (a skipped upkeep never begins), reset at the turn boundary; the amount subtracts the upkeep in progress         | `nUpkeepsThisTurn++` (`PhaseHandler.java:257`, non-skipped branch), reset `:415`; `AbilityUtils.java:2213-2216`                                |
| `attackingYou` card property             | attacking, and the defender is the source's controller itself                                                                                                                 | `CardProperty.java:1509,1514`, `Combat.isAttacking(card, player)`                                                                              |
| `attackedYouThisTurn` card property      | `Card.attackedThisTurn` (the declared defender, once per declaration, from `recordAttacked`) holds the source's controller; cleared at cleanup and on leaving the battlefield | `CardProperty.java:1193-1196`, `CardDamageHistory.hasAttackedThisTurn` (`:59-72`, `:93-95`), `newTurn` from `Card.onCleanupPhase` (`:287-288`) |
| `DefendingPlayer[Ctrl]` card property    | the card's owner (controller for `...Ctrl`) is attacked: some attacker's defender is that player, or a planeswalker or battle that player controls or protects                | `CardProperty.java:231-238`, `Combat.isPlayerAttacked` (`Combat.java:956-970`)                                                                 |

`BeenAttackedThisCombat` records the declaration, not the live combat: an attacker removed from combat (Champion's
Victory, Remove) leaves it true, and a creature put onto the battlefield attacking never sets it, as only
`checkDeclaredAttacker` appends. Reading `g.combat` instead would answer differently in both cases. A planeswalker or
battle attacked is not its controller attacked (`addCreaturesAttackedThisTurn` keeps `Player` defenders only).

New state: `Player.attackedPlayersThisCombat`, `Card.attackedThisTurn`, `Game.upkeepsThisTurn`. `Game.Clone` copies all
three (nil slices stay nil, so `TestCloneAllocationsStayBounded` is unaffected). No controller method was added.

`RestartGame` resets neither new counter on purpose: `PhaseHandler.restart` (`PhaseHandler.java:826-830`) resets only
the extra phases, extra turns and turn number, and the engine's own reset of `upkeepsThisTurn` sits at the turn boundary
that `StartTurn` does not pass through, so a restarted first turn counts on from the old turn as Java does. No corpus
spell reads the count in a restarted game's first upkeep.

## Probe: timed variants

`internal/smoke/timing.go` adds, after every existing variant, a timed variant per board (plain, rich, tailored), turn
and step for a card whose first `SP$` line names `ActivationPhases$`, `PlayerTurn$`, `OpponentTurn$` or `CheckSVar$`.
`PlayerTurn$` limits it to the probe player's turn (turn 1), `OpponentTurn$` to the opponent's (turn 2); otherwise the
opponent's turn is tried first. The game is set to the untap step of that turn and played with `Game.Step`, the turn
driver, to each step that grants priority (upkeep through end step, first-strike damage left out): the upkeep is
counted, the active player attacks with everything, nobody blocks, combat damage is dealt. The card is then put in hand
and cast. The probe never reads `ActivationPhases$`: which step the spell accepts is `timingRestrictionsMet`'s answer.
Appended last, the variants change no card an earlier variant already cast.

## Whole corpus, before and after

`crucible card-smoke -all -workers 8 -list`, same corpus:

| Outcome      | Before | After |
| ------------ | -----: | ----: |
| plays        |  30608 | 30637 |
| refused      |   2929 |  2938 |
| inconclusive |    168 |   130 |
| judged       |  33537 | 33575 |
| % plays      |   91.3 |  91.2 |

38 cards changed, the 37 timing cards and Yare, all from `inconclusive`. No card went from `plays` to `refused` or back.
The percentage falls because nine newly judged cards are refused.

| Now       | Cards                                                                                                                                                                                                                                                                                                                                                                                                                         |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `plays`   | Assassin's Blade, Camouflage, Champion's Victory, Command of Unsummoning, Curtain of Light, Dazzling Beauty, Deep Wood, Defiant Stand, Delirium, Disharmony, Eightfold Maze, Festival, Glyph of Reincarnation, Gorilla War Cry, Harsh Justice, Heavy Fog, Jabari's Influence, Just Fate, Melee, Necrologia, Panic, Rally the Troops, Remove, Reset, Scorching Winds, Siren's Call, Teleport, Treetop Defense, Warrior's Stand |
| `refused` | see below                                                                                                                                                                                                                                                                                                                                                                                                                     |

| Card                 | Refusal, at resolution                                                             |
| -------------------- | ---------------------------------------------------------------------------------- |
| Blaze of Glory       | `Pump: CanBlockAny$ not resolvable yet`                                            |
| False Orders         | `RemoveFromCombat: UnblockCreaturesBlockedOnlyBy$ not resolvable yet`              |
| Flash Foliage        | `Token: TokenBlocking$ not resolvable yet`                                         |
| Illusionist's Gambit | `RemoveFromCombat: engine: Defined$ "Valid Creature.attacking" not resolvable yet` |
| Take the Bait        | `Goad: engine: Defined$ "Valid Creature.attacking" not resolvable yet`             |
| Tears of Rage        | `PumpAll: AtEOT$ not resolvable yet`                                               |
| Wake the Dead        | `ChangeZone: AtEOT$ not resolvable yet`                                            |
| Winter's Chill       | `GenericChoice: choice Pay2 carries UnlessCost$, not resolvable yet`               |
| Yare                 | `Pump: CanBlockAmount$ not resolvable yet`                                         |

Yare has no timing restriction and was not on the row's list: its only target, `Creature.DefendingPlayerCtrl`, matched
nothing until that property was ported, and the existing combat variant now casts it.

## Gaps

- An unresolvable `CheckSVar$`, Java's phase constant names, the other `attacking...` suffixed forms and
  `DefenderCtrlForRemembered`: resolved or refused loudly in [`m6-r9l-combat-batches.md`](m6-r9l-combat-batches.md).
- Suffocation stays inconclusive: its `CheckSVar$ X` reads the last red instant or sorcery that dealt the caster damage
  this turn, a record the probe's board never has.

## Tests

Module tests in `attackedthiscombat_test.go`: the attacked caster casts a declare-attackers-only spell, the attacking
player and an unattacked one cannot, the step still applies; a planeswalker attacked is not its controller attacked; an
attacker removed from combat still counts; the record clears as combat ends and survives on a clone; the finished upkeep
count is 0 before and during the upkeep, 1 after, 0 next turn, and a clone keeps its own; Reset's restriction refuses in
the opponent's upkeep and allows the draw step; `attackingYou`, `DefendingPlayer[Ctrl]` (a planeswalker's controller
included) and `attackedYouThisTurn` (outlives the combat, not the turn). Probe: `probe_test.go` cases for Assassin's
Blade, Camouflage, Festival, Reset, Necrologia, Jabari's Influence (`plays`) and Suffocation (`inconclusive`). Scenarios
`cast-timing-champions-victory-bounces-the-attacker-of-its-caster`,
`cast-timing-champions-victory-stays-in-hand-for-the-attacking-player` and
`cast-timing-reset-waits-for-the-opponents-upkeep-to-end`.
