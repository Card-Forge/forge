# Port Log — Game State: Trigger Modes

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

Later `Mode$` additions and trigger gates.

## `Mode$ BecomesTarget` lands, and targeting gets a second real event

CR 115/603.3's own "whenever CARDNAME becomes the target of a spell or ability" -- `TriggerBecomesTarget`, ported at the
shape this port's own targeting mechanism
([`## Targeting itself lands`](targeting-and-chaining.md#targeting-itself-lands) -- written earlier in this file, but
landed earlier in the port) can reach. Java's own trigger point is `MagicStack.add`, right after a spell's own targets
are chosen and right before `SpellCastOrCopy` fires: "Run BecomesTarget triggers... Create a new object, since the
triggers aren't happening right away," walking every distinct object across every `TargetChoices` the cast collected (a
`Set<GameObject>` dedup, "so Becomes targets don't trigger for things like Seeds of Strength" -- one spell hitting the
same object twice from two different sub-targets fires the trigger once, not twice).

This port has exactly two places that finish choosing a target for anything today, and both are where
`checkBecomesTargetTriggers` (trigger.go, new) is called from: `pushTriggeredAbilities` (trigger.go), right after each
`PushAbility` -- a triggered ability's own `a.Targets`, freshly set by `resolveTargets` two lines earlier in the same
function -- and `castAura` (castspell.go), for an Aura's own single cast-time attach target. A targeted Instant/Sorcery
would be a third real site (CR 601.2c's own general spell-targeting case), but `CastSpell` only casts a permanent or an
Aura so far
([`## Casting a spell needed the stack for real, for the first time`](mana-and-casting.md#casting-a-spell-needed-the-stack-for-real-for-the-first-time))
-- the identical "mechanism now, content later" gap `resolveTargets`'s own doc comment already names for exactly this
future caller.

Deduplication is the identical `distinctObjects` set Java's own loop builds, just local to each call rather than
threaded through a second `AbilityKey.Targets`/`BecomesTargetOnce` pass (`BecomesTargetOnce`'s own real corpus role -- a
single trigger per spell naming EVERY distinct target it chose, rather than once per target -- has 0 real lines combined
with a resolvable shape this port could tell apart from `BecomesTarget` itself, so it is not built at all, the identical
"zero real lines, stays dormant" bucket `EnduringStory$`/`Monarch$` already sit in elsewhere in this file).

`ValidTarget$` (present on all 118 real lines) is matched with `attackedTargetMatches` (trigger.go,
`checkAttackersDeclaredTrigger`'s own dispatch for `AttackedTarget$`, reused outright): the identical
one-entity-of-either-kind problem a becomes-target event has, since a real target is a player
(`ValidTarget$ You,Permanent.YouCtrl+inZoneBattlefield`, Rayne, Academy Chancellor's own real line) or a card, never
both spelled out as one nested spec. `Card.AttachedBy`/`EnchantedBy` (Ice Cage's own real "enchanted creature becomes
the target of a spell or ability, destroy CARDNAME") needed no new code at all -- `Matches`' own existing
`EnchantedBy`/`AttachedBy` case ([`## The card's mutable parts`](../game-state.md#the-cards-mutable-parts)'s neighbor
section, `valid.go`) already reads its own `source` argument as "the object being checked for being attached to," and
`host.ID` -- the Aura itself -- is exactly that for a trigger living on the Aura naming its own enchanted creature.

`FirstTime$` (Glyph Keeper's own real "for the first time each turn, counter it") reads a new
`Card.BecameTargetThisTurn` (card.go) -- `Card.AttacksThisTurn`'s own boolean sibling, not its counting one: Java's own
`hasBecomeTargetThisTurn()` is `!targetedFromThisTurn.isEmpty()`, a per-target `Player` set, but no real `FirstTime$`
line on a `Mode$ BecomesTarget` trigger ever asks WHICH players are in that set, only whether it is empty -- the
identical simplification this port already made for `Player`-shaped state it tracks nowhere else (`matchesPlayerBase`'s
own "no team support" doc comment). Set unconditionally the moment a distinct target is checked, before any
`ValidTarget$` match is even attempted -- Java's own `addTargetFromThisTurn` runs in the SAME loop iteration as the
trigger's own `runTrigger` call, both fed by the same "was this the first time" check computed BEFORE either -- matching
`TestBecomesTargetSkipsWhenValidTargetDoesNotMatch`'s own proof that a card becomes a target (the flag flips)
independently of whether any trigger's own restriction happens to fire. Reset every cleanup (`cleanupStep`, turn.go)
alongside `AttacksThisTurn`.

`ValidSource$` resolves too now, through a new `becomesTargetSourceMatches` (trigger.go) -- matched against the
triggering `SpellAbility` itself (`AbilityKey.SourceSA` in Java), not a `Card`, `SpellAbility.isValid`'s own restriction
split (`incR[0]` against `Spell`/`Ability`/`Triggered`/`Activated`/`SpellAbility`, then a `.`-qualified property).
Rather than adding a general Spell/Activated/Triggered kind classifier to `Ability`, this reads the ONLY two ability
kinds this port's own two real call sites can ever produce: `castAura`'s own Aura is always a Spell
(`isSpellSource=true`), and a triggered ability pushed through `pushTriggeredAbilities` is always Java's own
`isTrigger()`/`isAbility()` pair at once (`isSpellSource=false`) -- this port has no activated-ability targeting built
yet, so `Ability` and `Triggered` collapse to the identical "not a Spell" test for now, a real simplification narrower
than Java's own three-way split but exact for every real corpus line either caller can ever reach. `SpellAbility` itself
matches unconditionally (Java's own "match anything" case, `SpellAbility.OppCtrl`'s/`SpellAbility.YouCtrl`'s own 46
combined real lines); `.YouCtrl`/`.OppCtrl` compare the ability's own controller (`sourceController`, threaded through
as a new parameter from each call site -- `pid` at `castAura`, `matches[i].Controller` at `pushTriggeredAbilities`)
against the watching trigger's own host controller, the identical YouCtrl/OppCtrl contract every other property in this
port already has; `.Aura` is trivially true whenever the kind itself is Spell, since this port's only Spell source
reaching here IS an Aura being cast (`castAura`'s own doc comment) -- `Spell.Aura`'s own 5 real lines need no further
check at all. 71 of the corpus's own 77 real lines naming `ValidSource$` resolve; 6 stay unresolved:
silverfur_partisan.txt's/wild_defiance.txt's own real `Instant,Sorcery` (a card-type check neither of this port's two
sources, a Trigger or an Aura, can ever satisfy) and four more real lines combining a kind with a property past
YouCtrl/OppCtrl/Aura (`namedGoblin Artisans`, `numTargets EQ1`, `Land+named...`, `Backup`), each its own further
mechanic, refused by the same fall-through `default: return false` every unrecognized head or property already hits
(GO-7).

101 of the corpus's own 118 real `Mode$ BecomesTarget` lines resolve now (`OptionalDecider$`, 12, every real line "You"
and none also naming `Valiant$`/`ActivationLimit$`/`Static$`, resolves too through `triggerEffectAPI`'s own
`triggerIsOptional`, "`CR 603.3d's own "may" triggered ability`," below). Not resolved, each failing loudly by name
rather than matching unconditionally (PORT-8/GO-7): `Valiant$` (10) -- `Card.isValiant`'s own separate per-activator
"have you not targeted this before" set (`getController().equals(p) && !targetedFromThisTurn.contains(p)`), a different
question than `FirstTime$`'s plain bool can answer even if it wanted to; `Static$` (1) -- its own further mechanic
(goblin_artisans.txt's own `Static$ True` line is a static ability synthesizing a trigger-shaped check, not a real `T:`
line at all, distinct from the `T:`-prefixed corpus-frequency count above).

Ten tests (`becomestarget_test.go`): `TestPushTriggeredAbilitiesFiresBecomesTargetOnCardTarget` and
`TestCastAuraFiresBecomesTargetTriggerOnEnchantedCreature` prove the two real call sites both fire, each isolating the
BecomesTarget-triggered `GainLife`'s own life change from whatever the parent ability itself does (an ETB trigger's own
`DB$ LoseLife | ValidTgts$ Creature.YouCtrl` finds no player among a card-shaped target and does nothing, the identical
isolation technique
[`## SubAbility chaining reaches every effect`](targeting-and-chaining.md#subability-chaining-reaches-every-effect)'s
own tests already use for a different reason); `TestBecomesTargetFirstTimeOnlyFiresOnceEachTurn` proves the flag's own
once-per-turn contract across two separate targeting events; `TestBecomesTargetSkipsWhenValidTargetDoesNotMatch` proves
`BecameTargetThisTurn` still flips even when no trigger's own `ValidTarget$` matches.
`TestBecomesTargetFiresForMatchingSpellAbilityController`/ `TestBecomesTargetSkipsForNonMatchingSpellAbilityController`
prove `SpellAbility.YouCtrl`/`.OppCtrl` both ways;
`TestBecomesTargetFiresForAuraSpellSource`/`TestBecomesTargetSkipsAuraSpellSourceWhenValidSourceRequiresTriggered` prove
the kind check both ways, the second isolating it from any controller comparison at all;
`TestBecomesTargetSkipsWhenSourceKindDoesNotMatch` is the original unresolved-shape regression test, kept and
re-described now that `Spell.OppCtrl` resolves (its own kind mismatch and controller mismatch both independently refuse
the line, so the two tests above are what isolate each reason on its own);
`TestBecomesTargetSkipsUnresolvedValidSourceShape` proves the truly-unrecognized-head case (`Instant,Sorcery`) refuses.
Every new branch was regression-checked by temporarily disabling it and confirming the corresponding test failed with
the expected wrong life total before restoring it -- the whole-dispatch toggle alone flipped all four "fires" tests to
failing, the `.YouCtrl`/`.OppCtrl` toggle flipped `TestBecomesTargetSkipsForNonMatchingSpellAbilityController`, and the
unresolved-head fallback toggle flipped `TestBecomesTargetSkipsUnresolvedValidSourceShape`.

---

## `Trigger.phasesCheck` lands

A general gate every trigger mode carries regardless of what it fires on -- `TriggerHandler.isTriggerActive` checks it
BEFORE `canRunTrigger`/`performTest` even runs, distinct from `CardTraitBase.meetsCommonRequirements`
(`triggerCommonRequirementsMet`, above), a different Java method on a different class entirely, called from INSIDE a
mode's own `performTest` rather than before it. This port had never checked either general gate's own params at all
until now: `triggerPhasesCheck` (trigger.go, new) ports `Trigger.phasesCheck` at the shape the real corpus uses, called
from `triggerEffectAPI` right before `triggerCommonRequirementsMet` -- Java's own ordering, `phasesCheck` gating whether
a trigger is even active before `canRunTrigger` asks anything else.

`Phase$` (19 real `T:` lines outside `Mode$ Phase`'s own dispatch) restricts a trigger of ANY mode to firing only during
the named step(s)/phase(s) -- reusing `phaseTriggerMatches` (`checkPhaseTriggers`'s own dispatch function, above)
generically, since the question is identical either way: does `Phase$` name the current phase. This closes a genuine
naming collision this port had not noticed until reading `Trigger.java` line by line: `Mode$ Phase`'s own
`TriggerPhase.performTest` checks only `ValidPlayer$` -- `Phase$` on a `Mode$ Phase` line is not that mode's own
dispatch param at all, it is THIS SAME general gate, just happening to restrict the one mode whose entire purpose is
firing at a phase boundary. A `Mode$ Phase` line now reaches `triggerPhasesCheck` too (through `triggerEffectAPI`, which
`checkPhaseTriggers` already calls), re-asking the identical question against the identical inputs
`checkPhaseTriggers`'s own explicit `phaseTriggerMatches` call already answered -- redundant, but harmless, and left
alone rather than refactored away: `checkPhaseTriggers` is tested, working code, and the redundancy costs nothing a real
game would ever notice.

`PlayerTurn$` (61 real lines) / `NotPlayerTurn$` (0, ported anyway for symmetry with Java's own
hasParam-not-value-checked contract -- neither key is ever value-sensitive in Java, so a hypothetical
`PlayerTurn$ False` would still restrict positively) restrict to the trigger's own host controller's turn, or explicitly
not it -- `Trigger.java`'s own `isPlayerTurn(hostController)` check, ported directly. sentinel_tower.txt's own real
"Whenever an instant or sorcery spell is cast during your turn, CARDNAME deals 1 damage to each opponent" is
`PlayerTurn$ True` on a `Mode$ SpellCast` line -- one of 43 real lines across six already-built modes (`SpellCast` 12+2,
`ChangesZone` 9+11, `LifeGained` 5, `Taps` 2, `Discarded` 1, `Drawn` 1) that fired **unconditionally** until this
landed: a wrong answer, not a coverage gap (PORT-8/GO-7) -- this port had simply never read either key before.
`OpponentTurn$` (23, `SpellCast`/`Drawn`) is `Player.isOpponentOf`'s own question, which collapses to the identical
check `NotPlayerTurn$` already makes in this port's own no-team model (`matchesPlayerBase`'s own doc comment: with no
teams, "not my turn" and "my opponent's turn" are the same fact).

`FirstCombat$` (6, `Attacks`/`AttackersDeclared`, both already built) is `PhaseHandler.isFirstCombat`'s own
`nCombatsThisTurn==1`: `Game.combatsThisTurn` counts combat phases begun this turn (each `CombatBegin`, reset as the
turn ends), and an `AddPhase` extra combat makes it two, so the second combat's triggers skip (`isFirstCombat`, turn.go;
`ConditionFirstCombat$` reads the same). A fixture set straight into a combat step through `SetTurnState` has begun
none, and counts as the first -- the combat it is in is. The param's value is not read, as Java does not read it; 0 real
lines write `FirstCombat$ False`.

Not resolved, each skipping the whole line rather than guessing (GO-7): `FirstUpkeep$` (1) / `FirstUpkeepThisGame$` (2,
both `Mode$ Phase` only) -- `PhaseHandler.isFirstUpkeep`/`isFirstUpkeepThisGame`'s own per-turn/per-game upkeep-step
counters; unlike `FirstCombat$`, `FirstUpkeepThisGame$` genuinely can be false on any turn after the first even without
a new mechanism (an ordinary game has one upkeep every turn, but only the very first one is the game's own first), and
this port tracks neither; `TurnCount$` (0 real lines, dormant).

`checkPhaseTriggers`'s own `hasAnyParam` pre-filter no longer names `FirstUpkeep$`/`FirstUpkeepThisGame$`/
`FirstCombat$`/`TurnCount$` -- `triggerPhasesCheck` resolves (or correctly skips) all four generically now, the
identical "remove the now-redundant special case" fix this file's own trigger-firing section already made once for
`IsPresent$`/`CheckSVar$` (above).

Eight new tests (`triggerphases_test.go`), all built on `spellCastWatcherDefExtra` (`spellCastWatcherDef`'s own sibling,
carrying whatever extra general param a test needs): `TestSpellCastFiresTriggerWhenPlayerTurnMatches`/
`SkipsTriggerWhenPlayerTurnDoesNotMatch`, `...WhenOpponentTurnMatches`/`SkipsTriggerWhenOpponentTurnDoesNotMatch` and
`...WhenPhaseMatches`/`SkipsTriggerWhenPhaseDoesNotMatch` each prove one restriction fires when it should and stays
silent when it should not -- the three `Skips` tests are the regression proof, each confirmed to fail exactly as
expected when `triggerPhasesCheck`'s own call from `triggerEffectAPI` was temporarily removed and the suite rerun, then
confirmed to pass again once restored. `TestAttacksFiresFirstCombatTrigger` proves `FirstCombat$ True` resolves through
a real `DeclareCombatAttackers` call (raph_leo_sibling_rivals.txt's own real shape).

---

## `Mode$ Untaps` lands

CR 502.3/603's own "whenever CARDNAME becomes untapped" -- `TriggerUntaps`, `Taps`'s own mirror image at the opposite
end of the identical event (a card's own `Tapped` field flipping). Java's own trigger point is `Card.untap()`: an early
`if (!tapped) return false` before anything else runs (a card already untapped generates no event at all), then
`ReplacementType.Untap`'s own "doesn't untap" check (`untapBlocked`,
[`## Replacement effects`](replacement.md#replacement-effects-entering-the-battlefield-tapped) -- landed well before
this trigger did), then `TriggerType.Untaps` fires, then `tapped` is actually cleared.

`untapStep` (turn.go) already had the middle piece (`untapBlocked`) from an earlier chunk; this one adds the first and
third. A new `wasTapped := c.Tapped` local, read before `untapBlocked`'s own check, stands in for Java's own early
return -- `checkUntapsTriggers` (trigger.go, new) is only ever called when `wasTapped` was true AND `untapBlocked` did
not block it, the exact two-part gate `Card.untap()` itself has. `untapStep` gained a `controller PlayerController`
parameter for this (`beginPhase`'s own call site, turn.go, already had one to hand it -- the identical "every
state-based-action-adjacent call site already carries a controller by now" pattern every earlier controller-threading
chunk this port has done already relied on).

`checkUntapsTriggers` is structurally `checkTapsTriggers`' own exact twin: `TriggerUntaps.performTest` never
special-cases its own host's trigger either (unlike `checkDiesTriggers`/`checkETBTriggers`, which need a separate "own"
and "other" half), so one battlefield walk covers both a card's own "Inspired" trigger (`ValidCard$ Card.Self`, the
corpus's own dominant real shape -- untapping during your own untap step and paying a cost for an effect) and
mesmeric_orb.txt's own bare "whenever a permanent becomes untapped" (`ValidCard$ Card`, matching anyone's). `ValidCard$`
absent is a pass, the identical contract `checkTapsTriggers` already has for it.

30 of the corpus's own 30 real `Mode$ Untaps` lines resolve now. `Phase$`/`CheckSVar$` (1 each) fold in for free through
`triggerPhasesCheck`/`triggerCommonRequirementsMet` (`triggerEffectAPI`'s own two general gates, both landed earlier
this session) -- `checkUntapsTriggers` needed no code of its own for either. `Secondary$` (1) is a pure display flag
(`CardTraitBase.isSecondary`, consulted only by `Card.java`'s own rules-text generation to avoid printing a duplicate
ability line, never by any `performTest`/gating path anywhere in Java) -- this port already ignores it everywhere else
for the identical reason, so `checkUntapsTriggers` does too, by simply never reading it. `OptionalDecider$` (3, every
real line "You") resolves too now, through `triggerEffectAPI`'s own `triggerIsOptional`
("`CR 603.3d's own "may" triggered ability`," below).

Six tests (`untaps_test.go`), all built on `untapsCreatureDef` (`becomestarget_test.go`'s own
inert-Execute$-for-isolation pattern, reused): `TestStartTurnFiresUntapsTriggerForSelf` proves the Inspired shape;
`TestStartTurnFiresUntapsTriggerForOtherPermanent` proves the bare-`Card` shape fires for a SEPARATE permanent untapping
(a plain nil-`Def` card stands in for the untapping permanent itself, since `baseMatches`'s own bare `"Card"` case needs
no `Type()` at all to answer true); `TestStartTurnSkipsUntapsTriggerForAlreadyUntappedCard` is the `wasTapped` guard's
own regression proof, asserting `StackLen() == 0` rather than just an unchanged life total, since nothing ever resolves
an ability this port never pushes in the first place.

`TestStartTurnFiresUntapsTriggerNamingOptionalDeciderWhenConfirmed` and
`TestStartTurnSkipsUntapsTriggerNamingOptionalDeciderWhenDeclined` prove `OptionalDecider$ You` both ways against this
mode's own real shape: a confirmed `ConfirmOptionalTrigger` runs the body, a declined one leaves life unchanged.
`TestStartTurnSkipsUntapsTriggerNamingUnresolvedOptionalDecider` proves an `OptionalDecider$` value this port cannot
resolve (`TriggeredCardController`) skips the whole line before ever asking -- if it asked, the scripted controller's
own empty queue would panic, so a clean run proves the question was never posed at all. The general mechanism's own
section below has its own regression-check account: a `Registry.Resolve` toggle, plus a `triggerIsOptional` toggle
proving the unresolved-decider skip specifically.

---

## `Phase`'s own qualified `ValidPlayer$`: `EnchantedController` and `descended`

`matchesPlayerProperty` (valid.go), `matchesPlayerSpec`'s own property half, gains two more real cases -- both reused
for free by every one of its nine existing callers (`checkSpellCastTriggers`'s own `matchesActivatingPlayer`,
`checkDamageDoneTriggersToPlayer`, `checkTapsForManaTriggers`, `checkPhaseTriggers`, `checkAttackersDeclaredTrigger`,
`attackedTargetMatches`, `checkDrawnTriggers`, `checkLifeGainedTriggers` in trigger.go; `applyOneContinuousRules` in
continuous.go; `targetCandidates` in targeting.go; `damagePreventedPlayer`/`drawPreventionMatches`/
`gainLifePreventionMatches` in replacement.go), since both `matchesPlayerSpec` and `matchesPlayerProperty` gained a
`source CardID` parameter -- the ability's own host card, threaded alongside the controller `PlayerID` they already took
-- purely mechanical plumbing through every call site, not a behavior change for any of the nine.

`EnchantedController` (`Player.EnchantedController` in Java's own `PlayerProperty.playerHasProperty`) reads
`source.getEnchantingCard()`, ported as `source.AttachedTo()` (`Card.go`'s own attachment link, the identical one Layer
2's own `GainControl$ You | Affected$ Card.EnchantedBy` already reads the other direction, item 27) -- the card the
trigger's own host (an Aura) is attached to -- then checks whether the candidate player controls it.
righteous_authority.txt's own real "at the beginning of the draw step of enchanted creature's controller, that player
draws an additional card" is the corpus's own dominant shape: 34 of `Mode$ Phase`'s own real qualified `ValidPlayer$`
lines resolve this way (a 35th, on `Mode$ AttackerUnblocked`, is moot -- that mode is not built at all).

`descended` (`Player.descended`/`getDescended()` in Java, incremented in `Zone.add`'s own `!rollback` branch whenever a
permanent, non-token card enters a graveyard from anywhere) is a new `Player.DescendedThisTurn bool` (player.go) --
Java's own field is a per-turn count, but every real corpus line only ever asks `< 1`, so a bool is enough. Set in
`Game.Move` (game.go): `isPermanent := c.Type().IsPermanent()` is read at the top of `Move`, before the
battlefield-leaving branch clears anything, and `kind == Graveyard && isPermanent` sets `DescendedThisTurn` on `owner`
right after the existing zone-transition switch -- every real `Move`-to-`Graveyard` call site in this port already
passes `owner` as the moving card's own `Owner` field, so this needed no new parameter threading of its own. Java's own
check also excludes a token, and so does this one (`!c.IsToken`). Reset for every player at `cleanupStep` (turn.go)
alongside `LandsPlayed`/`CardsDrawnThisTurn`. 10 of the corpus's own real `Mode$ Phase` lines resolve,
ruin_lurker_bat.txt's own "at the beginning of your end step, if you descended this turn" among them.

`Player.EnchantedBy` (14 real lines) and `Opponent.EnchantedBy` (2) resolve from `Player.Attachments` (an Aura
enchanting a player directly, CR 303.4h, [`player-attachment.md`](player-attachment.md)). `Player.Chosen` (3) needs a
"choose a player" ability this port does not have. `Player.isMonarch` (1) reads `Game.monarch`
(`PlayerProperty.java:85-92`, `matchesPlayerSpec`), as Layer 2's `GainControl$ Player.isMonarch` does
(`applyOneContinuousControl`, continuous.go).

While researching this chunk's own real corpus counts, two stale figures elsewhere in this port's own comments turned
out to be wrong and were corrected in place (DOC-16): `checkPhaseTriggers`'s own doc comment (trigger.go) attributed its
`Condition$` skip -- `SpellAbilityCondition`'s own separate switch, a key this pre-filter genuinely never matches on a
real `Mode$ Phase` line today -- the count "65" that actually belongs to a wholly different key,
`WerewolfTransformCondition$`/`WerewolfUntransformCondition$` (Innistrad's own day/night mechanic, unresolved for
`CardTraitBase.meetsCommonRequirements`'s own reason, "`Trigger.phasesCheck` lands," above); a real corpus check
confirms 0 lines carry the bare `Condition$` key on a `Mode$ Phase` line, and only 6 carry it at all corpus-wide, none
reachable through this port's own static trigger walk.

Eight new tests, all in `triggerphases_test.go`: `TestAdvancePhaseFiresPhaseTriggerWhenEnchantedControllerMatches`/
`TestAdvancePhaseSkipsPhaseTriggerWhenEnchantedControllerDoesNotMatch` (an Aura's own controller and the enchanted
creature's controller deliberately different players, so the test isolates "did the trigger fire" from "who the Aura's
own `Execute$` benefits"); `TestMoveSetsDescendedThisTurnForPermanentCardToGraveyard`/
`TestMoveDoesNotSetDescendedThisTurnForNonPermanentCardToGraveyard` (the `isPermanent` branch, an Instant proving the
negative);
`TestAdvancePhaseFiresPhaseTriggerWhenDescendedThisTurn`/`TestAdvancePhaseSkipsPhaseTriggerWhenNotDescendedThisTurn`;
`TestCleanupStepResetsDescendedThisTurn`. Every fire-side test was regression-checked by temporarily removing the
corresponding `matchesPlayerProperty` case or `Game.Move` hook and confirming the suite fails exactly as expected before
restoring it.

`enginelint.json`'s own `"valid"` group gained `"player"` in its allow-list -- `matchesPlayerProperty` reads
`g.Player(candidate).DescendedThisTurn` directly now, a type declared in player.go valid.go had never referenced before.

---

## `Mode$ LandPlayed` lands

CR 305/603.5's own "whenever a player plays a land" -- `TriggerLandPlayed`, ported at the one real fire site this port
has, `PlayLand` (land.go). Java's own real ordering, `Player.playLand`: `moveTo` fires ETB triggers internally as part
of the zone change, then the explicit `runTrigger(TriggerType.LandPlayed, ...)` call, then `addLandPlayedThisTurn()` --
`checkLandPlayedTriggers` (trigger.go, new) is called right after `checkETBTriggers`, and `PlayLand`'s own
`LandsPlayed++` was moved to run last to match, since `NotFirstLand$` (below) needs to read the count of lands played
strictly BEFORE this one, the same value Java's own `performTest` sees.

`ValidCard$` is matched the ordinary way (`Matches`). `Origin$` resolves through `hasZoneOrAny` (ETB triggers' own
dispatch, [`## Last-known-information`](targeting-and-chaining.md#last-known-information-lands) -- reused outright, no
new code) against the land's own origin zone -- this port's own `PlayLand` only ever moves a card out of Hand
(`c.Zone != Hand` is one of its own rejection conditions), no `MayPlay$` permission to play from elsewhere yet (Layer
8's own remaining gap, `MayLookAt$`/`MayPlay$`, game-state.md's own "Thin or missing" account), so 8 of the corpus's 9
real non-`Static$` `Origin$` lines (all naming Exile, or `Ante,Command,Exile,Graveyard,Library`, "from anywhere other
than your hand") never actually satisfy it today -- `hasZoneOrAny` itself is correct, the ability it gates is simply
unreachable given this port's own current scope, the identical "mechanically correct, presently unreachable" gap
`DB$ ReplaceDamage`'s own `hedron_field_purists.txt` lines already have (`#### DB$ ReplaceDamage`, above). The 9th,
Undying Vengeance's own real `Origin$ Hand` line, fires normally.

`NotFirstLand$` (1, a bare presence check -- `TriggerLandPlayed.java` never reads its own value) resolves through a new
pre-increment read of `Player.LandsPlayed` (player.go): `LandsPlayed < 1` fails the trigger on the very first land of
the turn (the count is still 0 at that point) and passes on every land after (the count already reflects every earlier
land this turn). `ValidActivatingPlayer$` (1, "You") resolves through `matchesActivatingPlayer` (trigger firing, above
-- reused outright) against `player`, the land-playing player threaded through as a new explicit parameter
(`checkLifeGainedTriggers`'s own `gainer PlayerID` precedent). `IsPresent$` (3) resolves generically through
`triggerEffectAPI`'s own `triggerCommonRequirementsMet` fold-in
([`## CardTraitBase.meetsCommonRequirements`](triggers.md#cardtraitbasemeetscommonrequirements-the-one-gate-every-trigger-mode-shares))
-- `checkLandPlayedTriggers` needed no code of its own for it.

38 of the corpus's own 42 real `T:Mode$ LandPlayed` lines resolve now. `Static$`/`ValidSA$` (5/4, 2 lines naming both --
"Once during each of your turns, you may play a historic land or cast a historic permanent spell from your graveyard"
among them) skip via `hasAnyParam`, the identical "whole line, not a guess" contract every other unresolved-shape gap in
this port already has: `Static$` marks a trigger ability that resolves without going on the stack at all, a mechanism
this port's own `pushTriggeredAbilities` does not model (every trigger this port fires goes on the stack the same way);
`ValidSA$` (always `SpellAbility.MayPlaySource` here) matches a `SpellAbility`, an object `Matches` cannot evaluate.
`OptionalDecider$` (3, every real line "You") resolves too now, through `triggerEffectAPI`'s own `triggerIsOptional`
("`CR 603.3d's own "may" triggered ability`," below) -- `checkLandPlayedTriggers`'s own `hasAnyParam` never named this
key at all, so search_the_city.txt's/jokulmorder.txt's/burgeoning.txt's own real "you may..." lines were firing
unconditionally before this, a real correctness fix (PORT-8/GO-7) rather than only a new resolution.

Eleven new tests (`trigger_test.go`): `TestPlayLandFiresLandPlayedTrigger`/
`TestPlayLandSkipsLandPlayedTriggerForNonMatchingValidCard` prove `ValidCard$` both ways;
`TestPlayLandFiresLandPlayedTriggerWithMatchingOrigin`/`TestPlayLandSkipsLandPlayedTriggerWithMismatchedOrigin` prove
`Origin$` both ways; `TestPlayLandSkipsLandPlayedTriggerOnFirstLandWithNotFirstLand`/
`TestPlayLandFiresLandPlayedTriggerOnSecondLandWithNotFirstLand` prove `NotFirstLand$` across two lands played the same
turn (`AdjustLandPlays$ Unlimited` lifting the per-turn limit, `land_test.go`'s own existing helper reused);
`TestPlayLandFiresLandPlayedTriggerForMatchingActivatingPlayer`/
`TestPlayLandSkipsLandPlayedTriggerForNonMatchingActivatingPlayer` prove `ValidActivatingPlayer$` both ways;
`TestPlayLandSkipsLandPlayedTriggerNamingStaticAndValidSA` proves the real combined `Static$`/`ValidSA$` shape refuses
outright; `TestPlayLandFiresLandPlayedTriggerNamingOptionalDeciderWhenConfirmed`/
`TestPlayLandSkipsLandPlayedTriggerNamingOptionalDeciderWhenDeclined` prove `OptionalDecider$ You` both ways against
burgeoning.txt's own real shape. Every new gate was regression-checked by temporarily disabling it and confirming the
corresponding test failed with the expected wrong `StackLen()`/hand count before restoring it -- including the call site
itself (`checkLandPlayedTriggers` commented out of `PlayLand`), which every "fires" test caught and every "skips" test
correctly stayed green through.

---

## Mode$ ChangesZoneAll lands, CR 603.6d's own batched trigger

`TriggerChangesZoneAll.performTest` is `Mode$ ChangesZone`'s own batched sibling: rather than firing once per card the
way the ordinary Dies/ETB triggers do, it fires once for a whole GROUP of cards that changed zones together in one game
action -- the real reason a board wipe's own "whenever one or more creatures you control die" trigger asks `Amount$`
(how many died) rather than firing five separate times for five simultaneous deaths. Java's own mechanism is
`CardZoneTable`: any `moveTo` call during one ability's resolution (or one SBA pass) accumulates into a shared table,
consulted once at the very end via `zoneMovements.triggerChangesZoneAll(game, sa)`.

This port does not build a general `CardZoneTable` threaded through every mover in the engine -- that would touch every
multi-card `Move` loop this port has, a disproportionately large refactor for a trigger mode whose own real corpus lines
split fairly evenly across several different real-world "batch" shapes. Instead, `checkChangesZoneAllTriggers`
(trigger.go) takes the already-gathered batch directly: `cards []CardID`, plus one shared `origin`/`destination` pair
rather than a per-card table -- every call site moves its whole batch through one uniform zone pair (a token batch
enters from `ZoneType` `None`, Java's `triggerList.put(ZoneType.None, ...)`), so this loses nothing observable yet. A
future call site mixing origins within a single batch would need a richer per-card table, not built.

Two real call sites feed it: `sacrificeCards` (sacrificeeffect.go) --
`SacrificeEffect.java`'s/`SacrificeAllEffect.java`'s own trailing `zoneMovements.triggerChangesZoneAll(game, sa)` call,
ported directly, so a plain `Sacrifice` fires it with a one-card batch and `SacrificeAll` with however many it actually
sacrificed -- and `destroyLethalToughness`/ `destroyDamagedCreatures` (action.go, CR 704.5f/CR 704.5g+h's own
simultaneous SBA sweeps): each already collects its own `dead []CardID` before moving any of them
([`## Not ported yet`](../game-state.md#not-ported-yet)'s own note that every SBA in this file collects candidates
before `Move` runs), so calling the new dispatch once after each sweep's own move-and-trigger loop was the whole change.

Every card in a batch has already left the battlefield by the time `checkChangesZoneAllTriggers` runs (every real call
site moves first, checks second), so unlike `checkSacrificedTriggers` -- which runs _before_ the move, and needs no
separate own-half walk for exactly that reason -- there is no own-half/other-half split to get wrong here either, for
the mirror-image reason: a card that was itself part of the batch is no longer on the battlefield to be asked about its
own trigger, `otherDiesTriggerMatches`'s own doc comment gives the identical reasoning. A single walk over every
remaining battlefield permanent, watching for the batch, is the whole dispatch.

`Destination$`/`Origin$` resolve through `hasZoneOrAny` (trigger.go), reused outright from the ETB/Dies dispatch built
for the identical params on `Mode$ ChangesZone` itself. `ValidCards$` (`changesZoneAllMatchingCards`, new) matches each
card in the batch against `g.LKI(id)` when a snapshot exists rather than the card's live state -- the identical "look
back in time" `checkDiesTriggers` already needs for a `Destination$ Graveyard` line to see the card's own pre-move
power/toughness/type/keywords/counters rather than the printed-only state `Move` has already reset it to.
`PlayerTurn$`/`OptionalDecider$`/the whole `IsPresent$`/`CheckSVar$`/... family all resolve too, but generically,
through `triggerEffectAPI`'s own shared gate, the identical free ride every other trigger mode reaching that chokepoint
already gets.

118 of the corpus's own 126 real `T:Mode$ ChangesZoneAll` lines resolve (`ActivationLimit$`, 41, through
[Trigger activation limits](#trigger-activation-limits)). Not resolved: `ValidCause$` (4) -- a `SpellAbility`, not a
`Card`, `Matches` cannot evaluate one; `NoResolvingCheck$`/`InvertValidCause$` (1 each) -- each unclear semantics, not
worth guessing at from one real line; `FirstTime$` (1) -- `CardUtil.getThisTurnEntered`, a further "already entered
earlier this turn" mechanic this port does not build. A trigger carrying any of these six is skipped entirely, not fired
unconditionally (GO-7).

`destroyLethalToughness` and `destroyDamagedCreatures` each fire their own separate `ChangesZoneAll` batch: a real,
narrow gap against CR 704.3's own "all applicable state-based actions are performed simultaneously as a single event" --
two creatures, one killed by each of this port's own two separate CR 704.5 functions in the identical
`CheckStateBasedActions` call, fire two batches rather than one shared one. This port splits CR 704.5 across one
function per clause rather than Java's single combined pass (`CheckStateBasedActions`'s own doc comment already notes
the split), and unifying the two into one shared batch would mean threading a table through `CheckStateBasedActions`
itself rather than each SBA function owning its own -- not built, and not observable against a corpus with no real card
whose own `ChangesZoneAll` line cares which SBA clause killed which creature, only that one or more matching creatures
died at all.

5 new tests (`changeszoneall_test.go`) drive the dispatch through the real cast-and-resolve pipeline and through
`CheckStateBasedActions` directly, `checkChangesZoneAllTriggers` itself being unexported (TEST-1): a `SacrificeAll`
sacrificing two creatures fires the trigger once (life +5, not +10, proving "once for the batch" rather than "once per
card" without needing `Amount$`, which this port does not resolve), `ValidCards$` filtering out a batch with no matching
card, `Destination$` rejecting a batch whose real destination does not match, two creatures reduced to zero toughness
and killed by the identical `destroyLethalToughness` SBA sweep firing the trigger once (proving the action.go wiring
specifically), and `ActivationLimit$ 1` firing once. Regression-verified by temporarily removing the new call from
`sacrificeCards` and confirming the batch test fails exactly as expected, then restoring it.

---

## Mode$ DamageDoneOnce lands, CR 603's own damage-batched trigger

`Mode$ DamageDoneOnce` is `Mode$ DamageDone`'s own batched sibling -- `Mode$ ChangesZoneAll`'s own shape (above) applied
to damage instead of zone changes -- and the corpus's own single largest remaining trigger mode at 206 real lines, ahead
of `ChangesZoneAll`'s own 126. `TriggerDamageDoneOnce.performTest` fires once per target that was dealt damage within
one damage-dealing action, summing every source that hit it, rather than once per `(source, target)` pair the way the
ordinary `DamageDone` trigger already fires. CR 510.2's own "all combat damage is dealt simultaneously" is why: a
creature blocked by two others takes damage from both blockers in the identical combat damage step, and a card reading
"whenever a creature is dealt damage" has to see the combined total once, not the same creature's own trigger firing
twice.

Java's own mechanism is `CardDamageTable`, a `Table<Card source, GameEntity target, Integer amount>` built up over one
whole damage-dealing action (`GameAction.dealDamage`'s own `damageMap`/`preventMap` parameters) and consumed once at its
very end via `damageMap.triggerDamageDoneOnce(isCombat, game)` -- itself firing four different trigger types from the
one table (`DamageDoneOnce` grouped by target, `DamageDealtOnce` grouped by source, `DamageDoneOnceByController` grouped
by target-and-controller, and `DamageAll` for the whole table at once). This port builds only the biggest of the four,
`DamageDoneOnce` itself; the other three are each their own further grouping over the identical table, not built.

`damageTable` (`[]damageEntry`, new, trigger.go) is `CardDamageTable`'s own port: `Source CardID`, `Target EntityID` (a
card or a player, the same mixed shape `AttackersDeclared`'s own `AttackedTarget$` already needed
`attackedTargetMatches` for), `Amount int` -- the actual amount dealt after prevention/replacement, never the raw
pre-reduction number a caller first computed. Declared in trigger.go rather than combatdamage.go, where it is built:
enginelint's own layering would otherwise need `trigger` to depend on `combatdamage` on top of `combatdamage` already
depending on `trigger` (to call `checkDamageDoneTriggersToCard`/`ToPlayer`), a cycle the tool refuses -- every builder
of a table already reaches trigger.go through its own existing dependency on it, so declaring the type there instead
costs nothing.

`dealPermanentDamage`/`dealPlayerDamage` (combatdamage.go) each gained a `table *damageTable` parameter: when non-nil,
the actual dealt amount is appended to it right where the existing per-exchange `checkDamageDoneTriggersToCard`/
`ToPlayer` call already sits, so the two happen at the identical point in the code for the identical reason -- this is
the amount that actually happened, after `damagePrevented`/`damageReplaced` (or their player-shaped twins) have already
run. `dealAttackerDamage`/`dealAttackTargetDamage` (combatdamage.go) just thread the pointer through unchanged. A nil
table (no real call site passes one today) means "not accumulating," the identical opt-out a missing parameter would
otherwise force every caller to build an unused table just to satisfy the signature.

Two real callers build one and consume it: `dealCombatDamageStep` (combatdamage.go) declares `var table damageTable`
once per first-strike-or-regular damage sub-step, threads `&table` through every exchange the step makes, and calls
`g.checkDamageDoneOnceTriggers(controller, table, true)` once after its own loop finishes -- CR 510.2's own simultaneity
boundary is exactly one sub-step, not the whole combat phase (first strike damage and regular damage are two genuinely
separate events, CR 510.4). `dealDamageEffect` (dealdamageeffect.go) builds its own local table per resolution -- more
than one entry when `Defined$` names several players at once (`Defined$ Player`, every player) -- and calls
`checkDamageDoneOnceTriggers` with `isCombat` false once its own loop of `dealPlayerDamage` calls (or the single
`Defined$ Self` call) finishes, so a script-driven ability's own damage is one batch too, not just a combat step's.

`checkDamageDoneOnceTriggers` (trigger.go) groups the table by target first, in first-seen order (GO-12) -- a plain
`map[EntityID][]damageEntry` keyed off an `order []EntityID` slice, since Go's own map iteration order is not stable
enough to trust for anything a card script's own resolution order could depend on. For each target, every watching
permanent's own `Mode$ DamageDoneOnce` trigger is checked once: `CombatDamage$` against `isCombat`
(`TriggerDamageDoneOnce.performTest`'s own first check); the summed amount -- `damageDoneOnceAmount`, new, ports
`TriggerDamageDoneOnce.getDamageAmount` directly: every entry in the target's own group, filtered first to only the ones
whose `Source` matches `ValidSource$` when the line names one, summed -- checked against `DamageAmount$`
(`damageAmountMatches`, `DamageDone`'s own dispatch, trigger.go, reused outright, the identical operator/operand parse
and the identical `TargetToughness` special case, computed once per target rather than once per line since every line
checked against the identical target shares the identical toughness); and `ValidTarget$` against the target itself
(`attackedTargetMatches([]EntityID{target}, ...)`, `AttackersDeclared`'s own dispatch, reused at its one-element case,
the identical shape `checkDamageDoneOnceTriggers`' own damage-batched sibling `checkChangesZoneAllTriggers` (above)
already reuses a sibling dispatch for). Every target's own live state is still current when this runs (called before any
state-based action can move a lethally damaged creature to the graveyard), so no `g.LKI` lookback is needed the way
`checkSacrificedTriggers`'/`checkChangesZoneAllTriggers`' own damage-adjacent siblings need one -- those run after their
own move, this runs before any SBA has had a chance to.

`PlayerTurn$`/`OptionalDecider$`/the whole `IsPresent$`/`CheckSVar$`/... family all resolve too, but generically,
through `triggerEffectAPI`'s own shared gate, the identical free ride every other trigger mode reaching that chokepoint
already gets. 200 of the corpus's own 206 real lines resolve. Not resolved: `ActiveZones$` (2) -- not read by
`TriggerDamageDoneOnce.performTest` at all, real meaning on the handful of lines naming either unclear; `DamageSource$`
(1) -- an object reference this port has no resolver for; `FirstTime$` (1) -- `GameEntity.getAssignedDamage`, a
per-target running total across the whole turn this port tracks nowhere. A trigger carrying any of these four is skipped
entirely, not fired unconditionally (GO-7).

6 new tests (`damagedoneonce_test.go`) drive the dispatch through the real combat-damage and cast-and-resolve pipelines,
`checkDamageDoneOnceTriggers` itself being unexported (TEST-1): a double-blocked attacker firing once for its combined
1+1 damage (life +5, not +10, proving "once for the batch" without needing `DamageAmount$`'s own value at all -- the
identical no-`Amount$`-needed proof `ChangesZoneAll`'s own tests already used), two unblocked attackers hitting one
player firing once for their combined 3+4, `ValidSource$` filtering an Elf-and-Goblin double block's own summed damage
down to the Elf blocker's 1 before `DamageAmount$ EQ1` is checked against it (an unfiltered sum of 2 would fail that
check and never fire, so a pass proves the filter genuinely ran first), a `DealDamage` hitting every player firing once
per player rather than merging every player into one shared batch, `CombatDamage$ True` rejecting a non-combat
`DealDamage`, and `ResolvedLimit$ 1` firing once. Regression-verified by temporarily removing the new call from
`dealCombatDamageStep` and confirming the double-block test fails exactly as expected, then restoring it.

---

## Mode$ DamageDealtOnce and Mode$ DamageAll land, DamageDoneOnce's own table-sharing siblings

`CardDamageTable.triggerDamageDoneOnce` (Java, already quoted in the previous section) does not fire only
`Mode$ DamageDoneOnce` off its own table -- it fires four trigger types off the identical one, in sequence:
`DamageDealtOnce` (grouped by source), `DamageDoneOnce` (grouped by target, "M6's own damage-batched trigger," above),
`DamageDoneOnceByController` (grouped by target and by each of the damaging sources' own controllers), and `DamageAll`
(the whole table at once, no grouping). This port already had the table (`damageTable`, trigger.go) and the two real
call sites that build one (`dealCombatDamageStep`, combatdamage.go; `dealDamageEffect`, dealdamageeffect.go) from the
previous chunk -- reusing both for two more of Java's own four trigger types was cheap enough to do in the same sitting
once `DamageDoneOnce` itself was done and tested.

A new `checkDamageTableTriggers` (trigger.go) is what both real call sites actually invoke now, in place of a bare
`checkDamageDoneOnceTriggers` call: it runs all three built dispatches off the one table, `CardDamageTable`'s own real
sequencing ported directly. `DamageDoneOnceByController`, the fourth, is The Initiative's own trigger -- 0 real corpus
lines name it -- and runs from the same call (`checkDamageDoneOnceByControllerTriggers`,
[`effects-monarch-initiative-venture.md`](effects-monarch-initiative-venture.md#takeinitiative-lands)). A future fifth
table-driven mode, if the corpus ever needs one, has exactly one call site to add to, not two.

`checkDamageDealtOnceTriggers` (`Mode$ DamageDealtOnce`, ported from `TriggerDamageDealtOnce.performTest`) is
`checkDamageDoneOnceTriggers`'s own mirror image: `bySource`, not `bySource[e.Target]`, groups the table -- a
gang-blocked attacker splitting its power between two blockers is one source (the attacker) dealing damage to two
targets (the blockers) in the same combat damage step, and this fires once for the attacker's own combined total, not
once per blocker it hit. `ValidSource$` matches the source directly through the ordinary `Matches` (a `Card`, unlike
`DamageDoneOnce`'s own mixed card-or-player target), the dominant real shape being the literal `Card.Self` -- "whenever
this creature deals damage" -- 49 of the corpus's own 49 real lines name it. `ValidTarget$`, when present, plays
`ValidSource$`'s own dual role from `checkDamageDoneOnceTriggers` in reverse: it both filters which of the group's own
entries count and sums only those (`damageDealtOnceAmount`, new, `TriggerDamageDealtOnce.getDamageAmount`'s own
dispatch, ported directly, `attackedTargetMatches` reused at its one-element case for the mixed target shape), and gates
the whole line on that filtered sum being positive -- the identical
`if hasParam(ValidTarget) { if amount <= 0 return false }` shape `DamageDoneOnce`'s own `ValidSource$` check already
has, mirrored. `DamageAmount$` is not a real param on this mode at all (`TriggerDamageDealtOnce.performTest` never reads
it), so no `damageAmountMatches` call is needed here the way `DamageDoneOnce`'s own dispatch has one. 47 of the corpus's
own 49 real lines resolve; not resolved: `AtLeastOneInstance$` (1) -- "at least one single damage instance meets this
comparison" (a `fullParam.substring`/`Expressions.compare` check against each individual entry's own amount, not the
summed total -- a genuinely different shape this dispatch has no evaluator for). A trigger carrying it is skipped
entirely, not fired unconditionally (GO-7). `ActivationLimit$` is enforced
([Trigger activation limits](#trigger-activation-limits)).

`checkDamageAllTriggers` (`Mode$ DamageAll`, ported from `TriggerDamageAll.performTest`) is the simplest of the three:
no grouping at all, firing once for the whole action whenever `table.filteredMap(ValidSource$, ValidTarget$, ...)` would
be non-empty in Java -- ported as `damageAllTableMatches` (new), which short-circuits on the first table entry matching
BOTH `ValidSource$` and `ValidTarget$` together (either absent is a pass for its own half) rather than building and
returning the filtered table itself, since nothing downstream of the emptiness check ever reads it back. 9 of the
corpus's own 9 real `T:Mode$ DamageAll` lines resolve -- every param this mode's own real lines carry
(`ValidSource$`/`ValidTarget$`/`CombatDamage$`/`PlayerTurn$`/`OptionalDecider$`) already has a resolver somewhere in
this port, the first trigger mode built this session with zero real unresolved lines left over.

6 new tests (`damagetabletriggers_test.go`) drive both dispatches through the real combat-damage and cast-and-resolve
pipelines, `checkDamageDealtOnceTriggers`/`checkDamageAllTriggers` both being unexported (TEST-1): a gang-blocked
attacker's own split damage firing `DamageDealtOnce` once for the combined 2+3 total (not twice), `ValidTarget$`
filtering an Elf-and-Goblin double block's own per-target amounts down to the Elf blocker's own 0 (a real negative
control: assigning 0 to the Elf blocker and 5 to the Goblin blocker means an unfiltered implementation summing every
target the attacker hit would see 5 and wrongly fire, while the correctly filtered sum is 0 and must not),
`ActivationLimit$ 1` firing `DamageDealtOnce` once, `DamageAll` firing once for a double block's own four separate
exchanges rather than once per exchange, `DamageAll` rejecting `ValidTarget$ Player` against an all-creature combat
(proving the filter is genuinely checked, not a pass-through), and `DamageAll` firing for a non-combat `DealDamage` too,
the identical wiring proof `DamageDoneOnce`'s own tests already gave. Regression-verified by temporarily narrowing
`checkDamageTableTriggers` back down to just its own `checkDamageDoneOnceTriggers` call and confirming every new
positive test in this chunk fails exactly as expected, then restoring it.

## Trigger activation limits

`ActivationLimit$ N` and `GameActivationLimit$ N` (157 real `T:` lines, 40 of them `ChangesZoneAll`) are
`Trigger.checkActivationLimit` (`Trigger.java:362-372`): the trigger has fired fewer than N times this turn (this game).
`triggerEffectAPI` (`trigger.go`), the gate every mode already passes through, applies `triggerActivationLimitMet`, and
`pushTriggeredAbilities` counts a firing as the trigger goes on the stack (`Trigger.triggerRun`, `Card.trigActs`, keyed
by the `Execute$` ability as Java keys it by its overriding ability). The count resets each cleanup, clears on any zone
change (a new object, CR 400.7) and clones with the game (`activationmod.go`). Every dispatch that skipped such a line
now fires it within the limit; before, a `ChangesZone` line with a limit fired every time. `ResolvedLimit$ N` (34, CR
603.2i) is `Trigger.checkResolvedLimit`: fewer than N resolutions of the trigger's ability this turn
(`Card.trigResolved`, noted in `Registry.Resolve`). Java checks it again when the trigger resolves
(`WrappedAbility.java:417`); this port checks it only when the trigger would go on the stack, so two triggers already on
the stack both resolve. Both limits must be literal integers, as Java's integer parse reads them.

## `AbilityCast`, `SpellAbilityCast`, `SpellCastOrCopy`, `Cycled`, `AttackerUnblocked`, `DiscardedAll`

| Mode | Fires | Java | Go |
| --- | --- | --- | --- |
| `AbilityCast` (44) | as an activated ability goes on the stack | `MagicStack.java:235-237`, `:350` | `checkAbilityCastTriggers`, from `ActivateAbility` |
| `SpellAbilityCast` (6), `SpellCastOrCopy` (31) | a cast spell (and an ability, for the first); copies are not fired | `MagicStack.java:331-400` | `spellCastModes` in `appendSpellCastMatches` |
| `Cycled` (78) | after a Cycling or TypeCycling ability is activated; `FirstTime$` the player's first this turn | `Player.addCycled`, `Player.java:3861` | `checkCycledTriggers`, `Player.CyclingThisTurn` |
| `AttackerUnblocked` (37), `...Once` | after blockers are declared, per unblocked attacker (forced-blocked ones are blocked) | `Combat.java:673-703` | `checkUnblockedTriggers`, from `DeclareCombatBlockers` |
| `DiscardedAll` (22) | once per batch of discards; `FirstTime$` none matching earlier this turn | `SpellAbilityEffect.java:943`, `CostDiscard.java:246` | `checkDiscardedAllTriggers`, `Defined$ TriggeredCards` |

`triggerscan.go` holds the shared walk (`scanTriggers`, `appendTriggerMatches`) and each mode's param whitelist
(`paramsResolvable`): a line naming a param outside it does not fire (GO-7). `ValidSA$` is evaluated by
`spellAbilityMatches` (`saspec.go`, `SpellAbility.isValid`/`SpellAbilityProperty.hasProperty`): the heads `Spell`,
`Activated`, `Triggered`, `Ability`, `SpellAbility`, `Instant`, `Sorcery`; the properties `ManaAbility`, `hasTapCost`,
`Loyalty`, `YouCtrl`, `OppCtrl`, `singleTarget`, `numTargets`, `cmc<cmp>X`, the keyword abilities (`Cycling`, `Equip`,
`Exhaust`, `Crew`, ...) and any card property of the host (`Self`, `Creature`, ...). A property naming how a spell was
cast or paid (`Kicked`, `Flashback`, `ManaSpent`, `ManaFrom`, `MayPlaySource`, ...) is unrecognized and the line does
not fire. `ValidSAonCard$`, `TargetsValid$` and `CanTargetOtherCondition$` lines on a cast trigger are still skipped. An
activated ability is marked `Ability.activated`, so it is not a trigger (`isTrigger`), whichever path pushed it. Tests:
`castactivatemodes_test.go`.
