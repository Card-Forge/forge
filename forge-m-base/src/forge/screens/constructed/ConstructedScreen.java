package forge.screens.constructed;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.base.Predicate;

import forge.Forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.deck.Deck;
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
import forge.toolbox.FEvent;
import forge.toolbox.FToggleSwitch;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.MyRandom;
import forge.util.NameGenerator;
import forge.utils.ForgePreferences;
import forge.utils.ForgePreferences.FPref;
import forge.utils.Utils;

public class ConstructedScreen extends LaunchScreen {
    private static final FSkinColor PLAYER_BORDER_COLOR = FSkinColor.get(Colors.CLR_THEME).alphaColor(0.8f);
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final float PADDING = 5;
    private static final int MAX_PLAYERS = 8;

    // General variables
    private int activePlayersNum = 2;
    private int lastArchenemy = 0;
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
    private final FScrollPane playersScroll = new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float y = 0;
            float height;
            for (int i = 0; i < activePlayersNum; i++) {
                height = playerPanels.get(i).getPreferredHeight();
                playerPanels.get(i).setBounds(0, y, visibleWidth, height);
                y += height;
            }
            return new ScrollBounds(visibleWidth, visibleHeight);
        }
    };

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
        
        /*lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        ////////////////////////////////////////////////////////
        //////////////////// Variants Panel ////////////////////

        // Populate and add variants panel
        vntVanguard.addItemListener(iListenerVariants);
        vntCommander.addItemListener(iListenerVariants);
        vntPlanechase.addItemListener(iListenerVariants);
        vntArchenemy.addItemListener(iListenerVariants);
        comboArchenemy.setSelectedIndex(0);
        comboArchenemy.setEnabled(vntArchenemy.isSelected());
        comboArchenemy.addActionListener(aeComboListener);

        variantsPanel.setOpaque(false);
        variantsPanel.add(newLabel("Variants:"));
        variantsPanel.add(vntVanguard);
        variantsPanel.add(vntCommander);
        variantsPanel.add(vntPlanechase);
        variantsPanel.add(vntArchenemy);
        comboArchenemy.addTo(variantsPanel);

        constructedFrame.add(new FScrollPane(variantsPanel, false, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                "w 100%, h 45px!, gapbottom 10px, spanx 2, wrap");*/

        ////////////////////////////////////////////////////////
        ///////////////////// Player Panel /////////////////////

        // Construct individual player panels
        for (int i = 0; i < MAX_PLAYERS; i++) {
            teams.add(i + 1);
            archenemyTeams.add(i == 0 ? 1 : 2);

            PlayerPanel player = new PlayerPanel(i);
            playerPanels.add(player);

            // Populate players panel
            player.setVisible(i < activePlayersNum);

            playersScroll.add(player);
        }

        add(playersScroll);

        addPlayerBtn.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                addPlayer();
            }
        });
        add(addPlayerBtn);
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        playersScroll.setBounds(0, startY, width, height - startY);
    }
    
    private void addPlayer() {
        if (activePlayersNum >= MAX_PLAYERS) {
            return;
        }

        int freeIndex = -1;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (!playerPanels.get(i).isVisible()) {
                freeIndex = i;
                break;
            }
        }

        playerPanels.get(freeIndex).setVisible(true);

        activePlayersNum++;
        addPlayerBtn.setEnabled(activePlayersNum < MAX_PLAYERS);

        playerPanels.get(freeIndex).setVisible(true);
    }

    private void removePlayer(int playerIndex) {
        activePlayersNum--;
        PlayerPanel player = playerPanels.get(playerIndex);
        player.setVisible(false);
        addPlayerBtn.setEnabled(true);
    }

    public boolean isPlayerAI(int playernum) {
        return playerPanels.get(playernum).getPlayerType() == PlayerType.COMPUTER;
    }

    public int getNumPlayers() {
        return activePlayersNum;
    }

    public final List<Integer> getParticipants() {
        final List<Integer> participants = new ArrayList<Integer>(activePlayersNum);
        for (final PlayerPanel panel : playerPanels) {
            if (panel.isVisible()) {
                participants.add(playerPanels.indexOf(panel));
            }
        }
        return participants;
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
        private final int index;

        private final FLabel nameRandomiser;
        private final FLabel avatarLabel = new FLabel.Builder().opaque(true).iconScaleFactor(0.99f).alphaComposite(1).iconInBackground(true).build();
        private int avatarIndex;

        private final FTextField txtPlayerName = new FTextField("Player name");
        private final FToggleSwitch humanAiSwitch = new FToggleSwitch("Human", "AI");

        private FComboBox<Object> teamComboBox = new FComboBox<Object>();
        private FComboBox<Object> aeTeamComboBox = new FComboBox<Object>();

        private final FLabel deckBtn = new FLabel.ButtonBuilder().text("Select a deck").build();
        private final FLabel deckLabel = newLabel("Deck:");

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

        public PlayerPanel(final int index0) {
            super();
            index = index0;
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

            humanAiSwitch.setToggled(index == 0);
            humanAiSwitch.setChangedHandler(humanAiSwitched);
            add(humanAiSwitch);

            add(newLabel("Team:"));
            populateTeamsComboBoxes();
            teamComboBox.setChangedHandler(teamChangedHandler);
            aeTeamComboBox.setChangedHandler(teamChangedHandler);
            add(teamComboBox);
            add(aeTeamComboBox);

            add(deckLabel);
            add(deckBtn);

            addHandlersDeckSelector();

            add(cmdLabel);
            add(cmdDeckSelectorBtn);
            add(cmdDeckEditor);

            add(scmLabel);
            add(scmDeckSelectorBtn);
            add(scmDeckEditor);

            add(pchLabel);
            add(pchDeckSelectorBtn);
            add(pchDeckEditor);

            add(vgdLabel);
            add(vgdSelectorBtn);

            addHandlersToVariantsControls();
            updateVariantControlsVisibility();
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float fieldHeight = txtPlayerName.getHeight();
            float avatarSize = 2 * fieldHeight + PADDING;

            avatarLabel.setBounds(x, y, avatarSize, avatarSize);
            x += avatarSize + PADDING;
            float w = width - x - fieldHeight - 2 * PADDING;
            txtPlayerName.setBounds(x, y, w, fieldHeight);
            x += w + PADDING;
            nameRandomiser.setBounds(x, y, fieldHeight, fieldHeight);

            y += fieldHeight + PADDING;
            humanAiSwitch.setSize(humanAiSwitch.getAutoSizeWidth(), fieldHeight);
            x = width - humanAiSwitch.getWidth();
            humanAiSwitch.setPosition(x, y);
            w = x - avatarSize - 2 * PADDING;
            x = avatarSize + 2 * PADDING;
            teamComboBox.setBounds(x, y, w, fieldHeight);

            y += fieldHeight + PADDING;
            x = PADDING;
            deckLabel.setBounds(x, y, avatarSize, fieldHeight);
            x += avatarSize + PADDING;
            w = width - x - PADDING;
            deckBtn.setBounds(x, y, w, fieldHeight);
        }

        private float getPreferredHeight() {
            int rows = 3;
            if (vntArchenemy.isSelected()) {
                rows++;
            }
            if (vntPlanechase.isSelected()) {
                rows++;
            }
            if (vntVanguard.isSelected()) {
                rows++;
            }
            return rows * (txtPlayerName.getHeight() + PADDING) + PADDING;
        }

        @Override
        protected void drawOverlay(Graphics g) {
            float y = getHeight();
            g.drawLine(1, PLAYER_BORDER_COLOR, 0, y, getWidth(), y);
        }

        private final FEventHandler humanAiSwitched = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                updateVanguardList(index);
            }
        };

        //Listens to name text fields and gives the appropriate player focus.
        //Also saves the name preference when leaving player one's text field. */
        private FEventHandler nameChangedHandler = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
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

        private FEventHandler avatarCommand = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                setRandomAvatar();

                //TODO: Support selecting avatar with option at top or bottom to select a random avatar
                
                /*final FLabel avatar = (FLabel)e.getSource();

                final AvatarSelector aSel = new AvatarSelector(getPlayerName(), avatarIndex, getUsedAvatars());
                for (final FLabel lbl : aSel.getSelectables()) {
                    lbl.setCommand(new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            setAvatar(Integer.valueOf(lbl.getName().substring(11)));
                            aSel.setVisible(false);
                        }
                    });
                }
                
                aSel.setVisible(true);
                aSel.dispose();*/

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
            return humanAiSwitch.isToggled() ? PlayerType.HUMAN : PlayerType.COMPUTER;
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
                teamComboBox.addItem("Team " + i);
            }
            teamComboBox.setSelectedIndex(teams.get(index) - 1);
            teamComboBox.setEnabled(true);
        }

        private FEventHandler teamChangedHandler = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FComboBox<Object> cb = (FComboBox<Object>)e.getSource();
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
                }
                else {
                    Integer sel = (Integer) selection;
                    teams.set(index, sel);
                }
            }
        };

        public void toggleIsPlayerArchenemy() {
            if (appliedVariants.contains(GameType.Archenemy)) {
                playerIsArchenemy = lastArchenemy == index;
            }
            else {
                playerIsArchenemy = appliedVariants.contains(GameType.ArchenemyRumble);
            }
            updateVariantControlsVisibility();
        }

        private void addHandlersToVariantsControls() {
            // Archenemy buttons
            scmDeckSelectorBtn.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    currentGameMode = archenemyType.contains("Classic") ? GameType.Archenemy : GameType.ArchenemyRumble;
                }
            });

            scmDeckEditor.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    currentGameMode = archenemyType.contains("Classic") ? GameType.Archenemy : GameType.ArchenemyRumble;
                    Predicate<PaperCard> predSchemes = new Predicate<PaperCard>() {
                        @Override
                        public boolean apply(PaperCard arg0) {
                            return arg0.getRules().getType().isScheme();
                        }
                    };

                    /*Forge.setCurrentScreen(FScreen.DECK_EDITOR_ARCHENEMY);
                    CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                            new CEditorVariant(FModel.getDecks().getScheme(), predSchemes, DeckSection.Schemes, FScreen.DECK_EDITOR_PLANECHASE));*/
                }
            });

            // Commander buttons
            cmdDeckSelectorBtn.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    currentGameMode = GameType.Commander;
                }
            });

            cmdDeckEditor.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    currentGameMode = GameType.Commander;
                    //Forge.setCurrentScreen(FScreen.DECK_EDITOR_COMMANDER);
                }
            });

            // Planechase buttons
            pchDeckSelectorBtn.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    currentGameMode = GameType.Planechase;
                }
            });

            pchDeckEditor.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    currentGameMode = GameType.Planechase;
                    /*Predicate<PaperCard> predPlanes = new Predicate<PaperCard>() {
                        @Override
                        public boolean apply(PaperCard arg0) {
                            return arg0.getRules().getType().isPlane() || arg0.getRules().getType().isPhenomenon();
                        }
                    };

                    Forge.setCurrentScreen(FScreen.DECK_EDITOR_PLANECHASE);
                    CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                            new CEditorVariant(FModel.getDecks().getPlane(), predPlanes, DeckSection.Planes, FScreen.DECK_EDITOR_PLANECHASE));*/
                }
            });

            // Vanguard buttons
            vgdSelectorBtn.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    currentGameMode = GameType.Vanguard;
                }
            });
        }

        private void addHandlersDeckSelector() {
            deckBtn.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    currentGameMode = GameType.Constructed;
                }
            });
        }

        private FLabel createNameRandomizer() {
            final FLabel newNameBtn = new FLabel.Builder().iconInBackground(false)
                    .icon(FSkinImage.EDIT).opaque(false).build();
            newNameBtn.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    String newName = getNewName();
                    if (null == newName) {
                        return;
                    }
                    txtPlayerName.setText(newName);

                    if (index == 0) {
                        prefs.setPref(FPref.PLAYER_NAME, newName);
                        prefs.save();
                    }
                }
            });
            return newNameBtn;
        }

        private void createNameEditor() {
            String name;
            if (index == 0) {
                name = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
                if (name.isEmpty()) {
                    name = "Human";
                }
            }
            else {
                name = NameGenerator.getRandomName("Any", "Any", getPlayerNames());
            }

            txtPlayerName.setText(name);
            txtPlayerName.setFontSize(14);
            txtPlayerName.setChangedHandler(nameChangedHandler);
        }

        private FLabel createCloseButton() {
            final FLabel closeBtn = new FLabel.Builder().iconInBackground(false)
                    .icon(FSkinImage.CLOSE).build();
            closeBtn.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
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
                avatarLabel.setIcon(new FTextureRegionImage(FSkin.getAvatars().get(avatarIndex)));
            }
            else {
                setRandomAvatar();
            }

            avatarLabel.setCommand(avatarCommand);
        }

        //Applies a random avatar, avoiding avatars already used.
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
            avatarLabel.setIcon(new FTextureRegionImage(FSkin.getAvatars().get(newAvatarIndex)));
        }

        public int getAvatarIndex() {
            return avatarIndex;
        }

        public void setPlayerName(String string) {
            txtPlayerName.setText(string);
        }

        public String getPlayerName() {
            return txtPlayerName.getText();
        }
    }

    /** Saves avatar prefs for players one and two. */
    private void updateAvatarPrefs() {
        int pOneIndex = playerPanels.get(0).getAvatarIndex();
        int pTwoIndex = playerPanels.get(1).getAvatarIndex();

        prefs.setPref(FPref.UI_AVATARS, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }

    /** Updates the avatars from preferences on update. */
    public void updatePlayersFromPrefs() {
        ForgePreferences prefs = FModel.getPreferences();

        // Avatar
        String[] avatarPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
        for (int i = 0; i < avatarPrefs.length; i++) {
            int avatarIndex = Integer.parseInt(avatarPrefs[i]);
            playerPanels.get(i).setAvatar(avatarIndex);
        }

        // Name
        String prefName = prefs.getPref(FPref.PLAYER_NAME);
        playerPanels.get(0).setPlayerName(StringUtils.isBlank(prefName) ? "Human" : prefName);
    }

    /** Adds a pre-styled FLabel component with the specified title. */
    private FLabel newLabel(String title) {
        return new FLabel.Builder().text(title).fontSize(14).align(HAlignment.RIGHT).build();
    }

    private List<Integer> getUsedAvatars() {
        List<Integer> usedAvatars = Arrays.asList(-1,-1,-1,-1,-1,-1,-1,-1);
        int i = 0;
        for (PlayerPanel pp : playerPanels) {
            usedAvatars.set(i++, pp.avatarIndex);
        }
        return usedAvatars;
    }

    private final String getNewName() {
        final String title = "Get new random name";
        final String message = "What type of name do you want to generate?";
        final FSkinImage icon = FOptionPane.QUESTION_ICON;
        final String[] genderOptions = new String[]{ "Male", "Female", "Any" };
        final String[] typeOptions = new String[]{ "Fantasy", "Generic", "Any" };

        final int genderIndex = FOptionPane.showOptionDialog(message, title, icon, genderOptions, 2);
        if (genderIndex < 0) {
            return null;
        }
        final int typeIndex = FOptionPane.showOptionDialog(message, title, icon, typeOptions, 2);
        if (typeIndex < 0) {
            return null;
        }

        final String gender = genderOptions[genderIndex];
        final String type = typeOptions[typeIndex];

        String confirmMsg, newName;
        List<String> usedNames = getPlayerNames();
        do {
            newName = NameGenerator.getRandomName(gender, type, usedNames);
            confirmMsg = "Would you like to use the name \"" + newName + "\", or try again?";
        } while (!FOptionPane.showConfirmDialog(confirmMsg, title, "Use this name", "Try again", true));

        return newName;
    }

    private List<String> getPlayerNames() {
        List<String> names = new ArrayList<String>();
        for (PlayerPanel pp : playerPanels) {
            names.add(pp.getPlayerName());
        }
        return names;
    }

    public String getPlayerName(int i) {
        return playerPanels.get(i).getPlayerName();
    }

    public int getPlayerAvatar(int i) {
        return playerPanels.get(i).getAvatarIndex();
    }

    public boolean isEnoughTeams() {
        int lastTeam = -1;
        final List<Integer> teamList = appliedVariants.contains(GameType.Archenemy) ? archenemyTeams : teams;

        for (final int i : getParticipants()) {
            if (lastTeam == -1) {
                lastTeam = teamList.get(i);
            }
            else if (lastTeam != teamList.get(i)) {
                return true;
            }
        }
        return false;
    }

    /////////////////////////////////////////////
    //========== Various listeners in build order

    /** This listener unlocks the relevant buttons for players
     * and enables/disables archenemy combobox as appropriate. */
    private ItemListener iListenerVariants = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent arg0) {
            FCheckBox cb = (FCheckBox) arg0.getSource();
            GameType variantType = null;

            if (cb == vntVanguard) {
                variantType = GameType.Vanguard;
            }
            else if (cb == vntCommander) {
                variantType = GameType.Commander;
            }
            else if (cb == vntPlanechase) {
                variantType = GameType.Planechase;
            }
            else if (cb == vntArchenemy) {
                variantType = archenemyType.contains("Classic") ? GameType.Archenemy : GameType.ArchenemyRumble;
                comboArchenemy.setEnabled(vntArchenemy.isSelected());
                if (arg0.getStateChange() != ItemEvent.SELECTED) {
                    appliedVariants.remove(GameType.Archenemy);
                    appliedVariants.remove(GameType.ArchenemyRumble);
                }
            }

            if (null != variantType) {
                if (arg0.getStateChange() == ItemEvent.SELECTED) {
                    appliedVariants.add(variantType);
                    currentGameMode = variantType;
                }
                else {
                    appliedVariants.remove(variantType);
                    if (currentGameMode == variantType) {
                        currentGameMode = GameType.Constructed;
                    }
                }
            }

            for (PlayerPanel pp : playerPanels) {
                pp.toggleIsPlayerArchenemy();
                pp.updateVariantControlsVisibility();
            }
        }
    };

    // Listens to the archenemy combo box
    private ActionListener aeComboListener = new ActionListener() {
        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed(ActionEvent e) {
            FComboBox<String> cb = (FComboBox<String>)e.getSource();
            archenemyType = (String)cb.getSelectedItem();
            GameType mode = archenemyType.contains("Classic") ? GameType.Archenemy : GameType.ArchenemyRumble;
            appliedVariants.remove(GameType.Archenemy);
            appliedVariants.remove(GameType.ArchenemyRumble);
            appliedVariants.add(mode);

            currentGameMode = mode;
            for (PlayerPanel pp : playerPanels) {
                pp.toggleIsPlayerArchenemy();
                pp.updateVariantControlsVisibility();
            }
        }
    };

    //This listener will look for a vanguard avatar being selected in the lists
    //and update the corresponding detail panel.
    /*private ListSelectionListener vgdLSListener = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            int index = vgdAvatarLists.indexOf(e.getSource());
            Object obj = vgdAvatarLists.get(index).getSelectedValue();
            PlayerPanel pp = playerPanels.get(index);
            CardDetailPanel cdp = vgdAvatarDetails.get(index);

            if (obj instanceof PaperCard) {
                pp.setVanguardButtonText(((PaperCard) obj).getName());
                cdp.setCard(Card.getCardForUi((PaperCard) obj));
                cdp.setVisible(true);
                refreshPanels(false, true);
            }
            else {
                pp.setVanguardButtonText((String) obj);
                cdp.setVisible(false);
            }
        }
    };*/

    /////////////////////////////////////
    //========== METHODS FOR VARIANTS

    public Set<GameType> getAppliedVariants() {
        return appliedVariants;
    }

    public int getTeam(final int playerIndex) {
        return appliedVariants.contains(GameType.Archenemy) ? archenemyTeams.get(playerIndex) : teams.get(playerIndex);
    }
    
    /*public List<FList<Object>> getPlanarDeckLists() {
        return planarDeckLists;
    }

    public List<FList<Object>> getCommanderDeckLists() {
        return commanderDeckLists;
    }

    public List<FList<Object>> getSchemeDeckLists() {
        return schemeDeckLists;
    }

    public List<FList<Object>> getVanguardLists() {
        return vgdAvatarLists;
    }*/

    public boolean isPlayerArchenemy(final int playernum) {
        return playerPanels.get(playernum).playerIsArchenemy;
    }

    /** Return all the Vanguard avatars. */
    public Iterable<PaperCard> getAllAvatars() {
        if (vgdAllAvatars.isEmpty()) {
            for (PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
                if (c.getRules().getType().isVanguard()) {
                    vgdAllAvatars.add(c);
                }
            }
        }
        return vgdAllAvatars;
    }

    /** Return the Vanguard avatars not flagged RemAIDeck. */
    public List<PaperCard> getAllAiAvatars() {
        return vgdAllAiAvatars;
    }

    /** Return the Vanguard avatars not flagged RemRandomDeck. */
    public List<PaperCard> getNonRandomHumanAvatars() {
        return nonRandomHumanAvatars;
    }

    /** Return the Vanguard avatars not flagged RemAIDeck or RemRandomDeck. */
    public List<PaperCard> getNonRandomAiAvatars() {
        return nonRandomAiAvatars;
    }

    /** Populate vanguard lists. */
    private void populateVanguardLists() {
        humanListData.add("Use deck's default avatar (random if unavailable)");
        humanListData.add("Random");
        aiListData.add("Use deck's default avatar (random if unavailable)");
        aiListData.add("Random");
        for (PaperCard cp : getAllAvatars()) {
            humanListData.add(cp);
            if (!cp.getRules().getAiHints().getRemRandomDecks()) {
                nonRandomHumanAvatars.add(cp);
            }
            if (!cp.getRules().getAiHints().getRemAIDecks()) {
                aiListData.add(cp);
                vgdAllAiAvatars.add(cp);
                if (!cp.getRules().getAiHints().getRemRandomDecks()) {
                    nonRandomAiAvatars.add(cp);
                }
            }
        }
    }

    /** update vanguard list. */
    public void updateVanguardList(int playerIndex) {
        /*FList<Object> vgdList = getVanguardLists().get(playerIndex);
        Object lastSelection = vgdList.getSelectedValue();
        vgdList.setListData(isPlayerAI(playerIndex) ? aiListData : humanListData);
        if (null != lastSelection) {
            vgdList.setSelectedValue(lastSelection, true);
        }

        if (-1 == vgdList.getSelectedIndex()) {
            vgdList.setSelectedIndex(0);
        }*/
    }
}
