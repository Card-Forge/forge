package forge.gui.home.sanctioned;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.game.GameType;
import forge.gui.deckchooser.FDeckChooser;
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
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextField;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.MyRandom;

/**
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuConstructed implements IVSubmenu<CSubmenuConstructed> {
    /** */
    SINGLETON_INSTANCE;
    private final static ForgePreferences prefs = Singletons.getModel().getPreferences();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Constructed Mode");

    /** */
    // General variables 
    private final LblHeader lblTitle = new LblHeader("Sanctioned Format: Constructed");
    private int activePlayersNum = 2;
    private int playerWithFocus = 0; // index of the player that currently has focus

    private final JCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final JCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");
    private final JCheckBox cbRemoveSmall = new FCheckBox("Remove Small Creatures");
    private final StartButton btnStart  = new StartButton();
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    private final JPanel constructedFrame = new JPanel(new MigLayout("insets 0, gap 5, wrap 2, fill")); // Main content frame

    // Variants frame and variables
    private final Set<GameType> appliedVariants = new TreeSet<GameType>();
    private final FPanel variantsPanel = new FPanel(new MigLayout("insets 10, gapx 20, fillx, hmax 100, nogrid"));
    private final FCheckBox vntVanguard = new FCheckBox("Vanguard");
    private final FCheckBox vntCommander = new FCheckBox("Commander");
    private final FCheckBox vntPlanechase = new FCheckBox("Planechase");
    private final FCheckBox vntArchenemy = new FCheckBox("Archenemy");
    private String archenemyType = "Classic";
    private final FComboBox<String> comboArchenemy = new FComboBox<>(new String[]{
    		"Classic Archenemy (player 1 is Archenemy)", "Archenemy Rumble (All players are Archenemies)"});

    // Player frame elements
    private final FPanel playersFrame = new FPanel(new MigLayout("insets 0, gapx 20, fill, w 50%"));
    private final FScrollPanel playersScroll = new FScrollPanel(new MigLayout("insets 8, gapx 20, fill"));
    private final List<FPanel> playerPanelList = new ArrayList<FPanel>(8);
    private final List<FPanel> activePlayerPanelList = new ArrayList<FPanel>(8);
    private final List<FPanel> inactivePlayerPanelList = new ArrayList<FPanel>(6);
    private final List<FTextField> playerNameBtnList = new ArrayList<FTextField>(8);
    private final List<JRadioButton> playerTypeRadios = new ArrayList<JRadioButton>(8);
    private final String[] avatarPrefs = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
    private final List<FLabel> avatarList = new ArrayList<FLabel>(8);
    private final TreeMap<Integer, Integer> usedAvatars = new TreeMap<Integer, Integer>();

    private final List<FLabel> closePlayerBtnList = new ArrayList<FLabel>(6);
    private final FLabel addPlayerBtn = new FLabel.Builder().opaque(true).hoverable(true).text("Add a Player").build();

    // Deck frame elements
    private final FPanel decksFrame = new FPanel(new MigLayout("insets 8, gapx 20, w 50%"));
    private final List<FPanel> deckPanelListMain = new ArrayList<FPanel>(8);
    private final List<FDeckChooser> deckChoosers = new ArrayList<FDeckChooser>(8);
    private final FLabel deckChooserHeader = new FLabel.Builder().opaque(true).fontStyle(1).build();
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

        constructedFrame.add(newLabel("Variants:"), "wrap");
        variantsPanel.setOpaque(false);
        variantsPanel.add(vntVanguard, "growx");
        variantsPanel.add(vntCommander, "growx");
        variantsPanel.add(vntPlanechase, "growx, wrap");
        variantsPanel.add(vntArchenemy);
        variantsPanel.add(comboArchenemy, "pushx");
        
        constructedFrame.add(variantsPanel, "growx, spanx 2");

        ////////////////////////////////////////////////////////
        ///////////////////// Player Panel /////////////////////

        // Construct individual player panels
        for (int i = 0; i < 8; i++) {
        	buildPlayerPanel(i);
        	FPanel player = playerPanelList.get(i);

        	// Populate players panel
        	if (i < activePlayersNum) {
        		playersScroll.add(player, "pushx, growx, wrap, hidemode 3");
        		activePlayerPanelList.add(player);
        	} else {
        		player.setVisible(false);
        		playersScroll.add(player, "pushx, growx, wrap, hidemode 3");
        		inactivePlayerPanelList.add(player);
        	}
        }

        addPlayerBtn.setFocusable(true);
        addPlayerBtn.addMouseListener(addOrRemoveMouseListener);
        addPlayerBtn.addKeyListener(addOrRemoveKeyListener);
    	playersScroll.add(addPlayerBtn, "height 40px, growx, pushx");
        playersFrame.add(playersScroll, "grow, pushx, NORTH");
        constructedFrame.add(playersFrame, "grow, push");

        ////////////////////////////////////////////////////////
        ////////////////////// Deck Panel //////////////////////

        for (int i = 0; i < 8; i++) {
        	buildDeckPanel(i);
        }
        populateDeckPanel(true);
        constructedFrame.add(decksFrame, "grow, push");
        constructedFrame.setOpaque(false);

        // Start Button
        final String strCheckboxConstraints = "h 30px!, gap 0 20px 0 0";
        pnlStart.setOpaque(false);
        pnlStart.add(cbSingletons, strCheckboxConstraints);
        pnlStart.add(btnStart, "span 1 3, growx, pushx, align center");
        pnlStart.add(cbArtifacts, strCheckboxConstraints);
        pnlStart.add(cbRemoveSmall, strCheckboxConstraints);
    }

    private FPanel buildPlayerPanel(final int playerIndex) {
        FPanel playerPanel = new FPanel();
        playerPanel.setLayout(new MigLayout("insets 10, gap 5px"));
    	
        // Avatar
        final FLabel avatar = new FLabel.Builder().opaque(true).hoverable(true)
        		.iconScaleFactor(0.99f).iconInBackground(true).build();
        if (playerIndex < avatarPrefs.length) {
            int avatarIndex = Integer.parseInt(avatarPrefs[playerIndex]);
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
        	name = "Player " + (playerIndex + 1);
        }
        final FTextField playerNameField = new FTextField.Builder().ghostText(name).text(name).build();
        playerNameField.setFocusable(true);
        playerNameField.addActionListener(nameListener);
        playerNameField.addFocusListener(nameFocusListener);
        playerPanel.add(newLabel("Player Name:"),"height 35px, gapx rel");
        playerPanel.add(playerNameField, "height 35px, gapy 5px, gapx unrel, pushx, growx, wrap 5");
        playerNameBtnList.add(playerNameField);

        // PlayerType
        ButtonGroup tempBtnGroup = new ButtonGroup();
        FRadioButton tmpHuman = new FRadioButton();
        tmpHuman.setText("Human");
        tmpHuman.setSelected(playerIndex == 0);
        tmpHuman.addFocusListener(radioFocusListener);
        FRadioButton tmpAI = new FRadioButton();
        tmpAI.setText("AI");
        tmpAI.setSelected(playerIndex != 0);
        tmpAI.addFocusListener(radioFocusListener);

        FPanel typeBtnPanel = new FPanel();
        typeBtnPanel.add(tmpHuman);
        typeBtnPanel.add(tmpAI);
        playerPanel.add(newLabel("Player Type:"), "height 35px, gapx rel");
        playerPanel.add(typeBtnPanel, "height 35px, gapy 5px, gapx unrel, pushx, growx, wrap");

        tempBtnGroup.add(tmpHuman);
        tempBtnGroup.add(tmpAI);
        playerTypeRadios.add(tmpHuman);
        playerTypeRadios.add(tmpAI);

        // Deck selector button
        FLabel deckBtn = new FLabel.ButtonBuilder().text("Select a deck").build();
        deckBtn.addFocusListener(deckLblFocusListener);
        deckBtn.addMouseListener(deckLblMouseListener);
        playerPanel.add(deckBtn, "height 30px, gapy 5px, growx, wrap, span 3 1");
        deckSelectorBtns.add(deckBtn);

        // Add a button to players 3+ to remove them from the setup
        if (playerIndex >= 2) {
        	FLabel closeBtn = new FLabel.Builder().opaque(true).hoverable(true).text("X").fontSize(10).build();
        	closeBtn.addMouseListener(addOrRemoveMouseListener);
        	closeBtn.addKeyListener(addOrRemoveKeyListener);
        	playerPanel.add(closeBtn, "w 15, h 15, pos (container.w-15) 0");
        	closePlayerBtnList.add(closeBtn);
        }

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

        	avatarList.get(playerPanelList.indexOf(player)).grabFocus();
    		refreshPanels(true, false);
    	}
    }

    private void removePlayer(int playerIndex) {
    	activePlayersNum--;
		FPanel player = playerPanelList.get(playerIndex);
    	player.setVisible(false);
		inactivePlayerPanelList.add(player);
		activePlayerPanelList.remove(player);

		refreshPanels(true, false);
    }

    public void updatePlayerName(int playerIndex) {
		String name = prefs.getPref(FPref.PLAYER_NAME);
		playerNameBtnList.get(0).setGhostText(name);
		playerNameBtnList.get(0).setText(name);
    }

    /** Applies a random avatar, avoiding avatars already used. 
     * @param playerIndex */
    private void setRandomAvatar(FLabel avatar, int playerIndex) {
        int random = 0;
        do {
            random = MyRandom.getRandom().nextInt(FSkin.getAvatars().size()); 
        } while (usedAvatars.values().contains(random));

        avatar.setIcon(FSkin.getAvatars().get(random));
        avatar.repaintSelf();
    	usedAvatars.put(playerIndex, random);
    }

    /** Builds the actual deck panel layouts for each player.
     * These are added to a list which can be referenced to populate the deck panel appropriately. */
    private void buildDeckPanel(final int playerIndex) {
    	String sectionConstraints = "insets 8";
    	
        // Main deck
        FPanel mainDeckPanel = new FPanel();
        mainDeckPanel.setLayout(new MigLayout(sectionConstraints));

        FDeckChooser mainChooser = new FDeckChooser("Main deck:", isPlayerAI(playerIndex));
        mainChooser.initialize();
        deckChoosers.add(mainChooser);
        mainDeckPanel.add(mainChooser, "grow, push, wrap");
        deckPanelListMain.add(mainDeckPanel);
    }

    /** Populates the deck panel with the focused player's deck choices. */
    private void populateDeckPanel(final boolean firstBuild) {
    	if (!firstBuild) { decksFrame.removeAll(); }

        String name = getPlayerName(playerWithFocus);
        deckChooserHeader.setText("Select a deck for " + name);

    	decksFrame.add(deckChooserHeader, "gap 0, pushx, growx, w 100%, h 35, wrap");
    	decksFrame.add(deckPanelListMain.get(playerWithFocus), "gap 0, grow, push, wrap");
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

        for(FDeckChooser fdc : deckChoosers) {
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
    	    playersFrame.validate();
    	    playersFrame.repaint();
    	}
    	if (refreshDeckFrame) {
    	    decksFrame.validate();
    	    decksFrame.repaint();
    	}
    }

    private void changePlayerFocus(int newFocusOwner) {
		if (newFocusOwner != playerWithFocus) {
			playerWithFocus = newFocusOwner;
			FPanel playerPanel = playerPanelList.get(playerWithFocus);

			//TODO (Marc) Style the actual panel for more visible indication of player with focus:
			/*for (FPanel itrPanel : playerPanelList) {
	    		if (itrPanel == playerPanel) {
	    			// Make panel less opaque
	    		} else {
	    			// restore default panel opacity
	    		}
	    	}*/

			changeAvatarFocus();
        	playersScroll.getViewport().scrollRectToVisible(playerPanel.getBounds());
			populateDeckPanel(false);

			refreshPanels(true, true);
			System.out.println("Focus changed to player " + (playerWithFocus + 1));
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
    	FLabel label = new FLabel.Builder().text(title).fontSize(11).fontStyle(Font.ITALIC).build();

    	return label;
    }

    /////////////////////////////////////////////
    //========== Various listeners in build order

    /** This listener unlocks the relevant buttons for players
     * and enables/disables archenemy combobox as appropriate. */
    ItemListener iListenerVariants = new ItemListener() {

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
            System.out.println("The following variants are applied: " + appliedVariants);
        }
    };

    // Listens to the archenemy combo box
    ActionListener aeComboListener = new ActionListener() {

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
    private FocusListener avatarFocusListener = new FocusListener() {

		@Override
    	public void focusGained(FocusEvent e) {
    		int avatarOwnerID = avatarList.indexOf((FLabel)e.getSource());
			changePlayerFocus(avatarOwnerID);
		}

		@Override
		public void focusLost(FocusEvent e) {
			
		}
    };

    private MouseListener avatarMouseListener = new MouseListener() {

		@Override
		public void mouseClicked(MouseEvent e) {
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			FLabel avatar = (FLabel)e.getSource();
			int playerIndex = avatarList.indexOf(avatar);

			changePlayerFocus(playerIndex);

			if (e.getButton() == 1) {
				avatar.grabFocus();
				// TODO: Do avatar selection, giving current avatar focus for keyboard control
			}

			if (e.getButton() == 3) {
				setRandomAvatar(avatar, playerIndex);
				avatar.grabFocus();
			}

			if (playerIndex < 2) { updateAvatarPrefs(); }
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
		}
    };

    /** Listens to the name text field and resets the adjusts player 1's name preference. */
    private ActionListener nameListener = new ActionListener() {

    	@Override
		public void actionPerformed(ActionEvent e) {
			FTextField nField = (FTextField)e.getSource();
			String newName = nField.getText().trim();
			String oldName = nField.getGhostText().trim();

			if (!StringUtils.isEmpty(newName) && !StringUtils.isBlank(newName)
					&& StringUtils.isAlphanumericSpace(newName) && !newName.equals(oldName)) {
				nField.setGhostText(newName);

		        deckChooserHeader.setText("Select a deck for " + newName); 

				if (playerNameBtnList.indexOf(nField) == 0) {
				    prefs.setPref(FPref.PLAYER_NAME, newName);
		            prefs.save();
				}
			}

			nField.transferFocus();
		}
    };

    /** Listens to name text fields and gives the appropriate player focus.
     *  Also saves the name preference when leaving player one's text field. */
    private FocusListener nameFocusListener = new FocusListener() {

		@Override
		public void focusGained(FocusEvent e) {
			FTextField nField = (FTextField)e.getSource();
			int panelOwnerID = playerNameBtnList.indexOf(nField);
			changePlayerFocus(panelOwnerID);
		}

		@Override
		public void focusLost(FocusEvent e) {
			FTextField nField = (FTextField)e.getSource();
			String newName = nField.getText().trim();
			String oldName = nField.getGhostText().trim();

			if (!StringUtils.isEmpty(newName) && !StringUtils.isBlank(newName)
					&& StringUtils.isAlphanumericSpace(newName) && !newName.equals(oldName)) {
				nField.setGhostText(newName);

		        deckChooserHeader.setText("Select a deck for " + newName); 

				if (playerNameBtnList.indexOf(nField) == 0) {
				    prefs.setPref(FPref.PLAYER_NAME, newName);
		            prefs.save();
				}
			}				
		}
    };

    /** Listens to name player type radio buttons and gives the appropriate player focus. */
    private FocusListener radioFocusListener = new FocusListener() {

		@Override
    	public void focusGained(FocusEvent e) {
    		int radioID = playerTypeRadios.indexOf((FRadioButton)e.getSource());
			int radioOwnerID = (int) Math.floor(radioID/2);
			changePlayerFocus(radioOwnerID);
		}

		@Override
		public void focusLost(FocusEvent e) {
			
		}
    };

    /** Listens to deck select buttons and gives the appropriate player focus. */
    private FocusListener deckLblFocusListener = new FocusListener() {

		@Override
    	public void focusGained(FocusEvent e) {
    		int deckLblID = deckSelectorBtns.indexOf((FLabel)e.getSource());
			changePlayerFocus(deckLblID);
			updateDeckSelectorLabel(deckLblID);
		}

		@Override
		public void focusLost(FocusEvent e) {
			
		}
    };

    private MouseListener deckLblMouseListener = new MouseListener() {

		@Override
		public void mouseClicked(MouseEvent e) {
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			FLabel deckLbl = (FLabel)e.getSource();
			changePlayerFocus(deckSelectorBtns.indexOf(deckLbl));
			// TODO: Give focus to deck chooser
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
		}
    };

    private MouseListener addOrRemoveMouseListener = new MouseListener() {

		@Override
		public void mouseClicked(MouseEvent e) {
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			FLabel btn = (FLabel)e.getSource();
			if (btn == addPlayerBtn) {
				addPlayer();
			} else {
				removePlayer(closePlayerBtnList.indexOf(btn) + 2);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
		}
    };

    private KeyListener addOrRemoveKeyListener = new KeyListener() {

		@Override
		public void keyTyped(KeyEvent e) {
			
		}

		@Override
		public void keyPressed(KeyEvent e) {
			
		}

		@Override
		public void keyReleased(KeyEvent e) {
			FLabel btn = (FLabel)e.getSource();
			if (btn == addPlayerBtn) {
				addPlayer();
			} else {
				removePlayer(closePlayerBtnList.indexOf(btn) + 2);
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

}
