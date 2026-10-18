// Package cost parses the value of a `Cost$` param: what a player pays to
// activate an ability or cast a spell.
//
// `1 R Sac<1/Creature.Other/another creature> T` is four parts — two mana
// symbols, a sacrifice and a tap. Paying one needs a game, so this package
// only says what the parts are.
//
// Ported from forge-game/src/main/java/forge/game/cost/Cost.java (its
// constructor, parseCostPart and abCostParse), with the field limits taken
// from the 51 branches of parseCostPart.
// Deviations recorded in docs/crucible/porting/port-log/cost-strings.md.
package cost

import (
	"strconv"
	"strings"
)

// Cost is a parsed cost string.
type Cost struct {
	// Parts are the named parts, in the order the string writes them.
	Parts []Part
	// Mana holds the tokens no named part claimed. Java appends them to a mana
	// string and hands that to the mana cost parser, so an unrecognised part is
	// silently mana rather than an error.
	Mana []string

	// Tap and Untap are `T`/`Tap` and `Q`/`Untap`. They are read in a pass of
	// their own before anything else, because two named parts take the flags as
	// constructor arguments.
	Tap   bool
	Untap bool
	// Mandatory is the `Mandatory` token, which makes the cost unskippable.
	Mandatory bool
	// XMin is an `XMin...` token, kept as written.
	XMin string

	// Text is the cost exactly as the script wrote it.
	Text string
}

// IsPureMana reports whether the cost is nothing but mana symbols -- no
// named Part, no Tap/Untap/Mandatory token, and no XMin. A caller that can
// only pay mana (Game.PayManaCost and its own callers) uses this to decide
// whether it can attempt payment at all, rather than reading every other
// field itself.
func (c Cost) IsPureMana() bool {
	return len(c.Parts) == 0 && !c.Tap && !c.Untap && !c.Mandatory && c.XMin == ""
}

// ActivationShape is a Cost decomposed into the primitives ActivateAbility/
// ActivateManaAbility (internal/engine) know how to pay, past the plain mana
// in Cost.Mana itself -- an optional Tap-self token, an optional
// self-sacrifice (Sac<1/CARDNAME>, "sacrifice this permanent," fetch lands'
// and sac outlets' own dominant real shape), an optional self-exile
// (Exile<1/CARDNAME>, the identical self-reference shape for exile rather
// than sacrifice), an optional self-return (Return<1/CARDNAME>, the
// identical self-reference shape for "return to hand"), an optional
// self-exert (Exert<1/CARDNAME>, CR 701.42a's own identical self-reference
// shape for "exert this permanent" -- every real corpus Exert<...> cost line
// names the self-reference shape, no chosen-type sibling exists the way
// Sac/Exile/Return each have one), an optional "discard N cards of your
// choice" (Discard<N/Card>), an optional "pay N life" (PayLife<N>), an
// optional "pay N energy counters" (PayEnergy<N>), an optional "tap N
// untapped permanents of a type" (tapXType<N/Type>), an optional
// "return N permanents of a type you control to their owner's hand"
// (Return<N/Type>, tapXType's own sibling: past SelfReturn, "any number
// greater than one" is always a choice among many, its own type spec
// carried through unparsed for the identical reason TapTypeSpec's own is),
// an optional "put N counters of a kind on this permanent" (AddCounter<N/
// Type>, CR 606's own dominant loyalty-ability cost shape -- Ajani
// Goldmane's own real "[+1]: You gain 2 life," AddCounter<1/LOYALTY>), and
// an optional "remove N counters of a kind from this permanent"
// (SubCounter<N/Type>, AddCounter's own mirror image -- Ajani's own real
// "[-1]:"/"[-6]:" abilities), an optional self-exile-from-the-graveyard
// (ExileFromGrave<1/CARDNAME>, SelfExile's own sibling for CostExile.java's
// other real "from" zone -- an Escape/Unearth-style ability's own dominant
// real cost, activated from the graveyard rather than the battlefield), an
// optional self-discard (Discard<1/CARDNAME>, CR 702.28's own Cycling and
// its own kin's dominant real cost, activated from the hand), and an
// optional self-exile-from-hand (ExileFromHand<1/CARDNAME>,
// SelfExileFromGrave's own sibling for CostExile.java's third real "from"
// zone -- Hand -- SelfDiscard's own sibling for "exile" rather than
// "discard"). Each started as its own predicate
// (IsPureManaOrTap, then IsPureManaTapAndSelfSac, SelfSac) before this type
// replaced all three the first three grew into: Discard's own count could
// not fit a bool the way Tap and SelfSac could, and three near-identical
// predicates was already the sign a fourth should not be a fourth. Every
// primitive since slotted into the same struct rather than becoming that
// fourth (then fifth, sixth, seventh, eighth, ninth, tenth, eleventh,
// twelfth, thirteenth, fourteenth, fifteenth) predicate all over again.
type ActivationShape struct {
	Tap bool
	// SelfSac is Sac<1/CARDNAME>'s (or Sac<1/NICKNAME>'s -- CostPart.java's
	// own payCostFromSource, which accepts either token as "the ability's
	// own host card") own presence -- the literal self-reference token,
	// never a chosen count or a chosen valid spec.
	SelfSac bool
	// SelfExile is Exile<1/CARDNAME|NICKNAME>'s own presence -- SelfSac's
	// own sibling, mutually exclusive with it in every real corpus line (a
	// permanent is never both sacrificed and exiled by the same cost).
	SelfExile bool
	// SelfReturn is Return<1/CARDNAME|NICKNAME>'s own presence -- SelfSac's
	// own second sibling, "return this permanent to its owner's hand,"
	// mutually exclusive with both SelfSac and SelfExile in every real
	// corpus line.
	SelfReturn bool
	// SelfExert is Exert<1/CARDNAME|NICKNAME>'s own presence -- SelfSac's
	// own third sibling, "exert this permanent" (CR 701.42a), mutually
	// exclusive with SelfSac/SelfExile/SelfReturn in every real corpus line
	// (exerting a permanent that also leaves the battlefield in the same
	// cost payment would have nothing left to not-untap next turn).
	SelfExert bool
	// DiscardN is Discard<N/Card>'s own N, or 0 when the cost names no
	// Discard part at all. Never negative -- ActivationShape's own second
	// result is false for anything that would make it so.
	DiscardN int
	// PayLifeN is PayLife<N>'s own N, or 0 when the cost names no PayLife
	// part at all. Never negative, the identical guarantee DiscardN carries.
	PayLifeN int
	// PayEnergyN is PayEnergy<N>'s own N, or 0 when the cost names no
	// PayEnergy part at all. Never negative, the identical guarantee DiscardN
	// and PayLifeN both carry.
	PayEnergyN int
	// TapTypeN is tapXType<N/Type>'s own N, or 0 when the cost names no
	// tapXType part at all. Never negative, the identical guarantee every
	// other N field carries.
	TapTypeN int
	// TapTypeSpec is tapXType<N/Type>'s own Type field, verbatim -- a
	// semicolon-separated OR list in Cost syntax (Cost's own choice, since a
	// literal comma can appear in the trailing description field a valid
	// string's own comma-separated OR syntax would collide with), never
	// itself parsed or validated here. Empty exactly when TapTypeN is 0.
	TapTypeSpec string
	// TapTypeTotalPower is the "any number of ... with total power N or more"
	// form (Crew N: tapXType<Any/Creature.Other+withTotalPowerGEN>): N, with
	// TapTypeN 1 (at least one permanent) and TapTypeSpec the type with the
	// withTotalPowerGE suffix removed. 0 for a plain tapXType.
	TapTypeTotalPower int
	// ReturnTypeN is Return<N/Type>'s own N for a Type past CARDNAME/
	// NICKNAME, or 0 when the cost names no such Return part at all (SelfReturn
	// covers the self-reference shape separately). Never negative, the
	// identical guarantee every other N field carries.
	ReturnTypeN int
	// ReturnTypeSpec is Return<N/Type>'s own Type field, verbatim --
	// TapTypeSpec's own identical unparsed-OR-list contract. Empty exactly
	// when ReturnTypeN is 0.
	ReturnTypeSpec string
	// AddCounterN is AddCounter<N/Type>'s own N, for the self-reference shape
	// only (CostPart.java's own payCostFromSource -- an absent third field
	// defaults to "CARDNAME" in Java's own constructor, ported as
	// isSelfReferenceField treating "" the identical way). Unlike every
	// other N field, 0 is a real value here, not "absent" -- CR 606's own
	// "+0" loyalty ability (AddCounter<0/LOYALTY>, 54 real corpus lines) is
	// exactly as legal a cost as any positive one, so AddCounterType, not
	// this field, is what ActivationShape's own second result checks for
	// presence.
	AddCounterN int
	// AddCounterType is AddCounter<N/Type>'s own CounterType field, verbatim
	// and uncanonicalized -- internal/cost has no dependency on the engine's
	// own CounterType casing rule (CounterEnumType.getType's own
	// uppercasing), the identical reason TapTypeSpec/ReturnTypeSpec both stay
	// raw. Empty exactly when the cost names no self-reference AddCounter
	// part at all.
	AddCounterType string
	// SubCounterN is SubCounter<N/Type>'s own N, the self-reference shape
	// only, AddCounterN's own mirror image for "remove" rather than "add" --
	// 0 is a real value here too (SubCounter<0/LOYALTY>, 5 real corpus
	// lines, an oddly-spelled "+0" ability some cards write this way
	// instead).
	SubCounterN int
	// SubCounterType is SubCounter<N/Type>'s own CounterType field, verbatim
	// -- AddCounterType's own mirror image. Empty exactly when the cost
	// names no self-reference SubCounter part at all.
	SubCounterType string
	// SelfExileFromGrave is ExileFromGrave<1/CARDNAME|NICKNAME>'s own
	// presence -- SelfExile's own sibling for CostExile.java's other real
	// "from" zone (Graveyard rather than Battlefield, the identical class,
	// a different ZoneType constructor argument): "exile CARDNAME from your
	// graveyard," the dominant real cost of an Escape/Unearth-style ability
	// activated from the graveyard rather than the battlefield
	// (ActivationZone$ Graveyard, ActivateAbility's own doc comment). Unlike
	// SelfExile, this is never combined with a battlefield-only primitive in
	// any real corpus line (Tap/SelfSac/Discard/... all assume a permanent
	// already on the battlefield, which a graveyard card is not), so
	// ActivateAbility itself -- not this decomposition -- is what refuses a
	// Graveyard-zone ability naming any of those.
	SelfExileFromGrave bool
	// SelfDiscard is Discard<1/CARDNAME|NICKNAME>'s own presence -- CR
	// 702.28's own Cycling and its own kin's dominant real cost, "discard
	// this card" -- distinct from DiscardN's own "discard N cards of your
	// choice" shape (Field(1) is "Card" there, never a self-reference token,
	// so the two cases can never both match one Part). Only meaningful for
	// an ability activated from the hand (ActivationZone$ Hand,
	// ActivateAbility's own doc comment); ActivateAbility itself, not this
	// decomposition, is what refuses a Battlefield-zone ability naming it.
	SelfDiscard bool
	// SelfExileFromHand is ExileFromHand<1/CARDNAME|NICKNAME>'s own presence
	// -- SelfExileFromGrave's own sibling for CostExile.java's third real
	// "from" zone (Hand rather than Battlefield or Graveyard), SelfDiscard's
	// own sibling for "exile this card" rather than "discard this card,"
	// both real corpus shapes an ActivationZone$ Hand ability pays with.
	SelfExileFromHand bool
	// The four chosen-card parts: N cards the payer picks among those matching
	// a type, each with its spec verbatim (a semicolon-separated OR list, as
	// TapTypeSpec is) and empty exactly when its N is 0. SacTypeN is
	// Sac<N/Type> past the self-reference token, a permanent the payer
	// controls; ExileTypeN is Exile<N/Type>, from the battlefield;
	// ExileGraveN is ExileFromGrave<N/Type>, from the payer's graveyard;
	// DiscardTypeN is Discard<N/Type> for a type that is not "Card" (that is
	// DiscardN) or one of CostDiscard's special words.
	SacTypeN        int
	SacTypeSpec     string
	ExileTypeN      int
	ExileTypeSpec   string
	ExileGraveN     int
	ExileGraveSpec  string
	DiscardTypeN    int
	DiscardTypeSpec string
	// CollectEvidenceN is CollectEvidence<N>'s own N (CostCollectEvidence:
	// exile graveyard cards with total mana value N or more), 0 when the cost
	// names no such part. Cryptex's "{T}, Collect evidence 3".
	CollectEvidenceN int
}

// chosenCardSpec reports whether field names a type to pick cards of: not the
// self-reference tokens, not "Card" (Discard's own plain shape), and none of the
// words CostDiscard, CostSacrifice and CostExile read as something other than
// a valid string ("Hand", "Random", "LastDrawn", "DifferentNames", "SameName",
// "All", "Any").
func chosenCardSpec(field string) bool {
	switch field {
	case "", "Card", "Hand", "Random", "LastDrawn", "DifferentNames", "SameName", "All", "Any":
		return false
	}
	return !isSelfReferenceField(field)
}

// isSelfReferenceField reports whether field is one of the two literal
// tokens CostPart.java's own payCostFromSource treats as "the ability's own
// host card" -- CARDNAME (the dominant real shape) or NICKNAME (an
// alternate-name reference a card with one carries, 11 real corpus lines
// across Sac<1/NICKNAME>/Exile<1/NICKNAME> that a CARDNAME-only check
// missed until this field existed to name it).
func isSelfReferenceField(field string) bool {
	return field == "CARDNAME" || field == "NICKNAME"
}

// ActivationShape reports whether the cost is nothing but mana symbols and
// zero or more of the fifteen primitives [ActivationShape] carries, decomposed
// into that value. The second result is false for anything past those --
// Untap/Mandatory/XMin, a chosen or SVar-sized Sac<...>/Exile<...>/
// Return<...>/Exert<...>/ExileFromGrave<...>/ExileFromHand<...>, a Discard<...> past the literal "1/CARDNAME|NICKNAME" self-reference or "N/Card" shape (a
// self-discard, a random discard, a type-restricted choice, ...), a
// PayLife<...> or PayEnergy<...> past a literal positive integer
// (PayLife<X>/PayEnergy<X> and their own kin -- an amount this port has no
// X-value/computed-total resolver to plug in here), a tapXType<...> or
// Return<...> naming a non-literal or non-positive N, an AddCounter<...> or
// SubCounter<...> naming a non-literal or negative N (X/All/X1+ and their
// own kin, the identical unresolved-amount reasoning) or a target field past
// the self-reference shape (a chosen permanent, "OriginalHost," a type
// list -- CostRemoveCounter.java's own non-self branch this decomposition
// does not carry), or any other named Part -- PORT-8/GO-7's "skip the whole
// line" applied at the cost's own shape rather than guessing at a partial
// payment.
func (c Cost) ActivationShape() (ActivationShape, bool) {
	if c.Untap || c.Mandatory || c.XMin != "" {
		return ActivationShape{}, false
	}
	var shape ActivationShape
	for _, p := range c.Parts {
		switch {
		case p.Name == "T":
			shape.Tap = true
		case p.Name == "Sac" && !shape.SelfSac && shape.SacTypeN == 0 && p.Field(0) == "1" && isSelfReferenceField(p.Field(1)):
			shape.SelfSac = true
		case p.Name == "Exile" && !shape.SelfExile && shape.ExileTypeN == 0 && p.Field(0) == "1" && isSelfReferenceField(p.Field(1)):
			shape.SelfExile = true
		case p.Name == "Return" && !shape.SelfReturn && shape.ReturnTypeN == 0 && p.Field(0) == "1" && isSelfReferenceField(p.Field(1)):
			shape.SelfReturn = true
		case p.Name == "Exert" && !shape.SelfExert && p.Field(0) == "1" && isSelfReferenceField(p.Field(1)):
			shape.SelfExert = true
		case p.Name == "Discard" && !shape.SelfDiscard && shape.DiscardN == 0 && p.Field(0) == "1" && isSelfReferenceField(p.Field(1)):
			shape.SelfDiscard = true
		case p.Name == "Discard" && shape.DiscardN == 0 && p.Field(1) == "Card":
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.DiscardN = n
		case p.Name == "Discard" && shape.DiscardTypeN == 0 && chosenCardSpec(p.Field(1)):
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.DiscardTypeN, shape.DiscardTypeSpec = n, p.Field(1)
		case p.Name == "Sac" && !shape.SelfSac && shape.SacTypeN == 0 && chosenCardSpec(p.Field(1)):
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.SacTypeN, shape.SacTypeSpec = n, p.Field(1)
		case p.Name == "Exile" && !shape.SelfExile && shape.ExileTypeN == 0 && chosenCardSpec(p.Field(1)):
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.ExileTypeN, shape.ExileTypeSpec = n, p.Field(1)
		case p.Name == "ExileFromGrave" && !shape.SelfExileFromGrave && shape.ExileGraveN == 0 && chosenCardSpec(p.Field(1)):
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.ExileGraveN, shape.ExileGraveSpec = n, p.Field(1)
		case p.Name == "PayLife" && shape.PayLifeN == 0:
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.PayLifeN = n
		case p.Name == "PayEnergy" && shape.PayEnergyN == 0:
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.PayEnergyN = n
		case p.Name == "tapXType" && shape.TapTypeN == 0 && p.Field(0) == "Any" && p.Field(1) != "":
			spec, power, ok := anyWithTotalPower(p.Field(1))
			if !ok {
				return ActivationShape{}, false
			}
			shape.TapTypeN, shape.TapTypeSpec, shape.TapTypeTotalPower = 1, spec, power
		case p.Name == "tapXType" && shape.TapTypeN == 0 && p.Field(1) != "":
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.TapTypeN = n
			shape.TapTypeSpec = p.Field(1)
		case p.Name == "Return" && !shape.SelfReturn && shape.ReturnTypeN == 0 && p.Field(1) != "" && !isSelfReferenceField(p.Field(1)):
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.ReturnTypeN = n
			shape.ReturnTypeSpec = p.Field(1)
		case p.Name == "AddCounter" && shape.AddCounterType == "" && (p.Field(2) == "" || isSelfReferenceField(p.Field(2))):
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n < 0 {
				return ActivationShape{}, false
			}
			shape.AddCounterN = n
			shape.AddCounterType = p.Field(1)
		case p.Name == "SubCounter" && shape.SubCounterType == "" && (p.Field(2) == "" || isSelfReferenceField(p.Field(2))):
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n < 0 {
				return ActivationShape{}, false
			}
			shape.SubCounterN = n
			shape.SubCounterType = p.Field(1)
		case p.Name == "ExileFromGrave" && !shape.SelfExileFromGrave && p.Field(0) == "1" && isSelfReferenceField(p.Field(1)):
			shape.SelfExileFromGrave = true
		case p.Name == "ExileFromHand" && !shape.SelfExileFromHand && p.Field(0) == "1" && isSelfReferenceField(p.Field(1)):
			shape.SelfExileFromHand = true
		case p.Name == "CollectEvidence" && shape.CollectEvidenceN == 0:
			n, err := strconv.Atoi(p.Field(0))
			if err != nil || n <= 0 {
				return ActivationShape{}, false
			}
			shape.CollectEvidenceN = n
		default:
			return ActivationShape{}, false
		}
	}
	return shape, true
}

// Part is one named cost part: a name and the fields of its `<...>` body.
type Part struct {
	// Name is the part's name, without the body.
	Name string
	// Fields are the `/`-separated body fields, split with this part's own
	// limit, so a description containing a slash keeps it.
	Fields []string
	// Text is the part as written, body included.
	Text string
}

// Field returns a body field by position, empty when the body has no such
// field. Java reads its optional fields exactly this way -- a length check and
// a default -- so the zero value is a real answer rather than a missing one.
func (p Part) Field(i int) string {
	if i < len(p.Fields) {
		return p.Fields[i]
	}
	return ""
}

// Parse reads a cost string.
//
// It never fails, because Java's constructor cannot: a token matching no named
// part becomes mana, and a malformed mana token is the mana parser's problem
// later.
func Parse(text string) Cost {
	out := Cost{Text: text}
	if strings.TrimSpace(text) == "" {
		return out
	}

	// Bracket-aware, because 12% of `<...>` bodies contain a space: splitting
	// on whitespace first would cut `Sac<1/Creature.Other/another creature>`
	// in half.
	tokens := split(text, ' ', maxEntries, '<', '>')

	// Java's own pre-pass. CostTapType and CostUntapType are constructed with
	// these flags, so they have to be known before any part is built.
	for _, token := range tokens {
		switch token {
		case "T", "Tap":
			out.Tap = true
		case "Q", "Untap":
			out.Untap = true
		}
	}

	for _, token := range tokens {
		switch {
		case strings.HasPrefix(token, "XMin"):
			out.XMin = token
		case token == "Mandatory":
			out.Mandatory = true
		default:
			if part, ok := parsePart(token); ok {
				out.Parts = append(out.Parts, part)
				continue
			}
			out.Mana = append(out.Mana, token)
		}
	}
	return out
}

// parsePart reads one token as a named cost part.
func parsePart(token string) (Part, bool) {
	for _, named := range namedParts {
		if named.exact {
			if token != named.prefix {
				continue
			}
		} else if !strings.HasPrefix(token, named.prefix) {
			continue
		}
		part := Part{Name: named.name, Text: token}
		if named.fields > 0 {
			part.Fields = split(bodyOf(token), '/', named.fields, 0, 0)
		}
		return part, true
	}
	return Part{}, false
}

// bodyOf returns what sits between the first `<` and the next `>`, which is
// abCostParse's substring.
func bodyOf(token string) string {
	open := strings.IndexByte(token, '<')
	if open < 0 {
		return ""
	}
	end := strings.IndexByte(token[open+1:], '>')
	if end < 0 {
		return token[open+1:]
	}
	return token[open+1 : open+1+end]
}

// String writes the cost back as it was read.
func (c Cost) String() string { return c.Text }

// maxEntries is Java's Integer.MAX_VALUE, meaning no cap.
const maxEntries = int(^uint(0) >> 1)

// anyWithTotalPower splits "Creature.Other+withTotalPowerGE3" into the type
// ("Creature.Other") and the power (3), false for any other shape.
func anyWithTotalPower(spec string) (string, int, bool) {
	const marker = "+withTotalPowerGE"
	i := strings.LastIndex(spec, marker)
	if i < 0 {
		return "", 0, false
	}
	n, err := strconv.Atoi(spec[i+len(marker):])
	if err != nil || n <= 0 {
		return "", 0, false
	}
	return spec[:i], n, true
}
