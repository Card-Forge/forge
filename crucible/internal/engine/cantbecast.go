// Mode$ CantBeCast (StaticAbilityCantBeCast.cantBeCastAbility): a static that
// stops a player casting a card, for every card and caster its ValidCard$ and
// Caster$ name.

//enginelint:allow id zone card game valid player continuous staticability zonemove

package engine

import (
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// cantBeCastParams are the params a CantBeCast line may carry that this port
// evaluates; any other (cmcGT$, CheckSVar$, a ValidCard$-filtered
// NumLimitEachTurn$, ...) makes the line unresolvable, and it is not applied
// (GO-7). The cosmetic ones (Description$, Secondary$, ...) change nothing.
var cantBeCastParams = map[string]bool{
	"mode": true, "validcard": true, "caster": true, "onlysorceryspeed": true, "origin": true,
	"numlimiteachturn": true, "effectzone": true, "condition": true, "phases": true, "playerturn": true,
	"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
}

// cantBeCast reports whether some Mode$ CantBeCast static stops pid casting
// card from the zone it is in now. Hosts are the battlefield and Command-zone
// statics plus the card's own (EffectZone$ All lines on the card itself).
// Params (applyCantBeCastAbility): ValidCard$, Caster$, OnlySorcerySpeed$ (the
// line applies only when the caster could not cast a sorcery), Origin$ (the
// zones it was cast from), and NumLimitEachTurn$ (applies once the caster has
// cast that many spells this turn; only resolved for a ValidCard$ of "Card" or
// none, since this port counts spells cast, not which).
func (g *Game) cantBeCast(pid PlayerID, card CardID) bool {
	c := g.Card(card)
	for _, host := range g.staticHostsWith(card) {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		for _, face := range h.traitFaces() {
			for _, s := range face.Statics {
				if strings.EqualFold(s.Name, "CantBeCast") && g.cantBeCastApplies(pid, c, h, s) {
					return true
				}
			}
		}
	}
	return false
}

func (g *Game) cantBeCastApplies(pid PlayerID, c, host *Card, s *compile.Ability) bool {
	for _, p := range s.Params {
		if !cantBeCastParams[strings.ToLower(p.Key)] {
			return false
		}
	}
	if !g.staticConditionsMet(host, s) {
		return false
	}
	validCard, hasValid := s.Param("ValidCard")
	if hasValid && !Matches(g, c, valid.Parse(validCard), host.Controller(), host.ID) {
		return false
	}
	if caster, ok := s.Param("Caster"); ok {
		matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, caster)
		if !recognized || !matched {
			return false
		}
	}
	if _, ok := s.Param("OnlySorcerySpeed"); ok && g.canActSorcerySpeed(pid) {
		return false
	}
	if origin, ok := s.Param("Origin"); ok {
		zones, err := parseZoneList(origin)
		if err != nil || !slices.Contains(zones, c.Zone) {
			return false
		}
	}
	if raw, ok := s.Param("NumLimitEachTurn"); ok {
		limit, err := strconv.Atoi(raw)
		if err != nil || (hasValid && validCard != "Card") || g.Player(pid).SpellsCastThisTurn < limit {
			return false
		}
	}
	return true
}

// staticHostsWith is every card whose static abilities apply to card: the
// ones in a static-ability source zone (traitHosts) and card itself, where its
// own EffectZone$ All lines live (StaticAbilityCantBeCast's own `allp.add(card)`).
func (g *Game) staticHostsWith(card CardID) []CardID {
	hosts := []CardID{card}
	for _, p := range g.Players() {
		for _, h := range g.traitHosts(p) {
			if h != card {
				hosts = append(hosts, h)
			}
		}
	}
	return hosts
}

// cantBeActivatedParams and cantPlayLandParams are the params those lines may
// carry that this port evaluates, as cantBeCastParams is for CantBeCast.
var (
	cantBeActivatedParams = map[string]bool{
		"mode": true, "validcard": true, "validsa": true, "affectedzone": true, "activator": true,
		"condition": true, "phases": true, "playerturn": true, "effectzone": true,
		"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
	}
	cantPlayLandParams = map[string]bool{
		"mode": true, "validcard": true, "player": true, "origin": true,
		"condition": true, "phases": true, "playerturn": true, "effectzone": true,
		"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
	}
)

// cantBeActivated reports whether some Mode$ CantBeActivated static stops pid
// activating ability, one of card's (StaticAbilityCantBeCast
// .cantBeActivatedAbility/applyCantBeActivatedAbility): ValidCard$ on the
// source card, ValidSA$ on the ability (validActivatedSA), AffectedZone$ the
// card's current zone and Activator$ the player. isMana and isLoyalty say
// which kind of activated ability it is. Hosts are the static-ability source
// zones only, as Java's loop has no `allp.add(card)` here.
func (g *Game) cantBeActivated(pid PlayerID, card *Card, isMana, isLoyalty bool) bool {
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if strings.EqualFold(s.Name, "CantBeActivated") && g.cantBeActivatedApplies(pid, card, h, s, isMana, isLoyalty) {
						return true
					}
				}
			}
		}
	}
	return false
}

func (g *Game) cantBeActivatedApplies(pid PlayerID, c, host *Card, s *compile.Ability, isMana, isLoyalty bool) bool {
	if !paramsResolvable(s, cantBeActivatedParams) || !g.staticConditionsMet(host, s) {
		return false
	}
	if validCard, ok := s.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(validCard), host.Controller(), host.ID) {
		return false
	}
	if sa, ok := s.Param("ValidSA"); ok {
		if matched, recognized := validActivatedSA(sa, isMana, isLoyalty); !recognized || !matched {
			return false
		}
	}
	if zone, ok := s.Param("AffectedZone"); ok {
		z, ok := ZoneByName(zone)
		if !ok || c.Zone != z {
			return false
		}
	}
	if activator, ok := s.Param("Activator"); ok {
		matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, activator)
		if !recognized || !matched {
			return false
		}
	}
	return true
}

// validActivatedSA evaluates a ValidSA$ spec against an activated ability: the
// base "Activated" with "+"-joined properties ManaAbility and Loyalty, each
// optionally negated (SpellAbilityProperty). recognized is false for any
// other base or property (Cycling, Equip, ...), which the caller treats as the
// line not applying.
func validActivatedSA(spec string, isMana, isLoyalty bool) (matched, recognized bool) {
	base, props, _ := strings.Cut(spec, ".")
	if base != "Activated" {
		return false, false
	}
	if props == "" {
		return true, true
	}
	matched = true
	for _, prop := range strings.Split(props, "+") {
		want := !strings.HasPrefix(prop, "!")
		switch strings.TrimPrefix(prop, "!") {
		case "ManaAbility":
			matched = matched && isMana == want
		case "Loyalty":
			matched = matched && isLoyalty == want
		default:
			return false, false
		}
	}
	return matched, true
}

// cantPlayLand reports whether some Mode$ CantPlayLand static stops pid
// playing land card (StaticAbilityCantBeCast.cantPlayLandAbility/
// applyCantPlayLandAbility): ValidCard$ on the land, Origin$ the zone it is
// played from, Player$ the player. A Player$ with no ValidCard$ names every
// land, as Java's absent-param match does. Hosts are the static-ability source
// zones only: unlike CantBeCast, Java's loop does not add the land itself.
func (g *Game) cantPlayLand(pid PlayerID, card CardID) bool {
	c := g.Card(card)
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if strings.EqualFold(s.Name, "CantPlayLand") && g.cantPlayLandApplies(pid, c, h, s) {
						return true
					}
				}
			}
		}
	}
	return false
}

func (g *Game) cantPlayLandApplies(pid PlayerID, c, host *Card, s *compile.Ability) bool {
	if !paramsResolvable(s, cantPlayLandParams) || !g.staticConditionsMet(host, s) {
		return false
	}
	if validCard, ok := s.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(validCard), host.Controller(), host.ID) {
		return false
	}
	if origin, ok := s.Param("Origin"); ok {
		zones, err := parseZoneList(origin)
		if err != nil || !slices.Contains(zones, c.Zone) {
			return false
		}
	}
	if player, ok := s.Param("Player"); ok {
		matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, player)
		if !recognized || !matched {
			return false
		}
	}
	return true
}

// damageNotRemoved is StaticAbilityNoCleanupDamage.damageNotRemoved: some
// Mode$ NoCleanupDamage static names card (ValidCard$, every card when absent),
// so the cleanup step leaves its damage on it. The card's own statics count
// (staticHostsWith). A line carrying a param other than ValidCard$ and the
// cosmetic ones is not applied (GO-7).
func (g *Game) damageNotRemoved(card CardID) bool {
	c := g.Card(card)
	for _, host := range g.staticHostsWith(card) {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		for _, face := range h.traitFaces() {
			for _, s := range face.Statics {
				if !strings.EqualFold(s.Name, "NoCleanupDamage") || !g.staticConditionsMet(h, s) {
					continue
				}
				if !onlyStaticParams(s, "mode", "validcard", "description", "secondary", "effectzone", "condition") {
					continue
				}
				if v, ok := s.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(v), h.Controller(), h.ID) {
					continue
				}
				return true
			}
		}
	}
	return false
}

// onlyStaticParams reports whether every param of s is one of keys (folded).
func onlyStaticParams(s *compile.Ability, keys ...string) bool {
	for _, p := range s.Params {
		if !slices.Contains(keys, strings.ToLower(p.Key)) {
			return false
		}
	}
	return true
}
