package forge.gui.home.sanctioned;

import com.google.common.collect.Iterables;

import forge.UiCommand;
import forge.Singletons;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.game.player.LobbyPlayer;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiDialog;
import forge.gui.deckchooser.DeckgenUtil;
import forge.gui.deckchooser.DecksComboBox.DeckType;
import forge.gui.framework.ICDoc;
import forge.gui.menus.IMenuProvider;
import forge.gui.menus.MenuUtil;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FOptionPane;
import forge.item.PaperCard;
import forge.model.CardCollections;
import forge.net.FServer;
import forge.net.Lobby;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;
import forge.util.storage.IStorage;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        		final CardCollections cColl = Singletons.getModel().getDecks();
        		FList<Object> deckList;

				for (int i = 0; i < 8; i++) {
					// Archenemy: reinit deck list and restore last selections (if any)
					deckList = view.getSchemeDeckLists().get(i);
					Vector<Object> listData = new Vector<Object>();
					listData.add("Use deck's scheme section (random if unavailable)");
					listData.add("Generate");
					if (cColl.getScheme().size() > 0) {
						listData.add("Random");
						for (Deck schemeDeck : cColl.getScheme()) {
							listData.add(schemeDeck);
						}
					}
					Object val = deckList.getSelectedValue();
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
            	final List<GameType> variantTypes = new ArrayList<GameType>(4);
            	variantTypes.addAll(view.getAppliedVariants());
                startGame(variantTypes);
            }
        });

        final ForgePreferences prefs = Singletons.getModel().getPreferences();
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
    private void startGame(final List<GameType> variantTypes) {
    	if (variantTypes.contains(GameType.Commander)) {
            FOptionPane.showMessageDialog("Commander matches cannot currently be started via the "
            		+ "Constructed match setup screen. Please this variant then restart the match");
            return;
        }

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

    	boolean checkLegality = Singletons.getModel().getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);
        if (checkLegality) {
        	for (final int i : view.getParticipants()) {
        		String name = view.getPlayerName(i);
        		String errMsg = GameType.Constructed.getDecksFormat().getDeckConformanceProblem(view.getDeckChooser(i).getPlayer().getDeck());
        		if (null != errMsg) {
        			FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Deck");
        			return;
        		}
        	}
        }

        Lobby lobby = FServer.instance.getLobby();
        List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        for (final int i : view.getParticipants()) {
        	String name = view.getPlayerName(i);
            LobbyPlayer lobbyPlayer = view.isPlayerAI(i) ? lobby.getAiPlayer(name,
            		view.getPlayerAvatar(i)) : lobby.getGuiPlayer();
        	RegisteredPlayer rp = view.getDeckChooser(i).getPlayer();
        	rp.setTeamNumber(view.getTeam(i));

        	if (variantTypes.isEmpty()) {
        		players.add(rp.setPlayer(lobbyPlayer));
        	} else {
        		// Initialise Variant variables
        		Deck deck = rp.getDeck();
        		Iterable<PaperCard> schemes = null;
        		boolean playerIsArchenemy = view.isPlayerArchenemy(i);
        		Iterable<PaperCard> planes = null;
        		PaperCard vanguardAvatar = null;
                Random randomSeed = new Random();

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
                        IStorage<Deck> sDecks = Singletons.getModel().getDecks().getScheme();
                        if (sel.equals("Random") && sDecks.size() != 0) {
                        	schemePool = Aggregates.random(sDecks).get(DeckSection.Schemes);                            
                        }
                    } else {
                    	schemePool = ((Deck) selected).get(DeckSection.Schemes);
                    }
                    if (schemePool == null) { //Can be null if player deselects the list selection or chose Generate
                    	schemePool = DeckgenUtil.generateSchemeDeck();
                    }
                    if (checkLegality) {
                    	String errMsg = GameType.Archenemy.getDecksFormat().getSchemeSectionConformanceProblem(schemePool);
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
                        IStorage<Deck> pDecks = Singletons.getModel().getDecks().getPlane();
                        if (sel.equals("Random") && pDecks.size() != 0) {
                        	planePool = Aggregates.random(pDecks).get(DeckSection.Planes);                            
                        }
                    } else {
                    	planePool = ((Deck) selected).get(DeckSection.Planes);
                    }
                    if (planePool == null) { //Can be null if player deselects the list selection or chose Generate
                    	planePool = DeckgenUtil.generatePlanarDeck();
                    }
                    if (checkLegality) {
                    	String errMsg = GameType.Planechase.getDecksFormat().getPlaneSectionConformanceProblem(planePool);
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
                        		vanguardAvatar = Iterables.get(view.getAllAvatars(), randomSeed.nextInt(Iterables.size(view.getNonRandomHumanAvatars())));
                        	} else { //AI
                        		vanguardAvatar = Iterables.get(view.getAllAiAvatars(), randomSeed.nextInt(Iterables.size(view.getNonRandomAiAvatars())));
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

        		players.add(RegisteredPlayer.forVariants(variantTypes, rp.getDeck(), schemes,
        				playerIsArchenemy, planes, vanguardAvatar).setPlayer(lobbyPlayer));
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
