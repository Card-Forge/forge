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
// ChosenCard/ChosenCardStrict/nonChosenCard, ChosenColor, IsRemembered and IsImprinted --
// membership in source's own Memory lists (sourceCard's own doc comment) --
// EnchantedBy/EquippedBy/AttachedBy/FortifiedBy, bare form only (one check
// in Java too, before it ever reaches CardProperty), inZone/inRealZone (c's
// own Zone, LKI-collapsed the same way YouCtrl already is), attacking,
// blocking and blocked, bare form only (the current Combat's Attackers/Blocks),
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
	"fmt"
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
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
	if hiddenPreparedSpell(c) {
		// Card.java:5845-5848: a prepared spell's copy in exile fails every
		// restriction -- a negated one reads its testFailed, true.
		return alt.Base.Negated
	}
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
// Spell is Card.isSpell (Card.java:5500-5502): an instant or sorcery card,
// or an Aura card anywhere but the battlefield, in any zone -- RestartGame's
// own RestrictFromValid$ carve-out (restartgameeffect.go) reads it on exiled
// cards. Effect, Emblem and Boon never match: this port has no emblems or
// boons, and Java's Effect is its immutable effect card, which no card
// valid string this port evaluates reaches yet -- a coverage gap, not a
// wrong answer.
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
	case "Spell":
		t := c.Type()
		return t.Has(cardtype.Instant) || t.Has(cardtype.Sorcery) || (t.HasSubtype("Aura") && c.Zone != Battlefield)
	case "Effect", "Emblem", "Boon":
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
		return compareMatches(g, c, *p.Compare, sourceController, source)
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
	case name == "ExiledWithSource":
		// CardProperty.java:397-411: c was exiled with source's own object
		// -- the same CardID and the same zoneStamp as the host object an
		// ability of source sees (hostObjectStamp, game.go), and listed on
		// it. Exact name only: ExiledWithSourceLKI and ExiledWithEffectSource
		// are different branches Java tests first, not ported.
		ew := c.exiledWith
		if source == NoCard || ew.host != source || !ew.listed {
			return false
		}
		stamp, _ := g.hostObjectStamp(source)
		return ew.stamp == stamp
	case name == "stickered":
		// CardProperty.java:1337-1339: Card.isStickered.
		return len(c.stickers) > 0
	case strings.HasPrefix(name, "stickeredWith "):
		// CardProperty.java:1340-1344: a sticker of the named kind is on c;
		// an unknown kind name matches no card (smartValueOf is null).
		return g.hasStickerKindNamed(c, strings.TrimPrefix(name, "stickeredWith "))
	case strings.HasPrefix(name, "delved"):
		// CardProperty.java:1367-1369 (startsWith, so any suffix matches):
		// c is among the cards source's own last delve exiled.
		sc, ok := sourceCard(g, source)
		return ok && slices.Contains(sc.delved, c.ID)
	case name == "HauntedBy":
		// CardProperty.java:553: c is haunted by source (Card.haunts). Only
		// the Haunt keyword's own trigger writes it (compile/haunt.go).
		sc, ok := sourceCard(g, source)
		return ok && sc.haunts(c)
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
	case strings.HasPrefix(name, "ChosenColor"):
		// CardStateProperty.java:58-59: c has the color source chose
		// (getChosenColor, the first of its chosen colors); no choice matches
		// nothing. Memory keeps the chosen colors as a set, so "the first" is
		// exact only for a single chosen color: none of the 56 corpus cards
		// reading the property also carries a TwoColors$/OrColors$ choice, and
		// Discard refuses a set of more (discardValidResolvable,
		// discardeffect.go).
		sc, ok := sourceCard(g, source)
		return ok && c.Colors().HasAny(sc.Memory.ChosenColors())
	case name == "ChosenSector":
		// CardProperty.java:119: c is in the sector the source's controller
		// chose (ChooseSector). Java NPEs on a source that chose none; no
		// card reads this without a ChooseSector chained ahead of it.
		sc, ok := sourceCard(g, source)
		return ok && sc.Memory.ChosenSector() != "" && sc.Memory.ChosenSector() == c.Sector
	case name == "DifferentSector":
		// CardProperty.java:123: c is not in the source's own sector.
		sc, ok := sourceCard(g, source)
		return ok && sc.Sector != c.Sector
	case name == "IsSuspected":
		return c.Suspected
	case name == "IsRingbearer":
		return g.isRingBearer(c)
	case name == "IsCommander":
		// CardProperty.java:1983-1986 reads Card.isCommander, which no
		// card here has (Card.isCommander's doc comment).
		return c.isCommander()
	case name == "IsSolved":
		return c.Solved
	case strings.HasPrefix(name, "ChosenMode"):
		// CardProperty.java:116-117: c's own chosen mode is the rest of the
		// property (a Siege's Card.ChosenModeKhans).
		return c.Memory.ChosenMode() == name[len("ChosenMode"):]
	case name == "IsPrepared":
		// CardProperty.java:1861: the permanent is prepared (prepare.go).
		return c.prepared != NoCard
	case name == "prepared":
		// CardStateProperty.java:79-80: the card is in its PreparedSpell state,
		// a prepared spell's copy (Codie's "whenever you cast a prepared spell").
		return c.preparedFrom != NoCard
	case name == "FullyUnlocked":
		// CardProperty.java:1863: both doors of a Room permanent unlocked.
		return len(c.UnlockedDoors()) == 2
	case strings.HasPrefix(name, "named"):
		// CardProperty.java:62-68: the rest of the property is a card name,
		// with `;` standing for the `,` a valid string cannot hold and `_`
		// for a space, matched by Card.sharesNameWith(String).
		want := strings.ReplaceAll(strings.ReplaceAll(name[len("named"):], ";", ","), "_", " ")
		return sharesName(c, want)
	case name == "NamedCard":
		sc, ok := sourceCard(g, source)
		if !ok || c.Def == nil {
			return false
		}
		return containsString(sc.Memory.NamedCards(), c.Name())
	case name == "NamedByRememberedPlayer":
		sc, ok := sourceCard(g, source)
		if !ok || c.Def == nil {
			return false
		}
		for _, e := range sc.Memory.Remembered() {
			if p, isPlayer := e.AsPlayer(); isPlayer && g.Player(p).NamedCard != c.Name() {
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
	case name == "kicked", name == "kicked 1", name == "kicked 2":
		// CardProperty.java:1691: the card was cast with a kicker paid (either,
		// or the first/second for "kicked 1"/"kicked 2").
		switch name {
		case "kicked 1":
			return c.kicker&kicker1 != 0
		case "kicked 2":
			return c.kicker&kicker2 != 0
		}
		return c.kickerMagnitude() > 0
	case name == "escaped":
		// CardProperty.java:1751-1755: the card's cast was for its Escape
		// cost ("CARDNAME escapes with ..." counters, Kroxa's "unless it
		// escaped").
		return c.castEscaped()
	case name == "IsRemembered":
		sc, ok := sourceCard(g, source)
		return ok && containsEntity(sc.Memory.Remembered(), CardEntity(c.ID))
	case name == "IsImprinted":
		sc, ok := sourceCard(g, source)
		return ok && containsCard(sc.Memory.Imprinted(), c.ID)
	case name == "sharesNameWith":
		// CardProperty.java:819-822: c shares a name with the source itself.
		sc, ok := sourceCard(g, source)
		return ok && g.sharesNameWith(c.ID, sc.ID)
	case strings.HasPrefix(name, "sharesNameWith "):
		return g.sharesNameWithAny(c, strings.TrimPrefix(name, "sharesNameWith "), sourceController, source)
	case strings.HasPrefix(name, "AttachedTo "):
		// CardProperty.java:441: c is attached to something that is itself
		// valid for the restriction after the space -- a player through
		// matchesPlayerSpec ("Curse.AttachedTo Player.EnchantedBy",
		// "Curse.AttachedTo You"), a card through Matches. Java's fallback to
		// the ability's defined cards/players ("Targeted", "ParentTarget")
		// needs an ability this evaluator is not given, so those match nothing.
		restriction := strings.TrimPrefix(name, "AttachedTo ")
		if pid, ok := c.AttachedToPlayer(); ok {
			matched, _ := matchesPlayerSpec(g, pid, sourceController, source, restriction)
			return matched
		}
		host, ok := c.AttachedTo()
		return ok && Matches(g, g.Card(host), valid.Parse(restriction), sourceController, source)
	case name == "EnchantedPlayer", name == "EnchantedPlayerCtrl":
		// CardProperty.java:238: the card's owner (or, with Ctrl, its
		// controller) is the player source enchants; a source enchanting
		// anything else matches nothing.
		sc, ok := sourceCard(g, source)
		if !ok {
			return false
		}
		pid, attached := sc.AttachedToPlayer()
		if !attached {
			return false
		}
		if name == "EnchantedPlayerCtrl" {
			return c.Controller() == pid
		}
		return c.Owner == pid
	case strings.HasPrefix(name, "EnchantedBy "):
		// CardProperty.java:466-488: "EnchantedBy Aura.Other" holds when one of
		// c's Auras matches the nested spec, tested with source as the source
		// (Daybreak Coronet: "Creature.EnchantedBy Aura.Other"). The nested
		// "Imprinted" and "Targeted" forms stay unrecognized: false.
		rest := strings.TrimPrefix(name, "EnchantedBy ")
		if rest == "Imprinted" || rest == "Targeted" {
			return false
		}
		nested := valid.Parse(rest)
		for _, att := range c.Attachments() {
			if Matches(g, g.Card(att), nested, sourceController, source) {
				return true
			}
		}
		return false
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
	case name == "IsRenowned":
		return c.renowned
	case name == "IsMonstrous":
		return c.monstrous
	case name == "HasDevoured":
		// CardProperty.java:1823-1826: its Devour sacrificed something as it
		// entered (Hellkite Hatchling).
		return len(c.devoured) > 0
	case strings.HasPrefix(name, "ThisTurnEnteredFrom"):
		// CardProperty.java:954-967: c entered the zone it is in this turn, from
		// the named zone. Java splits at "ThisTurnEnteredFrom_", so a name
		// without the underscore cannot be read.
		rest, ok := strings.CutPrefix(name, "ThisTurnEnteredFrom_")
		if !ok {
			return false
		}
		origin, ok := ZoneByName(strings.SplitN(rest, "_", 2)[0])
		return ok && c.zoneEntered && c.zoneEntryTurn == g.turn && c.zoneEntryFrom == origin
	case strings.HasPrefix(name, "cameUnderControlSinceLastUpkeep"):
		// CardProperty.java:1082.
		return c.cameUnderControl
	case strings.HasPrefix(name, "attacking "):
		// CardProperty.java:1529-1535, the generic "attacking [Defined]":
		// c attacks, and the entity it attacks is among those the Defined
		// names. Only a player defender against a player spec is read here
		// (a "Player.EnchantedBy" or "You", all the real lines): this group
		// cannot reach definedEntities without a cycle, and no real line
		// names a card there. TriggeredAttackedTarget (Jin Sakai) is the one
		// Defined form read: the recorded list off the ability in context.
		if !containsCard(g.combat.Attackers, c.ID) {
			return false
		}
		target, ok := g.combat.AttackTargets[c.ID]
		if !ok {
			return false
		}
		if strings.TrimPrefix(name, "attacking ") == "TriggeredAttackedTarget" {
			// getDefinedEntities over AbilityKey.AttackedTarget: players and
			// cards alike, so a planeswalker defender is compared as itself.
			attacked, _ := g.resolvingAttackedTargets(source, name)
			return containsEntity(attacked, target)
		}
		defender, isPlayer := target.AsPlayer()
		if !isPlayer {
			return false
		}
		matched, _ := matchesPlayerSpec(g, defender, sourceController, source, strings.TrimPrefix(name, "attacking "))
		return matched
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
	case strings.HasPrefix(name, "attackedThisCombat"):
		// CardProperty.java:1538: attacked in the combat in progress; an
		// optional <cmp>X compares it with 1 plus the other attackers.
		if !containsCard(g.combat.Attackers, c.ID) {
			return false
		}
		if len(name) == len("attackedThisCombat") {
			return true
		}
		// A comparison form is not read (no corpus line writes one).
		return false
	case name == "attackingYou":
		// CardProperty.java:1506-1514: c is attacking and its defender is the
		// source's controller itself (Combat.isAttacking(card, player)); a
		// planeswalker or battle of theirs is a different defender.
		return containsCard(g.combat.Attackers, c.ID) && g.combat.AttackTargets[c.ID] == PlayerEntity(sourceController)
	case strings.HasPrefix(name, "attacking"):
		// The rest of CardProperty.java:1506-1549's "attacking" chain.
		return attackingSuffixMatches(g, c, name, sourceController, source)
	case strings.HasPrefix(name, "attackedYouThisTurn"):
		// CardProperty.java:1193-1196: CardDamageHistory.hasAttackedThisTurn
		// (sourceController), the player itself among c's declared defenders
		// this turn (Card.attackedThisTurn).
		return containsEntity(c.attackedThisTurn, PlayerEntity(sourceController))
	case strings.HasPrefix(name, "DefendingPlayer"):
		// CardProperty.java:231-238: c's controller ("...Ctrl") or owner is
		// attacked in the combat in progress (Combat.isPlayerAttacked,
		// Combat.java:956-970): some attacker's defender is that player, or a
		// planeswalker or battle that player controls or protects. Java's
		// inCombat check is implied: no combat, no attackers.
		who := c.Owner
		if strings.HasSuffix(name, "Ctrl") {
			who = c.Controller()
		}
		for _, a := range g.combat.Attackers {
			target := g.combat.AttackTargets[a]
			if pid, ok := target.AsPlayer(); ok && pid == who {
				return true
			}
			if id, ok := target.AsCard(); ok && (g.Card(id).Controller() == who || g.Card(id).ProtectingPlayer == who) {
				return true
			}
		}
		return false
	case name == "blockedThisCombat":
		// CardProperty.java:1548: blocked a creature in the combat in progress.
		for _, b := range g.combat.Blocks {
			if b.Blocker == c.ID {
				return true
			}
		}
		return false
	case name == "DefenderCtrl", name == "DefenderCtrlForRemembered":
		// CardProperty.java:198-213: c's controller defends against the
		// source -- or, ForRemembered, against the source's first remembered
		// card (Holga's and Mercadia's Downfall's RepeatEach) -- as
		// Combat.getDefendingPlayerRelatedTo (Combat.java:452-463) reads it:
		// an Aura, Fortification or Equipment stands for the creature it is
		// attached to, and the defender is defenderPlayerByAttacker's (a
		// battle's protector, not its controller). No remembered card, or a
		// first remembered object that is not a card, matches nothing.
		attacker := source
		if name == "DefenderCtrlForRemembered" {
			sc, ok := sourceCard(g, source)
			if !ok || len(sc.Memory.Remembered()) == 0 {
				return false
			}
			if attacker, ok = sc.Memory.Remembered()[0].AsCard(); !ok {
				return false
			}
		}
		if a, ok := sourceCard(g, attacker); ok {
			if t := a.Type(); t.HasSubtype("Aura") || t.HasSubtype("Equipment") || t.HasSubtype("Fortification") {
				attacker, _ = a.AttachedTo()
			}
		}
		defender, ok := g.defenderPlayerByAttacker(attacker)
		return ok && defender == c.Controller()
	case name == "blocking":
		return isBlocking(g.combat.Blocks, c.ID)
	case name == "blocked":
		// CardProperty.java:1591-1592: combat.isBlocked(card) -- an attacker
		// with a blocker, or one an effect made blocked (ForcedBlocked).
		// Exact-matched: blockedBySource*, blockedThisTurn and the rest are
		// distinct Java branches this case must not swallow.
		return g.combat.isBlocked(c.ID)
	case name == "unblocked":
		// CardProperty.java:1677-1680: combat.isUnblocked(card) -- an attacker
		// whose band was declared unblocked (Combat.isUnblocked). Before
		// blockers are declared no attacker is (blockDecided). Java's
		// startsWith also takes a longer name; no corpus line writes one.
		return g.combat.isUnblocked(c.ID)
	case name == "sneaked":
		// CardProperty.java:1799-1806: getCastSA().isSneak(). The
		// isUnlinkedFromCastSA guard is left out, as for kicked.
		return c.sneaked
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
	// TargetedPlayerCtrl/TargetedPlayerOwn: c's controller/owner is a player
	// the resolving ability targets (AbilityUtils.getDefinedPlayers(source,
	// "TargetedPlayer", spellAbility)). The ability is Game.resolving when it
	// is source's own; outside one nothing is targeted and the property is
	// false, as Java's null spellAbility gives an empty list.
	// ControlledBy <player spec>: c's controller matches the spec from the
	// source's point of view (CardProperty.java's ControlledBy branch).
	case strings.HasPrefix(name, "ControlledBy "):
		spec := strings.TrimPrefix(name, "ControlledBy ")
		matched, ok := matchesPlayerSpec(g, c.Controller(), sourceController, source, spec)
		if !ok && source != NoCard {
			// CardProperty.java:310-317: a spec Player.isValid cannot read is a
			// Defined$ list (getDefinedPlayers) -- Zara, Renegade Recruiter's
			// "ControlledBy DefendingPlayer" -- read with the refs of source's
			// ability in context, when there is one.
			var refs abilityRefs
			if a := g.resolving; a != nil && a.Source == source {
				refs = a.refs()
			}
			players, err := definedPlayers(g, sourceController, source, spec, refs)
			return err == nil && slices.Contains(players, c.Controller())
		}
		return ok && matched
	case strings.HasPrefix(name, "leastPower"), strings.HasPrefix(name, "leastToughness"), strings.HasPrefix(name, "greatestPower"):
		return extremeStatMatches(g, c, name, sourceController, source)
	case strings.HasPrefix(name, "YourTeamCtrl"):
		// CardProperty.java:182-185: the controller's team is the source
		// controller's. This port has no team games, so a team is one player.
		return c.Controller() == sourceController
	case name == "NotedColor", name == "BorderColorSilver":
		// CardProperty.java:1998-2007 reads the controller's draft notes, which
		// a game here never has: no card carries the noted color. A card's border
		// color is not in the database: none is silver-bordered.
		return false
	// canBeBeamedUp: no CantBeBeamedUp static covers c (Marooned).
	case name == "canBeBeamedUp":
		return !g.cantBeBeamedUp(c)
	case strings.HasPrefix(name, "wasDealtExcessDamageThisTurn"):
		// CardProperty.java:1164-1167: Card.hasBeenDealtExcessDamageThisTurn.
		return c.Damage.ExcessThisTurn
	case name == "TriggeredTarget":
		// CardProperty.java:2045-2063: c is the root ability's
		// AbilityKey.Target triggering object, read off the ability of source
		// in context -- Toralf, God of Fury's "any target other than that
		// permanent", Blade of Shared Souls' "another target creature". False
		// with no ability of source's in context or no card target recorded,
		// as Java's non-SpellAbility branch and null object are.
		a := g.resolving
		if a == nil || source == NoCard || a.Source != source {
			return false
		}
		id, ok := a.triggered.target.AsCard()
		return ok && id == c.ID
	case name == "TriggeredAttackers":
		// CardProperty.java:2045-2063: c is in the root ability's
		// AbilityKey.Attackers; false with no ability of source's in context
		// (Java's non-SpellAbility branch).
		attackers, _ := g.resolvingAttackers(source, name)
		return containsCard(attackers, c.ID)
	case name == "NotDefinedTriggeredAttackers":
		// CardProperty.java:2064-2068: getDefinedCards(source,
		// "TriggeredAttackers", sa) does not contain c -- true with no ability
		// in context, Java's empty list.
		attackers, _ := g.resolvingAttackers(source, name)
		return !containsCard(attackers, c.ID)
	case name == "TargetedPlayerCtrl":
		return g.targetsPlayer(source, c.Controller())
	case name == "TargetedPlayerOwn":
		return g.targetsPlayer(source, c.Owner)
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
	case strings.HasPrefix(name, "Self"), name == "StrictlySelf":
		// StrictlySelf is Self's game-timestamp-aware twin
		// (Card.equalsWithGameTimestamp); identity here, for the reason
		// StrictlyOther's own comment above gives.
		return c.ID == source
	case strings.HasPrefix(name, "DamagedBy"):
		// CardProperty.java:1096-1124 without its Defined form (a space after
		// the name): the card was dealt damage this turn by the source
		// itself, or by a card matching the ";"-separated valid strings.
		// A source is its CardID; Java also compares game timestamps.
		rest := strings.TrimPrefix(name, "DamagedBy")
		if strings.HasPrefix(rest, " ") {
			return false
		}
		for _, dealer := range c.Damage.Sources {
			if rest == "" && dealer == source {
				return true
			}
			if rest != "" && anyValidMatches(g, g.Card(dealer), rest, sourceController, source) {
				return true
			}
		}
		return false
	case strings.HasPrefix(name, "Paired"):
		// CardProperty.java:564-570: Soulbond-paired, and for "PairedWith"
		// paired with the source itself.
		if !c.IsPaired() {
			return false
		}
		return !strings.HasSuffix(name, "With") || c.PairedWith() == source
	case name == "tapped":
		return c.Tapped
	case name == "wasCast":
		// CardProperty.java:1936: Card.wasCast, a cast spell's card.
		return c.wasCast
	case strings.HasPrefix(name, "CastSa "):
		// CardProperty.java:1906-1913: the spell the card was cast as, read as a
		// spell ability property. Only "Spell.ManaSpent <op><n>" (Primeval
		// Spawn's "no mana was spent") is resolved; a card never cast fails.
		return castSaManaSpent(strings.TrimPrefix(name, "CastSa "), c.castManaSpent) && c.wasCast
	case name == "faceDown":
		// CardProperty.java: card.isFaceDown().
		return c.IsFaceDown()
	case name == "faceUp":
		return !c.IsFaceDown()
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
	if matched, handled := turnHistoryMatches(g, c, name, sourceController, source); handled {
		return matched
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
		// Card.hasKeyword(String) reads the hidden table first, whole lines
		// ("CARDNAME count as Flame Burst."), by substring (Card.java:4995-5000),
		// since the valid string's own "." split drops the line's final period;
		// then the keyword list.
		for _, line := range c.hiddenKeywords {
			if strings.Contains(line, rest) {
				return true
			}
		}
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
	if strings.HasPrefix(name, "MonoColor") {
		// CardStateProperty.java:56-57: exactly one color.
		return c.Colors().Count() == 1
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
			for _, face := range h.traitFaces() {
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
// two-clause example, the removed port log's own count), so only Base.Property is
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
		propMatched, propOK := matchesPlayerProperties(g, candidate, host, source, property)
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

// matchesPlayerProperties is Player.isValid's own "+"-joined property AND
// (GameObject.java's incR[1].split("\\+")): every token must hold. The
// first token that is definitely false decides the answer, as in Java's
// short-circuit; one matchesPlayerProperty cannot evaluate, reached before
// any false, leaves the whole conjunction unevaluable. The split is as naive
// as Java's, so a "controls<Type>" type naming "+" is not supported (Java
// has the same limit, which is why its Turf War line escapes with "_").
func matchesPlayerProperties(g *Game, candidate, host PlayerID, source CardID, properties string) (matched, ok bool) {
	for _, property := range strings.Split(properties, "+") {
		m, recognized := matchesPlayerProperty(g, candidate, host, source, property)
		if !recognized {
			return false, false
		}
		if !m {
			return false, true
		}
	}
	return true, true
}

// controlsMatches is PlayerProperty.java:297-310's "controls<Type>[_<cmp>]":
// the candidate's battlefield cards matching the valid string <Type>
// (backslash-escaped "_" kept literal, Turf War's convention) are counted
// against "<cmp><n>" (default at least one). A comparator operand other than
// an integer literal is not evaluated.
func controlsMatches(g *Game, candidate, host PlayerID, source CardID, property string) (matched, ok bool) {
	var parts []string
	var cur strings.Builder
	for i := 0; i < len(property); i++ {
		switch {
		case property[i] == '\\' && i+1 < len(property):
			cur.WriteByte(property[i])
			cur.WriteByte(property[i+1])
			i++
		case property[i] == '_':
			parts = append(parts, cur.String())
			cur.Reset()
		default:
			cur.WriteByte(property[i])
		}
	}
	parts = append(parts, cur.String())
	spec := valid.Parse(strings.ReplaceAll(parts[0], "\\_", "_"))
	count := 0
	for _, id := range g.Zone(Battlefield, candidate).Cards() {
		if Matches(g, g.Card(id), spec, host, source) {
			count++
		}
	}
	op, operand := "GE", 1
	if len(parts) > 1 {
		if len(parts[1]) < 3 {
			return false, false
		}
		n, err := strconv.Atoi(parts[1][2:])
		if err != nil {
			return false, false
		}
		op, operand = parts[1][:2], n
	}
	return compareOp(count, op, operand), true
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
// Keen-Eared Sentry's CantVenture), and IsRemembered (the source card
// remembers the candidate, PlayerProperty.java:209-212 -- what Subgame's
// RememberPlayers$ feeds, subgameeffect.go).
//
// EnchantedBy (the source is among the candidate's own attachments -- an Aura
// enchanting a player directly, CR 303.4h -- distinct from Matches' own
// *card*-side EnchantedBy) is resolved from Player.Attachments. The turn-record
// and comparison properties (LostLifeThisTurn, wasDealt...ThisTurn[By],
// Defending, HasCardsIn..., withMore..., ...) are playerProperty's
// (playerproperty.go). Every other real property (Chosen, a ChosenPlayer
// memory slot) needs state this port does not track at all yet, and is left
// unrecognized here, ok=false, the same "skip rather than guess" contract
// every other unresolved param in this port already has (GO-7).
func matchesPlayerProperty(g *Game, candidate, host PlayerID, source CardID, property string) (matched, ok bool) {
	return matchesPlayerPropertyIn(g, candidate, host, source, property, nil, 0)
}

// matchesPlayerPropertyIn is matchesPlayerProperty with the amounts table an
// SVar operand ("HasCardsInHand_Card_GTY", "LostLifeThisTurn GEX") resolves
// in, and the SVar depth already reached: the PlayerCount amounts pass their
// own (amountplayers.go), every valid-spec caller none, which falls back to
// the source card's own SVars (playerPropertyOperand). A leading "!" negates
// the property (Player.hasProperty, Player.java:2203-2205: Mindstorm Crown's
// You.!CardsInHandAtBeginningOfTurn).
func matchesPlayerPropertyIn(g *Game, candidate, host PlayerID, source CardID, property string, amounts map[string]expr.Amount, depth int) (matched, ok bool) {
	if rest, negated := strings.CutPrefix(property, "!"); negated {
		m, known := matchesPlayerPropertyIn(g, candidate, host, source, rest, amounts, depth)
		return !m && known, known
	}
	if matched, ok := matchesPlayerBase(candidate, host, property); ok {
		return matched, true
	}
	if rest, ok := strings.CutPrefix(property, "controls"); ok {
		return controlsMatches(g, candidate, host, source, rest)
	}
	// PlayerProperty.java:140-175 for the "LastTurn" forms: wasDealtDamageLastTurn,
	// wasDealtCombatDamageLastTurn and wasDealtNonCombatDamageLastTurn compare
	// the damage dealt to the player last turn (all, combat or noncombat) with
	// the default GE1. The "ThisTurn" and "By" forms are wasDealtThisTurn's
	// (playerproperty.go).
	if strings.HasPrefix(property, "wasDealt") && strings.HasSuffix(property, "LastTurn") {
		d := g.Player(candidate).damageLastTurn
		switch {
		case strings.Contains(property, "NonCombat"):
			return d[0] >= 1, true
		case strings.Contains(property, "CombatDamage"):
			return d[1] >= 1, true
		default:
			return d[0]+d[1] >= 1, true
		}
	}
	// PlayerProperty.java:38-49: "OpponentOf <defined>" holds when candidate
	// is an opponent of every player the Defined (the word after the space,
	// Java's split(" ")[1]) names; "PlayerUID_<n>" when candidate is player n.
	// Any other Defined is read through definedPlayers with the refs of the
	// ability in context (Game.resolving), as Java passes its spellAbility to
	// getDefinedPlayers: Power Struggle's "Player.OpponentOf TriggeredPlayer".
	// Outside that ability's context, or a Defined it cannot read, stays
	// unrecognized (GO-7).
	if rest, ok := strings.CutPrefix(property, "OpponentOf "); ok {
		word, _, _ := strings.Cut(rest, " ")
		switch {
		case word == "You":
			return candidate != host, true
		case strings.HasPrefix(word, "PlayerUID_"):
			n, err := strconv.Atoi(strings.TrimPrefix(word, "PlayerUID_"))
			return err == nil && candidate != PlayerID(n), err == nil
		}
		a := g.resolving
		if a == nil || source == NoCard || a.Source != source {
			return false, false
		}
		named, err := definedPlayers(g, host, source, word, a.refs())
		if err != nil {
			return false, false
		}
		for _, p := range named {
			if candidate == p {
				return false, true
			}
		}
		return true, true
	}
	if rest, ok := strings.CutPrefix(property, "PlayerUID_"); ok {
		n, err := strconv.Atoi(rest)
		return err == nil && candidate == PlayerID(n), err == nil
	}
	switch property {
	case "Active":
		return candidate == g.ActivePlayer(), true
	case "NonActive":
		return candidate != g.ActivePlayer(), true
	case "Other":
		return candidate != host, true
	case "EnchantedBy":
		// PlayerProperty.java:249: source is among the player's attachments
		// (GameEntity.isEnchantedBy -- any attachment counts, CR 303.4k).
		return containsCard(g.Player(candidate).Attachments(), source), true
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
	case "withMostLife":
		// PlayerProperty.java:353: nobody in the game has more life.
		for _, pid := range g.Players() {
			if !g.Player(pid).Lost && g.Player(pid).Life > g.Player(candidate).Life {
				return false, true
			}
		}
		return true, true
	case "descended":
		return g.Player(candidate).DescendedThisTurn, true
	case "isMonarch":
		// PlayerProperty.java:85-88: Player.isMonarch (Game.monarch).
		return g.monarch == candidate, true
	case "hasInitiative":
		// PlayerProperty.java:89-92: Player.hasInitiative.
		return g.initiative == candidate, true
	case "committedCrimeThisTurn":
		// PlayerProperty.java:83-84: at least one crime this turn.
		return g.Player(candidate).committedCrimes > 0, true
	case "VenturedThisTurn":
		return g.Player(candidate).VenturedThisTurn > 0, true
	case "IsPoisoned":
		// PlayerProperty.java:273: at least one poison counter.
		return g.Player(candidate).Counters.Count(Poison) > 0, true
	case "IsCorrupted":
		// PlayerProperty.java:277 (CR 702.xxx "corrupted"): three or more.
		return g.Player(candidate).Counters.Count(Poison) >= 3, true
	case "IsRememberedOrController":
		// PlayerProperty.java:215-232: a remembered player is the candidate, or
		// a remembered card is controlled by it (Cut a Deal's drawn cards).
		sc, ok := sourceCard(g, source)
		if !ok {
			return false, true
		}
		for _, e := range sc.Memory.Remembered() {
			if pid, isPlayer := e.AsPlayer(); isPlayer && pid == candidate {
				return true, true
			}
			if cid, isCard := e.AsCard(); isCard && g.Card(cid).Controller() == candidate {
				return true, true
			}
		}
		return false, true
	case "IsRemembered":
		// PlayerProperty.java:209-212: source.isRemembered(player). No
		// source recognizes the property and matches nobody.
		sc, ok := sourceCard(g, source)
		return ok && containsEntity(sc.Memory.Remembered(), PlayerEntity(candidate)), true
	}
	// PlayerProperty.java's life<op><n> ("lifeGE1": at least 1 life, Worship's
	// ValidPlayer$ You.lifeGE1). A literal n resolves here; an SVar n is
	// playerProperty's (playerproperty.go).
	if len(property) > 6 && strings.HasPrefix(property, "life") {
		if n, err := strconv.Atoi(property[6:]); err == nil {
			return compareOp(g.Player(candidate).Life, property[4:6], n), true
		}
	}
	return playerProperty(g, candidate, host, source, property, amounts, depth)
}

// attackingSuffixMatches is the rest of CardProperty.java:1506-1549's chain
// for a name starting "attacking" that propertyMatches' own cases ("attacking",
// "attackingYou", "attacking <Defined>") do not take: c must be attacking, then
// the suffix narrows it.
//
//   - "...Alone" (attackingAlone, :1511-1513): c is the only attacker. Java
//     counts the attackers on the last-state battlefield; this counts the
//     attackers still on the battlefield plus c itself, so a creature that died
//     attacking alone (Thijarian Witness) still reads alone. An attacker that
//     died in the same event as c is not counted, where Java's snapshot would.
//   - "attackingSame" (:1515-1520): c attacks what the source attacks.
//   - "attackingYouOrYourPW", with or without LKI (:1530-1542): c attacks the
//     source's controller or a planeswalker they control. A battle defender
//     never matches (Java's "planeswalker that was removed from combat" guard).
//   - "attackingBattle" (:1521-1529) is refused: Java reads the source's
//     defender (combat.getDefenderByAttacker(source)), not c's, so Rampaging
//     Geoderm's "if it's attacking a battle" asks whether the Geoderm is. A
//     Forge bug (PORT-8): recorded as a pending error, never compensated.
//   - Any other suffix reads as "attacking", Java's fall-through.
//
// LKI spellings read c itself: a CardID is the card across zone changes and
// Move leaves combat as it was (combat.go's removeFromCombat).
func attackingSuffixMatches(g *Game, c *Card, name string, sourceController PlayerID, source CardID) bool {
	if !containsCard(g.combat.Attackers, c.ID) {
		return false
	}
	switch {
	case strings.HasSuffix(name, "Alone"):
		for _, id := range g.combat.Attackers {
			if id != c.ID && g.Card(id).Zone == Battlefield {
				return false
			}
		}
		return true
	case name == "attackingSame":
		attacked, ok := g.combat.AttackTargets[source]
		return ok && containsCard(g.combat.Attackers, source) && g.combat.AttackTargets[c.ID] == attacked
	case name == "attackingBattle":
		g.recordPendingError(fmt.Errorf("engine: valid property attackingBattle reads the source's defender (CardProperty.java:1522, a Forge bug); not resolvable"))
		return false
	case strings.HasPrefix(name, "attackingYouOrYourPW"):
		target := g.combat.AttackTargets[c.ID]
		if pid, ok := target.AsPlayer(); ok {
			return pid == sourceController
		}
		id, ok := target.AsCard()
		if !ok {
			return false
		}
		d := g.Card(id)
		return d.Type().Has(cardtype.Planeswalker) && d.Controller() == sourceController
	}
	return true
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
// Operand is Java's AbilityUtils.calculateAmount(source, operand): a plain
// base-10 integer, or the name of a value SVar on the source card ("X" in
// Creature.powerGTX), resolved against that card's own faces
// (compareOperand). A name no face of the source defines, or one that does
// not resolve, matches nothing: coverage gap, never a wrong answer.
func compareMatches(g *Game, c *Card, cmp valid.Compare, sourceController PlayerID, source CardID) bool {
	operand, ok := compareOperand(g, cmp.Operand, sourceController, source)
	if !ok {
		return false
	}
	value, ok := compareFieldValue(c, cmp.Field)
	if !ok {
		return false
	}
	return compareOp(value, cmp.Operator, operand)
}

// compareOperand resolves a Compare operand: an integer literal, or an SVar
// name read from the source card's live faces, or "Chosen", the number the
// source chose (CardProperty.java:1465-1469; no choice fails the property, as
// Java's does). The other calculateAmount shapes that are not a card's own
// SVar are not resolved.
func compareOperand(g *Game, operand string, sourceController PlayerID, source CardID) (int, bool) {
	if n, err := strconv.Atoi(operand); err == nil {
		return n, true
	}
	if operand == "Chosen" {
		sc, ok := sourceCard(g, source)
		if !ok {
			return 0, false
		}
		return sc.Memory.ChosenNumber()
	}
	key := strings.ToLower(operand)
	if f := g.relativeFace; f != nil {
		if amt, ok := f.Amounts[key]; ok {
			return resolveAmount(g, f.Amounts, sourceController, source, amt)
		}
	}
	if amounts := g.relativeAmounts; amounts != nil {
		if amt, ok := amounts[key]; ok {
			return resolveAmount(g, amounts, sourceController, source, amt)
		}
	}
	sc, ok := sourceCard(g, source)
	if !ok || sc.Def == nil {
		return 0, false
	}
	for i := range sc.Def.Faces[:liveFaces(sc.Def)] {
		f := &sc.Def.Faces[i]
		if amt, ok := f.Amounts[key]; ok {
			return resolveAmount(g, f.Amounts, sourceController, source, amt)
		}
	}
	return 0, false
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

// castSaManaSpent reads "Spell.ManaSpent <op><n>" (SpellAbilityProperty.java:
// 181-185) against the mana spent casting. Anything else is false.
func castSaManaSpent(spec string, spent int) bool {
	rest, ok := strings.CutPrefix(spec, "Spell.ManaSpent ")
	if !ok || len(rest) < 3 {
		return false
	}
	n, err := strconv.Atoi(rest[2:])
	if err != nil {
		return false
	}
	return compareOp(spent, rest[:2], n)
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

// sharesName is Card.sharesNameWith(String) (Card.java:5838-5863): c's name
// is name, or, off the battlefield, c is a split card one of whose halves is
// named name. Java's on-the-battlefield branch, a Room's unlocked door
// names, is not read: a Room permanent's Def already is the view of its
// unlocked doors (room.go), and no corpus named<Name> property names a door.
// Not ported: Card.hasNonLegendaryCreatureNames()'s own tail
// (Card.java:5864-5868, SpyKit's own text-changing ability) -- this port
// does model HasNonLegendaryCreatureNames (card.go), so this is a real,
// disclosed gap, not a forced omission; 0 real corpus named<Name> lines
// reach a SpyKit-shaped card today.
func sharesName(c *Card, name string) bool {
	if name == "" || c.Def == nil {
		return false
	}
	if c.Name() == name {
		return true
	}
	if c.Zone == Battlefield || c.Def.SplitType != carddb.SplitSplit {
		return false
	}
	return c.Def.Faces[0].Name == name || c.Def.Faces[carddb.FaceAlternate].Name == name
}

// sharesNameWithAny is CardProperty.java:823-869, `sharesNameWith <restriction>`:
// c shares a name with some card the restriction names. The zone forms
// (YourGraveyard, Graveyard, Battlefield, NonToken) scan those zones; any
// other restriction is a Defined$ card list (AbilityUtils.getDefinedCards),
// of which only the ones read off the source's own Memory -- Remembered and
// Imprinted (Doubling Chant, Clarion Ultimatum, Dichotomancy) -- are answered
// here, since a valid string is matched without the ability that names it.
// ThisTurnCast, MovedToGrave, TriggeredCard and the other Defined$ forms need
// that ability: they match nothing and leave a pending error.
func (g *Game) sharesNameWithAny(c *Card, restriction string, sourceController PlayerID, source CardID) bool {
	anyShares := func(ids []CardID) bool {
		for _, id := range ids {
			if g.sharesNameWith(id, c.ID) {
				return true
			}
		}
		return false
	}
	inZone := func(z ZoneType, players []PlayerID, keep func(*Card) bool) bool {
		for _, pid := range players {
			for _, id := range g.Zone(z, pid).Cards() {
				if keep(g.Card(id)) && g.sharesNameWith(id, c.ID) {
					return true
				}
			}
		}
		return false
	}
	all := func(*Card) bool { return true }
	switch restriction {
	case "YourGraveyard":
		return sourceController != NoPlayer && inZone(Graveyard, []PlayerID{sourceController}, all)
	case "Graveyard":
		return inZone(Graveyard, g.Players(), all)
	case "Battlefield":
		return inZone(Battlefield, g.Players(), all)
	case "NonToken":
		return inZone(Battlefield, g.Players(), func(o *Card) bool { return !o.IsToken })
	}
	sc, ok := sourceCard(g, source)
	if !ok {
		return false
	}
	switch restriction {
	case "Remembered":
		var cards []CardID
		for _, e := range sc.Memory.Remembered() {
			if id, isCard := e.AsCard(); isCard {
				cards = append(cards, id)
			}
		}
		return anyShares(cards)
	case "Imprinted":
		return anyShares(sc.Memory.Imprinted())
	}
	// A Defined$ form this matcher cannot answer: matching nothing would hide
	// the card's filter being ignored (GO-7).
	g.recordPendingError(fmt.Errorf("engine: sharesNameWith %s: restriction not evaluable in a valid string", restriction))
	return false
}

// targetsPlayer reports whether the ability resolving from source has pid
// among its chosen targets.
func (g *Game) targetsPlayer(source CardID, pid PlayerID) bool {
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		return false
	}
	return containsEntity(a.Targets, PlayerEntity(pid))
}

// resolvingAttackers is AbilityKey.Attackers as the ability of source in
// context (Game.resolving) recorded it, for the valid property prop. inContext
// is false with no ability of source's in context, where Java's spellAbility
// is no SpellAbility and its Triggered*/NotDefined* reads find nothing. A
// trigger that recorded no attackers leaves a pending error naming prop and
// reads as none (GO-7): Java reads a missing key as an empty list
// (AbilityUtils.java:697-700), which this port does not guess at.
func (g *Game) resolvingAttackers(source CardID, prop string) (attackers []CardID, inContext bool) {
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		return nil, false
	}
	if !a.triggered.attackersRecorded {
		g.recordPendingError(fmt.Errorf("engine: valid property %q: the trigger recorded no attackers", prop))
		return nil, true
	}
	return a.triggered.attackers, true
}

// resolvingAttackedTargets is resolvingAttackers for AbilityKey.AttackedTarget.
func (g *Game) resolvingAttackedTargets(source CardID, prop string) (attacked []EntityID, inContext bool) {
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		return nil, false
	}
	if !a.triggered.attackedTargetsRecorded {
		g.recordPendingError(fmt.Errorf("engine: valid property %q: the trigger recorded no attacked target", prop))
		return nil, true
	}
	return a.triggered.attackedTargets, true
}

// cantBeBeamedUp is StaticAbilityCantBeBeamedUp.cantBeBeamedUp: a battlefield
// Mode$ CantBeBeamedUp static whose ValidCard$ matches c.
func (g *Game) cantBeBeamedUp(c *Card) bool {
	for _, pid := range g.Players() {
		for _, host := range g.Zone(Battlefield, pid).Cards() {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.liveTraitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CantBeBeamedUp") {
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

// extremeStatMatches is CardProperty.java:1211-1262's greatestPower,
// leastPower and leastToughness: no creature on the battlefield has more
// (greatestPower) or less (the others) power or toughness than c. A
// "ControlledBy<player spec>" suffix narrows the creatures compared to that
// player's and requires c to be among them (Bolster's
// "Creature.leastToughnessControlledByYou"). A spec Defined$ cannot read
// matches nothing.
func extremeStatMatches(g *Game, c *Card, name string, sourceController PlayerID, source CardID) bool {
	stat := (*Card).Power
	prefix, greatest := "leastPower", false
	switch {
	case strings.HasPrefix(name, "leastToughness"):
		stat, prefix = (*Card).Toughness, "leastToughness"
	case strings.HasPrefix(name, "greatestPower"):
		prefix, greatest = "greatestPower", true
	}
	var owners []PlayerID
	limited := false
	if _, spec, ok := strings.Cut(strings.TrimPrefix(name, prefix), "ControlledBy"); ok {
		var refs abilityRefs
		if a := g.resolving; a != nil && source != NoCard && a.Source == source {
			refs = a.refs()
		}
		players, err := definedPlayers(g, sourceController, source, spec, refs)
		if err != nil {
			return false
		}
		owners, limited = players, true
	}
	mine, _ := stat(c)
	inScope := !limited
	for _, pid := range g.Players() {
		if limited && !slices.Contains(owners, pid) {
			continue
		}
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			other := g.Card(id)
			if !other.Type().Has(cardtype.Creature) {
				continue
			}
			if id == c.ID {
				inScope = true
			}
			if v, _ := stat(other); (!greatest && v < mine) || (greatest && v > mine) {
				return false
			}
		}
	}
	return inScope
}
