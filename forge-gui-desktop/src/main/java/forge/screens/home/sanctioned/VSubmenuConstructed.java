package forge.screens.home.sanctioned;

import com.google.common.base.Predicate;

import forge.UiCommand;
import forge.Singletons;
import forge.assets.FSkinProp;
import forge.deck.DeckSection;
import forge.deck.DeckType;
import forge.game.GameType;
import forge.game.card.Card;
import forge.gui.CardDetailPanel;
import forge.deck.DeckProxy;
import forge.deckchooser.DecksComboBoxEvent;
import forge.deckchooser.FDeckChooser;
import forge.deckchooser.IDecksComboBoxListener;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorCommander;
import forge.screens.deckeditor.controllers.CEditorVariant;
import forge.screens.home.*;
import forge.toolbox.*;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinImage;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.NameGenerator;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuConstructed implements IVSubmenu<CSubmenuConstructed> {
    SINGLETON_INSTANCE;
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final SkinColor unfocusedPlayerOverlay = FSkin.getColor(FSkin.Colors.CLR_OVERLAY).alphaColor(120);

    private final static int MAX_PLAYERS = 8;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Constructed Mode");

    // General variables
    private final LblHeader lblTitle = new LblHeader("Sanctioned Format: Constructed");
    private int activePlayersNum = 2;
    private int playerWithFocus = 0; // index of the player that currently has focus
    private PlayerPanel playerPanelWithFocus;
    private GameType currentGameMode = GameType.Constructed;
    private List<Integer> teams = new ArrayList<Integer>(MAX_PLAYERS);
    private List<Integer> archenemyTeams = new ArrayList<Integer>(MAX_PLAYERS);

    private final StartButton btnStart  = new StartButton();
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    private final JPanel constructedFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap 2")); // Main content frame

    // Variants frame and variables
    private final Set<GameType> appliedVariants = new TreeSet<GameType>();
	private final FPanel variantsPanel = new FPanel(new MigLayout("insets 10, gapx 10"));
    private final FCheckBox vntVanguard = new FCheckBox("Vanguard");
    private final FCheckBox vntMomirBasic = new FCheckBox("Momir Basic");
    private final FCheckBox vntCommander = new FCheckBox("Commander");
    private final FCheckBox vntPlanechase = new FCheckBox("Planechase");
    private final FCheckBox vntArchenemy = new FCheckBox("Archenemy");
    private final FCheckBox vntArchenemyRumble = new FCheckBox("Archenemy Rumble");

    // Player frame elements
    private final JPanel playersFrame = new JPanel(new MigLayout("insets 0, gap 0 5, wrap, hidemode 3"));
    private final FScrollPanel playersScroll = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"), true);
    private final List<PlayerPanel> playerPanels = new ArrayList<PlayerPanel>(MAX_PLAYERS);

    private final List<FLabel> closePlayerBtnList = new ArrayList<FLabel>(6);
    private final FLabel addPlayerBtn = new FLabel.ButtonBuilder().fontSize(14).text("Add a Player").build();

    // Deck frame elements
    private final JPanel decksFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
    private final List<FDeckChooser> deckChoosers = new ArrayList<FDeckChooser>(8);
    private final FCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final FCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");

    // Variants
    private final List<FList<Object>> schemeDeckLists = new ArrayList<FList<Object>>();
    private final List<FPanel> schemeDeckPanels = new ArrayList<FPanel>(MAX_PLAYERS);
    private int lastArchenemy = 0;

    private final List<FList<Object>> commanderDeckLists = new ArrayList<FList<Object>>();
    private final List<FPanel> commanderDeckPanels = new ArrayList<FPanel>(MAX_PLAYERS);

    private final List<FList<Object>> planarDeckLists = new ArrayList<FList<Object>>();
    private final List<FPanel> planarDeckPanels = new ArrayList<FPanel>(MAX_PLAYERS);

    private final List<FList<Object>> vgdAvatarLists = new ArrayList<FList<Object>>();
    private final List<FPanel> vgdPanels = new ArrayList<FPanel>(MAX_PLAYERS);
    private final List<CardDetailPanel> vgdAvatarDetails = new ArrayList<CardDetailPanel>();
    private final List<PaperCard> vgdAllAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> vgdAllAiAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> nonRandomHumanAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> nonRandomAiAvatars = new ArrayList<PaperCard>();
    private Vector<Object> humanListData = new Vector<Object>();
    private Vector<Object> aiListData = new Vector<Object>();

    // CTR
    private VSubmenuConstructed() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        ////////////////////////////////////////////////////////
        //////////////////// Variants Panel ////////////////////

        // Populate and add variants panel
        vntVanguard.addItemListener(iListenerVariants);
        vntMomirBasic.addItemListener(iListenerVariants);
        vntCommander.addItemListener(iListenerVariants);
        vntPlanechase.addItemListener(iListenerVariants);
        vntArchenemy.addItemListener(iListenerVariants);
        vntArchenemyRumble.addItemListener(iListenerVariants);

        variantsPanel.setOpaque(false);
        variantsPanel.add(newLabel("Variants:"));
        variantsPanel.add(vntVanguard);
        variantsPanel.add(vntMomirBasic);
        variantsPanel.add(vntCommander);
        variantsPanel.add(vntPlanechase);
        variantsPanel.add(vntArchenemy);
        variantsPanel.add(vntArchenemyRumble);

        constructedFrame.add(new FScrollPane(variantsPanel, false, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                "w 100%, h 45px!, gapbottom 10px, spanx 2, wrap");

        ////////////////////////////////////////////////////////
        ///////////////////// Player Panel /////////////////////

        // Construct individual player panels
        String constraints = "pushx, growx, wrap, hidemode 3";
        for (int i = 0; i < MAX_PLAYERS; i++) {
        	teams.add(i + 1);
        	archenemyTeams.add(i == 0 ? 1 : 2);

            PlayerPanel player = new PlayerPanel(i);
            playerPanels.add(player);

        	// Populate players panel
        	player.setVisible(i < activePlayersNum);

    	    playersScroll.add(player, constraints);

        	if (i == 0) {
        	    constraints += ", gaptop 5px";
        	}
        }

        playerPanelWithFocus = playerPanels.get(0);
        playerPanelWithFocus.setFocused(true);

        playersFrame.setOpaque(false);
        playersFrame.add(playersScroll, "w 100%, h 100%-35px");

        addPlayerBtn.setFocusable(true);
        addPlayerBtn.setCommand(new Runnable() {
            @Override
            public void run() {
                addPlayer();
            }
        });
        playersFrame.add(addPlayerBtn, "height 30px!, growx, pushx");

        constructedFrame.add(playersFrame, "gapright 10px, w 50%-5px, growy, pushy");

        ////////////////////////////////////////////////////////
        ////////////////////// Deck Panel //////////////////////

        for (int i = 0; i < MAX_PLAYERS; i++) {
        	buildDeckPanel(i);
        }
        constructedFrame.add(decksFrame, "w 50%-5px, growy, pushy");
        constructedFrame.setOpaque(false);
        decksFrame.setOpaque(false);

        // Start Button
        pnlStart.setOpaque(false);
        pnlStart.add(btnStart, "align center");
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
        playerPanels.get(freeIndex).focusOnAvatar();
    }

    private void removePlayer(int playerIndex) {
    	activePlayersNum--;
		FPanel player = playerPanels.get(playerIndex);
    	player.setVisible(false);
		addPlayerBtn.setEnabled(true);

		//find closest player still in game and give focus
		int min = MAX_PLAYERS;
	    int closest = 2;

	    for (int participantIndex : getParticipants()) {
	        final int diff = Math.abs(playerIndex - participantIndex);

	        if (diff < min) {
	            min = diff;
	            closest = participantIndex;
	        }
	    }

	    changePlayerFocus(closest);
	    playerPanels.get(closest).focusOnAvatar();
    }

    /** Builds the actual deck panel layouts for each player.
     * These are added to a list which can be referenced to populate the deck panel appropriately. */
    @SuppressWarnings("serial")
    private void buildDeckPanel(final int playerIndex) {
    	String sectionConstraints = "insets 0, gap 0, wrap";
        String labelConstraints = "gaptop 10px, gapbottom 5px";

        // Main deck
        final FDeckChooser mainChooser = new FDeckChooser(isPlayerAI(playerIndex));
        mainChooser.initialize();
        mainChooser.getLstDecks().setSelectCommand(new UiCommand() {
            @Override
            public void run() {
                VSubmenuConstructed.this.onDeckClicked(playerIndex, mainChooser.getSelectedDeckType(), mainChooser.getLstDecks().getSelectedItems());
            }
        });
        deckChoosers.add(mainChooser);

        // Scheme deck list
        FPanel schemeDeckPanel = new FPanel();
        schemeDeckPanel.setBorderToggle(false);
        schemeDeckPanel.setLayout(new MigLayout(sectionConstraints));
        schemeDeckPanel.add(new FLabel.Builder().text("Select Scheme deck:").build(), labelConstraints);
        FList<Object> schemeDeckList = new FList<Object>();
        schemeDeckList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        FScrollPane scrSchemes = new FScrollPane(schemeDeckList, true,
        		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        schemeDeckPanel.add(scrSchemes, "grow, push");
        schemeDeckLists.add(schemeDeckList);
        schemeDeckPanels.add(schemeDeckPanel);

        // Commander deck list
        FPanel commanderDeckPanel = new FPanel();
        commanderDeckPanel.setBorderToggle(false);
        commanderDeckPanel.setLayout(new MigLayout(sectionConstraints));
        commanderDeckPanel.add(new FLabel.Builder().text("Select Commander deck:").build(), labelConstraints);
        FList<Object> commanderDeckList = new FList<Object>();
        commanderDeckList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        FScrollPane scrCommander = new FScrollPane(commanderDeckList, true,
        		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        commanderDeckPanel.add(scrCommander, "grow, push");
        commanderDeckLists.add(commanderDeckList);
        commanderDeckPanels.add(commanderDeckPanel);

        // Planar deck list
        FPanel planarDeckPanel = new FPanel();
        planarDeckPanel.setBorderToggle(false);
        planarDeckPanel.setLayout(new MigLayout(sectionConstraints));
        planarDeckPanel.add(new FLabel.Builder().text("Select Planar deck:").build(), labelConstraints);
        FList<Object> planarDeckList = new FList<Object>();
        planarDeckList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        FScrollPane scrPlanes = new FScrollPane(planarDeckList, true,
        		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        planarDeckPanel.add(scrPlanes, "grow, push");
        planarDeckLists.add(planarDeckList);
        planarDeckPanels.add(planarDeckPanel);

        // Vanguard avatar list
        FPanel vgdDeckPanel = new FPanel();
        vgdDeckPanel.setBorderToggle(false);

        FList<Object> vgdAvatarList = new FList<Object>();
        vgdAvatarList.setListData(isPlayerAI(playerIndex) ? aiListData : humanListData);
        vgdAvatarList.setSelectedIndex(0);
        vgdAvatarList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        vgdAvatarList.addListSelectionListener(vgdLSListener);
        FScrollPane scrAvatars = new FScrollPane(vgdAvatarList, true,
        		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        CardDetailPanel vgdDetail = new CardDetailPanel(null);
        vgdAvatarDetails.add(vgdDetail);

        vgdDeckPanel.setLayout(new MigLayout(sectionConstraints));
        vgdDeckPanel.add(new FLabel.Builder().text("Select a Vanguard avatar:").build(), labelConstraints);
        vgdDeckPanel.add(scrAvatars, "grow, push");
        vgdDeckPanel.add(vgdDetail, "h 200px, pushx, growx, hidemode 3");
        vgdAvatarLists.add(vgdAvatarList);
        vgdPanels.add(vgdDeckPanel);
    }

    protected void onDeckClicked(int iPlayer, DeckType type, Collection<DeckProxy> selectedDecks) {
        String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
        playerPanels.get(iPlayer).setDeckSelectorButtonText(text);
    }

    /** Populates the deck panel with the focused player's deck choices. */
    private void populateDeckPanel(final GameType forGameType) {
    	decksFrame.removeAll();

    	if (GameType.Constructed == forGameType) {
    		decksFrame.add(deckChoosers.get(playerWithFocus), "grow, push");
    		if (deckChoosers.get(playerWithFocus).getSelectedDeckType().toString().contains("Random")) {
    			final String strCheckboxConstraints = "h 30px!, gap 0 20px 0 0";
    			decksFrame.add(cbSingletons, strCheckboxConstraints);
    			decksFrame.add(cbArtifacts, strCheckboxConstraints);
    		}
        }
        else if (GameType.Archenemy == forGameType || GameType.ArchenemyRumble == forGameType) {
            if (isPlayerArchenemy(playerWithFocus)) {
                decksFrame.add(schemeDeckPanels.get(playerWithFocus), "grow, push");
            } else {
            	populateDeckPanel(GameType.Constructed);
            }
        }
    	else if (GameType.Commander == forGameType) {
            decksFrame.add(commanderDeckPanels.get(playerWithFocus), "grow, push");
        }
    	else if (GameType.Planechase == forGameType) {
            decksFrame.add(planarDeckPanels.get(playerWithFocus), "grow, push");
        }
        else if (GameType.Vanguard == forGameType) {
    		updateVanguardList(playerWithFocus);
    		decksFrame.add(vgdPanels.get(playerWithFocus), "grow, push");
    	}
		refreshPanels(false, true);
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getGroupEnum()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }

    public final FDeckChooser getDeckChooser(int playernum) {
    	return deckChoosers.get(playernum);
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Constructed";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_CONSTRUCTED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));
        container.add(lblTitle, "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");

        for (final FDeckChooser fdc : deckChoosers) {
        	fdc.populate();
        	fdc.getDecksComboBox().addListener(new IDecksComboBoxListener() {
				@Override
				public void deckTypeSelected(DecksComboBoxEvent ev) {
				    playerPanelWithFocus.focusOnAvatar();
				}
			});
        }
    	populateDeckPanel(GameType.Constructed);
    	populateVanguardLists();

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(constructedFrame, "gap 20px 20px 20px 0px, push, grow");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "gap 0 0 3.5%! 3.5%!, ax center");

        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** Gets the random deck checkbox for Singletons. */
    public FCheckBox getCbSingletons() { return cbSingletons; }

    /** Gets the random deck checkbox for Artifacts. */
    public FCheckBox getCbArtifacts() { return cbArtifacts; }

    public boolean isPlayerAI(int playernum) {
        return playerPanels.get(playernum).isAi();
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

    /** Revalidates the player and deck sections. Necessary after adding or hiding any panels. */
    private void refreshPanels(boolean refreshPlayerFrame, boolean refreshDeckFrame) {
    	if (refreshPlayerFrame) {
    	    playersScroll.validate();
    	    playersScroll.repaint();
    	}
    	if (refreshDeckFrame) {
            decksFrame.validate();
    	    decksFrame.repaint();
    	}
    }

    @SuppressWarnings("serial")
    private class PlayerPanel extends FPanel {
        private final int index;

        private final FLabel nameRandomiser;
        private final FLabel avatarLabel = new FLabel.Builder().opaque(true).hoverable(true).iconScaleFactor(0.99f).iconInBackground(true).build();
        private int avatarIndex;

        private final FTextField txtPlayerName = new FTextField.Builder().text("Player name").build();
        private FRadioButton radioHuman;
        private FRadioButton radioAi;

        private FComboBoxWrapper<Object> teamComboBox = new FComboBoxWrapper<Object>();
        private FComboBoxWrapper<Object> aeTeamComboBox = new FComboBoxWrapper<Object>();

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
            this.index = index;
            playerIsArchenemy = index == 0;

            setLayout(new MigLayout("insets 10px, gap 5px"));

            // Add a button to players 3+ to remove them from the setup
            if (index >= 2) {
                FLabel closeBtn = createCloseButton();
                this.add(closeBtn, "w 20, h 20, pos (container.w-20) 0");
            }

            createAvatar();
            this.add(avatarLabel, "spany 2, width 80px, height 80px");

            createNameEditor();
            this.add(newLabel("Name:"), "w 40px, h 30px, gaptop 5px");
            this.add(txtPlayerName, "h 30px, pushx, growx");

            nameRandomiser = createNameRandomizer();
            this.add(nameRandomiser, "h 30px, w 30px, gaptop 5px");

            createPlayerTypeOptions();
            this.add(radioHuman, "gapright 5px");
            this.add(radioAi, "wrap");

            this.add(newLabel("Team:"), "w 40px, h 30px");
        	populateTeamsComboBoxes();
            teamComboBox.addActionListener(teamListener);
            aeTeamComboBox.addActionListener(teamListener);
            teamComboBox.addTo(this, variantBtnConstraints + ", pushx, growx, gaptop 5px");
            aeTeamComboBox.addTo(this, variantBtnConstraints + ", pushx, growx, gaptop 5px");

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
                @Override
                public void onLeftMouseDown(MouseEvent e) {
                    avatarLabel.requestFocusInWindow();
                }
            });
        }

        private final FMouseAdapter radioMouseAdapter = new FMouseAdapter() {
            @Override
            public void onLeftClick(MouseEvent e) {
                avatarLabel.requestFocusInWindow();
                updateVanguardList(index);
            }
        };

        /** Listens to name text fields and gives the appropriate player focus.
         *  Also saves the name preference when leaving player one's text field. */
        private FocusAdapter nameFocusListener = new FocusAdapter() {
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

        /** Listens to avatar buttons and gives the appropriate player focus. */
        private FocusAdapter avatarFocusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                changePlayerFocus(index);
            }
        };

        private FMouseAdapter avatarMouseListener = new FMouseAdapter() {
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
            boolean isCommanderApplied = false;
            boolean isPlanechaseApplied = false;
            boolean isVanguardApplied = false;
            boolean isArchenemyApplied = false;
            boolean archenemyVisiblity = false;
            boolean isDeckBuildingAllowed = true;
            
            for (GameType variant : appliedVariants) {
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
            if (playerPanelWithFocus != this) {
                FSkin.setGraphicsColor(g, unfocusedPlayerOverlay);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
        }

        public Boolean isAi() {
            return radioAi.isSelected();
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

        /**
         * @param index
         */
        private void addHandlersToVariantsControls() {
        	// Archenemy buttons
            scmDeckSelectorBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                	currentGameMode = vntArchenemy.isSelected() ? GameType.Archenemy : GameType.ArchenemyRumble;
                    scmDeckSelectorBtn.requestFocusInWindow();
                    changePlayerFocus(index, currentGameMode);
                }
            });

            scmDeckEditor.setCommand(new UiCommand() {
                @Override
                public void run() {
                    currentGameMode = vntArchenemy.isSelected() ? GameType.Archenemy : GameType.ArchenemyRumble;
                    Predicate<PaperCard> predSchemes = new Predicate<PaperCard>() {
                        @Override
                        public boolean apply(PaperCard arg0) {
                            return arg0.getRules().getType().isScheme();
                        }
                    };

                    Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_ARCHENEMY);
                    CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                    		new CEditorVariant(FModel.getDecks().getScheme(), predSchemes, DeckSection.Schemes, FScreen.DECK_EDITOR_PLANECHASE));
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
                            new CEditorVariant(FModel.getDecks().getPlane(), predPlanes, DeckSection.Planes, FScreen.DECK_EDITOR_PLANECHASE));
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
         */
        private void createPlayerTypeOptions() {
            radioHuman = new FRadioButton("Human", index == 0);
            radioAi = new FRadioButton("AI", index != 0);

            radioHuman.addMouseListener(radioMouseAdapter);
            radioAi.addMouseListener(radioMouseAdapter);

            ButtonGroup tempBtnGroup = new ButtonGroup();
            tempBtnGroup.add(radioHuman);
            tempBtnGroup.add(radioAi);
        }

        /**
         * @param index
         */
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
                name = NameGenerator.getRandomName("Any", "Any", getPlayerNames());
            }

            txtPlayerName.setText(name);
            txtPlayerName.setFocusable(true);
            txtPlayerName.setFont(FSkin.getFont(14));
            txtPlayerName.addActionListener(nameListener);
            txtPlayerName.addFocusListener(nameFocusListener);
        }

        private FLabel createCloseButton() {
            final FLabel closeBtn = new FLabel.Builder().tooltip("Close").iconInBackground(false)
                    .icon(FSkin.getIcon(FSkinProp.ICO_CLOSE)).hoverable(true).build();
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

            List<Integer> usedAvatars = getUsedAvatars();
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

    private void changePlayerFocus(int newFocusOwner) {
    	changePlayerFocus(newFocusOwner, appliedVariants.contains(currentGameMode) ? currentGameMode : GameType.Constructed);
    }

    private void changePlayerFocus(int newFocusOwner, GameType gType) {
        playerPanelWithFocus.setFocused(false);
    	playerWithFocus = newFocusOwner;
    	playerPanelWithFocus = playerPanels.get(playerWithFocus);
        playerPanelWithFocus.setFocused(true);

    	playersScroll.getViewport().scrollRectToVisible(playerPanelWithFocus.getBounds());
    	populateDeckPanel(gType);

    	refreshPanels(true, true);
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
    	return new FLabel.Builder().text(title).fontSize(14).fontStyle(Font.ITALIC).build();
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
    	final SkinImage icon = FOptionPane.QUESTION_ICON;
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
    		} else if (lastTeam != teamList.get(i)) {
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
            else if (cb == vntMomirBasic) {
                variantType = GameType.MomirBasic;
            }
            else if (cb == vntCommander) {
                variantType = GameType.Commander;
            }
            else if (cb == vntPlanechase) {
                variantType = GameType.Planechase;
            }
            else if (cb == vntArchenemy) {
                variantType = GameType.Archenemy;
            }
            else if (cb == vntArchenemyRumble) {
                variantType = GameType.ArchenemyRumble;
            }

            if (variantType != null) {
                if (arg0.getStateChange() == ItemEvent.SELECTED) {
                    appliedVariants.add(variantType);
                    currentGameMode = variantType;

                    //ensure other necessary variants are unchecked
                    switch (variantType) {
                    case Archenemy:
                        vntArchenemyRumble.setSelected(false);
                        break;
                    case ArchenemyRumble:
                        vntArchenemy.setSelected(false);
                        break;
                    case Commander:
                        vntMomirBasic.setSelected(false);
                        break;
                    case Vanguard:
                        vntMomirBasic.setSelected(false);
                        break;
                    case MomirBasic:
                        vntCommander.setSelected(false);
                        vntVanguard.setSelected(false);
                        break;
                    default:
                        break;
                    }
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
            changePlayerFocus(playerWithFocus, currentGameMode);
        }
    };

    private ActionListener nameListener = new ActionListener() {
    	@Override
		public void actionPerformed(ActionEvent e) {
			FTextField nField = (FTextField)e.getSource();
			nField.transferFocus();
		}
    };

    /** This listener will look for a vanguard avatar being selected in the lists
    / and update the corresponding detail panel. */
    private ListSelectionListener vgdLSListener = new ListSelectionListener() {

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
    };

    /////////////////////////////////////
    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONSTRUCTED;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CSubmenuConstructed getLayoutControl() {
        return CSubmenuConstructed.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    /////////////////////////////////////
    //========== METHODS FOR VARIANTS

    public Set<GameType> getAppliedVariants() {
    	return appliedVariants;
    }

    public int getTeam(final int playerIndex) {
    	return appliedVariants.contains(GameType.Archenemy) ? archenemyTeams.get(playerIndex) : teams.get(playerIndex);
    }

    /** Gets the list of planar deck lists. */
    public List<FList<Object>> getPlanarDeckLists() {
    	return planarDeckLists;
    }

    /** Gets the list of commander deck lists. */
    public List<FList<Object>> getCommanderDeckLists() {
    	return commanderDeckLists;
    }

    /** Gets the list of scheme deck lists. */
    public List<FList<Object>> getSchemeDeckLists() {
    	return schemeDeckLists;
    }

    public boolean isPlayerArchenemy(final int playernum) {
        return playerPanels.get(playernum).playerIsArchenemy;
    }

    /** Gets the list of Vanguard avatar lists. */
    public List<FList<Object>> getVanguardLists() {
    	return vgdAvatarLists;
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
    	FList<Object> vgdList = getVanguardLists().get(playerIndex);
		Object lastSelection = vgdList.getSelectedValue();
		vgdList.setListData(isPlayerAI(playerIndex) ? aiListData : humanListData);
		if (null != lastSelection) {
			vgdList.setSelectedValue(lastSelection, true);
		}

		if (-1 == vgdList.getSelectedIndex()) {
			vgdList.setSelectedIndex(0);
		}
    }
}
