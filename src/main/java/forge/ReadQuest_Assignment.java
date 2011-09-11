package forge;

import forge.error.ErrorViewer;
import forge.properties.NewConstants;
import forge.quest.data.QuestUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>ReadQuest_Assignment class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ReadQuest_Assignment implements Runnable, NewConstants {
    
    ArrayList<Quest_Assignment> allQuests   = new ArrayList<Quest_Assignment>();
    private ArrayList<Integer> ids          = new ArrayList<Integer>();

    private BufferedReader  br;
    private int             totalWins;
    private List<Integer>   completedQuests = new ArrayList<Integer>();
    
    // Constants, tied to properties in the quests.txt file.
    private static final String ID          = "id";
    private static final String ICON        = "Icon";
    private static final String TITLE       = "Title";
    private static final String DESC        = "Desc";
    private static final String DIFF        = "Diff";
    private static final String AILIFE      = "AILife";
    private static final String REPEAT      = "Repeat";
    private static final String WINS        = "Wins";
    private static final String CARDS       = "Card Reward";
    private static final String CREDITS     = "Credit Reward";
    private static final String HUMANEXTRAS = "HumanExtras";
    private static final String AIEXTRAS    = "AIExtras";

    /**
     * <p>Constructor for ReadQuest_Assignment.</p>
     * Sets parameters for available quests and prepares buffered reader for quests.txt.
     *
     * @param filename a {@link java.lang.String} object.
     * @param questData a {@link forge.quest.data.QuestData} object.
     */
    public ReadQuest_Assignment(String filename, forge.quest.data.QuestData questData) {
        this(new File(filename), questData);
    }

    /**
     * <p>Constructor for ReadQuest_Assignment.</p>
     * Sets parameters for available quests and prepares buffered reader for quests.txt.
     *
     * @param file a {@link java.io.File} object.
     * @param questData a {@link forge.quest.data.QuestData} object.
     */
    public ReadQuest_Assignment(File file, forge.quest.data.QuestData questData) {
        if (questData != null) {
            totalWins = questData.getWin();
            if (questData.getCompletedQuests() != null) {
                completedQuests = questData.getCompletedQuests();
            }
            else {
                completedQuests = new ArrayList<Integer>();
            }
        }

        try {
            br = new BufferedReader(new FileReader(file));
        } catch (Exception ex) {
            ErrorViewer.showError(ex, "File \"%s\" not found", file.getAbsolutePath());
            throw new RuntimeException("ReadQuest_Assignment > constructor error: "+
                    "BufferedReader failed, '"+file.getAbsolutePath()+"' not found.");
        }
    } // ReadQuest_Assignment()
    
    /**
     * <p>getQuests.</p>
     * Returns list of currently available quest objects.
     *
     * @return a {@link java.util.List} object.
     */
    public List<Quest_Assignment> getQuests() {
        ArrayList<Quest_Assignment> availableQuests = new ArrayList<Quest_Assignment>();
        
        for(Quest_Assignment qa : allQuests) {
            if (qa.getRequiredNumberWins() <= totalWins && !completedQuests.contains(qa.getId())) {
                availableQuests.add(qa);
            }
        }
        
        return availableQuests;
    }
    
    /**
     * <p>getQuests.</p>
     * Returns complete list of all quest objects.
     *
     * @return a {@link java.util.List} object.
     */
    public List<Quest_Assignment> getAllQuests() {
        return allQuests;
    }

    /**
     * <p>getQuestsByIds.</p>
     *
     * @param availableQuestIds a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public List<Quest_Assignment> getQuestsByIds(List<Integer> availableQuestIds) {
        List<Quest_Assignment> q = new ArrayList<Quest_Assignment>();
        
        for (Quest_Assignment qa : allQuests) {
            if (availableQuestIds.contains(qa.getId())) {
                q.add(qa);
            }
        }

        return q;
    }

    /**
     * <p>getQuestById.</p>
     *
     * @param i a int.
     * @return a {@link forge.Quest_Assignment} object.
     */
    public Quest_Assignment getQuestById(int id) {
        // Error handling for OOB ID?
        return allQuests.get(id);
    }

    /**
     * <p>run.</p>
     * Assembles Quest_Assignment instances into allQuests.
     */
    public void run() {
        Quest_Assignment        qa = null;
        String                  line;
        int                     i;
        String[]                linedata;
        
        try {
            while ((line = br.readLine()) != null) {
                if(line.equals("[quest]")) {
                    qa = new Quest_Assignment();
                    allQuests.add(qa);
                }
                else if(!line.equals("") && qa != null) { 
                    linedata = line.split("=", 2);
                    linedata[1] = linedata[1].trim();
                    
                    // If empty data, ignore the line (assignment will use default).
                    if(linedata[1].equals("")) {
                        continue;
                    }
                    
                    // Data OK.
                    if(linedata[0].equals(ID)) {
                        i = Integer.parseInt(linedata[1]);
                        
                        // Duplicate ID check
                        if(ids.contains(i)) {
                            throw new RuntimeException("ReadQuest_Assignment > run() error: duplicate quest ID ("+i+")");  
                        }
                        // Non-sequential ID check
                        else if(i != allQuests.size()) {
                            throw new RuntimeException("ReadQuest_Assignment > run() error: non-sequential quest ID ("+i+")");
                        }
                        // ID OK.
                        else {
                            ids.add(i);
                            qa.setId(i);
                        }
                    } 
                    else if(linedata[0].equals(ICON)) {
                        qa.setIconName(linedata[1]);
                    } 
                    else if(linedata[0].equals(TITLE)) {
                        qa.setName(linedata[1]);
                    } 
                    else if(linedata[0].equals(DESC)) {
                        qa.setDesc(linedata[1]);
                    } 
                    else if(linedata[0].equals(DIFF)) {
                        qa.setDifficulty(linedata[1]);
                    } 
                    else if(linedata[0].equals(REPEAT)) {
                        qa.setRepeatable(Boolean.parseBoolean(linedata[1]));
                    } 
                    else if(linedata[0].equals(AILIFE)) {
                        qa.setComputerLife(Integer.parseInt(linedata[1]));
                    } 
                    else if(linedata[0].equals(WINS)) {
                        qa.setRequiredNumberWins(Integer.parseInt(linedata[1]));
                    } 
                    else if(linedata[0].equals(CREDITS)) {
                        qa.setCreditsReward(Integer.parseInt(linedata[1]));
                    }
                    // Card reward list assembled here.
                    else if(linedata[0].equals(CARDS)) {
                        qa.setCardReward(linedata[1]);
                        qa.setCardRewardList(QuestUtil.generateCardRewardList(linedata[1]));
                    } 
                    // Human extra card list assembled here.
                    else if(linedata[0].equals(HUMANEXTRAS)) {
                        String[] names = linedata[1].split("\\|");
                        CardList templist = new CardList();
                        Card tempcard;
                        
                        for(String s : names) { 
                            // Token card creation
                            if(s.substring(0,5).equals("TOKEN")) {
                                tempcard = QuestUtil.createToken(s);
                                tempcard.addController(AllZone.getHumanPlayer());
                                tempcard.setOwner(AllZone.getHumanPlayer());
                                templist.add(tempcard);
                            }
                            // Standard card creation
                            else {
                                tempcard = AllZone.getCardFactory().getCard(s, AllZone.getHumanPlayer());
                                tempcard.setCurSetCode(tempcard.getMostRecentSet());
                                tempcard.setImageFilename(CardUtil.buildFilename(tempcard));
                                templist.add(tempcard); 
                            }
                        }
                        
                        qa.setHumanExtraCards(templist);
                    }
                    // AI extra card list assembled here.
                    else if(linedata[0].equals(AIEXTRAS)) {
                        String[] names = linedata[1].split("\\|");
                        CardList templist = new CardList();
                        Card tempcard;
                        
                        for(String s : names) { 
                            // Token card creation
                            if(s.substring(0,5).equals("TOKEN")) {
                                tempcard = QuestUtil.createToken(s);
                                tempcard.addController(AllZone.getComputerPlayer());
                                tempcard.setOwner(AllZone.getComputerPlayer());
                                templist.add(tempcard);
                            }
                            // Standard card creation
                            else {
                                tempcard = AllZone.getCardFactory().getCard(s, AllZone.getComputerPlayer());
                                tempcard.setCurSetCode(tempcard.getMostRecentSet());
                                tempcard.setImageFilename(CardUtil.buildFilename(tempcard));
                                templist.add(tempcard); 
                            }
                        }
                        
                        qa.setAIExtraCards(templist);
                    }
                } // else if()
            } // while()

            br.close();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        
        // Confirm that all quests have IDs.
        for(Quest_Assignment q : allQuests) {
            if(q.getId()==-1) {
                throw new RuntimeException("ReadQuest_Assignment > getQuests() error: "+
                        "Quest ID missing for '"+q.getName()+"'.");
            }
        }
        
    }   // run()
}
