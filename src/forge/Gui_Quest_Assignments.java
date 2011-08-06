package forge;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

public class Gui_Quest_Assignments extends JFrame implements NewConstants{
	
	private static final long serialVersionUID = 2409591658245091210L;
		
	private JFrame 			  questGui;
	private JLabel            titleLabel       = new JLabel();
	    
	private JButton           startQuestButton = new JButton();
    private JButton			  quitButton 	   = new JButton();
    
    private ButtonGroup       buttonGroup      = new ButtonGroup();
    
    private QuestData 		  questData;
    
    private Deck 			  hDeck;
    
    private ReadQuest_Assignment read;
    
    public Gui_Quest_Assignments(JFrame parent, Deck humanDeck) {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        
        questGui = parent;
        hDeck = humanDeck; 
        
        setup();

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(WindowEvent e)
			{
				Gui_Quest_Assignments.this.this_windowClosing(e);
			}
		});
        
        setSize(1024, 768);
        this.setResizable(false);
        Dimension screen = getToolkit().getScreenSize();
        Rectangle bounds = getBounds();
        bounds.width = 1024;
        bounds.height = 768;
        bounds.x = (screen.width - bounds.width) / 2;
        bounds.y = (screen.height - bounds.height) / 2;
        setBounds(bounds);
        
        
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
        
    	read = new ReadQuest_Assignment(ForgeProps.getFile(QUEST.QUESTS), questData);
    	read.run();
    	
    	ArrayList<Quest_Assignment> questsToDisplay = new ArrayList<Quest_Assignment>();
    	
    	if (questData.getAvailableQuests()!= null && questData.getAvailableQuests().size() > 0)
    	{
    		ArrayList<Quest_Assignment> availableQuests = read.getQuestsByIds(questData.getAvailableQuests());
        	questsToDisplay = availableQuests;
    		
    		/*
    		for (Quest_Assignment qa : availableQuests)
    		{
    			System.out.println(qa.getId() + " : " + qa.getName());
    		}*/
    		
    		//System.out.println("Not null");
    	}
    	else //generate some random quests
    	{
    		//System.out.println("null");
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
            
            this.getContentPane().add(radio);
            this.getContentPane().add(description);
            this.getContentPane().add(difficulty);
            this.getContentPane().add(repeatable);
            this.getContentPane().add(reward);
            //this.getContentPane().add(iconLabel);
            
            y+=80;
    	}//for
    	
    	
    }//setup();
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new java.awt.Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Quests");
        titleLabel.setBounds(new Rectangle(400, 5, 300, 60));
        ImageIcon icon = getIcon("MapIcon.png");
        titleLabel.setIcon(icon);
        this.getContentPane().setLayout(null);
        
        //String fileName = "LeafIconSmall.png";
    	//ImageIcon icon = getIcon(fileName);
    	
    	startQuestButton.setEnabled(true);
       
        quitButton.setBounds(new Rectangle(140, 650, 120, 50));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quitButton_actionPerformed(e);
            }
        });


        //jPanel2.add(quitButton, null);
        this.getContentPane().add(startQuestButton, null);
        this.getContentPane().add(titleLabel, null);
        this.getContentPane().add(quitButton,null);
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
    	
	    if(Gui_Quest.newGUICheckbox.isSelected()) AllZone.Display = new GuiDisplay4();
        else AllZone.Display = new GuiDisplay3();
    	
	    AllZone.GameAction.newGame(hDeck, computerDeck, QuestUtil.getHumanPlantAndPet(questData, selectedQuest), new CardList(), questData.getLife()+extraLife, 
	    						   selectedQuest.getComputerLife(), selectedQuest);
	    
	    AllZone.Display.setVisible(true);
        dispose();
    }
    
    private ImageIcon getIcon(String fileName)
    {
    	File base = ForgeProps.getFile(IMAGE_ICON);
    	File file = new File(base, fileName);
    	ImageIcon icon = new ImageIcon(file.toString());
    	return icon;
    }
    
    void quitButton_actionPerformed(ActionEvent e) {
    	//QuestData.saveData(questData);
        //new Gui_Shops();
    	questGui.setVisible(true);
    	
        dispose();
    }
    
    void this_windowClosing(WindowEvent e) {
		questGui.setVisible(true);
    }
    
}

