package forge.screens.constructed;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.google.common.base.Predicate;

import forge.assets.FSkin;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.game.player.LobbyPlayer;
import forge.game.player.RegisteredPlayer;
import forge.game.player.LobbyPlayer.PlayerType;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.net.FServer;
import forge.screens.LaunchScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FLabel;
import forge.toolbox.FRadioButton;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.MyRandom;
import forge.utils.ForgePreferences;
import forge.utils.ForgePreferences.FPref;
import forge.utils.Utils;

public class ConstructedScreen extends LaunchScreen {
    private static final ForgePreferences prefs = FModel.getPreferences();

    private static final int MAX_PLAYERS = 8;

    // General variables
    private int activePlayersNum = 2;
    private int playerWithFocus = 0; // index of the player that currently has focus
    private PlayerPanel playerPanelWithFocus;
    private GameType currentGameMode = GameType.Constructed;
    private List<Integer> teams = new ArrayList<Integer>(MAX_PLAYERS);
    private List<Integer> archenemyTeams = new ArrayList<Integer>(MAX_PLAYERS);

    // Variants frame and variables
    private final Set<GameType> appliedVariants = new TreeSet<GameType>();
    private final FCheckBox vntVanguard = new FCheckBox("Vanguard");
    private final FCheckBox vntCommander = new FCheckBox("Commander");
    private final FCheckBox vntPlanechase = new FCheckBox("Planechase");
    private final FCheckBox vntArchenemy = new FCheckBox("Archenemy");
    private String archenemyType = "Classic";
    private final FComboBox<String> comboArchenemy = new FComboBox<String>(new String[]{
            "Archenemy (Classic - One player is the Archenemy)", "Supervillan Rumble (All players are Archenemies)"});

    private final List<PlayerPanel> playerPanels = new ArrayList<PlayerPanel>(MAX_PLAYERS);

    private final List<FLabel> closePlayerBtnList = new ArrayList<FLabel>(6);
    private final FLabel addPlayerBtn = new FLabel.ButtonBuilder().fontSize(14).text("Add a Player").build();

    private final List<PaperCard> vgdAllAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> vgdAllAiAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> nonRandomHumanAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> nonRandomAiAvatars = new ArrayList<PaperCard>();
    private Vector<Object> humanListData = new Vector<Object>();
    private Vector<Object> aiListData = new Vector<Object>();

    public ConstructedScreen() {
        super("Constructed");
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        launchParams.gameType = GameType.Constructed;

        //TODO: Allow picking decks
        Deck humanDeck = Utils.generateRandomDeck(2);
        if (humanDeck == null) { return false; }
        LobbyPlayer humanLobbyPlayer = FServer.getLobby().getGuiPlayer();
        RegisteredPlayer humanRegisteredPlayer = new RegisteredPlayer(humanDeck);
        humanRegisteredPlayer.setPlayer(humanLobbyPlayer);
        launchParams.players.add(humanRegisteredPlayer);

        Deck aiDeck = Utils.generateRandomDeck(2);
        if (aiDeck == null) { return false; }
        LobbyPlayer aiLobbyPlayer = FServer.getLobby().getAiPlayer();
        RegisteredPlayer aiRegisteredPlayer = new RegisteredPlayer(aiDeck);
        aiRegisteredPlayer.setPlayer(aiLobbyPlayer);
        launchParams.players.add(aiRegisteredPlayer);

        return true;
    }

    private class PlayerPanel extends FContainer {
        /*private final int index;

        private final FLabel nameRandomiser;
        private final FLabel avatarLabel = new FLabel.Builder().opaque(true).iconScaleFactor(0.99f).iconInBackground(true).build();
        private int avatarIndex;

        private final FTextField txtPlayerName = new FTextField("Player name");
        private FRadioButton radioHuman;
        private FRadioButton radioAi;

        private FComboBox<Object> teamComboBox = new FComboBox<Object>();
        private FComboBox<Object> aeTeamComboBox = new FComboBox<Object>();

        private final FLabel deckBtn = new FLabel.ButtonBuilder().text("Select a deck").build();
        private final FLabel deckLabel = newLabel("Deck:");

        private final String variantBtnConstraints = "height 30px, hidemode 3";

        private boolean playerIsArchenemy = false;
        private final FLabel scmDeckSelectorBtn = new FLabel.ButtonBuilder().text("Select a scheme deck").build();
        private final FLabel scmDeckEditor = new FLabel.ButtonBuilder().text("Scheme Deck Editor").build();
        private final FLabel scmLabel = newLabel("Scheme deck:");

        private final FLabel cmdDeckSelectorBtn = new FLabel.ButtonBuilder().text("Select a Commander deck").build();
        private final FLabel cmdDeckEditor = new FLabel.ButtonBuilder().text("Commander Deck Editor").build();
        private final FLabel cmdLabel = newLabel("Commander deck:");

        private final FLabel pchDeckSelectorBtn = new FLabel.ButtonBuilder().text("Select a planar deck").build();
        private final FLabel pchDeckEditor = new FLabel.ButtonBuilder().text("Planar Deck Editor").build();
        private final FLabel pchLabel = newLabel("Planar deck:");

        private final FLabel vgdSelectorBtn = new FLabel.ButtonBuilder().text("Select a Vanguard avatar").build();
        private final FLabel vgdLabel = newLabel("Vanguard:");

        public PlayerPanel(final int index) {
            super();
            index = index;
            playerIsArchenemy = index == 0;

            // Add a button to players 3+ to remove them from the setup
            if (index >= 2) {
                FLabel closeBtn = createCloseButton();
                add(closeBtn);
            }

            createAvatar();
            add(avatarLabel);

            createNameEditor();
            add(newLabel("Name:"));
            add(txtPlayerName);

            nameRandomiser = createNameRandomizer();
            add(nameRandomiser);

            createPlayerTypeOptions();
            add(radioHuman, "gapright 5px");
            add(radioAi, "wrap");

            add(newLabel("Team:"), "w 40px, h 30px");
            populateTeamsComboBoxes();
            teamComboBox.addActionListener(teamListener);
            aeTeamComboBox.addActionListener(teamListener);
            teamComboBox.addTo(this, variantBtnConstraints + ", pushx, growx, gaptop 5px");
            aeTeamComboBox.addTo(this, variantBtnConstraints + ", pushx, growx, gaptop 5px");

            add(deckLabel, variantBtnConstraints + ", cell 0 2, sx 2, ax right");
            add(deckBtn, variantBtnConstraints + ", cell 2 2, pushx, growx, wmax 100%-153px, h 30px, spanx 4, wrap");

            addHandlersDeckSelector();

            add(cmdLabel, variantBtnConstraints + ", cell 0 3, sx 2, ax right");
            add(cmdDeckSelectorBtn, variantBtnConstraints + ", cell 2 3, growx, pushx");
            add(cmdDeckEditor, variantBtnConstraints + ", cell 3 3, sx 3, growx, wrap");

            add(scmLabel, variantBtnConstraints + ", cell 0 4, sx 2, ax right");
            add(scmDeckSelectorBtn, variantBtnConstraints + ", cell 2 4, growx, pushx");
            add(scmDeckEditor, variantBtnConstraints + ", cell 3 4, sx 3, growx, wrap");

            add(pchLabel, variantBtnConstraints + ", cell 0 5, sx 2, ax right");
            add(pchDeckSelectorBtn, variantBtnConstraints + ", cell 2 5, growx, pushx");
            add(pchDeckEditor, variantBtnConstraints + ", cell 3 5, sx 3, growx, wrap");

            add(vgdLabel, variantBtnConstraints + ", cell 0 6, sx 2, ax right");
            add(vgdSelectorBtn, variantBtnConstraints + ", cell 2 6, sx 4, growx, wrap");

            addHandlersToVariantsControls();
            updateVariantControlsVisibility();

            addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftMouseDown(MouseEvent e) {
                    avatarLabel.requestFocusInWindow();
                }
            });
        }*/

        /*private final FMouseAdapter radioMouseAdapter = new FMouseAdapter() {
            @Override
            public void onLeftClick(MouseEvent e) {
                avatarLabel.requestFocusInWindow();
                updateVanguardList(index);
            }
        };

        //Listens to name text fields and gives the appropriate player focus.
        //Also saves the name preference when leaving player one's text field. */
        /*private FocusAdapter nameFocusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                changePlayerFocus(index);
            }

            @Override
            public void focusLost(FocusEvent e) {
                final Object source = e.getSource();
                if (source instanceof FTextField) { // the text box
                    FTextField nField = (FTextField)source;
                    String newName = nField.getText().trim();
                    if (index == 0 && !StringUtils.isBlank(newName)
                            && StringUtils.isAlphanumericSpace(newName) && prefs.getPref(FPref.PLAYER_NAME) != newName) {
                        prefs.setPref(FPref.PLAYER_NAME, newName);
                        prefs.save();
                    }
                }
            }
        };

        // Listens to avatar buttons and gives the appropriate player focus.
        private FocusAdapter avatarFocusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                changePlayerFocus(index);
            }
        };*/

        @Override
        protected void doLayout(float width, float height) {
            // TODO Auto-generated method stub
            
        }

        /*private FMouseAdapter avatarMouseListener = new FMouseAdapter() {
            @Override
            public void onLeftClick(MouseEvent e) {
                final FLabel avatar = (FLabel)e.getSource();

                changePlayerFocus(index);
                avatar.requestFocusInWindow();

                final AvatarSelector aSel = new AvatarSelector(getPlayerName(), avatarIndex, getUsedAvatars());
                for (final FLabel lbl : aSel.getSelectables()) {
                    lbl.setCommand(new UiCommand() {
                        @Override
                        public void run() {
                            setAvatar(Integer.valueOf(lbl.getName().substring(11)));
                            aSel.setVisible(false);
                        }
                    });
                }

                aSel.setVisible(true);
                aSel.dispose();

                if (index < 2) {
                    updateAvatarPrefs();
                }
            }
            @Override
            public void onRightClick(MouseEvent e) {
                changePlayerFocus(index);
                avatarLabel.requestFocusInWindow();

                setRandomAvatar();

                if (index < 2) {
                    updateAvatarPrefs();
                }
            }
        };

        public void updateVariantControlsVisibility() {
            // Commander deck replaces basic deck, so hide that
            deckLabel.setVisible(!appliedVariants.contains(GameType.Commander));
            deckBtn.setVisible(!appliedVariants.contains(GameType.Commander));
            cmdDeckSelectorBtn.setVisible(appliedVariants.contains(GameType.Commander));
            cmdDeckEditor.setVisible(appliedVariants.contains(GameType.Commander));
            cmdLabel.setVisible(appliedVariants.contains(GameType.Commander));

            boolean archenemyVisiblity = appliedVariants.contains(GameType.ArchenemyRumble)
                    || (appliedVariants.contains(GameType.Archenemy) && playerIsArchenemy);
            scmDeckSelectorBtn.setVisible(archenemyVisiblity);
            scmDeckEditor.setVisible(archenemyVisiblity);
            scmLabel.setVisible(archenemyVisiblity);

            teamComboBox.setVisible(!appliedVariants.contains(GameType.Archenemy));
            aeTeamComboBox.setVisible(appliedVariants.contains(GameType.Archenemy));
            aeTeamComboBox.setEnabled(!(appliedVariants.contains(GameType.Archenemy) && playerIsArchenemy));

            pchDeckSelectorBtn.setVisible(appliedVariants.contains(GameType.Planechase));
            pchDeckEditor.setVisible(appliedVariants.contains(GameType.Planechase));
            pchLabel.setVisible(appliedVariants.contains(GameType.Planechase));

            vgdSelectorBtn.setVisible(appliedVariants.contains(GameType.Vanguard));
            vgdLabel.setVisible(appliedVariants.contains(GameType.Vanguard));
        }

        public PlayerType getPlayerType() {
            return radioAi.isSelected() ? PlayerType.COMPUTER : PlayerType.HUMAN;
        }

        public void setVanguardButtonText(String text) {
            vgdSelectorBtn.setText(text);
        }

        public void setDeckSelectorButtonText(String text) {
            deckBtn.setText(text);
        }

        private void populateTeamsComboBoxes() {
            aeTeamComboBox.addItem("Archenemy");
            aeTeamComboBox.addItem("Heroes");
            aeTeamComboBox.setSelectedIndex(archenemyTeams.get(index) - 1);
            aeTeamComboBox.setEnabled(playerIsArchenemy);

            for (int i = 1; i <= MAX_PLAYERS; i++) {
                teamComboBox.addItem(i);
            }
            teamComboBox.setSelectedIndex(teams.get(index) - 1);
            teamComboBox.setEnabled(true);
        }

        private ActionListener teamListener = new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                FComboBox<Object> cb = (FComboBox<Object>)e.getSource();
                cb.requestFocusInWindow();
                Object selection = cb.getSelectedItem();

                if (null == selection) {
                    return;
                }
                if (appliedVariants.contains(GameType.Archenemy)) {
                    String sel = (String) selection;
                    if (sel.contains("Archenemy")) {
                        lastArchenemy = index;
                        for (PlayerPanel pp : playerPanels) {
                            int i = pp.index;
                            archenemyTeams.set(i, i == lastArchenemy ? 1 : 2);
                            pp.aeTeamComboBox.setSelectedIndex(i == lastArchenemy ? 0 : 1);
                            pp.toggleIsPlayerArchenemy();
                        }
                    }
                } else {
                    Integer sel = (Integer) selection;
                    teams.set(index, sel);
                }

                changePlayerFocus(index);
            }
        };

        public void toggleIsPlayerArchenemy() {
            if (appliedVariants.contains(GameType.Archenemy)) {
                playerIsArchenemy = lastArchenemy == index;
            } else {
                playerIsArchenemy = appliedVariants.contains(GameType.ArchenemyRumble);
            }
            updateVariantControlsVisibility();
        }

        *//**
         * @param index
         *//*
        private void addHandlersToVariantsControls() {
            // Archenemy buttons
            scmDeckSelectorBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                    currentGameMode = archenemyType.contains("Classic") ? GameType.Archenemy : GameType.ArchenemyRumble;
                    scmDeckSelectorBtn.requestFocusInWindow();
                    changePlayerFocus(index, currentGameMode);
                }
            });

            scmDeckEditor.setCommand(new UiCommand() {
                @Override
                public void run() {
                    currentGameMode = archenemyType.contains("Classic") ? GameType.Archenemy : GameType.ArchenemyRumble;
                    Predicate<PaperCard> predSchemes = new Predicate<PaperCard>() {
                        @Override
                        public boolean apply(PaperCard arg0) {
                            return arg0.getRules().getType().isScheme();
                        }
                    };

                    Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_ARCHENEMY);
                    CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                            new CEditorVariant(Singletons.getModel().getDecks().getScheme(), predSchemes, DeckSection.Schemes, FScreen.DECK_EDITOR_PLANECHASE));
                }
            });

            // Commander buttons
            cmdDeckSelectorBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                    currentGameMode = GameType.Commander;
                    cmdDeckSelectorBtn.requestFocusInWindow();
                    changePlayerFocus(index, currentGameMode);
                }
            });

            cmdDeckEditor.setCommand(new UiCommand() {
                @Override
                public void run() {
                    currentGameMode = GameType.Commander;
                    Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_COMMANDER);
                    CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorCommander());
                }
            });

            // Planechase buttons
            pchDeckSelectorBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                    currentGameMode = GameType.Planechase;
                    pchDeckSelectorBtn.requestFocusInWindow();
                    changePlayerFocus(index, GameType.Planechase);
                }
            });

            pchDeckEditor.setCommand(new UiCommand() {
                @Override
                public void run() {
                    currentGameMode = GameType.Planechase;
                    Predicate<PaperCard> predPlanes = new Predicate<PaperCard>() {
                        @Override
                        public boolean apply(PaperCard arg0) {
                            return arg0.getRules().getType().isPlane() || arg0.getRules().getType().isPhenomenon();
                        }
                    };

                    Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_PLANECHASE);
                    CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                            new CEditorVariant(Singletons.getModel().getDecks().getPlane(), predPlanes, DeckSection.Planes, FScreen.DECK_EDITOR_PLANECHASE));
                }
            });

            // Vanguard buttons
            vgdSelectorBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                    currentGameMode = GameType.Vanguard;
                    vgdSelectorBtn.requestFocusInWindow();
                    changePlayerFocus(index, GameType.Vanguard);
                }
            });
        }

        /**
         * @param index
         *//*
        private void createPlayerTypeOptions() {
            radioHuman = new FRadioButton("Human", index == 0);
            radioAi = new FRadioButton("AI", index != 0);

            radioHuman.addMouseListener(radioMouseAdapter);
            radioAi.addMouseListener(radioMouseAdapter);

            ButtonGroup tempBtnGroup = new ButtonGroup();
            tempBtnGroup.add(radioHuman);
            tempBtnGroup.add(radioAi);
        }

        *//**
         * @param index
         *//*
        private void addHandlersDeckSelector() {
            deckBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                    currentGameMode = GameType.Constructed;
                    deckBtn.requestFocusInWindow();
                    changePlayerFocus(index, GameType.Constructed);
                }
            });
        }

        *//**
         * @param index
         * @return
         *//*
        private FLabel createNameRandomizer() {
            final FLabel newNameBtn = new FLabel.Builder().tooltip("Get a new random name").iconInBackground(false)
                    .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT)).hoverable(true).opaque(false)
                    .unhoveredAlpha(0.9f).build();
            newNameBtn.setCommand(new UiCommand() {
                @Override
                public void run() {
                    String newName = getNewName();
                    if (null == newName) {
                        return;
                    }
                    txtPlayerName.setText(newName);

                    if (index == 0) {
                        prefs.setPref(FPref.PLAYER_NAME, newName);
                        prefs.save();
                    }
                    txtPlayerName.requestFocus();
                    changePlayerFocus(index);
                }
            });
            newNameBtn.addFocusListener(nameFocusListener);
            return newNameBtn;
        }

        *//**
         * @param index
         * @return
         *//*
        private void createNameEditor() {
            String name;
            if (index == 0) {
                name = Singletons.getModel().getPreferences().getPref(FPref.PLAYER_NAME);
                if (name.isEmpty()) {
                    name = "Human";
                }
            }
            else {
                name = NameGenerator.getRandomName("Any", "Any", getPlayerNames());
            }

            txtPlayerName.setText(name);
            txtPlayerName.setFocusable(true);
            txtPlayerName.setFont(FSkin.getFont(14));
            txtPlayerName.addActionListener(nameListener);
            txtPlayerName.addFocusListener(nameFocusListener);
        }

        private FLabel createCloseButton() {
            final FLabel closeBtn = new FLabel.Builder().iconInBackground(false)
                    .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_CLOSE)).hoverable(true).build();
            closeBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                    removePlayer(closePlayerBtnList.indexOf(closeBtn) + 2);
                }
            });
            closePlayerBtnList.add(closeBtn);
            return closeBtn;
        }

        private void createAvatar() {
            String[] currentPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
            if (index < currentPrefs.length) {
                avatarIndex = Integer.parseInt(currentPrefs[index]);
                avatarLabel.setIcon(FSkin.getAvatars().get(avatarIndex));
            }
            else {
                setRandomAvatar();
            }

            avatarLabel.setToolTipText("L-click: Select avatar. R-click: Randomize avatar.");
            avatarLabel.addFocusListener(avatarFocusListener);
            avatarLabel.addMouseListener(avatarMouseListener);
        }

        *//** Applies a random avatar, avoiding avatars already used.
         * @param playerIndex *//*
        public void setRandomAvatar() {
            int random = 0;

            List<Integer> usedAvatars = getUsedAvatars();
            do {
                random = MyRandom.getRandom().nextInt(FSkin.getAvatars().size());
            } while (usedAvatars.contains(random));
            setAvatar(random);
        }

        public void setAvatar(int newAvatarIndex) {
            avatarIndex = newAvatarIndex;
            TextureRegion icon = FSkin.getAvatars().get(newAvatarIndex);
            avatarLabel.setIcon(icon);
            avatarLabel.repaintSelf();
        }

        private final FSkin.LineSkinBorder focusedBorder = new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS).alphaColor(255), 3);
        private final FSkin.LineSkinBorder defaultBorder = new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_THEME).alphaColor(200), 2);

        public int getAvatarIndex() {
            return avatarIndex;
        }

        public void setPlayerName(String string) {
            txtPlayerName.setText(string);
        }

        public String getPlayerName() {
            return txtPlayerName.getText();
        }*/
    }
}
