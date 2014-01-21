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

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.Command;
import forge.Singletons;
import forge.game.GameType;
import forge.gui.deckchooser.DecksComboBox.DeckType;
import forge.gui.deckchooser.FDeckChooser;
import forge.gui.deckeditor.DeckProxy;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FComboBox;
import forge.gui.toolbox.FComboBoxWrapper;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinColor;
import forge.gui.toolbox.FSkin.SkinImage;
import forge.gui.toolbox.FTextField;
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

    private final FCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final FCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");
    private final FCheckBox cbRemoveSmall = new FCheckBox("Remove Small Creatures");
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

        constructedFrame.add(new FScrollPanel(variantsPanel, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                "w 100%, gapbottom 10px, spanx 2, wrap");

        ////////////////////////////////////////////////////////
        ///////////////////// Player Panel /////////////////////

        // Construct individual player panels
        String constraints = "pushx, growx, wrap, hidemode 3";
        for (int i = 0; i < 8; i++) {
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
        playersFrame.add(playersScroll, "grow, push");

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
        populateDeckPanel(true);
        constructedFrame.add(decksFrame, "w 50%-5px, growy, pushy");
        constructedFrame.setOpaque(false);
        decksFrame.setOpaque(false);

        // Start Button
        final String strCheckboxConstraints = "h 30px!, gap 0 20px 0 0";
        pnlStart.setOpaque(false);
        pnlStart.add(cbSingletons, strCheckboxConstraints);
        pnlStart.add(btnStart, "span 1 3, growx, pushx, align center");
        pnlStart.add(cbArtifacts, strCheckboxConstraints);
        pnlStart.add(cbRemoveSmall, strCheckboxConstraints);
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
        playerPanel.add(newLabel("Name:"), "height 30px, gaptop 5px, gapx rel");
        playerPanel.add(playerNameField, "height 30px, pushx, growx");
        playerNameBtnList.add(playerNameField);

        // Name randomiser
        final FLabel newNameBtn = new FLabel.Builder().tooltip("Get a new random name").iconInBackground(false)
        		.icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT)).hoverable(true).opaque(false)
        		.unhoveredAlpha(0.9f).build();
        newNameBtn.setCommand(new Command() {
			@Override
			public void run() {
				newNameBtn.requestFocus();
    			String newName = getNewName();
    			int index = nameRandomisers.indexOf(newNameBtn);
    			FTextField nField = playerNameBtnList.get(index);

    			nField.setGhostText(newName);
    			nField.setText(newName);

				if (index == 0) {
					prefs.setPref(FPref.PLAYER_NAME, newName);
					prefs.save();
				}
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
                avatarList.get(deckSelectorBtns.indexOf(deckBtn)).requestFocusInWindow();
            }
        });
        playerPanel.add(newLabel("Deck:"), "height 30px, gapx rel");
        playerPanel.add(deckBtn, "height 30px, growx, pushx, spanx 4");
        deckSelectorBtns.add(deckBtn);

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
    		refreshPanels(true, false);
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
		refreshPanels(true, false);
    }

    public void updatePlayerName(int playerIndex) {
		String name = prefs.getPref(FPref.PLAYER_NAME);
		playerNameBtnList.get(0).setText(name);
    }

    public void refreshAvatarFromPrefs(int playerIndex) {
    	FLabel avatar = avatarList.get(playerIndex);
        String[] currentPrefs = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
    	if (playerIndex < currentPrefs.length) {
            int avatarIndex = Integer.parseInt(currentPrefs[playerIndex]);
            avatar.setIcon(FSkin.getAvatars().get(avatarIndex));
            avatar.repaintSelf();
        	usedAvatars.put(playerIndex, avatarIndex);
        } else {
        	setRandomAvatar(avatar, playerIndex);
        }
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

        // Main deck
        FPanel mainDeckPanel = new FPanel();
        mainDeckPanel.setLayout(new MigLayout(sectionConstraints));

        final FDeckChooser mainChooser = new FDeckChooser(isPlayerAI(playerIndex));
        mainChooser.initialize();
        mainChooser.getLstDecks().setSelectCommand(new Command() {
            @Override
            public void run() {
                VSubmenuConstructed.this.onDeckClicked(playerIndex, mainChooser.getSelectedDeckType(), mainChooser.getLstDecks().getSelectedItems());
            }
        });
        deckChoosers.add(mainChooser);
        mainDeckPanel.add(mainChooser, "grow, push, wrap");
    }

    protected void onDeckClicked(int iPlayer, DeckType type, Collection<DeckProxy> selectedDecks) {
        
        String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
        deckSelectorBtns.get(iPlayer).setText(text);
    }

    /** Populates the deck panel with the focused player's deck choices. */
    private void populateDeckPanel(final boolean firstBuild) {
    	if (!firstBuild) { decksFrame.removeAll(); }

    	decksFrame.add(deckChoosers.get(playerWithFocus), "grow, push");
    }

    /** Updates the deck selector button in all player panels. */
    public void updateDeckSelectorLabels() {
    	for (int i = 0; i < deckChoosers.size(); i++) {
    		updateDeckSelectorLabel(i);
    	}
    }

    /** Updates the deck selector button in the indexed player's panel. */
    private void updateDeckSelectorLabel(int playerIndex) {
    	final FLabel lbl = deckSelectorBtns.get(playerIndex);
    	String title = deckChoosers.get(playerIndex).getStateForLabel();

		if (!StringUtils.isBlank(title) && !lbl.getText().matches(title)) {
			lbl.setText(title);
		}
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

        for (FDeckChooser fdc : deckChoosers) {
        	fdc.populate();
        }
    	updateDeckSelectorLabels();

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
		if (newFocusOwner != playerWithFocus) {
			playerWithFocus = newFocusOwner;
			playerPanelWithFocus = playerPanelList.get(playerWithFocus);

			changeAvatarFocus();
			playersScroll.getViewport().scrollRectToVisible(playerPanelWithFocus.getBounds());
			populateDeckPanel(false);

			refreshPanels(true, true);
		}
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
    	final int typeIndex = FOptionPane.showOptionDialog(message, title, icon, typeOptions, 2);
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

            if (cb == vntVanguard) {
            	if (arg0.getStateChange() == ItemEvent.SELECTED) {
            		appliedVariants.add(GameType.Vanguard);
                } else {
            		appliedVariants.remove(GameType.Vanguard);
                }
            }
            else if (cb == vntCommander) {
            	if (arg0.getStateChange() == ItemEvent.SELECTED) {
            		appliedVariants.add(GameType.Commander);
                } else {
            		appliedVariants.remove(GameType.Commander);
                }
            }
            else if (cb == vntPlanechase) {
            	if (arg0.getStateChange() == ItemEvent.SELECTED) {
            		appliedVariants.add(GameType.Planechase);
                } else {
            		appliedVariants.remove(GameType.Planechase);
                }
            }
            else if (cb == vntArchenemy) {
                comboArchenemy.setEnabled(vntArchenemy.isSelected());
            	if (arg0.getStateChange() == ItemEvent.SELECTED) {
            		appliedVariants.add(archenemyType.contains("Classic")
            				? GameType.Archenemy : GameType.ArchenemyRumble);
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

			final AvatarSelector aSel = new AvatarSelector(usedAvatars.get(playerIndex), usedAvatars.values());
			for (final FLabel lbl : aSel.getSelectables()) {
				lbl.setCommand(new Command() {
					@Override
		            public void run() {
		                VSubmenuConstructed.this.setAvatar(avatar, playerIndex, Integer.valueOf(lbl.getName().substring(11)));
		                aSel.setVisible(false);
		                avatar.requestFocusInWindow();
		            }
				});
			}
			aSel.show(aSel);

			if (playerIndex < 2) { updateAvatarPrefs(); }
		}
		@Override
        public void onRightClick(MouseEvent e) {
            FLabel avatar = (FLabel)e.getSource();
            int playerIndex = avatarList.indexOf(avatar);

            changePlayerFocus(playerIndex);

            setRandomAvatar(avatar, playerIndex);
            avatar.requestFocusInWindow();

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
			    if (!StringUtils.isBlank(newName) && StringUtils.isAlphanumericSpace(newName) &&
			            prefs.getPref(FPref.PLAYER_NAME) != newName) {
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
}
