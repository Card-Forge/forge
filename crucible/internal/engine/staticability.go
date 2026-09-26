// Two static-ability modes with no layer-folding of their own: CR 509.1b's
// CantBlockBy (flying/reach, Fear, Horsemanship, and every real corpus S:
// line written in that shape) and the legend rule's own IgnoreLegendRule
// corner case. Neither needs CR 613's layer system -- each is a plain
// ValidCard/ValidAttacker/ValidBlocker match, same shape valid.go already
// evaluates for everything else -- which is what makes both buildable ahead
// of Mode$ Continuous itself (game-state.md's "Continuous effects" section
// has the reasoning in full).
//
// Ported from
// forge-game/src/main/java/forge/game/staticability/StaticAbilityCantAttackBlock.java's
// cantBlockBy/applyCantBlockByAbility, and
// forge-game/src/main/java/forge/game/staticability/StaticAbilityIgnoreLegendRule.java.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// cantBlockByKeywords is every keyword CardFactoryUtil.java turns into a
// Mode$ CantBlockBy static ability at CardState-build time
// (CardFactoryUtil.java:3906-3935), not a combat-code keyword check --
// Flying's own block restriction is this static ability, generated once for
// every card that carries the keyword, the same way Fear's and
// Horsemanship's are (block.go's own doc comment already established this
// for Flying specifically). ValidAttacker is always "Creature.Self" for a
// keyword-synthesized one, since Java attaches the synthesized
// StaticAbility to the very card carrying the keyword.
//
// Landwalk is not in this table: its own CantBlockBy carries no ValidBlocker
// at all, only ValidDefender$ Player.controls<Type>, and <Type> is the
// keyword's OWN argument (K:Landwalk:Island's own "Island") -- a different
// value per card, not a name shared by every card carrying the keyword the
// way Fear's/Flying's/Horsemanship's/Intimidate's fixed ValidBlocker strings
// are. landwalkType (below) reads it directly off the keyword line instead
// (enchantSpec's own precedent, action.go).
//
// Protection is not here either, for the same per-card reason Landwalk
// is not: its own ValidBlocker is built from the keyword's own argument
// (Protection.getProtectionValid), a different value per card, not a fixed
// string every carrier shares. protectionEach (below) reads it directly.
//
// Skulk is not here either, but for a third reason: its own
// ValidBlocker$ Creature.powerGTX names a Compare property whose operand
// (X) is not a fixed string OR a per-card script value -- CardFactoryUtil's
// own Skulk branch hardcodes `st.setSVar("X", "Count$CardPower")` on the
// synthesized StaticAbility itself, always measuring the ability's own
// host (the attacker, since ValidAttacker$ is always Creature.Self). A
// non-numeric Compare operand is otherwise unresolvable (compareMatches'
// own doc comment, valid.go), but X here is not a compareMatches question
// at all once that hardcoding is known: skulkBlocks (below) is a direct
// power comparison, cantBlockBy's own call site (below) checked against
// h.ID == attacker directly rather than through this table's fixed-string
// shape.
//
// Menace is not here either, but for a different reason: Forge itself does
// not run Menace through the static-ability engine at all --
// StaticAbilityCantAttackBlock.getMinMaxBlocker hardcodes
// `attacker.hasKeyword(Keyword.MENACE)` directly, a minimum-blocker-COUNT
// rule CantBlockBy's per-blocker-identity check cannot express. It is
// checked on the whole declaration instead, with Mode$ MinMaxBlocker's
// counts, by validateBlocks' per-attacker blocker count (minMaxBlockers,
// blockvalidation.go).
var cantBlockByKeywords = []struct {
	keyword      string
	validBlocker string
}{
	{"Flying", "Creature.withoutFlying+withoutReach"},
	{"Fear", "Creature.nonArtifact+nonBlack"},
	{"Horsemanship", "Creature.withoutHorsemanship"},
	{"Intimidate", "Creature.nonArtifact+!SharesColorWith"},
}

// cantBlockBy reports whether attacker cannot legally be blocked by blocker,
// per every Mode$ CantBlockBy static ability currently in play.
//
// Every battlefield permanent is walked as a possible host, not just
// attacker itself -- Java's own cantBlockBy walks every card in
// ZoneType.STATIC_ABILITIES_SOURCE_ZONES (Battlefield, Graveyard, Exile,
// Command, Stack), because a real corpus S:CantBlockBy line is as often
// written on an Aura or Equipment (ValidAttacker$
// Creature.EnchantedBy/EquippedBy, granting its host "can't be blocked") as
// on the attacker's own card. Only Battlefield is walked here: no real
// corpus CantBlockBy line needs a source in the other four zones
// (game-state.md's "Not ported yet" has the count this is based on).
func cantBlockBy(g *Game, attacker, blocker CardID) bool {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CantBlockBy") {
						continue
					}
					va, ok := s.Param("ValidAttacker")
					if !ok {
						continue
					}
					vb, hasVB := s.Param("ValidBlocker")
					vd, hasVD := s.Param("ValidDefender")
					if !applyCantBlockBy(g, h, va, vb, hasVB, vd, hasVD, attacker, blocker) {
						continue
					}
					if rel, ok := s.Param("ValidBlockerRelative"); ok {
						if matched, recognized := blockerRelativeMatches(g, &face, rel, attacker, blocker); !recognized || !matched {
							continue
						}
					}
					return true
				}
			}
			for _, kb := range cantBlockByKeywords {
				if h.HasKeyword(kb.keyword) && applyCantBlockBy(g, h, "Creature.Self", kb.validBlocker, true, "", false, attacker, blocker) {
					return true
				}
			}
			if typ, ok := landwalkType(h); ok &&
				applyCantBlockBy(g, h, "Creature.Self", "", false, "Player.controls"+typ, true, attacker, blocker) {
				return true
			}
			if refused, _ := protectionEach(h, func(vb string, hasVB bool) bool {
				return applyCantBlockBy(g, h, "Creature.Self", vb, hasVB, "", false, attacker, blocker)
			}); refused {
				return true
			}
			if h.ID == attacker && h.HasKeyword("Skulk") && skulkBlocks(g, h, blocker) {
				return true
			}
		}
	}
	return false
}

// skulkBlocks is CR 702.118a's own "can't be blocked by creatures with
// greater power" (K:Skulk), ported from CardFactoryUtil.java's own
// synthesis: `Mode$ CantBlockBy | ValidAttacker$ Creature.Self |
// ValidBlocker$ Creature.powerGTX`, with `st.setSVar("X", "Count$CardPower")`
// hardcoded on the very StaticAbility Java builds -- unlike Landwalk's
// type argument or Protection's restriction, X here is not a per-card
// script value at all, so it needs no expr/Count$ evaluator
// (compareMatches' own doc comment, valid.go, already names this as the
// unresolvable-operand case): CardProperty.java's own "power" branch always
// measures `x = AbilityUtils.calculateAmount(source, "X", ...)` against
// `source`, the static ability's own host -- and ValidAttacker$ Creature.Self
// means that host is always the attacker itself. So X is always the
// attacker's own power, and this reads it the same way `Card.Power()`
// (card.go) already resolves anyone else's -- through every Layer 7 effect
// currently applied, not a printed value.
//
// h == attacker is the caller's own job (cantBlockBy's own call site) --
// ValidAttacker$ Creature.Self is not re-evaluated through Matches here
// since h.ID == attacker already says the identical thing more directly.
// blocker is not re-checked for being a Creature either: CanBlock's own
// precondition (block.go) already guarantees it before cantBlockBy is ever
// called. false when either card's own power is unresolvable ("*/*" with no
// resolving continuous effect, Card.Power's own ok=false) -- skip rather
// than guess (GO-7), the same contract every other unresolved comparison in
// this port already has.
func skulkBlocks(g *Game, host *Card, blocker CardID) bool {
	attackerPower, ok := host.Power()
	if !ok {
		return false
	}
	blockerPower, ok := g.Card(blocker).Power()
	if !ok {
		return false
	}
	return blockerPower > attackerPower
}

// blockerRelativeMatches is a CantBlockBy static's ValidBlockerRelative$:
// StaticAbilityCantAttackBlock.applyCantBlockByAbility matches the blocker
// with the attacker as the source card (matchesValidParam(
// "ValidBlockerRelative", blocker, attacker)). The one shape resolved is The
// Ring's level-1 "can't be blocked by creatures with greater power"
// (Player.setRingLevel): Creature.powerGTX with the static's X set to
// Count$CardPower, so X is the attacker's power -- skulkBlocks' comparison.
// recognized is false for any other spec or X (Space Beleren's
// Creature.DifferentSector, 1 corpus line): the static is then skipped, the
// skip-rather-than-guess contract matchesValidDefender has, rather than
// applied as if the relative restriction were not there -- which would make
// the attacker unblockable by everything.
func blockerRelativeMatches(g *Game, face *compile.Face, spec string, attacker, blocker CardID) (matched, recognized bool) {
	if spec != "Creature.powerGTX" {
		return false, false
	}
	x, ok := face.Amounts["x"]
	if !ok || x.Kind != expr.Expression || !strings.EqualFold(x.Head, "Count") || x.Body != "CardPower" || x.Op != nil {
		return false, false
	}
	return skulkBlocks(g, g.Card(attacker), blocker), true
}

// landwalkType reports h's own Landwalk keyword argument (K:Landwalk:Island
// -> "Island", K:Landwalk:Forest.Snow:snow Forest -> "Forest.Snow" -- the
// keyword's own first Args() element, exactly Landwalk.java's own
// getValidType/KeywordWithType.type), and whether h carries the keyword at
// all. Read directly off the keyword line (KeywordLines, card.go -- printed
// and continuously granted alike) rather than through cantBlockByKeywords'
// fixed-string table, since this value is the one part of Landwalk's own
// CantBlockBy synthesis (CardFactoryUtil.java's `Landwalk landwalk` branch)
// that differs per card.
func landwalkType(h *Card) (string, bool) {
	for _, line := range h.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Landwalk" {
			continue
		}
		args := k.Args()
		if len(args) == 0 || args[0] == "" {
			continue
		}
		return args[0], true
	}
	return "", false
}

// protectionEach reports h's own Protection keyword's CantBlockBy
// restriction, ported from CardFactoryUtil.java's `keyword.startsWith("Protection")`
// branch and Protection.getProtectionValid(keyword, false) (damage=false,
// the block-legality call, not the damage-prevention one) -- the natural
// corpus reason CantBlockBy could not use a fixed cantBlockByKeywords entry
// the way Fear/Flying/Horsemanship/Intimidate do: what a blocker must avoid
// being differs per card (a color, a type, a subtype), not a name every
// carrier shares, the identical reason landwalkType exists.
//
// Two real corpus shapes, both handled: the natural-language form
// ("K:Protection from red," 154 of roughly 219 real lines) and the
// colon-structured form ("K:Protection:Artifact," 65 lines) --
// keyword.Parse already tells them apart (Details is "from red" for the
// first, the characteristic itself for the second, keyword.go's own doc
// comment on the space-vs-colon split). ok is false only when h carries no
// recognized Protection keyword at all. hasValidBlocker is false for
// "protection from everything" (1 real line): Java's own getProtectionValid
// returns an empty string there, which CardFactoryUtil reads as "omit
// ValidBlocker$ entirely," an unconditional CantBlockBy -- applyCantBlockBy's
// own contract for hasValidBlocker=false already gives this for free. Read
// off KeywordLines (card.go), printed and continuously granted alike, the
// same as landwalkType.
//
// h can carry more than one recognized Protection line -- 22 corpus cards
// do, Mirran Crusader's own "Protection from black" and "Protection from
// green" among them -- each refusing independently (CR 702.16b: "a source
// with two or more protection abilities... [applies] each individually").
// fn is called once per recognized line, in KeywordLines order, and
// protectionEach reports true the first time fn does (the block/attach/
// target is refused) -- a single-line version of this port used to report
// only the first recognized line, missing every card with a second one;
// every caller now loops here instead.
func protectionEach(h *Card, fn func(validBlocker string, hasValidBlocker bool) bool) (refused, ok bool) {
	for _, line := range h.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Protection" {
			continue
		}
		var vb string
		var hasVB bool
		if rest, isColor := strings.CutPrefix(k.Details, "from "); isColor {
			valid, hvb, recognized := protectionColorValid(rest)
			if !recognized {
				continue
			}
			vb, hasVB = valid, hvb
		} else {
			characteristic, _, _ := strings.Cut(k.Details, ":")
			if characteristic == "" {
				continue
			}
			vb, hasVB = characteristic, true
		}
		ok = true
		if fn(vb, hasVB) {
			return true, true
		}
	}
	return false, ok
}

// protectionColorValid is Protection.getProtectionValid's own color branch
// for the natural-language "Protection from <word>" form, damage=false
// (CantBlockBy, not damage prevention, so no "Source" suffix is ever
// appended -- that only happens on the damage=true call this port does not
// make). recognized is false only for a protectType this port's own corpus
// scan never found real ("each color" does not appear in the real corpus
// at all, so it is not special-cased -- a future card using it would fall
// here and correctly get skipped, GO-7, rather than silently mismatched).
func protectionColorValid(protectType string) (valid string, hasValidBlocker, recognized bool) {
	switch protectType {
	case "white":
		return "Card.White,Emblem.White", true, true
	case "blue":
		return "Card.Blue,Emblem.Blue", true, true
	case "black":
		return "Card.Black,Emblem.Black", true, true
	case "red":
		return "Card.Red,Emblem.Red", true, true
	case "green":
		return "Card.Green,Emblem.Green", true, true
	case "colorless":
		return "Card.Colorless,Emblem.Colorless", true, true
	case "everything":
		return "", false, true
	}
	return "", false, false
}

// hostRefusesAttach reports whether host's own Protection keyword makes it
// illegal for aura to remain attached to it -- CR 704.5m's own ongoing
// re-check (cleanupDanglingAttachments, action.go), Java's
// GameEntity.cantBeAttachedMsg -> StaticAbilityCantAttach.cantAttach
// (GameEntity.java:270): host's Enchant-restriction match
// (enchantSpec/Matches, action.go/castspell.go) is a card-TYPE question
// ("enchant a creature"), checked separately by the caller; this is "does
// THIS host, specifically, refuse to stay attached to aura" -- Protection's
// own CantAttach half only. Hexproof and Shroud generate no CantAttach
// ability in Java (cantBeEnchantedByMsg, GameEntity.java:292-304, checks
// only the Enchant restriction itself, never StaticAbilityCantTarget) --
// hexproof or shroud gained by an already-enchanted host after the Aura
// attached does not make it fall off; the Aura was a legal target when it
// targeted the host (cardCantBeTargetedBy, below, ran then), and CR 704.5m
// never re-runs that check. Do not add Hexproof/Shroud here.
//
// Ported from CardFactoryUtil.java's own Protection branch, which
// synthesizes a `Mode$ CantAttach | Target$ Card.Self | ValidCard$ <valid>`
// line alongside CantBlockBy's `ValidBlocker$ <valid>` -- the identical
// `valid` string protectionEach (above) already extracts, just matched
// against aura itself here (StaticAbilityCantAttach's own `card` parameter)
// rather than a candidate blocker. "Protection from everything"
// (hasValidBlocker false) refuses unconditionally, the same contract
// protectionEach's own doc comment already gives applyCantBlockBy. Each of
// host's own recognized Protection lines is checked independently
// (CR 702.16b), the reason this loops through protectionEach rather than
// asking for one line's answer.
func hostRefusesAttach(g *Game, aura *Card, host CardID) bool {
	h := g.Card(host)
	refused, _ := protectionEach(h, func(vb string, hasVB bool) bool {
		return !hasVB || Matches(g, aura, valid.Parse(vb), h.Controller(), h.ID)
	})
	return refused
}

// cardCantBeTargetedBy reports whether target refuses to be the target of
// an ability controlled by activator, sourced from source -- CR 702.11b/e
// (Hexproof), 702.19a/b (Shroud) and 702.16e (Protection's targeting half),
// Java's Card.canBeTargetedBy/Player.canBeTargetedBy ->
// StaticAbilityCantTarget.cantTarget (Card.java:6820-6838,
// StaticAbilityCantTarget.java:37-51), narrowed the same way protectionEach
// and hexproofValidSource already are, to the keyword-generated CantTarget
// abilities alone -- a hand-written `S:Mode$ CantTarget` line (Gaea's
// Revenge) is not read. Called identically at target selection
// (targetCandidates) and at the CR 608.2b resolution re-check
// (targetStillLegal), since Java's own SpellAbility.canTarget runs the exact
// same entity.canBeTargetedBy(this) at both call sites regardless of its
// fizzleCheck argument (SpellAbility.java:1608) -- no asymmetry to
// reproduce.
//
// Every one of these keyword-generated abilities carries no `AffectedZone$`
// (CardFactoryUtil.java's own Hexproof/Shroud/Protection branches never set
// one), so `applyCantTargetAbility`'s own default zone gate applies:
// `card.isInPlay()` (StaticAbilityCantTarget.java:70-72) -- target refuses
// nothing while it is anywhere but the battlefield. This is why Counterspell
// can still target an opposing creature spell printed with Hexproof: the
// spell on the stack is not in play, so its printed Hexproof's CantTarget
// ability does not apply to it there at all.
//
// Protection is checked first and unconditionally (no Activator$ line at
// all in Java's own Protection branch, CardFactoryUtil.java:3966-3971) --
// ValidSource$ is protectionEach's own string, matched here against
// source, the ability's own host card, exactly as hostRefusesAttach matches
// it against aura instead. Shroud next, also unconditional (no
// Activator$, no "Shroud from X" variant Keyword.java ever parses -- base
// Shroud refuses every spell/ability, the controller's own included, CR
// 702.19a). Hexproof last, gated on `Activator$ Opponent`
// (CardFactoryUtil.java:3920-3931) -- matchesPlayerSpec's own "Opponent"
// base already is that check, activator against target's controller as
// You; bare `K:Hexproof` (80 of 110 real lines) then refuses
// unconditionally, a qualified `Hexproof from <type>`
// (hexproofValidSource) matches source the same way ValidSource$ does for
// Protection. "Hexproof from triggered/activated abilities" (2 real
// lines) is not resolved -- hexproofValidSource's own `ok=false` for
// `Triggered`/`Activated` (Java's ValidSA$, not ValidSource$; Matches only
// ever takes a *Card) -- so it never refuses here (GO-7): a
// Counterspell-shaped ChangeTargets or the initial cast of an instant
// naming one of these two cards as ValidTgts$ incorrectly lets the target
// through; logged in game-state.md's Not ported yet.
//
// Player targets never refuse here: no keyword-granting mechanism exists
// yet for a Player entity (PlayerFactoryUtil.java's own
// `Affected$ You | AddKeyword$ Hexproof`-shaped continuous grant --
// Leyline of Sanctity's own line -- has nothing on the Go side to land on;
// Card.KeywordMod, continuous.go's own applyOneContinuousKeyword, only ever
// writes a Card's keywords). Logged as its own game-state.md row, separate
// from this one.
func cardCantBeTargetedBy(g *Game, target *Card, activator PlayerID, source CardID) bool {
	if target.Zone != Battlefield {
		return false
	}
	src := g.Card(source)
	if refused, _ := protectionEach(target, func(vb string, hasVB bool) bool {
		return !hasVB || Matches(g, src, valid.Parse(vb), target.Controller(), target.ID)
	}); refused {
		return true
	}
	for _, line := range target.KeywordLines() {
		if keyword.Parse(line).Name == "Shroud" {
			return true
		}
	}
	if matched, _ := matchesPlayerSpec(g, activator, target.Controller(), target.ID, "Opponent"); !matched {
		return false
	}
	for _, line := range target.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Hexproof" {
			continue
		}
		if k.Details == "" {
			return true
		}
		if vs, ok := hexproofValidSource(k.Details); ok {
			if vs == "" || Matches(g, src, valid.Parse(vs), target.Controller(), target.ID) {
				return true
			}
		}
	}
	return false
}

// hexproofValidSource is KeywordWithType.parse's own Hexproof branch
// (`type = k[0]` for a `Hexproof:<type>:<description>` line, or
// `"Card." + Capitalize(color)` for a bare color word with no second colon
// at all -- 7 of the corpus's 30 real qualified lines: `Black`/`White`/
// `Blue`, capitalized exactly as `colorFromName`, valid.go, already expects)
// -- turned into the `ValidSource$` string CardFactoryUtil.java's own
// Hexproof branch synthesizes. A bare card-type word (`Enchantment`,
// `Artifact`, `Planeswalker`, `Instant`, `Creature` -- 11 real lines) is
// NOT prepended with `Card.`: KeywordWithType.parse only does that for a
// recognized color name, leaving a type word bare, which `baseMatches`
// (valid.go) already resolves correctly as a plain type check, the
// identical fallthrough `Card.isValid` itself uses. A compound
// `Card.<Property>` value (`Card.MonoColor`, `Card.MultiColor`,
// `Card.nonColorless`, 5 real lines) is already qualified in the corpus
// text itself and needs no transformation either.
//
// Not resolved: `Triggered`/`Activated` (2 real lines, "Hexproof from
// triggered/activated abilities") -- Java's own branch would synthesize
// `ValidSA$`, not `ValidSource$`, for these (`getTypeDescription().
// contains("abilities")`), and Matches (valid.go) only ever evaluates a
// *Card, never a SpellAbility -- refused rather than passed through
// unchanged, which would silently mean "never blocks" for these two real
// cards specifically (GO-7): an Aura's own cast-time targeting is not
// itself a triggered or activated ability doing the targeting, so treating
// these as "no restriction" would have produced the same observable
// behavior here regardless, but explicit refusal is the correct reason,
// not an accident of what Matches happens to never match.
func hexproofValidSource(details string) (validSource string, ok bool) {
	validType, _, hasColon := strings.Cut(details, ":")
	if validType == "" || validType == "Triggered" || validType == "Activated" {
		return "", false
	}
	if !hasColon {
		if _, isColor := colorFromName(validType); isColor {
			return "Card." + validType, true
		}
	}
	return validType, true
}

// applyCantBlockBy is applyCantBlockByAbility's ValidAttacker/ValidBlocker
// half, the only two params any real corpus S:CantBlockBy line or
// keyword-synthesized one this port builds actually needs (ValidAttacker in
// every real line; ValidBlocker absent only on a handful of unconditional
// "can't be blocked" lines, applied here exactly as Java does: skipped
// rather than treated as "matches nothing"). host is the card the ability
// lives on -- Creature.Self in validAttacker/validBlocker resolves against
// it, not against attacker or blocker, matching Matches's own "source is
// the card the spec is written on" contract (valid.go). validBlocker's
// comma-separated alternatives need no manual splitting: valid.Parse already
// treats a comma as OR between Spec.Alternatives, the same as any other
// multi-alternative valid string this port already passes through whole
// (trigger.go's ValidCard$ Cleric.Other,Card.Self is the same shape).
//
// Not ported from applyCantBlockByAbility: the "Dragon Hunter" reach
// exception (a ValidBlocker alternative containing "withoutReach" is undone
// if a separate CanBlockIfReach static grants that specific blocker
// effective reach against this specific attacker) -- one real corpus card
// needs it; ValidAttackerRelative -- one real corpus card, Ironclaw Curse
// (ValidBlockerRelative is cantBlockBy's own check, blockerRelativeMatches);
// and the Landwalk ignore-check (StaticAbilityIgnoreLandwalk.java) --
// zero real corpus S:Mode$ IgnoreLandwalk lines exist, so nothing here can
// ever need to consult it.
func applyCantBlockBy(g *Game, host *Card, validAttacker, validBlocker string, hasValidBlocker bool,
	validDefender string, hasValidDefender bool, attacker, blocker CardID) bool {
	if !Matches(g, g.Card(attacker), valid.Parse(validAttacker), host.Controller(), host.ID) {
		return false
	}
	if hasValidBlocker && !Matches(g, g.Card(blocker), valid.Parse(validBlocker), host.Controller(), host.ID) {
		return false
	}
	if hasValidDefender && !matchesValidDefender(g, g.Card(blocker).Controller(), validDefender, host) {
		return false
	}
	return true
}

// matchesValidDefender is ValidDefender's own check
// (StaticAbilityCantAttackBlock.applyCantBlockByAbility:
// `stAb.matchesValidParam("ValidDefender", blocker.getController())`) -- a
// Player, not a Card, so Matches (valid.go) cannot evaluate it.
// matchesPlayerBase's own three bare values (valid.go) cover 6 of the 8 real
// literal ValidDefender$ lines, checked here against defender vs
// host.Controller(). A "Player.controls<Type>" value -- Landwalk's own entire
// restriction, landwalkType's own doc comment has the reason it is built
// per card rather than looked up -- asks whether defender controls at least
// one battlefield permanent valid.Parse(type) matches (PlayerProperty.java's
// own "controls" branch, `property.substring(8)`, no comparator suffix:
// every real corpus use of this shape is the bare "at least one" default).
// Any other value (Player.Condition, Card.Self -- 2 of the 8 real literal
// lines) never matches, the same skip-rather-than-fire contract every other
// unresolved param in this port gets.
func matchesValidDefender(g *Game, defender PlayerID, spec string, host *Card) bool {
	if matched, ok := matchesPlayerBase(defender, host.Controller(), spec); ok {
		return matched
	}
	if typ, ok := strings.CutPrefix(spec, "Player.controls"); ok {
		return controllerControlsType(g, defender, typ, host)
	}
	return false
}

// controllerControlsType reports whether pid controls at least one
// battlefield permanent valid.Parse(typeSpec) matches, source/sourceController
// the ability's own host -- Matches's own "source is the card the spec is
// written on" contract (valid.go), the same pairing every other
// staticability.go check already passes.
func controllerControlsType(g *Game, pid PlayerID, typeSpec string, host *Card) bool {
	spec := valid.Parse(typeSpec)
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		if Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
			return true
		}
	}
	return false
}

// ignoreLegendRule reports whether id is exempt from the legend rule (CR
// 704.5j) by some Mode$ IgnoreLegendRule static ability in play. Ported from
// StaticAbilityIgnoreLegendRule.ignoreLegendRule/applyIgnoreLegendRuleAbility:
// every battlefield permanent is walked as a possible host (Java's own
// STATIC_ABILITIES_SOURCE_ZONES, trimmed to Battlefield the same way
// cantBlockBy's own doc comment justifies), and a ValidCard-less line (1 of
// the 11 real corpus lines, an unconditional "the legend rule doesn't
// apply") matches every card, exactly Java's own
// `stAb.matchesValidParam("ValidCard", card)` contract for an absent param.
//
// Not ported: a line carrying IsPresent$/PresentCompare$ (2 of the 11 --
// "if you control exactly two permanents named X") -- StaticAbility.java's
// own checkConditions evaluates those generically for every static-ability
// mode, a mechanism this port has not built for any mode yet (game-state.md's
// "Not ported yet"). Skipped rather than guessed at: an ability this port
// cannot evaluate the condition for is treated as not currently active, the
// same safe default an unresolvable Toughness leaves a creature alive under
// (destroyLethalToughness's own doc comment, action.go) -- wrong only in the
// rare case the condition holds, never in the far more common case it does
// not.
func ignoreLegendRule(g *Game, id CardID) bool {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "IgnoreLegendRule") {
						continue
					}
					if _, ok := s.Param("IsPresent"); ok {
						continue
					}
					validCard, ok := s.Param("ValidCard")
					if !ok {
						return true
					}
					if Matches(g, g.Card(id), valid.Parse(validCard), h.Controller(), h.ID) {
						return true
					}
				}
			}
		}
	}
	return false
}

// ignorePlaneswalkerZeroLoyaltyRule reports whether id is exempt from CR
// 704.5i (destroyZeroLoyalty, action.go) by some Mode$
// IgnorePlaneswalkerZeroLoyaltyRule static ability in play. Ported from
// StaticAbilityIgnoreZeroLoyalty.ignorePlaneswalkerZeroLoyaltyRule/
// applyIgnorePlaneswalkerZeroLoyaltyRuleAbility, the exact same shape as
// ignoreLegendRule above (a plain ValidCard match walked over every
// battlefield permanent, an absent ValidCard$ matching every card) except
// this mode's own Java side has no IsPresent$/PresentCompare$ escape hatch
// to skip.
func ignorePlaneswalkerZeroLoyaltyRule(g *Game, id CardID) bool {
	for _, pid := range g.Players() {
		for _, host := range g.Zone(Battlefield, pid).Cards() {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "IgnorePlaneswalkerZeroLoyaltyRule") {
						continue
					}
					validCard, ok := s.Param("ValidCard")
					if !ok {
						return true
					}
					if Matches(g, g.Card(id), valid.Parse(validCard), h.Controller(), h.ID) {
						return true
					}
				}
			}
		}
	}
	return false
}
