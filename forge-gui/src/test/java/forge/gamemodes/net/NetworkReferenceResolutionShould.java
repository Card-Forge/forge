package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.game.event.GameEventCardTapped;
import forge.game.player.PlayerView;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How the card and player references the host sends resolve on a network client, in protocol args, wrapped events
 * and game views. Each case pins one way a reference could resolve to the wrong view, or to none.
 */
public class NetworkReferenceResolutionShould {

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

    private GuiGameEvent send(GuiGameEvent event) throws Exception {
        channel.writeInbound(encoder.encodeToBuf(event, ByteBufAllocator.DEFAULT));
        return channel.readInbound();
    }

    private CardView sendInEvent(CardView card) {
        List<Object> wrapped = TrackableSerializer.wrapEvents(List.of(new GameEventCardTapped(card, true)), host);
        return ((GameEventCardTapped) TrackableSerializer.unwrapEvents(wrapped, client).get(0)).card();
    }

    private static CardView card(Tracker tracker, String name) {
        CardView card = new CardView(CARD_ID, tracker);
        card.set(TrackableProperty.Name, name);
        card.getCurrentState().set(TrackableProperty.Name, name);
        return card;
    }
}
