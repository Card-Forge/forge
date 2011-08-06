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
    
    private JRadioButton      oppTwoRadio        = new JRadioButton();
    private JRadioButton      oppOneRadio        = new JRadioButton();
    private JRadioButton      oppThreeRadio      = new JRadioButton();
    private JLabel            jLabel5            = new JLabel();
    private JComboBox         deckComboBox       = new JComboBox();
    private JComboBox         petComboBox        = new JComboBox();
    private ButtonGroup       oppGroup           = new ButtonGroup();
    private static JCheckBox  smoothLandCheckBox = new JCheckBox("", false);
    private static JCheckBox  resizeCheckbox     = new JCheckBox("", true);
    private static JCheckBox  millLoseCheckBox 	 = new JCheckBox("", true);
    
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
        oppTwoRadio.setText(op[1]);
        oppThreeRadio.setText(op[2]);
        

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
        	//petComboBox.removeAll();
        	ArrayList<String> petList = QuestUtil.getPetNames(questData);
        	for (int i=0;i<petList.size();i++)
        		petComboBox.addItem(petList.get(i));
        	
        	petComboBox.addItem("None");
        	petComboBox.addItem("No Plant/Pet");
        	
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
        resizeCheckbox.setText("Resizable Game Area");
        resizeCheckbox.setBounds(new Rectangle(65, 28, 165, 24));
        millLoseCheckBox.setText("Milling = Loss Condition");
        millLoseCheckBox.setBounds(new Rectangle(65, 94, 190, 25));
        
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
        jPanel3.add(resizeCheckbox, null);
        jPanel3.add(millLoseCheckBox, null);
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
    
    //make sure credits/life get updated after shopping at bazaar
    public void setVisible(boolean b)
    {
    	refreshCredits();
    	refreshLife();
    	super.setVisible(b);
    }
    
    void infoButton_actionPerformed(ActionEvent e)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Abraham Lincoln 3	hard	Some flying creatures with Flamebreak and life gaining");
        sb.append("\r\n");
        sb.append("Albert Einstein 2	medium	Garruk Wildspeaker, W+G creatures with Needle Storm and Retribution of the Meek");
        sb.append("\r\n");
        sb.append("Albert Einstein 3	hard	Stronger version of the above deck");
        sb.append("\r\n");
        sb.append("Bamm Bamm Rubble 1	easy   	Sloth's easy domain deck, creatures and spells with the Domain ability");
        sb.append("\r\n");
        sb.append("Barney Rubble 1   	easy   	Sloth's Sovereigns of Lost Alara deck with walls and auras");
        sb.append("\r\n");
        sb.append("Barney Rubble 2   	medium   	Sloth's Sovereigns of Lost Alara deck with walls and auras");
        sb.append("\r\n");
        sb.append("Barney Rubble 3   	hard   	Sloth's Sovereigns of Lost Alara deck with walls and auras");
        sb.append("\r\n");
        sb.append("Bart Simpson 1   	easy   	AI has creatures that will tap your creatures and will use auras to keep them tapped");
        sb.append("\r\n");
        sb.append("Bart Simpson 2   	medium   	AI has creatures that will tap your creatures and will use auras to keep them tapped");
        sb.append("\r\n");
        sb.append("Bart Simpson 3   	hard   	AI has creatures that will tap your creatures and will use auras to keep them tapped");
        sb.append("\r\n");
        sb.append("Batman 3		hard	Creatures with Exalted and Unblockable abilities, WoG and Armageddon");
        sb.append("\r\n");
        sb.append("Bela Lugosi 3   		hard   	Rares' Vampire deck, B creatures, little to no spells");
        sb.append("\r\n");
        sb.append("Betty Rubble 3		hard   	Indicatie's Summer Bloom deck with mods, features Plant + Eldrazi Spawn tokens");
        sb.append("\r\n");
        sb.append("Blackbeard 3		hard	Soldiers with Preeminent Captain, WoG and Armageddon");
        sb.append("\r\n");
        sb.append("Boba Fett 3		hard	Dragons, Chandra Nalaar, Crucible of Fire and Dragon Roost");
        sb.append("\r\n");
        sb.append("Boris Karloff 3		hard	LokiUndergod's Boros Aggro (RW) deck with mods, Kors, levelers and threat removal");
        sb.append("\r\n");
        sb.append("Buffy 1		easy	Vampires and creatures with wither + Sorceress Queen");
        sb.append("\r\n");
        sb.append("Buffy 2		medium	Vampires and creatures with wither + Sorceress Queen");
        sb.append("\r\n");
        sb.append("Buffy 3		hard	Vampires and creatures with wither + Sorceress Queen");
        sb.append("\r\n");
        sb.append("C3PO 3		hard	Goblins, Goblin Ringleader, Kiki-Jiki, Mad Auntie and Sensation Gorger");
        sb.append("\r\n");
        sb.append("Catwoman 1		easy	Cat creatures G+W");
        sb.append("\r\n");
        sb.append("Catwoman 2		medium	Cats creatures G+W+R with Lightning Helix");
        sb.append("\r\n");
        sb.append("Comic Book Guy 3	hard	Dragons, Chandra Nalaar, Crucible of Fire + Enlightened Tutor, Moat");
        sb.append("\r\n");
        sb.append("Crocodile Dundee 1	easy	Some Mountainwalk creatures, Rakka Mar");
        sb.append("\r\n");
        sb.append("Crocodile Dundee 2	medium	Some Mountainwalk creatures, Kamahl, Pit Fighter");
        sb.append("\r\n");
        sb.append("Crocodile Dundee 3	hard	Some Mountainwalk creatures, Kamahl, Pit Fighter");
        sb.append("\r\n");
        sb.append("Cyclops 3		hard	Slivers mainly, some spells");
        sb.append("\r\n");
        sb.append("Da Vinci 1		easy	Some Swampwalk creatures, Morph creatures + discard spells");
        sb.append("\r\n");
        sb.append("Da Vinci 2		medium	Some Swampwalk creatures, Morph creatures + drain/gain life spells");
        sb.append("\r\n");
        sb.append("Da Vinci 3		hard	Some Swampwalk creatures, Morph creatures + Angel of Despair");
        sb.append("\r\n");
        sb.append("Darth Vader 3		hard	UW Battle of Wits style alternate win type deck, WoG");
        sb.append("\r\n");
        sb.append("Data 3		hard	Korlash, Heir to Blackblade, Liliana Vess");
        sb.append("\r\n");
        sb.append("Doc Holiday 1		easy	Morph + Regenerate GWU creatures");
        sb.append("\r\n");
        sb.append("Doc Holiday 2		medium	Morph + Regenerate GWU creatures");
        sb.append("\r\n");
        sb.append("Doc Holiday 3		hard	Morph + Regenerate GWU creatures");
        sb.append("\r\n");
        sb.append("Dr No 3  		hard   	The Rack, Balance, Propaganda, discard spells");
        sb.append("\r\n");
        sb.append("Electro 3  		hard   	Resonantg's Stormfront deck with mods, Arashi, the Sky Asunder + Ball Lightning");
        sb.append("\r\n");
        sb.append("Fat Albert 1   		easy   	Winter Orb, Keldon Warlord, mana Elves/Slivers + several 4/4 creatures");
        sb.append("\r\n");
        sb.append("Fat Albert 2   		medium   	Winter Orb, Keldon Warlord, mana Elves/Slivers + several 5/5 creatures");
        sb.append("\r\n");
        sb.append("Fat Albert 3   		hard  	Winter Orb, Keldon Warlord, mana Elves/Slivers + several 6/6 creatures");
        sb.append("\r\n");
        sb.append("Fred FLintstone 3	hard  	Reasontg's Predator's Garden deck with mods, featuring Lorescale Coatl");
        sb.append("\r\n");
        sb.append("Frodo 1		easy	New, Apthaven's AI Zoo Easy, some creature removal");
        sb.append("\r\n");
        sb.append("Frodo 2		medium	New, Apthaven's AI Zoo Medium, some creature removal + Glorious Anthem");
        sb.append("\r\n");
        sb.append("Frodo 3		hard	New, Apthaven's AI Zoo Hard, more creature removal + Glorious Anthems");
        sb.append("\r\n");
        sb.append("Genghis Khan 1	easy	Mana Elves + Birds + Armageddon, Llanowar Behemoth");
        sb.append("\r\n");
        sb.append("Genghis Khan 2	medium	Mana Elves + Birds + Armageddon, Llanowar Behemoth");
        sb.append("\r\n");
        sb.append("Genghis Khan 3	hard	Mana Elves + Birds + Armageddon, Llanowar Behemoth + Elspeth, Knight-Errant");
        sb.append("\r\n");
        sb.append("George of the Jungle 1	easy	Belligerent Hatchling, Battlegate Mimic, Ajani Vengeant + a few RW spells");
        sb.append("\r\n");
        sb.append("George of the Jungle 2	medium	Belligerent Hatchling, Battlegate Mimic, Ajani Vengeant + some RW spells");
        sb.append("\r\n");
        sb.append("George of the Jungle 3	hard	Belligerent Hatchling, Battlegate Mimic, Ajani Vengeant + many RW spells");
        sb.append("\r\n");
        sb.append("Gold Finger 3   	hard   	Rares' U control deck, various counter spells and Serra Sphinx + Memnarch");
        sb.append("\r\n");
        sb.append("Grampa Simpson 1   	easy   	Sloth' double strike deck, various equipments and auras");
        sb.append("\r\n");
        sb.append("Grampa Simpson 2   	medium   	Sloth' double strike deck, various equipments and auras");
        sb.append("\r\n");
        sb.append("Grampa Simpson 3   	hard   	Sloth' double strike deck, various equipments and auras");
        sb.append("\r\n");
        sb.append("Green Lantern 3	hard	Nicol Bolas, Planeswalker + threat removal and several creatures");
        sb.append("\r\n");
        sb.append("Han Solo 3		hard	WG enchantments deck with Sigil of the Empty Throne");
        sb.append("\r\n");
        sb.append("Harry Potter 1  		easy   	Sloth' deck, easier version of Beached As' mill and counter spell deck");
        sb.append("\r\n");
        sb.append("Harry Potter 2  		medium   	Sloth' deck, easier version of Beached As' mill and counter spell deck");
        sb.append("\r\n");
        sb.append("Harry Potter 3  		hard   	Beached As' deck, various milling cards, some speed up and counter spells");
        sb.append("\r\n");
        sb.append("Higgins 3		hard   	Corwin72's Grixis Control deck, lots of threat removal and some creatures");
        sb.append("\r\n");
        sb.append("Homer Simpson 1	easy	Morph + Regenerate BRU creatures, + Raise Dead");
        sb.append("\r\n");
        sb.append("Homer Simpson 2	medium	Morph + Regenerate BRU creatures, + Raise Dead");
        sb.append("\r\n");
        sb.append("Homer Simpson 3	hard	Morph + Regenerate BRU creatures, + card draw and creature buff");
        sb.append("\r\n");
        sb.append("Iceman 3		hard	BU Bounce and Control style deck");
        sb.append("\r\n");
        sb.append("Indiana Jones 1	easy	Sol'kanar + buff");
        sb.append("\r\n");
        sb.append("Indiana Jones 2	medium	Sol'kanar + buff + Raise Dead");
        sb.append("\r\n");
        sb.append("Indiana Jones 3	hard	Sol'kanar + buff + Terminate");
        sb.append("\r\n");
        sb.append("Jabba the Hut 3	hard	Creatures with exalted and land walking abilities");
        sb.append("\r\n");
        sb.append("Jack Sparrow 1		easy	Pirate type creatures + draw cards + counter spells");
        sb.append("\r\n");
        sb.append("Jack Sparrow 2		medium	Pirate type creatures + draw cards + threat removal");
        sb.append("\r\n");
        sb.append("Jack Sparrow 3		hard	Pirate type creatures + draw cards + creature control");
        sb.append("\r\n");
        sb.append("James Bond 1		easy	gohongohon's easy WG Agro with several Slivers");
        sb.append("\r\n");
        sb.append("James Bond 2		medium	gohongohon's Medium WG Agro with several Slivers + Glorious Anthem");
        sb.append("\r\n");
        sb.append("James Bond 3		hard	gohongohon's Hard WGR Agro");
        sb.append("\r\n");
        sb.append("James T Kirk 3		hard	Rares 40 card black discard deck + Liliana Vess");
        sb.append("\r\n");
        sb.append("Joe Kidd 1		easy	Voracious Hatchling, Nightsky Mimic, no planeswalkers + a few WB spells");
        sb.append("\r\n");
        sb.append("Joe Kidd 2		medium	Voracious Hatchling, Nightsky Mimic, no planeswalkers + some WB spells");
        sb.append("\r\n");
        sb.append("Joe Kidd 3		hard	Voracious Hatchling, Nightsky Mimic, no planeswalkers + many WB spells");
        sb.append("\r\n");
        sb.append("King Arthur 1		easy	Wilt-Leaf Cavaliers; Knight of the Skyward Eye and Leonin Skyhunter");
        sb.append("\r\n");
        sb.append("King Arthur 2		medium	Wilt-Leaf Cavaliers; Knights with flanking");
        sb.append("\r\n");
        sb.append("King Arthur 3		hard	Sir Shandlar of Eberyn; Knights with first strike");
        sb.append("\r\n");
        sb.append("King Edward 1		easy	Elementals, 5 color deck with Tribal Flames");
        sb.append("\r\n");
        sb.append("King Edward 2		medium	Elementals, 5 color deck with Tribal Flames");
        sb.append("\r\n");
        sb.append("King Edward 3		hard	Elementals, 5 color deck with Tribal Flames featuring Horde of Notions");
        sb.append("\r\n");
        sb.append("King Kong 1		easy	Squirrel tokens, changelings and Deranged Hermit + curse type auras");
        sb.append("\r\n");
        sb.append("King Kong 2		medium	Squirrel tokens, changelings and Deranged Hermit + curse type auras");
        sb.append("\r\n");
        sb.append("King Kong 3		hard	Squirrel tokens, changelings and Deranged Hermit + threat removal");
        sb.append("\r\n");
        sb.append("Kojak 1		easy	Some Islandwalk creatures GU + filler");
        sb.append("\r\n");
        sb.append("Kojak 2		medium	Some Islandwalk creatures GU + filler");
        sb.append("\r\n");
        sb.append("Kojak 3		hard	Some Islandwalk creatures GUB + filler");
        sb.append("\r\n");
        sb.append("Lisa Simpson 3   	hard   	GW deck, creates tokens which are devoured by Skullmulcher and Gluttonous Slime");
        sb.append("\r\n");
        sb.append("Luke Skywalker 3	hard	GWU weenie style deck with Garruk Wildspeaker and Gaea's Anthem");
        sb.append("\r\n");
        sb.append("Maggie Simpson 3	hard	This is a jund deck from the deck forum with some modifications");
        sb.append("\r\n");
        sb.append("Magneto 3		hard	Shriekmaw, Assassins, creature removal + Liliana Vess");
        sb.append("\r\n");
        sb.append("Magnum 1		easy	Sturdy Hatchling, Shorecrasher Mimic, Garruk & Jace, the Mind Sculptor + GU spells");
        sb.append("\r\n");
        sb.append("Magnum 2		medium	Sturdy Hatchling, Shorecrasher Mimic, Garruk & Jace, the Mind Sculptor + GU spells");
        sb.append("\r\n");
        sb.append("Magnum 3		hard	Sturdy Hatchling, Shorecrasher Mimic, Garruk & Jace, the Mind Sculptor + GU spells");
        sb.append("\r\n");
        sb.append("Marge Simpson 3 	hard   	RG deck, creates tokens which are devoured by R and RG creatures with devour");
        sb.append("\r\n");
        sb.append("Morpheus 3		hard	Elves with Overrun, Gaea's Anthem, Imperious Perfect and other pumps");
        sb.append("\r\n");
        sb.append("Mr Slate 3		hard	Corwin72's Don't Go in the Water deck with mods, Merfolk and Merfolk pumps");
        sb.append("\r\n");
        sb.append("Napoleon 3		hard	Walls, Rolling Stones and Doran, the Siege Tower");
        sb.append("\r\n");
        sb.append("Ned Flanders 1		easy	Sloth's reanimator deck, a few large creatures and some spells");
        sb.append("\r\n");
        sb.append("Ned Flanders 2		medium	Sloth's reanimator deck, a few large creatures and some spells");
        sb.append("\r\n");
        sb.append("Ned Flanders 3		hard	Sloth's reanimator deck, a few large creatures and some spells");
        sb.append("\r\n");
        sb.append("Neo 3		hard	RG with Groundbreaker and other attack once then sacrifice at EoT creatures");
        sb.append("\r\n");
        sb.append("Newton 3		hard	Relentless Rats, Ratcatcher, Aluren and Harmonize");
        sb.append("\r\n");
        sb.append("Pebbles Flintstone 2	medium	Sloth' Meekstone deck, Meekstone, Marble Titan and creatures with vigilance");
        sb.append("\r\n");
        sb.append("Pebbles Flintstone 3	hard	Sloth' Meekstone deck, Meekstone, Marble Titan and creatures with vigilance");
        sb.append("\r\n");
        sb.append("Picard 3		hard	UWG Elf deck similar to Morpheus but also has flying elves");
        sb.append("\r\n");
        sb.append("Pinky and the Brain 3	hard	Royal Assassin, WoG + Damnation, Liliana Vess, Beacon of Unrest");
        sb.append("\r\n");
        sb.append("Professor X 3		hard	Master of Etherium + Vedalken Archmage and many artifacts");
        sb.append("\r\n");
        sb.append("R2-D2 3   		hard  	Black Vise, bounce (Boomerang) spells, Howling Mine");
        sb.append("\r\n");
        sb.append("Rocky 1		easy	Pro red, Flamebreak + Tremor + Pyroclasm but no Pyrohemia");
        sb.append("\r\n");
        sb.append("Rocky 2		medium	Pro red, Flamebreak + Tremor + Pyroclasm but no Pyrohemia");
        sb.append("\r\n");
        sb.append("Rocky 3		hard	Pro red, Flamebreak + Tremor + Pyroclasm but no Pyrohemia");
        sb.append("\r\n");
        sb.append("Rogue 3		hard	Dragons including Tarox Bladewing, Dragon Roost, Chandra Nalaar");
        sb.append("\r\n");
        sb.append("Scooby Doo 3   	hard   	Rares' Red deck, Dragonmaster Outcast, Rakdos Pit Dragon, Kamahl, Pit Fighter");
        sb.append("\r\n");
        sb.append("Scotty 2		medium	Pestilence + Castle + Penumbra Kavu/Spider/Wurm but no pro black");
        sb.append("\r\n");
        sb.append("Seabiscuit 1		easy	Some Fear creatures, bounce and draw card spells");
        sb.append("\r\n");
        sb.append("Seabiscuit 2		medium	Some Fear creatures, Garza Zol, Plague Queen + draw card spells");
        sb.append("\r\n");
        sb.append("Seabiscuit 3	 	hard	Some Fear creatures, Garza Zol, Plague Queen + draw card & control spells");
        sb.append("\r\n");
        sb.append("Sherlock Holmes 1	easy	Some Forestwalk creatures, Damnation + pump spells");
        sb.append("\r\n");
        sb.append("Sherlock Holmes 2	medium	Some Forestwalk creatures, gain life + threat removal spells");
        sb.append("\r\n");
        sb.append("Sherlock Holmes 3	hard	Some Forestwalk creatures, gain life + threat removal spells + Eladamri, Lord of Leaves");
        sb.append("\r\n");
        sb.append("Silver Surfer 3		hard	Green creature beat down deck with several pump spells");
        sb.append("\r\n");
        sb.append("Spiderman 2		medium	White weenies with WoG, Armageddon, Mass Calcify");
        sb.append("\r\n");
        sb.append("Spock 2		medium	Rares elf deck with just a single copy of most of the elves");
        sb.append("\r\n");
        sb.append("Storm 1		easy	Creatures with Lifelink + filler");
        sb.append("\r\n");
        sb.append("Storm 2		medium	Creatures with Lifelink + filler");
        sb.append("\r\n");
        sb.append("Storm 3		hard	Creatures with Lifelink + filler");
        sb.append("\r\n");
        sb.append("Superman 1		easy	Vecc\'s easy Slivers deck, Raise Dead + Breath of Life");
        sb.append("\r\n");
        sb.append("Superman 2		medium	Vecc\'s medium Slivers deck, Zombify + Tribal Flames");
        sb.append("\r\n");
        sb.append("Tarzan 1		easy	Jungle creatures + pump spells");
        sb.append("\r\n");
        sb.append("Tarzan 2		medium	Tarzan with Silverback Ape + pump spells");
        sb.append("\r\n");
        sb.append("Terminator 3		hard	Master of Etherium + Control Magic and Memnarch + many artifacts");
        sb.append("\r\n");
        sb.append("The Great Gazoo 3	hard	Sloth's Sun Lotion deck, red damage all spells and pro from red creatures");
        sb.append("\r\n");
        sb.append("Uncle Owen 3		hard	Creature removal/control with Liliana Vess");
        sb.append("\r\n");
        sb.append("Wilma Flintstone 1	easy	Noxious Hatchling, Woodlurker Mimic, Liliana Vess + a few BG spells");
        sb.append("\r\n");
        sb.append("Wilma Flintstone 2	medium	Noxious Hatchling, Woodlurker Mimic, Liliana Vess + some BG spells");
        sb.append("\r\n");
        sb.append("Wilma Flintstone 3	hard	Noxious Hatchling, Woodlurker Mimic, Liliana Vess + many BG spells");
        sb.append("\r\n");
        sb.append("Wolverine 3		hard	Nightmare + Korlash, Heir to Blackblade + Kodama's Reach");
        sb.append("\r\n");
        sb.append("Wyatt Earp 1		easy	Some Plainswalk creatures + filler");
        sb.append("\r\n");
        sb.append("Wyatt Earp 2		medium	Some Plainswalk creatures + filler");
        sb.append("\r\n");
        sb.append("Wyatt Earp 3		hard	Some Plainswalk creatures + filler");

        File base = ForgeProps.getFile(IMAGE_ICON);
    	File file = new File(base, "notesIcon.png");
    	ImageIcon icon = new ImageIcon(file.toString());
            
        JTextArea area = new JTextArea(sb.toString(), 35, 70);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
            
        area.setOpaque(false);
            
        JOptionPane.showMessageDialog(null, new JScrollPane(area), "Opponent Deck Notes", JOptionPane.INFORMATION_MESSAGE, icon);
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
        
        //DO NOT CHANGE THIS ORDER, GuiDisplay needs to be created before cards are added
        if(resizeCheckbox.isSelected()) AllZone.Display = new GuiDisplay3();
        else AllZone.Display = new GuiDisplay2();
        
        if(smoothLandCheckBox.isSelected()) Constant.Runtime.Smooth[0] = true;
        else Constant.Runtime.Smooth[0] = false;
        
        if(millLoseCheckBox.isSelected())
        	Constant.Runtime.Mill[0] = true;
        else
        	Constant.Runtime.Mill[0] = false;
        
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
        
        if(smoothLandCheckBox.isSelected()) Constant.Runtime.Smooth[0] = true;
        else Constant.Runtime.Smooth[0] = false;
        
        if(millLoseCheckBox.isSelected())
        	Constant.Runtime.Mill[0] = true;
        else
        	Constant.Runtime.Mill[0] = false;
        
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
