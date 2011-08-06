package forge;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import forge.error.ErrorViewer;


//presumes AllZone.QuestData is not null
//AllZone.QuestData should be set by Gui_QuestOptions
public class Gui_Quest extends JFrame {
    private static final long serialVersionUID   = -6432089669283627896L;
    
    private QuestData         questData;
    
    private JLabel            jLabel1            = new JLabel();
    private JLabel            difficultlyLabel   = new JLabel();
    private JLabel            winLostLabel       = new JLabel();
    private JLabel            rankLabel          = new JLabel();
    private JLabel			  creditsLabel	     = new JLabel();
    @SuppressWarnings("unused")
    // border1
    private Border            border1;
    private TitledBorder      titledBorder1;
    private JButton 		  cardShopButton	 = new JButton();
    private JButton           deckEditorButton   = new JButton();
    private JPanel            jPanel2            = new JPanel();
    private JButton           playGameButton     = new JButton();
    private JRadioButton      oppTwoRadio        = new JRadioButton();
    private JRadioButton      oppOneRadio        = new JRadioButton();
    private JRadioButton      oppThreeRadio      = new JRadioButton();
    private JLabel            jLabel5            = new JLabel();
    private JComboBox         deckComboBox       = new JComboBox();
    private ButtonGroup       oppGroup           = new ButtonGroup();
    private static JCheckBox  smoothLandCheckBox = new JCheckBox("", true);
    private static JCheckBox  resizeCheckbox     = new JCheckBox("", true);
    private static JCheckBox  millLoseCheckBox 	 = new JCheckBox("", true);
    
    public static void main(String[] args) {
        new Gui_Quest();
    }
    
    public Gui_Quest() {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        
        questData = AllZone.QuestData;
        setup();
        
        setVisible(true);
    }
    
    private void setup() {
        //center window on the screen
        Dimension screen = this.getToolkit().getScreenSize();
        setBounds(screen.width / 4, 50, //position
                500, 610); //size
        
        //if user closes this window, go back to "Quest Options" screen
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                Gui_Quest.this.dispose();
                new Gui_QuestOptions();
            }
        });
        
        //set labels
        difficultlyLabel.setText(questData.getDifficulty());
        rankLabel.setText(questData.getRank());
        creditsLabel.setText("Credits: " + questData.getCredits());
        
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
    }//setup()
    
    private void jbInit() throws Exception {
        border1 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Choices");
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 25));
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText("Quest Mode");
        jLabel1.setBounds(new Rectangle(1, 7, 453, 45));
        this.setResizable(false);
        this.setTitle("Quest Mode");
        this.getContentPane().setLayout(null);
        difficultlyLabel.setText("Medium");
        difficultlyLabel.setBounds(new Rectangle(1, 52, 453, 41));
        difficultlyLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        difficultlyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        winLostLabel.setText("23 wins / 10 losses");
        winLostLabel.setBounds(new Rectangle(1, 130, 453, 43));
        winLostLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        winLostLabel.setHorizontalAlignment(SwingConstants.CENTER);
        winLostLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        rankLabel.setText("Card Flopper");
        rankLabel.setBounds(new Rectangle(1, 93, 453, 37));
        rankLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        rankLabel.setHorizontalAlignment(SwingConstants.CENTER);
        creditsLabel.setBounds(new Rectangle(1, 170, 453, 37));
        //creditsLabel.setText("Credits: 1000");
        creditsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        creditsLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        
        cardShopButton.setBounds(new Rectangle(291, 100, 142, 38));
        cardShopButton.setFont(new java.awt.Font("Dialog", 0, 18));
        cardShopButton.setText("Card Shop");
        cardShopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardShopButton_actionPerformed(e);
            }
        });
        
        deckEditorButton.setBounds(new Rectangle(291, 148, 142, 38));
        deckEditorButton.setFont(new java.awt.Font("Dialog", 0, 18));
        deckEditorButton.setText("Deck Editor");
        deckEditorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deckEditorButton_actionPerformed(e);
            }
        });
        jPanel2.setBorder(titledBorder1);
        jPanel2.setBounds(new Rectangle(39, 223, 441, 198));
        jPanel2.setLayout(null);
        playGameButton.setBounds(new Rectangle(150, 516, 161, 37));
        playGameButton.setFont(new java.awt.Font("Dialog", 0, 18));
        playGameButton.setText("Play Game");
        playGameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playGameButton_actionPerformed(e);
            }
        });
        oppTwoRadio.setText("Bob");
        oppTwoRadio.setBounds(new Rectangle(15, 75, 250, 41));
        oppOneRadio.setSelected(true);
        oppOneRadio.setText("Sam");
        oppOneRadio.setBounds(new Rectangle(15, 53, 250, 33));
        oppThreeRadio.setText("Generated Deck");
        oppThreeRadio.setBounds(new Rectangle(15, 116, 250, 25));
        jLabel5.setText("Your Deck:");
        jLabel5.setBounds(new Rectangle(15, 151, 125, 29));
        deckComboBox.setBounds(new Rectangle(98, 152, 185, 29));
        smoothLandCheckBox.setText("Stack AI land");
        smoothLandCheckBox.setBounds(new Rectangle(154, 455, 153, 21));
        //smoothLandCheckBox.setSelected(true);
        resizeCheckbox.setText("Resizable Game Area");
        resizeCheckbox.setBounds(new Rectangle(154, 424, 156, 24));
        millLoseCheckBox.setText("Milling = Loss Condition");
        millLoseCheckBox.setBounds(new Rectangle(154, 484, 165, 25));
        
        //resizeCheckbox.setSelected(true);
        this.getContentPane().add(rankLabel, null);
        this.getContentPane().add(jLabel1, null);
        this.getContentPane().add(difficultlyLabel, null);
        this.getContentPane().add(winLostLabel, null);
        this.getContentPane().add(creditsLabel,null);
        jPanel2.add(jLabel5, null);
        jPanel2.add(deckComboBox, null);
        jPanel2.add(oppOneRadio, null);
        jPanel2.add(oppTwoRadio, null);
        jPanel2.add(oppThreeRadio, null);
        jPanel2.add(cardShopButton, null);
        jPanel2.add(deckEditorButton, null);
        this.getContentPane().add(playGameButton, null);
        this.getContentPane().add(smoothLandCheckBox, null);
        this.getContentPane().add(resizeCheckbox, null);
        this.getContentPane().add(millLoseCheckBox, null);
        this.getContentPane().add(jPanel2, null);
        oppGroup.add(oppOneRadio);
        oppGroup.add(oppTwoRadio);
        oppGroup.add(oppThreeRadio);
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
        Deck computer = questData.ai_getDeck(getOpponent());
        
        Constant.Runtime.HumanDeck[0] = human;
        Constant.Runtime.ComputerDeck[0] = computer;
        
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
        
        AllZone.GameAction.newGame(human, computer);
        AllZone.Display.setVisible(true);
        //end - you can change stuff after this
        
        //close this window
        dispose();
        
    }//play game button
    
    String getOpponent() {
        if(oppOneRadio.isSelected()) return oppOneRadio.getText();
        
        else if(oppTwoRadio.isSelected()) return oppTwoRadio.getText();
        
        else if(oppThreeRadio.isSelected()) return oppThreeRadio.getText();
        
        return "";
    }
}
