# FRL-02K — Confirmation Attribution and Semantic Boundary Audit

Status: AUDIT ONLY. No FRL-02K production adapter is approved or implemented.

Audit date: 2026-08-10

Repository: `chrismaghuhn/forgeAI`

Audit worktree: `C:\forgeAI-confirmation`

Branch: `frl/02k-confirmation-boundary`

Architecture authority: `docs/AI-ML DOCS/ML_STRATEGY.md`

Determinism and safety authority: `docs/AI-ML DOCS/FRL_02K0_DETERMINISM_GATE_REPORT.md`

Primary recommendation after A2: `NO_SAFE_V0_YET`

The cleanest future candidate is `OPTIONAL_TRIGGER_NO_COST`, but it is not ready for implementation. Its callback semantics are clean only after the engine-owned seam, trigger provenance, public triggering-object context, static/delayed exclusions, and fail-closed visibility rules are specified and tested.

## 0. Checkpoint and scope

The original A1 checkpoint and the A2 rebase are both retained here. The A1 audit started from the K0 base; A2 rebased the unpushed audit branch onto the docs-only `origin/master` update without changing the other worktrees.

| Check | Result |
|---|---|
| Expected branch | `frl/02k-confirmation-boundary` |
| Actual branch | `frl/02k-confirmation-boundary` |
| A1 audit base | `c8835a22bf3de062980c368b4a9d55a1fc6d47b4` |
| A2 pre-rebase HEAD | `b4d6f4abf578d8727b681c09ed83b1edcf2cab92` |
| A2 `origin/master` | `266f44a7cae8f9cc7379a8429a137c5fc7c483bb` |
| A2 rebased HEAD before new work | `7c2ae5e2770832d7c21f1a9f3b58669d105d6681` |
| Starting worktree | clean |
| Starting `git diff --check` | pass |
| Production implementation changes at start | none |

`origin/master` no longer equals the historical checkpoint: it is `266f44a7cae8f9cc7379a8429a137c5fc7c483bb`, one separate docs-only commit (`docs: update ML strategy`) ahead of `c8835a22...`. That commit changes only `docs/AI-ML DOCS/ML_STRATEGY.md`. A1 intentionally did not absorb that remote drift; A2 rebased the isolated audit branch onto the exact docs-only commit. The original `C:\forgeAI` checkout and `C:\forgeAI-determinism-gate` worktree were not touched.

This audit does not add `DecisionType.CONFIRMATION`, `DecisionRequest.CONFIRMATION`, `LegalCandidate`, `ConfirmationContext`, `ConfirmationDecisionProvider`, `ConfirmationAdapter`, or any other production boundary. A2 adds only focused test-only evidence; production implementation remains absent.

Evidence labels used below:

- `[BESTAETIGT]` — directly established by current source or a passing focused test.
- `[STARKES INDIZ]` — reproducible runtime or source evidence that still depends on a boundary assumption.
- `[UNKLAERT]` — current source/workload does not establish the requested historical or runtime fact.
- `[BLOCKER]` — must be resolved before an adapter can be implemented safely.

## 1. Executive decision

The original homogeneous proposal is rejected by the current source:

```text
ConfirmationAdapter.TRIGGER
mandatory: [ACCEPT] forced
optional:  [ACCEPT, DECLINE]
```

The callback named `confirmTrigger` contains at least four different cases:

1. Ordinary mandatory triggers do not call `confirmTrigger`; the engine resolves them.
2. Optional no-cost triggers do call it, and the boolean maps directly to proceed versus skip.
3. Cost-bearing optional triggers normally use `confirmTrigger` procedurally to enter cost handling; the actual decline can be expressed by failing or declining the later cost payment path. A generic second `ACCEPT/DECLINE` request would overlap that rule.
4. `PlayerControllerAi.chooseContraptionsToCrank` directly invokes the same callback on a newly constructed `WrappedAbility` as an AI helper. A generic controller-method instrumentation point is therefore not semantically authoritative.

The controlled workloads produced 26 `confirmTrigger` entries in the reactive matchup and zero in the proactive matchup, but the 26 entries are not all equivalent policy decisions. The correct next architectural step is to define and test a narrow engine-owned optional no-cost trigger seam. Until that is complete, the audit verdict is `NO_SAFE_V0_YET`.

## 2. Current runtime callback attribution

### 2.1 Workloads

Each workload ran in a fresh JVM with the packaged desktop jar:

| Workload | Seed | Games | Result |
|---|---:|---:|---|
| Izzet Guild Kit vs Dimir Guild Kit | `20260810` | 10 | completed; Izzet 3, Dimir 7 |
| Dead and Alive vs Air Forces | `20260809` | 10 | completed; Dead and Alive 7, Air Forces 3 |

The exact method attribution used narrow audit-only JDI breakpoint counting. It did not change production classes or diagnostics. `PlayerController.confirmAction` is a convenience overload that delegates to the abstract AI callback; it is not counted as a second decision.

### 2.2 Exact callback counts

| Callback | Reactive | Proactive | Agent-required? | Semantic classification |
|---|---:|---:|---|---|
| `confirmAction` | 8 | 0 | Yes, when the caller represents a player choice | Heterogeneous caller-owned choices; not one confirmation domain |
| `confirmPayment` | 0 | 0 | Yes if reached | Payment decision; heterogeneous cost-part lifecycle and existing AI unsupported path |
| `confirmTrigger` | 26 | 0 | Only for optional no-cost trigger cases | Mandatory cases are engine-owned; cost-bearing cases are procedural/overlapping with payment |
| `confirmBidAction` | 0 | 0 | Yes if a bid is active | Bid-specific decision, followed by bid amount selection |
| `confirmReplacementEffect` | 0 | 0 | Yes if an optional replacement is active | Replacement-specific decision |
| `confirmStaticApplication` | 0 | 0 | Yes if a static application choice is active | Static application or alternative damage assignment |
| `chooseBinary` | 2 | 0 | Yes when the effect is a real binary choice | Domain-specific choices such as tap/untap or odds/evens |
| `chooseFlipResult` | 0 | 0 | Yes only if the external controller owns that result | Coin-flip result choice, not trigger confirmation |
| `payCostToPreventEffect` | 5 | 24 | Yes if the policy owns prevention payment | Payment/prevention decision, not generic confirmation |
| `payCostDuringRoll` | 0 | 0 | Yes if a reroll/payment choice is exposed | Roll-specific payment decision |
| `payCombatCost` | 0 | 0 | Yes if an attack/block cost is exposed | Combat declaration/payment decision |

The 26 reactive `confirmTrigger` calls all had `forge.game.trigger.WrappedAbility.resolve` as their immediate caller in the audit run. That is useful evidence for the normal optional-trigger path, but it does not erase the helper call site present in source or prove that all 26 have identical trigger semantics.

The existing CSV diagnostic field `downstream_callback_family` is not sufficient for this audit. The current implementation records the aggregate `CONFIRMATION` family only from `PlayerControllerAi.confirmAction` and `PlayerControllerAi.confirmPayment`, and only while a priority continuation is active. It does not record `confirmTrigger`, bid, replacement, static, binary, flip, or cost-payment callbacks as that family.

## 3. Complete confirm-like API inventory

The inventory is classified by caller semantics, not method name.

| API | Real callers / caller family | True means | False means | Are both legal? | Direct player decision? | Decision ownership | Controlled count |
|---|---|---|---|---|---|---|---:|
| `PlayerController.confirmAction(...)` | Many effect classes, including `ChangeZoneEffect`, `DiscardEffect`, `ChooseCardEffect`, `DiscoverEffect`, `SacrificeEffect`, `RollDiceEffect`, `SetStateEffect`, and alternate-destination effects | Proceed with the caller’s local branch or accept an optional operation | Decline, cancel, skip, or choose the caller’s alternate branch | Yes, but meaning depends on caller and mode | Sometimes | Caller-specific; often a different decision type | AI implementation: 8 reactive, 0 proactive |
| `confirmPayment(...)` | `PlaySpellAbility` cost branches; `HumanCostDecision` | Pay/accept the current cost part | Stop or decline the current payment branch | Yes where the cost part is optional | Yes when reached | `PAYMENT` / cost-part semantics | 0 / 0 |
| `confirmTrigger(...)` | `WrappedAbility.resolve`; AI helper `chooseContraptionsToCrank` | For optional no-cost resolution, allow the effect; for cost-bearing human flow, enter cost handling | For optional no-cost resolution, suppress the effect; for a mandatory wrapper, the callback is not reached | Not for normal mandatory wrappers; yes for optional paths | Only in the authoritative optional path | Future narrow trigger boundary; current method is mixed | 26 / 0 |
| `confirmBidAction(...)` | `BidLifeEffect` bidding loop | Continue bidding | Stop bidding | Yes | Yes, but not generic confirmation | `BID` plus `chooseNumber` | 0 / 0 |
| `confirmReplacementEffect(...)` | `ReplacementHandler` | Apply the optional replacement | Leave the event unreplaced | Yes | Yes | `REPLACEMENT` | 0 / 0 |
| `confirmStaticApplication(...)` | `Combat` alternative damage assignment; `StaticAbilityManaConvert`; `StaticAbilitySurveilNum` | Apply the static/alternative assignment choice | Do not apply it or use the alternate result | Yes in the relevant effect | Yes where the effect asks | `STATIC_APPLICATION` or combat-specific choice | 0 / 0 |
| `chooseBinary(...)` | `ChooseDirectionEffect`, `ChooseEvenOddEffect`, `CountersPutOrRemoveEffect`, `FlipCoinEffect`, `RollDiceEffect`, `TapOrUntapEffect`, `TapOrUntapAllEffect`, `TimeTravelEffect`, `Untap` | Select the first/domain-specific binary value | Select the second/domain-specific value | Yes | Yes, but not generic confirmation | Effect-specific decision type | 2 / 0 |
| `chooseFlipResult(...)` | `FlipCoinEffect` | Select heads/result according to the API contract | Select the opposite result | Yes | Yes only if the game mode exposes this choice | Flip-result decision | 0 / 0 |
| `payCostToPreventEffect(...)` | `AbilityUtils`, `SacrificeEffect` | Pay to prevent the effect | Do not pay; let the effect happen | Yes | Yes | `PAYMENT` / prevention | 5 / 24 |
| `payCostDuringRoll(...)` | `RollDiceEffect` | Pay for the roll/reroll branch | Do not pay | Yes | Yes | Roll/payment | 0 / 0 |
| `payCombatCost(...)` | `CombatUtil` attack/block cost paths | Pay and permit the combat declaration | Do not pay; declaration fails | Yes | Yes | Combat/payment | 0 / 0 |

### 3.1 `confirmAction` modes are not one domain

The current `PlayerActionConfirmMode` values are `Random`, `FromOpeningHand`, `ChangeZoneToAltDestination`, `ChangeZoneFromAltSource`, `ChangeZoneGeneral`, `BidLife`, `OptionalChoose`, `Tribute`, and `AlternativeDamageAssignment`. The current direct-call inventory shows no active call site for `FromOpeningHand`; the other modes have different legal-state and observation requirements.

Examples:

- `Random` is used by caller code such as discard or random state transitions; it is not an agent choice in every caller.
- `OptionalChoose` can accept or cancel a local selection process.
- `ChangeZoneToAltDestination`, `ChangeZoneFromAltSource`, and `ChangeZoneGeneral` choose among zone movement branches.
- `Tribute` decides a sacrifice/tribute branch.
- `AlternativeDamageAssignment` is a combat assignment decision, not a trigger confirmation.
- `BidLife` is only part of bidding and is followed by a numeric choice.

A future adapter must therefore include the caller’s semantic mode and context, or remain split by domain.

### 3.2 Payment APIs are not a single confirmation API

`PlaySpellAbility.payCostDuringAbilityResolve` invokes `confirmPayment` for several cost-part branches. Other cost parts, including `PayLife`, flow through `CostDecisionMakerBase` and `CostPayment` without a direct `confirmPayment` call. `AiController.confirmPayment` currently throws `UnsupportedOperationException` because the AI is not expected to reach that implementation in its current path. A generic `CONFIRM_PAYMENT` adapter would therefore be both incomplete and capable of duplicating cost-decision semantics.

## 4. Historical `CONFIRMATION` reconciliation

The prior observation was:

```text
reactive CONFIRMATION = 70
proactive CONFIRMATION = 0
```

### Confirmed current-head facts

- `PlayerControllerAi.confirmAction` records `DownstreamCallbackFamily.CONFIRMATION`.
- `PlayerControllerAi.confirmPayment` records the same aggregate family.
- `PlayerControllerAi.confirmTrigger` does not record that family.
- The current fresh reactive workload produced 8 `confirmAction` entries and 0 `confirmPayment` entries.
- The same workload produced 26 `confirmTrigger` entries, which were invisible to the aggregate `CONFIRMATION` family.
- The fresh proactive workload produced zero entries for both `confirmAction` and `confirmPayment`, as well as zero `confirmTrigger` entries.

### Historical limitation

`[UNKLAERT]` The exact split of the historical 70 cannot be reconstructed from the current repository. The historical raw callback trace or per-method counter is not present, and the current aggregate diagnostic intentionally collapses `confirmAction` and `confirmPayment` while omitting `confirmTrigger`. Therefore the audit does not claim that the old 70 was all `confirmAction`, all `confirmPayment`, or any amount of `confirmTrigger`.

The strongest current-head equivalent is:

```text
historical exact split: unknown
current-head equivalent in the fresh reactive workload:
    confirmAction  = 8
    confirmPayment = 0
    confirmTrigger = 26, excluded from aggregate CONFIRMATION
```

The old 70 is not evidence that trigger confirmations were the important controlled-slice boundary.

## 5. `TriggerHandler` construction truth table

The relevant construction is `TriggerHandler.runSingleTriggerInternal`:

1. It obtains the registered trigger host and overriding ability.
2. It sets the activating player/controller and trigger parameters.
3. It first checks `OptionalDecider`.
4. Otherwise it classifies the ability as mandatory when it is an `AbilitySub`, has no `Cost`, has a mandatory parsed cost, or has `Cost == "0"`.
5. The remaining cost-bearing cases are optional and use the activating player as decider.
6. It constructs `WrappedAbility(regtrig, sa, decider)`.
7. Static triggers go to `playTrigger` immediately; normal triggers go onto the simultaneous/normal stack path.

`SpellAbility.isOptionalTrigger()` reads the ability’s optional flag, and `WrappedAbility.isMandatory()` is derived from that wrapped ability. The local `isMandatory` passed to `playTrigger` is a separate local classification. They agree for normal mandatory triggers, but they must not be treated as the same variable for all paths.

| Trigger shape | `isOptionalTrigger` / wrapper flag | `wrapper.isMandatory()` | `decider` | `confirmTrigger` reached? | `playTrigger` lifecycle | Later cost decision? |
|---|---|---|---|---|---|---|
| Plain mandatory trigger: no `OptionalDecider`, no cost | false | true | null | No in normal `WrappedAbility.resolve` | Normal trigger is queued and later resolved; static variant uses immediate `playTrigger` | No |
| `OptionalDecider` trigger, no cost | true | false | Defined decider player | Yes when wrapper resolves; exactly once for the focused optional fixture | Normal trigger is queued; static variant enters immediate `playTrigger` | No same accept/decline payment |
| `Cost == "0"`, no `OptionalDecider` | false because the mandatory branch is reached | true | null | No | Normal mandatory or static mandatory lifecycle | No meaningful cost payment |
| `Cost == "0"`, with `OptionalDecider` | true because `OptionalDecider` wins the branch order | false | Defined decider player | Yes | Optional normal/static lifecycle | No nonzero payment |
| Nonzero cost, no `OptionalDecider` | true | false | Activating player after any trigger-controller rewrite | Yes procedurally before cost resolution | Normal trigger is queued; static variant uses immediate `playTrigger` | Yes, depending on the cost-part implementation |
| `AbilitySub`, no `OptionalDecider` | false because `AbilitySub` satisfies the mandatory branch | true | null | No in normal wrapper resolution | Normal mandatory or static mandatory lifecycle | No trigger-level optional payment |
| `AbilitySub` with `OptionalDecider` | true because the `OptionalDecider` branch is checked first | false | Defined decider player | Yes | Optional lifecycle; separate from the ordinary `AbilitySub` case | Depends on the ability cost |
| Static trigger, mandatory classification | Usually false/mandatory when the mandatory branch applies | true | null | No | `playTrigger(host, wrapper, isMandatory)` immediately | No |
| Static trigger, optional classification | true | false | Defined/activating decider | Technically yes because controller `playTrigger` executes the wrapper; not the normal queued-stack lifecycle | Immediate `playTrigger`, with controller-specific preselection and no-stack execution | Depends on ability |
| Delayed trigger when dequeued | Recomputed through the same `runSingleTriggerInternal` rules | Recomputed from the wrapped ability | Recomputed from `OptionalDecider` or activating player | Same as its recomputed classification | Delayed registration changes timing/provenance, not callback type | Depends on ability |
| Player-defined delayed trigger | Recomputed through the same construction | Recomputed | Recomputed | Same as its recomputed classification | Separate delayed list; provenance must be preserved before any adapter | Depends on ability |
| Trigger controlled by another player | Depends on the trigger’s optional/cost parameters | Depends on wrapped SA optional flag | `TriggerController` may replace activating player; `OptionalDecider` or resulting activating player identifies the decider | Only if optional classification reaches wrapper resolution | Same static/normal distinction | Depends on ability |

### Important branch-order result

`Cost == "0"` is not independently synonymous with optionality. It is a mandatory classification only when the earlier `OptionalDecider` branch did not already mark the ability optional. The string representation must not be used outside this existing classification logic.

## 6. Mandatory-trigger proof

### Source proof

For a normal mandatory no-cost trigger:

```text
no OptionalDecider
→ no cost / mandatory branch
→ isMandatory = true
→ decider = null
→ WrappedAbility(decider = null)
→ WrappedAbility.resolve skips confirmTrigger
→ playSpellAbilityNoStack resolves the effect
```

`WrappedAbility.resolve()` calls `decider.getController().confirmTrigger(this)` only inside `if (decider != null)`. The `Bitterblossom` fixture is a mandatory upkeep trigger with no `OptionalDecider` and a token-producing effect.

### Focused test proof

`forge-gui-desktop/src/test/java/forge/ai/ability/FRL02KConfirmationAuditTest.java` runs the real `TriggerHandler` phase path and then resolves the simultaneous wrapper with a test-only counting controller:

```text
mandatory Bitterblossom trigger:
    confirmTrigger calls = 0
    trigger effect resolves
    player life changes from 20 to 19
    token enters the battlefield
```

The test controller overrides only test behavior and does not change production classes. Its direct resolution override is intentional: it prevents the AI preselection heuristic from suppressing the fixture before the wrapper boundary is exercised.

### Verdict

**Does normal mandatory trigger resolution invoke `confirmTrigger`? NO.**

Overall `confirmTrigger` usage is **MIXED** because optional and helper/static paths exist, but a normal mandatory wrapper is engine-owned and must not receive a synthetic forced confirmation request.

## 7. Optional no-cost trigger proof

`Luminous Angel` is a real optional no-cost fixture:

```text
T:Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You
  | OptionalDecider$ You | Execute$ TrigToken
```

The focused test executes the real trigger construction and wrapper resolution twice:

| Policy return | `confirmTrigger` calls | Effect |
|---|---:|---|
| `true` | exactly 1 | token effect resolves |
| `false` | exactly 1 | token effect does not resolve |

There is no later payment decision for the no-cost token effect. This is the strongest direct boolean mapping found in the audit:

```text
optional no-cost trigger
→ confirmTrigger exactly once
→ true: resolve effect
→ false: skip effect
```

This establishes the semantic candidate `OPTIONAL_TRIGGER_NO_COST`, not an implementation approval. The future seam still needs to exclude mandatory, cost-bearing, static, delayed, and unsupported/hidden contexts.

## 8. Cost-bearing optional trigger proof

`Bringer of the Black Dawn` provides a real optional trigger whose executed ability has `Cost$ PayLife<2>`:

```text
optional upkeep trigger
→ change-zone/tutor ability with nonzero PayLife cost
```

The human implementation explicitly handles this case in `PlayerControllerHuman.confirmTrigger`:

```text
if (sa.hasParam("Cost") && !sa.getParam("Cost").equals("0")) {
    return true;
}
```

The source comment states that the trigger may proceed because the player can decline by not paying the cost later. The subsequent `PlaySpellAbility` / `CostPayment` path evaluates the cost. For a `PayLife` cost, the decision is made through the cost decision machinery rather than by a second generic trigger accept/decline prompt. For other cost parts, `confirmPayment` can be reached from `payCostDuringAbilityResolve` or `HumanCostDecision`.

The existing `TriggerLifeGateTest` exercises the same real Bringer trigger definition through the AI cost-safety path:

```text
life 20: AI accepts the life-cost trigger
life 5:  AI declines because paying 2 would cross the safety threshold
```

This test proves the cost-bearing trigger is not equivalent to a no-cost boolean trigger. The exact cost callback depends on the cost part, so it is incorrect to expose both a generic `confirmTrigger` choice and a generic `PAYMENT` choice for the same rule choice.

### Cost-bearing verdict

For a cost-bearing optional trigger, the policy shape is:

```text
confirmTrigger is procedural / enters optional-cost resolution
→ cost decision machinery decides whether the cost is paid
→ payment success resolves the effect
→ payment failure or decline prevents that effect
```

The exact branch is cost-part-specific. `PayLife` does not directly use `confirmPayment`; other cost parts can. Therefore the correct audit conclusion is **not** `CONFIRMATION → PAYMENT` as a universal pair. It is **procedural trigger entry → cost-specific decision**, with the future adapter boundary deferred until payment ownership is separately designed.

## 9. `confirmTrigger` call-site audit

Every direct production call found is classified below.

| Call site | Category | Evidence and consequence |
|---|---|---|
| `forge.game.trigger.WrappedAbility.resolve()` | `REAL_TRIGGER_RESOLUTION` | The engine calls the decider controller only when `decider != null`; this is the authoritative ordinary wrapper seam. |
| `forge.ai.PlayerControllerAi.chooseContraptionsToCrank()` | `AI_HEURISTIC_HELPER` | It constructs `new WrappedAbility(crankTrigger, crankTrigger.getOverridingAbility(), player)` and calls `confirmTrigger` while choosing contraptions. This is not proof of a later stack trigger resolution. |
| `PlayerControllerHuman.confirmTrigger()` | Implementation | UI/auto-yield policy, plus the special nonzero-cost procedural rule and visibility-aware prompt construction. |
| `PlayerControllerAi.confirmTrigger()` | Implementation | Mandatory wrappers return true; optional wrappers delegate to `brains.doTrigger`; target state is temporarily saved/restored. |

No separate production `confirmTrigger` call site was found in another controller utility. The helper call is sufficient to rule out unqualified instrumentation directly in `PlayerControllerAi.confirmTrigger()`.

## 10. Recommended instrumentation seam

### Option A — instrument `PlayerControllerAi.confirmTrigger`

**Rejected as the primary seam.** It is easy to count, but it mixes the authoritative wrapper call with `chooseContraptionsToCrank` helper calls. It is also AI-specific, so Human/AI parity and future external-controller replacement are poor. It cannot prove that a request is created immediately before the engine consumes the result.

### Option B — instrument `WrappedAbility.resolve()` around the decider call

**Recommended engine-owned seam for the next milestone.** The narrow region is the existing branch:

```text
if (decider != null) {
    ...
    boolean proceed = decider.getController().confirmTrigger(this);
    if (!proceed) return;
}
```

The future capture must be conditional on an explicit classification such as:

```text
wrapper is optional
AND trigger is a normal supported trigger lifecycle
AND the wrapped ability has no nonzero cost
AND public context encoding succeeds
```

This gives Human/AI/external-controller parity, excludes the helper call, keeps request-before-result timing next to the native callback, and lets `ActionContinuation` integrity be checked at the engine boundary. It also allows the callback to be replaced later without moving the semantic capture.

### Option C — another narrower engine-owned seam

A future trigger-specific adapter seam could be placed in `WrappedAbility.resolve` after the engine has established `decider != null` and before the controller callback. It must not be a generic `PlayerController` method wrapper or a description/UI layer. Static and delayed triggers should enter separate explicitly named lifecycle gates until their context and ordering are proven.

### Recommendation

Use **Option B**, with an explicit normal optional no-cost predicate and a fail-closed context encoder. Do not implement that seam in this audit.

## 11. Trigger identity audit

`Trigger.getId()` is not a canonical cross-run/training identity.

### Current behavior

- `Trigger.maxId` is a mutable static allocation counter.
- `Trigger.resetIDs()` resets it to `50000`.
- A normal non-LKI `Trigger.copy()` allocates a new ID.
- An LKI copy can retain the original ID.
- `equals` and `hashCode` use the ID.
- Generated, copied, delayed, and player-defined triggers can therefore have allocation-dependent identities.

This is useful for current in-memory equality but not stable across independent games, card-load order, trigger copying, or training runs.

`Card` has internal game identity based on per-game card ID and game timestamp. That can distinguish live public instances inside a game, but it is not by itself a perspective-safe observation and its raw values are not cross-run semantic identity.

### Required future semantic identity

A supported trigger context needs a structured identity containing, at minimum:

1. A perspective-visible source/host instance identity. For training, this must be an externally defined public instance identity or a run-normalized public identity, not a raw `Trigger.id`.
2. A stable trigger-definition ordinal or normalized definition hash within the host card/card-state definition.
3. Trigger mode and provenance class: intrinsic card trigger, static effect, delayed trigger, player-defined delayed trigger, copied/generated trigger, or another explicit class.
4. An occurrence discriminator for simultaneous instances when the same definition fires more than once.
5. Public triggering-object identities required by the decision.

The canonical identity must not be a localized description, `Trigger.toString()`, Java object identity, or `hashCode`.

## 12. Triggering-object context audit

`WrappedAbility.getTriggeringObjects()` delegates to the wrapped `SpellAbility` map. The map is keyed by `AbilityKey` and may contain objects such as:

- `Player` for phase and player-scoped triggers;
- attacking, blocking, attacked, defender, and defending-player objects;
- `Card` and card collections;
- `SpellAbility` and cause/source objects;
- damage source/target and zone-change/LKI objects;
- generated or remembered objects from the trigger definition.

Source card identity alone is therefore insufficient. One source card can carry several trigger definitions, and one definition can have simultaneous occurrences with different triggering objects.

### Safe `OPTIONAL_TRIGGER_NO_COST` v0 shape candidate

The only context shapes that can enter a future v0 candidate are those where all decision-relevant data is:

- visible to the decider under the normal `CardView.canBeShownTo` rules;
- limited to a known source card, stable definition identity, and public `Player`/`Card` instance identities;
- free of raw hidden `SpellAbility`, LKI, or opaque remembered-object identity;
- finite and explicitly typed rather than serialized by `toString()`;
- complete enough that two strategic alternatives cannot alias to the same neutral observation.

The Luminous Angel upkeep trigger is a minimal example: the source is a visible battlefield card and the triggering player is public. A simple public attack trigger could be supported only after attacker/defender object encoding is explicitly defined.

### Unsupported

The following must fail closed until a dedicated public encoding is proven:

- hidden-zone or face-down source cards;
- triggering cards in an opponent’s hand or library;
- face-down exile or face-down permanent identity not visible to the decider;
- raw `SpellAbility` or LKI objects whose public fields are not proven sufficient;
- remembered/generated/copied triggers without stable provenance;
- object collections containing a mixture of public and hidden members;
- any shape where source identity alone would alias distinct trigger occurrences.

No universal serializer is safe for this milestone.

## 13. Hidden-information and source-visibility result

The Human prompt provides a useful visibility rule: a triggering card is shown only when the decider controls it, its zone is unknown/public, or the zone is otherwise known. `CardView.canBeShownTo` separately hides library cards, opponent hand cards, and face-down objects.

### Safe for a future v0 adapter

- ordinary visible battlefield source card controlled by or publicly known to the decider;
- public stack, graveyard, command-zone, or other known-zone source where the source identity is legally visible;
- public player triggering object;
- explicit public card triggering object where the object and its relevant zone are visible;
- a trigger definition with a stable semantic ordinal/provenance mapping.

### Unsafe / fail closed

- source card in an opponent’s hand or library;
- face-down permanent or face-down exile when identity is not visible;
- a triggering card whose identity is hidden from the decider;
- `SpellAbility` fields that expose the originating card or hidden target;
- delayed/generated/copy trigger source where provenance cannot be reconstructed without leaking hidden identity;
- hidden choices or hidden selection results.

This audit does not assume that every trigger has an ordinary visible source card. Emblems, static effects, immutable effect cards, command-zone objects, copied abilities, generated triggers, and delayed triggers need an explicit source/provenance class. If that class cannot be represented without hidden information, the context is `UNSUPPORTED`.

## 14. `ActionContinuation` audit

`ActionContinuation` is scoped to a selected priority action. `PriorityActionDiagnostics.beginAction` installs it around `playChosenSpellAbility`, and `endAction` removes it in a `finally` block. Downstream callbacks are recorded only while that active priority action remains on the thread. Raw payment callbacks can be emitted without continuation fields.

The intended lifecycle is:

```text
priority action announced
→ action may cause a trigger
→ trigger is queued/placed on the stack
→ original priority action returns
→ continuation is closed
→ later trigger resolves
→ no continuation belongs to the trigger decision
```

The current source proves the begin/end scope. The fresh method-attribution harness did not alter diagnostics or read the private continuation state, so a direct runtime null sample at the trigger seam is not claimed here. `[BLOCKER]` The future engine-owned seam must assert that ordinary trigger resolution sees no active `ActionContinuation`; a non-null value is an integrity warning/regression unless a separately proven valid lifecycle exists.

The future trigger context must not inherit the action continuation merely because the trigger was caused by the action.

## 15. Static and delayed trigger distinction

Static triggers are not interchangeable with ordinary queued stack triggers.

`TriggerHandler.runSingleTriggerInternal` sends static triggers directly to `playTrigger(host, wrapper, isMandatory)`, then runs the static-resolution trigger event. Nonstatic triggers are added to the simultaneous/normal stack path and resolve later through the stack.

The source also shows that Human and AI `playTrigger` implementations execute the wrapper, so an optional static wrapper can technically reach `WrappedAbility.resolve` and `confirmTrigger`. Current card data contains optional static examples, including `Inquisitor Eisenhorn`, `God-Eternal Kefnet`, and `Snowfall`. This makes “static never calls confirmTrigger” incorrect.

However, static execution has a different timing and controller-preselection lifecycle. No static optional occurrence was counted in the two controlled workloads. Static triggers are therefore a separate lifecycle for the next design and are deferred from an `OPTIONAL_TRIGGER_NO_COST` v0 unless a dedicated fixture establishes public context, ordering, continuation state, and exactly-once callback behavior.

Delayed, this-turn-delayed, and player-defined delayed triggers are registered in separate handler lists and later passed back through the same trigger construction method. They do not introduce a new callback type, but they do introduce provenance and timing concerns. A delayed trigger is not safe for v0 merely because its eventual wrapper has `decider != null`; its source and triggering objects must remain publicly and stably identifiable. Unsupported delayed/generated/copy forms fail closed.

## 16. Auto-yield behavior

`AutoYieldStore.TriggerDecision` has `ASK`, `ACCEPT`, and `DECLINE`, with game/match/session persistence. Human `confirmTrigger` consults this store before normal UI prompting.

This is UI automation state, not game-rule legality. It must not be exported as neutral observation or treated as evidence that a policy chose `ACCEPT` or `DECLINE`. A future external controller can replace the UI decision while keeping auto-yield as a local execution optimization. The existing `WrappedAbility` yield key is description/host based and is not a canonical training identity.

## 17. Confirmation-v0 candidate matrix

Counts below are exact callback counts from the current fresh workloads, not aggregate diagnostic-family counts.

| Candidate adapter | Reactive / proactive frequency | Semantic cleanliness | Direct boolean mapping | Hidden-info risk | Payment overlap | Context complexity | RandomLegalPolicy relevance | Recommendation |
|---|---:|---|---|---|---|---|---|---|
| `OPTIONAL_TRIGGER_NO_COST` | 26 / 0 `confirmTrigger` entries; not all 26 proven no-cost | Clean for the explicitly filtered no-cost normal-wrapper subset | Yes: true resolves, false skips | High unless source/object visibility fails closed | None for genuine no-cost subset | High: source, definition, occurrence, objects, provenance | **Yes**; current reactive fallback blocker | Design candidate only; not ready |
| `confirmPayment` | 0 / 0 | Mixed cost-part semantics; AI implementation currently unsupported | Sometimes | Cost context can be hidden/complex | **Direct overlap** with payment lifecycle | High | Yes when reached, but not a safe generic confirmation | Defer to PAYMENT milestone |
| `confirmAction:OptionalChoose` | 0 / 0 | Caller-specific cancellation/selection completion | Yes only per caller | Depends on caller | Usually none, but may lead to payment | Medium/high | Potentially agent-required | Split by caller before adapter |
| `confirmAction:ChangeZone*` | 0 / 0 | Zone-branch choice, not trigger choice | Yes per caller | High for hand/library/exile | May lead to costs | High | Potentially agent-required | Defer |
| `confirmAction:Random` | 0 / 0 | Often engine/random semantics rather than agent choice | Not uniformly | Caller-dependent | Low | Medium | Often not agent-required | Defer |
| `confirmAction:Tribute` | 0 / 0 | Sacrifice/tribute rule branch | Yes per caller | Card-selection context | May lead to payment/effect | High | Agent-required when exposed | Separate decision type |
| `confirmAction:AlternativeDamageAssignment` | 0 / 0 | Combat assignment, not confirmation | Yes per combat mode | Public combat objects but complex | Combat-specific | High | Agent-required when exposed | Separate combat decision |
| `confirmBidAction` | 0 / 0 | Bid continuation followed by numeric bid | Yes, but not complete bid decision | Public bid/life context | No generic payment overlap | Medium | Agent-required when bidding | Separate bid decision |
| `confirmReplacementEffect` | 0 / 0 | Replacement choice | Yes | Replacement source/event context | Possible cost subpaths | High | Agent-required when exposed | Separate replacement decision |
| `confirmStaticApplication` | 0 / 0 | Static/alternative application choice | Yes per caller | Static source context | Caller-dependent | High | Agent-required when exposed | Separate static decision |
| `chooseBinary` / `chooseFlipResult` | 2 / 0 for binary; 0 / 0 flip | Domain-specific effects or result selection | Yes per effect | Usually public, but effect-specific | Sometimes | Medium | Other decision types | Do not fold into confirmation |
| `payCostToPreventEffect` | 5 / 24 | Payment/prevention | Yes | Cost and preventable-event context | Payment | Medium/high | Agent-required payment | Existing PAYMENT domain |

## 18. `RandomLegalPolicy` relevance and blockers

The question is not whether a method returns a boolean; it is whether an external policy must answer it after Forge AI is removed.

| Current path | Classification | Why |
|---|---|---|
| Normal mandatory trigger | `ENGINE_OWNED` | No `confirmTrigger` request exists; the engine resolves it. |
| Optional no-cost normal trigger | `AGENT_REQUIRED` | The external policy must choose proceed/skip; current AI `brains.doTrigger` is the fallback. |
| Cost-bearing optional trigger | `DEFERRED_BUT_BLOCKING` | Trigger entry and payment are coupled but not identical; cost-part-specific policy ownership is unresolved. |
| `confirmAction` generic calls | `AGENT_REQUIRED` only for caller-defined choice modes; otherwise `OTHER_DECISION_TYPE` or engine/random | The method spans many unrelated callers and cannot be removed through one adapter. |
| `confirmPayment` | `DEFERRED_BUT_BLOCKING` | It is agent-required when reached, but current AI and cost-part routing are not a single clean boundary. |
| `confirmBidAction` | `OTHER_DECISION_TYPE` | Bid continuation is not generic confirmation and requires numeric bid ownership. |
| `confirmReplacementEffect` | `OTHER_DECISION_TYPE` | Replacement semantics need their own context. |
| `confirmStaticApplication` | `OTHER_DECISION_TYPE` | Static/combat application is not normal stack trigger confirmation. |
| `chooseBinary` / `chooseFlipResult` | `OTHER_DECISION_TYPE` | These are effect/result decisions with their own domains. |
| `payCostToPreventEffect`, `payCostDuringRoll`, `payCombatCost` | `OTHER_DECISION_TYPE` / `PAYMENT` | Payment and combat/roll semantics must not be represented as generic confirmation. |
| Human auto-yield | `UI_AUTOMATION_ONLY` | Stored UI preference is not a game observation or policy decision. |

For the controlled v0, the exact confirmation-like blockers remaining are:

1. 26 reactive `confirmTrigger` entries, of which only the no-cost normal optional subset is a candidate agent boundary; mandatory and cost-bearing cases must be filtered at the engine seam.
2. 8 reactive `confirmAction` entries, which are real current callbacks but mixed caller-owned semantics and therefore not safely covered by a generic confirmation adapter.
3. No current `confirmPayment` runtime entries in these workloads, but the payment domain remains a future blocker because cost-bearing trigger resolution can reach cost-specific payment logic and the AI implementation is not a complete external policy seam.
4. 5 reactive and 24 proactive prevention-payment callbacks, which are payment decisions rather than confirmation and are outside this adapter.

The proactive workload’s zero confirmation callbacks does not prove that the proactive policy has no future decision work; it only says these callback paths were not reached in this ten-game sample.

## 19. Final implementation recommendation

### Primary verdict

```text
NO_SAFE_V0_YET
```

### Candidate for the next architecture review

```text
OPTIONAL_TRIGGER_NO_COST
```

This candidate should be reconsidered only after the following gates are approved and tested:

1. Capture at the engine-owned `WrappedAbility.resolve` seam, not generic `PlayerControllerAi.confirmTrigger`.
2. Require a normal, nonstatic, supported optional trigger lifecycle for v0.
3. Exclude `wrapper.isMandatory()` and all `decider == null` cases.
4. Exclude any nonzero cost; do not synthesize a second payment/confirmation decision.
5. Encode stable source/definition/occurrence identity and all decision-relevant public triggering objects.
6. Fail closed for hidden source/object data, raw opaque `SpellAbility`/LKI, and unsupported delayed/generated/copied provenance.
7. Assert no active `ActionContinuation` at ordinary trigger resolution.
8. Prove exactly-once request-before-native-result behavior for Human, AI, and an external controller substitute.
9. Add a runtime attribution test showing helper calls cannot enter the adapter.

No adapter or production seam is implemented by this audit.

## 20. Verification and audit artifacts

### Focused audit tests

| Command / fixture | Tests | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `FRL02KConfirmationAuditTest,TriggerLifeGateTest` | 3 | 3 | 0 | 0 | 0 |

`FRL02KConfirmationAuditTest` is test-only and proves the mandatory and optional no-cost wrapper semantics. `TriggerLifeGateTest` is an existing focused fixture for the real Bringer nonzero `PayLife` trigger and proves the AI cost-safety branch in both directions.

### Runtime audit artifacts

- Reactive fresh-JVM workload: 10 games, seed `20260810`, exit 0.
- Proactive fresh-JVM workload: 10 games, seed `20260809`, exit 0.
- Narrow method attribution counted each requested callback independently.
- Existing priority/mulligan diagnostic CSVs were inspected; no aggregate `CONFIRMATION` row was treated as exact trigger attribution.
- Temporary JDI source/output was kept under ignored `target/audit`; it is not production code and is not part of the report/test commit.

### Safety-gate preservation

The post-report verification completed as follows:

| Gate | Result |
|---|---|
| K0/base regression before the audit fixture | 286 tests: 286 passed, 0 failed, 0 errors, 0 skipped |
| Pre-A2 expanded audit selection | 287 tests: 287 passed, 0 failed, 0 errors, 0 skipped; module split `forge-game=12`, `forge-ai=20`, `forge-gui-desktop=255` |
| Post-A2 expanded selection | 288 tests: 288 passed, 0 failed, 0 errors, 0 skipped; one additional A2 test selection |
| Focused A2 boundary plus trigger-life gate | 4 tests: 4 passed, 0 failed, 0 errors, 0 skipped |
| `FullGameCollectorNeutralityTest` | 1 passed, 0 failed/errors/skipped |
| `WorkerIsolationSmokeTest` | 1 passed, 0 failed/errors/skipped |
| `mvn -pl forge-gui-desktop -am -DskipTests package` | `BUILD SUCCESS`; assembled jar and `forge.exe` created |
| Configured Checkstyle lifecycle (`mvn -pl forge-gui-desktop -am -DskipTests validate`) | `BUILD SUCCESS`; 0 violations in every reactor module |
| `git diff --check` | pass |

An exploratory direct `checkstyle:check` invocation was not used as the gate because it bypassed the repository’s configured rules and selected Maven’s default `sun_checks.xml`, producing unrelated legacy errors. The configured lifecycle above is the authoritative Checkstyle result; no source change was made in response to the exploratory output.

## 21. Direct callsite appendix

The following is the direct production callsite inventory used for the classifications above. Controller declarations, overrides, AI strategy methods, and the test-only counting override are excluded from the “real caller” lists.

### `PlayerController.confirmAction`

Direct engine callsites:

```text
forge-game/src/main/java/forge/game/ability/effects/AbandonEffect.java:26
forge-game/src/main/java/forge/game/ability/effects/AlterAttributeEffect.java:40
forge-game/src/main/java/forge/game/ability/effects/AnimateEffect.java:163
forge-game/src/main/java/forge/game/ability/effects/AttachEffect.java:144
forge-game/src/main/java/forge/game/ability/effects/ChangeCombatantsEffect.java:38
forge-game/src/main/java/forge/game/ability/effects/ChangeTargetsEffect.java:61
forge-game/src/main/java/forge/game/ability/effects/ChangeZoneAllEffect.java:75
forge-game/src/main/java/forge/game/ability/effects/ChangeZoneEffect.java:521,561,944,951,977,1214,1542,1688
forge-game/src/main/java/forge/game/ability/effects/CharmEffect.java:242
forge-game/src/main/java/forge/game/ability/effects/ChooseCardEffect.java:158
forge-game/src/main/java/forge/game/ability/effects/CloneEffect.java:111
forge-game/src/main/java/forge/game/ability/effects/ControlExchangeEffect.java:87
forge-game/src/main/java/forge/game/ability/effects/ControlGainEffect.java:154
forge-game/src/main/java/forge/game/ability/effects/CopyPermanentEffect.java:126,217,250
forge-game/src/main/java/forge/game/ability/effects/CopySpellAbilityEffect.java:97
forge-game/src/main/java/forge/game/ability/effects/CounterEffect.java:67
forge-game/src/main/java/forge/game/ability/effects/CountersPutEffect.java:529
forge-game/src/main/java/forge/game/ability/effects/CountersPutOrRemoveEffect.java:80
forge-game/src/main/java/forge/game/ability/effects/CountersRemoveEffect.java:86
forge-game/src/main/java/forge/game/ability/effects/DamageDealEffect.java:182
forge-game/src/main/java/forge/game/ability/effects/DestroyAllEffect.java:80
forge-game/src/main/java/forge/game/ability/effects/DigEffect.java:194,247,431
forge-game/src/main/java/forge/game/ability/effects/DigUntilEffect.java:154,216
forge-game/src/main/java/forge/game/ability/effects/DiscoverEffect.java:86
forge-game/src/main/java/forge/game/ability/effects/DiscardEffect.java:148,166,190
forge-game/src/main/java/forge/game/ability/effects/DrawEffect.java:80
forge-game/src/main/java/forge/game/ability/effects/EndTurnEffect.java:28
forge-game/src/main/java/forge/game/ability/effects/EndureEffect.java:71
forge-game/src/main/java/forge/game/ability/effects/EncodeEffect.java:50
forge-game/src/main/java/forge/game/ability/effects/ExploreEffect.java:82
forge-game/src/main/java/forge/game/ability/effects/FightEffect.java:67
forge-game/src/main/java/forge/game/ability/effects/InvestigateEffect.java:48
forge-game/src/main/java/forge/game/ability/effects/ManaEffect.java:52
forge-game/src/main/java/forge/game/ability/effects/MakeCardEffect.java:44
forge-game/src/main/java/forge/game/ability/effects/MillEffect.java:50
forge-game/src/main/java/forge/game/ability/effects/PeekAndRevealEffect.java:85
forge-game/src/main/java/forge/game/ability/effects/PlaneswalkEffect.java:27
forge-game/src/main/java/forge/game/ability/effects/PlayEffect.java:250
forge-game/src/main/java/forge/game/ability/effects/PumpEffect.java:288
forge-game/src/main/java/forge/game/ability/effects/RearrangeTopOfLibraryEffect.java:108
forge-game/src/main/java/forge/game/ability/effects/RepeatEffect.java:114
forge-game/src/main/java/forge/game/ability/effects/RepeatEachEffect.java:43,172
forge-game/src/main/java/forge/game/ability/effects/RevealHandEffect.java:44
forge-game/src/main/java/forge/game/ability/effects/RollDiceEffect.java:577
forge-game/src/main/java/forge/game/ability/effects/SacrificeEffect.java:41,97,143
forge-game/src/main/java/forge/game/ability/effects/ScryEffect.java:45
forge-game/src/main/java/forge/game/ability/effects/SetStateEffect.java:164
forge-game/src/main/java/forge/game/ability/effects/ShuffleEffect.java:21
forge-game/src/main/java/forge/game/ability/effects/SurveilEffect.java:49
forge-game/src/main/java/forge/game/GameAction.java:1837
```

`forge-gui/src/main/java/forge/player/HumanCostDecision.java` has many private `confirmAction(CostPart, String)` calls. Those are cost-part UI prompts, not `PlayerController.confirmAction`; the private helper delegates to `PlayerController.confirmPayment` at line 1534 and is classified under payment.

### Other requested APIs

```text
confirmPayment:
  forge-game/src/main/java/forge/game/player/PlaySpellAbility.java:175,180,223,238,254,331,337,370,383
  forge-gui/src/main/java/forge/player/HumanCostDecision.java:1534

confirmTrigger:
  forge-game/src/main/java/forge/game/trigger/WrappedAbility.java:435
  forge-ai/src/main/java/forge/ai/PlayerControllerAi.java:306 (AI helper construction/call)

confirmBidAction:
  forge-game/src/main/java/forge/game/ability/effects/BidLifeEffect.java:54

confirmReplacementEffect:
  forge-game/src/main/java/forge/game/replacement/ReplacementHandler.java:328

confirmStaticApplication:
  forge-game/src/main/java/forge/game/combat/Combat.java:733,811,823,832
  forge-game/src/main/java/forge/game/staticability/StaticAbilityManaConvert.java:43
  forge-game/src/main/java/forge/game/staticability/StaticAbilitySurveilNum.java:28

chooseBinary:
  forge-game/src/main/java/forge/game/ability/effects/ChooseDirectionEffect.java:25
  forge-game/src/main/java/forge/game/ability/effects/ChooseEvenOddEffect.java:35
  forge-game/src/main/java/forge/game/ability/effects/CountersPutOrRemoveEffect.java:138
  forge-game/src/main/java/forge/game/ability/effects/FlipCoinEffect.java:207
  forge-game/src/main/java/forge/game/ability/effects/RollDiceEffect.java:207
  forge-game/src/main/java/forge/game/ability/effects/TapOrUntapAllEffect.java:64
  forge-game/src/main/java/forge/game/ability/effects/TapOrUntapEffect.java:69
  forge-game/src/main/java/forge/game/ability/effects/TimeTravelEffect.java:56
  forge-game/src/main/java/forge/game/phase/Untap.java:190

chooseFlipResult:
  forge-game/src/main/java/forge/game/ability/effects/FlipCoinEffect.java:215

payCostToPreventEffect:
  forge-game/src/main/java/forge/game/ability/AbilityUtils.java:1430
  forge-game/src/main/java/forge/game/ability/effects/SacrificeEffect.java:44,67

payCostDuringRoll:
  forge-game/src/main/java/forge/game/ability/effects/RollDiceEffect.java:163,204

payCombatCost:
  forge-game/src/main/java/forge/game/combat/CombatUtil.java:268,331
```

## 22. Audit report conclusion

The normal mandatory-trigger path is engine-owned. The no-cost optional trigger path is the only currently identified clean boolean trigger boundary. Cost-bearing triggers are not ordinary accept/decline decisions, and the generic `confirmTrigger` method is contaminated by an AI helper call. Trigger IDs, descriptions, raw object identity, and universal serialization are not safe training identity mechanisms. Public source/object context and fail-closed visibility rules are mandatory.

This report records an architecture decision only.

**STOP: do not implement the selected adapter yet. Wait for architecture review.**

## 23. FRL-02K-A2 — authority correction and optional-trigger closure

### 23.1 Authority correction

`ML_STRATEGY.md` is now Revision 10. The strategy records the following separate states:

| Milestone | State |
|---|---|
| `FRL-02K0` determinism/safety gate | `PASS` |
| `FRL-02K` attribution audit | `PASS` |
| `FRL-02K` semantic adapter closure | `OPEN` |
| `FRL-02K` production implementation | `OPEN` |

The strategy no longer says that CONFIRMATION is complete or that all boundaries are complete through CONFIRMATION. Its architecture statement is now that CONFIRMATION is not one homogeneous callback family: normal mandatory triggers are engine-owned and produce no request; optional no-cost normal triggers are the candidate future adapter; nonzero-cost optional triggers belong to the cost/payment lifecycle; and action, bid, replacement, static-application, and binary callbacks remain separate semantic families.

The roadmap now sequences `FRL-02K0 PASS` → `FRL-02K attribution audit PASS` → `FRL-02K semantic adapter closure OPEN` → `CONFIRMATION implementation` → `ORDER Attribution Audit` → modern `DAMAGE_ASSIGNMENT` → `Runtime Gap Audit` → `Gap Closure / PAYMENT` → `Zero-Unsupported Gate` → `RandomLegalPolicy`. The Revision-9 discoveries about ORDER, the modern DAMAGE_ASSIGNMENT information barrier, PAYMENT being PARTIAL, DECISION_TRACE_V2 being closed, teacher-label coverage being PARTIAL, and the zero-unsupported gate are preserved.

### 23.2 Evidence-count correction

The labels are deliberately separated:

| Evidence label | Test count | Meaning |
|---|---:|---|
| K0/base regression | 286 | exact pre-audit baseline |
| pre-A2 audit branch | 287 | K0 plus the original A1 boundary fixture |
| post-A2 expanded selection | 288 | pre-A2 selection plus the A2 context/neutrality fixture |

`287` is not the exact-master K0 baseline. No passing evidence was changed; only its label was corrected.

### 23.3 Exact reconciliation of all 26 reactive trigger callbacks

The final fresh-JVM reactive run used the packaged artifact and the exact controlled workload (`Izzet Guild Kit` vs `Dimir Guild Kit`, seed `20260810`, ten games). The audit-only JDI probe reported:

```text
RESOLVE_OCCURRENCES=26
CALLBACK_OCCURRENCES=26
HELPER_OCCURRENCES=0
captureError=null for every occurrence
```

All 26 had immediate caller `WrappedAbility.resolve`, `wrapper.isMandatory() == false`, `static == false`, and `spawningAbility == false`. `continuation=ABSENT` in the metrics-enabled run for every occurrence. The strict v0 classifier treats `intrinsic == false` as untrusted generated/copied/granted provenance even when the visible object shape is otherwise simple.

The exact bucket sum is:

| Bucket | Count | Agent-required? | RandomLegalPolicy blocker? | Reason |
|---|---:|---|---|---|
| `NORMAL_OPTIONAL_NO_COST_PUBLIC` | 0 | — | — | no occurrence satisfies both public-context and trusted intrinsic-provenance admission |
| `NORMAL_OPTIONAL_NO_COST_CONTEXT_UNSUPPORTED` | 22 | Yes, if the trigger remains in the external policy slice | Yes | `CardLKI` and/or opaque `SpellAbility`/collection context cannot be projected safely |
| `COST_BEARING_OPTIONAL` | 1 | Not as generic confirmation | Yes, as payment coverage | `PayLife<3>` belongs to the later payment decision, not a duplicate accept/decline request |
| `STATIC_OPTIONAL` | 0 | — | — | none reached `confirmTrigger` in this workload; static uses another lifecycle |
| `DELAYED_OPTIONAL` | 0 | — | — | no delayed marker reached the seam |
| `GENERATED_OR_COPIED_OPTIONAL` | 3 | Yes if admitted at all; v0 rejects | Yes | visible `DamageDone` objects, but `intrinsic == false` makes provenance fail closed |
| `HIDDEN_SOURCE` | 0 | — | — | no hidden source was observed in these 26 entries |
| `HIDDEN_TRIGGERING_OBJECT` | 0 | — | — | no object was classified as hidden; LKI/opaque objects are in the context-unsupported bucket |
| `OTHER` | 0 | — | — | no remainder |
| **Total** | **26** |  |  | **invariant holds** |

Therefore the narrow correct optional-no-cost adapter coverage is **`0 / 26`** for this controlled run. The 22 context-unsupported and 3 nonintrinsic occurrences remain unresolved external-policy cases; the one cost-bearing occurrence is a separate PAYMENT blocker. No mandatory trigger was among these 26, so no engine-owned mandatory case is counted as a RandomLegalPolicy blocker.

The per-occurrence record below exports only public source categories, public seat identities, safe type categories, and rejection reasons. Hidden card/object values are intentionally not copied into the report.

| # | Game / turn / phase | Acting / decider | Source (zone, visibility) | Mode / API | Optional / wrapper / decider | Provenance / definition candidate | Cost | Triggering-object categories | Continuation | Native result | Bucket |
|---:|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 / 10 / MAIN2 | seat 1 / seat 1 | Blood Operative (battlefield, public) | ChangesZone / ChangeZone | true / false / true | intrinsic / ChangesZone#0 | absent | Card + CardLKI (reject LKI) | absent | true | context unsupported |
| 2 | 1 / 16 / MAIN1 | seat 1 / seat 1 | Blood Operative (graveyard, public) | Surveil / ChangeZone | true / false / false | intrinsic / Surveil#1 | `PayLife<3>` | public Player | absent | false | cost-bearing |
| 3 | 1 / 16 / COMBAT_DAMAGE | seat 1 / seat 1 | Lazav, Dimir Mastermind (battlefield, public) | ChangesZone / Clone | true / false / true | intrinsic / Clone#0 | absent | Card + CardLKI (reject LKI) | absent | true | context unsupported |
| 4 | 2 / 12 / COMBAT_DAMAGE | seat 1 / seat 1 | Nightveil Specter (battlefield, public) | DamageDone / Play | true / false / true | nonintrinsic / DamageDone#1 | absent | public Player/Card + numeric value | absent | true | generated/copied |
| 5 | 2 / 13 / MAIN1 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 6 | 2 / 15 / MAIN1 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 7 | 2 / 16 / MAIN2 | seat 1 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | true | context unsupported |
| 8 | 2 / 16 / MAIN2 | seat 1 / seat 1 | Blood Operative (graveyard, public) | ChangesZone / ChangeZone | true / false / true | intrinsic / ChangesZone#0 | absent | Card + CardLKI (reject LKI) | absent | true | context unsupported |
| 9 | 2 / 17 / MAIN2 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 10 | 3 / 25 / COMBAT_DAMAGE | seat 1 / seat 1 | Tibor and Lumia (battlefield, public) | DamageDone / Play | true / false / true | nonintrinsic / DamageDone#2 | absent | public Player/Card + numeric value | absent | true | generated/copied |
| 11 | 3 / 29 / COMBAT_DAMAGE | seat 1 / seat 1 | Tibor and Lumia (battlefield, public) | DamageDone / Play | true / false / true | nonintrinsic / DamageDone#2 | absent | public Player/Card + numeric value | absent | true | generated/copied |
| 12 | 4 / 9 / MAIN1 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 13 | 4 / 15 / MAIN2 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 14 | 4 / 15 / MAIN2 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 15 | 4 / 15 / MAIN2 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 16 | 4 / 15 / MAIN2 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 17 | 4 / 17 / UPKEEP | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | true | context unsupported |
| 18 | 4 / 17 / UPKEEP | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | true | context unsupported |
| 19 | 4 / 17 / UPKEEP | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | true | context unsupported |
| 20 | 4 / 17 / UPKEEP | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | true | context unsupported |
| 21 | 5 / 21 / UPKEEP | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | true | context unsupported |
| 22 | 5 / 23 / DRAW | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 23 | 5 / 23 / MAIN1 | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 24 | 5 / 27 / DRAW | seat 0 / seat 0 | Gelectrode (battlefield, public) | SpellCast / Untap | true / false / true | intrinsic / SpellCast#0 | absent | public Player/Card + CardLKI + opaque ability/collection | absent | false | context unsupported |
| 25 | 6 / 14 / MAIN1 | seat 1 / seat 1 | Lazav, Dimir Mastermind (battlefield, public) | ChangesZone / Clone | true / false / true | intrinsic / Clone#0 | absent | Card + CardLKI (reject LKI) | absent | true | context unsupported |
| 26 | 6 / 14 / MAIN1 | seat 1 / seat 1 | Lazav, Dimir Mastermind (battlefield, public) | ChangesZone / Clone | true / false / true | intrinsic / Clone#0 | absent | Card + CardLKI (reject LKI) | absent | true | context unsupported |

The table's `optional / wrapper / decider` columns are, in order, `isOptionalTrigger / isMandatory / OptionalDecider`. `acting` is the current Forge phase player; `decider` is the callback controller. This distinction matters in occurrence 7, where a trigger controlled by seat 0 resolves while seat 1 is the active phase player.

### 23.4 Stable trigger-definition identity

`Trigger.getId()` remains rejected as canonical training identity. Its allocation is object-lifecycle dependent, reset/copy behavior is not a semantic definition contract, and generated/delayed/copy paths can preserve or recreate IDs in ways that are not cross-run provenance. `hashCode()`, `toString()`, localized `TriggerDescription`, and Java object identity are also rejected.

The narrow identity needed for a future v0 candidate is a definition key with these components:

```text
host-card semantic definition (set code + canonical card name)
+ card-state name
+ ordinal in the ordered intrinsic trigger-definition collection
+ trigger mode
+ intrinsic/static flags
+ sorted normalized original parameter key/value pairs,
   excluding localized/descriptive TriggerDescription
```

This is a semantic definition identity, not an instance identity. The audit probe captured the ordinal and normalized parameter-key set from the source trigger and the focused fixture confirmed identical ordered definition keys for two independent `Luminous Angel` instances. Two fresh one-game JVM probes with the same seed produced identical safe occurrence output (`SHA-256 31549D3D0C15FFA6C08D79615D60CD15B390EB492F4EBF06A9ED4226F2610D` in both runs), including definition ordinals and modes. That is sufficient evidence for the admitted ordinary intrinsic slice, not a universal proof for generated or granted provenance. The future adapter must fail closed when the host/state/ordinal/normalized definition cannot be established.

### 23.5 Trace-local occurrence identity

Definition identity does not identify a firing. The future seam therefore needs a trace-local monotonic `occurrenceIndex` allocated in engine occurrence order:

```text
occurrenceIndex = 1, 2, 3, ... within one decision trace/session
```

It distinguishes repeated firings of the same source and definition, including different public triggering objects. It is not a semantic candidate ID and must not use PID, wall-clock time, random values, `Trigger.getId()`, or Java identity. The final ten-game probe allocated exactly 26 ordered occurrences; the two fresh same-seed one-game JVM outputs were byte-identical, proving the proposed ordering for the focused controlled fixture while preserving a fail-closed boundary for unsupported provenance.

### 23.6 Triggering-object context

The observed `AbilityKey` union was:

```text
Activator, Card, CardLKI, Cause, CurrentCastSpells,
CurrentStormCount, DamageAmount, DefendingPlayer, LifeAmount,
Player, Source, SpellAbility, SpellAbilityTargets, Target
```

The context audit classifies these as follows:

| AbilityKey / runtime category | Decision relevance in observed slice | Public encoding allowed for v0 | Result |
|---|---|---|---|
| `Player` / `Activator` / `Target` / `DefendingPlayer` → `Player` | can select controller, target, or opponent | public seat/player identity | allowed when Forge visibility says public |
| `Card` / `Source` → visible `Card` | can distinguish the source or public spell/card | public card definition plus a trace-local public instance identity; the probe's `(card id, game timestamp)` is diagnostic only, not canonical training identity | allowed only after visibility and provenance checks |
| `CardLKI` | carries last-known-information/runtime provenance, and may not be safely reconstructible as a public semantic object | none; do not export its identity | fail closed |
| `SpellAbility` | may contain targets, choices, and hidden source information | none; do not expose the runtime object | fail closed |
| `SpellAbilityTargets` / `CurrentCastSpells` → collection/runtime objects | potentially decision-relevant but not a stable neutral value in the observed callback | none; no universal collection serialization | fail closed |
| `DamageAmount`, `LifeAmount`, `CurrentStormCount` → numeric values | relevant only if the admitted trigger definition actually branches on them | typed bounded scalar, only when proven decision-relevant and public | not sufficient to admit the surrounding occurrence |
| `Cause == null` | no value to encode | absent | allowed as absence |

The future public DTO must contain typed neutral records only; it must not contain `Card`, `Player`, `SpellAbility`, `Trigger`, `GameEntity`, or LKI objects. The A2 focused test demonstrates anti-aliasing: the same public source and definition combined with two different public triggering players produce distinct conceptual contexts. It also compares a state fingerprint before/after projection and records zero audit-RNG draws. The test's card id/timestamp string is only a local diagnostic encoding; a production/training identity would need an independently approved public instance identity or a trace-local public ordinal.

There were no strict `NORMAL_OPTIONAL_NO_COST_PUBLIC` occurrences in the 26-run after provenance filtering. The three apparently public `DamageDone` occurrences are deliberately not admitted because `intrinsic == false`; the test does not silently turn public object shape into trusted provenance.

### 23.7 Hidden-information boundary

Visibility uses Forge's existing view semantics (`CardView.canBeShownTo` and `canFaceDownBeShownTo`) rather than a new audit-local rule. A v0 source/object is safe only when the decider can legitimately see it and its semantic identity is public. Ordinary visible battlefield, graveyard, or stack cards can be candidates after the definition/provenance checks; the decider and public triggering players can be represented by public seat identity.

The focused test proves fail-closed behavior for an opponent library card, an opponent face-down source, and a hidden triggering-card position. Their identity is not written into the neutral output. Opponent hand/library cards, face-down cards, hidden spell-ability sources, hidden LKI, and any object whose visibility cannot be proven are unsupported. Emblems, immutable effects, command-zone objects, and delayed/generated sources are also outside this v0 unless a future seam proves a public stable source and definition; source-card visibility alone is not enough.

### 23.8 ActionContinuation

The direct continuation checks are now closed for ordinary trigger resolution:

```text
metrics-enabled ten-game JDI run: continuation PRESENT = 0 / 26
focused accepted optional trigger: active continuation calls = 0
focused declined optional trigger: active continuation calls = 0
non-null ordinary trigger continuations: 0
```

The no-metrics repeat probe reports the diagnostic store as disabled, which is not an active continuation; it never reports `PRESENT`. The future trigger request must not carry priority-action continuation metadata. A non-null continuation at this seam remains an integrity warning and blocks v0 until explained.

### 23.9 Exact admission predicate

The future `OPTIONAL_TRIGGER_NO_COST` classifier must run at the engine-owned seam immediately around `WrappedAbility.resolve` and admit only when every predicate is true:

```text
decider != null
AND !wrapper.isMandatory()
AND normal non-static stack-trigger lifecycle
AND source is an ordinary publicly visible intrinsic host
AND stable card-state/definition identity is available
AND trigger occurrence index is available
AND existing Forge classification says the trigger is optional
AND no nonzero Cost parameter is present
AND Cost=="0" is handled only according to the existing TriggerHandler branch order
AND every decision-relevant triggering object has an approved public typed encoding
AND no CardLKI, opaque SpellAbility/collection, hidden object, or unstable provenance exists
AND ActionContinuation is absent
```

The predicate must reuse Forge's existing trigger classification; it must not create a second cost parser, inspect localized descriptions, consult AI heuristics, or export `AutoYieldStore` state. `Cost` absent, mandatory cost, `Cost == "0"`, and nonzero optional cost are not interchangeable: `OptionalDecider` is tested first, and the existing `TriggerHandler` branch order determines whether a zero-cost form is optional or mandatory. Nonzero cost is always excluded from this generic adapter.

The exact cost cases are:

| Forge shape | Existing classification used by the future predicate |
|---|---|
| Cost absent, no `OptionalDecider` | mandatory wrapper; no `confirmTrigger` request |
| Cost present, parsed payment mandatory | mandatory wrapper; no `confirmTrigger` request |
| `Cost == "0"`, no `OptionalDecider` | mandatory branch by the existing string/mandatory-cost check; no request |
| `OptionalDecider` plus `Cost == "0"` | `OptionalDecider` branch wins first; optional trigger semantics remain, but the v0 predicate still requires the normal public/provenance/context checks |
| nonzero `Cost` | optional cost-bearing path; exclude from generic confirmation and defer to PAYMENT |

No new parser is introduced by A2.

### 23.10 Excluded lifecycles and call-site contamination

| Lifecycle / call site | A2 disposition | Reason |
|---|---|---|
| normal mandatory trigger | engine-owned; no request | `TriggerHandler` leaves `decider == null`; `WrappedAbility.resolve` calls no controller confirmation |
| normal optional no-cost trigger | candidate only | direct boolean semantics are clean, but the 26 controlled occurrences had no admitted strict-v0 member |
| nonzero-cost optional trigger | exclude; PAYMENT | `confirmTrigger` is procedural entry into cost handling; decline belongs to payment/cost mechanics |
| `Cost == "0"` | follow existing branch order | do not infer rule optionality from the string alone |
| static trigger | exclude | static paths use `playTrigger`, not ordinary stack `WrappedAbility.resolve` confirmation |
| delayed/player-defined delayed | exclude by default | provenance and source identity are not yet stable/publicly complete |
| generated/granted/copied/nonintrinsic | exclude by default | A2 observed three `intrinsic == false` public-shaped cases; provenance is not trusted |
| hidden source/object | exclude | Forge visibility cannot prove a safe neutral identity |
| `PlayerControllerAi.chooseContraptionsToCrank` | exclude structurally | it directly calls `confirmTrigger(new WrappedAbility(...))` and does not pass through the `WrappedAbility.resolve` admission seam |

The `confirmTrigger` method itself is therefore not the instrumentation boundary. The authoritative future location is the engine-owned `WrappedAbility.resolve` branch around `decider.getController().confirmTrigger(this)`.

### 23.11 Exactly-once seam proof

The audit-only JDI harness captured prospective facts at the `WrappedAbility.resolve` call boundary, then captured the native boolean at the return line. In the ten-game run it observed 26 resolve entries, 26 callback entries, 26 native results, no helper entries, and no capture errors. The focused `Luminous Angel` fixture observed exactly one callback for acceptance and exactly one for decline, with the effect occurring only for `true`.

This seam has one native callback and one result after the callback. The future adapter can therefore use:

```text
prospective request facts
→ one native confirmTrigger callback
→ observe the returned boolean
```

without a second `doTrigger`, result replay, target reset, RNG draw, or game-state mutation. A helper invocation cannot satisfy the admission predicate because the required `WrappedAbility.resolve` call frame is absent.

### 23.12 Neutrality evidence

The A2 context/visibility test passed with:

```text
ForgeStateFingerprint before == after
DeterminismAuditRandom draw count = 0
```

for the public and rejected projections. The runtime JDI probe used getter-only observation and did not alter the workload; the fresh repeat outputs were identical. Exceptions in the prospective projection are treated as unsupported rather than converted into a request. This is evidence for the test-only classifier shape, not production adapter implementation.

### 23.13 Controlled-v0 blocker ledger

| Callback / lifecycle | Current controlled count | Classification for RandomLegalPolicy |
|---|---:|---|
| optional no-cost trigger | 26 raw; 0 admitted | `DEFERRED_BUT_BLOCKING` — strict context/provenance closure still has no admitted occurrence |
| cost-bearing trigger | 1 raw | `OTHER_DECISION_TYPE` / `DEFERRED_BUT_BLOCKING` — PAYMENT owns the real decline path |
| `confirmAction` | 8 reactive, 0 proactive | `DEFERRED_BUT_BLOCKING` — heterogeneous caller-owned modes |
| `confirmPayment` | 0 / 0 | `DEFERRED_BUT_BLOCKING` if reached; not measured as current blocker |
| `chooseBinary` | 2 / 0 | `OTHER_DECISION_TYPE` — domain-specific binary choices |
| `payCostToPreventEffect` | 5 / 24 | `OTHER_DECISION_TYPE` / `DEFERRED_BUT_BLOCKING` — payment/prevention |
| `confirmBidAction` | 0 / 0 | `NOT_REACHED`; separate BID family |
| `confirmReplacementEffect` | 0 / 0 | `NOT_REACHED`; separate REPLACEMENT family |
| `confirmStaticApplication` | 0 / 0 | `NOT_REACHED`; separate static/combat family |
| `chooseFlipResult`, `payCostDuringRoll`, `payCombatCost` | 0 / 0 | `NOT_REACHED`; separate families |
| mandatory normal triggers | not a callback count | `ENGINE_OWNED`; no RandomLegalPolicy blocker |

The current confirmation-like RandomLegalPolicy blockers are therefore not solved globally by trigger analysis: 8 `confirmAction`, 2 `chooseBinary`, 5 reactive / 24 proactive prevention payments, the one cost-bearing trigger's payment path, and the unresolved strict optional-trigger context/provenance cases remain separate work.

### 23.14 DECISION_TRACE_V2 compatibility

No schema expansion is required. If a future closure admits an occurrence, the existing trace contract is sufficient:

```text
REQUEST: DecisionType.CONFIRMATION
legalCandidates: [ACCEPT, DECLINE]
RESULT: CHOSEN
```

The candidate order is stable (`ACCEPT`, then `DECLINE`). Mandatory triggers produce no request. No `DECISION_TRACE_V3` is proposed. A2 generated no production request and therefore does not add `DecisionType.CONFIRMATION`.

### 23.15 A2 verdict

The architecture closure conditions are not all satisfied for a production v0 adapter. The exact remaining blockers are:

```text
22 / 26: CardLKI and/or opaque runtime context cannot be publicly encoded safely
3 / 26: visible object shape but intrinsic provenance is false/untrusted
1 / 26: nonzero Cost belongs to PAYMENT, not generic confirmation
```

The mandatory-trigger, helper-contamination, ActionContinuation, exactly-once seam, visibility fail-closed, state-neutrality, RNG-neutrality, and DECISION_TRACE_V2 questions are answered, but the strict candidate coverage is still zero in the controlled workload.

**A2 architecture verdict: `NO_SAFE_V0_YET`.**

This remains an audit-only decision. Do not implement `DecisionType.CONFIRMATION`, `ConfirmationDecisionProvider`, or any production adapter until architecture review explicitly approves a narrower admitted trigger shape and its context contract.
