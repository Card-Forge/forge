# AI performance measurement

This is the measurement foundation for AI performance work. It exists so that changes to the AI and
rules engines can be judged against a baseline **taken on the same revision**, rather than against
profiles gathered on older ones. Nothing here changes how the AI plays; it only observes.

Everything is **off by default** and costs a single volatile boolean read per probe when disabled.

## Contents

| Piece | Where | What it does |
|---|---|---|
| `PerfProbe` | `forge-core`, `forge.util.perf` | The measurement seam: opens/closes decisions, counts work, times spans, records traces. |
| `PerfCounter` / `PerfTimer` | `forge-core`, `forge.util.perf` | The counters and wall-clock spans that are recorded. |
| `DecisionRecord` / `PerfAccumulator` | `forge-core`, `forge.util.perf` | Per-decision and whole-run storage. |
| `PerfReport` | `forge-core`, `forge.util.perf` | Latency distributions (median/p95/p99) and work totals, as JSON. |
| `DecisionTraceWriter` | `forge-core`, `forge.util.perf` | Decision traces as JSON lines, for parity diffs. |
| `TracingRandom` | `forge-core`, `forge.util.perf` | A seeded `Random` that counts and traces draws without changing the sequence. |
| `GameStateDigest` | `forge-game`, `forge.game` | Canonical, order-sensitive state dump and its SHA-256. |
| `GameTraceDescriptors` | `forge-game`, `forge.game` | Stable text descriptors for cards, players and abilities. |
| `JfrPerfSink` | `forge-gui-desktop`, `forge.view` | One JFR event per decision. Desktop only — `jdk.jfr` does not exist on Android. |
| `AiBenchmark` | `forge-gui-desktop`, `forge.view` | The `forge bench` command line harness. |

## Running a benchmark

```
forge bench -d <deck1> <deck2> [-D deckDir] [-n games] [-w warmupGames] [-s seed]
            [-f format] [-a profile1 profile2] [-c simTimeoutSeconds]
            [-o outputDir] [-t] [-jfr] [-q]
```

For example, from the `forge-gui` directory:

```
java -jar ../forge-gui-desktop/target/forge-gui-desktop-*-jar-with-dependencies.jar bench \
     -d "deck-a.dck" "deck-b.dck" -D res/geneticaidecks -n 20 -w 2 -s 7 -c 120 -o baseline
```

This writes `baseline/report.json` and prints a summary:

```
ai-benchmark:
  priority n=589    median=2.425ms p95=11.365ms p99=19.229ms max=37.534ms
  attack   n=11     median=4.368ms p95=10.927ms p99=10.927ms max=10.927ms
  block    n=2      median=3.302ms p95=3.511ms p99=3.511ms max=3.511ms
work per decision:
  priority decisions=1.0 candidateCards=15.4 candidateAbilities=5.1 ... zoneAggregateQueries=913.5 ...
```

The work counters are the point of the exercise: a decision that takes twice as long because it saw
twice as many candidates is not the same finding as one that takes twice as long over the same
input, and only the counters can tell those apart.

### Determinism

Game `i` is played with seed `baseSeed + i`, installed into the process-global generator before the
game starts. Re-running the same command must reproduce the same winners, turn counts and state
digests — and, with `-t`, a byte-identical trace file. If it does not, the fixture is not usable as a
benchmark and the divergence must be fixed before any timing is believed.

`TracingRandom` overrides only `Random.next(int)`, which every other `Random` method is built on, and
delegates to `super`. The sequence for a given seed is therefore bit-identical to `new Random(seed)`;
installing it observes the stream without altering it.

### Warm-up

`-w N` plays `N` games with probing switched off before the measured ones, so JIT compilation and
first-use caches are not charged to the samples. Warm-up seeds are deliberately disjoint from the
measured seeds.

## Comparing two builds

The default pass criterion for a behaviour-equivalent optimisation is **exact trace identity**.

```
# on the baseline build
forge bench -d ... -s 7 -n 20 -t -o baseline
# on the optimised build
forge bench -d ... -s 7 -n 20 -t -o optimised
diff baseline/trace.jsonl optimised/trace.jsonl
```

An empty diff means the ordered candidate lists, heuristic verdicts, chosen abilities, declared
attackers and blockers, simulated branch scores and RNG draws all matched. Then re-run **without**
`-t` on both builds to compare timings; tracing builds strings on decision paths and must never be
mixed into a timing run.

If the traces differ, do not accept "equivalent score" as a defence. Target choice, ordering, RNG
consumption and later state can all differ while scores agree.

## Profiling alongside

```
java -XX:StartFlightRecording=filename=forge-ai.jfr,settings=profile,dumponexit=true \
     -jar forge-gui-desktop-*-jar-with-dependencies.jar bench ... -jfr
```

`-jfr` adds a `forge.AiDecision` event per decision carrying the same work counters as the JSON
report, so method-level CPU and allocation samples can be normalised by how much work each decision
actually faced.

## Using the probes from tests

```java
PerfProbe.reset();
PerfProbe.setEnabled(true);
PerfReport report = new PerfReport("my-scenario");
PerfProbe.addSink(report);
try {
    // ... play the scenario ...
} finally {
    PerfProbe.setEnabled(false);
}
```

`PerfProbe.getGlobal()` holds counter and timer totals for **all** probed work, including rules work
performed between decisions — phase transitions, stack resolution, combat setup. `PerfReport` holds
only work that occurred inside a decision window. Both matter: per-decision attribution for latency,
global totals for whole-game cost.

Trace entries are the exception: they are only recorded inside a decision. An entry emitted between
decisions has no decision to be compared against and no owner to drain it, so recording it would
grow without bound over a long batch.

## Adding a probe

```java
final long token = PerfProbe.start(PerfTimer.SOMETHING);
try {
    PerfProbe.count(PerfCounter.SOMETHING_ELSE);
    return doTheWork();
} finally {
    PerfProbe.stop(PerfTimer.SOMETHING, token);
}
```

Rules to follow:

- **Never change control flow.** Wrap the existing body in a private `...Impl` method and add a thin
  measuring wrapper, rather than editing the body in place.
- **Guard argument construction** with `PerfProbe.isEnabled()` (or `isTracing()` for traces) whenever
  building the argument costs anything. `count()` and `trace()` are cheap to call but their arguments
  are evaluated first.
- **Never read game state that can mutate it.** Descriptors must not set an activating player, tap a
  card, or trigger a rules recalculation; a diagnostic that changes what it observes is worse than no
  diagnostic.
- **Traces must be stable.** No object hash codes, no localised strings, no iteration-order-dependent
  dumps. Two runs of one fixture must produce byte-identical output.
- **Do not use `jdk.jfr` outside `forge-gui-desktop`.** It does not exist on Android. Implement
  `PerfSink` in the desktop module instead.

## Design notes

**Nesting.** Decisions nest: a simulated branch resolves combat inside a copied game and calls back
into the block controller. A nested decision does not get its own record; its work is attributed to
the outermost decision, because that is the unit a player waits for. Re-entrant occurrences of the
same timer are folded into the outermost span, so a recursive rules path cannot multiply its own
elapsed time.

**Threads.** A conventional priority decision does most of its work on a `"Game AI Eval-N"` watchdog
thread while the game thread waits on the decision's future. Both write to the same record, so
counters and timers use atomic arrays. This is bookkeeping for the measurement, not a claim that the
AI is thread-safe.

**Spans are inclusive.** `scoreEvaluation` contains the `gameCopy` the evaluator makes for combat
lookahead. Timers must not be summed across the enum and presented as a decision breakdown.

**What the digest covers.** Turn, phase, active and priority player, each player's life and counters,
every zone's contents in zone order (including hidden zones — library order changes later decisions),
per-card identity/name/owner/controller/tapped/sick/face-down/phased-out/timestamp/counters, the
stack, and combat with ordered blockers. Derived characteristics such as computed power/toughness are
deliberately excluded: reading them can trigger recalculation, and they are a function of the state
that is captured anyway.
