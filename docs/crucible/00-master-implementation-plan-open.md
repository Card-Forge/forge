# Crucible — Implementation Plan: Open

The only plan file. Everything still to do, sorted easiest first; the exit gates; milestones M7-M9, not yet started.
M0-M4 are done; M5-M6 are in progress, their landed work recorded per topic in
[`porting/port-log/game-state.md`](porting/port-log/game-state.md).

---

## Remaining work, easiest first

Sizes: **S** under a day, one sitting. **M** one porter batch, 1-3 days. **L** 1-2 weeks, a design call or several
batches. No row is larger. Within a size the order is by value per effort. `R` numbers are this list's own; milestone
items keep their numbers (24-41). The refusal figures come from `crucible card-smoke -all`
([`smoke-probe.md`](porting/port-log/smoke-probe.md)): 92.8% of the 33,572 judged corpus cards play, an upper bound,
since silent misses are invisible. Re-rank with the command before starting any M item, because the counts move with
every port.

| # | Work | Milestone | Size | Needs |
| --- | --- | --- | --- | --- |
| R3 | File upstream the Forge bugs marked "Not filed" in [`forge-java-defects.md`](porting/forge-java-defects.md) (about 30 rows, one PR each) and the `doubling_chant.txt:5` `Choices$` without `Creature` | M5 | S/PR | you; tests per PR |
| R10a | Leftovers of R10: the 7 pay-N-times cards (`Mana<…\NumTimes>` cost), Incinerator of the Guilty and Hordewing Skaab (refused for other causes), 3 cards whose cost now pays but whose effect refuses (Atraxa's Skitterfang `KWChoice$`, Biting-Palm Ninja `RevealHand` `Defined$`, Myr Battlesphere `DealDamage` `Defined$ TriggeredDefender.Opponent & Valid ...`); `Mill<N>` cost part waits on the Forge bug in `forge-java-defects.md` | M6 | S | `card-smoke` re-rank |
| R11a | Leftovers of R11: `Mode$ CantBlockUnless` (10 lines, blockers block free); mana-ability window for payments outside priority (attack costs, `UnlessCost$` at resolution pay from the pool only, CR 508.1i); exert asked after attack costs, Java before | M6 | S | `card-smoke` re-rank |
| R12b | Findings of R12a: `ChangesZone` lines from a stack, exile or command origin to a non-battlefield zone fire nothing (34 `Origin$ Exile`/`Destination$ Any` lines, 3 `Origin$ Stack`), and a graveyard host with no `TriggerZones$` is never walked; `WithCountersType$` on the hidden-origin search path still refuses a non-battlefield destination | M5 | M | `card-smoke` re-rank |
| R13 | Single-card scenarios for shapes that already play, untested: Assimilation Aegis (`Clone` `UntilUnattached`), Dinosaur Headdress (Craft), Curse of Leeches and Ludevic `Transform`, The Last Ronin's Technique (`Count$ValidStack`), Opposition Agent edge cases | M6 | M | `card-smoke` re-rank |
| R15a | Leftovers of R15: stickers do not follow a merge, meld, unmeld or mutate (`takeStickersFrom`, `chooseCardToKeepStickers`, a new `PlayerController` method); the fixture has no `queue sticker` / `queue stickerpos` action; no deck loader calls `SeatStickerSheets`; ticket payment emits no counter-removed event; `GainsStickerAbilitiesOf` on a static with `AddKeyword$` is untested beside it | M6 | S | the game-from-deck loader (M7) |
| R15b | Findings of R15: 5 sticker cards still refuse, each for another cause: `____ Trespasser` (`Pump KW$ HIDDEN` naming a blank, `"_"`), `Wolf in ____ Clothing` (`ImmediateTrigger RememberSVarAmount$`), Command Performance (`RollDice ToVisitYourAttractions$`), Lineprancers (`illegal declaration`: the pumped creature must block, the probe board cannot), Scampire (`ChangeZone SorcerySpeed$`) | M6 | S | card-smoke re-rank |
| R16a | Vedalken Squirrel-Whacker still refuses: its `K:ETBReplacement:Other` chain ends in `Animate Defined$ Self Duration$ Permanent`, an as-it-enters ability that changes the entering card (`entryChainAPIs` leaves Animate out because `Game.Move` rebuilds what it writes). The exchange itself (ADR-0045) is tested | M6 | S | `card-smoke` re-rank |
| R16b | Leftovers of the PutCounter keyword pack: `PowerUp$` (37 lines; the activation also costs the host's mana cost less when it entered this turn, `CostAdjustment.java:225-227`, and `PutCounter` still refuses it), `Bolster$`, the `Mode$ CanAdapt` static (Biomancer's Familiar), `StaticAbilityAdditionalActivations` raising the Exhaust/Power-up limit | M6 | S | `card-smoke` re-rank |
| R25g | Cast-time cost shapes `parseUnlessCost` does not know (about 14 cards left; `Discard<N/Random>`, `ExileFromGrave` `All` and `TopGraveyardCreature` landed): typed `Discard<N/Creature>`, `tapXType` with an unlisted type, `Reveal<N/Creature.Colorless>`, `Exile<N/...>`, `Sac<All/...>`, `Announce$ X` | M6 | M | `forge-oracle` per part |
| R25h | `RaiseCost` statics with a non-mana `Cost$` (about 25 cards): Aether Tide (`Discard<X/Creature>`), Crashing Wave, Foggy Swamp Visions, Waterbender's Restoration (`Waterbend<X>`), Tectonic Split, Liesa, Soul Immolation, the Behold family. `spellCost` refuses them today. Confirm Java's raw-cost X announce on these (a probable Forge bug, PORT-8) | M6 | M | R25g |
| R25j | Wildfire Howl: `Count$PromisedGift.1.0` and the gift cost (`GiftDescription$`, `Defined$ Promised`): a promised gift is an optional cost chosen as the spell is cast, with the opponent receiving the gift on resolution | M6 | S | R10 (optional-cost shapes; gift is one) |
| R25k | Geth's Summons: still not castable after its `IsCorrupted` amount resolves; cause not traced. Its second link is `ChangeZone` with `TargetsForEachPlayer$` and `TgtZone$ Graveyard`, so start there | M6 | S | — |
| R25l | Findings of R25d: 9 cards now judged and refused (Blaze of Glory `CanBlockAny$`, Yare `CanBlockAmount$`, Flash Foliage `TokenBlocking$`, False Orders `UnblockCreaturesBlockedOnlyBy$`, Illusionist's Gambit RemoveFromCombat and Take the Bait Goad on `Defined$ "Valid Creature.attacking"`, Tears of Rage PumpAll `AtEOT$`, Wake the Dead ChangeZone `AtEOT$`, Winter's Chill GenericChoice `UnlessCost$`); Suffocation needs a record of a red spell that dealt damage this turn; `FirstUpkeep$` suspected Forge bug (`PhaseHandler.java:257` increments before `isFirstUpkeep` at `:980-981`, unconfirmed) | M6 | S | `card-smoke` re-rank |
| R25m | Findings of R25e: `CopySpellAbility` of an ability on the stack (Strionic Resonator, Lithoform Engine, Kirol, Echo, Gogo, Tawnos, Return the Favor; a copy keeps its own host per CR 707.10, `copySpell` builds a new spell card, `Mode$ SpellAbilityCopy` triggers do not exist); an Effect card's ability matched through its effect source (`SpellAbility.java:2098-2107`, not recorded: only a bare `Card` spec answers); Chef's Kiss `ControlSpell` `RememberTargets$`; Emissary of Grudges `ChoosePlayer Secretly$`; Ertai Resurrected `Destroy RememberLKI$`; probe board lacks a colorless spell or trigger for Consign to Memory, Reroute, Teferi's Response, Not of This World; stale comment in `trigger.go` `checkBecomesTargetTriggers` ("no activated-ability targeting built yet") | M6 | M | `card-smoke` re-rank |
| R25n | Findings of R25f: 88 cards still inconclusive (the "72" was stale; only 32 needed game state); refusals now visible: `Attach` `Move$` with `Object$ ParentTarget` (Aura Graft), `PumpAll Duration$` (For the Common Good), `Pump AtEOT$` (Glyph of Destruction, with R25l), `ExchangeControl TargetsWithSameCardType$` (Role Reversal, Shifting Loyalties), `CopyPermanent Populate$` (Scion of Vitu-Ghazi); Flashback, Recoup and Sins of the Past still cannot be cast on the rich board (cause not traced); unported properties `MonoColor`, `NotedColor`, `greatestPower`, `BorderColorSilver`, `YourTeamCtrl`; `cloneUnportedProperties` in `cloneeffect.go` still lists `token`, which `valid.go` answers | M6 | M | `card-smoke` re-rank |
| R9m | Findings of R9k: ChangeZone `AtRandom$` (about 30 cards; Java sorts the fetch list by `CardView.toString`, `ChangeZoneEffect.java:1097`, `:1199`, an order Crucible cannot reproduce); ChooseType `AtRandom$` over a `Type$` list (HashSet order, `CardType.java:871-873`), `TypesFromDefined$` and `MostPrevalentInDefinedZone$` (ExiledWith/Craft and noted types); ChooseNumber `Secretly$` (HashMap order); NameCard `AtRandom$` over the card database; GenericChoice `AtRandom$ Urza`, `NumRandomChoices$`, `Guess$`; Codie `Defined$ Valid ...` on AlterAttribute; Coin of Mastery `ReplacedCard$CastTotalManaSpent`; a cast prepared copy has no legal-action listing (only `CastSpell` reaches it); the probe never tries the prepared copy; possible Forge bug unconfirmed: the token-in-exile check (`GameAction.java:1912-1932`) exempts a prepared copy in exile but not once cast | M6 | M | `card-smoke` re-rank |
| R9q | Findings of R9p: Unyaro (`PlaneswalkedToThisTurn`, then `Phases AllValid$`/`WontPhaseInNormal$`, Effect `Triggers$`); Coiling Rebirth (`PromisedGift`: Gift casting, `K:Gift`/`GiftAbility`, not ported); Mana Clash (FlipCoin `RememberResult$`, `FlippedTails`, `ClearCoinFlips$`); Spikeshell Harrier (inline compare operand, `NonTargetedController`, `HighestSpeed`: a compile change); Undercellar Sweep (`TriggeredAttackedTargetAndYou` in a trigger `CheckSVar$`: Java passes the `Trigger` as ctb, `CardTraitBase.java:472`, but `getDefinedPlayers`' `Triggered` branch needs a `SpellAbility`, `AbilityUtils.java:1013`); Ice Cauldron, Jeweled Amulet (`RememberCostMana$`: remembered mana read back by a restricted mana ability, no reader); `Game.costPaymentStack` (Professor Hojo, Tezzeret: ability `ReduceCost` lines not applied); X in a stack card's mana value outside amount measures (`Card.CMC` stays printed); 3 plays->refused naming real heads (Korvold, Valiant Changeling, Lullmage's Domination) | M6 | M | card-smoke re-rank |
| R17 | Close P4: the qualitative half, "every layer, every SBA" (Exit gates, P4), with the audit written down and M5's exit gate declared | M5 | L | — |
| R18 | The rest of `PlayerController` (about 90 Java decision methods against 52 here), `PhaseHandler` step bodies, `undoStack`, `CardState` flip trivia | M5 | L | mostly M7's AI needs them |
| R19 | Silent-miss audit: the probe cannot see a card that plays and misplays. A Java scenario runner for L2 parity (ADR-0010 names it; none exists) and a pass over the silent GO-7 gaps | M5-M6 | L | oracle-java work |
| R20a | Gauntlet decklists in `gauntlets/modern/` with `gauntlet.toml` (format, weights, source and date), loaded by `internal/deck`; `crucible card-smoke -decks gauntlets/modern` prints the played share | M6 | S | **your decklists** (none are invented) |
| R20b | Take every refusal `card-smoke -decks gauntlets/modern` reports and port it, in batches of about 30 cards by shared cause: the long tail of R9a-R9e and R10-R16 for this gauntlet. Done when every card plays or is documented as out of scope | M6 | L | R20a |
| R20c | Silent-miss pass over the gauntlet cards only (R19 method, applied to this card set): each card checked against its Oracle text and the Java that runs it with `check-card`, defects fixed or logged | M6 | L | R19, R20a |
| R21a | `RecordingPlayerController` in `oracle-java`: wraps the Java AI and writes every `PlayerController` decision (which method, the options offered, the answer) to a decision log; one game of a fixed seed and two decks | M6 | L | R18 |
| R21b | The event normaliser and the log format (ADR-0010 L3): Java `GameEvent`s and Go events mapped to one schema, so the two traces can be diffed line by line | M6 | M | R21a |
| R21c | `ReplayController` in Go: feeds the recorded answers back into `Game.Run`; the first-divergence reporter (turn, step, decision, both states) | M6 | M | R21b |
| R21d | Nightly corpus of recorded games (about 200 seeds over the gauntlet), the CI job, and the divergence triage list. **P5 gate:** replay parity green over the corpus | M6 | L | R19, R20b, R21c |
| R22a | `ComputerUtilMana` port (1,737 LOC): mana payment planning, the first thing the runner needs (item 33) | M7 | L | R18 |
| R22b | `ComputerUtilCombat` port (2,617 LOC): attack and block evaluation (item 33) | M7 | L | R22a |
| R22c | `ComputerUtilCard` and `CreatureEvaluator` ports (item 33) | M7 | M | R22a |
| R22d | `AiController` skeleton: the decision loop and `.ai` profile loading, answering every `PlayerController` method with a legal default (item 34) | M7 | M | R22a, R22c |
| R22e | Per-API `SpellAbilityAi` classes in batches by API group (removal, pump and combat tricks, draw and tutor, tokens and counters, mana and land, the long tail): each group a porter batch (item 34) | M7 | M x1 | R22d |
| R22f | Lookahead: `GameCopier` over the Go state clone (ADR-0009), `GameSimulator`, `GameStateEvaluator` (item 35) | M7 | L | R22e |
| R22g | `SpellAbilityPicker` and the statistical-parity run against Java (L4, ADR-0010). **P6 gate:** win rate within the stated bounds on the reference gauntlet | M7 | L | R22f, R21d |
| R23a | `internal/sim`: worker pool, seed management, turn cap, crash isolation (a panicking or hung game fails only itself) (item 36) | M8 | M | R22d |
| R23b | Gauntlet config: match schedule, play/draw alternation, games per matchup, seed derivation (item 36) | M8 | S | R23a, R20a |
| R23c | `internal/telemetry` recorder and the castability probe and mana sampler (item 37) | M8 | M | R23a |
| R23d | Tenure tracking and aggregation (per-card, per-matchup counters; item 37) | M8 | M | R23c |
| R23e | `internal/store`: shard writers, `manifest.json`, the streaming reader (ADR-0016; item 38) | M8 | M | R23d |
| R23f | 100k-game soak on the reference gauntlet: zero panics, zero hangs, byte-identical re-run from seed. **P7 gate** | M8 | M | R23b, R23e |
| R24a | `internal/report` core: matchup matrix with confidence intervals and the dead-card table (item 39) | M9 | M | R23f |
| R24b | Mana health, opening-hand analysis and the play/draw split (item 39) | M9 | M | R24a |
| R24c | Per-card impact with all four attribution modes, each labelled (item 39) | M9 | L | R24a |
| R24d | Markdown, HTML and JSON renderers and `crucible report` (item 40) | M9 | M | R24b, R24c |
| R24e | Swap proposer: candidate swaps from the dead-card and impact tables (item 41) | M9 | M | R24c |
| R24f | Confirmatory A/B gauntlet and the ranking by measured win-rate delta with intervals (item 41). The product | M9 | L | R24e, R23f |
| R24g | Blind test on a known-good and a known-bad deck against an expert assessment. **P8 gate** | M9 | M | R24d, R24f |

The rows are independent within a size unless `Needs` says otherwise. R3 needs you, because it publishes. R20a is the
only row whose input Claude must not invent: a fabricated metagame yields confident, wrong recommendations
([`meta-gauntlet.md`](research/meta-gauntlet.md)).

---

## Exit gates

Each milestone closes on its gate. Scenario parity = `TestScenarios` over `crucible/testdata/scenarios/` (ADR-0010 L2);
replay parity = recorded Java games replayed in Go (L3, harness not started); statistical parity = win-rate within stated
bounds (L4).

| Gate | Milestone | Condition | State |
| --- | --- | --- | --- |
| P4 | M5 rules kernel | Scenario parity green on ≥300 hand-written scenarios covering every step transition, every layer, every SBA | Count met (441), every layer met; trigger modes other than enters/dies/attacks/blocks/deals damage/is discarded/becomes tapped/taps for mana/casts a spell/upkeep lack a fixture (R17) |
| P5 | M6 effects | 100% supported for the active gauntlet (`card-smoke -decks`); scenario parity per API (normal, edge, interaction); replay parity green over the nightly log corpus | Not met: 92.1% of corpus judged cards play; replay harness not started |
| P6 | M7 AI | Statistical parity vs Java on the reference gauntlet, not bit-exact | Not started |
| P7 | M8 sim runner | 100k games on the reference gauntlet, zero panics, zero hangs (turn cap), seed re-run byte-identical telemetry | Not started |
| P8 | M9 reporting | Reports for known-good and known-bad decks give the expected diagnosis on a blind test set | Not started |

## Milestones

### M7 — AI port — 5–8 wks

33. `ComputerUtilMana` first (the runner cannot play a real game without mana planning), then `ComputerUtilCombat`,
    `ComputerUtilCard`, `CreatureEvaluator`.
34. `AiController` + per-API `SpellAbilityAi` decision logic; `.ai` profile loading.
35. Lookahead simulator on top of the cheap Go state clone. **Exit gate:** P6 gate — statistical parity within stated
    bounds vs. Java on the reference gauntlet.

### M8 — Simulation runner & telemetry — 2–3 wks

36. `internal/sim`: worker pool, seed management, gauntlet config, turn caps, crash isolation (a panicking game fails
    that game only).
37. `internal/telemetry`: recorder, castability probe, mana sampler, tenure tracking, aggregation.
38. `internal/store`: shard writers, `manifest.json`, and a streaming reader (ADR-0016). **Exit gate:** P7 gate — 100k
    games clean, deterministic re-run byte-identical.

### M9 — Reporting & the optimization loop — 2–4 wks

39. `internal/report`: matchup matrix w/ CIs, dead-card table, mana health, per-card impact (all four attribution modes,
    each labelled), opening-hand analysis, play/draw split.
40. Markdown + HTML + JSON output; `crucible report`.
41. **A/B swap optimizer**: propose candidate swaps from the dead-card and impact tables, run the confirmatory A/B
    gauntlet, rank by measured win-rate delta with CIs. This is the actual product. **Exit gate:** P8 gate — blind-test
    diagnosis matches expert assessment on known-good and known-bad decks.
