package engine

//enginelint:allow id control game zone card

// The CardZoneTable RepeatEachEffect opens for ChangeZoneTable$
// (RepeatEachEffect.java:84-86): the tokens and copies its loop creates are
// held in Game.zoneBatchCreated, and one ChangesZoneAll covers them when the
// loop ends (:202-205). Only TokenEffect (:115-136) and CopyPermanentEffect
// (:113-119, :267-268) read the parent's table; ChangeZoneEffect always builds
// its own (ChangeZoneEffect.java:492), so every other zone change still fires
// its batch at once.

// openZoneBatch starts a batch, restoring an enclosing one when the returned
// function runs, and firing this batch's ChangesZoneAll first. An erroring
// loop fires it too: the error ends the ability, and the tokens did enter.
func (g *Game) openZoneBatch(controller PlayerController) (done func()) {
	outerOpen, outerCreated := g.zoneBatchOpen, g.zoneBatchCreated
	g.zoneBatchOpen, g.zoneBatchCreated = true, nil
	return func() {
		created := g.zoneBatchCreated
		g.zoneBatchOpen, g.zoneBatchCreated = outerOpen, outerCreated
		g.checkChangesZoneAllTriggers(controller, created, None, Battlefield)
	}
}

// checkCreatedZoneAllTriggers is triggerChangesZoneAll for tokens or copies
// just put onto the battlefield: held in the open batch when a RepeatEach
// holds one, else fired now.
func (g *Game) checkCreatedZoneAllTriggers(controller PlayerController, created []CardID) {
	if g.zoneBatchOpen {
		g.zoneBatchCreated = append(g.zoneBatchCreated, created...)
		return
	}
	g.checkChangesZoneAllTriggers(controller, created, None, Battlefield)
}
