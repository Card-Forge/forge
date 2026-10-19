# Port Log — Game State: M5 rules-kernel remainder (batch E)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `action.go`, `card.go`, `staticability.go`,
  `valid.go`, `activateability.go`, `driver.go`, `combatdamage.go`, `mulligan.go`

## Printed `*` power and toughness (CR 704.5f/704.5g)

Java: `CardFace.parsePT` (`CardFace.java:112-127`) strips the `*` and its sign: `*` is 0, `1+*` is 1, `7-*` is 7.
`Card.BasePower`/`BaseToughness` now read that through `carddb.PrintedPT`, not `strconv.Atoi`. Layer 7a
(characteristic-defining `SetPower$`/`SetToughness$`, `Count$` amounts via `resolveAmount`) already replaces the base.

| Case | Result |
| --- | --- |
| `*` with no characteristic-defining static | base 0, dies to 704.5f |
| `*` with a CDA that resolved (Maro, Tarmogoyf) | the CDA's value |
| `*` with a CDA whose amount `resolveAmount` cannot evaluate | unresolved (`ok=false`), left alone: `Card.starUnresolved` |
| counters, `MODIFYPT`/`SETPT` effects | folded by `Card.Toughness` already; not new in this batch |

Reason for the third row: base 0 is parsePT's convention, not the creature's real value, so killing it would be a guess
(GO-7). Java 704.5e does not exist in Forge (704.5d is the token rule, with a stack exception for copies);
`Game.ceaseCopiedSpell` already covers a copied spell.

Tests: `blockrelative_test.go` (`*`/`1+*`, counter), scenarios `sba-cda-toughness-equal-to-an-empty-hand-dies`,
`sba-cda-toughness-equal-to-cards-in-hand-survives`.

## CR 704.5u sectors and Space Beleren

Java: `GameAction.stateBasedAction704_5u` (`GameAction.java:1801-1828`), `Card.sector`/`assignSector`,
`CardProperty.java:119-126`. `assignSectors` (`action.go`) runs while a permanent has `Space sculptor`: players that do
not control one are walked in seat order, then each sculptor controller, and each creature without `Card.Sector`
(cleared when it leaves the battlefield, as Java's per-zone `Card` copy does) is assigned through
`PlayerController.ChooseSector(g, controller, creature, sectors)`. `valid.go` gains `Creature.ChosenSector` and
`Creature.DifferentSector`. `chooseSectorReadUnbuilt` (the GO-7 rejection) is gone: both chained effects resolve.

Fixture: `queue sector <alpha|beta|gamma>`. Scenarios `sector-space-beleren-minus-one-counters-only-the-chosen-sector`,
`sector-space-beleren-ultimate-destroys-only-the-chosen-sector`; Go tests in `sectorchoice_test.go` (assignment order,
reset on leaving, `+1` blocking, `-1`, `-5`). The `+1` Effect lives in the Command zone, which a fixture cannot name, so
its blocking is a Go test.

## `ValidAttackerRelative$` / `ValidBlockerRelative$`

Java: `StaticAbilityCantAttackBlock.java:263-268` — each side is matched against the other creature as the source.
`Game.relativeMatches` replaces `blockerRelativeMatches`: any valid string, with a Compare operand read from the
static's own face (`Game.relativeFace`, consulted by `compareOperand`) and `Count$CardPower`/`CardToughness` measuring
the source creature. A `CantBlockBy` static without `ValidAttacker$` now matches every attacker (`matchesValidParam`
passes on an absent param); it was skipped before. Covers Ironclaw Curse, Space Beleren and the Ring's level-1 line.

Scenarios `ironclaw-curse-enchanted-creature-can-still-block-a-smaller-attacker`,
`ironclaw-curse-other-creature-blocks-an-attacker-the-enchanted-one-cannot`; illegal blocks in `blockrelative_test.go`
(a scenario cannot script a refused block).

## Cast/activate restrictions

| Key | Result |
| --- | --- |
| `Activation$ Blessing` | `Player.Blessing`; set by `assignBlessings` (Ascend permanent half, 10 permanents) |
| `Condition$ Blessing` | same flag, `continuousConditionMet` (9 real lines resolve now) |
| `Activation$ Solved` | `Card.Solved` |
| `ActivationGameTypes$` | refuses: a `Game` has no variant applied (Java's `hasAppliedVariant` is false there) |
| `ClassLevel$` | refuses (GO-7): no Class level is tracked; 0 real `A:`/`S:` lines |
| `InstantSpeed$` | not read: only matters for a mana ability mid-payment, never interleaved here |
| Sneak declare-blockers | `sneakAvailable` ([`m5-t-sneak-charm.md`](m5-t-sneak-charm.md)) |

The instant/sorcery Ascend check at resolution is ported in [`m5-kernel-2.md`](m5-kernel-2.md). A blessing granted makes
the state-based pass repeat (CR 702.131d). Scenarios `ascend-citys-blessing-lets-arch-of-orazca-draw`,
`ascend-citys-blessing-skymarcher-aspirant-attacks-unblocked`; negatives in `activationrestrictions_test.go`.

## Turn driver

- EndTurn's Cleanup repeats: `endTurnEffect` records whether the Cleanup it began wants priority
  (`Game.endTurnCleanup`); `Game.Step` plays that window and begins another Cleanup (`PhaseHandler.java:447-449`).
  Scenarios `endturn-cleanup-trigger-gets-priority-and-resolves`,
  `endturn-time-stop-skips-to-cleanup-and-goes-to-the-graveyard`.
- `dealsInStep` reads `Combat.dealtFirstStrike` (`Combat.java:906-917`): whoever took part in the first-strike step sits
  out the regular one unless it has double strike, whatever keywords it holds then. Cleared after the regular step and
  with the combat. Tests in `combatfirststrikeset_test.go` (a fixture cannot change keywords between steps).
- Not ported: a cap on actions within one priority round (Java's guard is AI-only).

## Hand size, damage history, match series

- Unlimited or modified maximum hand size was already resolved (`Player.HandSizeLimit`); scenarios
  `cleanup-reliquary-tower-keeps-the-whole-hand`, `cleanup-thought-vessel-keeps-the-whole-hand` now pin it.
- `wasDealt[NonCombat|Combat]DamageLastTurn`: `Player.damageThisTurn`/`damageLastTurn` by kind, rotated at turn start;
  `matchesPlayerProperty` reads them (default `GE1`). The `ThisTurn` and `By` forms stay unrecognized. Scenarios
  `damage-history-command-the-stage-returns-after-noncombat-damage`,
  `damage-history-command-the-stage-stays-after-only-combat-damage`.
- `DealOpeningHandsAfter`: the loser of the last game decides, `isFirstGame` false, no coin flip. Fixture:
  `dealopeninghands after <p>`. Scenarios `opening-hand-after-a-lost-game-*`, test `matchseries_test.go`. The Match loop
  (series score, who lost) stays the caller's. Puzzle, Archenemy and Power Play starting rules stay unported.

## Not done

Flip and the Match series are ported in [`m5-kernel-2.md`](m5-kernel-2.md).
