package forge.itemmanager;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.itemmanager.filters.*;
import forge.localinstance.properties.ForgePreferences;
import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckProxy;
import forge.deck.io.DeckPreferences;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.game.IHasGameType;
import forge.gamemodes.quest.QuestWorld;
import forge.gui.GuiUtils;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.item.InventoryItem;
import forge.itemmanager.views.ItemCellRenderer;
import forge.itemmanager.views.ItemListView;
import forge.itemmanager.views.ItemTableColumn;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CEditorConstructed;
import forge.screens.deckeditor.controllers.CEditorLimited;
import forge.screens.deckeditor.controllers.CEditorQuest;
import forge.screens.home.quest.DialogChooseFormats;
import forge.screens.home.quest.DialogChooseSets;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;

/**
 * ItemManager for decks
 *
 */
@SuppressWarnings("serial")
public final class DeckManager extends ItemManager<DeckProxy> implements IHasGameType {
    private static final FSkin.SkinIcon icoDelete = FSkin.getIcon(FSkinProp.ICO_DELETE);
    //private static final FSkin.SkinIcon icoDeleteOver = FSkin.getIcon(FSkinProp.ICO_DELETE_OVER);
    private static final FSkin.SkinIcon icoEdit = FSkin.getIcon(FSkinProp.ICO_EDIT);
    //private static final FSkin.SkinIcon icoEditOver = FSkin.getIcon(FSkinProp.ICO_EDIT_OVER);

    private final GameType gameType;
    private UiCommand cmdDelete, cmdSelect;

    /**
     * Creates deck list for selected decks for quick deleting, editing, and
     * basic info. "selectable" and "editable" assumed true.
     *
     * @param gt
     */
    public DeckManager(final GameType gt, final CDetailPicture cDetailPicture) {
        super(DeckProxy.class, cDetailPicture, true);
        this.gameType = gt;

        this.addSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(final ListSelectionEvent e) {
                if (cmdSelect != null) {
                    cmdSelect.run();
                }
            }
        });

        this.setItemActivateCommand(new UiCommand() {
            @Override
            public void run() {
                editDeck(getSelectedItem());
            }
        });
    }

    @Override
    public GameType getGameType() {
        return gameType;
    }

    @Override
    public void setup(final ItemManagerConfig config0) {
        final boolean wasStringOnly = (this.getConfig() == ItemManagerConfig.STRING_ONLY);
        final boolean isStringOnly = (config0 == ItemManagerConfig.STRING_ONLY);

        Map<ColumnDef, ItemTableColumn> colOverrides = null;
        if (config0.getCols().containsKey(ColumnDef.DECK_ACTIONS)) {
            final ItemTableColumn column = new ItemTableColumn(new ItemColumn(config0.getCols().get(ColumnDef.DECK_ACTIONS)));
            column.setCellRenderer(new DeckActionsRenderer());
            colOverrides = new HashMap<>();
            colOverrides.put(ColumnDef.DECK_ACTIONS, column);
        }
        super.setup(config0, colOverrides);

        if (isStringOnly != wasStringOnly) {
            this.restoreDefaultFilters();
        }
    }

    /**
     * Sets the delete command.
     *
     * @param c0 &emsp; {@link forge.gui.UiCommand} command executed on delete.
     */
    public void setDeleteCommand(final UiCommand c0) {
        this.cmdDelete = c0;
    }

    /**
     * Sets the select command.
     *
     * @param c0 &emsp; {@link forge.gui.UiCommand} command executed on row select.
     */
    public void setSelectCommand(final UiCommand c0) {
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

    private Map<String, HashMap> buildHierarchy(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Map hierarchy = new HashMap();
        String[] components = path.split("/", 2);
        Map value = new HashMap();
        if (components.length > 1) {
            value = buildHierarchy(components[1]);
        }
        hierarchy.put("/" + components[0], value);
        return hierarchy;
    }

    // borrowed from: https://stackoverflow.com/a/46052477
    private void merge(Map<String, HashMap> mapLeft, Map<String, HashMap> mapRight) {
        // go over all the keys of the right map
        for (String key : mapRight.keySet()) {
            // if the left map already has this key, merge the maps that are behind that key
            if (mapLeft.containsKey(key)) {
                merge(mapLeft.get(key), mapRight.get(key));
            } else {
                // otherwise just add the map under that key
                mapLeft.put(key, mapRight.get(key));
            }
        }
    }

    private void buildNestedMenu(Map tree, JMenu menu, String parentPath) {
        if (tree.size() > 0) {
            for (final Object key : tree.keySet()) {
                String fullPath = key.toString();
                if (parentPath != null) {
                    fullPath = parentPath + key.toString();
                }
                final String finalFullPath = fullPath;
                GuiUtils.addMenuItem(menu, key.toString(), null, new Runnable() {
                    @Override
                    public void run() {
                        addFilter(new DeckFolderFilter(DeckManager.this, finalFullPath));
                    }
                }, true);
                Map value = (Map) tree.get(key);
                if (value.size() > 0) {
                    final JMenu submenu = GuiUtils.createMenu(key.toString());
                    buildNestedMenu(value, submenu, finalFullPath);
                    menu.add(submenu);
                }
            }
        }
    }

    @Override
    protected void buildAddFilterMenu(final JMenu menu) {
        GuiUtils.addSeparator(menu); //separate from current search item

        Map hierarchy = new HashMap();
        for (final Entry<DeckProxy, Integer> deckEntry : getPool()) {
            final String path = deckEntry.getKey().getPath();
            if (StringUtils.isNotEmpty(path)) { //don't include root folder as option
                merge(hierarchy, buildHierarchy(path));
            }
        }

        final Localizer localizer = Localizer.getInstance();
        final JMenu folder = GuiUtils.createMenu(localizer.getMessage("lblFolder"));
        if (hierarchy.size() > 0) {
            buildNestedMenu(hierarchy, folder, null);
        }
        else {
            folder.setEnabled(false);
        }
        menu.add(folder);

        final JMenu fmt = GuiUtils.createMenu(localizer.getMessage("lblFormat"));

        for (final GameFormat f : FModel.getFormats().getFilterList()) {
            GuiUtils.addMenuItem(fmt, f.getName(), null, new Runnable() {
                @Override
                public void run() {
                    addFilter(new DeckFormatFilter(DeckManager.this, f));
                }
            }, FormatFilter.canAddFormat(f, getFilter(DeckFormatFilter.class)));
        }
        menu.add(fmt);

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblFormats") + "...", null, new Runnable() {
            @Override public void run() {
                final DeckFormatFilter existingFilter = getFilter(DeckFormatFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit();
                } else {
                    final DialogChooseFormats dialog = new DialogChooseFormats();
                    dialog.setOkCallback(new Runnable() {
                        @Override public void run() {
                            final List<GameFormat> formats = dialog.getSelectedFormats();
                            if (!formats.isEmpty()) {
                                for(GameFormat format: formats) {
                                    addFilter(new DeckFormatFilter(DeckManager.this, format));
                                }
                            }
                        }
                    });
                }
            }
        });


        GuiUtils.addMenuItem(menu, localizer.getMessage("lblSets") + "...", null, new Runnable() {
            @Override public void run() {
                final DeckSetFilter existingFilter = getFilter(DeckSetFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit();
                } else {
                    List<String> limitedSets = getFilteredSetCodesInCatalog();
                    final DialogChooseSets dialog = new DialogChooseSets(null, null, limitedSets, true);
                    dialog.setOkCallback(new Runnable() {
                        @Override public void run() {
                            final List<String> sets = dialog.getSelectedSets();
                            if (!sets.isEmpty()) {
                                addFilter(new DeckSetFilter(DeckManager.this, sets, limitedSets, dialog.getWantReprints()));
                            }
                        }
                    });
                }
            }
        });

        final JMenu world = GuiUtils.createMenu(localizer.getMessage("lblQuestWorld"));
        for (final QuestWorld w : FModel.getWorlds()) {
            GuiUtils.addMenuItem(world, w.getName(), null, new Runnable() {
                @Override public void run() {
                    addFilter(new DeckQuestWorldFilter(DeckManager.this, w));
                }
            }, DeckQuestWorldFilter.canAddQuestWorld(w, getFilter(DeckQuestWorldFilter.class)));
        }
        menu.add(world);

        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.LOAD_HISTORIC_FORMATS)) {
            JMenu blocks = GuiUtils.createMenu(localizer.getMessage("lblBlock"));
            final Iterable<GameFormat> blockFormats = FModel.getFormats().getBlockList();
            for (final GameFormat f : blockFormats) {
                GuiUtils.addMenuItem(blocks, f.getName(), null, new Runnable() {
                    @Override
                    public void run() {
                        addFilter(new DeckBlockFilter(DeckManager.this, f));
                    }
                }, DeckBlockFilter.canAddCardBlock(f, getFilter(DeckBlockFilter.class)));
            }
            menu.add(blocks);
        }

        GuiUtils.addSeparator(menu);

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblColors"), null, new Runnable() {
            @Override
            public void run() {
                addFilter(new DeckColorFilter(DeckManager.this));
            }
        }, getFilter(DeckColorFilter.class) == null);

        GuiUtils.addSeparator(menu);

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblAdvanced") + "...", null, new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                AdvancedSearchFilter<DeckProxy> filter = getFilter(AdvancedSearchFilter.class);
                if (filter != null) {
                    filter.edit();
                }
                else {
                    filter = new AdvancedSearchFilter<>(DeckManager.this);
                    lockFiltering = true; //ensure filter not applied until added
                    boolean result = filter.edit();
                    lockFiltering = false;
                    if (result) {
                        addFilter(filter);
                    }
                }
            }
        });
    }

    @Override
    protected List<String> getFilteredSetCodesInCatalog(){
        GameType gameType = getGameType();
        if (gameType == GameType.Brawl) {
            filteredSetCodesInCatalog = FModel.getFormats().get("Brawl").getAllowedSetCodes();
            return filteredSetCodesInCatalog;
        }
        return super.getFilteredSetCodesInCatalog();
    }

    public void editDeck(final DeckProxy deck) {
        ACEditorBase<? extends InventoryItem, ? extends DeckBase> editorCtrl = null;
        FScreen screen = null;

        switch (this.gameType) {
            case Quest:
                screen = FScreen.DECK_EDITOR_QUEST;
                editorCtrl = new CEditorQuest(FModel.getQuest(), getCDetailPicture());
                break;
            case Constructed:
                screen = FScreen.DECK_EDITOR_CONSTRUCTED;
                DeckPreferences.setCurrentDeck((deck != null) ? deck.toString() : "");
                editorCtrl = new CEditorConstructed(getCDetailPicture(), this.gameType);
                break;
            case Commander:
                screen = FScreen.DECK_EDITOR_CONSTRUCTED;  // re-use "Deck Editor", rather than creating a new top level tab
                DeckPreferences.setCommanderDeck((deck != null) ? deck.toString() : "");
                editorCtrl = new CEditorConstructed(getCDetailPicture(), this.gameType);
                break;
            case Oathbreaker:
                screen = FScreen.DECK_EDITOR_CONSTRUCTED;  // re-use "Deck Editor", rather than creating a new top level tab
                DeckPreferences.setCommanderDeck((deck != null) ? deck.toString() : "");
                editorCtrl = new CEditorConstructed(getCDetailPicture(), this.gameType);
                break;
            case Brawl:
                screen = FScreen.DECK_EDITOR_CONSTRUCTED;  // re-use "Deck Editor", rather than creating a new top level tab
                DeckPreferences.setBrawlDeck((deck != null) ? deck.toString() : "");
                editorCtrl = new CEditorConstructed(getCDetailPicture(), this.gameType);
                break;
            case TinyLeaders:
                screen = FScreen.DECK_EDITOR_CONSTRUCTED;  // re-use "Deck Editor", rather than creating a new top level tab
                DeckPreferences.setTinyLeadersDeck((deck != null) ? deck.toString() : "");
                editorCtrl = new CEditorConstructed(getCDetailPicture(), this.gameType);
                break;
            case Sealed:
                screen = FScreen.DECK_EDITOR_SEALED;
                editorCtrl = new CEditorLimited(FModel.getDecks().getSealed(), screen, getCDetailPicture());
                break;
            case Draft:
                screen = FScreen.DECK_EDITOR_DRAFT;
                editorCtrl = new CEditorLimited(FModel.getDecks().getDraft(), screen, getCDetailPicture());
                break;
            case Winston:
                screen = FScreen.DECK_EDITOR_DRAFT;
                editorCtrl = new CEditorLimited(FModel.getDecks().getWinston(), screen, getCDetailPicture());
                break;

            default:
                return;
        }

        if (!Singletons.getControl().ensureScreenActive(screen)) {
            return;
        }

        if (editorCtrl != null) {
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editorCtrl);
        }

        if (!SEditorIO.confirmSaveChanges(screen, true)) {
            return;
        } //ensure previous deck on screen is saved if needed

        if (deck != null) {
            CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().load(deck.getPath(), deck.getName());
        } else {
            CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().loadDeck(new Deck());
        }
    }

    public boolean deleteDeck(final DeckProxy deck) {
        if (deck == null) { return false; }

        if (!FOptionPane.showConfirmDialog(Localizer.getInstance().getMessage("lblConfirmDelete") + "'" + deck.getName() + "'?",
                Localizer.getInstance().getMessage("lblDeleteDeck"), Localizer.getInstance().getMessage("lblDelete"),
                Localizer.getInstance().getMessage("lblCancel"), false)) {
            return false;
        }

        // consider using deck proxy's method to delete deck
        switch(this.gameType) {
            case Brawl:
            case Commander:
            case Oathbreaker:
            case TinyLeaders:
            case Constructed:
            case Draft:
            case Sealed:
                deck.deleteFromStorage();
                break;
            case Quest:
                deck.deleteFromStorage();
                FModel.getQuest().save();
                break;
            default:
                throw new UnsupportedOperationException("Delete not implemented for game type = " + gameType.toString());
        }

        this.removeItem(deck, 1);

        if (this.cmdDelete != null) {
            this.cmdDelete.run();
        }
        return true;
    }

    public class DeckActionsRenderer extends ItemCellRenderer {
        //private final int overActionIndex = -1;
        private static final int imgSize = 20;

        @Override
        public boolean alwaysShowTooltip() {
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent
         * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
         */
        @Override
        public final Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            setToolTipText(null);
            return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        }

        @Override
        public <T extends InventoryItem> void processMouseEvent(final MouseEvent e, final ItemListView<T> listView, final Object value, final int row, final int column) {
            final Rectangle cellBounds = listView.getTable().getCellRect(row, column, false);
            final int x = e.getX() - cellBounds.x;

            if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == 1) {
                final DeckProxy deck = (DeckProxy) value;

                if (x >= 0 && x < imgSize) { //delete button
                    if (DeckManager.this.deleteDeck(deck)) {
                        e.consume();
                        return;
                    }
                }
                else if (x >= imgSize && x < imgSize * 2) { //edit button
                    DeckManager.this.editDeck(deck);
                }

                listView.getTable().setRowSelectionInterval(row, row);
                listView.getTable().repaint();
                e.consume();
            }
        }

        /*private void setOverActionIndex(final ItemListView<?> listView, int overActionIndex0) {
            if (this.overActionIndex == overActionIndex0) { return; }
            this.overActionIndex = overActionIndex0;
            switch (this.overActionIndex) {
            case -1:
                this.setToolTipText(null);
                break;
            case 0:
                this.setToolTipText("Delete this deck");
                break;
            case 1:
                this.setToolTipText("Edit this deck");
                break;
            }
            listView.getTable().repaint();
        }*/

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.JComponent#paint(java.awt.Graphics)
         */
        @Override
        public final void paint(final Graphics g) {
            super.paint(g);

            FSkin.drawImage(g, /*overActionIndex == 0 ? icoDeleteOver : */icoDelete, 0, 0, imgSize, imgSize);
            FSkin.drawImage(g, /*overActionIndex == 0 ? icoDeleteOver : */icoEdit, imgSize - 1, -1, imgSize, imgSize);
        }
    }
}
