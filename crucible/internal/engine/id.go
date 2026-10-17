// Handles. Identity in the engine is an integer, never a pointer.

package engine

// CardID addresses a card in its game's arena. It is the index, so a lookup is
// a slice index and a comparison is `==` (GO-9, ADR-0009).
//
// Handles are never reused within a game. A card that leaves the battlefield
// keeps its slot, because last-known-information snapshots, `Remembered` lists
// and delayed triggers all hold handles to objects that are gone, and reusing
// a slot would silently alias them to something else.
type CardID uint32

// NoCard is the absent card. Slot 0 of every arena is reserved for it, so the
// zero value of a CardID field means "none" rather than "the first card ever
// created" -- a distinction that is otherwise a bug per struct.
const NoCard CardID = 0

// StackItemID addresses one thing that has ever been on the stack (ADR-0018).
// It is not a CardID: a copy of a spell (CopySpellAbility) and the spell it
// copies are the same card, or no card at all once the original has moved to
// the graveyard, but two distinct stack items -- and CR 707.10a's copy stops
// existing the moment it leaves the stack, while its host card's own CardID,
// if any, lives on in the arena. Assigned by PushAbility from Game's own
// monotonic counter (stack.go), on the same "never reused within a game"
// terms as CardID (id.go's own doc comment) and Card.Timestamp (game.go).
type StackItemID uint32

// NoStackItem is the absent stack item, on the same reasoning as [NoCard].
const NoStackItem StackItemID = 0

// PlayerID addresses a player. Games are two-player by default and Commander
// runs to six, so a byte is not a gamble.
type PlayerID uint8

// NoPlayer is the absent player, on the same reasoning as [NoCard].
const NoPlayer PlayerID = 0

// EntityID is a card, a player or an ability on the stack, for the places the
// rules treat them interchangeably: targeting, damage, and the `Defined$`
// vocabulary. An ability on the stack is a target of "counter target
// activated ability" and its kin (CR 115.1d-e, ADR-0041); a spell is still
// its card (CardEntity).
//
// The tag is the high bits rather than a struct with a kind, because an
// EntityID is stored in the thousands -- in target lists, remembered lists and
// LKI snapshots -- and a 4-byte handle that compares with `==` is the point of
// ADR-0009.
type EntityID uint32

// entityPlayer tags an EntityID as a player, entityStackItem as an ability on
// the stack. Cards keep the low range, so a card's EntityID is numerically its
// CardID.
const (
	entityPlayer    EntityID = 1 << 31
	entityStackItem EntityID = 1 << 30
)

// NoEntity is the absent entity.
const NoEntity EntityID = 0

// CardEntity is the entity handle for a card.
func CardEntity(id CardID) EntityID { return EntityID(id) }

// PlayerEntity is the entity handle for a player.
func PlayerEntity(id PlayerID) EntityID { return entityPlayer | EntityID(id) }

// StackItemEntity is the entity handle for an ability on the stack: the
// target of Stifle and its kin (ADR-0041). It names that one stack object,
// which stops existing once it leaves the stack.
func StackItemEntity(id StackItemID) EntityID { return entityStackItem | EntityID(id) }

// AsCard returns the card this entity is, and whether it is one.
func (e EntityID) AsCard() (CardID, bool) {
	if e == NoEntity || e&(entityPlayer|entityStackItem) != 0 {
		return NoCard, false
	}
	return CardID(e), true
}

// AsStackItem returns the stack item this entity is, and whether it is one.
func (e EntityID) AsStackItem() (StackItemID, bool) {
	if e&entityPlayer != 0 || e&entityStackItem == 0 {
		return NoStackItem, false
	}
	return StackItemID(e &^ entityStackItem), true
}

// AsPlayer returns the player this entity is, and whether it is one.
func (e EntityID) AsPlayer() (PlayerID, bool) {
	if e&entityPlayer == 0 {
		return NoPlayer, false
	}
	return PlayerID(e &^ entityPlayer), true
}

// IsCard reports whether the entity is a card.
func (e EntityID) IsCard() bool { _, ok := e.AsCard(); return ok }

// IsPlayer reports whether the entity is a player.
func (e EntityID) IsPlayer() bool { _, ok := e.AsPlayer(); return ok }
