package engine

//enginelint:allow id card game ability defined amount control valid discardeffect effecthelpers zone

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// The resolve-time card choice several effects share: Choices$ (PutCounter,
// RemoveCounter, GainControl, MustBlock) or CardChoices$ (Tap) names the
// cards a Chooser$ player picks from as the effect resolves, in place of
// Defined$ and the targets (CountersPutEffect.java:190-244,
// CountersRemoveEffect.java:107-137, TapEffect.java:37-47,
// ControlGainEffect.java:106-120, MustBlockEffect.java:40-60).

// choiceChooser is Chooser$ (the first player it names; Java's get(0) throws
// on an empty list, here that is an error), else the activator.
func choiceChooser(g *Game, a *Ability, api string) (PlayerID, error) {
	spec, ok := a.Params.Param("Chooser")
	if !ok {
		return a.Controller, nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return NoPlayer, fmt.Errorf("engine: %s: Chooser$: %w", api, err)
	}
	if len(players) == 0 {
		return NoPlayer, fmt.Errorf("engine: %s: Chooser$ %q names no player", api, spec)
	}
	return players[0], nil
}

// choiceZones is ChoiceZone$ (ZoneType.smartValueOf, Battlefield when absent).
func choiceZones(a *Ability, api string) ([]ZoneType, error) {
	raw, ok := a.Params.Param("ChoiceZone")
	if !ok {
		return []ZoneType{Battlefield}, nil
	}
	var zones []ZoneType
	for _, name := range strings.Split(raw, ",") {
		z, ok := ZoneByName(strings.TrimSpace(name))
		if !ok {
			return nil, fmt.Errorf("engine: %s: ChoiceZone$ %q not resolvable yet", api, name)
		}
		zones = append(zones, z)
	}
	return zones, nil
}

// choiceCandidates is every card of zones, in seating order, that the valid
// string spec matches with the activator as "You" and the host as the source.
// A spec Matches would answer wrongly (a property it does not read) is an
// error rather than an empty offer (GO-7).
func choiceCandidates(g *Game, a *Ability, api string, zones []ZoneType, spec string) ([]CardID, error) {
	if err := discardValidResolvable(g, spec, a.Controller, a.Source); err != nil {
		return nil, fmt.Errorf("engine: %s: Choices$ %q: %w", api, spec, err)
	}
	for _, alt := range valid.Parse(spec).Alternatives {
		for _, p := range alt.Properties {
			if strings.HasPrefix(p.Name, "targetedBy") {
				return nil, fmt.Errorf("engine: %s: Choices$ property %q not resolvable yet", api, p.Name)
			}
		}
	}
	parsed := valid.Parse(spec)
	var out []CardID
	for _, z := range zones {
		for _, pid := range g.Players() {
			for _, cid := range g.Zone(z, pid).Cards() {
				if Matches(g, g.Card(cid), parsed, a.Controller, a.Source) {
					out = append(out, cid)
				}
			}
		}
	}
	return out, nil
}

// askChoiceCards puts the offer to chooser through controller and checks the
// answer. The bounds are clamped to the offer, which an empty offer reduces
// to no question at all.
func askChoiceCards(g *Game, controller PlayerController, a *Ability, api string, chooser PlayerID, offer []CardID, lo, hi int) ([]CardID, error) {
	hi = min(hi, len(offer))
	lo = max(min(lo, hi), 0)
	if hi <= 0 {
		return nil, nil
	}
	picked := controller.ChooseCardsForEffect(g, chooser, a.Source, offer, lo, hi)
	if err := checkChoice(picked, offer, lo, hi); err != nil {
		return nil, fmt.Errorf("engine: %s: %w", api, err)
	}
	return picked, nil
}
