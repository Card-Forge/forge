/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.quest.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.GuiDisplay;
import forge.ImageCache;
import forge.control.ControlAllUI;
import forge.deck.Deck;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.DeckEditorQuest;
import forge.gui.deckeditor.DeckEditorShop;
import forge.quest.data.QuestData;
import forge.quest.data.QuestUtil;
import forge.quest.data.item.QuestItemZeppelin;
import forge.quest.gui.main.QuestChallenge;
import forge.quest.gui.main.QuestChallengePanel;
import forge.quest.gui.main.QuestDuel;
import forge.quest.gui.main.QuestDuelPanel;
import forge.quest.gui.main.QuestEventManager;
import forge.quest.gui.main.QuestSelectablePanel;

/**
 * <p>
 * QuestMainPanel class.
 * </p>
 * VIEW - lays out swing components for duel and challenge events.
 * 
 * @author Forge
 * @version $Id: QuestMainPanel.java 10358 2011-09-11 05:20:13Z Doublestrike $
 */
public class QuestMainPanel extends QuestAbstractPanel {
    /** Constant <code>serialVersionUID=6142934729724012402L</code>. */
    private static final long serialVersionUID = 6142934729724012402L;

    private final forge.quest.data.QuestData questData;
    private forge.quest.gui.main.QuestEventManager qem;

    /** The credits label. */
    private final JLabel creditsLabel = new JLabel();

    /** The life label. */
    private final JLabel lifeLabel = new JLabel();

    /** The stats label. */
    private final JLabel statsLabel = new JLabel();

    /** The title label. */
    private final JLabel titleLabel = new JLabel();

    /** The next quest label. */
    private final JLabel nextQuestLabel = new JLabel();

    /** The pet combo box. */
    private final JComboBox petComboBox = new JComboBox();

    /** The deck combo box. */
    private final JComboBox deckComboBox = new JComboBox();

    /** The event button. */
    private final JButton eventButton = new JButton("Challenges");

    /** The play button. */
    private final JButton playButton = new JButton("Play");

    private QuestSelectablePanel selectedOpponent;

    /** The next match panel. */
    private final JPanel nextMatchPanel = new JPanel();

    /** The next match layout. */
    private CardLayout nextMatchLayout;

    /** The is showing challenges. */
    private boolean isShowingChallenges = false;
    private final JCheckBox devModeCheckBox = new JCheckBox("Developer Mode");
    // private JCheckBox newGUICheckbox = new JCheckBox("Use new UI", true);
    private final JCheckBox smoothLandCheckBox = new JCheckBox("Adjust AI Land");
    private final JCheckBox petCheckBox = new JCheckBox("Summon Pet");

    private final JCheckBox plantBox = new JCheckBox("Summon Plant");
    /** Constant <code>NO_DECKS_AVAILABLE="No decks available"</code>. */
    private static final String NO_DECKS_AVAILABLE = "No decks available";
    /** Constant <code>DUELS="Duels"</code>. */
    private static final String DUELS = "Duels";
    /** Constant <code>CHALLENGES="Challenges"</code>. */
    private static final String CHALLENGES = "Challenges";

    // TODO: Make this ordering permanent
    /**
     * Constant <code>lastUsedDeck="//TODO: Make this ordering permanent"</code>
     * .
     */
    private static String lastUsedDeck;
    private final JButton zeppelinButton = new JButton("<html>Launch<br>Zeppelin</html>", GuiUtils.getResizedIcon(
            GuiUtils.getIconFromFile("ZeppelinIcon.png"), 40, 40));
    private final JPanel zeppelinPanel = new JPanel();

    /**
     * <p>
     * Constructor for QuestMainPanel.
     * </p>
     * 
     * @param mainFrame
     *            a {@link forge.quest.gui.QuestFrame} object.
     */
    public QuestMainPanel(final QuestFrame mainFrame) {
        super(mainFrame);
        this.questData = AllZone.getQuestData();
        this.qem = AllZone.getQuestEventManager();

        // QuestEventManager is the MODEL for this VIEW.
        // All quest events are generated here, the first time the VIEW is made.
        if (this.qem == null) {
            this.qem = new QuestEventManager();
            this.qem.assembleAllEvents();
            AllZone.setQuestEventManager(this.qem);
        }

        this.initUI();
    }

    /**
     * <p>
     * initUI.
     * </p>
     */
    private void initUI() {
        this.refresh();
        this.setLayout(new BorderLayout(5, 5));
        final JPanel centerPanel = new JPanel(new BorderLayout());
        this.add(centerPanel, BorderLayout.CENTER);

        final JPanel northPanel = this.createStatusPanel();
        this.add(northPanel, BorderLayout.NORTH);

        final JPanel eastPanel = this.createSidePanel();
        this.add(eastPanel, BorderLayout.EAST);

        final JPanel matchSettingsPanel = this.createMatchSettingsPanel();
        centerPanel.add(matchSettingsPanel, BorderLayout.SOUTH);

        centerPanel.add(this.nextMatchPanel, BorderLayout.CENTER);
        this.setBorder(new EmptyBorder(5, 5, 5, 5));

    }

    /**
     * <p>
     * createStatusPanel.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createStatusPanel() {
        final JPanel northPanel = new JPanel();
        JLabel modeLabel;
        JLabel difficultyLabel; // Create labels at the top
        this.titleLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 28));
        this.titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(this.titleLabel);

        northPanel.add(Box.createVerticalStrut(5));

        final JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        modeLabel = new JLabel(this.questData.getMode());
        statusPanel.add(modeLabel);
        statusPanel.add(Box.createHorizontalGlue());

        difficultyLabel = new JLabel(this.questData.getDifficulty());
        statusPanel.add(difficultyLabel);
        statusPanel.add(Box.createHorizontalGlue());

        statusPanel.add(this.statsLabel);

        northPanel.add(statusPanel);
        return northPanel;
    }

    /**
     * <p>
     * createSidePanel.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createSidePanel() {
        final JPanel panel = new JPanel();
        JPanel optionsPanel; // Create options checkbox list
        optionsPanel = this.createOptionsPanel();

        final List<Component> eastComponents = new ArrayList<Component>();
        // Create buttons

        final JButton mainMenuButton = new JButton("Return to Main Menu");
        mainMenuButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                QuestMainPanel.this.getMainFrame().returnToMainMenu();
            }
        });
        eastComponents.add(mainMenuButton);

        final JButton cardShopButton = new JButton("Card Shop");
        cardShopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                QuestMainPanel.this.showCardShop();
            }
        });
        eastComponents.add(cardShopButton);
        cardShopButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));

        JButton bazaarButton = null;
        if (this.questData.getMode().equals(forge.quest.data.QuestData.FANTASY)) {

            bazaarButton = new JButton("Bazaar");
            bazaarButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    QuestMainPanel.this.showBazaar();
                }
            });
            eastComponents.add(bazaarButton);
            bazaarButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        }

        this.eventButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                QuestMainPanel.this.showChallenges();
            }
        });
        eastComponents.add(this.eventButton);
        this.eventButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        this.eventButton.setPreferredSize(new Dimension(0, 60));

        this.playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                QuestMainPanel.this.launchGame();
            }
        });

        this.playButton.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
        this.playButton.setPreferredSize(new Dimension(0, 100));

        eastComponents.add(this.playButton);
        eastComponents.add(optionsPanel);

        GuiUtils.setWidthToMax(eastComponents);

        panel.add(mainMenuButton);
        GuiUtils.addGap(panel);
        panel.add(optionsPanel);
        panel.add(Box.createVerticalGlue());
        panel.add(Box.createVerticalGlue());

        if (this.questData.getMode().equals(forge.quest.data.QuestData.FANTASY)) {
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

        if (this.questData.getMode().equals(forge.quest.data.QuestData.FANTASY)) {
            GuiUtils.addGap(panel);
            panel.add(bazaarButton);
        }

        panel.add(Box.createVerticalGlue());

        panel.add(this.eventButton);
        this.nextQuestLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        panel.add(this.nextQuestLabel);
        GuiUtils.addGap(panel);

        panel.add(this.playButton);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /**
     * <p>
     * createOptionsPanel.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createOptionsPanel() {
        JPanel optionsPanel;
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));

        // optionsPanel.add(this.newGUICheckbox);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(this.smoothLandCheckBox);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(this.devModeCheckBox);
        optionsPanel.setBorder(new TitledBorder(new EtchedBorder(), "Options"));
        return optionsPanel;
    }

    /**
     * <p>
     * createMatchSettingsPanel.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createMatchSettingsPanel() {

        final JPanel matchPanel = new JPanel();
        matchPanel.setLayout(new BoxLayout(matchPanel, BoxLayout.Y_AXIS));

        final JPanel deckPanel = new JPanel();
        deckPanel.setLayout(new BoxLayout(deckPanel, BoxLayout.X_AXIS));

        final JLabel deckLabel = new JLabel("Use Deck");
        deckPanel.add(deckLabel);
        GuiUtils.addGap(deckPanel);

        this.deckComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                QuestMainPanel.this.playButton.setEnabled(QuestMainPanel.this.canGameBeLaunched());
                QuestMainPanel.lastUsedDeck = (String) QuestMainPanel.this.deckComboBox.getSelectedItem();
            }
        });

        deckPanel.add(this.deckComboBox);
        GuiUtils.addGap(deckPanel);

        final JButton editDeckButton = new JButton("Deck Editor");
        editDeckButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                QuestMainPanel.this.showDeckEditor();
            }
        });
        deckPanel.add(editDeckButton);
        deckPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, deckPanel.getPreferredSize().height));
        deckPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        matchPanel.add(deckPanel);

        GuiUtils.addGap(matchPanel);

        if (this.questData.getMode().equals(forge.quest.data.QuestData.FANTASY)) {
            final JPanel fantasyPanel = new JPanel();
            fantasyPanel.setLayout(new BorderLayout());

            final JPanel petPanel = new JPanel();
            petPanel.setLayout(new BoxLayout(petPanel, BoxLayout.X_AXIS));

            this.petCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    if (QuestMainPanel.this.petCheckBox.isSelected()) {
                        QuestMainPanel.this.questData.getPetManager().setSelectedPet(
                                (String) QuestMainPanel.this.petComboBox.getSelectedItem());
                    } else {
                        QuestMainPanel.this.questData.getPetManager().setSelectedPet(null);
                    }

                    QuestMainPanel.this.petComboBox.setEnabled(QuestMainPanel.this.petCheckBox.isSelected());
                }
            });

            petPanel.add(this.petCheckBox);
            GuiUtils.addGap(petPanel);
            this.petComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    if (QuestMainPanel.this.petCheckBox.isSelected()) {
                        QuestMainPanel.this.questData.getPetManager().setSelectedPet(
                                (String) QuestMainPanel.this.petComboBox.getSelectedItem());
                    } else {
                        QuestMainPanel.this.questData.getPetManager().setSelectedPet(null);
                    }
                }
            });
            this.petComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) this.petCheckBox.getPreferredSize()
                    .getHeight()));
            petPanel.add(this.petComboBox);

            this.plantBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    QuestMainPanel.this.questData.getPetManager()
                            .setUsePlant(QuestMainPanel.this.plantBox.isSelected());
                }
            });

            GuiUtils.addGap(petPanel, 10);
            petPanel.add(this.plantBox);
            petPanel.setMaximumSize(petPanel.getPreferredSize());
            petPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            fantasyPanel.add(petPanel, BorderLayout.WEST);

            this.zeppelinButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    QuestMainPanel.this.questData.randomizeOpponents();
                    QuestMainPanel.this.refreshNextMatchPanel();
                    final QuestItemZeppelin zeppelin = (QuestItemZeppelin) QuestMainPanel.this.questData.getInventory()
                            .getItem("Zeppelin");
                    zeppelin.setZeppelinUsed(true);
                    QuestMainPanel.this.zeppelinButton.setEnabled(false);
                }
            });

            this.zeppelinButton.setMaximumSize(this.zeppelinButton.getPreferredSize());
            this.zeppelinPanel.setLayout(new BorderLayout());

            fantasyPanel.add(this.zeppelinPanel, BorderLayout.EAST);
            fantasyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            matchPanel.add(fantasyPanel);
        }
        return matchPanel;
    }

    /**
     * <p>
     * createDuelPanel.
     * </p>
     * Makes a parent panel, then selectable panel instances for all available
     * duels.
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createDuelPanel() {
        final JPanel duelPanel = new JPanel();
        QuestDuelPanel duelEvent;
        duelPanel.setLayout(new BoxLayout(duelPanel, BoxLayout.Y_AXIS));
        duelPanel.setBorder(new TitledBorder(new EtchedBorder(), "Available Duels"));

        final List<QuestDuel> duels = this.qem.generateDuels();

        for (final QuestDuel qd : duels) {
            duelEvent = new QuestDuelPanel(qd);
            duelPanel.add(duelEvent);
            duelEvent.addMouseListener(new SelectionAdapter(duelEvent));

            GuiUtils.addGap(duelPanel, 3);
        }

        duelPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        return duelPanel;
    }

    /**
     * <p>
     * createChallengePanel.
     * </p>
     * Makes a parent panel, then selectable panel instances for all available
     * challenges.
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel createChallengePanel() {
        final JPanel challengePanel = new JPanel();

        QuestSelectablePanel selpan;
        challengePanel.setLayout(new BoxLayout(challengePanel, BoxLayout.Y_AXIS));
        challengePanel.setBorder(new TitledBorder(new EtchedBorder(), "Available Challenges"));

        final List<QuestChallenge> challenges = this.qem.generateChallenges();

        for (final QuestChallenge qc : challenges) {
            selpan = new QuestChallengePanel(qc);
            challengePanel.add(selpan);
            selpan.addMouseListener(new SelectionAdapter(selpan));

            GuiUtils.addGap(challengePanel, 3);
        }

        return challengePanel;
    }

    /**
     * <p>
     * refresh.
     * </p>
     */
    final void refresh() {
        AllZone.getQuestData().saveData();

        this.devModeCheckBox.setSelected(Constant.Runtime.DEV_MODE[0]);
        this.smoothLandCheckBox.setSelected(Constant.Runtime.SMOOTH[0]);

        this.creditsLabel.setText(" " + this.questData.getCredits());
        this.statsLabel.setText(this.questData.getWin() + " wins / " + this.questData.getLost() + " losses");
        this.titleLabel.setText(this.questData.getRank());

        // copy lastUsedDeck as removal triggers selection change.
        final String lastUsedDeck = QuestMainPanel.lastUsedDeck;
        this.deckComboBox.removeAllItems();

        if (this.questData.getDeckNames().size() > 0) {
            this.deckComboBox.setEnabled(true);

            final List<String> deckNames = new ArrayList<String>(this.questData.getDeckNames());

            Collections.sort(deckNames, new Comparator<String>() {
                @Override
                public int compare(final String s, final String s1) {
                    return s.compareToIgnoreCase(s1);
                }
            });

            if (deckNames.contains(lastUsedDeck)) {
                deckNames.remove(lastUsedDeck);
                deckNames.add(0, lastUsedDeck);
            }

            for (final String deckName : deckNames) {
                this.deckComboBox.addItem(deckName);
            }
        } else {
            this.deckComboBox.addItem(QuestMainPanel.NO_DECKS_AVAILABLE);
            this.deckComboBox.setEnabled(false);
        }
        this.deckComboBox.setMinimumSize(new Dimension(150, 0));

        this.eventButton.setEnabled(this.nextChallengeInWins() == 0);

        this.playButton.setEnabled(this.canGameBeLaunched());

        if (this.questData.getMode().equals(QuestData.FANTASY)) {
            this.lifeLabel.setText(" " + this.questData.getLife());

            this.petComboBox.removeAllItems();

            final Set<String> petList = this.questData.getPetManager().getAvailablePetNames();

            if (petList.size() > 0) {
                this.petComboBox.setEnabled(true);
                this.petCheckBox.setEnabled(true);
                for (final String aPetList : petList) {
                    this.petComboBox.addItem(aPetList);
                }
            } else {
                this.petComboBox.addItem("No pets available");
                this.petComboBox.setEnabled(false);
                this.petCheckBox.setEnabled(false);
            }

            if (!this.questData.getPetManager().shouldPetBeUsed()) {
                this.petCheckBox.setSelected(false);
                this.petComboBox.setEnabled(false);
            } else {
                this.petCheckBox.setSelected(true);
                this.petComboBox.setSelectedItem(this.questData.getPetManager().getSelectedPet().getName());
            }

            this.plantBox.setEnabled(this.questData.getPetManager().getPlant().getLevel() > 0);
            this.plantBox.setSelected(this.questData.getPetManager().shouldPlantBeUsed());

            final QuestItemZeppelin zeppelin = (QuestItemZeppelin) this.questData.getInventory().getItem("Zeppelin");

            if (zeppelin.getLevel() > 0) {
                this.zeppelinPanel.removeAll();
                this.zeppelinPanel.add(this.zeppelinButton, BorderLayout.CENTER);
            }

            if (!zeppelin.hasBeenUsed()) {
                this.zeppelinButton.setEnabled(true);
            } else {
                this.zeppelinButton.setEnabled(false);
            }

        }

        if (this.nextChallengeInWins() > 0) {
            this.nextQuestLabel.setText("Next challenge in " + this.nextChallengeInWins() + " Wins.");
        } else {
            this.nextQuestLabel.setText("Next challenge available now.");
        }

        this.nextMatchLayout = new CardLayout();

        this.refreshNextMatchPanel();
    }

    /**
     * <p>
     * refreshNextMatchPanel.
     * </p>
     */
    private void refreshNextMatchPanel() {
        this.nextMatchPanel.removeAll();
        this.nextMatchLayout = new CardLayout();
        this.nextMatchPanel.setLayout(this.nextMatchLayout);
        this.nextMatchPanel.add(this.createDuelPanel(), QuestMainPanel.DUELS);
        this.nextMatchPanel.add(this.createChallengePanel(), QuestMainPanel.CHALLENGES);
        if (this.isShowingChallenges) {
            this.nextMatchLayout.show(this.nextMatchPanel, QuestMainPanel.CHALLENGES);
        } else {
            this.nextMatchLayout.show(this.nextMatchPanel, QuestMainPanel.DUELS);
        }
    }

    /**
     * <p>
     * nextChallengeInWins.
     * </p>
     * 
     * @return a int.
     */
    private int nextChallengeInWins() {

        // Number of wins was 25, lowereing the number to 20 to help short term
        // questers.
        if (this.questData.getWin() < 20) {
            return 20 - this.questData.getWin();
        }

        // The int mul has been lowered by one, should face special opps more
        // frequently.
        final int challengesPlayed = this.questData.getChallengesPlayed();
        int mul = 5;

        if (this.questData.getInventory().hasItem("Zeppelin")) {
            mul = 3;
        } else if (this.questData.getInventory().hasItem("Map")) {
            mul = 4;
        }

        final int delta = (challengesPlayed * mul) - this.questData.getWin();

        return (delta > 0) ? delta : 0;
    }

    /**
     * <p>
     * showDeckEditor.
     * </p>
     */
    final void showDeckEditor() {
        final Command exit = new Command() {
            private static final long serialVersionUID = -5110231879431074581L;

            @Override
            public void execute() {
                // saves all deck data
                AllZone.getQuestData().saveData();

                new QuestFrame();
            }
        };

        final DeckEditorQuest g = new DeckEditorQuest(AllZone.getQuestData());

        g.show(exit);
        g.setVisible(true);
        this.getMainFrame().dispose();
    } // deck editor button

    /**
     * <p>
     * showBazaar.
     * </p>
     */
    final void showBazaar() {
        this.getMainFrame().showBazaarPane();
    }

    /**
     * <p>
     * showCardShop.
     * </p>
     */
    final void showCardShop() {
        final Command exit = new Command() {
            private static final long serialVersionUID = 8567193482568076362L;

            @Override
            public void execute() {
                // saves all deck data
                AllZone.getQuestData().saveData();

                new QuestFrame();
            }
        };

        final DeckEditorShop g = new DeckEditorShop(this.questData);

        g.show(exit);
        g.setVisible(true);

        this.getMainFrame().dispose();

    } // card shop button

    /**
     * <p>
     * launchGame.
     * </p>
     */
    private void launchGame() {
        // TODO This is a temporary hack to see if the image cache affects the
        // heap usage significantly.
        ImageCache.clear();

        final QuestItemZeppelin zeppelin = (QuestItemZeppelin) this.questData.getInventory().getItem("Zeppelin");
        zeppelin.setZeppelinUsed(false);
        this.questData.randomizeOpponents();

        final String humanDeckName = (String) this.deckComboBox.getSelectedItem();

        final Deck humanDeck = this.questData.getDeck(humanDeckName);

        Constant.Runtime.HUMAN_DECK[0] = humanDeck;
        this.moveDeckToTop(humanDeckName);

        Constant.Quest.OPP_ICON_NAME[0] = this.getEventIconFilename();

        // Dev Mode occurs before Display
        Constant.Runtime.DEV_MODE[0] = this.devModeCheckBox.isSelected();

        if (Constant.Runtime.OLDGUI[0]) {
            AllZone.setDisplay(new GuiDisplay());
        } else {
            final ControlAllUI ui = new ControlAllUI();
            AllZone.setDisplay(ui.getMatchView());
            ui.getMatchController().initMatch();
        }

        Constant.Runtime.SMOOTH[0] = this.smoothLandCheckBox.isSelected();

        AllZone.getMatchState().reset();
        if (this.isShowingChallenges) {
            this.setupChallenge(humanDeck);
        } else {
            this.setupDuel(humanDeck);
        }

        AllZone.getQuestData().saveData();

        AllZone.getDisplay().setVisible(true);
        this.getMainFrame().dispose();
    }

    /**
     * <p>
     * setupDuel.
     * </p>
     * 
     * @param humanDeck
     *            a {@link forge.deck.Deck} object.
     */
    final void setupDuel(final Deck humanDeck) {
        final Deck computer = this.selectedOpponent.getEvent().getEventDeck();
        Constant.Runtime.COMPUTER_DECK[0] = computer;

        final QuestDuel selectedDuel = (QuestDuel) this.selectedOpponent.getEvent();
        AllZone.setQuestEvent(selectedDuel);

        AllZone.getGameAction().newGame(humanDeck, computer, QuestUtil.getHumanStartingCards(this.questData),
                QuestUtil.getComputerStartingCards(this.questData), this.questData.getLife(), 20, null);
    }

    /**
     * <p>
     * setupChallenge.
     * </p>
     * 
     * @param humanDeck
     *            a {@link forge.deck.Deck} object.
     */
    private void setupChallenge(final Deck humanDeck) {
        final QuestChallenge selectedChallenge = (QuestChallenge) this.selectedOpponent.getEvent();

        final Deck computer = this.selectedOpponent.getEvent().getEventDeck();
        Constant.Runtime.COMPUTER_DECK[0] = computer;

        AllZone.setQuestEvent(selectedChallenge);

        int extraLife = 0;

        if (this.questData.getInventory().getItemLevel("Gear") == 2) {
            extraLife = 3;
        }

        AllZone.getGameAction().newGame(humanDeck, computer,
                QuestUtil.getHumanStartingCards(this.questData, selectedChallenge),
                QuestUtil.getComputerStartingCards(this.questData, selectedChallenge),
                this.questData.getLife() + extraLife, selectedChallenge.getAILife(), selectedChallenge);

    }

    /**
     * <p>
     * getEventIconFilename.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    private String getEventIconFilename() {
        return this.selectedOpponent.getIconFilename();
    }

    /**
     * <p>
     * showChallenges.
     * </p>
     */
    final void showChallenges() {
        if (this.isShowingChallenges) {
            this.isShowingChallenges = false;
            this.eventButton.setText("Challenges");
        } else {
            this.isShowingChallenges = true;
            this.eventButton.setText("Duels");
        }

        if (this.selectedOpponent != null) {
            this.selectedOpponent.setSelected(false);
        }

        this.selectedOpponent = null;

        this.refresh();
    }

    /**
     * The Class SelectionAdapter.
     */
    class SelectionAdapter extends MouseAdapter {

        /** The selectable panel. */
        private final QuestSelectablePanel selectablePanel;

        /**
         * Instantiates a new selection adapter.
         * 
         * @param selectablePanel
         *            the selectable panel
         */
        SelectionAdapter(final QuestSelectablePanel selectablePanel) {
            super();
            this.selectablePanel = selectablePanel;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
         */
        @Override
        public void mouseClicked(final MouseEvent mouseEvent) {

            if (QuestMainPanel.this.selectedOpponent != null) {
                QuestMainPanel.this.selectedOpponent.setSelected(false);
            }

            this.selectablePanel.setSelected(true);

            QuestMainPanel.this.selectedOpponent = this.selectablePanel;
            QuestMainPanel.this.playButton.setEnabled(QuestMainPanel.this.canGameBeLaunched());
        }

    }

    /**
     * <p>
     * moveDeckToTop.
     * </p>
     * 
     * @param humanDeckName
     *            a {@link java.lang.String} object.
     */
    private void moveDeckToTop(final String humanDeckName) {
        QuestMainPanel.lastUsedDeck = humanDeckName;
    }

    /**
     * <p>
     * canGameBeLaunched.
     * </p>
     * 
     * @return a boolean.
     */
    final boolean canGameBeLaunched() {
        return !(QuestMainPanel.NO_DECKS_AVAILABLE.equals(this.deckComboBox.getSelectedItem()) || (this.selectedOpponent == null));
    }

    /** {@inheritDoc} */
    @Override
    public final void refreshState() {
        this.refresh();
    }

}
