package forge.net;

import forge.LobbyPlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.game.GameStage;
import forge.game.GameView;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.card.MagicColor;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.StackItemView;
import forge.game.spellability.SpellAbility;
import forge.game.ability.ApiType;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputPassPriority;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventCardChangeZone;
import com.google.common.eventbus.Subscribe;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.NetworkLogConfig;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.NetGameController;
import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.RemoteClientGuiGame;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IGameController;
import forge.interfaces.IUpdateable;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.ai.PlayerControllerAi;
import forge.util.MyRandom;
import forge.util.Localizer;
import forge.util.MessageUtil;
import org.testng.Assert;
import org.testng.IConfigurationListener;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.TestNG;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.lang.reflect.Field;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;

/** Focused live-state proof for the Constructed plus DanDan view over ordinary TCP. */
public class DanDanNetworkPlayTest {
    private static final String QUEUED_CHILD_PROPERTY = "forge.tcp.queuedLocalQuit.child";
    private static final String QUEUED_CHILD_MODE_PROPERTY = "forge.tcp.queuedLocalQuit.mode";
    private static final String QUEUED_CHILD_STATUS_PROPERTY = "forge.tcp.queuedLocalQuit.status";
    private static final String QUEUED_CHILD_TEST_METHOD =
            "constructedDanDanQueuedLocalOldControllerQuitHasN1Witness";
    private static final String[] QUEUED_CHILD_ALLOWED_OPENS = {
            "java.base/java.lang=ALL-UNNAMED",
            "java.base/java.time=ALL-UNNAMED",
            "java.base/java.text=ALL-UNNAMED",
            "java.base/java.util=ALL-UNNAMED",
            "java.base/java.util.regex=ALL-UNNAMED",
            "java.base/java.util.stream=ALL-UNNAMED",
            "java.base/java.lang.reflect=ALL-UNNAMED",
            "java.desktop/javax.imageio.spi=ALL-UNNAMED"
    };
    private static final long QUEUED_CHILD_STARTUP_TIMEOUT_SECONDS = 180;
    private static final long QUEUED_CHILD_CASE_TIMEOUT_SECONDS = 360;
    private static final long QUEUED_CHILD_DISPOSAL_GRACE_SECONDS = 3;
    private static final AtomicReference<QueuedChildSemanticReceipt> QUEUED_CHILD_SEMANTIC_RECEIPT =
            new AtomicReference<>();
    private static ScriptedHumanTcpSupportTest fixtureOwner;

    private enum LocalDecisionQueueMode { NONE, QUEUED_OLD_CONTROLLER_QUIT }
    private enum TcpOwnManaUndoMode { NONE, POSITIVE_REMOTE_OWN_UNDO }
    private enum PrivacyHistoryReconnectMode { NONE, RECONNECT_ONLY, RECONNECT_AND_NEW_GAME }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanDisconnectTakeoverContinuesWithOneHuman(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, false, false,
                false, false, false, LocalDecisionQueueMode.NONE, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanRemoteOwnManaUndoOverTcp(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, false, false,
                false, false, false, LocalDecisionQueueMode.NONE, false, false,
                TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO);
    }

    private record DisconnectDrawReceipt(int gameViewId, int cardId, int fromOwnerViewId,
            int toOwnerViewId, int eventOwnerViewId, int actualOwnerViewId, int turn, PhaseType phase,
            int turnPlayerViewId, boolean sharedLibraryAlias, boolean sharedGraveyardAlias,
            int physicalCardCount, int uniqueCardCount, boolean cardInHostHand, boolean cardRemovedFromLibrary) { }

    private static final class DisconnectDrawObserver {
        private final Game game;
        private final Player host;
        private final CountDownLatch drawn = new CountDownLatch(1);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private volatile DisconnectDrawReceipt receipt;

        private DisconnectDrawObserver(Game game, Player host) { this.game = game; this.host = host; }

        @Subscribe public void onCardZoneChange(GameEventCardChangeZone event) {
            try {
                if (receipt != null || event.card() == null || event.from() == null || event.to() == null
                        || event.from().zoneType() != ZoneType.Library || event.to().zoneType() != ZoneType.Hand
                        || event.to().player() == null || event.to().player().getId() != host.getId()) return;
                Player turnPlayer = game.getPhaseHandler().getPlayerTurn();
                if (game.getPhaseHandler().getPhase() != PhaseType.DRAW || turnPlayer != host) return;
                Card card = game.getCardsInGame().stream()
                        .filter(candidate -> candidate.getId() == event.card().getId()).findFirst().orElse(null);
                if (card == null || card.getOwner() == null || card.getZone() == null
                        || card.getZone().getZoneType() != ZoneType.Hand || card.getZone().getPlayer() != host) {
                    throw new AssertionError("actual host DRAW zone event did not leave its card in the host hand");
                }
                List<Player> players = game.getRegisteredPlayers();
                var library = players.get(0).getZone(ZoneType.Library);
                var graveyard = players.get(0).getZone(ZoneType.Graveyard);
                boolean sharedLibrary = players.stream().allMatch(player -> player.getZone(ZoneType.Library) == library);
                boolean sharedGraveyard = players.stream().allMatch(player -> player.getZone(ZoneType.Graveyard) == graveyard);
                Set<Integer> physical = game.getCardsInGame().stream().map(Card::getId)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                boolean cardInHostHand = host.getZone(ZoneType.Hand).getCards().stream()
                        .anyMatch(candidate -> candidate.getId() == card.getId());
                boolean cardRemovedFromLibrary = library.getCards().stream()
                        .noneMatch(candidate -> candidate.getId() == card.getId());
                receipt = new DisconnectDrawReceipt(game.getView().getId(), card.getId(),
                        event.from().player() == null ? -1 : event.from().player().getId(),
                        event.to().player().getId(), event.card().getOwner() == null ? -1 : event.card().getOwner().getId(),
                        card.getOwner().getId(), game.getPhaseHandler().getTurn(),
                        game.getPhaseHandler().getPhase(), turnPlayer.getId(), sharedLibrary, sharedGraveyard,
                        game.getCardsInGame().size(), physical.size(), cardInHostHand, cardRemovedFromLibrary);
                drawn.countDown();
            } catch (Throwable error) {
                failure.compareAndSet(null, error);
                drawn.countDown();
            }
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException { return drawn.await(timeout, unit); }
        Throwable failure() { return failure.get(); }
    }

    private record GuiPlayerState(String name, int avatar, int sleeve, String avatarCardKey,
                                  String sleeveArtKey, int sleeveArtOffset) { }
    private record MetamorphoseSetup(int hostViewId, int remoteViewId, int spellId, int targetId,
                                     List<Integer> hostManaIds, int hostOpeningIslandId, int remoteHandCardId) {
        private MetamorphoseSetup { hostManaIds = List.copyOf(hostManaIds); }
    }
    private record TcpOwnManaUndoSetup(Game game, int gameViewId, int remoteViewId, int islandCardId,
                                       int priorOwnerViewId, int recipientOwnerViewId,
                                       Set<Integer> physicalCardIds) {
        private TcpOwnManaUndoSetup {
            physicalCardIds = Set.copyOf(physicalCardIds);
        }
    }
    private record PrivacySetup(int hostViewId, int remoteViewId, int tellingTimeSeat, int fengrafSeat,
                                int tellingTimeId, int daysUndoingId, int fengrafId,
                                int fengrafAbilityId, int publicCreatureId, int negativeHandId, List<Integer> privateTopIds,
                                List<Integer> initialLibraryIds,
                                List<Integer> remoteManaIds, List<Integer> hostManaIds,
                                Set<String> privateTopNames, Set<String> chooserKnownNames,
                                String negativeHandName, String expectedRevealMessage,
                                Set<Integer> physicalCardIds) {
        private PrivacySetup {
            privateTopIds = List.copyOf(privateTopIds);
            initialLibraryIds = List.copyOf(initialLibraryIds);
            remoteManaIds = List.copyOf(remoteManaIds);
            hostManaIds = List.copyOf(hostManaIds);
            privateTopNames = Set.copyOf(privateTopNames);
            chooserKnownNames = Set.copyOf(chooserKnownNames);
            physicalCardIds = Set.copyOf(physicalCardIds);
        }
    }
    private record PrivacyTerminalReceipt(GameView terminalGameView, int inboundBytesAtOpening) { }
    private record CardState(String zone, int destinationPlayerId, int ownerId, int controllerId) { }
    private record LifecycleState(int gameViewId, Object trackerIdentity, Map<Integer, CardState> cards,
                                  Set<Integer> conflictingCardIds) {
        private LifecycleState {
            cards = Map.copyOf(cards);
            conflictingCardIds = Set.copyOf(conflictingCardIds);
        }
    }
    private record LifecycleCleanupReceipt(Game game, int gameViewId, int outcomeCount,
                                           boolean outcomeObserved, boolean engineReturnObserved) {
        boolean proves(Game expectedGame) {
            return game == expectedGame && gameViewId == expectedGame.getView().getId()
                    && outcomeCount == 1 && outcomeObserved && engineReturnObserved;
        }
    }
    private record QueuedLocalDecisionFacts(int oldGameViewId, int nextGameViewId,
            boolean retainedOriginIsOldGame, boolean nextGameIsDistinct,
            boolean transitionQueuedBeforeObservation, boolean staleQuitPostedAfterObservation,
            boolean preparationReceiptMatched, int preparationShuffleCount,
            boolean hostedGameIsNextGame, int registeredNextControllerCount,
            boolean registeredNextControllersMatch, int hostAfterGameEndCount,
            int remoteAfterGameEndCount, int matchOverCount, boolean nextGameOver,
            int nextGameOutcomeCount) { }

    private static final class LifecycleTrace {
        private final CountDownLatch outcome = new CountDownLatch(1);
        private final CountDownLatch engineReturn = new CountDownLatch(1);
        private volatile Game game;
        private volatile LifecycleState outcomeState;
        private volatile LifecycleState engineReturnState;
        private volatile int outcomeCount;
        private volatile Set<Integer> expectedPhysicalCardIds;
        private volatile boolean outcomeCardIdsMatch;
        private volatile boolean engineReturnCardIdsMatch;

        void attach(Game actualGame) {
            if (game != null) throw new AssertionError("Lifecycle trace attached twice");
            game = actualGame;
            actualGame.subscribeToEvents(this);
        }
        void expectPhysicalCardIds(Set<Integer> cardIds) { expectedPhysicalCardIds = Set.copyOf(cardIds); }
        @Subscribe public void onOutcome(GameEventGameOutcome ignored) {
            outcomeCount++;
            outcomeState = snapshotGame(game);
            outcomeCardIdsMatch = expectedPhysicalCardIds != null
                    && outcomeState.cards().keySet().equals(expectedPhysicalCardIds);
            outcome.countDown();
        }
        void recordEngineReturn(Game returnedGame) {
            if (returnedGame != game) throw new AssertionError("HostedMatch hook returned from another game");
            engineReturnState = snapshotGame(returnedGame);
            engineReturnCardIdsMatch = expectedPhysicalCardIds != null
                    && engineReturnState.cards().keySet().equals(expectedPhysicalCardIds);
            engineReturn.countDown();
        }
        int outcomeCount() { return outcomeCount; }
        boolean awaitOutcome(long timeout, TimeUnit unit) throws InterruptedException { return outcome.await(timeout, unit); }
        boolean awaitEngineReturn(long timeout, TimeUnit unit) throws InterruptedException { return engineReturn.await(timeout, unit); }
    }

    private static LifecycleState snapshotGame(Game game) {
        Map<Integer, CardState> cards = new LinkedHashMap<>();
        Set<Integer> conflictingCardIds = new HashSet<>();
        for (Player player : game.getRegisteredPlayers()) {
            for (Card card : player.getAllCards()) {
                CardState current = new CardState(
                    card.getZone() == null ? "null" : card.getZone().getZoneType().name(),
                    card.getZone() == null || card.getZone().getPlayer() == null ? -1 : card.getZone().getPlayer().getId(),
                    card.getOwner() == null ? -1 : card.getOwner().getId(),
                    card.getController() == null ? -1 : card.getController().getId());
                CardState previous = cards.putIfAbsent(card.getId(), current);
                if (previous != null && !previous.equals(current)) conflictingCardIds.add(card.getId());
            }
        }
        GameView view = game.getView();
        return new LifecycleState(view.getId(), view.getTracker(), cards, conflictingCardIds);
    }

    @BeforeClass
    public void initializeFixtureProfile() throws IOException {
        if (!Boolean.getBoolean("forge.tcp.support.isolated")) {
            throw new SkipException("Run this test only in a fresh isolated Surefire process");
        }
        fixtureOwner = new ScriptedHumanTcpSupportTest();
        fixtureOwner.initializeCardDatabaseOutsideCaseTimeout();
    }

    @AfterClass(alwaysRun = true)
    public void restoreFixtureProfile() throws IOException {
        if (fixtureOwner != null) fixtureOwner.restoreGuiAfterCardDatabaseInitialization();
    }

    @AfterMethod(alwaysRun = true)
    public void restorePreferencesAfterWritersStop() throws IOException {
        if (fixtureOwner != null) fixtureOwner.restoreMainPreferencesAfterFixtureWritersStop();
    }

    @DataProvider(name = "syncModes")
    public Object[][] syncModes() {
        String queuedChildMode = Boolean.getBoolean(QUEUED_CHILD_PROPERTY)
                ? System.getProperty(QUEUED_CHILD_MODE_PROPERTY) : null;
        if (queuedChildMode != null) {
            return switch (queuedChildMode) {
                case "full" -> new Object[][] {{false}};
                case "delta" -> new Object[][] {{true}};
                default -> throw new IllegalStateException("Unknown queued child mode");
            };
        }
        return new Object[][] {{false}, {true}}; // full-state first, then the delta counterpart
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanLiveStateUsesSharedZonesAndAssignedSeats(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanMulliganUsesActualFreeRegularAndLondonChoices(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanFreeMulliganRevealReachesRemotePeer(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, true, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanPrivacyHistoryUsesActualFreeRegularLondonPrefix(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, true, true, false, null, true, false, false, false, false,
                LocalDecisionQueueMode.NONE, false, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanPrivacyHistoryReconnectsSameGameAndResumesPriority(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, true, true, false, null, true, false, false, false, false,
                LocalDecisionQueueMode.NONE, false, true, TcpOwnManaUndoMode.NONE,
                PrivacyHistoryReconnectMode.RECONNECT_ONLY);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanPrivacyHistoryReconnectResetsIntoFreshGame(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, true, true, false, null, true, false, false, false, false,
                LocalDecisionQueueMode.NONE, false, true, TcpOwnManaUndoMode.NONE,
                PrivacyHistoryReconnectMode.RECONNECT_AND_NEW_GAME);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanReconnectResumesSameSeatAndState(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanMetamorphoseMayDeclineOptionalHandEntry(boolean deltaSync) throws Throwable {
        runDanDanMetamorphoseCase(deltaSync, false);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanMetamorphoseMayChooseFromTargetControllersHand(boolean deltaSync) throws Throwable {
        runDanDanMetamorphoseCase(deltaSync, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanHeldMetamorphoseConcessionContinuesAndQuits(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, false, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanRetainedOldControllerActionHasN1Witness(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, false, true, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanRetainedOldControllerQuitHasN1Witness(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, false, true, false, false, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 0)
    public void constructedDanDanQueuedLocalOldControllerQuitHasN1Witness(boolean deltaSync) throws Throwable {
        if (Boolean.getBoolean(QUEUED_CHILD_PROPERTY)) {
            String expectedMode = deltaSync ? "delta" : "full";
            if (!expectedMode.equals(System.getProperty(QUEUED_CHILD_MODE_PROPERTY))) {
                throw new AssertionError("queued child provider selected a mismatched mode");
            }
            QUEUED_CHILD_SEMANTIC_RECEIPT.set(null);
            writeQueuedChildStatus("CASE_STARTED", expectedMode, -1, -1, -1, -1, "none", null);
        } else {
            runQueuedLocalDecisionChild(deltaSync);
            return;
        }
        runDanDanCase(deltaSync, false, false, false, null, false, true, false, false, false,
                LocalDecisionQueueMode.QUEUED_OLD_CONTROLLER_QUIT);
        if (Boolean.getBoolean(QUEUED_CHILD_PROPERTY)) {
            QueuedChildSemanticReceipt receipt = QUEUED_CHILD_SEMANTIC_RECEIPT.get();
            if (receipt == null || !receipt.complete()) {
                throw new AssertionError("queued child method returned without its semantic cleanup receipt");
            }
        }
    }

    /** Child entry point runs only this TestNG method and emits fixed scalar status records. */
    public static final class QueuedLocalDecisionChild {
        private QueuedLocalDecisionChild() { }

        public static void main(String[] args) {
            String mode = args.length == 1 ? args[0] : "invalid";
            ChildResultCounts counts = new ChildResultCounts();
            TestNG testng = new TestNG();
            testng.setUseDefaultListeners(false);
            String statusPath = System.getProperty(QUEUED_CHILD_STATUS_PROPERTY);
            if (statusPath != null) {
                testng.setOutputDirectory(Path.of(statusPath).getParent().resolve("test-output").toString());
            }
            testng.addListener(counts);
            XmlSuite suite = new XmlSuite();
            suite.setName("queued-local-decision-child");
            XmlTest test = new XmlTest(suite);
            test.setName("queued-local-decision-" + mode);
            XmlClass testClass = new XmlClass(DanDanNetworkPlayTest.class);
            testClass.setIncludedMethods(List.of(new XmlInclude(QUEUED_CHILD_TEST_METHOD)));
            test.setXmlClasses(List.of(testClass));
            testng.setXmlSuites(List.of(suite));

            try {
                if (!Boolean.getBoolean(QUEUED_CHILD_PROPERTY)
                        || statusPath == null || statusPath.isBlank()
                        || !mode.equals(System.getProperty(QUEUED_CHILD_MODE_PROPERTY))
                        || !("full".equals(mode) || "delta".equals(mode))) {
                    throw new IllegalArgumentException("invalid queued child launch configuration");
                }
                testng.run();
                QueuedChildSemanticReceipt semantic = QUEUED_CHILD_SEMANTIC_RECEIPT.get();
                boolean passed = testng.getStatus() == 0 && counts.passed.get() == 1
                        && counts.failed.get() == 0 && counts.skipped.get() == 0
                        && counts.configurationFailures.get() == 0
                        && counts.configurationSkips.get() == 0 && semantic != null && semantic.complete();
                writeQueuedChildStatus(passed ? "PASS" : "TEST_FAILED", mode, counts.passed.get(),
                        counts.failed.get(), counts.skipped.get(),
                        counts.configurationFailures.get() + counts.configurationSkips.get(),
                        counts.failureType(), semantic);
                if (!passed) System.exit(1);
                System.exit(0);
            } catch (Throwable failure) {
                try {
                    writeQueuedChildStatus("RUNNER_FAILED", mode, counts.passed.get(), counts.failed.get(),
                            counts.skipped.get(),
                            counts.configurationFailures.get() + counts.configurationSkips.get(),
                            failure.getClass().getName(), QUEUED_CHILD_SEMANTIC_RECEIPT.get());
                } catch (Throwable ignored) {
                    // The parent treats a missing or malformed status as failure.
                }
                System.exit(1);
            }
        }
    }

    private static final class ChildResultCounts implements ITestListener, IConfigurationListener {
        private final AtomicInteger passed = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicInteger skipped = new AtomicInteger();
        private final AtomicInteger configurationFailures = new AtomicInteger();
        private final AtomicInteger configurationSkips = new AtomicInteger();
        private volatile String failureType = "none";

        @Override public void onTestSuccess(ITestResult result) { passed.incrementAndGet(); }
        @Override public void onTestFailure(ITestResult result) {
            failed.incrementAndGet();
            retainFailureType(result);
        }
        @Override public void onTestFailedWithTimeout(ITestResult result) {
            failed.incrementAndGet();
            retainFailureType(result);
        }
        @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
            failed.incrementAndGet();
            retainFailureType(result);
        }
        @Override public void onTestSkipped(ITestResult result) {
            skipped.incrementAndGet();
            retainFailureType(result);
        }
        @Override public void onConfigurationFailure(ITestResult result) {
            configurationFailures.incrementAndGet();
            retainFailureType(result);
        }
        @Override public void onConfigurationSkip(ITestResult result) {
            configurationSkips.incrementAndGet();
            retainFailureType(result);
        }
        private void retainFailureType(ITestResult result) {
            Throwable failure = result.getThrowable();
            if (failure != null) failureType = failure.getClass().getName();
        }
        String failureType() { return failureType; }
    }

    private record QueuedChildSemanticReceipt(boolean queuedConsumerWitness,
            boolean sameGameOutcomeObserved, boolean sameGameEngineReturnObserved) {
        boolean complete() {
            return queuedConsumerWitness && sameGameOutcomeObserved && sameGameEngineReturnObserved;
        }
    }

    private record QueuedChildStatus(String mode, String state, int passed, int failed, int skipped,
                                     int configurationFailures, String failureType,
                                     boolean queuedConsumerWitness, boolean sameGameOutcomeObserved,
                                     boolean sameGameEngineReturnObserved) {
        String encode() {
            return String.join("\t", "QUEUED_CHILD", "1", mode, state, Integer.toString(passed),
                    Integer.toString(failed), Integer.toString(skipped),
                    Integer.toString(configurationFailures), failureType,
                    Boolean.toString(queuedConsumerWitness), Boolean.toString(sameGameOutcomeObserved),
                    Boolean.toString(sameGameEngineReturnObserved));
        }

        static QueuedChildStatus read(Path path) {
            try {
                String[] fields = Files.readString(path, StandardCharsets.UTF_8).strip().split("\t", -1);
                if (fields.length != 12 || !"QUEUED_CHILD".equals(fields[0]) || !"1".equals(fields[1])) return null;
                if (!("true".equals(fields[9]) || "false".equals(fields[9]))
                        || !("true".equals(fields[10]) || "false".equals(fields[10]))
                        || !("true".equals(fields[11]) || "false".equals(fields[11]))) return null;
                return new QueuedChildStatus(fields[2], fields[3], Integer.parseInt(fields[4]),
                        Integer.parseInt(fields[5]), Integer.parseInt(fields[6]),
                        Integer.parseInt(fields[7]), fields[8], Boolean.parseBoolean(fields[9]),
                        Boolean.parseBoolean(fields[10]), Boolean.parseBoolean(fields[11]));
            } catch (IOException | NumberFormatException failure) {
                return null;
            }
        }

        boolean isTerminal() {
            return "PASS".equals(state) || "TEST_FAILED".equals(state) || "RUNNER_FAILED".equals(state)
                    || "INCONCLUSIVE".equals(state);
        }
    }

    private static void runQueuedLocalDecisionChild(boolean deltaSync) throws Throwable {
        String mode = deltaSync ? "delta" : "full";
        Path childRoot = Files.createTempDirectory("forge-queued-local-quit-");
        Path childHome = childRoot.resolve("home");
        Path statusPath = childRoot.resolve("status.tsv");
        Process child = null;
        Throwable failure = null;
        try {
            Files.createDirectories(childHome);
            List<String> command = new ArrayList<>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            command.add("-Duser.home=" + childHome.toAbsolutePath());
            command.add("-Dforge.tcp.support.isolated=true");
            command.add("-D" + QUEUED_CHILD_PROPERTY + "=true");
            command.add("-D" + QUEUED_CHILD_MODE_PROPERTY + "=" + mode);
            command.add("-D" + QUEUED_CHILD_STATUS_PROPERTY + "=" + statusPath.toAbsolutePath());
            command.addAll(activeQueuedChildOpens());
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(QueuedLocalDecisionChild.class.getName());
            command.add(mode);
            child = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();

            if (!awaitQueuedChildCaseStart(child, statusPath, mode)) {
                if (child.isAlive()) {
                    throw new AssertionError("INCONCLUSIVE: queued child did not finish isolated fixture startup");
                }
                int exit = child.exitValue();
                QueuedChildStatus status = QueuedChildStatus.read(statusPath);
                throw new AssertionError("queued child exited before case entry: mode=" + mode
                        + ", exit=" + exit + ", status=" + describeQueuedChildStatus(status));
            }
            if (!child.waitFor(QUEUED_CHILD_CASE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError("INCONCLUSIVE: queued child exceeded the 360-second case budget");
            }
            int exit = child.exitValue();
            QueuedChildStatus status = QueuedChildStatus.read(statusPath);
            boolean passed = exit == 0 && status != null && "PASS".equals(status.state())
                    && mode.equals(status.mode()) && status.passed() == 1 && status.failed() == 0
                    && status.skipped() == 0 && status.configurationFailures() == 0
                    && "none".equals(status.failureType()) && status.queuedConsumerWitness()
                    && status.sameGameOutcomeObserved() && status.sameGameEngineReturnObserved();
            System.out.println("QUEUED_LOCAL_CHILD mode=" + mode + " state="
                    + (passed ? "PASS" : status == null ? "MISSING_STATUS" : status.state())
                    + " exit=" + exit + " passed=" + (status == null ? -1 : status.passed())
                    + " failed=" + (status == null ? -1 : status.failed())
                    + " skipped=" + (status == null ? -1 : status.skipped())
                    + " configFailures=" + (status == null ? -1 : status.configurationFailures())
                    + " queuedWitness=" + (status != null && status.queuedConsumerWitness())
                    + " sameGameOutcome=" + (status != null && status.sameGameOutcomeObserved())
                    + " sameGameEngineReturn=" + (status != null && status.sameGameEngineReturnObserved()));
            if (!passed) throw new AssertionError("queued child result failed validation: mode=" + mode
                    + ", exit=" + exit + ", status=" + describeQueuedChildStatus(status));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            failure = new AssertionError("INCONCLUSIVE: queued child supervision was interrupted", interrupted);
        } catch (Throwable childFailure) {
            failure = childFailure;
        } finally {
            boolean childStopped = child == null || !child.isAlive();
            if (child != null && !childStopped) {
                childStopped = stopOwnedQueuedChild(child);
            }
            if (!childStopped) {
                failure = combine(failure, new AssertionError(
                        "owned queued child could not be confirmed dead; its private profile is retained"));
            } else {
                try { deleteQueuedChildTree(childRoot); }
                catch (Throwable deletionFailure) { failure = combine(failure, deletionFailure); }
            }
        }
        if (failure != null) throw failure;
    }

    private static boolean awaitQueuedChildCaseStart(Process child, Path statusPath, String mode)
            throws InterruptedException {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(QUEUED_CHILD_STARTUP_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            QueuedChildStatus status = QueuedChildStatus.read(statusPath);
            if (status != null && mode.equals(status.mode())
                    && ("CASE_STARTED".equals(status.state()) || status.isTerminal())) return true;
            if (!child.isAlive()) return false;
            Thread.sleep(25);
        }
        return false;
    }

    private static boolean stopOwnedQueuedChild(Process child) {
        if (child.isAlive()) child.destroy();
        if (awaitQueuedChildExit(child, QUEUED_CHILD_DISPOSAL_GRACE_SECONDS)) return true;
        child.destroyForcibly();
        return awaitQueuedChildExit(child, QUEUED_CHILD_DISPOSAL_GRACE_SECONDS);
    }

    private static boolean awaitQueuedChildExit(Process child, long seconds) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        boolean interrupted = Thread.interrupted();
        try {
            while (child.isAlive() && System.nanoTime() < deadline) {
                try {
                    child.waitFor(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            return !child.isAlive();
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static void deleteQueuedChildTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static String describeQueuedChildStatus(QueuedChildStatus status) {
        if (status == null) return "missing-or-malformed";
        return "state=" + status.state() + ",mode=" + status.mode() + ",passed=" + status.passed()
                + ",failed=" + status.failed() + ",skipped=" + status.skipped()
                + ",configFailures=" + status.configurationFailures()
                + ",queuedWitness=" + status.queuedConsumerWitness()
                + ",sameGameOutcome=" + status.sameGameOutcomeObserved()
                + ",sameGameEngineReturn=" + status.sameGameEngineReturnObserved()
                + ",failureType=" + status.failureType();
    }

    private static void writeQueuedChildStatus(String state, String mode, int passed, int failed,
            int skipped, int configurationFailures, String failureType,
            QueuedChildSemanticReceipt semantic) throws IOException {
        String statusPath = System.getProperty(QUEUED_CHILD_STATUS_PROPERTY);
        if (statusPath == null || statusPath.isBlank()) throw new IOException("queued child status path is missing");
        Path target = Path.of(statusPath);
        Path pending = target.resolveSibling(target.getFileName() + ".pending");
        Files.writeString(pending, new QueuedChildStatus(mode, state, passed, failed, skipped,
                configurationFailures, failureType, semantic != null && semantic.queuedConsumerWitness(),
                semantic != null && semantic.sameGameOutcomeObserved(),
                semantic != null && semantic.sameGameEngineReturnObserved()).encode(), StandardCharsets.UTF_8);
        try {
            Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void abortQueuedChildBeforeUnsafeCleanup(String category, Throwable... failures) {
        if (!Boolean.getBoolean(QUEUED_CHILD_PROPERTY)) {
            throw new AssertionError("unsafe queued-child cleanup guard reached outside its isolated child");
        }
        try {
            String mode = System.getProperty(QUEUED_CHILD_MODE_PROPERTY, "unknown");
            writeQueuedChildStatus("INCONCLUSIVE", mode, 0, 0, 0, 0,
                    "unsafe-cleanup:" + category + ":" + queuedChildFailureTypes(failures),
                    QUEUED_CHILD_SEMANTIC_RECEIPT.get());
        } catch (Throwable ignored) {
            // Even a failed status write must not resume cleanup against a possibly live Game worker.
        } finally {
            Runtime.getRuntime().halt(86);
        }
        throw new AssertionError("unreachable after isolated queued child abort");
    }

    private static String queuedChildFailureTypes(Throwable... failures) {
        List<String> types = new ArrayList<>();
        for (Throwable failure : failures) {
            if (failure == null) continue;
            types.add(failure.getClass().getSimpleName());
            for (Throwable suppressed : failure.getSuppressed()) {
                if (types.size() == 8) break;
                types.add(suppressed.getClass().getSimpleName());
            }
            if (types.size() == 8) break;
        }
        return types.isEmpty() ? "none" : String.join(",", types);
    }

    private static List<String> activeQueuedChildOpens() {
        List<String> active = new ArrayList<>();
        List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (int i = 0; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            String payload;
            if ("--add-opens".equals(argument) && i + 1 < arguments.size()) {
                payload = arguments.get(++i);
            } else if (argument.startsWith("--add-opens=")) {
                payload = argument.substring("--add-opens=".length());
            } else continue;
            for (String allowed : QUEUED_CHILD_ALLOWED_OPENS) {
                if (allowed.equals(payload)) {
                    active.add("--add-opens=" + payload);
                    break;
                }
            }
        }
        return active;
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanPrivateLookAndPublicGraveyardRevealStaySeatScoped(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanInversePrivacyActorsStaySeatScoped(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, true, false, false, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanPrivacyHistoryReshufflesAndEndsTurn(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, true, false, false,
                false, false, LocalDecisionQueueMode.NONE, false, true);
    }

    private void runDanDanMetamorphoseCase(boolean deltaSync, boolean acceptHandEntry) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, acceptHandEntry);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, false, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario, null);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, lifecycleScenario, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario, boolean staleActionWitness) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, lifecycleScenario, staleActionWitness, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario, boolean staleActionWitness,
                               boolean inversePrivacyActors) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, lifecycleScenario, staleActionWitness,
                inversePrivacyActors, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario, boolean staleActionWitness,
                               boolean inversePrivacyActors, boolean staleQuitWitness) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, lifecycleScenario, staleActionWitness,
                inversePrivacyActors, staleQuitWitness, LocalDecisionQueueMode.NONE);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario, boolean staleActionWitness,
                               boolean inversePrivacyActors, boolean staleQuitWitness,
                               LocalDecisionQueueMode localDecisionQueueMode) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, lifecycleScenario, staleActionWitness,
                inversePrivacyActors, staleQuitWitness, localDecisionQueueMode, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario, boolean staleActionWitness,
                               boolean inversePrivacyActors, boolean staleQuitWitness,
                               LocalDecisionQueueMode localDecisionQueueMode,
                               boolean disconnectAiScenario) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, lifecycleScenario, staleActionWitness,
                inversePrivacyActors, staleQuitWitness, localDecisionQueueMode, disconnectAiScenario, false);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario, boolean staleActionWitness,
                               boolean inversePrivacyActors, boolean staleQuitWitness,
                               LocalDecisionQueueMode localDecisionQueueMode,
                               boolean disconnectAiScenario, boolean privacyShuffleHistoryScenario) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, lifecycleScenario, staleActionWitness,
                inversePrivacyActors, staleQuitWitness, localDecisionQueueMode,
                disconnectAiScenario, privacyShuffleHistoryScenario, TcpOwnManaUndoMode.NONE);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario, boolean staleActionWitness,
                               boolean inversePrivacyActors, boolean staleQuitWitness,
                               LocalDecisionQueueMode localDecisionQueueMode,
                               boolean disconnectAiScenario, boolean privacyShuffleHistoryScenario,
                               TcpOwnManaUndoMode tcpOwnManaUndoMode) throws Throwable {
        runDanDanCase(deltaSync, mulliganScenario, hostStarterRevealScenario, reconnectScenario,
                metamorphoseAccept, privacyScenario, lifecycleScenario, staleActionWitness,
                inversePrivacyActors, staleQuitWitness, localDecisionQueueMode,
                disconnectAiScenario, privacyShuffleHistoryScenario, tcpOwnManaUndoMode,
                PrivacyHistoryReconnectMode.NONE);
    }

    private void runDanDanCase(boolean deltaSync, boolean mulliganScenario,
                               boolean hostStarterRevealScenario, boolean reconnectScenario,
                               Boolean metamorphoseAccept, boolean privacyScenario,
                               boolean lifecycleScenario, boolean staleActionWitness,
                               boolean inversePrivacyActors, boolean staleQuitWitness,
                               LocalDecisionQueueMode localDecisionQueueMode,
                               boolean disconnectAiScenario, boolean privacyShuffleHistoryScenario,
                               TcpOwnManaUndoMode tcpOwnManaUndoMode,
                               PrivacyHistoryReconnectMode privacyHistoryReconnectMode) throws Throwable {
        final IGuiBase oldGui = GuiBase.getInterface();
        final boolean oldDelta = RemoteClientGuiGame.useDeltaSync;
        final String oldLegality = FModel.getPreferences().getPref(FPref.ENFORCE_DECK_LEGALITY);
        final String oldPlayerName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
        final String oldMusic = FModel.getPreferences().getPref(FPref.UI_ENABLE_MUSIC);
        final String oldUndoMode = FModel.getPreferences().getPref(FPref.UI_UNDO_MODE);
        final String oldAutoPassNoActions = FModel.getPreferences().getPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS);
        final String oldRandomFoil = FModel.getPreferences().getPref(FPref.UI_RANDOM_FOIL);
        final String oldSelectFromCardDisplays = FModel.getPreferences().getPref(FPref.UI_SELECT_FROM_CARD_DISPLAYS);
        final LobbyPlayer guiPlayer = GamePlayerUtil.getGuiPlayer();
        final GuiPlayerState oldGuiPlayer = snapshotGuiPlayer(guiPlayer);
        final Random oldRandom = MyRandom.getRandom();
        final FServerManager server = FServerManager.getInstance();
        final ScriptedHumanTcpSupport.Scenario scenario = new ScriptedHumanTcpSupport.Scenario();
        if (tcpOwnManaUndoMode == TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO) {
            scenario.enablePositiveRemoteOwnManaUndo();
            scenario.configureStarterSeatZeroScenario();
        }
        if (privacyScenario) {
            scenario.beginPrivacyScenario(deltaSync, inversePrivacyActors ? 0 : 1, inversePrivacyActors ? 1 : 0);
            if (privacyShuffleHistoryScenario) scenario.enablePrivacyShuffleHistoryScenario();
            scenario.configureStarterSeatZeroScenario();
        }
        if (privacyHistoryReconnectMode != PrivacyHistoryReconnectMode.NONE) {
            if (!mulliganScenario || !hostStarterRevealScenario || reconnectScenario || disconnectAiScenario
                    || !privacyScenario || !privacyShuffleHistoryScenario || metamorphoseAccept != null
                    || lifecycleScenario || staleActionWitness || staleQuitWitness
                    || tcpOwnManaUndoMode != TcpOwnManaUndoMode.NONE) {
                throw new IllegalArgumentException("Reconnect continuation requires only the composite privacy-history prefix and no other fixture mode");
            }
            scenario.enablePrivacyHistoryReconnectScenario();
            if (privacyHistoryReconnectMode == PrivacyHistoryReconnectMode.RECONNECT_AND_NEW_GAME) {
                scenario.enablePrivacyHistoryNextGameScenario();
            }
        }
        if (metamorphoseAccept != null) {
            scenario.beginMetamorphoseScenario(metamorphoseAccept);
            scenario.enableMetamorphoseOpeningDiagnostic();
        }
        if (lifecycleScenario) {
            scenario.holdMetamorphoseOptionalForLifecycle();
            scenario.enableMetamorphoseOpeningDiagnostic();
            scenario.configureStarterSeatZeroScenario();
        }
        if (staleActionWitness) scenario.enableStaleActionWitness();
        if (staleQuitWitness) scenario.enableStaleQuitWitness();
        if (metamorphoseAccept != null) scenario.configureStarterSeatZeroScenario();
        if (hostStarterRevealScenario) scenario.configureStarterSeatZeroScenario();
        if (reconnectScenario || disconnectAiScenario || scenario.privacyHistoryReconnectScenario()) {
            scenario.configureStarterSeatZeroScenario();
            if (reconnectScenario) scenario.configureReconnectScenario();
            if (disconnectAiScenario) scenario.configureDisconnectAiScenario();
        }
        scenario.expectPostKeepCallback(deltaSync ? "applyDelta" : "setGameView");
        final String hostName = "DanDan scripted host";
        final String remoteName = "DanDan scripted remote";
        DanDanDesktop desktop = null;
        ScriptedHumanTcpSupport remote = null;
        ScriptedHumanTcpSupport parkedRemote = null;
        ScriptedHumanTcpSupport.ReconnectObservation reconnectObservation = null;
        ScriptedHumanTcpSupport.ReconnectObservation replacementReconnectObservation = null;
        AtomicReference<ScriptedHumanTcpSupportTest.CompletedServerInactiveReceipt> completedHistoryReconnectServerInactive =
                new AtomicReference<>();
        AtomicReference<ScriptedHumanTcpSupportTest.ServerInactiveObservation> pendingHistoryReconnectServerInactive =
                new AtomicReference<>();
        AtomicReference<Channel> pendingHistoryReconnectServerChannel = new AtomicReference<>();
        AtomicReference<RemoteClient> retainedRemoteClient = new AtomicReference<>();
        AtomicReference<ScriptedHumanTcpSupportTest.ServerInactiveObservation> disconnectServerInactive =
                new AtomicReference<>();
        AtomicReference<ScriptedHumanTcpSupportTest.CompletedServerInactiveReceipt> completedDisconnectServerInactive =
                new AtomicReference<>();
        AtomicReference<Integer> reconnectSendErrorBaseline = new AtomicReference<>();
        final CountDownLatch reconnectParkNotice = new CountDownLatch(1);
        final AtomicReference<String> reconnectParkNoticeText = new AtomicReference<>();
        int serverPort = -1;
        boolean serverStarted = false;
        boolean hostLobbyListenerInstalled = false;
        HostedMatch match = null;
        PrivacyTerminalReceipt privacyTerminalReceipt = null;
        AtomicReference<LifecycleCleanupReceipt> lifecycleCleanupReceipt = new AtomicReference<>();
        AtomicReference<Game> capturedLifecycleNPlusOne = new AtomicReference<>();
        AtomicBoolean queuedNextGameMayHaveLaunched = new AtomicBoolean();
        Throwable primaryFailure = null;
        ScriptedHumanTcpSupport.StaleActionRejectionLog staleActionRejectionLog = null;
        boolean staleActionNetworkLogActivated = false;
        try {
            // Prevent the headless start path from consuming the strict deck-shuffle RNG for music selection.
            FModel.getPreferences().setPref(FPref.UI_ENABLE_MUSIC, "false");
            if (tcpOwnManaUndoMode == TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO) {
                FModel.getPreferences().setPref(FPref.UI_UNDO_MODE, forge.localinstance.properties.ForgeConstants.UNDO_MODE_SINGLE);
            }
            if (metamorphoseAccept != null || privacyScenario || lifecycleScenario) {
                FModel.getPreferences().setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, "false");
            }
            HeadlessGuiDesktop.clearLastMatch();
            guiPlayer.setName(hostName);
            FModel.getPreferences().setPref(FPref.PLAYER_NAME, hostName);
            desktop = new DanDanDesktop(hostName, remoteName, scenario, mulliganScenario,
                    metamorphoseAccept != null || lifecycleScenario, privacyScenario);
            GuiBase.setInterface(desktop);
            if (staleActionWitness || staleQuitWitness) {
                Assert.assertNull(NetworkLogConfig.getInstanceSuffix(),
                        "isolated stale-action fixture starts without an existing network-log routing context");
                staleActionNetworkLogActivated = true;
                staleActionRejectionLog = ScriptedHumanTcpSupport.StaleActionRejectionLog.start(
                        "dandan-stale-action-" + (deltaSync ? "delta-" : "full-")
                                + Long.toUnsignedString(System.nanoTime()));
            }
            FModel.getPreferences().setPref(FPref.ENFORCE_DECK_LEGALITY, "false");
            if (mulliganScenario || metamorphoseAccept != null || privacyScenario || lifecycleScenario) {
                FModel.getPreferences().setPref(FPref.UI_RANDOM_FOIL, "false");
                FModel.getPreferences().setPref(FPref.UI_SELECT_FROM_CARD_DISPLAYS, "false");
            }
            RemoteClientGuiGame.useDeltaSync = deltaSync;
            MyRandom.setRandom(new Random(9317L + (deltaSync ? 1 : 0)));
            if (mulliganScenario) {
                if (hostStarterRevealScenario) scriptMulliganWithHostStarter(desktop.hostGui);
                else scriptMulligan(desktop.hostGui);
            } else if (reconnectScenario || disconnectAiScenario || metamorphoseAccept != null || privacyScenario || lifecycleScenario
                    || tcpOwnManaUndoMode == TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO) scriptKeepWithHostStarter(desktop.hostGui);
            else scriptKeepWithoutLand(desktop.hostGui);
            if (metamorphoseAccept != null || lifecycleScenario) desktop.hostGui.script(ScriptedHumanTcpSupport.Decision.CAST_METAMORPHOSE, null);

            serverPort = PortAllocator.allocatePort();
            final int port = serverPort;
            server.setLobbyListener(reconnectScenario || disconnectAiScenario || scenario.privacyHistoryReconnectScenario()
                    ? headlessHostLobbyListener(message -> {
                        if (message != null && message.contains("/skipreconnect")) {
                            reconnectParkNoticeText.set(message);
                            reconnectParkNotice.countDown();
                        }
                    }) : headlessHostLobbyListener());
            hostLobbyListenerInstalled = true;
            serverStarted = true;
            server.startServer(port);
            final ServerGameLobby lobby = new ServerGameLobby();
            server.setLobby(lobby);
            lobby.setListener(new IUpdateable() {
                @Override public void update(boolean fullUpdate) { server.updateLobbyState(); }
                @Override public void update(int slot, LobbySlotType type) { }
            });
            lobby.getSlot(0).setName(hostName);
            lobby.getSlot(0).setDeck(officialDanDanDeck());
            lobby.applyVariant(GameType.DanDan);
            Assert.assertTrue(lobby.hasVariant(GameType.DanDan));
            Assert.assertEquals(lobby.getSlot(0).getDeck().getName(), "DanDan_SecretLair");
            Assert.assertEquals(lobby.getSlot(1).getDeck().getName(), "DanDan_SecretLair");
            lobby.getSlot(0).setIsReady(true);
            lobby.getSlot(1).setName(remoteName);
            lobby.getSlot(1).setIsReady(false);

            remote = new ScriptedHumanTcpSupport(remoteName, 1, remoteName, port, scenario);
            if (mulliganScenario) {
                if (hostStarterRevealScenario) scriptMulliganWithHostStarter(remote.gui());
                else scriptMulligan(remote.gui());
            } else if (reconnectScenario || disconnectAiScenario || metamorphoseAccept != null || privacyScenario || lifecycleScenario
                    || tcpOwnManaUndoMode == TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO) scriptKeepWithHostStarter(remote.gui());
            else scriptKeepWithoutLand(remote.gui());
            remote.connect();
            Assert.assertTrue(remote.awaitAssigned(15, TimeUnit.SECONDS), "TCP login assigned remote DanDan seat");
            assertNoFailure(scenario);
            RemoteClient acceptedRemoteClient = server.getClientBySlotIndex(1);
            Assert.assertNotNull(acceptedRemoteClient, "server retains the remote client for final send-error inspection");
            retainedRemoteClient.set(acceptedRemoteClient);
            Assert.assertEquals(remote.assignedSlot(), 1);
            Assert.assertEquals(lobby.getSlot(1).getType(), LobbySlotType.REMOTE);
            remote.ready();
            Assert.assertTrue(remote.awaitReadyAcknowledged(15, TimeUnit.SECONDS), "remote received authoritative ready state");
            Assert.assertTrue(lobby.getSlot(1).isReady());
            if (privacyScenario) {
                reconnectObservation = new ScriptedHumanTcpSupport.ReconnectObservation();
                scenario.attachPrivacyObservation(reconnectObservation);
                remote.installReconnectObservation(reconnectObservation,
                        System.nanoTime() + TimeUnit.SECONDS.toNanos(5));
            }

            final Runnable start = lobby.startGame();
            Assert.assertNotNull(start, "both seats are ready with the official DanDan preset");
            start.run();
            match = desktop.match;
            if (tcpOwnManaUndoMode == TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO) {
                Assert.assertTrue(scenario.awaitPositiveRemoteOwnManaUndoSetup(60, TimeUnit.SECONDS),
                        "the asynchronous opening setup hook completed before the remote land action was armed");
                assertNoFailure(scenario);
                TcpOwnManaUndoSetup setup = desktop.tcpOwnManaUndoSetup;
                Assert.assertNotNull(setup, "the bounded setup receipt retained the actual owned Island acquisition");
                remote.gui().script(ScriptedHumanTcpSupport.Decision.PLAY_LAND, setup.islandCardId());
            }
            Assert.assertNotNull(match, "ordinary hosted match path created the DanDan match");
            Assert.assertEquals(match.getMatch().getRules().getGameType(), GameType.Constructed);
            Assert.assertEquals(match.getMatch().getPlayers().get(0).getPlayer().getName(), hostName);
            Assert.assertEquals(match.getMatch().getPlayers().get(1).getPlayer().getName(), remoteName);
            if (mulliganScenario || metamorphoseAccept != null || lifecycleScenario) {
                Assert.assertSame(match.getMatch().getPlayers().get(0), desktop.shuffledRegisteredPlayer,
                        "the deterministic transcript was derived from the same RegisteredPlayer Match uses");
                Assert.assertSame(match.getMatch().getPlayers().get(0).getDeck().get(DeckSection.Main), desktop.shuffleInputMain,
                        "Match enumerates the exact Main CardPool used to derive the transcript");
            }

            Assert.assertTrue(desktop.hostGui.awaitFull(30, TimeUnit.SECONDS), "host received initial GameView");
            Assert.assertTrue(desktop.hostGui.awaitOpenView(30, TimeUnit.SECONDS), "host received its assigned PlayerView");
            Assert.assertTrue(remote.gui().awaitFull(30, TimeUnit.SECONDS), "remote received initial GameView");
            Assert.assertTrue(remote.gui().awaitOpenView(30, TimeUnit.SECONDS), "remote received its assigned PlayerView");
            if (privacyScenario) {
                scenario.bindPrivacyViews(desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId());
                scenario.bindPrivacyHostGui(desktop.hostGui);
            }
            if (metamorphoseAccept != null || lifecycleScenario) {
                scenario.bindMetamorphoseViews(desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId());
            }
            assertNoFailure(scenario);
            boolean postKeepReady = scenario.awaitPostKeepState(30, TimeUnit.SECONDS);
            String postKeepTimeoutEvidence = postKeepReady ? "" : "; " + scenario.postKeepTimeoutEvidence()
                    + "; host=" + desktop.hostGui.openingKeepTimeoutEvidence()
                    + "; remote=" + remote.gui().openingKeepTimeoutEvidence();
            assertNoFailure(scenario);
            Assert.assertTrue(postKeepReady,
                    "both Keeps advanced into a real normal-priority prompt and a post-Keep remote callback was retained"
                            + postKeepTimeoutEvidence);
            if (mulliganScenario || metamorphoseAccept != null || lifecycleScenario) {
                Assert.assertNotNull(desktop.controlledShuffle, "the actual RegisteredPlayer deck supplied the controlled shuffle");
                Assert.assertTrue(desktop.controlledShuffle.isComplete(),
                        "the exact 80-card shuffle transcript was consumed before Keeps");
            }
            if (lifecycleScenario) {
                assertMetamorphoseLifecycle(deltaSync, scenario, desktop, remote, match, server,
                        staleActionRejectionLog, lifecycleCleanupReceipt, capturedLifecycleNPlusOne,
                        localDecisionQueueMode, queuedNextGameMayHaveLaunched);
                return;
            }
            if (metamorphoseAccept != null) {
                assertMetamorphoseTcpCase(deltaSync, metamorphoseAccept, scenario, desktop, remote, match, server);
                return;
            }
            if (privacyScenario) {
                privacyTerminalReceipt = assertTellingTimeFengrafPrivacyCase(deltaSync, scenario, desktop,
                        remote, match, reconnectObservation, privacyShuffleHistoryScenario);
                if (scenario.privacyHistoryReconnectScenario()) {
                    int inboundBytesAtOpening = privacyTerminalReceipt.inboundBytesAtOpening();
                    parkedRemote = remote;
                    remote = new ScriptedHumanTcpSupport(remoteName, 1, remoteName, serverPort, scenario);
                    replacementReconnectObservation = new ScriptedHumanTcpSupport.ReconnectObservation();
                    privacyTerminalReceipt = runPrivacyHistoryReconnectRoundTrip(
                            server, retainedRemoteClient.get(), parkedRemote, remote,
                            desktop.hostGui, scenario, match, desktop.privacySetup,
                            reconnectObservation, inboundBytesAtOpening,
                            reconnectParkNotice, reconnectParkNoticeText, replacementReconnectObservation,
                            pendingHistoryReconnectServerInactive, pendingHistoryReconnectServerChannel,
                            completedHistoryReconnectServerInactive);
                    if (privacyHistoryReconnectMode == PrivacyHistoryReconnectMode.RECONNECT_AND_NEW_GAME) {
                        assertPrivacyHistoryNextGame(deltaSync, scenario, desktop, remote, parkedRemote, match,
                                retainedRemoteClient.get(), server, reconnectObservation,
                                privacyTerminalReceipt, privacyShuffleHistoryScenario);
                    }
                }
                return;
            }
            ScriptedHumanTcpSupport.PostKeepReceipt postKeep = scenario.postKeepReceipt();
            Assert.assertNotNull(postKeep, "the post-Keep state receipt is owned by an actual remote callback");
            Assert.assertEquals(postKeep.playerViewIdsBySeat().get(0), desktop.hostGui.ownPlayerViewId());
            Assert.assertEquals(postKeep.playerViewIdsBySeat().get(1), remote.gui().ownPlayerViewId());
            Assert.assertTrue(postKeep.prioritySeat() == 0 || postKeep.prioritySeat() == 1);
            Assert.assertTrue(postKeep.priorityCallbackOrdinal() > 0,
                    "the fixture held at an actual first normal-priority callback");
            ScriptedHumanTcpSupport.ScriptedGui priorityGui = postKeep.prioritySeat() == 0
                    ? desktop.hostGui : remote.gui();
            Assert.assertTrue(priorityGui.captures().stream().anyMatch(c ->
                            "updateButtons".equals(c.callback())
                                    && c.callbackOrdinal() == postKeep.priorityCallbackOrdinal()),
                    "held priority receipt corresponds to a real updateButtons callback");
            ScriptedHumanTcpSupport.Capture stateCapture = postKeep.stateCapture();
            Assert.assertTrue(remote.gui().captures().stream().anyMatch(c ->
                            c.callbackOrdinal() == stateCapture.callbackOrdinal()
                                    && c.sequence() == stateCapture.sequence()),
                    "post-Keep state receipt matches a retained remote callback capture");
            if (deltaSync) {
                Assert.assertEquals(stateCapture.callback(), "applyDelta");
                Assert.assertTrue(stateCapture.stateBearing(), "delta receipt came from a populated packet");
                Assert.assertTrue(stateCapture.sequence() >= 0, "delta receipt retains protocol sequence provenance");
                assertDanDanZonesForCase(postKeep.appliedZonesByPlayer(), desktop.hostGui.ownPlayerViewId(),
                        remote.gui().ownPlayerViewId(), scenario, mulliganScenario);
                assertInitialSerializedDanDanIdentity(remote.gui().captures());
            } else {
                Assert.assertEquals(stateCapture.callback(), "setGameView");
                Assert.assertTrue(stateCapture.callbackOrdinal() > 0, "full state has a receive callback ordinal");
                GameView postKeepView = deserializeGameView(stateCapture.serializedArguments());
                if (mulliganScenario) {
                    assertDanDanIdentity(postKeepView, desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId());
                    assertDanDanZonesForCase(zoneSnapshot(postKeepView), desktop.hostGui.ownPlayerViewId(),
                            remote.gui().ownPlayerViewId(), scenario, true);
                } else {
                    assertDanDanView(postKeepView, desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId());
                }
                Assert.assertEquals(postKeep.appliedZonesByPlayer(), stateCapture.zonesByPlayer(),
                        "the applied full callback agrees with the captured serialized GameView zones");
                assertDanDanZonesForCase(stateCapture.zonesByPlayer(), desktop.hostGui.ownPlayerViewId(),
                        remote.gui().ownPlayerViewId(), scenario, mulliganScenario);
                Assert.assertEquals(remote.gui().populatedDeltaCount(), 0,
                        "full mode did not deliver state-bearing delta callbacks");
            }

            final Map<Integer, Map<ZoneType, List<Integer>>> postKeepZones = deltaSync
                    ? postKeep.appliedZonesByPlayer() : stateCapture.zonesByPlayer();
            assertDanDanZonesForCase(postKeepZones, desktop.hostGui.ownPlayerViewId(),
                    remote.gui().ownPlayerViewId(), scenario, mulliganScenario);
            Assert.assertEquals(postKeep.heldPriorityZonesByPlayer(), postKeepZones,
                    "the detached remote callback zones match the actual held first-priority boundary");
            assertDanDanZonesForCase(postKeep.heldPriorityZonesByPlayer(), desktop.hostGui.ownPlayerViewId(),
                    remote.gui().ownPlayerViewId(), scenario, mulliganScenario);
            final int hostViewId = desktop.hostGui.ownPlayerViewId();
            final int remoteViewId = remote.gui().ownPlayerViewId();
            List<ScriptedHumanTcpSupport.ActionReceipt> keepChoices = scenario.actions().stream()
                    .filter(a -> a.decision() == ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND).toList();
            Assert.assertEquals(keepChoices.size(), 2,
                    "both seats explicitly chose Keep, whether eligible for the free-choice dialog or not");
            Assert.assertTrue(keepChoices.stream().anyMatch(a -> a.seat() == 0
                            && a.chooserViewId() == hostViewId),
                    "host Keep choice is bound to its actual assigned PlayerView");
            Assert.assertTrue(keepChoices.stream().anyMatch(a -> a.seat() == 1
                            && a.chooserViewId() == remoteViewId),
                    "remote Keep choice is bound to its actual assigned PlayerView");
            if (mulliganScenario) {
                Map<Integer, Map<ZoneType, List<Integer>>> mulliganZones = postKeepZones;
                if (scenario.privacyMulliganPrefixScenario()) {
                    ScriptedHumanTcpSupport.PrivacyMulliganBoundaryReceipt boundary =
                            scenario.privacyMulliganBoundaryReceipt();
                    Assert.assertNotNull(boundary, "the same Game retained both sides of declared setup");
                    Assert.assertSame(boundary.game(), scenario.privacyGame());
                    Assert.assertTrue(scenario.mulliganChoices().stream().allMatch(choice ->
                            choice.gameViewId() == boundary.gameViewId()));
                    Assert.assertTrue(scenario.londonBottomDispatches().stream().allMatch(bottom ->
                            bottom.gameViewId() == boundary.gameViewId()));
                    mulliganZones = boundary.preSetupZones();
                    assertDanDanMulliganZones(mulliganZones, hostViewId, remoteViewId, scenario);
                }
                assertMulliganReceipts(scenario, mulliganZones, hostViewId, remoteViewId);
                if (hostStarterRevealScenario) {
                    assertRemoteFreeReveal(scenario, remote.gui(), postKeep, hostViewId, remoteViewId,
                            hostName, remoteName);
                }
            }
            ScriptedHumanTcpSupport.Decision starterDecision = hostStarterRevealScenario || reconnectScenario || disconnectAiScenario
                    || tcpOwnManaUndoMode == TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO
                    ? ScriptedHumanTcpSupport.Decision.START_SEAT_ZERO
                    : ScriptedHumanTcpSupport.Decision.PLAY_OR_DRAW;
            Assert.assertEquals(scenario.actions().stream().filter(a -> a.decision() == starterDecision).count(), 1);
            if (hostStarterRevealScenario || reconnectScenario || disconnectAiScenario
                    || tcpOwnManaUndoMode == TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO) {
                Assert.assertEquals(scenario.actions().stream().filter(a -> a.decision()
                        == ScriptedHumanTcpSupport.Decision.PLAY_OR_DRAW).count(), 0);
            }
            Assert.assertEquals(server.getTotalSendErrors(), 0, "no TCP send errors before terminal cleanup");
            if (tcpOwnManaUndoMode == TcpOwnManaUndoMode.POSITIVE_REMOTE_OWN_UNDO) {
                assertPositiveRemoteOwnManaUndoOverTcp(scenario, desktop, remote, match, server,
                        retainedRemoteClient.get(), desktop.tcpOwnManaUndoSetup, priorityGui);
                return;
            }
            if (disconnectAiScenario) {
                assertDisconnectAiContinuation(deltaSync, scenario, desktop, remote, match, server,
                        retainedRemoteClient.get(), reconnectParkNotice, reconnectParkNoticeText,
                        disconnectServerInactive, completedDisconnectServerInactive);
                return;
            }
            if (reconnectScenario) {
                parkedRemote = remote;
                remote = new ScriptedHumanTcpSupport(remoteName, 1, remoteName, serverPort, scenario);
                reconnectObservation = new ScriptedHumanTcpSupport.ReconnectObservation();
                remote.gui().expectReconnectReplay();
                runReconnectRoundTrip(deltaSync, server, retainedRemoteClient.get(), parkedRemote, remote,
                        desktop.hostGui, scenario, postKeep, hostViewId, remoteViewId,
                        reconnectParkNotice, reconnectParkNoticeText, reconnectObservation,
                        reconnectSendErrorBaseline);
            }
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            if (Boolean.getBoolean(QUEUED_CHILD_PROPERTY)
                    && localDecisionQueueMode == LocalDecisionQueueMode.QUEUED_OLD_CONTROLLER_QUIT
                    && queuedNextGameMayHaveLaunched.get()) {
                LifecycleCleanupReceipt receipt = lifecycleCleanupReceipt.get();
                Game expectedGame = capturedLifecycleNPlusOne.get();
                boolean cleanupProven = false;
                try {
                    cleanupProven = receipt != null && expectedGame != null && receipt.proves(expectedGame);
                } catch (Throwable proofFailure) {
                    abortQueuedChildBeforeUnsafeCleanup("outer-cleanup-proof-failed", primaryFailure, proofFailure);
                }
                if (!cleanupProven) {
                    abortQueuedChildBeforeUnsafeCleanup("outer-cleanup-without-engine-return", primaryFailure);
                }
            }
            scenario.releasePrivacyHostChoice();
            scenario.stopActions();
            Throwable cleanupFailure = null;
            try {
                final boolean runTerminalTimerOracle = primaryFailure == null
                        && privacyHistoryReconnectMode != PrivacyHistoryReconnectMode.RECONNECT_AND_NEW_GAME;
                final ScriptedHumanTcpSupport remoteForCleanup = remote;
                final ScriptedHumanTcpSupport.ScriptedGui hostGuiForCleanup = desktop == null ? null : desktop.hostGui;
                final DanDanDesktop desktopForCleanup = desktop;
                final PrivacyTerminalReceipt privacyTerminalForCleanup = privacyTerminalReceipt;
                final ScriptedHumanTcpSupport.ReconnectObservation reconnectObservationForCleanup = reconnectObservation;
                HostedMatch cleanupMatch = match != null ? match : desktop == null ? null : desktop.match;
                if (primaryFailure != null && scenario.lifecycleScenario() && cleanupMatch != null
                        && desktopForCleanup != null && remoteForCleanup != null) {
                    try {
                        LifecycleCleanupReceipt receipt = lifecycleCleanupReceipt.get();
                        if (receipt == null || !receipt.proves(capturedLifecycleNPlusOne.get())) {
                            cleanupLifecycleNextGameIfActive(cleanupMatch, scenario,
                                    desktopForCleanup, remoteForCleanup);
                        }
                    } catch (Throwable failure) {
                        cleanupFailure = combine(cleanupFailure, failure);
                    }
                }
                ScriptedHumanTcpSupportTest.CompletedServerInactiveReceipt historyInactiveForCleanup =
                        completedHistoryReconnectServerInactive.get();
                ScriptedHumanTcpSupportTest.ServerInactiveObservation cleanupInactiveObservation =
                        disconnectServerInactive.get();
                ScriptedHumanTcpSupportTest.CompletedServerInactiveReceipt cleanupInactiveReceipt =
                        completedDisconnectServerInactive.get();
                if (historyInactiveForCleanup != null && retainedRemoteClient.get() != null
                        && ScriptedHumanTcpSupport.serverChannel(retainedRemoteClient.get())
                        == historyInactiveForCleanup.channel()) {
                    cleanupInactiveObservation = null;
                    cleanupInactiveReceipt = historyInactiveForCleanup;
                } else {
                    ScriptedHumanTcpSupportTest.ServerInactiveObservation historyPendingForCleanup =
                            pendingHistoryReconnectServerInactive.get();
                    if (historyPendingForCleanup != null && retainedRemoteClient.get() != null
                            && pendingHistoryReconnectServerChannel.get() != null
                            && ScriptedHumanTcpSupport.serverChannel(retainedRemoteClient.get())
                            == pendingHistoryReconnectServerChannel.get()) {
                        cleanupInactiveObservation = historyPendingForCleanup;
                        cleanupInactiveReceipt = null;
                    }
                }
                cleanupFailure = combine(cleanupFailure, ScriptedHumanTcpSupportTest.cleanup(cleanupMatch, remote,
                        hostGuiForCleanup, server, serverStarted, retainedRemoteClient.get(), liveMatch -> {
                            RemoteClient client = retainedRemoteClient.get();
                            boolean completedLifecycle = scenario.lifecycleScenario() && liveMatch.getGame() == null
                                    && hostGuiForCleanup != null && remoteForCleanup != null
                                    && countAfterGameEnd(hostGuiForCleanup.captures()) == 2
                                    && countAfterGameEnd(remoteForCleanup.gui().captures()) == 2;
                            boolean completedPrivacyHistoryNextGame = scenario.privacyHistoryNextGamePhase()
                                    && liveMatch.getGame() == null
                                    && scenario.privacyHistoryNextGameTerminals().size() == 2;
                            if (completedPrivacyHistoryNextGame) return;
                            if (scenario.privacyHistoryNextGamePhase()) {
                                // Game N reconnect/outcome assertions completed; its terminal cleanup
                                // receipt must not be applied to the independent Game N+1.
                                liveMatch.endCurrentGame();
                                return;
                            }
                            if (runTerminalTimerOracle && !completedLifecycle && remoteForCleanup != null
                                    && hostGuiForCleanup != null && client != null
                                    && hostGuiForCleanup.awaitActualTerminal(0, TimeUnit.MILLISECONDS)
                                    && remoteForCleanup.gui().awaitActualTerminal(0, TimeUnit.MILLISECONDS)) {
                                endCurrentGameAfterForwardedAwaitPrompt(liveMatch, client, remoteForCleanup.gui());
                                if (privacyTerminalForCleanup != null) {
                                    assertTellingTimeFengrafPrivacyTerminalCase(deltaSync, scenario,
                                            desktopForCleanup, remoteForCleanup, liveMatch, server,
                                            reconnectObservationForCleanup, privacyTerminalForCleanup,
                                            privacyShuffleHistoryScenario);
                                }
                            } else {
                                if (scenario.lifecycleScenario() && liveMatch.getGame() == null) return;
                                liveMatch.endCurrentGame();
                                if (privacyTerminalForCleanup != null) {
                                    throw new AssertionError("privacy terminal receipt could not reach its cleanup-owned assertion fence: "
                                            + "the actual terminal timer prerequisites were unavailable");
                                }
                            }
                        }, cleanupInactiveObservation, cleanupInactiveReceipt));
                ScriptedHumanTcpSupportTest.CompletedServerInactiveReceipt historyInactive =
                        completedHistoryReconnectServerInactive.get();
                if (historyInactive != null && primaryFailure == null) {
                    try {
                        Assert.assertSame(historyInactive.accepted(), retainedRemoteClient.get(),
                                "cleanup reuses the completed inactive receipt bound to the original accepted RemoteClient");
                        Assert.assertFalse(historyInactive.channel().isActive(),
                                "the original channel remains inactive after cleanup of the resumed endpoint");
                    } catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                }
            } catch (Throwable failure) {
                cleanupFailure = combine(cleanupFailure, failure);
            } finally {
                if (scenario.lifecycleScenario()) {
                    try {
                        ScriptedHumanTcpSupport.MetamorphoseHeldOptionalRepaintReceipt heldOptionalRepaint =
                                scenario.lastMetamorphoseHeldOptionalRepaint();
                        Game originalGame = scenario.metamorphoseGame();
                        if (heldOptionalRepaint != null && (originalGame == null
                                || heldOptionalRepaint.gameViewId() != originalGame.getView().getId()
                                || heldOptionalRepaint.seat() != 1
                                || heldOptionalRepaint.chooserViewId() != scenario.metamorphoseRemoteViewId())) {
                            throw new AssertionError(
                                    "held optional repaint receipt escaped the original remote Game/seat");
                        }
                        System.out.println("HELD_OPTIONAL_REPAINT count="
                                + scenario.metamorphoseHeldOptionalRepaintCount()
                                + (heldOptionalRepaint == null ? "" : " lastCallback=" + heldOptionalRepaint.callback()
                                        + " lastOrdinal=" + heldOptionalRepaint.callbackOrdinal()
                                        + " gameViewId=" + heldOptionalRepaint.gameViewId()
                                        + " seat=" + heldOptionalRepaint.seat()));
                    } catch (Throwable failure) {
                        cleanupFailure = combine(cleanupFailure, failure);
                    }
                }
                if (parkedRemote != null) {
                    try { parkedRemote.close(); }
                    catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                }
                if (reconnectObservation != null) {
                    if (primaryFailure == null) {
                        try {
                            Assert.assertFalse(reconnectObservation.overflowed(),
                                    "owned inbound and wrapped-event capture stayed within its hard cap through cleanup");
                            Assert.assertNull(reconnectObservation.failure(),
                                    "pre-handler capture remained healthy through terminal callback drain");
                            Assert.assertEquals(reconnectObservation.rawOwnedByteCount(), reconnectObservation.inboundByteCount(),
                                    "every inbound chunk through cleanup remains owned");
                            List<ScriptedHumanTcpSupport.PreHandlerFact> finalFacts = reconnectObservation.facts();
                            Assert.assertEquals(reconnectObservation.wrappedOwnedEventCount(), finalFacts.stream()
                                            .mapToInt(ScriptedHumanTcpSupport.PreHandlerFact::wrappedEventCount).sum(),
                                    "all opaque event envelopes through cleanup remain owned");
                            Assert.assertEquals(reconnectObservation.wrappedOwnedByteCount(), finalFacts.stream()
                                            .mapToInt(ScriptedHumanTcpSupport.PreHandlerFact::wrappedEventBytes).sum(),
                                    "all owned opaque event bytes through cleanup match their detached facts");
                        } catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                    }
                    try { reconnectObservation.clearOwnedBytes(); }
                    catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                }
                if (replacementReconnectObservation != null) {
                    if (primaryFailure == null) {
                        try {
                            Assert.assertFalse(replacementReconnectObservation.overflowed(),
                                    "replacement reconnect capture stayed within its owned-byte cap through terminal cleanup");
                            Assert.assertNull(replacementReconnectObservation.failure(),
                                    "replacement reconnect pre-handler observation remained healthy through cleanup");
                            Assert.assertTrue(replacementReconnectObservation.inboundByteCount() > 0);
                            Assert.assertEquals(replacementReconnectObservation.rawOwnedByteCount(),
                                    replacementReconnectObservation.inboundByteCount());
                            List<ScriptedHumanTcpSupport.PreHandlerFact> facts = replacementReconnectObservation.facts();
                            Assert.assertEquals(replacementReconnectObservation.wrappedOwnedEventCount(), facts.stream()
                                            .mapToInt(ScriptedHumanTcpSupport.PreHandlerFact::wrappedEventCount).sum());
                            Assert.assertEquals(replacementReconnectObservation.wrappedOwnedByteCount(), facts.stream()
                                            .mapToInt(ScriptedHumanTcpSupport.PreHandlerFact::wrappedEventBytes).sum());
                        } catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                    }
                    try { replacementReconnectObservation.clearOwnedBytes(); }
                    catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                }
                if (hostLobbyListenerInstalled) {
                    try { server.setLobbyListener(null); }
                    catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                }
                if (staleActionNetworkLogActivated) {
                    try {
                        NetworkLogConfig.closeThreadLogger();
                        NetworkLogConfig.setTestMode(false);
                        NetworkLogConfig.closeThreadLogger();
                    } catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                }
            }
            if (primaryFailure == null && retainedRemoteClient.get() != null && reconnectScenario
                    && reconnectSendErrorBaseline.get() != null) {
                try {
                    Assert.assertEquals(retainedRemoteClient.get().getSendErrorCount(),
                            reconnectSendErrorBaseline.get().intValue(),
                            "no host-proxy send errors were added during ordinary terminal cleanup");
                } catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            } else if (primaryFailure == null && retainedRemoteClient.get() != null) {
                try {
                    Assert.assertEquals(retainedRemoteClient.get().getSendErrorCount(), 0,
                            "host proxy produced no TCP send errors through terminal callback drain and disconnect");
                } catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            }
            try { restoreGuiPlayer(guiPlayer, oldGuiPlayer); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { FModel.getPreferences().setPref(FPref.PLAYER_NAME, oldPlayerName); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { RemoteClientGuiGame.useDeltaSync = oldDelta; }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { FModel.getPreferences().setPref(FPref.ENFORCE_DECK_LEGALITY, oldLegality); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { FModel.getPreferences().setPref(FPref.UI_RANDOM_FOIL, oldRandomFoil); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { FModel.getPreferences().setPref(FPref.UI_ENABLE_MUSIC, oldMusic); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { FModel.getPreferences().setPref(FPref.UI_UNDO_MODE, oldUndoMode); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { FModel.getPreferences().setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, oldAutoPassNoActions); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { FModel.getPreferences().setPref(FPref.UI_SELECT_FROM_CARD_DISPLAYS, oldSelectFromCardDisplays); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { MyRandom.setRandom(oldRandom); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { GuiBase.setInterface(oldGui); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            if (cleanupFailure != null) {
                if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure);
                else if (cleanupFailure instanceof Exception e) throw e;
                else if (cleanupFailure instanceof Error e) throw e;
                else throw new RuntimeException(cleanupFailure);
            }
        }
    }

    private static void assertQueuedLocalDecisionConsumer(HostedMatch hosted, Game oldGame,
            forge.game.Match retainedMatch, ScriptedHumanTcpSupport.Scenario scenario,
            DanDanDesktop desktop, ScriptedHumanTcpSupport remote,
            AtomicReference<LifecycleCleanupReceipt> lifecycleCleanupReceipt,
            AtomicReference<Game> capturedLifecycleNPlusOne,
            AtomicBoolean queuedNextGameMayHaveLaunched) throws Throwable {
        // Retain both actual controllers before any terminal or match cleanup can remove a seat.
        List<PlayerControllerHuman> oldControllers = List.copyOf(hosted.getHumanControllers());
        Assert.assertEquals(oldControllers.size(), 2,
                "retain both actual HostedMatch human controllers while Game N is still registered");
        Assert.assertTrue(oldControllers.stream().allMatch(controller -> controller.getGame() == oldGame),
                "both retained human controllers immutably identify Game N");
        if (oldControllers.stream().anyMatch(controller -> controller.getPlayer() != null
                && controller.getPlayer().getOriginalLobbyPlayer() != controller.getLobbyPlayer())) {
            throw new AssertionError(
                    "INCONCLUSIVE: both retained Game N controllers must still be counted by HostedMatch tally");
        }
        scenario.armQueuedLocalDecisionGamePreparation(oldGame);
        CountDownLatch gateEntered = new CountDownLatch(1);
        CountDownLatch releaseEdt = new CountDownLatch(1);
        CountDownLatch observationComplete = new CountDownLatch(1);
        CountDownLatch edtDrain = new CountDownLatch(1);
        AtomicReference<Throwable> edtFailure = new AtomicReference<>();
        AtomicReference<Game> observedNextGame = new AtomicReference<>();
        AtomicReference<List<PlayerControllerHuman>> observedNextControllers = new AtomicReference<>(List.of());
        AtomicReference<LifecycleTrace> nextTrace = new AtomicReference<>();
        AtomicReference<PlayerControllerHuman> nextCleanupController = new AtomicReference<>();
        AtomicReference<PlayerControllerHuman> remoteFenceController = new AtomicReference<>();
        AtomicReference<ScriptedHumanTcpSupport.Scenario.QueuedLocalGamePreparationReceipt> preparationReceipt =
                new AtomicReference<>();
        AtomicReference<Boolean> transitionQueuedBeforeObservation = new AtomicReference<>(false);
        AtomicReference<Boolean> observationQueued = new AtomicReference<>(false);
        AtomicReference<Boolean> staleQuitPostedAfterObservation = new AtomicReference<>(false);
        AtomicReference<Boolean> drainQueued = new AtomicReference<>(false);
        AtomicReference<Integer> matchOverCount = new AtomicReference<>(0);
        boolean[] runPositiveCurrentQuit = {false};
        boolean[] frozenConsumerWitness = {false};
        boolean[] cleanupBoundaryAuthorized = {false};
        Throwable primaryFailure = null;
        Throwable cleanupFailure = null;

        hosted.setOnMatchOver(() -> matchOverCount.updateAndGet(count -> count + 1));
        FThreads.invokeInEdtLater(() -> {
            gateEntered.countDown();
            try {
                if (!releaseEdt.await(25, TimeUnit.SECONDS)) {
                    throw new AssertionError("EDT gate was not released for the queued local decision order");
                }
            } catch (Throwable failure) {
                edtFailure.compareAndSet(null, failure);
            }
        });

        try {
            if (!gateEntered.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("INCONCLUSIVE: EDT did not enter the controlled decision queue gate");
            }
            if (edtFailure.get() != null) throw new AssertionError(
                    "INCONCLUSIVE: controlled EDT gate failed", edtFailure.get());
            if (hosted.getGame() != oldGame) throw new AssertionError(
                    "INCONCLUSIVE: Game N was not active before queuing decisions");

            // These are actual PlayerControllerHuman -> Match.fireEvent -> HostedMatch visitor calls.
            queuedNextGameMayHaveLaunched.set(true);
            oldControllers.get(0).nextGameDecision(NextGameDecision.CONTINUE);
            if (hosted.getGame() != oldGame) throw new AssertionError(
                    "INCONCLUSIVE: first CONTINUE changed Game N before the second decision");
            oldControllers.get(1).nextGameDecision(NextGameDecision.CONTINUE);
            transitionQueuedBeforeObservation.set(true);
            if (hosted.getGame() != oldGame) throw new AssertionError(
                    "INCONCLUSIVE: final CONTINUE did not leave Game N active on the blocked EDT");

            FThreads.invokeInEdtLater(() -> {
                try {
                    Game nextGame = hosted.getGame();
                    if (nextGame == null) throw new AssertionError(
                            "CONTINUE did not install a next Game before observation");
                    observedNextGame.set(nextGame);
                    capturedLifecycleNPlusOne.set(nextGame);
                    if (nextGame == oldGame) throw new AssertionError("CONTINUE reused Game N");
                    LifecycleTrace trace = new LifecycleTrace();
                    trace.attach(nextGame);
                    nextTrace.set(trace);
                    hosted.setEndGameHook(() -> trace.recordEngineReturn(nextGame));
                    List<PlayerControllerHuman> controllers = List.copyOf(hosted.getHumanControllers());
                    observedNextControllers.set(controllers);
                    PlayerControllerHuman remoteController = controllers.stream()
                            .filter(controller -> controller.getGui() instanceof RemoteClientGuiGame)
                            .filter(controller -> controller.getGame() == nextGame)
                            .findFirst().orElse(null);
                    if (remoteController != null) {
                        nextCleanupController.set(remoteController);
                        remoteFenceController.set(remoteController);
                    } else if (!controllers.isEmpty()) nextCleanupController.set(controllers.get(0));
                    else {
                        nextGame.getPlayers().stream().map(Player::getController)
                                .filter(PlayerControllerHuman.class::isInstance)
                                .map(PlayerControllerHuman.class::cast).findFirst()
                                .ifPresent(nextCleanupController::set);
                    }
                    if (controllers.size() != 2 || controllers.stream().anyMatch(controller ->
                            controller.getGame() != nextGame) || controllers.stream().anyMatch(oldControllers::contains)) {
                        throw new AssertionError("N+1 did not synchronously install two fresh controllers bound to N+1");
                    }
                    if (remoteController == null) {
                        throw new AssertionError("N+1 did not install its actual remote-seat host controller");
                    }

                    ScriptedHumanTcpSupport.Scenario.QueuedLocalGamePreparationReceipt receipt =
                            scenario.queuedLocalGamePreparationReceipt();
                    preparationReceipt.set(receipt);
                    if (receipt == null || receipt.oldGame() != oldGame || receipt.game() != nextGame) {
                        throw new AssertionError(
                                "host setGameView did not register the actual N+1 preparation observer");
                    }
                    if (!receipt.awaitLibraryPreparation(15, TimeUnit.SECONDS) || receipt.shuffleCount() == 0) {
                        throw new AssertionError(
                                "N+1 engine did not emit its actual GameEventShuffle before stale QUIT");
                    }
                    Set<Integer> physicalIds = nextGame.getCardsInGame().stream().map(Card::getId)
                            .collect(java.util.stream.Collectors.toUnmodifiableSet());
                    if (physicalIds.size() != 80) throw new AssertionError(
                            "captured N+1 cleanup Game does not contain all 80 physical cards");
                    trace.expectPhysicalCardIds(physicalIds);
                    if (nextCleanupController.get() == null
                            || nextCleanupController.get().getGame() != nextGame) {
                        throw new AssertionError("captured N+1 cleanup controller does not identify N+1");
                    }
                    int gameViewId = nextGame.getView().getId();
                    if (gameViewId == oldGame.getView().getId()) {
                        throw new AssertionError("N+1 did not receive a fresh GameView identity");
                    }
                    if (countAfterGameEnd(desktop.hostGui.captures()) != 1) {
                        throw new AssertionError("N CONTINUE did not emit exactly one host afterGameEnd callback");
                    }
                } catch (Throwable failure) {
                    edtFailure.compareAndSet(null, failure);
                } finally {
                    observationComplete.countDown();
                }
            });
            observationQueued.set(true);
            transitionQueuedBeforeObservation.set(true);

            // Queue the retained Game N controller's actual QUIT after the observation runnable.
            oldControllers.get(0).nextGameDecision(NextGameDecision.QUIT);
            staleQuitPostedAfterObservation.set(true);
            FThreads.invokeInEdtLater(edtDrain::countDown);
            drainQueued.set(true);
        } catch (Throwable failure) {
            primaryFailure = failure;
        } finally {
            // Every wait/failure path releases the EDT before captured-game cleanup begins.
            releaseEdt.countDown();
        }

        try {
            if (observationQueued.get() && !observationComplete.await(25, TimeUnit.SECONDS)) {
                Throwable failure = new AssertionError(
                        "INCONCLUSIVE: intermediate EDT observation did not complete");
                if (primaryFailure == null) primaryFailure = failure;
                else primaryFailure.addSuppressed(failure);
            }
            if (drainQueued.get() && !edtDrain.await(25, TimeUnit.SECONDS)) {
                Throwable failure = new AssertionError("INCONCLUSIVE: queued old-N QUIT did not drain");
                if (primaryFailure == null) primaryFailure = failure;
                else primaryFailure.addSuppressed(failure);
            }
            if (primaryFailure == null && edtFailure.get() != null) {
                primaryFailure = new AssertionError("INCONCLUSIVE: N+1 observation prerequisite failed",
                        edtFailure.get());
            }

            Game nextGame = observedNextGame.get();
            ScriptedHumanTcpSupport.Scenario.QueuedLocalGamePreparationReceipt receipt = preparationReceipt.get();
            LifecycleTrace trace = nextTrace.get();
            PlayerControllerHuman cleanupController = nextCleanupController.get();
            PlayerControllerHuman remoteController = remoteFenceController.get();
            List<PlayerControllerHuman> nextControllers = observedNextControllers.get();
            if (primaryFailure == null && (nextGame == null || cleanupController == null || remoteController == null || trace == null
                    || receipt == null || !transitionQueuedBeforeObservation.get()
                    || !staleQuitPostedAfterObservation.get() || receipt.game() != nextGame
                    || receipt.oldGame() != oldGame || receipt.shuffleCount() == 0)) {
                primaryFailure = new AssertionError(
                        "INCONCLUSIVE: queued-decision origin, order, or N+1 engine-preparation prerequisite missing");
            }
            if (primaryFailure == null) {
                int hostAfterEndBeforeFence = countAfterGameEnd(desktop.hostGui.captures());
                // Require the remote to receive every synchronous host callback before freezing effects.
                try {
                    sendRemoteAfterGameEndFence(remoteController, remote.gui(), scenario,
                            Math.max(1, hostAfterEndBeforeFence));
                } catch (Throwable fenceFailure) {
                    throw new AssertionError("INCONCLUSIVE: remote afterGameEnd receipt fence failed",
                            fenceFailure);
                }
                if (scenario.failure() != null) {
                    throw new AssertionError("INCONCLUSIVE: scripted callback health failed before RED evaluation",
                            scenario.failure());
                }
                int remoteAfterEndCount = countAfterGameEnd(remote.gui().captures());
                int hostAfterEndCount = countAfterGameEnd(desktop.hostGui.captures());
                boolean controllersMatch = hosted.getHumanControllers().size() == 2
                        && hosted.getHumanControllers().containsAll(nextControllers)
                        && nextControllers.stream().allMatch(controller -> controller.getGame() == nextGame);
                QueuedLocalDecisionFacts facts = new QueuedLocalDecisionFacts(oldGame.getView().getId(),
                        nextGame.getView().getId(), oldControllers.stream().allMatch(controller ->
                                controller.getGame() == oldGame), nextGame != oldGame,
                        transitionQueuedBeforeObservation.get(), staleQuitPostedAfterObservation.get(),
                        receipt.oldGame() == oldGame && receipt.game() == nextGame, receipt.shuffleCount(),
                        hosted.getGame() == nextGame, hosted.getHumanControllers().size(), controllersMatch,
                        hostAfterEndCount, remoteAfterEndCount, matchOverCount.get(), nextGame.isGameOver(),
                        trace.outcomeCount());

                // Engine entry and controller registration must be sound before evaluating consumer effects.
                if (!facts.retainedOriginIsOldGame() || !facts.nextGameIsDistinct()
                        || !facts.transitionQueuedBeforeObservation() || !facts.staleQuitPostedAfterObservation()
                        || !facts.preparationReceiptMatched() || facts.preparationShuffleCount() < 1
                        || facts.nextGameOver() || facts.nextGameOutcomeCount() != 0) {
                    primaryFailure = new AssertionError(
                            "INCONCLUSIVE: pre-cleanup lifecycle facts do not isolate the queued QUIT consumer: "
                                    + facts);
                } else {
                    boolean preserved = facts.hostedGameIsNextGame()
                            && facts.registeredNextControllerCount() == 2
                            && facts.registeredNextControllersMatch()
                            && facts.hostAfterGameEndCount() == 1
                            && facts.remoteAfterGameEndCount() == 1
                            && facts.matchOverCount() == 0;
                    if (!preserved) {
                        boolean observedStaleTeardown = !facts.hostedGameIsNextGame()
                                && facts.registeredNextControllerCount() == 0
                                && !facts.registeredNextControllersMatch()
                                && facts.hostAfterGameEndCount() == 2
                                && facts.remoteAfterGameEndCount() == 2
                                && facts.matchOverCount() == 1
                                && !facts.nextGameOver() && facts.nextGameOutcomeCount() == 0;
                        if (observedStaleTeardown) {
                            cleanupBoundaryAuthorized[0] = true;
                            primaryFailure = new AssertionError(
                                    "RED: queued Game N QUIT synchronously tore down the active N+1 match: " + facts);
                        }
                        else primaryFailure = new AssertionError(
                                "INCONCLUSIVE: lifecycle state differs without the complete stale-QUIT teardown witness: "
                                        + facts);
                    } else {
                        cleanupBoundaryAuthorized[0] = true;
                        runPositiveCurrentQuit[0] = true;
                        frozenConsumerWitness[0] = true;
                    }
                }
            }
        } catch (Throwable failure) {
            if (primaryFailure == null) primaryFailure = failure;
            else primaryFailure.addSuppressed(failure);
        } finally {
            // Cleanup is anchored to the captured N+1 Game/controller, even when HostedMatch.game was cleared.
            Game capturedGame = observedNextGame.get();
            PlayerControllerHuman cleanupController = nextCleanupController.get();
            PlayerControllerHuman remoteController = remoteFenceController.get();
            LifecycleTrace trace = nextTrace.get();
            if (capturedGame != null) {
                boolean actualOutcomeAndReturnAlreadyProven = false;
                if (capturedGame.isGameOver() && trace != null && trace.outcomeCount() == 1) {
                    try {
                        actualOutcomeAndReturnAlreadyProven = trace.awaitEngineReturn(0, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (Boolean.getBoolean(QUEUED_CHILD_PROPERTY) && !actualOutcomeAndReturnAlreadyProven) {
                    try {
                        awaitLifecycleCondition(scenario,
                                () -> queuedLocalNextGameCleanupReadiness(capturedGame, scenario,
                                        desktop.hostGui, remote.gui()).ready(),
                                30, "captured N+1 reached its actual held first-priority input before cleanup");
                    } catch (Throwable readinessFailure) {
                        QueuedPriorityReadiness readiness = queuedLocalNextGameCleanupReadiness(
                                capturedGame, scenario, desktop.hostGui, remote.gui());
                        abortQueuedChildBeforeUnsafeCleanup("n-plus-one-held-priority-unproven:" + readiness.scalar(),
                                primaryFailure, readinessFailure);
                    }
                }
                try {
                    if (cleanupController == null) {
                        cleanupController = capturedGame.getRegisteredPlayers().stream()
                                .map(Player::getController)
                                .filter(PlayerControllerHuman.class::isInstance)
                                .map(PlayerControllerHuman.class::cast)
                                .filter(controller -> controller.getGame() == capturedGame)
                                .findFirst().orElse(null);
                    }
                    if (trace == null) {
                        trace = new LifecycleTrace();
                        trace.attach(capturedGame);
                        nextTrace.set(trace);
                        LifecycleTrace cleanupTrace = trace;
                        hosted.setEndGameHook(() -> cleanupTrace.recordEngineReturn(capturedGame));
                    }
                    var preparation = scenario.queuedLocalGamePreparationReceipt();
                    if (preparation != null) {
                        try {
                            if (!preparation.awaitLibraryPreparation(15, TimeUnit.SECONDS)) {
                                throw new AssertionError(
                                        "captured N+1 engine did not reach library preparation during cleanup");
                            }
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            cleanupFailure = combine(cleanupFailure, interrupted);
                        } catch (Throwable failure) {
                            cleanupFailure = combine(cleanupFailure, failure);
                        }
                    }
                    if (trace.expectedPhysicalCardIds == null) {
                        trace.expectPhysicalCardIds(capturedGame.getCardsInGame().stream().map(Card::getId)
                                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
                    }
                    if (cleanupBoundaryAuthorized[0] && scenario.failure() == null) {
                        try {
                            scenario.armQueuedLocalDecisionCleanup(capturedGame);
                        } catch (Throwable failure) {
                            cleanupFailure = combine(cleanupFailure, failure);
                        }
                    } else if (cleanupBoundaryAuthorized[0]) {
                        cleanupFailure = combine(cleanupFailure, new AssertionError(
                                "cleanup-only Keep boundary was not armed because scripted callback health failed",
                                scenario.failure()));
                    }
                    if (cleanupController == null) {
                        throw new AssertionError("captured N+1 Game has no retained actual human controller for cleanup");
                    }
                    if (!capturedGame.isGameOver()) {
                        try { cleanupController.concede(); }
                        catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                    }
                    boolean outcomeObserved = false;
                    boolean engineReturnObserved = false;
                    try {
                        outcomeObserved = trace.awaitOutcome(30, TimeUnit.SECONDS);
                        if (!outcomeObserved) throw new AssertionError(
                                "captured N+1 Game did not emit its actual cleanup outcome");
                    } catch (Throwable failure) {
                        cleanupFailure = combine(cleanupFailure, failure);
                    }
                    try {
                        engineReturnObserved = trace.awaitEngineReturn(30, TimeUnit.SECONDS);
                        if (!engineReturnObserved) throw new AssertionError(
                                "captured N+1 Game did not return from Match.startGame");
                    } catch (Throwable failure) {
                        cleanupFailure = combine(cleanupFailure, failure);
                    }
                    try {
                        ScriptedHumanTcpSupport.QueuedLocalCleanupKeepReceipt keepReceipt =
                                scenario.queuedLocalCleanupKeepReceipt();
                        if (keepReceipt != null && (keepReceipt.gameViewId() != capturedGame.getView().getId()
                                || !keepReceipt.matchesPreparedGame() || !keepReceipt.gameTerminalAtObservation()
                                || keepReceipt.observedHandCount() == 7)) {
                            throw new AssertionError("cleanup-only Keep receipt does not identify terminal captured N+1: "
                                    + keepReceipt);
                        }
                        System.out.println("QUEUED_LOCAL_CLEANUP_KEEP exercised=" + (keepReceipt != null)
                                + (keepReceipt == null ? "" : " gameViewId=" + keepReceipt.gameViewId()
                                        + " callbackOrdinal=" + keepReceipt.callbackOrdinal()
                                        + " seat=" + keepReceipt.seat()
                                        + " observedHandCount=" + keepReceipt.observedHandCount()
                                        + " matchesPreparedGame=" + keepReceipt.matchesPreparedGame()
                                        + " gameTerminalAtObservation=" + keepReceipt.gameTerminalAtObservation()));
                        if (!outcomeObserved || !engineReturnObserved || trace.outcomeCount() != 1
                                || !trace.outcomeCardIdsMatch || !trace.engineReturnCardIdsMatch
                                || trace.outcomeState == null || trace.engineReturnState == null
                                || !trace.outcomeState.conflictingCardIds().isEmpty()
                                || !trace.engineReturnState.conflictingCardIds().isEmpty()) {
                            throw new AssertionError(
                                    "captured N+1 outcome/engine-return snapshots failed physical-card cleanup checks");
                        }
                        lifecycleCleanupReceipt.set(new LifecycleCleanupReceipt(capturedGame,
                                capturedGame.getView().getId(), trace.outcomeCount(), true, true));
                    } catch (Throwable failure) {
                        cleanupFailure = combine(cleanupFailure, failure);
                    }

                    if (runPositiveCurrentQuit[0] && cleanupFailure == null) {
                        try {
                            if (remoteController == null || hosted.getGame() != capturedGame
                                    || cleanupController.getGame() != capturedGame) {
                                throw new AssertionError("positive current N+1 QUIT lost its active Game/controller identity");
                            }
                            cleanupController.nextGameDecision(NextGameDecision.QUIT);
                            CountDownLatch currentQuitDrain = new CountDownLatch(1);
                            FThreads.invokeInEdtLater(currentQuitDrain::countDown);
                            if (!currentQuitDrain.await(20, TimeUnit.SECONDS)) {
                                throw new AssertionError("positive current N+1 QUIT did not drain on EDT");
                            }
                            sendRemoteAfterGameEndFence(remoteController, remote.gui(), scenario, 2);
                            if (scenario.failure() != null) {
                                throw new AssertionError("positive current N+1 QUIT encountered a scripted callback failure",
                                        scenario.failure());
                            }
                            if (hosted.getGame() != null || matchOverCount.get() != 1
                                    || countAfterGameEnd(desktop.hostGui.captures()) != 2
                                    || countAfterGameEnd(remote.gui().captures()) != 2) {
                                throw new AssertionError(
                                        "valid current N+1 QUIT did not perform exactly one ordinary teardown/callback");
                            }
                        } catch (Throwable failure) {
                            cleanupFailure = combine(cleanupFailure, failure);
                        }
                    }
                } catch (Throwable failure) {
                    cleanupFailure = combine(cleanupFailure, failure);
                }
            }
            if (scenario.failure() != null) {
                cleanupFailure = combine(cleanupFailure, new AssertionError(
                        "scripted callback health failed during captured N+1 cleanup", scenario.failure()));
            }
        }

        if (cleanupFailure != null) {
            if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure);
            else primaryFailure = cleanupFailure;
        }
        if (Boolean.getBoolean(QUEUED_CHILD_PROPERTY) && primaryFailure == null
                && frozenConsumerWitness[0] && runPositiveCurrentQuit[0]) {
            LifecycleCleanupReceipt cleanupReceipt = lifecycleCleanupReceipt.get();
            Game capturedGame = capturedLifecycleNPlusOne.get();
            if (cleanupReceipt != null && capturedGame != null && cleanupReceipt.proves(capturedGame)) {
                QUEUED_CHILD_SEMANTIC_RECEIPT.set(new QueuedChildSemanticReceipt(true,
                        cleanupReceipt.outcomeObserved(), cleanupReceipt.engineReturnObserved()));
            }
        }
        if (primaryFailure != null) throw primaryFailure;
        Assert.assertSame(retainedMatch, hosted.getMatch(), "direct local event lifecycle retains the same Match");
    }

    private record QueuedPriorityReadiness(boolean preparationMatches, boolean priorityReceiptPresent,
            boolean priorityGameMatches, int prioritySeat, boolean priorityOrdinalPositive,
            boolean sameGameKeeps, int keepSeatMask, boolean keepHandsSeven,
            boolean keepChoosersMatch, boolean currentTurnMatches, boolean currentControllerMatches,
            boolean proxyIsPassPriority, boolean proxyAndQueueMatch, boolean inputOwnerMatches,
            boolean observationSucceeded, boolean priorityHeld, boolean priorityReleased, boolean matchingPromptCapture,
            boolean gameNotTerminal, boolean scenarioHealthy) {
        boolean ready() {
            return preparationMatches && priorityReceiptPresent && priorityGameMatches
                    && (prioritySeat == 0 || prioritySeat == 1) && priorityOrdinalPositive
                    && sameGameKeeps && keepSeatMask == 3 && keepHandsSeven && keepChoosersMatch
                    && currentTurnMatches && currentControllerMatches && proxyIsPassPriority
                    && proxyAndQueueMatch && inputOwnerMatches && priorityHeld && !priorityReleased
                    && observationSucceeded && matchingPromptCapture && gameNotTerminal && scenarioHealthy;
        }

        String scalar() {
            return "prep=" + preparationMatches + ",priority=" + priorityReceiptPresent
                    + ",priorityGame=" + priorityGameMatches + ",seat=" + prioritySeat
                    + ",ordinal=" + priorityOrdinalPositive + ",keeps=" + sameGameKeeps
                    + ",keepSeats=" + keepSeatMask + ",keep7=" + keepHandsSeven
                    + ",chooser=" + keepChoosersMatch + ",turn=" + currentTurnMatches
                    + ",controller=" + currentControllerMatches + ",proxyPass=" + proxyIsPassPriority
                    + ",proxyQueue=" + proxyAndQueueMatch + ",inputOwner=" + inputOwnerMatches
                    + ",observed=" + observationSucceeded
                    + ",held=" + priorityHeld + ",released=" + priorityReleased
                    + ",promptCapture=" + matchingPromptCapture + ",notTerminal=" + gameNotTerminal
                    + ",healthy=" + scenarioHealthy;
        }
    }

    private static QueuedPriorityReadiness queuedLocalNextGameCleanupReadiness(Game game,
            ScriptedHumanTcpSupport.Scenario scenario, ScriptedHumanTcpSupport.ScriptedGui hostGui,
            ScriptedHumanTcpSupport.ScriptedGui remoteGui) {
        var preparation = scenario.queuedLocalGamePreparationReceipt();
        ScriptedHumanTcpSupport.LifecyclePriorityReceipt priority = scenario.lifecyclePriorityReceipt();
        int gameViewId = game.getView().getId();
        boolean preparationMatches = preparation != null && preparation.game() == game
                && preparation.oldGame() != game;
        boolean priorityReceiptPresent = priority != null;
        boolean priorityGameMatches = priority != null && priority.gameViewId() == gameViewId;
        int seat = priority == null ? -1 : priority.seat();
        boolean priorityOrdinalPositive = priority != null && priority.callbackOrdinal() > 0;

        List<ScriptedHumanTcpSupport.LifecycleKeepReceipt> keeps = scenario.lifecycleKeepReceipts().stream()
                .filter(keep -> keep.gameViewId() == gameViewId).toList();
        int keepSeatMask = 0;
        boolean keepHandsSeven = keeps.size() == 2;
        boolean keepChoosersMatch = keeps.size() == 2;
        for (ScriptedHumanTcpSupport.LifecycleKeepReceipt keep : keeps) {
            if (keep.seat() >= 0 && keep.seat() < game.getPlayers().size()) {
                keepSeatMask |= 1 << keep.seat();
                keepChoosersMatch &= keep.chooserViewId() == game.getPlayers().get(keep.seat()).getView().getId();
            } else keepChoosersMatch = false;
            keepHandsSeven &= keep.callbackOrdinal() > 0 && keep.handIds().size() == 7;
        }
        boolean sameGameKeeps = keeps.size() == 2 && keepSeatMask == 3;

        boolean currentTurnMatches = false;
        boolean currentControllerMatches = false;
        boolean proxyIsPassPriority = false;
        boolean proxyAndQueueMatch = false;
        boolean inputOwnerMatches = false;
        boolean matchingPromptCapture = false;
        boolean observationSucceeded = false;
        if (priority != null && seat >= 0 && seat < game.getPlayers().size()) {
            try {
                Player turnPlayer = game.getPhaseHandler().getPlayerTurn();
                currentTurnMatches = game.getPhaseHandler().getTurn() == priority.turn()
                        && game.getPhaseHandler().getPhase() == priority.phase()
                        && turnPlayer != null && turnPlayer.getView().getId() == priority.turnPlayerViewId();
                Player heldPlayer = game.getPlayers().get(seat);
                if (heldPlayer.getController() instanceof PlayerControllerHuman controller) {
                    currentControllerMatches = controller.getGame() == game;
                    ScriptedHumanTcpSupport.PrivacyInputSnapshot input =
                            scenario.lifecyclePriorityInputSnapshot(game, seat);
                    proxyIsPassPriority = input != null && input.proxyInput() instanceof InputPassPriority;
                    proxyAndQueueMatch = input != null && input.proxyInput() != null
                            && input.proxyInput() == input.queuedInput();
                    inputOwnerMatches = input != null && input.proxyInput() != null
                            && input.proxyInput().getOwner() != null
                            && input.proxyInput().getOwner().getId() == heldPlayer.getView().getId();
                }
                ScriptedHumanTcpSupport.ScriptedGui promptGui = seat == 0 ? hostGui : remoteGui;
                matchingPromptCapture = priority.callbackOrdinal() > 0
                        && promptGui.captures().stream().anyMatch(capture ->
                                capture.callbackOrdinal() == priority.callbackOrdinal()
                                        && "updateButtons".equals(capture.callback()));
                observationSucceeded = true;
            } catch (RuntimeException ignored) {
                // Report the bounded false predicates below; never inspect or print callback payloads.
            }
        }
        return new QueuedPriorityReadiness(preparationMatches, priorityReceiptPresent, priorityGameMatches,
                seat, priorityOrdinalPositive, sameGameKeeps, keepSeatMask, keepHandsSeven,
                keepChoosersMatch, currentTurnMatches, currentControllerMatches, proxyIsPassPriority,
                proxyAndQueueMatch, inputOwnerMatches, observationSucceeded, scenario.postKeepPriorityHeld(),
                scenario.postKeepPriorityReleased(), matchingPromptCapture, !game.isGameOver(),
                scenario.failure() == null);
    }

    private static void sendRemoteAfterGameEndFence(PlayerControllerHuman retainedController,
            ScriptedHumanTcpSupport.ScriptedGui remoteGui, ScriptedHumanTcpSupport.Scenario scenario,
            int minimumAfterGameEndCount) throws Throwable {
        Assert.assertTrue(retainedController.getGui() instanceof RemoteClientGuiGame,
                "retained remote-seat controller exposes its actual host-side protocol GUI");
        List<ScriptedHumanTcpSupport.Capture> before = remoteGui.captures();
        long priorOrdinal = before.stream().mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal)
                .max().orElse(0);
        ((RemoteClientGuiGame) retainedController.getGui()).alertUser();
        awaitLifecycleCondition(scenario, () -> {
            List<ScriptedHumanTcpSupport.Capture> captures = remoteGui.captures();
            int afterEndCount = countAfterGameEnd(captures);
            if (afterEndCount < minimumAfterGameEndCount) return false;
            long lastAfterEnd = afterGameEndOrdinal(captures, afterEndCount);
            return captures.stream().anyMatch(capture -> "alertUser".equals(capture.callback())
                    && capture.callbackOrdinal() > priorOrdinal && capture.callbackOrdinal() > lastAfterEnd);
        }, 15, "remote callback receipt fence followed the required afterGameEnd callback count");
    }

    private static void assertPositiveRemoteOwnManaUndoOverTcp(ScriptedHumanTcpSupport.Scenario scenario,
            DanDanDesktop desktop,
            ScriptedHumanTcpSupport remote, HostedMatch hosted, FServerManager server,
            RemoteClient acceptedRemote, TcpOwnManaUndoSetup setup,
            ScriptedHumanTcpSupport.ScriptedGui heldPriorityGui) throws Throwable {
        Assert.assertNotNull(setup, "the actual pre-opening setup receipt is retained");
        Game game = setup.game();
        Assert.assertNotNull(game);
        Assert.assertSame(hosted.getGame(), game, "the hosted match still owns the captured engine Game");
        Assert.assertEquals(setup.gameViewId(), game.getView().getId());
        Assert.assertEquals(setup.priorOwnerViewId(), game.getRegisteredPlayers().get(0).getView().getId());
        Player host = game.getRegisteredPlayers().get(0);
        Player remotePlayer = game.getRegisteredPlayers().get(1);
        Assert.assertEquals(remotePlayer.getView().getId(), setup.recipientOwnerViewId());
        Assert.assertTrue(remotePlayer.getController() instanceof PlayerControllerHuman,
                "the captured remote seat retains its actual human server controller");
        PlayerControllerHuman serverController = (PlayerControllerHuman) remotePlayer.getController();
        Assert.assertSame(serverController.getGame(), game);
        Assert.assertEquals(host.getCardsIn(ZoneType.Hand).size(), 7,
                "the host retains its ordinary seven-card opening hand");
        GameView initialClientView = remote.gui().getGameView();
        Assert.assertNotNull(initialClientView, "remote applied its actual game view");
        Assert.assertEquals(initialClientView.getId(), setup.gameViewId());
        Object remoteTracker = initialClientView.getTracker();
        Assert.assertNotNull(heldPriorityGui, "the actual captured post-Keep priority endpoint is retained");
        heldPriorityGui.releaseHeldPostKeepPriority();
        Assert.assertTrue(scenario.awaitLand(1, 60, TimeUnit.SECONDS),
                "the remote applied the public Island hand-to-battlefield transition");
        Assert.assertTrue(scenario.awaitLandAbility(1, 60, TimeUnit.SECONDS),
                "the remote's ordinary scripted MAIN1 path selected the legal land play ability");
        ScriptedHumanTcpSupport.CallbackReceipt landMove = scenario.publicMoveReceipt(1);
        Assert.assertNotNull(landMove);
        Assert.assertTrue(landMove.callbackOrdinal() > 0);
        Assert.assertEquals(scenario.actions().stream().filter(action ->
                action.decision() == ScriptedHumanTcpSupport.Decision.PLAY_LAND
                        && action.seat() == 1 && action.chooserViewId() == setup.remoteViewId()
                        && action.cardId() == setup.islandCardId()).count(), 1L,
                "the actual remote chooser dispatched exactly the acquired Island as its land play");
        Assert.assertTrue(scenario.awaitPositiveRemoteOwnManaUndoPriority(90, TimeUnit.SECONDS),
                "the remote reached its real turn-two MAIN1 after playing the acquired Island");
        assertNoFailure(scenario);
        ScriptedHumanTcpSupport.OwnManaUndoPriorityReceipt priority =
                scenario.lastPositiveRemoteOwnManaUndoPriority();
        Assert.assertNotNull(priority, "the held current-priority receipt is typed and retained");
        Assert.assertEquals(priority.gameViewId(), setup.gameViewId());
        Assert.assertEquals(priority.seat(), 1);
        Assert.assertEquals(priority.chooserViewId(), setup.remoteViewId());
        Assert.assertEquals(priority.turn(), 2);
        Assert.assertEquals(priority.phase(), PhaseType.MAIN1);
        Assert.assertEquals(priority.turnPlayerViewId(), setup.remoteViewId());
        Assert.assertTrue(priority.callbackOrdinal() > 0);
        Assert.assertTrue(priority.firstEnabled() || priority.secondEnabled());

        InputPassPriority heldQueuedInput = requireOwnManaPriority(game, remotePlayer, serverController);
        Assert.assertFalse(game.isGameOver(), "the captured game remains active at the held priority");
        Assert.assertEquals(game.getPhaseHandler().getTurn(), 2);
        Assert.assertEquals(game.getPhaseHandler().getPhase(), PhaseType.MAIN1);
        Assert.assertSame(game.getPhaseHandler().getPlayerTurn(), remotePlayer);
        Card serverIsland = remotePlayer.getZone(ZoneType.Battlefield).getCards().stream()
                .filter(card -> card.getId() == setup.islandCardId()).findFirst()
                .orElseThrow(() -> new AssertionError("the actually acquired Island was not played onto the remote battlefield"));
        Assert.assertSame(serverIsland.getOwner(), remotePlayer);
        Assert.assertSame(serverIsland.getController(), remotePlayer);
        Assert.assertFalse(serverIsland.isTapped(), "the land is untapped before its mana ability is activated");
        Assert.assertEquals(remotePlayer.getManaPool().getAmountOfColor(MagicColor.BLUE), 0,
                "the remote has no blue mana before the Island ability");
        Assert.assertFalse(serverController.canUndoLastAction(),
                "the fixture begins the mana slice before any remote action is undoable");
        List<Integer> sharedLibraryAtPriority = host.getZone(ZoneType.Library).getCards().stream()
                .map(Card::getId).toList();
        List<Integer> sharedGraveyardAtPriority = host.getZone(ZoneType.Graveyard).getCards().stream()
                .map(Card::getId).toList();

        GameView heldClientView = remote.gui().getGameView();
        Assert.assertSame(heldClientView.getTracker(), remoteTracker,
                "the remote still uses the same applied tracker for the captured Game");
        Assert.assertEquals(heldClientView.getTurn(), 2);
        Assert.assertEquals(heldClientView.getPhase(), PhaseType.MAIN1);
        PlayerView heldRemoteView = heldClientView.getPlayers().stream()
                .filter(player -> player.getId() == setup.remoteViewId()).findFirst().orElseThrow();
        IGameController heldClientController = remote.gui().getGameController(heldRemoteView);
        Assert.assertTrue(heldClientController instanceof NetGameController,
                "the current remote view is bound to its real TCP game controller");
        CardView heldIslandView = heldRemoteView.getCards(ZoneType.Battlefield).stream()
                .filter(card -> card.getId() == setup.islandCardId()).findFirst()
                .orElseThrow(() -> new AssertionError("the remote's applied view lacks its acquired Island"));
        Assert.assertFalse(heldIslandView.isTapped());
        Assert.assertEquals(heldIslandView.getOwner().getId(), setup.remoteViewId());
        Assert.assertEquals(heldIslandView.getController().getId(), setup.remoteViewId());
        Assert.assertEquals(heldRemoteView.getMana(MagicColor.BLUE), 0);
        List<Integer> appliedLibraryAtPriority = heldClientView.getPlayers().get(0)
                .getCards(ZoneType.Library).stream().map(CardView::getId).toList();
        List<Integer> appliedGraveyardAtPriority = heldClientView.getPlayers().get(0)
                .getCards(ZoneType.Graveyard).stream().map(CardView::getId).toList();
        Assert.assertTrue(appliedLibraryAtPriority.equals(sharedLibraryAtPriority),
                "the applied remote view matches authoritative shared Library order at the held T2 priority");
        Assert.assertTrue(appliedGraveyardAtPriority.equals(sharedGraveyardAtPriority),
                "the applied remote view matches authoritative shared Graveyard order at the held T2 priority");
        Assert.assertNotNull(acceptedRemote, "the assigned remote retains its accepted TCP server endpoint");
        Channel serverChannel = ScriptedHumanTcpSupport.serverChannel(acceptedRemote);
        Assert.assertNotNull(serverChannel, "the accepted remote's current server channel is retained");
        Assert.assertTrue(serverChannel.isActive(), "the accepted remote channel remains active");
        try (ScriptedHumanTcpSupport.ServerDecodedEventTap serverTap =
                ScriptedHumanTcpSupport.ServerDecodedEventTap.install(serverChannel, 1)) {
            ScriptedHumanTcpSupport.GameViewReceipt beforeActivation = latestCurrentGameViewReceipt(remote.gui(), setup.gameViewId());
            long beforeActivationOrdinal = beforeActivation == null ? 0 : beforeActivation.callbackOrdinal();
            beforeActivationOrdinal = Math.max(beforeActivationOrdinal,
                    latestOwnManaUndoAppliedOrdinal(scenario, setup.gameViewId()));
            serverTap.arm(ProtocolMethod.selectCard);
            PlayerView currentRemoteView = remote.gui().getGameView().getPlayers().stream()
                    .filter(player -> player.getId() == setup.remoteViewId()).findFirst().orElseThrow();
            IGameController remoteController = remote.gui().getGameController(currentRemoteView);
            Assert.assertTrue(remoteController instanceof NetGameController,
                    "the command is issued by the assigned real remote TCP controller");
            Assert.assertSame(remoteController, heldClientController,
                    "the assigned TCP controller remains bound across the held current-priority view");
            FThreads.invokeInEdtAndWait(() -> {
                assertOwnManaUndoDispatchStillBound(game, remotePlayer, serverController,
                        heldQueuedInput, remote.gui(), remoteController, setup.gameViewId(), setup.remoteViewId());
                GameView dispatchView = remote.gui().getGameView();
                Assert.assertNotNull(dispatchView);
                Assert.assertSame(dispatchView.getTracker(), remoteTracker,
                        "the selected source comes from the same current applied GameView tracker");
                PlayerView dispatchRemote = dispatchView.getPlayers().stream()
                        .filter(player -> player.getId() == setup.remoteViewId()).findFirst().orElseThrow();
                CardView dispatchIsland = dispatchRemote.getCards(ZoneType.Battlefield).stream()
                        .filter(card -> card.getId() == setup.islandCardId()).findFirst().orElseThrow();
                Assert.assertSame(dispatchIsland.getTracker(), remoteTracker,
                        "the selected Island comes from the current applied GameView tracker");
                Assert.assertSame(remote.gui().getGameController(dispatchRemote), remoteController);
                Assert.assertTrue(remoteController.selectCard(dispatchIsland, null, null),
                        "the remote TCP controller accepted its own Island selection");
            });
            Assert.assertTrue(serverTap.awaitReceipt(15, TimeUnit.SECONDS),
                    "the server decoded the remote selectCard mana-ability request");
            ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt activationReceipt = serverTap.receipt();
            Assert.assertNotNull(activationReceipt);
            Assert.assertEquals(activationReceipt.method(), ProtocolMethod.selectCard);
            Assert.assertEquals(activationReceipt.seat(), 1);
            Assert.assertEquals(activationReceipt.gameViewId(), Integer.valueOf(setup.gameViewId()));
            Assert.assertSame(activationReceipt.channel(), serverChannel);
            Assert.assertNull(serverTap.failure(), "activation server receive observation remained healthy");
            Assert.assertTrue(scenario.awaitPositiveRemoteOwnManaUndoAbility(15, TimeUnit.SECONDS),
                    "the remote GUI returned its exact legal Island mana ability to the server controller");
            assertNoFailure(scenario);
            List<ScriptedHumanTcpSupport.OwnManaUndoAbilityReceipt> abilityReceipts =
                    scenario.positiveRemoteOwnManaUndoAbilityReceipts();
            Assert.assertEquals(abilityReceipts.size(), 2,
                    "one select request produced its ordered initial and additional-cost ability callbacks");
            ScriptedHumanTcpSupport.OwnManaUndoAbilityReceipt initialAbilityReceipt = abilityReceipts.get(0);
            ScriptedHumanTcpSupport.OwnManaUndoAbilityReceipt additionalCostReceipt = abilityReceipts.get(1);
            Assert.assertEquals(initialAbilityReceipt.stage(),
                    ScriptedHumanTcpSupport.OwnManaUndoAbilityStage.INITIAL_SELECTION);
            Assert.assertEquals(additionalCostReceipt.stage(),
                    ScriptedHumanTcpSupport.OwnManaUndoAbilityStage.ADDITIONAL_COST);
            Assert.assertTrue(additionalCostReceipt.callbackOrdinal() > initialAbilityReceipt.callbackOrdinal(),
                    "the additional-cost callback follows the initial Island ability choice");
            for (ScriptedHumanTcpSupport.OwnManaUndoAbilityReceipt abilityReceipt : abilityReceipts) {
                Assert.assertEquals(abilityReceipt.gameViewId(), setup.gameViewId());
                Assert.assertEquals(abilityReceipt.seat(), 1);
                Assert.assertEquals(abilityReceipt.chooserViewId(), setup.remoteViewId());
                Assert.assertEquals(abilityReceipt.cardId(), setup.islandCardId());
                Assert.assertEquals(abilityReceipt.abilityCount(), 1);
                Assert.assertEquals(abilityReceipt.zone(), ZoneType.Battlefield);
                Assert.assertEquals(abilityReceipt.ownerViewId(), setup.remoteViewId());
                Assert.assertEquals(abilityReceipt.controllerViewId(), setup.remoteViewId());
                Assert.assertTrue(abilityReceipt.canPlay());
                Assert.assertFalse(abilityReceipt.spell());
            }
            Assert.assertNull(serverTap.failure(),
                    "no duplicate remote selectCard packet arrived during both ordered ability callbacks");
            serverTap.disarm();
            ScriptedHumanTcpSupport.OwnManaUndoPriorityReceipt afterActivationPriority =
                    awaitOwnManaUndoPriorityAfter(scenario, priority.callbackOrdinal(), 15);
            Assert.assertTrue(afterActivationPriority.callbackOrdinal() > priority.callbackOrdinal());
            awaitOwnIslandState(game, remotePlayer, serverIsland, serverController, true, 1, 15, scenario);
            Assert.assertTrue(serverController.canUndoLastAction(),
                    "the actual remote Island mana ability is now undoable by its owner");
            InputPassPriority activationInput = requireOwnManaPriority(game, remotePlayer, serverController);
            Assert.assertTrue(host.getZone(ZoneType.Library).getCards().stream().map(Card::getId).toList()
                    .equals(sharedLibraryAtPriority),
                    "mana activation leaves the shared Library identity and order unchanged");
            Assert.assertTrue(host.getZone(ZoneType.Graveyard).getCards().stream().map(Card::getId).toList()
                    .equals(sharedGraveyardAtPriority),
                    "mana activation leaves the shared Graveyard identity and order unchanged");
            assertAppliedManaUndoState(remote.gui(), scenario, setup.gameViewId(), beforeActivationOrdinal,
                    remoteTracker, heldClientController, setup.remoteViewId(), setup.islandCardId(),
                    appliedLibraryAtPriority, appliedGraveyardAtPriority, true, 1);

            ScriptedHumanTcpSupport.GameViewReceipt afterActivation = latestCurrentGameViewReceipt(remote.gui(), setup.gameViewId());
            long beforeUndoOrdinal = afterActivation == null ? 0 : afterActivation.callbackOrdinal();
            beforeUndoOrdinal = Math.max(beforeUndoOrdinal,
                    latestOwnManaUndoAppliedOrdinal(scenario, setup.gameViewId()));
            final CountDownLatch undoRestored = new CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicBoolean undoRestoreStarted =
                    new java.util.concurrent.atomic.AtomicBoolean();
            game.subscribeToEvents(new Object() {
                @Subscribe public void onSnapshotRestored(forge.game.event.GameEventSnapshotRestored event) {
                    if (event.start()) undoRestoreStarted.set(true);
                    else if (undoRestoreStarted.compareAndSet(true, false)) undoRestored.countDown();
                }
            });
            serverTap.arm(ProtocolMethod.undoLastAction);
            FThreads.invokeInEdtAndWait(() -> {
                assertOwnManaUndoDispatchStillBound(game, remotePlayer, serverController,
                        activationInput, remote.gui(), remoteController, setup.gameViewId(), setup.remoteViewId());
                Assert.assertSame(requireOwnManaPriority(game, remotePlayer, serverController), activationInput,
                        "the undo request uses the exact current priority input at dispatch");
                remoteController.undoLastAction();
            });
            Assert.assertTrue(serverTap.awaitReceipt(15, TimeUnit.SECONDS),
                    "the same assigned remote TCP controller sent its own undo request");
            ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt undoReceipt = serverTap.receipt();
            Assert.assertNotNull(undoReceipt);
            Assert.assertEquals(undoReceipt.method(), ProtocolMethod.undoLastAction);
            Assert.assertEquals(undoReceipt.seat(), 1);
            Assert.assertEquals(undoReceipt.gameViewId(), Integer.valueOf(setup.gameViewId()));
            Assert.assertSame(undoReceipt.channel(), serverChannel);
            Assert.assertNull(serverTap.failure(), "undo server receive observation remained healthy");
            Assert.assertTrue(undoRestored.await(15, TimeUnit.SECONDS),
                    "the captured Game completed its typed snapshot restoration after the TCP undo request");
            awaitLifecycleCondition(scenario, () -> {
                Input current = serverController.getInputQueue().getInput();
                return current instanceof InputPassPriority && current != activationInput
                        && serverController.getInputProxy().getInput() == current
                        && current.getOwner() == remotePlayer.getView();
            }, 15, "snapshot restoration rebuilt a fresh current remote priority input");
            InputPassPriority restoredInput = requireOwnManaPriority(game, remotePlayer, serverController);
            Assert.assertNotSame(restoredInput, activationInput,
                    "the completed undo replaces the old priority input rather than retaining a stale input");
            awaitOwnIslandState(game, remotePlayer, serverIsland, serverController, false, 0, 15, scenario);
            Assert.assertFalse(serverController.canUndoLastAction(),
                    "the remote-owned undo has restored the action history to its pre-ability state");
            Assert.assertTrue(host.getZone(ZoneType.Library).getCards().stream().map(Card::getId).toList()
                    .equals(sharedLibraryAtPriority),
                    "undo leaves the shared Library identity and order unchanged");
            Assert.assertTrue(host.getZone(ZoneType.Graveyard).getCards().stream().map(Card::getId).toList()
                    .equals(sharedGraveyardAtPriority),
                    "undo leaves the shared Graveyard identity and order unchanged");
            ScriptedHumanTcpSupport.OwnManaUndoPriorityReceipt afterUndoPriority =
                    awaitOwnManaUndoPriorityAfter(scenario, afterActivationPriority.callbackOrdinal(), 15);
            Assert.assertTrue(afterUndoPriority.callbackOrdinal() > afterActivationPriority.callbackOrdinal());
            assertAppliedManaUndoState(remote.gui(), scenario, setup.gameViewId(), beforeUndoOrdinal,
                    remoteTracker, heldClientController, setup.remoteViewId(), setup.islandCardId(),
                    appliedLibraryAtPriority, appliedGraveyardAtPriority, false, 0);
            Assert.assertTrue(scenario.positiveRemoteOwnManaUndoPriorityCallbacks() >= 3,
                    "the same current priority remained held through the activation and undo boundary");
            assertNoFailure(scenario);
            Assert.assertEquals(server.getTotalSendErrors(), 0,
                    "the ordinary TCP controller sent the activation and undo without transport errors");

            GameView terminalRemoteView = remote.gui().getGameView();
            PlayerView terminalRemotePlayer = terminalRemoteView.getPlayers().stream()
                    .filter(player -> player.getId() == setup.remoteViewId()).findFirst().orElseThrow();
            IGameController terminalRemoteController = remote.gui().getGameController(terminalRemotePlayer);
            Assert.assertSame(terminalRemoteController, heldClientController);
            FThreads.invokeInEdtAndWait(() -> {
                assertOwnManaUndoDispatchStillBound(game, remotePlayer, serverController,
                        restoredInput, remote.gui(), terminalRemoteController,
                        setup.gameViewId(), setup.remoteViewId());
                terminalRemoteController.concede();
            });
            Assert.assertTrue(desktop.hostGui.awaitActualTerminal(30, TimeUnit.SECONDS),
                    "the actual remote concession reached the host terminal view");
            Assert.assertTrue(remote.gui().awaitActualTerminal(30, TimeUnit.SECONDS),
                    "the actual remote concession reached the remote terminal view");
            LifecycleTrace trace = desktop.tcpOwnManaUndoTrace;
            Assert.assertNotNull(trace, "the actual Game outcome and HostedMatch return are independently retained");
            Assert.assertTrue(trace.awaitOutcome(30, TimeUnit.SECONDS), "captured Game emitted its actual outcome");
            Assert.assertTrue(trace.awaitEngineReturn(30, TimeUnit.SECONDS),
                    "HostedMatch observed the actual captured Game engine return");
            Assert.assertSame(trace.game, game);
            Assert.assertEquals(trace.outcomeCount(), 1);
            Assert.assertNotNull(trace.outcomeState);
            Assert.assertNotNull(trace.engineReturnState);
            Assert.assertTrue(trace.outcomeCardIdsMatch && trace.engineReturnCardIdsMatch,
                    "outcome and engine return each retain all setup physical-card identities");
            Assert.assertTrue(trace.outcomeState.conflictingCardIds().isEmpty()
                    && trace.engineReturnState.conflictingCardIds().isEmpty());
            Assert.assertEquals(trace.outcomeState.gameViewId(), setup.gameViewId());
            Assert.assertEquals(trace.engineReturnState.gameViewId(), setup.gameViewId());
            Assert.assertEquals(trace.outcomeState.trackerIdentity(), trace.engineReturnState.trackerIdentity());
            assertNoFailure(scenario);
        }
    }

    private static void awaitOwnIslandState(Game game, Player remote, Card island,
            PlayerControllerHuman controller, boolean expectedTapped,
            int expectedBlueMana, long timeout, ScriptedHumanTcpSupport.Scenario scenario) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            assertNoFailure(scenario);
            if (game.getOutcome() != null) throw new AssertionError("captured Game ended before the mana receipt");
            if (island.isTapped() == expectedTapped
                    && remote.getManaPool().getAmountOfColor(MagicColor.BLUE) == expectedBlueMana) break;
            TimeUnit.MILLISECONDS.sleep(10);
        }
        Assert.assertEquals(island.isTapped(), expectedTapped,
                "the captured server Game has the expected exact land tap state");
        Assert.assertEquals(remote.getManaPool().getAmountOfColor(MagicColor.BLUE), expectedBlueMana,
                "the captured server Game has the expected blue-mana state");
        Assert.assertSame(controller.getGame(), game);
        requireOwnManaPriority(game, remote, controller);
        Assert.assertFalse(game.isGameOver());
        Assert.assertEquals(game.getPhaseHandler().getTurn(), 2);
        Assert.assertEquals(game.getPhaseHandler().getPhase(), PhaseType.MAIN1);
        Assert.assertSame(game.getPhaseHandler().getPlayerTurn(), remote);
        Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), remote,
                "the assigned remote seat owns actual server-side priority");
        Assert.assertTrue(game.getStack().isEmpty(), "the mana ability is activated from an empty stack");
    }

    private static InputPassPriority requireOwnManaPriority(Game game, Player remote,
            PlayerControllerHuman controller) {
        Assert.assertSame(controller.getGame(), game);
        Assert.assertFalse(game.isGameOver());
        Assert.assertEquals(game.getPhaseHandler().getTurn(), 2);
        Assert.assertEquals(game.getPhaseHandler().getPhase(), PhaseType.MAIN1);
        Assert.assertSame(game.getPhaseHandler().getPlayerTurn(), remote);
        Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), remote,
                "the current actual priority still belongs to the assigned remote seat");
        Assert.assertTrue(game.getStack().isEmpty(), "the dispatch remains at an empty-stack priority");
        var queuedInput = controller.getInputQueue().getInput();
        var proxyInput = controller.getInputProxy().getInput();
        Assert.assertTrue(queuedInput instanceof InputPassPriority,
                "the current remote controller owns a real priority input after the action boundary");
        Assert.assertSame(proxyInput, queuedInput,
                "the current remote InputProxy and InputQueue expose the same priority input");
        Assert.assertSame(queuedInput.getOwner(), remote.getView(),
                "the current priority input remains owned by the remote seat");
        return (InputPassPriority) queuedInput;
    }

    private static ScriptedHumanTcpSupport.OwnManaUndoPriorityReceipt awaitOwnManaUndoPriorityAfter(
            ScriptedHumanTcpSupport.Scenario scenario, long callbackOrdinal, long timeoutSeconds)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        ScriptedHumanTcpSupport.OwnManaUndoPriorityReceipt receipt;
        do {
            assertNoFailure(scenario);
            receipt = scenario.lastPositiveRemoteOwnManaUndoPriority();
            if (receipt != null && receipt.callbackOrdinal() > callbackOrdinal) return receipt;
            TimeUnit.MILLISECONDS.sleep(10);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("No fresh remote current-priority callback followed the action boundary");
    }

    private static void assertOwnManaUndoDispatchStillBound(Game game, Player remote,
            PlayerControllerHuman serverController, InputPassPriority expectedInput,
            ScriptedHumanTcpSupport.ScriptedGui remoteGui, IGameController remoteController,
            int gameViewId, int remoteViewId) {
        GameView current = remoteGui.getGameView();
        Assert.assertNotNull(current, "the remote's current applied GameView remains available at dispatch");
        Assert.assertEquals(current.getId(), gameViewId);
        Assert.assertEquals(current.getTurn(), 2);
        Assert.assertEquals(current.getPhase(), PhaseType.MAIN1);
        Assert.assertNotNull(current.getPlayerTurn());
        Assert.assertEquals(current.getPlayerTurn().getId(), remoteViewId);
        PlayerView currentRemote = current.getPlayers().stream()
                .filter(player -> player.getId() == remoteViewId).findFirst().orElseThrow();
        Assert.assertSame(remoteGui.getGameController(currentRemote), remoteController,
                "the remote command still uses the controller attached to its current applied PlayerView");
        Assert.assertTrue(remoteController instanceof NetGameController,
                "the command path remains the real remote TCP controller");
        Assert.assertSame(serverController.getGame(), game);
        Assert.assertFalse(game.isGameOver());
        Assert.assertEquals(game.getPhaseHandler().getTurn(), 2);
        Assert.assertEquals(game.getPhaseHandler().getPhase(), PhaseType.MAIN1);
        Assert.assertSame(game.getPhaseHandler().getPlayerTurn(), remote);
        Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), remote,
                "the exact server input owner retains actual phase priority at dispatch");
        Assert.assertTrue(game.getStack().isEmpty(), "the exact server input is an empty-stack priority");
        Assert.assertSame(serverController.getInputQueue().getInput(), expectedInput,
                "the dispatch still targets the retained current server InputPassPriority");
        Assert.assertSame(serverController.getInputProxy().getInput(), expectedInput,
                "server proxy still exposes the same retained current priority input");
        Assert.assertSame(expectedInput.getOwner(), remote.getView(),
                "the current server input remains owned by the assigned remote player");
    }

    private static void assertAppliedManaUndoState(ScriptedHumanTcpSupport.ScriptedGui gui,
            ScriptedHumanTcpSupport.Scenario scenario, int gameViewId, long afterOrdinal, Object tracker,
            IGameController expectedController, int remoteViewId,
            int islandId, List<Integer> sharedLibraryIds, List<Integer> sharedGraveyardIds,
            boolean tapped, int blueMana) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline && !hasAppliedManaUndoStateReceipt(gui, scenario,
                gameViewId, afterOrdinal, tracker, remoteViewId, 2, PhaseType.MAIN1,
                islandId, sharedLibraryIds, sharedGraveyardIds, tapped, blueMana)) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        Assert.assertTrue(hasAppliedManaUndoStateReceipt(gui, scenario, gameViewId, afterOrdinal,
                tracker, remoteViewId, 2, PhaseType.MAIN1, islandId,
                sharedLibraryIds, sharedGraveyardIds, tapped, blueMana),
                "remote applied a fresh same-Game callback after the expected state change");
        GameView current = gui.getGameView();
        Assert.assertNotNull(current);
        Assert.assertEquals(current.getId(), gameViewId);
        Assert.assertSame(current.getTracker(), tracker);
        PlayerView remote = current.getPlayers().stream().filter(player -> player.getId() == remoteViewId)
                .findFirst().orElseThrow();
        Assert.assertSame(gui.getGameController(remote), expectedController,
                "the same assigned TCP controller remains bound to the current remote PlayerView");
        CardView island = remote.getCards(ZoneType.Battlefield).stream()
                .filter(card -> card.getId() == islandId).findFirst()
                .orElseThrow(() -> new AssertionError("applied remote view lost the acquired Island"));
        Assert.assertEquals(island.isTapped(), tapped);
        Assert.assertEquals(island.getOwner().getId(), remoteViewId);
        Assert.assertEquals(island.getController().getId(), remoteViewId);
        Assert.assertSame(island.getTracker(), tracker,
                "the selected applied Island belongs to the current GameView tracker");
        Assert.assertEquals(remote.getMana(MagicColor.BLUE), blueMana);
        Assert.assertEquals(current.getTurn(), 2);
        Assert.assertEquals(current.getPhase(), PhaseType.MAIN1);
        Assert.assertTrue(current.getPlayers().get(0).getCards(ZoneType.Library).stream()
                .map(CardView::getId).toList().equals(sharedLibraryIds),
                "applied shared Library identity and order match the held baseline");
        Assert.assertTrue(current.getPlayers().get(0).getCards(ZoneType.Graveyard).stream()
                .map(CardView::getId).toList().equals(sharedGraveyardIds),
                "applied shared Graveyard identity and order match the held baseline");
    }

    private static boolean hasAppliedManaUndoStateReceipt(ScriptedHumanTcpSupport.ScriptedGui gui,
            ScriptedHumanTcpSupport.Scenario scenario, int gameViewId, long afterOrdinal,
            Object tracker, int remoteViewId, int turn, PhaseType phase,
            int islandId, List<Integer> sharedLibraryIds, List<Integer> sharedGraveyardIds,
            boolean tapped, int blueMana) {
        GameView current = gui.getGameView();
        if (current == null || current.getId() != gameViewId || current.getTurn() != turn
                || current.getPhase() != phase) return false;
        boolean exactAppliedReceipt = scenario.positiveRemoteOwnManaUndoAppliedReceipts().stream().anyMatch(receipt ->
                receipt.callbackOrdinal() > afterOrdinal && receipt.gameViewId() == gameViewId
                        && receipt.trackerIdentity() == tracker && receipt.remoteViewId() == remoteViewId
                        && receipt.islandCardId() == islandId && receipt.islandOnBattlefield()
                        && receipt.turn() == turn && receipt.phase() == phase
                        && receipt.turnPlayerViewId() == remoteViewId && receipt.islandTapped() == tapped
                        && receipt.islandOwnerViewId() == remoteViewId
                        && receipt.islandControllerViewId() == remoteViewId
                        && receipt.sharedLibraryIds().equals(sharedLibraryIds)
                        && receipt.sharedGraveyardIds().equals(sharedGraveyardIds)
                        && receipt.blueMana() == blueMana);
        if (!exactAppliedReceipt) return false;
        PlayerView remote = current.getPlayers().stream()
                .filter(player -> player.getId() == remoteViewId).findFirst().orElse(null);
        if (remote == null || remote.getMana(MagicColor.BLUE) != blueMana) return false;
        CardView island = remote.getCards(ZoneType.Battlefield).stream()
                .filter(card -> card.getId() == islandId).findFirst().orElse(null);
        return island != null && island.isTapped() == tapped
                && island.getOwner() != null && island.getOwner().getId() == remoteViewId
                && island.getController() != null && island.getController().getId() == remoteViewId
                && current.getTracker() == tracker
                && current.getPlayers().get(0).getCards(ZoneType.Library).stream()
                        .map(CardView::getId).toList().equals(sharedLibraryIds)
                && current.getPlayers().get(0).getCards(ZoneType.Graveyard).stream()
                        .map(CardView::getId).toList().equals(sharedGraveyardIds);
    }

    private static long latestOwnManaUndoAppliedOrdinal(ScriptedHumanTcpSupport.Scenario scenario,
            int gameViewId) {
        return scenario.positiveRemoteOwnManaUndoAppliedReceipts().stream()
                .filter(receipt -> receipt.gameViewId() == gameViewId)
                .mapToLong(ScriptedHumanTcpSupport.OwnManaUndoAppliedReceipt::callbackOrdinal)
                .max().orElse(0);
    }

    private static ScriptedHumanTcpSupport.GameViewReceipt latestCurrentGameViewReceipt(
            ScriptedHumanTcpSupport.ScriptedGui gui, int gameViewId) {
        return gui.gameViewReceipts().stream().filter(receipt -> receipt.gameViewId() == gameViewId)
                .max(Comparator.comparingLong(ScriptedHumanTcpSupport.GameViewReceipt::callbackOrdinal))
                .orElse(null);
    }

    private static void assertDisconnectAiContinuation(boolean deltaSync,
            ScriptedHumanTcpSupport.Scenario scenario, DanDanDesktop desktop,
            ScriptedHumanTcpSupport remote, HostedMatch hosted, FServerManager server,
            RemoteClient retainedRemote, CountDownLatch parkNotice,
            AtomicReference<String> parkNoticeText,
            AtomicReference<ScriptedHumanTcpSupportTest.ServerInactiveObservation> disconnectServerInactive,
            AtomicReference<ScriptedHumanTcpSupportTest.CompletedServerInactiveReceipt> completedDisconnectServerInactive)
            throws Throwable {
        Game oldGame = hosted.getGame();
        Assert.assertNotNull(oldGame, "disconnect begins with the actual hosted Game N");
        forge.game.Match retainedMatch = hosted.getMatch();
        List<PlayerControllerHuman> originalControllers = List.copyOf(hosted.getHumanControllers());
        Assert.assertEquals(originalControllers.size(), 2, "both live human seats are retained before disconnect");
        PlayerControllerHuman hostController = originalControllers.stream()
                .filter(controller -> controller.getGui() == desktop.hostGui && controller.getGame() == oldGame)
                .findFirst().orElseThrow(() -> new AssertionError("missing actual host controller for Game N"));
        PlayerControllerHuman remoteController = originalControllers.stream()
                .filter(controller -> controller.getGui() instanceof RemoteClientGuiGame
                        && controller.getGame() == oldGame)
                .findFirst().orElseThrow(() -> new AssertionError("missing actual remote controller for Game N"));
        Player host = oldGame.getRegisteredPlayers().stream()
                .filter(player -> player.getController() == hostController).findFirst().orElseThrow();
        int originalHostViewId = host.getId();
        Player departed = oldGame.getRegisteredPlayers().stream()
                .filter(player -> player.getController() == remoteController).findFirst().orElseThrow();
        var library = oldGame.getRegisteredPlayers().get(0).getZone(ZoneType.Library);
        var graveyard = oldGame.getRegisteredPlayers().get(0).getZone(ZoneType.Graveyard);
        Assert.assertTrue(oldGame.getRegisteredPlayers().stream()
                .allMatch(player -> player.getZone(ZoneType.Library) == library));
        Assert.assertTrue(oldGame.getRegisteredPlayers().stream()
                .allMatch(player -> player.getZone(ZoneType.Graveyard) == graveyard));
        Assert.assertEquals(oldGame.getCardsInGame().size(), 80,
                "actual Game N begins with its complete shared-pile physical deck");
        ScriptedHumanTcpSupport.PostKeepReceipt held = scenario.postKeepReceipt();
        Assert.assertNotNull(held, "disconnect starts at the actual held remote priority callback");
        Assert.assertEquals(held.prioritySeat(), 1);
        Assert.assertEquals(held.heldTurn(), 1);
        Assert.assertEquals(held.heldPhase(), PhaseType.UPKEEP);
        Assert.assertEquals(held.heldTurnPlayerViewId(), desktop.hostGui.ownPlayerViewId());
        Assert.assertEquals(desktop.hostGui.getGameView().getId(), oldGame.getView().getId());

        DisconnectDrawObserver drawObserver = new DisconnectDrawObserver(oldGame, host);
        oldGame.subscribeToEvents(drawObserver);
        LifecycleTrace oldTrace = new LifecycleTrace();
        oldTrace.attach(oldGame);
        Set<Integer> oldPhysicalIds = oldGame.getCardsInGame().stream().map(Card::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        oldTrace.expectPhysicalCardIds(oldPhysicalIds);
        hosted.setEndGameHook(() -> oldTrace.recordEngineReturn(oldGame));
        AtomicReference<Game> cleanupGame = new AtomicReference<>(oldGame);
        AtomicReference<LifecycleTrace> cleanupTrace = new AtomicReference<>(oldTrace);
        try {
        Channel oldChannel = remote.channel();
        Assert.assertNotNull(oldChannel);
        ScriptedHumanTcpSupportTest.ServerInactiveObservation serverInactive =
                ScriptedHumanTcpSupportTest.prepareServerInactiveObservation(retainedRemote);
        disconnectServerInactive.set(serverInactive);
        var closeFuture = oldChannel.closeFuture();
        remote.close();
        completedDisconnectServerInactive.set(
                ScriptedHumanTcpSupportTest.awaitServerInactive(serverInactive));
        Assert.assertTrue(closeFuture.await(15, TimeUnit.SECONDS) && closeFuture.isSuccess(),
                "the actual remote TCP channel closed successfully");
        var eventLoopTermination = oldChannel.eventLoop().terminationFuture();
        Assert.assertTrue(eventLoopTermination.await(15, TimeUnit.SECONDS) && eventLoopTermination.isSuccess(),
                "the disconnected endpoint event loop drained successfully");
        FThreads.invokeInEdtAndWait(() -> { });
        int disconnectedCallbackBaseline = remote.gui().captures().size();
        Assert.assertTrue(parkNotice.await(10, TimeUnit.SECONDS),
                "the actual server channelInactive path announced the parked seat");
        Assert.assertNotNull(parkNoticeText.get());
        Assert.assertSame(disconnectedClients(server).get(retainedRemote.getUsername()), retainedRemote,
                "the actual server parked the same RemoteClient after the TCP close");
        // The old remote PCH is no longer driven by this fixture; let later connected-host callbacks auto-pass.
        scenario.armDisconnectAiGameNPriority(oldGame, hostController, desktop.hostGui);
        scenario.releasePostKeepPriority();
        Assert.assertTrue(server.handleCommand("/skipreconnect " + retainedRemote.getUsername()),
                "the supported public command accepted the parked remote username");
        Assert.assertSame(hosted.getGame(), oldGame, "takeover retains the current Game N");
        Assert.assertTrue(departed.getController() instanceof PlayerControllerAi,
                "the actual departed seat now has its AI controller");
        Assert.assertNotSame(departed.getOriginalLobbyPlayer(), remoteController.getLobbyPlayer(),
                "the retained old remote PCH no longer matches the AI-controlled player's lobby identity");
        Assert.assertEquals(hosted.getHumanControllers().stream().filter(controller -> controller.getPlayer() == null
                || controller.getPlayer().getOriginalLobbyPlayer() == controller.getLobbyPlayer()).count(), 1L,
                "HostedMatch's actual still-playing predicate counts only the remaining host");
        Assert.assertSame(departed.getRegisteredPlayer().getPlayer(), departed.getController().getLobbyPlayer(),
                "the actual RegisteredPlayer is updated to the AI lobby player for later games");
        Assert.assertSame(host.getController(), hostController, "the connected host keeps its original human controller");
        Assert.assertSame(oldGame.getRegisteredPlayers().get(0).getZone(ZoneType.Library), library);
        Assert.assertSame(oldGame.getRegisteredPlayers().get(0).getZone(ZoneType.Graveyard), graveyard);

        try {
            Assert.assertTrue(drawObserver.await(60, TimeUnit.SECONDS),
                    "INCONCLUSIVE: AI takeover did not reach the remaining host's first non-skipped DRAW step");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        }
        Assert.assertNull(drawObserver.failure(), "the actual zone-event receipt callback remained healthy");
        DisconnectDrawReceipt draw = drawObserver.receipt;
        Assert.assertNotNull(draw, "the observed host draw has an immutable zone-change receipt");
        Assert.assertEquals(draw.gameViewId(), oldGame.getView().getId());
        Assert.assertEquals(draw.toOwnerViewId(), host.getId());
        Assert.assertEquals(draw.actualOwnerViewId(), host.getId(),
                "the ordinary DanDan draw assigns ownership to its destination player");
        Assert.assertEquals(draw.eventOwnerViewId(), draw.actualOwnerViewId(),
                "the detached zone event and actual card agree on the card's owner");
        Assert.assertTrue(oldGame.getRegisteredPlayers().stream()
                .anyMatch(player -> player.getId() == draw.actualOwnerViewId()),
                "the drawn physical card retains one actual registered owner");
        Assert.assertTrue(draw.turn() >= 3,
                "the host's first non-skipped draw follows its skipped first turn and AI's turn");
        Assert.assertEquals(draw.phase(), PhaseType.DRAW);
        Assert.assertEquals(draw.turnPlayerViewId(), host.getId());
        Assert.assertTrue(draw.sharedLibraryAlias() && draw.sharedGraveyardAlias());
        Assert.assertEquals(draw.physicalCardCount(), 80);
        Assert.assertEquals(draw.uniqueCardCount(), 80);
        Assert.assertTrue(draw.cardInHostHand() && draw.cardRemovedFromLibrary(),
                "the synchronously observed shared-library card entered only the host's hand");

        final long takeoverPriorityDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!scenario.isSameDisconnectAiGameNPriority(oldGame, hostController, desktop.hostGui)
                && System.nanoTime() < takeoverPriorityDeadline) {
            assertNoFailure(scenario);
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertNoFailure(scenario);
        Assert.assertTrue(scenario.isSameDisconnectAiGameNPriority(oldGame, hostController, desktop.hostGui),
                "Game N is waiting on its exact host priority input before concession");
        Assert.assertTrue(desktop.hostGui.priorityPrompts().stream().anyMatch(prompt ->
                prompt.seat() == 0 && prompt.chooserViewId() == host.getId() && prompt.turn() == draw.turn()
                        && prompt.phase() == PhaseType.MAIN1 && prompt.turnPlayerViewId() == host.getId()),
                "the held host priority follows the observed takeover draw in the same turn");
        scenario.armDisconnectAiNextGamePreparation(oldGame);
        hostController.concede();
        Assert.assertTrue(oldTrace.awaitOutcome(30, TimeUnit.SECONDS), "Game N emitted its actual outcome");
        Assert.assertTrue(oldTrace.awaitEngineReturn(30, TimeUnit.SECONDS),
                "the actual Match.startGame invocation returned from Game N");
        Assert.assertTrue(oldGame.isGameOver());
        Assert.assertEquals(oldTrace.outcomeCount(), 1);
        Assert.assertTrue(oldTrace.outcomeCardIdsMatch && oldTrace.engineReturnCardIdsMatch);
        Assert.assertSame(hosted.getMatch(), retainedMatch);
        Assert.assertEquals(retainedMatch.getOutcomes().size(), 1);

        int actionBaseline = scenario.actions().size();
        int decisionBaseline = scenario.nextGameDecisions().size();
        long priorityBaseline = desktop.hostGui.priorityPrompts().stream()
                .mapToLong(ScriptedHumanTcpSupport.PriorityReceipt::callbackOrdinal).max().orElse(0);
        scriptKeepWithHostStarter(desktop.hostGui);
        desktop.hostGui.decideNextGame(NextGameDecision.CONTINUE);
        awaitLifecycleCondition(scenario, () -> hosted.getGame() != null && hosted.getGame() != oldGame
                        && desktop.hostGui.getGameView() != null
                        && desktop.hostGui.getGameView().getId() == hosted.getGame().getView().getId(),
                30, "the actual host CONTINUE installed a fresh Game N+1");
        Game nextGame = hosted.getGame();
        Assert.assertNotSame(nextGame, oldGame);
        ScriptedHumanTcpSupport.Scenario.DisconnectAiNextGamePreparationReceipt preparation =
                scenario.disconnectAiNextGamePreparationReceipt();
        Assert.assertNotNull(preparation, "host N+1 setGameView installed its typed preparation observer before engine start");
        Assert.assertSame(preparation.oldGame(), oldGame);
        Assert.assertSame(preparation.game(), nextGame);
        Assert.assertTrue(preparation.awaitLibraryPreparation(30, TimeUnit.SECONDS)
                        && preparation.shuffleCount() > 0,
                "N+1 reached actual shared-library preparation before opening assertions");
        Player nextHost = nextGame.getRegisteredPlayers().stream()
                .filter(player -> player.getController() instanceof PlayerControllerHuman
                        && ((PlayerControllerHuman) player.getController()).getGui() == desktop.hostGui)
                .findFirst().orElseThrow(() -> new AssertionError("N+1 has no actual host human controller"));
        PlayerControllerHuman nextHostController = (PlayerControllerHuman) nextHost.getController();
        LifecycleTrace nextTrace = new LifecycleTrace();
        nextTrace.attach(nextGame);
        Set<Integer> nextPhysicalIds = nextGame.getCardsInGame().stream().map(Card::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        nextTrace.expectPhysicalCardIds(nextPhysicalIds);
        cleanupTrace.set(nextTrace);
        cleanupGame.set(nextGame);
        hosted.setEndGameHook(() -> nextTrace.recordEngineReturn(nextGame));
        awaitLifecycleCondition(scenario, () -> scenario.disconnectAiNextGamePriorities().stream().anyMatch(prompt ->
                        prompt.gameViewId() == nextGame.getView().getId()
                                && prompt.seat() == 0 && prompt.chooserViewId() == nextHost.getId()
                                && prompt.turn() == 1 && prompt.phase() == PhaseType.MAIN1
                                && prompt.turnPlayerViewId() == nextHost.getId()
                                && prompt.callbackOrdinal() > priorityBaseline
                                && scenario.isSameDisconnectAiNextGamePriority(
                                        nextGame, nextHostController, desktop.hostGui)),
                30, "N+1 reached the actual remaining-host opening input");

        List<ScriptedHumanTcpSupport.NextGameDecisionReceipt> continueReceipts =
                scenario.nextGameDecisions().subList(decisionBaseline, scenario.nextGameDecisions().size());
        Assert.assertEquals(continueReceipts.size(), 1,
                "the remaining host emitted one actual next-game decision and the disconnected seat emitted none");
        Assert.assertEquals(continueReceipts.get(0).seat(), 0);
        Assert.assertEquals(continueReceipts.get(0).chooserViewId(), originalHostViewId);
        Assert.assertEquals(continueReceipts.get(0).gameViewId(), oldGame.getView().getId());
        Assert.assertEquals(continueReceipts.get(0).decision(), NextGameDecision.CONTINUE);
        Player nextAi = nextGame.getRegisteredPlayers().stream()
                .filter(player -> player.getController() instanceof PlayerControllerAi)
                .findFirst().orElseThrow(() -> new AssertionError("N+1 did not retain the AI takeover seat"));
        Assert.assertEquals(nextGame.getRegisteredPlayers().stream()
                .filter(player -> player.getController() instanceof PlayerControllerHuman).count(), 1L,
                "exactly the remaining host is human in N+1");
        Assert.assertSame(nextAi.getRegisteredPlayer().getPlayer(), nextAi.getController().getLobbyPlayer());
        Assert.assertEquals(scenario.actions().subList(actionBaseline, scenario.actions().size()).stream()
                .filter(action -> action.decision() == ScriptedHumanTcpSupport.Decision.START_SEAT_ZERO
                        && action.seat() == 0).count(), 1L,
                "only the remaining host made the actual N+1 starter decision");
        Assert.assertEquals(scenario.actions().subList(actionBaseline, scenario.actions().size()).stream()
                .filter(action -> action.decision() == ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND
                        && action.seat() == 0).count(), 1L,
                "the remaining host made the actual N+1 Keep choice");
        List<ScriptedHumanTcpSupport.DisconnectAiOpeningKeepReceipt> nextKeeps =
                scenario.disconnectAiNextGameKeeps();
        Assert.assertEquals(nextKeeps.size(), 1, "one N+1 host Keep callback is retained independently of Game N");
        Assert.assertEquals(nextKeeps.get(0).seat(), 0);
        Assert.assertEquals(nextKeeps.get(0).gameViewId(), nextGame.getView().getId());
        Assert.assertEquals(nextKeeps.get(0).handCount(), 7);
        Assert.assertTrue(nextKeeps.get(0).callbackOrdinal() > 0);
        var nextLibrary = nextGame.getRegisteredPlayers().get(0).getZone(ZoneType.Library);
        var nextGraveyard = nextGame.getRegisteredPlayers().get(0).getZone(ZoneType.Graveyard);
        Assert.assertTrue(nextGame.getRegisteredPlayers().stream()
                .allMatch(player -> player.getZone(ZoneType.Library) == nextLibrary));
        Assert.assertTrue(nextGame.getRegisteredPlayers().stream()
                .allMatch(player -> player.getZone(ZoneType.Graveyard) == nextGraveyard));
        Assert.assertEquals(nextPhysicalIds.size(), 80,
                "N+1 owns one complete within-game physical-card set after takeover");

        Assert.assertTrue(scenario.isSameDisconnectAiNextGamePriority(nextGame, nextHostController, desktop.hostGui),
                "the exact N+1 host InputPassPriority remains current before the existing concession action");
        nextTrace.expectPhysicalCardIds(nextPhysicalIds);
        nextHostController.concede();
        Assert.assertTrue(nextTrace.awaitOutcome(30, TimeUnit.SECONDS), "N+1 emitted its actual outcome during cleanup");
        Assert.assertTrue(nextTrace.awaitEngineReturn(30, TimeUnit.SECONDS),
                "N+1 engine returned before the current host QUIT decision");
        Assert.assertTrue(nextGame.isGameOver());
        Assert.assertEquals(nextTrace.outcomeCount(), 1);
        Assert.assertTrue(nextTrace.outcomeCardIdsMatch && nextTrace.engineReturnCardIdsMatch,
                "N+1 outcome and Match.startGame return retained the complete physical-card set");
        Assert.assertNotNull(nextTrace.outcomeState);
        Assert.assertNotNull(nextTrace.engineReturnState);
        Assert.assertTrue(nextTrace.outcomeState.conflictingCardIds().isEmpty()
                        && nextTrace.engineReturnState.conflictingCardIds().isEmpty(),
                "N+1 outcome and engine return contain no conflicting physical-card identities");
        int quitBaseline = scenario.nextGameDecisions().size();
        desktop.hostGui.decideNextGame(NextGameDecision.QUIT);
        CountDownLatch quitDrain = new CountDownLatch(1);
        FThreads.invokeInEdtLater(quitDrain::countDown);
        Assert.assertTrue(quitDrain.await(15, TimeUnit.SECONDS), "terminal N+1 QUIT drained on the EDT");
        Assert.assertNull(hosted.getGame(), "the current host QUIT performed ordinary match teardown");
        List<ScriptedHumanTcpSupport.NextGameDecisionReceipt> quitReceipts =
                scenario.nextGameDecisions().subList(quitBaseline, scenario.nextGameDecisions().size());
        Assert.assertEquals(quitReceipts.size(), 1);
        Assert.assertEquals(quitReceipts.get(0).decision(), NextGameDecision.QUIT);
        Assert.assertEquals(quitReceipts.get(0).gameViewId(), nextGame.getView().getId());
        Assert.assertSame(hosted.getMatch(), retainedMatch);
        Assert.assertEquals(retainedMatch.getOutcomes().size(), 2);
        Assert.assertEquals(countAfterGameEnd(desktop.hostGui.captures()), 2,
                "the host received one afterGameEnd for N CONTINUE and one for terminal N+1 QUIT");
        Assert.assertEquals(remote.gui().captures().size(), disconnectedCallbackBaseline,
                "the closed remote endpoint received no post-close GameView or private-hand callback");
        assertNoFailure(scenario);
        Assert.assertTrue(deltaSync ? remote.gui().populatedDeltaCount() > 0
                        : remote.gui().captures().stream().anyMatch(capture -> "setGameView".equals(capture.callback())),
                "the disconnected peer received ordinary initial state before its actual close");
        System.out.println("DISCONNECT_AI_CONTINUATION drawGameViewId=" + draw.gameViewId()
                + " nextGameViewId=" + nextGame.getView().getId()
                + " hostContinueCount=1 disconnectedHumanDecisionCount=0 terminalQuitCount=1");
        } catch (Throwable primaryFailure) {
            try {
                Game active = hosted.getGame();
                Game target = active == null ? cleanupGame.get() : active;
                if (target != null) {
                    if (target != cleanupGame.get()) {
                        LifecycleTrace trace = new LifecycleTrace();
                        trace.attach(target);
                        trace.expectPhysicalCardIds(target.getCardsInGame().stream().map(Card::getId)
                                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
                        cleanupGame.set(target);
                        cleanupTrace.set(trace);
                        hosted.setEndGameHook(() -> trace.recordEngineReturn(target));
                    }
                    LifecycleTrace trace = cleanupTrace.get();
                    if (trace == null) throw new AssertionError("captured cleanup Game has no outcome trace");
                    if (!target.isGameOver()) {
                        PlayerControllerHuman cleanupController = target.getRegisteredPlayers().stream()
                                .map(Player::getController)
                                .filter(PlayerControllerHuman.class::isInstance)
                                .map(PlayerControllerHuman.class::cast)
                                .filter(controller -> controller.getGui() == desktop.hostGui
                                        && controller.getGame() == target)
                                .findFirst().orElse(null);
                        if (cleanupController == null) throw new AssertionError(
                                "captured cleanup Game has no actual host controller");
                        cleanupController.concede();
                    }
                    boolean outcomeObserved = trace.awaitOutcome(30, TimeUnit.SECONDS);
                    boolean engineReturnObserved = trace.awaitEngineReturn(30, TimeUnit.SECONDS);
                    if (!outcomeObserved || !engineReturnObserved
                            || trace.outcomeCount() != 1 || !trace.outcomeCardIdsMatch
                            || !trace.engineReturnCardIdsMatch) {
                        throw new AssertionError("captured cleanup Game lacks actual outcome and engine-return evidence");
                    }
                    if (hosted.getGame() == target) {
                        PlayerControllerHuman terminalController = target.getRegisteredPlayers().stream()
                                .map(Player::getController)
                                .filter(PlayerControllerHuman.class::isInstance)
                                .map(PlayerControllerHuman.class::cast)
                                .filter(controller -> controller.getGui() == desktop.hostGui
                                        && controller.getGame() == target)
                                .findFirst().orElseThrow(() -> new AssertionError(
                                        "captured terminal Game has no actual host controller for cleanup QUIT"));
                        terminalController.nextGameDecision(NextGameDecision.QUIT);
                        CountDownLatch cleanupQuitDrain = new CountDownLatch(1);
                        FThreads.invokeInEdtLater(cleanupQuitDrain::countDown);
                        if (!cleanupQuitDrain.await(15, TimeUnit.SECONDS) || hosted.getGame() != null) {
                            throw new AssertionError("captured terminal Game did not clear through actual host QUIT cleanup");
                        }
                    }
                }
            } catch (Throwable cleanupFailure) {
                primaryFailure.addSuppressed(cleanupFailure);
            }
            if (scenario.failure() != null) {
                primaryFailure.addSuppressed(new AssertionError(
                        "disconnect-to-AI scripted callback health failed", scenario.failure()));
            }
            throw primaryFailure;
        }
    }

    private static void assertMetamorphoseLifecycle(boolean deltaSync,
            ScriptedHumanTcpSupport.Scenario scenario, DanDanDesktop desktop,
            ScriptedHumanTcpSupport remote, HostedMatch hosted, FServerManager server,
            ScriptedHumanTcpSupport.StaleActionRejectionLog staleActionRejectionLog,
            AtomicReference<LifecycleCleanupReceipt> lifecycleCleanupReceipt,
            AtomicReference<Game> capturedLifecycleNPlusOne,
            LocalDecisionQueueMode localDecisionQueueMode,
            AtomicBoolean queuedNextGameMayHaveLaunched) throws Throwable {
        Game oldGame = hosted.getGame();
        Assert.assertNotNull(oldGame, "the actual host Game remains live at the held optional choice");
        Set<Integer> oldPhysicalCardIds = oldGame.getCardsInGame().stream().map(Card::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Assert.assertEquals(oldPhysicalCardIds.size(), 80, "capture every old-game physical card identity before concession");
        forge.game.Match retainedMatch = hosted.getMatch();
        LifecycleTrace trace = new LifecycleTrace();
        trace.attach(oldGame);
        trace.expectPhysicalCardIds(oldPhysicalCardIds);
        hosted.setEndGameHook(() -> {
            if (hosted.getGame() == oldGame) trace.recordEngineReturn(oldGame);
        });

        ScriptedHumanTcpSupport.PostKeepReceipt before = scenario.postKeepReceipt();
        Assert.assertNotNull(before, "Meta sequence begins at an actual held first priority");
        ScriptedHumanTcpSupport.ScriptedGui heldPriority = before.prioritySeat() == 0
                ? desktop.hostGui : remote.gui();
        heldPriority.releaseHeldPostKeepPriority();
        Assert.assertTrue(scenario.awaitMetamorphoseFirstStackPriority(30, TimeUnit.SECONDS),
                "the host received the actual Metamorphose stack priority");
        assertNoFailure(scenario);
        List<ScriptedHumanTcpSupport.MetamorphoseStackPriorityReceipt> firstStackReceipts =
                scenario.metamorphoseStackPriorities();
        Assert.assertEquals(firstStackReceipts.size(), 1,
                "the first-stack latch came from a retained valid actual priority receipt");
        ScriptedHumanTcpSupport.MetamorphoseStackPriorityReceipt firstStackReceipt = firstStackReceipts.get(0);
        Assert.assertEquals(firstStackReceipt.seat(), 0);
        Assert.assertEquals(firstStackReceipt.chooserViewId(), scenario.metamorphoseHostViewId());
        Assert.assertSame(firstStackReceipt.game(), oldGame,
                "the held stack priority belongs to the captured actual Game");
        Assert.assertEquals(firstStackReceipt.gameViewId(), oldGame.getView().getId());
        Assert.assertEquals(firstStackReceipt.turn(), 1);
        Assert.assertEquals(firstStackReceipt.phase(), PhaseType.MAIN1);
        Assert.assertEquals(firstStackReceipt.turnPlayerViewId(), scenario.metamorphoseHostViewId());
        Assert.assertEquals(firstStackReceipt.stackSize(), 1);
        Assert.assertTrue(firstStackReceipt.callbackOrdinal() > 0);
        desktop.hostGui.releaseMetamorphoseFirstStackPriority(firstStackReceipt);

        Assert.assertTrue(scenario.awaitMetamorphoseOptionalHeld(30, TimeUnit.SECONDS),
                "the actual remote Meta optional InputConfirm remains held for an explicit human-seat decision");
        assertNoFailure(scenario);
        List<ScriptedHumanTcpSupport.HumanChoiceReceipt> optional = scenario.humanChoiceReceipts().stream()
                .filter(receipt -> "Metamorphose optional question".equals(receipt.stage())).toList();
        Assert.assertEquals(optional.size(), 1, "one actual remote optional choice callback was held");
        Assert.assertEquals(optional.get(0).seat(), 1);
        Assert.assertEquals(optional.get(0).chooserViewId(), remote.gui().ownPlayerViewId());
        Assert.assertTrue(optional.get(0).callbackOrdinal() > 0);
        ScriptedHumanTcpSupport.PrivacyInputSnapshot heldInput = scenario.metamorphoseRemoteInputSnapshot(oldGame);
        Assert.assertNotNull(heldInput, "the assigned remote server controller exposes its live input");
        Assert.assertTrue(heldInput.proxyInput() instanceof InputConfirm,
                "the remote controller is paused in the actual Metamorphose InputConfirm");
        Assert.assertSame(heldInput.proxyInput(), heldInput.queuedInput(),
                "the prompt remains the current and queued input while held");
        IGameController retainedOldRemoteController = null;
        StackItemView retainedOldStackItem = null;
        int oldRemoteGameViewId = -1;
        long heldMetaCallbackOrdinal = -1;
        if (scenario.staleActionWitness()) {
            GameView oldRemoteView = remote.gui().getGameView();
            PlayerView oldRemoteAssignedView = oldRemoteView.getPlayers().stream()
                    .filter(player -> player.getId() == remote.gui().ownPlayerViewId()).findFirst().orElseThrow();
            retainedOldRemoteController = remote.gui().getGameController(oldRemoteAssignedView);
            Assert.assertTrue(retainedOldRemoteController instanceof NetGameController,
                    "retain B's actual Game N NetGameController while its Meta prompt is held");
            Assert.assertNotNull(oldRemoteView.getStack(), "the held Game N view has a current stack list");
            Assert.assertEquals(oldRemoteView.getStack().size(), 1,
                    "the actual Metamorphose remains the sole item on the held Game N stack");
            retainedOldStackItem = oldRemoteView.getStack().get(0);
            Assert.assertNotNull(retainedOldStackItem);
            Assert.assertNotNull(retainedOldStackItem.getSourceCard());
            Assert.assertEquals(retainedOldStackItem.getSourceCard().getId(), scenario.metamorphoseHostCardId(),
                    "retain the actual old Game N Metamorphose stack view for the stale RPC");
            heldMetaCallbackOrdinal = optional.get(0).callbackOrdinal();
            oldRemoteGameViewId = oldRemoteView.getId();
        }

        remote.gui().concedeFromAssignedSeat();
        Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                == ScriptedHumanTcpSupport.Decision.CONCEDE && action.seat() == 1
                && action.chooserViewId() == remote.gui().ownPlayerViewId()).count(), 1L,
                "the actual terminal choice came from the assigned remote seat");
        Assert.assertTrue(desktop.hostGui.awaitActualTerminal(30, TimeUnit.SECONDS),
                "the host received finishGame after the remote assigned controller conceded");
        Assert.assertTrue(remote.gui().awaitActualTerminal(30, TimeUnit.SECONDS),
                "the remote received finishGame after its assigned controller conceded");
        Assert.assertTrue(trace.awaitOutcome(30, TimeUnit.SECONDS), "the old engine emitted its actual outcome");
        Assert.assertTrue(trace.awaitEngineReturn(30, TimeUnit.SECONDS), "HostedMatch endGameHook observed Match.startGame return");
        Assert.assertEquals(trace.outcomeCount, 1, "one old-game outcome was captured at its event boundary");
        Assert.assertTrue(oldGame.isGameOver());
        Assert.assertNotNull(trace.outcomeState);
        Assert.assertNotNull(trace.engineReturnState);
        Assert.assertEquals(trace.outcomeState.cards().size(), 80, "outcome snapshot covers every captured physical card");
        Assert.assertEquals(trace.outcomeState.cards().keySet(), oldPhysicalCardIds,
                "outcome snapshot reads current cards for every original physical identity");
        Assert.assertTrue(trace.outcomeCardIdsMatch && trace.engineReturnCardIdsMatch,
                "both actual old-game boundaries cover the original physical card identity set");
        Assert.assertTrue(trace.outcomeState.conflictingCardIds().isEmpty()
                        && trace.engineReturnState.conflictingCardIds().isEmpty(),
                "duplicate old-game physical identities have consistent current zone/owner/controller state");
        Map<Integer, CardState> expectedEngineReturnCards = new LinkedHashMap<>(trace.outcomeState.cards());
        int resolvingSpellId = scenario.metamorphoseHostCardId();
        CardState spellAtOutcome = expectedEngineReturnCards.get(resolvingSpellId);
        Assert.assertNotNull(spellAtOutcome, "the actual Metamorphose spell remains in the outcome snapshot");
        Assert.assertEquals(spellAtOutcome.zone(), ZoneType.Stack.name(),
                "the actual Metamorphose spell is the resolving Stack card at the outcome boundary");
        Player firstRegisteredPlayer = oldGame.getRegisteredPlayers().get(0);
        var canonicalGraveyard = firstRegisteredPlayer.getZone(ZoneType.Graveyard);
        for (Player registeredPlayer : oldGame.getRegisteredPlayers()) {
            Assert.assertSame(registeredPlayer.getZone(ZoneType.Graveyard), canonicalGraveyard,
                    "the old game uses one authoritative shared graveyard");
        }
        expectedEngineReturnCards.put(resolvingSpellId, new CardState(
                canonicalGraveyard.getZoneType().name(), canonicalGraveyard.getPlayer().getId(),
                spellAtOutcome.ownerId(), spellAtOutcome.controllerId()));
        expectedEngineReturnCards = Map.copyOf(expectedEngineReturnCards);
        Assert.assertEquals(trace.engineReturnState.cards(), expectedEngineReturnCards,
                "after the actual outcome, only the resolving Metamorphose spell moves from Stack to the "
                        + "canonical shared graveyard with owner/controller preserved; "
                        + lifecycleCardDiff(trace.outcomeState, trace.engineReturnState, scenario));
        Assert.assertEquals(trace.engineReturnState.gameViewId(), trace.outcomeState.gameViewId());
        Assert.assertSame(trace.engineReturnState.trackerIdentity(), trace.outcomeState.trackerIdentity());
        ScriptedHumanTcpSupport.TerminalViewReceipt hostTerminal = desktop.hostGui.terminalViewReceipt();
        ScriptedHumanTcpSupport.TerminalViewReceipt remoteTerminal = remote.gui().terminalViewReceipt();
        Assert.assertNotNull(hostTerminal, "host captured its actual terminal GameView");
        Assert.assertNotNull(remoteTerminal, "remote captured its actual terminal GameView");
        for (ScriptedHumanTcpSupport.TerminalViewReceipt terminal : List.of(hostTerminal, remoteTerminal)) {
            Assert.assertEquals(terminal.gameViewId(), trace.outcomeState.gameViewId());
            Assert.assertTrue(terminal.gameOver(), "terminal GameView reflects the actual outcome");
            Assert.assertEquals(terminal.winningTeam(), retainedMatch.getLastOutcome().getWinningTeam());
            Assert.assertEquals(terminal.playedGames(), 0,
                    "terminal GameView retains its pre-outcome played-game count; Match records the outcome separately");
        }
        Assert.assertTrue(hostTerminal.winnerIsOwnPlayer(), "host terminal view identifies the actual outcome winner");
        Assert.assertFalse(remoteTerminal.winnerIsOwnPlayer(), "remote terminal view identifies the host as the actual winner");
        Assert.assertEquals(hosted.getMatch().getOutcomes().size(), 1,
                "the retained match recorded exactly the finished first game");
        Assert.assertSame(hosted.getMatch(), retainedMatch, "the first outcome belongs to the original retained match");
        Assert.assertSame(retainedMatch.getLastOutcome().getWinningPlayer(), retainedMatch.getPlayers().get(0),
                "the actual remote concession records the host as game winner");
        Assert.assertEquals(retainedMatch.getGamesWon().count(retainedMatch.getPlayers().get(0)), 1,
                "the retained match score records one host win before continuation");
        Assert.assertEquals(countAfterGameEnd(desktop.hostGui.captures()), 0,
                "host afterGameEnd follows a next-game decision, not concession");
        Assert.assertEquals(countAfterGameEnd(remote.gui().captures()), 0,
                "remote afterGameEnd follows a next-game decision, not concession");
        assertNoFailure(scenario);
        ScriptedHumanTcpSupport.BufferedMetaSelectableReceipt bufferedSelector =
                scenario.bufferedMetaSelectableReceipt();
        Assert.assertNotNull(bufferedSelector,
                "the only post-concession Meta selector is captured as terminal-view presentation noise");
        Assert.assertEquals(bufferedSelector.gameViewId(), trace.outcomeState.gameViewId());
        Assert.assertEquals(bufferedSelector.seat(), 1);
        Assert.assertTrue(bufferedSelector.callbackOrdinal() > 0);
        Assert.assertEquals(bufferedSelector.minimum(), 1);
        Assert.assertEquals(bufferedSelector.maximum(), 1);
        Assert.assertTrue(bufferedSelector.cardIds().contains(scenario.metamorphoseHandCardId()),
                "the buffered selector contains the actual eligible remote hand candidate");
        Assert.assertEquals(scenario.actions().size(), bufferedSelector.actionCountAtCapture(),
                "the terminal selector callback did not queue a card action");
        ScriptedHumanTcpSupport.GameViewReceipt oldRemoteView = latestNewGameReceipt(
                remote.gui().gameViewReceipts(), -1, false);
        Assert.assertEquals(oldRemoteView.gameViewId(), oldGame.getView().getId());

        hosted.setStartGameHook(null);
        int actionCountBeforeNextGame = scenario.staleActionWitness() ? scenario.actions().size() : -1;
        scenario.prepareLifecycleNextGame(deltaSync ? "applyDelta" : "setGameView");
        scriptKeepWithHostStarter(desktop.hostGui);
        scriptKeepWithHostStarter(remote.gui());
        if (localDecisionQueueMode == LocalDecisionQueueMode.QUEUED_OLD_CONTROLLER_QUIT) {
            assertQueuedLocalDecisionConsumer(hosted, oldGame, retainedMatch, scenario, desktop, remote,
                    lifecycleCleanupReceipt, capturedLifecycleNPlusOne, queuedNextGameMayHaveLaunched);
            return;
        }
        desktop.hostGui.decideNextGame(NextGameDecision.CONTINUE);
        Assert.assertEquals(countAfterGameEnd(desktop.hostGui.captures()), 0,
                "one CONTINUE does not tear down either seat's current game");
        Assert.assertEquals(countAfterGameEnd(remote.gui().captures()), 0);
        remote.gui().decideNextGame(NextGameDecision.CONTINUE);
        awaitLifecycleCondition(scenario, () -> countAfterGameEnd(desktop.hostGui.captures()) == 1
                        && countAfterGameEnd(remote.gui().captures()) == 1,
                30, "both afterGameEnd callbacks follow the two actual CONTINUE decisions");
        awaitLifecycleCheckpoint(scenario, () -> {
            ScriptedHumanTcpSupport.LifecyclePriorityReceipt held = scenario.lifecyclePriorityReceipt();
            long firstAfterEnd = afterGameEndOrdinal(remote.gui().captures(), 1);
            List<ScriptedHumanTcpSupport.GameViewReceipt> remoteReceiptsAfterEnd = held == null ? List.of()
                    : remote.gui().gameViewReceipts().stream()
                            .filter(receipt -> receipt.callbackOrdinal() > firstAfterEnd
                                    && receipt.gameViewId() == held.gameViewId())
                            .toList();
            boolean appliedFreshFullView = remoteReceiptsAfterEnd.stream().anyMatch(receipt ->
                    receipt.callback().equals("setGameView")
                            && receipt.incomingGameViewId() == held.gameViewId()
                            && receipt.gameViewId() == held.gameViewId()
                            && receipt.zonesByPlayer().size() == 2
                            && (deltaSync || receipt.zonesByPlayer().values().stream().allMatch(zones ->
                                    zones.getOrDefault(ZoneType.Hand, List.of()).size() == 7)));
            boolean remoteState = held != null && appliedFreshFullView && (deltaSync
                    ? remoteReceiptsAfterEnd.stream().anyMatch(receipt -> receipt.callback().equals("applyDelta")
                            && receipt.stateBearing() && receipt.zonesByPlayer().size() == 2
                            && receipt.zonesByPlayer().values().stream().allMatch(zones ->
                                    zones.getOrDefault(ZoneType.Hand, List.of()).size() == 7))
                    : remoteReceiptsAfterEnd.stream().noneMatch(receipt ->
                            receipt.callback().equals("applyDelta") && receipt.stateBearing()));
            List<ScriptedHumanTcpSupport.LifecycleKeepReceipt> keeps = held == null ? List.of()
                    : scenario.lifecycleKeepReceipts().stream()
                            .filter(keep -> keep.gameViewId() == held.gameViewId()).toList();
            boolean bothKeepsFollowedTransition = keeps.size() == 2 && keeps.stream().allMatch(keep -> {
                List<ScriptedHumanTcpSupport.Capture> captures = keep.seat() == 0
                        ? desktop.hostGui.captures() : remote.gui().captures();
                long afterEnd = afterGameEndOrdinal(captures, 1);
                return keep.callbackOrdinal() > afterEnd && hasCaptureOrdinal(captures, keep.callbackOrdinal());
            });
            return held != null && remoteState && bothKeepsFollowedTransition && hosted.getGame() != null
                    && held.gameViewId() == hosted.getGame().getView().getId()
                    && held.callbackOrdinal() > 0
                    && hasCaptureOrdinal(held.seat() == 0 ? desktop.hostGui.captures() : remote.gui().captures(),
                            held.callbackOrdinal())
                    && held.phase() != null
                    && keeps.stream().map(ScriptedHumanTcpSupport.LifecycleKeepReceipt::seat).distinct().count() == 2;
        }, 30, oldGame, hosted, desktop.hostGui, remote.gui(), deltaSync,
                "both actual Keeps and the new game's first priority are held at a receipt-keyed N+1 checkpoint");
        assertNoFailure(scenario);
        if (scenario.staleActionWitness()) {
            long scriptedHostPriorityPasses = scenario.actions().subList(actionCountBeforeNextGame,
                            scenario.actions().size()).stream()
                    .filter(action -> action.decision() == ScriptedHumanTcpSupport.Decision.PASS_PRIORITY
                            && action.seat() == 0).count();
            Assert.assertEquals(scriptedHostPriorityPasses, (long) scenario.lifecycleHostPriorityAutoPassCount(),
                    "the receipt-keyed B checkpoint followed exactly the existing scripted A pass, when needed");
        }

        Game newGame = hosted.getGame();
        Assert.assertNotNull(newGame, "CONTINUE retained the hosted match and created a fresh Game");
        Assert.assertNotSame(newGame, oldGame);
        Assert.assertNotSame(newGame.getTracker(), oldGame.getTracker(), "next game has its own host Tracker");
        Assert.assertNotEquals(newGame.getView().getId(), trace.outcomeState.gameViewId(),
                "next game has a fresh GameView identity");
        Assert.assertEquals(newGame.getView().getNumPlayedGamesInMatch(), 1,
                "N+1 GameView starts with the first outcome already in match history");
        Assert.assertEquals(hosted.getMatch().getOutcomes().size(), 1, "match score/history remains attached");
        Map<String, Integer> expectedMain = officialDanDanDeck().getMain().toFlatList().stream()
                .collect(java.util.stream.Collectors.toMap(PaperCard::getName, ignored -> 1, Integer::sum));
        Map<String, Integer> actualCards = newGame.getCardsInGame().stream()
                .collect(java.util.stream.Collectors.toMap(Card::getName, ignored -> 1, Integer::sum));
        Assert.assertEquals(actualCards, expectedMain,
                "the new game contains exactly the official 80-card main deck and no sideboard cards");
        Assert.assertEquals(actualCards.values().stream().mapToInt(Integer::intValue).sum(), 80);
        Assert.assertEquals(newGame.getPlayers().get(0).getCardsIn(ZoneType.Hand).size(), 7);
        Assert.assertEquals(newGame.getPlayers().get(1).getCardsIn(ZoneType.Hand).size(), 7);
        List<Integer> hostHandIds = newGame.getPlayers().get(0).getCardsIn(ZoneType.Hand).stream().map(Card::getId).toList();
        List<Integer> remoteHandIds = newGame.getPlayers().get(1).getCardsIn(ZoneType.Hand).stream().map(Card::getId).toList();
        Assert.assertTrue(Collections.disjoint(hostHandIds, remoteHandIds), "new private hands contain disjoint physical cards");
        Assert.assertSame(newGame.getPlayers().get(0).getZone(ZoneType.Library),
                newGame.getPlayers().get(1).getZone(ZoneType.Library), "DanDan seats share one authoritative library zone");
        Assert.assertSame(newGame.getPlayers().get(0).getZone(ZoneType.Graveyard),
                newGame.getPlayers().get(1).getZone(ZoneType.Graveyard), "DanDan seats share one authoritative graveyard zone");
        Assert.assertSame(hosted.getMatch(), retainedMatch, "CONTINUE preserved the original match object");
        Assert.assertEquals(retainedMatch.getOutcomes().size(), 1, "CONTINUE did not erase the recorded outcome");
        Assert.assertEquals(retainedMatch.getGamesWon().count(retainedMatch.getPlayers().get(0)), 1,
                "CONTINUE preserved the host's recorded score");
        ScriptedHumanTcpSupport.GameViewReceipt hostNext = latestNewGameReceipt(desktop.hostGui.gameViewReceipts(),
                trace.outcomeState.gameViewId(), false);
        ScriptedHumanTcpSupport.GameViewReceipt remoteNext = lifecycleStateReceipt(remote.gui().gameViewReceipts(),
                newGame.getView().getId(), afterGameEndOrdinal(remote.gui().captures(), 1), deltaSync);
        Assert.assertTrue(hostNext.danDan() && remoteNext.danDan(), "both clients retained the DanDan variant");
        Assert.assertTrue(hostNext.callbackOrdinal() > 0 && remoteNext.callbackOrdinal() > 0);
        Assert.assertEquals(hostNext.gameViewId(), newGame.getView().getId());
        Assert.assertEquals(remoteNext.gameViewId(), newGame.getView().getId());
        Assert.assertSame(hostNext.trackerIdentity(), newGame.getTracker());
        Assert.assertEquals(newGame.getView().getPlayers().stream()
                .mapToInt(player -> player.getCards(ZoneType.Hand).size()).sum(), 14,
                "host's fresh live GameView contains both newly dealt hands");
        Assert.assertNotSame(hostNext.trackerIdentity(), trace.outcomeState.trackerIdentity());
        Assert.assertEquals(remoteNext.stateBearing(), deltaSync,
                "state-bearing receipt reflects the selected full-view or delta transport mode");
        Assert.assertEquals(remoteNext.callback(), deltaSync ? "applyDelta" : "setGameView",
                "transport mode is identified by the actual callback kind");
        long nextGameTransitionOrdinal = afterGameEndOrdinal(remote.gui().captures(), 1);
        Assert.assertTrue(remoteNext.callbackOrdinal() > nextGameTransitionOrdinal,
                "remote next-game receipt follows the transition callback");
        if (!deltaSync) Assert.assertFalse(remote.gui().gameViewReceipts().stream().anyMatch(receipt ->
                        receipt.gameViewId() == newGame.getView().getId()
                                && receipt.callbackOrdinal() > nextGameTransitionOrdinal
                                && "applyDelta".equals(receipt.callback()) && receipt.stateBearing()),
                "full-state mode received no N+1 state-bearing delta callback");
        int hostPlayerViewId = newGame.getPlayers().get(0).getId();
        int remotePlayerViewId = newGame.getPlayers().get(1).getId();
        Map<Integer, Map<ZoneType, List<Integer>>> authoritativeZones = lifecycleZones(newGame.getView());
        Assert.assertEquals(authoritativeZones.get(hostPlayerViewId).get(ZoneType.Hand), hostHandIds,
                "host's live view associates the host's private hand with the host seat");
        Assert.assertEquals(authoritativeZones.get(remotePlayerViewId).get(ZoneType.Hand), remoteHandIds,
                "host's live view associates the remote private hand with the remote seat");
        List<Integer> sharedLibraryIds = newGame.getPlayers().get(0).getCardsIn(ZoneType.Library).stream()
                .map(Card::getId).toList();
        List<Integer> sharedGraveyardIds = newGame.getPlayers().get(0).getCardsIn(ZoneType.Graveyard).stream()
                .map(Card::getId).toList();
        for (int playerViewId : List.of(hostPlayerViewId, remotePlayerViewId)) {
            Assert.assertEquals(authoritativeZones.get(playerViewId).get(ZoneType.Library), sharedLibraryIds,
                    "live host view matches the authoritative shared library order");
            Assert.assertEquals(authoritativeZones.get(playerViewId).get(ZoneType.Graveyard), sharedGraveyardIds,
                    "live host view matches the authoritative shared graveyard order");
            Assert.assertEquals(remoteNext.zonesByPlayer().get(playerViewId).get(ZoneType.Library), sharedLibraryIds,
                    "remote view matches the same authoritative shared library order");
            Assert.assertEquals(remoteNext.zonesByPlayer().get(playerViewId).get(ZoneType.Graveyard), sharedGraveyardIds,
                    "remote view matches the same authoritative shared graveyard order");
        }
        Assert.assertEquals(remoteNext.zonesByPlayer().get(hostPlayerViewId).get(ZoneType.Hand).size(), 7);
        Assert.assertEquals(remoteNext.zonesByPlayer().get(remotePlayerViewId).get(ZoneType.Hand).size(), 7);
        GameView remoteNewView = remote.gui().getGameView();
        PlayerView remoteOwn = remoteNewView.getPlayers().stream()
                .filter(player -> player.getId() == remote.gui().ownPlayerViewId()).findFirst().orElseThrow();
        PlayerView remoteOpponent = remoteNewView.getPlayers().stream()
                .filter(player -> player.getId() != remote.gui().ownPlayerViewId()).findFirst().orElseThrow();
        PlayerView hostOwn = newGame.getView().getPlayers().stream()
                .filter(player -> player.getId() == desktop.hostGui.ownPlayerViewId()).findFirst().orElseThrow();
        PlayerView hostOpponent = newGame.getView().getPlayers().stream()
                .filter(player -> player.getId() != desktop.hostGui.ownPlayerViewId()).findFirst().orElseThrow();
        Assert.assertEquals(remoteOwn.getHand().size(), 7);
        Assert.assertEquals(remoteOpponent.getHand().size(), 7);
        Assert.assertEquals(remoteOwn.getHand().stream().map(CardView::getId).toList(), remoteHandIds,
                "remote seat receives its own physical hand");
        Assert.assertTrue(remoteOwn.getHand().stream().allMatch(card -> card.canBeShownTo(remoteOwn)),
                "remote seat can see its own private hand");
        Assert.assertTrue(remoteOpponent.getHand().stream().noneMatch(card -> card.canBeShownTo(remoteOwn)
                        || card.mayPlayerLook(remoteOwn)), "remote seat cannot see or inspect the opposing private hand");
        Assert.assertEquals(hostOwn.getHand().stream().map(CardView::getId).toList(), hostHandIds,
                "host seat receives its own physical hand");
        Assert.assertTrue(hostOwn.getHand().stream().allMatch(card -> card.canBeShownTo(hostOwn)),
                "host seat can see its own private hand");
        Assert.assertTrue(hostOpponent.getHand().stream().noneMatch(card -> card.canBeShownTo(hostOwn)
                        || card.mayPlayerLook(hostOwn)), "host seat cannot see or inspect the opposing private hand");
        ScriptedHumanTcpSupport.LifecyclePriorityReceipt heldNext = scenario.lifecyclePriorityReceipt();
        Assert.assertNotNull(heldNext, "N+1 has a receipt-keyed held priority");
        ScriptedHumanTcpSupport.PrivacyInputSnapshot heldNextInput = scenario.lifecyclePriorityInputSnapshot(
                newGame, heldNext.seat());
        Assert.assertNotNull(heldNextInput, "N+1 remains on the actual assigned priority input");
        Assert.assertTrue(heldNextInput.proxyInput() instanceof InputPassPriority,
                "N+1 priority was not auto-passed before state inspection");
        Assert.assertSame(heldNextInput.proxyInput(), heldNextInput.queuedInput());

        if (scenario.staleActionWitness()) {
            Assert.assertEquals(heldNext.seat(), 1, "the N+1 held input belongs to remote B");
            Assert.assertEquals(heldNext.gameViewId(), newGame.getView().getId(),
                    "the held B prompt is on the actual N+1 GameView");
            IGameController currentRemoteController = remote.gui().getGameController(remoteOwn);
            Assert.assertTrue(currentRemoteController instanceof NetGameController,
                    "N+1 has a current remote NetGameController");
            Assert.assertNotSame(currentRemoteController, retainedOldRemoteController,
                    "openView installed a new B controller for N+1");
            RemoteClient serverRemote = server.getClientBySlotIndex(1);
            Assert.assertNotNull(serverRemote, "the server still has B assigned to slot 1");
            Assert.assertEquals(serverRemote.getIndex(), 1);
            Assert.assertEquals(serverRemote.getUsername(), remote.clientUsername(),
                    "the host tap is installed on the active channel belonging to this scripted B client");
            Channel clientRemoteChannel = remote.channel();
            Assert.assertNotNull(clientRemoteChannel);
            Assert.assertTrue(clientRemoteChannel.isActive(), "B's original client-side TCP channel remains active");
            Channel serverRemoteChannel = ScriptedHumanTcpSupport.serverChannel(serverRemote);
            Assert.assertNotNull(serverRemoteChannel);
            Assert.assertTrue(serverRemoteChannel.isActive(), "B's host-side TCP channel remains active");
            if (scenario.staleQuitWitness()) {
                final int expectedOldGameViewId = oldRemoteGameViewId;
                PlayerControllerHuman cleanupRemoteController = null;
                LifecycleTrace cleanupTrace = new LifecycleTrace();
                boolean cleanupTraceAttached = false;
                ScriptedHumanTcpSupport.ServerDecodedEventTap serverTap = null;
                Throwable quitWitnessFailure = null;
                try {
                    Assert.assertTrue(newGame.getPlayers().get(1).getController() instanceof PlayerControllerHuman,
                            "capture B's actual N+1 human controller for cleanup");
                    cleanupRemoteController = (PlayerControllerHuman) newGame.getPlayers().get(1).getController();
                    Set<Integer> cleanupCardIds = newGame.getCardsInGame().stream().map(Card::getId)
                            .collect(java.util.stream.Collectors.toUnmodifiableSet());
                    Assert.assertEquals(cleanupCardIds.size(), 80,
                            "captured N+1 cleanup state contains all physical cards");
                    cleanupTrace.attach(newGame);
                    cleanupTraceAttached = true;
                    cleanupTrace.expectPhysicalCardIds(cleanupCardIds);
                    hosted.setEndGameHook(() -> cleanupTrace.recordEngineReturn(newGame));
                    int hostAfterGameEndBefore = countAfterGameEnd(desktop.hostGui.captures());
                    int remoteAfterGameEndBefore = countAfterGameEnd(remote.gui().captures());
                    int outcomeCountBefore = cleanupTrace.outcomeCount();
                    LifecycleState n1StateBeforeOldQuit = snapshotGame(newGame);
                    List<ScriptedHumanTcpSupport.NextGameDecisionReceipt> priorDecisions =
                            scenario.nextGameDecisions();
                    Assert.assertEquals(priorDecisions.size(), 2,
                            "both peers made their actual N CONTINUE decisions before the old QUIT witness");
                    Assert.assertTrue(priorDecisions.stream().allMatch(receipt ->
                                    receipt.decision() == NextGameDecision.CONTINUE
                                            && receipt.gameViewId() == expectedOldGameViewId),
                            "the two CONTINUE receipts belong to the actual Game N view");
                    Assert.assertEquals(priorDecisions.stream().map(
                            ScriptedHumanTcpSupport.NextGameDecisionReceipt::seat).distinct().count(), 2L);
                    Assert.assertEquals(hostAfterGameEndBefore, 1,
                            "the host has only the completed N game's afterGameEnd before old QUIT delivery");
                    Assert.assertEquals(remoteAfterGameEndBefore, 1,
                            "B has only the completed N game's afterGameEnd before old QUIT delivery");
                    Assert.assertSame(hosted.getGame(), newGame,
                            "capture the actual hosted N+1 Game before the retained controller action");
                    capturedLifecycleNPlusOne.set(newGame);
                    Assert.assertFalse(newGame.isGameOver(),
                            "the captured N+1 Game is live before old QUIT delivery");
                    Assert.assertNotNull(staleActionRejectionLog,
                            "the QUIT witness has a bounded NETWORK rejection receipt file");
                    staleActionRejectionLog.markBeforeAction();
                    serverTap = ScriptedHumanTcpSupport.ServerDecodedEventTap.install(serverRemoteChannel, 1);
                    serverTap.arm(ProtocolMethod.nextGameDecision);
                    remote.armClientProtocolObservation(ProtocolMethod.nextGameDecision, NextGameDecision.QUIT,
                            expectedOldGameViewId, heldMetaCallbackOrdinal);
                    retainedOldRemoteController.nextGameDecision(NextGameDecision.QUIT);
                    long sendDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    while (remote.clientProtocolSendReceipt() == null
                            && remote.clientProtocolObservationFailure() == null
                            && System.nanoTime() < sendDeadline) {
                        Thread.sleep(5);
                    }
                    ScriptedHumanTcpSupport.ClientProtocolSendReceipt clientSendReceipt =
                            remote.clientProtocolSendReceipt();
                    Assert.assertNull(remote.clientProtocolObservationFailure(),
                            "the retained old controller's send observation remained healthy");
                    Assert.assertNotNull(clientSendReceipt,
                            "the retained Game N controller emitted a real nextGameDecision event");
                    Assert.assertEquals(clientSendReceipt.method(), ProtocolMethod.nextGameDecision);
                    Assert.assertEquals(clientSendReceipt.nextGameDecision(), NextGameDecision.QUIT,
                            "the real old-controller event carries exactly the QUIT enum");
                    Assert.assertEquals(clientSendReceipt.gameViewId(), expectedOldGameViewId,
                            "the retained controller sent QUIT with its immutable Game N identity");
                    Assert.assertEquals(clientSendReceipt.promptCallbackOrdinal(), heldMetaCallbackOrdinal,
                            "the retained controller was captured at B's actual held Game N Meta prompt");

                    long receiveDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    while (serverTap.receipt() == null && serverTap.failure() == null
                            && System.nanoTime() < receiveDeadline) {
                        Thread.sleep(5);
                    }
                    Assert.assertNull(serverTap.failure(), "the host decoded-event tap remained healthy");
                    ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt serverReceipt = serverTap.receipt();
                    Assert.assertNotNull(serverReceipt,
                            "B's active host channel decoded the retained controller's real nextGameDecision");
                    Assert.assertSame(server.getClientBySlotIndex(1), serverRemote,
                            "B remained assigned to the same server client at decode");
                    Assert.assertSame(ScriptedHumanTcpSupport.serverChannel(serverRemote), serverRemoteChannel,
                            "the event arrived on B's current host-side channel");
                    Assert.assertSame(remote.channel(), clientRemoteChannel,
                            "the old controller still used B's original active client channel");
                    Assert.assertSame(serverReceipt.channel(), serverRemoteChannel);
                    Assert.assertEquals(serverReceipt.seat(), 1);
                    Assert.assertEquals(serverReceipt.method(), clientSendReceipt.method());
                    Assert.assertEquals(serverReceipt.nextGameDecision(), NextGameDecision.QUIT,
                            "the host decoded the same scalar QUIT decision");
                    Assert.assertEquals(serverReceipt.gameViewId(), expectedOldGameViewId,
                            "the host decoded the old controller's immutable Game N identity");
                    Assert.assertEquals(serverReceipt.eventId(), clientSendReceipt.eventId(),
                            "the host decoded the exact event emitted by the actual client send path");
                    Assert.assertTrue(serverReceipt.receiveOrdinal() > 0);
                    Assert.assertTrue(clientRemoteChannel.isActive(), "B's original sending channel stayed active");
                    Assert.assertTrue(serverRemoteChannel.isActive(), "B's original receiving channel stayed active");

                    Assert.assertNull(remote.clientProtocolObservationFailure(),
                            "exactly one actual old-controller nextGameDecision was sent");
                    Assert.assertNull(serverTap.failure(),
                            "exactly one actual old-controller nextGameDecision was decoded");
                    String rejectionReceipt = staleActionRejectionLog.awaitRejected(serverReceipt.eventId(),
                            ProtocolMethod.nextGameDecision, 1, expectedOldGameViewId, newGame.getView().getId(),
                            "gameViewMismatch", newGame.getView().getPhase().toString(), 10, TimeUnit.SECONDS);
                    Assert.assertTrue(rejectionReceipt.contains("REJECTED_STALE_GAME_ACTION"),
                            "the host positively logged rejection of this exact decoded retained-controller QUIT");
                    ScriptedHumanTcpSupport.PrivacyInputSnapshot afterOldQuit =
                            scenario.lifecyclePriorityInputSnapshot(newGame, 1);
                    Assert.assertNotNull(afterOldQuit, "B's actual N+1 priority input remains inspectable after rejection");
                    Assert.assertSame(afterOldQuit.proxyInput(), heldNextInput.proxyInput(),
                            "the rejected old QUIT leaves B's N+1 InputProxy unchanged");
                    Assert.assertSame(afterOldQuit.queuedInput(), heldNextInput.queuedInput(),
                            "the rejected old QUIT leaves B's queued N+1 input unchanged");
                    Assert.assertSame(hosted.getGame(), newGame,
                            "the correlated rejection leaves the captured N+1 Game installed");
                    Assert.assertFalse(newGame.isGameOver(), "the retained QUIT did not finish N+1");
                    Assert.assertEquals(cleanupTrace.outcomeCount(), outcomeCountBefore,
                            "the rejected retained QUIT produced no N+1 outcome");
                    Assert.assertEquals(snapshotGame(newGame), n1StateBeforeOldQuit,
                            "the rejected retained QUIT leaves N+1 card and zone state unchanged");
                    Assert.assertEquals(countAfterGameEnd(desktop.hostGui.captures()), hostAfterGameEndBefore,
                            "the rejected retained QUIT produced no host afterGameEnd callback");
                    Assert.assertEquals(countAfterGameEnd(remote.gui().captures()), remoteAfterGameEndBefore,
                            "the rejected retained QUIT produced no remote afterGameEnd callback");

                    IGameController freshRemoteController = remote.gui().getGameController(remoteOwn);
                    Assert.assertTrue(freshRemoteController instanceof NetGameController,
                            "the live N+1 remote controller remains available after stale rejection");
                    Assert.assertSame(freshRemoteController, currentRemoteController,
                            "stale rejection leaves B's installed N+1 controller unchanged");
                    Assert.assertNotSame(freshRemoteController, retainedOldRemoteController,
                            "the fresh controller is distinct from the retained N controller");
                    remote.disarmClientProtocolObservation();
                    serverTap.disarmSelectButtonOk();
                    serverTap.arm(ProtocolMethod.nextGameDecision);
                    remote.armClientProtocolObservation(ProtocolMethod.nextGameDecision, NextGameDecision.QUIT,
                            newGame.getView().getId(), heldNext.callbackOrdinal());
                    freshRemoteController.nextGameDecision(NextGameDecision.QUIT);
                    long freshSendDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    while (remote.clientProtocolSendReceipt() == null
                            && remote.clientProtocolObservationFailure() == null
                            && System.nanoTime() < freshSendDeadline) Thread.sleep(5);
                    ScriptedHumanTcpSupport.ClientProtocolSendReceipt freshSend = remote.clientProtocolSendReceipt();
                    Assert.assertNull(remote.clientProtocolObservationFailure(),
                            "the fresh-controller QUIT send observation remained healthy");
                    Assert.assertNotNull(freshSend, "the current N+1 controller emitted an actual QUIT");
                    Assert.assertEquals(freshSend.method(), ProtocolMethod.nextGameDecision);
                    Assert.assertEquals(freshSend.nextGameDecision(), NextGameDecision.QUIT);
                    Assert.assertEquals(freshSend.gameViewId(), newGame.getView().getId());
                    Assert.assertEquals(freshSend.promptCallbackOrdinal(), heldNext.callbackOrdinal());
                    long freshReceiveDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    while (serverTap.receipt() == null && serverTap.failure() == null
                            && System.nanoTime() < freshReceiveDeadline) Thread.sleep(5);
                    Assert.assertNull(serverTap.failure(), "the host tap remained healthy for the current N+1 QUIT");
                    ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt freshReceive = serverTap.receipt();
                    Assert.assertNotNull(freshReceive, "the host decoded B's fresh N+1 QUIT");
                    Assert.assertEquals(freshReceive.eventId(), freshSend.eventId());
                    Assert.assertEquals(freshReceive.method(), freshSend.method());
                    Assert.assertEquals(freshReceive.nextGameDecision(), NextGameDecision.QUIT);
                    Assert.assertEquals(freshReceive.gameViewId(), newGame.getView().getId());
                    Assert.assertTrue(freshReceive.receiveOrdinal() > serverReceipt.receiveOrdinal());
                    awaitLifecycleCondition(scenario, () -> hosted.getGame() != newGame
                                    && countAfterGameEnd(desktop.hostGui.captures()) > hostAfterGameEndBefore
                                    && countAfterGameEnd(remote.gui().captures()) > remoteAfterGameEndBefore,
                            30, "the actual fresh N+1 controller QUIT advances the retained Match to termination");
                } catch (Throwable failure) {
                    quitWitnessFailure = failure;
                    throw failure;
                } finally {
                    remote.disarmClientProtocolObservation();
                    Throwable cleanupFailure = null;
                    try { if (serverTap != null) serverTap.close(); }
                    catch (Throwable error) { cleanupFailure = combine(cleanupFailure, error); }
                    try {
                        if (cleanupRemoteController != null && !newGame.isGameOver()) cleanupRemoteController.concede();
                    } catch (Throwable error) { cleanupFailure = combine(cleanupFailure, error); }
                    boolean cleanupOutcomeObserved = false;
                    boolean cleanupEngineReturnObserved = false;
                    if (cleanupTraceAttached) {
                        try {
                            cleanupOutcomeObserved = cleanupTrace.awaitOutcome(30, TimeUnit.SECONDS);
                            Assert.assertTrue(cleanupOutcomeObserved,
                                    "captured N+1 Game emitted an outcome during direct-controller cleanup");
                        } catch (Throwable error) { cleanupFailure = combine(cleanupFailure, error); }
                        try {
                            cleanupEngineReturnObserved = cleanupTrace.awaitEngineReturn(30, TimeUnit.SECONDS);
                            Assert.assertTrue(cleanupEngineReturnObserved,
                                    "captured N+1 Game returned from Match.startGame independently of hosted.getGame()");
                        } catch (Throwable error) { cleanupFailure = combine(cleanupFailure, error); }
                    }
                    try {
                        if (hosted.getGame() == newGame) hosted.endCurrentGame();
                    } catch (Throwable error) { cleanupFailure = combine(cleanupFailure, error); }
                    if (cleanupFailure == null && cleanupTrace.game == newGame
                            && cleanupTrace.outcomeState != null && cleanupTrace.engineReturnState != null) {
                        lifecycleCleanupReceipt.set(new LifecycleCleanupReceipt(newGame,
                                newGame.getView().getId(), cleanupTrace.outcomeCount(),
                                cleanupOutcomeObserved, cleanupEngineReturnObserved));
                    }
                    if (cleanupFailure != null) {
                        if (quitWitnessFailure != null) quitWitnessFailure.addSuppressed(cleanupFailure);
                        else if (cleanupFailure instanceof Exception exception) throw exception;
                        else if (cleanupFailure instanceof Error error) throw error;
                        else throw new RuntimeException(cleanupFailure);
                    }
                }
                return;
            }
            ScriptedHumanTcpSupport.ServerDecodedEventTap serverTap =
                    ScriptedHumanTcpSupport.ServerDecodedEventTap.install(serverRemoteChannel, 1);
            long firstHostPromptOrdinal = desktop.hostGui.priorityPrompts().stream()
                    .mapToLong(ScriptedHumanTcpSupport.PriorityReceipt::callbackOrdinal).max().orElse(0);
            Assert.assertNotNull(staleActionRejectionLog,
                    "stale-action fixture initialized a bounded NETWORK rejection receipt file");
            Throwable witnessFailure = null;
            try {
                Assert.assertNotNull(retainedOldStackItem,
                        "the stale-action fixture retained the actual Game N stack item");
                IGameController retainedController = retainedOldRemoteController;
                StackItemView retainedStackItem = retainedOldStackItem;
                StaleGameActionReceipt staleStack = sendAndAssertStaleGameAction(scenario, remote, server,
                        serverRemote, clientRemoteChannel, serverRemoteChannel, serverTap, staleActionRejectionLog,
                        () -> retainedController.selectStackItem(retainedStackItem),
                        ProtocolMethod.selectStackItem, oldRemoteGameViewId, heldMetaCallbackOrdinal,
                        newGame, hosted, desktop.hostGui, heldNextInput, firstHostPromptOrdinal);
                StaleGameActionReceipt staleOk = sendAndAssertStaleGameAction(scenario, remote, server,
                        serverRemote, clientRemoteChannel, serverRemoteChannel, serverTap, staleActionRejectionLog,
                        retainedController::selectButtonOk, ProtocolMethod.selectButtonOk,
                        oldRemoteGameViewId, heldMetaCallbackOrdinal, newGame, hosted, desktop.hostGui,
                        heldNextInput, firstHostPromptOrdinal);
                Assert.assertEquals(staleOk.client().sendOrdinal(), staleStack.client().sendOrdinal() + 1,
                        "the two stale RPCs are consecutive sends from the retained controller");
                Assert.assertTrue(staleOk.server().receiveOrdinal() > staleStack.server().receiveOrdinal(),
                        "the host decoded both stale RPCs in their send order on B's active channel");
                serverTap.disarm();
                serverTap.armSelectButtonOk();
                remote.armSelectButtonOkObservation(newGame.getView().getId(), heldNext.callbackOrdinal());
                currentRemoteController.selectButtonOk();
                long currentSendDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (remote.clientSelectButtonOkReceipt() == null && System.nanoTime() < currentSendDeadline) {
                    assertNoFailure(scenario);
                    Thread.sleep(5);
                }
                ScriptedHumanTcpSupport.ClientProtocolSendReceipt currentClientSend =
                        remote.clientSelectButtonOkReceipt();
                Assert.assertNotNull(currentClientSend,
                        "the current N+1 B controller sends its real priority action after stale rejection");
                Assert.assertEquals(currentClientSend.sendOrdinal(), staleOk.client().sendOrdinal() + 1,
                        "the current-controller send immediately follows the retained controller's two stale sends");
                Assert.assertEquals(currentClientSend.method(), ProtocolMethod.selectButtonOk);
                Assert.assertEquals(currentClientSend.gameViewId(), newGame.getView().getId(),
                        "the fresh B action carries the immutable N+1 controller identity");
                Assert.assertEquals(currentClientSend.promptCallbackOrdinal(), heldNext.callbackOrdinal(),
                        "the fresh action is attached to B's actual held N+1 prompt");
                long currentReceiveDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (serverTap.receipt() == null && serverTap.failure() == null
                        && System.nanoTime() < currentReceiveDeadline) {
                    assertNoFailure(scenario);
                    Thread.sleep(5);
                }
                Assert.assertNull(serverTap.failure(), "the host tap remains healthy for the current N+1 action");
                ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt currentServerReceipt = serverTap.receipt();
                Assert.assertNotNull(currentServerReceipt, "the host decoded B's fresh priority action");
                Assert.assertEquals(currentServerReceipt.eventId(), currentClientSend.eventId());
                Assert.assertEquals(currentServerReceipt.method(), currentClientSend.method());
                Assert.assertEquals(currentServerReceipt.gameViewId(), newGame.getView().getId());
                Assert.assertTrue(currentServerReceipt.receiveOrdinal() > staleOk.server().receiveOrdinal(),
                        "the current B action follows both stale actions on the same decoded-event channel");
                Assert.assertEquals(currentServerReceipt.seat(), 1);
                Assert.assertSame(currentServerReceipt.channel(), serverRemoteChannel);

                ScriptedHumanTcpSupport.PrivacyInputSnapshot[] afterCurrentAction =
                        new ScriptedHumanTcpSupport.PrivacyInputSnapshot[1];
                awaitLifecycleCheckpoint(scenario, () -> {
                    afterCurrentAction[0] = scenario.lifecyclePriorityInputSnapshot(newGame, 1);
                    GameView appliedHostView = desktop.hostGui.getGameView();
                    boolean bInputResolved = afterCurrentAction[0] != null
                            && afterCurrentAction[0].proxyInput() != heldNextInput.proxyInput()
                            && afterCurrentAction[0].queuedInput() != heldNextInput.queuedInput();
                    boolean nextHostPrompt = desktop.hostGui.priorityPrompts().stream().anyMatch(prompt ->
                            prompt.seat() == 0 && prompt.chooserViewId() == desktop.hostGui.ownPlayerViewId()
                                    && prompt.callbackOrdinal() > firstHostPromptOrdinal);
                    return hosted.getGame() == newGame && !newGame.isGameOver()
                            && appliedHostView != null && appliedHostView.getId() == newGame.getView().getId()
                            && bInputResolved && nextHostPrompt;
                }, 20, oldGame, hosted, desktop.hostGui, remote.gui(), deltaSync,
                        "the fresh current-controller action resolves B's N+1 InputPassPriority and produces A's next real priority callback");
                Assert.assertSame(hosted.getGame(), newGame,
                        "the fresh current action's positive input-resolution receipt belongs to the still-live N+1 Game");
                Assert.assertFalse(newGame.isGameOver(), "the N+1 input advances before any concession or cleanup");
                Assert.assertEquals(desktop.hostGui.getGameView().getId(), newGame.getView().getId(),
                        "A's later real priority callback belongs to the applied N+1 GameView");
                Assert.assertNotSame(afterCurrentAction[0].proxyInput(), heldNextInput.proxyInput(),
                        "B's actual current N+1 InputPassPriority left the current InputProxy");
                Assert.assertNotSame(afterCurrentAction[0].queuedInput(), heldNextInput.queuedInput(),
                        "B's actual current N+1 InputPassPriority left the queued input");
                Assert.assertNull(remote.clientProtocolObservationFailure(),
                        "the client send observer saw one action in each separately armed stale/current window");
            } catch (Throwable failure) {
                witnessFailure = failure;
                throw failure;
            } finally {
                remote.disarmClientSelectButtonOkObservation();
                try {
                    serverTap.close();
                } catch (Throwable cleanupFailure) {
                    if (witnessFailure != null) {
                        witnessFailure.addSuppressed(cleanupFailure);
                    } else if (cleanupFailure instanceof Exception exception) {
                        throw exception;
                    } else if (cleanupFailure instanceof Error error) {
                        throw error;
                    } else {
                        throw new RuntimeException(cleanupFailure);
                    }
                }
            }
        }

        Set<Integer> nextPhysicalCardIds = newGame.getCardsInGame().stream().map(Card::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Assert.assertEquals(nextPhysicalCardIds.size(), 80, "capture all physical N+1 card identities before concession");
        LifecycleTrace nextTrace = new LifecycleTrace();
        nextTrace.attach(newGame);
        nextTrace.expectPhysicalCardIds(nextPhysicalCardIds);
        hosted.setEndGameHook(() -> {
            if (hosted.getGame() == newGame) nextTrace.recordEngineReturn(newGame);
        });
        remote.gui().concedeFromAssignedSeat();
        Assert.assertTrue(nextTrace.awaitOutcome(30, TimeUnit.SECONDS),
                "N+1 emitted its actual game outcome event");
        Assert.assertTrue(nextTrace.awaitEngineReturn(30, TimeUnit.SECONDS),
                "N+1 returned from Match.startGame through the HostedMatch end hook");
        awaitLifecycleCondition(scenario, () -> countCallback(desktop.hostGui.captures(), "finishGame") == 2
                        && countCallback(remote.gui().captures(), "finishGame") == 2
                        && desktop.hostGui.terminalViewReceipt() != null
                        && remote.gui().terminalViewReceipt() != null
                        && desktop.hostGui.terminalViewReceipt().gameViewId() == newGame.getView().getId()
                        && remote.gui().terminalViewReceipt().gameViewId() == newGame.getView().getId(),
                30, "both finishGame terminal GameViews identify the completed N+1 game");
        Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                == ScriptedHumanTcpSupport.Decision.CONCEDE && action.seat() == 1
                && action.chooserViewId() == remote.gui().ownPlayerViewId()).count(), 2L,
                "both game outcomes followed actual remote assigned-seat concessions");
        Assert.assertEquals(nextTrace.outcomeCount, 1);
        Assert.assertEquals(desktop.hostGui.terminalViewReceipt().gameViewId(), newGame.getView().getId());
        Assert.assertEquals(remote.gui().terminalViewReceipt().gameViewId(), newGame.getView().getId());
        Assert.assertTrue(desktop.hostGui.terminalViewReceipt().gameOver());
        Assert.assertTrue(remote.gui().terminalViewReceipt().gameOver());
        Assert.assertTrue(desktop.hostGui.terminalViewReceipt().winnerIsOwnPlayer());
        Assert.assertFalse(remote.gui().terminalViewReceipt().winnerIsOwnPlayer());
        Assert.assertEquals(desktop.hostGui.terminalViewReceipt().winningTeam(), retainedMatch.getLastOutcome().getWinningTeam());
        Assert.assertEquals(remote.gui().terminalViewReceipt().winningTeam(), retainedMatch.getLastOutcome().getWinningTeam());
        Assert.assertEquals(desktop.hostGui.terminalViewReceipt().playedGames(), 1);
        Assert.assertEquals(remote.gui().terminalViewReceipt().playedGames(), 1);
        Assert.assertEquals(nextTrace.outcomeState.cards().size(), 80);
        Assert.assertEquals(nextTrace.outcomeState.cards().keySet(), nextPhysicalCardIds,
                "N+1 outcome snapshot reads current cards for every original physical identity");
        Assert.assertTrue(nextTrace.outcomeCardIdsMatch && nextTrace.engineReturnCardIdsMatch,
                "both actual N+1 boundaries cover the original physical card identity set");
        Assert.assertTrue(nextTrace.outcomeState.conflictingCardIds().isEmpty()
                        && nextTrace.engineReturnState.conflictingCardIds().isEmpty(),
                "duplicate N+1 physical identities have consistent current zone/owner/controller state");
        Assert.assertEquals(nextTrace.engineReturnState.cards(), nextTrace.outcomeState.cards(),
                "no N+1 card or zone state changed between outcome and engine return");
        Assert.assertTrue(newGame.isGameOver());
        Assert.assertEquals(retainedMatch.getOutcomes().size(), 2, "the original match retains both actual game outcomes");
        Assert.assertSame(retainedMatch.getLastOutcome().getWinningPlayer(), retainedMatch.getPlayers().get(0));
        Assert.assertEquals(retainedMatch.getGamesWon().count(retainedMatch.getPlayers().get(0)), 2,
                "the retained match score includes both actual host wins");
        Assert.assertEquals(countAfterGameEnd(desktop.hostGui.captures()), 1,
                "N+1 concession alone does not emit afterGameEnd");
        Assert.assertEquals(countAfterGameEnd(remote.gui().captures()), 1,
                "N+1 concession alone does not emit afterGameEnd");
        assertNoFailure(scenario);

        desktop.hostGui.decideNextGame(NextGameDecision.QUIT);
        awaitLifecycleCondition(scenario, () -> hosted.getGame() == null
                        && countAfterGameEnd(desktop.hostGui.captures()) == 2
                        && countAfterGameEnd(remote.gui().captures()) == 2,
                30, "actual QUIT ended the next game for both peers");
        List<ScriptedHumanTcpSupport.NextGameDecisionReceipt> decisions = scenario.nextGameDecisions();
        Assert.assertEquals(decisions.stream().filter(receipt -> receipt.decision() == NextGameDecision.CONTINUE).count(), 2);
        Assert.assertEquals(decisions.stream().filter(receipt -> receipt.decision() == NextGameDecision.QUIT).count(), 1);
        Assert.assertTrue(decisions.stream().filter(receipt -> receipt.decision() == NextGameDecision.CONTINUE)
                .allMatch(receipt -> receipt.gameViewId() == trace.outcomeState.gameViewId()));
        Assert.assertTrue(decisions.stream().filter(receipt -> receipt.decision() == NextGameDecision.QUIT)
                .allMatch(receipt -> receipt.gameViewId() == newGame.getView().getId()));
        Assert.assertTrue(decisions.stream().filter(receipt -> receipt.decision() == NextGameDecision.CONTINUE)
                .anyMatch(receipt -> receipt.seat() == 0 && receipt.chooserViewId() == desktop.hostGui.ownPlayerViewId()));
        Assert.assertTrue(decisions.stream().filter(receipt -> receipt.decision() == NextGameDecision.CONTINUE)
                .anyMatch(receipt -> receipt.seat() == 1 && receipt.chooserViewId() == remote.gui().ownPlayerViewId()));
        assertNoFailure(scenario);
        Assert.assertEquals(server.getTotalSendErrors(), 0, "both transport modes completed the lifecycle without send errors");
        assertReadinessGateSummary(scenario, "lifecycle");
    }

    private record StaleGameActionReceipt(ScriptedHumanTcpSupport.ClientProtocolSendReceipt client,
                                          ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt server) { }

    private static StaleGameActionReceipt sendAndAssertStaleGameAction(
            ScriptedHumanTcpSupport.Scenario scenario, ScriptedHumanTcpSupport remote, FServerManager server,
            RemoteClient serverRemote, Channel clientRemoteChannel, Channel serverRemoteChannel,
            ScriptedHumanTcpSupport.ServerDecodedEventTap serverTap,
            ScriptedHumanTcpSupport.StaleActionRejectionLog rejectionLog, Runnable sendAction,
            ProtocolMethod method, int oldGameViewId, long oldPromptCallbackOrdinal, Game newGame,
            HostedMatch hosted, ScriptedHumanTcpSupport.ScriptedGui hostGui,
            ScriptedHumanTcpSupport.PrivacyInputSnapshot heldNextInput, long firstHostPromptOrdinal)
            throws Exception {
        LifecycleState stateBefore = snapshotGame(newGame);
        rejectionLog.markBeforeAction();
        serverTap.arm(method);
        remote.armClientProtocolObservation(method, null, oldGameViewId, oldPromptCallbackOrdinal);
        sendAction.run();

        long sendDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (remote.clientProtocolSendReceipt() == null && System.nanoTime() < sendDeadline) {
            assertNoFailure(scenario);
            Thread.sleep(5);
        }
        ScriptedHumanTcpSupport.ClientProtocolSendReceipt clientReceipt = remote.clientProtocolSendReceipt();
        Assert.assertNull(remote.clientProtocolObservationFailure(),
                "the retained controller sent exactly one " + method + " in the armed window");
        Assert.assertNotNull(clientReceipt, "the retained Game N controller emitted its real " + method + " RPC");
        Assert.assertEquals(clientReceipt.method(), method);
        Assert.assertNull(clientReceipt.nextGameDecision());
        Assert.assertEquals(clientReceipt.gameViewId(), oldGameViewId,
                "the retained action carries the actual Game N controller identity");
        Assert.assertEquals(clientReceipt.promptCallbackOrdinal(), oldPromptCallbackOrdinal,
                "the retained action originated at B's held Game N Meta prompt");

        long receiveDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (serverTap.receipt() == null && serverTap.failure() == null
                && System.nanoTime() < receiveDeadline) {
            assertNoFailure(scenario);
            Thread.sleep(5);
        }
        Assert.assertNull(serverTap.failure(), "the server decoded-event tap remained healthy for " + method);
        ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt serverReceipt = serverTap.receipt();
        Assert.assertNotNull(serverReceipt, "the active B server channel decoded the retained " + method);
        Assert.assertSame(server.getClientBySlotIndex(1), serverRemote,
                "B still owns the same assigned server client when its event is decoded");
        Assert.assertSame(ScriptedHumanTcpSupport.serverChannel(serverRemote), serverRemoteChannel,
                "the decoded event channel remains B's currently registered server channel");
        Assert.assertSame(remote.channel(), clientRemoteChannel,
                "the retained controller still sends through B's original active client channel");
        Assert.assertSame(serverReceipt.channel(), serverRemoteChannel,
                "the decoded event arrived on B's exact active host-side channel");
        Assert.assertEquals(serverReceipt.gameViewId(), oldGameViewId,
                "the decoded stale action carries B's immutable Game N identity");
        Assert.assertEquals(serverReceipt.seat(), 1);
        Assert.assertEquals(serverReceipt.method(), clientReceipt.method());
        Assert.assertNull(serverReceipt.nextGameDecision());
        Assert.assertEquals(serverReceipt.eventId(), clientReceipt.eventId(),
                "the host decoded the exact event captured from the real client send path");
        Assert.assertTrue(clientRemoteChannel.isActive(), "B's sending client channel stayed active through receipt");
        Assert.assertTrue(serverRemoteChannel.isActive(), "B's receiving server channel stayed active through receipt");
        Assert.assertTrue(serverReceipt.receiveOrdinal() > 0,
                "the receipt has a bounded per-channel decoded-event ordinal");

        String rejectionReceipt = rejectionLog.awaitRejected(serverReceipt.eventId(), method, 1,
                oldGameViewId, newGame.getView().getId(), "gameViewMismatch",
                newGame.getView().getPhase().toString(), 10, TimeUnit.SECONDS);
        Assert.assertTrue(rejectionReceipt.contains("REJECTED_STALE_GAME_ACTION"),
                "the host emitted an explicit correlated stale " + method + " rejection receipt");
        ScriptedHumanTcpSupport.PrivacyInputSnapshot afterStaleAction =
                scenario.lifecyclePriorityInputSnapshot(newGame, 1);
        Assert.assertNotNull(afterStaleAction, "B's N+1 InputPassPriority remains observable after rejection");
        Assert.assertSame(afterStaleAction.proxyInput(), heldNextInput.proxyInput(),
                "rejecting the old controller leaves B's current InputProxy untouched");
        Assert.assertSame(afterStaleAction.queuedInput(), heldNextInput.queuedInput(),
                "rejecting the old controller leaves B's queued input untouched");
        Assert.assertSame(hosted.getGame(), newGame,
                "the explicit rejection belongs to the still-live N+1 Game");
        Assert.assertFalse(newGame.isGameOver(), "the stale action rejection precedes cleanup or concession");
        Assert.assertEquals(hostGui.getGameView().getId(), newGame.getView().getId(),
                "the active host view remains N+1 after stale-action rejection");
        Assert.assertEquals(snapshotGame(newGame), stateBefore,
                "rejecting the old controller action leaves N+1 game state unchanged");
        Assert.assertFalse(hostGui.priorityPrompts().stream().anyMatch(prompt ->
                        prompt.seat() == 0 && prompt.chooserViewId() == hostGui.ownPlayerViewId()
                                && prompt.callbackOrdinal() > firstHostPromptOrdinal),
                "the rejected old action did not produce A's next priority callback");

        remote.disarmClientProtocolObservation();
        serverTap.disarm();
        return new StaleGameActionReceipt(clientReceipt, serverReceipt);
    }

    private static int countAfterGameEnd(List<ScriptedHumanTcpSupport.Capture> captures) {
        return countCallback(captures, "afterGameEnd");
    }

    private static int countCallback(List<ScriptedHumanTcpSupport.Capture> captures, String callback) {
        return (int) captures.stream().filter(capture -> callback.equals(capture.callback())).count();
    }

    private static long afterGameEndOrdinal(List<ScriptedHumanTcpSupport.Capture> captures, int occurrence) {
        return captures.stream().filter(capture -> "afterGameEnd".equals(capture.callback()))
                .skip(occurrence - 1L).mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal)
                .findFirst().orElseThrow(() -> new AssertionError("Missing afterGameEnd receipt " + occurrence));
    }

    private static boolean hasCaptureOrdinal(List<ScriptedHumanTcpSupport.Capture> captures, long ordinal) {
        return captures.stream().anyMatch(capture -> capture.callbackOrdinal() == ordinal);
    }

    private static ScriptedHumanTcpSupport.GameViewReceipt latestNewGameReceipt(
            List<ScriptedHumanTcpSupport.GameViewReceipt> receipts, int oldGameId) {
        return latestNewGameReceipt(receipts, oldGameId, true);
    }

    private static ScriptedHumanTcpSupport.GameViewReceipt latestNewGameReceipt(
            List<ScriptedHumanTcpSupport.GameViewReceipt> receipts, int oldGameId, boolean populatedHand) {
        return receipts.stream().filter(receipt -> receipt.gameViewId() != oldGameId
                        && (!populatedHand || receipt.zonesByPlayer().values().stream().allMatch(zones ->
                                zones.getOrDefault(ZoneType.Hand, List.of()).size() == 7)))
                .max(java.util.Comparator.comparingLong(ScriptedHumanTcpSupport.GameViewReceipt::callbackOrdinal))
                .orElseThrow(() -> new AssertionError("No bounded next-game receipt after old GameView " + oldGameId));
    }

    private static ScriptedHumanTcpSupport.GameViewReceipt lifecycleStateReceipt(
            List<ScriptedHumanTcpSupport.GameViewReceipt> receipts, int gameViewId,
            long transitionOrdinal, boolean deltaSync) {
        String callback = deltaSync ? "applyDelta" : "setGameView";
        return receipts.stream().filter(receipt -> receipt.gameViewId() == gameViewId
                        && receipt.callbackOrdinal() > transitionOrdinal
                        && callback.equals(receipt.callback()) && receipt.stateBearing() == deltaSync
                        && receipt.zonesByPlayer().size() == 2
                        && receipt.zonesByPlayer().values().stream().allMatch(zones ->
                                zones.getOrDefault(ZoneType.Hand, List.of()).size() == 7))
                .max(java.util.Comparator.comparingLong(ScriptedHumanTcpSupport.GameViewReceipt::callbackOrdinal))
                .orElseThrow(() -> new AssertionError("No populated N+1 " + callback + " receipt for GameView " + gameViewId));
    }

    private static Map<Integer, Map<ZoneType, List<Integer>>> lifecycleZones(GameView view) {
        Map<Integer, Map<ZoneType, List<Integer>>> result = new LinkedHashMap<>();
        for (PlayerView player : view.getPlayers()) {
            Map<ZoneType, List<Integer>> zones = new LinkedHashMap<>();
            for (ZoneType zone : List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard)) {
                zones.put(zone, player.getCards(zone).stream().map(CardView::getId).toList());
            }
            result.put(player.getId(), Collections.unmodifiableMap(zones));
        }
        return Collections.unmodifiableMap(result);
    }

    private static String lifecycleCardDiff(LifecycleState outcome, LifecycleState engineReturn,
                                            ScriptedHumanTcpSupport.Scenario scenario) {
        Set<Integer> cardIds = new java.util.TreeSet<>(outcome.cards().keySet());
        cardIds.addAll(engineReturn.cards().keySet());
        List<String> sample = new ArrayList<>();
        int changedCount = 0;
        for (int cardId : cardIds) {
            CardState before = outcome.cards().get(cardId);
            CardState after = engineReturn.cards().get(cardId);
            if (Objects.equals(before, after)) continue;
            changedCount++;
            if (sample.size() < 8) {
                String role = cardId == scenario.metamorphoseHostCardId() ? "metamorphoseSpell"
                        : cardId == scenario.metamorphoseTargetCardId() ? "metamorphoseTarget"
                        : cardId == scenario.metamorphoseHandCardId() ? "optionalHandCandidate" : "other";
                sample.add("id=" + cardId + ",role=" + role + ",outcome=" + before + ",engineReturn=" + after);
            }
        }
        return "changedCardCount=" + changedCount + ",sample=" + sample;
    }

    private static String viewHandoffDiagnostics(ScriptedHumanTcpSupport.ScriptedGui gui, int gameViewId) {
        List<ScriptedHumanTcpSupport.GameViewReceipt> matching = gui.gameViewReceipts().stream()
                .filter(receipt -> receipt.gameViewId() == gameViewId)
                .toList();
        List<String> recent = matching.stream().skip(Math.max(0, matching.size() - 3L))
                .map(receipt -> {
                    ScriptedHumanTcpSupport.Capture decoded = gui.captures().stream()
                            .filter(capture -> capture.callbackOrdinal() == receipt.callbackOrdinal())
                            .findFirst().orElse(null);
                    return receipt.callback() + "#" + receipt.sequence() + "/" + receipt.callbackOrdinal()
                            + " incomingView=" + receipt.incomingGameViewId()
                            + " decodedHandCounts=" + (decoded == null ? "missing" : handCounts(decoded.zonesByPlayer()))
                            + " appliedView=" + receipt.gameViewId()
                            + " appliedHandCounts=" + handCounts(receipt.zonesByPlayer())
                            + " state=" + receipt.stateBearing() + " danDan=" + receipt.danDan();
                })
                .toList();
        return "count=" + matching.size() + ",recent=" + recent;
    }

    private static List<Integer> handCounts(Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) {
        return zonesByPlayer.values().stream()
                .map(zones -> zones.getOrDefault(ZoneType.Hand, List.of()).size()).toList();
    }

    private static void assertLifecycleCheckpointNoFailure(ScriptedHumanTcpSupport.Scenario scenario,
            HostedMatch hosted, ScriptedHumanTcpSupport.ScriptedGui hostGui,
            ScriptedHumanTcpSupport.ScriptedGui remoteGui) {
        Throwable failure = scenario.failure();
        if (failure == null) return;
        Game current = hosted.getGame();
        String engine = current == null ? "game=null" : "gameViewId=" + current.getView().getId()
                + ",gameIdentity=" + System.identityHashCode(current) + ",gameOver=" + current.isGameOver()
                + ",age=" + current.getAge() + ",seatHandCounts="
                + current.getPlayers().stream().map(player -> player.getCardsIn(ZoneType.Hand).size()).toList()
                + ",trackerIdentity=" + System.identityHashCode(current.getTracker());
        GameView hostView = hostGui.getGameView();
        GameView remoteView = remoteGui.getGameView();
        int hostViewId = hostView == null ? -1 : hostView.getId();
        int remoteViewId = remoteView == null ? -1 : remoteView.getId();
        List<String> nextGameDecisions = scenario.nextGameDecisions().stream()
                .map(receipt -> "seat=" + receipt.seat() + "/view=" + receipt.gameViewId()
                        + "/decision=" + receipt.decision()).limit(4).toList();
        throw new AssertionError("scripted DanDan TCP fixture failed; lifecycleFailureDiagnostic={engine={"
                + engine + "},hostAppliedGameViewId=" + hostViewId + ",remoteAppliedGameViewId=" + remoteViewId
                + ",hostViewHandoffs=" + viewHandoffDiagnostics(hostGui, hostViewId)
                + ",remoteViewHandoffs=" + viewHandoffDiagnostics(remoteGui, remoteViewId)
                + ",capturedNextGameDecisions=" + nextGameDecisions
                + ",oldQuitObservationStarted=false}", failure);
    }

    private static void cleanupLifecycleNextGameIfActive(HostedMatch hosted,
            ScriptedHumanTcpSupport.Scenario scenario, DanDanDesktop desktop,
            ScriptedHumanTcpSupport remote) throws Exception {
        int oldGameViewId = scenario.metamorphoseAssignedConcessionGameViewId();
        if (!scenario.lifecycleScenario() || oldGameViewId < 0) return;
        Game nextGame = hosted.getGame();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        boolean bothContinued = scenario.nextGameDecisions().stream()
                .filter(receipt -> receipt.decision() == NextGameDecision.CONTINUE
                        && receipt.gameViewId() == oldGameViewId)
                .map(ScriptedHumanTcpSupport.NextGameDecisionReceipt::seat).distinct().count() == 2;
        while ((nextGame == null || nextGame.getView().getId() == oldGameViewId) && bothContinued
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
            nextGame = hosted.getGame();
        }
        if (nextGame == null || nextGame.getView().getId() == oldGameViewId) {
            if (bothContinued) {
                throw new AssertionError("Both actual CONTINUE decisions occurred but HostedMatch did not expose N+1");
            }
            return;
        }
        if (nextGame.isGameOver()) return;

        final Game nPlusOne = nextGame;
        LifecycleTrace cleanupTrace = new LifecycleTrace();
        cleanupTrace.attach(nPlusOne);
        hosted.setEndGameHook(() -> {
            if (hosted.getGame() == nPlusOne) cleanupTrace.recordEngineReturn(nPlusOne);
        });

        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        Set<Integer> physicalCardIds = Set.of();
        while (System.nanoTime() < deadline) {
            GameView hostView = desktop.hostGui.getGameView();
            GameView remoteView = remote.gui().getGameView();
            if (hosted.getGame() != nPlusOne) {
                throw new AssertionError("Hosted match replaced N+1 before its cleanup outcome");
            }
            if (!nPlusOne.isGameOver() && nPlusOne.getAge() != GameStage.BeforeMulligan
                    && hostView != null && hostView.getId() == nPlusOne.getView().getId()
                    && remoteView != null && remoteView.getId() == nPlusOne.getView().getId()) {
                physicalCardIds = nPlusOne.getCardsInGame().stream().map(Card::getId)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                if (physicalCardIds.size() == 80) break;
            }
            Thread.sleep(10);
        }
        Assert.assertEquals(physicalCardIds.size(), 80,
                "failure cleanup waited for the actual N+1 setup and both current GameViews");
        cleanupTrace.expectPhysicalCardIds(physicalCardIds);
        Assert.assertEquals(desktop.hostGui.getGameView().getId(), nPlusOne.getView().getId(),
                "failure cleanup observed the current host N+1 GameView");
        Assert.assertEquals(remote.gui().getGameView().getId(), nPlusOne.getView().getId(),
                "failure cleanup observed the current remote N+1 GameView");
        Assert.assertFalse(nPlusOne.isGameOver(), "N+1 is still active before its cleanup concession");
        remote.gui().concedeFromAssignedSeat();
        Assert.assertTrue(cleanupTrace.awaitOutcome(20, TimeUnit.SECONDS),
                "failure cleanup observed N+1's own actual outcome event");
        Assert.assertTrue(cleanupTrace.awaitEngineReturn(20, TimeUnit.SECONDS),
                "failure cleanup observed N+1 return through its own HostedMatch end hook");
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            ScriptedHumanTcpSupport.TerminalViewReceipt hostTerminal = desktop.hostGui.terminalViewReceipt();
            ScriptedHumanTcpSupport.TerminalViewReceipt remoteTerminal = remote.gui().terminalViewReceipt();
            if (hostTerminal != null && remoteTerminal != null
                    && hostTerminal.gameViewId() == nPlusOne.getView().getId()
                    && remoteTerminal.gameViewId() == nPlusOne.getView().getId()
                    && hostTerminal.gameOver() && remoteTerminal.gameOver()) {
                break;
            }
            Thread.sleep(10);
        }
        ScriptedHumanTcpSupport.TerminalViewReceipt hostTerminal = desktop.hostGui.terminalViewReceipt();
        ScriptedHumanTcpSupport.TerminalViewReceipt remoteTerminal = remote.gui().terminalViewReceipt();
        Assert.assertNotNull(hostTerminal, "failure cleanup received the host's actual N+1 terminal view");
        Assert.assertNotNull(remoteTerminal, "failure cleanup received the remote's actual N+1 terminal view");
        Assert.assertEquals(hostTerminal.gameViewId(), nPlusOne.getView().getId());
        Assert.assertEquals(remoteTerminal.gameViewId(), nPlusOne.getView().getId());
        Assert.assertTrue(hostTerminal.gameOver() && remoteTerminal.gameOver(),
                "both N+1 terminal views were published before engine game-view cleanup");
        hosted.endCurrentGame();
    }

    private static void awaitLifecycleCondition(ScriptedHumanTcpSupport.Scenario scenario,
                                                java.util.function.BooleanSupplier condition, int seconds,
                                                String description) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            assertNoFailure(scenario);
            if (condition.getAsBoolean()) return;
            Thread.sleep(10);
        }
        assertNoFailure(scenario);
        Assert.assertTrue(condition.getAsBoolean(), description);
    }

    private static void awaitLifecycleCheckpoint(ScriptedHumanTcpSupport.Scenario scenario,
            java.util.function.BooleanSupplier condition, int seconds, Game oldGame, HostedMatch hosted,
            ScriptedHumanTcpSupport.ScriptedGui hostGui, ScriptedHumanTcpSupport.ScriptedGui remoteGui,
            boolean deltaSync, String description) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            assertLifecycleCheckpointNoFailure(scenario, hosted, hostGui, remoteGui);
            if (condition.getAsBoolean()) return;
            Thread.sleep(10);
        }
        assertLifecycleCheckpointNoFailure(scenario, hosted, hostGui, remoteGui);
        if (condition.getAsBoolean()) return;
        Game current = hosted.getGame();
        GameView hostView = hostGui.getGameView();
        GameView remoteView = remoteGui.getGameView();
        ScriptedHumanTcpSupport.LifecyclePriorityReceipt priority = scenario.lifecyclePriorityReceipt();
        List<String> keepGameIds = scenario.lifecycleKeepReceipts().stream()
                .map(keep -> "seat" + keep.seat() + "=" + keep.gameViewId() + "#" + keep.callbackOrdinal())
                .limit(4).toList();
        long hostAfterEnd = afterGameEndOrdinal(hostGui.captures(), 1);
        long remoteAfterEnd = afterGameEndOrdinal(remoteGui.captures(), 1);
        throw new AssertionError(description + "; lifecycleCheckpointDiagnostic={delta=" + deltaSync
                + ",oldGameViewId=" + oldGame.getView().getId()
                + ",hostedGameViewId=" + (current == null ? -1 : current.getView().getId())
                + ",hostedAge=" + (current == null ? "null" : current.getAge())
                + ",hostedGameOver=" + (current != null && current.isGameOver())
                + ",hostedTurn=" + (current == null ? -1 : current.getPhaseHandler().getTurn())
                + ",hostedPhase=" + (current == null ? "null" : current.getPhaseHandler().getPhase())
                + ",hostAppliedViewId=" + (hostView == null ? -1 : hostView.getId())
                + ",remoteAppliedViewId=" + (remoteView == null ? -1 : remoteView.getId())
                + ",hostOpenViewId=" + hostGui.ownPlayerViewId()
                + ",remoteOpenViewId=" + remoteGui.ownPlayerViewId()
                + ",checkpointArmed=" + scenario.lifecycleCheckpointArmed()
                + ",priorityHeld=" + scenario.postKeepPriorityHeld()
                + ",keepGameIds=" + keepGameIds
                + ",priority=" + (priority == null ? "none" : "view=" + priority.gameViewId()
                        + "/seat=" + priority.seat() + "/turn=" + priority.turn()
                        + "/phase=" + priority.phase() + "/ordinal=" + priority.callbackOrdinal())
                + ",hostViewTail=" + lifecycleViewTail(hostGui, hostAfterEnd)
                + ",remoteViewTail=" + lifecycleViewTail(remoteGui, remoteAfterEnd) + "}");
    }

    private static List<String> lifecycleViewTail(ScriptedHumanTcpSupport.ScriptedGui gui,
            long afterGameEndOrdinal) {
        List<ScriptedHumanTcpSupport.GameViewReceipt> receipts = gui.gameViewReceipts().stream()
                .filter(receipt -> receipt.callbackOrdinal() > afterGameEndOrdinal).toList();
        int first = Math.max(0, receipts.size() - 6);
        return receipts.subList(first, receipts.size()).stream().map(receipt -> {
            List<Integer> handSizes = receipt.zonesByPlayer().values().stream()
                    .map(zones -> zones.getOrDefault(ZoneType.Hand, List.of()).size()).toList();
            return receipt.callback() + "#" + receipt.callbackOrdinal() + "(incoming="
                    + receipt.incomingGameViewId() + ",applied=" + receipt.gameViewId()
                    + ",state=" + receipt.stateBearing() + ",handSizes=" + handSizes + ")";
        }).toList();
    }

    private static void assertMetamorphoseTcpCase(boolean deltaSync, boolean acceptHandEntry,
            ScriptedHumanTcpSupport.Scenario scenario, DanDanDesktop desktop,
            ScriptedHumanTcpSupport remote, HostedMatch match, FServerManager server) throws Exception {
        ScriptedHumanTcpSupport.PostKeepReceipt before = scenario.postKeepReceipt();
        Assert.assertNotNull(before, "Metamorphose starts from the actual post-Keep held priority");
        MetamorphoseSetup setup = desktop.metamorphoseSetup;
        Assert.assertNotNull(setup, "post-opening setup retained exact official card IDs");
        Assert.assertEquals(scenario.keepOpeningReceipts().size(), 2,
                "both seats retained their own real seven-card opening before the setup hook changed zones");
        for (int seat = 0; seat < 2; seat++) {
            final int expectedSeat = seat;
            ScriptedHumanTcpSupport.KeepOpeningReceipt keep = scenario.keepOpeningReceipts().stream()
                    .filter(receipt -> receipt.seat() == expectedSeat).findFirst().orElse(null);
            Assert.assertNotNull(keep, "seat " + seat + " made a real Keep choice");
            Assert.assertEquals(keep.handIds().size(), 7, "seat " + seat + " Keep applied to its seven-card opening");
            Assert.assertEquals(keep.chooserViewId(), scenario.playerViewIdForSeat(seat));
            int openingIslandId = seat == 0 ? desktop.metamorphoseSetup.hostOpeningIslandId()
                    : desktop.metamorphoseSetup.remoteHandCardId();
            Assert.assertTrue(keep.handIds().contains(openingIslandId),
                    "seat " + seat + " actually kept the controlled opening Island");
        }
        Assert.assertEquals(before.playerViewIdsBySeat().get(0), desktop.hostGui.ownPlayerViewId());
        Assert.assertEquals(before.playerViewIdsBySeat().get(1), remote.gui().ownPlayerViewId());
        Assert.assertTrue(before.priorityCallbackOrdinal() > 0);
        Assert.assertTrue(before.heldTurn() == 1);
        Assert.assertEquals(before.heldPhase(), PhaseType.UNTAP,
                "the start-game hook exposes the host's actual turn 1 untap priority");
        Assert.assertEquals(before.prioritySeat(), 0, "actual Play/Draw actions make host seat 0 the starter");
        Assert.assertEquals(before.heldTurnPlayerViewId(), desktop.hostGui.ownPlayerViewId());
        List<ScriptedHumanTcpSupport.StarterButtonReceipt> starterButtons = scenario.starterButtonReceipts();
        Assert.assertEquals(starterButtons.size(), 1, "the actual coin-toss winner made one Play/Draw choice");
        Assert.assertEquals(scenario.starterPromptReceipts().size(), 1,
                "the actual Play/Draw choice followed its coin-toss prompt");
        ScriptedHumanTcpSupport.StarterButtonReceipt starter = starterButtons.get(0);
        Assert.assertEquals(starter.chooserViewId(), scenario.playerViewIdForSeat(starter.seat()));
        Assert.assertEquals(starter.chosePlay(), starter.seat() == 0,
                "seat 0 chose Play or seat 1 chose Draw to start the host");
        Assert.assertTrue(starter.playEnabled() && starter.drawEnabled());
        Assert.assertTrue(before.heldPriorityZonesByPlayer().get(remote.gui().ownPlayerViewId())
                        .get(ZoneType.Battlefield).contains(scenario.metamorphoseTargetCardId()),
                "actual remote callback shows the A-owned target under B's battlefield before casting");

        Assert.assertEquals(setup.hostViewId(), desktop.hostGui.ownPlayerViewId());
        Assert.assertEquals(setup.remoteViewId(), remote.gui().ownPlayerViewId());
        Assert.assertEquals(setup.hostManaIds().size(), 2);
        Assert.assertEquals(setup.hostManaIds().stream().distinct().count(), 2L);
        Game liveGame = match.getGame();
        Assert.assertNotNull(liveGame);
        Player host = liveGame.getPlayers().get(0);
        Player recipient = liveGame.getPlayers().get(1);
        Card spell = liveGame.findById(setup.spellId());
        Card target = liveGame.findById(setup.targetId());
        Card selected = liveGame.findById(setup.remoteHandCardId());
        Assert.assertSame(spell.getZone(), host.getZone(ZoneType.Hand));
        Assert.assertSame(target.getOwner(), host, "fixture target remains A-owned despite B control");
        Assert.assertSame(target.getController(), recipient);
        Assert.assertSame(target.getZone(), recipient.getZone(ZoneType.Battlefield));
        Assert.assertSame(selected.getOwner(), recipient);
        Assert.assertSame(selected.getZone(), recipient.getZone(ZoneType.Hand));
        for (int manaId : setup.hostManaIds()) {
            Card mana = liveGame.findById(manaId);
            Assert.assertSame(mana.getOwner(), host);
            Assert.assertSame(mana.getController(), host);
            Assert.assertSame(mana.getZone(), host.getZone(ZoneType.Battlefield));
            Assert.assertFalse(mana.isTapped());
        }

        ScriptedHumanTcpSupport.Capture initial = before.stateCapture();
        Assert.assertTrue(remote.gui().captures().stream().anyMatch(c -> c.callbackOrdinal() == initial.callbackOrdinal()
                        && c.sequence() == initial.sequence()),
                "setup state comes from the retained remote callback");
        if (deltaSync) {
            Assert.assertEquals(initial.callback(), "applyDelta");
            Assert.assertTrue(initial.stateBearing(), "setup arrived in a populated delta");
            Assert.assertTrue(initial.sequence() >= 0);
        } else {
            Assert.assertEquals(initial.callback(), "setGameView");
            GameView full = deserializeGameView(initial.serializedArguments());
            Assert.assertTrue(full.isDanDan());
            Assert.assertTrue(zoneSnapshot(full).get(remote.gui().ownPlayerViewId()).get(ZoneType.Battlefield)
                    .contains(setup.targetId()));
            Assert.assertEquals(before.appliedZonesByPlayer(), initial.zonesByPlayer());
        }

        ScriptedHumanTcpSupport.ScriptedGui held = before.prioritySeat() == 0 ? desktop.hostGui : remote.gui();
        held.releaseHeldPostKeepPriority();
        assertNoFailure(scenario);
        Assert.assertTrue(scenario.awaitMetamorphoseFirstStackPriority(30, TimeUnit.SECONDS),
                "host received the real first priority for the cast spell before it was passed");
        assertNoFailure(scenario);
        List<ScriptedHumanTcpSupport.MetamorphoseStackPriorityReceipt> firstStackReceipts =
                scenario.metamorphoseStackPriorities();
        Assert.assertEquals(firstStackReceipts.size(), 1,
                "the first-stack latch came from a retained valid actual priority receipt");
        Assert.assertEquals(firstStackReceipts.get(0).seat(), 0);
        Assert.assertEquals(firstStackReceipts.get(0).chooserViewId(), setup.hostViewId());
        Assert.assertEquals(firstStackReceipts.get(0).sourceCardId(), setup.spellId());
        Assert.assertEquals(firstStackReceipts.get(0).targetCardIds(), List.of(setup.targetId()));
        List<ScriptedHumanTcpSupport.MetamorphoseAbilityReceipt> abilityChoices = scenario.metamorphoseAbilityReceipts();
        Assert.assertEquals(abilityChoices.size(), 2,
                "normal casting records its initial ability choice and the engine's additional-cost selection");
        Assert.assertEquals(abilityChoices.stream().map(ScriptedHumanTcpSupport.MetamorphoseAbilityReceipt::role).toList(),
                List.of("initial cast selection", "additional-cost selection"));
        Assert.assertEquals(abilityChoices.get(0).seat(), 0);
        Assert.assertEquals(abilityChoices.get(1).seat(), 0);
        Assert.assertEquals(abilityChoices.get(0).chooserViewId(), setup.hostViewId());
        Assert.assertEquals(abilityChoices.get(1).chooserViewId(), setup.hostViewId());
        Assert.assertEquals(abilityChoices.get(0).cardId(), setup.spellId());
        Assert.assertEquals(abilityChoices.get(1).cardId(), setup.spellId());
        Assert.assertTrue(abilityChoices.get(1).callbackOrdinal() > abilityChoices.get(0).callbackOrdinal());
        Assert.assertEquals(abilityChoices.get(1).abilityFacts(), abilityChoices.get(0).abilityFacts(),
                "the engine's one additional-cost callback still offered the same sole playable spell ability");
        List<ScriptedHumanTcpSupport.ActionReceipt> phaseAdvances = scenario.actions().stream()
                .filter(a -> a.decision() == ScriptedHumanTcpSupport.Decision.ADVANCE_METAMORPHOSE_UNTAP).toList();
        Assert.assertEquals(phaseAdvances.size(), 2, "both actual UNTAP priorities were explicitly passed");
        Assert.assertEquals(phaseAdvances.stream().map(ScriptedHumanTcpSupport.ActionReceipt::seat).toList(),
                List.of(0, 1), "the active host and then the remote each passed real UNTAP priority");
        Assert.assertEquals(scenario.metamorphoseUntapPasses(), 2);
        List<ScriptedHumanTcpSupport.ActionReceipt> upkeepAdvances = scenario.actions().stream()
                .filter(a -> a.decision() == ScriptedHumanTcpSupport.Decision.ADVANCE_METAMORPHOSE_UPKEEP).toList();
        Assert.assertEquals(upkeepAdvances.size(), 2, "both actual UPKEEP priorities were explicitly passed");
        Assert.assertEquals(upkeepAdvances.stream().map(ScriptedHumanTcpSupport.ActionReceipt::seat).toList(),
                List.of(0, 1), "the active host and then the remote each passed real UPKEEP priority");
        Assert.assertEquals(liveGame.getStack().size(), 1);
        SpellAbility actualSpell = liveGame.getStack().peekAbility();
        Assert.assertNotNull(actualSpell);
        Assert.assertEquals(actualSpell.getHostCard().getId(), setup.spellId());
        List<Mana> paidMana = actualSpell.getPayingMana();
        Assert.assertEquals(paidMana.size(), 2, "the actual spell paid exactly two mana for its 1U cost");
        List<Integer> paidSourceIds = paidMana.stream()
                .map(Mana::getSourceCard).map(Card::getId).toList();
        Assert.assertEquals(new HashSet<>(paidSourceIds), new HashSet<>(setup.hostManaIds()),
                "the resolved spell recorded its actual payment from both distinct Islands");
        Assert.assertEquals(actualSpell.getPayingManaAbilities().size(), 2);
        Assert.assertEquals(actualSpell.getPayingManaAbilities().stream()
                .map(SpellAbility::getHostCard).map(Card::getId).collect(java.util.stream.Collectors.toSet()),
                new HashSet<>(setup.hostManaIds()),
                "the spell retained the real mana abilities that paid its 1U cost");
        desktop.hostGui.releaseMetamorphoseFirstStackPriority(firstStackReceipts.get(0));
        Assert.assertTrue(scenario.awaitMetamorphoseResolved(60, TimeUnit.SECONDS),
                "the actual B choice and normal stack resolution reached a received full/delta state callback");
        assertNoFailure(scenario);
        ScriptedHumanTcpSupport.MetamorphoseResolutionReceipt resolved = scenario.metamorphoseResolution();
        Assert.assertNotNull(resolved);
        Assert.assertEquals(resolved.receiverSeat(), 1);
        Assert.assertEquals(resolved.hostCardId(), setup.spellId());
        Assert.assertEquals(resolved.targetCardId(), setup.targetId());
        Assert.assertEquals(resolved.handCardId(), setup.remoteHandCardId());
        Card returnedTarget = liveGame.findById(setup.targetId());
        Assert.assertSame(returnedTarget.getOwner(), host);
        Assert.assertSame(returnedTarget.getZone(), host.getZone(ZoneType.Library));
        for (int manaId : setup.hostManaIds()) {
            Assert.assertTrue(liveGame.findById(manaId).isTapped(),
                    "actual normal payment tapped its distinct Island source " + manaId);
        }
        Assert.assertEquals(resolved.libraryIds().get(0).intValue(), setup.targetId(),
                "Metamorphose returned its A-owned/B-controlled target to the common library top");
        Assert.assertTrue(resolved.hostGraveyardIds().contains(setup.spellId()),
                "the normally cast Metamorphose resolved into the shared graveyard");
        Assert.assertEquals(resolved.callback(), deltaSync ? "applyDelta" : "setGameView");
        if (deltaSync) Assert.assertTrue(remote.gui().captures().stream().anyMatch(c ->
                c.callbackOrdinal() == resolved.callbackOrdinal() && c.sequence() == resolved.sequence() && c.stateBearing()));
        else Assert.assertTrue(remote.gui().captures().stream().anyMatch(c ->
                c.callbackOrdinal() == resolved.callbackOrdinal() && c.sequence() == resolved.sequence()));
        List<ScriptedHumanTcpSupport.MetamorphoseStackPriorityReceipt> stackPriorities = scenario.metamorphoseStackPriorities();
        Assert.assertEquals(stackPriorities.size(), 2,
                "both assigned seats passed priority once for the one normal Metamorphose spell");
        for (int seat = 0; seat < 2; seat++) {
            ScriptedHumanTcpSupport.MetamorphoseStackPriorityReceipt priority = stackPriorities.get(seat);
            Assert.assertEquals(priority.seat(), seat);
            Assert.assertEquals(priority.chooserViewId(), scenario.playerViewIdForSeat(seat));
            Assert.assertTrue(priority.callbackOrdinal() > 0);
            Assert.assertEquals(priority.sourceCardId(), setup.spellId());
            Assert.assertEquals(priority.activatorViewId(), setup.hostViewId());
            Assert.assertEquals(priority.targetCardIds(), List.of(setup.targetId()));
        }
        Assert.assertEquals(stackPriorities.get(0).stackItemId(), stackPriorities.get(1).stackItemId(),
                "both priority callbacks refer to the same stack item");

        List<ScriptedHumanTcpSupport.ActionReceipt> actions = scenario.actions();
        Assert.assertEquals(actions.stream().filter(a -> a.decision()
                == ScriptedHumanTcpSupport.Decision.ADVANCE_METAMORPHOSE_UPKEEP).count(), 2L);
        Assert.assertEquals(actions.stream().filter(a -> a.decision() == ScriptedHumanTcpSupport.Decision.CAST_METAMORPHOSE
                && a.seat() == 0 && a.chooserViewId() == setup.hostViewId() && a.cardId() == setup.spellId()).count(), 1L);
        List<ScriptedHumanTcpSupport.ActionReceipt> mana = actions.stream()
                .filter(a -> a.decision() == ScriptedHumanTcpSupport.Decision.PAY_METAMORPHOSE).toList();
        Assert.assertEquals(mana.size(), 2, "the real 1U cost used two separate Island sources");
        Assert.assertEquals(mana.stream().map(ScriptedHumanTcpSupport.ActionReceipt::cardId).collect(java.util.stream.Collectors.toSet()),
                new HashSet<>(setup.hostManaIds()));
        List<ScriptedHumanTcpSupport.MetamorphoseManaAbilityReceipt> manaAbilities = scenario.metamorphoseManaAbilityReceipts();
        Assert.assertEquals(manaAbilities.size(), 2,
                "each selected sole Island ability received its one real additional-cost callback");
        Assert.assertEquals(manaAbilities.stream().map(ScriptedHumanTcpSupport.MetamorphoseManaAbilityReceipt::sourceCardId).toList(),
                setup.hostManaIds());
        Assert.assertTrue(manaAbilities.stream().allMatch(r -> r.seat() == 0
                && r.chooserViewId() == setup.hostViewId() && r.abilityFacts().size() == 1));
        Assert.assertTrue(manaAbilities.get(1).callbackOrdinal() > manaAbilities.get(0).callbackOrdinal());
        Assert.assertEquals(actions.stream().filter(a -> a.decision() == ScriptedHumanTcpSupport.Decision.CHOOSE_METAMORPHOSE_HAND
                && a.seat() == 1 && a.chooserViewId() == setup.remoteViewId()
                && a.cardId() == setup.remoteHandCardId()).count(), acceptHandEntry ? 1L : 0L);
        List<ScriptedHumanTcpSupport.HumanChoiceReceipt> choices = scenario.humanChoiceReceipts();
        Assert.assertEquals(choices.stream().filter(r -> r.stage().equals("Metamorphose optional question")
                && r.seat() == 1 && r.chooserViewId() == setup.remoteViewId()
                && r.message().equals(scenario.metamorphoseOptionalMessage())).count(), 1L);
        Assert.assertEquals(choices.stream().filter(r -> r.stage().equals(acceptHandEntry
                ? "Metamorphose optional yes" : "Metamorphose optional no")
                && r.seat() == 1 && r.chooserViewId() == setup.remoteViewId()).count(), 1L);
        List<ScriptedHumanTcpSupport.SelectablesReceipt> offered = scenario.selectablesReceipts();
        Assert.assertEquals(offered.stream().filter(r -> r.stage().equals("Metamorphose target") && r.seat() == 0).count(), 1L);
        Assert.assertEquals(offered.stream().filter(r -> r.stage().equals("Metamorphose private hand") && r.seat() == 1).count(),
                acceptHandEntry ? 1L : 0L);
        Assert.assertTrue(offered.stream().filter(r -> r.stage().equals("Metamorphose private hand"))
                .allMatch(r -> r.chooserViewId() == setup.remoteViewId() && r.cardIds().contains(setup.remoteHandCardId())));
        List<ScriptedHumanTcpSupport.MetamorphoseInputButtonsReceipt> inputButtons = scenario.metamorphoseInputButtons();
        Assert.assertEquals(inputButtons.stream().filter(r -> r.prompt().equals("target")).count(), 1L);
        Assert.assertEquals(inputButtons.stream().filter(r -> r.prompt().equals("private hand")).count(),
                acceptHandEntry ? 1L : 0L);
        Assert.assertTrue(inputButtons.stream().allMatch(r -> r.promptOrdinal() > 0
                && r.buttonsOrdinal() > r.promptOrdinal()
                && r.okLabel().equalsIgnoreCase(Localizer.getInstance().getMessage("lblOK"))
                && r.cancelLabel().equalsIgnoreCase(Localizer.getInstance().getMessage("lblCancel"))
                && !r.okEnabled() && r.cancelEnabled()));
        Assert.assertTrue(inputButtons.stream().filter(r -> r.prompt().equals("target"))
                .allMatch(r -> r.seat() == 0 && r.chooserViewId() == setup.hostViewId()));
        Assert.assertTrue(inputButtons.stream().filter(r -> r.prompt().equals("private hand"))
                .allMatch(r -> r.seat() == 1 && r.chooserViewId() == setup.remoteViewId()));
        Assert.assertEquals(resolved.recipientBattlefieldIds().contains(setup.remoteHandCardId()), acceptHandEntry);
        Assert.assertEquals(resolved.recipientHandIds().contains(setup.remoteHandCardId()), !acceptHandEntry);
        assertMetamorphosePhysicalZones(resolved.zonesByPlayer(), setup.hostViewId(), setup.remoteViewId(),
                before.heldPriorityZonesByPlayer(), resolved, setup, acceptHandEntry);
        Assert.assertTrue(scenario.awaitMetamorphosePostResolutionPriority(30, TimeUnit.SECONDS),
                "host received and retained its real empty-stack MAIN1 prompt with the resolved card zones");
        ScriptedHumanTcpSupport.MetamorphosePostResolutionPriority hostPrompt =
                scenario.metamorphosePostResolutionPriority();
        assertNoFailure(scenario);
        Assert.assertNotNull(hostPrompt);
        Assert.assertTrue(hostPrompt.callbackOrdinal() > 0);
        Map<ZoneType, List<Integer>> hostPromptZones = hostPrompt.zonesByPlayer().get(setup.hostViewId());
        Assert.assertEquals(hostPromptZones.get(ZoneType.Library), resolved.libraryIds());
        Assert.assertEquals(hostPromptZones.get(ZoneType.Graveyard), resolved.hostGraveyardIds());
        Card finalChosen = liveGame.findById(setup.remoteHandCardId());
        Assert.assertSame(finalChosen.getOwner(), recipient, "B continues to own the selected card after the effect");
        Assert.assertSame(finalChosen.getController(), recipient, "B remains its actual controller");
        Assert.assertSame(finalChosen.getZone(), acceptHandEntry
                ? recipient.getZone(ZoneType.Battlefield) : recipient.getZone(ZoneType.Hand));
        desktop.hostGui.concedeMetamorphoseAfterAssertions();
        Assert.assertTrue(desktop.hostGui.awaitActualTerminal(30, TimeUnit.SECONDS),
                "host observed actual terminal completion after its normal controller concession");
        Assert.assertTrue(remote.gui().awaitActualTerminal(30, TimeUnit.SECONDS),
                "remote observed actual terminal completion after the host concession");
        assertNoFailure(scenario);
        Assert.assertTrue(match.getGameView().isGameOver(), "the actual host concession ended the game");
        Assert.assertEquals(scenario.actions().stream().filter(a -> a.decision() == ScriptedHumanTcpSupport.Decision.CONCEDE
                && a.seat() == 0 && a.chooserViewId() == setup.hostViewId()).count(), 1L);
        Assert.assertEquals(server.getTotalSendErrors(), 0, "no TCP send errors before terminal cleanup");
        assertReadinessGateSummary(scenario, "metamorphose");
    }

    private static PrivacyTerminalReceipt assertTellingTimeFengrafPrivacyCase(boolean deltaSync,
            ScriptedHumanTcpSupport.Scenario scenario, DanDanDesktop desktop,
            ScriptedHumanTcpSupport remote, HostedMatch match,
            ScriptedHumanTcpSupport.ReconnectObservation observation,
            boolean privacyShuffleHistoryScenario) throws Exception {
        PrivacySetup setup = desktop.privacySetup;
        int tellingTimeSeat = scenario.privacyTellingTimeSeat();
        int fengrafSeat = scenario.privacyFengrafSeat();
        int chooserViewId = scenario.privacyViewIdForSeat(tellingTimeSeat);
        int otherViewId = scenario.privacyViewIdForSeat(1 - tellingTimeSeat);
        int fengrafActorViewId = scenario.privacyViewIdForSeat(fengrafSeat);
        Map<Integer, Set<Integer>> gameNKnownIds = Map.of(0, new HashSet<>(), 1, new HashSet<>());
        for (ScriptedHumanTcpSupport.MulliganChoiceReceipt choice : scenario.mulliganChoices()) {
            gameNKnownIds.get(choice.seat()).addAll(choice.handIds());
        }
        for (ScriptedHumanTcpSupport.FreeRevealReceipt reveal : scenario.freeReveals()) {
            gameNKnownIds.get(reveal.receivingSeat()).addAll(reveal.cardIds());
        }
        for (ScriptedHumanTcpSupport.KeepOpeningReceipt keep : scenario.keepOpeningReceipts()) {
            gameNKnownIds.get(keep.seat()).addAll(keep.handIds());
        }
        for (ScriptedHumanTcpSupport.LondonBottomDispatchReceipt bottom : scenario.londonBottomDispatches()) {
            gameNKnownIds.get(bottom.seat()).addAll(bottom.chooserHandIds());
        }
        Assert.assertNotNull(setup, "post-Keep setup retained actual official card roles");
        if (scenario.privacyMulliganPrefixScenario()) {
            ScriptedHumanTcpSupport.PrivacyMulliganBoundaryReceipt boundary =
                    scenario.privacyMulliganBoundaryReceipt();
            Assert.assertNotNull(boundary, "the same Game retained pre- and post-setup mulligan boundaries");
            Assert.assertSame(boundary.game(), match.getGame());
            Assert.assertSame(boundary.game(), scenario.privacyGame());
            Assert.assertEquals(boundary.gameViewId(), match.getGame().getView().getId());
            Assert.assertEquals(boundary.preSetupZones().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Hand).size(), 7);
            Assert.assertEquals(boundary.preSetupZones().get(remote.gui().ownPlayerViewId())
                    .get(ZoneType.Hand).size(), 6);
            Assert.assertEquals(boundary.preSetupZones().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Library).size(), 67);
            Assert.assertTrue(boundary.preSetupZones().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Library).contains(scenario.londonBottomDispatches().get(0).cardId()));
            Assert.assertEquals(scenario.mulliganChoices().size(), 2);
            ScriptedHumanTcpSupport.MulliganChoiceReceipt free = scenario.freeMulliganChoice();
            ScriptedHumanTcpSupport.MulliganChoiceReceipt regular = scenario.regularMulliganChoice();
            ScriptedHumanTcpSupport.LondonBottomDispatchReceipt bottom = scenario.londonBottomDispatches().get(0);
            Assert.assertTrue(bottom.appliedHandCallbackOrdinal() < 0
                            || bottom.appliedHandCallbackOrdinal() > bottom.promptOrdinal(),
                    "a deferred London selection uses a post-button applied seven-card Hand receipt");
            System.out.println("DANDAN_LONDON_APPLIED_HAND deferred="
                    + (bottom.appliedHandCallbackOrdinal() > 0 ? 1 : 0)
                    + " (zero means this conditional deferral was unexercised)");
            Assert.assertEquals(free.seat(), 0, "the actual host owns the controlled free mulligan");
            Assert.assertEquals(regular.seat(), 1, "the actual remote owns the controlled regular London mulligan");
            Assert.assertTrue(free.receiptOrder() < regular.receiptOrder()
                    && regular.receiptOrder() < bottom.receiptOrder(),
                    "scenario receipt order joins real FREE, REGULAR, and London actions without comparing endpoint clocks");
            Assert.assertEquals(scenario.freeReveals().size(), 1);
            long revealOrder = scenario.freeReveals().get(0).receiptOrder();
            Assert.assertTrue(regular.receiptOrder() < revealOrder && revealOrder < bottom.receiptOrder(),
                    "actual free-hand reveal is causally between both choices and London bottom");
            Assert.assertEquals(free.gameViewId(), boundary.gameViewId());
            Assert.assertEquals(regular.gameViewId(), boundary.gameViewId());
            Assert.assertEquals(scenario.freeReveals().get(0).gameViewId(), boundary.gameViewId());
            Assert.assertEquals(bottom.gameViewId(), boundary.gameViewId());
            Assert.assertEquals(scenario.keepOpeningReceipts().size(), 2);
            Assert.assertTrue(scenario.keepOpeningReceipts().stream().allMatch(keep ->
                    keep.gameViewId() == boundary.gameViewId() && keep.receiptOrder() > bottom.receiptOrder()));
            Game compositeGame = boundary.game();
            Set<String> allSeenNames = new HashSet<>();
            for (int knownId : scenario.privacyMulliganKnownCardIds()) {
                Card known = compositeGame.findById(knownId);
                Assert.assertNotNull(known, "historically seen card ID remains resolvable in the retained Game");
                allSeenNames.add(known.getName());
            }
            for (int privateTopId : setup.privateTopIds()) {
                Card privateTop = compositeGame.findById(privateTopId);
                Assert.assertNotNull(privateTop, "declared hidden-top ID remains resolvable in the retained Game");
                Assert.assertFalse(allSeenNames.contains(privateTop.getName()),
                        "the declared hidden top excludes every identity exposed in the actual mulligan history");
            }
            Assert.assertTrue(Collections.disjoint(setup.privateTopIds(), scenario.privacyMulliganKnownCardIds()),
                    "the declared hidden top contains no physical card ID seen during the actual mulligan history");
            Set<Integer> preSetupPhysicalIds = boundary.preSetupZones().values().stream()
                    .flatMap(zones -> zones.values().stream()).flatMap(List::stream)
                    .collect(java.util.stream.Collectors.toSet());
            Set<Integer> postSetupPhysicalIds = boundary.postSetupZones().values().stream()
                    .flatMap(zones -> zones.values().stream()).flatMap(List::stream)
                    .collect(java.util.stream.Collectors.toSet());
            Assert.assertEquals(preSetupPhysicalIds.size(), 80,
                    "pre-setup boundary contains the actual full physical card pool");
            Assert.assertEquals(postSetupPhysicalIds, preSetupPhysicalIds,
                    "declared setup moves conserve exact physical identities across the same Game boundary");
            Assert.assertEquals(boundary.postSetupZones().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Hand).size(), 6);
            Assert.assertEquals(boundary.postSetupZones().get(remote.gui().ownPlayerViewId())
                    .get(ZoneType.Hand).size(), 7);
        }
        ScriptedHumanTcpSupport.PostKeepReceipt before = scenario.postKeepReceipt();
        Assert.assertNotNull(before, "privacy sequence starts from an actual held turn-1 prompt");
        Assert.assertEquals(before.prioritySeat(), 0);
        Assert.assertEquals(before.heldPhase(), PhaseType.UNTAP);
        Assert.assertEquals(before.heldTurnPlayerViewId(), desktop.hostGui.ownPlayerViewId());
        Assert.assertEquals(before.playerViewIdsBySeat().get(0), desktop.hostGui.ownPlayerViewId());
        Assert.assertEquals(before.playerViewIdsBySeat().get(1), remote.gui().ownPlayerViewId());
        Assert.assertNotEquals(desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId());
        Assert.assertEquals(scenario.keepOpeningReceipts().size(), 2);
        for (int seat = 0; seat < 2; seat++) {
            final int assignedSeat = seat;
            ScriptedHumanTcpSupport.KeepOpeningReceipt keep = scenario.keepOpeningReceipts().stream()
                    .filter(receipt -> receipt.seat() == assignedSeat).findFirst().orElse(null);
            Assert.assertNotNull(keep);
            Assert.assertEquals(keep.handIds().size(), scenario.privacyMulliganPrefixScenario() ? (seat == 0 ? 7 : 6) : 7,
                    "the actual Keep receipt records the post-London pre-setup hand size");
            Assert.assertEquals(keep.chooserViewId(), scenario.playerViewIdForSeat(seat));
        }
        List<ScriptedHumanTcpSupport.StarterButtonReceipt> starterButtons = scenario.starterButtonReceipts();
        Assert.assertEquals(starterButtons.size(), 1);
        Assert.assertEquals(scenario.starterPromptReceipts().size(), 1);
        Assert.assertTrue(starterButtons.get(0).chosePlay() == (starterButtons.get(0).seat() == 0));

        Map<Integer, Map<ZoneType, List<Integer>>> opening = before.heldPriorityZonesByPlayer();
        assertPrivacyPhysicalState(opening, desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId(),
                setup.physicalCardIds());
        Assert.assertEquals(opening.get(desktop.hostGui.ownPlayerViewId()).get(ZoneType.Library), setup.initialLibraryIds());
        Assert.assertEquals(opening.get(remote.gui().ownPlayerViewId()).get(ZoneType.Library), setup.initialLibraryIds());
        Assert.assertTrue(opening.get(chooserViewId).get(ZoneType.Hand).contains(setup.tellingTimeId()));
        Assert.assertTrue(opening.get(otherViewId).get(ZoneType.Hand).contains(setup.negativeHandId()));
        Assert.assertEquals(opening.get(desktop.hostGui.ownPlayerViewId()).get(ZoneType.Library).subList(0, 3), setup.privateTopIds());
        GameView hostPostKeepView = desktop.hostGui.getGameView();
        GameView remotePostKeepView = remote.gui().getGameView();
        Assert.assertEquals(hostPostKeepView.getId(), remotePostKeepView.getId());
        Assert.assertEquals(hostPostKeepView.getId(), scenario.privacyGame().getView().getId());
        List<Integer> hostPostKeepOwnHand = findPlayerView(hostPostKeepView, desktop.hostGui.ownPlayerViewId())
                .getHand().stream().map(CardView::getId).toList();
        List<Integer> remotePostKeepOwnHand = findPlayerView(remotePostKeepView, remote.gui().ownPlayerViewId())
                .getHand().stream().map(CardView::getId).toList();
        Assert.assertEquals(hostPostKeepOwnHand,
                opening.get(desktop.hostGui.ownPlayerViewId()).get(ZoneType.Hand));
        Assert.assertEquals(remotePostKeepOwnHand,
                opening.get(remote.gui().ownPlayerViewId()).get(ZoneType.Hand));
        gameNKnownIds.get(0).addAll(hostPostKeepOwnHand);
        gameNKnownIds.get(1).addAll(remotePostKeepOwnHand);
        for (Map<ZoneType, List<Integer>> zones : opening.values()) {
            for (ZoneType publicZone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                List<Integer> publicIds = zones.get(publicZone);
                if (publicIds != null) {
                    gameNKnownIds.get(0).addAll(publicIds);
                    gameNKnownIds.get(1).addAll(publicIds);
                }
            }
        }
        assertPrivacyEndpointVisibility(desktop.hostGui, remote.gui(), setup, false);
        assertPrivacyModeReceipt(deltaSync, before.stateCapture(), remote.gui());

        Assert.assertFalse(observation.overflowed());
        Assert.assertNull(observation.failure());
        Assert.assertTrue(observation.inboundByteCount() > 0, "the actual client channel supplied inbound bytes");
        Assert.assertEquals(observation.rawOwnedByteCount(), observation.inboundByteCount());
        int inboundBytesAtOpening = observation.inboundByteCount();
        int heldGameViewId = desktop.hostGui.getGameView().getId();
        Assert.assertTrue(heldGameViewId == remote.gui().getGameView().getId(),
                "both assigned endpoints begin this held-choice sample on the same game");

        desktop.hostGui.releasePrivacyInitialPriority();
        assertNoFailure(scenario);
        if (tellingTimeSeat == 0) {
            try {
                boolean localChoiceHeld = scenario.awaitPrivacyHostChoiceHeld(45, TimeUnit.SECONDS);
                assertNoFailure(scenario);
                Assert.assertTrue(localChoiceHeld,
                        "the assigned local chooser reached its actual first Telling Time choice");
                assertNoFailure(scenario);
                assertHeldLocalTellingTimePrivacySnapshot(scenario, desktop.hostGui, remote.gui(), setup,
                        heldGameViewId);
            } finally {
                scenario.releasePrivacyHostChoice();
            }
            assertNoFailure(scenario);
        }
        Assert.assertTrue(scenario.awaitPrivacyTellingTimeResolved(45, TimeUnit.SECONDS),
                "normal turn progression, cast, choices, and Telling Time resolution reached a remote applied-state receipt");
        assertNoFailure(scenario);
        Assert.assertTrue(scenario.awaitPrivacyFengrafActorPriority(30, TimeUnit.SECONDS),
                "the assigned Fengraf actor received its actual empty-stack priority after Telling Time");
        assertNoFailure(scenario);
        ScriptedHumanTcpSupport.PrivacyStateReceipt tellingState = scenario.privacyTellingTimeState();
        Assert.assertNotNull(tellingState);
        if (deltaSync && tellingTimeSeat == 1 && fengrafSeat == 0) {
            ScriptedHumanTcpSupport.PrivacyPaymentReceipt tellingPaymentForEvents = scenario.privacyPaymentReceipts().stream()
                    .filter(payment -> payment.sourceCardId() == setup.tellingTimeId()).findFirst().orElse(null);
            Assert.assertNotNull(tellingPaymentForEvents);
            List<ScriptedHumanTcpSupport.PrivacyChoiceReceipt> eventChoices = scenario.privacyChoices();
            Assert.assertEquals(eventChoices.size(), 2);
            Assert.assertTrue(eventChoices.get(0).callbackOrdinal() < eventChoices.get(1).callbackOrdinal(),
                    "the initial selected-card choice precedes the remaining-library ordering choice");
            awaitLifecycleCondition(scenario,
                    () -> hasPrivacyWrappedResolution(remote.gui().captures(), setup.tellingTimeId(),
                            tellingPaymentForEvents.abilityId()),
                    15, "remote endpoint captured Telling Time's exact wrapped resolution event");
            assertPrivacyWrappedEventFacts(remote.gui().captures(), before.stateCapture().callbackOrdinal(),
                    eventChoices.get(0).callbackOrdinal(), eventChoices.get(0).callbackOrdinal(),
                    eventChoices.get(1).callbackOrdinal(), eventChoices.get(1).callbackOrdinal(),
                    setup.privateTopIds().get(0), chooserViewId,
                    ZoneType.Library, ZoneType.Hand, setup.tellingTimeId(), tellingPaymentForEvents.abilityId(), null,
                    "Look at the top three cards of your library. Put one of those cards into your hand, one on top of your library, and one on the bottom of your library.",
                    setup.negativeHandName());
        }
        Assert.assertTrue(tellingState.callbackOrdinal() > scenario.privacyTellingTimeStartDrawOrdinal(),
                "Telling Time applied-state receipt follows the actual remote MAIN1 no-draw baseline");
        assertPrivacyModeReceipt(deltaSync, tellingState, remote.gui());
        Map<Integer, Map<ZoneType, List<Integer>>> afterTelling = tellingState.zonesByPlayer();
        assertPrivacyPhysicalState(afterTelling, desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId(),
                setup.physicalCardIds());
        List<Integer> expectedLibrary = new ArrayList<>();
        expectedLibrary.add(setup.privateTopIds().get(1));
        expectedLibrary.addAll(setup.initialLibraryIds().subList(3, setup.initialLibraryIds().size()));
        expectedLibrary.add(setup.privateTopIds().get(2));
        Assert.assertEquals(afterTelling.get(desktop.hostGui.ownPlayerViewId()).get(ZoneType.Library), expectedLibrary,
                "Telling Time put its selected second card on top and its remaining card on the bottom");
        Assert.assertEquals(afterTelling.get(remote.gui().ownPlayerViewId()).get(ZoneType.Library), expectedLibrary);
        Game liveGame = match.getGame();
        Card liveTellingTime = liveGame.findById(setup.tellingTimeId());
        Card livePrivateChoice = liveGame.findById(setup.privateTopIds().get(0));
        Assert.assertNotNull(liveTellingTime);
        Assert.assertNotNull(livePrivateChoice);
        Assert.assertSame(liveTellingTime.getOwner(), liveGame.getPlayers().get(tellingTimeSeat));
        Assert.assertSame(liveTellingTime.getZone(), liveGame.getPlayers().get(tellingTimeSeat).getZone(ZoneType.Graveyard),
                "the paid instant completed its actual stack movement");
        Assert.assertSame(livePrivateChoice.getOwner(), liveGame.getPlayers().get(tellingTimeSeat));
        Assert.assertSame(livePrivateChoice.getController(), liveGame.getPlayers().get(tellingTimeSeat));
        Assert.assertSame(livePrivateChoice.getZone(), liveGame.getPlayers().get(tellingTimeSeat).getZone(ZoneType.Hand),
                "the first Telling Time choice actually entered its recipient's hand");
        Assert.assertTrue(afterTelling.get(chooserViewId).get(ZoneType.Hand)
                .contains(setup.privateTopIds().get(0)), "remote player's actual hand acquired its private selected card");
        Assert.assertFalse(afterTelling.get(otherViewId).get(ZoneType.Hand)
                .contains(setup.privateTopIds().get(0)), "the selected private card did not enter the host hand");
        Assert.assertTrue(afterTelling.get(otherViewId).get(ZoneType.Hand)
                .contains(setup.negativeHandId()), "host retained its distinct negative hand sentinel");
        Assert.assertEquals(tellingState.drawnThisTurnByPlayer().get(desktop.hostGui.ownPlayerViewId()),
                scenario.privacyTellingTimeStartDrawCounts().get(desktop.hostGui.ownPlayerViewId()),
                "the host's actual drawn-this-turn count did not change during Telling Time resolution");
        Assert.assertEquals(tellingState.drawnThisTurnByPlayer().get(remote.gui().ownPlayerViewId()),
                scenario.privacyTellingTimeStartDrawCounts().get(remote.gui().ownPlayerViewId()),
                "the remote's actual drawn-this-turn count did not change during Telling Time resolution");
        Assert.assertTrue(scenario.privacyTellingTimeStartDrawCounts().size() == 2);
        Assert.assertTrue(scenario.privacyTellingTimeStartDrawOrdinal() > 0);
        GameView remoteAfterTellingView = remote.gui().getGameView();
        GameView hostAfterTellingView = desktop.hostGui.getGameView();
        GameView chooserAppliedView = tellingTimeSeat == 0 ? hostAfterTellingView : remoteAfterTellingView;
        GameView otherAppliedView = tellingTimeSeat == 0 ? remoteAfterTellingView : hostAfterTellingView;
        PlayerView chooserPlayerView = findPlayerView(chooserAppliedView, chooserViewId);
        PlayerView otherPlayerView = findPlayerView(otherAppliedView, otherViewId);
        Assert.assertNotNull(chooserPlayerView);
        Assert.assertNotNull(otherPlayerView);
        CardView chooserOwnChoice = findCardView(chooserAppliedView, setup.privateTopIds().get(0));
        Assert.assertNotNull(chooserOwnChoice, "assigned chooser's selected Telling Time card is in its own applied view");
        Assert.assertEquals(chooserOwnChoice.getOwner().getId(), chooserPlayerView.getId());
        Assert.assertEquals(chooserOwnChoice.getController().getId(), chooserPlayerView.getId());
        Assert.assertTrue(chooserOwnChoice.canBeShownTo(chooserPlayerView),
                "assigned chooser positively sees its selected card in its actual own-hand view");
        CardView otherOwnNegative = findCardView(otherAppliedView, setup.negativeHandId());
        Assert.assertNotNull(otherOwnNegative, "other endpoint's negative sentinel remains in its own applied view");
        Assert.assertEquals(otherOwnNegative.getOwner().getId(), otherPlayerView.getId());
        Assert.assertEquals(otherOwnNegative.getController().getId(), otherPlayerView.getId());
        Assert.assertTrue(otherOwnNegative.canBeShownTo(otherPlayerView),
                "other endpoint positively sees its own negative hand sentinel");
        for (int cardId : setup.privateTopIds().subList(1, 3)) {
            List<Boolean> visibility = tellingState.privateTopVisibilityByRemote().get(cardId);
            Assert.assertNotNull(visibility, "remote applied-state callback retained the card's look permission facts");
            Assert.assertFalse(visibility.get(0), "Telling Time's temporary look permission ended for the unselected card");
            Assert.assertFalse(visibility.get(1), "unselected library card is no longer showable to remote");
        }
        for (int cardId : setup.privateTopIds()) {
            CardView otherCard = findCardView(otherAppliedView, cardId);
            Assert.assertNotNull(otherCard, "other endpoint retains physical ID " + cardId);
            Assert.assertFalse(otherCard.mayPlayerLook(otherPlayerView),
                    "other endpoint has no temporary Telling Time look permission");
            Assert.assertFalse(otherCard.canBeShownTo(otherPlayerView),
                    "other endpoint does not show a hidden Telling Time card");
        }
        assertPrivacyEndpointVisibility(desktop.hostGui, remote.gui(), setup, true);
        CardView chooserNegative = findCardView(chooserAppliedView, setup.negativeHandId());
        Assert.assertNotNull(chooserNegative);
        Assert.assertFalse(chooserNegative.canBeShownTo(chooserPlayerView),
                "chooser's applied view does not show the other endpoint's negative hand card");

        List<ScriptedHumanTcpSupport.PrivacyChoiceReceipt> choices = scenario.privacyChoices();
        Assert.assertEquals(choices.size(), 2, "Telling Time prompted the remote chooser for both real choices");
        Assert.assertEquals(choices.get(0).seat(), tellingTimeSeat);
        Assert.assertEquals(choices.get(1).seat(), tellingTimeSeat);
        Assert.assertEquals(choices.stream().map(ScriptedHumanTcpSupport.PrivacyChoiceReceipt::chooserViewId).toList(),
                List.of(chooserViewId, chooserViewId));
        Assert.assertEquals(choices.get(0).cardIds(), setup.privateTopIds());
        Assert.assertEquals(choices.get(0).selectedCardId(), setup.privateTopIds().get(0));
        Assert.assertEquals(choices.get(1).cardIds(),
                List.of(setup.privateTopIds().get(2), setup.privateTopIds().get(1)));
        Assert.assertEquals(choices.get(1).selectedCardId(), setup.privateTopIds().get(1));
        Assert.assertTrue(choices.get(1).callbackOrdinal() > choices.get(0).callbackOrdinal());
        Assert.assertTrue(choices.stream().allMatch(choice -> (choice.eventId() >= 0) == (tellingTimeSeat == 1)
                && choice.method().equals("chooseEntitiesForEffect") && choice.minimum() == 1 && choice.maximum() == 1));
        for (ScriptedHumanTcpSupport.PrivacyChoiceReceipt choice : choices) {
            Assert.assertEquals(choice.mayLookByChooser().size(), choice.cardIds().size());
            Assert.assertEquals(choice.canShowToChooser().size(), choice.cardIds().size());
            Assert.assertTrue(choice.mayLookByChooser().stream().allMatch(Boolean::booleanValue),
                    "the actual assigned private chooser was temporarily allowed to look at each candidate");
            Assert.assertTrue(choice.canShowToChooser().stream().allMatch(Boolean::booleanValue),
                    "each private candidate was actually showable to the assigned chooser");
            Assert.assertTrue(choice.mayLookByOther().stream().noneMatch(Boolean::booleanValue));
            Assert.assertTrue(choice.canShowToOther().stream().noneMatch(Boolean::booleanValue));
            Assert.assertEquals(choice.hostMayLook().size(), choice.cardIds().size());
            Assert.assertEquals(choice.hostCanShow().size(), choice.cardIds().size());
            Assert.assertTrue(choice.hostMayLook().stream().allMatch(value -> value == (tellingTimeSeat == 0)));
            Assert.assertTrue(choice.hostCanShow().stream().allMatch(value -> value == (tellingTimeSeat == 0)));
        }
        Assert.assertEquals(desktop.hostGui.captures().stream().filter(c ->
                "chooseEntitiesForEffect".equals(c.callback())).count(), tellingTimeSeat == 0 ? 2L : 0L);
        Assert.assertEquals(remote.gui().captures().stream().filter(c ->
                "chooseEntitiesForEffect".equals(c.callback())).count(), tellingTimeSeat == 1 ? 2L : 0L);
        List<ScriptedHumanTcpSupport.PrivacyPromptReceipt> privacyPrompts = scenario.privacyPrompts();
        Assert.assertTrue(privacyPrompts.stream().filter(prompt -> prompt.seat() == (1 - tellingTimeSeat))
                        .noneMatch(prompt -> setup.privateTopNames().stream().anyMatch(prompt.message()::contains)),
                "nonchooser prompts did not mention private Telling Time cards");
        Assert.assertTrue(choices.stream().noneMatch(choice -> choice.message().contains(setup.negativeHandName())),
                "the two actual remote Telling Time choice prompts did not mention the host's negative hand card");
        Assert.assertFalse(setup.chooserKnownNames().contains(setup.negativeHandName()),
                "negative hand name is distinct from names already known to the chooser");
        long hostStartPrompt = scenario.postKeepReceipt().priorityCallbackOrdinal();
        long remoteStartPrompt = before.stateCapture().callbackOrdinal();
        assertPrivacyPromptWindow(scenario.privacyPrompts(), 0, desktop.hostGui.ownPlayerViewId(), hostStartPrompt,
                scenario.privacyTellingTimeFenceOrdinal(), tellingTimeSeat == 0
                        ? Set.of(setup.negativeHandName()) : setup.privateTopNames());
        assertPrivacyPromptWindow(scenario.privacyPrompts(), 1, remote.gui().ownPlayerViewId(), remoteStartPrompt,
                tellingState.callbackOrdinal(), tellingTimeSeat == 1
                        ? Set.of(setup.negativeHandName()) : setup.privateTopNames());

        (fengrafSeat == 0 ? desktop.hostGui : remote.gui()).activatePrivacyFengraf();
        assertNoFailure(scenario);
        Assert.assertTrue(scenario.awaitPrivacyFengrafReveal(30, TimeUnit.SECONDS),
                "Fengraf made its actual public getChoices callback to the other assigned seat");
        assertNoFailure(scenario);
        Assert.assertTrue(scenario.awaitPrivacyFengrafState(45, TimeUnit.SECONDS),
                "Fengraf's normally resolved public card reached the remote applied-state receipt");
        assertNoFailure(scenario);
        Assert.assertTrue(scenario.awaitPrivacyFengrafResolved(30, TimeUnit.SECONDS),
                "the active player received its real empty-stack priority after Fengraf resolved");
        assertNoFailure(scenario);
        ScriptedHumanTcpSupport.PrivacyStateReceipt fengrafState = scenario.privacyFengrafState();
        Assert.assertNotNull(fengrafState);
        if (deltaSync && tellingTimeSeat == 1 && fengrafSeat == 0) {
            ScriptedHumanTcpSupport.PrivacyPaymentReceipt fengrafPaymentForEvents = scenario.privacyPaymentReceipts().stream()
                    .filter(payment -> payment.sourceCardId() == setup.fengrafId()).findFirst().orElse(null);
            Assert.assertNotNull(fengrafPaymentForEvents);
            Assert.assertEquals(fengrafPaymentForEvents.sacrificedCardIds(), List.of(setup.fengrafId()),
                    "Fengraf retained its actual self-sacrifice cost");
            awaitLifecycleCondition(scenario,
                    () -> hasPrivacyWrappedResolution(remote.gui().captures(), setup.fengrafId(),
                            fengrafPaymentForEvents.abilityId()),
                    15, "remote endpoint captured Fengraf's exact wrapped resolution event");
            assertPrivacyWrappedEventFacts(remote.gui().captures(), tellingState.callbackOrdinal(),
                    scenario.privacyRevealOrdinal(), tellingState.callbackOrdinal(),
                    scenario.privacyRevealOrdinal(), scenario.privacyRevealOrdinal(),
                    setup.publicCreatureId(), fengrafActorViewId,
                    ZoneType.Graveyard, ZoneType.Hand, setup.fengrafId(), fengrafPaymentForEvents.abilityId(),
                    fengrafPaymentForEvents.sacrificedCardIds().get(0),
                    "from their graveyard to their hand.", setup.negativeHandName());
        }
        assertPrivacyModeReceipt(deltaSync, fengrafState, remote.gui());
        assertPrivacyPhysicalState(fengrafState.zonesByPlayer(), desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId(),
                setup.physicalCardIds());
        Assert.assertEquals(fengrafState.zonesByPlayer().get(desktop.hostGui.ownPlayerViewId()).get(ZoneType.Library), expectedLibrary);
        Assert.assertTrue(fengrafState.zonesByPlayer().get(fengrafActorViewId).get(ZoneType.Hand)
                .contains(setup.publicCreatureId()));
        assertPrivacyEndpointVisibility(desktop.hostGui, remote.gui(), setup, true);
        ScriptedHumanTcpSupport.ScriptedGui fengrafActorGui = fengrafSeat == 0 ? desktop.hostGui : remote.gui();
        List<Boolean> fengrafActorHand = fengrafActorGui.visibilityFacts(List.of(setup.publicCreatureId()),
                fengrafActorGui.ownPlayerViewId()).get(setup.publicCreatureId());
        Assert.assertTrue(fengrafActorHand.get(1), "Fengraf actor endpoint positively sees the acquired card in its own hand");
        Assert.assertFalse(fengrafActorHand.get(0), "acquisition left no temporary look flag on the public card");
        Assert.assertFalse(remote.gui().visibilityFacts(List.of(setup.publicCreatureId()), remote.gui().ownPlayerViewId())
                .get(setup.publicCreatureId()).get(0), "public reveal did not leave temporary look permission after acquisition");
        Assert.assertFalse(fengrafState.zonesByPlayer().get(otherViewId).get(ZoneType.Graveyard)
                .contains(setup.publicCreatureId()));
        Card liveFengraf = liveGame.findById(setup.fengrafId());
        Card livePublicCreature = liveGame.findById(setup.publicCreatureId());
        Assert.assertNotNull(liveFengraf);
        Assert.assertSame(liveFengraf.getOwner(), liveGame.getPlayers().get(fengrafSeat));
        Assert.assertSame(liveFengraf.getController(), liveGame.getPlayers().get(fengrafSeat));
        Assert.assertSame(liveFengraf.getZone(), liveGame.getPlayers().get(fengrafSeat).getZone(ZoneType.Graveyard));
        Assert.assertNotNull(livePublicCreature);
        Assert.assertSame(livePublicCreature.getOwner(), liveGame.getPlayers().get(fengrafSeat));
        Assert.assertSame(livePublicCreature.getController(), liveGame.getPlayers().get(fengrafSeat));
        Assert.assertSame(livePublicCreature.getZone(), liveGame.getPlayers().get(fengrafSeat).getZone(ZoneType.Hand));

        List<ScriptedHumanTcpSupport.PrivacyRevealReceipt> reveals = scenario.privacyReveals();
        Assert.assertEquals(reveals.size(), 1);
        ScriptedHumanTcpSupport.PrivacyRevealReceipt reveal = reveals.get(0);
        Assert.assertEquals(reveal.seat(), 1 - fengrafSeat);
        Assert.assertEquals(reveal.chooserViewId(), scenario.privacyViewIdForSeat(1 - fengrafSeat));
        ScriptedHumanTcpSupport.FreeRevealReceipt openingReveal = scenario.freeReveals().isEmpty()
                ? null : scenario.freeReveals().get(0);
        int expectedHostGetChoices = (openingReveal != null && openingReveal.receivingSeat() == 0 ? 1 : 0)
                + (reveal.seat() == 0 ? 1 : 0);
        int expectedRemoteGetChoices = (openingReveal != null && openingReveal.receivingSeat() == 1 ? 1 : 0)
                + (reveal.seat() == 1 ? 1 : 0);
        List<ScriptedHumanTcpSupport.Capture> hostGetChoices = desktop.hostGui.captures().stream()
                .filter(capture -> "getChoices".equals(capture.callback())).toList();
        List<ScriptedHumanTcpSupport.Capture> remoteGetChoices = remote.gui().captures().stream()
                .filter(capture -> "getChoices".equals(capture.callback())).toList();
        Assert.assertEquals(hostGetChoices.size(), expectedHostGetChoices,
                "host received exactly the typed actual FREE and Fengraf reveal callbacks assigned to it");
        Assert.assertEquals(remoteGetChoices.size(), expectedRemoteGetChoices,
                "remote received exactly the typed actual FREE and Fengraf reveal callbacks assigned to it");
        if (scenario.privacyMulliganPrefixScenario()) {
            ScriptedHumanTcpSupport.PrivacyMulliganBoundaryReceipt boundary =
                    scenario.privacyMulliganBoundaryReceipt();
            ScriptedHumanTcpSupport.PostKeepReceipt compositePostKeep = scenario.postKeepReceipt();
            Assert.assertNotNull(openingReveal);
            Assert.assertNotNull(boundary);
            Assert.assertNotNull(compositePostKeep);
            Assert.assertNotNull(scenario.privacyObservation(), "composite run retained its actual endpoint wire observation");
            Assert.assertEquals(openingReveal.receivingSeat(), 1);
            Assert.assertEquals(openingReveal.gameViewId(), boundary.gameViewId());
            Assert.assertTrue(openingReveal.eventId() >= 0);
            Assert.assertNotEquals(openingReveal.eventId(), reveal.eventId(),
                    "opening FREE and Fengraf callbacks came from distinct wire events");
            Assert.assertTrue(openingReveal.getChoicesCallbackOrdinal() < reveal.callbackOrdinal(),
                    "same remote endpoint received opening FREE reveal before the later Fengraf reveal");
            Assert.assertEquals(remoteGetChoices.stream()
                            .map(ScriptedHumanTcpSupport.Capture::callbackOrdinal).toList(),
                    List.of(openingReveal.getChoicesCallbackOrdinal(), reveal.callbackOrdinal()),
                    "the two remote callbacks correlate one-to-one with distinct typed opening and Fengraf receipts");
            List<ScriptedHumanTcpSupport.PreHandlerFact> getChoicesFacts = scenario.privacyObservation().facts().stream()
                    .filter(fact -> "getChoices".equals(fact.method())).toList();
            Assert.assertEquals(getChoicesFacts.stream().map(ScriptedHumanTcpSupport.PreHandlerFact::eventId).toList(),
                    List.of(openingReveal.eventId(), reveal.eventId()),
                    "the endpoint's two wire requests correlate in order to the two actual callback receipts");
            Assert.assertTrue(openingReveal.getChoicesCallbackOrdinal() <
                            scenario.londonBottomDispatches().get(0).promptOrdinal(),
                    "opening FREE callback preceded the actual same-endpoint London prompt");
            ScriptedHumanTcpSupport.KeepOpeningReceipt remoteKeep = scenario.keepOpeningReceipts().stream()
                    .filter(keep -> keep.seat() == 1).findFirst().orElseThrow();
            Assert.assertTrue(remote.gui().captures().stream()
                            .anyMatch(capture -> capture == compositePostKeep.stateCapture()),
                    "the retained post-Keep state receipt is the actual remote endpoint callback");
            Assert.assertTrue(scenario.londonBottomDispatches().get(0).zeroCardPromptOrdinal()
                            < remoteKeep.callbackOrdinal(),
                    "the actual remote Keep callback followed its London bottom response before setup");
            Assert.assertTrue(remoteKeep.callbackOrdinal() < compositePostKeep.stateCapture().callbackOrdinal()
                            && compositePostKeep.stateCapture().callbackOrdinal() < reveal.callbackOrdinal()
                            && reveal.callbackOrdinal() < fengrafState.callbackOrdinal(),
                    "opening Keep/setup and later Fengraf callback remain ordered on the remote endpoint");
        }
        Assert.assertEquals(reveal.cardIds(), List.of(setup.publicCreatureId()));
        gameNKnownIds.get(0).addAll(reveal.cardIds());
        gameNKnownIds.get(1).addAll(reveal.cardIds());
        Assert.assertEquals(reveal.message(), scenario.privacyExpectedRevealMessage());
        Assert.assertEquals(reveal.minimum(), -1);
        Assert.assertEquals(reveal.maximum(), -1);
        Assert.assertEquals(reveal.eventId() >= 0, reveal.seat() == 1);
        Assert.assertTrue(reveal.canShowToChooser().stream().allMatch(Boolean::booleanValue),
                "the actual public graveyard reveal is showable to its assigned remote recipient: "
                        + reveal.diagnostic());
        Assert.assertTrue(reveal.mayLookByChooser().stream().allMatch(Boolean::booleanValue),
                "the public reveal temporarily allowed its assigned remote chooser to look at F: "
                        + reveal.diagnostic());
        if (reveal.seat() == 1) {
            Assert.assertTrue(reveal.callbackOrdinal() > tellingState.callbackOrdinal()
                            && reveal.callbackOrdinal() < fengrafState.callbackOrdinal(),
                    "the remote reveal callback is fenced by remote applied-state receipts on the same endpoint");
        } else {
            assertHostPrivacyRevealEndpointOrder(desktop.hostGui, setup, expectedLibrary,
                    desktop.hostGui.getGameView().getId(), scenario.privacyTellingTimeFenceOrdinal(),
                    scenario.privacyFengrafFenceOrdinal(), reveal.callbackOrdinal());
        }
        Assert.assertTrue(scenario.privacyRevealOrdinal() == reveal.callbackOrdinal());
        Assert.assertTrue(fengrafState.stackSize() == 0,
                "the selected Fengraf result is an actual post-reveal empty-stack state");
        List<Boolean> fengrafRemoteCleanup = fengrafState.privateTopVisibilityByRemote().get(setup.publicCreatureId());
        Assert.assertNotNull(fengrafRemoteCleanup,
                "the actual post-reveal state copied F's remote visibility permissions");
        Assert.assertFalse(fengrafRemoteCleanup.get(0), "F's temporary look permission cleared after the real reveal returned");
        Assert.assertEquals(fengrafRemoteCleanup.get(1), fengrafSeat == 1,
                "the acquired card is showable to B exactly when B owns its actual new hand entry");
        Assert.assertTrue(scenario.privacyFengrafConfirmOrdinal() > scenario.privacyConfirmButtonsOrdinal());
        if (fengrafSeat == 1) {
            ScriptedHumanTcpSupport.ClientProtocolSendReceipt confirmationSend = remote.clientProtocolSendReceipt();
            Assert.assertNull(remote.clientProtocolObservationFailure());
            Assert.assertNotNull(confirmationSend, "the remote confirmation produced one observed client send");
            Assert.assertEquals(confirmationSend.method(), ProtocolMethod.selectButtonOk);
            Assert.assertEquals(confirmationSend.gameViewId(), remote.gui().getGameView().getId());
            Assert.assertEquals(confirmationSend.promptCallbackOrdinal(), scenario.privacyFengrafConfirmOrdinal());
            System.out.printf("FENGRAF_POST_SEND_REPAINT delta=%s eventId=%d gameViewId=%d confirmOrdinal=%d buttonsOrdinal=%d repaintButtonsOrdinal=%d repaintPromptOrdinal=%d postSendButtonsAccepted=%s completedPostSendPair=%s%n",
                    deltaSync, confirmationSend.eventId(), confirmationSend.gameViewId(),
                    scenario.privacyFengrafConfirmOrdinal(), scenario.privacyConfirmButtonsOrdinal(),
                    scenario.privacyConfirmRepaintOrdinal(), scenario.privacyConfirmRepaintPromptOrdinal(),
                    scenario.privacyPostSendRepaintButtonsAccepted(),
                    scenario.privacyPostSendRepaintAccepted());
        }
        Assert.assertEquals(scenario.privacyFengrafConfirmMessage(), scenario.privacyExpectedSacrificeMessage());
        Assert.assertTrue(scenario.privacyPrompts().stream().anyMatch(prompt -> prompt.seat() == fengrafSeat
                && prompt.chooserViewId() == fengrafActorViewId
                && prompt.message().equals(scenario.privacyExpectedSacrificeMessage())));
        Set<String> hostFengrafForbiddenNames = tellingTimeSeat == 0
                ? Set.of(setup.negativeHandName()) : setup.privateTopNames();
        Set<String> remoteFengrafForbiddenNames = tellingTimeSeat == 1
                ? Set.of(setup.negativeHandName()) : setup.privateTopNames();
        assertPrivacyPromptWindow(scenario.privacyPrompts(), 0, desktop.hostGui.ownPlayerViewId(),
                scenario.privacyTellingTimeFenceOrdinal(),
                scenario.privacyFengrafFenceOrdinal(), hostFengrafForbiddenNames);
        assertPrivacyPromptWindow(scenario.privacyPrompts(), 1, remote.gui().ownPlayerViewId(),
                tellingState.callbackOrdinal(), fengrafState.callbackOrdinal(), remoteFengrafForbiddenNames);

        List<ScriptedHumanTcpSupport.PrivacyStackViewReceipt> stacks = scenario.privacyStackViewReceipts();
        Assert.assertEquals(stacks.size(), 4, "each assigned seat received one priority callback for each real stack item");
        List<ScriptedHumanTcpSupport.PrivacyStackViewReceipt> tellingStack = stacks.stream()
                .filter(receipt -> receipt.sourceCardId() == setup.tellingTimeId()).toList();
        List<ScriptedHumanTcpSupport.PrivacyStackViewReceipt> fengrafStack = stacks.stream()
                .filter(receipt -> receipt.sourceCardId() == setup.fengrafId()).toList();
        Assert.assertEquals(tellingStack.size(), 2);
        Assert.assertEquals(fengrafStack.size(), 2);
        Assert.assertEquals(tellingStack.stream().map(ScriptedHumanTcpSupport.PrivacyStackViewReceipt::seat).toList(),
                List.of(tellingTimeSeat, 1 - tellingTimeSeat));
        Assert.assertEquals(fengrafStack.stream().map(ScriptedHumanTcpSupport.PrivacyStackViewReceipt::seat).toList(),
                List.of(fengrafSeat, 1 - fengrafSeat));
        Assert.assertTrue(tellingStack.stream().noneMatch(ScriptedHumanTcpSupport.PrivacyStackViewReceipt::ability));
        Assert.assertTrue(fengrafStack.stream().allMatch(ScriptedHumanTcpSupport.PrivacyStackViewReceipt::ability));
        for (ScriptedHumanTcpSupport.PrivacyStackViewReceipt receipt : tellingStack) {
            Assert.assertEquals(receipt.activatorViewId(), chooserViewId);
        }
        for (ScriptedHumanTcpSupport.PrivacyStackViewReceipt receipt : fengrafStack) {
            Assert.assertEquals(receipt.activatorViewId(), fengrafActorViewId);
        }
        List<ScriptedHumanTcpSupport.PrivacyPaymentReceipt> payments = scenario.privacyPaymentReceipts();
        Assert.assertEquals(payments.size(), 2, "host retained each actual live paid stack ability once");
        ScriptedHumanTcpSupport.PrivacyPaymentReceipt tellingPayment = payments.stream()
                .filter(receipt -> receipt.sourceCardId() == setup.tellingTimeId()).findFirst().orElse(null);
        ScriptedHumanTcpSupport.PrivacyPaymentReceipt fengrafPayment = payments.stream()
                .filter(receipt -> receipt.sourceCardId() == setup.fengrafId()).findFirst().orElse(null);
        Assert.assertNotNull(tellingPayment);
        Assert.assertNotNull(fengrafPayment);
        Assert.assertEquals(payments.stream().map(ScriptedHumanTcpSupport.PrivacyPaymentReceipt::sourceCardId).toList(),
                List.of(setup.tellingTimeId(), setup.fengrafId()));
        Assert.assertTrue(payments.stream().allMatch(payment -> stacks.stream().anyMatch(stack -> stack.seat() == 0
                && stack.sourceCardId() == payment.sourceCardId()
                && stack.callbackOrdinal() == payment.callbackOrdinal())));
        Assert.assertEquals(sortedIds(tellingPayment.payingManaSourceIds()), sortedIds(setup.remoteManaIds()));
        Assert.assertEquals(sortedIds(tellingPayment.payingAbilitySourceIds()), sortedIds(setup.remoteManaIds()));
        Assert.assertEquals(tellingPayment.payingManaSourceIds().size(), 2);
        Assert.assertTrue(tellingPayment.payingManaColors().stream()
                .anyMatch(color -> (color & MagicColor.BLUE) != 0), "actual Telling Time payment included blue mana");
        Assert.assertEquals(sortedIds(fengrafPayment.payingManaSourceIds()), sortedIds(setup.hostManaIds()));
        Assert.assertEquals(fengrafPayment.payingManaSourceIds().size(), 3);
        Assert.assertTrue(fengrafPayment.sacrificedCardIds().contains(setup.fengrafId()),
                "live ability's Sacrificed LKI retained the actual Fengraf source");
        Assert.assertEquals(fengrafPayment.sacrificedCardIds(), List.of(setup.fengrafId()));
        Assert.assertTrue(fengrafPayment.sourceTapped(),
                "the Sacrificed LKI was tapped when the real activation paid both costs");
        List<ScriptedHumanTcpSupport.PrivacyAbilityReceipt> abilities = scenario.privacyAbilities();
        List<ScriptedHumanTcpSupport.PrivacyAbilityReceipt> tellingAbilities = abilities.stream()
                .filter(receipt -> receipt.cardId() == setup.tellingTimeId()).toList();
        List<ScriptedHumanTcpSupport.PrivacyAbilityReceipt> fengrafAbilities = abilities.stream()
                .filter(receipt -> receipt.cardId() == setup.fengrafId()).toList();
        Assert.assertEquals(tellingAbilities.size(), 2);
        Assert.assertEquals(tellingAbilities.stream().map(ScriptedHumanTcpSupport.PrivacyAbilityReceipt::role).toList(),
                List.of("Telling Time initial spell choice", "Telling Time additional-cost choice"));
        Assert.assertTrue(tellingAbilities.stream().allMatch(receipt -> receipt.seat() == tellingTimeSeat
                && receipt.chooserViewId() == chooserViewId
                && (receipt.eventId() >= 0) == (tellingTimeSeat == 1)));
        Assert.assertTrue(tellingAbilities.get(1).callbackOrdinal() > tellingAbilities.get(0).callbackOrdinal());
        Assert.assertEquals(fengrafAbilities.size(), 2);
        Assert.assertEquals(fengrafAbilities.stream().map(ScriptedHumanTcpSupport.PrivacyAbilityReceipt::role).toList(),
                List.of("Haunted Fengraf activation ability", "Haunted Fengraf additional-cost choice"));
        Assert.assertTrue(fengrafAbilities.stream().allMatch(receipt -> receipt.seat() == fengrafSeat
                && receipt.chooserViewId() == fengrafActorViewId
                && receipt.abilityId() == setup.fengrafAbilityId()
                && (receipt.eventId() >= 0) == (fengrafSeat == 1)));
        Assert.assertTrue(fengrafAbilities.get(1).callbackOrdinal() > fengrafAbilities.get(0).callbackOrdinal());
        List<ScriptedHumanTcpSupport.PrivacyAbilityReceipt> manaAbilities = abilities.stream()
                .filter(receipt -> setup.remoteManaIds().contains(receipt.cardId())
                        || setup.hostManaIds().contains(receipt.cardId())).toList();
        Assert.assertEquals(manaAbilities.size(), 5,
                "the retained original Telling Time/Fengraf evidence has not been inflated by the later shuffle leg");
        Assert.assertEquals(manaAbilities.stream().map(ScriptedHumanTcpSupport.PrivacyAbilityReceipt::cardId)
                .collect(java.util.stream.Collectors.toSet()),
                java.util.stream.Stream.concat(setup.remoteManaIds().stream(), setup.hostManaIds().stream())
                        .collect(java.util.stream.Collectors.toSet()));
        Assert.assertTrue(manaAbilities.stream().allMatch(receipt -> receipt.seat() == 1
                ? receipt.eventId() >= 0 : receipt.eventId() == -1));
        Assert.assertTrue(abilities.stream().allMatch(receipt -> receipt.abilityId() >= 0));
        Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                == ScriptedHumanTcpSupport.Decision.PAY_TELLING_TIME).count(), 2L);
        Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                == ScriptedHumanTcpSupport.Decision.PAY_FENGRAF && Objects.equals(action.cardId(), setup.fengrafId())).count(), 1L);

        for (int manaId : setup.remoteManaIds()) Assert.assertTrue(liveGame.findById(manaId).isTapped());
        for (int manaId : setup.hostManaIds()) Assert.assertTrue(liveGame.findById(manaId).isTapped());

        if (privacyShuffleHistoryScenario) {
            Assert.assertTrue(setup.daysUndoingId() > 0,
                    "the first-leg fixture reserved the actual official Day's Undoing in its declared setup");
            desktop.hostGui.continuePrivacyShuffleHistory();
            Assert.assertTrue(scenario.awaitPrivacyShuffleHistoryT3CastHeld(90, TimeUnit.SECONDS),
                    "the actual host T3 MAIN1 Day's Undoing priority is retained pending remote discard proof");
            assertNoFailure(scenario);
            Assert.assertTrue(scenario.awaitPrivacyShuffleTurnThreeDraw(45, TimeUnit.SECONDS),
                    "the remote applied its same-game T3 MAIN1 state before the held host cast is released");
            assertNoFailure(scenario);
            int turnTwoDiscardId = scenario.privacyShuffleTurnTwoDiscardId();
            Assert.assertTrue(turnTwoDiscardId > 0, "the assigned remote controller selected one actual cleanup card");
            Assert.assertEquals(scenario.actions().stream().filter(action ->
                    action.decision() == ScriptedHumanTcpSupport.Decision.DISCARD_T2_CLEANUP
                            && action.seat() == 1 && action.chooserViewId() == remote.gui().ownPlayerViewId()
                            && Objects.equals(action.cardId(), turnTwoDiscardId)).count(), 1L,
                    "the assigned remote controller made exactly one legal cleanup discard");
            ScriptedHumanTcpSupport.PrivacyStateReceipt turnThreeDraw = scenario.privacyShuffleTurnThreeDraw();
            ScriptedHumanTcpSupport.PrivacyStateReceipt turnTwoDraw = scenario.privacyShuffleTurnTwoDraw();
            Assert.assertNotNull(turnTwoDraw,
                    "the remote's actual held turn-2 draw state is available for its own-hand knowledge history");
            Assert.assertEquals(turnTwoDraw.turn(), 2);
            Assert.assertEquals(turnTwoDraw.phase(), PhaseType.MAIN1);
            Assert.assertEquals(turnTwoDraw.turnPlayerViewId(), remote.gui().ownPlayerViewId());
            Assert.assertTrue(turnTwoDraw.drawnThisTurnByPlayer()
                    .getOrDefault(remote.gui().ownPlayerViewId(), 0) > 0);
            Assert.assertNotNull(turnThreeDraw,
                    "the remote applied its first same-game T3 MAIN1 state after the actual cleanup discard");
            Assert.assertEquals(turnThreeDraw.turn(), 3);
            Assert.assertEquals(turnThreeDraw.phase(), PhaseType.MAIN1);
            Assert.assertEquals(turnThreeDraw.turnPlayerViewId(), desktop.hostGui.ownPlayerViewId());
            Assert.assertEquals(turnThreeDraw.zonesByPlayer().get(remote.gui().ownPlayerViewId())
                    .get(ZoneType.Hand).size(), 7, "remote T3 view has its legal post-cleanup hand size");
            Assert.assertTrue(turnThreeDraw.zonesByPlayer().get(remote.gui().ownPlayerViewId())
                    .get(ZoneType.Graveyard).contains(turnTwoDiscardId),
                    "remote applied state carries the exact T2 discard into its graveyard");
            gameNKnownIds.get(1).addAll(turnTwoDraw.zonesByPlayer()
                    .get(remote.gui().ownPlayerViewId()).get(ZoneType.Hand));
            GameView hostAtT3 = desktop.hostGui.getGameView();
            Assert.assertEquals(hostAtT3.getId(), scenario.privacyShuffleGameViewId());
            Assert.assertEquals(hostAtT3.getTurn(), 3);
            Assert.assertEquals(hostAtT3.getPhase(), PhaseType.MAIN1);
            List<Integer> hostOwnT3Hand = findPlayerView(hostAtT3, desktop.hostGui.ownPlayerViewId())
                    .getHand().stream().map(CardView::getId).toList();
            Assert.assertEquals(hostOwnT3Hand, turnThreeDraw.zonesByPlayer()
                    .get(desktop.hostGui.ownPlayerViewId()).get(ZoneType.Hand));
            gameNKnownIds.get(0).addAll(hostOwnT3Hand);
            gameNKnownIds.get(0).add(turnTwoDiscardId);
            gameNKnownIds.get(1).add(turnTwoDiscardId);
            ScriptedHumanTcpSupport.GameViewReceipt remoteT3Receipt = remote.gui().gameViewReceipts().stream()
                    .filter(receipt -> receipt.callbackOrdinal() == turnThreeDraw.callbackOrdinal())
                    .findFirst().orElse(null);
            Assert.assertNotNull(remoteT3Receipt, "T3 state retains its exact applied GameView/tracker receipt");
            Assert.assertEquals(remoteT3Receipt.gameViewId(), scenario.privacyShuffleGameViewId());
            Assert.assertNotNull(remoteT3Receipt.trackerIdentity());
            GameView remoteCurrentAtT3 = remote.gui().getGameView();
            Assert.assertNotNull(remoteCurrentAtT3);
            Assert.assertEquals(remoteCurrentAtT3.getId(), scenario.privacyShuffleGameViewId());
            Assert.assertSame(remoteT3Receipt.trackerIdentity(), remoteCurrentAtT3.getTracker(),
                    "the applied T3 receipt is still the remote endpoint's current tracker");
            List<Integer> remoteOwnT3Hand = findPlayerView(remoteCurrentAtT3, remote.gui().ownPlayerViewId())
                    .getHand().stream().map(CardView::getId).toList();
            Assert.assertEquals(remoteOwnT3Hand, turnThreeDraw.zonesByPlayer()
                    .get(remote.gui().ownPlayerViewId()).get(ZoneType.Hand));
            gameNKnownIds.get(1).addAll(remoteOwnT3Hand);
            java.util.function.Predicate<ScriptedHumanTcpSupport.Capture> isDiscardMove = capture ->
                    "handleGameEvents".equals(capture.callback()) && capture.eventFacts().stream().anyMatch(fact ->
                            fact.eventClass().equals("forge.game.event.GameEventCardChangeZone")
                                    && fact.cardViewIds().contains(turnTwoDiscardId)
                                    && fact.zoneFacts().stream().anyMatch(zone -> zone.field().equals("from")
                                    && zone.zoneType() == ZoneType.Hand
                                    && Objects.equals(zone.playerViewId(), remote.gui().ownPlayerViewId()))
                                    && fact.zoneFacts().stream().anyMatch(zone -> zone.field().equals("to")
                                    && zone.zoneType() == ZoneType.Graveyard));
            awaitLifecycleCondition(scenario,
                    () -> remote.gui().captures().stream().anyMatch(isDiscardMove), 45,
                    "remote endpoint received the exact typed T2 discard movement event");
            assertNoFailure(scenario);
            List<ScriptedHumanTcpSupport.Capture> remoteCaptures = remote.gui().captures();
            ScriptedHumanTcpSupport.Capture discardMove = remoteCaptures.stream().filter(isDiscardMove)
                    .findFirst().orElse(null);
            Assert.assertNotNull(discardMove,
                    "remote received the exact typed Hand-to-Graveyard event for its actual T2 discard");
            Assert.assertTrue(discardMove.callbackOrdinal() > scenario.privacyShuffleTurnTwoDiscardOrdinal(),
                    "the received discard movement belongs to the remote applied T3 state window");
            if (discardMove.parentCallbackOrdinal() > 0) {
                Assert.assertTrue(discardMove.parentCallbackOrdinal() > scenario.privacyShuffleTurnTwoDiscardOrdinal());
                Assert.assertTrue(discardMove.parentCallbackOrdinal() <= turnThreeDraw.callbackOrdinal());
            }
            desktop.hostGui.continuePrivacyShuffleHistoryAfterDiscard(turnThreeDraw, remoteT3Receipt);
            assertNoFailure(scenario);
            awaitLifecycleCondition(scenario, () -> scenario.privacyAbilities().stream().anyMatch(ability ->
                    ability.cardId() == setup.daysUndoingId()
                            && "Day's Undoing actual host spell choice".equals(ability.role())), 30,
                    "the retained host priority selected Day's Undoing through its actual ability callback");
            assertNoFailure(scenario);
            awaitLifecycleCondition(scenario, () -> scenario.privacyStackViewReceipts().stream().anyMatch(stack ->
                    stack.sourceCardId() == setup.daysUndoingId()
                            && stack.activatorViewId() == desktop.hostGui.ownPlayerViewId() && !stack.ability())
                    && scenario.privacyPaymentReceipts().stream().anyMatch(payment ->
                            payment.sourceCardId() == setup.daysUndoingId()
                                    && payment.activatorViewId() == desktop.hostGui.ownPlayerViewId()), 30,
                    "the selected Day's Undoing reached the actual host stack with its paid ability");
            assertNoFailure(scenario);
            awaitLifecycleCondition(scenario, () -> {
                List<ScriptedHumanTcpSupport.PrivacyShuffleEventReceipt> events = scenario.privacyShuffleEvents();
                return events.stream().anyMatch(event -> event.playerViewId() == desktop.hostGui.ownPlayerViewId()
                                && event.turn() == 3 && event.phase() == PhaseType.MAIN1
                                && event.sourceCardId() == setup.daysUndoingId()
                                && event.castPriorityOrdinal() == scenario.privacyDaysUndoingCastOrdinal()
                                && event.paidStackCallbackOrdinal() > event.castPriorityOrdinal())
                        && events.stream().anyMatch(event -> event.playerViewId() == remote.gui().ownPlayerViewId()
                                && event.turn() == 3 && event.phase() == PhaseType.MAIN1
                                && event.sourceCardId() == setup.daysUndoingId()
                                && event.castPriorityOrdinal() == scenario.privacyDaysUndoingCastOrdinal()
                                && event.paidStackCallbackOrdinal() > event.castPriorityOrdinal());
            }, 45, "the paid Day's Undoing emitted a typed shuffle receipt for each assigned player");
            assertNoFailure(scenario);
            Assert.assertTrue(scenario.awaitPrivacyShuffleHistoryUpkeep(90, TimeUnit.SECONDS),
                    "remote applied and retained the exact turn-4 UPKEEP state before draw");
            assertNoFailure(scenario);
            ScriptedHumanTcpSupport.PrivacyStateReceipt afterShuffle = scenario.privacyShuffleHistoryState();
            Assert.assertNotNull(afterShuffle, "remote applied-view receipt captured the held turn-4 UPKEEP");
            Assert.assertTrue(scenario.awaitPrivacyShuffleHistoryPriorityHeld(30, TimeUnit.SECONDS),
                    "the remote's actual turn-4 UPKEEP priority callback is held before draw");
            assertNoFailure(scenario);
            Assert.assertEquals(afterShuffle.turn(), 4);
            Assert.assertEquals(afterShuffle.phase(), PhaseType.UPKEEP);
            Assert.assertEquals(afterShuffle.turnPlayerViewId(), remote.gui().ownPlayerViewId());
            Assert.assertEquals(afterShuffle.stackSize(), 0);
            Assert.assertNotNull(turnTwoDraw, "the remote completed its ordinary turn-2 draw before passing");
            Assert.assertNotNull(turnThreeDraw, "the host naturally untapped and completed its turn-3 draw before casting");
            assertPrivacyPhysicalState(afterShuffle.zonesByPlayer(), desktop.hostGui.ownPlayerViewId(),
                    remote.gui().ownPlayerViewId(), setup.physicalCardIds());
            Assert.assertEquals(afterShuffle.zonesByPlayer().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Hand).size(), 7, "Day's Undoing drew seven for the host before its normal turn draw");
            Assert.assertEquals(afterShuffle.zonesByPlayer().get(remote.gui().ownPlayerViewId())
                    .get(ZoneType.Hand).size(), 7, "Day's Undoing drew seven for the remote before its normal turn draw");
            Assert.assertTrue(afterShuffle.zonesByPlayer().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Exile).contains(setup.daysUndoingId()),
                    "the resolved Day's Undoing left the stack and is represented in exile");
            GameView remoteCurrent = remote.gui().getGameView();
            GameView hostCurrent = desktop.hostGui.getGameView();
            Assert.assertNotNull(remoteCurrent);
            Assert.assertNotNull(hostCurrent);
            Assert.assertEquals(remoteCurrent.getTurn(), 4);
            Assert.assertEquals(remoteCurrent.getPhase(), PhaseType.UPKEEP);
            Assert.assertEquals(hostCurrent.getTurn(), 4);
            Assert.assertEquals(hostCurrent.getPhase(), PhaseType.UPKEEP);
            Assert.assertEquals(remoteCurrent.getId(), hostCurrent.getId(),
                    "both assigned endpoints hold the current same-game UPKEEP view");
            ScriptedHumanTcpSupport.GameViewReceipt remoteAppliedView = remote.gui().gameViewReceipts().stream()
                    .filter(receipt -> receipt.callbackOrdinal() == afterShuffle.callbackOrdinal())
                    .findFirst().orElse(null);
            Assert.assertNotNull(remoteAppliedView,
                    "the held remote state is joined to its exact current GameView receipt");
            Assert.assertEquals(remoteAppliedView.gameViewId(), remoteCurrent.getId());
            Assert.assertSame(remoteAppliedView.trackerIdentity(), remoteCurrent.getTracker(),
                    "the sampled remote snapshot retains its exact current tracker identity");
            Assert.assertTrue(remoteAppliedView.danDan());
            Assert.assertTrue(desktop.hostGui.gameViewReceipts().stream().anyMatch(receipt ->
                            receipt.gameViewId() == hostCurrent.getId()
                                    && receipt.trackerIdentity() == hostCurrent.getTracker()
                                    && receipt.danDan()),
                    "the host endpoint also retained the same current DanDan tracker");
            Set<Integer> eventDerivedShuffleIds = Set.of(setup.privateTopIds().get(0), setup.publicCreatureId(),
                    setup.tellingTimeId(), setup.fengrafId());
            List<Integer> currentLibrary = afterShuffle.zonesByPlayer().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Library);
            List<Integer> hostCurrentHand = afterShuffle.zonesByPlayer().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Hand);
            List<Integer> remoteCurrentHand = afterShuffle.zonesByPlayer().get(remote.gui().ownPlayerViewId())
                    .get(ZoneType.Hand);
            List<Integer> preShuffleHostHand = scenario.privacyShuffleHostPreShuffleHandIds();
            Assert.assertTrue(scenario.privacyShufflePreShuffleGameViewId() > 0
                            && scenario.privacyShufflePreShuffleGameViewId() == hostCurrent.getId(),
                    "the pre-shuffle host hand is tied to this same live GameView");
            List<Integer> hostRenderedHand = findPlayerView(hostCurrent, desktop.hostGui.ownPlayerViewId())
                    .getHand().stream().map(CardView::getId).toList();
            List<Integer> remoteRenderedHand = findPlayerView(remoteCurrent, remote.gui().ownPlayerViewId())
                    .getHand().stream().map(CardView::getId).toList();
            Assert.assertEquals(hostRenderedHand, hostCurrentHand,
                    "the held host view's own hand matches the actual T4 state fence");
            Assert.assertEquals(remoteRenderedHand, remoteCurrentHand,
                    "the held remote view's own hand matches the actual T4 state fence");
            gameNKnownIds.get(0).addAll(hostRenderedHand);
            gameNKnownIds.get(1).addAll(remoteRenderedHand);
            gameNKnownIds.get(0).addAll(preShuffleHostHand);
            Assert.assertFalse(scenario.privacyChoices().isEmpty(),
                    "the actual Telling Time chooser has captured private candidate-choice receipts");
            for (ScriptedHumanTcpSupport.PrivacyChoiceReceipt choice : scenario.privacyChoices()) {
                Assert.assertEquals(choice.seat(), tellingTimeSeat,
                        "the captured private top-card choices belong to the actual Telling Time chooser");
                Assert.assertEquals(choice.chooserViewId(), chooserViewId);
                gameNKnownIds.get(tellingTimeSeat).addAll(choice.cardIds());
            }
            for (ScriptedHumanTcpSupport.PrivacyStateReceipt publicState :
                    List.of(turnTwoDraw, turnThreeDraw, afterShuffle)) {
                for (Map<ZoneType, List<Integer>> zones : publicState.zonesByPlayer().values()) {
                    for (ZoneType publicZone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                        List<Integer> publicIds = zones.get(publicZone);
                        if (publicIds != null) {
                            gameNKnownIds.get(0).addAll(publicIds);
                            gameNKnownIds.get(1).addAll(publicIds);
                        }
                    }
                }
            }
            Assert.assertEquals(afterShuffle.zonesByPlayer().get(remote.gui().ownPlayerViewId())
                    .get(ZoneType.Library), currentLibrary, "both seats identify the same current library membership");
            for (int cardId : eventDerivedShuffleIds) {
                CardView hostSample = findCardView(hostCurrent, cardId);
                CardView remoteSample = findCardView(remoteCurrent, cardId);
                PlayerView hostOwn = findPlayerView(hostCurrent, desktop.hostGui.ownPlayerViewId());
                PlayerView remoteOwn = findPlayerView(remoteCurrent, remote.gui().ownPlayerViewId());
                Assert.assertNotNull(hostSample);
                Assert.assertNotNull(remoteSample);
                Assert.assertNotNull(hostOwn);
                Assert.assertNotNull(remoteOwn);
                if (currentLibrary.contains(cardId)) {
                    Assert.assertFalse(hostSample.mayPlayerLook(hostOwn));
                    Assert.assertFalse(hostSample.canBeShownTo(hostOwn));
                    Assert.assertFalse(remoteSample.mayPlayerLook(remoteOwn));
                    Assert.assertFalse(remoteSample.canBeShownTo(remoteOwn));
                }
            }
            List<Integer> retainedKnownInLibrary = setup.privateTopIds().stream()
                    .filter(currentLibrary::contains).toList();
            System.out.println("DANDAN_PRIVACY_SHUFFLE_PRIOR_CHOOSER_LIBRARY_WITNESSES=" + retainedKnownInLibrary.size());
            Assert.assertFalse(retainedKnownInLibrary.isEmpty(),
                    "the fixed-seed Telling Time witness remains in the current library");
            for (int priorChooserKnownLibraryId : retainedKnownInLibrary) {
                CardView hostKnown = findCardView(hostCurrent, priorChooserKnownLibraryId);
                CardView remoteKnown = findCardView(remoteCurrent, priorChooserKnownLibraryId);
                PlayerView hostOwn = findPlayerView(hostCurrent, desktop.hostGui.ownPlayerViewId());
                PlayerView remoteOwn = findPlayerView(remoteCurrent, remote.gui().ownPlayerViewId());
                Assert.assertFalse(hostKnown.mayPlayerLook(hostOwn) || hostKnown.canBeShownTo(hostOwn),
                        "Telling Time history grants no current host library permission");
                Assert.assertFalse(remoteKnown.mayPlayerLook(remoteOwn) || remoteKnown.canBeShownTo(remoteOwn),
                        "Telling Time history grants no current remote library permission");
            }
            List<Integer> priorOwnHandInLibrary = preShuffleHostHand.stream()
                    .filter(currentLibrary::contains).toList();
            System.out.println("DANDAN_PRIVACY_SHUFFLE_PRIOR_OWN_HAND_LIBRARY_WITNESSES=" + priorOwnHandInLibrary.size());
            Assert.assertFalse(priorOwnHandInLibrary.isEmpty(),
                    "the fixed-seed host-hand witness remains in the current library");
            for (int priorOwnHandLibraryId : priorOwnHandInLibrary) {
                CardView hostPrior = findCardView(hostCurrent, priorOwnHandLibraryId);
                CardView remotePrior = findCardView(remoteCurrent, priorOwnHandLibraryId);
                PlayerView hostOwn = findPlayerView(hostCurrent, desktop.hostGui.ownPlayerViewId());
                PlayerView remoteOwn = findPlayerView(remoteCurrent, remote.gui().ownPlayerViewId());
                Assert.assertFalse(hostPrior.mayPlayerLook(hostOwn) || hostPrior.canBeShownTo(hostOwn),
                        "a prior host-hand identity grants no current host library permission");
                Assert.assertFalse(remotePrior.mayPlayerLook(remoteOwn) || remotePrior.canBeShownTo(remoteOwn),
                        "a prior host-hand identity grants no current remote library permission");
            }
            Set<Integer> allPostDayHandIds = new HashSet<>(hostCurrentHand);
            allPostDayHandIds.addAll(remoteCurrentHand);
            PlayerView hostOwn = findPlayerView(hostCurrent, desktop.hostGui.ownPlayerViewId());
            PlayerView remoteOwn = findPlayerView(remoteCurrent, remote.gui().ownPlayerViewId());
            Assert.assertEquals(allPostDayHandIds.size(), 14,
                    "the two actual post-Day hands contain fourteen distinct physical cards");
            for (int cardId : allPostDayHandIds) {
                assertCurrentPrivateHandPermission(cardId, hostCurrentHand, remoteCurrentHand,
                        findCardView(hostCurrent, cardId), findCardView(remoteCurrent, cardId), hostOwn, remoteOwn);
            }
            // These membership and permission checks do not assert a shuffled position or that either player forgets identities.
            int hostHandControl = afterShuffle.zonesByPlayer().get(desktop.hostGui.ownPlayerViewId())
                    .get(ZoneType.Hand).get(0);
            int remoteHandControl = afterShuffle.zonesByPlayer().get(remote.gui().ownPlayerViewId())
                    .get(ZoneType.Hand).get(0);
            Assert.assertTrue(findCardView(hostCurrent, hostHandControl).canBeShownTo(
                    findPlayerView(hostCurrent, desktop.hostGui.ownPlayerViewId())));
            Assert.assertFalse(findCardView(remoteCurrent, hostHandControl).canBeShownTo(
                    findPlayerView(remoteCurrent, remote.gui().ownPlayerViewId())));
            Assert.assertTrue(findCardView(remoteCurrent, remoteHandControl).canBeShownTo(
                    findPlayerView(remoteCurrent, remote.gui().ownPlayerViewId())));
            Assert.assertFalse(findCardView(hostCurrent, remoteHandControl).canBeShownTo(
                    findPlayerView(hostCurrent, desktop.hostGui.ownPlayerViewId())));
            List<ScriptedHumanTcpSupport.PrivacyShuffleEventReceipt> shuffleEvents = scenario.privacyShuffleEvents();
            Assert.assertEquals(shuffleEvents.stream().map(ScriptedHumanTcpSupport.PrivacyShuffleEventReceipt::playerViewId)
                            .collect(java.util.stream.Collectors.toSet()),
                    Set.of(desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId()),
                    "the actual effect emitted a typed shuffle event for each assigned player; count/order are unconstrained");
            Assert.assertTrue(shuffleEvents.stream().allMatch(event -> event.turn() == 3
                            && event.phase() == PhaseType.MAIN1
                            && event.sourceCardId() == setup.daysUndoingId()
                            && event.castPriorityOrdinal() == scenario.privacyDaysUndoingCastOrdinal()
                            && event.paidStackCallbackOrdinal() > event.castPriorityOrdinal()),
                    "each typed shuffle is captured inside the paid Day's Undoing turn-3 resolution interval");
            Assert.assertTrue(scenario.privacyStackViewReceipts().stream().anyMatch(stack ->
                            stack.sourceCardId() == setup.daysUndoingId()
                                    && stack.activatorViewId() == desktop.hostGui.ownPlayerViewId()
                                    && !stack.ability()),
                    "the assigned host's actual Day's Undoing spell reached the stack");
            ScriptedHumanTcpSupport.PrivacyPaymentReceipt daysPayment = scenario.privacyPaymentReceipts().stream()
                    .filter(payment -> payment.sourceCardId() == setup.daysUndoingId()).findFirst().orElse(null);
            Assert.assertNotNull(daysPayment, "the actual turn-3 Day's Undoing stack retained its paid ability");
            ScriptedHumanTcpSupport.PrivacyAbilityReceipt daysAbility = scenario.privacyAbilities().stream()
                    .filter(ability -> ability.cardId() == setup.daysUndoingId()).findFirst().orElse(null);
            Assert.assertNotNull(daysAbility, "the host selected Day's Undoing through the actual ability callback");
            Assert.assertEquals(daysAbility.seat(), 0);
            Assert.assertEquals(daysAbility.chooserViewId(), desktop.hostGui.ownPlayerViewId());
            Assert.assertTrue(scenario.privacyDaysUndoingCastOrdinal() > 0
                            && scenario.privacyDaysUndoingCastOrdinal() < daysAbility.callbackOrdinal()
                            && daysAbility.callbackOrdinal() < daysPayment.callbackOrdinal(),
                    "the cast priority, actual spell ability, and paid stack receipt are causally ordered");
            int heldT4GameViewId = scenario.privacyGame().getView().getId();
            awaitLifecycleCondition(scenario, () -> {
                GameView currentRemoteView = remote.gui().getGameView();
                return currentRemoteView != null && currentRemoteView.getId() == heldT4GameViewId
                        && scenario.privacyGame().getView().getId() == heldT4GameViewId
                        && (!deltaSync || hasPrivacyWrappedResolution(remote.gui().captures(),
                                setup.daysUndoingId(), daysAbility.abilityId()))
                        && currentRemoteView.getGameLog() != null
                        && currentRemoteView.getGameLog().getAllEntries().stream().anyMatch(entry ->
                                entry.type() == GameLogEntryType.STACK_RESOLVE && entry.sourceCard() != null
                                        && entry.sourceCard().getId() == setup.daysUndoingId()
                                        && !entry.message().isBlank()
                                        && remote.gui().captures().stream()
                                                .filter(capture -> "handleGameEvents".equals(capture.callback()))
                                                .flatMap(capture -> capture.eventFacts().stream())
                                                .anyMatch(fact -> fact.eventClass().endsWith("GameEventSpellResolved")
                                                        && fact.scalarFacts().contains(
                                                                "spell.id=" + daysAbility.abilityId())
                                                        && fact.scalarFacts().contains(
                                                                "spell.hostCardId=" + setup.daysUndoingId())
                                                        && fact.stackDescription() != null
                                                        && !fact.stackDescription().isBlank()
                                                        && entry.message().equals(fact.stackDescription())));
            }, 15, "the held Game N T4 view retains the exact Day's Undoing event receipt");
            ScriptedHumanTcpSupport.Capture dayCastCapture = remote.gui().captures().stream()
                    .filter(capture -> "handleGameEvents".equals(capture.callback())
                            && capture.eventFacts().stream().anyMatch(fact ->
                                    fact.eventClass().endsWith("GameEventSpellAbilityCast")
                                            && fact.scalarFacts().contains("sa.id=" + daysAbility.abilityId())
                                            && fact.scalarFacts().contains("sa.hostCardId=" + setup.daysUndoingId())))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "actual Day's Undoing cast receipt is absent"));
            ScriptedHumanTcpSupport.EventFact dayCastFact = dayCastCapture.eventFacts().stream()
                    .filter(fact -> fact.eventClass().endsWith("GameEventSpellAbilityCast")
                            && fact.scalarFacts().contains("sa.id=" + daysAbility.abilityId())
                            && fact.scalarFacts().contains("sa.hostCardId=" + setup.daysUndoingId()))
                    .findFirst().orElseThrow();
            ScriptedHumanTcpSupport.Capture dayResolvedCapture = remote.gui().captures().stream()
                    .filter(capture -> "handleGameEvents".equals(capture.callback())
                            && capture.eventFacts().stream().anyMatch(fact ->
                                    fact.eventClass().endsWith("GameEventSpellResolved")
                                            && fact.scalarFacts().contains("spell.id=" + daysAbility.abilityId())
                                            && fact.scalarFacts().contains("spell.hostCardId=" + setup.daysUndoingId())))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "actual Day's Undoing resolution receipt is absent"));
            ScriptedHumanTcpSupport.EventFact dayResolution = dayResolvedCapture.eventFacts().stream()
                    .filter(fact -> fact.eventClass().endsWith("GameEventSpellResolved")
                            && fact.scalarFacts().contains("spell.id=" + daysAbility.abilityId())
                            && fact.scalarFacts().contains("spell.hostCardId=" + setup.daysUndoingId()))
                    .findFirst().orElseThrow();
            int dayCastFactIndex = dayCastCapture.eventFacts().indexOf(dayCastFact);
            int dayResolvedFactIndex = dayResolvedCapture.eventFacts().indexOf(dayResolution);
            Assert.assertTrue(dayCastCapture.callbackOrdinal() < dayResolvedCapture.callbackOrdinal()
                            || dayCastCapture.callbackOrdinal() == dayResolvedCapture.callbackOrdinal()
                            && dayCastFactIndex < dayResolvedFactIndex,
                    "the actual Day cast event precedes its resolution event receipt");
            if (deltaSync) {
                ScriptedHumanTcpSupport.Capture dayCastParent = remote.gui().captures().stream()
                        .filter(parent -> "applyDelta".equals(parent.callback())
                                && parent.callbackOrdinal() == dayCastCapture.parentCallbackOrdinal()
                                && parent.sequence() == dayCastCapture.parentSequence())
                        .findFirst().orElse(null);
                Assert.assertNotNull(dayCastParent, "delta Day cast receipt has its exact applying packet");
                ScriptedHumanTcpSupport.Capture dayParent = remote.gui().captures().stream()
                        .filter(parent -> "applyDelta".equals(parent.callback())
                                && parent.callbackOrdinal() == dayResolvedCapture.parentCallbackOrdinal()
                                && parent.sequence() == dayResolvedCapture.parentSequence())
                        .findFirst().orElse(null);
                Assert.assertNotNull(dayParent,
                        "delta Day's Undoing event receipt matches its exact applying packet");
                Assert.assertEquals(dayParent.wrappedEventCount(), dayResolvedCapture.eventCount());
                Assert.assertTrue(dayCastParent.callbackOrdinal() >= turnThreeDraw.callbackOrdinal()
                                && dayCastParent.callbackOrdinal() <= afterShuffle.callbackOrdinal(),
                        "the actual Day cast's parent packet falls from the applied T3 state through held T4");
                Assert.assertTrue(dayCastParent.sequence() >= turnThreeDraw.sequence()
                                && dayCastParent.sequence() <= afterShuffle.sequence());
                Assert.assertTrue(dayParent.callbackOrdinal() >= dayCastParent.callbackOrdinal()
                                && dayParent.callbackOrdinal() <= afterShuffle.callbackOrdinal(),
                        "the actual Day resolution's parent packet falls from its cast through held T4");
                Assert.assertTrue(dayParent.sequence() >= turnThreeDraw.sequence()
                                && dayParent.sequence() <= afterShuffle.sequence());
            } else {
                Assert.assertEquals(dayResolvedCapture.eventCount(), dayResolvedCapture.eventFacts().size(),
                        "full-sync Game N has a complete actual event receipt");
                Assert.assertEquals(dayCastCapture.eventCount(), dayCastCapture.eventFacts().size(),
                        "full-sync Game N has a complete actual Day cast receipt");
            }
            Assert.assertNotNull(dayResolution.stackDescription());
            Assert.assertFalse(dayResolution.stackDescription().isBlank());
            List<forge.game.GameLogEntry> hostLogEntries = hostCurrent.getGameLog().getAllEntries();
            List<forge.game.GameLogEntry> remoteLogEntries = remoteCurrent.getGameLog().getAllEntries();
            for (List<forge.game.GameLogEntry> entries : List.of(hostLogEntries, remoteLogEntries)) {
                for (int publicSpellId : List.of(setup.tellingTimeId(), setup.fengrafId(), setup.daysUndoingId())) {
                    Assert.assertTrue(entries.stream().anyMatch(entry ->
                                    entry.type() == GameLogEntryType.STACK_ADD && entry.sourceCard() != null
                                            && entry.sourceCard().getId() == publicSpellId
                                            && !entry.message().isBlank()),
                            "the actual public STACK_ADD entry is present for card " + publicSpellId);
                }
                Assert.assertTrue(entries.stream().anyMatch(entry ->
                                entry.type() == GameLogEntryType.STACK_RESOLVE && entry.sourceCard() != null
                                        && entry.sourceCard().getId() == setup.daysUndoingId()
                                        && !entry.message().isBlank()
                                        && entry.message().equals(dayResolution.stackDescription())),
                        "the actual Day's Undoing resolved log row carries its source card and captured stack text");
            }
            for (int publicSpellId : List.of(setup.tellingTimeId(), setup.fengrafId(), setup.daysUndoingId())) {
                gameNKnownIds.get(0).add(publicSpellId);
                gameNKnownIds.get(1).add(publicSpellId);
            }
            Set<Integer> privateIdentityIds = new HashSet<>(setup.privateTopIds());
            for (ScriptedHumanTcpSupport.MulliganChoiceReceipt choice : scenario.mulliganChoices()) {
                privateIdentityIds.addAll(choice.handIds());
            }
            for (ScriptedHumanTcpSupport.FreeRevealReceipt freeOpeningReveal : scenario.freeReveals()) {
                privateIdentityIds.addAll(freeOpeningReveal.cardIds());
            }
            for (ScriptedHumanTcpSupport.KeepOpeningReceipt keep : scenario.keepOpeningReceipts()) {
                privateIdentityIds.addAll(keep.handIds());
            }
            for (ScriptedHumanTcpSupport.LondonBottomDispatchReceipt bottom : scenario.londonBottomDispatches()) {
                privateIdentityIds.addAll(bottom.chooserHandIds());
                privateIdentityIds.add(bottom.cardId());
            }
            privateIdentityIds.addAll(hostCurrentHand);
            privateIdentityIds.addAll(remoteCurrentHand);
            privateIdentityIds.addAll(preShuffleHostHand);
            privateIdentityIds.addAll(turnTwoDraw.zonesByPlayer()
                    .get(remote.gui().ownPlayerViewId()).get(ZoneType.Hand));
            assertPrivacyLogRowsAtFirstObservation(scenario, scenario.privacyGame(), privateIdentityIds,
                    heldT4GameViewId, List.of(desktop.hostGui, remote.gui()));
            Set<String> hostKnownNames = privacyKnownNames(scenario.privacyGame(), gameNKnownIds.get(0));
            Set<String> remoteKnownNames = privacyKnownNames(scenario.privacyGame(), gameNKnownIds.get(1));
            Set<String> hostForbiddenNames = privacyUnknownNames(scenario.privacyGame(), privateIdentityIds, hostKnownNames);
            Set<String> remoteForbiddenNames = privacyUnknownNames(scenario.privacyGame(), privateIdentityIds, remoteKnownNames);
            Assert.assertFalse(hostForbiddenNames.isEmpty(),
                    "the actual fixed-seed T4 history supplies a still-unknown host identity witness");
            Assert.assertFalse(remoteForbiddenNames.isEmpty(),
                    "the actual fixed-seed T4 history supplies a still-unknown remote identity witness");
            assertPrivacyLogObservationPositiveRows(List.of(desktop.hostGui, remote.gui()), heldT4GameViewId,
                    List.of(setup.tellingTimeId(), setup.fengrafId(), setup.daysUndoingId()),
                    setup.daysUndoingId(), dayResolution.stackDescription());
            assertPrivacyLogOmitsNames(hostCurrent, hostForbiddenNames,
                    "Game N host log at the held post-shuffle T4 fence");
            assertPrivacyLogOmitsNames(remoteCurrent, remoteForbiddenNames,
                    "Game N remote log at the held post-shuffle T4 fence");
            Assert.assertEquals(daysPayment.activatorViewId(), desktop.hostGui.ownPlayerViewId());
            Assert.assertEquals(daysPayment.payingManaSourceIds().size(), 3);
            Assert.assertEquals(new HashSet<>(daysPayment.payingManaSourceIds()), new HashSet<>(setup.hostManaIds()));
            Assert.assertTrue(daysPayment.payingManaColors().stream()
                    .anyMatch(color -> (color & MagicColor.BLUE) != 0), "actual Day's Undoing payment included blue mana");
            Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                    == ScriptedHumanTcpSupport.Decision.CAST_DAYS_UNDOING
                    && Objects.equals(action.cardId(), setup.daysUndoingId())).count(), 1L);
            Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                    == ScriptedHumanTcpSupport.Decision.PAY_DAYS_UNDOING).count(), 3L);
            Assert.assertEquals(scenario.privacyAbilities().stream().filter(receipt ->
                    setup.remoteManaIds().contains(receipt.cardId()) || setup.hostManaIds().contains(receipt.cardId())).count(), 8L,
                    "the three later host Island abilities are distinct from the five preserved privacy-payment receipts");
            Assert.assertEquals(afterShuffle.drawnThisTurnByPlayer().getOrDefault(remote.gui().ownPlayerViewId(), -1), 0,
                    "the remote is held at UPKEEP before its normal turn-4 draw");
            if (scenario.privacyHistoryReconnectScenario()) {
                Assert.assertNotNull(scenario.privacyShuffleT4Controller(),
                        "the same Game retained B's actual human controller at its held T4 prompt");
                Assert.assertNotNull(scenario.privacyShuffleT4Input(),
                        "the same Game retained B's exact InputPassPriority at its held T4 prompt");
                Assert.assertTrue(scenario.isSamePrivacyShuffleT4PriorityInput(),
                        "the old B priority input remains current before the intentional disconnect");
                return new PrivacyTerminalReceipt(null, inboundBytesAtOpening);
            }
            Game privacyGame = scenario.privacyGame();
            Assert.assertNotNull(privacyGame);
            Assert.assertSame(match.getGame(), privacyGame,
                    "the retained terminal trace is attached to this same actual hosted Game");
            LifecycleTrace terminalTrace = new LifecycleTrace();
            terminalTrace.expectPhysicalCardIds(setup.physicalCardIds());
            terminalTrace.attach(privacyGame);
            match.setEndGameHook(() -> terminalTrace.recordEngineReturn(privacyGame));
            desktop.hostGui.concedePrivacyShuffleHistoryAfterAssertions();
            Assert.assertTrue(desktop.hostGui.awaitActualTerminal(30, TimeUnit.SECONDS));
            assertNoFailure(scenario);
            Assert.assertTrue(remote.gui().awaitActualTerminal(30, TimeUnit.SECONDS));
            assertNoFailure(scenario);
            Assert.assertTrue(terminalTrace.awaitOutcome(30, TimeUnit.SECONDS),
                    "the actual same-game outcome event fired before hosted teardown");
            Assert.assertTrue(terminalTrace.awaitEngineReturn(30, TimeUnit.SECONDS),
                    "the actual same-game Match.startGame return reached HostedMatch endGameHook");
            Assert.assertEquals(terminalTrace.outcomeCount(), 1);
            Assert.assertEquals(terminalTrace.game, privacyGame);
            Assert.assertTrue(terminalTrace.outcomeCardIdsMatch && terminalTrace.engineReturnCardIdsMatch,
                    "outcome and engine-return snapshots conserve the exact setup physical-card set");
            GameView terminalGameView = match.getGameView();
            Assert.assertNotNull(terminalGameView, "the actual terminal GameView is retained before hosted cleanup");
            Assert.assertTrue(terminalGameView.isGameOver());
            Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                    == ScriptedHumanTcpSupport.Decision.CONCEDE && action.seat() == 0).count(), 1L);
            assertReadinessGateSummary(scenario, "privacy-history");
            return new PrivacyTerminalReceipt(terminalGameView, inboundBytesAtOpening);
        }

        desktop.hostGui.concedePrivacyAfterAssertions();
        Assert.assertTrue(desktop.hostGui.awaitActualTerminal(30, TimeUnit.SECONDS));
        assertNoFailure(scenario);
        Assert.assertTrue(remote.gui().awaitActualTerminal(30, TimeUnit.SECONDS));
        assertNoFailure(scenario);
        GameView terminalGameView = match.getGameView();
        Assert.assertNotNull(terminalGameView, "actual terminal GameView is retained before hosted cleanup");
        Assert.assertTrue(terminalGameView.isGameOver());
        Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                == ScriptedHumanTcpSupport.Decision.CONCEDE && action.seat() == 0).count(), 1L);
        assertReadinessGateSummary(scenario, "privacy");
        return new PrivacyTerminalReceipt(terminalGameView, inboundBytesAtOpening);
    }

    private static void assertReadinessGateSummary(ScriptedHumanTcpSupport.Scenario scenario, String caseName) {
        ScriptedHumanTcpSupport.ReadinessGateSummary summary = scenario.readinessGateSummary();
        Assert.assertEquals(summary.openingKeepCompletions(), summary.openingKeepDeferrals(),
                "every deferred opening Keep reached exactly one applied-state completion");
        Assert.assertEquals(summary.openingKeepOkDispatches(), summary.openingKeepCompletions(),
                "each deferred opening Keep made exactly one endpoint OK dispatch");
        List<ScriptedHumanTcpSupport.OpeningKeepGateReceipt> keepReceipts = scenario.openingKeepGateReceipts();
        Assert.assertEquals(keepReceipts.size(), summary.openingKeepCompletions());
        int expectedGateHandCount = scenario.privacyMulliganPrefixScenario() && !keepReceipts.isEmpty() ? 6 : 7;
        int expectedLondonBottomId = scenario.privacyMulliganPrefixScenario() && !keepReceipts.isEmpty()
                ? scenario.londonBottomDispatches().get(0).cardId() : -1;
        long expectedGateStateLowerBound = scenario.privacyMulliganPrefixScenario() && !keepReceipts.isEmpty()
                ? scenario.londonBottomDispatches().get(0).zeroCardPromptOrdinal() : -1;
        Assert.assertTrue(keepReceipts.stream().allMatch(receipt -> receipt.inputType()
                        == forge.gamemodes.match.input.InputConfirmMulligan.class
                        && receipt.handCount() == expectedGateHandCount
                        && receipt.londonBottomCardId() == expectedLondonBottomId
                        && (expectedLondonBottomId < 0 || receipt.bottomInSharedLibrary())
                        && receipt.sameGame() && receipt.sameTracker()
                        && receipt.okDispatchCount() == 1
                        && receipt.appliedCallbackOrdinal() > receipt.appliedStateLowerBoundOrdinal()
                        && receipt.appliedStateLowerBoundOrdinal() == (expectedLondonBottomId < 0
                                ? receipt.promptOrdinal() : expectedGateStateLowerBound)
                        && (expectedLondonBottomId >= 0
                                || receipt.appliedCallbackOrdinal() > receipt.promptOrdinal())),
                "each readiness-gated Keep used its typed input, causal current hand/tracker, and one OK");
        if (summary.fengrafPreSendRepaints() > 0) {
            long preSendOrdinal = scenario.privacyPreSendRepaintOrdinal();
            Assert.assertTrue(preSendOrdinal > scenario.privacyFengrafConfirmOrdinal()
                            && preSendOrdinal > scenario.privacyConfirmButtonsOrdinal(),
                    "the exact same-input Fengraf repaint was observed after confirmation but before send");
            if (scenario.privacyConfirmRepaintOrdinal() > 0) {
                Assert.assertTrue(preSendOrdinal < scenario.privacyConfirmRepaintOrdinal(),
                        "the pre-send repaint precedes the separate post-send repaint");
            }
        }
        if (summary.t4RepeatedPriorityCallbacks() > 0) {
            Assert.assertTrue(scenario.privacyShuffleT4FirstPriorityOrdinal() > 0,
                    "a repeated T4 callback follows a retained first actual priority callback");
        }
        System.out.printf("READINESS_GATE case=%s deferredKeep=%d completedKeep=%d keepOkCalls=%d "
                        + "fengrafPreSendRepaints=%d t4RepeatedCallbacks=%d%n",
                caseName, summary.openingKeepDeferrals(), summary.openingKeepCompletions(),
                summary.openingKeepOkDispatches(), summary.fengrafPreSendRepaints(),
                summary.t4RepeatedPriorityCallbacks());
    }

    private static void assertCurrentPrivateHandPermission(int cardId, List<Integer> hostHand,
            List<Integer> remoteHand, CardView hostCard, CardView remoteCard,
            PlayerView hostPlayer, PlayerView remotePlayer) {
        Assert.assertNotNull(hostCard);
        Assert.assertNotNull(remoteCard);
        Assert.assertNotNull(hostPlayer);
        Assert.assertNotNull(remotePlayer);
        Assert.assertNotNull(hostCard.getOwner());
        Assert.assertNotNull(remoteCard.getOwner());
        Assert.assertNotNull(hostCard.getController());
        Assert.assertNotNull(remoteCard.getController());
        Assert.assertEquals(hostCard.getOwner().getId(), remoteCard.getOwner().getId(),
                "both endpoints retain the same current owner for this physical card");
        Assert.assertEquals(hostCard.getZone(), remoteCard.getZone(),
                "both endpoints retain the same current zone for this physical card");
        if (hostHand.contains(cardId)) {
            Assert.assertEquals(hostCard.getOwner().getId(), hostPlayer.getId(),
                    "the current host-hand holder owns this physical card");
            Assert.assertEquals(remoteCard.getOwner().getId(), hostPlayer.getId(),
                    "the remote endpoint agrees that the host-hand holder owns this physical card");
            Assert.assertEquals(hostCard.getZone(), ZoneType.Hand);
            Assert.assertEquals(remoteCard.getZone(), ZoneType.Hand);
            Assert.assertEquals(hostCard.getController().getId(), hostPlayer.getId());
            Assert.assertEquals(remoteCard.getController().getId(), hostPlayer.getId());
            Assert.assertTrue(hostCard.canBeShownTo(hostPlayer));
            Assert.assertFalse(remoteCard.mayPlayerLook(remotePlayer));
            Assert.assertFalse(remoteCard.canBeShownTo(remotePlayer));
        } else if (remoteHand.contains(cardId)) {
            Assert.assertEquals(remoteCard.getOwner().getId(), remotePlayer.getId(),
                    "the current remote-hand holder owns this physical card");
            Assert.assertEquals(hostCard.getOwner().getId(), remotePlayer.getId(),
                    "the host endpoint agrees that the remote-hand holder owns this physical card");
            Assert.assertEquals(remoteCard.getZone(), ZoneType.Hand);
            Assert.assertEquals(hostCard.getZone(), ZoneType.Hand);
            Assert.assertEquals(remoteCard.getController().getId(), remotePlayer.getId());
            Assert.assertEquals(hostCard.getController().getId(), remotePlayer.getId());
            Assert.assertTrue(remoteCard.canBeShownTo(remotePlayer));
            Assert.assertFalse(hostCard.mayPlayerLook(hostPlayer));
            Assert.assertFalse(hostCard.canBeShownTo(hostPlayer));
        }
    }

    private static void assertHeldLocalTellingTimePrivacySnapshot(
            ScriptedHumanTcpSupport.Scenario scenario,
            ScriptedHumanTcpSupport.ScriptedGui host,
            ScriptedHumanTcpSupport.ScriptedGui remote,
            PrivacySetup setup,
            int expectedGameViewId) {
        assertNoFailure(scenario);
        List<ScriptedHumanTcpSupport.PrivacyChoiceReceipt> choices = scenario.privacyChoices();
        Assert.assertEquals(choices.size(), 1,
                "the first local private choice is retained while the engine is held before selection returns");
        ScriptedHumanTcpSupport.PrivacyChoiceReceipt choice = choices.get(0);
        Assert.assertEquals(choice.seat(), 0);
        Assert.assertEquals(choice.chooserViewId(), setup.hostViewId());
        Assert.assertTrue(choice.callbackOrdinal() > 0 && choice.eventId() == -1,
                "the held choice is the actual local callback, not a received remote event");
        Assert.assertEquals(choice.method(), "chooseEntitiesForEffect");
        Assert.assertEquals(choice.minimum(), 1);
        Assert.assertEquals(choice.maximum(), 1);
        Assert.assertTrue(choice.cardIds().equals(setup.privateTopIds()),
                "the held callback received exactly the deterministic top-three candidates");
        Assert.assertTrue(choice.selectedCardId() == setup.privateTopIds().get(0),
                "the scripted local choice selected the planned first candidate");
        Assert.assertEquals(choice.mayLookByChooser(), List.of(true, true, true));
        Assert.assertEquals(choice.canShowToChooser(), List.of(true, true, true));
        Assert.assertTrue(scenario.privacyHostChoiceStillHeld(),
                "the actual local chooser callback remains blocked through endpoint sampling");

        AtomicReference<ScriptedHumanTcpSupport.PrivacyHeldEndpointSnapshot> hostSnapshot = new AtomicReference<>();
        AtomicReference<ScriptedHumanTcpSupport.PrivacyHeldEndpointSnapshot> remoteSnapshot = new AtomicReference<>();
        FThreads.invokeInEdtAndWait(() -> {
            hostSnapshot.set(host.snapshotPrivacyHeldEndpoint(setup.privateTopIds(), setup.negativeHandId()));
            remoteSnapshot.set(remote.snapshotPrivacyHeldEndpoint(setup.privateTopIds(), setup.negativeHandId()));
        });
        assertNoFailure(scenario);
        Assert.assertTrue(scenario.privacyHostChoiceStillHeld(),
                "both snapshots were taken before the real chooser callback was released");
        ScriptedHumanTcpSupport.PrivacyHeldEndpointSnapshot local = hostSnapshot.get();
        ScriptedHumanTcpSupport.PrivacyHeldEndpointSnapshot unauthorized = remoteSnapshot.get();
        Assert.assertNotNull(local);
        Assert.assertNotNull(unauthorized);
        Assert.assertTrue(local.ownPlayerViewId() == setup.hostViewId());
        Assert.assertTrue(unauthorized.ownPlayerViewId() == setup.remoteViewId());
        Assert.assertTrue(local.gameViewId() == expectedGameViewId
                        && unauthorized.gameViewId() == expectedGameViewId,
                "both endpoint snapshots still reference the game captured before priority release");
        Assert.assertTrue(local.zonesByPlayer().get(setup.hostViewId()).get(ZoneType.Library)
                .equals(setup.initialLibraryIds()), "local host snapshot retains the common preselection library");
        Assert.assertTrue(local.zonesByPlayer().get(setup.remoteViewId()).get(ZoneType.Library)
                .equals(setup.initialLibraryIds()), "local host snapshot retains the other seat's shared library");
        Assert.assertTrue(unauthorized.zonesByPlayer().get(setup.hostViewId()).get(ZoneType.Library)
                .equals(setup.initialLibraryIds()), "remote snapshot retains the host's shared library");
        Assert.assertTrue(unauthorized.zonesByPlayer().get(setup.remoteViewId()).get(ZoneType.Library)
                .equals(setup.initialLibraryIds()), "remote snapshot retains its shared library");
        Assert.assertEquals(local.privateTopVisibility().values().stream().toList(),
                List.of(List.of(true, true), List.of(true, true), List.of(true, true)));
        Assert.assertEquals(unauthorized.privateTopVisibility().values().stream().toList(),
                List.of(List.of(false, false), List.of(false, false), List.of(false, false)));
        Assert.assertTrue(unauthorized.zonesByPlayer().get(setup.remoteViewId()).get(ZoneType.Hand)
                .contains(setup.negativeHandId()), "remote own-hand control remains in its assigned Hand zone");
        Assert.assertTrue(unauthorized.negativeHandVisibility().equals(List.of(false, true)),
                "remote own-hand control remains hidden from looking but showable to its owner");
        Assert.assertTrue(local.zonesByPlayer().get(setup.remoteViewId()).get(ZoneType.Hand)
                .contains(setup.negativeHandId()), "host snapshot identifies the same control as the other seat's Hand card");
        Assert.assertFalse(local.zonesByPlayer().get(setup.hostViewId()).get(ZoneType.Hand)
                .contains(setup.negativeHandId()));
        Assert.assertTrue(local.negativeHandVisibility().equals(List.of(false, false)),
                "the local chooser cannot look at or show the other seat's known hand control");
        assertNoFailure(scenario);
    }

    private static void assertTellingTimeFengrafPrivacyTerminalCase(boolean deltaSync,
            ScriptedHumanTcpSupport.Scenario scenario, DanDanDesktop desktop,
            ScriptedHumanTcpSupport remote, HostedMatch match, FServerManager server,
            ScriptedHumanTcpSupport.ReconnectObservation observation,
            PrivacyTerminalReceipt terminalReceipt, boolean privacyShuffleHistoryScenario) {
        PrivacySetup setup = desktop.privacySetup;
        ScriptedHumanTcpSupport.PrivacyStateReceipt fengrafState = scenario.privacyFengrafState();
        Assert.assertNotNull(setup);
        Assert.assertNotNull(fengrafState);
        Assert.assertTrue(terminalReceipt.terminalGameView().isGameOver(),
                "the captured actual GameView was terminal before HostedMatch teardown");
        Assert.assertNull(match.getGame(), "HostedMatch completed its actual endCurrentGame lifecycle");
        assertNoFailure(scenario);
        long hostEndFence = desktop.hostGui.captures().stream()
                .filter(capture -> "afterGameEnd".equals(capture.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        long remoteEndFence = remote.gui().captures().stream()
                .filter(capture -> "afterGameEnd".equals(capture.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        Assert.assertTrue(hostEndFence > 0);
        Assert.assertTrue(remoteEndFence > 0);
        if (scenario.privacyHistoryReconnectScenario()) {
            Assert.assertTrue(remote.gui().gameViewReceipts().stream().anyMatch(receipt ->
                            receipt.gameViewId() == scenario.privacyShuffleT4GameViewId()
                                    && receipt.trackerIdentity() != null),
                    "replacement retained a current same-Game applied view through terminal");
        } else {
            assertPrivacyModeReceipt(deltaSync, fengrafState, remote.gui());
        }
        List<ScriptedHumanTcpSupport.PrivacyPromptReceipt> finalPrompts = scenario.privacyPrompts();
        Assert.assertTrue(finalPrompts.stream().filter(prompt -> prompt.seat() == 0)
                        .allMatch(prompt -> prompt.callbackOrdinal() < hostEndFence),
                "host prompt observations are closed by its actual afterGameEnd callback");
        if (!privacyShuffleHistoryScenario) {
            Assert.assertTrue(finalPrompts.stream().filter(prompt -> prompt.seat() == 0)
                            .noneMatch(prompt -> (setup.tellingTimeSeat() == 1
                                    ? setup.privateTopNames() : Set.of(setup.negativeHandName())).stream()
                                    .anyMatch(prompt.message()::contains)),
                    "no host prompt through terminal exposed the other endpoint's private card name");
        }
        if (scenario.privacyHistoryReconnectScenario()) {
            Assert.assertTrue(remote.gui().priorityPrompts().stream()
                            .allMatch(prompt -> prompt.callbackOrdinal() < remoteEndFence),
                    "replacement endpoint prompts are closed by that same endpoint's actual afterGameEnd callback");
        } else {
            Assert.assertTrue(finalPrompts.stream().filter(prompt -> prompt.seat() == 1)
                            .allMatch(prompt -> prompt.callbackOrdinal() < remoteEndFence),
                    "remote prompt observations are closed by its actual afterGameEnd callback");
        }
        if (!privacyShuffleHistoryScenario) {
            Assert.assertTrue(finalPrompts.stream().filter(prompt -> prompt.seat() == 1)
                            .noneMatch(prompt -> (setup.tellingTimeSeat() == 0
                                    ? setup.privateTopNames() : Set.of(setup.negativeHandName())).stream()
                                    .anyMatch(prompt.message()::contains)),
                    "no remote prompt through terminal exposed the other endpoint's private card name");
        }
        Assert.assertFalse(observation.overflowed());
        Assert.assertNull(observation.failure());
        Assert.assertTrue(observation.inboundByteCount() > terminalReceipt.inboundBytesAtOpening(),
                "the retained raw inbound tap observed bytes after the opening state");
        Assert.assertEquals(observation.rawOwnedByteCount(), observation.inboundByteCount());
        Assert.assertEquals(server.getTotalSendErrors(), 0, "actual TCP host proxy remained error-free through terminal callbacks");
    }

    private static void assertPrivacyModeReceipt(boolean deltaSync,
            ScriptedHumanTcpSupport.Capture state, ScriptedHumanTcpSupport.ScriptedGui remote) {
        Assert.assertTrue(state.callbackOrdinal() > 0);
        Assert.assertTrue(remote.captures().stream().anyMatch(c -> c.callbackOrdinal() == state.callbackOrdinal()
                        && c.sequence() == state.sequence() && c.callback().equals(state.callback())),
                "privacy state receipt corresponds to one retained actual remote callback");
        if (deltaSync) {
            Assert.assertEquals(state.callback(), "applyDelta");
            Assert.assertTrue(state.sequence() >= 0);
            Assert.assertTrue(state.stateBearing());
            Assert.assertTrue(remote.captures().stream().anyMatch(c -> c.callbackOrdinal() == state.callbackOrdinal()
                    && c.stateBearing()), "delta receipt came from a populated packet");
        } else {
            Assert.assertEquals(state.callback(), "setGameView");
            Assert.assertFalse(state.stateBearing(), "full mode state came from the ordinary full GameView callback");
            Assert.assertTrue(state.zonesByPlayer().size() >= 2,
                    "full mode retained a populated applied GameView state snapshot");
            Assert.assertFalse(remote.captures().stream().anyMatch(c -> "applyDelta".equals(c.callback()) && c.stateBearing()),
                    "full mode did not apply any populated delta state");
        }
    }

    private static void assertPrivacyModeReceipt(boolean deltaSync,
            ScriptedHumanTcpSupport.PrivacyStateReceipt state, ScriptedHumanTcpSupport.ScriptedGui remote) {
        Assert.assertTrue(state.callbackOrdinal() > 0);
        Assert.assertTrue(remote.captures().stream().anyMatch(c -> c.callbackOrdinal() == state.callbackOrdinal()
                        && c.sequence() == state.sequence() && c.callback().equals(state.callback())),
                "privacy state receipt corresponds to one retained actual remote callback");
        if (deltaSync) {
            Assert.assertEquals(state.callback(), "applyDelta");
            Assert.assertTrue(state.sequence() >= 0);
            Assert.assertTrue(remote.captures().stream().anyMatch(c -> c.callbackOrdinal() == state.callbackOrdinal()
                    && c.stateBearing()), "delta receipt came from a populated packet");
        } else {
            Assert.assertEquals(state.callback(), "setGameView");
            Assert.assertFalse(state.stateBearing(), "full mode initial state came from the ordinary full GameView callback");
            Assert.assertTrue(state.zonesByPlayer().size() >= 2,
                    "full mode retained a populated applied GameView state snapshot");
            Assert.assertFalse(remote.captures().stream().anyMatch(c -> "applyDelta".equals(c.callback()) && c.stateBearing()),
                    "full mode did not apply any populated delta state");
        }
    }

    private static void assertPrivacyPhysicalState(Map<Integer, Map<ZoneType, List<Integer>>> zones,
            int hostId, int remoteId, Set<Integer> expectedPhysicalIds) {
        Map<ZoneType, List<Integer>> host = zones.get(hostId);
        Map<ZoneType, List<Integer>> remote = zones.get(remoteId);
        Assert.assertNotNull(host);
        Assert.assertNotNull(remote);
        for (ZoneType shared : List.of(ZoneType.Library, ZoneType.Graveyard)) {
            Assert.assertEquals(host.get(shared), remote.get(shared), "both assigned views retain identical shared " + shared);
        }
        Set<Integer> physical = new HashSet<>();
        for (Map.Entry<ZoneType, List<Integer>> entry : host.entrySet()) {
            Assert.assertEquals(entry.getValue().size(), new HashSet<>(entry.getValue()).size(),
                    "host zone contains each physical card ID once: " + entry.getKey());
            for (Integer id : entry.getValue()) Assert.assertTrue(physical.add(id),
                    "host zones contain unique physical card ID " + id);
        }
        for (Map.Entry<ZoneType, List<Integer>> entry : remote.entrySet()) {
            Assert.assertEquals(entry.getValue().size(), new HashSet<>(entry.getValue()).size(),
                    "remote zone contains each physical card ID once: " + entry.getKey());
            if (entry.getKey() == ZoneType.Library || entry.getKey() == ZoneType.Graveyard) continue;
            for (Integer id : entry.getValue()) Assert.assertTrue(physical.add(id), "unique physical card ID " + id);
        }
        Assert.assertEquals(physical.size(), 80, "the views retain the complete 80-card physical pool");
        Assert.assertEquals(physical, expectedPhysicalIds,
                "the actual state retains the exact initially observed physical card identities");
    }

    private static void assertPrivacyPromptWindow(List<ScriptedHumanTcpSupport.PrivacyPromptReceipt> prompts,
            int seat, int chooserViewId, long afterOrdinal, long throughOrdinal, Set<String> forbiddenNames) {
        Assert.assertTrue(afterOrdinal > 0 && throughOrdinal > afterOrdinal,
                "privacy prompt window has ordered callback fences for seat " + seat);
        List<ScriptedHumanTcpSupport.PrivacyPromptReceipt> window = prompts.stream()
                .filter(prompt -> prompt.seat() == seat && prompt.chooserViewId() == chooserViewId
                        && prompt.callbackOrdinal() > afterOrdinal
                        && prompt.callbackOrdinal() < throughOrdinal).toList();
        Assert.assertFalse(window.isEmpty(), "assigned seat " + seat + " delivered a prompt in the fenced effect window");
        Assert.assertTrue(window.stream().noneMatch(prompt -> forbiddenNames.stream()
                        .anyMatch(prompt.message()::contains)),
                "assigned seat " + seat + " prompt window contains no forbidden private card name");
    }

    private static void assertHostPrivacyRevealEndpointOrder(ScriptedHumanTcpSupport.ScriptedGui host,
            PrivacySetup setup, List<Integer> expectedLibrary, int hostGameViewId,
            long tellingTimeFenceOrdinal, long fengrafFenceOrdinal, long revealOrdinal) {
        List<ScriptedHumanTcpSupport.GameViewReceipt> receipts = host.gameViewReceipts().stream()
                .filter(receipt -> receipt.gameViewId() == hostGameViewId
                        && "privacyPriority".equals(receipt.callback()))
                .toList();
        int fengrafActorViewId = setup.fengrafSeat() == 0 ? setup.hostViewId() : setup.remoteViewId();
        ScriptedHumanTcpSupport.GameViewReceipt beforeReveal = receipts.stream()
                .filter(receipt -> receipt.callbackOrdinal() == tellingTimeFenceOrdinal
                        && expectedLibrary.equals(receipt.zonesByPlayer().get(setup.hostViewId()).get(ZoneType.Library))
                        && receipt.zonesByPlayer().get(setup.tellingTimeSeat() == 0
                                ? setup.hostViewId() : setup.remoteViewId()).get(ZoneType.Hand)
                                .contains(setup.privateTopIds().get(0)))
                .findFirst().orElse(null);
        ScriptedHumanTcpSupport.GameViewReceipt afterReveal = receipts.stream()
                .filter(receipt -> receipt.callbackOrdinal() == fengrafFenceOrdinal
                        && expectedLibrary.equals(receipt.zonesByPlayer().get(setup.hostViewId()).get(ZoneType.Library))
                        && receipt.zonesByPlayer().get(fengrafActorViewId).get(ZoneType.Hand)
                                .contains(setup.publicCreatureId())
                        && !receipt.zonesByPlayer().get(setup.hostViewId()).get(ZoneType.Graveyard)
                                .contains(setup.publicCreatureId()))
                .findFirst().orElse(null);
        Assert.assertNotNull(beforeReveal,
                "host applied the resolved Telling Time state before its own public Fengraf reveal callback");
        Assert.assertNotNull(afterReveal,
                "host applied the acquired Fengraf card state after its own public reveal callback");
        Assert.assertTrue(beforeReveal.callbackOrdinal() < revealOrdinal
                        && revealOrdinal < afterReveal.callbackOrdinal(),
                "host-side reveal and both actual state receipts share one callback-ordinal domain");
    }

    private static void assertPrivacyEndpointVisibility(ScriptedHumanTcpSupport.ScriptedGui host,
            ScriptedHumanTcpSupport.ScriptedGui remote, PrivacySetup setup, boolean partitionResolved) {
        List<Integer> privateIds = setup.privateTopIds();
        Map<Integer, List<Boolean>> hostFacts = host.visibilityFacts(privateIds, host.ownPlayerViewId());
        Map<Integer, List<Boolean>> remoteFacts = remote.visibilityFacts(privateIds, remote.ownPlayerViewId());
        int acquired = privateIds.get(0);
        Map<Integer, List<Boolean>> chooserFacts = setup.tellingTimeSeat() == 0 ? hostFacts : remoteFacts;
        Map<Integer, List<Boolean>> otherFacts = setup.tellingTimeSeat() == 0 ? remoteFacts : hostFacts;
        for (int id : privateIds) {
            Assert.assertFalse(chooserFacts.get(id).get(0),
                    "no temporary Telling Time look permission remains in applied state for " + id);
            Assert.assertEquals(chooserFacts.get(id).get(1), partitionResolved && id == acquired,
                    "only the acquired card is visible to the chooser after partition");
            Assert.assertFalse(otherFacts.get(id).get(0), "other endpoint has no temporary look permission for " + id);
            Assert.assertFalse(otherFacts.get(id).get(1), "other endpoint does not show private Telling Time card " + id);
        }
        List<Boolean> hostOwn = host.visibilityFacts(List.of(setup.negativeHandId()), host.ownPlayerViewId())
                .get(setup.negativeHandId());
        List<Boolean> remoteNegative = remote.visibilityFacts(List.of(setup.negativeHandId()), remote.ownPlayerViewId())
                .get(setup.negativeHandId());
        List<Boolean> chooserNegative = setup.tellingTimeSeat() == 0 ? hostOwn : remoteNegative;
        List<Boolean> otherNegative = setup.tellingTimeSeat() == 0 ? remoteNegative : hostOwn;
        Assert.assertTrue(otherNegative.get(1), "other endpoint positively sees its own negative hand sentinel");
        Assert.assertFalse(chooserNegative.get(1), "chooser does not see the other endpoint's negative hand sentinel");
        if (!partitionResolved) {
            ScriptedHumanTcpSupport.ScriptedGui chooserGui = setup.tellingTimeSeat() == 0 ? host : remote;
            Assert.assertTrue(chooserGui.visibilityFacts(List.of(setup.tellingTimeId()), chooserGui.ownPlayerViewId())
                    .get(setup.tellingTimeId()).get(1), "remote endpoint positively sees its own Telling Time card");
        }
        List<Boolean> publicHost = host.visibilityFacts(List.of(setup.publicCreatureId()), host.ownPlayerViewId())
                .get(setup.publicCreatureId());
        List<Boolean> publicRemote = remote.visibilityFacts(List.of(setup.publicCreatureId()), remote.ownPlayerViewId())
                .get(setup.publicCreatureId());
        if (!partitionResolved) {
            Assert.assertTrue(publicHost.get(1), "host endpoint positively sees the public graveyard card");
            Assert.assertTrue(publicRemote.get(1), "remote endpoint positively sees the public graveyard card");
        }
    }

    private static Map<Integer, Map<ZoneType, List<Integer>>> assertPrivacyReconnectKnowledgeParity(
            ScriptedHumanTcpSupport.ScriptedGui host,
            ScriptedHumanTcpSupport.ScriptedGui oldRemote, ScriptedHumanTcpSupport.ScriptedGui replacement,
            PrivacySetup setup, Map<Integer, Map<ZoneType, List<Integer>>> currentZones,
            ScriptedHumanTcpSupport.Scenario scenario, Channel replacementChannel,
            Map<Integer, Map<ZoneType, List<Integer>>> decodedFullZones,
            Map<Integer, Map<ZoneType, List<Integer>>> appliedFullZones) throws Throwable {
        ReplacementIoSnapshotGate gate = new ReplacementIoSnapshotGate(replacementChannel);
        AtomicReference<Map<Integer, Map<ZoneType, List<Integer>>>> fencedReplacementZones = new AtomicReference<>();
        Throwable primary = null;
        try {
            gate.awaitReady();
            assertNoFailure(scenario);
            FThreads.invokeInEdtAndWait(() -> fencedReplacementZones.set(assertPrivacyReconnectKnowledgeParityOnGuiThread(
                    host, oldRemote, replacement, setup, currentZones, decodedFullZones, appliedFullZones, scenario)));
        } catch (Throwable failure) {
            primary = failure;
        } finally {
            gate.release();
            gate.awaitCompletion();
        }

        Throwable gateFailure = gate.failure();
        if (gateFailure != null) {
            try { scenario.fail(gateFailure); }
            catch (Throwable failureHookError) { primary = combine(primary, failureHookError); }
        }
        try { assertNoFailure(scenario); }
        catch (Throwable failure) { primary = combine(primary, failure); }
        if (primary != null) throw primary;
        return fencedReplacementZones.get();
    }

    /** Pauses only the replacement client's actual Netty event loop during a live-model snapshot. */
    private static final class ReplacementIoSnapshotGate {
        private static final long TIMEOUT_SECONDS = 5;
        private final CountDownLatch ready = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch completed = new CountDownLatch(1);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final Future<?> ioTask;

        ReplacementIoSnapshotGate(Channel channel) {
            Assert.assertNotNull(channel, "replacement endpoint has its real client channel");
            Assert.assertTrue(channel.isActive(), "replacement client channel is active for its state snapshot");
            Future<?> submitted;
            try {
                submitted = channel.eventLoop().submit(() -> {
                    try {
                        ready.countDown();
                        if (!release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                            recordFailure(new TimeoutException("Replacement IO snapshot gate expired"));
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        recordFailure(interrupted);
                    } catch (Throwable taskFailure) {
                        recordFailure(taskFailure);
                    } finally {
                        completed.countDown();
                    }
                });
            } catch (Throwable submissionFailure) {
                recordFailure(submissionFailure);
                completed.countDown();
                submitted = null;
            }
            ioTask = submitted;
        }

        void awaitReady() {
            Throwable priorFailure = failure.get();
            if (priorFailure != null) {
                throw new AssertionError("Replacement IO snapshot gate could not be submitted", priorFailure);
            }
            try {
                if (!ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    recordFailure(new TimeoutException("Replacement IO snapshot gate did not become ready"));
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                recordFailure(interrupted);
            }
            Throwable recorded = failure.get();
            if (recorded != null) throw new AssertionError("Replacement IO snapshot gate failed before the read", recorded);
        }

        void release() { release.countDown(); }

        void awaitCompletion() {
            if (ioTask == null) return;
            try {
                if (!completed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    recordFailure(new TimeoutException("Replacement IO snapshot task did not signal completion"));
                }
                ioTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                recordFailure(interrupted);
            } catch (ExecutionException executionFailure) {
                recordFailure(executionFailure.getCause() == null ? executionFailure : executionFailure.getCause());
            } catch (TimeoutException timeout) {
                recordFailure(timeout);
            } catch (Throwable taskFailure) {
                recordFailure(taskFailure);
            }
        }

        Throwable failure() { return failure.get(); }

        private void recordFailure(Throwable error) { failure.compareAndSet(null, error); }
    }

    private static Map<Integer, Map<ZoneType, List<Integer>>> assertPrivacyReconnectKnowledgeParityOnGuiThread(
            ScriptedHumanTcpSupport.ScriptedGui host, ScriptedHumanTcpSupport.ScriptedGui oldRemote,
            ScriptedHumanTcpSupport.ScriptedGui replacement, PrivacySetup setup,
            Map<Integer, Map<ZoneType, List<Integer>>> currentZones,
            Map<Integer, Map<ZoneType, List<Integer>>> decodedFullZones,
            Map<Integer, Map<ZoneType, List<Integer>>> appliedFullZones,
            ScriptedHumanTcpSupport.Scenario scenario) {
        GameView hostView = host.getGameView();
        GameView oldRemoteView = oldRemote.getGameView();
        GameView replacementView = replacement.getGameView();
        Assert.assertNotNull(hostView);
        Assert.assertNotNull(oldRemoteView);
        Assert.assertNotNull(replacementView);
        Map<Integer, Map<ZoneType, List<Integer>>> fencedZones = privacyZoneSnapshot(replacementView);
        String zoneEvidence = privacyZoneBaselineEvidence(decodedFullZones, appliedFullZones, fencedZones, currentZones);
        Assert.assertTrue(fencedZones.equals(currentZones),
                "fenced current replacement zones match the held baseline; " + zoneEvidence);
        assertCurrentViewTrackerReceipt(host, hostView);
        assertCurrentViewTrackerReceipt(oldRemote, oldRemoteView);
        assertCurrentViewTrackerReceipt(replacement, replacementView);
        Assert.assertEquals(hostView.getId(), replacementView.getId());
        Assert.assertEquals(oldRemoteView.getId(), replacementView.getId());
        List<Integer> joinedRoleIds = new ArrayList<>(setup.privateTopIds());
        joinedRoleIds.addAll(List.of(setup.negativeHandId(), setup.tellingTimeId(), setup.daysUndoingId(),
                setup.fengrafId(), setup.publicCreatureId()));
        for (int cardId : new LinkedHashSet<>(joinedRoleIds)) {
            List<Boolean> hostFacts = currentVisibilityFacts(hostView, cardId,
                    setup.hostViewId(), setup.remoteViewId());
            List<Boolean> oldFacts = currentVisibilityFacts(oldRemoteView, cardId,
                    setup.hostViewId(), setup.remoteViewId());
            List<Boolean> replacementFacts = currentVisibilityFacts(replacementView, cardId,
                    setup.hostViewId(), setup.remoteViewId());
            Assert.assertEquals(replacementFacts, oldFacts,
                    "replacement retained same-Game permission flags for a role card");
            Assert.assertEquals(replacementFacts, hostFacts,
                    "current endpoint snapshots agree on same-Game permission flags for a role card");
        }
        List<Integer> hostHand = currentZones.get(setup.hostViewId()).get(ZoneType.Hand);
        List<Integer> remoteHand = currentZones.get(setup.remoteViewId()).get(ZoneType.Hand);
        Assert.assertEquals(hostHand.size(), 7);
        Assert.assertEquals(remoteHand.size(), 7);
        Set<Integer> handIds = new HashSet<>(hostHand);
        handIds.addAll(remoteHand);
        PlayerView hostOwn = findPlayerView(hostView, setup.hostViewId());
        PlayerView remoteOwn = findPlayerView(hostView, setup.remoteViewId());
        PlayerView oldRemoteHostOwn = findPlayerView(oldRemoteView, setup.hostViewId());
        PlayerView oldRemoteOwn = findPlayerView(oldRemoteView, setup.remoteViewId());
        PlayerView replacementHostOwn = findPlayerView(replacementView, setup.hostViewId());
        PlayerView replacementRemoteOwn = findPlayerView(replacementView, setup.remoteViewId());
        Assert.assertNotNull(hostOwn);
        Assert.assertNotNull(remoteOwn);
        Assert.assertNotNull(oldRemoteHostOwn);
        Assert.assertNotNull(oldRemoteOwn);
        Assert.assertNotNull(replacementHostOwn);
        Assert.assertNotNull(replacementRemoteOwn);
        for (int cardId : handIds) {
            CardView hostCard = findCardView(hostView, cardId);
            CardView oldRemoteCard = findCardView(oldRemoteView, cardId);
            CardView replacementCard = findCardView(replacementView, cardId);
            Assert.assertNotNull(hostCard, "current host hand card is present");
            Assert.assertNotNull(oldRemoteCard, "current old-remote hand card is present");
            Assert.assertNotNull(replacementCard, "current replacement hand card is present");
            Assert.assertSame(hostCard.getTracker(), hostView.getTracker(),
                    "host hand card belongs to the current host GameView tracker");
            Assert.assertSame(oldRemoteCard.getTracker(), oldRemoteView.getTracker(),
                    "old-remote hand card belongs to the current old-remote GameView tracker");
            Assert.assertSame(replacementCard.getTracker(), replacementView.getTracker(),
                    "replacement hand card belongs to the current replacement GameView tracker");
            assertCurrentPrivateHandPermission(cardId, hostHand, remoteHand,
                    hostCard, hostCard, hostOwn, remoteOwn);
            assertCurrentPrivateHandPermission(cardId, hostHand, remoteHand,
                    oldRemoteCard, oldRemoteCard, oldRemoteHostOwn, oldRemoteOwn);
            assertCurrentPrivateHandPermission(cardId, hostHand, remoteHand,
                    replacementCard, replacementCard, replacementHostOwn, replacementRemoteOwn);
        }
        assertPrivacyReconnectLogHistoryHandoff(oldRemote, replacement, setup, scenario);
        return fencedZones;
    }

    private static void assertPrivacyReconnectLogHistoryHandoff(
            ScriptedHumanTcpSupport.ScriptedGui oldRemote,
            ScriptedHumanTcpSupport.ScriptedGui replacement, PrivacySetup setup,
            ScriptedHumanTcpSupport.Scenario scenario) {
        int gameViewId = scenario.privacyShuffleT4GameViewId();
        GameView oldView = oldRemote.getGameView();
        GameView replacementView = replacement.getGameView();
        Assert.assertEquals(oldView.getId(), gameViewId);
        Assert.assertEquals(replacementView.getId(), gameViewId);
        List<ScriptedHumanTcpSupport.PrivacyLogRow> oldRows = actualPrivacyLogRows(oldView);
        List<ScriptedHumanTcpSupport.PrivacyLogRow> replacementRows = actualPrivacyLogRows(replacementView);
        Assert.assertFalse(oldRows.isEmpty(), "the disconnected same-Game endpoint retained actual log history");
        Assert.assertEquals(replacementRows, oldRows,
                "same-Game reconnect replay preserves every ordered log-row occurrence, including equal-valued rows");
        List<ScriptedHumanTcpSupport.PrivacyLogRow> oldObserved = observedPrivacyLogRows(oldRemote, gameViewId);
        List<ScriptedHumanTcpSupport.PrivacyLogRow> replacementObserved = observedPrivacyLogRows(replacement, gameViewId);
        Assert.assertEquals(oldObserved, oldRows,
                "the old endpoint's post-consumer row observations account for its complete current GameLog");
        Assert.assertEquals(replacementObserved, replacementRows,
                "replacement post-consumer observations account for the complete replayed GameLog");
        Set<Integer> privateCandidates = new HashSet<>(setup.privateTopIds());
        privateCandidates.add(setup.negativeHandId());
        PlayerView oldHostView = findPlayerView(oldView, setup.hostViewId());
        PlayerView oldRemotePlayerView = findPlayerView(oldView, setup.remoteViewId());
        Assert.assertNotNull(oldHostView);
        Assert.assertNotNull(oldRemotePlayerView);
        oldHostView.getHand().forEach(card -> privateCandidates.add(card.getId()));
        oldRemotePlayerView.getHand().forEach(card -> privateCandidates.add(card.getId()));
        for (ScriptedHumanTcpSupport.MulliganChoiceReceipt receipt : scenario.mulliganChoices()) {
            privateCandidates.addAll(receipt.handIds());
        }
        for (ScriptedHumanTcpSupport.FreeRevealReceipt receipt : scenario.freeReveals()) {
            privateCandidates.addAll(receipt.cardIds());
        }
        for (ScriptedHumanTcpSupport.KeepOpeningReceipt receipt : scenario.keepOpeningReceipts()) {
            privateCandidates.addAll(receipt.handIds());
        }
        for (ScriptedHumanTcpSupport.LondonBottomDispatchReceipt receipt : scenario.londonBottomDispatches()) {
            privateCandidates.addAll(receipt.chooserHandIds());
            privateCandidates.add(receipt.cardId());
        }
        for (ScriptedHumanTcpSupport.PrivacyChoiceReceipt receipt : scenario.privacyChoices()) {
            privateCandidates.addAll(receipt.cardIds());
        }
        for (ScriptedHumanTcpSupport.PrivacyRevealReceipt receipt : scenario.privacyReveals()) {
            privateCandidates.addAll(receipt.cardIds());
        }
        for (ScriptedHumanTcpSupport.PrivacyStateReceipt state : new ScriptedHumanTcpSupport.PrivacyStateReceipt[] {
                scenario.privacyShuffleHistoryState(), scenario.privacyShuffleTurnTwoDraw(),
                scenario.privacyShuffleTurnThreeDraw()}) {
            if (state == null) continue;
            for (Map<ZoneType, List<Integer>> zones : state.zonesByPlayer().values()) {
                privateCandidates.addAll(zones.getOrDefault(ZoneType.Hand, List.of()));
            }
        }
        Map<Integer, Set<Integer>> oldEndpointKnowledge = assertPrivacyLogRowsAtFirstObservation(scenario,
                scenario.privacyGame(), privateCandidates, gameViewId, List.of(oldRemote));
        Set<Integer> handedKnowledgeIds = oldEndpointKnowledge.get(setup.remoteViewId());
        Assert.assertNotNull(handedKnowledgeIds,
                "replacement inherits knowledge audited by the exact old endpoint and assigned seat");
        Assert.assertFalse(handedKnowledgeIds.isEmpty(),
                "replacement receives concrete same-Game knowledge from the disconnected endpoint");
        List<ScriptedHumanTcpSupport.PrivacyLogObservation> replacementObservations = replacement
                .privacyLogObservations().stream().filter(receipt -> receipt.gameViewId() == gameViewId)
                .sorted(Comparator.comparingLong(ScriptedHumanTcpSupport.PrivacyLogObservation::observationOrdinal))
                .toList();
        int replayedRows = 0;
        Set<Integer> replacementKnownIds = new HashSet<>(handedKnowledgeIds);
        Set<String> replayUnknownWitnesses = new HashSet<>();
        for (ScriptedHumanTcpSupport.PrivacyLogObservation observation : replacementObservations) {
            Assert.assertEquals(observation.ownPlayerViewId(), setup.remoteViewId(),
                    "reconnect history remains in the replacement's assigned seat namespace");
            replacementKnownIds.addAll(observation.ownHandIds());
            replacementKnownIds.addAll(observation.publicCardIds());
            for (ScriptedHumanTcpSupport.PrivacyLogRow row : observation.appendedRows()) {
                Assert.assertTrue(replayedRows < oldRows.size(),
                        "same-Game reconnect is scoped to the ordered replayed history");
                Assert.assertEquals(row, oldRows.get(replayedRows++),
                        "replayed history is matched by ordered occurrence, not value deduplication");
                Set<String> forbiddenNames = privacyUnknownNames(scenario.privacyGame(), privateCandidates,
                        privacyKnownNames(scenario.privacyGame(), replacementKnownIds));
                if (!forbiddenNames.isEmpty()) replayUnknownWitnesses.addAll(forbiddenNames);
                for (String forbiddenName : forbiddenNames) {
                    Assert.assertFalse(row.message().contains(forbiddenName),
                            "replayed Game N row omits never-known name " + forbiddenName);
                }
            }
        }
        Assert.assertEquals(replayedRows, oldRows.size(),
                "replacement's ordered observation stream accounts for all replayed same-Game history");
        Assert.assertFalse(replayUnknownWitnesses.isEmpty(),
                "replacement replay rows are checked while an inherited unknown-name witness remains");
    }

    private static List<ScriptedHumanTcpSupport.PrivacyLogRow> actualPrivacyLogRows(GameView view) {
        Assert.assertNotNull(view.getGameLog(), "same-Game endpoint retains its actual GameLog");
        return view.getGameLog().getAllEntries().stream().map(entry ->
                new ScriptedHumanTcpSupport.PrivacyLogRow(entry.type().name(), entry.message(),
                        entry.sourceCard() == null ? -1 : entry.sourceCard().getId())).toList();
    }

    private static List<ScriptedHumanTcpSupport.PrivacyLogRow> observedPrivacyLogRows(
            ScriptedHumanTcpSupport.ScriptedGui endpoint, int gameViewId) {
        return endpoint.privacyLogObservations().stream().filter(item -> item.gameViewId() == gameViewId)
                .sorted(Comparator.comparingLong(ScriptedHumanTcpSupport.PrivacyLogObservation::observationOrdinal))
                .flatMap(item -> item.appendedRows().stream()).toList();
    }

    private static void assertCurrentViewTrackerReceipt(ScriptedHumanTcpSupport.ScriptedGui gui, GameView view) {
        Assert.assertNotNull(view.getTracker(), "the current endpoint GameView retains its tracker");
        Assert.assertTrue(gui.gameViewReceipts().stream().anyMatch(receipt ->
                        receipt.gameViewId() == view.getId() && receipt.trackerIdentity() == view.getTracker()),
                "the endpoint retained a receipt for its current same-Game tracker");
    }

    private static List<Boolean> currentVisibilityFacts(GameView view, int cardId, int hostViewId, int remoteViewId) {
        CardView card = findCardView(view, cardId);
        PlayerView host = findPlayerView(view, hostViewId);
        PlayerView remote = findPlayerView(view, remoteViewId);
        Assert.assertNotNull(card, "same-Game role card remains represented in current view");
        Assert.assertSame(card.getTracker(), view.getTracker(),
                "role CardView belongs to the endpoint's current GameView tracker");
        Assert.assertNotNull(host);
        Assert.assertNotNull(remote);
        return List.of(card.mayPlayerLook(host), card.canBeShownTo(host),
                card.mayPlayerLook(remote), card.canBeShownTo(remote));
    }

    private static void assertMetamorphosePhysicalZones(
            Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer, int hostId, int remoteId,
            Map<Integer, Map<ZoneType, List<Integer>>> beforeCast,
            ScriptedHumanTcpSupport.MetamorphoseResolutionReceipt resolved,
            MetamorphoseSetup setup, boolean acceptHandEntry) {
        Map<ZoneType, List<Integer>> host = zonesByPlayer.get(hostId);
        Map<ZoneType, List<Integer>> remote = zonesByPlayer.get(remoteId);
        Map<ZoneType, List<Integer>> originalHost = beforeCast.get(hostId);
        Map<ZoneType, List<Integer>> originalRemote = beforeCast.get(remoteId);
        Assert.assertNotNull(host, "the accepted state has the assigned host PlayerView");
        Assert.assertNotNull(remote, "the accepted state has the assigned remote PlayerView");
        Assert.assertNotNull(originalHost, "pre-cast callback retained host's actual hand IDs");
        Assert.assertNotNull(originalRemote, "pre-cast callback retained remote's actual hand IDs");
        for (ZoneType shared : List.of(ZoneType.Library, ZoneType.Graveyard)) {
            Assert.assertEquals(remote.get(shared), host.get(shared), "both views retain identical ordered shared " + shared);
        }
        Assert.assertEquals(host.get(ZoneType.Library).get(0), Integer.valueOf(resolved.targetCardId()),
                "the returned target is first in the common library");
        Assert.assertTrue(host.get(ZoneType.Graveyard).contains(resolved.hostCardId()),
                "the resolved spell is in the common graveyard");
        Set<Integer> expectedHostHand = new HashSet<>(originalHost.get(ZoneType.Hand));
        Assert.assertTrue(expectedHostHand.remove(setup.spellId()), "the spell was in host's pre-cast retained hand");
        Assert.assertEquals(new HashSet<>(host.get(ZoneType.Hand)), expectedHostHand,
                "the host's final hand is its exact pre-cast hand minus Metamorphose");
        Assert.assertEquals(remote.get(ZoneType.Hand), resolved.recipientHandIds());
        Assert.assertEquals(remote.get(ZoneType.Battlefield), resolved.recipientBattlefieldIds());
        Assert.assertEquals(remote.get(ZoneType.Hand).size(), acceptHandEntry ? 6 : 7);
        Set<Integer> expectedRemoteHand = new HashSet<>(originalRemote.get(ZoneType.Hand));
        if (acceptHandEntry) Assert.assertTrue(expectedRemoteHand.remove(setup.remoteHandCardId()),
                "the selected permanent was in B's pre-effect private hand");
        Assert.assertEquals(new HashSet<>(remote.get(ZoneType.Hand)), expectedRemoteHand,
                acceptHandEntry ? "Yes removes exactly the selected card from B's hand"
                        : "No leaves B's exact hand unchanged");
        Assert.assertTrue(Collections.disjoint(new HashSet<>(host.get(ZoneType.Hand)),
                new HashSet<>(remote.get(ZoneType.Hand))), "private hands retain distinct physical IDs");

        Set<Integer> physicalIds = new HashSet<>();
        for (Map<ZoneType, List<Integer>> playerZones : List.of(host, remote)) {
            for (List<Integer> cardIds : playerZones.values()) {
                Assert.assertEquals(cardIds.size(), new HashSet<>(cardIds).size(), "each received zone has unique card IDs");
            }
        }
        for (ZoneType zone : List.of(ZoneType.Library, ZoneType.Graveyard, ZoneType.Hand,
                ZoneType.Battlefield, ZoneType.Exile)) {
            for (Integer id : host.getOrDefault(zone, List.of())) {
                Assert.assertTrue(physicalIds.add(id), "physical ID appears once across host zones: " + id);
            }
            if (zone != ZoneType.Library && zone != ZoneType.Graveyard) {
                for (Integer id : remote.getOrDefault(zone, List.of())) {
                    Assert.assertTrue(physicalIds.add(id), "physical ID appears once across remote zones: " + id);
                }
            }
        }
        Assert.assertEquals(physicalIds.size(), 80, "the final received snapshot conserves all 80 official physical cards");
    }

    private static void endCurrentGameAfterForwardedAwaitPrompt(HostedMatch match, RemoteClient client,
            ScriptedHumanTcpSupport.ScriptedGui remoteGui) throws Throwable {
        RemoteClientGuiGame hostProxyGui = client.getGui();
        Assert.assertNotNull(hostProxyGui, "retained remote client still owns its host-side protocol GUI");
        long previousAwaitOrdinal = remoteGui.captures().stream()
                .filter(capture -> "awaitNextInput".equals(capture.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(0);
        FThreads.invokeInEdtAndWait(() -> {
            // ProtocolGuiGame forwards this callback to the receiving GUI, which owns the wait timer.
            hostProxyGui.awaitNextInput();
            match.endCurrentGame();
        });

        Assert.assertTrue(remoteGui.awaitAfterGameEnd(10, TimeUnit.SECONDS),
                "remote received the actual hosted afterGameEnd callback");
        remoteGui.expectAlertUserFence();
        hostProxyGui.alertUser(); // receive a fence after the actual terminal callback
        Assert.assertTrue(remoteGui.awaitAlertUser(10, TimeUnit.SECONDS),
                "remote received the in-order callback fence while its channel remained connected");
        List<ScriptedHumanTcpSupport.Capture> captures = remoteGui.captures();
        long awaitOrdinal = captures.stream().filter(c -> "awaitNextInput".equals(c.callback())
                        && c.callbackOrdinal() > previousAwaitOrdinal)
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        long endedOrdinal = captures.stream().filter(c -> "afterGameEnd".equals(c.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        long fenceOrdinal = captures.stream().filter(c -> "alertUser".equals(c.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        Assert.assertTrue(awaitOrdinal > previousAwaitOrdinal && endedOrdinal > awaitOrdinal
                        && fenceOrdinal > endedOrdinal,
                "the forwarded await callback, hosted afterGameEnd, and received fence stay in wire order");
        List<String> latePrompts = captures.stream()
                .filter(c -> c.callbackOrdinal() > endedOrdinal && c.callbackOrdinal() < fenceOrdinal)
                .map(ScriptedHumanTcpSupport.Capture::callback)
                .filter(callback -> List.of("showPromptMessage", "updateButtons").contains(callback))
                .toList();
        Assert.assertTrue(latePrompts.isEmpty(), "no wait prompt was delivered after game end: " + latePrompts);
    }

    private static void runReconnectRoundTrip(boolean deltaSync, FServerManager server,
            RemoteClient retainedClient, ScriptedHumanTcpSupport oldRemote,
            ScriptedHumanTcpSupport replacement, ScriptedHumanTcpSupport.ScriptedGui hostGui,
            ScriptedHumanTcpSupport.Scenario scenario, ScriptedHumanTcpSupport.PostKeepReceipt before,
            int hostViewId, int remoteViewId, CountDownLatch parkNotice,
            AtomicReference<String> parkNoticeText,
            ScriptedHumanTcpSupport.ReconnectObservation observation,
            AtomicReference<Integer> sendErrorBaseline) throws Throwable {
        Assert.assertNotNull(retainedClient, "the original server RemoteClient is retained before disconnect");
        Assert.assertEquals(before.prioritySeat(), 1, "remote owns the held live UPKEEP input before disconnect");
        Assert.assertEquals(before.heldTurn(), 1);
        Assert.assertEquals(before.heldTurnPlayerViewId(), hostViewId);
        ScriptedHumanTcpSupport.PriorityReceipt heldPrompt = oldRemote.gui().priorityPrompts().stream()
                .filter(p -> p.seat() == 1 && p.chooserViewId() == remoteViewId
                        && p.turn() == 1 && p.phase() == PhaseType.UPKEEP)
                .findFirst().orElseThrow(() -> new AssertionError("Old endpoint has no detached held priority prompt"));
        ScriptedHumanTcpSupport.AppliedViewReceipt heldState = oldRemote.gui().appliedViews().stream()
                .filter(view -> view.callbackOrdinal() == before.stateCapture().callbackOrdinal())
                .findFirst().orElseThrow(() -> new AssertionError("Missing detached held-state receipt"));
        Assert.assertEquals(heldState.turn(), 1);
        Assert.assertEquals(heldState.phase(), PhaseType.UPKEEP);
        Assert.assertEquals(heldState.turnPlayerViewId(), hostViewId);
        Assert.assertEquals(heldState.stackItemCount(), 0, "finite reconnect checkpoint begins with an empty stack");
        Assert.assertTrue(heldState.zonesByPlayer().get(hostViewId).get(ZoneType.Battlefield).isEmpty());
        Assert.assertTrue(heldState.zonesByPlayer().get(remoteViewId).get(ZoneType.Battlefield).isEmpty());
        List<ScriptedHumanTcpSupport.ActionReceipt> initialPasses = scenario.actions().stream()
                .filter(action -> action.decision() == ScriptedHumanTcpSupport.Decision.PASS_PRIORITY).toList();
        Assert.assertEquals(initialPasses.size(), 1, "host made exactly one real pass before the remote checkpoint");
        Assert.assertEquals(initialPasses.get(0).seat(), 0);
        Assert.assertEquals(initialPasses.get(0).chooserViewId(), hostViewId);
        Channel oldChannel = oldRemote.channel();
        Assert.assertNotNull(oldChannel, "old endpoint retains its connected client channel before close");
        long closeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        var closeFuture = oldChannel.closeFuture();
        oldRemote.close();
        long remainingCloseNanos = closeDeadline - System.nanoTime();
        Assert.assertTrue(remainingCloseNanos > 0
                        && closeFuture.await(remainingCloseNanos, TimeUnit.NANOSECONDS) && closeFuture.isSuccess(),
                "old client channel completed its normal close successfully within the bounded deadline");
        remainingCloseNanos = closeDeadline - System.nanoTime();
        var eventLoopTermination = oldChannel.eventLoop().terminationFuture();
        Assert.assertTrue(remainingCloseNanos > 0
                        && eventLoopTermination.await(remainingCloseNanos, TimeUnit.NANOSECONDS)
                        && eventLoopTermination.isSuccess(),
                "old client event loop completed its existing graceful shutdown successfully within the same deadline");
        Assert.assertFalse(oldChannel.isOpen(), "old endpoint channel is closed before capturing its GUI baseline");
        FThreads.invokeInEdtAndWait(() -> { });
        int oldCaptureCount = oldRemote.gui().captures().size();
        Assert.assertTrue(parkNotice.await(10, TimeUnit.SECONDS), "host received the existing post-park reconnect notice");
        Assert.assertNotNull(parkNoticeText.get());
        Map<String, RemoteClient> disconnected = disconnectedClients(server);
        Assert.assertEquals(retainedClient.getIndex(), 1);
        Assert.assertSame(disconnected.get(retainedClient.getUsername()), retainedClient,
                "server parked the exact original RemoteClient under its username");
        Assert.assertTrue(reconnectTimers(server).containsKey(retainedClient.getUsername()),
                "parked client has its ordinary timeout until successful same-name login");

        long gateDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        ReconnectWorkerGate gate = new ReconnectWorkerGate(server, gateDeadline);
        Throwable primary = null;
        FutureTask<Void> connect = null;
        Thread connectThread = null;
        try {
            gate.awaitWorkers();
            connect = new FutureTask<>(() -> { replacement.connect(); return null; });
            connectThread = new Thread(connect, "DanDan-reconnect-connect");
            connectThread.setDaemon(true);
            connectThread.start();
            connect.get(gate.remainingNanos(), TimeUnit.NANOSECONDS);
            replacement.installReconnectObservation(observation, gateDeadline);
            gate.releaseForInstalledTap();

            Assert.assertTrue(replacement.gui().awaitFull(15, TimeUnit.SECONDS), "replacement received reconnect full baseline");
            Assert.assertTrue(replacement.gui().awaitOpenView(15, TimeUnit.SECONDS), "replacement received its assigned openView");
            Assert.assertTrue(replacement.gui().awaitPostOpenFullState(15, TimeUnit.SECONDS),
                    "replacement received the full baseline sent after openView");
            assertNoFailure(scenario);
            Assert.assertEquals(server.getClientBySlotIndex(1), retainedClient,
                    "same-username login resumed the original server RemoteClient and seat");
            Assert.assertFalse(reconnectTimers(server).containsKey(retainedClient.getUsername()),
                    "successful resumption canceled the parked client's reclaim timeout");
            Assert.assertTrue(replacement.gui().awaitReconnectReplayPriority(15, TimeUnit.SECONDS),
                    "the server replayed the actual held remote UPKEEP prompt after resync");
            assertNoFailure(scenario);

            List<ScriptedHumanTcpSupport.Capture> captures = replacement.gui().captures();
            long firstFull = captures.stream().filter(c -> "setGameView".equals(c.callback()))
                    .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).min().orElse(-1);
            long open = captures.stream().filter(c -> "openView".equals(c.callback()))
                    .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).min().orElse(-1);
            long postOpenFull = captures.stream().filter(c -> "setGameView".equals(c.callback()) && c.callbackOrdinal() > open)
                    .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).min().orElse(-1);
            Assert.assertTrue(firstFull > 0 && firstFull < open && open < postOpenFull,
                    "reconnect ordering is initial full state, assigned view, then post-open full baseline");
            Assert.assertEquals(replacement.gui().ownPlayerViewId(), remoteViewId,
                    "reconnect retained the exact previously assigned remote PlayerView ID");
            Assert.assertTrue(replacement.gui().priorityPrompts().stream().anyMatch(p -> p.seat() == 1
                            && p.chooserViewId() == remoteViewId && p.turn() == 1 && p.phase() == PhaseType.UPKEEP
                            && p.turnPlayerViewId() == hostViewId && p.callbackOrdinal() > postOpenFull),
                    "replayed prompt retains the original owner, turn, phase and turn-player identity");

            List<ScriptedHumanTcpSupport.AppliedViewReceipt> applied = replacement.gui().appliedViews();
            ScriptedHumanTcpSupport.AppliedViewReceipt finalBaseline = applied.stream()
                    .filter(v -> "setGameView".equals(v.callback()) && v.callbackOrdinal() > open)
                    .findFirst().orElseThrow(() -> new AssertionError("Missing applied post-open full baseline"));
            assertReconnectZones(before.appliedZonesByPlayer(), finalBaseline.zonesByPlayer(), hostViewId,
                    remoteViewId, "same-seat resync baseline");
            List<ScriptedHumanTcpSupport.PreHandlerFact> facts = observation.facts();
            Assert.assertTrue(observation.inboundByteCount() > 0, "the replacement received captured inbound TCP bytes");
            Assert.assertFalse(observation.overflowed(), "bounded inbound capture did not overflow");
            Assert.assertNull(observation.failure(), "pre-handler observation completed without errors");
            int firstPreFull = indexOfFact(facts, 0, "setGameView");
            int preOpenIndex = indexOfFact(facts, 0, "openView");
            int postOpenPreFull = indexOfFact(facts, preOpenIndex + 1, "setGameView");
            Assert.assertTrue(firstPreFull >= 0 && firstPreFull < preOpenIndex && preOpenIndex < postOpenPreFull,
                    "pre-handler capture retains initial full state, openView and final baseline in order");
            Assert.assertEquals(facts.get(preOpenIndex).playerViewIds(), List.of(remoteViewId),
                    "pre-handler openView names the retained remote PlayerView");
            int replayedPromptIndex = -1;
            for (int i = postOpenPreFull + 1; i < facts.size(); i++) {
                ScriptedHumanTcpSupport.PreHandlerFact fact = facts.get(i);
                if ("updateButtons".equals(fact.method()) && fact.promptOwnerViewId() == remoteViewId
                        && fact.promptOne().equals(heldPrompt.one()) && fact.promptTwo().equals(heldPrompt.two())) {
                    replayedPromptIndex = i;
                    break;
                }
            }
            Assert.assertTrue(replayedPromptIndex > postOpenPreFull,
                    "pre-handler priority prompt follows the final full baseline");
            ScriptedHumanTcpSupport.PreHandlerFact replayedPrompt = facts.get(replayedPromptIndex);
            Assert.assertEquals(replayedPrompt.promptOneEnabled(), heldPrompt.enabledOne());
            Assert.assertEquals(replayedPrompt.promptTwoEnabled(), heldPrompt.enabledTwo());
            for (ScriptedHumanTcpSupport.PreHandlerFact fact : facts) {
                if ("setGameView".equals(fact.method())) {
                    Assert.assertTrue(fact.danDan(), "pre-handler full baseline preserves the actual DanDan view");
                    Assert.assertTrue(fact.playerViewIds().containsAll(List.of(hostViewId, remoteViewId)));
                }
            }
            int sendErrorsAtBaseline = retainedClient.getSendErrorCount();
            Assert.assertEquals(sendErrorsAtBaseline, 0,
                    "intentional old-endpoint closure produced no server send error before replacement baseline");
            sendErrorBaseline.set(sendErrorsAtBaseline);
            List<ScriptedHumanTcpSupport.Capture> oldCapturesAtBaseline = oldRemote.gui().captures();
            List<ScriptedHumanTcpSupport.ActionReceipt> actionsAtBaseline = scenario.actions();
            Assert.assertEquals(oldCapturesAtBaseline.size(), oldCaptureCount,
                    "the intentionally closed old GUI received no callbacks during replacement resync");

            replacement.gui().dispatchHeldReconnectPriorityPass();
            List<ScriptedHumanTcpSupport.ActionReceipt> actionsAfterPass = scenario.actions();
            Assert.assertEquals(actionsAfterPass.size(), actionsAtBaseline.size() + 1,
                    "replacement replay produced exactly one additional scripted action");
            Assert.assertEquals(actionsAfterPass.get(actionsAfterPass.size() - 1),
                    new ScriptedHumanTcpSupport.ActionReceipt(ScriptedHumanTcpSupport.Decision.PASS_PRIORITY,
                            1, remoteViewId, null, true));
            Assert.assertTrue(hostGui.awaitTurnOneMainOnePriority(15, TimeUnit.SECONDS),
                    "replacement PASS advanced the actual game to host turn 1 MAIN1 priority");
            Assert.assertTrue(hostGui.priorityPrompts().stream().anyMatch(p -> p.seat() == 0
                            && p.chooserViewId() == hostViewId && p.turn() == 1 && p.phase() == PhaseType.MAIN1
                            && p.turnPlayerViewId() == hostViewId && (p.enabledOne() || p.enabledTwo())),
                    "the next actual turn-1 MAIN1 input belongs to the host's assigned view");
            Assert.assertTrue(replacement.gui().awaitTurnOneMainOneView(15, TimeUnit.SECONDS),
                    "replacement received the resulting applied MAIN1 state");
            assertNoFailure(scenario);
            List<ScriptedHumanTcpSupport.AppliedViewReceipt> progress = replacement.gui().appliedViews().stream()
                    .filter(v -> v.callbackOrdinal() > postOpenFull && v.turn() == 1 && v.phase() == PhaseType.MAIN1)
                    .toList();
            Assert.assertFalse(progress.isEmpty(), "a later applied state proves the resumed priority action advanced phase");
            List<ScriptedHumanTcpSupport.PreHandlerFact> postActionFacts = observation.facts();
            Assert.assertEquals(postActionFacts.subList(0, facts.size()), facts,
                    "post-action protocol facts retain the complete pre-action baseline prefix");
            if (deltaSync) {
                Assert.assertTrue(progress.stream().anyMatch(v -> "applyDelta".equals(v.callback())
                                && v.stateBearing() && v.sequence() >= 0),
                        "delta mode delivered a populated state-bearing delta after replacement PASS");
                Assert.assertTrue(postActionFacts.subList(replayedPromptIndex + 1, postActionFacts.size()).stream()
                                .anyMatch(f -> "applyDelta".equals(f.method()) && f.sequence() >= 0
                                        && f.changedObjectCount() > 0),
                        "pre-handler delta facts retain a later populated packet after the replayed prompt");
                Assert.assertTrue(replacement.gui().populatedDeltaCount() > 0);
            } else {
                Assert.assertTrue(progress.stream().anyMatch(v -> "setGameView".equals(v.callback())),
                        "full mode delivered a later full MAIN1 state");
                Assert.assertTrue(postActionFacts.subList(replayedPromptIndex + 1, postActionFacts.size()).stream()
                                .anyMatch(f -> "setGameView".equals(f.method())),
                        "pre-handler facts retain a later full-state MAIN1 callback");
                Assert.assertFalse(postActionFacts.stream().anyMatch(f -> "applyDelta".equals(f.method())
                                && f.changedObjectCount() > 0),
                        "full mode did not receive populated delta payloads");
                Assert.assertEquals(replacement.gui().populatedDeltaCount(), 0,
                        "full mode did not deliver state-bearing deltas");
            }
            Assert.assertEquals(retainedClient.getSendErrorCount(), sendErrorsAtBaseline,
                    "server proxy send-error count did not increase after replacement baseline");
            Assert.assertEquals(oldRemote.gui().captures().size(), oldCaptureCount,
                    "the old endpoint remained quiescent after replacement advanced the game");
            Assert.assertSame(server.getClientBySlotIndex(1), retainedClient,
                    "the same server client identity remains assigned after the finite action");
            Assert.assertEquals(retainedClient.getIndex(), 1);
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            gate.releaseUnconditionally();
            if (primary != null) {
                if (connect != null) connect.cancel(true);
                try { replacement.close(); }
                catch (Throwable closeFailure) { primary.addSuppressed(closeFailure); }
                if (connectThread != null) {
                    try {
                        connectThread.join(1_000);
                        if (connectThread.isAlive()) primary.addSuppressed(
                                new AssertionError("Bounded reconnect connect helper did not stop after cancellation"));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        primary.addSuppressed(interrupted);
                    }
                }
            }
        }
    }

    private static PrivacyTerminalReceipt runPrivacyHistoryReconnectRoundTrip(FServerManager server,
            RemoteClient retainedClient,
            ScriptedHumanTcpSupport oldRemote, ScriptedHumanTcpSupport replacement,
            ScriptedHumanTcpSupport.ScriptedGui hostGui,
            ScriptedHumanTcpSupport.Scenario scenario, HostedMatch match,
            PrivacySetup setup,
            ScriptedHumanTcpSupport.ReconnectObservation oldObservation,
            int inboundBytesAtOpening,
            CountDownLatch parkNotice, AtomicReference<String> parkNoticeText,
            ScriptedHumanTcpSupport.ReconnectObservation replacementObservation,
            AtomicReference<ScriptedHumanTcpSupportTest.ServerInactiveObservation> pendingInactiveOwner,
            AtomicReference<Channel> pendingChannelOwner,
            AtomicReference<ScriptedHumanTcpSupportTest.CompletedServerInactiveReceipt> completedInactiveOwner)
            throws Throwable {
        Assert.assertNotNull(retainedClient);
        Assert.assertNotNull(oldObservation);
        Assert.assertNotNull(replacementObservation);
        Assert.assertNotNull(pendingInactiveOwner);
        Assert.assertNotNull(pendingChannelOwner);
        Assert.assertNotNull(completedInactiveOwner);
        Game game = scenario.privacyGame();
        Assert.assertNotNull(game);
        Assert.assertSame(match.getGame(), game, "reconnect continuation remains in the accepted same Game N");
        Assert.assertEquals(scenario.privacyShuffleT4GameViewId(), game.getView().getId());
        Assert.assertTrue(scenario.isSamePrivacyShuffleT4PriorityInput(),
                "the exact B T4 InputPassPriority remains live at disconnect entry");
        Assert.assertSame(game.getPlayers().get(1).getController(), scenario.privacyShuffleT4Controller());
        Assert.assertTrue(oldObservation.inboundByteCount() > 0,
                "the old endpoint already has its live channel observation before it is parked");
        Assert.assertFalse(oldObservation.overflowed());
        Assert.assertNull(oldObservation.failure());
        GameView hostBeforeDisconnect = hostGui.getGameView();
        GameView oldRemoteBeforeDisconnect = oldRemote.gui().getGameView();
        Assert.assertNotNull(hostBeforeDisconnect);
        Assert.assertNotNull(oldRemoteBeforeDisconnect);
        Assert.assertEquals(hostBeforeDisconnect.getId(), scenario.privacyShuffleT4GameViewId());
        Assert.assertEquals(oldRemoteBeforeDisconnect.getId(), scenario.privacyShuffleT4GameViewId());
        assertCurrentViewTrackerReceipt(hostGui, hostBeforeDisconnect);
        assertCurrentViewTrackerReceipt(oldRemote.gui(), oldRemoteBeforeDisconnect);
        Object hostTrackerBeforeReconnect = hostBeforeDisconnect.getTracker();

        Channel oldChannel = oldRemote.channel();
        Assert.assertNotNull(oldChannel);
        Channel oldServerChannel = ScriptedHumanTcpSupport.serverChannel(retainedClient);
        Assert.assertNotNull(oldServerChannel);
        pendingChannelOwner.set(oldServerChannel);
        ScriptedHumanTcpSupportTest.ServerInactiveObservation oldServerInactive =
                ScriptedHumanTcpSupportTest.prepareServerInactiveObservation(retainedClient);
        pendingInactiveOwner.set(oldServerInactive);
        Assert.assertSame(ScriptedHumanTcpSupport.serverChannel(retainedClient), oldServerChannel,
                "the inactive-channel observer was installed on the accepted client's current channel before close");
        long closeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        var closeFuture = oldChannel.closeFuture();
        int oldCaptureCount = -1;
        Throwable primary = null;
        FutureTask<Void> connect = null;
        Thread connectThread = null;
        ReconnectWorkerGate gate = null;
        ScriptedHumanTcpSupport.ServerDecodedEventTap serverTap = null;
        ScriptedHumanTcpSupportTest.CompletedServerInactiveReceipt completedOldServerInactive = null;
        try {
            oldRemote.close();
            long remaining = closeDeadline - System.nanoTime();
            Assert.assertTrue(remaining > 0 && closeFuture.await(remaining, TimeUnit.NANOSECONDS)
                            && closeFuture.isSuccess(),
                    "the original assigned endpoint closed normally within the bounded deadline");
            remaining = closeDeadline - System.nanoTime();
            var eventLoopTermination = oldChannel.eventLoop().terminationFuture();
            Assert.assertTrue(remaining > 0 && eventLoopTermination.await(remaining, TimeUnit.NANOSECONDS)
                            && eventLoopTermination.isSuccess(),
                    "the old endpoint event loop stopped cleanly before same-name reconnect");
            FThreads.invokeInEdtAndWait(() -> { });
            Assert.assertTrue(parkNotice.await(10, TimeUnit.SECONDS),
                    "host received the existing post-park reconnect notice");
            Assert.assertNotNull(parkNoticeText.get());
            completedOldServerInactive = ScriptedHumanTcpSupportTest.awaitServerInactive(oldServerInactive);
            completedInactiveOwner.set(completedOldServerInactive);
            Assert.assertSame(completedOldServerInactive.accepted(), retainedClient);
            Assert.assertSame(completedOldServerInactive.channel(), oldServerChannel);
            Assert.assertFalse(completedOldServerInactive.channel().isActive(),
                    "the exact pre-close accepted server channel reached inactive and drained");
            oldCaptureCount = oldRemote.gui().captures().size();
            Assert.assertEquals(retainedClient.getIndex(), 1);
            Assert.assertSame(disconnectedClients(server).get(retainedClient.getUsername()), retainedClient,
                    "the exact original server RemoteClient is parked under its username");
            Assert.assertTrue(reconnectTimers(server).containsKey(retainedClient.getUsername()),
                    "the parked client's ordinary timeout remains armed until login");

            long gateDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            gate = new ReconnectWorkerGate(server, gateDeadline);
            gate.awaitWorkers();
            replacement.gui().expectReconnectReplay();
            connect = new FutureTask<>(() -> { replacement.connect(); return null; });
            connectThread = new Thread(connect, "DanDan-history-reconnect-connect");
            connectThread.setDaemon(true);
            connectThread.start();
            connect.get(gate.remainingNanos(), TimeUnit.NANOSECONDS);
            replacement.installReconnectObservation(replacementObservation, gateDeadline);
            gate.releaseForInstalledTap();
            assertNoFailure(scenario);
            Assert.assertTrue(replacement.gui().awaitFull(15, TimeUnit.SECONDS));
            assertNoFailure(scenario);
            Assert.assertTrue(replacement.gui().awaitOpenView(15, TimeUnit.SECONDS));
            assertNoFailure(scenario);
            Assert.assertTrue(replacement.gui().awaitPostOpenFullState(15, TimeUnit.SECONDS));
            assertNoFailure(scenario);
            Assert.assertSame(server.getClientBySlotIndex(1), retainedClient);
            Assert.assertEquals(retainedClient.getIndex(), 1);
            Assert.assertEquals(replacement.clientUsername(), retainedClient.getUsername());
            Assert.assertFalse(reconnectTimers(server).containsKey(retainedClient.getUsername()));
            Assert.assertTrue(replacement.gui().awaitReconnectReplayPriority(15, TimeUnit.SECONDS),
                    "replacement received the exact held T4 UPKEEP prompt after the final full baseline");
            assertNoFailure(scenario);
            Channel replacementServerChannel = ScriptedHumanTcpSupport.serverChannel(retainedClient);
            Assert.assertNotNull(replacementServerChannel);
            Assert.assertTrue(replacementServerChannel.isActive());
            Assert.assertNotSame(replacementServerChannel, completedOldServerInactive.channel(),
                    "the resumed RemoteClient is attached to a new active server channel");
            Assert.assertSame(server.getClientBySlotIndex(1), retainedClient);
            serverTap = ScriptedHumanTcpSupport.ServerDecodedEventTap.install(replacementServerChannel, 1);
            Assert.assertEquals(replacement.gui().ownPlayerViewId(), scenario.privacyRemoteViewId());
            GameView replacementView = replacement.gui().getGameView();
            Assert.assertNotNull(replacementView);
            Assert.assertEquals(replacementView.getId(), scenario.privacyShuffleT4GameViewId(),
                    "replacement resumed Game N with its same GameView identity");
            Assert.assertEquals(replacementView.getTurn(), 4);
            Assert.assertEquals(replacementView.getPhase(), PhaseType.UPKEEP);
            Assert.assertEquals(replacementView.getPlayerTurn().getId(), scenario.privacyRemoteViewId());

            List<ScriptedHumanTcpSupport.Capture> captures = replacement.gui().captures();
            long firstFull = captures.stream().filter(c -> "setGameView".equals(c.callback()))
                    .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).min().orElse(-1);
            long open = captures.stream().filter(c -> "openView".equals(c.callback()))
                    .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).min().orElse(-1);
            long postOpenFull = captures.stream().filter(c -> "setGameView".equals(c.callback())
                            && c.callbackOrdinal() > open)
                    .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).min().orElse(-1);
            Assert.assertTrue(firstFull > 0 && firstFull < open && open < postOpenFull,
                    "replacement's full resync precedes openView and its post-open full baseline");
            List<ScriptedHumanTcpSupport.PreHandlerFact> preHandlerFacts = replacementObservation.facts();
            int firstPreFull = indexOfFact(preHandlerFacts, 0, "setGameView");
            int preOpen = indexOfFact(preHandlerFacts, 0, "openView");
            int postOpenPreFull = indexOfFact(preHandlerFacts, preOpen + 1, "setGameView");
            Assert.assertTrue(firstPreFull >= 0 && firstPreFull < preOpen && preOpen < postOpenPreFull,
                    "replacement pre-handler receipt independently retains full, openView, full order");
            ScriptedHumanTcpSupport.PreHandlerFact replacementPreBaseline = preHandlerFacts.get(postOpenPreFull);
            Assert.assertTrue(replacementPreBaseline.danDan() && replacementPreBaseline.turn() == 4
                            && "UPKEEP".equals(replacementPreBaseline.phase())
                            && replacementPreBaseline.turnPlayerViewId() == scenario.privacyRemoteViewId()
                            && replacementPreBaseline.playerViewIds().size() == 2
                            && replacementPreBaseline.playerViewIds().contains(scenario.privacyViewIdForSeat(0))
                            && replacementPreBaseline.playerViewIds().contains(scenario.privacyRemoteViewId()),
                    "replacement's decoded final baseline identifies the same two-player T4 UPKEEP Game");
            ScriptedHumanTcpSupport.GameViewReceipt replacementBaseline = replacement.gui().gameViewReceipts().stream()
                    .filter(receipt -> receipt.callbackOrdinal() == postOpenFull).findFirst().orElse(null);
            Assert.assertNotNull(replacementBaseline);
            Assert.assertEquals(replacementBaseline.gameViewId(), scenario.privacyShuffleT4GameViewId());
            Assert.assertNotNull(replacementBaseline.trackerIdentity(),
                    "replacement's full baseline has its own current tracker identity");
            Assert.assertSame(replacementView.getTracker(), replacementBaseline.trackerIdentity(),
                    "the retained applied replacement view is backed by its own baseline tracker");
            ScriptedHumanTcpSupport.PrivacyStateReceipt oldHeldState = scenario.privacyShuffleHistoryState();
            Assert.assertNotNull(oldHeldState);
            Assert.assertTrue(replacementBaseline.zonesByPlayer().equals(oldHeldState.zonesByPlayer()),
                    "replacement resync preserves the same N physical-state snapshot");
            List<ScriptedHumanTcpSupport.PriorityReceipt> appliedReplayPrompts = replacement.gui().priorityPrompts().stream()
                    .filter(prompt -> prompt.seat() == 1 && prompt.chooserViewId() == scenario.privacyRemoteViewId()
                            && prompt.turn() == 4 && prompt.phase() == PhaseType.UPKEEP
                            && prompt.turnPlayerViewId() == scenario.privacyRemoteViewId()
                            && prompt.callbackOrdinal() > postOpenFull
                            && (prompt.enabledOne() || prompt.enabledTwo()))
                    .toList();
            Assert.assertEquals(appliedReplayPrompts.size(), 1,
                    "one applied priority receipt identifies B's held prompt after its final baseline");
            ScriptedHumanTcpSupport.PriorityReceipt replayedPrompt = appliedReplayPrompts.get(0);
            List<ScriptedHumanTcpSupport.PreHandlerFact> matchingReplayFacts = new ArrayList<>();
            int decodedButtons = 0;
            int positiveEventIds = 0;
            int ownerMatches = 0;
            int labelMatches = 0;
            int enabledMatches = 0;
            for (int i = postOpenPreFull + 1; i < preHandlerFacts.size(); i++) {
                ScriptedHumanTcpSupport.PreHandlerFact fact = preHandlerFacts.get(i);
                if (!"updateButtons".equals(fact.method())) continue;
                decodedButtons++;
                if (fact.eventId() <= 0) continue;
                positiveEventIds++;
                if (fact.promptOwnerViewId() != scenario.privacyRemoteViewId()) continue;
                ownerMatches++;
                if (!fact.promptOne().trim().equals(replayedPrompt.one())
                        || !fact.promptTwo().trim().equals(replayedPrompt.two())) continue;
                labelMatches++;
                if (fact.promptOneEnabled() != replayedPrompt.enabledOne()
                        || fact.promptTwoEnabled() != replayedPrompt.enabledTwo()) continue;
                enabledMatches++;
                matchingReplayFacts.add(fact);
            }
            Assert.assertEquals(matchingReplayFacts.size(), 1,
                    "one decoded prompt event matches the applied held prompt after the post-open baseline"
                            + " (postBaselineButtons=" + decodedButtons + ", positiveIds=" + positiveEventIds
                            + ", ownerMatches=" + ownerMatches + ", labelMatches=" + labelMatches
                            + ", enabledMatches=" + enabledMatches + ")");
            int replayEventId = matchingReplayFacts.get(0).eventId();
            Assert.assertTrue(replayEventId > 0
                            && preHandlerFacts.stream().filter(fact -> fact.eventId() == replayEventId).count() == 1,
                    "the correlated replayed button prompt has one unique positive-ID wire event");
            int replayFactIndex = preHandlerFacts.indexOf(matchingReplayFacts.get(0));
            List<ScriptedHumanTcpSupport.PreHandlerFact> decodedButtonCallbacks = preHandlerFacts.stream()
                    .filter(fact -> "updateButtons".equals(fact.method())).toList();
            List<ScriptedHumanTcpSupport.Capture> receivedButtonCallbacks = replacement.gui().captures().stream()
                    .filter(capture -> "updateButtons".equals(capture.callback())).toList();
            Assert.assertEquals(receivedButtonCallbacks.size(), decodedButtonCallbacks.size(),
                    "each decoded button event has its corresponding replacement GUI capture");
            long replayButtonRank = preHandlerFacts.subList(0, replayFactIndex + 1).stream()
                    .filter(fact -> "updateButtons".equals(fact.method())).count() - 1;
            Assert.assertTrue(replayButtonRank >= 0 && replayButtonRank < receivedButtonCallbacks.size());
            ScriptedHumanTcpSupport.Capture receivedReplayButton = receivedButtonCallbacks.get((int) replayButtonRank);
            Assert.assertEquals(receivedReplayButton.sequence(), matchingReplayFacts.get(0).sequence(),
                    "the decoded replay event joins its exact replacement updateButtons callback by sequence");
            ScriptedHumanTcpSupport.PriorityReceipt correlatedReplayPriority = replacement.gui().priorityPrompts().stream()
                    .filter(receipt -> receipt.callbackOrdinal() == receivedReplayButton.callbackOrdinal())
                    .findFirst().orElseThrow(() -> new AssertionError("Missing priority receipt for decoded replay button"));
            Assert.assertEquals(correlatedReplayPriority, replayedPrompt,
                    "the decoded replay event joins the applied priority receipt for the same endpoint callback");
            int currentBaselineFactIndex = -1;
            for (int i = postOpenPreFull; i < replayFactIndex; i++) {
                if ("setGameView".equals(preHandlerFacts.get(i).method())) currentBaselineFactIndex = i;
            }
            Assert.assertTrue(currentBaselineFactIndex >= postOpenPreFull,
                    "the matching replay prompt follows a decoded full current-state callback");
            for (int i = currentBaselineFactIndex + 1; i < replayFactIndex; i++) {
                ScriptedHumanTcpSupport.PreHandlerFact between = preHandlerFacts.get(i);
                Assert.assertFalse("applyDelta".equals(between.method()) && between.changedObjectCount() > 0,
                        "no populated delta changed the decoded current baseline before the matching replay prompt");
            }
            ScriptedHumanTcpSupport.PreHandlerFact decodedCurrentBaseline = preHandlerFacts.get(currentBaselineFactIndex);
            Map<Integer, Map<ZoneType, List<Integer>>> decodedCurrentBaselineZones =
                    replacementObservation.decodedFullZones(decodedCurrentBaseline);
            Assert.assertTrue(decodedCurrentBaseline.danDan() && decodedCurrentBaseline.turn() == 4
                            && "UPKEEP".equals(decodedCurrentBaseline.phase())
                            && decodedCurrentBaseline.turnPlayerViewId() == scenario.privacyRemoteViewId()
                            && decodedCurrentBaseline.playerViewIds().size() == 2
                            && decodedCurrentBaseline.playerViewIds().contains(scenario.privacyViewIdForSeat(0))
                            && decodedCurrentBaseline.playerViewIds().contains(scenario.privacyRemoteViewId()),
                    "the latest decoded full before the replay prompt still describes Game N at remote T4 UPKEEP");
            List<ScriptedHumanTcpSupport.PreHandlerFact> decodedFullCallbacks = preHandlerFacts.stream()
                    .filter(fact -> "setGameView".equals(fact.method())).toList();
            List<ScriptedHumanTcpSupport.Capture> receivedFullCallbacks = replacement.gui().captures().stream()
                    .filter(capture -> "setGameView".equals(capture.callback())).toList();
            Assert.assertEquals(receivedFullCallbacks.size(), decodedFullCallbacks.size(),
                    "each decoded full callback has its corresponding replacement GUI capture");
            long currentFullRank = preHandlerFacts.subList(0, replayFactIndex + 1).stream()
                    .filter(fact -> "setGameView".equals(fact.method())).count() - 1;
            Assert.assertTrue(currentFullRank >= 0 && currentFullRank < receivedFullCallbacks.size());
            ScriptedHumanTcpSupport.Capture receivedCurrentBaseline = receivedFullCallbacks.get((int) currentFullRank);
            Assert.assertEquals(receivedCurrentBaseline.sequence(), decodedCurrentBaseline.sequence(),
                    "the decoded current full callback joins its exact replacement GUI receipt by sequence");
            ScriptedHumanTcpSupport.GameViewReceipt currentBaselineReceipt = replacement.gui().gameViewReceipts().stream()
                    .filter(receipt -> receipt.callbackOrdinal() == receivedCurrentBaseline.callbackOrdinal()
                            && "setGameView".equals(receipt.callback()))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing applied receipt for decoded current full baseline"));
            ScriptedHumanTcpSupport.GameViewReceipt latestAppliedFullBeforePrompt = replacement.gui().gameViewReceipts().stream()
                    .filter(receipt -> "setGameView".equals(receipt.callback())
                            && receipt.callbackOrdinal() < replayedPrompt.callbackOrdinal())
                    .max(Comparator.comparingLong(ScriptedHumanTcpSupport.GameViewReceipt::callbackOrdinal))
                    .orElseThrow(() -> new AssertionError("Missing applied full callback before replay prompt"));
            Assert.assertEquals(currentBaselineReceipt.callbackOrdinal(), latestAppliedFullBeforePrompt.callbackOrdinal(),
                    "the decoded current full is also the latest applied full before the replay prompt");
            Assert.assertTrue(currentBaselineReceipt.callbackOrdinal() >= replacementBaseline.callbackOrdinal(),
                    "the applied current baseline follows the replacement's initial post-open baseline");
            Assert.assertTrue(currentBaselineReceipt.callbackOrdinal() < replayedPrompt.callbackOrdinal(),
                    "the applied current baseline precedes the replacement's own priority receipt");
            Assert.assertEquals(currentBaselineReceipt.gameViewId(), replacementView.getId());
            Assert.assertSame(currentBaselineReceipt.trackerIdentity(), replacementView.getTracker(),
                    "the latest applied full baseline is still the replacement's current Game N tracker");
            Assert.assertTrue(currentBaselineReceipt.danDan(),
                    "the applied current baseline is a DanDan full view");
            Assert.assertTrue(scenario.isSamePrivacyShuffleT4PriorityInput(),
                    "the old exact B server input is still current immediately before replacement response");
            Map<Integer, Map<ZoneType, List<Integer>>> fencedReplacementZones = assertPrivacyReconnectKnowledgeParity(
                    hostGui, oldRemote.gui(), replacement.gui(), setup, oldHeldState.zonesByPlayer(), scenario,
                    replacement.channel(), decodedCurrentBaselineZones, currentBaselineReceipt.zonesByPlayer());
            String zoneEvidence = privacyZoneBaselineEvidence(decodedCurrentBaselineZones,
                    currentBaselineReceipt.zonesByPlayer(), fencedReplacementZones, oldHeldState.zonesByPlayer());
            Assert.assertTrue(decodedCurrentBaselineZones.equals(oldHeldState.zonesByPlayer()),
                    "the pre-beforeCall decoded full frame preserves the held Game N state; " + zoneEvidence);
            Assert.assertTrue(currentBaselineReceipt.zonesByPlayer().equals(oldHeldState.zonesByPlayer()),
                    "the latest applied full baseline preserves the held Game N state; " + zoneEvidence);

            long bPassesBeforeReplacement = scenario.actions().stream().filter(action ->
                    action.decision() == ScriptedHumanTcpSupport.Decision.PASS_PRIORITY
                            && action.seat() == 1 && action.chooserViewId() == scenario.privacyRemoteViewId()).count();
            serverTap.armSelectButtonOk();
            replacement.armSelectButtonOkObservation(replacementView.getId(), replayedPrompt.callbackOrdinal());
            replacement.gui().dispatchHeldReconnectPriorityPass();
            ScriptedHumanTcpSupport.ClientProtocolSendReceipt clientSend = replacement.clientProtocolSendReceipt();
            Assert.assertNotNull(clientSend, "the replacement emitted one actual client PASS event");
            Assert.assertEquals(clientSend.method(), ProtocolMethod.selectButtonOk);
            Assert.assertEquals(clientSend.gameViewId(), Integer.valueOf(replacementView.getId()));
            Assert.assertEquals(clientSend.promptCallbackOrdinal(), replayedPrompt.callbackOrdinal());
            long receiveDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (serverTap.receipt() == null && serverTap.failure() == null
                    && System.nanoTime() < receiveDeadline) Thread.sleep(5);
            Assert.assertNull(serverTap.failure(), "server decoded-event tap remained healthy");
            ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt serverReceive = serverTap.receipt();
            Assert.assertNotNull(serverReceive, "the server decoded the replacement's actual PASS event");
            Assert.assertEquals(serverReceive.eventId(), clientSend.eventId());
            Assert.assertEquals(serverReceive.method(), clientSend.method());
            Assert.assertEquals(serverReceive.gameViewId(), clientSend.gameViewId());
            Assert.assertEquals(serverReceive.seat(), 1);
            Assert.assertSame(serverReceive.channel(), ScriptedHumanTcpSupport.serverChannel(retainedClient));
            long bPassesAfterReplacement = scenario.actions().stream().filter(action ->
                    action.decision() == ScriptedHumanTcpSupport.Decision.PASS_PRIORITY
                            && action.seat() == 1 && action.chooserViewId() == scenario.privacyRemoteViewId()).count();
            Assert.assertEquals(bPassesAfterReplacement, bPassesBeforeReplacement + 1,
                    "the correlated replacement event produced exactly one B action receipt");
            Assert.assertTrue(replacementObservation.inboundByteCount() > 0,
                    "replacement pre-handler observer captured its own resumed endpoint traffic");
            Assert.assertFalse(replacementObservation.overflowed());
            Assert.assertNull(replacementObservation.failure());

            boolean hostContinuationObserved = scenario.awaitPrivacyHistoryReconnectHostHeld(30, TimeUnit.SECONDS);
            assertNoFailure(scenario);
            Assert.assertTrue(hostContinuationObserved,
                    "replacement B PASS advanced to the earliest actual host A T4 UPKEEP callback, which was held");
            Assert.assertTrue(scenario.isSamePrivacyHistoryReconnectHostInput(),
                    "host retains the exact same-Game A InputPassPriority after B's replacement response");
            Assert.assertFalse(scenario.isSamePrivacyShuffleT4PriorityInput(),
                    "the server consumed B's retained input and advanced to A's next priority");
            Assert.assertTrue(scenario.privacyHistoryReconnectHostCallbackOrdinal() > 0,
                    "host hold came from its actual endpoint-local callback ordinal");
            GameView hostHeld = hostGui.getGameView();
            Assert.assertNotNull(hostHeld);
            Assert.assertEquals(hostHeld.getId(), scenario.privacyShuffleT4GameViewId());
            Assert.assertEquals(hostHeld.getTurn(), 4);
            Assert.assertEquals(hostHeld.getPhase(), PhaseType.UPKEEP);
            Assert.assertEquals(hostHeld.getPlayerTurn().getId(), scenario.privacyRemoteViewId());
            Assert.assertEquals(hostHeld.getStack().size(), 0);
            Assert.assertSame(hostHeld.getTracker(), hostTrackerBeforeReconnect,
                    "host retained the same N tracker across replacement replay and priority progression");
            Map<Integer, Map<ZoneType, List<Integer>>> hostHeldZones = zoneSnapshot(hostHeld);
            for (PlayerView player : hostHeld.getPlayers()) {
                hostHeldZones.get(player.getId()).put(ZoneType.Exile, ids(player.getCards(ZoneType.Exile)));
            }
            Assert.assertTrue(hostHeldZones.equals(oldHeldState.zonesByPlayer()),
                    "the next host priority is the same N state after replacement B's pass");
            assertPrivacyReconnectKnowledgeParity(hostGui, oldRemote.gui(), replacement.gui(), setup,
                    oldHeldState.zonesByPlayer(), scenario, replacement.channel(), null, null);

            Assert.assertSame(replacement.clientProtocolSendReceipt(), clientSend,
                    "the exact N replacement send receipt remains retained through server consumption");
            Assert.assertSame(serverTap.receipt(), serverReceive,
                    "the exact N server decode receipt remains retained through host priority continuation");
            Assert.assertNull(replacement.clientProtocolObservationFailure());
            Assert.assertNull(serverTap.failure());
            replacement.disarmClientSelectButtonOkObservation();
            serverTap.disarmSelectButtonOk();
            Assert.assertSame(replacement.clientProtocolSendReceipt(), clientSend,
                    "closing the N observation window preserves its already-asserted receipt");
            Assert.assertSame(serverTap.receipt(), serverReceive,
                    "closing the N decoder window preserves its already-asserted receipt");

            LifecycleTrace terminalTrace = new LifecycleTrace();
            terminalTrace.expectPhysicalCardIds(setup.physicalCardIds());
            terminalTrace.attach(game);
            match.setEndGameHook(() -> terminalTrace.recordEngineReturn(game));
            hostGui.concedePrivacyHistoryReconnectAfterAssertions();
            Assert.assertTrue(hostGui.awaitActualTerminal(30, TimeUnit.SECONDS));
            assertNoFailure(scenario);
            Assert.assertTrue(replacement.gui().awaitActualTerminal(30, TimeUnit.SECONDS));
            assertNoFailure(scenario);
            Assert.assertTrue(terminalTrace.awaitOutcome(30, TimeUnit.SECONDS));
            Assert.assertTrue(terminalTrace.awaitEngineReturn(30, TimeUnit.SECONDS));
            Assert.assertEquals(terminalTrace.outcomeCount(), 1);
            Assert.assertSame(terminalTrace.game, game);
            Assert.assertTrue(terminalTrace.outcomeCardIdsMatch && terminalTrace.engineReturnCardIdsMatch);
            GameView terminalView = match.getGameView();
            Assert.assertNotNull(terminalView);
            Assert.assertTrue(terminalView.isGameOver());
            Assert.assertEquals(scenario.actions().stream().filter(action -> action.decision()
                    == ScriptedHumanTcpSupport.Decision.CONCEDE && action.seat() == 0
                    && action.chooserViewId() == scenario.privacyHostViewId()).count(), 1L);
            Assert.assertEquals(oldRemote.gui().captures().size(), oldCaptureCount,
                    "the old disconnected endpoint received no callbacks after it was parked");
            Assert.assertEquals(retainedClient.getSendErrorCount(), 0,
                    "same-client reconnect and terminal cleanup introduced no send errors");
            assertReadinessGateSummary(scenario, "privacy-history-reconnect");
            return new PrivacyTerminalReceipt(terminalView, inboundBytesAtOpening);
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            if (serverTap != null) {
                try { serverTap.close(); }
                catch (Throwable closeFailure) {
                    if (primary != null) primary.addSuppressed(closeFailure);
                    else {
                        if (closeFailure instanceof Exception exception) throw exception;
                        if (closeFailure instanceof Error error) throw error;
                        throw new RuntimeException(closeFailure);
                    }
                }
            }
            if (gate != null) gate.releaseUnconditionally();
            if (primary != null) {
                if (connect != null) connect.cancel(true);
                if (connectThread != null) {
                    try {
                        connectThread.join(1_000);
                        if (connectThread.isAlive()) primary.addSuppressed(
                                new AssertionError("Bounded history reconnect helper did not stop after cancellation"));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        primary.addSuppressed(interrupted);
                    }
                }
            }
        }
    }

    private static Set<String> privacyKnownNames(Game game, Set<Integer> knownIds) {
        Set<String> names = new HashSet<>();
        for (int cardId : knownIds) {
            Card card = game.findById(cardId);
            Assert.assertNotNull(card, "receipt-backed physical card identity belongs to this Game");
            names.add(card.getName());
        }
        return names;
    }

    private static Map<Integer, Set<Integer>> assertPrivacyLogRowsAtFirstObservation(ScriptedHumanTcpSupport.Scenario scenario,
            Game game, Set<Integer> privateIdentityIds, int gameViewId,
            List<ScriptedHumanTcpSupport.ScriptedGui> endpoints) {
        Map<Integer, Set<Integer>> earnedIdsByView = new LinkedHashMap<>();
        for (ScriptedHumanTcpSupport.ScriptedGui endpoint : endpoints) {
            int seat = endpoint.ownPlayerViewId() == scenario.privacyViewIdForSeat(0) ? 0
                    : endpoint.ownPlayerViewId() == scenario.privacyViewIdForSeat(1) ? 1 : -1;
            Assert.assertTrue(seat >= 0, "log observer belongs to its assigned privacy seat");
            List<ScriptedHumanTcpSupport.PrivacyLogObservation> observations = endpoint.privacyLogObservations().stream()
                    .filter(item -> item.gameViewId() == gameViewId)
                    .sorted(Comparator.comparingLong(ScriptedHumanTcpSupport.PrivacyLogObservation::observationOrdinal))
                    .toList();
            Assert.assertFalse(observations.isEmpty(), "each assigned endpoint has post-consumer Game N log observations");
            Assert.assertTrue(observations.stream().anyMatch(item -> !item.appendedRows().isEmpty()),
                    "each assigned endpoint consumed at least one actual Game N log row");
            Set<Integer> knownIds = new HashSet<>();
            Set<String> checkedTemporalForbiddenWitnesses = new HashSet<>();
            int expectedViewId = scenario.privacyViewIdForSeat(seat);
            for (ScriptedHumanTcpSupport.PrivacyLogObservation observation : observations) {
                Assert.assertEquals(observation.ownPlayerViewId(), expectedViewId,
                        "temporal knowledge is joined only within its assigned seat namespace");
                Assert.assertEquals(observation.gameViewId(), gameViewId,
                        "temporal knowledge is joined only within its exact GameView namespace");
                Assert.assertTrue(endpoint.captures().stream().anyMatch(capture ->
                                capture.callback().equals(observation.callback())
                                        && capture.callbackOrdinal() == observation.sourceCallbackOrdinal()
                                        && capture.sequence() == observation.sourceSequence()),
                        "post-consumer observation retains its exact source callback correlation");
                if (observation.parentCallbackOrdinal() > 0) {
                    Assert.assertTrue(endpoint.captures().stream().anyMatch(parent ->
                                    "applyDelta".equals(parent.callback())
                                            && parent.callbackOrdinal() == observation.parentCallbackOrdinal()
                                            && parent.sequence() == observation.parentSequence()),
                            "nested event observation retains its exact applying packet correlation");
                }
                knownIds.addAll(observation.ownHandIds());
                knownIds.addAll(observation.publicCardIds());
                for (ScriptedHumanTcpSupport.MulliganChoiceReceipt receipt : scenario.mulliganChoices()) {
                    if (receipt.gameViewId() == gameViewId && receipt.seat() == seat
                            && receipt.chooserViewId() == expectedViewId
                            && receipt.callbackOrdinal() <= observation.observationOrdinal()) knownIds.addAll(receipt.handIds());
                }
                for (ScriptedHumanTcpSupport.FreeRevealReceipt receipt : scenario.freeReveals()) {
                    if (receipt.gameViewId() != gameViewId || receipt.receivingSeat() != seat) continue;
                    if (receipt.getChoicesCallbackOrdinal() <= observation.observationOrdinal()) knownIds.addAll(receipt.cardIds());
                    else addLawfullyPermissionedEarlierIds(knownIds, observation, receipt.cardIds());
                }
                for (ScriptedHumanTcpSupport.KeepOpeningReceipt receipt : scenario.keepOpeningReceipts()) {
                    if (receipt.gameViewId() == gameViewId && receipt.seat() == seat
                            && receipt.chooserViewId() == expectedViewId
                            && receipt.callbackOrdinal() <= observation.observationOrdinal()) knownIds.addAll(receipt.handIds());
                }
                for (ScriptedHumanTcpSupport.LondonBottomDispatchReceipt receipt : scenario.londonBottomDispatches()) {
                    if (receipt.gameViewId() == gameViewId && receipt.seat() == seat
                            && receipt.chooserViewId() == expectedViewId
                            && receipt.promptOrdinal() <= observation.observationOrdinal()) {
                        knownIds.addAll(receipt.chooserHandIds());
                        knownIds.add(receipt.cardId());
                    }
                }
                for (ScriptedHumanTcpSupport.PrivacyChoiceReceipt receipt : scenario.privacyChoices()) {
                    if (receipt.seat() != seat || receipt.chooserViewId() != expectedViewId) continue;
                    if (receipt.callbackOrdinal() <= observation.observationOrdinal()) knownIds.addAll(receipt.cardIds());
                    else addLawfullyPermissionedEarlierIds(knownIds, observation, receipt.cardIds());
                }
                for (ScriptedHumanTcpSupport.PrivacyRevealReceipt receipt : scenario.privacyReveals()) {
                    if (receipt.seat() != seat || receipt.chooserViewId() != expectedViewId) continue;
                    if (receipt.callbackOrdinal() <= observation.observationOrdinal()) knownIds.addAll(receipt.cardIds());
                    else addLawfullyPermissionedEarlierIds(knownIds, observation, receipt.cardIds());
                }
                for (ScriptedHumanTcpSupport.PrivacyStackViewReceipt receipt : scenario.privacyStackViewReceipts()) {
                    if (receipt.seat() == seat && receipt.chooserViewId() == expectedViewId
                            && receipt.callbackOrdinal() <= observation.observationOrdinal()) {
                        knownIds.add(receipt.sourceCardId());
                    }
                }
                for (ScriptedHumanTcpSupport.Capture capture : endpoint.captures()) {
                    if (!"handleGameEvents".equals(capture.callback())
                            || capture.callbackOrdinal() > observation.observationOrdinal()) continue;
                    for (int publicSpellId : List.of(scenario.privacyTellingTimeId(),
                            scenario.privacyFengrafId(), scenario.privacyDaysUndoingId())) {
                        if (capture.eventFacts().stream().anyMatch(fact ->
                                (fact.eventClass().endsWith("GameEventSpellAbilityCast")
                                        && fact.scalarFacts().contains("sa.hostCardId=" + publicSpellId))
                                        || (fact.eventClass().endsWith("GameEventSpellResolved")
                                        && fact.scalarFacts().contains("spell.hostCardId=" + publicSpellId)))) {
                            knownIds.add(publicSpellId);
                        }
                    }
                }
                Set<String> forbiddenNames = privacyUnknownNames(game, privateIdentityIds,
                        privacyKnownNames(game, knownIds));
                if (!observation.appendedRows().isEmpty() && !forbiddenNames.isEmpty()) {
                    checkedTemporalForbiddenWitnesses.addAll(forbiddenNames);
                }
                for (ScriptedHumanTcpSupport.PrivacyLogRow row : observation.appendedRows()) {
                    for (String forbidden : forbiddenNames) {
                        Assert.assertFalse(row.message().contains(forbidden),
                                "first-observed log row at endpoint ordinal " + observation.observationOrdinal()
                                        + " does not reveal never-yet-known name " + forbidden
                                        + " (" + observation.callback() + ")");
                    }
                }
            }
            Assert.assertFalse(checkedTemporalForbiddenWitnesses.isEmpty(),
                    "the Game N temporal interval checked actual appended rows while unknown-name witnesses remained");
            earnedIdsByView.put(expectedViewId, Set.copyOf(knownIds));
        }
        return earnedIdsByView;
    }

    private static void addLawfullyPermissionedEarlierIds(Set<Integer> knownIds,
            ScriptedHumanTcpSupport.PrivacyLogObservation observation, List<Integer> exactReceiptIds) {
        for (int id : exactReceiptIds) {
            if (observation.permissionedPrivateCardIds().contains(id)) knownIds.add(id);
        }
    }

    private static void assertPrivacyLogObservationPositiveRows(
            List<ScriptedHumanTcpSupport.ScriptedGui> endpoints, int gameViewId,
            List<Integer> publicSpellIds, int resolvedSpellId, String resolvedText) {
        for (ScriptedHumanTcpSupport.ScriptedGui endpoint : endpoints) {
            List<ScriptedHumanTcpSupport.PrivacyLogRow> rows = endpoint.privacyLogObservations().stream()
                    .filter(observation -> observation.gameViewId() == gameViewId)
                    .flatMap(observation -> observation.appendedRows().stream()).toList();
            Assert.assertFalse(rows.isEmpty(), "captured consumer observations contain actual log rows");
            for (int cardId : publicSpellIds) {
                Assert.assertTrue(rows.stream().anyMatch(row -> row.type().equals(GameLogEntryType.STACK_ADD.name())
                                && row.sourceCardId() == cardId && !row.message().isBlank()),
                        "consumer observations contain the exact positive public STACK_ADD row for " + cardId);
            }
            Assert.assertTrue(rows.stream().anyMatch(row -> row.type().equals(GameLogEntryType.STACK_RESOLVE.name())
                            && row.sourceCardId() == resolvedSpellId && row.message().equals(resolvedText)),
                    "consumer observations contain the exact positive Day's Undoing STACK_RESOLVE row");
        }
    }

    private static void assertNextGameTemporalLogRows(Game game, Set<Integer> privateHands,
            List<ScriptedHumanTcpSupport.ScriptedGui> endpoints, int hostViewId, int remoteViewId) {
        int gameViewId = game.getView().getId();
        for (ScriptedHumanTcpSupport.ScriptedGui endpoint : endpoints) {
            int assignedViewId = endpoint.ownPlayerViewId();
            Assert.assertTrue(assignedViewId == hostViewId || assignedViewId == remoteViewId,
                    "N+1 log observer is bound to a player in the current Game");
            List<ScriptedHumanTcpSupport.PrivacyLogObservation> observations = endpoint.privacyLogObservations().stream()
                    .filter(receipt -> receipt.gameViewId() == gameViewId)
                    .sorted(Comparator.comparingLong(ScriptedHumanTcpSupport.PrivacyLogObservation::observationOrdinal))
                    .toList();
            Assert.assertFalse(observations.isEmpty(), "each N+1 endpoint retains current-Game log observations");
            Assert.assertTrue(observations.stream().anyMatch(receipt -> !receipt.appendedRows().isEmpty()),
                    "each N+1 endpoint consumed actual newly appended log rows");
            Assert.assertTrue(observations.stream().flatMap(receipt -> receipt.appendedRows().stream())
                            .anyMatch(row -> row.type().equals(GameLogEntryType.TURN.name()) && !row.message().isBlank()),
                    "N+1 temporal observations contain the actual positive TURN row");
            Set<Integer> knownIds = new HashSet<>();
            Set<String> temporalWitnesses = new HashSet<>();
            for (ScriptedHumanTcpSupport.PrivacyLogObservation observation : observations) {
                Assert.assertEquals(observation.ownPlayerViewId(), assignedViewId,
                        "N+1 observation remains in the assigned endpoint namespace");
                knownIds.addAll(observation.ownHandIds());
                knownIds.addAll(observation.publicCardIds());
                Set<String> forbiddenNames = privacyUnknownNames(game, privateHands,
                        privacyKnownNames(game, knownIds));
                if (!observation.appendedRows().isEmpty() && !forbiddenNames.isEmpty()) {
                    temporalWitnesses.addAll(forbiddenNames);
                }
                for (ScriptedHumanTcpSupport.PrivacyLogRow row : observation.appendedRows()) {
                    for (String forbidden : forbiddenNames) {
                        Assert.assertFalse(row.message().contains(forbidden),
                                "first-observed N+1 log row at endpoint ordinal " + observation.observationOrdinal()
                                        + " omits never-known opponent-hand name " + forbidden);
                    }
                }
            }
            Assert.assertFalse(temporalWitnesses.isEmpty(),
                    "N+1 temporal observations check a real appended row while an opponent-hand name remains unknown");
        }
    }

    private static Set<String> privacyUnknownNames(Game game, Iterable<Integer> candidateIds,
            Set<String> knownNames) {
        Set<String> names = new HashSet<>();
        for (int cardId : candidateIds) {
            Card card = game.findById(cardId);
            Assert.assertNotNull(card, "the actual hidden-zone witness belongs to this Game");
            if (!knownNames.contains(card.getName())) {
                names.add(card.getName());
            }
        }
        return names;
    }

    private static void assertPrivacyLogOmitsNames(GameView view, Set<String> forbiddenNames, String checkpoint) {
        Assert.assertNotNull(view.getGameLog(), checkpoint + " has its actual GameLog");
        List<String> entries = view.getGameLog().getAllEntries().stream()
                .map(forge.game.GameLogEntry::message).toList();
        Assert.assertFalse(entries.isEmpty(), checkpoint + " has actual log entries");
        for (String forbiddenName : forbiddenNames) {
            Assert.assertTrue(entries.stream().noneMatch(message -> message.contains(forbiddenName)),
                    checkpoint + " omits never-known card name " + forbiddenName);
        }
    }

    private static void assertPrivacyHistoryNextGame(boolean deltaSync,
            ScriptedHumanTcpSupport.Scenario scenario, DanDanDesktop desktop,
            ScriptedHumanTcpSupport replacement, ScriptedHumanTcpSupport oldRemote,
            HostedMatch hosted, RemoteClient retainedClient, FServerManager server,
            ScriptedHumanTcpSupport.ReconnectObservation reconnectObservation,
            PrivacyTerminalReceipt nTerminalReceipt, boolean privacyShuffleHistoryScenario) throws Throwable {
        Game oldGame = scenario.privacyGame();
        forge.game.Match retainedMatch = hosted.getMatch();
        List<ScriptedHumanTcpSupport.PrivacyPromptReceipt> gameNHostPrivacyPrompts =
                scenario.privacyPrompts().stream().filter(prompt -> prompt.seat() == 0).toList();
        List<ScriptedHumanTcpSupport.PriorityReceipt> gameNReplacementPriorityPrompts =
                replacement.gui().priorityPrompts();
        ScriptedHumanTcpSupport.ClientProtocolSendReceipt nReconnectSendReceipt =
                replacement.clientProtocolSendReceipt();
        Assert.assertNotNull(nReconnectSendReceipt,
                "the accepted N reconnect PASS receipt remains frozen before N+1 startup");
        Assert.assertNotNull(oldGame);
        Assert.assertTrue(oldGame.isGameOver());
        Assert.assertTrue(hosted.getMatch().getOutcomes().size() == 1);
        GameView oldHostView = desktop.hostGui.getGameView();
        GameView oldReplacementView = replacement.gui().getGameView();
        GameView parkedOldView = oldRemote.gui().getGameView();
        Assert.assertNotNull(oldHostView);
        Assert.assertNotNull(oldReplacementView);
        Assert.assertNotNull(parkedOldView);
        Object oldHostTracker = oldHostView.getTracker();
        Object oldReplacementTracker = oldReplacementView.getTracker();
        Object parkedOldTracker = parkedOldView.getTracker();
        int parkedOldGameViewId = parkedOldView.getId();
        int parkedCallbackCount = oldRemote.gui().captures().size();
        Channel replacementChannel = replacement.channel();
        Assert.assertNotNull(replacementChannel);
        Assert.assertTrue(replacementChannel.isActive());

        scenario.beginPrivacyHistoryNextGame(oldGame);
        hosted.setStartGameHook(null);
        desktop.hostGui.script(ScriptedHumanTcpSupport.Decision.START_SEAT_ZERO, null);
        desktop.hostGui.script(ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND, null);
        replacement.gui().script(ScriptedHumanTcpSupport.Decision.START_SEAT_ZERO, null);
        replacement.gui().script(ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND, null);
        desktop.hostGui.decideNextGame(NextGameDecision.CONTINUE);
        replacement.gui().decideNextGame(NextGameDecision.CONTINUE);
        boolean replacementPriorityHeld = scenario.awaitPrivacyHistoryNextGamePriority(30, TimeUnit.SECONDS);
        assertNoFailure(scenario);
        Assert.assertTrue(replacementPriorityHeld,
                "the new game's actual replacement-seat priority remains held after both Keeps");

        ScriptedHumanTcpSupport.PrivacyHistoryNextGamePriorityReceipt held =
                scenario.privacyHistoryNextGamePriority();
        Assert.assertNotNull(held);
        Game nextGame = held.game();
        Assert.assertNotSame(nextGame, oldGame);
        Assert.assertSame(hosted.getGame(), nextGame);
        Assert.assertSame(hosted.getMatch(), retainedMatch);
        Assert.assertEquals(hosted.getMatch().getOutcomes().size(), 1);
        Assert.assertEquals(nextGame.getView().getNumPlayedGamesInMatch(), 1);
        Assert.assertNotSame(nextGame.getTracker(), oldGame.getTracker());
        Assert.assertEquals(nextGame.getPlayers().size(), 2);
        Map<String, Integer> expectedMain = officialDanDanDeck().getMain().toFlatList().stream()
                .collect(java.util.stream.Collectors.toMap(PaperCard::getName, ignored -> 1, Integer::sum));
        Map<String, Integer> actualMain = nextGame.getCardsInGame().stream()
                .collect(java.util.stream.Collectors.toMap(Card::getName, ignored -> 1, Integer::sum));
        Assert.assertEquals(actualMain, expectedMain,
                "the actual new Game uses the official 80-card Main without sideboard cards");
        Assert.assertEquals(nextGame.getCardsInGame().size(), 80);
        Assert.assertEquals(nextGame.getCardsInGame().stream().map(Card::getId).distinct().count(), 80L);
        Assert.assertSame(nextGame.getPlayers().get(0).getZone(ZoneType.Library),
                nextGame.getPlayers().get(1).getZone(ZoneType.Library));
        Assert.assertSame(nextGame.getPlayers().get(0).getZone(ZoneType.Graveyard),
                nextGame.getPlayers().get(1).getZone(ZoneType.Graveyard));

        List<ScriptedHumanTcpSupport.PrivacyHistoryNextGameKeepReceipt> keeps =
                scenario.privacyHistoryNextGameKeeps();
        Assert.assertEquals(keeps.size(), 2);
        Assert.assertEquals(keeps.stream().map(ScriptedHumanTcpSupport.PrivacyHistoryNextGameKeepReceipt::seat)
                .collect(java.util.stream.Collectors.toSet()), Set.of(0, 1));
        Assert.assertTrue(keeps.stream().allMatch(keep -> keep.gameViewId() == nextGame.getView().getId()
                        && keep.handIds().size() == 7 && keep.trackerIdentity() != null
                        && keep.trackerOwnsPlayerView() && keep.controllerOwnsGame()),
                "both current endpoints made one assigned seven-card Keep in Game N+1");

        GameView hostView = desktop.hostGui.getGameView();
        GameView remoteView = replacement.gui().getGameView();
        Assert.assertNotNull(hostView);
        Assert.assertNotNull(remoteView);
        Assert.assertEquals(hostView.getId(), nextGame.getView().getId());
        Assert.assertEquals(remoteView.getId(), nextGame.getView().getId());
        Assert.assertNotSame(hostView.getTracker(), oldHostTracker);
        Assert.assertNotSame(remoteView.getTracker(), oldReplacementTracker);
        Assert.assertSame(oldRemote.gui().getGameView(), parkedOldView,
                "the parked old endpoint keeps its own unchanged stale GameView history");
        Assert.assertSame(oldRemote.gui().getGameView().getTracker(), parkedOldTracker);
        Assert.assertNotEquals(remoteView.getId(), parkedOldGameViewId,
                "the N+1 replacement render model uses the new GameView identity");
        Assert.assertNotSame(remoteView.getTracker(), parkedOldTracker,
                "the active replacement does not render through the parked old endpoint tracker");
        Assert.assertSame(hostView.getTracker(), keeps.stream().filter(keep -> keep.seat() == 0)
                .findFirst().orElseThrow().trackerIdentity());
        Assert.assertSame(remoteView.getTracker(), keeps.stream().filter(keep -> keep.seat() == 1)
                .findFirst().orElseThrow().trackerIdentity());
        Assert.assertTrue(hostView.isDanDan() && remoteView.isDanDan());
        Assert.assertEquals(remoteView.getNumPlayedGamesInMatch(), 1);
        int hostId = nextGame.getPlayers().get(0).getId();
        int remoteId = nextGame.getPlayers().get(1).getId();
        Assert.assertTrue(replacementChannel.isActive());
        Assert.assertSame(replacement.channel(), replacementChannel);
        Assert.assertEquals(oldRemote.gui().captures().size(), parkedCallbackCount,
                "the parked N endpoint receives no callbacks during the current replacement's N+1");

        long remoteOpen = replacement.gui().captures().stream()
                .filter(capture -> "openView".equals(capture.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        Assert.assertTrue(remoteOpen > 0);
        long hostOpen = desktop.hostGui.captures().stream()
                .filter(capture -> "openView".equals(capture.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        Assert.assertTrue(hostOpen > 0);
        long hostEnd = desktop.hostGui.captures().stream()
                .filter(capture -> "afterGameEnd".equals(capture.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        long remoteEnd = replacement.gui().captures().stream()
                .filter(capture -> "afterGameEnd".equals(capture.callback()))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
        Assert.assertTrue(hostEnd > 0 && remoteEnd > 0 && hostEnd < hostOpen && remoteEnd < remoteOpen,
                "N+1 openView follows each endpoint's terminal transition fence");
        Assert.assertTrue(gameNHostPrivacyPrompts.stream()
                        .allMatch(prompt -> prompt.callbackOrdinal() < hostEnd),
                "every archived Game N host privacy prompt precedes its own afterGameEnd fence");
        Assert.assertTrue(gameNReplacementPriorityPrompts.stream()
                        .allMatch(prompt -> prompt.callbackOrdinal() < remoteEnd),
                "every archived Game N replacement priority precedes its own afterGameEnd fence");
        List<ScriptedHumanTcpSupport.GameViewReceipt> remoteFulls = replacement.gui().gameViewReceipts().stream()
                .filter(receipt -> receipt.gameViewId() == nextGame.getView().getId()
                        && "setGameView".equals(receipt.callback())).toList();
        Assert.assertTrue(remoteFulls.stream().anyMatch(receipt -> receipt.callbackOrdinal() < remoteOpen)
                        && remoteFulls.stream().anyMatch(receipt -> receipt.callbackOrdinal() > remoteOpen),
                "replacement applies N+1 full state both before and after openView");
        ScriptedHumanTcpSupport.PrivacyHistoryNextGameKeepReceipt hostKeep = keeps.stream()
                .filter(keep -> keep.seat() == 0).findFirst().orElseThrow();
        ScriptedHumanTcpSupport.PrivacyHistoryNextGameKeepReceipt remoteKeep = keeps.stream()
                .filter(keep -> keep.seat() == 1).findFirst().orElseThrow();
        Assert.assertEquals(hostKeep.chooserViewId(), hostId);
        Assert.assertEquals(remoteKeep.chooserViewId(), remoteId);
        Assert.assertTrue(hostKeep.callbackOrdinal() > hostOpen);
        Assert.assertTrue(remoteKeep.callbackOrdinal() > remoteOpen);
        ScriptedHumanTcpSupport.GameViewReceipt hostBaseline = desktop.hostGui.gameViewReceipts().stream()
                .filter(receipt -> receipt.gameViewId() == nextGame.getView().getId()
                        && receipt.callbackOrdinal() < hostOpen && "setGameView".equals(receipt.callback()))
                .max(java.util.Comparator.comparingLong(ScriptedHumanTcpSupport.GameViewReceipt::callbackOrdinal))
                .orElseThrow(() -> new AssertionError("host lacks an applied pre-open N+1 full view"));
        Assert.assertSame(hostBaseline.trackerIdentity(), hostView.getTracker());
        Assert.assertTrue(hostBaseline.danDan());
        Assert.assertEquals(hostBaseline.zonesByPlayer().size(), 2);
        Assert.assertSame(hostKeep.trackerIdentity(), hostBaseline.trackerIdentity(),
                "the host's post-open Keep uses the tracker installed by its pre-open full view");
        ScriptedHumanTcpSupport.GameViewReceipt remoteState = replacement.gui().gameViewReceipts().stream()
                .filter(receipt -> receipt.gameViewId() == nextGame.getView().getId()
                        && receipt.callbackOrdinal() > remoteOpen
                        && receipt.callbackOrdinal() > remoteKeep.callbackOrdinal()
                        && receipt.callback().equals(deltaSync ? "applyDelta" : "setGameView")
                        && receipt.stateBearing() == deltaSync)
                .max(java.util.Comparator.comparingLong(ScriptedHumanTcpSupport.GameViewReceipt::callbackOrdinal))
                .orElseThrow(() -> new AssertionError("N+1 endpoint lacks its selected full/delta current-state receipt"));
        Assert.assertSame(remoteState.trackerIdentity(), remoteView.getTracker());
        Assert.assertEquals(remoteState.zonesByPlayer().size(), 2);
        List<Integer> hostHand = nextGame.getPlayers().get(0).getCardsIn(ZoneType.Hand).stream()
                .map(Card::getId).toList();
        List<Integer> remoteHand = nextGame.getPlayers().get(1).getCardsIn(ZoneType.Hand).stream()
                .map(Card::getId).toList();
        Assert.assertEquals(hostHand.size(), 7);
        Assert.assertEquals(remoteHand.size(), 7);
        Map<Integer, Map<ZoneType, List<Integer>>> hostZones = lifecycleZones(hostView);
        List<Integer> library = nextGame.getPlayers().get(0).getCardsIn(ZoneType.Library).stream()
                .map(Card::getId).toList();
        List<Integer> graveyard = nextGame.getPlayers().get(0).getCardsIn(ZoneType.Graveyard).stream()
                .map(Card::getId).toList();
        for (int playerId : List.of(hostId, remoteId)) {
            Assert.assertEquals(hostZones.get(playerId).get(ZoneType.Library), library);
            Assert.assertEquals(hostZones.get(playerId).get(ZoneType.Graveyard), graveyard);
            Assert.assertEquals(remoteState.zonesByPlayer().get(playerId).get(ZoneType.Library), library);
            Assert.assertEquals(remoteState.zonesByPlayer().get(playerId).get(ZoneType.Graveyard), graveyard);
        }
        PlayerView hostOwn = findPlayerView(hostView, hostId);
        PlayerView remoteOwn = findPlayerView(remoteView, remoteId);
        PlayerView hostOpponent = findPlayerView(hostView, remoteId);
        PlayerView remoteOpponent = findPlayerView(remoteView, hostId);
        Assert.assertEquals(hostOwn.getHand().stream().map(CardView::getId).toList(), hostHand);
        Assert.assertEquals(remoteOwn.getHand().stream().map(CardView::getId).toList(), remoteHand);
        Map<Integer, Set<Integer>> gameNPlusOneKnownIds = Map.of(0, new HashSet<>(), 1, new HashSet<>());
        gameNPlusOneKnownIds.get(0).addAll(hostKeep.handIds());
        gameNPlusOneKnownIds.get(1).addAll(remoteKeep.handIds());
        Set<String> hostNPlusOneKnownNames = privacyKnownNames(nextGame, gameNPlusOneKnownIds.get(0));
        Set<String> remoteNPlusOneKnownNames = privacyKnownNames(nextGame, gameNPlusOneKnownIds.get(1));
        Set<String> hostNPlusOneForbiddenNames = privacyUnknownNames(nextGame, remoteKeep.handIds(),
                hostNPlusOneKnownNames);
        Set<String> remoteNPlusOneForbiddenNames = privacyUnknownNames(nextGame, hostKeep.handIds(),
                remoteNPlusOneKnownNames);
        Assert.assertFalse(hostNPlusOneForbiddenNames.isEmpty(),
                "actual N+1 fixed-seed Keeps supply an unknown opponent identity for host");
        Assert.assertFalse(remoteNPlusOneForbiddenNames.isEmpty(),
                "actual N+1 fixed-seed Keeps supply an unknown opponent identity for remote");
        Set<Integer> nPlusOnePrivateHands = new HashSet<>(hostKeep.handIds());
        nPlusOnePrivateHands.addAll(remoteKeep.handIds());
        assertNextGameTemporalLogRows(nextGame, nPlusOnePrivateHands,
                List.of(desktop.hostGui, replacement.gui()), hostId, remoteId);
        Assert.assertTrue(hostView.getGameLog().getAllEntries().stream().anyMatch(entry ->
                        entry.type() == GameLogEntryType.TURN && !entry.message().isBlank()),
                "the fresh N+1 host GameLog contains its actual current turn entry");
        Assert.assertTrue(remoteView.getGameLog().getAllEntries().stream().anyMatch(entry ->
                        entry.type() == GameLogEntryType.TURN && !entry.message().isBlank()),
                "the fresh N+1 remote GameLog contains its actual current turn entry");
        assertPrivacyLogOmitsNames(hostView, hostNPlusOneForbiddenNames,
                "fresh Game N+1 host log at the held priority fence");
        assertPrivacyLogOmitsNames(remoteView, remoteNPlusOneForbiddenNames,
                "fresh Game N+1 remote log at the held priority fence");
        ScriptedHumanTcpSupport.PriorityReceipt heldPriority = replacement.gui().priorityPrompts().stream()
                .filter(prompt -> prompt.callbackOrdinal() == held.callbackOrdinal()).findFirst().orElseThrow();
        for (String forbiddenName : remoteNPlusOneForbiddenNames) {
            Assert.assertFalse(heldPriority.one().contains(forbiddenName)
                            || heldPriority.two().contains(forbiddenName),
                    "the actual N+1 held priority labels omit never-known opposing hand identity " + forbiddenName);
        }
        Assert.assertTrue(hostOwn.getHand().stream().allMatch(card -> card.canBeShownTo(hostOwn)));
        Assert.assertTrue(remoteOwn.getHand().stream().allMatch(card -> card.canBeShownTo(remoteOwn)));
        Assert.assertTrue(hostOpponent.getHand().stream().noneMatch(card -> card.canBeShownTo(hostOwn)
                || card.mayPlayerLook(hostOwn)));
        Assert.assertTrue(remoteOpponent.getHand().stream().noneMatch(card -> card.canBeShownTo(remoteOwn)
                || card.mayPlayerLook(remoteOwn)));
        Set<Integer> allCurrentHandIds = new HashSet<>(hostHand);
        allCurrentHandIds.addAll(remoteHand);
        Assert.assertEquals(allCurrentHandIds.size(), 14);
        for (int cardId : allCurrentHandIds) {
            assertCurrentPrivateHandPermission(cardId, hostHand, remoteHand,
                    findCardView(hostView, cardId), findCardView(remoteView, cardId), hostOwn, remoteOwn);
        }
        for (Card card : nextGame.getPlayers().get(0).getCardsIn(ZoneType.Library)) {
            CardView hostCard = findCardView(hostView, card.getId());
            CardView remoteCard = findCardView(remoteView, card.getId());
            Assert.assertNotNull(hostCard);
            Assert.assertNotNull(remoteCard);
            Assert.assertFalse(hostCard.mayPlayerLook(hostOwn) || hostCard.canBeShownTo(hostOwn));
            Assert.assertFalse(remoteCard.mayPlayerLook(remoteOwn) || remoteCard.canBeShownTo(remoteOwn));
        }

        Assert.assertEquals(held.seat(), 1);
        Assert.assertSame(held.game(), nextGame);
        Assert.assertEquals(held.gameViewId(), remoteView.getId());
        Assert.assertSame(held.controller(), nextGame.getPlayers().get(1).getController());
        Assert.assertTrue(held.endpointController() instanceof NetGameController);
        Assert.assertSame(held.input(), held.controller().getInputProxy().getInput());
        Assert.assertSame(held.input(), held.controller().getInputQueue().getInput());
        Assert.assertEquals(held.input().getOwner().getId(), remoteId);
        Assert.assertSame(replacement.clientProtocolSendReceipt(), nReconnectSendReceipt,
                "N+1 opening does not overwrite the frozen N reconnect send receipt");
        Assert.assertTrue(replacement.gui().priorityPrompts().stream().anyMatch(prompt ->
                prompt.seat() == 1 && prompt.chooserViewId() == remoteId
                        && prompt.callbackOrdinal() == held.callbackOrdinal()));
        Assert.assertTrue(held.callbackOrdinal() > remoteKeep.callbackOrdinal());

        Channel serverChannel = ScriptedHumanTcpSupport.serverChannel(retainedClient);
        ScriptedHumanTcpSupport.ServerDecodedEventTap serverTap =
                ScriptedHumanTcpSupport.ServerDecodedEventTap.install(serverChannel, 1);
        LifecycleTrace trace = new LifecycleTrace();
        trace.expectPhysicalCardIds(nextGame.getCardsInGame().stream().map(Card::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        trace.attach(nextGame);
        hosted.setEndGameHook(() -> trace.recordEngineReturn(nextGame));
        Throwable primary = null;
        try {
            serverTap.armSelectButtonOk();
            replacement.armPrivacyHistoryNextGamePassObservation(held.gameViewId(), held.callbackOrdinal());
            scenario.armPrivacyHistoryNextGamePass();
            FThreads.invokeInEdtAndWait(() -> {
                GameView current = replacement.gui().getGameView();
                PlayerView own = findPlayerView(current, replacement.gui().ownPlayerViewId());
                IGameController controller = own == null ? null : replacement.gui().getGameController(own);
                if (current == null || current.getId() != held.gameViewId()
                        || controller != held.endpointController()
                        || current.getTracker() != remoteState.trackerIdentity()
                        || held.input() != held.controller().getInputProxy().getInput()
                        || held.input() != held.controller().getInputQueue().getInput()) {
                    throw new AssertionError("Replacement lost its exact N+1 current priority before PASS");
                }
                controller.selectButtonOk();
            });
            Assert.assertTrue(serverTap.awaitReceipt(10, TimeUnit.SECONDS),
                    "the N+1 actual replacement PASS reached the server decoder");
            ScriptedHumanTcpSupport.ClientProtocolSendReceipt send = replacement.clientSelectButtonOkReceipt();
            ScriptedHumanTcpSupport.ServerProtocolReceiveReceipt receive = serverTap.receipt();
            Assert.assertNotNull(send);
            Assert.assertNotNull(receive);
            Assert.assertEquals(send.method(), ProtocolMethod.selectButtonOk);
            Assert.assertEquals(receive.method(), ProtocolMethod.selectButtonOk);
            Assert.assertEquals(send.gameViewId(), held.gameViewId());
            Assert.assertEquals(receive.gameViewId(), held.gameViewId());
            Assert.assertEquals(send.eventId(), receive.eventId());
            Assert.assertEquals(receive.seat(), 1);
            Assert.assertSame(receive.channel(), serverChannel);
            Assert.assertEquals(send.promptCallbackOrdinal(), held.callbackOrdinal());
            Assert.assertNull(replacement.clientProtocolObservationFailure());
            Assert.assertNull(serverTap.failure());
            boolean hostPriorityInstalled = scenario.awaitPrivacyHistoryNextGameNextPriority(30, TimeUnit.SECONDS);
            assertNoFailure(scenario);
            Assert.assertTrue(hostPriorityInstalled,
                    "server consumed B's N+1 PASS and installed the next host priority input");
            ScriptedHumanTcpSupport.PrivacyHistoryNextGamePriorityReceipt next =
                    scenario.privacyHistoryNextGameNextPriority();
            Assert.assertNotNull(next);
            Assert.assertEquals(next.seat(), 0);
            Assert.assertSame(next.game(), nextGame);
            Assert.assertNotSame(next.input(), held.input());
            Assert.assertSame(next.endpointController(), next.controller());
            Assert.assertTrue(desktop.hostGui.captures().stream().anyMatch(capture ->
                    "updateButtons".equals(capture.callback())
                            && capture.callbackOrdinal() == next.callbackOrdinal()));
            ScriptedHumanTcpSupport.PriorityReceipt nextHostPriority = desktop.hostGui.priorityPrompts().stream()
                    .filter(promptReceipt -> promptReceipt.callbackOrdinal() == next.callbackOrdinal())
                    .findFirst().orElseThrow();
            for (String forbiddenName : hostNPlusOneForbiddenNames) {
                Assert.assertFalse(nextHostPriority.one().contains(forbiddenName)
                                || nextHostPriority.two().contains(forbiddenName),
                        "the actual N+1 host priority labels omit never-known opposing hand identity " + forbiddenName);
            }
            Assert.assertSame(next.input(), next.controller().getInputProxy().getInput());
            Assert.assertSame(next.input(), next.controller().getInputQueue().getInput());
            Assert.assertEquals(next.input().getOwner().getId(), hostId);
            assertNoFailure(scenario);
            next.controller().concede();
            boolean nPlusOneTerminalsReceived =
                    scenario.awaitPrivacyHistoryNextGameTerminals(30, TimeUnit.SECONDS);
            assertNoFailure(scenario);
            Assert.assertTrue(nPlusOneTerminalsReceived,
                    "both N+1 endpoints received their own fresh terminal callback receipts");
            List<ScriptedHumanTcpSupport.PrivacyHistoryNextGameTerminalReceipt> terminals =
                    scenario.privacyHistoryNextGameTerminals();
            Assert.assertEquals(terminals.size(), 2);
            Assert.assertEquals(terminals.stream().map(
                    ScriptedHumanTcpSupport.PrivacyHistoryNextGameTerminalReceipt::seat)
                    .collect(java.util.stream.Collectors.toSet()), Set.of(0, 1));
            Assert.assertTrue(terminals.stream().allMatch(terminal -> terminal.gameViewId() == nextGame.getView().getId()
                    && terminal.callbackOrdinal() > 0 && terminal.gameOver() && terminal.playedGames() == 1));
            Assert.assertTrue(nextGame.isGameOver());
            Assert.assertTrue(desktop.hostGui.captures().stream().anyMatch(capture ->
                    capture.callback().equals("finishGame") && capture.callbackOrdinal() == terminals.stream()
                            .filter(terminal -> terminal.seat() == 0).findFirst().orElseThrow().callbackOrdinal()));
            Assert.assertTrue(replacement.gui().captures().stream().anyMatch(capture ->
                    capture.callback().equals("finishGame") && capture.callbackOrdinal() == terminals.stream()
                            .filter(terminal -> terminal.seat() == 1).findFirst().orElseThrow().callbackOrdinal()));
            Assert.assertTrue(trace.awaitOutcome(30, TimeUnit.SECONDS));
            Assert.assertTrue(trace.awaitEngineReturn(30, TimeUnit.SECONDS));
            Assert.assertEquals(trace.outcomeCount(), 1);
            Assert.assertSame(trace.game, nextGame);
            Assert.assertTrue(trace.outcomeCardIdsMatch && trace.engineReturnCardIdsMatch);
            Assert.assertTrue(nextGame.isGameOver());
            Assert.assertSame(hosted.getGame(), nextGame,
                    "the returned terminal Game N+1 remains current until its own QUIT cleanup decision");
            Assert.assertEquals(hosted.getMatch().getOutcomes().size(), 2);
            GameView currentHostView = desktop.hostGui.getGameView();
            GameView currentRemoteView = replacement.gui().getGameView();
            Assert.assertNotNull(currentHostView);
            Assert.assertNotNull(currentRemoteView);
            Assert.assertEquals(currentHostView.getId(), nextGame.getView().getId());
            Assert.assertEquals(currentRemoteView.getId(), nextGame.getView().getId());
            Assert.assertTrue(desktop.hostGui.getGameController(findPlayerView(currentHostView, hostId))
                    instanceof PlayerControllerHuman,
                    "current host QUIT is sent through its assigned N+1 controller");
            Assert.assertTrue(replacement.gui().getGameController(findPlayerView(currentRemoteView, remoteId))
                    instanceof NetGameController,
                    "remote N+1 view remains bound to its current network controller through cleanup");
            int hostEndsBeforeQuit = countAfterGameEnd(desktop.hostGui.captures());
            int remoteEndsBeforeQuit = countAfterGameEnd(replacement.gui().captures());
            int decisionsBeforeQuit = scenario.nextGameDecisions().size();
            Assert.assertEquals(hostEndsBeforeQuit, 1,
                    "Game N's afterGameEnd is retained before N+1 cleanup");
            Assert.assertEquals(remoteEndsBeforeQuit, 1,
                    "replacement Game N's afterGameEnd is retained before N+1 cleanup");
            desktop.hostGui.decideNextGame(NextGameDecision.QUIT);
            awaitLifecycleCondition(scenario, () -> hosted.getGame() == null
                            && countAfterGameEnd(desktop.hostGui.captures()) == hostEndsBeforeQuit + 1
                            && countAfterGameEnd(replacement.gui().captures()) == remoteEndsBeforeQuit + 1,
                    30, "current N+1 host QUIT performs the ordinary match teardown for both endpoints");
            List<ScriptedHumanTcpSupport.NextGameDecisionReceipt> quitReceipts =
                    scenario.nextGameDecisions().subList(decisionsBeforeQuit, scenario.nextGameDecisions().size());
            Assert.assertEquals(quitReceipts.size(), 1);
            Assert.assertEquals(quitReceipts.get(0).seat(), 0);
            Assert.assertEquals(quitReceipts.get(0).chooserViewId(), hostId);
            Assert.assertEquals(quitReceipts.get(0).gameViewId(), nextGame.getView().getId());
            Assert.assertEquals(quitReceipts.get(0).decision(), NextGameDecision.QUIT);
            Assert.assertSame(hosted.getMatch(), retainedMatch);
            Assert.assertEquals(hosted.getMatch().getOutcomes().size(), 2);
            assertTellingTimeFengrafPrivacyTerminalCase(deltaSync, scenario, desktop, replacement, hosted, server,
                    reconnectObservation, nTerminalReceipt, privacyShuffleHistoryScenario);
            Assert.assertEquals(oldRemote.gui().captures().size(), parkedCallbackCount);
            Assert.assertTrue(replacementChannel.isActive());
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            try { serverTap.close(); }
            catch (Throwable closeFailure) {
                if (primary != null) primary.addSuppressed(closeFailure);
                else throw closeFailure;
            }
        }
    }

    private static void assertReconnectZones(Map<Integer, Map<ZoneType, List<Integer>>> before,
            Map<Integer, Map<ZoneType, List<Integer>>> after, int hostId, int remoteId, String label) {
        Assert.assertEquals(after.get(hostId).get(ZoneType.Library), before.get(hostId).get(ZoneType.Library),
                label + " preserves ordered common library IDs");
        Assert.assertEquals(after.get(remoteId).get(ZoneType.Library), before.get(remoteId).get(ZoneType.Library),
                label + " preserves the same common library in both seat views");
        Assert.assertEquals(after.get(hostId).get(ZoneType.Graveyard), before.get(hostId).get(ZoneType.Graveyard));
        Assert.assertEquals(after.get(remoteId).get(ZoneType.Graveyard), before.get(remoteId).get(ZoneType.Graveyard));
        Assert.assertEquals(after.get(remoteId).get(ZoneType.Hand), before.get(remoteId).get(ZoneType.Hand),
                label + " preserves the reconnecting seat's own hand IDs");
    }

    private static int indexOfFact(List<ScriptedHumanTcpSupport.PreHandlerFact> facts, int start, String method) {
        for (int i = Math.max(0, start); i < facts.size(); i++) if (method.equals(facts.get(i).method())) return i;
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, RemoteClient> disconnectedClients(FServerManager server) throws ReflectiveOperationException {
        Field field = FServerManager.class.getDeclaredField("disconnectedClients");
        field.setAccessible(true);
        return (Map<String, RemoteClient>) field.get(server);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Timer> reconnectTimers(FServerManager server) throws ReflectiveOperationException {
        Field field = FServerManager.class.getDeclaredField("reconnectTimers");
        field.setAccessible(true);
        return (Map<String, Timer>) field.get(server);
    }

    private static final class ReconnectWorkerGate {
        private final long deadlineNanos;
        private final CountDownLatch arrived;
        private final CountDownLatch exited;
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final AtomicBoolean expired = new AtomicBoolean();
        private final EventLoopGroup group;
        private final int workerCount;
        private boolean released;

        ReconnectWorkerGate(FServerManager server, long deadlineNanos) throws ReflectiveOperationException {
            this.deadlineNanos = deadlineNanos;
            Field field = FServerManager.class.getDeclaredField("workerGroup");
            field.setAccessible(true);
            group = (EventLoopGroup) field.get(server);
            if (group == null) throw new AssertionError("Server worker group is not running");
            int count = 0;
            for (EventExecutor ignored : group) count++;
            if (count == 0) throw new AssertionError("Server worker group has no event loops");
            workerCount = count;
            arrived = new CountDownLatch(count);
            exited = new CountDownLatch(count);
            try {
                for (EventExecutor loop : group) loop.execute(() -> {
                    arrived.countDown();
                    try {
                        long remaining = deadlineNanos - System.nanoTime();
                        if (remaining <= 0 || !release.await(remaining, TimeUnit.NANOSECONDS)) {
                            synchronized (this) {
                                if (!released) {
                                    expired.set(true);
                                    failure.compareAndSet(null, new TimeoutException("Server worker gate expired"));
                                }
                            }
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        synchronized (this) {
                            if (!released) {
                                expired.set(true);
                                failure.compareAndSet(null, error);
                            }
                        }
                    } finally { exited.countDown(); }
                });
            } catch (RuntimeException submissionFailure) {
                release.countDown();
                throw submissionFailure;
            }
        }

        long remainingNanos() {
            long remaining = deadlineNanos - System.nanoTime();
            if (remaining <= 0) throw new AssertionError("Reconnect gate's single five-second deadline expired");
            return remaining;
        }

        void awaitWorkers() throws InterruptedException {
            Assert.assertTrue(arrived.await(remainingNanos(), TimeUnit.NANOSECONDS),
                    "every server worker positively reached the reconnect gate within the shared deadline");
            assertHeld();
        }

        void assertHeld() {
            if (expired.get() || failure.get() != null || arrived.getCount() != 0 || exited.getCount() != workerCount) {
                throw new AssertionError("Reconnect capture gate expired or failed before tap installation", failure.get());
            }
            remainingNanos();
        }

        synchronized void releaseForInstalledTap() {
            assertHeld();
            released = true;
            release.countDown();
        }

        synchronized void releaseUnconditionally() { released = true; release.countDown(); }
    }

    private static void assertDanDanView(GameView view, int hostId, int remoteId) {
        assertDanDanIdentity(view, hostId, remoteId);
        assertDanDanZones(zoneSnapshot(view), hostId, remoteId);
    }

    private static void assertDanDanIdentity(GameView view, int hostId, int remoteId) {
        Assert.assertNotNull(view);
        Assert.assertNull(view.getGame(), "received view has no local Game dependency");
        Assert.assertNull(view.getMatch(), "received view has no local Match dependency");
        Assert.assertTrue(view.isDanDan(), "received GameView identifies the applied DanDan variant");
        PlayerView host = findPlayerView(view, hostId);
        PlayerView remote = findPlayerView(view, remoteId);
        Assert.assertNotNull(host, "host seat id resolves to its own received PlayerView");
        Assert.assertNotNull(remote, "remote seat id resolves to its own received PlayerView");
        Assert.assertNotEquals(host.getId(), remote.getId());
    }

    private static void assertDanDanZonesForCase(Map<Integer, Map<ZoneType, List<Integer>>> zones,
            int hostId, int remoteId, ScriptedHumanTcpSupport.Scenario scenario, boolean mulliganScenario) {
        if (scenario.privacyMulliganPrefixScenario()) return; // this composite checks the pre-setup London and post-setup states separately
        if (mulliganScenario) assertDanDanMulliganZones(zones, hostId, remoteId, scenario);
        else assertDanDanZones(zones, hostId, remoteId);
    }

    private static void assertDanDanMulliganZones(Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer,
            int hostId, int remoteId, ScriptedHumanTcpSupport.Scenario scenario) {
        Map<ZoneType, List<Integer>> host = zonesByPlayer.get(hostId);
        Map<ZoneType, List<Integer>> remote = zonesByPlayer.get(remoteId);
        Assert.assertNotNull(host, "host assigned PlayerView appears in detached mulligan snapshot");
        Assert.assertNotNull(remote, "remote assigned PlayerView appears in detached mulligan snapshot");
        List<Integer> hostLibrary = host.getOrDefault(ZoneType.Library, List.of());
        List<Integer> remoteLibrary = remote.getOrDefault(ZoneType.Library, List.of());
        Assert.assertEquals(hostLibrary.size(), 67, "one shared library contains the 67 undealt cards");
        Assert.assertEquals(remoteLibrary, hostLibrary, "both seat views expose the same ordered shared library");
        List<Integer> hostGraveyard = host.getOrDefault(ZoneType.Graveyard, List.of());
        List<Integer> remoteGraveyard = remote.getOrDefault(ZoneType.Graveyard, List.of());
        Assert.assertEquals(hostGraveyard, List.of());
        Assert.assertEquals(remoteGraveyard, hostGraveyard, "shared graveyard views match");
        int freeSeat = scenario.mulliganChoices().stream()
                .filter(choice -> choice.decision() == ScriptedHumanTcpSupport.Decision.FREE_MULLIGAN)
                .mapToInt(ScriptedHumanTcpSupport.MulliganChoiceReceipt::seat).findFirst().orElse(-1);
        Assert.assertTrue(freeSeat == 0 || freeSeat == 1, "a real seat owns the free-mulligan hand");
        int hostHandSize = freeSeat == 0 ? 7 : 6;
        int remoteHandSize = freeSeat == 1 ? 7 : 6;
        List<Integer> hostHand = host.getOrDefault(ZoneType.Hand, List.of());
        List<Integer> remoteHand = remote.getOrDefault(ZoneType.Hand, List.of());
        Assert.assertEquals(hostHand.size(), hostHandSize, "host retains the expected post-London hand size");
        Assert.assertEquals(remoteHand.size(), remoteHandSize, "remote retains the expected post-London hand size");
        Assert.assertTrue(Collections.disjoint(new HashSet<>(hostHand), new HashSet<>(remoteHand)),
                "private hands contain distinct physical card IDs");
        Set<Integer> physicalIds = new HashSet<>();
        for (Integer id : hostLibrary) Assert.assertTrue(physicalIds.add(id), "library has unique card ID " + id);
        for (Integer id : hostGraveyard) Assert.assertTrue(physicalIds.add(id), "graveyard has unique card ID " + id);
        for (Integer id : hostHand) Assert.assertTrue(physicalIds.add(id), "host hand has unique card ID " + id);
        for (Integer id : remoteHand) Assert.assertTrue(physicalIds.add(id), "remote hand has unique card ID " + id);
        Assert.assertEquals(physicalIds.size(), 80, "one copy of the shared library plus both hands conserves all 80 physical cards");
        for (ScriptedHumanTcpSupport.MulliganChoiceReceipt choice : scenario.mulliganChoices()) {
            for (Integer id : choice.handIds()) {
                Assert.assertTrue(physicalIds.contains(id), "initially dealt card returned or remained in a final zone: " + id);
            }
        }
        Assert.assertEquals(scenario.londonBottomDispatches().size(), 1, "one validated London bottom dispatch is retained");
        Assert.assertEquals(hostLibrary.get(hostLibrary.size() - 1), scenario.londonBottomDispatches().get(0).cardId(),
                "the one actual London selection is at the common library bottom");
    }

    private static void assertMulliganReceipts(ScriptedHumanTcpSupport.Scenario scenario,
            Map<Integer, Map<ZoneType, List<Integer>>> zones, int hostViewId, int remoteViewId) {
        List<ScriptedHumanTcpSupport.MulliganChoiceReceipt> choices = scenario.mulliganChoices();
        Assert.assertEquals(choices.size(), 2, "both players made one actual initial mulligan choice");
        ScriptedHumanTcpSupport.MulliganChoiceReceipt free = choices.stream()
                .filter(choice -> choice.decision() == ScriptedHumanTcpSupport.Decision.FREE_MULLIGAN)
                .findFirst().orElseThrow();
        ScriptedHumanTcpSupport.MulliganChoiceReceipt regular = choices.stream()
                .filter(choice -> choice.decision() == ScriptedHumanTcpSupport.Decision.REGULAR_MULLIGAN)
                .findFirst().orElseThrow();
        Assert.assertEquals(free.handIds().size(), 7);
        Assert.assertEquals(free.landCount(), 0, "controlled opening gives the free chooser seven nonlands");
        Assert.assertEquals(regular.handIds().size(), 7);
        Assert.assertTrue(free.callbackOrdinal() > 0 && regular.callbackOrdinal() > 0);
        Assert.assertTrue(regular.callback().equals("showOptionDialog") || regular.callback().equals("updateButtons"),
                "regular choice came from the real eligible dialog or ineligible Keep/Mulligan callback");
        Assert.assertEquals(scenario.freeReveals().size(), 1, "one free hand produces one opponent reveal receipt");
        ScriptedHumanTcpSupport.FreeRevealReceipt reveal = scenario.freeReveals().get(0);
        Assert.assertEquals(reveal.receivingSeat(), regular.seat(), "only the non-owner GUI receives the reveal");
        Assert.assertEquals(new HashSet<>(reveal.cardIds()), new HashSet<>(free.handIds()),
                "opponent getChoices receipt contains the owner's actual initial hand IDs");
        Assert.assertEquals(reveal.cardIds().size(), free.handIds().size(), "reveal receipt has no repeated card ID");
        Assert.assertEquals(scenario.londonBottomDispatches().size(), 1, "regular mulligan drove one validated London bottom dispatch");
        ScriptedHumanTcpSupport.LondonBottomDispatchReceipt bottom = scenario.londonBottomDispatches().get(0);
        Assert.assertEquals(bottom.seat(), regular.seat(), "the regular chooser selected from their own assigned seat");
        Assert.assertEquals(bottom.chooserViewId(), regular.chooserViewId());
        Assert.assertTrue(bottom.promptOrdinal() > regular.callbackOrdinal());
        Assert.assertEquals(bottom.oneCardPrompt(), String.format(
                Localizer.getInstance().getMessage("lblReturnForLondon"), 1));
        Assert.assertTrue(bottom.zeroCardPromptOrdinal() > bottom.promptOrdinal(),
                "the actual zero-card London prompt followed the initial prompt");
        Assert.assertEquals(bottom.zeroCardPrompt(), String.format(
                Localizer.getInstance().getMessage("lblReturnForLondon"), 0));
        Assert.assertTrue(bottom.zeroCardPromptOrdinal() > 0,
                "the bottom dispatch includes the actual zero-card prompt before enabled OK");
        Assert.assertEquals(scenario.actions().stream().filter(a -> a.decision()
                == ScriptedHumanTcpSupport.Decision.FREE_MULLIGAN).count(), 1);
        Assert.assertEquals(scenario.actions().stream().filter(a -> a.decision()
                == ScriptedHumanTcpSupport.Decision.REGULAR_MULLIGAN).count(), 1);
        Assert.assertEquals(scenario.actions().stream().filter(a -> a.decision()
                == ScriptedHumanTcpSupport.Decision.BOTTOM_LONDON).count(), 1);
        List<ScriptedHumanTcpSupport.ActionReceipt> actions = scenario.actions();
        int freeAction = actions.indexOf(actions.stream().filter(a -> a.decision()
                == ScriptedHumanTcpSupport.Decision.FREE_MULLIGAN).findFirst().orElseThrow());
        int regularAction = actions.indexOf(actions.stream().filter(a -> a.decision()
                == ScriptedHumanTcpSupport.Decision.REGULAR_MULLIGAN).findFirst().orElseThrow());
        int bottomAction = actions.indexOf(actions.stream().filter(a -> a.decision()
                == ScriptedHumanTcpSupport.Decision.BOTTOM_LONDON).findFirst().orElseThrow());
        int firstKeepAction = actions.indexOf(actions.stream().filter(a -> a.decision()
                == ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND).findFirst().orElseThrow());
        Assert.assertTrue(freeAction < regularAction && regularAction < bottomAction && bottomAction < firstKeepAction,
                "the actual free/regular decisions, bottom input, and final Keeps occurred in engine order");
        Assert.assertEquals(zones.get(free.chooserViewId()).get(ZoneType.Hand).size(), 7);
        Assert.assertEquals(zones.get(regular.chooserViewId()).get(ZoneType.Hand).size(), 6);
        Assert.assertTrue(free.seat() == 0 ? free.chooserViewId() == hostViewId : free.chooserViewId() == remoteViewId);
        Assert.assertTrue(regular.seat() == 0 ? regular.chooserViewId() == hostViewId : regular.chooserViewId() == remoteViewId);
    }

    private static void assertRemoteFreeReveal(ScriptedHumanTcpSupport.Scenario scenario,
            ScriptedHumanTcpSupport.ScriptedGui remote,
            ScriptedHumanTcpSupport.PostKeepReceipt postKeep, int hostViewId, int remoteViewId,
            String hostName, String remoteName) {
        ScriptedHumanTcpSupport.MulliganChoiceReceipt free = scenario.mulliganChoices().stream()
                .filter(choice -> choice.decision() == ScriptedHumanTcpSupport.Decision.FREE_MULLIGAN)
                .findFirst().orElseThrow();
        ScriptedHumanTcpSupport.MulliganChoiceReceipt regular = scenario.mulliganChoices().stream()
                .filter(choice -> choice.decision() == ScriptedHumanTcpSupport.Decision.REGULAR_MULLIGAN)
                .findFirst().orElseThrow();
        Assert.assertEquals(free.seat(), 0, "host starts and owns the eligible seven nonlands");
        Assert.assertEquals(free.chooserViewId(), hostViewId);
        Assert.assertEquals(regular.seat(), 1, "the remote seat makes the complementary Regular choice");
        Assert.assertEquals(regular.chooserViewId(), remoteViewId);
        Assert.assertEquals(postKeep.prioritySeat(), 0,
                "the actual first-priority callback belongs to host seat 0");
        Assert.assertEquals(postKeep.heldTurn(), 1, "the held callback is in the actual first turn");
        Assert.assertEquals(postKeep.heldTurnPlayerViewId(), hostViewId,
                "the held GameView names the host as the actual turn player");

        List<ScriptedHumanTcpSupport.StarterButtonReceipt> buttonReceipts = scenario.starterButtonReceipts();
        Assert.assertEquals(buttonReceipts.size(), 1, "one real Play/Draw choice selected the host as starter");
        ScriptedHumanTcpSupport.StarterButtonReceipt button = buttonReceipts.get(0);
        Assert.assertEquals(button.chooserViewId(), button.seat() == 0 ? hostViewId : remoteViewId);
        Assert.assertEquals(button.playLabel(), Localizer.getInstance().getMessage("lblPlay"));
        Assert.assertEquals(button.drawLabel(), Localizer.getInstance().getMessage("lblDraw"));
        Assert.assertTrue(button.playEnabled() && button.drawEnabled(), "both real Play/Draw buttons were enabled");
        Assert.assertEquals(button.chosePlay(), button.seat() == 0,
                "the actual coin winner chose Play as host or Draw as the remote opponent");

        List<ScriptedHumanTcpSupport.StarterPromptReceipt> prompts = scenario.starterPromptReceipts();
        Assert.assertEquals(prompts.size(), 1, "the assigned coin winner received one localized starting prompt");
        ScriptedHumanTcpSupport.StarterPromptReceipt prompt = prompts.get(0);
        Assert.assertEquals(prompt.seat(), button.seat());
        Assert.assertEquals(prompt.chooserViewId(), button.chooserViewId());
        Assert.assertTrue(prompt.callbackOrdinal() > button.callbackOrdinal(),
                "the real localized prompt followed the enabled Play/Draw button callback");
        String winner = Localizer.getInstance().getMessage("lblYouHaveWonTheCoinToss",
                button.seat() == 0 ? hostName : remoteName);
        Assert.assertEquals(prompt.message(), winner + "\n\n"
                + Localizer.getInstance().getMessage("lblWouldYouLiketoPlayorDraw"));

        Assert.assertEquals(scenario.freeReveals().size(), 1,
                "the actual remote endpoint received one FREE hand reveal presentation");
        ScriptedHumanTcpSupport.FreeRevealReceipt reveal = scenario.freeReveals().get(0);
        Assert.assertEquals(reveal.receivingSeat(), 1);
        Assert.assertEquals(new HashSet<>(reveal.cardIds()), new HashSet<>(free.handIds()),
                "remote reveal contains exactly the host owner's initial hand IDs");
        Assert.assertEquals(reveal.cardIds().size(), free.handIds().size(),
                "remote reveal has no repeated hand ID");
        Assert.assertTrue(reveal.getChoicesCallbackOrdinal() > 0);
        List<ScriptedHumanTcpSupport.Capture> remoteCaptures = remote.captures();
        Assert.assertTrue(remoteCaptures.stream().anyMatch(c -> c.callback().equals("getChoices")
                        && c.callbackOrdinal() == reveal.getChoicesCallbackOrdinal()),
                "receipt is linked to the actual remote getChoices request callback");
        PlayerView owner = findPlayerView(remote.getGameView(), hostViewId);
        PlayerView receiver = findPlayerView(remote.getGameView(), remoteViewId);
        Assert.assertNotNull(owner);
        Assert.assertNotNull(receiver);
        String revealPrefix = Localizer.getInstance().getMessage("lblDanDanFreeMulliganRevealed", owner.getName());
        String revealSuffix = Localizer.getInstance().getMessage("lblPlayerZone", "{player's}",
                ZoneType.Hand.getTranslatedName().toLowerCase());
        Assert.assertEquals(reveal.message(), MessageUtil.formatMessage(revealPrefix + " " + revealSuffix,
                        receiver, owner),
                "remote presentation names the owner and the actual hand zone");
    }

    private static void assertDanDanZones(Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer,
                                          int hostId, int remoteId) {
        Map<ZoneType, List<Integer>> host = zonesByPlayer.get(hostId);
        Map<ZoneType, List<Integer>> remote = zonesByPlayer.get(remoteId);
        Assert.assertNotNull(host, "host assigned PlayerView appears in the detached callback snapshot");
        Assert.assertNotNull(remote, "remote assigned PlayerView appears in the detached callback snapshot");
        List<Integer> hostLibrary = host.getOrDefault(ZoneType.Library, List.of());
        List<Integer> remoteLibrary = remote.getOrDefault(ZoneType.Library, List.of());
        Assert.assertFalse(hostLibrary.isEmpty(), "live common library is populated");
        Assert.assertEquals(remoteLibrary.size(), hostLibrary.size(), "both seat views report the same common-library count");
        Assert.assertEquals(remoteLibrary, hostLibrary, "both seat views expose the same ordered common library");
        List<Integer> hostGraveyard = host.getOrDefault(ZoneType.Graveyard, List.of());
        List<Integer> remoteGraveyard = remote.getOrDefault(ZoneType.Graveyard, List.of());
        Assert.assertEquals(remoteGraveyard, hostGraveyard, "both seat views expose the same ordered common graveyard");
        Assert.assertEquals(hostGraveyard.size(), 0, "the starting common graveyard is empty");
        List<Integer> hostHand = host.getOrDefault(ZoneType.Hand, List.of());
        List<Integer> remoteHand = remote.getOrDefault(ZoneType.Hand, List.of());
        Assert.assertEquals(hostHand.size(), 7, "host seat view retains its dealt hand");
        Assert.assertEquals(remoteHand.size(), 7, "remote seat view retains its dealt hand");
        Assert.assertTrue(Collections.disjoint(new HashSet<>(hostHand), new HashSet<>(remoteHand)),
                "the two seat hands contain distinct physical card IDs");
    }

    private static boolean hasPrivacyWrappedResolution(List<ScriptedHumanTcpSupport.Capture> captures,
            int spellCardId, int abilityId) {
        return captures.stream().anyMatch(events -> "handleGameEvents".equals(events.callback())
                && events.eventFacts().stream().anyMatch(fact ->
                        fact.eventClass().endsWith("GameEventSpellResolved")
                                && fact.scalarFacts().contains("spell.id=" + abilityId)
                                && fact.scalarFacts().contains("spell.hostCardId=" + spellCardId))
                && captures.stream().anyMatch(parent -> "applyDelta".equals(parent.callback())
                        && parent.callbackOrdinal() == events.parentCallbackOrdinal()
                        && parent.sequence() == events.parentSequence()));
    }

    private static void assertPrivacyWrappedEventFacts(List<ScriptedHumanTcpSupport.Capture> captures,
            long afterOrdinal, long castBeforeOrdinal, long movementAfterOrdinal,
            long movementBeforeOrdinal, long resolutionAfterOrdinal,
            int movedCardId, int recipientViewId,
            ZoneType fromZone, ZoneType toZone, int spellCardId, int abilityId, Integer costCardId,
            String expectedStackText, String neverKnownNegativeHandName) {
        ScriptedHumanTcpSupport.Capture exactResolvedParent = captures.stream()
                .filter(capture -> "handleGameEvents".equals(capture.callback())
                        && capture.eventFacts().stream().anyMatch(fact ->
                                fact.eventClass().endsWith("GameEventSpellResolved")
                                        && fact.scalarFacts().contains("spell.id=" + abilityId)
                                        && fact.scalarFacts().contains("spell.hostCardId=" + spellCardId)))
                .map(events -> captures.stream().filter(parent -> "applyDelta".equals(parent.callback())
                        && parent.callbackOrdinal() == events.parentCallbackOrdinal()
                        && parent.sequence() == events.parentSequence()).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .filter(parent -> parent.callbackOrdinal() > afterOrdinal)
                .findFirst().orElse(null);
        Assert.assertNotNull(exactResolvedParent,
                "the actual ability resolution has a matching wrapped event in its applying packet");
        long throughParentOrdinal = exactResolvedParent.callbackOrdinal();
        List<ScriptedHumanTcpSupport.Capture> eventCaptures = captures.stream()
                .filter(capture -> "handleGameEvents".equals(capture.callback())
                        && capture.parentCallbackOrdinal() > afterOrdinal
                        && capture.parentCallbackOrdinal() <= throughParentOrdinal
                        && !capture.eventFacts().isEmpty()).toList();
        Assert.assertFalse(eventCaptures.isEmpty(), "wrapped event facts were captured inside the expected applied-state fence");
        for (ScriptedHumanTcpSupport.Capture events : eventCaptures) {
            ScriptedHumanTcpSupport.Capture parent = captures.stream()
                    .filter(capture -> "applyDelta".equals(capture.callback())
                            && capture.callbackOrdinal() == events.parentCallbackOrdinal()
                            && capture.sequence() == events.parentSequence()).findFirst().orElse(null);
            Assert.assertNotNull(parent, "resolved events retain their exact applying packet");
            Assert.assertTrue(parent.callbackOrdinal() < events.callbackOrdinal(),
                    "the parent packet precedes its resolved event callback");
            Assert.assertEquals(parent.wrappedEventCount(), events.eventCount(),
                    "every wrapped envelope resolved in this callback was captured");
            Assert.assertEquals(events.eventCount(), events.eventFacts().size());
        }

        List<ScriptedHumanTcpSupport.EventFact> facts = eventCaptures.stream()
                .flatMap(capture -> capture.eventFacts().stream()).toList();
        ScriptedHumanTcpSupport.EventFact movement = facts.stream()
                .filter(fact -> fact.eventClass().endsWith("GameEventCardChangeZone")
                        && fact.cardViewIds().contains(movedCardId)
                        && fact.zoneFacts().stream().anyMatch(zone -> "from".equals(zone.field()) && zone.zoneType() == fromZone)
                        && fact.zoneFacts().stream().anyMatch(zone -> "to".equals(zone.field())
                                && zone.zoneType() == toZone && Objects.equals(zone.playerViewId(), recipientViewId)))
                .findFirst().orElse(null);
        Assert.assertNotNull(movement, "the actual selected card changed from the expected source zone into its chooser's hand");

        ScriptedHumanTcpSupport.EventFact cast = facts.stream()
                .filter(fact -> fact.eventClass().endsWith("GameEventSpellAbilityCast")
                        && fact.scalarFacts().contains("sa.id=" + abilityId)
                        && fact.scalarFacts().contains("sa.hostCardId=" + spellCardId)
                        && fact.scalarFacts().contains("si.sourceCardId=" + spellCardId))
                .findFirst().orElse(null);
        Assert.assertNotNull(cast, "the actual cast event retained its host and ability identities");
        Assert.assertTrue(cast.spellAbilityViewIds().contains(abilityId));
        Assert.assertEquals(cast.stackItemViewIds().size(), 1, "cast event retained its stack-item identity");
        Assert.assertNull(cast.targetDescription(), "the actual Telling Time/Fengraf cast had no target description");
        ScriptedHumanTcpSupport.EventFact resolved = facts.stream()
                .filter(fact -> fact.eventClass().endsWith("GameEventSpellResolved")
                        && fact.scalarFacts().contains("spell.id=" + abilityId)
                        && fact.scalarFacts().contains("spell.hostCardId=" + spellCardId))
                .findFirst().orElse(null);
        Assert.assertNotNull(resolved, "the actual resolved event retained its host and ability identities");
        Assert.assertTrue(resolved.spellAbilityViewIds().contains(abilityId));
        if (expectedStackText != null) {
            Assert.assertNotNull(resolved.stackDescription(), "the actual resolution retained its source stack text");
            Assert.assertTrue(resolved.stackDescription().contains(expectedStackText),
                    "the actual resolution retained its source-backed stack description text");
        }
        Assert.assertFalse(resolved.stackDescription() != null
                        && resolved.stackDescription().contains(neverKnownNegativeHandName),
                "the actual resolution stack description did not disclose the never-known opposing hand name");
        Assert.assertTrue(facts.indexOf(cast) < facts.indexOf(resolved),
                "the actual cast event precedes the actual resolution event");
        long movementOrdinal = eventCaptures.stream().filter(capture -> capture.eventFacts().contains(movement))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).findFirst().orElse(-1);
        long castOrdinal = eventCaptures.stream().filter(capture -> capture.eventFacts().contains(cast))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).findFirst().orElse(-1);
        long resolvedOrdinal = eventCaptures.stream().filter(capture -> capture.eventFacts().contains(resolved))
                .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).findFirst().orElse(-1);
        Assert.assertTrue(movementAfterOrdinal < movementBeforeOrdinal,
                "the movement window is ordered around its actual choice/reveal callbacks");
        Assert.assertTrue(castOrdinal < castBeforeOrdinal,
                "cast event precedes the first choice or public reveal callback");
        Assert.assertTrue(movementOrdinal > movementAfterOrdinal && movementOrdinal < movementBeforeOrdinal,
                "selected card movement falls in its source-proven choice/reveal window");
        Assert.assertTrue(resolvedOrdinal > resolutionAfterOrdinal,
                "resolution follows the final ordering choice or public reveal callback");

        if (costCardId != null) {
            ScriptedHumanTcpSupport.EventFact cost = facts.stream()
                    .filter(fact -> fact.eventClass().endsWith("GameEventCardChangeZone")
                            && fact.cardViewIds().contains(costCardId)
                            && fact.zoneFacts().stream().anyMatch(zone -> "from".equals(zone.field())
                                    && zone.zoneType() == ZoneType.Battlefield)
                            && fact.zoneFacts().stream().anyMatch(zone -> "to".equals(zone.field())
                                    && zone.zoneType() == ZoneType.Graveyard))
                    .findFirst().orElse(null);
            Assert.assertNotNull(cost, "Fengraf's actual self-sacrifice moved from Battlefield to Graveyard");
        }
    }

    private static Map<Integer, Map<ZoneType, List<Integer>>> zoneSnapshot(GameView view) {
        Map<Integer, Map<ZoneType, List<Integer>>> result = new java.util.LinkedHashMap<>();
        for (PlayerView player : view.getPlayers()) {
            Map<ZoneType, List<Integer>> zones = new java.util.EnumMap<>(ZoneType.class);
            zones.put(ZoneType.Hand, ids(player.getHand()));
            zones.put(ZoneType.Library, ids(player.getCards(ZoneType.Library)));
            zones.put(ZoneType.Graveyard, ids(player.getCards(ZoneType.Graveyard)));
            zones.put(ZoneType.Battlefield, ids(player.getCards(ZoneType.Battlefield)));
            result.put(player.getId(), zones);
        }
        return result;
    }

    private static Map<Integer, Map<ZoneType, List<Integer>>> privacyZoneSnapshot(GameView view) {
        Map<Integer, Map<ZoneType, List<Integer>>> result = new java.util.LinkedHashMap<>();
        for (PlayerView player : view.getPlayers()) {
            Map<ZoneType, List<Integer>> zones = new java.util.EnumMap<>(ZoneType.class);
            zones.put(ZoneType.Hand, ids(player.getHand()));
            zones.put(ZoneType.Library, ids(player.getCards(ZoneType.Library)));
            zones.put(ZoneType.Battlefield, ids(player.getCards(ZoneType.Battlefield)));
            zones.put(ZoneType.Graveyard, ids(player.getCards(ZoneType.Graveyard)));
            zones.put(ZoneType.Exile, ids(player.getCards(ZoneType.Exile)));
            Map<ZoneType, List<Integer>> copiedZones = new java.util.EnumMap<>(ZoneType.class);
            zones.forEach((zone, ids) -> copiedZones.put(zone, List.copyOf(ids)));
            result.put(player.getId(), java.util.Collections.unmodifiableMap(copiedZones));
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    private static String privacyZoneBaselineEvidence(
            Map<Integer, Map<ZoneType, List<Integer>>> decoded,
            Map<Integer, Map<ZoneType, List<Integer>>> applied,
            Map<Integer, Map<ZoneType, List<Integer>>> fenced,
            Map<Integer, Map<ZoneType, List<Integer>>> held) {
        return "zoneBaseline{decodedEqualsHeld=" + zoneMapsEqual(decoded, held)
                + ",appliedEqualsHeld=" + zoneMapsEqual(applied, held)
                + ",fencedEqualsHeld=" + zoneMapsEqual(fenced, held)
                + ",decodedEqualsApplied=" + zoneMapsEqual(decoded, applied)
                + ",appliedEqualsFenced=" + zoneMapsEqual(applied, fenced)
                + ",counts={decoded:" + privacyZoneCounts(decoded)
                + ",applied:" + privacyZoneCounts(applied)
                + ",fenced:" + privacyZoneCounts(fenced)
                + ",held:" + privacyZoneCounts(held) + "}}";
    }

    private static boolean zoneMapsEqual(Map<Integer, Map<ZoneType, List<Integer>>> left,
            Map<Integer, Map<ZoneType, List<Integer>>> right) {
        return left != null && right != null && left.equals(right);
    }

    private static String privacyZoneCounts(Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) {
        if (zonesByPlayer == null) return "unavailable";
        List<String> playerCounts = new ArrayList<>();
        for (Map<ZoneType, List<Integer>> zones : zonesByPlayer.values()) {
            playerCounts.add("[hand=" + privacyZoneCount(zones, ZoneType.Hand)
                    + ",library=" + privacyZoneCount(zones, ZoneType.Library)
                    + ",battlefield=" + privacyZoneCount(zones, ZoneType.Battlefield)
                    + ",graveyard=" + privacyZoneCount(zones, ZoneType.Graveyard)
                    + ",exile=" + privacyZoneCount(zones, ZoneType.Exile) + "]");
        }
        return "players=" + zonesByPlayer.size() + ",perPlayer=" + playerCounts;
    }

    private static int privacyZoneCount(Map<ZoneType, List<Integer>> zones, ZoneType zone) {
        List<Integer> ids = zones.get(zone);
        return ids == null ? -1 : ids.size();
    }

    private static Map<Integer, Map<ZoneType, List<Integer>>> zoneSnapshot(Game game) {
        Map<Integer, Map<ZoneType, List<Integer>>> result = new java.util.LinkedHashMap<>();
        for (Player player : game.getPlayers()) {
            Map<ZoneType, List<Integer>> zones = new java.util.EnumMap<>(ZoneType.class);
            for (ZoneType zone : List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard,
                    ZoneType.Battlefield, ZoneType.Exile)) {
                zones.put(zone, player.getCardsIn(zone).stream().map(Card::getId).toList());
            }
            result.put(player.getId(), zones);
        }
        return result;
    }

    private static void assertInitialSerializedDanDanIdentity(List<ScriptedHumanTcpSupport.Capture> captures)
            throws Exception {
        boolean found = false;
        for (ScriptedHumanTcpSupport.Capture capture : captures) {
            if (!"setGameView".equals(capture.callback())) continue;
            GameView view = deserializeGameView(capture.serializedArguments());
            if (view == null || !view.isDanDan()) continue;
            Assert.assertNull(view.getGame());
            Assert.assertNull(view.getMatch());
            found = true;
        }
        Assert.assertTrue(found, "delta mode began from a serialized DanDan full view");
    }

    private static GameView deserializeGameView(byte[] bytes) throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object args = input.readObject();
            if (!(args instanceof Object[] values) || values.length == 0 || !(values[0] instanceof GameView view)) return null;
            return view;
        }
    }

    private static PlayerView findPlayerView(GameView view, int id) {
        if (view == null || view.getPlayers() == null) return null;
        for (PlayerView player : view.getPlayers()) if (player.getId() == id) return player;
        return null;
    }

    private static CardView findCardView(GameView view, int id) {
        if (view == null || view.getPlayers() == null) return null;
        for (PlayerView player : view.getPlayers()) {
            for (ZoneType zone : List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield,
                    ZoneType.Graveyard, ZoneType.Exile)) {
                var cards = player.getCards(zone);
                if (cards == null) continue;
                for (CardView card : cards) if (card.getId() == id) return card;
            }
        }
        return null;
    }

    private static List<Integer> ids(Iterable<CardView> cards) {
        List<Integer> result = new ArrayList<>();
        if (cards != null) for (CardView card : cards) result.add(card.getId());
        return result;
    }

    private static List<Integer> sortedIds(List<Integer> ids) {
        return ids.stream().sorted().toList();
    }

    private static void scriptKeepWithoutLand(ScriptedHumanTcpSupport.ScriptedGui gui) {
        gui.script(ScriptedHumanTcpSupport.Decision.PLAY_OR_DRAW, null);
        gui.script(ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND, null);
    }

    private static void scriptKeepWithHostStarter(ScriptedHumanTcpSupport.ScriptedGui gui) {
        gui.script(ScriptedHumanTcpSupport.Decision.START_SEAT_ZERO, null);
        gui.script(ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND, null);
    }

    private static void scriptMulligan(ScriptedHumanTcpSupport.ScriptedGui gui) {
        gui.script(ScriptedHumanTcpSupport.Decision.PLAY_OR_DRAW, null);
        gui.script(ScriptedHumanTcpSupport.Decision.MULLIGAN_ANY, null);
        gui.script(ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND, null);
    }

    private static void scriptMulliganWithHostStarter(ScriptedHumanTcpSupport.ScriptedGui gui) {
        gui.script(ScriptedHumanTcpSupport.Decision.START_SEAT_ZERO, null);
        gui.script(ScriptedHumanTcpSupport.Decision.MULLIGAN_ANY, null);
        gui.script(ScriptedHumanTcpSupport.Decision.KEEP_WITHOUT_LAND, null);
    }

    private static Deck officialDanDanDeck() {
        for (DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
            if ("DanDan_SecretLair".equals(proxy.getName())) return proxy.getDeck();
        }
        throw new AssertionError("Official DanDan preset DanDan_SecretLair was not loaded");
    }

    private static ILobbyListener headlessHostLobbyListener() {
        return headlessHostLobbyListener(message -> { });
    }

    private static ILobbyListener headlessHostLobbyListener(Consumer<String> messageConsumer) {
        return new ILobbyListener() {
            @Override public void update(GameLobbyData state, int slot) { }
            @Override public void message(String source, String message, ChatMessage.MessageType type) { messageConsumer.accept(message); }
            @Override public void close() { }
            @Override public ClientGameLobby getLobby() { return null; }
        };
    }

    private static GuiPlayerState snapshotGuiPlayer(LobbyPlayer player) {
        return new GuiPlayerState(player.getName(), player.getAvatarIndex(), player.getSleeveIndex(),
                player.getAvatarCardImageKey(), player.getSleeveArtKey(), player.getSleeveArtOffset());
    }

    private static void restoreGuiPlayer(LobbyPlayer player, GuiPlayerState state) {
        player.setName(state.name());
        player.setAvatarIndex(state.avatar());
        player.setSleeveIndex(state.sleeve());
        player.setAvatarCardImageKey(state.avatarCardKey());
        player.setSleeveArtKey(state.sleeveArtKey());
        player.setSleeveArtOffset(state.sleeveArtOffset());
    }

    private static Throwable combine(Throwable current, Throwable addition) {
        if (addition == null) return current;
        if (current == null) return addition;
        if (current != addition) current.addSuppressed(addition);
        return current;
    }

    private static void assertNoFailure(ScriptedHumanTcpSupport.Scenario scenario) {
        if (scenario.failure() != null) throw new AssertionError("scripted DanDan TCP fixture failed", scenario.failure());
    }

    private static final class DanDanDesktop extends HeadlessGuiDesktop {
        private final ScriptedHumanTcpSupport.ScriptedGui hostGui;
        private final ScriptedHumanTcpSupport.Scenario scenario;
        private final boolean mulliganScenario;
        private final boolean metamorphoseScenario;
        private volatile MetamorphoseSetup metamorphoseSetup;
        private volatile TcpOwnManaUndoSetup tcpOwnManaUndoSetup;
        private volatile LifecycleTrace tcpOwnManaUndoTrace;
        private final boolean privacyScenario;
        private volatile PrivacySetup privacySetup;
        private HostedMatch match;
        private ControlledShuffleRandom controlledShuffle;
        private RegisteredPlayer shuffledRegisteredPlayer;
        private CardPool shuffleInputMain;
        private final String hostName;
        private final String remoteName;
        private DanDanDesktop(String hostName, String remoteName,
                ScriptedHumanTcpSupport.Scenario scenario, boolean mulliganScenario, boolean metamorphoseScenario,
                boolean privacyScenario) {
            hostGui = new ScriptedHumanTcpSupport.ScriptedGui(0, false, hostName, scenario);
            this.scenario = scenario;
            this.mulliganScenario = mulliganScenario;
            this.metamorphoseScenario = metamorphoseScenario;
            this.privacyScenario = privacyScenario;
            this.hostName = hostName;
            this.remoteName = remoteName;
        }
        @Override public HostedMatch hostMatch() {
            if (!mulliganScenario && !metamorphoseScenario && !privacyScenario
                    && !scenario.positiveRemoteOwnManaUndoScenario()) {
                match = super.hostMatch();
                return match;
            }
            match = new HostedMatch() {
                @Override public void startMatch(GameType gameType, Set<GameType> variants,
                        List<RegisteredPlayer> players, Map<RegisteredPlayer, IGuiGame> guis) {
                    if (mulliganScenario || metamorphoseScenario) {
                        Assert.assertEquals(players.size(), 2);
                        shuffledRegisteredPlayer = players.get(0);
                        var validated = shuffledRegisteredPlayer.getDeck().getValid();
                        Assert.assertSame(validated.getLeft(), shuffledRegisteredPlayer.getDeck(),
                                "Match ultimately enumerates this exact validated RegisteredPlayer deck");
                        Assert.assertTrue(validated.getRight().isEmpty(), "official preset has no unsupported cards removed before shuffle");
                        shuffleInputMain = validated.getLeft().get(DeckSection.Main);
                        controlledShuffle = metamorphoseScenario
                                ? ControlledShuffleRandom.forMetamorphoseActualMain(shuffleInputMain, 9317L, scenario)
                                : ControlledShuffleRandom.forActualMain(shuffleInputMain, 9317L, scenario);
                        if (mulliganScenario) scenario.configureMulliganScenario(controlledShuffle::isComplete);
                        MyRandom.setRandom(controlledShuffle);
                    }
                    if (metamorphoseScenario) {
                        setStartGameHook(() -> metamorphoseSetup = setupMetamorphose(getGame(), scenario));
                    }
                    if (privacyScenario) {
                        setStartGameHook(() -> privacySetup = setupTellingTimeFengraf(getGame(), scenario));
                    }
                    if (scenario.positiveRemoteOwnManaUndoScenario()) {
                        setStartGameHook(() -> {
                            Game actualGame = getGame();
                            tcpOwnManaUndoSetup = setupTcpOwnManaUndo(actualGame, scenario);
                            if (tcpOwnManaUndoSetup != null) {
                                LifecycleTrace trace = new LifecycleTrace();
                                trace.attach(actualGame);
                                trace.expectPhysicalCardIds(tcpOwnManaUndoSetup.physicalCardIds());
                                tcpOwnManaUndoTrace = trace;
                                setEndGameHook(() -> trace.recordEngineReturn(actualGame));
                                scenario.completePositiveRemoteOwnManaUndoSetup(actualGame);
                            }
                        });
                    }
                    super.startMatch(gameType, variants, players, guis);
                }
            };
            return match;
        }
        @Override public IGuiGame getNewGuiGame() { return hostGui; }
        @Override public ISkinImage createLayeredImage(PaperCard card, FSkinProp background,
                                                       String overlayFilename, float opacity) {
            return null;
        }

        private static TcpOwnManaUndoSetup setupTcpOwnManaUndo(Game game,
                ScriptedHumanTcpSupport.Scenario scenario) {
            try {
                Assert.assertNotNull(game);
                Assert.assertEquals(game.getPlayers().size(), 2);
                Player host = game.getPlayers().get(0);
                Player remote = game.getPlayers().get(1);
                var sharedLibrary = host.getZone(ZoneType.Library);
                Assert.assertSame(sharedLibrary, remote.getZone(ZoneType.Library),
                        "both assigned players refer to the same authoritative DanDan Library");
                Card sourceIsland = findOwnedLibraryCard(host, "Island", Set.of());
                Assert.assertSame(sourceIsland.getZone(), sharedLibrary);
                Assert.assertSame(sourceIsland.getOwner(), host);
                Card handFiller = remote.getCardsIn(ZoneType.Hand).stream()
                        .filter(card -> card.getOwner() == remote)
                        .findFirst().orElseThrow(() -> new AssertionError(
                                "Remote opening has no owned hand card available to conserve its seven-card hand"));
                Assert.assertSame(handFiller.getZone(), remote.getZone(ZoneType.Hand));

                List<Integer> libraryBefore = sharedLibrary.getCards().stream().map(Card::getId).toList();
                List<Integer> remoteHandBefore = remote.getCardsIn(ZoneType.Hand).stream().map(Card::getId).toList();
                List<Integer> graveyardBefore = host.getZone(ZoneType.Graveyard).getCards().stream()
                        .map(Card::getId).toList();
                Set<Integer> physicalBefore = game.getCardsInGame().stream().map(Card::getId)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                Assert.assertEquals(remoteHandBefore.size(), 7, "the actual remote opening hand has seven cards");

                Card movedFiller = game.getAction().moveTo(remote.getZone(ZoneType.Library), handFiller, null);
                Assert.assertNotNull(movedFiller);
                Assert.assertTrue(movedFiller.getId() == handFiller.getId(),
                        "returning the filler preserves its physical card identity");
                Assert.assertSame(movedFiller.getOwner(), remote);
                Assert.assertSame(movedFiller.getZone(), sharedLibrary);
                Card acquiredIsland = game.getAction().moveTo(remote.getZone(ZoneType.Hand), sourceIsland, null);
                Assert.assertNotNull(acquiredIsland);
                Assert.assertTrue(acquiredIsland.getId() == sourceIsland.getId(),
                        "the acquired Island is the same physical card that began in the shared Library");
                Assert.assertSame(acquiredIsland.getOwner(), remote,
                        "the accepted shared-Library-to-remote-Hand transition assigned remote ownership");
                Assert.assertSame(acquiredIsland.getZone(), remote.getZone(ZoneType.Hand));
                Assert.assertSame(acquiredIsland.getController(), remote);

                List<Integer> libraryAfter = sharedLibrary.getCards().stream().map(Card::getId).toList();
                List<Integer> remoteHandAfter = remote.getCardsIn(ZoneType.Hand).stream().map(Card::getId).toList();
                List<Integer> graveyardAfter = host.getZone(ZoneType.Graveyard).getCards().stream()
                        .map(Card::getId).toList();
                Set<Integer> physicalAfter = game.getCardsInGame().stream().map(Card::getId)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                Assert.assertEquals(remoteHandAfter.size(), 7);
                Assert.assertTrue(remoteHandAfter.contains(acquiredIsland.getId()));
                Assert.assertFalse(remoteHandAfter.contains(movedFiller.getId()));
                Assert.assertTrue(libraryAfter.contains(movedFiller.getId()));
                Assert.assertFalse(libraryAfter.contains(acquiredIsland.getId()));
                Assert.assertEquals(libraryAfter.size(), libraryBefore.size());
                Assert.assertTrue(graveyardAfter.equals(graveyardBefore),
                        "setup did not change the shared Graveyard membership or order");
                Assert.assertTrue(physicalAfter.equals(physicalBefore),
                        "setup conserved the exact set of physical card identities");
                Assert.assertEquals(physicalAfter.size(), 80,
                        "the setup retained the complete physical 80-card DanDan collection");

                scenario.attachPositiveRemoteOwnManaUndoSetup(game, acquiredIsland.getId(), remote.getView().getId());
                game.subscribeToEvents(new Object() {
                    private boolean prepared;

                    @Subscribe public void onPriority(forge.game.event.GameEventPlayerPriority event) {
                        Card island = game.findById(acquiredIsland.getId());
                        if (!prepared && game.getPhaseHandler().getTurn() == 2
                                && event.phase() == PhaseType.MAIN1 && event.priority().equals(remote.getView())
                                && island != null && island.getZone() == remote.getZone(ZoneType.Battlefield)) {
                            prepared = true;
                            // Drop the setup land play before the normal game-thread priority checkpoint.
                            game.getUndoManager().barrier("Mana undo fixture setup complete");
                        }
                    }
                });
                return new TcpOwnManaUndoSetup(game, game.getView().getId(), remote.getView().getId(),
                        acquiredIsland.getId(), host.getView().getId(), acquiredIsland.getOwner().getView().getId(),
                        physicalAfter);
            } catch (Throwable failure) {
                scenario.fail(failure);
                return null;
            }
        }

        private static MetamorphoseSetup setupMetamorphose(Game game, ScriptedHumanTcpSupport.Scenario scenario) {
            try {
                final Player host = game.getPlayers().get(0);
                final Player remote = game.getPlayers().get(1);
                final Card spell = findOwnedLibraryCard(host, "Metamorphose", Set.of());
                final Card target = findOwnedLibraryCard(host, "Island", Set.of(spell.getId()));
                final Card hostManaOne = findOwnedLibraryCard(host, "Island", Set.of(spell.getId(), target.getId()));
                final Card hostManaTwo = findOwnedLibraryCard(host, "Island", Set.of(spell.getId(), target.getId(), hostManaOne.getId()));
                final Card remoteHand = remote.getCardsIn(ZoneType.Hand).stream()
                        .filter(card -> card.getName().equals("Island") && card.getOwner() == remote)
                        .findFirst().orElseThrow(() -> new AssertionError(
                                "Controlled opening must deal one Island to the remote player's actual hand"));
                final Card hostOpeningIsland = host.getCardsIn(ZoneType.Hand).stream()
                        .filter(card -> card.getName().equals("Island") && card.getOwner() == host)
                        .findFirst().orElseThrow(() -> new AssertionError(
                                "Controlled opening must deal one Island to the host player's actual hand"));
                Assert.assertSame(spell.getOwner(), host);
                Assert.assertSame(target.getOwner(), host);
                Assert.assertSame(hostManaOne.getOwner(), host);
                Assert.assertSame(hostManaTwo.getOwner(), host);
                Assert.assertSame(remoteHand.getOwner(), remote);

                final Card movedSpell = game.getAction().moveTo(host.getZone(ZoneType.Hand), spell, null);
                game.getAction().moveTo(host.getZone(ZoneType.Battlefield), hostManaOne, null);
                game.getAction().moveTo(host.getZone(ZoneType.Battlefield), hostManaTwo, null);
                game.getAction().moveTo(host.getZone(ZoneType.Battlefield), target, null);
                target.setController(remote, game.getNextTimestamp());
                game.getAction().controllerChangeZoneCorrection(target);
                game.getAction().checkStateEffects(true);

                Assert.assertSame(movedSpell.getZone(), host.getZone(ZoneType.Hand));
                Assert.assertSame(target.getOwner(), host, "setup preserves A ownership for the B-controlled target");
                Assert.assertSame(target.getController(), remote);
                Assert.assertSame(target.getZone(), remote.getZone(ZoneType.Battlefield));
                Assert.assertSame(remoteHand.getOwner(), remote);
                Assert.assertSame(remoteHand.getZone(), remote.getZone(ZoneType.Hand));
                Assert.assertFalse(hostManaOne.isTapped());
                Assert.assertFalse(hostManaTwo.isTapped());
                String optionalPrompt = Localizer.getInstance().getMessage("lblSearchPlayerZoneConfirm",
                        "{player's}", ZoneType.Hand.getTranslatedName().toLowerCase());
                scenario.configureMetamorphoseScenario(game, scenario.metamorphoseAccept(),
                        spell.getId(), target.getId(), remoteHand.getId(),
                        List.of(hostManaOne.getId(), hostManaTwo.getId()),
                        MessageUtil.formatMessage(optionalPrompt, remote, remote));
                return new MetamorphoseSetup(host.getId(), remote.getId(), spell.getId(), target.getId(),
                        List.of(hostManaOne.getId(), hostManaTwo.getId()), hostOpeningIsland.getId(), remoteHand.getId());
            } catch (Throwable failure) {
                scenario.fail(failure);
                return null;
            }
        }

        private static PrivacySetup setupTellingTimeFengraf(Game game, ScriptedHumanTcpSupport.Scenario scenario) {
            try {
                scenario.attachPrivacyGame(game);
                Map<Integer, Map<ZoneType, List<Integer>>> preSetupZones = zoneSnapshot(game);
                Player host = game.getPlayers().get(0);
                Player remote = game.getPlayers().get(1);
                int tellingTimeSeat = scenario.privacyTellingTimeSeat();
                int fengrafSeat = scenario.privacyFengrafSeat();
                Player tellingTimePlayer = game.getPlayers().get(tellingTimeSeat);
                Player fengrafPlayer = game.getPlayers().get(fengrafSeat);
                Player otherThanTellingTimePlayer = game.getPlayers().get(1 - tellingTimeSeat);
                Set<Integer> originalPhysicalIds = physicalCardIds(game);
                Assert.assertEquals(originalPhysicalIds.size(), 80,
                        "the real post-opening game begins with the physical official 80-card pool");
                Set<Integer> usedIds = new HashSet<>();
                Card tellingTime = findAvailableCard(game, "Telling Time", usedIds);
                usedIds.add(tellingTime.getId());
                Card daysUndoing = scenario.privacyShuffleHistoryScenario()
                        ? findAvailableCard(game, "Day's Undoing", usedIds) : null;
                if (daysUndoing != null) {
                    usedIds.add(daysUndoing.getId());
                    if (daysUndoing.getZone().is(ZoneType.Hand)) {
                        daysUndoing = game.getAction().moveToLibrary(daysUndoing, 0, null);
                    }
                    daysUndoing = game.getAction().moveTo(host.getZone(ZoneType.Hand), daysUndoing, null);
                    Assert.assertSame(daysUndoing.getOwner(), host,
                            "the actual stock spell enters the host's declared setup hand through the shared pool");
                    Assert.assertSame(daysUndoing.getController(), host);
                    Assert.assertSame(daysUndoing.getZone(), host.getZone(ZoneType.Hand));
                }
                Card fengraf = findAvailableCard(game, "Haunted Fengraf", usedIds);
                usedIds.add(fengraf.getId());
                List<Card> remoteMana = findAvailableCards(game, host, "Island", 2, usedIds);
                remoteMana.forEach(card -> usedIds.add(card.getId()));
                List<Card> hostMana = findAvailableCards(game, host, "Island", 3, usedIds);
                hostMana.forEach(card -> usedIds.add(card.getId()));
                Card publicCreature = findAvailableCreature(game, host, usedIds);
                usedIds.add(publicCreature.getId());

                if (tellingTime.getOwner() != tellingTimePlayer || tellingTime.getZone() != tellingTimePlayer.getZone(ZoneType.Hand)) {
                    if (tellingTime.getZone().is(ZoneType.Hand)) {
                        tellingTime = game.getAction().moveToLibrary(tellingTime, 0, null);
                    }
                    tellingTime = game.getAction().moveTo(tellingTimePlayer.getZone(ZoneType.Hand), tellingTime, null);
                }
                if (fengraf.getOwner() != fengrafPlayer) {
                    if (fengraf.getZone().is(ZoneType.Hand)) fengraf = game.getAction().moveToLibrary(fengraf, 0, null);
                    fengraf = game.getAction().moveTo(fengrafPlayer.getZone(ZoneType.Hand), fengraf, null);
                }
                Assert.assertSame(fengraf.getOwner(), fengrafPlayer,
                        "the assigned Fengraf actor acquired the real official card through the shared-library transition");
                fengraf = game.getAction().moveTo(fengrafPlayer.getZone(ZoneType.Battlefield), fengraf, null);
                SpellAbility fengrafAbility = fengraf.getAllPossibleAbilities(fengrafPlayer, true).stream()
                        .filter(ability -> ability.getApi() == ApiType.ChangeZone && !ability.isManaAbility())
                        .findFirst().orElseThrow(() -> new AssertionError(
                                "Haunted Fengraf has no actual nonmana ChangeZone ability"));
                for (Card source : remoteMana) {
                    Card moved = game.getAction().moveTo(host.getZone(ZoneType.Battlefield), source, null);
                    moved.setController(game.getPlayers().get(tellingTimeSeat), game.getNextTimestamp());
                    game.getAction().controllerChangeZoneCorrection(moved);
                }
                for (Card source : hostMana) {
                    Card moved = game.getAction().moveTo(host.getZone(ZoneType.Battlefield), source, null);
                    moved.setController(game.getPlayers().get(fengrafSeat), game.getNextTimestamp());
                    game.getAction().controllerChangeZoneCorrection(moved);
                }
                publicCreature = game.getAction().moveTo(host.getZone(ZoneType.Graveyard), publicCreature, null);

                Set<String> publicNames = new HashSet<>();
                for (Player player : game.getPlayers()) {
                    for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                        for (Card card : player.getCardsIn(zone)) publicNames.add(card.getName());
                    }
                }
                Set<String> chooserKnownNames = tellingTimePlayer.getCardsIn(ZoneType.Hand).stream()
                        .map(Card::getName).collect(java.util.stream.Collectors.toSet());
                Set<String> otherKnownNames = otherThanTellingTimePlayer.getCardsIn(ZoneType.Hand).stream()
                        .map(Card::getName).collect(java.util.stream.Collectors.toSet());
                Set<String> historicalKnownNames = new HashSet<>();
                if (scenario.privacyMulliganPrefixScenario()) {
                    for (int knownId : scenario.privacyMulliganKnownCardIds()) {
                        Card known = game.findById(knownId);
                        Assert.assertNotNull(known, "every card seen during the actual mulligan prefix remains in this Game");
                        historicalKnownNames.add(known.getName());
                    }
                }
                List<Card> privateTop = new ArrayList<>();
                Set<String> selectedNames = new HashSet<>();
                for (Card card : host.getCardsIn(ZoneType.Library)) {
                    if (card.getOwner() == host && !publicNames.contains(card.getName())
                            && !chooserKnownNames.contains(card.getName())
                            && !otherKnownNames.contains(card.getName())
                            && !historicalKnownNames.contains(card.getName())
                            && !"Telling Time".equals(card.getName())
                            && selectedNames.add(card.getName())) privateTop.add(card);
                    if (privateTop.size() == 3) break;
                }
                Assert.assertEquals(privateTop.size(), 3,
                        "setup can choose three distinct never-public identities from the complete shared library");
                Card negative = otherThanTellingTimePlayer.getCardsIn(ZoneType.Hand).stream()
                        .filter(card -> !publicNames.contains(card.getName()) && !selectedNames.contains(card.getName())
                                && !chooserKnownNames.contains(card.getName()) && !historicalKnownNames.contains(card.getName())
                                && !"Telling Time".equals(card.getName()))
                        .findFirst().orElse(null);
                if (negative == null) {
                    negative = otherThanTellingTimePlayer.getCardsIn(ZoneType.Library).stream()
                            .filter(card -> !publicNames.contains(card.getName()) && !selectedNames.contains(card.getName())
                                    && !chooserKnownNames.contains(card.getName()) && !historicalKnownNames.contains(card.getName())
                                    && !"Telling Time".equals(card.getName()))
                            .findFirst().orElseThrow(() -> new AssertionError(
                                    "Official host pool has no unique nonpublic negative hand sentinel"));
                    negative = game.getAction().moveTo(otherThanTellingTimePlayer.getZone(ZoneType.Hand), negative, null);
                }
                if (scenario.privacyShuffleHistoryScenario()) {
                    Set<Integer> protectedSetupIds = new HashSet<>(usedIds);
                    protectedSetupIds.add(negative.getId());
                    privateTop.forEach(card -> protectedSetupIds.add(card.getId()));
                    for (Player player : List.of(host, remote)) {
                        int targetHandSize = player == host ? 6 : 7;
                        Assert.assertTrue(player.getCardsIn(ZoneType.Hand).size() >= targetHandSize,
                                "history setup can preserve every required role while balancing the opening hand");
                        while (player.getCardsIn(ZoneType.Hand).size() > targetHandSize) {
                            Card filler = player.getCardsIn(ZoneType.Hand).stream()
                                    .filter(card -> card.getOwner() == player)
                                    .filter(card -> !protectedSetupIds.contains(card.getId()))
                                    .findFirst().orElseThrow(() -> new AssertionError(
                                            "History hand balance has no unprotected owned filler card"));
                            Card moved = game.getAction().moveToLibrary(filler, 0, null);
                            Assert.assertEquals(moved.getId(), filler.getId());
                            Assert.assertSame(moved.getOwner(), player);
                            Assert.assertSame(moved.getZone(), player.getZone(ZoneType.Library));
                            Assert.assertSame(moved.getZone(), host.getZone(ZoneType.Library),
                                    "the declared filler swap returns the owned card to the shared library");
                        }
                        while (player.getCardsIn(ZoneType.Hand).size() < targetHandSize) {
                            Card filler = host.getCardsIn(ZoneType.Library).stream()
                                    .filter(card -> card.getOwner() == player)
                                    .filter(card -> !protectedSetupIds.contains(card.getId()))
                                    .findFirst().orElseThrow(() -> new AssertionError(
                                            "History hand balance has no unprotected owned library filler"));
                            Card moved = game.getAction().moveTo(player.getZone(ZoneType.Hand), filler, null);
                            Assert.assertEquals(moved.getId(), filler.getId());
                            Assert.assertSame(moved.getOwner(), player);
                            Assert.assertSame(moved.getZone(), player.getZone(ZoneType.Hand));
                        }
                    }
                    Assert.assertEquals(host.getCardsIn(ZoneType.Hand).size(), 6,
                            "host pre-effect hand keeps Day's Undoing and sentinel roles for the T1 Fengraf return");
                    Assert.assertEquals(remote.getCardsIn(ZoneType.Hand).size(), 7,
                            "remote pre-effect hand retains its normal size before turn-2 draw");
                }
                List<Integer> libraryBeforeReorder = host.getCardsIn(ZoneType.Library).stream()
                        .map(Card::getId).toList();
                for (int i = privateTop.size() - 1; i >= 0; i--) {
                    game.getAction().moveToLibrary(privateTop.get(i), 0, null);
                }

                List<Integer> privateTopIds = privateTop.stream().map(Card::getId).toList();
                List<Integer> remoteManaIds = remoteMana.stream().map(Card::getId).toList();
                List<Integer> hostManaIds = hostMana.stream().map(Card::getId).toList();
                List<Integer> currentTopIds = host.getCardsIn(ZoneType.Library).stream().limit(3)
                        .map(Card::getId).toList();
                Assert.assertEquals(currentTopIds, privateTopIds,
                        "the whole existing library was preserved while private roles were positioned at top");
                Assert.assertTrue(host.getCardsIn(ZoneType.Library).size() > 3,
                        "the private partition has existing library cards below its chosen three");
                List<Integer> initialLibraryIds = host.getCardsIn(ZoneType.Library).stream()
                        .map(Card::getId).toList();
                List<Integer> expectedLibrary = new ArrayList<>(privateTopIds);
                for (Integer cardId : libraryBeforeReorder) if (!privateTopIds.contains(cardId)) expectedLibrary.add(cardId);
                Assert.assertEquals(initialLibraryIds, expectedLibrary,
                        "setup retained the exact existing library remainder order");
                Assert.assertSame(tellingTime.getZone(), tellingTimePlayer.getZone(ZoneType.Hand));
                Assert.assertSame(tellingTime.getOwner(), tellingTimePlayer,
                        "the ordinary DanDan shared-pool hand transition transferred ownership to its recipient");
                Assert.assertSame(fengraf.getController(), fengrafPlayer);
                Assert.assertSame(fengraf.getOwner(), fengrafPlayer);
                Assert.assertSame(publicCreature.getZone(), host.getZone(ZoneType.Graveyard));
                Assert.assertEquals(publicCreature.getOwner(), host);
                Assert.assertSame(negative.getOwner(), otherThanTellingTimePlayer);
                Assert.assertSame(negative.getZone(), otherThanTellingTimePlayer.getZone(ZoneType.Hand));
                Assert.assertFalse(chooserKnownNames.contains(negative.getName()));
                Assert.assertTrue(publicCreature.getType().isCreature());
                Assert.assertEquals(remoteMana.size(), 2);
                Assert.assertEquals(hostMana.size(), 3);
                Assert.assertEquals(new HashSet<>(remoteManaIds).size(), 2);
                Assert.assertEquals(new HashSet<>(hostManaIds).size(), 3);
                game.getAction().checkStateEffects(true);
                Set<Integer> setupPhysicalIds = physicalCardIds(game);
                Assert.assertEquals(setupPhysicalIds, originalPhysicalIds,
                        "real setup moves preserve the exact original physical pool");
                if (scenario.privacyMulliganPrefixScenario()) {
                    scenario.recordPrivacyMulliganBoundary(game, preSetupZones, zoneSnapshot(game));
                }
                String revealPrompt = Localizer.getInstance().getMessage("lblLookCardInPlayerZone",
                        "{player's}", ZoneType.Hand.getTranslatedName().toLowerCase());
                String expectedRevealMessage = MessageUtil.formatMessage(revealPrompt,
                        game.getPlayers().get(1 - fengrafSeat), fengrafPlayer);
                String expectedSacrificeMessage = Localizer.getInstance().getMessage("lblSacrificeCardConfirm",
                        fengraf.getTranslatedName());
                PrivacySetup result = new PrivacySetup(host.getId(), remote.getId(), tellingTimeSeat, fengrafSeat,
                        tellingTime.getId(), daysUndoing == null ? -1 : daysUndoing.getId(),
                        fengraf.getId(), fengrafAbility.getId(), publicCreature.getId(), negative.getId(), privateTopIds,
                        initialLibraryIds, remoteManaIds, hostManaIds, selectedNames, chooserKnownNames,
                        negative.getName(), expectedRevealMessage,
                        originalPhysicalIds);
                scenario.configurePrivacyScenario(result.hostViewId(), result.remoteViewId(), result.tellingTimeId(),
                        result.fengrafId(), result.fengrafAbilityId(), result.publicCreatureId(), result.daysUndoingId(),
                        result.privateTopIds(), result.initialLibraryIds(), result.remoteManaIds(), result.hostManaIds(),
                        result.expectedRevealMessage(), expectedSacrificeMessage);
                return result;
            } catch (Throwable failure) {
                scenario.fail(failure);
                return null;
            }
        }

        private static List<Card> findAvailableCards(Game game, Player owner, String name, int count, Set<Integer> excluded) {
            List<Card> cards = availableCards(game, excluded).stream()
                    .filter(card -> card.getOwner() == owner && name.equals(card.getName())).limit(count).toList();
            if (cards.size() != count) throw new AssertionError(
                    "Official DanDan pool lacks " + count + " available " + name + " role cards");
            return cards;
        }

        private static Card findAvailableCard(Game game, String name, Set<Integer> excluded) {
            return availableCards(game, excluded).stream().filter(card -> name.equals(card.getName()))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "Official DanDan pool lacks an available " + name + " role card"));
        }

        private static Card findAvailableCreature(Game game, Player owner, Set<Integer> excluded) {
            return availableCards(game, excluded).stream().filter(card -> card.getOwner() == owner && card.getType().isCreature())
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "Official DanDan pool lacks an available creature for the public graveyard control"));
        }

        private static List<Card> availableCards(Game game, Set<Integer> excluded) {
            Map<Integer, Card> distinct = new LinkedHashMap<>();
            for (Player player : game.getPlayers()) {
                for (ZoneType zone : List.of(ZoneType.Library, ZoneType.Hand)) {
                    for (Card card : player.getCardsIn(zone)) {
                        if (!excluded.contains(card.getId())) distinct.putIfAbsent(card.getId(), card);
                    }
                }
            }
            return List.copyOf(distinct.values());
        }

        private static Set<Integer> physicalCardIds(Game game) {
            Set<Integer> ids = new HashSet<>();
            for (Player player : game.getPlayers()) {
                for (ZoneType zone : List.of(ZoneType.Library, ZoneType.Hand, ZoneType.Battlefield,
                        ZoneType.Graveyard, ZoneType.Exile)) {
                    for (Card card : player.getCardsIn(zone)) ids.add(card.getId());
                }
            }
            return ids;
        }

        private static Card findOwnedLibraryCard(Player owner, String name, Set<Integer> excluded) {
            for (Card card : owner.getCardsIn(ZoneType.Library)) {
                if (card.getName().equals(name) && card.getOwner() == owner && !excluded.contains(card.getId())) return card;
            }
            throw new AssertionError("Official DanDan shared library has no remaining " + name + " owned by " + owner.getName());
        }
    }

    private static final class ControlledShuffleRandom extends Random {
        private final Random delegate;
        private final List<Integer> transcript;
        private final ScriptedHumanTcpSupport.Scenario scenario;
        private volatile int consumed;

        private ControlledShuffleRandom(List<Integer> transcript, long seed,
                ScriptedHumanTcpSupport.Scenario scenario) {
            super(0L);
            this.transcript = List.copyOf(transcript);
            this.delegate = new Random(seed);
            this.scenario = scenario;
        }

        static ControlledShuffleRandom forActualMain(CardPool actualMain, long seed,
                ScriptedHumanTcpSupport.Scenario scenario) {
            List<PaperCard> initial = new ArrayList<>();
            for (Map.Entry<PaperCard, Integer> entry : actualMain) {
                for (int count = 0; count < entry.getValue(); count++) initial.add(entry.getKey());
            }
            Assert.assertEquals(initial.size(), 80, "controlled shuffle input is the actual 80-card shared Main");
            List<Integer> nonlandIndexes = new ArrayList<>();
            for (int i = 0; i < initial.size(); i++) {
                if (!initial.get(i).getRules().getMainPart().getType().isLand()) nonlandIndexes.add(i);
            }
            Assert.assertTrue(nonlandIndexes.size() >= 7, "official shared Main contains seven nonlands for deterministic eligibility");
            List<PaperCard> target = new ArrayList<>(Collections.nCopies(initial.size(), null));
            boolean[] used = new boolean[initial.size()];
            for (int i = 0; i < 7; i++) {
                int sourceIndex = nonlandIndexes.get(i);
                target.set(i * 2, initial.get(sourceIndex));
                used[sourceIndex] = true;
            }
            int next = 0;
            for (int i = 0; i < target.size(); i++) {
                if (target.get(i) != null) continue;
                while (used[next]) next++;
                target.set(i, initial.get(next));
                used[next++] = true;
            }
            List<PaperCard> working = new ArrayList<>(initial);
            List<Integer> transcript = new ArrayList<>(initial.size() - 1);
            for (int i = working.size() - 1; i > 0; i--) {
                int choice = -1;
                for (int j = 0; j <= i; j++) {
                    if (working.get(j).equals(target.get(i))) { choice = j; break; }
                }
                Assert.assertTrue(choice >= 0, "deterministic Fisher–Yates transcript can construct target permutation");
                transcript.add(choice);
                Collections.swap(working, i, choice);
            }
            Assert.assertEquals(working, target, "transcript produces qualifying alternating opening positions");
            return new ControlledShuffleRandom(transcript, seed, scenario);
        }

        static ControlledShuffleRandom forMetamorphoseActualMain(CardPool actualMain, long seed,
                ScriptedHumanTcpSupport.Scenario scenario) {
            List<PaperCard> initial = new ArrayList<>();
            for (Map.Entry<PaperCard, Integer> entry : actualMain) {
                for (int count = 0; count < entry.getValue(); count++) initial.add(entry.getKey());
            }
            Assert.assertEquals(initial.size(), 80, "Metamorphose shuffle uses the actual official 80-card Main");
            List<Integer> islands = new ArrayList<>();
            List<Integer> openingFillers = new ArrayList<>();
            int metamorphoseCount = 0;
            for (int i = 0; i < initial.size(); i++) {
                String name = initial.get(i).getName();
                if ("Metamorphose".equals(name)) metamorphoseCount++;
                else if ("Island".equals(name)) islands.add(i);
                else openingFillers.add(i);
            }
            Assert.assertTrue(metamorphoseCount > 0, "official Main contains Metamorphose");
            Assert.assertTrue(islands.size() >= 5, "official Main has enough Islands for two opening draws and setup");
            Assert.assertTrue(openingFillers.size() >= 12, "official Main has twelve non-Metamorphose/non-Island opening fillers");

            List<PaperCard> target = new ArrayList<>(initial.size());
            boolean[] used = new boolean[initial.size()];
            for (int index : islands.subList(0, 2)) {
                target.add(initial.get(index));
                used[index] = true;
            }
            for (int index : openingFillers.subList(0, 12)) {
                target.add(initial.get(index));
                used[index] = true;
            }
            for (int i = 0; i < initial.size(); i++) {
                if (!used[i]) target.add(initial.get(i));
            }
            Assert.assertEquals(target.size(), 80);
            Assert.assertTrue(target.subList(0, 14).stream().noneMatch(card -> "Metamorphose".equals(card.getName())),
                    "all official Metamorphose copies remain in the library after the opening deal");
            Assert.assertEquals(target.subList(0, 14).stream().filter(card -> "Island".equals(card.getName())).count(), 2L,
                    "the first two alternating opening draws give each seat an Island");

            List<PaperCard> working = new ArrayList<>(initial);
            List<Integer> transcript = new ArrayList<>(initial.size() - 1);
            for (int i = working.size() - 1; i > 0; i--) {
                int choice = -1;
                for (int j = 0; j <= i; j++) {
                    if (working.get(j).equals(target.get(i))) { choice = j; break; }
                }
                Assert.assertTrue(choice >= 0, "Metamorphose transcript constructs the deterministic permutation");
                transcript.add(choice);
                Collections.swap(working, i, choice);
            }
            Assert.assertEquals(working, target, "Metamorphose transcript preserves the exact official card multiset");
            return new ControlledShuffleRandom(transcript, seed, scenario);
        }

        boolean isComplete() { return consumed == transcript.size() && transcript.size() == 79; }

        private AssertionError unexpected(String primitive) {
            AssertionError error = new AssertionError("Unexpected random call before the 80-card shuffle transcript completed: " + primitive);
            scenario.fail(error);
            return error;
        }

        @Override public int nextInt(int bound) {
            if (consumed < transcript.size()) {
                int expectedBound = 80 - consumed;
                if (bound != expectedBound) throw unexpected("nextInt(" + bound + "), expected nextInt(" + expectedBound + ")");
                return transcript.get(consumed++);
            }
            return delegate.nextInt(bound);
        }
        @Override public int nextInt(int origin, int bound) {
            if (!isComplete()) throw unexpected("nextInt(" + origin + ", " + bound + ")");
            return delegate.nextInt(origin, bound);
        }
        @Override public int nextInt() {
            if (consumed < transcript.size()) {
                List<StackWalker.StackFrame> frames = StackWalker.getInstance().walk(stream -> stream.limit(4).toList());
                if (frames.size() != 4
                        || !frames.get(0).getClassName().equals(ControlledShuffleRandom.class.getName())
                        || !frames.get(0).getMethodName().equals("nextInt")
                        || !frames.get(1).getClassName().equals("forge.util.MyRandom$CountingRandom")
                        || !frames.get(1).getMethodName().equals("next")
                        || !frames.get(2).getClassName().equals(Random.class.getName())
                        || !frames.get(2).getMethodName().equals("nextInt")
                        || !frames.get(2).getDescriptor().equals("(I)I")
                        || !frames.get(3).getClassName().equals(Collections.class.getName())
                        || !frames.get(3).getMethodName().equals("shuffle")) {
                    throw unexpected("nextInt() outside the expected MyRandom/Random/Collections.shuffle call chain");
                }
                int bound = 80 - consumed;
                int choice = transcript.get(consumed++);
                int sample = Integer.bitCount(bound) == 1
                        ? (int) ((long) choice << (31 - Integer.numberOfTrailingZeros(bound)))
                        : choice;
                return sample << 1;
            }
            return delegate.nextInt();
        }
        @Override public long nextLong() { if (!isComplete()) throw unexpected("nextLong()"); return delegate.nextLong(); }
        @Override public long nextLong(long bound) { if (!isComplete()) throw unexpected("nextLong(" + bound + ")"); return delegate.nextLong(bound); }
        @Override public long nextLong(long origin, long bound) {
            if (!isComplete()) throw unexpected("nextLong(" + origin + ", " + bound + ")");
            return delegate.nextLong(origin, bound);
        }
        @Override public boolean nextBoolean() { if (!isComplete()) throw unexpected("nextBoolean()"); return delegate.nextBoolean(); }
        @Override public float nextFloat() { if (!isComplete()) throw unexpected("nextFloat()"); return delegate.nextFloat(); }
        @Override public double nextDouble() { if (!isComplete()) throw unexpected("nextDouble()"); return delegate.nextDouble(); }
        @Override public double nextDouble(double bound) { if (!isComplete()) throw unexpected("nextDouble(" + bound + ")"); return delegate.nextDouble(bound); }
        @Override public double nextDouble(double origin, double bound) {
            if (!isComplete()) throw unexpected("nextDouble(" + origin + ", " + bound + ")");
            return delegate.nextDouble(origin, bound);
        }
        @Override public double nextGaussian() { if (!isComplete()) throw unexpected("nextGaussian()"); return delegate.nextGaussian(); }
        @Override public double nextGaussian(double mean, double standardDeviation) {
            if (!isComplete()) throw unexpected("nextGaussian(" + mean + ", " + standardDeviation + ")");
            return delegate.nextGaussian(mean, standardDeviation);
        }
        @Override public double nextExponential() { if (!isComplete()) throw unexpected("nextExponential()"); return delegate.nextExponential(); }
        @Override public void nextBytes(byte[] bytes) { if (!isComplete()) throw unexpected("nextBytes()"); delegate.nextBytes(bytes); }
        @Override protected int next(int bits) {
            if (!isComplete()) throw unexpected("next(" + bits + ")");
            return delegate.nextInt() >>> (32 - bits);
        }
    }
}
