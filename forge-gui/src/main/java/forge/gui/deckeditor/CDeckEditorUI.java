/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.deckeditor;

import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.Command;
import forge.Singletons;
import forge.deck.DeckBase;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CEditorConstructed;
import forge.gui.deckeditor.controllers.CProbabilities;
import forge.gui.deckeditor.controllers.CStatistics;
import forge.gui.deckeditor.controllers.DeckController;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CPicture;
import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerIO;
import forge.gui.toolbox.itemmanager.SItemManagerIO.EditorPreference;
import forge.gui.toolbox.itemmanager.views.ItemListView;
import forge.item.InventoryItem;
import forge.util.ItemPool;

/**
 * Constructs instance of deck editor UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CDeckEditorUI implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final HashMap<FScreen, ACEditorBase<? extends InventoryItem, ? extends DeckBase>> screenChildControllers;
	private ACEditorBase<? extends InventoryItem, ? extends DeckBase> childController;

    private CDeckEditorUI() {
        screenChildControllers = new HashMap<FScreen, ACEditorBase<? extends InventoryItem, ? extends DeckBase>>();
    }

    /**
     * Set Pack, for when Packs can be shown in the CardPicturePanel.
     * @param item
     */
    public void setCard(final InventoryItem item) {
        CDetail.SINGLETON_INSTANCE.showCard(item);
        CPicture.SINGLETON_INSTANCE.showImage(item);
    }

    public boolean hasChanges() {
        if (this.childController == null) { return false; }

        final DeckController<?> deckController = this.childController.getDeckController();
        if (deckController == null) { return false; }

        return !deckController.isSaved();
    }

    public boolean canSwitchAway(boolean isClosing) {
        if (this.childController != null) {
            if (!this.childController.canSwitchAway(isClosing)) {
                return false;
            }
            this.childController.resetUIChanges();
            if (isClosing) {
                screenChildControllers.remove(this.childController.getScreen());
            }
        }
        return true;
    }

    //========= Accessor/mutator methods
    /**
     * @return ACEditorBase<?, ?>
     */
    public ACEditorBase<? extends InventoryItem, ? extends DeckBase> getCurrentEditorController() {
        return childController;
    }

    /**
     * Set controller for a given editor screen.
     */
    public void setEditorController(ACEditorBase<? extends InventoryItem, ? extends DeckBase> childController0) {
        FScreen screen = childController0.getScreen();
        screenChildControllers.put(screen, childController0);
        if (screen == Singletons.getControl().getCurrentScreen()) {
            setCurrentEditorController(childController0);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends InventoryItem> void incrementDeckQuantity(T item, int delta) {
        if (item == null || delta == 0) { return; }

        if (delta > 0) { //add items
            int qty = Math.min(delta, ((ItemManager<T>)childController.getCatalogManager()).getItemCount(item));
            if (qty == 0) { return; }
            ((ACEditorBase<T, ?>)childController).addItem(item, qty, false);
        }
        else { //remove items
            int qty = Math.min(-delta, ((ItemManager<T>)childController.getDeckManager()).getItemCount(item));
            if (qty == 0) { return; }
            ((ACEditorBase<T, ?>)childController).removeItem(item, qty, false);
        }

        CStatistics.SINGLETON_INSTANCE.update();
        CProbabilities.SINGLETON_INSTANCE.update();
    }

    private interface _MoveAction {
        public <T extends InventoryItem> void move(Iterable<Entry<T, Integer>> items);
    }

    private <T extends InventoryItem> void moveSelectedItems(ItemManager<T> itemManager, _MoveAction moveAction, int maxQty) {
        if (maxQty == 0) { return; }

        ItemPool<T> items = new ItemPool<T>(itemManager.getGenericType());
        for (T item : itemManager.getSelectedItems()) {
            int qty = Math.min(maxQty, itemManager.getItemCount(item));
            if (qty > 0) {
                items.add(item, qty);
            }
        }

        if (items.isEmpty()) { return; }

        moveAction.move(items);
        CStatistics.SINGLETON_INSTANCE.update();
        CProbabilities.SINGLETON_INSTANCE.update();
    }

    @SuppressWarnings("unchecked")
    public void addSelectedCards(final boolean toAlternate, int number) {
        moveSelectedItems(childController.getCatalogManager(), new _MoveAction() {
            @Override
            public <T extends InventoryItem> void move(Iterable<Entry<T, Integer>> items) {
                ((ACEditorBase<T, ?>)childController).addItems(items, toAlternate);
            }
        }, number);
    }

    @SuppressWarnings("unchecked")
    public void removeSelectedCards(final boolean toAlternate, int number) {
        moveSelectedItems(childController.getDeckManager(), new _MoveAction() {
            @Override
            public <T extends InventoryItem> void move(Iterable<Entry<T, Integer>> items) {
                ((ACEditorBase<T, ?>)childController).removeItems(items, toAlternate);
            }
        }, number);
    }

    @SuppressWarnings("unchecked")
    public void removeAllCards(final boolean toAlternate) {
        ItemManager<?> v = childController.getDeckManager();
        v.getTable().selectAll();
        moveSelectedItems(v, new _MoveAction() {
            @Override
            public <T extends InventoryItem> void move(Iterable<Entry<T, Integer>> items) {
                ((ACEditorBase<T, ?>)childController).removeItems(items, toAlternate);
            }
        }, Integer.MAX_VALUE);
    }

    //========== Other methods
    private interface _MoveCard {
        void moveCard(boolean toAlternate, int qty);
    }

    private class _ContextMenuBuilder implements ACEditorBase.ContextMenuBuilder {
        private final MouseEvent  _e;
        private final ItemManager<?> _itemManager;
        private final ItemManager<?> _nextItemManager;
        private final _MoveCard   _onMove;
        private final JPopupMenu  _menu = new JPopupMenu("ItemViewContextMenu");

        public _ContextMenuBuilder(MouseEvent e, ItemManager<?> itemManager, ItemManager<?> nextItemManager, _MoveCard onMove) {
            _e         = e;
            _itemManager = itemManager;
            _nextItemManager = nextItemManager;
            _onMove    = onMove;

            //ensure the item manager has focus
            itemManager.focus();

            //if item under the cursor is not selected, select it
            int index = itemManager.getTable().getIndexAtPoint(e.getPoint());
            boolean needSelection = true;
            for (Integer selectedIndex : itemManager.getSelectedIndices()) {
                if (selectedIndex == index) {
                    needSelection = false;
                    break;
                }
            }
            if (needSelection) {
                itemManager.setSelectedIndex(index);
            }
        }

        private void show() {
            _menu.addSeparator();

            GuiUtils.addMenuItem(_menu, "Jump to previous table",
                    KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), new Runnable() {
                @Override public void run() { _nextItemManager.focus(); }
            });
            GuiUtils.addMenuItem(_menu, "Jump to next table",
                    KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), new Runnable() {
                @Override public void run() { _nextItemManager.focus(); }
            });
            GuiUtils.addMenuItem(_menu, "Jump to text filter",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    new Runnable() {
                @Override public void run() {
                    _itemManager.focusSearch();
                }
            });

            _menu.show(_e.getComponent(), _e.getX(), _e.getY());
        }

        private String _doNoun(String nounSingular, String nounPlural) {
            int numSelected = _itemManager.getSelectionCount();
            if (1 == numSelected) {
                return nounSingular;
            }
            return String.format("%d %s", numSelected, nounPlural);
        }

        private String _doDest(String destination) {
            if (null == destination) {
                return "";
            }
            return " " + destination;
        }

        @Override
        public void addMoveItems(String verb, String nounSingular, String nounPlural, String destination) {
            String noun = _doNoun(nounSingular, nounPlural);
            String dest = _doDest(destination);

            GuiUtils.addMenuItem(_menu,
                    String.format("%s %s%s", verb, noun, dest),
                    KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), new Runnable() {
                        @Override public void run() { _onMove.moveCard(false, 1); }
                    }, true, true);
            GuiUtils.addMenuItem(_menu,
                    String.format("%s 4 copies of %s%s", verb, noun, dest),
                    KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.SHIFT_DOWN_MASK), new Runnable() {
                        @Override public void run() { _onMove.moveCard(false, 4); }
                    });
        }

        @Override
        public void addMoveAlternateItems(String verb, String nounSingular, String nounPlural, String destination) {
            String noun = _doNoun(nounSingular, nounPlural);
            String dest = _doDest(destination);

            // yes, CTRL_DOWN_MASK and not getMenuShortcutKeyMask().  On OSX, cmd-space is hard-coded to bring up Spotlight
            GuiUtils.addMenuItem(_menu,
                    String.format("%s %s%s", verb, noun, dest),
                    KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK), new Runnable() {
                        @Override public void run() { _onMove.moveCard(true, 1); }
                    });

            // getMenuShortcutKeyMask() instead of CTRL_DOWN_MASK since on OSX, ctrl-shift-space brings up the window manager
            GuiUtils.addMenuItem(_menu,
                    String.format("%s 4 copies of %s%s", verb, noun, dest),
                    KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    new Runnable() {
                        @Override public void run() { _onMove.moveCard(true, 4); }
                    });
        }
    }

    /**
     * Set current editor controller
     */
    private void setCurrentEditorController(ACEditorBase<? extends InventoryItem, ? extends DeckBase> childController0) {
        this.childController = childController0;
        Singletons.getControl().getForgeMenu().setProvider(childController0);

        if (childController == null) { return; }

        final ItemManager<? extends InventoryItem> catView  = childController.getCatalogManager();
        final ItemManager<? extends InventoryItem> deckView = childController.getDeckManager();
        final ItemListView<? extends InventoryItem> catTable  = catView.getTable();
        final ItemListView<? extends InventoryItem> deckTable = deckView.getTable();

        VCardCatalog.SINGLETON_INSTANCE.setItemManager(catView);
        VCurrentDeck.SINGLETON_INSTANCE.setItemManager(deckView);

        if (!childController.listenersHooked) { //hook listeners the first time the controller is updated
            catTable.getComponent().addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (!catView.isIncrementalSearchActive() && KeyEvent.VK_SPACE == e.getKeyCode()) {
                        addSelectedCards(e.isControlDown() || e.isMetaDown(), e.isShiftDown() ? 4: 1);
                    }
                    else if (KeyEvent.VK_LEFT == e.getKeyCode() || KeyEvent.VK_RIGHT == e.getKeyCode()) {
                        deckTable.focus();
                        e.consume(); //prevent losing selection
                    }
                    else if (KeyEvent.VK_F == e.getKeyCode()) {
                        // let ctrl/cmd-F set focus to the text filter box
                        if (e.isControlDown() || e.isMetaDown()) {
                            catView.focusSearch();
                        }
                    }
                }
            });

            deckTable.getComponent().addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (!catView.isIncrementalSearchActive() && KeyEvent.VK_SPACE == e.getKeyCode()) {
                        removeSelectedCards(e.isControlDown() || e.isMetaDown(), e.isShiftDown() ? 4: 1);
                    }
                    else if (KeyEvent.VK_LEFT == e.getKeyCode() || KeyEvent.VK_RIGHT == e.getKeyCode()) {
                        catTable.focus();
                        e.consume(); //prevent losing selection
                    }
                    else if (KeyEvent.VK_F == e.getKeyCode()) {
                        // let ctrl/cmd-F set focus to the text filter box
                        if (e.isControlDown() || e.isMetaDown()) {
                            deckView.focusSearch();
                        }
                    }
                }
            });

            final _MoveCard onAdd = new _MoveCard() {
                @Override
                public void moveCard(boolean toAlternate, int qty) {
                    addSelectedCards(toAlternate, qty);
                }
            };
            final _MoveCard onRemove = new _MoveCard() {
                @Override
                public void moveCard(boolean toAlternate, int qty) {
                    removeSelectedCards(toAlternate, qty);
                }
            };

            catTable.getComponent().addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftDoubleClick(MouseEvent e) {
                    if (e.isConsumed()) { return; } //don't add cards if inline button double clicked
                    addSelectedCards(false, 1);
                }

                @Override
                public void onRightClick(MouseEvent e) {
                    _ContextMenuBuilder cmb = new _ContextMenuBuilder(e, catView, deckView, onAdd);
                    childController.buildAddContextMenu(cmb);
                    cmb.show();
                }
            });

            deckTable.getComponent().addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftDoubleClick(MouseEvent e) {
                    if (e.isConsumed()) { return; } //don't remove cards if inline button double clicked
                    removeSelectedCards(false, 1);
                }

                @Override
                public void onRightClick(MouseEvent e) {
                    _ContextMenuBuilder cmb = new _ContextMenuBuilder(e, deckView, catView, onRemove);
                    childController.buildRemoveContextMenu(cmb);
                    cmb.show();
                }
            });

            //set card when selection changes
            catView.addSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					setCard(catView.getSelectedItem());
				}
			});

            deckView.addSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					setCard(deckView.getSelectedItem());
				}
			});

            catView.setAllowMultipleSelections(true);
            deckView.setAllowMultipleSelections(true);

            childController.listenersHooked = true;
        }

        childController.update();

        boolean wantElastic = SItemManagerIO.getPref(EditorPreference.elastic_columns);
        boolean wantUnique = SItemManagerIO.getPref(EditorPreference.display_unique_only);
        catTable.setWantElasticColumns(wantElastic);
        deckTable.setWantElasticColumns(wantElastic);
        catView.setWantUnique(wantUnique);
        deckView.setWantUnique(wantUnique);
        catView.applyFilters();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                catView.focus();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        //change to previously open child controller based on screen
        FScreen screen = Singletons.getControl().getCurrentScreen();
        ACEditorBase<? extends InventoryItem, ? extends DeckBase> screenChildController = screenChildControllers.get(screen);
        if (screenChildController != null) {
            setCurrentEditorController(screenChildController);
        }
        else if (screen == FScreen.DECK_EDITOR_CONSTRUCTED) {
            setEditorController(new CEditorConstructed()); //ensure Constructed deck editor controller initialized
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() { }
}

