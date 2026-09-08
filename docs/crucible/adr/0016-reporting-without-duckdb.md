# ADR-0016 — Reporting Without DuckDB for v1

- **Status:** Accepted
- **Date:** 2026-09-08
- **Deciders:** `jc@archlab.pl`
- **Narrows:** [ADR-0014](0014-telemetry-storage-format.md)'s querying decision. Its storage decision is unchanged

## Context

[ADR-0014](0014-telemetry-storage-format.md) chose DuckDB for reporting, invoked as a tool rather than imported, and
rejected hand-written Go aggregation as "reimplementing group-by and window functions to avoid a tool". That reasoning
was written against the full report set: matchup matrices with confidence intervals, per-card impact across four
attribution modes, dead-card histograms.

The v1 report set is smaller than the one that argument assumed. Dead-card rate, mean turns stuck, block-reason
histogram, mana screw and flood rates, curve adherence, win rate on the play and on the draw
([`../telemetry/metric-definitions.md`](../telemetry/metric-definitions.md), `MET-4` to `MET-15`) are **counters and
means grouped by one or two keys**. No joins, no windows, no correlated subqueries.

The queries that genuinely want SQL are the per-card impact estimators (`MET-20` to `MET-23`), and those depend on a
causal-attribution method that has no ADR yet.

## Decision Drivers

- Every tool in the chain is a prerequisite someone has to install before they can read their own results.
- The write path is already streaming; a streaming reader is the same shape and needs no new concepts.
- The cost of being wrong is low in one direction and not the other: adding DuckDB later costs nothing, because the
  shards do not change.

## Considered Options

1. **DuckDB from M8**, as ADR-0014 decided.
2. **Go aggregation over the shards for v1; DuckDB when a query stops being a counter.**
3. Go aggregation permanently, reimplementing whatever SQL would have given.

Rejections:

- **Option 1** installs an analytical engine to compute means. It also splits the prerequisites: `crucible run` needs
  nothing, `crucible report` needs DuckDB, which is the gap ADR-0014's own consequences flagged as "needs saying in the
  runbook".
- **Option 3** is what ADR-0014 rejected, and rightly: the impact estimators are joins and windows, and writing those by
  hand to avoid a tool would be stubbornness rather than simplicity.

## Decision

**v1 reports are computed in Go, streaming the shards once. DuckDB arrives with the first query that needs SQL.**

| Report                                                  | Shape                            | v1  |
| ------------------------------------------------------- | -------------------------------- | --- |
| Win rate, overall and split by play/draw (`MET-24`)     | Counter                          | Yes |
| Dead-card rate, turns stuck, block-reason histogram     | Group by card, counter and mean  | Yes |
| Mana screw, flood, colour screw, curve adherence        | Group by turn, counter and mean  | Yes |
| Matchup matrix                                          | Group by opponent deck           | Yes |
| Per-card impact, four estimators (`MET-20` to `MET-23`) | Joins, windows, confidence bands | No  |

The reader is `encoding/json` over `compress/gzip`, one shard at a time, accumulating into the same structs the recorder
already produces. Nothing is held in memory but the accumulators, so the report cost is one pass and a few megabytes
whatever the run size.

**DuckDB stays exactly where ADR-0014 put it — a tool, never a dependency — and is now also deferred.** The shard format
does not change, so the first SQL query works against every run recorded before it existed:

```sql
SELECT * FROM read_json_auto('runs/<id>/shards/*.ndjson.gz');
```

## Consequences

**Good.** `crucible run` and `crucible report` have the same prerequisites, which is none. One less tool to pin, install
and version in CI. The v1 report path is testable with an ordinary Go test over a fixture shard, rather than by shelling
out to a database.

**Bad.** The first estimator that needs a window function will need DuckDB, and the boundary between "counter" and
"wants SQL" is a judgement call that will be made under deadline. Ad-hoc exploration — the thing SQL is genuinely good
at — is unavailable until then, so an unanticipated question means writing Go rather than typing a query.

**Neutral.** Some aggregation logic written for v1 becomes redundant when DuckDB arrives. That is a few hundred lines,
and it is the cost of not installing an analytical engine to compute a mean.

## Related

- [ADR-0014](0014-telemetry-storage-format.md) — the storage format this keeps, and the querying half it narrows
- [ADR-0013](0013-telemetry-event-bus.md) — what produces the rows
- [`../telemetry/metric-definitions.md`](../telemetry/metric-definitions.md) — which metrics are counters and which are
  estimators
- [`../design/telemetry-pipeline.md`](../design/telemetry-pipeline.md) — stages 6 and 7
