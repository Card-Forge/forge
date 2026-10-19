# Port Log — Game State: cast options

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `castoptions.go`, `castspell.go`
  (`castFromHand`), `continuous.go` (`applyOneContinuousMayPlay`), `castrecord.go`, `foretell.go`, `valid.go`

## One card, several ways to cast it

Java offers each `MayPlay$` grant as its own `SpellAbility` and the player picks (`GameActionUtil`, `CardPlayOption`).
`Game.castOptions` lists the ways `pid` may cast a card from where it is, `castFromHand` takes the only one or asks
`PlayerController.ChooseOption` over their labels, and the choice sets `castOpts` (`withoutManaCost`, `altCost`,
`anyType`). A grant that only adds flash merges into the option it matches; a `MayPlayDontGrantZonePermissions$` grant
is an option only when the card is castable from where it is anyway (the hand, or another grant that gives the zone).

| Source | Option |
| --- | --- |
| the card is in `pid`'s hand | the normal cast, listed first |
| a Layer 8 `MayPlay$` static (`Game.mayPlay`) | `MayPlayWithoutManaCost$`, `MayPlayAltManaCost$`, `MayPlayIgnoreType$`, `MayPlayWithFlash$` |
| `Game.exileGrants` (Airbend, Heist) | `{2}` / any type of mana, from exile |

`applyOneContinuousMayPlay` now reads `MayPlayLimit$` (casts per turn through the static, `Game.mayPlayUses`),
`MayPlayAltManaCost$`, `MayPlayIgnoreType$`, `MayPlayPlayer$` (the grantees, e.g. `Player.Active`), `CheckSVar$` +
`SVarCompare$` and `AffectedZone$ All`; `Affected$` is read from the host controller's point of view. Still ungranted:
`MayPlayIgnoreColor$`, `MayPlaySnowIgnoreColor$`, `RaiseCost$`, `MayPlayText$`, `CheckThirdSVar$`, `IsPresent$`,
`ValidSA$`, `ValidAfterStack$`, `ReplaceGraveyard$`. Mana of any type (`anyTypeCost`) turns every single-mana shard of
the final cost into generic. A face-down exiled card (Heist) is cast face up, and put back face down when the cast does
not happen.

## Count$ThisTurnCast and ControlledBy

`Game.castThisTurn` records each spell cast this turn with its caster and the zone it was cast from;
`Count$ThisTurnCast_<valid>` (about 100 corpus uses) counts the entries matching `<valid>` as their caster controlled
them (`recordSpellCast`, `thisTurnCastCount`). The valid property `ControlledBy <player spec>` matches a card's
controller against a player spec. `wasCast...` properties are not ported.

## Foretell

`Game.Foretell` is the special action (CR 702.143a, `CardFactoryUtil.java:2961`): on your turn, pay {2}, exile the card
from your hand, foretold (`Card.foretold`, `foretoldTurn`; not turned face down, the engine is omniscient). From a later
turn only its owner may cast it from exile for `K:Foretell:<cost>` (`foretellCost`, `GameActionUtil.java:205`); leaving
exile clears the flag. `ActionForetell` and the `foretell` fixture verb drive it. A foretell granted by effect
(`ForetoldCost$`) is not ported.

## ChangeZone from the stack

`Origin$ Stack` resolves for a spell exiling itself (`Defined$ Self`, Mnemonic Betrayal's "Exile CARDNAME"): the spell
leaves the stack mid-resolution and `moveResolvedSpellToGraveyard` finds nothing to move. Any other spell on the stack
fails closed.

Tests: `grantedcast_test.go`, `foretell_test.go`, `mayplay_test.go` (`TestMayPlayHandChoiceOffersBothWays`).

Known limits: `Count$ThisTurnCast_*` matches a card's present state with the cast-time controller, not a full last-known
copy; a condition evaluated for a host whose other ability is resolving reads that ability's references
(`definedPresentMatches` infers the ability from `Game.resolving`); only options playable under their own timing are
offered, so a controller is never asked about a cast that then fails on timing.
