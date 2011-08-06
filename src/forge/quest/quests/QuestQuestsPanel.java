package forge.quest.quests;

import forge.*;
import forge.error.ErrorViewer;
import forge.gui.GuiUtils;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.QuestAbstractPanel;
import forge.quest.QuestFrame;
import forge.quest.main.QuestMainPanel;

import javax.swing.*;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

public class QuestQuestsPanel extends QuestAbstractPanel{
	
	private static final long serialVersionUID = 2409591658245091210L;

	private JLabel            titleLabel       = new JLabel();
	    
	private JButton           startQuestButton = new JButton();
    private JButton			  quitButton 	   = new JButton();
    
    private ButtonGroup       buttonGroup      = new ButtonGroup();
    
    private QuestData 		  questData;
    
    private Deck 			  hDeck;

    private QuestQuestsReader read;
    
    public QuestQuestsPanel(QuestFrame mainFrame) {
        super(mainFrame);
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }

        setup();
    }
    
    //only do this ONCE:
    private void setup() {
    	questData = AllZone.QuestData;
    	startQuestButton.setBounds(new Rectangle(10, 650, 120, 50));
    	startQuestButton.setText("Start Quest");
    	//buyPlantButton.setIcon(icon);
    	startQuestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
					startQuestButton_actionPerformed(e);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
        
    	read = new QuestQuestsReader(ForgeProps.getFile(NewConstants.QUEST.QUESTS), questData);
    	read.run();
    	
    	ArrayList<Quest_Assignment> questsToDisplay = new ArrayList<Quest_Assignment>();
    	
    	if (questData.getAvailableQuests()!= null && questData.getAvailableQuests().size() > 0)
    	{
    		ArrayList<Quest_Assignment> availableQuests = read.getQuestsByIds(questData.getAvailableQuests());
        	questsToDisplay = availableQuests;
    		
    	}
    	else
        {
    		ArrayList<Quest_Assignment> allAvailableQuests = read.getQuests();
    		
    		ArrayList<Integer> availableInts = new ArrayList<Integer>();
    		
    		int maxQuests = questData.getWin() / 10;
    		if (maxQuests > 7) maxQuests = 7;
    		if (allAvailableQuests.size() < maxQuests) maxQuests = allAvailableQuests.size();
    		
    		Collections.shuffle(allAvailableQuests);
    		
    		for (int i=0;i<maxQuests;i++)
    		{
    			Quest_Assignment qa = allAvailableQuests.get(i);
    			
    			availableInts.add(qa.getId());

    			questsToDisplay.add(qa);
    		}
    		questData.setAvailableQuests(availableInts);
    		QuestData.saveData(questData);
    	}//else
    	
    	JRadioButton radio;
    	JLabel description;
    	JLabel difficulty;
    	JLabel repeatable;
    	JLabel reward;
    	//JLabel iconLabel;
    	
    	int y = 75;
    	//display the choices:
    	for (Quest_Assignment qa : questsToDisplay)
    	{
    		radio = new JRadioButton();
    		radio.setText(qa.getName());
            radio.setBounds(new Rectangle(15, y, 250, 25));
            radio.setName(""+qa.getId());
            
            description = new JLabel();
            description.setFont(new Font("sserif", 0, 12));
            description.setText(qa.getDesc());
            description.setBounds(new Rectangle(15, y+20, 800, 25));
            
            difficulty = new JLabel();
            difficulty.setFont(new Font("sserif", 0, 12));
            difficulty.setText(qa.getDifficulty());
            difficulty.setBounds(new Rectangle(15, y+40, 80, 25));
            
            StringBuilder sb = new StringBuilder();
            sb.append("<html>This quest is <b>");
            if (qa.isRepeatable())
            	sb.append("repeatable");
            else
            	sb.append("not repeatable");
            sb.append("</b></html>");
            
            repeatable = new JLabel();
            repeatable.setFont(new Font("sserif", 0, 12));
            repeatable.setText(sb.toString());
            repeatable.setBounds(new Rectangle(115, y+40, 200, 25));
            
            sb = new StringBuilder();
            sb.append("<html><u>Reward</u>: <b>");
            sb.append(qa.getCardReward());
            sb.append(", ");
            sb.append(qa.getCreditsReward());
            sb.append(" credits</b></html>");
            
            reward = new JLabel();
            reward.setFont(new Font("sserif", 0, 12));
            reward.setText(sb.toString());
            reward.setBounds(new Rectangle(320, y+40, 500, 25));
            
            /*
            ImageIcon icon = getIcon(qa.getIconName());
            iconLabel = new JLabel();
            iconLabel.setIcon(icon);
            iconLabel.setBounds(new Rectangle(900, y-35, 128, 128));
            */
            
            buttonGroup.add(radio);
            
            this.add(radio);
            this.add(description);
            this.add(difficulty);
            this.add(repeatable);
            this.add(reward);
            
            y+=80;
    	}//for
    	
    	
    }//setup();
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Quests");
        titleLabel.setBounds(new Rectangle(400, 5, 300, 60));
        ImageIcon icon = GuiUtils.getIconFromFile("MapIcon.png");
        titleLabel.setIcon(icon);
        this.setLayout(null);
        
        //String fileName = "LeafIconSmall.png";
    	//ImageIcon icon = getIcon(fileName);
    	
    	startQuestButton.setEnabled(true);
       
        quitButton.setBounds(new Rectangle(140, 650, 120, 50));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mainFrame.showMainPane();
            }
        });


        //jPanel2.add(quitButton, null);
        this.add(startQuestButton, null);
        this.add(titleLabel, null);
        this.add(quitButton,null);
    }
    
    void startQuestButton_actionPerformed(ActionEvent e) throws Exception {
    	
    	Quest_Assignment selectedQuest = new Quest_Assignment();
    	
    	ButtonModel bm = buttonGroup.getSelection();
    	if (bm == null)
    		System.out.println("no button is selected!");
    	else
    	{
    		for (Enumeration<AbstractButton> en=buttonGroup.getElements(); en.hasMoreElements(); )
    		{
    			JRadioButton b = (JRadioButton)en.nextElement();
    			if (b.getModel() == buttonGroup.getSelection()) 
    			{ 
    				//System.out.println(b.getName());
    				selectedQuest = read.getQuestById(Integer.parseInt(b.getName()));
    			} 
    		}
    	}
    	
    	//DeckIO deckIO = new NewDeckIO(ForgeProps.getFile(QUEST.DECKS));
    	//Deck computerDeck = deckIO.readDeck("quest"+selectedQuest.getId());
    	Deck computerDeck = questData.ai_getDeckNewFormat("quest"+selectedQuest.getId());
    	
    	//System.out.println(computerDeck.getName());
    	
    	Constant.Runtime.HumanDeck[0] = hDeck;
        Constant.Runtime.ComputerDeck[0] = computerDeck;
        
        //Constant.Quest.qa[0] = selectedQuest;
    	AllZone.QuestAssignment = selectedQuest;
    	
    	Constant.Quest.oppIconName[0] = selectedQuest.getIconName();
    	
    	int extraLife = 0;
    	if (questData.getGearLevel() == 2)
    		extraLife = 3;
    	
	    if(QuestMainPanel.newGUICheckbox.isSelected()) AllZone.Display = new GuiDisplay4();
        else AllZone.Display = new GuiDisplay3();
    	
	    AllZone.GameAction.newGame(hDeck, computerDeck, QuestUtil.getHumanPlantAndPet(questData, selectedQuest), new CardList(), questData.getLife()+extraLife, 
	    						   selectedQuest.getComputerLife(), selectedQuest);
	    
	    AllZone.Display.setVisible(true);
        mainFrame.dispose();
    }

    @Override
    public void refreshState() {
    }

    public void setDeck(Deck deck) {
        this.hDeck = deck;
    }
}

