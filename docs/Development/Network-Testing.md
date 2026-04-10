# Network Testing

Automated testing tools have been developed for validating the network play pipeline. These run AI-vs-AI games over actual TCP connections and analyze the results, checking for crashes, desync, and network performance issues.

> [!NOTE]
> These tools are specifically for testing the **network pipeline** (server, client, sync, protocol). If you just want to run AI-vs-AI games without network overhead, see the [command-line controls on the AI wiki page](https://github.com/Card-Forge/forge/wiki/ai).

All tests live under `forge-gui-desktop/src/test/java/forge/net/`.

---

# Prerequisites

- Java 17+ and Maven installed
- Forge repository cloned and buildable (`mvn -pl forge-gui-desktop -am install -DskipTests`)
- No special network configuration needed — tests use localhost

---

# How It Works

Every test game starts a real TCP server and connects one or more headless AI clients to it. The server and clients exchange game state over the network just like a real multiplayer game — the only difference is there's no GUI and the AI makes all decisions. By default this uses delta sync, but you can test the full-state sync path instead with `-Dforge.deltasync=false`.

Batch tests use random preconstructed decks from Forge's built-in quest precons (or commander precons for Commander-format games). The vertical slice test uses minimal 10-card basic land decks instead, so games end quickly by decking out.

Batch tests automatically run the log analyzer on completion and produce a markdown report alongside the log files. You can also run the analyzer separately against any log files — including logs from real games (see [Log Analyzer](#log-analyzer) below).

There are two execution modes for running batches of games:

- **Sequential** — all games run in the same JVM process, one after another. Uses minimal resources and keeps all output in one place, but is slow for large batches.
- **Parallel** — each game spawns its own JVM process (via `MultiProcessGameExecutor` and `ComprehensiveGameRunner`). Much faster for large batches, but each game is a full JVM with a server and client, so it can be demanding on CPU and memory. Adjust `-Dtest.batchSize` to control how many games run concurrently — lower it if your machine struggles.

The key entry points in `NetworkPlayIntegrationTest`:

| Test Method | Mode | What It Does                                                       |
|---|---|--------------------------------------------------------------------|
| `testTrueNetworkTraffic` | Single game | Vertical slice — validates the network pipeline with minimal decks |
| `testConfigurableSequential` | Sequential | Configurable batch in one JVM                                      |
| `testConfigurableParallel` | Parallel | Configurable batch in separate JVMs                                |
| `runQuickDeltaSyncTest` | Parallel | Preset: 10-game mixed batch for quick checks                       |
| `runComprehensiveDeltaSyncTest` | Parallel | Preset: 100-game mixed batch for full validation                   |
| `analyzeLog` | N/A | Analyze existing log files without running games                   |

---

# Running Tests

All network tests are run via Maven from the repository root. Most batch tests are gated behind `-Drun.stress.tests=true` so they don't run during normal CI builds.

The basic command structure is:

```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="NetworkPlayIntegrationTest#<testMethod>" \
    -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false \
    <configuration properties>
```

## Configuration Properties

All entry points accept the same configuration properties:

| Property | Default | Description |
|---|---|---|
| `-Dtest.2pGames=N` | 3 | Number of 2-player games |
| `-Dtest.3pGames=N` | 0 | Number of 3-player games |
| `-Dtest.4pGames=N` | 0 | Number of 4-player games |
| `-Dtest.commanderPct=N` | 30 | Percentage of games using Commander format |
| `-Dtest.batchSize=N` | 10 | Games to run in parallel at once (lower this if your machine struggles) |
| `-Dtest.timeoutMs=N` | 300000 | Per-game timeout in milliseconds (default 5 min) |

## Entry Points

**`testTrueNetworkTraffic`** — single 2-player game using minimal 10-card basic land decks (players deck out in a few turns). A fast vertical slice that validates the network pipeline end-to-end, not a real game. Under 60 seconds. The only network test that runs in CI — does not require `-Drun.stress.tests`.

**`testConfigurableSequential`** — runs games sequentially in one JVM. Defaults to 3 x 2-player if no properties are set.

**`testConfigurableParallel`** — runs games in parallel, each in its own JVM. Same defaults.

**`runQuickDeltaSyncTest`** — preset: 10 games (5 x 2-player, 3 x 3-player, 2 x 4-player) with relaxed pass thresholds.

**`runComprehensiveDeltaSyncTest`** — preset: 100 games (50 x 2-player, 30 x 3-player, 20 x 4-player) with stricter thresholds. `-Dtest.*` properties override the preset defaults.

These two are standardised configurations used during network development to provide consistent, repeatable validation runs.

## Examples

Quick pipeline check (no stress flag needed):
```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="NetworkPlayIntegrationTest#testTrueNetworkTraffic"
```

5 sequential games for debugging:
```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="NetworkPlayIntegrationTest#testConfigurableSequential" \
    -Dtest.2pGames=3 -Dtest.3pGames=1 -Dtest.4pGames=1 \
    -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
```

20-game custom mix with more Commander:
```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="NetworkPlayIntegrationTest#testConfigurableParallel" \
    -Dtest.2pGames=10 -Dtest.3pGames=5 -Dtest.4pGames=5 \
    -Dtest.commanderPct=50 \
    -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
```

Full-state sync test (no deltas):
```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="NetworkPlayIntegrationTest#runQuickDeltaSyncTest" \
    -Dforge.deltasync=false \
    -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
```
---

# Log Analyzer

Batch tests automatically run the log analyzer and write a report to the log directory (named `network-debug-{batchId}-results.md`).

The generated markdown report includes:

- **Summary** — total games analyzed, success/failure counts
- **Bandwidth** — delta sync compression savings (how much smaller delta packets are vs full state)
- **Errors** — most common error patterns, grouped and sorted by frequency
- **Checksum mismatches** — games where client and server state diverged, with context
- **Breakdowns** — success rates split by player count (2/3/4) and format (Constructed/Commander)

You can also run the analyzer manually against any network log files — including logs from real multiplayer games — using the `analyzeLog` entry point. You can point it at a single log file or a directory.

```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="NetworkPlayIntegrationTest#analyzeLog" \
    -Dlog.input="C:/Users/YourName/AppData/Roaming/Forge/networklogs/" \
    -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
```

If no output path is given, the report is written as `network-log-analysis.md` in the same directory as the input. Optionally specify the output path:

```bash
    -Dlog.output="C:/Users/YourName/Desktop/report.md"
```

---

# Log Location and Cleanup

Network logs are stored in your Forge data directory under `networklogs/`:

| Platform | Path |
|---|---|
| **Windows** | `%APPDATA%/Forge/networklogs/` |
| **macOS** | `~/Library/Application Support/Forge/networklogs/` |
| **Linux** | `~/.forge/networklogs/` |

Batch tests group their logs into subdirectories named by timestamp (e.g., `run20260410-143022/`). Each game within a batch gets its own log file.

Forge automatically cleans up old log files, keeping the 10 most recent batches. This can be configured in network preferences (`NET_LOG_CLEANUP_ENABLED`, `NET_MAX_LOG_FILES`).