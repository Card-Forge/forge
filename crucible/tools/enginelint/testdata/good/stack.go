package fake

// stack may reference layers and state.
func resolve(c Card) Card { return applyLayers(c) }

// Builder shares its name with strings.Builder, which state.go uses.
type Builder struct{}
