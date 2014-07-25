package forge.screens.constructed;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.FThreads;
import forge.Forge;
import forge.Graphics;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.deck.FVanguardChooser;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.DeckManager;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.LaunchScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FList;
import forge.toolbox.FToggleSwitch;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.Callback;
import forge.util.Lang;
import forge.util.NameGenerator;
import forge.util.Utils;

public class ConstructedScreen extends LaunchScreen {
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final float PADDING = Utils.scaleMin(5);
    private static final int MAX_PLAYERS = 2; //8; //TODO: Support multiplayer
    private static final FSkinFont VARIANTS_FONT = FSkinFont.get(12);
    private static final FSkinFont LABEL_FONT = FSkinFont.get(14);

    // General variables
    private final FLabel lblPlayers = new FLabel.Builder().text("Players:").font(VARIANTS_FONT).build();
    private final FComboBox<Integer> cbPlayerCount;
    private List<Integer> teams = new ArrayList<Integer>(MAX_PLAYERS);
    private List<Integer> archenemyTeams = new ArrayList<Integer>(MAX_PLAYERS);

    // Variants frame and variables
    private final FLabel lblVariants = new FLabel.Builder().text("Variants:").font(VARIANTS_FONT).build();
    private final FComboBox<Object> cbVariants;
    private final Set<GameType> appliedVariants = new TreeSet<GameType>();

    private final List<PlayerPanel> playerPanels = new ArrayList<PlayerPanel>(MAX_PLAYERS);
    private final FScrollPane playersScroll = new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float y = 0;
            float height;
            for (int i = 0; i < getNumPlayers(); i++) {
                height = playerPanels.get(i).getPreferredHeight();
                playerPanels.get(i).setBounds(0, y, visibleWidth, height);
                y += height;
            }
            return new ScrollBounds(visibleWidth, y);
        }

        @Override
        public void drawOnContainer(Graphics g) {
            //draw top border above items
            float y = playersScroll.getTop() - FList.LINE_THICKNESS / 2;
            g.drawLine(FList.LINE_THICKNESS, FList.LINE_COLOR, 0, y, getWidth(), y);
        }
    };

    // Variants
    private int lastArchenemy = 0;

    public ConstructedScreen() {
        super("Constructed");

        btnStart.setEnabled(false); //disable start button until decks loaded

        add(lblPlayers);
        cbPlayerCount = add(new FComboBox<Integer>());
        cbPlayerCount.setFont(VARIANTS_FONT);
        for (int i = 2; i <= MAX_PLAYERS; i++) {
            cbPlayerCount.addItem(i);
        }
        cbPlayerCount.setSelectedItem(2);
        cbPlayerCount.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                int numPlayers = getNumPlayers();
                for (int i = 0; i < MAX_PLAYERS; i++) {
                    playerPanels.get(i).setVisible(i < numPlayers);
                }
                playersScroll.revalidate();
            }
        });

        add(lblVariants);
        cbVariants = add(new FComboBox<Object>());
        cbVariants.setFont(VARIANTS_FONT);
        cbVariants.addItem("(None)");
        cbVariants.addItem(GameType.Vanguard);
        cbVariants.addItem(GameType.Commander);
        cbVariants.addItem(GameType.Planechase);
        cbVariants.addItem(GameType.Archenemy);
        cbVariants.addItem(GameType.ArchenemyRumble);
        cbVariants.addItem("More....");
        cbVariants.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (cbVariants.getSelectedIndex() <= 0) {
                    appliedVariants.clear();
                    updateLayoutForVariants();
                }
                else if (cbVariants.getSelectedIndex() == cbVariants.getItemCount() - 1) {
                    Forge.openScreen(new MultiVariantSelect());
                    updateVariantSelection();
                }
                else {
                    appliedVariants.clear();
                    appliedVariants.add((GameType)cbVariants.getSelectedItem());
                    updateLayoutForVariants();
                }
            }
        });

        // Construct individual player panels
        for (int i = 0; i < MAX_PLAYERS; i++) {
            teams.add(i + 1);
            archenemyTeams.add(i == 0 ? 1 : 2);

            PlayerPanel player = new PlayerPanel(i);
            playerPanels.add(player);

            // Populate players panel
            player.setVisible(i < getNumPlayers());

            playersScroll.add(player);
        }

        add(playersScroll);

        updatePlayersFromPrefs();

        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                getDeckChooser(0).initialize(FPref.CONSTRUCTED_P1_DECK_STATE, DeckType.PRECONSTRUCTED_DECK);
                getDeckChooser(1).initialize(FPref.CONSTRUCTED_P2_DECK_STATE, DeckType.COLOR_DECK);
                /*getDeckChooser(2).initialize(FPref.CONSTRUCTED_P3_DECK_STATE, DeckType.COLOR_DECK);
                getDeckChooser(3).initialize(FPref.CONSTRUCTED_P4_DECK_STATE, DeckType.COLOR_DECK);
                getDeckChooser(4).initialize(FPref.CONSTRUCTED_P5_DECK_STATE, DeckType.COLOR_DECK);
                getDeckChooser(5).initialize(FPref.CONSTRUCTED_P6_DECK_STATE, DeckType.COLOR_DECK);
                getDeckChooser(6).initialize(FPref.CONSTRUCTED_P7_DECK_STATE, DeckType.COLOR_DECK);
                getDeckChooser(7).initialize(FPref.CONSTRUCTED_P8_DECK_STATE, DeckType.COLOR_DECK);*/ //TODO: Support multiplayer and improve performance of loading this screen by using background thread

                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        btnStart.setEnabled(true);
                    }
                });
            }
        });

        //disable player count and variants for now until they work properly
        lblPlayers.setEnabled(false);
        cbPlayerCount.setEnabled(false);
    }

    private void updateVariantSelection() {
        if (appliedVariants.isEmpty()) {
            cbVariants.setSelectedIndex(0);
        }
        else if (appliedVariants.size() == 1) {
            cbVariants.setSelectedItem(appliedVariants.iterator().next());
        }
        else {
            String text = "";
            for (GameType variantType : appliedVariants) {
                if (text.length() > 0) {
                    text += ", ";
                }
                text += variantType.toString();
            }
            cbVariants.setText(text);
        }
    }

    private void updateLayoutForVariants() {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            playerPanels.get(i).updateVariantControlsVisibility();
        }
        playersScroll.revalidate();
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float fieldHeight = cbPlayerCount.getHeight();
        lblPlayers.setBounds(x, y, lblPlayers.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
        x += lblPlayers.getWidth();
        cbPlayerCount.setBounds(x, y, Utils.AVG_FINGER_WIDTH, fieldHeight);
        x += cbPlayerCount.getWidth() + PADDING;
        lblVariants.setBounds(x, y, lblVariants.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
        x += lblVariants.getWidth();
        cbVariants.setBounds(x, y, width - x - PADDING, fieldHeight);

        y += cbPlayerCount.getHeight() + PADDING;
        playersScroll.setBounds(0, y, width, height - y);
    }

    public final FDeckChooser getDeckChooser(int playernum) {
        return playerPanels.get(playernum).deckChooser;
    }

    public int getNumPlayers() {
        return cbPlayerCount.getSelectedItem();
    }
    public void setNumPlayers(int numPlayers) {
        cbPlayerCount.setSelectedItem(numPlayers);
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        launchParams.gameType = GameType.Constructed;

        if (!isEnoughTeams()) {
            FOptionPane.showMessageDialog("There are not enough teams! Please adjust team allocations.");
            return false;
        }

        for (int i = 0; i < getNumPlayers(); i++) {
            if (getDeckChooser(i).getPlayer() == null) {
                FOptionPane.showMessageDialog("Please specify a deck for " + getPlayerName(i));
                return false;
            }
        } // Is it even possible anymore? I think current implementation assigns decks automatically.

        launchParams.appliedVariants.addAll(appliedVariants);

        boolean checkLegality = FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);
        if (checkLegality && !appliedVariants.contains(GameType.Commander)) { //Commander deck replaces regular deck and is checked later
            for (int i = 0; i < getNumPlayers(); i++) {
                String name = getPlayerName(i);
                String errMsg = GameType.Constructed.getDecksFormat().getDeckConformanceProblem(getDeckChooser(i).getPlayer().getDeck());
                if (errMsg != null) {
                    FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Deck");
                    return false;
                }
            }
        }

        for (int i = 0; i < getNumPlayers(); i++) {
            PlayerPanel playerPanel = playerPanels.get(i);
            String name = getPlayerName(i);
            LobbyPlayer lobbyPlayer = playerPanel.isPlayerAI() ? GuiBase.getInterface().createAiPlayer(name,
                    getPlayerAvatar(i)) : GuiBase.getInterface().getGuiPlayer();
            RegisteredPlayer rp = playerPanel.deckChooser.getPlayer();

            if (appliedVariants.isEmpty()) {
                rp.setTeamNumber(getTeam(i));
                launchParams.players.add(rp.setPlayer(lobbyPlayer));
            }
            else {
                Deck deck = null;
                boolean isCommanderMatch = appliedVariants.contains(GameType.Commander);
                if (isCommanderMatch) {
                    deck = playerPanel.lstCommanderDecks.getDeck();
                    if (checkLegality) {
                        String errMsg = GameType.Commander.getDecksFormat().getDeckConformanceProblem(deck);
                        if (errMsg != null) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Commander Deck");
                            return false;
                        }
                    }
                }

                // Initialize variables for other variants
                deck = deck == null ? rp.getDeck() : deck;
                Iterable<PaperCard> schemes = null;
                boolean playerIsArchenemy = isPlayerArchenemy(i);
                Iterable<PaperCard> planes = null;
                PaperCard vanguardAvatar = null;

                //Archenemy
                if (appliedVariants.contains(GameType.ArchenemyRumble)
                        || (appliedVariants.contains(GameType.Archenemy) && playerIsArchenemy)) {
                    Deck schemeDeck = playerPanel.lstSchemeDecks.getDeck();
                    CardPool schemePool = schemeDeck.get(DeckSection.Schemes);
                    if (checkLegality) {
                        String errMsg = GameType.Archenemy.getDecksFormat().getSchemeSectionConformanceProblem(schemePool);
                        if (errMsg != null) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Scheme Deck");
                            return false;
                        }
                    }
                    schemes = schemePool.toFlatList();
                }

                //Planechase
                if (appliedVariants.contains(GameType.Planechase)) {
                    Deck planarDeck = playerPanel.lstPlanarDecks.getDeck();
                    CardPool planePool = planarDeck.get(DeckSection.Planes);
                    if (checkLegality) {
                        String errMsg = GameType.Planechase.getDecksFormat().getPlaneSectionConformanceProblem(planePool);
                        if (null != errMsg) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Planar Deck");
                            return false;
                        }
                    }
                    planes = planePool.toFlatList();
                }

                //Vanguard
                if (appliedVariants.contains(GameType.Vanguard)) {
                    vanguardAvatar = playerPanel.lstVanguardAvatars.getLstVanguards().getSelectedItem();
                    if (vanguardAvatar == null) {
                        FOptionPane.showErrorDialog("No Vanguard avatar selected for " + name
                                + ". Please choose one or disable the Vanguard variant");
                        return false;
                    }
                }

                rp = RegisteredPlayer.forVariants(appliedVariants, deck, schemes, playerIsArchenemy, planes, vanguardAvatar);
                rp.setTeamNumber(getTeam(i));
                launchParams.players.add(rp.setPlayer(lobbyPlayer));
            }
            getDeckChooser(i).saveState();
        }

        return true;
    }

    private class PlayerPanel extends FContainer {
        private final int index;

        private final FLabel nameRandomiser;
        private final FLabel avatarLabel = new FLabel.Builder().opaque(true).iconScaleFactor(0.99f).alphaComposite(1).iconInBackground(true).build();
        private int avatarIndex;

        private final FTextField txtPlayerName = new FTextField("Player name");
        private final FToggleSwitch humanAiSwitch = new FToggleSwitch("Human", "AI");

        private boolean playerIsArchenemy = false;
        private FComboBox<Object> cbTeam = new FComboBox<Object>();
        private FComboBox<Object> cbArchenemyTeam = new FComboBox<Object>();

        private final FLabel btnDeck           = new FLabel.ButtonBuilder().text("Loading Deck...").build();
        private final FLabel btnSchemeDeck     = new FLabel.ButtonBuilder().text("Scheme Deck: Random Generated Deck").build();
        private final FLabel btnCommanderDeck  = new FLabel.ButtonBuilder().text("Commander Deck: Random Generated Deck").build();
        private final FLabel btnPlanarDeck     = new FLabel.ButtonBuilder().text("Planar Deck: Random Generated Deck").build();
        private final FLabel btnVanguardAvatar = new FLabel.ButtonBuilder().text("Vanguard Avatar: Random").build();

        private final FDeckChooser deckChooser, lstSchemeDecks, lstCommanderDecks, lstPlanarDecks;
        private final FVanguardChooser lstVanguardAvatars;

        public PlayerPanel(final int index0) {
            super();
            index = index0;
            playerIsArchenemy = index == 0;
            btnDeck.setEnabled(false); //disable deck button until done loading decks
            boolean isAi = isPlayerAI();
            deckChooser = new FDeckChooser(GameType.Constructed, isAi, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    btnDeck.setEnabled(true);
                    btnDeck.setText(deckChooser.getSelectedDeckType().toString() + ": " +
                            Lang.joinHomogenous(((DeckManager)e.getSource()).getSelectedItems(), DeckProxy.FN_GET_NAME));
                }
            });
            lstCommanderDecks = new FDeckChooser(GameType.Commander, isAi, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    btnCommanderDeck.setText("Commander Deck: " + ((DeckManager)e.getSource()).getSelectedItem().getName());
                }
            });
            lstSchemeDecks = new FDeckChooser(GameType.Archenemy, isAi, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    btnSchemeDeck.setText("Scheme Deck: " + ((DeckManager)e.getSource()).getSelectedItem().getName());
                }
            });
            lstPlanarDecks = new FDeckChooser(GameType.Planechase, isAi, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    btnPlanarDeck.setText("Planar Deck: " + ((DeckManager)e.getSource()).getSelectedItem().getName());
                }
            });
            lstVanguardAvatars = new FVanguardChooser(isAi, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    btnVanguardAvatar.setText("Vanguard: " + ((CardManager)e.getSource()).getSelectedItem().getName());
                }
            });

            createAvatar();
            add(avatarLabel);

            createNameEditor();
            add(newLabel("Name:"));
            add(txtPlayerName);

            nameRandomiser = createNameRandomizer();
            add(nameRandomiser);

            humanAiSwitch.setToggled(index != 0);
            humanAiSwitch.setChangedHandler(humanAiSwitched);
            add(humanAiSwitch);

            add(newLabel("Team:"));
            populateTeamsComboBoxes();
            cbTeam.setChangedHandler(teamChangedHandler);
            cbArchenemyTeam.setChangedHandler(teamChangedHandler);
            add(cbTeam);
            add(cbArchenemyTeam);

            add(btnDeck);
            btnDeck.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    deckChooser.setHeaderCaption("Select Deck for " + txtPlayerName.getText());
                    Forge.openScreen(deckChooser);
                }
            });
            add(btnCommanderDeck);
            btnCommanderDeck.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    lstCommanderDecks.setHeaderCaption("Select Commander Deck for " + txtPlayerName.getText());
                    Forge.openScreen(lstCommanderDecks);
                }
            });
            add(btnSchemeDeck);
            btnSchemeDeck.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    lstSchemeDecks.setHeaderCaption("Select Scheme Deck for " + txtPlayerName.getText());
                    Forge.openScreen(lstSchemeDecks);
                }
            });
            add(btnPlanarDeck);
            btnPlanarDeck.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    lstPlanarDecks.setHeaderCaption("Select Planar Deck for " + txtPlayerName.getText());
                    Forge.openScreen(lstPlanarDecks);
                }
            });
            add(btnVanguardAvatar);
            btnVanguardAvatar.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    lstVanguardAvatars.setHeaderCaption("Select Vanguard for " + txtPlayerName.getText());
                    Forge.openScreen(lstVanguardAvatars);
                }
            });

            updateVariantControlsVisibility();

            //disable team combo boxes for now
            cbTeam.setEnabled(false);
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float fieldHeight = txtPlayerName.getHeight();
            float avatarSize = 2 * fieldHeight + PADDING;
            float dy = fieldHeight + PADDING;

            avatarLabel.setBounds(x, y, avatarSize, avatarSize);
            x += avatarSize + PADDING;
            float w = width - x - fieldHeight - 2 * PADDING;
            txtPlayerName.setBounds(x, y, w, fieldHeight);
            x += w + PADDING;
            nameRandomiser.setBounds(x, y, fieldHeight, fieldHeight);

            y += dy;
            humanAiSwitch.setSize(humanAiSwitch.getAutoSizeWidth(fieldHeight), fieldHeight);
            x = width - humanAiSwitch.getWidth() - PADDING;
            humanAiSwitch.setPosition(x, y);
            w = x - avatarSize - 3 * PADDING;
            x = avatarSize + 2 * PADDING;
            if (cbArchenemyTeam.isVisible()) {
                cbArchenemyTeam.setBounds(x, y, w, fieldHeight);
            }
            else {
                cbTeam.setBounds(x, y, w, fieldHeight);
            }

            y += dy;
            x = PADDING;
            w = width - 2 * PADDING;
            if (btnCommanderDeck.isVisible()) {
                btnCommanderDeck.setBounds(x, y, w, fieldHeight);
            }
            else {
                btnDeck.setBounds(x, y, w, fieldHeight);
            }
            y += dy;
            if (btnSchemeDeck.isVisible()) {
                btnSchemeDeck.setBounds(x, y, w, fieldHeight);
                y += dy;
            }
            if (btnPlanarDeck.isVisible()) {
                btnPlanarDeck.setBounds(x, y, w, fieldHeight);
                y += dy;
            }
            if (btnVanguardAvatar.isVisible()) {
                btnVanguardAvatar.setBounds(x, y, w, fieldHeight);
            }
        }

        private float getPreferredHeight() {
            int rows = 3;
            if (!appliedVariants.isEmpty()) {
                if (btnSchemeDeck.isVisible()) {
                    rows++;
                }
                if (btnPlanarDeck.isVisible()) {
                    rows++;
                }
                if (btnVanguardAvatar.isVisible()) {
                    rows++;
                }
            }
            return rows * (txtPlayerName.getHeight() + PADDING) + PADDING;
        }

        @Override
        protected void drawOverlay(Graphics g) {
            float y = getHeight() - FList.LINE_THICKNESS / 2;
            g.drawLine(FList.LINE_THICKNESS, FList.LINE_COLOR, 0, y, getWidth(), y);
        }

        private final FEventHandler humanAiSwitched = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstVanguardAvatars.setIsAi(isPlayerAI());
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
                AvatarSelector.show(getPlayerName(), avatarIndex, getUsedAvatars(), new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        setAvatar(result);

                        if (index < 2) {
                            updateAvatarPrefs();
                        }
                    }
                });
            }
        };

        public void updateVariantControlsVisibility() {
            boolean isCommanderApplied = appliedVariants.contains(GameType.Commander);
            btnDeck.setVisible(!isCommanderApplied); // Commander deck replaces basic deck, so hide that
            btnCommanderDeck.setVisible(isCommanderApplied);

            boolean isArchenemyApplied = appliedVariants.contains(GameType.Archenemy);
            boolean archenemyVisiblity = appliedVariants.contains(GameType.ArchenemyRumble)
                    || (isArchenemyApplied && playerIsArchenemy);
            btnSchemeDeck.setVisible(archenemyVisiblity);

            cbTeam.setVisible(!isArchenemyApplied);
            cbArchenemyTeam.setVisible(isArchenemyApplied);
            cbArchenemyTeam.setEnabled(!(isArchenemyApplied && playerIsArchenemy));

            btnPlanarDeck.setVisible(appliedVariants.contains(GameType.Planechase));
            btnVanguardAvatar.setVisible(appliedVariants.contains(GameType.Vanguard));
        }

        public boolean isPlayerAI() {
            return humanAiSwitch.isToggled();
        }

        private void populateTeamsComboBoxes() {
            cbArchenemyTeam.addItem("Archenemy");
            cbArchenemyTeam.addItem("Heroes");
            cbArchenemyTeam.setSelectedIndex(archenemyTeams.get(index) - 1);
            cbArchenemyTeam.setEnabled(playerIsArchenemy);

            for (int i = 1; i <= MAX_PLAYERS; i++) {
                cbTeam.addItem("Team " + i);
            }
            cbTeam.setSelectedIndex(teams.get(index) - 1);
            cbTeam.setEnabled(true);
        }

        private FEventHandler teamChangedHandler = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                @SuppressWarnings("unchecked")
                FComboBox<Object> cb = (FComboBox<Object>)e.getSource();
                if (cb.getSelectedIndex() == -1) {
                    return;
                }
                if (appliedVariants.contains(GameType.Archenemy)) {
                    String sel = (String) cb.getSelectedItem();
                    if (sel.contains("Archenemy")) {
                        lastArchenemy = index;
                        for (PlayerPanel pp : playerPanels) {
                            int i = pp.index;
                            archenemyTeams.set(i, i == lastArchenemy ? 1 : 2);
                            pp.cbArchenemyTeam.setSelectedIndex(i == lastArchenemy ? 0 : 1);
                            pp.toggleIsPlayerArchenemy();
                        }
                    }
                }
                else {
                    teams.set(index, cb.getSelectedIndex() + 1);
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
            updateLayoutForVariants();
        }

        private FLabel createNameRandomizer() {
            final FLabel newNameBtn = new FLabel.Builder().iconInBackground(false)
                    .icon(FSkinImage.EDIT).opaque(false).build();
            newNameBtn.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    getNewName(new Callback<String>() {
                        @Override
                        public void run(String newName) {
                            if (newName == null) { return; }

                            txtPlayerName.setText(newName);

                            if (index == 0) {
                                prefs.setPref(FPref.PLAYER_NAME, newName);
                                prefs.save();
                            }
                        }
                    });
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
            txtPlayerName.setFont(LABEL_FONT);
            txtPlayerName.setChangedHandler(nameChangedHandler);
        }

        private void createAvatar() {
            String[] currentPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
            if (index < currentPrefs.length) {
                setAvatar(Integer.parseInt(currentPrefs[index]));
            }
            else {
                setAvatar(AvatarSelector.getRandomAvatar(getUsedAvatars()));
            }
            avatarLabel.setCommand(avatarCommand);
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
    private void updatePlayersFromPrefs() {
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
        return new FLabel.Builder().text(title).font(LABEL_FONT).align(HAlignment.RIGHT).build();
    }

    private List<Integer> getUsedAvatars() {
        List<Integer> usedAvatars = Arrays.asList(-1,-1,-1,-1,-1,-1,-1,-1);
        int i = 0;
        for (PlayerPanel pp : playerPanels) {
            usedAvatars.set(i++, pp.avatarIndex);
        }
        return usedAvatars;
    }

    private final void getNewName(final Callback<String> callback) {
        final String title = "Get new random name";
        final String message = "What type of name do you want to generate?";
        final FSkinImage icon = FOptionPane.QUESTION_ICON;
        final String[] genderOptions = new String[]{ "Male", "Female", "Any" };
        final String[] typeOptions = new String[]{ "Fantasy", "Generic", "Any" };

        FOptionPane.showOptionDialog(message, title, icon, genderOptions, 2, new Callback<Integer>() {
            @Override
            public void run(final Integer genderIndex) {
                if (genderIndex == null || genderIndex < 0) {
                    callback.run(null);
                    return;
                }
                
                FOptionPane.showOptionDialog(message, title, icon, typeOptions, 2, new Callback<Integer>() {
                    @Override
                    public void run(final Integer typeIndex) {
                        if (typeIndex == null || typeIndex < 0) {
                            callback.run(null);
                            return;
                        }

                        generateRandomName(genderOptions[genderIndex], typeOptions[typeIndex], getPlayerNames(), title, callback);
                    }
                });
            }
        });
    }

    private void generateRandomName(final String gender, final String type, final List<String> usedNames, final String title, final Callback<String> callback) {
        final String newName = NameGenerator.getRandomName(gender, type, usedNames);
        String confirmMsg = "Would you like to use the name \"" + newName + "\", or try again?";
        FOptionPane.showConfirmDialog(confirmMsg, title, "Use this name", "Try again", true, new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    callback.run(newName);
                }
                else {
                    generateRandomName(gender, type, usedNames, title, callback);
                }
            }
        });
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

        for (int i = 0; i < getNumPlayers(); i++) {
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
    
    private class MultiVariantSelect extends FScreen {
        private final FList<Variant> lstVariants = add(new FList<Variant>());

        private MultiVariantSelect() {
            super("Select Variants");

            lstVariants.setListItemRenderer(new VariantRenderer());
            lstVariants.addItem(new Variant(GameType.Vanguard, "Each player has a special \"Avatar\" card that affects the game."));
            lstVariants.addItem(new Variant(GameType.Commander, "Each player has a legendary \"General\" card which can be cast at any time and determines deck colors."));
            lstVariants.addItem(new Variant(GameType.Planechase, "Plane cards apply global effects. Plane card changed when a player rolls \"Chaos\" on the planar die."));
            lstVariants.addItem(new Variant(GameType.Archenemy, "One player is the Archenemy and can play scheme cards."));
            lstVariants.addItem(new Variant(GameType.ArchenemyRumble, "All players are Archenemies and can play scheme cards."));
        }

        @Override
        protected void doLayout(float startY, float width, float height) {
            lstVariants.setBounds(0, startY, width, height - startY);
        }

        private class Variant {
            private final GameType gameType;
            private final String description;
            
            private Variant(GameType gameType0, String description0) {
                gameType = gameType0;
                description = description0;
            }

            private void draw(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float w, float h) {
                x += w - h;
                w = h;
                FCheckBox.drawCheckBox(g, SettingsScreen.DESC_COLOR, color, appliedVariants.contains(gameType), x, y, w, h);
            }

            private void toggle() {
                if (appliedVariants.contains(gameType)) {
                    appliedVariants.remove(gameType);
                }
                else {
                    appliedVariants.add(gameType);

                    //only allow setting one of Archenemy or ArchenemyRumble
                    if (gameType == GameType.Archenemy) {
                        appliedVariants.remove(GameType.ArchenemyRumble);
                    }
                    else if (gameType == GameType.ArchenemyRumble) {
                        appliedVariants.remove(GameType.Archenemy);
                    }
                }
                updateVariantSelection();
                updateLayoutForVariants();
            }
        }

        private class VariantRenderer extends FList.ListItemRenderer<Variant> {
            @Override
            public float getItemHeight() {
                return SettingsScreen.SETTING_HEIGHT;
            }

            @Override
            public boolean tap(Integer index, Variant value, float x, float y, int count) {
                value.toggle();
                return true;
            }

            @Override
            public void drawValue(Graphics g, Integer index, Variant value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                float offset = w * SettingsScreen.INSETS_FACTOR - FList.PADDING;
                x += offset;
                y += offset;
                w -= 2 * offset;
                h -= 2 * offset;

                String text = value.gameType.toString();
                float totalHeight = h;
                h = font.getMultiLineBounds(text).height + SettingsScreen.SETTING_PADDING;

                g.drawText(text, font, foreColor, x, y, w, h, false, HAlignment.LEFT, false);
                value.draw(g, font, foreColor, x, y, w, h);
                h += SettingsScreen.SETTING_PADDING;
                g.drawText(value.description, SettingsScreen.DESC_FONT, SettingsScreen.DESC_COLOR, x, y + h, w, totalHeight - h + w * SettingsScreen.INSETS_FACTOR, true, HAlignment.LEFT, false);            
            }
        }
    }

    /////////////////////////////////////
    //========== METHODS FOR VARIANTS

    public Set<GameType> getAppliedVariants() {
        return appliedVariants;
    }

    public int getTeam(final int playerIndex) {
        return appliedVariants.contains(GameType.Archenemy) ? archenemyTeams.get(playerIndex) : teams.get(playerIndex);
    }

    public boolean isPlayerAI(final int playernum) {
        return playerPanels.get(playernum).isPlayerAI();
    }

    public boolean isPlayerArchenemy(final int playernum) {
        return playerPanels.get(playernum).playerIsArchenemy;
    }
}
