package forge.screens.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.SwingUtilities;

import com.google.common.collect.Iterables;

import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deckchooser.FDeckChooser;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FList;

public class CLobby {

    private final VLobby view;
    public CLobby(final VLobby view) {
        this.view = view;
        this.view.setForCommander(true);
    }

    private void addDecks(final Iterable<DeckProxy> commanderDecks, FList<Object> deckList, String... initialItems) {
        Vector<Object> listData = new Vector<>();
        listData.addAll(Arrays.asList(initialItems));
        listData.add("Generate");
        if (!Iterables.isEmpty(commanderDecks)) {
            listData.add("Random");
            for (DeckProxy comDeck : commanderDecks) {
                listData.add(comDeck.getDeck());
            }
        }
        Object val = deckList.getSelectedValue();
        deckList.setListData(listData);
        if (null != val) {
            deckList.setSelectedValue(val, true);
        }
        if (-1 == deckList.getSelectedIndex()) {
            deckList.setSelectedIndex(0);
        }
    }
    
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public final void run() {
                final Iterable<DeckProxy> schemeDecks = DeckProxy.getAllSchemeDecks();
                final Iterable<DeckProxy> planarDecks = DeckProxy.getAllPlanarDecks();

                for (int i = 0; i < VLobby.MAX_PLAYERS; i++) {
                    addDecks(schemeDecks, view.getSchemeDeckLists().get(i),
                            "Use deck's scheme section (random if unavailable)");
                    addDecks(planarDecks, view.getPlanarDeckLists().get(i),
                            "Use deck's planes section (random if unavailable)");
                    view.updateVanguardList(i);
                }

                // General updates when switching back to this view
                view.getBtnStart().requestFocusInWindow();
            }
        });
    }

    public void initialize() {
        for (int iSlot = 0; iSlot < VLobby.MAX_PLAYERS; iSlot++) {
            final FDeckChooser fdc = view.getDeckChooser(iSlot);
            fdc.initialize(FPref.CONSTRUCTED_DECK_STATES[iSlot], defaultDeckTypeForSlot(iSlot));
            fdc.populate();
            /*fdc.getDecksComboBox().addListener(new IDecksComboBoxListener() {
                @Override public final void deckTypeSelected(final DecksComboBoxEvent ev) {
                    view.focusOnAvatar();
                }
            });*/
            final FDeckChooser fdccom = view.getCommanderDeckChooser(iSlot);
            fdccom.initialize(FPref.COMMANDER_DECK_STATES[iSlot], defaultDeckTypeForCommanderSlot(iSlot));
            fdccom.populate();
            final FDeckChooser fdobcom = view.getOathbreakerDeckChooser(iSlot);
            fdobcom.initialize(FPref.OATHBREAKER_DECK_STATES[iSlot], defaultDeckTypeForOathbreakerSlot(iSlot));
            fdobcom.populate();
            final FDeckChooser fdtlcom = view.getTinyLeaderDeckChooser(iSlot);
            fdtlcom.initialize(FPref.TINY_LEADER_DECK_STATES[iSlot], defaultDeckTypeForTinyLeaderSlot(iSlot));
            fdtlcom.populate();
            final FDeckChooser fdbcom = view.getBrawlDeckChooser(iSlot);
            fdbcom.initialize(FPref.BRAWL_DECK_STATES[iSlot], defaultDeckTypeForBrawlSlot(iSlot));
            fdbcom.populate();
        }

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

    private static DeckType defaultDeckTypeForSlot(final int iSlot) {
        return iSlot == 0 ? DeckType.PRECONSTRUCTED_DECK : DeckType.COLOR_DECK;
    }

    private static DeckType defaultDeckTypeForCommanderSlot(final int iSlot) {
        return iSlot == 0 ? DeckType.COMMANDER_DECK : DeckType.RANDOM_CARDGEN_COMMANDER_DECK;
    }

    private static DeckType defaultDeckTypeForOathbreakerSlot(final int iSlot) {
        return iSlot == 0 ? DeckType.OATHBREAKER_DECK : DeckType.RANDOM_CARDGEN_COMMANDER_DECK;
    }

    private static DeckType defaultDeckTypeForTinyLeaderSlot(final int iSlot) {
        return iSlot == 0 ? DeckType.TINY_LEADERS_DECK : DeckType.RANDOM_CARDGEN_COMMANDER_DECK;
    }

    private static DeckType defaultDeckTypeForBrawlSlot(final int iSlot) {
        return iSlot == 0 ? DeckType.BRAWL_DECK : DeckType.CUSTOM_DECK;
    }
}
