package fake

import "strings"

type Card struct{ ID uint32 }

func newCard(id uint32) Card { return Card{ID: id} }

// label names strings.Builder: a qualified reference, never one to stack.go's
// Builder, so it is no violation.
func label(b *strings.Builder) string { return b.String() }
