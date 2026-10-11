# Port Log — Game State: M5 Control Change, batch K

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)
- **Follows:** [`m5-control.md`](m5-control.md)

Activation order (CR 602.2b), sub-ability target re-check (CR 608.2b), the last `LoseControl$` tokens, host-bound
durations on Pump/Animate/Goad, delayed `ChangesZone` watches for bounce and library moves, and the pieces of Krovikan
Vampire.

## Targets before costs (`activateability.go`, `chaintargets.go`)

| Java                                                                      | Go                                                                                                                |
| ------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `PlaySpellAbility.java:675-683`: announce X, `setupTargets`, then payment | `ActivateAbility` calls `chooseAbilityTargets` after cost feasibility and the cost-card picks, before any payment |
| `MagicStack.hasFizzled` recursion into `getSubAbility()` (`:745-751`)     | `dropIllegalTargets` walks `Ability.chainTargets`, stamps kept per link                                           |

`chooseAbilityTargets` is Charm modes, `resolveTargets` and `resolveChainTargets`, shared with `pushTriggeredAbilities`;
`Ability.targetsChosen` stops the second choice. An activated ability with no legal target now pays nothing: the
regression is `TestActivateAbilityWithNoLegalTargetPaysNothing` (`{T}`, `PayLife`, `Sac<1/CARDNAME>`). A scenario cannot
express it: the harness errors on a refused activation. `activate-legal-target-*` pin the paying side.

The re-check shares one running fizzle flag across the chain, as Java does: a legal target on any link keeps the whole
ability from fizzling, and every illegal one is removed. Charm modes' and copies' links:
[`m5-t-sneak-charm.md`](m5-t-sneak-charm.md).

## `LoseControl$` (`controlcommands.go`)

| Token                   | Java                                                         | Go                                                                          |
| ----------------------- | ------------------------------------------------------------ | --------------------------------------------------------------------------- |
| `StaticCommandCheck`    | `Card.staticCommandList`, `GameAction.java:1180-1198`        | `Card.staticCmds` of `staticCheck`, `runStaticCommands` after the layers    |
| `UntilSourceUnattached` | `unattachCommandList`, run by `unattachFromEntity` (`:3995`) | `Card.unattachCmds`, run by `Game.Unattach`; also leaves-play and phase-out |

`staticCheck` keeps the host's `Amounts`, the SVar name and the compare string: the left SVar is evaluated with the
affected card as source, the operand on the host. Missing `StaticCommandCheckSVar$`/`StaticCommandSVarCompare$` is an
error (Java reads a null SVar). `UntilSourceUnattached` reads the attachment from the trigger's `Source`
(`triggeredObjects.source`); `Mode$ Attached` is ported ([`m5-q-attached.md`](m5-q-attached.md)), so Eriette reaches it;
an ability without a source errors. The unattach list is pinned by an internal test (`unattachcommand_internal_test.go`,
TEST-2 row 3).

## `Duration$ AsLongAsControl` / `UntilLoseControlOfHost` on Pump, Animate, Goad

`registerHostBoundEnd` is `addUntilCommand`'s host branches (`SpellAbilityEffect.java:1007-1013`), shared with Effect
cards; `validHostDuration` is `checkValidDuration`. New command kinds `commandEndPump`, `commandEndAnimate`,
`commandEndGoad` end the record by `(card, timestamp)`. Pump and Animate check validity first (`PumpEffect.java:271`,
`AnimateEffect.java:34`); `GoadEffect` never does, so a host that already left leaves the goad in place (Java quirk,
reproduced). `goad.Timestamp` is new. Corpus: Pump 6 lines, Goad 1 (Vislor Turlough), Animate 3 (Quicksmith Rebel/Spy's
`Abilities$` and Welcome to . . .'s `TargetMax$ OneEach`, both in
[`m5-t-triggers-animate.md`](m5-t-triggers-animate.md)).

## Delayed and static `ChangesZone` (`leftto.go`, `turnbegin.go`, `statictrigger.go`)

| Piece                                                    | Go                                                                                         |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Leaves to hand or library (CR 603.6d)                    | `checkLeftToTriggers` (own, other watchers, delayed watches); `moveByEffect` calls it      |
| Delayed watch on a bounce                                | `checkReturnedTriggers` is `checkLeftToTriggers(Hand)`, so `returnCards` fires it as well  |
| `Origin$ Graveyard` line, dest not battlefield/graveyard | `checkLeftGraveyardTriggers`, from `moveByEffect`                                          |
| `Static$ True` on dies, exile, hand, library, ETB, those | `Ability.staticTrigger`; `pushTriggeredAbilities` resolves them inline first               |
| `Mode$ TurnBegin` (9 corpus lines, all static resets)    | `checkTurnBeginTriggers`, in `advanceStep` as the turn is handed over (`PhaseHandler:524`) |
| `ThisTurnEnteredFrom_<Zone>` (`CardProperty.java:954`)   | `Card.zoneEntered/zoneEntryTurn/zoneEntryFrom`, set by `Move`/`MoveToLibraryTop`           |

Krovikan Vampire's chain is now whole: static dies trigger remembers, static `Origin$ Graveyard` trigger forgets, leaves
clears, `TurnBegin` clears, `CheckSVar$ X` over `Remembered$Amount` (already read by `contextValue`), and
`ChangeZoneAll`'s `ForgetOtherRemembered$` (already ported). Fixtures `control-krovikan-vampire-*` (2).

Every trigger mode sets `staticTrigger` for a `Static$ True` line, so it runs inline
([`smoke-probe.md`](../smoke-probe.md)). The moved card's own `Origin$ Graveyard` triggers, graveyard exits outside
`moveByEffect` and battlefield-to-library redirects: [`m5-t-triggers-animate.md`](m5-t-triggers-animate.md).

## Fixtures and tests

Scenarios: `activate-legal-target-pays-and-destroys-royal-assassin`,
`activate-legal-target-pays-mana-and-destroys-rathi-assassin`, `control-krovikan-vampire-*` (2). Module tests:
`activatetargetsfirst_test.go`, `chainfizzle_test.go`, `hostboundeffects_test.go`, `delayedleaves_test.go`,
`statictriggersites_test.go`, `unattachcommand_internal_test.go`.
