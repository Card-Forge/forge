// SpellAbility valid strings: ValidSA$ on a trigger or static ability, the
// vocabulary SpellAbility.isValid and SpellAbilityProperty.hasProperty
// evaluate (SpellAbility.java:2199-2270, SpellAbilityProperty.java). A spec
// is a comma list of alternatives, each a head (Spell, Activated, Triggered,
// SpellAbility, Instant, Sorcery, Ability, with an optional leading "!")
// followed by "."-joined "+"-separated properties.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// saKeywordProperties are the SpellAbility properties that name the keyword
// an ability was built from (Ability.Keyword, set where a keyword expands to
// an ability): each is true for an ability of that keyword.
var saKeywordProperties = map[string][]string{
	"Crew": {"Crew"}, "Saddle": {"Saddle"}, "Station": {"Station"}, "Cycling": {"Cycling", "TypeCycling"},
	"Unearth": {"Unearth"}, "Modular": {"Modular"}, "Equip": {"Equip"}, "Exhaust": {"Exhaust"},
	"Outlast": {"Outlast"}, "PowerUp": {"PowerUp"}, "Boast": {"Boast"}, "Monstrosity": {"Monstrosity"},
}

// saUnresolvedProperties are SpellAbility properties that read state this
// port does not track (how a spell was cast, what mana paid for it): a spec
// naming one is not evaluated.
var saUnresolvedPrefixes = []string{
	"CountersRemovedToPay", "Bargain", "Backup", "Bestow", "Blitz", "Buyback", "Craft", "Dash",
	"Disturb", "Embalm", "Eternalize", "BeamMeUp", "Flashback", "Harmonize", "Jumpstart", "Kicked", "Aftermath",
	"MorphUp", "ManifestUp", "Unlock", "isTurnFaceUp", "isCastFaceDown", "Mayhem", "Mutate",
	"Ninjutsu", "Sneak", "Foretelling", "Foretold", "Plotting", "Modal", "ClassLevelUp", "Daybound",
	"Nightbound", "Warp", "Ward", "CumulativeUpkeep", "SameKeyword", "ChapterNotLore", "EffectSourceAbility",
	"LastChapter", "paidPhyrexianMana", "ManaSpent", "ManaFrom", "MayPlaySource",
	"ManaAbilityCantPaidFor", "NamedSpell", "otherAbility", "CouldCastTiming", "NamedAbility", "withoutXCost",
}

// spellAbilityMatches is StaticAbility/Trigger.matchesValidParam(key, sa):
// whether the spell or ability a matches spec for a trait hosted by host
// (controlled by hostController, reading X from amounts). recognized is false
// when spec names something this port does not evaluate.
func (g *Game) spellAbilityMatches(a *Ability, spec string, host *Card, hostController PlayerID, amounts map[string]expr.Amount) (matched, recognized bool) {
	return g.spellAbilityMatchesAs(a, g.Card(a.Source), spec, host, hostController, amounts)
}

// spellAbilityMatchesAs is spellAbilityMatches with a's own source read as
// source: a last-known copy for an ability recorded earlier (the
// SpellAbility copy on an LKI host MagicStack.addAbilityActivatedThisTurn
// keeps, MagicStack.java:986).
func (g *Game) spellAbilityMatchesAs(a *Ability, source *Card, spec string, host *Card, hostController PlayerID, amounts map[string]expr.Amount) (matched, recognized bool) {
	recognized = true
	for _, alt := range strings.Split(spec, ",") {
		ok, known := g.spellAbilityMatchesOne(a, source, strings.TrimSpace(alt), host, hostController, amounts)
		if !known {
			recognized = false
			continue
		}
		matched = matched || ok
	}
	return matched, recognized
}

func (g *Game) spellAbilityMatchesOne(a *Ability, source *Card, alt string, host *Card, hostController PlayerID, amounts map[string]expr.Amount) (matched, recognized bool) {
	head, props, _ := strings.Cut(alt, ".")
	negated := strings.HasPrefix(head, "!")
	head = strings.TrimPrefix(head, "!")
	kind := a.causeKind()
	var headOK bool
	switch head {
	case "Spell":
		headOK = kind == causeSpell
	case "Ability":
		headOK = kind != causeSpell
	case "Instant":
		headOK = source.Type().Has(cardtype.Instant)
	case "Sorcery":
		headOK = source.Type().Has(cardtype.Sorcery)
	case "Triggered":
		headOK = kind == causeTriggered
	case "Activated":
		headOK = kind == causeActivated
	case "SpellAbility":
		headOK = true
	default:
		// Static and LandAbility heads are not stack objects here.
		return false, head == "Static" || head == "LandAbility"
	}
	if !headOK {
		return negated, true
	}
	if props != "" {
		for _, prop := range strings.Split(props, "+") {
			ok, known := g.spellAbilityHasProperty(a, source, prop, host, hostController, amounts)
			if !known {
				return false, false
			}
			if !ok {
				return negated, true
			}
		}
	}
	return !negated, true
}

// spellAbilityHasProperty is SpellAbility.hasProperty: a leading "!" negates,
// anything this table does not own falls to the host card's own properties
// (SpellAbilityProperty.java's last branch).
func (g *Game) spellAbilityHasProperty(a *Ability, source *Card, prop string, host *Card, hostController PlayerID, amounts map[string]expr.Amount) (matched, recognized bool) {
	if rest, ok := strings.CutPrefix(prop, "!"); ok {
		m, known := g.spellAbilityHasProperty(a, source, rest, host, hostController, amounts)
		return !m, known
	}
	for _, p := range saUnresolvedPrefixes {
		if strings.HasPrefix(prop, p) {
			return false, false
		}
	}
	switch {
	case prop == "ManaAbility":
		return a.Params != nil && a.Params.Record == compile.Activated && strings.EqualFold(a.Params.Name, "Mana"), true
	case prop == "hasTapCost":
		if a.Params == nil {
			return false, true
		}
		text, ok := a.Params.Param("Cost")
		return ok && cost.Parse(text).Tap, true
	case prop == "Loyalty":
		if a.Params == nil {
			return false, true
		}
		_, ok := a.Params.Param("Planeswalker")
		return ok, true
	case prop == "YouCtrl":
		return a.Controller == hostController, true
	case prop == "OppCtrl":
		matched, _ := matchesPlayerSpec(g, a.Controller, hostController, host.ID, "Opponent")
		return matched, true
	case prop == "singleTarget":
		return len(a.Targets) == 1, true
	case prop == "Teamwork":
		// SpellAbilityProperty.java:93-94, SpellAbility.isTeamwork (:658-660):
		// the spell's own Teamwork optional cost was paid. Only a spell records
		// one (spellOptional); a probe reads the cost it is tried with.
		if a.probing {
			return a.probeTeamwork, true
		}
		return a.spell && g.spellOptional(a.Source).has(optTeamwork), true
	case strings.HasPrefix(prop, "numTargets"):
		return g.saTargetCount(a, prop, host, amounts)
	case strings.HasPrefix(prop, "cmc"):
		return g.saCMC(a, prop, host, amounts)
	case strings.HasPrefix(prop, "XCost"):
		return g.saXCost(a, prop, host, amounts)
	case strings.HasPrefix(prop, "IsTargeting"):
		return g.saIsTargeting(a, prop, host, hostController)
	}
	if names, ok := saKeywordProperties[prop]; ok {
		if a.Params == nil {
			return false, true
		}
		have := keyword.Parse(a.Params.Keyword).Name
		for _, n := range names {
			if have == n {
				return true, true
			}
		}
		return false, true
	}
	// A card property of the host card (Self, Creature, nonCreature, Vehicle,
	// ...): Card.hasProperty with the trait's own host as the source.
	return Matches(g, source, valid.Parse("Card."+prop), hostController, host.ID), true
}

// saCMC is the cmc<cmp><amount> property: the mana value of the spell on the
// stack, or of an ability's mana cost, against an amount read from the trait.
func (g *Game) saCMC(a *Ability, prop string, host *Card, amounts map[string]expr.Amount) (matched, recognized bool) {
	if len(prop) < 6 {
		return false, false
	}
	var have int
	if source := g.Card(a.Source); source.Zone == Stack {
		have = source.CMC()
	} else if a.Params != nil {
		text, ok := a.Params.Param("Cost")
		if !ok {
			return false, true
		}
		parsed := cost.Parse(text)
		if len(parsed.Mana) > 0 {
			mc, err := mana.Parse(strings.Join(parsed.Mana, " "))
			if err != nil {
				return false, false
			}
			have = mc.CMC()
		}
	}
	want, ok := resolveNamedAmount(g, amounts, host, prop[5:])
	if !ok {
		return false, false
	}
	return compareOp(have, prop[3:5], want), true
}

// saXCost is the XCost<cmp><amount> property (SpellAbilityProperty.java:33-36):
// the X the spell's mana cost was paid with against an amount, 0 when none was
// announced. A spell already on the stack carries its X on the card.
func (g *Game) saXCost(a *Ability, prop string, host *Card, amounts map[string]expr.Amount) (matched, recognized bool) {
	if len(prop) < 8 {
		return false, false
	}
	have := 0
	switch {
	case a.hasXManaCostPaid:
		have = a.xManaCostPaid
	case g.Card(a.Source).Zone == Stack:
		have = g.Card(a.Source).castX
	}
	want, ok := resolveNamedAmount(g, amounts, host, prop[7:])
	if !ok {
		return false, false
	}
	return compareOp(have, prop[5:7], want), true
}

// saIsTargeting is IsTargeting <defined> (SpellAbilityProperty.java:234-244):
// some object the defined text names is a target of the ability. Only the
// "Valid <spec>" form (battlefield cards, "~" standing for "+") is read; any
// other defined text is not evaluated.
func (g *Game) saIsTargeting(a *Ability, prop string, host *Card, hostController PlayerID) (matched, recognized bool) {
	_, text, ok := strings.Cut(prop, " ")
	spec, isValid := strings.CutPrefix(text, "Valid ")
	if !ok || !isValid || spec == "" {
		return false, false
	}
	parsed := valid.Parse(strings.ReplaceAll(spec, "~", "+"))
	targets := allTargetsOf(*a)
	if a.Target != NoCard {
		targets = append(targets, CardEntity(a.Target))
	}
	for _, t := range targets {
		id, isCard := t.AsCard()
		if !isCard || g.Card(id).Zone != Battlefield {
			continue
		}
		if Matches(g, g.Card(id), parsed, hostController, host.ID) {
			return true, true
		}
	}
	return false, true
}

// saTargetCount is numTargets <cmp><amount>: the distinct targets chosen.
func (g *Game) saTargetCount(a *Ability, prop string, host *Card, amounts map[string]expr.Amount) (matched, recognized bool) {
	_, rest, ok := strings.Cut(prop, " ")
	if !ok || len(rest) < 3 {
		return false, false
	}
	want, ok := resolveNamedAmount(g, amounts, host, rest[2:])
	if !ok {
		return false, false
	}
	seen := map[EntityID]bool{}
	for _, t := range a.Targets {
		seen[t] = true
	}
	return compareOp(len(seen), rest[:2], want), true
}
