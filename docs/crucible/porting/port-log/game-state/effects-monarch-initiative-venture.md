# Effects: BecomeMonarch, TakeInitiative, Venture

Designations and dungeons: Command-zone cards whose own triggers run the mechanic. Batch file for the three APIs
(DOC-12, `port-effect` step 5).

---

## BecomeMonarch lands

`BecomeMonarch` (64 corpus lines) resolves: each targeted or `Defined$` player (default the activator) still in the game
becomes the monarch (CR 724), unless a `Mode$ CantBecomeMonarch` static names them. `becomemonarcheffect.go`, ported
from `BecomeMonarchEffect.java`, `GameAction.becomeMonarch` (`GameAction.java:2536-2555`) and
`Player.createMonarchEffect`/`removeMonarchEffect` (`Player.java:3438-3481`).

**The Monarch is an effect card built in Go, not a parsed script.** Java builds it in `createMonarchEffect` from two
trigger strings; this port builds the same `compile.Ability` trees directly (`monarchEffectDef`), the way
`earthbendReturnTrigger` already builds Earthbend's delayed triggers. Nothing is parsed at runtime (PORT-2), and the
card needs no database entry, so every `NewGame(nil, ...)` test game can hold one.

| Trigger | Effect |
| --- | --- |
| `Mode$ Phase \| Phase$ End of Turn \| TriggerZones$ Command \| ValidPlayer$ You` | `DB$ Draw \| Defined$ You` |
| `Mode$ DamageDone \| ValidSource$ Creature \| ValidTarget$ You \| CombatDamage$ True \| TriggerZones$ Command` | `DB$ BecomeMonarch \| Defined$ TriggeredSourceController` |

The card is an `IsEffect` Command-zone card, so `traitHosts` and `checkPhaseTriggers` already run its triggers there
(the `Effect` port's machinery). Its lifetime is `effectPermanent`: the designation, not a `Duration$`, removes it.

**One card per player, reused.** `Player.monarchEffect` holds the player's card for the whole game, as Java's
`Player.monarchEffect` field does. Losing the monarchy parks it in `None` (`exileEffect`); becoming the monarch again
puts the same card back. Reason: making a new card per swap grows the `CardID` arena (never freed, ADR-0009) with every
monarch change.

**`becomeMonarch` keeps Java's order.** The previous monarch's card leaves first; then, if a `CantBecomeMonarch` static
stops the new player, nothing else happens and `Game.monarch` still names the previous monarch
(`GameAction.java:2542-2548`). Otherwise the card enters, `Game.monarch` is set, and `Mode$ BecomeMonarch` triggers run.

### New engine state

| State | Why |
| --- | --- |
| `Game.monarch`, `Game.monarchBeginTurn` | `Game.monarch`/`monarchBeginTurn`; the second is set as each turn begins (`PhaseHandler.java:848`) for `Mode$ BecomeMonarch`'s `BeginTurn$` |
| `Player.monarchEffect` | The one reused card, above |
| `Player.lossHandled` | `Game.onPlayerLost` runs once per loser; without the flag a monarch the successor cannot replace would be re-passed on every later SBA pass |
| `Ability.triggered` (`triggeredObjects`: source, sourceController, player) | Java's triggering-objects map, the keys ported modes set. Carried to every chained sub-ability, as `TriggerRemembered` is |

`Game.Clone` copies both `Game` fields; the `Player` and `Ability` fields are values copied with their structs.

**`Defined$ Triggered*` is new and fails closed.** `definedPlayers` reads `TriggeredSource`, `TriggeredSourceController`
and `TriggeredPlayer` off `Ability.triggered`. A trigger mode that does not record the key leaves it zero, and the
reference is an `error` (`the trigger recorded no source`), never an empty player list. Set today:

| Mode | Key recorded |
| --- | --- |
| `DamageDone` (card or player) | Source = the damage source; sourceController read at damage time. Java stores an LKI copy (`TriggerDamageDone.java`'s `getLKICopy`) |
| `BecomeMonarch` | Player = the new monarch (`TriggerBecomeMonarch.setTriggeringObjects`) |
| `DungeonCompleted` | Player = who completed it (`TriggerCompletedDungeon.setTriggeringObjects`) |
| `TakesInitiative` | Player = who took it (`TriggerTakesInitiative.setTriggeringObjects`) |
| `DamageDoneOnceByController` | Source = the player whose creatures dealt the damage (`TriggerDamageDoneOnceByController.setTriggeringObjects`) |

A card source named as players (`Defined$ TriggeredSource` on a `DamageDone` trigger) is rejected:
`AbilityUtils.addPlayer` over a card is a shape no ported line needs.

**`Mode$ BecomeMonarch`** (5 corpus lines) runs through `playerActionTriggerMatches`, `checkPlayerActionTriggers`' walk
split out with a per-mode gate: `ValidPlayer$` against the new monarch, `BeginTurn$` against `Game.monarchBeginTurn`
(nobody matches nothing, as `matchesValid` on `null`). `Static$ True` lines (1, Palace Jailer's "exiled until an
opponent becomes the monarch") are skipped: no mode here resolves a trigger off the stack.

**`Mode$ CantBecomeMonarch`** (1 line, Jared Carthalion, reached through `DB$ Effect | StaticAbilities$`):
`ValidPlayer$` through `matchesPlayerSpec`, absent naming every player. A static carrying any other param (a
`checkConditions` shape) or a `ValidPlayer$` `matchesPlayerSpec` cannot read is an `error` (`noPlayerStatic`).

**CR 724.4, the monarch leaving the game** (`Game.java:988-996`): `onPlayersLost`, called from `CheckStateBasedActions`
after CR 704.5a-c, passes the monarchy once per loser, in seat order -- to the active player, or, when the loser is the
active player, to the next player Java still holds in `ingamePlayers` (`nextInGameAfter`: everyone not yet processed as
a loser, so a loser of the same pass still waiting its turn counts, as in `Game.getNextPlayerAfter`). It runs before the
game-over check, as `checkGameOverCondition` does, so a two-player game's winner ends as the monarch.

**Engine fix: `playersInAPNAPOrder` with a lost active player.** It walked `nextPlayerAfter` round to the active player,
which `nextPlayerAfter` never returns once that player has lost -- an infinite loop the first time a trigger fired in
the SBA pass that eliminated the active player. It now starts at the next player still in the game, as
`MagicStack.java:835-838` does. `TestMonarchPassesWhenTheMonarchLoses` is the regression test.

**Rejected** (`error` before acting): `ConditionDefined$` (1 line) -- `subAbilityConditionMet` reads it as unmet, which
would skip the effect silently; `Defined$ TriggeredTarget` (1) -- `DamageDone`'s Target key is not recorded.

**Fixture key `monarch=`** (Crucible-only, `lost=`/`won=`/`over=`'s pattern, `game-state-fixture.md`). Java's
`GameState` dumps the monarch's effect card by name into the command zone and cannot load it back; `monarch=<player>`
writes the designation, `Load` recreates the card through `Game.SetMonarch`, and `Dump` leaves designation cards out of
the command zone. `compareGames` compares the monarch by name. Scenario:
`testdata/scenarios/monarch-draws-at-end-step-and-passes-by-combat-damage`.

**Readers.** `Player.isMonarch`/`hasInitiative` (`matchesPlayerProperty`, `PlayerProperty.java:85-92`),
`CheckDefinedPlayer$ You.isMonarch` (R9n, [`m6-r9n-checksvar-heads.md`](m6-r9n-checksvar-heads.md)) and Layer 2's
`GainControl$ Player.isMonarch` (Fealty to the Realm, R9o, [`m6-r9o-r9o-findings.md`](m6-r9o-r9o-findings.md)) read
`Game.monarch`/`Game.initiative`.

**Forge `getMonarchSet` (PORT-8, `forge-java-defects.md`).** No Crucible counterpart: this port carries no set codes,
and `becomeMonarch` takes none.

---

## Venture lands

`Venture` (46 corpus lines) resolves: each targeted or `Defined$` player (default the activator) still in the game
ventures into the dungeon (CR 701.49) -- into `Dungeon$`'s dungeon type when named (the initiative's Undercity).
`ventureeffect.go`, ported from `VentureEffect.java` (`getDungeonCard`, `chooseNextRoom`, `ventureIntoDungeon`),
`GameAction.completeDungeon` (`GameAction.java:2783-2790`) and `stateBasedAction_Dungeon` (`:1736-1743`).

**Rooms compile from `K:Dungeon:` (PORT-2).** A dungeon's room SVars are named only by its keyword, so nothing compiled
them. `compile.dungeonRooms` expands the keyword the way `CardFactoryUtil.java:1955-1989` does at card creation: one
`Mode$ RoomEntered | TriggerZones$ Command | ValidCard$ Card.Self | ValidRoom$ <RoomName>` trigger per room, in keyword
order, the room's ability as its `Execute$`, plus `NextRoomName$` (the `RoomName$` values of its `NextRoom$` SVars) on
each room ability. Only the four `res/tokenscripts` dungeons carry the keyword; no `cardsfolder` card does, so the
golden AST is unchanged. A room without `RoomName$`, or leading to an SVar its keyword does not list, is
`compile.ErrBadRoom` -- Java dereferences `null` on both.

| Step | Port |
| --- | --- |
| Which dungeon | The player's dungeon in the Command zone, unless its marker is on its last room -- that one is completed first. Otherwise a new one from the token table (`DB.TokenScripts`, new, sorted), `Dungeon$`'s subtype or every enterable dungeon, offered sorted by name through `ChooseOption` (Java's `chooseSingleCardFace` over a `TreeMap`) -- asked even for one option, as Java asks |
| Enterable | `CardRules.isEnterableDungeon` reads oracle text for "You can't enter this dungeon unless"; `compile.Face` has none, so the matching `K:` line (Undercity's only) stands in |
| Which room | Empty marker: the first room. Otherwise the one `NextRoomName$` room, or the player's pick of several through `ChooseAbilitiesForEffect` over room names (Java's `chooseSingleSpellForEffect`) |
| Enter | `Card.CurrentRoom` set; `Mode$ RoomEntered` triggers run (`playerActionTriggerMatches`, gated on `ValidCard$` and an exact `ValidRoom$` match); `Player.VenturedThisTurn` counts up |
| Complete (CR 309.7) | SBA `completeFinishedDungeons`, next to `removeTokensOffBattlefield` (Java's same 704.5d loop): marker on the last room and nothing from the dungeon on the stack. The dungeon joins `Player.completedDungeons`, ceases to exist (`None`, as `exileEffect` parks an effect card), `Mode$ DungeonCompleted` triggers run |

**New engine state:** `Card.CurrentRoom` (`Card.currentRoom`), `Player.VenturedThisTurn` (reset at cleanup, as
`Player.onCleanupPhase` does), `Player.completedDungeons` (deep-copied by `Game.Clone`; nil until the first completion,
so `TestCloneAllocationsStayBounded` holds). `Game.CompletedDungeons` exposes the list.

**Statics and properties.** `Mode$ CantVenture` (Keen-Eared Sentry) shares `noPlayerStatic` with `CantBecomeMonarch`;
its `ValidPlayer$ Opponent.VenturedThisTurn` needed the `VenturedThisTurn` player property (`matchesPlayerProperty`,
`PlayerProperty.java:482`). `Mode$ DungeonCompleted` (4 corpus lines) records `TriggeredPlayer`.

**Rejected:** `ConditionDefined$` (0 corpus lines; rejected for the reason `BecomeMonarch` rejects it). Unported and not
reached by the dungeons: `Count$DungeonsCompleted` and the other dungeon counts, and the Panharmonicon-style
`ValidMode$ RoomEntered` statics.

**Rooms that resolve.** A room's ability is an ordinary ability of another API, so a dungeon is only as playable as its
rooms. Each of the 30 rooms, resolved on its own with a token table holding the scripts it names:

| Dungeon | Rooms | Resolve | Error (the room's own API rejects it) |
| --- | ---: | --- | --- |
| Lost Mine of Phandelver | 7 | Cave Entrance, Goblin Lair, Mine Tunnels, Dark Pool, Temple of Dumathoin | Storeroom, Fungi Cavern (`PutCounter`/`Pump` reject `ValidTgts$`) |
| Undercity | 9 | Secret Entrance, Lost Well, Trap!, Arena, Stash, Catacombs | Forge (`PutCounter` `ValidTgts$`), Archive (bare `DB$ Draw`: `drawEffect` has no `Defined$` default), Throne of the Dead Three (`Dig` `WithCounters$`) |
| Tomb of Annihilation | 5 | Trapped Entry, Cradle of the Death God | Veils of Fear, Sandfall Cell (`UnlessCost$`), Oubliette (`Sacrifice` `SacEachValid$`) |
| Dungeon of the Mad Mage | 9 | Yawning Portal, Dungeon Level, Goblin Bazaar, Lost Level, Deep Mines, Muiral's Graveyard | Twisted Caverns (`Pump` `ValidTgts$`), Runestone Caverns (`Play` unported), Mad Wizard's Lair (`Draw` `RememberDrawn$`) |

A room that errors fails its trigger's resolution loudly, never silently; each gap is its own API's to close. The bare
`DB$ Draw` default (Java's `getTargetPlayers` falls back to the activator) is the widest: it is not specific to rooms.

**Real runs have no token table.** Only tests attach one (`DB.WithTokens`); `LoadDB`, the fixture loader and the
scenario harness do not. Until something loads `res/tokenscripts` for a real game, every venture errors
(`no dungeon "" in the token database`), the same gap `Token` has.

**No scenario fixture.** A dungeon in the Command zone cannot be written down: `GameState.java` has no current-room key,
and this port's `Load` does not load `T:` token entries. Module tests (`dungeon_test.go`) walk the real Lost Mine of
Phandelver to completion instead.

---

## TakeInitiative lands

`TakeInitiative` (23 corpus lines) resolves: each targeted or `Defined$` player (default the activator) still in the
game takes the initiative (CR 725) -- even one who already has it, which ventures again. `takeinitiativeeffect.go`,
ported from `TakeInitiativeEffect.java`, `GameAction.takeInitiative` (`GameAction.java:2557-2579`) and
`Player.createInitiativeEffect`/`removeInitiativeEffect` (`Player.java:3490-3548`).

"The Initiative" is built the way "The Monarch" is (`initiativeEffectDef`, one reused card per player in
`Player.initiativeEffect`), carrying Java's three triggers in Java's order:

| Trigger | Effect |
| --- | --- |
| `Mode$ DamageDoneOnceByController \| ValidSource$ Player \| ValidTarget$ You \| CombatDamage$ True \| TriggerZones$ Command` | `DB$ TakeInitiative \| Defined$ TriggeredSource` |
| `Mode$ Phase \| Phase$ Upkeep \| TriggerZones$ Command \| ValidPlayer$ You \| Secondary$ True` | `DB$ Venture \| Dungeon$ Undercity` |
| `Mode$ TakesInitiative \| ValidPlayer$ You \| TriggerZones$ Command` | `DB$ Venture \| Dungeon$ Undercity` |

Java re-registers the new card's triggers (`registerActiveTrigger`) so its own TakesInitiative trigger sees the take
that created it; this port's trigger checks walk the zones live, so the card in the Command zone is enough.

**Two trigger modes, both 0 corpus lines outside this card.** `Mode$ TakesInitiative` runs through
`playerActionTriggerMatches` (`ValidPlayer$` against the taker) and records `TriggeredPlayer`.
`Mode$ DamageDoneOnceByController` (`checkDamageDoneOnceByControllerTriggers`) joins `checkDamageTableTriggers`, after
`DamageDoneOnce`: per damaged target in first-seen order, once per distinct controller of the sources that damaged it,
in first-seen order (`CardDamageTable.java:77-104`), the controller read at damage time. Two attacking creatures of one
player make one take, not two.

**New engine state:** `Game.initiative` (`Game.hasInitiative`, copied by `Game.Clone`) and `Player.initiativeEffect`. CR
725.4 shares `onPlayersLost` and its successor rule with the monarchy. Fixture key `initiative=`, `monarch=`'s pattern
(`game-state-fixture.md`).

**Forge defect reproduced, not corrected (PORT-8, `forge-java-defects.md`).** `GameAction.java:2568-2573`: a player who
has lost passes the initiative to the next player, and then -- no `return` after the recursive call -- takes it anyway.
Reached from `onPlayersLost` when the holder and the active player lose in one SBA pass. This port runs the same code,
so the lost active player ends holding the initiative, as in Java (`TestInitiativeToALostActivePlayerReproducesJava`);
correcting it in Go alone would make Crucible disagree with the oracle.

**Rejected:** `ConditionDefined$` (0 corpus lines), for `BecomeMonarch`'s reason.

**No scenario fixture:** taking the initiative puts Undercity in the Command zone, which a fixture cannot write down
(`## Venture lands`).
