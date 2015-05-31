package forge.screens.home;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

import forge.AIOption;
import forge.Singletons;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.match.LobbySlot;
import forge.match.LobbySlotType;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorCommander;
import forge.screens.deckeditor.controllers.CEditorVariant;
import forge.screens.home.sanctioned.AvatarSelector;
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
import forge.util.MyRandom;
import forge.util.NameGenerator;
import forge.util.gui.SOptionPane;

@SuppressWarnings("serial")
public class PlayerPanel extends FPanel {
    private final static ForgePreferences prefs = FModel.getPreferences();
    private static final SkinColor unfocusedPlayerOverlay = FSkin.getColor(FSkin.Colors.CLR_OVERLAY).alphaColor(120);

    private LobbySlotType type;
    private final int index;
    private String playerName = StringUtils.EMPTY;
    private boolean mayEdit, mayControl, mayRemove;

    private final FLabel nameRandomiser;
    private final FLabel avatarLabel = new FLabel.Builder().opaque(true).hoverable(true).iconScaleFactor(0.99f).iconInBackground(true).build();
    private int avatarIndex;

    private final FTextField txtPlayerName = new FTextField.Builder().build();
    private FRadioButton radioHuman;
    private FRadioButton radioAi;
    private JCheckBoxMenuItem radioAiUseSimulation;
    private FRadioButton radioOpen;
    private FCheckBox chkReady;

    private final FComboBoxWrapper<Object> teamComboBox = new FComboBoxWrapper<Object>();
    private final FComboBoxWrapper<Object> aeTeamComboBox = new FComboBoxWrapper<Object>();

    private final FLabel closeBtn;
    private final FLabel deckBtn = new FLabel.ButtonBuilder().text("Select a deck").build();
    private final FLabel deckLabel;

    private final String variantBtnConstraints = "height 30px, hidemode 3";

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
    public PlayerPanel(final VLobby lobby, final boolean allowNetworking, final int index, final LobbySlot slot, final boolean mayEdit, final boolean mayControl) {
        super();

        this.lobby = lobby;
        this.index = index;
        this.mayEdit = mayEdit;
        this.mayControl = mayControl;

        this.deckLabel = lobby.newLabel("Deck:");
        this.scmLabel = lobby.newLabel("Scheme deck:");
        this.cmdLabel = lobby.newLabel("Commander deck:");
        this.pchLabel = lobby.newLabel("Planar deck:");
        this.vgdLabel = lobby.newLabel("Vanguard:");

        setLayout(new MigLayout("insets 10px, gap 5px"));

        // Add a button to players 3+ (or if server) to remove them from the setup
        closeBtn = createCloseButton();
        this.add(closeBtn, "w 20, h 20, pos (container.w-20) 0");

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

        // Set these before action listeners are added
        this.setTeam(slot == null ? index : slot.getTeam());
        this.setIsArchenemy(slot != null && slot.isArchenemy());

        teamComboBox.addActionListener(teamListener);
        aeTeamComboBox.addActionListener(teamListener);
        teamComboBox.addTo(this, variantBtnConstraints + ", pushx, growx, gaptop 5px");
        aeTeamComboBox.addTo(this, variantBtnConstraints + ", pushx, growx, gaptop 5px");

        createReadyButton();
        if (allowNetworking) {
            this.add(radioOpen, "cell 4 1, ax left, sx 2");
            this.add(chkReady, "cell 5 1, ax left, sx 2, wrap");
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

        this.addMouseListener(new FMouseAdapter() {
            @Override public final void onLeftMouseDown(final MouseEvent e) {
                avatarLabel.requestFocusInWindow();
            }
        });

        this.type = slot == null ? LobbySlotType.LOCAL : slot.getType();
        this.setPlayerName(slot == null ? "" : slot.getName());
        this.setAvatarIndex(slot == null ? 0 : slot.getAvatarIndex());

        update();
    }

    void update() {
        avatarLabel.setEnabled(mayEdit);
        avatarLabel.setIcon(FSkin.getAvatars().get(Integer.valueOf(type == LobbySlotType.OPEN ? -1 : avatarIndex)));
        avatarLabel.repaintSelf();

        txtPlayerName.setEnabled(mayEdit);
        txtPlayerName.setText(type == LobbySlotType.OPEN ? StringUtils.EMPTY : playerName);
        nameRandomiser.setEnabled(mayEdit);
        deckLabel.setVisible(mayEdit);
        deckBtn.setVisible(mayEdit);
        chkReady.setVisible(type == LobbySlotType.LOCAL || type == LobbySlotType.REMOTE);
        chkReady.setEnabled(mayEdit);

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

    private final FMouseAdapter radioMouseAdapter(final FRadioButton source, final LobbySlotType type) {
        return new FMouseAdapter() {
            @Override public final void onLeftClick(final MouseEvent e) {
                if (!source.isEnabled()) {
                    return;
                }
                setType(type);
                lobby.firePlayerChangeListener(index);
                avatarLabel.requestFocusInWindow();
                lobby.updateVanguardList(index);
            }
        };
    };

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
        @Override public final void onLeftClick(final MouseEvent e) {
            if (!avatarLabel.isEnabled()) {
                return;
            }

            final FLabel avatar = (FLabel)e.getSource();

            lobby.changePlayerFocus(index);
            avatar.requestFocusInWindow();

            final AvatarSelector aSel = new AvatarSelector(playerName, avatarIndex, lobby.getUsedAvatars());
            for (final FLabel lbl : aSel.getSelectables()) {
                lbl.setCommand(new UiCommand() {
                    @Override
                    public void run() {
                        setAvatarIndex(Integer.valueOf(lbl.getName().substring(11)));
                        aSel.setVisible(false);
                    }
                });
            }

            aSel.setVisible(true);
            aSel.dispose();

            if (index < 2) {
                lobby.updateAvatarPrefs();
            }

            lobby.firePlayerChangeListener(index);
        }

        @Override public final void onRightClick(final MouseEvent e) {
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

    private void updateVariantControlsVisibility() {
        final boolean isCommanderApplied = mayEdit && (lobby.hasVariant(GameType.Commander) || lobby.hasVariant(GameType.TinyLeaders));
        final boolean isPlanechaseApplied = mayEdit && lobby.hasVariant(GameType.Planechase);
        final boolean isVanguardApplied = mayEdit && lobby.hasVariant(GameType.Vanguard);
        final boolean isArchenemyApplied = mayEdit && lobby.hasVariant(GameType.Archenemy);
        final boolean archenemyVisiblity = mayEdit && lobby.hasVariant(GameType.ArchenemyRumble) || (isArchenemyApplied && isArchenemy());
        // Commander deck building replaces normal one, so hide it
        final boolean isDeckBuildingAllowed = mayEdit && !isCommanderApplied && !lobby.hasVariant(GameType.MomirBasic);

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
                : Collections.<AIOption>emptySet();
    }
    private boolean isSimulatedAi() {
        return radioAi.isSelected() && radioAiUseSimulation.isSelected();
    }
    public void setUseAiSimulation(final boolean useSimulation) {
        radioAiUseSimulation.setSelected(useSimulation);
    }

    public boolean isLocal() {
        return type == LobbySlotType.LOCAL;
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

    public void focusOnAvatar() {
        avatarLabel.requestFocusInWindow();
    }

    private void populateTeamsComboBoxes() {
        aeTeamComboBox.addItem("Archenemy");
        aeTeamComboBox.addItem("Heroes");

        for (int i = 1; i <= VLobby.MAX_PLAYERS; i++) {
            teamComboBox.addItem(i);
        }
        teamComboBox.setEnabled(true);
    }

    private final ActionListener teamListener = new ActionListener() {
        @SuppressWarnings("unchecked")
        @Override public final void actionPerformed(final ActionEvent e) {
            final FComboBox<Object> cb = (FComboBox<Object>) e.getSource();
            cb.requestFocusInWindow();
            final Object selection = cb.getSelectedItem();

            if (null != selection) {
                lobby.changePlayerFocus(index);
                lobby.firePlayerChangeListener(index);
            }
        }
    };

    /**
     * @param index
     */
    private void addHandlersToVariantsControls() {
        // Archenemy buttons
        scmDeckSelectorBtn.setCommand(new Runnable() {
            @Override public final void run() {
                lobby.setCurrentGameMode(lobby.hasVariant(GameType.Archenemy) ? GameType.Archenemy : GameType.ArchenemyRumble);
                scmDeckSelectorBtn.requestFocusInWindow();
                lobby.changePlayerFocus(index);
            }
        });

        scmDeckEditor.setCommand(new UiCommand() {
            @Override
            public void run() {
                lobby.setCurrentGameMode(lobby.hasVariant(GameType.Archenemy) ? GameType.Archenemy : GameType.ArchenemyRumble);
                final Predicate<PaperCard> predSchemes = new Predicate<PaperCard>() {
                    @Override public final boolean apply(final PaperCard arg0) {
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
                lobby.setCurrentGameMode(lobby.hasVariant(GameType.TinyLeaders) ? GameType.TinyLeaders : GameType.Commander);
                cmdDeckSelectorBtn.requestFocusInWindow();
                lobby.changePlayerFocus(index);
            }
        });

        cmdDeckEditor.setCommand(new UiCommand() {
            @Override
            public void run() {
                lobby.setCurrentGameMode(lobby.hasVariant(GameType.TinyLeaders) ? GameType.TinyLeaders : GameType.Commander);
                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_COMMANDER);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorCommander(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
            }
        });

        // Planechase buttons
        pchDeckSelectorBtn.setCommand(new Runnable() {
            @Override
            public void run() {
                lobby.setCurrentGameMode(GameType.Planechase);
                pchDeckSelectorBtn.requestFocusInWindow();
                lobby.changePlayerFocus(index, GameType.Planechase);
            }
        });

        pchDeckEditor.setCommand(new UiCommand() {
            @Override
            public void run() {
                lobby.setCurrentGameMode(GameType.Planechase);
                final Predicate<PaperCard> predPlanes = new Predicate<PaperCard>() {
                    @Override
                    public boolean apply(final PaperCard arg0) {
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
                lobby.setCurrentGameMode(GameType.Vanguard);
                vgdSelectorBtn.requestFocusInWindow();
                lobby.changePlayerFocus(index, GameType.Vanguard);
            }
        });
    }

    /**
     * @param index
     */
    private void createPlayerTypeOptions() {
        radioHuman = new FRadioButton("Human");
        radioAi = new FRadioButton("AI");
        radioOpen = new FRadioButton("Open");

        final JPopupMenu menu = new  JPopupMenu();
        radioAiUseSimulation = new JCheckBoxMenuItem("Use Simulation");
        menu.add(radioAiUseSimulation);
        radioAiUseSimulation.addActionListener(new ActionListener() {
            @Override public final void actionPerformed(final ActionEvent e) {
                lobby.firePlayerChangeListener(index);
            } });
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
        chkReady = new FCheckBox("Ready");
        chkReady.addActionListener(new ActionListener() {
            @Override public final void actionPerformed(final ActionEvent e) {
                lobby.setReady(index, chkReady.isSelected());
            }
        });
    }

    /**
     * @param index
     */
    private void addHandlersDeckSelector() {
        deckBtn.setCommand(new Runnable() {
            @Override
            public void run() {
                lobby.setCurrentGameMode(GameType.Constructed);
                deckBtn.requestFocusInWindow();
                lobby.changePlayerFocus(index, GameType.Constructed);
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
                if (type == LobbySlotType.REMOTE && !SOptionPane.showConfirmDialog(String.format("Really kick %s?", playerName), "Kick", false)) {
                    return;
                }
                lobby.removePlayer(index);
            }
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

        avatarLabel.setToolTipText("L-click: Select avatar. R-click: Randomize avatar.");
        avatarLabel.addFocusListener(avatarFocusListener);
        avatarLabel.addMouseListener(avatarMouseListener);
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

    public void setMayEdit(final boolean mayEdit) {
        this.mayEdit = mayEdit;
    }

    public void setMayControl(final boolean mayControl) {
        this.mayControl = mayControl;
    }

    public void setMayRemove(final boolean mayRemove) {
        this.mayRemove = mayRemove;
    }
}