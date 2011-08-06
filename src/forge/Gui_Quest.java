package forge;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


//presumes AllZone.QuestData is not null
//AllZone.QuestData should be set by Gui_QuestOptions
public class Gui_Quest extends JFrame implements NewConstants{
    private static final long serialVersionUID   = -6432089669283627896L;
    
    private QuestData         questData;
    
    private JLabel            jLabel1            = new JLabel();
    private JLabel            difficultyLabel    = new JLabel();
    private JLabel            winLostLabel       = new JLabel();
    private JLabel            rankLabel          = new JLabel();
    private JLabel            creditsLabel       = new JLabel();
    private JLabel            lifeLabel          = new JLabel();
    @SuppressWarnings("unused")
    // border1
    private Border            border1;
    private TitledBorder      titledBorder1;
    private TitledBorder      titledBorder2;
    private JButton           infoButton         = new JButton();
    private JButton 		  otherShopsButton   = new JButton();
    private JButton 		  cardShopButton	 = new JButton();
    private JButton           deckEditorButton   = new JButton();
    private JPanel            jPanel2            = new JPanel();
    private JPanel            jPanel3            = new JPanel();
    private JButton           playGameButton     = new JButton();
    private JButton           questsButton       = new JButton();

    private JRadioButton      oppOneRadio        = new JRadioButton();
    private JRadioButton      oppTwoRadio        = new JRadioButton();
    private JRadioButton      oppThreeRadio      = new JRadioButton();
    private JLabel            jLabel5            = new JLabel();
    private JComboBox         deckComboBox       = new JComboBox();
    private JComboBox         petComboBox        = new JComboBox();
    private ButtonGroup       oppGroup           = new ButtonGroup();
    private static JCheckBox  smoothLandCheckBox = new JCheckBox("", false);
    public static JCheckBox   newGUICheckbox	 = new JCheckBox("", true);
    private static JCheckBox  devModeCheckBox 	 = new JCheckBox("", true);
    
    public static void main(String[] args) {
        new Gui_Quest();
    }
    
    public Gui_Quest() {
    	questData = AllZone.QuestData;
    	
    	try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        setup();
        
        setVisible(true);
    }
    
    private void setup() {
        //center window on the screen
        Dimension screen = this.getToolkit().getScreenSize();
        setBounds(screen.width / 4, 50, //position
                500, 615); //size
        
        //if user closes this window, go back to "Quest Options" screen
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                Gui_Quest.this.dispose();
                new Gui_QuestOptions();
            }
        });
        
        //set labels
        difficultyLabel.setText(questData.getDifficulty() + " - " + questData.getMode());
        rankLabel.setText(questData.getRank());
        creditsLabel.setText("Credits: " + questData.getCredits());
        
        if (questData.getMode().equals("Fantasy"))
        {
	        int life = questData.getLife();
	        if (life<15)
	        	questData.setLife(15);
	        lifeLabel.setText("Max Life: "+questData.getLife());
        }
        
        String s = questData.getWin() + " wins / " + questData.getLost() + " losses";
        winLostLabel.setText(s);
        
        String[] op = questData.getOpponents();
        oppOneRadio.setText(op[0]);
        oppOneRadio.setToolTipText(Gui_Quest_Deck_Info.getDescription(op[0]));
        oppTwoRadio.setText(op[1]);
        oppTwoRadio.setToolTipText(Gui_Quest_Deck_Info.getDescription(op[1]));
        oppThreeRadio.setText(op[2]);
        oppThreeRadio.setToolTipText(Gui_Quest_Deck_Info.getDescription(op[2]));
        

        //get deck names as Strings
        ArrayList<String> list = questData.getDeckNames();
        Collections.sort(list);
        for(int i = 0; i < list.size(); i++)
            deckComboBox.addItem(list.get(i));
        
        if(Constant.Runtime.HumanDeck[0] != null) deckComboBox.setSelectedItem(Constant.Runtime.HumanDeck[0].getName());
        
        if ("Fantasy".equals(questData.getMode()))
        {
        	
        }
        
        
    }//setup()
    
    private void jbInit() throws Exception {
        border1 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Choices");
        titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
        		"Settings");
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 25));
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText("Quest Mode");
        jLabel1.setBounds(new Rectangle(1, 7, 499, 45));
        this.setResizable(false);
        this.setTitle("Quest Mode");
        this.getContentPane().setLayout(null);
        difficultyLabel.setText("Medium");
        difficultyLabel.setBounds(new Rectangle(1, 52, 499, 41));
        difficultyLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        difficultyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        winLostLabel.setText("23 wins / 10 losses");
        winLostLabel.setBounds(new Rectangle(1, 130, 499, 43));
        winLostLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        winLostLabel.setHorizontalAlignment(SwingConstants.CENTER);
        winLostLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        rankLabel.setText("Card Flopper");
        rankLabel.setBounds(new Rectangle(1, 93, 499, 37));
        rankLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        rankLabel.setHorizontalAlignment(SwingConstants.CENTER);
        creditsLabel.setBounds(new Rectangle(1, 175, 499, 15));
        //creditsLabel.setText("Credits: 1000");
        creditsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        creditsLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        
        
        infoButton.setBounds(new Rectangle(338, 235, 142, 28));
        infoButton.setFont(new java.awt.Font("Dialog", 0, 13));
        infoButton.setText("Opponent Notes");
        infoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                infoButton_actionPerformed(e);
            }
        });
        
        //if (questData.getMode().equals("Fantasy"))
        if ("Fantasy".equals(questData.getMode()))
        {
        	refreshPets();
        	
            lifeLabel.setBounds(new Rectangle(1, 195, 499, 15));
            lifeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            lifeLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            
        	otherShopsButton.setBounds(new Rectangle(338, 290, 142, 38));
        	otherShopsButton.setFont(new java.awt.Font("Dialog", 0, 16));
        	otherShopsButton.setText("Bazaar");
        	otherShopsButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    otherShopsButton_actionPerformed(e);
                }
            });
        	
        	questsButton.setBounds(new Rectangle(338, 497, 142, 37));
        	questsButton.setFont(new java.awt.Font("Dialog", 0, 18));
        	questsButton.setText("Quests");
        	questsButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    questsButton_actionPerformed(e);
                }
            });
        	
        }
        
        cardShopButton.setBounds(new Rectangle(338, 334, 142, 38));
        cardShopButton.setFont(new java.awt.Font("Dialog", 0, 16));
        cardShopButton.setText("Card Shop");
        cardShopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardShopButton_actionPerformed(e);
            }
        });
        
        deckEditorButton.setBounds(new Rectangle(338, 378, 142, 38));
        deckEditorButton.setFont(new java.awt.Font("Dialog", 0, 16));
        deckEditorButton.setText("Deck Editor");
        deckEditorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deckEditorButton_actionPerformed(e);
            }
        });
        jPanel2.setBorder(titledBorder1);
        jPanel2.setBounds(new Rectangle(20, 223, 300, 198));
        jPanel2.setLayout(null);
        jPanel3.setBorder(titledBorder2);
        jPanel3.setBounds(new Rectangle(20, 433, 300, 142));
        jPanel3.setLayout(null);
        playGameButton.setBounds(new Rectangle(338, 538, 142, 37));
        playGameButton.setFont(new java.awt.Font("Dialog", 0, 18));
        playGameButton.setText("Play Game");
        playGameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playGameButton_actionPerformed(e);
            }
        });
        
        
        oppTwoRadio.setText("Bob");
        oppTwoRadio.setBounds(new Rectangle(20, 65, 250, 35));
        oppOneRadio.setSelected(true);
        oppOneRadio.setText("Sam");
        oppOneRadio.setBounds(new Rectangle(20, 30, 250, 35));
        oppThreeRadio.setText("Generated Deck");
        oppThreeRadio.setBounds(new Rectangle(20, 100, 250, 35));
        jLabel5.setText("Your Deck:");
        jLabel5.setBounds(new Rectangle(20, 151, 125, 29));
        deckComboBox.setBounds(new Rectangle(98, 152, 185, 29));
        petComboBox.setBounds(new Rectangle(338, 437, 142, 38));
        smoothLandCheckBox.setText("Stack AI land");
        smoothLandCheckBox.setBounds(new Rectangle(65, 62, 153, 21));
        //smoothLandCheckBox.setSelected(true);
        newGUICheckbox.setText("New GUI");
        newGUICheckbox.setBounds(new Rectangle(65, 28, 165, 24));
        devModeCheckBox.setText("Developer Mode");
        devModeCheckBox.setBounds(new Rectangle(65, 94, 190, 25));
        devModeCheckBox.setSelected(Constant.Runtime.DevMode[0]);
        
        //resizeCheckbox.setSelected(true);
        this.getContentPane().add(rankLabel, null);
        this.getContentPane().add(jLabel1, null);
        this.getContentPane().add(difficultyLabel, null);
        this.getContentPane().add(winLostLabel, null);
        this.getContentPane().add(creditsLabel,null);
        jPanel2.add(jLabel5, null);
        this.getContentPane().add(infoButton, null);
        jPanel2.add(deckComboBox, null);
        //jPanel2.add(petComboBox, null);
        jPanel2.add(oppOneRadio, null);
        jPanel2.add(oppTwoRadio, null);
        jPanel2.add(oppThreeRadio, null);
        if ("Fantasy".equals(questData.getMode())) {
        	this.getContentPane().add(otherShopsButton, null);
        	this.getContentPane().add(lifeLabel,null);
        	this.getContentPane().add(questsButton, null);
        	this.getContentPane().add(petComboBox, null);
        	
        	int questsPlayed = questData.getQuestsPlayed();
        	int div = 6;
        	if (questData.getGearLevel() == 1)
        		div = 5;
        	else if (questData.getGearLevel() == 2)
        		div = 4;
        		
        	//System.out.println("questsPlayed: " + questsPlayed);
        	if (questData.getWin() / div < questsPlayed || questData.getWin() < 25)
        		questsButton.setEnabled(false);
        	else
        		questsButton.setEnabled(true);
        }
        this.getContentPane().add(cardShopButton, null);
        this.getContentPane().add(deckEditorButton, null);
        this.getContentPane().add(playGameButton, null);
        jPanel3.add(smoothLandCheckBox, null);
        jPanel3.add(newGUICheckbox, null);
        jPanel3.add(devModeCheckBox, null);
        this.getContentPane().add(jPanel2, null);
        this.getContentPane().add(jPanel3, null);
        oppGroup.add(oppOneRadio);
        oppGroup.add(oppTwoRadio);
        oppGroup.add(oppThreeRadio);
    }
    
    void refreshCredits()
    {
    	creditsLabel.setText("Credits: " + questData.getCredits());
    }
    
    void refreshLife()
    {
    	lifeLabel.setText("Max Life: " + questData.getLife());
    }
    
    void refreshPets(){
    	petComboBox.removeAllItems();
    	ArrayList<String> petList = QuestUtil.getPetNames(questData);
    	for (int i=0;i<petList.size();i++)
    		petComboBox.addItem(petList.get(i));
    	
    	petComboBox.addItem("None");
    	petComboBox.addItem("No Plant/Pet");
    }
    
    //make sure credits/life get updated after shopping at bazaar
    public void setVisible(boolean b)
    {
    	refreshPets();
    	refreshCredits();
    	refreshLife();
    	super.setVisible(b);
    }
    
    void infoButton_actionPerformed(ActionEvent e)
    {
        Gui_Quest_Deck_Info.showDeckList();
    }
    
    void deckEditorButton_actionPerformed(ActionEvent e) {
        Command exit = new Command() {
            private static final long serialVersionUID = -5110231879441074581L;
            
            public void execute() {
                //saves all deck data
                QuestData.saveData(AllZone.QuestData);
                
                new Gui_Quest();
            }
        };
        
        Gui_Quest_DeckEditor g = new Gui_Quest_DeckEditor();
        
        g.show(exit);
        g.setVisible(true);
        
        this.dispose();
    }//deck editor button
    
    void otherShopsButton_actionPerformed(ActionEvent e)
    {
        Gui_Shops g = new Gui_Shops(this);
        g.setVisible(true);
        
        this.dispose();
    }
    
    void cardShopButton_actionPerformed(ActionEvent e) {
        Command exit = new Command() {
			private static final long serialVersionUID = 8567193482568076362L;

			public void execute() {
                //saves all deck data
                QuestData.saveData(AllZone.QuestData);
                
                new Gui_Quest();
            }
        };
        
        Gui_CardShop g = new Gui_CardShop(questData);
        
        g.show(exit);
        g.setVisible(true);
        
        this.dispose();
    }//card shop button
    
    void playGameButton_actionPerformed(ActionEvent e) {
        Object check = deckComboBox.getSelectedItem();
        if(check == null || getOpponent().equals("")) return;
        
        Deck human = questData.getDeck(check.toString());
        Deck computer = questData.ai_getDeckNewFormat(getOpponent());
        
        Constant.Runtime.HumanDeck[0] = human;
        Constant.Runtime.ComputerDeck[0] = computer;
        
        String oppIconName = getOpponent();
        oppIconName = oppIconName.substring(0, oppIconName.length()-1).trim() + ".jpg";
        //System.out.println(oppIconName);
        
        Constant.Quest.oppIconName[0] = oppIconName;
        
        //smoothLandCheckBox.isSelected() - for the AI
        
        // Dev Mode occurs before Display
        Constant.Runtime.DevMode[0] = devModeCheckBox.isSelected();
        
        //DO NOT CHANGE THIS ORDER, GuiDisplay needs to be created before cards are added
        if(newGUICheckbox.isSelected()) AllZone.Display = new GuiDisplay4();
        else AllZone.Display = new GuiDisplay3();
        
        Constant.Runtime.Smooth[0] = smoothLandCheckBox.isSelected();
        
        if (questData.getMode().equals("Realistic"))
        	AllZone.GameAction.newGame(human, computer);
        else
        {
        	Object pet = petComboBox.getSelectedItem();
        	if (pet != null)
        		questData.setSelectedPet(pet.toString());
        	
        	CardList hCl = QuestUtil.getHumanPlantAndPet(questData);
        	int hLife = QuestUtil.getLife(questData);
            AllZone.GameAction.newGame(human, computer, hCl, new CardList(), hLife, 20, null);
        }
        
       
        AllZone.Display.setVisible(true);
        //end - you can change stuff after this
        
        //close this window
        dispose();
        
    }//play game button
    
    void questsButton_actionPerformed(ActionEvent e)
    {
    	Object check = deckComboBox.getSelectedItem();
        if(check == null) return;
        
        Deck human = questData.getDeck(check.toString());
        
        Constant.Runtime.Smooth[0] = smoothLandCheckBox.isSelected();
        
        Constant.Runtime.DevMode[0] = devModeCheckBox.isSelected();
        
        Object pet = petComboBox.getSelectedItem();
    	if (pet != null)
    		questData.setSelectedPet(pet.toString());
    	
    	Gui_Quest_Assignments g = new Gui_Quest_Assignments(this, human);
        g.setVisible(true);
        
        this.dispose();
    }
    
    String getOpponent() {
        if(oppOneRadio.isSelected()) return oppOneRadio.getText();
        
        else if(oppTwoRadio.isSelected()) return oppTwoRadio.getText();
        
        else if(oppThreeRadio.isSelected()) return oppThreeRadio.getText();
        
        return "";
    }
}
