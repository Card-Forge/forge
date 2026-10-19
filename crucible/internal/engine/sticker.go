// Stickers (CR 123): a sticker sheet is a card in the StickerSheets zone, a
// placed sticker an appliedSticker on the card that carries it (ADR-0044).
// The sticker's own data is compiled onto the sheet (compile.Sticker); this
// file holds the state a placement leaves, the availability rule and the
// Layer 7b effect of a PT sticker. Ported from forge-game's
// card/sticker/StickerSheet.java, AppliedSticker.java and Card.java:4617-4698.

package engine

import (
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// Ticket is the player counter a sticker's cost is paid with
// (PutStickerEffect.java:124-126).
const Ticket CounterType = "TICKET"

// StickerOption is one sticker a player may place: its sheet, its slot on it
// and the data a controller chooses by (Sticker.java's description).
type StickerOption struct {
	Sheet   CardID
	Slot    string
	Kind    compile.StickerKind
	Tickets int
	// Word is a Name sticker's word; Text an Ability or PT sticker's rules
	// text.
	Word, Text string
}

// appliedSticker is Java's AppliedSticker (AppliedSticker.java:332-334): a
// sticker placed on a card, in placement order, which is timestamp order.
type appliedSticker struct {
	sheet     CardID
	slot      string
	timestamp uint64
	// namePos is where a Name sticker's word goes among the blanks of the
	// card's name (chooseStickerNamePosition).
	namePos int
}

// hiddenZone is ZoneType.isHidden (ZoneType.java:14-34): stickers do not
// survive a move into one (GameAction.java:268-270).
func hiddenZone(z ZoneType) bool {
	switch z {
	case Hand, Library, Sideboard, SchemeDeck, PlanarDeck, AttractionDeck, ContraptionDeck, Subgame, ExtraHand, None:
		return true
	}
	return false
}

// stickerAt is the compiled sticker Slot names on sheet, nil when the sheet
// has no such slot (a sheet's slots are fixed once loaded).
func (g *Game) stickerAt(sheet CardID, slot string) *compile.Sticker {
	def := g.Card(sheet).Def
	if def == nil {
		return nil
	}
	for _, face := range def.Faces {
		for i := range face.Stickers {
			if face.Stickers[i].Slot == slot {
				return &face.Stickers[i]
			}
		}
	}
	return nil
}

// stickerInUse is StickerSheet.collectAvailable's exclusion (:53-58): the
// sticker already sits on a card in a public zone of the player's -- a
// sticker is placed once (CR 123.3a).
func (g *Game) stickerInUse(pid PlayerID, sheet CardID, slot string) bool {
	for z := ZoneType(0); int(z) < numZoneTypes; z++ {
		if hiddenZone(z) {
			continue
		}
		for _, id := range g.Zone(z, pid).Cards() {
			for _, st := range g.Card(id).stickers {
				if st.sheet == sheet && st.slot == slot {
					return true
				}
			}
		}
	}
	return false
}

// availableStickers is StickerSheet.getAvailableStickers (:39-70) with
// PutStickerEffect.availableStickers' kind filter (:86-87): every
// implemented sticker on the player's sheets that costs at most budget
// tickets, is not placed yet and, when only is set, has that kind. Sheets and
// slots in zone order, so a controller sees the same list every run (GO-12).
func (g *Game) availableStickers(pid PlayerID, budget int, only *compile.StickerKind) []StickerOption {
	var out []StickerOption
	for _, sheet := range g.Zone(StickerSheets, pid).Cards() {
		def := g.Card(sheet).Def
		if def == nil {
			continue
		}
		for _, face := range def.Faces {
			for i := range face.Stickers {
				st := &face.Stickers[i]
				if !st.Implemented() || st.Tickets > budget || (only != nil && st.Kind != *only) {
					continue
				}
				if g.stickerInUse(pid, sheet, st.Slot) {
					continue
				}
				out = append(out, StickerOption{Sheet: sheet, Slot: st.Slot, Kind: st.Kind, Tickets: st.Tickets, Word: st.Word, Text: st.Text})
			}
		}
	}
	return out
}

// addSticker is Card.addSticker (Card.java:4634): the sticker joins the card's
// list with a fresh timestamp. Its effect is rebuilt from the list every
// state-based pass (stickerPT), so nothing is applied here beyond running
// that pass's own input.
func (g *Game) addSticker(target CardID, o StickerOption, namePos int) {
	g.timestamp++
	c := g.Card(target)
	c.stickers = append(c.stickers[:len(c.stickers):len(c.stickers)], appliedSticker{sheet: o.Sheet, slot: o.Slot, timestamp: g.timestamp, namePos: namePos})
}

// Stickers is the card's placed stickers' sheet and slot, in placement order.
func (c *Card) Stickers() []StickerOption {
	out := make([]StickerOption, 0, len(c.stickers))
	for _, st := range c.stickers {
		out = append(out, StickerOption{Sheet: st.sheet, Slot: st.slot})
	}
	return out
}

// stickerPT is AppliedSticker.applyEffect's PT branch (:58-67), Card.addNewPT:
// a PT sticker sets the card's base power and toughness at its own timestamp,
// Layer 7b. Rebuilt every pass beside animatePT, because applyContinuousPT
// clears the effects it reads first; the list on the card is the state.
func stickerPT(g *Game) {
	for i := 1; i < len(g.cards); i++ {
		c := &g.cards[i]
		for _, st := range c.stickers {
			def := g.stickerAt(st.sheet, st.slot)
			if def == nil || def.Kind != compile.StickerPT {
				continue
			}
			c.PT.Add(PTEffect{
				Layer: LayerSetPT, Timestamp: st.timestamp,
				Power: def.Power, Toughness: def.Toughness, HasPower: true, HasToughness: true,
			})
		}
	}
}

// newPTEffects re-adds every card's addNewPT list as Layer 7b set effects,
// beside stickerPT: applyContinuousPT clears the effects it reads first, so
// the list on the card is the state.
func newPTEffects(g *Game) {
	for i := 1; i < len(g.cards); i++ {
		c := &g.cards[i]
		for _, e := range c.newPTs {
			c.PT.Add(PTEffect{
				Layer: LayerSetPT, Timestamp: e.timestamp,
				Power: e.power, Toughness: e.toughness, HasPower: true, HasToughness: true,
			})
		}
	}
}

// nameBlank is Card.NAME_BLANK's first match (Card.java:4697, `_{2,}`): the
// byte range of the first run of two or more underscores in name, or nil.
func nameBlank(name string) []int {
	for i := 0; i < len(name); i++ {
		if name[i] != '_' {
			continue
		}
		j := i
		for j < len(name) && name[j] == '_' {
			j++
		}
		if j-i >= 2 {
			return []int{i, j}
		}
		i = j
	}
	return nil
}

// addStickerWord is Card.addStickerWord (Card.java:4699-4709): a Name sticker
// fills the leftmost blank still empty (CR 123.6a); with none left it goes in
// after position words, clamped to the end.
func addStickerWord(name, word string, position int) string {
	if loc := nameBlank(name); loc != nil {
		return name[:loc[0]] + word + name[loc[1]:]
	}
	words := strings.Fields(name)
	position = min(max(position, 0), len(words))
	words = append(words[:position:position], append([]string{word}, words[position:]...)...)
	return strings.Join(words, " ")
}

// applyStickerNames is Card.recomputeStickerName (Card.java:4677-4693) for
// every card: its Name stickers apply in timestamp order (CR 123.6c), the
// order the list is already in, to its printed name. Rebuilt every pass from
// the list, like stickerPT.
func applyStickerNames(g *Game) {
	for i := 1; i < len(g.cards); i++ {
		c := &g.cards[i]
		c.stickerName = ""
		if c.Def == nil {
			continue
		}
		name, changed := c.Def.Name, false
		for _, st := range c.stickers {
			def := g.stickerAt(st.sheet, st.slot)
			if def == nil || def.Kind != compile.StickerName {
				continue
			}
			name, changed = addStickerWord(name, def.Word, st.namePos), true
		}
		if changed {
			c.stickerName = name
		}
	}
}

// stickerAbilities is AppliedSticker.grantAbility (:84-100) for every card:
// an Ability sticker adds its Keywords$ lines to the card's keywords and its
// Ability$, Triggers$ and Statics$ bodies as one trait grant, at the
// sticker's timestamp. It runs where animateAbilities does and is rebuilt
// every pass (ADR-0023 decision 2): the list on the card is the state, the
// grant is its overlay. The traits' SVars read the sheet's, whose amounts they
// carry (getGrantedTraits reads them off the sheet's current state, :115-118).
func stickerAbilities(g *Game) {
	for i := 1; i < len(g.cards); i++ {
		c := &g.cards[i]
		for _, st := range c.stickers {
			def := g.stickerAt(st.sheet, st.slot)
			if def == nil || def.Kind != compile.StickerAbility {
				continue
			}
			if len(def.Keywords) > 0 {
				c.KeywordMod.Add(KeywordEffect{Timestamp: st.timestamp, AddKeywords: def.Keywords})
			}
			if len(def.Abilities) > 0 || len(def.Triggers) > 0 || len(def.Statics) > 0 {
				c.traitGrants = append(append([]traitGrant(nil), c.traitGrants...), traitGrant{
					abilities: def.Abilities, triggers: def.Triggers, statics: def.Statics,
					amounts: g.Card(st.sheet).Def.Faces[0].Amounts,
				})
			}
		}
	}
}

// gainedSticker is an Ability sticker a static reads the traits of, with the
// sheet whose SVars they resolve against.
type gainedSticker struct {
	def   *compile.Sticker
	sheet CardID
}

// gainedStickers is StaticAbilityContinuous.cardsGainedFrom (:1043-1057) for
// GainsStickerAbilitiesOf$ / OfDefined$ and the loop over the stickers of the
// cards it names (:316-331): the Ability stickers on them, in card then
// placement order. GainsStickerAbilitiesOf$ is a valid string over the zones
// GainsAbilitiesOfZones$ lists (the battlefield by default), OfDefined$ a
// Defined$ spec. ok is false for a spec this port cannot resolve (GO-7); a
// static naming neither returns nothing.
func (g *Game) gainedStickers(host *Card, s *compile.Ability) (out []gainedSticker, ok bool) {
	var sources []CardID
	if defined, has := s.Param("GainsStickerAbilitiesOfDefined"); has {
		cards, err := definedCards(host, defined, abilityRefs{})
		if err != nil {
			return nil, false
		}
		sources = cards
	} else if spec, has := s.Param("GainsStickerAbilitiesOf"); has {
		zones := []ZoneType{Battlefield}
		if raw, has := s.Param("GainsAbilitiesOfZones"); has {
			zones = nil
			for _, name := range strings.Split(raw, ",") {
				z, known := ZoneByName(strings.TrimSpace(name))
				if !known {
					return nil, false
				}
				zones = append(zones, z)
			}
		}
		parsed := valid.Parse(spec)
		for _, z := range zones {
			for _, pid := range g.Players() {
				for _, id := range g.Zone(z, pid).Cards() {
					if Matches(g, g.Card(id), parsed, host.Controller(), host.ID) {
						sources = append(sources, id)
					}
				}
			}
		}
	} else {
		return nil, true
	}
	for _, id := range sources {
		for _, st := range g.Card(id).stickers {
			if def := g.stickerAt(st.sheet, st.slot); def != nil && def.Kind == compile.StickerAbility {
				out = append(out, gainedSticker{def: def, sheet: st.sheet})
			}
		}
	}
	return out, true
}

// gainedStickerGrants is gainedStickers as trait grants, one per sticker: its
// traits resolve against its own sheet's SVars, not the static's
// (StaticAbilityContinuous.java:802-803), so each carries that sheet's amounts.
func (g *Game) gainedStickerGrants(host *Card, s *compile.Ability) ([]traitGrant, bool) {
	stickers, ok := g.gainedStickers(host, s)
	if !ok {
		return nil, false
	}
	var out []traitGrant
	for _, gs := range stickers {
		def := gs.def
		if len(def.Abilities) == 0 && len(def.Triggers) == 0 && len(def.Statics) == 0 {
			continue
		}
		out = append(out, traitGrant{abilities: def.Abilities, triggers: def.Triggers, statics: def.Statics, amounts: g.Card(gs.sheet).Def.Faces[0].Amounts})
	}
	return out, true
}

// hasStickerKindNamed reports whether c carries a sticker of the kind name
// spells; a name that is no kind matches nothing (StickerKind.smartValueOf is
// null, CardProperty.java:1340-1344).
func (g *Game) hasStickerKindNamed(c *Card, name string) bool {
	k, ok := compile.StickerKindByName(name)
	if !ok {
		return false
	}
	for _, st := range c.stickers {
		if def := g.stickerAt(st.sheet, st.slot); def != nil && def.Kind == k {
			return true
		}
	}
	return false
}

// stickerLetters is Sticker.getLetters (Sticker.java:82-84): the word's ASCII
// letters.
func stickerLetters(word string) string {
	var b strings.Builder
	for _, r := range word {
		if (r >= 'A' && r <= 'Z') || (r >= 'a' && r <= 'z') {
			b.WriteRune(r)
		}
	}
	return b.String()
}

// uniqueVowels is Sticker.getUniqueVowelCount (Sticker.java:86-95): how many of
// A, E, I, O, U, Y the word's letters hold.
func uniqueVowels(word string) int {
	letters := strings.ToUpper(stickerLetters(word))
	n := 0
	for _, v := range "AEIOUY" {
		if strings.ContainsRune(letters, v) {
			n++
		}
	}
	return n
}

// countStickers is AbilityUtils.countStickers (:2897-2955) for Count$CardStickers
// on c, sq the parts after the head: nothing counts every sticker; a kind
// counts those of it; a Name sticker with a further argument is read by
// NameStartsWith (the argument's first letter, ChosenType being c's chosen
// type), NameLetter (each occurrence of its letter), NameMinLetters and
// NameMaxLetters (the word's length against the number), all case-insensitive.
// ok is false for a number that is not one (Java throws).
func (g *Game) countStickers(c *Card, sq []string) (int, bool) {
	if len(sq) == 0 || sq[0] == "" {
		return len(c.stickers), true
	}
	kind := sq[0]
	arg, hasArg := "", len(sq) > 1
	if hasArg {
		arg = sq[1]
	}
	count := 0
	for _, st := range c.stickers {
		def := g.stickerAt(st.sheet, st.slot)
		if def == nil {
			continue
		}
		if def.Kind == compile.StickerName && hasArg {
			letters := stickerLetters(def.Word)
			switch kind {
			case "NameStartsWith":
				wanted := arg
				if arg == "ChosenType" {
					wanted = c.Memory.ChosenType(false)
				}
				if letters != "" && wanted != "" && strings.EqualFold(letters[:1], wanted[:1]) {
					count++
				}
				continue
			case "NameLetter":
				if arg == "" {
					return 0, false
				}
				count += strings.Count(strings.ToUpper(letters), strings.ToUpper(arg[:1]))
				continue
			case "NameMinLetters", "NameMaxLetters":
				n, err := strconv.Atoi(arg)
				if err != nil {
					return 0, false
				}
				if (kind == "NameMinLetters" && len(letters) >= n) || (kind == "NameMaxLetters" && len(letters) <= n) {
					count++
				}
				continue
			}
		}
		if want, ok := compile.StickerKindByName(kind); ok && def.Kind == want {
			count++
		}
	}
	return count, true
}

// stickerPTSum is the sum of the power or toughness of the PT stickers on cards
// (AbilityUtils.java:3784-3791, StickerPower and StickerToughness).
func (g *Game) stickerPTSum(cards []*Card, power bool) int {
	total := 0
	for _, c := range cards {
		for _, st := range c.stickers {
			def := g.stickerAt(st.sheet, st.slot)
			if def == nil || def.Kind != compile.StickerPT {
				continue
			}
			if power {
				total += def.Power
			} else {
				total += def.Toughness
			}
		}
	}
	return total
}

// stickerSheetsKept is DeckFormat.CHOSEN_STICKER_SHEETS (DeckFormat.java:138):
// how many of a deck's sheets a player keeps (CR 123.2a/c).
const stickerSheetsKept = 3

// SeatStickerSheets is the sticker half of Player.java:3047-3053: of a deck's
// Stickers section, pid keeps stickerSheetsKept sheets chosen at random,
// Aggregates.random's reservoir sample (Aggregates.java:156-171) drawn from
// the game's own generator, and they sit in pid's StickerSheets zone. It
// returns the sheets' IDs in the order they were seated.
func (g *Game) SeatStickerSheets(pid PlayerID, sheets []*compile.Card) []CardID {
	var kept []*compile.Card
	for i, sheet := range sheets {
		if i < stickerSheetsKept {
			kept = append(kept, sheet)
		} else if j := int(g.rand.Int32n(int32(i + 1))); j < stickerSheetsKept {
			kept[j] = sheet
		}
	}
	ids := make([]CardID, 0, len(kept))
	for _, sheet := range kept {
		ids = append(ids, g.NewCard(sheet, pid, StickerSheets))
	}
	return ids
}
