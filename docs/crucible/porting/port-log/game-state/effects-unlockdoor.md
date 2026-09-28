# Effects: UnlockDoor lands, with Room door state

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `unlockdooreffect.go` (effect), `room.go` (door
  state, views, unlock/lock, `Mode$ UnlockDoor`/`FullyUnlock` triggers, the unlock special action), `card.go` (fields),
  `game.go` (`Move`/`MoveToLibraryTop`/`NewCard` hooks), `castspell.go` (`CastRoomDoor`, cast-half unlock),
  `priority.go` (`Action.Door`, `ActionUnlockDoor`), `control.go` (`ChooseRoomDoor`), `valid.go` (`FullyUnlocked`),
  `cloneeffect.go` (copiable values), `discovereffect.go`;
  [`internal/fixture`](../../../../../crucible/internal/fixture) (`UnlockedRoom:`, verbs)

Batch file for `UnlockDoor`, CR 709.5 Rooms (Duskmourn). Supersedes `UnlockDoor`'s deferred row in `effects-batch-b.md`
(closed file, left as written). No ADR: the door state is a `Def` swap, the pattern `frontDef` (transform) and
`faceUpDef` (face down) already use. Three additive decisions, none cross-cutting or hard to reverse (ADRP-1): a plain
`PlayerController` method (`ChooseRoomDoor`) with its own caller; `Action.Door` (`priority.go`), a new field on the
existing `Action` struct read only when `Kind` is `ActionCast`/`ActionUnlockDoor` (every other `Action` kind's own
meaning is unchanged); and `ActionUnlockDoor` itself, a new `Action` kind appended after ADR-0026's own three
(`ActionPlayLand`/`ActionTapForMana`/`ActionManaAbility`), the identical "a special action where the player keeps
priority" shape as `ActionPlayLand`.

---

## UnlockDoor lands, with Room door state

Ported from `forge-game/src/main/java/forge/game/ability/effects/UnlockDoorEffect.java`'s `resolve`, with
`Card.unlockRoom`/`lockRoom`/`updateRooms` (`Card.java:8006-8064`) as `Game.unlockDoor`/`lockDoor`/`Card.refreshRoom`.

| Shape                                                                         | Real lines | Status                                                                                                        |
| ----------------------------------------------------------------------------- | ---------: | ------------------------------------------------------------------------------------------------------------- |
| `DB$ UnlockDoor \| Mode$ Unlock \| ValidTgts$ Room.YouCtrl` (Keybearer)       |          1 | Resolves                                                                                                      |
| `DB$ UnlockDoor \| Mode$ Unlock \| Choices$ Room.YouCtrl+!FullyUnlocked`      |          1 | Resolves (Ghostly Dancers)                                                                                    |
| `AB$ UnlockDoor \| Mode$ LockOrUnlock \| ValidTgts$ Room.YouCtrl`             |          2 | Resolves (Marina Vendrell, Keys to the House)                                                                 |
| Default `Mode$ ThisDoor`                                                      |          0 | Rejected in the effect; Java synthesizes it, see the special action                                           |
| `T:Mode$ UnlockDoor \| ValidPlayer$ You \| ValidCard$ Card.Self \| ThisDoor$` |         29 | Fires (`doorTriggerMatches`); 1 more line without `ThisDoor$` (Solitary Study, Alchemy) fires for either door |
| `T:Mode$ FullyUnlock \| ValidCard$ Card.Room \| ValidPlayer$ You`             |         17 | Fires; 16 `TriggerZones$ Battlefield`, 1 `Graveyard`                                                          |

**Why the default mode has no script line.** `Mode$ ThisDoor` unlocks `sa.getCardStateName()`, the half the ability
belongs to. Its only producers are `CardFactoryUtil.abilityUnlockRoom` (`CardFactoryUtil.java:124`, the per-door
`ST$ UnlockDoor | Cost$ <half cost> | Unlock$ True` special action) and the cast-half unlock (`GameAction.java:571`).
Both run here as Go primitives, not abilities; an `Ability` carries no card state name to read. A script line naming
`Mode$ ThisDoor` or no mode is an `error` (GO-7).

### Resolution

| Step    | Go                                                                                            | Java                                                                |
| ------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| Rooms   | `Choices$`: activator picks 1 of the matching battlefield permanents; else targets/`Defined$` | `chooseSingleEntityForEffect` (`:32-41`) / `getTargetCards` (`:43`) |
| Unlock  | locked doors offered; none: skip; one: taken; two: `ChooseRoomDoor`                           | `chooseSingleCardState(getLockedRooms)` (`:55-62`)                  |
| Lock/un | 0 locked: pick an unlocked, lock it; 1: pick either, it flips; 2: pick a locked, unlock       | `:64-100`                                                           |
| Actor   | `a.Controller` unlocks                                                                        | `sa.getActivatingPlayer()`                                          |

A target that is not a Room permanent is skipped. `Choices$` over no match does nothing (Java's `null` return).

### Room state: a `Def` view

Every trait walk reads `range c.Def.Faces` (87 sites) and every characteristic reads `Def.Faces[0]`, so today's split
card on the battlefield had both halves live. Gating each site on door state would touch all of them; swapping `Def` for
a view of the unlocked doors touches none.

| `Card` field | Holds                                                                                                 |
| ------------ | ----------------------------------------------------------------------------------------------------- |
| `roomDef`    | printed split card while the card is a Room permanent or a Room spell cast as a half; `nil` otherwise |
| `doors`      | `doorSet` bitmask, `Card.unlockedRooms`; left then right is Java's `EnumSet` order                    |
| `castDoor`   | half a Room spell was cast as, read once as it enters                                                 |

All three are values or a shared immutable pointer: `Game.Clone`'s slice copy of `Card` copies them, no allocation.

| Doors unlocked | View (`roomView`)                                                                             | Java state                              |
| -------------- | --------------------------------------------------------------------------------------------- | --------------------------------------- |
| none           | unnamed `Enchantment Room`, no mana cost, no traits                                           | `EmptyRoom` (`CardUtil.java:218-231`)   |
| one            | that half alone in `Faces[0]`                                                                 | `LeftSplit` / `RightSplit`              |
| both           | `Faces[0]` left half with combined name, cost, color, type; `Faces[FaceAlternate]` right half | `Original` (`CardFactory.java:329-339`) |

Halves keep separate faces when both are unlocked so each trait keeps its own face's SVars. `doorAtSlot` maps a view
face back to its door: that is how `ThisDoor$` checks the trigger sits on the unlocked door (`TriggerUnlockDoor.java`,
the trigger's `getCardStateName`). The view goes in the card's own slot: `faceUpDef` while face down, `uncopiedDef`
under a copy effect, else `Def` — so turning face up or a copy ending shows the current doors (`Card.java:894`).

| Path                            | Go                                                    | Java                                                 |
| ------------------------------- | ----------------------------------------------------- | ---------------------------------------------------- |
| Enters the battlefield, any way | `Move`/`NewCard` → `enterRoom`: both locked           | `changeZone` → `updateRooms` (`GameAction.java:148`) |
| Cast as a half                  | `castAsDoor` before paying; the spell is that half    | `setSplitStateToPlayAbility`, CR 709.3               |
| Cast half resolves              | `permanentEffect` → `unlockDoor(caster, castDoor)`    | `GameAction.java:571`                                |
| Leaves battlefield or stack     | `leaveRoom` after the LKI snapshot: printed card back | new object, empty `unlockedRooms`, `Original`        |

`unlockDoor` swaps the view before collecting triggers (`Card.java:8012` then `:8019`), so the new door's own trigger is
live, then adds `FullyUnlock` matches once both doors are unlocked, and pushes all of them in one APNAP pass. `lockDoor`
fires nothing. Java also fires `GameEventDoorChanged`; ADR-0013's schema has no such kind, so no event.

**Casting a half.** `Game.CastRoomDoor(pid, card, door, controller)`; `CastSpell` casts a Room's left half.
`Action.Door` carries the half for `ActionCast` (zero value `DoorLeft`, so every existing action is unchanged). Other
split cards keep today's behavior; `playCastGap` still rejects them in `Play`.

**The unlock special action.** `Game.UnlockDoor(pid, card, door, controller)` / `ActionUnlockDoor`: the Room's
controller, sorcery timing (`canActSorcerySpeed`), door locked, Room face up, not a copy, phased in
(`Card.java:7414-7421`), pays that half's mana cost, unlocks, no stack, priority kept. It is how a second door normally
unlocks.

**Copies.** A Room permanent's copiable values are its whole printed card (`CardFactory.java:535-542`); door state is
not copied, so a token copy enters with both doors locked. A permanent _becoming_ a copy of a Room gets its own door
state in Java (`CloneEffect.java:146`); `Clone` rejects that shape with an `error`.

### New `PlayerController` decision

`ChooseRoomDoor(g *Game, decider PlayerID, room CardID, doors []Door) Door` — Java's `chooseSingleCardState` over the
Room's `LeftSplit`/`RightSplit` states. Called only with two doors: `PlayerControllerHuman.chooseSingleCardState` takes
a single option without asking (`PlayerControllerHuman.java:2058-2060`), and the effect does the same. The effect checks
the answer is one of `doors` (GO-7). `ScriptedController.QueueRoomDoor`; fixture
`queue roomdoor <LeftSplit|RightSplit>`.

**Java order not reproduced.** `Card.getLockedRooms` builds a `Sets.newHashSet` (`Card.java:7991`): enum identity hash
codes, so the two locked doors reach `chooseSingleCardState` in an order that can differ between JVM runs. Crucible
offers left then right. No game outcome depends on it; a recorded human answer is by door, not position.

### Fixture

| Key / verb                                | Meaning                                                                                  |
| ----------------------------------------- | ---------------------------------------------------------------------------------------- |
| `\|UnlockedRoom:<LeftSplit\|RightSplit>`  | `Game.LoadUnlockedDoor`, no trigger (`GameState.java:1419`, triggers suppressed `:617`)  |
| dump                                      | written last, left then right (`GameState.java:445-450`), under `Card.PrintedDef().Name` |
| `queue action <p> cast <id> <door>`       | `ActionCast` with `Door`                                                                 |
| `queue action <p> unlockdoor <id> <door>` | `ActionUnlockDoor`                                                                       |
| `queue roomdoor <door>`                   | `ChooseRoomDoor`'s answer                                                                |

`UnlockedRoom:` off the battlefield is recorded in `Loaded.Unapplied`. `compareGames` compares unlocked doors and names
a card by `PrintedDef()`. Scenarios: `room-lock-or-unlock-fires-that-doors-trigger` (Marina Vendrell unlocks Bottomless
Pool's locked door; its trigger bounces a creature), `room-cast-right-door-then-unlock-left-as-a-sorcery`.

### Rejected with an error, or not modeled

| Gap                                                                                                         | Why                                                                                                                                                                                                                                                                                                                                            |
| ----------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Effect `Mode$ ThisDoor` / no `Mode$`                                                                        | 0 script lines; runs as `Game.UnlockDoor` and the cast-half unlock                                                                                                                                                                                                                                                                             |
| `Clone` making a permanent a copy of a Room                                                                 | the copy's own door state (`CloneEffect.java:146`) needs door state on copy effects                                                                                                                                                                                                                                                            |
| `Discover` casting a Room                                                                                   | no half choice in that path; `error` before casting                                                                                                                                                                                                                                                                                            |
| `CopySpellAbility` copying a Room spell on the stack                                                        | the same one-way loss of Room-ness as `Clone`/`Discover`: `orig.Source`'s `Def` while cast is `doorView`'s single-half projection, not the split card; `copySpell` rejects with an `error` rather than build a copy that can never be a Room again (found by rules review, `roomDef != nil` check)                                             |
| `Count$UnlockedDoors` (2 lines), `Count$DistinctUnlockedDoors` (1)                                          | amount heads not ported                                                                                                                                                                                                                                                                                                                        |
| A Room off the battlefield has only its left half's characteristics                                         | pre-existing split-card gap: Java's `Original` state combines both halves in every zone, so a Room's mana value in hand/library (Discover's check) reads low                                                                                                                                                                                   |
| `GameEventDoorChanged`                                                                                      | no event kind in ADR-0013's schema                                                                                                                                                                                                                                                                                                             |
| Unlock special action under `S:Mode$ ReduceCost \| ValidSpell$ Static.Unlock` (Inquisitive Glimmer, 1 line) | no cost-changing static is applied anywhere; `UnlockDoor` refuses (`unlockCostModified`) rather than charge the printed cost. Scoped by `Activator$` against the unlocking player (`matchesPlayerSpec`), fixed by rules review: an unscoped first version let one player's Glimmer refuse every player's unlock, not only its own controller's |
| Mana restricted to unlocking (`RestrictValid$ ...Static.Unlock`, 2 lines)                                   | already rejected by `Mana`/mana abilities (`RestrictValid$`)                                                                                                                                                                                                                                                                                   |
| Cast Room entering: its unlock trigger and ETB triggers ordered together                                    | Java holds both (`unlockRoom` at `GameAction.java:571`, `ChangesZone` after the move) and the player orders them; here the unlock trigger is pushed in its own pass before `checkETBTriggers`, so an "enchantment enters" trigger always resolves first. Batching needs a `trigger.go` change                                                  |

No Forge bugs found.
