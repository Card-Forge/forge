package engine

//enginelint:allow id zone sticker valid effecthelpers card game player ability defined condition control parts trigger event

import (
	"fmt"
	"math"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// putStickerEffect is PutStickerEffect.java: each target card the activator
// owns gets one sticker from the activator's sheets, paid for in tickets (CR
// 123.4), then Mode$ StickerPlaced fires. Without Choices$ the targets are
// ValidTgts$/Defined$ (default Self); with it the activator picks one
// permanent they own among the cards of ChoiceZone$ matching it.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/PutStickerEffect.java's
// resolve (:97-141) and chooseObjects (:53-76).
type putStickerEffect struct{}

func (putStickerEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if err := rejectUnreadCondition(a, "PutSticker"); err != nil {
		return err
	}
	// hasParam, not a value test: "Optional$ False" would still be optional
	// (PutStickerEffect.java:70, :99; no corpus line writes it).
	optional := hasParam(a, "Optional")
	var only *compile.StickerKind
	if raw, ok := a.Params.Param("Kind"); ok {
		// StickerKind.smartValueOf: an unknown name filters nothing
		// (PutStickerEffect.java:86-87).
		if k, ok := compile.StickerKindByName(raw); ok {
			only = &k
		}
	}
	maxTickets := math.MaxInt32
	if hasParam(a, "MaxTickets") {
		n, err := optionalAmount(g, a, "PutSticker", "MaxTickets", math.MaxInt32)
		if err != nil {
			return err
		}
		maxTickets = n
	}
	free := hasParam(a, "NoTicketCost")
	// StickersPlaced and StickerUniqueVowels (PutStickerEffect.java:100-101,
	// :131, :134-135): Java keeps them on the resolving ability, which its
	// sub-abilities read; this port reads a named SVar from the host, the
	// copy StoreSVar already writes (storesvareffect.go).
	if source.svars == nil {
		source.svars = make(map[string]int)
	}
	source.svars["stickersplaced"], source.svars["stickeruniquevowels"] = 0, 0
	placed := 0

	targets, err := g.stickerTargets(a, c, source, optional)
	if err != nil {
		return err
	}
	for _, target := range targets {
		owner := g.Card(target).Owner
		if owner != a.Controller {
			continue
		}
		budget := maxTickets
		if free {
			budget = math.MaxInt32
		} else {
			budget = min(budget, g.Player(owner).Counters.Count(Ticket))
		}
		options := g.availableStickers(owner, budget, only)
		if len(options) == 0 {
			continue
		}
		idx := c.ChooseSticker(g, owner, a.Source, target, options, optional)
		if idx < -1 || idx >= len(options) || (idx == -1 && !optional) {
			return fmt.Errorf("engine: PutSticker: controller chose %d of %d stickers (optional %v)", idx, len(options), optional)
		}
		if idx == -1 {
			continue
		}
		chosen := options[idx]
		// Tickets are taken after the choice, at resolution, not as a cost
		// (PutStickerEffect.java:124-126); only NoTicketCost skips them.
		if chosen.Tickets > 0 && !free {
			g.Player(owner).Counters.Add(Ticket, -chosen.Tickets)
		}
		namePos := 0
		if chosen.Kind == compile.StickerName {
			namePos = c.ChooseStickerNamePosition(g, owner, target, chosen)
			source.svars["stickeruniquevowels"] = uniqueVowels(chosen.Word)
		}
		g.addSticker(target, chosen, namePos)
		placed++
		source.svars["stickersplaced"] = placed
		g.checkStickerPlacedTriggers(c, target, owner, chosen.Kind)
	}
	return nil
}

// stickerTargets is PutStickerEffect.chooseObjects (:53-76): the Choices$ pick
// when there is one, else the targeted or Defined$ cards.
func (g *Game) stickerTargets(a *Ability, c PlayerController, source *Card, optional bool) ([]CardID, error) {
	spec, ok := a.Params.Param("Choices")
	if !ok {
		cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
		if err != nil {
			return nil, fmt.Errorf("engine: PutSticker: %w", err)
		}
		return cards, nil
	}
	zone := Battlefield
	if raw, ok := a.Params.Param("ChoiceZone"); ok {
		z, ok := ZoneByName(raw)
		if !ok {
			return nil, fmt.Errorf("engine: PutSticker: ChoiceZone$ %q is no zone", raw)
		}
		zone = z
	}
	parsed := valid.Parse(spec)
	var pool []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(zone, pid).Cards() {
			if g.Card(id).Owner == a.Controller && Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
				pool = append(pool, id)
			}
		}
	}
	if len(pool) == 0 {
		return nil, nil
	}
	lo := 1
	if optional {
		lo = 0
	}
	picked := c.ChooseCardsForEffect(g, a.Controller, a.Source, pool, lo, 1)
	if err := checkChoice(picked, pool, lo, 1); err != nil {
		return nil, fmt.Errorf("engine: PutSticker: %w", err)
	}
	return picked, nil
}

// checkStickerPlacedTriggers fires Mode$ StickerPlaced for one placed sticker
// (PutStickerEffect.java:137-140): ValidCard$ is matched against the stickered
// card, ValidPlayer$ against its owner and StickerKind$ (a comma list) against
// the sticker's kind (TriggerStickerPlaced.performTest, :36-52). The run
// params are Card, Player and StickerKind (setTriggeringObjects, :54-55).
func (g *Game) checkStickerPlacedTriggers(controller PlayerController, target CardID, owner PlayerID, kind compile.StickerKind) {
	card := g.Card(target)
	gate := func(h *Card, t *compile.Ability) bool {
		if spec, ok := t.Param("ValidCard"); ok && !Matches(g, card, valid.Parse(spec), h.Controller(), h.ID) {
			return false
		}
		if kinds, ok := t.Param("StickerKind"); ok {
			found := false
			for _, k := range strings.Split(kinds, ",") {
				found = found || strings.EqualFold(strings.TrimSpace(k), kind.String())
			}
			return found
		}
		return true
	}
	g.pushTriggeredAbilities(controller, g.playerActionTriggerMatchesWith(owner, triggeredObjects{card: target, player: owner}, gate, "StickerPlaced"))
}
