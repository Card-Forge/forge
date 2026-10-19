# Blockers-declared triggers, the attacked-target record, triggering objects while targets are chosen (R9g)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `blockerstriggers.go`
  (`checkBlockersDeclaredTriggers`, `checkAttackerBlockedOnceTriggers`), `block.go` (call order), `trigger.go`
  (`attackersDeclaredObjects`), `ability.go` (`triggeredObjects.blockers`, `attackedTargets`), `defined.go`
  (`TriggeredBlockers`, `TriggeredAttackedTarget`), `valid.go` (`TriggeredAttackers`, `NotDefinedTriggeredAttackers`,
  `attacking TriggeredAttackedTarget`, the `Chosen` operand), `targeting.go` (`inAbilityContext`), `dividedtargets.go`
- **Java:** `TriggerBlockersDeclared.java:50-58`, `TriggerAttackerBlockedOnce.java:19-31`,
  `PhaseHandler.declareBlockersTurnBasedAction` (`PhaseHandler.java:746-820`),
  `TriggerAttackersDeclared.setTriggeringObjects` (`:79-98`), `CardProperty.java:1465-1469` (`Chosen`), `:1543-1549`
  (`attacking <Defined>`), `:2045-2068` (`Triggered<Key>`, `NotDefined<Key>`), `AbilityUtils.getDefinedCards`
  (`:167-199`), `getDefinedPlayers` (`:1013-1075`), `getDefinedEntities` (`:755-762`)

## What was wrong

Four findings of R9c ([`m6-r9c-commander-attackers.md`](m6-r9c-commander-attackers.md)), each a card the probe counted
as playing for the wrong reason.

| Finding | Effect before |
| --- | --- |
| No check for `Mode$ BlockersDeclared` or `AttackerBlockedOnce` | The triggers never fired: Tide of War, Big Spender, Hezrou, Neyith of the Dire Hunt did nothing |
| `TriggeredAttackers`, `NotDefinedTriggeredAttackers` not in `Matches` | An unknown property never matches: Squall's `ConditionCheckSVar$` count read 0, and its trigger never fired at all (`powerEQChosen`'s `Chosen` operand was unresolved too) |
| `AbilityKey.AttackedTarget` not recorded | Every `Defined$`/`TargetsWithDefinedController$`/`attacking TriggeredAttackedTarget` reader had nothing to read |
| Amounts read while targets are chosen had no ability | `Game.resolving` was set only by `Registry.resolve`, so Case of the Market Melee's `TargetMax$ TriggerObjectsAttackers$Amount` was unresolved and its trigger silently stayed off the stack |

## BlockersDeclared and AttackerBlockedOnce

| Mode | Fires | `performTest` | Records |
| --- | --- | --- | --- |
| `BlockersDeclared` | once, when any blocker was declared (`PhaseHandler.java:748-762`) | always true (`TriggerBlockersDeclared.java:50`) | `Blockers` (each declared blocker once), `Attackers` (each blocked attacker once, blocker order) |
| `AttackerBlockedOnce` | once, after every `AttackerBlocked`, when any attacker is blocked (`:816-820`) | `ValidCard$` matches any blocked attacker; absent passes | `Attackers`: the blocked attackers in `combat.getAttackers()` order, not block order |

| Decision | Why |
| --- | --- |
| New file `blockerstriggers.go` in the `trigger` group | `trigger.go` is shared by every porter; one file per finding keeps the merge small. Same group, so no allow-list change |
| Zone walk of the `AttackersDeclared` scan | `phaseTriggerZones` with `TriggerZones$` honoured (`onceTriggerMatches`): all 4 real lines name `TriggerZones$ Battlefield` |
| `BlockersDeclared` before the first `Blocks` trigger, `AttackerBlockedOnce` after the last `AttackerBlocked` | Java's relative order (`PhaseHandler.java:748-820`). The unblocked-attacker triggers still run last in `DeclareCombatBlockers`, where Java runs them first (`Combat.fireTriggersForUnblockedAttackers`, `:746`); not changed by this row |
| `blockers` nil means unset | The mode fires only for a non-empty list, so an empty recorded list never exists. `Defined$ TriggeredBlockers` with nothing recorded is the loud "the trigger recorded no blockers" (GO-7) |
| Blocker order is the declaration order | Java's `Combat.getAllBlockers` walks the `blockedBands` multimap; in a two-player game that is the declaration order too |

## AttackedTarget

`attackersDeclaredObjects` records it as `TriggerAttackersDeclared.java:80-96` builds it: the defenders the caller
passed (every attacked defender, or the one for `AttackersDeclaredOneTarget`); under `ValidAttackers$`, the defenders of
the kept attackers instead, once each in attacker order; under `AttackedTarget$`, only the entities that spec names,
each tried on its own. Players and cards share one `[]EntityID` with an `attackedTargetsRecorded` flag.

| Reader | Answer |
| --- | --- |
| `definedPlayers` `TriggeredAttackedTarget` | the players in the list (`TargetsWithDefinedController$`, `Defined$` on LoseLife, DealDamage, Draw, ...); a recorded list with no player is no players |
| `definedCards` `TriggeredAttackedTarget` | the planeswalkers and battles in the list; `definedEntities` and `dealDamageTargets` join both halves |
| valid `attacking TriggeredAttackedTarget` | the card's defender, player or card, is in the list (`CardProperty.java:1543-1549`) |
| unrecorded (any other mode) | an error naming the key; the valid property leaves a pending error |

Java's `AttackedTarget$` filter is a lazy `Iterable` (`IterableUtil.filter`), re-run on every read; here it runs once as
the trigger fires. The two differ only when the spec's answer changes before the ability resolves (an Aura moved under
`Player.EnchantedBy`).

## Valid properties read off the ability in context

`TriggeredAttackers` (membership in `AbilityKey.Attackers`) and `NotDefinedTriggeredAttackers` (not a member) read the
ability in context (`Game.resolving`) when it is the source's own, as `targetsPlayer` already did. With no such ability
Java's `spellAbility` is no `SpellAbility`: `Triggered...` is false and `NotDefined...` true, which a static's
`Affected$` sees. Inside its own ability, a mode that recorded no attackers leaves a pending error.

| Decision | Why |
| --- | --- |
| Exact names, no generic `Triggered<Key>`/`NotDefined<Key>` | A generic branch erroring on every other key would turn cards that play today into refusals on keys this row did not audit (`!TriggeredAttacker` 5, `NotDefinedTargeted` 14, ...) |
| Fields read directly, no call into `defined.go` | `defined` depends on `valid`; the reverse is a cycle enginelint forbids |
| `Chosen` operand (`powerEQChosen`) | Squall's `ValidAttackers$` needs it: `CardProperty.java:1465-1469` compares with the source's chosen number and fails the property when none was chosen. 10 corpus occurrences (`cmc`, `power`, `toughness`) |

## Triggering objects while targets are chosen

Java hands the `SpellAbility` being put on the stack to every `calculateAmount` and `isValid` its targeting reads
(`getMinTargets`/`getMaxTargets`, `dividedValue`, `canTarget`), so its triggering objects are readable then.
`inAbilityContext` makes the ability being targeted `Game.resolving` for the duration of `targetChoiceFor`,
`trimTargetSet`, `dividedTotal` and `targetStillLegal` (the CR 608.2b re-check), and restores the previous one.

| Decision | Why |
| --- | --- |
| Reuse `Game.resolving`, no second field | Every context reader (`amountcontext.go`, `amountplayers.go`, `presentcheck.go`, `valid.go`) already keys on it and on `a.Source == source`; a parallel field would need each one taught twice |
| Set inside the four functions, not at call sites | Covers every caller at once: trigger push, cast (`castspell.go`), Charm modes, copies, ChangeTargets, `SubAbility$` links. A link is its own context, as Java's `sa` is the link |
| The fizzle re-check's link carries the root's triggering objects | `dropIllegalChainTargets` built its link without them; Java's link reads its root's (`getRootAbility`). Ordruun Mentor's `Creature.attacking TriggeredAttackedTarget` would otherwise fizzle at resolution |
| Nested target choice reads its own ability | A trigger pushed mid-resolution used to see the resolving ability as its context; now it sees itself |

No new `Game`/`Player` state, no `PlayerController` method: `blockers`, `attackedTargets` and its flag are
`triggeredObjects` fields, copied by value with the `Ability` and never edited in place, so `Game.Clone` shares them.

## Forge bug found (PORT-8), not compensated

Jin Sakai, Ghost of Tsushima (`forge-gui/res/cardsfolder/j/jin_sakai_ghost_of_tsushima.txt:7`) gates its modes with
`ConditionCheckSVar$ X | ConditionSVarCompare$ EQ0` on the `DB$ Charm` itself. `CharmEffect.chainAbilities`
(`CharmEffect.java:293-317`) appends the chosen modes as sub-abilities and `CharmEffect.resolve` is empty (`:197-200`).
The trigger's `WrappedAbility.resolve` hands the inner Charm to `playSpellAbilityNoStack` (`WrappedAbility.java:440`),
which calls `AbilityUtils.resolve` on it (`ComputerUtil.java:270`, `AbilityUtils.java:1333`); the Charm is no wrapper,
so `resolveApiAbility` (`AbilityUtils.java:1395-1403`) skips only its own `resolve()` when the condition fails and then
resolves the sub-abilities anyway. The "if no other creatures are attacking that player" check never gates anything in
Forge. This port's Charm does not read `Condition*$` either, so it agrees with Java; the fix belongs upstream (move the
condition onto each mode, or onto the trigger as `CheckSVar$`). In a two-player game `X` is 0 whenever the trigger
fires, so no game this port plays is affected.

## Smoke probe

`crucible card-smoke -all -workers 8 -list`, before → after:

| Status | Before | After |
| --- | --- | --- |
| plays | 30,618 | 30,624 |
| refused | 2,895 | 2,889 |
| placed | 372 | 372 |
| inconclusive | 193 | 193 |
| judged | 33,513 | 33,513 |
| judged play | 91.4% | 91.4% |

| Change | Cards |
| --- | --- |
| refused → plays | 7, every one on the `AttackedTarget` record: Firkraag, Gornog, Karazikar, Seifer (`TargetsWithDefinedController$ TriggeredAttackedTarget`), Archnemesis (`LoseLife Defined$`), Zurzoth (`Draw Defined$ TriggeredAttackedTarget & You`), Commander Liara Portyr (`TriggeredPlayersAttackedTarget$Amount`) |
| plays → refused | 1, honest: Echoing Assault's `ValidTgts$ Creature.!token+attacking TriggeredAttackedTarget` used to have no candidate, so its trigger silently stayed off the stack; it now targets and reaches CopyPermanent's refused `AtEOT$` |
| new refusal cause | Raging River: past `RepeatPlayers$ TriggeredAttackedTarget`, now refused on `Effect: ImprintOnHost$` |
| still refused | Combat Calligrapher, Soaring Lightbringer, Ellie: `Token: TokenAttacking$ TriggeredAttackedTarget`, the next reader of the record |

Tide of War, Big Spender, Hezrou, Neyith, Case of the Market Melee, Squall, Jin Sakai and Ordruun Mentor played before
and play now; before, their triggers never fired, stayed off the stack or read nothing (the probe counts silent misses
as plays).

## Tests

`blockdeclarationtriggers_test.go`: `BlockersDeclared` records every blocker and blocked attacker and fires once, not
with no block, Tide of War's real script sacrifices opposite sides on a won and a lost flip; Big Spender makes one
Treasure for two blocked attackers, none when it defends or nothing is blocked; `AttackerBlockedOnce` records only the
blocked attackers; an unrecorded `TriggeredBlockers` errors. `attackedtargetobjects_test.go`: the attacked player, the
`ValidAttackers$` rewrite to the planeswalker's defender (card half, player half empty), the `AttackedTarget$` filter,
both unrecorded readers, the three valid properties in context, outside it (a static's `Affected$`), unrecorded inside
it, and Squall's real script with a matching and a non-matching chosen number. `targetingcontext_test.go`: Case of the
Market Melee's `TargetMax$`/`DividedAsYouChoose$ TriggerObjectsAttackers$Amount` with one and two targets. Scenarios:
`trigger-hezrou-shrinks-each-blocker-once`, `trigger-big-spender-one-treasure-for-two-blocked-attackers`,
`trigger-ordruun-mentor-first-strike-for-an-attacker-of-that-player`.
