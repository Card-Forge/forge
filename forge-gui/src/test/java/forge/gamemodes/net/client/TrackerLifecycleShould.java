package forge.gamemodes.net.client;

import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.event.GameEventAddLog;
import forge.game.event.GameEventSnapshotRestored;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolGuiGame;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.player.HumanControlledGame;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static forge.gamemodes.net.ProtocolMethod.applyDelta;
import static forge.gamemodes.net.ProtocolMethod.openView;
import static forge.gamemodes.net.ProtocolMethod.setGameView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * A network game's tracker lifecycle, which matches the host's: each game has its own tracker and restarts card ids
 * at 1. The host sends one full view per game, before openView, and the client builds a new tracker from it. Once a
 * game is under way, a resync or an undo re-sends every object in a delta instead.
 */
public class TrackerLifecycleShould {

    private IGuiBase previousGuiBase;

    @BeforeMethod
    public void mockGuiBase() {
        previousGuiBase = GuiBase.getInterface();
        final IGuiBase guiBase = Mockito.mock(IGuiBase.class);
        // Runs the GUI's half of each message at once, so the test sees it before the next message
        Mockito.doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; }).when(guiBase).invokeInEdtLater(any());
        GuiBase.setInterface(guiBase);
    }

    @AfterMethod
    public void restoreGuiBase() {
        GuiBase.setInterface(previousGuiBase);
    }

    // Fails if the client keeps one tracker across games, or merges a full view into the previous game's objects
    @Test
    public void resolveReusedIdsToTheNewGame() throws Exception {
        final HumanControlledGame first = gameWith("Lightning Bolt");
        final HumanControlledGame second = gameWith("Shock");
        final CardView shock = cardOf(second);
        assertThat(shock.getId()).isEqualTo(cardOf(first).getId());

        final IGuiGame gui = Mockito.mock(IGuiGame.class);
        // Handlers are added after the channel is active, so the handler's login on channelActive does not run
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(
                new CompatibleObjectEncoder(null),
                new CompatibleObjectDecoder(9766 * 1024, ClassResolvers.cacheDisabled(null)),
                new GameClientHandler(new FGameClient("Alice", gui, "localhost", 0)));
        final CompatibleObjectEncoder host = new CompatibleObjectEncoder(null);
        try {
            receive(channel, host, first.game.getTracker(), new GuiGameEvent(setGameView, first.game.getView(), 0L));
            receive(channel, host, second.game.getTracker(), new GuiGameEvent(setGameView, second.game.getView(), 0L));
            assertThat(second.game.getTracker().getObj(TrackableTypes.CardViewType, shock.getId()))
                    .as("the host sends the card as a reference").isNotNull();
            receive(channel, host, second.game.getTracker(), new GuiGameEvent(ProtocolMethod.setCard, shock));
        } finally {
            channel.finishAndReleaseAll();
        }

        final ArgumentCaptor<GameView> views = ArgumentCaptor.forClass(GameView.class);
        verify(gui, times(2)).setGameView(views.capture(), anyLong());
        final CardView inSecondView = views.getAllValues().get(1).getPlayers().get(0).getGraveyard().get(0);
        final ArgumentCaptor<CardView> shown = ArgumentCaptor.forClass(CardView.class);
        verify(gui).setCard(shown.capture());
        assertThat(shown.getValue()).isSameAs(inSecondView);
        assertThat(shown.getValue().getName()).isEqualTo("Shock");
        assertThat(shown.getValue().getZone()).isEqualTo(ZoneType.Graveyard);
    }

    // Fails if anything goes out before a game's full view, if the full view goes after openView, more than once or
    // not at all, or if a subgame switch re-setting the same game sends one
    @Test
    public void sendOneFullViewPerGame() {
        final List<GuiGameEvent> sent = new ArrayList<>();
        final ProtocolGuiGame host = recordingHost(sent);
        final HumanControlledGame firstGame = gameWith("Lightning Bolt");
        final GameView first = firstGame.game.getView();
        final GameView second = gameWith("Shock").game.getView();

        host.setGameView(first);
        host.updateGameView();
        assertThat(sent).as("nothing goes before the full view").isEmpty();
        host.openView(seat(first));
        host.updateGameView();
        host.setGameView(first);
        // A change after the re-set still goes out as a delta, so the re-set did not restart sync
        firstGame.human.setLife(19, null);
        host.updateGameView();
        host.setGameView(second);
        host.openView(seat(second));

        assertThat(sent.stream().map(GuiGameEvent::getMethod).toList()).containsExactly(
                setGameView, openView, applyDelta, applyDelta,
                setGameView, openView);
    }

    // Fails if a resync or an undo sends a full view, which would give the client a new tracker mid-game, or only the
    // changed objects, so an object the client has wrong stays wrong.
    // The game never starts, so the undo takes the path of a client that reconnected before the first turn.
    @Test
    public void resendEverythingOnResyncAndUndo() {
        final List<GuiGameEvent> sent = new ArrayList<>();
        final ProtocolGuiGame host = recordingHost(sent);
        final GameView view = gameWith("Lightning Bolt").game.getView();
        host.setGameView(view);
        host.openView(seat(view));

        final Set<Integer> everyObject = newObjectsSentBy(sent, host::updateGameView);
        assertThat(newObjectsSentBy(sent, host::updateGameView)).isEmpty();
        assertThat(newObjectsSentBy(sent, () -> { host.setResyncPending(); host.updateGameView(); }))
                .as("resync").containsExactlyInAnyOrderElementsOf(everyObject);
        assertThat(newObjectsSentBy(sent, () -> host.handleGameEvents(List.of(new GameEventSnapshotRestored(false)))))
                .as("undo").containsExactlyInAnyOrderElementsOf(everyObject);
    }

    // Fails if a reconnect sends more than one full view, or the client's new view does not get the game's log back
    @Test
    public void replayTheLogAfterAReconnect() {
        final List<GuiGameEvent> sent = new ArrayList<>();
        final ProtocolGuiGame host = recordingHost(sent);
        final HumanControlledGame g = gameWith("Lightning Bolt");
        final GameView view = g.game.getView();
        host.setGameView(view);
        host.openView(seat(view));
        g.game.getGameLog().add(GameLogEntryType.TURN, "Turn 1 (Human)");
        g.game.getGameLog().add(GameLogEntryType.STACK_ADD, "Human cast Lightning Bolt");
        sent.clear();

        // As FServerManager.resumeAndResync does for a client that reconnects
        host.resetSync();
        host.openView(seat(view));

        assertThat(sent.stream().map(GuiGameEvent::getMethod).toList()).containsExactly(setGameView, openView, applyDelta);
        assertThat(((DeltaPacket) sent.get(2).getObjects()[0]).getEvents())
                .extracting(e -> ((GameEventAddLog) e).message())
                .containsExactlyElementsOf(g.game.getGameLog().getAllEntries().stream().map(GameLogEntry::message).toList());
    }


    private static void receive(final EmbeddedChannel channel, final CompatibleObjectEncoder host,
                                final Tracker hostTracker, final GuiGameEvent event) throws Exception {
        host.setTracker(hostTracker);
        channel.writeInbound(host.encodeToBuf(event, ByteBufAllocator.DEFAULT));
        channel.checkException();
    }

    private static ProtocolGuiGame recordingHost(final List<GuiGameEvent> sent) {
        return new ProtocolGuiGame(new IRemote() {
            @Override
            public void send(final NetEvent event) {
                sent.add((GuiGameEvent) event);
            }

            @Override
            public Object sendAndWait(final IdentifiableNetEvent event) {
                throw new AssertionError("no prompt expected: " + event);
            }
        });
    }

    private static Set<Integer> newObjectsSentBy(final List<GuiGameEvent> sent, final Runnable action) {
        final int from = sent.size();
        action.run();
        final Set<Integer> keys = new HashSet<>();
        for (final GuiGameEvent event : sent.subList(from, sent.size())) {
            assertThat(event.getMethod()).isEqualTo(applyDelta);
            keys.addAll(((DeltaPacket) event.getObjects()[0]).getNewObjects().keySet());
        }
        return keys;
    }

    private static TrackableCollection<PlayerView> seat(final GameView view) {
        return new TrackableCollection<>(view.getPlayers().get(0));
    }

    /** An unstarted game with one card in the first player's graveyard, built without the card database. */
    private static HumanControlledGame gameWith(final String cardName) {
        final HumanControlledGame g = new HumanControlledGame();
        // The graveyard, not the hand: a human hand reads the hand-ordering preference from FModel
        g.human.getZone(ZoneType.Graveyard).add(g.card(g.human, cardName, "Types:Instant"));
        return g;
    }

    private static CardView cardOf(final HumanControlledGame g) {
        return g.human.getZone(ZoneType.Graveyard).get(0).getView();
    }
}
