package forge.screens.home;

import java.util.Arrays;
import java.util.Vector;

import javax.swing.SwingUtilities;

import com.google.common.collect.Iterables;

import forge.deck.DeckProxy;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FList;

public class CLobby {

    private final VLobby view;
    public CLobby(final VLobby view) {
        this.view = view;
    }

    private void addDecks(final Iterable<DeckProxy> commanderDecks, FList<Object> deckList, String... initialItems) {
        Vector<Object> listData = new Vector<>(Arrays.asList(initialItems));
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
        SwingUtilities.invokeLater(() -> {
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
        });
    }

    public void initialize() {
        final ForgePreferences prefs = FModel.getPreferences();
        // Checkbox event handling
        view.getCbSingletons().addActionListener(arg0 -> {
            prefs.setPref(FPref.DECKGEN_SINGLETONS, String.valueOf(view.getCbSingletons().isSelected()));
            prefs.save();
        });

        view.getCbArtifacts().addActionListener(arg0 -> {
            prefs.setPref(FPref.DECKGEN_ARTIFACTS, String.valueOf(view.getCbArtifacts().isSelected()));
            prefs.save();
        });

        // Pre-select checkboxes
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
    }
}
