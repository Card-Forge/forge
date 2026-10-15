package forge.game;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameCopier;
import forge.deck.Deck;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardCopyService;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class DanDanSharedZonesTest extends AITest {
    @Test
    public void memoryLapseReturnsSpellToSharedLibraryAndDrawsItOnce() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);

        Card next = createCard("Island", first);
        first.getZone(ZoneType.Library).add(next);
        Card target = addCardToZone("Ponder", second, ZoneType.Hand);
        var submittedTargetSpell = target.getSpells().get(0);
        submittedTargetSpell.setActivatingPlayer(second);
        game.getStack().addAndUnfreeze(submittedTargetSpell);
        var targetSpell = game.getStack().getSpellMatchingHost(target);
        assertEquals(target.getZone().getZoneType(), ZoneType.Stack);
        assertNotNull(game.getStack().getInstanceMatchingSpellAbilityID(targetSpell));

        Card memoryLapse = addCardToZone("Memory Lapse", first, ZoneType.Hand);
        memoryLapse.getFirstSpellAbility().setActivatingPlayer(first);
        memoryLapse.getFirstSpellAbility().getTargets().add(targetSpell);
        assertTrue(memoryLapse.getFirstSpellAbility().getTargets().isTargetingAnySpell());
        AbilityUtils.resolve(memoryLapse.getFirstSpellAbility());

        Card returnedToLibrary = game.findById(target.getId());
        assertEquals(returnedToLibrary.getZone().getZoneType(), ZoneType.Library);
        assertEquals(ids(second.getCardsIn(ZoneType.Library)), List.of(target.getId(), next.getId()));
        assertEquals(viewIds(second.getView().getCards(ZoneType.Library)), List.of(target.getId(), next.getId()));
        assertEquals(ids(first.drawCards(1)), List.of(target.getId()));
        assertEquals(ids(first.getCardsIn(ZoneType.Library)), List.of(next.getId()));
        assertEquals(visitCounts(game).get(target.getId()), Integer.valueOf(1));
    }

    @Test
    public void sharedPileHasOneOrderedIdentityThroughMixedMovesAndLoss() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);

        assertSame(first.getZone(ZoneType.Library), second.getZone(ZoneType.Library));
        assertSame(first.getZone(ZoneType.Graveyard), second.getZone(ZoneType.Graveyard));

        Card top = createCard("Island", first);
        Card next = createCard("Memory Lapse", second);
        Card grave = createCard("Mental Note", first);
        first.getZone(ZoneType.Library).add(top);
        first.getZone(ZoneType.Library).add(next);
        first.getZone(ZoneType.Graveyard).add(grave);

        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of(top.getId(), next.getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Graveyard)), List.of(grave.getId()));
        assertEquals(visitCounts(game), Map.of(top.getId(), 1, next.getId(), 1, grave.getId(), 1));
        Random originalRandom = MyRandom.getRandom();
        try {
            MyRandom.setRandom(new Random(20261008));
            second.shuffle(null);
        } finally {
            MyRandom.setRandom(originalRandom);
        }
        List<Integer> shuffledLibrary = ids(first.getCardsIn(ZoneType.Library));
        List<Integer> expectedLibrary = List.of(top.getId(), next.getId()).stream().sorted().toList();
        assertEquals(shuffledLibrary.stream().sorted().toList(), expectedLibrary);
        assertEquals(viewIds(first.getView().getCards(ZoneType.Library)), shuffledLibrary);
        assertEquals(viewIds(second.getView().getCards(ZoneType.Library)), shuffledLibrary);
        assertEquals(second.getView().getCards(ZoneType.Library).size(), 2,
                "both authorized views must show the same public shared-library state");

        game.getAction().moveTo(second.getZone(ZoneType.Graveyard), next, null);
        assertEquals(ids(first.getCardsIn(ZoneType.Graveyard)), List.of(grave.getId(), next.getId()));
        assertEquals(viewIds(second.getView().getCards(ZoneType.Graveyard)), List.of(grave.getId(), next.getId()));
        assertEquals(visitCounts(game), Map.of(top.getId(), 1, next.getId(), 1, grave.getId(), 1));

        game.onPlayerLost(first);
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of(top.getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Graveyard)), List.of(grave.getId(), next.getId()));
        assertEquals(visitCounts(game), Map.of(top.getId(), 1, next.getId(), 1, grave.getId(), 1));
    }

    @Test
    public void mentalNoteKeepsDeclaredTopOrderWithoutStaleOwnerPrompt() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);

        Card mixedTop = createCard("Island", second);
        Card mixedNext = createCard("Memory Lapse", first);
        Card draw = createCard("Island", first);
        List<Card> library = List.of(mixedTop, mixedNext, draw);
        library.forEach(first.getZone(ZoneType.Library)::add);

        Card note = addCardToZone("Mental Note", second, ZoneType.Hand);
        note.getFirstSpellAbility().setActivatingPlayer(second);
        note.getFirstSpellAbility().putParam("RememberMilled", "True");
        assertEquals(ids(first.getCardsIn(ZoneType.Library)),
                List.of(mixedTop.getId(), mixedNext.getId(), draw.getId()));
        AbilityUtils.resolve(note.getFirstSpellAbility());
        assertEquals(cardIds(note.getRemembered()), List.of(mixedTop.getId(), mixedNext.getId()));

        assertEquals(ids(first.getCardsIn(ZoneType.Graveyard)),
                List.of(mixedTop.getId(), mixedNext.getId()));
        assertTrue(second.getCardsIn(ZoneType.Hand).contains(draw),
                "Mental Note draws the card after the original top two cards are milled");
    }

    @Test
    public void snapshotCopyRetainsSharedPilesAndAcquiredHandState() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);

        Card libraryTop = createCard("Island", first);
        Card libraryNext = createCard("Memory Lapse", second);
        Card grave = createCard("Accumulated Knowledge", first);
        Card acquired = createCard("Dandân", first);
        acquired.setOwner(second);
        Card controlled = createCard("Dandân", second);
        controlled.setController(first, game.getNextTimestamp());
        assertEquals(acquired.getOwner(), second);
        assertEquals(acquired.getController(), second);
        first.getZone(ZoneType.Library).add(libraryTop);
        first.getZone(ZoneType.Library).add(libraryNext);
        first.getZone(ZoneType.Graveyard).add(grave);
        second.getZone(ZoneType.Hand).add(acquired);
        first.getZone(ZoneType.Battlefield).add(controlled);
        assertEquals(controlled.getOwner(), second);
        assertEquals(controlled.getController(), first);
        assertSame(controlled.getZone(), first.getZone(ZoneType.Battlefield));

        Card exiled = createCard("Think Twice", first);
        exiled.setOwners(List.of(first, second));
        second.getZone(ZoneType.Exile).add(exiled);
        Card detached = new CardCopyService(exiled).copyCard(false);
        Card lki = new CardCopyService(exiled).getLKICopy();
        assertEquals(detached.getOwners(), Set.of(first, second));
        assertNull(detached.getExplicitController());
        assertEquals(lki.getOwners(), Set.of(first, second));
        assertNull(lki.getExplicitController());

        Game copied = new GameSnapshot(game).makeCopy();
        Player copiedFirst = copied.getRegisteredPlayers().get(0);
        Player copiedSecond = copied.getRegisteredPlayers().get(1);
        Card copiedAcquired = copied.findById(acquired.getId());
        assertEquals(copied.findById(libraryTop.getId()).getOwners(), Set.of(copiedFirst, copiedSecond));
        assertEquals(copied.findById(grave.getId()).getOwners(), Set.of(copiedFirst, copiedSecond));
        Card copiedExiled = copied.findById(exiled.getId());
        assertEquals(copiedExiled.getOwners(), Set.of(copiedFirst, copiedSecond));
        assertNull(copiedExiled.getExplicitController());
        assertSame(copiedExiled.getZone(), copiedSecond.getZone(ZoneType.Exile));

        assertSame(copiedFirst.getZone(ZoneType.Library), copiedSecond.getZone(ZoneType.Library));
        assertSame(copiedFirst.getZone(ZoneType.Graveyard), copiedSecond.getZone(ZoneType.Graveyard));
        assertNotSame(copiedFirst.getZone(ZoneType.Library), first.getZone(ZoneType.Library));
        assertNotSame(copiedFirst.getZone(ZoneType.Graveyard), first.getZone(ZoneType.Graveyard));
        assertEquals(ids(copied.getCardsIn(ZoneType.Library)), List.of(libraryTop.getId(), libraryNext.getId()));
        assertEquals(ids(copied.getCardsIn(ZoneType.Graveyard)), List.of(grave.getId()));
        assertEquals(viewIds(copiedFirst.getView().getCards(ZoneType.Library)),
                viewIds(copiedSecond.getView().getCards(ZoneType.Library)));
        assertEquals(viewIds(copiedFirst.getView().getCards(ZoneType.Graveyard)),
                viewIds(copiedSecond.getView().getCards(ZoneType.Graveyard)));
        assertEquals(viewIds(copiedFirst.getView().getCards(ZoneType.Library)),
                List.of(libraryTop.getId(), libraryNext.getId()));
        assertEquals(viewIds(copiedFirst.getView().getCards(ZoneType.Graveyard)), List.of(grave.getId()));
        assertEquals(copiedAcquired.getOwner(), copiedSecond,
                "the copy keeps the recipient ownership already present in the hand");
        assertEquals(copiedAcquired.getController(), copiedSecond,
                "a hand card remains associated with its recipient");
        assertEquals(copiedAcquired.getView().getOwner(), copiedSecond.getView());
        assertEquals(copiedAcquired.getView().getController(), copiedSecond.getView());
        Card copiedControlled = copied.findById(controlled.getId());
        assertEquals(copiedControlled.getOwner(), copiedSecond,
                "copying keeps permanent owner separate from its current controller");
        assertEquals(copiedControlled.getController(), copiedFirst);
        assertEquals(copiedControlled.getView().getOwner(), copiedSecond.getView());
        assertEquals(copiedControlled.getView().getController(), copiedFirst.getView());
        assertEquals(copiedSecond.getCardsIn(ZoneType.Hand).stream().map(Card::getId).toList(),
                List.of(acquired.getId()));

        Card copiedTop = copied.findById(libraryTop.getId());
        copied.getAction().moveTo(copiedFirst.getZone(ZoneType.Hand), copiedTop, null);
        assertEquals(ids(copied.getCardsIn(ZoneType.Library)), List.of(libraryNext.getId()));
        assertEquals(ids(first.getCardsIn(ZoneType.Library)), List.of(libraryTop.getId(), libraryNext.getId()));
        assertEquals(viewIds(first.getView().getCards(ZoneType.Library)),
                List.of(libraryTop.getId(), libraryNext.getId()));
    }

    @Test
    public void gameCopierRetainsSharedZonesAndAcquiredOwner() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);
        assertEquals(game.getPhaseHandler().getPhase(), PhaseType.MAIN1);
        assertSame(game.getPhaseHandler().getPlayerTurn(), first);

        Card firstCard = createCard("Island", first);
        Card secondCard = createCard("Memory Lapse", second);
        first.getZone(ZoneType.Library).add(firstCard);
        first.getZone(ZoneType.Library).add(secondCard);
        Card acquired = createCard("Dandân", first);
        acquired.setOwner(second);
        assertSame(acquired.getOwner(), second);
        assertSame(acquired.getController(), second);
        second.getZone(ZoneType.Hand).add(acquired);
        assertTrue(second.getZone(ZoneType.Hand).getCards().contains(acquired));

        Card exiled = createCard("Think Twice", first);
        exiled.setOwners(List.of(first, second));
        second.getZone(ZoneType.Exile).add(exiled);

        Game copied = new GameCopier(game).makeCopy();
        Player copiedFirst = copied.getRegisteredPlayers().get(0);
        Player copiedSecond = copied.getRegisteredPlayers().get(1);
        assertEquals(copied.getPhaseHandler().getPhase(), PhaseType.MAIN1);
        assertSame(copied.getPhaseHandler().getPlayerTurn(), copiedFirst);
        assertSame(copiedFirst.getZone(ZoneType.Library), copiedSecond.getZone(ZoneType.Library));
        assertEquals(ids(copied.getCardsIn(ZoneType.Library)), List.of(firstCard.getId(), secondCard.getId()));
        assertEquals(copied.findById(acquired.getId()).getOwner(), copiedSecond);
        assertEquals(copied.findById(firstCard.getId()).getOwners(), Set.of(copiedFirst, copiedSecond));
        Card copiedExiled = copied.findById(exiled.getId());
        assertEquals(copiedExiled.getOwners(), Set.of(copiedFirst, copiedSecond));
        assertNull(copiedExiled.getExplicitController());
        assertSame(copiedExiled.getZone(), copiedSecond.getZone(ZoneType.Exile));
        assertEquals(visitCounts(copied), Map.of(firstCard.getId(), 1, secondCard.getId(), 1,
                acquired.getId(), 1, exiled.getId(), 1));
    }

    @Test
    public void snapshotRestoreRetainsSharedPilesAndAcquiredCardState() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);
        Card top = createCard("Island", first);
        Card next = createCard("Memory Lapse", second);
        Card grave = createCard("Mental Note", first);
        Card acquired = createCard("Dandân", first);
        acquired.setOwner(second);
        Card controlled = createCard("Dandân", second);
        controlled.setController(first, game.getNextTimestamp());
        first.getZone(ZoneType.Library).add(top);
        first.getZone(ZoneType.Library).add(next);
        first.getZone(ZoneType.Graveyard).add(grave);
        second.getZone(ZoneType.Hand).add(acquired);
        first.getZone(ZoneType.Battlefield).add(controlled);

        Card exiled = createCard("Think Twice", first);
        exiled.setOwners(List.of(first, second));
        second.getZone(ZoneType.Exile).add(exiled);

        GameSnapshot snapshot = new GameSnapshot(game);
        snapshot.makeCopy();
        game.getAction().moveTo(second.getZone(ZoneType.Hand), next, null);
        assertEquals(acquired.getOwner(), second);
        assertEquals(acquired.getController(), second);
        controlled.setOwner(first);
        controlled.setController(second, game.getNextTimestamp());

        exiled.setOwner(second);
        snapshot.restoreGameState(game);

        assertSame(first.getZone(ZoneType.Library), second.getZone(ZoneType.Library));
        assertSame(first.getZone(ZoneType.Graveyard), second.getZone(ZoneType.Graveyard));
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of(top.getId(), next.getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Graveyard)), List.of(grave.getId()));
        assertEquals(game.findById(acquired.getId()).getOwner(), second);
        assertEquals(game.findById(acquired.getId()).getController(), second);
        assertEquals(game.findById(controlled.getId()).getOwner(), second);
        assertEquals(game.findById(controlled.getId()).getController(), first);
        assertEquals(visitCounts(game).get(next.getId()), Integer.valueOf(1));
        assertEquals(game.findById(exiled.getId()).getOwners(), Set.of(first, second));
        assertSame(game.findById(exiled.getId()).getOwner(), first);
        assertNull(game.findById(exiled.getId()).getExplicitController());
        assertSame(game.findById(exiled.getId()).getZone(), second.getZone(ZoneType.Exile));
    }

    @Test
    public void snapshotRestoreRemovesCardsFromTheirActualCurrentZone() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);
        Card controlled = createCard("Dandân", second);
        controlled.setController(first, game.getNextTimestamp());
        first.getZone(ZoneType.Battlefield).add(controlled);

        GameSnapshot movedToHand = new GameSnapshot(game);
        movedToHand.makeCopy();
        game.getAction().moveTo(second.getZone(ZoneType.Hand), controlled, null);
        Card inHand = game.findById(controlled.getId());
        assertSame(inHand.getZone(), second.getZone(ZoneType.Hand));
        assertTrue(second.getZone(ZoneType.Hand).getCards().contains(inHand));

        movedToHand.restoreGameState(game);
        Card restored = game.findById(controlled.getId());
        assertSame(restored.getZone(), first.getZone(ZoneType.Battlefield));
        assertTrue(first.getZone(ZoneType.Battlefield).getCards().contains(restored));
        assertTrue(!second.getZone(ZoneType.Hand).getCards().contains(restored));
        assertEquals(visitCounts(game).get(controlled.getId()), Integer.valueOf(1));
        assertEquals(restored.getOwner(), second);
        assertEquals(restored.getController(), first);
        assertEquals(restored.getView().getOwner(), second.getView());
        assertEquals(restored.getView().getController(), first.getView());
    }

    @Test
    public void snapshotRestoreRemovesCardFromOtherBattlefieldWithSameZoneType() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);
        Card controlled = createCard("Dandân", second);
        controlled.setController(first, game.getNextTimestamp());
        first.getZone(ZoneType.Battlefield).add(controlled);

        GameSnapshot movedToOtherBattlefield = new GameSnapshot(game);
        movedToOtherBattlefield.makeCopy();
        controlled.setController(second, game.getNextTimestamp());
        game.getAction().controllerChangeZoneCorrection(controlled);
        Card onSecondBattlefield = game.findById(controlled.getId());
        assertSame(onSecondBattlefield.getZone(), second.getZone(ZoneType.Battlefield));

        movedToOtherBattlefield.restoreGameState(game);
        Card restoredAfterControlTransfer = game.findById(controlled.getId());
        assertSame(restoredAfterControlTransfer.getZone(), first.getZone(ZoneType.Battlefield));
        assertTrue(first.getZone(ZoneType.Battlefield).getCards().contains(restoredAfterControlTransfer));
        assertTrue(!second.getZone(ZoneType.Battlefield).getCards().contains(restoredAfterControlTransfer));
        assertEquals(visitCounts(game).get(controlled.getId()), Integer.valueOf(1));
        assertEquals(restoredAfterControlTransfer.getOwner(), second);
        assertEquals(restoredAfterControlTransfer.getController(), first);
    }

    @Test
    public void ordinaryZonesStaySeparateAndSnapshotOwnershipRemainsCorrect() {
        Game game = newGame(false);
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        assertNotSame(first.getZone(ZoneType.Library), second.getZone(ZoneType.Library));
        assertNotSame(first.getZone(ZoneType.Graveyard), second.getZone(ZoneType.Graveyard));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);
        Card controlled = createCard("Dandân", second);
        controlled.setController(first, game.getNextTimestamp());
        assertEquals(controlled.getOwner(), second);
        assertEquals(controlled.getController(), first);
        first.getZone(ZoneType.Battlefield).add(controlled);
        Game copied = new GameSnapshot(game).makeCopy();
        Card copy = copied.findById(controlled.getId());
        assertEquals(copy.getOwner(), copied.getRegisteredPlayers().get(1));
        assertEquals(copy.getController(), copied.getRegisteredPlayers().get(0));

        GameSnapshot snapshot = new GameSnapshot(game);
        snapshot.makeCopy();
        game.getAction().moveTo(second.getZone(ZoneType.Hand), controlled, null);
        snapshot.restoreGameState(game);
        Card restored = game.findById(controlled.getId());
        assertSame(restored.getZone(), first.getZone(ZoneType.Battlefield));
        assertTrue(first.getZone(ZoneType.Battlefield).getCards().contains(restored));
        assertTrue(!second.getZone(ZoneType.Hand).getCards().contains(restored));
        assertEquals(restored.getOwner(), second);
        assertEquals(restored.getController(), first);
        assertEquals(visitCounts(game).get(controlled.getId()), Integer.valueOf(1));
    }

    @Test
    public void mentalNoteSkipsOwnerOrderingForNeverAcquiredSharedCards() {
        Game danDan = newDanDanGame();
        Player first = danDan.getRegisteredPlayers().get(0);
        Player second = danDan.getRegisteredPlayers().get(1);
        danDan.getPhaseHandler().devModeSet(PhaseType.MAIN1, first);
        RecordingOrderController firstController = useRecordingOrderController(danDan, first);
        RecordingOrderController secondController = useRecordingOrderController(danDan, second);
        Card firstBackedTop = createCard("Island", first);
        Card firstBackedNext = createCard("Memory Lapse", first);
        Card draw = createCard("Island", first);
        first.getZone(ZoneType.Library).add(firstBackedTop);
        first.getZone(ZoneType.Library).add(firstBackedNext);
        first.getZone(ZoneType.Library).add(draw);

        Card note = addCardToZone("Mental Note", second, ZoneType.Hand);
        note.getFirstSpellAbility().setActivatingPlayer(second);
        note.getFirstSpellAbility().putParam("RememberMilled", "True");
        AbilityUtils.resolve(note.getFirstSpellAbility());

        assertEquals(firstController.orderRequests, 0);
        assertEquals(secondController.orderRequests, 0);
        assertEquals(ids(first.getCardsIn(ZoneType.Graveyard)),
                List.of(firstBackedTop.getId(), firstBackedNext.getId()));
        assertEquals(cardIds(note.getRemembered()), List.of(firstBackedTop.getId(), firstBackedNext.getId()));
        assertTrue(second.getCardsIn(ZoneType.Hand).contains(draw));

        Game ordinary = newGame(false);
        Player ordinaryFirst = ordinary.getRegisteredPlayers().get(0);
        Player ordinarySecond = ordinary.getRegisteredPlayers().get(1);
        ordinary.getPhaseHandler().devModeSet(PhaseType.MAIN1, ordinaryFirst);
        RecordingOrderController ordinarySecondController = useRecordingOrderController(ordinary, ordinarySecond);
        Card ordinaryTop = createCard("Island", ordinarySecond);
        Card ordinaryNext = createCard("Memory Lapse", ordinarySecond);
        Card ordinaryDraw = createCard("Island", ordinarySecond);
        ordinarySecond.getZone(ZoneType.Library).add(ordinaryTop);
        ordinarySecond.getZone(ZoneType.Library).add(ordinaryNext);
        ordinarySecond.getZone(ZoneType.Library).add(ordinaryDraw);
        Card ordinaryNote = addCardToZone("Mental Note", ordinarySecond, ZoneType.Hand);
        ordinaryNote.getFirstSpellAbility().setActivatingPlayer(ordinarySecond);
        AbilityUtils.resolve(ordinaryNote.getFirstSpellAbility());

        assertTrue(ordinarySecondController.orderRequests > 0,
                "ordinary two-card milling still reaches the existing ordering decision");
        assertEquals(ids(ordinarySecond.getCardsIn(ZoneType.Graveyard)),
                List.of(ordinaryTop.getId(), ordinaryNext.getId()));
        assertTrue(ordinarySecond.getCardsIn(ZoneType.Hand).contains(ordinaryDraw));
    }

    @Test
    public void sharedGraveyardCountsAccumulatedKnowledgeOnce() {
        Game game = newDanDanGame();
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);

        Card oldFirst = createCard("Accumulated Knowledge", first);
        Card oldSecond = createCard("Accumulated Knowledge", second);
        first.getZone(ZoneType.Graveyard).add(oldFirst);
        first.getZone(ZoneType.Graveyard).add(oldSecond);
        List<Card> draws = List.of(createCard("Island", first), createCard("Memory Lapse", second),
                createCard("Predict", first), createCard("Island", second), createCard("Unsubstantiate", first));
        draws.forEach(first.getZone(ZoneType.Library)::add);
        Card spell = addCardToZone("Accumulated Knowledge", second, ZoneType.Hand);
        spell.getFirstSpellAbility().setActivatingPlayer(second);

        AbilityUtils.resolve(spell.getFirstSpellAbility());

        assertEquals(ids(second.getCardsIn(ZoneType.Hand)),
                List.of(spell.getId(), draws.get(0).getId(), draws.get(1).getId(), draws.get(2).getId()));
    }

    private Game newDanDanGame() {
        return newGame(true);
    }

    private RecordingOrderController useRecordingOrderController(Game game, Player player) {
        RecordingOrderController controller = new RecordingOrderController(game, player,
                new LobbyPlayerAi(player.getName(), null));
        player.dangerouslySetController(controller);
        return controller;
    }

    private Game newGame(boolean danDan) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("second", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (danDan) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Match match = new Match(rules, players, "DanDan shared-zone test");
        return match.createGame();
    }

    private static List<Integer> ids(Iterable<Card> cards) {
        List<Integer> result = new ArrayList<>();
        for (Card card : cards) result.add(card.getId());
        return result;
    }

    private static List<Integer> viewIds(Iterable<CardView> cards) {
        List<Integer> result = new ArrayList<>();
        for (CardView card : cards) result.add(card.getId());
        return result;
    }

    private static Map<Integer, Integer> visitCounts(Game game) {
        Map<Integer, Integer> ids = new HashMap<>();
        game.forEachCardInGame(card -> {
            ids.merge(card.getId(), 1, Integer::sum);
            return true;
        });
        return ids;
    }

    private static List<Integer> cardIds(Iterable<Object> cards) {
        List<Integer> result = new ArrayList<>();
        for (Object card : cards) result.add(((Card) card).getId());
        return result;
    }

    private static final class RecordingOrderController extends PlayerControllerAi {
        private int orderRequests;

        private RecordingOrderController(Game game, Player player, LobbyPlayerAi lobbyPlayer) {
            super(game, player, lobbyPlayer);
        }

        @Override
        public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone,
                forge.game.spellability.SpellAbility source) {
            orderRequests++;
            return cards;
        }
    }
}
