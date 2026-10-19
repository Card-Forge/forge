# Port Log — Game State: CantSacrifice, CantPayLife, CantLoseLife

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `staticability.go` (`cantSacrifice`,
  `cantLoseLife`, `cantPayLife`, `causeMatches`), `sacrificeeffect.go` (`sacrificeCardsFor`)

`StaticAbilityCantSacrifice` and `StaticAbilityCantGainLosePayLife` are read where Java reads them:

| Reader | Stops | Java |
| --- | --- | --- |
| `cantSacrifice(card, effect, cause)` | a sacrifice effect's candidates and the card itself; a Sac cost's picks and `Sac<1/CARDNAME>`; `sacrificeCardsFor` as a backstop | `Card.canBeSacrificedBy` |
| `cantLoseLife(pid)` (CantLoseLife, CantChangeLife) | LoseLife effects, damage's life loss, `RLoseLife` replacements | `Player.canLoseLife` |
| `cantPayLife(pid, effect, cause)` (+ the two above) | PayLife activation costs, unless costs, a Phyrexian mana payment | `Player.canPayLife` |

`ForCost$ True` limits a line to costs, `False` to effects (`effect` says which). `ValidCause$` is matched by
`causeMatches` against the kind of ability (`Spell`, `Activated`, `Triggered`, `SpellAbility`, with `ManaAbility` and
`YouCtrl`/`OppCtrl` properties): a payment knows its kind (a cast in progress is a Spell, an activation Activated), a
sacrifice effect knows its ability's controller but not its kind, so `Triggered` causes (The Master, Multiplied) are not
matched. A sacrifice with no cause leaves a `ValidCause$` line unapplied (GO-7).

Not ported: statics granted by `Animate` (Jon Irenicus's `staticAbilities$ SCantSac`); `Life` loss through
lifelink-style paths other than the four above, `LifeExchange`/`LifeSet` honoring `canLoseLife`. Tests:
`staticrestrictions_test.go`.
