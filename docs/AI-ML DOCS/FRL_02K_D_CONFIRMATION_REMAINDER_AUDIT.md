# FRL-02K-D — Remaining CONFIRMATION Attribution and Closure Audit

Status: FRL_02K_D_PASS

This is an audit result, not a global CONFIRMATION implementation. The audit closes the current controlled-v0 boolean-callback ledger, retains B1 and C2A ownership, removes false generic-confirmation gaps, and identifies one exact next profile:

~~~
NEXT:
FRL-02K-D1
IMPLEMENT_BLOOD_ETB_CONFIRMATION_SLICE
~~~

The D1 slice is safe to implement only with its narrow public Target-A projection. It must not become a generic callback adapter, a second target decision, or a replacement for PAYMENT, PLAY, or CLONE semantics.

Audit date: 2026-08-12

Repository: chrismaghuhn/forgeAI

Architecture authority:

~~~
docs/AI-ML DOCS/ML_STRATEGY.md
docs/AI-ML DOCS/FRL_02K_CONFIRMATION_AUDIT.md
docs/AI-ML DOCS/FRL_02K_C2A_TRIGGERED_TARGET_AUDIT.md
~~~

Evidence labels:

- [BESTAETIGT] — directly established by current source or a passing current focused test.
- [STARKES INDIZ] — reproducible runtime/source evidence with a remaining boundary assumption.
- [BLOCKER] — must be resolved before the named future profile is implemented safely.
- [WIDERLEGT] — the proposed interpretation is contradicted by current source/runtime evidence.

## 1. Checkpoint and worktree

The protected checkout C:\forgeAI was clean but was one commit behind origin/master at the start of this milestone. It was not modified. The existing C2A worktree C:\forgeAI-triggered-target-c2a was also not modified.

The isolated audit worktree is:

~~~
path:          C:\forgeAI-confirmation-remainder
branch:        frl/02k-d-confirmation-remainder-audit
HEAD:          8b498edeaa5f4aac4505e3d2ab3b84ebc231cbbd
origin/master: 8b498edeaa5f4aac4505e3d2ab3b84ebc231cbbd
merge-base:    8b498edeaa5f4aac4505e3d2ab3b84ebc231cbbd
~~~

[BESTAETIGT] The worktree was clean before the audit plan was added. No Magic card script, production decision provider, ObservationEncoder, HistoryEvent, or learner code was changed by FRL-02K-D.

## 2. Scope and non-goals

This milestone answers which runtime boolean surfaces are:

~~~
SUPPORTED_CONFIRMATION
FUTURE_CONFIRMATION_PROFILE
ENGINE_OWNED
PAYMENT
FLIP_CALL
BID
REPLACEMENT
STATIC_APPLICATION
TARGET_RELATED_BUT_NOT_A_TARGET_DECISION
CALLER_OWNED_SEMANTIC_DECISION
DUPLICATE_ENGINE_SURFACE
NOT_AGENT_RELEVANT
UNREACHED
~~~

UNREACHED below always means UNREACHED_IN_V0_WORKLOAD; it does not mean globally irrelevant or safe.

This milestone does not implement:

~~~
global CONFIRMATION
Blood CONFIRMATION
generic CONFIRMATION ownership
new TARGET profiles
PAYMENT completion
FLIP_CALL
BID
REPLACEMENT
STATIC_APPLICATION
ORDER
DAMAGE_ASSIGNMENT
ObservationEncoder
HistoryEvent redesign
~~~

## 3. Canonical workloads and method

The current-head runtime inventory used the existing opt-in, fresh-JVM harness in forge-gui-desktop/src/test/java/forge/view/FRL02KRemainingConfirmationAuditTest.java. Each simulated run launches forge.view.Main sim in a child JVM, writes audit output to a run-local temporary path, and deletes that path after assertions complete.

| Run | Decks | Games | Seed | Purpose |
|---|---|---:|---:|---|
| reactive-audit | Izzet Guild Kit vs Dimir Guild Kit | 10 | 20260810 | Required canonical boolean inventory with audit enabled |
| reactive-control | Izzet Guild Kit vs Dimir Guild Kit | 10 | 20260810 | Audit-disabled determinism control |
| proactive-audit | Dead and Alive vs Air Forces | 10 | 20260809 | Payment/prevention comparison workload |

[BESTAETIGT] The canonical reactive run reproduced the current merged-master counts. The proactive run was used only to expose the payment/prevention surface; it was not substituted for the required reactive canonical workload.

## 4. Raw callback totals

### 4.1 Reactive canonical run

~~~
total boolean callbacks = 41
confirmTrigger           = 26
confirmAction            = 8
confirmPayment           = 0
confirmBidAction         = 0
confirmReplacementEffect = 0
confirmStaticApplication = 0
chooseBinary             = 2
payCostToPreventEffect   = 5
~~~

The family sum is exact:

~~~
26 + 8 + 0 + 0 + 0 + 0 + 2 + 5 = 41
~~~

### 4.2 Proactive comparison run

~~~
total boolean callbacks = 24
confirmTrigger           = 0
confirmAction            = 0
confirmPayment           = 0
confirmBidAction         = 0
confirmReplacementEffect = 0
confirmStaticApplication = 0
chooseBinary             = 0
payCostToPreventEffect   = 24
~~~

The family sum is exact:

~~~
0 + 0 + 0 + 0 + 0 + 0 + 0 + 24 = 24
~~~

These are current measurements, not the historical 70-callback aggregate. Historical C-stage counts remain historical evidence in the earlier audit and are not used as current runtime assertions.

## 5. Opt-in diagnostics and safety properties

The callback recorder is forge-game/src/main/java/forge/game/decision/BooleanCallbackAuditDiagnostics.java.

[BESTAETIGT] It is disabled unless forge.booleanCallback.metricsFile is set. When enabled it:

- records value-only CSV fields and never calls a chooser or AI helper;
- projects source visibility as PUBLIC, HIDDEN, or NONE;
- emits <HIDDEN> for inaccessible source identities;
- excludes raw Card, CardLKI, SpellAbility, WrappedAbility, GameEntity, localized prompts, and JVM identities;
- uses a worker/JVM-local synchronized event list and a run-local output path;
- catches diagnostic projection failures so they cannot alter the callback/game path;
- writes only at process shutdown.

The Boolean CSV has 28 fields:

~~~
family, immediate_caller, owner_hint, source_name, visibility,
ability_api, mode, choice_kind, mandatory, optional_param, cost_bearing,
decider_player, affected_player, active_player, triggering_player,
candidate_shape, action_continuation, provenance, native_result,
card_state, trigger_type, normalized_trigger_params, execute,
live_wrapped_effect, intrinsic, spawning_ability, triggering_object_keys,
source_controller
~~~

[BESTAETIGT] The fresh audit passed the safe-projection assertions and produced identical audit-enabled/audit-disabled reactive determinism trees. The separate C1/C2/C2A diagnostics additionally fingerprint the Blood ChangesZone lifecycle and assert state_neutral=true, rng_delta=0, and no continuation for the exact target profile. The Boolean recorder itself is observational; its source path does not invoke RNG or mutate Forge state.

Qualification: the current Boolean CSV does not add a per-row state fingerprint or a process-id column. Worker locality is established by child-JVM isolation and run-local paths, while per-row state/RNG evidence for Blood comes from the specialized ChangesZone/target diagnostics. No stronger global parallel-worker claim is made.

## 6. confirmTrigger breakdown

The 26 reactive rows reconcile exactly:

| Source/profile | API | Trigger mode | Count | Current semantic result |
|---|---|---|---:|---|
| Gelectrode SpellCast -> Untap Self | Untap | SpellCast | 17 | SUPPORTED_CONFIRMATION / B1 |
| Blood Operative ETB target effect | ChangeZone | ChangesZone | 2 | FUTURE_CONFIRMATION_PROFILE after C2A TARGET |
| Blood Operative Surveil return | ChangeZone | Surveil | 1 | PAYMENT |
| Lazav copy trigger | Clone | ChangesZone | 3 | DUPLICATE_ENGINE_SURFACE |
| Cipher-derived Nightveil Specter | Play | DamageDone | 1 | CALLER_OWNED_SEMANTIC_DECISION |
| Cipher-derived Tibor and Lumia | Play | DamageDone | 2 | CALLER_OWNED_SEMANTIC_DECISION |
| Total |  |  | 26 |  |

All 26 rows were optional (mandatory=NO) and had action_continuation=false. The cost-bearing Blood Surveil row was the only cost_bearing=YES trigger row. Gelectrode, Blood ETB, Lazav, and Cipher rows were no-cost trigger wrappers.

The immediate runtime caller for the 26 observed rows was forge.game.trigger.WrappedAbility#resolve. Source also contains the AI helper path PlayerControllerAi.chooseContraptionsToCrank, which constructs a temporary wrapper and calls confirmTrigger; it was not reached by this workload and is not admitted by this audit.

## 7. confirmAction breakdown

The 8 reactive rows reconcile exactly:

| Caller | Source/profile | Count | Oracle-level question | Classification |
|---|---|---:|---|---|
| EncodeEffect#resolve | Stolen Identity | 4 | May the spell be exiled and encoded, followed by creature selection? | CALLER_OWNED_SEMANTIC_DECISION / CIPHER_ENCODE |
| EncodeEffect#resolve | Call of the Nightwing | 1 | May the spell be exiled and encoded, followed by creature selection? | CALLER_OWNED_SEMANTIC_DECISION / CIPHER_ENCODE |
| CloneEffect#resolve | Lazav, Dimir Mastermind | 3 | May Lazav become a copy of the triggering creature card? | CALLER_OWNED_SEMANTIC_DECISION / CLONE_OPTIONAL_COPY |
| Total |  | 8 |  |  |

PlayerController.confirmAction overloads are forwarding conveniences, not additional decisions. The caller owns the richer semantics. Encode acceptance is followed by a creature selection; Clone acceptance applies copy state. A future policy request cannot safely be inferred from the method name or reduced to one global adapter.

## 8. chooseBinary breakdown

The 2 reactive rows are both:

~~~
Stitch in Time -> FlipCoinEffect#flipCoin -> HeadsOrTails
~~~

Classification: FLIP_CALL.

[BESTAETIGT] FlipCoinEffect calls chooseBinary for the player's heads/tails call before it draws MyRandom.getRandom().nextBoolean(). The call is not the random outcome. The legal semantic candidates are HEADS and TAILS, not ACCEPT and DECLINE. chooseFlipResult is a separate result-selection path and is not the Stitch-in-Time call boundary.

## 9. payCostToPreventEffect breakdown

The reactive and proactive rows are payment/prevention decisions, not generic confirmation.

| Workload | Caller | Source/cost shape | Count | Classification |
|---|---|---|---:|---|
| Reactive | AbilityUtils#handleUnlessCost | Syncopate / counter payment | 5 | PAYMENT |
| Proactive | SacrificeEffect#resolve | Morinfen / sacrifice payment | 3 | PAYMENT |
| Proactive | AbilityUtils#handleUnlessCost | Hidden Horror / sacrifice payment | 4 | PAYMENT |
| Proactive | AbilityUtils#handleUnlessCost | Waterspout Djinn / sacrifice payment | 15 | PAYMENT |
| Proactive | AbilityUtils#handleUnlessCost | Barrow Ghoul / sacrifice payment | 1 | PAYMENT |
| Proactive | AbilityUtils#handleUnlessCost | Disrupt / counter payment | 1 | PAYMENT |
| Totals |  |  | 5 / 24 |  |

The semantic candidates are pay/do-not-pay, with Forge cost affordability and payment legality authoritative. A generic [ACCEPT, DECLINE] trigger request would duplicate or bypass PAYMENT ownership. This is consistent with FRL-02C compatibility and does not claim PAYMENT completion.

## 10. Blood Operative ETB confirmation audit

### 10.1 Exact Forge call and Oracle relation

The card definition is forge-gui/res/cardsfolder/b/blood_operative.txt:

~~~
T: ... Destination$ Battlefield ... OptionalDecider$ You ... Execute$ TrigChangeZone
SVar:TrigChangeZone:DB$ ChangeZone | Origin$ Graveyard | Destination$ Exile | ValidTgts$ Card
~~~

[BESTAETIGT] The Oracle-level choices are separate:

~~~
Target A: choose one legal graveyard card for the ETB ChangeZone effect.
Confirmation: may proceed with the already-targeted effect.
~~~

The C2A provider/coordinator owns Target A. The later Boolean belongs semantically at WrappedAbility.resolve, after the target has been selected and before playSpellAbilityNoStack resolves the effect. It is not another TARGET request.

### 10.2 Target A, temporary Target B, and resolution Target C

The current native path in forge-ai/src/main/java/forge/ai/PlayerControllerAi.java:

1. saves the live target list A;
2. clears the live targets;
3. calls brains.doTrigger(sa, false), which may evaluate temporary target B;
4. uses B only to produce the native Boolean;
5. restores A before returning from the callback.

The resolving ChangeZone effect then consumes the restored live target C. Existing C1/C2/C2A evidence establishes:

~~~
A is the C2A externally/native-selected target.
B is AI-only temporary evaluation state.
C is the target consumed by the authoritative effect.
~~~

The native audit observed both A=B and A!=B cases while preserving the stored target through resolution. B can affect the native Boolean result because `brains.doTrigger` evaluates it, but it is still only temporary AI evaluation state: it must not become an agent decision or overwrite A. It can be eliminated from the externally owned path only when D1 takes ownership at the exact confirmation seam.

[BESTAETIGT] An external C2A target resolver is called once, the native target callback is not called, exactly one target is applied, and the same target is consumed by the effect. The existing target context has continuation=null, decisionSequenceId=null, and subdecisionIndex=null.

### 10.3 Exact profile and candidates

For D1, the exact Blood confirmation profile is:

~~~
source                    = Blood Operative, Original state, public
trigger                   = intrinsic ChangesZone -> Battlefield
decider                   = You
live effect               = ChangeZone Graveyard -> Exile
cost                     = none / free
trigger optional decider  = You
live ChangeZone Optional  = false
Target A                  = already selected by C2A
confirmation candidates   = [ACCEPT, DECLINE]
~~~

The [ACCEPT, DECLINE] candidates answer only whether to proceed with the already-targeted effect. They must not select, reselect, or enumerate a graveyard card.

### 10.4 External ownership readiness

The semantic boundary is the trigger's optional yes/no decision, surfaced by the `confirmTrigger` callback. `WrappedAbility.resolve` is the correct production ownership seam for a future exact Blood provider because it sees the already-selected target before the wrapped effect resolves; no second trigger or target seam is required. When an external resolver is active, the provider must:

- admit only this exact Blood profile;
- expose a value-only public Target-A projection if the policy needs target context;
- call the external resolver once;
- skip the native brains.doTrigger temporary Target-B evaluation;
- preserve the already-applied Target A through resolution;
- map the resolver result only to ACCEPT or DECLINE;
- leave Forge legality, fizzle, stack, and effect resolution authoritative;
- reject active ActionContinuation rather than inventing continuation metadata.

[BLOCKER] The current ConfirmationDecisionContext contains source, event, and player IDs for Gelectrode but no Blood-specific public projection of Target A’s public identity/zone. The next slice must add that narrow exact-profile context or fail closed. It must not add a generic observation/history redesign.

Decision:

~~~
BLOOD_CONFIRMATION_SAFE_NEXT_SLICE
~~~

The profile is safe as the next exact implementation slice, with the narrow Target-A context projection as an explicit D1 gate. Blood CONFIRMATION remains OPEN in the current code.

## 11. Blood Surveil cost-bearing trigger

The second Blood trigger is:

~~~
T: Mode$ Surveil ... Execute$ TrigReturn ... PresentZone$ Graveyard
SVar:TrigReturn:AB$ ChangeZone | Cost$ PayLife<3> | Defined$ Self | Origin$ Graveyard | Destination$ Hand
~~~

[BESTAETIGT] The Oracle-level choice is whether to pay PayLife<3>. TriggerHandler treats a nonzero-cost trigger as optional and procedural; confirmTrigger permits entry to the cost-bearing resolution path. The actual affordability/payment decision belongs to Forge’s cost machinery (CostDecisionMakerBase, CostPayment, and cost-part handling), not to a second generic confirmation request.

Classification:

~~~
PAYMENT
~~~

The current reactive row has cost_bearing=YES, and no generic [ACCEPT, DECLINE] request may be added in parallel. confirmPayment is a separate cost-part callback and was not reached in either controlled workload. PayLife<3> is not a reason to classify Blood Surveil as CONFIRMATION.

## 12. Lazav duplicate-choice audit

The card definition has both:

~~~
OptionalDecider$ You
SVar:LazavCopy: ... Optional$ True ...
~~~

The runtime therefore exposes one Oracle-level choice—whether Lazav becomes the triggering creature’s copy—through:

~~~
confirmTrigger -> WrappedAbility.resolve
confirmAction  -> CloneEffect.resolve
~~~

[BESTAETIGT] The current workload measured 3 trigger callbacks and 3 Clone callbacks. CloneEffect obtains Defined$ TriggeredCardLKICopy, defaults the clone target to Lazav, and applies clone state only after its optional confirmAction accepts.

The authoritative future policy boundary is:

~~~
CloneEffect Optional -> CLONE_OPTIONAL_COPY
~~~

The trigger-level confirmTrigger is classified:

~~~
DUPLICATE_ENGINE_SURFACE
~~~

It must not become a second agent request. The two callbacks are not two Oracle decisions. A future Clone profile still needs a public, value-only projection of the triggering creature’s copyable identity/definition and must fail closed for hidden or untrusted LKI/provenance.

## 13. Cipher-derived trigger audit

The observed Cipher-derived rows are:

~~~
Nightveil Specter: 1 confirmTrigger row
Tibor and Lumia:  2 confirmTrigger rows
~~~

The generated Cipher trigger is DamageDone -> PlayEncoded, with intrinsic=false, OptionalDecider$ You, and DB$ Play | Defined$ OriginalHost | WithoutManaCost$ True | CopyCard$ True generated in CardFactoryUtil.

[BESTAETIGT] The Oracle-level choice is “may cast/play a copy of the encoded card,” not a generic no-cost trigger with no downstream semantics. Acceptance enters the Play pipeline, which can carry richer playability, target, mode, and payment/alternative-cost decisions. The damage event is public, but the encoded source/definition and provenance are not automatically a safe public policy projection.

Classification:

~~~
CALLER_OWNED_SEMANTIC_DECISION
future family = CIPHER_PLAY_ENCODED / PLAY- or PRIORITY-like choice
~~~

The intrinsic=false marker is an exclusion signal for the B1 trigger profile, not a complete semantic category. No Cipher provider is recommended by D. The Encode-time confirmAction rows are a distinct Oracle choice (CIPHER_ENCODE), not duplicates of the damage-time play choice.

## 14. Unreached callback source audit

Zero in the controlled v0 workloads is not evidence of irrelevance. The following source routes were inspected:

| Callback | Source call sites | Source semantic owner | V0 status | Final classification |
|---|---|---|---|---|
| confirmPayment | PlaySpellAbility.payCostDuringAbilityResolve; HumanCostDecision.confirmPayment; PlayerControllerAi.confirmPayment | cost-part/payment lifecycle; AI implementation currently throws if reached | No current rows (0/0) | UNREACHED with PAYMENT owner |
| confirmBidAction | BidLifeEffect; PlayerControllerAi/AiController bridge | continue/stop bidding, followed by numeric bid choice | No current rows (0/0) | UNREACHED with BID owner |
| confirmReplacementEffect | ReplacementHandler | apply or leave an optional replacement | No current rows (0/0) | UNREACHED with REPLACEMENT owner |
| confirmStaticApplication | Combat alternative damage assignment; StaticAbilityManaConvert; StaticAbilitySurveilNum | static application or combat-specific assignment | No current rows (0/0) | UNREACHED with STATIC_APPLICATION owner |

These families are reachable in broader card/effect pools, but no focused fixture was added because doing so would expand the controlled two-deck v0 workload and would not change their source-proven semantic owner. They are not reached by either current canonical workload. They remain deferred, not safe and not irrelevant.

Adjacent boolean siblings outside the required eight-family runtime inventory are also not generic confirmation:

| Sibling | Source owner | Classification |
|---|---|---|
| chooseFlipResult | FlipCoinEffect result path after the RNG/fixed-result branch | FLIP_CALL result semantics |
| payCostDuringRoll | RollDiceEffect cost/reroll branch | PAYMENT |
| payCombatCost | CombatUtil attack/block cost paths | PAYMENT / combat cost |

They had no measured current-v0 rows and are not added to the generic confirmation ledger.

## 15. Mandatory and engine-owned surfaces

[BESTAETIGT] A normal mandatory trigger has no OptionalDecider, receives decider=null in TriggerHandler.runSingleTriggerInternal, and reaches WrappedAbility.resolve without a confirmation callback. The engine resolves it directly.

Therefore:

~~~
mandatory trigger -> ENGINE_OWNED
no forced [ACCEPT] request
no behavior-cloning label
~~~

The current Bitterblossom mandatory-trigger lock continues to prove no confirmTrigger call while the effect resolves. Cost == 0 is not independently synonymous with optionality; OptionalDecider branch order remains authoritative.

## 16. ActionContinuation audit

Every observed row in both workloads recorded:

~~~
action_continuation=false
~~~

The current C2A TARGET context records:

~~~
continuation = null
decisionSequenceId = null
subdecisionIndex = null
~~~

[BESTAETIGT] Queued triggered resolution is not a continuation of the priority action that caused the trigger. The B1 provider rejects an active continuation, and the C2A coordinator rejects it before target generation/resolution. D1 must retain continuation=null; it must not synthesize a sequence merely to link Target A with the later confirmation.

## 17. Visibility and observation/history requirement

| Profile | Required public information | Classification |
|---|---|---|
| Gelectrode B1 | public source identity, event/profile, triggering player, decider | CURRENT_OBSERVATION_SUFFICIENT |
| Blood ETB confirmation | source, decider, trigger profile, Target A public card identity/zone, and current legality/fizzle state | OBSERVATION_BRIDGE_REQUIRED |
| Blood Surveil | source/trigger and cost affordability/payment state | STACK_PUBLIC_STATE_SUFFICIENT for Forge legality, but PAYMENT is a separate owner |
| Lazav clone | source, triggering creature’s public copyable identity/definition, and provenance | OBSERVATION_BRIDGE_REQUIRED |
| Cipher play | encoded source/definition, derived trigger provenance, and play context | OBSERVATION_BRIDGE_REQUIRED |
| Encode step | public source and visible controlled creatures, then caller-owned card selection | CURRENT_OBSERVATION_SUFFICIENT for the narrow observed path, but not a generic confirmation profile |

No HistoryEvent is required for the B1 or C2A target facts already exposed by the current exact fixtures. Blood D1 needs a narrow confirmation-context projection of Target A; it does not need a global history redesign. If Target A is no longer legal at resolution, Forge remains authoritative for fizzle/clear behavior.

## 18. Teacher-label safety

| Candidate profile | Native label mapping | Temporary state risk | Label status |
|---|---|---|---|
| Gelectrode B1 | native true/false maps uniquely to ACCEPT/DECLINE | no target evaluation in exact effect | Safe; already supported |
| Blood ETB | native result maps uniquely to ACCEPT/DECLINE after brains.doTrigger returns | native helper temporarily evaluates B, but C2A diagnostics prove A is restored and effect consumes C | Safe only at the exact post-restore seam; external D1 must skip B |
| Blood Surveil | Boolean is procedural entry to a cost-bearing path | cost/payment helper can contaminate a generic label | Not a confirmation label; PAYMENT-owned |
| Lazav | trigger result and Clone result can both be true/false for one Oracle may | duplicate Oracle surface | Not safe until CloneEffect becomes sole owner |
| Cipher | Boolean allows or suppresses a richer Play path | derived provenance and downstream play decisions | Not a generic confirmation label |
| Stitch in Time | Boolean encodes HEADS/TAILS, not accept/decline | RNG occurs after call | Not a confirmation label; FLIP_CALL |

No observed callback row was forced. Forced decisions are not policy samples. External TARGET results are not native teacher labels. D1 must preserve the existing DECISION_TRACE_V2 rule: only a non-forced, native-mapped, semantically valid confirmation result can be a BC sample.

## 19. Full semantic ownership matrix

The matrix below includes every observed cluster. Counts are separated by workload where the same profile appears in both workloads.

| Callback | Caller | Profile | Count | Oracle-level semantic question | Agent relevant? | Candidate semantics | Cost-bearing? | Target relation | Authoritative owner | Duplicate surface? | ActionContinuation | Public-info requirement | Future family | v0 status | Readiness / blocker |
|---|---|---|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| confirmTrigger | WrappedAbility.resolve | Gelectrode | 17 | Untap self after your instant/sorcery? | Yes | ACCEPT/DECLINE | No | None | B1 ConfirmationDecisionProvider | No | Null | Current observation sufficient | CONFIRMATION | Supported | SUPPORTED_CONFIRMATION; no blocker |
| confirmTrigger | WrappedAbility.resolve | Blood ETB | 2 | Proceed with the already-targeted exile? | Yes | ACCEPT/DECLINE | No | Target A already owned by C2A; no second TARGET | Future exact Blood provider at WrappedAbility.resolve | No; B is AI-only | Null | Narrow Target-A projection required | CONFIRMATION | TARGET supported; confirmation open | D1 after public Target-A projection; fail closed otherwise |
| confirmTrigger | WrappedAbility.resolve | Blood Surveil | 1 | Enter the PayLife<3> cost path? | Yes, through payment | Pay/decline cost part | Yes | None | Forge trigger/cost lifecycle | Yes if generic confirmation added | Null | Payment context | PAYMENT | Observed | Defer to PAYMENT; no generic label |
| confirmTrigger | WrappedAbility.resolve | Lazav | 3 | Become a copy of the triggering creature? | Yes, but one choice | Copy/no-copy | No | Not TARGET; defined trigger object | CloneEffect Optional | Yes; this row loses | Null | Clone source/provenance bridge | CLONE_OPTIONAL_COPY | Observed | Defer until one owner and public copy projection |
| confirmTrigger | WrappedAbility.resolve | Cipher PlayEncoded | 3 | Cast/play an encoded copy? | Yes, caller-owned | May play/cast, then richer Play path | No trigger cost; downstream Play semantics | Downstream target/mode/payment may follow | Play/caller pipeline | No; Encode is distinct | Null | Encoded source/provenance bridge | CIPHER_PLAY_ENCODED | Observed | Defer; not generic confirmation |
| confirmAction | EncodeEffect.resolve | Stolen Identity | 4 | May the spell be exiled and encoded? | Yes, caller-owned | Encode/no-encode, then creature selection | No | Downstream creature selection, not TARGET callback | EncodeEffect | No | Null | Current public path, exact caller context | CIPHER_ENCODE | Observed | Defer; no common adapter |
| confirmAction | EncodeEffect.resolve | Call of the Nightwing | 1 | May the spell be exiled and encoded? | Yes, caller-owned | Encode/no-encode, then creature selection | No | Downstream creature selection, not TARGET callback | EncodeEffect | No | Null | Current public path, exact caller context | CIPHER_ENCODE | Observed | Defer; no common adapter |
| confirmAction | CloneEffect.resolve | Lazav | 3 | Apply the copy state? | Yes, caller-owned | Copy/no-copy | No | Defined source; clone target is Lazav | CloneEffect Optional | It is the authoritative one | Null | Public copyable source/provenance bridge | CLONE_OPTIONAL_COPY | Observed | Defer until exact profile projection |
| chooseBinary | FlipCoinEffect#flipCoin | Stitch in Time | 2 | Which call does the player make? | Yes | HEADS/TAILS | No | None | FlipCoinEffect | No | Null | Current public observation sufficient | FLIP_CALL | Observed | Defer; RNG is engine-owned |
| payCostToPreventEffect | AbilityUtils#handleUnlessCost | Syncopate | 5 reactive | Pay to prevent countered spell? | Yes, payment | Pay/do not pay | Yes | None | Cost/payment path | No | Null | Cost affordability/payment context | PAYMENT | Observed | FRL-02C-compatible; not confirmation |
| payCostToPreventEffect | SacrificeEffect#resolve | Morinfen | 3 proactive | Pay sacrifice cost? | Yes, payment | Pay/do not pay | Yes | None | Cost/payment path | No | Null | Cost affordability/payment context | PAYMENT | Observed | Defer PAYMENT completion |
| payCostToPreventEffect | AbilityUtils#handleUnlessCost | Hidden Horror | 4 proactive | Pay sacrifice cost? | Yes, payment | Pay/do not pay | Yes | None | Cost/payment path | No | Null | Cost affordability/payment context | PAYMENT | Observed | Defer PAYMENT completion |
| payCostToPreventEffect | AbilityUtils#handleUnlessCost | Waterspout Djinn | 15 proactive | Pay sacrifice cost? | Yes, payment | Pay/do not pay | Yes | None | Cost/payment path | No | Null | Cost affordability/payment context | PAYMENT | Observed | Defer PAYMENT completion |
| payCostToPreventEffect | AbilityUtils#handleUnlessCost | Barrow Ghoul | 1 proactive | Pay sacrifice cost? | Yes, payment | Pay/do not pay | Yes | None | Cost/payment path | No | Null | Cost affordability/payment context | PAYMENT | Observed | Defer PAYMENT completion |
| payCostToPreventEffect | AbilityUtils#handleUnlessCost | Disrupt | 1 proactive | Pay counter-prevention cost? | Yes, payment | Pay/do not pay | Yes | None | Cost/payment path | No | Null | Cost affordability/payment context | PAYMENT | Observed | Defer PAYMENT completion |

## 20. Callback reconciliation proof

### Reactive rows

~~~
confirmTrigger:
  17 Gelectrode
   2 Blood ETB
   1 Blood Surveil
   3 Lazav
   1 Nightveil Specter Cipher
   2 Tibor and Lumia Cipher
  =26

confirmAction:
  4 Stolen Identity Encode
  1 Call of the Nightwing Encode
  3 Lazav Clone
  =8

chooseBinary:
  2 Stitch in Time
  =2

payCostToPreventEffect:
  5 Syncopate
  =5

reactive total = 26 + 8 + 2 + 5 = 41
~~~

### Proactive rows

~~~
payCostToPreventEffect:
  15 Waterspout Djinn
   4 Hidden Horror
   3 Morinfen
   1 Disrupt
   1 Barrow Ghoul
  =24

proactive total = 24
~~~

Every observed row is represented in the matrix. No measured row is assigned to generic OTHER, UNKNOWN, or MISC.

## 21. Remaining true CONFIRMATION profiles

~~~
Gelectrode SpellCast -> Untap Self
  CONFIRMATION
  SUPPORTED via B1

Blood ETB exile Graveyard card
  TARGET A
  SUPPORTED via C2A exact profile

Blood ETB may/decline after Target A
  CONFIRMATION
  FUTURE_CONFIRMATION_PROFILE
  exact candidates = [ACCEPT, DECLINE]
  next exact slice = FRL-02K-D1
~~~

The D1 request must be one atomic confirmation after Target A, not a combined target-plus-confirmation request. It must preserve null continuation and exact Forge resolution/fizzle semantics.

## 22. Profiles removed from generic CONFIRMATION scope

~~~
Blood Surveil PayLife<3>       PAYMENT
Lazav trigger-level may        DUPLICATE_ENGINE_SURFACE; CloneEffect owns CLONE_OPTIONAL_COPY
Lazav CloneEffect optional     CALLER_OWNED_SEMANTIC_DECISION / CLONE_OPTIONAL_COPY
Cipher encode                  CALLER_OWNED_SEMANTIC_DECISION / CIPHER_ENCODE
Cipher may-play                CALLER_OWNED_SEMANTIC_DECISION / CIPHER_PLAY_ENCODED
Stitch in Time call             FLIP_CALL
payCostToPreventEffect          PAYMENT
mandatory triggers              ENGINE_OWNED
confirmPayment                  UNREACHED_IN_V0_WORKLOAD / PAYMENT
confirmBidAction                UNREACHED_IN_V0_WORKLOAD / BID
confirmReplacementEffect        UNREACHED_IN_V0_WORKLOAD / REPLACEMENT
confirmStaticApplication        UNREACHED_IN_V0_WORKLOAD / STATIC_APPLICATION
~~~

## 23. Retained support statuses

~~~
FRL-02K0                         PASS / retained
FRL-02K-B1 Gelectrode             SUPPORTED / retained
FRL-02K-C/C1/C2                  PASS / retained
FRL-02K-C2A Blood ETB TARGET      SUPPORTED / retained
Blood CONFIRMATION                OPEN
global triggered TARGET           OPEN
global CONFIRMATION               OPEN
~~~

No B1 or C2A semantics were changed by this audit.

## 24. Verification evidence

### 24.1 Focused remaining-callback audit

~~~
mvn -pl forge-gui-desktop -am \
  '-Dtest=FRL02KRemainingConfirmationAuditTest' \
  '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

Result:

~~~
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
~~~

The Surefire output recorded reactive-audit total=41 and proactive-audit total=24.

### 24.2 Retained B1/C/C1/C2/C2A/provider selector

~~~
mvn -pl forge-gui-desktop -am \
  '-Dtest=FRL02KConfirmationAuditTest,GelectrodeConfirmationWorkloadTest,FRL02KRemainingConfirmationAuditTest,FRL02KChangesZoneProjectionAuditTest,FRL02KTriggeredTargetOwnershipAuditTest,FRL02KTriggeredTargetProviderAuditTest,FRL02KTriggeredTargetExternalOwnershipAuditTest' \
  '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

Result:

~~~
Tests run: 34
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
~~~

This selector retained the Gelectrode mandatory/confirmation/neutrality locks, target provider/coordinator locks, C1 projection/ownership checks, C2A exact Blood external/native ownership, and the remaining callback inventory.

### 24.3 Broad gates

The required broad gates were executed after the focused locks:

~~~
mvn -pl forge-gui-desktop -am test
mvn -pl forge-gui-desktop -am -DskipTests package
mvn -pl forge-gui-desktop -am validate
git diff --check
~~~

Results:

~~~
mvn -pl forge-gui-desktop -am test
  Tests run: 707, Failures: 0, Errors: 0, Skipped: 6
  all six reactor modules SUCCESS; BUILD SUCCESS
  checkstyle: 0 violations in each module

mvn -pl forge-gui-desktop -am -DskipTests package
  all six reactor modules SUCCESS; BUILD SUCCESS
  checkstyle: 0 violations in each module
  forge.exe and the assembled desktop jar created

mvn -pl forge-gui-desktop -am validate
  all six reactor modules SUCCESS; BUILD SUCCESS
  checkstyle: 0 violations in each module

git diff --check
  PASS
~~~

The full test output also retained the FRL-02K0 worker equality result: gameplay, RNG, decision, and priority outputs were identical with zero collisions and zero parse errors. No hosted GitHub result is inferred from local Maven output.

## 25. Findings

### P0

None.

### P1

None after the current source/runtime reconciliation.

### P2

1. The Boolean CSV has no per-row fingerprint or process/worker identifier. The audit relies on source-level observational behavior, fresh-JVM audit-on/off determinism, and the specialized C1/C2/C2A state/RNG diagnostics. A future diagnostic enhancement may add stable occurrence identity, but it is not needed to classify the current rows.
2. D1 must add a Blood-specific public Target-A projection before external policy ownership. The current B1 confirmation context cannot be reused unchanged for Blood.
3. confirmPayment, bid, replacement, and static-application families need focused fixtures only when their specific future milestones are opened; their current source ownership is already established.

## 26. Milestone verdict

~~~
FRL_02K_D_PASS
~~~

Meaning of this PASS:

~~~
all observed current-v0 boolean callbacks reconcile exactly
every observed callback has one semantic owner
cost-bearing triggers are not generic confirmation
Lazav's duplicate Oracle choice is explained and not doubled
mandatory triggers remain engine-owned
B1 Gelectrode remains supported
C2A Blood Target A remains supported
the exact next production profile is identified
~~~

It does not mean global CONFIRMATION, Blood CONFIRMATION, PAYMENT, Cipher, Clone, Flip-call, Bid, Replacement, or Static Application is implemented.

## 27. Draft PR boundary

After final broad verification and independent architecture review:

~~~
commit: audit-only documentation and plan
branch: frl/02k-d-confirmation-remainder-audit
PR:    Draft
title: FRL-02K-D: audit remaining confirmation profiles
merge: prohibited
ready-for-review: prohibited unless explicitly requested
~~~

## 28. STOP

Do not continue into FRL-02K-D1 implementation in this worktree or milestone.

STOP
