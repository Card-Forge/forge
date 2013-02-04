package forge.game;

import com.google.common.base.Function;

import forge.Card;
import forge.deck.Deck;
import forge.game.player.Player;
import forge.item.CardPrinted;


public class PlayerStartConditions {
    private final Deck originalDeck;
    private Deck currentDeck;
    
    private int startingLife = 20;
    private int startingHand = 7;
    private Function<Player, Iterable<Card>> cardsOnBattlefield = null;
    private Function<Player, Iterable<CardPrinted>> cardsInCommand = null;
    private Function<Player, Iterable<CardPrinted>> schemes = null;
    private Function<Player, Iterable<CardPrinted>> planes = null;

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
    public Iterable<CardPrinted> getCardsInCommand(Player p) {
        return cardsInCommand == null ? null : cardsInCommand.apply(p);
    }

    /**
     * @param function the cardsInCommand to set
     */
    public void setCardsInCommand(Function<Player, Iterable<CardPrinted>> function) {
        this.cardsInCommand = function;
    }

    /**
     * @return the schemes
     */
    public Iterable<CardPrinted> getSchemes(Player p) {
        return schemes == null ? null : schemes.apply(p);
    }

    /**
     * @param schemes0 the schemes to set
     */
    public void setSchemes(Function<Player, Iterable<CardPrinted>> s) {
        this.schemes = s;
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void restoreOriginalDeck() {
        currentDeck = originalDeck;
    }

    /**
     * @return the planes
     */
    public Iterable<CardPrinted> getPlanes(final Player p) {
        return planes == null ? null : planes.apply(p);
    }

    /**
     * @param planes0 the planes to set
     */
    public void setPlanes(Function<Player, Iterable<CardPrinted>> planes0) {
        this.planes = planes0;
    }


}
