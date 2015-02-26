package forge.screens.home;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;

import forge.Singletons;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorCommander;
import forge.screens.deckeditor.controllers.CEditorVariant;
import forge.screens.home.sanctioned.AvatarSelector;
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
import forge.util.MyRandom;
import forge.util.NameGenerator;
import forge.util.gui.SOptionPane;

@SuppressWarnings("serial")
public class PlayerPanel extends FPanel {
    private final static ForgePreferences prefs = FModel.getPreferences();
    private static final SkinColor unfocusedPlayerOverlay = FSkin.getColor(FSkin.Colors.CLR_OVERLAY).alphaColor(120);

    private final int index;
    private final boolean allowRemote;

    private final FLabel nameRandomiser;
    private final FLabel avatarLabel = new FLabel.Builder().opaque(true).hoverable(true).iconScaleFactor(0.99f).iconInBackground(true).build();
    private int avatarIndex;

    private final FTextField txtPlayerName = new FTextField.Builder().text("Player name").build();
    private FRadioButton radioHuman;
    private FRadioButton radioAi;
    private JCheckBoxMenuItem radioAiUseSimulation;
    private FRadioButton radioOpen;
    /** Whether this panel is occupied by a remote player. */
    private boolean isRemote;

    private FComboBoxWrapper<Object> teamComboBox = new FComboBoxWrapper<Object>();
    private FComboBoxWrapper<Object> aeTeamComboBox = new FComboBoxWrapper<Object>();

    private final FLabel deckBtn = new FLabel.ButtonBuilder().text("Select a deck").build();
    private final FLabel deckLabel;

    private final String variantBtnConstraints = "height 30px, hidemode 3";

    private boolean playerIsArchenemy = false;
    private final FLabel scmDeckSelectorBtn = new FLabel.ButtonBuilder().text("Select a scheme deck").build();
    private final FLabel scmDeckEditor = new FLabel.ButtonBuilder().text("Scheme Deck Editor").build();
    private final FLabel scmLabel;

    private final FLabel cmdDeckSelectorBtn = new FLabel.ButtonBuilder().text("Select a Commander deck").build();
    private final FLabel cmdDeckEditor = new FLabel.ButtonBuilder().text("Commander Deck Editor").build();
    private final FLabel cmdLabel;

    private final FLabel pchDeckSelectorBtn = new FLabel.ButtonBuilder().text("Select a planar deck").build();
    private final FLabel pchDeckEditor = new FLabel.ButtonBuilder().text("Planar Deck Editor").build();
    private final FLabel pchLabel;

    private final FLabel vgdSelectorBtn = new FLabel.ButtonBuilder().text("Select a Vanguard avatar").build();
    private final FLabel vgdLabel;

    private final VLobby lobby;
    public PlayerPanel(final VLobby lobby, final int index, final boolean allowRemote) {
        super();
        this.lobby = lobby;
        this.deckLabel = lobby.newLabel("Deck:");
        this.scmLabel = lobby.newLabel("Scheme deck:");
        this.cmdLabel = lobby.newLabel("Commander deck:");
        this.pchLabel = lobby.newLabel("Planar deck:");
        this.vgdLabel = lobby.newLabel("Vanguard:");

        this.index = index;
        this.allowRemote = allowRemote;
        this.playerIsArchenemy = index == 0;

        setLayout(new MigLayout("insets 10px, gap 5px"));

        // Add a button to players 3+ (or if server) to remove them from the setup
        if (index >= 2 || allowRemote) {
            FLabel closeBtn = createCloseButton();
            this.add(closeBtn, "w 20, h 20, pos (container.w-20) 0");
        }

        createAvatar();
        this.add(avatarLabel, "spany 2, width 80px, height 80px");

        createNameEditor();
        this.add(lobby.newLabel("Name:"), "w 40px, h 30px, gaptop 5px");
        this.add(txtPlayerName, "h 30px, pushx, growx");

        nameRandomiser = createNameRandomizer();
        this.add(nameRandomiser, "h 30px, w 30px, gaptop 5px");

        createPlayerTypeOptions();
        this.add(radioHuman, "gapright 5px");
        this.add(radioAi, "wrap");

        this.add(lobby.newLabel("Team:"), "w 40px, h 30px");
        populateTeamsComboBoxes();
        teamComboBox.addActionListener(teamListener);
        aeTeamComboBox.addActionListener(teamListener);
        teamComboBox.addTo(this, variantBtnConstraints + ", pushx, growx, gaptop 5px");
        aeTeamComboBox.addTo(this, variantBtnConstraints + ", pushx, growx, gaptop 5px");
        if (allowRemote) {
            this.add(radioOpen, "gapleft 1px");
        }

        this.add(deckLabel, variantBtnConstraints + ", cell 0 2, sx 2, ax right");
        this.add(deckBtn, variantBtnConstraints + ", cell 2 2, pushx, growx, wmax 100%-153px, h 30px, spanx 4, wrap");

        addHandlersDeckSelector();

        this.add(cmdLabel, variantBtnConstraints + ", cell 0 3, sx 2, ax right");
        this.add(cmdDeckSelectorBtn, variantBtnConstraints + ", cell 2 3, growx, pushx");
        this.add(cmdDeckEditor, variantBtnConstraints + ", cell 3 3, sx 3, growx, wrap");

        this.add(scmLabel, variantBtnConstraints + ", cell 0 4, sx 2, ax right");
        this.add(scmDeckSelectorBtn, variantBtnConstraints + ", cell 2 4, growx, pushx");
        this.add(scmDeckEditor, variantBtnConstraints + ", cell 3 4, sx 3, growx, wrap");

        this.add(pchLabel, variantBtnConstraints + ", cell 0 5, sx 2, ax right");
        this.add(pchDeckSelectorBtn, variantBtnConstraints + ", cell 2 5, growx, pushx");
        this.add(pchDeckEditor, variantBtnConstraints + ", cell 3 5, sx 3, growx, wrap");

        this.add(vgdLabel, variantBtnConstraints + ", cell 0 6, sx 2, ax right");
        this.add(vgdSelectorBtn, variantBtnConstraints + ", cell 2 6, sx 4, growx, wrap");

        addHandlersToVariantsControls();
        updateVariantControlsVisibility();

        this.addMouseListener(new FMouseAdapter() {
            @Override public final void onLeftMouseDown(final MouseEvent e) {
                avatarLabel.requestFocusInWindow();
            }
        });

        update();
    }

    private void update() {
        final boolean enableComponents = !(isOpen() || isRemote());
        avatarLabel.setEnabled(enableComponents);
        txtPlayerName.setEnabled(enableComponents);
        nameRandomiser.setEnabled(enableComponents);
        deckLabel.setVisible(enableComponents);
        deckBtn.setVisible(enableComponents);
    }

    private final FMouseAdapter radioMouseAdapter = new FMouseAdapter() {
        @Override public final void onLeftClick(final MouseEvent e) {
            avatarLabel.requestFocusInWindow();
            lobby.updateVanguardList(index);
            update();
        }
    };

    /** Listens to name text fields and gives the appropriate player focus.
     *  Also saves the name preference when leaving player one's text field. */
    private FocusAdapter nameFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            lobby.changePlayerFocus(index);
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

    /** Listens to avatar buttons and gives the appropriate player focus. */
    private FocusAdapter avatarFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            lobby.changePlayerFocus(index);
        }
    };

    private FMouseAdapter avatarMouseListener = new FMouseAdapter() {
        @Override public final void onLeftClick(final MouseEvent e) {
            if (!avatarLabel.isEnabled()) {
                return;
            }

            final FLabel avatar = (FLabel)e.getSource();

            PlayerPanel.this.lobby.changePlayerFocus(index);
            avatar.requestFocusInWindow();

            final AvatarSelector aSel = new AvatarSelector(getPlayerName(), avatarIndex, PlayerPanel.this.lobby.getUsedAvatars());
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
                PlayerPanel.this.lobby.updateAvatarPrefs();
            }
        }
        @Override public final void onRightClick(final MouseEvent e) {
            if (!avatarLabel.isEnabled()) {
                return;
            }

            PlayerPanel.this.lobby.changePlayerFocus(index);
            avatarLabel.requestFocusInWindow();

            setRandomAvatar();

            if (index < 2) {
                PlayerPanel.this.lobby.updateAvatarPrefs();
            }
        }
    };

    public void updateVariantControlsVisibility() {
        boolean isCommanderApplied = false;
        boolean isPlanechaseApplied = false;
        boolean isVanguardApplied = false;
        boolean isArchenemyApplied = false;
        boolean archenemyVisiblity = false;
        boolean isDeckBuildingAllowed = true;
        
        for (final GameType variant : lobby.getAppliedVariants()) {
            switch (variant) {
            case Archenemy:
                isArchenemyApplied = true;
                if (playerIsArchenemy) {
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

        deckLabel.setVisible(isDeckBuildingAllowed);
        deckBtn.setVisible(isDeckBuildingAllowed);
        cmdDeckSelectorBtn.setVisible(isCommanderApplied);
        cmdDeckEditor.setVisible(isCommanderApplied);
        cmdLabel.setVisible(isCommanderApplied);

        scmDeckSelectorBtn.setVisible(archenemyVisiblity);
        scmDeckEditor.setVisible(archenemyVisiblity);
        scmLabel.setVisible(archenemyVisiblity);

        teamComboBox.setVisible(!isArchenemyApplied);
        aeTeamComboBox.setVisible(isArchenemyApplied);
        aeTeamComboBox.setEnabled(!(isArchenemyApplied && playerIsArchenemy));

        pchDeckSelectorBtn.setVisible(isPlanechaseApplied);
        pchDeckEditor.setVisible(isPlanechaseApplied);
        pchLabel.setVisible(isPlanechaseApplied);

        vgdSelectorBtn.setVisible(isVanguardApplied);
        vgdLabel.setVisible(isVanguardApplied);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (lobby.getPlayerPanelWithFocus() != this) {
            FSkin.setGraphicsColor(g, unfocusedPlayerOverlay);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }

    int getIndex() {
        return index;
    }

    public boolean isAi() {
        return radioAi.isSelected();
    }

    public boolean isSimulatedAi() {
        return radioAi.isSelected() && radioAiUseSimulation.isSelected();
    }

    public boolean isOpen() {
        return radioOpen.isSelected() && !isRemote;
    }

    public boolean isArchenemy() {
        return playerIsArchenemy;
    }

    public boolean isRemote() {
        return isRemote;
    }

    public void setRemote(final boolean remote) {
        isRemote = remote;
        update();
    }

    public void setVanguardButtonText(String text) {
        vgdSelectorBtn.setText(text);
    }

    public void setDeckSelectorButtonText(String text) {
        deckBtn.setText(text);
    }

    public void focusOnAvatar() {
        avatarLabel.requestFocusInWindow();
    }

    private void populateTeamsComboBoxes() {
        aeTeamComboBox.addItem("Archenemy");
        aeTeamComboBox.addItem("Heroes");
        aeTeamComboBox.setSelectedIndex(lobby.getArchenemyTeams().get(index) - 1);
        aeTeamComboBox.setEnabled(playerIsArchenemy);

        for (int i = 1; i <= VLobby.MAX_PLAYERS; i++) {
            teamComboBox.addItem(i);
        }
        teamComboBox.setSelectedIndex(lobby.getTeams().get(index) - 1);
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
            if (PlayerPanel.this.lobby.getAppliedVariants().contains(GameType.Archenemy)) {
                String sel = (String) selection;
                if (sel.contains("Archenemy")) {
                    PlayerPanel.this.lobby.setLastArchenemy(index);
                    for (PlayerPanel pp : PlayerPanel.this.lobby.getPlayerPanels()) {
                        int i = pp.index;
                        PlayerPanel.this.lobby.getArchenemyTeams().set(i, i == PlayerPanel.this.lobby.getLastArchenemy() ? 1 : 2);
                        pp.aeTeamComboBox.setSelectedIndex(i == PlayerPanel.this.lobby.getLastArchenemy() ? 0 : 1);
                        pp.toggleIsPlayerArchenemy();
                    }
                }
            } else {
                Integer sel = (Integer) selection;
                PlayerPanel.this.lobby.getTeams().set(index, sel);
            }

            PlayerPanel.this.lobby.changePlayerFocus(index);
        }
    };

    public void toggleIsPlayerArchenemy() {
        if (lobby.getAppliedVariants().contains(GameType.Archenemy)) {
            playerIsArchenemy = lobby.getLastArchenemy() == index;
        } else {
            playerIsArchenemy = lobby.getAppliedVariants().contains(GameType.ArchenemyRumble);
        }
        updateVariantControlsVisibility();
    }

    /**
     * @param index
     */
    private void addHandlersToVariantsControls() {
    	// Archenemy buttons
        scmDeckSelectorBtn.setCommand(new Runnable() {
            @Override
            public void run() {
                PlayerPanel.this.lobby.setCurrentGameMode(PlayerPanel.this.lobby.getVntArchenemy().isSelected() ? GameType.Archenemy : GameType.ArchenemyRumble);
                scmDeckSelectorBtn.requestFocusInWindow();
                PlayerPanel.this.lobby.changePlayerFocus(index, PlayerPanel.this.lobby.getCurrentGameMode());
            }
        });

        scmDeckEditor.setCommand(new UiCommand() {
            @Override
            public void run() {
                PlayerPanel.this.lobby.setCurrentGameMode(PlayerPanel.this.lobby.getVntArchenemy().isSelected() ? GameType.Archenemy : GameType.ArchenemyRumble);
                Predicate<PaperCard> predSchemes = new Predicate<PaperCard>() {
                    @Override
                    public boolean apply(PaperCard arg0) {
                        return arg0.getRules().getType().isScheme();
                    }
                };

                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_ARCHENEMY);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                        new CEditorVariant(FModel.getDecks().getScheme(), predSchemes, DeckSection.Schemes, FScreen.DECK_EDITOR_ARCHENEMY, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
            }
        });

        // Commander buttons
        cmdDeckSelectorBtn.setCommand(new Runnable() {
            @Override
            public void run() {
                PlayerPanel.this.lobby.setCurrentGameMode(PlayerPanel.this.lobby.getVntTinyLeaders().isSelected() ? GameType.TinyLeaders : GameType.Commander);
                cmdDeckSelectorBtn.requestFocusInWindow();
                PlayerPanel.this.lobby.changePlayerFocus(index, PlayerPanel.this.lobby.getCurrentGameMode());
            }
        });

        cmdDeckEditor.setCommand(new UiCommand() {
            @Override
            public void run() {
                PlayerPanel.this.lobby.setCurrentGameMode(PlayerPanel.this.lobby.getVntTinyLeaders().isSelected() ? GameType.TinyLeaders : GameType.Commander);
                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_COMMANDER);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorCommander(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
            }
        });

        // Planechase buttons
        pchDeckSelectorBtn.setCommand(new Runnable() {
            @Override
            public void run() {
                PlayerPanel.this.lobby.setCurrentGameMode(GameType.Planechase);
                pchDeckSelectorBtn.requestFocusInWindow();
                PlayerPanel.this.lobby.changePlayerFocus(index, GameType.Planechase);
            }
        });

        pchDeckEditor.setCommand(new UiCommand() {
            @Override
            public void run() {
                PlayerPanel.this.lobby.setCurrentGameMode(GameType.Planechase);
                Predicate<PaperCard> predPlanes = new Predicate<PaperCard>() {
                    @Override
                    public boolean apply(PaperCard arg0) {
                        return arg0.getRules().getType().isPlane() || arg0.getRules().getType().isPhenomenon();
                    }
                };

                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_PLANECHASE);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                        new CEditorVariant(FModel.getDecks().getPlane(), predPlanes, DeckSection.Planes, FScreen.DECK_EDITOR_PLANECHASE, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
            }
        });

        // Vanguard buttons
        vgdSelectorBtn.setCommand(new Runnable() {
            @Override
            public void run() {
                PlayerPanel.this.lobby.setCurrentGameMode(GameType.Vanguard);
                vgdSelectorBtn.requestFocusInWindow();
                PlayerPanel.this.lobby.changePlayerFocus(index, GameType.Vanguard);
            }
        });
    }

    /**
     * @param index
     */
    private void createPlayerTypeOptions() {
        radioHuman = new FRadioButton(allowRemote ? "Local" : "Human", index == 0);
        radioAi = new FRadioButton("AI", !allowRemote && index != 0);
        radioOpen = new FRadioButton("Open", allowRemote && index != 0);
        final JPopupMenu menu = new  JPopupMenu();
        radioAiUseSimulation = new JCheckBoxMenuItem("Use Simulation");
        menu.add(radioAiUseSimulation);
        radioAi.setComponentPopupMenu(menu);

        radioHuman.addMouseListener(radioMouseAdapter);
        radioAi.addMouseListener(radioMouseAdapter);
        radioOpen.addMouseListener(radioMouseAdapter);

        final ButtonGroup tempBtnGroup = new ButtonGroup();
        tempBtnGroup.add(radioHuman);
        tempBtnGroup.add(radioAi);
        tempBtnGroup.add(radioOpen);
    }

    /**
     * @param index
     */
    private void addHandlersDeckSelector() {
        deckBtn.setCommand(new Runnable() {
            @Override
            public void run() {
                PlayerPanel.this.lobby.setCurrentGameMode(GameType.Constructed);
                deckBtn.requestFocusInWindow();
                PlayerPanel.this.lobby.changePlayerFocus(index, GameType.Constructed);
            }
        });
    }

    /**
     * @param index
     * @return
     */
    private FLabel createNameRandomizer() {
        final FLabel newNameBtn = new FLabel.Builder().tooltip("Get a new random name").iconInBackground(false)
                .icon(FSkin.getIcon(FSkinProp.ICO_EDIT)).hoverable(true).opaque(false)
                .unhoveredAlpha(0.9f).build();
        newNameBtn.setCommand(new UiCommand() {
            @Override
            public void run() {
                String newName = PlayerPanel.this.lobby.getNewName();
                if (null == newName) {
                    return;
                }
                txtPlayerName.setText(newName);

                if (index == 0) {
                    prefs.setPref(FPref.PLAYER_NAME, newName);
                    prefs.save();
                }
                txtPlayerName.requestFocus();
                PlayerPanel.this.lobby.changePlayerFocus(index);
            }
        });
        newNameBtn.addFocusListener(nameFocusListener);
        return newNameBtn;
    }

    /**
     * @param index
     * @return
     */
    private void createNameEditor() {
        String name;
        if (index == 0) {
            name = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
            if (name.isEmpty()) {
                name = "Human";
            }
        }
        else {
            name = NameGenerator.getRandomName("Any", "Any", lobby.getPlayerNames());
        }

        txtPlayerName.setText(name);
        txtPlayerName.setFocusable(true);
        txtPlayerName.setFont(FSkin.getFont(14));
        txtPlayerName.addActionListener(lobby.nameListener);
        txtPlayerName.addFocusListener(nameFocusListener);
    }

    private FLabel createCloseButton() {
        final FLabel closeBtn = new FLabel.Builder().tooltip("Remove").iconInBackground(false)
                .icon(FSkin.getIcon(FSkinProp.ICO_CLOSE)).hoverable(true).build();
        closeBtn.setCommand(new Runnable() {
            @Override public final void run() {
                if (isRemote() && !SOptionPane.showConfirmDialog("Really kick player?", "Kick", false)) {
                    return;
                }
                PlayerPanel.this.lobby.removePlayer(index);
            }
        });
        return closeBtn;
    }

    private void createAvatar() {
        String[] currentPrefs = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");
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

    /** Applies a random avatar, avoiding avatars already used.
     * @param playerIndex */
    public void setRandomAvatar() {
        int random = 0;

        List<Integer> usedAvatars = lobby.getUsedAvatars();
        do {
            random = MyRandom.getRandom().nextInt(FSkin.getAvatars().size());
        } while (usedAvatars.contains(random));
        setAvatar(random);
    }

    public void setAvatar(int newAvatarIndex) {
        avatarIndex = newAvatarIndex;
        SkinImage icon = FSkin.getAvatars().get(newAvatarIndex);
        avatarLabel.setIcon(icon);
        avatarLabel.repaintSelf();
    }

    private final FSkin.LineSkinBorder focusedBorder = new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS).alphaColor(255), 3);
    private final FSkin.LineSkinBorder defaultBorder = new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_THEME).alphaColor(200), 2);

    public void setFocused(boolean focused) {
        avatarLabel.setBorder(focused ? focusedBorder : defaultBorder);
        avatarLabel.setHoverable(focused);
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