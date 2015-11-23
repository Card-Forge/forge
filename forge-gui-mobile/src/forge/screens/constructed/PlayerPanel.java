package forge.screens.constructed;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import forge.AIOption;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.deck.FVanguardChooser;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.DeckManager;
import forge.match.LobbySlot;
import forge.match.LobbySlotType;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.toolbox.FToggleSwitch;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.util.Lang;
import forge.util.NameGenerator;
import forge.util.Utils;

public class PlayerPanel extends FContainer {
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final float PADDING = Utils.scale(5);
    private static final FSkinFont LABEL_FONT = FSkinFont.get(14);

    private final LobbyScreen screen;
    private final int index;
    private final boolean allowNetworking;
    private boolean mayEdit = true;
    private boolean isReady, mayControl, mayRemove, useAiSimulation;
    private LobbySlotType type = LobbySlotType.LOCAL;

    private final FLabel nameRandomiser;
    private final FLabel avatarLabel = new FLabel.Builder().opaque(true).iconScaleFactor(0.99f).alphaComposite(1).iconInBackground(true).build();
    private int avatarIndex;

    private final FTextField txtPlayerName = new FTextField("Player name");
    private final FToggleSwitch humanAiSwitch;

    private FComboBox<Object> cbTeam = new FComboBox<Object>();
    private FComboBox<Object> cbArchenemyTeam = new FComboBox<Object>();

    private final FLabel btnDeck           = new FLabel.ButtonBuilder().text("Loading Deck...").build();
    private final FLabel btnSchemeDeck     = new FLabel.ButtonBuilder().text("Scheme Deck: Random Generated Deck").build();
    private final FLabel btnCommanderDeck  = new FLabel.ButtonBuilder().text("Commander Deck: Random Generated Deck").build();
    private final FLabel btnPlanarDeck     = new FLabel.ButtonBuilder().text("Planar Deck: Random Generated Deck").build();
    private final FLabel btnVanguardAvatar = new FLabel.ButtonBuilder().text("Vanguard Avatar: Random").build();

    private final FDeckChooser deckChooser, lstSchemeDecks, lstCommanderDecks, lstPlanarDecks;
    private final FVanguardChooser lstVanguardAvatars;

    public PlayerPanel(final LobbyScreen screen0, final boolean allowNetworking0, final int index0, final LobbySlot slot, final boolean mayEdit0, final boolean mayControl0) {
        super();
        screen = screen0;
        allowNetworking = allowNetworking0;
        if (allowNetworking) {
            humanAiSwitch = new FToggleSwitch("Not Ready", "Ready");
        }
        else {
            humanAiSwitch = new FToggleSwitch("Human", "AI");
        }
        index = index0;
        populateTeamsComboBoxes();
        setTeam(slot.getTeam());
        setIsArchenemy(slot.isArchenemy());
        setType(slot.getType());
        setPlayerName(slot.getName());
        setAvatarIndex(slot.getAvatarIndex());

        btnDeck.setEnabled(false); //disable deck button until done loading decks

        boolean isAi = isAi();
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

        humanAiSwitch.setChangedHandler(humanAiSwitched);
        add(humanAiSwitch);

        add(newLabel("Team:"));
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

        if (mayEdit == mayEdit0) {
            updateVariantControlsVisibility();
        }
        else {
            setMayEdit(false);
        }
        setMayControl(mayControl0);

        //disable team combo boxes for now
        cbTeam.setEnabled(false);
    }

    public void initialize(FPref savedStateSetting, DeckType defaultDeckType) {
        deckChooser.initialize(savedStateSetting, defaultDeckType);
        lstCommanderDecks.initialize(null, DeckType.RANDOM_DECK);
        lstPlanarDecks.initialize(null, DeckType.RANDOM_DECK);
        lstSchemeDecks.initialize(null, DeckType.RANDOM_DECK);
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
            y += dy;
        }
        else if (btnDeck.isVisible()) {
            btnDeck.setBounds(x, y, w, fieldHeight);
            y += dy;
        }
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

    public float getPreferredHeight() {
        int rows = 3;
        if (!btnDeck.isVisible()) {
            rows--;
        }
        if (btnCommanderDeck.isVisible()) {
            rows++;
        }
        if (btnSchemeDeck.isVisible()) {
            rows++;
        }
        if (btnPlanarDeck.isVisible()) {
            rows++;
        }
        if (btnVanguardAvatar.isVisible()) {
            rows++;
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
            boolean toggled = humanAiSwitch.isToggled();
            if (allowNetworking) {
                setIsReady(toggled);
                screen.setReady(index, toggled);
            }
            else {
                type = toggled ? LobbySlotType.AI : LobbySlotType.LOCAL;
                onIsAiChanged(toggled);

                LobbySlot slot = screen.getLobby().getSlot(index);
                slot.setType(type);

                //update may edit in-case it changed as a result of the AI change
                setMayEdit(screen.getLobby().mayEdit(index));
                setAvatarIndex(slot.getAvatarIndex());
                setPlayerName(slot.getName());
            }
        }
    };

    private void onIsAiChanged(boolean isAi) {
        deckChooser.setIsAi(isAi);
        lstCommanderDecks.setIsAi(isAi);
        lstPlanarDecks.setIsAi(isAi);
        lstSchemeDecks.setIsAi(isAi);
        lstVanguardAvatars.setIsAi(isAi);
    }

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
                    if (allowNetworking) {
                        screen.firePlayerChangeListener(index);
                    }
                }
            }
        }
    };

    private FEventHandler avatarCommand = new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            AvatarSelector.show(getPlayerName(), avatarIndex, screen.getUsedAvatars(), new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    setAvatarIndex(result);

                    if (index < 2) {
                        screen.updateAvatarPrefs();
                    }
                    if (allowNetworking) {
                        screen.firePlayerChangeListener(index);
                    }
                }
            });
        }
    };

    public void setDeckSelectorButtonText(String text) {
        btnDeck.setText(text);
    }

    public void updateVariantControlsVisibility() {
        boolean isCommanderApplied = false;
        boolean isPlanechaseApplied = false;
        boolean isVanguardApplied = false;
        boolean isArchenemyApplied = false;
        boolean archenemyVisiblity = false;
        boolean isDeckBuildingAllowed = mayEdit;

        for (GameType variant : screen.getLobby().getAppliedVariants()) {
            switch (variant) {
            case Archenemy:
                isArchenemyApplied = true;
                if (isArchenemy()) {
                    archenemyVisiblity = true;
                }
                break;
            case ArchenemyRumble:
                archenemyVisiblity = true;
                break;
            case Commander:
            case TinyLeaders:
                isCommanderApplied = true;
                isDeckBuildingAllowed = false; //Commander deck replaces basic deck, so hide that
                break;
            case Planechase:
                isPlanechaseApplied = true;
                break;
            case Vanguard:
                isVanguardApplied = true;
                break;
            default:
                if (variant.isAutoGenerated()) {
                    isDeckBuildingAllowed = false;
                }
                break;
            }
        }

        btnDeck.setVisible(isDeckBuildingAllowed);
        btnCommanderDeck.setVisible(isCommanderApplied && mayEdit);

        btnSchemeDeck.setVisible(archenemyVisiblity && mayEdit);

        cbTeam.setVisible(!isArchenemyApplied);
        cbArchenemyTeam.setVisible(isArchenemyApplied);

        btnPlanarDeck.setVisible(isPlanechaseApplied && mayEdit);
        btnVanguardAvatar.setVisible(isVanguardApplied && mayEdit);
    }

    public boolean isAi() {
        return type == LobbySlotType.AI;
    }

    public boolean isArchenemy() {
        return cbArchenemyTeam.getSelectedIndex() == 0;
    }
    public void setIsArchenemy(boolean isArchenemy0) {
        cbArchenemyTeam.setSelectedIndex(isArchenemy0 ? 0 : 1);
    }

    private void populateTeamsComboBoxes() {
        cbArchenemyTeam.addItem("Archenemy");
        cbArchenemyTeam.addItem("Heroes");

        for (int i = 1; i <= LobbyScreen.MAX_PLAYERS; i++) {
            cbTeam.addItem("Team " + i);
        }
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
            if (screen.hasVariant(GameType.Archenemy)) {
                String sel = (String) cb.getSelectedItem();
                if (sel.contains("Archenemy")) {
                    screen.lastArchenemy = index;
                    for (PlayerPanel pp : screen.getPlayerPanels()) {
                        int i = pp.index;
                        int team = i == screen.lastArchenemy ? 0 : 1;
                        pp.setArchenemyTeam(team);
                        pp.toggleIsPlayerArchenemy();
                    }
                }
            }
        }
    };

    public void toggleIsPlayerArchenemy() {
        if (screen.hasVariant(GameType.Archenemy)) {
            setIsArchenemy(screen.lastArchenemy == index);
        }
        else {
            setIsArchenemy(screen.hasVariant(GameType.ArchenemyRumble));
        }
        screen.updateLayoutForVariants();
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
                        if (allowNetworking) {
                            screen.firePlayerChangeListener(index);
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
            name = NameGenerator.getRandomName("Any", "Any", screen.getPlayerNames());
        }

        txtPlayerName.setText(name);
        txtPlayerName.setFont(LABEL_FONT);
        txtPlayerName.setChangedHandler(nameChangedHandler);
    }

    private void createAvatar() {
        String[] currentPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
        if (index < currentPrefs.length) {
            setAvatarIndex(Integer.parseInt(currentPrefs[index]));
        }
        else {
            setAvatarIndex(AvatarSelector.getRandomAvatar(screen.getUsedAvatars()));
        }
        avatarLabel.setCommand(avatarCommand);
    }

    public void setAvatarIndex(int newAvatarIndex) {
        avatarIndex = newAvatarIndex;
        if (avatarIndex != -1) {
            avatarLabel.setIcon(new FTextureRegionImage(FSkin.getAvatars().get(newAvatarIndex)));
        }
        else {
            avatarLabel.setIcon(null);
        }
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

    public LobbySlotType getType() {
        return type;
    }
    public void setType(LobbySlotType type0) {
        if (type == type0) { return; }

        boolean wasAi = isAi();

        type = type0;

        switch (type) {
        case LOCAL:
            humanAiSwitch.setToggled(false);
            break;
        case AI:
            humanAiSwitch.setToggled(true);
            break;
        case OPEN:
            isReady = false;
            humanAiSwitch.setToggled(false);
            break;
        case REMOTE:
            humanAiSwitch.setToggled(isReady);
            break;
        }

        boolean isAi = isAi();
        if (isAi != wasAi && deckChooser != null) {
            onIsAiChanged(isAi);
        }
    }

    public Set<AIOption> getAiOptions() {
        return isSimulatedAi()
                ? ImmutableSet.of(AIOption.USE_SIMULATION)
                : Collections.<AIOption>emptySet();
    }
    private boolean isSimulatedAi() {
        return isAi() && useAiSimulation;
    }
    public void setUseAiSimulation(final boolean useAiSimulation0) {
        useAiSimulation = useAiSimulation0;
    }

    public int getTeam() {
        return cbTeam.getSelectedIndex();
    }
    public void setTeam(int team0) {
        cbTeam.setSelectedIndex(team0);
    }

    public int getArchenemyTeam() {
        return cbTeam.getSelectedIndex();
    }
    public void setArchenemyTeam(int team0) {
        cbTeam.setSelectedIndex(team0);
    }

    public boolean isReady() {
        return isReady;
    }
    public void setIsReady(boolean isReady0) {
        if (isReady == isReady0) { return; }
        isReady = isReady0;
        if (allowNetworking) {
            humanAiSwitch.setToggled(isReady);
        }
    }

    public void setMayEdit(boolean mayEdit0) {
        if (mayEdit == mayEdit0) { return; }
        mayEdit = mayEdit0;
        avatarLabel.setEnabled(mayEdit);
        txtPlayerName.setEnabled(mayEdit);
        nameRandomiser.setEnabled(mayEdit);
        humanAiSwitch.setEnabled(mayEdit);
        updateVariantControlsVisibility();

        //if panel has height already, ensure height updated to account for button visibility changes
        if (getHeight() > 0) {
            screen.getPlayersScroll().revalidate();
        }
    }

    public void setMayControl(boolean mayControl0) {
        if (mayControl == mayControl0) { return; }
        mayControl = mayControl0;
    }

    public void setMayRemove(boolean mayRemove0) {
        if (mayRemove == mayRemove0) { return; }
        mayRemove = mayRemove0;
    }

    public FDeckChooser getDeckChooser() {
        return deckChooser;
    }

    public Deck getDeck() {
        return deckChooser.getDeck();
    }

    public Deck getCommanderDeck() {
        return lstCommanderDecks.getDeck();
    }

    public Deck getSchemeDeck() {
        return lstSchemeDecks.getDeck();
    }

    public Deck getPlanarDeck() {
        return lstPlanarDecks.getDeck();
    }

    public PaperCard getVanguardAvatar() {
        return lstVanguardAvatars.getLstVanguards().getSelectedItem();
    }

    /** Adds a pre-styled FLabel component with the specified title. */
    private FLabel newLabel(String title) {
        return new FLabel.Builder().text(title).font(LABEL_FONT).align(HAlignment.RIGHT).build();
    }
    
    private static final ImmutableList<String> genderOptions = ImmutableList.of("Male",    "Female",  "Any");
    private static final ImmutableList<String> typeOptions   = ImmutableList.of("Fantasy", "Generic", "Any");
    private final void getNewName(final Callback<String> callback) {
        final String title = "Get new random name";
        final String message = "What type of name do you want to generate?";
        final FSkinImage icon = FOptionPane.QUESTION_ICON;

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

                        generateRandomName(genderOptions.get(genderIndex), typeOptions.get(typeIndex), screen.getPlayerNames(), title, callback);
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
}
