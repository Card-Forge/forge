package forge.screens.home.sanctioned;

import forge.GuiBase;
import forge.LobbyPlayer;
import forge.UiCommand;
import forge.Singletons;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckType;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiDialog;
import forge.gui.framework.ICDoc;
import forge.interfaces.IGuiBase;
import forge.item.PaperCard;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.model.CardCollections;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.util.Aggregates;
import forge.util.storage.IStorage;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

//import forge.gui.home.variant.VSubmenuVanguard;

/**
 * Controls the constructed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuConstructed implements ICDoc, IMenuProvider {
    /** */
    SINGLETON_INSTANCE;

    private final VSubmenuConstructed view = VSubmenuConstructed.SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {

        MenuUtil.setMenuProvider(this);

        SwingUtilities.invokeLater(new Runnable() {
        	@Override public void run() {
        		final CardCollections cColl = FModel.getDecks();
        		FList<Object> deckList;
        		Vector<Object> listData;
        		Object val;

				for (int i = 0; i < 8; i++) {
					// Commander: reinit deck list and restore last selections (if any)
					deckList = view.getCommanderDeckLists().get(i);
					listData = new Vector<Object>();
					listData.add("Generate");
					if (cColl.getCommander().size() > 0) {
						listData.add("Random");
						for (Deck comDeck : cColl.getCommander()) {
							listData.add(comDeck);
						}
					}
					val = deckList.getSelectedValue();
					deckList.setListData(listData);
					if (null != val) {
						deckList.setSelectedValue(val, true);
					}
					if (-1 == deckList.getSelectedIndex()) {
						deckList.setSelectedIndex(0);
					} // End Commander

					// Archenemy: reinit deck list and restore last selections (if any)
					deckList = view.getSchemeDeckLists().get(i);
					listData = new Vector<Object>();
					listData.add("Use deck's scheme section (random if unavailable)");
					listData.add("Generate");
					if (cColl.getScheme().size() > 0) {
						listData.add("Random");
						for (Deck schemeDeck : cColl.getScheme()) {
							listData.add(schemeDeck);
						}
					}
					val = deckList.getSelectedValue();
					deckList.setListData(listData);
					if (null != val) {
						deckList.setSelectedValue(val, true);
					}
					if (-1 == deckList.getSelectedIndex()) {
						deckList.setSelectedIndex(0);
					} // End Archenemy

	        		// Planechase: reinit deck lists and restore last selections (if any)
	        		deckList = view.getPlanarDeckLists().get(i);
	        		listData = new Vector<Object>();            

	        		listData.add("Use deck's planes section (random if unavailable)");
	        		listData.add("Generate");
	        		if (cColl.getPlane().size() > 0) {
	        			listData.add("Random");
	        			for (Deck planarDeck : cColl.getPlane()) {
	        				listData.add(planarDeck);
	        			}                
	        		}

	        		val = deckList.getSelectedValue();
	        		deckList.setListData(listData);
	        		if (null != val) {
	        			deckList.setSelectedValue(val, true);
	        		}

	        		if (-1 == deckList.getSelectedIndex()) {
	        			deckList.setSelectedIndex(0);
	        		} // End Planechase

        			view.updateVanguardList(i);
        		}

        		// General updates when switching back to this view
        		view.updatePlayersFromPrefs();
        		view.getBtnStart().requestFocusInWindow();
        	}
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
	@Override
    public void initialize() {
    	view.getDeckChooser(0).initialize(FPref.CONSTRUCTED_P1_DECK_STATE, DeckType.PRECONSTRUCTED_DECK);
    	view.getDeckChooser(1).initialize(FPref.CONSTRUCTED_P2_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(2).initialize(FPref.CONSTRUCTED_P3_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(3).initialize(FPref.CONSTRUCTED_P4_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(4).initialize(FPref.CONSTRUCTED_P5_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(5).initialize(FPref.CONSTRUCTED_P6_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(6).initialize(FPref.CONSTRUCTED_P7_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(7).initialize(FPref.CONSTRUCTED_P8_DECK_STATE, DeckType.COLOR_DECK);

    	// Start button event handling
        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                startGame(view.getAppliedVariants());
            }
        });

        final ForgePreferences prefs = FModel.getPreferences();
        // Checkbox event handling
        view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(FPref.DECKGEN_SINGLETONS, String.valueOf(view.getCbSingletons().isSelected()));
                prefs.save();
            }
        });

        view.getCbArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(FPref.DECKGEN_ARTIFACTS, String.valueOf(view.getCbArtifacts().isSelected()));
                prefs.save();
            }
        });

        // Pre-select checkboxes
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
    }

    /** Starts a match with the applied variants. */
    private void startGame(final Set<GameType> variantTypes) {
    	if (!view.isEnoughTeams()) {
    	    FOptionPane.showMessageDialog("There are not enough teams! Please adjust team allocations.");
    	    return;
    	}

        for (final int i : view.getParticipants()) {
        	if (view.getDeckChooser(i).getPlayer() == null) {
                FOptionPane.showMessageDialog("Please specify a deck for " + view.getPlayerName(i));
                return;
            }
        } // Is it even possible anymore? I think current implementation assigns decks automatically.

        GameType autoGenerateVariant = null;
        boolean isCommanderMatch = false;
        if (!variantTypes.isEmpty()) {
            isCommanderMatch = variantTypes.contains(GameType.Commander);
            if (!isCommanderMatch) {
                for (GameType variant : variantTypes) {
                    if (variant.isAutoGenerated()) {
                        autoGenerateVariant = variant;
                        break;
                    }
                }
            }
        }

    	boolean checkLegality = FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);

    	//Auto-generated decks don't need to be checked here
        //Commander deck replaces regular deck and is checked later
        if (checkLegality && autoGenerateVariant == null && !isCommanderMatch) {
        	for (final int i : view.getParticipants()) {
        		String name = view.getPlayerName(i);
        		String errMsg = GameType.Constructed.getDeckFormat().getDeckConformanceProblem(view.getDeckChooser(i).getPlayer().getDeck());
        		if (null != errMsg) {
        			FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Deck");
        			return;
        		}
        	}
        }

        IGuiBase fc = GuiBase.getInterface();
        List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        for (final int i : view.getParticipants()) {
        	String name = view.getPlayerName(i);
            LobbyPlayer lobbyPlayer = view.isPlayerAI(i) 
            		? fc.createAiPlayer(name, view.getPlayerAvatar(i)) 
            		: fc.getGuiPlayer();
            RegisteredPlayer rp = view.getDeckChooser(i).getPlayer();

            if (variantTypes.isEmpty()) {
                rp.setTeamNumber(view.getTeam(i));
                players.add(rp.setPlayer(lobbyPlayer));
            }
            else {
                Deck deck = null;
                PaperCard vanguardAvatar = null;
        		if (isCommanderMatch) {
                    Object selected = view.getCommanderDeckLists().get(i).getSelectedValue();
                    if (selected instanceof String) {
                        String sel = (String) selected;
                        IStorage<Deck> comDecks = FModel.getDecks().getCommander();
                        if (sel.equals("Random") && comDecks.size() > 0) {
                        	deck = Aggregates.random(comDecks);                            
                        }
                    }
                    else {
                    	deck = (Deck) selected;
                    }
                    if (deck == null) { //Can be null if player deselects the list selection or chose Generate
                    	deck = DeckgenUtil.generateCommanderDeck(view.isPlayerAI(i));
                    }
                    if (checkLegality) {
                    	String errMsg = GameType.Commander.getDeckFormat().getDeckConformanceProblem(deck);
                        if (null != errMsg) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Commander Deck");
                            return;
                        }
                    }
        		}
                else if (autoGenerateVariant != null) {
                    deck = autoGenerateVariant.autoGenerateDeck(rp);
                    CardPool avatarPool = deck.get(DeckSection.Avatar);
                    if (avatarPool != null) {
                        vanguardAvatar = avatarPool.get(0);
                    }
                }

        		// Initialise variables for other variants
        		deck = deck == null ? rp.getDeck() : deck;
        		Iterable<PaperCard> schemes = null;
        		boolean playerIsArchenemy = view.isPlayerArchenemy(i);
        		Iterable<PaperCard> planes = null;

        		//Archenemy
        		if (variantTypes.contains(GameType.ArchenemyRumble)
                        || (variantTypes.contains(GameType.Archenemy) && playerIsArchenemy)) {
                    Object selected = view.getSchemeDeckLists().get(i).getSelectedValue();
                    CardPool schemePool = null;
                    if (selected instanceof String) {
                        String sel = (String) selected;
                        if (sel.contains("Use deck's scheme section")) {
                        	if (deck.has(DeckSection.Schemes)) {
                        		schemePool = deck.get(DeckSection.Schemes);
                        	} else {
                        		sel = "Random";
                        	}
                        }
                        IStorage<Deck> sDecks = FModel.getDecks().getScheme();
                        if (sel.equals("Random") && sDecks.size() != 0) {
                        	schemePool = Aggregates.random(sDecks).get(DeckSection.Schemes);                            
                        }
                    } else {
                    	schemePool = ((Deck) selected).get(DeckSection.Schemes);
                    }
                    if (schemePool == null) { //Can be null if player deselects the list selection or chose Generate
                    	schemePool = DeckgenUtil.generateSchemePool();
                    }
                    if (checkLegality) {
                    	String errMsg = GameType.Archenemy.getDeckFormat().getSchemeSectionConformanceProblem(schemePool);
                        if (null != errMsg) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Scheme Deck");
                            return;
                        }
                    }
                    schemes = schemePool.toFlatList();
        		}

        		//Planechase
        		if (variantTypes.contains(GameType.Planechase)) {
                    Object selected = view.getPlanarDeckLists().get(i).getSelectedValue();
                    CardPool planePool = null;
                    if (selected instanceof String) {
                        String sel = (String) selected;
                        if (sel.contains("Use deck's planes section")) {
                        	if (deck.has(DeckSection.Planes)) {
                        		planePool = deck.get(DeckSection.Planes);
                        	} else {
                        		sel = "Random";
                        	}
                        }
                        IStorage<Deck> pDecks = FModel.getDecks().getPlane();
                        if (sel.equals("Random") && pDecks.size() != 0) {
                        	planePool = Aggregates.random(pDecks).get(DeckSection.Planes);                            
                        }
                    } else {
                    	planePool = ((Deck) selected).get(DeckSection.Planes);
                    }
                    if (planePool == null) { //Can be null if player deselects the list selection or chose Generate
                    	planePool = DeckgenUtil.generatePlanarPool();
                    }
                    if (checkLegality) {
                    	String errMsg = GameType.Planechase.getDeckFormat().getPlaneSectionConformanceProblem(planePool);
                        if (null != errMsg) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Planar Deck");
                            return;
                        }
                    }
                    planes = planePool.toFlatList();
        		}

        		//Vanguard
        		if (variantTypes.contains(GameType.Vanguard)) {
                    Object selected = view.getVanguardLists().get(i).getSelectedValue();
                    if (selected instanceof String) {
                        String sel = (String) selected;
                        if (sel.contains("Use deck's default avatar") && deck.has(DeckSection.Avatar)) {
                        	vanguardAvatar = deck.get(DeckSection.Avatar).get(0);
                        } else { //Only other string is "Random"
                        	if (!view.isPlayerAI(i)) { //Human
                        		vanguardAvatar = Aggregates.random(view.getNonRandomHumanAvatars());
                        	} else { //AI
                        		vanguardAvatar = Aggregates.random(view.getNonRandomAiAvatars());
                        	}
                        }
                    } else {
                    	vanguardAvatar = (PaperCard)selected;
                    }
                    if (vanguardAvatar == null) { //ERROR! null if avatar deselected on list
                        GuiDialog.message("No Vanguard avatar selected for " + name
                        		+ ". Please choose one or disable the Vanguard variant");
                        return;
                    }
        		}

            	rp = RegisteredPlayer.forVariants(variantTypes, deck, schemes, playerIsArchenemy, planes, vanguardAvatar);
            	rp.setTeamNumber(view.getTeam(i));
        		players.add(rp.setPlayer(lobbyPlayer));
        	}
        	view.getDeckChooser(i).saveState();
        }
        
        Singletons.getControl().startMatch(GameType.Constructed, variantTypes, players);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(ConstructedGameMenu.getMenu());
        return menus;
    }

}
