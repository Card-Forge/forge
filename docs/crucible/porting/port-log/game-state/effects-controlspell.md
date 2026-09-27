# Port Log — Game State: M6 Effects: ControlSpell

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `controlspelleffect.go`, `game.go` (`Move`,
  `MoveToLibraryTop`), `changetargetseffect.go`
- **Java:** `forge-game/src/main/java/forge/game/ability/effects/ControlSpellEffect.java:54-101`

## ControlSpell lands

CR 110.2a: an effect can change a spell's controller. The new controller resolves it — every "you" in its text, its
`SubAbility$` chain, each chosen Charm mode — and a permanent spell enters under their control (CR 608.3a).

The `effects-batch-b.md` deferral no longer holds: `targetChoiceFor` offers stack spells for `TargetType$ Spell`
(`stackSpellCandidates`, `targeting.go`), and `castInstantOrSorcery` casts Instants/Sorceries through the stack
(ADR-0018). No ADR needed: no documented contract fixes a stack item's controller once pushed.

### What changes, per spell

Java changes the spell's controller in two places; this port stores it in four.

| Where                               | Change                                                           | Java                                                                                   | Reason                                                                                                                               |
| ----------------------------------- | ---------------------------------------------------------------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Stack item `Ability.Controller`     | set to new controller                                            | `SpellAbilityStackInstance.setActivatingPlayer` (`SpellAbilityStackInstance.java:170`) | read at resolution; `resolveSubAbility`/`resolveAdditional` build each child from it, so the sub chain follows with no extra write   |
| Each `Ability.Modes[i].Controller`  | set to new controller, `Modes` copied first                      | same, trickling into `subInstance`                                                     | `chooseCharmModes` stores a controller per mode (`charmeffect.go:59`); `Game.Clone` shares the `Modes` backing array                 |
| Card `tempControllers`              | `ControlEffect{Timestamp, Controller}` appended                  | `Card.addTempController` (`ControlSpellEffect.java:98`)                                | `permanentEffect` moves the card to `a.Controller`'s battlefield; `Card.Controller()` must agree, and a temp entry rides along there |
| `Move`/`MoveToLibraryTop` off Stack | `tempControllers = nil` beside the existing `controller = Owner` | `GameAction.java:651-654` `clearControllers`                                           | otherwise a stolen instant in its owner's graveyard still reports the thief, and so does its next cast                               |

Not `changeControllerAt` for the spell: summoning sickness and removal from combat are permanent semantics (CR 302.6,
506.4). The exchanged host does use it.

### Shapes

| Param                              | Resolution                                                                                                                                                                                                                                    |
| ---------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ValidTgts$` + `TargetType$ Spell` | each targeted card on the stack (`spellItemOf`)                                                                                                                                                                                               |
| `Defined$ Targeted`                | same list, a sub-ability carrying its parent's `Targets` (`getAllTargetChoices`, `AbilityUtils.java:1261-1275`)                                                                                                                               |
| `Defined$ TriggeredSpellAbility`   | `triggered.spellAbility`, the Mode$ SpellCast record; named even after the spell left the stack                                                                                                                                               |
| `Mode$ Gain`                       | spell to `NewController$`'s first player (`definedPlayers`), else a player target when targeting, else the activator (`getDefinedPlayersOrTargeted`)                                                                                          |
| `Mode$ Exchange`                   | host skipped unless on the battlefield, phased in, spell still on the stack, spell's controller still in the game (`ControlSpellEffect.java:74-81`); host to the spell's controller, spell to the host's controller, one timestamp (`:69-90`) |
| `Remember$`                        | host (Exchange), then the spell, on the host's `Memory`                                                                                                                                                                                       |

`runChangeControllerCommands` (`:88`, `:96`) has nothing to run: the one change-controller command this port has, losing
the Ring-bearer designation, is already in `changeControllerAt`.

### Rejected before acting (PORT-8, GO-7)

| Shape                                                    | Reason                                                                                                                                                                                           |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `RememberTargets$`                                       | `AbilityUtils.java:1517`'s generic pre-resolve remember; not run by this port                                                                                                                    |
| `TargetValidTargeting$`                                  | `targetChoiceFor` reads it for ChangeTargets only                                                                                                                                                |
| `DefinedExchange$`                                       | an exchange object other than the host; no corpus line                                                                                                                                           |
| `Condition$`, `ConditionDefined$`                        | `subAbilityConditionMet` reads `ConditionDefined$` as never met (`isPresentMatches`, `trigger.go`): a silent skip                                                                                |
| missing `Mode$`; `Mode$` other than `Gain`/`Exchange`    | Java NPEs on the missing param (`ControlSpellEffect.java:58`) and reads any other value as Gain; every corpus line says `Gain` or `Exchange`, so an unknown value is refused rather than guessed |
| `Defined$` other than `Targeted`/`TriggeredSpellAbility` | `getDefinedSpellAbilities` shapes not built (`Remembered`, `ValidStack`, ...)                                                                                                                    |
| `CantGainControl` static in play, Exchange mode          | `Card.canBeControlledBy`'s static half                                                                                                                                                           |
| Gain mode, spell no longer on the stack                  | Java dereferences the null stack instance (`ControlSpellEffect.java:99`); no corpus chain reaches it, every Gain spell being a target its parent fizzles without                                 |

`ChangeTargets` now rejects `ConditionDefined$` too (`changeTargetsUnresolvedParams`): Perplexing Chimera's retarget is
gated on `ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ GE2`, which read as never met and
skipped silently.

### Per card

| Card                | Status                                                                                                                                                                 |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Aethersnatch        | Full, retarget included                                                                                                                                                |
| Commandeer          | Full (alternative cost `ExileFromHand` aside)                                                                                                                          |
| Invert Polarity     | Win branch full; lose branch fails closed in `Counter` (`Defined$` rejected, `countereffect.go`)                                                                       |
| Perplexing Chimera  | Exchange resolves; the chained `ChangeTargets` then fails closed on `ConditionDefined$`                                                                                |
| Sudden Substitution | Fails closed: `ConditionDefined$` on the ControlSpell line; also a `SubAbility$` with its own `ValidTgts$` and `NewController$ Player.IsRemembered` (`definedPlayers`) |
| Chef's Kiss         | Never cast: `TargetType$ Spell.numTargets EQ1` takes `targetChoiceFor`'s literal-`Spell` branch, which has no candidates; its chain also needs `RandomTarget$`         |

Unblocking item for Chimera and Sudden Substitution: `ConditionDefined$` in `subAbilityConditionMet`
(`SpellAbilityCondition.areMet`'s `getDefinedCards` count). Shared by ~485 corpus lines of the
`ConditionDefined$ Remembered | ConditionPresent$ Card` shape, so it is its own change, not this one.

### Tests

`spellcontrol_test.go`; scenario `aethersnatch-stolen-creature-spell-enters-under-the-thief` (Bears enter on ai's
battlefield, `Owner:human`).
