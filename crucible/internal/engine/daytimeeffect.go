package engine

//enginelint:allow game ability control effecthelpers condition card id zone

import "fmt"

// DayTime is Game.daytime (CR 726): neither until an effect or a daybound
// permanent makes it day or night.
type DayTime uint8

// The three day/night states.
const (
	DayNeither DayTime = iota
	Day
	Night
)

// DayTime reports whether it is day, night, or neither.
func (g *Game) DayTime() DayTime { return g.dayTime }

// dayTimeEffect is DayTimeEffect.java: Value$ Day or Night sets it, Switch
// flips it (neither counts as day). Daybound and nightbound permanents turn
// over when it changes, which needs transforming this port does not model,
// so the effect fails while one is on the battlefield; a CantChangeDayTime
// static fails it the same way.
type dayTimeEffect struct{}

func (dayTimeEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "DayTime"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if g.daynightBoundInPlay() || battlefieldStaticNames(g, "CantChangeDayTime") {
		return fmt.Errorf("engine: DayTime: daybound/nightbound permanents or CantChangeDayTime not resolvable yet")
	}
	value, _ := a.Params.Param("Value")
	switch value {
	case "Day":
		g.dayTime = Day
	case "Night":
		g.dayTime = Night
	case "Switch":
		if g.dayTime == Night {
			g.dayTime = Day
		} else {
			g.dayTime = Night
		}
	default:
		return fmt.Errorf("engine: DayTime: Value$ %q not resolvable", value)
	}
	return nil
}

// dayTimeAtUntap is Untap.doDayTime (CR 726.3a): as a turn's untap step
// begins, day becomes night if the previous turn's active player cast no
// spells that turn, and night becomes day if they cast two or more.
func (g *Game) dayTimeAtUntap() {
	if g.previousPlayer == NoPlayer {
		return
	}
	switch {
	case g.dayTime == Day && g.previousPlayerSpells == 0:
		g.dayTime = Night
	case g.dayTime == Night && g.previousPlayerSpells > 1:
		g.dayTime = Day
	}
}

// daynightBoundInPlay reports whether a daybound or nightbound permanent is
// on the battlefield.
func (g *Game) daynightBoundInPlay() bool {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if c.HasKeyword("Daybound") || c.HasKeyword("Nightbound") {
				return true
			}
		}
	}
	return false
}
