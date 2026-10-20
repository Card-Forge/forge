package engine

//enginelint:allow id card game ability defined condition control amount parts zone effecthelpers

import "fmt"

// peekAndRevealUnresolvedParams are PeekAndRevealEffect.java's params this
// port cannot honour yet: SourceZone$ (1 real line; everything else peeks at
// the library); a bare CheckSVar$ is an activation gate otherRestrictionsMet
// enforces.
var peekAndRevealUnresolvedParams = [...]string{
	"SourceZone"}

// peekAndRevealEffect is PeekAndRevealEffect.java: the activator looks at
// the top PeekAmount$ (default 1) cards of each Defined$/ValidTgts$
// player's library (default You), and reveals the ones matching
// RevealValid$ (default any card) unless NoReveal$ is set or nothing
// matches; RevealOptional$ asks first (ConfirmReveal). Revealed cards are
// remembered (RememberRevealed$) or imprinted (ImprintRevealed$); an
// unrevealed peek remembers them under RememberPeeked$ instead. NoPeek$
// only suppresses the private look, which changes no state here.
//
// Java clamps its numPeek variable to each player's library size in place,
// so a small library shrinks the peek for every later player in the same
// loop; that is reproduced (PORT-7), since parity depends on it.
type peekAndRevealEffect struct{}

func (peekAndRevealEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range peekAndRevealUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: PeekAndReveal: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	peekAmount, ok := a.Params.Param("PeekAmount")
	if !ok {
		peekAmount = "1"
	}
	numPeek, ok := resolveNamedAmount(g, a.Amounts, source, peekAmount)
	if !ok {
		return fmt.Errorf("engine: PeekAndReveal: PeekAmount$ %q not resolvable yet", peekAmount)
	}
	revealValid, ok := a.Params.Param("RevealValid")
	if !ok {
		revealValid = "Card"
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: PeekAndReveal: %w", err)
	}
	_, noReveal := a.Params.Param("NoReveal")
	_, revealOptional := a.Params.Param("RevealOptional")
	_, rememberRevealed := a.Params.Param("RememberRevealed")
	_, imprintRevealed := a.Params.Param("ImprintRevealed")
	_, rememberPeeked := a.Params.Param("RememberPeeked")

	for _, pid := range players {
		library := g.Zone(Library, pid).Cards()
		if numPeek > len(library) {
			numPeek = len(library)
		}
		peeked := library[:numPeek]
		revealable := filterValid(g, peeked, revealValid, a.Controller, a.Source)
		doReveal := !noReveal && len(revealable) > 0
		if doReveal && revealOptional {
			doReveal = controller.ConfirmReveal(g, a.Controller, a.Source)
		}
		switch {
		case doReveal:
			for _, cid := range revealable {
				if rememberRevealed {
					source.Memory.Remember(CardEntity(cid))
				}
				if imprintRevealed {
					source.Memory.Imprint(cid)
				}
			}
		case rememberPeeked:
			for _, cid := range revealable {
				source.Memory.Remember(CardEntity(cid))
			}
		}
	}
	return nil
}
