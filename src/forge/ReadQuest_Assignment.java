package forge;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


public class ReadQuest_Assignment implements Runnable, NewConstants {
    private BufferedReader  in;
    private ArrayList<Quest_Assignment> allQuests = new ArrayList<Quest_Assignment>();
    
    private int totalWins;
    private List<Integer> completedQuests = new ArrayList<Integer>();
    
    public static void main(String args[]) throws Exception {
        try {
            ReadQuest_Assignment read = new ReadQuest_Assignment(ForgeProps.getFile(QUEST.QUESTS), null);
            
            javax.swing.SwingUtilities.invokeAndWait(read);
            //    read.run();
            
            Quest_Assignment qa[] = new Quest_Assignment[read.allQuests.size()];
            read.allQuests.toArray(qa);
            for(int i = 0; i < qa.length; i++) {
                System.out.println(qa[i].getId());
                System.out.println(qa[i].getName());
                System.out.println(qa[i].getDesc());
                System.out.println(qa[i].getDifficulty());
                System.out.println(qa[i].isRepeatable());
                System.out.println(qa[i].getRequiredNumberWins());
            }
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            System.out.println("Error reading file " + ex);
        }
    }
    
    public List<Quest_Assignment> getQuests() {
        return new ArrayList<Quest_Assignment>(allQuests);
    }
    
    public List<Quest_Assignment> getQuestsByIds(List<Integer> availableQuestIds) {
    	List<Quest_Assignment> quests = new ArrayList<Quest_Assignment>();
    	
    	for(Quest_Assignment qa:allQuests)
    	{
    		if (availableQuestIds.contains(qa.getId()))
    			quests.add(qa);
    	}
    	
    	return quests;
    }
    
    public Quest_Assignment getQuestById(int i)
    {
    	for(Quest_Assignment qa:allQuests)
    	{
    		if (qa.getId() == i)
    			return qa;
    	}
    	return null;
    }
    
    /*
    public Quest_Assignment getQuestById(int id) {
    	return allQuests.get(id);
    }
    */
    
    public ReadQuest_Assignment(String filename, forge.quest.data.QuestData questData) {
        this(new File(filename), questData);
    }
    
    public ReadQuest_Assignment(File file, forge.quest.data.QuestData questData) {
        
    	if (questData != null) {
	    	totalWins = questData.getWin();
	    	if (questData.getCompletedQuests() != null)
	    		completedQuests = questData.getCompletedQuests();
	    	else
	    		completedQuests = new ArrayList<Integer>();
    	}
    	
    	if(!file.exists())
            throw new RuntimeException("ReadQuest_Assignment : constructor error -- file not found -- filename is "
                    + file.getAbsolutePath());
        
        //makes the checked exception, into an unchecked runtime exception
        try {
            in = new BufferedReader(new FileReader(file));
        } catch(Exception ex) {
            ErrorViewer.showError(ex, "File \"%s\" not found", file.getAbsolutePath());
            throw new RuntimeException("ReadQuest_Assignment : constructor error -- file not found -- filename is "
                    + file.getPath());
        }
    }//ReadCard()
    
    /* id
     * name
     * desc
     * difficulty
     * repeatable
     * numberWinsRequired
     * cardReward
     * creditsReward
     */
    
    public void run() {
        Quest_Assignment qa;
        String s = readLine();
        ArrayList<Integer> ids = new ArrayList<Integer>();
        
        while(!s.equals("End")) {
            qa = new Quest_Assignment();
            if(s.equals("")) throw new RuntimeException("ReadQuest_Assignment : run() reading error, id is blank");
            int id = Integer.parseInt(s);
            qa.setId(id);

            s = readLine();
            qa.setName(s);
            
            s = readLine();
            qa.setDesc(s);

            
            s = readLine();
            qa.setDifficulty(s);
            if (qa.getDifficulty().equals("Medium"))
        		qa.setComputerLife(25);
        	else if (qa.getDifficulty().equals("Hard"))
        		qa.setComputerLife(30);
        	else if (qa.getDifficulty().equals("Very Hard"))
        		qa.setComputerLife(35);
        	else if (qa.getDifficulty().equals("Expert"))
        		qa.setComputerLife(50);
        	else if (qa.getDifficulty().equals("Insane"))
        		qa.setComputerLife(100);

            s = readLine();
            if (s.equals("Repeatable"))
            	qa.setRepeatable(true);
            else
            	qa.setRepeatable(false);
            
            s = readLine();
            int wins = Integer.valueOf(s);
            qa.setRequiredNumberWins(wins);
            
            s = readLine();
            qa.setCardReward(s);
            
            s = readLine();
            long reward = Long.parseLong(s.trim());
            qa.setCreditsReward(reward);
            
            s = readLine();
            qa.setIconName(s);
            
            //s = readLine();
            s = readLine();
            
            if(ids.contains(qa.getId())) {
                System.out.println("ReadQuest_Assignment:run() error - duplicate card name: " + qa.getId());
                throw new RuntimeException("ReadQuest_Assignment:run() error - duplicate card name: " + qa.getId());
            }
            
            ids.add(qa.getId());
            if (qa.getRequiredNumberWins() <= totalWins && !completedQuests.contains(qa.getId()) ) {
            	forge.quest.data.QuestUtil.setupQuest(qa);
            	allQuests.add(qa);
            }
            
            //id:
            s = readLine();
        }
    }//run()
       
    private String readLine() {
        //makes the checked exception, into an unchecked runtime exception
        try {
            String s = in.readLine();
            if(s != null) s = s.trim();
            return s;
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ReadQuest_Assignment: readLine(Quest_Assignment) error");
        }
    }//readLine(Quest_Assignment)
}