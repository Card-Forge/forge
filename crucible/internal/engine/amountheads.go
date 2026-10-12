// Amount heads: the Count$ and PlayerCount$ measurements resolveAmount
// (amount.go) evaluates -- each a direct read of state this port already
// models (a player's life, a zone's cards, a permanent's counters, a card's
// printed mana cost), dispatched on the exact head name xCount itself would
// reach for it. Every head here was checked against xCount's own if-chain
// for an earlier `contains`/`startsWith` branch that would catch it first;
// none does.
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// xCount (:1566), playerXCount (:3288) and playerXProperty (:3420), and
// forge-game/src/main/java/forge/game/staticability/StaticAbilityDevotion.java.

package engine

import (
	"math"
	"slices"
	"sort"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// countValue is xCount for a Count$ body already read at load (expr.Count):
// the Valid family (validFamilyValue, amountpaid.go), then the exact heads
// below. Any other head -- Party, YourTurns and the rest of xCount's
// own two hundred-odd branches -- reports false (GO-7).
//
//   - xPaid: the X the resolving root ability announced (Game.xctx), else the
//     source card's own cast X (Card.castX); CardPower/CardToughness: the
//     source's net power/toughness.
//   - YourLifeTotal: the controller's life (Player.getLife);
//     OppGreatestLifeTotal, the highest among its opponents.
//   - YouDrewThisTurn: cards the controller drew this turn
//     (Player.getNumDrawnThisTurn; Player.CardsDrawnThisTurn here).
//   - NumInAllHands: every card in every hand -- no earlier xCount branch
//     matches it, so it reaches getCardListForXCount, whose only matching
//     qualifier is "InAllHands" (game.getCardsIn(Hand)).
//   - Domain/DomainActivePlayer: basic land types among the controller's (or
//     the active player's) lands (domainCount).
//   - Devotion.<Color>/DevotionDual.<Color>.<Color>: devotionCount.
//   - Chroma[.<Color>]/ChromaInGrave[.<Color>]/ChromaSource[.<Color>]:
//     chromaCount.
//   - CardCounters.<TYPE>|ALL: counters on source itself (c.getCounters /
//     getNumAllCounters) -- the host, since calculateAmount hands xCount the
//     card the amount is read for.
//   - ChosenNumber: source's own chosen number, 0 when none was chosen
//     (`i == null ? 0 : i`).
func countValue(g *Game, sourceController PlayerID, source CardID, count expr.Count) (int, bool) {
	if expr.IsValidHead(count.Head) {
		return validFamilyValue(g, sourceController, source, count)
	}
	if spec, ok := strings.CutPrefix(count.Head, "ThisTurnCast_"); ok {
		if count.Argument != "" {
			spec += " " + count.Argument // a valid string with a space in it (ControlledBy Player.Active)
		}
		return g.thisTurnCastCount(source, spec)
	}
	if strings.HasPrefix(count.Head, "ThisTurnEntered_") {
		if sourceController == NoPlayer {
			return 0, false
		}
		// ParseCount cut the head at the first `.` and the first space; the
		// zone split reads the string whole (AbilityUtils.java:2806).
		text := count.Head
		if len(count.Parameters) > 0 {
			text += "." + strings.Join(count.Parameters, ".")
		}
		if count.Argument != "" {
			text += " " + count.Argument
		}
		return g.thisTurnEnteredCount(sourceController, source, text)
	}
	switch count.Head {
	case "Intensity":
		// AbilityUtils.java:2094: Card.getIntensity(true), the host's own.
		if source == NoCard {
			return 0, false
		}
		return g.Card(source).Intensity + startingIntensity(g.Card(source)), true
	case "CardCounters":
		if source == NoCard || len(count.Parameters) == 0 {
			return 0, false
		}
		c := g.Card(source)
		if count.Parameters[0] == "ALL" {
			return c.Counters.Total(), true
		}
		return c.Counters.Count(CounterType(strings.ToUpper(count.Parameters[0]))), true
	case "xPaid":
		// AbilityUtils.java:1631: the root ability's announced X when it has
		// one, else the source card's own (Card.getXManaCostPaid).
		if g.xctx.has && g.xctx.source == source {
			return g.xctx.value, true
		}
		if source != NoCard && source == g.castPending {
			// The spell being cast: the X announced before its targets, none
			// yet for a spell with no targets, which pays before reading it.
			return g.preX, g.hasPreX
		}
		if source == NoCard {
			return 0, false
		}
		return g.Card(source).castX, true
	case "ResolvedThisTurn":
		// AbilityUtils.java:1843: how often the ability being resolved has
		// resolved this turn, itself included (SpellAbility.getResolvedThisTurn
		// is the host's count for that ability). Unresolved outside a resolution.
		if g.resolving == nil || g.resolving.Params == nil || source == NoCard {
			return 0, false
		}
		turn, _ := g.Card(source).trigResolved.of(g.resolving.Params)
		return turn, true
	case "CardManaCost":
		// Count$CardManaCost: the source card's own mana value (Cascade's X).
		if source == NoCard {
			return 0, false
		}
		return g.Card(source).CMC(), true
	case "CardPower", "CardToughness":
		// AbilityUtils.java: the source's own net power/toughness.
		if source == NoCard {
			return 0, false
		}
		if count.Head == "CardPower" {
			return g.Card(source).Power()
		}
		return g.Card(source).Toughness()
	case "Kicked":
		// Count$Kicked.<n if kicked>.<n if not> (AbilityUtils.java:1691).
		if source == NoCard || len(count.Parameters) != 2 {
			return 0, false
		}
		yes, errYes := strconv.Atoi(count.Parameters[0])
		no, errNo := strconv.Atoi(count.Parameters[1])
		if errYes != nil || errNo != nil {
			return 0, false
		}
		if g.Card(source).kickerMagnitude() > 0 {
			return yes, true
		}
		return no, true
	case "TimesKicked":
		if source == NoCard {
			return 0, false
		}
		return g.Card(source).kickerMagnitude(), true
	case "OptionalGenericCostPaid", "Teamwork":
		// Count$OptionalGenericCostPaid.<paid>.<not> and Count$Teamwork.<paid>.<not>
		// (AbilityUtils.java:1700-1706): the ability's own cast record, which
		// only a spell has (spellOptional).
		return optionalBranch(g.spellOptional(source), count)
	}
	if typ, ok := strings.CutPrefix(count.Head, "Presence_"); ok {
		return g.presenceCount(sourceController, source, typ, count)
	}
	switch count.Head {
	case "ChosenNumber":
		if source == NoCard {
			return 0, false
		}
		n, _ := g.Card(source).Memory.ChosenNumber()
		return n, true
	case "ChromaSource":
		if source == NoCard {
			return 0, false
		}
		return chromaCount([]CardID{source}, g, count.Parameters)
	}
	if sourceController == NoPlayer {
		return 0, false
	}
	if which, ok := strings.CutPrefix(count.Head, "ManaPool:"); ok {
		return manaPoolCount(g.Player(sourceController), which)
	}
	switch count.Head {
	case "TotalDamageDoneByThisTurn":
		// Card.getTotalDamageDoneBy: what the host dealt this turn.
		if source == NoCard {
			return 0, false
		}
		return g.Card(source).damageDealtThisTurn, true
	case "LifeYouGainedThisTurn":
		return g.Player(sourceController).LifeGainedThisTurn, true
	case "CreaturesAttackedThisTurn":
		return g.creaturesAttackedCount(sourceController, source, count.Argument)
	case "CountersAddedThisTurn":
		return g.countersAddedCount(sourceController, source, count.Argument)
	case "UnlockedDoors":
		return len(g.unlockedDoorNames(sourceController)), true
	case "DistinctUnlockedDoors":
		seen := map[string]bool{}
		for _, name := range g.unlockedDoorNames(sourceController) {
			seen[name] = true
		}
		return len(seen), true
	case "Blessing":
		// Count$Blessing.<n with>.<n without> (AbilityUtils.java:2269):
		// the controller's city's blessing picks the branch. Both branches
		// are plain numbers here; any other shape is unresolved (GO-7).
		if len(count.Parameters) != 2 {
			return 0, false
		}
		branch := count.Parameters[1]
		if g.Player(sourceController).Blessing {
			branch = count.Parameters[0]
		}
		n, err := strconv.Atoi(branch)
		return n, err == nil
	case "BloodthirstAmount":
		return g.bloodthirstAmount(sourceController), true
	case "YourLifeTotal":
		return g.Player(sourceController).Life, true
	case "OppGreatestLifeTotal":
		// Player.getOpponentsGreatestLifeTotal: Aggregates.max seeds with
		// Integer.MIN_VALUE, which is what no opponent at all reads as.
		n := math.MinInt32
		for _, pid := range g.Players() {
			if pid != sourceController && !g.Player(pid).Lost {
				n = max(n, g.Player(pid).Life)
			}
		}
		return n, true
	case "Party":
		return partyCount(g, sourceController), true
	case "YourTurns":
		// Count$YourTurns (AbilityUtils.java:2438): Player.getTurn() of the
		// host's controller, the turns it has taken, this one included.
		return g.Player(sourceController).Turn, true
	case "YouDrewThisTurn":
		return g.Player(sourceController).CardsDrawnThisTurn, true
	case "NumInAllHands":
		n := 0
		for _, pid := range g.Players() {
			n += len(g.Zone(Hand, pid).Cards())
		}
		return n, true
	case "Domain":
		return domainCount(g, sourceController), true
	case "DomainActivePlayer":
		return domainCount(g, g.ActivePlayer()), true
	case "Devotion", "DevotionDual":
		return devotionCount(g, sourceController, count)
	case "Chroma":
		return chromaCount(battlefieldControlledBy(g, sourceController), g, count.Parameters)
	case "ChromaInGrave":
		return chromaCount(g.Zone(Graveyard, sourceController).Cards(), g, count.Parameters)
	}
	return 0, false
}

// battlefieldControlledBy is Player.getCardsIn(Battlefield): what pid
// controls. g.Zone(Battlefield, pid) is keyed by controller (ADR-0037,
// correctControllerZone), so a permanent GainControl$/ExchangeControl$ moved
// is already in its new controller's list. Every xCount head that reads
// "permanents pid controls" -- Domain, Devotion, Chroma among them -- reads
// it through here.
func battlefieldControlledBy(g *Game, pid PlayerID) []CardID {
	return append([]CardID(nil), g.Zone(Battlefield, pid).Cards()...)
}

// domainCount is xCount's own Count$Domain: how many of the five basic land
// types appear among pid's lands (Player.getLandsInPlay, filtered by
// CardLists.getType's own hasStringType per type). Reads each land's current
// type line, Layer 4 folded in (Card.Type).
func domainCount(g *Game, pid PlayerID) int {
	// MagicColor.Constant.BASIC_LANDS, in its own order.
	basicLandTypes := [...]string{"Plains", "Island", "Swamp", "Mountain", "Forest"}
	var seen [len(basicLandTypes)]bool
	for _, id := range battlefieldControlledBy(g, pid) {
		t := g.Card(id).Type()
		if !t.Has(cardtype.Land) {
			continue
		}
		for i, basic := range basicLandTypes {
			if t.HasStringType(basic) {
				seen[i] = true
			}
		}
	}
	n := 0
	for _, s := range seen {
		if s {
			n++
		}
	}
	return n
}

// devotionCount is xCount's own Count$Devotion/DevotionDual: every mana
// symbol of the named color(s) in the mana costs of permanents pid controls
// (ManaCostShard.isColor -- a symbol counts once if any of its colors is in
// the mask, so a hybrid symbol counts once even toward a dual devotion),
// plus Player.getDevotionMod (devotionMod). A color written as "Chosen..."
// (the host's own chosen color) or one ManaAtom.fromName would not read as a
// color at all reports false.
func devotionCount(g *Game, pid PlayerID, count expr.Count) (int, bool) {
	want := 2
	if count.Head == "Devotion" {
		want = 1
	}
	if len(count.Parameters) < want {
		return 0, false
	}
	var mask mana.Colors
	for _, name := range count.Parameters[:want] {
		c, ok := manaAtomColor(name)
		if !ok {
			return 0, false
		}
		mask |= c
	}
	mod, ok := devotionMod(g, pid)
	if !ok {
		return 0, false
	}
	return shardsOfColor(g, battlefieldControlledBy(g, pid), mask) + mod, true
}

// chromaCount is xCount's own Count$Chroma (CardLists.getTotalChroma): the
// mana symbols of the named color -- all five when none is named, ManaAtom's
// own ALL_MANA_COLORS -- in the mana costs of cards.
func chromaCount(cards []CardID, g *Game, params []string) (int, bool) {
	mask := mana.AllColors
	if len(params) > 0 {
		c, ok := manaAtomColor(params[0])
		if !ok {
			return 0, false
		}
		mask = c
	}
	return shardsOfColor(g, cards, mask), true
}

// shardsOfColor counts the symbols among cards' own printed mana costs
// whose colors intersect mask -- Card.getManaCost's own shard iteration,
// generic mana excluded (it is no shard).
func shardsOfColor(g *Game, cards []CardID, mask mana.Colors) int {
	n := 0
	for _, id := range cards {
		c := g.Card(id)
		if c.Def == nil {
			continue
		}
		for _, s := range c.Def.Faces[0].ManaCost.Shards() {
			if s.Colors().HasAny(mask) {
				n++
			}
		}
	}
	return n
}

// manaAtomColor is ManaAtom.fromName for the five colors: one or two mana
// letters, or a full color name in any case. Colorless (ManaAtom.COLORLESS,
// a mana type with no mana.Colors bit) and anything unrecognized (fromName's
// own "generic" 0) report false.
func manaAtomColor(name string) (mana.Colors, bool) {
	if len(name) == 1 || len(name) == 2 {
		var out mana.Colors
		for i := 0; i < len(name); i++ {
			c, ok := mana.ColorFromLetter(strings.ToUpper(name[i : i+1])[0])
			if !ok {
				return 0, false
			}
			out |= c
		}
		return out, true
	}
	switch strings.ToLower(name) {
	case "white":
		return mana.White, true
	case "blue":
		return mana.Blue, true
	case "black":
		return mana.Black, true
	case "red":
		return mana.Red, true
	case "green":
		return mana.Green, true
	}
	return 0, false
}

// devotionMod is StaticAbilityDevotion.getDevotionMod: the sum of Value$
// (default 1) over every active Mode$ Devotion static ability whose own
// ValidPlayer$ matches pid -- Altar of the Pantheon's "your devotion to each
// color ... is increased by one," the one real corpus line. A line carrying
// any param past Mode$/ValidPlayer$/Value$/Description$ (checkConditions'
// own Condition$ family), a non-integer Value$, or a ValidPlayer$ that
// matchesPlayerSpec cannot evaluate makes the whole count unresolvable
// rather than silently dropping its contribution (GO-7).
func devotionMod(g *Game, pid PlayerID) (int, bool) {
	mod := 0
	for _, owner := range g.Players() {
		for _, host := range g.traitHosts(owner) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "Devotion") {
						continue
					}
					for _, p := range s.Params {
						switch strings.ToLower(p.Key) {
						case "mode", "validplayer", "value", "description":
						default:
							return 0, false
						}
					}
					if spec, ok := s.Param("ValidPlayer"); ok {
						matched, ok := matchesPlayerSpec(g, pid, h.Controller(), host, spec)
						if !ok {
							return 0, false
						}
						if !matched {
							continue
						}
					}
					v := 1
					if raw, ok := s.Param("Value"); ok {
						n, err := strconv.Atoi(raw)
						if err != nil {
							return 0, false
						}
						v = n
					}
					mod += v
				}
			}
		}
	}
	return mod, true
}

// javaBucket is the bucket Java's HashMap<String, _> with 16 buckets (Guava's
// MultimapBuilder.hashKeys(), 8 expected keys) puts key in: String.hashCode
// spread by h ^ (h >>> 16). Count$Party's greedy assignment visits the
// multi-typed groups in this order when their sizes tie (AbilityUtils.java:2583,
// 2628), so it is reproduced rather than replaced by a fixed one (PORT-7).
func javaBucket(key string) uint32 {
	var h int32
	for _, r := range key {
		h = 31*h + r
	}
	u := uint32(h)
	return (u ^ u>>16) & 15
}

// partyCount is xCount's Count$Party (AbilityUtils.java:2580-2640): the size
// of the largest party among pid's creatures, at most four, one creature per
// Cleric, Rogue, Warrior and Wizard. A one-type creature fills its type, a
// four-type creature (a changeling) is a wildcard, and creatures of two or
// three party types are assigned greedily, smallest group first, in the
// order Java's hashed multimap yields the groups.
func partyCount(g *Game, pid PlayerID) int {
	chosen := map[string]bool{}
	wildcard := 0
	groups := map[string][]CardID{}
	for _, id := range battlefieldControlledBy(g, pid) {
		t := g.Card(id).Type()
		if !t.Has(cardtype.Creature) {
			continue
		}
		var mine []string
		for _, name := range partyTypes {
			if t.HasSubtype(name) {
				mine = append(mine, name)
			}
		}
		switch len(mine) {
		case 4:
			wildcard++
		case 1:
			chosen[mine[0]] = true
		case 2, 3:
			for _, name := range mine {
				groups[name] = append(groups[name], id)
			}
		}
		if len(chosen)+wildcard >= 4 {
			break
		}
	}
	if len(chosen)+wildcard < 4 {
		var keys []string
		for _, name := range partyTypes {
			if _, ok := groups[name]; ok && !chosen[name] {
				keys = append(keys, name)
			}
		}
		// Hash order first, then the stable sort by group size.
		sort.SliceStable(keys, func(i, j int) bool { return javaBucket(keys[i]) < javaBucket(keys[j]) })
		sort.SliceStable(keys, func(i, j int) bool { return len(groups[keys[i]]) < len(groups[keys[j]]) })
		var taken []CardID
		for _, key := range keys {
			var rest []CardID
			for _, id := range groups[key] {
				if !slices.Contains(taken, id) {
					rest = append(rest, id)
				}
			}
			if len(rest) > 0 {
				chosen[key] = true
				taken = append(taken, rest[0])
			}
		}
	}
	return min(len(chosen)+wildcard, 4)
}

// exiledWithValue is calculateAmount's `ExiledWith$<property>` head
// (AbilityUtils.java:502-503): handlePaid over source's own exiled cards
// (Card.getExiledCards, the cards markExiledWith listed on this host object),
// tokens excluded (Card.retainPaidList). An empty list is 0 whatever the
// property. CardPower/CardToughness sum each card's net value (handlePaid's
// generic tail); the exiled cards are not being rebuilt by Layer 7 the way
// another permanent is, so the value is safe to read here.
func exiledWithValue(g *Game, source CardID, property string) (int, bool) {
	if source == NoCard {
		return 0, false
	}
	stamp, _ := g.hostObjectStamp(source)
	var cards []CardID
	for i := 1; i < len(g.cards); i++ {
		c := &g.cards[i]
		ew := c.exiledWith
		if ew.host == source && ew.listed && ew.stamp == stamp && !c.IsToken {
			cards = append(cards, c.ID)
		}
	}
	return measureListed(g, cards, property)
}

// rememberedValue is calculateAmount's `Remembered$<property>` head
// (AbilityUtils.java:512-536): handlePaid over the cards source remembers.
// A remembered player is skipped. The LKI forms read last-known copies and are
// not resolved.
func rememberedValue(g *Game, source CardID, property string) (int, bool) {
	if source == NoCard || strings.Contains(property, "LKI") {
		return 0, false
	}
	var cards []CardID
	for _, e := range g.Card(source).Memory.Remembered() {
		if id, ok := e.AsCard(); ok {
			cards = append(cards, id)
		}
	}
	return measureListed(g, cards, property)
}

// imprintedValue is calculateAmount's `Imprinted$<property>` head: handlePaid
// over the cards source imprinted. `Valid <spec>` (handlePaid's own Valid
// branch) counts those matching the spec; any other property is measured as
// for an exiled list.
func imprintedValue(g *Game, controller PlayerID, source CardID, property string) (int, bool) {
	if source == NoCard {
		return 0, false
	}
	imprinted := g.Card(source).Memory.Imprinted()
	spec, isValid := strings.CutPrefix(property, "Valid ")
	if !isValid {
		return measureListed(g, imprinted, property)
	}
	parsed := valid.Parse(spec)
	n := 0
	for _, id := range imprinted {
		if Matches(g, g.Card(id), parsed, controller, source) {
			n++
		}
	}
	return n, true
}

// manaPoolCount is Count$ManaPool:<All|color> (AbilityUtils.java:2544-2553):
// the controller's floating mana, in total or of one color named in lower
// case ("green"). A name that is no color is unresolved.
func manaPoolCount(p *Player, which string) (int, bool) {
	if which == "All" {
		return p.ManaPool.Total(), true
	}
	for i, name := range [...]string{"white", "blue", "black", "red", "green"} {
		if which == name {
			return p.ManaPool.Breakdown()[i], true
		}
	}
	return 0, false
}

// startingIntensity is the magnitude of c's Starting intensity keyword lines,
// the part of Card.getIntensity(true) beyond the intensity counted so far.
func startingIntensity(c *Card) int {
	total := 0
	for _, line := range c.KeywordLines() {
		kw := keyword.Parse(line)
		if kw.Name != "Starting intensity" {
			continue
		}
		if args := kw.Args(); len(args) > 0 {
			if n, err := strconv.Atoi(args[0]); err == nil {
				total += n
			}
		}
	}
	return total
}

// dungeonsCompletedValue is calculateAmount's `DungeonsCompleted$<property>`
// head (AbilityUtils.java:500-501): handlePaid over the dungeons the source
// controller has completed. `Valid <spec>` counts those matching the spec
// (read from the controller's point of view); an empty list is 0 whatever
// the property.
func dungeonsCompletedValue(g *Game, controller PlayerID, source CardID, property string) (int, bool) {
	if controller == NoPlayer {
		return 0, false
	}
	done := g.Player(controller).completedDungeons
	spec, isValid := strings.CutPrefix(property, "Valid ")
	if !isValid {
		return measureListed(g, done, property)
	}
	parsed := valid.Parse(spec)
	n := 0
	for _, id := range done {
		if Matches(g, g.Card(id), parsed, controller, source) {
			n++
		}
	}
	return n, true
}

// rememberedPlayersLife is PlayerCountRemembered$LifeTotal: the sum of the
// life totals of the players source remembers (playerXCount's addPlayer takes
// each remembered Player directly, then playerXProperty sums LifeTotal). Any
// other property is unresolved.
func rememberedPlayersLife(g *Game, source CardID, property string) (int, bool) {
	if source == NoCard || property != "LifeTotal" {
		return 0, false
	}
	total := 0
	for _, e := range g.Card(source).Memory.Remembered() {
		if pid, ok := e.AsPlayer(); ok {
			total += g.Player(pid).Life
		}
	}
	return total, true
}

// measureListed is handlePaid over an already collected list: 0 for an empty
// one, else paidMeasure's property, with CardPower and CardToughness (the
// generic tail's xCount per card) read as the sum of the cards' net values.
func measureListed(g *Game, cards []CardID, property string) (int, bool) {
	if len(cards) == 0 {
		return 0, true
	}
	if property == "CardPower" || property == "CardToughness" {
		total := 0
		for _, id := range cards {
			v, ok := g.Card(id).Power()
			if property == "CardToughness" {
				v, ok = g.Card(id).Toughness()
			}
			if !ok {
				return 0, false
			}
			total += v
		}
		return total, true
	}
	measure, ok := paidMeasure(property)
	if !ok {
		return 0, false
	}
	return measure(g, cards), true
}

// playerCountValue is calculateAmount's own PlayerCount<hType>$ dispatch
// into playerXCount, for the one shape a real CDA writes (Adamaro, First to
// Desire's `PlayerCountOpponents$HighestCardsInHand`) and its immediate
// siblings: hType Opponents or Players (the empty hType is Players too),
// body Highest<Property> or Lowest<Property>, Property CardsInHand or
// LifeTotal (playerXProperty's own `value.contains` checks, reached by these
// two names before any other branch). Highest starts from 0 and Lowest from
// 99999, Java's own seeds; no players at all is 0. Players who have left the
// game are not counted (Game.getPlayers holds only players still in it).
func playerCountValue(g *Game, sourceController PlayerID, source CardID, hType, body string) (int, bool) {
	if hType == "Remembered" {
		return rememberedPlayersLife(g, source, body)
	}
	var players []PlayerID
	for _, pid := range g.Players() {
		if g.Player(pid).Lost {
			continue
		}
		switch hType {
		case "", "Players":
		case "Opponents":
			if pid == sourceController {
				continue
			}
		default:
			return 0, false
		}
		players = append(players, pid)
	}
	// PlayerCount<hType>$Amount: how many players there are
	// (AbilityUtils.java:3431-3434), Welcome to . . .'s "for each opponent"
	// TargetMax$.
	if body == "Amount" {
		return len(players), true
	}
	// PlayerCount<hType>$HasProperty<property>: how many of those players have
	// the player property (playerXCount's own "HasProperty" branch). A property
	// matchesPlayerProperty does not know makes the whole amount unresolved.
	if prop, ok := strings.CutPrefix(body, "HasProperty"); ok {
		n := 0
		for _, pid := range players {
			matched, known := matchesPlayerProperty(g, pid, sourceController, NoCard, prop)
			if !known {
				return 0, false
			}
			if matched {
				n++
			}
		}
		return n, true
	}
	var highest bool
	var property string
	switch {
	case strings.HasPrefix(body, "Highest"):
		highest, property = true, strings.TrimPrefix(body, "Highest")
	case strings.HasPrefix(body, "Lowest"):
		property = strings.TrimPrefix(body, "Lowest")
	default:
		return 0, false
	}
	var value func(PlayerID) int
	switch property {
	case "CardsInHand":
		value = func(pid PlayerID) int { return len(g.Zone(Hand, pid).Cards()) }
	case "LifeTotal":
		value = func(pid PlayerID) int { return g.Player(pid).Life }
	case "CardsInGraveyard":
		value = func(pid PlayerID) int { return len(g.Zone(Graveyard, pid).Cards()) }
	case "Counters.Poison":
		value = func(pid PlayerID) int { return g.Player(pid).Counters.Count(Poison) }
	default:
		return 0, false
	}
	if len(players) == 0 {
		return 0, true
	}
	n := 99999
	if highest {
		n = 0
	}
	for _, pid := range players {
		v := value(pid)
		if highest && v > n || !highest && v < n {
			n = v
		}
	}
	return n, true
}
