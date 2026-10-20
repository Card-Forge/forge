// Two static-ability modes with no layer-folding of their own: CR 509.1b's
// CantBlockBy (flying/reach, Fear, Horsemanship, and every real corpus S:
// line written in that shape) and the legend rule's own IgnoreLegendRule
// corner case. Neither needs CR 613's layer system -- each is a plain
// ValidCard/ValidAttacker/ValidBlocker match, same shape valid.go already
// evaluates for everything else -- which is what makes both buildable ahead
// of Mode$ Continuous itself (the removed port log's "Continuous effects" section
// has the reasoning in full).
//
// Ported from
// forge-game/src/main/java/forge/game/staticability/StaticAbilityCantAttackBlock.java's
// cantBlockBy/applyCantBlockByAbility, and
// forge-game/src/main/java/forge/game/staticability/StaticAbilityIgnoreLegendRule.java.

package engine

import (
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
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
	// Shadow's attacker half (CardFactoryUtil.java:3996): a creature with
	// shadow can be blocked only by creatures with shadow. The blocker half
	// (effect2, "can block only creatures with shadow") is checked in
	// cantBlockBy beside this table, since its ValidBlocker$ is Creature.Self.
	{"Shadow", "Creature.withoutShadow"},
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
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CantBlockBy") {
						continue
					}
					// An absent ValidAttacker$ matches every attacker
					// (CardTraitBase.matchesValidParam): Ironclaw Curse names only
					// the relative form.
					va, ok := s.Param("ValidAttacker")
					if !ok {
						va = "Card"
					}
					vb, hasVB := s.Param("ValidBlocker")
					vd, hasVD := s.Param("ValidDefender")
					if !applyCantBlockBy(g, h, va, vb, hasVB, vd, hasVD, attacker, blocker) {
						continue
					}
					// StaticAbilityCantAttackBlock.java:263-268: each side's relative
					// spec is matched with the other creature as the source.
					if rel, ok := s.Param("ValidAttackerRelative"); ok && !g.relativeMatches(&face, rel, attacker, blocker) {
						continue
					}
					if rel, ok := s.Param("ValidBlockerRelative"); ok && !g.relativeMatches(&face, rel, blocker, attacker) {
						continue
					}
					return true
				}
			}
			if h.HasKeyword("Shadow") && applyCantBlockBy(g, h, "Creature.withoutShadow", "Creature.Self", true, "", false, attacker, blocker) {
				return true
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
			if refused, _ := protectionEach(h.KeywordLines(), func(vb string, hasVB bool) bool {
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

// relativeMatches is a CantBlockBy static's ValidAttackerRelative$ or
// ValidBlockerRelative$: StaticAbilityCantAttackBlock.applyCantBlockByAbility
// matches subject against spec with relative as the source card
// (matchesValidParam(param, subject, relative)), so `Creature.DifferentSector`
// compares subject's sector with relative's and a Compare operand such as
// Ironclaw Curse's `Creature.powerGEIronclawX` measures relative's own
// toughness through the static's SVar (face.Amounts, carried to the
// evaluator by Game.relativeFace). The Ring's level-1 `Creature.powerGTX`
// is the same path with X = Count$CardPower of the attacker.
//
// An operand that does not resolve matches nothing (compareMatches), the
// skip-rather-than-guess contract every unresolved comparison has.
func (g *Game) relativeMatches(face *compile.Face, spec string, subject, relative CardID) bool {
	prev := g.relativeFace
	g.relativeFace = face
	defer func() { g.relativeFace = prev }()
	rc := g.Card(relative)
	return Matches(g, g.Card(subject), valid.Parse(spec), rc.Controller(), relative)
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
// lines can carry more than one recognized Protection line -- 22 corpus
// cards do, Mirran Crusader's own "Protection from black" and "Protection
// from green" among them -- each refusing independently (CR 702.16b: "a
// source with two or more protection abilities... [applies] each
// individually"). fn is called once per recognized line, in order, and
// protectionEach reports true the first time fn does (the block/attach/
// target is refused) -- a single-line version of this port used to report
// only the first recognized line, missing every card with a second one;
// every caller now loops here instead.
//
// lines is a *Card's or a *Player's own KeywordLines -- Absolute Virtue's
// own "Protection:Player.Opponent:each of your opponents" (`AddKeyword$`
// naming a player, `Affected$ You`, so lines is Player.KeywordLines when it
// reaches here) is the reason this reads lines rather than a *Card
// directly: Gor Muldrak, Amphinologist's identically-shaped
// "Protection:Salamander" already resolves whether it is printed on a card
// or granted to a player through continuous.go's own player branch, the
// same characteristic split either way.
func protectionEach(lines []string, fn func(validBlocker string, hasValidBlocker bool) bool) (refused, ok bool) {
	for _, line := range lines {
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
			// A player-relative characteristic ("Player.Opponent", Absolute
			// Virtue; "Player.PlayerUID_<n>", True-Name Nemesis and the
			// other "protection from the chosen player" lines;
			// "Player.OpponentOf PlayerUID_<n>", Cliffside Rescuer -- 10 real
			// corpus lines, printed and Pump-granted) names a controller,
			// not a card type: Protection.getProtectionValid turns it into
			// "ControlledBy <characteristic>" and then falls through to the
			// same "Card.<v>,Emblem.<v>" wrap every other characteristic
			// gets (Protection.java:13-17, 63-65). Matches evaluates it
			// through the ControlledBy property (valid.go).
			if strings.HasPrefix(characteristic, "Player") {
				v := "ControlledBy " + characteristic
				vb, hasVB = "Card."+v+",Emblem."+v, true
			} else {
				vb, hasVB = characteristic, true
			}
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
	refused, _ := protectionEach(h.KeywordLines(), func(vb string, hasVB bool) bool {
		return !hasVB || Matches(g, aura, valid.Parse(vb), h.Controller(), h.ID)
	})
	return refused
}

// cardCantBeTargetedBy reports whether target refuses to be the target of
// an ability controlled by activator, sourced from source -- CR 702.11b/e
// (Hexproof), 702.18a/b (Shroud) and 702.16e (Protection's targeting half),
// Java's Card.canBeTargetedBy/Player.canBeTargetedBy ->
// StaticAbilityCantTarget.cantTarget (Card.java:6820-6838,
// StaticAbilityCantTarget.java:37-51), narrowed the same way protectionEach
// and hexproofValidSource already are, to the keyword-generated CantTarget
// abilities, plus a card's own hand-written `S:Mode$ CantTarget` line
// (cantTargetStatic, Gaea's Revenge). Called identically at target selection
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
// 702.18a). Hexproof last, gated on `Activator$ Opponent`
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
// A Player target is playerCantBeTargetedBy's own job (below) -- Player has
// no `protectionEach`/battlefield zone, so the two do not share a body, only
// the Shroud/Hexproof shape.
func cardCantBeTargetedBy(g *Game, target *Card, activator PlayerID, source CardID, ask targetAsk) bool {
	kind := ask.kind
	if g.cantTargetStatic(CardEntity(target.ID), activator, source, ask) {
		return true
	}
	if target.Zone != Battlefield {
		return false
	}
	src := g.Card(source)
	if refused, _ := protectionEach(target.KeywordLines(), func(vb string, hasVB bool) bool {
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
		if sa, ok := hexproofValidSA(k.Details); ok {
			if matched, _ := saKindMatches(sa, kind); matched {
				return true
			}
			continue
		}
		if vs, ok := hexproofValidSource(k.Details); ok {
			if vs == "" || Matches(g, src, valid.Parse(vs), target.Controller(), target.ID) {
				return true
			}
		}
	}
	return false
}

// playerCantBeTargetedBy is cardCantBeTargetedBy's own Player-entity
// counterpart -- Java's Player.canBeTargetedBy -> StaticAbilityCantTarget.
// cantTarget (Player.java:1030-1041), ported from PlayerFactoryUtil.java's
// own Hexproof/Shroud/Protection branches (`ValidTarget$ Player.You`,
// `EffectZone$ Command`, otherwise identical to the Card branches
// CardFactoryUtil.java synthesizes).
//
// Protection is checked first and unconditionally, `protectionEach` shared
// with the Card branch (above) unchanged: Gor Muldrak, Amphinologist's own
// `Protection:Salamander` (a plain colon-structured characteristic) resolves
// against `Player.KeywordLines` exactly the way it would against a card's --
// Runed Halo's `Protection:ChosenName` and Serra's Emissary's
// `Protection:ChosenType` arrive substituted (`Card.named<Name>`, the chosen
// type; layerSubstituteKeyword), and a `Protection:Player.<spec>` line is read
// as the source test "Card.ControlledBy Player.<spec>" (protectionEach).
//
// target's own KeywordLines (player.go) is entirely Layer 6's doing --
// applyOneContinuousKeyword's own player branch (continuous.go), the one
// source of a Player's keyword lines, since a player has no printed face
// to fold onto the way a card does. Shroud next and unconditional, same as
// the Card branch; Hexproof last, gated on `Activator$ Opponent`, matched
// the same way.
func playerCantBeTargetedBy(g *Game, target PlayerID, activator PlayerID, source CardID, ask targetAsk) bool {
	kind := ask.kind
	if g.cantTargetStatic(PlayerEntity(target), activator, source, ask) {
		return true
	}
	p := g.Player(target)
	src := g.Card(source)
	// Matches' own source parameter is "the card the spec is written on,"
	// for a host-relative property (Self/Other/HostCard...) to resolve
	// against -- the Card branch passes the protected card itself (its
	// closest equivalent of Java's player.getKeywordCard()); a player has no
	// such card, so NoCard here, not source (the ATTACKING card): passing
	// source would resolve a host-relative property against the wrong side
	// entirely. A ControlledBy line resolves relative to target (the
	// protected player), which is Matches' sourceController argument.
	if refused, _ := protectionEach(p.KeywordLines(), func(vb string, hasVB bool) bool {
		return !hasVB || Matches(g, src, valid.Parse(vb), target, NoCard)
	}); refused {
		return true
	}
	for _, line := range p.KeywordLines() {
		if keyword.Parse(line).Name == "Shroud" {
			return true
		}
	}
	if matched, _ := matchesPlayerSpec(g, activator, target, source, "Opponent"); !matched {
		return false
	}
	for _, line := range p.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Hexproof" {
			continue
		}
		if k.Details == "" {
			return true
		}
		if sa, ok := hexproofValidSA(k.Details); ok {
			if matched, _ := saKindMatches(sa, kind); matched {
				return true
			}
			continue
		}
		if vs, ok := hexproofValidSource(k.Details); ok {
			if vs == "" || Matches(g, src, valid.Parse(vs), target, source) {
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
// `Triggered`/`Activated` (2 real lines, "Hexproof from triggered/activated
// abilities") are not a ValidSource$ at all: Java synthesizes `ValidSA$`
// for them (`getTypeDescription().contains("abilities")`), which
// hexproofValidSA reads, so this reports ok=false for them.
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
// needs it; and the Landwalk ignore-check (StaticAbilityIgnoreLandwalk.java) --
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
	// Heartwood Dryad / Wall of Diffusion / Aetherflame Wall / Aether Web
	// (StaticAbilityCantAttackBlock.java:250): a Mode$ CanBlockIfShadow static
	// lets the blocker block as though it had shadow, lifting a "without
	// shadow" restriction.
	if hasValidBlocker && strings.Contains(validBlocker, "withoutShadow") && canBlockIfShadow(g, attacker, blocker) {
		return false
	}
	if hasValidDefender && !matchesValidDefender(g, g.Card(blocker).Controller(), validDefender, host) {
		return false
	}
	return true
}

// canBlockIfShadow is StaticAbilityCantAttackBlock.canBlockIfShadow
// (StaticAbilityCantAttackBlock.java:304-326): some Mode$ CanBlockIfShadow
// static in play has its ValidAttacker$ and ValidBlocker$ (each absent is a
// pass) match the pair, evaluated against that static's own host. Only
// battlefield and Command hosts are walked (traitHosts), the trimming
// cantBlockBy's own doc comment justifies.
func canBlockIfShadow(g *Game, attacker, blocker CardID) bool {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CanBlockIfShadow") || !continuousConditionMet(g, h, s) {
						continue
					}
					if va, ok := s.Param("ValidAttacker"); ok && !Matches(g, g.Card(attacker), valid.Parse(va), h.Controller(), h.ID) {
						continue
					}
					if vb, ok := s.Param("ValidBlocker"); ok && !Matches(g, g.Card(blocker), valid.Parse(vb), h.Controller(), h.ID) {
						continue
					}
					return true
				}
			}
		}
	}
	return false
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
// mode, a mechanism this port has not built for any mode yet (the removed port log's
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
			for _, face := range h.traitFaces() {
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
			for _, face := range h.traitFaces() {
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

// netCombatDamage is Card.getNetCombatDamage (Card.java:4537): how much combat
// damage c assigns. A Mode$ AssignNoCombatDamage static naming c makes it 0;
// else a Mode$ CombatDamageToughness static naming c makes it c's toughness;
// else c's power, negated by Mode$ CombatDamageNegatePower (Loot, the Anomaly:
// "if his power is negative, he assigns combat damage as though it were
// positive"). ok is false when the value read is unresolvable (a "*" power or
// toughness this port has no evaluator for), propagated like Power/Toughness.
func netCombatDamage(g *Game, c *Card) (int, bool) {
	switch {
	case combatDamageStatic(g, c, "AssignNoCombatDamage"):
		return 0, true
	case combatDamageStatic(g, c, "CombatDamageToughness"):
		return c.Toughness()
	}
	power, ok := c.Power()
	if combatDamageStatic(g, c, "CombatDamageNegatePower") {
		power = -power
	}
	return power, ok
}

// combatDamageStatic reports whether some static of the given Mode$ names c.
// All three modes share one body (StaticAbilityAssignNoCombatDamage,
// StaticAbilityCombatDamageToughness, StaticAbilityCombatDamageNegatePower):
// the source's conditions hold (staticConditionsMet) and c matches ValidCard$.
//
// Hosts come from traitHosts, so an Effect-card static in the Command zone
// (EffectZone$ Command) and a delayed "this turn" effect's static both count.
func combatDamageStatic(g *Game, c *Card, mode string) bool {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, mode) {
						continue
					}
					if !g.staticConditionsMet(h, s) {
						continue
					}
					validCard, ok := s.Param("ValidCard")
					if !ok || Matches(g, c, valid.Parse(validCard), h.Controller(), h.ID) {
						return true
					}
				}
			}
		}
	}
	return false
}

// castsWithFlash reports whether pid may cast card as though it had flash
// (SpellAbility.withFlash, SpellAbility.java:2608): the printed or granted
// Flash keyword, or a Mode$ CastWithFlash static (StaticAbilityCastWithFlash)
// whose ValidCard$ matches the card, whose Caster$ matches pid and whose
// ValidSA$ matches the spell (a plain Spell, or a property
// spellAbilityMatches reads). A line whose ValidSA$ names IsTargeting or XCost
// is not read here: Java's applyWithFlashNeedsInfo asks it once targets and X
// are chosen (flashMode, castsWithFlashChosen). A ValidSA$ property this port
// does not evaluate, or a condition staticConditionsMet cannot resolve, is
// skipped, never assumed to hold (GO-7). The card's own statics count wherever it is (EffectZone$ All,
// Card.Self lines).
func (g *Game) castsWithFlash(pid PlayerID, card CardID) bool {
	if g.Card(card).HasKeyword("Flash") {
		return true
	}
	return g.flashStatic(pid, card, &Ability{Source: card, Controller: pid, spell: true}, flashPlain)
}

// flashMode is which half of StaticAbilityCastWithFlash a flashStatic walk is.
type flashMode uint8

const (
	// flashPlain is anyWithFlash before anything is chosen: a line whose
	// ValidSA$ needs the targets or X (flashNeedsInfo) is not read.
	flashPlain flashMode = iota
	// flashProvisional is anyWithFlashNeedsInfo: only those lines, with their
	// ValidSA$ unread, so the cast may begin and decide once the info exists.
	flashProvisional
	// flashFinal is anyWithFlash once the targets and X are chosen: every line,
	// ValidSA$ read against them.
	flashFinal
)

// flashNeedsInfo is applyWithFlashNeedsInfo's test of a line's ValidSA$: it
// names IsTargeting or XCost, which only a cast with its targets and X chosen
// can answer (StaticAbilityCastWithFlash.java:63-66).
func flashNeedsInfo(s *compile.Ability) bool {
	validSA, _ := s.Param("ValidSA")
	return strings.Contains(validSA, "IsTargeting") || strings.Contains(validSA, "XCost")
}

// castsWithFlashNeedsInfo is SpellAbilityRestriction.canPlay's second chance
// (SpellAbilityRestriction.java:559): a CastWithFlash line naming IsTargeting
// or XCost lets the cast begin at a timing it would otherwise be refused, and
// castsWithFlashChosen then decides once the targets and X are known.
func (g *Game) castsWithFlashNeedsInfo(pid PlayerID, card CardID) bool {
	return g.flashStatic(pid, card, &Ability{Source: card, Controller: pid, spell: true}, flashProvisional)
}

// castsWithFlashChosen is the decision castsWithFlashNeedsInfo deferred: some
// line grants flash to spell, which now carries its targets and X.
func (g *Game) castsWithFlashChosen(pid PlayerID, card CardID, spell *Ability) bool {
	return g.flashStatic(pid, card, spell, flashFinal)
}

// activatesWithFlash is SpellAbility.withFlash for an activated ability
// (StaticAbilityCastWithFlash with ValidSA$ Activated.Equip or
// Activated.Loyalty): pid may activate ability of card at instant speed
// although it is sorcery-speed (Equip, a loyalty ability). ValidCard$ names
// the ability's host.
func (g *Game) activatesWithFlash(pid PlayerID, card CardID, ability *compile.Ability) bool {
	return g.flashStatic(pid, card, &Ability{Source: card, Controller: pid, Params: ability, activated: true}, flashPlain)
}

// flashStatic is StaticAbilityCastWithFlash.anyWithFlash: some CastWithFlash
// line applies to the spell or ability sa of card for pid.
func (g *Game) flashStatic(pid PlayerID, card CardID, sa *Ability, mode flashMode) bool {
	c := g.Card(card)
	for _, host := range g.staticHostsWith(card) {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		for _, face := range h.traitFaces() {
			for _, s := range face.Statics {
				if !strings.EqualFold(s.Name, "CastWithFlash") || !g.castWithFlashApplies(pid, c, h, s, sa, mode) {
					continue
				}
				return true
			}
		}
	}
	return false
}

// castWithFlashApplies is one CastWithFlash line's own test, see
// castsWithFlash. An absent ValidSA$ matches any spell or ability
// (matchesValidParam on a missing key).
func (g *Game) castWithFlashApplies(pid PlayerID, c, host *Card, s *compile.Ability, sa *Ability, mode flashMode) bool {
	needsInfo := flashNeedsInfo(s)
	if (mode == flashPlain && needsInfo) || (mode == flashProvisional && !needsInfo) {
		return false
	}
	if validSA, ok := s.Param("ValidSA"); ok && mode != flashProvisional {
		if matched, recognized := g.spellAbilityMatches(sa, validSA, host, host.Controller(), host.abilityAmounts(s)); !recognized || !matched {
			return false
		}
	}
	if !g.staticConditionsMet(host, s) {
		return false
	}
	if caster, ok := s.Param("Caster"); ok {
		matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, caster)
		if !recognized || !matched {
			return false
		}
	}
	validCard, ok := s.Param("ValidCard")
	return !ok || Matches(g, c, valid.Parse(validCard), host.Controller(), host.ID)
}

// playerStatic reports whether some static of the given Mode$ names pid and
// satisfies keep: its Condition$ holds (StaticAbility.checkConditions) and its
// ValidPlayer$ matches pid (an absent one matches everyone, as
// matchesValidParam does). The player-restriction modes (CantGainLife,
// CantDraw, ...) share this body in Java. A line whose ValidPlayer$ this port
// cannot recognize (matchesPlayerSpec) is skipped, never assumed to hold
// (GO-7).
func (g *Game) playerStatic(pid PlayerID, mode string, keep func(s *compile.Ability) bool) bool {
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, mode) || !playerStaticApplies(g, pid, h, s) {
						continue
					}
					if keep == nil || keep(s) {
						return true
					}
				}
			}
		}
	}
	return false
}

func playerStaticApplies(g *Game, pid PlayerID, host *Card, s *compile.Ability) bool {
	if !g.staticConditionsMet(host, s) {
		return false
	}
	spec, ok := s.Param("ValidPlayer")
	if !ok {
		return true
	}
	matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, spec)
	return recognized && matched
}

// cantGainLife is Player.canGainLife (StaticAbilityCantGainLosePayLife
// .anyCantGainLife): a player out of the game, or named by a Mode$ CantGainLife
// or CantChangeLife static, gains no life. CantChangeLife's losing half
// (anyCantLoseLife, anyCantPayLife) is not read.
func (g *Game) cantGainLife(pid PlayerID) bool {
	return g.Player(pid).Lost || g.playerStatic(pid, "CantGainLife", nil) || g.playerStatic(pid, "CantChangeLife", nil)
}

// cantLoseLife is Player.canLoseLife (StaticAbilityCantGainLosePayLife.
// anyCantLoseLife): a player out of the game, or named by a Mode$ CantLoseLife
// or CantChangeLife static, loses no life.
func (g *Game) cantLoseLife(pid PlayerID) bool {
	return g.Player(pid).Lost || g.playerStatic(pid, "CantLoseLife", nil) || g.playerStatic(pid, "CantChangeLife", nil)
}

// causeMatches is StaticAbility.matchesValidParam("ValidCause", cause) for the
// kinds it names: a comma list of Spell, Activated, Triggered or SpellAbility
// (any), each with +/. properties among ManaAbility and its negation (an
// activated mana ability is both Activated and ManaAbility) and YouCtrl/OppCtrl
// (the cause's controller against the static's host's, when known). Anything
// else does not match.
func causeMatches(spec, kind string, cause, host PlayerID) bool {
	for _, alt := range strings.Split(spec, ",") {
		head, props, _ := strings.Cut(alt, ".")
		isActivated := kind == causeActivated || kind == causeManaAbil
		switch head {
		case "SpellAbility":
		case causeSpell:
			if kind != causeSpell {
				continue
			}
		case causeActivated:
			if !isActivated {
				continue
			}
		case causeTriggered:
			if kind != causeTriggered {
				continue
			}
		default:
			continue
		}
		ok := true
		for _, p := range strings.Split(props, "+") {
			switch p {
			case "":
			case "ManaAbility":
				ok = ok && kind == causeManaAbil
			case "!ManaAbility":
				ok = ok && kind != causeManaAbil
			case "YouCtrl":
				ok = ok && cause != NoPlayer && cause == host
			case "OppCtrl":
				ok = ok && cause != NoPlayer && cause != host
			default:
				ok = false
			}
		}
		if ok {
			return true
		}
	}
	return false
}

// cantPayLife is Player.canPayLife's static half (anyCantPayLife): a
// CantPayLife, CantLoseLife or CantChangeLife static naming the player stops a
// payment of life, ForCost$ narrowing it to a cost (True) or an effect (False)
// and ValidCause$ to the kind of ability paying (cause). effect says which this
// payment is.
func (g *Game) cantPayLife(pid PlayerID, effect bool, cause string) bool {
	keep := func(s *compile.Ability) bool {
		if spec, ok := s.Param("ValidCause"); ok && !causeMatches(spec, cause, pid, pid) {
			return false
		}
		if forCost, ok := s.Param("ForCost"); ok && strings.EqualFold(forCost, "True") == effect {
			return false
		}
		return true
	}
	return g.playerStatic(pid, "CantPayLife", keep) || g.playerStatic(pid, "CantLoseLife", keep) || g.playerStatic(pid, "CantChangeLife", keep)
}

// cantSacrifice is StaticAbilityCantSacrifice.cantSacrifice: a Mode$
// CantSacrifice static whose ValidCard$ matches c. ForCost$ narrows it to a
// cost (True) or an effect (False); effect says which this sacrifice is. A line
// naming ValidCause$ is matched against cause's controller when there is a
// cause (SpellAbility.OppCtrl, YouCtrl), and not applied without one (GO-7).
func (g *Game) cantSacrifice(c *Card, effect bool, cause *Ability) bool {
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CantSacrifice") || !g.staticConditionsMet(h, s) {
						continue
					}
					if spec, ok := s.Param("ValidCause"); ok {
						// Only an ability this port can name the controller of
						// is matched; without one the line is not applied.
						if cause == nil || !causeMatches(spec, causeNone, cause.Controller, h.Controller()) {
							continue
						}
					}
					if forCost, ok := s.Param("ForCost"); ok && strings.EqualFold(forCost, "True") == effect {
						continue
					}
					spec, ok := s.Param("ValidCard")
					if !ok || Matches(g, c, valid.Parse(spec), h.Controller(), host) {
						return true
					}
				}
			}
		}
	}
	return false
}

// cantDraw is Player.canDraw, cantDrawAmount for one card.
func (g *Game) cantDraw(pid PlayerID) bool { return g.cantDrawAmount(pid, 1) }

// cantDrawAmount is the negation of StaticAbilityCantDraw.canDrawThisAmount: a
// Mode$ CantDraw static allows DrawLimit$ (default 0) draws a turn, so with
// the player having drawn some already it allows max(limit - drawn, 0) more,
// and n cards are refused when that is fewer.
func (g *Game) cantDrawAmount(pid PlayerID, n int) bool {
	drawn := g.Player(pid).CardsDrawnThisTurn
	return g.playerStatic(pid, "CantDraw", func(s *compile.Ability) bool {
		limit := 0
		if raw, ok := s.Param("DrawLimit"); ok {
			parsed, err := strconv.Atoi(raw)
			if err != nil {
				return false
			}
			limit = parsed
		}
		return n > max(limit-drawn, 0)
	})
}

// unresolvedStaticConditions are the generic condition params
// StaticAbility.checkConditions and CardTraitBase.meetsCommonRequirements read
// that this port does not evaluate for a static of any mode: a line carrying
// one is not applied (GO-7). IsPresent$, IsPresent2$ and CheckSVar$ are
// evaluated by staticConditionsMet through the trigger side's own
// isPresentMatches/checkSVarMatches.
var unresolvedStaticConditions = [...]string{
	"CheckSecondSVar", "LifeTotal", "CheckDefinedPlayer",
	"TopCardOfLibraryIs", "Metalcraft", "Delirium", "Threshold", "Hellbent", "Bloodthirst", "FatefulHour",
	"Monarch", "Revolt", "Blessing", "EnduringStory", "DayTime", "Adamant",
}

// staticConditionsMet is StaticAbility.checkConditions (StaticAbility.java:362)
// for the conditions every static mode shares: Condition$ (continuousConditionMet),
// Phases$ (the current step in the named range, parsePhaseRange) and PlayerTurn$
// (the active player among the defined players). The source's own zone is the
// caller's job: hosts come from traitHosts. A line carrying a condition in
// unresolvedStaticConditions does not hold.
func (g *Game) staticConditionsMet(host *Card, s *compile.Ability) bool {
	return staticHostZoneOK(host, s) && g.staticOtherConditionsMet(host, s)
}

// staticOtherConditionsMet is staticConditionsMet past the host's zone: a
// spell being cast (cantTargetHost) is not yet in the Stack zone Go-side, so
// its EffectZone$ Stack lines skip only the zone test.
func (g *Game) staticOtherConditionsMet(host *Card, s *compile.Ability) bool {
	for _, key := range unresolvedStaticConditions {
		if _, ok := s.Param(key); ok {
			return false
		}
	}
	if !continuousConditionMet(g, host, s) {
		return false
	}
	amounts := host.abilityAmounts(s)
	if !isPresentMatches(g, host, amounts, s, "IsPresent", "PresentCompare", "PresentDefined", "PresentZone", "PresentPlayer") ||
		!isPresentMatches(g, host, amounts, s, "IsPresent2", "PresentCompare2", "PresentDefined2", "PresentZone2", "PresentPlayer2") ||
		!checkSVarMatches(g, host, amounts, s, "CheckSVar", "SVarCompare", "CheckSecondSVar") {
		return false
	}
	if phases, ok := s.Param("Phases"); ok {
		set, ok := parsePhaseRange(phases)
		if !ok || !set.has(g.activePhase) {
			return false
		}
	}
	if turn, ok := s.Param("PlayerTurn"); ok {
		players, err := definedPlayers(g, host.Controller(), host.ID, turn, abilityRefs{})
		if err != nil || !slices.Contains(players, g.activePlayer) {
			return false
		}
	}
	return true
}

// cantPutCounterParams are the params a CantPutCounter line may carry that
// this port evaluates (AffectedZone$ is read by Java for Continuous only, and
// ignored here as there).
var cantPutCounterParams = map[string]bool{
	"mode": true, "validcard": true, "validplayer": true, "countertype": true, "affectedzone": true,
	"condition": true, "phases": true, "playerturn": true, "effectzone": true,
	"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
}

// cantPutCounter is Card.canReceiveCounters / Player.canReceiveCounters
// (StaticAbilityCantPutCounter.anyCantPutCounter): some Mode$ CantPutCounter
// static names object and counter kind ct. A card is named by ValidCard$ (a
// line with ValidPlayer$ is the player half), a player by ValidPlayer$
// (a line with ValidCard$ is the card half); an absent CounterType$ names
// every kind. Unresolvable lines are not applied (GO-7).
func (g *Game) cantPutCounter(object EntityID, ct CounterType) bool {
	cid, isCard := object.AsCard()
	pid, isPlayer := object.AsPlayer()
	if !isCard && !isPlayer {
		return false
	}
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CantPutCounter") || !paramsResolvable(s, cantPutCounterParams) || !g.staticConditionsMet(h, s) {
						continue
					}
					if kind, ok := s.Param("CounterType"); ok && !strings.EqualFold(kind, string(ct)) {
						continue
					}
					_, hasCard := s.Param("ValidCard")
					_, hasPlayer := s.Param("ValidPlayer")
					switch {
					case isCard && hasPlayer, isPlayer && hasCard:
						continue
					case isCard:
						v, _ := s.Param("ValidCard")
						if !hasCard || Matches(g, g.Card(cid), valid.Parse(v), h.Controller(), h.ID) {
							return true
						}
					default:
						v, _ := s.Param("ValidPlayer")
						matched, recognized := true, true
						if hasPlayer {
							matched, recognized = matchesPlayerSpec(g, pid, h.Controller(), h.ID, v)
						}
						if recognized && matched {
							return true
						}
					}
				}
			}
		}
	}
	return false
}

// paramsResolvable reports whether every key s carries is in known.
func paramsResolvable(s *compile.Ability, known map[string]bool) bool {
	for _, p := range s.Params {
		if !known[strings.ToLower(p.Key)] {
			return false
		}
	}
	return true
}

// staticHostZoneOK is StaticAbility.zonesCheck (StaticAbility.java:337): the
// host's own zone must be one the line is active in. EffectZone$ names them
// ("All", or a comma list); without it the default is in play. The Command
// zone is accepted by default too, since the effect-like hosts traitHosts walks
// there carry their statics without an EffectZone$ of their own. This is what
// keeps a card's own statics (a host the callers add for EffectZone$ All lines)
// from applying while it sits in hand.
func staticHostZoneOK(host *Card, s *compile.Ability) bool {
	return staticZoneOK(host.Zone, s)
}

// staticZoneOK is staticHostZoneOK for a host taken to be in zone: a spell
// being cast is not in the Stack zone Go-side yet, so its own statics are read
// as if it were (manaConvertApplies).
func staticZoneOK(zone ZoneType, s *compile.Ability) bool {
	zones, ok := s.Param("EffectZone")
	if !ok {
		return zone == Battlefield || zone == Command
	}
	if strings.EqualFold(zones, "All") {
		return true
	}
	list, err := parseZoneList(zones)
	return err == nil && slices.Contains(list, zone)
}

// canDamagePrevented is Card.canDamagePrevented: damage from source is
// preventable unless a Mode$ CantPreventDamage static names it
// (StaticAbilityCantPreventDamage). IsCombat$ must equal isCombat when present
// and ValidSource$ must match source; the source's own statics count too
// (Spell.Self lines on the stack), through staticHostsWith. A line with a
// param outside that list is not applied (GO-7). Prevention shields and
// Prevent$ replacements are skipped by the callers when this is false.
func (g *Game) canDamagePrevented(source CardID, isCombat bool) bool {
	c := g.Card(source)
	for _, host := range g.staticHostsWith(source) {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		for _, face := range h.traitFaces() {
			for _, s := range face.Statics {
				if !strings.EqualFold(s.Name, "CantPreventDamage") || !paramsResolvable(s, cantPreventDamageParams) || !g.staticConditionsMet(h, s) {
					continue
				}
				if raw, ok := s.Param("IsCombat"); ok && strings.EqualFold(raw, "True") != isCombat {
					continue
				}
				if v, ok := s.Param("ValidSource"); ok && !Matches(g, c, valid.Parse(v), h.Controller(), h.ID) {
					continue
				}
				return false
			}
		}
	}
	return true
}

var cantPreventDamageParams = map[string]bool{
	"mode": true, "iscombat": true, "validsource": true, "effectzone": true,
	"condition": true, "phases": true, "playerturn": true,
	"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
}

// maxCounterParams are the params a MaxCounter line may carry that this port
// evaluates.
var maxCounterParams = map[string]bool{
	"mode": true, "validcard": true, "countertype": true, "maxnum": true, "condition": true,
	"effectzone": true, "description": true, "secondary": true,
}

// maxCounter is StaticAbilityMaxCounter.maxCounter: the smallest MaxNum$ of
// every Mode$ MaxCounter static naming card id and counter kind ct, with ok
// false when none does. Java asks only for Dream counters (Card.getCounterMax,
// Card.java:1744); the caller passes ct through the same gate. MaxNum$ is read
// as a literal -- the one real line writes 7 -- and a line with another form,
// or a param this port does not evaluate, is not applied (GO-7).
func (g *Game) maxCounter(id CardID, ct CounterType) (limit int, ok bool) {
	if ct != "DREAM" {
		return 0, false
	}
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "MaxCounter") || !paramsResolvable(s, maxCounterParams) || !g.staticConditionsMet(h, s) {
						continue
					}
					if kind, has := s.Param("CounterType"); has && !strings.EqualFold(kind, string(ct)) {
						continue
					}
					if v, has := s.Param("ValidCard"); has && !Matches(g, g.Card(id), valid.Parse(v), h.Controller(), h.ID) {
						continue
					}
					raw, _ := s.Param("MaxNum")
					n, err := strconv.Atoi(raw)
					if err != nil {
						continue
					}
					if !ok || n < limit {
						limit, ok = n, true
					}
				}
			}
		}
	}
	return limit, ok
}

// cantTargetParams are the params a hand-written CantTarget line may carry
// that this port evaluates.
var cantTargetParams = map[string]bool{
	"mode": true, "validtarget": true, "validsa": true, "validsource": true, "activator": true,
	"affectedzone": true, "effectzone": true, "condition": true, "description": true,
	"secondary": true, "spelldescription": true, "stackdescription": true,
	"sourcecanonlytarget": true,
}

// targetAsk is what StaticAbilityCantTarget.applyCantTargetAbility reads off
// the SpellAbility asking whether an entity can be its target.
type targetAsk struct {
	// kind is causeSpell, causeActivated or causeTriggered.
	kind string
	// root is the compiled line of the asking ability's root (a Charm mode
	// asks through its Charm), for SourceCanOnlyTarget$ to walk. nil when
	// the asker is unknown.
	root *compile.Ability
	// enchant is an Aura spell's Enchant restriction, which stands in for
	// the ValidTgts$ of the Attach ability Java builds for it.
	enchant string
	// casting is true while the spell is being cast, before it is on the
	// stack (CR 601.2c): the one moment an EffectZone$ Stack static of the
	// spell itself (Enthralling Hold) is live
	// (StaticAbilityCantTarget.java:63-68).
	casting bool
}

// askOf is the targetAsk of an ability already built.
func askOf(a *Ability) targetAsk {
	root := a.charmRoot
	if root == nil {
		root = a.Params
	}
	return targetAsk{kind: a.causeKind(), root: root, casting: a.casting}
}

// onlyTargets is the SourceCanOnlyTarget$ test (StaticAbilityCantTarget.java:
// 108-129): every targeting ability under the asking root -- a Charm's every
// mode chain, or the root's own SubAbility$ chain -- names a ValidTgts$ that
// contains word, holds no comma and no "non"+word. An ability asked with no
// known root never qualifies.
func (t targetAsk) onlyTargets(word string) bool {
	ok := func(tgts string) bool {
		return strings.Contains(tgts, word) && !strings.Contains(tgts, ",") && !strings.Contains(tgts, "non"+word)
	}
	if t.root == nil {
		return t.enchant != "" && ok(t.enchant)
	}
	var chains []*compile.Ability
	if strings.EqualFold(t.root.Name, "Charm") {
		for _, sub := range t.root.Subs {
			if strings.EqualFold(sub.Key, "Choices") {
				chains = append(chains, sub.Ability)
			}
		}
	} else {
		chains = []*compile.Ability{t.root}
	}
	for _, c := range chains {
		for next := c; next != nil; {
			if tgts, has := next.Param("ValidTgts"); has && !ok(tgts) {
				return false
			}
			var following *compile.Ability
			for _, sub := range next.Subs {
				if strings.EqualFold(sub.Key, "SubAbility") {
					following = sub.Ability
					break
				}
			}
			next = following
		}
	}
	return true
}

// cantTargetStatic is StaticAbilityCantTarget.cantTarget for the lines a card
// writes itself (Gaea's Revenge, Ground Seal, Silent Gravestone, ...): some
// Mode$ CantTarget static names entity as a target of an ability of the given
// kind (causeSpell, causeActivated, causeTriggered) that activator controls
// and source is the host of. The keyword-generated Hexproof, Shroud and
// Protection lines are read by cardCantBeTargetedBy itself.
//
// An EffectZone$ Stack line is live only on the spell being cast, before it
// is on the stack (ask.casting): Java's game.getCardsIn(STATIC_ABILITIES_
// SOURCE_ZONES) finds the spell in the Stack zone, and applyCantTargetAbility
// drops the line for a Card entity once getSpellMatchingHost finds it pushed.
func (g *Game) cantTargetStatic(entity EntityID, activator PlayerID, source CardID, ask targetAsk) bool {
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			if g.cantTargetHost(g.Card(host), false, entity, activator, source, ask) {
				return true
			}
		}
	}
	return ask.casting && source != NoCard && g.cantTargetHost(g.Card(source), true, entity, activator, source, ask)
}

// cantTargetHost is cantTargetStatic's loop over one host's CantTarget lines,
// the ones whose EffectZone$ names the Stack when onStack, the rest otherwise.
func (g *Game) cantTargetHost(h *Card, onStack bool, entity EntityID, activator PlayerID, source CardID, ask targetAsk) bool {
	if h.Def == nil {
		return false
	}
	for _, face := range h.traitFaces() {
		for _, s := range face.Statics {
			if !strings.EqualFold(s.Name, "CantTarget") || !paramsResolvable(s, cantTargetParams) {
				continue
			}
			if zone, ok := s.Param("EffectZone"); (ok && strings.Contains(zone, "Stack")) != onStack {
				continue
			}
			if onStack && !g.staticOtherConditionsMet(h, s) || !onStack && !g.staticConditionsMet(h, s) {
				continue
			}
			if g.cantTargetApplies(s, h, entity, activator, source, ask) {
				return true
			}
		}
	}
	return false
}

// cantTargetApplies is StaticAbilityCantTarget.applyCantTargetAbility for one
// line s on host h.
func (g *Game) cantTargetApplies(s *compile.Ability, h *Card, entity EntityID, activator PlayerID, source CardID, ask targetAsk) bool {
	kind := ask.kind
	zoneSpec, hasAffected := s.Param("AffectedZone")
	if id, isCard := entity.AsCard(); isCard {
		c := g.Card(id)
		if hasAffected {
			zones, err := parseZoneList(zoneSpec)
			if err != nil || !slices.Contains(zones, c.Zone) {
				return false
			}
		} else if c.Zone != Battlefield {
			return false
		}
		if v, ok := s.Param("ValidTarget"); ok && !Matches(g, c, valid.Parse(v), h.Controller(), h.ID) {
			return false
		}
	} else if pid, isPlayer := entity.AsPlayer(); isPlayer {
		if hasAffected {
			return false
		}
		if v, ok := s.Param("ValidTarget"); ok {
			if matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), h.ID, v); !matched || !recognized {
				return false
			}
		}
	} else {
		return false
	}
	if v, ok := s.Param("ValidSA"); ok {
		matched, recognized := saKindMatches(v, kind)
		if !matched || !recognized {
			return false
		}
	}
	if v, ok := s.Param("ValidSource"); ok {
		if source == NoCard || !Matches(g, g.Card(source), valid.Parse(v), h.Controller(), h.ID) {
			return false
		}
	}
	if v, ok := s.Param("Activator"); ok {
		if matched, recognized := matchesPlayerSpec(g, activator, h.Controller(), h.ID, v); !matched || !recognized {
			return false
		}
	}
	if v, ok := s.Param("SourceCanOnlyTarget"); ok && !ask.onlyTargets(v) {
		return false
	}
	return true
}

// saKindMatches is StaticAbility.matchesValidParam("ValidSA", ability) for a
// comma list of Spell, Activated, Triggered and SpellAbility (any). An
// activated mana ability is Activated too. recognized is false when the list
// holds a token this port does not classify.
func saKindMatches(spec, kind string) (matched, recognized bool) {
	recognized = true
	for _, tok := range strings.Split(spec, ",") {
		switch tok {
		case "SpellAbility":
			matched = true
		case causeSpell, causeTriggered:
			matched = matched || kind == tok
		case causeActivated:
			matched = matched || kind == causeActivated || kind == causeManaAbil
		default:
			recognized = false
		}
	}
	return matched, recognized
}

// hexproofValidSA is the ValidSA$ half of CardFactoryUtil's Hexproof branch:
// "Hexproof from triggered abilities" and "from activated abilities"
// (Triggered, Activated -- 2 real lines) name the kind of ability, not a
// source, and are matched against the ability's kind (saKindMatches).
func hexproofValidSA(details string) (sa string, ok bool) {
	validType, _, _ := strings.Cut(details, ":")
	if validType == "Triggered" || validType == "Activated" {
		return validType, true
	}
	return "", false
}
