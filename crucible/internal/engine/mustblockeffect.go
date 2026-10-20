// MustBlock: "target creature blocks CARDNAME this turn if able" -- a
// block requirement recorded on the blocker (CR 509.1c), read by the block
// validator (blockvalidation.go, ADR-0024).

package engine

//enginelint:allow id zone card game ability defined condition control effecthelpers resolvechoice

import "fmt"

// mustBlockReq is one attacker a creature must block if able, and whether
// the requirement ends at end of combat (Duration$ UntilEndOfCombat) rather
// than at cleanup.
type mustBlockReq struct {
	Attacker         CardID
	UntilEndOfCombat bool
}

// MustBlockAttackers is Card.getMustBlockCards: every attacker c must block
// if able, in the order the requirements were made.
func (c *Card) MustBlockAttackers() []CardID {
	if len(c.mustBlock) == 0 {
		return nil
	}
	out := make([]CardID, 0, len(c.mustBlock))
	for _, r := range c.mustBlock {
		out = append(out, r.Attacker)
	}
	return out
}

// endMustBlocks ends block requirements: at end of combat only the
// Duration$ UntilEndOfCombat ones (addUntilCommand's EndOfCombat.addUntil),
// at cleanup all of them (Card.onCleanupPhase's clearMustBlockCards, run for
// every battlefield card). A card off the battlefield has none left to end
// (Game.Move clears them).
func (g *Game) endMustBlocks(endOfCombatOnly bool) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if len(c.mustBlock) == 0 {
				continue
			}
			if !endOfCombatOnly {
				c.mustBlock = nil
				continue
			}
			kept := c.mustBlock[:0]
			for _, r := range c.mustBlock {
				if !r.UntilEndOfCombat {
					kept = append(kept, r)
				}
			}
			if len(kept) == 0 {
				kept = nil
			}
			c.mustBlock = kept
		}
	}
}

// mustBlockEffect is MustBlockEffect.java: each targeted (or Defined$)
// creature must block the attacker -- DefinedAttacker$, default the host --
// if able; with BlockAllDefined$, every DefinedAttacker$ card. The
// requirement is recorded on the blocker (MustBlockEffect.java:76/:79) and
// checked when blockers are declared (validateBlocks, blockvalidation.go).
// A creature no longer on the battlefield is skipped (Java's
// equalsWithGameTimestamp: a card that changed zones is a new object).
//
// Choices$ with Chooser$ (Crashing Boars) lets the chooser pick the blocker.
// Rejected: Duration$ values other than UntilEndOfCombat (none in the corpus).
// DefinedAttacker$ ParentTarget and "Valid ..." fail in definedCards: a
// sub-ability is not targeted separately from its parent (subability.go),
// and definedCards has no valid-string form.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/MustBlockEffect.java's
// resolve.
type mustBlockEffect struct{}

func (mustBlockEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	untilEndOfCombat := false
	if d, ok := a.Params.Param("Duration"); ok {
		if d != "UntilEndOfCombat" {
			return fmt.Errorf("engine: MustBlock: Duration$ %q not resolvable yet", d)
		}
		untilEndOfCombat = true
	}
	attackers := []CardID{a.Source}
	if spec, ok := a.Params.Param("DefinedAttacker"); ok {
		cards, err := definedCards(source, spec, a.refs())
		if err != nil {
			return fmt.Errorf("engine: MustBlock: DefinedAttacker$: %w", err)
		}
		if len(cards) == 0 {
			return nil
		}
		attackers = cards
	}
	if !hasParam(a, "BlockAllDefined") {
		attackers = attackers[:1]
	}
	var blockers []CardID
	if spec, ok := a.Params.Param("Choices"); ok {
		// MustBlockEffect.java:39-58: Chooser$ (default the activator) picks one
		// battlefield card matching Choices$; none to pick is no requirement.
		chooser := a.Controller
		if raw, ok := a.Params.Param("Chooser"); ok {
			players, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
			if err != nil {
				return fmt.Errorf("engine: MustBlock: Chooser$: %w", err)
			}
			if len(players) == 0 {
				return fmt.Errorf("engine: MustBlock: Chooser$ %q names no player", raw)
			}
			chooser = players[0]
		}
		choices, err := choiceCandidates(g, a, "MustBlock", []ZoneType{Battlefield}, spec)
		if err != nil {
			return err
		}
		if len(choices) > 0 {
			picked, err := askChoiceCards(g, controller, a, "MustBlock", chooser, choices, 1, 1)
			if err != nil {
				return err
			}
			blockers = picked
		}
	} else {
		var err error
		if blockers, err = targetedOrDefinedCards(source, a.Params, a.refs()); err != nil {
			return fmt.Errorf("engine: MustBlock: %w", err)
		}
	}
	for _, id := range blockers {
		c := g.Card(id)
		// A phased-out permanent is treated as though it doesn't exist
		// (CR 702.26b) -- c.Zone still reads Battlefield (ADR-0021 decision
		// 3), so this needs its own guard, matching Java's skip
		// (MustBlockEffect.java:72).
		if c.Zone != Battlefield || c.IsPhasedOut() {
			continue
		}
		for _, att := range attackers {
			c.mustBlock = append(c.mustBlock, mustBlockReq{Attacker: att, UntilEndOfCombat: untilEndOfCombat})
		}
	}
	return nil
}
