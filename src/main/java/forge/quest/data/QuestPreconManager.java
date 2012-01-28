package forge.quest.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import forge.deck.DeckIO;
import forge.item.PreconDeck;

/** 
 * Very simple function - store all precons
 *
 */
public class QuestPreconManager {

    final List<PreconDeck> decks = new ArrayList<PreconDeck>();
    
    
    public QuestPreconManager(File deckDir) {
        final List<String> decksThatFailedToLoad = new ArrayList<String>();
        File[] files = deckDir.listFiles(DeckIO.DCK_FILE_FILTER);
        for (final File file : files) {
            try {
                decks.add(new PreconDeck(file));
            } catch (final NoSuchElementException ex) {
                final String message = String.format("%s failed to load because ---- %s", file.getName(), ex.getMessage());
                decksThatFailedToLoad.add(message);
            }
        }
        
        if (!decksThatFailedToLoad.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    StringUtils.join(decksThatFailedToLoad, System.getProperty("line.separator")),
                    "Some of your decks were not loaded.", JOptionPane.WARNING_MESSAGE);
        }
    }


    /**
     * TODO: Write javadoc for this method.
     * @param q
     * @return
     */
    public List<PreconDeck> getDecksForCurrent(QuestData q) {
        List<PreconDeck> meetRequirements = new ArrayList<PreconDeck>();
        for(PreconDeck deck : decks)
        {
            if( deck.getRecommendedDeals().meetsRequiremnts(q))
                meetRequirements.add(deck);
        }
        return meetRequirements;
    }


    public final List<PreconDeck> getDecks() {
        return decks;
    }

}
