package forge.net;

import forge.deck.Deck;
import forge.LobbyPlayer;
import forge.game.GameStage;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.HostingServer;
import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.server.RemoteClientGuiGame;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IUpdateable;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.MyRandom;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Two ordinary TCP human seats. Run only this class in a fresh Surefire process, e.g.
 * {@code mvn -pl forge-gui-desktop -am test -Dtest=ScriptedHumanTcpSupportTest
 * -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=1 -DreuseForks=false
 * -Dforge.tcp.support.isolated=true}.
 */
public class ScriptedHumanTcpSupportTest {
    private static final String FIXTURE_HOST_NAME = "Scripted host";
    private static IGuiBase guiBeforeInitialization;
    private static Path mainPreferencesPath;
    private static boolean mainPreferencesExisted;
    private static byte[] mainPreferencesBeforeFixture;
    private static GuiPlayerState guiPlayerAfterInitialization;
    private static String userHomeBeforeFixture;
    private static Path isolatedUserHome;
    private static boolean isolatedProfileOwned;

    static final class ServerInactiveObservation {
        private final RemoteClient accepted;
        private final Channel channel;
        private final CountDownLatch inactive;
        private final long deadlineNanos;
        private CompletedServerInactiveReceipt completedReceipt;

        private ServerInactiveObservation(RemoteClient accepted, Channel channel,
                                          CountDownLatch inactive, long deadlineNanos) {
            this.accepted = accepted;
            this.channel = channel;
            this.inactive = inactive;
            this.deadlineNanos = deadlineNanos;
        }
    }

    static final class CompletedServerInactiveReceipt {
        private final RemoteClient accepted;
        private final Channel channel;

        private CompletedServerInactiveReceipt(RemoteClient accepted, Channel channel) {
            this.accepted = accepted;
            this.channel = channel;
        }

        RemoteClient accepted() { return accepted; }
        Channel channel() { return channel; }
    }

    private record GuiPlayerState(String name, int avatar, int sleeve, String avatarCardKey,
                                  String sleeveArtKey, int sleeveArtOffset) { }

    @BeforeClass
    public void initializeCardDatabaseOutsideCaseTimeout() throws IOException {
        if (!Boolean.getBoolean("forge.tcp.support.isolated")) {
            throw new SkipException("Run only through the documented single-class fresh-fork Surefire command");
        }
        userHomeBeforeFixture = System.getProperty("user.home");
        isolatedUserHome = Files.createTempDirectory("forge-tcp-support-home-");
        System.setProperty("user.home", isolatedUserHome.toString());
        isolatedProfileOwned = true;
        guiBeforeInitialization = GuiBase.getInterface();
        GuiBase.setInterface(new HeadlessGuiDesktop());
        mainPreferencesPath = Path.of(ForgeConstants.MAIN_PREFS_FILE);
        mainPreferencesExisted = Files.exists(mainPreferencesPath);
        mainPreferencesBeforeFixture = mainPreferencesExisted ? Files.readAllBytes(mainPreferencesPath) : null;
        TestUtils.ensureFModelInitialized();
        guiPlayerAfterInitialization = snapshotGuiPlayer(GamePlayerUtil.getGuiPlayer());
    }

    @AfterClass(alwaysRun = true)
    public void restoreGuiAfterCardDatabaseInitialization() throws IOException {
        if (!isolatedProfileOwned) return;
        try { restoreMainPreferencesFile(); }
        finally {
            try {
                if (guiPlayerAfterInitialization != null) restoreGuiPlayer(GamePlayerUtil.getGuiPlayer(), guiPlayerAfterInitialization);
            } finally {
                if (guiBeforeInitialization != null) GuiBase.setInterface(guiBeforeInitialization);
                restoreUserHomeAndDeleteIsolatedProfile();
            }
        }
    }

    private static void restoreUserHomeAndDeleteIsolatedProfile() throws IOException {
        try {
            if (isolatedUserHome != null && Files.exists(isolatedUserHome)) {
                try (Stream<Path> paths = Files.walk(isolatedUserHome)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(path);
                    }
                }
            }
        } finally {
            if (userHomeBeforeFixture == null) System.clearProperty("user.home");
            else System.setProperty("user.home", userHomeBeforeFixture);
            isolatedProfileOwned = false;
        }
    }

    @AfterMethod(alwaysRun = true)
    public void restoreMainPreferencesAfterFixtureWritersStop() throws IOException {
        if (!isolatedProfileOwned) return;
        if (GuiBase.getInterface() != null) FThreads.invokeInEdtAndWait(() -> { });
        restoreMainPreferencesFile();
    }

    private static void restoreMainPreferencesFile() throws IOException {
        if (mainPreferencesPath == null) return;
        if (mainPreferencesExisted) Files.write(mainPreferencesPath, mainPreferencesBeforeFixture);
        else Files.deleteIfExists(mainPreferencesPath);
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

    private static final class ScriptedDesktop extends HeadlessGuiDesktop {
        private final ScriptedHumanTcpSupport.ScriptedGui hostGui;
        private ScriptedDesktop(String hostName, ScriptedHumanTcpSupport.Scenario scenario) {
            hostGui = new ScriptedHumanTcpSupport.ScriptedGui(0, false, hostName, scenario);
        }
        @Override public IGuiGame getNewGuiGame() { return hostGui; }
        @Override public ISkinImage createLayeredImage(PaperCard card, FSkinProp background,
                                                       String overlayFilename, float opacity) {
            return null;
        }
    }

    @DataProvider(name = "syncModes")
    public Object[][] syncModes() { return new Object[][] {{true}, {false}}; }

    @Test(dataProvider = "syncModes", timeOut = 180_000)
    public void twoHumanTcpScriptCapturesAndCompletesOrdinaryGame(boolean deltaSync) throws Throwable {
        final IGuiBase oldGui = GuiBase.getInterface();
        final boolean oldDelta = RemoteClientGuiGame.useDeltaSync;
        final String oldLegality = FModel.getPreferences().getPref(FPref.ENFORCE_DECK_LEGALITY);
        final String oldPlayerName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
        final LobbyPlayer guiPlayer = GamePlayerUtil.getGuiPlayer();
        final GuiPlayerState oldGuiPlayer = snapshotGuiPlayer(guiPlayer);
        final Random oldRandom = MyRandom.getRandom();
        final FServerManager server = FServerManager.getInstance();
        final ScriptedHumanTcpSupport.Scenario scenario = new ScriptedHumanTcpSupport.Scenario();
        final String remoteName = "scripted remote";
        String hostName = null;
        ScriptedDesktop desktop = null;
        ScriptedHumanTcpSupport remote = null;
        boolean serverStarted = false;
        boolean hostLobbyListenerInstalled = false;
        HostedMatch match = null;
        RemoteClient retainedRemoteClient = null;
        Throwable primaryFailure = null;
        try {
            HeadlessGuiDesktop.clearLastMatch();
            guiPlayer.setName(FIXTURE_HOST_NAME);
            FModel.getPreferences().setPref(FPref.PLAYER_NAME, FIXTURE_HOST_NAME);
            hostName = guiPlayer.getName();
            desktop = new ScriptedDesktop(hostName, scenario);
            GuiBase.setInterface(desktop);
            FModel.getPreferences().setPref(FPref.ENFORCE_DECK_LEGALITY, "false");
            RemoteClientGuiGame.useDeltaSync = deltaSync;
            MyRandom.setRandom(new Random(7301L + (deltaSync ? 1 : 0)));
            scriptOrdinaryGame(desktop.hostGui, true);

            final int port = PortAllocator.allocatePort();
            server.setLobbyListener(headlessHostLobbyListener());
            hostLobbyListenerInstalled = true;
            serverStarted = true;
            server.startServer(port);
            final ServerGameLobby lobby = new ServerGameLobby();
            server.setLobby(lobby);
            final CountDownLatch serverReadyObserved = new CountDownLatch(1);
            // Mirror NetConnectUtil.host's lobby-to-network bridge without a lobby view.
            lobby.setListener(new IUpdateable() {
                @Override public void update(boolean fullUpdate) {
                    if (lobby.getSlot(1).isReady()) serverReadyObserved.countDown();
                    server.updateLobbyState();
                }
                @Override public void update(int slot, LobbySlotType type) { }
            });
            lobby.getSlot(0).setName(hostName);
            lobby.getSlot(0).setDeck(lands(hostName));
            lobby.getSlot(0).setIsReady(true);
            lobby.getSlot(1).setName(remoteName);
            lobby.getSlot(1).setDeck(lands(remoteName));
            lobby.getSlot(1).setIsReady(false);

            remote = new ScriptedHumanTcpSupport(remoteName, 1, remoteName, port, scenario);
            scriptOrdinaryGame(remote.gui(), false);
            remote.connect();
            Assert.assertTrue(remote.awaitAssigned(15, TimeUnit.SECONDS), "TCP login assigned remote slot");
            assertNoFailure(scenario);
            RemoteClient acceptedRemoteClient = server.getClientBySlotIndex(1);
            Assert.assertNotNull(acceptedRemoteClient, "server retains the assigned remote client for post-cleanup inspection");
            retainedRemoteClient = acceptedRemoteClient;
            Assert.assertEquals(remote.assignedSlot(), 1);
            Assert.assertEquals(lobby.getSlot(1).getType(), LobbySlotType.REMOTE);
            remote.ready();
            Assert.assertTrue(serverReadyObserved.await(15, TimeUnit.SECONDS),
                    "server lobby listener observed the remote slot ready after applying its request");
            Assert.assertTrue(remote.awaitReadyAcknowledged(15, TimeUnit.SECONDS), "client received authoritative ready lobby update");
            assertNoFailure(scenario);
            Assert.assertEquals(remote.lobby().getSlot(1).getType(), LobbySlotType.REMOTE);
            Assert.assertTrue(remote.lobby().getSlot(1).isReady());
            Assert.assertTrue(lobby.getSlot(1).isReady(), "server lobby recorded ready state");

            final Runnable start = lobby.startGame();
            Assert.assertNotNull(start, "both human seats are ready with decks");
            start.run();
            match = HeadlessGuiDesktop.getLastMatch();
            Assert.assertNotNull(match, "ordinary hosted match path created the match");
            Assert.assertEquals(match.getMatch().getPlayers().get(0).getPlayer().getName(), hostName);
            Assert.assertEquals(match.getMatch().getPlayers().get(1).getPlayer().getName(), remoteName);

            Assert.assertTrue(desktop.hostGui.awaitFull(30, TimeUnit.SECONDS), "host received lifecycle game view");
            Assert.assertTrue(desktop.hostGui.awaitOpenView(30, TimeUnit.SECONDS), "host received exact controlled seat view");
            Assert.assertTrue(remote.gui().awaitFull(30, TimeUnit.SECONDS), "remote received initial game view");
            Assert.assertTrue(remote.gui().awaitOpenView(30, TimeUnit.SECONDS), "remote received exact assigned seat view");
            assertNoFailure(scenario);
            Assert.assertTrue(remote.gui().awaitDealtState(30, TimeUnit.SECONDS), "remote applied populated private state for both registered players");
            assertNoFailure(scenario);
            final ScriptedHumanTcpSupport.CallbackReceipt dealtReceipt = remote.gui().dealtReceipt();
            Assert.assertNotNull(dealtReceipt, "remote applied-state barrier records its callback receipt");
            if (deltaSync) Assert.assertTrue(dealtReceipt.wireSequence() >= 0, "delta callback carries protocol sequence");
            else Assert.assertTrue(dealtReceipt.callbackOrdinal() > 0, "full callback has local receive ordinal");
            if (deltaSync) {
                Assert.assertTrue(remote.gui().awaitPopulatedDelta(30, TimeUnit.SECONDS), "remote applied a post-deal state-bearing delta");
                assertNoFailure(scenario);
                Assert.assertTrue(hasPrivateSentinels(remote.gui().captures(), List.of(
                        desktop.hostGui.ownPlayerViewId(), remote.gui().ownPlayerViewId())),
                        "immutable remote delta captures include populated own and opponent hand/library sentinels");
            } else {
                Assert.assertTrue(hasPopulatedFullSentinel(remote.gui().captures()),
                        "remote full-state callback carried both dealt hands and libraries");
            }
            Assert.assertTrue(remote.gui().awaitNonemptyEvents(30, TimeUnit.SECONDS), "remote applied a nonempty event callback");
            assertNoFailure(scenario);
            Assert.assertTrue(hasNonemptyEventCapture(remote.gui().captures()), "remote callback capture contains a nonempty event envelope");

            Assert.assertTrue(scenario.awaitLand(0, 60, TimeUnit.SECONDS), "remote applied host's selected land in a public zone");
            Assert.assertTrue(scenario.awaitLand(1, 60, TimeUnit.SECONDS), "remote applied its selected land in a public zone");
            assertNoFailure(scenario);
            Assert.assertTrue(scenario.awaitLandAbility(0, 30, TimeUnit.SECONDS), "host stock controller selected the queued land ability");
            Assert.assertTrue(scenario.awaitLandAbility(1, 30, TimeUnit.SECONDS), "remote stock controller selected the queued land ability");
            assertNoFailure(scenario);
            Assert.assertTrue(desktop.hostGui.awaitTerminalOrFailure(60, TimeUnit.SECONDS), "host observed terminal callback or scripted failure");
            Assert.assertTrue(remote.gui().awaitTerminalOrFailure(60, TimeUnit.SECONDS), "remote observed terminal callback or scripted failure");
            if (scenario.failure() != null) throw new AssertionError("scripted human action failed", scenario.failure());
            Assert.assertTrue(desktop.hostGui.awaitActualTerminal(1, TimeUnit.SECONDS), "host observed actual terminal completion");
            Assert.assertTrue(remote.gui().awaitActualTerminal(1, TimeUnit.SECONDS), "remote observed actual terminal completion");
            Assert.assertNotNull(match.getGameView());
            Assert.assertTrue(match.getGameView().isGameOver(), "one scripted remote concession ended the game");
            Assert.assertEquals(countActions(scenario.actions(), ScriptedHumanTcpSupport.Decision.KEEP), 2);
            Assert.assertEquals(countActions(scenario.actions(), ScriptedHumanTcpSupport.Decision.PLAY_OR_DRAW), 1);
            Assert.assertEquals(countActions(scenario.actions(), ScriptedHumanTcpSupport.Decision.PLAY_LAND), 2);
            Assert.assertEquals(countActions(scenario.actions(), ScriptedHumanTcpSupport.Decision.CONCEDE), 1);
            Assert.assertTrue(countActions(scenario.actions(), ScriptedHumanTcpSupport.Decision.PASS_PRIORITY) > 0,
                    "stock priority prompts followed the bounded explicit pass script");
            Assert.assertTrue(scenario.bothLandsObserved(), "both exact selected card IDs were applied in public zones");
            for (int seat = 0; seat < 2; seat++) {
                final int expectedSeat = seat;
                final ScriptedHumanTcpSupport.ActionReceipt land = scenario.actions().stream()
                        .filter(a -> a.decision() == ScriptedHumanTcpSupport.Decision.PLAY_LAND && a.seat() == expectedSeat)
                        .findFirst().orElse(null);
                Assert.assertNotNull(land, "seat " + seat + " selected its queued land ability");
                Assert.assertTrue(land.abilityChosen());
                Assert.assertTrue(land.cardId() != null && land.cardId() > 0);
                Assert.assertEquals(land.chooserViewId(), seat == 0 ? desktop.hostGui.ownPlayerViewId() : remote.gui().ownPlayerViewId());
                ScriptedHumanTcpSupport.CallbackReceipt moveReceipt = scenario.publicMoveReceipt(seat);
                Assert.assertNotNull(moveReceipt, "remote public-move barrier records callback receipt for seat " + seat);
                if (deltaSync) Assert.assertTrue(moveReceipt.wireSequence() >= 0, "delta callback carries protocol sequence");
                else Assert.assertTrue(moveReceipt.callbackOrdinal() > 0, "full callback has local receive ordinal");
                Assert.assertTrue(hasPublicCardCapture(remote.gui().captures(), land.cardId(), moveReceipt),
                        "remote applied callback capture contains selected public card " + land.cardId());
            }
            if (!deltaSync) {
                Assert.assertEquals(remote.gui().populatedDeltaCount(), 0,
                        "full mode delivered no state-bearing deltas through both terminal callbacks");
            }
            Assert.assertEquals(server.getTotalSendErrors(), 0, "zero transport send errors");
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            scenario.stopActions();
            Throwable cleanupFailure = null;
            try {
                cleanupFailure = cleanup(match, remote, desktop == null ? null : desktop.hostGui, server,
                        serverStarted, retainedRemoteClient, HostedMatch::endCurrentGame, null, null);
            } catch (Throwable failure) {
                cleanupFailure = combine(cleanupFailure, failure);
            } finally {
                if (hostLobbyListenerInstalled) {
                    try { server.setLobbyListener(null); }
                    catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
                }
            }
            if (retainedRemoteClient != null) {
                try {
                    Assert.assertEquals(retainedRemoteClient.getSendErrorCount(), 0,
                            "remote client has no send errors through final state receipt and disconnect");
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
            try { MyRandom.setRandom(oldRandom); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            try { GuiBase.setInterface(oldGui); }
            catch (Throwable failure) { cleanupFailure = combine(cleanupFailure, failure); }
            if (cleanupFailure != null) {
                if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure);
                else throw cleanupFailure;
            }
        }
    }

    private static ILobbyListener headlessHostLobbyListener() {
        return new ILobbyListener() {
            @Override public void update(GameLobbyData state, int slot) { }
            @Override public void message(String source, String message, ChatMessage.MessageType type) { }
            @Override public void close() { }
            @Override public ClientGameLobby getLobby() { return null; }
        };
    }

    @FunctionalInterface
    interface EndCurrentGameStrategy {
        void end(HostedMatch match) throws Throwable;
    }

    static Throwable cleanup(HostedMatch match, ScriptedHumanTcpSupport remote,
            ScriptedHumanTcpSupport.ScriptedGui hostGui, FServerManager server, boolean serverStarted,
            RemoteClient acceptedRemoteClient, EndCurrentGameStrategy endCurrentGame,
            ServerInactiveObservation retainedServerInactive,
            CompletedServerInactiveReceipt completedServerInactive) {
        Throwable failure = null;
        boolean gameEndSucceeded = false;
        final HostedMatch liveMatch = match != null ? match : HeadlessGuiDesktop.getLastMatch();
        final boolean hadLiveGame = liveMatch != null && liveMatch.getGame() != null;
        try {
            if (liveMatch != null && liveMatch.getGame() != null) {
                AtomicReference<Throwable> actionFailure = new AtomicReference<>();
                // Match.startGame prepares zones on its game thread; before Mulligan is the
                // existing boundary before that worker is ready to accept a gameplay action.
                if (!liveMatch.getGame().isGameOver()
                        && liveMatch.getGame().getAge() != GameStage.BeforeMulligan && hostGui != null) {
                    FThreads.invokeInEdtLater(() -> {
                        try {
                            PlayerView hostView = findPlayerView(hostGui, hostGui.ownPlayerViewId());
                            if (hostView == null) throw new AssertionError("Host player view unavailable for cleanup concession");
                            hostGui.getGameController(hostView).concede();
                        } catch (Throwable t) { actionFailure.set(t); }
                    });
                }
                if (hostGui == null) {
                    failure = combine(failure, new AssertionError("Cannot verify host terminal callback without its GUI"));
                } else if (!hostGui.awaitActualTerminal(8, TimeUnit.SECONDS)) {
                    Throwable waitFailure = actionFailure.get();
                    failure = combine(failure, waitFailure != null ? waitFailure
                            : new AssertionError("Timed out waiting for actual hosted-game termination during cleanup"));
                }
                if (remote != null && !remote.gui().awaitActualTerminal(8, TimeUnit.SECONDS)) {
                    failure = combine(failure, new AssertionError("Timed out waiting for remote actual terminal callback during cleanup"));
                }
                if (actionFailure.get() != null) failure = combine(failure, actionFailure.get());
            }
        } catch (Throwable cleanupError) {
            failure = combine(failure, cleanupError);
        } finally {
            try {
                if (liveMatch != null) {
                    endCurrentGame.end(liveMatch);
                    gameEndSucceeded = true;
                }
            }
            catch (Throwable cleanupError) { failure = combine(failure, cleanupError); }
            if (gameEndSucceeded && hadLiveGame && remote != null
                    && remote.gui().captures().stream().anyMatch(c -> "openView".equals(c.callback()))) {
                try {
                    if (!remote.gui().awaitAfterGameEnd(8, TimeUnit.SECONDS)
                            || remote.gui().captures().stream().noneMatch(c -> "afterGameEnd".equals(c.callback()))) {
                        failure = combine(failure, new AssertionError(
                                "Timed out waiting for remote afterGameEnd receipt while its TCP connection remained open"));
                    }
                } catch (Throwable cleanupError) { failure = combine(failure, cleanupError); }
            }
            ServerInactiveObservation serverInactive = null;
            if (serverStarted && server != null && remote != null) {
                try {
                    if (remote.assignedSlot() == 1) {
                        Assert.assertNotNull(acceptedRemoteClient,
                                "an assigned remote retains its accepted server client for disconnect fencing");
                    }
                    if (acceptedRemoteClient != null) {
                        if (completedServerInactive != null) {
                            Assert.assertSame(completedServerInactive.accepted(), acceptedRemoteClient,
                                    "completed disconnect receipt belongs to the exact accepted server client");
                            Assert.assertSame(completedServerInactive.channel(), acceptedChannel(acceptedRemoteClient),
                                    "completed disconnect receipt remains bound to the accepted client's same channel");
                            Assert.assertFalse(completedServerInactive.channel().isActive(),
                                    "the receipted channel is the same now-inactive accepted endpoint");
                        } else if (retainedServerInactive != null) {
                            Assert.assertSame(retainedServerInactive.accepted, acceptedRemoteClient,
                                    "retained disconnect receipt belongs to the exact accepted server client");
                            Assert.assertSame(retainedServerInactive.channel, acceptedChannel(acceptedRemoteClient),
                                    "pending disconnect receipt remains bound to the accepted client's same channel");
                            serverInactive = retainedServerInactive;
                        } else {
                            serverInactive = prepareServerInactiveObservation(acceptedRemoteClient);
                        }
                    }
                } catch (Throwable cleanupError) { failure = combine(failure, cleanupError); }
            }
            try { if (remote != null) remote.close(); }
            catch (Throwable cleanupError) { failure = combine(failure, cleanupError); }
            if (serverInactive != null) {
                try { awaitServerInactive(serverInactive); }
                catch (Throwable cleanupError) { failure = combine(failure, cleanupError); }
            }
            try { if (serverStarted) server.stopServer(); }
            catch (Throwable cleanupError) { failure = combine(failure, cleanupError); }
            HeadlessGuiDesktop.clearLastMatch();
        }
        if (liveMatch != null && liveMatch.getGame() != null) {
            failure = combine(failure, new AssertionError("Hosted game remained active after fixture cleanup"));
        }
        if (serverStarted && HostingServer.isHosting()) {
            failure = combine(failure, new AssertionError("Hosted TCP server remained active after fixture cleanup"));
        }
        return failure;
    }

    static ServerInactiveObservation prepareServerInactiveObservation(RemoteClient accepted)
            throws ReflectiveOperationException, java.util.concurrent.ExecutionException,
            java.util.concurrent.TimeoutException, InterruptedException {
        Channel channel = acceptedChannel(accepted);
        Assert.assertNotNull(channel, "accepted RemoteClient retained its current server channel");
        CountDownLatch inactive = new CountDownLatch(1);
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        Future<?> installed = channel.eventLoop().submit(() -> {
            if (!channel.isActive()) {
                throw new AssertionError("accepted server channel became inactive before its disconnect receipt was installed");
            }
            String deregisterHandler = channel.pipeline().names().stream()
                    .filter(name -> channel.pipeline().get(name).getClass().getName()
                            .endsWith("$DeregisterClientHandler"))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "accepted server channel lacks its owned DeregisterClientHandler"));
            channel.pipeline().addAfter(deregisterHandler, "scripted-fixture-inactive-receipt",
                    new ChannelInboundHandlerAdapter() {
                        @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                            try { super.channelInactive(ctx); }
                            finally { inactive.countDown(); }
                        }
                    });
        });
        installed.get(remainingNanos(deadlineNanos, "installing server disconnect receipt"), TimeUnit.NANOSECONDS);
        return new ServerInactiveObservation(accepted, channel, inactive, deadlineNanos);
    }

    private static Channel acceptedChannel(RemoteClient accepted) throws ReflectiveOperationException {
        var channelField = RemoteClient.class.getDeclaredField("channel");
        channelField.setAccessible(true);
        return (Channel) channelField.get(accepted);
    }

    static synchronized CompletedServerInactiveReceipt awaitServerInactive(
            ServerInactiveObservation observation) throws Exception {
        if (observation.completedReceipt != null) return observation.completedReceipt;
        Assert.assertTrue(observation.inactive.await(
                        remainingNanos(observation.deadlineNanos, "waiting for server deregistration"),
                        TimeUnit.NANOSECONDS),
                "accepted server channel reached the downstream receipt after DeregisterClientHandler");
        Future<?> eventLoopDrain = observation.channel.eventLoop().submit(() -> { });
        eventLoopDrain.get(remainingNanos(observation.deadlineNanos, "draining server event loop"),
                TimeUnit.NANOSECONDS);
        observation.completedReceipt = new CompletedServerInactiveReceipt(
                observation.accepted, observation.channel);
        return observation.completedReceipt;
    }

    private static long remainingNanos(long deadlineNanos, String stage) {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) throw new AssertionError("Timed out " + stage);
        return remaining;
    }

    private static PlayerView findPlayerView(ScriptedHumanTcpSupport.ScriptedGui gui, int id) {
        if (gui.getGameView() == null) return null;
        for (PlayerView player : gui.getGameView().getPlayers()) if (player.getId() == id) return player;
        return null;
    }

    private static void assertNoFailure(ScriptedHumanTcpSupport.Scenario scenario) {
        if (scenario.failure() != null) throw new AssertionError("scripted TCP fixture failed", scenario.failure());
    }

    private static Throwable combine(Throwable current, Throwable addition) {
        if (current == null) return addition;
        if (current != addition) current.addSuppressed(addition);
        return current;
    }

    private static boolean hasPrivateSentinels(List<ScriptedHumanTcpSupport.Capture> captures, List<Integer> playerIds) {
        for (int playerId : playerIds) {
            boolean hand = false;
            boolean library = false;
            for (ScriptedHumanTcpSupport.Capture capture : captures) {
                if (!"applyDelta".equals(capture.callback()) || !capture.stateBearing()) continue;
                final Map<ZoneType, List<Integer>> zones = capture.zonesByPlayer().get(playerId);
                if (zones == null) continue;
                hand |= !zones.getOrDefault(ZoneType.Hand, List.of()).isEmpty();
                library |= !zones.getOrDefault(ZoneType.Library, List.of()).isEmpty();
            }
            if (!hand || !library) return false;
        }
        return true;
    }

    private static boolean hasPopulatedFullSentinel(List<ScriptedHumanTcpSupport.Capture> captures) {
        return captures.stream().filter(c -> "setGameView".equals(c.callback())).anyMatch(c ->
                c.zonesByPlayer().size() >= 2 && c.zonesByPlayer().values().stream().allMatch(z ->
                        !z.getOrDefault(ZoneType.Hand, List.of()).isEmpty()
                                && !z.getOrDefault(ZoneType.Library, List.of()).isEmpty()));
    }

    private static boolean hasNonemptyEventCapture(List<ScriptedHumanTcpSupport.Capture> captures) {
        for (ScriptedHumanTcpSupport.Capture applied : captures) {
            if (!"handleGameEvents".equals(applied.callback()) || applied.eventCount() == 0
                    || applied.eventFacts().size() != applied.eventCount()
                    || applied.parentCallbackOrdinal() <= 0 || applied.parentSequence() != applied.sequence()) continue;
            if (applied.eventFacts().stream().anyMatch(fact -> fact.eventClass().isBlank())) continue;
            boolean hasViewIds = applied.eventFacts().stream().anyMatch(fact ->
                    !fact.playerViewIds().isEmpty() || !fact.cardViewIds().isEmpty());
            if (!hasViewIds) continue;
            boolean retainedParentEnvelope = captures.stream().anyMatch(packet ->
                    "applyDelta".equals(packet.callback())
                            && packet.callbackOrdinal() == applied.parentCallbackOrdinal()
                            && packet.sequence() == applied.parentSequence()
                            && packet.wrappedEventCount() == applied.eventCount()
                            && packet.wrappedEventEnvelopeBytes().length > 0);
            if (retainedParentEnvelope) return true;
        }
        return false;
    }

    private static boolean hasPublicCardCapture(List<ScriptedHumanTcpSupport.Capture> captures, int cardId,
            ScriptedHumanTcpSupport.CallbackReceipt receipt) {
        for (ScriptedHumanTcpSupport.Capture capture : captures) {
            if (capture.callbackOrdinal() != receipt.callbackOrdinal() || capture.sequence() != receipt.wireSequence()) continue;
            if (capture.publicCardIds().contains(cardId)) return true;
            for (Map<ZoneType, List<Integer>> zones : capture.zonesByPlayer().values()) {
                for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                    if (zones.getOrDefault(zone, List.of()).contains(cardId)) return true;
                }
            }
        }
        return false;
    }

    private static long countActions(List<ScriptedHumanTcpSupport.ActionReceipt> actions, ScriptedHumanTcpSupport.Decision decision) {
        return actions.stream().filter(a -> a.decision() == decision).count();
    }

    private static void scriptOrdinaryGame(ScriptedHumanTcpSupport.ScriptedGui gui, boolean host) {
        gui.script(ScriptedHumanTcpSupport.Decision.PLAY_OR_DRAW, null);
        gui.script(ScriptedHumanTcpSupport.Decision.KEEP, null);
        gui.script(ScriptedHumanTcpSupport.Decision.PLAY_LAND, null);
        if (!host) gui.script(ScriptedHumanTcpSupport.Decision.CONCEDE, null);
    }

    private static Deck lands(String name) {
        final Deck deck = new Deck(name);
        deck.getMain().add("Plains", 40);
        return deck;
    }
}
