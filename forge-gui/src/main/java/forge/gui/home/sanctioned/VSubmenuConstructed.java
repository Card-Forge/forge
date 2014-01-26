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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
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
import forge.item.IPaperCard;
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

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Constructed Mode");

    // General variables
    private final LblHeader lblTitle = new LblHeader("Sanctioned Format: Constructed");
    private int activePlayersNum = 2;
    private int playerWithFocus = 0; // index of the player that currently has focus
    private PlayerPanel playerPanelWithFocus;
    private GameType currentGameMode = GameType.Constructed;
    private List<Integer> teams = new ArrayList<Integer>(8);

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
    private final List<PlayerPanel> playerPanelList = new ArrayList<PlayerPanel>(8);
    private final List<FPanel> activePlayerPanelList = new ArrayList<FPanel>(8);
    private final List<FPanel> inactivePlayerPanelList = new ArrayList<FPanel>(6);
    private final List<FTextField> playerNameBtnList = new ArrayList<FTextField>(8);
    private final List<String> playerNames = new ArrayList<String>(8);
    private final List<FLabel> nameRandomisers = new ArrayList<FLabel>(8);
    private final List<FRadioButton> playerTypeRadios = new ArrayList<FRadioButton>(8);
    private final List<FLabel> avatarList = new ArrayList<FLabel>(8);
    private final TreeMap<Integer, Integer> usedAvatars = new TreeMap<Integer, Integer>();

    private final List<FLabel> closePlayerBtnList = new ArrayList<FLabel>(6);
    private final FLabel addPlayerBtn = new FLabel.ButtonBuilder().fontSize(14).text("Add a Player").build();

    // Deck frame elements
    private final JPanel decksFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
    private final List<FDeckChooser> deckChoosers = new ArrayList<FDeckChooser>(8);
    private final List<FLabel> deckSelectorBtns = new ArrayList<FLabel>(8);
    private final FCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final FCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");

    // Variants
    private final List<FList<Object>> planarDeckLists = new ArrayList<FList<Object>>();
    private final List<FPanel> planarDeckPanels = new ArrayList<FPanel>(8);
    private final List<FLabel> plnDeckSelectorBtns = new ArrayList<FLabel>(8);
    private final List<FLabel> plnEditors = new ArrayList<FLabel>(8);

    private final List<FList<Object>> vgdAvatarLists = new ArrayList<FList<Object>>();
    private final List<FPanel> vgdPanels = new ArrayList<FPanel>(8);
    private final List<FLabel> vgdSelectorBtns = new ArrayList<FLabel>(8);
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
        for (int i = 0; i < 8; i++) {
        	teams.add(i+1);
        	buildPlayerPanel(i);
        	FPanel player = playerPanelList.get(i);

        	// Populate players panel
        	if (i < activePlayersNum) {
        	    playersScroll.add(player, constraints);
        		activePlayerPanelList.add(player);
        	}
        	else {
        		player.setVisible(false);
        		playersScroll.add(player, constraints);
        		inactivePlayerPanelList.add(player);
        	}
        	if (i == 0) {
        	    constraints += ", gaptop 5px";
        	}
        }

        playerPanelWithFocus = playerPanelList.get(0);

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
        
        for (int i = 0; i < 8; i++) {
        	buildDeckPanel(i);
        }
        constructedFrame.add(decksFrame, "w 50%-5px, growy, pushy");
        constructedFrame.setOpaque(false);
        decksFrame.setOpaque(false);

        // Start Button
        pnlStart.setOpaque(false);
        pnlStart.add(btnStart, "align center");
    }

    @SuppressWarnings("serial")
	private FPanel buildPlayerPanel(final int playerIndex) {
        PlayerPanel playerPanel = new PlayerPanel();
        playerPanel.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftClick(MouseEvent e) {
                avatarList.get(playerPanelList.indexOf((FPanel)e.getSource())).requestFocusInWindow();
            }
        });
        playerPanel.setLayout(new MigLayout("insets 10px, gap 5px"));

        // Add a button to players 3+ to remove them from the setup
        if (playerIndex >= 2) {
            final FLabel closeBtn = new FLabel.Builder().tooltip("Close").iconInBackground(false)
        			.icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_CLOSE)).hoverable(true).build();
            closeBtn.setCommand(new Runnable() {
                @Override
                public void run() {
                    removePlayer(closePlayerBtnList.indexOf(closeBtn) + 2);
                }
            });
        	playerPanel.add(closeBtn, "w 20, h 20, pos (container.w-20) 0");
        	closePlayerBtnList.add(closeBtn);
        }

        // Avatar
        final FLabel avatar = new FLabel.Builder().opaque(true).hoverable(true)
        		.iconScaleFactor(0.99f).iconInBackground(true).build();
        String[] currentPrefs = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
        if (playerIndex < currentPrefs.length) {
            int avatarIndex = Integer.parseInt(currentPrefs[playerIndex]);
        	avatar.setIcon(FSkin.getAvatars().get(avatarIndex));
        	usedAvatars.put(playerIndex, avatarIndex);
        } else {
        	setRandomAvatar(avatar, playerIndex);
        }
        changeAvatarFocus();
        avatar.setToolTipText("L-click: Select avatar. R-click: Randomize avatar.");
        avatar.addFocusListener(avatarFocusListener);
        avatar.addMouseListener(avatarMouseListener);
        avatarList.add(avatar);

        playerPanel.add(avatar, "spany 2, width 80px, height 80px");

        // Name
        String name;
        if (playerIndex == 0) {
        	name = Singletons.getModel().getPreferences().getPref(FPref.PLAYER_NAME);
        	if (name.isEmpty()) {
        		name = "Human";
        	}
        } else {
        	name = NameGenerator.getRandomName("Any", "Any", playerNames);
        }
    	playerNames.add(name);
        final FTextField playerNameField = new FTextField.Builder().text(name).build();
        playerNameField.setFocusable(true);
        playerNameField.setFont(FSkin.getFont(14));
        playerNameField.addActionListener(nameListener);
        playerNameField.addFocusListener(nameFocusListener);
        playerPanel.add(newLabel("Name:"), "w 40px, h 30px, gaptop 5px");
        playerPanel.add(playerNameField, "h 30px, pushx, growx");
        playerNameBtnList.add(playerNameField);

        // Name randomiser
        final FLabel newNameBtn = new FLabel.Builder().tooltip("Get a new random name").iconInBackground(false)
        		.icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT)).hoverable(true).opaque(false)
        		.unhoveredAlpha(0.9f).build();
        newNameBtn.setCommand(new Command() {
			@Override
			public void run() {
    			String newName = getNewName();
    			if ( null == newName )
    			    return;
                int index = nameRandomisers.indexOf(newNameBtn);
    			FTextField nField = playerNameBtnList.get(index);

    			nField.setText(newName);

				if (index == 0) {
					prefs.setPref(FPref.PLAYER_NAME, newName);
					prefs.save();
				}
				playerNameBtnList.get(index).requestFocus();
				changePlayerFocus(index);
			}
        });
        newNameBtn.addFocusListener(nameFocusListener);
        playerPanel.add(newNameBtn, "h 30px, w 30px, gaptop 5px");
        nameRandomisers.add(newNameBtn);

        // PlayerType
        ButtonGroup tempBtnGroup = new ButtonGroup();
        FRadioButton tmpHuman = new FRadioButton();
        tmpHuman.setText("Human");
        tmpHuman.setSelected(playerIndex == 0);
        tmpHuman.addMouseListener(radioMouseAdapter);
        FRadioButton tmpAI = new FRadioButton();
        tmpAI.setText("AI");
        tmpAI.setSelected(playerIndex != 0);
        tmpAI.addMouseListener(radioMouseAdapter);
        tempBtnGroup.add(tmpHuman);
        tempBtnGroup.add(tmpAI);
        playerTypeRadios.add(tmpHuman);
        playerTypeRadios.add(tmpAI);

        playerPanel.add(tmpHuman, "gapright 5px");
        playerPanel.add(tmpAI, "wrap");

        // Deck selector button
        final FLabel deckBtn = new FLabel.ButtonBuilder().text("Select a deck").build();
        deckBtn.setCommand(new Runnable() {
            @Override
            public void run() {
            	currentGameMode = GameType.Constructed;
                deckBtn.requestFocusInWindow();
            	changePlayerFocus(deckSelectorBtns.indexOf(deckBtn), GameType.Constructed);
            }
        });
        playerPanel.add(newLabel("Deck:"), "w 40px, h 30px");
        playerPanel.add(deckBtn, "pushx, growx, wmax 100%-157px, h 30px, spanx 4, wrap");
        deckSelectorBtns.add(deckBtn);

        // Variants
        String variantBtnConstraints = "height 30px, hidemode 3";
        
        // Planechase buttons
        final FLabel plnDeckSelectorBtn = new FLabel.ButtonBuilder().text("Select a planar deck").build();
        plnDeckSelectorBtn.setVisible(appliedVariants.contains(GameType.Planechase));
        plnDeckSelectorBtn.setCommand(new Runnable() {
            @Override
            public void run() {
            	currentGameMode = GameType.Planechase;
            	plnDeckSelectorBtn.requestFocusInWindow();
            	changePlayerFocus(plnDeckSelectorBtns.indexOf(plnDeckSelectorBtn), GameType.Planechase);
            }
        });
        final FLabel plnDeckEditor = new FLabel.ButtonBuilder().text("Planar Deck Editor").build();
        plnDeckEditor.setVisible(appliedVariants.contains(GameType.Planechase));
        plnDeckEditor.setCommand(new Command() {
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
        playerPanel.add(plnDeckSelectorBtn, variantBtnConstraints + ", spanx, split 2, growx, pushx, gapright rel");
        playerPanel.add(plnDeckEditor, variantBtnConstraints + ", width 150px, wrap");
        plnDeckSelectorBtns.add(plnDeckSelectorBtn);
        plnEditors.add(plnDeckEditor);
        
        // Vanguard buttons
        final FLabel vgdSelectorBtn = new FLabel.ButtonBuilder().text("Select a Vanguard avatar").build();
        vgdSelectorBtn.setVisible(appliedVariants.contains(GameType.Vanguard));
        vgdSelectorBtn.setCommand(new Runnable() {
            @Override
            public void run() {
            	currentGameMode = GameType.Vanguard;
            	vgdSelectorBtn.requestFocusInWindow();
            	changePlayerFocus(vgdSelectorBtns.indexOf(vgdSelectorBtn), GameType.Vanguard);
            }
        });
        playerPanel.add(vgdSelectorBtn, variantBtnConstraints + ", spanx, growx, pushx, wrap");
        vgdSelectorBtns.add(vgdSelectorBtn);

        playerPanelList.add(playerPanel);

        return playerPanel;
    }

    private void addPlayer() {
    	if (activePlayersNum < 8) {
    		FPanel player = inactivePlayerPanelList.get(0);
    		player.setVisible(true);
    		inactivePlayerPanelList.remove(player);
    		activePlayerPanelList.add(player);
    		addPlayerBtn.setEnabled(activePlayersNum < 7);
        	activePlayersNum++;

        	avatarList.get(playerPanelList.indexOf(player)).requestFocusInWindow();
    	}
    }

    private void removePlayer(int playerIndex) {
    	activePlayersNum--;
		FPanel player = playerPanelList.get(playerIndex);
    	player.setVisible(false);
		inactivePlayerPanelList.add(player);
		activePlayerPanelList.remove(player);
		addPlayerBtn.setEnabled(true);

		//find closest player still in game and give focus
		int min = 8;
	    int closest = 2;

	    for (int participantIndex : getParticipants()) {
	        final int diff = Math.abs(playerIndex - participantIndex);

	        if (diff < min) {
	            min = diff;
	            closest = participantIndex;
	        }
	    }

	    changePlayerFocus(closest);
	    avatarList.get(closest).requestFocusInWindow();
    }

    /** Applies a random avatar, avoiding avatars already used.
     * @param playerIndex */
    private void setRandomAvatar(FLabel avatar, int playerIndex) {
        int random = 0;
        do {
            random = MyRandom.getRandom().nextInt(FSkin.getAvatars().size());
        } while (usedAvatars.values().contains(random));

        setAvatar(avatar, playerIndex, random);
    }

    private void setAvatar(FLabel avatar, int playerIndex, int newAvatarIndex) {
    	avatar.setIcon(FSkin.getAvatars().get(newAvatarIndex));
        avatar.repaintSelf();
    	usedAvatars.put(playerIndex, newAvatarIndex);
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
        vgdDeckPanel.add(vgdDetail, componentConstraints + ", growx, pushx");
        vgdAvatarLists.add(vgdAvatarList);
        vgdPanels.add(vgdDeckPanel);
    }

    protected void onDeckClicked(int iPlayer, DeckType type, Collection<DeckProxy> selectedDecks) {
        String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
        deckSelectorBtns.get(iPlayer).setText(text);
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
					avatarList.get(playerWithFocus).requestFocusInWindow();
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
    	// playerTypeRadios list contains human radio, AI radio, human, AI, etc
    	// so playernum * 2 + 1 points to the appropriate AI radio.
    	return playerTypeRadios.get(playernum * 2 + 1).isSelected();
    }

    public int getNumPlayers() {
        return activePlayersNum;
    }

    public final List<Integer> getParticipants() {
    	final List<Integer> participants = new ArrayList<Integer>(activePlayersNum);
    	for (final FPanel panel : activePlayerPanelList) {
    		participants.add(playerPanelList.indexOf(panel));
    	}
        return participants;
    }

    public final String getPlayerName(int playerIndex) {
    	return playerNameBtnList.get(playerIndex).getText();
    }

    /** Gets the index of the appropriate player's avatar. */
    public final int getPlayerAvatar(int playerIndex) {
    	return usedAvatars.get(playerIndex);
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
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (playerPanelWithFocus != this) {
                FSkin.setGraphicsColor(g, unfocusedPlayerOverlay);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
        }
    }

    private void changePlayerFocus(int newFocusOwner) {
    	changePlayerFocus(newFocusOwner, appliedVariants.contains(currentGameMode) ? currentGameMode : GameType.Constructed);
    }

    private void changePlayerFocus(int newFocusOwner, GameType gType) {
    	playerWithFocus = newFocusOwner;
    	playerPanelWithFocus = playerPanelList.get(playerWithFocus);

    	changeAvatarFocus();
    	playersScroll.getViewport().scrollRectToVisible(playerPanelWithFocus.getBounds());
    	populateDeckPanel(gType);

    	refreshPanels(true, true);
    }

    /** Changes avatar appearance dependant on focus player. */
    private void changeAvatarFocus() {
    	int index = 0;
    	for (FLabel avatar : avatarList) {
    		if (index == playerWithFocus) {
    	        avatar.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS).alphaColor(255), 3));
    	        avatar.setHoverable(false);
    		} else {
    	        avatar.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_THEME).alphaColor(200), 2));
    	        avatar.setHoverable(true);
    		}
    		index++;
    	}
    }

    /** Saves avatar prefs for players one and two. */
    private void updateAvatarPrefs() {
    	int pOneIndex = usedAvatars.get(0);
    	int pTwoIndex = usedAvatars.get(1);

        prefs.setPref(FPref.UI_AVATARS, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }

    /** Updates the avatars from preferences on update. */
    public void updatePlayersFromPrefs() {
    	ForgePreferences prefs = Singletons.getModel().getPreferences();
    	
    	// Avatar
        String[] avatarPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
    	for (int i = 0; i < avatarPrefs.length; i++) {
        	FLabel avatar = avatarList.get(i);
            int avatarIndex = Integer.parseInt(avatarPrefs[i]);
            avatar.setIcon(FSkin.getAvatars().get(avatarIndex));
            avatar.repaintSelf();
        	usedAvatars.put(i, avatarIndex);
        }

    	// Name
    	FTextField nameField = playerNameBtnList.get(0);
    	String prefName = prefs.getPref(FPref.PLAYER_NAME);
    	nameField.setText(StringUtils.isBlank(prefName) ? "Human" : prefName);
    }

    /** Adds a pre-styled FLabel component with the specified title. */
    private FLabel newLabel(String title) {
    	return new FLabel.Builder().text(title).fontSize(14).fontStyle(Font.ITALIC).build();
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

    	do {
    		newName = NameGenerator.getRandomName(gender, type, playerNames);
    		confirmMsg = "Would you like to use the name \"" + newName + "\", or try again?";
    	} while (!FOptionPane.showConfirmDialog(confirmMsg, title, "Use this name", "Try again", true));

        return newName;
    }

    /////////////////////////////////////////////
    //========== Various listeners in build order

    /** This listener unlocks the relevant buttons for players
     * and enables/disables archenemy combobox as appropriate. */
    private ItemListener iListenerVariants = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent arg0) {
            FCheckBox cb = (FCheckBox) arg0.getSource();
            GameType variantType = GameType.Constructed;

            if (cb == vntVanguard) {
            	variantType = GameType.Vanguard;
            	if (arg0.getStateChange() == ItemEvent.SELECTED) {
            		appliedVariants.add(variantType);
            		for (int i = 0; i < 8; i++) {
            			vgdSelectorBtns.get(i).setVisible(true);
            			changePlayerFocus(playerWithFocus, variantType);
            		}
                } else {
            		appliedVariants.remove(variantType);
            		for (int i = 0; i < 8; i++) {
            			vgdSelectorBtns.get(i).setVisible(false);
            			changePlayerFocus(playerWithFocus, GameType.Constructed);
            		}
                }
            }
            else if (cb == vntCommander) {
            	variantType = GameType.Commander;
            	if (arg0.getStateChange() == ItemEvent.SELECTED) {
            		appliedVariants.add(variantType);
                } else {
            		appliedVariants.remove(variantType);
                }
            }
            else if (cb == vntPlanechase) {
            	variantType = GameType.Planechase;
            	if (arg0.getStateChange() == ItemEvent.SELECTED) {
            		appliedVariants.add(variantType);
            		for (int i = 0; i < 8; i++) {
            			plnDeckSelectorBtns.get(i).setVisible(true);
            			plnEditors.get(i).setVisible(true);
            			changePlayerFocus(playerWithFocus, variantType);
            		}
                } else {
            		appliedVariants.remove(variantType);
            		for (int i = 0; i < 8; i++) {
            			plnDeckSelectorBtns.get(i).setVisible(false);
            			plnEditors.get(i).setVisible(false);
            			changePlayerFocus(playerWithFocus, GameType.Constructed);
            		}
                }
            }
            else if (cb == vntArchenemy) {
            	variantType = archenemyType.contains("Classic") ? GameType.Archenemy : GameType.ArchenemyRumble;
                comboArchenemy.setEnabled(vntArchenemy.isSelected());
            	if (arg0.getStateChange() == ItemEvent.SELECTED) {
            		appliedVariants.add(variantType);
                } else {
            		appliedVariants.remove(GameType.Archenemy);
            		appliedVariants.remove(GameType.ArchenemyRumble);
                }
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
			appliedVariants.remove(GameType.Archenemy);
    		appliedVariants.remove(GameType.ArchenemyRumble);
    		appliedVariants.add(archenemyType.contains("Classic")
    				? GameType.Archenemy : GameType.ArchenemyRumble);
		}
    };

    /** Listens to avatar buttons and gives the appropriate player focus. */
    private FocusAdapter avatarFocusListener = new FocusAdapter() {
		@Override
    	public void focusGained(FocusEvent e) {
    		int avatarOwnerID = avatarList.indexOf((FLabel)e.getSource());
			changePlayerFocus(avatarOwnerID);
		}
    };

    private FMouseAdapter avatarMouseListener = new FMouseAdapter() {
		@SuppressWarnings("serial")
		@Override
		public void onLeftClick(MouseEvent e) {
			final FLabel avatar = (FLabel)e.getSource();
			final int playerIndex = avatarList.indexOf(avatar);

			changePlayerFocus(playerIndex);
            avatar.requestFocusInWindow();

			final AvatarSelector aSel = new AvatarSelector(getPlayerName(playerIndex), usedAvatars.get(playerIndex), usedAvatars.values());
			for (final FLabel lbl : aSel.getSelectables()) {
				lbl.setCommand(new Command() {
					@Override
		            public void run() {
		                VSubmenuConstructed.this.setAvatar(avatar, playerIndex, Integer.valueOf(lbl.getName().substring(11)));
		                aSel.setVisible(false);
		            }
				});
			}

			aSel.setVisible(true);
	        aSel.dispose();

			if (playerIndex < 2) { updateAvatarPrefs(); }
		}
		@Override
        public void onRightClick(MouseEvent e) {
            FLabel avatar = (FLabel)e.getSource();
            int playerIndex = avatarList.indexOf(avatar);

            changePlayerFocus(playerIndex);
            avatar.requestFocusInWindow();

            setRandomAvatar(avatar, playerIndex);

            if (playerIndex < 2) { updateAvatarPrefs(); }
        }
    };

    private ActionListener nameListener = new ActionListener() {
    	@Override
		public void actionPerformed(ActionEvent e) {
			FTextField nField = (FTextField)e.getSource();
			nField.transferFocus();
		}
    };

    /** Listens to name text fields and gives the appropriate player focus.
     *  Also saves the name preference when leaving player one's text field. */
    private FocusAdapter nameFocusListener = new FocusAdapter() {
		@Override
		public void focusGained(FocusEvent e) {
			int panelOwnerID = 0;
			final Object source = e.getSource();

			if (source instanceof FTextField) { // the text box
				FTextField nField = (FTextField)source;
				panelOwnerID = playerNameBtnList.indexOf(nField);
			} else if (source instanceof FLabel) { // the name randomiser button
				FLabel randBtn = (FLabel)source;
				panelOwnerID = nameRandomisers.indexOf(randBtn);
			}

			changePlayerFocus(panelOwnerID);
		}

		@Override
		public void focusLost(FocusEvent e) {
			final Object source = e.getSource();
			if (source instanceof FTextField) { // the text box
				FTextField nField = (FTextField)source;
				String newName = nField.getText().trim();
			    if (playerNameBtnList.indexOf(nField) == 0 && !StringUtils.isBlank(newName)
			    		&& StringUtils.isAlphanumericSpace(newName) && prefs.getPref(FPref.PLAYER_NAME) != newName) {
                    prefs.setPref(FPref.PLAYER_NAME, newName);
                    prefs.save();
                }
			}
		}
    };

    private FMouseAdapter radioMouseAdapter = new FMouseAdapter() {
        @Override
        public void onLeftClick(MouseEvent e) {
            int radioID = playerTypeRadios.indexOf((FRadioButton)e.getSource());
            int radioOwnerID = (int) Math.floor(radioID / 2);
            avatarList.get(radioOwnerID).requestFocusInWindow();
            updateVanguardList(radioOwnerID);
        }
    };

    /** This listener will look for a vanguard avatar being selected in the lists
    / and update the corresponding detail panel. */
    private ListSelectionListener vgdLSListener = new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int index = vgdAvatarLists.indexOf(e.getSource());
            Object obj = vgdAvatarLists.get(index).getSelectedValue();

            if (obj instanceof PaperCard) {
                vgdAvatarDetails.get(index).setCard(Card.getCardForUi((IPaperCard) obj));
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
        humanListData.add("Random");
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
		Vector<Object> listData = new Vector<Object>();

		listData.add("Use deck's default avatar (random if unavailable)");
		listData.add("Random");

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
