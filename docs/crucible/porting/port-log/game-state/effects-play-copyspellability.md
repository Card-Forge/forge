# Port Log — Game State: M6 Effects: Play and CopySpellAbility

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

## Play and CopySpellAbility researched, not ported

Neither resolves; both stay `ErrUnimplemented`. Reason: the dominant corpus shape of each casts or copies an instant or
sorcery spell, and this port cannot put one on the stack. A permanents-only port would fail its game whenever the chosen
card is an instant or sorcery, which the dominant shapes cannot rule out.

**Not the blocker: the priority window.** Neither dominant shape needs a player to respond to anything.

| API | Why no priority window is needed |
| --- | --- |
| `Play` | Casts during resolution; timing is bypassed (`PlayEffect.java:460`, `playSaFromPlayEffect`), as `castWithoutPaying` (`discovereffect.go`) already does |
| `CopySpellAbility` | 164 of 255 lines are `Defined$ TriggeredSpellAbility` under a `Mode$ SpellCast` trigger. The trigger sits above the spell and resolves first under `ResolveStack`'s all-pass model, so the spell is there |

**The blocker: no instant or sorcery exists as a stack object.** `CastSpell` (`castspell.go`) casts only non-Aura
permanents and Auras; `castWithoutPaying` pushes only `APIPermanentCreature`/`APIPermanentNoncreature`. `ResolveStack`
(`stack.go`) pops and dispatches an `Ability`, and nothing moves a spell card off the stack after its effect.

### Play (330 corpus lines)

Java: `PlayEffect.java:84-493` (`resolve`), `PlayerControllerHuman.java:2416` / `PlayerControllerAi.java:1385`
(`playSaFromPlayEffect` → full `playSpellAbility`, cost payment and targeting included), `AbilityUtils`
`getSpellsFromPlayEffect`.

| Shape | Lines | Casts |
| --- | --- | --- |
| `Optional$` | 310 | -- |
| `WithoutManaCost$` | 284 | -- |
| `ValidSA$ Spell...` (205 bare `Spell`, rest with `cmc`/type suffixes) | 242 | Mostly any card type |
| `Defined$ Remembered` / `Valid$ Card.IsRemembered` | 128 | Whatever an earlier step exiled |
| `ValidSA$`/`ValidTgts$`/`Valid$` naming only `Instant`/`Sorcery` per item | 63 | Only an instant or sorcery, always |
| `CopyCard$` | 56 | A token copy cast from its zone |
| `ReplaceGraveyard$` | 35 | Exile instead of graveyard, on cast |

A port reusing `castWithoutPaying` resolves permanent spells only: 63 lines always error, and most of the rest error
whenever the exiled or remembered card is an instant or sorcery. `Discover` takes that route (`discovereffect.go`); for
`Play` it is not an honest port of the dominant shape.

### CopySpellAbility (255 corpus lines)

Java: `CopySpellAbilityEffect.java:65-210` (`resolve`), `CardFactory.java:80-117` (`copySpellHost`, a `COPIED_SPELL`
card built from the original), `CardFactory.java:127-167` (`copySpellAbilityAndPossiblyHost`, costs cleared),
`MagicStack.java:680-681` (copy ceases to exist leaving the stack, CR 707.10a).

| Shape | Lines | Needs |
| --- | --- | --- |
| `MayChooseTarget$` | 207 | New targets for the copy (CR 707.10c) |
| `Defined$ TriggeredSpellAbility` | 164 | The `SpellCast` trigger's triggering stack item |
| `TargetType$ Spell` / `ValidTgts$ Instant,Sorcery` and variants | 52 | Targeting a stack item |
| `TargetType$ Activated`/`Triggered`, `ValidStack Ability...` | 17 | Copy of an ability; stack-item identity exists since R25e (ADR-0041), the copy itself is unbuilt |
| Copies of permanent spells (`Card.Outlaw`, `Creature.YouCtrl`) | few | Token on resolution (CR 707.10f), Layer 1 copiable data |

The 17 ability-copy lines are not carved out. Reason: they need the same stack-item identity the dominant shape needs,
and building it for 17 lines would decide that design ahead of it.

### Smallest real design, in order

1. **Instant/sorcery spell object.** Pick the card's `A:SP$` line (`Def.Faces[0].Abilities`), choose modes
   (`chooseCharmModes`) and targets (`resolveTargets`, `targeting.go:54`), pay or skip the cost, push
   `Ability{API, Params, Amounts, Targets}` with a spell marker, fire `SpellCast`, `checkSpellCastTriggers`,
   `checkBecomesTargetTriggers`. After resolution, `ResolveStack` moves the card Stack → Graveyard through replacement
   (`MagicStack.java:647-692`, `removeCardFromStack`). Unblocks `Play`, `Discover`'s rejected branch, the cast in
   `ChangeZoneEffect.java:1556`, `ReplaceGraveyard$`, flashback, and a consumer for `Game.MayPlayFromExile` (Airbend,
   Heist grants).
2. **CR 608.2b fizzle check** (`MagicStack.java:704-752`, `hasFizzled`), with step 1. Reachable without priority: a
   `SpellCast` trigger resolving above the spell, or `Play`'s own `SubAbility$` chain, can remove the spell's target.
3. **For `CopySpellAbility`:** stack-item identity (`Ability` has none); the triggering stack item on the `SpellCast`
   trigger's ability (`checkSpellCastTriggers`, `trigger.go`, records none); `Game.Clone` coverage for both; a copy-host
   card that ceases to exist leaving the stack; `MayChooseTarget$` through `ChooseTargets`; permanent-spell copies
   becoming tokens, which needs Layer 1 copiable values and overlaps `Clone`.
4. Steps 1-3 change `ResolveStack`'s and `Ability`'s documented contracts ("only the no-response case", "Nil for a cast
   ability"). ADR first (ADRP-4).

**Forge bugs (PORT-8).** None found.

**Researched and deferred.**

| API | Blocker |
| --- | --- |
| `Play` | No instant/sorcery spell object on the stack: `A:SP$` cast, Stack → Graveyard after resolution, CR 608.2b fizzle (design steps 1-2, above) |
| `CopySpellAbility` | Steps 1-2, plus stack-item identity, `TriggeredSpellAbility` on `SpellCast` triggers, copy-host cards ceasing to exist, Layer 1 permanent-spell copies (3) |
