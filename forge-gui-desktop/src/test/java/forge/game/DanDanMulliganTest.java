package forge.game;

import forge.StaticData;
import forge.MulliganDefs;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.mulligan.MulliganService;
import forge.game.player.Player;
import forge.game.player.PlayerController.DanDanMulliganDecision;
import forge.game.spellability.SpellAbility;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.PlayerControllerForTests;
import forge.util.MyRandom;
import org.testng.annotations.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/** Behavioral witnesses for the existing public opening and mulligan entry points. */
public class DanDanMulliganTest extends AITest {
    @Test
    public void dandanOpeningDealAlternatesFromChosenFirstDespiteFilteredPreference() {
        boolean previousFiltered = StaticData.instance().getFilteredHandsEnabled();
        MulliganDefs.MulliganRule previousRule = StaticData.instance().getMulliganRule();
        Random previousRandom = MyRandom.getRandom();
        try {
            StaticData.instance().setFilteredHandsEnabled(true);
            StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.London);
            for (int starterIndex = 0; starterIndex < 2; starterIndex++) {
                Game game = newGame(true);
                Player seatOne = game.getPlayers().get(0);
                Player seatTwo = game.getPlayers().get(1);
                Player chosenFirst = game.getPlayers().get(starterIndex);
                List<MulliganController> controllers = installControllers(game, chosenFirst,
                        List.of(true), List.of(true), null);
                List<Card> ordered = addOpeningLibrary(seatOne);
                boolean[] hookReached = {false};
                MyRandom.setRandom(new Random(61));
                game.getAction().startGame(null, () -> {
                    assertSame(game.getPhaseHandler().getPlayerTurn(), chosenFirst);
                    assertEquals(ids(chosenFirst.getCardsIn(ZoneType.Hand)), alternatingIds(ordered, 0));
                    assertEquals(ids((chosenFirst == seatOne ? seatTwo : seatOne).getCardsIn(ZoneType.Hand)),
                            alternatingIds(ordered, 1));
                    game.setGameOver(GameEndReason.Draw);
                    hookReached[0] = true;
                });
                assertTrue(hookReached[0], "the actual startGame opening path must reach its existing hook");
                assertEquals(controllers.get(0).startingPlayerChoices + controllers.get(1).startingPlayerChoices, 1);
                assertEquals(controllers.get(0).keepDecisions, 1);
                assertEquals(controllers.get(1).keepDecisions, 1);
                controllers.forEach(MulliganController::assertAnswersConsumed);
            }
        } finally {
            StaticData.instance().setFilteredHandsEnabled(previousFiltered);
            StaticData.instance().setMulliganRule(previousRule);
            MyRandom.setRandom(previousRandom);
        }
    }

    @Test
    public void dandanMulliganServiceCollectsDecisionsBeforeChangingTheSharedPile() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player second = game.getPlayers().get(1);
        List<DecisionSnapshot> snapshots = new ArrayList<>();
        List<Integer> decisionPlayerIds = new ArrayList<>();
        List<MulliganController> controllers = installControllers(game, second,
                List.of(), List.of(), snapshots, decisionPlayerIds);
        controllers.get(0).danDanAnswers = new ArrayDeque<>(List.of(DanDanMulliganDecision.REGULAR,
                DanDanMulliganDecision.REGULAR, DanDanMulliganDecision.KEEP));
        controllers.get(1).danDanAnswers = new ArrayDeque<>(List.of(DanDanMulliganDecision.REGULAR,
                DanDanMulliganDecision.REGULAR, DanDanMulliganDecision.KEEP));
        List<Integer> shuffleOrder = new ArrayList<>();
        controllers.forEach(controller -> controller.shuffleOrder = shuffleOrder);
        addStableOpeningHand(first);
        addStableOpeningHand(second);
        List<Integer> originalHands = new ArrayList<>();
        originalHands.addAll(ids(first.getCardsIn(ZoneType.Hand)));
        originalHands.addAll(ids(second.getCardsIn(ZoneType.Hand)));
        addCards(first, "Memory Lapse", 40, ZoneType.Library);
        game.setAge(GameStage.Mulligan);

        MulliganDefs.MulliganRule previousRule = StaticData.instance().getMulliganRule();
        Random previousRandom = MyRandom.getRandom();
        try {
            StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.London);
            MyRandom.setRandom(new Random(61));
            new MulliganService(second).perform();

            assertEquals(snapshots.size(), 6, "both players make three rounds of decisions");
            assertEquals(decisionPlayerIds, List.of(second.getId(), first.getId(), second.getId(), first.getId(),
                    second.getId(), first.getId()), "players make decisions in chosen-first order each round");
            assertEquals(snapshots.get(0).firstHand(), snapshots.get(1).firstHand());
            assertEquals(snapshots.get(0).secondHand(), snapshots.get(1).secondHand());
            assertEquals(snapshots.get(0).library(), snapshots.get(1).library(),
                    "the second player's first-round decision must happen before either selected hand returns or shuffles");
            assertEquals(snapshots.get(2).firstHand(), snapshots.get(3).firstHand());
            assertEquals(snapshots.get(2).secondHand(), snapshots.get(3).secondHand());
            assertEquals(snapshots.get(2).library(), snapshots.get(3).library(),
                    "the second-round declaration must precede either player's second return or shuffle");
            List<Integer> firstRedealOrder = controllers.get(0).shuffleInputs.get(0);
            assertTrue(firstRedealOrder.size() >= 14);
            assertEquals(controllers.get(1).bottomedIds.get(0), firstRedealOrder.get(0));
            assertEquals(controllers.get(0).bottomedIds.get(0), firstRedealOrder.get(1));
            assertEquals(snapshots.get(2).secondHand(), alternatingIdsAfterFirst(firstRedealOrder.subList(0, 14), 0),
                    "the chosen first player receives alternating cards from the final shuffle");
            assertEquals(snapshots.get(2).firstHand(), alternatingIdsAfterFirst(firstRedealOrder.subList(0, 14), 1),
                    "the other player receives the interleaved cards from the same shuffle");
            assertEquals(snapshots.get(2).firstHand().size(), 6,
                    "the first regular London mulligan bottoms one before the next declaration");
            assertEquals(snapshots.get(2).secondHand().size(), 6);
            assertEquals(snapshots.get(2).library().subList(snapshots.get(2).library().size() - 2,
                    snapshots.get(2).library().size()), List.of(controllers.get(1).bottomedIds.get(0),
                    controllers.get(0).bottomedIds.get(0)),
                    "the chosen-first player's first private bottom batch sits above the other batch");
            assertEquals(snapshots.get(4).firstHand(), snapshots.get(5).firstHand());
            assertEquals(snapshots.get(4).secondHand(), snapshots.get(5).secondHand());
            assertEquals(snapshots.get(4).library(), snapshots.get(5).library(),
                    "the third-round declaration must precede either player's third return or shuffle");
            assertEquals(snapshots.get(4).library().subList(snapshots.get(4).library().size() - 4,
                    snapshots.get(4).library().size()), List.of(controllers.get(1).bottomedIds.get(1),
                    controllers.get(1).bottomedIds.get(2), controllers.get(0).bottomedIds.get(1),
                    controllers.get(0).bottomedIds.get(2)),
                    "the chosen-first player's two-card batch sits above the other player's batch");
            assertEquals(snapshots.get(4).firstHand().size(), 5,
                    "the second regular London mulligan bottoms two before the next declaration");
            assertEquals(snapshots.get(4).secondHand().size(), 5);
            assertEquals(controllers.get(0).keepDecisions, 3);
            assertEquals(controllers.get(1).keepDecisions, 3);
            assertEquals(shuffleOrder, List.of(second.getId(), first.getId(), second.getId(), first.getId()),
                    "each selected player keeps a separately ordered shuffle instruction");
            assertTrue(controllers.get(1).shuffleInputs.get(0).containsAll(originalHands),
                    "the first shuffle starts only after both players' hands have returned");
            assertTrue(controllers.get(0).shuffleInputs.get(0).containsAll(originalHands));
            assertTrue(controllers.get(1).shuffleInputs.get(1).contains(controllers.get(1).bottomedIds.get(0)));
            assertTrue(controllers.get(1).shuffleInputs.get(1).contains(controllers.get(0).bottomedIds.get(0)),
                    "the later shared shuffle includes both previously bottomed cards");
            controllers.forEach(MulliganController::assertAnswersConsumed);
        } finally {
            StaticData.instance().setMulliganRule(previousRule);
            MyRandom.setRandom(previousRandom);
        }
    }

    @Test
    public void dandanFreeMulligansRepeatUntilThatPlayerChoosesRegular() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player second = game.getPlayers().get(1);
        List<MulliganController> controllers = installControllers(game, first,
                List.of(), List.of(true), null);
        controllers.get(0).danDanAnswers = new ArrayDeque<>(List.of(
                DanDanMulliganDecision.FREE, DanDanMulliganDecision.FREE,
                DanDanMulliganDecision.REGULAR, DanDanMulliganDecision.KEEP));
        addHand(first, "Memory Lapse", 7);
        addHand(second, "Island", 7);
        List<Integer> keptSecondHand = ids(second.getCardsIn(ZoneType.Hand));
        addCards(first, "Memory Lapse", 50, ZoneType.Library);
        game.setAge(GameStage.Mulligan);

        MulliganDefs.MulliganRule previousRule = StaticData.instance().getMulliganRule();
        Random previousRandom = MyRandom.getRandom();
        try {
            StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.Paris);
            MyRandom.setRandom(new Random(61));
            new MulliganService(first).perform();

            assertEquals(StaticData.instance().getMulliganRule(), MulliganDefs.MulliganRule.Paris,
                    "DanDan uses London locally without changing the selected global rule");
            assertEquals(controllers.get(0).danDanPenalties, List.of(0, 0, 0, 1));
            assertEquals(controllers.get(0).danDanFreeChoices, List.of(true, true, true, false));
            assertEquals(controllers.get(0).danDanAnswers.size(), 0);
            assertEquals(controllers.get(0).revealedCards, 0,
                    "the mulliganing player already knows their own hand");
            assertEquals(controllers.get(1).revealedCards, 14,
                    "each accepted free mulligan reveals that hand to the opponent");
            assertEquals(first.getCardsIn(ZoneType.Hand).size(), 6,
                    "the regular London mulligan immediately bottoms its one-card penalty");
            assertEquals(second.getCardsIn(ZoneType.Hand).size(), 7,
                    "the player who kept cannot be pulled into later rounds");
            assertEquals(ids(second.getCardsIn(ZoneType.Hand)), keptSecondHand);
            assertEquals(controllers.get(1).keepDecisions, 1,
                    "the kept player's independent decision is requested only once");
            controllers.forEach(MulliganController::assertAnswersConsumed);
        } finally {
            StaticData.instance().setMulliganRule(previousRule);
            MyRandom.setRandom(previousRandom);
        }
    }

    @Test
    public void regularChoiceEndsOnlyThatPlayersFreeEligibility() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player second = game.getPlayers().get(1);
        List<DecisionSnapshot> snapshots = new ArrayList<>();
        List<MulliganController> controllers = installControllers(game, first,
                List.of(), List.of(), snapshots);
        controllers.get(0).danDanAnswers = new ArrayDeque<>(List.of(
                DanDanMulliganDecision.REGULAR, DanDanMulliganDecision.KEEP));
        controllers.get(1).danDanAnswers = new ArrayDeque<>(List.of(
                DanDanMulliganDecision.FREE, DanDanMulliganDecision.FREE, DanDanMulliganDecision.KEEP));
        addHand(first, "Memory Lapse", 7);
        addHand(second, "Memory Lapse", 7);
        addCards(first, "Memory Lapse", 50, ZoneType.Library);
        game.setAge(GameStage.Mulligan);

        MulliganDefs.MulliganRule previousRule = StaticData.instance().getMulliganRule();
        Random previousRandom = MyRandom.getRandom();
        try {
            StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.Paris);
            MyRandom.setRandom(new Random(61));
            new MulliganService(first).perform();

            assertEquals(controllers.get(0).danDanPenalties, List.of(0, 1));
            assertEquals(controllers.get(0).danDanFreeChoices, List.of(true, false));
            assertEquals(controllers.get(1).danDanPenalties, List.of(0, 0, 0));
            assertEquals(controllers.get(1).danDanFreeChoices, List.of(true, true, true));
            assertEquals(controllers.get(0).revealedHands,
                    List.of(snapshots.get(1).secondHand(), snapshots.get(3).secondHand()),
                    "only the player's two accepted free choices reveal their current hand");
            assertEquals(ids(first.getCardsIn(ZoneType.Hand)), snapshots.get(2).firstHand(),
                    "the player who chose regular keeps the hand from their later Keep");
            assertEquals(ids(second.getCardsIn(ZoneType.Hand)), snapshots.get(4).secondHand());
            assertEquals(StaticData.instance().getMulliganRule(), MulliganDefs.MulliganRule.Paris);
            controllers.forEach(MulliganController::assertAnswersConsumed);
        } finally {
            StaticData.instance().setMulliganRule(previousRule);
            MyRandom.setRandom(previousRandom);
        }
    }

    @Test
    public void dandanFreeMulliganLandBoundariesAreConditionalPerPlayer() {
        MulliganDefs.MulliganRule previousRule = StaticData.instance().getMulliganRule();
        try {
            StaticData.instance().setMulliganRule(MulliganDefs.MulliganRule.Houston);
            for (int lands : List.of(0, 1, 2, 5, 6, 7)) {
                Game game = newGame(true);
                Player first = game.getPlayers().get(0);
                Player second = game.getPlayers().get(1);
                List<MulliganController> controllers = installControllers(game, first,
                        List.of(true), List.of(true), null);
                addHand(first, "Island", lands);
                addHand(first, "Memory Lapse", 7 - lands);
                addHand(second, "Memory Lapse", 7);
                game.setAge(GameStage.Mulligan);
                new MulliganService(first).perform();

                assertEquals(controllers.get(0).danDanFreeChoices, List.of(lands <= 1 || lands >= 6),
                        "free eligibility for a " + lands + "-land hand");
                assertEquals(controllers.get(0).keepDecisions, 1);
                assertEquals(controllers.get(1).keepDecisions, 1);
                controllers.forEach(MulliganController::assertAnswersConsumed);
            }
        } finally {
            StaticData.instance().setMulliganRule(previousRule);
        }
    }

    @Test
    public void concessionDuringDanDanMulliganDecisionStopsBeforeAnyHandMoves() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player second = game.getPlayers().get(1);
        List<MulliganController> controllers = installControllers(game, first,
                List.of(true), List.of(true), null);
        addStableOpeningHand(first);
        addStableOpeningHand(second);
        List<Card> library = addCards(first, "Memory Lapse", 20, ZoneType.Library);
        List<Integer> firstHand = ids(first.getCardsIn(ZoneType.Hand));
        List<Integer> secondHand = ids(second.getCardsIn(ZoneType.Hand));
        List<Integer> libraryIds = ids(library);
        game.setAge(GameStage.Mulligan);
        controllers.get(0).onDanDanDecision = () -> game.setGameOver(GameEndReason.Draw);

        new MulliganService(first).perform();

        assertEquals(ids(first.getCardsIn(ZoneType.Hand)), firstHand);
        assertEquals(ids(second.getCardsIn(ZoneType.Hand)), secondHand);
        assertEquals(ids(first.getCardsIn(ZoneType.Library)), libraryIds);
        assertEquals(controllers.get(1).keepDecisions, 0,
                "concession releases the decision loop before prompting the other player");
    }

    @Test
    public void concessionDuringFreeHandRevealStopsBeforeReturningAnyHand() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player second = game.getPlayers().get(1);
        List<MulliganController> controllers = installControllers(game, first,
                List.of(), List.of(), null);
        controllers.get(0).danDanAnswers = new ArrayDeque<>(List.of(DanDanMulliganDecision.FREE));
        controllers.get(1).danDanAnswers = new ArrayDeque<>(List.of(DanDanMulliganDecision.FREE));
        addHand(first, "Memory Lapse", 7);
        addHand(second, "Island", 7);
        List<Card> library = addCards(first, "Memory Lapse", 20, ZoneType.Library);
        List<Integer> firstHand = ids(first.getCardsIn(ZoneType.Hand));
        List<Integer> secondHand = ids(second.getCardsIn(ZoneType.Hand));
        List<Integer> libraryIds = ids(library);
        game.setAge(GameStage.Mulligan);
        controllers.get(1).onReveal = () -> game.setGameOver(GameEndReason.Draw);

        new MulliganService(first).perform();

        assertEquals(ids(first.getCardsIn(ZoneType.Hand)), firstHand);
        assertEquals(ids(second.getCardsIn(ZoneType.Hand)), secondHand);
        assertEquals(ids(first.getCardsIn(ZoneType.Library)), libraryIds);
        assertEquals(controllers.get(0).shuffleInputs.size(), 0,
                "concession from the reveal callback must occur before return and shuffle");
        assertEquals(controllers.get(1).revealedCards, 7);
        assertEquals(controllers.get(0).revealedCards, 0,
                "the second free hand is not revealed after the first reveal causes concession");
    }

    @Test
    public void concessionDuringFirstPrivateBottomStopsBeforeMovingOrPromptingAgain() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player chosenFirst = game.getPlayers().get(1);
        List<MulliganController> controllers = installControllers(game, chosenFirst,
                List.of(), List.of(), null);
        controllers.get(0).danDanAnswers = new ArrayDeque<>(List.of(DanDanMulliganDecision.REGULAR));
        controllers.get(1).danDanAnswers = new ArrayDeque<>(List.of(DanDanMulliganDecision.REGULAR));
        addStableOpeningHand(first);
        addStableOpeningHand(chosenFirst);
        addCards(first, "Memory Lapse", 40, ZoneType.Library);
        game.setAge(GameStage.Mulligan);
        List<Integer> handAtConcession = new ArrayList<>();
        List<Integer> libraryAtConcession = new ArrayList<>();
        controllers.get(1).onTuck = () -> {
            handAtConcession.addAll(ids(chosenFirst.getCardsIn(ZoneType.Hand)));
            libraryAtConcession.addAll(ids(chosenFirst.getCardsIn(ZoneType.Library)));
            game.setGameOver(GameEndReason.Draw);
        };

        Random previousRandom = MyRandom.getRandom();
        try {
            MyRandom.setRandom(new Random(61));
            new MulliganService(chosenFirst).perform();

            int selectedBottomId = controllers.get(1).bottomedIds.get(0);
            assertEquals(controllers.get(1).tuckCalls, 1);
            assertEquals(controllers.get(0).tuckCalls, 0,
                    "concession during the chosen-first private bottom prompt stops before the other prompt");
            assertTrue(ids(chosenFirst.getCardsIn(ZoneType.Hand)).contains(selectedBottomId),
                    "the selected card remains in hand after the prompt concedes");
            assertTrue(!ids(chosenFirst.getCardsIn(ZoneType.Library)).contains(selectedBottomId),
                    "the selected card was not moved to the library");
            assertEquals(ids(chosenFirst.getCardsIn(ZoneType.Hand)), handAtConcession);
            assertEquals(ids(chosenFirst.getCardsIn(ZoneType.Library)), libraryAtConcession);
            controllers.forEach(MulliganController::assertAnswersConsumed);
        } finally {
            MyRandom.setRandom(previousRandom);
        }
    }

    @Test
    public void regularMulligansCanReachZeroButDoNotOfferAnEighthDeclaration() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player second = game.getPlayers().get(1);
        List<MulliganController> controllers = installControllers(game, first,
                List.of(), List.of(true), null);
        controllers.get(0).danDanAnswers = new ArrayDeque<>(List.of(
                DanDanMulliganDecision.REGULAR, DanDanMulliganDecision.REGULAR,
                DanDanMulliganDecision.REGULAR, DanDanMulliganDecision.REGULAR,
                DanDanMulliganDecision.REGULAR, DanDanMulliganDecision.REGULAR,
                DanDanMulliganDecision.REGULAR));
        addStableOpeningHand(first);
        addHand(second, "Memory Lapse", 7);
        addCards(first, "Memory Lapse", 90, ZoneType.Library);
        game.setAge(GameStage.Mulligan);
        Random previousRandom = MyRandom.getRandom();
        try {
            MyRandom.setRandom(new Random(61));
            new MulliganService(first).perform();

            assertEquals(first.getCardsIn(ZoneType.Hand).size(), 0,
                    "the seventh regular London mulligan redraws seven and immediately bottoms seven");
            assertEquals(controllers.get(0).danDanPenalties, List.of(0, 1, 2, 3, 4, 5, 6));
            assertEquals(controllers.get(0).keepDecisions, 7,
                    "no eighth regular decision is offered after the penalty reaches seven");
            assertEquals(controllers.get(0).danDanAnswers.size(), 0);
            assertEquals(second.getCardsIn(ZoneType.Hand).size(), 7);
        } finally {
            MyRandom.setRandom(previousRandom);
        }
    }

    private Game newGame(boolean danDan) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("seat one", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("seat two", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (danDan) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Match match = new Match(rules, players, "DanDan mulligan behavior test");
        return match.createGame();
    }

    private List<MulliganController> installControllers(Game game, Player chosenFirst, List<Boolean> seatOneAnswers,
            List<Boolean> seatTwoAnswers, List<DecisionSnapshot> snapshots) {
        return installControllers(game, chosenFirst, seatOneAnswers, seatTwoAnswers, snapshots, null);
    }

    private List<MulliganController> installControllers(Game game, Player chosenFirst, List<Boolean> seatOneAnswers,
            List<Boolean> seatTwoAnswers, List<DecisionSnapshot> snapshots, List<Integer> decisionPlayerIds) {
        List<MulliganController> controllers = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            List<Boolean> answers = player == game.getPlayers().get(0) ? seatOneAnswers : seatTwoAnswers;
            MulliganController controller = new MulliganController(game, player,
                    new LobbyPlayerAi(player.getName(), null), chosenFirst, answers, snapshots, decisionPlayerIds);
            player.dangerouslySetController(controller);
            controllers.add(controller);
        }
        return controllers;
    }

    private List<Card> addOpeningLibrary(Player player) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            cards.add(addCardToZone(i < 7 ? "Memory Lapse" : "Island", player, ZoneType.Library));
        }
        return cards;
    }

    private List<Card> addCards(Player player, String name, int count, ZoneType zone) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(addCardToZone(name, player, zone));
        }
        return cards;
    }

    private void addHand(Player player, String name, int count) {
        addCards(player, name, count, ZoneType.Hand);
    }

    private void addStableOpeningHand(Player player) {
        addHand(player, "Island", 3);
        addHand(player, "Memory Lapse", 4);
    }

    private static List<Integer> alternatingIds(List<Card> source, int parity) {
        List<Integer> result = new ArrayList<>();
        for (int i = parity; i < source.size(); i += 2) {
            result.add(source.get(i).getId());
        }
        return result;
    }

    private static List<Integer> alternatingIdsAfterFirst(List<Integer> source, int parity) {
        List<Integer> result = new ArrayList<>();
        for (int i = parity + 2; i < source.size(); i += 2) {
            result.add(source.get(i));
        }
        return result;
    }

    private static List<Integer> ids(Iterable<Card> cards) {
        List<Integer> result = new ArrayList<>();
        for (Card card : cards) result.add(card.getId());
        return result;
    }

    private record DecisionSnapshot(List<Integer> firstHand, List<Integer> secondHand, List<Integer> library) {}

    private static final class MulliganController extends PlayerControllerForTests {
        private final Player chosenFirst;
        private final ArrayDeque<Boolean> keepAnswers;
        private final List<DecisionSnapshot> snapshots;
        private final List<Integer> decisionPlayerIds;
        private ArrayDeque<DanDanMulliganDecision> danDanAnswers;
        private final List<Integer> danDanPenalties = new ArrayList<>();
        private final List<Boolean> danDanFreeChoices = new ArrayList<>();
        private int startingPlayerChoices;
        private int keepDecisions;
        private int revealedCards;
        private final List<List<Integer>> revealedHands = new ArrayList<>();
        private int tuckCalls;
        private Runnable onDanDanDecision;
        private Runnable onReveal;
        private List<Integer> shuffleOrder;
        private Runnable onTuck;
        private final List<List<Integer>> shuffleInputs = new ArrayList<>();
        private final List<Integer> bottomedIds = new ArrayList<>();

        private MulliganController(Game game, Player player, LobbyPlayerAi lobbyPlayer, Player chosenFirst,
                List<Boolean> keepAnswers, List<DecisionSnapshot> snapshots, List<Integer> decisionPlayerIds) {
            super(game, player, lobbyPlayer);
            this.chosenFirst = chosenFirst;
            this.keepAnswers = new ArrayDeque<>(keepAnswers);
            this.snapshots = snapshots;
            this.decisionPlayerIds = decisionPlayerIds;
        }

        @Override
        public Player chooseStartingPlayer(boolean isFirstGame) {
            startingPlayerChoices++;
            return chosenFirst;
        }

        @Override
        public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix,
                boolean addSuffix) {
            if (zone == ZoneType.Hand) {
                revealedCards += cards.size();
                revealedHands.add(ids(cards));
                if (onReveal != null) {
                    onReveal.run();
                }
            }
        }

        @Override
        public CardCollectionView cheatShuffle(CardCollectionView cards) {
            shuffleInputs.add(ids(cards));
            if (shuffleOrder != null) {
                shuffleOrder.add(getPlayer().getId());
            }
            return cards;
        }

        @Override
        public CardCollectionView tuckCardsViaMulligan(CardCollectionView hand, int cardsToReturn) {
            tuckCalls++;
            CardCollectionView cards = super.tuckCardsViaMulligan(hand, cardsToReturn);
            bottomedIds.addAll(ids(cards));
            if (onTuck != null) {
                onTuck.run();
            }
            return cards;
        }

        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            return null;
        }

        @Override
        public boolean mulliganKeepHand(Player firstPlayer, int cardsToReturn) {
            keepDecisions++;
            recordDecision();
            if (keepAnswers.isEmpty()) {
                throw new AssertionError("Unexpected extra mulligan decision for " + getPlayer());
            }
            return keepAnswers.removeFirst();
        }

        private void recordDecision() {
            if (decisionPlayerIds != null) {
                decisionPlayerIds.add(getPlayer().getId());
            }
            if (snapshots != null) {
                List<Player> players = getGame().getPlayers();
                snapshots.add(new DecisionSnapshot(ids(players.get(0).getCardsIn(ZoneType.Hand)),
                        ids(players.get(1).getCardsIn(ZoneType.Hand)),
                        ids(players.get(0).getCardsIn(ZoneType.Library))));
            }
        }

        @Override
        public DanDanMulliganDecision chooseDanDanMulligan(Player firstPlayer, int cardsToReturn,
                boolean canChooseFree) {
            danDanPenalties.add(cardsToReturn);
            danDanFreeChoices.add(canChooseFree);
            if (onDanDanDecision != null) {
                onDanDanDecision.run();
            }
            if (danDanAnswers == null) {
                return super.chooseDanDanMulligan(firstPlayer, cardsToReturn, canChooseFree);
            }
            keepDecisions++;
            recordDecision();
            if (danDanAnswers.isEmpty()) {
                throw new AssertionError("Unexpected extra DanDan decision for " + getPlayer());
            }
            return danDanAnswers.removeFirst();
        }

        private void assertAnswersConsumed() {
            assertTrue(keepAnswers.isEmpty(), "all scripted mulligan choices should be consumed for " + getPlayer());
            if (danDanAnswers != null) {
                assertTrue(danDanAnswers.isEmpty(), "all scripted DanDan choices should be consumed for " + getPlayer());
            }
        }
    }
}
