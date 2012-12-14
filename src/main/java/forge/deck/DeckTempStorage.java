package forge.deck;

import forge.Card;
import java.util.List;

/**
 * Temporary storage for decks across various duels in a match, used to save
 * the temporary state of main/sideboard across several consecutive duels.
 * 
 * Currently only used for the human's deck.
 * 
 * @author Agetian
 */
public class DeckTempStorage {
    
    private static List<Card> humanMain = null;
    private static List<Card> humanSideboard = null;

    public static List<Card> getHumanMain() {
        return humanMain;
    }

    public static List<Card> getHumanSideboard() {
        return humanSideboard;
    }

    public static void setHumanMain(List<Card> main) {
        humanMain = main;
    }

    public static void setHumanSideboard(List<Card> sideboard) {
        humanSideboard = sideboard;
    }
}
