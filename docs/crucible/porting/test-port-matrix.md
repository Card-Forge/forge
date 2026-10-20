# Test Port Matrix

- **Status:** Active. M5 gate: every rules-relevant row is green
- **Applies to:** every TestNG test inherited from upstream Forge; M5 exit gate (plan §3.5)

Disposition of every Java test class in upstream Forge. Rules-relevant classes get one row per test method, everything
else one row per class. M5 exits when every row marked `M5? yes` is green. Reason: the Java suite is the acceptance
floor for the rules kernel (plan §3.4); a row nobody triaged is a gap nobody sees.

## Status vocabulary

| Status       | Meaning                                                          | Required in row                              |
| ------------ | ---------------------------------------------------------------- | -------------------------------------------- |
| `ported`     | Go fixture or Go test covers same behavior                       | Path under `crucible/`; Go-test-only → Note  |
| `superseded` | Covered more broadly by differential or scenario suite           | Suite name                                   |
| `n/a`        | Out of scope for Crucible, or owned by a later milestone (named) | Reason + plan citation                       |
| `todo`       | Not addressed                                                    | Nothing; `M5? yes` rows listed in M5 backlog |

Green = any status except `todo`. Plan §3.5 prefers fixtures (`crucible/testdata/scenarios/<case>/`, TEST-5) over Go
functions; a `ported` row citing only a Go test is green but a fixture candidate.

`M5?` = row asserts game-rules behavior the M5 kernel owns (combat, stack, triggers, SBAs, layers, mana payment, zone
changes, casting). `forge/ai/simulation/` and `forge/ai/ability/` are `M5? no` even where they touch rules: plan §3.5
milestone table ports both at M7.

## Source inventory

Measured 2026-09-26 over `*/src/test/java/**/*.java`, excluding `crucible/oracle-java/` and `.claude/worktrees/`. Tests
= method-level `@Test` plus public methods of a class-level-`@Test` class (`CardRequestTest` +9, `HeadlessStartupTest`
+5). 16 of 490 carry `enabled = false`; `BoosterDraftTest` is disabled at class level.

| Java family                  | Files | Tests | Disposition                                                                                    |
| ---------------------------- | ----: | ----: | ---------------------------------------------------------------------------------------------- |
| `forge/ai/simulation/`       |     7 |   142 | `n/a` for M5 — ported at M7 (plan §3.5). `GameSimulationTest` expanded below: rules assertions |
| `forge/card/`                |     9 |    83 | `n/a` — printing/edition lookup; `internal/carddb` indexes rules cards only                    |
| `forge/deck/`                |     6 |    89 | `n/a` — deck-list parsing and generation, M8 (plan §3.5)                                       |
| `forge/ai/ability/`          |    13 |    38 | `n/a` — AI per-API behavior, M7 (plan §3.5)                                                    |
| `forge/ai/` other            |     7 |    28 | `n/a` — AI attack/block/payment/sacrifice choices, M7 AI work                                  |
| `forge/net/`                 |    18 |    16 | `n/a` — no network play (plan §3.4)                                                            |
| `forge/gamesimulationtests/` |    31 |    15 | Per method: `ported` with path, else `todo`. Builder DSL itself superseded by TEST-5 fixtures  |
| `forge/game/` (gui-desktop)  |     5 |    13 | Casting permission, mana refund: rules-relevant, per method                                    |
| everything else              |    25 |    63 | Draft, GUI, image download, deck hints, harness guards, `FCollection`, `gamemodes/net`         |
| `forge-game/src/test/`       |     2 |     3 | `ManaCostBeingPaidTest` (convoke) rules-relevant; `AbilityKeyTest` Java plumbing               |
| **Total**                    |   123 |   490 |                                                                                                |

52 files hold no `@Test`: harness bases (`AITest`, `SimulationTest`, `BaseGameSimulationTest`), `gamesimulationtests`
builders, `forge/net` runners, `planarconquestgenerate` GA generators. No row.

Row tally: 169 rows — 7 `ported`, 0 `superseded`, 141 `n/a`, 21 `todo`. 26 rows `M5? yes`: 7 green, 19 `todo`.

## Rules-relevant classes — one row per method

Paths relative to `forge-gui-desktop/src/test/java/forge/` unless prefixed `forge-game`.

| Java test                                                                                                                                          | Status     | M5? | Go fixture / suite                                                                                                                                             | Note                                                                                                                                    |
| -------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `forge-game:game/mana/ManaCostBeingPaidTest#testPayManaViaConvoke`                                                                                 | ported     | yes | `crucible/internal/engine/convokecolors_test.go` `TestConvokeCreatureColorsPayColoredShardsFirstElseGeneric`                                                   | Go test only. All four Java orders                                                                                                      |
| `game/mana/ManaRefundServiceTest#testManaRefundsToManaPlayer`                                                                                      | superseded | yes | Scenario `mana-payment-fails-atomically`                                                                                                                       | `Game.PayManaCost` mutates the pool only on success, so there is no rewind to refund; mana is the caster's pool, with no per-mana owner |
| `game/spellability/BeamMeUpTest#beamsUpFromTheGraveyardForItsOwnCost`                                                                              | ported     | yes | `crucible/testdata/scenarios/beam-me-up-casts-from-the-graveyard-returning-a-creature/`                                                                        | Exile on leaving the stack is Flashback's replacement                                                                                   |
| `game/spellability/BeamMeUpTest#doesNotBeamUpWithNoCreature`                                                                                       | ported     | yes | `crucible/testdata/scenarios/beam-me-up-needs-a-creature-to-return/`                                                                                           | —                                                                                                                                       |
| `game/spellability/BeamMeUpTest#aMaroonedCreatureCannotBeBeamedUp`                                                                                 | ported     | yes | `crucible/testdata/scenarios/beam-me-up-a-marooned-creature-cannot-be-returned/`                                                                               | `canBeBeamedUp` valid property                                                                                                          |
| `game/spellability/BeamMeUpTest#anotherCreatureStillPaysTheCost`                                                                                   | ported     | yes | `crucible/testdata/scenarios/beam-me-up-another-creature-still-pays-the-cost/`                                                                                 | —                                                                                                                                       |
| `game/spellability/BeamMeUpTest#eachKeywordTargetsItsOwnSpellProperty`                                                                             | todo       | no  | —                                                                                                                                                              | Asserts Java `ValidStackSa` param strings — keyword expansion shape, `internal/keyword`                                                 |
| `game/spellability/CastFromOwnZoneTest#onlyTheOwnerMayFlashback`                                                                                   | ported     | yes | `crucible/testdata/scenarios/flashback-opponent-may-not-cast-from-the-owners-graveyard/`, `flashback-owner-casts-from-own-graveyard-and-it-is-exiled/`         | Owner half. Shaman's Trance and `MayPlay` grants into another graveyard are the `GrantedCastTest` rows                                  |
| `game/spellability/CastFromOwnZoneTest#onlyTheOwnerMayCastAForetoldCard`                                                                           | ported     | yes | `crucible/internal/engine/foretell_test.go` `TestForetoldCardIsCastableOnlyByItsOwnerOnALaterTurn`; scenario `foretell-special-action-exiles-the-card-for-two` | Go test only. Cast options (`castoptions.go`)                                                                                           |
| `game/spellability/GrantedCastTest#aCostReductionAloneDoesNotReachAnotherHand`                                                                     | ported     | yes | `crucible/internal/engine/grantedcast_test.go` `TestACostReductionAloneDoesNotReachAnotherHand`, `TestMayPlayLimitAllowsOneDiscountedCastPerTurn`              | Go test only. Cast options (`castoptions.go`)                                                                                           |
| `game/spellability/GrantedCastTest#aGrantThatReachesTheZoneStillWorks`                                                                             | ported     | yes | `crucible/internal/engine/grantedcast_test.go` `TestAGrantThatReachesTheZoneStillWorks`                                                                        | Go test only. Cast options (`castoptions.go`)                                                                                           |
| `game/spellability/GrantedCastTest#aCostReductionRidesOnTopOfAGrant`                                                                               | ported     | yes | `crucible/internal/engine/grantedcast_test.go` `TestAGrenzoDiscountRidesOnTopOfAHeistGrant`, `TestAHeistGrantLetsTheHeisterSpendAnyTypeOfMana`                 | Go test only. Cast options (`castoptions.go`)                                                                                           |
| `game/spellability/GrantedCastTest#aCostReductionGrantedToTheActivePlayerReachesOnlyTheirOwnCards`                                                 | ported     | yes | `crucible/internal/engine/grantedcast_test.go` `TestWeftwalkingFreeCastReachesOnlyTheActivePlayersCards`, `TestWeftwalkingIsForTheFirstSpellOnly`              | Go test only. Cast options (`castoptions.go`)                                                                                           |
| `gamesimulationtests/ReplacementHandlerTest#testPerpetualEntersTappedReplacementEffect`                                                            | ported     | yes | `crucible/internal/engine/perpetualentersttapped_test.go` `TestPerpetualEntersTappedReplacementTapsTheCardAsItEnters`                                          | Go test only. Real Boareskyr Tollkeeper chain; `Animate` `Replacements$` grants rows (`grantedTriggers.replacements`)                   |
| `ComprehensiveRulesSection103#test_103_3_players_start_at_20_life`                                                                                 | ported     | yes | `crucible/internal/engine/gameloss_test.go` `TestNewGameStartsEveryPlayerAtTwentyLife`                                                                         | Go test only. `NewGame` now seeds `startingLife`                                                                                        |
| `ComprehensiveRulesSection103#test_103_7a_first_player_skips_draw_step_of_first_turn`                                                              | ported     | yes | `crucible/internal/engine/turn_test.go` `TestDrawSkipsFirstPlayerFirstTurnTwoPlayer`                                                                           | Go test only                                                                                                                            |
| `ComprehensiveRulesSection104#test_104_2a_player_wins_if_all_opponents_left_even_if_he_couldnt_win`                                                | ported     | yes | `crucible/testdata/scenarios/cant-win-abyssal-persecutor-opponent-survives-until-conceding/`                                                                   | `Game.Concede`, `concede` verb, `gameloss.go`                                                                                           |
| `ComprehensiveRulesSection104#test_104_2b_effect_may_state_that_player_wins`                                                                       | ported     | yes | `crucible/internal/engine/winsgameeffect_test.go` `TestWinsGameEffectEndsTheGameImmediatelyWithOpponentsStillAlive`                                            | Go test only. Near-Death Experience upkeep trigger itself not                                                                           |
| `ComprehensiveRulesSection104#test_104_3b_player_with_zero_life_loses_the_game`                                                                    | ported     | yes | `crucible/testdata/scenarios/life-loss-at-zero/`                                                                                                               | —                                                                                                                                       |
| `ComprehensiveRulesSection104#test_104_3b_player_with_less_than_zero_life_loses_the_game`                                                          | ported     | yes | `crucible/testdata/scenarios/life-loss-below-zero-also-loses/`                                                                                                 | —                                                                                                                                       |
| `ComprehensiveRulesSection104#test_104_3b_player_with_less_than_zero_life_loses_the_game_only_when_a_player_receives_priority`                     | ported     | yes | `crucible/testdata/scenarios/sba-negative-life-mid-resolution-helix-on-self-survives/`                                                                         | —                                                                                                                                       |
| `ComprehensiveRulesSection104#test_104_3b_player_with_less_than_zero_life_loses_the_game_only_when_a_player_receives_priority_variant_with_combat` | ported     | yes | `crucible/testdata/scenarios/sba-lifelink-and-damage-simultaneous-survive/`                                                                                    | —                                                                                                                                       |
| `ComprehensiveRulesSection104#test_104_3c_player_who_draws_card_with_empty_library_loses`                                                          | ported     | yes | `crucible/testdata/scenarios/draw-from-empty-library-loses/`                                                                                                   | —                                                                                                                                       |
| `ComprehensiveRulesSection104#test_104_3c_player_who_draws_more_cards_than_library_contains_draw_as_much_as_possible_and_loses`                    | ported     | yes | `crucible/testdata/scenarios/sba-draw-more-than-library-draws-all-then-loses/`                                                                                 | —                                                                                                                                       |
| `ComprehensiveRulesSection104#test_104_3d_player_with_ten_poison_counters_loses`                                                                   | ported     | yes | `crucible/testdata/scenarios/poison-loss/`                                                                                                                     | —                                                                                                                                       |
| `ComprehensiveRulesSection104#test_104_3d_player_with_more_than_ten_poison_counters_loses`                                                         | ported     | yes | `crucible/testdata/scenarios/sba-poison-eleven-loses/`                                                                                                         | —                                                                                                                                       |
| `ComprehensiveRulesSection104#test_104_3e_effect_may_state_that_player_loses`                                                                      | ported     | yes | `crucible/internal/engine/losesgameeffect_test.go` `TestLosesGameEffectSetsLostAndTheSurvivorWins`                                                             | Go test only. Final Fortune delayed trigger itself not                                                                                  |
| `ComprehensiveRulesSection104#test_104_3f_if_a_player_would_win_and_lose_simultaneously_he_loses`                                                  | n/a        | no  | —                                                                                                                                                              | `enabled = false` upstream: Forge fails it, oracle cannot confirm (PORT-8)                                                              |

`ComprehensiveRulesSection10x` = `gamesimulationtests/comprehensiverules/`.

## Other classes — one row per class

| Java test                                        | Tests | Status | M5? | Go fixture / suite | Note                                                                                        |
| ------------------------------------------------ | ----: | ------ | --- | ------------------ | ------------------------------------------------------------------------------------------- |
| `forge-game:game/ability/AbilityKeyTest`         |     2 | n/a    | no  | —                  | Java enum/`Map` plumbing; Go trigger run params are typed fields (GO-8)                     |
| `game/event/GameEventSerializationTest`          |     1 | n/a    | no  | —                  | Network serializability of Java event records; no network play (plan §3.4)                  |
| `ai/simulation/GameSimulatorSpellChoiceTest`     |     7 | n/a    | no  | —                  | Simulator keeps kicker/target choices. M7 (plan §3.5)                                       |
| `ai/simulation/GameStateEvaluatorTest`           |     4 | n/a    | no  | —                  | AI board scoring. M7                                                                        |
| `ai/simulation/OnePlaySafetyCheckerTest`         |    14 | n/a    | no  | —                  | AI lethal-self-harm check. M7                                                               |
| `ai/simulation/SpellAbilityPickerSimulationTest` |    34 | n/a    | no  | —                  | AI spell choice. M7                                                                         |
| `ai/simulation/TerminalScoreDiscountTest`        |     5 | n/a    | no  | —                  | AI win/loss score discount. M7                                                              |
| `ai/ability/BecomeMonarchAiTest`                 |     3 | n/a    | no  | —                  | M7 (plan §3.5)                                                                              |
| `ai/ability/BlightAiTest`                        |     2 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/ChangeZoneAiTest`                    |     3 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/ChoosePlayerAiTest`                  |     1 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/CloneOpponentCreatureAiTest`         |     3 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/CountersProliferateAiTest`           |     4 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/CountersPutAiTest`                   |     1 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/DamageDealAiTest`                    |     4 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/DestroyAiTest`                       |     2 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/StoriedCardsTest`                    |     3 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/StoriedTest`                         |     7 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/TriggerLifeGateTest`                 |     1 | n/a    | no  | —                  | M7                                                                                          |
| `ai/ability/UnlockDoorAiTest`                    |     4 | n/a    | no  | —                  | M7                                                                                          |
| `ai/AIIntegrationTests`                          |    10 | n/a    | no  | —                  | AI attack/mode choices (escalate, spree, entwine). M7 AI work                               |
| `ai/attacking/BasicAttackTests`                  |     4 | n/a    | no  | —                  | AI attack decisions. M7                                                                     |
| `ai/blocking/BasicBlockTests`                    |     4 | n/a    | no  | —                  | AI block decisions. M7                                                                      |
| `ai/controller/AutoPaymentTest`                  |     4 | n/a    | no  | —                  | AI mana-source preference. M7                                                               |
| `ai/controller/ReserveManaSourcesTest`           |     3 | n/a    | no  | —                  | AI mana reservation. M7                                                                     |
| `ai/sacrifice/AiSacrificeDecisionTest`           |     3 | n/a    | no  | —                  | AI sacrifice choice. M7                                                                     |
| `card/CardDbCardMockTestCase`                    |    54 | n/a    | no  | —                  | Printing lookup by edition/art/collector no. Plan §3.5 M2 row lists it; not ported          |
| `card/CardDbLazyCardLoadingCardMockTestCase`     |     4 | n/a    | no  | —                  | Lazy printing load. Same                                                                    |
| `card/CardDbPerformanceTests`                    |     4 | n/a    | no  | —                  | Benchmarks, 2 disabled                                                                      |
| `card/CardDbWithNoImageCardDbMockTestCase`       |     1 | n/a    | no  | —                  | Image presence                                                                              |
| `card/CardEditionCollectionCardMockTestCase`     |     2 | n/a    | no  | —                  | Edition selection                                                                           |
| `card/CardRequestTest`                           |    13 | n/a    | no  | —                  | `Name\|SET\|art` request strings. Nearest: `internal/deck/parsing_test.go` (edition only)   |
| `card/CardTranslationTest`                       |     5 | n/a    | no  | —                  | Localized card text                                                                         |
| `deck/DeckRecognizerTest`                        |    84 | n/a    | no  | —                  | Deck-list text recognition. M8 (plan §3.5)                                                  |
| `deck/DeckRecognizerPortablePatternTest`         |     1 | n/a    | no  | —                  | M8                                                                                          |
| `deck/CommanderBracketDataTest`                  |     1 | n/a    | no  | —                  | Commander bracket list. M8                                                                  |
| `deck/generate/Generate2ColorDeckTest`           |     1 | n/a    | no  | —                  | Deck generation, disabled. M8                                                               |
| `deck/generate/Generate3ColorDeckTest`           |     1 | n/a    | no  | —                  | Same                                                                                        |
| `deck/generate/Generate5ColorDeckTest`           |     1 | n/a    | no  | —                  | Same                                                                                        |
| `net/AbuseLimitsTest`                            |     1 | n/a    | no  | —                  | No network play (plan §3.4)                                                                 |
| `net/DeltaSyncUnitTest`                          |     4 | n/a    | no  | —                  | Same                                                                                        |
| `net/LobbySlotAuthorizationTest`                 |     1 | n/a    | no  | —                  | Same                                                                                        |
| `net/NetworkPlayIntegrationTest`                 |    10 | n/a    | no  | —                  | Same                                                                                        |
| `gamemodes/net/WireClassFilterTest`              |     2 | n/a    | no  | —                  | Same                                                                                        |
| `gamemodes/net/WireDeserializationTest`          |     4 | n/a    | no  | —                  | Same                                                                                        |
| `BoosterDraft1Test`                              |     1 | n/a    | no  | —                  | Draft, disabled                                                                             |
| `BoosterDraftTest`                               |     1 | n/a    | no  | —                  | Draft, class disabled                                                                       |
| `CardRankerTest`                                 |     1 | n/a    | no  | —                  | Draft pick ranking                                                                          |
| `FCollectionTest`                                |     1 | todo   | no  | —                  | Remove while iterating `threadSafeIterable`. `pkg/collect` OrderedSet has no such test (M1) |
| `GuiDownloadPicturesLQTest`                      |     1 | n/a    | no  | —                  | GUI, disabled                                                                               |
| `GuiDownloadSetPicturesLQTest`                   |     1 | n/a    | no  | —                  | GUI, disabled                                                                               |
| `GuiProgressBarWindowTest`                       |     1 | n/a    | no  | —                  | GUI                                                                                         |
| `HeadlessStartupTest`                            |     5 | n/a    | no  | —                  | Desktop app headless startup                                                                |
| `PanelTest`                                      |     1 | n/a    | no  | —                  | GUI, disabled                                                                               |
| `RunTest`                                        |     1 | n/a    | no  | —                  | Disabled, body commented out                                                                |
| `TestSuiteIntegrityTest`                         |     1 | n/a    | no  | —                  | TestNG/PowerMock class-inspection guard; `go test` has no silent-skip mode                  |
| `TinyTest`                                       |     1 | n/a    | no  | —                  | Harness smoke (`assert true`)                                                               |
| `gui/ListChooserTest`                            |     1 | n/a    | no  | —                  | GUI, disabled                                                                               |
| `gui/download/CdnUuidCacheTest`                  |    19 | n/a    | no  | —                  | Image CDN cache                                                                             |
| `gui/download/ScryfallSetSyncTest`               |    10 | n/a    | no  | —                  | Image set sync                                                                              |
| `gui/game/CardDetailPanelTest`                   |     1 | n/a    | no  | —                  | GUI, disabled                                                                               |
| `item/DeckHintsTest`                             |     7 | n/a    | no  | —                  | Deck-building hints. M8                                                                     |
| `model/FModelTest`                               |     3 | n/a    | no  | —                  | `FModel` singleton (GO-2), all disabled                                                     |

## GameSimulationTest — one row per method

`ai/simulation/GameSimulationTest`, 78 tests. Real rules assertions (plan §3.4), but `M5? no`: plan §3.5 ports
`ai/simulation` at M7. Cards column = names passed to `addCard*` helpers, for fixture search at M7; not the full cast
(`createCard` and other helpers missed), `—` = none via `addCard*`.

| Java test                                       | Status | M5? | Go fixture / suite | Cards                                                                                                                          |
| ----------------------------------------------- | ------ | --- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| `testActivateAbilityTriggers`                   | n/a    | no  | —                  | Plains                                                                                                                         |
| `testStaticAbilities`                           | n/a    | no  | —                  | Plains, Spear of Heliod                                                                                                        |
| `testStaticEffectsMonstrous`                    | n/a    | no  | —                  | —                                                                                                                              |
| `testEquippedAbilities`                         | n/a    | no  | —                  | Whispersilk Cloak                                                                                                              |
| `testEnchantedAbilities`                        | n/a    | no  | —                  | Lifelink                                                                                                                       |
| `testEtbTriggers`                               | n/a    | no  | —                  | Black Knight, Swamp                                                                                                            |
| `testSimulateUnmorph`                           | n/a    | no  | —                  | —                                                                                                                              |
| `testFindingOwnCard`                            | n/a    | no  | —                  | Skull Fracture, Runeclaw Bear, Swamp                                                                                           |
| `testPlaneswalkerAbilities`                     | n/a    | no  | —                  | Sorin, Solemn Visitor                                                                                                          |
| `testPlaneswalkerEmblems`                       | n/a    | no  | —                  | Gideon, Ally of Zendikar                                                                                                       |
| `testManifest`                                  | n/a    | no  | —                  | Plains, Soul Summons, Ornithopter                                                                                              |
| `testManifest2`                                 | n/a    | no  | —                  | Plains, Soul Summons                                                                                                           |
| `testManifest3`                                 | n/a    | no  | —                  | Plains, Soul Summons, Dryad Arbor                                                                                              |
| `testTypeOfPermanentChanging`                   | n/a    | no  | —                  | —                                                                                                                              |
| `testDistributeCountersAbility`                 | n/a    | no  | —                  | —                                                                                                                              |
| `testDamagePreventedTrigger`                    | n/a    | no  | —                  | Mountain, Lightning Bolt                                                                                                       |
| `testChosenColors`                              | n/a    | no  | —                  | Hall of Triumph                                                                                                                |
| `testDarkDepthsCopy`                            | n/a    | no  | —                  | Swamp, Dark Depths, Thespian's Stage                                                                                           |
| `testThespianStageSelfCopy`                     | n/a    | no  | —                  | Swamp, Thespian's Stage                                                                                                        |
| `testDash`                                      | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testTokenAbilities`                            | n/a    | no  | —                  | Forest, Call the Scions                                                                                                        |
| `testMarkedDamage`                              | n/a    | no  | —                  | Mountain, Shock                                                                                                                |
| `testLifelinkDamageSpell`                       | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testLifelinkDamageSpellMultiplier`             | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testLifelinkDamageSpellRedirected`             | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testLifelinkDamageSpellMultipleDamage`         | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testTransform`                                 | n/a    | no  | —                  | Swamp                                                                                                                          |
| `testEnergy`                                    | n/a    | no  | —                  | Island                                                                                                                         |
| `testFloatingMana`                              | n/a    | no  | —                  | Swamp, Dark Ritual, Dark Confidant, Deathrite Shaman                                                                           |
| `testEnKor`                                     | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testRazia`                                     | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testRazia2`                                    | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testMassRemovalVsKalitas`                      | n/a    | no  | —                  | Kalitas, Traitor of Ghet, Plains, Aboroth, Wrath of God                                                                        |
| `testKalitasNumberOfTokens`                     | n/a    | no  | —                  | Kalitas, Traitor of Ghet, Anointed Procession, Swamp, Mountain, Raging Goblin, Fatal Push, Electrify                           |
| `testPlayerXCount`                              | n/a    | no  | —                  | Bloodghast                                                                                                                     |
| `testDeathsShadow`                              | n/a    | no  | —                  | Platinum Angel, Death's Shadow                                                                                                 |
| `testBludgeonBrawlLatticeAura`                  | n/a    | no  | —                  | Lifelink, Mycosynth Lattice, Bludgeon Brawl                                                                                    |
| `testBludgeonBrawlLatticeCurse`                 | n/a    | no  | —                  | Mycosynth Lattice, Bludgeon Brawl                                                                                              |
| `testBludgeonBrawlFortification`                | n/a    | no  | —                  | Mountain, Darksteel Garrison, Bludgeon Brawl                                                                                   |
| `testBludgeonBrawlFortificationDryad`           | n/a    | no  | —                  | Dryad Arbor, Darksteel Garrison, Bludgeon Brawl                                                                                |
| `testRiotEnchantment`                           | n/a    | no  | —                  | Rhythm of the Wild, Mountain, Forest                                                                                           |
| `testTeysaKarlovXathridNecromancer`             | n/a    | no  | —                  | Teysa Karlov, Xathrid Necromancer, Plains, Wrath of God                                                                        |
| `testDoubleTeysaKarlovXathridNecromancer`       | n/a    | no  | —                  | Teysa Karlov, Xathrid Necromancer, Plains, Swamp                                                                               |
| `testTeysaKarlovGitrogMonster`                  | n/a    | no  | —                  | Teysa Karlov, The Gitrog Monster, Dryad Arbor, Plains, Armageddon                                                              |
| `testTeysaKarlovGitrogMonsterGitrogDies`        | n/a    | no  | —                  | Teysa Karlov, The Gitrog Monster, Dryad Arbor, Plains, Wrath of God                                                            |
| `testTeysaKarlovGitrogMonsterTeysaDies`         | n/a    | no  | —                  | Teysa Karlov, The Gitrog Monster, Dryad Arbor, Plains, Wrath of God                                                            |
| `testCloneTransform`                            | n/a    | no  | —                  | Forest, Island, Cytoshape, Moonmist                                                                                            |
| `testVolrathsShapeshifter`                      | n/a    | no  | —                  | Volrath's Shapeshifter, Abattoir Ghoul, Plains                                                                                 |
| `testSparkDoubleAndGideon`                      | n/a    | no  | —                  | Plains, Island, Gideon Blackblade, Spark Double                                                                                |
| `testVituGhaziAndCytoshape`                     | n/a    | no  | —                  | Plains, Island, Forest, Wastes, Awakening of Vitu-Ghazi, Cytoshape, Raging Goblin                                              |
| `testNecroticOozeActivateOnce`                  | n/a    | no  | —                  | Swamp, Forest, Basking Rootwalla, Necrotic Ooze                                                                                |
| `testEpochrasite`                               | n/a    | no  | —                  | Swamp, Epochrasite, Animate Dead, Plains, Island, Dimir Doppelganger, Jushi Apprentice, Runeclaw Bear, Nezumi Shortfang        |
| `testStaticMultiPump`                           | n/a    | no  | —                  | Creakwood Liege                                                                                                                |
| `testPathtoExileActofTreason`                   | n/a    | no  | —                  | Serra Angel, Act of Treason, Path to Exile, Plateau, Island, Forest                                                            |
| `testAmassTrigger`                              | n/a    | no  | —                  | Island                                                                                                                         |
| `testEverAfterWithWaywardServant`               | n/a    | no  | —                  | Swamp                                                                                                                          |
| `testCantBePrevented`                           | n/a    | no  | —                  | Mountain, Pyroclasm                                                                                                            |
| `testAlphaBrawl`                                | n/a    | no  | —                  | Mountain                                                                                                                       |
| `testGlarecaster`                               | n/a    | no  | —                  | Plains, Mountain, Inferno                                                                                                      |
| `testETBCounterMowu`                            | n/a    | no  | —                  | Forest                                                                                                                         |
| `testETBCounterCorpsejack`                      | n/a    | no  | —                  | Forest, Swamp                                                                                                                  |
| `testETBCounterCorpsejackMentor`                | n/a    | no  | —                  | Swamp                                                                                                                          |
| `testHushbringer`                               | n/a    | no  | —                  | Naban, Dean of Iteration, Hushbringer, Ingenious Artillerist, Spellbook, Memnarch, Forest, Island, Mountain, Genesis Ultimatum |
| `testLKITransformableTokenCopy`                 | n/a    | no  | —                  | Ratadrabik of Urborg, Island, Swamp, Murder                                                                                    |
| `testBasicSpellFizzling`                        | n/a    | no  | —                  | Swamp, Bear Cub, Annihilate, Island, Mage's Guile                                                                              |
| `testControlLayerDependency`                    | n/a    | no  | —                  | Bear Cub, Island, Vedalken Orrery, Mind Control, Confiscate                                                                    |
| `testTypeLayerDependency`                       | n/a    | no  | —                  | Breeding Pool, Life and Limb, Blood Moon, Shroofus Sproutsire                                                                  |
| `testHenzie`                                    | n/a    | no  | —                  | Henzie "Toolbox" Torre, Wastes, Plains, Serra Angel                                                                            |
| `testVoloJournal`                               | n/a    | no  | —                  | Island, Mountain, Forest, Volo, Itinerant Scholar, Cathartic Adept, Drowner Initiate, Atog                                     |
| `testGameCopyPreservesFaceDownExileState`       | n/a    | no  | —                  | Behold the Multiverse                                                                                                          |
| `testGameCopyWiresPlayerEffectCards`            | n/a    | no  | —                  | —                                                                                                                              |
| `testGameCopyPreservesCardIds`                  | n/a    | no  | —                  | Plains, Runeclaw Bear, Island, Shock, Forest                                                                                   |
| `testCounterAddedAllTriggerRespectsCounterType` | n/a    | no  | —                  | Swamp, Forest, Runeclaw Bear, Virulent Wound, Battlegrowth                                                                     |
| `testLibraryMovementLosesManaAbility`           | n/a    | no  | —                  | —                                                                                                                              |
| `testManaAbilityKeptWithoutLibraryMovement`     | n/a    | no  | —                  | —                                                                                                                              |
| `testEffectLibraryMovement`                     | n/a    | no  | —                  | —                                                                                                                              |
| `testEffectLibraryMovementFollowsZoneParams`    | n/a    | no  | —                  | —                                                                                                                              |
| `testCostLibraryMovement`                       | n/a    | no  | —                  | —                                                                                                                              |

## M5 backlog

None: every `M5? yes` row is `ported` or `superseded`. The two `todo` rows left are `M5? no` (owned by a later
milestone).

## Related

- [03-testing-standards](../guidelines/03-testing-standards.md) — TEST-5 fixture format
- [port-log/game-state](port-log/game-state.md) — "Not ported yet"
