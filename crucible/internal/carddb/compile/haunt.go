package compile

import (
	"errors"
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/keyword"
)

// ErrBadHaunt is a `K:Haunt` keyword that names no SVar.
var ErrBadHaunt = errors.New("bad Haunt keyword")

// hauntSVar is the synthesized SVar every Haunt trigger that exiles the card
// executes: CardFactoryUtil.java:1262's hauntDiesEffectStr. Slotted like a
// keyword.Expand SVar, so it never collides with one the card defines.
const hauntSVar = "KWHaunt" + keyword.Slot

// expandHaunt is the `K:Haunt:<SVar>` keyword (CR 702.55), the two places
// CardFactoryUtil builds it: addTriggerAbility (CardFactoryUtil.java:1185-1272)
// and addSpellAbility (:3069-3083). It needs the face's type and the SVar's own
// text, which keyword.Expand never sees, so it is expanded here.
//
// For a card that is not a creature the SVar is also the spell itself:
// CardFactoryUtil.java:3075-3081 rewrites its "DB$" to "SP$" and appends
// "Cost$ 0 | StackDescription$ SpellDescription", then sets the spell's cost to
// the card's mana cost. The Cost$ 0 placeholder is not appended here: an SP$
// line's Cost$ is its additional cost in this port (spellAdditionalCost,
// engine/castspell.go), and no Cost$ already means "the card's mana cost".
// StackDescription$ is display only.
//
// The triggers, in Java's order minus the two Static$ "unhaunt" lines
// (haunterUnExiled, trigHauntRemoved), whose job the engine does by object
// identity instead (the haunt mark records both objects' zone stamps and
// stops matching once either leaves its zone, CR 400.7):
//
//   - a creature's "when this enters": the SVar.
//   - the haunter going to a graveyard: from the battlefield for a creature,
//     from the stack only after resolving (Fizzle$ False) for a spell. Runs
//     DB$ Haunt targeting a creature, which exiles the card haunting it.
//   - the haunted creature dying, watched from exile: the SVar.
func (c *faceCompiler) expandHaunt(out *Face, line, slot string) error {
	k := keyword.Parse(line)
	args := k.Args()
	if len(args) < 1 || args[0] == "" {
		return fmt.Errorf("%w: %q names no SVar", ErrBadHaunt, line)
	}
	effect := args[0]
	body, ok := c.face.SVars.Get(effect)
	if !ok {
		return fmt.Errorf("%w: Haunt names %q", ErrMissingSVar, effect)
	}
	exile := strings.ReplaceAll(hauntSVar, keyword.Slot, slot)
	if _, own := c.face.SVars.Get(exile); own {
		return fmt.Errorf("keyword %q: synthesized SVar %q collides with the card's own", line, exile)
	}
	if c.extra == nil {
		c.extra = map[string]string{}
	}
	c.extra[exile] = "DB$ Haunt | ValidTgts$ Creature | TgtPrompt$ Choose target creature to haunt"

	creature := c.face.Type.Has(cardtype.Creature)
	if !creature {
		spell, err := c.line(strings.ReplaceAll(body, "DB$", "SP$"), Spell)
		if err != nil {
			return fmt.Errorf("keyword %q: %w", line, err)
		}
		spell.Keyword = line
		out.Abilities = append(out.Abilities, spell)
	}

	var triggers []string
	if creature {
		triggers = append(triggers, "Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ "+effect+" | Secondary$ True")
		triggers = append(triggers, "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | Secondary$ True | Execute$ "+exile)
	} else {
		triggers = append(triggers, "Mode$ ChangesZone | Origin$ Stack | Fizzle$ False | Destination$ Graveyard | ValidCard$ Card.Self | Secondary$ True | Execute$ "+exile)
	}
	triggers = append(triggers, "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | TriggerZones$ Exile | ValidCard$ Creature.HauntedBy | Execute$ "+effect)
	for _, text := range triggers {
		t, err := c.line(text, Trigger)
		if err != nil {
			return fmt.Errorf("keyword %q: %w", line, err)
		}
		t.Keyword = line
		out.Triggers = append(out.Triggers, t)
	}
	return nil
}
