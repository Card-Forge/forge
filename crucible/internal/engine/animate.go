// Animate-shaped continuous effects: the one-shot counterpart to a Mode$
// Continuous line, created by a resolving Animate/AnimateAll/Debuff/
// Protection/ProtectionAll ability rather than a card's own S: line.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/
// AnimateEffectBase.java's doAnimate and the addChanged* calls
// DebuffEffect/ProtectEffect make.

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// animateRecord is one resolved effect's contribution to one card, every
// layer it touches under one timestamp: Layer 4 (Types), Layer 5 (Colors),
// Layer 6 (Keywords), Layer 7b (SetPower/SetToughness -- Java's addNewPT).
// Permanent is Duration$ Permanent; anything else ends at cleanup, the
// "until end of turn" default every one of these effects shares
// (SpellAbilityEffect.addUntilCommand).
type animateRecord struct {
	Card      CardID
	Timestamp uint64
	Permanent bool
	// endBound is a host-bound Duration$ (AsLongAsControl) whose end is a
	// command on endHost, registered per card by animateCards.
	endBound string
	endHost  CardID

	Types TypeEffect

	Colors          mana.Colors
	HasColors       bool
	OverwriteColors bool

	AddKeywords    []string
	RemoveKeywords []string
	RemoveAllKW    bool

	Power, Toughness       int
	HasPower, HasToughness bool

	// abilities are the activated abilities Abilities$ grants (Layer 6,
	// AnimateEffectBase.java:159-168), compiled at load (ADR-0023 decision
	// 1), and abilityAmounts the granting face's SVars they read. Shared,
	// never edited: Clone copies the record by value.
	abilities      []*compile.Ability
	abilityAmounts map[string]expr.Amount
	// triggers are the triggers a Triggers$ without Duration$ Perpetual
	// grants (AnimateEffect.java:127-130): read through traitGrants while the
	// record lives, so they end with it (cleanup, or Permanent: never) and
	// go with the card at a zone change.
	triggers []*compile.Ability
}

// changesTypes reports whether r carries any Layer 4 change.
func (r *animateRecord) changesTypes() bool {
	t := r.Types
	return !t.AddTypes.IsEmpty() || !t.RemoveTypes.IsEmpty() || t.RemoveCardTypes ||
		t.RemoveSuperTypes || t.RemoveSubTypes || t.DropSubtype != nil
}

// applyAnimateEffects re-adds every animateRecord into its card's layer
// mods, all four layers at once -- addAnimate's immediate application. The
// state-based-action pass instead calls each layer's half at the start of
// that layer (animateTypes, animateColors, animateKeywords, animatePT), right
// after the layer's own clear: a resolved effect is already on the card when
// the layer's statics evaluate their Affected$ sets, as Java's changed*
// tables keep it (ADR-0025, Decision 1). A record whose card is not on the
// battlefield is skipped, and so is one whose card is phased out, for
// applyPumpEffects' own reason: the Clear() passes never reach it
// (ADR-0021), so re-adding would stack.
func applyAnimateEffects(g *Game) {
	animateTypes(g)
	animateColors(g)
	animateKeywords(g)
	animateAbilities(g)
	animatePT(g)
}

// liveAnimates calls fn for every animateRecord whose card is on the
// battlefield and phased in.
func liveAnimates(g *Game, fn func(c *Card, r *animateRecord)) {
	for i := range g.animates {
		r := &g.animates[i]
		c := g.Card(r.Card)
		if c.Zone != Battlefield || c.IsPhasedOut() {
			continue
		}
		fn(c, r)
	}
}

// animateTypes is applyAnimateEffects' Layer 4 half.
func animateTypes(g *Game) {
	liveAnimates(g, func(c *Card, r *animateRecord) {
		if r.changesTypes() {
			te := r.Types
			te.Timestamp = r.Timestamp
			c.TypeMod.Add(te)
		}
	})
}

// animateColors is applyAnimateEffects' Layer 5 half.
func animateColors(g *Game) {
	liveAnimates(g, func(c *Card, r *animateRecord) {
		if r.HasColors {
			c.ColorMod.Add(ColorEffect{Timestamp: r.Timestamp, Colors: r.Colors, Overwrite: r.OverwriteColors})
		}
	})
}

// animateKeywords is applyAnimateEffects' Layer 6 half.
func animateKeywords(g *Game) {
	liveAnimates(g, func(c *Card, r *animateRecord) {
		if len(r.AddKeywords) > 0 || len(r.RemoveKeywords) > 0 || r.RemoveAllKW {
			c.KeywordMod.Add(KeywordEffect{
				Timestamp: r.Timestamp, AddKeywords: r.AddKeywords,
				RemoveKeywords: r.RemoveKeywords, RemoveAll: r.RemoveAllKW,
			})
		}
	})
}

// animateAbilities is applyAnimateEffects' Layer 6 trait half: each record's
// Abilities$ grant becomes one traitGrant row on its card, the overlay
// abilityAt reads after the printed lines (ADR-0023 decision 2: a one-shot
// grant carries its own duration, the record's, and is re-added every pass
// like the continuous ones). It runs where animateKeywords does, before the
// statics' own Layer 6 grants and removals, which then apply over it.
func animateAbilities(g *Game) {
	liveAnimates(g, func(c *Card, r *animateRecord) {
		if len(r.abilities) > 0 || len(r.triggers) > 0 {
			c.traitGrants = append(append([]traitGrant(nil), c.traitGrants...), traitGrant{abilities: r.abilities, triggers: r.triggers, amounts: r.abilityAmounts})
		}
	})
}

// animatePT is applyAnimateEffects' Layer 7b half.
func animatePT(g *Game) {
	liveAnimates(g, func(c *Card, r *animateRecord) {
		if r.HasPower || r.HasToughness {
			c.PT.Add(PTEffect{
				Layer: LayerSetPT, Timestamp: r.Timestamp,
				Power: r.Power, Toughness: r.Toughness,
				HasPower: r.HasPower, HasToughness: r.HasToughness,
			})
		}
	})
}

// addAnimate records r and applies it at once, so the card's
// characteristics change as the effect resolves rather than at the next
// state-based-action pass -- a SubAbility$ chained after it (or a trigger
// checked right after) already sees the animated card, the way Java's
// addChanged* calls take effect immediately.
func (g *Game) addAnimate(r animateRecord) {
	g.animates = append(g.animates, r)
	saved := g.animates
	g.animates = g.animates[len(g.animates)-1:]
	applyAnimateEffects(g)
	g.animates = saved
}

// clearAnimates drops every animateRecord naming id -- clearPumps' own
// reason (game.go): a CardID survives a zone change here, so a Permanent
// record would otherwise reapply to the card when it returns.
func (g *Game) clearAnimates(id CardID) {
	kept := g.animates[:0]
	for _, r := range g.animates {
		if r.Card != id {
			kept = append(kept, r)
		}
	}
	g.animates = kept
}

// endAnimatesAtCleanup drops every non-Permanent record: CR 514.2's "until
// end of turn" effects ending, the same cleanup Pump records get.
func (g *Game) endAnimatesAtCleanup() {
	kept := g.animates[:0]
	for _, r := range g.animates {
		if r.Permanent {
			kept = append(kept, r)
		}
	}
	g.animates = kept
}

// subtypeCategoryDrop builds DropSubtype for the Remove*Types$ category
// params, reading the corpus' subtype vocabulary off the game's DB. A
// category removal with no vocabulary to test against is unresolvable.
func subtypeCategoryDrop(g *Game, land, creature, artifact, enchantment bool) (func(string) bool, bool) {
	if !land && !creature && !artifact && !enchantment {
		return nil, true
	}
	reg := g.db.Types()
	if reg == nil {
		return nil, false
	}
	return func(s string) bool {
		return (land && reg.IsLandType(s)) ||
			(creature && reg.IsCreatureType(s)) ||
			(artifact && reg.Is(cardtype.CategoryArtifact, s)) ||
			(enchantment && reg.Is(cardtype.CategoryEnchantment, s))
	}, true
}

// animateUnresolvedParams are the doAnimate/AnimateEffect params this port
// does not model: granted statics and SVars (neither is compiled at load
// yet, PORT-2 -- Triggers$, Replacements$ and Abilities$ are,
// animateTriggerGrants and animateAbilityGrants), TransferActivator$'s
// "only you may activate" on a granted ability (Martyrdom), hidden keywords,
// "can't have" keywords, removing abilities, all creature types, a
// renaming, mana-cost changes, a revert cost, a leave-the-battlefield
// replacement, what the animated card remembers or imprints, an Optional$
// confirmation. Condition$ is
// the two effects' own (rejectUnreadCondition, then subAbilityConditionMet).
var animateUnresolvedParams = [...]string{
	"staticAbilities", "sVars", "HiddenKeywords", "TransferActivator",
	"CantHaveKeyword", "RemoveAllAbilities", "RemoveNonManaAbilities", "RemoveThisAbility",
	"AddAllCreatureTypes", "Name", "ManaCost", "Incorporate", "RevertCost", "LeaveBattlefield",
	"RememberObjects", "ImprintCards", "Optional", "OptionQuestion", "TgtZone",
	"ConditionDefined",
}

// animateDuration reads Duration$: absent is until end of turn, Permanent
// never ends; every other duration (UntilHostLeavesPlay, UntilYourNextTurn,
// Perpetual on anything but a trigger grant, ...) needs a lifetime this port
// does not track.
func animateDuration(a *Ability, api string) (bool, error) {
	d, ok := a.Params.Param("Duration")
	if !ok {
		return false, nil
	}
	if d != "Permanent" {
		return false, fmt.Errorf("engine: %s: Duration$ %q not resolvable yet", api, d)
	}
	return true, nil
}

// buildAnimate is AnimateEffect.resolve's and AnimateAllEffect.resolve's
// shared read of the characteristic params, before doAnimate: Power$/
// Toughness$ (Layer 7b), Types$/RemoveTypes$ (comma lists) and the
// Remove*Types$ flags (Layer 4), Colors$ -- a comma list of color names,
// ChosenColor or All -- with OverwriteColors$ (Layer 5), and Keywords$/
// RemoveKeywords$ (" & " lists, Layer 6). Types$ ChosenType and a
// keyword naming one of the host's SVars (Java substitutes its text) are
// not resolved. The triggers a Duration$ Perpetual Triggers$ grants come
// back beside the record (animateTriggerGrants), nil when there are none.
func buildAnimate(g *Game, a *Ability, api string) (animateRecord, []*compile.Ability, error) {
	var r animateRecord
	if err := rejectParams(a, api, animateUnresolvedParams[:]...); err != nil {
		return r, nil, err
	}
	grants, timed, err := animateTriggerGrants(a, api)
	if err != nil {
		return r, nil, err
	}
	r.triggers = timed
	if d, _ := a.Params.Param("Duration"); grants == nil && api == "Animate" && hostBoundDuration(d) {
		// AnimateEffectBase's unanimate closure goes through addUntilCommand,
		// so the record outlives cleanup and a command on the host ends it.
		r.Permanent, r.endBound, r.endHost = true, d, a.Source
	} else if grants == nil {
		if r.Permanent, err = animateDuration(a, api); err != nil {
			return r, nil, err
		}
	}
	r, err = buildAnimateCharacteristics(g, a, api, r)
	if err != nil {
		return r, nil, err
	}
	if r.abilities, err = animateAbilityGrants(a, api); err != nil {
		return r, nil, err
	}
	r.abilityAmounts = a.Amounts
	if grants != nil && !r.empty() {
		// PerpetualPTBoost/PerpetualTypes/... (AnimateEffectBase.java:51-235)
		// each need a zone-surviving store of their own; an ordinary record
		// would be dropped by clearAnimates at the card's next zone change.
		return r, nil, fmt.Errorf("engine: %s: Duration$ Perpetual on a characteristic change not resolvable yet", api)
	}
	return r, grants, nil
}

// buildAnimateCharacteristics reads buildAnimate's characteristic params
// into r.
func buildAnimateCharacteristics(g *Game, a *Ability, api string, r animateRecord) (animateRecord, error) {
	var err error
	if r.HasPower = hasParam(a, "Power"); r.HasPower {
		if r.Power, err = optionalAmount(g, a, api, "Power", 0); err != nil {
			return r, err
		}
	}
	if r.HasToughness = hasParam(a, "Toughness"); r.HasToughness {
		if r.Toughness, err = optionalAmount(g, a, api, "Toughness", 0); err != nil {
			return r, err
		}
	}
	for _, key := range [...]string{"Types", "RemoveTypes"} {
		raw, ok := a.Params.Param(key)
		if !ok {
			continue
		}
		var line cardtype.Line
		for _, word := range strings.Split(raw, ",") {
			word = strings.TrimSpace(word)
			if strings.HasPrefix(word, "ChosenType") {
				return r, fmt.Errorf("engine: %s: %s$ %q not resolvable yet", api, key, raw)
			}
			line = line.Union(cardtype.ParseToken(word))
		}
		if key == "Types" {
			r.Types.AddTypes = line
		} else {
			r.Types.RemoveTypes = line
		}
	}
	r.Types.RemoveCardTypes = hasParam(a, "RemoveCardTypes")
	r.Types.RemoveSuperTypes = hasParam(a, "RemoveSuperTypes")
	r.Types.RemoveSubTypes = hasParam(a, "RemoveSubTypes")
	drop, ok := subtypeCategoryDrop(g, hasParam(a, "RemoveLandTypes"), hasParam(a, "RemoveCreatureTypes"),
		hasParam(a, "RemoveArtifactTypes"), hasParam(a, "RemoveEnchantmentTypes"))
	if !ok {
		return r, fmt.Errorf("engine: %s: a Remove*Types$ category needs the subtype vocabulary, which this game's DB lacks", api)
	}
	r.Types.DropSubtype = drop
	r.Types.RemoveLandTypes = hasParam(a, "RemoveLandTypes")

	if raw, ok := a.Params.Param("Colors"); ok {
		r.HasColors = true
		r.OverwriteColors = hasParam(a, "OverwriteColors")
		switch raw {
		case "ChosenColor":
			r.Colors = g.Card(a.Source).Memory.ChosenColors()
		case "All":
			r.Colors = mana.AllColors
		default:
			for _, name := range strings.Split(raw, ",") {
				name = strings.TrimSpace(name)
				if strings.EqualFold(name, "Colorless") {
					continue
				}
				c, ok := colorFromName(strings.ToUpper(name[:1]) + strings.ToLower(name[1:]))
				if !ok {
					return r, fmt.Errorf("engine: %s: Colors$ %q not resolvable", api, raw)
				}
				r.Colors |= c
			}
		}
	}
	if raw, ok := a.Params.Param("Keywords"); ok {
		for _, k := range strings.Split(raw, " & ") {
			if _, isSVar := a.Amounts[strings.ToLower(k)]; isSVar || strings.Contains(k, "HIDDEN") {
				return r, fmt.Errorf("engine: %s: Keywords$ %q not resolvable yet", api, k)
			}
			r.AddKeywords = append(r.AddKeywords, k)
		}
	}
	if raw, ok := a.Params.Param("RemoveKeywords"); ok {
		r.RemoveKeywords = strings.Split(raw, " & ")
	}
	return r, nil
}

// empty reports whether r changes nothing at all.
func (r *animateRecord) empty() bool {
	return !r.changesTypes() && !r.HasColors && len(r.AddKeywords) == 0 && len(r.RemoveKeywords) == 0 &&
		!r.RemoveAllKW && !r.HasPower && !r.HasToughness && len(r.abilities) == 0 && len(r.triggers) == 0
}

// animateCards applies template to each card on the battlefield under one
// fresh timestamp (every Animate event takes getNextTimestamp once).
func (g *Game) animateCards(template animateRecord, cards []CardID) {
	g.timestamp++
	ts := g.timestamp
	for _, id := range cards {
		// A phased-out permanent is not animated at all
		// (AnimateEffect.java:168, :176).
		if c := g.Card(id); c.Zone != Battlefield || c.IsPhasedOut() {
			continue
		}
		r := template
		r.Card, r.Timestamp = id, ts
		g.addAnimate(r)
		if r.endBound != "" {
			g.registerHostBoundEnd(g.Card(r.endHost), cardCommand{Kind: commandEndAnimate, Target: id, Timestamp: ts}, r.endBound)
		}
	}
}

// animateTriggerGrants reads Triggers$ and Replacements$ (ADR-0023's trigger
// slice). Duration$ Perpetual is PerpetualAbilities over changedCardTraits
// (AnimateEffectBase.java:221-233): a grant that never ends and survives zone
// changes (grantedTriggers, card.go), returned as perpetual. Any other
// Triggers$ is a trigger that lives as the animateRecord does -- until end of
// turn, or Permanent -- returned as timed (animateAbilities reads it). A
// Replacements$ without Perpetual is refused, as is a grant compile left
// unfollowed because the trigger regrants itself (Snarlfang Vermin,
// compile.go's regrants). An Execute$ naming TgtZone$ (Pass the Torch's own
// TrigPlay) needs no refusal of its own here: pushTriggeredAbilities calls
// resolveTargets uniformly for every pushed ability, granted trigger
// included, and targetChoiceFor (targeting.go) reads TgtZone$ there the same
// way any other pushed ability's targeting would.
func animateTriggerGrants(a *Ability, api string) (perpetual, timed []*compile.Ability, err error) {
	d, _ := a.Params.Param("Duration")
	for _, key := range [...]string{"Triggers", "Replacements"} {
		raw, ok := a.Params.Param(key)
		if !ok {
			continue
		}
		if d != "Perpetual" && key == "Replacements" {
			return nil, nil, fmt.Errorf("engine: %s: %s$ not resolvable yet", api, key)
		}
		var subs []compile.SubRef
		for _, sub := range a.Params.Subs {
			if strings.EqualFold(sub.Key, key) {
				subs = append(subs, sub)
			}
		}
		names := 0
		for _, name := range strings.Split(raw, ",") {
			if strings.TrimSpace(name) != "" {
				names++
			}
		}
		if len(subs) != names {
			return nil, nil, fmt.Errorf("engine: %s: %s$ %q grants a trait that regrants itself, not resolvable yet", api, key, raw)
		}
		for _, sub := range subs {
			switch {
			case d != "Perpetual":
				timed = append(timed, sub.Ability)
			case key == "Replacements" && !perpetualReplacementSupported(sub.Ability):
				return nil, nil, fmt.Errorf("engine: %s: Replacements$ %q is not an enters-tapped replacement, not resolvable yet", api, raw)
			default:
				perpetual = append(perpetual, sub.Ability)
			}
		}
	}
	return perpetual, timed, nil
}

// animateAbilityGrants reads Abilities$: the compiled activated abilities the
// resolution grants (AnimateEffect.java:116-118, AnimateEffectBase.java:
// 161-163), nil when it names none. They ride the animateRecord, so they last
// as its characteristic changes do -- until end of turn, Duration$ Permanent,
// or a host-bound AsLongAsControl/UntilHostLeavesPlay ending (Quicksmith
// Rebel's "for as long as you control Quicksmith Rebel"). A Perpetual grant
// never reaches here: animateDuration and the Perpetual characteristic check
// refuse it, its zone-surviving store holding triggers only. A name compile
// left unfollowed (a grant that regrants itself) is refused, as
// animateTriggerGrants does.
func animateAbilityGrants(a *Ability, api string) ([]*compile.Ability, error) {
	raw, ok := a.Params.Param("Abilities")
	if !ok {
		return nil, nil
	}
	var out []*compile.Ability
	for _, sub := range a.Params.Subs {
		if sub.Key == "Abilities" {
			out = append(out, sub.Ability)
		}
	}
	names := 0
	for _, name := range strings.Split(raw, ",") {
		if strings.TrimSpace(name) != "" {
			names++
		}
	}
	if len(out) != names {
		return nil, fmt.Errorf("engine: %s: Abilities$ %q grants an ability that regrants itself, not resolvable yet", api, raw)
	}
	return out, nil
}

// perpetualReplacementSupported reports whether r is the one granted
// replacement shape something reads: "this permanent enters tapped" (Event$
// Moved, ValidCard$ Card.Self, Destination$ Battlefield, ReplaceWith$ a DB$
// Tap), which checkMovedReplacement applies from the entering card's own
// grant rows. Any other granted replacement would be inert, so it is refused
// when granted (GO-7).
func perpetualReplacementSupported(r *compile.Ability) bool {
	if !strings.EqualFold(r.Name, "Moved") {
		return false
	}
	if v, ok := r.Param("ValidCard"); !ok || v != "Card.Self" {
		return false
	}
	if v, ok := r.Param("Destination"); !ok || v != "Battlefield" {
		return false
	}
	for _, sub := range r.Subs {
		if strings.EqualFold(sub.Key, "ReplaceWith") {
			return strings.EqualFold(sub.Ability.Name, "Tap")
		}
	}
	return false
}

// grantPerpetualTriggers gives each of cards one grant row holding triggers,
// in whatever zone the card is -- AnimateEffect.java:166-178 skips only a
// phased-out card, and Racketeer Boss grants to cards in hand. Every row
// shares the resolution's one new timestamp (AnimateEffect.java:57) as its
// id; amounts is the granting face's SVars (grantedTriggers' own doc
// comment).
func (g *Game) grantPerpetualTriggers(cards []CardID, traits []*compile.Ability, amounts map[string]expr.Amount) {
	g.timestamp++
	id := g.timestamp
	var triggers, replacements []*compile.Ability
	for _, t := range traits {
		if t.Record == compile.Replacement {
			replacements = append(replacements, t)
		} else {
			triggers = append(triggers, t)
		}
	}
	for _, cid := range cards {
		c := g.Card(cid)
		if c.IsPhasedOut() {
			continue
		}
		c.grants = c.withGrant(grantedTriggers{id: id, triggers: triggers, replacements: replacements, amounts: amounts})
	}
}
