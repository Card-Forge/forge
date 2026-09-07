package fake

type Card struct{ ID uint32 }

// Violation: state must not know about the stack.
func badBackReference(c Card) Card { return resolve(c) }
