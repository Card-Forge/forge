// Effect dispatch: the contract, and the array it is looked up in.

package engine

//enginelint:allow id zone parts card player game ability control subability defined manapay unlesscost textwords costparts

import (
	"errors"
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// Effect resolves one ability.
//
// Implementations are stateless shared values, which is what Java measured
// rather than what this port assumes: ApiType's isStateLess parameter defaults
// to true and the count of constants passing false is zero. Everything an
// effect needs comes from the game and the ability it is handed, so there is
// no per-resolution allocation and no instance per card.
//
// Resolve takes the resolving player's controller too, threaded from
// ResolveStack's own parameter of the same name -- discardEffect's own
// Mode$ TgtChoose (discardeffect.go) is the first implementation that needs
// to ask a player anything mid-resolution rather than reading the game state
// outright; every effect before it ignores the parameter.
type Effect interface {
	Resolve(g *Game, a *Ability, controller PlayerController) error
}

//go:generate go run ../../tools/genregistry

// Registry maps an API to the code that resolves it.
//
// An array rather than a map: dispatch sits under the stack resolution loop,
// so it is an index into an interface value with no allocation and no hashing
// (ADR-0008).
type Registry [numAPITypes]Effect

// ErrUnimplemented is what an unregistered API resolves to. During the port
// most of the array is empty, and a gap has to be a clear diagnostic naming
// the API rather than a nil dereference.
var ErrUnimplemented = errors.New("engine: no effect registered for API")

// Resolve asks first, if a.Optional says to (CR 603.3d's own "may" trigger,
// WrappedAbility.resolve()'s own `if (decider != null) { if
// (!decider.getController().confirmTrigger(this)) return; }`, checked before
// anything else there too), then dispatches to its effect, then chains its
// own SubAbility$ if it names one (resolveSubAbility, subability.go) --
// AbilityUtils.resolveApiAbility's own "sa.resolve(); resolveSubAbilities(sa,
// game)" pairing, recursive through this same method for a chain more than
// one deep. A decline skips both -- the whole ability, chain included, never
// ran -- the identical early return WrappedAbility.resolve() gives before
// ever reaching its own playSpellAbilityNoStack call.
//
// A missing effect is an error and not a panic: it is a gap in the port, which
// the corpus coverage gate tracks, not an invariant breach (GO-7, ADR-0011).
// One unimplemented API must fail its game and no more -- including one
// reached only by chaining into a SubAbility$ this port has not implemented
// yet, the identical error a card naming it as its own top-level ability
// would already get. Checked after the optional confirm, not before: a
// declined "may" is real Magic's own outcome regardless of whether this port
// can run the ability behind it, so asking first and failing loudly only once
// something would actually try to run matches CR 603.3d more closely than
// erroring out a card a controller would have declined anyway.
//
// An ability naming UnlessCost$ takes resolveUnlessCost's own alternative
// route instead of the plain Resolve/resolveSubAbility pairing below --
// AbilityUtils.resolveApiAbility's own if/else between `sa.resolve()` and
// `handleUnlessCost(sa, game)`, the same branch point.
//
// A static trigger the effect set off at a site with no error return (an
// effect tapping a land for mana, activateabilityeffect.go) leaves its error
// pending on the Game; it is returned here, once the effect has run, as this
// ability's own failure (ADR-0020 decision 4).
func (r *Registry) Resolve(g *Game, a *Ability, controller PlayerController) error {
	err := r.resolve(g, a, controller)
	if g != nil {
		if pending := g.TakePendingError(); pending != nil {
			if err == nil {
				return pending
			}
			return errors.Join(err, pending)
		}
	}
	return err
}

// resolve is Resolve without the pending static-trigger error check.
func (r *Registry) resolve(g *Game, a *Ability, controller PlayerController) error {
	if g != nil {
		// A spell whose text a word change reached since it was cast resolves
		// the rewritten text (textwords.go, ADR-0039).
		a = g.textChangedSpell(a)
		g.registry = r
		prev := g.xctx
		g.xctx = xContext{value: a.xManaCostPaid, has: a.hasXManaCostPaid, source: a.Source}
		prevResolving := g.resolving
		g.resolving = a
		defer func() { g.xctx, g.resolving = prev, prevResolving }()
	}
	if !a.stackRoot && rememberTargetsLine(a.Params) {
		// AbilityUtils.handleRemembering runs on the ability popped off the
		// stack alone (MagicStack.java:646): a sub-ability's, a Charm mode's or
		// a choice's RememberTargets$ never reaches it, so the targets are never
		// remembered and the chain reading Remembered acts on nothing. A Forge
		// script defect (Volcanic Offering's DBDestroyLand, Urza, Academy
		// Headmaster's Destroy11U), refused rather than half-resolved (PORT-8,
		// GO-7).
		return fmt.Errorf("engine: %s: RememberTargets$ off the stack's own ability never reaches AbilityUtils.handleRemembering (MagicStack.java:646), a script defect", a.API)
	}
	if a.evolve != NoCard && !g.Card(a.Source).evolvedBy(g.Card(a.evolve)) {
		return nil
	}
	if a.Optional && !controller.ConfirmOptionalTrigger(g, a.Controller, a.Source) {
		return nil
	}
	if a.targetsErr != nil {
		// The targets (or their division, or who chooses them) could not be
		// asked for as the ability went on the stack (resolveTargets,
		// resolveChainTargets): it fails here, before any cost is paid or any
		// effect acts on a target set nobody chose (GO-7).
		return fmt.Errorf("engine: %s: %w", a.API, a.targetsErr)
	}
	if paid, err := r.payTriggeredCost(g, a, controller); err != nil || !paid {
		return err
	}
	if int(a.API) >= numAPITypes {
		return fmt.Errorf("%w: %s", ErrUnimplemented, a.API)
	}
	e := r[a.API]
	if e == nil {
		return fmt.Errorf("%w: %s", ErrUnimplemented, a.API)
	}
	a.game = g
	if g != nil && a.Params != nil && (a.isTrigger || a.Params.Record != compile.Spell) {
		// AbilityUtils.resolve counts each resolution of a non-wrapper ability,
		// a sub-ability included (AbilityUtils.java:1313-1319): ResolvedLimit$'s
		// state, and Count$ResolvedThisTurn's.
		g.Card(a.Source).trigResolved.note(a.Params)
	}
	if a.Params != nil {
		if unlessCost, ok := a.Params.Param("UnlessCost"); ok {
			return r.resolveUnlessCost(g, a, controller, e, unlessCost)
		}
	}
	if err := resolveEffectBatched(g, e, a, controller); err != nil {
		return err
	}
	// A static trigger the effect set off failed: stop here, before the
	// sub-ability chain changes anything more (GO-7, ADR-0020 decision 4).
	if g != nil && g.pendingErr != nil {
		return nil
	}
	return r.resolveSubAbility(g, a, controller)
}

// resolveEffectBatched runs one effect's body inside a trigger batch
// (beginTriggerBatch): everything it sets off -- each card it moves entering
// or dying, then ChangesZoneAll for the lot, the damage it deals -- reaches
// the stack in one CR 603.3b APNAP pass once the body is done, as Java's
// frozen stack holds them in simultaneousStackEntryList for one ordering
// (TriggerHandler.java:264, MagicStack.java:855-880). A nested resolution (a
// RepeatEach's or Charm's sub-ability inside the body) joins the outer batch.
func resolveEffectBatched(g *Game, e Effect, a *Ability, controller PlayerController) error {
	if g == nil {
		return e.Resolve(g, a, controller)
	}
	g.beginTriggerBatch()
	defer g.endTriggerBatch(controller)
	return e.Resolve(g, a, controller)
}

// payTriggeredCost is the Cost$ half of WrappedAbility.resolve's
// playSpellAbilityNoStack (WrappedAbility.java:440): a trigger whose Execute$
// is an AB$ line with a Cost$ pays it as the ability resolves, and the
// ability does nothing when the cost is not paid. Java makes such a trigger
// optional -- "triggers with a cost can't be mandatory"
// (TriggerHandler.java:511) -- unless the cost says Mandatory or is 0, so the
// controller is asked once (ConfirmOptionalTrigger) unless an OptionalDecider$
// already did. An ability whose cost ActivateAbility already paid
// (Ability.costPaid), one of the host's own printed A: lines, and every DB$
// line skip this. An X in the cost is announced once the controller agreed to
// pay (announceTriggeredX) or, when the card's X is computed, evaluated
// (triggeredCostShape). A cost parseUnlessCost does not read is an error
// rather than a free resolution (GO-7). paid reports whether the ability goes
// on to resolve.
func (r *Registry) payTriggeredCost(g *Game, a *Ability, controller PlayerController) (paid bool, err error) {
	if a.costPaid || a.Params == nil || a.Params.Record != compile.Activated || isOwnActivatedAbility(g.Card(a.Source), a.Params) {
		return true, nil
	}
	text, ok := a.Params.Param("Cost")
	if !ok || text == "0" {
		return true, nil
	}
	uc, announce, err := g.triggeredCostShape(a, text, controller)
	if err != nil {
		return false, err
	}
	if !a.Optional && !uc.mandatory && !controller.ConfirmOptionalTrigger(g, a.Controller, a.Source) {
		return false, nil
	}
	if announce {
		if uc, ok = g.announceTriggeredX(controller, a, text, uc); !ok {
			return false, nil
		}
	}
	// The cost is the ability's own: what it sacrificed or discarded is the
	// paid list its Sacrificed$/Discarded$ amounts read (Disciple of
	// Freyalise's "X is that creature's power").
	paid = g.payUnlessCostInto(controller, a, a.Controller, uc, &a.paid)
	a.paid.recorded = paid
	return paid, nil
}

// resolveUnlessCost is CR's own "unless a cost is paid" gate --
// AbilityUtils.handleUnlessCost, ported apart from the ordinary
// Resolve/resolveSubAbility pairing above because Java's own version
// decides both whether the ability's body runs AND whether/when its own
// SubAbility$ chains, in one place, rather than falling through to the
// identical unconditional trailing call every other ability gets.
//
// Each of UnlessPayer$'s own players (definedPlayers, reused; an absent
// UnlessPayer$ defaults to "TargetedController" as in Java) is asked
// ConfirmPayCost in turn and, on a yes, actually charged via PayManaCost
// (manapay.go) -- payCostToPreventEffect's own "decide, then pay" pairing,
// split the identical way every other mana decision on PlayerController
// already is. The ability's own body runs when paying did NOT happen
// (handleUnlessCost's own `alreadyPaid == isSwitched`, isSwitched false by
// default -- UnlessSwitched$'s own presence flips it, "pay to make it
// happen instead"). UnlessResolveSubs$ decides whether the chained
// SubAbility$ still runs regardless (absent, Java's own "Always") or only
// on one particular outcome ("WhenPaid"/"WhenNotPaid").
//
// Trimmed to what parseUnlessCost (unlesscost.go) reads: mana tokens,
// PayLife<N>, Discard<N/Card> and one Sac<N/Type> part -- no Tap/Untap/
// Mandatory/XMin token and no X shard, each its own further mechanic -- and
// the payers definedPlayers resolves. A cost this port cannot read fails loudly here.
// Of the corpus's 727 real UnlessCost$ lines, those with a readable cost are
// reachable by this port at all -- an activated ability's own
// Cost$-gated UnlessCost$ line composes with ActivateAbility
// (activateability.go) too, now that general activated-ability casting is
// built, an instant/sorcery's own top-level UnlessCost$ line still does not
// (CastSpell's own doc comment: "an instant or sorcery resolves into a
// script effect this port does not build"), and a line reached only through
// an unbuilt API's own SubAbility$/RepeatSubAbility$/... chain link (DB$
// Effect, DB$ Repeat, DB$ GenericChoice, DB$ DelayedTrigger, S:...
// AddTrigger$'s own dynamically granted trigger, none of them built) all
// fail loudly one hop up the call chain rather than here, PORT-8/GO-7's
// "skip the whole line" applied at whichever link in the chain the actual
// gap sits.
func (r *Registry) resolveUnlessCost(g *Game, a *Ability, controller PlayerController, e Effect, unlessCostText string) error {
	// Ward's several costs ("Ward:Discard<1/Card>:2", Ward.parse) arrive as one
	// text: the payer picks which to pay (a GenericChoice, CardFactoryUtil's
	// Ward branch), or none and the spell is countered.
	var alternatives []unlessCost
	for _, part := range strings.Split(unlessCostText, ":") {
		text, err := g.expandUnlessCost(a, part, controller)
		if errors.Is(err, errUnlessNoCost) {
			// calculateUnlessCost returned null (AbilityUtils.java:1416-1420): the
			// ability resolves with nothing to pay.
			if err := resolveEffectBatched(g, e, a, controller); err != nil {
				return err
			}
			return r.resolveSubAbility(g, a, controller)
		}
		if err != nil {
			return err
		}
		if text == "LifeTotalHalfUp" {
			// Each payer's own cost, set below (AbilityUtils.java:1433-1436).
			alternatives = append(alternatives, unlessCost{})
			continue
		}
		uc, ok := parseUnlessCost(text)
		if !ok {
			return fmt.Errorf("engine: UnlessCost$ %q not resolvable yet", unlessCostText)
		}
		uc, err = uc.withTriggered(g, a)
		if err != nil {
			return err
		}
		alternatives = append(alternatives, uc.withTargets(a))
	}

	// UnlessPayer$ defaults to TargetedController (AbilityUtils.java:1407): the
	// controller of each card the ability targets, nobody for one with no
	// target.
	payerSpec, ok := a.Params.Param("UnlessPayer")
	if !ok {
		payerSpec = "TargetedController"
	}
	payers, err := definedPlayers(g, a.Controller, a.Source, payerSpec, a.refs())
	if err != nil {
		return fmt.Errorf("engine: UnlessPayer$: %w", err)
	}

	resolveSubs, hasResolveSubs := a.Params.Param("UnlessResolveSubs")
	execWhenPaid := !hasResolveSubs || resolveSubs == "WhenPaid"
	execWhenNotPaid := !hasResolveSubs || resolveSubs == "WhenNotPaid"
	_, switched := a.Params.Param("UnlessSwitched")

	paid := false
	for _, pid := range payers {
		alts := alternatives
		if strings.TrimSpace(unlessCostText) == "LifeTotalHalfUp" {
			// PayLife<ceil(life / 2)>, 0 for a payer at or below 0 life.
			half, _ := parseUnlessCost(fmt.Sprintf("PayLife<%d>", max(0, (g.Player(pid).Life+1)/2)))
			alts = []unlessCost{half}
		}
		uc, ok := g.pickUnlessAlternative(controller, a, pid, alts)
		if !ok {
			continue
		}
		if (uc.mandatory || controller.ConfirmPayCost(g, pid, uc.parsed, a.Source)) && g.payUnlessCost(controller, a, pid, uc) {
			paid = true
		}
	}

	if paid == switched {
		if err := resolveEffectBatched(g, e, a, controller); err != nil {
			return err
		}
	}
	if paid && execWhenPaid || !paid && execWhenNotPaid {
		return r.resolveSubAbility(g, a, controller)
	}
	return nil
}

// pickUnlessAlternative is the cost pid is asked to pay: the only one, or the
// one pid picks among those whose non-mana parts they can pay (Ward's
// GenericChoice; its FallbackAbility counters when none can be). ok is false
// when there is nothing to pay.
func (g *Game) pickUnlessAlternative(controller PlayerController, a *Ability, pid PlayerID, alternatives []unlessCost) (unlessCost, bool) {
	if len(alternatives) == 1 {
		return alternatives[0], true
	}
	var payable []unlessCost
	var labels []string
	for _, uc := range alternatives {
		if g.unlessPayable(pid, a.Source, uc) {
			payable = append(payable, uc)
			labels = append(labels, uc.parsed.Text)
		}
	}
	switch len(payable) {
	case 0:
		return unlessCost{}, false
	case 1:
		return payable[0], true
	}
	i := controller.ChooseOption(g, pid, a.Source, labels)
	if i < 0 || i >= len(payable) {
		i = 0
	}
	return payable[i], true
}

// Implemented is how many APIs have an effect. The corpus coverage report
// reads it, so progress through M6 is measured rather than estimated.
func (r *Registry) Implemented() int {
	n := 0
	for _, e := range r {
		if e != nil {
			n++
		}
	}
	return n
}

// NumAPIs is how many ability APIs Forge declares.
func NumAPIs() int { return numAPITypes }

// isOwnActivatedAbility reports whether ab is one of host's own printed A:
// lines -- abilities whose Cost$ ActivateAbility (activateability.go) pays
// before they reach the stack, however they were pushed.
func isOwnActivatedAbility(host *Card, ab *compile.Ability) bool {
	if host.Def == nil {
		return false
	}
	for _, own := range host.Def.Faces[0].Abilities {
		if own == ab {
			return true
		}
	}
	return false
}
