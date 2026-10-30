package forge.gamemodes.net;

import forge.game.card.Card;
import forge.game.card.CardCopyService;
import forge.game.card.CardView;
import forge.game.event.GameEventCardTapped;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.gui.control.GameEventForwarder;
import forge.interfaces.IGameController;
import forge.player.HumanControlledGame;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a network host sends and how each side resolves it. Most cases pin one way a card or player reference could
 * resolve to the wrong view, or to none: in protocol args, wrapped events and game views, in either direction. The
 * rest pin which copy of a card the host sends, and how a seat follows control changes.
 */
public class NetworkPlayShould {

    private static final int CONSUMER = 0;
    private static final int CARD_ID = 7;
    private static final int PLAYER_ID = 1;

    private Tracker host;
    private Tracker client;
    private CompatibleObjectEncoder encoder;
    private EmbeddedChannel channel;

    @BeforeMethod
    public void connect() {
        host = new Tracker();
        client = new Tracker();
        encoder = new CompatibleObjectEncoder(null);
        encoder.setTracker(host);
        encoder.setConsumerId(CONSUMER);
        CompatibleObjectDecoder decoder = new CompatibleObjectDecoder(1024 * 1024, ClassResolvers.cacheDisabled(null));
        decoder.setTracker(client);
        channel = new EmbeddedChannel(decoder);
    }

    @AfterMethod
    public void disconnect() {
        channel.finishAndReleaseAll();
    }

    @Test
    public void resolveAHostMessagesCardAfterTheDeltasSentBeforeIt() throws Exception {
        CardView onHost = new CardView(CARD_ID, host);
        onHost.registerConsumer(CONSUMER);
        client.putObj(TrackableTypes.CardViewType, CARD_ID, new CardView(CARD_ID, client));

        GuiGameEvent received = send(new GuiGameEvent(ProtocolMethod.setCard, onHost));
        // The delta ahead of the message replaces the client's view of the card before the message is handled
        CardView replacement = new CardView(CARD_ID, client);
        client.putObj(TrackableTypes.CardViewType, CARD_ID, replacement);

        assertThat(TrackableSerializer.decodeArgs(received.getEncodedObjects(), client)).containsExactly(replacement);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolveACardInAListAfterEarlierDeltas() throws Exception {
        CardView onHost = new CardView(CARD_ID, host);
        onHost.registerConsumer(CONSUMER);
        client.putObj(TrackableTypes.CardViewType, CARD_ID, new CardView(CARD_ID, client));

        GuiGameEvent received = send(new GuiGameEvent(ProtocolMethod.showRevealedCards, List.of(onHost)));
        CardView replacement = new CardView(CARD_ID, client);
        client.putObj(TrackableTypes.CardViewType, CARD_ID, replacement);

        assertThat((List<Object>) TrackableSerializer.decodeArgs(received.getEncodedObjects(), client)[0]).containsExactly(replacement);
    }

    @Test
    public void resolveAHostMessageWithoutACardWhenTheClientReadsIt() throws Exception {
        PlayerView onClient = new PlayerView(PLAYER_ID, client);
        client.putObj(TrackableTypes.PlayerViewType, PLAYER_ID, onClient);

        GuiGameEvent received = send(new GuiGameEvent(ProtocolMethod.showManaPool, new PlayerView(PLAYER_ID, host)));

        assertThat(received.getEncodedObjects()).isNull();
        assertThat(received.getObjects()).containsExactly(onClient);
    }

    @Test
    public void sendACardTheClientWasNeverSentInFull() throws Exception {
        // The host's tracker holds the card, but no delta has carried it to this client
        CardView onHost = card(host, "Lightning Bolt");
        host.putObj(TrackableTypes.CardViewType, CARD_ID, onHost);

        GuiGameEvent received = send(new GuiGameEvent(ProtocolMethod.setCard, onHost));

        assertThat(((CardView) received.getObjects()[0]).getName()).isEqualTo("Lightning Bolt");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sendTheViewsThatFillTheClientTrackerInFull() throws Exception {
        PlayerView onHost = new PlayerView(PLAYER_ID, host);
        onHost.set(TrackableProperty.Name, "Alice");

        GuiGameEvent received = send(new GuiGameEvent(ProtocolMethod.openView, new TrackableCollection<>(onHost)));

        assertThat((TrackableCollection<PlayerView>) received.getObjects()[0]).extracting(PlayerView::getName).containsExactly("Alice");
    }

    @Test
    public void showAnEventsCardByNameWhenTheClientDoesNotHaveIt() {
        CardView onHost = card(host, "Lightning Bolt");
        host.putObj(TrackableTypes.CardViewType, CARD_ID, onHost);

        assertThat(sendInEvent(onHost).getName()).isEqualTo("Lightning Bolt");
    }

    @Test
    public void keepAnEventsOwnCardWhenTheHostHasSinceReplacedIt() {
        CardView inEvent = card(host, "Delver of Secrets");
        // The card has transformed since the event, which gives it a new view on both sides
        host.putObj(TrackableTypes.CardViewType, CARD_ID, card(host, "Insectile Aberration"));
        client.putObj(TrackableTypes.CardViewType, CARD_ID, card(client, "Insectile Aberration"));

        assertThat(sendInEvent(inEvent).getName()).isEqualTo("Delver of Secrets");
    }

    // Fails if a stale copy of a card, walked after the live one, replaces it in the delta
    @Test
    public void sendTheLiveCardOverAStaleCopy() {
        HumanControlledGame g = new HumanControlledGame();
        g.human.updateOpponentsForView();
        g.opponent.updateOpponentsForView();
        Card commander = g.card(g.human, "Kozilek", "Types:Legendary Creature Eldrazi", "PT:10/9");
        // The commander list keeps the card as it was before it entered the battlefield, at the same id
        g.human.addCommander(new CardCopyService(commander).copyCard(false));
        Card entered = g.game.getAction().moveToPlay(commander, null, null);
        // The walk reaches the opponent through the human's opponents, so it reaches the opponent's battlefield before
        // the human's commander list
        entered.addTempController(g.opponent, 1L);
        g.game.getAction().controllerChangeZoneCorrection(entered);
        Card live = g.opponent.getZone(ZoneType.Battlefield).get(0);
        live.setTapped(true);
        CardView stale = g.human.getView().getCommanders().get(0);
        assertThat(stale).isNotSameAs(live.getView());
        assertThat(stale.getId()).isEqualTo(live.getId());

        DeltaPacket packet = new DeltaSyncManager().collectDeltas(g.game.getView());

        assertThat(packet.getNewObjects().get(DeltaPacket.makeDeltaKey(live.getView())))
                .containsEntry(TrackableProperty.Tapped, true);
    }

    // Fails if a seat's host proxy does not register an opponent its player gains control of
    @Test
    public void acceptAControlledOpponent() {
        HumanControlledGame g = new HumanControlledGame();
        ProtocolGuiGame seat = seatOf(g);
        GameEventForwarder events = forwarderTo(seat, g);

        g.opponent.addController(1L, g.human);
        events.flush();
        seat.setCurrentPlayer(g.opponent.getView());

        assertThat(seat.getCurrentPlayer()).isEqualTo(g.opponent.getView());
    }

    // Fails if the game-end control event, which names no new controller, keeps the opponent local to the seat
    @Test
    public void releaseAControlledOpponentAtGameEnd() {
        HumanControlledGame g = new HumanControlledGame();
        ProtocolGuiGame seat = seatOf(g);
        GameEventForwarder events = forwarderTo(seat, g);
        g.opponent.addController(1L, g.human);
        events.flush();

        // As Game.setGameOver does
        g.opponent.clearController();
        events.flush();

        // clearController does not update the view, so the opponent still names the human as its master
        assertThat(g.opponent.getView().getMindSlaveMaster()).isEqualTo(g.human.getView());
        assertThat(seat.isLocalPlayer(g.opponent.getView())).isFalse();
    }

    private GuiGameEvent send(GuiGameEvent event) throws Exception {
        channel.writeInbound(encoder.encodeToBuf(event, ByteBufAllocator.DEFAULT));
        return channel.readInbound();
    }

    private CardView sendInEvent(CardView card) {
        List<Object> wrapped = TrackableSerializer.wrapEvents(List.of(new GameEventCardTapped(card, true)), host);
        return ((GameEventCardTapped) TrackableSerializer.unwrapEvents(wrapped, client).get(0)).card();
    }

    /** The host's proxy for the human's seat, registered as a game in progress would register it. */
    private static ProtocolGuiGame seatOf(HumanControlledGame g) {
        ProtocolGuiGame seat = new ProtocolGuiGame(new IRemote() {
            @Override
            public void send(NetEvent event) {
            }

            @Override
            public Object sendAndWait(IdentifiableNetEvent event) {
                throw new AssertionError("no prompt expected: " + event);
            }
        });
        seat.setGameView(g.game.getView());
        seat.setOriginalGameController(g.human.getView(), Mockito.mock(IGameController.class));
        return seat;
    }

    /** Forwards the game's events to the seat in batches, as HostedMatch does. */
    private static GameEventForwarder forwarderTo(ProtocolGuiGame seat, HumanControlledGame g) {
        GameEventForwarder forwarder = new GameEventForwarder(seat);
        g.game.subscribeToEvents(forwarder);
        return forwarder;
    }

    private static CardView card(Tracker tracker, String name) {
        CardView card = new CardView(CARD_ID, tracker);
        card.set(TrackableProperty.Name, name);
        card.getCurrentState().set(TrackableProperty.Name, name);
        return card;
    }
}
