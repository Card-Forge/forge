# Port Log — Game State: targeted-spell records, `Defined$ Parent` from the stack, activation paying mana, loud `ReduceCost` amounts, Repeat and dice gates (R9p)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `ability.go` (`stackTargetRef`,
  `spellCastRecord`, `rootSpell`, `paidLists.mana`), `targeting.go` (`appendStackTargetRefs`, `spellTargetRef`),
  `amountcontext.go` (`targetedCards`, `spellTargetView`, `spellTargetedValue`, `targetedByTargetCards`,
  `rootPayingManaCount`, `castSAValue` xPaid), `amountplayers.go`, `amountpaid.go` (`perCardMeasure`), `amountheads.go`
  (`TotalManaSpent`, `CardManaCost`), `amountrecords.go` (`searchedLibraryCount`), `card.go` (`stackX`, `isCommander`),
  `valid.go`, `defined.go` (`TargetedAndYou`, `targetingPlayers` moved here), `changezoneeffect.go`
  (`changeZoneKnownCards`, `librarySearched`), `subability.go`, `additional.go`, `activateability.go`, `costmod.go`,
  `repeateffect.go`, `rolldiceeffect.go`, `player.go`, `turn.go`, `restartgameeffect.go`, `dealdamageeffect.go`,
  `playeffect.go`, `castspell.go` (`castCost` `paying`), `charmeffect.go`, `castrecord.go`; `enginelint.json` (`ability`
  may name `mana`)
- **Java:** `AbilityUtils.java:146-151`, `:415-423`, `:572-640`, `:674-676`, `:970-975`, `:1218-1278`, `:1585-1587`,
  `:1856-1865`, `:1890-1891`, `:1941-1948`, `:2231-2243`, `:3742-3851`; `SpellAbility.java:1916-1928`, `:1961-1986`,
  `:1691-1704`; `Card.java:1630-1636`, `:6930-6933`, `:7340-7353`; `Zone.java:162-167`; `CardProperty.java:1983-1986`;
  `ChangeZoneEffect.java:471-579`, `:1000-1092`, `:1600-1633`; `MagicStack.java:600-687`; `RepeatEffect.java:23-52`;
  `RollDiceEffect.java:459-533`; `AbilityFactory.java:50-67`, `:265-271`; `CostAdjustment.java:459-480`;
  `GameAction.java:170-251`; `Player.java:2156-2164`, `:2467-2509`; `TriggerHandler.java:494`

## Targeted spells read as they were on the stack

Java targets a spell as its `SpellAbility`. Its host stays the `Card` object that was on the stack after a Counter or a
ChangeZone moved it: `GameAction.changeZone` builds a new object for the destination (`:207`), and `Zone.remove` never
resets the old one's zone (`Zone.java:162-167`), so `isInZone(Stack)` stays true (`Card.java:6930-6933`), `getCMC` still
adds the X paid (`:7350-7353`) and `getCastSA` still answers (`:1630-1636`). Here a spell is targeted as its card, and
`Game.Move` clears the card's cast record (`castManaSpent`, `castX`, sources) once it leaves the stack.

`recordStackTargets` (run at the CR 608.2b check, before resolution) now records each card target that is a spell on the
stack beside the ability targets it already kept: one ordered `stackTargetRef` list, root targets first then the links',
which is `getAllTargetChoices` order (`SpellAbility.java:1916-1928`). A spell entry carries the card's zone stamp, its
controller, a `spellCastRecord` (cast, X, mana spent, snow, paying sources) and the item's own targets; an ability entry
now carries its targets too. Sub-abilities already inherit the list. Nothing allocates when no stack item is targeted.

| Head                          | Reads                                                                                                                                                                                                                                               |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Targeted$<measure>`          | `findTargetedCards` (`SpellAbility.java:1961-1986`): card targets when there is one, else each targeted spell's view (the live card while it is that spell, else a copy with the recorded zone, controller and cast record) and each ability's host |
| `SpellTargeted$<measure>`     | the first targeted spell or ability (`AbilityUtils.java:637-640`, `:1264-1278`), measured on its host as `xCount(sat.getHostCard(), …, sat)`; 0 with none; a measure `liveCardMeasure` does not know is unresolved                                  |
| `TargetedByTarget$<body>`     | the card targets of every targeted spell or ability (`:615-623`, `:146-151`), live; a card still on the stack is a spell, which `getTargetCards` never lists                                                                                        |
| per-card `CardManaCost`       | `getCMC`: the X paid added while the card is on the stack (`Card.stackX`); also `Count$CardManaCost` on the source                                                                                                                                  |
| per-card `CardManaCostLKI`    | `:2231-2243`: X added off the stack too. Java takes that X from the measuring `SpellAbility` when it has one; every corpus line measures a spell through its own (`SpellTargeted$`, `TriggeredSpellAbility$`), whose X is the card's                |
| per-card `CastTotalManaSpent` | `:1896-1898` on each card: its cast's mana spent, 0 for a card not cast                                                                                                                                                                             |
| `IsCommander`                 | `CardProperty.java:1983-1986` reads `Card.isCommander`, which only `Player.addCommander` sets; no Commander setup is ported, so it is false for every card (`Card.isCommander`). The DealDamage `DamageSource$` and Play refusals of it are gone    |

## `Defined$ Parent` on ChangeZone

`getDefinedCards` has no `Parent` case; `getDefinedSpellAbilities` answers the root (`AbilityUtils.java:1232-1233`), and
known-origin ChangeZone moves it through its spells-on-the-stack loop, skipping a root that is not a spell
(`ChangeZoneEffect.java:496-506`, `removeFromStack :1600-1633`), before and without the `Origin$` filter. Go: a new
`Ability.rootSpell` flag, set where sub-abilities, additional abilities and resolving Charm modes are built, tells a
link its root is a spell; additional abilities now also carry the root's `stackTargets`. `changeZoneKnownCards` moves
the resolving spell's card (still in its Stack zone; the stack item was popped) as the host, which the existing
`Origin$ Stack` path already does for `Defined$ Self`; `moveResolvedSpellToGraveyard` then skips it. A non-spell root
moves nothing; a `Defined$ Parent` line without `Origin$ Stack` is refused rather than filtered by an Origin Java never
reads there.

## Paying mana of the root ability

`Count$TotalManaSpent <valid>` (`AbilityUtils.java:1856-1865`) reads `getRootAbility().getPayingMana()`.
`ActivateAbility` now keeps the sources its mana payment spent (`paidLists.mana`, inherited by every link); a spell root
reads its card's cast sources. Any other root (a trigger, a replacement) is unresolved: none records its paying mana,
and Java's empty list for a trigger without a cost is not told apart from a trigger's paid `Cost$`.

## Smaller heads and gates

| Piece                               | Java                                                       | Go                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ----------------------------------- | ---------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `CastSA>Count$xPaid`                | `:1890-1891` with the cast SA as ctb                       | `castSAValue`: the cast's `castX`, a `Sac<X/…>` part's X included (`announceXFor`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `Count$SearchedLibrary.<players>`   | `:1941-1948`; `ChangeZoneEffect.java:1067-1079`            | `Player.librarySearched`, counted when the decider searches their own library (`Searched$` counts), reset at cleanup and restart (`Player.java:2499`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `Defined$ TargetedAndYou`           | `AbilityUtils.java:970-975`                                | the nearest player-targeting ability's players, then the activator, each once; none when nothing targets a player                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `RepeatCheckSVar$`                  | `RepeatEffect.java:39-52`: read after each repetition      | the up-front read is gone; an unresolvable check stays a pending error at its first read                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `ResultSubAbilities$ …,Else:SVar`   | `AbilityFactory.java:265-271`, `RollDiceEffect.java:478`   | the `Else` pair is the Else ability                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `NoteDoubles$`                      | `RollDiceEffect.java:526-533`; `AbilityUtils.java:415-423` | `Doubles` bound to 0 when no double was rolled and none was bound: Java's unset SVar reads 0                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `ReduceCost` `Amount$` unresolvable | `CostAdjustment.java:459-480` always applies it            | at payment (`spellCost` with `paying`, after the X and targets) the cast is refused with a pending `ReduceCost: Amount$ … is not resolvable`; it was skipped, so the spell cost more than in Java. The probe before the X announce (`announceXFor`) still skips such a line: a reduction never touches the X, and a `Relative$` amount may read the X or targets not chosen yet. Only the host's live faces are read (`liveTraitFaces`): a modal card's other face is not its state (Extus, Oriq Overlord's back-face reduction was read while the creature was cast). Blaster Hulk's inline amount resolves (`addInlineChecks` already compiles static params) |

## Smoke probe

`card-smoke -all -workers 8 -list`, same binary flags before and after:

| Measure      | Before | After  |
| ------------ | ------ | ------ |
| plays        | 30,917 | 30,947 |
| refused      | 2,701  | 2,671  |
| placed       | 379    | 379    |
| inconclusive | 81     | 81     |
| hung         | 0      | 0      |
| judged play  | 92.0%  | 92.1%  |

Per-card lists: scratchpad `adfb-smoke-before.txt`, `adfb-smoke-after.txt`, diff `adfb-smoke-diff.txt`.

| Flip            | Cards                                                                                                                                                                                                                                 | Why                                                                      |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| refused → plays | Reject Imperfection, Sound the Trumpets, Unravel, Bane's Contingency, Gale's Redirection                                                                                                                                              | targeted-spell heads                                                     |
| refused → plays | Beacon of Creation, Destruction, Immortality, Tomorrows and Unrest; Blue, Green, Red and White Sun's Zenith; Cerebral Eruption, Hanabi Blast, Haunting Imitation, Molten Birth, Sanguine Sacrament, Spell Crumple, Unexpected Results | `Defined$ Parent` on ChangeZone                                          |
| refused → plays | Forsworn Paladin, Jetmir's Fixer                                                                                                                                                                                                      | `Count$TotalManaSpent Treasure`                                          |
| refused → plays | Bloodroot Apothecary, Oath of the Grey Host, Thran Spider, A-Thran Spider, Keen Duelist                                                                                                                                               | `Defined$ TargetedAndYou`                                                |
| refused → plays | Plumb the Forbidden, Archive Trap, Sword of Dungeons & Dragons, Celebr-8000, Geode Golem                                                                                                                                              | `CastSA>xPaid`, `SearchedLibrary`, Repeat and dice gates, `IsCommander`  |
| plays → refused | Korvold, Gleeful Glutton (player property `SacrificedPermanentTypesThisTurn`), Valiant Changeling (`$CreatureType`), Lullmage's Domination (`TargetedController$` read against the spell being cast, no resolving ability)            | ReduceCost amount loud at payment; each was cast at full price, silently |
| cause changed   | Approach of the Second Sun (now `LibraryPosition$`), Mana Clash (now FlipCoin `RememberResult$`)                                                                                                                                      | past this row's refusal                                                  |

Pulse of the Forge already "played" (the probe's line never met its condition); its return now resolves.

## Deferred, with the reason

| Head / shape                                                    | Cards                                               | Why not here                                                                                                                                                                              |
| --------------------------------------------------------------- | --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PlaneswalkedToThisTurn`                                        | Unyaro                                              | a planechase record for one card whose ability then needs `Phases AllValid$`/`WontPhaseInNormal$` and an Effect `Triggers$`                                                               |
| `PromisedGift`                                                  | Coiling Rebirth                                     | Gift casting (`K:Gift`, the `GiftAbility` additional ability) is not ported                                                                                                               |
| FlipCoin `RememberResult$`, `FlippedTails`, `ClearCoinFlips$`   | Mana Clash                                          | its next refusal, a coin-flip record                                                                                                                                                      |
| inline compare operand, `NonTargetedController`, `HighestSpeed` | Spikeshell Harrier                                  | a compile change for one card                                                                                                                                                             |
| `TriggeredAttackedTargetAndYou` in a trigger's `CheckSVar$`     | Undercellar Sweep                                   | Java passes the `Trigger` as ctb (`CardTraitBase.java:472`); `getDefinedPlayers`' `Triggered` branch needs a `SpellAbility` (`AbilityUtils.java:1013`), so Java's answer there is unclear |
| `RememberCostMana$`                                             | Ice Cauldron, Jeweled Amulet                        | a remembered mana string read back by a mana ability with a spend restriction; no reader exists                                                                                           |
| `Game.costPaymentStack`                                         | Professor Hojo, Tezzeret (Type$ Ability ReduceCost) | ability ReduceCost lines are not applied at all                                                                                                                                           |
| X in the mana value of a stack card elsewhere                   | `cmcGE` and other valid properties                  | `Card.CMC` stays printed; only the amount measures add the X (`stackX`)                                                                                                                   |

## Decisions

| Decision                                                          | Why                                                                                                                  |
| ----------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Extend `stackTargetRef` with spell entries, not a parallel record | one ordered list is `getAllTargetChoices`; it is already recorded at the CR 608.2b check and inherited by every link |
| Scalars and a view card, not a frozen `Card` copy                 | the reader needs the zone, controller and cast record only; a value copy on every counterspell is not                |
| `rootSpell` on `Ability` rather than a root pointer               | IDs and values only (GO-9); set at the two places links are built                                                    |
| `IsCommander` false through `Card.isCommander`, refusals removed  | Java's own answer for a game with no commander; one helper, beside `commanderCasts`                                  |
| Loud ReduceCost amount refuses the cast                           | full price is as wrong as a free cast; three cards flip to refused and name their real gap                           |
| Bind `Doubles` 0, never parse `Number$ 1`                         | Java's unset SVar is 0; the spaced script value is never read in Java, so it is not read here                        |
| No new `PlayerController` method, no ADR                          | every piece mirrors a Java record or reads existing state                                                            |

## Forge defects (PORT-8)

| Where                                                                                 | Defect                                                                                                                                        |
| ------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `forge-gui/res/cardsfolder/m/mana_clash.txt:11`, `s/sword_of_dungeons_dragons.txt:12` | `Number$ 0` / `Number$ 1` with a space: `Integer.parseInt` (`AbilityUtils.java:1585-1587`) would throw. Latent: never read before `StoreSVar` |
| `forge-gui/res/cardsfolder/v/volcanic_offering.txt:5`                                 | unchanged from R9o: a sub-ability `RememberTargets$` Java never runs; still refused                                                           |

## Tests

`targetedspellrecords_test.go` (Sound the Trumpets on an X spell and a two-drop, Unravel full price and reduced,
`TargetedByTarget$` and Bane's Contingency, a targeted ability's host, `SpellTargeted$` with none, an operator and an
unknown measure), `diceelseandrepeat_test.go` (the `Else` pair, `NoteDoubles$` both ways, Repeat reading a stored check,
`TargetedAndYou`), `castandactivationpayments_test.go` (Forsworn Paladin with and without Treasure mana,
`TotalManaSpent` outside an activation and on a spell, Plumb the Forbidden with X 2 and 0, Blaster Hulk's reduction and
a loud unresolvable one, a `Sac<X>` reduction read only at payment, Extus cast without its back face's reduction, a
Charm mode's and a Branch result's `Defined$ Parent`, a Branch result's `SpellTargeted$`, `SearchedLibrary` with clone
and cleanup, Pulse of the Forge both ways, Beacon of Creation, `Defined$ Parent` off a trigger and without
`Origin$ Stack`). Scenarios `checksvar-sound-the-trumpets-reads-the-countered-x-spells-mana-value`,
`changezone-pulse-of-the-forge-returns-itself-when-the-target-has-more-life`,
`checksvar-plumb-the-forbidden-copies-itself-once-per-sacrificed-creature`.
