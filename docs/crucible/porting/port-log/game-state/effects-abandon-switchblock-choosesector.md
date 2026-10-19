# Effects: Abandon lands; SwitchBlock and ChooseSector stay deferred

Batch file for three M6 `ApiType`s (DOC-12, `port-effect` step 5). This file closes `effects-batches.md`'s own entry for
all three and is never appended to again.

---

## Abandon lands

`Abandon` (21 corpus lines, all `Ongoing Scheme` cards) resolves: the ability's own host card leaves the Command zone
for the SchemeDeck (CR 904.9's own "if this scheme is abandoned"). `abandoneffect.go`, ported from
`forge-game/src/main/java/forge/game/ability/effects/AbandonEffect.java`'s `resolve`.

`AbandonEffect.java` never reads a `Defined$`/`ValidTgts$` param — it always acts on `sa.getHostCard()`. This port takes
no target either. 20 of the 21 real lines resolve: the 14 that are a bare `DB$ Abandon`, plus `Optional$`,
`RememberAbandoned$` and `SubAbility$` chains. `UnlessCost$`/`UnlessPayer$`/`UnlessSwitched$`/`ConditionCheckSVar$`/
`ConditionPresent$`/`ConditionCompare$` never reach this file at all: `resolveUnlessCost` (`effect.go`) and
`subAbilityConditionMet` (`condition.go`) both gate the whole ability before `Registry.Resolve` runs it, the identical
shape every other M6 effect already has -- including the port's own 3 real non-pure-mana `UnlessCost$` lines
(`PayLife<3>`, `Discard<1/Card>`, `Sac<2/Creature>`), which `resolveUnlessCost`'s own `IsPureMana` gate already fails
loudly on before reaching this file.

**`ConditionDefined$`** (1 real line, `i_am_duskmourn.txt`'s own
`ConditionDefined$ Remembered | ConditionPresent$ Card`) is evaluated by `subAbilityConditionMet`
([`condition-defined.md`](condition-defined.md)); `Condition$` itself stays rejected.

`Optional$` asks `controller.ConfirmEffect` (Java's `confirmAction`) first; `RememberAbandoned$` remembers the host on
itself, `changeZoneMemory`'s (`changezoneeffect.go`) identical `RememberX$` shape. Java's own
`clearActiveTriggers`/`registerActiveTrigger` pair around the move is a trigger-cache invalidation this port needs
nothing for: `checkAbandonedTriggers` (below) reads each host's own current `Zone` through `traitHosts` rather than a
cached registration, the same reasoning the earlier `Abandon` research already gave (`effects-batches.md`).

### New trigger mode: `Mode$ Abandoned` — built and tested, 0 of 2 real corpus lines reachable yet

`checkAbandonedTriggers` (`trigger.go`), the one piece the earlier research named as the actual blocker. Ported from
`TriggerAbandoned.java`'s `performTest`: `ValidCard$` matches against the abandoned card itself (`AbilityKey.Scheme` is
always the abandoned card, never a watching permanent's own state). Built the same way `checkSacrificedTriggers` is —
one walk of every player's `traitHosts`, `isAbandonedTrigger` gating on `Mode$ Abandoned` by name, then the shared
`triggerEffectAPI` gate every other mode already uses (`ConditionCheckSVar$`, `OptionalDecider$`, phases, ...). Tested
on a synthetic non-`Static$` watcher (`TestAbandonEffectFiresWatchingAbandonedTrigger`, `abandoneffect_test.go`), the
shape a watching Command-zone `Effect` card built by `DB$ Effect` (`RememberObjects$ Self`, `effecteffect.go`) would
use.

**Every real `Mode$ Abandoned` line in the corpus is `bow_to_my_command.txt`'s own two, and neither is reachable
today:**

- `SVar:TrigAbandoned:Mode$ Abandoned | ValidCard$ Card.IsRemembered | Execute$ EffectCleanup | Static$ True` carries
  `Static$ True`, which `checkAbandonedTriggers` skips rather than fires (below) — so the synthetic watcher test above
  exercises the walk itself, not this line.
- `T:Mode$ Abandoned | ValidCard$ Card.Self | Execute$ DBCleanup` sits on the scheme card's own printed triggers, which
  `traitHosts` cannot reach at all (below).

Consequence: as things stand, abandoning Bow to My Command never fires either of its own two cleanup triggers, so its
`CantAttackEffect` (the effect granting "creatures the chosen player controls can't attack you") is never removed — once
Archenemy schemes are playable at all, this is a real gap to close alongside the two reasons below, not a solved case.

**`Static$ True` runs inline**, ahead of the stack (`TriggerHandler.java:301-307`), as `pushTriggeredAbilities` does for
every match carrying `staticTrigger`. `TestAbandonEffectRunsStaticTriggerInline`.

**Not reachable yet: a scheme's own printed `Mode$ Abandoned` trigger**, on the scheme card itself rather than a
synthetic Effect wrapper. `traitHosts` (`game.go:341`) only walks the battlefield and `IsEffect` Command-zone cards, not
a plain Scheme-typed card sitting in Command — and no engine path puts one there yet anyway: Archenemy's own
`Mode$ SetInMotion`/`ApiType.SetInMotion` (`SetInMotionEffect.java`) is not ported, so a real scheme never legitimately
reaches the Command zone through play today. Once Archenemy schemes are set in motion, a scheme's own trigger will need
`traitHosts` (or a scheme-specific caller) to also walk plain Command-zone Scheme cards — tracked here rather than
worked around, since the corpus's only real users of that exact shape (`Card.Self` on the scheme itself) are, without
exception, cards this port cannot deal from a scheme deck at all yet.

### Tests (`abandoneffect_test.go`)

| Test | Proves |
| --- | --- |
| `TestAbandonEffectMovesCommandCardToSchemeDeckAndRemembers` | Bare `DB$ Abandon`, `RememberAbandoned$ True` |
| `TestAbandonEffectOptionalDeclinedLeavesSchemeInCommand` | `Optional$ True`, declined -> no-op |
| `TestAbandonEffectOptionalAcceptedAbandons` | `Optional$ True`, accepted |
| `TestAbandonEffectConditionUnmetLeavesSchemeInCommand` | `ConditionCheckSVar$`/`ConditionSVarCompare$` gates the whole ability |
| `TestAbandonEffectHostNotInCommandIsNoOp` | A host not in Command when `DB$ Abandon` resolves is a no-op, not a panic |
| `TestAbandonEffectFiresWatchingAbandonedTrigger` | `checkAbandonedTriggers`' `ValidCard$ Card.IsRemembered` match and non-match |
| `TestAbandonEffectSkipsStaticTrigger` | `Static$ True` is skipped, not pushed onto the stack |

---

## SwitchBlock

`SwitchBlockEffect.java` (both real corpus users: `general_jarkeld.txt`, `sorrows_path.txt`), checked end to end against
each combat piece it touches in `combat.go`/`block.go`/`combatdamage.go`:

- **Attacking bands** (`Combat.getBandOfAttacker`, `Combat.java:309`): General Jarkeld's own branch only switches
  blockers when its two attackers are in _different_ bands. This port has no banding keyword and no band grouping at all
  (`Combat.Attackers` is a flat `[]CardID`) -- every attacker is trivially its own band, so this check can just be
  skipped (always "different bands," never a reason to stop). Not a blocker.
- **`canBlockAdditional`** (`Card.java:7866`): Sorrow's Path's own branch compares a blocker's current gang-block count
  against `canBlockAdditional()+1`. `CanBlockAmount$`/`CanBlockAny$`, the only params that would ever raise it above 0,
  are both unresolved in this port (`pumpeffect.go:33,83`), so `canBlockAdditional()` is always 0 here -- the check
  collapses to "is this blocker already blocking more than one attacker," a plain count against `Combat.Blocks`. Not a
  blocker.
- **Damage assignment order** (`orderBlockersForDamageAssignment`/`orderAttackersForDamageAssignment`,
  `Combat.java:493,547`): Java re-asks for an explicit order immediately after switching because its own model stores
  one. This port's `AssignCombatDamage(g, decider, attacker, blockers)` (`control.go:137`) is asked fresh, from whatever
  `Combat.Blocks` says, at actual damage-resolution time (`combatdamage.go`) -- there is no stored order to invalidate
  or re-ask for. Not a blocker.

The real gaps are elsewhere, in targeting and the valid-string vocabulary both real lines actually need:

- **`Creature.blockingTargeted`** (General Jarkeld's own `DefinedBlocker$ Valid Creature.blockingTargeted`):
  `CardProperty.java:1558-1577`'s own `blocking` + suffix dispatch resolves `Targeted` through
  `AbilityUtils.getDefinedCards(source, "Targeted", sa)` -- the ability's own targeted cards. `Matches` (`valid.go:67`)
  takes no ability/targets parameter at all today; every call site passes only `source`/`sourceController`. Evaluating
  this one suffix needs `Matches` (or a targets-aware variant reachable from the `Valid ...` property path) to see the
  resolving ability's own `Targets`, a gap this file's own local reject cannot paper over.
- **`Creature.blockedByValidThisTurn`** (Sorrow's Path's own
  `DefinedAttacker$ Valid Creature.blockedByValidThisTurn Targeted`): `Card.getBlockedByThisTurn()` is a per-turn list
  Java's own `Card` tracks and clears each turn (`CardProperty.java:1618`). This port's `Card` (`card.go`) has no such
  field, and nothing resets one per turn today.
- **`TargetsWithSameController$`** (Sorrow's Path's own targeting restriction, forcing both targets to share a
  controller): the compiled param exists (`params_gen.go`, 2 corpus uses), but nothing in `targeting.go` reads it --
  every ability that names it resolves its targets exactly as if it were absent.

None of these three is a narrow param gap this file can reject its way past: the first needs `Matches` (or a
targets-aware sibling) to see the resolving ability's own targets, the second is new per-`Card` per-turn state with its
own reset hook, the third is an unread targeting restriction. Deferred on these, not on bands/`canBlockAdditional`/
damage order, none of which are real gaps here.

---

## ChooseSector

`ChooseSector`'s only real corpus user is _Space Beleren_ (`space_beleren.txt`). `ChooseSectorEffect.java` and its call
sites show "sector" is not a Battle-card defender concept or anything generic — it is Space Beleren's own printed rules
text ("Space Beleren divides the battlefield into alpha, beta, and gamma sectors...").

`ChooseSectorEffect.resolve` just asks `controller.chooseSector` and stores the answer on the host
(`Card.setChosenSector`, `Card.java:2348`), which alone would be a one-line, portable `PlayerController` addition. But
the mechanic it feeds is not:

- **State-based action 704.5u** (`GameAction.java:1801-1825`, `stateBasedAction704_5u`): every creature on the
  battlefield without a sector gets assigned one, asking `chooseSector` per creature, as part of the SBA sweep. This
  port's `CheckStateBasedActions` (`action.go`) has no such rule, and `Card` (`card.go`) has no per-`Card` sector field
  to assign in the first place.
- **`Creature.ChosenSector`** (`CardProperty.java:119-120`): compares the ability source's own chosen sector against
  `card.getSector()` — the same missing per-`Card` field, plus a new valid-string property.
- **`Creature.DifferentSector`**: Space Beleren's own `+1`
  (`SVar:SectorBlock:Mode$ CantBlockBy | ValidBlockerRelative$ Creature.DifferentSector`) needs a further, still-unbuilt
  valid-string property on the same field.

A real new mechanic scoped to one card — a per-`Card` sector field, a new state-based action, two new valid-string
properties — not a narrow param gap.

---

**Researched and deferred.**

| API | Blocker |
| --- | --- |
| `SwitchBlock` | `Matches` takes no ability/targets param (`Creature.blockingTargeted`), no per-turn blocked-by state on `Card`, no `TargetsWithSameController$` (above) |
| `ChooseSector` | Space Beleren's own single-card sector mechanic: per-`Card` sector field, a new state-based action, two new valid-string properties (above) |

## Forge bugs found

None. `AbandonEffect.java`, `SwitchBlockEffect.java` and `ChooseSectorEffect.java` all do what their own names and
params say (PORT-8's own bar) — the two deferrals are missing Crucible mechanics, not Forge defects.
