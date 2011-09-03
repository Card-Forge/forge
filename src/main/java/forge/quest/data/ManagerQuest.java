package forge.quest.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.AllZone;
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


/**
 * <p>ManagerQuest</p>
 * MODEL - Provides static methods to work with quest-related tasks.
 *
 */

// This could be combined with BattleManager and moved into QuestUtil?
public class ManagerQuest {
    /**
     * <p>getQuests</p>
     * 
     * Returns list of DeckSingleQuest objects storing data 
     * of the quests currently available.
     *
     * @return a {@link java.util.List} object.
     */

    public static List<DeckSingleQuest> getQuests() {
        QuestData questData = AllZone.getQuestData();
        
        List<Integer> idsCompleted = questData.getCompletedQuests();
        List<Integer> idsAvailable = new ArrayList<Integer>();
        List<DeckSingleQuest> allQuests = new ArrayList<DeckSingleQuest>();
        List<DeckSingleQuest> questsAvailable = new ArrayList<DeckSingleQuest>();
        
        // Generate DeckSingleQuest objects for available quest IDs.
        // If there are quests IDs available, use them.
        if (questData.getAvailableQuests() != null && questData.getAvailableQuests().size() > 0) {
            idsAvailable = questData.getAvailableQuests();
            for (int id : idsAvailable) {
                questsAvailable.add(new DeckSingleQuest(getAIDeckFromFile("quest"+id)));
            } 
        } 
        // Otherwise, re-generate list.
        // To do this, each quest file must be opened and metadata examined.
        // DeckSingleQuest objects are grabbed directly from opened list. 
        else {            
            File[] files = ForgeProps.getFile(NewConstants.QUEST.DECKS).listFiles();
            
            for(File f : files) {
                if(!f.isDirectory() && f.getName().substring(0,5).equals("quest")) {
                    DeckSingleQuest temp = new DeckSingleQuest(getAIDeckFromFile(
                            f.getName().substring(0, f.getName().lastIndexOf('.'))));
                    
                    idsAvailable.add(temp.getID());
                    if (temp.getNumberWinsRequired() <= questData.getWin() && 
                            !idsCompleted.contains(temp.getID())) {
                        allQuests.add(temp);
                    }
                }
            }
            
            // Limit available IDs.
            int maxQuests = questData.getWin() / 10;
            if (maxQuests > 5) {
                maxQuests = 5;
            }
            if (idsAvailable.size() < maxQuests) {
                maxQuests = idsAvailable.size();
            }

            Collections.shuffle(idsAvailable);
            idsAvailable = idsAvailable.subList(0,maxQuests);
            
            questData.setAvailableQuests(idsAvailable); 
            questData.saveData(); 
            
            for (int id : idsAvailable) {
                questsAvailable.add(allQuests.get(id));
            } 
        }
        
        return questsAvailable;
    }       

    
    /**
     * <p>getDeckFromFile.</p>
     * Returns a deck object built from a file name.
     * Req'd because NewConstants.QUEST.DECKS must be used.
     *
     * @param deckName a {@link java.lang.String} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public static Deck getAIDeckFromFile(String deckName) {
        final File file = ForgeProps.getFile(NewConstants.QUEST.DECKS);
        final DeckManager manager = new DeckManager(file);
        return manager.getDeck(deckName);
    } 
}
