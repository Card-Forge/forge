# Player properties over the turn's records (R9a)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `playerproperty.go` (new), `valid.go`
  (`matchesPlayerPropertyIn`), `amountplayers.go` (`playerCountPlayers`, `playerXProperty`), `defined.go`, the record
  hooks in `castrecord.go`, `combatdamage.go`, `sacrificeeffect.go`, `manaability.go`, `activatemanaability.go`,
  `turn.go`, `restartgameeffect.go`; [`internal/carddb/compile`](../../../../../crucible/internal/carddb/compile)
  (`lifePropertyOperand`)
- **Java:** `PlayerProperty.playerHasProperty` (`forge/game/player/PlayerProperty.java:24-517`);
  `AbilityUtils.playerXProperty` `:3523-3544`, `:3665-3673`, `getDefinedPlayers` `:1173-1176`, `:1194-1200`;
  `Player.java` `addCreaturesAttackedThisTurn` `:1835-1842`, `onCleanupPhase` `:2481-2517`, `addSacrificedThisTurn`
  `:2272-2285`; `CardDamageHistory.registerDamage`/`getDamageDoneThisTurn` `:236-281`; `Game.getDamageDoneThisTurn`
  `:1334-1350`; `Combat.getAttackedOpponents` `:199-209`, `getAttackersAndDefenders` `:234-236`;
  `PhaseHandler.java:190-191` (`setNumPowerSurgeLands`); `AbilityManaPart.tapsForMana` `:214-221`;
  `GameAction.sacrifice` `:2134-2135`

## What refused

`card-smoke -all` refused 24 cards on an amount whose player property or player measure the engine did not know
(`m6-r9-amounts.md`, "Not resolved"), 3 more on an amount reading a player comparison, and 15 on a `Defined$` player
spec with one of the same properties:

| Shape                                                                                                              | Cards | Cause before                                               |
| ------------------------------------------------------------------------------------------------------------------ | ----- | ---------------------------------------------------------- |
| `PlayerCount...$HasProperty<p>`: `LostLifeThisTurn`, `wasDealt[Combat]DamageThisTurn[By]`, `Defending`, ...        | 18    | `matchesPlayerProperty` did not know the property          |
| `OpponentsAttackedThisCombat`/`ThisTurn`, `SacrificedThisTurn <valid>`, `NumPowerSurgeLands`                       | 6     | No per-player record                                       |
| `PlayerCountPropertywithAtLeast<d>More...$Amount`, `HasPropertywithMoreLandsThanYou` in a `BranchConditionSVar$`   | 3     | The same, Oreskos Explorer, Surveyor's Scope, Stoic Farmer |
| `Defined$ Player.<p>`, `You.<p>`, `Opponent.<p>` (`withMostType...`, `HasCardsIn...`, `LostLifeThisTurn GE3`, ...) | 15    | Unknown property, and only a `Player.` base was filtered   |

## What landed

| Property or measure                                                             | Java                                          | Engine                                                                                                                                        |
| ------------------------------------------------------------------------------- | --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `LostLifeThisTurn[ <op><n>]`                                                    | `:189-202`                                    | `Player.LifeLostThisTurn` against the operand, at least 1 by default                                                                          |
| `wasDealt[Combat\|NonCombat]DamageThisTurn`                                     | `:140-172`                                    | any matching damage to the candidate this turn (Java's `anyIsEnough` list: size 0 or 1)                                                       |
| `wasDealt...ThisTurnBy <valid> [<op><n>]`                                       | `:149-168`                                    | distinct sources matching `<valid>`, each as it was when it dealt the damage, matched with the candidate as controller (Java passes `player`) |
| `wasDealt...ThisTurnBySource[Times] [<op><n>]`                                  | `:165-166`                                    | the damage the source card dealt the candidate, or the number of times                                                                        |
| `Defending`                                                                     | `:185-188`                                    | some attacker's target is the candidate itself (`attackedDirectly`)                                                                           |
| `attackedWithCreaturesThisTurn`, `attackedYouTheirCurrentTurn`/`LastTurn`       | `:448-476`                                    | `Player.attackedThisTurn`, `attackedPlayersThisTurn`/`LastTurn`                                                                               |
| `HasCardsIn<Zone>_<valid>_<op><n>`                                              | `:311-318`                                    | the candidate's cards in the zone matching `<valid>` from the host's view, against the operand                                                |
| `withMore<T>sThan<You\|Active>`, `withAtLeast<d>More<T>sThan...`                | `:319-335`                                    | battlefield type counts (`HasStringType`) against host or active player                                                                       |
| `hasMore<Life\|CardsInHand>[Than<Active>]`, `hasFewer<T>sIn<Play\|Yard>Than...` | `:336-352`                                    | strictly more / strictly fewer; only `CreaturesInYard` reads the graveyard, as Java                                                           |
| `withMost<PermanentInPlay\|CardsInHand\|Type<T>[Only]>`, `withLowestLife`       | `:353-439`                                    | Java's tallies, ties included (`withMostLife` was already ported)                                                                             |
| `OpponentToActive`, `castSpellThisTurn`                                         | `:66-70`, `:444-447`                          | not the active player; `SpellsCastThisTurn > 0`                                                                                               |
| `TappedLandForManaThisTurn`, `CardsInHandAtBeginningOfTurn`                     | `:203-210`                                    | `tappedLandForManaThisTurn`, `cardsInHandStartedTurn > 0`                                                                                     |
| `life<op><SVar or expression>`                                                  | `:266-272`                                    | the operand resolved as an amount; a literal stays `matchesPlayerPropertyIn`'s own case                                                       |
| `OpponentsAttackedThisTurn`                                                     | playerXProperty `:3665-3668`                  | `len(attackedPlayersThisTurn)`                                                                                                                |
| `OpponentsAttackedThisCombat`                                                   | `:3670-3673`, `Combat.java:199-209`           | for the active player, the players some attacker attacks directly; 0 for anyone else                                                          |
| `SacrificedThisTurn[ <valid>]`                                                  | `:3523-3532`                                  | the player's sacrifices of the turn, as last known, `<valid>` matched with the player as controller                                           |
| `NumPowerSurgeLands`                                                            | `:3542-3544`, `PhaseHandler.java:190-191`     | untapped lands the player controlled as their turn began, before untap                                                                        |
| `Defined$ You.<p>`, `Opponent.<p>`                                              | getDefinedPlayers `:1173-1176`, `:1194-1200`  | `definedPlayers`' fallthrough (defined.go) takes a `You`/`Opponent` base besides `Player`, filtered by `matchesPlayerSpec`                    |
| `!<p>`                                                                          | `Player.hasProperty`, `Player.java:2203-2205` | the property negated (Mindstorm Crown's `You.!CardsInHandAtBeginningOfTurn`, Arboria's `!IsRemembered`); unknown stays unknown                |

## Operands

A property's comparison operand is `calculateAmount(source, operand)` in Java. `matchesPlayerPropertyIn` takes the
amounts table and SVar depth: a `PlayerCount` amount passes its own (`playerXCount`, `playerCountPlayers`), so
`HasCardsInHand_Card_GTY` reads `Y` there and the hop counts towards `maxAmountDepth`. A valid-spec caller passes none;
the operand then resolves against the source card's SVars (`compareOperand`, the card-side numeric comparisons' own
resolver). Great Unclean One writes the operand as an expression (`HasPropertylifeLTCount$YourLifeTotal`); compile
registers it in `Face.Amounts` by its folded text (`lifePropertyOperand`), once at load (PORT-2), the key
`addInlineChecks` entries use.

## New engine state

| State                                       | Why                                                                                                                                          |
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `Player.attackedPlayersThisTurn`/`LastTurn` | Java's `attackedThisTurn` Player keys and `attackedPlayersLastTurn`; filled in `recordAttacked`, rotated at the attacker's own cleanup       |
| `Player.numPowerSurgeLands`                 | Set as the turn begins (`AdvancePhase`'s turn rotation, `StartTurn`), never reset                                                            |
| `Player.cardsInHandStartedTurn`             | Hand size after the cleanup discard (`Player.onCleanupPhase`)                                                                                |
| `Player.tappedLandForManaThisTurn`          | Set by a land's tap mana ability (`TapLandForMana`, `activateManaAbility`), cleared at cleanup                                               |
| `Game.playerDamageThisTurn []playerDamage`  | Each damage instance to a player, after prevention and replacement, with a last-known copy of the source; cleared at cleanup                 |
| `Game.sacrificedThisTurn []sacrificeRecord` | Each sacrifice (`sacrificeCardsFor`, the one path every sacrifice takes) with its last-known battlefield copy; a player's cleared at cleanup |

The two `Game` slices hold `Card` values, so they sit on `Game` rather than `Player` (enginelint: `player` may not
reference `card`). `Game.Clone` copies all four slices (`append(nil, ...)`); the `Card` snapshots inside are shared the
way `enteredThisTurn`'s are. `Defending` and `OpponentsAttackedThisCombat` read the live `Combat`; no combat record was
added. No new `PlayerController` decision.

## Quirks reproduced (PORT-7)

| Java                          | Behavior                                                                                                                                                                                           |
| ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PlayerProperty.java:384-395` | `withMostCardsInHand` scans with a strict `>`: a tie for the largest hand goes to the earliest seat, not to nobody. Sokenzan Renegade guards it with `APlayerHasMostCardsInHand$`                  |
| `PlayerProperty.java:404-421` | `withMostType<T>` starts its maximum at 0: when nobody has one, every player is "with the most"                                                                                                    |
| `PlayerProperty.java:185-188` | `Defending` and `OpponentsAttackedThisCombat` count only players attacked directly, not a planeswalker's or battle's controller. Matches Pack Attack's and Iron Mastiff's "players being attacked" |

## Forge defects

| Where                       | Defect                                                                                                                                                                                                                                                                                           |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `GameAction.java:2398-2401` | `setNumCardsInHandStartedThisTurnWith` runs after `startFirstTurn` returns, i.e. after the game loop ended, so `CardsInHandAtBeginningOfTurn` is false for the whole first turn: Mindstorm Crown on turn 1 draws even from a full hand. Reproduced (zero until the first cleanup), reported here |

## Not resolved, by name

Each stays a loud error naming the value:

| Shape                                                                                                       | Reason                                                                                   |
| ----------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `wasDealtCombatDamageThisCombatBy`, `wasDealtDamageThisGameBy`                                              | No per-combat or per-game damage record (Glen Elendra, Diseased Vermin)                  |
| `wasDealt...LastTurn <op><n>`, `wasDealt...` without `ThisTurn`                                             | Java ignores or misreads the comparator; not reproduced blind                            |
| `withMost<other>`, `withLowest<other>`, `hasMore<other>`                                                    | Java answers true without comparing anything; no corpus line                             |
| `BeenAttackedThisCombat`, `Attacking`, `attackedBySource...`, `wasAttackedThisTurnBy`, `NotedDefender`, ... | No corpus amount reaches them past an earlier refusal; `valid-strings.md` lists the rest |
| `SacrificedPermanentTypesThisTurn`                                                                          | Distinct permanent types of the sacrifices; no corpus line reaches it                    |

Of the 24 amount cards, 21 play and Raiders' Allegiance is placed. Belbe, Corrupted Observer and Power Surge resolve
their properties but refuse on `TriggeredPlayer` from a `Mode$ Phase` trigger that records no player (open plan R9b):
Belbe's `Defined$ TriggeredPlayer`, Power Surge's `TriggeredPlayer$NumPowerSurgeLands` head.

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 30,313 | 30,347 |
| refused      | 3,203  | 3,168  |
| placed       | 367    | 369    |
| inconclusive | 195    | 194    |
| judged       | 33,516 | 33,515 |
| judged play  | 90.4%  | 90.5%  |

35 refused → plays, 1 inconclusive → plays (Fire and Brimstone), 3 refused → placed (Raiders' Allegiance, The Mighty
Will Fall, Your Inescapable Doom). 4 stay refused at a later stage: Belbe (`Mana: Defined$ TriggeredPlayer`, R9b),
Desolation (`RepeatEach: DefinedCards$`), Loxodon Peacekeeper (`GainControl: NewController$`), Arboria
(`Pump: RememberObjects$ TriggeredCardController`, its `!IsRemembered` trigger now fires). 3 that played or were placed
now refuse, each on a path a now-recognized property lets the engine reach:

| Card                 | Now refused on                                                                                 |
| -------------------- | ---------------------------------------------------------------------------------------------- |
| Mysterious Stranger  | `ChooseCard: AtRandom$` (`HasPropertyHasCardsInGraveyard_Instant,Sorcery_GE1` now counts)      |
| Naktamun Lorespinner | `AlterAttribute` attribute (`HasPropertyHasCardsInHand_Card_LE1` now counts)                   |
| Hold the Perimeter   | `Token: Defined$ TriggeredPlayer` from `Mode$ Phase` (R9b; `Player.Other+!IsRemembered` reads) |

## Tests

`playerproperties_test.go` (`package engine_test`): each property family through a `PlayerCount` amount, the noncombat
and combat damage forms, the attack records during combat and across cleanup, sacrifices, Power Surge lands, the player
comparisons, `Defined$ You.`/`Opponent.` specs, the refusals, and a clone keeping its own records. `compiling_test.go`:
the compiled `life` operand. Scenarios: `player-property-moonshae-pixie-draws-for-an-opponent-dealt-combat-damage`,
`player-property-wojek-investigator-investigates-for-a-bigger-hand`,
`player-property-pack-attack-pumps-by-the-players-being-attacked`.
