# M5 Rules Coverage

- **Status:** Active. P4 qualitative half open: every `missing` and `impl-no-fixture` row is listed in
  [Backlog](#backlog)
- **Applies to:** P4 exit gate, qualitative half — "every step transition, every layer, every SBA"
  ([open plan, Exit gates](../00-master-implementation-plan-open.md#exit-gates))

Checkable form of that sentence. One row per rule; a rule the port handles in parts gets one row per part. **Half met
when [Backlog](#backlog) is empty.** Reason: "every layer, every SBA" read as prose has no end state; a row per CR item
does, and a gap nobody listed is a gap nobody closes.

Paths: Java relative to `forge-game/src/main/java/forge/game/`, Go relative to `crucible/internal/engine/`. Fixture
column: kebab-case = directory under `crucible/testdata/scenarios/`, `Test…` = Go test in `crucible/internal/engine/`.

## Status vocabulary

| Status | Meaning | Gate |
| --- | --- | --- |
| `covered` | Implemented, and a named fixture or Go test asserts it | Green |
| `impl-no-fixture` | Implemented, nothing asserts it | Open, Backlog |
| `missing` | Not implemented, implemented wrong, or only partly — reason in row | Open, Backlog |
| `n/a` | Out of scope; reason in Rule cell | Green |

`covered` by a `Test…` name only is green but a fixture candidate: plan P4 counts scenarios, TEST-5 prefers them. Same
convention as [test-port-matrix](test-port-matrix.md#status-vocabulary).

CR letters are current Comprehensive Rules lettering (704.5 skips `l` and `o`). Java and Go comments quote older letters
in places; the Rule cell flags each mismatch. Reason: citing a comment's stale letter sends a reader to the wrong rule.

---

## State-based actions

Go entry: `CheckStateBasedActions` (`action.go:109`). Java entry: `GameAction.checkStateEffects`
(`GameAction.java:1400`) plus `Player.checkLoseCondition` (`player/Player.java:2022`). Design notes:
[state-based-actions](port-log/game-state/state-based-actions.md).

| Rule | Java site | Go site | Fixture(s) | Status |
| --- | --- | --- | --- | --- |
| 704.3 — repeat check until no SBA applies | `GameAction.java:1417`, `:1613-1616` | `action.go` (`checkStateBasedActions`, repeats to 9 passes) | `TestANewRoleReplacesTheOlderOneOfTheSameController`, `TestCheckStateBasedActionsLethalToughnessCascadesToAttachments` | `covered` |
| 704.5a — life 0 or less loses | `player/Player.java:2040-2041` | `action.go:144-155` | `life-loss-at-zero`, `life-loss-below-zero-also-loses` | `covered` |
| 704.5b — drew from empty library loses | `player/Player.java:2031-2034` | `action.go:147-152`, `turn.go:412` | `draw-from-empty-library-loses` | `covered` |
| 704.5c — ten or more poison loses | `player/Player.java:2045-2047` | `action.go:152` | `poison-loss`, `poison-nine-survives` | `covered` |
| 104.2a/104.4a — last player standing wins, zero left draws (rides on 704.5a-c) | `GameAction.java:1911` | `action.go:158-182` | `life-loss-eliminates-one-player-game-continues`, `TestCheckStateBasedActionsEmitsGameEndedOnce` | `covered` |
| 704.5d — token outside battlefield ceases to exist | `GameAction.java:1889` | `token.go:64` | `TestTokenLeavingBattlefieldCeasesToExist` | `covered` |
| 704.5e — spell copy off the stack ceases to exist | `zone/MagicStack.java:681` | `game.go:570` | `TestCopySpellCopyCeasesWhenMoved` | `covered` |
| 704.5e — card copy outside stack/battlefield ceases to exist | not located | — | missing | `missing` |
| 704.5f — toughness 0 or less to graveyard, no regeneration | `GameAction.java:1455-1458` | `action.go:271` | `counters-minus-one-reduces-toughness-to-zero-destroyed`, `TestCheckStateBasedActionsLethalToughnessDies` | `covered` |
| 704.5g — lethal damage destroys; indestructible and regeneration apply | `GameAction.java:1459-1482` | `action.go:322` | `combat-single-block-kills-attacker`, `combat-indestructible-attacker-survives-lethal-damage`, `TestRegenerateEffectSavesFromLethalDamage` | `covered` |
| 704.5h — any deathtouch damage destroys | `GameAction.java:1475` | `action.go:322` | `combat-deathtouch-kills-regardless-of-toughness`, `combat-blocker-deathtouch-kills-regardless-of-toughness` | `covered` |
| 704.5i — planeswalker at 0 loyalty to graveyard (Java comment unnumbered) | `GameAction.java:1992` | `action.go:379` | `planeswalker-zero-loyalty-destroyed` | `covered` |
| 704.5j — legend rule, same name per controller | `GameAction.java:2006` | `action.go:551` | `legend-rule-keeps-one`, `legend-rule-three-copies-keeps-one` | `covered` |
| 704.5j — Corner Case 1: borrowed names vs corpus-wide nonlegendary creature names | `GameAction.java:2006-2067` | — | missing | `missing` |
| 704.5k — World rule, newest survives (Java `:1560` and Go comments say 704.5m) | `GameAction.java:2068` | `action.go:636` | `world-rule-keeps-newest`, `world-rule-three-copies-keeps-newest` | `covered` |
| 704.5m — Aura unattached, host gone, or host fails `Enchant`/protection | `GameAction.java:1511-1515`, `:1754-1777` | `action.go:710-727` | `aura-falls-off-when-host-dies-sent-to-graveyard`, `aura-enchant-restriction-sends-illegal-aura-to-graveyard` | `covered` |
| 704.5m — Aura enchanting a player | `GameAction.java:1760-1761` | `action.go` `playerEnchantLegal` | `TestCurseOnAPlayerWhoLeftTheGameGoesToTheGraveyard`, `TestEnchantOpponentAuraOnItsOwnControllerGoesToTheGraveyard`, `TestEquipmentAttachedToAPlayerBecomesUnattached` | `covered` |
| 704.5n — Equipment/Fortification unattaches when host leaves | `GameAction.java:1766-1772` | `action.go` (`cleanupDanglingAttachments`, `enchantSpecOf`) | `sba-equipment-attached-to-a-non-creature-becomes-unattached` | `covered` |
| 704.5n — Equipment/Fortification unattaches when host is illegal (`canBeAttached`) | `GameAction.java:1760-1761` | `action.go` (`cleanupDanglingAttachments`, `enchantSpecOf`) | `sba-equipment-attached-to-a-non-creature-becomes-unattached` | `covered` |
| 704.5p — attached creature or battle unattaches (Java `:1759` says 704.5q) | `GameAction.java:1760` | `action.go` (`cleanupDanglingAttachments`) | `sba-equipment-attached-to-a-non-creature-becomes-unattached` | `covered` |
| 704.5q — +1/+1 and -1/-1 counters annihilate | `GameAction.java:1845` | `action.go:234` | `counters-annihilate-plus-minus` | `covered` |
| 704.5q — "counters can't be removed" guard | `GameAction.java:1852` | — | missing | `missing` |
| 704.5r — counter cap; Java checks dream counters only | `GameAction.java:1867` | `action.go` (`trimDreamCounters`) | `TestMaxCounterTrimsExcessDreamCountersAsAStateBasedAction` | `covered` |
| 704.5s — Saga at final chapter sacrificed | `GameAction.java:1649` | `action.go` (`sacrificeCompletedSagas`) | `sba-saga-at-its-final-chapter-with-none-on-the-stack-is-sacrificed` | `covered` |
| 704.5t — dungeon on last room completed | `GameAction.java:1736` | `ventureeffect.go:251` | `TestVentureWalksLostMineToCompletion` | `covered` |
| 704.5u — n/a: space sculptor appears only on Space Beleren, an acorn card, legal in no sanctioned format | `GameAction.java:1801` | — | — | `n/a` |
| 704.5v — battle at 0 defense to graveyard | `GameAction.java:1705` | `action.go:497` | `battle-zero-defense-destroyed` | `covered` |
| 704.5v — exception: battle is source of a trigger still on the stack | `GameAction.java:1705-1710` | `action.go` (`destroyZeroDefense`, `hasSourceOnStack`) | `TestZeroDefenseBattleWithItsTriggerOnTheStackSurvivesUntilItResolves` | `covered` |
| 704.5w — battle with no protector gets one | `GameAction.java:1676-1700` | `action.go:442` | `battle-protector-assigned-to-opponent`, `TestAssignBattleProtectorAsksWhenNoneSet` | `covered` |
| 704.5x — Siege controller is its protector, reassign | `GameAction.java:1680` | `action.go:442` | `TestAssignBattleProtectorReassignsWhenProtectorIsOwnController` | `covered` |
| 704.5y — two or more Roles from one controller, newest stays | `GameAction.java:1714` | `action.go` (`resolveRoleRule`) | `TestANewRoleReplacesTheOlderOneOfTheSameController` | `covered` |
| 704.5z — start your engines with no speed sets speed 1 | `GameAction.java:1548-1552` | `action.go` (`startYourEngines`) | `TestStartYourEnginesGivesSpeedOne` | `covered` |
| n/a: Contraption SBA — Unstable, silver-bordered, legal in no constructed format | `GameAction.java:1778` | — | — | `n/a` |
| 704.6a/704.6b — n/a: Two-Headed Giant team life/poison (CR 810 variant); Java quotes 704.6b at `player/Player.java:2046` | `player/Player.java:2046` | — | — | `n/a` |
| 704.6c — n/a: 21 combat damage from one commander (CR 903 casual variant; PORT-6 scopes to constructed) | `player/Player.java:2052` | — | — | `n/a` |
| 704.6d — n/a: commander to command zone from graveyard/exile (CR 903 variant) | `GameAction.java:1830` | — | — | `n/a` |
| 704.6e/704.6f — n/a: Archenemy scheme reset, Planechase (variants); letters unverified | `GameAction.java:1745` | — | — | `n/a` |

704.6 sub-letters past `b` are from memory, not checked against the CR text; the n/a status does not depend on them.

---

## Layers and timestamps

Go entry: the rebuild block in `CheckStateBasedActions` (`action.go:195-210`). Java entry:
`GameAction.checkStaticAbilities` (`GameAction.java:1076`), layer loop `:1120` over
`StaticAbilityLayer.CONTINUOUS_LAYERS` (`staticability/StaticAbilityLayer.java:45-46`); per-layer bodies in
`staticability/StaticAbilityContinuous.java`. Design notes: [layers](port-log/game-state/layers.md),
[ADR-0025](../adr/0025-continuous-effect-evaluation-order.md).

| Rule | Java site | Go site | Fixture(s) | Status |
| --- | --- | --- | --- | --- |
| 613.1 — layers applied in order 1-7, each seeing earlier layers from this pass | `GameAction.java:1120` | `action.go` (`checkStateBasedActionsPass`: control, text, type, color, abilities, P/T; resolved Animate/Pump at the start of their layer) | `TestLayerSevenSeesThisPassLayerFourTypes`, `TestCloneLaterLayersApplyOverCopiedValues`, `TestApplyContinuousControlRunsBeforeKeywordSoYouCtrlSeesTheNewController` | `covered` |
| 613.2a Layer 1a — copy effects, "becomes a copy" | `card/Card.java:4255`, `ability/effects/CloneEffect.java:52` | `cloneeffect.go:218`, `:58` | `copy-effect-attacker-fights-as-copied-creature`, `TestCloneHostBecomesPermanentCopyOfTarget` | `covered` |
| 613.2a Layer 1a — "enters as a copy" (`K:ETBReplacement:Copy`, Clone, Phantasmal Image) | `ability/effects/CloneEffect.java:52` | `entersascopy.go` | `TestEntersAsCopyCloneCopiesTheChosenCreature`, `clone-enters-as-copy-and-gets-the-copied-etb-trigger` | `covered` |
| 613.2b Layer 1b — face-down characteristics (manifest, cloak, morph) | `card/Card.java:832` | `facedown.go:18` | `TestManifestAndCloakAndTurnFaceUp` | `covered` |
| Layer 2 — control-changing effects | `StaticAbilityContinuous.java:571`, `ability/effects/ControlGainEffect.java:91` | `continuous.go:879`, `gaincontroleffect.go` | `TestApplyContinuousControlGrantsControlOfEnchantedCreature`, `TestGainControlEffectTakesAndUntaps` | `covered` |
| Layer 3 — text-changing effects (`GainTextOf$`; `ChangeText` deferred) | `StaticAbilityContinuous.java:579`, `card/Card.java:5389` | `continuous.go` (`applyContinuousText`) | `TestGainTextOfTakesTopCreaturesFullText` | `covered` |
| Layer 4 — type-changing effects (static and Animate) | `StaticAbilityContinuous.java:370` | `continuous.go:236`, `typemod.go:57`, `animate.go:56` | `TestApplyContinuousTypeAddsTypeToMatchingCreatures`, `TestAnimateEffectLandBecomesCreatureUntilEndOfTurn` | `covered` |
| 305.7 — setting a land's subtype removes its printed abilities (Blood Moon) | `card/Card.java:3416`, `card/CardState.java:555-610` | `card.go` (`printedTraitsRemoved`, `traitDef`) | `TestBloodMoonRemovesAPrintedManaAbility`, `TestBloodMoonRemovesPrintedKeywordsButNotGrantedOnes` | `covered` |
| Layer 5 — color-changing effects | `StaticAbilityContinuous.java:451` | `continuous.go:358`, `colormod.go:51` | `TestApplyContinuousColorSetColorOverwrites`, `TestAnimateColorsAndRemember` | `covered` |
| Layer 6 — keyword grants | `StaticAbilityContinuous.java:326`, `:771` | `continuous.go:473` | `TestApplyContinuousKeywordGrantsKeywordToMatchingCreatures`, `TestApplyContinuousKeywordGrantedFlyingAffectsCanBlock` | `covered` |
| Layer 6 — keyword removal by a resolved effect | `ability/effects/PumpEffect.java:270` | `debuffeffect.go:51`, `animate.go:71` | `TestDebuffEffectRemovesKeywordUntilEndOfTurn` | `covered` |
| Layer 6 — static `RemoveKeyword$`/`RemoveAllAbilities$`/`RemoveNonManaAbilities$`, trait grants (ADR-0023) | `StaticAbilityContinuous.java:167`, `:327-331` | `continuous.go` (`applyOneContinuousTraits`, `removeTraits`) | `TestHumilityRemovesEveryKindOfPrintedAbility`, `TestRemoveNonManaAbilitiesKeepsManaAbilities` | `covered` |
| Layer 7a — characteristic-defining P/T | `StaticAbilityContinuous.java:143`, `:679` | `continuous.go:212` | `TestApplyContinuousCharacteristicDefiningSetsFromCountValid`, `TestPowerToughnessCharacteristicResolvesUnresolvableBase` | `covered` |
| Layer 7b — P/T-setting effects | `StaticAbilityContinuous.java:143`, `:679` | `continuous.go:141`, `card.go:493` | `TestPowerToughnessSetPTReplaces`, `TestApplyContinuousPTSetPowerToughnessPartial` | `covered` |
| Layer 7c — P/T-modifying effects (statics, pumps) | `StaticAbilityContinuous.java:155`, `:694` | `continuous.go:141`, `:667` | `TestApplyContinuousPTAppliesAnthemToMatchingCreatures`, `TestPowerToughnessModifyPTStacks` | `covered` |
| Layer 7c — +1/+1 and -1/-1 counters (613.4c) | `card/Card.java:4438`, `:4453` | `card.go:433-447` | `counters-minus-one-reduces-toughness-to-zero-destroyed`, `TestPowerToughnessCounters` | `covered` |
| 613.4 — sublayer order 7a, 7b, 7c regardless of timestamp | `StaticAbilityLayer.java:45-46` | `card.go:493` | `TestPowerToughnessSetPTAppliesBeforeModifyPT` | `covered` |
| Forge Layer 8 — rules effects (hand size, land plays); no CR layer | `StaticAbilityContinuous.java:462`, `:876` | `continuous.go:736` | `TestApplyContinuousRulesSetsFixedHandSize`, `TestApplyContinuousRulesAdjustsLandPlays` | `covered` |
| 613.3 — CDAs first within layers 2-6, then timestamp | `GameAction.java:82-83`, `:1117` | `continuous.go` (`continuousStatics` sorts CDA lines first); type and color `CharacteristicDefining$` lines are still skipped | missing | `missing` |
| 613.6 — an effect applies to the same objects in every layer | `GameAction.java:1138-1151` | `continuouslayers.go` (`staticAffected`, `staticLive`) | `TestAnEffectAppliesToTheSameObjectsInEveryLayer`, `TestAnEffectKeepsApplyingAfterItsSourceLosesItsAbilities` | `covered` |
| 613.7 — within-layer timestamp order, Layer 1 (later copy wins) | `card/Card.java:4255` | `cloneeffect.go:70` | `TestCloneCopiesStackByTimestamp` | `covered` |
| 613.7 — within-layer timestamp order, layers 4, 5, 7b (non-commuting effects) | `GameAction.java:1117` | `typemod.go`, `colormod.go`, `card.go`, `continuous.go` | `TestLayerStaticsEvaluateInTimestampOrder`, `TestEquipmentIsRestampedWhenItBecomesAttached` | `covered` |
| 613.7a — static effect takes its source's timestamp | `GameAction.java:82-83` | `continuous.go` (`continuousStatics`) | `TestLayerStaticsEvaluateInTimestampOrder` | `covered` |
| 613.7b — resolved effect timestamped when created | `ability/effects/PumpEffect.java:270` | `pumpeffect.go:148` | missing | `impl-no-fixture` |
| 613.7c — object timestamped on entering a zone | `GameAction.java:2068` (world timestamp read) | `game.go:597-603` | `world-rule-keeps-newest` | `covered` |
| 613.7d — Aura/Equipment/Fortification restamped on attach (Java cites 613.7e) | `card/Card.java:3938` | `game.go` (`Attach`) | `TestEquipmentIsRestampedWhenItBecomesAttached` | `covered` |
| 613.7e — permanent restamped when turned face up or down (Java cites 613.7f) | `card/Card.java:840`, `:891` | `setstateeffect.go` (face up); a face-down permanent enters as a new object | missing | `impl-no-fixture` |
| 613.7f — double-faced permanent restamped on transform | `card/Card.java:693` | `setstateeffect.go:144-145` | missing | `impl-no-fixture` |
| 613.7 — simultaneous timestamps ordered by active player | not located | — (`game.go:599` stamps sequentially, no choice) | missing | `missing` |
| 613.8 — dependency reorders within layers 2, 4 and Layer 6's keyword half (affected-set test); cycles by timestamp | `GameAction.java:1273`, `:1312-1341`, `:1362-1369` | `dependency.go` (layers 2, 4, 6 keywords; affected-set test) | `TestDependentLayerFourEffectAppliesAfterItsDependency`, `TestDependencyLoopIsIgnoredButNotTheEffectsDependingOnIt` | `covered` |
| 613.8 — dependency in the other layers Java checks: 1, 3, 7b | `StaticAbilityLayer.java:47-48` | — (walked in effectOrder only) | missing | `missing` |
| 613.8a — existence half: a dependency that removes the other static (Blood Moon with Urborg) | `GameAction.java:1297`, `:1327` | `dependency.go` (`staticExists`), `card.go` (`printedTraitsRemoved`) | `TestBloodMoonRemovesUrborgsAbilityWhateverTheTimestamps` | `covered` |

613.7 sub-letters (c-f) are from memory; the behaviour in each row is what the Java site does, whatever its letter.

---

## Step transitions and turn-based actions

Go entry: `beginPhase` (`turn.go:224`) runs mana-pool emptying, the step's action, `checkPhaseTriggers`
(`trigger.go:1781`), then `CheckStateBasedActions`. Java entry: `PhaseHandler.onPhaseBegin`
(`phase/PhaseHandler.java:240`), `onPhaseEnd` (`:453`), `mainLoopStep` (`:1039`). Design notes:
[turn-stack-combat](port-log/game-state/turn-stack-combat.md).

**Two turn walks shape this table (ADR-0026).** `AdvancePhase` (fixture verb `advance`) is bookkeeping: no combat
turn-based actions, no priority. The driver, `Game.Step`/`Game.Run` (`driver.go:32`, `:68`, fixture verbs `step`/`run`),
runs combat's turn-based actions inside the step (`beginStep`, `turn.go:234`), applies the skip rules and opens a
priority round per step grant. Rows about per-step priority cite the driver.

| Rule | Java site | Go site | Fixture(s) | Status |
| --- | --- | --- | --- | --- |
| 500.1 — step order Untap to Cleanup, turn wraps and rotates | `PhaseHandler.java:149` | `turn.go:63` | `untap-and-draw`, `TestAdvancePhaseWalksStepsInOrder`, `TestAdvancePhaseWrapsToNextTurnAndRotatesPlayer` | `covered` |
| 500.4 — mana pools empty at every step end | `PhaseHandler.java:459-469` | `turn.go:297` | `TestEmptyManaPoolsClearsEveryPlayer` | `covered` |
| 500.7/500.8 — extra turns, extra and skipped phases | `PhaseHandler.java:861`, `:908` | `turn.go:75-78`, `:143`, `:539` | `TestAddTurnEffectTakesExtraTurnThenResumes`, `TestAddPhaseEffectExtraCombatThenMain`, `TestSkipPhaseEffectSkipsOpponentsDraw` | `covered` |
| 704.3 — SBAs checked on every step entry | `PhaseHandler.java:1162` | `turn.go:275` | `TestAdvancePhaseChecksStateBasedActionsEveryStep` | `covered` |
| 117.3a — priority round in every step that grants it, driven by the turn loop | `PhaseHandler.java:1039` | `driver.go:32`, `priority.go:137` | `driver-upkeep-trigger-resolves-through-priority`, `driver-main-phase-plays-land-taps-and-casts` | `covered` |
| Combat turn-based actions run on step entry, not by caller | `PhaseHandler.java:305-344` | `turn.go:234` (driven) | `driver-attack-deals-damage-in-combat-damage-step` | `covered` |
| 502.1 — Untap: phasing (ADR-0021) | `phase/Untap.java:198` | `phasing.go` | `phasing-untap-step-phases-in-and-out` | `covered` |
| 502.2 — Untap: day/night check | `phase/Untap.java:251` | `daytimeeffect.go:59` | `TestDayTimeSetAndUntapRule`, `TestDayTimeNightBecomesDay` | `covered` |
| 502.3 — Untap: active player untaps, "doesn't untap" and exert honoured | `phase/Untap.java:86` | `turn.go:326`, `replacement.go:339` | `untap-and-draw`, `TestUntapSkipsExertedPermanentAndClearsFlag`, `TestUntapBlockedBySelfCantHappenReplacement` | `covered` |
| 502.4 — Untap: no priority | `PhaseHandler.java:251` | `turn.go:241` | `driver-untap-grants-no-priority` | `covered` |
| 503.1a — Upkeep: "at the beginning of upkeep" triggers | `PhaseHandler.java:256-266`, `:436-441` | `trigger.go:1781` | `TestAdvancePhaseFiresPhaseTriggerAtCorrectStep`, `TestInitiativeVenturesAtUpkeep` | `covered` |
| n/a: Upkeep crank counter — Contraptions, Unstable, silver-bordered | `PhaseHandler.java:262-264` | — | — | `n/a` |
| 504.1 — Draw: active player draws | `PhaseHandler.java:268-273` | `turn.go:350` | `untap-and-draw`, `TestDrawMovesTopOfLibraryToHand` | `covered` |
| Draw: first player skips first draw, two-player (code cites CR 103.7a) | `PhaseHandler.java:221-222` | `turn.go:362`, `:124` (driven) | `TestDrawSkipsFirstPlayerFirstTurnTwoPlayer`, `driver-no-attackers-skips-blockers-and-damage-steps` | `covered` |
| 504.2 — Draw: "at the beginning of draw step" triggers | `PhaseHandler.java:436-441` | `trigger.go:1781` | `TestAdvancePhaseFiresPhaseTriggerWhenEnchantedControllerMatches` | `covered` |
| Main 1: lore counter on each Saga (Java cites CR 703.4f) | `PhaseHandler.java:282-289` | `saga.go` (`sagaLoreCounters`) | `TestSagaRunsItsChaptersAndIsSacrificed` | `covered` |
| Main 1: roll to visit Attractions (Java cites CR 703.4g; Unfinity, Legacy/Vintage-legal) | `PhaseHandler.java:291-294` | — | missing | `missing` |
| n/a: Main 1 Archenemy scheme set in motion — variant | `PhaseHandler.java:278-280` | — | — | `n/a` |
| Main 1: "at the beginning of precombat main phase" triggers | `PhaseHandler.java:436-441` | `trigger.go:1781` | missing — `TestAdvancePhaseSkipsMainSecondTriggerOnMain1` is negative only | `impl-no-fixture` |
| 307.1 — main phase, empty stack: sorcery-speed actions allowed | `player/Player.java:2512` | `priority.go:81` | `land-play-limit-one-per-turn`, `TestPassPriorityErrorsOnIllegalTiming` | `covered` |
| 507.1 — n/a: choose defending player — only variants where opponents are not all defending | `PhaseHandler.java:298-303` | — | — | `n/a` |
| 507 — Beginning of combat: triggers | `PhaseHandler.java:436-441` | `trigger.go:1781` | missing | `impl-no-fixture` |
| 508.1 — Declare attackers: declare, tap, vigilance, summoning sickness | `PhaseHandler.java:305-312`, `:534` | `attack.go:47` | `combat-attacker-unblocked`, `combat-vigilance-attacker-stays-untapped`, `TestDeclareCombatAttackersSummoningSickIsIneligibleWithoutHaste` | `covered` |
| 508.1c — Declare attackers: optional attack costs (exert) | `PhaseHandler.java:534` | `attack.go:100` | `cr-508-1c-exert-attacker-runs-payoff` | `covered` |
| 508.1m/508.2 — Declare attackers: attack triggers | `PhaseHandler.java:534` | `attack.go:47` | `copy-effect-attacker-fights-as-copied-creature`, `TestAttacksFiresFirstCombatTrigger` | `covered` |
| 508.8 — no attackers: skip declare blockers and damage steps | `PhaseHandler.java:228-233`, `:311` | `turn.go:124` | `driver-no-attackers-skips-blockers-and-damage-steps` | `covered` |
| 509.1 — Declare blockers: declare, legality (menace, can't-block) | `PhaseHandler.java:314-319`, `:656` | `block.go:65` | `combat-single-block-kills-attacker`, `combat-split-across-two-defending-players`, `ring-bearer-cant-be-blocked-by-greater-power` | `covered` |
| 509 — Declare blockers: block triggers | `PhaseHandler.java:656` | `block.go:65` | `TestDeclareCombatBlockersFiresBlocksTrigger`, `TestDeclareCombatBlockersFiresAttackerBlockedTrigger` | `covered` |
| 510.4 — First-strike damage step | `PhaseHandler.java:321-332` | `combatdamage.go:16` | `combat-first-strike-prevents-return-damage`, `combat-double-strike-deals-damage-twice`, `combat-mixed-first-strike-gang-block` | `covered` |
| 510.4 — no first or double striker: no damage, no priority | `PhaseHandler.java:326-328` | `turn.go:263`, `combatdamage.go:123` | `driver-no-first-strike-step-grants-no-priority` | `covered` |
| 510.1/510.2 — Combat damage: assign and deal simultaneously | `PhaseHandler.java:334-344` | `combatdamage.go:28` | `combat-gang-block-damage-assignment`, `combat-trample-excess-to-player`, `combat-attack-and-damage-a-battle` | `covered` |
| 510 — Combat damage: damage triggers | `PhaseHandler.java:334-344` | `combatdamage.go:28` | `monarch-draws-at-end-step-and-passes-by-combat-damage`, `TestInitiativeTakenByCombatDamage` | `covered` |
| 510 — Combat damage: no damage dealt, no priority | `PhaseHandler.java:339-341` | `turn.go:263`, `combatdamage.go:123` | missing | `impl-no-fixture` |
| 511.1 — End of combat: "at end of combat" triggers | `PhaseHandler.java:346-354` | `trigger.go:1781` | `TestFirstCombatOnlyInFirstOfTwoCombats`, `TestEffectCardUntilEndOfCombat` | `covered` |
| 511.3 — creatures removed from combat as the step ends | `PhaseHandler.java:492-501` | `turn.go` (`advanceStep`) | `TestEndOfCombatTriggerSeesTheCombat`, `TestAdvancePhaseIntoCombatEndClearsCombat` | `covered` |
| 505 — Main 2: "at the beginning of your second main phase" triggers | `PhaseHandler.java:356-359` | `trigger.go:1781` | `TestAdvancePhaseFiresMainSecondTriggerOnMain2` | `covered` |
| 513.1 — End step: "at the beginning of the end step" and delayed triggers | `PhaseHandler.java:361-367` | `trigger.go:1781` | `monarch-draws-at-end-step-and-passes-by-combat-damage`, `TestDelayedTriggerFiresOnceAtEndStep` | `covered` |
| 514.1 — Cleanup: active player discards to hand size | `PhaseHandler.java:370-397` | `turn.go:484-491` | `cleanup-discards-to-hand-size`, `TestCleanupDiscardOnlyAppliesToTheActivePlayer` | `covered` |
| 514.2 — Cleanup: damage removed, "until end of turn" effects end | `PhaseHandler.java:399-408` | `turn.go:493-523` | `cleanup-resets-lands-played-for-next-turn`, `TestCleanupClearsDamageForEveryPlayer`, `TestCloneUntilEndOfTurnEndsAtCleanup` | `covered` |
| 514.3/514.3a — no priority unless SBA or trigger, then another cleanup step | `PhaseHandler.java:421-428`, `:446-450`, `:507-509` | `turn.go:272`, `:279`, `action.go:123`, `driver.go:32` | `TestStepRepeatsCleanupWhenItGrantsPriority`, `TestStepRepeatsCleanupWhenAStateBasedActionIsPerformed`, `TestStepEndsCleanupWithoutPriorityWhenNothingHappens` | `covered` |

---

## Backlog

Every `missing` and `impl-no-fixture` row, grouped by table. Order within a group: constructed relevance first; at equal
relevance, rules that give a wrong answer today before rules that never fire. Closing a row = a fixture under
`crucible/testdata/scenarios/` (TEST-5) plus, for `missing`, the implementation.

### Backlog: state-based actions

| Row | Why this position |
| --- | --- |
| 704.3 — repeat until no SBA applies | Wrong answer: anthem source dying leaves a 0-toughness creature alive a whole pass |
| 704.5q — "counters can't be removed" guard | Rare static |
| 704.5e — card copy outside stack/battlefield | Java site not located; rare |
| 704.5j — Corner Case 1 | One card (Spy Kit); needs corpus name index |

### Backlog: layers and timestamps

| Row | Why this position |
| --- | --- |
| 613.3 — CDAs first within layers 2-6 | Changeling/Devoid-style CDAs on type and color lines skipped today |
| 613.7e — restamp on face up/down | Morph/manifest interacting with other effects |
| 613.7b — resolved effect timestamp | Implemented, unasserted |
| 613.7f — restamp on transform | Implemented, unasserted |
| 613.7 — simultaneous timestamps by active player | Java site not located; rare |

### Backlog: step transitions

| Row | Why this position |
| --- | --- |
| 510 — no combat damage, no priority | Implemented, unasserted |
| Main 1 — precombat main triggers | Implemented, unasserted |
| 507 — beginning of combat triggers | Implemented, unasserted |
| Main 1 — Attractions | Legacy/Vintage only |

---

## Open questions

- **Commander scope.** No ADR names Commander in or out. If a Commander gauntlet comes into scope, 704.6c and 704.6d
  flip from `n/a` to `missing`.

## Related

- [test-port-matrix](test-port-matrix.md) — Java TestNG disposition; its M5 backlog overlaps this one on SBAs
- [port-log/game-state.md](port-log/game-state.md) — `Not ported yet`
- [ADR-0019](../adr/0019-interactive-priority.md), [ADR-0026](../adr/0026-turn-driver.md),
  [ADR-0021](../adr/0021-phasing-battlefield-view.md),
  [ADR-0023](../adr/0023-granted-abilities-over-compiled-definitions.md),
  [ADR-0025](../adr/0025-continuous-effect-evaluation-order.md)
