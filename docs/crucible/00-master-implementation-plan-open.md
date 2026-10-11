# Crucible — Implementation Plan: Open

Everything still to do, sorted easiest first, and milestones M7-M9, not yet started. Roadmap overview and completed
milestones (M0-M4): [00-master-implementation-plan.md](00-master-implementation-plan.md). Currently in progress (M5-M6):
[00-master-implementation-plan-in-progress.md](00-master-implementation-plan-in-progress.md).

---

## Remaining work, easiest first

Sizes: **S** under a day, one sitting. **M** one porter batch, 1-3 days. **L** 1-2 weeks, a design call or several
batches. **XL** multi-week. Within a size the order is by value per effort. `R` numbers are this list's own; milestone
items keep their numbers (24-41). The refusal figures come from `crucible card-smoke -all`
([`smoke-probe.md`](porting/port-log/smoke-probe.md)): 86.0% of the 33,321 judged corpus cards play, an upper bound,
since silent misses are invisible. Re-rank with the command before starting any M item, because the counts move with
every port.

| #   | Work                                                                                                                                                                                                                                                                                                                                                                                                       | Milestone | Size | Needs                                  |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | ---- | -------------------------------------- |
| R2  | Retitle and redescribe Card-Forge/forge#12083: `TargetControls$` is removed, not guarded                                                                                                                                                                                                                                                                                                                   | M5        | S    | you                                    |
| R3  | File upstream the Forge bugs marked "Not filed" in [`forge-java-defects.md`](porting/forge-java-defects.md) (about 30 rows, one PR each) and the `doubling_chant.txt:5` `Choices$` without `Creature`                                                                                                                                                                                                      | M5        | S/PR | you; tests per PR                      |
| R5  | `Mode$ DrawFromBottom` (Lantern of Undersight, River Song): the draw reads the static                                                                                                                                                                                                                                                                                                                      | M5        | S    | —                                      |
| R6  | `Defined$ ParentTargetedController` (Draconic Roar's second `DealDamage`)                                                                                                                                                                                                                                                                                                                                  | M6        | S    | —                                      |
| R7  | `playeffect.go` still lists `sharesNameWith` as unresolved: resolve the forms `valid.go` answers, keep refusing the rest                                                                                                                                                                                                                                                                                   | M5        | S    | —                                      |
| R8  | `Static$ True` triggers resolve inline only for `ChangesZone`, `TapsForMana` and `Phase`; the other modes stack them (51 real lines: `SpellCast` 17, `TurnBegin` 8, `DamageDone` 6, `LandPlayed` 5, `Drawn` 5, ...)                                                                                                                                                                                        | M5        | M    | —                                      |
| R25 | Resolve the 401 cards `card-smoke` still cannot cast: an extra cost the board cannot pay, X spells whose targets scale with X, an Aura enchanting a card in a graveyard (Animate Dead, Dance of the Dead), "creature dealt damage this turn", and the Anchor to Reality / Arcum's Astrolabe group with no targeting. Per group, a probe variant or, where the engine silently fails to cast, an engine fix | M6        | M    | `card-smoke -all -list` for the names  |
| R9  | Refusal causes, largest first, one batch per group: `Discard` `ValidTgts$`/`Mode$` (225 cards), `Pump` `KW$` values (94), `ChangeZone` `Duration$`/`Chooser$`/`OriginAlternative$` (219), `DealDamage` `DamageSource$` (82), `PutCounter` `TargetMin$` (72), unresolved amounts in `DealDamage`/`Draw`/`Token`/`GainLife` (190)                                                                            | M6        | M x7 | `card-smoke` re-rank                   |
| R10 | Remaining `Mode$ OptionalCost` shapes: Quantum Reduction (`Spell.Teamwork` flash), Cryptex (`CollectEvidence` as an activation cost), Dragon's Fire (`RevealOrChoose`), Celestial Reunion, Redemptor Dreadnought; triggered `AB$` `Cost$` (44 cards)                                                                                                                                                       | M6        | M    | —                                      |
| R11 | Attack costs: `CombatUtil.getAttackCost` in `AttackConstraints`, Propaganda, `Mode$ CantAttackUnless`                                                                                                                                                                                                                                                                                                      | M5        | M    | —                                      |
| R12 | Graveyard-zone ETB and dies watchers (Bloodghast, Bridge from Below; about 38 lines) and hand/library-origin lines to a non-battlefield zone                                                                                                                                                                                                                                                               | M5        | M    | —                                      |
| R13 | Single-card shapes: Assimilation Aegis (`Clone` `UntilUnattached`), Loose in the Park (`Draft`, `ExiledWith`), Dinosaur Headdress (Craft), Curse of Leeches and Ludevic `Transform` scenarios, The Last Ronin's Technique (`Count$ValidStack`), Opposition Agent edge cases                                                                                                                                | M6        | M    | —                                      |
| R14 | Layer 4/5 effects of a granted static, `SharedRestrictions$ delved` (Soulflayer)                                                                                                                                                                                                                                                                                                                           | M5        | L    | design note on `traitGrants`           |
| R15 | Stickers: `StickerSheet`, `PutSticker`, `StickerPlaced`, `ZoneType.StickerSheets`, `Count$CardStickers` (about 48 cards)                                                                                                                                                                                                                                                                                   | M6        | L    | —                                      |
| R16 | `RollDice` `DicePTExchanges` (Vedalken Squirrel-Whacker): `chooseRollToSwap` and `chooseRollSwapValue` are two new `PlayerController` methods                                                                                                                                                                                                                                                              | M6        | L    | ADR first (ADRP-4); one card           |
| R17 | Close P4: the qualitative half, "every layer, every SBA" (Plan Section 3.2), with the audit written down and M5's exit gate declared                                                                                                                                                                                                                                                                       | M5        | L    | —                                      |
| R18 | The rest of `PlayerController` (about 90 Java decision methods against 52 here), `PhaseHandler` step bodies, `undoStack`, `CardState` flip trivia                                                                                                                                                                                                                                                          | M5        | L    | mostly M7's AI needs them              |
| R19 | Silent-miss audit: the probe cannot see a card that plays and misplays. A Java scenario runner for L2 parity (ADR-0010 names it; none exists) and a pass over the silent GO-7 gaps                                                                                                                                                                                                                         | M5-M6     | L    | oracle-java work                       |
| R20 | Gauntlet decklists in `gauntlets/modern/` with `gauntlet.toml`, then `crucible card-smoke -decks gauntlets/modern` to 100% played: the long tail of R9-R16                                                                                                                                                                                                                                                 | M6        | XL   | **your decklists** (none are invented) |
| R21 | Replay-parity harness (L3, item 32): `RecordingPlayerController` over the Java AI in `oracle-java`, `ReplayController` and the event normaliser in Go. **P5 gate**                                                                                                                                                                                                                                         | M6        | XL   | R18, R19                               |
| R22 | M7 AI port (items 33-35), P6 gate                                                                                                                                                                                                                                                                                                                                                                          | M7        | XL   | R18, R21                               |
| R23 | M8 simulation runner and telemetry (items 36-38), P7 gate                                                                                                                                                                                                                                                                                                                                                  | M8        | XL   | R22                                    |
| R24 | M9 reporting and the A/B swap optimizer (items 39-41), P8 gate                                                                                                                                                                                                                                                                                                                                             | M9        | XL   | R23                                    |

The rows are independent within a size unless `Needs` says otherwise. R2 and R3 need you, because they publish. R20 is
the only row whose input Claude must not invent: a fabricated metagame yields confident, wrong recommendations
([`meta-gauntlet.md`](research/meta-gauntlet.md)).

---

## Milestones

### M7 — AI port — 5–8 wks

33. `ComputerUtilMana` first (the runner cannot play a real game without mana planning), then `ComputerUtilCombat`,
    `ComputerUtilCard`, `CreatureEvaluator`.
34. `AiController` + per-API `SpellAbilityAi` decision logic; `.ai` profile loading.
35. Lookahead simulator on top of the cheap Go state clone. **Exit gate:** P6 gate — statistical parity within stated
    bounds vs. Java on the reference gauntlet.

### M8 — Simulation runner & telemetry — 2–3 wks

36. `internal/sim`: worker pool, seed management, gauntlet config, turn caps, crash isolation (a panicking game fails
    that game only).
37. `internal/telemetry`: recorder, castability probe, mana sampler, tenure tracking, aggregation.
38. `internal/store`: shard writers, `manifest.json`, and a streaming reader (ADR-0016). **Exit gate:** P7 gate — 100k
    games clean, deterministic re-run byte-identical.

### M9 — Reporting & the optimization loop — 2–4 wks

39. `internal/report`: matchup matrix w/ CIs, dead-card table, mana health, per-card impact (all four attribution modes,
    each labelled), opening-hand analysis, play/draw split.
40. Markdown + HTML + JSON output; `crucible report`.
41. **A/B swap optimizer**: propose candidate swaps from the dead-card and impact tables, run the confirmatory A/B
    gauntlet, rank by measured win-rate delta with CIs. This is the actual product. **Exit gate:** P8 gate — blind-test
    diagnosis matches expert assessment on known-good and known-bad decks.
