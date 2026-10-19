# Port Log — Game State: Layers batch P (CDA hosts, Layer 7a heads, Layer 8 remainder)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`continuous.go`](../../../../../crucible/internal/engine/continuous.go),
  [`continuouslayers.go`](../../../../../crucible/internal/engine/continuouslayers.go),
  [`castoptions.go`](../../../../../crucible/internal/engine/castoptions.go),
  [`castspell.go`](../../../../../crucible/internal/engine/castspell.go),
  [`amountheads.go`](../../../../../crucible/internal/engine/amountheads.go),
  [`amountpaid.go`](../../../../../crucible/internal/engine/amountpaid.go),
  [`attack.go`](../../../../../crucible/internal/engine/attack.go)
- **Java:** `StaticAbilityContinuous.java:322,473-494,531-553,751,892-922`, `StaticAbility.java` (`zonesCheck`),
  `GameActionUtil.java:334-402`, `CostAdjustment.java:82-90`, `CardPlayOption.java:56-77`, `AbilityUtils.java:502-536`
  (`ExiledWith`, `Remembered`), `:2438` (`YourTurns`), `:2580-2640` (`Party`), `CardLists.java:498-518`,
  `AttackRestriction.java:46-96`, `Untap.java:114,158-161`, `Card.java:4699,4995-5000`

Supersedes the "deferred" rows of [`layer7a-cda-amounts.md`](layer7a-cda-amounts.md) and the MayPlay skip table of
[`layers-text-and-rules.md`](layers-text-and-rules.md); those files keep the history of the shapes they landed.

## Off-battlefield hosts: `continuousStaticsAllZones`

`traitHosts` is not widened: about 60 trigger, replacement and cost scans read it, and each would start firing from the
hand and graveyard. `continuousStatics` (battlefield and Command effect/scheme hosts) stays the walk for Layer 2 and
Layer 3. `continuousStaticsAllZones` adds `appendOffZoneStatics`: every `Mode$ Continuous` line of a card in the hand,
library, graveyard, exile or on the stack that functions where it sits (`staticFunctionsOffBattlefield`).

| Line | Functions in | Java |
| --- | --- | --- |
| `CharacteristicDefining$` (any value) | every zone except its `ExcludeZone$` ones | `zonesCheck` returns early for a CDA |
| any other line | the zones `EffectZone$` names (`layerZoneList`) | `StaticAbility.zonesCheck` |

| Applier | Walk | Why |
| --- | --- | --- |
| Types, colors, keywords, rules (Layers 4-6, 8) | all zones | Their effects land on cards of any zone (`forEachOffBattlefieldCard` already clears them) |
| Power/toughness (7a-7c) | all zones; off-battlefield `PT` cleared each pass | A CDA stacks otherwise: Grist is a 1/1 Insect creature in the graveyard, a Tarmogoyf in hand has a value |
| Control (2), text (3), names | battlefield and Command hosts | No real off-battlefield line gives control or text; names are battlefield-only |

Only face 0 of an off-battlefield card is walked (a card outside play shows its front face). A spell is on the stack of
its caster and in the shared `NoPlayer` pool; the pool is read only for cards the per-player walk did not see.
`applyOneCharacteristicDefiningPT` now honors `ExcludeZone$` through `layerAffectedCards`.

Out by design: the 68 `EffectZone$ Command` lines on planes, phenomena and Vanguard avatars (planar and Vanguard play
are not in a constructed deck test), and any non-`Continuous` mode of a card off the battlefield (a `ReduceCost` with
`EffectZone$ Graveyard` is a cost-modification scan over `traitHosts`).

Quirk kept (PORT-7): Layer 8 builds after Layer 7 in a pass, so a CDA that reads a hidden keyword grant
(`Card.hasKeyword...`) sees the previous pass's. A Flame Burst resolving later reads the current one.

## Layer 8: `MayPlay$`

`applyOneContinuousMayPlay` now gates through `layerStaticApplies`, the one `StaticAbility.checkConditions` port: host
zone (`EffectZone$`), `Condition$`, `IsPresent$`/`PresentCompare$`, `TopCardOfLibraryIs$` and the whole `CheckSVar$`
chain (`CheckThirdSVar$` included). No real `MayPlay$` line writes `Phases$`/`PlayerTurn$`/`GameStage$`/`ClassLevel$`,
the keys that gate reports false for.

| Param (real lines, S: + Effect SVar) | Behavior |
| --- | --- |
| `MayPlayIgnoreColor$` (4 + 26) | `anyColorCost`: every single colored or hybrid shard becomes generic, a `{C}` shard still needs colorless. `MayPlayIgnoreType$` wins when both are written (`CardPlayOption.java:69-75`) |
| `RaiseCost$` (17 + 1) | Added on top of the cost, free casts included. A name that is an SVar of the host is generic mana; else `parseUnlessCost` (PayLife, Discard, Sac, ExileFromGrave, RemoveAnyCounter). Mana part joins the cost (`castOpts.raiseMana`), the rest rides `castOpts.extra`. A shape it cannot pay grants nothing |
| `MayPlayText$` (7 + 8) | A label only (`GameActionUtil.java:398`): no behavior |
| `ReplaceGraveyard$ Exile` (2) | `Card.graveyardToExile`: leaving the stack for a graveyard sends the spell to exile (`Game.Move`) |
| `ValidAfterStack$ Spell.<props>` (6 + 1) | Evaluated against the card as the grant is built (Java checks after the spell is on the stack and rolls the cast back); differs only for a mana value reading X |
| `ValidSA$ Spell` (1) | Every cast. `Spell.Blitz`/`Warp`/`Bestow`/`Mutate` (5 + 1) need an alternative cast mode this port cannot make: no grant |
| `MayPlaySnowIgnoreColor$` (1) | Granted: `castOption.snowAnyColor` sets `Game.snowAnyColor` while the cast pays, and `Pool.payWithSnow` lets any snow mana (colorless included) pay a colored pip after the pip's own plain and snow mana (`ManaCostBeingPaid.canBePaidWith`) |
| `EffectZone$ Graveyard`/`Exile` hosts (33) | Walked now (Gravecrawler, Squee, Skaab Ruinator, ...) |
| `EffectZone$ Command` on a plane (2) | Out by design |

Measured over `scenarioDB`: 656 real `MayPlay$` lines (183 `S:`, 473 Effect SVar). 8 still carry a skipped param
(`ValidSA$` past `Spell` 6, `EffectZone$ Command` on a plane 2); every other line is subject to its own
`Condition$`/`Affected$` shapes being ones the engine evaluates.

`MayLookAt$` (108 lines) stays a no-op: the engine is omniscient (`lookateffect.go`), so a permission to look at a
hidden card changes no state.

## Layer 8: `AddHiddenKeyword$`

Every distinct string in the corpus is read now (53 real lines, 10 strings), so no line is skipped for its text.

| String | Reader |
| --- | --- |
| `This card doesn't untap during your next untap step.` | `untapBlocked` (`Card.canUntap`, `Card.java:4699`). The grant is rebuilt from the live static each pass, so an Effect card exiled by its own untap-step trigger releases it, and an Aura (Stunning Strike) keeps it |
| `CARDNAME can't attack alone.` | `attackAloneViolation`: a one-attacker declaration with it is illegal (`AttackRestriction.java:60,94-96`); `can't attack or block alone.` is the same string family |
| `CARDNAME can only attack alone.` | A declaration of more than one attacker with it is illegal (`:49`) |
| `CARDNAME count as <name>.` | The `hasKeyword<line>` valid property, by substring over the hidden lines (`Card.java:4995-5000`); the valid string's `.` split drops the final period. `AffectedZone$ Graveyard` is walked through `layerAffectedCards` |

A printed "can't attack or block alone" creature (K: line) is now held to the attack half too; before only the block
half was enforced.

## Layer 7a: the last 16 dimensions

All 374 dimensions resolve (`cdaCorpusFloor` 363 to 374). New heads, each in `resolveAmount` so every caller gets them:

| Head | Dims | Semantics |
| --- | --- | --- |
| `ExiledWith$<prop>` | 5 | Cards `markExiledWith` listed on this host object, tokens excluded; `CardPower` is the sum of net power, `Colors` the distinct colors |
| `Remembered$<prop>` | 4 | The remembered cards; `Amount`, `CardManaCost` (sum). `...LKI` forms are unresolved |
| `PlayerCountRemembered$LifeTotal` | 2 | Sum of the remembered players' life (`playerXCount`'s `addPlayer`) |
| `$DifferentCardNames` | 2 | `CardLists.getDifferentNamesCount`: Spy Kit cards last, nameless cards count for nothing; `sharesNameWith` uses the DB's non-legendary creature names |
| `Count$YourTurns` | 2 | `Player.Turn`, incremented as a turn begins (`StartTurn`, `advanceStep`), so the current turn counts |
| `Count$Party` | 1 | `partyCount`: one-type creatures, four-type wildcards, then Java's greedy assignment over two- and three-type groups in the order of a 16-bucket `HashMap<String>` (`javaBucket`) |

Also reachable for `CheckSVar$`/`PresentCompare$` operands: `Imprinted$Valid <spec>` (9 lines),
`PlayerCountOpponents$HighestCardsInGraveyard` and `...HighestCounters.Poison`.

`Player.Turn` is not part of the fixture dump (only the game's turn is), so no `expect.state` changed.

## Not resolved, and why

| Shape | Needs |
| --- | --- |
| `ControlOpponentsSearchingLibrary$` (Opposition Agent) | A new decision redirect: the searching player's choices in `ChangeZone` from the library go to another controller (`ChangeZoneEffect.java:1066-1076`, `Player.addController`). ADR-0030/0036 cover turn and declaration redirects, not a per-effect one: **needs an ADR before code** (ADRP-4) |
| `IgnoreEffectCost$ 2` (Leonin Arbiter) | Resolved: ADR-0040, [`m5-s-redirects.md`](m5-s-redirects.md) |
| `ValidSA$` alternative modes | Blitz/Warp/Bestow/Mutate casts |
| `Count$` `...Type` handlePaid properties (`CreatureType`, ...) | `*cardtype.Registry` reachable through the DB; no CDA writes one |

## Forge issues (PORT-8)

Reported, not worked around, none reachable from a real card:

| File:line | Issue |
| --- | --- |
| `StaticAbilityContinuous.java:532-533` | `Iterables.getFirst(..., null)` can hand a null controller to `addControlledWhileSearching`; `addController` then dereferences it |
| `CardPlayOption.java:115-118` | The RaiseCost description sums two `indexOf` results as a substring start; UI text only |
| `ComputerUtilMana.java:618` | The AI payment path never calls `setSnowForColor`, so the AI ignores `MayPlaySnowIgnoreColor$` |

## Tests

`layersp_mayplay_test.go` (IgnoreColor against IgnoreType, RaiseCost in life, mana, SVar and free casts, an unpayable
shape, `ValidAfterStack$`/`ValidSA$`, Kess's exile, Gravecrawler from the graveyard, `IsPresent$`),
`layersp_cda_test.go` (Remembered, ExiledWith, YourTurns, Party table, DifferentCardNames, flash and colors and Grist in
other zones, Brawn in the graveyard), `layersp_hidden_test.go` (attack alone, only alone, untap lock, count-as,
Imprinted/graveyard/poison heads). Scenarios
`mayplay-gravecrawler-casts-from-the-graveyard-while-a-zombie-is-controlled`,
`mayplay-gravecrawler-stays-in-the-graveyard-without-a-zombie`, `mayplay-kess-exiles-the-spell-cast-from-the-graveyard`,
`cda-awakened-amalgam-dies-with-two-distinct-land-names`,
`cda-awakened-amalgam-survives-with-three-distinct-land-names`,
`cda-control-win-condition-is-one-one-on-its-controllers-first-turn`,
`attack-alone-sightless-brawler-must-have-company`, `attack-alone-sightless-brawler-attacks-with-a-friend`.
