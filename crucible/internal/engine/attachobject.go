// AttachEffect's Object$ branch: attach named cards to a named host without
// a choice of host (Stolen Uniform).

package engine

//enginelint:allow id card game ability control defined zone condition staticability valid action effecthelpers replacement

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// attachObject is AttachEffect.resolve's `Object$` branch without Choices$:
// each Object$ card is attached to the creature Defined$ (or a target) names
// (Stolen Uniform's "attach it to the chosen creature"). Java asks the
// chooser to pick among the host candidates; with one candidate that is no
// decision, and several are refused rather than guessed (GO-7). An
// attachment that left the battlefield is skipped; the equipment legality
// checks are the activated shape's (a creature host, Protection).
func (g *Game) attachObject(a *Ability, controller PlayerController) error {
	for _, key := range [...]string{"PlayerChoices", "Chooser", "Move"} {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Attach: %s$ with Object$ not resolvable yet", key)
		}
	}
	if z, ok := a.Params.Param("ChoiceZone"); ok && z != "Battlefield" {
		return fmt.Errorf("engine: Attach: ChoiceZone$ %q with Object$ not resolvable yet", z)
	}
	_, hasChoices := a.Params.Param("Choices")
	if _, optional := a.Params.Param("Optional"); optional && !hasChoices {
		return fmt.Errorf("engine: Attach: Optional$ with Object$ and no Choices$ not resolvable yet")
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, _ := a.Params.Param("Object")
	attachments, err := definedCards(source, raw, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Attach: Object$: %w", err)
	}
	if hasChoices {
		return g.attachObjectToChoice(a, controller, attachments)
	}
	targets, err := targetedOrDefinedEntities(g, a)
	if err != nil {
		return err
	}
	if len(targets) == 0 {
		return nil
	}
	if len(targets) > 1 {
		return fmt.Errorf("engine: Attach: choosing among %d Defined$ hosts not resolvable yet", len(targets))
	}
	host, ok := targets[0].AsCard()
	if !ok || g.Card(host).Zone != Battlefield || !g.Card(host).Type().Has(cardtype.Creature) {
		return nil
	}
	for _, id := range attachments {
		if g.Card(id).Zone != Battlefield || id == host || hostRefusesAttach(g, g.Card(id), host) {
			continue
		}
		g.attachTo(controller, id, host)
	}
	return nil
}

// attachDefined is AttachEffect.resolve for an Attach with neither Object$ nor
// ValidTgts$: the source itself attaches to the one host Defined$ names
// (Animate Dead's "DB$ Attach | Defined$ Remembered", re-attaching the Aura to
// the creature it just returned, AttachEffect.java:111-119, 133-152). Java
// asks the chooser to pick among several hosts; with one that is no decision,
// and several are refused rather than guessed (GO-7), as attachObject does. The
// attachment goes through Card.attachToEntity's canBeAttached gate (canBeAttached,
// below) and is skipped when the source has left the battlefield.
func (g *Game) attachDefined(a *Ability, controller PlayerController) error {
	for _, key := range [...]string{"Choices", "PlayerChoices", "Optional", "Chooser", "Move"} {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Attach: %s$ with Defined$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	targets, err := targetedOrDefinedEntities(g, a)
	if err != nil {
		return err
	}
	if len(targets) == 0 {
		return nil
	}
	if len(targets) > 1 {
		return fmt.Errorf("engine: Attach: choosing among %d Defined$ hosts not resolvable yet", len(targets))
	}
	host, ok := targets[0].AsCard()
	if !ok {
		return fmt.Errorf("engine: Attach: a player host via Defined$ not resolvable yet")
	}
	if source.Zone != Battlefield || !g.canBeAttached(source, host) {
		return nil
	}
	g.attachTo(controller, a.Source, host)
	if cur, ok := source.AttachedTo(); ok && cur == host && hasParam(a, "RememberAttached") {
		source.Memory.Remember(CardEntity(a.Source))
	}
	return nil
}

// attachObjectToChoice is AttachEffect.java:72-152 for Object$ with Choices$:
// the activator picks one battlefield permanent matching Choices$ that every
// attachment can be attached to (CardPredicates.canBeAttached), a pick that
// is not optional (chooseSingleEntityForEffect's four-argument form); then
// each attachment still on the battlefield is attached to it, after an
// Optional$ "do you want to attach" confirm. Gift of Doom's "as it is turned
// face up, you may attach it to a creature".
func (g *Game) attachObjectToChoice(a *Ability, controller PlayerController, attachments []CardID) error {
	if len(attachments) == 0 {
		return nil
	}
	spec, _ := a.Params.Param("Choices")
	parsed := valid.Parse(spec)
	var choices []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if !Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
				continue
			}
			ok := true
			for _, att := range attachments {
				ok = ok && g.canBeAttached(g.Card(att), id)
			}
			if ok {
				choices = append(choices, id)
			}
		}
	}
	if len(choices) == 0 {
		return nil
	}
	picked := controller.ChooseCardsForEffect(g, a.Controller, a.Source, choices, 1, 1)
	if err := checkChoice(picked, choices, 1, 1); err != nil {
		return fmt.Errorf("engine: Attach: %w", err)
	}
	host := picked[0]
	_, optional := a.Params.Param("Optional")
	for _, id := range attachments {
		if g.Card(id).Zone != Battlefield {
			continue
		}
		if optional && !controller.ConfirmEffect(g, a.Controller, a.Source) {
			continue
		}
		g.attachTo(controller, id, host)
		// AttachEffect.java:149-151: RememberAttached$ remembers an attachment
		// that did end up on the host.
		if cur, ok := g.Card(id).AttachedTo(); ok && cur == host && hasParam(a, "RememberAttached") {
			g.Card(a.Source).Memory.Remember(CardEntity(id))
		}
	}
	return nil
}

// canBeAttached is Card.cantBeAttachedMsg == null for a card host on the
// battlefield (Card.java:7029-7034, GameEntity.java:236-307,
// Card.java:6996-7026): only an attachment (Aura, Equipment, Fortification),
// never onto itself, not a creature (Reconfigure is not ported), neither one
// phased out; an Aura needs an Enchant keyword whose type the host is (a
// player Aura's is not a card's); Equipment a creature host; Fortification a
// land host and not itself a land. Of StaticAbilityCantAttach only the
// host's Protection is read (hostRefusesAttach); no other CantAttach static
// is modelled.
func (g *Game) canBeAttached(attachment *Card, host CardID) bool {
	h := g.Card(host)
	t := attachment.Type()
	isAura, isEquipment, isFortification := t.HasSubtype("Aura"), t.HasSubtype("Equipment"), t.HasSubtype("Fortification")
	if !isAura && !isEquipment && !isFortification {
		return false
	}
	if attachment.ID == host || t.Has(cardtype.Creature) || attachment.IsPhasedOut() || h.IsPhasedOut() {
		return false
	}
	ht := h.Type()
	if isAura {
		if _, player := enchantPlayerSpec(attachment); player {
			return false
		}
		spec, ok := enchantSpec(attachment)
		if !ok || !Matches(g, h, spec, attachment.Controller(), attachment.ID) {
			return false
		}
		// Card.java:6986: a host outside play only when the Enchant type names
		// a zone.
		if h.Zone != Battlefield && !enchantsOutsidePlay(spec) {
			return false
		}
	}
	if isEquipment && !ht.Has(cardtype.Creature) {
		return false
	}
	if isFortification && (!ht.Has(cardtype.Land) || t.Has(cardtype.Land)) {
		return false
	}
	return !hostRefusesAttach(g, attachment, host)
}

// targetedOrDefinedEntities is getDefinedEntitiesOrTargeted(sa, "Defined"):
// the ability's targets when it has ValidTgts$, else its Defined$ entities
// (default Self).
func targetedOrDefinedEntities(g *Game, a *Ability) ([]EntityID, error) {
	if _, ok := a.Params.Param("ValidTgts"); ok {
		return a.Targets, nil
	}
	def, ok := a.Params.Param("Defined")
	if !ok {
		def = "Self"
	}
	es, err := definedEntities(g, a.Controller, g.Card(a.Source), def, a.refs())
	if err != nil {
		return nil, fmt.Errorf("engine: Attach: Defined$: %w", err)
	}
	return es, nil
}
