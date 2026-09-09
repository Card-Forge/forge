# Card Script Defects

- **Status:** Active

Defects in upstream card scripts that the port found. PORT-8: an upstream bug is reported and fixed upstream, never
worked around in Go, because a workaround makes Crucible disagree with the oracle for a reason no diff can explain.

Each was found by a gate rather than by reading: a parser or scanner that treats "Forge ignores this" as an error.

## Status

| Card                     | Defect                                                      | Found by               | Upstream                                                         |
| ------------------------ | ----------------------------------------------------------- | ---------------------- | ---------------------------------------------------------------- |
| `the_dawning_archaic`    | `ODeckHints:` for `DeckHints:`                              | Unknown-key rejection  | [#11831](https://github.com/Card-Forge/forge/pull/11831), merged |
| `spirit_of_resilience`   | `DBCleanup:` for `SVar:DBCleanup:`                          | Unknown-key rejection  | [#11831](https://github.com/Card-Forge/forge/pull/11831), merged |
| `favor_of_jukai`         | Missing `\|` fuses `ValidTgts$` and `NumAtt$`               | Valid-base vocabulary  | [#11836](https://github.com/Card-Forge/forge/pull/11836), merged |
| `casey_raph_hotheads`    | `SVar:DBCleanup` never written                              | Sub-ability resolution | Open, carried                                                    |
| `circadian_struggle`     | Cleanup chains to a `DBEffect` that does not exist          | Sub-ability resolution | Open, carried                                                    |
| `withering_curse`        | Chains to a `DBPutCounter` that does not exist              | Sub-ability resolution | Open, carried                                                    |
| `worzel_the_protector`   | Chains to a `DBAttach` that does not exist                  | Sub-ability resolution | Open, carried                                                    |
| `typhoid_mary_fractured` | Chains to a `DBCharm` that does not exist                   | Sub-ability resolution | Open, carried                                                    |
| `goblin_razerunners`     | `ValidTgts$ Player, Planeswalker` — a space after the comma | Valid-string parsing   | Open, carried                                                    |
| `flamewave_invoker`      | `ValidTgts$ Player, Planeswalker` — a space after the comma | Valid-string parsing   | Open, carried                                                    |

An **open, carried** fix is applied to Crucible's corpus while its pull request waits, logged in
[`upstream-patches.md`](upstream-patches.md) with the condition that deletes it. All five are carried, so
`internal/carddb/compile` compiles the whole corpus with no exemption of any kind.

Two more are known and deliberately unreported: `the_eagles_are_coming` writes `SubAbility$` twice on one line, where
Java's param map keeps only the last, and `worzel_the_protector`'s `Oracle:` line spells "Faerie ceratures". Neither
changes what a card does.

## Why a broken chain is silent

An `A:`, `T:`, `S:` or `R:` value is a param map, `Key$ Value | Key$ Value`, whose first key declares the record — `SP$`
a spell, `AB$` an activated ability, `DB$` a sub-ability, `Mode$` a trigger. Effects chain by name: `SubAbility$ DBFoo`
resolves `SVar:DBFoo` next, so a card is a linked list walked at resolution.

`AbilityFactory.getSubAbility` (`forge-game/src/main/java/forge/game/ability/AbilityFactory.java:357`) looks the name up
and, when it is absent:

```java
System.out.println("SubAbility '"+ sSub +"' not found for: " + state.getName());
return null;
```

A line on stdout, a null child, and the chain ends early. Nothing fails at load, so the card ships playing as if the
missing link were never written. `internal/carddb/compile` errors instead — 33,684 of 33,689 cards resolve cleanly, and
the five that do not are above.

`Remember` and `Cleanup` are a pair, which is what makes a missing cleanup a bug rather than dead text. An effect
records what it created or moved with `RememberChanged$` or `RememberTokens$`; later links read it back as `Remembered`.
The list lives on the card and outlives the spell, so the chain ends with `DB$ Cleanup | ClearRemembered$ True`
(`docs/Card-scripting-API/AbilityFactory.md:64`, `:186`).

## Casey & Raph, Hotheads

{4}{R} Legendary Creature — Mutant Ninja Human Turtle, 4/4. Teenage Mutant Ninja Turtles Eternal (`tmc`).

```text
When Casey & Raph enter, choose one or both. Each mode must target a different player.
• Target player exiles the top card of their library. Until that player's next end step, they may play that card
  without paying its mana cost.
• Target player creates two Treasure tokens.
```

Chain: `TrigCharm` → `DBExile` (exiles, `RememberChanged$ True`) → `DBEffect` (grants the permission, reading
`RememberObjects$ Remembered`) → `SubAbility$ DBCleanup`, **which was never written**.

The exiled card therefore stays on the remembered list after the spell finishes. Casey & Raph is a permanent whose
trigger can fire again into the same list, and the `Effect` copies whatever the list holds.

Fix: add `SVar:DBCleanup:DB$ Cleanup | ClearRemembered$ True`, which is the line `riku_of_many_paths` and
`ral_monsoon_mage_ral_leyline_prodigy` already carry for the same `Effect` shape.

**The only one of the four that changes how a card plays.**

## Circadian Struggle

{4}{G/U}{G/U} Instant. Alchemy: Lorwyn Eclipsed (`yecl`).

```text
Vivid — Seek X cards that each share a color with one or more permanents you control, where X is the number of
colors among permanents you control. For each color among permanents you control, those cards perpetually gain
"This spell costs {1} less to cast."
```

Chain: `Seek` → five `AnimateAll` steps, one per colour, each gated on `ConditionPresent$ Permanent.YouCtrl+<colour>`
and carrying `Duration$ Perpetual | staticAbilities$ ReduceCost` → `DBCleanup`.

Those five steps are the card's "for each color", so the perpetual reduction is already complete. `DBCleanup` then
chains to `SubAbility$ DBEffect`, which does not exist. A cleanup is the end of a chain.

Fix: drop the trailing `| SubAbility$ DBEffect`. Nothing changes; Forge discarded it already.

## Withering Curse

{1}{B}{B} Sorcery, mythic. Secrets of Strixhaven (`sos`). **Standard legal**, unlike the other three.

```text
All creatures get -2/-2 until end of turn.
Infusion — If you gained life this turn, destroy all creatures instead.
```

Chain: `PumpAll` (when `X`, life gained this turn, is 0) → `DBDestroyAll` (otherwise) → `SubAbility$ DBPutCounter`,
which does not exist. Neither the printed text nor the script involves counters; `foolish_fate` writes the same Infusion
clause with no such link.

Fix: drop `| SubAbility$ DBPutCounter`.

## Worzel, the Protector

{1}{W}{W}{W} Legendary Planeswalker — Worzel, loyalty 4. Mystery Booster Commander Edition (`mbc`).

```text
0: Create a 1/1 white Cat creature token with "{T}: Put a loyalty counter on each planeswalker you control."
−1: Look at the top six cards of your library. You may reveal a planeswalker or basic Plains card from among them
    and put it into your hand. Put the rest on the bottom of your library in a random order.
−8: Create ten Scryb Sprites tokens. (They're {G} 1/1 Faerie creatures with flying.)
Worzel, the Protector can be your commander.
```

The `0:` ability creates the Cat, remembers it with `RememberTokens$ True`, then chains to `SubAbility$ DBAttach`, which
does not exist. Attaching is for Auras and Equipment; a Cat token is neither, and nothing in the printed text attaches
anything.

Fix: drop `| SubAbility$ DBAttach`. `RememberTokens$ True` stays — whether it still earns its keep is a separate
question from the broken reference, and widening the diff makes the defect harder to review.

## Typhoid Mary, Fractured

{1}{B}{R} Legendary Creature — Mutant Villain, 3/3. Marvel Super Heroes Commander (`msc`).

```text
Whenever Typhoid Mary attacks, choose one at random. If you discarded a card this turn, you choose one instead.
• Mary — Create a Treasure token.
• Typhoid Mary — Draw a card.
• Bloody Mary — Each opponent loses 2 life and you gain 2 life.
```

Chain: the attack trigger runs `TrigCharm`, which chains to `SubAbility$ DBCharm`, which does not exist.

**The card is already correct without it.** `Random$ Compare | RandomCompareSVar$ Y | RandomCompare$ LT1` is the whole
of "choose one at random, unless you discarded": `CharmEffect` computes `random = Expressions.compare(Y, "LT", 1)`, and
when that is false it falls through to the ordinary choose-a-mode path in the same effect
(`forge-game/src/main/java/forge/game/ability/effects/CharmEffect.java:245-262`). `Y` is
`PlayerCountPropertyYou$CardsDiscardedThisTurn`.

So `DBCharm` is a leftover of the same kind as the other three, not a missing feature — a second `Charm` would make the
card offer two modes, which its text does not say.

Fix: drop `| SubAbility$ DBCharm`.

## Goblin Razerunners and Flamewave Invoker

{2}{R}{R} Creature — Goblin Warrior, 3/4, Neon Dynasty Commander (`nec`), and {2}{R} Creature — Goblin Mutant, 2/2,
Battlebond (`bbd`).

```text
{1}{R}, Sacrifice a land: Put a +1/+1 counter on this creature.
At the beginning of your end step, you may have this creature deal damage equal to the number of +1/+1 counters
on it to target player or planeswalker.
```

```text
{7}{R}: This creature deals 5 damage to target player or planeswalker.
```

Both write `ValidTgts$ Player, Planeswalker`. `CardTraitBase.java:261` splits the param with a plain `split(",")`, so
the second alternative is `" Planeswalker"`, and `CardType.hasStringType` rejects it: no subtype is spelled with a
leading space, `StringUtils.capitalize` leaves one untouched, and `CoreType.getEnum` and `Supertype.getEnum` are exact
lookups (`forge-core/src/main/java/forge/card/CardType.java:347`).

So the alternative matches nothing and both abilities can only target players, against their own text. 136 other cards
write `ValidTgts$ Player,Planeswalker` with no space.

Fix: delete one space each.

`internal/valid` parses without trimming for exactly this reason. The vocabulary scanner trims each alternative before
counting, which collapsed `" Planeswalker"` and `"Planeswalker"` into one token and hid the defect; a parser that tidies
its input cannot find the bugs that tidying would fix.
