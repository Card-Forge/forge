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
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// contextHeads are the ability-context amount heads that name a list of cards
// (AbilityUtils.java:672-719), read only while that ability resolves.
var contextHeads = map[string]bool{
	"Targeted": true, "ParentTargeted": true, "TriggeredCard": true, "TriggeredAttacker": true, "TriggeredBlocker": true,
	"TriggeredSource": true, "TriggerRemembered": true, "Sacrificed": true, "Exiled": true, "Discarded": true,
	"Collected": true, "Revealed": true, "TriggerObjectsAttackers": true,
}

// listHeadCards is the card list an amount head names, in calculateAmount's
// own order (AbilityUtils.java:503-719): the host's lists first --
// ExiledWith, DungeonsCompleted, ChosenCard, Remembered/RememberedLKI,
// Imprinted/ImprintedLKI, Enchanted/Equipped -- then the resolving ability's
// (contextHeads). handled is false for a head that names no list; ok is false
// for a list this port does not keep (Convoked, Emerged, Crewed) or cannot
// read here.
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
	case strings.HasPrefix(head, "Convoked"), strings.HasPrefix(head, "Emerged"), strings.HasPrefix(head, "Crewed"):
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
		// while source is that spell (spellOptional).
		if source == NoCard {
			return nil, false
		}
		ids = optionalList(g.spellOptional(source), head)
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
		head == "TriggerObjectsAttackers"
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
	case "Targeted":
		for _, e := range a.Targets {
			if id, ok := e.AsCard(); ok {
				ids = append(ids, id)
			}
		}
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
// the host's cast record (Card.getCastSA) -- the optional-cost branches and the
// Collected/Revealed paid lists, the heads that record holds. cast is false
// for a host that was not cast; ok is false for any other head.
func (g *Game) castSAValue(source CardID, amt expr.Amount) (n int, ok, cast bool) {
	switch {
	case amt.Count != nil && (amt.Count.Head == "OptionalGenericCostPaid" || amt.Count.Head == "Teamwork"):
	case amt.Head == "Collected" || amt.Head == "Revealed":
	default:
		return 0, false, false
	}
	rec, cast := g.castSARecord(source)
	if !cast {
		return 0, true, false
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
