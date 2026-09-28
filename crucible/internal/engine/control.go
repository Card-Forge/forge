// The controller interface: where the game asks a player to decide
// something, and the fixture-driven implementation that answers from a
// script instead of a person or an AI.

package engine

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/mana"
)

// PlayerController is where the game asks a player to decide something.
// Ported from forge-game/src/main/java/forge/game/player/PlayerController.java,
// which has 110 abstract methods; only the thirty answerable with today's
// engine are here.
//
// The rest need SpellAbility, targeting, replacement effects and the rest of
// cost payment -- types that do not exist until the stack and layer system
// fully land in M5. Each is added when its own caller is, the same as these
// twenty-four: mulligans and the starting-player choice have callers in
// GameAction and mulligan/, even though neither is ported yet, and
// ChooseLegendaryToKeep's, DeclareCombatAttackers's, ChooseAttackTarget's,
// DeclareCombatBlockers's, AssignCombatDamage's, DiscardToHandSize's,
// ChooseBattleProtector's, ChooseHybridManaColor's,
// ChoosePayMonocoloredHybrid's, ChoosePayColorlessHybrid's,
// ChoosePayPhyrexian's, ChoosePayHybridPhyrexian's, ChoosePayGeneric's,
// ChoosePayX's, ChoosePaySnow's and ChooseEnchantTarget's own callers
// (resolveLegendRule, action.go; Game.DeclareCombatAttackers and
// Game.chooseAttackTargets, attack.go; Game.DeclareCombatBlockers, block.go;
// Game.DealCombatDamage, combatdamage.go; Game.cleanupStep, turn.go;
// assignBattleProtector, action.go; Game.PayManaCost, manapay.go, eight
// times over; Game.CastSpell, castspell.go) are fully built, so the decision
// point can be built ahead of them (Plan Section 1.3). ChooseCardsToDiscard's
// own caller (discardEffect.Resolve, discardeffect.go) is the first
// Effect implementation to need one at all -- Effect.Resolve gained a
// PlayerController parameter for it (effect.go's own doc comment).
// ArrangeForScry's own caller (scryEffect.Resolve, scryeffect.go) is the
// second, ArrangeForSurveil's own caller (surveilEffect.Resolve,
// surveileffect.go) is the third, and ChoosePermanentsToSacrifice's own
// caller (sacrificeEffect.Resolve, sacrificeeffect.go) is the fourth.
// ChooseTargets's own caller (resolveTargets, targeting.go) is not another
// Effect at all -- it runs before an ability is even pushed. ConfirmOptionalTrigger's
// own caller (Registry.Resolve, effect.go) is not an Effect either -- it runs
// before one dispatches at all, CR 603.3d's own "may" triggered ability
// (Ability.Optional's own doc comment, ability.go).
//
// Forge instantiates one controller per player. Go's methods take the
// deciding player as an explicit PlayerID instead of binding an instance to
// one seat, so a single ScriptedController answers for every player in a
// fixture without a controller-per-player wiring step (PORT-1) -- and so an
// implementation carries no per-player state, the same reasoning GO-2 applies
// to the rest of the engine.
//
// ConfirmPayCost's own caller (resolveUnlessCost, effect.go) is the
// twenty-seventh -- CR's own "unless a cost is paid" gate, a further
// SpellAbility-level decision distinct from ConfirmOptionalTrigger's own
// CR 603.3d "may."
//
// TakeAction (below) is CR 117's own priority ask, a different shape from
// every method above it: it is asked an unbounded number of times per
// PassPriority round rather than once per decision point (priority.go,
// ADR-0019).
type PlayerController interface {
	// ChooseStartingPlayer decides who takes the first turn. decider is the
	// player being asked -- the winner of a coin flip on game one, the loser
	// of the previous game after that -- and the return value need not be
	// decider: choosing to put an opponent on the play is choosing draw.
	ChooseStartingPlayer(g *Game, decider PlayerID, isFirstGame bool) PlayerID

	// ChooseStartingHand picks among several candidate opening hands, for
	// variants that deal more than one (Backup, extra-hand modes). hands[i]
	// is the i-th candidate's cards, and the return value is its index.
	ChooseStartingHand(g *Game, decider PlayerID, hands [][]CardID) int

	// MulliganKeepHand asks whether decider keeps their current hand.
	// firstPlayer is who is on the play this game, which the free-mulligan
	// count keys off; cardsToReturn is not what keeping costs -- keeping is
	// always free under London, the only rule this port has -- it is what
	// taking one *more* mulligan would cost, so the decision is informed by
	// the price of saying no.
	MulliganKeepHand(g *Game, decider PlayerID, firstPlayer PlayerID, cardsToReturn int) bool

	// TuckCardsViaMulligan picks which cards from hand go to the bottom of
	// the library under a London or Houston mulligan. The return value is a
	// subset of hand of length cardsToReturn. Moving the cards is the
	// mulligan rule's job, not the controller's -- Forge's own callers
	// (LondonMulligan, HoustonMulligan) do the move themselves after asking.
	TuckCardsViaMulligan(g *Game, decider PlayerID, hand []CardID, cardsToReturn int) []CardID

	// ChooseLegendaryToKeep decides which of several legendary permanents
	// sharing a name decider keeps when the legend rule applies; the rest
	// go to their owner's graveyard (resolveLegendRule, action.go).
	// duplicates always has at least two elements, and the return value
	// must be one of them.
	ChooseLegendaryToKeep(g *Game, decider PlayerID, duplicates []CardID) CardID

	// DeclareCombatAttackers decides which of decider's eligible creatures attack
	// (CR 508.1, Game.DeclareCombatAttackers, attack.go). eligible is never
	// empty (Game.DeclareCombatAttackers does not call this otherwise). The
	// answer is checked, not trusted (ADR-0024): it must be a subset of
	// eligible and obey as many attack requirements (goad, Mode$ MustAttack)
	// as Java's AttackConstraints says are possible, or
	// Game.DeclareCombatAttackers returns an *IllegalDeclarationError. The
	// requirements themselves are not passed in; a controller that needs them
	// reads the game (ADR-0024 Decision 4). An empty return is legal when no
	// requirement is in force.
	DeclareCombatAttackers(g *Game, decider PlayerID, eligible []CardID) []CardID

	// ExertAttackers decides which of the newly declared attackers decider
	// exerts (CR 508.1c, Game.DeclareCombatAttackers, attack.go) -- Mode$
	// OptionalAttackCost's own Cost$ Exert<1/CARDNAME> shape
	// (Ahn-Crop Crasher, "you may exert CARDNAME as it attacks"). eligible
	// is every declared attacker carrying that static ability, never empty
	// (Game.DeclareCombatAttackers does not call this otherwise). The
	// return value is a subset of eligible, not re-checked -- trust the
	// controller's answer, the same as DeclareCombatAttackers's own; an
	// empty return is legal, the player declining every offer.
	ExertAttackers(g *Game, decider PlayerID, eligible []CardID) []CardID

	// ChooseAttackTarget decides what a single declared attacker is
	// attacking -- the defending player, or one of the planeswalkers/battles
	// they control (CR 508.1b, Game.chooseAttackTargets, attack.go). eligible
	// always has at least two elements: Game.chooseAttackTargets assigns a
	// lone eligible target automatically without asking. An answer outside
	// eligible makes the whole declaration an *IllegalDeclarationError
	// (ADR-0024).
	ChooseAttackTarget(g *Game, decider PlayerID, attacker CardID, eligible []EntityID) EntityID

	// DeclareCombatBlockers decides which of decider's eligible creatures block
	// which attacker (CR 509.1, Game.DeclareCombatBlockers, block.go). eligible is
	// never empty. The answer is checked, not trusted (ADR-0024): every
	// pairing must be legal (CR 509.1a-b) and the whole declaration must pass
	// Java's CombatUtil.validateBlocks (block requirements, Menace and other
	// blocker-count limits), or Game.DeclareCombatBlockers returns an
	// *IllegalDeclarationError. Declining to block anything is legal when no
	// requirement is in force.
	DeclareCombatBlockers(g *Game, decider PlayerID, attackers []CardID, eligible []CardID) []Block

	// AssignCombatDamage decides how a gang-blocked attacker's combat damage
	// divides among the creatures blocking it (CR 510.1c,
	// Game.DealCombatDamage, combatdamage.go). Only called when len(blockers)
	// > 1 -- a single blocker gets the attacker's full power automatically,
	// nothing to decide. blockers always has at least two elements; the
	// return value's order is the order the attacking player assigns in, and
	// is not re-checked for the "lethal before moving on" requirement CR
	// 510.1c imposes -- trust the controller's answer, the same as
	// ChooseLegendaryToKeep.
	AssignCombatDamage(g *Game, decider PlayerID, attacker CardID, blockers []CardID) []DamageAssignment

	// DiscardToHandSize decides which of decider's hand to discard at
	// cleanup (CR 514.1, Game.cleanupStep, turn.go). Only called when hand
	// has more than decider's own HandSizeLimit (player.go, Layer 8's own
	// SetMaxHandSize$/RaiseMaxHandSize$ folded on top of MaxHandSize's own
	// default); count is exactly how many the returned slice must have
	// (hand.Len() - limit), a constraint not re-checked here -- trust the
	// controller's answer, the same as ChooseLegendaryToKeep.
	DiscardToHandSize(g *Game, decider PlayerID, hand []CardID, count int) []CardID

	// ChooseCardsToDiscard decides which of decider's own hand decider
	// discards when a card effect asks them to pick (Discard's own
	// Mode$ TgtChoose, discardeffect.go) -- Forge's own
	// chooseCardsToDiscardFrom, a different decision from
	// DiscardToHandSize's own CR 514.1 cleanup discard even though both end
	// up asking for exactly N cards out of the same hand:
	// chooseCardsToDiscardFrom additionally supports a DiscardValid$-filtered
	// choice set and a count that need not be exact, neither modeled here
	// (discardeffect.go's own doc comment) -- so count is always exact, the
	// same "trust the controller's answer" contract DiscardToHandSize
	// already has. hand is decider's whole hand; count is exactly how many
	// the returned slice must have (min(NumCards$, len(hand))).
	ChooseCardsToDiscard(g *Game, decider PlayerID, hand []CardID, count int) []CardID

	// ArrangeForScry decides how decider scries (CR 701.19, Discard's own
	// interactive-decision shape reused for a second one, scryeffect.go) --
	// Forge's own arrangeForScry. topN is the top ScryNum$ cards of
	// decider's own library, in their current top-to-bottom order (topN[0]
	// on top); the two returned slices partition topN between them (every
	// element of topN in exactly one, none invented), not re-checked here --
	// trust the controller's answer, the same as ChooseLegendaryToKeep. toTop
	// is the order those cards go back on top in, top-to-bottom (toTop[0]
	// ends up the new top card); toBottom is the order the rest go to the
	// bottom in (toBottom's own last element ends up the new bottom card,
	// matching Java's own moveToBottomOfLibrary called once per element in
	// list order). Either may be nil -- an empty scry decision (everything to
	// the other pile) is a legal answer.
	ArrangeForScry(g *Game, decider PlayerID, topN []CardID) (toTop, toBottom []CardID)

	// ArrangeForSurveil decides how decider surveils (CR 701.42,
	// ArrangeForScry's own sibling, scryeffect.go's shape reused for a
	// second reordering decision, surveileffect.go) -- Forge's own
	// arrangeForSurveil. topN is the top Amount$ cards of decider's own
	// library, in their current top-to-bottom order; the two returned
	// slices partition topN between them, not re-checked here -- trust the
	// controller's answer, the same as ArrangeForScry. toTop is the order
	// those cards go back on top in, top-to-bottom (toTop[0] ends up the
	// new top card, the identical convention ArrangeForScry's own toTop
	// has); toGraveyard is the order the rest go to their owner's graveyard
	// in. Either may be nil.
	ArrangeForSurveil(g *Game, decider PlayerID, topN []CardID) (toTop, toGraveyard []CardID)

	// ChoosePermanentsToSacrifice decides which of decider's own battlefield
	// decider sacrifices when a card effect asks them to pick (SacValid$'s
	// own choice, sacrificeeffect.go) -- Forge's own
	// choosePermanentsToSacrifice, ChooseCardsToDiscard's own shape reused
	// for a second exactly-N-of-a-set decision: candidates is every one of
	// decider's own battlefield permanents SacValid$ matches; count is
	// exactly how many the returned slice must have (min(Amount$,
	// len(candidates)), sacrificeeffect.go's own clamp -- this port never
	// asks for more than exist, unlike StrictAmount$'s own further "fewer
	// than asked, sacrifice none of them" rule, not modeled here). Not
	// re-checked here -- trust the controller's answer, the same as
	// ChooseCardsToDiscard.
	ChoosePermanentsToSacrifice(g *Game, decider PlayerID, candidates []CardID, count int) []CardID

	// ChoosePermanentsToTap decides which of decider's own untapped,
	// type-matched battlefield permanents decider taps when an activation
	// cost names a tapXType<N/Type> part (CR 602.2, ActivateAbility/
	// ActivateManaAbility, activateability.go/activatemanaability.go) --
	// Forge's own CostTapType.doListPayment, ChoosePermanentsToSacrifice's
	// own shape reused for a third exactly-N-of-a-set decision: candidates is
	// every one of decider's own untapped battlefield permanents the cost's
	// own type spec matches (the ability's own host itself already excluded
	// when the same cost also taps it via a separate plain T token,
	// tapTypeCandidates's own doc comment, taptype.go); count is exactly how
	// many the returned slice must have (min(N, len(candidates)),
	// ChoosePermanentsToSacrifice's own identical clamp). Not re-checked here
	// -- trust the controller's answer, the same as ChoosePermanentsToSacrifice.
	ChoosePermanentsToTap(g *Game, decider PlayerID, candidates []CardID, count int) []CardID

	// ChoosePermanentsToReturn decides which of decider's own type-matched
	// battlefield permanents decider returns to their owner's hand when an
	// activation cost names a Return<N/Type> part past the self-reference
	// shape (CR 602.2, returncost.go) -- Forge's own CostReturn.doPayment,
	// ChoosePermanentsToTap's own shape reused for a fourth exactly-N-of-a-set
	// decision: candidates is every one of decider's own battlefield
	// permanents the cost's own type spec matches (returnTypeCandidates,
	// returncost.go -- unlike ChoosePermanentsToTap's own candidates, never
	// filtered by tapped state, and never excluding the ability's own host);
	// count is exactly how many the returned slice must have (min(N,
	// len(candidates)), ChoosePermanentsToTap's own identical clamp). Not
	// re-checked here -- trust the controller's answer, the same as
	// ChoosePermanentsToTap.
	ChoosePermanentsToReturn(g *Game, decider PlayerID, candidates []CardID, count int) []CardID

	// ChooseTargets decides which of valid an ability's own controller
	// targets it with (CR 601.2c/603.3b, targeting.go's own resolveTargets,
	// the sole caller). valid is every legal candidate a ValidTgts$ line
	// names -- every EntityID a CardEntity, or every one a PlayerEntity,
	// never a mix (targeting.go's own doc comment has the reason);
	// targetMin/targetMax are TargetMin$/TargetMax$ (1/1 when neither is
	// named). The
	// returned slice's own length and membership are not re-checked here --
	// trust the controller's answer, the same as ChooseLegendaryToKeep.
	ChooseTargets(g *Game, decider PlayerID, valid []EntityID, targetMin, targetMax int) []EntityID

	// ChooseBattleProtector decides which opponent defends decider's Battle
	// (CR 704.5w/704.5x, assignBattleProtector, action.go). eligible is
	// never empty. The return value should be one of eligible's elements,
	// and is not re-checked -- trust the controller's answer, the same as
	// ChooseLegendaryToKeep.
	ChooseBattleProtector(g *Game, decider PlayerID, battle CardID, eligible []PlayerID) PlayerID

	// ChooseHybridManaColor decides which of a two-colour hybrid mana
	// symbol's colours decider pays with (CR 601.2h, [Game.PayManaCost],
	// manapay.go). options is exactly the two colours the symbol offers
	// ([mana.Shard.Colors]); the return value should be exactly one of
	// them, and is not re-checked -- trust the controller's answer, the
	// same as ChooseLegendaryToKeep.
	ChooseHybridManaColor(g *Game, decider PlayerID, options mana.Colors) mana.Colors

	// ChoosePayMonocoloredHybrid decides whether decider pays a monocoloured
	// hybrid symbol ({2/W}, CR 601.2h) with color or with generic mana
	// instead. generic is the shard's own [mana.Shard.CMC] -- 2 for every
	// {2/W}-shaped symbol printed so far, carried as a parameter rather than
	// assumed so a future symbol with a different generic side is not a
	// silent wrong answer. true pays with color; false leaves the payment to
	// [Game.PayManaCost]'s own extra generic instead. Not re-checked --
	// trust the controller's answer, the same as ChooseLegendaryToKeep.
	ChoosePayMonocoloredHybrid(g *Game, decider PlayerID, color mana.Colors, generic int) bool

	// ChoosePayColorlessHybrid decides whether decider pays a colourless
	// hybrid symbol ({C/W}, CR 601.2h) with color or with {C} instead. color
	// is exactly the one colour the symbol offers ([mana.Shard.Colors]).
	// true pays with color; false substitutes [mana.ShardC] for the symbol
	// instead of adding to generic -- a colourless hybrid's other side is a
	// specific mana type, not an amount, unlike ChoosePayMonocoloredHybrid's
	// generic side. Not re-checked -- trust the controller's answer, the
	// same as ChooseLegendaryToKeep.
	ChoosePayColorlessHybrid(g *Game, decider PlayerID, color mana.Colors) bool

	// ChoosePayPhyrexian decides whether decider pays a single-colour
	// Phyrexian mana symbol ({W/P}, CR 118.4/601.2h) with color or with 2
	// life instead. color is the one colour the symbol offers
	// ([mana.Shard.Colors]). true pays with color; false leaves
	// [Game.PayManaCost] to deduct the life once the rest of the payment is
	// confirmed to succeed, the same as [Game.PayManaCost] never spending
	// mana it cannot finish paying. A hybrid Phyrexian symbol ({B/G/P}, two
	// colours plus life) is a three-way choice this method's boolean shape
	// cannot express -- ChoosePayHybridPhyrexian asks that one. Not
	// re-checked -- trust the controller's answer, the same as
	// ChooseLegendaryToKeep.
	ChoosePayPhyrexian(g *Game, decider PlayerID, color mana.Colors) bool

	// ChoosePayHybridPhyrexian decides how decider pays a hybrid Phyrexian
	// mana symbol ({B/G/P}, CR 118.4/601.2h): with either of its two
	// colours, or with 2 life. colors is exactly the two colours the symbol
	// offers ([mana.Shard.Colors]). Returning one of those two colours pays
	// with it; returning the zero [mana.Colors] pays with 2 life instead,
	// deducted by [Game.PayManaCost] once the rest of the payment is
	// confirmed to succeed, the same guarantee ChoosePayPhyrexian's own
	// life side gets. Not re-checked -- trust the controller's answer, the
	// same as ChooseLegendaryToKeep.
	ChoosePayHybridPhyrexian(g *Game, decider PlayerID, colors mana.Colors) mana.Colors

	// ChoosePayGeneric decides which single type of mana decider spends
	// toward one unit of a mana cost's generic amount (CR 106.6: "any type
	// of mana, including colorless mana, can be used to pay a generic mana
	// cost"; CR 601.2h). [Game.PayManaCost] calls this once per unit of
	// generic still owed, after every colour, hybrid and Phyrexian shard is
	// already resolved -- CR 601.2h's own pips-before-generic order. The
	// return value should be one of mana.ShardW/U/B/R/G/C; it is not
	// re-checked here, but [Pool.Pay]'s own bucket check fails the whole
	// payment if decider's pool does not actually hold what was chosen, the
	// same as an unavailable hybrid colour choice already does.
	ChoosePayGeneric(g *Game, decider PlayerID) mana.Shard

	// ChoosePayX decides the value of X for a cost carrying one or more X
	// symbols (CR 601.2b/107.3f: chosen once per cast, then every X symbol in
	// the cost stands for that same value -- a cost with two X symbols owes
	// twice the chosen amount, not one value each). cost is the whole mana
	// cost being paid, exactly as [Game.PayManaCost] received it, so a real
	// controller can see what accompanies the X symbols and how much floating
	// mana is available before answering; [mana.Cost.CountX] is how many X
	// symbols it carries.
	//
	// The return value is folded into the cost's generic amount (chosen
	// value times [mana.Cost.CountX]) before anything else resolves, the
	// same "ahead of every other shard" position CR 601.2b's own ordering
	// puts X's announcement in. A negative answer is not re-checked here,
	// the same as an unavailable hybrid colour choice: [Game.PayManaCost]
	// reports payment failure rather than trusting a value CR 601.2b's own
	// "non-negative integer" rule forbids.
	ChoosePayX(g *Game, decider PlayerID, cost mana.Cost) int

	// ChoosePaySnow decides which color of floating snow mana decider spends
	// on one snow ({S}, CR 106.3a) symbol. [Game.PayManaCost] calls this once
	// per {S} symbol the cost carries, independently -- unlike ChoosePayX,
	// two {S} symbols in the same cost are two separate questions, not one
	// value reused, since each can be paid with a different color's snow
	// mana. The return value should be one of mana.ShardW/U/B/R/G/C; it is
	// not re-checked here, but [Pool.PayWithSnow]'s own bucket check fails
	// the whole payment if decider's pool does not actually hold snow mana of
	// the color chosen, the same as an unavailable hybrid colour choice
	// already does. Snow-tagged mana still pays a same-color pip or a
	// generic unit the same as plain mana of that color -- this method exists
	// only for the one requirement plain mana cannot cover.
	ChoosePaySnow(g *Game, decider PlayerID) mana.Shard

	// ChooseEnchantTarget decides which permanent an Aura being cast attaches
	// to (CR 601.2c, Game.CastSpell, castspell.go). eligible always has at
	// least two elements: CastSpell assigns a lone eligible target
	// automatically without asking, the same "nothing meaningful to decide"
	// reasoning chooseAttackTargets already applies. The return value should
	// be one of eligible's elements, and is not re-checked -- trust the
	// controller's answer, the same as ChooseLegendaryToKeep.
	ChooseEnchantTarget(g *Game, decider PlayerID, aura CardID, eligible []CardID) CardID

	// ConfirmOptionalTrigger decides whether a "may" triggered ability
	// actually does anything (CR 603.3d, Registry.Resolve, effect.go) --
	// WrappedAbility.resolve()'s own `decider.getController().confirmTrigger
	// (this)`. decider is the ability's own Controller (Ability.Optional's
	// own doc comment, ability.go: only OptionalDecider$ You is resolved, so
	// decider is always the trigger's own host controller, never a
	// different player). source is the ability's own host card, carried so a
	// real controller could describe what it is confirming; this port's own
	// answer is not re-checked against anything -- true runs the ability
	// (and its own SubAbility$ chain) exactly as if it had not been
	// optional at all, false skips both, the identical two outcomes a
	// mandatory ability's own success/no-legal-target split already has.
	ConfirmOptionalTrigger(g *Game, decider PlayerID, source CardID) bool

	// ConfirmPayCost decides whether decider pays an UnlessCost$ cost to
	// prevent an ability's own effect from happening (resolveUnlessCost,
	// effect.go) -- PlayerController.payCostToPreventEffect's own decision
	// half, ported apart from its own payment half: a true answer still
	// needs PayManaCost (manapay.go) to actually succeed, exactly the
	// two-step "decide, then pay" split every other mana decision on this
	// interface already has (ChoosePayMonocoloredHybrid, ...). cost is the
	// UnlessCost$ line's own parsed mana cost, source the ability's own
	// host card, carried so a real controller could describe what it is
	// paying to prevent.
	ConfirmPayCost(g *Game, decider PlayerID, cost mana.Cost, source CardID) bool

	// ChooseManaColor decides which color a Produced$ Any or Produced$ Combo
	// mana ability adds (CR 605.3b, ActivateManaAbility,
	// activatemanaability.go). options is the offered set -- AllColors for
	// "Any" (the real corpus's own "Add one mana of any color" always offers
	// all five), or the specific two-to-four colors a "Combo W U"-shaped
	// dual/tri-land lists (a duo/triome's own real "Add W or U" shape) --
	// distinct from ChooseHybridManaColor's own always-exactly-two contract
	// (that method's own doc comment), which this one generalizes past.
	// source is the permanent whose ability is resolving. The return value
	// should be exactly one color options itself contains -- not re-checked
	// by the interface itself, but ActivateManaAbility validates both
	// (Count() == 1 and options.Has(color)) before ever calling Pool.Add,
	// since Pool.Add panics on anything but a single valid color
	// ([Pool.Add]'s own doc comment) and a bad script answer is not an
	// engine invariant breach (GO-7).
	ChooseManaColor(g *Game, decider PlayerID, source CardID, options mana.Colors) mana.Colors

	// ChooseCardsForEffect picks between lo and hi cards out of options for
	// an effect resolving from source -- Java PlayerController's own
	// chooseCardsForEffect, what ChooseCard's default shape and Reveal's
	// chooseCardsToRevealFromHand both ask. The answer is re-checked by the
	// caller (size and membership) before it is used (GO-7).
	ChooseCardsForEffect(g *Game, decider PlayerID, source CardID, options []CardID, lo, hi int) []CardID

	// ChoosePlayerForEffect picks one player out of options -- Java's
	// chooseSingleEntityForEffect as ChoosePlayer calls it.
	ChoosePlayerForEffect(g *Game, decider PlayerID, source CardID, options []PlayerID) PlayerID

	// ChooseColors picks between lo and hi colors out of options -- Java's
	// chooseColors as ChooseColor calls it.
	ChooseColors(g *Game, decider PlayerID, source CardID, options mana.Colors, lo, hi int) mana.Colors

	// ChooseNumber picks an integer in [lo, hi] -- Java's chooseNumber as
	// ChooseNumber calls it.
	ChooseNumber(g *Game, decider PlayerID, source CardID, lo, hi int) int

	// ChooseTapOrUntap decides whether TapOrUntap taps (true) or untaps
	// (false) target -- Java's chooseBinary with BinaryChoiceType.TapOrUntap.
	ChooseTapOrUntap(g *Game, decider PlayerID, target CardID) bool

	// ChooseEntitiesForEffect picks between lo and hi entities out of
	// options -- Java's chooseEntitiesForEffect as Proliferate calls it.
	ChooseEntitiesForEffect(g *Game, decider PlayerID, source CardID, options []EntityID, lo, hi int) []EntityID

	// ConfirmReveal answers PeekAndReveal's RevealOptional$ prompt -- Java's
	// confirmAction with "reveal this card to other players?".
	ConfirmReveal(g *Game, decider PlayerID, source CardID) bool

	// ConfirmEffect answers an effect's generic yes/no prompt -- Java's
	// confirmAction as an Optional$ param, ShuffleNonMandatory$, MayShuffle$
	// or Explore's "put this card into your graveyard?" asks it.
	ConfirmEffect(g *Game, decider PlayerID, source CardID) bool

	// OrderCardsForZone orders cards moving together into dest -- Java's
	// orderMoveToZoneList. The answer is the order they are moved in, so for
	// a library top the last card ends up on top. The caller checks it is a
	// permutation of cards (GO-7).
	OrderCardsForZone(g *Game, decider PlayerID, cards []CardID, dest ZoneType) []CardID

	// ChooseAbilitiesForEffect picks amount of the offered modes, answering
	// with their indices into options (the SVar names GenericChoice's
	// Choices$ lists) -- Java's chooseSpellAbilitiesForEffect.
	ChooseAbilitiesForEffect(g *Game, decider PlayerID, source CardID, options []string, amount int) []int

	// ChooseModesForAbility picks between lo and hi of a Charm's modes,
	// answering with distinct indices into options (the Choices$ SVar
	// names) -- Java's chooseModeForAbility. The caller checks the answer
	// (GO-7).
	ChooseModesForAbility(g *Game, decider PlayerID, source CardID, options []string, lo, hi int) []int

	// ChooseProtectionType picks one of options (a color or a card type) for
	// a Protection effect's Gains$ Choice, answering with its index -- Java's
	// chooseProtectionType.
	ChooseProtectionType(g *Game, decider PlayerID, source CardID, options []string) int

	// CallCoinFlip is the flipper's call before a coin flip: true for heads
	// -- Java's chooseBinary with BinaryChoiceType.HeadsOrTails.
	CallCoinFlip(g *Game, decider PlayerID, source CardID) bool

	// WillPutCardOnTop answers Clash's "put the revealed card on top of your
	// library, or on the bottom?": true for the top -- Java's
	// willPutCardOnTop.
	WillPutCardOnTop(g *Game, decider PlayerID, card CardID) bool

	// ChooseBinary answers one of Java's PlayerController.chooseBinary
	// questions, kind naming its BinaryChoiceType: true is the first of the
	// pair (Tap, Odd, Left).
	ChooseBinary(g *Game, decider PlayerID, source CardID, kind BinaryChoice) bool

	// ChooseOption picks one of options, answering with its index -- the
	// shared shape of Java's chooseSomeType, chooseCardName, vote and
	// pile choices, each a pick from a list of strings.
	ChooseOption(g *Game, decider PlayerID, source CardID, options []string) int

	// ChooseSector picks one of sectors ("Alpha", "Beta", "Gamma", in that
	// order -- PlayerController.java:254-256), answering with its index --
	// Java's chooseSector. assignee is the creature being assigned a sector
	// by CR 704.5u's state-based action (GameAction.java:1815), or NoCard
	// when ChooseSectorEffect asks for the host's own chosen sector
	// (ChooseSectorEffect.java:12 passes null). Only the effect calls it
	// today; the parameter keeps Java's own shape so the SBA caller lands
	// without changing every implementation again. The caller checks the
	// answer (GO-7).
	ChooseSector(g *Game, decider PlayerID, assignee CardID, sectors []string) int

	// ChooseRoomDoor picks one of doors, doors of the Room room -- Java's
	// chooseSingleCardState over the Room's LeftSplit/RightSplit states,
	// asked by UnlockDoor's Mode$ Unlock ("Choose Room to unlock") and Mode$
	// LockOrUnlock (unlockdooreffect.go). doors always has two entries,
	// left then right: with one the effect takes it without asking,
	// PlayerControllerHuman.chooseSingleCardState's own shortcut. The caller
	// checks the answer is one of doors (GO-7).
	ChooseRoomDoor(g *Game, decider PlayerID, room CardID, doors []Door) Door

	// TakeAction is CR 117's own priority ask (ADR-0019): what, if anything,
	// pid does with priority right now. The zero value, ActionPass, is a
	// pass -- Java's chooseSpellAbilityToPlay returning null
	// (PlayerController.java:278, PlayerControllerHuman.java:1714-1716).
	// PassPriority (priority.go) is the only caller.
	TakeAction(g *Game, pid PlayerID) Action
}

// BinaryChoice names a PlayerController.BinaryChoiceType.
type BinaryChoice string

// The BinaryChoiceType values the ported effects ask.
const (
	TapOrUntap   BinaryChoice = "TapOrUntap"
	OddsOrEvens  BinaryChoice = "OddsOrEvens"
	LeftOrRight  BinaryChoice = "LeftOrRight"
	AddOrRemove  BinaryChoice = "AddOrRemove"
	Pile1OrPile2 BinaryChoice = "Pile1OrPile2"
)

// ScriptedController answers every decision from a pre-loaded queue, one per
// method. It is what a TEST-5 fixture runs against: no AI, no heuristics, so
// a scenario's outcome is deterministic and any divergence from the Java
// oracle is a rules bug, never an AI one (Plan Section 3.3, Layer 2).
//
// A queue running dry mid-game is a fixture-authoring mistake, not a rules
// question a card script could cause, so it panics rather than returning a
// zero value that would silently pass the scenario for the wrong reason
// (GO-7).
type ScriptedController struct {
	startingPlayers  []PlayerID
	startingHands    []int
	keepHand         []bool
	tucked           [][]CardID
	legendaryKeep    []CardID
	attackers        [][]CardID
	exertAttackers   [][]CardID
	attackTargets    []EntityID
	blocks           [][]Block
	damage           [][]DamageAssignment
	discards         [][]CardID
	discardChoices   [][]CardID
	battleProtector  []PlayerID
	hybridMana       []mana.Colors
	monoHybrid       []bool
	colorlessHybrid  []bool
	phyrexian        []bool
	hybridPhyrexian  []mana.Colors
	genericMana      []mana.Shard
	xValues          []int
	snowMana         []mana.Shard
	enchantTargets   []CardID
	scryDecisions    []scryDecision
	surveilDecisions []scryDecision
	targets          [][]EntityID
	optionalTrigger  []bool
	sacrificeChoices [][]CardID
	payCost          []bool
	manaColor        []mana.Colors
	tapChoices       [][]CardID
	returnChoices    [][]CardID
	cardChoices      [][]CardID
	playerChoices    []PlayerID
	colorChoices     []mana.Colors
	numberChoices    []int
	tapOrUntap       []bool
	entityChoices    [][]EntityID
	confirmReveal    []bool
	confirmEffect    []bool
	cardOrders       [][]CardID
	abilityChoices   [][]int
	modeChoices      [][]int
	protectionChoice []int
	coinCalls        []bool
	cardOnTop        []bool
	binary           []bool
	option           []int
	sector           []int
	roomDoor         []Door
	// actionQueue is TakeAction's own per-player queue (ADR-0019), indexed
	// by PlayerID like Game.players already is -- not a map, GO-12. A slot
	// left nil means "nothing queued for this player yet," the same as an
	// empty slice.
	actionQueue []*[]Action
}

// scryDecision is one queued answer to ArrangeForScry or ArrangeForSurveil
// -- a pair, so QueueScry/QueueSurveil each take both halves together rather
// than as two separately queued slices that could desync under a partial
// scenario edit. The two decisions share this one type (toBottom and
// toGraveyard are the identical shape, a card-order slice) rather than each
// declaring its own trivial struct.
type scryDecision struct {
	toTop, toBottom []CardID
}

// NewScriptedController builds a controller with no decisions queued yet.
func NewScriptedController() *ScriptedController {
	return &ScriptedController{}
}

// QueueStartingPlayer appends the answer to the next ChooseStartingPlayer call.
func (c *ScriptedController) QueueStartingPlayer(p PlayerID) {
	c.startingPlayers = append(c.startingPlayers, p)
}

// QueueStartingHand appends the answer to the next ChooseStartingHand call.
func (c *ScriptedController) QueueStartingHand(index int) {
	c.startingHands = append(c.startingHands, index)
}

// QueueKeepHand appends the answer to the next MulliganKeepHand call.
func (c *ScriptedController) QueueKeepHand(keep bool) {
	c.keepHand = append(c.keepHand, keep)
}

// QueueTuck appends the answer to the next TuckCardsViaMulligan call.
func (c *ScriptedController) QueueTuck(cards []CardID) {
	c.tucked = append(c.tucked, cards)
}

// QueueLegendaryToKeep appends the answer to the next ChooseLegendaryToKeep
// call.
func (c *ScriptedController) QueueLegendaryToKeep(id CardID) {
	c.legendaryKeep = append(c.legendaryKeep, id)
}

// QueueAttackers appends the answer to the next DeclareCombatAttackers call. An
// empty or nil cards declines to attack with anything, a legal answer that
// still consumes the queue slot.
func (c *ScriptedController) QueueAttackers(cards []CardID) {
	c.attackers = append(c.attackers, cards)
}

// QueueExertAttackers appends the answer to the next ExertAttackers call. An
// empty or nil cards declines every offer, a legal answer that still
// consumes the queue slot.
func (c *ScriptedController) QueueExertAttackers(cards []CardID) {
	c.exertAttackers = append(c.exertAttackers, cards)
}

// QueueAttackTarget appends the answer to the next ChooseAttackTarget call.
func (c *ScriptedController) QueueAttackTarget(target EntityID) {
	c.attackTargets = append(c.attackTargets, target)
}

// QueueBlocks appends the answer to the next DeclareCombatBlockers call. Nil
// declines to block anything, a legal answer that still consumes the queue
// slot.
func (c *ScriptedController) QueueBlocks(blocks []Block) {
	c.blocks = append(c.blocks, blocks)
}

// QueueDamageAssignment appends the answer to the next AssignCombatDamage
// call.
func (c *ScriptedController) QueueDamageAssignment(assignment []DamageAssignment) {
	c.damage = append(c.damage, assignment)
}

// QueueDiscard appends the answer to the next DiscardToHandSize call.
func (c *ScriptedController) QueueDiscard(cards []CardID) {
	c.discards = append(c.discards, cards)
}

// QueueDiscardChoice appends the answer to the next ChooseCardsToDiscard
// call. A separate queue from QueueDiscard's: the two are different
// decisions (ChooseCardsToDiscard's own doc comment) even when a scenario
// happens to script the identical cards for both.
func (c *ScriptedController) QueueDiscardChoice(cards []CardID) {
	c.discardChoices = append(c.discardChoices, cards)
}

// QueueScry appends the answer to the next ArrangeForScry call: toTop and
// toBottom together, ArrangeForScry's own doc comment has their order
// conventions.
func (c *ScriptedController) QueueScry(toTop, toBottom []CardID) {
	c.scryDecisions = append(c.scryDecisions, scryDecision{toTop: toTop, toBottom: toBottom})
}

// QueueSurveil appends the answer to the next ArrangeForSurveil call: toTop
// and toGraveyard together, a separate queue from QueueScry's own even
// though both share scryDecision's own shape -- the two are different
// decisions (ArrangeForSurveil's own doc comment) even when a scenario
// happens to script the identical cards for both.
func (c *ScriptedController) QueueSurveil(toTop, toGraveyard []CardID) {
	c.surveilDecisions = append(c.surveilDecisions, scryDecision{toTop: toTop, toBottom: toGraveyard})
}

// QueueSacrificeChoice appends the answer to the next
// ChoosePermanentsToSacrifice call.
func (c *ScriptedController) QueueSacrificeChoice(cards []CardID) {
	c.sacrificeChoices = append(c.sacrificeChoices, cards)
}

// QueueTapChoice appends the answer to the next ChoosePermanentsToTap call.
func (c *ScriptedController) QueueTapChoice(cards []CardID) {
	c.tapChoices = append(c.tapChoices, cards)
}

// QueueReturnChoice appends the answer to the next ChoosePermanentsToReturn
// call.
func (c *ScriptedController) QueueReturnChoice(cards []CardID) {
	c.returnChoices = append(c.returnChoices, cards)
}

// QueueTargets appends the answer to the next ChooseTargets call.
func (c *ScriptedController) QueueTargets(chosen []EntityID) {
	c.targets = append(c.targets, chosen)
}

// QueueBattleProtector appends the answer to the next ChooseBattleProtector
// call.
func (c *ScriptedController) QueueBattleProtector(p PlayerID) {
	c.battleProtector = append(c.battleProtector, p)
}

// ChooseStartingPlayer returns the next answer QueueStartingPlayer queued.
func (c *ScriptedController) ChooseStartingPlayer(_ *Game, _ PlayerID, _ bool) PlayerID {
	if len(c.startingPlayers) == 0 {
		panic(scriptExhausted("starting player"))
	}
	v := c.startingPlayers[0]
	c.startingPlayers = c.startingPlayers[1:]
	return v
}

// ChooseStartingHand returns the next answer QueueStartingHand queued.
func (c *ScriptedController) ChooseStartingHand(_ *Game, _ PlayerID, _ [][]CardID) int {
	if len(c.startingHands) == 0 {
		panic(scriptExhausted("starting hand"))
	}
	v := c.startingHands[0]
	c.startingHands = c.startingHands[1:]
	return v
}

// MulliganKeepHand returns the next answer QueueKeepHand queued.
func (c *ScriptedController) MulliganKeepHand(_ *Game, _, _ PlayerID, _ int) bool {
	if len(c.keepHand) == 0 {
		panic(scriptExhausted("keep hand"))
	}
	v := c.keepHand[0]
	c.keepHand = c.keepHand[1:]
	return v
}

// TuckCardsViaMulligan returns the next answer QueueTuck queued.
func (c *ScriptedController) TuckCardsViaMulligan(_ *Game, _ PlayerID, _ []CardID, _ int) []CardID {
	if len(c.tucked) == 0 {
		panic(scriptExhausted("tuck via mulligan"))
	}
	v := c.tucked[0]
	c.tucked = c.tucked[1:]
	return v
}

// ChooseLegendaryToKeep returns the next answer QueueLegendaryToKeep queued.
func (c *ScriptedController) ChooseLegendaryToKeep(_ *Game, _ PlayerID, _ []CardID) CardID {
	if len(c.legendaryKeep) == 0 {
		panic(scriptExhausted("legendary to keep"))
	}
	v := c.legendaryKeep[0]
	c.legendaryKeep = c.legendaryKeep[1:]
	return v
}

// DeclareCombatAttackers returns the next answer QueueAttackers queued.
func (c *ScriptedController) DeclareCombatAttackers(_ *Game, _ PlayerID, _ []CardID) []CardID {
	if len(c.attackers) == 0 {
		panic(scriptExhausted("attackers"))
	}
	v := c.attackers[0]
	c.attackers = c.attackers[1:]
	return v
}

// ExertAttackers returns the next answer QueueExertAttackers queued.
func (c *ScriptedController) ExertAttackers(_ *Game, _ PlayerID, _ []CardID) []CardID {
	if len(c.exertAttackers) == 0 {
		panic(scriptExhausted("exert attackers"))
	}
	v := c.exertAttackers[0]
	c.exertAttackers = c.exertAttackers[1:]
	return v
}

// ChooseAttackTarget returns the next answer QueueAttackTarget queued.
func (c *ScriptedController) ChooseAttackTarget(_ *Game, _ PlayerID, _ CardID, _ []EntityID) EntityID {
	if len(c.attackTargets) == 0 {
		panic(scriptExhausted("attack target"))
	}
	v := c.attackTargets[0]
	c.attackTargets = c.attackTargets[1:]
	return v
}

// DeclareCombatBlockers returns the next answer QueueBlocks queued.
func (c *ScriptedController) DeclareCombatBlockers(_ *Game, _ PlayerID, _ []CardID, _ []CardID) []Block {
	if len(c.blocks) == 0 {
		panic(scriptExhausted("blocks"))
	}
	v := c.blocks[0]
	c.blocks = c.blocks[1:]
	return v
}

// AssignCombatDamage returns the next answer QueueDamageAssignment queued.
func (c *ScriptedController) AssignCombatDamage(_ *Game, _ PlayerID, _ CardID, _ []CardID) []DamageAssignment {
	if len(c.damage) == 0 {
		panic(scriptExhausted("damage assignment"))
	}
	v := c.damage[0]
	c.damage = c.damage[1:]
	return v
}

// DiscardToHandSize returns the next answer QueueDiscard queued.
func (c *ScriptedController) DiscardToHandSize(_ *Game, _ PlayerID, _ []CardID, _ int) []CardID {
	if len(c.discards) == 0 {
		panic(scriptExhausted("discard"))
	}
	v := c.discards[0]
	c.discards = c.discards[1:]
	return v
}

// ChooseCardsToDiscard returns the next answer QueueDiscardChoice queued.
func (c *ScriptedController) ChooseCardsToDiscard(_ *Game, _ PlayerID, _ []CardID, _ int) []CardID {
	if len(c.discardChoices) == 0 {
		panic(scriptExhausted("discard choice"))
	}
	v := c.discardChoices[0]
	c.discardChoices = c.discardChoices[1:]
	return v
}

// ChoosePermanentsToSacrifice returns the next answer QueueSacrificeChoice
// queued.
func (c *ScriptedController) ChoosePermanentsToSacrifice(_ *Game, _ PlayerID, _ []CardID, _ int) []CardID {
	if len(c.sacrificeChoices) == 0 {
		panic(scriptExhausted("sacrifice choice"))
	}
	v := c.sacrificeChoices[0]
	c.sacrificeChoices = c.sacrificeChoices[1:]
	return v
}

// ChoosePermanentsToTap returns the next answer QueueTapChoice queued.
func (c *ScriptedController) ChoosePermanentsToTap(_ *Game, _ PlayerID, _ []CardID, _ int) []CardID {
	if len(c.tapChoices) == 0 {
		panic(scriptExhausted("tap choice"))
	}
	v := c.tapChoices[0]
	c.tapChoices = c.tapChoices[1:]
	return v
}

// ChoosePermanentsToReturn returns the next answer QueueReturnChoice queued.
func (c *ScriptedController) ChoosePermanentsToReturn(_ *Game, _ PlayerID, _ []CardID, _ int) []CardID {
	if len(c.returnChoices) == 0 {
		panic(scriptExhausted("return choice"))
	}
	v := c.returnChoices[0]
	c.returnChoices = c.returnChoices[1:]
	return v
}

// ArrangeForScry returns the next answer QueueScry queued.
func (c *ScriptedController) ArrangeForScry(_ *Game, _ PlayerID, _ []CardID) (toTop, toBottom []CardID) {
	if len(c.scryDecisions) == 0 {
		panic(scriptExhausted("scry"))
	}
	v := c.scryDecisions[0]
	c.scryDecisions = c.scryDecisions[1:]
	return v.toTop, v.toBottom
}

// ArrangeForSurveil returns the next answer QueueSurveil queued.
func (c *ScriptedController) ArrangeForSurveil(_ *Game, _ PlayerID, _ []CardID) (toTop, toGraveyard []CardID) {
	if len(c.surveilDecisions) == 0 {
		panic(scriptExhausted("surveil"))
	}
	v := c.surveilDecisions[0]
	c.surveilDecisions = c.surveilDecisions[1:]
	return v.toTop, v.toBottom
}

// ChooseTargets returns the next answer QueueTargets queued.
func (c *ScriptedController) ChooseTargets(_ *Game, _ PlayerID, _ []EntityID, _, _ int) []EntityID {
	if len(c.targets) == 0 {
		panic(scriptExhausted("targets"))
	}
	v := c.targets[0]
	c.targets = c.targets[1:]
	return v
}

// ChooseBattleProtector returns the next answer QueueBattleProtector queued.
func (c *ScriptedController) ChooseBattleProtector(_ *Game, _ PlayerID, _ CardID, _ []PlayerID) PlayerID {
	if len(c.battleProtector) == 0 {
		panic(scriptExhausted("battle protector"))
	}
	v := c.battleProtector[0]
	c.battleProtector = c.battleProtector[1:]
	return v
}

// QueueHybridManaColor appends the answer to the next ChooseHybridManaColor
// call.
func (c *ScriptedController) QueueHybridManaColor(color mana.Colors) {
	c.hybridMana = append(c.hybridMana, color)
}

// ChooseHybridManaColor returns the next answer QueueHybridManaColor queued.
func (c *ScriptedController) ChooseHybridManaColor(_ *Game, _ PlayerID, _ mana.Colors) mana.Colors {
	if len(c.hybridMana) == 0 {
		panic(scriptExhausted("hybrid mana color"))
	}
	v := c.hybridMana[0]
	c.hybridMana = c.hybridMana[1:]
	return v
}

// QueuePayMonocoloredHybrid appends the answer to the next
// ChoosePayMonocoloredHybrid call.
func (c *ScriptedController) QueuePayMonocoloredHybrid(payColor bool) {
	c.monoHybrid = append(c.monoHybrid, payColor)
}

// ChoosePayMonocoloredHybrid returns the next answer QueuePayMonocoloredHybrid queued.
func (c *ScriptedController) ChoosePayMonocoloredHybrid(_ *Game, _ PlayerID, _ mana.Colors, _ int) bool {
	if len(c.monoHybrid) == 0 {
		panic(scriptExhausted("pay monocolored hybrid"))
	}
	v := c.monoHybrid[0]
	c.monoHybrid = c.monoHybrid[1:]
	return v
}

// QueuePayColorlessHybrid appends the answer to the next
// ChoosePayColorlessHybrid call.
func (c *ScriptedController) QueuePayColorlessHybrid(payColor bool) {
	c.colorlessHybrid = append(c.colorlessHybrid, payColor)
}

// ChoosePayColorlessHybrid returns the next answer QueuePayColorlessHybrid queued.
func (c *ScriptedController) ChoosePayColorlessHybrid(_ *Game, _ PlayerID, _ mana.Colors) bool {
	if len(c.colorlessHybrid) == 0 {
		panic(scriptExhausted("pay colorless hybrid"))
	}
	v := c.colorlessHybrid[0]
	c.colorlessHybrid = c.colorlessHybrid[1:]
	return v
}

// QueuePayPhyrexian appends the answer to the next ChoosePayPhyrexian call.
func (c *ScriptedController) QueuePayPhyrexian(payColor bool) {
	c.phyrexian = append(c.phyrexian, payColor)
}

// ChoosePayPhyrexian returns the next answer QueuePayPhyrexian queued.
func (c *ScriptedController) ChoosePayPhyrexian(_ *Game, _ PlayerID, _ mana.Colors) bool {
	if len(c.phyrexian) == 0 {
		panic(scriptExhausted("pay phyrexian"))
	}
	v := c.phyrexian[0]
	c.phyrexian = c.phyrexian[1:]
	return v
}

// QueuePayHybridPhyrexian appends the answer to the next
// ChoosePayHybridPhyrexian call. Queue the zero [mana.Colors] for "pay with
// life instead."
func (c *ScriptedController) QueuePayHybridPhyrexian(colors mana.Colors) {
	c.hybridPhyrexian = append(c.hybridPhyrexian, colors)
}

// ChoosePayHybridPhyrexian returns the next answer QueuePayHybridPhyrexian queued.
func (c *ScriptedController) ChoosePayHybridPhyrexian(_ *Game, _ PlayerID, _ mana.Colors) mana.Colors {
	if len(c.hybridPhyrexian) == 0 {
		panic(scriptExhausted("pay hybrid phyrexian"))
	}
	v := c.hybridPhyrexian[0]
	c.hybridPhyrexian = c.hybridPhyrexian[1:]
	return v
}

// QueuePayGeneric appends the answer to the next ChoosePayGeneric call.
func (c *ScriptedController) QueuePayGeneric(s mana.Shard) {
	c.genericMana = append(c.genericMana, s)
}

// ChoosePayGeneric returns the next answer QueuePayGeneric queued.
func (c *ScriptedController) ChoosePayGeneric(_ *Game, _ PlayerID) mana.Shard {
	if len(c.genericMana) == 0 {
		panic(scriptExhausted("pay generic"))
	}
	v := c.genericMana[0]
	c.genericMana = c.genericMana[1:]
	return v
}

// QueuePayX appends the answer to the next ChoosePayX call.
func (c *ScriptedController) QueuePayX(x int) {
	c.xValues = append(c.xValues, x)
}

// ChoosePayX returns the next answer QueuePayX queued.
func (c *ScriptedController) ChoosePayX(_ *Game, _ PlayerID, _ mana.Cost) int {
	if len(c.xValues) == 0 {
		panic(scriptExhausted("pay x"))
	}
	v := c.xValues[0]
	c.xValues = c.xValues[1:]
	return v
}

// QueuePaySnow appends the answer to the next ChoosePaySnow call.
func (c *ScriptedController) QueuePaySnow(s mana.Shard) {
	c.snowMana = append(c.snowMana, s)
}

// ChoosePaySnow returns the next answer QueuePaySnow queued.
func (c *ScriptedController) ChoosePaySnow(_ *Game, _ PlayerID) mana.Shard {
	if len(c.snowMana) == 0 {
		panic(scriptExhausted("pay snow"))
	}
	v := c.snowMana[0]
	c.snowMana = c.snowMana[1:]
	return v
}

// QueueEnchantTarget appends the answer to the next ChooseEnchantTarget call.
func (c *ScriptedController) QueueEnchantTarget(host CardID) {
	c.enchantTargets = append(c.enchantTargets, host)
}

// ChooseEnchantTarget returns the next answer QueueEnchantTarget queued.
func (c *ScriptedController) ChooseEnchantTarget(_ *Game, _ PlayerID, _ CardID, _ []CardID) CardID {
	if len(c.enchantTargets) == 0 {
		panic(scriptExhausted("enchant target"))
	}
	v := c.enchantTargets[0]
	c.enchantTargets = c.enchantTargets[1:]
	return v
}

// QueueConfirmOptionalTrigger appends the answer to the next
// ConfirmOptionalTrigger call.
func (c *ScriptedController) QueueConfirmOptionalTrigger(confirm bool) {
	c.optionalTrigger = append(c.optionalTrigger, confirm)
}

// ConfirmOptionalTrigger returns the next answer QueueConfirmOptionalTrigger
// queued.
func (c *ScriptedController) ConfirmOptionalTrigger(_ *Game, _ PlayerID, _ CardID) bool {
	if len(c.optionalTrigger) == 0 {
		panic(scriptExhausted("confirm optional trigger"))
	}
	v := c.optionalTrigger[0]
	c.optionalTrigger = c.optionalTrigger[1:]
	return v
}

// QueueConfirmPayCost appends the answer to the next ConfirmPayCost call.
func (c *ScriptedController) QueueConfirmPayCost(pay bool) {
	c.payCost = append(c.payCost, pay)
}

// ConfirmPayCost returns the next answer QueueConfirmPayCost queued.
func (c *ScriptedController) ConfirmPayCost(_ *Game, _ PlayerID, _ mana.Cost, _ CardID) bool {
	if len(c.payCost) == 0 {
		panic(scriptExhausted("confirm pay cost"))
	}
	v := c.payCost[0]
	c.payCost = c.payCost[1:]
	return v
}

// QueueManaColor appends the answer to the next ChooseManaColor call.
func (c *ScriptedController) QueueManaColor(color mana.Colors) {
	c.manaColor = append(c.manaColor, color)
}

// ChooseManaColor returns the next answer QueueManaColor queued.
func (c *ScriptedController) ChooseManaColor(_ *Game, _ PlayerID, _ CardID, _ mana.Colors) mana.Colors {
	if len(c.manaColor) == 0 {
		panic(scriptExhausted("mana color"))
	}
	v := c.manaColor[0]
	c.manaColor = c.manaColor[1:]
	return v
}

// QueueCardChoice appends the answer to the next ChooseCardsForEffect call.
func (c *ScriptedController) QueueCardChoice(ids []CardID) {
	c.cardChoices = append(c.cardChoices, ids)
}

// ChooseCardsForEffect returns the next answer QueueCardChoice queued.
func (c *ScriptedController) ChooseCardsForEffect(_ *Game, _ PlayerID, _ CardID, _ []CardID, _, _ int) []CardID {
	return popQueue(&c.cardChoices, "card choice")
}

// QueuePlayerChoice appends the answer to the next ChoosePlayerForEffect call.
func (c *ScriptedController) QueuePlayerChoice(p PlayerID) {
	c.playerChoices = append(c.playerChoices, p)
}

// ChoosePlayerForEffect returns the next answer QueuePlayerChoice queued.
func (c *ScriptedController) ChoosePlayerForEffect(_ *Game, _ PlayerID, _ CardID, _ []PlayerID) PlayerID {
	return popQueue(&c.playerChoices, "player choice")
}

// QueueColorChoice appends the answer to the next ChooseColors call.
func (c *ScriptedController) QueueColorChoice(colors mana.Colors) {
	c.colorChoices = append(c.colorChoices, colors)
}

// ChooseColors returns the next answer QueueColorChoice queued.
func (c *ScriptedController) ChooseColors(_ *Game, _ PlayerID, _ CardID, _ mana.Colors, _, _ int) mana.Colors {
	return popQueue(&c.colorChoices, "color choice")
}

// QueueNumberChoice appends the answer to the next ChooseNumber call.
func (c *ScriptedController) QueueNumberChoice(n int) { c.numberChoices = append(c.numberChoices, n) }

// ChooseNumber returns the next answer QueueNumberChoice queued.
func (c *ScriptedController) ChooseNumber(_ *Game, _ PlayerID, _ CardID, _, _ int) int {
	return popQueue(&c.numberChoices, "number choice")
}

// QueueTapOrUntap appends the answer to the next ChooseTapOrUntap call.
func (c *ScriptedController) QueueTapOrUntap(tap bool) { c.tapOrUntap = append(c.tapOrUntap, tap) }

// ChooseTapOrUntap returns the next answer QueueTapOrUntap queued.
func (c *ScriptedController) ChooseTapOrUntap(_ *Game, _ PlayerID, _ CardID) bool {
	return popQueue(&c.tapOrUntap, "tap-or-untap")
}

// QueueEntityChoice appends the answer to the next ChooseEntitiesForEffect call.
func (c *ScriptedController) QueueEntityChoice(ids []EntityID) {
	c.entityChoices = append(c.entityChoices, ids)
}

// ChooseEntitiesForEffect returns the next answer QueueEntityChoice queued.
func (c *ScriptedController) ChooseEntitiesForEffect(_ *Game, _ PlayerID, _ CardID, _ []EntityID, _, _ int) []EntityID {
	return popQueue(&c.entityChoices, "entity choice")
}

// QueueConfirmReveal appends the answer to the next ConfirmReveal call.
func (c *ScriptedController) QueueConfirmReveal(v bool) { c.confirmReveal = append(c.confirmReveal, v) }

// ConfirmReveal returns the next answer QueueConfirmReveal queued.
func (c *ScriptedController) ConfirmReveal(_ *Game, _ PlayerID, _ CardID) bool {
	return popQueue(&c.confirmReveal, "confirm reveal")
}

// QueueConfirmEffect appends the answer to the next ConfirmEffect call.
func (c *ScriptedController) QueueConfirmEffect(v bool) { c.confirmEffect = append(c.confirmEffect, v) }

// ConfirmEffect returns the next answer QueueConfirmEffect queued.
func (c *ScriptedController) ConfirmEffect(_ *Game, _ PlayerID, _ CardID) bool {
	return popQueue(&c.confirmEffect, "confirm effect")
}

// QueueCardOrder appends the answer to the next OrderCardsForZone call.
func (c *ScriptedController) QueueCardOrder(ids []CardID) { c.cardOrders = append(c.cardOrders, ids) }

// OrderCardsForZone returns the next answer QueueCardOrder queued.
func (c *ScriptedController) OrderCardsForZone(_ *Game, _ PlayerID, _ []CardID, _ ZoneType) []CardID {
	return popQueue(&c.cardOrders, "card order")
}

// QueueAbilityChoice appends the answer to the next ChooseAbilitiesForEffect call.
func (c *ScriptedController) QueueAbilityChoice(idx []int) {
	c.abilityChoices = append(c.abilityChoices, idx)
}

// ChooseAbilitiesForEffect returns the next answer QueueAbilityChoice queued.
func (c *ScriptedController) ChooseAbilitiesForEffect(_ *Game, _ PlayerID, _ CardID, _ []string, _ int) []int {
	return popQueue(&c.abilityChoices, "ability choice")
}

// QueueModeChoice appends the answer to the next ChooseModesForAbility call.
func (c *ScriptedController) QueueModeChoice(idx []int) { c.modeChoices = append(c.modeChoices, idx) }

// ChooseModesForAbility returns the next answer QueueModeChoice queued.
func (c *ScriptedController) ChooseModesForAbility(_ *Game, _ PlayerID, _ CardID, _ []string, _, _ int) []int {
	return popQueue(&c.modeChoices, "mode choice")
}

// QueueProtectionChoice appends the answer to the next ChooseProtectionType call.
func (c *ScriptedController) QueueProtectionChoice(i int) {
	c.protectionChoice = append(c.protectionChoice, i)
}

// ChooseProtectionType returns the next answer QueueProtectionChoice queued.
func (c *ScriptedController) ChooseProtectionType(_ *Game, _ PlayerID, _ CardID, _ []string) int {
	return popQueue(&c.protectionChoice, "protection choice")
}

// QueueCoinCall appends the answer to the next CallCoinFlip call.
func (c *ScriptedController) QueueCoinCall(heads bool) { c.coinCalls = append(c.coinCalls, heads) }

// CallCoinFlip returns the next answer QueueCoinCall queued.
func (c *ScriptedController) CallCoinFlip(_ *Game, _ PlayerID, _ CardID) bool {
	return popQueue(&c.coinCalls, "coin call")
}

// QueueCardOnTop appends the answer to the next WillPutCardOnTop call.
func (c *ScriptedController) QueueCardOnTop(top bool) { c.cardOnTop = append(c.cardOnTop, top) }

// WillPutCardOnTop returns the next answer QueueCardOnTop queued.
func (c *ScriptedController) WillPutCardOnTop(_ *Game, _ PlayerID, _ CardID) bool {
	return popQueue(&c.cardOnTop, "card on top")
}

// QueueBinary appends the answer to the next ChooseBinary call.
func (c *ScriptedController) QueueBinary(first bool) { c.binary = append(c.binary, first) }

// ChooseBinary returns the next answer QueueBinary queued.
func (c *ScriptedController) ChooseBinary(_ *Game, _ PlayerID, _ CardID, _ BinaryChoice) bool {
	return popQueue(&c.binary, "binary choice")
}

// QueueOption appends the answer to the next ChooseOption call.
func (c *ScriptedController) QueueOption(i int) { c.option = append(c.option, i) }

// ChooseOption returns the next answer QueueOption queued.
func (c *ScriptedController) ChooseOption(_ *Game, _ PlayerID, _ CardID, _ []string) int {
	return popQueue(&c.option, "option")
}

// QueueSector appends the answer to the next ChooseSector call: an index
// into the sectors offered (0 Alpha, 1 Beta, 2 Gamma).
func (c *ScriptedController) QueueSector(i int) { c.sector = append(c.sector, i) }

// ChooseSector returns the next answer QueueSector queued.
func (c *ScriptedController) ChooseSector(_ *Game, _ PlayerID, _ CardID, _ []string) int {
	return popQueue(&c.sector, "sector")
}

// QueueRoomDoor appends the answer to the next ChooseRoomDoor call.
func (c *ScriptedController) QueueRoomDoor(d Door) { c.roomDoor = append(c.roomDoor, d) }

// ChooseRoomDoor returns the next answer QueueRoomDoor queued.
func (c *ScriptedController) ChooseRoomDoor(_ *Game, _ PlayerID, _ CardID, _ []Door) Door {
	return popQueue(&c.roomDoor, "room door")
}

// QueueAction appends a to pid's own priority-answer queue, consumed by
// pid's next TakeAction call. Per player (ADR-0019), not one shared FIFO:
// CR 117.3c lets a player be asked again immediately after acting, so a
// single shared queue would let that second ask pop an entry meant for a
// different player's own later turn to act.
func (c *ScriptedController) QueueAction(pid PlayerID, a Action) {
	c.actionsFor(pid, true)
	*c.actionQueue[pid] = append(*c.actionQueue[pid], a)
}

// TakeAction returns pid's own next queued Action, or ActionPass if none is
// queued -- the one ScriptedController decision that defaults instead of
// panicking on empty (control.go's own struct comment covers why every
// other one panics): CR 117.3c means TakeAction is asked an unbounded
// number of times per priority round, and "nothing left queued for this
// player" is the ordinary way every round ends, not a fixture mistake.
func (c *ScriptedController) TakeAction(_ *Game, pid PlayerID) Action {
	q := c.actionsFor(pid, false)
	if q == nil || len(*q) == 0 {
		return Action{}
	}
	v := (*q)[0]
	*q = (*q)[1:]
	return v
}

// actionsFor returns pid's own queue slot, growing actionQueue to cover pid
// if grow is true (QueueAction's own call) or returning nil if it does not
// exist yet (TakeAction's own call -- an unqueued player has simply never
// been queued anything, the same as an empty queue).
func (c *ScriptedController) actionsFor(pid PlayerID, grow bool) *[]Action {
	if int(pid) >= len(c.actionQueue) {
		if !grow {
			return nil
		}
		grown := make([]*[]Action, int(pid)+1)
		copy(grown, c.actionQueue)
		c.actionQueue = grown
	}
	if c.actionQueue[pid] == nil {
		if !grow {
			return nil
		}
		c.actionQueue[pid] = &[]Action{}
	}
	return c.actionQueue[pid]
}

// popQueue pops the head of one ScriptedController queue, panicking with kind
// when it is empty (scriptExhausted).
func popQueue[T any](q *[]T, kind string) T {
	if len(*q) == 0 {
		panic(scriptExhausted(kind))
	}
	v := (*q)[0]
	*q = (*q)[1:]
	return v
}

// scriptExhausted is what a queue running dry mid-scenario panics with. It
// names the decision kind, because a fixture with several queues needs to
// know which one came up short.
func scriptExhausted(kind string) string {
	return fmt.Sprintf("engine: scripted controller ran out of %s decisions", kind)
}
