package forge.screens.home;

import forge.ai.AiProfileUtil;
import forge.deckchooser.FDeckChooser;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;

import forge.Singletons;
import forge.ai.AIOption;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorVariant;
import forge.screens.home.sanctioned.AvatarSelector;
import forge.screens.home.sanctioned.SleeveSelector;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FPanel;
import forge.toolbox.FRadioButton;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FTextField;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.NameGenerator;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class PlayerPanel extends FPanel {
    final Localizer localizer = Localizer.getInstance();
    private final static ForgePreferences prefs = FModel.getPreferences();
    private static final SkinColor unfocusedPlayerOverlay = FSkin.getColor(FSkin.Colors.CLR_OVERLAY).alphaColor(120);

    private LobbySlotType type;
    private final int index;
    private String playerName = StringUtils.EMPTY;
    private boolean mayEdit, mayControl, mayRemove;

    private final FLabel nameRandomiser;
    private final FLabel avatarLabel = new FLabel.Builder().opaque(true).hoverable(true).iconScaleFactor(0.99f).iconInBackground(true).build();
    private final FLabel sleeveLabel = new FLabel.Builder().opaque(true).hoverable(true).iconScaleFactor(0.99f).iconInBackground(true).build();
    private int avatarIndex, sleeveIndex;

    private final FTextField txtPlayerName = new FTextField.Builder().build();
    private FRadioButton radioHuman;
    private FRadioButton radioAi;
    private JCheckBoxMenuItem radioAiUseSimulation;
    private FRadioButton radioOpen;
    private FCheckBox chkReady;

    // AI picker
    private String aiProfile;
    private final FLabel aiPickerLabel = new FLabel.Builder().text(localizer.getMessage("lblAiPickerPanel") + ":").build();
    private FComboBoxWrapper<Object> aiPickerComboBox = new FComboBoxWrapper<>();

    private final FComboBoxWrapper<Object> teamComboBox = new FComboBoxWrapper<>();
    private final FComboBoxWrapper<Object> aeTeamComboBox = new FComboBoxWrapper<>();

    private final FLabel closeBtn;
    private final FLabel deckBtn = new FLabel.ButtonBuilder().text(localizer.getMessage("lblSelectaDeck")).build();
    private final FLabel deckLabel;

    private final String variantBtnConstraints = "height 30px, hidemode 3";

    private final FLabel scmDeckSelectorBtn = new FLabel.ButtonBuilder().text(localizer.getMessage("lblSelectaSchemeDeck")).build();
    private final FLabel scmDeckEditor = new FLabel.ButtonBuilder().text(localizer.getMessage("lblSchemeDeckEditor")).build();
    private final FLabel scmLabel;

    private final FLabel cmdDeckSelectorBtn = new FLabel.ButtonBuilder().text(localizer.getMessage("lblSelectaCommanderDeck")).build();
    private final FLabel cmdLabel;

    private final FLabel pchDeckSelectorBtn = new FLabel.ButtonBuilder().text(localizer.getMessage("lblSelectaPlanarDeck")).build();
    private final FLabel pchDeckEditor = new FLabel.ButtonBuilder().text(localizer.getMessage("lblPlanarDeckEditor")).build();
    private final FLabel pchLabel;

    private final FLabel vgdSelectorBtn = new FLabel.ButtonBuilder().text(localizer.getMessage("lblSelectaVanguardAvatar")).build();
    private final FLabel vgdLabel;

    private FCheckBox chkDevMode;

    private boolean allowNetworking;

    private FDeckChooser deckChooser;

    private final VLobby lobby;
    public PlayerPanel(final VLobby lobby, final boolean allowNetworking, final int index, final LobbySlot slot, final boolean mayEdit, final boolean mayControl) {
        super();

        this.lobby = lobby;
        this.index = index;
        this.mayEdit = mayEdit;
        this.mayControl = mayControl;
        this.allowNetworking = allowNetworking;

        this.deckLabel = lobby.newLabel(localizer.getMessage("lblDeck") + ":");
        this.scmLabel = lobby.newLabel(localizer.getMessage("lblSchemeDeck") + ":");
        this.cmdLabel = lobby.newLabel(localizer.getMessage("lblCommanderDeck") + ":");
        this.pchLabel =  lobby.newLabel(localizer.getMessage("lblPlanarDeck") + ":");
        this.vgdLabel =  lobby.newLabel(localizer.getMessage("lblVanguard") + ":");

        setLayout(new MigLayout("insets 10px, gap 5px"));

        // Add a button to players 3+ (or if server) to remove them from the setup
        closeBtn = createCloseButton();
        this.add(closeBtn, "w 20, h 20, pos (container.w-20) 0");

        createAvatar();
        this.add(avatarLabel, "spany 2, width 80px, height 80px");

        /*TODO Layout and Override for PC*/
        //createSleeve();
        //this.add(sleeveLabel, "spany 2, width 60px, height 80px");

        createNameEditor();
        this.add(lobby.newLabel(localizer.getMessage("lblName") +":"), "w 40px, h 30px, gaptop 5px");
        this.add(txtPlayerName, "h 30px, pushx, growx");

        nameRandomiser = createNameRandomizer();
        this.add(nameRandomiser, "h 30px, w 30px, gaptop 5px");

        createPlayerTypeOptions();
        this.add(radioHuman, "gapright 5px");
        this.add(radioAi, "wrap");

        int cellY = 1;
        if (prefs.getPrefBoolean(FPref.UI_ENABLE_AI_PICKER)) {
            this.add(aiPickerLabel, "w 40px, h 30px");
            populateAiPickerComboBox();
            aiPickerComboBox.addTo(this, "h 30px, pushx, growx, wrap");
            aiPickerComboBox.addActionListener(aiPickerListener);
            cellY += 1;
        }
        this.setAiProfile(slot.getAiProfile());

        this.add(lobby.newLabel(localizer.getMessage("lblTeam") + ":"), "cell 0 " + cellY +", sx 2, ax right, w 40px, h 30px");
        populateTeamsComboBoxes();

        // Set these before action listeners are added
        this.setTeam(slot == null ? index : slot.getTeam());
        this.setIsArchenemy(slot != null && slot.isArchenemy());

        teamComboBox.addActionListener(teamListener);
        aeTeamComboBox.addActionListener(teamListener);
        teamComboBox.addTo(this, variantBtnConstraints + ", cell 2 " + cellY + ", growx, gaptop 5px, wrap");
        aeTeamComboBox.addTo(this, variantBtnConstraints + ", cell 2 " + cellY + ", growx, gaptop 5px, wrap");

        createReadyButton();
        if (allowNetworking) {
            this.add(radioOpen, "cell 4 4, ax left, sx 2");
            this.add(chkReady, "cell 5 4, ax left, sx 2, wrap");
        }

        this.add(deckLabel, variantBtnConstraints + ", cell 0 5, sx 2, ax right");
        this.add(deckBtn, variantBtnConstraints + ", cell 2 5, pushx, growx, wmax 100%-153px, h 30px, spanx 4, wrap");

        addHandlersDeckSelector();

        this.add(cmdLabel, variantBtnConstraints + ", cell 0 2, sx 2, ax right");
        this.add(cmdDeckSelectorBtn, variantBtnConstraints + ", cell 2 2, pushx, growx, wmax 100%-153px, h 30px, spanx 4, wrap");

        this.add(scmLabel, variantBtnConstraints + ", cell 0 4, sx 2, ax right");
        this.add(scmDeckSelectorBtn, variantBtnConstraints + ", cell 2 4, growx, pushx");
        this.add(scmDeckEditor, variantBtnConstraints + ", cell 3 4, sx 3, growx, wrap");

        this.add(pchLabel, variantBtnConstraints + ", cell 0 5, sx 2, ax right");
        this.add(pchDeckSelectorBtn, variantBtnConstraints + ", cell 2 5, growx, pushx");
        this.add(pchDeckEditor, variantBtnConstraints + ", cell 3 5, sx 3, growx, wrap");

        this.add(vgdLabel, variantBtnConstraints + ", cell 0 6, sx 2, ax right");
        this.add(vgdSelectorBtn, variantBtnConstraints + ", cell 2 6, sx 4, growx, wrap");

        addHandlersToVariantsControls();

        this.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftMouseDown(final MouseEvent e) {
                avatarLabel.requestFocusInWindow();
            }
        });

        if (isNetworkHost()) {
            createDevModeButton();
            this.add(chkDevMode);
        }

        this.type = slot == null ? LobbySlotType.LOCAL : slot.getType();
        this.setPlayerName(slot == null ? "" : slot.getName());
        this.setAvatarIndex(slot == null ? 0 : slot.getAvatarIndex());

        update();
    }

    boolean isNetworkHost() {
        return this.allowNetworking && this.index == 0;
    }

    void update() {
        avatarLabel.setEnabled(mayEdit);
        avatarLabel.setIcon(FSkin.getAvatars().get(type == LobbySlotType.OPEN ? -1 : avatarIndex));
        avatarLabel.repaintSelf();

        sleeveLabel.setEnabled(mayEdit);
        sleeveLabel.setIcon(FSkin.getSleeves().get(type == LobbySlotType.OPEN ? -1 : sleeveIndex));
        sleeveLabel.repaintSelf();

        txtPlayerName.setEnabled(mayEdit);
        txtPlayerName.setText(type == LobbySlotType.OPEN ? StringUtils.EMPTY : playerName);
        nameRandomiser.setEnabled(mayEdit);

        boolean enableAiPicker = mayEdit && type == LobbySlotType.AI && prefs.getPrefBoolean(FPref.UI_ENABLE_AI_PICKER);
        aiPickerLabel.setVisible(enableAiPicker);
        aiPickerComboBox.setVisible(enableAiPicker);
        aiPickerComboBox.setEnabled(enableAiPicker);

        teamComboBox.setEnabled(mayEdit);
        deckLabel.setVisible(mayEdit);
        deckBtn.setVisible(mayEdit);
        chkReady.setVisible(type == LobbySlotType.LOCAL || type == LobbySlotType.REMOTE);
        chkReady.setEnabled(mayEdit);

        if (chkDevMode != null) {
            chkDevMode.setEnabled(mayEdit);
        }

        closeBtn.setVisible(mayRemove);

        if (mayRemove) {
            radioHuman.setEnabled(mayControl);
            radioAi.setEnabled(mayControl);
            radioOpen.setEnabled(mayControl);
        } else {
            radioHuman.setVisible(mayControl);
            radioAi.setVisible(mayControl);
            radioOpen.setVisible(mayControl);
        }

        radioHuman.setSelected(type == LobbySlotType.LOCAL);
        radioAi.setSelected(type == LobbySlotType.AI);
        radioOpen.setSelected(type == LobbySlotType.OPEN);

        updateVariantControlsVisibility();
    }

    private FMouseAdapter radioMouseAdapter(final FRadioButton source, final LobbySlotType type) {
        return new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                if (!source.isEnabled()) {
                    return;
                }
                setType(type);
                if (type == LobbySlotType.AI && getPlayerName().isEmpty()) {
                    final String newName = NameGenerator.getRandomName("Any", "Any", lobby.getPlayerNames());
                    setPlayerName(newName);
                }
                lobby.firePlayerChangeListener(index);
                avatarLabel.requestFocusInWindow();
                lobby.updateVanguardList(index);
            }
        };
    }

    /**
     * Listens to name text fields and gives the appropriate player focus. Also
     * saves the name preference when leaving player one's text field.
     */
    private final FocusAdapter nameFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(final FocusEvent e) {
            lobby.changePlayerFocus(index);
        }

        @Override
        public void focusLost(final FocusEvent e) {
            final Object source = e.getSource();
            if (source instanceof FTextField) { // the text box
                final FTextField nField = (FTextField)source;
                final String newName = nField.getText().trim();
                if (index == 0 && !StringUtils.isBlank(newName)
                        && StringUtils.isAlphanumericSpace(newName) && prefs.getPref(FPref.PLAYER_NAME) != newName) {
                    prefs.setPref(FPref.PLAYER_NAME, newName);
                    prefs.save();
                }
                lobby.firePlayerChangeListener(index);
            }
        }
    };

    /** Listens to avatar buttons and gives the appropriate player focus. */
    private final FocusAdapter avatarFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(final FocusEvent e) {
            lobby.changePlayerFocus(index);
        }
    };

    private final FMouseAdapter avatarMouseListener = new FMouseAdapter() {
        @Override public void onLeftClick(final MouseEvent e) {
            if (!avatarLabel.isEnabled()) {
                return;
            }

            final FLabel avatar = (FLabel)e.getSource();

            lobby.changePlayerFocus(index);
            avatar.requestFocusInWindow();

            final AvatarSelector aSel = new AvatarSelector(playerName, avatarIndex, lobby.getUsedAvatars());
            for (final FLabel lbl : aSel.getSelectables()) {
                lbl.setCommand((UiCommand) () -> {
                    setAvatarIndex(Integer.parseInt(lbl.getName().substring(11)));
                    aSel.setVisible(false);
                });
            }

            aSel.setVisible(true);
            aSel.dispose();

            if (index < 2) {
                lobby.updateAvatarPrefs();
            }

            lobby.firePlayerChangeListener(index);
        }

        @Override public void onRightClick(final MouseEvent e) {
            if (!avatarLabel.isEnabled()) {
                return;
            }

            lobby.changePlayerFocus(index);
            avatarLabel.requestFocusInWindow();

            setRandomAvatar();

            if (index < 2) {
                lobby.updateAvatarPrefs();
            }
        }
    };

    /** Listens to sleeve buttons and gives the appropriate player focus. */
    private final FocusAdapter sleeveFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(final FocusEvent e) {
            lobby.changePlayerFocus(index);
        }
    };

    private final FMouseAdapter sleeveMouseListener = new FMouseAdapter() {
        @Override public void onLeftClick(final MouseEvent e) {
            if (!sleeveLabel.isEnabled()) {
                return;
            }

            final FLabel sleeve = (FLabel)e.getSource();

            lobby.changePlayerFocus(index);
            sleeve.requestFocusInWindow();

            final SleeveSelector sSel = new SleeveSelector(playerName, sleeveIndex, lobby.getUsedSleeves());
            for (final FLabel lbl : sSel.getSelectables()) {
                lbl.setCommand((UiCommand) () -> {
                    setSleeveIndex(Integer.parseInt(lbl.getName().substring(11)));
                    sSel.setVisible(false);
                });
            }

            sSel.setVisible(true);
            sSel.dispose();

            if (index < 2) {
                lobby.updateSleevePrefs();
            }

            lobby.firePlayerChangeListener(index);
        }

        @Override public void onRightClick(final MouseEvent e) {
            if (!sleeveLabel.isEnabled()) {
                return;
            }

            lobby.changePlayerFocus(index);
            sleeveLabel.requestFocusInWindow();

            setRandomSleeve();

            if (index < 2) {
                lobby.updateSleevePrefs();
            }
        }
    };

    private void updateVariantControlsVisibility() {
        final boolean isOathbreaker = lobby.hasVariant(GameType.Oathbreaker);
        final boolean isTinyLeaders = lobby.hasVariant(GameType.TinyLeaders);
        final boolean isBrawl = lobby.hasVariant(GameType.Brawl);
        final boolean isCommanderApplied = mayEdit && (lobby.hasVariant(GameType.Commander) || isOathbreaker || isTinyLeaders || isBrawl);
        final boolean isPlanechaseApplied = mayEdit && lobby.hasVariant(GameType.Planechase);
        final boolean isVanguardApplied = mayEdit && lobby.hasVariant(GameType.Vanguard);
        final boolean isArchenemyApplied = mayEdit && lobby.hasVariant(GameType.Archenemy);
        final boolean archenemyVisiblity = mayEdit && lobby.hasVariant(GameType.ArchenemyRumble) || (isArchenemyApplied && isArchenemy());
        // Commander deck building replaces normal one, so hide it
        final boolean isDeckBuildingAllowed = mayEdit && !isCommanderApplied && !lobby.hasVariant(GameType.MomirBasic)
                && !lobby.hasVariant(GameType.MoJhoSto);

        deckLabel.setVisible(isDeckBuildingAllowed);
        deckBtn.setVisible(isDeckBuildingAllowed);
        cmdDeckSelectorBtn.setVisible(isCommanderApplied);
        cmdLabel.setVisible(isCommanderApplied);

        scmDeckSelectorBtn.setVisible(archenemyVisiblity);
        scmDeckEditor.setVisible(archenemyVisiblity);
        scmLabel.setVisible(archenemyVisiblity);

        teamComboBox.setVisible(!isArchenemyApplied);
        aeTeamComboBox.setVisible(isArchenemyApplied);

        pchDeckSelectorBtn.setVisible(isPlanechaseApplied);
        pchDeckEditor.setVisible(isPlanechaseApplied);
        pchLabel.setVisible(isPlanechaseApplied);

        vgdSelectorBtn.setVisible(isVanguardApplied);
        vgdLabel.setVisible(isVanguardApplied);
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (!hasFocusInLobby()) {
            FSkin.setGraphicsColor(g, unfocusedPlayerOverlay);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }

    private boolean hasFocusInLobby() {
        return lobby.hasFocus(index);
    }

    LobbySlotType getType() {
        return type;
    }

    public boolean isAi() {
        return type == LobbySlotType.AI;
    }

    public Set<AIOption> getAiOptions() {
        return isSimulatedAi()
                ? ImmutableSet.of(AIOption.USE_SIMULATION)
                : Collections.emptySet();
    }
    private boolean isSimulatedAi() {
        return radioAi.isSelected() && radioAiUseSimulation.isSelected();
    }
    public void setUseAiSimulation(final boolean useSimulation) {
        radioAiUseSimulation.setSelected(useSimulation);
    }

    public boolean isArchenemy() {
        return aeTeamComboBox.getSelectedIndex() == 0;
    }

    public void setType(final LobbySlotType type) {
        this.type = type;
        switch (type) {
        case LOCAL:
            radioHuman.setSelected(true);
            break;
        case AI:
            radioAi.setSelected(true);
            break;
        case OPEN:
            radioOpen.setSelected(true);
            break;
        case REMOTE:
            break;
        }
        update();
    }

    public void setRemote(final boolean remote) {
        if (remote) {
            setType(LobbySlotType.REMOTE);
            radioHuman.setSelected(false);
            radioAi.setSelected(false);
            radioOpen.setSelected(false);
        } else {
            setType(LobbySlotType.OPEN);
        }
    }

    public void setVanguardButtonText(final String text) {
        vgdSelectorBtn.setText(text);
    }

    public void setDeckSelectorButtonText(final String text) {
        deckBtn.setText(text);
    }

    public void setCommanderDeckSelectorButtonText(final String text) {
        cmdDeckSelectorBtn.setText(text);
    }

    public void focusOnAvatar() {
        avatarLabel.requestFocusInWindow();
    }

    /**
     * Setup the AI Picker combo box with the known AI profiles.
     * Default the combo box selection to the default value of FPref.UI_CURRENT_AI_PROFILE.
     */
    private void populateAiPickerComboBox() {
        aiPickerComboBox.removeAllItems();
        final List<String> aiProfiles = AiProfileUtil.getAvailableProfiles();
        for (final String profile : aiProfiles) {
            aiPickerComboBox.addItem(profile);
        }
        aiPickerComboBox.setSelectedItem(FPref.UI_CURRENT_AI_PROFILE.getDefault());
        aiPickerComboBox.setEnabled(true);
    }

    private void populateTeamsComboBoxes() {
        aeTeamComboBox.addItem(localizer.getMessage("lblArchenemy"));
        aeTeamComboBox.addItem(localizer.getMessage("lblHeroes"));

        for (int i = 1; i <= VLobby.MAX_PLAYERS; i++) {
            teamComboBox.addItem(i);
        }
        teamComboBox.setEnabled(true);
    }

    private final ActionListener teamListener = new ActionListener() {
        @SuppressWarnings("unchecked")
        @Override public void actionPerformed(final ActionEvent e) {
            final FComboBox<Object> cb = (FComboBox<Object>) e.getSource();
            cb.requestFocusInWindow();
            final Object selection = cb.getSelectedItem();

            if (null != selection) {
                lobby.changePlayerFocus(index);
                lobby.firePlayerChangeListener(index);
            }
        }
    };

    private final ActionListener aiPickerListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            final FComboBox<Object> comboBox = (FComboBox<Object>) e.getSource();
            closeBtn.requestFocusInWindow();
            final Object selection = comboBox.getSelectedItem();

            if (selection != null) {
                setAiProfile(selection.toString());
                lobby.changePlayerFocus(index);
                lobby.firePlayerChangeListener(index);
            }
        }
    };

    private void addHandlersToVariantsControls() {
        // Archenemy buttons
        scmDeckSelectorBtn.setCommand((Runnable) () -> {
            lobby.setCurrentGameMode(lobby.hasVariant(GameType.Archenemy) ? GameType.Archenemy : GameType.ArchenemyRumble);
            scmDeckSelectorBtn.requestFocusInWindow();
            lobby.changePlayerFocus(index);
        });

        scmDeckEditor.setCommand((UiCommand) () -> {
            lobby.setCurrentGameMode(lobby.hasVariant(GameType.Archenemy) ? GameType.Archenemy : GameType.ArchenemyRumble);
            final Predicate<PaperCard> predSchemes = arg0 -> arg0.getRules().getType().isScheme();

            Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_ARCHENEMY);
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                    new CEditorVariant(FModel.getDecks().getScheme(), predSchemes, DeckSection.Schemes, FScreen.DECK_EDITOR_ARCHENEMY, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
        });

        // Commander buttons
        cmdDeckSelectorBtn.setCommand((Runnable) () -> {
            lobby.setCurrentGameMode(
                    lobby.hasVariant(GameType.Oathbreaker) ? GameType.Oathbreaker :
                    lobby.hasVariant(GameType.TinyLeaders) ? GameType.TinyLeaders :
                    lobby.hasVariant(GameType.Brawl) ? GameType.Brawl :
                    GameType.Commander);
            cmdDeckSelectorBtn.requestFocusInWindow();
            lobby.changePlayerFocus(index);
        });

        // Planechase buttons
        pchDeckSelectorBtn.setCommand((Runnable) () -> {
            lobby.setCurrentGameMode(GameType.Planechase);
            pchDeckSelectorBtn.requestFocusInWindow();
            lobby.changePlayerFocus(index, GameType.Planechase);
        });

        pchDeckEditor.setCommand((UiCommand) () -> {
            lobby.setCurrentGameMode(GameType.Planechase);
            final Predicate<PaperCard> predPlanes = arg0 -> arg0.getRules().getType().isPlane() || arg0.getRules().getType().isPhenomenon();

            Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_PLANECHASE);
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                    new CEditorVariant(FModel.getDecks().getPlane(), predPlanes, DeckSection.Planes, FScreen.DECK_EDITOR_PLANECHASE, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
        });

        // Vanguard buttons
        vgdSelectorBtn.setCommand((Runnable) () -> {
            lobby.setCurrentGameMode(GameType.Vanguard);
            vgdSelectorBtn.requestFocusInWindow();
            lobby.changePlayerFocus(index, GameType.Vanguard);
        });
    }

    private void createPlayerTypeOptions() {
        radioHuman = new FRadioButton(localizer.getMessage("lblHuman"));
        radioAi = new FRadioButton(localizer.getMessage("lblAI"));
        radioOpen = new FRadioButton(localizer.getMessage("lblOpen"));

        final JPopupMenu menu = new  JPopupMenu();
        radioAiUseSimulation = new JCheckBoxMenuItem(localizer.getMessage("lblUseSimulation"));
        menu.add(radioAiUseSimulation);
        radioAiUseSimulation.addActionListener(e -> lobby.firePlayerChangeListener(index));
        radioAi.setComponentPopupMenu(menu);

        radioHuman.addMouseListener(radioMouseAdapter(radioHuman, LobbySlotType.LOCAL));
        radioAi.addMouseListener   (radioMouseAdapter(radioAi,    LobbySlotType.AI));
        radioOpen.addMouseListener (radioMouseAdapter(radioOpen,  LobbySlotType.OPEN));

        final ButtonGroup tempBtnGroup = new ButtonGroup();
        tempBtnGroup.add(radioHuman);
        tempBtnGroup.add(radioAi);
        tempBtnGroup.add(radioOpen);
    }

    private void createReadyButton() {
        chkReady = new FCheckBox(localizer.getMessage("lblReady"));
        chkReady.addActionListener(e -> lobby.setReady(index, chkReady.isSelected()));
    }

    private void createDevModeButton() {
        chkDevMode = new FCheckBox(localizer.getMessage("cbDevMode"));

        chkDevMode.addActionListener(e -> {
            final boolean toggle = chkDevMode.isSelected();
            prefs.setPref(FPref.DEV_MODE_ENABLED, String.valueOf(toggle));
            ForgePreferences.DEV_MODE = toggle;

            // ensure that preferences panel reflects the change
            prefs.save();

            lobby.setDevMode(index);
        });
    }

    private void addHandlersDeckSelector() {
        deckBtn.setCommand((Runnable) () -> {
            lobby.setCurrentGameMode(GameType.Constructed);
            deckBtn.requestFocusInWindow();
            lobby.changePlayerFocus(index, GameType.Constructed);
        });
    }

    private FLabel createNameRandomizer() {
        final FLabel newNameBtn = new FLabel.Builder().tooltip(localizer.getMessage("lblGetaNewRandomName")).iconInBackground(false)
                .icon(FSkin.getIcon(FSkinProp.ICO_EDIT)).hoverable(true).opaque(false)
                .unhoveredAlpha(0.9f).build();
        newNameBtn.setCommand((UiCommand) () -> {
            final String newName = lobby.getNewName();
            if (null == newName) {
                return;
            }
            txtPlayerName.setText(newName);

            if (index == 0) {
                prefs.setPref(FPref.PLAYER_NAME, newName);
                prefs.save();
            }
            txtPlayerName.requestFocus();
            lobby.changePlayerFocus(index);
        });
        newNameBtn.addFocusListener(nameFocusListener);
        return newNameBtn;
    }

    private void createNameEditor() {
        String name;
        if (index == 0) {
            name = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
            if (name.isEmpty()) {
                name = localizer.getMessage("lblHuman");
            }
        }
        else {
            name = NameGenerator.getRandomName("Any", "Any", lobby.getPlayerNames());
        }

        txtPlayerName.setText(name);
        txtPlayerName.setFocusable(true);
        txtPlayerName.setFont(FSkin.getRelativeFont(14));
        txtPlayerName.addActionListener(lobby.nameListener);
        txtPlayerName.addFocusListener(nameFocusListener);
    }

    private FLabel createCloseButton() {
        final FLabel closeBtn = new FLabel.Builder().tooltip(localizer.getMessage("lblRemove")).iconInBackground(false)
                .icon(FSkin.getIcon(FSkinProp.ICO_CLOSE)).hoverable(true).build();
        closeBtn.setCommand((Runnable) () -> {
            if (type == LobbySlotType.REMOTE && !SOptionPane.showConfirmDialog(String.format(localizer.getMessage("lblReallyKick"), playerName), localizer.getMessage("lblKick"), false)) {
                return;
            }
            lobby.removePlayer(index);
        });
        return closeBtn;
    }

    private void createAvatar() {
        final String[] currentPrefs = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");
        if (index < currentPrefs.length) {
            avatarIndex = Integer.parseInt(currentPrefs[index]);
            avatarLabel.setIcon(FSkin.getAvatars().get(avatarIndex));
        } else {
            setRandomAvatar(false);
        }

        avatarLabel.setToolTipText(localizer.getMessage("ttlblAvatar"));
        avatarLabel.addFocusListener(avatarFocusListener);
        avatarLabel.addMouseListener(avatarMouseListener);
    }

    private void createSleeve() {
        final String[] currentPrefs = FModel.getPreferences().getPref(FPref.UI_SLEEVES).split(",");
        if (index < currentPrefs.length) {
            sleeveIndex = Integer.parseInt(currentPrefs[index]);
            sleeveLabel.setIcon(FSkin.getSleeves().get(sleeveIndex));
        } else {
            setRandomSleeve(false);
        }

        sleeveLabel.setToolTipText("L-click: Select sleeve. R-click: Randomize sleeve.");
        sleeveLabel.addFocusListener(sleeveFocusListener);
        sleeveLabel.addMouseListener(sleeveMouseListener);
    }

    /** Applies a random avatar, avoiding avatars already used. */
    private void setRandomAvatar() {
        setRandomAvatar(true);
    }
    private void setRandomAvatar(final boolean fireListeners) {
        int random = 0;

        final List<Integer> usedAvatars = lobby.getUsedAvatars();
        do {
            random = MyRandom.getRandom().nextInt(FSkin.getAvatars().size());
        } while (usedAvatars.contains(random));
        setAvatarIndex(random);

        if (fireListeners) {
            lobby.firePlayerChangeListener(index);
        }
    }

    /** Applies a random sleeve, avoiding sleeve already used. */
    private void setRandomSleeve() {
        setRandomSleeve(true);
    }
    private void setRandomSleeve(final boolean fireListeners) {
        int random = 0;

        final List<Integer> usedSleeves = lobby.getUsedSleeves();
        do {
            random = MyRandom.getRandom().nextInt(FSkin.getSleeves().size());
        } while (usedSleeves.contains(random));
        setSleeveIndex(random);

        if (fireListeners) {
            lobby.firePlayerChangeListener(index);
        }
    }

    private final FSkin.LineSkinBorder focusedBorder = new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS).alphaColor(255), 3);
    private final FSkin.LineSkinBorder defaultBorder = new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_THEME).alphaColor(200), 2);

    public void setFocused(final boolean focused) {
        avatarLabel.setBorder(focused ? focusedBorder : defaultBorder);
    }

    String getPlayerName() {
        return txtPlayerName.getText();
    }
    public void setPlayerName(final String string) {
        playerName = string;
        txtPlayerName.setText(string);
    }

    public int getAvatarIndex() {
        return avatarIndex;
    }
    public void setAvatarIndex(final int avatarIndex0) {
        avatarIndex = avatarIndex0;
        final SkinImage icon = FSkin.getAvatars().get(avatarIndex);
        avatarLabel.setIcon(icon);
        avatarLabel.repaintSelf();
    }

    public int getSleeveIndex() {
        return sleeveIndex;
    }
    public void setSleeveIndex(final int sleeveIndex0) {
        sleeveIndex = sleeveIndex0;
        final SkinImage icon = FSkin.getSleeves().get(sleeveIndex);
        sleeveLabel.setIcon(icon);
        sleeveLabel.repaintSelf();
    }

    public int getTeam() {
        return teamComboBox.getSelectedIndex();
    }
    public void setTeam(final int team) {
        teamComboBox.suppressActionListeners();
        teamComboBox.setSelectedIndex(team);
        teamComboBox.unsuppressActionListeners();
    }

    public int getArchenemyTeam() {
        return aeTeamComboBox.getSelectedIndex();
    }
    public void setIsArchenemy(final boolean isArchenemy) {
        aeTeamComboBox.suppressActionListeners();
        aeTeamComboBox.setSelectedIndex(isArchenemy ? 0 : 1);
        aeTeamComboBox.unsuppressActionListeners();
    }

    public boolean isReady() {
        return chkReady.isSelected();
    }
    public void setIsReady(final boolean isReady) {
        chkReady.setSelected(isReady);
    }

    public boolean isDevMode() {
        return chkDevMode != null && chkDevMode.isSelected();
    }
    public void setIsDevMode(final boolean isDevMode) {
        if (chkDevMode != null) {
            chkDevMode.setSelected(isDevMode);
        }
    }

    public void setMayEdit(final boolean mayEdit) {
        this.mayEdit = mayEdit;
    }

    public void setMayControl(final boolean mayControl) {
        this.mayControl = mayControl;
    }

    public void setMayRemove(final boolean mayRemove) {
        this.mayRemove = mayRemove;
    }

    FDeckChooser getDeckChooser() {
        return deckChooser;
    }

    void setDeckChooser(final FDeckChooser deckChooser) {
        this.deckChooser = deckChooser;
    }

    public void setAiProfile(String aiProfile) {
        this.aiProfile = aiProfile;
        if (aiProfile != null) {
            aiPickerComboBox.setSelectedItem(aiProfile);
        }
    }

    public String getAiProfile() {
        final Object selection = aiPickerComboBox.getSelectedItem();
        if (selection != null) {
            return selection.toString();
        }
        return aiProfile;
    }
}