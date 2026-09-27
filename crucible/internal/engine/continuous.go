// Continuous effects: CR 613, four layers deep so far. Layer 7b/7c's own
// power/toughness keys (SetPower$/SetToughness$/AddPower$/AddToughness$) are
// the single most common real corpus shape (2,192 of 2,426 real S:Mode$
// Continuous lines carrying one of these four keys, port-log/game-state.md's
// "Continuous effects" section); Layer 4's own type-changing keys (AddType$/
// RemoveType$, applyContinuousType), Layer 5's own color-changing keys
// (AddColor$/SetColor$, applyContinuousColor) and Layer 6's own
// ability-granting key (AddKeyword$, applyContinuousKeyword below -- the
// single largest real slice of all four, 1,556 of 1,857 real lines) are the
// next three, all four evaluated against the same blanket Affected$
// valid-string.
//
// Ported from
// forge-game/src/main/java/forge/game/staticability/StaticAbilityContinuous.java's
// applyContinuousAbility/getAffectedCards.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// continuousConditionMet is StaticAbility.checkConditions' own Condition$
// switch (StaticAbility.java), the one general runtime gate every
// Mode$ Continuous line's six appliers below can carry -- CR 613 folding
// happens every CheckStateBasedActions pass regardless of Condition$, so
// this is evaluated fresh alongside them rather than latched once. A line
// naming no Condition$ passes unconditionally. Ported for the real corpus's
// own values on a Mode$ Continuous line (317 total, port-log/game-state.md's
// "Continuous effects" section): PlayerTurn/NotPlayerTurn (141, 8 -- the
// active player compared against host's own controller, Java's own
// PhaseHandler.isPlayerTurn collapsed to that one comparison), Threshold (61
// -- Player.hasThreshold, seven-plus cards in the controller's own
// graveyard), Metalcraft (18 -- Player.hasMetalcraft, three-plus artifacts
// the controller controls), Delirium (23 -- Player.hasDelirium, four-plus
// distinct core types among cards in the controller's own graveyard,
// AbilityUtils.countCardTypesFromList's own permanentTypes=false form) and
// FatefulHour (3 -- the controller's own life at 5 or below). Not resolved:
// MaxSpeed (40), Blessing (9), EnduringStory (4) and Monarch (2) -- each its
// own mechanic (Alchemy's speed counter, City's Blessing, Saga chapters,
// the monarch) this port tracks no state for anywhere yet, so (like an
// unrecognized Affected$ value already does) the line is skipped rather
// than treated as met (GO-7); an unrecognized value not in the real corpus
// today falls to the same case.
func continuousConditionMet(g *Game, host *Card, s *compile.Ability) bool {
	condition, ok := s.Param("Condition")
	if !ok {
		return true
	}
	controller := host.Controller()
	switch condition {
	case "PlayerTurn":
		return g.ActivePlayer() == controller
	case "NotPlayerTurn":
		return g.ActivePlayer() != controller
	case "Threshold":
		return len(g.Zone(Graveyard, controller).Cards()) >= 7
	case "Hellbent":
		return len(g.Zone(Hand, controller).Cards()) == 0
	case "Metalcraft":
		return battlefieldArtifactCount(g, controller) >= 3
	case "Delirium":
		return graveyardCoreTypeCount(g, controller) >= 4
	case "FatefulHour":
		return g.Player(controller).Life <= 5
	default:
		return false
	}
}

// applyContinuousPT recomputes every battlefield permanent's own Layer
// 7b/7c PTEffects from scratch, from every real Mode$ Continuous S: line
// currently in play. CR 613's own continuous effects are not stored and
// incrementally updated the way a resolved spell's own damage or a counter
// is -- Java's own applyContinuousAbility runs fresh from
// GameAction.checkStateEffects every state-based-action pass, which is why
// this is called from CheckStateBasedActions (action.go) rather than from
// wherever a permanent enters or leaves: an anthem effect has to apply to a
// creature that enters AFTER it, and stop applying the instant the anthem
// itself leaves, neither of which a one-time push at either card's own
// entry could give it.
//
// Every battlefield card's own PT.effects is cleared first, then rebuilt --
// safe because the one other source of a PTEffect, a resolved Pump effect
// (pumpeffect.go), is not rebuilt from a card script here at all: it re-adds
// its own duration-scoped record fresh every pass too, from Game.pumps
// rather than from a card's own Statics, via applyPumpEffects (below),
// called from the identical CheckStateBasedActions sequence right after this
// function and applyContinuousKeyword.
func applyContinuousPT(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).PT.Clear()
		}
	}
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					applyOneContinuousPT(g, h, face.Amounts, s)
				}
			}
		}
	}
}

// applyOneContinuousPT applies s to every battlefield permanent its own
// Affected$ valid-string matches, if s is a Mode$ Continuous line this slice
// can resolve -- or, when s is CharacteristicDefining$ (Layer 7a), computes
// host's own power/toughness and applies it to host alone
// (applyOneCharacteristicDefiningPT, below).
//
// Not resolved, each for a specific reason (game-state.md's "Continuous
// effects" section has the corpus counts behind every number below):
//   - A Condition$ value this port has no player-state for (MaxSpeed,
//     Blessing, EnduringStory, Monarch -- continuousConditionMet's own doc
//     comment has the full account); the resolvable values (PlayerTurn,
//     Threshold, Metalcraft, Delirium, Hellbent, FatefulHour) no longer skip
//     the line here.
//   - AffectedDefined$/AffectedZone$ (0 and 24) -- a targeted or
//     Remembered-driven affected set (AbilityUtils.getDefinedCards) rather
//     than a blanket valid-string match against the whole battlefield.
//   - A non-numeric, non-resolvable AddPower$/AddToughness$/SetPower$/
//     SetToughness$ -- a plain integer or a named SVar resolveAmount
//     (amount.go) evaluates resolves (ptParam, below); only a value it
//     cannot evaluate (xPaid, Count$Party, ExiledWith$, ...: resolveAmount's
//     own doc comment) is skipped, per missing dimension rather than per
//     whole line -- a real corpus line naming both a resolvable and an
//     unresolvable dimension together is not a shape worth losing the
//     resolvable half over.
func applyOneContinuousPT(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	for _, key := range [...]string{"AffectedDefined", "AffectedZone"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	if _, ok := s.Param("CharacteristicDefining"); ok {
		applyOneCharacteristicDefiningPT(g, host, amounts, s)
		return
	}
	affected, ok := s.Param("Affected")
	if !ok {
		return
	}
	addP, hasAddP := ptParam(g, amounts, host, s, "AddPower")
	addT, hasAddT := ptParam(g, amounts, host, s, "AddToughness")
	setP, hasSetP := ptParam(g, amounts, host, s, "SetPower")
	setT, hasSetT := ptParam(g, amounts, host, s, "SetToughness")
	if !hasAddP && !hasAddT && !hasSetP && !hasSetT {
		return
	}

	spec := valid.Parse(affected)
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if !Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
				continue
			}
			c := g.Card(id)
			if hasSetP || hasSetT {
				c.PT.Add(PTEffect{
					Layer: LayerSetPT, Timestamp: host.Timestamp,
					Power: setP, Toughness: setT,
					HasPower: hasSetP, HasToughness: hasSetT,
				})
			}
			if hasAddP || hasAddT {
				c.PT.Add(PTEffect{Layer: LayerModifyPT, Timestamp: host.Timestamp, Power: addP, Toughness: addT})
			}
		}
	}
}

// applyOneCharacteristicDefiningPT is Layer 7a: a characteristic-defining
// ability's own SetPower$/SetToughness$ describes what host's power/
// toughness IS, not an anthem effect reaching other permanents --
// StaticAbilityContinuous.getAffectedCards' own CharacteristicDefining
// branch hardcodes the affected set to `new CardCollection(hostCard)`
// regardless of any Affected$ a real corpus line happens to also carry
// (revenant.txt's own "Affected$ Card.Self," redundant with what Java
// already does unconditionally) -- so this reads no Affected$ param at all,
// unlike every other applyOneContinuous* sibling.
//
// AddPower$/AddToughness$ are not read here: CR 613.3's own "characteristic-
// defining ability... functions in the layer the appropriate
// characteristic-setting ability would normally apply" means a CDA always
// SETS the base value it defines, never adds to one -- no real corpus
// CharacteristicDefining line pairs SetPower$/SetToughness$ with an
// Add-shaped key.
//
// The amount itself is resolveAmount's (amount.go, amountheads.go,
// amountpaid.go): 363 of the corpus's 378 real "*" CDA power/toughness
// dimensions resolve (TestCharacteristicDefiningCorpusFloor, whose four
// planeswalker misses are the test's own loyalty-less walkers dying, not an
// amount gap) -- the Count$Valid family with or without a doXMath
// suffix or a handlePaid property (Tarmogoyf's CardTypes, GreatestCardManaCost,
// ...), SVar$/Number$, Domain, YourLifeTotal, Devotion, Chroma, CardCounters,
// NumInAllHands, ChosenNumber, YouDrewThisTurn, OppGreatestLifeTotal and
// PlayerCountOpponents$HighestCardsInHand. A dimension that does not resolve
// is left off the effect (HasPower/HasToughness false), so a printed "*"
// stays unresolvable rather than reading as zero.
//
// ExcludeZone$ (1 real line among 264 CharacteristicDefining$ True cards) --
// skip host entirely while it sits in one of the named zones -- is not
// resolved: a single real line is not a shape worth a separate zone check
// for, and applyContinuousPT's own battlefield-only walk means host is
// always on the one zone this port could check anyway.
func applyOneCharacteristicDefiningPT(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) {
	if _, ok := s.Param("ExcludeZone"); ok {
		return
	}
	setP, hasSetP := ptParam(g, amounts, host, s, "SetPower")
	setT, hasSetT := ptParam(g, amounts, host, s, "SetToughness")
	if !hasSetP && !hasSetT {
		return
	}
	host.PT.Add(PTEffect{
		Layer: LayerCharacteristic, Timestamp: host.Timestamp,
		Power: setP, Toughness: setT,
		HasPower: hasSetP, HasToughness: hasSetT,
	})
}

// applyContinuousType recomputes every battlefield permanent's own Layer 4
// TypeMod effects from scratch, from every real Mode$ Continuous S: line
// currently in play -- applyContinuousPT's own reasoning applies identically
// here: Java's own applyContinuousAbility runs fresh from
// GameAction.checkStateEffects every state-based-action pass, not stored and
// incrementally updated, so a type-granting effect (an anthem-shaped
// "creatures you control are Zombies") has to reach a creature that enters
// after it and stop the instant it itself leaves.
func applyContinuousType(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).TypeMod.Clear()
		}
	}
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					applyOneContinuousType(g, h, s)
				}
			}
		}
	}
}

// applyOneContinuousType is applyOneContinuousPT's own Layer 4 counterpart:
// s applies to every battlefield permanent its own Affected$ valid-string
// matches, if s is a Mode$ Continuous line naming AddType$ and/or RemoveType$
// in the one shape this slice can resolve -- a plain, space-and-ampersand
// (" & ") separated list of literal type words, no dynamic value and no
// bulk-removal flag.
//
// A whole line is skipped, not applied partially, the instant it carries
// anything past that shape (game-state.md's "Continuous effects" section has
// the corpus counts):
//   - AffectedDefined$/AffectedZone$/CharacteristicDefining$/an unresolved
//     Condition$ value -- applyOneContinuousPT's own skip reasons, identical
//     here since all are properties of the static ability itself, not of
//     which layer it happens to write to.
//   - ChosenType$/ChosenType2$/ImprintedCreatureType$/AllBasicLandType$/
//     AllNonBasicLandType$ as an AddType$ or RemoveType$ token (29 of 256
//     real AddType$ lines) -- each needs a runtime value (a chosen type, an
//     imprinted card's own creature types, the basic-land-type enum) this
//     port has no evaluator for.
//   - RemoveSuperTypes$/RemoveCardTypes$/RemoveSubTypes$/RemoveLandTypes$/
//     RemoveCreatureTypes$/RemoveArtifactTypes$/RemoveEnchantmentTypes$ (62
//     of 284 real AddType$/RemoveType$ lines) -- a bulk "wipe this whole
//     category first" flag most often paired with AddType$ in a real "becomes
//     a Turtle" shape (StaticAbilityContinuous.java:425-448); applying AddType$
//     alone without the wipe would leave the card BOTH its old and new
//     creature types, an actively wrong answer worse than the coverage gap of
//     skipping the whole line (the same reasoning Intimidate's own doc
//     comment, staticability.go, already gives for a property this port would
//     otherwise get backwards).
//   - AddAllCreatureTypes$ (8) -- every creature type in the game, an enum
//     this port's cardtype.Registry is not plumbed into the engine to read
//     from a static-ability effect yet (ParseToken's own doc comment,
//     cardtype.go).
//
// 173 of 256 real AddType$ lines and all 28 real RemoveType$ lines (173+28 of
// 284, game-state.md) carry none of the above and resolve here.
func applyOneContinuousType(g *Game, host *Card, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	for _, key := range [...]string{
		"AffectedDefined", "AffectedZone", "CharacteristicDefining",
		"AddAllCreatureTypes",
		"RemoveSuperTypes", "RemoveCardTypes", "RemoveSubTypes", "RemoveLandTypes",
		"RemoveCreatureTypes", "RemoveArtifactTypes", "RemoveEnchantmentTypes",
	} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	addTypes, hasAdd := typeTokens(s, "AddType")
	removeTypes, hasRemove := typeTokens(s, "RemoveType")
	if !hasAdd && !hasRemove {
		return
	}
	affected, ok := s.Param("Affected")
	if !ok {
		return
	}

	spec := valid.Parse(affected)
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if !Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
				continue
			}
			g.Card(id).TypeMod.Add(TypeEffect{Timestamp: host.Timestamp, AddTypes: addTypes, RemoveTypes: removeTypes})
		}
	}
}

// typeTokens reads key (AddType$ or RemoveType$) as its own " & "-separated
// list of literal type words, each classified by cardtype.ParseToken and
// unioned together -- the fragment TypeEffect carries. false, along with a
// dynamic value (ChosenType and the rest, applyOneContinuousType's own list)
// mixed anywhere into the list, since a token this cannot resolve makes the
// whole line's own Add/Remove set wrong, not just incomplete (the same
// whole-line skip its own doc comment explains).
func typeTokens(s *compile.Ability, key string) (cardtype.Line, bool) {
	v, ok := s.Param(key)
	if !ok {
		return cardtype.Line{}, false
	}
	var out cardtype.Line
	for _, word := range strings.Split(v, " & ") {
		switch word {
		case "ChosenType", "ChosenType2", "ImprintedCreatureType", "AllBasicLandType", "AllNonBasicLandType":
			return cardtype.Line{}, false
		}
		out = out.Union(cardtype.ParseToken(word))
	}
	return out, true
}

// applyContinuousColor recomputes every battlefield permanent's own Layer 5
// ColorMod effects from scratch, from every real Mode$ Continuous S: line
// currently in play -- applyContinuousPT's/applyContinuousType's own
// reasoning applies identically here.
func applyContinuousColor(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).ColorMod.Clear()
		}
	}
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					applyOneContinuousColor(g, h, s)
				}
			}
		}
	}
}

// applyOneContinuousColor is applyOneContinuousPT's/applyOneContinuousType's
// own Layer 5 counterpart: s applies to every battlefield permanent its own
// Affected$ valid-string matches, if s is a Mode$ Continuous line naming
// AddColor$ and/or SetColor$ in the one shape this slice can resolve -- a
// plain, " & "-separated list of literal color words (White/Blue/Black/
// Red/Green), plus the two fixed tokens "All" (WUBRG) and "Colorless" (no
// color at all, `SetColor$ Colorless`'s own real corpus shape).
//
// Skipped, the same reasons applyOneContinuousPT/applyOneContinuousType
// already give for their own params: AffectedDefined$/AffectedZone$/
// CharacteristicDefining$/an unresolved Condition$ value. A "ChosenColor"
// token (7 of 61 real AddColor$/SetColor$ lines) skips the whole line -- a
// runtime value (Card.getChosenColors()) this port has no evaluator for, the
// identical "whole line, not partial" choice typeTokens already makes for
// ChosenType.
// 54 of 61 real lines carry none of it.
func applyOneContinuousColor(g *Game, host *Card, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	for _, key := range [...]string{"AffectedDefined", "AffectedZone", "CharacteristicDefining"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	addColors, hasAdd := colorTokens(s, "AddColor")
	setColors, hasSet := colorTokens(s, "SetColor")
	if !hasAdd && !hasSet {
		return
	}
	affected, ok := s.Param("Affected")
	if !ok {
		return
	}

	spec := valid.Parse(affected)
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if !Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
				continue
			}
			c := g.Card(id)
			if hasSet {
				c.ColorMod.Add(ColorEffect{Timestamp: host.Timestamp, Colors: setColors, Overwrite: true})
			}
			if hasAdd {
				c.ColorMod.Add(ColorEffect{Timestamp: host.Timestamp, Colors: addColors})
			}
		}
	}
}

// colorTokens reads key (AddColor$ or SetColor$) as its own " & "-separated
// list of literal color words, unioned together via colorFromName (valid.go)
// -- the same per-word classification colorMatches uses, without its "non"
// prefix handling, which no real AddColor$/SetColor$ token carries. "All"
// resolves to every color (mana.AllColors) and "Colorless" to no color at
// all (mana.Colors(0), already the zero value) -- Java's own getColorsFromParam
// special-cases both the identical way. false, for the whole token list, the
// moment "ChosenColor" appears anywhere in it -- applyOneContinuousColor's
// own doc comment has the reason.
func colorTokens(s *compile.Ability, key string) (mana.Colors, bool) {
	v, ok := s.Param(key)
	if !ok {
		return 0, false
	}
	var out mana.Colors
	for _, word := range strings.Split(v, " & ") {
		switch word {
		case "ChosenColor":
			return 0, false
		case "All":
			out |= mana.AllColors
		case "Colorless":
			// No color at all -- contributes nothing to out, which is
			// exactly right for a lone "Colorless" token.
		default:
			c, ok := colorFromName(word)
			if !ok {
				return 0, false
			}
			out |= c
		}
	}
	return out, true
}

// applyContinuousKeyword recomputes every battlefield permanent's own Layer
// 6 KeywordMod effects from scratch, from every real Mode$ Continuous S:
// line currently in play -- applyContinuousPT's/applyContinuousType's own
// reasoning applies identically here.
func applyContinuousKeyword(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).KeywordMod.Clear()
		}
	}
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					applyOneContinuousKeyword(g, h, s)
				}
			}
		}
	}
}

// applyOneContinuousKeyword is applyOneContinuousPT's/Type's/Color's own
// Layer 6 counterpart: s applies to every battlefield permanent its own
// Affected$ valid-string matches, if s is a Mode$ Continuous line naming
// AddKeyword$ in the one shape this slice can resolve -- a plain, " & "-
// separated list of literal keyword lines, already written exactly the way
// a real K: line would be ("Ward:2", "First Strike", "Protection:...") --
// keywordTokens (below) hands each one to KeywordEffect verbatim, and
// HasKeyword (card.go) reads them back with keyword.Parse the identical way
// it already reads a printed keyword.
//
// A whole line is skipped, not applied partially, the instant it carries:
//   - RemoveKeyword$/RemoveAllAbilities$ (5 of 1,561 real AddKeyword$
//     lines) -- this slice does not resolve either removal direction yet
//     (KeywordMod's own doc comment), and applying the add half of a "gains
//     X, loses Y" line without the remove half would leave the card with
//     both, an answer worse than the coverage gap of skipping the whole
//     line -- applyOneContinuousType's own "becomes a Turtle" paragraph
//     gives the identical reasoning.
//   - SharedKeywords$/FromDraftNotes$ -- a game-wide, remembered-list or
//     draft-note source for the keyword list rather than a fixed token list
//     (StaticAbilityContinuous.java's own alternate addKeywords-building
//     branches).
//   - a dynamic-value marker anywhere inside any one token (keywordTokens'
//     own doc comment has the full list, StaticAbilityContinuous.java's own
//     removeIf lambda) -- 42 of 1,857 real AddKeyword$ lines.
//
// 1,556 of 1,857 real AddKeyword$ lines carry none of the above and
// resolve.
func applyOneContinuousKeyword(g *Game, host *Card, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	for _, key := range [...]string{
		"AffectedDefined", "AffectedZone", "CharacteristicDefining",
		"RemoveKeyword", "RemoveAllAbilities", "SharedKeywords", "FromDraftNotes",
	} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	keywords, ok := keywordTokens(s, "AddKeyword")
	if !ok {
		return
	}
	affected, ok := s.Param("Affected")
	if !ok {
		return
	}

	spec := valid.Parse(affected)
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if !Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
				continue
			}
			g.Card(id).KeywordMod.Add(KeywordEffect{Timestamp: host.Timestamp, AddKeywords: keywords})
		}
	}
}

// applyContinuousNames recomputes every battlefield permanent's own
// HasNonLegendaryCreatureNames flag (card.go) from scratch, from every real
// Mode$ Continuous S: line currently in play naming
// AddNames$ AllNonLegendaryCreatureNames -- applyContinuousPT's own "recompute
// fresh every pass" reasoning applies identically here, and there is no
// timestamp fold to do: this is a plain "does any current line grant it"
// question, not a value more than one source could disagree about.
//
// Spy Kit is the corpus's only real line naming AddNames$ at all (1), and its
// own shape is AffectedDefined$ Equipped -- host's own AttachedTo() (card.go)
// resolves that directly, since this port already models Equipment
// attachment the identical way an Aura's is (Attach/AttachedTo). resolveLegendRule
// (action.go) is the one reader.
func applyContinuousNames(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).HasNonLegendaryCreatureNames = false
		}
	}
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					applyOneContinuousNames(g, h, s)
				}
			}
		}
	}
}

// applyOneContinuousNames grants s's own target(s) HasNonLegendaryCreatureNames,
// if s is a Mode$ Continuous line naming AddNames$ AllNonLegendaryCreatureNames
// -- the only real value this key takes corpus-wide, so any other value skips
// the line rather than guessing (GO-7). Affected$/AffectedDefined$ resolve the
// identical way applyOneContinuousPT's own do, except AffectedDefined$ is not
// refused here: it is the one real corpus line's own shape
// (AffectedDefined$ Equipped | Affected$ Creature), so skipping on it would
// make this whole applier dead code against the actual corpus. AffectedZone$/
// CharacteristicDefining$ (0 real lines paired with AddNames$) are refused,
// the same as every other layer's own applier.
func applyOneContinuousNames(g *Game, host *Card, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	addNames, ok := s.Param("AddNames")
	if !ok || !strings.EqualFold(addNames, "AllNonLegendaryCreatureNames") {
		return
	}
	for _, key := range [...]string{"AffectedZone", "CharacteristicDefining"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}

	var targets []CardID
	if affectedDefined, ok := s.Param("AffectedDefined"); ok {
		if !strings.EqualFold(affectedDefined, "Equipped") {
			return
		}
		equipped, attached := host.AttachedTo()
		if !attached {
			return
		}
		targets = []CardID{equipped}
	} else {
		for _, pid := range g.Players() {
			targets = append(targets, g.Zone(Battlefield, pid).Cards()...)
		}
	}

	affected, ok := s.Param("Affected")
	if !ok {
		return
	}
	spec := valid.Parse(affected)
	for _, id := range targets {
		if !Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
			continue
		}
		g.Card(id).HasNonLegendaryCreatureNames = true
	}
}

// applyPumpEffects re-adds every resolved Pump effect's own contribution
// (pumpeffect.go) into its target's Layer 7b/7c PT and Layer 6 KeywordMod --
// the one-shot counterpart to applyContinuousPT's/applyContinuousKeyword's
// own Mode$ Continuous statics loop, called from CheckStateBasedActions
// (action.go) right after both so it runs after their own Clear() has
// already emptied every battlefield card's effects for this pass. A Pump
// record has no S: line behind it to re-derive from, so it is kept in
// Game.pumps (game.go) directly instead and applied fresh every pass the
// identical way a static ability's own line is -- cleanupStep (turn.go)
// drops every non-Permanent record at end of turn, CR 514.2's own "until
// end of turn" effects wearing off, closing the gap applyContinuousPT's own
// doc comment used to name.
//
// A record whose card has left the battlefield since it was recorded (or
// was never there -- Enchanted$/Equipped$ resolving to a non-permanent) is
// skipped rather than applied: Card.PT/KeywordMod are only ever read for a
// battlefield permanent (Card.Power/Toughness/HasKeyword), so adding to
// either for a card nowhere reads them from would be inert, not wrong, but
// skipping is also what keeps a since-departed card's own entry from
// silently piling up in Game.pumps until this turn's cleanup removes it.
//
// A phased-out card's record is skipped too, and kept: the Clear() passes
// before this walk the battlefield enumeration, which leaves a phased-out
// permanent out (Zone.Cards, ADR-0021), so re-adding to it would stack one
// more copy of the pump every pass. It applies again once the card phases
// back in, as Java's own pump -- a boost stored on the card itself -- does.
func applyPumpEffects(g *Game) {
	for _, p := range g.pumps {
		c := g.Card(p.Card)
		if c.Zone != Battlefield || c.IsPhasedOut() {
			continue
		}
		if p.Power != 0 || p.Toughness != 0 {
			c.PT.Add(PTEffect{Layer: LayerModifyPT, Timestamp: p.Timestamp, Power: p.Power, Toughness: p.Toughness})
		}
		if len(p.Keywords) > 0 {
			c.KeywordMod.Add(KeywordEffect{Timestamp: p.Timestamp, AddKeywords: p.Keywords})
		}
	}
}

// keywordTokens reads key (AddKeyword$) as its own " & "-separated list of
// literal keyword lines, returned verbatim -- each token is exactly what a
// K: line would carry, HasKeyword's own job to parse further at query time,
// not this function's. false, for the whole line, the moment a
// dynamic-value marker (StaticAbilityContinuous.java's own removeIf lambda:
// ChosenColor, ChosenType, ChosenNumber, ChosenPlayer, ChosenName,
// ChosenEvenOdd, AllColors/allColors, CommanderColorID,
// ColorsYouCtrl/colorsYouCtrl, YourBasic) appears anywhere within any one
// token -- checked by substring, matching Java's own `input.contains(...)`,
// since a marker is often a qualifier embedded in a larger token
// ("Protection:Card.ChosenColor:chosenColor") rather than the whole token
// itself.
func keywordTokens(s *compile.Ability, key string) ([]string, bool) {
	v, ok := s.Param(key)
	if !ok {
		return nil, false
	}
	tokens := strings.Split(v, " & ")
	for _, tok := range tokens {
		for _, marker := range [...]string{
			"ChosenColor", "ChosenType", "ChosenNumber", "ChosenPlayer", "ChosenName",
			"ChosenEvenOdd", "chosenEvenOdd", "AllColors", "allColors", "CommanderColorID",
			"ColorsYouCtrl", "colorsYouCtrl", "YourBasic",
		} {
			if strings.Contains(tok, marker) {
				return nil, false
			}
		}
	}
	return tokens, true
}

// ptParam reads key, then resolves it the same way resolveNamedAmount
// (trigger.go) does: a plain base-10 integer (optionally negative) --
// AddPower$/AddToughness$/SetPower$/SetToughness$'s own corpus-frequent
// shape -- or, failing that, the name of an SVar amounts defines (Java's own
// `ctb.getSVar(n)` lookup, xCount), resolved via resolveAmount (amount.go).
// Reports false for a missing key, or a value that is neither a plain
// integer nor a name amounts resolves (a genuinely dynamic value --
// AffectedX, xPaid, Count$Party, ... -- resolveAmount's own doc comment has
// the full account) -- the same "not resolvable, coverage gap rather than a
// wrong answer" contract compareMatches (valid.go) already documents.
func ptParam(g *Game, amounts map[string]expr.Amount, host *Card, s *compile.Ability, key string) (int, bool) {
	v, ok := s.Param(key)
	if !ok {
		return 0, false
	}
	return resolveNamedAmount(g, amounts, host, v)
}

// applyContinuousRules recomputes every player's own Layer 8 RulesEffects
// from scratch, applyContinuousPT's own reasoning (above) applied to a
// player rather than a card: SetMaxHandSize$/RaiseMaxHandSize$/
// AdjustLandPlays$ Read Player.HandSizeLimit/LandPlayLimit (player.go).
func applyContinuousRules(g *Game) {
	for _, pid := range g.Players() {
		g.Player(pid).Rules.Clear()
	}
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					applyOneContinuousRules(g, h, face.Amounts, s)
				}
			}
		}
	}
}

// applyOneContinuousRules is Layer 8: s applies to every player its own
// Affected$ spec matches (matchesPlayerSpec, valid.go -- the identical
// dispatch every other player-shaped Affected/ValidPlayer/ValidActivatingPlayer
// check in this port already reuses, applied here against a static
// ability's Affected$ rather than a trigger's own player-shaped param), if
// s is a Mode$ Continuous line naming SetMaxHandSize$, RaiseMaxHandSize$
// and/or AdjustLandPlays$ in a shape rulesEffect (below) can resolve.
//
// Not resolved, each for a specific reason:
//   - AffectedDefined$/AffectedZone$/CharacteristicDefining$/an unresolved
//     Condition$ value -- applyOneContinuousPT's own skip reasons (a
//     CharacteristicDefining line makes no sense for a player-facing effect
//     anyway). The one real line pairing Condition$ Delirium with
//     SetMaxHandSize$ (Winter, Misanthropic Guide) applies: its
//     `Number$7/Minus.X` over a `Count$ValidGraveyard ...$CardTypes` X
//     resolves through resolveAmount (amount.go, amountpaid.go).
//   - MayLookAt$/MayPlay$ (88, 181 real lines corpus-wide) -- a cast-time
//     zone-eligibility permission CastSpell's own hand-only check
//     (castspell.go) has nowhere to consult yet.
//   - ControlOpponentsSearchingLibrary$/ControlVote$/AdditionalVote$/
//     AdditionalOptionalVote$/AdditionalVillainousChoice$/
//     DeclaresAttackers$/DeclaresBlockers$ (0-3 real lines each) --
//     multiplayer/vote mechanics this port has no concept of at all.
//   - IgnoreEffectCost$/AddHiddenKeyword$ (4, 19) -- each its own separate
//     mechanic (a cost-ignoring ability grant; a hidden functional keyword
//     whose own real values -- "must be blocked if able," "can't attack
//     alone," "doesn't untap," ... -- are each a distinct
//     block/attack/untap-step rule this port's own combat/turn model has no
//     hook for, none of them sharing enough machinery to be worth building
//     as one slice the way SetMaxHandSize/AdjustLandPlays do).
//   - A qualified Affected$ matchesPlayerSpec cannot resolve
//     (Player.NotedForGreenAnchor, Player.Chosen -- 1 real line each,
//     matchesPlayerSpec's own doc comment has the general reason).
//
// 75 of the corpus's 78 real SetMaxHandSize$/RaiseMaxHandSize$/
// AdjustLandPlays$ lines resolve here.
func applyOneContinuousRules(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	for _, key := range [...]string{"AffectedDefined", "AffectedZone", "CharacteristicDefining"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	effect, ok := rulesEffect(g, host, amounts, s)
	if !ok {
		return
	}
	affected, ok := s.Param("Affected")
	if !ok {
		return
	}
	for _, pid := range g.Players() {
		matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, affected)
		if !recognized || !matched {
			continue
		}
		g.Player(pid).Rules.Add(effect)
	}
}

// rulesEffect reads s's own SetMaxHandSize$/RaiseMaxHandSize$/
// AdjustLandPlays$ params into one RulesEffect. "Unlimited" (Java's own
// literal sentinel for `p.setUnlimitedHandSize(true)`/
// `p.addMaxLandPlaysInfinite`) is checked before falling to ptParam (above)
// for the numeric case, since ptParam itself would just report it
// unresolvable (neither a plain integer nor a name amounts defines) --
// correctly, on its own terms, but the caller here needs to tell "no
// maximum" apart from "genuinely could not resolve this." ok is false the
// moment ANY dimension s names cannot be resolved, not just the ones that
// can -- applyOneContinuousType's own "skip the whole line rather than
// apply it partially" contract, ported here even though no real corpus
// line currently names more than one of the three at once.
func rulesEffect(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) (RulesEffect, bool) {
	e := RulesEffect{Timestamp: host.Timestamp}
	hasEffect := false

	if v, ok := s.Param("SetMaxHandSize"); ok {
		if strings.EqualFold(v, "Unlimited") {
			e.HasSetHandSize, e.SetHandSizeUnlimited, hasEffect = true, true, true
		} else if n, ok := ptParam(g, amounts, host, s, "SetMaxHandSize"); ok {
			e.HasSetHandSize, e.SetHandSize, hasEffect = true, n, true
		} else {
			return RulesEffect{}, false
		}
	}
	if _, ok := s.Param("RaiseMaxHandSize"); ok {
		n, ok := ptParam(g, amounts, host, s, "RaiseMaxHandSize")
		if !ok {
			return RulesEffect{}, false
		}
		e.HasRaiseHandSize, e.RaiseHandSize, hasEffect = true, n, true
	}
	if v, ok := s.Param("AdjustLandPlays"); ok {
		if strings.EqualFold(v, "Unlimited") {
			e.HasAdjustLandPlays, e.AdjustLandPlaysUnlimited, hasEffect = true, true, true
		} else if n, ok := ptParam(g, amounts, host, s, "AdjustLandPlays"); ok {
			e.HasAdjustLandPlays, e.AdjustLandPlays, hasEffect = true, n, true
		} else {
			return RulesEffect{}, false
		}
	}
	return e, hasEffect
}

// applyContinuousControl recomputes every battlefield card's own Layer 2
// ControlMod from scratch, the identical "clear every card first, then walk
// every Mode$ Continuous static and rebuild" shape applyContinuousPT's own
// doc comment gives -- called FIRST among the six appliers
// (CheckStateBasedActions, action.go), ahead of Layers 4/5/6/7/8, since CR
// 613.1 puts the control layer before every one of them and, concretely,
// applyOneContinuousType/Color/Keyword/PT/Rules all read Affected$ specs
// that can themselves name "YouCtrl" -- a stale Controller() at that point
// would be evaluating those specs against last pass's controller, not this
// one's.
func applyContinuousControl(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).ControlMod.Clear()
		}
	}
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					applyOneContinuousControl(g, h, s)
				}
			}
		}
	}
}

// applyOneContinuousControl is Layer 2: s hands control of every battlefield
// card its own Affected$ valid-string matches to whatever player its own
// GainControl$ names, if s is a Mode$ Continuous line naming GainControl$ in
// a shape this resolves.
//
// Ported from StaticAbilityContinuous.java's own CONTROL branch
// (applyContinuousAbility): `AbilityUtils.getDefinedPlayers(hostCard,
// params.get("GainControl"), stAb).get(0)` -- a "defined player" lookup, not
// a valid-string membership test the way Rules'/PT's own Affected$-for-
// players dispatch (matchesPlayerSpec) is, since GainControl$ names WHO
// gains control rather than describing a set to test candidates against.
//
// Of the corpus's 44 real S:Mode$ Continuous lines naming GainControl$ (a
// separate, unrelated `DB$ ChangeZone | GainControl$ True`/`DB$ Dig | ... |
// GainControl$ True` one-shot "put onto the battlefield under your control"
// effect -- ChangeZoneEffect.java, M6's own remaining script-effect gap, not
// this layer at all -- shares the same param name and inflates a naive
// corpus grep for "GainControl$" past 44 unless the two are told apart by
// Mode$ first):
//   - GainControl$ You (43 of 44) resolves: getDefinedPlayers' own "You"
//     case is `players.add(player)`, and `player` is `card.getController()`
//     whenever sa is not a SpellAbility (every real Continuous static
//     ability here), i.e. the effect's own host -- host.Controller() below.
//   - GainControl$ Player.isMonarch (1 of 44) does not: a qualified
//     getDefinedPlayers form (the "else" branch's own
//     `game.getPlayersInTurnOrder()` filtered by `PlayerPredicates
//     .restriction`) this port has no monarch mechanic to filter by, so the
//     whole line is skipped (PORT-8/GO-7) rather than guessing "the
//     controller" and being wrong for every game that ever changes hands.
//
// Affected$ on these 44 lines is overwhelmingly Card.EnchantedBy/
// Permanent.EnchantedBy/Creature.EnchantedBy (42 of 44, Control Magic's own
// shape -- the Aura's host) -- already the identical valid-string
// evaluation applyOneContinuousPT's own Matches call uses, needing nothing
// new here.
func applyOneContinuousControl(g *Game, host *Card, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	for _, key := range [...]string{"AffectedDefined", "AffectedZone", "CharacteristicDefining"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	gain, ok := s.Param("GainControl")
	if !ok || !strings.EqualFold(gain, "You") {
		return
	}
	affected, ok := s.Param("Affected")
	if !ok {
		return
	}
	gainer := host.Controller()
	spec := valid.Parse(affected)
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if !Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
				continue
			}
			g.Card(id).ControlMod.Add(ControlEffect{Timestamp: host.Timestamp, Controller: gainer})
		}
	}
}
