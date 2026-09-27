// RulesMod: Layer 8's own player-facing continuous effects, the mutable
// part a Player's own HandSizeLimit/LandPlayLimit (player.go) folds against
// the printed defaults (MaxHandSize, land.go's own default land-play count).

package engine

// RulesMod is the Layer 8 continuous effects currently affecting one
// player -- CR 613's own rule-changing catch-all layer, which carries no CR
// number of its own (layer.go's own doc comment), trimmed to the corpus's
// player-facing real shapes: SetMaxHandSize$/RaiseMaxHandSize$ (a player's
// own maximum hand size, 43 and 8 real lines), AdjustLandPlays$ (how many
// lands a turn allows, 27 real lines) and the vote/villainous-choice params
// Vote and VillainousChoice read (AdditionalVote$, AdditionalOptionalVote$,
// AdditionalVillainousChoice$, ControlVote$). The per-card Layer 8 params
// live elsewhere; applyOneContinuousRules's own doc comment (continuous.go)
// has what is not resolved and why.
type RulesMod struct {
	effects []RulesEffect
}

// RulesEffect is one continuous effect's contribution to a player's own
// hand-size and/or land-play limit. HasSetHandSize/HasRaiseHandSize mirror
// PTEffect's own HasPower/HasToughness split (pt.go): a real corpus line
// carries at most one of SetMaxHandSize$/RaiseMaxHandSize$, never both, but
// the flags still let HandSizeLimit (player.go) apply only the dimension a
// given effect actually names, the same reason PTEffect's own flags exist.
//
// HasAdjustLandPlays plays the identical role for AdjustLandPlays$, even
// though Player.getMaxLandPlays's own unconditional sum (Java) would make a
// bare zero-value RulesEffect harmless there too (0 added is already a
// no-op) -- kept anyway so a RulesEffect that names only a hand-size
// dimension cannot be misread as also asserting "adjust land plays by 0."
type RulesEffect struct {
	// Timestamp orders this effect against every other one on the same
	// player, foldPT's own combine convention (card.go) -- the one dimension
	// here that is order-dependent (HandSizeLimit's own doc comment).
	Timestamp uint64

	HasSetHandSize       bool
	SetHandSize          int
	SetHandSizeUnlimited bool

	HasRaiseHandSize bool
	RaiseHandSize    int

	HasAdjustLandPlays       bool
	AdjustLandPlays          int
	AdjustLandPlaysUnlimited bool

	// AdditionalVotes, AdditionalOptionalVotes and AdditionalVillainousChoices
	// are AdditionalVote$/AdditionalOptionalVote$/AdditionalVillainousChoice$
	// (Player.addAdditionalVote/addAdditionalOptionalVote/
	// addAdditionalVillainousChoices): each sums across every effect on the
	// player, order-free, the way Player.getAdditionalVotesAmount does. Zero
	// is "this effect adds none", so no Has flag is needed.
	AdditionalVotes             int
	AdditionalOptionalVotes     int
	AdditionalVillainousChoices int

	// ControlVote is ControlVote$ (Player.addControlVote): the player
	// chooses how every player votes. Game.getControlVote picks the player
	// holding the latest such effect by Timestamp.
	ControlVote bool
}

// additionalVotes sums every effect's AdditionalVotes
// (Player.getAdditionalVotesAmount).
func (r RulesMod) additionalVotes() int {
	n := 0
	for _, e := range r.effects {
		n += e.AdditionalVotes
	}
	return n
}

// additionalOptionalVotes sums every effect's AdditionalOptionalVotes
// (Player.getAdditionalOptionalVotesAmount).
func (r RulesMod) additionalOptionalVotes() int {
	n := 0
	for _, e := range r.effects {
		n += e.AdditionalOptionalVotes
	}
	return n
}

// additionalVillainousChoices sums every effect's AdditionalVillainousChoices
// (Player.getAdditionalVillainousChoices).
func (r RulesMod) additionalVillainousChoices() int {
	n := 0
	for _, e := range r.effects {
		n += e.AdditionalVillainousChoices
	}
	return n
}

// controlVote is Player.getHighestControlVote: the latest Timestamp among
// this player's ControlVote effects, and whether there is one.
func (r RulesMod) controlVote() (uint64, bool) {
	var best uint64
	found := false
	for _, e := range r.effects {
		if e.ControlVote && (!found || e.Timestamp > best) {
			best, found = e.Timestamp, true
		}
	}
	return best, found
}

// Add records one continuous effect. Order does not matter here for the
// same reason PT.Add's own doc comment gives: HandSizeLimit sorts by
// Timestamp itself before folding.
func (r *RulesMod) Add(e RulesEffect) { r.effects = append(r.effects, e) }

// Clear removes every effect -- applyContinuousRules' own recompute-fresh
// pass (continuous.go), PT.Clear's own reasoning applied to a player rather
// than a card.
func (r *RulesMod) Clear() { r.effects = nil }

// clone is RulesMod's half of Game.Clone, PT.clone's own reasoning: a
// shared backing array would let a push on the clone alias the original.
func (r RulesMod) clone() RulesMod {
	return RulesMod{effects: append([]RulesEffect(nil), r.effects...)}
}

// mayPlayGrant is one MayPlay$ permission a Mode$ Continuous static gives
// this pass: Java's CardPlayOption (Card.setMayPlay, Card.java:3818), the
// Layer 8 grant that lets Grantee cast or play CardID from a zone they
// normally could not.
//
// Timestamp is the card's own as the grant was made: every zone change
// stamps a new one, so a card that moved since (cast, milled, returned) is
// a new object the grant no longer names (CR 400.7). WithoutManaCost is
// MayPlayWithoutManaCost$, WithFlash MayPlayWithFlash$, and ZonePermission
// is false only under MayPlayDontGrantZonePermissions$: such a grant
// changes how a card is cast but not whether its zone allows casting at
// all (SpellAbilityRestriction.java:239-241).
type mayPlayGrant struct {
	CardID          CardID
	Timestamp       uint64
	Grantee         PlayerID
	WithoutManaCost bool
	WithFlash       bool
	ZonePermission  bool
}
