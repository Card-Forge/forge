package fake

// layers may reference state — the config allows it.
func applyLayers(c Card) Card { return newCard(c.ID) }
