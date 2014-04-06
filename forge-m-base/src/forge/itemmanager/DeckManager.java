package forge.itemmanager;

import forge.deck.DeckProxy;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.itemmanager.filters.DeckColorFilter;
import forge.itemmanager.filters.DeckFolderFilter;
import forge.itemmanager.filters.DeckFormatFilter;
import forge.itemmanager.filters.DeckSearchFilter;
import forge.itemmanager.filters.DeckSetFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.menu.FSubMenu;
import forge.model.FModel;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.toolbox.FOptionPane;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/** 
 * ItemManager for decks
 *
 */
public final class DeckManager extends ItemManager<DeckProxy> {
    private final GameType gametype;
    private FEventHandler cmdDelete, cmdSelect;

    /**
     * Creates deck list for selected decks for quick deleting, editing, and
     * basic info. "selectable" and "editable" assumed true.
     *
     * @param gt
     */
    public DeckManager(final GameType gt) {
        super(DeckProxy.class, true);
        this.gametype = gt;

        this.setItemActivateCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editDeck(getSelectedItem());
            }
        });
    }

    @Override
    public void setup(ItemManagerConfig config0) {
        boolean wasStringOnly = (this.getConfig() == ItemManagerConfig.STRING_ONLY);
        boolean isStringOnly = (config0 == ItemManagerConfig.STRING_ONLY);

        super.setup(config0, null);

        if (isStringOnly != wasStringOnly) {
            this.restoreDefaultFilters();
        }
    }

    public void setDeleteCommand(final FEventHandler c0) {
        this.cmdDelete = c0;
    }

    public void setSelectCommand(final FEventHandler c0) {
        this.cmdSelect = c0;
    }

    @Override
    protected void addDefaultFilters() {
        if (this.getConfig() == ItemManagerConfig.STRING_ONLY) { return; }

        addFilter(new DeckColorFilter(this));
    }

    @Override
    protected ItemFilter<DeckProxy> createSearchFilter() {
        return new DeckSearchFilter(this);
    }

    @Override
    protected void buildAddFilterMenu(FPopupMenu menu) {
        final Set<String> folders = new HashSet<String>();
        for (final Entry<DeckProxy, Integer> deckEntry : getPool()) {
            String path = deckEntry.getKey().getPath();
            if (StringUtils.isNotEmpty(path)) { //don't include root folder as option
                folders.add(path);
            }
        }
        menu.addItem(new FSubMenu("Folder", new FPopupMenu() {
            @Override
            protected void buildMenu() {
                for (final String f : folders) {
                    addItem(new FMenuItem(f, new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            addFilter(new DeckFolderFilter(DeckManager.this, f));
                        }
                    }));
                }
            }
        }, folders.size() > 0));

        menu.addItem(new FSubMenu("Format", new FPopupMenu() {
            @Override
            protected void buildMenu() {
                for (final GameFormat f : FModel.getFormats()) {
                    addItem(new FMenuItem(f.getName(), new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            addFilter(new DeckFormatFilter(DeckManager.this, f));
                        }
                    }, DeckFormatFilter.canAddFormat(f, getFilter(DeckFormatFilter.class))));
                }
            }
        }));

        menu.addItem(new FMenuItem("Sets...", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckSetFilter existingFilter = getFilter(DeckSetFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit();
                }
                else {
                    /*final DialogChooseSets dialog = new DialogChooseSets(null, null, true);
                    dialog.setOkCallback(new Runnable() {
                        @Override
                        public void run() {
                            List<String> sets = dialog.getSelectedSets();
                            if (!sets.isEmpty()) {
                                addFilter(new DeckSetFilter(DeckManager.this, sets, dialog.getWantReprints()));
                            }
                        }
                    });*/
                }
            }
        }));

        /*menu.addItem(new FSubMenu("Quest world", new FPopupMenu() {
            @Override
            protected void buildMenu() {
                for (final QuestWorld w : FModel.getWorlds()) {
                    addItem(new FMenuItem(w.getName(), new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            addFilter(new DeckQuestWorldFilter(DeckManager.this, w));
                        }
                    }, CardFormatFilter.canAddQuestWorld(w, getFilter(DeckQuestWorldFilter.class))));
                }
            }
        }));*/

        menu.addItem(new FMenuItem("Colors", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                addFilter(new DeckColorFilter(DeckManager.this));
            }
        }, getFilter(DeckColorFilter.class) == null));
    }

    private void editDeck(final DeckProxy deck) {
        if (deck == null) { return; }

        /*FScreen screen = null;

        switch (this.gametype) {
            case Quest:
                screen = FScreen.DECK_EDITOR_QUEST;
                editorCtrl = new CEditorQuest(FModel.getQuest());
                break;
            case Constructed:
                screen = FScreen.DECK_EDITOR_CONSTRUCTED;
                DeckPreferences.setCurrentDeck(deck.toString());
                //re-use constructed controller
                break;
            case Sealed:
                screen = FScreen.DECK_EDITOR_SEALED;
                editorCtrl = new CEditorLimited(FModel.getDecks().getSealed(), screen);
                break;
            case Draft:
                screen = FScreen.DECK_EDITOR_DRAFT;
                editorCtrl = new CEditorLimited(FModel.getDecks().getDraft(), screen);
                break;

            default:
                return;
        }

        if (!Singletons.getControl().ensureScreenActive(screen)) { return; }

        if (editorCtrl != null) {
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editorCtrl);
        }

        if (!SEditorIO.confirmSaveChanges(screen, true)) { return; } //ensure previous deck on screen is saved if needed

        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().load(deck.getPath(), deck.getName());*/
    }

    public boolean deleteDeck(DeckProxy deck) {
        if (deck == null) { return false; }

        if (!FOptionPane.showConfirmDialog(
                "Are you sure you want to delete '" + deck.getName() + "'?",
                "Delete Deck", "Delete", "Cancel", false)) {
            return false;
        }

        // consider using deck proxy's method to delete deck
        switch(this.gametype) {
            case Constructed:
            case Draft:
            case Sealed:
                deck.deleteFromStorage();
                break;
            case Quest:
                deck.deleteFromStorage();
                //FModel.getQuest().save();
                break;
            default:
                throw new UnsupportedOperationException("Delete not implemneted for game type = " + gametype.toString());
        }

        this.removeItem(deck, 1);

        if (this.cmdDelete != null) {
            this.cmdDelete.handleEvent(new FEvent(this, FEventType.DELETE));
        }
        return true;
    }
}
