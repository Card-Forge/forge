package forge.net;

import forge.LobbyPlayer;
import forge.game.GameStage;
import forge.game.player.PlayerView;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.HostingServer;
import forge.gamemodes.net.server.RemoteClient;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.player.GamePlayerUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.testng.Assert;
import org.testng.SkipException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/** Isolated profile and lifecycle cleanup shared by the scripted TCP tests. */
public class ScriptedHumanTcpSupportTest {
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


    private static Throwable combine(Throwable current, Throwable addition) {
        if (current == null) return addition;
        if (current != addition) current.addSuppressed(addition);
        return current;
    }

}
