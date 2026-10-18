// Prepare (Secrets of Strixhaven): a creature with a prepare spell "becomes
// prepared", and while it is its controller may cast a copy of that spell;
// casting the copy, or the creature leaving the battlefield, unprepares it.
//
// Java builds this from an Effect card (AlterAttributeEffect.java:65-89): a
// token copy of the creature in its PreparedSpell state is put into its
// activator's exile (CopyPermanentEffect.getProtoType, GamePieceType.TOKEN),
// and a command-zone effect holds a MayPlay$ static for that copy
// (MayPlayPlayer$ EffectSourceController), a static SpellCast trigger that
// unprepares the creature when the copy is cast, and the creature gets a
// leaves-play command that unprepares it. Here the creature and the copy name
// each other (Card.prepared, Card.preparedFrom): castOptions reads the link as
// the MayPlay$ grant, castFromHand unprepares after the cast, and Game.Move
// unprepares a creature leaving the battlefield.

package engine

import (
	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// prepare makes id prepared for activator (AlterAttributeEffect.java:67-89):
// nothing happens when it already is or has no prepare spell
// (hasState(PreparedSpell): a Prepare card's second face).
func (g *Game) prepare(id CardID, activator PlayerID) {
	c := g.Card(id)
	if c.prepared != NoCard || c.Def == nil || c.Def.SplitType != carddb.SplitPrepare || c.Def.Faces[1].Name == "" {
		return
	}
	copyID := g.NewCard(preparedSpellDef(c.Def), activator, Exile)
	// NewCard may have grown the arena: re-read both cards.
	spell := g.Card(copyID)
	spell.IsToken = true
	spell.preparedFrom = id
	g.Card(id).prepared = copyID
}

// preparedSpellDef is the copy's characteristics: the card in its
// PreparedSpell state, its second face standing alone.
func preparedSpellDef(def *compile.Card) *compile.Card {
	out := &compile.Card{Filename: def.Filename, Name: def.Faces[1].Name}
	out.Faces[0] = def.Faces[1]
	return out
}

// unprepare is Card.setPrepared(null) (Card.java:6685-6694): the creature
// stops being prepared, and a copy still in exile ceases to exist (a cast copy
// is on the stack and stays a spell).
func (g *Game) unprepare(id CardID) {
	c := g.Card(id)
	spell := c.prepared
	if spell == NoCard {
		return
	}
	c.prepared = NoCard
	if g.Card(spell).Zone == Exile {
		g.ceaseToExist(spell)
	}
}

// preparedCaster is the player who may cast card as a prepared spell: the
// controller of the creature it is the prepared copy of, while it is still
// that creature's copy and in exile (the effect's MayPlay$ static,
// MayPlayPlayer$ EffectSourceController, AffectedZone$ Exile). NoPlayer for
// any other card.
func (g *Game) preparedCaster(card CardID) PlayerID {
	c := g.Card(card)
	if c.preparedFrom == NoCard || c.Zone != Exile {
		return NoPlayer
	}
	host := g.Card(c.preparedFrom)
	if host.prepared != card {
		return NoPlayer
	}
	return host.Controller()
}

// preparedCast is the effect's static SpellCast trigger
// (AlterAttributeEffect.java:77-82): casting a creature's prepared copy
// unprepares the creature.
func (g *Game) preparedCast(card CardID) {
	c := g.Card(card)
	if c.preparedFrom == NoCard {
		return
	}
	if host := g.Card(c.preparedFrom); host.prepared == card {
		host.prepared = NoCard
	}
}

// hiddenPreparedSpell reports whether c is a prepared copy waiting in exile,
// which no valid string matches (Card.isValid's "filter out prepared spells
// for other cards", Card.java:5845-5848) and which is not a token CR 704.5d
// removes (GameAction.java:1922-1924).
func hiddenPreparedSpell(c *Card) bool {
	return c.preparedFrom != NoCard && c.Zone == Exile
}
