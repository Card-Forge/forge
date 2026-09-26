# Port Log — Game State: Targeting, Chaining, LKI, Events

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

Targeting, SubAbility chaining, last-known information, event wiring.

## Targeting itself lands

Every M6 effect built so far -- `Pump`, `PumpAll`, `LoseLife`, `PutCounter`, `Discard`, `Scry`, `Surveil` -- blocks
`ValidTgts$` outright in its own `Resolve`, each one's own doc comment naming it "this port's own targeting gap." CR
601.2c (a spell) and 603.3b (a triggered ability) both put "choose targets" at the moment the ability goes on the stack,
alongside the identical-timing choices this port already has real content for (`Ability.Target`, `Attach`'s own
single-Aura-target shape, `castAura`, castspell.go) -- targeting itself was always going to need a real answer before
most of those blocked lines could ever resolve, and M6's own effect-by-effect progress had reached the point where it
was the single most repeated line in every "not resolved" list. This is the M5 chunk that closes it.

`resolveTargets` (new `targeting.go`) is the CR 601.2c/603.3b moment itself, called from the one place this port has
that puts an ability on the stack at all today: `pushTriggeredAbilities` (trigger.go). `CastSpell` (castspell.go) does
not need to call it yet -- it only casts a permanent or an Aura, and neither carries `ValidTgts$` on its own top-level
record the way an Instant or Sorcery would (not built: this port has no cast path for either). It reports whether the
ability stays eligible to be pushed at all -- `bool`, not `(bool, error)` -- for a reason worth stating plainly: CR
603.3c's own real rule ("if the ability requires a target and there are no legal targets, it doesn't go on the stack")
and "a target shape this port cannot parse" are indistinguishable from the caller's own vantage point. Both mean the
ability does nothing. Giving the second case a loud error and the first a quiet `false` would draw a distinction nothing
outside `resolveTargets` itself could act on differently, and GO-7's own "fail one game, not the batch" reasoning has
nothing to grab onto for a card that was never going to resolve either way. A small blocklist (`targetUnresolvedParams`:
`Radiance$`, 4 real corpus lines -- "and each other permanent that shares a color with it," a second, derived candidate
set no single `ValidTgts$` evaluation produces on its own; `TargetsForEachPlayer$`/
`TargetsWithDefinedController$`/`TargetUnique$`, 0 real lines each) folds into that same `false` rather than being
checked by every future consumer separately.

`TargetMin$`/`TargetMax$` resolve through `resolveNamedAmount` exactly as every other numeric param already does,
defaulting to `1`/`1` when neither is named -- `TargetRestrictions.java`'s own `getOrDefault`. Candidate computation
(`targetCandidates`) has to decide first whether `ValidTgts$` names players or cards, and does it with one trial call
rather than a hand-rolled string check: `matchesPlayerSpec` (valid.go, already built for `Phase`'s/`DamageDone`'s/
`SpellCast`'s own qualified player specs) reports `ok=false` whenever its own base token is not one of `You`/
`Opponent`/`Player`, regardless of which candidate is asked, so a single call against any placeholder pair settles the
shape for the whole spec before ever walking a real candidate list. Player-shaped candidates are every player still in
the game -- a player who has lost is filtered out here the identical way `definedPlayers`'s own `if (!p.isInGame())`
reading already is, a real correctness gap this chunk caught while writing `targetCandidates` rather than one carried
over from anywhere else. Card-shaped candidates are every card on any player's battlefield, via the unchanged `Matches`
(valid.go) -- CR's own implicit "target creature" scope, and the only zone 0 real corpus `TgtZone$` lines across the
whole vocabulary ever ask this port to look anywhere else than.

`PlayerController` gained a twenty-fourth method, `ChooseTargets` (control.go), the identical "trust the controller's
answer" contract every other decision here already has -- the returned slice's own length and membership are not
re-checked against `TargetMin$`/`TargetMax$` or the candidate list. `ScriptedController` gained a `targets [][]EntityID`
queue and `QueueTargets` to fill it, following `QueueDiscardChoice`'s/`QueueScry`'s own established shape exactly.

Getting a controller into `resolveTargets` at all, called from `pushTriggeredAbilities`, meant `pushTriggeredAbilities`
itself needed one -- and it is called from sixteen places, all within trigger.go (`checkETBTriggers`,
`checkDiesTriggers`, `checkAttacksTriggers`, `checkSpellCastTriggers`, `checkBlocksTriggers`,
`checkAttackerBlockedTriggers`, `checkAttackerBlockedByCreatureTriggers`, `checkDamageDoneTriggersToCard`,
`checkDamageDoneTriggersToPlayer`, `checkDiscardedTriggers`, `checkTapsTriggers`, `checkTapsForManaTriggers`,
`checkPhaseTriggers`, `checkAttackersDeclaredTrigger`, `checkDrawnTriggers`, `checkLifeGainedTriggers`), each of which
needed the parameter added to its own signature and threaded to every one of ITS OWN external callers in turn. That
fan-out reached into `action.go` (`destroyLethalToughness`/`destroyDamagedCreatures`/`destroyZeroLoyalty`/
`destroyZeroDefense`/`cleanupDanglingAttachments`/`resolveWorldRule`, all called from `CheckStateBasedActions`),
`attack.go` (`DeclareCombatAttackers`'s own body), `block.go` (`DeclareCombatBlockers`'s own body), `combatdamage.go`
(`dealPermanentDamage`/`dealPlayerDamage`/`dealAttackTargetDamage`, called from
`dealAttackerDamage`/`DealCombatDamage`'s own chain and from `dealDamageEffect`), `manaability.go` (`TapLandForMana`),
`turn.go` (`drawStep`/`DrawCards`, called from `beginPhase` and from `drawEffect`), `land.go` (`PlayLand`), and
`castspell.go` (`permanentEffect`/`attachEffect`'s own `_ PlayerController` params, unused until now, finally read).
Every path bottomed out at a function some earlier chunk had already given a controller to -- `CheckStateBasedActions`,
`DeclareCombatAttackers`, `DeclareCombatBlockers`, `DealCombatDamage`, `beginPhase`, `CastSpell`, or `Effect.Resolve`'s
own parameter (the Discard chunk's own addition) -- so the cascade, while wide, never had to reach further than one or
two calls past a controller already in scope. `PlayLand` and `TapLandForMana` had no internal caller at all before this
(only tests and `internal/fixture`'s own `actions.go`, the `testdata/scenarios/` walker, which picked up the same two
calls), so gaining the parameter there was a clean addition rather than a threading exercise.

`definedPlayers`/`definedCards` (defined.go) both gained a `targets []EntityID` parameter and a
`"TargetedPlayer"`/`"Targeted"` (players) or `"Targeted"`/`"ThisTargetedCard"` (cards) case reading it --
`AbilityUtils.getDefinedPlayers`'s/`getDefinedCards`'s own literal cases for a `Defined$` value that explicitly names
what got targeted, the shape a sub-ability written with `Defined$ Targeted` reads back from its own parent's choice.
Every one of the eleven existing call sites across `dealdamageeffect.go`/`draweffect.go`/
`discardeffect.go`/`gainlifeeffect.go`/`loselifeeffect.go`/`pumpalleffect.go`/`pumpeffect.go`/`surveileffect.go`/
`scryeffect.go`/`putcountereffect.go` (twice, via its own `definedCounterTargets` wrapper) now passes `a.Targets`
through, whether or not that particular effect's own real corpus lines ever reach the new case yet.

`loseLifeEffect` is targeting's first real consumer, and it does not go through those new `defined.go` cases at all:
`LifeLoseEffect.java`'s own `getTargetPlayers(sa)` (`SpellAbilityEffect.java`'s own base helper) reads
`sa.getTargets().getTargetPlayers()` directly the moment the ability uses targeting at all, never falling through to
`AbilityUtils.getDefinedPlayers`'s own `Defined$` switch in that case -- and 0 real `LoseLife` lines combine
`ValidTgts$` with a `Defined$` of their own, confirming the corpus never relies on the fallback coexisting.
`loseLifeEffect.Resolve` mirrors that exactly: when `ValidTgts$` is present, read `a.Targets` straight into the player
list, bypassing `Defined$` resolution outright; only when it is absent does `Defined$` get read at all. 300 of the
corpus's 445 real `(AB|DB)$ LoseLife` lines resolve now (226 by `Defined$` alone, 74 more via
`ValidTgts$ Opponent`/`Player`) -- the other 2 real `ValidTgts$` lines name a qualified base
(`Player.wasDealtDamageThisTurnBySource`/`Player.LostLifeThisTurn`) `matchesPlayerProperty` does not recognize, so
`matchesPlayerSpec`'s own trial call reports `ok=false` for them, `targetCandidates` falls through to the card-shaped
branch, finds no card matching a `Player`-rooted spec, and the ability lands on CR 603.3c's own "no legal targets"
outcome -- the identical bucket a genuinely unresolvable shape already shares, not a wrong answer.

Building this surfaced two real regressions in already-shipped tests, both from the same root cause: five
"`RejectsValidTgts`" tests (`pumpalleffect_test.go`, `putcountereffect_test.go`, `discardeffect_test.go`,
`scryeffect_test.go`, `surveileffect_test.go`) had proved their own effect rejects `ValidTgts$` by casting a line naming
it and asserting `ResolveStack` returns an error -- but `resolveTargets` now resolves that same `ValidTgts$` line
successfully before the ability ever reaches `Resolve`, calling `ChooseTargets` on a `ScriptedController` none of the
five had queued an answer on, panicking on the exhausted-queue check every other decision here already has. The fix in
each case was not to change what the test proves (each effect's own `Resolve` still names `ValidTgts$` in its own
blocked-param list, so the assertion -- "this effect rejects it" -- is still true) but to queue a plausible target
answer first so `resolveTargets` itself succeeds and the ability actually reaches the effect's own check.
`draweffect_test.go`'s own `TestDrawEffectUnsupportedDefinedErrors` had a third, different regression: it used
`Defined$ Targeted` with no `ValidTgts$` at all to represent an unsupported `Defined$` shape, which now resolves (via
`definedPlayers`'s new case) to zero players -- a real change, not a bug, since a card writing `Defined$ Targeted` with
nothing to target is not a shape any real corpus line produces; the test now names `Defined$ TriggeredPlayer`, still
genuinely unsupported.

6 new tests (`loselifeeffect_test.go`) prove the mechanism through its first real consumer: `ValidTgts$ Opponent`
draining the chosen opponent and leaving the caster untouched, `ValidTgts$ Player` legally choosing the caster
themselves, `ValidTgts$` winning over a `Defined$` present on the identical line (0 real lines combine them, but the
dispatch itself should not silently prefer the wrong one if it ever happened), the unrecognized-property line resolving
to zero legal targets rather than a wrong one, `Radiance$` folding into that same "no legal targets" bucket, and
`TargetMax$ 2` reaching `ChooseTargets` for two opponents at once. `control_test.go`'s own
`TestScriptedControllerEachQueuePanicsWhenExhausted` table gained a `"targets"` row, and `mulligan_test.go`'s
`scriptedMulliganController` gained a panicking `ChooseTargets` stub. New `enginelint` group `targeting`
(`id`/`card`/`game`/`player`/`ability`/`control`/`valid`/`amount`/`zone`), `trigger` gaining it as a dependency;
`land`/`manaability` each gained `control` (and `land` gained nothing else new, `manaability` gained `trigger` too, both
newly needing to call into groups their own files had not referenced before this chunk).

---

## CR 608.2b: every target re-checked at resolution

`targetsStillLegal` (`targeting.go`, called by `resolveTop`; ADR-0027) is `MagicStack.hasFizzled`
(`MagicStack.java:704-752`). Each chosen target is checked on its own (`targetStillLegal`); an illegal one is removed
from the ability's `Targets`, or its Charm mode's, before the effect runs (`MagicStack.java:748-750`). The ability
fizzles — no effect, no sub-ability, no `AbilityResolved` — when at least one target was chosen and none is left, unless
it or a chosen mode names `CantFizzle$`. A fizzled spell still goes to its owner's graveyard
(`moveResolvedSpellToGraveyard`).

| Target | Illegal when                                                                                             | Java                                                         |
| ------ | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| Card   | it changed zones since targeted (its `zoneStamp` differs from the one `stampTargets` recorded; CR 400.7) | `equalsWithGameTimestamp`, `:716-722`                        |
| Card   | it is phased out (CR 702.26b)                                                                            | `Card.canBeTargetedBy`, `Card.java:6829-6831`                |
| Card   | it no longer matches the ability's `ValidTgts$`                                                          | `canTarget`'s `isValid`, `SpellAbility.java:1591-1594`       |
| Card   | Hexproof, Shroud or Protection refuses activator/source (`cardCantBeTargetedBy`, below)                  | `Card.canBeTargetedBy`, `StaticAbilityCantTarget.java:37-51` |
| Player | they left the game, or no longer match `ValidTgts$`                                                      | `Player.canBeTargetedBy`, `Player.java:1033-1043`            |
| other  | never (an ability targeted by `ChangeTargets`)                                                           | —                                                            |

`zoneStamp` (`card.go`) is Java's `gameTimestamp`: set only as a card enters a zone (`put`/`putFront`), so a transform
(which restamps `Timestamp` for layer order) does not fizzle a spell targeting the transformed permanent. `PushAbility`
records the stamps, an Aura's own `Target` included; a card already stamped keeps its stamp, so a target that changed
zones stays illegal when `ChangeTargets` rewrites a different target or a `CopySpellAbility` copy keeps the original's
targets.

Per entity, never by recomputing the candidate scan and intersecting it: `TestRemoveFromGameSpellOnStack`
(`pack3shapes_test.go`) targets a spell on the stack through a plain `ValidTgts$ Card` that `targetCandidates`'
battlefield scan would never list. The check is held to what choosing a target checks, no more: Hexproof, Shroud and
Protection are checked at both points now (below); Ward is not, and `canTarget`'s multi-target params (`TargetUnique`,
`SameController`, ...) are not resolvable in this port yet (`game-state.md`, `Not ported yet`).

Fixtures: `fizzle-helix-countered-on-resolution-gains-no-life`; `fizzle_test.go` for zone change, partial targets,
`ValidTgts$` no longer matching, and a player who lost.

## Hexproof, Shroud and Protection refuse a target

`cardCantBeTargetedBy` (`staticability.go`) is `Card.canBeTargetedBy`/`Player.canBeTargetedBy` ->
`StaticAbilityCantTarget.cantTarget` (`Card.java:6820-6838`, `StaticAbilityCantTarget.java:37-51`), narrowed to the
three keywords that generate a `Mode$ CantTarget` ability with no `AffectedZone$`: Hexproof, Shroud, Protection. Called
identically at target selection (`targetCandidates`) and at the CR 608.2b resolution re-check (`targetStillLegal`,
`auraTargetStillLegal`) — Java's own `SpellAbility.canTarget` runs `entity.canBeTargetedBy(this)` at both call sites
regardless of `fizzleCheck` (`SpellAbility.java:1608`), no asymmetry to reproduce.

| Keyword    | Refuses when                                                                                         | Activator gate | CR        |
| ---------- | ---------------------------------------------------------------------------------------------------- | -------------- | --------- |
| Protection | source (the ability's own host card) matches `protectionEach`'s spec                                 | none           | 702.16e   |
| Shroud     | always                                                                                               | none           | 702.19a   |
| Hexproof   | activator is an opponent of target's controller, and (bare, or source matches `hexproofValidSource`) | Opponent       | 702.11b/e |

`hexproofValidSource` (`staticability.go`, built for an Aura's own attach check) is reused unchanged — the same
`"Card.<color>,Emblem.<color>"` spec Java's `Protection.getProtectionValid` produces, matched here against `source`
instead of the Aura itself. `protectionEach` (`staticability.go`, built for `CantBlockBy`) is reused too, but its own
single-line bug (below) is fixed as part of this pack, so every caller of the old `protectionValid` — `CantBlockBy`,
`hostRefusesAttach`, and this function — picked up the fix at once. No `AffectedZone$` on any of the three means Java's
own default zone gate applies (`StaticAbilityCantTarget.java:70-72`, `card.isInPlay()`): `cardCantBeTargetedBy` returns
false outright for anything not on the battlefield, so a Counterspell can still target a Hexproof creature's own spell
on the stack — the spell is not in play, so its printed Hexproof does not apply to it there.

`protectionEach` replaces the old `protectionValid`, which reported only the first recognized `K:Protection` line on a
card (`strings.CutPrefix`/`strings.Cut` inside a loop that `return`s on the first match). 22 corpus cards carry two or
more — Mirran Crusader's own "Protection from black" then "Protection from green" — and CR 702.16b requires each to
apply independently. `protectionValid`'s bug reached only `CantBlockBy` and the Aura attach check before this pack;
wiring `cardCantBeTargetedBy` into ordinary targeting made it reach every targeted spell or ability too, which is why it
is fixed now rather than carried forward as a further Not-ported-yet row: `protectionEach` calls its own callback once
per recognized line and reports refused the first time the callback does, so a card needs every one of its Protection
lines to let a source through, not just the first the port happened to read.

`hostRefusesEnchant` (staticability.go) split in two, since the old single function conflated two different Java checks:
`hostRefusesAttach` is `StaticAbilityCantAttach.cantAttach` (Protection's own CantAttach half) alone, called from
`cleanupDanglingAttachments` (CR 704.5m's ongoing re-check) the way `GameEntity.cantBeAttachedMsg` calls it
(`GameEntity.java:270`) — Hexproof and Shroud generate no CantAttach ability in Java at all (`cantBeEnchantedByMsg`,
`GameEntity.java:292-304`, checks only the `Enchant` restriction), so a host gaining Hexproof or Shroud after an Aura
already attached does not make it fall off; the old function's Hexproof branch there was a bug, fixed by the split
rather than reproduced (PORT-8 does not apply — no card script depends on the old behavior, it was never exercised by a
real corpus interaction, only by this port's own prior scope limit). `enchantTargets` (cast-time, CR 601.2c) and
`auraTargetStillLegal` (the CR 608.2b re-check) call `cardCantBeTargetedBy` instead, picking up Shroud for an Aura's own
target for the first time too.

Not resolved: "Hexproof from triggered/activated abilities" (2 real corpus lines) — `hexproofValidSource`'s own
`ok=false` for `Triggered`/`Activated` (Java's `ValidSA$`, not `ValidSource$`; `Matches` only ever takes a `*Card`) — so
it never refuses; a `Ward` (`Keyword.WARD`, `CardFactoryUtil.java`'s own
`Mode$ BecomesTarget | Execute$ DB$ Counter | UnlessCost$ ...` synthesis) is not a targeting restriction at all — a
Warded permanent is targeted successfully, then its trigger may counter the triggering spell/ability afterward — and
stays unbuilt: no keyword-to-trigger synthesis exists yet for turning a bare `K:Ward:N` line into that trigger+effect
pair (Not ported yet). A Player entity's Hexproof/Shroud (`PlayerFactoryUtil.java`'s own
`Affected$ You | AddKeyword$ Hexproof`-shaped continuous grant, Leyline of Sanctity's own line) never refuses either:
`continuous.go`'s `applyOneContinuousKeyword` only ever writes a Card's `KeywordMod`, nothing grants a Player entity a
keyword yet.

Fixtures use `Lightning Bolt` (`DealDamage`), not `Pump`, to prove a target was accepted or refused — a Pump's own
`+3/+3` marks no state `fixture.Dump` carries, so a fizzled Pump and a resolved one dump identically; Bolt's damage does
not. `hexproof-refuses-opponents-bolt-on-resolution`, `hexproof-allows-controllers-own-bolt`,
`shroud-refuses-controllers-own-bolt-on-resolution`, `protection-from-red-refuses-bolt-on-resolution` cover the four
keyword/gate combinations; `counterspell-targets-hexproof-creatures-own-spell` covers the zone gate;
`pacifism-stays-attached-to-hexproof-host` covers the `hostRefusesAttach` split's own regression.

## SubAbility chaining itself lands

`SubAbility$` sits in every M6 effect's own "not resolved" list built so far -- this port's own second-most-cited gap
after targeting (above). 16,022 real corpus lines name it, 12% of the whole corpus, across 9,446 distinct files.
`AbilityFactory.getAbility`/`getSubAbility` already resolve the whole reference chain at compile time
(`compile.Ability.Subs`, `internal/carddb/compile/compile.go`, ADR-0007) -- the compiled tree has always carried the
next link, nothing at the engine layer had ever walked it.

`AbilityUtils.resolveApiAbility` is the Java shape ported: check the ability's own `metConditions()`, resolve if it
holds, then call `resolveSubAbilities` regardless of whether it did. That "regardless" is the whole feature. Sphinx
Sovereign is the real card that makes it concrete: "At the beginning of your end step, you gain 3 life if Sphinx
Sovereign is untapped. Otherwise, each opponent loses 3 life" compiles to one `DB$ LoseLife` (`ConditionDefined$ Self`
`ConditionPresent$ Card.tapped`, untested here -- game-state.md's own "Not ported yet") with a
`SubAbility$ DB$ GainLife` carrying the identical `Condition$` pair negated (`ConditionCompare$ EQ0`). Whichever half's
own condition fails, the OTHER half still has to run -- an ability that only chained when its own parent's body executed
would silently drop exactly the branch Sphinx Sovereign needs half the time.

`resolveSubAbility` (new `subability.go`) is called from `Registry.Resolve` (`effect.go`) itself, right after its own
`e.Resolve(g, a, controller)` call succeeds -- the direct Go analog of `resolveApiAbility`'s own
`sa.resolve(); resolveSubAbilities(sa, game);` pairing, both statements inside the one function rather than split across
a caller and a callee. It looks for the one `Subs` entry (`compile.Ability.Subs`) whose own `Key` matches `"SubAbility"`
case-insensitively (`mergeParams`, compile.go, already collapses a repeated key to one value, so at most one exists),
builds a child `Ability` carrying the parent's own `Source`/`Controller`/`Target`/`Targets`/`Amounts` unchanged, and
calls `r.Resolve(g, &child, controller)` -- recursing through the SAME method rather than dispatching to
`r[api].Resolve` directly, so a chain more than one hop deep just keeps going without this function needing a loop of
its own (`compile.Ability.Subs` already holds the whole tree). 10,466 real references are exactly one hop, 3,856 exactly
two hops past that, 1,172 three hops past that, and it keeps going all the way to 13 hops deep once.

Only the literal `SubAbility$` key auto-chains this way. `compile.go`'s own `subAbilityKeys` map has a much longer list
-- `PreventionSubAbility$`, and every "additional ability" key `AbilityFactory.java` attaches
(`WinSubAbility$`/`ChooseSubAbility$`/`ResultSubAbilities$`/`Choices$`, ...) -- but those are all fetched and resolved
explicitly by their own effect's own Go code once that effect exists (`FlipCoinEffect.java`, `ChoosePlayerEffect.java`,
`RollDiceEffect.java`, ...), not through this port's generic post-resolve chain the way Java's own `sa.getSubAbility()`
is: `resolveAdditional` (additional.go) is each such effect's own call, and `"SubAbility"` is the only key this chain
follows by itself.

Propagating the parent's own `Targets`/`Target` unchanged onto the child means a sub-ability naming `Defined$ Targeted`
(`definedPlayers`/`definedCards`'s own case, "Targeting itself lands," above) reads the SAME chosen target the parent's
own `ValidTgts$` resolved -- `scavenging_ooze.txt`'s/`hellhole_rats.txt`'s/dozens more real corpus lines' own shape. A
sub-ability naming its OWN `ValidTgts$` (891 of the 16,022 real referenced lines, 5.6%) is a different, unbuilt story:
`resolveTargets` runs exactly once, on the ability actually pushed onto the stack, before any of this -- there is no
second targeting pass for a node two levels down the tree. Such a sub-ability simply inherits whatever `Targets` the
parent had (often nothing) and an effect gating on `ValidTgts$` presence finds no candidates to act on -- the identical
"an unsupported shape observably folds into no legal targets" choice `resolveTargets`'s own doc comment already
committed to for the top-level case, not a new wrong-guess category this chunk introduces.

Chaining into an API this port has not registered an `Effect` for yet still fails with `ErrUnimplemented` naming it --
`Registry.Resolve`'s own existing contract for a top-level ability, inherited for free the moment the recursion runs
back through that same method rather than a separate code path. `riverwise_augur.txt`'s own real
`DB$ Draw | Defined$ You | NumCards$ 3 | SubAbility$ DBChangeZone` proves it end to end: the three cards are already in
hand (`drawEffect`'s own body ran and returned `nil` before the chain was ever attempted) by the time
`resolveSubAbility` reaches `DB$ ChangeZone` (203 script-driven APIs away from built, and the single most-referenced
`SubAbility$` target in the whole corpus at 1,505 real lines) and the whole `ResolveStack` call fails naming it. That
partial visible state is deliberate, not a rollback bug: CR's own sequential resolution means the parts of a multi-part
ability that already happened stay happened even when a later part cannot -- "draw two cards, then [something this port
cannot do]" really did draw two cards in a real game too.

A `SubAbility$` SVar body whose own leading value `ApiType.java` has no constant for -- an `APIByName` miss -- is not
reachable against the real corpus today: `ApiType.java`'s own generated vocabulary (`ability.go`) and the
apiscan/vocabscan gates (M3) already require every real API string to resolve. `resolveSubAbility` still checks for it
and returns an error naming the unrecognized value, the identical PORT-8 "a card cannot be trusted not to be the first"
reasoning `triggerEffectAPI`'s own identical defensive check (trigger.go) already used for a trigger's `Execute$` --
`compile.Compile` itself never validates an API name against any vocabulary at all (that check happens only at resolve
time), so nothing upstream of this would have caught it either.

Getting a real second half of a chain to actually run meant unblocking `SubAbility$` in at least one effect capable of
gating on `Condition$` at all -- every effect wired to `subAbilityConditionMet` still named `"SubAbility"` in its own
unresolved-param list (`dealdamageeffect.go`, `pumpeffect.go`, `pumpalleffect.go`, `loselifeeffect.go`,
`putcountereffect.go`), meaning the mechanism above would never actually have fired for any of them without a second
change. `gainLifeEffect`/`loseLifeEffect` are the pair unblocked this chunk -- the same pair "Targeting itself lands"
(above) already extended once, kept together since they are each other's mirror image and Sphinx Sovereign's own real
shape needs exactly this pair. `SubAbility` is simply removed from both `gainLifeUnresolvedParams` and
`loseLifeUnresolvedParams`; nothing else in either file changes, since the chain itself runs one level up, inside
`Registry.Resolve`. 18 of the corpus's own 253 real SVar-defined `GainLife` lines and 144 of 382 real SVar-defined
`LoseLife` lines naming `SubAbility$` now chain to an already-built leaf ability (no further `SubAbility$` of its own)
and resolve end to end -- a chain more than one hop deep, or one whose target is not built yet, is not counted by either
figure, since each of those targets' own effect already tracks that half of the question on its own terms. `drawEffect`
never named `SubAbility$` among its OWN unresolved params in the first place (there was simply nowhere for the reference
to go before now), so it started chaining for free the moment `resolveSubAbility` existed: 170 of 747 real SVar-defined
`Draw` lines. `DealDamage`/`Pump`/`PumpAll`/`PutCounter`/`Discard`/`Scry`/`Surveil` still block `SubAbility$` outright
in their own `Resolve` this chunk -- the mechanism exists for any of them, unblocking each one is a later chunk's own
job ("SubAbility chaining reaches every effect," further below, is that job).

Two existing regression tests broke for the identical reason as targeting's own five:
`TestGainLifeEffectRejectsSubAbilityChain` and `TestLoseLifeEffectRejectsSubAbilityChain` had proved their own effect
errors on a `SubAbility$` line naming `DB$ Cleanup` (an unbuilt API) -- true before this chunk because `"SubAbility"`
itself was rejected first, no longer true now that the param is unblocked and the chain actually reaches `Cleanup`'s own
real `ErrUnimplemented`. Both were rewritten as
`TestGainLifeEffectChainsIntoSubAbility`/`TestLoseLifeEffectChainsIntoSubAbility`, repointing the chained line at an
already-built leaf (`GainLife`'s own chains into `LoseLife`, `LoseLife`'s own chains into `GainLife`,
`radiant_epicure.txt`'s real shape with a plain integer standing in for its own unresolved Converge-driven `X`) and
asserting BOTH halves' own state change now happens, rather than asserting a rejection that no longer occurs.

6 new tests (`subability_test.go`) prove the mechanism itself rather than any one effect's own dispatch: Rousing Read's
real "draw two cards, then discard a card" chain resolving both halves; a synthetic `GainLife`-into-`LoseLife` chain
(the zone-scan `ConditionPresent$`/`ConditionCompare$` family standing in for Sphinx Sovereign's own unresolved
`ConditionDefined$`) proving the chained half runs even though the parent's own condition failed; a synthetic
`LoseLife`-into-`GainLife` chain proving `Targets` propagates unchanged onto the child; a synthetic three-level
`LoseLife`-into-`GainLife`-into-`Draw` chain proving the recursion itself keeps going past one hop; riverwise_augur's
real-shaped chain into unbuilt `Token` proving `ErrUnimplemented` propagates naming it, with the already-resolved half
staying resolved; and a synthetic unrecognized-API SVar body proving the defensive `APIByName` check errors rather than
silently dropping the chain. New `enginelint` group `subability` (`id`/`game`/`ability`/`control`/`effect`), `effect`
gaining it as a dependency to call into.

---

## SubAbility chaining reaches every effect

The prior chunk's own mechanism (`resolveSubAbility`, subability.go, above) had exactly two real consumers --
`gainLifeEffect`/`loseLifeEffect` -- because unblocking `SubAbility$` needed touching each effect's own unresolved-param
list individually, and the other seven `Condition$`-and-non-`Condition$`-capable effects (`dealDamageEffect`,
`pumpEffect`, `pumpAllEffect`, `putCounterEffect`, `discardEffect`, `scryEffect`, `surveilEffect`) still named
`"SubAbility"` there, rejecting the param outright before `Registry.Resolve`'s own post-resolve chain call ever got a
chance to run. `SubAbility` is removed from all seven files' own unresolved-param arrays (`dealdamageeffect.go`,
`pumpeffect.go`, `pumpalleffect.go`, `putcountereffect.go`, `discardeffect.go`, `scryeffect.go`, `surveileffect.go`) --
the identical one-line change each of the first two got, nothing else in any of the seven files changes, since the chain
itself still runs one level up inside `Registry.Resolve`.

Real corpus grounding for three of the seven: `sword_of_fire_and_ice_and_war_and_peace.txt`'s own `DB$ DealDamage`
chaining into `DB$ GainLife`; `rabaroo_troop.txt`'s own `DB$ Pump | Defined$ Self | KW$ Flying` chaining into
`DB$ GainLife | Defined$ You | LifeAmount$ 1`; `well_rested.txt`'s own
`DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 2` chaining into `DB$ GainLife` (that real card's own
`GainLife` half omits `Defined$` entirely, which would default to `You` in Java but errors in this port today --
`definedPlayers` has no such default, a separate, unrelated gap that stays open; the port's own test and doc-comment
examples name `Defined$ You` explicitly to sidestep it rather than exercise it). The other four (`PumpAll`, `Discard`,
`Scry`, `Surveil` as parents) use a synthetic chain into `GainLife` instead, since no clean real-corpus example
combining a resolvable parent shape with an already-built leaf turned up for those four specifically.

Counting "how many real lines now resolve end to end" needed the identical script shape the first two effects' own
counts used, extended to cover all seven: for each real `DB$ <Effect>` line naming `SubAbility$`, check the effect's OWN
remaining unresolved-param list (now without `"SubAbility"`) plus whatever else its own dispatch requires (`PumpAll`
needs `ValidCards$` present and, if named, a resolvable `Defined$`; `Discard` needs `Mode$ TgtChoose` and a resolvable
`Defined$`; the rest just need a resolvable `Defined$` where their own dispatch reads one), then check the referenced
sub-ability's own leading API name against the ten built effects and that its own params pass the identical gate one
level down (a leaf only -- a further `SubAbility$` two hops deep is not counted, the identical conservative choice the
first two effects' own counts already made). 9 of 316 real SVar-defined `DealDamage` lines, 17 of 571 `Pump`, 6 of 75
`PumpAll`, 56 of 623 `PutCounter`, 11 of 254 `Discard`, 31 of 57 `Scry`, and 2 of 15 `Surveil` naming `SubAbility$` now
chain to an already-built leaf ability and resolve end to end.

9 new tests, one per effect (`dealdamageeffect_test.go`, `pumpeffect_test.go`, `pumpalleffect_test.go`,
`putcountereffect_test.go`, `discardeffect_test.go`, `scryeffect_test.go`, `surveileffect_test.go`), each rewritten from
its own `Test<Effect>EffectRejectsSubAbilityChain` (which had proved rejection via a `DB$ Cleanup` target, the identical
now-false assumption `TestGainLifeEffectRejectsSubAbilityChain`/`TestLoseLifeEffectRejectsSubAbilityChain` already had)
into `Test<Effect>EffectChainsIntoSubAbility`, asserting both the parent's own body and the chained `GainLife`'s own
life change actually happen. `PutCounter`'s own rewritten test surfaced the `Defined$`-default gap directly: its old
test's chained target named no `Defined$` at all and, once `SubAbility$` stopped blocking it, `definedCounterTargets`
reached that absent value and errored -- naming `Defined$ Self` explicitly on the new test's own `PutCounter` line
(which the old, always-rejected test never needed to get right) fixed it without touching the separate
default-resolution gap itself.

Every one of the ten script-driven effects built so far now chains a `SubAbility$` it names, closing this port's own
second-most-cited gap (after targeting) completely at the mechanism level -- what remains is 193 more script-driven
effects each becoming a leaf (or a parent) other chains can reach, M6's own ordinary remaining scope, not a further
SubAbility-specific gap.

---

## Last-known-information lands

CR 603.6d's "look back in time": an object that leaves a zone is checked, for the purposes of anything watching it
leave, using its characteristics as they were immediately before it left, not as a new, reset object in its destination
zone. Java gets this from `CardCopyService.getLKICopy()` (`Card.java`'s own 8,105 LOC neighbor), called from
`GameAction.changeZone` before the card's own fields are cleared for its new zone -- a several-dozen-field copy covering
everything from P/T to exile history to cast-from information. This port needed exactly one slice of it: 116 of the
corpus's own 7,574 real `Mode$ ChangesZone` lines whose `Destination$` permits Graveyard name a `ValidCard$` testing the
dying card's own power, toughness, type, color, a keyword or a counter (Retched Wretch's own real "when CARDNAME dies,
if it had a -1/-1 counter on it, you gain 2 life" -- `Card.Self+counters_GE1_M1M1`; Reyhan, Last of the Abzan's own
"whenever a creature you control with a +1/+1 counter on it dies" -- `Creature.YouCtrl+counters_GE1_P1P1`).

`checkDiesTriggers`/`otherDiesTriggerMatches` (trigger.go,
[`## Trigger firing`](triggers.md#trigger-firing-entering-dying-attacking-blocking-dealing-damage-being-discarded-becoming-tapped-tapping-for-mana-casting-a-spell-the-beginning-of-a-step-or-phase-a-player-attacking-drawing-a-card-and-watching-another-permanent))
already carried a doc comment claiming no lookback was needed at all: `Card.Def` is fixed at compile time regardless of
zone, and `Card.Controller()` is not one of the fields `Move`'s own battlefield-leaving branch clears, so both keep
reading correctly after the card has already moved. That reasoning held for those two fields and stopped there -- it did
not extend to `Counters`/`PT`/`TypeMod`/`ColorMod`/`KeywordMod`, every one of which `Move` clears immediately, before
either trigger-check function ever runs (`destroyDamagedCreatures`/`destroyLethalToughness`/... in action.go call `Move`
then `checkDiesTriggers` back to back). A `ValidCard$` reading any of those five would silently under-fire: a creature
that died carrying a `+1/+1` counter would test as counterless by the time its own or a watcher's dies trigger ran, the
same "a card cannot be trusted not to be the first" corpus-frequency finding (116 real lines, not zero) that turned this
from a hypothetical into a real, if narrow, bug.

`Game.lki` (`game.go`) is the fix: a new `map[CardID]*Card`, written by exactly one call site -- the first line of
`Move`'s own battlefield-leaving branch, a plain `snap := *c; g.lki[id] = &snap` taken before any of the five fields
above are cleared. `Game.LKI(id)` reads it back, returning `nil` for a card that has never left the battlefield.
`checkDiesTriggers`/`otherDiesTriggerMatches` both now prefer `g.LKI(left)` over `g.Card(left)` for every read the
matching pass makes against the dying card -- `Def`/`Controller()` included, since using the frozen copy for those too
is free (both are identical either way) and simpler than special-casing which fields need the swap. `Game.Clone` (M7's
own AI lookahead) gained the identical per-field independent-copy treatment `PT`/`TypeMod`/`ColorMod`/
`KeywordMod`/`ControlMod`/`Counters`/`Memory`/`attachments` already get for a live arena card, applied to each stored
snapshot instead -- a clone's own LKI copy has to be as independent of the original's as everything else Clone already
guarantees ([`## Cloning`](../game-state.md#cloning)).

The snapshot is a plain struct copy, not Java's own field-by-field reconstruction: nothing else this port's own
`Matches`/`compareFieldValue` (valid.go) reads is affected by leaving the battlefield the way those five ledgers are, so
there was nothing else worth capturing. It is overwritten whole on every subsequent trip off the battlefield, never
merged with an earlier one -- `getLKICopy()`'s own contract, and the one a card leaving, returning, and leaving again
with different counters needs (`TestLKIOverwrittenOnEachSubsequentLeave`, lki_test.go).

Four new tests (`lki_test.go`): `TestLKIAbsentBeforeLeavingBattlefield`, `TestLKIFreezesStateAtTheMomentOfLeaving`,
`TestLKIOverwrittenOnEachSubsequentLeave` and `TestCloneCopiesLKI`. Two more (`trigger_test.go`) prove the actual bug
this closes, each checked against the OLD behavior directly (reverting the `g.LKI` lookup made both fail exactly as
expected before being restored): `TestDestroyDamagedCreaturesDiesTriggerSeesCounterAtTimeOfDeath` (a creature's own
`Card.Self+counters_GE1_P1P1` dies trigger) and `TestDestroyDamagedCreaturesOtherDiesTriggerSeesCounterAtTimeOfDeath` (a
separate watcher's own `Creature.YouCtrl+counters_GE1_P1P1` dies trigger, `otherDiesTriggerMatches`'s own half).

Not resolved: everything else `getLKICopy()` copies (exiled-with, cast-from, damage history, remembered/imprinted cards,
...) -- each real only once some other still-unbuilt mechanism would ever read a graveyard/exile card's own past-tense
state through it, PORT-8's "build the consumer's own real need, not the whole Java method" the identical discipline
`resolveAmount`/`subAbilityConditionMet` already followed for their own Java counterparts. The legend rule's own Corner
Case 1 is item 25's other named gap and is unrelated to LKI at all -- it needs a card-name lookup across every creature
card this game has ever printed, and this port's `*Game` holds no `*carddb.DB` reference to ask
([`## The legend rule needed CheckStateBasedActions to take a controller`](state-based-actions.md#the-legend-rule-needed-checkstatebasedactions-to-take-a-controller)).

---

## Events, wired

ADR-0013's schema (`event.go`) landed with the turn structure it names but with nothing behind it: no `Game` field held
a `Sink`, and nothing called `Emit`. Every mechanism this port has built now does:

| Call site                                                                       | Kind(s)                                             |
| ------------------------------------------------------------------------------- | --------------------------------------------------- |
| `Move`                                                                          | `ZoneChanged`                                       |
| `StartTurn`, `AdvancePhase` (on wrap)                                           | `TurnBegan`                                         |
| `beginPhase` (every step)                                                       | `PhaseBegan`                                        |
| `drawStep`                                                                      | `CardDrawn`, alongside `Move`'s own `ZoneChanged`   |
| `CheckStateBasedActions` (once, on end)                                         | `GameEnded`                                         |
| `PushAbility`                                                                   | `AbilityActivated`                                  |
| `ResolveStack` (per item)                                                       | `AbilityResolved`                                   |
| `CastSpell` (on a successful cast)                                              | `SpellCast`                                         |
| `dealCombatDamageStep` (per exchange, both damage steps)                        | `DamageDealt`, plus `LifeChanged` for player damage |
| `annihilateCounters`, `dealPermanentDamage`, `Move`'s ETB loyalty/defense grant | `CounterChanged`                                    |
| `PayManaCost` (a Phyrexian or hybrid Phyrexian shard resolved to life)          | `LifeChanged`                                       |
| `Game.phase` (phasing.go, every permanent phasing in or out, ADR-0021)          | `Phased` (schema v2)                                |

`Game.sink` defaults to `DiscardSink{}`, set in `NewGame`, so no existing caller — every test, `fixture.Load` — had to
start constructing one. `SetSink` is the opt-in a recorder (M8) uses. `Game.Clone` always gives the clone a fresh
`DiscardSink` regardless of what the original holds, which is the reason `DiscardSink`'s own doc comment already gave
before anything called it: the AI's lookahead explores lines that never happened, and a clone holding the real sink
would record imagined casts as real.

`PhaseBegan` fires before that phase's own actions, not after — a recorder reading the stream in order sees "entered
Draw" before "drew a card," matching when a real player would notice the step change. `TurnBegan` fires before
`ActivePhase` changes to `Untap`, for the same reason.

`CounterChanged` fires from every counter change this port can cause today — `annihilateCounters`'s CR 704.5q pile
shrink, `dealPermanentDamage`'s loyalty/defense removal, `Move`'s ETB loyalty/defense grant — with `Amount` as the
signed delta (negative for a loss, matching `LifeChanged`'s own convention) and `Detail` as a `CounterDetail`
(`event.go`) encoding which `CounterType` changed. `CounterDetail` is closed over the eight named `CounterType`
constants (`counters.go`) — `P1P1`, `M1M1`, `Loyalty`, `Defense`, `Charge`, `Stun`, `Shield`, `Poison` — not the open
string `CounterType` itself allows, because nothing yet creates a counter from a script-written name; that needs a
`SpellAbility` to run one, M6's problem. `counterDetail` (unexported, `event.go`) returns `ok == false` for anything
outside that set, and the caller drops the event rather than emit one with a lying `Detail` — unreachable today, since
every existing caller passes a named constant, but the seam exists for when M6's script-driven counters make it
reachable. Extending the switch, or replacing it with a per-`Game` interning table (ADR-0009's arena pattern, the same
shape as `CardID`) if the corpus turns out to need more than a closed set, is whichever shape M6 needs when a real
caller forces the choice — not a decision worth making before one exists.

Not wired, and it is a real gap rather than an oversight: anything from `PerformMulligans` itself — a mulligan is fully
visible as the `ZoneChanged` cascade `Move` already produces, and no `MulliganTaken`-shaped kind exists in the schema to
add without also bumping `SchemaVersion`, a more deliberate act than this pass earned. `SpellCast` fires for real now
too
([`## Casting a spell needed the stack for real, for the first time`](mana-and-casting.md#casting-a-spell-needed-the-stack-for-real-for-the-first-time))
— `CastSpell` is its first caller, once `PushAbility`/`ResolveStack` had one worth pointing it at.
`AbilityActivated`/`AbilityResolved` are wired the same way ([`## Stack`](turn-stack-combat.md#stack)) — the same
"mechanism now, content later" the effect registry already established, now with `CastSpell`/`ResolveStack` as the first
real (non-test) callers of either. `DamageDealt`/`LifeChanged` are wired for real content too
([`## Combat`](turn-stack-combat.md#combat-declaring-attackers-declaring-blockers-and-dealing-damage)) — every combat
exchange fires `DamageDealt`, and player damage also fires `LifeChanged` with `Amount` as the signed change (negative
for the ordinary case, a loss), a sign convention this port chose freely since nothing wired either kind before combat
damage did. `LifeChanged` fires a second way now too, from `PayManaCost`
([`## Mana pool and payment`](mana-and-casting.md#mana-pool-and-payment)) when a Phyrexian or hybrid Phyrexian shard
resolves to life instead of mana — `Source: NoCard`, since paying a cost has no card of its own to name the way combat
damage's attacker does.
