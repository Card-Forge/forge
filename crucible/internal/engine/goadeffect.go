package engine

//enginelint:allow game ability control controlcommands effecthelpers card condition defined zone id parts untilcommand

import "fmt"

// goadEffect is GoadEffect.java (CR 701.15): each Defined$ or targeted
// permanent on the battlefield is goaded by the activator -- until that
// player's next turn, or for good with Duration$ Permanent. A goaded
// creature attacks each combat if able, and attacks a player other than a
// goading player if able (DeclareCombatAttackers, chooseAttackTargets).
// NoLonger$ ends every goad on it; RememberGoaded$ remembers it.
type goadEffect struct{}

func (goadEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Goad"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	duration, ok := a.Params.Param("Duration")
	if !ok {
		duration = "UntilYourNextTurn"
	}
	until := ""
	if duration != "UntilYourNextTurn" && duration != "Permanent" {
		if err := durationRefused("Goad", duration); err != nil {
			return err
		}
		if endsAtCleanup(duration) {
			return fmt.Errorf("engine: Goad: Duration$ %q not resolvable yet", duration)
		}
		until = duration
	}
	// GoadEffect never calls checkValidDuration: a host that already left
	// play registers a command nothing runs, and the goad stays (Java quirk,
	// reproduced for parity per PORT-7).
	g.timestamp++
	ts := g.timestamp
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Goad: %w", err)
	}
	for _, id := range cards {
		c := g.Card(id)
		if c.Zone != Battlefield {
			continue
		}
		if hasParam(a, "NoLonger") {
			c.goadedBy = nil
			continue
		}
		c.goadedBy = append(c.goadedBy, goad{By: a.Controller, Permanent: duration == "Permanent" || until != "", Timestamp: ts})
		if until != "" {
			if err := g.addUntilCommand(a, until, cardCommand{Kind: commandEndGoad, Target: id, Timestamp: ts}); err != nil {
				return err
			}
		}
		if hasParam(a, "RememberGoaded") {
			source.Memory.Remember(CardEntity(id))
		}
	}
	return nil
}

// goad is one goading of a creature: who goaded it, and whether it lasts.
type goad struct {
	By        PlayerID
	Permanent bool
	// Timestamp is the resolution's timestamp, the key GoadEffect's removeGoad
	// closure ends it by.
	Timestamp uint64
}

// IsGoaded reports whether c is goaded.
func (c *Card) IsGoaded() bool { return len(c.goadedBy) > 0 }

// goadedBy reports whether p goaded c.
func (c *Card) isGoadedBy(p PlayerID) bool {
	for _, gd := range c.goadedBy {
		if gd.By == p {
			return true
		}
	}
	return false
}

// endGoads ends every non-permanent goad p made, as p's turn begins.
func (g *Game) endGoads(p PlayerID) {
	for i := range g.cards {
		c := &g.cards[i]
		if len(c.goadedBy) == 0 {
			continue
		}
		kept := c.goadedBy[:0]
		for _, gd := range c.goadedBy {
			if gd.Permanent || gd.By != p {
				kept = append(kept, gd)
			}
		}
		c.goadedBy = kept
	}
}

// goadTargets narrows an attacker's eligible attack targets to the ones it can
// attack at all (attackableTargets: CantAttack statics, Defender), then to players who
// did not goad it, when it is goaded and any such player can be attacked.
func (g *Game) goadTargets(attacker CardID, eligible []EntityID) []EntityID {
	eligible = g.attackableTargets(attacker, eligible)
	c := g.Card(attacker)
	if !c.IsGoaded() {
		return eligible
	}
	var others []EntityID
	for _, e := range eligible {
		if p, ok := e.AsPlayer(); ok && !c.isGoadedBy(p) {
			others = append(others, e)
		}
	}
	if len(others) == 0 {
		return eligible
	}
	return others
}
