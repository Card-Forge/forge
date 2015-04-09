package forge.screens.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.SwingUtilities;

import forge.deck.Deck;
import forge.deck.DeckType;
import forge.deckchooser.DecksComboBoxEvent;
import forge.deckchooser.FDeckChooser;
import forge.deckchooser.IDecksComboBoxListener;
import forge.model.CardCollections;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FList;

public class CLobby {

    private final VLobby view;
    public CLobby(final VLobby view) {
        this.view = view;
    }

    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public final void run() {
                final CardCollections cColl = FModel.getDecks();
                FList<Object> deckList;
                Vector<Object> listData;
                Object val;

                for (int i = 0; i < VLobby.MAX_PLAYERS; i++) {
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
                view.getBtnStart().requestFocusInWindow();
            }
        });
    }

    public void initialize() {
        for (int iSlot = 0; iSlot < VLobby.MAX_PLAYERS; iSlot++) {
            final FDeckChooser fdc = view.getDeckChooser(iSlot);
            fdc.initialize(FPref.CONSTRUCTED_DECK_STATES[iSlot], defaultDeckTypeForSlot(iSlot));
            fdc.populate();
            fdc.getDecksComboBox().addListener(new IDecksComboBoxListener() {
                @Override public final void deckTypeSelected(final DecksComboBoxEvent ev) {
                    view.focusOnAvatar();
                }
            });
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
}
