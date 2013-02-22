package forge.game;

import java.util.ArrayList;
import java.util.Collections;

import forge.deck.Deck;
import forge.game.player.Player;
import forge.item.CardPrinted;
import forge.item.IPaperCard;


public class PlayerStartConditions {
    private final Deck originalDeck;
    private Deck currentDeck;

    private static final Iterable<CardPrinted> EmptyList = Collections.unmodifiableList(new ArrayList<CardPrinted>());
    private int startingLife = 20;
    private int startingHand = 7;
    private Iterable<IPaperCard> cardsOnBattlefield = null;
    private Iterable<? extends IPaperCard> cardsInCommand = null;
    private Iterable<? extends IPaperCard> schemes = null;
    private Iterable<CardPrinted> planes = null;

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
    
    public void setCurrentDeck(Deck currentDeck0) {
        this.currentDeck = currentDeck0; 
    }
    
    
    public final int getStartingLife() {
        return startingLife;
    }
    public final Iterable<? extends IPaperCard> getCardsOnBattlefield(Player p) {
        return cardsOnBattlefield == null ? EmptyList : cardsOnBattlefield;
    }

    public final void setStartingLife(int startingLife) {
        this.startingLife = startingLife;
    }

    public final void setCardsOnBattlefield(Iterable<IPaperCard> cardsOnTable) {
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
    public Iterable<? extends IPaperCard> getCardsInCommand(Player p) {
        return cardsInCommand == null ? EmptyList : cardsInCommand;
    }

    /**
     * @param function the cardsInCommand to set
     */
    public void setCardsInCommand(Iterable<? extends IPaperCard> function) {
        this.cardsInCommand = function;
    }

    /**
     * @return the schemes
     */
    public Iterable<? extends IPaperCard> getSchemes(Player p) {
        return schemes == null ? EmptyList : schemes;
    }

    /**
     * @param schemes0 the schemes to set
     */
    public void setSchemes(Iterable<? extends IPaperCard> s) {
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
        return planes == null ? EmptyList : planes;
    }

    /**
     * @param planes0 the planes to set
     */
    public void setPlanes(Iterable<CardPrinted> planes0) {
        this.planes = planes0;
    }


}
