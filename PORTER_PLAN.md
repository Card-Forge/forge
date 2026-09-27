# Porter plan: RollPlanarDice (ADR-0029, 4th of Planechase bundle)

Assigned: `RollPlanarDice` (RollPlanarDiceEffect.java, PlanarDice.java).

Steps:

1. Research Java: RollPlanarDiceEffect.java, PlanarDice.java, Player.java planar die ability, triggers fired.
2. `crucible/internal/engine/rollplanardiceeffect.go`: single roll, `g.rand.Int32n(6)` (no +1) -> face; Planeswalk face
   -> reuse Planeswalk port's planeswalk path; Chaos face -> `g.checkChaosEnsuesTriggers`; fire PlanarDice / RolledDie
   triggers as far as Crucible wires them (reproduce Result=0 quirk). Reject extra-roll replacement shapes.
3. Registry regen, enginelint, tests in package engine_test.
4. Docs: `effects-rollplanardice.md`, index row in `game-state.md`, counts in CLAUDE.md / plan.
5. Gates full, commit, delete this plan in final commit.
