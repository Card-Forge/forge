// A player's notes: what Pump NoteCards$/NoteCardsFor$ writes about cards and
// NoteNumber$ about a number, read back by the NotedFor<key> card and player
// properties and Count$NotedNumber (Player.addNoteForName, getNotesForName,
// clearNotesForName, noteNumberForName, getNotedNumberForName,
// Player.java:1519-1546). A note is "Id:<card id>"; the properties also read
// "Name:<card name>" notes, which no effect here writes.

//enginelint:allow id card game player

package engine

import "strconv"

// playerNote is the notes kept under one name.
type playerNote struct {
	name    string
	entries []string
}

// playerNotedNumber is the number noted under one name.
type playerNotedNumber struct {
	name string
	n    int
}

func noteID(card CardID) string { return "Id:" + strconv.FormatUint(uint64(card), 10) }

func (p *Player) addNote(name, note string) {
	for i := range p.notes {
		if p.notes[i].name == name {
			p.notes[i].entries = append(p.notes[i].entries, note)
			return
		}
	}
	p.notes = append(p.notes, playerNote{name: name, entries: []string{note}})
}

func (p *Player) clearNotes(name string) {
	for i := range p.notes {
		if p.notes[i].name == name {
			p.notes[i].entries = nil
		}
	}
}

// notedCard reports whether p noted card under name, by id or by card name.
func (p *Player) notedCard(name string, card *Card) bool {
	for _, n := range p.notes {
		if n.name != name {
			continue
		}
		for _, e := range n.entries {
			if e == noteID(card.ID) || (card.Def != nil && e == "Name:"+card.Def.Name) {
				return true
			}
		}
	}
	return false
}

func (p *Player) noteNumber(name string, n int) {
	for i := range p.notedNumbers {
		if p.notedNumbers[i].name == name {
			p.notedNumbers[i].n = n
			return
		}
	}
	p.notedNumbers = append(p.notedNumbers, playerNotedNumber{name, n})
}

func (p *Player) notedNumber(name string) int {
	for _, n := range p.notedNumbers {
		if n.name == name {
			return n.n
		}
	}
	return 0
}
