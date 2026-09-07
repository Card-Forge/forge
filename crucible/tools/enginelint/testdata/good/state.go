package fake

type Card struct{ ID uint32 }

func newCard(id uint32) Card { return Card{ID: id} }
