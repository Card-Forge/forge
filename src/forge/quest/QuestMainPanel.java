package forge.quest;

import forge.*;
import forge.gui.GuiUtils;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


//presumes AllZone.QuestData is not null

//AllZone.QuestData should be set by Gui_QuestOptions
public class QuestMainPanel extends JPanel {
    private QuestData questData;

    private QuestMainFrame mainFrame;

    JLabel creditsLabel = new JLabel();
    JLabel lifeLabel = new JLabel();
    JLabel statsLabel = new JLabel();
    JLabel titleLabel = new JLabel();

    JComboBox petComboBox = new JComboBox();
    JComboBox deckComboBox = new JComboBox();

    JButton questButton = new JButton("Quest");

    QuestOpponent selectedOpponent = null;

    private JCheckBox devModeCheckBox = new JCheckBox("Developer Mode");
    public static JCheckBox newGUICheckbox = new JCheckBox("Use new UI", true);
    private JCheckBox smoothLandCheckBox = new JCheckBox("Adjust AI Land");

    private JCheckBox petCheckBox = new JCheckBox("Summon Pet");
    private JCheckBox plantBox = new JCheckBox("Summon Plant");

    private static final String NO_DECKS_AVAILABLE = "No decks available";

    public QuestMainPanel(QuestMainFrame mainFrame) {
        questData = AllZone.QuestData;
        this.mainFrame = mainFrame;

        initUI();
    }

    private void initUI() {
        JLabel modeLabel;
        JLabel difficultyLabel;

        JPanel nextMatchPanel;
        JPanel optionsPanel;

        refresh();

        nextMatchPanel = createNextMatchPanel();
        this.setLayout(new BorderLayout(5, 5));
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel eastPanel = new JPanel();
        this.add(northPanel, BorderLayout.NORTH);
        this.add(centerPanel, BorderLayout.CENTER);
        centerPanel.add(nextMatchPanel, BorderLayout.CENTER);
        centerPanel.add(eastPanel, BorderLayout.EAST);

        //Create labels at the top
        titleLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 28));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(titleLabel);

        northPanel.add(Box.createVerticalStrut(5));

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusPanel.setAlignmentX(LEFT_ALIGNMENT);

        modeLabel = new JLabel(questData.getMode());
        statusPanel.add(modeLabel);
        statusPanel.add(Box.createHorizontalGlue());

        difficultyLabel = new JLabel(questData.getDifficulty());
        statusPanel.add(difficultyLabel);
        statusPanel.add(Box.createHorizontalGlue());

        statusPanel.add(statsLabel);
        northPanel.add(statusPanel);
        GuiUtils.addGap(northPanel);

        //Create options checkbox list
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));

        optionsPanel.add(this.devModeCheckBox);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(this.newGUICheckbox);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(this.smoothLandCheckBox);
        optionsPanel.setBorder(new TitledBorder(new EtchedBorder(), "Options"));

        List<Component> eastComponents = new ArrayList<Component>();
        //Create buttons

        JButton mainMenuButton = new JButton("Return to Main Menu");
        mainMenuButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                mainFrame.returnToMainMenu();
            }
        });
        eastComponents.add(mainMenuButton);

        JButton cardShopButton = new JButton("Card Shop");
        cardShopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                QuestMainPanel.this.showCardShop();
            }
        });
        eastComponents.add(cardShopButton);
        cardShopButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));

        JButton bazaarButton = null;

        if (questData.getMode().equals(QuestData.FANTASY)) {

            bazaarButton = new JButton("Bazaar");
            bazaarButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    QuestMainPanel.this.showBazaar();
                }
            });
            eastComponents.add(bazaarButton);
            bazaarButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        }



        questButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                QuestMainPanel.this.showQuests();
            }
        });
        eastComponents.add(questButton);
        questButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        questButton.setPreferredSize(new Dimension(0, 60));


        JButton playButton = new JButton("Play");
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                QuestMainPanel.this.launchGame();
            }
        });

        playButton.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
        playButton.setPreferredSize(new Dimension(0, 100));


        eastComponents.add(playButton);
        eastComponents.add(optionsPanel);

        GuiUtils.setWidthToMax(eastComponents);

        eastPanel.add(mainMenuButton);
        GuiUtils.addGap(eastPanel);
        eastPanel.add(optionsPanel);
        eastPanel.add(Box.createVerticalGlue());
        eastPanel.add(Box.createVerticalGlue());
        eastPanel.add(creditsLabel);
        GuiUtils.addGap(eastPanel);
        eastPanel.add(cardShopButton);

        if (questData.getMode().equals(QuestData.FANTASY)) {
            GuiUtils.addGap(eastPanel);
            eastPanel.add(bazaarButton);
        }

        eastPanel.add(Box.createVerticalGlue());

        eastPanel.add(questButton);
        GuiUtils.addGap(eastPanel);
        eastPanel.add(playButton);

        eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));

    }

    private JPanel createNextMatchPanel() {
        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(new TitledBorder(new EtchedBorder(), "Next Match"));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel opponentPanel = new JPanel();
        opponentPanel.setLayout(new BoxLayout(opponentPanel, BoxLayout.Y_AXIS));
        opponentPanel.setBorder(new TitledBorder(new EtchedBorder(), "Opponent"));

        List<QuestOpponent> opponents = QuestOpponent.getOpponents();

        for (QuestOpponent opponent : opponents) {
            opponentPanel.add(opponent);
            opponent.addMouseListener(new OpponentAdapter(opponent));

            GuiUtils.addGap(opponentPanel, 3);


        }

        opponentPanel.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(opponentPanel);

        GuiUtils.addGap(mainPanel, 10);

        JPanel deckPanel = new JPanel();
        deckPanel.setLayout(new BoxLayout(deckPanel, BoxLayout.X_AXIS));

        JLabel deckLabel = new JLabel("Use Deck");
        deckPanel.add(deckLabel);
        GuiUtils.addGap(deckPanel);
        deckPanel.add(this.deckComboBox);
        GuiUtils.addGap(deckPanel);

        JButton editDeckButton = new JButton("Deck Editor");
        editDeckButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showDeckEditor();
            }
        });
        deckPanel.add(editDeckButton);
        deckPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, deckPanel.getPreferredSize().height));
        deckPanel.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(deckPanel);


        GuiUtils.addGap(mainPanel);

        if (questData.getMode().equals(QuestData.FANTASY)) {
            JPanel petPanel = new JPanel();
            petPanel.setLayout(new BoxLayout(petPanel, BoxLayout.X_AXIS));
            petPanel.add(this.petCheckBox);
            GuiUtils.addGap(petPanel);
            petPanel.add(this.petComboBox);
            mainPanel.add(petPanel);
            petPanel.setMaximumSize(petPanel.getPreferredSize());
            petPanel.setAlignmentX(LEFT_ALIGNMENT);


            GuiUtils.addGap(mainPanel);
            mainPanel.add(this.lifeLabel);
            this.lifeLabel.setAlignmentX(LEFT_ALIGNMENT);
            this.lifeLabel.setMaximumSize(this.lifeLabel.getPreferredSize());
        }
        outerPanel.add(mainPanel);

        return outerPanel;
    }

    void refresh() {
        creditsLabel.setText("Credits: " + questData.getCredits());
        statsLabel.setText(questData.getWin() + " wins / " + questData.getLost() + " losses");
        titleLabel.setText(questData.getRank());

        deckComboBox.removeAllItems();

        if (questData.getDeckNames().size() > 0) {

            deckComboBox.setEnabled(true);
            for (String deckName : questData.getDeckNames()) {
                deckComboBox.addItem(deckName);
            }
        }

        else {
            deckComboBox.addItem(NO_DECKS_AVAILABLE);
            deckComboBox.setEnabled(false);
        }
        deckComboBox.setMinimumSize(new Dimension(150, 0));

        questButton.setEnabled(shouldQuestsBeEnabled());

        if (questData.getMode().equals(QuestData.FANTASY)) {
            lifeLabel.setText("Starting Life: " + questData.getLife());
            petComboBox.removeAllItems();

            List<String> petList = QuestUtil.getPetNames(questData);

            if (petList.size() > 0) {
                petComboBox.setEnabled(true);
                petCheckBox.setEnabled(true);
                for (String aPetList : petList) {
                    petComboBox.addItem(aPetList);
                }
            }

            else {
                petComboBox.addItem("No pets available");
                petComboBox.setEnabled(false);
                petCheckBox.setEnabled(false);
            }
        }
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


    void showDeckEditor() {
        Command exit = new Command() {
            private static final long serialVersionUID = -5110231879431074581L;

            public void execute() {
                //saves all deck data
                QuestData.saveData(AllZone.QuestData);

                new QuestMainFrame();
            }
        };

        Gui_Quest_DeckEditor g = new Gui_Quest_DeckEditor();

        g.show(exit);
        g.setVisible(true);
        mainFrame.dispose();
    }//deck editor button

    void showBazaar() {
        mainFrame.showPane(QuestMainFrame.BAZAAR_PANEL);
    }

    void showCardShop() {
        Command exit = new Command() {
			private static final long serialVersionUID = 8567193482568076362L;

			public void execute() {
                //saves all deck data
                QuestData.saveData(AllZone.QuestData);

                new QuestMainFrame();
            }
        };

        Gui_CardShop g = new Gui_CardShop(questData);

        g.show(exit);
        g.setVisible(true);

        this.mainFrame.dispose();

    }//card shop button

    void launchGame() {
        Object check = deckComboBox.getSelectedItem();
        if (check == NO_DECKS_AVAILABLE || getSelectedOpponent().equals("")) {
            return;
        }

        Deck human = questData.getDeck(check.toString());
        Deck computer = questData.ai_getDeckNewFormat(getSelectedOpponent());

        Constant.Runtime.HumanDeck[0] = human;
        Constant.Runtime.ComputerDeck[0] = computer;

        String oppIconName = getSelectedOpponent();
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

        if (questData.getMode().equals(QuestData.REALISTIC)) {
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
        mainFrame.dispose();
    }//play game button

    private String getSelectedOpponent() {
        if (selectedOpponent == null) {
            return "";
        }

        return selectedOpponent.getName();
    }

    void showQuests() {
        Object deckName = deckComboBox.getSelectedItem();
        if (deckName == null) {
            return;
        }

        Deck human = questData.getDeck(deckName.toString());

        Constant.Runtime.Smooth[0] = smoothLandCheckBox.isSelected();

        Constant.Runtime.DevMode[0] = devModeCheckBox.isSelected();

        Object pet = petComboBox.getSelectedItem();
        if (pet != null) {
            questData.setSelectedPet(pet.toString());
        }

        Gui_Quest_Assignments g = new Gui_Quest_Assignments(human);
        g.setVisible(true);
        mainFrame.dispose();
    }

    class OpponentAdapter extends MouseAdapter {
        QuestOpponent opponent;

        OpponentAdapter(QuestOpponent opponent) {
            super();
            this.opponent = opponent;
        }

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {

            if (selectedOpponent != null) {
                selectedOpponent.setSelected(false);
            }

            opponent.setSelected(true);

            selectedOpponent = opponent;
        }
    }

}
