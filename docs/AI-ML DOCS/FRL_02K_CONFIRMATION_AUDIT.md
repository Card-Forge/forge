# FRL-02K — Confirmation Attribution and Semantic Boundary Audit

Status: A3 audit retained; FRL-02K-B1 Gelectrode production addendum PASS; FRL-02K-C1 ChangesZone projection evidence PASS with C1R semantic corrections recorded; FRL-02K-C2A exact Blood Operative ETB TARGET profile SUPPORTED / PASS. Global triggered TARGET, Blood CONFIRMATION, and global CONFIRMATION remain OPEN.

Audit date: 2026-08-11 (historical A1/A2/A3 evidence begins 2026-08-10)

Repository: `chrismaghuhn/forgeAI`

Audit worktree: `C:\forgeAI-confirmation`

Branch: `frl/02k-confirmation-boundary`

Architecture authority: `docs/AI-ML DOCS/ML_STRATEGY.md`

## Current authority (R3)

The following is the current C2A authority. Sections 29.19 and 29.20 retain the pre-C2A C2 disposition for historical traceability only and do not override this status.

| Boundary | Current authority |
|---|---|
| `FRL-02K-C2A` | `SUPPORTED / PASS` for the exact Blood Operative ETB `TARGET` profile only. |
| `resolver == null` | Native Forge ownership is preserved. |
| `resolver != null` | Only the exact Blood profile is externally owned. All other player-owned targeted triggers fail closed; there is no Forge-AI fallback. |
| `global triggered TARGET` | `OPEN` |
| `Blood CONFIRMATION` | `OPEN` |
| `global CONFIRMATION` | `OPEN` |
| Agent completeness | Blood is not agent-complete; only the exact ETB `TARGET` profile is supported, and the later Blood confirmation remains open. |

Reference set: [C2A design/spec](../superpowers/specs/2026-08-11-frl-02k-c2a-triggered-target-provider-seam-design.md), [C2A implementation plan](../superpowers/plans/2026-08-11-frl-02k-c2a-triggered-target-provider-seam.md), and [C2A audit](FRL_02K_C2A_TRIGGERED_TARGET_AUDIT.md).

The recorded current validation evidence is the audit's [evidence matrix](FRL_02K_C2A_TRIGGERED_TARGET_AUDIT.md#6-evidence-matrix) and [exact Task 12 commands](FRL_02K_C2A_TRIGGERED_TARGET_AUDIT.md#61-final-task-12-exact-commands). This central-ledger update adds no new test result.

Determinism and safety authority: `docs/AI-ML DOCS/FRL_02K0_DETERMINISM_GATE_REPORT.md`

Historical primary recommendation after A2: `NO_SAFE_V0_YET`

The cleanest future candidate identified by A2 was `OPTIONAL_TRIGGER_NO_COST`. Section 26 records the separately
reviewed and implemented `Gelectrode` slice; the other optional-trigger shapes remain unimplemented.

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

The A1/A2/A3 audit portions below did not add `DecisionType.CONFIRMATION`, `DecisionRequest.CONFIRMATION`,
`LegalCandidate`, `ConfirmationContext`, `ConfirmationDecisionProvider`, `ConfirmationAdapter`, or any other
production boundary. Section 26 is the later B1 production addendum and is intentionally separated from those
historical audit claims.

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

The controlled workloads produced 26 `confirmTrigger` entries in the reactive matchup and zero in the proactive matchup, but the 26 entries are not all equivalent policy decisions. The historical A2/A3 verdict was
`NO_SAFE_V0_YET`; B1 now closes only the named Gelectrode shape and leaves the other shapes open.

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

All 26 had immediate caller `WrappedAbility.resolve`, `wrapper.isMandatory() == false`, `static == false`, and `spawningAbility == false`. `continuation=ABSENT` in the metrics-enabled run for every occurrence. The strict v0 classifier treats `intrinsic == false` as untrusted nonintrinsic provenance even when the visible object shape is otherwise simple; it does not infer the exact generated, copied, or granted construction path from that flag alone.

The exact bucket sum is:

| Bucket | Count | Agent-required? | RandomLegalPolicy blocker? | Reason |
|---|---:|---|---|---|
| `NORMAL_OPTIONAL_NO_COST_PUBLIC` | 0 | — | — | no occurrence satisfies both public-context and trusted intrinsic-provenance admission |
| `NORMAL_OPTIONAL_NO_COST_CONTEXT_UNSUPPORTED` | 22 | Yes, if the trigger remains in the external policy slice | Yes | the current generic A2 projection cannot safely admit `CardLKI` and/or opaque `SpellAbility`/collection context; field decision-relevance is not yet proven |
| `COST_BEARING_OPTIONAL` | 1 | Not as generic confirmation | Yes, as payment coverage | `PayLife<3>` belongs to the later payment decision, not a duplicate accept/decline request |
| `STATIC_OPTIONAL` | 0 | — | — | none reached `confirmTrigger` in this workload; static uses another lifecycle |
| `DELAYED_OPTIONAL` | 0 | — | — | no delayed marker reached the seam |
| `GENERATED_OR_COPIED_OPTIONAL` | 3 | Yes if admitted at all; v0 rejects | Yes | visible `DamageDone` objects, but `intrinsic == false` is untrusted provenance and therefore fails closed |
| `HIDDEN_SOURCE` | 0 | — | — | no hidden source was observed in these 26 entries |
| `HIDDEN_TRIGGERING_OBJECT` | 0 | — | — | no object was classified as hidden; LKI/opaque objects are in the context-unsupported bucket |
| `OTHER` | 0 | — | — | no remainder |
| **Total** | **26** |  |  | **invariant holds** |

Therefore the narrow correct optional-no-cost adapter coverage is **`0 / 26`** for this controlled run. This is a result of the **current strict A2 admission predicate**, not a proof that the 22 runtime shapes are permanently unrepresentable. The current generic projection refuses to expose raw or insufficiently justified runtime context; a future semantic-relevance audit may prove that individual fields are `ENGINE_ONLY`, `REDUNDANT`, or `DERIVABLE_FROM_PUBLIC_CONTEXT`, or may replace them with a smaller public projection. The 22 context-unsupported and 3 nonintrinsic occurrences therefore remain unresolved external-policy cases, not impossibility findings; the one cost-bearing occurrence is a separate PAYMENT blocker. No mandatory trigger was among these 26, so no engine-owned mandatory case is counted as a RandomLegalPolicy blocker.

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

The narrow identity needed for a future v0 candidate is a **semantic definition key** with these components:

```text
host-card canonical/rules definition identity
+ card-state name
+ ordinal in the ordered intrinsic trigger-definition collection
+ trigger mode
+ intrinsic/static flags
+ sorted normalized original parameter key/value pairs,
   excluding localized/descriptive TriggerDescription
```

This is a semantic definition identity, not an instance identity. The repository does not yet provide a proven universal canonical rules-definition identifier, so the A2 test uses an explicitly audit-only approximation based on canonical card name, card state, ordered definition ordinal, mode/flags, and normalized nonlocalized parameters. The test deliberately excludes set code, printing, runtime card ID, game timestamp, `Trigger.getId()`, hash code, object identity, and localized descriptions from the semantic key. Set code, printing, runtime IDs, timestamps, and `Trigger.getId()` may remain diagnostic/provenance metadata, but they must not silently become policy/training semantics. Two independent `Luminous Angel` instances still produce the same audit-only semantic definition keys. Two fresh one-game JVM probes with the same seed produced identical safe occurrence output (`SHA-256 31549D3D0C15FFA6C08D79615D60CD15B390EB492F4EBF06A9ED4226F2610D` in both runs), including definition ordinals and modes. That is evidence for the admitted ordinary intrinsic slice, not a universal proof for generated or granted provenance. The future adapter must fail closed when the host/state/ordinal/normalized definition cannot be established.

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
| `CardLKI` | carries last-known-information/runtime provenance; A2 did not prove whether every field is policy-relevant | none in the current generic projection; a future audit may prove an explicit public replacement or engine-only status | current A2 projection rejects the raw value; this is not a permanent unrepresentability finding |
| `SpellAbility` | may contain targets, choices, and hidden source information; A2 did not prove which fields, if any, the policy needs | none for the raw object; only an explicitly proven typed public subset could be considered later | current A2 projection rejects the raw value; this is not a permanent unrepresentability finding |
| `SpellAbilityTargets` / `CurrentCastSpells` → collection/runtime objects | potentially decision-relevant, redundant, or engine-only; A2 did not prove which | none for the opaque collection; only a future explicitly typed public subset could be considered | current A2 projection rejects the opaque value pending semantic-relevance proof |
| `DamageAmount`, `LifeAmount`, `CurrentStormCount` → numeric values | relevant only if the admitted trigger definition actually branches on them | typed bounded scalar, only when proven decision-relevant and public | not sufficient to admit the surrounding occurrence |
| `Cause == null` | no value to encode | absent | allowed as absence |

For `SpellAbilityTargets` and `CurrentCastSpells`, the current table's `fail closed` result means only that the opaque collection is not admitted by the generic A2 projection. It does not establish that every member or derived fact is needed by the policy, nor that a smaller public event projection is impossible. A future relevance audit must make that distinction explicitly.

The future public DTO must contain typed neutral records only; it must not contain `Card`, `Player`, `SpellAbility`, `Trigger`, `GameEntity`, or LKI objects. The A2 focused test demonstrates anti-aliasing from one fixed decider/viewer perspective: the same public source and definition combined with two different public triggering players produce distinct conceptual contexts. It also compares a state fingerprint before/after projection and records zero audit-RNG draws. The test's card id/timestamp string is only a local diagnostic encoding; a production/training identity would need an independently approved public instance identity or a trace-local public ordinal.

There were no strict `NORMAL_OPTIONAL_NO_COST_PUBLIC` occurrences in the 26-run after provenance filtering. The three apparently public `DamageDone` occurrences are deliberately not admitted because `intrinsic == false`; the test does not silently turn public object shape into trusted provenance. Likewise, the 22 context-unsupported occurrences are currently unsupported by the generic A2 projection; the presence of `CardLKI`, `SpellAbility`, or an opaque collection alone does not establish that every contained fact is required by the external policy or impossible to replace with a smaller public event projection.

### 23.7 Hidden-information boundary

Visibility uses Forge's existing view semantics (`CardView.canBeShownTo` and `canFaceDownBeShownTo`) rather than a new audit-local rule. A v0 source/object is safe only when the decider can legitimately see it and its semantic identity is public. Ordinary visible battlefield, graveyard, or stack cards can be candidates after the definition/provenance checks; the decider and public triggering players can be represented by public seat identity. The A2 rejection of an opaque runtime value is not a substitute for this visibility decision: a future relevance audit must still distinguish engine-only or derivable facts from genuinely unsafe hidden information.

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
AND the current strict projection contains no raw CardLKI, opaque SpellAbility/collection,
    hidden object, or untrusted provenance
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
| generated/granted/copied/nonintrinsic | exclude by default | A2 observed three `intrinsic == false` public-shaped cases; provenance is not trusted, but `intrinsic == false` is not treated as a universal generated/copied/granted proof |
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

The architecture closure conditions are not all satisfied for a production v0 adapter. The exact remaining blockers under the **current strict A2 predicate** are:

```text
22 / 26: current generic projection rejects CardLKI and/or opaque runtime context pending decision-relevance proof
3 / 26: visible object shape but intrinsic provenance is false/untrusted pending dedicated provenance attribution
1 / 26: nonzero Cost belongs to PAYMENT, not generic confirmation
```

The mandatory-trigger, helper-contamination, ActionContinuation, exactly-once seam, visibility fail-closed, state-neutrality, RNG-neutrality, and DECISION_TRACE_V2 questions are answered, but the strict candidate coverage is still zero in the controlled workload. The `22 / 26` line is a current unsupported classification, not a theorem that those runtime shapes can never be represented safely; decision relevance and any smaller public projection remain open.

**A2 architecture verdict: `NO_SAFE_V0_YET`.**

This remains an audit-only decision. Do not implement `DecisionType.CONFIRMATION`, `ConfirmationDecisionProvider`, or any production adapter until architecture review explicitly approves a narrower admitted trigger shape and its context contract.

## 24. FRL-02K-A2R architecture-review corrections

This section records the review corrections without reopening the A2 findings or changing the `NO_SAFE_V0_YET` verdict.

### 24.1 Meaning of `0 / 26`

`NORMAL_OPTIONAL_NO_COST_PUBLIC = 0 / 26` means that no measured occurrence satisfies the **current strict A2 admission predicate**. The current generic projection fails closed when it encounters raw `CardLKI`, raw `SpellAbility`, opaque collections, hidden values, or untrusted provenance. This is a statement of current support, not a proof that those runtime shapes are permanently unrepresentable. A future semantic-relevance audit must determine whether each field is `ENGINE_ONLY`, `REDUNDANT`, `DERIVABLE_FROM_PUBLIC_CONTEXT`, publicly projectable through an explicit typed value, or genuinely unsafe. Runtime context presence alone is not enough to conclude impossibility.

### 24.2 Semantic identity versus provenance identity

The future policy/training semantic key is conceptually:

```text
canonical card/rules identity
+ card-state identity
+ ordered trigger-definition discriminator
+ trigger mode
+ normalized nonlocalized semantic parameters
```

The test-only helper uses canonical card name plus card state, ordered definition ordinal, mode/flags, and normalized parameters as an explicitly limited approximation because no universal canonical rules identifier has been proven here. Set code, printing, runtime card ID, game timestamp, `Trigger.getId()`, hash code, object identity, and localized descriptions are not semantic policy identity. They may be retained separately as diagnostic/provenance metadata.

### 24.3 Fixed-perspective anti-aliasing

The test-only context helper now takes independent `deciderViewer` and `triggeringPlayer` values. The anti-aliasing assertion keeps one fixed decider/viewer, one source, and one definition, then varies only the public triggering player. Source visibility is checked against the fixed decider/viewer, and the two conceptual contexts must differ. This corrects the earlier test weakness that changed viewer perspective between the compared events.

### 24.4 Hidden-information and provenance preservation

The test continues to use Forge's `CardView.canBeShownTo` and `canFaceDownBeShownTo` authority. Opponent library/hidden-zone identity and face-down identity fail closed without being copied into the neutral output. `intrinsic == false` remains currently untrusted provenance for strict v0; it is not treated as a universal semantic synonym for generated, copied, or granted. Exact provenance remains a dedicated future audit question.

### 24.5 Evidence labels

The historical labels remain `K0/base = 286`, `pre-A2 audit branch = 287`, and `post-A2 expanded selection = 288`. Any result from these review-correction tests is reported as `post-A2R`, never relabeled as a K0 or A2 baseline.

### 24.6 A2R verification result

The unchanged expanded A2 selection was rerun after the corrections and remains `post-A2R = 288` tests with zero failures, errors, and skips. The focused correction/lifecycle selection ran 4 tests (`FRL02KConfirmationAuditTest` = 3, `TriggerLifeGateTest` = 1) with zero failures, errors, and skips. `FullGameCollectorNeutralityTest` and `WorkerIsolationSmokeTest` each passed as one-test gates; their combined orchestration exceeded the initial 120-second wrapper limit, so they were rerun separately and completed successfully. Package, configured `validate`/Checkstyle, and `git diff --check` also passed. No production implementation file changed.

## 25. FRL-02K-A3 decision relevance and public event projection

This section records the A3 audit after PR #12 was merged into `master` at
`62ea04e8dd2c0f374208a4ecaeba66d5d423422f`. A3 is test-only architecture evidence on branch
`frl/02k-a3-trigger-event-projection`. It does not add `DecisionType.CONFIRMATION`, a confirmation provider,
a production trigger context, a serializer, or external policy integration.

The A3 question is narrower than “can every Forge run parameter be serialized?” It asks which values are
actually needed by the external policy for one measured trigger shape, and whether those facts can be projected
from public, typed semantics while omitting the raw Forge objects.

### 25.1 Reproduction of the A2 occurrence set

The canonical reactive workload was rerun in a fresh JVM:

```text
Izzet Guild Kit vs Dimir Guild Kit
seed 20260810
10 games
```

The audit-only JDI probe observed:

```text
WrappedAbility.resolve occurrences = 26
confirmTrigger callbacks           = 26
helper-origin callbacks            = 0
native results                     = 26
capture errors                     = 0
ordinary continuations present     = 0
```

A second fresh JVM run produced the same 26 occurrence records in the same order. After removing only the
additional diagnostic `originalHostName` field used for the provenance audit, the two complete occurrence
record lists compared equal (`26` versus `26`, difference count `0`). The proactive workload remains at zero
`confirmTrigger` callbacks. The A2 count is therefore reproduced exactly; no workload divergence occurred.

### 25.2 Semantic shape clustering

| Semantic shape | Count | Cost class | Provenance | A2 bucket |
|---|---:|---|---|---|
| `Gelectrode`: `SpellCast -> Untap` | 17 | absent / zero | intrinsic | `NORMAL_OPTIONAL_NO_COST_CONTEXT_UNSUPPORTED` |
| `Lazav, Dimir Mastermind`: `ChangesZone -> Clone` | 3 | absent / zero | intrinsic | `NORMAL_OPTIONAL_NO_COST_CONTEXT_UNSUPPORTED` |
| `Blood Operative`: `ChangesZone -> ChangeZone` | 2 | absent / zero | intrinsic | `NORMAL_OPTIONAL_NO_COST_CONTEXT_UNSUPPORTED` |
| `Blood Operative`: `Surveil -> ChangeZone` | 1 | `PayLife<3>` | intrinsic | `COST_BEARING_OPTIONAL` |
| Cipher-derived `DamageDone -> Play`: `Nightveil Specter` (1), `Tibor and Lumia` (2) | 3 | absent / zero | `intrinsic=false`, derived from `Stolen Identity` | `GENERATED_OR_COPIED_OPTIONAL` |
| **Total** | **26** |  |  |  |

### 25.3 Dominant candidate

The dominant normal candidate is intrinsic `Gelectrode`, `SpellCast -> Untap`, at `17 / 26` callbacks. It is
non-static, non-delayed in the measured run, has `OptionalDecider$ You`, has no nonzero cost, and has a visible
battlefield source. It is therefore the narrow A3 candidate. A3 does not generalize its projection to the other
four semantic shapes.

### 25.4 Exact Forge engine trace

The card script is `forge-gui/res/cardsfolder/g/gelectrode.txt`:

```text
T:Mode$ SpellCast | ValidCard$ Instant,Sorcery | ValidActivatingPlayer$ You
  | TriggerZones$ Battlefield | Execute$ TrigUntap | OptionalDecider$ You
SVar:TrigUntap:DB$ Untap | Defined$ Self
```

The source path is:

```text
TriggerSpellAbilityCastOrCopy.performTest(runParams)
  -> requires SpellAbility and Activator
  -> checks ValidActivatingPlayer and ValidCard
TriggerHandler.runSingleTriggerInternal(...)
  -> constructs the execution SpellAbility
  -> copies trigger objects into it
  -> OptionalDecider sets optional=true and the decider
  -> creates WrappedAbility
  -> queues the non-static wrapper on the simultaneous stack
MagicStack.addAllTriggeredAbilitiesToStack()
  -> invokes the controller's simultaneous-resolution path
WrappedAbility.resolve()
  -> calls confirmTrigger exactly once when decider != null
  -> true reaches playSpellAbilityNoStack(sa, false)
  -> false returns before the effect
```

Forge needs the complete `SpellAbility` and run-parameter map to establish that a qualifying spell was cast and
to execute `TrigUntap`. That does not make the complete map policy context. The actual Gelectrode effect is
“untap this visible source”; it has no target, card-selection, count, life-payment, or spell-identity branch.

### 25.5 AbilityKey decision-relevance matrix for Gelectrode

| AbilityKey | Runtime type in the workload | Why Forge has it | Needed for this ACCEPT/DECLINE decision? | Public replacement | Classification |
|---|---|---|---|---|---|
| `Activator` | `Player` | Valid activating-player matching and decider resolution | Yes: identifies the public event participant | typed player/seat identity | `REQUIRED_FOR_POLICY_CONTEXT` |
| `Card` | `Card` | `ValidCard$ Instant,Sorcery` matching and trigger diagnostics | No; the trigger already qualifies and the effect does not inspect the cast card | none in the trigger-local context; public card facts remain ordinary observation/history if needed | `REQUIRED_FOR_ENGINE_ONLY` |
| `CardLKI` | `Card` / LKI copy | Generic cast-trigger object propagation for effects and last-known information | No for Gelectrode | omit; never expose the raw LKI object | `REQUIRED_FOR_ENGINE_ONLY` |
| `CurrentCastSpells` | opaque `ArrayList` | Generic run-parameter propagation for triggers that count or inspect casts | No; Gelectrode has no cast-count predicate | omit | `REDUNDANT` |
| `CurrentStormCount` | `Integer` | Generic storm/count trigger support | No | omit | `REDUNDANT` |
| `LifeAmount` | `Integer` | Generic life-payment propagation from a cast ability | No | omit | `REDUNDANT` |
| `SpellAbility` | `SpellApiBased` | Trigger qualification and the execution carrier | No as a raw policy value | event type plus typed public participants | `REQUIRED_FOR_ENGINE_ONLY` |
| `SpellAbilityTargets` | opaque `FCollection` when present | Generic target-sensitive trigger support | No; Gelectrode has no target predicate or target effect | omit | `REDUNDANT` |

The distinction is per semantic shape. The same key can be policy-relevant for a different trigger; A3 does not
declare a global AbilityKey rule.

### 25.6 CardLKI verdict

**`ENGINE_ONLY` for the selected Gelectrode shape.** `TriggerSpellAbilityCastOrCopy.setTriggeringObjects` copies
`CardLKI` into the wrapped ability because the common cast-trigger machinery preserves a broad engine object
surface. Gelectrode's own `ValidCard`, `ValidActivatingPlayer`, and `TrigUntap` semantics do not inspect the
LKI identity. The A3 runtime fixture deliberately supplies a `CardLKI` object, then proves that the public
projection ignores it. A hidden LKI value is never converted into a public card identity. If another trigger
uses an LKI fact to decide, that trigger is outside this slice and must fail closed until separately proven.

### 25.7 SpellAbility and collection verdict

The raw `SpellAbility` is **`ENGINE_ONLY`** for Gelectrode. `SpellAbilityTargets` and `CurrentCastSpells` are
**`REDUNDANT`** for this shape; no raw replacement is permitted. The exact replacement surface is:

```text
event = SPELL_CAST
triggeringPlayer = public player/seat
```

combined with the visible source, semantic trigger definition, and the ordinary player-perspective observation.
The A3 test creates two separate runtime SpellAbility objects and two separate collection objects. The
projection remains equal after occurrence identity is removed. A hidden `CardLKI` and a hidden collection member
also leave the projection unchanged and their identities do not appear in the output.

### 25.8 Minimum sufficient public event context

For this exact shape, the smallest conceptual decision-local context is:

```text
source              = public source instance reference
definition          = semantic trigger-definition key
occurrenceIndex     = trace-local monotonic occurrence identity
event               = SPELL_CAST
triggeringPlayer    = public player/seat identity
deciderViewer       = separate decision-maker perspective
```

The future source reference must be an explicitly approved public instance identity or trace-local public ordinal;
the test-only diagnostic card ID and game timestamp are not the production contract. The definition key is the
semantic key described below. `activePlayer` remains a separate game-state/observation fact and is not collapsed
into `triggeringPlayer` or `deciderViewer`.

For Gelectrode, the cast-card identity, LKI, raw SpellAbility, targets, storm count, life amount, and collections
are intentionally omitted. If a future event shape requires one of those facts, it needs a separate relevance
proof rather than reuse of this projection.

### 25.9 Observation/history split

The normal visible game observation owns the current battlefield/source state, player/seat entities, and any
public card or event facts that the existing observation contract already exposes. A future public history event
may own the already-observable fact that a player cast an instant or sorcery. The trigger-specific request only
needs to identify the decision-local event and the source/definition/occurrence that is currently asking for
`ACCEPT` or `DECLINE`; it must not copy the whole game state or duplicate an existing public history event.

No new `HistoryEvent` or production DTO was added in A3. The current `DecisionRequest` model has only
decision-specific context slots and no approved generic confirmation-context slot. This is an implementation
placement task for the future slice, not a reason to invent `DECISION_TRACE_V3`: the trace-level REQUEST/RESULT
contract remains compatible, while the exact typed context slot must be approved during implementation.

### 25.10 Fixed-perspective anti-aliasing

The test-only helper takes independent `deciderViewer` and `triggeringPlayer` arguments. The fixed-viewer test
uses the same visible source and semantic definition, keeps `deciderViewer` unchanged, and compares
`triggeringPlayer = player A` with `triggeringPlayer = player B`. The resulting contexts are distinct. Source
visibility is checked against `deciderViewer`, never against the event participant. This is the required
perspective-fixed invariant, not a viewer-switching comparison.

### 25.11 Opaque-object invariance and sufficiency result

The runtime-backed A3 fixture resolves two Gelectrode SpellCast triggers with:

```text
different Card instances
different SpellAbility instances
different CurrentCastSpells collection instances
CardLKI present in the engine map
same visible source, event shape, and public triggering player
```

The raw maps differ by object identity, but the public conceptual contexts are identical after removing the
trace-local occurrence index. A second adversarial map adds an LKI copy of an opponent library card and a
collection containing a hidden member; the projection is unchanged and the hidden card name is absent. This
proves intentional abstraction for the selected shape: the omitted objects do not provide a policy distinction
for Gelectrode. It does not authorize dropping opaque objects for other trigger definitions.

### 25.12 Provenance audit of the three `intrinsic=false` callbacks

All three A2 occurrences are Cipher-derived triggers, not proven universal “generated/copied/granted” cases:

| Occurrence(s) | Visible source | `originalHostName` | Construction path | A3 category | v0 status |
|---|---|---|---|---|---|
| 4 | `Nightveil Specter` | `Stolen Identity` | Cipher static effect adds `CipherTrigger` to the encoded card; `Card.getTriggerForStaticAbility(..., false, stAb)` parses the trigger | `DERIVED_BUT_STABLY_ATTRIBUTABLE` | exclude pending trusted provenance contract |
| 10, 11 | `Tibor and Lumia` | `Stolen Identity` | same Cipher `AddTrigger$ CipherTrigger` path; the trigger executes `PlayEncoded` with `CopyCard$ True` | `DERIVED_BUT_STABLY_ATTRIBUTABLE` | exclude pending trusted provenance contract |

Forge's Cipher construction is visible in `CardFactoryUtil` (`AddTrigger$ CipherTrigger`, `PlayEncoded`) and the
static-trigger path in `StaticAbilityContinuous`/`Card`. `intrinsic=false` therefore means “not an intrinsic
card-script trigger on the current host,” not “universally generated, copied, or granted.” A3 keeps all three
out of the admitted slice rather than increasing coverage by assumption.

### 25.13 Semantic identity refinement

The future semantic definition key remains conceptually:

```text
canonical rules/card identity
+ card-state identity
+ ordered intrinsic trigger-definition discriminator or normalized definition hash
+ trigger mode
+ normalized nonlocalized semantic parameters
```

For this audit only, `stableSemanticDefinitionKeys` uses canonical card name plus current card state, trigger
ordinal, mode/flags, and sorted nonlocalized parameters. The test proves equal keys for independent instances of
the same semantic card-state definition and deliberately excludes set code, printing, runtime card ID, game
timestamp, `Trigger.getId()`, hash code, Java object identity, `toString()`, and localized descriptions.

Set/printing, runtime ID, timestamp, and `Trigger.getId()` may be retained as separate diagnostic/provenance
metadata. They are not policy/training identity. The audit does not claim to have solved universal canonical
card identity; it has only prevented printing identity from becoming the semantic contract.

### 25.14 Occurrence identity and determinism

A future implementation should allocate a trace-local monotonic `occurrenceIndex` at the engine-owned seam:

```text
1, 2, 3, ... within the decision trace
```

It is an occurrence discriminator, not a semantic candidate identity. It must not use PID, time, randomness,
object identity, `hashCode()`, or `Trigger.getId()`. The current A3 harness does not add a production counter; it
uses its local resolve order only as evidence. Two fresh JVM runs of the same seed/workload produced identical
26-record occurrence order, supporting this deterministic trace-local design. Same source/definition events with
different triggering objects remain distinguishable by occurrence index even when the semantic projection is the
same.

### 25.15 Hidden-information boundary and information monotonicity

Supported for the Gelectrode slice:

```text
visible battlefield Gelectrode source
public triggering player/seat
public decider/viewer perspective
```

Omitted by construction:

```text
CardLKI
raw SpellAbility
raw Card and Player objects
SpellAbilityTargets
CurrentCastSpells and other opaque collections
hidden cast-card identity
```

Rejected or deferred:

```text
opponent library/hand source identity
face-down source identity
hidden triggering card identity
hidden collection members when a future trigger actually needs them
delayed/generated/copied/granted or untrusted-provenance sources
```

The existing A2 visibility fixture uses Forge's `CardView.canBeShownTo` and
`CardView.canFaceDownBeShownTo` authority for opponent-library and face-down cards and fails closed. The A3
adversarial map demonstrates that an opponent hidden LKI/member is not exported because the Gelectrode projector
does not inspect it. The information-monotonicity rule is explicit: a trigger request may contain no information
the deciding player could not legally know at that exact point, and projection may not be more informative than
Forge's own player-perspective state/history plus the public event semantics.

### 25.16 ActionContinuation

The measured A2/A3 workload has:

```text
non-null ordinary trigger continuations = 0 / 26
```

The focused optional-trigger fixtures also observe zero active continuations. The lifecycle remains:

```text
causing action -> trigger queued -> original action continuation closes
  -> later trigger resolution -> no continuation on the trigger decision
```

The future admission rule treats a non-null continuation at ordinary trigger resolution as an integrity warning
and fails closed unless a separately proven valid lifecycle exists. No continuation is copied into the proposed
context.

### 25.17 A3 admission predicate and exclusions

The Gelectrode slice admission predicate is:

```text
engine-owned WrappedAbility.resolve seam
AND decider != null
AND wrapper is optional
AND normal non-static resolution
AND intrinsic trusted provenance
AND no nonzero cost under Forge's existing classification
AND visible source under the decider's perspective
AND stable semantic definition identity
AND trace-local occurrence identity
AND all decision-relevant objects have approved public typed encodings
AND no active ActionContinuation
```

The predicate rejects mandatory triggers because they are engine-owned and produce no request; nonzero-cost
triggers because decline belongs to cost/payment semantics; static triggers because they use `playTrigger`; delayed
and player-defined delayed triggers because timing/provenance are not closed; generated/copied/granted and
`intrinsic=false` triggers because provenance is not trusted for this slice; helper calls because they do not pass
through the `WrappedAbility.resolve` seam; and hidden sources/objects because visibility fails closed.

`Cost == "0"` continues to follow `TriggerHandler`'s existing branch order. `OptionalDecider` wins before the
zero-cost mandatory branch; string text is not reparsed by the audit.

### 25.18 State and RNG neutrality

The corrected fixed-viewer, hidden-source, hidden-object, and Gelectrode projection tests all compare
`ForgeStateFingerprint` before and after projection and use `DeterminismAuditRandom` as the active RNG. Results:

```text
ForgeStateFingerprint before == after: PASS
DeterminismAuditRandom draws:         0
supported Gelectrode projection:      PASS
rejected hidden projection:           PASS
opaque/LKI omission projection:      PASS
```

The projector is getter-only and exception-isolated. Any unsupported or unexpected value is an `UNSUPPORTED`
result, not a mutation, RNG fallback, or Forge-AI fallback.

### 25.19 A2 versus A3 26-way reconciliation

| Bucket | A2 count | A3 count | Why changed |
|---|---:|---:|---|
| `NORMAL_OPTIONAL_NO_COST_PUBLIC` | 0 | 0 | The old generic public-object bucket remains strict and is not reused. |
| `NORMAL_OPTIONAL_NO_COST_EVENT_PROJECTABLE` | 0 | 17 | Gelectrode's raw CardLKI/SpellAbility/collection fields were proven engine-only or redundant for this semantic shape. |
| `NORMAL_OPTIONAL_NO_COST_CONTEXT_UNSUPPORTED` | 22 | 5 | 17 Gelectrode occurrences moved to the explicit event-projectable bucket; Lazav and Blood Operative remain unresolved. |
| `COST_BEARING_OPTIONAL` | 1 | 1 | Payment-owned semantics unchanged. |
| `STATIC_OPTIONAL` | 0 | 0 | Not measured in the controlled workload; still excluded. |
| `DELAYED_OPTIONAL` | 0 | 0 | Not measured; still excluded. |
| `PROVENANCE_UNTRUSTED_DERIVED` | 0 | 3 | A2's `GENERATED_OR_COPIED_OPTIONAL` label was refined to Cipher-derived but untrusted provenance. |
| hidden source/object | 0 | 0 | No measured hidden source/object callback; future hidden cases still fail closed. |
| other | 0 | 0 | No remainder. |
| **Total** | **26** | **26** | **No occurrence left unclassified.** |

The strict A2 admitted count remains `0 / 26`; A3 semantically projectable count is `17 / 26`; remaining
non-admitted callbacks are `9 / 26` (`5` unsupported normal no-cost, `1` cost-bearing, `3` untrusted derived
provenance).

### 25.20 Controlled RandomLegalPolicy blocker ledger

| Current family | Count | A3 status |
|---|---:|---|
| Gelectrode optional no-cost trigger slice | 17 reactive | `SUPPORTED_BY_PROPOSED_ADAPTER` at architecture level; no production adapter exists yet |
| Other normal optional no-cost triggers | 5 reactive | `DEFERRED_BUT_BLOCKING` |
| Cost-bearing trigger | 1 reactive | `OTHER_DECISION_TYPE` / `DEFERRED_BUT_BLOCKING`; cost/payment-owned |
| `confirmAction` | 8 reactive | `DEFERRED_BUT_BLOCKING`; heterogeneous caller-owned semantics |
| `confirmPayment` | 0 / 0 | `DEFERRED_BUT_BLOCKING` if reached; separate PAYMENT lifecycle |
| `chooseBinary` | 2 reactive | `OTHER_DECISION_TYPE`; effect-specific binary domains |
| `payCostToPreventEffect` | 5 reactive / 24 proactive | `OTHER_DECISION_TYPE` / `DEFERRED_BUT_BLOCKING`; PAYMENT/prevention |
| bid | 0 / 0 | `NOT_REACHED`; separate BID family |
| replacement | 0 / 0 | `NOT_REACHED`; separate REPLACEMENT family |
| static application | 0 / 0 | `NOT_REACHED`; separate static/combat family |

The A3 slice does not make CONFIRMATION globally complete. The known `confirmAction`, `chooseBinary`, payment,
bid, replacement, and static families remain separate RandomLegalPolicy work.

### 25.21 Commander/generalization review

The context uses player/seat entities and retains three distinct roles:

```text
decider/viewer
triggering player
active player
```

It does not encode `SELF`/`OPPONENT`. The same shape can therefore be extended to more than two players, multiple
opponents, an affected player distinct from the decider, and several simultaneous public events. Simultaneous
events remain separated by trace-local occurrence identity. Multiplayer Commander rules are not implemented or
claimed by A3.

### 25.22 DECISION_TRACE_V2 compatibility

The candidate remains:

```text
REQUEST
  DecisionType.CONFIRMATION
  legalCandidates = [ACCEPT, DECLINE]

RESULT
  CHOSEN
```

The stable candidate order remains `ACCEPT`, then `DECLINE`. Mandatory triggers generate no request. The event
projection is compatible with `DECISION_TRACE_V2` at the trace level; A3 adds no V3 schema. The future
implementation must add or select an approved typed request-context slot without exposing raw Forge objects.

### 25.23 A3 architecture verdict

The dominant Gelectrode shape is now closed as a **slice-level architecture candidate**: its direct boolean
semantics, engine seam, public decision relevance, opaque-object invariance, fixed-perspective anti-aliasing,
hidden-information behavior, stable audit identity approximation, deterministic occurrence order, absent
continuation, exactly-once callback, state neutrality, RNG neutrality, and V2 trace semantics are all supported
by source and test-only evidence.

**A3 architecture verdict: `IMPLEMENT_GELECTRODE_OPTIONAL_TRIGGER_SLICE`.**

This verdict authorizes only a future implementation review for that named slice. It does not authorize production
CONFIRMATION code in A3, does not admit the other nine callbacks, and does not close global CONFIRMATION.

### 25.24 A3 verification evidence

Historical labels are preserved:

```text
K0/base regression              = 286
pre-A2 audit branch             = 287
post-A2 expanded selection      = 288
```

Current A3 results are reported separately:

| Gate | Executed | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Focused A1/A2/A3 plus `TriggerLifeGateTest` | 5 | 5 | 0 | 0 | 0 |
| Full reactor decision/determinism regression, successful run before final hidden-member assertion | 620 | 614 | 0 | 0 | 6 |
| Full reactor rerun after final hidden-member assertion | 620 | 613 | 1 | 0 | 6 |
| Isolated `NetworkPlayIntegrationTest.testServerStartAndStop` rerun | 1 | 0 | 1 | 0 | 0 |
| `FullGameCollectorNeutralityTest` | 1 | 1 | 0 | 0 | 0 |
| `WorkerIsolationSmokeTest` | 1 | 1 | 0 | 0 | 0 |

The successful full-suite run was `620/614/0/0/6` on the A3 branch before the final one-line test-fixture
hardening that added a hidden member to the adversarial collection. The final focused A3 selection reran that
hardening and remains fully green at `5/5/0/0/0`. The subsequent broad rerun and isolated network-test rerun
were blocked only by `java.net.BindException: Address already in use` on hard-coded Forge test port `55556`.
At the time of diagnosis, no listener held the port; Discord held it as an unrelated outbound ephemeral local
port, so no process was terminated and no environment-wide setting was changed. Package (`BUILD SUCCESS`),
configured `validate`/Checkstyle (`0` violations), and `git diff --check` all pass after the final changes. No
production implementation is part of A3.

## 26. FRL-02K-B1 production Gelectrode slice

### 26.1 Gate and scope

PR #13 was verified at reviewed head `f65b852fc5a755a93724633ab00f1fd511e84651` before merge. The exact-head
broad rerun recorded `620` tests, `613` passed, `1` failure, `0` errors and `6` skipped; the sole failure was
`NetworkPlayIntegrationTest.testServerStartAndStop`, a `java.net.BindException` on Forge test port `55556`.
The A3 focused selection remained green at `5/5/0/0/0`. Under the A3 rule this was accepted as
`INFRASTRUCTURE_FLAKE_ACCEPTED`. PR #13 was then merged as `86894c502bf1f7b6f0c736507506b7347b83db2e`.

B1 was implemented from that exact `origin/master` in isolated worktree
`C:\forgeAI-confirmation-b1`, branch `frl/02k-b1-gelectrode-confirmation`. No other confirmation profile,
ORDER, or DAMAGE_ASSIGNMENT path was added.

### 26.2 Exact production admission profile

The only admitted profile is `GELECTRODE_SPELL_CAST_UNTAP_SELF`. Admission requires the conjunction below:

```text
canonical source/rules identity = Gelectrode in CardStateName.Original
trigger = intrinsic, normal, non-static, optional
Mode = SpellCast
ValidCard = Instant,Sorcery
ValidActivatingPlayer = You
OptionalDecider = You
TriggerZones = Battlefield
Execute = TrigUntap
TrigUntap = DB$ Untap | Defined$ Self
no nonzero cost
source visible to decider
no active ActionContinuation
```

Card name alone, localized text, `Trigger.getId()`, Java identity, opaque triggering objects, and raw collections
are insufficient. Four public token copies in the canonical 17 are admitted only because their live trigger is
still intrinsic and matches the complete Gelectrode signature; token status is not treated as trigger provenance.
Copied/generated/granted trigger provenance remains rejected by the intrinsic/spawning-ability checks.

### 26.3 Typed context and identity separation

`ConfirmationDecisionContext` is immutable and contains exactly:

```text
ConfirmationTriggerProfile profile
ConfirmationEventType event = SPELL_CAST
CardSelectionCard sourcePublicIdentity
int triggeringPlayerId
int deciderPlayerId
```

The existing typed public-card identity is reused. Its `(cardId, gameTimestamp)` pair is runtime/entity
correlation only and is not trigger semantics or candidate semantics. The semantic trigger identity is the fixed
profile `GELECTRODE_SPELL_CAST_UNTAP_SELF`; candidate semantics are `ACCEPT` and `DECLINE`.

The context contains no raw `Card`, `CardLKI`, `SpellAbility`, `Trigger`, `WrappedAbility`, `Player`,
`GameEntity`, collection, localized description, runtime trigger ID, request ID, timestamp outside the typed
public-card identity, or agent-facing `occurrenceIndex`. Request IDs remain provider-local monotonic
infrastructure. Any trace-local request index is separate correlation metadata and is not a model feature.

The repository exposes Forge's view-based `GameEventSpellAbilityCast`, stack, and log surfaces, but does not yet
prove a ForgeRL player-perspective cast-history contract that correlates the concrete public cast to this seam.
The B1 observation result is therefore `OBSERVATION_HISTORY_GAP`; raw cast-card or LKI data was not added to
the context.

### 26.4 Engine seam, ownership, and candidates

The production seam is `forge-game/src/main/java/forge/game/trigger/WrappedAbility.java`,
`WrappedAbility.resolve()`, immediately around the existing decider callback. The controller-owned narrow
`ConfirmationDecisionProvider` performs admission, request generation, candidate validation, and application.
The Contraption helper's direct `confirmTrigger(new WrappedAbility(...))` call does not invoke `resolve()` and
therefore cannot enter B1. `PlayerControllerAi.confirmTrigger` was not globally instrumented.

An admitted request has the immutable candidate list, in this exact order:

```text
ACCEPT
DECLINE
```

Both candidates are legal and `forced=false`. A native teacher callback is called once and maps
`true -> ACCEPT`, `false -> DECLINE`. An explicitly installed external/test resolver supplies one candidate
directly. The two ownership paths are mutually exclusive. `ACCEPT -> true -> TrigUntap`; `DECLINE -> false`
returns before `TrigUntap`. Unknown, stale, wrong-type, cross-request, and cross-profile candidates fail closed.

Mandatory triggers generate no request. Cost-bearing optional triggers generate no generic CONFIRMATION request.
When an external resolver owns the decision, an unsupported profile raises
`UnsupportedConfirmationDecisionException` with the structured status/reason and no native fallback; this makes
the episode invalid instead of turning unsupported into an implicit `DECLINE`. Native teacher compatibility remains
unchanged when no external resolver is installed.

The admission also validates the live `WrappedAbility` effect, not only the static `TrigUntap` SVar: the live
ability must have `ApiType.Untap`, exactly `DB$ Untap | Defined$ Self` parameters, and no sub- or additional
ability branch. A matching card script with a mismatched live effect is rejected as `LIVE_EFFECT_MISMATCH`.

### 26.5 Hidden information, continuation, neutrality, and exactly-once

The projector rejects a face-down/hidden source or a source not visible to the decider and exports no raw LKI,
`SpellAbility`, collection, or hidden identity. Hidden/opaque adversarial fixtures fail closed as
`UNSUPPORTED_HIDDEN` or `UNSUPPORTED_PROFILE` without a request. Ordinary admitted Gelectrode resolution has
no non-null `ActionContinuation`; an active continuation makes the profile unsupported.

Supported, unsupported, cost-bearing, and hidden projection tests all assert unchanged Forge state fingerprints
and zero `DeterminismAuditRandom` draws. `FullGameCollectorNeutralityTest` and `WorkerIsolationSmokeTest`
passed. The two worker traces had identical gameplay, RNG, DECISION_TRACE_V2, and priority hashes, with zero
collisions and zero parse errors.

Each admitted ACCEPT and DECLINE path has one request, one provider choice, one applied boolean, and one native
effect decision. The provider now rejects a second choose or apply for the same request. ACCEPT performs one
untap; DECLINE performs none. No second `confirmTrigger`, duplicate `doTrigger`, target replay, or extra RNG
path is introduced.

The focused suite uses a real intrinsic-false derived-trigger fixture and the canonical fresh-JVM workload for
the three provenance-untrusted callbacks. A standalone Cipher encode/exile fixture is not currently available
without constructing the full encoded-card lifecycle; that remains a small test-fixture gap, while the
production provenance gate and the measured `3` rejected callbacks remain unchanged.

### 26.6 DECISION_TRACE_V2

Representative native-teacher trace records are:

```text
DECISION_TRACE_V2|REQUEST|0|13|MAIN1|0|CONFIRMATION|GELECTRODE_CONFIRMATION|0|false|[ACCEPT,DECLINE]|...
DECISION_TRACE_V2|RESULT|0|CHOSEN|DECLINE|true|true|false|false|false
```

The omitted tail is the existing trace correlation token; it is not a raw Forge object. The request contains
`CONFIRMATION`, `[ACCEPT,DECLINE]`, and `forced=false`; the result is one legal `CHOSEN` candidate. No V3
schema or localized/raw value was added. An external-policy result uses the same V2 schema with ownership flags
`nativeCallbackCompleted=false` and `mappingAttempted=false`:

```text
DECISION_TRACE_V2|RESULT|0|CHOSEN|ACCEPT|false|false|false|false|false
```

Such a result is valid trajectory/history but is explicitly excluded from `isBCPolicySample`; only a native
teacher callback with both flags true can produce a BC label.

### 26.7 Controlled workloads and the hard 17/26 invariant

The fresh-JVM runtime workloads produced:

| Workload | Raw callbacks | Admitted | Rejected/deferred | Results | ACCEPT | DECLINE | Confirmation trace requests/results |
|---|---:|---:|---:|---:|---:|---:|---:|
| Izzet Guild Kit vs Dimir Guild Kit, seed `20260810`, 10 games | 26 | 17 | 9 | 17 | 6 | 11 | 17 / 17 |
| Dead and Alive vs Air Forces, seed `20260809`, 10 games | 0 | 0 | 0 | 0 | 0 | 0 | 0 / 0 |

Reactive classification was exactly:

```text
Gelectrode admitted                         17
other normal optional no-cost, deferred     5  (UNSUPPORTED_PROFILE / CARD_IDENTITY)
cost-bearing optional, deferred              1  (UNSUPPORTED_COST / NONZERO_COST)
provenance-untrusted derived, deferred       3  (UNSUPPORTED_PROVENANCE / UNTRUSTED_PROVENANCE)
total                                       26
```

Helper admissions were `0`, mandatory requests were `0`, and continuation violations were `0`. No changes were
made to `confirmAction`, `confirmPayment`, `chooseBinary`, `payCostToPreventEffect`, `confirmBidAction`,
replacement, or static-application callbacks.

### 26.8 B1 blocker ledger

| Family | Current result |
|---|---|
| Gelectrode optional trigger | `SUPPORTED` — 17 reactive occurrences |
| Other normal optional no-cost triggers | `DEFERRED_BUT_BLOCKING` — 5 |
| Cost-bearing trigger | `PAYMENT / DEFERRED_BUT_BLOCKING` — 1 |
| `confirmAction` | `DEFERRED_BUT_BLOCKING` — 8 reactive observations; no B1 interception |
| `chooseBinary` | `OTHER_DECISION_TYPE / BLOCKING` — 2 reactive observations |
| `payCostToPreventEffect` | `PAYMENT / BLOCKING` — 5 reactive / 24 proactive observations |
| bid | `NOT_REACHED / separate family` — 0 / 0 |
| replacement | `NOT_REACHED / separate family` — 0 / 0 |
| static application | `NOT_REACHED / separate family` — 0 / 0 |

### 26.9 B1 verification totals

The final focused selection ran `40` tests: `7` in `forge-game` and `33` in `forge-gui-desktop`, with
`40` passed, `0` failed, `0` errors, and `0` skipped. The new Gelectrode provider tests are `19/19` after
the single-use request, external-ownership, and live-effect regression tests; the
fresh-JVM canonical workload test was `1/1`; collector neutrality and worker isolation were `2/2`.

The full decision/determinism reactor ran `641` tests: `635` passed, `0` failed, `0` errors, and `6` skipped.
The final package command and configured Checkstyle/Validate both returned `BUILD SUCCESS` with `0` Checkstyle
violations. `git diff --check` is clean.

### 26.10 FRL-02K-B1R architecture-review corrections

The B1R correction pass made exactly four scoped changes and did not add another confirmation profile:

```text
external unsupported profile -> UnsupportedConfirmationDecisionException / invalid episode
external CHOSEN result       -> valid history, not a BC teacher sample
native vs external trace     -> native=true/true; external=false/false
admission                    -> static script and live WrappedAbility effect must both match
```

The fresh focused suite and canonical workloads remain green after these corrections. The hard classifier result
is still exactly `17 admitted / 26 callbacks`; Lazav, Blood Operative, Cipher-derived callbacks, and all other
non-Gelectrode profiles remain outside the production adapter.

**B1R production verdict: `FRL_02K_B1_PASS`.** The Gelectrode slice is supported; global CONFIRMATION remains
open.

### 26.11 FRL-02K-B1R2 hidden-information correction

The B1R2 fix closes the unsupported-external error channel without changing admission or execution semantics:

```text
UnsupportedConfirmationDecisionException message
    Unsupported FRL-02K-B1 CONFIRMATION decision: <status> / <reason>
```

The propagated exception no longer receives or formats `WrappedAbility`, source-card names, source IDs,
descriptions, or other wrapper data. The hidden-source external-ownership test now asserts
`UNSUPPORTED_HIDDEN`, reason `UNSUPPORTED_HIDDEN`, zero resolver/native callbacks, and absence of the hidden
card name from the exception message. Any additional diagnostics must remain in an engine-internal channel;
the environment-visible failure contains only the typed status and public reason code.

The B1R2 focused selection is `40/40/0/0/0`; the full reactor is `641/635/0/0/6`. The exact `17/26`
classifier invariant, hidden-information boundary, native/external trace ownership flags, and all
exactly-once assertions remain unchanged.

**B1R2 production verdict: `FRL_02K_B1_PASS`.** The Gelectrode slice is supported; global CONFIRMATION remains
open. The `CardSelectionCard` identity naming remains a documented future cleanup and is not part of B1R2.

## 27. FRL-02K-C remaining confirmation and boolean boundary audit

FRL-02K-C is an audit and attribution milestone. It adds only opt-in, neutral callback diagnostics and focused
tests. It does not add a new CONFIRMATION profile, a generic `confirmAction` adapter, a BINARY implementation,
PAYMENT expansion, ORDER, DAMAGE_ASSIGNMENT, ObservationEncoder, or an RL player.

### 27.1 Corrected post-B1 checkpoint

The intentionally dirty primary checkout was preserved without reset, clean, checkout, stash, commit, rebase,
merge, or modification:

```text
primary checkout: C:\\forgeAI
branch: chore/decision-diagnostics-column-contract
HEAD: 7a0dea0ebb5b1ec8aac5c97d94c0a06c809471c5
expected user modifications: ML_STRATEGY.md and PriorityActionDiagnosticsTest.java
origin/master: ee4d46e5b41de0f9d07756e9f80de57e3479421e
```

The isolated audit worktree was created from exactly `origin/master`:

```text
worktree: C:\\forgeAI-confirmation-c
branch: frl/02k-c-remaining-confirmation-audit
HEAD: ee4d46e5b41de0f9d07756e9f80de57e3479421e
merge-base: ee4d46e5b41de0f9d07756e9f80de57e3479421e
working tree at creation: clean
```

The prior run stopped before worktree creation and produced no audit evidence. Its result is treated as
`PREVIOUS_RUN_ABORTED_BEFORE_AUDIT`, not as an architecture verdict.

### 27.2 Neutral runtime instrumentation

`BooleanCallbackAuditDiagnostics` is enabled only when `forge.booleanCallback.metricsFile` is set. It records
the exact callback family, immediate production caller, owner hint, public source marker, API/mode, typed
boolean shape, player seats, continuation state, provenance, native result, and trigger-only metadata. Trigger
metadata includes card state, `TriggerType`, sorted non-descriptive normalized parameters, `Execute`, live API,
intrinsic state, spawning-ability presence, triggering-object key names, and public source-controller seat.

The recorder never calls AI helpers, consumes RNG, resolves an effect, changes targets, or exports raw
`SpellAbility`, `Trigger`, `WrappedAbility`, `Card`, `CardLKI`, `GameEntity`, localized prompt, Java identity, or
trigger ID values. Hidden source cards become a typed `HIDDEN` marker. `CardLKI` and other engine-only values may
appear only as key names in the internal audit row; their values are never serialized and such context remains
unsupported for an agent-facing decision.

### 27.3 Canonical fresh-JVM runtime counts

The exact controlled workloads were rerun from this C branch in fresh child JVMs:

```text
reactive:  Izzet Guild Kit vs Dimir Guild Kit, 10 games, seed 20260810
proactive: Dead and Alive vs Air Forces, 10 games, seed 20260809
```

| Callback family | Reactive | Proactive | Observed boolean rows | Immediate runtime status |
|---|---:|---:|---:|---|
| `confirmTrigger` | 26 | 0 | 26 | 17 B1-admitted; 9 deferred by profile/cost/provenance |
| `confirmAction` | 8 | 0 | 8 | caller-owned, heterogeneous |
| `chooseBinary` | 2 | 0 | 2 | effect-specific `HeadsOrTails` |
| `payCostToPreventEffect` | 5 | 24 | 29 | payment/prevention |
| `confirmPayment` | 0 | 0 | 0 | not reached |
| `confirmBidAction` | 0 | 0 | 0 | not reached |
| `confirmReplacementEffect` | 0 | 0 | 0 | not reached |
| `confirmStaticApplication` | 0 | 0 | 0 | not reached |

The boolean audit therefore reconciles `41` reactive rows (`26 + 8 + 2 + 5`) and `24` proactive rows. No
observed callback row remains outside a family or cluster.

### 27.4 B1 regression lock

The same reactive run still produced the hard B1 classifier invariant:

```text
raw confirmTrigger-related callbacks: 26
Gelectrode admitted:                 17
other normal optional no-cost:        5
cost-bearing optional:                1
provenance-untrusted derived:         3
confirmation results:                17
```

The five normal no-cost rows, one cost-bearing row, and three derived rows were not newly admitted. B1 remains
the named `GELECTRODE_SPELL_CAST_UNTAP_SELF` slice with candidates `ACCEPT` and `DECLINE`; global
`CONFIRMATION` remains `OPEN`.

### 27.5 Remaining `confirmTrigger` attribution

The fresh rows were individually reconciled as follows. All rows were public-source rows, had no active
`ActionContinuation`, and had explicit native booleans. `source_controller`, `decider`, and active-player seats
are recorded as typed public seats; a blank triggering player means that no public player-valued triggering key
was present.

| Runtime cluster | Count | Trigger/rules identity | Live effect | Keys/provenance | Classification |
|---|---:|---|---|---|---|
| Gelectrode | 17 | `SpellCast`, `Mode=SpellCast`, `OptionalDecider=You`, `TriggerZones=Battlefield`, `ValidActivatingPlayer=You`, `ValidCard=Instant,Sorcery`, `Execute=TrigUntap`, `Original` | `LIVE_API=Untap` | public source; `Activator`, `Card`, `CardLKI`, cast/ability keys; intrinsic | `SUPPORTED B1` |
| Blood Operative | 2 | `ChangesZone`, Battlefield destination, `OptionalDecider=You`, `ValidCard=Card.Self`, `Execute=TrigChangeZone`, `Original` | `LIVE_API=ChangeZone` | `Card;CardLKI`; intrinsic; no cost; A/B/C target projections | `BLOOD_OPERATIVE_TARGET_OWNERSHIP_UNPROVEN` |
| Lazav, Dimir Mastermind | 3 | `ChangesZone`, Graveyard destination, `OptionalDecider=You`, `ValidCard=Creature.!token+OppOwn`, `Execute=LazavCopy`, `Original`; one Oracle `may` | `LIVE_API=Clone` plus `Optional$ True` | `Card;CardLKI`; intrinsic; no cost; trigger/effect surfaces duplicate one rule decision | `SAME_RULE_DECISION_DUPLICATED_BY_ENGINE_SURFACES` |
| Blood Operative | 1 | `Surveil`, Graveyard trigger zone, `PresentPlayer=You`, `IsPresent=Card.StrictlySelf`, `Execute=TrigReturn`, `Original` | `LIVE_API=ChangeZone` | `Player` key; intrinsic; `PayLife<3>` cost; no `OptionalDecider` parameter | payment-owned cost-bearing trigger |
| Nightveil Specter | 1 | `DamageDone`, `CombatDamage=True`, `OptionalDecider=You`, `ValidSource=Card.Self`, `ValidTarget=Player`, `Execute=PlayEncoded`, `Original` | `LIVE_API=Play` | public damage/source keys; `intrinsic=false`; derived/cipher | provenance blocker |
| Tibor and Lumia | 2 | same `DamageDone`/combat-damage family, `Execute=PlayEncoded`, `Original` | `LIVE_API=Play` | public damage/source keys; `intrinsic=false`; derived/cipher | provenance blocker |

The 5 normal no-cost rows are not a generic "optional and no-cost" admission. C1R keeps the narrow
`ChangesZone`-trigger audit split by the live `ChangeZone` versus `Clone` effect. Both carry `CardLKI` in the live
triggering-object map, but Lazav's `TriggeredCardLKICopy` consumes the triggering `Card`, not the separate LKI
value. Blood's current target identity is correlated through A/B/C value projections; target policy ownership is
still not established. Raw `CardLKI` presence does not prove that hidden information is policy-relevant: the
canonical current-card result remains a typed public projection, while hidden-origin coverage is still open.

The cost-bearing row is not a second generic accept/decline profile. `WrappedAbility.resolve()` reaches the
native `confirmTrigger` callback, and `PlayerControllerAi.confirmTrigger()` delegates to
`AiController.doTrigger(sa, false)`. The AI trigger path checks cost feasibility and the ability-specific
`willPayCosts` logic before the wrapped effect is allowed to resolve. In this run the native result was false,
so resolution returned before `playSpellAbilityNoStack` and no second payment callback was generated for that
occurrence. A future policy must preserve this ownership boundary and trace the cost-specific payment path
before exposing any payment intent.

The 3 derived/cipher rows are semantically optional trigger proceed/suppress callbacks, but they are not B1
profiles: their live trigger is `intrinsic=false`, generated/copy provenance, and their public-looking damage
keys do not establish a stable public source/definition/occurrence identity. They remain
`PROVENANCE_BLOCKER` candidates for a later cipher/generated-trigger audit.

### 27.6 `confirmAction` caller attribution

The exact current total is `8`, with all clusters summing to `8`:

| Cluster | Count | True means | False means | Owner |
|---|---:|---|---|---|
| `EncodeEffect#resolve`, `Stolen Identity`, API `Encode` | 4 | continue to exile/encode | abort the encode operation | caller-owned optional effect |
| `EncodeEffect#resolve`, `Call of the Nightwing`, API `Encode` | 1 | continue to exile/encode | abort the encode operation | caller-owned optional effect |
| `CloneEffect#resolve`, `Lazav, Dimir Mastermind`, API `Clone` | 3 | continue the clone branch | cancel the clone branch | caller-owned optional effect |

This surface is not a single confirmation semantic. The method's production callers include zone movement,
discard, selection, discover, sacrifice, roll/state branches, optional effects, and combat alternatives. The
observed 8 are specifically Encode and Clone branches; their prompts are localized at the caller and were not
exported. A generic `confirmAction -> CONFIRMATION` adapter is rejected. Future work must split at
`EncodeEffect#resolve` and `CloneEffect#resolve` (or another exact caller seam) and provide each caller's public
candidate context.

The private `HumanCostDecision.confirmAction(CostPart, String)` calls are not this `PlayerController.confirmAction`
surface. They delegate to `confirmPayment` and belong to PAYMENT.

### 27.7 `chooseBinary` attribution

The exact total is `2`, both from `FlipCoinEffect#flipCoin` for public `Stitch in Time` abilities with
`BinaryChoiceType.HeadsOrTails`. The boolean is not ACCEPT/DECLINE:

```text
true  -> call heads
false -> call tails
```

The later `chooseFlipResult` callback is a different result decision and was not reached in this workload. The
observed choice has a stable two-label domain and public source, but the production callback is also used by
tap/untap, odds/evens, direction, counter, time-travel, and roll effects. A general `BINARY_CHOICE` adapter is
not justified by the Java return type or these two rows. The future slice should be a typed flip-call decision,
or another explicitly named effect family, with semantic alternatives rather than localized prompt text.

### 27.8 `payCostToPreventEffect` attribution

The exact current total is `5` reactive and `24` proactive. Every row has the stable shape:

```text
true  -> pay the supplied cost and prevent/replace the effect
false -> do not pay, or the cost is not payable; allow the non-payment branch
```

Reactive:

```text
AbilityUtils.handleUnlessCost -> Syncopate / Counter: 5
```

Proactive:

```text
SacrificeEffect.resolve             -> Morinfen / Sacrifice: 3
AbilityUtils.handleUnlessCost      -> Disrupt / Counter: 1
AbilityUtils.handleUnlessCost      -> Hidden Horror / Sacrifice: 4
AbilityUtils.handleUnlessCost      -> Barrow Ghoul / Sacrifice: 1
AbilityUtils.handleUnlessCost      -> Waterspout Djinn / Sacrifice: 15
```

`AbilityUtils.handleUnlessCost` invokes the callback once per payer and then uses `alreadyPaid` and the
`UnlessSwitched`/sub-ability rules to decide which engine branch resolves. `SacrificeEffect` uses the same
callback for Echo and cumulative upkeep payment gates. This is a PAYMENT/prevention boundary, not generic
CONFIRMATION. The boolean may express a payment intent in a caller, but actual construction is the subsequent
`CostPayment` path. These rows are post-native-result attribution records, not a future pre-decision PAYMENT
context contract: they are written after the native payment path has evaluated feasibility and called
`CostPayment.payComputerCosts(...)`. A future PAYMENT-CLOSURE capture must be placed before that payment call.
It must decide whether an explicit `PAYMENT_INTENT` is needed; it must not expose a generic confirmation and
then ask the agent to pay the same cost a second time.

### 27.9 Unreached callback families

| Surface | Production owner/caller | Reactive | Proactive | Classification |
|---|---|---:|---:|---|
| `confirmPayment` | `PlaySpellAbility` cost-part branches and `HumanCostDecision` payment helper | 0 | 0 | `NOT_REACHED`; PAYMENT, not CONFIRMATION |
| `confirmBidAction` | `BidLifeEffect` loop; followed by `chooseNumber` bid amount | 0 | 0 | `NOT_REACHED`; separate multi-step BID family |
| `confirmReplacementEffect` | `ReplacementHandler.executeReplacement` optional replacement branch | 0 | 0 | `NOT_REACHED`; REPLACEMENT family |
| `confirmStaticApplication` | combat alternative damage assignment, `StaticAbilityManaConvert`, `StaticAbilitySurveilNum` | 0 | 0 | `NOT_REACHED`; STATIC_APPLICATION or DAMAGE_ASSIGNMENT |

No artificial workload was created to force these paths. Mandatory normal triggers remain engine-owned: the
`WrappedAbility.resolve()` decider gate is not entered when the engine constructs a mandatory wrapper with
`decider == null`. The controller callback instrumentation has a mandatory guard for diagnostic attribution,
but it does not manufacture a policy request.

### 27.10 ActionContinuation and context boundary

All `41` reactive and `24` proactive boolean audit rows had `ActionContinuation=false`. The canonical B1
admitted rows also had no active continuation. The recorder reads the existing
`PriorityActionDiagnostics.hasActiveActionContinuation()` marker and creates no top-level request, so the
current evidence leaves no continuation ambiguity.

If any future callback is reached inside an active continuation, the boolean must be represented inside that
existing action sequence and owned by its semantic family. A separate top-level confirmation would risk a
duplicate decision and must fail closed until the continuation contract is explicit.

The current public projection is deliberately incomplete for future policy use. Source names and player seats
are emitted only under Forge view visibility. Localized descriptions, raw callback parameters, hidden hand or
library identity, face-down identity, LKI values, raw ability/trigger/entity objects, opaque collections,
`Trigger.getId()`, Java identity, and prompt strings are not emitted. The 5 `CardLKI` trigger rows remain
unresolved pending context-relevance/public-replacement and history audits; raw `CardLKI` presence is not proof
of policy-relevant hidden information. The 3 generated/cipher rows remain provenance-blocked even though their
source cards were visible in this workload.

### 27.11 Semantic ownership matrix

| Surface | Runtime count | Semantic cluster | Correct owner | Binary? | Safe public context now? | Future action |
|---|---:|---|---|---|---|---|
| `confirmTrigger` | 17 | Gelectrode `SpellCast -> Untap` | named `CONFIRMATION` B1 profile | yes, ACCEPT/DECLINE | yes for the approved B1 DTO | supported B1; keep exact predicate |
| `confirmTrigger` | 5 | Blood/Lazav `ChangesZone`, no cost | Blood trigger surface; Lazav one rule `may` duplicated across trigger/Clone surfaces | yes at current Forge surfaces | public current `Card` observed; Blood target ownership remains unproven; Lazav `CardLKI` is not the Clone source | C1R corrected; no production C change |
| `confirmTrigger` | 1 | Blood Operative `Surveil -> ChangeZone`, `PayLife<3>` | PAYMENT/cost-gated trigger entry | not as a second generic request | public source only; cost context incomplete | PAYMENT closure; no duplicate CONFIRMATION |
| `confirmTrigger` | 3 | cipher-derived `DamageDone -> Play` | generated/provenance trigger family | yes procedurally | no stable provenance/context contract | future cipher/provenance audit |
| `confirmAction` | 8 | Encode (5) and Clone (3) caller branches | caller-owned effect semantics | yes per caller | not generic; prompts/context differ | C2a Encode, C2b Clone |
| `chooseBinary` | 2 | `FlipCoinEffect`, Heads/Tails | effect-specific binary choice | yes, heads/tails | yes for current public source | C3 typed flip-call slice |
| `payCostToPreventEffect` | 5 / 24 | unless/prevention, Echo, cumulative upkeep | PAYMENT/prevention | yes, pay/do-not-pay | requires typed cost/effect context | PAYMENT-CLOSURE |
| `confirmPayment` | 0 / 0 | cost-part payment confirmation | PAYMENT | unknown in this workload | not observed | defer until reached |
| `confirmBidAction` | 0 / 0 | continue bid plus numeric amount | BID | yes but multi-step | not observed | defer separate BID |
| `confirmReplacementEffect` | 0 / 0 | apply/decline replacement | REPLACEMENT | yes | not observed | defer replacement |
| `confirmStaticApplication` | 0 / 0 | static application or combat alternative | STATIC_APPLICATION/DAMAGE_ASSIGNMENT | yes | not observed | defer separate family |

Every observed callback row is represented exactly once by the rows above. The zero rows are explicitly
unreached, not inferred coverage.

### 27.12 Future implementation decomposition

No slice below is implemented by FRL-02K-C:

| Future slice | Exact seam and scope | Required context/blockers | Expected coverage | Zero-Unsupported relevance |
|---|---|---|---:|---|
| C1a | `WrappedAbility.resolve` narrow `Blood Operative ChangesZone -> ChangeZone` no-cost profile | A/B/C target correlation measured; A==C, but stored-target policy ownership remains unproven and hidden-origin coverage is absent | 2 reactive | audit evidence retained; production implementation remains blocked |
| C1b | `WrappedAbility.resolve` narrow `Lazav ChangesZone -> Clone` no-cost profile | one Oracle `may` duplicated across trigger/Clone surfaces; future owner is one caller-specific CloneEffect slice; raw `CardLKI` is not the Clone source; hidden-origin coverage is absent | 3 reactive | audit evidence retained; production implementation remains blocked |
| C1p | generated/cipher `DamageDone -> Play` provenance profile | stable generated source/definition/occurrence provenance and public damage context | 3 reactive | blocks current reactive zero-unsupported |
| C2a | `EncodeEffect.resolve` caller-owned optional encode branch | public host/available-encoder context; no localized prompt | 5 reactive | blocks current reactive zero-unsupported |
| C2b | `CloneEffect.resolve` caller-owned optional clone branch | chosen-card/target public identity and clone legality | 3 reactive | blocks current reactive zero-unsupported |
| C3 | `FlipCoinEffect.flipCoin` `HeadsOrTails` typed choice | semantic labels `HEADS`/`TAILS`; separate later result path | 2 reactive | blocks current reactive zero-unsupported |
| PAYMENT-CLOSURE | `AbilityUtils.handleUnlessCost`/`SacrificeEffect` through `CostPayment` | typed payer/cost/effect context; explicit intent-versus-construction contract | 5 reactive, 24 proactive, plus cost trigger closure | blocks both controlled workload slices |
| BID / REPLACEMENT / STATIC | their exact engine callers only when reached | family-specific typed context and continuation rules | 0 / 0 currently | does not block current slice |

The most important architectural result is negative: there is no evidence for a generic boolean-to-
`CONFIRMATION` adapter. The next implementation review should choose one small family and preserve the B1 named
profile unchanged.

### 27.13 DecisionType recommendations

```text
CONFIRMATION:
    keep global status OPEN; retain only GELECTRODE_SPELL_CAST_UNTAP_SELF as supported B1.

confirmAction:
    no generic DecisionType adapter. Use caller-specific Encode/Clone decision contracts if approved later.

chooseBinary:
    do not add general BINARY_CHOICE from the Java boolean alone. The observed stable candidate is a future
    typed FLIP_CALL / HEADS_TAILS family; other enum kinds must remain separate until observed.

payCostToPreventEffect and cost-bearing trigger:
    PAYMENT owns the boundary. Do not add generic CONFIRMATION + PAYMENT for the same event. Add an explicit
    PAYMENT_INTENT only if a future engine trace proves a distinct pre-payment strategic choice.

confirmPayment:
    existing PAYMENT family; defer until a controlled workload reaches it.

confirmBidAction:
    BID / continue-bid plus numeric amount, not generic confirmation.

confirmReplacementEffect:
    REPLACEMENT.

confirmStaticApplication:
    STATIC_APPLICATION for static effects and DAMAGE_ASSIGNMENT for combat alternatives.
```

### 27.14 Zero-Unsupported relevance

For the exact controlled workloads, the current blockers are the 5 remaining normal no-cost triggers, 1
cost-bearing trigger boundary, 3 derived/cipher triggers, 8 caller-owned `confirmAction` callbacks, 2
effect-specific binary callbacks, and 5 reactive/24 proactive payment-prevention callbacks. Solving these
families is required before RandomLegalPolicy can finish these workloads without Forge-AI fallback.

The zero-count `confirmPayment`, bid, replacement, and static-application surfaces do not block the current
workloads. They remain real future families rather than being declared complete. Mandatory normal triggers are
engine-owned and do not create a missing agent decision merely because the controller class contains a boolean
method.

### 27.15 FRL-02K-C test and neutrality evidence

The new fresh-JVM focused test exercised the exact reactive and proactive workloads, parsed the 28-column safe
audit projection, asserted all family totals and semantic clusters, checked the 17/26 B1 invariant, asserted
the 5 no-cost versus 1 cost-bearing trigger split, and verified no raw object identity or localized prompt
value was emitted. It compared audit-on and audit-off deterministic trace trees for the same reactive seed.

The existing B1 trigger-context tests separately assert unchanged `ForgeStateFingerprint` and zero
`DeterminismAuditRandom` draws for neutral projection work. The final focused selection passed `28` tests with
`0` failures, `0` errors, and `0` skips, including the collector and worker neutrality tests. The broad
`mvn -pl forge-gui-desktop -am test` reactor passed `642` tests with `636` passed, `0` failures, `0` errors,
and `6` existing stress/network skips. No `BindException` or port-55556 failure occurred. Package and validate
both completed with `BUILD SUCCESS`; all six reactor modules reported `0` Checkstyle violations, and
`git diff --check` is clean. No diagnostic callback invokes an AI helper or changes the game loop.

### 27.16 C-R1 architecture review correction (historical baseline)

The five no-cost `ChangesZone` rows were previously classified as `SAFE_NARROW_PROFILE_CANDIDATE` with
`PUBLIC_CONTEXT_PROJECTION_UNPROVEN` and `OBSERVATION_HISTORY_GAP`. This subsection is retained as the
pre-C1 baseline. Section 28 supersedes its C1a/C1b interpretation with the measured public Card relevance,
Lazav single-rule duplication, and Blood A/B/C target-ownership correction.

The `payCostToPreventEffect` rows are post-native-result attribution records, not a pre-decision PAYMENT context
contract. Future PAYMENT-CLOSURE capture belongs before `CostPayment.payComputerCosts(...)`. The historical review
disposition was `P0=0`, `P1=0`, `P2=1`; the current C1R disposition is recorded in section 28.

**FRL-02K-C audit verdict: `FRL_02K_C_PASS`.**

## 28. FRL-02K-C1 — ChangesZone Trigger Decision-Relevance, Public Event Projection, and History Audit

### 28.1 Gate and exact checkout

[BESTAETIGT] The C1 gate was evaluated after `git fetch origin` in the protected primary checkout. PR #15,
`FRL-02K-C: audit remaining confirmation and boolean boundaries`, was merged. Its reviewed head was
`2c17cf8d357bd3dc3da1a62111c050bac751841a`, its merge commit was
`72a574e7235002064f648b21d7b4387f8ac50be4`, and the current `origin/master` was the same merge commit.

The primary `C:\forgeAI` checkout remained on
`chore/decision-diagnostics-column-contract` with its pre-existing user modifications untouched. C1 used the
clean isolated worktree `C:\forgeAI-confirmation-c1` on
`frl/02k-c1-changeszone-trigger-projection`, with `HEAD == origin/master == 72a574e7235002064f648b21d7b4387f8ac50be4`
and a clean initial tree.

### 28.2 Audit-only scope

[BESTAETIGT] C1 added no `DecisionType.CONFIRMATION`, no new confirmation provider/profile, no generic
`confirmAction` adapter, no production `HistoryEvent`, and no public DTO. The only runtime output is enabled by
the opt-in `forge.changesZone.auditFile` property. The diagnostic recorder catches its own failures and does
not call an AI helper or choose a target.

The exact runtime profiles are deliberately narrow:

| Profile | Trigger | Executed effect | Reactive occurrences |
|---|---|---|---:|
| `BLOOD_OPERATIVE` | `ChangesZone -> ChangeZone` | `TrigChangeZone` | 2 |
| `LAZAV` | `ChangesZone -> Clone` | `LazavCopy` | 3 |

No other `ChangesZone` trigger is admitted by the recorder.

### 28.3 Decision-relevance and engine ownership

[STARKES INDIZ] `GameAction` supplies both the moved current `Card` and a `CardLKI` value when it raises the
`ChangesZone` trigger. `TriggerChangesZone` stores both keys, but for these `Origin$ Any` profiles its validity
test uses the current `runParams.Card`. The audit records the two values only as typed `Card` and `CardLKI`
presence/visibility metadata; it never exports either object or a value derived from raw object identity.

[BESTAETIGT] Lazav's `Defined$ TriggeredCardLKICopy` path is resolved by `AbilityUtils.getDefinedCards` through
the triggering `Card` key. The `CardLKI` key is present in the trigger map but is not consumed as the clone source
by this script. This is a profile-specific decision-relevance result, not a universal `ChangesZone` rule.

### 28.4 Public projection and information monotonicity

[BESTAETIGT] The C1 CSV projection contains only typed, safe fields: profile, trigger mode/parameters, effect
API, rule-defined context token, player seats, visibility markers, zone types, public card names when
`CardView.canBeShownTo` permits them, target ordering, result booleans, clone-state count deltas, and neutral
audit markers. `public_context_key` is built only from public name/zone/seat/type facts and excludes LKI values,
runtime IDs, timestamps, `Trigger.getId()`, Java identity, process IDs, wall-clock values, and RNG values.

`Card` and `CardLKI` remain distinct typed fields. Hidden-at-decision and previously-hidden states are explicit;
hidden card names and target names are represented by a marker. The canonical five-row set observed public
source/current-card context for this workload and did not manufacture a hidden-hand or hidden-library case.
That absence is a coverage limitation, not permission to generalize the projection to hidden-information policy.

### 28.5 Blood Operative target ordering

[BESTAETIGT] `PlayerControllerAi.confirmTrigger` temporarily clears the stored target choices, lets the existing
`ChangeZoneAi` evaluator consider Blood's graveyard target, and restores the original target choices before the
native callback returns. C1R records three separate value-only target projections:

| Projection | Audit event | Meaning |
|---|---|---|
| A | `STORED_TARGET_BEFORE_CONFIRM` | Target already stored on the triggered ability before the AI callback |
| B | `AI_TARGET_EVALUATION` | Temporary target produced while `brains.doTrigger()` evaluates the trigger |
| C | `CHANGE_ZONE_EFFECT_ENTER` | Target actually supplied to `ChangeZoneEffect` |

Both Blood occurrences were accepted in the canonical run and both entered/exited `ChangeZoneEffect`. A equals C
for both occurrences, proving that effect resolution uses the stored trigger target. A equals B only for the first
occurrence; the second has a different temporary AI target (`Electrolyze`) while the stored/effect target is
`Wee Dragonauts`. This proves the AI preselection surface is not the authoritative effect target and prevents
claiming that an external confirmation already owns Blood's target.

The C1R verdict is `BLOOD_OPERATIVE_TARGET_OWNERSHIP_UNPROVEN` / `BLOOD_OPERATIVE_AI_PRESELECTION_BLOCKER`.
No second generic confirmation was inserted and no target was changed by the recorder.

### 28.6 Lazav duplicate semantic choice

[BESTAETIGT] Lazav's accepted trigger enters `CloneEffect`, which then invokes its caller-owned optional
`confirmAction` for the optional copy. C1 records the enter/result pair and the subsequent clone-state change.
All three Lazav triggers were accepted; all three entered `CloneEffect` and invoked the second optional choice.
Two copy choices were accepted and changed clone state; one copy choice was declined and changed no clone state.

The Oracle text contains one Magic `may`. The Forge script exposes that one rule decision through two engine
surfaces: `OptionalDecider$ You` on the trigger and `Optional$ True` on `LazavCopy`. The observed
`confirmTrigger=ACCEPT` followed by `confirmAction=ACCEPT/DECLINE` therefore proves
`SAME_RULE_DECISION_DUPLICATED_BY_ENGINE_SURFACES`, not two independent Magic decisions. The Human path's two
prompts are additional evidence of the duplicate engine exposure, not a second rules-level choice.

The future ForgeRL owner is one decision slice and one training sample at the caller-specific `CloneEffect`
optional-copy seam, because that seam owns the specific creature card and the copy/no-copy outcome. Whether the
trigger-level check becomes procedural/auto-proceed or the card-script optionality is corrected is deferred; C1R
adds neither implementation.

### 28.7 Deterministic lifecycle and history audit

[BESTAETIGT] The five trigger occurrences received deterministic trace-local tokens `1` through `5`; tokens are
allocated in observed game order and do not use Java identity, process IDs, wall-clock time, RNG, or
`Trigger.getId()`. The fresh-JVM audit produced 37 lifecycle rows with the following exact event shapes:

```text
Blood 1: TRIGGER_ENTER > STORED_TARGET_BEFORE_CONFIRM > AI_TARGET_EVALUATION
          > CONFIRM_TRIGGER_RESULT
          > CHANGE_ZONE_EFFECT_ENTER > CHANGE_ZONE_EFFECT_EXIT > TRIGGER_EXIT
Lazav 2: TRIGGER_ENTER > CONFIRM_TRIGGER_RESULT > CLONE_EFFECT_ENTER
          > CLONE_CONFIRM_ACTION_ENTER > CLONE_CONFIRM_ACTION_RESULT
          > CLONE_STATE_CHANGED > CLONE_EFFECT_EXIT > TRIGGER_EXIT
Blood 3: same shape as Blood 1
Lazav 4: TRIGGER_ENTER > CONFIRM_TRIGGER_RESULT > CLONE_EFFECT_ENTER
          > CLONE_CONFIRM_ACTION_ENTER > CLONE_CONFIRM_ACTION_RESULT
          > CLONE_EFFECT_EXIT > TRIGGER_EXIT
Lazav 5: same shape as Lazav 2
```

The recorder is a diagnostic history projection only. No engine history event or continuation object was added;
the remaining architecture question is whether a future public event contract should carry this lifecycle and
which events are policy-relevant. C1 closes the observation gap for these five profiles but does not approve a
production history schema.

### 28.8 Timing, continuation, state, and RNG neutrality

[BESTAETIGT] Every emitted C1 row reported `ActionContinuation=false`, `state_neutral=true`, and
`rng_delta=0`. The audit-on and audit-off canonical reactive runs produced identical determinism trace trees.
Projection reads are performed around the existing engine calls; they do not enter the AI decision path, consume
RNG, mutate targets, or alter clone/zone effects.

### 28.9 C1 verification evidence

The focused fresh-JVM test ran the exact `Izzet Guild Kit` versus `Dimir Guild Kit` workload for 10 games with
seed `20260810`, and passed with `1` test, `0` failures, `0` errors, and `0` skips. It verified the two/three
profile counts, source/rule shapes, the single Lazav Oracle `may` against its two Forge surfaces, Card/CardLKI
typed projection, no raw-object or localized-prompt leakage, the five per-token lifecycle orderings, all Blood
A/B/C target projections (`A == C` twice; `A == B` once; `A != B` once), neutrality markers, and audit-on versus
audit-off determinism.

The inherited `FRL02KRemainingConfirmationAuditTest` remains the regression lock for B1 and the prior C callback
inventory: reactive `confirmTrigger=26`, B1 admitted `17`, unsupported-profile `5`, unsupported-cost `1`,
unsupported-provenance `3`, with `confirmAction=8`, `chooseBinary=2`, and `payCostToPreventEffect=5`.

The broad `mvn -pl forge-gui-desktop -am test` reactor passed `643` tests with `637` passed, `0` failures,
`0` errors, and `6` existing stress/network skips. The full run completed with `BUILD SUCCESS`; its warnings were
existing card-data, network-filter, and media-environment diagnostics, not C1 failures. The normal
`mvn -pl forge-gui-desktop -am package` run repeated the same `643/637/0/0/6` result, created the Forge jar,
executable, and bundled jar-with-dependencies, and completed with `BUILD SUCCESS`. The final
`mvn -pl forge-gui-desktop -am validate` run completed with `BUILD SUCCESS`; all six modules again reported zero
Checkstyle violations. After the final non-semantic projection cleanup, a final `-DskipTests package` and
`validate` also completed with `BUILD SUCCESS` on the exact handoff tree.

### 28.10 C1 disposition and blockers

[BESTAETIGT] Raw `CardLKI` presence is not sufficient evidence that a generic confirmation context must expose
LKI identity. For these two profiles, the decision-relevant current `Card` and the rule-defined
`TriggeredCardLKICopy` path are observable without raw LKI export.

[BESTAETIGT] Lazav is one Oracle `may` duplicated across trigger and CloneEffect engine surfaces, not two
independent Magic decisions. The future policy boundary must produce one request and one training sample.

[BLOCKER] Blood's stored target is the target used by `ChangeZoneEffect`, but its policy ownership is not proven:
the temporary AI evaluation can diverge from the stored target. Preserve
`BLOOD_OPERATIVE_TARGET_OWNERSHIP_UNPROVEN` until target selection ownership is explicitly moved or attributed.

[UNKLAERT] The result is not a universal proof for hidden-origin `ChangesZone` events, copied/granted triggers,
or other `ChangesZone` modes and effects. Hidden-information coverage, broader public replacement semantics, and
stable production history ownership remain separate audits.

[BLOCKER] Global `CONFIRMATION` remains OPEN. C1 is an audit PASS for the five named runtime occurrences, not a
production implementation approval and not a zero-unsupported result.

**FRL-02K-C1 audit verdict: `FRL_02K_C1_PASS` (evidence retained; C1R interpretation corrected).**

## 29. FRL-02K-C2 - Triggered TARGET Ownership and Stack-Time Decision Seam Audit (historical C2/C2R record; superseded by C2A)

### 29.1 Gate, checkpoint, and scope

[BESTAETIGT] The C2 gate was evaluated after `git fetch origin` in the protected primary checkout. PR #16,
`FRL-02K-C1: audit ChangesZone trigger public context`, is merged. The reviewed final head is
`d64a009958cda0b4a12f20d23097c8f066550bde`, the merge commit is
`f7120316e6d88953ff5f3f257f847429533b6abd`, and `origin/master` is
`4813d58039f3e10858d16dd74e7c5ef7d427e624`. `git merge-base --is-ancestor
f7120316e6d88953ff5f3f257f847429533b6abd origin/master` passed.

The protected `C:\forgeAI` checkout was not modified. C2 used the isolated worktree
`C:\forgeAI-triggered-target-c2` on `frl/02k-c2-triggered-target-ownership`, initially at
`HEAD == origin/master == 4813d58039f3e10858d16dd74e7c5ef7d427e624` with a clean tree. The scope is audit-only:
the new recorder is enabled only by `forge.triggeredTarget.auditFile`, and the added call sites observe existing
objects and return without selecting, applying, or reordering a target.

No `DecisionType` was added. No Blood script, Lazav script, `TargetDecisionProvider` production behavior,
`CONFIRMATION` profile, `PAYMENT`, `ORDER`, or `DAMAGE_ASSIGNMENT` behavior was changed. `ML_STRATEGY.md` was
not changed because C2 identifies a future seam but does not approve a durable production architecture.

### 29.2 Blood target shape and normalized Forge state

[BESTAETIGT] `forge-gui/res/cardsfolder/b/blood_operative.txt` defines the relevant trigger as:

```text
Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self
| Execute$ TrigChangeZone | OptionalDecider$ You
```

Its `TrigChangeZone` SVar is a `ChangeZone` effect with `Origin$ Graveyard`, `Destination$ Exile`,
`ValidTgts$ Card`, and a card target prompt. `AbilityFactory.adjustChangeZoneTarget` at
`forge-game/src/main/java/forge/game/ability/AbilityFactory.java:370-380` normalizes a non-player ChangeZone
target's zone from `Origin`; the C2 provider fixture therefore verifies the normalized `TgtZone$ Graveyard`
shape. The second Blood surveil ability uses `Defined$ Self` and is not part of this target audit.

### 29.3 Exact runtime call graph

[BESTAETIGT] The current AI path for the canonical Blood trigger is:

```text
TriggerHandler.runSingleTriggerInternal:457
  -> new WrappedAbility(regtrig, sa, decider):518
  -> MagicStack.addSimultaneousStackEntry:818
  -> MagicStack.chooseOrderOfSimultaneousStackEntry:844+
  -> PlayerControllerAi.orderAndPlaySimultaneousSa:1362+
  -> PlayerControllerAi.prepareSingleSa:1393+
  -> AiController.brains.doTrigger
  -> SpellAbilityAi.doTriggerNoCostWithSubs:189
  -> ChangeZoneAi.doTriggerNoCost:212
  -> ChangeZoneAi.knownOriginTriggerAI:1420
  -> ChangeZoneAi.isPreferredTarget:839
  -> sa.getTargets().add(choice):1209
  -> ComputerUtil.playStack
  -> MagicStack.add:251
  -> MagicStack.push:524
  -> MagicStack.resolveStack:564
  -> WrappedAbility.resolve:409
  -> PlayerControllerAi.confirmTrigger:438
  -> PlayerControllerAi.playSpellAbilityNoStack
  -> ChangeZoneEffect.resolve:446
```

[BESTAETIGT] The Human path is materially different at target policy but shares the normal stack-time
preparation boundary:

```text
MagicStack.chooseOrderOfSimultaneousStackEntry
  -> PlayerControllerHuman.orderAndPlaySimultaneousSa:2389
  -> PlaySpellAbility.playSpellAbility
  -> PlaySpellAbility.playAbility:677
  -> SpellAbility.setupTargets:2159
  -> PlayerControllerHuman.chooseTargetsFor:2426
  -> TargetSelection.chooseTargets:73
  -> TargetChoices.add
  -> PlaySpellAbility.playAbility:729
  -> MagicStack.addAndUnfreeze
```

The source map proves the Human and AI selection surfaces are not the same method. A headless Human GUI
interaction was not manufactured for this audit; the Human source path is confirmed, while its interactive
runtime replay remains a coverage limitation.

### 29.4 T0-T8 timing

| Checkpoint | Confirmed current lifecycle |
|---|---|
| T0 | `ChangesZone` trigger condition is raised with the current `Card` and trigger parameters. |
| T1 | `TriggerHandler.runSingleTriggerInternal` builds the underlying `SpellAbility`, assigns trigger data, wraps it in `WrappedAbility`, and queues the wrapper. |
| T2 | The `TrigChangeZone` ability already has its target restriction and one-card minimum/max before target preparation; ChangeZone origin normalization supplies Graveyard legality. |
| T3 | Target preparation starts in AI `prepareSingleSa -> brains.doTrigger`; Human starts in `SpellAbility.setupTargets -> chooseTargetsFor`. `ActionContinuation` is not active. |
| T4 | AI target A is first stored by `ChangeZoneAi.isPreferredTarget` after `sa.canTarget(choice)` and `TargetChoices.add`. Human stores the selected card in `TargetSelection` during `setupTargets`. |
| T5 | `ComputerUtil.playStack` calls `MagicStack.add`; Forge checks `hasLegalTargeting` and then `push`es the already-targeted ability. The C2 recorder emits before/after push. |
| T6 | `MagicStack.resolveStack` begins resolution and rechecks target legality through `hasFizzled`; this can remove an illegal target but does not make a new strategic choice. |
| T7 | `WrappedAbility.resolve` invokes the later optional `confirmTrigger`; Blood's native AI callback temporarily evaluates another target and restores A. |
| T8 | Accepted Blood resolution enters `ChangeZoneEffect.resolve`; `getDefinedCardsOrTargeted` consumes the stored target. C2 records C at effect entry. |

[WIDERLEGT] Target selection does not occur in `TriggerHandler` construction, in `MagicStack.add`, or at
resolution. Those stages construct, validate, lock, and consume the existing target choices respectively.

### 29.4.1 Native 0-target engine disposition

[BESTAETIGT] A focused native fixture closes the engine-side 0-target question; this is separate from the
provider's `INVALID_TARGETING` status. With a real `Blood Operative` on the battlefield and an empty Graveyard,
the underlying native triggered `SpellAbility` has a mandatory one-card target but zero legal candidates. The
native path is:

```text
SpellAbilityAi.doTrigger(sa, mandatory=true)
  -> TargetRestrictions.getNumCandidates(sa) == 0
  -> return sa.isTargetNumberValid() == false   (minimum target count = 1)
  -> PlayerControllerAi.prepareSingleSa(...) == false
  -> PlayerControllerAi.playTrigger(...) does not call ComputerUtil.playNoStack(...)
```

For a queued non-static trigger, `PlayerControllerAi.orderAndPlaySimultaneousSa` has the same guard:
`ComputerUtil.playStack(...)` is called only when `prepareSingleSa(...)` returns `true`. The native fixture
asserts `playTrigger(...) == false`, unchanged stack size, and no stored target. Therefore:

```text
0 legal required targets
  -> TARGET preparation fails
  -> trigger is not pushed onto the stack
  -> engine legality outcome, not unsupported-policy failure
```

This is the disposition that a future C2A external path must mirror when the provider returns
`INVALID_TARGETING`.

### 29.5 Exact origin and owner of target A

[BESTAETIGT] For the canonical AI workload, target A is first created at
`forge-ai/src/main/java/forge/ai/ability/ChangeZoneAi.java:1209`:

```text
list.remove(choice);
if (sa.canTarget(choice)) {
    sa.getTargets().add(choice);
}
```

The C2 recorder is placed immediately after that existing `TargetChoices.add` and does not participate in the
choice. The caller chain is `PlayerControllerAi.prepareSingleSa -> brains.doTrigger -> ChangeZoneAi` and the
target policy is `ComputerUtilCard.getBestAI(list)` at the selection branch. The legal domain was prepared by
`CardLists.getTargetableCards`, then filtered by `sa.canTarget` and ChangeZone-specific AI filters.

Therefore the current owner is an AI-specific target-preparation path, not a generic `TARGET` provider seam.
The exact game target A is authoritative for later resolution, but its strategic ownership is currently
`BLOOD_TARGET_OWNER_IDENTIFIED_BUT_AI_ENTANGLED`.

### 29.6 Legality versus strategic policy

[BESTAETIGT] Forge remains the legality authority. The relevant boundaries are:

```text
TargetRestrictions.getAllCandidates / target zone
CardUtil.getValidCardsToTarget
StaticAbilityMustTarget.filterMustTargetCards
SpellAbility.canTarget
min/max target count and TargetChoices
```

The current strategic selectors are separate: Human uses `TargetSelection`/controller input; AI uses
`ChangeZoneAi.isPreferredTarget` and `ComputerUtilCard.getBestAI`. The audit recorder never calls either selector.
The future contract must keep Forge candidate enumeration and legality in Forge while moving only strategic choice
to an external policy.

### 29.7 Existing TargetDecisionProvider compatibility

[BESTAETIGT] The existing provider can represent Blood's normalized underlying `SpellAbility` without a new
`DecisionType` or a trigger-specific candidate kind. `TargetDecisionProvider.generateTargetRequest` accepts a
targeting ability, uses Forge's candidate and MustTarget APIs, applies candidates through the normal
`TargetChoices.add`, and supports `continuation == null` explicitly. `TargetCandidateKind.TARGET_CARD` and the
existing request-local candidate-correlation key convention are sufficient for Graveyard cards.

The C2 provider test verified 0/1/many candidates, exact `CardUtil` candidate completeness, forced-one behavior,
null continuation context, state/RNG neutrality, and a face-down candidate with an empty visible name.

There is one small integration caveat: passing `WrappedAbility` directly to the provider would inherit the
wrapper's zero cost/root semantics because `WrappedAbility` delegates targeting but does not override every
cost/root accessor. The safe future call is on `wrapper.getWrappedAbility()` while applying to that same live
`TargetChoices`, with an explicit chooser (the Blood decider/activating player for this slice). This is an
orchestration/context extension, not a legality rewrite.

**Target provider verdict: `TARGET_PROVIDER_COMPATIBLE_WITH_SMALL_EXTENSION`.** The extension is the
trigger-stack-time orchestration and explicit underlying-ability/chooser context; the provider's legal target
model is reusable as-is for Blood.

### 29.8 ActionContinuation and occurrence correlation

[BESTAETIGT] C2 records `action_continuation=false` at target preparation, A storage, stack, confirmation, and
effect rows. A triggered ability's target is not a subdecision of the priority action that caused the trigger, so
`null` continuation is correct and no continuation was invented.

The C2 token is a diagnostic-only, deterministic, trace-local occurrence token allocated at wrapper construction
and bound to the wrapper, underlying ability, and sub-abilities by identity inside the engine recorder. It is not
exported as model semantics and is not `Trigger.getId`, Java identity, `hashCode`, PID, wall clock, or RNG.
Existing `DECISION_TRACE_V2` request ordering is sufficient for a future target request; C2 does not require a
trace-schema V3. A future public history contract may carry an engine-owned occurrence relation, but no such DTO
was added here.

### 29.9 Teacher mapping feasibility

[STARKES INDIZ] A future teacher map is architecturally possible: run the provider's complete legal candidate
enumeration before mutation, compute the same deterministic request-local candidate-correlation key, and map the
native Forge AI card A to exactly one `TARGET_CARD` candidate. That key is audit/runtime entity correlation only;
it is not a cross-run policy or training identity. The mapping can be done without a second AI call, target replay,
RNG, or target mutation. The current native AI target is a policy trace, not evidence that an external policy
already owns A; C2 adds no BC data or training implementation.

### 29.10 External ownership seam

[BESTAETIGT] The conceptual future path is:

```text
constructed triggered ability
  -> provider.generateTargetRequest(underlying SA, explicit chooser, null)
  -> external policy selects one legal candidate
  -> provider.apply
  -> TargetChoices contains A
  -> existing stack insertion
```

The current AI path must bypass or split `prepareSingleSa -> brains.doTrigger` for the strategic target portion;
otherwise the AI can preselect A or recompute B before the external policy. The legality calls remain in Forge.
The seam classification is `AI_TARGET_SELECTION_ENTANGLED` and `TARGET_PREPARATION_REQUIRES_REFACTOR`.

### 29.11 TARGET versus later CONFIRMATION

[BESTAETIGT] Blood has two distinct decisions: `TARGET` chooses which public Graveyard card, and the later
trigger-level `may` asks whether to perform the exile. A exists on the triggered ability even if the later may is
declined; accepted resolution uses A, and declined resolution has no `ChangeZoneEffect` entry. The C2 design
therefore does not collapse target selection into confirmation or duplicate a target choice in the confirmation
provider.

The current public `TargetDecisionContext` does not itself expose a stack-history relation. The selected card is
visible in Forge's stack-time state and is recorded by the opt-in C2 audit, but no ObservationEncoder-/History-
contract audit was performed and no production Observation/History event was added. C2 therefore establishes
`TARGET_VISIBILITY_AT_CONFIRMATION_UNPROVEN`, not `TARGET_HISTORY_EVENT_REQUIRED`. C2A must first verify whether
the resolving trigger and its public stored target are already available in the confirmation-time observation or
stack projection. Only if that existing contract cannot express the relation is the disposition
`OBSERVATION_OR_HISTORY_BRIDGE_REQUIRED`. A future confirmation context must not duplicate target identity when
the selected target is already public through the existing contract.

### 29.12 Human/AI parity and stack locking

[BESTAETIGT] Both Human and AI complete target preparation before `MagicStack.add` pushes the triggered ability,
but their policy owners differ. Human's common engine seam is `SpellAbility.setupTargets`; AI currently bypasses
that controller path and mutates `TargetChoices` in `ChangeZoneAi`.

`MagicStack.add` calls `hasLegalTargeting` before `push`; it does not choose. At resolution `hasFizzled` rechecks
the stored card against current legality and removes invalid choices according to Forge's fizzle rules. This is
legality revalidation, not a second strategic TARGET request. A future external path must select exactly once
before stack insertion and preserve the existing fizzle behavior.

### 29.13 0/1/many, completeness, and hidden information

[BESTAETIGT] The focused provider fixture measured:

| Legal Graveyard cards | Provider behavior |
|---:|---|
| 0 | `INVALID_TARGETING`; no impossible policy request is exposed. |
| 1 | `DECISION`, one `TARGET_CARD`, `isForced=true`; application completes through normal `TargetChoices`. |
| 2+ | `DECISION`, `isForced=false`; every Forge-legal card is offered in deterministic request-local candidate-correlation order. |

For the multi-card fixture, provider candidate IDs equal the sorted IDs from `CardUtil.getValidCardsToTarget`.
`TargetRestrictions`, `canTarget`, and MustTarget are retained as the legality sources. Public Graveyard cards
are emitted as public typed projections. A face-down candidate may remain legal but its candidate name is empty;
the C2 recorder emits `<HIDDEN>` and never raw `Card`, `CardLKI`, Java identity, localized prompt, or hidden name.
This confirms the public-information boundary for this fixture, not a universal hidden-zone claim.

The native engine disposition is independently confirmed by the focused fixture: zero mandatory legal targets
fail target preparation and do not reach stack insertion. This must remain distinct from the provider's
unsupported/environment status; `INVALID_TARGETING` is the provider-side representation of Forge's no-stack
legality outcome for this mandatory targeted trigger.

For C2A the three branches are therefore explicit:

```text
provider INVALID_TARGETING
  -> mirror Forge's no-stack legality outcome
provider forced single candidate
  -> apply automatically; no policy call and no BC sample
provider multiple candidates
  -> issue the real DecisionType.TARGET request
```

### 29.14 C2 lifecycle and A/B/C evidence

[BESTAETIGT] The opt-in CSV records the following per Blood occurrence:

```text
TRIGGER_CONSTRUCTED > TRIGGER_QUEUED > TARGET_STORED > TARGET_PREPARATION
> STACK_BEFORE_PUSH > STACK_AFTER_PUSH > RESOLVE_ENTER
> CONFIRM_TRIGGER_ENTER > TARGET_A_BEFORE_CONFIRM > TARGET_STORED
> TARGET_B_EVALUATION > CONFIRM_TRIGGER_RESULT
> EFFECT_ENTER > EFFECT_EXIT > RESOLVE_EXIT
```

The second `TARGET_STORED` is the existing AI helper's temporary B mutation after `confirmTrigger` cleared A;
it is not a second game target. In the canonical ten-game workload, two Blood occurrences were correlated,
both accepted, A equaled C twice, and A/B diverged once. `EFFECT_ENTER` is the C comparison point because
`EFFECT_EXIT` observes the same card after its zone has changed to Exile. This preserves the C1R result and
classifies B as evaluation-only.

The diagnostic `target_order` value is a request-local/runtime entity correlation key of the form
`TARGET_CARD|zone|cardId|gameTimestamp`. It is suitable for ordering and correlating rows within this audit;
`(cardId, gameTimestamp)` is not a cross-run policy/training identity and is not a production semantic contract.

### 29.15 Exactly-once and observation/history contract

[BESTAETIGT] The current native lifecycle has one authoritative game target A, one later may decision, and one
effect consumption C per accepted trigger. It also performs one temporary AI B evaluation during native
confirmation. External ownership must keep the first three operations as exactly one target selection, one stored
target, and zero target reselection during confirmation; B must not be treated as a second game decision.

The minimum future public bridge is:

```text
CURRENT_OBSERVATION: source, decider, public legal candidates, selected public stack target
STACK_PUBLIC_STATE: the triggered stack item and its stored TargetChoices projection
PUBLIC_HISTORY_EVENT: conditional occurrence relation only if observation/stack projection cannot expose A
DECISION_LOCAL_CONTEXT: target group/min/max and null continuation
ENGINE_ONLY: raw Card/CardLKI references and the diagnostic scope map
```

No ObservationEncoder or production HistoryEvent was changed in C2. The history bridge remains conditional on
the unperformed confirmation-time visibility audit; C2 does not require a new `HistoryEvent`.

### 29.16 Required target ownership matrix

| Stage | Engine object/state | Current setter/owner | Strategic decision? | Future owner | Agent-facing? |
|---|---|---|---|---|---|
| trigger construction | `SpellAbility` inside `WrappedAbility` | `TriggerHandler.runSingleTriggerInternal` | No | Forge | No |
| legal enumeration | `TargetRestrictions`/candidate list | Forge APIs plus AI filters | Legality only | Forge legality oracle | Yes, via provider |
| target A creation | underlying `TargetChoices` | `ChangeZoneAi.isPreferredTarget` / Human `TargetSelection` | Yes | external TARGET policy after seam split | Yes |
| temporary B | reset/repopulated `TargetChoices` | `PlayerControllerAi.confirmTrigger -> brains.doTrigger` | AI evaluation only | none; must be skipped for external ownership | No |
| stack insertion | `MagicStack`/`SpellAbilityStackInstance` | `MagicStack.add -> push` | No | Forge | Stack state yes |
| confirmTrigger | wrapper confirmation callback | decider controller | Yes, may/decline | CONFIRMATION policy | Yes, separately |
| effect target C | same underlying `TargetChoices` consumed by effect | `ChangeZoneEffect` | No | Forge effect resolution | Result/history only |

### 29.17 TargetDecisionProvider compatibility matrix

| Requirement | Current provider | Blood triggered target | Result |
|---|---|---|---|
| targeting ability supported | `usesTargeting`, `TargetRestrictions` | normalized one-card `ChangeZone` | PASS |
| chooser known | explicit `Player` argument; context carries chooser ID | decider/activating player known, AI `targetingPlayer` field currently unset | SMALL CONTEXT EXTENSION |
| complete legal candidates | `getAllCandidates` + `CardUtil` + `canTarget` | Graveyard cards complete in fixture | PASS |
| visibility safe | `canBeShownTo`, blank face-down name | public Graveyard / hidden-name test | PASS |
| min/max targets | provider and Forge target count | 1/1 | PASS |
| MustTarget | existing static filter | retained | PASS |
| nullable continuation | explicit null support | C2 T3/T4 false/null | PASS |
| candidate application | normal `TargetChoices.add` | underlying SA only | PASS |
| deterministic order | semantic key sort | stable card order | PASS |
| teacher mapping | native A can match one candidate key | feasible, not implemented | FEASIBLE |
| external ownership | no orchestration hook yet | AI preselection remains in path | BLOCKED BY SEAM |

Final compatibility: `TARGET_PROVIDER_COMPATIBLE_WITH_SMALL_EXTENSION`.

### 29.18 Regression, neutrality, and zero-unsupported impact

[BESTAETIGT] The baseline reactor run before C2 changes was `643 run = 637 passed + 6` existing skips, with
`0` failures and `0` errors. The focused C2/C1/B1 regression selection after C2R was `37 run = 37 passed`, with
`0` failures, `0` errors, and `0` skips. The post-C2R full-reactor run was `647 run = 641 passed + 6` existing skips, with `0` failures and `0` errors.
The focused C2 lifecycle test passed its fresh-JVM audit-on/audit-off ten-game workload; the provider audit passed
its three tests, including the native 0-target fixture. C2's audit-on and audit-off determinism trace trees matched,
every C2 row reported
`state_neutral=true`, `rng_delta=0`, and `action_continuation=false`.

The inherited B1/C/C1 locks remain authoritative: `confirmTrigger=26`, Gelectrode admitted `17`, other no-cost
`5`, cost-bearing `1`, provenance-untrusted `3`; `confirmAction=8`, `chooseBinary=2`,
`payCostToPreventEffect=5`; Blood `2`, Lazav `3`; C1R A==C twice and one reproducible A/B divergence. The
provider audit adds no admission profile and does not change the zero-unsupported status of global
`CONFIRMATION`; it is a target ownership audit, not a production provider rollout. Post-change
`mvn -pl forge-gui-desktop -am -DskipTests package` and `validate` both passed; validate reported zero
Checkstyle violations in all six reactor modules.

### 29.19 Historical C2 disposition: recommended next milestone and blockers (superseded by C2A)

[HISTORICAL C2 DISPOSITION] At the C2 checkpoint, the next safe milestone was `FRL-02K-C2A IMPLEMENT_TRIGGERED_TARGET_PROVIDER_SEAM`, with priority:

1. Split triggered target preparation from `brains.doTrigger` for the external-owner path.
2. Call the existing provider on the underlying triggered `SpellAbility` with explicit chooser and null
   continuation, retaining Forge legality and TargetChoices application.
3. Preserve Human `setupTargets`, native AI behavior, stack legality, fizzle rechecks, and B-free external
   confirmation evaluation as separate regression paths.
4. Only after that seam is reviewed, audit/implement a Blood confirmation slice.

At that historical C2 checkpoint, `REMOVE_AI_TARGET_OWNERSHIP_FROM_TRIGGER_PREPARATION` was the implementation
substance of C2A, not an approved C2 change. Blood production support remained blocked until that seam had
architecture approval. The seam is now the narrow, validated C2A profile recorded in the current authority above;
no production correctness fix was required to complete C2.

### 29.20 Historical C2 verification verdict (superseded by C2A)

[HISTORICAL C2 VERDICT] Target A origin, timing, current owner, B non-authority, C correlation, Human/AI parity,
provider compatibility, native 0-target no-stack disposition, 0/1/many behavior, null continuation,
hidden-information boundary, state/RNG neutrality, and the next milestone are all established for the requested
Blood slice. Confirmation-time target visibility remains unproven; C2 does not establish a mandatory history event.

**Historical FRL-02K-C2 audit verdict: `FRL_02K_C2_PASS` (audit-only; its pre-C2A Blood TARGET production blocker is superseded by the current FRL-02K-C2A exact-profile status above).**
