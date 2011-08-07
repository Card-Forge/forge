package forge.quest.gui.main;


import forge.*;
import forge.deck.Deck;
import forge.gui.GuiUtils;
import forge.quest.data.QuestBattleManager;
import forge.quest.data.QuestData;
import forge.quest.data.item.QuestItemZeppelin;
import forge.quest.gui.QuestAbstractPanel;
import forge.quest.gui.QuestFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;


/**
 * <p>QuestMainPanel class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class QuestMainPanel extends QuestAbstractPanel {
    /** Constant <code>serialVersionUID=6142934729724012402L</code> */
    private static final long serialVersionUID = 6142934729724012402L;

    private forge.quest.data.QuestData questData;

    JLabel creditsLabel = new JLabel();
    JLabel lifeLabel = new JLabel();
    JLabel statsLabel = new JLabel();
    JLabel titleLabel = new JLabel();
    JLabel nextQuestLabel = new JLabel();

    JComboBox petComboBox = new JComboBox();
    JComboBox deckComboBox = new JComboBox();

    JButton questButton = new JButton("Quests");
    JButton playButton = new JButton("Play");

    private QuestSelectablePanel selectedOpponent;

    JPanel nextMatchPanel = new JPanel();
    CardLayout nextMatchLayout;

    boolean isShowingQuests = false;
    private JCheckBox devModeCheckBox = new JCheckBox("Developer Mode");
    //private JCheckBox newGUICheckbox = new JCheckBox("Use new UI", true);
    private JCheckBox smoothLandCheckBox = new JCheckBox("Adjust AI Land");
    private JCheckBox petCheckBox = new JCheckBox("Summon Pet");

    private JCheckBox plantBox = new JCheckBox("Summon Plant");
    /** Constant <code>NO_DECKS_AVAILABLE="No decks available"</code> */
    private static final String NO_DECKS_AVAILABLE = "No decks available";
    /** Constant <code>BATTLES="Battles"</code> */
    private static final String BATTLES = "Battles";
    /** Constant <code>QUESTS="Quests"</code> */
    private static final String QUESTS = "Quests";

    //TODO: Make this ordering permanent
    /** Constant <code>lastUsedDeck="//TODO: Make this ordering permanent"</code> */
    private static String lastUsedDeck;
    private JButton zeppelinButton = new JButton("<html>Launch<br>Zeppelin</html>",
            GuiUtils.getResizedIcon(GuiUtils.getIconFromFile("ZeppelinIcon.png"), 40, 40));
    private JPanel zeppelinPanel = new JPanel();

    /**
     * <p>Constructor for QuestMainPanel.</p>
     *
     * @param mainFrame a {@link forge.quest.gui.QuestFrame} object.
     */
    public QuestMainPanel(QuestFrame mainFrame) {
        super(mainFrame);
        questData = AllZone.getQuestData();

        initUI();
    }

    /**
     * <p>initUI.</p>
     */
    private void initUI() {
        refresh();
        this.setLayout(new BorderLayout(5, 5));
        JPanel centerPanel = new JPanel(new BorderLayout());
        this.add(centerPanel, BorderLayout.CENTER);

        JPanel northPanel = createStatusPanel();
        this.add(northPanel, BorderLayout.NORTH);

        JPanel eastPanel = createSidePanel();
        this.add(eastPanel, BorderLayout.EAST);

        JPanel matchSettingsPanel = createMatchSettingsPanel();
        centerPanel.add(matchSettingsPanel, BorderLayout.SOUTH);

        centerPanel.add(nextMatchPanel, BorderLayout.CENTER);
        this.setBorder(new EmptyBorder(5, 5, 5, 5));

    }

    /**
     * <p>createStatusPanel.</p>
     *
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createStatusPanel() {
        JPanel northPanel = new JPanel();
        JLabel modeLabel;
        JLabel difficultyLabel;//Create labels at the top
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
        return northPanel;
    }

    /**
     * <p>createSidePanel.</p>
     *
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createSidePanel() {
        JPanel panel = new JPanel();
        JPanel optionsPanel;//Create options checkbox list
        optionsPanel = createOptionsPanel();

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
        if (questData.getMode().equals(forge.quest.data.QuestData.FANTASY)) {

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
        questButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        questButton.setPreferredSize(new Dimension(0, 60));


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

        panel.add(mainMenuButton);
        GuiUtils.addGap(panel);
        panel.add(optionsPanel);
        panel.add(Box.createVerticalGlue());
        panel.add(Box.createVerticalGlue());

        if (questData.getMode().equals(forge.quest.data.QuestData.FANTASY)) {
            panel.add(this.lifeLabel);
            this.lifeLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
            this.lifeLabel.setIcon(GuiUtils.getResizedIcon(GuiUtils.getIconFromFile("Life.png"), 30, 30));
        }

        GuiUtils.addGap(panel);
        panel.add(this.creditsLabel);
        this.creditsLabel.setIcon(GuiUtils.getResizedIcon(GuiUtils.getIconFromFile("CoinStack.png"), 30, 30));
        this.creditsLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        GuiUtils.addGap(panel, 10);
        panel.add(cardShopButton);

        if (questData.getMode().equals(forge.quest.data.QuestData.FANTASY)) {
            GuiUtils.addGap(panel);
            panel.add(bazaarButton);
        }

        panel.add(Box.createVerticalGlue());

        panel.add(questButton);
        this.nextQuestLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        panel.add(nextQuestLabel);
        GuiUtils.addGap(panel);

        panel.add(playButton);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /**
     * <p>createOptionsPanel.</p>
     *
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createOptionsPanel() {
        JPanel optionsPanel;
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));

        //optionsPanel.add(this.newGUICheckbox);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(this.smoothLandCheckBox);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(this.devModeCheckBox);
        optionsPanel.setBorder(new TitledBorder(new EtchedBorder(), "Options"));
        return optionsPanel;
    }

    /**
     * <p>createMatchSettingsPanel.</p>
     *
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createMatchSettingsPanel() {

        JPanel matchPanel = new JPanel();
        matchPanel.setLayout(new BoxLayout(matchPanel, BoxLayout.Y_AXIS));

        JPanel deckPanel = new JPanel();
        deckPanel.setLayout(new BoxLayout(deckPanel, BoxLayout.X_AXIS));

        JLabel deckLabel = new JLabel("Use Deck");
        deckPanel.add(deckLabel);
        GuiUtils.addGap(deckPanel);

        this.deckComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                playButton.setEnabled(canGameBeLaunched());
                lastUsedDeck = (String) deckComboBox.getSelectedItem();
            }
        });

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
        matchPanel.add(deckPanel);


        GuiUtils.addGap(matchPanel);

        if (questData.getMode().equals(forge.quest.data.QuestData.FANTASY)) {
            JPanel fantasyPanel = new JPanel();
            fantasyPanel.setLayout(new BorderLayout());

            JPanel petPanel = new JPanel();
            petPanel.setLayout(new BoxLayout(petPanel, BoxLayout.X_AXIS));

            this.petCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    if (petCheckBox.isSelected()) {
                        questData.getPetManager().setSelectedPet((String) petComboBox.getSelectedItem());
                    } else {
                        questData.getPetManager().setSelectedPet(null);
                    }

                    petComboBox.setEnabled(petCheckBox.isSelected());
                }
            });

            petPanel.add(this.petCheckBox);
            GuiUtils.addGap(petPanel);
            this.petComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    if (petCheckBox.isSelected()) {
                        questData.getPetManager().setSelectedPet((String) petComboBox.getSelectedItem());
                    } else {
                        questData.getPetManager().setSelectedPet(null);
                    }
                }
            });
            this.petComboBox.setMaximumSize(
                    new Dimension(Integer.MAX_VALUE,
                            (int) this.petCheckBox.getPreferredSize().getHeight()));
            petPanel.add(this.petComboBox);

            this.plantBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    questData.getPetManager().usePlant = plantBox.isSelected();
                }
            });

            GuiUtils.addGap(petPanel, 10);
            petPanel.add(this.plantBox);
            petPanel.setMaximumSize(petPanel.getPreferredSize());
            petPanel.setAlignmentX(LEFT_ALIGNMENT);

            fantasyPanel.add(petPanel, BorderLayout.WEST);

            zeppelinButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    questData.randomizeOpponents();
                    refreshNextMatchPanel();
                    QuestItemZeppelin zeppelin = (QuestItemZeppelin) questData.getInventory().getItem("Zeppelin");
                    zeppelin.setZeppelinUsed(true);
                    zeppelinButton.setEnabled(false);
                }
            });

            zeppelinButton.setMaximumSize(zeppelinButton.getPreferredSize());
            zeppelinPanel.setLayout(new BorderLayout());

            fantasyPanel.add(zeppelinPanel, BorderLayout.EAST);
            fantasyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            matchPanel.add(fantasyPanel);
        }
        return matchPanel;
    }

    /**
     * <p>createBattlePanel.</p>
     *
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createBattlePanel() {
        JPanel BattlePanel = new JPanel();
        BattlePanel.setLayout(new BoxLayout(BattlePanel, BoxLayout.Y_AXIS));
        BattlePanel.setBorder(new TitledBorder(new EtchedBorder(), "Available Battles"));

        List<QuestSelectablePanel> Battles = QuestBattle.getBattles();

        for (QuestSelectablePanel Battle : Battles) {
            BattlePanel.add(Battle);
            Battle.addMouseListener(new SelectionAdapter(Battle));

            GuiUtils.addGap(BattlePanel, 3);
        }

        BattlePanel.setAlignmentX(LEFT_ALIGNMENT);

        return BattlePanel;
    }

    /**
     * <p>createQuestPanel.</p>
     *
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createQuestPanel() {
        JPanel questPanel = new JPanel();
        questPanel.setLayout(new BoxLayout(questPanel, BoxLayout.Y_AXIS));
        questPanel.setBorder(new TitledBorder(new EtchedBorder(), "Available Quests"));


        List<QuestQuest> quests = QuestQuest.getQuests();

        for (QuestQuest quest : quests) {
            questPanel.add(quest);
            quest.addMouseListener(new SelectionAdapter(quest));

            GuiUtils.addGap(questPanel, 3);
        }


        return questPanel;
    }

    /**
     * <p>refresh.</p>
     */
    void refresh() {
        AllZone.getQuestData().saveData();

        devModeCheckBox.setSelected(Constant.Runtime.DevMode[0]);
        smoothLandCheckBox.setSelected(Constant.Runtime.Smooth[0]);
        //newGUICheckbox.setSelected(Gui_NewGame.preferences.newGui);

        creditsLabel.setText(" " + questData.getCredits());
        statsLabel.setText(questData.getWin() + " wins / " + questData.getLost() + " losses");
        titleLabel.setText(questData.getRank());

        //copy lastUsedDeck as removal triggers selection change. 
        String lastUsedDeck = QuestMainPanel.lastUsedDeck;
        deckComboBox.removeAllItems();

        if (questData.getDeckNames().size() > 0) {
            deckComboBox.setEnabled(true);

            List<String> deckNames = new ArrayList<String>(questData.getDeckNames());

            Collections.sort(deckNames, new Comparator<String>() {
                public int compare(String s, String s1) {
                    return s.compareToIgnoreCase(s1);
                }
            });

            if (deckNames.contains(lastUsedDeck)) {
                deckNames.remove(lastUsedDeck);
                deckNames.add(0, lastUsedDeck);
            }

            for (String deckName : deckNames) {
                deckComboBox.addItem(deckName);
            }
        } else {
            deckComboBox.addItem(NO_DECKS_AVAILABLE);
            deckComboBox.setEnabled(false);
        }
        deckComboBox.setMinimumSize(new Dimension(150, 0));

        questButton.setEnabled(nextQuestInWins() == 0);

        playButton.setEnabled(canGameBeLaunched());

        if (questData.getMode().equals(QuestData.FANTASY)) {
            lifeLabel.setText(" " + questData.getLife());

            petComboBox.removeAllItems();

            Set<String> petList = questData.getPetManager().getAvailablePetNames();

            if (petList.size() > 0) {
                petComboBox.setEnabled(true);
                petCheckBox.setEnabled(true);
                for (String aPetList : petList) {
                    petComboBox.addItem(aPetList);
                }
            } else {
                petComboBox.addItem("No pets available");
                petComboBox.setEnabled(false);
                petCheckBox.setEnabled(false);
            }

            if (!questData.getPetManager().shouldPetBeUsed()) {
                petCheckBox.setSelected(false);
                petComboBox.setEnabled(false);
            } else {
                petCheckBox.setSelected(true);
                petComboBox.setSelectedItem(questData.getPetManager().getSelectedPet().getName());
            }


            this.plantBox.setEnabled(questData.getPetManager().getPlant().getLevel() > 0);
            this.plantBox.setSelected(questData.getPetManager().shouldPlantBeUsed());

            QuestItemZeppelin zeppelin = (QuestItemZeppelin) questData.getInventory().getItem("Zeppelin");

            if (zeppelin.getLevel() > 0) {
                zeppelinPanel.removeAll();
                zeppelinPanel.add(zeppelinButton, BorderLayout.CENTER);
            }

            if (!zeppelin.hasBeenUsed()) {
                zeppelinButton.setEnabled(true);
            } else {
                zeppelinButton.setEnabled(false);
            }


        }

        if (nextQuestInWins() > 0) {
            nextQuestLabel.setText("Next Quest in " + nextQuestInWins() + " Wins.");
        } else {
            nextQuestLabel.setText("Next Quest available now.");
        }

        nextMatchLayout = new CardLayout();

        refreshNextMatchPanel();
    }

    /**
     * <p>refreshNextMatchPanel.</p>
     */
    private void refreshNextMatchPanel() {
        nextMatchPanel.removeAll();
        nextMatchLayout = new CardLayout();
        nextMatchPanel.setLayout(nextMatchLayout);
        nextMatchPanel.add(createBattlePanel(), BATTLES);
        nextMatchPanel.add(createQuestPanel(), QUESTS);
        if (isShowingQuests) {
            this.nextMatchLayout.show(nextMatchPanel, QUESTS);
        } else {
            this.nextMatchLayout.show(nextMatchPanel, BATTLES);
        }
    }

    /**
     * <p>nextQuestInWins.</p>
     *
     * @return a int.
     */
    private int nextQuestInWins() {

        // Number of wins was 25, lowereing the number to 20 to help short term questers.
        if (questData.getWin() < 20) {
            return 20 - questData.getWin();
        }

        // The int mul has been lowered by one, should face special opps more frequently.
        int questsPlayed = questData.getQuestsPlayed();
        int mul = 5;

        if (questData.getInventory().hasItem("Zeppelin")) {
            mul = 3;
        } else if (questData.getInventory().hasItem("Map")) {
            mul = 4;
        }

        int delta = (questsPlayed * mul) - questData.getWin();

        return (delta > 0) ? delta : 0;
    }


    /**
     * <p>showDeckEditor.</p>
     */
    void showDeckEditor() {
        Command exit = new Command() {
            private static final long serialVersionUID = -5110231879431074581L;

            public void execute() {
                //saves all deck data
                AllZone.getQuestData().saveData();

                new QuestFrame();
            }
        };

        Gui_Quest_DeckEditor g = new Gui_Quest_DeckEditor();

        g.show(exit);
        g.setVisible(true);
        mainFrame.dispose();
    }//deck editor button

    /**
     * <p>showBazaar.</p>
     */
    void showBazaar() {
        mainFrame.showBazaarPane();
    }

    /**
     * <p>showCardShop.</p>
     */
    void showCardShop() {
        Command exit = new Command() {
            private static final long serialVersionUID = 8567193482568076362L;

            public void execute() {
                //saves all deck data
                AllZone.getQuestData().saveData();

                new QuestFrame();
            }
        };

        Gui_CardShop g = new Gui_CardShop(questData);

        g.show(exit);
        g.setVisible(true);

        this.mainFrame.dispose();

    }//card shop button

    /**
     * <p>launchGame.</p>
     */
    private void launchGame() {

        //TODO: This is a temporary hack to see if the image cache affects the heap usage significantly.
        ImageCache.clear();

        QuestItemZeppelin zeppelin = (QuestItemZeppelin) questData.getInventory().getItem("Zeppelin");
        zeppelin.setZeppelinUsed(false);
        questData.randomizeOpponents();

        String humanDeckName = (String) deckComboBox.getSelectedItem();
        Deck humanDeck = questData.getDeck(humanDeckName);
        Constant.Runtime.HumanDeck[0] = humanDeck;
        moveDeckToTop(humanDeckName);

        Constant.Quest.oppIconName[0] = getMatchIcon();

        // Dev Mode occurs before Display
        Constant.Runtime.DevMode[0] = devModeCheckBox.isSelected();

        //DO NOT CHANGE THIS ORDER, GuiDisplay needs to be created before cards are added
        //if (newGUICheckbox.isSelected()) {
            AllZone.setDisplay(new GuiDisplay4());
        //} else {
        //    AllZone.setDisplay(new GuiDisplay3());
        //}

        //Gui_NewGame.preferences.newGui = newGUICheckbox.isSelected();

        Constant.Runtime.Smooth[0] = smoothLandCheckBox.isSelected();


        if (isShowingQuests) {
            setupQuest(humanDeck);
        } else {
            setupBattle(humanDeck);
        }

        AllZone.getQuestData().saveData();

        AllZone.getDisplay().setVisible(true);
        mainFrame.dispose();
    }


    /**
     * <p>setupBattle.</p>
     *
     * @param humanDeck a {@link forge.deck.Deck} object.
     */
    void setupBattle(Deck humanDeck) {

        Deck computer = QuestBattleManager.getAIDeckNewFormat((selectedOpponent).getName());
        Constant.Runtime.ComputerDeck[0] = computer;

        AllZone.getGameAction().newGame(humanDeck, computer, forge.quest.data.QuestUtil.getHumanPlantAndPet(questData),
                new CardList(), questData.getLife(), 20, null);
    }

    /**
     * <p>setupQuest.</p>
     *
     * @param humanDeck a {@link forge.deck.Deck} object.
     */
    private void setupQuest(Deck humanDeck) {
        Quest_Assignment selectedQuest = ((QuestQuest) selectedOpponent).getQuestAssignment();

        Deck computerDeck = QuestBattleManager.getAIDeckNewFormat("quest" + selectedQuest.getId());
        Constant.Runtime.ComputerDeck[0] = computerDeck;

        AllZone.setQuestAssignment(selectedQuest);

        int extraLife = 0;

        if (questData.getInventory().getItemLevel("Gear") == 2) {
            extraLife = 3;
        }

        AllZone.getGameAction().newGame(humanDeck, computerDeck,
                forge.quest.data.QuestUtil.getHumanPlantAndPet(questData, selectedQuest), new CardList(),
                questData.getLife() + extraLife, selectedQuest.getComputerLife(), selectedQuest);

    }

    /**
     * <p>getMatchIcon.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getMatchIcon() {
        String oppIconName;

        if (isShowingQuests) {
            Quest_Assignment selectedQuest = ((QuestQuest) selectedOpponent).getQuestAssignment();
            oppIconName = selectedQuest.getIconName();
        } else {
            oppIconName = selectedOpponent.getName();
            oppIconName = oppIconName.substring(0, oppIconName.length() - 1).trim() + ".jpg";
        }
        return oppIconName;
    }

    /**
     * <p>showQuests.</p>
     */
    void showQuests() {
        if (isShowingQuests) {
            isShowingQuests = false;
            questButton.setText("Quests");
        } else {
            isShowingQuests = true;
            questButton.setText("Battles");
        }

        if (selectedOpponent != null) {
            selectedOpponent.setSelected(false);
        }

        selectedOpponent = null;

        refresh();
    }

    class SelectionAdapter extends MouseAdapter {
        QuestSelectablePanel selectablePanel;

        SelectionAdapter(QuestSelectablePanel selectablePanel) {
            super();
            this.selectablePanel = selectablePanel;
        }

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {

            if (selectedOpponent != null) {
                selectedOpponent.setSelected(false);
            }

            selectablePanel.setSelected(true);

            selectedOpponent = selectablePanel;
            playButton.setEnabled(canGameBeLaunched());
        }

    }

    /**
     * <p>moveDeckToTop.</p>
     *
     * @param humanDeckName a {@link java.lang.String} object.
     */
    private void moveDeckToTop(String humanDeckName) {
        QuestMainPanel.lastUsedDeck = humanDeckName;
    }


    /**
     * <p>canGameBeLaunched.</p>
     *
     * @return a boolean.
     */
    boolean canGameBeLaunched() {
        return !(NO_DECKS_AVAILABLE.equals(deckComboBox.getSelectedItem()) || selectedOpponent == null);
    }

    /** {@inheritDoc} */
    @Override
    public void refreshState() {
        this.refresh();
    }

}
