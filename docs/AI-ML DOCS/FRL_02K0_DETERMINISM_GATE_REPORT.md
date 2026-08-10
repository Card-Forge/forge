# FRL-02K0 Determinism, RNG-Neutrality and Trace Gate

## Gate result

```text
FRL-02K0 PASS
```

The current decision regression suite is green, same-seed reproduction is exact, all enabled neutral diagnostics
are behavior-neutral after the fix, every representative neutral probe is state- and RNG-neutral, and the two
historical score transitions are explained. CONFIRMATION remains out of scope and paused for review.

## 1. Preserved stopped work and gate checkout

The original checkout was inspected before any gate work:

```text
path:   C:\forgeAI
branch: chore/decision-diagnostics-column-contract
HEAD:   7a0dea0ebb5b1ec8aac5c97d94c0a06c809471c5
status: M forge-gui-desktop/src/test/java/forge/game/decision/PriorityActionDiagnosticsTest.java
```

It was not reset, cleaned, rebased, switched, or used for implementation. A later user-side modification to
`ML_STRATEGY.md` also appeared in that checkout; the gate did not touch it.

`origin/master` was fetched and verified as exactly
`7a0dea0ebb5b1ec8aac5c97d94c0a06c809471c5`. The isolated gate checkout is:

```text
path:   C:\forgeAI-determinism-gate
branch: frl/02k0-determinism-safety-gate
base:   7a0dea0ebb5b1ec8aac5c97d94c0a06c809471c5
```

Intentional checkpoints before this report:

```text
ad250a703d7034cbcb98de9a5afd79075e3c2251  test: align priority diagnostics column contract
b4b4ce3db98aa1c213ca040e604c29b5ff04f6d4  test: add determinism trace gate
bb3a2e7f2a0455a38cfc71794f53b523f122dcf9  fix: keep neutral probes behavior-neutral
e8ebe193a6feeb11de49e3cb31adabfe3f4bea9e  test: reject ambiguous trace outcomes
75d795c379a9e98a750b46bccb74131b9e636d2c  fix: preserve priority audit ability surface
```

## 2. Green baseline repair

Production already declared `COLUMN_COUNT = 55`, the header already had 55 columns, BLOCK already wrote
`selection_stage` at index 54, and non-stage rows left index 54 blank. Seven tests still expected 54 columns.
The baseline checkpoint changed those stale assertions to exact equality with 55 and added explicit checks that:

- the header and `COLUMN_COUNT` both equal 55;
- every covered formatted row has exactly 55 fields;
- BLOCK writes its expected `BlockDeclarationStage` to field 54;
- non-stage rows have `fields[54] == ""`.

No assertion was removed or weakened. Baseline evidence was 11/11 focused diagnostics tests and 220/220 complete
decision regressions. The final expanded regression command executes 255 tests with 0 failures, 0 errors, and
0 skipped. The package lifecycle and configured Checkstyle lifecycle both pass with zero violations.

## 3. Canonical trace architecture

All hashes use Java standard-library SHA-256 over each UTF-8 canonical record followed by one `\n`. Raw records are
retained under the opt-in `forge.determinism.traceDir` directory. Normal runs do not attach a collector.

### Decision trace

Version and field order:

```text
DECISION_TRACE_V1
sequenceIndex
turn
phase
actingPlayerSeat
DecisionType
adapterOrStage
decisionStepIndex
forced
selectedCandidateSemanticKey
```

Semantic candidate keys come from the existing neutral decision model. Latency, prompt text, request IDs, process
IDs, and paths are excluded. Unmapped callbacks emit `MAPPING_FAILED`. OFF runs correctly retain no decision file
and report `decisionHash=ABSENT`; that absence is never compared to an ON decision hash.

### Gameplay trace

Each record is:

```text
GAMEPLAY_TRACE_V1|sequenceIndex|checkpointKind|FORGE_STATE_V1|...
```

The state fingerprint includes turn, phase, active and priority seats; ordered players; life, poison, win/loss,
zone sizes and internal card identities; battlefield owner/controller/tapped/phased/state/counters; stack source/API;
combat assignments; and game-over state. Stable card identity is `cardId:gameTimestamp`. Players, battlefield,
unordered counters, attackers, and blockers are sorted by stable identity. Ordered zones preserve Forge order.
The full hidden state is audit-internal and is not exposed through `DecisionRequest`.

Wall clocks, localization, absolute paths, process IDs, object strings, Java identity hashes, and unordered map/set
iteration are excluded. An ambiguous multiple-winner state is emitted as
`INVALID_WINNER_SEATS_[...]`; the trace never chooses an arbitrary `HashMap` winner.

### RNG trace

`DeterminismAuditRandom` subclasses `java.util.Random` and calls `super.next(bits)` exactly once. Its canonical record
is:

```text
RNG_TRACE_V1|drawIndex|requestedBits|unsignedRawValue
```

The separately retained diagnostic record appends the first Forge call-site frame. Call-site metadata is excluded
from the equality hash. Parity tests prove the same seed returns the same Java Random values and that metadata
capture performs no extra draw. No reflection, rewind, seed restoration, or game copy is used.

`DeterminismTraceHasher.firstDivergence` returns the first unequal or missing record index, so every mismatch can be
reduced to a last-equal/first-different tuple.

## 4. Environment manifest and reconstructed benchmark

Current controlled environment:

```text
experiment commit: 75d795c379a9e98a750b46bccb74131b9e636d2c
JDK:               Eclipse Temurin 17.0.19+10, 64-bit Server VM
OS:                Windows 11 10.0 amd64
application locale: en-US
build locale:       de_DE
game type:          Constructed
worker model:       one game at a time, fresh JVM per run
decks:              Izzet Guild Kit (seat 0) vs Dimir Guild Kit (seat 1)
Izzet deck SHA-256: cc58bbe8572d0261ec14d4a87d3ea31855aba801a5eacfb51e5a29bb97f9310c
Dimir deck SHA-256: 43a8a5a5ec81bbc2dc3ef8a5fc58be168b2c05f6f5f4bcceef6bebe90f44ad5a
AI profiles:        no -a override; Forge default profile for both seats
mulligan:           configured Forge London rule
seed:               20260810
games:              10
timeout:            Forge default 120 seconds
```

Reconstructed command, run from `forge-gui` against the packaged artifact:

```text
java [diagnostic properties] -cp ..\forge-gui-desktop\target\forge-gui-desktop-2.0.14-SNAPSHOT-jar-with-dependencies.jar forge.view.Main sim -d "Izzet Guild Kit" "Dimir Guild Kit" -n 10 -s 20260810 -q
```

The surviving FRL-02G log confirms the historical priority property form:

```text
-Dforge.priority.metricsFile=<csv>
```

Current `ALL_NEUTRAL_DIAGNOSTICS_ON` additionally uses
`-Dforge.mulligan.metricsFile=<csv>`. OFF omits both properties. The trace directory property is present in every
hashed run and is audit infrastructure rather than a neutral decision diagnostic.

The supplemental proactive remeasurement used the same environment and flags on branch head
`e05db6035781a80a84d53d0cc3da4316cdc98b8b`; the only commit after the measured production-code head is this
report. Its exact workload was:

```text
decks:                    Dead and Alive (seat 0) vs Air Forces (seat 1)
Dead and Alive SHA-256:   3ac49a5a180a78193861c1fd191c6212f8ba29570fb1ff122e161afaf24480a3
Air Forces SHA-256:       a8be5bfcb8a2871a83203c87cfd94c88bb0227ed4f0c1c8b1268cf71b1c50a12
AI profiles:              no -a override; Forge default profile for both seats
mulligan:                 configured Forge London rule
seed:                     20260809
games:                    10
```

The packaged command differed only in deck names and seed:

```text
java [diagnostic properties] -cp ..\forge-gui-desktop\target\forge-gui-desktop-2.0.14-SNAPSHOT-jar-with-dependencies.jar forge.view.Main sim -d "Dead and Alive" "Air Forces" -n 10 -s 20260809 -q
```

## 5. Current-head reproducibility and diagnostics neutrality

The aggregate hashes below are SHA-256 reductions of the ten ordered per-game hashes/outcomes.

| Run | Diagnostics | Gameplay aggregate | RNG aggregate | Outcome aggregate | Result |
|---|---|---|---|---|---:|
| OFF A | none | `0583abff4cf2abe0c6dda117b64cdfe5439da3e379fa54e8854b306804f5b922` | `93f81836e80a88011e94c8253ff96d6850fcf2ec3e0f730a815b2f45d277767a` | `6500807e60e6c35704e074bfa7d61a5fca9c135ebb27217df2fcc766f5d1eca4` | 3-7 |
| OFF B | none | same | same | same | 3-7 |
| ON A | priority + mulligan | same | same | same | 3-7 |
| ON B | priority + mulligan | same | same | same | 3-7 |

ON decision aggregate A and B is identically
`87fc16e6e48577559099bd94f8341b993f24b423773ba59ad912f3e6d3b897f6`. OFF has no decision artifacts.

Authoritative per-game values, identical in all four runs:

| Game | Gameplay hash | RNG hash | Draws | Outcome |
|---:|---|---|---:|---|
| 1 | `d7eed7b9d75d3503a1296e9fb72cdf98ed1f375735c2282b92089542618caf1d` | `bcb802e32a0bf276259d9d2de16025f9ee3a98dc5b0c0f5628d48bc273baf840` | 810 | WINNER_SEAT_1 |
| 2 | `61a47b3b2fde41c648ddc524cfbfc49b945f7ae5934bf0c35af535fd6bff78aa` | `1fc3b9460a69f1e0068808a66c91d8220f8539f415271a936e1c6ec8ae89c8fc` | 712 | WINNER_SEAT_0 |
| 3 | `e769d76b12bbe3939b52468d4395a464892f85c21b4fdd81792db4d76d987486` | `75acc2c443a24aa49658ba37c5b10daec762246987b73d40c487a8c70d5f89e7` | 1052 | WINNER_SEAT_1 |
| 4 | `819888c129ea4dafb9f7b068f64cccc706c4b0f72db502f4332371d5748b9f49` | `2ab13640e7b5fac8ad88a8100ebcbcc13ece33cf2aa777786e93f63deb326d1b` | 1708 | WINNER_SEAT_1 |
| 5 | `b649b89b619d9d923b10cd2f481ee6b3a6164be245c27e07990f8434d25d278f` | `1b32e779a6854474ee14bffcd45e63d5d836d3f4ba5190b69f048be12a320f61` | 679 | WINNER_SEAT_0 |
| 6 | `7e8b8d5b38ab84092b94b236e324e48585a58ca7adf775e36ba27ac1cf323bbf` | `87ab2264d887d596685ad1f4b0ed67fc941555458043a52a4edd4753abecfd9a` | 1928 | WINNER_SEAT_1 |
| 7 | `5ea082cc576869ba80f004d7fa25689ad371d7c158b9219b07618dd5f37a5008` | `fc2ae2f3475cc28a601376261bca7b72ca41951fba3beaf6b57f7ad773e545df` | 845 | WINNER_SEAT_0 |
| 8 | `dbf70e46108812e4f1f126fce568b4ccff6a601e4490073110ecfe0083a76a70` | `41a40d672aa3e8228d785a249c250c00bb82909b9f90e02438bd5522533ce1ce` | 789 | WINNER_SEAT_1 |
| 9 | `e2add839b6b2b65c306443db63f54eecf23da5f8fc05029d94d4921525881ccf` | `ee1e7b2e86fe8e0956cb9ecd5f7aa2d3974981ef050653d623def5d69384c782` | 1494 | WINNER_SEAT_1 |
| 10 | `1bb69601a3d7aeb3ee276f13e7a1c9f6ee4b1967efdc5b31e451988b210a7c67` | `e15748d8e4b85066f76d5c576124f65596ea2425390ee175d8176f5808859035` | 609 | WINNER_SEAT_1 |

Therefore same-commit reproducibility and diagnostics OFF-vs-ON neutrality both pass at per-record granularity,
not merely at final score granularity.

### Supplemental proactive fixed-head remeasurement

The proactive workload was independently repeated in four fresh JVMs. It uses the same comparison rules as the
reactive workload above and revalidates the pre-fix proactive measurement cohort on the fixed head.

| Run | Diagnostics | Gameplay aggregate | RNG aggregate | Outcome aggregate | Result |
|---|---|---|---|---|---:|
| OFF A | none | `071a396a353e4dbd1a9db62bdeece836ddcaaa4d900f368efa223a137d0e8162` | `36001966aa6ab3c8e2950d444b1974577a6e6d9ea9c37b25690994376ff3a917` | `f093203eb188901ab338111c45565afe6dac27899e28161690febc0c9ea0c077` | 7-3 |
| OFF B | none | same | same | same | 7-3 |
| ON A | priority + mulligan | same | same | same | 7-3 |
| ON B | priority + mulligan | same | same | same | 7-3 |

ON decision aggregate A and B is identically
`a0a46f4868fe4b09dc039ad3c0493684b35b46ef88b621dbefc7119eac8e0b04`. Both ON metrics files contain the same
28,187 priority rows and 52 mulligan rows, with zero `PRIORITY_STATE` or `MAPPING_FAILED` records.

Authoritative proactive per-game values, identical in all four runs:

| Game | Gameplay hash | RNG hash | Draws | Outcome |
|---:|---|---|---:|---|
| 1 | `2162268d1cb4dcd81012617d0c818aebc4a8e035d1219be9766ff56ce27ec814` | `5ac028ce610abe5e9081a9f47e023d33d23a45e72b851f93efa20bb25ac0b0cb` | 1282 | WINNER_SEAT_0 |
| 2 | `d18ce0110daff364f966eec2526a0a81a415468aa3f73d0623aa5076ef280db0` | `ef42a71508e5ce2747fdc555afa9fafe7fe614514c00ec8b1137f52aa08c869c` | 501 | WINNER_SEAT_0 |
| 3 | `1ad36dcc2846291b77936a0f17614f23b7b1893ef4c655485ac1088bb9a6b2ca` | `3a860d9edac87e662b2e12f19992ee78327d17580347747a976b774831b56d79` | 388 | WINNER_SEAT_1 |
| 4 | `356e9c0f83b489e0d388fdef26d7d7083ec665366c6099d4db6ab1681687b367` | `6a27933dffd56e3e7fdc0c471923fb0c622ab36a8664e116af60790838248a33` | 5201 | WINNER_SEAT_0 |
| 5 | `1e9022298400953634d52c8005edf9d47bfb57fa474c7861418b09785d4d429a` | `458f3f111a7eb6a18d173f012f451d62171552400cdc8ce245f8f035246e468f` | 527 | WINNER_SEAT_1 |
| 6 | `2f859dacd127ea93cb6d21c55572c74b5dcfcc07ea3b21fa9b9596047a12d3a0` | `33080eea9d83c94f9c9ad5be544ad976ae7be193ce7781617a8a5c73e8c9d6e8` | 616 | WINNER_SEAT_1 |
| 7 | `58bc3b64535880a43300b9202503ed2ab0cae67344dc49bb15ff4f6c81cb7b53` | `e5fc9ea24544dc36ad3d92eff62a1cc5bc0a1a6a56a289c45dfa21c9fb9f6efc` | 314 | WINNER_SEAT_0 |
| 8 | `6ff200961ad8a8564d0d2de4ccc9cb22add9abd1cc1782aeb0553331da5a664f` | `c9d9efaf705fcf3c64ebd5d089ac1d54b818317301a00782dca33b2ea6619fc2` | 482 | WINNER_SEAT_0 |
| 9 | `2134ce75ab4220d2805b892fa89cdd1144136ee027eb8b4032cf8a2828ebdc3a` | `c41db0fc62f3c6d1fe0adbadc2d6fff7fa88cc2b13b97523e193e352d55cae18` | 549 | WINNER_SEAT_0 |
| 10 | `3127f0a300b2e4ffa8e6732ef7951aec273ece4dff5790ccd0be0a758acc096e` | `aa95cbc9c877b355507359bcc2edc7fc905e705f70bf60fffba782400b41b6de` | 451 | WINNER_SEAT_0 |

## 6. Trace collector and direct probe neutrality

The collector regression captures the canonical state and RNG count before attach/snapshot/hash/write and verifies
both remain identical afterward. It also verifies absent decision output when no neutral decision is recorded.
The same five-file collector patch applied without semantic changes to all historical worktrees. Its Java Random
algorithm parity and zero-extra-draw behavior are separately tested.

`NeutralityAssertions` installs an audit RNG, captures `FORGE_STATE_V1`, runs only the neutral operation, then
requires the same state and draw count. Representative results:

| Family | Neutral region | State | RNG draws |
|---|---|---|---:|
| PRIORITY_ACTION | generation and cost preview | unchanged | 0 |
| TARGET | candidate generation/probes | unchanged | 0 |
| PAYMENT | generation/probes | unchanged | 0 |
| X_VALUE | feasibility and candidate generation | unchanged | 0 |
| MODE | legality/completion probes | unchanged | 0 |
| CARD_SELECTION | discard replay | unchanged | 0 |
| ATTACK | declaration replay | unchanged | 0 |
| BLOCK | capture and replay | unchanged | 0 |
| MULLIGAN | KEEP/REDRAW generation and bottom replay | unchanged | 0 |

The priority regression also verifies the live ability activator is unchanged and the global `SpellAbility` ID
sequence advances by exactly one between two sentinel allocations, proving the neutral preview allocates no IDs.
An unsupported priority fixture verifies diagnostic failure returns `null` without changing game state or RNG and
does not escape into the native game loop.

## 7. Historical audit method

Repository history verifies the exact milestones:

| Milestone | Merge commit |
|---|---|
| FRL-02C | `115762217f84e13041cc4ed315dc58e8b95abae8` |
| FRL-02D | `c3d5eebae0765cbc4784febf9c4da425f50e63c2` |
| FRL-02E | `d06ebc79852bfcc05e466d6d3a593524689bad1a` |
| FRL-02F | `1da8d2a2a2a163aa433b6bfbf870744544cc2d0b` |
| FRL-02G | `64ffeb8e630f22d4dc05d4c64a40991b3de6a3ed` |

Each commit was checked out detached in its own temporary worktree. One identical audit-only patch added the RNG
tracer, trace hasher/session, gameplay fingerprint, and simulator hook. No current decision provider, adapter,
diagnostic behavior, or tests were cherry-picked. All five patched historical artifacts built with zero configured
Checkstyle violations. Each was run twice in a fresh JVM with priority diagnostics enabled.

The retained reports document C `5-5`, D `5-5`, E `3-7`, F `5-5`; the retained G report itself documents `3-7`,
not the prompt's `5-5`. Surviving G logs contain the priority property and the same deck orientation/seed.

## 8. Historical matrix

The raw simulator score is shown because it is historically visible, but it is invalid for games 2 and 3. The
canonical column is the trace-based classification.

| Milestone | Replay A | Replay B | Gameplay repeat | RNG repeat | Common teacher projection | Canonical outcome |
|---|---:|---:|---|---|---|---|
| 02C | 5-5 | 3-7 | exact | exact | exact | 3-5 plus 2 invalid aborts |
| 02D | 3-7 | 3-7 | exact | exact | exact | 3-5 plus 2 invalid aborts |
| 02E | 5-5 | 5-5 | exact | exact | exact | 3-5 plus 2 invalid aborts |
| 02F | 5-5 | 5-5 | exact | exact | exact | 3-5 plus 2 invalid aborts |
| 02G | 3-7 | 5-5 | exact | exact | exact | 3-5 plus 2 invalid aborts |

Every run and every milestone has the same ordered aggregate gameplay hash:

```text
536786652892a55b14f609083f4b14c0798cdcbab9e5537a549248726cd06737
```

and RNG hash:

```text
fa4bc7d2bc1dbf98bb972c58142dad56e129822a88945df33861c48272c12d52
```

The common PRIORITY teacher projection contains 4,018 records at every milestone and hashes to:

```text
cbfb204a417a86f1de5b140efb79137d3d2600abc682b7cf52f7027b61f958d7
```

The projection uses ordered turn, phase, acting player, top-level kind/source, and native selection mapping. All
adjacent comparisons C->D, D->E, E->F, and F->G have first divergence `-1` for gameplay, RNG, and this common
teacher surface.

## 9. Root cause and first-divergence evidence

The shared historical failure path is:

```text
PriorityActionDiagnostics.capture
  -> PriorityActionProvider.generatePriorityRequest
  -> PriorityCostFeasibility returns COST_ADJUSTMENT_CHOICE_REQUIRED
  -> UnsupportedPriorityActionException escapes the diagnostic boundary
  -> PhaseHandler/main game loop aborts
  -> SimulateMatch catches the exception
  -> finally calls game.setGameOver(Draw)
  -> Player.onGameOver marks both still-unresolved players as winners
  -> GameOutcome stores both in HashMap<RegisteredPlayer,...>
  -> getWinningPlayer/getWinningLobbyPlayer returns the first HashMap entry
```

`RegisteredPlayer` has identity equality/hash semantics, so the first entry is not a stable semantic choice across
fresh processes. This is why the score changes while gameplay, RNG, and teacher traces remain identical.

Games 2 and 3 abort identically at every milestone:

```text
game 2: Invoke the Firemind / COST_ADJUSTMENT_CHOICE_REQUIRED
game 3: Direct Current / COST_ADJUSTMENT_CHOICE_REQUIRED
```

Compact first-divergence evidence for both D->E and E->F:

| Surface | D->E | E->F |
|---|---|---|
| first differing gameplay record | none (`-1`) | none (`-1`) |
| first differing RNG draw | none (`-1`) | none (`-1`) |
| first differing common teacher decision | none (`-1`) | none (`-1`) |
| first differing value | invalid post-abort winner metadata | invalid post-abort winner metadata |

For game 2 the last normal gameplay checkpoint is index 990 (`GameEventPlayerControl`, turn 7 MAIN1), followed by
the invalid two-winner `GameEventGameOutcome` at 991 and FINAL at 992. The last equal RNG draw is global index 1095,
31 bits, value `2101831428`, from `forge.util.MyRandom#percentTrue`. Game 3 similarly ends with normal checkpoint
1144, invalid outcome 1145, FINAL 1146, and last equal RNG draw index 1320/value `933907951` from
`MyRandom#percentTrue`.

Thus the MODE observer diff in D->E did not change gameplay or RNG, and the CARD_SELECTION observer diff in E->F
did not restore gameplay. Both reported score transitions are different arbitrary resolutions of the same invalid
post-abort state. Same-commit C and G repeats changing between 5-5 and 3-7 prove the score artifact independently of
the cross-commit diffs.

## 10. Minimal fix

The production fix is deliberately narrow:

1. `PriorityActionDiagnostics.capture` catches diagnostic `RuntimeException`, emits a deterministic
   `PRIORITY_STATE`/`MAPPING_FAILED` record, and returns `null`, allowing the unchanged native callback/game loop to
   continue.
2. Priority diagnostics retain Forge's authoritative `Card.getAllPossibleAbilities` expansion, including
   alternative costs, may-play grants, modal backs, unlock actions, and face-down actions. The expansion runs in a
   request-local `SpellAbility.withAuditIdSequence` scope: new audit-only abilities receive unique negative IDs and
   never advance the global positive live-game `SpellAbility.maxId` sequence.
3. Discovered actions are detached with `SpellAbility.copy(host, player, true)` and all original live ability
   activators are restored in `finally`. PAYMENT mana previews use the same ID-preserving copy. A flashback
   regression prevents a future safety change from narrowing the canonical Forge action surface.
4. Trace outcome reduction rejects multiple winners instead of consulting unordered `GameOutcome` winner lookup.

There is no RNG rewind, seed manipulation, game clone, AI-heuristic legality call, general RNG rewrite, or gameplay
balance change. The first uncorrected current ON experiment reproduced the historical game-2 abort: only 286 RNG
draws occurred versus OFF's 712. After exception isolation, the two-game causal replay matched OFF byte-for-byte;
the final four ten-game runs then matched in full.

## 11. Permanent gates

- Known SHA-256 vector and first-divergence tests.
- Java Random parity and zero-metadata-draw tests.
- Trace output/version/absence, collector state/RNG neutrality, and ambiguous-outcome rejection.
- Fingerprint stability and mutation sensitivity.
- Simulator audit-RNG selection and explicit-seed requirement.
- Per-family `FORGE_STATE_V1` plus RNG-count assertions for all nine supported families.
- Priority live-activator, global ability-ID neutrality, full Forge alternative-cost coverage, and
  unsupported-exception isolation regressions.
- Whole-run reproducibility and OFF-vs-ON artifacts retained for review under
  `target/frl02k0-audit-20260810`.

## 12. Measurement reconciliation and latency interpretation

The supplemental fixed-head proactive ON-A run supplies the previously missing current cohort. ON-B reproduced the
same callback, request, forced, and strategic counts; latency is reported from ON-A to remain comparable with the
single-run reactive table.

| Family | Raw callbacks/observations | Atomic requests | Ratio | Forced | Strategic | Forced share | Generation p50/p95/p99 ns | Native p50/p95/p99 ns |
|---|---:|---:|---:|---:|---:|---:|---|---|
| PRIORITY | 4,385 | 4,385 | 1.000 | 530 | 3,855 | 12.1% | 1,120,900 / 4,107,400 / 6,586,200 | 2,095,200 / 19,159,800 / 46,483,800 |
| TARGET | 0 | 0 | n/a | 0 | 0 | n/a | unavailable | unavailable |
| PAYMENT | 789 | 430 | 0.545 | 64 | 366 | 14.9% | 389,100 / 1,131,700 / 2,144,100 | unavailable |
| MODE | 0 | 0 | n/a | 0 | 0 | n/a | unavailable | unavailable |
| CARD_SELECTION | 0 | 0 | n/a | 0 | 0 | n/a | unavailable | unavailable |
| ATTACK | 122 | 239 | 1.959 | 70 | 169 | 29.3% | 64,300 / 239,300 / 902,700 | 8,631,100 / 50,797,600 / 85,475,700 |
| BLOCK | 37 | 65 | 1.757 | 18 | 47 | 27.7% | 218,700 / 745,400 / 2,149,100 | 4,678,800 / 13,684,600 / 23,424,300 |
| MULLIGAN KEEP/REDRAW | 23 | 23 | 1.000 | 0 | 23 | 0.0% | 0 / 0 / 0 | 30,600 / 136,200 / 6,811,600 |
| MULLIGAN_BOTTOM | 3 | 3 | 1.000 | 0 | 3 | 0.0% | 100,600 / 6,736,700 / 6,736,700 | 3,273,000 / 3,334,800 / 3,334,800 |

The current final reactive ON-A evidence (one ten-game Izzet/Dimir run) demonstrates the opposite cohort:

| Family | Raw callbacks/observations | Atomic requests | Ratio | Forced | Strategic | Forced share | Generation p50/p95/p99 ns | Native p50/p95/p99 ns |
|---|---:|---:|---:|---:|---:|---:|---|---|
| PRIORITY | 5,120 | 5,001 | 0.977 | 1,358 | 3,643 | 27.2% | 1,901,800 / 18,811,000 / 155,543,800 | 4,654,800 / 29,219,300 / 54,909,700 |
| TARGET | 3 | 3 | 1.000 | 0 | 3 | 0.0% | 409,200 / 532,100 / 532,100 | unavailable |
| PAYMENT | 1,292 | 303 | 0.235 | 75 | 228 | 24.8% | 433,600 / 1,263,500 / 7,033,000 | unavailable |
| MODE | 7 | 2 | 0.286 | 0 | 2 | 0.0% | 1,278,200 / 137,215,900 / 137,215,900 | unavailable |
| CARD_SELECTION | 34 | 37 | 1.088 | 0 | 37 | 0.0% | 49,600 / 174,800 / 1,505,300 | 92,700 / 566,900 / 1,055,500 |
| ATTACK | 126 | 221 | 1.754 | 54 | 167 | 24.4% | 86,400 / 229,400 / 405,300 | 7,566,000 / 47,169,100 / 84,879,000 |
| BLOCK | 21 | 61 | 2.905 | 26 | 35 | 42.6% | 297,700 / 967,300 / 1,577,100 | 4,804,300 / 29,721,900 / 41,827,800 |
| MULLIGAN KEEP/REDRAW | 24 | 24 | 1.000 | 0 | 24 | 0.0% | 0 / 0 / 0 | 33,500 / 645,700 / 28,373,200 |
| MULLIGAN_BOTTOM | 4 | 4 | 1.000 | 0 | 4 | 0.0% | 52,800 / 82,000 / 82,000 | 231,100 / 3,932,000 / 3,932,000 |

The fixed-head proactive run exactly revalidates the retained proactive callback/request counts for PAYMENT
`789 -> 430`, ATTACK `122 -> 239`, and BLOCK `37 -> 65`. Combined with the fixed-head reactive cohort, the current
two-matchup PAYMENT total is `2,081 -> 733` (0.352), with 139 forced and 594 strategic requests (18.96% forced).
This replaces the pre-fix combined `1,637 -> 655` value for Revision 7; it must not be assembled from one current
cohort and one historical cohort. The fixed-head forced shares range from 0% to 42.6% across families and matchups.

Neutral generation, native teacher callback, and future external-policy inference are distinct costs. They are not
summed as a future controller cost because the external policy replaces the teacher callback. X_VALUE remains the
known generation outlier: the final focused measurement is approximately 31.56 / 40.26 / 48.98 ms p50/p95/p99.
No X architecture changed in this gate.

## 13. Verification and remaining risks

Final verification evidence:

```text
focused diagnostics baseline: 11 tests, 0 failures/errors/skips
baseline decision regression:  220 tests, 0 failures/errors/skips
final expanded gate regression: 255 tests, 0 failures/errors/skips
determinism trace focused:      3 tests, 0 failures/errors/skips
proactive fixed-head matrix:    40 games, 0 per-game trace/outcome differences
package:                        BUILD SUCCESS
configured Checkstyle:          0 violations
git diff --check:               clean
```

Remaining non-blocking limitations:

- Unsupported priority states are now explicit diagnostic failures and do not export incomplete requests; 119 such
  observations occurred in the current ten-game ON cohort.
- Several Nightveil Specter may-play results remain explicitly unmapped in diagnostics. They do not alter the native
  callback, gameplay hash, or RNG hash.
- The internal fingerprint is deliberately broad but not a serialization of every Forge object. Event order, state,
  RNG, teacher projection, and final outcomes all agree in the measured gates.
- Full trace mode is development/audit infrastructure and is intentionally heavier. Disabled mode does not snapshot
  game state.
- Proactive fixed-head evidence is retained under `target/frl02k0-proactive-fixed-20260809`; these audit artifacts
  are intentionally not committed.

No determinism blocker remains for this gate. CONFIRMATION was not started.
