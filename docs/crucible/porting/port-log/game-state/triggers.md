# Port Log — Game State: Trigger Firing

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

The trigger-firing core: every `Mode$` checked at its event.

## Trigger firing: entering, dying, attacking, blocking, dealing damage, being discarded, becoming tapped, tapping for mana, casting a spell, the beginning of a step or phase, a player attacking, drawing a card, and watching another permanent

`checkETBTriggers` (`trigger.go`) is CR 603 at its narrowest: only `Mode$ ChangesZone` with `Destination$ Battlefield`
fires — a permanent's own "when this enters" trigger. `checkDiesTriggers` is the identical narrowness applied to the
corpus's other frequent `ChangesZone` shape — `Origin$ Battlefield`, `Destination$ Graveyard`, CR 700.4's "dies" —
checked only against the dying card's own `Card.Self` triggers. `isETBTrigger`/`isDiesTrigger` factor the shared
`Mode$ ChangesZone` + zone-key check both need, on top of `hasZone`, and both port `TriggerChangesZone.performTest`'s
own `Origin$`/`Destination$` semantics exactly rather than the narrower literal-only match they started with:
`hasZoneOrAny` treats a key that is absent, or present naming the literal value `"Any"`, as no restriction at all
(Java's own `!hasParam(key)` and `getParam(key).equals("Any")`), falling back to `hasZone`'s membership check only when
the param names something else. This closed two real gaps at once, not a hypothetical cleanup: `isDiesTrigger`'s own
`Destination$` used to require the literal value `"Graveyard"`, so 253 real `Destination$ Any` lines and 11 more with no
`Destination$` at all — CR 603.6c's own unqualified "leaves the battlefield" — never fired even on an ordinary death;
its `Origin$` used to require the literal value `"Battlefield"`, so 31 real lines naming only `Destination$ Graveyard`
("put into a graveyard from anywhere") missed the battlefield-origin instance of themselves too. `isETBTrigger` gained a
real `origin ZoneType` parameter for the identical reason on its own `Origin$` side — 21 real lines (12
`Origin$ Graveyard`, a reanimation-flavored "enters from a graveyard," plus a handful of
`Hand`/`Stack`/`Exile`/`AttractionDeck`) used to fire unconditionally regardless of where the card actually came from,
an over-firing bug this port had until `checkETBTriggers`/`otherETBTriggerMatches` threaded `origin` through from the
`origin := c.Zone` local already computed at each of the three real call sites (`permanentEffect`/`attachEffect`,
castspell.go; `Game.PlayLand`, land.go — read for `checkMovedReplacement` before `Game.Move` overwrites it).
`changesZoneResolvable` skips a `Mode$ ChangesZone` line naming
`ValidCause$`/`NotThisAbility$`/`ConditionYouCastThisTurn$`/`CheckOnTriggeredCard$`/`ExcludedOrigins$`/
`ExcludedDestinations$` (12 of 7,609 real lines combined) rather than firing unconditionally and guessing wrong
(PORT-8/GO-7) — `TriggerChangesZone.performTest`'s own remaining params this port has no reference vocabulary or
per-turn-cast-count tracking for.

`checkAttacksTriggers` is CR 508.3's own mode, `Mode$ Attacks`, entirely — not a `ChangesZone` shape at all, so it needs
no zone-key check, only `ValidCard` matched against the declared attacker. Ported from `TriggerAttacks.performTest`.
Unlike `checkETBTriggers`/`checkDiesTriggers`, this needs no separate "own" and "other" loop: `TriggerAttacks` itself
never special-cases the attacker's own trigger, so `ValidCard$ Card.Self`/`Creature.Self` (1,282 of 1,606 real corpus
lines — the attacker's own trigger) and `ValidCard$ Creature.YouCtrl` (an anthem-shaped "whenever a creature you control
attacks" watcher) fall out of the identical single walk over every battlefield permanent, just with a different
`ValidCard` string and a different host — one loop where `otherETBTriggerMatches` and `otherDiesTriggerMatches` each
needed a second one.

`Attacked$` (47 real lines) and `FirstAttack$` (4) are resolved now too. `performTest` matches `Attacked$` against
`AbilityKey.Attacked`, a single `GameEntity` (a player, planeswalker or Battle) rather than a `*Card` — resolved through
`attackedTargetMatches` (built for `AttackersDeclared`'s own `AttackedTarget$`, below, which faces the identical
player-shaped/card-shaped mixed-token dispatch problem for a whole collection of attacked entities) passed a one-element
`[]EntityID{g.combat.AttackTargets[attacker]}`, no new dispatch logic needed for the single-entity case. `FirstAttack$`
reads a new `Card.AttacksThisTurn` (card.go) — `CardDamageHistory.getCreatureAttacksThisTurn`'s own per-card counter,
incremented for each declared attacker right before `checkAttacksTriggers` runs (`DeclareCombatAttackers`, attack.go)
and reset every cleanup (`cleanupStep`, turn.go) alongside `Damage`/`LandsPlayed`/`CardsDrawnThisTurn` — checked as
`> 1` (ported directly from `performTest`'s own skip condition) immediately after the increment, so 1 means this is the
first attack this turn. 1,555 of 1,606 real lines carry neither param, unaffected either way.

Resolved: `Alone$` (60) — `attacksOtherCount` counts `Combat.Attackers` other than the declared attacker itself,
`CombatUtil.checkDeclaredAttacker`'s own `AbilityKey.OtherAttackers` (every other attacker declared this combat, not
just ones sharing this one's own defender — every real corpus line reads `Alone$ True`, never `False`);
`DefendingPlayerPoisoned$` (1) — `defenderOf(attacker)` (attack.go) is `AbilityKey.DefendingPlayer`, and
`Counters.Count(Poison)` (counters.go) is `Player.getPoisonCounters()`; `AttackDifferentPlayers$` (1) —
`attacksMultiplePlayers` walks `Combat.Attackers`/`Combat.AttackTargets` the same way `performTest` walks
`AbilityKey.Defenders`, since this port has no separate "defenders actually attacked" list of its own to build. Called
once per declared attacker, after tapping and target assignment both land (`Game.DeclareCombatAttackers`, attack.go) —
`enginelint`'s `attack` group gained `trigger` as a dependency, the same way `castspell`/`land`/`action` already have;
`trigger` itself gained `player` (the `Player` type `g.Player(defenderOf(attacker))` returns, player.go) and `parts`
(`Counters.Count`, counters.go).

`checkBlocksTriggers` is CR 509.2's own "whenever ~ blocks" mode, `Mode$ Blocks`, ported from
`TriggerBlocks.performTest` — the identical one-walk shape `checkAttacksTriggers` already established, since
`TriggerBlocks` never special-cases the blocker's own trigger either: `ValidCard` matched against the declared blocker
covers both "when this blocks" (106 of 127 real lines, `Card.Self`) and "whenever a creature you control blocks" alike.
`ValidBlocked$` (8 of 127 real lines, every one an "or blocks/becomes blocked by one or more X creatures" description)
is checked against `blk.Attacker` directly: `performTest` itself matches it against the FULL collection of attackers one
blocker blocks (`AbilityKey.Attackers`), ANY of which satisfying it fires the trigger once, but `checkBlocksTriggers` is
already called once per declared `Block` (a per-pair granularity, next paragraph), never once per blocker with every
attacker gathered — so checking the one attacker each call already has stands in for "any member of the collection"
correctly for the overwhelming single-attacker case, and no worse than the existing per-pair granularity for the rare
double-block one (a blocker legally blocking two attackers at once now fires once per matching attacker, where Java
fires once total — an existing divergence, not a new one this param introduces). Called once per declared `Block`, from
`DeclareCombatBlockers` (block.go), after `CanBlock` and `menaceLegal` have both already filtered the pairing down to a
legal one — CR 509.2 fires only for a legally declared block, not one either filter already dropped. `enginelint`'s
`trigger` group gained `combat` as a dependency (for `Block` itself); `block` gained `trigger`.

`checkSpellCastTriggers` is CR 603's own "a player casts a spell" mode, `Mode$ SpellCast`, ported from
`TriggerSpellAbilityCastOrCopy.performTest`. Like `checkAttacksTriggers`, one walk over every battlefield permanent
covers both a card's own trigger and another permanent watching for someone to cast a spell — Java's `performTest` never
special-cases the caster's own card either. `ValidCard` is optional here, unlike every other mode this port checks:
`matchesValidParam` (`CardTraitBase.java`) returns true for a missing param, and 100 of 1,435 real corpus lines carry no
`ValidCard` at all ("whenever you cast a spell," no restriction on which one). `ValidActivatingPlayer` is the corpus's
dominant param (1,216 of 1,435 — more common than `ValidCard` itself), matched by a new `matchesActivatingPlayer` rather
than `Matches` (valid.go): a `Player`, not a `Card`. Three bare values cover 1,191 of those 1,216 — `You`
(`activator == hostController`), `Opponent` (`activator != hostController`, the identical no-team simplification
`OppCtrl`/`OppOwn` already carry, `valid.go`'s own doc comment) and `Player` (unrestricted), via `matchesPlayerSpec`
(valid.go's own doc comment has the reason `matchesActivatingPlayer` calls it rather than `matchesPlayerBase` directly).
A qualified form (`Player.Opponent`, `Player.EnchantedBy`, `Player.NonActive`, `Player.Active`, `Player.Other`,
`Player.Chosen` — 25 lines) resolves 19 of those through `matchesPlayerSpec`'s own dotted-property layer:
`Player.Opponent` (12), `Player.NonActive` (4), `Player.Active` (2), `Player.Other` (1) and `Opponent.NonActive` (1).
`Player.EnchantedBy` (5) resolves from `Player.Attachments` ([`player-attachment.md`](player-attachment.md));
`Player.Chosen` (1) stays unresolved — a `ChosenPlayer` memory slot this port tracks nothing for — the identical "skip
rather than fire unconditionally" contract `hasAnyParam` already gives `checkAttacksTriggers`' own five unresolved
params. Also skipped via `hasAnyParam`: `ValidSA`/`ValidSAonCard` (a `SpellAbility`, not a `Card` — `Matches` cannot
evaluate one), `TargetsValid`/`CanTargetOtherCondition` (no per-trigger target-inspection hook),
`HasXManaCost`/`NoColoredMana`/ `SnowSpentForCardsColor` (no mana-payment-detail tracking past whether the cost was
paid), `IsSingleTarget` (no generic target-count reader) and `ActivatorThisTurnCast`/`ActivatorThisTurnCastEach` (a
per-turn cast-history count this port tracks nothing for). 1,163 of 1,435 real lines carry none of these. Fired from
both `CastSpell` branches (castspell.go) right where `SpellCast` (the event) already fires — cast time, not resolution,
the same place Java's own `checkTriggerEffects` call sits.

`checkDamageDoneTriggersToCard`/`checkDamageDoneTriggersToPlayer` are CR 603's own "whenever ~ deals damage" mode,
`Mode$ DamageDone`, ported from `TriggerDamageDone.performTest` — split in two because Java's own `DamageTarget` is a
`GameEntity` that can be either a `Card` or a `Player`, and `ValidTarget` needs a different evaluator for each:
`Matches` (valid.go) for the first, `matchesPlayerSpec` (the same one `matchesActivatingPlayer` uses) for the second — 4
of the 5 real qualified `ValidTarget$ Player.*` lines resolve this way (`Player.Opponent` x3, `Player.Other` x1); the
fifth, `Player.EnchantedBy`, does not (`matchesPlayerSpec`'s own doc comment, valid.go). `damageDoneMatches` is
everything the two share (one walk, `ValidSource`, `CombatDamage$`) except that one check. `ValidSource` is optional the
same way `SpellCast`'s own `ValidCard` is (`matchesValidParam`'s absent-is-a-pass contract); `CombatDamage$` is checked
against a hardcoded `true` at both real call sites (`dealPermanentDamage`/`dealPlayerDamage`, combatdamage.go), since
nothing outside combat deals damage in this port yet — a `CombatDamage$ False` line (a rare "whenever ~ deals noncombat
damage" shape) can never fire, and one carrying `True` or neither always passes that half. Resolved: `DamageAmount$` (8
of 1,080 real lines) — `damageAmountMatches` ports `performTest`'s own hand-rolled parse directly
(`fullParam.substring(0,2)`/`substring(2)`, never `AbilityUtils.calculateAmount` — every real line is a plain integer or
the literal `TargetToughness`, never an SVar reference), reusing `compareOp` (valid.go), the same
`Expressions.compare`-ported switch `compareMatches`'s own numeric-comparison branch already has, rather than a second
one. `TargetToughness` reads the damaged card's own folded `Toughness()` at the moment of damage — only meaningful for
`checkDamageDoneTriggersToCard`, so `checkDamageDoneTriggersToPlayer` passes `hasToughness = false` and a line naming it
there is skipped (a shape that cannot arise for real; Java itself would throw `ClassCastException` casting the player to
a `Card`). Not resolved: `ValidCause$` (1) — a `SpellAbility`, not a `Card`; `TargetRelativeToCause$`/
`TargetRelativeToSource$` (0 real lines alongside the shapes above) — a `GameEntity`-vs-`GameEntity` relative match this
port has no evaluator for. 1,079 of 1,080 real lines carry neither. Both `checkDamageDoneTriggersToCard`/`ToPlayer` and
their two real call sites (`dealPermanentDamage`/`dealPlayerDamage`, combatdamage.go) now thread the actual `amount`
dealt through, fired right after each already emits `DamageDealt`/`LifeChanged` — CR 510.2's "simultaneous" damage is
still applied one exchange at a time (`dealCombatDamageStep`'s own doc comment), so the trigger fires per exchange too,
the identical simplification. `enginelint`'s `combatdamage` group gained `trigger` as a dependency.

`checkDiscardedTriggers`/`otherDiscardedTriggerMatches` are CR 603's own "whenever ~ is discarded" mode,
`Mode$ Discarded`, ported from `TriggerDiscarded.performTest` — and the one mode so far that could NOT reuse the
single/own-plus-other walk shape unchanged. A "Card.Self" Discarded trigger (14 of 105 real lines, the Madness-adjacent
"when this card is discarded, you may cast it" shape) lives on a card that is never on the battlefield at the moment it
fires — it is discarded FROM HAND. Every other mode this port checks has its own `TriggerZones$` synthesized as
`Battlefield`/`Stack` by `CardFactoryUtil.java`, so a single battlefield walk always finds a "Card.Self" host among the
permanents being walked; Java's own `TriggerReplacementBase.zonesCheck` is unrestricted by default
(`validHostZones == null` passes regardless of the host's current zone), and a real corpus Discarded line commonly
carries no `TriggerZones$` at all, so it is checked wherever its host card currently sits. This was caught by
`TestCleanupFiresDiscardedTrigger` failing on the first implementation attempt (a battlefield-only walk, mirroring every
other mode) — a real, self-caught gap, not a hypothetical one. `checkDiscardedTriggers` now has an explicit "own" half,
checking the discarded card's own `Triggers` directly (its own Move-preserved `Controller` as source,
`checkDiesTriggers`' own precedent for reading a card no longer on the battlefield), before
`otherDiscardedTriggerMatches`' battlefield walk for a watcher ("Whenever you discard a card, ..."). `ValidPlayer` — a
`Player`, not a `Card` — is `matchesPlayerBase` again. Not resolved: `ValidCause$` (11 of 105 real lines) — a
`SpellAbility`, not a `Card`. 94 of 105 real lines carry none of it. Fired from `cleanupStep`'s own CR 514.1
discard-to-hand-size loop (turn.go), the only place this port discards a card at all today, right after `Move` —
`checkDiesTriggers`' own after-the-fact timing. `enginelint`'s `turn` group gained `trigger` as a dependency.

`checkTapsTriggers` is CR 603's own "whenever ~ becomes tapped" mode, `Mode$ Taps`, ported from
`TriggerTaps.performTest` — the identical single-walk shape `checkAttacksTriggers`/`checkBlocksTriggers`/
`checkDamageDoneTriggersToCard` already established. `player` is the tapped card's own controller at both real tap sites
this port has (`DeclareCombatAttackers`, attack.go; `TapLandForMana`, manaability.go) — neither models anyone else's
action tapping a permanent, so `ValidPlayer`'s own dominant real value ("You," all 4 of the real lines that carry it) is
exactly this. `Attacker$` (2 of 177 real lines) IS resolved: `isAttacker` reports whether the tap was caused by
attacking or something else, the same boolean `TriggerTaps.performTest` itself compares against `AbilityKey.Attacker`.
Not resolved: `FirstTime$` (1) — "the first time a permanent taps this turn," per-card-per-turn state this port tracks
nothing for; `Teamwork$` (1) — `CostTeamwork`, a cost-type this port has no concept of; `ValidCause$` (0). 173 of 177
real lines carry none of the three skipped params. `attack.go`'s own Vigilance check (only a non-Vigilant attacker
actually taps, CR 508.1f) is what `checkTapsTriggers` is called from inside, not unconditionally per declared attacker —
a Vigilance attacker never taps, so it correctly never fires the trigger either.

`checkTapsForManaTriggers` is `Mode$ TapsForMana`'s own narrower mode, ported from `TriggerTapsForMana.performTest` — a
mana ability specifically, its own separate Java `Trigger` subclass, so its own separate check here too rather than a
param on `checkTapsTriggers`. `Activator` — a `Player`, `matchesPlayerSpec`'s own job — is `player` again, the identical
"the tapped card's own controller" simplification, since `TapLandForMana` is the only real mana-ability call site this
port has, but unlike `checkTapsTriggers`'s own `ValidPlayer` this one real corpus line qualifies it:
`Activator$ Player.NonActive` resolves through `matchesPlayerSpec`'s own `Active`/`NonActive` property
(`Game.ActivePlayer()`) — `TapLandForMana` has no active-player restriction of its own (unlike `CastSpell`'s sorcery-
speed-only one, castspell.go), so both a `Player.Active` and a `Player.NonActive` activator are real, distinguishable
cases here. Not resolved: `Produced$` (3 of 65 real lines) — `"C"` (2) can never match anyway, since `TapLandForMana`
only ever produces one of the five colors, never colorless; `"ChosenColor"` (1) needs a runtime value this port has no
evaluator for; skipped together. 62 of 65 real lines carry none of it. Both `checkTapsTriggers` and
`checkTapsForManaTriggers` are methods, not free functions or types — `enginelint`'s own dependency checker only tracks
package-level free functions/types (`topLevelNames`' own doc comment, `tools/enginelint/lint.go`), so calling either
from `attack.go`/`manaability.go` needed no new allow-list entry, unlike every earlier `checkXTriggers` call site this
port has (`combatdamage`/`turn`'s own `trigger` allow-list entries were added on the same assumption, ahead of time, and
turned out unnecessary once this was understood — harmless, since an unused allow-list entry is not itself a violation).

`checkPhaseTriggers` is CR 500's own "at the beginning of a step or phase" mode, `Mode$ Phase`, ported from
`TriggerPhase.performTest` plus the base `Trigger` class's own `phasesCheck` (`Trigger.java`) — corpus-frequency
research, done only after the first nine modes above had already landed, found this the corpus's SECOND most frequent
trigger mode of all (2,362 real lines, ahead of `Attacks`' own 1,606), even though `phase.go`'s own
`PhaseByName`/`PhaseType.String` and `TestPhaseNamesRoundTrip` (event_test.go) were built with exactly this mode in mind
from M5's very first turn-structure work — plumbing that sat unused until now. `TriggerPhase.performTest` itself has no
`ValidCard` at all: there is no object a step or phase change happens TO, only the change itself, so this is a single
condition-only walk rather than a match-against-something one. Two differences from every earlier mode: `ValidPlayer$`
is matched against the ACTIVE player (`AbilityKey.Player`, set to `phaseHandler.getPlayerTurn()` in
`PhaseHandler.onPhaseBegin`), not the trigger's own host controller the way `SpellCast`'s `ValidActivatingPlayer`/
`DamageDone`'s `ValidTarget`-as-a-player/`TapsForMana`'s `Activator` all are — resolved through the identical
`matchesPlayerSpec` (valid.go) regardless, since the function itself only ever compares two `PlayerID`s and does not
care which one is "the active player" and which is "the host's controller." And `TriggerZones$` is not implicit the way
every earlier mode's is: `Attacks`/`Blocks`/`DamageDone`/etc. all only ever fire from a card already on the battlefield
in the real corpus, so their own single walk over `Zone(Battlefield, pid)` stands in for a `TriggerZones$` check no one
has needed yet, but `Mode$ Phase` is real from the graveyard (28 real lines), exile (3) and command zone (84) too — a
suspend/exiled-with-a-ticking-trigger shape, and (speculatively) an emblem's own upkeep trigger, though this port has no
way to create a Command-zone object yet either. `phaseTriggerZones` (trigger.go) is the four real zones (`Battlefield`,
`Command`, `Graveyard`, `Exile`) `checkPhaseTriggers` walks instead of `Battlefield` alone, and
`phaseTriggerZoneMatches` is `TriggerReplacementBase.zonesCheck`'s own contract (`TriggerZones$` absent passes
regardless of zone, 26 of 2,362 real lines carry none) applied per zone in that walk.

`Phase$` itself (`phaseTriggerMatches`, trigger.go) resolves through the existing `PhaseByName` for every bare token the
real corpus writes (`Upkeep`, `BeginCombat`, `Draw`, `Main1`, `EndCombat`, `Main2`, `Cleanup`, `Untap`,
`Declare Attackers`, and a `Main1,Main2` comma-list, 1 real line) — a new `phaseNameFold`, `PhaseByName`'s own
case-insensitive twin, is needed only for `End of Turn`'s own 3 real lines spelled `End Of Turn` (a capital `O`), the
one phase name the corpus itself is inconsistent about (`ZoneByName`'s own doc comment says zone names never are);
`PhaseByName` itself stays exact, since its other two callers (`fixture.go`'s `GameState` parser, its own test) have no
such inconsistency to tolerate. `Main` bare (29 real lines, every one paired with `PhaseCount$ 2` — Survival's own
cards, "at the beginning of your second main phase") is the one token `PhaseByName` deliberately refuses
(`TestPhaseNamesRoundTrip`'s own assertion, event_test.go, written well before this trigger mode existed to consume it)
— `PhaseType.parseRange`'s own special case for it (`PhaseType.java`) expands to both `Main1` and `Main2` when no
`PhaseCount$` narrows it to the second one alone, ported directly rather than through Java's own
`phaseHandler.getNumMain()` counting trick, since this port's own `PhaseType` already has separate `Main1`/`Main2`
constants Java's single `MAIN` constant does not.

`IsPresent$`/`PresentCompare$` (272, 104 real lines) and `CheckSVar$`/`SVarCompare$` (310) are resolved now too --
`checkPhaseTriggers`'s own `hasAnyParam` pre-filter used to name all three anyway, a leftover from before
`triggerCommonRequirementsMet` (below) existed that kept 686 real lines combined skipped even after that general
mechanism landed and started resolving the identical params for every other trigger mode. Removing them from the
pre-filter was the whole fix: `triggerEffectAPI`, which `checkPhaseTriggers` already called for every match that got
past the pre-filter, already ran `triggerCommonRequirementsMet` first, so nothing else needed to change for
`Mode$ Phase` triggers to start reaching `isPresentMatches`/`checkSVarMatches` (item 26's own `meetsCommonRequirements`
paragraph) the same way `Draw`-carrying ETB triggers already did.

Not resolved, still skipped via `hasAnyParam`: `Condition$` (`SpellAbilityCondition`'s own separate gate on the ability
itself, distinct from `CheckSVar$`/`SVarCompare$`'s now-resolved `CardTraitBase` shape -- 0 real `Mode$ Phase` lines
carry the bare key today; a real corpus check found only 6 lines carrying it at all, corpus-wide, none reachable through
this port's own static trigger walk); `APlayerHasMoreLifeThanEachOther$`/`APlayerHasMostCardsInHand$` (2 and 1 real
lines -- `Trigger.requirementsCheck`'s own whole-table comparisons, a third general gate distinct from both
`meetsCommonRequirements` and `phasesCheck`, below). `FirstUpkeep$`/`FirstUpkeepThisGame$`/`FirstCombat$`/`TurnCount$`
are gone from this pre-filter entirely now --
[`## Trigger.phasesCheck lands`](trigger-modes.md#triggerphasescheck-lands), resolves (or correctly skips) all four
generically, the identical "remove the now-redundant special case once the general mechanism lands" fix this exact
paragraph already made once for `IsPresent$`/`CheckSVar$`, above. `ValidPlayer$`'s own qualified forms
`matchesPlayerSpec` cannot resolve (`Player.EnchantedBy`, 14; `Player.Chosen`, 3; `Opponent.EnchantedBy`, 2;
`Player.isMonarch`, 1 — 20 real lines) stay unresolved for the identical reason `SpellCast`'s own
`Player.EnchantedBy`/`Player.Chosen` do (`matchesPlayerSpec`'s own doc comment); `Player.EnchantedController` (34) and
`You.descended` (10) resolve now too ("`Phase`'s own qualified `ValidPlayer$`: `EnchantedController` and `descended`,"
below). 2,001 of 2,065 real `ValidPlayer$` lines resolve regardless of any of this (`You`, 1,832; `Player`, 113;
`Opponent`, 47; `Player.Opponent`, 7; `Player.Other`, 2).

`TestAdvancePhaseFiresPhaseTriggerAtCorrectStep`, `TestAdvancePhaseSkipsPhaseTriggerAtWrongStep`,
`TestAdvancePhaseSkipsPhaseTriggerForNonActivePlayer`, `TestAdvancePhaseFiresPhaseTriggerForOpponentValidPlayer`,
`TestAdvancePhaseFiresMainSecondTriggerOnMain2`, `TestAdvancePhaseSkipsMainSecondTriggerOnMain1`,
`TestAdvancePhaseFiresPhaseTriggerFromGraveyard`, `TestAdvancePhaseFiresPhaseTriggerWhenIsPresentConditionMet`,
`TestAdvancePhaseSkipsPhaseTriggerWhenIsPresentConditionNotMet` and
`TestAdvancePhaseSkipsPhaseTriggerWithUnresolvedParam` (trigger_test.go, the last two proving the fix above -- one a
tapped-permanent positive fire, the other `Condition$`'s own remaining skip) prove all of the above against synthetic
Upkeep-watcher- and Survival-second-main-phase-shaped cards, `AdvancePhase` (turn.go) itself the real (non-test) caller
through `beginPhase`'s own new `g.checkPhaseTriggers(controller)` call, right after a step's own mechanical body (if
any) and right before the state-based-action check that already follows every phase entry.

`checkAttackersDeclaredTrigger` is CR 508.1's own "whenever a player attacks" mode, `Mode$ AttackersDeclared`, ported
from `TriggerAttackersDeclared.performTest` and the exact Java call site that fires it,
`PhaseHandler.declareAttackersStep`: once per combat, right after every declared attacker is tapped and assigned a
target, and only `if (!combat.getAttackers().isEmpty())` -- `DeclareCombatAttackers` (attack.go) carries the identical
guard, so a combat with no declared attacker never reaches the check at all. Corpus-frequency research (286 real
`S:T:Mode$ AttackersDeclared` lines) turned this mode up once `Phase`'s own remaining gaps stopped being worth chasing
further -- bigger than what `SpellCast`'s own unresolved remainder had left, and cheap here specifically because it
reuses three pieces of machinery already built for other modes rather than needing any of its own.

`AttackingPlayer$` (175 of 286 real lines) resolves through the existing `matchesPlayerSpec`, checked against
`g.activePlayer` rather than the trigger's own host controller -- `Combat.getAttackingPlayer()` is always the active
player in this port's own combat model (only the active player ever calls `DeclareCombatAttackers`), the identical
"compare two `PlayerID`s, whichever they represent" indifference `Phase`'s own `ValidPlayer$` check already relies on.
`TriggerZones$` reuses `phaseTriggerZones`/`phaseTriggerZoneMatches` outright rather than a second, mode-specific zone
list: 273 of 286 real lines carry `Battlefield`, but 7 carry `Command` and 5 carry `Graveyard`, the identical
minority-but-real split that made `Phase`'s own four-zone walk necessary in the first place, so nothing here needed its
own version of that decision.

`AttackedTarget$` (63 of 286) is the one genuinely new piece: `attackedTargetMatches` (trigger.go) ports
`CardTraitBase.matchesValid`'s own `Iterable` branch, which tries every attacked entity against the whole comma-split
spec and reports true the instant any one of them matches any one token -- real specs mix player-shaped tokens (`You`,
`Player,Planeswalker`) and card-shaped tokens (`Planeswalker.YouCtrl`) in the very same comma list
(`You,Planeswalker.YouCtrl`, 4 real lines), and Java's own dispatch is by the CANDIDATE's type (`Player.isValid` vs
`Card.isValid`), not the token's own syntax. `attackedTargetMatches` mirrors that: for every attacked entity and every
token, it tries `matchesPlayerSpec` when the entity is a player and `Matches` (valid.go) when it is a card, and a
mismatched attempt (a card-shaped token against a player entity, or the reverse) simply reports "not recognized" and
moves on -- `matchesPlayerSpec`'s own `ok=false` for an unrecognized base already gives this for free, and `valid.Parse`
building a `Spec` no real player or card token like `"You"` ever satisfies as a card base gives it for the reverse case
just as cheaply, with no new dispatch logic required. `attackedTargetsOf` (trigger.go) collects the distinct entities
actually attacked this combat, straight off `Combat.AttackTargets`, the same "only what someone is actually attacking"
set `PhaseHandler.java`'s own `for (GameEntity ge : combat.getDefenders()) if (!combat.getAttackersOf(ge).isEmpty())`
filter builds. A qualified player token `matchesPlayerSpec` cannot resolve (`Player.EnchantedBy`, 9;
`Player.hasInitiative`, `Player.IsPoisoned`, `Opponent.lifeGTX`, 1 each -- 12 of 63 combined) never matches through
either branch, so a trigger naming one of these simply never fires -- GO-7's usual outcome, reached here by never
matching rather than a separate skip check, since Java's own per-candidate dispatch has no "unresolvable, abort" case of
its own to mirror.

`ValidAttackers$`/`ValidAttackersAmount$` (123 of 286) is `validAttackersCountMatches` (trigger.go): how many of the
attackers its own caller passes in the `ValidAttackers$` spec matches (the existing `Matches`, valid.go) -- for this
mode, always `Combat.Attackers`, every attacker declared this combat; `Mode$ AttackersDeclaredOneTarget`'s own paragraph
below passes a narrower subset -- compared against `ValidAttackersAmount$` -- default `"GE1"`, Java's own
`getParamOrDefault("ValidAttackersAmount", "GE1")`, "one or more," matching the corpus's own dominant
`TriggerDescription$` phrasing, "whenever one or more Knights you control attack." Every real `ValidAttackersAmount$`
value is a plain two-letter-operator-plus-digit shape (`GE2`, `GE3`, `EQ1`, ...), never an SVar or `"X"` the way Java's
own code path technically allows for, so this reads the digits directly through the existing `compareOp` (valid.go)
rather than resolving an amount through `resolveAmount` (amount.go) -- the identical simplification
`damageAmountMatches` already made for `DamageDone`'s own `DamageAmount$`.

`IsPresent$`/`PresentCompare$` (14, 4) and `CheckSVar$` (13) are resolved now too, the identical fix `Phase`'s own
paragraph above describes: this function's own `hasAnyParam` pre-filter used to name all three anyway, blocking
`triggerCommonRequirementsMet` (already run by `triggerEffectAPI`, this function's own final step) from ever evaluating
them for a `Mode$ AttackersDeclared` trigger even after that general mechanism could.

Not resolved, still skipped via `hasAnyParam`, the same "whole line, not a guess" contract every other mode's own
skip-list already has: `Condition$` (1) -- `StaticAbility.java`'s own runtime gate, no equivalent for any trigger mode
yet.

`TestDeclareCombatAttackersFiresAttackersDeclaredTrigger`,
`TestDeclareCombatAttackersSkipsAttackersDeclaredTriggerWithNoAttackers`,
`TestDeclareCombatAttackersFiresAttackingPlayerYouTrigger`,
`TestDeclareCombatAttackersSkipsAttackingPlayerYouTriggerForDefender`,
`TestDeclareCombatAttackersFiresAttackedTargetYouTrigger`,
`TestDeclareCombatAttackersSkipsAttackedTargetYouTriggerForAttacker`,
`TestDeclareCombatAttackersFiresValidAttackersAmountTrigger`,
`TestDeclareCombatAttackersSkipsValidAttackersAmountTriggerBelowThreshold`,
`TestDeclareCombatAttackersFiresAttackersDeclaredTriggerWhenCheckSVarConditionMet`,
`TestDeclareCombatAttackersSkipsAttackersDeclaredTriggerWhenCheckSVarConditionNotMet` and
`TestDeclareCombatAttackersSkipsAttackersDeclaredTriggerWithUnresolvedParam` (trigger_test.go, the last two of these
proving the fix above -- a met/unmet `CheckSVar$` pair against a literal SVar, and `Condition$`'s own remaining skip)
prove all of the above against a synthetic watcher-permanent def, `DeclareCombatAttackers` (attack.go) itself the real
(non-test) caller through its own new `g.checkAttackersDeclaredTrigger()` call, right after the per-attacker
`checkAttacksTriggers` loop.

### `Mode$ AttackersDeclaredOneTarget`: the identical trigger, fired per defender

`TriggerType.java`'s own enum entry names it plainly: `AttackersDeclaredOneTarget(TriggerAttackersDeclared.class)` --
the exact same Java `Trigger` subclass `Mode$ AttackersDeclared` already ports, registered a second time under a
different `TriggerType` so `PhaseHandler.java`'s own `declareAttackersStep` can fire it at a second granularity. Reading
that method (`## checkAttackersDeclaredTrigger`, above, already covers half of it) shows the real split: right before
the one `runTrigger(AttackersDeclared, ...)` call that carries every attacker and every attacked defender at once, a
loop over `combat.getDefenders()` fires `runTrigger(AttackersDeclaredOneTarget, ...)` once for every defender that has
at least one attacker, `AbilityKey.Attackers` narrowed to `combat.getAttackersOf(ge)` and `AbilityKey.AttackedTarget`
narrowed to a singleton list holding just that one `ge`. Two different `RunParams` shapes feeding the identical
`performTest` body -- not two mechanisms to port, one mechanism called twice with different inputs.

`checkAttackersDeclaredOneTargetTrigger` (trigger.go, new) is that second call site, `checkAttackersDeclaredTrigger`'s
own sibling: for every distinct entity `attackedTargetsOf` (above) already finds attacked this combat, it computes
`attackersTargeting` (new) -- the subset of `Combat.Attackers` whose own `AttackTargets` entry is that one entity, in
`Combat.Attackers`' own declaration order, `combat.getAttackersOf(ge)` ported directly -- then walks the identical
four-zone/per-face/per-trigger loop `checkAttackersDeclaredTrigger` already has, just against
`isAttackersDeclaredOneTargetTrigger` instead of `isAttackersDeclaredTrigger` and this one defender's own
attacker/target pair instead of the whole combat's.

The two functions no longer duplicate the actual param dispatch: a new `attackersDeclaredParamsMatch` (trigger.go) holds
`Condition$`'s own skip and the `AttackingPlayer$`/`AttackedTarget$`/`ValidAttackers$` checks both callers already had
inline, taking the attacker subset and target list as plain parameters rather than reading
`g.combat.Attackers`/`attackedTargetsOf` itself -- `checkAttackersDeclaredTrigger` passes the whole-combat pair,
`checkAttackersDeclaredOneTargetTrigger` passes the per-defender one. `validAttackersCountMatches` (above) changed the
identical way, taking `attackers []CardID` as a parameter instead of hardcoding `g.combat.Attackers` -- its only other
caller updated to pass that explicitly, and its own doc comment above updated to describe both shapes rather than only
the first one it had before this mode existed.

35 of the corpus's own 35 real `Mode$ AttackersDeclaredOneTarget` lines resolve end to end -- a full corpus scan of the
mode's own `[A-Za-z0-9]+\$` vocabulary (vocabscan-style, by hand) turns up only `TriggerZones$`/`TriggerDescription$`/
`Mode$`/`Execute$`/`AttackedTarget$` (35 each), `ValidAttackers$` (28), `AttackingPlayer$` (6), `ValidAttackersAmount$`
(5) and `Secondary$` (1) -- every one of those already resolved by
`attackersDeclaredParamsMatch`/`phaseTriggerZoneMatches`, the identical dispatch `checkAttackersDeclaredTrigger` already
has. 0 real lines carry `Condition$`/`OptionalDecider$`/`CheckDefinedPlayer$`/`IsPresent$`, the params that stay
unresolved for the plain `AttackersDeclared` mode's own remainder (`## AttackersDeclared`, above) -- this mode's own
real corpus population happens to avoid every one of those, not a claim this file makes about the mode in general.

`DeclareCombatAttackers` (attack.go) calls `g.checkAttackersDeclaredOneTargetTrigger(controller)` right before
`g.checkAttackersDeclaredTrigger(controller)`, `PhaseHandler.java`'s own call order (every per-defender firing inside
the loop, the one whole-combat firing after it) -- CR 603.3b's own APNAP ordering still applies within each call
separately (`pushTriggeredAbilities`, [`## Stack`](turn-stack-combat.md#stack)), called twice for two related but
independent trigger sweeps rather than once for a combined one, the same "each check function owns its own push" pattern
every other trigger mode in this file already has.

Four new tests in `trigger_test.go`, `attackersDeclaredOneTargetTriggerDef` the new helper
(`attackersDeclaredTriggerDef`'s own sibling, `Mode$ AttackersDeclaredOneTarget` in place of `Mode$ AttackersDeclared`):
`TestDeclareCombatAttackersFiresAttackersDeclaredOneTargetTriggerOncePerDefender` (two attackers split across the
defending player and a planeswalker they control fires a bare watcher twice, not once -- the mode's own defining
difference from `AttackersDeclared`, proven by drawing two cards rather than one);
`TestDeclareCombatAttackersOneTargetAttackedTargetMatchesOnlyThatDefender` (`AttackedTarget$ You` matches only the
player-targeted firing, not the planeswalker-targeted one);
`TestDeclareCombatAttackersOneTargetValidAttackersCountsOnlyThatDefendersOwnAttackers`/
`TestDeclareCombatAttackersOneTargetValidAttackersCountsBothAttackingOneDefender` (the same two attackers split across
two defenders fails `ValidAttackersAmount$ GE2` for both firings -- neither defender has two attackers of its own, even
though `Combat.Attackers` has two total -- while sending both at the SAME defender satisfies it, the positive twin
proving the negative one is not simply "never fires"). The whole-combat/per-defender split itself was regression-checked
by reverting `DeclareCombatAttackers`' own new call and confirming three of the four failed with the expected wrong card
count before restoring it (the fourth, a "must not fire" case, stays trivially true either way, so toggling it proves
nothing on its own).

### CR 603.3d's own "may" triggered ability

`OptionalDecider$` is the corpus's own single largest unresolved trigger param by real line count in this file -- 1,584
real lines corpus-wide, 1,497 of them on a `T:` line -- and, until now, this port had no resolution-time hook to ask
anyone anything at all. Reading `TriggerHandler.java`'s own `registerActiveTrigger` (the method every trigger's own
fire, `pushTriggeredAbilities`'s own Java counterpart, calls before the ability ever reaches the stack) shows why this
could not be a trigger-fire-time gate the way every other unresolved restriction in this file already is:
`OptionalDecider$` never stops an ability from being pushed. `sa.setOptionalTrigger(true)` and
`decider = AbilityUtils.getDefinedPlayers(host, regtrig.getParam("OptionalDecider"), sa).get(0)` run unconditionally the
moment a `T:` line names the key at all, and the built `WrappedAbility` goes onto the stack the identical way a
mandatory trigger's own does. The confirmation itself happens only once `WrappedAbility.resolve()` runs -- right before
its own `getActivatingPlayer().getController().playSpellAbilityNoStack(sa, false)` call, the literal line that runs the
ability's own body:

```java
if (decider != null) {
    if (!decider.isInGame()) {
        decider = SpellAbilityEffect.getNewChooser(sa, decider);
    }
    if (!decider.getController().confirmTrigger(this)) {
        return;
    }
}
```

A decline is a hard `return` out of `resolve()` -- before the ability's own body runs, and before anything chained onto
it does either, since Java's own `resolveSubAbilities` call happens inside `playSpellAbilityNoStack`'s own resolution
path, never reached at all once `resolve()` has already returned. "May" is a property of the WHOLE ability, chain
included, not a gate on its own top-level effect alone.

This port's own equivalent: `Ability` (ability.go) gained an `Optional bool` field, and `Registry.Resolve` (effect.go)
checks it first, before anything else that function does (including the `ErrUnimplemented` check for an API this port
has not built -- a declined "may" is real Magic's own outcome regardless of whether this port could have run the ability
behind it, so asking first matches CR 603.3d more closely than erroring out a card a controller would have declined
anyway):

```go
func (r *Registry) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if a.Optional && !controller.ConfirmOptionalTrigger(g, a.Controller, a.Source) {
		return nil
	}
	...
}
```

A new `PlayerController.ConfirmOptionalTrigger` (control.go, the interface's own twenty-fifth method) is
`WrappedAbility.resolve()`'s own `decider.getController().confirmTrigger(this)`, decider always `a.Controller` --
`Ability.Optional`'s own doc comment has the reason no separate decider field exists yet: only `OptionalDecider$ You` is
resolved, and "You" always means the ability's own controller (`AbilityUtils.getDefinedPlayers(host, "You", sa)`'s own
single-element result), already exactly what `Ability.Controller` already carries at every one of this port's own call
sites. `true` runs the ability (and its own `SubAbility$` chain via `resolveSubAbility`, subability.go,
`Registry.Resolve`'s own recursive call for it) exactly as if it had never been optional; `false` skips both,
`Registry.Resolve`'s own early `return nil` before `resolveSubAbility` is ever called at all -- the identical "whole
ability, chain included" semantics `WrappedAbility.resolve()`'s own early return already has.

`Ability.Optional` itself is set by a new `triggerIsOptional` (trigger.go), folded into `triggerEffectAPI`'s own shared
gate alongside `triggerPhasesCheck`/`triggerCommonRequirementsMet` -- every one of `triggerEffectAPI`'s own twenty-eight
call sites across every trigger mode this port has built runs through it for free, no per-mode code needed. 1,506 of the
corpus's own 1,584 real `OptionalDecider$` lines (95%) name "You": `triggerIsOptional` resolves exactly that value and
refuses every other one (`TriggeredCardController`, 43 real lines; `True`, 11; `TriggeredSourceController`, 5;
`TriggeredPlayer`/`Opponent`, 4 each; `EnchantedController`, 3;
`TriggeredAttackingPlayer`/`TriggeredActivator`/`TargetedController`, 2 each; eleven more distinct values, 1 real line
apiece) -- each names a decider this port has no resolver for, a further `AbilityUtils.getDefinedPlayers`-shaped
question distinct from the "You" case `host.Controller()` already answers directly, with nobody correct for
`ConfirmOptionalTrigger` to ask yet. `triggerEffectAPI` reports `ok=false` for those, the identical "skip the whole
line, don't guess" contract every other unresolved trigger restriction in this file already has (GO-7) -- chosen over
Java's own "push regardless, ask nobody in particular" fallback, since a wrong guess at WHO decides is worse than not
firing at all:

```go
func triggerIsOptional(t *compile.Ability) (optional, ok bool) {
	decider, present := t.Param("OptionalDecider")
	if !present {
		return false, true
	}
	if !strings.EqualFold(decider, "You") {
		return false, false
	}
	return true, true
}
```

Three already-built modes resolve real lines they could not before, simply by reaching this shared gate for the first
time -- none needed their own code changed, only their own `hasAnyParam` skip-list's own explicit `"OptionalDecider"`
entry removed, where one existed: `Mode$ Untaps`'s own remaining 3 real lines (`## checkUntapsTriggers`, above -- 30 of
30 now), `Mode$ LifeGained`'s own 7
([`## M6's fourth effect: GainLife, and Mode$ LifeGained`](effects-m6-first.md#m6s-fourth-effect-gainlife-and-mode-lifegained)
-- 93 of 98), `Mode$ BecomesTarget`'s own 12
([`## Mode$ BecomesTarget lands`](trigger-modes.md#mode-becomestarget-lands-and-targeting-gets-a-second-real-event) --
101 of 118, none of the 12 also naming `Valiant$`/`ActivationLimit$`/`Static$`). A fourth, `Mode$ LandPlayed`'s own 3
real lines ([`## Mode$ LandPlayed lands`](trigger-modes.md#mode-landplayed-lands), above -- 38 of 42 now), is a real
correctness fix rather than only a new resolution: `checkLandPlayedTriggers`'s own `hasAnyParam` call never named
`"OptionalDecider"` at all, so search_the_city.txt's/jokulmorder.txt's/burgeoning.txt's own real "you may..." lines were
reaching the old `triggerEffectAPI` (which had no check for the key either) and firing unconditionally -- the identical
class of bug `LifeGained`'s own `ActivationLimit$` fix
([`## M6's fourth effect: GainLife, and Mode$ LifeGained`](effects-m6-first.md#m6s-fourth-effect-gainlife-and-mode-lifegained))
already caught and fixed earlier this session, found here by the same routine due-diligence rather than by a bug report.

83 more real lines name `OptionalDecider$` on a sub-ability's own SVar body
(`SVar:TrigFoo:DB$ ... | OptionalDecider$ You | ...`) rather than a `T:` line -- a chained `SubAbility$`'s own
independent "may," CR 700.2's own "then" text sometimes itself optional. A smaller, separate gap this change does not
reach: `resolveSubAbility` (subability.go) builds its own `child := Ability{...}` with no `Optional` field set at all,
so a chained sub-ability is never treated as optional regardless of its own `OptionalDecider$`, deliberately out of
scope for this chunk (`Ability.Optional`'s own doc comment names it explicitly).

Six new tests in a new `optionaltrigger_test.go`, `optionalDeciderTriggerDef` the new helper (an Enchantment with one
`Mode$ AttackersDeclared` trigger naming `OptionalDecider$ <decider>`, `Execute$` chaining `DB$ Draw` into a
`SubAbility$ DB$ LoseLife` -- SubAbility chaining, `## Sub-ability chaining lands`, above, reused to prove a decline
skips the WHOLE ability): `TestConfirmedOptionalTriggerRunsWholeAbilityChain` (confirmed, both the draw and the chained
life loss happen); `TestDeclinedOptionalTriggerSkipsWholeAbilityChain` (declined, neither does -- `Registry.Resolve`'s
own early return before `resolveSubAbility` is ever reached, not merely the top-level `DB$ Draw` itself refusing);
`TestUnresolvedOptionalDeciderSkipsTriggerWithoutAsking` (`OptionalDecider$ TriggeredCardController` skips before ever
calling `ConfirmOptionalTrigger` at all -- proven by a clean run against a `ScriptedController` with an EMPTY
`optionalTrigger` queue: asking at all would panic, `scriptExhausted`'s own contract, so a clean pass proves the
question was never posed, not merely answered no). Three more in `untaps_test.go`, against `Mode$ Untaps`'s own real
shape rather than a synthetic mode: `TestStartTurnFiresUntapsTriggerNamingOptionalDeciderWhenConfirmed`/
`TestStartTurnSkipsUntapsTriggerNamingOptionalDeciderWhenDeclined`/
`TestStartTurnSkipsUntapsTriggerNamingUnresolvedOptionalDecider` (this file's own former
`TestStartTurnSkipsUntapsTriggerNamingOptionalDecider`, rewritten now that `OptionalDecider$ You` no longer means
"always skip" -- DOC-16). Two more in `trigger_test.go`, against `Mode$ LandPlayed`'s own real burgeoning.txt shape:
`TestPlayLandFiresLandPlayedTriggerNamingOptionalDeciderWhenConfirmed`/
`TestPlayLandSkipsLandPlayedTriggerNamingOptionalDeciderWhenDeclined`.

Every new gate was regression-checked: `Registry.Resolve`'s own confirm check, temporarily removed, made both
`TestStartTurnSkipsUntapsTriggerNamingOptionalDeciderWhenDeclined` and
`TestDeclinedOptionalTriggerSkipsWholeAbilityChain` fail with the ability having run anyway, before being restored;
`triggerIsOptional`'s own `"You"`-only check, temporarily widened to accept any decider, made
`TestStartTurnSkipsUntapsTriggerNamingUnresolvedOptionalDecider` panic on the scripted controller's own empty queue
(proving it would otherwise have asked), before being restored. `ScriptedController` gained a twenty-fifth queue,
`optionalTrigger []bool`, and `QueueConfirmOptionalTrigger`/`ConfirmOptionalTrigger` alongside it, the identical
slice-pop-and-panic-when-empty shape every other queued decision already has. `scriptedMulliganController`
(mulligan_test.go), the only other real `PlayerController` implementer, gained a `panic`-stub `ConfirmOptionalTrigger`
too, `ChooseEnchantTarget`'s own sibling stub for a method the mulligan tests never reach.

`checkDrawnTriggers` is CR 120.3's own "whenever you draw a card" mode, `Mode$ Drawn`, ported from
`TriggerDrawn.performTest`, called from `DrawCards`' own per-card loop (turn.go) -- a call site that already existed in
exactly the shape this needed before this mode existed to consume it: `DrawCards`' own doc comment always drew one card
at a time rather than moving `n` at once, reasoning that "matters once something reacts to an individual draw rather
than the batch," written well before this trigger mode was the thing that did.

161 real `S:T:Mode$ Drawn` lines corpus-wide (vocabscan), reusing `phaseTriggerZones`'s own four-zone walk again (150 of
161 real lines carry `TriggerZones$ Battlefield`, but 6 carry `Command` and 3 carry `Graveyard` -- the identical
minority-but-real split every other mode's own zone walk exists for). `ValidCard$` (156 of 161) is checked against the
drawn card itself through the existing `Matches` (valid.go): every real value (`Card.YouCtrl`, `Card.OppOwn`,
`Card.YouOwn`, `Card.OwnedBy`, a bare `Card`, ...) is an ordinary valid-string this port's evaluator already covers,
needing nothing new -- unlike `AttackersDeclared`'s own `AttackedTarget$`, nothing here mixes a player-shaped token with
a card-shaped one, so there is no dispatch-by-candidate-type problem to solve. `ValidPlayer$` (13) resolves through the
existing `matchesPlayerSpec`, against the player who actually drew (`TriggerDrawn.performTest`'s own
`AbilityKey.Player`, set to the drawing player in `Player.java`'s own `drawCard`) rather than the trigger's own host
controller -- the identical "compare two `PlayerID`s, whichever they represent" indifference `Phase`'s own
`ValidPlayer$` and `AttackersDeclared`'s own `AttackingPlayer$` checks already rely on.

`Number$` (79 of 161) is the one genuinely new piece: a new `Player.CardsDrawnThisTurn` (player.go) -- `LandsPlayed`'s
own per-turn-counter shape, ported from Java's own `numDrawnThisTurn` (`Player.java`). `DrawCards` (turn.go) increments
it once per card, in the identical order Java's own `drawCard` does: the increment happens BEFORE the trigger check
runs, so `Number$ 2` means "the second card this player has drawn this turn, counting this one," not "about to draw its
second." `cleanupStep` (turn.go) resets it to zero for every player alongside `LandsPlayed`, the same per-turn reset
scope (`Player.java`'s own `onCleanupPhase` resets `numDrawnThisTurn` in the identical place). Not resolved, skipped via
`hasAnyParam`: `FirstCardInDrawStep$` (5) -- Java's own separate `numDrawnThisDrawStep`, a narrower per-draw-STEP
counter (as opposed to per-turn) this port tracks nothing for, since nothing else needs it yet; `ForReveal$` (5) --
Java's own `AbilityKey.CanReveal`, a reveal-while-drawing flag (Sensei's Divining Top-adjacent shapes) this port's own
`DrawCards` has no equivalent state for.

`TestDrawCardsFiresDrawnTriggerForCardYouCtrl`, `TestDrawCardsSkipsDrawnTriggerForCardYouCtrlWhenHostControlledByOther`,
`TestDrawCardsFiresDrawnTriggerForValidPlayerOpponent`,
`TestDrawCardsSkipsDrawnTriggerForValidPlayerOpponentWhenHostIsDrawer`,
`TestDrawCardsFiresDrawnTriggerForMatchingNumber`, `TestDrawCardsSkipsDrawnTriggerForNonMatchingNumber` and
`TestDrawCardsSkipsDrawnTriggerWithUnresolvedParam` (trigger_test.go) prove all of the above, `DrawCards` (turn.go)
itself the real (non-test) caller through its own new `g.checkDrawnTriggers(pid, id, ...)` call, right after the
`CardDrawn` event each drawn card already emits.

`matchesPlayerBase` (valid.go) is the shared `You`/`Opponent`/`Player` three-way dispatch, factored out once a THIRD
caller needed the identical switch `matchesActivatingPlayer` and `matchesValidDefender` had each already written
separately — `SpellCast`'s own `ValidActivatingPlayer`, `CantBlockBy`'s own `ValidDefender`, `DamageDone`'s own
`ValidSource`/`ValidTarget`-as-a-player, `Discarded`'s own `ValidPlayer`, `Taps`'s own `ValidPlayer` and `TapsForMana`'s
own `Activator` all reuse it, returning `(matched, ok)` so a caller with its own additional dispatch
(`matchesValidDefender`'s own `"Player.controls<Type>"`) can tell "does not match" apart from "not a shape this function
recognizes at all, keep looking." It lives in `valid.go` rather than `trigger.go`/`staticability.go` specifically so
both groups can reach it without a new `enginelint` cross-dependency: both already allow `valid`.

`matchesPlayerSpec`/`matchesPlayerProperty` (valid.go) sit on top of `matchesPlayerBase`, the identical `Base.Property`
split `Player.isValid` (Player.java) itself does on the first `.` before ANDing every `+`-joined property via
`hasProperty`/`PlayerProperty.playerHasProperty` — ported once `SpellCast`'s own `ValidActivatingPlayer` turned out to
need it for 19 of its 25 real qualified lines (above). `matchesPlayerSpec` checks the base clause through
`matchesPlayerBase` first, then the property (when there is one) through `matchesPlayerProperty`, which itself tries
`matchesPlayerBase` again first (Java's own "Opponent"/"You" property branches reuse the identical base check a property
token gets) before its own two further conditions: `Active`/`NonActive` (`Game.ActivePlayer()`, the same accessor
`Matches`' own `ActivePlayerCtrl` property already reads for a `*Card`) and `Other` (not `sourceController` — collapses
into the identical check as `Opponent` under this port's own no-team simplification). Only `matchesActivatingPlayer`
(`SpellCast`), `checkDamageDoneTriggersToPlayer` (`DamageDone`'s own `ValidTarget`-as-a-player) and
`checkTapsForManaTriggers` (`TapsForMana`'s own `Activator`) call it — the other three `matchesPlayerBase` callers
(`CantBlockBy`'s `ValidDefender`, `Discarded`'s `ValidPlayer`, `Taps`'s `ValidPlayer`) keep calling `matchesPlayerBase`
directly, verified against the real corpus to carry zero qualified lines for those exact params, so routing them through
the extra split would add a dependency with nothing real to resolve.

Both started out checked only against the one card's own `Triggers`, not every other permanent's own triggers watching
for someone else's zone change. `otherETBTriggerMatches`/`otherDiesTriggerMatches` close that gap for both: every
permanent already on the battlefield, other than the one that just entered (for dying, no such exclusion is even needed
— the dying card already left the battlefield by the time `checkDiesTriggers` runs, so it was never going to appear in
the walk), gets its own `Triggers` walked against the event too (`ValidCard` matched with the watcher as source and the
watcher's own controller — Impact Tremors' `Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Creature.YouCtrl`
fires off any other creature its controller's own control enters; a Blood-Artist-shaped
`Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Creature.YouCtrl` does the identical thing for dying).
`TriggerZones$ Battlefield`, present on most corpus lines shaped either way, needs no separate check: only a card this
loop already found on the battlefield is walked, so a watcher not there is never considered. An earlier version of this
port's own reasoning held that dying's version of this gap could not close the same way, since the dying card is
"already gone" by the time `checkDiesTriggers` runs — that reasoning does not survive scrutiny: it is the _watcher_ that
needs to still be on the battlefield, not the dying card being matched against, and the watcher is exactly as unaffected
by some other card leaving as an ETB watcher is by one arriving. `otherDiesTriggerMatches` is the identical shape to
`otherETBTriggerMatches`, once that was noticed.

What made this reachable in the first place, back when only the ETB half existed, was a discovery rather than new work:
`compile.Face.Triggers []*Ability` has held every `T:` line's compiled form since M3 — the identical typed shape an
`A:`/`S:`/`R:` line compiles to (`compile.Ability`, `Record: Trigger`, `Name` the trigger's own `Mode$` value) — read by
nothing downstream until now. A trigger's `Execute$` key names an `SVar` whose own compiled `Ability.Name` is the API
its effect would run (`triggerEffectAPI`); `APIByName` turns that back into an `APIType` the same way `CastSpell`
already turns `c.Type().Has(cardtype.Creature)` into `APIPermanentCreature`. No new parser, no compile-pipeline change —
`T:`/ `SVar:` lines were always compiled, just never consumed.

`checkETBTriggers` is called from every real "moves onto the battlefield" site this port has —
`permanentEffect.Resolve`, `attachEffect.Resolve` (castspell.go), `Game.PlayLand` (land.go) — rather than from
`Game.Move` itself: `Matches` (valid.go) depends on `game.go`, so `game.go` cannot depend back on anything that calls it
without the exact cycle `ability.go`'s own doc comment already describes for why `Ability` moved out of `effect.go`.
`checkDiesTriggers` is called from every state-based action that can move a card to a graveyard (`action.go`):
`destroyLethalToughness`, `destroyDamagedCreatures`, `destroyZeroLoyalty`, `assignBattleProtector`'s
no-eligible-protector fallback, `destroyZeroDefense`, `resolveLegendRule`, `resolveWorldRule`,
`cleanupDanglingAttachments` — eight call sites, one per SBA that can put a permanent in a graveyard from the
battlefield today. `enginelint`'s `trigger` group sits above `game`/`valid`/`stack`; `castspell`/`land` gained it as a
dependency for the ETB half, `action` for the dies half.

A matching trigger pushes its own `Ability` onto the stack the same way `CastSpell` pushes a cast spell (CR 603.3's own
"a triggered ability becomes an object on the stack") — `ResolveStack`'s loop ([`## Stack`](turn-stack-combat.md#stack))
finds it on top the instant the resolution that pushed it returns, with nothing "frozen" in between since nothing can
respond either way. `Ability` gained a `Params *compile.Ability` field (`ability.go`) to carry the trigger's own
`Execute$` sub-ability along onto the stack: `triggerEffectAPI` used to return only the `APIType`, discarding
`Defined$`/`NumCards$`/every other key the sub-ability itself carries, which worked only as long as nothing on the stack
ever needed to read one back — `drawEffect` (below) is the first thing that does. Resolving what fires is still mostly a
gap: a trigger's own `Execute$` sub-ability can be any of the 203 corpus-frequency effects M6 owns; `NewRegistry`
implements `Draw`, `DealDamage`, `GainLife`, `Pump`, `PumpAll` and `LoseLife` today (alongside casting a permanent and
an Aura, M5's own two) — `ResolveStack` reports `ErrUnimplemented` for the other 196 once `PutCounter` is counted
alongside them too. `TestCastSpellFiresETBTrigger`, `TestDestroyLethalToughnessFiresDiesTrigger`,
`TestDestroyLethalToughnessFiresOtherPermanentsWatchingDiesTrigger`, `TestCastSpellFiresOtherPermanentsWatchingTrigger`,
`TestDeclareCombatAttackersFiresAttacksTrigger`, `TestDeclareCombatAttackersFiresOtherPermanentsWatchingAttackTrigger`,
`TestDeclareCombatAttackersFiresAttacksTriggerWhenAttackedConditionMet`,
`TestDeclareCombatAttackersSkipsAttacksTriggerWhenAttackedConditionNotMet`,
`TestDeclareCombatAttackersFiresFirstAttackTriggerOnFirstAttackThisTurn`,
`TestDeclareCombatAttackersSkipsFirstAttackTriggerWhenAlreadyAttackedThisTurn`,
`TestDeclareCombatBlockersFiresBlocksTrigger`, `TestDeclareCombatBlockersFiresOtherPermanentsWatchingBlockTrigger`,
`TestDeclareCombatBlockersSkipsBlocksTriggerWithUnresolvedParam`,
`TestCastSpellFiresSpellCastTriggerForControllerActivatingPlayer`,
`TestCastSpellSkipsSpellCastTriggerForNonControllerActivatingPlayer`,
`TestCastSpellFiresSpellCastTriggerForOpponentActivatingPlayer`,
`TestCastSpellSkipsSpellCastTriggerWithUnresolvedParam`, `TestDealCombatDamageFiresDamageDoneTriggerToPlayer`,
`TestDealCombatDamageFiresDamageDoneTriggerToCard`, `TestDealCombatDamageFiresOtherPermanentsWatchingDamageDoneTrigger`,
`TestDealCombatDamageFiresDamageDoneTriggerWithMatchingDamageAmount`,
`TestDealCombatDamageSkipsDamageDoneTriggerWithNonMatchingDamageAmount`,
`TestDealCombatDamageFiresDamageDoneTriggerWithMatchingTargetToughness`,
`TestDeclareCombatAttackersFiresAloneTriggerWhenAttackingAlone`,
`TestDeclareCombatAttackersSkipsAloneTriggerWithAnotherAttacker`,
`TestDeclareCombatAttackersFiresDefendingPlayerPoisonedTrigger`,
`TestDeclareCombatAttackersSkipsDefendingPlayerPoisonedTriggerWithNoPoison`,
`TestDeclareCombatAttackersFiresAttackDifferentPlayersTrigger`,
`TestDeclareCombatAttackersSkipsAttackDifferentPlayersTriggerAgainstOnePlayer`, `TestCleanupFiresDiscardedTrigger`,
`TestCleanupFiresOtherPermanentsWatchingDiscardedTrigger`, `TestCleanupSkipsDiscardedTriggerWithUnresolvedParam`,
`TestDeclareCombatAttackersFiresTapsTrigger`, `TestDeclareCombatAttackersSkipsTapsTriggerForVigilantAttacker`,
`TestTapLandForManaFiresOtherPermanentsWatchingTapsTrigger`,
`TestDeclareCombatAttackersSkipsTapsTriggerWithUnresolvedParam`, `TestTapLandForManaFiresTapsForManaTrigger` and
`TestDeclareCombatAttackersDoesNotFireTapsForManaTrigger` (trigger_test.go) prove nine of the twelve modes against
synthetic Elvish-Visionary-, Rotting-Regisaur-, Blood-Artist-, Impact-Tremors- and attacking/blocking/damage-dealing/
discarded/tapping/casting-creature-shaped cards, each compiled through the real pipeline; the tenth, eleventh and
twelfth, `Phase`, `AttackersDeclared` and `Drawn`, each have their own test list right after their own descriptive
paragraph, above.

Corpus-frequency: 5,688 cards carry the ETB shape (`Destination$ Battlefield`); 1,341 carry the dies shape
(`Origin$ Battlefield` + `Destination$ Graveyard`, 205 of them watching some other creature rather than themselves,
`ValidCard$ *.Other`/`*.YouCtrl` — the count behind `otherDiesTriggerMatches`); 1,606 carry `Mode$ Attacks`, 1,555 of
them resolvable; 127 carry `Mode$ Blocks`, 119 of them resolvable; 1,080 carry `Mode$ DamageDone`, 1,079 of them
resolvable; 105 carry `Mode$ Discarded`, 94 of them resolvable; 177 carry `Mode$ Taps`, 173 of them resolvable; 65 carry
`Mode$ TapsForMana`, 62 of them resolvable; 1,435 carry `Mode$ SpellCast`, 1,163 of them resolvable; 2,362 carry
`Mode$ Phase` — the corpus's second most frequent mode of all, ahead of `Attacks` itself — most of them resolvable (the
`checkPhaseTriggers` paragraph, above, has the precise breakdown across `Phase$`/`ValidPlayer$`/`TriggerZones$` and what
remains unresolved); 286 carry `Mode$ AttackersDeclared` — most of them resolvable (the `checkAttackersDeclaredTrigger`
paragraphs, above, have the precise breakdown across `AttackingPlayer$`/`AttackedTarget$`/`ValidAttackers$` and what
remains unresolved); 161 carry `Mode$ Drawn`, 156 of them resolvable through `ValidCard$` alone, most of the rest
through `ValidPlayer$`/`Number$` too (the `checkDrawnTriggers` paragraph, above, has the precise breakdown).

One real fixture changed because of this: `cast-a-battle-spell-reaches-the-stack` (formerly
`...-resolves-to- battlefield`) stops at the stack rather than resolving fully, because every Battle in the corpus turns
out to carry its own "create a token" ETB trigger — `Invasion of Belenon` among them — and `TestScenarios` has no way to
assert an expected `RunActions` failure the way an internal test can with `errors.Is`. The other four permanent-type
fixtures (creature, artifact, enchantment, planeswalker) are unaffected: none of those four cards carries a `T:` line.

### `Mode$ AttackerBlocked` and `Mode$ AttackerBlockedByCreature`: CR 509.2's own other side

`checkBlocksTriggers` (above) is the blocker's own "whenever this blocks." CR 509.2 groups a second, symmetric family
into the same declare-blockers step: the attacker's own "whenever this becomes blocked" (`Mode$ AttackerBlocked`, 127
real lines, `TriggerAttackerBlocked.java`) and "whenever this becomes blocked by a creature"
(`Mode$ AttackerBlockedByCreature`, 102 real lines, `TriggerAttackerBlockedByCreature.java`), both real now.

The two Java classes differ in granularity, and `checkAttackerBlockedTriggers`/`checkAttackerBlockedByCreatureTriggers`
(trigger.go) each keep it: `AttackerBlockedByCreature` fires once per (attacker, blocker) pair, `checkBlocksTriggers`'s
own exact mirror image — `ValidCard$` matched against `blk.Attacker` where `checkBlocksTriggers` matches it against
`blk.Blocker`, `ValidBlocker$` matched against `blk.Blocker` where `checkBlocksTriggers` matches `ValidBlocked$` against
`blk.Attacker` — called from the identical per-`Block` loop in `DeclareCombatBlockers` (block.go) `checkBlocksTriggers`
already runs in. `AttackerBlocked` fires once per attacker instead, with every legal blocker gathered first:
`DeclareCombatBlockers` now builds a `map[CardID][]CardID` (blockers by attacker) alongside `blocks` itself while it
already walks them for `checkBlocksTriggers`/`checkAttackerBlockedByCreatureTriggers`, then calls
`checkAttackerBlockedTriggers` once per distinct attacker afterward, the whole group in hand.

`ValidCard$` (127 of 127, 74 with nothing else — an unqualified "becomes blocked") matches the attacker directly, the
same shape every other trigger mode's own `ValidCard$` already does. `ValidBlocker$`/`ValidBlockerAmount$` (53 of 127
carry one or both) needed a new `validCardsCountMatches` (trigger.go): `validAttackersCountMatches`'s own
count-and-compare shape (item 26's own `AttackersDeclared` account, above) generalized from `g.combat.Attackers`
specifically to any `[]CardID`, since the group here is one attacker's own blockers, gathered fresh per attacker, never
the whole combat's. Neither mode needs a separate own/other loop: `TriggerAttackerBlocked`/
`TriggerAttackerBlockedByCreature` never special-case the attacker's own trigger any more than `TriggerBlocks` did for
the blocker's, so one battlefield walk already covers "this creature becomes blocked" and "a creature you control
becomes blocked" alike — `TestDeclareCombatBlockersFiresOtherPermanentsWatchingAttackerBlockedTrigger` (trigger_test.go)
proves it directly, the identical shape `TestDeclareCombatBlockersFiresOtherPermanentsWatchingBlockTrigger` already
proved for `Blocks`.

Not resolved: `ValidCard$ LessPowerThanBlocker`/`ValidBlocker$ LessPowerThanAttacker` (1 real line each) — a hardcoded
power comparison rather than a valid-string (Skulk's own hardcoded-`X` shape, `skulkBlocks`, staticability.go, for a
different pairing), explicitly refused in both `checkAttackerBlockedTriggers`/`checkAttackerBlockedByCreatureTriggers`
rather than left to fall through to a bare-word valid-string parse that would silently match no card and never fire —
the identical observable result, but for the wrong reason, PORT-8's own concern regardless of whether it happens to look
harmless here. `Mode$ AttackerBlockedOnce` (3 real lines, its own once-per-declaration Java class,
`TriggerAttackerBlockedOnce.java`) is [`m6-r9g-attack-triggers.md`](m6-r9g-attack-triggers.md)'s.

`TestDeclareCombatBlockersFiresAttackerBlockedTrigger`/
`TestDeclareCombatBlockersFiresOtherPermanentsWatchingAttackerBlockedTrigger`/
`TestDeclareCombatBlockersFiresAttackerBlockedTriggerWhenBlockerAmountMatches`/
`TestDeclareCombatBlockersSkipsAttackerBlockedTriggerWhenBlockerAmountDoesNotMatch` prove `AttackerBlocked`, the last
two via a real two-blocker gang block;
`TestDeclareCombatBlockersFiresAttackerBlockedByCreatureTriggerWhenValidBlockerMatches`/
`TestDeclareCombatBlockersSkipsAttackerBlockedByCreatureTriggerWhenValidBlockerDoesNotMatch`/
`TestDeclareCombatBlockersFiresAttackerBlockedByCreatureTriggerOncePerBlocker` prove `AttackerBlockedByCreature`, the
last proving the per-pair granularity directly: two matching blockers draw two cards, not one.

### `CardTraitBase.meetsCommonRequirements`: the one gate every trigger mode shares

Every check-triggers function above shares one blind spot: `Trigger.java`'s own `performTest` never runs at all unless
`meetsCommonRequirements` passes first (CardTraitBase.java,
[`## Layer 7, Layer 4...`](layers.md#layer-7-layer-4-layer-5-layer-6-layer-8-and-layer-2-pttypemodcolormodkeywordmodrulesmodcontrolmod-cardpowertoughnesstypecolorshaskeywordcontroller-and-the-first-real-continuous-callers)
above already ports its `IsPresent$`/`Condition$` cousins for static abilities), and until now this port checked none of
it for a trigger -- a real card naming `IsPresent$`/`CheckSVar$`/`Threshold$`/whatever alongside an already-resolved
mode fired unconditionally, the gate silently never consulted. A corpus tally directly against `T:` lines (grepping the
bare param name catches some `S:`/`A:` lines too, a different switch on the identical name --
`StaticAbility. checkConditions`'s own `Condition$`, `SpellAbilityCondition`'s own `Condition$`/`ConditionPresent$`,
neither this gate's problem) puts every param `meetsCommonRequirements` reads at ~1,271 real `T:` lines.

`triggerCommonRequirementsMet` (trigger.go, new) closes 1,148 of them. It is called from inside `triggerEffectAPI`
itself, not duplicated at each of the eighteen check-triggers call sites: every one of them already funnels its own
match through that one function to turn it into a pushed `Ability`, so `triggerEffectAPI` gaining `g`/`host`/`amounts`
parameters (threaded from the identical `for _, face := range h.Def.Faces { for _, t := range face.Triggers {` loop
every caller already has `face.Amounts` inside) reaches every mode for free. The eighteen call sites themselves needed
only their own argument list updated -- `triggerEffectAPI(t)` to `triggerEffectAPI(g, h, face.Amounts, t)` (or `c`/`w`
in the two own/other split functions still walking a bare `*Card` rather than a `host` id) -- nothing about their own
matching logic changed.

`isPresentMatches` ports `IsPresent$`/`PresentCompare$`/`PresentZone$`/`PresentPlayer$` (624 of ~1,271, and the
identical `IsPresent2$` pair counted with it) -- `PresentZone$` a comma list defaulting to `Battlefield`, `ZoneByName`
per entry; `PresentPlayer$` `"You"` (host's own controller only) or the corpus's own default `"Any"` (every player --
Java's own three additive You/Opponent/Allies blocks collapsed to the one partition a single-valued param actually
produces, since a real line is never all three sources at once); the counted set run through `Matches` the identical way
every other valid-string check in this port already is. `PresentDefined$` (40 of 624) skips: no
Defined$-to-cards resolver for an arbitrary reference exists yet, `drawDefinedPlayers`' own narrow You/Opponent form
(draweffect.go) being the only `Defined$`
evaluator built so far, and it resolves players, not cards.

`checkSVarMatches` ports `CheckSVar$`/`SVarCompare$` (474) -- both sides resolved through a new `resolveNamedAmount`
(amount.go): `ptParam`'s own literal-or-named-SVar shape (continuous.go), factored out once this needed the identical
resolution against a `*Card` rather than reading one specific `*compile.Ability` param directly; `ptParam` itself is now
a two-line wrapper calling it. A line also naming `CheckSecondSVar$` (0 real `T:` lines today) skips outright -- Java
ORs a second check against the first (the identical "secondCheck" shape `SpellAbilityCondition.areMet` already has for
its own `ConditionCheckSVar$`/`OrOtherConditionSVarCompare$` pair), and nothing forces guessing at that shape blind when
the real corpus does not exercise it.

`boolFlagMatches` ports the six-times-repeated `"True".equalsIgnoreCase(params.get(key)) != predicate()` shape for
`Metalcraft$`/`Delirium$`/`Threshold$`/`Hellbent$`/ `FatefulHour$` (38 combined) -- reusing `continuousConditionMet`'s
own underlying predicates
([`## Condition$: the one gate all six appliers share`](layers.md#condition-the-one-gate-all-six-appliers-share)): the
identical player-state question, asked here as an explicit flag (`Threshold$ False` meaning "must NOT have threshold" is
as real a line as `Threshold$ True`) rather than as the whole condition the way a static ability's own
`Condition$ Threshold` is. Moving them mattered structurally, not just for reuse:
`battlefieldArtifactCount`/`graveyardCoreTypeCount` lived in continuous.go, and leaving them there while
`resolveNamedAmount` needed to serve both `ptParam` (continuous.go) and `triggerCommonRequirementsMet` (trigger.go)
would have made `continuous`→`trigger` and `trigger`→`continuous` both real edges -- a dependency cycle
`tools/enginelint`'s own acyclic-parts rule (ADR-0003's own premise, `javacycles` at the Java-package level, this tool's
own equivalent inside `internal/engine`) catches immediately, not a false positive the way `ControlEffect`'s own
`Player`-named field or `compile.Ability`'s own textual collision with `ability.go`'s `Ability` were
([`## Layer 7...`](layers.md#layer-7-layer-4-layer-5-layer-6-layer-8-and-layer-2-pttypemodcolormodkeywordmodrulesmodcontrolmod-cardpowertoughnesstypecolorshaskeywordcontroller-and-the-first-real-continuous-callers)
and [`## Replacement effects`](replacement.md#replacement-effects-entering-the-battlefield-tapped) above, both). Both
functions moved to a new `playerstate.go` (its own `enginelint.json` group, depending on nothing either `continuous` or
`trigger` themselves provide), and `resolveNamedAmount` moved to amount.go, next to `resolveAmount` itself -- neither
shared function belongs to just one caller, so neither stayed in either caller's own file.

`lifeTotalMatches` ports `LifeTotal$`/`LifeAmount$` (12) -- `"You"` (`g.Player(host.Controller()).Life`) and
`"ActivePlayer"` (`g.Player(g.ActivePlayer()).Life`), the corpus's own two real `T:` values;
`OpponentSmallest`/`OpponentGreatest` carry none and stay unresolved.

Not resolved, each skipped whole rather than treated as met (GO-7, the identical "cannot evaluate, so do not fire" rule
an unresolved `Affected$`/`Condition$` value already has elsewhere in this port): (`Revolt$` is resolved since R9d,
[`m6-r9d-cast-leftovers.md`](m6-r9d-cast-leftovers.md)); `WerewolfTransformCondition$`/ `WerewolfUntransformCondition$`
(65) -- Innistrad's own day/night mechanic, keyed off a "spells cast last turn" list this port tracks nowhere;
`CheckDefinedPlayer$` (20) -- every real line qualifies it with `isMonarch`, `hasInitiative`, `withMostLife` or
`withMostType`, mechanics this port has none of, not a shape a general
Defined$-to-players resolver
could close on its own even if one existed (`Player.withMostLife` as a valid string resolves now, in `matchesPlayerProperty`,
but `CheckDefinedPlayer$`
is a different reader).

`ManaSpent$`/`ManaNotSpent$` (8) -- no paying-colors-by-cast tracked; `Adamant$` (1); `Bloodthirst$`, `Monarch$`,
`EnduringStory$`, `DayTime$` and `ClassLevel$` (0 real `T:` lines each, dormant rather than actively skipped).

Eleven new tests (`TestPlayLandFiresETBTriggerWhen*`/`SkipsETBTriggerWhen*`, trigger_test.go) prove `IsPresent$` both
ways plus its `PresentZone$`/`PresentDefined$` variants, `CheckSVar$` both ways, `Threshold$` both ways, `LifeTotal$`
both ways, and `Revolt$`'s own unresolved skip -- each driven through a real `Game.PlayLand` rather than a synthetic
call, `commonReqTriggerLandDef`'s own new helper mirroring `continuousDef`'s reasoning (a land needs no mana cost to
move, so the setup stays about the common-requirements param under test).

## Keyword triggers (ADR-0038)

The triggers a printed keyword expands to need these in the trigger layer:

- **`Mode$ AttackerBlocked` and `AttackerBlockedByCreature` record the attacker** (`triggeredObjects.attacker`), as
  `TriggerAttackerBlocked`/`TriggerAttackerBlockedByCreature.setTriggeringObjects` do, so
  `Defined$ TriggeredDefendingPlayer` (Afflict) and `TriggeredAttacker` resolve on them.
- **`Attacked$ Player.withMostLife`** (Dethrone) matches through `matchesPlayerProperty`'s `withMostLife`: no player in
  the game has more life (`PlayerProperty.java:353`; a lost player is not counted).
- **`Condition$ Evolve`** on a `Mode$ ChangesZone` trigger is `Trigger.meetsRequirementsOnTriggeredObjects`' CR 702.100c
  branch (`Trigger.java:377`), which Java also runs for the Evolve keyword itself: the entering creature has greater
  power or toughness than the host, both creatures. `evolveCheck` (`trigger.go`) runs it when the trigger is matched and
  `Ability.evolve` keeps the entering creature so `Registry.resolve` runs `evolveMet` again against current values, as
  `WrappedAbility.resolve` does. Before this, `Condition$ Evolve` on the two cards that script it by hand was ignored
  and the trigger fired for every creature. `TestEvolveChecksAgainOnResolution` pins the second check.
- **`Count$CardPower`/`CardToughness`** are amount heads: the source's net power or toughness (`countValue`).

`WrappedAbility.resolve`'s general `requirementsCheck` (the rest of the intervening "if": `IsPresent$`, `CheckSVar$`,
and so on, skipped by `NoResolvingCheck$`) is still not re-run at resolution; only Evolve's own check is.
