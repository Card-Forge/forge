package forge.game;

import com.google.common.base.Function;
import forge.Card;
import forge.deck.Deck;
import forge.game.player.Player;


public class PlayerStartConditions {
    private final Deck originalDeck;
    private Deck currentDeck;
    
    private int startingLife = 20;
    private int startingHand = 7;
    private Function<Player, Iterable<Card>> cardsOnBattlefield = null;
    private Function<Player, Iterable<Card>> cardsInCommand = null;
    private Function<Player, Iterable<Card>> schemes = null;

    public PlayerStartConditions(Deck deck0) {
        originalDeck = deck0;
        currentDeck = originalDeck;
    }

    public final Deck getOriginalDeck() {
        return originalDeck;
    }
    
    public final Deck getCurrentDeck() {
        return currentDeck;
    }
    
    public final int getStartingLife() {
        return startingLife;
    }
    public final Iterable<Card> getCardsOnBattlefield(Player p) {
        return cardsOnBattlefield == null ? null : cardsOnBattlefield.apply(p);
    }

    public final void setStartingLife(int startingLife) {
        this.startingLife = startingLife;
    }

    public final void setCardsOnBattlefield(Function<Player, Iterable<Card>> cardsOnTable) {
        this.cardsOnBattlefield = cardsOnTable;
    }

    /**
     * @return the startingHand
     */
    public int getStartingHand() {
        return startingHand;
    }

    /**
     * @param startingHand0 the startingHand to set
     */
    public void setStartingHand(int startingHand0) {
        this.startingHand = startingHand0;
    }

    /**
     * @return the cardsInCommand
     */
    public Iterable<Card> getCardsInCommand(Player p) {
        return cardsInCommand == null ? null : cardsInCommand.apply(p);
    }

    /**
     * @param function the cardsInCommand to set
     */
    public void setCardsInCommand(Function<Player, Iterable<Card>> function) {
        this.cardsInCommand = function;
    }

    /**
     * @return the schemes
     */
    public Iterable<Card> getSchemes(Player p) {
        return schemes == null ? null : schemes.apply(p);
    }

    /**
     * @param schemes0 the schemes to set
     */
    public void setSchemes(Function<Player, Iterable<Card>> s) {
        this.schemes = s;
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void restoreOriginalDeck() {
        currentDeck = originalDeck;
    }


}
