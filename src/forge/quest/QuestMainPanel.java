package forge.quest;

import forge.*;
import forge.error.ErrorViewer;
import forge.properties.NewConstants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Collections;


//presumes AllZone.QuestData is not null

//AllZone.QuestData should be set by Gui_QuestOptions
public class QuestMainPanel extends JPanel implements NewConstants {
    private QuestData questData;

    private JLabel modeNameLabel = new JLabel();
    private JLabel difficultyLabel = new JLabel();
    private JLabel winLostLabel = new JLabel();
    private JLabel rankLabel = new JLabel();
    private JLabel creditsLabel = new JLabel();
    private JLabel lifeLabel = new JLabel();
    private JButton infoButton = new JButton();
    private JButton bazaarButton = new JButton();
    private JButton cardShopButton = new JButton();
    private JButton deckEditorButton = new JButton();
    private JButton playGameButton = new JButton();
    private JButton questsButton = new JButton();

    private JRadioButton opponentRadio1 = new JRadioButton();
    private JRadioButton opponentRadio2 = new JRadioButton();
    private JRadioButton opponentRadio3 = new JRadioButton();
    private JComboBox deckComboBox = new JComboBox();
    private JComboBox petComboBox = new JComboBox();
    private ButtonGroup opponentGroup = new ButtonGroup();
    private static JCheckBox smoothLandCheckBox = new JCheckBox("", false);
    public static JCheckBox newGUICheckbox = new JCheckBox("", true);
    private static JCheckBox devModeCheckBox = new JCheckBox("", true);

    private QuestMainFrame mainFrame;

    public QuestMainPanel(QuestMainFrame mainFrame) {
        questData = AllZone.QuestData;

        this.mainFrame = mainFrame;
        try {
            initUI();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
        setup();

        setVisible(true);
    }

    private void setup() {
        //set labels
        difficultyLabel.setText(questData.getDifficulty() + " - " + questData.getMode());
        rankLabel.setText(questData.getRank());
        creditsLabel.setText("Credits: " + questData.getCredits());

        if (questData.getMode().equals("Fantasy")) {
            int life = questData.getLife();
            if (life < 15) {
                questData.setLife(15);
            }
            lifeLabel.setText("Max Life: " + questData.getLife());
        }

        String s = questData.getWin() + " wins / " + questData.getLost() + " losses";
        winLostLabel.setText(s);

        String[] op = questData.getOpponents();
        opponentRadio1.setText(op[0]);
        opponentRadio1.setToolTipText(Gui_Quest_Deck_Info.getDescription(op[0]));
        opponentRadio2.setText(op[1]);
        opponentRadio2.setToolTipText(Gui_Quest_Deck_Info.getDescription(op[1]));
        opponentRadio3.setText(op[2]);
        opponentRadio3.setToolTipText(Gui_Quest_Deck_Info.getDescription(op[2]));


        //get deck names as Strings
        List<String> list = questData.getDeckNames();
        Collections.sort(list);
        for (String aList : list) {
            deckComboBox.addItem(aList);
        }

        if (Constant.Runtime.HumanDeck[0] != null) {
            deckComboBox.setSelectedItem(Constant.Runtime.HumanDeck[0].getName());
        }

    }//setup()

    private void initUI() throws Exception {
        this.setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new FlowLayout());
        
        Border opponentsBorder = new TitledBorder(
                BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Opponents");
        Border settingsBorder = new TitledBorder(
                BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Settings");
        Border playerBorder = new TitledBorder(
                BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Player");

        JPanel opponentPanel = new JPanel();
        opponentPanel.setBorder(opponentsBorder);
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(settingsBorder);
        JPanel playerPanel = new JPanel();
        playerPanel.setBorder(playerBorder);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0,1));

        modeNameLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        modeNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        modeNameLabel.setText("Quest Mode");

        difficultyLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        difficultyLabel.setHorizontalAlignment(SwingConstants.CENTER);

        winLostLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        winLostLabel.setHorizontalAlignment(SwingConstants.CENTER);
        winLostLabel.setHorizontalTextPosition(SwingConstants.CENTER);

        rankLabel.setFont(new java.awt.Font("Dialog", 0, 25));
        rankLabel.setHorizontalAlignment(SwingConstants.CENTER);

        creditsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        creditsLabel.setHorizontalTextPosition(SwingConstants.CENTER);


        infoButton.setFont(new java.awt.Font("Dialog", 0, 13));
        infoButton.setText("Opponent Notes");
        infoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showOpponentInfo(e);
            }
        });


        cardShopButton.setFont(new java.awt.Font("Dialog", 0, 16));
        cardShopButton.setText("Card Shop");
        cardShopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCardShop(e);
            }
        });

        deckEditorButton.setFont(new java.awt.Font("Dialog", 0, 16));
        deckEditorButton.setText("Deck Editor");
        deckEditorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showDeckEditor(e);
            }
        });




        playGameButton.setFont(new java.awt.Font("Dialog", 0, 18));
        playGameButton.setText("Play Game");
        playGameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                launchGame(e);
            }
        });

        smoothLandCheckBox.setText("Stack AI land");
        newGUICheckbox.setText("New GUI");
        devModeCheckBox.setText("Developer Mode");
        devModeCheckBox.setSelected(Constant.Runtime.DevMode[0]);



        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(settingsBorder);
        settingsPanel.add(smoothLandCheckBox, null);
        settingsPanel.add(newGUICheckbox, null);
        settingsPanel.add(devModeCheckBox, null);

        opponentPanel.setLayout(
                new BoxLayout(opponentPanel,BoxLayout.Y_AXIS));

        opponentRadio1.setAlignmentX(Component.LEFT_ALIGNMENT);
        opponentPanel.add(opponentRadio1);
        opponentRadio2.setAlignmentX(Component.LEFT_ALIGNMENT);
        opponentPanel.add(opponentRadio2);
        opponentRadio3.setAlignmentX(Component.LEFT_ALIGNMENT);
        opponentPanel.add(opponentRadio3);

        opponentGroup.add(opponentRadio1);
        opponentGroup.add(opponentRadio2);
        opponentGroup.add(opponentRadio3);
        opponentRadio1.setSelected(true);

        infoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        opponentPanel.add(infoButton);

        creditsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        playerPanel.add(creditsLabel, null);

        JPanel deckNamePanel = new JPanel();
        deckNamePanel.setLayout(new BoxLayout(deckNamePanel,BoxLayout.X_AXIS));
        JLabel deckNameLabel = new JLabel("Your Deck:");
        
        deckNamePanel.add(deckNameLabel);
        deckNamePanel.add(Box.createHorizontalStrut(5));
        deckNamePanel.add(deckComboBox);

        BoxLayout playerLayout = new BoxLayout(playerPanel, BoxLayout.Y_AXIS);
        playerPanel.setLayout(playerLayout);
        deckNamePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        playerPanel.add(deckNamePanel);

        if ("Fantasy".equals(questData.getMode())) {
            playerPanel.add(Box.createVerticalStrut(5));

            lifeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            lifeLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            lifeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            playerPanel.add(lifeLabel);

            refreshPets();
            playerPanel.add(Box.createVerticalStrut(5));
            petComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            playerPanel.add(petComboBox);

            bazaarButton.setFont(new java.awt.Font("Dialog", 0, 16));
            bazaarButton.setText("Bazaar");
            bazaarButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showBazaar(e);
                }
            });
            buttonPanel.add(bazaarButton, null);

            questsButton.setFont(new java.awt.Font("Dialog", 0, 18));
            questsButton.setText("Quests");
            questsButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showQuests(e);
                }
            });

            questsButton.setEnabled(shouldQuestsBeEnabled());
            buttonPanel.add(questsButton, null);

        }


        centerPanel.add(modeNameLabel, null);
        centerPanel.add(difficultyLabel, null);
        centerPanel.add(rankLabel, null);
        centerPanel.add(winLostLabel, null);
        buttonPanel.add(cardShopButton, null);
        buttonPanel.add(deckEditorButton, null);
        buttonPanel.add(playGameButton, null);
        centerPanel.add(opponentPanel, null);
        centerPanel.add(settingsPanel, null);
        centerPanel.add(playerPanel);
        centerPanel.add(buttonPanel);

        this.add(centerPanel, BorderLayout.CENTER);
    }

    private boolean shouldQuestsBeEnabled() {
        int questsPlayed = questData.getQuestsPlayed();
        int div = 6;

        if (questData.getGearLevel() == 1) {
            div = 5;
        }
        else if (questData.getGearLevel() == 2) {
            div = 4;
        }

        return !(questData.getWin() / div < questsPlayed || questData.getWin() < 25);
    }

    void refreshCredits() {
        creditsLabel.setText("Credits: " + questData.getCredits());
    }

    void refreshLife() {
        lifeLabel.setText("Max Life: " + questData.getLife());
    }

    void refreshPets() {

        petComboBox.removeAllItems();
        List<String> petList = QuestUtil.getPetNames(questData);

        for (String aPetList : petList) {
            petComboBox.addItem(aPetList);
        }

        petComboBox.addItem("None");
        petComboBox.addItem("No Plant/Pet");
    }

    //make sure credits/life get updated after shopping at bazaar
    public void setVisible(boolean b) {
        refreshPets();
        refreshCredits();
        refreshLife();
        super.setVisible(b);
    }

    void showOpponentInfo(ActionEvent e) {
        Gui_Quest_Deck_Info.showDeckList();
    }

    void showDeckEditor(ActionEvent e) {

        Gui_Quest_DeckEditor g = new Gui_Quest_DeckEditor();

        g.setVisible(true);

    }//deck editor button

    void showBazaar(ActionEvent e) {
        mainFrame.showPane(QuestMainFrame.BAZAAR_PANEL);
    }

    void showCardShop(ActionEvent e) {
        Gui_CardShop g = new Gui_CardShop(questData);

        g.show(null);
        g.setVisible(true);

    }//card shop button

    void launchGame(ActionEvent e) {
        Object check = deckComboBox.getSelectedItem();
        if (check == null || getOpponent().equals("")) {
            return;
        }

        Deck human = questData.getDeck(check.toString());
        Deck computer = questData.ai_getDeckNewFormat(getOpponent());

        Constant.Runtime.HumanDeck[0] = human;
        Constant.Runtime.ComputerDeck[0] = computer;

        String oppIconName = getOpponent();
        oppIconName = oppIconName.substring(0, oppIconName.length() - 1).trim() + ".jpg";


        Constant.Quest.oppIconName[0] = oppIconName;


        // Dev Mode occurs before Display
        Constant.Runtime.DevMode[0] = devModeCheckBox.isSelected();

        //DO NOT CHANGE THIS ORDER, GuiDisplay needs to be created before cards are added
        if (newGUICheckbox.isSelected()) {
            AllZone.Display = new GuiDisplay4();
        }
        else {
            AllZone.Display = new GuiDisplay3();
        }

        Constant.Runtime.Smooth[0] = smoothLandCheckBox.isSelected();

        if (questData.getMode().equals("Realistic")) {
            AllZone.GameAction.newGame(human, computer);
        }
        else {
            Object pet = petComboBox.getSelectedItem();
            if (pet != null) {
                questData.setSelectedPet(pet.toString());
            }

            CardList hCl = QuestUtil.getHumanPlantAndPet(questData);
            int hLife = QuestUtil.getLife(questData);
            AllZone.GameAction.newGame(human, computer, hCl, new CardList(), hLife, 20, null);
        }


        AllZone.Display.setVisible(true);
        //end - you can change stuff after this

        //close this window

    }//play game button

    void showQuests(ActionEvent e) {
        Object check = deckComboBox.getSelectedItem();
        if (check == null) {
            return;
        }

        Deck human = questData.getDeck(check.toString());

        Constant.Runtime.Smooth[0] = smoothLandCheckBox.isSelected();

        Constant.Runtime.DevMode[0] = devModeCheckBox.isSelected();

        Object pet = petComboBox.getSelectedItem();
        if (pet != null) {
            questData.setSelectedPet(pet.toString());
        }

        Gui_Quest_Assignments g = new Gui_Quest_Assignments(human);
        g.setVisible(true);

    }

    String getOpponent() {
        if (opponentRadio1.isSelected()) {
            return opponentRadio1.getText();
        }

        else if (opponentRadio2.isSelected()) {
            return opponentRadio2.getText();
        }

        else if (opponentRadio3.isSelected()) {
            return opponentRadio3.getText();
        }

        return "";
    }
}
