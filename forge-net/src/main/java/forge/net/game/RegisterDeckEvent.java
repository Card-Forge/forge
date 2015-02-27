package forge.net.game;

import forge.deck.Deck;
import forge.net.game.server.RemoteClient;

public class RegisterDeckEvent implements NetEvent {
    private static final long serialVersionUID = -6553476654530937343L;

    private final Deck deck;
    public RegisterDeckEvent(final Deck deck) {
        this.deck = deck;
    }
    public void updateForClient(final RemoteClient client) {
    }

    public final Deck getDeck() {
        return deck;
    }
}
