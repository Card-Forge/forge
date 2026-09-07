# MET — Metric Definitions

- **Status:** Active
- **Applies to:** every figure Crucible reports
- **Metrics version:** 1
- **Rule IDs:** cite as `MET-n`

Normative. A metric means what this document says it means, and nothing else.

---

## Why this exists before the engine

**A metric definition is a requirement on the engine, not a property of the report.**

"Mana flood" needs both surplus lands and nothing worth spending mana on (MET-11). An engine that never recorded
`ManaAvailable - ManaSpent` per turn cannot compute it, and the fix is not a query change — it is re-running the batch.
Every definition below therefore names the emission it depends on, and those emissions are the specification the
telemetry recorder is built against.

The second reason is worse. A metric that is subtly wrong is more damaging than one that is missing, because the report
still looks authoritative. "Mana screw: 18%" is unfalsifiable without knowing what counts as screw, and a reader has no
way to tell a considered threshold from an arbitrary one.

---

## MET-1 — Every output carries `MetricsVersion`

Stamped into every telemetry row, every stored shard and every report header. Changing any definition or default
threshold in this document increments it.

Runs with different `MetricsVersion` values are not comparable and tooling must refuse to merge them. Historical batches
stay interpretable because the version says which definitions produced them.

---

## MET-2 — Thresholds are configuration, and appear in the report

Every threshold below has a default and is overridable per run. **The values in force are printed in the report
header**, not just recorded in `manifest.json`.

An unlabelled percentage is not actionable. A reader must be able to see that "flood" meant `FloodStartTurn = 5` without
opening a config file.

---

## MET-3 — Every figure is labelled with how it was derived

Especially impact metrics (MET-20 to MET-23), where four estimators of very different strength coexist. A number printed
without its mode is a number that will be misread as causal.

---

# Dead cards

## MET-4 — A card is dead when it could not be used, not when it was not used

The distinction is the whole value of the family. A card that sat in hand because the deck could not cast it indicts the
**mana base or curve**. A card that sat in hand because the AI never wanted it indicts **the card**. These lead to
opposite deck changes.

**Requires:** the castability probe (MET-5).

## MET-5 — Castability probe

Once per turn, at end of the controller's precombat main phase with an empty stack, the engine evaluates every card in
hand and records whether it could be cast and, if not, why.

| Block reason                  | Meaning                                                |
| ----------------------------- | ------------------------------------------------------ |
| `BlockInsufficientMana`       | Total available mana is below the cost                 |
| `BlockColorUnavailable`       | Payable by quantity, not by colour — see MET-6         |
| `BlockNoLegalTarget`          | A targeting requirement cannot be met                  |
| `BlockTimingRestriction`      | Sorcery speed, wrong phase, once-per-turn already used |
| `BlockZoneOrStateRestriction` | An "only if" condition, threshold, or similar is unmet |
| `BlockOpponentLock`           | A static ability held by an opponent forbids it        |
| `BlockLandDropUsed`           | Land specifically, land drop already spent             |

Reasons are a bitmask; a card may be blocked several ways at once.

**Requires:** engine support. This is not derivable from an event stream after the fact — it is a question only the
rules engine can answer, and it must be asked while the game is running.

## MET-6 — Colour screw is measured by solving twice

`BlockColorUnavailable` is distinguished from `BlockInsufficientMana` by running the mana solver twice: once against the
real available sources, once against a hypothetical pool of the same size producing every colour.

Payable in the second and not the first means the card is blocked **on colour, not quantity** — which is the single most
actionable dead-card signal a deckbuilder can receive, because the fix is "add sources of colour X".

## MET-7 — Hand tenure

Opened when a card enters hand, closed when it leaves or the game ends.

| Field                        | Meaning                                           |
| ---------------------------- | ------------------------------------------------- |
| `EnteredTurn`, `Source`      | Opening hand, draw, bounce, tutor                 |
| `ExitTurn`, `Exit`           | Cast, discarded, bounced, still in hand at end    |
| `TurnsHeld`                  | Turns spent in hand                               |
| `TurnsBlocked`               | Turns the probe said not castable                 |
| `TurnsPlayableUnused`        | Turns the probe said castable and it was not cast |
| `BlockedBy`, `DominantBlock` | Union, and most frequent single reason            |

## MET-8 — `TurnsBlocked` and `TurnsPlayableUnused` are never summed

They are reported as separate columns and never combined into a single "dead" figure.

Merging them produces a report that blames the mana base for a weak card, or the card for a broken mana base. Both
recommendations are wrong, and both look reasonable.

## MET-9 — Dead rate

```text
DeadRate(card) = games where TurnsBlocked >= DeadThresholdTurns / games where the card was drawn
```

Default `DeadThresholdTurns = 2`. Reported alongside mean `TurnsBlocked`, the block-reason histogram, and dead rate
split by game outcome.

---

# Mana

**Requires:** a per-turn mana sample taken at end of turn, plus a record at each mana payment. Fields: lands in play,
lands in hand, non-land sources, untapped sources, available by colour, colours available, colours needed in hand, mana
available, mana spent, mana floated and lost, missed land drop, land drop available, curve target.

## MET-10 — Mana screw

```text
LandsInPlay < min(Turn, ScrewCurveCap)
  for >= ScrewConsecutiveTurns consecutive turns
  within turns 1..ScrewWindow
```

Defaults: `ScrewCurveCap = 4`, `ScrewConsecutiveTurns = 2`, `ScrewWindow = 6`.

Opening hands kept on one land or fewer are flagged separately and reported alongside, since a keep decision is a
different failure from a draw sequence.

## MET-11 — Mana flood requires two conditions, not one

```text
after Turn > FloodStartTurn:
      LandsInPlay + LandsInHand > CurveTarget + FloodExcess
  AND ManaAvailable - ManaSpent >= FloodSlack
```

Defaults: `FloodStartTurn = 5`, `FloodExcess = 2`, `FloodSlack = 2`.

**Both conditions are required, and that is the important part of this definition.** Surplus lands alone is not flood —
a ramp deck holding extra lands while deploying its curve is functioning exactly as designed. Flood is surplus lands
_and nothing worth spending mana on_. A single-condition definition reports the best draws of every ramp deck as
failures.

## MET-12 — Colour screw

Any turn on which a card in hand carries `BlockColorUnavailable` (MET-6).

Reported per missing colour and per card, never as a single rate. "Colour screw: 12%" tells a deckbuilder nothing; "12
games wanted a second black source by turn 3" tells them what to change.

## MET-13 — Unused mana

```text
UnusedMana = sum over turns of (ManaAvailable - ManaSpent)
FloatedLost = sum over turns of ManaFloatedLost
```

Reported raw and normalised per turn, split by game outcome. Floated-and-lost mana is kept separate because it indicates
a sequencing error rather than a deck construction problem.

## MET-14 — Missed land drops

Count and turn distribution, split into "no land in hand" and "had one, did not play it". The second is an AI or
sequencing issue and must not be reported as a mana base failure.

## MET-15 — Curve adherence

Fraction of turns where `ManaSpent >= min(Turn, DeckCurveTop)`, where `DeckCurveTop` is the highest mana value the deck
meaningfully wants to reach. Reported as a per-turn curve, not a single number.

---

# Impact

## MET-20 — Four estimators, and the weakest one is confounded

Naive `P(win | card was cast)` is **confounded by game length**: players who are winning cast more spells, so almost
every card appears to cause wins. The bias is worst for expensive cards, which are exactly the ones worth evaluating.

Crucible reports four modes and labels every figure with its mode (MET-3).

| Mode | Estimator                                                         | Strength                                                                                                        |
| ---: | ----------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
|    1 | `P(win \| cast)` vs `P(win \| not cast)`                          | Weakest. Observational. Reported, marked as such                                                                |
|    2 | `P(win \| cast by turn T)` vs baseline at T, over games live at T | Removes most game-length bias                                                                                   |
|    3 | `P(win \| in opening hand)` vs `P(win \| not in opening hand)`    | **Best cheap estimator.** Opener membership is randomised by the shuffle, so this is near-experimental for free |
|    4 | A/B swap — same gauntlet, one card exchanged                      | The only causal mode. The confirmatory gate                                                                     |

## MET-21 — Mode 3 leads the report

Because the shuffle randomises opener membership, mode 3 approximates a randomised trial at no extra cost. It is the
first impact figure a reader should see.

## MET-22 — No recommendation without a mode 4 result

Modes 2 and 3 rank candidates. **Mode 4 confirms them.** Nothing is emitted as a recommendation on the strength of an
observational estimator, however large the sample.

## MET-23 — Confidence intervals are mandatory

Every rate is reported with an interval. A difference without one is noise presented as a finding, and a matchup matrix
of point estimates over small samples is the easiest way for this tool to mislead.

---

# Game context

## MET-24 — Play and draw are recorded and reported split

`OnThePlay` is recorded per game and every metric is reportable split by it. The play/draw win-rate gap is itself a
diagnostic: a large gap indicates a deck that cannot win from behind.

## MET-25 — Opening hands

Keep or mulligan decisions, hand size kept, land count, colour coverage, curve signature, and the resulting win rate.

Surfaces statements a deckbuilder can act on directly — "this deck mulligans 22% of games", "five-land keeps win 31%".

---

## Threshold defaults

| Threshold               | Default | Used by |
| ----------------------- | ------: | ------- |
| `DeadThresholdTurns`    |       2 | MET-9   |
| `ScrewCurveCap`         |       4 | MET-10  |
| `ScrewConsecutiveTurns` |       2 | MET-10  |
| `ScrewWindow`           |       6 | MET-10  |
| `FloodStartTurn`        |       5 | MET-11  |
| `FloodExcess`           |       2 | MET-11  |
| `FloodSlack`            |       2 | MET-11  |

Defaults are a starting point, not a finding. They are expected to be revised once real batches exist, and revising one
increments `MetricsVersion` (MET-1).

---

## Engine emissions this document requires

The specification the telemetry recorder is built against. Anything missing here cannot be computed later.

| Emission                                              | Required by                 | Milestone                        |
| ----------------------------------------------------- | --------------------------- | -------------------------------- |
| Castability probe, once per turn, with block reasons  | MET-4, MET-5, MET-6, MET-12 | M8, needs engine support from M5 |
| Two-pass mana solve for colour attribution            | MET-6, MET-12               | M8                               |
| Hand tenure events — enter, exit, source, disposition | MET-7, MET-9                | M8                               |
| Per-turn mana sample                                  | MET-10 to MET-15            | M8                               |
| Mana floated and lost at each step boundary           | MET-13                      | M8                               |
| Per-game outcome, turn count, `OnThePlay`             | MET-20 to MET-24            | M8                               |
| Opening hand contents and mulligan decisions          | MET-21, MET-25              | M8                               |
| Run seed and game index on every row                  | reproducibility (ADR-0006)  | M8                               |

## Open questions

- `DeckCurveTop` (MET-15) has no definition yet. Highest mana value in the deck overpays for a single top-end card;
  something like the 90th percentile of the non-land curve is probably closer, and needs real batches to settle.
- Thresholds are all unvalidated. They are defensible starting points, not measurements.

## Related

- [`../00-master-implementation-plan.md`](../00-master-implementation-plan.md) — Phase 4, the telemetry design
- [`../adr/0006-determinism-and-rng.md`](../adr/0006-determinism-and-rng.md) — seeds recorded per row
- [`../guidelines/00-documentation-style.md`](../guidelines/00-documentation-style.md) — DOC-4, every rule carries a
  reason
