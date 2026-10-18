# Port Log — Game State: Replacement Effects

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

CR 614/616 replacement effects and `requirementsCheck`.

## `ReplacementEffect.requirementsCheck` lands

A general gate every replacement carries regardless of its own `Event$`, checked before its own shape-specific
`canReplace` -- `Trigger.phasesCheck`'s own exact sibling
(["`Trigger.phasesCheck` lands"](trigger-modes.md#triggerphasescheck-lands),), a different Java method on a different
class, not the same one reused twice: `Trigger` and `ReplacementEffect` are sibling subclasses of
`TriggerReplacementBase` rather than one inheriting from the other.

`replacementRequirementsCheck` (`replacement.go`, new) ports it at the shape this port can reach.

`PlayerTurn$` (8 real `R:` lines combined across `DamageDone`/`Draw`/`CreateToken`/`LifeReduced`/`TurnFaceUp`, every one
the literal value `True` -- 0 real lines use Java's own Defined$-reference else-branch, so only the literal-`True`
branch is ported) checks `isPlayerTurn(hostController)` directly.

`ActivePhases$` (1 real line, island_sanctuary.txt's own `Draw` shape) reuses `phaseTriggerMatches` (trigger.go,
`Mode$ Phase`'s own dispatch function) at its own key rather than `Phase$`'s. `phaseTriggerMatches` gained a `key`
parameter for this; its two existing call sites (`checkPhaseTriggers`, `triggerPhasesCheck`) both updated to pass the
literal `"Phase"` explicitly, so the identical phase-list parser now serves three callers under three different param
names for the identical underlying question.

`triggerCommonRequirementsMet` (`CardTraitBase.meetsCommonRequirements`'s own port, trigger firing above) is called
outright last -- `ReplacementEffect.requirementsCheck`'s own final line calls the identical Java method a `Trigger`'s
own `performTest` already does, so this port's identical shared function serves both for the same reason.

Every existing consumer in `replacement.go` (`damagePreventionMatches`, `untapReplacementMatches`,
`replacementTapsOnMove`) had its own separate, narrower allow-list of extra params it tolerated before this landed; each
now calls `replacementRequirementsCheck` and widens its own allow-list to admit the keys it reads, closing 7 of the 10
previously-skipped real `DamageDone`|`Prevent$` lines (`PlayerTurn$` 4 -- guardian_naga_banishing_coils.txt's own real
"can't be dealt damage during your turn" among them; `CheckSVar$`/`SVarCompare$` 2; `IsPresent$` 1) and 5 of the 7
previously-skipped `Untap`|`CantHappen` lines (`IsPresent$` 4; `CheckSVar$`/`SVarCompare$` 1) for free -- neither
function needed a single line of its own new logic for either, just a wider switch and one extra call. The exact counts
are below, in each shape's own section.

Folding the general gate into `replacementTapsOnMove` also fixed a real, if narrow, wrong-firing bug that predates this
chunk: `replacementTapsOnMove` carried no allow-list at all before now (unlike its two siblings), so ANY extra param on
an "enters tapped" `R:` line was silently ignored rather than gating anything. archelos_lagoon_mystic.txt's own real
toggle -- "As long as CARDNAME is tapped, other permanents you control enter tapped. As long as CARDNAME is untapped,
other permanents enter untapped" -- writes exactly this as two
`Event$ Moved | ... | IsPresent$ Card.Self+tapped/+untapped | ReplaceWith$ ETBTapped/ETBUntapped` lines, restricting
each half to fire only while Archelos ITSELF is tapped or untapped respectively. Before this chunk, `IsPresent$` was
never checked at all, so the `ETBTapped` half (`ReplaceWith$` resolving to a plain `DB$ Tap`, `tapAbilityResolvesTap`'s
own recognized shape) would apply regardless of whether Archelos was actually tapped -- the `ETBUntapped` half never
mattered either way, since `tapAbilityResolvesTap` only recognizes `Tap`-named sub-abilities, not `Untap`. A corpus
check confirmed this is the ONLY real line among all 618 `ETBTapped`/`LandTapped`-shaped `Moved` lines carrying any of
`triggerCommonRequirementsMet`'s own hard-skip keys or the general gate's own `PlayerTurn$`/`ActivePhases$`, so folding
the check in here changes no other real card's behavior, confirmed and not merely assumed.

Two new consumers reuse the same general gate directly for a shape neither `checkMovedReplacement` nor
`damagePrevented`/`untapBlocked` cover: **`drawPrevented`/`gainLifePrevented`** (`replacement.go`, new) resolve CR
120.3's/119's own `Prevent$ True` shape for `Event$ Draw`/`GainLife` -- CR 119's own "life gain replacement" family this
port's own `gainLifeEffect` doc comment already flagged as entirely unbuilt has its one directly resolvable real shape
now. 2 of the corpus's own 39 real `Draw` lines resolve end to end: possessed_portal.txt's own bare
`ValidPlayer$ Player | Prevent$ True` ("if a player would draw a card, that player skips that draw instead") and
living_conundrum.txt's own `IsPresent$ Card.YouOwn | PresentZone$ Library | PresentCompare$ EQ0`-qualified form ("if you
would draw a card while your library has no cards in it") -- resolved through `replacementRequirementsCheck`'s own
`triggerCommonRequirementsMet` fold-in with no code of its own needed, the identical zone-presence check `LandTapped`'s
own checkland condition already exercises. obstinate_familiar.txt's own third real `Prevent$` line (`Optional$ True`)
stays unresolved: an interactive "may" confirm this port's own `PlayerController` has no hook for, the identical gap
`Discard`'s own `Optional$`/`BecomesTarget`'s own `OptionalDecider$` already document. 1 of the corpus's own 21 real
`GainLife` lines resolves end to end -- sulfuric_vortex.txt's own bare "if a player would gain life, that player gains
no life instead," and the ONLY one of the 21 naming `Prevent$` at all.

`drawPrevented` is checked from `DrawCards` (turn.go) one card at a time, BEFORE the empty-library check runs at all --
Java's own `Player.doDraw` checks its `Event$ Draw` replacement before it ever looks at whether the library is empty,
ported directly: a card `drawPrevented` reports true for never reaches the empty-library branch below it, so a draw CR
614 prevents cannot also be CR 704.5b's own "attempted to draw from an empty library" loss -- possessed_portal.txt's own
real shield would otherwise still lose its own controller to state-based action 704.5b even though the draw it prevented
was the only thing that could have exposed the empty library to begin with. `gainLifePrevented` is checked from
`gainLifeEffect` (gainlifeeffect.go) per player, before `Player.Life` is touched at all.

The other 36 real `Draw` lines and 20 real `GainLife` lines all name `ReplaceWith$` instead of `Prevent$` --
`DrawTwo`/`Dig`/`ExileTop`/... for `Draw`, `GainDouble`/`RLoseLife`/`Draw` for `GainLife`. At the time this section was
written, every one of these needed "the amount that would have been drawn/gained" as a runtime `X`/`Y` value (Java's own
`AbilityKey.ReplacedAmount` threading into a fresh `calculateAmount` call) this port's `resolveAmount` had no way to
read back. `## Draw`'s own `ReplaceWith$`, below, and
[`## GainLife`](#gainlifes-own-replacewith-the-one-runtime-value-draws-dispatch-never-needed)'s own `ReplaceWith$`,
further below, resolve 3 and 4 of these two totals respectively, including `nefarious_lich.txt`'s own real
`GainLife`-into-`Draw` line named here originally as an example of the gap -- `resolveGainLifeReplacementAmount`'s own
narrow `ReplaceCount$LifeGained` case reads that runtime value directly from the raw amount `gainLifeEffect.Resolve`
already computes, rather than through `resolveAmount`/`resolveNamedAmount` at all. `rain_of_gore.txt`'s own line stays
unresolved, but for a different reason than "the amount": its own restriction is
`ValidSource$ SpellAbility | SourceController$ True`, not a `ValidPlayer$` this dispatch's own allow-list recognizes at
all.

Fourteen new tests total: five in `replacement_test.go`
(`TestUntapBlockedWhenIsPresentConditionMet`/`TestUntapNotBlockedWhenIsPresentConditionNotMet` replace the old
`TestUntapNotBlockedByUnresolvedExtraParam`, since `IsPresent$` no longer means "skip" -- the OLD test's own real shape,
`IsPresent$ Creature.YouCtrl` checked against a lock creature that IS itself the only creature its controller controls,
now genuinely blocks, the opposite of what the old test asserted; `TestDamageToCreatureNotPreventedByUndefinedCheckSVar`
replaces `TestDamageToCreatureNotPreventedByUnresolvedExtraParam` for the identical reason, its own numeric assertion
unchanged since `CheckSVar$ X` with no `SVar:X` on the card still fails closed through `resolveNamedAmount`'s own
unresolvable-reference return, just for the honest reason of an undefined reference rather than an unrecognized param
name now; `TestDamageToPlayerPreventedWhenPlayerTurnMatches`/`TestDamageToPlayerNotPreventedWhenPlayerTurnDoesNotMatch`
prove the new `PlayerTurn$` fold-in both ways) and six in a new `drawgainlifeprevented_test.go`
(`TestDrawPreventedByBareReplacement`, `TestDrawPreventedDoesNotCauseEmptyLibraryLoss` -- the CR-faithful ordering proof
above, `TestDrawPreventedWhenIsPresentConditionMet`/`TestDrawNotPreventedWhenIsPresentConditionNotMet`,
`TestDrawNotPreventedByUnresolvedOptional`, `TestGainLifePreventedByBareReplacement`). All five renamed/new
`replacement_test.go` cases and the four `drawPrevented`/`gainLifePrevented`-proving cases were each regression-checked
by temporarily removing the corresponding code path and confirming the suite fails exactly as expected before restoring
it.

`enginelint.json`'s own `"replacement"` group gained `"trigger"` in its allow-list -- `replacementRequirementsCheck`
calls `phaseTriggerMatches`/`triggerCommonRequirementsMet` directly now, and `"condition"` (already in `"replacement"`'s
allow-list) already reached into `"trigger"` itself, so this adds no new cycle, just a direct edge alongside an existing
indirect one the tool's own per-file check does not treat as equivalent.

---

## `Draw`'s own `ReplaceWith$`: CR 616's "the event is replaced"

Every replacement shape this file resolved before now (`checkMovedReplacement`, `untapBlocked`, `damagePrevented`,
`drawPrevented`, `gainLifePrevented`) either applies a fixed outcome (`Tapped = true`, blocked, prevented -- CR 616's
own "more than one could apply" choice is moot because applying any of them twice is a no-op) or is a bare
`Prevent$ True` (the event simply does not happen). `ReplaceWith$` -- CR 616's own "the event is replaced by a DIFFERENT
event" -- is real content for the first time, for `Draw` alone.

The obvious design, resolving a `ReplaceWith$` target through `Registry.Resolve` (effect.go) the way `resolveSubAbility`
(subability.go) already chains a `SubAbility$`, does not work: `Registry.Resolve` needs a `*Registry`, and `DrawCards`'
own call chain (`drawStep`/`beginPhase`/`AdvancePhase`, turn.go, and `drawEffect.Resolve`, draweffect.go) has no way to
reach one without extending `AdvancePhase`'s own public signature -- which every existing `AdvancePhase` caller across
the whole test suite would then need to pass one to, a blast radius wildly disproportionate to unlocking a handful of
real corpus lines. `Effect.Resolve` itself also returns no error `DrawCards` (a `void` function) has anywhere to
propagate.

`drawReplaced` (replacement.go, new) sidesteps both problems the way `tapAbilityResolvesTap`
([`## Replacement effects`](#replacement-effects-entering-the-battlefield-tapped), below) already does for a different
Event$: recognize a narrow, hand-verified-safe shape and run its own underlying primitive directly, rather than going
through the general effect machinery at all. Two shapes are recognized, `applyDrawReplacement`'s own dispatch.

The first is a plain `DB$ Draw | Defined$ You | NumCards$ N`. `drawOneCard` (turn.go) runs it -- `DrawCards`' own
per-card loop body, factored out into its own primitive for this. The second is a plain
`DB$ PutCounter | CounterType$ X | CounterNum$ N | Defined$ Self`. `Card.Counters.Add` runs it directly, the identical
shape `putCounterEffect` already resolves, hand-run here instead of called.

Both refuse outright the moment the target ability itself names `SubAbility$` -- return false, meaning the line is
skipped and the original draw proceeds unreplaced. Chaining one needs the identical `*Registry` this call site cannot
reach, and running the leaf half alone while silently dropping the chained half would be a wrong answer, not an honest
gap (GO-7). blood_scrivener.txt's own real "draw two cards, then lose 1 life" is the corpus's own example of exactly
this shape.

`Defined$` on the target ability is read as "the player who would have drawn" directly (the `player` parameter already
threaded through `drawReplaced`) rather than through the general `definedPlayers` (defined.go): every real corpus line
needing this dispatch writes the literal token `You`, and `ValidPlayer$` (checked one level up, in `drawReplaced`
itself) already restricts the whole dispatch to the one case where `player` equals the host's own controller anyway, so
the two readings are the identical answer for every real line this covers -- not a general `Defined$` resolver, a narrow
one scoped to what `drawReplaced` alone needs.

The recursion question this design had to answer explicitly: thought_reflection.txt's own real "if you would draw a
card, draw two cards instead" -- what stops its own substitute draws from being checked against thought_reflection's OWN
line again, forever? Java's answer is `ReplacementHandler`'s own `hasRun` set (ReplacementHandler.java): the specific
`ReplacementEffect` object currently executing is excluded from `getReplacementList`'s own candidates while its own
substitute ability is still resolving, cleared only once that resolution fully returns. This port builds no equivalent
per-line guard at all -- instead, `drawOneCard` is called directly for the substitute's own N draws, never routing back
through `DrawCards`/`drawPrevented`/`drawReplaced` itself, so there is nothing left for thought_reflection's own line to
recheck against. The real, narrow cost: the substitute's own draws are not checked against ANY other replacement or
prevention effect on the battlefield either, not just the one that produced them -- a card with two independent
Draw-replacing permanents would see only the first apply, not both compounding the way CR 616 (and Java's own
`hasRun`-guarded recursion) actually allows. No real corpus deck combines two such permanents today, so this is not
observable against the corpus, and is flagged rather than silently accepted.

7 of the corpus's own 36 real `Event$ Draw | ReplaceWith$` lines resolve end to end: thought_reflection.txt's own bare
form (`ValidPlayer$ You`, target `DB$ Draw | Defined$ You | NumCards$ 2`); phial_of_galadriel.txt's own
`Hellbent$ True`-qualified identical shape ("while you have no cards in hand," resolved through
`replacementRequirementsCheck`'s own `triggerCommonRequirementsMet` fold-in with no code of its own needed);
ormos_archive_keeper.txt's own `IsPresent$ Card.YouOwn | PresentZone$ Library | PresentCompare$ EQ0`-qualified line,
whose own target is `DB$ PutCounter | CounterType$ P1P1 | CounterNum$ 5 | Defined$ Self` rather than a `Draw` at all
("if your library has no cards in it, instead put five +1/+1 counters on CARDNAME"); and 4 more resolved through
`NotFirstCardInDrawStep$`'s own gate, the next section below.

Not resolved, each for its own real reason found while dumping every real `Event$ Draw | ReplaceWith$` line and checking
what its own target ability actually needs: reed_richards_smartest_man.txt's own `FirstExtraCardDrawnThisTurn$` (1);
hullbreacher.txt's own `NotFirstCardInDrawStep$` shape, whose own target is `DB$ Token | TokenScript$ c_a_treasure_sac`
rather than `Draw`/`PutCounter` -- `CreateToken` is not a built `Effect` yet, unrelated to the gate the next section
resolves (1); magus_of_the_chains.txt's/chains_of_mephistopheles.txt's own `Defined$ ReplacedPlayer` plus
`SubAbility$ DBDraw` and breathstealers_crypt.txt's/sea_of_sand.txt's own `Defined$ ReplacedPlayer` plus `SubAbility$`
(4 combined -- "the player who would have drawn" as a general `Defined$` token, distinct from this dispatch's own narrow
`player`-parameter reading above, which only ever covers the literal token `You`); blood_scrivener.txt's own
`SubAbility$ DBLoseLife` alone (1, the chained-target refusal above); booby_trap.txt's own `ValidPlayer$ Player.Chosen`
(1, `matchesPlayerSpec`'s own doc comment) and pursuit_of_knowledge.txt's own `Optional$ True` (1, `Discard`'s own
`Optional$` gap). `alms_collector.txt`'s/`quantum_riddler.txt`'s own two real lines name `Event$ DrawCards` rather than
`Event$ Draw` -- Java's own OUTER, once-per-batch-call replacement check (`Player.drawCards`'s own
`ReplacementType.DrawCards` dispatch, checked before its per-card loop even starts, distinct from `ReplacementType.Draw`
inside `doDraw()` that every other real line in this file targets) -- a different granularity this port's own per-card
`DrawCards` loop has nowhere to hook, not attempted.

`GainLife`'s own 20 real `ReplaceWith$` lines split the same way: 15 target `DB$ ReplaceEffect`, a dedicated API this
port does not build, and the other 5 name `ReplaceCount$LifeGained` -- "the amount of life that would have been gained"
as a runtime value, distinct from an ordinary `Count$` expression `resolveAmount` evaluates.
[`## GainLife`](#gainlifes-own-replacewith-the-one-runtime-value-draws-dispatch-never-needed)'s own `ReplaceWith$`,
further below, resolves 4 of those 5 (nefarious_lich.txt's/lich.txt's/tainted_remedy.txt's/ plague_drone.txt's own real
lines) once `resolveGainLifeReplacementAmount` reads that runtime value directly rather than through `resolveAmount`;
rain_of_gore.txt's own line stays unresolved for its own separate reason, that section's own doc comment has it.

Seven new tests, all in a new `drawreplaced_test.go`: `TestDrawReplacedByBareReplacement`,
`TestDrawReplacedSetsDrewFromEmptyLibraryWhenItRunsOut` (the substitute's own draws are real draw attempts, not skipped
ones -- unlike `Prevent$`, running out of library partway through a replaced draw still sets `DrewFromEmptyLibrary`),
`TestDrawReplacedWhenHellbentConditionMet`/`TestDrawNotReplacedWhenHellbentConditionNotMet`,
`TestDrawReplacedWithPutCounterInstead`, `TestDrawNotReplacedByTargetAbilityWithSubAbility` (two cards in the library,
so a wrongly-permitted double-draw and a correctly-refused single draw are actually distinguishable -- the first draft
of this test used only one library card and passed either way, a self-caught weak assertion fixed before finalizing),
`TestDrawNotReplacedByUnrecognizedTargetAbility`. Every fire-side test and the `SubAbility$` refusal were
regression-checked by temporarily removing the corresponding code path and confirming the suite fails exactly as
expected before restoring it.

`enginelint.json`'s own `"replacement"` group gained `"control"`, `"amount"`, `"putcountereffect"`, `"parts"` and
`"event"` in its allow-list -- `drawReplaced`'s own hand-run dispatch reaches `PlayerController` (a type), and
`resolveNamedAmount`/`putCounterType`/`Counters`/`emitCounterChanged` (functions and a type), none of which
replacement.go referenced before.

---

## `Draw`'s own `NotFirstCardInDrawStep$`: "except the first" and `notion_thief.txt`'s own divert

`ReplaceDraw.canReplace` (Java) carries a second gate past `ValidPlayer$`/`ValidCause$`: `NotFirstCardInDrawStep$ True`
exempts exactly one draw from the whole line -- the very first card the affected player draws while the game's current
phase is that player's own Draw step. Java's own two-part check is `p.numDrawnThisDrawStep() == 0 && ownDraw`, where
`ownDraw` is "the current phase is Draw AND `p` is the active player" -- a later draw in the identical step, or any draw
of `p`'s outside `p`'s own Draw step entirely (an instant-speed effect during another player's turn, or during `p`'s own
non-Draw phase), is never exempt.

`Player.DrawnThisDrawStep` (player.go, new) is Java's own `numDrawnThisDrawStep` -- distinct from `CardsDrawnThisTurn`'s
whole-turn scope. `drawStep` (turn.go) resets it for every player at the start of each Draw step (`PhaseHandler.java`'s
own per-player reset loop, ported directly), and `drawOneCard` (turn.go) increments it for whichever player actually
draws, whenever the game's current phase is Draw -- Java's own unconditional `game.getPhaseHandler().is(PhaseType.DRAW)`
check, with no player comparison, so a non-active player who draws an extra card while the active player's own Draw step
is still current counts too. A skipped Draw step (CR 103.7a, the first player's own first turn in a two-player game)
resets nothing, matching Java's own case body never running when the step itself is skipped.

`notFirstCardInDrawStepExempts` (replacement.go, new) reads both fields to answer Java's own two-part question directly:
`g.activePhase == Draw && g.activePlayer == player` for `ownDraw`, `g.Player(player).DrawnThisDrawStep == 0` for the
count -- both must hold for the exemption to apply. `drawReplaced` calls it right after the `ValidPlayer$` match,
skipping the whole replacement line (the draw proceeds normally) when it returns true.

teferis_ageless_insight.txt's/alhammarrets_archive.txt's/bard_king_of_dale.txt's own real "except the first one you draw
in each of your draw steps, draw two cards instead" (`ValidPlayer$ You`) resolve through this gate onto the
already-built plain `DB$ Draw | Defined$ You | NumCards$ 2` target `drawReplaced` already knew how to run.

notion_thief.txt's own real "if an opponent would draw a card except the first one they draw in each of their draw
steps, instead you draw a card" (`ValidPlayer$ Opponent`) resolves through the identical gate but needed one more fix:
`applyDrawReplacementDraw`'s own `Defined$ You` used to be read as `player` directly -- the event's own affected player
-- because every real line it had ever covered also carried `ValidPlayer$ You`, making the two the same player by
construction. notion_thief's own `ValidPlayer$ Opponent` breaks that coincidence: "you" in the replacement's own text is
Notion Thief's controller, not the opponent whose draw is being replaced. `applyDrawReplacementDraw` now reads
`Defined$ You` as `host.Controller()` instead -- Java's own `AbilityUtils.getDefinedPlayers` resolves a replacement
ability's own "You" to its activating player, always the host card's controller, never the event's own affected player
-- which happens to be an identical answer for every previously-resolved line (their own `ValidPlayer$ You` already
forced `player == host.Controller()`) and the correct one for notion_thief's. `player` was consequently dropped from
`applyDrawReplacementDraw`'s and `applyDrawReplacement`'s own signatures -- both had it only for that one now-corrected
read.

hullbreacher.txt's own identical `NotFirstCardInDrawStep$`/`ValidPlayer$ Opponent` shape stays unresolved: its own
`ReplaceWith$` targets `DB$ Token | TokenScript$ c_a_treasure_sac`, and `CreateToken` is not a built `Effect` in this
port -- an unrelated gap the gate itself does nothing to close.

`drawReplacementMatches`'s own allow-list gained `"notfirstcardindrawstep"` -- without it every real line carrying the
param would be rejected outright (the general "any param besides these skips the whole line" rule, GO-7), never reaching
`notFirstCardInDrawStepExempts` at all. `enginelint.json`'s own `"replacement"` group gained `"phase"` in its allow-list
for the new `g.activePhase == Draw` reference (`Draw`, a `PhaseType` constant declared in phase.go).

Eight new tests in `drawreplaced_test.go`: `TestDrawNotReplacedOnFirstDrawOfOwnDrawStep`/
`TestDrawReplacedOnSecondDrawOfOwnDrawStep` (the exemption's own count half, `DrawnThisDrawStep` set directly to isolate
it from the increment), `TestDrawnThisDrawStepAdvancesAcrossConsecutiveDraws` (the increment actually runs, proven by
two real `DrawCards` calls in the same Draw step rather than poking the field),
`TestDrawnThisDrawStepResetsAtEachDrawStep` (drives a full `AdvancePhase` cycle from turn 2's Draw step to turn 3's,
proving the counter does not accumulate across turns), `TestDrawReplacedOutsideOwnDrawStepEvenOnFirstDraw` (the
`ownDraw` half alone: a draw during Main1 is never exempt even at count 0),
`TestDrawReplacedByNotionThiefShapeDivertsToHostController`/
`TestDrawNotReplacedByNotionThiefShapeOnOpponentsOwnFirstDraw` (the `Defined$ You` fix, and its own exemption mirror).
Every new gate -- the exemption function itself, the allow-list entry, the increment, the reset, and the
`host.Controller()` reading -- was regression-checked by temporarily disabling it and confirming the corresponding test
failed with the expected wrong count before restoring it.

---

## `GainLife`'s own `ReplaceWith$`: the one runtime value `Draw`'s dispatch never needed

`gainLifeReplaced` (replacement.go, new) is `drawReplaced`'s own sibling for CR 119's "gain life" event, built the
identical way for the identical reason -- a `*Registry` `gainLifeEffect.Resolve`'s own call chain cannot reach, a
`SubAbility$`-carrying target refused outright rather than run with the chained half dropped. What it resolves that
`drawReplaced` never had to: `ReplaceCount$LifeGained`, an `expr.Amount` with `Kind == Expression` and
`Head == "ReplaceCount"` that `resolveAmount` (amount.go) does not evaluate at all -- its own `Expression` case requires
`Head == "Count"` exactly, so this is not a gap in that function's own dispatch table so much as a completely different
question: not "what does the game currently look like," but "what would this specific event have done." Nothing else
this port has built needed that distinction before now.

A new `resolveGainLifeReplacementAmount` (replacement.go) answers it without touching `resolveAmount`/
`resolveNamedAmount` at all: it tries a literal integer first, then checks whether `amounts[value]` is itself a
`ReplaceCount`-headed `Expression` and, if so, returns `replaced` directly -- the raw `LifeAmount$`
`gainLifeEffect.Resolve` already computed for the event now being replaced, threaded through `gainLifeReplaced`'s own
parameter of the same name -- falling through to the ordinary `resolveNamedAmount` for every other named reference. This
is deliberately not a new case inside `resolveAmount` itself: every other caller of that function (a continuous effect's
own numeric param, a trigger's own `NumDmg$`, ...) has no "replaced amount" in scope at all, so threading one through
its general signature would mean carrying a meaningless zero value almost everywhere it is called -- narrower and
correct beats general and wrong here.

Two already-built leaf effects are recognized as `ReplaceWith$` targets, `applyGainLifeReplacement`'s own dispatch: a
plain `DB$ Draw | Defined$ You | NumCards$` naming a `ReplaceCount$LifeGained` SVar (`applyGainLifeReplacementDraw`,
`applyDrawReplacementDraw`'s own shape reused, calling the identical `drawOneCard` primitive) and a plain
`DB$ LoseLife | LifeAmount$` naming one | `Defined$ You` or `Defined$ ReplacedPlayer`
(`applyGainLifeReplacementLoseLife`). `ReplacedPlayer` -- "the player who would have gained the life this replaces" --
resolves to the `player` parameter directly, the identical narrow `Defined$` reading `applyDrawReplacementDraw`'s own
doc comment already gives for its own literal `You` case: every real corpus line needing this dispatch writes one of
these two tokens, and both mean the same player `gainLifeReplaced`'s own `ValidPlayer$` check already narrowed things
down to.

Both of these are CR 616's own full-substitution ("Replaced") outcome: the gain event never happens at all, something
else happens instead. `gainLifeReplaced` originally reported that outcome as a plain `bool`, `gainLifeEffect.Resolve`'s
own `if g.gainLifeReplaced(...) { continue }` skipping the normal gain entirely on a match -- adequate for exactly this
outcome, but with nowhere to put CR 616's OTHER outcome, "Updated" (the same gain event, a different number), which
`damageReplaced` (above) already carries for a different `Event$`. `DB$ ReplaceEffect | VarName$ LifeGained` lines -- 15
real ones, angel_of_vitality.txt's/rhox_faithmender.txt's/... own "double"/"plus 1" life-gain shapes -- are exactly that
outcome, so `gainLifeReplaced`'s own signature changed from `bool` to `int`: it now returns the amount that should
actually be gained -- `amount` itself, unchanged, when nothing matches (`NotReplaced`); `0` when a full substitution
matched (`applyGainLifeReplacement`, unchanged, now signaling "nothing left to gain" the identical way
`applyDamageReplaceCounter`'s own full substitution already does by returning 0); or the resized amount when
`applyGainLifeReplaceEffect` (new, below) matches. `gainLifeEffect.Resolve`'s own call site changed from
`if g.gainLifeReplaced(...) { continue }` to `gain := g.gainLifeReplaced(...); if gain <= 0 { continue }` -- one check
that folds Java's own two separate ones (`Player.gainLife`, `if (!canGainLife() || lifeGain <= 0) return false` before
running any replacement at all, and `if (lifeGain <= 0) return false` again right after) into the single point this
port's own call ordering needs it at, matching Java's behavior for both directions without adding a second explicit
guard.

`applyGainLifeReplaceEffect` (replacement.go, new) is `applyDamageReplaceEffect`'s own sibling for a `DB$ ReplaceEffect`
line naming `VarName$ LifeGained` instead of `VarName$ DamageAmount`, resolving `VarValue$` through the newly
generalized `resolveReplaceCountAmount` (renamed from `resolveDamageReplaceCountAmount`, its own doc comment above --
the function itself was untouched beyond taking `wantBody` as a parameter instead of hardcoding `"DamageAmount"`) read
against `"LifeGained"`. The real corpus shape is narrower than `DamageAmount`'s own: every one of the 15 real lines
carries exactly `Twice` (7 -- rhox_faithmender.txt's/the_wind_crystal.txt's/selenia_the_cursed_heart.txt's/
alhammarrets_archive.txt's/doctor_strange_surgeon.txt's/boon_reflection.txt's/phial_of_galadriel.txt's own real "gain
twice that much life instead") or `Plus.1` (8 -- angel_of_vitality.txt's/heron_of_hope.txt's/honor_troll.txt's/
cleric_class.txt's/bilbo_birthday_celebrant.txt's/knight_of_dawns_light.txt's/leyline_of_hope.txt's/ pest_rescuer.txt's
own real "gain that much life plus 1 instead") -- no `Thrice`/`HalfDown`/`Minus` among them, though
`resolveReplaceCountAmount` resolves those too, generically, for free. A target ability naming `SubAbility$` is refused
outright, the identical chained-target refusal `applyGainLifeReplacement`'s own doc comment already gives (no real
corpus line among the 15 needs it).

19 of the corpus's own 20 real `Event$ GainLife | ReplaceWith$` lines resolve end to end now: the 4 already named above
(`Draw`/`LoseLife`'s own full substitution) plus the 15 `DB$ ReplaceEffect` lines just described. Both real
full-substitution pairs also carry `AILogic$` (`LichDraw`/`LoseLife`) and every real `DB$ ReplaceEffect` pair carries
`AILogic$ DoubleLife` -- a pure AI hint this port's own resolution never reads, already on
`gainLifeReplacementMatches`'s own allow-list alongside the params `drawReplacementMatches` already tolerates.

Not resolved: rain_of_gore.txt's own real "if a spell or ability would cause its controller to gain life, that player
loses that much life instead" -- `ValidSource$ SpellAbility | SourceController$ True`, no `ValidPlayer$` at all, a
restriction on what CAUSED the event (was it a spell/ability, and is its own controller the one gaining) rather than who
the event affects, a shape neither `gainLifeReplacementMatches`' own allow-list nor any sibling function in this file
has ever needed to check before -- falls through and skips the whole line (GO-7) rather than guessing.

Eight new tests, all in `gainlifereplaced_test.go`: `TestGainLifeReplacedByDrawInstead` (LifeAmount$ 3 threading through
as exactly 3 cards drawn, not a hardcoded 1 -- the test that actually exercises `resolveGainLifeReplacementAmount`'s own
runtime-value reading rather than a coincidence), `TestGainLifeNotReplacedWhenValidPlayerDoesNotMatch`,
`TestGainLifeReplacedByLoseLifeInstead`, `TestGainLifeNotReplacedByUnrecognizedSourceRestriction` (rain_of_gore.txt's
own real shape), `TestGainLifeDoubledByReplaceEffect` (rhox_faithmender.txt's own real `Twice` shape, and the only one
of the eight that also checks `LifeGainedTimesThisTurn` advances -- proving a resize still runs the normal gain path,
unlike a full substitution), `TestGainLifePlusOneByReplaceEffect` (the `Plus` operator, not just `Twice`),
`TestGainLifeNotReplacedByChainedReplaceEffect`, `TestGainLifeNotReplacedByTargetAbilityWithSubAbility`. Every fire-side
test, both `SubAbility$` refusals, and the `ReplaceCount` branch of `resolveGainLifeReplacementAmount` itself were
regression-checked by temporarily removing the corresponding code path and confirming the suite fails exactly as
expected before restoring it.

---

## Replacement effects: entering the battlefield tapped

CR 614's own replacement-effect system (`forge-game/src/main/java/forge/game/replacement/`, 3,742 LOC across
`ReplacementHandler`/`ReplacementEffect`/44 per-event `ReplaceXxx` subclasses, `ReplaceMoved` among them) had zero real
content until now: item 26's own closing line named it "the whole replacement-effect system remains a gap." A corpus
tally (44 files, `Event$` on `R:` lines) puts it at 2,210 real lines across 1,646 cards -- comparable in size to the
trigger vocabulary above, and led by `Event$ Moved` (969, CR 614.1's own "replace a zone-change event") ahead of
`DamageDone` (218), `Untap` (158) and `Counter` (118).

`ReplaceWith$`'s own value distribution on `Event$ Moved` lines turned up the single largest atomic real shape in the
entire replacement-effect corpus: `ETBTapped` (617) and `LandTapped` (135) together are 752 of 969 Moved lines --
"enters the battlefield tapped," MTG's own most common rules text, unimplemented in this port until now. Reading each
named SVar's own body (`grep SVar:ETBTapped:`, six distinct bodies corpus-wide) found the real shape underneath the
name: `DB$ Tap | Defined$ Self | ETB$ True` (587 of 618 real ETBTapped lines) or the identical shape naming
`Defined$ ReplacedCard` instead (31, the "another permanent enters tapped" shape -- `ReplacedCard` is Java's own way of
naming "the card actually moving" when the replacement's own host is a different permanent). Six lines chain a
`SubAbility$` (a counter grant) and stay unresolved.

`LandTapped`'s own 140 real `DB$ Tap` lines carry a Condition-family param too -- Rootbound Crag's own checkland text,
"enters tapped unless you control a Mountain or a Forest" -- and are resolved now: `subAbilityConditionMet`
(`condition.go`, new) ports `SpellAbilityCondition.areMet`'s own gate, trimmed to the two shapes these lines actually
use (`ConditionPresent$`/`ConditionCompare$`, a zone-presence count, 106/103 of the 140; `ConditionCheckSVar$`/
`ConditionSVarCompare$`, a named-SVar comparison, 34/33) -- SpellAbilityCondition is a wholly different Java class from
`CardTraitBase.meetsCommonRequirements` a trigger already checks (`triggerCommonRequirementsMet`, above), gating an
_ability's own resolution_ rather than whether a trigger fires at all, but the two families share the identical param
shapes under different key names: `isPresentMatches`/`checkSVarMatches` (trigger.go) were both already parameterized, or
made so, by the exact key names each caller needs (`checkSVarMatches` gained `checkKey`/`compareKey`/`secondKey`
parameters once this became its second caller), so `subAbilityConditionMet` is a thin wrapper passing
`"ConditionPresent"`/`"ConditionCompare"`/`"ConditionDefined"`/`"ConditionZone"` and
`"ConditionCheckSVar"`/`"ConditionSVarCompare"`/`"OrOtherConditionSVarCompare"` rather than new logic.

A few of the 140 carry a param past that pair, and skip the whole line via `subAbilityUnresolvedParams` (condition.go)
and `tapAbilityResolvesTap`'s own allow-list (below) rather than tapping unconditionally and guessing wrong
(PORT-8/GO-7): applying half of "enters tapped unless you control a Mountain" would be a wrong answer, not a partial
one.

- 15 carry `SubAbility$`, a chained counter grant -- `resolveSubAbility`'s own chaining mechanism ("SubAbility chaining
  itself lands," further below) is specific to a stack-resolving `Ability`; a replacement effect applies inline, outside
  `Registry.Resolve` entirely, so it would need its own separate integration this port does not have.
- 7 carry `ConditionDefined$`, an arbitrary reference with no Defined$-to-objects resolver built.
- 2 carry `ConditionPlayerTurn$` or `ConditionPhases$`, each its own mechanic.

A third Condition-family shape, the plain `Condition$` flag (SpellAbilityCondition's own separate
Threshold/Metalcraft/... switch), carries zero real `DB$ Tap` lines and stays unresolved for the identical reason.

`checkMovedReplacement` (`replacement.go`, new) resolves the 618 unconditional and 116 of the 140 conditional lines. It
reads `Face.Replacements` (`compile.go`) -- M3's own compiled field, typed identically to `Face.Triggers`/`Face.Statics`
since `R:` lines share the exact `Key$ Value` grammar, compiled the same way, and never read by the engine before now.
`ReplaceWith` is already one of `subAbilityKeys` (compile.go), so `ReplaceWith$ ETBTapped` resolves to `Ability.Subs`
for free -- zero new compiler work, the SVar it names already sitting there the identical way a trigger's own `Execute$`
sub-ability does.

Checked against two sets of Replacements, the identical own/other split `checkETBTriggers`/`otherETBTriggerMatches`
(above) already established: the moved card's own (`ValidCard$ Card.Self`, 587 of 618) and every OTHER battlefield
permanent's (`ValidCard$ Creature.OppCtrl`/`Land.OppCtrl`/..., 31 of 618 -- a static "creatures your opponents control
enter tapped" effect). `Destination$`/`Origin$`, present on 624 and 2 of the real ETBTapped-named lines respectively,
are checked only when present (`replacementZoneMatches` -- unlike `trigger.go`'s own `hasZone`, which a trigger's
`isETBTrigger`/`isDiesTrigger` always require, `ReplaceMoved.java`'s own `hasParam` guard around each check means an
absent one is unrestricted, not a non-match). `origin` is threaded in from each of the three real "enters the
battlefield" call sites -- `permanentEffect`/`attachEffect` (castspell.go), `Game.PlayLand` (land.go) -- read off the
card's own `Zone` field before `Game.Move` changes it, the one piece of state `checkETBTriggers` never needed since no
trigger mode this port resolves reads a moved card's own `Origin$`.

Called right after `Game.Move`, before `checkETBTriggers`: CR 614.1 puts a replacement before CR 603's own triggers, so
a tapped-on-entry permanent must already be tapped by the time a "when this enters" trigger looks at it -- the identical
ordering concern `applyContinuousControl` running first among the six continuous appliers already proved matters (item
27, above), applied here between two different mechanisms instead of within one. Unlike a trigger match, no APNAP
ordering or collect-then-push step is needed: the one outcome this file produces, `Card.Tapped = true`, is idempotent,
so CR 616's own "more than one replacement effect could apply, the affected player chooses" procedure -- needing a
`PlayerController` hook this port does not have, the identical gap a single player's own multiple simultaneous triggers
already has (above) -- has no observable answer to get wrong here: the first match found in either loop is applied and
the search stops immediately.

`TestPlayLandEntersTappedViaReplacement`/`TestCastSpellCreatureEntersTappedViaReplacement` (replacement_test.go) prove
the two real call sites; `TestCheckMovedReplacementAppliesToOtherPermanentsEntering` proves the "other" half;
`TestPlayLandDoesNotEnterTappedWhenDestinationDoesNotMatch` and `TestCheckMovedReplacementSkipsSubAbilityChain` prove
two of the ways a line does not resolve at all; `TestCheckMovedReplacementTapsCheckland`/
`TestCheckMovedReplacementDoesNotTapChecklandWhenConditionUnmet` and
`TestCheckMovedReplacementTapsWhenConditionCheckSVarIsMet`/`TestCheckMovedReplacementDoesNotTapWhenConditionCheckSVarIsNotMet`
each prove a met/unmet pair for the two now-resolved Condition-family shapes.

Not resolved: `ETBTapped`/`LandTapped` naming a `SubAbility$`, `ConditionDefined$`, `ConditionPlayerTurn$`,
`ConditionPhases$` or `Condition$` itself (above); `ReplaceWith$ Exile`/`DBTap`/`DBExile`/`DoDay`/`PayBeforeETB`/...
(352 of the remaining 969 Moved lines); `Untap`'s and `DamageDone`'s own `ReplaceWith$`-driven remainders (above);
`Draw`'s and `GainLife`'s own `ReplaceWith$`-driven remainders (33 and 16 real lines respectively, 3 and 4 of their own
36 and 20 real totals resolved by `## Draw`'s own `ReplaceWith$` and
[`## GainLife`](#gainlifes-own-replacewith-the-one-runtime-value-draws-dispatch-never-needed)'s own `ReplaceWith$`, both
further below); every `Event$` value past `Moved`/`Untap`/`DamageDone`/`Draw`/`GainLife` (`Counter` -- CR 701.5's own
"can't be countered," not the +1/+1-counter mechanic, moot until this port builds a spell-countering effect to protect
against; `AddCounter`, `CreateToken`, `BeginPhase`, `GameLoss`, `ProduceMana`, ... -- most naming a
`DB$ ReplaceCounter`/`ReplaceEffect`-shaped special replacement-modifying ability this port does not have a general
mechanism for, distinct from an ordinary script effect); and CR 616's own general layering/ordering procedure entirely,
moot for every resolved shape's own idempotent outcome but real the moment a second resolvable replacement effect
produces a different one.

`enginelint.json` gained a `"replacement"` group (`replacement.go`), added to `"land"`'s and `"castspell"`'s own allow
lists (both now call `checkMovedReplacement`) and, like `"trigger"`/`"continuous"` before it, to its own allow list an
`"ability"` entry it does not actually depend on: `compile.Ability` collides textually with the top-level `Ability`
declared in `ability.go` the identical way `ControlEffect.Player` once collided with the `Player` type (item 27's own
`ControlMod` paragraph) -- `enginelint`'s own identifier scan cannot tell a qualified external reference apart from an
unqualified same-package one. A new `"condition"` group (`condition.go`) sits between it and `"trigger"`:
`"replacement"` and `"dealdamageeffect"` both gained `"condition"` (`subAbilityConditionMet`'s two real callers), and
`"condition"` itself gained `"trigger"` (`isPresentMatches`/`checkSVarMatches`, both parameterized by key name once this
became their second caller) and the identical `"ability"` collision entry `compile.Ability` forces everywhere it
appears.

### `Event$ Untap`: CR 502.3/614.17's own "doesn't untap"

`untapStep`'s own doc comment (`turn.go`) had named this gap exactly the way `block.go`'s own named CantBlockBy:
"Effects that skip a permanent's untap (CR 502.3) are not modeled -- no card can grant that yet -- so every permanent
the active player controls untaps unconditionally." A corpus tally (`Event$ Untap` on `R:` lines) puts it at 158 real
lines, the corpus's own third most frequent `Event$` value behind `Moved` and `DamageDone`.

156 of those 158 carry `Layer$ CantHappen` -- CR 614.17's own dedicated layer, checked by `Card.canUntap`'s own
`cantHappenCheck` before `Untap.doUntap`'s own filter
(`untapList = CardLists.filter(untapList, c -> c.canUntap(active, false))`) ever considers a permanent for untapping at
all -- `ReplaceUntap.canReplace` ported directly: `ValidCard$` (present on every real line) matched against the affected
card, and `ValidStepTurnToController$` (154 of 156, always `"You"`) matched against the untap-step's own active player
relative to the affected card's controller. The other 2 name `ReplaceWith$` instead, a genuine substitution -- not the
"the event doesn't happen" shape `Layer$ CantHappen` itself already is -- and stay unresolved.

Reading `Untap.java`'s own `doUntap` closely turned up why `ValidStepTurnToController$` needs no separate check here at
all: its main loop (`for (final Card c : untapList)`, `untapList` built from `active.getCardsIn(Battlefield)`) only ever
considers permanents the untap-step's own active player already controls, so "the untapping player is this card's own
controller" -- `"You"`'s own contract -- already holds by construction for every real value the param carries.
`untapStep` (turn.go) has the identical invariant (`g.Zone(Battlefield, g.activePlayer).Cards()`), so `untapBlocked`
(`replacement.go`, new) checks only `Layer$`/`ValidCard$`/`ActiveZones$` and leaves `ValidStepTurnToController$` unread
-- reading it would just re-check something the loop already guarantees. `Untap.java`'s own second loop
(`active.getAllOtherPlayers().getCardsIn(Battlefield)`, granting permission to untap an opponent's permanent via
`StaticAbilityUntapOtherPlayer`) is the one case where the param would matter, and is not built: no card grants that
permission in this port yet, the identical reason `untapStep`'s own prior doc comment already gave for the whole gap.

`untapBlocked` walks every player's own Battlefield and Command zones (`replacementZones`, new -- 105 of 156 real lines
name `ActiveZones$ Battlefield` explicitly, the corpus's own default when absent; 2 name `Command`, an emblem-shaped
host rather than a permanent) looking for a match against the card about to untap, the identical own/other split every
replacement and trigger check in this port already has, collapsed here into one loop since a lock's own host is never
the card it locks. The first match found blocks the untap outright and the search stops -- CR 616's own "more than one
could apply" choice producing the identical outcome (blocked) no matter which is picked, `checkMovedReplacement`'s own
reasoning applied to a different outcome. `IsPresent$` (4) now resolves too, folded in through
`replacementRequirementsCheck` ("`ReplacementEffect.requirementsCheck` lands," above) --
alirios_enraptured.txt's/merseine.txt's/cocoon.txt's/winters_rest.txt's own real zone-scan conditions among them.
`CheckSVar$`/`SVarCompare$` (1, walking_dream.txt's own "if an opponent controls two or more creatures") also reaches
the general gate now rather than being skip-listed, but does not actually produce the CR-correct answer: its own `X`
SVar is `PlayerCountOpponents$HighestValid Creature.YouCtrl`, a distinct SVar family `resolveNamedAmount` does not
evaluate (the `Count$Valid` family `resolveAmount` resolves is a different one), so `checkSVarMatches` fails closed
(condition never met) and this one card's own lock never actually blocks anything -- the identical observable behavior
it had before this chunk (skip-listed then, unresolvable-and-thus-unmet now), just reached through the honest general
mechanism instead of an ad-hoc allow-list rejection. `EnduringStory$`/`AddSVar$` (2 of 156, each its own further
restriction this file cannot evaluate) still skip the whole line rather than blocking unconditionally (PORT-8/GO-7) --
an allow-list of exactly the params real corpus lines pair with this shape
(`Event$`/`Layer$`/`Description$`/`ValidCard$`/`ValidStepTurnToController$`/`ActiveZones$`/`Secondary$`, plus the keys
`replacementRequirementsCheck` itself reads), the identical style `tapAbilityIsPlainTap` already has, rather than a
reject-list of the ones found: a param neither list has seen yet skips by construction instead of silently passing.

`untapStep` (turn.go) calls `untapBlocked` per permanent before clearing `Tapped`, leaving `SummonSick`'s own clearing
unconditional: CR 502.3's own "doesn't untap" restricts only the untapping action, not CR 302.6's own continuous-control
question a doesn't-untap lock has nothing to do with.

`TestUntapBlockedBySelfCantHappenReplacement` proves the simplest real shape and the SummonSick independence;
`TestUntapBlockedByOpponentsCantHappenReplacement` proves the "other" half; `TestUntapBlockedWhenHostInCommandZone`
proves the `ActiveZones$` generalization; `TestUntapNotBlockedWhenValidCardDoesNotMatch` and
`TestUntapNotBlockedByReplaceWithShape` (replacement_test.go) prove two of the ways a line does not block;
`TestUntapBlockedWhenIsPresentConditionMet`/`TestUntapNotBlockedWhenIsPresentConditionNotMet` prove the new `IsPresent$`
fold-in both ways ("`ReplacementEffect.requirementsCheck` lands," above, has the full account).

### `Event$ DamageDone`: CR 614's own "prevent all of this damage"

218 real `Event$ DamageDone` lines, the corpus's own second most frequent `Event$` value. 72 name `Prevent$ True`; the
other 146 name `ReplaceWith$`, split by the target's own underlying `DB$` API (each counted only when a literal
top-level `R:` line actually names it, `face.Replacements`' own walk): `DB$ ReplaceEffect` (71 total, split by its own
`VarName$` -- 59 name `VarName$ DamageAmount`, raphael_the_muscle.txt's own "double all damage" among them, a
doubling/adding/subtracting-a-computed-amount-from-the-in-flight-`DamageAmount$` API, its own section below; the other
12 name `VarName$ Affected`/`LifeGained`/`Number`/`Ignore` instead, an entirely different substitution -- redirecting
who is damaged or what else changes, not resizing the damage itself -- still unresolved), `DB$ ReplaceDamage` (27,
"prevent N of that damage" -- a flat reduction, not a doubling; its own section below), `DB$ RemoveCounter` (17) and
`DB$ PutCounter` (14, "damage becomes counters" instead, 31 combined -- its own section below too), and a dozen smaller
shapes (`DB$ Mill`/`ChangeZone`/`Dig`/`ImmediateTrigger`/`RollDice`/`Draw`/`Token`/`Sacrifice`/`ChoosePlayer`/
`GainLife`/`HealDamage`, 1-3 lines each) -- no shape past those three anywhere near `ETBTapped`'s own 618-line
concentration, so none of the rest was worth building on its own; those 49 stay unresolved.

`Prevent$ True` needed no sub-ability at all to resolve, `ReplacementHandler.java`'s own dispatch read directly: a
`Prevent$ True` (or `PreventionEffect$`) line returns `ReplacementResult.Prevented` straight off `getParam("Prevent")`,
nothing replaces the event, it simply does not happen -- the identical "the event doesn't happen" shape `Event$ Untap`'s
own `Layer$ CantHappen` already is, for a different `Event$` value. `ReplaceDamage.canReplace` is the resolvable half
read for its own `canReplace` gate -- `ValidSource$`/`ValidTarget$` matched the identical way `damageDoneMatches`'s own
pair already is (trigger firing, above), split into a `*Card`/ `*Player` pair for the reason
`checkDamageDoneTriggersToCard`/`ToPlayer` already are (`damagePrevented`/ `damagePreventedPlayer`, `replacement.go`,
new), and `IsCombat$` compared against a hardcoded `true` the identical way every real `DamageDone` trigger check
already does, since nothing outside combat deals damage in this port yet. `PlayerTurn$` (4), `CheckSVar$`/`SVarCompare$`
(2) and `IsPresent$` (1) now resolve too, the identical `replacementRequirementsCheck` fold-in `untapReplacementMatches`
above just got --
guardian_naga_banishing_coils.txt's/gideon_blackblade.txt's/frodo_determined_hero.txt's/personal_sanctuary.txt's own
real "can't be dealt damage during your turn" lines among the `PlayerTurn$` four. `DamageAmount$` (1 of 72 --
callous_giant.txt's own "if a source would deal 3 or less damage to CARDNAME, prevent that damage") resolves too now,
reusing `damageAmountMatches` (trigger firing, above) against the ORIGINAL amount about to be dealt -- the identical
operator/operand split a trigger's own `DamageAmount$` already reads once damage actually happens, just consulted here
before the replacement decides whether to apply at all. The remaining 2 of 72 stay unresolved: one names
`ValidCause$`/`CauseIsSource$` together (a SpellAbility comparison `Matches` cannot evaluate), the other
`RelativeToSource$` (a GameEntity-vs-GameEntity relative match this port has no evaluator for) -- each its own further
restriction `ReplaceDamage.canReplace` itself reads but this file cannot, still skipping the whole line via the
identical allow-list style `untapReplacementMatches` above has, rather than preventing unconditionally.

`dealPermanentDamage`/`dealPlayerDamage` (combatdamage.go) call `damagePrevented`/`damagePreventedPlayer` first, before
marking any damage, emitting `DamageDealt`, or checking CR 603's own "deals damage" trigger -- a prevented damage
instance never happened at all, the identical "look at the event before it happens" ordering CR 614.1 already has over
CR 603 for `checkMovedReplacement`, applied here to a different `Event$` value and a different outcome (nothing, rather
than `Tapped = true`).

`TestDamageToPlayerPreventedByReplacement`/`TestDamageToCreaturePreventedByReplacement` (replacement_test.go) prove the
`*Player`/`*Card` split; `TestDamageToPlayerNotPreventedWhenValidTargetDoesNotMatch`/
`TestDamageToCreatureNotPreventedWhenValidSourceDoesNotMatch` prove `ValidTarget$`/`ValidSource$` are checked, not
assumed; `TestDamageToCreatureNotPreventedByUndefinedCheckSVar` proves an unresolvable `CheckSVar$` reference fails
closed rather than preventing; `TestDamageToPlayerPreventedWhenPlayerTurnMatches`/
`TestDamageToPlayerNotPreventedWhenPlayerTurnDoesNotMatch` prove the new `PlayerTurn$` fold-in both ways;
`TestDamageToCreaturePreventedWhenDamageAmountGateMatches`/`TestDamageToCreatureNotPreventedWhenDamageAmountExceedsGate`
prove the new `DamageAmount$` gate both ways.

#### `DB$ ReplaceDamage`: CR 616's own "Updated" outcome, "prevent N of that damage"

`ReplaceDamageEffect.resolve` (Java) has two outcomes past `Prevent$ True`'s own "the event does not happen": if
reducing the incoming amount by `Amount$ N` brings it to zero or below, the event is `Replaced` -- the identical "no
damage happens" outcome `Prevent$ True` already gives; otherwise it is `Updated` -- the SAME `DamageDealt` event still
happens, marking/`checkDamageDoneTriggersToCard`/`ToPlayer`/emitting the event all still run, just against the smaller
number. Neither outcome existed in this port before now: `damagePrevented`'s own contract is a plain bool, nothing in
between "the whole event happens" and "none of it does."

`damageReplaced`/`damageReplacedPlayer` (`replacement.go`, new) are `damagePrevented`'s/`damagePreventedPlayer`'s own
siblings, called from `dealPermanentDamage`/`dealPlayerDamage` (combatdamage.go) right after them, reassigning the same
`amount` local both functions already thread through to marking/the event/the trigger check below -- a trigger matching
`DamageAmount$` (`damageAmountMatches`, trigger firing above) sees the number damage was actually reduced to, not the
number that would have applied before any shield intervened, CR 616's own ordering (replacement effects apply before the
event, so before anything downstream ever observes it). Each guards `amount <= 0` immediately after the reduction the
identical way the top of `dealPermanentDamage` already guards the original `amount`, folding a full reduction into the
identical early return `damagePrevented` already gives -- no separate `Replaced` state to carry.

Of the corpus's own 39 real `DB$ ReplaceDamage` SVar definitions, only 27 are ever named by a literal top-level `R:`
line at all -- `damageReplacementMatches`' own `face.Replacements` walk, the identical mechanism every other dispatch in
this file already uses, can only ever see these 27. 18 of them resolve end to end through `damageReplacementMatches`
(`DamageDone`, `ReplaceWith$` present, otherwise the identical
`ActiveZones$`/`ValidSource$`/`IsCombat$`/`DamageAmount$`/ `replacementRequirementsCheck` gate `damagePreventionMatches`
already has) and `applyDamageReplaceDamage` (the `applyDrawReplacement`-style "recognize the one shape, run it by hand"
dispatch, since neither call site can reach the `*Registry` chaining a `SubAbility$` would need): a plain integer
`Amount$`, no `SubAbility$` and no `DivideShield$` (a shield's own remaining capacity split across more than one
simultaneous instance of damage in the same resolution, a fold this dispatch keeps no state for) --
guardian_seraph.txt's/orbs_of_warding.txt's/the five Sphere-of-\*.txt's own real "prevent N of that damage" lines among
them, plus reidane_god_of_the_worthy_valkmira_protectors_shield.txt's/plated_pegasus.txt's own
`ValidTarget$ You,Permanent.YouCtrl`/`Permanent,Player` (`matchesPlayerSpec`'s own comma-OR split, below, closes their
own player-target half). The remaining 9 of the 27 name a named-SVar `Amount$` this dispatch cannot resolve
(`ShieldAmount`/`X`/`PaidAmount`/`AlchemicX`, each its own further mechanic -- a depleting shield counter that rewrites
its own `Number$` body downward as it absorbs hits, an X spent casting the spell, mana paid for an activated ability,
...), one of them (rock_hydra.txt's) also chaining its own `SubAbility$`.

The other 12 of the 39 corpus-wide `DB$ ReplaceDamage` definitions are never named by any literal `R:` line at all, so
this dispatch's own walk cannot reach them regardless of its own shape -- each blocked by a wholly different missing
mechanism: hedron_field_purists.txt's own 2 (`DBReplace1`/`DBReplace2`, one per Level-up tier) are referenced only
through a Layer 6 `AddReplacementEffect$` on a `Mode$ Continuous` line (`applyContinuousKeyword`'s own doc comment,
above, resolves `AddKeyword$` only, not this key), so the replacement text never becomes a compiled `Replacements` entry
at all. The other 10 (forcefield.txt's/ajani_steadfast.txt's/urza_academy_headmaster.txt's/
divine_deflection.txt's/refraction_trap.txt's/healing_grace.txt's/barbed_wire.txt's/dark_sphere.txt's/
tornellan_protector.txt's/torrent_of_lava.txt's own) are created dynamically at resolution time by an activated or
triggered ability's own `DB$ Effect | ReplacementEffects$ <SVar>` (CR 611.2c/113.6's "create an effect with an ability"
-- forcefield.txt's own real `{1}: The next time an unblocked creature... prevent all but 1 of that damage` is the
corpus's own example: an activated ability creates a temporary `Effect` object carrying the replacement text as one of
its own params, not as a card's own printed `R:` line) -- this port does not model `Effect` objects or `DB$ Effect`
itself at all (M6's own remaining script-effect territory), so there is no compiled card anywhere for
`face.Replacements` to have found this text on in the first place.

`TestDamageToPlayerReducedByReplaceDamage`/`TestDamageToCreatureReducedByReplaceDamage` (replacement_test.go) prove the
`*Player`/`*Card` split for the reduction itself; `TestDamageToPlayerReductionClampsAtZero` proves a reduction bigger
than the incoming damage stops at zero rather than going negative; `TestDamageToCreatureNotReducedByChainedSubAbility`/
`TestDamageToPlayerNotReducedByDivideShieldAmount` prove the two refusals (GO-7);
`TestDamageToPlayerNotReducedWhenValidSourceDoesNotMatch` proves `ValidSource$` gates the ReplaceWith$ shape the
identical way it already gates `Prevent$ True`. Every new gate was regression-checked by temporarily disabling it and
confirming the corresponding test failed with the expected wrong number before restoring it.

#### `matchesPlayerSpec`'s own comma-OR split -- closing `DamageDone`'s own mixed player/card `ValidTarget$`

`Player.isValid` (Java) splits its own restriction string on comma the identical way `Card.isValid` does --
`CardTraitBase.java:261`, the same split `internal/valid.Parse` already gives the `*Card` side (`valid.go`'s own doc
comment). `matchesPlayerSpec` (`valid.go`) never did: it cut the whole spec on the first `.` and looked the resulting
base up directly, so a comma-joined spec like `You,Permanent.YouCtrl` never matched `matchesPlayerBase`'s own literal
`You`/`Opponent`/`Player` table at all -- the whole string, comma included, was the "base." Every one of
`matchesPlayerSpec`'s nine callers across `trigger.go`/`replacement.go`/`continuous.go`/`targeting.go` inherited the gap
for free.

The real shape only ever mixes one player-shaped alternative with one card-shaped alternative in the same OR
(`reidane_god_of_the_worthy_valkmira_protectors_shield.txt`'s/`plated_pegasus.txt`'s own `DB$ ReplaceDamage` lines,
`gratuitous_violence.txt`'s own `DB$ ReplaceEffect` line, all three `DamageDone`'s own `ValidTarget$`, the paragraph
above/below) -- "prevent/double damage dealt to you or a permanent you control," "... to a permanent or player." Fixed
by splitting `spec` on comma inside `matchesPlayerSpec` itself, evaluating each alternative independently and OR-ing the
results: an alternative whose base is not `You`/`Opponent`/`Player` matches nothing rather than aborting the whole spec
-- `valid.Parse`'s own contract for a base it does not recognize (valid.go's own doc comment: "a base it does not
recognise simply matches nothing"), reused here rather than invented, since a card-shaped alternative like
`Permanent.YouCtrl` is not a player spec this port fails to understand, it is a spec for a different kind of object
entirely, the reason the OR combines the two in the first place. `ok` stays false only when no alternative matches and
at least one player-shaped alternative names a property `matchesPlayerProperty` does not recognize -- GO-7's usual "skip
rather than guess," now scoped to the alternative that actually needs it instead of the whole spec.

For a spec with no comma at all -- every existing caller's every existing test, all nine call sites' every real corpus
line still resolvable before this change -- the loop runs exactly once over the whole spec, identical to the prior
single `Cut`/`matchesPlayerBase` pair: no behavior changes for a spec this port already resolved.

`TestDamageToPlayerReducedByReplaceDamageWithCommaValidTarget` (replacement_test.go) proves reidane's/plated_pegasus's
own real `You,Permanent.YouCtrl` shape now prevents 1 of 3 combat damage dealt to a player, not just to a permanent;
`TestDamageToPlayerDoubledByReplaceEffectWithCommaValidTarget` proves gratuitous_violence.txt's own real
`Permanent,Player` shape now doubles combat damage dealt to a player too -- a real correctness fix, not just a new
resolution, since that card's own `ValidTarget$` was already counted resolved for its card-target half
(`DB$ ReplaceEffect`, below) and was silently never doubling a hit against a player at all before this. Both were
regression-checked by reverting `matchesPlayerSpec` to its own pre-split form and confirming each failed with the
pre-fix (undoubled/unreduced) life total before restoring it.

#### `DB$ ReplaceEffect`: CR 616's own "Updated" outcome, a computed replacement

`ReplaceEffect.java`'s own `resolve` has several `VarType$`-selected branches (Card/Player/GameEntity/Map/CardSet, each
redirecting a different replacing-object field to a different value); the default branch, taken when `VarType$` is
absent, is the one this dispatch resolves: `params.put(varName, AbilityUtils.calculateAmount(card, varValue, sa))`,
recomputing whichever field `VarName$` names as a plain number. For `VarName$ DamageAmount` specifically, that number is
the SAME `DamageAmount$` this dispatch's own two callers (`damageReplaced`/`damageReplacedPlayer`, above) already have
in scope as the pre-replacement amount -- CR 616's own "Updated" outcome again, computing a new number rather than
skipping the event (`Prevent$ True`) or reducing it by a flat amount (`DB$ ReplaceDamage`, above). `VarValue$` is either
a plain integer (a flat replacement, ignoring the original amount entirely) or a named SVar whose own body is
`ReplaceCount$DamageAmount/<operator>` -- `AbilityUtils.calculateAmount`'s own `ReplaceCount$` branch
(`root.getReplacingObject(AbilityKey.fromString(l[0]))`, the game's own replacing-object map; the pre-replacement amount
already in scope stands in for it directly) feeding `AbilityUtils.doXMath` for the operator suffix.
`resolveReplaceCountAmount`/`applyDamageReplaceEffect` (`replacement.go`, new) port the five `doXMath` branches every
real corpus line pairs with this shape: `Twice`/`Thrice` (multiply), `HalfDown` (integer-divide by 2, safe since damage
is never negative), and `Plus`/`Minus` (a literal digit or a further-resolvable SVar operand, `resolveNamedAmount`
reused the identical way `applyDamageReplaceDamage`'s own `Amount$` already is) -- no other `doXMath` operator
(`HalfUp`, `ThirdUp`/`Down`, `Negative`, `Times`, `Pow`, `Divide*`, `Mod`, `Abs`, `LimitMax`/`Min`) pairs with a real
`ReplaceCount$DamageAmount` line. The result is clamped at 0 defensively, matching `applyDamageReplaceDamage`'s own
contract, though every real caller already guards the original amount positive before either dispatch runs.

Of the 59 real lines naming `DB$ ReplaceEffect | VarName$ DamageAmount`, 56 resolve end to end:
raphael_the_muscle.txt's/ gratuitous_violence.txt's/furnace_of_rath.txt's/... own real "double all damage" (`Twice`, the
corpus's own dominant shape), torbran_thane_of_red_fell.txt's own "plus 2" (`Plus.2`),
benevolent_unicorn.txt's/lashknife_barrier.txt's own "minus 1" (`Minus.1`), ghosts_of_the_innocent.txt's own "half,
rounded down" (`HalfDown`), fiery_emancipation.txt's/ city_on_fire.txt's own "triple" (`Thrice`), and
forethought_amulet.txt's/divine_presence.txt's own flat "deals N damage instead" (a plain integer `VarValue$`, gated by
the R: line's own `DamageAmount$` threshold matching the ORIGINAL amount -- the identical `DamageAmount$` gate
`damagePreventionMatches`'s own new fold-in just added, `damageReplacementMatches` above, folded in here too since these
two real lines are the only `ReplaceWith$` shape that needs it). 3 stay unresolved:
fated_firepower.txt's/hawkeye_young_avenger.txt's own `Plus.Y` operand (`Count$CardCounters.FIRE`/`Count$CardPower`,
neither the Valid family `resolveAmount` evaluates -- an amount head this port has no evaluator for, not anything
specific to this dispatch) and ojer_axonil_deepest_might_temple_of_power.txt's own bare `Count$CardPower` `VarValue$`
(no `ReplaceCount$` at all -- damage set equal to the host's own power, not measured off the original amount at all, the
identical unsupported `Count$CardPower` head). The other 12 of the 71 corpus-wide `DB$ ReplaceEffect` references naming
`VarName$` something other than `DamageAmount` (`Affected`/`LifeGained`/`Number`/`Ignore`) are filtered out by requiring
`VarName$ DamageAmount` specifically, not resolved by anything in this file.

`TestDamageToPlayerDoubledByReplaceEffect`/`TestDamageToCreatureTripledByReplaceEffect` (replacement_test.go) prove the
`*Player`/`*Card` split for `Twice`/`Thrice`; `TestDamageToPlayerReplaceEffectPlusLiteral`/
`TestDamageToPlayerReplaceEffectMinusClampsAtZero`/`TestDamageToPlayerReplaceEffectHalfDown` prove the remaining three
operators, the Minus case also proving the same zero-clamp `TestDamageToPlayerReductionClampsAtZero` already proves for
`DB$ ReplaceDamage`; `TestDamageToPlayerReplaceEffectFlatReplacementAboveGate`/
`TestDamageToPlayerReplaceEffectFlatReplacementBelowGateNotApplied` prove the flat-integer shape and its own
`DamageAmount$` gate both ways; `TestDamageToPlayerReplaceEffectUnresolvedOperandNotApplied` proves an unresolvable
`Plus` operand refuses outright (GO-7) rather than treating it as zero. Every new gate and branch was regression-checked
the identical way `DB$ ReplaceDamage`'s own were.

#### `DB$ RemoveCounter`/`DB$ PutCounter`: CR 616's own "Replaced" outcome

A third real `ReplaceWith$` shape, and a different CR 616 outcome from either of the first two:
`ReplacementHandler.java`'s own dispatch (line ~360) sets `ReplacementResult.Updated` only for `ApiType.ReplaceDamage`/
`ReplaceSplitDamage`/`ReplaceEffect` (`damageReplaced`'s/`applyDamageReplaceEffect`'s own outcomes, above) or
`ReplaceToken`/`ReplaceMana` (not built) -- every other `ApiType`, `RemoveCounter`/`PutCounter` among them, gets the
default `ReplacementResult.Replaced`: the ORIGINAL `DamageDone` event does not happen at all (no marking, no event, no
trigger check), a counter changes on some object INSTEAD -- the identical "different thing happens, not a smaller
version of the same thing" shape `drawReplaced`'s/`gainLifeReplaced`'s own substitutions already have, not
`damageReplaced`'s/`applyDamageReplaceEffect`'s own reduced-or-computed-but-still-damage shape.
`applyDamageReplaceCounter` (`replacement.go`, new) reports whether a recognized shape ran; `damageReplaced`/
`damageReplacedPlayer` return `amount` 0 when it does, the same "nothing left to mark" outcome a full
`DB$ ReplaceDamage` reduction already folds into.

`Defined$` resolves three ways: `Self` (the replacement's own host -- the dominant real shape, every "Phantom"
creature's own real "if damage would be dealt to CARDNAME, prevent that damage. Remove a +1/+1 counter" among them, 10
real cards sharing the identical line); `Equipped` (panther_habit.txt's own real "if equipped creature would be dealt
damage, prevent that damage and put that many +1/+1 counters on it," `Card.AttachedTo()` -- `applyContinuousNames`'s own
precedent, reused outright); and `ReplacedTarget` (the object the damage would have hit, threaded straight through from
`damageReplaced`'s/`damageReplacedPlayer`'s own `target` parameter -- soul_scar_mage.txt's own real "if a source you
control would deal noncombat damage to a creature an opponent controls, put that many -1/-1 counters on that creature
instead," the counters landing on the DAMAGED creature, not the replacement's own host). `CounterType$` reuses
`putCounterType` (`putcountereffect.go`) outright -- `RemoveCounter`/`PutCounter` share the identical param.
`CounterNum$` resolves through `resolveReplaceCountAmount` (above), now generalized to accept a bare, operator-less
`ReplaceCount$DamageAmount` -- `AbilityUtils.doXMath`'s own `operators == null` identity, `original` unchanged -- the
dominant real shape for THIS dispatch specifically (lichenthrope.txt's/phytohydra.txt's own real `CounterNum$ X`,
`SVar:X:ReplaceCount$DamageAmount`, among 20 of the 25 resolvable lines), distinct from `applyDamageReplaceEffect`'s own
suffixed `Twice`/`Plus`/... branches, which 0 real lines here pair with. `SubAbility$` refuses outright, the identical
chained-target refusal every other hand-run dispatch in this file already gives (5 real lines, underdark_beholder.txt's
own "remove counters, then sacrifice if none left" among them). `AlwaysReplace$`/`ExecuteMode$` (real on several of
these 25 lines) are allow-listed in `damageReplacementMatches` as pure no-ops: `ReplacementHandler.java`'s own dispatch
only reads `AlwaysReplace$` when `NoPreventDamage` is set on the runParams -- a "damage can't be prevented" flag this
port's own damage pipeline has no equivalent state for at all -- and `ExecuteMode$`'s own `PerSource`/`PerTarget` split
only matters when Java batches more than one simultaneous damage instance into one replacement pass, something this
port's own `dealPermanentDamage`/`dealPlayerDamage` never do (each call is already exactly one source and one target).

26 of the corpus's own 31 real `Event$ DamageDone` lines naming `DB$ RemoveCounter`/`DB$ PutCounter` resolve end to end.
The 5 chaining `SubAbility$` above stay unresolved. jared_carthalion_true_heir.txt's R: line naming
`CheckDefinedPlayer$ You.isMonarch` resolves: `damageReplacementMatches` reads `CheckDefinedPlayer$`/
`DefinedPlayerCompare$` against `Game.monarch` (R9n, [`m6-r9n-checksvar-heads.md`](m6-r9n-checksvar-heads.md)).

`TestDamageToCreatureReplacedByRemoveCounter` proves the "Phantom" shape (`Defined$ Self`);
`TestDamageToPlayerReplacedByPutCounterUsingBareReplaceCount` proves the bare `ReplaceCount$DamageAmount` read;
`TestDamageToCreatureReplacedByPutCounterOnReplacedTarget`/`TestDamageToCreatureReplacedByPutCounterOnEquipped` prove
`Defined$ ReplacedTarget`/`Equipped`; `TestDamageToCreatureNotReplacedByRemoveCounterWithSubAbility` proves the
`SubAbility$` refusal; `TestDamageToPlayerNotReplacedByPutCounterNamingCheckDefinedPlayer` proves the
`CheckDefinedPlayer$` gap end to end against the real corpus card's own shape. Every new gate was regression-checked by
temporarily disabling it and confirming the corresponding test failed with the expected wrong `Damage.Marked`/counter
count before restoring it.

All five shapes -- `Prevent$ True`, both halves each of `ReplaceWith$ DB$ ReplaceDamage`/`DB$ ReplaceEffect`, and
`DB$ RemoveCounter`/`DB$ PutCounter` -- share `replacementActiveZones`/`hostInActiveZones` (`replacement.go`),
generalizing `ActiveZones$` past Battlefield alone -- absent means Battlefield, a comma list otherwise, an unrecognized
zone name skipping the whole line the identical contract `validCountZones` (amount.go) already has for a
`Count$Valid<Zone>` suffix -- `replacementZones`, the `[Battlefield, Command]` pair `untapBlocked`/`damagePrevented`/
`damagePreventedPlayer`/`damageReplaced`/`damageReplacedPlayer` all walk across every player, since 2 real lines of each
shape name a Command-zone host -- and now `damageAmountMatches` (trigger firing, above), the identical operator/operand
split a trigger's own `DamageAmount$` reads, reused by `damagePreventionMatches` and `damageReplacementMatches` alike
against the ORIGINAL amount before either decides whether its own line applies at all.

## `Event$ Moved` to the graveyard

CR 614.1, `ReplaceMoved.java`. 91 real `R:` lines name `Event$ Moved | Destination$ Graveyard` (Rest in Peace, Leyline
of the Void, Binding Geist, Wheel of Sun and Moon, ...); the port applied none, only the battlefield-entry "enters
tapped" shapes. `Game.Move` cannot reach replacements (`game.go` cannot depend on `replacement.go`), so every "put into
a graveyard" site calls `moveToGraveyard` (`replacement.go`) instead: 17 sites in `action.go`, `turn.go`, `stack.go` and
the destroy, sacrifice, discard, mill and surveil effects, plus `moveByEffect` for a `ChangeZone` to the graveyard.

`movedGraveyardDestination` finds the first `Event$ Moved` replacement in play (`eachReplacement`, CR 616's
simplification) naming `Destination$ Graveyard` whose `Origin$`, `ValidCard$` and `ValidLKI$` match the card (each
absent is a pass; `ValidLKI$`, 28 real lines, is matched against the card as it is, since this runs before the move) and
whose requirements hold. It applies the `ReplaceWith$` when that is a bare `DB$ ChangeZone | Defined$ ReplacedCard` to
Exile or Hand (79 of 91 lines). Any other param, a `SubAbility$` (Kalitas' Zombie token), a Library destination's
position and shuffle, or `Cycling$` is an unmodelled consequence: the line is not applied and `Game.recordPendingError`
names the card (GO-7), never a half-replacement.

`checkDiesTriggers` returns at once for a card that is not in the graveyard: an exiled card did not die, and an exile
trigger fires instead (`checkExiledTriggers`). Tests: `movedtograveyard_test.go`; scenarios
`replacement-rest-in-peace-...` and `replacement-leyline-of-the-void-...`.

## `CantGainLife` and `CantDraw` statics

Two player-restriction statics (plus `CantChangeLife`'s gaining half), ported from
`StaticAbilityCantGainLosePayLife.anyCantGainLife` and `StaticAbilityCantDraw.canDrawThisAmount`, next to the `Prevent$`
replacements for the same events. They share `playerStatic` (`staticability.go`): a static of the Mode on a battlefield
or Command-zone host whose `Condition$` holds (`continuousConditionMet`) and whose `ValidPlayer$` names the player
(`matchesPlayerSpec`; absent means everyone, as `matchesValidParam` does). A line carrying `IsPresent$` or `CheckSVar$`,
or a `ValidPlayer$` `matchesPlayerSpec` does not recognize (`Player.EnchantedBy`), is skipped, never assumed to hold
(GO-7).

| Static                       | Where it bites                                                                                                                       |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `CantGainLife` (20 cards)    | `Game.gainLife` (`gainlifeeffect.go`): `GainLife`, lifelink, and `SetLife`/exchange gains                                            |
| `CantDraw` (7), `DrawLimit$` | each iteration of `Game.DrawCards` (`turn.go`), so the draw step and `Draw` obey it; `Draw<N/You>` unless-costs ask `cantDrawAmount` |

`DrawLimit$` defaults to 0 (no draws); `N` allows `N` draws a turn: the next draw is refused once `CardsDrawnThisTurn`
reaches it. `setPlayerLife` now calls `gainLife` for its gain half, which also gives it Java's `isInGame` check. Tests:
`cantgaindraw_test.go`. Not ported: `CantPutCounter`, `CantPlayLand`, `CantBeActivated`, `CantBeCast` (see
`Not ported yet`).

## `CantPutCounter`

`StaticAbilityCantPutCounter` (10 cards) stops counters of a kind being put on a card or a player. `cantPutCounter`
(`staticability.go`) is its `anyCantPutCounter`: `countersReplaced` (`replacement.go`) asks it first, before any
`AddCounter` replacement, so `PutCounter`, wither and infect damage, and every other counter placement that goes through
that funnel are covered. A card is named by `ValidCard$` (a line with `ValidPlayer$` is the player half), a player by
`ValidPlayer$`; an absent `CounterType$` names every kind; `AffectedZone$` is ignored, as Java ignores it outside
`Continuous`. Placements that write `Counters.Add` directly (a permanent entering with counters, loyalty and defense
counters, `Proliferate`, move/double effects) do not ask it yet. Tests: `cantputcounter_test.go`.

## `CantPreventDamage`

`Card.canDamagePrevented` (`Card.java:6116`) asks `StaticAbilityCantPreventDamage` before any prevention applies
(`GameEntity.staticDamagePrevention`, `ReplacementHandler.java:399`). `canDamagePrevented` (`staticability.go`) ports it
(11 cards: Leyline of Punishment, Everlasting Torment, Rampaging Wurm-style "can't be prevented"): a static whose
`IsCombat$` equals the damage's combat flag and whose `ValidSource$` matches the source, the source's own statics
included (`Spell.Self` lines on the stack). `dealPermanentDamage` and `dealPlayerDamage` (`combatdamage.go`) then skip
the `Prevent$` replacements, `Fog`'s combat prevention and prevention shields; `damageReplaced` (redirects, splits) is
not prevention and still runs. A line with `CheckSVar$` or another unlisted param is not applied. Tests:
`cantpreventdamage_test.go`.

## "Enters tapped unless" costs (shock lands, reveal lands)

`DB$ Tap | ETB$ True | UnlessCost$ ... | UnlessPayer$ You` (47 real lines: 27 `PayLife<2>`, 19 `Reveal<1/...>`, 1
`Behold`) is `tapAbilityResolvesTap`'s third shape (`replacement.go`), beside the bare tap and the Condition-gated
checkland tap. `payEntersTappedUnless` is `AbilityUtils.handleUnlessCost` for it: the payer is the host's controller
(`UnlessPayer$ You` only), is asked `ConfirmPayCost` when the cost is payable (`unlessPayable`: life at least 2, a
matching card in hand), and pays through `payUnlessCost`. Paying keeps the land untapped. A cost `parseUnlessCost` does
not read (`Behold`), or another payer, records a pending error and the land enters untapped (GO-7). Before this shape
was read these lines matched nothing and every shock land entered untapped without a question. `checkMovedReplacement`
takes the controller for the decision. Tests: `enterstappedunless_test.go`, scenarios
`shock-land-paid-enters-untapped-and-costs-two-life`, `shock-land-declined-enters-tapped`.
