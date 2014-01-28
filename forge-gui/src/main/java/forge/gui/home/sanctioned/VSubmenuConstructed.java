package forge.gui.home.sanctioned;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;

import forge.Command;
import forge.Singletons;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.player.LobbyPlayer.PlayerType;
import forge.gui.CardDetailPanel;
import forge.gui.deckchooser.DecksComboBox.DeckType;
import forge.gui.deckchooser.DecksComboBoxEvent;
import forge.gui.deckchooser.FDeckChooser;
import forge.gui.deckeditor.DeckProxy;
import forge.gui.deckchooser.IDecksComboBoxListener;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorVariant;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FComboBox;
import forge.gui.toolbox.FComboBoxWrapper;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FScrollPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinColor;
import forge.gui.toolbox.FSkin.SkinImage;
import forge.gui.toolbox.FTextField;

import forge.item.PaperCard;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.NameGenerator;

/**
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuConstructed implements IVSubmenu<CSubmenuConstructed> {
    SINGLETON_INSTANCE;
    private static final ForgePreferences prefs = Singletons.getModel().getPreferences();
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

    private final StartButton btnStart  = new StartButton();
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    private final JPanel constructedFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap 2")); // Main content frame

    // Variants frame and variables
    private final Set<GameType> appliedVariants = new TreeSet<GameType>();
	private final FPanel variantsPanel = new FPanel(new MigLayout("insets 10, gapx 10"));
    private final FCheckBox vntVanguard = new FCheckBox("Vanguard");
    private final FCheckBox vntCommander = new FCheckBox("Commander");
    private final FCheckBox vntPlanechase = new FCheckBox("Planechase");
    private final FCheckBox vntArchenemy = new FCheckBox("Archenemy");
    private String archenemyType = "Classic";
    private final FComboBoxWrapper<String> comboArchenemy = new FComboBoxWrapper<String>(new String[]{
    		"Classic Archenemy (player 1 is Archenemy)", "Archenemy Rumble (All players are Archenemies)"});

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
                "w 100%, h 45px!, gapbottom 10px, spanx 2, wrap");

        ////////////////////////////////////////////////////////
        ///////////////////// Player Panel /////////////////////

        // Construct individual player panels
        String constraints = "pushx, growx, wrap, hidemode 3";
        for (int i = 0; i < MAX_PLAYERS; i++) {
        	teams.add(i+1);
        	
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
        if (activePlayersNum >= MAX_PLAYERS)
            return;

        int freeIndex = -1;
        for(int i = 0; i < MAX_PLAYERS; i++)
            if ( !playerPanels.get(i).isVisible() ) {
                freeIndex = i;
                break;
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
    	String sectionConstraints = "insets 8";
    	String componentConstraints = "gap 0px 0px 10px 10px, wrap";

        // Main deck
        final FDeckChooser mainChooser = new FDeckChooser(isPlayerAI(playerIndex));
        mainChooser.initialize();
        mainChooser.getLstDecks().setSelectCommand(new Command() {
            @Override
            public void run() {
                VSubmenuConstructed.this.onDeckClicked(playerIndex, mainChooser.getSelectedDeckType(), mainChooser.getLstDecks().getSelectedItems());
            }
        });
        deckChoosers.add(mainChooser);

        // Planar deck list
        FPanel planarDeckPanel = new FPanel();
        planarDeckPanel.setLayout(new MigLayout(sectionConstraints));
        planarDeckPanel.add(new FLabel.Builder().text("Select Planar deck:").build(), componentConstraints);
        FList<Object> planarDeckList = new FList<Object>();
        planarDeckList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        FScrollPane scrPlanes = new FScrollPane(planarDeckList, false,
        		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        planarDeckPanel.add(scrPlanes, componentConstraints + ", h 95%, grow, push");
        planarDeckLists.add(planarDeckList);
        planarDeckPanels.add(planarDeckPanel);

        // Vanguard avatar list
        FPanel vgdDeckPanel = new FPanel();

        FList<Object> vgdAvatarList = new FList<Object>();
        vgdAvatarList.setListData(isPlayerAI(playerIndex) ? aiListData : humanListData);
        vgdAvatarList.setSelectedIndex(0);
        vgdAvatarList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        vgdAvatarList.addListSelectionListener(vgdLSListener);
        FScrollPane scrAvatars = new FScrollPane(vgdAvatarList, false,
        		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        CardDetailPanel vgdDetail = new CardDetailPanel(null);
        vgdAvatarDetails.add(vgdDetail);

        vgdDeckPanel.setLayout(new MigLayout(sectionConstraints));
        vgdDeckPanel.add(new FLabel.Builder().text("Select a Vanguard avatar:").build(), componentConstraints);
        vgdDeckPanel.add(scrAvatars, componentConstraints + ", grow, push");
        vgdDeckPanel.add(vgdDetail, componentConstraints + ", growx, pushx, hidemode 3");
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
    	} else if (GameType.Planechase == forGameType) {
    		decksFrame.add(planarDeckPanels.get(playerWithFocus), "grow, push");
    	} else if (GameType.Vanguard == forGameType) {
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
				    playerPanels.get(playerWithFocus).focusOnAvatar();
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
        return playerPanels.get(playernum).getPlayerType() == PlayerType.COMPUTER;
    }

    public int getNumPlayers() {
        return activePlayersNum;
    }

    public final List<Integer> getParticipants() {
    	final List<Integer> participants = new ArrayList<Integer>(activePlayersNum);
    	for (final PlayerPanel panel : playerPanels) {
    	    if(panel.isVisible())
	            participants.add(playerPanels.indexOf(panel));
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
        
        
        private final FLabel deckBtn = new FLabel.ButtonBuilder().text("Select a deck").build();
        
        private final String variantBtnConstraints = "height 30px, hidemode 3";
        
        private final FLabel pchDeckSelectorBtn = new FLabel.ButtonBuilder().text("Select a planar deck").build();
        private final FLabel pchDeckEditor = new FLabel.ButtonBuilder().text("Planar Deck Editor").build();
        private final FLabel pchLabel = newLabel("Planar deck:");
        
        private final FLabel vgdSelectorBtn = new FLabel.ButtonBuilder().text("Select a Vanguard avatar").build();
        private final FLabel vgdLabel = newLabel("Vanguard:");
        
        

        public PlayerPanel(final int index) {
            super();
            this.index = index;
            
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

            this.add(newLabel("Deck:"), "w 40px, h 30px");
            this.add(deckBtn, "pushx, growx, wmax 100%-153px, h 30px, spanx 4, wrap");

            addHandlersDeckSelector();
            
            this.add(pchLabel, variantBtnConstraints + ", cell 0 2, sx 2, ax right");
            this.add(pchDeckSelectorBtn, variantBtnConstraints + ", cell 2 2, growx, pushx");
            this.add(pchDeckEditor, variantBtnConstraints + ", cell 3 2, sx 3, growx, wrap");

            this.add(vgdSelectorBtn, variantBtnConstraints + ", cell 2 3, sx 4, growx, wrap");
            this.add(vgdLabel, variantBtnConstraints + ", cell 0 3, sx 2, ax right");
            
            addHandlersToVariantsControls();
            updateVariantControlsVisibility();
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
                    lbl.setCommand(new Command() {
                        @Override
                        public void run() {
                            setAvatar(Integer.valueOf(lbl.getName().substring(11)));
                            aSel.setVisible(false);
                        }
                    });
                }

                aSel.setVisible(true);
                aSel.dispose();

                if (index < 2)
                    updateAvatarPrefs();
            }
            @Override
            public void onRightClick(MouseEvent e) {
                changePlayerFocus(index);
                avatarLabel.requestFocusInWindow();

                setRandomAvatar();

                if (index < 2) 
                    updateAvatarPrefs();
            }
        };
        
        
        public void updateVariantControlsVisibility() {
            pchDeckSelectorBtn.setVisible(appliedVariants.contains(GameType.Planechase));
            pchDeckEditor.setVisible(appliedVariants.contains(GameType.Planechase));
            pchLabel.setVisible(appliedVariants.contains(GameType.Planechase));
            
            vgdSelectorBtn.setVisible(appliedVariants.contains(GameType.Vanguard));
            vgdLabel.setVisible(appliedVariants.contains(GameType.Vanguard));
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (playerPanelWithFocus != this) {
                FSkin.setGraphicsColor(g, unfocusedPlayerOverlay);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
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


        public void focusOnAvatar() {
            avatarLabel.requestFocusInWindow();
            
        }

        /**
         * @param index
         */
        private void addHandlersToVariantsControls() {
            // Planechase buttons
            pchDeckSelectorBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                    currentGameMode = GameType.Planechase;
                    pchDeckSelectorBtn.requestFocusInWindow();
                    changePlayerFocus(index, GameType.Planechase);
                }
            });
            
            pchDeckEditor.setCommand(new Command() {
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
                    .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT)).hoverable(true).opaque(false)
                    .unhoveredAlpha(0.9f).build();
            newNameBtn.setCommand(new Command() {
                @Override
                public void run() {
                    String newName = getNewName();
                    if ( null == newName )
                        return;

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
                name = Singletons.getModel().getPreferences().getPref(FPref.PLAYER_NAME);
                if (name.isEmpty()) {
                    name = "Human";
                }
            } else {
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
            String[] currentPrefs = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
            if (index < currentPrefs.length) {
                avatarIndex = Integer.parseInt(currentPrefs[index]);
                avatarLabel.setIcon(FSkin.getAvatars().get(avatarIndex));
            } else {
                setRandomAvatar();
            }
            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftClick(MouseEvent e) {
                    avatarLabel.requestFocusInWindow();
                }
            });
            
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
    	playerWithFocus = newFocusOwner;
    	playerPanelWithFocus = playerPanels.get(playerWithFocus);

    	changeAvatarFocus();
    	playersScroll.getViewport().scrollRectToVisible(playerPanelWithFocus.getBounds());
    	populateDeckPanel(gType);

    	refreshPanels(true, true);
    }

    /** Changes avatar appearance dependant on focus player. */
    private void changeAvatarFocus() {
    	for (int i = 0; i < playerPanels.size(); i++) {
    	    PlayerPanel pp = playerPanels.get(i);
    	    pp.setFocused(i == playerWithFocus);
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
        ForgePreferences prefs = Singletons.getModel().getPreferences();
        
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
    	if ( genderIndex < 0 )
    	    return null;
    	final int typeIndex = FOptionPane.showOptionDialog(message, title, icon, typeOptions, 2);
        if ( typeIndex < 0 )
            return null;
    	final String gender = genderOptions[genderIndex];
    	final String type = typeOptions[typeIndex];

    	String confirmMsg;
    	String newName;

    	List<String> usedNames = getPlayerNames();
    	do {
    		newName = NameGenerator.getRandomName(gender, type, usedNames);
    		confirmMsg = "Would you like to use the name \"" + newName + "\", or try again?";
    	} while (!FOptionPane.showConfirmDialog(confirmMsg, title, "Use this name", "Try again", true));

        return newName;
    }

    /////////////////////////////////////////////
    //========== Various listeners in build order

    private List<String> getPlayerNames() {
        List<String> names = new ArrayList<String>();
        for(PlayerPanel pp : playerPanels) {
            names.add(pp.getPlayerName());
        }
        return names;
    }

    /** This listener unlocks the relevant buttons for players
     * and enables/disables archenemy combobox as appropriate. */
    private ItemListener iListenerVariants = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent arg0) {
            FCheckBox cb = (FCheckBox) arg0.getSource();
            GameType variantType = null;

            if (cb == vntVanguard)
                variantType = GameType.Vanguard;
            else if (cb == vntCommander)
                variantType = GameType.Commander;
            else if (cb == vntPlanechase)
                variantType = GameType.Planechase;
            else if (cb == vntArchenemy) {
                variantType = archenemyType.contains("Classic") ? GameType.Archenemy : GameType.ArchenemyRumble;
                comboArchenemy.setEnabled(vntArchenemy.isSelected());
                if (arg0.getStateChange() != ItemEvent.SELECTED) {
                    appliedVariants.remove(GameType.Archenemy);
                    appliedVariants.remove(GameType.ArchenemyRumble);
                }
            }
            
            if ( null != variantType ) {
                if (arg0.getStateChange() == ItemEvent.SELECTED)
                    appliedVariants.add(variantType);
                else
                    appliedVariants.remove(variantType);
            }

            for (PlayerPanel pp : playerPanels) {
                pp.updateVariantControlsVisibility();
            }
            changePlayerFocus(playerWithFocus, variantType);
        }
    };

    // Listens to the archenemy combo box
    private ActionListener aeComboListener = new ActionListener() {
    	@SuppressWarnings("unchecked")
		@Override
		public void actionPerformed(ActionEvent e) {
			FComboBox<String> cb = (FComboBox<String>)e.getSource();
			archenemyType = (String)cb.getSelectedItem();
			appliedVariants.remove(GameType.Archenemy);
    		appliedVariants.remove(GameType.ArchenemyRumble);
    		appliedVariants.add(archenemyType.contains("Classic")
    				? GameType.Archenemy : GameType.ArchenemyRumble);
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
            } else {
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
    	return teams.get(playerIndex);
    }

    /** Gets the list of planar deck lists. */
    public List<FList<Object>> getPlanarDeckLists() {
    	return planarDeckLists;
    }

    /** Gets the list of Vanguard avatar lists. */
    public List<FList<Object>> getVanguardLists() {
    	return vgdAvatarLists;
    }

    /** Return all the Vanguard avatars. */
    public Iterable<PaperCard> getAllAvatars() {
        if (vgdAllAvatars.isEmpty()) {
            for (PaperCard c : Singletons.getMagicDb().getVariantCards().getAllCards()) {
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

    public String getPlayerName(int i) {
        return playerPanels.get(i).getPlayerName();
    }

    public int getPlayerAvatar(int i) {
        return playerPanels.get(i).getAvatarIndex();
    }
}
