// Ability-context amounts: `Targeted$CardPower`, `TriggeredCard$CardToughness`,
// `Remembered$Valid Creature`, `Sacrificed$CardManaCost` and the rest of
// AbilityUtils.calculateAmount's object-list branches (AbilityUtils.java:
// 640-715). The head names a list of cards the resolving ability carries --
// its targets, what it was triggered by, what its host remembers or imprinted,
// what its cost used up -- and the body measures them (handlePaid).
//
// What the host remembers or imprinted is read off the card the amount is
// evaluated for (AbilityUtils.java:512-555 uses its `card` argument), so a
// trigger or layer check running mid-resolution reads its own host, not the
// resolving ability's. The lists an ability carries (targets, trigger, paid
// cost) belong to Game.resolving, set by Registry.resolve -- a sub-ability
// reads its root's lists because the chain copies them -- and are read only
// when the evaluated card is that ability's own source; any other evaluation
// stays unresolved (GO-7).

package engine

import (
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// contextHeads are the ability-context amount heads that name a list of cards
// (AbilityUtils.java:672-719), read only while that ability resolves.
var contextHeads = map[string]bool{
	"Targeted": true, "ParentTargeted": true, "TriggeredCard": true, "TriggeredAttacker": true, "TriggeredBlocker": true,
	"TriggeredSource": true, "TriggerRemembered": true, "Sacrificed": true, "Exiled": true, "Discarded": true,
	"Collected": true, "Revealed": true, "TriggerObjectsAttackers": true, "TriggeredExploited": true,
	"TriggeredDevoured": true, "CardTriggeredTarget": true,
}

// listHeadCards is the card list an amount head names, in calculateAmount's
// own order (AbilityUtils.java:503-719): the host's lists first --
// ExiledWith, DungeonsCompleted, ChosenCard, Remembered/RememberedLKI,
// Imprinted/ImprintedLKI, Enchanted/Equipped -- then the resolving ability's
// (contextHeads). handled is false for a head that names no list; ok is false
// for a list this port does not keep (Convoked, Crewed) or cannot read here.
//
// A Remembered or Imprinted card is read as it is now (game.getCardState);
// the LKI spellings read the remembered object itself, which for a card that
// has since left the battlefield is its last-known information.
func (g *Game) listHeadCards(sourceController PlayerID, source CardID, head string) (cards []*Card, ok, handled bool) {
	switch {
	case strings.HasPrefix(head, "ExiledWith"):
		if source == NoCard {
			return nil, false, true
		}
		return g.cardsOf(g.exiledWithCards(source), false), true, true
	case head == "DungeonsCompleted":
		if sourceController == NoPlayer {
			return nil, false, true
		}
		return g.cardsOf(g.Player(sourceController).completedDungeons, false), true, true
	case strings.HasPrefix(head, "Emerged"):
		// AbilityUtils.java:509-510: Card.getEmerged, the permanent its cast
		// sacrificed for Emerge, as it last was.
		if source == NoCard {
			return nil, false, true
		}
		return g.emergedCards(g.Card(source)), true, true
	case strings.HasPrefix(head, "Convoked"), strings.HasPrefix(head, "Crewed"):
		return nil, false, true
	case strings.HasPrefix(head, "ChosenCard"):
		if source == NoCard {
			return nil, false, true
		}
		return g.cardsOf(g.Card(source).Memory.Chosen(), false), true, true
	case strings.HasPrefix(head, "Remembered"):
		if source == NoCard {
			return nil, false, true
		}
		var ids []CardID
		for _, e := range g.Card(source).Memory.Remembered() {
			if id, isCard := e.AsCard(); isCard {
				ids = append(ids, id)
			}
		}
		return g.cardsOf(ids, strings.HasSuffix(head, "LKI")), true, true
	case strings.HasPrefix(head, "Imprinted"):
		if source == NoCard {
			return nil, false, true
		}
		return g.cardsOf(g.Card(source).Memory.Imprinted(), strings.HasSuffix(head, "LKI")), true, true
	case head == "Enchanted", head == "Equipped":
		// Java tests isEnchanting (isAttachedToEntity) for both spellings.
		if source == NoCard {
			return nil, false, true
		}
		if id, attached := g.Card(source).AttachedTo(); attached {
			return []*Card{g.Card(id)}, true, true
		}
		return nil, true, true
	case contextHeads[head]:
		cards, ok := g.contextCards(source, head)
		return cards, ok, true
	}
	return nil, false, false
}

// cardsOf is ids as cards; lki reads a card that has left the battlefield from
// its last-known information when the engine kept one.
func (g *Game) cardsOf(ids []CardID, lki bool) []*Card {
	cards := make([]*Card, 0, len(ids))
	for _, id := range ids {
		c := g.Card(id)
		if lki && c.Zone != Battlefield {
			if snap := g.LKI(id); snap != nil {
				c = snap
			}
		}
		cards = append(cards, c)
	}
	return cards
}

// contextCards is the cards head names for the ability resolving now. A card
// that left the battlefield (a sacrificed creature) is read from its
// last-known information.
func (g *Game) contextCards(source CardID, head string) ([]*Card, bool) {
	var ids []CardID
	switch head {
	case "Collected", "Revealed":
		// The cast spell's paid lists (CostCollectEvidence, CostReveal), read
		// while source is that spell (spellOptional). Revealed is Java's LKI
		// list: each card as it was revealed or chosen (CostPartWithList.java:
		// 136-138), so Dragon's Fire reads the Dragon's power as it was paid.
		if source == NoCard {
			return nil, false
		}
		rec := g.spellOptional(source)
		if head == "Revealed" && len(rec.revealedLKI) == len(rec.revealed) && len(rec.revealed) > 0 {
			return append([]*Card(nil), rec.revealedLKI...), true
		}
		ids = optionalList(rec, head)
	case "Targeted":
		// SpellAbility.findTargetedCards (SpellAbility.java:1961-1986).
		a := g.resolving
		if a == nil || source == NoCard || a.Source != source {
			return nil, false
		}
		return g.targetedCards(a), true
	default:
		var ok bool
		if ids, ok = g.abilityListIDs(source, head); !ok {
			return nil, false
		}
	}
	// A recorded attacker that has since died is read as it last was: Java's
	// list holds the battlefield Card object, which the move to the graveyard
	// replaced with a copy (GameAction.java:207).
	lki := head == "Sacrificed" || head == "Exiled" || head == "TriggeredCard" || head == "TriggerRemembered" ||
		head == "TriggerObjectsAttackers" || head == "TriggeredExploited" || head == "TriggeredDevoured" ||
		head == "CardTriggeredTarget"
	return g.cardsOf(ids, lki), true
}

// abilityListIDs is the cards of the list head names that the resolving
// ability carries, when it is source's own ability. A paid list is unresolved
// unless the cost that paid for the ability recorded it (paidLists.recorded):
// an empty list would read as 0, which Java's recorded-empty list is but an
// unrecorded one is not.
func (g *Game) abilityListIDs(source CardID, head string) ([]CardID, bool) {
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		return nil, false
	}
	var ids []CardID
	switch head {
	case "ParentTargeted":
		// AbilityUtils.java:688-693: the cards the nearest ancestor that
		// targets a card targets (getParentTargetingCard). A targeting
		// ancestor left with none is Java's null list, which reads 0; no
		// targeting ancestor at all is a reference this port does not carry
		// (definedCards' ParentTarget), unresolved.
		if len(a.parentTargets) == 0 && !a.ancestorTargeting {
			return nil, false
		}
		for _, e := range a.parentTargets {
			if id, ok := e.AsCard(); ok {
				ids = append(ids, id)
			}
		}
	case "TriggeredCard":
		ids = nonNone(a.triggered.card)
	case "TriggeredAttacker":
		ids = nonNone(a.triggered.attacker)
	case "TriggeredBlocker":
		ids = nonNone(a.triggered.blocker)
	case "TriggeredExploited":
		// AbilityKey.Exploited of Mode$ Exploited (TriggerExploited.java:71);
		// unrecorded by any other mode, which is unresolved, not 0.
		if a.triggered.exploited == NoCard {
			return nil, false
		}
		ids = []CardID{a.triggered.exploited}
	case "TriggeredDevoured":
		// AbilityKey.Devoured of Mode$ Devoured (TriggerDevoured.java:67).
		if a.triggered.devoured == NoCard {
			return nil, false
		}
		ids = []CardID{a.triggered.devoured}
	case "TriggerObjectsAttackers":
		// AbilityUtils.java:697-701: the trigger's AbilityKey.Attackers list.
		// Java reads a key the mode never set as an empty list; here that is
		// unresolved, a recorded empty list 0 (triggeredObjects.attackersRecorded).
		if !a.triggered.attackersRecorded {
			return nil, false
		}
		ids = a.triggered.attackers
	case "TriggeredSource":
		// AbilityKey.Source as a card; a player source is no card, and an
		// unrecorded source is not Java's empty list (GO-7).
		if a.triggered.source == NoEntity {
			return nil, false
		}
		if id, isCard := a.triggered.source.AsCard(); isCard {
			ids = []CardID{id}
		}
	case "CardTriggeredTarget":
		// AbilityUtils.java:702-707: AbilityKey.Target read as a card
		// ("CardTriggered" bypasses keys that could be a player). An
		// unrecorded or player target is no card: unresolved (GO-7).
		id, isCard := a.triggered.target.AsCard()
		if !isCard {
			return nil, false
		}
		ids = []CardID{id}
	case "TriggerRemembered":
		// SpellAbility.getTriggerRemembered, its cards only.
		for _, e := range a.TriggerRemembered {
			if id, isCard := e.AsCard(); isCard {
				ids = append(ids, id)
			}
		}
	case "Sacrificed", "Exiled", "Discarded":
		if !a.paid.recorded {
			return nil, false
		}
		switch head {
		case "Sacrificed":
			ids = a.paid.sacrificed
		case "Exiled":
			ids = a.paid.exiled
		default:
			ids = a.paid.discarded
		}
	default:
		return nil, false
	}
	return ids, true
}

// targetedCards is SpellAbility.findTargetedCards for a (SpellAbility.java:
// 1961-1986): its card targets when it targets any card, else the host of each
// spell or ability on the stack it targets, in target order. Java targets a
// spell as its SpellAbility, never as a card; here targeting offers a spell as
// its card, so a card target recorded as a spell (stackTargetRef.spell) is
// one of the second kind, read as Java's host object (spellTargetView). A
// sub-ability carries its root's targets, Java's parent-targeting fallback.
func (g *Game) targetedCards(a *Ability) []*Card {
	var cards, hosts []*Card
	for _, e := range a.Targets {
		if ref, ok := g.stackTargetOf(e, a.stackTargets); ok {
			hosts = append(hosts, g.Card(ref.host))
			continue
		}
		id, ok := e.AsCard()
		if !ok {
			continue
		}
		if view, isSpell := g.spellTargetView(id, a.stackTargets); isSpell {
			hosts = append(hosts, view)
			continue
		}
		cards = append(cards, g.Card(id))
	}
	if len(cards) > 0 {
		return cards
	}
	return hosts
}

// spellTargetView is the targeted spell card id as Java's target SpellAbility
// reads its host (stackTargetRef's doc comment): the live card while it is
// still that spell on the stack, else a copy carrying the zone, controller and
// cast record it had there. false when refs records no spell on id.
func (g *Game) spellTargetView(id CardID, refs []stackTargetRef) (*Card, bool) {
	for i := range refs {
		r := &refs[i]
		if !r.spell || r.host != id {
			continue
		}
		c := g.Card(id)
		if c.Zone == Stack && c.zoneStamp == r.stamp {
			return c, true
		}
		view := *c
		view.Zone, view.controller = Stack, r.controller
		view.wasCast, view.castX, view.castManaSpent = r.cast.wasCast, r.cast.x, r.cast.manaSpent
		view.castSnow, view.castPaidSources = r.cast.snow, r.cast.sources
		return &view, true
	}
	return nil, false
}

// spellTargetedValue is `SpellTargeted$<measure>` (AbilityUtils.java:637-640):
// the first spell or ability on the stack the resolving ability's chain
// targets (getDefinedSpellAbilities' Targeted branch over getAllTargetChoices,
// :1264-1278), measured on its host card as xCount(sat.getHostCard(), ...,
// sat) does; 0 when it targets none. Only the per-card measures are read
// (liveCardMeasure); any other measure is unresolved (GO-7).
func (g *Game) spellTargetedValue(amounts map[string]expr.Amount, sourceController PlayerID, source CardID, amt expr.Amount, depth int) (int, bool) {
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		return 0, false
	}
	measure, ok := liveCardMeasure(amt.Body)
	if !ok {
		return 0, false
	}
	if len(a.stackTargets) == 0 {
		return 0, true
	}
	ref := a.stackTargets[0]
	host := g.Card(ref.host)
	if ref.spell {
		host, _ = g.spellTargetView(ref.host, a.stackTargets[:1])
	}
	n, ok := measure(host)
	if !ok {
		return 0, false
	}
	if amt.Op != nil {
		return applyOperator(g, amounts, sourceController, source, n, amt.Op, depth)
	}
	return n, true
}

// targetedByTargetCards is the `TargetedByTarget$` list (AbilityUtils.java:
// 615-623): for each spell or ability on the stack the resolving ability's
// chain targets, the cards that one targets (getDefinedCards' Targeted over its
// own getAllTargetChoices, :146-151), as they are now.
func (g *Game) targetedByTargetCards(source CardID) ([]*Card, bool) {
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		return nil, false
	}
	var cards []*Card
	for _, ref := range a.stackTargets {
		for _, e := range ref.targets {
			// A card on the stack is a spell, which Java targets as its
			// SpellAbility: not one of getTargetCards.
			if id, ok := e.AsCard(); ok && g.Card(id).Zone != Stack {
				cards = append(cards, g.Card(id))
			}
		}
	}
	return cards, true
}

// rootPayingManaCount is how many units of the mana the resolving ability's
// root paid (SpellAbility.getRootAbility().getPayingMana()) came from a card
// keep accepts: a spell's cast record, an activation's paidLists.mana, each
// source read as it was when it made the mana (manaSourceCard). false outside
// source's own resolution and for a root that recorded neither.
func (g *Game) rootPayingManaCount(source CardID, keep func(*Card) bool) (int, bool) {
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		return 0, false
	}
	sources := a.paid.mana
	switch {
	case a.paid.manaRecorded:
	case a.spell || a.rootSpell:
		sources = nil
		if c := g.Card(source); c.wasCast {
			sources = c.castPaidSources
		}
	default:
		return 0, false
	}
	n := 0
	for _, src := range sources {
		if from := g.manaSourceCard(src); from != nil && keep(from) {
			n++
		}
	}
	return n, true
}

func nonNone(id CardID) []CardID {
	if id == NoCard {
		return nil
	}
	return []CardID{id}
}

// liveCardMeasure is the per-card xCount of a card read outside a layer pass
// (handlePaid, amountpaid.go): power and toughness too (perCardMeasure refuses
// them, for it serves the layer pass, where Layer 7 is being rebuilt), mana
// value, a counter count, the number of colors.
func liveCardMeasure(head string) (func(*Card) (int, bool), bool) {
	switch head {
	case "CardPower":
		return func(c *Card) (int, bool) { return c.Power() }, true
	case "CardToughness":
		return func(c *Card) (int, bool) { return c.Toughness() }, true
	case "CardSumPT":
		return func(c *Card) (int, bool) {
			p, okP := c.Power()
			t, okT := c.Toughness()
			return p + t, okP && okT
		}, true
	case "CardNumColors":
		return func(c *Card) (int, bool) { return c.Colors().Count(), true }, true
	case "ChromaSource":
		// AbilityUtils.java:2703-2720 with the card itself as the list and no
		// color named (ManaAtom.ALL_MANA_COLORS): its colored mana symbols.
		return func(c *Card) (int, bool) {
			n := 0
			if c.Def != nil {
				for _, s := range c.Def.Faces[0].ManaCost.Shards() {
					if s.Colors().HasAny(mana.AllColors) {
						n++
					}
				}
			}
			return n, true
		}, true
	}
	if m, ok := perCardMeasure(head); ok {
		return func(c *Card) (int, bool) { return m(c), true }, true
	}
	return nil, false
}

// triggerCount is `TriggerCount$<Key>`: the integer the trigger that put the
// resolving ability on the stack recorded under key (AbilityUtils.java:638).
// Unresolved outside that ability and for a key its mode did not record.
func (g *Game) triggerCount(source CardID, key string) (int, bool) {
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		return 0, false
	}
	return a.triggered.counts.count(key)
}

// optionalList is the paid list head names in rec: "Collected" or "Revealed".
func optionalList(rec castOptional, head string) []CardID {
	if head == "Collected" {
		return rec.collected
	}
	return rec.revealed
}

// optionalBranch is Count$OptionalGenericCostPaid.<paid>.<not> and
// Count$Teamwork.<paid>.<not> read against rec (AbilityUtils.java:1700-1706).
// A branch that is not a plain number (an SVar name) is unresolved.
func optionalBranch(rec castOptional, count expr.Count) (int, bool) {
	if len(count.Parameters) != 2 {
		return 0, false
	}
	kind := optGeneric
	if count.Head == "Teamwork" {
		kind = optTeamwork
	}
	branch := count.Parameters[1]
	if rec.has(kind) {
		branch = count.Parameters[0]
	}
	n, err := strconv.Atoi(branch)
	return n, err == nil
}

// castSAValue is an amount under a CastSA> context: the head measured against
// the host's cast record (Card.getCastSA) -- the optional-cost branches, the
// Adamant test over the mana the cast spent and the Collected/Revealed paid
// lists, the heads that record holds. cast is false
// for a host that was not cast; ok is false for any other head.
func (g *Game) castSAValue(source CardID, amt expr.Amount) (n int, ok, cast bool) {
	switch {
	case amt.Count != nil && (amt.Count.Head == "OptionalGenericCostPaid" || amt.Count.Head == "Teamwork"):
	case amt.Count != nil && strings.HasPrefix(amt.Count.Head, "Adamant"):
	case amt.Count != nil && amt.Count.Head == "xPaid" && amt.Count.Argument == "":
	case amt.Head == "Collected" || amt.Head == "Revealed":
	default:
		return 0, false, false
	}
	rec, cast := g.castSARecord(source)
	if !cast {
		return 0, true, false
	}
	if amt.Count != nil && amt.Count.Head == "xPaid" {
		// xCount's xPaid with the cast SpellAbility as ctb (AbilityUtils.java:
		// 1890-1891): Card.getXManaCostPaid, the X the cast announced -- a
		// Sac<X/…> part's X too (announceXFor), Plumb the Forbidden's count.
		return g.Card(source).castX, true, true
	}
	if amt.Count != nil && strings.HasPrefix(amt.Count.Head, "Adamant") {
		// The cast's own paying mana (castAdamant, amountrecords.go).
		n, ok = g.castAdamant(source, *amt.Count)
		return n, ok, true
	}
	if amt.Count != nil {
		n, ok = optionalBranch(rec, *amt.Count)
		return n, ok, true
	}
	// The operator is applied after, by the caller; for the Amount and Valid
	// measures this reads that is where handlePaid applies it too.
	n, ok = handlePaid(g, nil, g.Card(source).Controller(), source, g.cardsOf(optionalList(rec, amt.Head), false), amt.Body, nil, 0)
	return n, ok, true
}

// presenceCount is Count$Presence_<Type>.<found>.<not>
// (AbilityUtils.java:2180-2192): found when the card was cast and its cast
// revealed (or beheld) a card of the type, or its controller controls a
// permanent of the type. Java reads the battlefield as the measurement is
// taken (Count$LastStateBattlefield, :2186), not as the spell was cast,
// which the Oracle text ("as you cast this spell") asks; reproduced, a Forge
// defect (PORT-8, forge-java-defects.md).
func (g *Game) presenceCount(sourceController PlayerID, source CardID, typ string, count expr.Count) (int, bool) {
	if source == NoCard || len(count.Parameters) != 2 {
		return 0, false
	}
	yes, errYes := strconv.Atoi(count.Parameters[0])
	no, errNo := strconv.Atoi(count.Parameters[1])
	if errYes != nil || errNo != nil {
		return 0, false
	}
	c := g.Card(source)
	if !c.wasCast {
		return no, true
	}
	spec := valid.Parse(typ)
	for _, id := range c.castOpt.revealed {
		if Matches(g, g.Card(id), spec, sourceController, source) {
			return yes, true
		}
	}
	controlled := valid.Parse(typ + ".YouCtrl")
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if Matches(g, g.Card(id), controlled, sourceController, source) {
				return yes, true
			}
		}
	}
	return no, true
}
