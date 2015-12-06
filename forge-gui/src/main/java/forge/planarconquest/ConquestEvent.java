package forge.planarconquest;

import java.util.Set;

import forge.deck.Deck;
import forge.game.GameType;

public abstract class ConquestEvent {
    private String opponentName;
    private Deck opponentDeck;

    public String getOpponentName() {
        return opponentName;
    }

    public Deck getOpponentDeck() {
        return opponentDeck;
    }

    public abstract void addVariants(Set<GameType> variants);
    public abstract String getAvatarImageKey();
}
