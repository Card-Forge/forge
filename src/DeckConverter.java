/**
 * DeckConverter.java
 * 
 * Created on 08.11.2009
 */

import static java.lang.String.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import forge.DeckIO;
import forge.NewDeckIO;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


/**
 * The class DeckConverter. This class uses an {@link OldDeckIO} and a {@link NewDeckIO} to convert the old
 * all-decks2 file into the new folder structure.
 * 
 * @version V0.0 08.11.2009
 * @author Clemens Koza
 */
public class DeckConverter {
    public static void main(String[] args) {
        File oldDecks = ForgeProps.getFile(NewConstants.DECKS);
        File newDecks = ForgeProps.getFile(NewConstants.NEW_DECKS);
        
        if(oldDecks == null || !oldDecks.isFile()) return;
        
        int choice = JOptionPane.showConfirmDialog(null, format(
                "This dialog lets you migrate your old decks to the new version of forge.%n"
                        + "Your old decks file: %s%nYour new decks directory: %s%n%n"
                        + "If you don't want to see this dialog again, please remove/rename your %1$s file,%n"
                        + "or otherwise hide it from forge.%n%nDo you want to migrate your decks now?", oldDecks,
                newDecks), "Deck Migration", JOptionPane.YES_NO_OPTION);
        
        if(choice != JOptionPane.YES_OPTION) return;
        System.out.println("migrating deck file...");
        
        DeckIO odio = new OldDeckIO(oldDecks);
        List<forge.Deck> deckList = new ArrayList<forge.Deck>(Arrays.asList(odio.getDecks()));
        Map<String, forge.Deck[]> boosterMap = odio.getBoosterDecks();
        System.out.println("Decks found:");
        System.out.printf("\t%d normal decks%n", deckList.size());
        System.out.printf("\t%d booster decks%n", boosterMap.size());
        
        //the constructor loads the decks from NEW_DECKS and preserves those from the collections
        DeckIO ndio = new NewDeckIO(newDecks, deckList, boosterMap);
        
        System.out.println("Decks in NewDeckIO:");
        System.out.printf("\t%d normal decks%n", ndio.getDecks().length);
        System.out.printf("\t%d booster decks%n", ndio.getBoosterDecks().size());
        
        //stores all the decks (new and old) to the directory.
        ndio.close();
        
        JOptionPane.showMessageDialog(null, "Your deck file was successfully migrated!");
    }
    
    /**
     * 
     */
    public static Object toForgeDeck(Object o) {
        if(o instanceof forge.Deck) {
            //a single new-type deck
            return o;
        } else if(o instanceof forge.Deck[]) {
            //a new-type booster deck
            return o;
        } else if(o instanceof Deck) {
            // an old type deck
            Deck d = (Deck) o;
            return d.migrate();
        } else if(o instanceof Deck[]) {
            Deck[] d = (Deck[]) o;
            forge.Deck[] result = new forge.Deck[d.length];
            for(int i = 0; i < d.length; i++) {
                result[i] = d[i].migrate();
            }
            return result;
        } else throw new IllegalArgumentException();
    }
}
