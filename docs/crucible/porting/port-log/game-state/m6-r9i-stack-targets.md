# Port Log — Game State: spells targeted on the stack, tokens entering attacking, the R9f findings (R9i)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `targeting.go` (`targetChoiceFor`,
  `zoneListCandidates`, `stackSpellCards`, `stackAbilityCandidates`, `targetStillLegal`, `dropIllegalChainTargets`),
  `changezoneeffect.go` (`changeZoneKnown`), `stack.go` (`removeSpellFromStack`), `changecombatantseffect.go`
  (`attackAmong`, `addToCombatAttacking`), `tokeneffect.go`, `copypermanenteffect.go`, `countereffect.go`,
  `charmeffect.go` (`modeHasLegalTargets`), `defined.go` (`TriggeredCardOpponent`), `valid.go` (`OpponentOf`),
  `exploit.go`, `amountcontext.go` (`TriggeredExploited`), `combatdamage.go`, `zonemove.go` (`enterCounters.placer`),
  `makecardeffect.go`, `putcounteralleffect.go`, `multiplycountereffect.go`, `block.go`, `game.go` (`Move`,
  `MoveToLibraryTop`), `chaintargets.go`, `ability.go` (`triggeredObjects.exploited`, `chainTarget.Targeter`)
- **Java:** `AbilityFactory.java:370-381` (`adjustChangeZoneTarget`); `TargetRestrictions.java:568-593`
  (`getAllCandidates`); `TargetSelection.java:97-101`, `:288-305`; `SpellAbility.java:1386-1395`, `:1474-1476`,
  `:2141-2164`; `ChangeZoneEffect.java:495-506`, `:577-579`, `:614-620`; `MagicStack.java:735-781`, `:799-805`;
  `Zone.java:141-146`; `SpellAbilityEffect.java:758-793`; `TokenEffectBase.java:140-145`, `:188`;
  `MakeCardEffect.java:179-206`; `CountersPutAllEffect.java:92`, `:118`; `CountersMultiplyEffect.java:62-69`;
  `Card.java:6343`, `:6354`; `SacrificeEffect.java:171-173`; `TriggerExploited.java:57-72`;
  `AbilityUtils.java:1038-1050`; `PlayerProperty.java:38-49`; `PhaseHandler.java:744-820`; `GameAction.java:671-673`;
  `Card.java:3765-3770` (`clearControllers`)

## A ChangeZone targets a spell as its card

`adjustChangeZoneTarget` makes Origin$ the target zones of a ChangeZone that does not target players. A spell on the
stack is a card there (`Card.inZoneStack`), so Unsubstantiate's `Origin$ Battlefield,Stack` offers creatures and spells.

| Piece | Does | Java |
| --- | --- | --- |
| `zoneListCandidates` | Players, then each Origin$ zone's cards in Origin$ order; the stack's are the cards of its spell items | `getAllCandidates`, `Game.getCardsIn` |
| `stackSpellCards` | Stack alone: top first (`chooseCardFromStack`); in a zone list: oldest first (`Zone.add` appends) | `TargetSelection.java:97-101` |
| CR 115.5 | The spell being cast is never its own candidate: it is not on the stack yet while its targets are chosen; `a.spell` excludes it besides | `SpellAbility.java:1391-1395` |
| `changeZoneKnown` | A targeted card in the Stack zone leaves the stack (`removeSpellFromStack`), then moves; a copy ceases to exist and flashback exiles in `Move` | `ChangeZoneEffect.java:577-579` |
| `removeSpellFromStack` | Drops the spell items of the card only; a cast trigger whose source is the spell's card stays | `MagicStack.remove(Card)`, `:799-805` |
| Defined$ spell | Still refused: a Defined$ card on the stack other than the host is unaudited | — |
| ChangeZoneAll | An Origin$ Stack target is refused when targets are chosen: its Resolve does not take a spell off the stack | — |

`TargetType$ Spell` on a ChangeZone (Failure, Smirking Spelljacker) reaches the same move.

## `TokenAttacking$`

`addToCombatAttacking` is `addToCombat`'s attacking half, called per token by Token and CopyPermanent
(`TokenEffectBase.java:188`, which CopyPermanentEffect inherits): only a creature, only during combat, only one the
attacking player controls. `True` offers every defender (`eligibleAttackTargets`); any other value is the `" & "`-joined
`definedEntities`. The activator picks (`ChooseAttackTarget`, an existing decision). `attackAmong` also marks the
attacker's band unblocked (`setBlocked(false)`, `:791`, `Combat.blockDecided`), so ChangeCombatants and Meld now do too.

| Shape | Result |
| --- | --- |
| `ForEach$` with a Defined$ value | Error: Java reads it off the token, not ported |
| A Defined$ value that does not resolve | Error (Seraphic Greatsword, Najeela) |

## Target questions that were dropped silently

| Was | Now |
| --- | --- |
| An unresolvable `TargetMin$`/`TargetMax$` left the ability off the stack | `choice.err`: pushed, fails at resolution naming the param; a Charm mode with one is offered (`modeHasLegalTargets`) and fails |
| `TargetType$` other than `Spell` (non-ChangeTargets) left it off the stack | Asked over every stack item (`stackAbilityCandidates`): an ability that is a legal target is a candidate since R25e ([`m6-r25e-abilities-on-stack.md`](m6-r25e-abilities-on-stack.md)); spells are candidates; none is no legal target |
| Counter refused any `TargetType$` but `Spell` | A targeted Counter resolves under any TargetType$: every target is a spell by then. An untargeted one must still name `Spell` |
| At fizzle time a link's targeting player was re-derived, falling back to the controller | Recorded per link (`chainTarget.Targeter`, `SpellAbility.java:2158-2161`); an unrecorded one that no longer resolves is a pending error, target illegal |
| A per-card restriction error at fizzle time made the target illegal silently | Pending error |
| A link read no ParentTarget at fizzle time | `dropIllegalChainTargets` passes each link its parent's chosen targets, before removal (`MagicStack.java:773-779`) |

The last row is a real bug the pending error surfaced: Legerdemain and Gauntlets of Chaos lost their link's target at
every resolution (`TargetsWithSharedCardType$ ParentTarget` read no parent), so ExchangeControl did nothing while the
smoke probe counted them as playing.

## `TargetsWithDefinedController$` over the trigger

| Spec | Reads |
| --- | --- |
| `TriggeredCardOpponent` | Opponents of the stored card's controller, the LKI's when it left the battlefield (`cardController`) |
| `Player.OpponentOf <Defined>` | `definedPlayers` with the refs of the ability in context (`Game.resolving`); outside it, unrecognized |

`OpponentOf` with any Defined also resolves `Choices$ Player.OpponentOf Remembered` (Bend or Break) and
`OpponentOf ReplacedPlayer` (Shared Fate).

## Counter placements through the counter table

| Placement | Placer | Change |
| --- | --- | --- |
| Wither/infect -1/-1 counters | source's controller (`:6354`) | `addCardCounters`, after the DamageDone trigger check (`Card.java:6343`) |
| `WithCountersType$` / token / MakeCard on entry | activator / creator / player | `enterCounters.placer`; joined to `Card.pendingEnter`, so `applyEnterCounters` sums them with the ETB$ piles |
| MakeCard `WithCounter$` off the battlefield | player | `addCardCounters` |
| PutCounterAll, MultiplyCounter | per card / activator | `countersReplaced` first (Hardened Scales); MultiplyCounter reads every amount before placing |

## Smaller fixes

| Where | Fix |
| --- | --- |
| `exploit.go` | `Card` is the exploiter, `Exploited` the sacrificed creature (`SacrificeEffect.java:171-173`); amount head `TriggeredExploited` (Profaner of the Dead, Henry Wu) |
| `block.go` | Unblocked-attacker triggers run first, after every attacker's blocked status is decided (`PhaseHandler.java:746`) |
| `Move`, `MoveToLibraryTop` | A static's Layer 2 row (`ControlMod`) is cleared leaving the battlefield (`GameAction.java:671-673`). `applyContinuousControl` rebuilds battlefield rows only, so the row stayed for good, not until the next layer pass: the thief could Raise Dead the card from its owner's graveyard |

## Decisions

| Decision | Why |
| --- | --- |
| A spell stays a `CardEntity` target; no SpellAbility target kind | Counter, CopySpellAbility, ChangeTargets already read spells that way; `removeFromStack(tgtSA)` and the card path end the same |
| New state: `triggeredObjects.exploited`, `chainTarget.Targeter`, `enterCounters.placer` | Values on value structs; `Ability` and `Game.Clone` copy them with no allocation; `clonedChainTargets` copies the field |
| No new `PlayerController` method | The token's defender is `ChooseAttackTarget`; targets are `ChooseTargets` |
| enginelint: `valid` may reference `defined` | `OpponentOf <Defined>` is Java's `getDefinedPlayers` from `PlayerProperty` |

## Smoke probe

`crucible card-smoke -all -workers 8 -list`, before → after:

| Status | Before | After |
| --- | --- | --- |
| plays | 30,626 | 30,670 |
| refused | 2,887 | 2,864 |
| placed | 372 | 373 |
| inconclusive | 193 | 171 |
| judged play | 91.4% | 91.5% |

| Flip | Cards | Which |
| --- | --- | --- |
| refused → plays | 13 | Stack targets: A-Divide by Zero, Brutal Expulsion, Divide by Zero, Failure, Jeskai Revelation, Press the Enemy, Reprieve, Shell of the Last Kappa, Sink into Stupor, Unsubstantiate, Venser, Shaper Savant; `OpponentOf`: Bend or Break; CopyPermanent `TokenAttacking$`: Satya, Aetherflux Genius |
| refused → plays | 26 | `TokenAttacking$`: Anim Pakal, Brimaz (its `TokenBlocking$` still refused, the probe never blocks), Caesar, Captain's Claws, Combat Calligrapher, Ellie, Falconer Adept, General Kreat, Ghalta and Mavren, Gut, Hanweir Garrison, Hero of Bladehold, Indulge, Inquisitorial Rosette, Kessig Cagebreakers, Leonin Warleader, Maestros Diabolist, Otharri, Pugnacious Pugilist, Sigiled Sword of Valeron, Skyknight Vanguard, Soaring Lightbringer, Squee, Squirrel Squatters, Suki, Warren Warleader |
| refused → placed | 1 | Dark Wings Bring Your Downfall |
| inconclusive → plays | 6 | TargetType$ past Spell now asked: Deny the Witch, Disallow, Katara's Reversal, Spider-Sense, Voidslime (counter a spell), Wyll's Reversal |
| inconclusive → refused | 16 | Were left off the stack silently: unresolvable TargetMin$/TargetMax$ (Consumed by Greed, Dewdrop Cure, Graveyard Dig, Into the Flood Maw, Long River's Pull, Mind Spiral, Peerless Recycling, Sazacap's Brew, Valley Rally, Wear Down, Wildfire Howl); TargetType$ properties (Consign to Memory, Louisoix's Sacrifice, Tale's End); Chef's Kiss, Radiate now reach their own refused params |
| plays → refused | 1 | Slight Malfunction: `TargetMax$ Spawner>SVar$X` left its reflexive trigger off the stack silently |
| refused, reason changed | 23 | Aether Gust, Endless Detour, Subtlety, Sudden Setback, Swat Away (`DestinationAlternative$`); Commit (`LibraryPosition$ 1`); Bilbo's Gambit (`Effect: ConditionZone$`); Talion's Throneguard (`Animate: staticAbilities$`); Gale's Redirection; Fire Navy Trebuchet, Geist of Saint Traft, Gemini Engine, Invocation of Saint Traft, Kari Zev, War Effort (`AtEOT$`); Ghired, Stangg (CopyPermanent params); Najeela, Seraphic Greatsword (Defined$); Necrotic Plague (`AttachedTo$`); Power Struggle (`TargetsAtRandom$`); Return the Favor; Shared Fate |

The plays → refused card was a silent miss before. Legerdemain and Gauntlets of Chaos stay "plays"; their link now keeps
its target at fizzle time (not separately tested).

## Tests

`stackspelltargets_test.go` (a spell bounced to its owner's hand, battlefield-then-stack candidate order, a target that
left the stack fizzles, a Defined$ spell and ChangeZoneAll's Origin$ Stack refused), `tokenattacking_test.go` (`True`
and a Defined$ defender, outside combat, `ForEach$` and an unresolvable value refused), `targetquestions_test.go`
(unresolvable bounds, Disallow-style Counter of a spell, Stifle over an empty stack not castable, both
`TargetsWithDefinedController$` specs, `OpponentOf` out of context, an unrecorded targeting player at fizzle time),
`counterplacementpaths_test.go` (wither CounterAdded, entry counters with Hardened Scales, MakeCard off the battlefield,
PutCounterAll and MultiplyCounter with Hardened Scales), `exploittriggeringobjects_test.go` (Profaner of the Dead,
`TriggeredCard` is the exploiter), `unblockedtriggerorder_test.go`. Scenarios
`stack-target-unsubstantiate-returns-a-creature-spell-to-its-owners-hand`,
`token-attacking-leonin-warleader-cats-enter-attacking-and-deal-damage`,
`layer2-control-magic-creature-dead-in-owners-graveyard-is-not-the-thiefs`.
