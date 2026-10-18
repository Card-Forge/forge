// Continuous effects: CR 613, four layers deep so far. Layer 7b/7c's own
// power/toughness keys (SetPower$/SetToughness$/AddPower$/AddToughness$) are
// the single most common real corpus shape (2,192 of 2,426 real S:Mode$
// Continuous lines carrying one of these four keys, port-log/game-state.md's
// "Continuous effects" section); Layer 4's own type-changing keys (AddType$/
// RemoveType$, applyContinuousType), Layer 5's own color-changing keys
// (AddColor$/SetColor$, applyContinuousColor) and Layer 6's own
// ability-granting key (AddKeyword$, applyContinuousKeyword below -- the
// single largest real slice of all four, 1,710 of 1,875 real lines) are the
// next three, Layers 4-6 through continuouslayers.go's gate and affected
// set (AffectedDefined$/AffectedZone$/Affected$).
//
// Ported from
// forge-game/src/main/java/forge/game/staticability/StaticAbilityContinuous.java's
// applyContinuousAbility/getAffectedCards.

package engine

import (
	"fmt"
	"slices"
	"sort"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cost"
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
// FatefulHour (3 -- the controller's own life at 5 or below), Blessing (9 -- Player.hasBlessing) and Monarch (2 --
// Player.isMonarch, Game.Monarch) and EnduringStory (Player.hasEnduringStory,
// set by assignEnduringStories). Not resolved:
// MaxSpeed (40) -- Alchemy's speed counter, a
// mechanic this port tracks no state for anywhere yet, so (like an
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
	case "Blessing":
		return g.Player(controller).Blessing
	case "Monarch":
		return g.Monarch() == controller
	case "EnduringStory":
		return g.Player(controller).EnduringStory
	default:
		return false
	}
}

// layerStatic is one static ability in play, as a layer applier walks it:
// the host, the face it is printed on and its index there (Layer 3's
// textChange key, Layer 8's MayPlay grant key), the definition it was read
// from (Layer 3 swaps the host's own Def mid-walk) and that face's amounts.
type layerStatic struct {
	host    CardID
	def     *compile.Card
	face    int
	index   int
	s       *compile.Ability
	amounts map[string]expr.Amount
}

// continuousStatics is every static ability printed on every trait host (a
// host that has lost them still lists them: a static already applying in an
// earlier layer keeps applying, CR 613.6 -- each applier asks staticLive) in Java's
// effectOrder (GameAction.java:82-83): characteristic-defining lines first
// (CR 613.3), then by the host's timestamp (CR 613.7), ties kept in the
// battlefield's own order. Each applier walks this order, so an Affected$
// set is evaluated after exactly the effects that precede it in this layer
// have applied, not after whichever hosts happened to sit earlier on the
// battlefield. Read fresh per layer: a Layer 3 text change swaps a host's
// Def, and later layers then see the gained statics (Java's toAdd list).
func continuousStatics(g *Game) []layerStatic {
	return gatherStatics(g, false)
}

// continuousStaticsAllZones is continuousStatics plus the Mode$ Continuous
// lines of cards outside the battlefield that function in the zone they sit
// in (offZoneStatics): a characteristic-defining ability "functions in every
// zone" (CR 604.3) and an EffectZone$ line functions in the zones it names. Every
// applier whose effects land on cards of every zone, or on a player or the
// game, walks this one; Layer 2 alone does not (applyContinuousControl).
func continuousStaticsAllZones(g *Game) []layerStatic {
	return gatherStatics(g, true)
}

func gatherStatics(g *Game, offZone bool) []layerStatic {
	var out []layerStatic
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			def := g.Card(host).Def
			if def == nil {
				continue
			}
			// Only the faces a card is showing (liveFaces): an untransformed
			// DFC's back face -- Invasion of Belenon's Belenon War Anthem --
			// has no static in play, as Java reads currentState alone
			// (triggerFaces reads them the same way).
			for fi, face := range def.Faces[:liveFaces(def)] {
				for si, s := range face.Statics {
					out = append(out, layerStatic{host: host, def: def, face: fi, index: si, s: s, amounts: face.Amounts})
				}
			}
			// Statics a Layer 6 AddStaticAbility$ granted this pass: face -1,
			// index running across the grants so a per-static key stays unique.
			n := 0
			for _, grant := range g.Card(host).traitGrants {
				for _, s := range grant.statics {
					out = append(out, layerStatic{host: host, def: def, face: -1, index: n, s: s, amounts: grant.amounts})
					n++
				}
			}
		}
	}
	if offZone {
		out = appendOffZoneStatics(g, out)
	}
	sort.SliceStable(out, func(i, j int) bool {
		ci, cj := hasParamOn(out[i].s, "CharacteristicDefining"), hasParamOn(out[j].s, "CharacteristicDefining")
		if ci != cj {
			return ci
		}
		return g.Card(out[i].host).Timestamp < g.Card(out[j].host).Timestamp
	})
	return out
}

// applyContinuousPT recomputes every battlefield permanent's own Layer
// 7a-7c PTEffects from scratch, from every real Mode$ Continuous S: line
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
// Every battlefield card's own PT is cleared first, then rebuilt -- safe
// because the one other source of a PTEffect, a resolved Pump effect
// (pumpeffect.go), is not rebuilt from a card script here at all: it re-adds
// its own duration-scoped record fresh every pass too, from Game.pumps
// rather than from a card's own Statics, via pumpPT (below), right after the
// clear and before the statics, as animatePT re-adds a resolved Animate.
//
// The three sublayers run one after another, each over every static, as
// GameAction.checkStaticAbilities does (GameAction.java:1120): 7a
// (characteristic-defining lines) in effectOrder with no search, 7b
// (SetPower$/SetToughness$) in dependency order (CR 613.8; MODIFYPT is not
// in CONTINUOUS_LAYERS_WITH_DEPENDENCY), 7c (AddPower$/AddToughness$) in
// effectOrder. A 7c line therefore never evaluates its Affected$ before a
// later-timestamped 7b line has applied, which one walk over all statics in
// timestamp order let it do. Layer 7d (a switch) is PT.Switched's, read by
// Card.Power/Toughness.
func applyContinuousPT(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).PT.Clear()
		}
	}
	// A characteristic-defining power/toughness functions in every zone
	// (Grist's 1/1 off the battlefield, a Tarmogoyf in hand), so a card off the
	// battlefield is rebuilt too.
	forEachOffBattlefieldCard(g, func(c *Card) { c.PT.Clear() })
	animatePT(g)
	pumpPT(g)
	statics := continuousStaticsAllZones(g)
	for _, ls := range statics {
		if hasParamOn(ls.s, "CharacteristicDefining") && g.staticLive(g.Card(ls.host), ls.s) {
			applyOneContinuousPT(g, g.Card(ls.host), ls.amounts, ls.s, LayerCharacteristic)
		}
	}
	applyInDependencyOrder(g, setPTStatics(statics), setPTLayerOps(g))
	for _, ls := range statics {
		if g.staticLive(g.Card(ls.host), ls.s) {
			applyOneContinuousPT(g, g.Card(ls.host), ls.amounts, ls.s, LayerModifyPT)
		}
	}
}

// setPTStatics is Layer 7b's set: the lines naming SetPower$/SetToughness$
// that are not characteristic-defining (StaticAbility.generateLayer,
// StaticAbility.java:171).
func setPTStatics(statics []layerStatic) []layerStatic {
	var out []layerStatic
	for _, ls := range staticsWithAny(statics, "SetPower", "SetToughness") {
		if !hasParamOn(ls.s, "CharacteristicDefining") {
			out = append(out, ls)
		}
	}
	return out
}

// applyOneContinuousPT applies the part of s that belongs to one sublayer --
// layer is LayerCharacteristic (a CharacteristicDefining$ line's
// SetPower$/SetToughness$, applied to host alone, applyOneCharacteristicDefiningPT
// below), LayerSetPT (SetPower$/SetToughness$) or LayerModifyPT
// (AddPower$/AddToughness$) -- to every battlefield permanent its own
// Affected$ valid-string matches, if s is a Mode$ Continuous line this slice
// can resolve. A line naming both Set and Add keys is applied in two calls,
// its affected set fixed by the first (staticAffected, CR 613.6).
//
// Not resolved, each for a specific reason (game-state.md's "Continuous
// effects" section has the corpus counts behind every number below):
//   - A line layerStaticApplies turns off: a Condition$ value this port has
//     no player-state for (MaxSpeed --
//     continuousConditionMet's own doc comment has the full account), an
//     IsPresent$/CheckSVar$ that compares false (a Level Up line applies
//     only at its own level), a host outside its EffectZone$.
//   - AffectedZone$ (24) -- a card outside the battlefield. AffectedDefined$
//     Self/Enchanted/Equipped/"AttachedBy Self" resolve (layerAffectedCards:
//     Pacifism-style auras, every Equipment's "equipped creature gets +N/+N");
//     any other AffectedDefined$ (a targeted or Remembered-driven set) skips
//     the line.
//   - A non-numeric, non-resolvable AddPower$/AddToughness$/SetPower$/
//     SetToughness$ -- a plain integer or a named SVar resolveAmount
//     (amount.go) evaluates resolves (ptParam, below); only a value it
//     cannot evaluate (a per-card power read, a context-prefixed head, ...: resolveAmount's
//     own doc comment) is skipped, per missing dimension rather than per
//     whole line -- a real corpus line naming both a resolvable and an
//     unresolvable dimension together is not a shape worth losing the
//     resolvable half over.
func applyOneContinuousPT(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability, layer StaticAbilityLayer) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if _, ok := s.Param("AffectedZone"); ok {
		return
	}
	if !layerStaticApplies(g, host, amounts, s) {
		return
	}
	if _, ok := s.Param("CharacteristicDefining"); ok {
		if layer == LayerCharacteristic {
			applyOneCharacteristicDefiningPT(g, host, amounts, s)
		}
		return
	}
	_, hasDefined := s.Param("AffectedDefined")
	_, hasAffected := s.Param("Affected")
	if !hasAffected && !hasDefined {
		return
	}
	var addP, addT, setP, setT int
	var hasAddP, hasAddT, hasSetP, hasSetT bool
	switch layer {
	case LayerSetPT:
		setP, hasSetP = ptParam(g, amounts, host, s, "SetPower")
		setT, hasSetT = ptParam(g, amounts, host, s, "SetToughness")
	case LayerModifyPT:
		addP, hasAddP = ptParam(g, amounts, host, s, "AddPower")
		addT, hasAddT = ptParam(g, amounts, host, s, "AddToughness")
	default:
		return
	}
	if !hasAddP && !hasAddT && !hasSetP && !hasSetT {
		return
	}

	// AffectedDefined$ (Enchanted, Equipped, Self, "AttachedBy Self") names
	// the cards layerAffectedCards resolves, Affected$ then filtering them;
	// without it every battlefield permanent matching Affected$ is affected.
	ids, ok := g.staticAffected(host, s)
	if !ok {
		return
	}
	for _, id := range ids {
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
// amountpaid.go): all 374 of the corpus's real "*" CDA power/toughness
// dimensions on a card that stays on the battlefield resolve
// (TestCharacteristicDefiningCorpusFloor) -- the Count$Valid family with or
// without a doXMath
// suffix or a handlePaid property (Tarmogoyf's CardTypes, GreatestCardManaCost,
// ...), SVar$/Number$, Domain, YourLifeTotal, Devotion, Chroma, CardCounters,
// NumInAllHands, ChosenNumber, YouDrewThisTurn, OppGreatestLifeTotal and
// PlayerCountOpponents$HighestCardsInHand. A dimension that does not resolve
// is left off the effect (HasPower/HasToughness false), so a printed "*"
// stays unresolvable rather than reading as zero.
//
// ExcludeZone$ (Grist, the Hunger Tide's "isn't on the battlefield" 1/1) skips
// host entirely while it sits in one of the named zones
// (layerAffectedCards), and the walk is the all-zones one, so the line applies
// to host in hand, library, graveyard, exile or on the stack.
func applyOneCharacteristicDefiningPT(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) {
	if ids, ok := layerAffectedCards(g, host, s); !ok || len(ids) == 0 {
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

// applyContinuousType recomputes every card's own Layer 4 TypeMod effects
// from scratch, from every real Mode$ Continuous S: line currently in play
// -- applyContinuousPT's own reasoning applies identically here: Java's own
// applyContinuousAbility runs fresh from GameAction.checkStateEffects every
// state-based-action pass, not stored and incrementally updated, so a
// type-granting effect (an anthem-shaped "creatures you control are
// Zombies") has to reach a creature that enters after it and stop the
// instant it itself leaves. Cards off the battlefield are cleared too: an
// AffectedZone$ line reaches them (forEachOffBattlefieldCard,
// continuouslayers.go).
func applyContinuousType(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).TypeMod.Clear()
		}
	}
	forEachOffBattlefieldCard(g, func(c *Card) { c.TypeMod.Clear() })
	animateTypes(g)
	applyInDependencyOrder(g, staticsWithAny(continuousStaticsAllZones(g), typeLayerKeys...), typeLayerOps(g))
	applyChangelings(g)
}

// applyOneContinuousType is Layer 4 for one Mode$ Continuous line: when the
// line is on (layerStaticApplies) and names a type change layerTypeChange
// resolves -- AddType$/RemoveType$ with their runtime tokens, the
// Remove*Types$ category flags -- every card layerAffectedCards names gets
// that TypeEffect. A line layerTypeChange or layerAffectedCards cannot
// resolve does nothing: the whole line, never a part of it, since applying
// "is a Turtle" without the "loses its other creature types" it also asks
// for would leave the card with both, an answer worse than the coverage
// gap. port-log/game-state/layers-4-5-6.md has the corpus counts.
func applyOneContinuousType(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	effect, ok := layerTypeChange(g, host, s)
	if !ok || !layerStaticApplies(g, host, amounts, s) {
		return
	}
	affected, ok := g.staticAffected(host, s)
	if !ok {
		return
	}
	for _, id := range affected {
		g.Card(id).TypeMod.Add(effect)
	}
}

// applyContinuousColor recomputes every card's own Layer 5 ColorMod effects
// from scratch, from every real Mode$ Continuous S: line currently in play
// -- applyContinuousType's own reasoning, off-battlefield clear included.
func applyContinuousColor(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).ColorMod.Clear()
		}
	}
	forEachOffBattlefieldCard(g, func(c *Card) { c.ColorMod.Clear() })
	animateColors(g)
	for _, ls := range continuousStaticsAllZones(g) {
		if !g.staticLive(g.Card(ls.host), ls.s) {
			continue
		}
		applyOneContinuousColor(g, g.Card(ls.host), ls.amounts, ls.s)
	}
}

// applyOneContinuousColor is Layer 5 for one Mode$ Continuous line:
// applyOneContinuousType's shape, with layerColorChange reading
// AddColor$/SetColor$ (a literal color list, All, Colorless, or host's own
// ChosenColor).
func applyOneContinuousColor(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	effect, ok := layerColorChange(host, s)
	if !ok || !layerStaticApplies(g, host, amounts, s) {
		return
	}
	affected, ok := g.staticAffected(host, s)
	if !ok {
		return
	}
	for _, id := range affected {
		g.Card(id).ColorMod.Add(effect)
	}
}

// applyContinuousKeyword recomputes every card's own Layer 6 KeywordMod
// effects from scratch, from every real Mode$ Continuous S: line currently
// in play -- applyContinuousType's own reasoning, off-battlefield clear
// included.
func applyContinuousKeyword(g *Game) {
	for i := 1; i < len(g.cards); i++ {
		g.cards[i].traitGrants = nil
	}
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).KeywordMod.Clear()
		}
		g.Player(pid).KeywordMod.Clear()
	}
	forEachOffBattlefieldCard(g, func(c *Card) { c.KeywordMod.Clear() })
	animateKeywords(g)
	animateAbilities(g)
	pumpLayerKeywords(g)
	applyInDependencyOrder(g, staticsWithAny(continuousStaticsAllZones(g), keywordLayerKeys...), abilitiesLayerOps(g))
}

// applyOneContinuousKeyword is Layer 6's keyword half for one Mode$
// Continuous line: applyOneContinuousType's shape, with layerKeywordChange
// resolving AddKeyword$ (literal lines, verbatim -- HasKeyword (card.go)
// reads a granted "Ward:2" or "Protection:..." with keyword.Parse exactly as
// it reads a printed one -- and the runtime tokens Java substitutes) and
// RemoveKeyword$/RemoveAllAbilities$, then layerKeywordsFor finishing the
// tokens that name the affected card itself.
//
// A Player entity, not just a card, can be Affected$ too: Leyline of
// Sanctity's own `Affected$ You | AddKeyword$ Hexproof`
// (PlayerFactoryUtil.java's own precedent for a player-granted keyword)
// matches no card at all -- targetCandidates' own union reasoning
// (targeting.go) applies here too, probing both pools unconditionally
// rather than picking one by a spec's own shape. `change.add` (not
// layerKeywordsFor's per-card rewrite, which needs a *Card for
// CardColors/ConvertedManaCost -- no real corpus line pairs either token
// with a player-shaped Affected$) is what a player receives.
func applyOneContinuousKeyword(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	change, ok := layerKeywordChange(g, host, amounts, s)
	if !ok || !layerStaticApplies(g, host, amounts, s) {
		return
	}
	affected, ok := g.staticAffected(host, s)
	if !ok {
		return
	}
	for _, id := range affected {
		c := g.Card(id)
		c.KeywordMod.Add(KeywordEffect{
			Timestamp:      host.Timestamp,
			AddKeywords:    change.layerKeywordsFor(c),
			RemoveKeywords: change.remove,
			RemoveAll:      change.removeAll,
			CantHave:       change.cantHave,
		})
	}
	if spec, ok := s.Param("Affected"); ok {
		for _, pid := range g.Players() {
			if matched, _ := matchesPlayerSpec(g, pid, host.Controller(), host.ID, spec); matched && !g.playerIgnores(pid, host, s) {
				g.Player(pid).KeywordMod.Add(KeywordEffect{
					Timestamp:      host.Timestamp,
					AddKeywords:    change.add,
					RemoveKeywords: change.remove,
					RemoveAll:      change.removeAll,
				})
			}
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
//
// SetName$ (5 real lines: Ensoul Ring, Honest Work, Witness Protection, Psychic
// Paper, Awestruck Cygnet) shares the pass: Card.changedCardNames is one
// timestamp-ordered table that both AddNames$ and SetName$ write, an overwrite
// resetting the non-legendary-names flag and AddNames$ setting it
// (Card.hasNonLegendaryCreatureNames, Card.java:997-1007), so the statics apply
// in effectOrder here and the last write wins.
func applyContinuousNames(g *Game) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			g.Card(id).HasNonLegendaryCreatureNames = false
			g.Card(id).changedName = ""
		}
	}
	for _, ls := range continuousStatics(g) {
		if !g.staticLive(g.Card(ls.host), ls.s) {
			continue
		}
		applyOneContinuousNames(g, g.Card(ls.host), ls.amounts, ls.s)
	}
}

// setNameOf is the name a SetName$ line gives (StaticAbilityContinuous.java:647-655):
// ChosenName is the host's last NameCard pick, and an empty result writes
// nothing.
func setNameOf(host *Card, s *compile.Ability) (string, bool) {
	name, ok := s.Param("SetName")
	if !ok {
		return "", false
	}
	if name == "ChosenName" {
		picks := host.Memory.NamedCards()
		if len(picks) == 0 {
			return "", false
		}
		name = picks[len(picks)-1]
	}
	return name, name != ""
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
func applyOneContinuousNames(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	newName, setting := setNameOf(host, s)
	addNames, hasAdd := s.Param("AddNames")
	adding := hasAdd && strings.EqualFold(addNames, "AllNonLegendaryCreatureNames")
	if !setting && !adding {
		return
	}
	for _, key := range [...]string{"AffectedZone", "CharacteristicDefining"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	if !layerStaticApplies(g, host, amounts, s) {
		return
	}
	targets, ok := g.staticAffected(host, s)
	if !ok {
		return
	}
	for _, id := range targets {
		c := g.Card(id)
		if setting {
			c.changedName = newName
			c.HasNonLegendaryCreatureNames = false
		}
		if adding {
			c.HasNonLegendaryCreatureNames = true
		}
	}
}

// clearContinuousText ends every Layer 3 text change the previous pass
// applied (clearTextChange, card.go), before Layer 2 runs: Java's own
// checkStaticAbilities clears every static effect first
// (StaticEffects.clearStaticEffects) and only then collects the static
// abilities to apply from each card's own restored text, so a static the
// gained text carries never takes part in a layer ahead of Layer 3. Walks
// the whole arena rather than the battlefield enumeration, since a
// phased-out permanent is missing from the latter (Zone.Cards, ADR-0021)
// and would otherwise keep last pass's text forever.
func clearContinuousText(g *Game) {
	for i := range g.cards {
		g.cards[i].clearTextChange()
	}
}

// applyContinuousText is Layer 3 (CR 613.1c): every Mode$ Continuous static
// currently in play naming GainTextOf$ rewrites its affected permanent's
// text, applyOneContinuousText below. Called after applyContinuousControl
// and before every other applier (CheckStateBasedActions, action.go), CR
// 613.1's own order: the host's controller decides whose graveyard
// TopOfGraveyard reads, and Layers 4-7 then fold over the text-changed Def
// exactly the way they already fold over a copy effect's, picking up the
// gained text's own statics with no change to any of them -- Java's own
// checkStaticAbilities adds a text-gained static to every layer after the
// one that gained it (the toAdd list).
//
// The host's statics are read from a Def captured before the walk, since
// applying the line to the host itself (AffectedDefined$ Self, the one real
// shape) swaps that very Def mid-walk.
func applyContinuousText(g *Game) {
	for _, ls := range continuousStatics(g) {
		if !g.staticLive(g.Card(ls.host), ls.s) {
			continue
		}
		applyOneContinuousText(g, g.Card(ls.host), ls.s, textChange{owner: ls.def, face: ls.face, static: ls.index})
	}
	// Word substitution (ChangeText, ExchangeTextBox, ChangeColorWordsTo$)
	// folds over whatever text GainTextOf$ left, ADR-0039.
	applyTextWords(g)
}

// applyOneContinuousText is StaticAbilityContinuous.java's own TEXT-layer
// GainTextOf$ branch: the affected permanent gets the full text of the
// GainTextOf$ card -- name, mana cost, color, types, abilities, power and
// toughness (Java's own addChangedName/addChangedManaCost/addColorByText/
// addChangedCardTypesByText/addChangedCardTraitsByText/
// addChangedCardKeywordsByText/addNewPTByText, one call each) -- plus every
// GainTextAbilities$ ability, and loses every ability of its own
// (CardTraitChanges' own `e -> true` removal).
//
// Built for the corpus's one real line, Volrath's Shapeshifter:
//
//	AffectedDefined$ Self | GainTextOf$ TopOfGraveyard.Creature | GainTextAbilities$ VolrathDiscard
//
// The text read is the source card's own printed front face (its current
// state in the graveyard -- Java's first.getCurrentStateName()), never a
// back or adventure face: textChangedDef leaves every other face blank and
// SplitType zero, so a transforming DFC on top of the graveyard cannot make
// the permanent transform.
//
// Skipped, not guessed (GO-7): any AffectedDefined$ other than Self,
// Affected$/AffectedZone$/CharacteristicDefining$ alongside GainTextOf$, and
// a GainTextOf$ Defined other than TopOfGraveyard -- none is a real corpus
// shape. The other Layer 3 params StaticAbility.java:143 lists
// (ChangeColorWordsTo$, Incorporate$, ManaCost$) are not read here at all;
// AddNames$ is applyOneContinuousNames' own.
//
// key names s by its position in the host's own definition (textChange's
// own doc comment); applyOneContinuousText fills in the rest.
func applyOneContinuousText(g *Game, host *Card, s *compile.Ability, key textChange) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	gainTextOf, ok := s.Param("GainTextOf")
	if !ok {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	for _, key := range [...]string{"Affected", "AffectedZone", "CharacteristicDefining"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	if defined, _ := s.Param("AffectedDefined"); !strings.EqualFold(defined, "Self") {
		return
	}
	source, ok := topOfGraveyard(g, host, gainTextOf)
	if !ok {
		return
	}
	src := g.Card(source).Def
	if src == nil {
		return
	}
	key.from = src
	cached := host.text
	if cached.def != nil && cached.from == key.from && cached.owner == key.owner &&
		cached.face == key.face && cached.static == key.static {
		key.def = cached.def
		host.setTextChange(key)
		return
	}
	var gained []*compile.Ability
	for _, sub := range s.Subs {
		if strings.EqualFold(sub.Key, "GainTextAbilities") {
			gained = append(gained, sub.Ability)
		}
	}
	key.def = textChangedDef(src, gained)
	host.setTextChange(key)
}

// topOfGraveyard resolves AbilityUtils.getDefinedCards' own "TopOfGraveyard"
// Defined for a static ability: the last card of host's controller's
// graveyard (grave.getLast()), kept only if it matches the optional
// ".<valid>" filter after the head (getDefinedCards' own incR[1]
// restriction). false for an empty graveyard, a filtered-out top card, or a
// Defined other than TopOfGraveyard.
func topOfGraveyard(g *Game, host *Card, defined string) (CardID, bool) {
	head, filter, hasFilter := strings.Cut(defined, ".")
	if head != "TopOfGraveyard" {
		return NoCard, false
	}
	grave := g.Zone(Graveyard, host.Controller()).Cards()
	if len(grave) == 0 {
		return NoCard, false
	}
	top := grave[len(grave)-1]
	if hasFilter && !Matches(g, g.Card(top), valid.Parse(filter), host.Controller(), host.ID) {
		return NoCard, false
	}
	return top, true
}

// textChangedDef is the definition a GainTextOf$ change gives its permanent:
// src's own front face, with gained appended to its abilities. The ability
// slice is copied before appending, because src is shared by every game in
// the process (ADR-0007) and appending into its spare capacity would write
// into all of them at once. Every other slice the face holds is shared
// read-only, as any Def's is.
func textChangedDef(src *compile.Card, gained []*compile.Ability) *compile.Card {
	face := src.Faces[0]
	face.Abilities = append(append(make([]*compile.Ability, 0, len(face.Abilities)+len(gained)), face.Abilities...), gained...)
	out := &compile.Card{Filename: src.Filename, Name: src.Name}
	out.Faces[0] = face
	return out
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
	pumpLayerKeywords(g)
	pumpPT(g)
}

// livePumps calls fn for every pump record whose card is on the
// battlefield and phased in.
func livePumps(g *Game, fn func(c *Card, p *pumpRecord)) {
	for i := range g.pumps {
		p := &g.pumps[i]
		if p.OnPlayer {
			continue
		}
		c := g.Card(p.Card)
		if c.Zone != Battlefield || c.IsPhasedOut() {
			continue
		}
		fn(c, p)
	}
}

// pumpLayerKeywords is applyPumpEffects' Layer 6 half, called at the start of
// applyContinuousKeyword.
func pumpLayerKeywords(g *Game) {
	for _, p := range g.pumps {
		if p.OnPlayer && len(p.Keywords) > 0 {
			g.Player(p.Player).KeywordMod.Add(KeywordEffect{Timestamp: p.Timestamp, AddKeywords: p.Keywords})
		}
	}
	livePumps(g, func(c *Card, p *pumpRecord) {
		if len(p.Keywords) > 0 {
			c.KeywordMod.Add(KeywordEffect{Timestamp: p.Timestamp, AddKeywords: p.Keywords})
		}
	})
}

// pumpPT is applyPumpEffects' Layer 7c half, called at the start of
// applyContinuousPT.
func pumpPT(g *Game) {
	livePumps(g, func(c *Card, p *pumpRecord) {
		if p.Power != 0 || p.Toughness != 0 {
			c.PT.Add(PTEffect{Layer: LayerModifyPT, Timestamp: p.Timestamp, Power: p.Power, Toughness: p.Toughness})
		}
		if p.Switched {
			c.PT.AddSwitch()
		}
	})
}

// pumpHiddenKeywords puts every live pump record's hidden lines
// (Card.addHiddenExtrinsicKeywords, PumpEffect.java:71-73) back on its card.
// The record is the state; c.hiddenKeywords is rebuilt from scratch every pass
// (clearHiddenKeywords), so a line written there directly would vanish on the
// next one. Called from applyContinuousRules.
func pumpHiddenKeywords(g *Game) {
	livePumps(g, func(c *Card, p *pumpRecord) {
		c.hiddenKeywords = append(c.hiddenKeywords, p.Hidden...)
	})
}

// endNextUntapStepKeyword is Untap.doUntap's cleanup (Untap.java:157-161):
// once the active player's permanents have untapped, "This card doesn't untap
// during your next untap step." comes off every pump record on a permanent
// that player controls -- tapped or not, whoever made the record. A record
// left granting nothing is dropped. The record's Hidden slice is replaced,
// never edited in place: Game.Clone copies g.pumps shallowly, so a clone
// shares it.
func (g *Game) endNextUntapStepKeyword() {
	kept := g.pumps[:0]
	for _, r := range g.pumps {
		if !r.OnPlayer && slices.Contains(r.Hidden, untapNextStepKeyword) {
			c := g.Card(r.Card)
			if c.Zone == Battlefield && !c.IsPhasedOut() && c.Controller() == g.activePlayer {
				var hidden []string
				for _, l := range r.Hidden {
					if l != untapNextStepKeyword {
						hidden = append(hidden, l)
					}
				}
				r.Hidden = hidden
				if r.Power == 0 && r.Toughness == 0 && len(r.Keywords) == 0 && len(r.Hidden) == 0 && !r.Switched {
					continue
				}
			}
		}
		kept = append(kept, r)
	}
	g.pumps = kept
}

// keywordTokens reads key (GainControl's AddKWs$; Pump's KW$ checks each token
// with keywordTokenDynamic instead; a static
// AddKeyword$ goes through layerKeywordChange) as its " & "-separated list of
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
		if keywordTokenDynamic(tok) {
			return nil, false
		}
	}
	return tokens, true
}

// keywordTokenDynamic reports whether one keyword token carries a
// dynamic-value marker (keywordTokens' list).
func keywordTokenDynamic(tok string) bool {
	for _, marker := range [...]string{
		"ChosenColor", "ChosenType", "ChosenNumber", "ChosenPlayer", "ChosenName",
		"ChosenEvenOdd", "chosenEvenOdd", "AllColors", "allColors", "CommanderColorID",
		"ColorsYouCtrl", "colorsYouCtrl", "YourBasic",
	} {
		if strings.Contains(tok, marker) {
			return true
		}
	}
	return false
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
// AdjustLandPlays$ Read Player.HandSizeLimit/LandPlayLimit (player.go). The
// same walk recomputes every card's own AddHiddenKeyword$ grants
// (applyOneContinuousHiddenKeyword), Java's own RULES-layer param, after
// the hidden lines a live Pump/PumpAll record holds (pumpHiddenKeywords).
func applyContinuousRules(g *Game) {
	for _, pid := range g.Players() {
		g.Player(pid).Rules.Clear()
	}
	clearHiddenKeywords(g)
	pumpHiddenKeywords(g)
	g.mayPlay = nil
	for _, ls := range continuousStaticsAllZones(g) {
		if !g.staticLive(g.Card(ls.host), ls.s) {
			continue
		}
		h := g.Card(ls.host)
		applyOneContinuousRules(g, h, ls.amounts, ls.s)
		grantIgnoreEffect(g, h, ls.amounts, ls.s)
		applyOneContinuousHiddenKeyword(g, h, ls.s)
		applyOneContinuousMayPlay(g, h, ls.amounts, ls.s, ls.index)
	}
}

// applyOneContinuousRules is Layer 8: s applies to every player its own
// Affected$ spec matches (matchesPlayerSpec, valid.go -- the identical
// dispatch every other player-shaped Affected/ValidPlayer/ValidActivatingPlayer
// check in this port already reuses, applied here against a static
// ability's Affected$ rather than a trigger's own player-shaped param), if
// s is a Mode$ Continuous line naming SetMaxHandSize$, RaiseMaxHandSize$,
// AdjustLandPlays$ and/or one of the four vote params in a shape
// rulesEffect (below) can resolve.
//
// Not resolved, each for a specific reason:
//   - AffectedDefined$/AffectedZone$/CharacteristicDefining$/an unresolved
//     Condition$ value -- applyOneContinuousPT's own skip reasons (a
//     CharacteristicDefining line makes no sense for a player-facing effect
//     anyway). The one real line pairing Condition$ Delirium with
//     SetMaxHandSize$ (Winter, Misanthropic Guide) applies: its
//     `Number$7/Minus.X` over a `Count$ValidGraveyard ...$CardTypes` X
//     resolves through resolveAmount (amount.go, amountpaid.go).
//   - MayPlay$/MayLookAt$ are not this function's: MayPlay$ is a per-card
//     grant (applyOneContinuousMayPlay, below) and MayLookAt$ changes no
//     state in an omniscient engine.
//   - The vote params (AdditionalVote$, AdditionalOptionalVote$,
//     AdditionalVillainousChoice$, ControlVote$) do resolve here, into
//     RulesEffect fields Vote/VillainousChoice read, and so do
//     DeclaresAttackers$/DeclaresBlockers$ (1 S: line, 5 Effect SVars), into
//     the fields AttackDeclarer/BlockDeclarer read (ADR-0036), and
//     ControlOpponentsSearchingLibrary$ (1 real line), into the field
//     Game.SearchController reads (ADR-0040).
//   - IgnoreEffectCost$ (4) is not this function's: it is a granted ability
//     (compile's ignore-effect sub, ignoreeffect.go), and a player it frees is
//     left out of every player-facing pass (playerIgnores). AddHiddenKeyword$
//     is not this function's either: applyOneContinuousHiddenKeyword (below)
//     resolves it per card.
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
		if !recognized || !matched || g.playerIgnores(pid, host, s) {
			continue
		}
		g.Player(pid).Rules.Add(effect)
	}
}

// rulesEffect reads s's own SetMaxHandSize$/RaiseMaxHandSize$/
// AdjustLandPlays$/AdditionalVote$/AdditionalOptionalVote$/
// AdditionalVillainousChoice$/ControlVote$/DeclaresAttackers$/DeclaresBlockers$ params into one RulesEffect. "Unlimited" (Java's own
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
	for _, v := range [...]struct {
		key string
		dst *int
	}{
		{"AdditionalVote", &e.AdditionalVotes},
		{"AdditionalOptionalVote", &e.AdditionalOptionalVotes},
		{"AdditionalVillainousChoice", &e.AdditionalVillainousChoices},
	} {
		if _, ok := s.Param(v.key); !ok {
			continue
		}
		n, ok := ptParam(g, amounts, host, s, v.key)
		if !ok {
			return RulesEffect{}, false
		}
		*v.dst, hasEffect = n, true
	}
	if _, ok := s.Param("ControlVote"); ok {
		e.ControlVote, hasEffect = true, true
	}
	for _, v := range [...]struct {
		key string
		dst *PlayerID
	}{
		{"DeclaresAttackers", &e.DeclaresAttackers},
		{"DeclaresBlockers", &e.DeclaresBlockers},
		{"ControlOpponentsSearchingLibrary", &e.SearchControl},
	} {
		spec, ok := s.Param(v.key)
		if !ok {
			continue
		}
		// StaticAbilityContinuous.java:555-565: the first defined player, and
		// nothing at all when the param names nobody (AttackingPlayer outside
		// combat), which leaves the effect unrecorded.
		players, err := definedPlayers(g, host.Controller(), host.ID, spec, abilityRefs{})
		if err != nil {
			return RulesEffect{}, false
		}
		if len(players) > 0 {
			*v.dst, hasEffect = players[0], true
		}
	}
	return e, hasEffect
}

// clearHiddenKeywords drops every card's AddHiddenKeyword$ grants before
// applyContinuousRules rebuilds them. The whole arena, not the battlefield
// enumeration: a card that left play or phased out since the last pass
// must not keep a grant nothing re-derives.
func clearHiddenKeywords(g *Game) {
	for i := range g.cards {
		g.cards[i].hiddenKeywords = nil
	}
}

// hiddenKeywordRead reports whether line is an AddHiddenKeyword$ line (or a
// Pump/PumpAll KW$ "HIDDEN " line, pumpKeywords) something in this port reads
// (hasKeywordText/hasKeywordTextPrefix: block legality, block requirements,
// canAttackAtAll, untapBlocked).
func hiddenKeywordRead(line string) bool {
	switch line {
	case "CARDNAME can't block.", "CARDNAME can't attack.", "CARDNAME can't attack or block.",
		"All creatures able to block CARDNAME do so.", "CARDNAME must be blocked if able.",
		"CARDNAME can't attack alone.", "CARDNAME can only attack alone.",
		untapNextStepKeyword:
		return true
	}
	// "CARDNAME count as <name>." is read by a valid string's hasKeyword
	// property (Flame Burst's Count$ValidGraveyard Card.hasKeywordCARDNAME ...).
	return strings.HasPrefix(line, "CARDNAME count as ")
}

// applyOneContinuousHiddenKeyword is StaticAbilityContinuous.java's own
// RULES-layer AddHiddenKeyword$ (lines 322-323, 751-752): every " & "-split
// line goes onto each affected card's hidden keywords
// (Card.addHiddenExtrinsicKeywords), seen by Card.hasKeyword's exact-text
// shortcut (Card.java:4981) but not part of its keyword list -- the one
// thing that makes a hidden keyword differ from an AddKeyword$ one.
//
// The affected set is AffectedDefined$ Self/Enchanted/Equipped (the host, or
// what it is attached to -- 15 of the 19 real S: lines), filtered by
// Affected$ when present, or else every battlefield card Affected$ matches
// (the real Effect-SVar lines, AffectedZone$ Battlefield written out).
//
// Skipped whole, not applied partially (GO-7):
//   - a line naming a keyword nothing here reads (hiddenKeywordRead); every
//     hidden keyword string in the corpus is read: the untap step
//     (untapBlocked), the attack-alone pair (attackAloneViolation), block
//     legality, and "count as <name>." (the hasKeyword valid property).
//   - CharacteristicDefining$, any other AffectedDefined$, an unresolved
//     Condition$.
func applyOneContinuousHiddenKeyword(g *Game, host *Card, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	raw, ok := s.Param("AddHiddenKeyword")
	if !ok {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	if _, ok := s.Param("CharacteristicDefining"); ok {
		return
	}
	lines := strings.Split(raw, " & ")
	for i, l := range lines {
		lines[i] = strings.TrimSpace(l)
		if !hiddenKeywordRead(lines[i]) {
			return
		}
	}

	var targets []CardID
	affected, filtered := s.Param("Affected")
	var spec valid.Spec
	if filtered {
		spec = valid.Parse(affected)
	}
	if defined, ok := s.Param("AffectedDefined"); ok {
		switch {
		case strings.EqualFold(defined, "Self"):
			targets = []CardID{host.ID}
		case strings.EqualFold(defined, "Enchanted"), strings.EqualFold(defined, "Equipped"):
			if attached, ok := host.AttachedTo(); ok {
				targets = []CardID{attached}
			}
		default:
			return
		}
		for _, id := range targets {
			c := g.Card(id)
			if c.Zone != Battlefield || c.IsPhasedOut() {
				continue
			}
			if filtered && !Matches(g, c, spec, host.Controller(), host.ID) {
				continue
			}
			c.hiddenKeywords = append(c.hiddenKeywords, lines...)
		}
		return
	}
	if !filtered {
		return
	}
	// AffectedZone$ names the zones searched (the battlefield by default):
	// "CARDNAME count as <name>." lives on a graveyard card.
	ids, ok := layerAffectedCards(g, host, s)
	if !ok {
		return
	}
	for _, id := range ids {
		c := g.Card(id)
		if c.Zone == Battlefield && c.IsPhasedOut() {
			continue
		}
		c.hiddenKeywords = append(c.hiddenKeywords, lines...)
	}
}

// applyOneContinuousMayPlay is StaticAbilityContinuous.java's own RULES-layer
// MayPlay$ (lines 473-489, 892-911): every card in an AffectedZone$ zone
// that Affected$ matches gets a mayPlayGrant for the host's controller,
// read back by CastSpell/PlayLand (mayPlayOption, game.go).
//
// The static must be active where its host is (StaticAbility.zonesCheck):
// a battlefield host needs no EffectZone$ or EffectZone$ Battlefield/All;
// an Effect card's statics work from the Command zone whatever they name
// (EffectEffect.java). A host in any other zone is not walked at all
// (traitHosts), so the EffectZone$ Graveyard "cast this from your
// graveyard" lines stay a gap. Battlefield and Stack in AffectedZone$ grant
// nothing: nothing is cast from either.
//
// MayLookAt$ on the same line needs nothing: the engine is omniscient
// (lookateffect.go), so "may look at" changes no state.
func applyOneContinuousMayPlay(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability, index int) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if _, ok := s.Param("MayPlay"); !ok {
		return
	}
	// The static is on (StaticAbility.checkConditions): the host is in a zone
	// the line functions from, and its Condition$, IsPresent$ and CheckSVar$
	// chain hold.
	if !layerStaticApplies(g, host, amounts, s) {
		return
	}
	// Params that change what the grant allows or when it holds in a way
	// this does not model, so a line naming any of them grants nothing
	// (GO-7): CharacteristicDefining$. MayPlaySnowIgnoreColor$ (snow mana pays
	// a colored part) is read below; MayPlayText$ is only the option's label
	// (GameActionUtil.java:398) and changes nothing.
	for _, key := range [...]string{"CharacteristicDefining"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	// ValidSA$ names the spell ability the option is for: a plain Spell is
	// every cast; Spell.Blitz/Warp/Bestow/Mutate need an alternative cast
	// this port has no way to make, so that grant would never be used.
	if v, ok := s.Param("ValidSA"); ok && v != "Spell" {
		return
	}
	var afterStack valid.Spec
	_, hasAfterStack := s.Param("ValidAfterStack")
	if v, ok := s.Param("ValidAfterStack"); ok {
		// SpellAbility.isLegalAfterStack, checked once the spell is on the stack
		// (PlaySpellAbility.java:679): here against the card before the cast,
		// which differs only for a mana value that reads an X.
		rest, found := strings.CutPrefix(v, "Spell")
		if !found {
			return
		}
		afterStack = valid.Parse("Card" + rest)
	}
	affected, ok := s.Param("Affected")
	if !ok {
		return
	}
	rawZones, ok := s.Param("AffectedZone")
	if !ok {
		return
	}
	var zones []ZoneType
	for _, name := range strings.Split(rawZones, ",") {
		name = strings.TrimSpace(name)
		if strings.EqualFold(name, "All") {
			zones = append(zones, Hand, Graveyard, Library, Exile, Command)
			continue
		}
		z, ok := ZoneByName(name)
		if !ok {
			return
		}
		if z != Battlefield && z != Stack {
			zones = append(zones, z)
		}
	}
	grant := mayPlayGrant{LimitKey: mayPlayLimitKey{Host: host.ID, Index: index}}
	_, grant.WithoutManaCost = s.Param("MayPlayWithoutManaCost")
	_, grant.WithFlash = s.Param("MayPlayWithFlash")
	_, noZonePermission := s.Param("MayPlayDontGrantZonePermissions")
	grant.ZonePermission = !noZonePermission
	_, grant.AnyType = s.Param("MayPlayIgnoreType")
	_, grant.AnyColor = s.Param("MayPlayIgnoreColor")
	_, grant.SnowAnyColor = s.Param("MayPlaySnowIgnoreColor")
	if v, ok := s.Param("ReplaceGraveyard"); ok {
		if v != "Exile" {
			return
		}
		grant.ReplaceExile = true
	}
	if raw, ok := s.Param("RaiseCost"); ok {
		// A name the host defines as an SVar is its amount, generic mana
		// (GameActionUtil.java:374-380); anything else is a cost string.
		if n, isSVar := namedAmountOf(g, amounts, host, raw); isSVar {
			if n < 0 {
				return
			}
			raw = strconv.Itoa(n)
		}
		if _, ok := parseUnlessCost(raw); !ok {
			return
		}
		grant.RaiseText = raw
	}
	if raw, ok := s.Param("MayPlayAltManaCost"); ok {
		mc, err := mana.Parse(raw)
		if err != nil || mc.CountX() > 0 || !cost.Parse(raw).IsPureMana() {
			return
		}
		grant.AltCost, grant.HasAltCost = mc, true
	}
	if raw, ok := s.Param("MayPlayLimit"); ok {
		n, err := strconv.Atoi(raw)
		if err != nil || n <= 0 {
			return
		}
		grant.Limit = n
	}
	grantees := []PlayerID{host.Controller()}
	if spec, ok := s.Param("MayPlayPlayer"); ok {
		grantees = nil
		for _, pid := range g.Players() {
			if matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, spec); !recognized {
				return
			} else if matched {
				grantees = append(grantees, pid)
			}
		}
	}
	parsed := valid.Parse(affected)
	for _, player := range grantees {
		for _, z := range zones {
			for _, pid := range g.Players() {
				for _, id := range g.Zone(z, pid).Cards() {
					c := g.Card(id)
					if !Matches(g, c, parsed, host.Controller(), host.ID) {
						continue
					}
					if hasAfterStack && !Matches(g, c, afterStack, host.Controller(), host.ID) {
						continue
					}
					gr := grant
					gr.CardID, gr.Timestamp, gr.Grantee = id, c.Timestamp, player
					g.mayPlay = append(g.mayPlay, gr)
				}
			}
		}
	}
}

// namedAmountOf reports whether name is an SVar the host defines (runtime or
// compiled) and its amount. A defined SVar that does not resolve reports
// (-1, true): the caller drops the line rather than read the name as text.
func namedAmountOf(g *Game, amounts map[string]expr.Amount, host *Card, name string) (int, bool) {
	key := strings.ToLower(name)
	_, runtime := host.svars[key]
	_, compiled := amounts[key]
	if !runtime && !compiled {
		return 0, false
	}
	n, ok := resolveNamedAmount(g, amounts, host, name)
	if !ok {
		return -1, true
	}
	return n, true
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
	applyInDependencyOrder(g, staticsWithAny(continuousStatics(g), controlLayerKeys...), controlLayerOps(g))
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
// effect -- ChangeZoneEffect.java, not this layer at all -- shares the same
// param name), 43 name GainControl$ You and one (Fealty to the Realm)
// Player.isMonarch. Both are read the way StaticAbilityContinuous.java:597-600
// reads them: definedPlayers' first player gains control, nobody when it
// names none (no monarch). "You" is the host's controller -- `player` is
// `card.getController()` whenever sa is not a SpellAbility; Player.isMonarch
// is the getDefinedPlayers fallthrough over the players in turn order,
// filtered by Game.monarch (matchesPlayerSpec). A spec definedPlayers cannot
// read records a pending error (GO-7) rather than leaving the control silently
// unchanged.
//
// The affected set on these lines is AffectedDefined$ Enchanted (35 of 42
// Mode$ Continuous lines, Control Magic's own shape -- the Aura's host,
// resolved by layerAffectedCards), or an Affected$ valid-string (7:
// Permanent/Creature), the identical evaluation applyOneContinuousPT's own
// Matches call uses.
func applyOneContinuousControl(g *Game, host *Card, s *compile.Ability) {
	if !strings.EqualFold(s.Name, "Continuous") {
		return
	}
	if !continuousConditionMet(g, host, s) {
		return
	}
	for _, key := range [...]string{"AffectedZone", "CharacteristicDefining"} {
		if _, ok := s.Param(key); ok {
			return
		}
	}
	gain, ok := s.Param("GainControl")
	if !ok {
		return
	}
	gainers, err := definedPlayers(g, host.Controller(), host.ID, gain, abilityRefs{})
	if err != nil {
		g.recordPendingError(fmt.Errorf("engine: %s: Continuous GainControl$: %w", host.Def.Name, err))
		return
	}
	if len(gainers) == 0 {
		return
	}
	gainer := gainers[0]
	// AffectedDefined$ Enchanted/Equipped/Self (Control Magic's own shape since
	// the upstream move off Affected$ ...EnchantedBy, #11932) or an Affected$
	// valid string: staticAffected, the resolver Layers 4-8 share. An
	// unresolvable defined set skips the line (GO-7).
	if _, ok := s.Param("AffectedDefined"); !ok {
		if _, ok := s.Param("Affected"); !ok {
			return
		}
	}
	ids, ok := g.staticAffected(host, s)
	if !ok {
		return
	}
	for _, id := range ids {
		if g.Card(id).Zone == Battlefield {
			g.Card(id).ControlMod.Add(ControlEffect{Timestamp: host.Timestamp, Controller: gainer})
		}
	}
}

// applyOneContinuousTraits is Layer 6's trait half (ADR-0023) for one static: AddTrigger$/AddAbility$ write the
// compiled SVars they name onto each card they affect, and RemoveAllAbilities$/RemoveNonManaAbilities$ take away the
// card's own text and every trait granted before it (applyOneContinuousRemoval). AddStaticAbility$ and
// AddReplacementEffect$ ride the same grant row (traitGrant.statics/replacements) and reach every walk over the
// card's statics and replacements through traitFaces. It returns the statics it just granted, as the layer statics
// they now are on their new hosts: GameAction.checkStaticAbilities applies each at once in the layer that granted it
// and lists it for every later layer (GameAction.java:1152-1168), which continuousStatics does for the later ones.
func applyOneContinuousTraits(g *Game, host *Card, amounts map[string]expr.Amount, s *compile.Ability) []layerStatic {
	if !strings.EqualFold(s.Name, "Continuous") {
		return nil
	}
	var grant traitGrant
	for _, sub := range s.Subs {
		switch {
		case strings.EqualFold(sub.Key, "AddTrigger"):
			grant.triggers = append(grant.triggers, sub.Ability)
		case strings.EqualFold(sub.Key, "AddAbility"):
			grant.abilities = append(grant.abilities, sub.Ability)
		case strings.EqualFold(sub.Key, "AddStaticAbility"):
			grant.statics = append(grant.statics, sub.Ability)
		case strings.EqualFold(sub.Key, "AddReplacementEffect"):
			grant.replacements = append(grant.replacements, sub.Ability)
		}
	}
	removal := removalNone
	switch {
	case hasParamOn(s, "RemoveAllAbilities"):
		removal = removalAll
	case hasParamOn(s, "RemoveNonManaAbilities"):
		removal = removalNonMana
	}
	granting := len(grant.triggers) > 0 || len(grant.abilities) > 0 || len(grant.statics) > 0 || len(grant.replacements) > 0
	if !granting && removal == removalNone {
		return nil
	}
	if !layerStaticApplies(g, host, amounts, s) {
		return nil
	}
	affected, ok := g.staticAffected(host, s)
	if !ok {
		return nil
	}
	grant.amounts = amounts
	var added []layerStatic
	for _, id := range affected {
		c := g.Card(id)
		// CardTraitChanges.applySpellAbility and its siblings: the effect's own
		// removal first, over everything accumulated so far (the printed text
		// and every earlier grant), then its own additions.
		if removal != removalNone {
			c.removeTraits(removal)
		}
		if granting {
			// CardManaCost / ConvertedManaCost in a granted body name the card
			// that receives it (StaticAbilityContinuous.java:777-784, 842-847).
			cg := costSubstitutedGrant(grant, c)
			c.traitGrants = append(append([]traitGrant(nil), c.traitGrants...), cg)
			for _, st := range cg.statics {
				added = append(added, layerStatic{host: id, def: c.Def, face: -1, s: st, amounts: amounts})
			}
		}
	}
	return added
}

// removeTraits is a RemoveAllAbilities$ (every trait) or
// RemoveNonManaAbilities$ (every trait but mana abilities) effect reaching c:
// the printed text goes (abilityRemoval, read by printedTraitsRemoved) and so
// does every trait granted before it, keeping the granted mana abilities under
// the second form. A grant with a later timestamp is added after this runs.
func (c *Card) removeTraits(kind abilityRemoval) {
	if kind > c.abilityRemoval {
		c.abilityRemoval = kind
	}
	if kind == removalAll {
		c.traitGrants = nil
		return
	}
	var kept []traitGrant
	for _, g := range c.traitGrants {
		var mana []*compile.Ability
		for _, ab := range g.abilities {
			if strings.EqualFold(ab.Name, "Mana") {
				mana = append(mana, ab)
			}
		}
		if len(mana) > 0 {
			g.triggers, g.abilities, g.statics, g.replacements = nil, mana, nil, nil
			kept = append(kept, g)
		}
	}
	c.traitGrants = kept
}
