# Forge AI performance engineering plan

**Repository:** [`Card-Forge/forge`](https://github.com/Card-Forge/forge)  
**Revision inspected:** [`e3c5554c79e6e6f225697b821d36db1863eb467d`](https://github.com/Card-Forge/forge/commit/e3c5554c79e6e6f225697b821d36db1863eb467d) (2026-08-12)  
**Scope:** behavior-equivalent reductions in AI decision and turn-processing time; no reduction in legal actions, targets, search depth, simulations, rules checks, or heuristic strength.

## Evidence and limits

The labels in this report are deliberate:

- **Observed** means directly verified in the pinned source.
- **Measured** means supported by an existing Forge profile or repeatable benchmark. The measurement may be historical and is identified as such.
- **Inferred** means the cost follows strongly from control flow, input-dependent loops, or allocation structure, but has not been timed in this revision.
- **Proposed** means a design, not an existing Forge API or thread-safety guarantee.
- **Uncertain** identifies a point that must be resolved by profiling or parity tests.

I could inspect the complete source and Git history, but could not build or profile this revision in the supplied environment: Java 17 is present, while Maven is absent, the repository has no Maven wrapper, and no compiled dependency tree is available. Consequently, this report does **not** call any newly identified path a measured bottleneck. Historical measurements are useful evidence but are not substitutes for a fresh baseline.

Two existing measurements are particularly relevant:

1. Merged PR [#11440](https://github.com/Card-Forge/forge/pull/11440) measured `AiDeckStatistics.fromPlayer` at 0.1513 ms per call before its cache and 0.0030 ms after it (2,000-call average). That fix is already in the inspected source, so it is evidence that stable, decision-local derived data can pay off—not a remaining 50× opportunity.
2. Open PR [#11366](https://github.com/Card-Forge/forge/pull/11366) reports a fixed-seed AI-vs-AI game-time reduction from 35,877 ms to 16,773 ms (2.15×) by caching per-`CardState` trait lists, with the same reported turns, winners, and scores. It also reports repeat variation under 1%. This is compelling measured evidence for that proposal, but the change is unmerged and its invalidation/correctness review is not complete; the result must be reproduced on the pinned revision and a larger corpus.

Historical PR [#11160](https://github.com/Card-Forge/forge/pull/11160) includes profiler stacks in which `Card.canTap`/`canUntap` reached `ReplacementHandler.getReplacementList` and full-game traversal thousands of times through mana grouping and creature evaluation. Several targeted fixes from that work are already merged. The remaining broad discovery path is still observable, but its current share requires a new profile.

## Implementation status

This document is the plan this repository is executing, so it now also records what each phase
actually did. Sections below keep their original analysis of the pinned revision; where the code has
since moved, an **Implemented** note says how. Phases 2 to 5 are untouched.

| Phase | State | Where |
|---|---|---|
| 0 — measurement foundation | Done | [AI-Performance-Measurement.md](AI-Performance-Measurement.md) |
| 1 — low-risk work elimination | Done, with two design changes and two deferrals | [AI-Performance-Phase1.md](AI-Performance-Phase1.md) |
| 2 to 5 | Not started | — |

Three findings from phase 1 change what a later phase should assume:

1. **The baseline-invariance premise in §3.1 and §10.1 is false at decision scope.** The shadow
   assertion the plan required was written first and it failed: on a full board with recursive
   simulation, the score evaluated at the top of `chooseSpellAbilityToPlay` no longer matches a fresh
   evaluation by the time the branches run, because deciding whether each candidate can be played and
   paid for touches state the evaluator reads. The plan's fallback — narrow the scope or abandon —
   was taken: reuse is scoped to the branches of one candidate. Any later work that wants to treat an
   evaluator result as stable across a decision has to prove that separately.
2. **A per-controller evaluation worker is not viable** (§3.6). Every simulated game copy builds its
   own players and controllers, and the AI in a copy takes priority while an outer decision is still
   on the stack. One worker per controller deadlocks the nested decision; one per copy parks a thread
   per copy. This also means "one AI decision at a time" is not a safe assumption for §7 concurrency
   work.
3. **Recursive simulation has a pre-existing game-copy inaccuracy.** A fixture with creatures in play
   before combat fails `GameSimulator`'s own copy-score check on the unmodified source: the copy comes
   back with a creature missing on each side and the opponent two life adrift. This is not something
   phase 1 introduced or fixed, but it bounds what a `GameCopier` redesign (§10.9) can be validated
   against, and it deserves its own investigation.

## 1. AI architecture overview

### 1.1 Live priority-to-action flow

The live game loop is serial. [`PhaseHandler.mainGameLoop` / `mainLoopStep`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/phase/PhaseHandler.java#L1032-L1144) performs state-based action and priority work, then calls the priority player's controller at line 1056. For an AI player the path is:

```text
PhaseHandler.mainLoopStep
  -> PlayerControllerAi.chooseSpellAbilityToPlay
     -> AiController.chooseSpellAbilityToPlay
        -> full simulation: SpellAbilityPicker.chooseSpellAbilityToPlay
        -> conventional AI: AiController.getSpellAbilityToPlay
           -> ComputerUtilAbility.getAvailableCards
           -> ComputerUtilAbility.getSpellAbilities
           -> AiController.chooseSpellAbilityToPlayFromList
              -> ComputerUtilAbility.getOriginalAndAltCostAbilities
              -> canPlayAndPayFor / canPlaySa
              -> ComputerUtilCost.canPayCost
              -> SpellAbilityAi.canPlayAI / heuristic-specific target choice
  -> PhaseHandler.playChosenSpellAbility
  -> stack/SBA/trigger processing
```

[`PlayerControllerAi`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/PlayerControllerAi.java#L821-L833) is the game-controller adapter. [`AiController.chooseSpellAbilityToPlay`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/AiController.java#L1357-L1383) clears `AiCache`, resets decision memories, and selects the full-simulation or conventional path. The conventional path builds and orders the candidates in [`getSpellAbilityToPlay` and `chooseSpellAbilityToPlayFromList`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/AiController.java#L1520-L1726).

The latter is not parallel candidate evaluation. It creates a `FutureTask`, starts a fresh `Thread("Game AI Eval")`, and performs the entire candidate loop in that single worker. The caller waits, times out cooperatively, joins, and retains a deprecated `Thread.stop()` fallback. This is a watchdog boundary, not multicore scaling.

> **Implemented (phase 1, §3.6).** The boundary and its semantics are unchanged, but the thread per
> decision is gone: `AiEvaluationExecutor` runs the candidate loop on a pooled daemon worker. The
> `Thread.stop()` fallback stays, because the loop still only honours cancellation between abilities;
> a run that ignores it keeps its thread and never returns to the pool, as before.

### 1.2 Candidate generation, legality, scoring, targeting, mana, and costs

| Concern | Package / class / method | Important caller | Important callees and actual work |
|---|---|---|---|
| Available action sources | `forge.ai.ComputerUtilAbility.getAvailableCards` | `AiController`, `SpellAbilityPicker` | Builds a `CardCollection` from hand, graveyard, each library top, command, exile, and battlefield. |
| Spell/ability collection | `forge.ai.ComputerUtilAbility.getSpellAbilities` | Candidate generation | Builds an `ArrayListMultimap` per card and collects playable spell abilities. |
| Alternate/optional costs | `ComputerUtilAbility.getOriginalAndAltCostAbilities` | Conventional and simulation candidate paths | Copies/expands abilities and asks the controller about optional costs. |
| Conventional ordering | `forge.ai.AiController.chooseSpellAbilityToPlayFromList`; `SpellAbilityComparator` | `AiController.getSpellAbilityToPlay` | Stable list sorts invoke CMC, priority, energy, trigger/static, and creature evaluation repeatedly. |
| Ability-specific heuristic | `forge.ai.SpellAbilityAi.canPlayAI` and subclasses under `forge.ai.ability` | `AiController.canPlaySa` | Legal timing, cost checks, targets, and API-specific heuristics. |
| Cost feasibility | `forge.ai.ComputerUtilCost.canPayCost` | Candidate selection and many ability AIs | Extra costs, wards, casualty, mana feasibility, then `CostPayment.canPayAdditionalCosts`. |
| Mana feasibility | `forge.ai.ComputerUtilMana.canPayManaCost`, `calculateManaCost`, `groupSourcesByManaColor` | Cost feasibility and heuristic probes | Adjusts costs, scans/group mana abilities, checks playability and mana replacement effects. |
| Target candidates | `forge.game.spellability.TargetRestrictions.getAllCandidates` / `getNumCandidates` | Legality, `SpellAbilityAi`, simulations | Allocates a target list from eligible players plus cards in configured zones; `getNumCandidates` materializes the list and takes its size. |
| Simulation targets/modes | `forge.ai.simulation.SpellAbilityChoicesIterator`, `MultiTargetSelector`, `PossibleTargetSelector` | `SpellAbilityPicker.evaluateSa` | Enumerates modes and target selections; candidate legality for later subabilities is regenerated after earlier targets change. |

The source for the first three rows is [`ComputerUtilAbility`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/ComputerUtilAbility.java#L67-L153). Cost and mana paths are visible in [`ComputerUtilCost.canPayCost`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/ComputerUtilCost.java#L529-L635), [`CostPayment.canPayAdditionalCosts`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/cost/CostPayment.java#L95-L105), and [`ComputerUtilMana`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/ComputerUtilMana.java#L1206-L1595). Target allocation is in [`TargetRestrictions`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/spellability/TargetRestrictions.java#L554-L592).

### 1.3 Full-simulation flow and state copying

When simulation AI is enabled, [`SpellAbilityPicker`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/simulation/SpellAbilityPicker.java#L57-L181) generates the same basic candidate family, calculates the original game score, and serially evaluates candidates. For each candidate, [`evaluateSa`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/simulation/SpellAbilityPicker.java#L341-L370) does the following for every target/mode/card-choice selection:

1. Save the process-global `MyRandom` generator and consume a branch seed.
2. Replace that global generator with `new Random(seed)`.
3. Construct `GameSimulator`, which constructs `GameCopier` and makes a complete game copy.
4. Map and execute the ability in the copy, resolve stack/SBAs/triggers, score the result, and recursively pick subsequent plays up to `SimulationController`'s default depth of three.
5. Restore the original global generator.

[`GameSimulator`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/simulation/GameSimulator.java#L44-L87) also recomputes the unchanged original-state score in every constructor. [`GameStateEvaluator.getScoreForGameState`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/simulation/GameStateEvaluator.java#L39-L118) may create yet another `GameCopier` to simulate imminent combat before scoring.

[`GameCopier.makeCopy`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/simulation/GameCopier.java#L68-L185) creates a new match/game, copies players, mana, phases, all relevant zones, remembered objects, command/effect cards, combat, and ability relationships, then runs state effects and resets active triggers. [`createCardCopy`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/simulation/GameCopier.java#L298-L347) explicitly says reparsing a non-token card from its `PaperCard` is “very expensive” and accounts for the vast majority of `GameCopier` time. An alternate direct-copy path is present but marked inaccurate. It must not be enabled as a performance shortcut.

The copier's fixed zone list includes `Library`, and `copyGameState` iterates `origGame.getCardsIn(zone)` for every listed zone. Thus a Commander simulation copies all players' libraries, not only visible or immediately relevant cards. That is observed behavior and helps explain why copy cost can grow with player/deck count; omitting library cards is not behavior-equivalent because hidden-zone order and later effects are part of the simulated state.

The experimental `GameSnapshot` path is disabled by default, tests force it off, and the implementation contains unresolved correctness TODOs. It is not an established behavior-equivalent replacement.

`SimulationController` already has a narrow effect cache. For a one-card target, it maps the copied host/target back through simulator stacks, keys by original host identity, ability string, original target identity, and current evaluated target-card score, and reuses only a negative score delta when the target's score is unchanged. That narrow validity test is evidence that Forge's authors already avoid broader state-result reuse without a stronger key. Preserve it and measure its hit rate; it is not a general transposition table.

### 1.4 Combat

Attack declaration follows `PhaseHandler.declareAttackersTurnBasedAction -> PlayerControllerAi.declareAttackers -> AiController.declareAttackers -> AiAttackController.declareAttackers`. Blocking follows the corresponding controller calls into [`AiBlockController.assignBlockersForCombat`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/AiBlockController.java#L1005-L1155).

`AiAttackController` builds predicted combats, defender scores, attack requirements, and possible attack sets. Every `Combat` constructs [`AttackConstraints`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/combat/AttackConstraints.java#L29-L101). Its constructor evaluates restrictions/requirements per possible attacker and currently creates an `A-1` “other attackers” list for every attacker before calling `StaticAbilityMustAttack.getAttackRequirements`. Legal-attacker collection can recursively branch when attack limits or conditional requirements exist.

Blocking is multi-pass. [`makeGangBlocks`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/AiBlockController.java#L368-L525) explores single blockers, pairs, and triples for each attacker, interleaving creature/combat/static queries. Its triple search is `O(A × B²)` in available blockers, excluding the cost of those queries. This aligns with user reports of severe token-combat stalls ([#6985](https://github.com/Card-Forge/forge/issues/6985), [#9000](https://github.com/Card-Forge/forge/issues/9000)), but those reports do not prove which method dominates.

There is already concurrency in [`AiAttackController.declareAttackers`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-ai/src/main/java/forge/ai/AiAttackController.java#L876-L959): one `CompletableFuture.supplyAsync` per attacker on the common pool. Tasks read shared combat/requirement objects and can call `combat.addAttacker`; only that individual method is synchronized. A timeout completes the aggregate future but does not cancel unfinished tasks. This code is not proof that attack evaluation is safely parallel; it is a correctness and determinism risk that should be tested or removed before broader concurrency work.

> **Implemented (phase 1, §3.7).** Removed. The loop runs serially in attacker order, so the
> declaration is reproducible and no task can outlive the method. A failing attacker is still skipped
> rather than failing the whole declaration, which is what the discarded `exceptionally` handler did.
> There is now **no** concurrency in AI attack evaluation, and §7 should not treat any as precedent.

### 1.5 Rules-derived state: triggers, replacements, statics, zones

| Concern | Package / class / method | Caller/callee structure |
|---|---|---|
| Triggers | `forge.game.trigger.TriggerHandler.resetActiveTriggers`, `runWaitingTrigger(s)` | Resets rescan cards/game triggers; waiting-trigger execution scans active triggers for static and non-static processing and handles delayed triggers. Called from phase, state-effect, zone-change, and simulation resolution paths. |
| Replacements | `forge.game.replacement.ReplacementHandler.getReplacementList`, `run` | For each replacement layer, builds lists by scanning cards and their replacement effects, then applies `hasRun`, mode, zone, requirement, and `canReplace` filters. Special ordering/LKI rules exist. |
| Static abilities | `forge.game.GameAction.checkStaticAbilities`, `findStaticAbilityToApply` | Clears prior static effects, scans card statics, sorts them, repeatedly filters per layer, and performs dependency discovery by temporarily applying/removing effects and comparing affected/result sets. |
| State-based actions | `GameAction.checkStateEffects` | Up to nine iterations; invokes static checks, player/card scans, waiting triggers, trigger reset, and a final static pass. Simulations call it too. |
| Zone aggregation | `forge.game.Game.getCardsIn(ZoneType)` | Synchronized. Non-stack aggregate queries delegate across players and generally create aggregate collections. `Game.forEachCardInGame` exists specifically to traverse without a temporary list. |

Source: [`TriggerHandler`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/trigger/TriggerHandler.java#L185-L330), [`ReplacementHandler`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/replacement/ReplacementHandler.java#L69-L209), [`GameAction.checkStaticAbilities`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/GameAction.java#L1070-L1360), [`GameAction.checkStateEffects`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/GameAction.java#L1397-L1638), and [`Game.getCardsIn` / `forEachCardInGame`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-game/src/main/java/forge/game/Game.java#L606-L765).

### 1.6 Random behavior and AI-vs-AI execution

[`MyRandom`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-core/src/main/java/forge/util/MyRandom.java#L27-L66) holds one mutable static `Random`. Simulation temporarily replaces it. Other static mutable state includes simulation debug output, lazily initialized ability-AI singletons, some AI helper caches/fields, and loaded AI profiles. Live `Game`, `Player`, `Card`, `SpellAbility`, `Combat`, trigger/replacement handlers, zones, and event/listener systems are mutable and not documented as thread-safe. Even scoring is not universally read-only: mana-base evaluation sets an ability's activating player.

[`SimulateMatch`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-gui-desktop/src/main/java/forge/view/SimulateMatch.java#L80-L220) supports a fixed seed but runs matches sequentially in the same thread. Forge's test infrastructure has a separate-JVM multi-process executor; that is the safe current way to scale independent games because it isolates global RNG and mutable static state.

### 1.7 Component map with callers and callees

This table consolidates the execution map so that an implementation owner can enter at a method rather than only a class name.

| Component | Package / file / class / method | Important callers | Important callees |
|---|---|---|---|
| Turn/priority control | `forge.game.phase/PhaseHandler.java` — `PhaseHandler.mainGameLoop`, `mainLoopStep` | Match/game start and phase advancement | SBA/static/trigger work, `PlayerController.chooseSpellAbilityToPlay`, chosen-action play, stack resolution |
| AI controller adapter | `forge.ai/PlayerControllerAi.java` — `chooseSpellAbilityToPlay`, `declareAttackers`, `declareBlockers` | `PhaseHandler` and combat turn-based actions | Matching `AiController` methods |
| AI decision root | `forge.ai/AiController.java` — `chooseSpellAbilityToPlay`, `getSpellAbilityToPlay`, `chooseSpellAbilityToPlayFromList` | `PlayerControllerAi` | Cache reset, candidate generation, sorts, alt costs, play/cost checks, `SpellAbilityAi`, or `SpellAbilityPicker` |
| Candidate actions | `forge.ai/ComputerUtilAbility.java` — `getAvailableCards`, `getSpellAbilities`, `getOriginalAndAltCostAbilities` | Conventional AI, simulation picker, several ability AIs | Zone/card ability access, multimap/list construction, optional-cost controller choices |
| Conventional action evaluation | `forge.ai/SpellAbilityAi.java` plus `forge.ai.ability.*` — `canPlayAI`/API-specific methods | `AiController.canPlaySa` | Timing/property queries, target selection, mana/cost, threat/card/combat utilities |
| Candidate order/priority | `forge.ai/ComputerUtilAbility.java` — spell ability comparator and `getSpellAbilityPriority` | `AiController.chooseSpellAbilityToPlayFromList` | CMC/energy, trigger/static scans, creature presence/evaluation |
| Card filtering/property queries | `forge.game.card/CardLists.java` — `filter`, targetable/type/keyword helpers; `CardPredicates` | AI abilities, combat, mana, rules utilities | Predicate evaluation over card collections; usually returns a new `CardCollection` |
| Target legality | `forge.game.spellability/TargetRestrictions.java` — `getAllCandidates`, `getNumCandidates` | AI legality, ability AIs, triggers/effects, simulation selectors | Player/card zone sources, validity/targetability predicates |
| Target/mode enumeration | `forge.ai.simulation/SpellAbilityChoicesIterator.java`, `MultiTargetSelector.java`, `PossibleTargetSelector.java` | `SpellAbilityPicker.evaluateSa` and simulated controller choices | Mode combinations, target candidate regeneration, target mutation on the selected SA |
| Mana and cost | `forge.ai/ComputerUtilCost.java` — `canPayCost`; `ComputerUtilMana.java` — `canPayManaCost`, `calculateManaCost`, `groupSourcesByManaColor`; `forge.game.cost/CostPayment.java` | Candidate/heuristic legality and payment probes | Cost adjustment, mana abilities, replacement discovery, additional-cost feasibility |
| General combat/threat facts | `forge.ai/ComputerUtilCombat.java`, `ComputerUtilCard.java`, `CreatureEvaluator.java` | Ability AIs, attack/block controllers, state evaluator | Card filters/properties, static/replacement queries, predicted combat/scoring |
| Attacks | `forge.ai/AiAttackController.java` — constructor, `declareAttackers`; `forge.game.combat/AttackConstraints.java` | `AiController.declareAttackers`, predicted combats, `Combat` construction/validation | Defender/attacker scoring, restrictions/requirements, legal attacker recursion, live combat mutation |
| Blocks | `forge.ai/AiBlockController.java` — `assignBlockersForCombat`, `makeGangBlocks` and strategy passes | `AiController.declareBlockersFor`, some ability AIs | Possible blockers, combat legality, creature evaluation, block mutations |
| Triggers | `forge.game.trigger/TriggerHandler.java` — `resetActiveTriggers`, `runWaitingTrigger(s)` | Phase, zones, SBA/effects, copier/simulation resolution | Card/game trigger discovery, active/delayed trigger scans, ability creation/stacking |
| Replacements | `forge.game.replacement/ReplacementHandler.java` — `getReplacementList`, `run` | Rules resolution, mana, tap/untap and property evaluation | Full-card effect discovery, layer/mode/zone/requirement checks, player effect choice |
| Static abilities/SBAs | `forge.game/GameAction.java` — `checkStaticAbilities`, `findStaticAbilityToApply`, `checkStateEffects` | Phase/rules resolution, effects, copier, simulator | Full-game trait discovery, layer ordering/dependencies, state scans, triggers/events |
| Zone queries | `forge.game/Game.java` — `getCardsIn`, `forEachCardInGame`; player/zone classes | Candidate, trigger/replacement/static, copying, broad AI utilities | Cross-player aggregation or ordered no-list visitor traversal |
| State value | `forge.ai.simulation/GameStateEvaluator.java` — `getScoreForGameState`, `simulateUpcomingCombatThisTurn` | Picker root, simulator result scoring, assertions/tests | Player/hand/mana/battlefield evaluation and optional copied combat lookahead |
| Hypothetical search | `forge.ai.simulation/SpellAbilityPicker.java`, `GameSimulator.java`, `SimulationController.java` | Full-simulation `AiController`, hybrid safety checker | Candidates, choices, copies, ability mapping/resolution, scoring, recursive next play |
| Game copying | `forge.ai.simulation/GameCopier.java` — `makeCopy`, `copyGameState`, `createCardCopy` | Simulator and state-evaluator combat lookahead | New game/player/card/zone graph, card parsing, object mapping, combat, SBA/static/trigger rebuild |
| Random behavior | `forge.util/MyRandom.java`; `SpellAbilityPicker.evaluateSa` | Game and AI random decisions; simulation branch setup | Process-global generator get/set, seeded `Random` construction |
| AI-vs-AI | `forge.view/SimulateMatch.java` — `simulate`, `simulateSingleMatch` | Desktop simulation command | Fixed-seed setup, match/game start, sequential result/log processing |

## 2. Highest-value suspected bottlenecks

No item below is newly “confirmed by profiling” on this revision. Where historical profiling exists, the confidence says so explicitly.

| Rank | Field | Information |
|---:|---|---|
| 1 | File | `forge-game/.../card/CardState.java`; open PR #11366 |
| | Class / method | `CardState.getTriggers`, `getStaticAbilities`, and both variants of `getReplacementEffects` |
| | Called from | Candidate generation, static/replacement/trigger discovery, creature/mana/combat evaluation, copying, property checks |
| | Main cost | Rebuilding many small `FCollection`s from base/changed traits; allocation, traversal, GC |
| | Frequency | Reported by PR #11366 as tens of millions of calls in the tested game; source shows these accessors throughout inner paths |
| | Evidence | Historical fixed-seed benchmark: 35,877 ms -> 16,773 ms game time (2.15×), same reported turn counts/winners/match scores; no action-level trace was reported; current source still rebuilds these views |
| | Likely impact | **Potentially very high** across general AI and rules processing |
| | Confidence | **Measured on a nearby revision; correctness/invalidation requires review and reproduction** |
| 2 | File | `forge-ai/.../simulation/GameCopier.java`, `GameSimulator.java`, `GameStateEvaluator.java`, `SpellAbilityPicker.java` |
| | Class / method | `GameCopier.makeCopy/createCardCopy`; `GameSimulator` constructor; `GameStateEvaluator.getScoreForGameState`; `SpellAbilityPicker.evaluateSa` |
| | Called from | Every full-simulation target/mode branch; hybrid `OnePlaySafetyChecker`; combat lookahead |
| | Main cost | Whole-game/card copying, card reparsing, state reconstruction, SBA/static/trigger rebuild, repeated unchanged baseline scoring, nested combat copies |
| | Frequency | At least once per simulated choice; more with targets, modes, and recursive depth |
| | Evidence | Explicit source comment identifies reparsing as the majority of copier time; call graph proves nested copies and baseline recomputation |
| | Likely impact | **Potentially very high** when full/hybrid simulation is active; low otherwise |
| | Confidence | **Strongly supported by code; needs current allocation/CPU profile** |
| 3 | File | `forge-game/.../replacement/ReplacementHandler.java`; `forge-ai/.../ComputerUtilMana.java`, `CreatureEvaluator.java` |
| | Class / method | `getReplacementList`; `groupSourcesByManaColor`; tap/untap/mana property probes |
| | Called from | Cost feasibility and repeated scoring/property checks |
| | Main cost | Full-card and replacement traversal, trait-list creation, filtering, temporary lists |
| | Frequency | Can be thousands per decision in the historical profiles; current path retains broad scans, although zone-specific optimizations are merged |
| | Evidence | Historical profiler evidence in PR #11160; current source scan and special performance-mode comments |
| | Likely impact | **High** in mana-, static-, and permanent-heavy states |
| | Confidence | **Historically confirmed; current magnitude requires profiling** |
| 4 | File | `forge-game/.../GameAction.java` |
| | Class / method | `checkStaticAbilities`, `findStaticAbilityToApply`, `checkStateEffects` |
| | Called from | Live resolution, phase transitions, copies, simulations, token/counter/effect operations |
| | Main cost | Repeated all-card scans, trait lists, sorts/layer lists, dependency comparisons, up to nine SBA iterations |
| | Frequency | Multiple times during trigger/effect-heavy resolution and for every simulation copy |
| | Evidence | Direct nested loops and repeated call sites; dependency work can approach cubic-like repeated pair analysis in a layer |
| | Likely impact | **High** in static-ability-heavy and simulation workloads |
| | Confidence | **Strongly supported by code** |
| 5 | File | `forge-game/.../combat/AttackConstraints.java`; `forge-ai/.../AiAttackController.java`, `AiBlockController.java` |
| | Class / method | `AttackConstraints` constructor/`collectLegalAttackers`; `declareAttackers`; `makeGangBlocks` |
| | Called from | Real and predicted combat construction, attack validation, block assignment |
| | Main cost | Per-attacker repeated static scans and `A-1` allocations, recursive legal-set search, pair/triple block search, repeated creature/combat queries |
| | Frequency | Every combat and many predicted combats; grows with attackers/blockers |
| | Evidence | Direct `O(A²)` setup and `O(A × B²)` gang-block structure; token-stall issues corroborate workload, not method attribution |
| | Likely impact | **High**, potentially very high in token combat |
| | Confidence | **Strongly supported by code; profile required to rank subpaths** |
| 6 | File | `forge-ai/.../simulation/SpellAbilityChoicesIterator.java`, `MultiTargetSelector.java`, `PossibleTargetSelector.java` |
| | Class / method | `chooseModes`, `chooseTargets`, selector reset/increment |
| | Called from | `SpellAbilityPicker.evaluateSa` |
| | Main cost | Mode combinations, cross-subability target enumeration, repeated target candidate construction, and one full simulation copy per uncached selection |
| | Frequency | Product of choices and target-bearing subabilities |
| | Evidence | Explicit combinations iterator, backtracking selectors, and simulator construction inside the choice loop |
| | Likely impact | **High** for complex targeting/modal abilities |
| | Confidence | **Strongly supported by code** |
| 7 | File | `forge-ai/.../ComputerUtilCost.java`, `ComputerUtilMana.java`; `forge-game/.../CostPayment.java` |
| | Class / method | `canPayCost`, `calculateManaCost`, `groupSourcesByManaColor`, `canPayAdditionalCosts` |
| | Called from | Nearly every candidate and many heuristic probes |
| | Main cost | Repeated structural cost adjustment, mana source grouping, playability/replacement checks, collections/maps |
| | Frequency | Multiple times per ability and sometimes per X/payment alternative |
| | Evidence | Direct call graph and source TODO noting duplicate `CostAdjustment.adjust`; mana path appears in historical profiles |
| | Likely impact | **Medium to high** |
| | Confidence | **Strongly supported for redundancy; magnitude requires profiling** |
| 8 | File | `forge-game/.../trigger/TriggerHandler.java` |
| | Class / method | `resetActiveTriggers`, `runWaitingTrigger(s)` |
| | Called from | Phase/state/zone/effect and simulated stack resolution |
| | Main cost | Full-card trigger rediscovery and repeated active-list scans; temporary copies |
| | Frequency | Repeated in trigger-heavy resolution and after copy reconstruction |
| | Evidence | Direct scans and high-frequency call sites |
| | Likely impact | **Medium to high** in trigger-heavy games |
| | Confidence | **Strongly supported by code; needs profile** |
| 9 | File | `forge-game/.../Game.java`; `forge-ai/.../AiController.java`, `ComputerUtilAbility.java`, `AiCache.java` |
| | Class / method | Aggregate zone queries, candidate collections, comparator sorting, cache lookup |
| | Called from | Broad conventional AI paths |
| | Main cost | Short-lived lists/multimaps, repeated filters and aggregate zone materialization, repeated comparator facts; linear bucket scan in a global synchronized cache |
| | Frequency | Every priority decision; repeated within sorts and property queries |
| | Evidence | Observable allocations and comparator call structure |
| | Likely impact | **Medium**, amplified by large boards |
| | Confidence | **Plausible but requires allocation profiling** |

The top-ranked items are not independent. Trait-view rebuilding magnifies replacement, static, trigger, mana, combat, and copy costs. `GameCopier` magnifies all rules-derived-state costs. Optimizing a leaf in isolation can therefore move—rather than remove—the hotspot.

The source does not currently support ranking I/O as an AI-decision bottleneck: logging exists, but the traced hot decisions are dominated structurally by computation, traversal, and allocation. Lock contention is also unmeasured; `Game.getCardsIn` is synchronized and `AiCache` is a synchronized global structure, yet ordinary live AI execution is largely serial. JFR lock events must decide whether these locks matter. Cache locality is plausibly poor during whole-game copying and pointer-rich rule scans, but no hardware-counter measurement supports a separate cache-locality optimization. Single-threading limits full-simulation branch throughput, whereas indiscriminate concurrency is unsafe for the current mutable architecture.

## 3. Low-risk optimizations

These are the best first implementation candidates because they eliminate demonstrably repeated work without changing the explored action/target space.

1. **Pass a verified reusable baseline `Score` into `GameSimulator`.** `SpellAbilityPicker` calculates `origGameScore`, but each `GameSimulator` constructor recalculates it. `Score` is observed to contain only two final integers. Candidate/choice setup does mutate `SpellAbility` metadata, however, so first add a shadow assertion that a freshly evaluated baseline remains equal before every branch. If the corpus proves that invariant, pass the immutable value in a simulation context and retain sampled debug validation. If it fails, scope reuse only between branches with the same evaluator-relevant setup or abandon it. The successful case removes evaluation and possible combat-lookahead copies per branch without changing its value.

   > **Implemented, narrowed.** The shadow assertion was written first and it *failed*: the baseline
   > taken at the top of `chooseSpellAbilityToPlay` does not survive candidate generation, so the
   > `SimulationRoot`/whole-decision form of this is unsound. Reuse is instead scoped to the branches
   > of one candidate — the first branch evaluates exactly where it always did, the rest take that
   > value — via a fifth `GameSimulator` constructor parameter. The assertion ships and runs in every
   > assertion-enabled build.

2. **Add thresholded target counting.** Most AI callers only need `candidateCount >= minTargets` or “any candidate”. Add a new method rather than changing `getNumCandidates` semantics:

   ```java
   // NEW API in TargetRestrictions
   boolean hasAtLeastCandidates(SpellAbility sa, int required, boolean includePlayers)
   ```

   Traverse candidates in the same player/card order, apply the same validity predicate and duplicate behavior, and stop exactly at `required`. Replace only threshold consumers such as `ComputerUtilAbility.isFullyTargetable` and mandatory-target checks. Keep `getAllCandidates` for consumers that need order or members. This is equivalent because the caller observes only the boolean.

   > **Implemented** as `TargetRestrictions.hasAtLeastCandidates(sa, required)`, migrating
   > `ComputerUtilAbility.isFullyTargetable`, `AiController.canPlaySa`, `SpellAbilityAi.doTrigger`
   > and `CharmEffect`. One correction to the sketch: the traversal must **not** return early before
   > `applyTargetTextChanges`, which runs between the player and card passes and mutates `validTgts`
   > that later readers depend on. The player pass therefore always completes.

3. **Cache stable sort facts for one conventional decision.** Before sorting, calculate decision-wide facts (for example `aiHasNoCreatures`) once and lazily store per-`SpellAbility` facts in an `IdentityHashMap`: converted CMC, existing priority value, energy value, creature score, and flags. Keep the existing stable list order and comparator branches exactly. Do not “fix” suspicious comparator asymmetries in the same patch; a behavior parity trace must show the exact same ordered list.

   > **Implemented** as `ComputerUtilAbility.SortFacts`, shared between both ordering passes so a
   > creature's evaluation is paid for once per decision rather than once per comparison. The
   > `saEvaluator` singleton still derives everything on demand, so other callers are untouched. The
   > comparator asymmetries were left exactly as they are.

4. **Use no-allocation traversal where the result is not retained.** Replace aggregate `Game.getCardsIn(zone)` materialization only in consumers that merely iterate and do not require a snapshot, indexing, sorting, or mutation. `Game.forEachCardInGame` is an existing precedent. Introduce an ordered zone visitor/iterator if a subset of zones is required. Preserve player and zone iteration order.

   > **Deferred.** The roadmap in §12 gives this no phase 1 row, and §4.1 makes it conditional on an
   > allocation profile selecting the call sites. Converting an aggregate zone query is per-call-site
   > work — each consumer has to be audited for snapshot, indexing and mutation assumptions — so it
   > belongs with the phase 2 allocation pass, after a profile says which call sites are worth it.

5. **Reuse one structurally adjusted `Cost` within a single feasibility call.** `ComputerUtilCost.canPayCost` reaches both mana and additional-cost checks, which each invoke structural `CostAdjustment.adjust`. Add internal overloads accepting a previously adjusted `Cost`; still run the separate `ManaCostBeingPaid` adjustment. This must be guarded by tests because adjustment temporarily manipulates face-down state and other parameters. Scope reuse to one call stack; do not cache adjusted costs across mutations.

   > **Implemented, guarded.** The mana check reports the adjustment it derived and
   > `CostPayment.canPayAdjustedAdditionalCosts` takes it. The two adjustments are **not**
   > unconditionally equal, which the plan anticipated: while it works out the mana cost,
   > `calculateManaCost` temporarily points the host's `castFrom` at its current zone, and the
   > adjustment reads that for commander tax and for a static's `AffectedZone` requirement. Reuse is
   > declined where the adjustment can see that difference (a commander, a card that has been cast)
   > and where the ability announces `NumTimes`. Every reuse is shadow-checked under assertions.

6. **Replace decision-thread construction with a reusable single-thread executor.** This removes one OS-thread creation per priority decision while retaining the watchdog boundary. Use a named daemon `ThreadPoolExecutor(1,1)` owned by the AI/game lifecycle, a bounded queue, and cancellation. Remove `Thread.stop()` only after all evaluation loops honor interruption/cancellation; otherwise timeouts can leave mutation in progress. This is primarily a robustness/allocation improvement and is likely lower impact than the work-elimination items.

   > **Implemented, as a shared pool rather than a per-controller worker.** A worker owned by one
   > controller deadlocks a nested decision, and one owned by each controller parks a thread per game
   > copy — every copy builds its own controllers, and the AI in a copy takes priority while an outer
   > decision is still on the stack. `AiEvaluationExecutor` therefore pools workers process-wide,
   > sized by actual concurrency. `Thread.stop()` was **not** removed, for the reason given here: the
   > evaluation loop honours cancellation only between abilities.

7. **Fix the existing forced-attacker futures before adding concurrency.** The low-risk correctness choice is to run that small loop serially. A behavior-preserving parallel rewrite is possible only after extracting a pure computation over an immutable snapshot and applying results serially in original attacker order. Current timed-out tasks can outlive the method and mutate shared combat; that should not remain as an assumed-safe foundation.

   > **Implemented.** The loop is serial and in attacker order; the `CompletableFuture` fan-out, its
   > aggregate timeout and the `synchronized (combat)` block are gone. A pure-computation parallel
   > rewrite, if ever wanted, now starts from a clean serial baseline.

## 4. Allocation and GC optimizations

### 4.1 Concrete allocation sources

| Hot path | Observed allocation | Behavior-equivalent reduction | Validation |
|---|---|---|---|
| `CardState` trait accessors | New `FCollection` views assembled on repeated calls | Cache immutable/read-only views per `CardState` generation; invalidate on every base/changed/temporary trait mutation | Mutator audit, randomized trait mutation tests, current PR #11366 review, JFR allocation samples |
| `GameCopier.createCardCopy` | New `Game`, players, every copied card/ability/zone collection; card definition reparsing | First remove redundant copies/scores. A direct structural copy is Phase 5 only and must match every copied field/reference relation | Full copy equivalence tests and deterministic simulation corpus |
| `ComputerUtilAbility` | `CardCollection`, `ArrayListMultimap`, ability lists, alt-cost copies per decision | Decision context owns canonical candidate list once; return a nonescaping iterable where grouping is unnecessary | Exact candidate descriptor/order comparison |
| `TargetRestrictions.getNumCandidates` | Full target list just to obtain size | Threshold visitor described above | Property tests against `getAllCandidates(...).size()` for all target restriction forms |
| `SpellAbilityChoicesIterator` | Mode lists, per-selection target selectors, `ArrayList<Score>` with null placeholders, candidate lists | Reuse iterator-local selector and scratch arrays where reset semantics allow; store uncached selection indices in a primitive/int structure | Exact selection sequence and target state after each increment |
| Aggregate zone queries | Per-call cross-player `CardCollection` | Ordered visitors or lazy concatenated views for iteration-only code | No mutation, snapshot, or synchronization assumptions at converted call sites |
| Comparator sorting | Same derived facts and intermediate filters on each comparison | Per-sort identity facts | Exact before/after comparator output/order |
| `AiCache` | Synchronized multimap; lookup copies a bucket to `ArrayList`, then linearly searches | Replace with a per-decision typed identity-key context; keep narrow special caches separate | Cache on/off decision parity and concurrency tests |
| `AttackConstraints` | `possibleAttackers.stream().filter(...).collect(toList())` for every attacker | Defer construction until a matching static actually needs “other attackers”; preferably batch the static scan | Exact requirement map and legal attack set parity |
| `AiBlockController.makeGangBlocks` | Repeated temporary blocker/card lists across multi-pass strategies | Pass-scoped scratch lists and identity caches; discard/rebuild after combat mutation | Exact attack-to-blocker mapping and evaluation trace |

The most important allocation question is not “streams versus loops”; it is whether repeated construction can be removed. Rewriting a stream is useful only after JFR or async-profiler allocation output shows that call site materially contributes. Likewise, object pooling is not recommended: these objects often carry complex mutable game relationships, pooling raises stale-state risk, and no current GC measurement justifies it.

### 4.2 Trait-list cache correctness requirements

The measured proposal deserves special care. Open PR #11366 caches four results: triggers, static abilities, and replacement effects separately for `rulesHost=true` and `false`. It explicitly does **not** cache `getSpellAbilities`. Its invalidation spans the card and all states where necessary because an Original split state merges LeftSplit/RightSplit traits. The patch invalidates on trait mutators, changed-trait tables, `copyFrom`/`addAbilitiesFrom`, state replacement, keyword and type refresh, and counters that create replacement effects such as shield/stun. This concrete design is a better starting point than a generic memoizer.

Retain that explicit `Card.invalidateTraitCaches()` / `CardState.invalidateTraitCache()` model, and optionally add a monotonically increasing debug generation to detect a getter returning an entry built before the latest invalidation. The cache must not expose a mutable collection that callers can modify and must preserve the exact current concatenation/deduplication order. Because a card state dies with its card/game, the cached views do not need a global LRU and should not retain another game by themselves. Do not extend the patch to spell/mana ability views without a separate dependency audit: those getters synthesize land/permanent/aura abilities and call card-level update logic.

Before merging, instrument generation mismatches in a debug build: occasionally rebuild uncached and compare identity/order/content. Run this through transform, copy, perpetual/change-text, gain/lose ability, face-down, mutate/merge, LKI, and simulation-copy tests. If a complete mutator audit is not possible, do not merge this cache merely on benchmark results.

### 4.3 Scratch collection policy

Reusable scratch buffers are safe only when ownership is explicit. Use one `AiDecisionContext` per decision and pass it down; do not use static buffers or a process-global `ThreadLocal` that can retain whole games. A context can own `IdentityHashMap`s and `ArrayList`s that are cleared and released at decision end. Iterator-local arrays are preferable for simulation choice enumeration. Parallel branches, once supported, require separate branch contexts.

Several string costs are observable but not yet ranked: `GameCopier` reparses card definitions; `SimulationController.CachedEffect` snapshots `SpellAbility.toString()`; target similarity compares type text; and comparator paths perform string parsing/normalization. Card reparsing is explicitly identified as expensive. The others should be optimized only if allocation/CPU profiles select them. Where selected, store a decision-local normalized value once and preserve exact comparison semantics; do not replace rule strings with lossy hashes.

## 5. Algorithmic optimizations

### 5.1 Attack requirement construction

**Current algorithm.** `AttackConstraints` iterates possible attackers. For each attacker it constructs restrictions, obtains per-defender legality, constructs an `A-1` list, and invokes must-attack discovery. `StaticAbilityMustAttack` then scans static-source cards for that attacker. Setup therefore includes `O(A²)` temporary elements plus repeated `O(A × S)` static-source discovery, before legal-set recursion.

**Proposed replacement.** Introduce a new internal `AttackConstraintFacts` builder:

1. Traverse static-source cards once in the same current order and collect only applicable attack-restriction/requirement abilities.
2. Evaluate these abilities across attackers/defenders in the same nested encounter order and populate the same concrete collection/map types in the same insertion sequence. `AttackConstraints` currently uses hash maps; do not silently change their iteration behavior in this patch.
3. Only materialize “other attackers” for a static whose validity expression actually references that set; otherwise evaluate without it.
4. Construct the existing `AttackRestriction`/`AttackRequirement` results from those facts, then leave `collectLegalAttackers` unchanged.

This changes discovery from repeated `A × S` scans to `S + matches × A`, and removes unconditional `A²` list construction. Typical games with no relevant must-attack static benefit most predictably. The principal uncertainty is whether validity evaluation has hidden side effects or relies on per-attacker re-traversal; establish that by tracing each `StaticAbilityMustAttack` helper and comparing full requirement maps before switching.

### 5.2 Block gang evaluation

**Current algorithm.** For each attacker, `makeGangBlocks` tests eligible blockers, then pairs/triples, making the structural upper bound `O(A × B²)` for the pair/triple portion. Repeated creature scoring, destroy/deathtouch/trample facts, and `CombatUtil` calls make each edge expensive. The outer block strategy may clear and rerun passes.

**Exact improvement.** At the start of a pass, compute identity-indexed blocker facts and pair facts that depend only on the current combat snapshot: creature score with the exact same flags, lethal/deathtouch status, block legality, and attack/block power/toughness values. The pair/triple enumeration and ordering remain unchanged. When a blocker is added/removed, or an earlier evaluation mutates targets/activating-player state, increment a pass generation and rebuild any facts whose inputs changed. Do not cache context-sensitive damage outcomes across a combat mutation.

This does not improve the worst-case combinatorial term, but it turns repeated expensive rules queries into `O(1)` lookups. A deeper algorithmic replacement—for example, a matching/flow formulation—would risk changing the heuristic's ordered pair/triple choice and is not recommended unless it can reproduce the current objective and tie-breaking exactly.

### 5.3 Target availability versus target enumeration

`TargetRestrictions.getNumCandidates` is `O(P + C)` plus allocation even when callers need only one or `minTargets`. Thresholded counting lowers typical behavior to `O(k)` matches after traversing the same ordered sources, where `k` is the required count. It does not alter full target enumeration.

In full simulation, multiple target-bearing subabilities form a dependent Cartesian/backtracking search. Later legal candidates can depend on earlier chosen targets, so globally precomputing every target list is unsound. Safe improvements are narrower:

- Reuse a target list only for the same subability, prior-target signature, game-copy generation, activating player, and targeting-player context.
- Use stable entity IDs in a compact prior-target signature, not mutable object hash codes.
- Invalidate on every simulated state mutation; a branch-local cache can simply die with its copied game.
- Preserve the current selector's sliding-window target sequence. It does not enumerate arbitrary combinations in all cases, and “completing” that search would change AI behavior and cost.

`PossibleTargetSelector.SimilarTargetSkipper` also scans previously seen same-name targets to decide whether a new target is equivalent. In the adversarial case of many same-name but non-equivalent tokens this is `O(T²)`, with comparisons that include owner/controller, ability count, type text, creature score, and combat status. An exact replacement can build a stable `TargetEquivalenceKey` containing precisely those observed fields and use a `LinkedHashMap` from key to the first target ordinal. The original encounter order and “first representative” rule remain. This is safe only within one selector reset and only if key construction is side-effect free; run the old skipper in shadow mode and compare every skip decision before switching.

### 5.4 Static ability layers and dependencies

`checkStaticAbilities` currently rescans the full static list to construct a list for each continuous layer. That list-allocation work can be reduced with ordered layer buckets. New statics discovered while applying effects complicate this: insertion must use the same comparator and discovery semantics, and dependency resolution must see exactly the same set at the same point.

The dependency routine compares a candidate to other statics and may temporarily apply/remove effects and recompute affected/result sets. Caching those sets is safe only within a single dependency pass and only under a pass generation that changes on each temporary application/removal. A broader “static state cache” without a comprehensive game mutation version would be stale. Start with trait views and per-invocation layer scratch structures; profile again before attempting incremental continuous-effect recomputation.

### 5.5 Ordered trigger/replacement indexes

For triggers, maintain ordered `EnumMap<TriggerType, List<Trigger>>` buckets alongside the canonical active list. Registration, clear, suppression-reset, and active-trigger reset update both. `runWaitingTrigger` then scans the relevant bucket while retaining delayed-trigger and Panharmonicon behavior. The memory cost is one reference per active trigger plus list headers. This removes repeated irrelevant-trigger tests but not trigger execution.

For replacements, an ordered `(ReplacementType, ReplacementLayer)` registry could avoid scanning irrelevant effects. It is substantially higher risk: the current method has LKI/pre-list treatment, affected-zone checks, `hasRun`, requirements, `canReplace`, and order selection. The index may filter only by immutable declared type/layer; all dynamic checks and exact game traversal order must remain. Update it on zone movement, trait-generation changes, controller changes where order is affected, and LKI construction. Implement only after trait caching and profiling show discovery still dominates.

### 5.6 Game-state versioning

Forge's game timestamp is an effect-order timestamp, not a verified universal mutation version. It must not be reused as a cache-validity token. Any cross-call derived-state cache needs a **new** `GameStateVersion` whose increments are centralized at all mutations relevant to the cached property: zones, characteristics, counters, life, mana, controller, phase/priority, stack, choices, combat, remembered/imprinted data, triggers/replacements, and random-dependent state as applicable. That is a large correctness surface. Prefer per-invocation or per-decision caches whose lifetime is already bounded by known no-mutation regions; add universal versioning only with mutation coverage tests.

## 6. Caching opportunities

| Computation | Current path | Cache key | Lifetime | Invalidation | Memory cost | Expected benefit | Risk |
|---|---|---|---|---|---|---|---|
| Trigger/static/replacement trait views | `CardState.getTriggers`, `getStaticAbilities`, `getReplacementEffects(boolean)` | `CardState` identity + accessor kind + `rulesHost` for replacements | Until explicit card/state trait-cache invalidation | PR #11366's audited trait/changed-trait/copy/state/keyword/type/counter mutators; all card states for split dependencies | Up to four small retained views per `CardState` | **Potentially very high; historically measured 2.15× in one seeded game** | Medium until invalidation audit and corpus parity are complete |
| Original game score | `SpellAbilityPicker` then each `GameSimulator` constructor | Simulation root identity + evaluation phase/context | One root decision/search node | Any game mutation; easiest is immutable argument valid only before branch mutation | One `Score` per node | High in full simulation, especially when score causes combat copy | Low |
| Conventional sort facts | Ability comparator | `SpellAbility` identity + exact sort context | One candidate sort | Discard after sort; no live mutation allowed during sort | Tens of bytes per candidate plus maps | Medium | Low if exact comparator trace matches |
| Target existence/count threshold | `getNumCandidates` | No cache needed; bounded traversal is preferable | One call | N/A | None | Low to medium broadly; high for large zones and many probes | Low |
| Full target list | `PossibleTargetSelector.reset` | `(copiedGame branch id/generation, SA identity, subability index, prior-target stable IDs, targeting player)` | One branch/choice traversal | Any mutation of branch state or prior targets | `O(targets × active selector contexts)`; strict local cap | Medium/high in target-heavy abilities if hit rate exists | High stale-legality risk |
| Similar-target equivalence | `PossibleTargetSelector.SimilarTargetSkipper` same-name prior scan | Exact immutable tuple of the fields compared today, scoped to selector reset | One selector reset | Rebuild on reset or combat/characteristic change | `O(T)` keys | Medium/high for many same-name targets; changes worst case from `O(T²)` comparisons to expected `O(T)` | Medium; equality key must exactly match old predicate |
| Creature/combat facts | `AiBlockController`, attack scoring, repeated `CreatureEvaluator` | `(Card identity, evaluation flags, combatPassGeneration)` | One immutable combat pass | Add/remove attacker/blocker, combat damage assignment, characteristic/controller/status change | `O(A+B)` values plus optional `O(A×B)` legality bits | High in token combat | Medium; many queries are context-sensitive |
| Attack static-source facts | `AttackConstraints`, `StaticAbilityMustAttack` | Construction-local; static ability identity and ordered attacker/defender IDs | One `AttackConstraints` construction | Discard after construction | `O(S + A + defenders)` | Medium/high with many attackers/statics | Medium |
| Mana-source grouping | `ComputerUtilMana.groupSourcesByManaColor` | `(player, SA/cost context, decision generation, checkPlayable, relevant static/replacement generation)` | Prefer one feasibility call initially; decision-wide only after versioning | Zone, tap/untap, mana ability, cost, controller, static/replacement changes | `O(mana abilities)` | Potentially high given historical stacks | High if scoped too broadly |
| Adjusted structural cost | `ComputerUtilCost` -> mana and additional-cost paths | Ability/cost identity + payer + one call context | One `canPayCost` invocation | Discard on return | One adjusted `Cost` | Medium | Low/medium; adjustment side effects require audit |
| Active triggers by type | `TriggerHandler.runWaitingTrigger` | Trigger type in handler-owned ordered buckets | Until active trigger set changes | Registration, removal, reset, suppression and trait/zone changes | One extra reference per active trigger | Medium/high in trigger-heavy games | Medium, ordering/LKI/delayed semantics |
| Replacements by type/layer | `ReplacementHandler.getReplacementList` | Handler-owned `(type, layer)` ordered bucket | Until replacement-source set changes | Zone/trait/controller/order/LKI changes | One extra reference per effect per declared bucket | High if discovery remains hot | High |
| Existing `AiCache` facts | Global static synchronized multimap, cleared by every AI decision | Replace with typed keys in `AiDecisionContext`; identity IDs rather than game object retention | One decision, except explicitly immutable deck facts | Drop entire context | Bounded by queried cards/actions in one decision | Medium and removes cross-game interference | Low/medium depending on fact purity |
| Cross-turn/game evaluation cache | Not generally present | Requires canonical complete state plus all AI context/RNG—currently unavailable | None recommended now | Unproven | Could retain whole games | Uncertain | **Very high; reject until canonical state/version exists** |

`AiDeckStatistics` is a useful special case: simulation copies share stable deck information, and its current cache is already measured. It should remain separate from mutable state caches. Soft references are not recommended for correctness-sensitive cache policy because eviction timing is GC-dependent and unpredictable. Weak keys are appropriate only where owner identity naturally defines lifetime and values do not retain the key/game; otherwise explicit generation/decision ownership is clearer.

## 7. Multicore opportunities

### 7.1 Thread-safety audit

No inspected type carries a general thread-safety contract. The verdicts below are based on observed mutation, not an assumption that absence of `synchronized` alone proves a bug.

| Type / subsystem | Observed mutable/shared state or side effect | Parallel-read verdict |
|---|---|---|
| `Game` | Players, zones, stack, phase, action/handlers, timestamps, events/listeners; aggregate query synchronization covers only a method, not a state snapshot | Unsafe to evaluate concurrently with live mutation; a complete isolated copy is required |
| `Player` and zone objects | Life/mana/controller/keywords/zones and ordered collections change during rules and AI payment/choice | Unsafe on live game; no verified immutable snapshot contract |
| `Card` / `CardState` | State, traits, controller, counters, timestamps, remembered relationships and lazily constructed abilities/caches; getters can synthesize/update views | Unsafe on shared live/card-copy instance; branch-owned cards only |
| `SpellAbility` | Activating player, targets, modes, X/payment and subability links are mutable; scoring/mana helpers set activating player | Unsafe to score the same instance concurrently; copy/freeze descriptors then map to branch-owned SA |
| `Combat` / `AttackConstraints` | Attacker/blocker maps and requirement state mutate; `addAttacker` being synchronized does not make compound evaluation atomic | Unsafe for concurrent evaluation/application; immutable facts plus serial mutation only |
| `TriggerHandler` / trigger objects | Active/delayed/waiting collections, suppression, trigger ability state and ordering mutate | Remain serial per game/branch |
| `ReplacementHandler` / effects | Per-run layer/history and applicability/order choices interact with mutable game/LKI | Remain serial per game/branch |
| Static effects / `GameAction` | Applies/removes continuous effects temporarily, mutates derived characteristics, affected sets, and emits events | Remain serial per game/branch |
| `AiController` / `AiCache` | Controller memories reset/change per decision; `AiCache` is process-global synchronized and cleared by each AI | Do not share decision state; replace with branch/decision ownership |
| `SimulationController` | Mutable score/simulator/current-target stacks, best sequence and narrow effect cache | One instance per branch; never shared |
| `GameCopier` | Owns mutable player/card bi-maps and reads an original game while building a new graph | One copier per branch; original must not mutate during copy |
| `MyRandom` | One process-global mutable generator replaced by simulation | Hard blocker for concurrent deterministic branches/games |
| Ability-AI registry/helpers | Lazy static `SpellApiToAi` map and singleton `SpellAbilityAi` objects; examples of static scratch/cache/profile/debug state exist | Eager immutable registry plus per-branch state audit required; do not assume subclass statelessness |
| Logging/event UI | Shared output ordering and event delivery can become observable | Workers should record branch-local diagnostics; emit serially by ordinal |

Even apparently read-only `CreatureEvaluator` calls transitively reach mutable abilities, replacements, and static rules. Purity has to be established method-by-method and represented by immutable input/output types before a helper enters a worker.

### 7.2 Candidate regions

| Candidate | Current path | Independence | Mutation risk | Proposed model | Determinism strategy | Activation threshold | Likely benefit |
|---|---|---|---|---|---|---|---|
| Conventional `SpellAbilityAi` candidate scoring on live game | `AiController.chooseSpellAbilityToPlayFromList` | Low: heuristics share live objects and may set activating players/targets or consult mutable controller state | **High** | Keep serial. Extract/cache pure subfacts only | Existing order unchanged | Never parallel in current architecture | None safely available |
| Top-level full-simulation candidates | `SpellAbilityPicker.chooseSpellAbilityToPlayImpl/evaluateSa` | Conceptually independent after each branch receives its own copied game, controller, context, and RNG | Currently **high** because `MyRandom`, AI singletons/debug fields, and setup touch globals | After isolation work, submit one task per top-level candidate (or chunk); recursive levels remain serial | Freeze ordered candidate list; preassign seeds serially in current draw order; result array by index; serial strict-`>` reduction preserves first-on-tie | At least 2 candidates and predicted serial branch time comfortably above scheduling/copy-memory cost | High for several expensive branches, bounded by serial copying/rules work and memory |
| Target combinations inside one candidate | `SpellAbilityChoicesIterator` loop | Each complete target/mode branch can be independent only with its own game copy and choice state | High today: selector mutates original SA targets and global RNG | Defer until candidate-level parallelism is proven; candidate chunks are coarser and safer | Stable selection ordinal and preassigned seed per ordinal; serial reduction | Only exceptionally large choice count; avoid nested pools | Medium, but memory multiplication can erase gain |
| Combat score subfacts | `AiAttackController`, `AiBlockController` | Some card/edge facts are pure over a frozen snapshot | Current scoring helpers can mutate SAs and read combat in progress | Compute explicitly pure DTO facts in parallel; apply attack/block changes serially | Stable card indices and ordered serial application | Board-size/time model says fact phase exceeds overhead | Medium on very large token boards |
| Existing forced-attacker futures | `AiAttackController.declareAttackers` | Not established | **High**, including post-timeout mutation | Serialize immediately, or rewrite over immutable facts | Original attacker order | Serial by default | Likely low performance loss; correctness gain |
| Independent AI-vs-AI games in one JVM | `SimulateMatch` sequential loop | Games are logically independent | **High** today: process-global RNG and mutable static caches/singletons | Use separate JVM worker processes now; consider in-process only after global-state audit/migration | One seed per game index; results emitted/reduced by index | ≥2 games and process startup amortized | High throughput scaling with isolation |
| Independent AI-vs-AI games as processes | Existing test `MultiProcessGameExecutor` precedent | Strong process isolation | Low, subject to files/log output | Bounded process pool | Preassign seeds/decks/index; deterministic ordered report | Batch workload | Near-linear until CPU/memory/IO saturation; must measure |

### 7.3 What must remain serial

The following must remain serial unless explicitly redesigned around isolated copied state:

- `PhaseHandler` priority, stack resolution, SBA, triggers, replacements, zone changes, event/listener delivery, and live `Game` mutation.
- Conventional `canPlaySa`/`SpellAbilityAi` evaluation on the live game. “Read-looking” helpers can mutate `SpellAbility` target or activating-player state.
- Attack/block application and validation on a live `Combat`.
- Trigger/replacement order selection and continuous-effect dependency resolution.
- Final candidate reduction and application to the live game.
- Recursive simulation by default. Nested parallelism risks pool starvation, task explosion, multiplied copies, and changed RNG draw order.

### 7.4 Required isolation and determinism work

Candidate simulation is not safe merely because `GameCopier` exists. The branch must also own:

- its `SimulationController`, `SpellAbilityPicker`, choice iterator, debug sink, and AI decision context;
- a game-scoped RNG, replacing temporary writes to static `MyRandom`;
- non-shared mutable `SpellAbilityAi` state, or verified immutable/eager singleton implementations;
- no static `ChangeZoneAi` scratch collection or global cache mutation;
- a copied/immutable profile/configuration snapshot.

The migration must first make serial output identical. At the root, consume `nextLong()` in the same candidate/choice order as today and attach each seed to a stable ordinal. Workers never draw from the root stream. Store scores in an indexed array; after all tasks finish, run today's strict `>` comparison from index zero. This preserves earlier-candidate tie-breaking. Avoid hash-based reduction and floating-point parallel aggregation. If a branch throws, cancel the decision, wait for isolated tasks to terminate, and run the entire decision serially from an unmodified root state; never mix partial results.

## 8. Adaptive CPU manager

This section proposes a **new** component; Forge does not currently have an AI-specific resource manager. [`ThreadUtil`](https://github.com/Card-Forge/forge/blob/e3c5554c79e6e6f225697b821d36db1863eb467d/forge-core/src/main/java/forge/util/ThreadUtil.java#L5-L35) exposes `Runtime.availableProcessors()`, but `getComputingPool(loadFactor)` creates a new pool and divides processor count by `(1-loadFactor)`, which can produce more workers than processors. It is unsuitable for AI policy. `GuiBase.isMobile()`/Android/iOS checks exist, but `forge-ai` should receive platform capabilities through configuration rather than depend on the GUI module.

Java reliably exposes logical processors through `Runtime.getRuntime().availableProcessors()`. Standard Java does not reliably expose physical cores across desktop and mobile platforms; do not make physical-core detection part of correctness or the default. Optional OS MXBean information can be diagnostic only. Container CPU quotas can also make `availableProcessors` JVM-version/configuration dependent, so expose the detected value in diagnostics and allow override.

### 8.1 Default worker policy

```java
// NEW: forge.ai.concurrent.AiResourceManager (package location illustrative)
static int defaultWorkers(int logical, boolean mobile, int userMaximum) {
    logical = Math.max(1, logical);
    int usable;
    if (logical <= 2) {
        usable = 1;
    } else if (logical <= 4) {
        usable = logical - 2;
    } else {
        usable = logical - Math.max(2, (logical + 3) / 4); // reserve >=25%, >=2
    }
    int platformCap = mobile ? 2 : 12;
    int configuredCap = userMaximum > 0 ? userMaximum : platformCap;
    return Math.max(1, Math.min(usable, Math.min(configuredCap, platformCap)));
}
```

Initial mapping:

| Logical CPUs | Reserved by formula | Desktop AI workers | Mobile AI workers |
|---:|---:|---:|---:|
| 2 | 1 | 1 | 1 |
| 4 | 2 | 2 | 2 |
| 8 | 2 | 6 | 2 |
| 16 | 4 | 12 | 2 |
| 32+ | at least 25% | 12 (default cap) | 2 |

The reservation covers the UI/event thread, the waiting/main game thread, GC/JIT/OS work, and unrelated Forge executors. The desktop cap avoids multiplying full-game copies across every hardware thread; it is a starting policy to validate, not a claimed optimum. A user preference of `0 = automatic` and `1..N = maximum` should never exceed the platform cap unless a separate explicit “allow more than recommended” advanced setting is introduced. `1` guarantees serial execution for bisecting regressions.

### 8.2 Workload-aware activation

Do not use only candidate count. Maintain allocation-free exponentially weighted timing estimates by workload bucket, updated after serial and parallel decisions:

```text
work bucket = (candidate-count bucket,
               copied-card-count bucket,
               target/mode-choice-count bucket,
               full-simulation depth)
```

Parallelize only when all are true:

1. More than one isolated top-level branch is ready.
2. At least two worker and memory permits are available.
3. The estimated average branch time is greater than `2 × dispatchP95`.
4. Estimated serial total is greater than `4 × dispatchP95`.
5. The decision is not already executing inside an AI worker.

`dispatchP95` is measured with no-op submissions to this already-started executor at application/game startup and refreshed infrequently; no fixed millisecond threshold is assumed across Android and desktop. Unknown buckets run serially for the first observations, except an advanced opt-in benchmarking mode. Card/choice counts serve as fallback predictors, but thresholds must be fitted from the representative benchmark corpus. Scheduling policy affects latency only, not branch content or selection.

### 8.3 Executor, lifecycle, and failure model

- One process-wide, explicitly named fixed `ThreadPoolExecutor` for isolated AI evaluation; no `ForkJoinPool.commonPool`.
- Core timeout off while a match is active; daemon threads with Forge's uncaught-exception logging.
- Bounded queue, initially `2 × workerCount`. Rejection uses caller-runs only for an already isolated branch; otherwise execute the whole decision serially.
- Top-level task count no greater than `min(branches, workers, memoryPermits)`. Chunk adjacent stable branch ordinals when candidates greatly exceed workers, reducing queue/synchronization overhead.
- Start lazily from application AI services; update pool size between decisions when settings/platform state changes; call orderly `shutdown`, bounded `awaitTermination`, then `shutdownNow` during application shutdown.
- Interruption is cooperative and checked between choice simulations/copies. It must never stop live-game resolution.
- A worker exception is captured with branch ordinal/seed. Cancel and await every sibling, discard all results, and retry once serially from the untouched root. If serial retry fails, propagate the original game error; do not select from partial scores.

## 9. Adaptive memory manager

This is another **new** component. The reliable budget authority is the JVM heap, not installed RAM:

```java
Runtime rt = Runtime.getRuntime();
long maxHeap = rt.maxMemory();
long usedHeap = rt.totalMemory() - rt.freeMemory();
long headroom = Math.max(0L, maxHeap - usedHeap);
```

Physical RAM may be reported on some platforms (`GuiBase` already reports mobile device RAM and the desktop JVM can sometimes expose an OS MXBean), but the JVM cannot spend memory above `maxHeap`; physical RAM is therefore a diagnostic/cap modifier only. The manager should receive platform information through an `AiPlatformInfo` interface so the core AI module stays platform-neutral.

### 9.1 Initial budget calculation

```java
// NEW policy; all constants are conservative initial settings to benchmark.
long safetyReserve = Math.max(64L << 20, maxHeap * 15 / 100);
long spendable = Math.max(0L, headroom - safetyReserve);
long automaticCacheBudget = Math.min(maxHeap / 20, spendable / 4); // <=5% heap
long cacheBudget = Math.min(userCacheCapOrUnlimited, automaticCacheBudget);
long simulationTransientBudget = Math.min(maxHeap / 4,
    Math.max(0L, spendable - cacheBudget));
```

On mobile, start with cache and transient caps halved unless device-specific benchmarks support more. A user setting of `0 = automatic` is preferable to a default fixed number of megabytes. An explicit `off` disables optional longer-lived caches but not tiny correctness-neutral invocation-local memoization.

### 9.2 Cache and pressure policy

- **Invocation/decision caches:** identity maps owned by `AiDecisionContext`; no eviction during the decision, but the context and all game references are released at its end. Enforce an entry cap derived from candidate/board size and stop adding entries when reached.
- **CardState trait caches:** lifecycle-owned by the card state and generation-invalidated, not part of a global LRU. They cache tiny views of already-owned traits.
- **Any longer-lived derived cache:** weighted size-bounded LRU with explicit generation keys and values that do not retain `Game`, `Player`, or `Card` graphs. Weight approximate arrays/list capacity and referenced value size, not only entry count.
- **No soft-reference cache:** soft-reference clearing is GC-policy dependent and often creates latency spikes. Weak keys are acceptable only when value-to-key/game retention is impossible.
- **Pressure detection:** sample old-generation/heap-pool usage through `MemoryPoolMXBean` after GC notifications when available, plus rolling GC time. Enter pressure mode when post-GC occupancy exceeds 85% of the relevant maximum, heap headroom falls below `safetyReserve`, or GC consumes more than 10% of a recent 10-second window. These are starting thresholds to calibrate.
- **Pressure response:** reject new optional cache entries, evict longer-lived caches to 25% of budget, lower simulation permits, and run new decisions serially if only one permit remains. Exit with hysteresis (for example post-GC occupancy below 70% for two windows). Never call `System.gc()`.

### 9.3 Bounded simulation concurrency

Full game copies dominate transient memory risk. Measure p95 allocated and p95 retained bytes for one simulation branch using JFR/allocation profiling in each workload bucket. A weighted semaphore gates submissions:

```java
long availableForBranches = simulationTransientBudget;
long branchBytes = Math.max(measuredP95Bytes(bucket), conservativeFloorBytes);
int memoryPermits = (int) Math.max(1L, availableForBranches / branchBytes);
int concurrentBranches = Math.min(cpuWorkers, memoryPermits);
```

Until measurements exist, allow only two concurrent full-copy simulations on desktop and one on mobile. This bootstrap cap is intentionally conservative. Update estimates between decisions, never while a permit calculation is active. A branch acquires its weighted permit before copying and releases in `finally`. If unavailable, apply backpressure by running queued ordinals serially after existing branches complete; do not create unbounded waiting tasks.

This coordinates CPU and RAM: increasing workers cannot multiply `GameCopier` memory beyond transient budget, while memory pressure reduces both cache growth and branch concurrency. Separate-JVM AI-vs-AI throughput needs an outer process manager with the same equation using per-process measured RSS/heap; do not independently let every process choose the full machine's CPU and memory budget.

## 10. Proposed code changes

Every type explicitly marked **NEW** below is a design proposal, not an existing Forge class or API.

### 10.1 Reuse the root game score

**Existing:** `SpellAbilityPicker.chooseSpellAbilityToPlayImpl/evaluateSa`; `GameSimulator` constructor.

**Before:**

```java
Score origGameScore = evaluator.getScoreForGameState(game, player);
// ... for each candidate/choice
GameSimulator simulator = new GameSimulator(controller, game, player, phase);
// constructor recomputes evaluator.getScoreForGameState(game, player)
```

**After, conditional on the baseline-invariance shadow test:**

```java
// NEW immutable value object, or constructor parameters if kept smaller
record SimulationRoot(Game game, Player ai, Score baseline, PhaseType phase) {}

GameSimulator(SimulationController controller, SimulationRoot root) {
    this.origScore = root.baseline();
    // copy and map as today
}
```

Keep a compatibility constructor for tests/callers that do not already own a baseline. Compute the reusable value at the same serial point from which branch constructors currently observe the original game, and do not reuse it after evaluator-relevant mutation. The `Score` value itself is thread-safe because its two fields are final integers; that says nothing about the evaluator. Correctness test: constructor-supplied score must equal freshly evaluated score before every branch in a debug corpus, including pre-combat states that trigger lookahead and abilities whose setup changes targets, modes, X, or activating player.

> **Implemented (phase 1), and the condition did not hold.** The shadow test above was built first
> and it failed on a full board with recursive simulation: the score evaluated at the top of
> `chooseSpellAbilityToPlay` no longer equalled a fresh evaluation by the time the branches ran,
> because deciding whether each candidate can be played and paid for touches state the evaluator
> reads. `SimulationRoot` was therefore not introduced. What shipped is narrower:
>
> ```java
> // GameSimulator, fifth parameter; the four-argument constructor still evaluates for itself
> GameSimulator(SimulationController controller, Game origGame, Player origAiPlayer,
>         PhaseType advanceToPhase, Score knownOrigScore)
>
> // SpellAbilityPicker.evaluateSa: the first branch of a candidate evaluates the baseline,
> // the rest take it
> Score branchBaseline = null;
> do {
>     GameSimulator simulator = new GameSimulator(controller, game, player, phase, branchBaseline);
>     if (!GameSimulator.COPY_STACK) {
>         branchBaseline = simulator.getScoreForOrigGame();
>     }
>     ...
> } while (choicesIterator.advance(lastScore));
> ```
>
> The `COPY_STACK` guard matters: with a copied stack the simulator's baseline is the
> post-resolution score, not the score of the game as it stands, so it must not be handed on. The
> shadow check is kept as an `assert` inside the constructor, so it runs over the whole test suite
> and costs nothing in a shipped build.

### 10.2 Add bounded target predicates

**Existing:** `TargetRestrictions.getNumCandidates`, consumers in `ComputerUtilAbility.isFullyTargetable`, `SpellAbilityAi`, and charm/trigger checks.

**Proposed signature:**

```java
// NEW overload; name can follow Forge conventions
public boolean hasAtLeastCandidates(
    SpellAbility sa, int required, boolean includePlayers) { ... }
```

Implement a visitor that uses the exact existing player/card source order and predicate. Stop after `required`; return true immediately for `required <= 0`. Preserve current treatment of stack abilities and any duplicate semantics—do not “correct” the TODO in this performance patch. Replace boolean/threshold callers only. Keep list-returning callers unchanged. The traversal uses only caller state and needs no shared cache.

### 10.3 Cache `CardState` trigger/static/replacement views

**Existing:** `CardState` trigger/static/replacement fields, changed-trait maps/lists, and getters. Start from the concrete two-file design in open PR #11366; do not cache spell/mana ability views in this change.

**Proposed fields/API sketch:**

```java
// NEW fields on CardState, following PR #11366
private FCollectionView<Trigger> cachedTraitTriggers;
private FCollectionView<StaticAbility> cachedStaticAbilities;
private FCollectionView<ReplacementEffect> cachedReplacementsAsRulesHost;
private FCollectionView<ReplacementEffect> cachedReplacementsPlain;

final void invalidateTraitCache() {
    cachedTraitTriggers = null;
    cachedStaticAbilities = null;
    cachedReplacementsAsRulesHost = null;
    cachedReplacementsPlain = null;
}

// NEW method on Card for split/card-wide dependencies
public final void invalidateTraitCaches() {
    for (CardState state : states.values()) {
        state.invalidateTraitCache();
    }
}
```

Each getter returns its cached view when non-null and otherwise executes today's builder unchanged before storing it. Replacement getters use separate entries for `rulesHost`. Invalidation must include the concrete inputs enumerated above, not only add/remove base traits. Copying must leave caches cold or build independent views; it must not share cached collection objects between card states. This code is not made concurrently safe by caching: live card states remain serial and isolated branches own separate copies. A debug generation/rebuild comparison can strengthen the open PR's seeded-game evidence before merge.

### 10.4 Introduce a decision context

**NEW:** `AiDecisionContext`, constructed in `AiController.chooseSpellAbilityToPlay` and passed through newly overloaded helper methods.

```java
final class AiDecisionContext implements AutoCloseable {
    final long decisionId;
    final IdentityHashMap<SpellAbility, SpellSortFacts> sortFacts;
    final IdentityHashMap<Card, EnumMap<CreatureEvalMode, Integer>> creatureFacts;
    final AiCancellation cancellation;
    // No static ownership and no references after close().
}
```

First migrate `AiCache` facts whose purity and lifetime are already clear; do not bulk-cache every property query. Existing signatures can delegate to overloads with a no-cache context to keep patches reviewable. In simulations, each branch gets a child context; no mutable map is shared. The principal correctness risk is caching across a hidden mutation within one decision, so each entry documents its dependency and either uses a local generation or a shorter scope.

### 10.5 Batch attack-constraint discovery

**Existing:** `AttackConstraints` constructor, `StaticAbilityMustAttack.getAttackRequirements/entitiesMustAttack`.

**NEW internal helper:**

```java
AttackConstraintFacts collectAttackConstraintFacts(
    Game game,
    List<Card> orderedAttackers,
    List<GameEntity> orderedDefenders);
```

The helper collects only declared static sources once and evaluates them in today's nested encounter order. `AttackConstraints` is then populated with the same concrete restriction/requirement objects, and `collectLegalAttackers` is untouched. Build a shadow mode that computes old and new maps in the same run and asserts an order-sensitive serialization match. If any static evaluator mutates game/ability state or relies on re-traversal, narrow the batching change instead of masking the mismatch.

### 10.6 Create trigger type buckets

**Existing:** `TriggerHandler.activeTriggers` and reset/run methods.

**Proposed:** retain the canonical ordered collection and add `EnumMap<TriggerType, FCollection<Trigger>> activeByType`. Centralize registration/removal so both structures change together. `resetActiveTriggers` rebuilds both from the same single card traversal. `runWaitingTrigger` selects the relevant bucket but keeps current static/non-static passes, delayed-trigger handling, Panharmonicon calculation, and order choice. Debug builds periodically compare bucket concatenation/filtering with the old scan. Handler access remains confined to the serial game thread or branch-owned copy.

### 10.7 Make full-simulation RNG game/branch scoped

**Existing:** `MyRandom` static generator and `SpellAbilityPicker.evaluateSa` save/swap/restore.

**Proposed staged migration:**

1. Add **NEW** `GameRandom`/`RandomSource` owned by `Game` or an injected match context.
2. Route simulation and all code reachable from a simulation branch through that source; keep a compatibility adapter for non-migrated code.
3. In serial mode, prove exact draw count/order against the old seeded execution.
4. Preassign root branch seeds serially and give each copied game its branch source.
5. Only then enable top-level parallel execution via `AiResourceManager`.

Merely replacing the static with `ThreadLocal<Random>` is not equivalent: it changes which stream consumes each draw and can change decisions. The compatibility phase should record `(call-site category, ordinal, value)` traces under a test seed.

### 10.8 Deterministic top-level simulation executor

**Existing:** serial loop in `SpellAbilityPicker.chooseSpellAbilityToPlayImpl`.

**NEW API sketch:**

```java
List<BranchResult> evaluateTopLevel(
    List<FrozenCandidate> ordered,
    SimulationRoot root,
    AiResourceLease resources);

record BranchResult(int ordinal, Score score, DecisionTrace trace) {}
```

Freeze candidate descriptors and branch seeds before submission. Each worker copies the root and owns all mutation. Results are stored by ordinal. The existing serial reduction selects the first strict maximum. Do not share a `SimulationController`; its stacks, best sequence, cache, and current target are mutable. Recursion remains serial inside the worker. On timeout/cancel, workers must stop only at safe branch boundaries and their discarded copies must become unreachable.

### 10.9 Reduce or redesign `GameCopier`

This is Phase 5, not an early patch. First instrument bytes/time by zone and card-copy type. Then consider an exact structural copier that creates new mutable objects while sharing only verified immutable definitions (`PaperCard`, parsed script metadata, immutable keyword/type data). Build a field/reference audit against `GameCopier`: player mapping, card IDs/timestamps, states, traits, remembered/imprinted/exiled links, stack/subabilities/targets, combat, mana, delayed triggers, command/effect cards, LKI, and listeners.

Run old and new copiers side by side on serialized deterministic states and compare an order-sensitive canonical dump plus subsequent legal actions, scores, and resolved outcomes. Do not use current experimental `GameSnapshot` or the inaccurate direct-card-copy branch as-is. Copy-on-write live game structures are not recommended without a much broader ownership redesign.

### 10.10 Replacement and static registries

Only after fresh profiles:

- Add handler-owned ordered replacement buckets keyed by declared type/layer. Dynamic `canReplace`, validity, zone, layer history, LKI/pre-list, and player choice still execute exactly as now.
- Add invocation-local ordered static layer buckets. Do not make them cross-state caches until a complete mutation/version model exists.

These touch core rules behavior and have high correctness risk. Ship behind debug parity modes first, run the full rules suite, and keep the old implementation selectable for corpus comparison.

## 11. Benchmark and correctness plan

### 11.1 Build a reproducible decision harness first

Extend Forge's existing desktop simulation tests rather than trying to put an entire game in JMH. The repository already contains `GameSimulationTest`, `SpellAbilityPickerSimulationTest`, `GameStateEvaluatorTest`, `GameSimulatorSpellChoiceTest`, basic attack/block tests, `AIIntegrationTest`, and `OnePlaySafetyCheckerTest`. Add a headless integration harness that can:

- load a versioned game-state fixture or deterministically advance fixed decks to a checkpoint;
- set deck lists, player count/order, AI profiles, seed, phase, priority player, and JVM options;
- execute exactly one AI decision, one AI turn, one game, or a game batch;
- warm up without mixing warm-up samples into results;
- emit a machine-readable decision trace and metrics JSON;
- run baseline and optimized binaries from the same fixture corpus.

A state fixture must include or reconstruct everything that affects decisions: zones and their order, stack, phase/priority, life/mana/counters, card/controller/timestamps, remembered/imprinted links, combat, triggers/replacements, AI memory/profile, and RNG state. If Forge's existing game-state serialization cannot reproduce those exactly, use deterministic deck/command scripts to reach the checkpoint and verify a canonical state hash before timing.

### 11.2 Instrumentation and tools

Use complementary measurements:

1. **Java Flight Recorder / Mission Control.** Record CPU samples, Java monitor/thread events, object allocation samples, GC, compiler, and thread scheduling. A representative command is:

   ```text
   java -XX:StartFlightRecording=filename=forge-ai.jfr,settings=profile,dumponexit=true ...
   ```

   Add custom low-overhead JFR events around priority decisions, candidate generation, legality/cost, target enumeration, `GameCopier`, state score/combat lookahead, static/SBA, trigger/replacement discovery, attack constraints, and block passes. Include counts (cards, candidates, target choices, statics, triggers, replacements, attackers/blockers, copied cards) so samples can be normalized.

2. **async-profiler.** Capture separate CPU, allocation, and lock profiles/flame graphs. CPU shows executed work, allocation identifies short-lived collections/copies, and lock mode tests whether synchronized caches/zone queries matter. Use wall-clock mode to expose parking/scheduling and timeout behavior. Do not infer allocation volume from CPU flames alone.

3. **GC logs.** Enable unified logging (`-Xlog:gc*` on the chosen supported JVM) and correlate total/pause time with decision windows. Record collector, heap size, and JVM version; do not compare runs with different collectors or adaptive heap settings.

4. **JMH.** Use only for isolated, deterministic kernels after the profile identifies them: cached versus rebuilt trait views, bounded target counting, comparator facts, attack-constraint batching, and target selector scratch structures. Supply realistic cardinalities and consume results. Do not use JMH for the whole Forge game loop, which is stateful, multi-phase, and better measured by the integration harness.

5. **Counters.** Record cache lookup/hit/miss/eviction/stale-debug-check counts, copied cards/bytes, simulation branches, memory-permit wait time, queue depth, worker busy time, cancellation, and serial fallback. Counters must be disabled or sampled for final validation to quantify their own overhead.

Every optimization gets four runs: baseline without instrumentation, optimized without instrumentation, baseline profiled, optimized profiled. Fork fresh JVMs, pin heap/JVM flags, use enough iterations for confidence intervals, and randomize baseline/optimized run order to reduce thermal/background bias.

### 11.3 Representative scenarios

Store exact deck lists and fixture-generation scripts in the repository. At minimum:

| ID | Scenario | State characteristics | Primary paths stressed |
|---:|---|---|---|
| 1 | Simple early-game turn | Two players, few cards/permanents, no stack | Scheduling overhead, candidate baseline; must remain serial-fast |
| 2 | Medium board | Several permanents and cards in hand per player, normal costs/targets | Conventional candidate, mana, sort, general rules queries |
| 3 | Large Commander | Four players, large libraries/graveyards, varied commanders/permanents | Aggregate zones, replacement/static scans, game copying, multiplayer scaling |
| 4 | Token-heavy board | Dozens/hundreds of similar and non-identical tokens | Card/property scans, trait views, combat constraints, allocation/GC |
| 5 | Trigger-heavy board | Many active and delayed triggers from distinct sources | Trigger reset/discovery/type filtering, stack/SBA loops |
| 6 | Static-heavy board | Many continuous effects across layers and dependencies | Static discovery, layering/dependencies, state-effect iterations |
| 7 | Complex targeting | Modal/X ability with multiple dependent target-bearing subabilities | Target construction, selector enumeration, per-choice simulations |
| 8 | Large combat | Many attackers/blockers, evasion, must/can't attack/block and banding where supported | `AttackConstraints`, attack permutations, gang blocks, combat evaluation |
| 9 | Single AI-vs-AI game | Fixed decks, profiles, seed; complete match | End-to-end latency, decision tail, outcome/trace parity |
| 10 | Multiple independent games | Same indexed seed/deck corpus in separate JVM workers, later isolated in-process experiment | Throughput, CPU scaling, aggregate heap/RSS, contention |

Include at least one fixture derived from the workloads in issues [#6944](https://github.com/Card-Forge/forge/issues/6944) and [#10150](https://github.com/Card-Forge/forge/issues/10150), but turn the report into an exact reproducible state rather than treating the anecdote as a benchmark.

For each scenario record:

- median, p95, and p99 AI decision latency;
- AI turn duration and phase-specific duration;
- process and worker CPU utilization;
- allocation bytes and object count per decision;
- GC pause count/time, total GC time, and peak/post-GC heap;
- candidate, target-choice, copy, static, trigger, replacement, attacker, and blocker counts;
- cache hit/miss/eviction rate and retained weight;
- executor queue delay, worker utilization, permit wait, cancellation/fallback;
- complete-game duration and AI-vs-AI games/hour.

Report raw samples, JVM/OS/hardware, logical CPUs, heap, collector, build commit, AI profile, decks, seeds, and fixture hashes. For parallel scaling run workers `1, 2, 4, 6, 8, 12` where hardware permits; compute speedup and efficiency relative to worker 1. Watch p99/UI responsiveness, not only throughput.

### 11.4 Optimization-specific experiments

| Change | Required comparison | Success criterion beyond “faster” |
|---|---|---|
| Baseline score reuse | Count/evaluate root score and nested combat copies per branch before/after | Exactly one baseline evaluation per search node; identical decision trace |
| Trait-view cache | CPU/allocation profile, hit rate, generation invalidations, PR #11366 scenario and full corpus | Reproduce material gain; zero debug rebuild mismatches; bounded per-state retention |
| Target threshold traversal | JMH cardinalities plus target-heavy integration | Same boolean for every fixture/property-generated restriction; less allocation/time for threshold consumers |
| Sort facts | Candidate-list sort profiles and ordered output dump | Byte-for-byte ordered candidate descriptor list |
| Cost adjustment reuse | Invocation counters, mana/cost scenarios | One structural adjustment where formerly duplicate; same payable result and chosen payment |
| Attack batching/pass facts | Token/static/combat scenarios over increasing A/B | Same restrictions, legal attack set, and declared combat; slope/constant reduction demonstrated |
| Trigger buckets | Trigger-heavy fixtures with registration/removal/suppression/LKI | Same ordered fired-trigger trace; irrelevant scans reduced |
| Replacement buckets | Replacement-heavy fixtures across layers/LKI/order choices | Same ordered applicable list and selected effect; discovery share reduced |
| Parallel simulation | Worker sweep under target/Commander/full-sim states | Exact serial trace/outcome/RNG trace; lower latency without GC/p99 regression |
| Multi-process games | Process-count sweep | Higher games/hour within CPU/RAM caps; indexed results identical to serial batch |

### 11.5 Correctness and determinism tests

Performance tests are not enough. Add a decision-trace mode that emits stable descriptors—not object hash codes—for:

- ordered generated legal candidates and optional/alternate-cost variants;
- each candidate's modes, targets, X/payment choices, score, and heuristic outcome;
- final chosen ability, source zone/card stable ID, targets and modes;
- declared attackers/defenders/bands and ordered blockers;
- ordered triggers/replacements considered, applicable, selected, and resolved;
- RNG draw category, ordinal, and value;
- resulting canonical state hash and game outcome.

Run the deterministic corpus in these pairs:

1. baseline versus each optimization;
2. cache disabled versus enabled;
3. original target counting versus bounded predicate in shadow mode;
4. old versus new attack/trigger/replacement discovery in shadow mode;
5. optimized serial versus optimized parallel at every worker count;
6. single-game serial batch versus indexed separate-process batch;
7. copied-state old implementation versus any future structural copier.

Add focused tests for legal action generation, timing restrictions, alternative/optional costs, multi-target subabilities, target ordering, ward/additional costs, attack requirements/limits, gang blocking, trigger suppression/delays/order, replacement layers/LKI/order, static dependencies, deterministic seeds, and simulation recursion.

The default pass criterion is **exact trace identity**. If actions differ, first show whether baseline candidates tied under the exact existing comparison and whether both lead to the same observable behavior. Do not automatically accept “equivalent score”: target, order, RNG consumption, or later state can differ. Any intentional tie-policy relaxation belongs in a separate gameplay/AI change, not this performance program.

## 12. Prioritized implementation roadmap

| Phase | Priority | Optimization | Difficulty | Correctness Risk | Expected Impact | Memory Cost | Requires Profiling |
|---|---:|---|---|---|---|---|---|
| Phase 1 — Measurement and low-risk fixes | P0 | Pin fixtures/seeds/JVM; add decision metrics, JFR events, canonical traces, and baseline dashboards | Medium | Low | Enables every reliable decision | Low when disabled/sampled | Yes—this is the profiling foundation |
| Phase 1 | P0 | Reproduce historical PR #11366 and #11160 evidence on pinned/current source | Medium | Low | Establishes actual ranking | Profiling overhead only | Yes |
| Phase 1 | P0 | Remove or make pure the current common-pool forced-attacker futures; add timeout regression test | Medium | Medium (existing behavior may expose races) | Reliability/determinism; performance uncertain | Lower or neutral | Yes, to quantify impact |
| Phase 1 | P1 | Pass root `Score` into `GameSimulator`; eliminate repeated unchanged score/combat-copy work | Low | Low | High in full simulation | Lower transient allocation | Yes, verify copy/score counts |
| Phase 1 | P1 | Add `hasAtLeastCandidates` and migrate threshold-only callers | Low/medium | Low | Low/medium broadly, larger with zones | Lower | JMH plus integration |
| Phase 1 | P1 | Per-sort decision facts with exact ordered-output test | Low/medium | Low | Medium conventional AI | Small decision-local map | Yes |
| Phase 1 | P2 | Reuse one structural adjusted cost within `canPayCost` | Medium | Medium | Medium | Lower | Yes |
| Phase 1 | P2 | Replace per-decision OS thread with lifecycle executor and cooperative cancellation | Medium | Medium | Low latency/allocation; robustness | One retained thread/queue | Yes |
| Phase 2 — Redundant computation, allocations, and caching | P0 | Finish/integrate invalidation-safe `CardState` trigger/static/replacement cache | Medium/high | Medium/high until mutator audit passes | Potentially very high; one historical 2.15× result | Small per card state | Yes, reproduce and monitor retention |
| Phase 2 | P1 | Introduce scoped `AiDecisionContext`; migrate proven `AiCache` facts | Medium | Medium | Medium | Bounded per decision | Yes; require hit rates |
| Phase 2 | P1 | Batch attack static discovery and remove unconditional `A²` “other” list creation | High | Medium/high | High in token/static combat | `O(A+S)` facts | Yes |
| Phase 2 | P1 | Pass-scoped combat/creature facts with mutation generation | High | Medium/high | High in large combat | `O(A+B)`; optional bounded edge facts | Yes |
| Phase 2 | P2 | Ordered trigger-type buckets | Medium | Medium | Medium/high trigger-heavy | One reference per active trigger | Yes |
| Phase 2 | P2 | Iterator-local target selector/scratch reuse | Medium | Medium | Medium target-heavy | Lower transient allocation | Yes |
| Phase 3 — Safe multicore evaluation | P0 | Audit/migrate `MyRandom` to game/branch-scoped source with serial draw-trace parity | High | High | Prerequisite, little direct speedup | One RNG/context per game/branch | Yes |
| Phase 3 | P0 | Remove/static-isolate mutable AI helpers, debug sinks, and lazy singleton hazards from simulation reachability | High | High | Prerequisite | Small per branch | Yes/static audit |
| Phase 3 | P1 | Deterministic top-level full-simulation branch executor; recursive search remains serial | High | High | High in multi-branch full simulation | Multiple bounded game copies | Yes, worker sweep |
| Phase 3 | P2 | Pure parallel combat subfact prototype, serial application | High | High | Medium in very large combat | Bounded fact arrays | Yes; abandon if helpers cannot be pure |
| Phase 3 | P1 | Production bounded separate-JVM AI-vs-AI batch runner | Medium | Low/medium | High throughput | One heap per worker process | Yes, process sweep |
| Phase 4 — Adaptive CPU and memory management | P1 | Add `AiResourceManager`, user cap, bounded executor, adaptive threshold, diagnostics | Medium | Low after branch isolation | Preserves gains across hardware/latency classes | Small | Yes, especially mobile/2–32 CPUs |
| Phase 4 | P1 | Add `AiMemoryBudget`, weighted semaphore, pressure hysteresis, cache budgets | Medium/high | Medium | Avoids GC regressions/OOM; enables safe parallelism | Explicitly bounded | Yes, allocation/retained heap |
| Phase 4 | P2 | Coordinate outer multi-game process limits with per-game AI limits | Medium | Medium | High batch efficiency | Explicit aggregate cap | Yes |
| Phase 5 — Deeper architectural improvements | P1 | Profile-guided exact structural `GameCopier` replacement with shadow equivalence | Very high | Very high | Potentially very high in simulation | Likely lower per copy; development instrumentation higher | Yes |
| Phase 5 | P2 | Ordered replacement type/layer registry | High | Very high | High only if discovery remains dominant | One/few references per replacement | Yes; do not start without profile |
| Phase 5 | P3 | Incremental static-effect recomputation backed by comprehensive mutation versioning | Very high | Very high | Potentially high in static boards | Potentially substantial/bounded | Yes; long-term only |
| Phase 5 | P3 | In-process parallel independent games after global-state removal | Very high | Very high | High throughput, but process model may remain preferable | Multiple games in one heap | Yes |

The gates matter: do not start Phase 3 because a machine has many cores, and do not start replacement/static registries before Phase 2 profiles show they remain dominant. Reprofile after each major work-elimination change because the hotspot distribution will move.

### Phase 0 and 1 outcomes

| Row | Outcome |
|---|---|
| P0 Pin fixtures/seeds/JVM; decision metrics, JFR events, traces, dashboards | **Done** (phase 0): `PerfProbe`, `PerfReport`, `DecisionTraceWriter`, `GameStateDigest`, `JfrPerfSink`, `forge bench` |
| P0 Reproduce PR #11366 and #11160 evidence on current source | **Not done.** A timing run needs a machine that is not sharing CPU, which §11.2 is explicit about; the container this work was done in is not one. The runbook is in the phase 1 doc. #11366 additionally cannot be reproduced until its `CardState` cache exists, which is phase 2 P0 |
| P0 Remove or make pure the forced-attacker futures; timeout regression test | **Done.** Serial in attacker order; covered by a repeat-declaration determinism test |
| P1 Pass root `Score` into `GameSimulator` | **Done, narrowed to one candidate's branches.** The whole-decision form failed its shadow test — see §10.1 |
| P1 Add `hasAtLeastCandidates` and migrate threshold-only callers | **Done.** Four callers migrated |
| P1 Per-sort decision facts with exact ordered-output test | **Done.** `SortFacts`, shared across both ordering passes |
| P2 Reuse one structural adjusted cost within `canPayCost` | **Done, guarded.** Declined where the two adjustments can see a different `castFrom`, or on `NumTimes` |
| P2 Replace per-decision OS thread with lifecycle executor | **Done, as a shared pool.** Per-controller ownership is not viable — see §3.6 |
| (§3.4) No-allocation traversal where the result is not retained | **Deferred to phase 2.** No phase 1 row here, and §4.1 gates it on an allocation profile |

Parity for every shipped item was checked as exact trace identity against the merge base, on a
conventional full-board turn and a full-simulation multi-branch turn: ordered decision traces and
the final canonical state digests were byte-identical.

## 13. Expected overall improvement

There is no defensible aggregate speedup number for the pinned revision without a build and fresh baseline. Performance is also mode- and state-dependent: baseline-score reuse affects full simulation but not ordinary heuristic turns; combat work affects token battles; trigger/static indexing affects their respective board states.

- **Conservative outcome:** lower allocation and tail latency from exact work elimination (baseline score reuse, threshold target queries, sort/cost facts), plus removal of unsafe task behavior. Simple turns should not regress because the adaptive policy keeps them serial.
- **Plausible outcome:** trait-view caching reproduces a material fraction of the 2.15× historical seeded result in broad AI/rules workloads; simulation-specific changes materially reduce complex target/full-simulation decisions; combat batching reduces token-board scaling constants. This is a hypothesis until the specified corpus is run.
- **Upper bound:** isolated top-level branches can approach the parallel fraction's core limit only when there are several similarly expensive branches, copies fit within the memory budget, and the remaining serial rules/copy/reduction fraction is small. Amdahl's law, GC bandwidth, uneven branches, and recursive serial work make linear scaling across all cores unrealistic. No numeric upper-bound factor should be published before the worker sweep supplies the serial fraction and memory curve.

The highest-value order is therefore: measure; merge a rigorously invalidated trait cache; remove repeated root scoring and other exact redundancy; reduce combat/cost/target allocation; then isolate RNG and mutable branch state for deterministic top-level parallel simulation. More RAM and cores are useful only after those work-elimination and correctness foundations are in place.
