// The mana pool: CR 106.4's floating mana, and CR 601.2h/601.2i's payment
// algorithm for the plain-colored-and-generic case.

package engine

import "github.com/jczastkiewicz/crucible/internal/mana"

// Pool is what one player has floating (CR 106.4) -- mana added but not yet
// spent, held separately by the five colors and by the {C} colorless mana
// type, and separately again by whether it is snow mana (CR 106.3a: mana a
// snow permanent produces, still its own color, but also able to pay a
// snow ({S}) requirement no non-snow mana of that color can). The zero
// value is empty.
//
// The snow half of each color is a disjoint bucket, not a subset count
// layered on top of the plain one -- Add puts mana in one or the other, and
// spending it (below) decides which bucket a unit comes from, rather than
// tracking "how many of this color happen to be snow" as a derived fact
// that would need its own invariant against the plain count.
//
// Generic amount is not stored here: CR 106.6's "any type of mana can pay a
// generic cost" means nothing about a floating mana's own color changes
// once it is already in the pool -- the distinction generic payment cares
// about is which shards of the *cost* are colored, not which mana in the
// pool was added as what.
type Pool struct {
	white, blue, black, red, green, colorless                         int
	snowWhite, snowBlue, snowBlack, snowRed, snowGreen, snowColorless int
	// units is every floating unit's provenance in the order it was added
	// (ADR-0042): per bucket exactly as many entries as that bucket's
	// counter, the oldest first. The counters stay the payment authority; a
	// payment takes each decremented bucket's oldest units (takeSpent), as
	// Java's ManaPool.tryPayCostWithColor spends the first Mana of a color
	// in its list (ManaPool.java:228-250). The slice is owned by this Pool:
	// Game.Clone copies it, and Empty drops it rather than truncating, so a
	// value copy (DrainMana's drained pool) never sees a later write.
	units []poolUnit
}

// manaSource is the card a unit of mana came from and that card's
// zone-change timestamp when its mana ability was activated: Mana.sourceCard,
// the object as it was then (ADR-0042). The zero value (NoCard) is mana
// added with no source -- a fixture, the smoke probe, a test.
type manaSource struct {
	card  CardID
	stamp uint64
}

// poolUnit is one floating unit: its bucket (0-5 plain white, blue, black,
// red, green, {C}; 6-11 the same order snow) and its source.
type poolUnit struct {
	bucket uint8
	src    manaSource
}

// Pool buckets, the index order of counters and SnowBreakdown.
const (
	bucketColorless = 5
	bucketSnow      = 6
)

// counters is the twelve buckets in poolUnit.bucket order.
func (p *Pool) counters() [12]*int {
	return [12]*int{&p.white, &p.blue, &p.black, &p.red, &p.green, &p.colorless,
		&p.snowWhite, &p.snowBlue, &p.snowBlack, &p.snowRed, &p.snowGreen, &p.snowColorless}
}

// colorBucket is the plain bucket of one color. color must be exactly one of
// the five; anything else is an engine invariant breach (GO-7), reported by
// ok false for the caller to panic on with its own name.
func colorBucket(color mana.Colors) (int, bool) {
	switch color {
	case mana.White:
		return 0, true
	case mana.Blue:
		return 1, true
	case mana.Black:
		return 2, true
	case mana.Red:
		return 3, true
	case mana.Green:
		return 4, true
	}
	return 0, false
}

// addUnits adds n units to bucket b, each from src.
func (p *Pool) addUnits(b, n int, src manaSource) {
	if n <= 0 {
		return
	}
	*p.counters()[b] += n
	for range n {
		p.units = append(p.units, poolUnit{bucket: uint8(b), src: src})
	}
}

// Add puts n mana of one color in the pool. color must be exactly one of
// mana.White/Blue/Black/Red/Green -- anything else (the zero value, more
// than one bit set) is an engine invariant breach, not something a card
// script can cause, so it panics rather than silently doing nothing (GO-7).
func (p *Pool) Add(color mana.Colors, n int) {
	b, ok := colorBucket(color)
	if !ok {
		panic("engine: Pool.Add wants exactly one color")
	}
	p.addUnits(b, n, manaSource{})
}

// AddColorless puts n {C} mana in the pool -- CR 106.3's colorless mana
// symbol, distinct from mana.Colors' own empty set (mana.Colors' own doc
// comment: {C} is a way to pay a cost, not a color, and is not
// representable there at all).
func (p *Pool) AddColorless(n int) { p.addUnits(bucketColorless, n, manaSource{}) }

// AddSnow puts n snow mana of one color in the pool -- the same color
// contract [Pool.Add] carries, just landing in the snow bucket instead of
// the plain one. Snow mana of a color pays that color's pips and a generic
// amount exactly the same as plain mana of it does ([Pool.Pay]'s own
// fallback); the only thing it can do that plain mana cannot is pay a snow
// ({S}) requirement.
func (p *Pool) AddSnow(color mana.Colors, n int) {
	b, ok := colorBucket(color)
	if !ok {
		panic("engine: Pool.AddSnow wants exactly one color")
	}
	p.addUnits(bucketSnow+b, n, manaSource{})
}

// AddSnowColorless puts n snow {C} mana in the pool -- [Pool.AddColorless]'s
// own contract, snow-tagged.
func (p *Pool) AddSnowColorless(n int) { p.addUnits(bucketSnow+bucketColorless, n, manaSource{}) }

// takeSpent removes the units a payment spent -- for every bucket, its
// oldest units past the bucket's counter -- and returns them in pool order.
func (p *Pool) takeSpent() []poolUnit {
	var excess [12]int
	for _, u := range p.units {
		excess[u.bucket]++
	}
	spentAny := false
	for b, n := range p.counters() {
		excess[b] -= *n
		spentAny = spentAny || excess[b] > 0
	}
	if !spentAny {
		return nil
	}
	var spent []poolUnit
	kept := p.units[:0]
	for _, u := range p.units {
		if excess[u.bucket] > 0 {
			excess[u.bucket]--
			spent = append(spent, u)
			continue
		}
		kept = append(kept, u)
	}
	p.units = kept
	return spent
}

// clone is the pool with its own copy of units; an empty list allocates
// nothing (TestCloneAllocationsStayBounded).
func (p Pool) clone() Pool {
	if len(p.units) > 0 {
		p.units = append([]poolUnit(nil), p.units...)
	} else {
		p.units = nil
	}
	return p
}

// Total is how much mana of any kind is in the pool, snow and plain alike.
func (p *Pool) Total() int {
	return p.white + p.blue + p.black + p.red + p.green + p.colorless +
		p.snowWhite + p.snowBlue + p.snowBlack + p.snowRed + p.snowGreen + p.snowColorless
}

// Breakdown is how much floating mana of each type the pool holds, snow and
// plain summed together since both are equally that color, in a fixed
// order -- white, blue, black, red, green, {C} -- rather than one exported
// reader per field: a caller that needs to compare two pools wholesale (the
// scenario harness, TestScenarios) can compare the array directly with ==.
// [Pool.SnowBreakdown] is the same shape for the snow half alone; the two
// together pin down every one of the twelve underlying buckets exactly
// (plain = Breakdown - SnowBreakdown), without exporting a reader per bucket.
func (p *Pool) Breakdown() [6]int {
	return [6]int{
		p.white + p.snowWhite,
		p.blue + p.snowBlue,
		p.black + p.snowBlack,
		p.red + p.snowRed,
		p.green + p.snowGreen,
		p.colorless + p.snowColorless,
	}
}

// SnowBreakdown is [Pool.Breakdown]'s own shape and order, snow mana only.
func (p *Pool) SnowBreakdown() [6]int {
	return [6]int{p.snowWhite, p.snowBlue, p.snowBlack, p.snowRed, p.snowGreen, p.snowColorless}
}

// convertTo turns every unit of mana in the pool into one type, snow kept as
// snow: ManaPool.convertManaColor for each other type, what an Event$
// LoseMana replacement (Kruphix, God of Horizons) does instead of emptying
// the pool (loseManaConversion, replacementevents.go). colorless picks {C};
// otherwise color must be exactly one of the five.
func (p *Pool) convertTo(color mana.Colors, colorless bool) {
	plain := [6]*int{&p.white, &p.blue, &p.black, &p.red, &p.green, &p.colorless}
	snow := [6]*int{&p.snowWhite, &p.snowBlue, &p.snowBlack, &p.snowRed, &p.snowGreen, &p.snowColorless}
	to := 5
	if !colorless {
		switch color {
		case mana.White:
			to = 0
		case mana.Blue:
			to = 1
		case mana.Black:
			to = 2
		case mana.Red:
			to = 3
		case mana.Green:
			to = 4
		default:
			panic("engine: Pool.convertTo wants exactly one color or colorless")
		}
	}
	for i := range plain {
		if i == to {
			continue
		}
		*plain[to] += *plain[i]
		*plain[i] = 0
		*snow[to] += *snow[i]
		*snow[i] = 0
	}
	// Each unit keeps its source and its place, now of the one type
	// (ManaPool.convertManaColor's Mana.convertColor keeps sourceCard).
	for i, u := range p.units {
		if u.bucket >= bucketSnow {
			p.units[i].bucket = uint8(bucketSnow + to)
		} else {
			p.units[i].bucket = uint8(to)
		}
	}
}

// Empty clears the pool -- CR 500.4, run once per phase/step transition
// (emptyManaPools, turn.go) for every player, not something a card ability
// triggers.
func (p *Pool) Empty() { *p = Pool{} }

// Pay spends cost from the pool and reports whether it succeeded. The pool
// is unchanged if it did not -- Pay never spends part of a cost it cannot
// finish paying. It is [Pool.PayWithSnow] called with no snow shards to
// resolve.
//
// Only cost.Generic and the six "pure" shards (ShardW/U/B/R/G/C, CR
// 601.2h's colored-and-generic case, and Java's own ManaCostShard
// declaration order: "shards that offer the fewest ways to be paid come
// first") are handled. A shard with any other atom -- hybrid (ShardWU and
// the rest), Phyrexian ({R/P}), {X}, a raw snow ({S}) this port's own
// choice for which color has not resolved -- is a shape this port has no
// substitution rule for yet: which of two colors a hybrid symbol takes,
// whether a Phyrexian symbol is paid with mana or 2 life, is a decision (the
// same category ChooseLegendaryToKeep's own answer is), and this port has
// no PlayerController method to ask an unresolved one of these with. Pay
// reports false for a cost containing one, the same "cannot resolve this
// yet" answer insufficient mana gets -- a caller cannot tell the two apart
// from the bool alone, which is deliberate: neither means the pool can
// safely be spent.
//
// A plain colored pip or a unit of generic spends the matching plain bucket
// first, falling back to that color's snow bucket only once the plain one is
// empty (CR 106.3a: snow mana is still that color, so it pays a same-color
// pip or a generic unit exactly as plain mana does) -- {S} is the only
// requirement snow-tagged mana pays that plain mana of the same color
// cannot, so nothing else needs the split at all, and this fallback order is
// what keeps a pool holding only snow mana able to pay an all-plain cost
// unchanged. Generic itself is paid from whatever the pool has left after
// every pip, in a fixed order (colorless, then white/blue/black/red/green,
// plain before that color's own snow) -- a caller that bypasses PayManaCost
// and reaches Pay directly gets this order instead of a real choice.
// PayManaCost (manapay.go) is CR 601.2h/CR 106.6's real answer: it resolves
// every unit of generic into an explicit shard via ChoosePayGeneric before it
// ever calls Pay, so the cost Pay sees when called from there always has
// zero generic left to guess about.
func (p *Pool) Pay(cost mana.Cost) bool {
	return p.PayWithSnow(cost, nil)
}

// PayWithSnow is [Pool.Pay] extended with snow ({S}, CR 106.3a) shards
// already resolved to a color: snow answers one per occurrence, in the same
// order [Game.PayManaCost] asked ChoosePaySnow for them, each spent only
// from that color's own snow bucket -- unlike a plain pip or a generic unit,
// nothing else can cover an {S} requirement, so there is no plain-first
// fallback here the way there is for the rest of cost.
//
// The whole payment -- cost's own shards and generic, plus every entry in
// snow -- succeeds or fails together: a snow shard consumed by an early
// snow entry is still on the table for a later plain pip's fallback (and
// vice versa is not true, since a plain bucket is never touched to cover an
// {S} requirement), and any failure anywhere leaves the pool exactly as it
// was, the same all-or-nothing guarantee Pay itself already makes.
func (p *Pool) PayWithSnow(cost mana.Cost, snow []mana.Shard) bool {
	_, ok := p.payWithSnow(cost, snow, false)
	return ok
}

// payWithSnow is [Pool.PayWithSnow]; snowAny is a cast through
// MayPlaySnowIgnoreColor$ (ManaConversionMatrix.snowForColor), where snow mana
// of any type pays a colored pip (ManaCostBeingPaid.canBePaidWith: any shard
// with a color mask takes any snow mana). It is tried after the pip's own plain
// and snow mana, colorless snow first, then the other colors in WUBRG order --
// the choice of which snow source pays is the payer's, and this port picks.
// It returns the units spent (takeSpent, ADR-0042).
func (p *Pool) payWithSnow(cost mana.Cost, snow []mana.Shard, snowAny bool) ([]poolUnit, bool) {
	spend := *p
	for _, s := range cost.Shards() {
		var plain, snowBucket *int
		switch s {
		case mana.ShardW:
			plain, snowBucket = &spend.white, &spend.snowWhite
		case mana.ShardU:
			plain, snowBucket = &spend.blue, &spend.snowBlue
		case mana.ShardB:
			plain, snowBucket = &spend.black, &spend.snowBlack
		case mana.ShardR:
			plain, snowBucket = &spend.red, &spend.snowRed
		case mana.ShardG:
			plain, snowBucket = &spend.green, &spend.snowGreen
		case mana.ShardC:
			plain, snowBucket = &spend.colorless, &spend.snowColorless
		default:
			return nil, false
		}
		switch {
		case *plain > 0:
			*plain--
		case *snowBucket > 0:
			*snowBucket--
		case snowAny && s != mana.ShardC && spend.takeAnySnow():
		default:
			return nil, false
		}
	}

	for _, s := range snow {
		var bucket *int
		switch s {
		case mana.ShardW:
			bucket = &spend.snowWhite
		case mana.ShardU:
			bucket = &spend.snowBlue
		case mana.ShardB:
			bucket = &spend.snowBlack
		case mana.ShardR:
			bucket = &spend.snowRed
		case mana.ShardG:
			bucket = &spend.snowGreen
		case mana.ShardC:
			bucket = &spend.snowColorless
		default:
			return nil, false
		}
		if *bucket == 0 {
			return nil, false
		}
		*bucket--
	}

	generic := cost.Generic()
	for generic > 0 {
		switch {
		case spend.colorless > 0:
			spend.colorless--
		case spend.snowColorless > 0:
			spend.snowColorless--
		case spend.white > 0:
			spend.white--
		case spend.snowWhite > 0:
			spend.snowWhite--
		case spend.blue > 0:
			spend.blue--
		case spend.snowBlue > 0:
			spend.snowBlue--
		case spend.black > 0:
			spend.black--
		case spend.snowBlack > 0:
			spend.snowBlack--
		case spend.red > 0:
			spend.red--
		case spend.snowRed > 0:
			spend.snowRed--
		case spend.green > 0:
			spend.green--
		case spend.snowGreen > 0:
			spend.snowGreen--
		default:
			return nil, false
		}
		generic--
	}

	*p = spend
	return p.takeSpent(), true
}

// takeAnySnow spends one snow mana of any type, colorless first, and reports
// whether the pool had one.
func (p *Pool) takeAnySnow() bool {
	for _, bucket := range [...]*int{&p.snowColorless, &p.snowWhite, &p.snowBlue, &p.snowBlack, &p.snowRed, &p.snowGreen} {
		if *bucket > 0 {
			*bucket--
			return true
		}
	}
	return false
}

// merge adds every mana in o to p, snow mana staying snow -- DrainMana's
// DrainMana$ True moving a drained pool into the activator's own.
func (p *Pool) merge(o Pool) {
	p.white += o.white
	p.blue += o.blue
	p.black += o.black
	p.red += o.red
	p.green += o.green
	p.colorless += o.colorless
	p.snowWhite += o.snowWhite
	p.snowBlue += o.snowBlue
	p.snowBlack += o.snowBlack
	p.snowRed += o.snowRed
	p.snowGreen += o.snowGreen
	p.snowColorless += o.snowColorless
	// The drained units keep their sources, after p's own.
	p.units = append(p.units, o.units...)
}
