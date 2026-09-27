// Replacement effects: CR 614. The corpus's own 1,711 real R:Event$ lines
// split across three shapes this file resolves and everything else:
//
//   - Moved (969 lines) -- a permanent replacing its own "enters the
//     battlefield" event with "enters the battlefield tapped" (CR 614.1,
//     ReplaceMoved.java). 618 of those carry ReplaceWith$ pointing at a bare
//     `DB$ Tap` -- Java's own ETBTapped/LandTapped-named SVar convention --
//     and are the only Moved shape this file resolves (checkMovedReplacement,
//     below).
//   - Untap (158 lines) -- CR 502.3/614.17's own "doesn't untap during its
//     controller's untap step" (untapBlocked, below): 156 name
//     `Layer$ CantHappen`, the shape ReplaceUntap.canReplace ports directly;
//     the other 2 name ReplaceWith$ instead, a genuine substitution this file
//     does not resolve.
//   - DamageDone (218 lines) -- CR 614's own "prevent all of this damage"
//     (damagePrevented/damagePreventedPlayer, below): 72 name `Prevent$ True`,
//     ReplacementHandler's own unconditional-void dispatch for that value
//     (ReplaceDamage.canReplace plus the handler's own Prevent$ branch); the
//     other 146 name ReplaceWith$ -- most a real sub-ability substitution
//     (RemoveCounter/PutCounter/..., no single shape anywhere near Moved's
//     own 618-line concentration, not resolved), but two do: 16 of the 27
//     real `DB$ ReplaceDamage | Amount$ N` lines this file's own
//     `face.Replacements` walk can even reach (a partial "prevent N of that
//     damage" reduction) and 56 of the 59 real `DB$ ReplaceEffect | VarName$
//     DamageAmount | VarValue$ ...` lines (a computed replacement -- flat,
//     doubled/tripled/halved, or plus/minus an amount) both resolve, CR
//     616's own "Updated" outcome rather than a full substitution
//     (damageReplaced/damageReplacedPlayer, below).
//
// Draw and GainLife (39 and 21 real lines) have their own real content too:
// drawPrevented/gainLifePrevented resolve Prevent$ True (2 and 1 lines), and
// drawReplaced/gainLifeReplaced resolve ReplaceWith$ naming a plain,
// already-built leaf ability with no SubAbility$ of its own (3 of Draw's
// own 36 real ReplaceWith$ lines; 4 of GainLife's own 20, once
// ReplaceCount$LifeGained -- "the amount of life that would have been
// gained" -- resolves too). Every other Event$ value (Counter, ...) is a
// gap game-state.md's own trigger-firing-style account names, not a reason
// to have skipped the shapes that do resolve.
//
// Ported from
// forge-game/src/main/java/forge/game/replacement/{ReplacementHandler,ReplaceMoved,ReplaceUntap,ReplaceDamage,ReplacementEffect}.java,
// trimmed the same way trigger.go's own checkETBTriggers is: CR 616's own
// "more than one replacement effect could apply, the affected player
// chooses" procedure needs a PlayerController hook this port does not have,
// so it is not built at all. Moot for most outcomes this file produces
// (Tapped = true, blocked = true, prevented = true), since applying any of
// those more than once is a no-op, not a wrong answer -- the first real
// match found is applied (or, for Untap/DamageDone's own Prevent$ half,
// simply reported) directly and the search stops. Real, if narrow, for
// damageReplaced's/damageReplacedPlayer's own non-idempotent "prevent N of
// that damage" reduction (below) and drawReplaced's/gainLifeReplaced's own
// non-idempotent substitutions: two independent such permanents on one
// battlefield would only see the first found apply, not both compounding
// the way a real game lets the affected player choose to stack; not
// observable against a corpus with no two of any one such shape combined on
// one battlefield today.

package engine

import (
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// checkMovedReplacement is CR 614.1's own "look at the event before it
// happens" for a card that just moved onto the battlefield -- called, after
// the Copy layer, by enterBattlefieldReplacements (entersascopy.go), which
// every real "moves onto the battlefield" site this port has calls
// (permanentEffect.Resolve/attachEffect.Resolve -- castspell.go;
// Game.PlayLand -- land.go; moveByEffect -- zonemove.go), right after
// Game.Move and, like
// checkETBTriggers, not from Game.Move itself (Matches depends on game.go;
// game.go cannot depend back on anything that calls it, enginelint's own
// acyclic-parts rule). It runs BEFORE checkETBTriggers: a replacement
// changes the event itself, so an "enters tapped" permanent must already be
// tapped by the time a "when this enters" trigger looks at it, the same
// ordering CR 614.1 gives every replacement over CR 603's own triggers.
//
// Checked against two sets of Replacements, the identical split
// checkETBTriggers/otherETBTriggerMatches (above) already established:
// moved's own ("CARDNAME enters tapped," ValidCard$ Card.Self, 587 of 618
// real lines this resolves) and every OTHER permanent already on the
// battlefield ("creatures your opponents control enter tapped," 31 of 618).
// Unlike a trigger match, a replacement match here needs no APNAP ordering
// and no separate collect-then-push step: the one outcome this file
// produces, Tapped = true, is idempotent, so the first real match found (in
// either loop) is applied directly and the search stops -- CR 616's own
// "which one applies" choice has no observable answer to get wrong when
// every candidate would produce the identical result.
func (g *Game) checkMovedReplacement(moved CardID, origin ZoneType) {
	movedCard := g.Card(moved)
	if movedCard.Def != nil {
		for _, face := range movedCard.Def.Faces {
			for _, r := range face.Replacements {
				if shouldTap, matched := replacementTapsOnMove(g, r, movedCard, origin, movedCard.Controller(), moved, face.Amounts); matched {
					movedCard.Tapped = shouldTap
					return
				}
			}
		}
	}
	for _, pid := range g.Players() {
		for _, watcher := range g.Zone(Battlefield, pid).Cards() {
			if watcher == moved {
				continue
			}
			w := g.Card(watcher)
			if w.Def == nil {
				continue
			}
			for _, face := range w.Def.Faces {
				for _, r := range face.Replacements {
					if shouldTap, matched := replacementTapsOnMove(g, r, movedCard, origin, w.Controller(), watcher, face.Amounts); matched {
						movedCard.Tapped = shouldTap
						return
					}
				}
			}
		}
	}
}

// replacementRequirementsCheck ports ReplacementEffect.requirementsCheck --
// a general gate every replacement carries regardless of what Event$ it
// names, checked before its own shape-specific canReplace, mirroring
// triggerPhasesCheck's own role for triggers (trigger.go): the two are
// genuinely parallel Java methods (Trigger.phasesCheck /
// ReplacementEffect.requirementsCheck), not the same one reused, since
// Trigger and ReplacementEffect are sibling subclasses of TriggerReplacementBase
// rather than one inheriting from the other. Every consumer in this file
// (damagePreventionMatches, untapReplacementMatches, replacementTapsOnMove,
// below) had its own separate, narrower allow-list of extra params it
// tolerated before this landed; each now includes this call and widens its
// own allow-list to admit the keys this function itself reads (below),
// closing 7 of 10 previously-skipped real DamageDone|Prevent$ lines and 5 of
// 7 previously-skipped Untap|CantHappen lines for free, plus fixing a real,
// if narrow, wrong-firing bug on `replacementTapsOnMove`'s own side:
// archelos_lagoon_mystic.txt's own real "enters tapped" toggle names
// IsPresent$ Card.Self+tapped/+untapped restricting Archelos's own two
// replacement lines to only apply while ARCHELOS ITSELF is tapped/untapped
// respectively -- unchecked before this, both lines were reachable
// regardless of Archelos's own state (a genuine wrong answer, not a gap,
// since replacementTapsOnMove carried no allow-list at all to skip on
// instead of guessing).
//
// PlayerTurn$ (8 real R: lines combined across DamageDone/Draw/CreateToken/
// LifeReduced/TurnFaceUp, every one the literal value "True" -- 0 real lines
// use the Defined$-reference else-branch Java's own requirementsCheck also
// has, so only the literal-True branch is ported) checks
// game.getPhaseHandler().isPlayerTurn(hostController) directly.
// ActivePhases$ (1 real line, island_sanctuary.txt's own Draw shape) reuses
// phaseTriggerMatches (trigger.go, Mode$ Phase's own dispatch function) at
// its own key rather than Phase$'s, the identical general-purpose phase-list
// parser either way. triggerCommonRequirementsMet (trigger.go,
// CardTraitBase.meetsCommonRequirements's own port) is then called outright
// -- ReplacementEffect.requirementsCheck's own final line calls the
// identical Java method a Trigger's own performTest already does, so this
// port's identical shared function serves both for the same reason.
func replacementRequirementsCheck(g *Game, host *Card, amounts map[string]expr.Amount, r *compile.Ability) bool {
	if v, ok := r.Param("PlayerTurn"); ok {
		if !strings.EqualFold(v, "True") {
			// Java's own else-branch (a Defined$ player reference rather than
			// the literal "True") -- 0 real lines use it, so skipping rather
			// than resolving it is the honest answer, not a guess (GO-7).
			return false
		}
		if g.ActivePlayer() != host.Controller() {
			return false
		}
	}
	if _, ok := r.Param("ActivePhases"); ok {
		if !phaseTriggerMatches(r, "ActivePhases", g.ActivePhase()) {
			return false
		}
	}
	return triggerCommonRequirementsMet(g, host, amounts, r)
}

// replacementTapsOnMove reports whether r is a resolvable "enters tapped"
// replacement matching moved's own zone change (matched, the second return
// value) and, if so, whether it actually taps (shouldTap, the first): Event$
// Moved, an optional Origin$/Destination$ restriction (present on 2 and 624
// of the real ETBTapped-named lines respectively -- absence of either means
// unrestricted, ReplaceMoved.java's own hasParam guard), ValidCard$ matched
// against moved the identical way a trigger's own ValidCard$ is (Matches,
// valid.go), replacementRequirementsCheck (above), and a ReplaceWith$
// sub-ability tapAbilityResolvesTap (below) recognizes -- matched true
// whether or not the checkland-style condition inside that sub-ability
// actually holds, since CR 616's own "which replacement applies" choice is
// decided by ReplaceWith$ naming a resolvable shape at all, not by what that
// shape's own resolution produces.
func replacementTapsOnMove(g *Game, r *compile.Ability, movedCard *Card, origin ZoneType, hostController PlayerID, host CardID, amounts map[string]expr.Amount) (shouldTap, matched bool) {
	if !strings.EqualFold(r.Name, "Moved") {
		return false, false
	}
	if !replacementZoneMatches(r, "Destination", Battlefield) {
		return false, false
	}
	if !replacementZoneMatches(r, "Origin", origin) {
		return false, false
	}
	validCard, ok := r.Param("ValidCard")
	if !ok {
		return false, false
	}
	if !Matches(g, movedCard, valid.Parse(validCard), hostController, host) {
		return false, false
	}
	if !replacementRequirementsCheck(g, g.Card(host), amounts, r) {
		return false, false
	}
	for _, sub := range r.Subs {
		if strings.EqualFold(sub.Key, "ReplaceWith") {
			return tapAbilityResolvesTap(g, sub.Ability, g.Card(host), amounts)
		}
	}
	return false, false
}

// replacementZoneMatches reports whether r's key names zone among its
// comma-separated zone list, or carries no such param at all -- an absent
// Origin$/Destination$ is not a restriction (ReplaceMoved.java's own
// `hasParam` guard around each check), unlike trigger.go's own hasZone,
// which a trigger's own isETBTrigger/isDiesTrigger always require present.
func replacementZoneMatches(r *compile.Ability, key string, zone ZoneType) bool {
	v, ok := r.Param(key)
	if !ok {
		return true
	}
	for _, z := range strings.Split(v, ",") {
		if z == zone.String() {
			return true
		}
	}
	return false
}

// tapAbilityResolvesTap reports whether a is a resolvable ReplaceWith$ shape
// (recognized, the second return value) and, if so, whether it actually taps
// (shouldTap, the first): a bare `DB$ Tap` naming Defined$ Self or Defined$
// ReplacedCard -- Java's own distinction between "the card carrying this
// replacement" and "the card the replacement is actually about," identical
// here since this file only ever reaches a's own host through the card that
// is moving (never through a separate targeted/remembered reference) --
// optionally gated by SpellAbilityCondition's own ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$
// (subAbilityConditionMet, condition.go) and nothing else. ETB$ True,
// present on every real line, is read as part of a's own params but never
// checked: it exists in Java to mark the tap as happening as part of
// entering rather than a later, ordinary tap (relevant to a
// first-strike-of-untap-step check no card in this shape needs), not to gate
// whether the tap itself happens. 618 of the corpus's 624 real ETBTapped
// lines carry no Condition-family param at all (shouldTap always true once
// recognized); 140 more real DB$ Tap lines do -- LandTapped's own checkland/
// slowland "unless" shape (Rootbound Crag: "enters tapped unless you control
// a Mountain or a Forest") -- 116 of those resolving through
// subAbilityConditionMet, the rest (SubAbility$, ConditionDefined$,
// ConditionPlayerTurn$, ConditionPhases$) still recognized false: applying
// half of "enters tapped unless you control a Mountain" would be a wrong
// answer, not a partial one, so a's own unresolved-param guard
// (subAbilityUnresolvedParams, condition.go) refuses the whole ability
// rather than tapping unconditionally and guessing wrong (PORT-8/GO-7). Any
// param past db/defined/etb/the four Condition keys, this function's own
// separate switch -- skips (recognized false) for the identical reason.
// replacementActiveZones parses r's own ActiveZones$ -- the zone(s) its host
// itself must occupy for r to apply at all (ReplacementEffect's own
// zonesCheck, distinct from a trigger's TriggerZones$, though both are the
// identical comma-list-of-zone-names shape). Absent is Battlefield alone,
// the corpus's own overwhelming default for both families below (105 of 156
// real Untap|CantHappen lines name it explicitly; 41 of 72 real
// DamageDone|Prevent lines do); an unrecognized zone name skips the whole
// line rather than guessing (GO-7), the identical contract validCountZones
// (amount.go) already has for a Count$Valid<Zone> suffix.
func replacementActiveZones(r *compile.Ability) ([]ZoneType, bool) {
	v, ok := r.Param("ActiveZones")
	if !ok {
		return []ZoneType{Battlefield}, true
	}
	zones := make([]ZoneType, 0, 1)
	for _, name := range strings.Split(v, ",") {
		z, ok := ZoneByName(name)
		if !ok {
			return nil, false
		}
		zones = append(zones, z)
	}
	return zones, true
}

// hostInActiveZones reports whether hostZone is one of r's own ActiveZones$.
//
// An effect card's replacements are active in the Command zone alone,
// whatever ActiveZones$ says (EffectEffect.java's
// setActiveZone(EnumSet.of(ZoneType.Command))).
func hostInActiveZones(h *Card, r *compile.Ability, hostZone ZoneType) bool {
	if h.IsEffect {
		return hostZone == Command
	}
	zones, ok := replacementActiveZones(r)
	if !ok {
		return false
	}
	for _, z := range zones {
		if z == hostZone {
			return true
		}
	}
	return false
}

// replacementZones is every zone a replacement's own host is worth checking
// in, across every player: Battlefield alone would miss the 2 real
// Command-zone lines each of Untap|CantHappen and DamageDone|Prevent carries
// (an emblem- or effect-shaped host, not a permanent), so both callers below
// walk this pair rather than Battlefield alone.
var replacementZones = [...]ZoneType{Battlefield, Command}

// untapBlocked is CR 502.3/614.17's own "can't happen" replacement applied
// to CR 502's own untap step -- Card.canUntap's own cantHappenCheck,
// ReplaceUntap.canReplace ported directly. Walks every player's own
// Battlefield/Command looking for a replacement whose ValidCard$ matches
// card -- the first match found blocks the untap outright, CR 616's own
// "more than one could apply" choice producing the identical outcome
// (blocked) no matter which is picked, the same reasoning this file's own
// doc comment already gives for checkMovedReplacement.
//
// ValidStepTurnToController$ (154 of 156 real lines, always "You") is not
// checked: the one caller, untapStep (turn.go), only ever considers cards
// g.activePlayer already controls, so "the untapping player is this card's
// own controller" already holds by construction for every real value this
// param carries -- Java's own Untap.doUntap loop has the identical
// invariant for its own "self" untap pass (the only one this port models;
// doUntap's own "untap a card you don't control" branch,
// StaticAbilityUntapOtherPlayer, is not built, no card grants that
// permission yet).
func (g *Game) untapBlocked(card *Card) bool {
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if untapReplacementMatches(g, r, card, h.Controller(), host, z, face.Amounts) {
							return true
						}
					}
				}
			}
		}
	}
	return false
}

// untapReplacementMatches is ReplaceUntap.canReplace's own resolvable half:
// Event$ Untap, Layer$ CantHappen (156 of 158 real lines; the other 2 name
// ReplaceWith$ instead -- a genuine substitution, not a "can't happen," a
// different shape this file does not resolve), r's own host in one of its
// own ActiveZones$ (replacementActiveZones/hostInActiveZones, above),
// ValidCard$ matched against card the identical way checkMovedReplacement's
// own ValidCard$ already is, and replacementRequirementsCheck (above), which
// now folds in IsPresent$/CheckSVar$/SVarCompare$ generically -- closing 5 of
// the 7 real lines this shape used to skip for naming one of those three.
// Any param besides the ones real corpus lines pair with this shape
// (Secondary$, purely descriptive, plus whatever replacementRequirementsCheck
// itself reads) still skips the whole line rather than guessing (GO-7):
// EnduringStory$/AddSVar$ carry the remaining 2 of 156, each its own further
// restriction this file cannot evaluate.
func untapReplacementMatches(g *Game, r *compile.Ability, card *Card, hostController PlayerID, host CardID, hostZone ZoneType, amounts map[string]expr.Amount) bool {
	if !strings.EqualFold(r.Name, "Untap") {
		return false
	}
	layer, ok := r.Param("Layer")
	if !ok || !strings.EqualFold(layer, "CantHappen") {
		return false
	}
	for _, p := range r.Params {
		switch strings.ToLower(p.Key) {
		case "event", "layer", "description", "validcard", "validstepturntocontroller", "activezones", "secondary",
			"ispresent", "checksvar", "svarcompare":
		default:
			return false
		}
	}
	if !hostInActiveZones(g.Card(host), r, hostZone) {
		return false
	}
	validCard, ok := r.Param("ValidCard")
	if !ok {
		return false
	}
	if !Matches(g, card, valid.Parse(validCard), hostController, host) {
		return false
	}
	return replacementRequirementsCheck(g, g.Card(host), amounts, r)
}

// damagePrevented is CR 614's own "prevent all of this damage" shape
// (Prevent$ True) applied to a *Card target -- ReplaceDamage.canReplace's
// own resolvable half plus ReplacementHandler's own Prevent$ True dispatch
// (ReplacementResult.Prevented: nothing replaces the event, it simply does
// not happen). The two real damage-dealing call sites this port has
// (dealPermanentDamage/dealPlayerDamage, combatdamage.go) check this before
// marking any damage or emitting DamageDealt -- a prevented damage instance
// never happened, the same "look at the event before it happens" ordering
// this file's own doc comment already gives CR 614 over CR 603's own
// triggers.
func (g *Game) damagePrevented(source, target CardID, isCombat bool, amount int) bool {
	if isCombat && g.combatDamagePrevented {
		return true
	}
	targetCard := g.Card(target)
	toughness, hasToughness := targetCard.Toughness()
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !damagePreventionMatches(g, r, source, h.Controller(), host, z, isCombat, face.Amounts, amount, toughness, hasToughness) {
							continue
						}
						if validTarget, ok := r.Param("ValidTarget"); ok &&
							!Matches(g, targetCard, valid.Parse(validTarget), h.Controller(), host) {
							continue
						}
						return true
					}
				}
			}
		}
	}
	return false
}

// damagePreventedPlayer is damagePrevented's own player-target twin, ported
// for the identical reason checkDamageDoneTriggersToPlayer (trigger.go) is
// checkDamageDoneTriggersToCard's own: ValidTarget$ matched against a Player
// through matchesPlayerSpec rather than Matches.
func (g *Game) damagePreventedPlayer(source CardID, target PlayerID, isCombat bool, amount int) bool {
	if isCombat && g.combatDamagePrevented {
		return true
	}
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !damagePreventionMatches(g, r, source, h.Controller(), host, z, isCombat, face.Amounts, amount, 0, false) {
							continue
						}
						if validTarget, ok := r.Param("ValidTarget"); ok {
							matched, recognized := matchesPlayerSpec(g, target, h.Controller(), host, validTarget)
							if !recognized || !matched {
								continue
							}
						}
						return true
					}
				}
			}
		}
	}
	return false
}

// damagePreventionMatches is damagePrevented/damagePreventedPlayer's own
// shared half: Event$ DamageDone, Prevent$ True, r's own host in one of its
// own ActiveZones$, ValidSource$ matched against source the identical way
// damageDoneMatches' own ValidSource$ already is (trigger.go), IsCombat$
// agreeing with isCombat, and replacementRequirementsCheck (above), which
// now folds in PlayerTurn$/CheckSVar$/SVarCompare$/IsPresent$ generically --
// closing 7 of the 10 real lines this shape used to skip for naming one of
// those four (guardian_naga_banishing_coils.txt's own real "can't be dealt
// damage during your turn," PlayerTurn$ True, among them). Any param besides
// the ones real corpus lines pair with this shape (Secondary$, purely
// descriptive, plus whatever replacementRequirementsCheck itself reads)
// still skips the whole line. DamageAmount$ (1 of 72 -- callous_giant.txt's
// own "if a source would deal 3 or less damage to CARDNAME, prevent that
// damage") resolves too now, reusing damageAmountMatches (trigger.go, its
// own TriggerDamageDone.performTest-ported operator/operand split) against
// amount/toughness/hasToughness -- the ORIGINAL amount about to be dealt,
// the identical value a trigger's own DamageAmount$ checks once damage
// actually happens, just consulted here before this replacement decides
// whether to apply at all. The remaining 2 of 72 stay unresolved: one names
// ValidCause$/CauseIsSource$ together (a SpellAbility comparison Matches
// cannot evaluate), the other RelativeToSource$ (a GameEntity-vs-GameEntity
// relative match this port has no evaluator for) -- each its own further
// restriction this file cannot evaluate (GO-7).
func damagePreventionMatches(g *Game, r *compile.Ability, source CardID, hostController PlayerID, host CardID, hostZone ZoneType, isCombat bool, amounts map[string]expr.Amount, amount, toughness int, hasToughness bool) bool {
	if !strings.EqualFold(r.Name, "DamageDone") {
		return false
	}
	prevent, ok := r.Param("Prevent")
	if !ok || !strings.EqualFold(prevent, "True") {
		return false
	}
	for _, p := range r.Params {
		switch strings.ToLower(p.Key) {
		case "event", "prevent", "description", "validtarget", "activezones", "validsource", "iscombat", "secondary",
			"playerturn", "checksvar", "svarcompare", "ispresent", "damageamount":
		default:
			return false
		}
	}
	if !hostInActiveZones(g.Card(host), r, hostZone) {
		return false
	}
	if validSource, ok := r.Param("ValidSource"); ok && !Matches(g, g.Card(source), valid.Parse(validSource), hostController, host) {
		return false
	}
	if combat, ok := r.Param("IsCombat"); ok && strings.EqualFold(combat, "True") != isCombat {
		return false
	}
	if da, ok := r.Param("DamageAmount"); ok && !damageAmountMatches(da, amount, toughness, hasToughness) {
		return false
	}
	return replacementRequirementsCheck(g, g.Card(host), amounts, r)
}

// damageReplaced is CR 616's own "the event is replaced by a different
// one" outcome applied to CR 614's own damage family, ReplaceDamageEffect's
// own third outcome past Prevented/unaffected: DB$ ReplaceDamage | Amount$ N
// ("prevent N of that damage") REDUCES the incoming amount rather than
// skipping the event outright (damagePrevented's own bare Prevent$ True,
// above) -- Java's own ReplacementResult.Updated, the same DamageDealt event
// still happening with a smaller number, folding into the identical
// Replaced/"no damage at all" outcome only once the reduction reaches zero
// or below.
//
// Only the first matching line applies: CR 616's own general "more than one
// replacement effect could apply, the affected player chooses" ordering
// procedure is not built (the "Not ported yet" table's own row already
// names this gap), so two independent damage-reducing permanents on one
// battlefield would only see the first found reduce the hit, not both
// compounding the way a real game lets the player choose to stack. Not
// observable against a corpus with no two such permanents combined on one
// battlefield today.
//
// Of the corpus's own 39 real `DB$ ReplaceDamage` SVar definitions, only 27
// are ever named by a literal top-level `R:Event$ DamageDone` line at all --
// the same `face.Replacements` walk every other dispatch in this file uses,
// game-state.md's own "`Event$ DamageDone`" section has the other 12's own
// accounting (each blocked by an entirely different missing mechanism, not
// by anything specific to this shape). 16 of those 27 resolve end to end
// through this and replaceDamageEffect (replaceeffect.go): every real line naming
// a plain integer Amount$, no SubAbility$ of its own and no DivideShield$ (a
// shield's own remaining capacity split across more than one simultaneous
// instance of damage in the same resolution, a fold this dispatch has no
// state for) -- guardian_seraph.txt's/orbs_of_warding.txt's/the five
// Sphere-of-*.txt's own real "prevent N of that damage" lines among them. 2
// more of the 27 resolve their own card-target half only
// (reidane_god_of_the_worthy_valkmira_protectors_shield.txt's/
// plated_pegasus.txt's own `ValidTarget$ You,Permanent.YouCtrl`/
// `Permanent,Player` -- valid.Parse's own comma-as-OR already matches the
// `Permanent...` alternative through Matches below, but matchesPlayerSpec
// (damageReplacedPlayer, below), unlike valid.Parse, does not split a
// ValidTarget$ on comma, so "or dealt to you" itself never resolves -- a
// general ValidTarget$ comma dispatch across both matchers is not built).
// The remaining 9 name a named-SVar Amount$ this dispatch cannot resolve
// (`ShieldAmount`/`X`/`PaidAmount`/`AlchemicX`, each its own further
// mechanic -- a depleting shield counter, an X spent on the spell, mana
// paid, ...), one (rock_hydra.txt's) also chaining its own `SubAbility$`
// (the identical chained-target refusal `applyDrawReplacement` already
// gives, GO-7).
//
// A second real ReplaceWith$ shape resolves here too now: `DB$ ReplaceEffect
// | VarName$ DamageAmount | VarValue$ ...` (ReplaceEffect.java's own
// "amount" VarType branch, AbilityUtils.calculateAmount) -- CR 616's own
// "Updated" outcome again, but resizing the incoming amount by a computed
// expression (a flat replacement, a multiple, or an addition/subtraction)
// rather than ReplaceDamage's own flat "prevent N" -- raphael_the_muscle.txt's/
// gratuitous_violence.txt's own real "deals double damage" among them.
// replaceEffectEffect (replaceeffect.go) resolves 56 of the corpus's 59 real
// lines naming it with VarName$ DamageAmount specifically (12 more name
// VarName$ Affected/LifeGained/Number/Ignore instead -- an entirely
// different substitution, redirecting who is damaged or what else changes,
// not resizing the damage itself, out of scope for this dispatch).
//
// A third real shape resolves here too: `DB$ RemoveCounter`/`DB$ PutCounter`
// (applyDamageReplaceCounter, below) -- CR 616's own "Replaced" outcome this
// time, not "Updated": the damage does not happen AT ALL, a counter changes
// on some object instead (every "Phantom" creature's own "prevent that
// damage, remove a counter" among them, 25 of 31 real lines). Reported by
// returning amount 0 -- the identical "nothing left to mark" a full
// DB$ ReplaceDamage reduction already folds into, since neither caller
// distinguishes "zero because it was reduced to zero" from "zero because
// something else happened instead."
func (g *Game) damageReplaced(source, target CardID, isCombat bool, amount int) (int, EntityID, int) {
	targetCard := g.Card(target)
	toughness, hasToughness := targetCard.Toughness()
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !damageReplacementMatches(g, r, source, h.Controller(), host, z, isCombat, face.Amounts, amount, toughness, hasToughness) {
							continue
						}
						if validTarget, ok := r.Param("ValidTarget"); ok &&
							!Matches(g, targetCard, valid.Parse(validTarget), h.Controller(), host) {
							continue
						}
						if !damageRedirectAllowed(g, r, h, CardEntity(target)) {
							continue
						}
						for _, sub := range r.Subs {
							if !strings.EqualFold(sub.Key, "ReplaceWith") {
								continue
							}
							ev := replacementEvent{amountName: "DamageAmount", amount: amount, affected: CardEntity(target)}
							if g.runReplaceWith(nil, h, face.Amounts, sub.Ability, &ev) {
								return ev.amount, ev.redirect, ev.redirectAmount
							}
							if applyDamageReplaceCounter(g, host, CardEntity(target), sub.Ability, face.Amounts, amount) {
								return 0, NoEntity, 0
							}
						}
					}
				}
			}
		}
	}
	return amount, NoEntity, 0
}

// damageReplacedPlayer is damageReplaced's own player-target twin, the
// identical split damagePreventedPlayer already has from damagePrevented:
// ValidTarget$ matched against a Player through matchesPlayerSpec rather
// than Matches.
func (g *Game) damageReplacedPlayer(source CardID, target PlayerID, isCombat bool, amount int) (int, EntityID, int) {
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !damageReplacementMatches(g, r, source, h.Controller(), host, z, isCombat, face.Amounts, amount, 0, false) {
							continue
						}
						if validTarget, ok := r.Param("ValidTarget"); ok {
							matched, recognized := matchesPlayerSpec(g, target, h.Controller(), host, validTarget)
							if !recognized || !matched {
								continue
							}
						}
						if !damageRedirectAllowed(g, r, h, PlayerEntity(target)) {
							continue
						}
						for _, sub := range r.Subs {
							if !strings.EqualFold(sub.Key, "ReplaceWith") {
								continue
							}
							ev := replacementEvent{amountName: "DamageAmount", amount: amount, affected: PlayerEntity(target)}
							if g.runReplaceWith(nil, h, face.Amounts, sub.Ability, &ev) {
								return ev.amount, ev.redirect, ev.redirectAmount
							}
							if applyDamageReplaceCounter(g, host, PlayerEntity(target), sub.Ability, face.Amounts, amount) {
								return 0, NoEntity, 0
							}
						}
					}
				}
			}
		}
	}
	return amount, NoEntity, 0
}

// damageReplacementMatches is damagePreventionMatches' own ReplaceWith$
// sibling: Event$ DamageDone, ReplaceWith$ present instead of Prevent$ True,
// otherwise the identical gate (ActiveZones$/ValidSource$/IsCombat$/
// DamageAmount$/replacementRequirementsCheck) -- the general params real
// corpus lines pair with either shape are the same set. DamageAmount$ (2 of
// 146 -- forethought_amulet.txt's/divine_presence.txt's own "if a source
// would deal N or more damage..., it deals M damage instead," gating a flat
// VarValue$ replacement in replaceEffectEffect (replaceeffect.go) on the ORIGINAL amount
// meeting a threshold) reuses damageAmountMatches the identical way
// damagePreventionMatches' own new check does. AlwaysReplace$/ExecuteMode$
// (real on applyDamageReplaceCounter's own 25 lines, below) are allow-listed
// as pure no-ops: ReplacementHandler.java's own dispatch (line ~341) only
// reads AlwaysReplace$ when NoPreventDamage is set on the runParams -- a
// "damage can't be prevented" flag this port's own damage pipeline has no
// equivalent state for at all -- and ExecuteMode$'s own PerSource/PerTarget
// split only matters when Java batches more than one simultaneous damage
// instance into one replacement pass, something this port's own
// dealPermanentDamage/dealPlayerDamage never do (each call is already
// exactly one source and one target, GO-7's own "moot, not wrong" contract
// this file's own top-of-file doc comment already applies to Tapped/
// blocked/prevented).
func damageReplacementMatches(g *Game, r *compile.Ability, source CardID, hostController PlayerID, host CardID, hostZone ZoneType, isCombat bool, amounts map[string]expr.Amount, amount, toughness int, hasToughness bool) bool {
	if !strings.EqualFold(r.Name, "DamageDone") {
		return false
	}
	if _, ok := r.Param("ReplaceWith"); !ok {
		return false
	}
	for _, p := range r.Params {
		switch strings.ToLower(p.Key) {
		case "event", "replacewith", "description", "validtarget", "activezones", "validsource", "iscombat", "secondary",
			"playerturn", "checksvar", "svarcompare", "ispresent", "preventioneffect", "damageamount",
			"alwaysreplace", "executemode", "damagetarget":
		default:
			return false
		}
	}
	if !hostInActiveZones(g.Card(host), r, hostZone) {
		return false
	}
	if validSource, ok := r.Param("ValidSource"); ok && !Matches(g, g.Card(source), valid.Parse(validSource), hostController, host) {
		return false
	}
	if combat, ok := r.Param("IsCombat"); ok && strings.EqualFold(combat, "True") != isCombat {
		return false
	}
	if da, ok := r.Param("DamageAmount"); ok && !damageAmountMatches(da, amount, toughness, hasToughness) {
		return false
	}
	return replacementRequirementsCheck(g, g.Card(host), amounts, r)
}

// resolveReplaceCountAmount evaluates a DB$ ReplaceEffect's own
// VarValue$/CounterNum$ against original -- the pre-replacement value of
// whatever field wantBody names -- AbilityUtils.calculateAmount's own
// ReplaceCount$ branch (`root.getReplacingObject(AbilityKey.fromString(l[0]))`,
// the game's own replacing-object map; original stands in for it directly,
// since it IS that field's own pre-replacement value, already in scope at
// every one of this function's own callers), then AbilityUtils.doXMath for
// the operator suffix, when there is one. A plain integer is a flat
// replacement regardless of original (forethought_amulet.txt's/
// divine_presence.txt's own "deals N damage instead"). A named SVar must
// itself be a `ReplaceCount$<wantBody>` expression -- DamageAmount for
// replaceEffectEffect's/applyDamageReplaceCounter's own two callers
// (this dispatch never reached for any Event$ other than DamageDone there),
// LifeGained for replaceEffectEffect's own (below) -- either bare (no
// operator at all: doXMath's own `operators == null` identity, `original`
// unchanged -- lichenthrope.txt's/phytohydra.txt's/most of
// applyDamageReplaceCounter's own real CounterNum$ X/Y lines, the dominant
// real shape for THAT dispatch specifically) or carrying one of doXMath's
// own operators: Twice/Thrice/HalfDown (no operand) or Plus/Minus (a literal
// digit or a further-resolvable SVar operand, resolveNamedAmount reused the
// identical way replaceDamageEffect's own Amount$ already is) -- the
// five suffixed branches every real corpus line pairs with this shape
// (rhox_faithmender.txt's own real Twice, angel_of_vitality.txt's own real
// Plus.1 among LifeGained's own). Every other operator doXMath itself has
// (HalfUp, ThirdUp/Down, Negative, Times, Pow, Divide*, Mod, Abs,
// LimitMax/Min) carries 0 real lines here and is refused rather than guessed
// at (GO-7); so are hawkeye_young_avenger.txt's own Plus.Y operand and
// ojer_axonil_deepest_might_temple_of_power.txt's own bare VarValue$ (no
// ReplaceCount$ at all -- damage set equal to the host's own power, not
// measured off original at all), both Count$CardPower, a head resolveAmount
// has no evaluator for; and fated_firepower.txt's own VarValue$, which writes
// the ReplaceCount$ expression inline rather than naming an SVar, so the
// named lookup here never reaches it (its Plus.Y operand,
// Count$CardCounters.FIRE, itself resolves).
func resolveReplaceCountAmount(g *Game, amounts map[string]expr.Amount, host *Card, value string, original int, wantBody string) (int, bool) {
	if n, err := strconv.Atoi(value); err == nil {
		return n, true
	}
	amt, ok := amounts[strings.ToLower(value)]
	if !ok || amt.Kind != expr.Expression || !strings.EqualFold(amt.Head, "ReplaceCount") ||
		!strings.EqualFold(amt.Body, wantBody) {
		return 0, false
	}
	if amt.Op == nil {
		return original, true
	}
	switch amt.Op.Name {
	case "Twice":
		return original * 2, true
	case "Thrice":
		return original * 3, true
	case "HalfDown":
		return original / 2, true
	case "Plus", "Minus":
		if amt.Op.Operand == "" {
			return 0, false
		}
		operand, ok := resolveNamedAmount(g, amounts, host, amt.Op.Operand)
		if !ok {
			return 0, false
		}
		if amt.Op.Name == "Plus" {
			return original + operand, true
		}
		return original - operand, true
	}
	return 0, false
}

// applyDamageReplaceCounter runs a plain "DB$ RemoveCounter | ..." or
// "DB$ PutCounter | ..." ReplaceWith$ target directly --
// replaceDamageEffect's/replaceEffectEffect's own third sibling,
// but a different CR 616 outcome from either: Java's own
// ReplacementResult.Replaced (ReplacementHandler.java's own default, every
// ApiType past ReplaceDamage/ReplaceSplitDamage/ReplaceEffect/ReplaceToken/
// ReplaceMana) rather than Updated -- the ORIGINAL DamageDone event does not
// happen at all (no marking, no event, no trigger check), a counter changes
// on some object INSTEAD, the identical "different thing happens, not a
// smaller version of the same thing" shape drawReplaced's/gainLifeReplaced's
// own substitutions already have. Reports whether a recognized shape
// actually ran; damageReplaced/damageReplacedPlayer (below) return amount 0
// when it does, the same "nothing left to mark" outcome a full
// DB$ ReplaceDamage reduction already folds into.
//
// Defined$ resolves three ways: Self (the replacement's own host -- the
// dominant real shape, every "Phantom" creature's own "prevent that damage,
// remove a counter" among them), Equipped (panther_habit.txt's own real
// line, Card.AttachedTo() -- applyContinuousNames' own precedent, reused),
// and ReplacedTarget (the object the damage would have hit -- replacedTarget,
// threaded straight through from damageReplaced's/damageReplacedPlayer's own
// target parameter, soul_scar_mage.txt's own real "put -1/-1 counters on
// that creature instead" among them). CounterType$ reuses putCounterType
// (putcountereffect.go) outright -- RemoveCounter and PutCounter share the
// identical param. CounterNum$ resolves through resolveReplaceCountAmount
// (above, against "DamageAmount"), defaulting to 1 the identical way
// putCounterEffect's own CounterNum$ already does. SubAbility$ refuses
// outright, the identical chained-target refusal every other hand-run
// dispatch in this file already gives (5 real lines, underdark_beholder.txt's
// own "remove counters, then sacrifice if none left" among them).
//
// 25 of the corpus's own 31 real DamageDone lines naming DB$ RemoveCounter/
// PutCounter resolve end to end. 6 stay unresolved: the 5 chaining
// SubAbility$ above, and jared_carthalion_true_heir.txt's own real R: line
// naming CheckDefinedPlayer$ You.isMonarch -- no monarch mechanic to check
// (GainLife's own Player.isMonarch gap, port-log/game-state.md), skipped by
// damageReplacementMatches' own allow-list before this function is ever
// reached.
func applyDamageReplaceCounter(g *Game, host CardID, replacedTarget EntityID, a *compile.Ability, amounts map[string]expr.Amount, amount int) bool {
	remove := strings.EqualFold(a.Name, "RemoveCounter")
	if !remove && !strings.EqualFold(a.Name, "PutCounter") {
		return false
	}
	if _, ok := a.Param("SubAbility"); ok {
		return false
	}
	counterType, err := putCounterType(a)
	if err != nil {
		return false
	}
	n := 1
	if v, ok := a.Param("CounterNum"); ok {
		parsed, ok := resolveReplaceCountAmount(g, amounts, g.Card(host), v, amount, "DamageAmount")
		if !ok {
			return false
		}
		n = parsed
	}
	defined, ok := a.Param("Defined")
	if !ok {
		return false
	}
	var target EntityID
	switch defined {
	case "Self":
		target = CardEntity(host)
	case "Equipped":
		equipped, attached := g.Card(host).AttachedTo()
		if !attached {
			return false
		}
		target = CardEntity(equipped)
	case "ReplacedTarget":
		target = replacedTarget
	default:
		return false
	}
	delta := n
	if remove {
		delta = -n
	}
	if cid, ok := target.AsCard(); ok {
		g.Card(cid).Counters.Add(counterType, delta)
		emitCounterChanged(g.sink, host, target, counterType, delta)
		return true
	}
	if pid, ok := target.AsPlayer(); ok {
		g.Player(pid).Counters.Add(counterType, delta)
		emitCounterChanged(g.sink, host, target, counterType, delta)
		return true
	}
	return false
}

// drawPrevented is CR 121.4/614's own "prevent this draw" shape (Prevent$
// True) applied to CR 120.3's own "draw a card" event -- ReplaceDraw's own
// resolvable half plus ReplacementHandler's own Prevent$ True dispatch, the
// identical contract damagePreventedPlayer already has for a different
// Event$. DrawCards (turn.go) checks this before drawing each individual
// card -- Java's own Player.doDraw runs the identical Event$ Draw
// replacement check before ever looking at whether the library is empty, so
// a prevented draw does not count as CR 704.5b's own "attempted to draw from
// an empty library" either: this port's own DrawCards checks drawPrevented
// first and, when it reports true, never reaches the empty-library check at
// all for that card -- possessed_portal.txt's own real "if a player would
// draw a card, that player skips that draw instead" would otherwise still
// lose a player to state-based action 704.5b even though the draw it never
// got to attempt was the one thing keeping the library from mattering.
//
// 2 of the corpus's own 39 real Event$ Draw lines naming Prevent$ True
// resolve end to end (possessed_portal.txt's own bare form, ValidPlayer$
// Player with no further restriction; living_conundrum.txt's own
// IsPresent$ Card.YouOwn | PresentZone$ Library | PresentCompare$ EQ0,
// "if you would draw while your library has no cards," resolved through
// replacementRequirementsCheck's own triggerCommonRequirementsMet fold-in
// with no code of its own needed). Not resolved: Optional$ (1 of 3 real
// Prevent$ lines) -- a replacement effect's own interactive "may" confirm,
// Discard's own Optional$ (discardeffect.go) already documents the
// identical gap -- distinct from a trigger's own OptionalDecider$
// (Ability.Optional's own doc comment, ability.go), which resolves through
// Registry.Resolve/PlayerController.ConfirmOptionalTrigger now. The
// other 36 real Draw lines name ReplaceWith$ instead of Prevent$ -- a real
// substitution (DrawTwo/Dig/ExileTop/...), no single shape anywhere near
// Moved's own 618-line concentration, not resolved.
func (g *Game) drawPrevented(player PlayerID) bool {
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !drawPreventionMatches(g, r, host, z, face.Amounts) {
							continue
						}
						if validPlayer, ok := r.Param("ValidPlayer"); ok {
							matched, recognized := matchesPlayerSpec(g, player, h.Controller(), host, validPlayer)
							if !recognized || !matched {
								continue
							}
						}
						return true
					}
				}
			}
		}
	}
	return false
}

// drawPreventionMatches is drawPrevented's own shared half: Event$ Draw,
// Prevent$ True, r's own host in one of its own ActiveZones$, and
// replacementRequirementsCheck (above). Any param besides the ones real
// corpus lines pair with this shape skips the whole line rather than
// guessing (GO-7): Optional$/NotFirstCardInDrawStep$/
// FirstExtraCardDrawnThisTurn$/ValidCause$ together carry the 1 of 3 real
// Prevent$ Draw lines this shape would otherwise match but cannot resolve.
func drawPreventionMatches(g *Game, r *compile.Ability, host CardID, hostZone ZoneType, amounts map[string]expr.Amount) bool {
	if !strings.EqualFold(r.Name, "Draw") {
		return false
	}
	prevent, ok := r.Param("Prevent")
	if !ok || !strings.EqualFold(prevent, "True") {
		return false
	}
	for _, p := range r.Params {
		switch strings.ToLower(p.Key) {
		case "event", "prevent", "description", "validplayer", "activezones", "secondary",
			"playerturn", "activephases", "checksvar", "svarcompare",
			"ispresent", "presentcompare", "presentzone", "presentplayer", "presentdefined":
		default:
			return false
		}
	}
	return hostInActiveZones(g.Card(host), r, hostZone) && replacementRequirementsCheck(g, g.Card(host), amounts, r)
}

// gainLifePrevented is CR 119/614's own "prevent this life gain" shape
// (Prevent$ True) applied to CR 119.3's own "gain life" event -- the
// identical contract drawPrevented has for a different Event$.
// gainLifeEffect (gainlifeeffect.go) checks this before applying each
// player's own LifeAmount$, per player named by Defined$/ValidTgts$ -- CR
// 119's own life-gain replacement family game-state.md's own "M6's fourth
// effect: GainLife" section already flagged as entirely unbuilt is real now
// for its one directly resolvable real shape.
//
// 1 of the corpus's own 21 real Event$ GainLife lines resolves end to end,
// and it is also the ONLY one naming Prevent$ at all: sulfuric_vortex.txt's
// own bare "if a player would gain life, that player gains no life instead"
// (Prevent$ True, no ValidPlayer$ at all -- every player's own life gain is
// prevented, not just the caster's). The other 20 real lines all name
// ReplaceWith$ instead -- GainDouble/RLoseLife/Draw/GainLife/ReplaceGainLife
// among them -- gainLifeReplaced's own doc comment, below, has the count.
func (g *Game) gainLifePrevented(player PlayerID) bool {
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !gainLifePreventionMatches(g, r, host, z, face.Amounts) {
							continue
						}
						if validPlayer, ok := r.Param("ValidPlayer"); ok {
							matched, recognized := matchesPlayerSpec(g, player, h.Controller(), host, validPlayer)
							if !recognized || !matched {
								continue
							}
						}
						return true
					}
				}
			}
		}
	}
	return false
}

// gainLifePreventionMatches is gainLifePrevented's own shared half: Event$
// GainLife, Prevent$ True, r's own host in one of its own ActiveZones$, and
// replacementRequirementsCheck (above). No real corpus line combines
// Prevent$ True with any param outside this shape's own allow-list --
// sulfuric_vortex.txt's own line is the entire real GainLife|Prevent$
// population, so this allow-list is wider than the corpus strictly needs
// today, kept symmetric with drawPreventionMatches' own identical shape
// rather than pared down to one card's exact param set.
func gainLifePreventionMatches(g *Game, r *compile.Ability, host CardID, hostZone ZoneType, amounts map[string]expr.Amount) bool {
	if !strings.EqualFold(r.Name, "GainLife") {
		return false
	}
	prevent, ok := r.Param("Prevent")
	if !ok || !strings.EqualFold(prevent, "True") {
		return false
	}
	for _, p := range r.Params {
		switch strings.ToLower(p.Key) {
		case "event", "prevent", "description", "validplayer", "activezones", "secondary",
			"playerturn", "activephases", "checksvar", "svarcompare",
			"ispresent", "presentcompare", "presentzone", "presentplayer", "presentdefined":
		default:
			return false
		}
	}
	return hostInActiveZones(g.Card(host), r, hostZone) && replacementRequirementsCheck(g, g.Card(host), amounts, r)
}

func tapAbilityResolvesTap(g *Game, a *compile.Ability, host *Card, amounts map[string]expr.Amount) (shouldTap, recognized bool) {
	if !strings.EqualFold(a.Name, "Tap") {
		return false, false
	}
	defined, ok := a.Param("Defined")
	if !ok || (!strings.EqualFold(defined, "Self") && !strings.EqualFold(defined, "ReplacedCard")) {
		return false, false
	}
	for _, p := range a.Params {
		switch strings.ToLower(p.Key) {
		case "db", "defined", "etb",
			"conditionpresent", "conditioncompare", "conditionchecksvar", "conditionsvarcompare":
		default:
			return false, false
		}
	}
	return subAbilityConditionMet(g, host, amounts, a), true
}

// drawReplaced is CR 616's own "the event is replaced by a different one"
// outcome applied to CR 120.3's own "draw a card" event -- ReplaceDraw's own
// ReplaceWith$ dispatch, ported for the one shape this port can actually run
// the substitute ability for: a plain DB$ Draw or DB$ PutCounter naming no
// SubAbility$ of its own and no param past what CardTraitBase's own general
// requirements already resolve. DrawCards (turn.go) checks this right after
// drawPrevented, before the empty-library check -- the identical
// "before the empty-library check" reasoning drawPrevented's own doc comment
// already gives, since thought_reflection.txt's own real "if you would draw
// a card, draw two cards instead" must never let CR 704.5b's own
// empty-library loss see the replaced draw at all.
//
// The substitute ability's own draws run through drawOneCard directly
// (turn.go), not back through DrawCards/drawPrevented/drawReplaced itself:
// Java's own ReplacementHandler guards a replacement effect against
// reapplying to an event its own resolution produced (the `hasRun` set,
// ReplacementHandler.java), a per-line recursion guard this port does not
// build -- thought_reflection.txt's own replaced draw would otherwise
// recheck thought_reflection.txt's own line against itself, forever.
// Reusing the unguarded primitive instead sidesteps the need for one, at the
// cost of a real, narrow simplification: the replacement's own draws are
// not themselves checked against any OTHER replacement or prevention effect
// on the battlefield either, not just the one that produced them. No real
// corpus line combines two Draw-replacing permanents today, so this is not
// observable against the corpus, but it is not full CR 616 either.
//
// 7 of the corpus's own 36 real Event$ Draw lines naming ReplaceWith$
// resolve end to end: thought_reflection.txt's own bare "if you would draw
// a card, draw two cards instead" (ValidPlayer$ You, ReplaceWith$ naming a
// plain DB$ Draw | Defined$ You | NumCards$ 2); phial_of_galadriel.txt's own
// Hellbent$ True-qualified identical shape ("while you have no cards in
// hand," resolved through replacementRequirementsCheck's own
// triggerCommonRequirementsMet fold-in); ormos_archive_keeper.txt's own
// IsPresent$ Card.YouOwn | PresentZone$ Library | PresentCompare$ EQ0
// qualified line, whose own ReplaceWith$ names a DB$ PutCounter instead of a
// DB$ Draw ("if your library has no cards in it, instead put five +1/+1
// counters on CARDNAME"); teferis_ageless_insight.txt's/
// alhammarrets_archive.txt's/bard_king_of_dale.txt's own real "except the
// first one you draw in each of your draw steps, draw two cards instead"
// (ValidPlayer$ You, NotFirstCardInDrawStep$ True, resolved through
// notFirstCardInDrawStepExempts below); notion_thief.txt's own real "except
// the first one they draw ..., instead you draw a card" (ValidPlayer$
// Opponent, the same NotFirstCardInDrawStep$ gate, ReplaceWith$ naming
// Defined$ You -- host's own controller, not the opponent whose draw got
// replaced, applyDrawReplacementDraw's own doc comment has the full reason
// that reading had to change to make this one resolve).
//
// Not resolved: reed_richards_smartest_man.txt's own
// FirstExtraCardDrawnThisTurn$; hullbreacher.txt's own identical
// NotFirstCardInDrawStep$ shape, whose own ReplaceWith$ targets DB$ Token
// instead of DB$ Draw/PutCounter -- CreateToken is not a built Effect yet,
// unrelated to the gate itself; magus_of_the_chains.txt's/
// chains_of_mephistopheles.txt's own Defined$ ReplacedPlayer plus
// SubAbility$ DBDraw (2); breathstealers_crypt.txt's/sea_of_sand.txt's own
// Defined$ ReplacedPlayer plus SubAbility$ (2) -- "the player who would have
// drawn," a Defined$ token this port's definedPlayers (defined.go) has no
// case for, distinct from an ordinary player token; blood_scrivener.txt's
// own SubAbility$ DBLoseLife (1) -- the target ability itself chains
// further, which this dispatch explicitly refuses rather than silently
// dropping the chained half (GO-7); booby_trap.txt's own ValidPlayer$
// Player.Chosen (1) and pursuit_of_knowledge.txt's own Optional$ True (1) --
// both already-documented gaps (matchesPlayerSpec's/Discard's own doc
// comments). 4 of the corpus's own 20 real Event$ GainLife ReplaceWith$
// lines resolve end to end too, through gainLifeReplaced (below) --
// unrelated to this function, which only ever runs for Event$ Draw.
func (g *Game) drawReplaced(controller PlayerController, player PlayerID) bool {
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !drawReplacementMatches(g, r, host, z, face.Amounts) {
							continue
						}
						if validPlayer, ok := r.Param("ValidPlayer"); ok {
							matched, recognized := matchesPlayerSpec(g, player, h.Controller(), host, validPlayer)
							if !recognized || !matched {
								continue
							}
						}
						if notFirstCardInDrawStepExempts(g, r, player) {
							continue
						}
						for _, sub := range r.Subs {
							if !strings.EqualFold(sub.Key, "ReplaceWith") {
								continue
							}
							if applyDrawReplacement(g, controller, h, sub.Ability, face.Amounts) {
								return true
							}
						}
					}
				}
			}
		}
	}
	return false
}

// drawReplacementMatches is drawReplaced's own shared half: Event$ Draw,
// ReplaceWith$ present, r's own host in one of its own ActiveZones$, and
// replacementRequirementsCheck (above) -- drawPreventionMatches' own exact
// shape with "replacewith" in place of "prevent" ("hellbent" added,
// phial_of_galadriel.txt's own real line needing it). Any param besides the
// ones real corpus lines pair with this shape skips the whole line rather
// than guessing (GO-7).
func drawReplacementMatches(g *Game, r *compile.Ability, host CardID, hostZone ZoneType, amounts map[string]expr.Amount) bool {
	if !strings.EqualFold(r.Name, "Draw") {
		return false
	}
	if _, ok := r.Param("ReplaceWith"); !ok {
		return false
	}
	for _, p := range r.Params {
		switch strings.ToLower(p.Key) {
		case "event", "replacewith", "description", "validplayer", "activezones", "secondary",
			"playerturn", "activephases", "checksvar", "svarcompare", "hellbent",
			"ispresent", "presentcompare", "presentzone", "presentplayer", "presentdefined",
			"notfirstcardindrawstep":
		default:
			return false
		}
	}
	return hostInActiveZones(g.Card(host), r, hostZone) && replacementRequirementsCheck(g, g.Card(host), amounts, r)
}

// notFirstCardInDrawStepExempts is ReplaceDraw.canReplace's own
// NotFirstCardInDrawStep$ check: only the very first card player draws
// during their own current Draw step is exempt from a ReplaceWith$ naming
// it (numDrawnThisDrawStep()==0 && ownDraw, Java's own two-part guard) --
// any later draw in that same step, or any draw of player's outside their
// own Draw step entirely (an instant-speed effect during another player's
// turn, or during their own non-Draw phase), is not exempt and this
// replacement still applies. teferis_ageless_insight.txt's/
// bard_king_of_dale.txt's own real "except the first one you draw in each
// of your draw steps, draw two cards instead" (ValidPlayer$ You) and
// notion_thief.txt's own real "except the first one they draw ..., instead
// you draw a card" (ValidPlayer$ Opponent) both carry this param -- the
// exemption is about player, the event's own affected player, regardless of
// which ValidPlayer$ token routed the match here.
func notFirstCardInDrawStepExempts(g *Game, r *compile.Ability, player PlayerID) bool {
	if v, ok := r.Param("NotFirstCardInDrawStep"); !ok || !strings.EqualFold(v, "True") {
		return false
	}
	ownDraw := g.activePhase == Draw && g.activePlayer == player
	return ownDraw && g.Player(player).DrawnThisDrawStep == 0
}

// applyDrawReplacement runs a's own ReplaceWith$ target ability directly --
// a plain DB$ Draw or DB$ PutCounter naming no SubAbility$ of its own,
// tapAbilityResolvesTap's own "recognize the one shape, run it by hand"
// precedent applied to a second Event$. Neither drawEffect nor
// putCounterEffect is called: both need a *Registry (effect.go) to chain a
// SubAbility$ once their own body finishes, which drawReplaced's own caller
// chain (DrawCards, turn.go) has no way to reach -- reusing the underlying
// primitives (drawOneCard/Card.Counters.Add) directly instead sidesteps
// that, at the cost of never chaining a SubAbility$ of its own, which is why
// a is refused outright when it names one rather than run with the chained
// half silently dropped (GO-7). Reports whether a recognized shape actually
// ran.
func applyDrawReplacement(g *Game, controller PlayerController, host *Card, a *compile.Ability, amounts map[string]expr.Amount) bool {
	if _, ok := a.Param("SubAbility"); ok {
		return false
	}
	switch {
	case strings.EqualFold(a.Name, "Draw"):
		return applyDrawReplacementDraw(g, controller, host, a, amounts)
	case strings.EqualFold(a.Name, "PutCounter"):
		return applyDrawReplacementPutCounter(g, host, a, amounts)
	}
	return false
}

// applyDrawReplacementDraw runs a plain "DB$ Draw | Defined$ You |
// NumCards$ N" ReplaceWith$ target directly -- drawEffect's own two
// resolvable params, hand-run here since drawEffect.Resolve needs a
// *Registry this call site cannot reach (applyDrawReplacement's own doc
// comment). Defined$ You is read as host's own controller -- Java's own
// AbilityUtils.getDefinedPlayers resolves the replacement ability's own
// "You" to the replacement's activating player, always the host card's
// controller -- rather than as player (the event's own affected player):
// thought_reflection.txt's/phial_of_galadriel.txt's own ValidPlayer$ You
// lines have player equal to host.Controller() already, so this reads
// identically for them, but notion_thief.txt's own real "if an opponent
// would draw ..., instead YOU draw a card" (ValidPlayer$ Opponent) needs the
// two told apart -- host's controller draws, not the opponent whose draw
// got replaced. A Defined$ value past "You," or one of drawEffect's own
// remaining unresolved params (Upto$/OptionalDecider$/Reveal$/
// RememberDrawn$), refuses rather than guesses.
func applyDrawReplacementDraw(g *Game, controller PlayerController, host *Card, a *compile.Ability, amounts map[string]expr.Amount) bool {
	for _, key := range []string{"Upto", "OptionalDecider", "Reveal", "RememberDrawn"} {
		if _, ok := a.Param(key); ok {
			return false
		}
	}
	defined, ok := a.Param("Defined")
	if !ok || !strings.EqualFold(defined, "You") {
		return false
	}
	drawer := host.Controller()
	n := 1
	if v, ok := a.Param("NumCards"); ok {
		parsed, ok := resolveNamedAmount(g, amounts, host, v)
		if !ok {
			return false
		}
		n = parsed
	}
	for i := 0; i < n; i++ {
		if !g.drawOneCard(controller, drawer) {
			break
		}
	}
	return true
}

// applyDrawReplacementPutCounter runs a plain "DB$ PutCounter |
// CounterType$ X | CounterNum$ N | Defined$ Self" ReplaceWith$ target
// directly -- putCounterEffect's own resolvable shape, hand-run here for
// the identical *Registry reason applyDrawReplacementDraw already gives.
// Defined$ is restricted to "Self" (the host card itself,
// ormos_archive_keeper.txt's own real shape) rather than the general
// definedCounterTargets (putcountereffect.go): no real corpus line combines
// this shape with a player-facing Defined$ value, and "Self" needs no
// controller or target list to resolve at all.
func applyDrawReplacementPutCounter(g *Game, host *Card, a *compile.Ability, amounts map[string]expr.Amount) bool {
	defined, ok := a.Param("Defined")
	if !ok || !strings.EqualFold(defined, "Self") {
		return false
	}
	counterType, err := putCounterType(a)
	if err != nil {
		return false
	}
	counterNum, ok := a.Param("CounterNum")
	if !ok {
		counterNum = "1"
	}
	amount, ok := resolveNamedAmount(g, amounts, host, counterNum)
	if !ok {
		return false
	}
	host.Counters.Add(counterType, amount)
	emitCounterChanged(g.sink, host.ID, CardEntity(host.ID), counterType, amount)
	return true
}

// gainLifeReplaced is drawReplaced's own sibling for CR 119's "gain life"
// event, but carries BOTH of CR 616's own outcomes past Prevented, the way
// damageReplaced (above) already does for a different Event$, not just
// Replaced: a full substitution (Draw/LoseLife, applyGainLifeReplacement,
// below) reports "nothing left to gain" the identical way
// applyDamageReplaceCounter's own full substitution does, by returning 0;
// replaceEffectEffect's own computed resize (below) returns the new
// amount instead, still to be gained through the normal path. amount is the
// pre-replacement LifeAmount$ gainLifeEffect.Resolve (below) already has in
// scope; the return value is what should actually be gained -- amount
// itself, unchanged, when no replacement matches at all, matching
// Player.gainLife's own `switch (... run(...)) { case NotReplaced: break;
// ...}` fallthrough. gainLifeEffect.Resolve's own `if gain <= 0 { continue }`
// right after calling this is Player.gainLife's own identical double
// zero-check (line 440's own pre-replacement guard AND line 463's own
// post-replacement one folded into the one check this port's own call
// ordering already needs, since neither guard does anything different here).
// Checked per player, the identical "before Life is touched at all" ordering
// gainLifePrevented's own doc comment already gives.
//
// 19 of the corpus's own 20 real Event$ GainLife | ReplaceWith$ lines
// resolve end to end now: lich.txt's/nefarious_lich.txt's own real "if you
// would gain life, draw that many cards instead" (ValidPlayer$ You,
// ReplaceWith$ naming a plain DB$ Draw | Defined$ You | NumCards$ <SVar
// naming ReplaceCount$LifeGained>); tainted_remedy.txt's/plague_drone.txt's
// own real "if an opponent would gain life, that player loses that much
// life instead" (ValidPlayer$ Opponent, ReplaceWith$ naming a plain
// DB$ LoseLife | LifeAmount$ <the same SVar shape> | Defined$
// ReplacedPlayer -- "the player who would have gained," read as the
// player parameter directly, the identical narrow reading
// applyDrawReplacementDraw's own doc comment already gives for Defined$
// You); and, through replaceEffectEffect (replaceeffect.go), 15 more real
// DB$ ReplaceEffect | VarName$ LifeGained | VarValue$ ... lines --
// rhox_faithmender.txt's/the_wind_crystal.txt's/selenia_the_cursed_heart.txt's/
// alhammarrets_archive.txt's/doctor_strange_surgeon.txt's/boon_reflection.txt's/
// phial_of_galadriel.txt's own real "gain twice that much life instead"
// (Twice) and angel_of_vitality.txt's/heron_of_hope.txt's/honor_troll.txt's/
// cleric_class.txt's/bilbo_birthday_celebrant.txt's/knight_of_dawns_light.txt's/
// leyline_of_hope.txt's/pest_rescuer.txt's own real "gain that much life plus
// 1 instead" (Plus.1) -- replaceEffectEffect's own identical
// resolveReplaceCountAmount dispatch (above), read against "LifeGained"
// instead of "DamageAmount".
//
// Not resolved: rain_of_gore.txt's own real "if a SPELL OR ABILITY would
// cause its controller to gain life" (ValidSource$ SpellAbility |
// SourceController$ True, no ValidPlayer$ at all -- a restriction on WHAT
// CAUSED the event rather than who it affects, a shape this file's own
// allow-lists have never needed to check before).
func (g *Game) gainLifeReplaced(controller PlayerController, player PlayerID, amount int) int {
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !gainLifeReplacementMatches(g, r, host, z, face.Amounts) {
							continue
						}
						if validPlayer, ok := r.Param("ValidPlayer"); ok {
							matched, recognized := matchesPlayerSpec(g, player, h.Controller(), host, validPlayer)
							if !recognized || !matched {
								continue
							}
						}
						for _, sub := range r.Subs {
							if !strings.EqualFold(sub.Key, "ReplaceWith") {
								continue
							}
							if applyGainLifeReplacement(g, controller, h, sub.Ability, face.Amounts, player, amount) {
								return 0
							}
							ev := replacementEvent{amountName: "LifeGained", amount: amount}
							if g.runReplaceWith(controller, h, face.Amounts, sub.Ability, &ev) {
								return ev.amount
							}
						}
					}
				}
			}
		}
	}
	return amount
}

// gainLifeReplacementMatches is gainLifeReplaced's own shared half:
// Event$ GainLife, ReplaceWith$ present, r's own host in one of its own
// ActiveZones$, and replacementRequirementsCheck (above) --
// drawReplacementMatches' own exact shape for a different Event$, plus
// "ailogic": every one of the 4 real lines this dispatch resolves carries
// AILogic$, a pure AI hint this port's own resolution never reads. Any
// param besides the ones real corpus lines pair with this shape skips the
// whole line rather than guessing (GO-7) -- rain_of_gore.txt's own
// ValidSource$/SourceController$ shape is exactly what falls through here.
func gainLifeReplacementMatches(g *Game, r *compile.Ability, host CardID, hostZone ZoneType, amounts map[string]expr.Amount) bool {
	if !strings.EqualFold(r.Name, "GainLife") {
		return false
	}
	if _, ok := r.Param("ReplaceWith"); !ok {
		return false
	}
	for _, p := range r.Params {
		switch strings.ToLower(p.Key) {
		case "event", "replacewith", "description", "validplayer", "activezones", "secondary", "ailogic",
			"playerturn", "activephases", "checksvar", "svarcompare",
			"ispresent", "presentcompare", "presentzone", "presentplayer", "presentdefined":
		default:
			return false
		}
	}
	return hostInActiveZones(g.Card(host), r, hostZone) && replacementRequirementsCheck(g, g.Card(host), amounts, r)
}

// applyGainLifeReplacement is applyDrawReplacement's own sibling: recognize
// a plain DB$ Draw or DB$ LoseLife target ability and run it by hand, the
// identical *Registry-avoidance reason applyDrawReplacement's own doc
// comment gives -- CR 616's own full-substitution outcome, gainLifeReplaced's
// own doc comment above reporting it as a gain of 0, the same convention
// applyDamageReplaceCounter's own full substitution already has.
// replaceEffectEffect (replaceeffect.go) is this function's own sibling for CR
// 616's OTHER outcome, a resized gain rather than a substituted one, tried
// second by gainLifeReplaced itself since real corpus lines never combine
// the two target shapes on one R: line. A target ability naming SubAbility$
// is refused outright rather than run with any chained half silently dropped
// (GO-7) -- no real corpus line among the 4 this dispatch resolves needs it,
// but neither target function below checks for it on its own.
func applyGainLifeReplacement(g *Game, controller PlayerController, host *Card, a *compile.Ability, amounts map[string]expr.Amount, player PlayerID, replaced int) bool {
	if _, ok := a.Param("SubAbility"); ok {
		return false
	}
	switch {
	case strings.EqualFold(a.Name, "Draw"):
		return applyGainLifeReplacementDraw(g, controller, host, a, amounts, player, replaced)
	case strings.EqualFold(a.Name, "LoseLife"):
		return applyGainLifeReplacementLoseLife(g, host, a, amounts, player, replaced)
	}
	return false
}

// applyGainLifeReplacementDraw runs a plain "DB$ Draw | Defined$ You |
// NumCards$ <ReplaceCount$LifeGained>" ReplaceWith$ target directly --
// applyDrawReplacementDraw's own shape, with resolveGainLifeReplacementAmount
// (below) in place of resolveNamedAmount so NumCards$'s own named SVar can
// resolve to replaced rather than failing the way resolveAmount's own
// Count-only Expression case already does for any other head.
func applyGainLifeReplacementDraw(g *Game, controller PlayerController, host *Card, a *compile.Ability, amounts map[string]expr.Amount, player PlayerID, replaced int) bool {
	for _, key := range []string{"Upto", "OptionalDecider", "Reveal", "RememberDrawn"} {
		if _, ok := a.Param(key); ok {
			return false
		}
	}
	defined, ok := a.Param("Defined")
	if !ok || !strings.EqualFold(defined, "You") {
		return false
	}
	n := 1
	if v, ok := a.Param("NumCards"); ok {
		parsed, ok := resolveGainLifeReplacementAmount(g, amounts, host, v, replaced)
		if !ok {
			return false
		}
		n = parsed
	}
	for i := 0; i < n; i++ {
		if !g.drawOneCard(controller, player) {
			break
		}
	}
	return true
}

// applyGainLifeReplacementLoseLife runs a plain "DB$ LoseLife | LifeAmount$
// <ReplaceCount$LifeGained> | Defined$ ReplacedPlayer" ReplaceWith$ target
// directly -- loseLifeEffect's own resolvable shape, hand-run for the
// identical *Registry reason applyDrawReplacementDraw's own doc comment
// gives. Defined$ accepts "ReplacedPlayer" (tainted_remedy.txt's/
// plague_drone.txt's own real value) alongside "You": both name the same
// player parameter already threaded through -- the player who would have
// gained the life this replaces -- the identical narrow reading
// applyDrawReplacementDraw's own doc comment gives for its own "You" case.
func applyGainLifeReplacementLoseLife(g *Game, host *Card, a *compile.Ability, amounts map[string]expr.Amount, player PlayerID, replaced int) bool {
	defined, ok := a.Param("Defined")
	if !ok || (!strings.EqualFold(defined, "You") && !strings.EqualFold(defined, "ReplacedPlayer")) {
		return false
	}
	lifeAmount, ok := a.Param("LifeAmount")
	if !ok {
		return false
	}
	amount, ok := resolveGainLifeReplacementAmount(g, amounts, host, lifeAmount, replaced)
	if !ok {
		return false
	}
	g.Player(player).Life -= amount
	g.sink.Emit(Event{Kind: LifeChanged, Source: host.ID, Target: PlayerEntity(player), Amount: -int32(amount)})
	return true
}

// resolveGainLifeReplacementAmount is resolveNamedAmount's own sibling for
// the one Expression head resolveAmount does not evaluate:
// ReplaceCount$LifeGained -- "the amount of life that would have been
// gained," a value only meaningful at this dispatch's own two callers
// above, which already have it in scope as replaced, not a general
// resolveAmount case (every other caller has no replaced amount in scope
// at all). A literal integer or any other named reference falls through to
// resolveNamedAmount unchanged.
func resolveGainLifeReplacementAmount(g *Game, amounts map[string]expr.Amount, host *Card, value string, replaced int) (int, bool) {
	if n, err := strconv.Atoi(value); err == nil {
		return n, true
	}
	if amt, ok := amounts[strings.ToLower(value)]; ok && amt.Kind == expr.Expression && amt.Op == nil &&
		strings.EqualFold(amt.Head, "ReplaceCount") {
		if amt.Negative {
			return -replaced, true
		}
		return replaced, true
	}
	return resolveNamedAmount(g, amounts, host, value)
}

// runReplaceWith resolves sub, a replacement's ReplaceWith$ ability, against
// ev when it is one of the Replace* APIs, through that API's own effect
// (replaceeffect.go) -- the code the Registry dispatches to, run on the
// spot with ev as its replacing object, which is how ReplacementHandler
// runs a ReplaceWith$ ability. Reports whether it ran and changed the
// event. A chained SubAbility$ is refused, since ReplacementHandler buffers
// that chain to run after the event, a step this port does not model; so
// is any param the effect itself rejects. Either way the line is skipped,
// never half applied (GO-7), and ev is left as it was.
func (g *Game) runReplaceWith(controller PlayerController, h *Card, amounts map[string]expr.Amount, sub *compile.Ability, ev *replacementEvent) bool {
	api, ok := APIByName(sub.Name)
	if !ok {
		return false
	}
	eff, ok := replaceEffectFor(api)
	if !ok {
		return false
	}
	if _, chained := sub.Param("SubAbility"); chained {
		return false
	}
	a := Ability{API: api, Source: h.ID, Controller: h.Controller(), Params: sub, Amounts: amounts, replacing: ev}
	saved := *ev
	if err := eff.Resolve(g, &a, controller); err != nil {
		*ev = saved
		return false
	}
	return ev.result != replacementNotReplaced
}

// eachReplacement walks every replacement of Event$ event whose host is
// active -- a Battlefield or Command card, in its ActiveZones$ -- calling fn
// until fn reports it applied one. The first to apply wins, this file's own
// CR 616 simplification (its top-of-file comment).
func (g *Game) eachReplacement(event string, fn func(h *Card, amounts map[string]expr.Amount, r *compile.Ability) bool) {
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, r := range face.Replacements {
						if !strings.EqualFold(r.Name, event) || !hostInActiveZones(h, r, z) {
							continue
						}
						if fn(h, face.Amounts, r) {
							return
						}
					}
				}
			}
		}
	}
}

// replaceWithSub is r's ReplaceWith$ ability, nil when it names none.
func replaceWithSub(r *compile.Ability) *compile.Ability {
	for _, sub := range r.Subs {
		if strings.EqualFold(sub.Key, "ReplaceWith") {
			return sub.Ability
		}
	}
	return nil
}

// onlyParams reports whether every param r names is one of keys (folded)
// or one of replacementRequirementsCheck's own -- the allow-list each
// dispatch below uses to skip a line naming something it cannot check.
func onlyParams(r *compile.Ability, keys ...string) bool {
	for _, p := range r.Params {
		k := strings.ToLower(p.Key)
		switch k {
		case "event", "replacewith", "description", "activezones", "secondary",
			"playerturn", "activephases", "checksvar", "svarcompare",
			"ispresent", "presentcompare", "presentzone", "presentplayer", "presentdefined":
			continue
		}
		if !containsString(keys, k) {
			return false
		}
	}
	return true
}

// countersReplaced is ReplaceAddCounter (Event$ AddCounter): n counters of
// kind ct that placer's effect is about to put on object. Returns how many
// to put instead -- n when nothing applies. Checks ValidCard$ (a card
// receiving), ValidPlayer$ (a player receiving), ValidObject$ (either),
// ValidCounterType$, ValidSource$ (placer) and EffectOnly$ (every caller is
// an effect). ValidCause$ skips the line: this port does not track what
// caused a placement.
func (g *Game) countersReplaced(controller PlayerController, placer PlayerID, object EntityID, ct CounterType, n int) int {
	g.eachReplacement("AddCounter", func(h *Card, amounts map[string]expr.Amount, r *compile.Ability) bool {
		if !onlyParams(r, "validcard", "validplayer", "validobject", "validcountertype", "validsource", "effectonly") {
			return false
		}
		if !replacementObjectMatches(g, r, h, object) {
			return false
		}
		if v, ok := r.Param("ValidCounterType"); ok && !strings.EqualFold(v, string(ct)) {
			return false
		}
		if v, ok := r.Param("ValidSource"); ok {
			matched, recognized := matchesPlayerSpec(g, placer, h.Controller(), h.ID, v)
			if !recognized || !matched {
				return false
			}
		}
		if !replacementRequirementsCheck(g, h, amounts, r) {
			return false
		}
		sub := replaceWithSub(r)
		if sub == nil {
			return false
		}
		ev := replacementEvent{amountName: "CounterNum", amount: n, counterType: ct, affected: object}
		if !g.runReplaceWith(controller, h, amounts, sub, &ev) {
			return false
		}
		n = ev.amount
		return true
	})
	return n
}

// replacementObjectMatches checks ValidCard$/ValidPlayer$/ValidObject$
// against object: a card must match ValidCard$ and ValidObject$ and fail
// any ValidPlayer$; a player the reverse. Absent keys pass.
func replacementObjectMatches(g *Game, r *compile.Ability, h *Card, object EntityID) bool {
	if cid, ok := object.AsCard(); ok {
		if _, ok := r.Param("ValidPlayer"); ok {
			return false
		}
		for _, key := range [...]string{"ValidCard", "ValidObject"} {
			if v, ok := r.Param(key); ok && !Matches(g, g.Card(cid), valid.Parse(v), h.Controller(), h.ID) {
				return false
			}
		}
		return true
	}
	pid, ok := object.AsPlayer()
	if !ok {
		return false
	}
	if _, ok := r.Param("ValidCard"); ok {
		return false
	}
	for _, key := range [...]string{"ValidPlayer", "ValidObject"} {
		v, ok := r.Param(key)
		if !ok {
			continue
		}
		if matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), h.ID, v); !recognized || !matched {
			return false
		}
	}
	return true
}

// tokensReplaced is ReplaceToken (Event$ CreateToken): n tokens from def
// that owner is about to create. Returns how many to create instead.
// ValidToken$ is matched against the token as it would enter -- def,
// owned and controlled by owner, not yet in any zone (Java matches the
// prototype in its TokenCreateTable the same way). ValidPlayer$ is the
// creating player. Optional$ and Layer$ skip the line: the one asks a
// question this dispatch has no hook for, the other names a copy or
// control replacement, not an amount.
func (g *Game) tokensReplaced(controller PlayerController, owner PlayerID, def *compile.Card, n int) int {
	// proto has no arena slot: ID is the NoCard zero value. Matches must
	// only read fields off the pointer for this call, never look the card
	// up by ID (g.Card(proto.ID) panics on NoCard). Every real ValidToken$
	// line in the corpus is a plain type/owner/control check, so this holds
	// today; a property that dereferences by ID would need proto allocated
	// through NewCard instead.
	proto := Card{Def: def, Owner: owner, controller: owner, IsToken: true, Zone: None}
	g.eachReplacement("CreateToken", func(h *Card, amounts map[string]expr.Amount, r *compile.Ability) bool {
		if !onlyParams(r, "validtoken", "validplayer", "effectonly") {
			return false
		}
		if v, ok := r.Param("ValidToken"); ok && !Matches(g, &proto, valid.Parse(v), h.Controller(), h.ID) {
			return false
		}
		if v, ok := r.Param("ValidPlayer"); ok {
			matched, recognized := matchesPlayerSpec(g, owner, h.Controller(), h.ID, v)
			if !recognized || !matched {
				return false
			}
		}
		if !replacementRequirementsCheck(g, h, amounts, r) {
			return false
		}
		sub := replaceWithSub(r)
		if sub == nil {
			return false
		}
		ev := replacementEvent{amountName: "TokenNum", amount: n}
		if !g.runReplaceWith(controller, h, amounts, sub, &ev) {
			return false
		}
		n = ev.amount
		return true
	})
	return n
}

// manaReplaced is ReplaceProduceMana (Event$ ProduceMana): m is the mana
// source's production for activator. Returns what is produced instead.
// ValidCard$ is the mana source, ValidActivator$ the activating player.
// ManaAmount$ and ValidSA$ skip the line.
func (g *Game) manaReplaced(controller PlayerController, activator PlayerID, source CardID, m producedMana) producedMana {
	g.eachReplacement("ProduceMana", func(h *Card, amounts map[string]expr.Amount, r *compile.Ability) bool {
		if !onlyParams(r, "validcard", "validactivator") {
			return false
		}
		if v, ok := r.Param("ValidCard"); ok && !Matches(g, g.Card(source), valid.Parse(v), h.Controller(), h.ID) {
			return false
		}
		if v, ok := r.Param("ValidActivator"); ok {
			matched, recognized := matchesPlayerSpec(g, activator, h.Controller(), h.ID, v)
			if !recognized || !matched {
				return false
			}
		}
		if !replacementRequirementsCheck(g, h, amounts, r) {
			return false
		}
		sub := replaceWithSub(r)
		if sub == nil {
			return false
		}
		ev := replacementEvent{amountName: "Mana", mana: m}
		if !g.runReplaceWith(controller, h, amounts, sub, &ev) {
			return false
		}
		m = ev.mana
		return true
	})
	return m
}

// damageRedirectAllowed is ReplaceDamage.canReplace's DamageTarget$ check,
// which a redirecting replacement (ReplaceSplitDamage's) names on its own
// line: the damaged object must not carry "Damage that would be dealt to
// CARDNAME can't be redirected.", every player DamageTarget$ names must
// still be in the game, and every card must be a creature, planeswalker or
// battle on the battlefield. The Replaced* spellings are allowed as Java
// allows them. Not modeled: the cause's NoRedirection$ (Lava Burst), since
// this dispatch is not handed the causing ability.
func damageRedirectAllowed(g *Game, r *compile.Ability, h *Card, affected EntityID) bool {
	def, ok := r.Param("DamageTarget")
	if !ok {
		return true
	}
	if cid, ok := affected.AsCard(); ok && g.Card(cid).HasKeyword("Damage that would be dealt to CARDNAME can't be redirected.") {
		return false
	}
	switch def {
	case "ReplacedSourceController":
		return true
	case "ReplacedTargetController":
		_, isCard := affected.AsCard()
		return isCard
	}
	if strings.HasPrefix(def, "Replaced") {
		return false
	}
	objs, err := definedEntities(g, h.Controller(), h, def, abilityRefs{})
	if err != nil {
		return false
	}
	for _, e := range objs {
		if pid, ok := e.AsPlayer(); ok && g.Player(pid).Lost {
			return false
		}
		if cid, ok := e.AsCard(); ok {
			c := g.Card(cid)
			t := c.Type()
			if c.Zone != Battlefield || !t.Has(cardtype.Creature) && !t.Has(cardtype.Planeswalker) && !t.Has(cardtype.Battle) {
				return false
			}
		}
	}
	return true
}
