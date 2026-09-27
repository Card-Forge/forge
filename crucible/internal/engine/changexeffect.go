package engine

//enginelint:allow ability

// XManaCostPaid is SpellAbility.getXManaCostPaid: the value of X announced
// for a's own mana cost, and whether one was announced at all -- false is
// Java's null, a cost with no X or one not paid (a WithoutManaCost$ cast).
func (a *Ability) XManaCostPaid() (int, bool) {
	return a.xManaCostPaid, a.hasXManaCostPaid
}
