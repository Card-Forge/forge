package forge.itemmanager;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardFaceSymbols;
import forge.card.CardRenderer;
import forge.card.ColorSet;
import forge.deck.DeckProxy;
import forge.deck.io.DeckPreferences;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.itemmanager.filters.DeckColorFilter;
import forge.itemmanager.filters.DeckFolderFilter;
import forge.itemmanager.filters.DeckFormatFilter;
import forge.itemmanager.filters.DeckSearchFilter;
import forge.itemmanager.filters.DeckSetFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.itemmanager.views.ItemListView.ItemRenderer;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.menu.FSubMenu;
import forge.model.FModel;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.Utils;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/** 
 * ItemManager for decks
 *
 */
public final class DeckManager extends ItemManager<DeckProxy> {
    private final GameType gametype;
    private FEventHandler cmdDelete;

    /**
     * Creates deck list for selected decks for quick deleting, editing, and
     * basic info. "selectable" and "editable" assumed true.
     *
     * @param gt
     */
    public DeckManager(final GameType gt) {
        super(DeckProxy.class, true);
        gametype = gt;

        setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editDeck(getSelectedItem());
            }
        });

        setCaption("Decks");
    }

    @Override
    public void setup(ItemManagerConfig config0) {
        boolean wasStringOnly = (getConfig() == ItemManagerConfig.STRING_ONLY);
        boolean isStringOnly = (config0 == ItemManagerConfig.STRING_ONLY);

        super.setup(config0, null);

        if (isStringOnly != wasStringOnly) {
            restoreDefaultFilters();
        }
    }

    public void setDeleteCommand(final FEventHandler c0) {
        cmdDelete = c0;
    }

    @Override
    protected void addDefaultFilters() {
        if (getConfig() == ItemManagerConfig.STRING_ONLY) { return; }

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

        switch (gametype) {
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

    public void deleteDeck(final DeckProxy deck) {
        if (deck == null) { return; }

        FOptionPane.showConfirmDialog(
                "Are you sure you want to delete '" + deck.getName() + "'?",
                "Delete Deck", "Delete", "Cancel", false, new Callback<Boolean>() {
                    @Override
                    public void run(Boolean result) {
                        if (!result) { return; }

                        // consider using deck proxy's method to delete deck
                        switch(gametype) {
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

                        removeItem(deck, 1);

                        if (cmdDelete != null) {
                            cmdDelete.handleEvent(new FEvent(DeckManager.this, FEventType.DELETE));
                        }
                    }
        });
    }

    private static final float IMAGE_SIZE = FSkinImage.MANA_W.getNearestHQHeight(Utils.AVG_FINGER_HEIGHT / 2);

    @Override
    public ItemRenderer<DeckProxy> getListItemRenderer() {
        return new ItemRenderer<DeckProxy>() {
            @Override
            public float getItemHeight() {
                if (DeckManager.this.getConfig().getCols().size() == 1) {
                    //if just string column, use normal list item height
                    return Utils.AVG_FINGER_HEIGHT;
                }
                return IMAGE_SIZE + 2 * FSkinFont.get(12).getLineHeight() + 4 * FList.PADDING;
            }

            @Override
            public boolean tap(Entry<DeckProxy, Integer> value, float x, float y, int count) {
                float bottomRight = IMAGE_SIZE + 2 * FList.PADDING;
                if (x <= bottomRight && y <= bottomRight) {
                    DeckPreferences prefs = DeckPreferences.getPrefs(value.getKey());
                    prefs.setStarCount((prefs.getStarCount() + 1) % 2); //TODO: consider supporting more than 1 star
                    return true;
                }
                return false;
            }

            @Override
            public void drawValue(Graphics g, Entry<DeckProxy, Integer> value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
                DeckProxy deck = value.getKey();

                if (DeckManager.this.getConfig().getCols().size() == 1) {
                    //if just string column, just draw deck string value
                    g.drawText(deck.toString(), font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);
                    return;
                }

                //draw favorite, name, and color on first line
                g.drawImage(DeckPreferences.getPrefs(deck).getStarCount() > 0 ? FSkinImage.STAR_FILLED : FSkinImage.STAR_OUTINE, x, y, IMAGE_SIZE, IMAGE_SIZE);
                x += IMAGE_SIZE + FList.PADDING;
                ColorSet deckColor = deck.getColor();
                float availableNameWidth = w - CardFaceSymbols.getWidth(deckColor, IMAGE_SIZE) - IMAGE_SIZE - 2 * FList.PADDING;
                g.drawText(deck.getName(), font, foreColor, x, y, availableNameWidth, IMAGE_SIZE, false, HAlignment.LEFT, true);
                x += availableNameWidth + FList.PADDING;
                CardFaceSymbols.drawColorSet(g, deckColor, x, y, IMAGE_SIZE);

                //draw path and main/side on second line
                x = FList.PADDING;
                y += IMAGE_SIZE + FList.PADDING;
                font = font.shrink().shrink();
                float lineHeight = font.getLineHeight();

                int mainSize = deck.getMainSize();
                if (mainSize < 0) {
                    mainSize = 0; //show main as 0 if empty
                }
                int sideSize = deck.getSideSize();
                if (sideSize < 0) {
                    sideSize = 0; //show sideboard as 0 if empty
                }
                String countStr = mainSize + " / " + sideSize;
                float countWidth = font.getBounds(countStr).width;
                if (!deck.getPath().isEmpty()) {
                    g.drawText(deck.getPath().substring(1) + "/", font, foreColor, x, y, w - countWidth - FList.PADDING, lineHeight, false, HAlignment.LEFT, true);
                }
                g.drawText(countStr, font, foreColor, x, y, w, lineHeight, false, HAlignment.RIGHT, true);

                //draw formats and set/highest rarity on third line
                x = FList.PADDING;
                y += lineHeight + FList.PADDING;
                String set = deck.getEdition().getCode();
                float setWidth = CardRenderer.getSetWidth(font, set);
                float availableFormatWidth = w - setWidth - FList.PADDING;
                g.drawText(deck.getFormatsString(), font, foreColor, x, y, availableFormatWidth, lineHeight, false, HAlignment.LEFT, true);
                x += availableFormatWidth + FList.PADDING;
                CardRenderer.drawSetLabel(g, font, set, deck.getHighestRarity(), x, y, setWidth + 1, lineHeight + 1); //provide a little more padding for set label
            }
        };
    }
}
