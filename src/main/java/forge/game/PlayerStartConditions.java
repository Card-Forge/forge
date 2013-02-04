package forge.game;

import java.util.ArrayList;
import java.util.Collections;

import com.google.common.base.Function;

import forge.Card;
import forge.deck.Deck;
import forge.game.player.Player;
import forge.item.CardPrinted;


public class PlayerStartConditions {
    private final Deck originalDeck;
    private Deck currentDeck;

    private static final Iterable<CardPrinted> EmptyList = Collections.unmodifiableList(new ArrayList<CardPrinted>());
    private int startingLife = 20;
    private int startingHand = 7;
    private Function<Player, Iterable<Card>> cardsOnBattlefield = null;
    private Iterable<CardPrinted> cardsInCommand = null;
    private Iterable<CardPrinted> schemes = null;
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
        return cardsInCommand == null ? EmptyList : cardsInCommand;
    }

    /**
     * @param function the cardsInCommand to set
     */
    public void setCardsInCommand(Iterable<CardPrinted> function) {
        this.cardsInCommand = function;
    }

    /**
     * @return the schemes
     */
    public Iterable<CardPrinted> getSchemes(Player p) {
        return schemes == null ? EmptyList : schemes;
    }

    /**
     * @param schemes0 the schemes to set
     */
    public void setSchemes(Iterable<CardPrinted> s) {
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
