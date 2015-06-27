package forge.itemmanager;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.DeckBase;
import forge.deck.DeckProxy;
import forge.deck.io.DeckPreferences;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.game.IHasGameType;
import forge.gui.GuiUtils;
import forge.gui.framework.FScreen;
import forge.item.InventoryItem;
import forge.itemmanager.filters.DeckColorFilter;
import forge.itemmanager.filters.DeckFolderFilter;
import forge.itemmanager.filters.DeckFormatFilter;
import forge.itemmanager.filters.DeckQuestWorldFilter;
import forge.itemmanager.filters.DeckSearchFilter;
import forge.itemmanager.filters.DeckSetFilter;
import forge.itemmanager.filters.FormatFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.itemmanager.views.ItemCellRenderer;
import forge.itemmanager.views.ItemListView;
import forge.itemmanager.views.ItemTableColumn;
import forge.model.FModel;
import forge.quest.QuestWorld;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CEditorLimited;
import forge.screens.deckeditor.controllers.CEditorQuest;
import forge.screens.home.quest.DialogChooseSets;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;

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
            colOverrides = new HashMap<ColumnDef, ItemTableColumn>();
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
     * @param c0 &emsp; {@link forge.UiCommand} command executed on delete.
     */
    public void setDeleteCommand(final UiCommand c0) {
        this.cmdDelete = c0;
    }

    /**
     * Sets the select command.
     *
     * @param c0 &emsp; {@link forge.UiCommand} command executed on row select.
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

    @Override
    protected void buildAddFilterMenu(final JMenu menu) {
        GuiUtils.addSeparator(menu); //separate from current search item

        final Set<String> folders = new HashSet<String>();
        for (final Entry<DeckProxy, Integer> deckEntry : getPool()) {
            final String path = deckEntry.getKey().getPath();
            if (StringUtils.isNotEmpty(path)) { //don't include root folder as option
                folders.add(path);
            }
        }
        final JMenu folder = GuiUtils.createMenu("Folder");
        if (folders.size() > 0) {
            for (final String f : folders) {
                GuiUtils.addMenuItem(folder, f, null, new Runnable() {
                    @Override
                    public void run() {
                        addFilter(new DeckFolderFilter(DeckManager.this, f));
                    }
                }, true);
            }
        }
        else {
            folder.setEnabled(false);
        }
        menu.add(folder);

        final JMenu fmt = GuiUtils.createMenu("Format");
        for (final GameFormat f : FModel.getFormats().getOrderedList()) {
            GuiUtils.addMenuItem(fmt, f.getName(), null, new Runnable() {
                @Override
                public void run() {
                    addFilter(new DeckFormatFilter(DeckManager.this, f));
                }
            }, FormatFilter.canAddFormat(f, getFilter(DeckFormatFilter.class)));
        }
        menu.add(fmt);

        GuiUtils.addMenuItem(menu, "Sets...", null, new Runnable() {
            @Override public void run() {
                final DeckSetFilter existingFilter = getFilter(DeckSetFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit();
                } else {
                    final DialogChooseSets dialog = new DialogChooseSets(null, null, true);
                    dialog.setOkCallback(new Runnable() {
                        @Override public void run() {
                            final List<String> sets = dialog.getSelectedSets();
                            if (!sets.isEmpty()) {
                                addFilter(new DeckSetFilter(DeckManager.this, sets, dialog.getWantReprints()));
                            }
                        }
                    });
                }
            }
        });

        final JMenu world = GuiUtils.createMenu("Quest world");
        for (final QuestWorld w : FModel.getWorlds()) {
            GuiUtils.addMenuItem(world, w.getName(), null, new Runnable() {
                @Override public void run() {
                    addFilter(new DeckQuestWorldFilter(DeckManager.this, w));
                }
            }, DeckQuestWorldFilter.canAddQuestWorld(w, getFilter(DeckQuestWorldFilter.class)));
        }
        menu.add(world);

        GuiUtils.addSeparator(menu);

        GuiUtils.addMenuItem(menu, "Colors", null, new Runnable() {
            @Override
            public void run() {
                addFilter(new DeckColorFilter(DeckManager.this));
            }
        }, getFilter(DeckColorFilter.class) == null);
    }

    private void editDeck(final DeckProxy deck) {
        if (deck == null) { return; }

        ACEditorBase<? extends InventoryItem, ? extends DeckBase> editorCtrl = null;
        FScreen screen = null;

        switch (this.gameType) {
        case Quest:
            screen = FScreen.DECK_EDITOR_QUEST;
            editorCtrl = new CEditorQuest(FModel.getQuest(), getCDetailPicture());
            break;
        case Constructed:
            screen = FScreen.DECK_EDITOR_CONSTRUCTED;
            DeckPreferences.setCurrentDeck(deck.toString());
            //re-use constructed controller
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

        if (!Singletons.getControl().ensureScreenActive(screen)) { return; }

        if (editorCtrl != null) {
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editorCtrl);
        }

        if (!SEditorIO.confirmSaveChanges(screen, true)) { return; } //ensure previous deck on screen is saved if needed

        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().load(deck.getPath(), deck.getName());
    }

    public boolean deleteDeck(final DeckProxy deck) {
        if (deck == null) { return false; }

        if (!FOptionPane.showConfirmDialog(
                "Are you sure you want to delete '" + deck.getName() + "'?",
                "Delete Deck", "Delete", "Cancel", false)) {
            return false;
        }

        // consider using deck proxy's method to delete deck
        switch(this.gameType) {
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
