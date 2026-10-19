# Effects batch B: Regeneration lands

One of this batch's seven assigned `ApiType`s (`ControlPlayer` 11 corpus lines, `ChaosEnsues` 11, `Meld` 7,
`ControlSpell` 6, `UnlockDoor` 4, `Subgame` 3, `Regeneration` 3) actually needed new code; the other six were researched
and deferred — table below. `Regeneration` is now registered in `NewRegistry` (159 of the corpus's 203 script-driven
`ApiType`s resolve) and its 3 real corpus lines resolve end to end.

## Regeneration

`RegenerationEffect.java` is CR 701.16's own regeneration action (heal damage, tap, remove from combat) — distinct from
`RegenerateEffect.java` (`regenerateeffect.go`), which only grants the shield a later destruction spends. `Regeneration`
itself has no `AB$`/`SP$` corpus line at all: every one of the corpus's 3 real `DB$ Regeneration` lines
(`mossbridge_troll.txt:5-6`, `knight_of_the_holy_nimbus.txt:6-7`, `clergy_of_the_holy_nimbus.txt:6-7`) is reached only
through a permanent's own always-on replacement,
`R:Event$ Destroy | ActiveZones$ Battlefield | ValidCard$ Card.Self | Regeneration$ True | ReplaceWith$ <SVar>` naming
`DB$ Regeneration | Defined$ ReplacedCard` — a `Destroy`-event replacement shape `replacement.go` does not resolve (only
`Moved`, `Untap`, `DamageDone`, `Draw` and `GainLife` are; the shield mechanism, `Game.regenerate`, was the only
`Destroy`-type replacement this port modeled before this batch).

`regeneration.go` now resolves that shape end to end:

- `destroyReplacedByRegeneration` walks `host.Def.Faces[*].Replacements` the identical way `checkMovedReplacement` does,
  looking for a match, and — once found — builds a synthetic `*Ability` from the `ReplaceWith$` SVar and calls
  `regenerationEffect{}.Resolve` (`regenerationeffect.go`) directly, as a value rather than through `Registry.Resolve`.
  The call sites that decide a destruction (`destroyEffect`, `destroyAllEffect`, `destroyDamagedCreatures`) run outside
  any `Registry.Resolve` call — a fresh game's very first state-based-action check, before any ability has ever
  resolved, is one of them — so `Game.registry` (`game.go`) cannot be relied on to be set yet, the identical reason
  `drawReplaced`'s/`gainLifeReplaced`'s own `applyDrawReplacement`/`applyGainLifeReplacement` (`replacement.go`)
  hand-run their own `Event$`s' `ReplaceWith$` targets. Calling the real registered effect directly (rather than a
  second hand-rolled copy of its body) means the registered API and the one the corpus's own 3 real lines run are the
  same code.
- `regenerationReplacementMatches` is the replacement match itself: `Event$ Destroy`, `Regeneration$ True`, host inside
  one of the replacement's own `ActiveZones$` (`hostInActiveZones`, `replacement.go`), `ValidCard$` matched against
  host, `replacementRequirementsCheck` (`replacement.go`). Any param past the corpus's own real six (`Event`,
  `ActiveZones`, `ValidCard`, `Regeneration`, `ReplaceWith`, `Description`) refuses the whole line (GO-7) — none exist
  today, so this allow-list is exhaustive against the real corpus, not aspirational. The `ReplaceWith$` target itself is
  recognized only when it names `DB$ Regeneration` with no `SubAbility$` of its own (0 real lines chain one; PORT-8/GO-7
  refuses rather than drops a chain silently) — anything else is left unmatched.
- `regenerationEffect.Resolve` (`regenerationeffect.go`) reads `Defined$`: `ReplacedCard` (100% of the corpus's 3 real
  lines) — Java's own "the object the replacement is actually about" — and `Self` (0 real lines, but the identical
  substitution `AbilityUtils.getDefinedCards` would make for it, since every real line's own `ValidCard$ Card.Self`
  already means the two resolve to the same card) both resolve to `a.Source`; absent, it defaults to `Self`
  (`targetedOrDefinedCards`'s own convention). Any other `Defined$` value, or any other param, is rejected (GO-7).
- `cardCantRegenerate` is a new `cantBlockBy`-shaped scan (`staticability.go`'s own `Game.traitHosts` walk applied to a
  different `Mode$`): `Mode$ CantRegenerate` — the disruption half of
  `knight_of_the_holy_nimbus.txt:8-9`/`clergy_of_the_holy_nimbus.txt:7-8`'s own opponent-only "{N}: CARDNAME can't be
  regenerated this turn," created as an effect card's `StaticAbilities$` trait (`effecteffect.go`) — blocks both
  regeneration sources, checked in `Game.regenerate` before either. Without it, an opponent paying for that ability
  would have no effect: these two cards would regenerate every time regardless, an actively wrong answer (Knight/Clergy
  of the Holy Nimbus effectively indestructible), not merely an incomplete one, since `AB$ Effect | StaticAbilities$`
  was already resolved by `effecteffect.go` before this batch and nothing consulted the trait it built.
- `Game.regenerate` (the shield-spending entry point every destroy call site already calls — `destroyeffect.go:86`,
  `destroyalleffect.go:90`, `action.go:341`) checks `cardCantRegenerate` first, then the static replacement (it costs
  nothing); only when neither applies does a shield actually get spent. A corpus with no card combining a shield and
  this replacement on one permanent cannot tell the replacement-before-shield order apart from the reverse empirically,
  but checking the free one first is the more useful of the two. `regenerateBody` is the three-part action
  (`c.Damage.Clear()`, tap with `checkTapsTriggers`, `removeFromCombat`) both sources share.

No changes to `destroyeffect.go`, `destroyalleffect.go` or `action.go` were needed: all three already call
`Game.regenerate` for every real destruction, so folding the new checks into that one function reaches every real call
site for free.

## Researched and deferred

| API | Blocker |
| --- | --- |
| `ChaosEnsues` | Not Archenemy — `ChaosEnsuesEffect.java` is Planechase's own CR 311.7 chaos-ability trigger: its first real line is `if (game.getActivePlanes() == null) return` (Planechase-only), and its `Defined$` branch widens a plane card's own trigger active zones into the planar deck it's revealed from — non-`Battlefield` zone scanning. Identical blocker `Planeswalk` was already deferred for (`effects-batch-a.md`): this port has no active-planes state at all, and registering the API would count it "resolved" for a game mode this port cannot run |
| `ControlPlayer` | Mindslaver's own shape: "player B's decisions are made by A's controller for B's next turn," scheduled via `game.getBeginOfCombat()/getCleanup().addUntil(...)` (a delayed hook at a future phase boundary) and applied through `Player.addController`/`removeController` — Java routes every decision Player B's own `PlayerController` would otherwise answer through Player A's controller instead for the duration. This port's `control.go` explicitly "carries no per-player state" (`PlayerController` methods take the deciding player as an explicit `PlayerID` rather than binding to one seat) — there is no per-player controller-routing seam a control-changing effect could redirect, and no phase-scheduled delayed-hook mechanism either (`stack.go`'s own doc comment: "casting still has no cost-payment or targeting to drive that," the identical class of gap) |
| `Meld` | CR 712's own two-cards-become-one-permanent mechanic: `Card.changeToState(CardStateName.Meld)`/`setMeldedWith`/`PlayerZoneBattlefield.addToMelded` track a second physical card riding along with the melded permanent through every future zone change (CR 712.4c). This port's `Card` has no melded-companion field and no meld-aware move path — building one is a new card-state concept, not a state change over pieces this port already has |
| `ControlSpell` | Same blocker as `Play`/`CopySpellAbility` (deferred in the prior batch, `effects-play-copyspellability.md`): the corpus's own dominant shape targets an instant/sorcery spell on the stack (`sa.getTargetRestrictions().setZone(Stack)`) and reassigns `SpellAbilityStackInstance.setActivatingPlayer`. This port's stack (`stack.go`) is `[]Ability` with no targeting path that can name a stack object at all — `resolveTargets` (`targeting.go`) has no `TargetType$ Spell`/`Zone$ Stack` case, and instant/sorcery casting itself does not exist yet (`CastSpell` only casts a permanent or an Aura) — there is no targetable spell for `ControlSpell` to ever act on |
| `UnlockDoor` | CR 719's own Room split-card mechanic: a permanent with two half-faces, each independently locked/unlocked (`Card.lockRoom`/`unlockRoom`/`getLockedRooms`), a `chooseSingleCardState` decision over which half to (un)lock, and text/ability access gated by which half is unlocked. This port's `Card.CurrentRoom` (`card.go:47`) is a dungeon's own venture marker, an unrelated mechanic (CR 309) that happens to share the English word "room" — no locked/unlocked half-state, and no `ChooseCardState`-shaped `PlayerController` decision, exists |
| `Subgame` | CR 720's own "start and finish an entire second game inside this one's resolution" (Mind's Desire-style Class 91.4 subgames): a whole nested `Game`, its own turn loop run to completion before this ability's resolution returns, all zones re-mapped there and back. Same class as `RestartGame`/`Play`'s own recursive-whole-game shapes, not a state change over pieces this port's single top-level `Game` loop already has |

No Forge bugs found researching this batch's ported or deferred APIs.
