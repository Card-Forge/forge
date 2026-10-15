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
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputPassPriority;
import forge.game.event.GameEventGameOutcome;
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
import forge.util.MyRandom;
import forge.util.Localizer;
import forge.util.MessageUtil;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;

/** Focused live-state proof for the Constructed plus DanDan view over ordinary TCP. */
public class DanDanNetworkPlayTest {
    private static ScriptedHumanTcpSupportTest fixtureOwner;

    private record GuiPlayerState(String name, int avatar, int sleeve, String avatarCardKey,
                                  String sleeveArtKey, int sleeveArtOffset) { }
    private record MetamorphoseSetup(int hostViewId, int remoteViewId, int spellId, int targetId,
                                     List<Integer> hostManaIds, int hostOpeningIslandId, int remoteHandCardId) {
        private MetamorphoseSetup { hostManaIds = List.copyOf(hostManaIds); }
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
        return new Object[][] {{false}, {true}};
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanLiveStateUsesSharedZonesAndAssignedSeats(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanFreeMulliganRevealReachesRemotePeer(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, true, true);
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

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanPrivateLookAndPublicGraveyardRevealStaySeatScoped(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, true);
    }

    @Test(dataProvider = "syncModes", timeOut = 360_000)
    public void constructedDanDanInversePrivacyActorsStaySeatScoped(boolean deltaSync) throws Throwable {
        runDanDanCase(deltaSync, false, false, false, null, true, false, false, true);
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

        if (privacyScenario) {
            scenario.beginPrivacyScenario(deltaSync, inversePrivacyActors ? 0 : 1, inversePrivacyActors ? 1 : 0);

            scenario.configureStarterSeatZeroScenario();
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
        if (reconnectScenario) {
            scenario.configureStarterSeatZeroScenario();
            if (reconnectScenario) scenario.configureReconnectScenario();

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
        Throwable primaryFailure = null;
        ScriptedHumanTcpSupport.StaleActionRejectionLog staleActionRejectionLog = null;
        boolean staleActionNetworkLogActivated = false;
        try {
            // Prevent the headless start path from consuming the strict deck-shuffle RNG for music selection.
            FModel.getPreferences().setPref(FPref.UI_ENABLE_MUSIC, "false");

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
            } else if (reconnectScenario || metamorphoseAccept != null || privacyScenario || lifecycleScenario) scriptKeepWithHostStarter(desktop.hostGui);
            else scriptKeepWithoutLand(desktop.hostGui);
            if (metamorphoseAccept != null || lifecycleScenario) desktop.hostGui.script(ScriptedHumanTcpSupport.Decision.CAST_METAMORPHOSE, null);

            serverPort = PortAllocator.allocatePort();
            final int port = serverPort;
            server.setLobbyListener(reconnectScenario
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
            } else if (reconnectScenario || metamorphoseAccept != null || privacyScenario || lifecycleScenario) scriptKeepWithHostStarter(remote.gui());
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
                        staleActionRejectionLog, lifecycleCleanupReceipt, capturedLifecycleNPlusOne);
                return;
            }
            if (metamorphoseAccept != null) {
                assertMetamorphoseTcpCase(deltaSync, metamorphoseAccept, scenario, desktop, remote, match, server);
                return;
            }
            if (privacyScenario) {
                privacyTerminalReceipt = assertTellingTimeFengrafPrivacyCase(deltaSync, scenario, desktop,
                        remote, match, reconnectObservation);

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

                assertMulliganReceipts(scenario, mulliganZones, hostViewId, remoteViewId);
                if (hostStarterRevealScenario) {
                    assertRemoteFreeReveal(scenario, remote.gui(), postKeep, hostViewId, remoteViewId,
                            hostName, remoteName);
                }
            }
            ScriptedHumanTcpSupport.Decision starterDecision = hostStarterRevealScenario || reconnectScenario

                    ? ScriptedHumanTcpSupport.Decision.START_SEAT_ZERO
                    : ScriptedHumanTcpSupport.Decision.PLAY_OR_DRAW;
            Assert.assertEquals(scenario.actions().stream().filter(a -> a.decision() == starterDecision).count(), 1);
            if (hostStarterRevealScenario || reconnectScenario) {
                Assert.assertEquals(scenario.actions().stream().filter(a -> a.decision()
                        == ScriptedHumanTcpSupport.Decision.PLAY_OR_DRAW).count(), 0);
            }
            Assert.assertEquals(server.getTotalSendErrors(), 0, "no TCP send errors before terminal cleanup");

            if (!mulliganScenario && !reconnectScenario) {
                assertOwnershipTransitions(deltaSync, match.getGame(), scenario, desktop.hostGui, remote.gui());
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

            scenario.releasePrivacyHostChoice();
            scenario.stopActions();
            Throwable cleanupFailure = null;
            try {
                final boolean runTerminalTimerOracle = primaryFailure == null
                       ;
                final ScriptedHumanTcpSupport remoteForCleanup = remote;
                final ScriptedHumanTcpSupport.ScriptedGui hostGuiForCleanup = desktop == null ? null : desktop.hostGui;
                final DanDanDesktop desktopForCleanup = desktop;
                final PrivacyTerminalReceipt privacyTerminalForCleanup = privacyTerminalReceipt;
                final ScriptedHumanTcpSupport.ReconnectObservation reconnectObservationForCleanup = reconnectObservation;
                HostedMatch cleanupMatch = match != null ? match : desktop == null ? null : desktop.match;
                if (primaryFailure != null && scenario.lifecycleScenario() && cleanupMatch != null && desktopForCleanup != null && remoteForCleanup != null) {
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
                if (historyInactiveForCleanup != null && retainedRemoteClient.get() != null && ScriptedHumanTcpSupport.serverChannel(retainedRemoteClient.get())
                        == historyInactiveForCleanup.channel()) {
                    cleanupInactiveObservation = null;
                    cleanupInactiveReceipt = historyInactiveForCleanup;
                } else {
                    ScriptedHumanTcpSupportTest.ServerInactiveObservation historyPendingForCleanup =
                            pendingHistoryReconnectServerInactive.get();
                    if (historyPendingForCleanup != null && retainedRemoteClient.get() != null && pendingHistoryReconnectServerChannel.get() != null && ScriptedHumanTcpSupport.serverChannel(retainedRemoteClient.get())
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
                            boolean completedPrivacyHistoryNextGame = false
                                    && liveMatch.getGame() == null
                                    && scenario.privacyHistoryNextGameTerminals().size() == 2;
                            if (completedPrivacyHistoryNextGame) return;

                            if (runTerminalTimerOracle && !completedLifecycle && remoteForCleanup != null && hostGuiForCleanup != null && client != null && hostGuiForCleanup.awaitActualTerminal(0, TimeUnit.MILLISECONDS) && remoteForCleanup.gui().awaitActualTerminal(0, TimeUnit.MILLISECONDS)) {
                                endCurrentGameAfterForwardedAwaitPrompt(liveMatch, client, remoteForCleanup.gui());
                                if (privacyTerminalForCleanup != null) {
                                    assertTellingTimeFengrafPrivacyTerminalCase(deltaSync, scenario,
                                            desktopForCleanup, remoteForCleanup, liveMatch, server,
                                            reconnectObservationForCleanup, privacyTerminalForCleanup);
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
            if (primaryFailure == null && retainedRemoteClient.get() != null && reconnectScenario && reconnectSendErrorBaseline.get() != null) {
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

    private static void assertMetamorphoseLifecycle(boolean deltaSync,
            ScriptedHumanTcpSupport.Scenario scenario, DanDanDesktop desktop,
            ScriptedHumanTcpSupport remote, HostedMatch hosted, FServerManager server,
            ScriptedHumanTcpSupport.StaleActionRejectionLog staleActionRejectionLog,
            AtomicReference<LifecycleCleanupReceipt> lifecycleCleanupReceipt,
            AtomicReference<Game> capturedLifecycleNPlusOne) throws Throwable {
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
                    if (cleanupFailure == null && cleanupTrace.game == newGame && cleanupTrace.outcomeState != null && cleanupTrace.engineReturnState != null) {
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
        try {
            awaitLifecycleCondition(scenario, () -> countCallback(desktop.hostGui.captures(), "finishGame") == 2
                            && countCallback(remote.gui().captures(), "finishGame") == 2
                            && desktop.hostGui.terminalViewReceipt() != null
                            && remote.gui().terminalViewReceipt() != null
                            && desktop.hostGui.terminalViewReceipt().gameViewId() == newGame.getView().getId()
                            && remote.gui().terminalViewReceipt().gameViewId() == newGame.getView().getId(),
                    30, "both finishGame terminal GameViews identify the completed N+1 game");
        } catch (AssertionError failure) {
            throw new AssertionError("N+1 terminal receipt diagnostic: expectedGame=" + newGame.getView().getId()
                    + ", hostFinishCount=" + countCallback(desktop.hostGui.captures(), "finishGame")
                    + ", remoteFinishCount=" + countCallback(remote.gui().captures(), "finishGame")
                    + ", hostTerminal=" + desktop.hostGui.terminalViewReceipt()
                    + ", remoteTerminal=" + remote.gui().terminalViewReceipt()
                    + ", hostCurrentView=" + (desktop.hostGui.getGameView() == null
                            ? -1 : desktop.hostGui.getGameView().getId())
                    + ", remoteCurrentView=" + (remote.gui().getGameView() == null
                            ? -1 : remote.gui().getGameView().getId()), failure);
        }
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
            if (!nPlusOne.isGameOver() && nPlusOne.getAge() != GameStage.BeforeMulligan && hostView != null && hostView.getId() == nPlusOne.getView().getId() && remoteView != null && remoteView.getId() == nPlusOne.getView().getId()) {
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
            if (hostTerminal != null && remoteTerminal != null && hostTerminal.gameViewId() == nPlusOne.getView().getId() && remoteTerminal.gameViewId() == nPlusOne.getView().getId() && hostTerminal.gameOver() && remoteTerminal.gameOver()) {
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
            ScriptedHumanTcpSupport.ReconnectObservation observation) throws Exception {
        PrivacySetup setup = desktop.privacySetup;
        int tellingTimeSeat = scenario.privacyTellingTimeSeat();
        int fengrafSeat = scenario.privacyFengrafSeat();
        int chooserViewId = scenario.privacyViewIdForSeat(tellingTimeSeat);
        int otherViewId = scenario.privacyViewIdForSeat(1 - tellingTimeSeat);
        int fengrafActorViewId = scenario.privacyViewIdForSeat(fengrafSeat);
        Assert.assertNotNull(setup, "post-Keep setup retained actual official card roles");

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
            Assert.assertEquals(keep.handIds().size(), false ? (seat == 0 ? 7 : 6) : 7,
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

        Assert.assertEquals(reveal.cardIds(), List.of(setup.publicCreatureId()));
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
        int expectedGateHandCount = 7;
        int expectedLondonBottomId = -1;
        long expectedGateStateLowerBound = -1;
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
            PrivacyTerminalReceipt terminalReceipt) {
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

            assertPrivacyModeReceipt(deltaSync, fengrafState, remote.gui());

        List<ScriptedHumanTcpSupport.PrivacyPromptReceipt> finalPrompts = scenario.privacyPrompts();
        Assert.assertTrue(finalPrompts.stream().filter(prompt -> prompt.seat() == 0)
                        .allMatch(prompt -> prompt.callbackOrdinal() < hostEndFence),
                "host prompt observations are closed by its actual afterGameEnd callback");

            Assert.assertTrue(finalPrompts.stream().filter(prompt -> prompt.seat() == 0)
                            .noneMatch(prompt -> (setup.tellingTimeSeat() == 1
                                    ? setup.privateTopNames() : Set.of(setup.negativeHandName())).stream()
                                    .anyMatch(prompt.message()::contains)),
                    "no host prompt through terminal exposed the other endpoint's private card name");

            Assert.assertTrue(finalPrompts.stream().filter(prompt -> prompt.seat() == 1)
                            .allMatch(prompt -> prompt.callbackOrdinal() < remoteEndFence),
                    "remote prompt observations are closed by its actual afterGameEnd callback");

            Assert.assertTrue(finalPrompts.stream().filter(prompt -> prompt.seat() == 1)
                            .noneMatch(prompt -> (setup.tellingTimeSeat() == 0
                                    ? setup.privateTopNames() : Set.of(setup.negativeHandName())).stream()
                                    .anyMatch(prompt.message()::contains)),
                    "no remote prompt through terminal exposed the other endpoint's private card name");

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

    private static void assertOwnershipTransitions(boolean deltaSync, Game game,
            ScriptedHumanTcpSupport.Scenario scenario, ScriptedHumanTcpSupport.ScriptedGui hostGui,
            ScriptedHumanTcpSupport.ScriptedGui remoteGui) throws Exception {
        Player first = game.getPlayers().get(0);
        Player second = game.getPlayers().get(1);
        Card source = game.getCardsIn(ZoneType.Library).stream()
                .filter(card -> card.getName().equals("Island")).findFirst().orElseThrow();
        int cardId = source.getId();
        int initialOwnerId = source.getOwner().getId();
        List<ScriptedHumanTcpSupport.ScriptedGui> endpoints = List.of(hostGui, remoteGui);
        for (ZoneType destination : List.of(ZoneType.Library, ZoneType.Hand, ZoneType.Graveyard)) {
            if (destination != ZoneType.Library) {
                long before = remoteGui.captures().stream()
                        .mapToLong(ScriptedHumanTcpSupport.Capture::callbackOrdinal).max().orElse(-1);
                FutureTask<Void> move = new FutureTask<>(() -> {
                    game.getAction().moveTo(second.getZone(destination), game.findById(cardId), null);
                    for (Player player : game.getPlayers()) {
                        var gui = ((PlayerControllerHuman) player.getController()).getGui();
                        if (gui instanceof forge.gamemodes.net.ProtocolGuiGame protocol) {
                            protocol.updateGameView();
                        }
                    }
                    return null;
                });
                game.getAction().invoke(move);
                move.get(30, TimeUnit.SECONDS);
                awaitLifecycleCondition(scenario, () -> remoteGui.gameViewReceipts().stream().anyMatch(receipt ->
                        receipt.callbackOrdinal() > before
                                && receipt.gameViewId() == game.getView().getId()
                                && (deltaSync ? "applyDelta" : "setGameView").equals(receipt.callback())
                                && (!deltaSync || receipt.stateBearing())
                                && receipt.zonesByPlayer().get(second.getId()).get(destination).contains(cardId)),
                        30, "the remote received the ownership transition to " + destination);
            }
            FThreads.invokeInEdtAndWait(() -> {
                for (var endpoint : endpoints) {
                    GameView view = endpoint.getGameView();
                    if (endpoint == remoteGui) {
                        Assert.assertNull(view.getGame(), "ownership assertions inspect the received remote state");
                    }
                    CardView card = findCardView(view, cardId);
                    PlayerView receivedFirst = findPlayerView(view, first.getId());
                    PlayerView receivedSecond = findPlayerView(view, second.getId());
                    Set<PlayerView> expectedPlayers = destination == ZoneType.Hand
                            ? Set.of(receivedSecond) : Set.of(receivedFirst, receivedSecond);
                    int expectedOwnerId = destination == ZoneType.Library ? initialOwnerId : second.getId();
                    Assert.assertNotNull(card);
                    Assert.assertEquals(card.getZone(), destination);
                    Assert.assertEquals(Set.copyOf(card.getOwners()), expectedPlayers);
                    Assert.assertEquals(card.getOwner().getId(), expectedOwnerId);
                    Assert.assertEquals(card.getController().getId(), expectedOwnerId);
                    Assert.assertEquals(Set.copyOf(card.getZonePlayers()), expectedPlayers);
                    Assert.assertEquals(card.canBeShownTo(receivedFirst), destination == ZoneType.Graveyard);
                    Assert.assertEquals(card.canBeShownTo(receivedSecond), destination != ZoneType.Library);
                }
            });
        }
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
                if ("updateButtons".equals(fact.method()) && fact.promptOwnerViewId() == remoteViewId && fact.promptOne().equals(heldPrompt.one()) && fact.promptTwo().equals(heldPrompt.two())) {
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
         // this composite checks the pre-setup London and post-setup states separately
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
            if (!mulliganScenario && !metamorphoseScenario && !privacyScenario) {
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

        private static MetamorphoseSetup setupMetamorphose(Game game, ScriptedHumanTcpSupport.Scenario scenario) {
            try {
                final Player host = game.getPlayers().get(0);
                final Player remote = game.getPlayers().get(1);
                final Card spell = findOwnedLibraryCard(host, "Metamorphose", Set.of());
                Card target = findOwnedLibraryCard(host, "Island", Set.of(spell.getId()));
                Card hostManaOne = findOwnedLibraryCard(host, "Island", Set.of(spell.getId(), target.getId()));
                Card hostManaTwo = findOwnedLibraryCard(host, "Island", Set.of(spell.getId(), target.getId(), hostManaOne.getId()));
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
                hostManaOne = game.getAction().moveTo(host.getZone(ZoneType.Battlefield), hostManaOne, null);
                hostManaTwo = game.getAction().moveTo(host.getZone(ZoneType.Battlefield), hostManaTwo, null);
                target = game.getAction().moveTo(host.getZone(ZoneType.Battlefield), target, null);
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
                Card daysUndoing = false
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

                List<Card> privateTop = new ArrayList<>();
                Set<String> selectedNames = new HashSet<>();
                for (Card card : host.getCardsIn(ZoneType.Library)) {
                    if (card.getOwner() == host && !publicNames.contains(card.getName()) && !chooserKnownNames.contains(card.getName()) && !otherKnownNames.contains(card.getName()) && !historicalKnownNames.contains(card.getName()) && !"Telling Time".equals(card.getName()) && selectedNames.add(card.getName())) privateTop.add(card);
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
                if (frames.size() != 4 || !frames.get(0).getClassName().equals(ControlledShuffleRandom.class.getName()) || !frames.get(0).getMethodName().equals("nextInt") || !frames.get(1).getClassName().equals("forge.util.MyRandom$CountingRandom") || !frames.get(1).getMethodName().equals("next") || !frames.get(2).getClassName().equals(Random.class.getName()) || !frames.get(2).getMethodName().equals("nextInt") || !frames.get(2).getDescriptor().equals("(I)I") || !frames.get(3).getClassName().equals(Collections.class.getName()) || !frames.get(3).getMethodName().equals("shuffle")) {
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
