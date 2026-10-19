# Effects batch C: FlipOntoBattlefield lands

One script-driven `ApiType` resolves, 159 of the corpus's 203. Corpus lines: FlipOntoBattlefield 2. Six more APIs in
this batch's assignment (SwitchBlock 2, SetInMotion 2, LosePerpetual 2, ExchangeTextBox 2, ChooseSector 2, ChangeX 2)
were researched and deferred — table below.

## FlipOntoBattlefield

`flipontobattlefieldeffect.go` ports `FlipOntoBattlefieldEffect.java`'s resolve for both of its real corpus lines
(`chaos_orb.txt`, `falling_star.txt`) — the two Un-set "physically flip the card" gonzo effects CR does not otherwise
describe. The controller picks a battlefield permanent as the landing spot (`ChooseCardsForEffect`, no bounding-box
system exists — Java's own `FlipOntoBattlefieldEffect.java:33` TODO says the same); `flipNeighbor` then finds one
neighbor of that spot, replaying `getNeighboringCard(tgtLoc, -1)`'s own logic exactly: an attachment half the time if
the spot carries one (the 50% `hitAttachment` lottery), else the spot's own left neighbor in its controller's
battlefield order — or, when the spot sits at index 0 and a right neighbor exists, that right neighbor instead
(`FlipOntoBattlefieldEffect.java:125-129`'s own "leftmost" quirk, preserved because parity with the shipped card depends
on it, PORT-7). `getNeighboringCard(tgtLoc, +1)` (`java:39`) is not ported: it never draws (the attachment lottery is
gated on `direction < 0`) and its own result is always discarded (`getNeighboringCard` never returns `null`, so
`java:43`'s own `lhsNeighbor != null` is always true and the `rhsNeighbor` branch at `java:45-47` never runs) —
computing it would change nothing this port can observe.

The rest is a straight RNG replay, in Java's own draw order, through `pkg/javarand` (ADR-0010) — the first M6 effect to
draw from `Game.Rand()` for its own outcome rather than only for a stable-order shuffle: a 15% chance the card never
turns over at all (no remember, no further draws); otherwise one more draw picks a times-flipped count this port also
drops (nothing downstream reads `TimesFlipped` — neither `chaos_orb.txt`'s nor `falling_star.txt`'s own `SubAbility$`
chain names a `Condition*SVar$` on it — but the draw itself still has to happen, to keep the RNG stream in the same
place Java's own would be); then a last draw decides whether the flip lands on both the spot and its neighbor, on one of
the two, or on neither. `Aggregates.random(List, count)`'s own reservoir sampling never actually draws for this file's
own `randChoices`, which never grows past 2 elements against a count of 1 or 2 — verified by hand from
`Aggregates.java`'s own reservoir loop, `i <= count` always true across a 2-element list — so the two-card hit is a
plain slice, not a sampled one; only the one-card hit draws (`randomIndex`, `random.go`, already built for
`seekeffect.go`/`heisteffect.go`).

Rejected, GO-7: `AllowRandom$` (0 real corpus lines) feeds `chooseCardsForEffect`'s own `isOptional` argument, which can
answer an empty pick even with permanents on the battlefield — a shape with no real corpus line to confirm against. An
empty battlefield at resolution (Falling Star's own real shape: a sorcery, not yet a permanent, that can resolve into an
empty board) is also rejected with an error rather than left to panic the way `FlipOntoBattlefieldEffect.java:36`'s own
`tgtBox.getFirst()` would NPE on it.

Forge bug (PORT-8): `FlipOntoBattlefieldEffect.java:109`. `getNeighboringCard`'s own filter,
`c.isPlaneswalker() || c.isArtifact() || (c.isEnchantment() && !c.isAura())`, re-tests the landing spot itself (`c`)
instead of the candidate under test (`card`) on its own third clause. Entering the branch at all needs only one of the
three OR'd conditions on `c`: a planeswalker or artifact spot that is not also a non-Aura enchantment reaches the return
with its own third clause false, so it degenerates to the correct `card.isPlaneswalker() || card.isArtifact()` — no bug
there. Only a non-Aura-enchantment landing spot makes that third clause true unconditionally, and the whole return
degenerates to "true": every permanent on that spot's controller's battlefield becomes a valid neighbor candidate. Not
reproduced: this port rejects a non-Aura-enchantment landing spot with an error (`flipCandidates`) instead of silently
sweeping the whole battlefield for a card that names no such thing — a planeswalker or artifact spot, including Chaos
Orb choosing itself, is unaffected and resolves normally.

`enginelint.json` unchanged (skill step 3): `flipontobattlefieldeffect.go`'s own `//enginelint:allow` comment adds
`zone` and `parts` (`Card.Memory`) to its group, the first M6 effect file in this batch to need either.

## Researched and deferred

| API | Blocker |
| --- | --- |
| `SwitchBlock` | `effects-batches.md`'s own earlier note already flagged both real lines (`general_jarkeld.txt`, `sorrows_path.txt`) as `Defined$ Valid ...`. Reading `SwitchBlockEffect.java` directly confirms a deeper blocker past that: `combat.go` has `removeFromCombat` (`combat.go:54-65`) but no `unregisterAttacker`/`addBlocker`/`orderBlockersForDamageAssignment`/`orderAttackersForDamageAssignment` — attacking "bands" and a per-attacker blocker damage-assignment order, both of which reassigning blockers needs and neither of which this port's combat model builds. Each of the two real lines also takes a different one of the two symmetric branches (`isTargetingAttacker` true/false), so there is no single dominant shape to land partially either |
| `SetInMotion` | Every real corpus line is a `Mode$ SetInMotion` trigger (`TriggerZones$ Command`, fires when a scheme is set in motion, 85 real lines), not an `(AB\|DB\|SP)$ SetInMotion` effect line naming this `ApiType` — 2 real lines (`my_laughter_echoes.txt`, `plots_that_span_centuries.txt`). `Player.setSchemeInMotion` (Java) is Archenemy: a Scheme deck, Scheme cards living face-up in the Command zone, and a `SetInMotion` trigger mode. This port's `zone.go` carries a `SchemeDeck` zone constant only — no scheme-in-motion mechanic, no Command-zone scheme card handling, and no `SetInMotion` entry in `trigger.go`'s mode table exist to reach |
| `LosePerpetual` | `LosePerpetualEffect.java` reads `host.getChangedCardTraits()`, a per-card `Table<Long, Long, ICardTraitChanges>` that a "grant this card a trigger" effect (Java's `AddTrigger$`/`Bestow`-adjacent shapes) writes onto, keyed by timestamp so it can later be found and stripped by exactly this API. This port has no such store — `animate.go:160`'s own doc comment lists `Perpetual` only as a duration a card can carry, not a place a trigger gets attached and later removed from — so the trigger `LosePerpetual` is meant to strip (`pass_the_torch.txt`, `racketeer_boss.txt`) can never exist on a Crucible card in the first place |
| `ExchangeTextBox` | Ported, see [`layers-o-text.md`](layers-o-text.md) and [ADR-0039](../../../adr/0039-layer3-word-substitution.md). |
| `ChooseSector` | `ChooseSectorEffect.java` reads `card.getController().getController().chooseSector(...)`, a new `PlayerController` decision, and writes `Card.setChosenSector`. The one real corpus card, `space_beleren.txt`, is "Space sculptor": a keyword mechanic where every creature on the battlefield is assigned to one of three sectors (opponents assign first), read back by `Creature.ChosenSector`/`Creature.DifferentSector` valid properties on its own other two abilities. Building `ChooseSector` alone would compile and "resolve" while writing a value nothing meaningfully uses, since the sector-assignment half (a controller decision loop across every player's creatures) does not exist |
| `ChangeX` | `ChangeXEffect.java` sets `XManaCostPaid` on a targeted stack `SpellAbility` (`TargetType$ Spell`, `stackSpellCandidates`, `targeting.go`, already resolves the target). This port's `Ability` struct (`ability.go`) carries no X-mana-paid field at all — `amount.go`'s and `continuous.go`'s own doc comments both list `xPaid` among the amount dimensions this port does not resolve, and instant/sorcery casting itself is still `ErrUnimplemented` (`CLAUDE.md`'s own "real instant/sorcery casting" gap). This is the "new stack/casting mechanic" stop condition, not a param this file alone could add |

No new Forge bugs found in the deferred five past the one already on record above for FlipOntoBattlefield.
