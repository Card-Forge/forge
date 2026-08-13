# FRL-02L Live ORDER Attribution Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Attribute every ordering-related Forge callback and determine, from source and fresh runtime evidence, whether the controlled v0 environment requires an agent-owned `DecisionType.ORDER` before modern `DAMAGE_ASSIGNMENT`.

**Architecture:** Preserve Forge as the legality and execution authority. Treat legacy combat damage ordering, engine sorting, partition-plus-order callbacks, and real strategic permutations as distinct semantic surfaces; do not add an ORDER provider. Use opt-in, worker-local, state-neutral diagnostics only when existing tests cannot establish runtime attribution.

**Tech Stack:** Java/Maven Forge modules, PowerShell, JUnit/Surefire, Git worktree, Markdown evidence reports.

---

### Task 1: Verify the protected checkpoint and worktree

**Files:**
- Read: `C:\forgeAI` Git state
- Read: `C:\forgeAI-order-audit` Git state

- [x] Verify the primary checkout with `git fetch origin`, `git status`, `git diff --stat`, `git diff`, `git diff --check`, `git rev-parse HEAD`, and `git rev-parse origin/master`.
- [x] Require the primary checkout to be clean and both SHAs to equal `ec52000158448811bafb76763c3117f6e2101f75`.
- [x] Verify the isolated branch with `git status --short --branch`, `git rev-parse HEAD`, `git rev-parse origin/master`, `git merge-base HEAD origin/master`, and `git diff --check`.
- [x] Do not modify retained worktrees or the primary checkout.

### Task 2: Read authority and map source surfaces

**Files:**
- Read: `docs/AI-ML DOCS/ML_STRATEGY.md`
- Read: `docs/AI-ML DOCS/FRL_02K_CONFIRMATION_AUDIT.md`
- Read: `docs/AI-ML DOCS/FRL_02K_D1_BLOOD_CONFIRMATION_AUDIT.md`
- Read: historical FRL-02I/combat documentation where present
- Read: `forge-game/src/main/java/forge/game/player/PlayerController.java`
- Read: `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java`
- Read: `forge-gui/src/main/java/forge/player/PlayerControllerHuman.java`
- Read: callers in `forge-game`, `forge-ai`, `forge-gui`, and `forge-gui-desktop`

- [x] Inventory declarations and real callers for `orderSimultaneousSa`, `orderAndPlaySimultaneousSa`, `orderBlockers`, `orderBlocker`, `orderAttackers`, `orderMoveToZoneList`, `arrangeForScry`, `arrangeForSurveil`, `orderCosts`, `reorderHand`, and semantic equivalents.
- [x] Search semantic ordering terms beyond the literal method name: permutation, arrange, sort, sequence, top/bottom, any-order, choose-order, simultaneous, stack, scry, surveil, library, graveyard, and combat damage.
- [x] Attribute each surface to one required classification without using unexplained `OTHER`, `MISC`, or `UNKNOWN`.
- [x] Trace simultaneous trigger collection, APNAP grouping, ordering, and stack insertion through `TriggerHandler`, `MagicStack`, and effect-resolution callers.
- [x] Separate `DAMAGE_ASSIGNMENT` from every legacy combat order callback.

### Task 3: Establish v0 card-pool reachability

**Files:**
- Read: `forge-gui/res/quest/precons/Izzet Guild Kit.dck`
- Read: `forge-gui/res/quest/precons/Dimir Guild Kit.dck`
- Read: relevant card/effect scripts and API implementations
- Read: existing canonical workload tests under `forge-gui-desktop/src/test/java/forge/view`

- [x] Determine whether the two-deck pool can create two or more legally player-selectable same-controller simultaneous triggers.
- [x] Determine whether zone-ordering, Scry, Surveil, and other multi-item ordering families are reachable in the controlled workload.
- [x] Use source/card-pool evidence in addition to observed counts; never infer impossibility from a zero count alone.
- [x] Add only a narrow focused fixture when source reachability is insufficient to answer semantic ownership.

### Task 4: Measure the canonical workload and targeted evidence

**Files:**
- Temporary audit-only changes under the isolated worktree, removed before the final commit unless a retained test is necessary and explicitly documented
- Read: generated worker-local diagnostic output

- [x] Run `Izzet Guild Kit` versus `Dimir Guild Kit`, 10 games, seed `20260810`.
- [x] Record every instrumented surface, call count, acting player, item-count buckets `0`, `1`, `2`, and `>=3`, exact input/result membership, and whether the result changed order.
- [x] Keep diagnostics opt-in, worker-local, sanitized, deterministic, state-neutral, and RNG-neutral.
- [x] Run audit-on/audit-off controls and compare gameplay/determinism evidence.
- [x] Run focused fixtures only for source-reachable families not exercised by the canonical workload.

### Task 5: Write the attribution report and update the strategy ledger

**Files:**
- Create: `docs/AI-ML DOCS/FRL_02L_ORDER_ATTRIBUTION_AUDIT.md`
- Modify only if the final disposition is established: `docs/AI-ML DOCS/ML_STRATEGY.md`
- Modify: `docs/superpowers/plans/2026-08-13-frl-02l-order-attribution-audit.md`

- [x] Include checkpoint, branch/worktree, source inventory, caller traces, APNAP findings, canonical counts, reachability, focused evidence, combat separation, identity/visibility, teacher-label feasibility, continuation, neutrality, reconciliation, ownership matrix, severity findings, and exact final next-step decision.
- [x] Put every discovered ordering-related surface in the ownership matrix exactly once.
- [x] State explicitly that `FRL-02I` legacy combat order is distinct from `FRL-02L` live ORDER attribution.
- [x] If no live agent-required ORDER exists in controlled v0, record `V0_ORDER_NOT_REQUIRED` and `NO_ORDER_IMPLEMENTATION_BEFORE_V0_GATE`.
- [x] If a live profile exists, name the exact profile and recommend only that future slice; do not add generic ORDER production support.
- [x] Do not rewrite historical FRL-02I or confirmation conclusions.

### Task 6: Run focused and broad verification

**Files:**
- Read: focused test and build output
- Read: final Git diff and status

- [x] Run focused audit tests first, including retained FRL-02K0, B1, C2A, D1, `DecisionTraceV2`, `PriorityActionDiagnostics`, `TriggeredTargetDecisionCoordinator`, and `ConfirmationDecisionProvider` locks.
- [x] Run the canonical workload and record exact test counts and exit status.
- [x] Run `mvn -pl forge-gui-desktop -am test`.
- [x] Run `mvn -pl forge-gui-desktop -am -DskipTests package`.
- [x] Run `mvn -pl forge-gui-desktop -am validate`.
- [x] Run `git diff --check` and `git status --short --branch`.
- [x] Report local results separately from hosted CI; do not mark unavailable gates as PASS.

### Task 7: Independent reviews and delivery boundary

**Files:**
- Read: final report and diff
- Create: one Git commit on `frl/02l-order-attribution-audit`

- [x] Perform an independent architecture review of semantic attribution and a separate evidence review of source/runtime reconciliation.
- [x] Classify findings as `P0`, `P1`, or `P2`; resolve every `P0` and `P1` before delivery.
- [x] Re-run final verification after review fixes.
- [ ] Commit the audit documentation only after fresh verification.
- [ ] Push `frl/02l-order-attribution-audit` to the configured remote.
- [ ] Open a Draft PR titled `FRL-02L: audit live ORDER decision surfaces`.
- [x] Do not merge, mark ready, implement ORDER, or begin DAMAGE_ASSIGNMENT; end the milestone report with `STOP`.

### Self-review checklist

- [x] Every user-required report section is present or explicitly marked not applicable with evidence.
- [x] Source-reachable, runtime-observed, v0-reachable, and future-pool-only states are not conflated.
- [x] One-item callbacks are classified `FORCED_ORDER`, not strategic decisions.
- [x] `orderMoveToZoneList` is split into selection-plus-order, engine-owned, or true permutation semantics by caller.
- [x] Scry/Surveil partition and ordering are reported separately.
- [x] Simultaneous trigger ordering is not treated as arbitrary stack reordering.
- [x] No raw engine objects or hidden information are exported in the report's proposed future boundary.
- [x] The final `ORDER_DECISION` and `NEXT` block use exactly one of the three permitted forms.
