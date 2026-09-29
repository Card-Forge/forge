# Port Log — Game State: M6 Effects: Play and CopySpellAbility land

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)
- **Research record:** [`effects-play-copyspellability.md`](effects-play-copyspellability.md) — corpus shapes, the
  deferral this file closes
- **Design:** [ADR-0018](../../../adr/0018-instant-sorcery-spell-object.md) — built on master's stack-item identity
  (`Ability.ID`, `PushAbility`) and `castInstantOrSorcery`, not a second version of either

## Stack and casting pieces both APIs share

Each is the smallest change to an existing piece, not a parallel mechanism. Reason for each in its row.

| Piece                                          | Where                                                             | Why                                                                                                                                                                                                       | Java                                                  |
| ---------------------------------------------- | ----------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| `castSpell(controller, pid, card, castOpts)`   | `castspell.go`; `CastSpell` = timing/hand gates + this            | One cast path from any zone for all three spell shapes; Play casts through it, `castWithoutPaying` (Discover) now calls it                                                                                | `PlaySpellAbility.playSpellAbility`                   |
| `castOpts.withoutManaCost`, `payCastCost`      | `castspell.go`                                                    | `WithoutManaCost$`: nothing paid, X is 0, `ChoosePayX` never asked                                                                                                                                        | `SpellAbility.copyWithNoManaCost`                     |
| `putSpellOnStack`                              | `castspell.go`                                                    | Caster becomes the spell's controller (CR 110.2). `Move` never set it, so an opponent's card cast by Play resolved under the opponent                                                                     | `MagicStack.add`'s `source.setController(activator)`  |
| `Move`/`MoveToLibraryTop` controller reset     | `game.go`                                                         | Spell leaving the stack for anywhere but the battlefield returns to its owner's control (CR 108.4a)                                                                                                       | new `Card` object per zone                            |
| `Ability.spell`                                | `ability.go`; set by every cast path and `copySpell`              | Card in the Stack zone does not identify its spell entry: a "when you cast this spell" trigger's `Source` is the same card, pushed above it                                                               | `SpellAbility.isSpell`                                |
| `spellItemOf`, `stackItem`                     | `stack.go`                                                        | Card → its spell's `StackItemID`; ID → the live stack item                                                                                                                                                | `MagicStack.getInstanceMatchingSpellAbilityID`        |
| `triggeredObjects.spellAbility`                | `ability.go`; set by `checkSpellCastTriggers` (`trigger.go`)      | `Defined$ TriggeredSpellAbility`. Read from the top of the stack before any trigger is pushed; no signature change. `resolveSubAbility` already carries `triggered` down the chain                        | `AbilityKey.SpellAbility`                             |
| `Card.IsCopiedSpell`, `ceaseCopiedSpell`       | `card.go`, `game.go` (called first by `Move`, `MoveToLibraryTop`) | Copy moving anywhere is removed silently: no timestamp, no `ZoneChanged`, no trigger. Parked in owner's `None` zone like a ceased token. Both move primitives call it: `MoveToLibraryTop` bypasses `Move` | `GameAction.changeZone` (`GameAction.java:100-105`)   |
| `copyBecomesToken`                             | `castspell.go`, first line of `permanentEffect`/`attachEffect`    | Copy of a permanent spell resolves as a token (CR 111.11), so its `Move` onto the battlefield is not a cease                                                                                              | `GameAction.java:96-98`                               |
| `targetChoiceFor`                              | `targeting.go`; `resolveTargets` = this + `ChooseTargets`         | One candidates-and-bounds scan shared by casting and a copy's new targets                                                                                                                                 | `TargetRestrictions.getAllCandidates`                 |
| `APICopySpellAbility` → `stackSpellCandidates` | `targeting.go`                                                    | `ValidTgts$` without `TargetType$` names spells (Mischievous Quanar's `Instant,Sorcery`); the battlefield scan found none                                                                                 | `CopySpellAbilityEffect.buildSpellAbility` (`:28-33`) |
| `playLandNow`, `hasLandDrop`                   | `land.go`; `PlayLand` = gates + these                             | Play's land option: any zone, caster's turn, a drop left (CR 305.3)                                                                                                                                       | `Player.playLandNoCheck`, `canPlayLand(…, true, …)`   |
| `Game.Clone` copies `nextStackItemID`          | `game.go`                                                         | Crucible bug: a clone restarted at 0 and reused IDs already on its stack. Regression `TestCloneContinuesStackItemIDs` (`clone_test.go`)                                                                   | —                                                     |
| `queue confirmeffect <bool>`                   | `internal/fixture/actions.go`                                     | Scenario verb for `ConfirmEffect`; Play's single option and CopySpellAbility's `Optional$`/`MayChooseTarget$` ask it                                                                                      | —                                                     |

No new `PlayerController` method. Reason: every decision both APIs ask maps onto an existing one (tables below).

Inherited, not changed: `castInstantOrSorcery` passes `isSpellSource` false to `checkBecomesTargetTriggers` (only an
Aura is a spell source there, since `becomesTargetSourceMatches` reads `.Aura` as always true under one). CastSpell
offers no alternative cost (flashback, evoke, kicker); a Play that pays casts the basic spell only, like `CastSpell`.

## Play lands

`playeffect.go`, `PlayEffect.java:84-493`, `AbilityUtils.getSpellsFromPlayEffect` (`AbilityUtils.java:2910-2979`). Casts
during resolution, timing ignored (CR 608.2g), through `castSpell`: an instant or sorcery Play casts sits above the
resolving Play and resolves next. Corpus: 330 lines.

| Param                                                                  | Resolved as                                                                                                                                   |
| ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `Valid$` + `ValidZone$` (default Hand)                                 | zone by zone, each across every player (`Game.getCardsIn(Iterable)`), activator as "You" (`filterListByType`)                                 |
| else `Defined$` (default Self) / `ValidTgts$`                          | `targetedOrDefinedCards`                                                                                                                      |
| `ValidSA$`                                                             | `validSAMatches`: heads `Spell`, `SpellAbility`, `LandAbility`/`Ability`/`Static` (the land play), `Instant`/`Sorcery`; `!`; properties below |
| `Controller$`                                                          | first `definedPlayers` answer; none is an error (Java's `.get(0)`)                                                                            |
| `Amount$` (`All` = every candidate)                                    | `optionalAmount`                                                                                                                              |
| `Optional$`                                                            | `ChooseCardsForEffect` lo 0; one candidate with `Amount$` 1 asks `ConfirmEffect` instead (`singleOption`)                                     |
| `WithoutManaCost$`                                                     | `castOpts.withoutManaCost`; without it the cost is paid and a "no cost" card skipped (`:387-389`)                                             |
| `CopyCard$`                                                            | token copy (`IsToken`) of the chosen card in its zone, cast instead; CR 704.5d removes it once off the stack                                  |
| `AllowRepeats$`                                                        | the pick stays a candidate                                                                                                                    |
| `RememberPlayed$`/`ImprintPlayed$`/`ForgetPlayed$`/`ForgetRemembered$` | host memory after a successful play; `ForgetPlayed$` forgets the chosen original (`tgtCard`, `:473`)                                          |
| `ShowCardToActivator$`                                                 | no-op: `revealTo` is display-only                                                                                                             |
| land chosen                                                            | `playLandNow` if the caster's turn and a drop is left, then `ChangesZoneAll`                                                                  |
| spell cast                                                             | `castSpell`, then `ChangesZoneAll` origin → Stack (`triggerList.triggerChangesZoneAll`, `:476-481`)                                           |

`ValidSA$` properties (`SpellAbilityProperty.java:245-320`): `YouCtrl` true (caster is "You"), `OppCtrl` false,
`cmc<op><X>` against the card's mana value with the operand through the host's SVars (`resolveNamedAmount`), anything
else the card's own property through `Matches`.

Decisions: `ChooseCardsForEffect` (pick), `ConfirmEffect` (single option), then the cast's own (`ChooseTargets`,
`ChooseEnchantTarget`, Charm modes, mana payment).

Rejected before acting (`playUnresolvedParams`):

| Param                                                                                   | Reason                                                                                                  |
| --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `ReplaceGraveyard$`/`ReplaceGraveyardValid$` (35 lines)                                 | effect-card replacement on the spell's own Stack → Graveyard move, which has no hook (ADR-0018 point 3) |
| `TgtZone$` (33)                                                                         | `resolveTargets` scans the battlefield only, so a pushed target came from the wrong zone                |
| `ConditionDefined$` (20), `Condition$` (1)                                              | `subAbilityConditionMet` reads either as never met, silently                                            |
| `CopyFromChosenName$`, `AnySupportedCard$`, `RandomCopied$`, `RandomNum$`, `ChoiceNum$` | card built from outside the game                                                                        |
| `CastFaceDown$`, `CastTransformed$`, `ReplaceIlluMask$`                                 | alternate states                                                                                        |
| `PlayCost$`, `PlayReduceCost$`, `PlayRaiseCost$`, `ManaConversion$`                     | alternative or modified costs                                                                           |
| `ControlledByPlayer$`, `WithTotalCMC$`, `ShowCards$`, `ZoneRegardless$`                 | each its own mechanic                                                                                   |

Also an error, never a silent empty pool or a guess (GO-7):

| Case                                                                                                                                                  | Where                                 |
| ----------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| `Valid$` property `Matches` has no case for: `ExiledWith…` (23), `TargetedPlayerCtrl` (6), `OwnedBy`/`ControlledBy` (8), `shares…`, non-literal `cmc` | `playSpecGap`                         |
| `ValidSA$` cmc operand not resolvable, or card property with no case                                                                                  | `validSAPropertyMatches`              |
| chosen split/adventure/omen/modal/prepare card (a choice of spells, `getAbilityToPlay`, no decision here)                                             | `playCastGap`                         |
| chosen instant/sorcery with no `A:SP$` line, two of them, or `Cost$` on it (an additional cost `castInstantOrSorcery` does not pay)                   | `playCastGap`                         |
| a card with nothing to play, or no mana cost to pay, under `AllowRepeats$`                                                                            | `playRepeatLoop`; Forge defect, below |

The `ValidSA$` pre-filter keeps a card `playCastGap` names (Java would offer it); only choosing it fails.

**One cast path.** Play casts only what `castSpell` casts: a permanent, an Aura, or an instant/sorcery with exactly one
`A:SP$` line and no `Cost$` on it, paying the printed mana cost or nothing. Not covered, each an `error` from the two
tables above: split/adventure/omen/modal/prepare cards, alternate states, alternative and modified costs, Stack →
Graveyard replacement, targets outside the battlefield. Reason: each needs a decision or cast step `castSpell` lacks
(`getAbilityToPlay`'s choice among spells, alternate-cost payment, a replacement hook on the resolved spell's move), not
more Play code.

Not ported, no corpus line affected: `equalsWithGameTimestamp` on targeted cards (a `CardID` is stable across zones),
`XMin$` on the cast spell under `WithoutManaCost$` (`:365`), `getAbilityToPlay`'s cancel (`:327-333`).

**Known gap, PORT-2.** `validSAMatches`/`validSAPropertyMatches` hand-parse `ValidSA$`'s grammar at resolution time
(`strings.Split`/`strings.Cut` on the raw spec) instead of compiling it into a typed spec at load and routing it through
`internal/valid` the way every other `Valid$` key does. `ValidSA$` names a spell-ability shape, not a game object, so
`valid.Match`'s existing `EntityID` contract does not fit it directly; this needs its own compiled representation, not
reuse of the existing one. Left as runtime string parsing until that lands.

`TestSubAbilityChainUnimplementedAPIErrors` chains into `Phases` as its unbuilt API, since `Play` resolves.

| Test                                                  | Proves                                                                                                                                                                                                                |
| ----------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| scenario `play-casts-a-milled-instant-without-paying` | Jace's Mindseeker mills Lightning Bolt, casts it from the opponent's graveyard free under its controller; Bolt resolves, returns to its owner's graveyard                                                             |
| `playcasting_test.go`                                 | stack object and cast trigger, decline, optional stop, `ValidSA$` cmc filter and offer order, zone-major scan, `CopyCard$`, land drop and turn, `Amount$ All`, control, targets and Aura, rejections, `AllowRepeats$` |

**Forge bug (PORT-8).** `PlayEffect.java:312`/`:389` `AllowRepeats$` re-offer loop, latent
([`forge-java-defects.md`](../../forge-java-defects.md)).

## CopySpellAbility lands

`copyspellabilityeffect.go`, `CopySpellAbilityEffect.java:65-210`, `CardFactory.java:80-167` (`copySpellHost`,
`copySpellAbilityAndPossiblyHost`). Each copy is a new stack object (own `StackItemID`) on a new card, pushed above the
resolving ability, so it resolves next. Not cast (CR 707.10): no `SpellCast` event, no `SpellsCastThisTurn`, no cast
triggers, no cost. Corpus: 255 lines.

| Param / shape                                                 | Resolved as                                                                                                                                               |
| ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Defined$ TriggeredSpellAbility` (164 lines)                  | stack item the `Mode$ SpellCast` trigger recorded (`triggeredObjects.spellAbility`)                                                                       |
| `ValidTgts$` + `TargetType$ Spell` (42), no `TargetType$` (8) | targets scanned on the stack (`stackSpellCandidates`); each targeted card → its spell item (`spellItemsOf`)                                               |
| `Defined$ Targeted` (8)                                       | same mapping over the ability's targets                                                                                                                   |
| `MayChooseTarget$` (207)                                      | per targeting part: `ConfirmEffect` ("new targets?"), then `ChooseTargets` over `targetChoiceFor`'s scan for the copier; Aura copy: `ChooseEnchantTarget` |
| `Amount$` (34, default 1)                                     | `optionalAmount`; that many copies per spell per copier                                                                                                   |
| `Controller$` (35, default `You`)                             | `definedPlayers`; each player copies under their own control, owns the copy card                                                                          |
| `Optional$` (11)                                              | `ConfirmEffect` per copier per spell, before its copies (Java `confirmAction`, `:97`)                                                                     |
| `RememberNewCard$` (6)                                        | each copy's card remembered on the host (`CardFactory.java:113-115`)                                                                                      |
| `CantCopy$` on the original                                   | spell dropped (`SpellAbility.cantBeCopied`, `:460-462`)                                                                                                   |
| `IgnoreFreeze$` (3), `Secondary$`                             | no-op: this port never freezes the stack; `Secondary$` is description-only                                                                                |
| Charm original                                                | chosen modes and their targets carried over, never re-chosen (CR 707.10)                                                                                  |
| permanent spell original                                      | copy resolves as a token (`copyBecomesToken`, CR 111.11); Aura copy attaches to its own (possibly new) target                                             |

Copy card: `NewCard` from the original's `Def`, owned by the copier, straight into `Stack`, `IsCopiedSpell` set. The
original's `Def` is its copiable values: a card on the stack carries no Layer 1 copy effect (`endCopiesOnLeave`).
Leaving the stack by any move it ceases to exist (`ceaseCopiedSpell`), unless it resolves as a permanent.

**Trigger order.** Every copy of one resolution goes on the stack before any `BecomesTarget` trigger a copy's targets
raise. Reason: `MagicStack.add` only hands those to the trigger handler; they reach the stack when a player next
receives priority (`addSimultaneousStackEntry`), so under `Amount$ 2` both copies sit below both triggers. Test:
`TestCopySpellCopiesGoOnTheStackBeforeTheirTargetTriggers`.

**Copy order.** One copier's copies go on the stack in spell order, then copy order, no ordering decision. Java hands
them to `orderAndPlaySimultaneousSa` (`PlayerControllerAi.java:1296`), where a human orders them. Affects only several
distinct spells for one copier: Display of Power (1 line, `TargetMax$ X`). Same deterministic-order simplification
`pushTriggeredAbilities` (`trigger.go`) documents for simultaneous triggers.

**X.** Java carries X onto the copy (`copySpellHost`, `setXManaCostPaidByColor`). This port records no X on any spell;
`xPaid` is not a `resolveAmount` shape, so a copy of an X spell fails where the original does, never resolves with 0.

Decisions: `ConfirmEffect` (`Optional$`, `MayChooseTarget$`), `ChooseTargets`, `ChooseEnchantTarget`. No new
`PlayerController` method, no new engine state beyond the shared pieces above.

Rejected before acting (`copySpellUnresolvedParams`, plus shape checks):

| Param / shape                                                                                                         | Reason                                                                                                                     |
| --------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `CopyForEachCanTarget$` (11), `ChooseOnlyOne$` (3)                                                                    | CR 707.10d: one copy per other legal target, a per-target copy loop not built                                              |
| `DefinedTarget$` (3)                                                                                                  | CR 707.10e: copy's target set by the effect, `changeToLegalTarget`                                                         |
| `NonLegendary$` (7), `SetPower$`/`SetToughness$` (2), `AddTypes$` (2), `SetColor$` (1)                                | Layer 1 changes to the copy's copiable values (`getCloneStates`), not built for copies                                     |
| `RememberCopies$` (3)                                                                                                 | remembers `SpellAbility` objects; `Memory` holds entities only                                                             |
| `TargetValidTargeting$` (3), `SingleChoice$`, `Epic$`, `UseOriginalHost$` (1)                                         | each its own mechanic                                                                                                      |
| `ConditionDefined$` (6), `Condition$`                                                                                 | `subAbilityConditionMet` reads either as never met, silently                                                               |
| `Defined$ Parent` (10)                                                                                                | needs the resolving root spell, which a chained sub-ability does not carry                                                 |
| `Defined$ ValidStack` (3), `Remembered` (3), `Imprinted` (2), `TriggeredSourceSA` (2), `Spawner>…` (1), `Self`, `You` | other `getDefinedSpellAbilities` shapes, none built                                                                        |
| `TargetType$` other than `Spell` (17: `Activated`, `Triggered`, `SpellAbility.numTargets`, `Spell.numTargets`)        | ability copies (CR 707.10b) and targeting-count restrictions                                                               |
| triggering or targeted spell no longer on the stack                                                                   | Java copies from the `SpellAbility` it still holds (last-known information); this port keeps no stack item after it leaves |
| `Mode$ SpellCopy`/`SpellCastOrCopy`/`SpellAbilityCopy` trigger on a trait host                                        | `MagicStack.java:442-450` fires them for a copy; none built. `SpellCastOrCopy` covers Magecraft                            |
| `ReplacementType.CopySpell` replacement or `Mode$ CantBeCopied` static on the battlefield                             | copy-count replacement (`:177-202`) and `StaticAbilityCantBeCopied` not built                                              |

A targeted card that is no longer a spell on the stack (resolved, left) is skipped: nothing to copy. A new target
outside the legal candidates is an error (`checkChoice`), never set on the copy.

| Test                                                    | Proves                                                                                                                                                                                                                                                                    |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| scenario `copy-spell-swarm-intelligence-retargets-bolt` | Swarm Intelligence's optional trigger copies Lightning Bolt; copy retargeted to Grizzly Bears kills it and ceases; original hits ai for 3                                                                                                                                 |
| `spellcopy_test.go`                                     | not cast, ceases on any move, new targets or kept, illegal new target, trigger order, `Amount$`/`Controller$`/`Optional$`/`RememberNewCard$`, token copy, Aura retarget, stack targeting with and without `TargetType$`, Charm modes, `CantCopy$`, rejections, spell gone |

Scenario verb added: `queue optionaltrigger <bool>` (`internal/fixture/actions.go`, `ConfirmOptionalTrigger`), for an
`OptionalDecider$` trigger such as Swarm Intelligence's.

No Forge defect found in `CopySpellAbilityEffect.java` or `CardFactory.java`'s copy path. `PlayerControllerAi.java:1314`
(`FIXME`: AI uses `chooseNewTargetsForCopy`, not `setupNewTargets`) is an AI-only difference, not a rules defect.
