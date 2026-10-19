# Port Log — Game State: spell-only stack removal, `TokenBlocking$`, random targets, one APNAP pass per event, damage-history counts (R9j)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `countereffect.go`, `removefromgameeffect.go`
  (`ceaseToExist`), `replacementevents.go` (`counterReplaced`), `stack.go` (`removeSpellFromStack`),
  `changecombatantseffect.go` (`addToCombatBlocking`, `defenderPlayerByAttacker`), `tokeneffect.go`,
  `copypermanenteffect.go`, `blockerstriggers.go` (`runAttackerBlockedOnce`), `trigger.go` (`beginTriggerBatch`,
  `endTriggerBatch`, `damageEntry.Record`), `attack.go`, `block.go`, `targeting.go` (`chooseTargets`, `randomTargets`),
  `chaintargets.go`, `random.go` (`randomInt`), `exchangecontroleffect.go`, `gaincontroleffect.go`, `fighteffect.go`,
  `game.go` (`cardDamage`, `cardDamageThisTurn`, `triggerBatch`), `playerproperty.go` (`logCardDamage`),
  `combatdamage.go`, `excessdamage.go`, `damage.go` (`ExcessReceived`), `amountheads.go` (`damageThisTurnCount`),
  `amountcontext.go` (`CardTriggeredTarget`), `amountpaid.go`, `turn.go`
- **Java:** `MagicStack.java:792-805`, `:851-880`; `CounterEffect.java:247-272`; `GameAction.java:969-972`,
  `:2738-2767`; `ChangeZoneEffect.java:577-579`; `SpellAbilityEffect.java:758-829`; `TokenEffectBase.java:188`;
  `Combat.java:289-306`, `:374-382`, `:527-538`; `TargetRestrictions.java:202-207`; `TargetSelection.java:122-137`
  (forge-gui); `Aggregates.java:131-142`, `:191-193` (forge-core); `AbilityUtils.java:702-707`, `:2406-2408`,
  `:2434-2450`; `Game.java:1334-1368`; `CardDamageHistory.java:236-281`; `CardDamageTable.java:113-163`;
  `CardCopyService.java:331-333`; `Card.java:6181-6205`

## A spell leaves the stack alone

`MagicStack.remove(Card)` drops spell items only (`si.isSpell()`, `:799-805`); `CounterEffect.removeFromStack` drops the
countered item (`remove(si)`, `:258`). Three Go sites dropped every item sourced by the card, so a "when you cast this
spell" trigger died with its spell. All three now call `removeSpellFromStack`.

| Site | Java |
| --- | --- |
| `counterEffect` spell branch | `CounterEffect.java:258` |
| `ceaseToExist` (RemoveFromGame) | `GameAction.java:970-972` → `MagicStack.remove(Card)` |
| `counterReplaced` (Guile's exile) | the ReplaceWith$ ChangeZone, `ChangeZoneEffect.java:577-579` |

Every spell push sets `Ability.spell` (the cast paths in `castspell.go`, `copySpell`), so the filter finds the spell.

## `TokenBlocking$`

`addToCombatBlocking` is `addToCombat`'s blocking half, run per token by Token and CopyPermanent after the attacking
half (`TokenEffectBase.java:188`). CopyPermanent read neither `TokenBlocking$` nor refused it before; Mirror Match was
refused earlier by RepeatEach's `ChangeZoneTable$`.

| Step | Java |
| --- | --- |
| A creature, during combat | `:761` |
| Attacker = first card the Defined$ names | `:796` |
| Token's controller defends against the attacker | `:797`, `getDefenderPlayerByAttacker` |
| Block added, no legality check, no blocked-pair record | `Combat.addBlocker` |
| Last in damage assignment order | `addBlockerToDamageAssignmentOrder` |
| AttackerBlockedByCreature, AttackerBlockedOnce (this attacker only) | `:802-812` |
| AttackerBlocked and blocked status, only if not blocked before | `:815-826` |

Brimaz's token blocks an attacker Brimaz already blocks: AttackerBlocked does not run again. A block before blockers are
declared is refused (`DeclareCombatBlockers` would replace it; no corpus line gets there).

## `TargetsAtRandom$`

Java picks random targets in the human controller's `TargetSelection` (`:123-137`); its AI decides per API
(`DamageDealAi.java:466`, `ControlGainAi.java:96`). Here it is the rules' "chosen at random" for every controller:
`chooseTargets` draws instead of asking, for a root and for a SubAbility$ link.

| Shape | Draws |
| --- | --- |
| `TargetsAtRandom$` | TargetMin$ times `Aggregates.random` over the remaining candidates; one candidate draws nothing |
| `RandomNumTargets$` | `randomInt(max(TargetMin$,1), min(TargetMax$, candidates))` first |
| Lower bound above upper | Error (Java's `nextInt` throws) |

ExchangeControl, GainControl and Fight no longer refuse the param. BecomesTargetOnce's `Random` key
(`MagicStack.java:441`) is not recorded: the mode is not ported.

## One APNAP pass per event

Each trigger check pushed its own APNAP pass, so a non-active player's trigger of an earlier mode sat under the active
player's trigger of a later mode. Java adds every trigger to `simultaneousStackEntryList` and orders the whole list per
player (`MagicStack.java:855-880`). `beginTriggerBatch`/`endTriggerBatch` collect what `pushTriggeredAbilities` is
handed and push it in one pass when the outermost batch closes.

| Event | Modes in the batch |
| --- | --- |
| `DeclareCombatAttackers` | Taps, Exerted, Attacks, AttackersDeclaredOneTarget, AttackersDeclared |
| `DeclareCombatBlockers` | AttackerUnblocked[Once], BlockersDeclared, Blocks, AttackerBlockedByCreature, AttackerBlocked, AttackerBlockedOnce |
| `addToCombatBlocking` | its three modes |

`Static$ True` triggers still run when found. Triggers the final push raises (BecomesTarget, ward, crimes) go on as
found, Java's next round. Other events (combat damage, a zone change with ChangesZoneAll) still push per mode.

## Damage-history counts

`cardDamageThisTurn` is the card-target half of `Game.globalDamageHistory`/`damageThisTurnLKI` (`playerDamageThisTurn`
was the player half): source and target copies, amount, combat. `logCardDamage` records it once the damage is applied
(`CardDamageHistory.registerDamage`, `GameAction.java:2757`).

| Piece | Does | Java |
| --- | --- | --- |
| Target copy's excess flag | Not copied; set to the batch's outcome by `triggerExcessDamage` (`damageEntry.Record`) | `CardCopyService.java:331-333`, `CardDamageTable.java:130` |
| `Count$[Num\|Max][NonCombat\|Combat]DamageThisTurn <src> <tgt>` | Per-source sums of matching instances; Num, Max or total | `AbilityUtils.java:2434-2450`, `Game.java:1334-1350` |
| `Damage.ExcessReceived` | `logExcessDamage`'s running total; Heal keeps it, cleanup clears it | `Card.java:6193-6205` |
| `Count$ExcessDamageReceivedThisTurn` | The host's total; per card through handlePaid | `AbilityUtils.java:2406-2408` |
| `CardTriggeredTarget$` | AbilityKey.Target as a card, read from its LKI | `AbilityUtils.java:702-707` |

Sources are grouped by CardID; Java's history belongs to the card object. TotalDamageThisTurn, MaxOppDamageThisTurn,
MaxCombatDamageThisTurn and SingleMaxDamageThisTurn are Java's earlier branches and stay unresolved. Heal and
regeneration keeping `Damage.Sources` and the excess flag was R25f's; verified, not redone.

## Decisions

| Decision | Why |
| --- | --- |
| Per-event trigger batch, not a global list flushed at priority | Push timing elsewhere stays as it was; no test or scenario moved. The batch closes inside the same engine call, so ADR-0019's "no waiting queue a priority ask drains" still holds |
| New state: `Game.triggerBatch`/`triggerBatchDepth`, `Game.cardDamageThisTurn`, `Damage.ExcessReceived`, `damageEntry.Record` | Values on value structs; `Game.Clone` copies the slices, empty ones allocate nothing |
| Random targets in the engine, not a controller method | CR 115/601.2c "at random" is no player's decision |
| No new `PlayerController` method | — |
| enginelint: `changecombatantseffect` may reference `trigger`, `combat`, `blockeffect`, `phase`; `targeting` may reference `random` | `addToCombatBlocking` runs block triggers; random picks use `random.go` |

## Forge defects

| Where | Defect |
| --- | --- |
| `SpellAbilityEffect.java:797` | `getDefenderPlayerByAttacker` returns null for an attacker no longer in combat; `.equals` throws. Go skips the block |
| `overclocked_electromancer.txt:9` | "deals combat damage to a creature" with no `CombatDamage$ True` on the DamageDone trigger: fires on noncombat damage too |

## Engine findings

| Where | Finding |
| --- | --- |
| `attack.go` `defenderOf` | Returns a battle's controller, not its `ProtectingPlayer` (`Combat.java:297-299`; `cantattack.go:103` reads the protector): `DeclareCombatBlockers` asks the wrong player about an attack on a battle. Unverified by a scenario |
| `presentcheck.go` `checkSVarMatches` | An unresolvable CheckSVar reads false silently: Rith and the other `Count$...DamageThisTurn` cards "played" in the smoke probe while their condition never held |
| `changezoneeffect.go` | `Attacking$` (34 corpus lines) and `Blocking$` (1) still refused |

## Smoke probe

`crucible card-smoke -all -workers 8 -list`, before → after:

| Status | Before | After |
| --- | --- | --- |
| plays | 30,686 | 30,691 |
| refused | 2,931 | 2,925 |
| placed | 373 | 374 |
| inconclusive | 88 | 88 |
| judged play | 91.3% | 91.3% |

| Flip | Cards | Which |
| --- | --- | --- |
| refused → plays | 5 | Flash Foliage (`TokenBlocking$`); Power Struggle, Scab-Clan Giant, Witch Hunt (`TargetsAtRandom$`); Reverse Polarity (`Count$DamageThisTurn`) |
| refused → placed | 1 | Nature Shields Its Own (`TokenBlocking$`) |

No card went from plays to refused. Brimaz, Rith, Inferno Trap, Case of the Burning Masks, Wolverine, Cinderheart Giant,
Goblin Test Pilot, Goblin Polka Band and Power Pack were "plays" before and still are: their missing behavior was silent
(the probe never blocks; an unresolved CheckSVar or count read false; random targets were asked of the controller).
Module tests and scenarios are the evidence for them.

## Tests

`stackspellremoval_test.go` (Counter, RemoveFromGame and a Guile-style replacement keep the spell's cast trigger),
`tokenblocking_test.go` (Brimaz, an unblocked attacker becoming blocked, CopyPermanent, the skip shapes, refusals),
`triggerbatchorder_test.go` (blockers and attackers: the non-active player's trigger of an earlier mode on top),
`randomtargets_test.go` (draw sequence against a copy of the RNG, one candidate, random count, a link, impossible
count), `damagethisturncount_test.go` (Num/Max/total/kind/operator, Rith's per-batch excess, unresolved heads,
`ExcessDamageReceivedThisTurn` on the host and through `CardTriggeredTarget$`). Scenarios
`counter-keeps-wretched-gryff-cast-trigger`, `token-blocking-brimaz-cat-soldier-joins-the-block`,
`rith-dragon-after-excess-damage-to-opponents-creature`.
