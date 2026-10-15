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
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.event.ReplyEvent;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.trackable.TrackableCollection;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
    private LinkedBlockingQueue<Runnable> queuedGuiTasks;
    private boolean deferGuiTasks;
    private boolean libgdxPort;

    @BeforeMethod
    public void installHeadlessGuiBase() {
        previousGui = GuiBase.getInterface();
        // Match the established forge-gui engine test bootstrap before GameType initializes.
        Localizer.getInstance().initialize("en-US", "res/languages/");
        Lang.createInstance("en-US");
        queuedGuiTasks = new LinkedBlockingQueue<>();
        deferGuiTasks = false;
        libgdxPort = false;
        final IGuiBase guiBase = mock(IGuiBase.class);
        when(guiBase.isLibgdxPort()).thenAnswer(invocation -> libgdxPort);
        when(guiBase.isGuiThread()).thenAnswer(invocation ->
                !deferGuiTasks);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(guiBase).invokeInEdtNow(any(Runnable.class));
        doAnswer(invocation -> {
            final Runnable task = invocation.getArgument(0);
            if (!deferGuiTasks) {
                task.run();
            } else {
                queuedGuiTasks.add(task);
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
        final Game oldGame = newGame(true);
        final Game nextGame = newGame(true);
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
            discardClientLogin(channel);

            FThreads.invokeInEdtAndWait(() -> channel.writeInbound(coalescedFrames));

            assertThat(appliedViews).hasSize(3);
            final GameView oldApplied = appliedViews.get(0);
            assertThat(oldApplied.isDanDan()).isTrue();
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

        } finally {
            channel.finishAndReleaseAll();
            if (serverChannel != null) serverChannel.finishAndReleaseAll();
            client.beginShutdown();
        }
    }

    @Test
    public void preservesSameGameFullViewAndItsTrackerDuringCanonicalPlayerViewReplication() throws Exception {
        final boolean previousDeltaSync = RemoteClientGuiGame.useDeltaSync;
        final int playerId = 37;
        final Game game = newGame();
        final Tracker serverTracker = game.getTracker();
        final GameView initialServerView = gameView(game, playerId, List.of());
        final AtomicReference<PlayerView> lastButtonsOwner = new AtomicReference<>();
        final NetworkGuiGame gui = mock(NetworkGuiGame.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        doAnswer(invocation -> {
            lastButtonsOwner.set(invocation.getArgument(0));
            return null;
        }).when(gui).updateButtons(any(PlayerView.class), anyString(), anyString(),
                anyBoolean(), anyBoolean(), anyBoolean());
        final FGameClient client = spy(new FGameClient("queued-replication-test", gui, "localhost", 1));
        doNothing().when(client).send(any(NetEvent.class));
        final CompatibleObjectEncoder serverEncoder = new CompatibleObjectEncoder(new NetworkByteTracker());
        serverEncoder.setTracker(serverTracker);
        final CompatibleObjectDecoder decoder = new CompatibleObjectDecoder(
                10_000_000, ClassResolvers.cacheDisabled(null));
        final CompatibleObjectEncoder clientEncoder = new CompatibleObjectEncoder(new NetworkByteTracker());
        final EmbeddedChannel channel = new EmbeddedChannel(clientEncoder, decoder, new GameClientHandler(client));
        RemoteClientGuiGame.useDeltaSync = false;
        try {
            channel.writeInbound(encode(serverEncoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, initialServerView.getId(), initialServerView, 1L)));
            final PlayerView canonicalPlayer = gui.getGameView().getPlayers().get(0);
            final Tracker originalTracker = canonicalPlayer.getTracker();
            clientEncoder.setTracker(originalTracker);
            final ByteBuf buttonsFrame = encode(clientEncoder, GuiGameEvent.forGameView(
                    ProtocolMethod.updateButtons, initialServerView.getId(), canonicalPlayer,
                    "OK", "Cancel", false, false, false));
            final CardView drawnCard = new CardView(23, serverTracker, "drawn card");
            final GameView fullServerView = gameView(game, playerId, List.of(drawnCard));
            final ByteBuf fullFrame = encode(serverEncoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, fullServerView.getId(), fullServerView, 2L));
            final Game nextGame = newGame();
            final GameView nextServerView = gameView(nextGame, playerId, List.of());
            serverEncoder.setTracker(nextGame.getTracker());
            final ByteBuf nextGameFrame = encode(serverEncoder, GuiGameEvent.forGameView(
                    ProtocolMethod.setGameView, nextServerView.getId(), nextServerView, 3L));
            final CardView libraryCard = new CardView(51, originalTracker, "library card");
            final CopyCountingCollection beforeCallback = new CopyCountingCollection(libraryCard);
            canonicalPlayer.set(TrackableProperty.Library, beforeCallback);

            // Decode all three frames before applying GUI callbacks. The handler has already
            // advanced to N+1 when the queued callback for N replicates its PlayerView.
            deferGuiTasks = true;
            channel.writeInbound(fullFrame);
            channel.writeInbound(buttonsFrame);
            channel.writeInbound(nextGameFrame);
            assertThat(queuedGuiTasks).hasSize(3);
            assertThat(lastButtonsOwner.get()).isNull();
            assertThat(canonicalPlayer.getHand()).isEmpty();
            assertThat(beforeCallback.snapshots).as("decoding must not replicate PlayerView properties").isZero();

            deferGuiTasks = false;
            queuedGuiTasks.remove().run();
            assertThat(gui.getGameView().getPlayers().get(0)).isSameAs(canonicalPlayer);
            assertThat(canonicalPlayer.getHand()).hasSize(1);
            assertThat(canonicalPlayer.getHand().get(0).getId()).isEqualTo(drawnCard.getId());

            // FULL replaces the library property, so observe the callback's self-copy separately.
            final CopyCountingCollection duringCallback = new CopyCountingCollection(libraryCard);
            canonicalPlayer.set(TrackableProperty.Library, duringCallback);
            queuedGuiTasks.remove().run();
            assertThat(duringCallback.snapshots).as("the queued callback replicates PlayerView properties").isPositive();
            assertThat(lastButtonsOwner.get()).isSameAs(canonicalPlayer);
            assertThat(lastButtonsOwner.get().getTracker()).isSameAs(originalTracker);
            assertThat(canonicalPlayer.getHand()).hasSize(1);

            queuedGuiTasks.remove().run();
            assertThat(gui.getGameView().getId()).isEqualTo(nextServerView.getId());
            assertThat(gui.getGameView().getPlayers().get(0).getTracker()).isNotSameAs(originalTracker);
            assertThat(gui.getGameView().getPlayers().get(0).getHand()).isEmpty();
            assertThat(queuedGuiTasks).isEmpty();
        } finally {
            deferGuiTasks = false;
            RemoteClientGuiGame.useDeltaSync = previousDeltaSync;
            channel.finishAndReleaseAll();
            client.beginShutdown();
        }
    }

    private static final class CopyCountingCollection extends TrackableCollection<CardView> {
        private int snapshots;

        private CopyCountingCollection(CardView card) {
            super(List.of(card));
        }

        @Override
        public <E> E[] toArray(E[] array) {
            snapshots++;
            return super.toArray(array);
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

    private static ByteBuf encode(CompatibleObjectEncoder encoder, GuiGameEvent event) throws Exception {
        return encoder.encodeToBuf(event, UnpooledByteBufAllocator.DEFAULT);
    }
}
