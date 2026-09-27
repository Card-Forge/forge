// Valid-string matching: deciding whether a card satisfies a parsed
// internal/valid.Spec. internal/valid only parses the grammar (it must not
// import the engine, ADR-0003); this is the evaluation half its own doc
// comment says lands here.
//
// Ported from forge-game/src/main/java/forge/game/card/Card.java's
// isValid/hasProperty and the slice of CardProperty.java's 2,135-line
// cardHasProperty (plus, for color, CardStateProperty.java's own chain --
// colorMatches's own doc comment has the reason color lives there instead)
// this covers. CardProperty, CardStateProperty, PlayerProperty and
// SpellAbilityProperty together answer 928 residual property names
// (docs/crucible/porting/port-log/valid-strings.md); this is:
// ChosenCard/ChosenCardStrict/nonChosenCard, IsRemembered and IsImprinted --
// membership in source's own Memory lists (sourceCard's own doc comment) --
// EnchantedBy/EquippedBy/AttachedBy/FortifiedBy, bare form only (one check
// in Java too, before it ever reaches CardProperty), inZone/inRealZone (c's
// own Zone, LKI-collapsed the same way YouCtrl already is), attacking and
// blocking, bare form only (the current Combat's Attackers/Blocks),
// HasCounters and counters_<op><n>_<type> (countersMatches' own doc
// comment), enchanted/equipped/modified (attachedByType/isModified's own
// doc comments), RememberedPlayerCtrl/RememberedPlayerOwn (membership in
// source's own Memory, by player rather than by card) and ActivePlayerCtrl
// (c's controller against Game.ActivePlayer), Historic/Outlaw/Party (pure
// CardType checks, isHistoric/isTribalMember's own doc comments),
// controller/owner relative to sourceController (YouCtrl, YouDontCtrl,
// OppCtrl, YouOwn, YouDontOwn, OppOwn), identity relative to source (Self,
// Other, StrictlyOther), the five colors plus Colorless and MultiColor, a
// generic keyword check under three spellings (with/without/hasKeyword),
// tapped/untapped, the numeric comparisons (power, toughness, cmc and the
// rest of compareFields, crossed with
// LT/LE/EQ/GE/GT/NE/M2 -- compareMatches' own doc comment) for a
// plain-integer operand, the generic `non<Type>` fallback every chain
// shares, and the bare type/supertype/subtype fallthrough every chain ends
// on. The rest is M5-M6, corpus-frequency order
// (tools/vocabscan -kind validProperty), the same shape effect.go's Registry
// was always going to grow in (ADR-0011).

package engine

import (
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// Matches decides whether c satisfies spec, from sourceController's
// perspective, with source as the card the spec is written on -- a card's
// own `Enchant`/`ValidCard`, an ability's `ValidTgts`. Both PlayerID/CardID
// parameters are exactly `Ability.Controller()`/`Ability.Source` where the spec
// comes from a resolving ability, but Matches does not require one:
// `cleanupDanglingAttachments` (action.go)'s eventual `Enchant`-restriction
// check would call this with the Aura's own controller and the Aura itself,
// no `Ability` in sight. g resolves source to its own *Card when a property
// needs to read something off it (a Remembered/Imprinted/Chosen list --
// sourceCard's own doc comment); nothing else here needs the game.
//
// An alternative matches when its base and every property do (`Spec`'s own
// doc comment: alternatives are OR, properties within one are AND). A `!`
// on the base negates that whole AND, not just the base -- Java's
// `testFailed` short-circuit, reproduced exactly in altMatches, because
// getting this backwards silently inverts every negated valid string in the
// corpus. A `!` on a property negates only that property, which is the
// simple case (`Card.hasProperty`'s own wrapper).
func Matches(g *Game, c *Card, spec valid.Spec, sourceController PlayerID, source CardID) bool {
	for _, alt := range spec.Alternatives {
		if altMatches(g, c, alt, sourceController, source) {
			return true
		}
	}
	return false
}

func altMatches(g *Game, c *Card, alt valid.Alternative, sourceController PlayerID, source CardID) bool {
	if !baseMatches(c, alt.Base.Name) {
		return alt.Base.Negated
	}
	for _, p := range alt.Properties {
		ok := propertyMatches(g, c, p, sourceController, source)
		if p.Negated {
			ok = !ok
		}
		if !ok {
			return alt.Base.Negated
		}
	}
	return !alt.Base.Negated
}

// baseMatches is Card.isValid's own switch on the token before the first
// `.`. The named cases are Java's special ones; everything else falls
// through to a type/supertype/subtype check, the same fallthrough
// `getType().hasStringType(incR[0])` is in Java.
//
// Spell, Effect, Emblem and Boon never match: this port has nothing on the
// stack, no continuous-effect objects, no emblems and no boons yet, so a
// restriction naming one is a coverage gap, not a wrong answer -- the same
// as any SBA this port has not reached.
func baseMatches(c *Card, name string) bool {
	switch name {
	case "Permanent":
		return c.Type().IsPermanent()
	case "card", "Card":
		// Java excludes isImmutable() objects (Effect, Emblem, Boon) here.
		// Nothing this port creates is ever one, so every real card matches.
		return true
	case "Any":
		// CR 115's own "any target": a creature, planeswalker or Battle here
		// -- matchesPlayerBase's own "Any" case (above) is this card-side
		// case's own player-side sibling, the two together giving
		// targetCandidates (targeting.go) every legal "any target" answer.
		return c.Type().Has(cardtype.Creature) || c.Type().Has(cardtype.Planeswalker) || c.Type().Has(cardtype.Battle)
	case "Spell", "Effect", "Emblem", "Boon":
		return false
	default:
		return c.Type().HasStringType(name)
	}
}

// propertyMatches is the slice of CardProperty.cardHasProperty (and, for
// color, CardStateProperty.hasProperty -- colorMatches's own doc comment)
// this port answers. p.Compare is checked first and, when set, dispatches
// straight to compareMatches: internal/valid already parsed a numeric
// comparison out of p.Name at load time, so nothing below ever needs to
// re-derive one from the name string. `ChosenCard`/`IsRemembered`/
// `IsImprinted` come next, the one family here that reads source's own
// Card rather than c's or sourceController's -- sourceCard's own doc
// comment has the NoCard case. Ownership/control and identity
// branches are ported by name with `strings.HasPrefix` rather than `==`,
// because Java's own chain
// tests with `startsWith` (a property can carry a suffix argument on other
// branches this port does not reach, and reproducing the match style is
// what keeps a future addition from silently behaving differently on the
// bare token). `tapped`/`untapped` are exact-matched instead, the same
// choice colorMatches' own tokens make, since Java's `startsWith` there has
// no observed suffixed form in the corpus to preserve. Keyword, color and
// the generic `non<Type>` fallback come next, and a final fallthrough
// covers a bare type/supertype/subtype word used as a property, the same
// fallthrough CardState.hasProperty eventually reaches for one
// (`internal/cardtype.CoreTypeNames`'s own doc comment).
//
// Java's `YouCtrl`/`OppCtrl`/`YouOwn`/`OppOwn` compare against the
// controller/owner `game.getChangeZoneLKIInfo` resolves, not
// `card.getController()`/`card.getOwner()` directly -- last-known
// information for a card whose own zone change is mid-resolution. This
// port has no LKI tracking (game-state.md's "Not ported yet"), so these
// read `c.Controller()`/`c.Owner` as of now, which agrees with Java's LKI
// everywhere except the one moment a card's own leaving is what a property
// is trying to describe. `Self`/`Other`/`StrictlyOther` carry the same
// simplification one step further: Java's "Strictly" forms are
// game-timestamp-aware (telling a card from a same-named copy of itself
// apart), which this port also has no tracking for, so they read
// identically to their non-"Strictly" counterparts.
//
// `OppCtrl`/`OppOwn` are `X.getOpponents().contains(sourceController)` in
// Java, which is team-aware. This port has no team system, so both read as
// "controlled/owned by anyone other than sourceController" -- correct for
// every game this port can play today (two players, or free-for-all with
// no teams), wrong only once a team variant exists to disagree with it.
func propertyMatches(g *Game, c *Card, p valid.Property, sourceController PlayerID, source CardID) bool {
	name := p.Name
	// CR 702.26b: a phased-out permanent has no property unless the property
	// asks for a phased-out one -- "phasedOutSelf" reads "Self" on it, every
	// other property false (CardProperty.java:50-56). The base type is not
	// a property: a bare "Creature" still matches it, Card.isValid's own
	// split (Card.java:5751-5763). Checked before the comparison dispatch,
	// as Java's guard is at the top of cardHasProperty. A negated property
	// reads true on a phased-out card, Java's own hasProperty negation.
	if c.IsPhasedOut() {
		rest, ok := strings.CutPrefix(name, "phasedOut")
		if !ok {
			return false
		}
		name = rest
	} else if p.Compare != nil {
		return compareMatches(c, *p.Compare)
	}
	switch {
	case strings.HasPrefix(name, "phasedOut"):
		// Reached for a phased-in card, or a doubled prefix
		// (CardProperty.java:1051-1054).
		return c.IsPhasedOut()
	case strings.HasPrefix(name, "phasedIn"):
		return !c.IsPhasedOut()
	case name == "Permanent":
		// CardProperty.java:90, "Card.phasedOutPermanent"'s own tail.
		return c.Type().IsPermanent()
	case name == "token":
		// CardProperty.java:1336's exact form; its "tokenCreated..." suffixes
		// fall through to the type fallback below, false for every card.
		return c.IsToken
	case name == "EffectSource":
		// CardProperty.java:424: c is the card that created source, an
		// effect card (effecteffect.go's effectLifetime.host).
		sc, ok := sourceCard(g, source)
		return ok && sc.IsEffect && sc.effectLife.host != NoCard && sc.effectLife.host == c.ID
	case strings.HasPrefix(name, "ChosenCard"):
		// ChosenCardStrict collapses to ChosenCard: Java's "Strict" form
		// additionally checks equalsWithGameTimestamp, telling a chosen card
		// from a same-named copy of itself apart across a zone change this
		// port has no game-timestamp tracking for -- the same simplification
		// Self/StrictlyOther's own doc comment already makes.
		sc, ok := sourceCard(g, source)
		return ok && containsCard(sc.Memory.Chosen(), c.ID)
	case name == "nonChosenCard":
		sc, ok := sourceCard(g, source)
		return ok && !containsCard(sc.Memory.Chosen(), c.ID)
	case name == "ChosenType" || name == "IsNotChosenType" || name == "ChosenType2":
		// CardStateProperty: a chosen "Non<Type>" inverts ChosenType; an
		// unset choice matches no type.
		sc, ok := sourceCard(g, source)
		if !ok {
			return false
		}
		chosen := sc.Memory.ChosenType(name == "ChosenType2")
		if chosen == "" {
			return name == "IsNotChosenType"
		}
		if name == "IsNotChosenType" {
			return !c.Type().HasStringType(chosen)
		}
		if rest, ok := strings.CutPrefix(chosen, "Non"); ok && name == "ChosenType" && rest != "" {
			return !c.Type().HasStringType(strings.ToUpper(rest[:1]) + rest[1:])
		}
		return c.Type().HasStringType(chosen)
	case name == "IsSuspected":
		return c.Suspected
	case name == "IsRingbearer":
		return g.isRingBearer(c)
	case name == "IsSolved":
		return c.Solved
	case name == "NamedCard":
		sc, ok := sourceCard(g, source)
		if !ok || c.Def == nil {
			return false
		}
		return containsString(sc.Memory.NamedCards(), c.Def.Name)
	case name == "NamedByRememberedPlayer":
		sc, ok := sourceCard(g, source)
		if !ok || c.Def == nil {
			return false
		}
		for _, e := range sc.Memory.Remembered() {
			if p, isPlayer := e.AsPlayer(); isPlayer && g.Player(p).NamedCard != c.Def.Name {
				return false
			}
		}
		return true
	case name == "cmcChosenEvenOdd" || name == "cmcNotChosenEvenOdd":
		// CardProperty: false while the source has no pick; otherwise the
		// card's mana value parity against it, inverted for "Not".
		sc, ok := sourceCard(g, source)
		if !ok || sc.Memory.ChosenEvenOdd() == "" {
			return false
		}
		match := (c.CMC()%2 == 0) == (sc.Memory.ChosenEvenOdd() == "Even")
		return match == (name == "cmcChosenEvenOdd")
	case name == "TopLibrary":
		// CardProperty.java:610-624's plain form: c is the top card of its
		// owner's library -- index 0, the end DrawCards reads from. The
		// "TopLibrary_<valid>"/"BottomLibrary" forms are not resolved.
		lib := g.Zone(Library, c.Owner).Cards()
		return len(lib) > 0 && lib[0] == c.ID
	case name == "IsRemembered":
		sc, ok := sourceCard(g, source)
		return ok && containsEntity(sc.Memory.Remembered(), CardEntity(c.ID))
	case name == "IsImprinted":
		sc, ok := sourceCard(g, source)
		return ok && containsCard(sc.Memory.Imprinted(), c.ID)
	case name == "EnchantedBy", name == "EquippedBy", name == "AttachedBy", name == "FortifiedBy":
		// All four are one check in Java too: GameEntity.isEnchantedBy,
		// isEquippedBy and isFortifiedBy each just call hasCardAttachment,
		// and hasCardAttachment is getAttachedCards().contains(c) --
		// GameEntity.java's own comment on isEnchantedBy: "Even if c is no
		// Aura it still counts". This port's Attach/Unattach is one
		// mechanism for Auras, Equipment and Fortifications alike
		// (card.go's own doc comment on Attachments), so there is nothing
		// left to distinguish between the four names for the bare form --
		// exact-matched, not prefix-matched, because a trailing restriction
		// ("EnchantedBy Aura.YouCtrl", "EquippedByTargeted") is a nested
		// valid.Spec matched against each attachment, or an ability's
		// current targets, neither of which this covers yet. That is a
		// genuine gap, but a small one: 2,338 of 2,345 occurrences of the
		// four names are the bare form, negated or not (tools/vocabscan
		// -kind validProperty), and a suffixed name simply is not equal to
		// any of the four cases here, so it falls through to the same
		// "false for every card" answer any other unimplemented property
		// gets.
		return containsCard(c.Attachments(), source)
	case strings.HasPrefix(name, "inRealZone"):
		// inRealZone reads c.Zone directly, no LKI involved (card.isInZone).
		zone, ok := ZoneByName(strings.TrimPrefix(name, "inRealZone"))
		return ok && c.Zone == zone
	case strings.HasPrefix(name, "inZone"):
		// inZone reads Java's LKI-derived zone, which "falls back" to the
		// object's own current zone once there is no better LKI (Java's own
		// comment on the branch) -- this port has no LKI tracking
		// (YouCtrl's own doc comment already makes the same simplification),
		// so inZone and inRealZone read identically here: both are just
		// c.Zone.
		zone, ok := ZoneByName(strings.TrimPrefix(name, "inZone"))
		return ok && c.Zone == zone
	case name == "attacking":
		// Java checks combat != nil before card.isAttacking(); this port has
		// no nil combat, only a zero-valued one, but Attackers is empty
		// either way when no attack was declared, so containsCard answers
		// the same "false" a nil combat would without a separate check.
		// Reads g.combat.Attackers directly rather than calling g.Attackers()
		// (attack.go): that exported accessor is nothing more than this same
		// field read, and calling it would make this "valid" group depend on
		// "attack" for no reason beyond a wrapper -- enginelint would then
		// forbid "attack"/"block" from ever depending on "valid" back, which
		// cantBlockBy (staticability.go) needs to.
		return containsCard(g.combat.Attackers, c.ID)
	case name == "DefenderCtrl":
		// CardProperty.java:197-212: c's controller is the player the
		// source is attacking, or the controller of the planeswalker or
		// battle it attacks (Combat.getDefendingPlayerRelatedTo,
		// Combat.java:452-463: an Aura, Fortification or Equipment source
		// stands for the creature it is attached to). Read off
		// AttackTargets directly, for the reason "attacking" (above) gives.
		// Every ValidTgts$/Valid*$ naming it (47 corpus files) resolves
		// through here; the ForRemembered suffix is a different name and
		// never matches.
		attacker := source
		if host, ok := sourceCard(g, source); ok {
			if t := host.Type(); t.HasSubtype("Aura") || t.HasSubtype("Equipment") || t.HasSubtype("Fortification") {
				attacker, _ = host.AttachedTo()
			}
		}
		target, ok := g.combat.AttackTargets[attacker]
		if !ok {
			return false
		}
		if pid, isPlayer := target.AsPlayer(); isPlayer {
			return c.Controller() == pid
		}
		cid, _ := target.AsCard()
		return g.Card(cid).Controller() == c.Controller()
	case name == "blocking":
		return isBlocking(g.combat.Blocks, c.ID)
	case name == "HasCounters":
		return c.Counters.Any()
	case strings.HasPrefix(name, "counters_"):
		return countersMatches(c, name)
	case name == "enchanted":
		return attachedByType(g, c, "Aura")
	case name == "equipped":
		return attachedByType(g, c, "Equipment")
	case name == "modified":
		return isModified(g, c)
	// RememberedPlayerCtrl/RememberedPlayerOwn ask whether c's controller/owner
	// is among the players source has remembered -- CardProperty.java's own
	// ternary picks the field by whether the property string ends in "Ctrl",
	// not by matching a name against a fixed set of two, so a third suffix
	// this port has never seen would silently fall to the owner check in
	// Java too. Exact-matching the two names the corpus actually writes is
	// safer than reproducing that ternary: a `$GreatestCardManaCost` tail
	// (2 occurrences on RememberedPlayerCtrl, 1 on RememberedPlayerOwn,
	// tools/vocabscan -kind validProperty) does not end in "Ctrl" either, so
	// Java's own ternary reads it as an owner check regardless of which name
	// it is attached to -- a coupling to an unrelated Count$-style suffix
	// this port has no reason to reproduce when it does not resolve that
	// suffix at all. Both fall through to a coverage gap here instead.
	case name == "RememberedPlayerCtrl":
		sc, ok := sourceCard(g, source)
		return ok && containsEntity(sc.Memory.Remembered(), PlayerEntity(c.Controller()))
	case name == "RememberedPlayerOwn":
		sc, ok := sourceCard(g, source)
		return ok && containsEntity(sc.Memory.Remembered(), PlayerEntity(c.Owner))
	// ActivePlayerCtrl is c's controller relative to whose turn it is, not
	// relative to sourceController -- Game.ActivePlayer already exists
	// (turn.go); nothing new to build.
	case name == "ActivePlayerCtrl":
		return c.Controller() == g.ActivePlayer()
	// Historic, Outlaw and Party are all CardType's own methods
	// (forge-core/src/main/java/forge/card/CardType.java) -- pure type
	// checks, nothing that needed a Def this port didn't already read for
	// the bare type/supertype/subtype fallthrough below.
	case name == "Historic":
		return isHistoric(c.Type())
	case name == "Outlaw":
		return isTribalMember(c.Type(), outlawTypes)
	case name == "Party":
		return isTribalMember(c.Type(), partyTypes)
	case strings.HasPrefix(name, "YouCtrl"):
		return c.Controller() == sourceController
	case strings.HasPrefix(name, "YouDontCtrl"):
		return c.Controller() != sourceController
	case strings.HasPrefix(name, "OppCtrl"):
		return c.Controller() != sourceController
	case strings.HasPrefix(name, "YouDontOwn"):
		return c.Owner != sourceController
	case strings.HasPrefix(name, "YouOwn"):
		return c.Owner == sourceController
	case strings.HasPrefix(name, "OppOwn"):
		return c.Owner != sourceController
	case strings.HasPrefix(name, "StrictlyOther"), strings.HasPrefix(name, "Other"):
		// StrictlySelf/StrictlyOther are Java's game-timestamp-aware forms
		// of Self/Other, for telling a card from a same-named copy of
		// itself apart. This port has no LKI/game-timestamp tracking
		// (game-state.md's "Not ported yet"), so both read as plain
		// identity, the same simplification Self's own doc comment already
		// makes for YouCtrl/OppCtrl's LKI gap.
		return c.ID != source
	case strings.HasPrefix(name, "Self"):
		return c.ID == source
	case name == "tapped":
		return c.Tapped
	case name == "untapped":
		return !c.Tapped
	case name == "SharesColorWith":
		// CardProperty.java's bare form (card.sharesColorWith(source)) --
		// the colorless check Java does explicitly on c falls out for free
		// here, since HasAny(0) is always false whichever side is
		// colorless. A suffixed form (SharesColorWith MostProminentColor,
		// SharesColorWithOther <restriction>, ...) reads a game-wide or
		// remembered-list comparison this port has no evaluator for and is
		// not matched by this exact-equality case, so it falls through to
		// the same "false for every card" answer any other unimplemented
		// property gets -- 5 of 26 literal corpus occurrences are the bare
		// form. Every one of Intimidate's own 23 real K:Intimidate cards
		// hard-codes the bare form too (CardFactoryUtil.java's own keyword
		// expansion, not literal script text, so it never shows up in that
		// count), the actual reason this case exists (staticability.go's
		// own cantBlockByKeywords).
		sc, ok := sourceCard(g, source)
		return ok && c.Colors().HasAny(sc.Colors())
	}
	if rest, ok := strings.CutPrefix(name, "without"); ok {
		return !c.HasKeyword(rest)
	}
	if rest, ok := strings.CutPrefix(name, "with"); ok {
		// "without" is checked first: it also starts with "with", and
		// stripping the shorter prefix from it would leave "out<Keyword>"
		// instead of the keyword name, the same ordering mistake Java's own
		// nested if avoids by checking the longer prefix first.
		return c.HasKeyword(rest)
	}
	if rest, ok := strings.CutPrefix(name, "hasKeyword"); ok {
		return c.HasKeyword(rest)
	}
	if base, ok := strings.CutSuffix(name, "Source"); ok {
		if matched, ok := sourceColorMatches(g, c, base); ok {
			return matched
		}
	}
	if color, mustHave, ok := colorMatches(name); ok {
		return mustHave == c.Colors().Has(color)
	}
	switch name {
	case "Colorless":
		return c.Colors().IsColorless()
	case "nonColorless":
		return !c.Colors().IsColorless()
	case "MultiColor":
		return c.Colors().Count() > 1
	}
	if rest, ok := strings.CutPrefix(name, "non"); ok {
		// CardStateProperty.java's own generic tail, reached once none of
		// its named branches (color included, checked first, above) claim
		// the property -- "nonLand", "nonCreature", "nonArtifact", and
		// every other `non<Type>` this port's `cardtype` recognizes.
		return !c.Type().HasStringType(rest)
	}
	return c.Type().HasStringType(name)
}

// sourceColorMatches is CardStateProperty.hasProperty's withSource form of
// its color branches -- "RedSource", "nonWhiteSource", "ColorlessSource",
// base being the property name with its "Source" suffix already stripped.
// Java reads c's own colors, except that c counts as colorless while a
// Mode$ ColorlessDamageSource static ability applies to it
// (StaticAbilityColorlessDamageSource.colorlessDamageSource, ghostly_flame.
// txt, the corpus's only such static). ok is false when base is not a
// color property at all ("ExiledWith", "Effect", ...), leaving name to the
// branches below.
func sourceColorMatches(g *Game, c *Card, base string) (matched, ok bool) {
	color, mustHave, isColor := colorMatches(base)
	isColorless := base == "Colorless" || base == "nonColorless"
	if !isColor && !isColorless {
		return false, false
	}
	colors := c.Colors()
	if colorlessDamageSource(g, c.ID) {
		colors = 0
	}
	if isColorless {
		return (base == "Colorless") == colors.IsColorless(), true
	}
	return mustHave == colors.Has(color), true
}

// colorlessDamageSource is StaticAbilityColorlessDamageSource.
// colorlessDamageSource: whether any Mode$ ColorlessDamageSource static
// ability whose ValidCard$ matches id is in play, making id a colorless
// source. Only battlefield hosts are scanned, the same reach
// ignoreLegendRule's own scan (staticability.go) has: Java walks
// ZoneType.STATIC_ABILITIES_SOURCE_ZONES, but the corpus's one such static
// (ghostly_flame.txt, an Enchantment with no EffectZone$) is only ever
// active on the battlefield. A missing ValidCard$ matches every card, Java's
// own matchesValidParam default.
func colorlessDamageSource(g *Game, id CardID) bool {
	return eachColorlessDamageSource(g, func(h *Card, validCard string, ok bool) bool {
		return !ok || Matches(g, g.Card(id), valid.Parse(validCard), h.Controller(), h.ID)
	})
}

// colorlessDamageSourceInPlay reports whether any Mode$
// ColorlessDamageSource static is on the battlefield at all -- for a
// caller that must refuse a shape colorlessDamageSource cannot answer yet
// (a spell on the stack: baseMatches has no Spell case).
func colorlessDamageSourceInPlay(g *Game) bool {
	return eachColorlessDamageSource(g, func(*Card, string, bool) bool { return true })
}

// eachColorlessDamageSource calls f with each battlefield Mode$
// ColorlessDamageSource static's host and ValidCard$, stopping at the
// first true.
func eachColorlessDamageSource(g *Game, f func(h *Card, validCard string, ok bool) bool) bool {
	for _, pid := range g.Players() {
		for _, host := range g.Zone(Battlefield, pid).Cards() {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "ColorlessDamageSource") {
						continue
					}
					validCard, ok := s.Param("ValidCard")
					if f(h, validCard, ok) {
						return true
					}
				}
			}
		}
	}
	return false
}

// colorMatches is CardStateProperty.hasProperty's color branch (White,
// Blue, Black, Red, Green, each with a `non` form), exact-matched rather
// than Java's `Contains`/prefix-stripped form: the "Source" suffix
// (`WhiteSource`) is stripped by propertyMatches before it gets here
// (sourceColorMatches), and an exact match is what keeps any other suffixed
// or compound name from being silently misread as bare "White".
//
// mustHave mirrors Java's own local of the same name: false for the `non`
// form, meaning the card must lack the color rather than carry it.
func colorMatches(name string) (color mana.Colors, mustHave bool, ok bool) {
	mustHave = true
	colorName := name
	if rest, isNon := strings.CutPrefix(name, "non"); isNon {
		mustHave, colorName = false, rest
	}
	color, ok = colorFromName(colorName)
	return color, mustHave, ok
}

// colorFromName maps one of the five color words to its mana.Colors bit --
// colorMatches' own switch, pulled out so a caller that needs the bare
// name-to-color mapping without the "non" prefix handling (continuouslayers.go's
// layerColorChange, Layer 5's own AddColor$/SetColor$) does not duplicate it.
func colorFromName(name string) (mana.Colors, bool) {
	switch name {
	case "White":
		return mana.White, true
	case "Blue":
		return mana.Blue, true
	case "Black":
		return mana.Black, true
	case "Red":
		return mana.Red, true
	case "Green":
		return mana.Green, true
	}
	return 0, false
}

// matchesPlayerBase is CardTraitBase's own bare "You"/"Opponent"/"Player"
// dispatch against a Player rather than a Card, shared by every
// ValidXPlayer-shaped check this port has needed so far -- directly, where
// the real corpus never qualifies the value with a dotted property
// (CantBlockBy's own ValidDefender, matchesValidDefender, staticability.go;
// Discarded's own ValidPlayer and Taps's own ValidPlayer, trigger.go), and
// through matchesPlayerSpec (below), where it sometimes does (SpellCast's
// own ValidActivatingPlayer, DamageDone's own ValidTarget when the damaged
// object is a player, and TapsForMana's own Activator). None of these is a
// *Card, so Matches itself cannot answer any of them. "Opponent"/"You" reuse
// the same no-team simplification OppCtrl/OppOwn already carry (this file's
// own doc comment on propertyMatches): "controlled by anyone other than
// host" stands in for getOpponents().contains(candidate). ok is false for
// spec anything else, so a caller with its own additional dispatch
// (matchesValidDefender's own "Player.controls<Type>") knows to keep
// looking rather than treat an unrecognized spec as a plain non-match.
func matchesPlayerBase(candidate, host PlayerID, spec string) (matched, ok bool) {
	switch spec {
	case "You":
		return candidate == host, true
	case "Opponent":
		return candidate != host, true
	case "Player", "Any":
		// "Any" (CR 115's own "any target") matches every player the same as
		// bare "Player" does -- Player.isValid's own literal "Any" case
		// (Player.java) applies no further restriction, the identical
		// unconditional match baseMatches' own "Any" case (below) gives a
		// creature/planeswalker/Battle candidate on the card side.
		return true, true
	}
	return false, false
}

// matchesPlayerSpec is matchesPlayerBase plus the one dotted-property layer
// real corpus lines put on top of it, plus Player.isValid's own comma-as-OR
// split (CardTraitBase.java's own restriction-string split, the identical
// comma valid.Parse already gives Matches' own *Card* side, valid.go's own
// doc comment) -- Player.isValid splits its restriction string on the first
// "." within each comma-separated alternative, checks the base clause first
// (matchesPlayerBase, above), then ANDs every "+"-joined property via
// hasProperty/PlayerProperty.playerHasProperty. No real corpus line this
// port's callers pass ever joins more than one property this way (SpellCast's
// own ValidActivatingPlayer$ Opponent.NonActive is the single "+"-free
// two-clause example, game-state.md's own count), so only Base.Property is
// split within an alternative, not Base.Property1+Property2.
//
// An alternative whose base is none of You/Opponent/Player -- DamageDone's
// own ValidTarget$ You,Permanent.YouCtrl, Permanent,Player (reidane_god_of_the_worthy_valkmira_protectors_shield.txt's/
// plated_pegasus.txt's/gratuitous_violence.txt's own real "or a permanent you
// control"/"or player" half of an OR that also covers the damaged player
// directly) -- matches nothing, the identical "an unrecognized base matches
// nothing" contract valid.Parse's own doc comment already gives the card
// side, rather than aborting the whole spec: this is what lets a mixed
// player/card ValidTarget$ resolve its own player-shaped alternative at all
// (damageReplaced's own doc comment, replacement.go, has the full count).
//
// ok is false only once every alternative has been checked and none matched:
// at least one alternative names a property matchesPlayerProperty does not
// recognize, and no other alternative matched outright (a caller with its own
// further dispatch, e.g. matchesValidDefender's own "Player.controls<Type>",
// checks that first and never reaches this function for those specs). A
// definite match on any alternative returns true immediately regardless of
// any other alternative's own property being unrecognized, since
// Player.isValid's own OR is true the moment one side is.
//
// source is the trigger/ability's own host card, threaded through only for
// matchesPlayerProperty's own EnchantedController case, below -- every other
// property this function resolves needs no card at all, so a caller with
// none in scope (matchesActivatingPlayer's own SpellCast dispatch, at the
// point this port first built it, before EnchantedController existed) simply
// passes NoCard the same way Matches' own callers already pass it for a
// property that never reads source (sourceCard's own doc comment, below).
func matchesPlayerSpec(g *Game, candidate, host PlayerID, source CardID, spec string) (matched, ok bool) {
	sawPlayerBase := false
	sawUnrecognizedProperty := false
	for _, alt := range strings.Split(spec, ",") {
		base, property, hasProperty := strings.Cut(alt, ".")
		baseMatched, baseOK := matchesPlayerBase(candidate, host, base)
		if !baseOK {
			continue
		}
		sawPlayerBase = true
		if !hasProperty {
			if baseMatched {
				return true, true
			}
			continue
		}
		propMatched, propOK := matchesPlayerProperty(g, candidate, host, source, property)
		if !propOK {
			sawUnrecognizedProperty = true
			continue
		}
		if baseMatched && propMatched {
			return true, true
		}
	}
	if sawUnrecognizedProperty {
		return false, false
	}
	return false, sawPlayerBase
}

// matchesPlayerProperty is matchesPlayerSpec's own property half, ported
// from the slice of PlayerProperty.playerHasProperty real corpus lines this
// port can evaluate: a bare You/Opponent/Player value (Java's own
// "Opponent"/"You" property branches reuse the identical base check a
// property token gets, so this does too, via matchesPlayerBase), Active/
// NonActive (Game.ActivePlayer(), the same accessor Matches' own
// ActivePlayerCtrl property already reads for a *Card, valid.go), Other
// (not sourceController -- Java's own distinction from "Opponent," a
// teammate counts as "Other" but not "Opponent," collapses into the
// identical check anyway under this port's own no-team simplification,
// matchesPlayerBase's own doc comment), EnchantedController (the controller
// of whatever source -- an Aura -- is attached to, source.AttachedTo(),
// card.go; a source with no attachment, or none passed at all, recognizes
// the property but matches no candidate, PlayerProperty.java's own null
// check ported directly rather than "cannot evaluate," since an unattached
// Aura is an ordinary, expected state to ask this about, not a shape this
// port fails to understand), and descended (Player.DescendedThisTurn,
// player.go -- CR's own "descend" tracker, Java's own getDescended() < 1,
// this port needing only the boolean "at all" question every real corpus
// line asks), and VenturedThisTurn (Player.VenturedThisTurn, at least one
// venture this turn -- PlayerProperty.java:482's getVenturedThisTurn() < 1,
// Keen-Eared Sentry's CantVenture).
//
// Every other real property (EnchantedBy and Chosen on a *player* -- an
// Aura enchanting a player directly, CR 303.4h, and a ChosenPlayer memory
// slot -- distinct from Matches' own *card*-side EnchantedBy, which this
// port already resolves) needs state this port does not track at all yet,
// and is left unrecognized here, ok=false, the same "skip rather than
// guess" contract every other unresolved param in this port already has
// (GO-7).
func matchesPlayerProperty(g *Game, candidate, host PlayerID, source CardID, property string) (matched, ok bool) {
	if matched, ok := matchesPlayerBase(candidate, host, property); ok {
		return matched, true
	}
	switch property {
	case "Active":
		return candidate == g.ActivePlayer(), true
	case "NonActive":
		return candidate != g.ActivePlayer(), true
	case "Other":
		return candidate != host, true
	case "EnchantedController":
		sc, ok := sourceCard(g, source)
		if !ok {
			return false, true
		}
		enchanting, attached := sc.AttachedTo()
		if !attached {
			return false, true
		}
		return candidate == g.Card(enchanting).Controller(), true
	case "descended":
		return g.Player(candidate).DescendedThisTurn, true
	case "VenturedThisTurn":
		return g.Player(candidate).VenturedThisTurn > 0, true
	}
	return false, false
}

// sourceCard resolves source to its own *Card, for the properties that read
// something off the card the spec is written on rather than the candidate c
// -- ChosenCard, IsRemembered, IsImprinted. Game.Card panics on NoCard
// (GO-7: that is an engine invariant breach everywhere else it is called),
// but a Matches caller legitimately passes NoCard when there is no
// meaningful source at all (Matches' own doc comment: the base/property
// checks that do not need one). ok is false in exactly that case, so a
// property that needs a source but was not given one matches nothing, the
// same "false for every card" answer any other unresolvable property gives,
// rather than panicking on a caller that was never wrong to omit one.
func sourceCard(g *Game, source CardID) (*Card, bool) {
	if source == NoCard {
		return nil, false
	}
	return g.Card(source), true
}

// containsCard and containsEntity are linear membership checks over a
// Memory list (Chosen/Imprinted/Remembered) -- these lists hold at most a
// handful of entries (memory.go's own doc comment: "the overwhelming
// majority of cards remember nothing"), so a set is not worth building for
// them the way collect.OrderedSet already is for the list itself.
func containsCard(list []CardID, id CardID) bool {
	for _, x := range list {
		if x == id {
			return true
		}
	}
	return false
}

func containsEntity(list []EntityID, e EntityID) bool {
	for _, x := range list {
		if x == e {
			return true
		}
	}
	return false
}

// isHistoric is CardType.isHistoric: Legendary, Artifact, or a Saga --
// CR's own "historic" umbrella, three different chains (a supertype, a
// core type, a subtype) that a printed card can satisfy any one of.
func isHistoric(t cardtype.Line) bool {
	return t.HasSupertype(cardtype.Legendary) || t.Has(cardtype.Artifact) || t.HasSubtype("Saga")
}

// outlawTypes and partyTypes are CardType.Constant.OUTLAW_TYPES/PARTY_TYPES
// (forge-core/src/main/java/forge/card/CardType.java) -- the fixed
// creature-type sets the Outlaw (Assassin, Mercenary, Pirate, Rogue,
// Warlock) and Party (Cleric, Rogue, Warrior, Wizard) valid-string
// properties name. Neither list is derivable from the type grammar itself;
// both are closed, hand-picked sets a rules text names by mechanic.
var (
	outlawTypes = []string{"Assassin", "Mercenary", "Pirate", "Rogue", "Warlock"}
	partyTypes  = []string{"Cleric", "Rogue", "Warrior", "Wizard"}
)

// isTribalMember is CardType.isOutlaw/isParty's shared shape: a Creature or
// Kindred card with at least one of names as a subtype. Kindred is checked
// alongside Creature because both mechanics predate typal(Kindred)
// permanents that carry a creature type without being a Creature
// themselves, and CardType.java checks both the same way.
func isTribalMember(t cardtype.Line, names []string) bool {
	if !t.Has(cardtype.Creature) && !t.Has(cardtype.Kindred) {
		return false
	}
	for _, n := range names {
		if t.HasSubtype(n) {
			return true
		}
	}
	return false
}

// isBlocking reports whether id is a Blocker in any Block -- CardProperty's
// own "blocking" branch reads combat.isBlocking(card), true the moment a
// card blocks anything at all, gang block or not.
func isBlocking(blocks []Block, id CardID) bool {
	for _, b := range blocks {
		if b.Blocker == id {
			return true
		}
	}
	return false
}

// attachedByType reports whether any of c's attachments carries the named
// subtype -- GameEntity.isEnchanted/isEquipped in Java
// (`getAttachedCards().anyMatch(Card::isAura)`/`Card::isEquipment`), read
// here by resolving each attachment through g rather than a predicate,
// since this port has no Card::isAura-shaped method to call in bulk.
func attachedByType(g *Game, c *Card, subtype string) bool {
	for _, id := range c.Attachments() {
		if g.Card(id).Type().HasSubtype(subtype) {
			return true
		}
	}
	return false
}

// isModified is CR 707.9's own name for Card.isModified: c has a counter of
// any kind, an Equipment attached, or an Aura attached that its own
// controller controls -- Java's exact three-way OR
// (`isEquipped() || hasCounters() || getEnchantedBy().anyMatch(isController(controller))`).
// The Aura leg is not plain attachedByType(g, c, "Aura"): "modified" cares
// who controls the enchanting Aura, an ordinary "enchanted" check does not,
// so it gets its own loop rather than reusing that one with a filter bolted
// on.
func isModified(g *Game, c *Card) bool {
	if attachedByType(g, c, "Equipment") || c.Counters.Any() {
		return true
	}
	for _, id := range c.Attachments() {
		aura := g.Card(id)
		if aura.Type().HasSubtype("Aura") && aura.Controller() == c.Controller() {
			return true
		}
	}
	return false
}

// compareMatches is the numeric-comparison branch of CardProperty.java:1423
// ("power"/"basePower"/"toughness"/"baseToughness"/"cmc"/"totalPT"/
// "numColors"/"numTypes", each crossed with LT/LE/EQ/GE/GT/NE/M2) --
// internal/valid.parseCompare already split the property into Field,
// Operator and Operand at load time (that package's own doc comment says
// evaluation waits here).
//
// Operand is only handled when it is a plain base-10 integer. Java resolves
// it with AbilityUtils.calculateAmount, which also accepts "X", "Chosen"
// (source.getChosenNumber()) and an SVar name -- none of which this port can
// resolve without an ability-context evaluator internal/expr does not have
// yet (compare.go's own doc comment: "resolving it needs a game"). A
// non-numeric Operand is a coverage gap, so the property matches nothing,
// the same as any other unimplemented property -- not a wrong answer for
// the common numeric case, which is what the corpus mostly uses these for.
func compareMatches(c *Card, cmp valid.Compare) bool {
	operand, err := strconv.Atoi(cmp.Operand)
	if err != nil {
		return false
	}
	value, ok := compareFieldValue(c, cmp.Field)
	if !ok {
		return false
	}
	return compareOp(value, cmp.Operator, operand)
}

// compareFieldValue reads the measured field CardProperty.java:1432-1451
// names. power/toughness are the full current values (Power/Toughness,
// Layer 7 and counters both folded in -- Java's getNetPower/getNetToughness).
// basePower/baseToughness are Layer 7 folded in but counters not yet added
// (layer7Power/layer7Toughness's own doc comment has the reason this is not
// BasePower/BaseToughness despite the name). totalPT is full power plus full
// toughness. numColors and numTypes have no unresolvable form, so they are
// always ok.
//
// ok is false wherever the underlying accessor's is -- an unresolvable "*"
// or Count$ printed value this port has no expr evaluator to resolve
// (BasePower's own doc comment), propagated rather than guessed at.
func compareFieldValue(c *Card, field string) (int, bool) {
	switch field {
	case "power":
		return c.Power()
	case "basePower":
		return c.layer7Power()
	case "toughness":
		return c.Toughness()
	case "baseToughness":
		return c.layer7Toughness()
	case "cmc":
		return c.CMC(), true
	case "totalPT":
		p, okP := c.Power()
		t, okT := c.Toughness()
		return p + t, okP && okT
	case "numColors":
		return c.Colors().Count(), true
	case "numTypes":
		return len(c.Type().CoreTypes()), true
	}
	return 0, false
}

// compareOp is Expressions.compare (forge/util/Expressions.java), ported
// operator for operator including M2's modulo-2 equality (a creature's power
// and an operand agreeing on even/odd, not on value).
func compareOp(left int, operator string, right int) bool {
	switch operator {
	case "LT":
		return left < right
	case "LE":
		return left <= right
	case "EQ":
		return left == right
	case "GE":
		return left >= right
	case "GT":
		return left > right
	case "NE":
		return left != right
	case "M2":
		return left%2 == right%2
	}
	return false
}

// countersMatches is CardProperty.java's counters_ branch (its own comment:
// "syntax example: counters_GE9_P1P1 or counters_LT12_TIME") -- a second,
// independent numeric-comparison shape from compareMatches' own, so it gets
// its own split rather than folding into internal/valid's Compare (that
// grammar has no field name to key off before the operator; here the field
// is the whole property, "counters", so it is always the same one
// measurement, `Card.Counters`, crossed with an operator, an operand and a
// CounterType name, "_"-separated instead of packed into one token).
//
// name is split on "_" into exactly three parts, `counters`, an
// operator+operand run together the way compareMatches' own tokens are, and
// a CounterType name -- the caller's `counters_` prefix match already spent
// the underscore that separates them. `countersReceivedThisTurn_<op><n>_<type>_<player>`
// (Java: `splitProperty[0].endsWith("ReceivedThisTurn")`, a per-turn count
// this port does not track, game-state.md's "Not ported yet") never reaches
// here at all: Java's own property spells that segment with no underscore
// of its own, "countersReceivedThisTurn", so it does not match the caller's
// `counters_` prefix either -- the three-part check below is defensive, not
// what does the excluding. CounterType is an open string (counters.go's own
// doc comment), so the type name needs no lookup, only a cast.
func countersMatches(c *Card, name string) bool {
	parts := strings.Split(name, "_")
	if len(parts) != 3 {
		return false
	}
	operator, ok := operatorPrefix(parts[1])
	if !ok {
		return false
	}
	operand, err := strconv.Atoi(strings.TrimPrefix(parts[1], operator))
	if err != nil {
		return false
	}
	return compareOp(c.Counters.Count(CounterType(parts[2])), operator, operand)
}

// counterOperators is compareOp's own operator set, Java's order
// (Expressions.compare's own chain of `contains` checks) -- internal/valid's
// own compareOperators is unexported and parses a different token shape
// (countersMatches' own doc comment on why counters_ gets its own split), so
// this is its own copy rather than a shared one.
var counterOperators = [...]string{"LT", "LE", "EQ", "GE", "GT", "NE", "M2"}

// operatorPrefix finds which of counterOperators parts[1] (an operator
// immediately followed by its operand, "GE1", "LT12") starts with. Unlike
// compareOp's own containment search, this one has to anchor on the prefix:
// the operand that follows is arbitrary digits, and nothing else in "GE1"
// could contain a second operator's two letters by accident, so a prefix
// check is enough (and, unlike compareFields' offsets, needs no
// per-operator length table -- every operator here is exactly two
// characters).
func operatorPrefix(s string) (string, bool) {
	for _, operator := range counterOperators {
		if strings.HasPrefix(s, operator) {
			return operator, true
		}
	}
	return "", false
}
