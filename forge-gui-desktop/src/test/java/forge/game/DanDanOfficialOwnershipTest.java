package forge.game;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.ability.ApiType;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.CardCollection;
import forge.game.card.CardCopyService;
import forge.game.card.CardPredicates;
import forge.game.card.CardProperty;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.RegisteredPlayer;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/** Official-card ownership lifecycle checks across Item 3 recipients and Item 4 zone transitions. */
public class DanDanOfficialOwnershipTest extends AITest {
    @Test
    public void sharedLandPlayPermissionsPrepareIncomingWithoutChangingLiveSource() {
        for (ZoneType origin : List.of(ZoneType.Library, ZoneType.Graveyard)) {
            for (boolean prevent : List.of(true, false)) {
                Game game = newGame(true);
                Player first = game.getRegisteredPlayers().get(0);
                Player recipient = game.getRegisteredPlayers().get(1);
                game.getPhaseHandler().devModeSet(PhaseType.MAIN1, recipient);
                Card land = addCardToZone("Island", first, origin);
                Card permission = addCard(origin == ZoneType.Library ? "Courser of Kruphix" : "Crucible of Worlds", recipient);
                Card opponentsBurgeoning = addCard("Burgeoning", first);
                Card ownBurgeoning = addCard("Burgeoning", recipient);
                game.getTriggerHandler().registerActiveTrigger(opponentsBurgeoning, false);
                game.getTriggerHandler().registerActiveTrigger(ownBurgeoning, false);
                boolean[] inspected = {false};
                recipient.dangerouslySetController(new PlayerControllerAi(game, recipient,
                        new LobbyPlayerAi(recipient.getName(), null)) {
                    @Override
                    public boolean confirmReplacementEffect(ReplacementEffect replacement, SpellAbility effect,
                            GameEntity affected, String question) {
                        assertSame(game.getCardState(land), land);
                        assertTrue(first.getCardsIn(origin).contains(land));
                        assertEquals(land.getOwners(), game.getZoneParticipants(land.getZone()));
                        assertNull(land.getExplicitController(), "the held land play leaves source control unchanged");
                        assertFalse(land.isFaceDown());
                        assertFalse(land.mayPlay(recipient).isEmpty(), "the live source retains its real play permission");
                        Card incoming = (Card) affected;
                        assertNotSame(incoming, land);
                        assertSame(incoming.getOwner(), recipient);
                        assertSame(incoming.getExplicitController(), recipient);
                        assertFalse(incoming.mayPlay(recipient).isEmpty(), "the incoming candidate retains the granted play option");
                        Card sourceLki = (Card) effect.getReplacingObject(AbilityKey.CardLKI);
                        assertTrue(sourceLki.isOwnedBy(first) && sourceLki.isOwnedBy(recipient));
                        assertNull(sourceLki.getExplicitController(), "entry replacements receive the original source LKI");
                        inspected[0] = true;
                        return prevent;
                    }
                });
                permission.setSVar("HoldEntry", "DB$ Cleanup");
                permission.addReplacementEffect(ReplacementHandler.parseReplacement(
                        "Event$ Moved | ActiveZones$ Battlefield | Origin$ " + origin
                                + " | Destination$ Battlefield | ValidCard$ Land.YouOwn | Optional$ True | Prevent$ True"
                                + " | ReplaceWith$ HoldEntry"
                                + " | Description$ Hold this land entry.", permission, false));
                game.getAction().checkStaticAbilities();
                SpellAbility play = land.getAllPossibleAbilities(recipient, true).stream()
                        .filter(SpellAbility::isLandAbility).filter(sa -> sa.getMayPlay() != null).findFirst().orElseThrow();
                assertTrue(play.canPlay(), "the real permanent grants B permission to play the shared land");
                play.resolve();
                game.getTriggerHandler().runWaitingTriggers();
                assertTrue(inspected[0], "the real play reached the held entry replacement");
                assertTrue(game.getStack().hasSourceOnStack(opponentsBurgeoning, SpellAbility::isTrigger),
                        "A's real Burgeoning sees B play a land");
                assertFalse(game.getStack().hasSourceOnStack(ownBurgeoning, SpellAbility::isTrigger),
                        "B's real Burgeoning does not treat B's own land play as an opponent's");
                assertNull(land.getExplicitController());
                assertTrue(land.isOwnedBy(first) && land.isOwnedBy(recipient));
                Card result = game.getCardState(land);
                assertSame(result.getZone(), recipient.getZone(prevent ? origin : ZoneType.Battlefield));
                if (!prevent) {
                    assertSame(result.getOwner(), recipient);
                    assertSame(result.getExplicitController(), recipient);
                }
            }
        }
    }

    @Test
    public void sharedManifestAndCloakPrepareFaceDownIncomingWithoutChangingLiveSource() {
        for (boolean cloak : List.of(false, true)) {
            for (boolean prevent : List.of(true, false)) {
                Game game = newGame(true);
                Player first = game.getRegisteredPlayers().get(0);
                Player recipient = game.getRegisteredPlayers().get(1);
                Card selected = addCardToZone(cloak ? "Think Twice" : "Mulldrifter", first, ZoneType.Library);
                Card gate = addCardToZone("Veteran Ghoulcaller", recipient, ZoneType.Command);
                boolean[] inspected = {false};
                recipient.dangerouslySetController(new PlayerControllerAi(game, recipient,
                        new LobbyPlayerAi(recipient.getName(), null)) {
                    @Override
                    public boolean confirmReplacementEffect(ReplacementEffect replacement, SpellAbility effect,
                            GameEntity affected, String question) {
                        assertSame(game.getCardState(selected), selected);
                        assertTrue(first.getCardsIn(ZoneType.Library).contains(selected));
                        assertTrue(selected.isOwnedBy(first) && selected.isOwnedBy(recipient));
                        assertNull(selected.getExplicitController());
                        assertFalse(selected.isFaceDown(), "a held face-down entry preserves the face-up library source");
                        assertFalse(selected.isManifested());
                        assertFalse(selected.isCloaked());
                        Card incoming = (Card) affected;
                        assertNotSame(incoming, selected);
                        assertTrue(incoming.isFaceDown());
                        assertTrue(incoming.isCreature());
                        assertEquals(incoming.getNetPower(), 2);
                        assertEquals(incoming.getNetToughness(), 2);
                        assertEquals(incoming.isManifested(), !cloak);
                        assertEquals(incoming.isCloaked(), cloak);
                        assertSame(incoming.getOwner(), recipient);
                        assertSame(incoming.getExplicitController(), recipient);
                        Card sourceLki = (Card) effect.getReplacingObject(AbilityKey.CardLKI);
                        assertTrue(sourceLki.isOwnedBy(first) && sourceLki.isOwnedBy(recipient));
                        assertFalse(sourceLki.isFaceDown());
                        assertNull(sourceLki.getExplicitController());
                        if (cloak) assertTrue(incoming.hasKeyword("Ward:2"));
                        inspected[0] = true;
                        return prevent;
                    }
                });
                gate.setSVar("HoldEntry", "DB$ Cleanup");
                gate.addReplacementEffect(ReplacementHandler.parseReplacement(
                        "Event$ Moved | ActiveZones$ Command | Origin$ Library | Destination$ Battlefield"
                                + " | ValidCard$ Card.YouOwn | Optional$ True | Prevent$ True"
                                + " | ReplaceWith$ HoldEntry"
                                + " | Description$ Hold this face-down entry.", gate, false));
                if (cloak) {
                    Card coat = addCard("Cryptic Coat", recipient);
                    SpellAbility ability = coat.getTriggers().get(0).ensureAbility();
                    ability.setActivatingPlayer(recipient);
                    AbilityUtils.resolve(ability);
                } else {
                    Card summons = addCardToZone("Soul Summons", recipient, ZoneType.Hand);
                    resolveStacked(game, summons.getSpells().get(0), recipient);
                }
                assertTrue(inspected[0], "the real manifest/cloak script reached the held replacement; cloak=" + cloak
                        + ", prevent=" + prevent + ", actual=" + game.getCardState(selected).getZone()
                        + ", faceDown=" + game.getCardState(selected).isFaceDown());
                assertFalse(selected.isFaceDown());
                assertFalse(selected.isManifested());
                assertFalse(selected.isCloaked());
                assertNull(selected.getExplicitController());
                Card result = game.getCardState(selected);
                assertSame(result.getZone(), recipient.getZone(prevent ? ZoneType.Library : ZoneType.Battlefield));
                if (!prevent) {
                    assertTrue(result.isFaceDown());
                    assertEquals(result.isManifested(), !cloak);
                    assertEquals(result.isCloaked(), cloak);
                    if (cloak) assertTrue(result.hasKeyword("Ward:2"));
                    assertFalse(game.getStack().hasSimultaneousStackEntries(), "face-down Mulldrifter creates no draw trigger");
                }
            }
        }
    }

    @Test
    public void eitherSharedGraveyardOwnerCanEnumerateAndPlayThinkTwiceFlashback() {
        Game game = newGame(true);
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        Card thinkTwice = addCardToZone("Think Twice", first, ZoneType.Graveyard);
        thinkTwice.setOwners(List.of(first, second));

        for (Player player : List.of(first, second)) {
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, player);
            boolean offered = false;
            for (Card card : player.getZone(ZoneType.Graveyard).getCardsPlayerCanActivate(player)) {
                offered |= card == thinkTwice;
            }
            assertTrue(offered, player + " can select shared Think Twice for flashback");
            SpellAbility flashback = thinkTwice.getAllPossibleAbilities(player, false).stream()
                    .filter(SpellAbility::isFlashback).findFirst().orElseThrow();
            assertTrue(flashback.canPlay(true), player + " can actually play the generated flashback ability");
        }

        Game ordinary = newGame(false);
        Player owner = ordinary.getRegisteredPlayers().get(0);
        Player opponent = ordinary.getRegisteredPlayers().get(1);
        Card privateThinkTwice = addCardToZone("Think Twice", owner, ZoneType.Graveyard);
        for (Player player : List.of(owner, opponent)) {
            ordinary.getPhaseHandler().devModeSet(PhaseType.MAIN1, player);
            SpellAbility flashback = GameActionUtil.getAlternativeCosts(privateThinkTwice.getSpells().get(0), player, false).stream()
                    .filter(SpellAbility::isFlashback).findFirst().orElseThrow();
            assertEquals(flashback.canPlay(true), player == owner,
                    "ordinary flashback remains restricted to its sole owner");
        }
        Card stolenIsland = addCard("Island", owner);
        stolenIsland.setController(opponent, ordinary.getNextTimestamp());
        SpellAbility mana = stolenIsland.getManaAbilities().get(0);
        mana.setActivatingPlayer(owner);
        assertFalse(mana.canPlay(true), "the owner cannot activate an Island controlled by the opponent");
        mana.setActivatingPlayer(opponent);
        assertTrue(mana.canPlay(true), "the actual controller can activate the stolen Island");
    }

    @Test
    public void ownerPropertiesUseMembershipAndControlPropertiesKeepTheirSourceSnapshot() {
        Game game = newGame(true);
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        Card shared = addCardToZone("Think Twice", first, ZoneType.Exile);
        shared.setOwners(List.of(first, second));
        Card source = addCardToZone("Thought Scour", first, ZoneType.Hand);
        SpellAbility ability = source.getSpells().get(0);
        ability.setActivatingPlayer(first);
        ability.getTargets().add(second);
        source.addRemembered(second);
        for (Player player : List.of(first, second)) {
            assertTrue(CardPredicates.isOwner(player).test(shared));
            assertTrue(CardProperty.cardHasProperty(shared, "YouOwn", player, source, ability));
            assertFalse(CardProperty.cardHasProperty(shared, "YouDontOwn", player, source, ability));
            assertTrue(CardProperty.cardHasProperty(shared, "OppOwn", player, source, ability));
            assertTrue(CardProperty.cardHasProperty(shared, "YouCtrl", player, source, ability));
            assertFalse(CardProperty.cardHasProperty(shared, "YouDontCtrl", player, source, ability));
            assertTrue(CardProperty.cardHasProperty(shared, "OppCtrl", player, source, ability));
            assertTrue(CardProperty.cardHasProperty(shared, "ControlledBy You", player, source, ability));
        }
        assertTrue(CardProperty.cardHasProperty(shared, "TargetedPlayerOwn", first, source, ability));
        assertTrue(CardProperty.cardHasProperty(shared, "RememberedPlayerOwn", first, source, ability));
        shared.setController(first, game.getNextTimestamp());
        Card snapshot = CardCopyService.getLKICopy(shared);
        shared.setOwner(second);
        shared.setController(second, game.getNextTimestamp());
        game.addChangeZoneLKIInfo(snapshot);
        assertTrue(CardProperty.cardHasProperty(shared, "YouOwn", second, source, ability),
                "ownership properties use the current object");
        assertFalse(CardProperty.cardHasProperty(shared, "YouOwn", first, source, ability));
        assertTrue(CardProperty.cardHasProperty(shared, "YouCtrl", first, source, ability),
                "control properties still use the registered source snapshot");
        assertFalse(CardProperty.cardHasProperty(shared, "YouCtrl", second, source, ability));
        assertTrue(CardProperty.cardHasProperty(shared, "OwnerDoesntControl", first, source, ability),
                "the current sole owner differs from the controller in the source snapshot");
        assertTrue(CardProperty.cardHasProperty(snapshot, "YouOwn", first, source, ability));
        assertTrue(CardProperty.cardHasProperty(snapshot, "YouOwn", second, source, ability));
        assertTrue(CardPredicates.ownerLives().test(snapshot));
    }

    @Test
    public void fengrafAcquisitionSurvivesMetamorphoseAndUnsubstantiate() {
        Game game = newGame(true);
        Player caster = game.getRegisteredPlayers().get(0);
        Player recipient = game.getRegisteredPlayers().get(1);
        addCard("Island", caster);
        List<Card> recipientIslands = List.of(addCard("Island", recipient), addCard("Island", recipient));
        game.getAction().checkStateEffects(true);

        Card fishSpell = addCardToZone("Dandân", caster, ZoneType.Hand);
        resolveStacked(game, fishSpell.getSpells().get(0), caster);
        Card fish = currentCard(game, fishSpell.getId());
        assertTrue(caster.getCardsIn(ZoneType.Battlefield).contains(fish), "A casts Dandân onto the battlefield");
        assertSame(fish.getOwner(), caster);
        assertSame(fish.getController(), caster);

        game.copyLastState();
        game.getAction().moveToGraveyard(fish, null);
        fish = currentCard(game, fishSpell.getId());
        assertTrue(game.getCardsIn(ZoneType.Graveyard).contains(fish), "the cast fish dies into the shared graveyard");
        assertEquals(game.getCardsIn(ZoneType.Graveyard).stream().filter(Card::isCreature).count(), 1L,
                "Fengraf has exactly one eligible creature and its random choice is deterministic");

        Card fengraf = addCard("Haunted Fengraf", recipient);
        SpellAbility fengrafAbility = findChangeZoneAbility(fengraf);
        assertTrue(fengrafAbility.isActivatedAbility());
        assertFalse(fengrafAbility.isManaAbility(), "select Fengraf's retrieval ability, not its {T}: add {C} ability");
        assertEquals(fengrafAbility.getApi(), ApiType.ChangeZone);
        assertEquals(fengrafAbility.getParam("Origin"), "Graveyard");
        assertEquals(fengrafAbility.getParam("Destination"), "Hand");
        assertEquals(fengrafAbility.getParam("AtRandom"), "True");
        fengrafAbility.setActivatingPlayer(recipient);
        SpellAbility registeredFengrafAbility = putAbilityOnStack(game, fengrafAbility);
        assertSame(registeredFengrafAbility.getActivatingPlayer(), recipient);
        game.getStack().resolveStack();

        assertTrue(game.getStack().isEmpty(), "Fengraf's ability has completely resolved before ability legality is checked");
        fish = currentCard(game, fishSpell.getId());
        assertTrue(recipient.getCardsIn(ZoneType.Hand).contains(fish),
                "the real Fengraf effect returns the sole common-graveyard creature to B's hand");
        assertFalse(caster.getCardsIn(ZoneType.Hand).contains(fish));
        assertSame(fish.getOwner(), recipient, "B acquires ownership when the fish enters B's hand");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, recipient);
        assertTrue(recipient.canCastSorcery(), "B has a legal sorcery-speed window for the acquired Dandân");
        int fishId = fish.getId();
        assertTrue(fish.getAllPossibleAbilities(recipient, true).stream()
                        .anyMatch(ability -> ability.isSpell() && ability.getHostCard().getId() == fishId),
                "B's legal hand-cast options contain the acquired fish's own spell ability");
        assertTrue(recipient.getZone(ZoneType.Hand).getCardsAddedThisTurn(ZoneType.Graveyard).stream()
                .anyMatch(lki -> lki.getId() == fishId && lki.getOwner() == caster),
                "Fengraf's hand-entry observer retains the fish's pre-move owner in source LKI");

        Card firstIsland = currentCard(game, recipientIslands.get(0).getId());
        Card retainedIsland = currentCard(game, recipientIslands.get(1).getId());
        IdChoiceController recipientChoices = useIdChoices(recipient, List.of(fish.getId()), -1, -1);
        Card metamorphose = addCardToZone("Metamorphose", caster, ZoneType.Hand);
        SpellAbility morph = metamorphose.getSpells().get(0);
        morph.setActivatingPlayer(caster);
        assertTrue(morph.canTarget(firstIsland), "Metamorphose targets B's Island");
        morph.getTargets().add(firstIsland);
        resolveStacked(game, morph, caster);

        fish = currentCard(game, fishSpell.getId());
        assertTrue(recipient.getCardsIn(ZoneType.Battlefield).contains(fish),
                "B chooses the acquired fish from B's private hand for noncast battlefield entry");
        assertTrue(recipient.getCardsIn(ZoneType.Battlefield).contains(retainedIsland),
                "B retains the second Island so Dandân remains on the battlefield");
        assertSame(fish.getOwner(), recipient);
        assertSame(fish.getController(), recipient,
                "Metamorphose gives control to the chosen hand card's receiving player");

        Card unsubstantiate = addCardToZone("Unsubstantiate", caster, ZoneType.Hand);
        SpellAbility bounce = unsubstantiate.getSpells().get(0);
        bounce.setActivatingPlayer(caster);
        assertTrue(bounce.canTarget(fish), "the real Unsubstantiate spell can target B-controlled Dandân");
        bounce.getTargets().add(fish);
        resolveStacked(game, bounce, caster);

        fish = currentCard(game, fishSpell.getId());
        assertTrue(recipient.getCardsIn(ZoneType.Hand).contains(fish),
                "Unsubstantiate returns the acquired fish to its owner's hand");
        assertFalse(caster.getCardsIn(ZoneType.Hand).contains(fish));
        assertSame(fish.getOwner(), recipient);
        assertTrue(recipient.getZone(ZoneType.Hand).getCardsAddedThisTurn(ZoneType.Battlefield).stream()
                .anyMatch(lki -> lki.getId() == fishId && lki.getOwner() == recipient),
                "the bounce hand-entry observer records the acquired owner as source owner");
        assertTrue(recipientChoices.handChoiceWasSelected(metamorphose.getId(), fishId, recipient, caster),
                "B's hidden-zone callback selected the acquired fish while A remained spell activator");
    }

    @Test
    public void sanctuaryEntryUsesIslandCountAndItsControllerForTheRealOptionalTrigger() {
        SanctuaryOutcome enabled = resolveSanctuaryMetamorphose(4);
        assertFalse(enabled.sanctuary.isTapped(), "three other Islands let the actual Sanctuary replacement keep it untapped");
        assertEquals(enabled.game.getCardsIn(ZoneType.Library).get(0).getId(), enabled.spellId,
                "B's real Sanctuary trigger puts the selected common-graveyard spell on top");
        assertTrue(enabled.recorder.triggerWasConfirmed(enabled.sanctuaryId, enabled.recipient),
                "B confirms the queued optional trigger through confirmTrigger");
        assertTrue(enabled.triggerQueued && enabled.triggerRegistered,
                "the real untapped-entry trigger moved from the queued list onto and through the stack");
        assertTrue(enabled.recorder.triggerTargetWasScripted(enabled.sanctuaryId, enabled.spellId),
                "B scripts the actual legal Predict target through the trigger-order callback");
        assertSame(currentCard(enabled.game, enabled.spellId).getOwner(), enabled.originalOwner,
                "Sanctuary's graveyard-to-library move does not acquire ownership");
        assertSame(enabled.sanctuary.getOwner(), enabled.recipient);
        assertSame(enabled.sanctuary.getController(), enabled.recipient);

        SanctuaryOutcome disabled = resolveSanctuaryMetamorphose(3);
        assertTrue(disabled.sanctuary.isTapped(), "only two other Islands make Sanctuary enter tapped");
        assertFalse(disabled.recorder.triggerWasConfirmed(disabled.sanctuaryId, disabled.recipient),
                "the tapped entry creates no optional Sanctuary trigger decision");
        assertFalse(disabled.triggerQueued || disabled.triggerRegistered,
                "the tapped-entry case has no queued or registered Sanctuary trigger");
        assertFalse(disabled.recorder.triggerTargetWasScripted(disabled.sanctuaryId, disabled.spellId),
                "the tapped entry creates no targeted Sanctuary trigger on the stack");
        assertTrue(disabled.game.getCardsIn(ZoneType.Graveyard).stream().anyMatch(card -> card.getId() == disabled.spellId),
                "without the untapped-entry trigger, the graveyard card stays in the shared graveyard");
    }

    private SanctuaryOutcome resolveSanctuaryMetamorphose(int islandCount) {
        Game game = newGame(true);
        Player caster = game.getRegisteredPlayers().get(0);
        Player recipient = game.getRegisteredPlayers().get(1);
        addCard("Island", caster);
        List<Card> islands = new ArrayList<>();
        for (int i = 0; i < islandCount; i++) {
            islands.add(addCard("Island", recipient));
        }
        game.getAction().checkStateEffects(true);
        Card spell = addCardToZone("Predict", caster, ZoneType.Graveyard);
        Card sanctuaryInLibrary = addCardToZone("Mystic Sanctuary", caster, ZoneType.Library);
        game.copyLastState();
        recipient.drawCards(1);
        Card sanctuaryInHand = currentCard(game, sanctuaryInLibrary.getId());
        assertTrue(recipient.getCardsIn(ZoneType.Hand).contains(sanctuaryInHand),
                "B actually draws the A-backed Sanctuary from the shared library");
        assertSame(sanctuaryInHand.getOwner(), recipient,
                "the successful shared-library-to-hand draw acquires Sanctuary for B");
        assertTrue(recipient.getZone(ZoneType.Hand).getCardsAddedThisTurn(ZoneType.Library).stream()
                .anyMatch(lki -> lki.getId() == sanctuaryInHand.getId() && lki.getOwner() == caster),
                "the draw observer retains Sanctuary's original library owner in source LKI");
        Card metamorphose = addCardToZone("Metamorphose", caster, ZoneType.Hand);
        IdChoiceController recorder = useIdChoices(recipient, List.of(sanctuaryInHand.getId()),
                sanctuaryInHand.getId(), spell.getId());
        SpellAbility morph = metamorphose.getSpells().get(0);
        morph.setActivatingPlayer(caster);
        Card targetedIsland = currentCard(game, islands.get(0).getId());
        assertTrue(morph.canTarget(targetedIsland));
        morph.getTargets().add(targetedIsland);
        resolveStacked(game, morph, caster);

        boolean triggerQueued = game.getStack().hasSimultaneousStackEntries();
        boolean triggerRegistered = false;
        if (islandCount == 4) {
            assertTrue(triggerQueued, "the untapped Sanctuary queues its real ETB trigger");
            Card spentMetamorphose = currentCard(game, metamorphose.getId());
            game.copyLastState();
            game.getAction().moveTo(ZoneType.Exile, spentMetamorphose, null, AbilityKey.newMap());
            assertTrue(game.getStack().addAllTriggeredAbilitiesToStack(),
                    "the queued ETB trigger is ordered and moved onto the actual stack");
            SpellAbilityStackInstance trigger = findTrigger(game, sanctuaryInHand.getId());
            assertNotNull(trigger, "the Sanctuary trigger has a registered stack instance");
            SpellAbility triggerAbility = trigger.getSpellAbility();
            assertTrue(triggerAbility.isTrigger());
            assertSame(triggerAbility.getActivatingPlayer(), recipient);
            assertEquals(cardIds(triggerAbility.getTargets().getTargetCards()), List.of(spell.getId()),
                    "the real trigger has the scripted, legal Predict target before resolution");
            triggerRegistered = true;
            game.getStack().resolveStack();
        } else {
            assertFalse(triggerQueued, "the tapped Sanctuary queues no ETB trigger");
            assertFalse(game.getStack().hasSourceOnStack(currentCard(game, sanctuaryInHand.getId()),
                    SpellAbility::isTrigger), "no Sanctuary trigger appears on either trigger stack");
        }

        Card sanctuary = currentCard(game, sanctuaryInHand.getId());
        assertTrue(recipient.getCardsIn(ZoneType.Battlefield).contains(sanctuary),
                "the actual Metamorphose effect places Sanctuary under its hand recipient");
        int sanctuaryId = sanctuary.getId();
        assertTrue(recipient.getZone(ZoneType.Battlefield).getCardsAddedThisTurn(ZoneType.Hand).stream()
                .anyMatch(lki -> lki.getId() == sanctuaryId && lki.getOwner() == recipient),
                "the Sanctuary entry observer sees the named hand recipient as owner");
        assertTrue(recipient.getCardsIn(ZoneType.Battlefield).stream()
                .filter(Card::isLand)
                .filter(card -> card.getType().hasSubtype("Island"))
                .count() == islandCount,
                "Metamorphose trades one Island for Sanctuary while preserving the other Island count");
        assertTrue(recorder.handChoiceWasSelected(metamorphose.getId(), sanctuary.getId(), recipient, caster),
                "B selects Sanctuary from B's private hand through the real zone-choice callback");
        return new SanctuaryOutcome(game, caster, recipient, spell.getId(), sanctuary.getId(), sanctuary,
                recorder, triggerQueued, triggerRegistered);
    }

    private Game newGame(boolean danDan) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("official first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("official second", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (danDan) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Match match = new Match(rules, players, "DanDan official ownership lifecycle");
        Game game = match.createGame();
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getRegisteredPlayers().get(0));
        return game;
    }

    private void resolveStacked(Game game, SpellAbility ability, Player caster) {
        ability.setActivatingPlayer(caster);
        Card host = ability.getHostCard();
        game.copyLastState();
        game.getStack().addAndUnfreeze(ability);
        SpellAbility registered = game.getStack().getSpellMatchingHost(host);
        assertNotNull(registered, "the real card ability is registered on the game stack");
        assertNotNull(game.getStack().getInstanceMatchingSpellAbilityID(registered),
                "the real ability has a current stack instance before resolution");
        game.getStack().resolveStack();
    }

    private SpellAbility putAbilityOnStack(Game game, SpellAbility ability) {
        game.copyLastState();
        game.getStack().addAndUnfreeze(ability);
        SpellAbility registered = game.getStack().peekAbility();
        assertNotNull(registered, "the official activated ability is the current stack item");
        assertSame(registered.getHostCard(), ability.getHostCard());
        assertSame(registered.getOriginalAbility(), ability,
                "MagicStack's live activated-ability copy is associated with the selected card ability");
        assertEquals(registered.getApi(), ability.getApi());
        assertNotNull(game.getStack().getInstanceMatchingSpellAbilityID(registered),
                "the current activated-ability copy has a registered stack instance");
        return registered;
    }

    private SpellAbilityStackInstance findTrigger(Game game, int hostId) {
        for (SpellAbilityStackInstance instance : game.getStack()) {
            if (instance.isTrigger() && instance.getSourceCard().getId() == hostId) {
                return instance;
            }
        }
        return null;
    }

    private SpellAbility findChangeZoneAbility(Card card) {
        return card.getSpellAbilities().stream()
                .filter(SpellAbility::isActivatedAbility)
                .filter(ability -> ability.getApi() == ApiType.ChangeZone)
                .findFirst().orElseThrow();
    }

    private Card currentCard(Game game, int cardId) {
        Card current = game.findById(cardId);
        assertNotNull(current, "the expected physical card remains registered after its zone transition");
        return current;
    }

    private List<Integer> cardIds(Iterable<Card> cards) {
        List<Integer> ids = new ArrayList<>();
        for (Card card : cards) {
            ids.add(card.getId());
        }
        return ids;
    }

    private IdChoiceController useIdChoices(Player player, List<Integer> handChoiceIds,
            int triggerHostId, int triggerTargetId) {
        IdChoiceController controller = new IdChoiceController(player, handChoiceIds,
                triggerHostId, triggerTargetId);
        player.dangerouslySetController(controller);
        return controller;
    }

    private static final class IdChoiceController extends PlayerControllerAi {
        private final Player decisionPlayer;
        private final List<Integer> expectedHandChoiceIds;
        private final int expectedTriggerHostId;
        private final int expectedTriggerTargetId;
        private final List<HandChoice> handChoices = new ArrayList<>();
        private final List<Integer> scriptedTriggerHosts = new ArrayList<>();
        private final List<Integer> confirmedTriggerHosts = new ArrayList<>();

        private IdChoiceController(Player player, List<Integer> handChoiceIds,
                int triggerHostId, int triggerTargetId) {
            super(player.getGame(), player, new LobbyPlayerAi(player.getName(), null));
            this.decisionPlayer = player;
            this.expectedHandChoiceIds = handChoiceIds;
            this.expectedTriggerHostId = triggerHostId;
            this.expectedTriggerTargetId = triggerTargetId;
        }

        @Override
        public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa,
                CardCollection offered, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional,
                Player decider) {
            int hostId = sa.getHostCard().getId();
            int expectedId = expectedHandChoiceIds.get(handChoices.size());
            Card selected = offered.stream().filter(card -> card.getId() == expectedId).findFirst().orElse(null);
            handChoices.add(new HandChoice(hostId, expectedId, selected == null ? -1 : selected.getId(),
                    decider, sa.getActivatingPlayer()));
            return selected;
        }

        @Override
        public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
            for (SpellAbility sa : activePlayerSAs) {
                if (sa.getHostCard().getId() != expectedTriggerHostId) {
                    continue;
                }
                Card target = getGame().findById(expectedTriggerTargetId);
                if (target == null || !sa.canTarget(target)) {
                    throw new AssertionError("Expected legal trigger target " + expectedTriggerTargetId
                            + " for Sanctuary " + expectedTriggerHostId);
                }
                sa.getTargets().add(target);
                scriptedTriggerHosts.add(sa.getHostCard().getId());
            }
            super.orderAndPlaySimultaneousSa(activePlayerSAs);
        }

        @Override
        public boolean confirmTrigger(WrappedAbility wrapper) {
            if (wrapper.getHostCard().getId() == expectedTriggerHostId) {
                if (wrapper.getDecider() != decisionPlayer || wrapper.getActivatingPlayer() != decisionPlayer) {
                    throw new AssertionError("Sanctuary trigger must be controlled and decided by its recipient");
                }
                List<Integer> targetIds = wrapper.getWrappedAbility().getTargets().getTargetCards().stream()
                        .map(Card::getId).toList();
                if (!targetIds.equals(List.of(expectedTriggerTargetId))) {
                    throw new AssertionError("Expected Sanctuary target " + expectedTriggerTargetId
                            + " but stack trigger targets were " + targetIds);
                }
                confirmedTriggerHosts.add(wrapper.getHostCard().getId());
            }
            return true;
        }

        @Override
        public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode,
                String message, List<String> options, Card cardToShow, Map<String, Object> params) {
            return true;
        }

        private boolean handChoiceWasSelected(int hostId, int cardId, Player decider, Player activator) {
            return handChoices.stream().anyMatch(choice -> choice.hostId == hostId && choice.expectedId == cardId
                    && choice.selectedId == cardId && choice.decider == decider && choice.activator == activator);
        }

        private boolean triggerWasConfirmed(int hostId, Player decider) {
            return confirmedTriggerHosts.contains(hostId) && decisionPlayer == decider;
        }

        private boolean triggerTargetWasScripted(int hostId, int targetId) {
            return scriptedTriggerHosts.contains(hostId) && expectedTriggerTargetId == targetId;
        }
    }

    private static final class HandChoice {
        private final int hostId;
        private final int expectedId;
        private final int selectedId;
        private final Player decider;
        private final Player activator;

        private HandChoice(int hostId, int expectedId, int selectedId,
                Player decider, Player activator) {
            this.hostId = hostId;
            this.expectedId = expectedId;
            this.selectedId = selectedId;
            this.decider = decider;
            this.activator = activator;
        }
    }

    private static final class SanctuaryOutcome {
        private final Game game;
        private final Player originalOwner;
        private final Player recipient;
        private final int spellId;
        private final int sanctuaryId;
        private final Card sanctuary;
        private final IdChoiceController recorder;
        private final boolean triggerQueued;
        private final boolean triggerRegistered;

        private SanctuaryOutcome(Game game, Player originalOwner, Player recipient, int spellId, int sanctuaryId,
                Card sanctuary, IdChoiceController recorder, boolean triggerQueued, boolean triggerRegistered) {
            this.game = game;
            this.originalOwner = originalOwner;
            this.recipient = recipient;
            this.spellId = spellId;
            this.sanctuaryId = sanctuaryId;
            this.sanctuary = sanctuary;
            this.recorder = recorder;
            this.triggerQueued = triggerQueued;
            this.triggerRegistered = triggerRegistered;
        }
    }
}
