# Effects batch A: OpenAttraction and AssembleContraption land

Two script-driven `ApiType`s resolve, 154 of the corpus's 203. Corpus lines: OpenAttraction 20, AssembleContraption 23.
Five more APIs in this batch's assignment (Phases 72, RingTemptsYou 49, Planeswalk 30, Abandon 21, ChangeText 17) were
researched and deferred — table below. ChangeTargets, also assigned, lands in
[`effects-changetargets.md`](effects-changetargets.md).

## OpenAttraction

`openattractioneffect.go` ports `OpenAttractionEffect.java`'s resolve in full for its own real corpus shapes: each
`Defined$`/targeted player (default `You`) puts the top `Amount$` (default 1, a plain integer or named `SVar`) cards of
their own `AttractionDeck` zone onto the battlefield, `Remember$`-ing each one on the source's `Memory` when asked. A
player who has lost is skipped (`isInGame()`'s own reading, already how `targetedOrDefinedPlayers` filters). An empty
deck stops that player's own loop early rather than erroring — `OpenAttractionEffect.java`'s own
`if (attractionDeck.isEmpty()) continue`.

No real corpus line (20 total) carries a param this port needs to reject: every one is either bare or `Amount$ 2`. The
move itself reuses `moveByEffect` (`zonemove.go`), which already runs the general enters-the-battlefield replacement
check and ETB triggers; the batch-level `Mode$ ChangesZoneAll` check runs once after the whole loop,
`changezonealleffect.go`'s own shape, keyed by origin `AttractionDeck`/destination `Battlefield` regardless of which
player's own deck a card came from (`CardZoneTable`'s own per-type-pair keying, not per-player).

Not modeled: `ReplacementType.OpenAttraction`-shaped replacement effects (`R:Event$ OpenAttraction`, 0 real corpus
lines) — this port's replacement dispatch has no `OpenAttraction` case, so a future card defining one would silently not
apply; safe today because none exists.

## AssembleContraption

`assemblecontraptioneffect.go` ports `AssembleContraptionEffect.java`'s resolve for its own dominant shape (20 of 23
real corpus lines): the ability's own Controller (`DefinedAssembler$`'s default, `host.isCreature() ? "Self" : "You"`,
and its two real explicit values, `Self`/`You`, all three of which resolve to `a.Controller` here — the assembling
permanent's controller and the ability's own Controller are the same card at every real call site, an ETB/damage/cast
trigger on the assembler itself or the casting player's own spell) puts the top `Amount$` (default 1) cards of their own
`ContraptionDeck` onto the battlefield, each freshly dialed to a sprocket (1, 2 or 3) via a new `ChooseNumber(1,3)` call
— reusing the existing `PlayerController.ChooseNumber` decision rather than adding a `ChooseSprocket` one, since its
`(lo, hi int) int` contract already fits (`control.go`'s own `ChooseNumber`, untouched).

New engine state: `Card.Sprocket int` (`card.go`), the Contraption's own dial. Zero for a card that has never been
assembled. `Game.Move`'s own battlefield-leaving reset (both branches: leaving via `Move` and via `MoveToLibraryTop`,
`game.go`) clears it alongside `Tapped`/`Exerted`/the rest of that same reset list — CR 725.4a: a Contraption
reassembled later rolls a fresh dial, it does not remember its last one. `Game.Clone` needs no change: `Sprocket` is a
plain `int` copied by `copy(out.cards, g.cards)`'s own whole-struct value copy before the deep-copy loop runs
(`game.go`), the same free ride every other plain scalar `Card` field already gets.

Rejected, GO-7 (0-2 real corpus lines each): `DefinedContraption$`/`Reassemble$` (a different shape — rewiring one
already-assembled Contraption's own dial rather than assembling a new one from the deck,
`AssembleContraptionEffect .java`'s own separate branch); `DefinedAssembler$ ReplacedCause` (1 real line,
`wrench_rigger.txt`-style Rigger's own `R:Event$ AssembleContraption | ReplaceWith$ AssembleTwo` — this port's
replacement dispatch has no `AssembleContraption` case, so the assembler named by the replaced cause is unreachable);
`Amount$ Result` (2 real lines, a magic-string reference to a preceding roll/damage result this port's
`resolveNamedAmount` does not special-case).

## Researched and deferred

| API | Blocker |
| --- | --- |
| `Phases` | CR 702.26b: a phased-out permanent is treated as though it doesn't exist. Java gets that free from `getCardsIn(Battlefield)` excluding it; this port's `Zone(Battlefield, pid).Cards()` is called directly by targeting, combat and every SBA walk, none of which know to exclude a `PhasedOut` flag. `destroyeffect.go:101` already names phasing as not ported. A flag nothing reads is silently wrong: the card still attacks, blocks and gets targeted |
| `RingTemptsYou` | `Player.createTheRing`/`setRingLevel` (`Player.java`) build a per-player Ring whose granted abilities grow with level — a synthetic Command-zone object with its own triggers, the same shape `BecomeMonarch`/`TakeInitiative` were deferred for. `Mode$ RingTemptsYou` (9 real corpus lines) is also a new trigger mode this port's trigger.go has no dedicated `check*Triggers` walk for |
| `Planeswalk` | Every reachable branch of `PlaneswalkEffect.java` requires `game.getActivePlanes() != null` — a Planechase game. This port has no active-planes state at all (`effecthelpers.go:99` excludes `PlanarDeck` from the zones a card can be moved to by this port; `RollPlanarDice` is unregistered) and no card sits in a real deck without one, so every real corpus line resolves the identical always-no-op branch Java itself takes for a non-Planechase game — registering this API would count it "resolved" for a game mode this port cannot run at all |
| `Abandon` | The zone move itself (`Command` → `SchemeDeck`, `RememberAbandoned$`, `Optional$` via `ConfirmEffect`) is routine, and re-evaluating the moved card's own triggers needs no code (this port's triggers already walk `TriggerZones$` off the card's current `Zone` rather than an explicit register/clear step, `trigger.go`'s own `phaseTriggerZoneMatches`). The blocker is `Mode$ Abandoned` (1 real corpus line): a new trigger mode with no `check*Triggers` walk built for it yet — the "new trigger mode" line this batch's routine effects stay clear of |
| `ChangeText` | CR 612 text-changing substitutes words across a card's own printed abilities/keywords/valid strings, re-derived continuously (color-word and type-word swaps read back by Layer 3/4/6 evaluation). Card scripts compile once into an immutable AST (PORT-2); `layer.go`'s own `LayerText` is a named constant with no application function reading it anywhere in this port — there is no hook to land a rules-text substitution on without building one |

No Forge bugs found researching this batch's ported or deferred APIs.
