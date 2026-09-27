# Card Script Defects

- **Status:** Active

Defects in upstream card scripts that the port found. PORT-8: an upstream bug is reported and fixed upstream, never
worked around in Go, because a workaround makes Crucible disagree with the oracle for a reason no diff can explain.

Each was found by a gate rather than by reading: a parser or scanner that treats "Forge ignores this" as an error.

## Status

| Card                                       | Defect                                                                                             | Found by                        | Upstream                                                         |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------- | ------------------------------- | ---------------------------------------------------------------- |
| `the_dawning_archaic`                      | `ODeckHints:` for `DeckHints:`                                                                     | Unknown-key rejection           | [#11831](https://github.com/Card-Forge/forge/pull/11831), merged |
| `spirit_of_resilience`                     | `DBCleanup:` for `SVar:DBCleanup:`                                                                 | Unknown-key rejection           | [#11831](https://github.com/Card-Forge/forge/pull/11831), merged |
| `favor_of_jukai`                           | Missing `\|` fuses `ValidTgts$` and `NumAtt$`                                                      | Valid-base vocabulary           | [#11836](https://github.com/Card-Forge/forge/pull/11836), merged |
| `casey_raph_hotheads`                      | `SVar:DBCleanup` never written                                                                     | Sub-ability resolution          | [#11840](https://github.com/Card-Forge/forge/pull/11840), merged |
| `circadian_struggle`                       | Cleanup chains to a `DBEffect` that does not exist                                                 | Sub-ability resolution          | [#11840](https://github.com/Card-Forge/forge/pull/11840), merged |
| `withering_curse`                          | Chains to a `DBPutCounter` that does not exist                                                     | Sub-ability resolution          | [#11840](https://github.com/Card-Forge/forge/pull/11840), merged |
| `worzel_the_protector`                     | Chains to a `DBAttach` that does not exist                                                         | Sub-ability resolution          | [#11840](https://github.com/Card-Forge/forge/pull/11840), merged |
| `typhoid_mary_fractured`                   | Chains to a `DBCharm` that does not exist                                                          | Sub-ability resolution          | [#11840](https://github.com/Card-Forge/forge/pull/11840), merged |
| `goblin_razerunners`                       | `ValidTgts$ Player, Planeswalker` — a space after the comma                                        | Valid-string parsing            | [#11841](https://github.com/Card-Forge/forge/pull/11841), merged |
| `flamewave_invoker`                        | `ValidTgts$ Player, Planeswalker` — a space after the comma                                        | Valid-string parsing            | [#11841](https://github.com/Card-Forge/forge/pull/11841), merged |
| `peace_talks`                              | `StaticAbilities$` names `STCantTargetPlayer`, never defined                                       | Effect trait compilation        | pending                                                          |
| `ludevic_necrogenius_olag_ludevics_hubris` | `AddColors$ Blue & Black`; `CardFactory.java:497` splits on `,`, so Olag gains no color            | Clone port (`effects-clone.md`) | Not filed                                                        |
| `taskmaster_mercenary_mimic`               | Clone's `RemoveCreatureTypes$` is read by nothing in `getCloneStates` (`CardFactory.java:579-581`) | Clone port (`effects-clone.md`) | Not filed                                                        |
| `captured_by_the_consulate`                | `TriggeredSourceSA` under `Mode$ SpellCast`: never set (`TriggerSpellAbilityCastOrCopy.java:232`)  | ChangeTargets port              | Not filed                                                        |
| `mount_keralia`                            | `TriggeredCard$` under `Mode$ PlaneswalkedFrom`, which sets only `Cards`: X is 0                   | Planeswalk port                 | Not filed                                                        |

Every row but `peace_talks`, the two Clone rows, `captured_by_the_consulate` and `mount_keralia` is merged upstream; the
Clone rows and `captured_by_the_consulate` are rejected with an `error` meanwhile (`Defined$ TriggeredSourceSA` is not
resolvable, so the fix, `Defined$ TriggeredSpellAbility`, is what the port would resolve); `mount_keralia` resolves X to
0 instead (no counters to read back), so its eruption silently deals no damage until fixed upstream. `peace_talks` is
carried as a pending fix, logged in [upstream-patches.md](upstream-patches.md), until upstream merges it.
`internal/carddb/compile` compiles the whole corpus with no exemption of any kind, and `internal/valid` parses all
49,615 valid strings with no padded base.

Two more change no rules behaviour:

`the_eagles_are_coming` writes `SubAbility$` twice on one line. `FileSection.parseToMap` keeps the last value, and the
last value — `DBDelayedTrigger` — is the one with an `SVar:`; the first names a `DBChangeZone` the file does not define.
So the card plays correctly, and only because the map resolves the way it does. Worth tidying, because the line reads as
broken to anyone who does not know that rule.

`worzel_the_protector` spells "Faerie ceratures" in both `SpellDescription$` and `Oracle:`. Printed reminder text is
"(They're {G} 1/1 Faerie creatures with flying.)" — a one-character typo in two places, no rules effect.

## Unread parameters

A second gate, `tools/apiscan`, checks every param key a script writes against every key Forge reads. **18 uses of 13
keys across 17 cards are read by nothing.** All thirteen are fixed. A literal grep for each key across every `.java`
file in the repository returns zero.

Recovering "every key Forge reads" is the hard half. Forge reads a param six ways, and a scan that knows only the first
reports 326 keys as dead that Forge reads perfectly well:

| Shape                                                | Example                                         |
| ---------------------------------------------------- | ----------------------------------------------- |
| The accessors on an ability                          | `sa.getParam("NumDmg")`                         |
| The raw map, before an ability object exists         | `mapParams.containsKey("Layer")`                |
| A helper taking the key as an argument               | `getDefinedPlayersOrTargeted(sa, "TokenOwner")` |
| A key bound to a variable first                      | `final String key = "ResultSubAbilities"`       |
| Triggers and replacements matching against the event | `matchesValidParam("ValidExplorer", …)`         |
| A list literal with no call site                     | `additionalAbilityKeys`                         |

Each candidate rename was then checked four ways, because a plausible rename is not evidence: the effect's own source
and its default when the key is absent, `git log -S` over the Java for the commit that stopped reading it, the sibling
cards that write the candidate key, and the card's printed Oracle text.

### Seven that change what a card does

| Card                       | Defect                               | Consequence                                   | Upstream                                                       |
| -------------------------- | ------------------------------------ | --------------------------------------------- | -------------------------------------------------------------- |
| `leader_super_genius`      | `ValidConniver$` for `ValidCard$`    | Replacement fires on **every** connive        | [#11846](https://github.com/Card-Forge/forge/pull/11846), open |
| `mindblaze`                | `PeekNum$` for `PeekAmount$`         | Reveals 1 card, not the whole library         | [#11846](https://github.com/Card-Forge/forge/pull/11846), open |
| `mob_verdict`              | `Secret$` for `Secretly$`            | Secret council votes cast openly              | [#11846](https://github.com/Card-Forge/forge/pull/11846), open |
| `beorns_hospitality`       | `ValidTgtDesc$` for `ValidTgtsDesc$` | Prompt reads `Select target Creature.YouCtrl` | [#11846](https://github.com/Card-Forge/forge/pull/11846), open |
| `generous_revival`         | `ValidTgtDesc$` for `ValidTgtsDesc$` | Prompt reads the raw valid string             | [#11846](https://github.com/Card-Forge/forge/pull/11846), open |
| `galion_elvenkings_butler` | `ValidTgtsDes$` for `ValidTgtsDesc$` | Prompt reads the raw valid string             | [#11846](https://github.com/Card-Forge/forge/pull/11846), open |
| `shuttle_crew`             | `ValidTgtsDes$` for `ValidTgtsDesc$` | Prompt reads the raw valid string             | [#11846](https://github.com/Card-Forge/forge/pull/11846), open |

`ValidConniver` is the sharpest. `ReplaceConnive.canReplace` reads only `"ValidCard"`, and
`CardTraitBase.matchesValidParam` returns `!hasParam("Invert" + param)` — true — when the param is absent. The
restriction is not loosened but gone, so an opponent's connive is replaced too. The four description keys cost only a
readable prompt: `TargetRestrictions` falls back to `Lang.buildValidDesc`, which lowercases bare card types and nothing
else, so a valid string carrying properties reaches the player verbatim.

### Nine that change nothing

| Card                                  | Key                        | Why it is dead                                              |
| ------------------------------------- | -------------------------- | ----------------------------------------------------------- |
| `orochi_hatchery`                     | `TokenController$`         | `TokenOwner` defaults to `You`; no targeting on the ability |
| `spawning_pit`                        | `TokenController$`         | Same                                                        |
| `tomb_of_urami`                       | `TokenController$`         | Same                                                        |
| `faerie_dragon`, twice                | `RememberRandomChoice$`    | `DamageDealEffect` already remembers unconditionally        |
| `dance_of_the_dead`                   | `OverwriteSpells$`         | Reader deleted by `25900ee10cd` (#6996)                     |
| `natural_order`                       | `AISearchGoal$`            | Reader deleted by `0ba88f3ce5c`                             |
| `invasion_of_arcavios_invocation_...` | `AlternativeMessage$`      | Reader deleted by `da0db2282c1`, which missed this card     |
| `explosive_getaway`                   | `SpeTgtPrompt$`            | Never existed in Java, any revision                         |
| `nihiloor`                            | `TrigDescReminderDefined$` | Never existed in Java, any revision                         |

Deleted rather than corrected, in [#11848](https://github.com/Card-Forge/forge/pull/11848). `TokenController` never
appeared in Java in any revision and predates the 2013 module re-org; 2,198 corpus lines write `TokenOwner$ You`.
`AlternativeMessage` was stripped from roughly 60 cards by the commit that removed its reader, and 62 cards use
`OriginAlternative` — this is the one the sweep missed.

### The thirteenth, and why it took a rules reading

`dead_ringers` writes `ConditionPresentCompare$ EQ2` where the vocabulary is `ConditionCompare$`, so the compare keeps
its default `GE1`. Targets are pinned at two by `TargetMin$`/`TargetMax$`, so the two differ only when an opponent
removes one target in response.

The condition was not decoration. `ca362664b7c` removed the `Condition$ AllTargetsLegal` vocabulary item and rewrote
both cards that used it — Goblin Welder got an equivalent count-of-legal-targets branch, Dead Ringers got this — so the
typo silently disabled a deliberate fix, and deleting the param would have reverted it.

Which behaviour is right is settled by CR 608.2, not by the card:

> Illegal targets, if any, won't be affected by parts of a resolving spell's effect for which they're illegal. … If part
> of the effect requires information about an illegal target, it fails to determine any such information. Any part of
> the effect that requires that information won't happen.

"Destroy two target nonblack creatures unless either one is a color the other isn't" gates the destruction on a colour
comparison between both targets. With one illegal, that information cannot be determined, so the destruction does not
happen and the survivor lives. `EQ2` does that; `GE1` destroys the survivor.

| Behaviour                                  | One target removed in response | Verdict                                  |
| ------------------------------------------ | ------------------------------ | ---------------------------------------- |
| `GE1` — today, by accident                 | Destroys the survivor          | Wrong                                    |
| `ConditionCompare$ EQ2` — intended in 2024 | Spell does nothing             | **Correct under CR 608.2**               |
| Pre-2024 `RememberOriginalTargets$`        | Compares the original pair     | Wrong — reads an illegal target's colour |

Fixed in [#11850](https://github.com/Card-Forge/forge/pull/11850), open. The two Gatherer rulings, both 2004-10-04,
cover only the both-targets-legal case and are already implemented by `ConditionNoDifferentColors$ Targeted`; neither is
affected.

### Counting them needs the same care as finding them

A plain `grep -F 'Secret$'` reports two cards, because `KeepSecret$` on `ominous_lockbox` ends with the searched string.
Keys are only ever preceded by a line start, a pipe or a colon:

```console
$ grep -rhoE '(^|[|:] )Secret\$' forge-gui/res/cardsfolder/ | wc -l
1
```

Same class of mistake as the vocabulary scanner trimming its input and hiding `ValidTgts$ Player, Planeswalker`: a
search that ignores where a token can start finds tokens that are not there, and misses ones that are.

### Params the named effect does not read

`tools/apiscan -check -api` asks the stronger question: not "does anything read this key" but "does the effect this card
names read it". It reported **39 uses across 24 (API, key) pairs**, each verified against the effect's complete param
list and against the card's printed text. All 39 are fixed.

Nine shared one root cause. `DigEffect` and `ChangeZoneEffect` use **opposite names for the same two concepts**, and
cards written for one API reach for the other's:

| Concept              | `Dig` reads     | `ChangeZone` reads |
| -------------------- | --------------- | ------------------ |
| Who chooses          | `Choser`        | `Chooser`          |
| Prompt shown to them | `PrimaryPrompt` | `SelectPrompt`     |

`Choser` is Forge's misspelling and it is load-bearing on `Dig`: the corpus writes `Chooser$` 278 times against
`Choser$` 12 times, so the cards that lose the setting are mostly the ones spelling it correctly on a `Dig` line.

#### Four that change what a card does

| Card                    | Fix                                  | Effect today                                     |
| ----------------------- | ------------------------------------ | ------------------------------------------------ |
| `reality_shaping`       | `Choser$` → `Chooser$`               | One player picks the card every player puts down |
| `mirkwood_trapper`      | `Chooser$` → `Defined$`              | The wrong player chooses the creature            |
| `echocasting_symposium` | `TokenOwner$` → `Controller$`        | The caster gets the token, not the target player |
| `my_followers_ascend`   | `RememberChosen$` → `RememberCards$` | The creature never gains flying and vigilance    |

`ChooseCardEffect` takes its choosers from `Defined$` and reads no `Chooser` at all, which is why Mirkwood Trapper's fix
is a different key rather than a spelling. `CopyPermanentEffect` reads `Controller` and defaults to the activating
player (`CopyPermanentEffect.java:135-142`). `CountersPutEffect` remembers with `RememberCards`
(`CountersPutEffect.java:658`), and My Followers Ascend chains to `DBPump | Defined$ Remembered`, so nothing remembered
means nothing pumped.

#### Seven that lose a prompt

`blood_for_bones` (twice), `kazandu_stomper`, `mycoid_resurrection` and `rocco_cabaretti_caterer` write `PrimaryPrompt$`
on a `ChangeZone`; `sandstalker_moloch` writes `SelectPrompt$` on a `Dig`; `myra_the_magnificent` writes `ChoicePrompt$`
where `CountersPutEffect` reads `ChoiceTitle`. Generic prompt instead of the written one, no rules effect.

#### Twenty-seven that change nothing

Deleted rather than corrected, in three groups. **Already the default:** `DigUntil` always reveals and `PeekAndReveal`
reveals unless told not to, so nine `Reveal$ True` are decoration; `ChooseCard` is optional unless `Mandatory` and
reveals only when `Reveal` is set; `Dig`'s chooser already defaults to the activator (`DigEffect.java:116`). **Another
line on the same card already does it:** Chaotic Transformation imprints on its `ChangeZone`, Chandra uses
`ReplaceGraveyard$ Exile`, The Seventh Doctor guesses in its `GenericChoice`. **Belongs to a different API:**
`ChangeZoneAll` has no description parameter at all, `Draft` reads only `Spellbook`, `Effect` reads `ForgetOnMoved` and
has no `PumpZone`, `Token` has no `ForgetOtherRemembered`.

Every one is a key some effect reads, written on an effect that does not — the signature of a card script copied from
another card and edited.

### Why an unread key is silent

`AbilityFactory` builds the param map from the script line and hands it to the effect; the effect asks for the keys it
knows. A key nobody asks for is neither rejected nor logged — unlike a broken `SubAbility$` chain, which at least prints
to stdout. So the failure mode is worse than the one above: the card loads, plays, and simply does less than its own
line says.

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
