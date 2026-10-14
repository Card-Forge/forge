package forge.gamemodes.net.client;

import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.Match;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.NetworkByteTracker;
import forge.gamemodes.net.NetworkGuiGame;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.server.RemoteClientGuiGame;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.LoginEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.event.ReplyEvent;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;
import forge.util.Lang;
import forge.util.Localizer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** Exercises a game-ID tracker rotation through the real client handler and framed codec. */
public class GameClientHandlerShould {

    private IGuiBase previousGui;
    private Thread guiThread;
    private LinkedBlockingQueue<Runnable> queuedGuiTasks;
    private boolean deferGuiTasks;
    private boolean libgdxPort;
    private volatile CountDownLatch deferredGuiProgress;

    @BeforeMethod
    public void installHeadlessGuiBase() {
        previousGui = GuiBase.getInterface();
        // Match the established forge-gui engine test bootstrap before GameType initializes.
        Localizer.getInstance().initialize("en-US", "res/languages/");
        Lang.createInstance("en-US");
        guiThread = Thread.currentThread();
        queuedGuiTasks = new LinkedBlockingQueue<>();
        deferGuiTasks = false;
        libgdxPort = false;
        deferredGuiProgress = null;
        final IGuiBase guiBase = mock(IGuiBase.class);
        when(guiBase.isLibgdxPort()).thenAnswer(invocation -> libgdxPort);
        when(guiBase.isGuiThread()).thenAnswer(invocation ->
                !deferGuiTasks || Thread.currentThread() == guiThread);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(guiBase).invokeInEdtNow(any(Runnable.class));
        doAnswer(invocation -> {
            final Runnable task = invocation.getArgument(0);
            if (!deferGuiTasks || Thread.currentThread() == guiThread) {
                task.run();
            } else {
                queuedGuiTasks.add(task);
                if (queuedGuiTasks.size() >= 2 && deferredGuiProgress != null) {
                    deferredGuiProgress.countDown();
                }
            }
            return null;
        }).when(guiBase).invokeInEdtLater(any(Runnable.class));
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(guiBase).invokeInEdtAndWait(any(Runnable.class));
        GuiBase.setInterface(guiBase);
    }

    @AfterMethod(alwaysRun = true)
    public void restoreGuiBase() {
        GuiBase.setInterface(previousGui);
    }

    @Test
    public void rotateTheCodecTrackerBetweenCoalescedFullViewsButKeepItForSameGameViews() throws Exception {
        final int playerId = 37;
        final int oldOnlyCardId = 19;
        final int reusedCardId = 23;
        final int newOnlyCardId = 31;
        final Game oldGame = newGame();
        final Game nextGame = newGame();
        final Tracker oldServerTracker = oldGame.getTracker();
        final CardView oldOnlyServerCard = new CardView(oldOnlyCardId, oldServerTracker, "old-only game card");
        final CardView oldServerCard = new CardView(reusedCardId, oldServerTracker, "old game card");
        oldServerTracker.putObj(TrackableTypes.CardViewType, oldOnlyCardId, oldOnlyServerCard);
        oldServerTracker.putObj(TrackableTypes.CardViewType, reusedCardId, oldServerCard);
        final GameView oldServerView = gameView(oldGame, playerId, List.of(oldOnlyServerCard, oldServerCard));

        final Tracker newServerTracker = nextGame.getTracker();
        final CardView newServerCard = new CardView(reusedCardId, newServerTracker, "new game card");
        final CardView newOnlyServerCard = new CardView(newOnlyCardId, newServerTracker, "new-only game card");
        newServerTracker.putObj(TrackableTypes.CardViewType, reusedCardId, newServerCard);
        newServerTracker.putObj(TrackableTypes.CardViewType, newOnlyCardId, newOnlyServerCard);
        final GameView sameGameServerView = gameView(oldGame, playerId, List.of(oldOnlyServerCard, oldServerCard));
        final GameView newServerView = gameView(nextGame, playerId, List.of(newServerCard, newOnlyServerCard));

        final List<GameView> appliedViews = new ArrayList<>();
        final List<?>[] receivedChoices = new List<?>[1];
        final IGuiGame gui = mock(IGuiGame.class);
        doAnswer(invocation -> {
            appliedViews.add(invocation.getArgument(0));
            return null;
        }).when(gui).setGameView(any(GameView.class), anyLong());
        doAnswer(invocation -> {
            receivedChoices[0] = invocation.getArgument(3);
            return invocation.getArgument(3);
        }).when(gui).getChoices(anyString(), anyInt(), anyInt(), anyList(), anyList(), any());

        // Capture protocol replies only; this EmbeddedChannel test has no bound FGameClient socket.
        final FGameClient client = spy(new FGameClient("codec-test", gui, "localhost", 1));
        doNothing().when(client).send(any(NetEvent.class));
        final CompatibleObjectEncoder encoder = new CompatibleObjectEncoder(new NetworkByteTracker());
        encoder.setTracker(newServerTracker);
        final CompatibleObjectDecoder decoder = new CompatibleObjectDecoder(
                10_000_000, ClassResolvers.cacheDisabled(null));
        final CompatibleObjectEncoder clientEncoder = new CompatibleObjectEncoder(new NetworkByteTracker());
        final EmbeddedChannel channel = new EmbeddedChannel(clientEncoder, decoder, new GameClientHandler(client));
        EmbeddedChannel serverChannel = null;
        try {
            final ByteBuf oldFull = encode(encoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, oldServerView.getId(), oldServerView, 1L));
            encoder.setTracker(oldServerTracker);
            final ByteBuf sameGameFull = encode(encoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, sameGameServerView.getId(), sameGameServerView, 2L));
            encoder.setTracker(newServerTracker);
            final ByteBuf newFull = encode(encoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, newServerView.getId(), newServerView, 3L));
            final GuiGameEvent newGameChoiceEvent = GuiGameEvent.forGameView(
                    ProtocolMethod.getChoices, newServerView.getId(), "current choice", 0, 1,
                    List.of(newServerCard), List.of(), null);
            final ByteBuf newGameChoice = encode(encoder, newGameChoiceEvent);

            final ByteBuf coalescedFrames = Unpooled.buffer();
            coalescedFrames.writeBytes(oldFull).writeBytes(sameGameFull).writeBytes(newFull).writeBytes(newGameChoice);
            oldFull.release();
            sameGameFull.release();
            newFull.release();
            newGameChoice.release();

            serverChannel = new EmbeddedChannel(new CompatibleObjectDecoder(
                    10_000_000, ClassResolvers.cacheDisabled(null)));
            final ByteBuf encodedLogin = channel.readOutbound();
            assertThat(encodedLogin).isNotNull();
            serverChannel.writeInbound(encodedLogin);
            final Object decodedLogin = serverChannel.readInbound();
            assertThat(decodedLogin).isInstanceOf(LoginEvent.class);
            final LoginEvent login = (LoginEvent) decodedLogin;
            assertThat(login.getUsername()).isEqualTo("codec-test");
            assertThat(login.getVersion()).isNotEmpty();
            assertThat(login.getAvatarIndex()).isEqualTo(Integer.parseInt(
                    FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0]));
            assertThat(login.getSleeveIndex()).isEqualTo(Integer.parseInt(
                    FModel.getPreferences().getPref(FPref.UI_SLEEVES).split(",")[0]));
            assertThat(login.isLibgdx()).isFalse();
            assertThat(serverChannel.<Object>readInbound()).isNull();
            assertThat(channel.<Object>readOutbound()).isNull();

            FThreads.invokeInEdtAndWait(() -> channel.writeInbound(coalescedFrames));

            assertThat(appliedViews).hasSize(3);
            final GameView oldApplied = appliedViews.get(0);
            final GameView sameGameApplied = appliedViews.get(1);
            final GameView newApplied = appliedViews.get(2);
            assertThat(sameGameApplied.getTracker()).isSameAs(oldApplied.getTracker());
            assertThat(newApplied.getTracker()).isNotSameAs(oldApplied.getTracker());

            final CardView oldAppliedCard = oldApplied.getPlayers().get(0).getHand().get(1);
            final CardView currentAppliedCard = newApplied.getPlayers().get(0).getHand().get(0);
            assertThat(currentAppliedCard).isNotSameAs(oldAppliedCard);
            assertThat(newApplied.getTracker().getObj(TrackableTypes.CardViewType, oldOnlyCardId)).isNull();
            assertThat(newApplied.getTracker().getObj(TrackableTypes.CardViewType, reusedCardId))
                    .isSameAs(currentAppliedCard);
            assertThat(receivedChoices[0]).hasSize(1);
            assertThat(receivedChoices[0].get(0)).isSameAs(currentAppliedCard);
            final ArgumentCaptor<NetEvent> replyCaptor = ArgumentCaptor.forClass(NetEvent.class);
            verify(client, times(1)).send(replyCaptor.capture());
            assertThat(replyCaptor.getValue()).isInstanceOf(ReplyEvent.class);
            final ReplyEvent choicesReply = (ReplyEvent) replyCaptor.getValue();
            assertThat(choicesReply.getIndex()).isEqualTo(newGameChoiceEvent.getId());
            assertThat((List<?>) choicesReply.getReply()).hasSize(1);
            assertThat(((List<?>) choicesReply.getReply()).get(0)).isSameAs(currentAppliedCard);
            assertThat(channel.<Object>readOutbound()).isNull();

            final GuiGameEvent clientSelection = GuiGameEvent.forGameView(
                    ProtocolMethod.selectCard, newApplied.getId(), newApplied.getPlayers().get(0).getHand().get(1));
            FThreads.invokeInEdtAndWait(() -> channel.writeOutbound(clientSelection));
            final ByteBuf encodedSelection = channel.readOutbound();
            assertThat(encodedSelection).isNotNull();
            assertThat(channel.<Object>readOutbound()).isNull();
            serverChannel.pipeline().get(CompatibleObjectDecoder.class).setTracker(newServerTracker);
            serverChannel.writeInbound(encodedSelection);
            final GuiGameEvent decodedSelection = serverChannel.readInbound();
            assertThat(decodedSelection.getMethod()).isEqualTo(ProtocolMethod.selectCard);
            assertThat(decodedSelection.getGameViewId()).isEqualTo(newApplied.getId());
            assertThat(decodedSelection.getObjects()[0]).isSameAs(newOnlyServerCard);
            assertThat(serverChannel.<Object>readInbound()).isNull();

            final GuiGameEvent oldViewSelection = GuiGameEvent.forGameView(
                    ProtocolMethod.selectCard, oldApplied.getId(), oldApplied.getPlayers().get(0).getHand().get(0));
            FThreads.invokeInEdtAndWait(() -> channel.writeOutbound(oldViewSelection));
            final ByteBuf encodedOldViewSelection = channel.readOutbound();
            assertThat(encodedOldViewSelection).isNotNull();
            serverChannel.writeInbound(encodedOldViewSelection);
            final GuiGameEvent decodedOldViewSelection = serverChannel.readInbound();
            assertThat(decodedOldViewSelection.getMethod()).isEqualTo(ProtocolMethod.selectCard);
            assertThat(decodedOldViewSelection.getGameViewId()).isEqualTo(oldApplied.getId());
            assertThat(decodedOldViewSelection.getObjects()[0]).isInstanceOf(CardView.class);
            assertThat(((CardView) decodedOldViewSelection.getObjects()[0]).getId()).isEqualTo(oldOnlyCardId);
            assertThat(decodedOldViewSelection.getObjects()[0]).isNotSameAs(oldOnlyServerCard);
            assertThat(serverChannel.<Object>readInbound()).isNull();
            assertThat(channel.<Object>readOutbound()).isNull();
        } finally {
            channel.finishAndReleaseAll();
            if (serverChannel != null) serverChannel.finishAndReleaseAll();
            client.beginShutdown();
        }
    }

    @Test
    public void preservesSameGameFullViewAndItsTrackerDuringCanonicalPlayerViewReplication() throws Exception {
        deferGuiTasks = true;
        final boolean previousDeltaSync = RemoteClientGuiGame.useDeltaSync;
        final int playerId = 37;
        final Game game = newGame();
        final Tracker serverTracker = game.getTracker();
        final GameView initialServerView = playerStateView(game, playerId, 0, 80);
        final AtomicReference<PlayerView> lastButtonsOwner = new AtomicReference<>();
        final AtomicReference<PlayerView> lastFullViewPlayer = new AtomicReference<>();
        final NetworkGuiGame gui = mock(NetworkGuiGame.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        doAnswer(invocation -> {
            lastButtonsOwner.set(invocation.getArgument(0));
            return null;
        }).when(gui).updateButtons(any(PlayerView.class), anyString(), anyString(),
                anyBoolean(), anyBoolean(), anyBoolean());
        doAnswer(invocation -> {
            lastFullViewPlayer.set(((GameView) invocation.getArgument(0)).getPlayers().get(0));
            return invocation.callRealMethod();
        }).when(gui).setGameView(any(GameView.class));
        final FGameClient client = spy(new FGameClient("self-copy-test", gui, "localhost", 1));
        doNothing().when(client).send(any(NetEvent.class));
        final CompatibleObjectEncoder serverEncoder = new CompatibleObjectEncoder(new NetworkByteTracker());
        serverEncoder.setTracker(serverTracker);
        final CompatibleObjectDecoder decoder = new CompatibleObjectDecoder(
                10_000_000, ClassResolvers.cacheDisabled(null));
        final CompatibleObjectEncoder clientEncoder = new CompatibleObjectEncoder(new NetworkByteTracker());
        final GameClientHandler handler = new GameClientHandler(client);
        final EmbeddedChannel channel = new EmbeddedChannel(clientEncoder, decoder, handler);
        final CountDownLatch selfCopyEntered = new CountDownLatch(1);
        final CountDownLatch releaseSelfCopy = new CountDownLatch(1);
        final CountDownLatch fullCallbackQueued = new CountDownLatch(1);
        final CountDownLatch allowButtonsCallback = new CountDownLatch(1);
        final CountDownLatch allowNextGameCallback = new CountDownLatch(1);
        final CountDownLatch workReady = new CountDownLatch(1);
        final AtomicReference<Thread> ioThreadRef = new AtomicReference<>();
        Throwable primaryFailure = null;
        deferredGuiProgress = workReady;
        RemoteClientGuiGame.useDeltaSync = false;
        try {
            final ByteBuf initialFrame = encode(serverEncoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, initialServerView.getId(), initialServerView, 1L));
            channel.writeInbound(initialFrame);
            final PlayerView canonicalPlayer = gui.getGameView().getPlayers().get(0);
            final Tracker originalTracker = canonicalPlayer.getTracker();
            assertThat(canonicalPlayer.getHand()).isEmpty();
            assertThat(canonicalPlayer.getLibrary()).hasSize(80);

            // A normal decoded IdRef reaches the canonical object and its self-copy is harmless.
            clientEncoder.setTracker(canonicalPlayer.getTracker());
            final GuiGameEvent buttonsEvent = GuiGameEvent.forGameView(
                    ProtocolMethod.updateButtons, initialServerView.getId(), canonicalPlayer,
                    "OK", "Cancel", false, false, false);
            final ByteBuf normalButtons = encode(clientEncoder, buttonsEvent);
            channel.writeInbound(normalButtons);
            assertThat(lastButtonsOwner.get()).isSameAs(canonicalPlayer);
            assertThat(gui.getGameView().getPlayers().get(0).getLibrary()).hasSize(80);

            final BlockingTrackableCollection<CardView> blockingLibrary = new BlockingTrackableCollection<>(
                    List.copyOf(canonicalPlayer.getLibrary()), selfCopyEntered, workReady, releaseSelfCopy);
            canonicalPlayer.set(TrackableProperty.Library, blockingLibrary);

            final ByteBuf concurrentButtons = encode(clientEncoder, buttonsEvent);
            final GameView fullServerView = playerStateView(game, playerId, 7, 66);
            final ByteBuf fullFrame = encode(serverEncoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, fullServerView.getId(), fullServerView, 2L));
            final Game nextGame = newGame();
            final GameView nextServerView = playerStateView(nextGame, playerId, 0, 80);
            serverEncoder.setTracker(nextGame.getTracker());
            final ByteBuf nextGameFrame = encode(serverEncoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, nextServerView.getId(), nextServerView, 3L));
            serverEncoder.setTracker(serverTracker);
            final AtomicReference<Throwable> ioFailure = new AtomicReference<>();
            final Thread ioThread = new Thread(() -> {
                try {
                    // The real channel processes FULL before the next protocol callback.
                    // Its GUI apply remains queued while the test controls the EDT ordering.
                    channel.writeInbound(fullFrame);
                    fullCallbackQueued.countDown();
                    if (!allowButtonsCallback.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting to continue protocol input");
                    }
                    channel.writeInbound(concurrentButtons);
                    if (!allowNextGameCallback.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting to continue next-game input");
                    }
                    channel.writeInbound(nextGameFrame);
                } catch (Throwable failure) {
                    ioFailure.set(failure);
                }
            }, "trackable-self-copy-probe");
            ioThreadRef.set(ioThread);
            ioThread.start();
            assertThat(fullCallbackQueued.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(queuedGuiTasks).hasSize(1);
            allowButtonsCallback.countDown();
            assertThat(workReady.await(5, TimeUnit.SECONDS)).isTrue();

            if (selfCopyEntered.getCount() == 0) {
                // The old IO-thread replication path is held inside canonical self-copy here.
                // Applying FULL now reproduces the lost 7/66 update before the test fails below.
                queuedGuiTasks.remove().run();
                final int handSeenDuringCopy = gui.getGameView().getPlayers().get(0).getHand().size();
                final int librarySeenDuringCopy = gui.getGameView().getPlayers().get(0).getLibrary().size();
                releaseSelfCopy.countDown();
                assertThat(handSeenDuringCopy).isEqualTo(7);
                assertThat(librarySeenDuringCopy).isEqualTo(66);
            }
            allowNextGameCallback.countDown();
            ioThread.join(TimeUnit.SECONDS.toMillis(5));
            assertThat(ioThread.isAlive()).isFalse();
            assertThat(ioFailure.get()).isNull();
            assertThat(queuedGuiTasks).hasSize(3);

            // Apply queued FULL N first, after IO has already decoded updateButtons(N) and
            // FULL N+1. The queued update must retain N's PlayerView tracker after handler
            // state has advanced to N+1.
            final Runnable queuedFullApply = queuedGuiTasks.poll(5, TimeUnit.SECONDS);
            assertThat(queuedFullApply).isNotNull();
            queuedFullApply.run();
            final PlayerView incomingPlayer = lastFullViewPlayer.get();
            assertThat(incomingPlayer).isNotSameAs(canonicalPlayer);
            assertThat(incomingPlayer.getHand()).hasSize(7);
            assertThat(incomingPlayer.getLibrary()).hasSize(66);
            assertThat(gui.getGameView().getPlayers().get(0)).isSameAs(canonicalPlayer);
            assertThat(gui.getGameView().getPlayers().get(0).getHand()).hasSize(7);
            assertThat(gui.getGameView().getPlayers().get(0).getLibrary()).hasSize(66);
            assertThat(canonicalPlayer.getTracker()).isSameAs(originalTracker);

            releaseSelfCopy.countDown();
            final Runnable queuedUpdate = queuedGuiTasks.poll(5, TimeUnit.SECONDS);
            assertThat(queuedUpdate).isNotNull();
            queuedUpdate.run();
            assertThat(lastButtonsOwner.get()).isSameAs(canonicalPlayer);
            assertThat(lastButtonsOwner.get().getTracker()).isSameAs(originalTracker);
            assertThat(canonicalPlayer.getHand()).hasSize(7);
            assertThat(canonicalPlayer.getLibrary()).hasSize(66);
            final Runnable queuedNextGameApply = queuedGuiTasks.poll(5, TimeUnit.SECONDS);
            assertThat(queuedNextGameApply).isNotNull();
            queuedNextGameApply.run();
            assertThat(gui.getGameView().getId()).isEqualTo(nextServerView.getId());
            assertThat(gui.getGameView().getPlayers().get(0).getTracker()).isNotSameAs(originalTracker);
            assertThat(gui.getGameView().getPlayers().get(0).getHand()).isEmpty();
            assertThat(gui.getGameView().getPlayers().get(0).getLibrary()).hasSize(80);
            assertThat(queuedGuiTasks).isEmpty();
        } catch (Exception | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            releaseSelfCopy.countDown();
            allowButtonsCallback.countDown();
            allowNextGameCallback.countDown();
            Throwable cleanupFailure = null;
            try {
                final Thread ioThread = ioThreadRef.get();
                if (ioThread != null) {
                    ioThread.join(TimeUnit.SECONDS.toMillis(5));
                    if (ioThread.isAlive()) {
                        throw new IllegalStateException("probe IO thread did not stop before channel cleanup");
                    }
                }
                channel.finishAndReleaseAll();
                client.beginShutdown();
            } catch (Throwable failure) {
                cleanupFailure = failure;
            } finally {
                deferredGuiProgress = null;
                RemoteClientGuiGame.useDeltaSync = previousDeltaSync;
            }
            if (cleanupFailure != null) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(cleanupFailure);
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

    @Test
    public void refusesDanDanFullViewOnMobileBeforeBindingOrGuiDispatch() throws Exception {
        libgdxPort = true;
        final GameView danDanView = gameView(newGame(true), 37, List.of());
        final CountDownLatch noticeShown = new CountDownLatch(1);
        final AtomicReference<String> noticeText = new AtomicReference<>();
        final IGuiGame gui = mock(IGuiGame.class);
        doAnswer(invocation -> {
            noticeText.set(invocation.getArgument(0));
            noticeShown.countDown();
            return null;
        }).when(gui).showErrorDialog(anyString(), anyString());
        final FGameClient client = spy(new FGameClient("mobile-dandan-test", gui, "localhost", 1));
        final EmbeddedChannel channel = newClientChannel(client);
        try {
            discardClientLogin(channel);
            when(client.isActiveChannel(channel)).thenReturn(true);

            channel.writeInbound(encodeGameView(danDanView));

            assertThat(channel.isOpen()).isFalse();
            verify(client, times(1)).close();
            verify(client, never()).receivedGameView(any(Channel.class), anyInt());
            verify(gui, never()).setGameView(any(GameView.class), anyLong());
            assertThat(client.hasCurrentGameViewBinding(channel)).isFalse();
            assertThat(noticeShown.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(noticeText.get()).isEqualTo(Localizer.getInstance()
                    .getMessage("lblDanDanNotSupportedOnMobile"));
        } finally {
            channel.finishAndReleaseAll();
            client.close();
        }
    }

    @Test
    public void acceptsOrdinaryMobileGameView() throws Exception {
        assertGameViewAllowed(true, false);
    }

    @Test
    public void acceptsDanDanGameViewOnDesktop() throws Exception {
        assertGameViewAllowed(false, true);
    }

    @Test
    public void nonactiveMobileDanDanSourceCannotCloseOrNotifyTheActiveClient() throws Exception {
        libgdxPort = true;
        final GameView danDanView = gameView(newGame(true), 37, List.of());
        final IGuiGame gui = mock(IGuiGame.class);
        final FGameClient client = spy(new FGameClient("stale-mobile-dandan-test", gui, "localhost", 1));
        final EmbeddedChannel channel = newClientChannel(client);
        try {
            discardClientLogin(channel);

            channel.writeInbound(encodeGameView(danDanView));

            assertThat(channel.isOpen()).isFalse();
            verify(client, never()).close();
            verify(gui, never()).showErrorDialog(anyString(), anyString());
            verify(client, never()).receivedGameView(any(Channel.class), anyInt());
            verify(gui, never()).setGameView(any(GameView.class), anyLong());
        } finally {
            channel.finishAndReleaseAll();
            client.close();
        }
    }

    private void assertGameViewAllowed(boolean mobile, boolean danDan) throws Exception {
        libgdxPort = mobile;
        final GameView view = gameView(newGame(danDan), 37, List.of());
        final AtomicReference<GameView> appliedView = new AtomicReference<>();
        final IGuiGame gui = mock(IGuiGame.class);
        doAnswer(invocation -> {
            appliedView.set(invocation.getArgument(0));
            return null;
        }).when(gui).setGameView(any(GameView.class), anyLong());
        final FGameClient client = spy(new FGameClient("game-view-compatibility-test", gui, "localhost", 1));
        final EmbeddedChannel channel = newClientChannel(client);
        try {
            discardClientLogin(channel);
            when(client.isActiveChannel(channel)).thenReturn(true);

            channel.writeInbound(encodeGameView(view));

            assertThat(channel.isOpen()).isTrue();
            assertThat(appliedView.get()).isNotNull();
            assertThat(appliedView.get().isDanDan()).isEqualTo(danDan);
            assertThat(client.hasCurrentGameViewBinding(channel)).isTrue();
            verify(client, never()).close();
        } finally {
            channel.finishAndReleaseAll();
            client.close();
        }
    }

    private static Game newGame() {
        return newGame(false);
    }

    private static Game newGame(boolean danDan) {
        final GameRules rules = new GameRules(GameType.Constructed);
        if (danDan) {
            rules.addAppliedVariant(GameType.DanDan);
        }
        final Match match = new Match(rules, List.of(), "codec tracker test");
        return new Game(List.of(), rules, match);
    }

    private static EmbeddedChannel newClientChannel(FGameClient client) {
        return new EmbeddedChannel(
                new CompatibleObjectEncoder(new NetworkByteTracker()),
                new CompatibleObjectDecoder(10_000_000, ClassResolvers.cacheDisabled(null)),
                new GameClientHandler(client));
    }

    private static void discardClientLogin(EmbeddedChannel channel) {
        final ByteBuf login = channel.readOutbound();
        assertThat(login).isNotNull();
        login.release();
        assertThat(channel.<Object>readOutbound()).isNull();
    }

    private static ByteBuf encodeGameView(GameView view) throws Exception {
        final CompatibleObjectEncoder encoder = new CompatibleObjectEncoder(new NetworkByteTracker());
        encoder.setTracker(view.getTracker());
        return encode(encoder, GuiGameEvent.forGameView(
                ProtocolMethod.setGameView, view.getId(), view, 1L));
    }

    private static GameView gameView(Game game, int playerId, List<CardView> hand) {
        final Tracker tracker = game.getTracker();
        final GameView view = new GameView(game);
        final PlayerView player = new PlayerView(playerId, tracker);
        player.set(TrackableProperty.Hand, new TrackableCollection<>(hand));
        view.set(TrackableProperty.Players, new TrackableCollection<>(List.of(player)));
        return view;
    }

    private static GameView playerStateView(Game game, int playerId, int handCount, int libraryCount) {
        final Tracker tracker = game.getTracker();
        final PlayerView player = new PlayerView(playerId, tracker);
        final List<CardView> hand = new ArrayList<>();
        final List<CardView> library = new ArrayList<>();
        for (int i = 0; i < handCount; i++) {
            final int cardId = i + 1;
            hand.add(new CardView(cardId, tracker, "player hand " + cardId));
        }
        final int libraryStart = handCount == 7 ? 15 : 1;
        for (int i = 0; i < libraryCount; i++) {
            final int cardId = libraryStart + i;
            library.add(new CardView(cardId, tracker, "player library " + cardId));
        }
        player.set(TrackableProperty.Hand, new TrackableCollection<>(hand));
        player.set(TrackableProperty.Library, new TrackableCollection<>(library));
        final GameView view = new GameView(game);
        view.set(TrackableProperty.Players, new TrackableCollection<>(List.of(player)));
        return view;
    }

    private static final class BlockingTrackableCollection<T extends TrackableObject>
            extends TrackableCollection<T> {
        private final CountDownLatch entered;
        private final CountDownLatch progress;
        private final CountDownLatch release;
        private final AtomicBoolean blockFirstSnapshot = new AtomicBoolean(true);

        private BlockingTrackableCollection(Collection<T> cards, CountDownLatch entered,
                                            CountDownLatch progress, CountDownLatch release) {
            super(cards);
            this.entered = entered;
            this.progress = progress;
            this.release = release;
        }

        @Override
        public <E> E[] toArray(E[] array) {
            if (blockFirstSnapshot.compareAndSet(true, false)) {
                entered.countDown();
                progress.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting to release trackable copy");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }
            return super.toArray(array);
        }
    }

    private static ByteBuf encode(CompatibleObjectEncoder encoder, GuiGameEvent event) throws Exception {
        return encoder.encodeToBuf(event, UnpooledByteBufAllocator.DEFAULT);
    }
}
