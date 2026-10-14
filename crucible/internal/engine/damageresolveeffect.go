package engine

//enginelint:allow ability control game effecthelpers trigger card zone id replacedying

// damageResolveEffect is DamageResolveEffect.java: the damage a DamageMap$
// ability recorded (pendingDamage) is dealt all at once -- GameAction.
// dealDamage over the whole map, with this ability as the cause, so its own
// RememberDamaged$ has the host remember what the host's damage reached
// (GameAction.java:2787-2794) -- then the damage-done-once triggers check the
// batch, then ReplaceDyingDefined$ makes its effect (replaceDying,
// DamageResolveEffect.java:30). Without a map it does nothing, as in Java.
type damageResolveEffect struct{}

func (damageResolveEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "DamageResolve", "Condition"); err != nil {
		return err
	}
	if err := checkReplaceDying(g, a, "DamageResolve"); err != nil {
		return err
	}
	if a.damageMap == nil {
		return nil
	}
	entries := a.damageMap.entries
	a.damageMap.entries = nil
	table := g.dealPendingDamage(controller, entries)
	if hasParam(a, "RememberDamaged") {
		rememberDamaged(g, a.Source, table)
	}
	g.checkDamageTableTriggers(controller, table, false)
	return g.replaceDying(a, "DamageResolve")
}

// rememberDamaged is GameAction.dealDamage's RememberDamaged$
// (GameAction.java:2790-2794): the host remembers every entity its own
// damage reached in the batch, in the order dealt. A recorded entry is
// damage actually dealt, so an entity whose damage was prevented in full is
// not remembered, as Java's runReplaceDamage removes it from the table
// (ReplacementHandler.java:526-527).
func rememberDamaged(g *Game, host CardID, table damageTable) {
	for _, e := range table {
		if e.Source == host && e.Amount > 0 {
			g.Card(host).Memory.Remember(e.Target)
		}
	}
}

// dealPendingDamage deals each recorded entry in order -- the loop of
// GameAction.dealDamage over a CardDamageTable (GameAction.java:2742-2764),
// noncombat -- and returns what was actually dealt. Deathtouch is read off
// each entry's own source; a card no longer on the battlefield takes none.
func (g *Game) dealPendingDamage(controller PlayerController, entries []pendingDamageEntry) damageTable {
	var table damageTable
	for _, e := range entries {
		src := g.Card(e.source)
		deathtouch := src.HasKeyword("Deathtouch")
		if id, ok := e.target.AsCard(); ok {
			if g.Card(id).Zone != Battlefield {
				continue
			}
			g.dealPermanentDamage(controller, e.source, id, e.amount, deathtouch, false, &table)
			continue
		}
		if p, ok := e.target.AsPlayer(); ok {
			g.dealPlayerDamage(controller, e.source, p, e.amount, false, &table)
		}
	}
	return table
}

// pendingDamage is a CardDamageTable waiting to be dealt: each entry one
// source dealing an amount to a target, in the order recorded.
type pendingDamage struct {
	entries []pendingDamageEntry
}

type pendingDamageEntry struct {
	source CardID
	target EntityID
	amount int
}

// add records amount more damage from source to target; the table sums
// repeated pairs (CardDamageTable is a Guava table keyed by the pair).
func (m *pendingDamage) add(source CardID, target EntityID, amount int) {
	if amount <= 0 {
		return
	}
	for i := range m.entries {
		if m.entries[i].source == source && m.entries[i].target == target {
			m.entries[i].amount += amount
			return
		}
	}
	m.entries = append(m.entries, pendingDamageEntry{source: source, target: target, amount: amount})
}
