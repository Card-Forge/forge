// What Layers 4, 5 and 6 (CR 613.1d-f) share for a Mode$ Continuous line:
// whether the static ability is on at all (StaticAbility.checkConditions),
// which cards it reaches (StaticAbilityContinuous.getAffectedCards), and the
// runtime values its AddType$/AddColor$/AddKeyword$ tokens stand for
// (StaticAbilityContinuous.applyContinuousAbility's TYPE, COLOR and
// ABILITIES branches). continuous.go's applyOneContinuousType/Color/Keyword
// are the only callers; the names carry a "layer" prefix so a sibling helper
// written for another layer does not collide with them.
//
// Ported from
// forge-game/src/main/java/forge/game/staticability/StaticAbility.java
// (checkConditions, zonesCheck) and StaticAbilityContinuous.java.

package engine

//enginelint:allow id zone card game player valid parts ability amount trigger continuous animate

import (
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// layerStaticApplies is StaticAbility.checkConditions for one Mode$
// Continuous line hosted by host: the host is in a zone the line functions
// from (layerHostZoneActive), its Condition$ holds (continuousConditionMet,
// continuous.go), its IsPresent$ count compares true (isPresentMatches,
// trigger.go -- the identical CardTraitBase block triggers already use), its
// TopCardOfLibraryIs$ matches the controller's top library card, and its
// CheckSVar$ chain compares true (layerCheckSVars).
//
// Phases$, PlayerTurn$, GameStage$ and ClassLevel$ report false: each needs a
// phase-range parser, a defined-player turn check, the game's own stage or a
// Class level this port does not read for statics yet (1, 0, 1 and 0 real
// Mode$ Continuous lines). False means the line does nothing this pass -- a
// coverage gap rather than an effect applied when it should not be (GO-7).
func layerStaticApplies(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) bool {
	if _, cda := s.Param("CharacteristicDefining"); !cda && !layerHostZoneActive(host, s) {
		return false
	}
	if !continuousConditionMet(g, host, s) {
		return false
	}
	for _, key := range [...]string{"Phases", "PlayerTurn", "GameStage", "ClassLevel"} {
		if _, ok := s.Param(key); ok {
			return false
		}
	}
	if spec, ok := s.Param("TopCardOfLibraryIs"); ok {
		lib := g.Zone(Library, host.Controller()).Cards()
		if len(lib) == 0 || !Matches(g, g.Card(lib[0]), valid.Parse(spec), host.Controller(), host.ID) {
			return false
		}
	}
	if !isPresentMatches(g, host, amounts, s, "IsPresent", "PresentCompare", "PresentDefined", "PresentZone", "PresentPlayer") {
		return false
	}
	return layerCheckSVars(g, host, amounts, s)
}

// layerHostZoneActive is StaticAbility.zonesCheck without its
// CharacteristicDefining short-circuit (layerStaticApplies takes that): an
// effect card's traits are active in the Command zone alone
// (EffectEffect.java's setActiveZone, Card.IsEffect's own doc comment);
// otherwise EffectZone$ names the zones the line functions from ("All" and a
// comma list, layerZoneList) and its absence means the battlefield. A
// battlefield card whose line reads EffectZone$ Graveyard is therefore off
// while it is in play.
//
// This function itself has no zone bias, but every caller only ever visits
// hosts g.traitHosts (game.go) returns -- battlefield cards and Command-zone
// effect cards -- so an EffectZone$ naming Graveyard/Hand/Stack/Library/Exile
// alone never has a host to test true against: that line stays a coverage
// gap (GO-7's "does nothing" case), not resolved, until traitHosts walks
// those zones too.
func layerHostZoneActive(host *Card, s *compile.Ability) bool {
	if host.IsEffect {
		return host.Zone == Command
	}
	v, ok := s.Param("EffectZone")
	if !ok {
		return host.Zone == Battlefield
	}
	zones, ok := layerZoneList(v)
	return ok && slices.Contains(zones, host.Zone)
}

// layerZoneList is ZoneType.listValueOf: "All" is Java's own fixed seven
// zones, in its order, and anything else a comma-or-space separated list of
// zone names. A name ZoneByName does not know reports false for the whole
// list -- Java drops it silently, but no real Mode$ Continuous line writes
// one, so refusing is the safer reading of a typo (GO-7).
func layerZoneList(v string) ([]ZoneType, bool) {
	if v == "All" {
		return []ZoneType{Battlefield, Hand, Graveyard, Exile, Stack, Library, Command}, true
	}
	var zones []ZoneType
	for _, name := range strings.FieldsFunc(v, func(r rune) bool { return r == ',' || r == ' ' }) {
		z, ok := ZoneByName(name)
		if !ok {
			return nil, false
		}
		zones = append(zones, z)
	}
	return zones, len(zones) > 0
}

// layerSVarChecks are StaticAbility.checkConditions' four CheckSVar blocks,
// in the order Java evaluates them.
var layerSVarChecks = [...][2]string{
	{"CheckSVar", "SVarCompare"},
	{"CheckSecondSVar", "SecondSVarCompare"},
	{"CheckThirdSVar", "ThirdSVarCompare"},
	{"CheckFourthSVar", "FourthSVarCompare"},
}

// layerCheckSVars is checkConditions' CheckSVar$ chain: each value compared
// against its *Compare$ (default GE1), both sides through resolveNamedAmount
// (amount.go). Java stops at the first absent key -- a CheckSecondSVar$ with
// no CheckSVar$ is never read (StaticAbility.java:470-472) -- and that quirk
// is kept (PORT-7). An operand resolveNamedAmount cannot compute reports
// false: the line does nothing rather than apply on a guess (GO-7).
func layerCheckSVars(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) bool {
	for _, keys := range layerSVarChecks {
		check, ok := s.Param(keys[0])
		if !ok {
			return true
		}
		left, ok := resolveNamedAmount(g, amounts, host, check)
		if !ok {
			return false
		}
		compare, ok := s.Param(keys[1])
		if !ok {
			compare = "GE1"
		}
		if len(compare) < 3 {
			return false
		}
		right, ok := resolveNamedAmount(g, amounts, host, compare[2:])
		if !ok || !compareOp(left, compare[:2], right) {
			return false
		}
	}
	return true
}

// layerAffectedCards is StaticAbilityContinuous.getAffectedCards: the cards
// s changes. A CharacteristicDefining$ line (Java tests the key's presence,
// not its value) affects host alone, or nothing while host is in one of its
// ExcludeZone$ zones. Otherwise the candidates are AffectedDefined$'s cards
// (layerDefinedCards, phased-in only), else every card in AffectedZone$'s
// zones, else the whole battlefield -- then narrowed by Affected$ when the
// line has one. A line with neither Affected$ nor AffectedDefined$ reaches
// every permanent, as Java's does. false means the line names a defined set
// or zone this port cannot resolve.
func layerAffectedCards(g *Game, host *Card, s *compile.Ability) ([]CardID, bool) {
	if _, cda := s.Param("CharacteristicDefining"); cda {
		if v, ok := s.Param("ExcludeZone"); ok {
			zones, ok := layerZoneList(v)
			if !ok {
				return nil, false
			}
			if slices.Contains(zones, host.Zone) {
				return nil, true
			}
		}
		return []CardID{host.ID}, true
	}

	var candidates []CardID
	if defined, ok := s.Param("AffectedDefined"); ok {
		ids, ok := layerDefinedCards(host, defined)
		if !ok {
			return nil, false
		}
		for _, id := range ids {
			if !g.Card(id).IsPhasedOut() {
				candidates = append(candidates, id)
			}
		}
	} else if v, ok := s.Param("AffectedZone"); ok {
		zones, ok := layerZoneList(v)
		if !ok {
			return nil, false
		}
		for _, z := range zones {
			for _, pid := range g.Players() {
				candidates = append(candidates, g.Zone(z, pid).Cards()...)
			}
			if z == Stack {
				// The shared stack zone game.go keeps beside each player's
				// own; a cast spell sits in its caster's (castspell.go).
				candidates = append(candidates, g.Zone(Stack, NoPlayer).Cards()...)
			}
		}
	} else {
		for _, pid := range g.Players() {
			candidates = append(candidates, g.Zone(Battlefield, pid).Cards()...)
		}
	}

	affected, ok := s.Param("Affected")
	if !ok {
		return candidates, true
	}
	spec := valid.Parse(affected)
	kept := candidates[:0]
	for _, id := range candidates {
		if Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
			kept = append(kept, id)
		}
	}
	return kept, true
}

// layerDefinedCards is AbilityUtils.getDefinedCards for the four
// AffectedDefined$ values the corpus's Mode$ Continuous lines write: Self,
// and Enchanted/Equipped/"AttachedBy Self" -- all three
// Card.getAttachedTo() of host in Java (getEnchantingCard and getEquipping
// both return it), so an unattached Aura or Equipment affects nothing.
func layerDefinedCards(host *Card, defined string) ([]CardID, bool) {
	switch defined {
	case "Self":
		return []CardID{host.ID}, true
	case "Enchanted", "Equipped", "AttachedBy Self":
		if id, ok := host.AttachedTo(); ok {
			return []CardID{id}, true
		}
		return nil, true
	}
	return nil, false
}

// forEachOffBattlefieldCard calls fn on every card not on the battlefield,
// in arena order. An AffectedZone$ line (Hand, Graveyard, Stack, Library,
// All) writes Layer 4/5/6 changes onto such cards, and Move clears them only
// on leaving the battlefield, so each applier clears these as well as the
// battlefield's before it rebuilds. Phased-out permanents are on the
// battlefield and are not visited: their Pump and Animate records re-apply
// the moment they phase back in (applyPumpEffects' own doc comment).
func forEachOffBattlefieldCard(g *Game, fn func(*Card)) {
	for i := 1; i < len(g.cards); i++ {
		if c := &g.cards[i]; c.Zone != Battlefield {
			fn(c)
		}
	}
}

// layerTypeChange is StaticAbilityContinuous's TYPE branch
// (StaticAbilityContinuous.java:370-449) for one line, as a TypeEffect.
//
// AddType$ is split on " & ", then Java's removeIf: an unchosen ChosenType or
// ChosenType2 token is dropped; ImprintedCreatureType becomes the creature
// types of host's last imprinted card; AllBasicLandType and
// AllNonBasicLandType become the registry's [BasicTypes] and [LandTypes]
// sections (CardType.getBasicTypes/getNonBasicTypes). Expansions go after
// the surviving tokens, and every token then has ChosenType2, then
// ChosenType, replaced by host's own choices. RemoveType$ drops an unchosen
// ChosenType token and substitutes nothing -- Java's own asymmetry, kept.
//
// The Remove*Types$ flags apply only when AddType$ is absent or kept at
// least one token (StaticAbilityContinuous.java:425-426, "overwrite doesn't
// work without new value"). The category flags (RemoveLandTypes$,
// RemoveCreatureTypes$, ...) and the three expansions read the subtype
// vocabulary off the game's DB (compile.DB.Types) -- Animate's own seam,
// subtypeCategoryDrop -- and report false without one.
//
// false too for AddAllCreatureTypes$ (8 real lines): Java's CardType keeps
// "every creature type" as a flag, and cardtype.Line has no such flag yet.
// Materialising it as a few hundred subtypes on every Type() fold is the
// alternative, and a cost this port does not pay for eight lines.
func layerTypeChange(g *Game, host *Card, s *compile.Ability) (TypeEffect, bool) {
	if _, ok := s.Param("AddAllCreatureTypes"); ok {
		return TypeEffect{}, false
	}
	reg := g.db.Types()
	e := TypeEffect{Timestamp: host.Timestamp}

	add, hasAdd := s.Param("AddType")
	var addWords []string
	if hasAdd {
		chosen, chosen2 := host.Memory.ChosenType(false), host.Memory.ChosenType(true)
		var extra []string
		for _, word := range strings.Split(add, " & ") {
			switch word {
			case "ChosenType":
				if chosen == "" {
					continue
				}
			case "ChosenType2":
				if chosen2 == "" {
					continue
				}
			case "ImprintedCreatureType", "AllBasicLandType", "AllNonBasicLandType":
				if reg == nil {
					return TypeEffect{}, false
				}
				extra = append(extra, layerExpandTypeToken(g, host, word)...)
				continue
			}
			addWords = append(addWords, word)
		}
		addWords = append(addWords, extra...)
		for i, word := range addWords {
			if chosen2 != "" {
				word = strings.ReplaceAll(word, "ChosenType2", chosen2)
			}
			if chosen != "" {
				word = strings.ReplaceAll(word, "ChosenType", chosen)
			}
			e.AddTypes = e.AddTypes.Union(cardtype.ParseToken(word))
			addWords[i] = word
		}
	}

	removeCount := 0
	if remove, ok := s.Param("RemoveType"); ok {
		for _, word := range strings.Split(remove, " & ") {
			if word == "ChosenType" && host.Memory.ChosenType(false) == "" {
				continue
			}
			e.RemoveTypes = e.RemoveTypes.Union(cardtype.ParseToken(word))
			removeCount++
		}
	}

	flags := false
	if !hasAdd || len(addWords) > 0 {
		e.RemoveCardTypes = hasParamOn(s, "RemoveCardTypes")
		e.RemoveSuperTypes = hasParamOn(s, "RemoveSuperTypes")
		e.RemoveSubTypes = hasParamOn(s, "RemoveSubTypes")
		land, creature := hasParamOn(s, "RemoveLandTypes"), hasParamOn(s, "RemoveCreatureTypes")
		artifact, enchantment := hasParamOn(s, "RemoveArtifactTypes"), hasParamOn(s, "RemoveEnchantmentTypes")
		drop, ok := subtypeCategoryDrop(g, land, creature, artifact, enchantment)
		if !ok {
			return TypeEffect{}, false
		}
		e.DropSubtype = drop
		flags = e.RemoveCardTypes || e.RemoveSuperTypes || e.RemoveSubTypes || drop != nil
	}
	if len(addWords) == 0 && removeCount == 0 && !flags {
		return TypeEffect{}, false
	}
	return e, true
}

// layerExpandTypeToken is layerTypeChange's three expanding AddType$ tokens.
// A token with nothing to expand to (no imprinted card) contributes nothing,
// as Java's removeIf does.
func layerExpandTypeToken(g *Game, host *Card, word string) []string {
	reg := g.db.Types()
	switch word {
	case "ImprintedCreatureType":
		imprinted := host.Memory.Imprinted()
		if len(imprinted) == 0 {
			return nil
		}
		return g.Card(imprinted[len(imprinted)-1]).Type().CreatureTypes(reg)
	case "AllBasicLandType":
		return reg.Members(cardtype.CategoryBasic)
	default: // "AllNonBasicLandType"
		return reg.Members(cardtype.CategoryLand)
	}
}

// hasParamOn reports whether s carries key at all, whatever its value --
// Java's params.containsKey, which is how every Remove*Types$ flag is read.
func hasParamOn(s *compile.Ability, key string) bool {
	_, ok := s.Param(key)
	return ok
}

// layerColorChange is StaticAbilityContinuous's COLOR branch
// (StaticAbilityContinuous.java:451-460) with getColorsFromParam
// (:933-946): SetColor$ wins over AddColor$ and overwrites; the whole value
// "ChosenColor" is host's own chosen colors, and nothing at all -- not an
// overwrite to colorless -- while none is chosen; "All" is WUBRG; anything
// else a " & " list of color words, "Colorless" contributing none. A word
// colorFromName does not know reports false rather than Java's silent
// colorless (no real line writes one).
func layerColorChange(host *Card, s *compile.Ability) (ColorEffect, bool) {
	key, overwrite := "AddColor", false
	if _, ok := s.Param("SetColor"); ok {
		key, overwrite = "SetColor", true
	}
	v, ok := s.Param(key)
	if !ok {
		return ColorEffect{}, false
	}
	e := ColorEffect{Timestamp: host.Timestamp, Overwrite: overwrite}
	switch v {
	case "ChosenColor":
		e.Colors = host.Memory.ChosenColors()
		if e.Colors == 0 {
			return ColorEffect{}, false
		}
	case "All":
		e.Colors = mana.AllColors
	default:
		for _, word := range strings.Split(v, " & ") {
			if word == "Colorless" {
				continue
			}
			c, ok := colorFromName(word)
			if !ok {
				return ColorEffect{}, false
			}
			e.Colors |= c
		}
	}
	return e, true
}

// layerColorNames are MagicColor's long color names in WUBRG order, the
// order a ColorSet iterates in; capitalised, they are colorFromName's words.
var layerColorNames = [...]struct {
	color mana.Colors
	name  string
}{
	{mana.White, "white"}, {mana.Blue, "blue"}, {mana.Black, "black"}, {mana.Red, "red"}, {mana.Green, "green"},
}

// layerBasicLandTypes is MagicColor.Constant.BASIC_LANDS, in its order.
var layerBasicLandTypes = [...]string{"Plains", "Island", "Swamp", "Mountain", "Forest"}

// layerKeywords is one line's resolved ABILITIES-branch keyword change,
// before the per-affected-card pass (layerKeywordsFor).
type layerKeywords struct {
	add        []string
	remove     []string
	removeAll  bool
	multiplier int
	// perCard marks an add token naming the affected card itself
	// (CardColors, ConvertedManaCost): layerKeywordsFor rewrites those per
	// card, and shares add unchanged otherwise.
	perCard bool
}

// layerKeywordChange is StaticAbilityContinuous's ABILITIES branch, keyword
// half (StaticAbilityContinuous.java:167-331), for one line.
//
// AddKeyword$ is split on " & ", then Java's removeIf in its order: a token
// naming an unchosen ChosenColor/ChosenType/ChosenNumber/ChosenPlayer/
// ChosenName/ChosenEvenOdd is dropped; AllColors expands to one keyword per
// color; CommanderColorID is dropped (this port has no commanders, which is
// Java's own drop for a non-Commander game); ColorsYouCtrl expands per color
// among the controller's permanents; YourBasic per basic land type among the
// controller's lands; EachCMCAmongDefined per distinct mana value among the
// KeywordDefined$ permanents. Expansions go after the surviving tokens, and
// every token then has host's choices, HostCardUID/HostCardControllerUID
// and CalcKeywordN$ substituted (layerSubstituteKeyword).
//
// RemoveKeyword$ removes by prefix and RemoveAllAbilities$/
// RemoveNonManaAbilities$ remove every keyword (Java's removeAbilities !=
// null, passed to addChangedCardKeywords as removeAll) -- the keyword half
// of "loses all abilities" only: this port has no way yet to switch off the
// affected card's own triggers, activated abilities or statics.
//
// false when the line has no keyword change, or one this cannot resolve:
// SharedKeywordsZone$ (CardFactoryUtil.sharedKeywords), FromDraftNotes$,
// ShareRememberedKeywords$, CantHaveKeyword$ (a grant blocker a later
// timestamp's AddKeyword$ must respect), CardManaCost in a token (needs
// ManaCost.getShortString, not ported), more than one chosen color behind a
// ChosenColor token (Java takes the first chosen, an order mana.Colors does
// not keep), or a CalcKeywordN$ resolveNamedAmount cannot compute.
func layerKeywordChange(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) (layerKeywords, bool) {
	for _, key := range [...]string{"SharedKeywordsZone", "FromDraftNotes", "ShareRememberedKeywords", "CantHaveKeyword"} {
		if _, ok := s.Param(key); ok {
			return layerKeywords{}, false
		}
	}
	k := layerKeywords{multiplier: 1}
	if v, ok := s.Param("RemoveKeyword"); ok {
		k.remove = strings.Split(v, " & ")
	}
	k.removeAll = hasParamOn(s, "RemoveAllAbilities") || hasParamOn(s, "RemoveNonManaAbilities")
	if v, ok := s.Param("KeywordMultiplier"); ok {
		n, err := strconv.Atoi(v)
		if err != nil || n < 0 {
			return layerKeywords{}, false
		}
		k.multiplier = n
	}

	if v, ok := s.Param("AddKeyword"); ok {
		var extra []string
		for _, tok := range strings.Split(v, " & ") {
			keep, more, ok := layerExpandKeyword(g, host, s, tok)
			if !ok {
				return layerKeywords{}, false
			}
			extra = append(extra, more...)
			if keep {
				k.add = append(k.add, tok)
			}
		}
		k.add = append(k.add, extra...)
		for i, tok := range k.add {
			out, ok := layerSubstituteKeyword(g, host, amounts, s, tok)
			if !ok {
				return layerKeywords{}, false
			}
			k.add[i] = out
			if strings.Contains(out, "CardManaCost") {
				return layerKeywords{}, false
			}
			if strings.Contains(out, "CardColors") || strings.Contains(out, "cardColors") ||
				strings.Contains(out, "ConvertedManaCost") {
				k.perCard = true
			}
		}
	}
	if len(k.add) == 0 && k.remove == nil && !k.removeAll {
		return layerKeywords{}, false
	}
	return k, true
}

// layerExpandKeyword is one token's pass through Java's removeIf: keep is
// false when the token is dropped, more is what it expands to.
func layerExpandKeyword(g *Game, host *Card, s *compile.Ability, tok string) (keep bool, more []string, ok bool) {
	m := &host.Memory
	_, hasNumber := m.ChosenNumber()
	switch {
	case m.ChosenColors() == 0 && strings.Contains(tok, "ChosenColor"),
		m.ChosenType(false) == "" && strings.Contains(tok, "ChosenType"),
		!hasNumber && strings.Contains(tok, "ChosenNumber"),
		m.ChosenPlayer() == NoPlayer && strings.Contains(tok, "ChosenPlayer"),
		len(m.NamedCards()) == 0 && strings.Contains(tok, "ChosenName"),
		m.ChosenEvenOdd() == "" && (strings.Contains(tok, "ChosenEvenOdd") || strings.Contains(tok, "chosenEvenOdd")):
		return false, nil, true
	case strings.Contains(tok, "AllColors") || strings.Contains(tok, "allColors"):
		return false, layerKeywordPerColor(tok, "AllColors", "allColors", mana.AllColors), true
	case strings.Contains(tok, "CommanderColorID"):
		return false, nil, true
	case strings.Contains(tok, "ColorsYouCtrl") || strings.Contains(tok, "colorsYouCtrl"):
		var colors mana.Colors
		for _, id := range g.Zone(Battlefield, host.Controller()).Cards() {
			colors |= g.Card(id).Colors()
		}
		return false, layerKeywordPerColor(tok, "ColorsYouCtrl", "colorsYouCtrl", colors), true
	case strings.Contains(tok, "YourBasic"):
		for _, basic := range layerBasicLandTypes {
			for _, id := range g.Zone(Battlefield, host.Controller()).Cards() {
				if t := g.Card(id).Type(); t.Has(cardtype.Land) && t.HasSubtype(basic) {
					more = append(more, strings.ReplaceAll(tok, "YourBasic", basic))
					break
				}
			}
		}
		return false, more, true
	case strings.Contains(tok, "EachCMCAmongDefined"):
		defined, ok := s.Param("KeywordDefined")
		if !ok {
			return false, nil, false
		}
		spec := valid.Parse(defined)
		for _, pid := range g.Players() {
			for _, id := range g.Zone(Battlefield, pid).Cards() {
				if !Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
					continue
				}
				cmc := strconv.Itoa(g.Card(id).CMC())
				y := strings.Replace(tok, " from EachCMCAmongDefined",
					":Card.cmcEQ"+cmc+":Protection from mana value "+cmc, 1)
				if !slices.Contains(more, y) {
					more = append(more, y)
				}
			}
		}
		return false, more, true
	}
	return true, nil, true
}

// layerKeywordPerColor is the AllColors/ColorsYouCtrl/CardColors expansion:
// one copy of tok per color in colors, WUBRG order, upper replaced by the
// capitalised color name and lower by the lower-case one.
func layerKeywordPerColor(tok, upper, lower string, colors mana.Colors) []string {
	var out []string
	for _, c := range layerColorNames {
		if colors&c.color == 0 {
			continue
		}
		y := strings.ReplaceAll(tok, upper, strings.ToUpper(c.name[:1])+c.name[1:])
		out = append(out, strings.ReplaceAll(y, lower, c.name))
	}
	return out
}

// layerSubstituteKeyword is the map() pass after removeIf
// (StaticAbilityContinuous.java:261-291): host's chosen color, type,
// number, player, name and even/odd, then HostCardUID/HostCardControllerUID
// and CalcKeywordN$. The UIDs become this port's own CardID/PlayerID
// numbers: Java writes its own card and player ids there, the only identity
// either engine has, and no valid-string property reads them back yet
// (CardUID_/PlayerUID_ are unported), so the text only has to be stable.
func layerSubstituteKeyword(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability, tok string) (string, bool) {
	m := &host.Memory
	if c := m.ChosenColors(); c != 0 && (strings.Contains(tok, "ChosenColor") || strings.Contains(tok, "chosenColor")) {
		if c.Count() != 1 {
			return "", false
		}
		named := layerKeywordPerColor(tok, "ChosenColor", "chosenColor", c)
		tok = named[0]
	}
	if t := m.ChosenType(false); t != "" {
		tok = strings.ReplaceAll(tok, "ChosenType", t)
	}
	if n, ok := m.ChosenNumber(); ok {
		tok = strings.ReplaceAll(tok, "ChosenNumber", strconv.Itoa(n))
	}
	if p := m.ChosenPlayer(); p != NoPlayer {
		tok = strings.ReplaceAll(tok, "ChosenPlayerUID", strconv.Itoa(int(p)))
		tok = strings.ReplaceAll(tok, "ChosenPlayerName", g.Player(p).Name)
	}
	if names := m.NamedCards(); len(names) > 0 {
		tok = strings.ReplaceAll(tok, "ChosenName", "Card.named"+strings.ReplaceAll(names[len(names)-1], ",", ";"))
	}
	if eo := m.ChosenEvenOdd(); eo != "" {
		tok = strings.ReplaceAll(tok, "ChosenEvenOdd", eo)
		tok = strings.ReplaceAll(tok, "chosenEvenOdd", strings.ToLower(eo))
	}
	tok = strings.ReplaceAll(tok, "HostCardUID", strconv.Itoa(int(host.ID)))
	tok = strings.ReplaceAll(tok, "HostCardControllerUID", strconv.Itoa(int(host.Controller())))
	if v, ok := s.Param("CalcKeywordN"); ok {
		n, ok := resolveNamedAmount(g, amounts, host, v)
		if !ok {
			return "", false
		}
		tok = strings.ReplaceAll(tok, "N", strconv.Itoa(n))
	}
	return tok, true
}

// layerKeywordsFor is the per-affected-card half
// (StaticAbilityContinuous.java:706-748): a CardColors/cardColors token
// becomes one keyword per color of c (none while c is colorless),
// ConvertedManaCost becomes c's mana value, and KeywordMultiplier$ repeats
// the list. The shared add slice is returned as-is when neither applies.
func (k *layerKeywords) layerKeywordsFor(c *Card) []string {
	add := k.add
	if k.perCard {
		var kept, extra []string
		for _, tok := range add {
			if strings.Contains(tok, "CardColors") || strings.Contains(tok, "cardColors") {
				extra = append(extra, layerKeywordPerColor(tok, "CardColors", "cardColors", c.Colors())...)
				continue
			}
			kept = append(kept, tok)
		}
		kept = append(kept, extra...)
		add = kept
		for i, tok := range add {
			if strings.Contains(tok, "ConvertedManaCost") {
				add[i] = strings.ReplaceAll(tok, "ConvertedManaCost", strconv.Itoa(c.CMC()))
			}
		}
	}
	if k.multiplier != 1 && len(add) > 0 {
		repeated := make([]string, 0, len(add)*k.multiplier)
		for _, tok := range add {
			for range k.multiplier {
				repeated = append(repeated, tok)
			}
		}
		add = repeated
	}
	return add
}
