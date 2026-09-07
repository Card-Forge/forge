package fake

// stack may reference layers and state.
func resolve(c Card) Card { return applyLayers(c) }
