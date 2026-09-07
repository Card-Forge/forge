# ADR-0014 — Telemetry Storage Format

- **Status:** Proposed
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

[ADR-0002](0002-toolchain-and-dependency-policy.md) named this decision in advance as the point where the
near-zero-dependency rule meets reality:

> Telemetry storage at M8 needs zstd compression and a columnar format; neither is in the standard library. That
> decision belongs to ADR-0014, and until it is made the answer is `compress/gzip` and NDJSON.

Naming the pressure point was meant to stop the rule being broken quietly. This is where it gets decided openly.

**The volume, from [ADR-0013](0013-telemetry-event-bus.md)'s ~80 rows per game:**

| Run        |       Rows | NDJSON raw | gzip (~6x) | zstd (~8x) |
| ---------- | ---------: | ---------: | ---------: | ---------: |
| 10^5 games |  8,000,000 |     1.2 GB |    0.20 GB |    0.15 GB |
| 10^6 games | 80,000,000 |    12.0 GB |    2.00 GB |    1.50 GB |

**zstd buys 0.5 GB on the largest realistic run.** That is the number the dependency question turns on, and it is
smaller than expected — the anticipation in ADR-0002 was written without it.

Reporting is the other half. Reports are ad-hoc analytical queries over tens of millions of rows: matchup matrices with
confidence intervals, per-card impact across four attribution modes, dead-card histograms. Hand-writing those
aggregations in Go is possible and unpleasant; SQL over columnar storage is the right tool.

## Decision Drivers

- ADR-0002: runtime is stdlib only, and any addition needs an ADR — this one.
- Reports are analytical queries, not lookups.
- A run must be readable years later, by whatever exists then.
- Writing happens per worker, concurrently; reading happens once, offline.

## Considered Options

### Storage

1. **NDJSON + gzip, stdlib.** **Chosen.**
2. NDJSON + zstd. Rejected — a dependency for 0.5 GB on a run that produces 2 GB either way.
3. Parquet. Genuinely better for the query side, but every Go implementation is a substantial dependency, and it makes
   the write path — concurrent, append-only, per worker — harder rather than easier.
4. SQLite or an embedded database as the primary store. Rejected — cgo, and it turns a concurrent append into a
   coordinated write.

### Querying

1. Hand-written Go aggregations. Rejected — reimplementing group-by and window functions to avoid a tool.
2. **DuckDB, invoked as a tool rather than imported as a dependency.** **Chosen.**
3. DuckDB as a linked library. Rejected — cgo, and it would make an analytical engine a runtime dependency of a
   simulator that does not query anything.

## Decision

**Storage is NDJSON + gzip, written with the standard library. Nothing is added to the runtime dependency list.**

```text
runs/<run-id>/
  manifest.json          git SHA, engine + schema + metrics versions, seeds,
                         deck hashes, gauntlet, every metric threshold
  shards/
    worker-000.ndjson.gz one writer per worker, append-only, never merged in place
    worker-001.ndjson.gz
```

One writer per worker means no coordination, which is what keeps [ADR-0005](0005-concurrency-model.md)'s independence
intact all the way to disk. Shards are immutable once closed.

**Querying is DuckDB, and this is the part worth being precise about.** DuckDB reads gzipped NDJSON directly, so
reporting is:

```sql
SELECT * FROM read_json_auto('runs/<id>/shards/*.ndjson.gz');
```

**DuckDB is a tool, not a dependency.** It is invoked as a process, exactly like `prettier` and `golangci-lint`, and
sits in [ADR-0002](0002-toolchain-and-dependency-policy.md)'s third scope — "invoked, never imported, pinned by version
in CI". Nothing links it, `crucible run` does not need it installed, and a run's output remains readable without it.

That distinction is the whole decision. The tension ADR-0002 anticipated dissolves once the analytical engine is allowed
to be a tool rather than a library, and the runtime stays stdlib-only.

**Parquet is deliberately deferred, not rejected forever.** If report queries become slow enough to matter, DuckDB can
convert shards to Parquet as a post-processing step — still a tool, still no import. The measurement that would justify
it does not exist yet, and adding a columnar writer now would be a dependency bought on speculation.

## Consequences

**Good.** The runtime dependency list stays empty, which was ADR-0002's target and is now tested rather than asserted.
NDJSON is readable by anything, including a text editor and `zcat`, so a run from today is interpretable in five years
without Crucible. Concurrent append with no coordination keeps the write path as simple as the concurrency model.
Reporting gets full SQL without linking an analytical engine into a simulator.

**Bad.** 2 GB per million-game run is real disk, and gzip rather than zstd concedes 25% of that for the sake of a rule.
NDJSON is also slow to scan compared to a columnar format, so report latency will be worse than Parquet would give —
acceptable while reports are run occasionally, and the reason the Parquet door is left open. Reporting additionally
requires a tool the simulator does not, so "it ran" and "I can read the results" have different prerequisites, which
needs saying in the runbook.

**Neutral.** Shards are per worker rather than per anything meaningful, so shard count varies with `GOMAXPROCS` while
contents do not. Queries glob, so this is invisible until someone tries to diff two runs' shard layouts and finds they
differ without the data differing.

## Related

- [ADR-0002](0002-toolchain-and-dependency-policy.md) — the pressure point this resolves, and the tool/dependency split
  that resolves it
- [ADR-0005](0005-concurrency-model.md) — one shard writer per worker
- [ADR-0013](0013-telemetry-event-bus.md) — what produces the rows
- [`../telemetry/metric-definitions.md`](../telemetry/metric-definitions.md) — MET-1, versioning stamped into every
  shard
