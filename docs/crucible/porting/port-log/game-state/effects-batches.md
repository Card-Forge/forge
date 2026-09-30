# Port Log — Game State: M6 Effect Batches

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

Multi-effect batches from Destroy/Tap/Untap onward. New effects add a section here.

## Destroy, Tap and Untap land, M6's thirteenth through fifteenth script-driven effects

`destroyEffect`/`tapEffect`/`untapEffect` (`destroyeffect.go`/`tapeffect.go`/`untapeffect.go`) are the first three
effects in this port to read `Ability.Targets` directly for their own dominant real shape rather than only through
`Defined$`'s own `"Targeted"` case: `SpellAbilityEffect.getTargetCards(sa)`'s own either/or contract
(`AbilityUtils.getDefinedCards`, `forge-game`'s own `SpellAbilityEffect.java`), ported as `targetedOrDefinedCards`
(`defined.go`) — an ability naming `ValidTgts$` uses its own chosen target (already populated by `resolveTargets`
against these same `Params` before `Resolve` is ever called, `targeting.go`), `Defined$` is not consulted at all even
when also present on the same line; no `ValidTgts$` at all falls back to `Defined$`, defaulting to `"Self"` the same way
Java's own `getParamOrDefault(definedParam, "Self")` does. `Ability.Targets`'s own doc comment (`ability.go`) updated to
match — it used to claim no effect read the field directly, which stopped being true the moment these three landed.

`destroyEffect` (`DestroyEffect.java`) is CR 701.7: 986 of the corpus's real `(AB|DB)$ Destroy` lines excluding
`DestroyAll` (a separate `ApiType`/effect class this port does not build), 782 naming `ValidTgts$` and 205 naming
`Defined$` instead — nearly the whole non-`DestroyAll` population resolves through one or the other. `canBeDestroyed`
ports `Card.canBeDestroyed`'s own formula minus the `isInPlay`/`isPhasedOut` halves the caller already checks (phasing
does not exist in this port): Indestructible blocks destruction unless the card is a creature at 0 or less toughness, CR
704.5f's own carve-out, `destroyDamagedCreatures`'s own identical reasoning (`action.go`) applied here to an explicit
`Destroy` rather than lethal damage. `NoRegen$` (62 real lines) needs no handling at all: this port builds no
`ReplacementType.Destroy` shield (`replacement.go` has none), so running `GameAction.destroy`'s own check would always
answer `NotReplaced` regardless of the param — a real, narrow simplification, not a gap. `TriggerType.Destroyed` (a rare
"whenever a permanent is destroyed" mode, distinct from `Mode$ Dies`, which `checkDiesTriggers` already fires for every
real battlefield departure below) has no entry in this port's own trigger table yet, the identical "an unbuilt `Mode$`
simply never fires" reasoning every other still-missing mode already has, below. `RememberDestroyed$` writes the
destroyed card onto the host's own `Memory` (`sacrificeEffect`'s own `RememberSacrificed$` precedent,
`sacrificeeffect.go`); `Condition$`/`ConditionDefined$`/`PlayerTurn$`/`SorcerySpeed$`/`Ultimate$`/`ModeCost$`/
`RememberLKI$` fail loudly (PORT-8/GO-7) rather than silently no-opping or destroying past what the line actually asked
for.

`tapEffect` (`TapEffect.java`) is CR 701.21: the corpus's own dominant real `(AB|DB)$ Tap` shape (818 of 1,395 real
lines) names `ETB$` — CR 614's own "enters the battlefield tapped" replacement effect, already resolved end to end by
`replacement.go`'s own `tapAbilityResolvesTap`/`replacementTapsOnMove` (a `ReplaceWith$` target's own sub-ability runs
by hand, never reaching `Registry.Resolve` at all, `drawReplaced`'s own identical reasoning) — set aside, `ETB$` listed
in `tapEffect`'s own unresolved-params anyway, a defensive reject in case a real line somehow combines it with a shape
that reaches this path some other way. Of the remaining 577, 413 name `ValidTgts$` and 151 name `Defined$`.
`RememberTapped$`/`AlwaysRemember$` write the tapped card onto the host's `Memory`, matching Java's own
`gameCard.isUntapped() && remTapped || alwaysRem` condition exactly (remembered even when the target was already tapped,
if `AlwaysRemember$` says so); tapping itself only runs, and only fires `checkTapsTriggers`, when the target was
untapped beforehand — `Card.tap()`'s own early `if (tapped) return false`. `TriggerType.TapAll` is not fired, the
identical "2 real corpus `T:` lines, not worth building" reasoning `taptype.go`'s own cost-side tap already gives for
the same trigger. `CardChoices$`/`ChoiceAmount$`/`ChoicePrompt$`/`AnyNumber$`/`Tapper$`/`Condition$`/`ConditionDefined$`
fail loudly.

`untapEffect` (`UntapEffect.java`) is CR 701.22, Tap's own mirror image at the opposite end of `Card.Tapped`: 431 real
`(AB|DB)$ Untap` lines once `UntapUpTo$`/`UntapExactly$` (18 combined, `untapChoose`'s own interactive "choose up to N"
shape — no `ChoosePermanentsToUntap` method exists on `PlayerController` yet, the identical gap `CardChoices$` has for
Tap) are set aside — `Defined$` is the dominant real shape here (228 of 431) rather than `ValidTgts$`'s own dominance on
Destroy/Tap (163). Untapping fires `checkUntapsTriggers` (`trigger.go`, `Mode$ Untaps`, already built for the automatic
untap step but never before called from a script-driven effect) only when the target was tapped beforehand. `ETB$` (6
real lines, an "enters the battlefield untapped" override) has no replacement-side mirror of `Tap`'s own
`tapAbilityResolvesTap` built, so it fails loudly here rather than risk untapping-without-a-trigger the wrong permanent.
`UntapType$`/`Amount$`/`Condition$`/`ConditionDefined$` fail loudly too.

`NewRegistry` (`castspell.go`) registers all three; `enginelint.json` gained three new one-file groups plus `"ability"`
added to `defined`'s own allow-list (`compile.Ability`'s own identically-spelled type, referenced for the first time
from `defined.go` by `targetedOrDefinedCards`'s own signature).

---

## Fight and Mill land, M6's sixteenth and seventeenth script-driven effects

`fightEffect` (`fighteffect.go`, CR 701.12) is 146 real `(AB|DB)$ Fight` lines, every one naming `Defined$`, 138 also
naming `ValidTgts$` (`fight_with_fire.txt`'s own dominant real shape, `Defined$ Self | ValidTgts$ Creature` -- CARDNAME
fights a chosen target) and 8 naming `Defined$` alone. `fightFighters` ports `getFighters`'s own real combining rule: at
most one card read directly off `ValidTgts$`'s own chosen target (not through `targetedOrDefinedCards`, since Fight's
own combining logic needs to know separately whether a target supplied a card, unlike every other targeted effect this
port has), and `Defined$`'s own resolved cards -- a lone `Defined$` card pairs with an already-found target (`fighter2`
= the target, `fighter1` = the `Defined$` card); two or more `Defined$` cards with no target found yet become both
fighters outright. CR 701.12b's own "no longer on the battlefield or no longer a creature" legality check is applied to
`Defined$`'s own resolved cards only, Java's own identical asymmetry -- a freshly-chosen `ValidTgts$` target was already
legality-filtered by `resolveTargets`'s own candidate walk moments before this runs, and nothing can happen to it in
between (`attachEffect`'s own CR 608.2b doc comment, `castspell.go`). Damage itself reuses `dealPermanentDamage`
(`combatdamage.go`) twice, once per direction, sharing one `damageTable` so `checkDamageTableTriggers` (`trigger.go`)
fires once for the whole exchange -- `dealDamageEffect`'s own identical batching reasoning (`dealdamageeffect.go`). CR
701.12c's own "fights itself" case (`fighter1 == fighter2`) deals twice the fighter's own power to itself instead. Not
fired: `TriggerType.Fight`/`FightOnce` -- no entry in this port's own trigger table for either mode yet, the identical
"an unbuilt `Mode$` simply never fires" reasoning every other still-missing mode already has, below.
`ReplaceDyingDefined$`/`ReplaceDyingExiledWith$`/`Optional$`/
`ExcessSVarCondition$`/`ExcessSVar$`/`TargetsAtRandom$`/`SorcerySpeed$`/`Condition$`/`ConditionDefined$` fail loudly.

A regression worth naming: the first version of both new multi-fighter tests died mid-debugging, not from a bug in
`fightFighters` or `dealPermanentDamage` at all -- `ResolveStack`'s own `CheckStateBasedActions` pass (`stack.go`) runs
after every ability resolves, and a fight that leaves a fighter lethally damaged gets it destroyed and moved to the
graveyard before the test's own assertion ever runs, `Move`'s own CR 400.7 characteristic reset zeroing `Damage.Marked`
right along with it. Both fighters in every fight-and-survive test now carry toughness 10, comfortably past any single
test's own damage total, so the assertion reads the fight's own effect rather than a subsequent, unrelated zone
change's.

`millEffect` (`milleffect.go`, CR 701.13) is 507 real `(AB|DB)$ Mill` lines, 317 naming `Defined$` and 130 naming
`ValidTgts$` instead -- `SpellAbilityEffect.getTargetPlayers(sa)`'s own either/or contract, ported as
`targetedOrDefinedPlayers` (`defined.go`), the player-shaped twin of `targetedOrDefinedCards` Destroy/Tap/Untap already
use
([`## Destroy, Tap and Untap land`](#destroy-tap-and-untap-land-m6s-thirteenth-through-fifteenth-script-driven-effects),
above), defaulting to `"You"` rather than `"Self"` when neither `ValidTgts$` nor `Defined$` is present -- a player
list's own default, Java's own `getParamOrDefault(definedParam, "You")`. 0 real Mill lines name `Destination$` at all:
CR 701.13a's own default (the graveyard) is the whole real corpus, so `Destination$` fails loudly anyway rather than
silently assuming Graveyard for a value this port has never seen. The actual move reuses `scryEffect`'s own defensive
top-N copy (`topN := append([]CardID(nil), lib[:n]...)`, `scryeffect.go`) before mutating the zone out from under the
slice being iterated, `GameAction.mill`'s own `Iterables.limit(milledView, n)` clamped to library size the identical
way. `RememberMilled$` (84 real lines) and `Imprint$` (6) write every milled card onto the host's own `Memory`;
`ForgetOtherRemembered$` clears it first. Not fired: `TriggerType.Milled`/ `MilledOnce`/`MilledAll` -- no entry in this
port's own trigger table for any of the three modes yet.
`Optional$`/`Ultimate$`/`SorcerySpeed$`/`ReduceCost$`/`ModeCost$`/`Condition$`/`ConditionDefined$` fail loudly;
`ShowMilledCards$` is a reveal-dialog flag this port has no UI to show or hide, changing nothing about the actual zone
change, so it is simply ignored rather than rejected.

`NewRegistry` (`castspell.go`) registers both; `enginelint.json` gained two new one-file groups, `fighteffect` allowing
`combatdamage`/`trigger` (`damageTable`'s own declaring file) and `milleffect` allowing `amount` (`resolveNamedAmount`'s
own declaring file) for the first time from an M6 effect not already reading it.

---

## Five more land: RemoveCounter, DamageAll, SetLife, Shuffle and ExchangeLife

`removeCounterEffect` (`removecountereffect.go`, CR 121.4) is PutCounter's own mirror image -- 199 real
`(AB|DB)$ RemoveCounter` lines, all naming `Defined$`, reusing `definedCounterTargets` (`putcountereffect.go`) outright
rather than a second copy. `CounterType$` is a single literal name (171 of 201) or the literal `"All"` (15, every kind
the target carries, `CounterNum$` ignored entirely when it is -- `Counters.Kinds()`, `counters.go`, enumerates them);
`CounterNum$` is a resolvable amount (134) or the literal `"All"` (55, the target's own CURRENT count of that one kind,
computed per target inside the loop since a fixed amount cannot answer "how many are there" up front). `Counters.Add`'s
own clamp-at-zero contract (counters.go's own doc comment, "remove two counters from a card with one removes one") meant
`removeCounters` needed no clamping of its own past that. Two params looked cheap and were not:
`RememberRemoved$`/`RememberAmount$` (28/13 real lines) -- `CountersRemoveEffect.java`'s own
`source.addRemembered(Pair.of(counterType, i))`/`addRemembered(totalRemoved)` remember a (type, index) pair and a raw
integer respectively, neither an `EntityID` `Memory.Remember` (memory.go) has anywhere to put; both fail loudly instead
of being silently mapped onto the wrong thing. `ValidTgts$` (36) stays deferred, putCounterEffect's own identical scope;
`Choices$`/`UpTo$`/`CounterType$ Any` each need a `PlayerController` hook this port does not have.

`damageAllEffect` (`damagealleffect.go`, CR 119) is DealDamage's own "to every matching creature and/or player at once"
sibling -- 307 real `(AB|DB)$ DamageAll` lines, 254 naming `ValidCards$` and 126 naming `ValidPlayers$` (91 both). 288
of 307 name no `DamageSource$` at all, `AbilityUtils.getDefinedCards`'s own `def == null ? "Self" : ...` default
confirmed by reading it directly (`definedCards`'s own identical `"Self"` case, defined.go). Reuses
`dealPermanentDamage`/`dealPlayerDamage` (combatdamage.go) in a loop over both matched lists, one shared `damageTable`
so `checkDamageDoneOnceTriggers` fires once for the whole sweep -- `dealDamageEffect`'s own multi-player loop the closer
precedent than `fightEffect`'s own two-call shape, since a sweep can be arbitrarily wide rather than fixed at two.
Deathtouch is never checked, faithfully: `DamageAllEffect.java`'s own resolve never reads `hasKeyword(Deathtouch)` at
all -- a real corpus reality (a sweeper's own source is overwhelmingly a spell or a planeswalker ability, not a
creature), not a port gap.

`setLifeEffect` (`setlifeeffect.go`, CR 119.4) reads `Player.setLife`'s own Java body precisely rather than porting a
description of it: `newLife > life ? gainLife(...) : newLife < life ? loseLife(...) : no-op` (CR 119.5), confirmed by
reading `Player.setLife` directly rather than assuming a plain field set the way `LifeSetEffect.java`'s own call site
suggested at a glance. `setPlayerLife`, the new shared function this forced into existence, re-derives exactly that
dispatch: a rise reuses `gainLifeEffect`'s own `gainLifePrevented`/`gainLifeReplaced`/`checkLifeGainedTriggers` sequence
(gainlifeeffect.go) rather than a third copy of it, a drop reuses `loseLifeEffect`'s own plain subtract (no prevention
machinery built, loselifeeffect.go's own identical gap), and an unchanged total does nothing at all, CR 119.5's own
explicit carve-out. `Redistribute$`'s own multi-player life-total-swap solver (`getDistribution`, `LifeSetEffect.java`)
is not ported -- 2 real lines, its own further mechanic entirely.

`shuffleEffect` (`shuffleeffect.go`, CR 701.20) turned out to be the thinnest M6 effect yet: `Game.Shuffle` (game.go)
already existed, `mulligan.go`'s own real caller for London's own "shuffle the cards put back" step, so this file is a
`targetedOrDefinedPlayers` loop around one already-built call, nothing more.

`exchangeLifeEffect` (`exchangelifeeffect.go`, CR 119.10) is `setPlayerLife`'s own second caller: every one of the 6
real `(AB|DB)$ ExchangeLife` lines names `ValidTgts$`, one target exchanging with the ability's own activator or two
targets exchanging with each other, and CR 119.10's own net effect reduces to "the higher life total loses the
difference, the lower gains it" -- `setPlayerLife` applied twice rather than a bespoke swap.

`NewRegistry` (`castspell.go`) registers all five; `enginelint.json` gained five new one-file groups, plus
`removecountereffect` allowing `putcountereffect` (`definedCounterTargets`'s own declaring file) and
`exchangelifeeffect` allowing `setlifeeffect` (`setPlayerLife`'s own declaring file) -- the first two M6 effect files
this port has that reference a THIRD effect file's own package-level function, rather than only the shared
non-effect-specific files (`defined.go`/`condition.go`/`amount.go`/...) every earlier one already did.

---

## TapAll, UntapAll, PutCounterAll, RemoveCounterAll and MultiplyCounter land

`tapAllEffect`/`untapAllEffect` (`tapalleffect.go`/`untapalleffect.go`, CR 701.21/701.22) are `tapEffect`'s/
`untapEffect`'s own battlefield-scan siblings -- 61/105 real `(AB|DB)$ TapAll`/`UntapAll` lines, every one naming
`ValidCards$` (a `valid.Parse`/`Matches` scan, not a target). `TapAllEffect.java`'s/`UntapAllEffect.java`'s own
`!sa.usesTargeting() && !sa.hasParam("Defined") ? game.getCardsIn(Battlefield) : getTargetPlayers(sa).getCardsIn(Battlefield)`
reads correctly only by hand: `targetedOrDefinedPlayers`'s own "neither present" default (`"You"`, one player) does not
match this API's own ("every player"), so both files guard the branch themselves rather than leaning on that default.
Both reuse `Memory.Remember` for `RememberTapped$`/`RememberUntapped$` (4/1 real lines) the identical way
`sacrificeAllEffect`'s/`pumpAllEffect`'s/`millEffect`'s own already do. `TapperController$`/`ControllerUntaps$`
("tapped/untapped by" attribution) stay unresolved: `tapEffect.go`'s own doc comment already found nothing downstream
reads a tap's own attributed player, and neither file changes that.

`putCounterAllEffect`/`removeCounterAllEffect` (`putcounteralleffect.go`/`removecounteralleffect.go`, CR 121.1/121.4)
are `putCounterEffect`'s/`removeCounterEffect`'s own battlefield-scan siblings -- 285/19 real lines, every one naming
both `ValidCards$` and `CounterType$`. `removeCounterAllEffect` reuses `removeCounters` (removecountereffect.go)
outright for its own per-card body -- the first M6 "All" sibling to share its per-entity write path with its own
singular effect's file directly, rather than each effect owning a private copy the way `damageAllEffect`'s own
`dealPermanentDamage`/`dealPlayerDamage` reuse (a different pair of files) already did. `AllCounters$` (12 of 19) reads
each target's own CURRENT count individually, `removeCounterEffect`'s own `CounterNum$ All` shape carried over
unchanged. `ValidTgts$` (8 of 285) on `putCounterAllEffect` narrows the sweep to one player's own permanents --
`CardLists.filterControlledBy` ported as scanning only that player's own battlefield zone via `targetedOrDefinedPlayers`
rather than scanning every zone and filtering after, an equivalent narrowing reached the opposite way round. Both files
defer a second `ValidCards2$`/`CounterType2$`/`CounterNum2$` pass and `Placer$`'s own per-card controller/owner override
-- each its own further mechanic, `putCounterEffect`'s own identical `putcountereffect.go` gap.

`multiplyCounterEffect` (`multiplycountereffect.go`, CR 121.5) is PutCounter's own doubling sibling -- 53 real
`(AB|DB)$ MultiplyCounter` lines, 38 naming `Defined$` (`Self`/`Targeted` dominant, `TriggeredAttackerLKICopy`/
`TriggeredSourceLKICopy` deferred, this port's own trigger-context `Defined$` gap) and 14 naming `ValidTgts$` --
`SpellAbilityEffect.getTargetEntities(sa)`'s own either/or contract ported as `targetedOrDefinedCards` since every real
line names a card spec on both sides, never a player: the one `Defined$ You` line (doubling a PLAYER's own poison/energy
counters) fails loudly instead of a special case, `definedCards`'s own unresolved-value error already covering it.
`CounterType$` absent (12 of 53) doubles every kind the target carries at once -- `CountersMultiplyEffect.java`'s own
`getCounterType(sa)` returning `null`, a distinct shape from `removeCounterEffect`'s own literal `CounterType$ All`
(this API's own real corpus never spells `"All"` here). `Game.getCardState`'s own timestamp-stale-LKI-copy re-read guard
(`CountersMultiplyEffect.java`'s own defensive check against a target that has since left the battlefield) is not
ported: `targetedOrDefinedCards` never returns a card the game itself no longer tracks, so nothing here needs protecting
against.

`NewRegistry` (`castspell.go`) registers all five, M6's twenty-third through twenty-seventh script-driven effects;
`enginelint.json` gained five new one-file groups, `removecounteralleffect` allowing `removecountereffect` (the second
M6 "All"-sibling file to reference a singular sibling's own package-level function,
[`## Five more land`](#five-more-land-removecounter-damageall-setlife-shuffle-and-exchangelife)'s own closing paragraph
naming the first two).

---

## Mana, MoveCounter, Poison, Unattach, RevealHand, LosesGame, WinsGame, Radiation, RemoveFromCombat and Connive land

`manaEffect` (`manaeffect.go`, CR 106.1) is a resolving mana burst -- 263 real `(SP|DB)$ Mana` lines (24 `SP$`, the
ritual-spell shape; 239 `DB$`, a sub-ability chain), distinct from `A:AB$ Mana`'s own 2,134 real lines (a permanent's
own printed mana ability, `ActivateManaAbility`,
[`## ActivateManaAbility lands`](activation.md#activatemanaability-lands-cr-6053-past-a-basic-lands-own-intrinsic-version)).
`Produced$` dispatches through that file's own `producedManaColor`/`parseComboColors`/`ChooseManaColor` reused outright
rather than re-derived -- Layer 605.1's own vocabulary is identical whether the mana comes from an activated ability or
a resolving effect.

`moveCounterEffect` (`movecountereffect.go`, CR 121.5) is PutCounter's/RemoveCounter's own card-to-card sibling -- 30
real `(AB|DB)$ MoveCounter` lines, every one a single `Source$` card (dominant `Self`) moving to one or more
`ValidTgts$`/`Defined$` destinations (`targetedOrDefinedCards`). `CountersMoveEffect.java`'s own "many sources to one
destination"/"one source to many destinations" branches and its own two-target shape (the ability's first TARGET itself
the source) all need a `chooseCardsForEffect`/`chooseNumber`/second-target-as-source dispatch this port does not have --
0 real lines need the first two, `TargetMin$`/`TargetMax$` (always literal 2) block the third.

`poisonEffect`/`radiationEffect` (`poisoneffect.go`/`radiationeffect.go`, CR 121.13/CR 121.13 respectively -- Radiation
is the newer of the pair, CR numbering aside both are a flat `Num$` add-or-remove onto `Player.Counters`) are
mirror-shaped: 35/22 real lines, a positive `Num$` adds, negative removes, `Counters.Add`'s own clamp-at-zero absorbing
an over-large removal the identical way `removeCounterEffect`'s own does. `Radiation` itself is a new `CounterType`
constant (`counters.go`) -- CR 704.5u's own upkeep dice-roll-or-lose-life consequence of carrying it needs
`PhaseHandler`'s own Upkeep step body, not built, but that is a separate CR paragraph from CR 121.13's own "a player who
is given radiation counters gets them," the whole of what this file resolves.

`unattachEffect` (`unattacheffect.go`, CR 704.5m) is a target-resolution loop around the already-built `Game.Unattach`
-- 7 real lines, all `Defined$` alone. Detaching an Aura leaves it dangling; `cleanupDanglingAttachments` (action.go)
already sends a now-unattached Aura to the graveyard on the very next state-based-action pass, which is why this
effect's own real corpus use always pairs it with a `SubAbility$` reattaching to a new host before that pass runs.

`revealHandEffect` (`revealhandeffect.go`, CR 701.15) discovered mid-port that revealing itself has nothing to change:
this port's engine is already omniscient, no hidden-information model for any zone, so Java's own
`game.getAction().reveal` call -- an information-only UI/log event -- has no state-changing counterpart. What DOES
change state is `RememberRevealed$`/`RememberRevealedPlayer$` (16/3 of the 50 real lines), both plain `Memory.Remember`
-- the reason this file exists at all rather than reducing to a silent no-op.

`losesGameEffect`/`winsGameEffect` (`losesgameeffect.go`/`winsgameeffect.go`, CR 104.3a/CR 104.1) are the first two M6
effects to touch `CheckStateBasedActions` (action.go) itself. `LosesGame` needed no engine change at all: `Player.Lost`
already existed for CR 704.5a's own 0-life check, and `CheckStateBasedActions` already reads it unconditionally every
pass regardless of why it became true, so 47 real lines resolve as a bare `Player.Lost = true` loop. `WinsGame` needed
one: `Player.Won` existed only as `CheckStateBasedActions`'s own CR 104.2a elimination-count OUTPUT, nothing read it as
an independent win condition, so a player set to have won some OTHER way (an effect, not elimination) would never
actually end the game. `CheckStateBasedActions` now checks `Player.Won` first, above the elimination count, ending the
game immediately the way CR 104.1 says an effect-driven win does -- CR 104.4a's own simultaneous-win draw covered too
(more than one `Won` player at once, `Actor` left `NoPlayer`). 42 real `WinsGame` lines resolve now.

`removeFromCombatEffect` (`removefromcombateffect.go`, CR 506.4) needed a new primitive: `Game.removeFromCombat`
(combat.go) drops id from `Attackers`, `AttackTargets` and every `Blocks` entry naming it either side -- CR 509.1h's own
"removed from combat" cleanup, not built anywhere yet since nothing before this effect ever removed a permanent from
combat outside dying. 24 real lines, 23 naming `Defined$`.

`conniveEffect` (`conniveeffect.go`, CR 702.164) is the first M6 effect built entirely by composing three already-built
primitives -- `Game.DrawCards`, `ChooseCardsToDiscard`/`discardCards` (discardeffect.go) and `Counters.Add` -- rather
than adding a new one of its own: "draw a card, discard a card, if the discarded card was nonland put a +1/+1 counter on
this creature" reduces to one call of each per conniver (51 real lines, `ValidTgts$`/ `Defined$` both through
`targetedOrDefinedCards`). `CountersMoveEffect`'s own APNAP-turn-order grouping and choose-which-conniver-resolves-first
step is not ported: this file resolves each targeted/defined card independently in target order instead, since which
physical card's own draw-then-discard happens first never changes any of their own outcomes.

`NewRegistry` (`castspell.go`) registers all ten, M6's twenty-eighth through thirty-seventh script-driven effects;
`enginelint.json` gained ten new one-file groups.

---

## Cleanup, the Choose family, DestroyAll, Reveal, PeekAndReveal, TapOrUntap and Proliferate land

Ten more M6 APIs, built as one pack because five of them only matter together: `Cleanup` and the four Choose effects
write the host card's `Memory` (`memory.go`), and `defined.go` gains the readers that make those writes observable. Real
`(AB|DB)$` line counts, Java source, and the shape each resolves:

- `Cleanup` — 3,011, `CleanUpEffect.java`: `ClearRemembered$` (2,705), `ClearChosenCard$` (238), `ClearImprinted$`
  (207), `ClearChosenPlayer$` (40), `ClearChosenColor$` (10).
- `ChooseCard` — 399, `ChooseCardEffect.java`: the default `chooseCardsForEffect` path — `Choices$`, `ChoiceZone$`,
  `DefinedCards$`, `Amount$`/`MinAmount$`/`Mandatory$`, `ControlledByPlayer$ Chooser`; then `RememberChosen$`/
  `ImprintChosen$`/`ForgetChosen$`/`ForgetOtherRemembered$`.
- `DestroyAll` — 238, `DestroyAllEffect.java`: `ValidCards$` sweep, first-targeted-player filter, `RememberDestroyed$`.
- `PeekAndReveal` — 138, `PeekAndRevealEffect.java`: `PeekAmount$`, `RevealValid$`, `NoReveal$`, `RevealOptional$`,
  `RememberRevealed$`/`ImprintRevealed$`/`RememberPeeked$`.
- `ChoosePlayer` — 127, `ChoosePlayerEffect.java`: `Choices$` as a `Defined$` player list, `RememberChosen$`,
  `ForgetOtherRemembered$`.
- `ChooseColor` — 122, `ChooseColorEffect.java`: `Choices$`, `Exclude$`, `TwoColors$`, `OrColors$`, `UpTo$`.
- `Proliferate` — 91, `CountersProliferateEffect.java`: `Amount$` rounds; players then permanents that have counters;
  one more of each kind (CR 701.34a).
- `Reveal` — 68, `RevealEffect.java`: chosen hand cards (`RevealValid$`, `NumCards$`, `AnyNumber$`, `Optional$`),
  `RevealDefined$`, `RevealAllValid$`, `RememberRevealed$`. The engine is omniscient, so the reveal itself changes no
  state; the remembered list is what the chain reads.
- `ChooseNumber` — 47, `ChooseNumberEffect.java`: the open (non-secret) `Min$`/`Max$` choice; `ChooseAnyNumber$` is the
  same `[min, max]` contract and shares it.
- `TapOrUntap` — 41, `TapOrUntapEffect.java`: the activator decides per target, or `Toggle$` flips it.

New `Defined$` readers (`defined.go`), all reading the host's `Memory`:

- Cards: `Remembered`/`RememberedCard` (card entries of the remembered list), `Imprinted`, `ChosenCard` —
  `AbilityUtils.getDefinedCards`' own branches.
- Players: `ChosenPlayer`; `Remembered` (remembered players, plus the players a remembered card itself remembers, one
  level deep — `addPlayer`'s own `skipRemembered` guard); `RememberedController`/`RememberedOwner` (controller/owner of
  each remembered card).

`definedPlayers`/`targetedOrDefinedPlayers` take the host `CardID` so the player readers can reach its `Memory`; every
caller passes `a.Source`.

`Memory` gains `Forget` (Java `removeRemembered`, for `ForgetChosen$`) and single-value `ChosenPlayer`/`ChosenColors`/
`ChosenNumber` slots, each overwritten by the next choice and cleared by `Cleanup`. `ChosenNumber` reports `ok`
separately because zero is a legal pick.

`PlayerController` gains seven decisions, each named for its Java `PlayerController` counterpart:

- `ChooseCardsForEffect` — `chooseCardsForEffect`/`chooseCardsToRevealFromHand`, for `ChooseCard` and `Reveal`.
- `ChoosePlayerForEffect` — `chooseSingleEntityForEffect`, for `ChoosePlayer`.
- `ChooseColors` — `chooseColors`, for `ChooseColor`.
- `ChooseNumber` — `chooseNumber`/`announceRequirements`, for `ChooseNumber`.
- `ChooseTapOrUntap` — `chooseBinary(BinaryChoiceType.TapOrUntap)`, for `TapOrUntap`.
- `ChooseEntitiesForEffect` — `chooseEntitiesForEffect`, for `Proliferate`.
- `ConfirmReveal` — `confirmAction` ("reveal to other players?"), for `PeekAndReveal`'s `RevealOptional$`.

Every answer is re-checked by `checkChoice` (`choosecardeffect.go`): drawn from the offer, no repeats, count in range. A
bad answer is an `error`, not a panic (GO-7).

Deliberately unresolved, each failing the whole line closed (PORT-8):

- Every `Random$`/`AtRandom$` across the pack. Java draws through `Aggregates.random`/`MyRandom`; matching its exact
  draw order is a parity question of its own.
- `ChooseCard`'s alternative loops (`ChooseEach$`, `EachBasicType$`, `WithTotalPower$`, `WithDifferentPowers$`,
  `EachDifferentPower$`, `ControlAndNot$`, `QuasiLibrarySearch$`), `ChosenMap$` and `Secretly$`.
- `ChooseNumber`'s `Secretly$` branch and its `Highest$`/`Lowest$`/`Guess*$`/`Matched*$` AdditionalAbility dispatch.
  `RememberChosen$` and `RemoveChoices$` remember Integers, which `Memory` cannot hold.
- `ChoosePlayer`'s `ChooseSubAbility$`/`CantChooseSubAbility$` (AdditionalAbility dispatch again) and `Protect$`.
- `DestroyAll` `ValidCards$` containing `X`. Java textually substitutes the X value into the valid string before
  parsing, a runtime script re-interpretation PORT-2 forbids.
- `Proliferate`'s own replacement family and trigger mode are not ported; it adds counters directly, the same way
  `putCounterEffect` does.

`PeekAndReveal` reproduces one Java quirk (PORT-7): its `numPeek` is clamped to each player's library size in place, so
a short library shrinks the peek for every later player in the same resolution.

54 new tests across ten behavior files. `choosecardeffect_test.go` holds `etbChainDef`/`castETBChain`/
`newTwoPlayerGame`, shared by the pack: a Memory-writing ETB trigger chained through `SubAbility$` to a reader.

---

## ChangeZone, Dig, the AdditionalAbility effects, regeneration, extra turns and control land

Twenty more M6 APIs in one pack, grouped by the engine piece each needed. Real `(AB|DB)$` line counts and Java source:

- Zone movement: `ChangeZone` (5,576, `ChangeZoneEffect.java`), `Dig` (813, `DigEffect.java`), `ChangeZoneAll` (530,
  `ChangeZoneAllEffect.java`), `DigUntil` (153, `DigUntilEffect.java`), `Explore` (42, `ExploreEffect.java`),
  `RearrangeTopOfLibrary` (29, `RearrangeTopOfLibraryEffect.java`), `LookAt` (6, `LookAtEffect.java`).
- AdditionalAbility dispatch: `RepeatEach` (288, `RepeatEachEffect.java`), `GenericChoice` (149,
  `ChooseGenericEffect.java`), `Branch` (100, `BranchEffect.java`), `Repeat` (37, `RepeatEffect.java`).
- Rules state: `Regenerate` (262, `RegenerateEffect.java`), `GainControl` (256, `ControlGainEffect.java`), `AddTurn`
  (43, `AddTurnEffect.java`), `ExchangeControl` (32, `ControlExchangeEffect.java`), `Fog` (15, `FogEffect.java`),
  `SkipTurn` (13, `SkipTurnEffect.java`).
- Small: `EachDamage` (27, `DamageEachEffect.java`), `DrainMana` (5, `DrainManaEffect.java`), `HealDamage` (2,
  `HealDamageEffect.java`).

### Zone movement

`moveByEffect` (`zonemove.go`) is `GameAction.moveTo` as an effect drives it. A card entering the battlefield goes to
the new controller's side (`GainControl$`), is tapped first (`Tapped$`), then runs `checkMovedReplacement` and the ETB
triggers exactly as `permanentEffect` does. A library destination honours the top (0) or the bottom (-1). A card leaving
the battlefield fires the dies, exiled or returned trigger matching its destination. `orderCardsByTheirOwners` is
`GameActionUtil.orderCardsByTheirOwners` (CR 613.7m): per deciding player in APNAP order, each group of two or more
ordered through the new `OrderCardsForZone` decision.

`ChangeZone` keeps Java's `isHidden` split. The known-origin path moves targeted or `Defined$` cards still in an
`Origin$` zone (`Optional$`, `ShuffleNonMandatory$`, CR 401.4 owner ordering into a library, `Shuffle$ True`). The
hidden-origin path searches the `Origin$` zones for up to `ChangeNum$` cards matching `ChangeType$`, or takes
`Defined$`/`ChooseFromDefined$` cards. Each fetcher (`DefinedPlayer$`, default You; the first targeted player with
`ValidTgts$`) chooses before anything moves, as Java's `HiddenOriginChoices` does. A searched library is shuffled
afterwards, or before the move when the destination is that library, so a card put on top stays there.

`Dig` moves its picks in reverse pick order (Java's own `Collections.reverse`) before owner ordering.
`RestRandomOrder$`, `ChangeZoneAll`'s `RandomOrder$` and `DigUntil`'s `RevealRandomOrder$` shuffle with `Game.rand`, the
same Java-parity stream library shuffles use. `DigUntil` reproduces Java's "sequential" mode: when the found and
revealed destinations are the same zone, every revealed card moves in reveal order. `LibraryPosition$` values other than
0 and -1 fail closed; `Game` has no insert-at-depth move.

### AdditionalAbility dispatch

`compile` already resolves every AdditionalAbility key (`TrueSubAbility$`, `RepeatSubAbility$`, `Choices$`, ...) into
`Ability.Subs`. `additional.go` resolves one on demand: a child `Ability` sharing the parent's source, controller and
targets, through the `Registry` driving the stack. `Registry.Resolve` records itself on `Game.registry` for this. That
makes `Branch` (`TrueSubAbility$`/`FalseSubAbility$`), `GenericChoice` (the new `ChooseAbilitiesForEffect` decision;
`TempRemember$` swaps the chooser in as the only remembered player), `Repeat` (`RepeatPresent$`/`RepeatCheckSVar$` reuse
`isPresentMatches`/`checkSVarMatches` with the `Repeat*` keys; `RepeatOptional$`; `MaxRepeat$`) and `RepeatEach` (each
card, target or player swapped into `Memory` for one resolution; `UseImprinted$`) resolvable.

### Regeneration, extra turns, control, Fog

- `Card.RegenShields` counts shields (`Regenerate`). `Game.regenerate` (`regeneration.go`) is `RegenerationEffect.java`:
  one shield replaces one destruction by removing all damage, tapping, and removing from combat. `Destroy`, `DestroyAll`
  and the lethal-damage state-based action (`destroyDamagedCreatures`) all consult it; `NoRegen$`/`NoRegenValid$` skip
  it. Shields end at cleanup and when the permanent leaves the battlefield.
- `Game.extraTurns` is Java's extra-turn stack: the first extra turn also pushes the player whose normal turn comes
  next, so turn order resumes from the right seat. `Player.TurnsToSkip` is `SkipTurn`'s counted-down BeginTurn
  replacement. `nextActivePlayer` (`turn.go`) reads both.
- `Card.tempControllers` is Java's `addTempController`: one-shot control changes merged by timestamp with Layer 2's
  continuous `ControlMod` in `Controller()`. A real change removes the permanent from combat and makes it summoning sick
  (CR 506.4, 302.6). Leaving the battlefield clears them and resets the base controller to the owner.
- `Game.combatDamagePrevented` is `Fog`'s effect card: `damagePrevented`/`damagePreventedPlayer` prevent combat damage
  while it is set, and cleanup clears it.

### Decisions and helpers

`PlayerController` gains `ConfirmEffect` (Java's generic `confirmAction` for `Optional$`-style prompts),
`OrderCardsForZone` (`orderMoveToZoneList`) and `ChooseAbilitiesForEffect` (`chooseSpellAbilitiesForEffect`). Small
helpers several effects share (`checkChoice`, `filterValid`, `hasParam`, the remembered-player swap) move into
`effecthelpers.go`, so effect files no longer reach into each other. `enginelint.json` drops 129 allow entries that no
longer match a real reference.

### Deliberately unresolved (fail closed, PORT-8)

- `ChangeZone`: entering-state modifiers (`Transformed$`, `WithCountersType$`, `FaceDown$`, `AttachedTo$`),
  `Attacking$`, durations and delayed follow-ups (`Duration$`, `AtEOT$`, `LeaveBattlefield$`), alternative zones, the
  non-default search loops (`AtRandom$`, `Different*$`, `WithTotal*$`, `EACH` change types), `Chooser$`, and origins or
  destinations outside the five modeled zones (`Stack`, `Command`, `Sideboard`, `All`).
- `Dig`/`DigUntil`: `FromBottom$`, `SourceZone$`/`DigZone$`, face-down and counter modifiers, `ChangeValid$` naming
  `ChosenType`.
- `GenericChoice`: random picks, `SetChosenMode$`, and any choice carrying `UnlessCost$` (Java drops choices whose cost
  cannot be paid; that payability check is not ported).
- `RepeatEach`: stack-object and card-type loops, `NextTurnForEachPlayer$`, vote amounts, the batched damage/zone/life
  tables, `ChooseOrder$`, `StartingWith$`.
- `GainControl`: every duration-scoped change (`LoseControl$`, 126 lines), `AddKWs$`, `AllValid$`, and the targeting
  restrictions `targeting.go` does not enforce. `ExchangeControl` rejects the same restrictions.
- `AddTurn`'s delayed-trigger, skip-untap and scheme variants; `Regenerate`'s `RegenerationAbility$`; `DrainMana`'s
  `RememberDrainedMana$` (an Integer in `Memory`).

62 new test functions. `failclosed_test.go` walks one fail-closed line per unresolved branch across the pack.
`subability_test.go`'s unimplemented-API chain now ends in `Token`, since `ChangeZone` resolves.

---

## Tokens, Animate, delayed triggers, Charm, dice and extra phases land

Twenty more script-driven APIs resolve, 87 of the corpus's 203. They share five new pieces of engine.

**Tokens** (token.go). `compile.DB` now holds every `res/tokenscripts` file keyed by file name (`LoadTokenScripts`,
`DB.Token`; `TestTokenScriptsCompile` compiles all 853), because `TokenScript$` names a file, not a printed name, and
many scripts share one ("Soldier Token"). `createToken` is `TokenEffectBase.makeTokenTable`'s inner loop: a new card
flagged `IsToken`, `TokenPower$`/`TokenToughness$` as its base P/T, tapped with `TokenTapped$`, enter-with +1/+1
counters for `Incubate`, then `moveByEffect` from `ZoneType` `None` -- ETB replacement and triggers included,
`Origin$ Any` matching and `Origin$ Hand` not. A token anywhere but the battlefield leaves the game silently at the next
state-based action check (CR 704.5d, `removeTokensOffBattlefield`), and a token reaching a graveyard no longer marks its
owner as having descended.

- `Token`: every `TokenScript$` × `TokenAmount$` for every `TokenOwner$` player -- named, else the targets, else You --
  in APNAP order (`SpellAbilityEffect.getPlayers`' sort), `ChangesZoneAll` once per batch. `RememberTokens$`,
  `ImprintTokens$`, `RememberSource$`, `PumpKeywords$`/`PumpDuration$` resolve. `TokenAttacking$`, `AttachedTo$`,
  `WithCountersType$`, `TokenTypes$`/`TokenColors$`, `AtEOT$` and the remembered-object params fail closed.
- `Investigate`: a Clue (`c_a_clue_draw`) per player per `Num$`.
- `Amass` (CR 701.47): an Army when the amasser controls none, counters on one they choose, and "becomes a <Type>" as a
  Permanent Layer 4 record. Java rewrites `b_0_0_army`'s types and name; every real `Type$` (Goblin 15, Orc 33, Sliver
  1, Zombie 24) has its own `b_0_0_<type>_army` script with that exact result, so this port uses it and fails closed on
  a type without one.
- `Incubate`: `incubator_c_0_0_a_phyrexian` with `Amount$` counters, `Times$` times.

`definedPlayers` gains `TargetedController` (42 real `TokenOwner$` lines) and `ThisTargetedPlayer`.

**Animate-shaped layer records** (animate.go). A resolved Animate, AnimateAll, Debuff, Protection or ProtectionAll is an
`animateRecord`: one timestamp's Layer 4, 5, 6 and 7b change to one card, re-applied every state-based action pass after
the Mode$ Continuous appliers (the `Game.pumps` pattern) and applied at once as it resolves, so a chained `SubAbility$`
already sees the change. It ends at cleanup unless `Duration$ Permanent`, and leaves with the card. Two layer folds
changed to reach it:

- `KeywordEffect` carries removals: `KeywordLines` folds printed keywords through every change in timestamp order,
  removals first (`KeywordsChange.applyKeywords`), a removal dropping every line starting with it
  (`KeywordCollection.remove`'s `startsWith`). A static `RemoveKeyword$` line still skips.
- `TypeEffect` carries the category removals and applies in `CardChangedType.applyChanges` order -- category flags, then
  `RemoveTypes`, then `AddTypes` (the fold had added before removing; nothing overlapped, so no line changed). CR 205.1a
  keeps Instant/Sorcery under `RemoveCardTypes$`. `RemoveCreatureTypes$` and its land/artifact/enchantment siblings test
  the corpus' subtype vocabulary, now on `compile.DB` (`DB.Types`); a DB without one fails the line.

Resolved: `Power$`, `Toughness$`, `Types$`, `RemoveTypes$`, the `Remove*Types$` flags, `Colors$` (names, `ChosenColor`,
`All`) with `OverwriteColors$`, `Keywords$`, `RemoveKeywords$`, `RememberAnimated$`. Failing closed: granted abilities,
triggers, replacements, statics and SVars, `HiddenKeywords$`, `RemoveAllAbilities$`, `Perpetual` and every duration past
end of turn and Permanent, `Types$ ChosenType`. Debuff fails closed when removing "Protection from <color>" from a card
whose protection Java would split first. Protection's `Gains$ Choice` asks `ChooseProtectionType`; a card type becomes
`Protection:<type>` (ProtectEffect's `isACardType` split), which `protectionEach` already reads.

**Delayed and reflexive triggers** (delayedtrigger.go). `Game.delayed` is `TriggerHandler`'s delayed list: a
`DB$ DelayedTrigger` line registers itself, fires once, and is gone. `ThisTurn$` lapses as the next turn begins
(`clearThisTurnDelayedTrigger`); `NextTurn$`/`UpcomingTurn$` wait for the cleanup `executeUntil` Java runs after
`handleNextTurn`, so a `NextTurn$` trigger lives for exactly the following turn; `DelayedTriggerDefinedPlayer$` waits
for that player's turn. Only `Mode$ Phase` fires -- 380 of 461 real lines; the other modes fail closed rather than
register a trigger no check would ever consult. `RememberObjects$` resolves through the new `definedEntities`
(`getDefinedEntities`: players, then cards) into `Ability.TriggerRemembered`, carried onto every sub-ability the way
Java reads the root ability; `Defined$ DelayTriggerRemembered`/`DelayTriggerRememberedLKI` (29/208 real lines) read it,
threaded through the `Defined$` helpers as `abilityRefs`. `ImmediateTrigger` pushes `TriggerAmount$` copies of
`Execute$` at once -- Java's "fire at the next trigger check" lands them above everything the parent left on the stack,
the same order. `ConditionPhases$` (`parsePhaseRange`, `PhaseType.parseRange`), `ConditionPlayerTurn$` and
`ConditionFirstCombat$` join the sub-ability condition gate.

**Charm** (charmeffect.go). Modes are chosen as the Charm goes on the stack (CR 700.2, Java's
`PlaySpellAbility`→`makeChoices`): `pushTriggeredAbilities` -- the push both triggers and activations use -- calls
`chooseCharmModes`, which drops modes with no legal target (CR 603.3c), asks `ChooseModesForAbility` for `MinCharmNum$`
to `CharmNum$` of them (or reservoir-samples with `Random$`), and resolves each chosen mode's targets. Resolving the
Charm resolves `Ability.Modes` in printed order. A choice error rides the pushed Charm and surfaces when it resolves,
since pushing has no error path. `ChoiceRestriction$`, `Chooser$` and `CanRepeatModes$` fail closed.

**Dice, coins, clash, seek, stored SVars, balance.**

- `FlipCoin` (CR 705): one `nextBoolean` per flip on the game's stream, called or not, `FlipUntilYouLose$`, `Wins`/
  `Losses` (or `SaveNumFlipsToSVar$`) bound for the sub it resolves. A `FlipCoinMod`/`FlipCoinDoubler` static in play
  fails the flip closed.
- `RollDice` (CR 706): `nextInt(sides)+1` per die, sorted, `IgnoreLower$`, `Modifier$`, `ResultSubAbilities$` ranges and
  `Else$`, `SubsForEach$`, `ResultSVar$` and the counted results bound for the chain. A reroll or increment card or a
  RollDice replacement in play fails the roll closed; `Mode$ RolledDie` triggers are not ported.
- `Clash` (CR 701.30): top cards, higher mana value wins, each player keeps theirs on top or puts it under
  (`WillPutCardOnTop`, reordered in place -- Java suppresses the zone change).
- `Seek`: `Aggregates.random(pool, n)`'s reservoir sample of matching library cards to hand.
- `StoreSVar`: `Card.svars`, a runtime SVar shadowing the script's for `resolveNamedAmount`; `Number`, `Calculate`,
  `CountSVar` (an SVar with a `Plus`/`Minus`/`Twice` suffix) and `AdditiveForEach` resolve -- the types 137 of 140 real
  lines name; `Targeted`/`Triggered` and `SVar$ EachPlayer` fail closed.
- `Balance`: everyone down to the fewest, sacrificing as each chooses or discarding together.

Every random pick uses `Game.rand` with Java's exact draw sequence (random.go's `randomSample` is `Aggregates.random`'s
reservoir), so a seeded game replays the oracle's result.

**Extra and skipped phases** (turn.go, addphaseeffect.go). `Game.extraPhases` is `PhaseHandler.extraPhases` as an array
indexed by phase: `AdvancePhase` takes the phase an `AddPhase` queued after the current one before the normal next one,
and clears the stack as the turn ends; `addExtraPhase` ports the linking order, so an extra combat "followed by an
additional main phase" still returns to the turn's own second main phase. `SkipPhase` records are Java's command-zone
`BeginPhase` replacement: a named phase or step of that player's turn is passed over without beginning -- a skipped
combat jumps to its end step first, as `advanceToNextPhase` does -- once, or each time until end of turn with
`Duration$ EndOfTurn`. `Game.combatsThisTurn` counts combat phases begun, for `FirstCombat$`.

`BecomeMonarch` was researched and left out here: Java's monarch is an effect card in the command zone carrying its own
draw and combat-damage triggers, and nothing at this point gave an ability a host without a card. Ported later, once the
`Effect` port gave Command-zone `IsEffect` cards their own live triggers --
[`effects-monarch-initiative-venture.md`](effects-monarch-initiative-venture.md).

---

## Fifty more: combat changes, choices, prevention, copies, face-down cards, transform, counterspells

Fifty more script-driven APIs resolve, 137 of the corpus's 203. Most are small; the engine pieces they share:

**Decisions.** `PlayerController` gains `ChooseBinary` (Java's `chooseBinary`, a `BinaryChoice` naming its
`BinaryChoiceType`: TapOrUntap, OddsOrEvens, LeftOrRight, AddOrRemove, Pile1OrPile2) and `ChooseOption` (a pick from a
list of strings: `chooseSomeType`, `chooseCardName`, spellbook picks, counter types). `Memory` records a chosen
even/odd, direction, type, second type and named cards; valid strings read them (`cmcChosenEvenOdd`/
`cmcNotChosenEvenOdd`, `ChosenType`/`IsNotChosenType`/`ChosenType2`, `NamedCard`, `NamedByRememberedPlayer`,
`IsSuspected`, `IsSolved`), and `Cleanup`'s `ClearChosenType$`/`ClearNamedCard$` now clear them.

**Turn and game state.**

- `EndTurn` (CR 723) exiles every spell on the stack, empties it, ends combat, checks state-based actions and begins
  cleanup at once (`endTurnByEffect`); `EndCombatPhase` does the same inside combat and moves on past end of combat.
- `ReverseTurnOrder` flips `Game.turnOrderReversed`, read by `nextPlayerAfter`/`nextPlayerInDirection`
  (`getNextPlayerAfter(p, direction)`).
- `GameDrawn` ends the game with no winner. `ChangeSpeed` moves `Player.Speed` within 1-4.
- `DayTime` sets day, night or switches (`Game.dayTime`); CR 726.3a's untap-step change reads the spells the previous
  turn's active player cast (`Player.SpellsCastThisTurn`, counted by `CastSpell`). It fails closed while a daybound or
  nightbound permanent or a `CantChangeDayTime` static is out: those need transforms the day change would trigger.
- `Detain` (CR 701.35): `Card.detainedBy`, read by the attack and block eligibility checks and every activation path
  (`ActivateAbility`, `ActivateManaAbility`, `TapLandForMana`), each ending as its detainer's turn begins.
- `Goad` (CR 701.15): `Card.goadedBy`, until the goader's next turn or `Duration$ Permanent`. A goaded creature that can
  attack must be declared attacking (CR 508.1d, `validateAttackers`, [`effects-mustblock.md`](effects-mustblock.md)),
  and attacks a player other than a goader if it can (`goadTargets`).

**Combat.** `Combat.ForcedBlocked` is `setBlocked` without a blocker: `BecomesBlocked` adds to it and combat damage
treats the attacker as blocked (no damage without trample). `Block` adds blocks with their triggers; `ChangeCombatants`
with `Attacking$ True` adds or redirects an attacker to the defender the activator picks.

**Prevention shields.** `PreventDamage` gives each target a "prevent the next N" shield (`Game.preventShields`), spent
oldest first in `dealPermanentDamage`/`dealPlayerDamage` after full prevention and before damage replacement, ending at
cleanup or when a card leaves. Java builds a command-zone effect with a `PreventionEffect$ NextN` replacement per
target.

**Damage maps.** `DealDamage`'s `DamageMap$` records into the ability's `pendingDamage` (shared down the sub-ability
chain the way `getDamageMap` walks to the parent), summed per source and target; `DamageResolve` deals the batch.

**Face-down and transformed cards.** A face-down permanent's `Card.Def` is a nameless, colorless 2/2 creature
definition, its own kept in `faceUpDef`; `Manifest`, `Cloak` (ward 2) and `ManifestDread` put cards onto the battlefield
that way, and `SetState` `Mode$ TurnFaceUp` restores it. Transform works the same way: `compile.Face` now carries each
face's name and `compile.Card` its `SplitType`, and `Game.transform` swaps a transforming double-faced permanent's `Def`
for a one-face definition built from the other face, with a new timestamp (CR 613.7g). A card leaving the battlefield
turns face up and front face up. `Ability.hostTransforms` is Java's `StoredTransform` (CR 701.28f), taken as the ability
goes on the stack or as a delayed trigger is made.

**Stack targets.** `TargetType$ Spell` targets a spell on the stack -- a card in the Stack zone
(`stackSpellCandidates`); any other `TargetType$` is a shape the targeting step does not resolve. `Counter` removes the
spell and sends its card to `Destination$`.

**Copies and conjuring.** `CopyPermanent` makes token copies from the original's copiable values -- its definition and
any base power/toughness its creating effect set, with `SetPower$`/`SetToughness$` as copy exceptions (CR 707.9b).
`MakeCard` makes cards from the game's DB by name, name list, defined card or spellbook pick.

**The rest.** `BlankLine`; `RemoveFromGame`/`RemoveFromMatch` (`ceaseToExist`, match and inventory bookkeeping being
outside one game); `GainOwnership`; `ReorderZone` (`setZoneOrder`, no zone change); `ChooseEvenOdd`; `ChooseDirection`;
`ExchangeLifeVariant`; `ExchangePower`; `TapOrUntapAll`; `AddOrRemoveCounter`; `GainControlVariant` (all five
`ChangeController$` modes); `ExchangeControlVariant`; `Intensify`; `Blight`; `TimeTravel`; `Endure`; `AssignGroup`;
`VillainousChoice`; `TwoPiles`; `MultiplePiles`; `ChooseType`; `NameCard` (front faces of the DB's cards under
`CardFacePredicates.valid`); `DigMultiple`; `Recruit`; `BidLife`; `Vote`; `AlterAttribute` (Suspected with menace and
"can't block", Solved, Harnessed, Plotted); `Learn` (sideboard Lesson or rummage); `ActivateAbility` with `ManaAbility$`
(intrinsic basic land abilities and scripted Mana abilities).

**Order where Java's is arbitrary.** Java keeps several collections in hash order: `Vote`'s tally and `EachVote$` order,
`MultiplePiles`' per-player piles, `DigMultiple`'s categories, and the subtype lists `ChooseType` offers. This port uses
the options' own order, sorted subtypes, and refuses `AtRandom$` over a list with no stable order.

**Deliberately unresolved (fail closed, PORT-8).**

- `Play`, `Clone` past its "becomes a copy" shapes, `CopySpellAbility`, `Phases`, `MustBlock`, `RingTemptsYou` (a
  command-zone effect with its own triggers, same shape as the now-ported `BecomeMonarch`/ `TakeInitiative`/`Venture`),
  `SwitchBlock` (both real lines use `Defined$ Valid ...`), `ChooseSector`, and the Planechase/Archenemy/Un-set/Alchemy
  APIs.
- `Counter`: abilities as targets, `Defined$` spells, a `CantBeCountered` static or `Counter` replacement,
  `RememberCounteredCMC$` (an Integer). `SetState`: `Flip`, `TurnFaceDown`, `Specialize`, a `CantTransform` static or
  `Transform` replacement. `CopyPermanent`: every copy exception past power/toughness, end-of-turn cleanup, attacking
  copies. `Vote`: extra and controlled votes. `VillainousChoice`: extra choices. `Learn`: a `Learn` replacement.
  `AlterAttribute`: Prepared, Saddled, Commander, Suspected under `CantBeSuspected`. `ChooseType`: `Secretly$`, `Note$`,
  `TypesFromDefined$`. `NameCard`: `ChooseFromDefinedCards$`, `AtRandom$` over every card, `ManaCost=` filters.
  `ActivateAbility`: non-mana activations.

## ChooseSource and Empower land

139 of 203 script-driven APIs resolve.

**`ChooseSource`** (`choosesourceeffect.go`, `ChooseSourceEffect.java:32-142`). Each chooser (`Defined$`/`ValidTgts$`,
default `You`) picks one source; the pick becomes the host's chosen card (`Memory.Choose`), `RememberChosen$` also
remembers it. 65 of 67 real lines chain into `DB$ Effect`, still `ErrUnimplemented`.

| Pool group, in Java's order  | This port                                                                             |
| ---------------------------- | ------------------------------------------------------------------------------------- |
| Battlefield permanents       | Every player's battlefield, seat order                                                |
| Stack item sources           | Resolving ability first, then `g.stack` top to bottom                                 |
| Objects stack items refer to | First targeted card per item (`getTargetCard`); triggering/replacing objects not held |
| Face-up Command-zone cards   | Every player's Command zone, face-down skipped                                        |

- Pool is an `OrderedSet`: Java's `CardCollection` keeps a card at its first position, so an activated ability's source
  or a targeted permanent is offered once, among the permanents. Reason: an `[]CardID` would offer it twice.
- Resolving ability counts as the first stack item. Reason: `MagicStack.resolveStack` removes it only after resolving
  (`MagicStack.java:572`); `ResolveStack` pops first (CR 608.2m), so the resolving ability stands in for that first item
  and an `SP$ ChooseSource` spell offers its own card, as Java does.
- Java's four `--PERMANENTS:--`-style divider cards are left out. Reason: its own do/while rejects every pick naming
  one, so none can reach the chosen list.
- One `ChooseCardsForEffect(lo=1, hi=1)` per chooser, pick removed from the shared pool. `setChosenCards` replaces per
  chooser (`:137`), so with several choosers only the last pick stays chosen (PORT-7); `RememberChosen$` keeps all.
- `Ability` carries no triggering or replacing objects (`getTriggeringObjects`/`getReplacingObjects`), so a card only a
  trigger or replacement on the stack refers to is not offered.

Rejected with `not resolvable yet`: `Amount$` (0 real lines; Java's do/while never ends once the pool runs dry),
`TargetControls$` (0 real lines), `Choices$` naming `ChosenColor` (2 lines) or a suffixed `SharesColorWith` (2 lines).
Reason: each would read false for every card and silently empty the pool.

Also rejected: a color-`Source` `Choices$` while a `Mode$ ColorlessDamageSource` static is in play and a stack item's
source is off the battlefield. Reason: Ghostly Flame's `Spell.<Color>+inZoneStack` clauses need a `Spell` base `Matches`
has no case for, so a red spell would still read red.

**Forge bugs (PORT-8, not carried; tracked in [`forge-java-defects.md`](../../forge-java-defects.md)).**

| Site                              | Bug                                                                                                                                | Here           |
| --------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | -------------- |
| `ChooseSourceEffect.java:84-89`   | `TargetControls$` read by presence only; `tgtPlayers.get(0)` unguarded, `IndexOutOfBoundsException` when the chooser left the game | Param rejected |
| `ChooseSourceEffect.java:131-133` | Pool exhausted before every chooser picks → do/while rejects the dividers forever, game hangs                                      | `error`        |

**`<Color>Source` valid property** lands with it: 15 of 67 `Choices$` lines name one (`Card.RedSource`, ...).
`valid-strings.md` has the rule and the Ghostly Flame exception.

**`Empower`** (`empowereffect.go`, `EmpowerEffect.java:48-98`). First target or `Defined$` player (default `You`); none
is a no-op (`FCollection.getFirst` returns null). No token of `Type$` on their battlefield → create one from
`u_empower_<type>` if the DB holds it, else `u_empower` (the only one shipped), with `Type$` added to its type line and
named `<Type$> Token`. Then `Num$` (default 1) loyalty counters on one such token, the player's choice among several.

- Prototype is a copy of the compiled token script. Reason: the DB's `*compile.Card` is shared, immutable across games
  (GO-2).
- Counters go on after the token enters and before state-based actions, so a 0-loyalty token survives.
- `GameEntityCounterTable.replaceCounterEffect` not ported, same gap as `PutCounter`.
- All 32 corpus lines are `Type$ Jace`.

**Researched and deferred.**

| API             | Blocker                                                                                                  |
| --------------- | -------------------------------------------------------------------------------------------------------- |
| `MustBlock`     | Enforcement breaks `DeclareCombatBlockers`' "not re-checked" contract; re-prompt vs correct needs an ADR |
| `ManaReflected` | Triggered mana abilities (CR 605.1b), [`effects-manareflected.md`](effects-manareflected.md)             |

`BecomeMonarch`/`TakeInitiative`/`Venture` retired from this table: their own blocker (synthetic Command-zone effect
cards with their own triggers) stopped applying once the `Effect` port gave Command-zone `IsEffect` cards live triggers
-- [`effects-monarch-initiative-venture.md`](effects-monarch-initiative-venture.md).

**Forge bug (PORT-8, found researching `BecomeMonarch`; tracked in
[`forge-java-defects.md`](../../forge-java-defects.md)).** `Player.java:3434-3436`, `getMonarchSet`: condition inverted
(`monarchEffect == null ? monarchEffect.getSetCode() : null`) — always null in the normal case, NPE otherwise. Sibling
`getInitiativeSet` (`:3486-3488`) is correct. Cosmetic (set code for the effect card's image); to report upstream.

---

## Earthbend, Airbend, Discover, Draft, Heist and ExchangeZone land

Six more script-driven APIs resolve, 145 of the corpus's 203 (`Empower` landed with `ChooseSource`, above). Corpus
lines: Earthbend 38, Airbend 13, Discover 37, Draft 42, Heist 8, ExchangeZone 1.

| API            | Resolves                                                                                                                                                                                                             | Fails closed / not modeled                                                                                   |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `Earthbend`    | Target land you control becomes a 0/0 haste land creature (Permanent `animateRecord`), `Num$` +1/+1 counters, two delayed "return it tapped under your control" triggers (dies, exiled), then ElementalBend triggers | `AB$ Earthbend` (3 lines): `ActivateAbility` chooses no targets yet                                          |
| `Airbend`      | Each target or `Defined$` permanent exiled; owner may cast a nonland non-token one for {2} (`ExilePlayGrant`); `ChangesZoneAll`; ElementalBend triggers when anything moved                                          | `TgtZone$` (1 line, a spell on the stack)                                                                    |
| `Discover`     | Exile from top until nonland with MV ≤ `Num$`, one `ChangesZoneAll` per card; cast (`ConfirmEffect` true) or hand; rest to bottom shuffled; `Mode$ Discover` triggers; `RememberDiscovered$`                         | Casting an instant, sorcery or Aura: checked by peeking before anything moves, then an error                 |
| `Draft`        | `DraftNum$` rounds: spellbook shuffled, first three made (the `A-` version when the DB has it), one picked to hand; `RememberDrafted$`                                                                               | --                                                                                                           |
| `Heist`        | `Num$` rounds: three random nonland cards of the target's library, one exiled face down; heister may play it with any mana type (`ExilePlayGrant`)                                                                   | `addMayLookFaceDownExile` (no hidden-information model); `canExiledBy` (no `CantExile` static)               |
| `ExchangeZone` | `Object$` (default host) in `Zone1$` swaps with a chosen `ValidExchange$` card in `Zone2$`; optional unless `Mandatory$`                                                                                             | `Type$` (Aura re-attachment, 0 lines). The one real line (Darkpact) is an ante sorcery this port cannot cast |

Shared engine pieces:

- **`ExilePlayGrant`** (`airbendeffect.go`, `Game.exileGrants`, copied by `Clone`): the `MayPlay$` static on the
  command-zone effect card `AirbendEffect.java:95-104` and `HeistEffect.java:58-67` create. Their forget-on-moved and
  forget-on-cast triggers fold into the card's exile timestamp: any later zone change ends the grant.
  `Game.MayPlayFromExile` is the query; `CastSpell` still casts only from hand, so nothing consumes it yet.
- **Delayed battlefield-leaving triggers** (`delayedtrigger.go`, `delayedLeftBattlefieldMatches`): a delayed
  `Mode$ ChangesZone` (Origin Battlefield, Destination the zone) or `Mode$ Exiled` trigger with
  `ValidCard$ Card.IsTriggerRemembered`, checked from `checkDiesTriggers` and `checkExiledTriggers`. Fires once and is
  removed; the other of Earthbend's pair stays, as in Java. `earthbendReturnTrigger` builds the trigger tree
  `buildTrigger` (`EarthbendEffect.java:74-89`) parses from strings.
- **Implied targets:** `EarthbendEffect.buildSpellAbility` (`EarthbendEffect.java:40-44`) sets `ValidTgts$ Land.YouCtrl`
  itself; `resolveTargets` supplies it for `APIEarthbend`. Earthbend re-checks the target on resolution (CR 608.2b).
- **Player-action triggers:** `checkPlayerActionTriggers` runs `ValidPlayer$`-only modes -- `ElementalBend` then
  `Earthbend`/`Airbend` (`Player.triggerElementalBend`, `Player.java:4081-4088`) and `Discover`. `ActivationLimit$`
  lines skip, as in `checkLifeGainedTriggers`. `elementalBendThisTurn` is not kept: Firebend and Waterbend are not
  ported, so "all four this turn" cannot happen.
- **`castWithoutPaying`** (`discovereffect.go`): `CastSpell`'s tail without timing or payment -- stack, permanent
  ability, `SpellCast`, cast and zone-change triggers.
- **Face-down exile:** a heisted card's `Def` is a blank definition (`Card.turnFaceDown`'s FaceDown state), its own kept
  in `faceUpDef`; `Game.Move` turns it face up as it leaves exile.

Decisions: Draft and Heist use `ChooseCardsForEffect` for Java's `chooseSingleCardForZoneChange`; Discover's cast/hand
choice is `ConfirmEffect` (Java's `confirmAction`, "Cast" first). No new `PlayerController` method.

Rebalanced cards: `DraftEffect.java:52-55` swaps a name for its `A-` version when `PaperCard.isUnRebalanced` -- an
edition lists `A-<name>` under `[rebalanced]`. This port has no edition data and asks the DB for `A-<name>`. All nine
spellbook names with an `A-` card (Akki Ronin, Ancestral Katana, Asari Captain, Cauldron Familiar, Eiganjo Exemplar,
Imperial Subduer, Patrician Geist, Peerless Samurai, Shipwreck Sifters) are listed under `[rebalanced]`, so both agree
on every real line.

---

## Effect, ReplaceEffect, ReplaceDamage, ReplaceSplitDamage, ReplaceToken, ReplaceCounter and ReplaceMana land

Java: `forge-game/src/main/java/forge/game/ability/effects/EffectEffect.java` (`resolve`), `SpellAbilityEffect.java`
(`createEffect`, `checkValidDuration`, `addUntilCommand`, `addForgetOnMovedTrigger`, `addExileOnMovedTrigger`,
`addForgetOnCastTrigger`), `GameAction.java` (`exileEffect`, `changeZone`'s immutable branch),
`Replace{,Damage,SplitDamage,Token,Counter,Mana}Effect.java`, `replacement/ReplaceDamage.java` (`DamageTarget$`),
`replacement/ReplacementHandler.java` (split-damage bookkeeping).

**Effect cards.** `effectEffect` (`effecteffect.go`) builds, per `EffectOwner$` player (default the activator), a card
in that player's Command zone with `Card.IsEffect` set. Its definition is built at resolution from compiled abilities
only: `StaticAbilities$`, `Triggers$` and `ReplacementEffects$` name SVars that `internal/carddb/compile` now follows
for the `Effect` API (`effectTraitKeys`; `StaticAbilities$` SVars compile as `StaticEffect`), and the card's amounts are
the host's SVars (`createEffect`'s `eff.setSVars(sa.getSVars())`). No script text is read at resolution (PORT-2). It
remembers `RememberObjects$` (split on `" & "`), imprints `ImprintCards$`, copies the host's choices
(`Memory.copyChoicesFrom`: colors, cards, player, direction, both types, named cards, number) and takes
`SetChosenNumber$`. `Name$` names it (default `<host>'s Effect`); `Unique$` skips a player already holding one of that
name.

**Active zone.** An effect card's traits are active in the Command zone alone, whatever `TriggerZones$`/`ActiveZones$`
say (`setActiveZone(EnumSet.of(ZoneType.Command))`):

| Walker                                                                                                                             | Change                                                                 |
| ---------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Every `continuous.go` applier, `cantBlockBy`, `ignoreLegendRule`, and every battlefield-walking trigger check in `trigger.go` (19) | Host list is `Game.traitHosts`: battlefield, then Command effect cards |
| `phaseTriggerZoneMatches` (Phase, AttackersDeclared, Drawn, LifeGained, LandPlayed)                                                | Effect card matches the Command zone only                              |
| `hostInActiveZones` (every replacement dispatch)                                                                                   | Effect card matches the Command zone only                              |

**Lifetime.** `effectLifetime` on the card records the `Duration$` and the move watch; `Game.Clone` copies it by value.

| `Duration$`                                                   | Ends                                                                                                                                                     |
| ------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| absent, `EndOfTurn`, `UntilEndOfTurn` (Java's default branch) | Next cleanup step (`endEffectsAtCleanup`)                                                                                                                |
| `Permanent`                                                   | Never by duration                                                                                                                                        |
| `UntilYourNextTurn`                                           | As its controller's next turn begins (`endEffectsAtTurnStart`, `AdvancePhase`)                                                                           |
| `UntilTheEndOfYourNextTurn`                                   | Cleanup of the controller's next turn; one made during their turn survives that one                                                                      |
| `UntilEndOfCombat`                                            | `endCombat` (`endEffectsAtEndOfCombat`)                                                                                                                  |
| `UntilHostLeavesPlay`, `UntilHostLeavesPlayOrEOT`             | Host leaves the battlefield (and, for the second, next cleanup); no effect if host is neither on the battlefield nor on the stack (`checkValidDuration`) |

`ExileOnMoved$ <zones>` ends the effect when a remembered card leaves one of the zones. `ForgetOnMoved$ <zones>` forgets
a remembered card that leaves one of them for anywhere but the stack or exile, that is exiled from anywhere, or --
unless the value is `Stack` or `ForgetOnCast$ False` -- that is cast; an effect left remembering no card ends. The three
Java triggers fold into one check in `Game.Move`/`MoveToLibraryTop` (`effectCardsSeeMove`). Not modeled: the Exiled
trigger's `ValidCause$ SpellAbility.!EffectSourceAbility` exception; the corpus's chains exile before they make the
effect, so it has nothing to apply to.

An ended effect leaves the game: `exileEffect` removes it from the Command zone and parks it in `None`, no zone-change
event -- `GameAction.changeZone` removes an immutable card moving to exile and adds it nowhere. Consequence for
fixtures: a live effect card dumps into `<player>command=` under a name the DB does not hold, so a scenario's
`expect.state` can only be taken once every effect has ended (`effect-card-prevents-combat-damage-this-turn` runs to
cleanup).

**Rejected (fail closed).** `Abilities$`, `RememberSpell$`, `RememberLKI$`, `RememberKeywords$`/`SharedKeywordsZone$`/
`SharedRestrictions$`, `ForgetCounter$`, `ForgetOnPhasedIn$`, `ExileOnCounter$`, `NoteCounterDefined$`, `ExileOnLost$`,
`Boon$` (the one-shot removal after its first trigger), `AtEOT$`, `ImprintOnHost$`, `Adventure$`, `Condition$`/
`ConditionDefined$`/`ConditionZone$`, and every other `Duration$` (`AsLongAsControl`, `UntilTheEndOfYourNextUntap`,
`UntilYourNextEndStep`, `UntilUntaps`, `AsLongAsInPlay`, `UntilYourNextUpkeep`, `ThisTurnAndNextTurn`, ...). A trait an
effect card carries is still subject to its own dispatch's gaps: `Mode$ Continuous | MayPlay$` (the largest static shape
on effect cards) stays skipped by `AffectedZone$`, as it is on any card.

**Upstream fix.** Following `StaticAbilities$` found `peace_talks.txt:4` naming `STCantTargetPlayer`, an SVar the card
never defines; `EffectEffect.resolve` drops the null static silently. Fixed in the fork, logged in
`porting/upstream-patches.md` and `porting/card-script-defects.md` (PORT-8).

**Replace\* effects.** The six APIs are registry effects (`replaceeffect.go`) editing `Ability.replacing`, a
`replacementEvent` (Java's `OriginalParams` map as a struct: result, `amountName` + amount, affected, the split-off
redirect, counter type, produced mana). The replacement dispatches run a `ReplaceWith$` naming one of them through
`Game.runReplaceWith`, which calls the same effect the Registry holds, with the event attached -- one mechanism, not a
hand-run copy. The former hand-run `applyDamageReplaceDamage`/`applyDamageReplaceEffect`/`applyGainLifeReplaceEffect`
are gone. A `ReplaceWith$` naming `SubAbility$`, or a param its effect rejects, still skips the line whole, event
untouched. On the stack (no event) `ReplaceEffect` fails, the other five do nothing
(`if (!sa.isReplacementAbility()) return;`).

| API                  | Resolves                                                                                                                                                              | Rejected                                                            |
| -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| `ReplaceEffect`      | Default `VarType$` (amount): `VarName$` must be the event's (`DamageAmount`, `LifeGained`, ...); `VarValue$` a literal, `ReplaceCount$` expression or any host amount | `VarType$` Card/Player/GameEntity/Map/CardSet/PlanarDice, `VarKey$` |
| `ReplaceDamage`      | Prevent `Amount$` (default 1); a `Number$` SVar is a depleting shield written back to the host, an effect card exiled once spent; `PreventedDamage` SVar set          | `DivideShield$`                                                     |
| `ReplaceSplitDamage` | `VarName$` (default 1) of the damage goes to `DamageTarget$`, dealt by the caller as its own event (`dealRedirectedDamage`); a spent effect card is exiled            | --                                                                  |
| `ReplaceToken`       | `Type$ Amount`, `Amount$` Twice/Thrice/HalfUp/HalfDown/Plus.N/Minus.N (default Twice)                                                                                 | `Type$` AddToken/ReplaceToken/ReplaceController                     |
| `ReplaceCounter`     | `Amount$` over `ReplaceCount$CounterNum`; `ValidCounterType$`; `ChooseCounter$` moot (one source per placement)                                                       | `ValidSource$`                                                      |
| `ReplaceMana`        | `ReplaceMana$` (a symbol or `Any`), `ReplaceType$`, `ReplaceColor$` (+`ReplaceOnly$`, `Chosen`), `ReplaceAmount$`                                                     | --                                                                  |

**New replacement events** (`replacement.go`, all through `eachReplacement`: Battlefield and Command hosts, first match
applies, the file's CR 616 simplification):

| Event         | Wired at                                                     | Checks                                                                                                                      | Skips the line                           |
| ------------- | ------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------- |
| `AddCounter`  | `putCounterEffect`, per card and per player                  | `ValidCard$`, `ValidPlayer$`, `ValidObject$`, `ValidCounterType$`, `ValidSource$` (placer), `EffectOnly$`                   | `ValidCause$`, anything else             |
| `CreateToken` | `tokenEffect`, per owner and script                          | `ValidToken$` (against the unentered token), `ValidPlayer$` (creator), `EffectOnly$`                                        | `Optional$`, `Layer$`                    |
| `ProduceMana` | `TapLandForMana`, `ActivateManaAbility` (`addProducedMana`)  | `ValidCard$` (the source), `ValidActivator$`                                                                                | `ManaAmount$`, `ValidSA$`                |
| `DamageDone`  | existing; now also `DamageTarget$` (`damageRedirectAllowed`) | the can't-be-redirected keyword, defined players in the game, defined cards creature/planeswalker/battle on the battlefield | the cause's `NoRedirection$` is not seen |

Every other counter, token or mana site (`Counters.Add` in costs and keyword effects, `Amass`/`Investigate`/`Incubate`
tokens, `Mana` effects) does not consult these events yet.

Tests: `effectcard_test.go`, `replaceeffects_test.go`; scenario `effect-card-prevents-combat-damage-this-turn` (Haze
Frog).
