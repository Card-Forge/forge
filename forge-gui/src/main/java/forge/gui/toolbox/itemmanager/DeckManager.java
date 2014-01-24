package forge.gui.toolbox.itemmanager;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.Command;
import forge.Singletons;
import forge.deck.DeckBase;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.DeckProxy;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CEditorLimited;
import forge.gui.deckeditor.controllers.CEditorQuest;
import forge.gui.framework.FScreen;
import forge.gui.home.quest.DialogChooseSets;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.itemmanager.filters.DeckColorFilter;
import forge.gui.toolbox.itemmanager.filters.DeckFormatFilter;
import forge.gui.toolbox.itemmanager.filters.DeckQuestWorldFilter;
import forge.gui.toolbox.itemmanager.filters.DeckSearchFilter;
import forge.gui.toolbox.itemmanager.filters.DeckSetFilter;
import forge.gui.toolbox.itemmanager.filters.ItemFilter;
import forge.gui.toolbox.itemmanager.views.ItemColumn.ColumnDef;
import forge.gui.toolbox.itemmanager.views.ItemCellRenderer;
import forge.gui.toolbox.itemmanager.views.ItemColumn;
import forge.gui.toolbox.itemmanager.views.ItemListView;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.item.InventoryItem;
import forge.model.CardCollections;
import forge.quest.QuestWorld;

/** 
 * ItemManager for cards
 *
 */
@SuppressWarnings("serial")
public final class DeckManager extends ItemManager<DeckProxy> {
    private static final FSkin.SkinIcon icoDelete = FSkin.getIcon(FSkin.InterfaceIcons.ICO_DELETE);
    private static final FSkin.SkinIcon icoDeleteOver = FSkin.getIcon(FSkin.InterfaceIcons.ICO_DELETE_OVER);
    private static final FSkin.SkinIcon icoEdit = FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT);
    private static final FSkin.SkinIcon icoEditOver = FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT_OVER);

    private final GameType gametype;
    private boolean stringOnly, preventEdit;
    private Command cmdDelete, cmdSelect;
    private final Map<ColumnDef, ItemColumn> columns = SColumnUtil.getColumns(
            ColumnDef.DECK_ACTIONS,
            ColumnDef.NAME,
            ColumnDef.DECK_COLOR,
            ColumnDef.DECK_FORMAT,
            ColumnDef.DECK_MAIN,
            ColumnDef.DECK_SIDE);

    /**
     * Creates deck list for selected decks for quick deleting, editing, and
     * basic info. "selectable" and "editable" assumed true.
     *
     * @param gt
     */
    public DeckManager(final GameType gt) {
        super(DeckProxy.class, true);
        this.gametype = gt;

        columns.get(ColumnDef.DECK_ACTIONS).setCellRenderer(new DeckActionsRenderer());
        columns.get(ColumnDef.NAME).setSortPriority(1);

        this.addSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (cmdSelect != null) {
                    cmdSelect.run();
                }
            }
        });

        this.setItemActivateCommand(new Command() {
            @Override
            public void run() {
                editDeck(getSelectedItem());
            }
        });
    }

    /**
     * Update table columns
     */
    public void update() {
        update(false, false);
    }
    public void update(boolean stringOnly0) {
        update(stringOnly0, stringOnly0);
    }
    public void update(boolean stringOnly0, boolean preventEdit0) {
        if (this.stringOnly != stringOnly0) {
            this.stringOnly = stringOnly0;
            this.restoreDefaultFilters();
        }
        if (stringOnly0) {
            preventEdit0 = true; //if name only, always prevent edit
        }
        if (this.preventEdit != preventEdit0) {
            this.preventEdit = preventEdit0;
            columns.get(ColumnDef.DECK_ACTIONS).setVisible(!preventEdit0);
        }
        if (stringOnly0) {
            this.getTable().setup(SColumnUtil.getStringColumn());
        }
        else {
            this.getTable().setup(columns);
        }
    }

    /**
     * Sets the delete command.
     *
     * @param c0 &emsp; {@link forge.Command} command executed on delete.
     */
    public void setDeleteCommand(final Command c0) {
        this.cmdDelete = c0;
    }

    /**
     * Sets the select command.
     *
     * @param c0 &emsp; {@link forge.Command} command executed on row select.
     */
    public void setSelectCommand(final Command c0) {
        this.cmdSelect = c0;
    }

    @Override
    protected void addDefaultFilters() {
        if (!this.stringOnly) {
            addFilter(new DeckColorFilter(this));
        }
    }

    @Override
    protected ItemFilter<DeckProxy> createSearchFilter() {
        return new DeckSearchFilter(this);
    }

    @Override
    protected void buildAddFilterMenu(JMenu menu) {
        if (this.stringOnly) { return; }

        GuiUtils.addSeparator(menu); //separate from current search item

        JMenu fmt = GuiUtils.createMenu("Format");
        for (final GameFormat f : Singletons.getModel().getFormats()) {
            GuiUtils.addMenuItem(fmt, f.getName(), null, new Runnable() {
                @Override
                public void run() {
                    addFilter(new DeckFormatFilter(DeckManager.this, f));
                }
            }, DeckFormatFilter.canAddFormat(f, getFilter(DeckFormatFilter.class)));
        }
        menu.add(fmt);

        GuiUtils.addMenuItem(menu, "Sets...", null, new Runnable() {
            @Override
            public void run() {
                DeckSetFilter existingFilter = getFilter(DeckSetFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit();
                }
                else {
                    final DialogChooseSets dialog = new DialogChooseSets(null, null, true);
                    dialog.setOkCallback(new Runnable() {
                        @Override
                        public void run() {
                            List<String> sets = dialog.getSelectedSets();
                            if (!sets.isEmpty()) {
                                addFilter(new DeckSetFilter(DeckManager.this, sets, dialog.getWantReprints()));
                            }
                        }
                    });
                }
            }
        });

        JMenu world = GuiUtils.createMenu("Quest world");
        for (final QuestWorld w : Singletons.getModel().getWorlds()) {
            GuiUtils.addMenuItem(world, w.getName(), null, new Runnable() {
                @Override
                public void run() {
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

    private <T extends DeckBase> void editDeck(final DeckProxy deck) {
        if (deck == null || this.preventEdit) { return; }

        ACEditorBase<? extends InventoryItem, ? extends DeckBase> editorCtrl = null;
        FScreen screen = null;

        switch (this.gametype) {
            case Quest:
                screen = FScreen.DECK_EDITOR_QUEST;
                editorCtrl = new CEditorQuest(Singletons.getModel().getQuest());
                break;
            case Constructed:
                screen = FScreen.DECK_EDITOR_CONSTRUCTED;
                //re-use constructed controller
                break;
            case Sealed:
                screen = FScreen.DECK_EDITOR_SEALED;
                editorCtrl = new CEditorLimited(Singletons.getModel().getDecks().getSealed(), screen);
                break;
            case Draft:
                screen = FScreen.DECK_EDITOR_DRAFT;
                editorCtrl = new CEditorLimited(Singletons.getModel().getDecks().getDraft(), screen);
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

    public boolean deleteDeck(DeckProxy deck) {
        if (deck == null || this.preventEdit) { return false; }

        if (!FOptionPane.showConfirmDialog(
                "Are you sure you want to delete '" + deck.getName() + "'?",
                "Delete Deck", "Delete", "Cancel", false)) {
            return false;
        }

        final CardCollections deckManager = Singletons.getModel().getDecks();

        switch(this.gametype) {
            case Constructed:
                deckManager.getConstructed().delete(deck.getName()); break;
            case Draft:
                deckManager.getDraft().delete(deck.getName()); break;
            case Sealed:
                deckManager.getSealed().delete(deck.getName()); break;
            case Quest:
                Singletons.getModel().getQuest().getMyDecks().delete(deck.getName());
                Singletons.getModel().getQuest().save();
                break;
            default:
                throw new UnsupportedOperationException("Delete not implemneted for game type = " + gametype.toString());
        }


        this.removeItem(deck, 1);

        if (this.cmdDelete != null) {
            this.cmdDelete.run();
        }
        return true;
    }

    public class DeckActionsRenderer extends ItemCellRenderer {
        private int overActionIndex = -1;
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
            Rectangle cellBounds = listView.getTable().getCellRect(row, column, false);
            int x = e.getX() - cellBounds.x;

            if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == 1) {
                DeckProxy deck = (DeckProxy) value;

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

            FSkin.drawImage(g, overActionIndex == 0 ? icoDeleteOver : icoDelete, 0, 0, imgSize, imgSize);
            FSkin.drawImage(g, overActionIndex == 1 ? icoEditOver : icoEdit, imgSize - 1, -1, imgSize, imgSize);
        }
    }
}
