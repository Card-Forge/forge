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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import forge.Command;
import forge.Singletons;
import forge.deck.DeckBase;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CEditorConstructed;
import forge.gui.deckeditor.controllers.CProbabilities;
import forge.gui.deckeditor.controllers.CStatistics;
import forge.gui.deckeditor.controllers.DeckController;
import forge.gui.deckeditor.menus.CDeckEditorUIMenus;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CPicture;
import forge.gui.menus.IMenuProvider;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.FSkin;
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
public enum CDeckEditorUI implements ICDoc, IMenuProvider {
    /** */
    SINGLETON_INSTANCE;

    private final HashMap<FScreen, ACEditorBase<? extends InventoryItem, ? extends DeckBase>> screenChildControllers;
	private ACEditorBase<? extends InventoryItem, ? extends DeckBase> childController;
    private boolean isFindingAsYouType = false;

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
                    if (!isFindingAsYouType && KeyEvent.VK_SPACE == e.getKeyCode()) {
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
                    if (!isFindingAsYouType && KeyEvent.VK_SPACE == e.getKeyCode()) {
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

            final _FindAsYouType catFind  = new _FindAsYouType(catView);
            final _FindAsYouType deckFind = new _FindAsYouType(deckView);
    
            catTable.getComponent().addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent arg0) {
                    catFind.cancel();
                }
            });
            deckTable.getComponent().addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent arg0) {
                    deckFind.cancel();
                }
            });
            
            // highlight items as the user types a portion of their names
            catTable.getComponent().addKeyListener(catFind);
            deckTable.getComponent().addKeyListener(deckFind);
            
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
    }
    
    private class _FindAsYouType extends KeyAdapter {
        private StringBuilder str = new StringBuilder();
        private final FLabel popupLabel = new FLabel.Builder().fontAlign(SwingConstants.LEFT).opaque().build();
        private boolean popupShowing = false;
        private Popup popup;
        private Timer popupTimer;
        private final ItemManager<? extends InventoryItem> tableView;
        static final int okModifiers = KeyEvent.SHIFT_MASK | KeyEvent.ALT_GRAPH_MASK;
        
        public _FindAsYouType(ItemManager<? extends InventoryItem> tableView) {
            this.tableView = tableView;
        }

        private void _setPopupSize() {
            // resize popup to size of label (ensure there's room for the next character so the label
            // doesn't show '...' in the time between when we set the text and when we increase the size
            Dimension labelDimension = popupLabel.getPreferredSize();
            Dimension popupDimension = new Dimension(labelDimension.width + 12, labelDimension.height + 4);
            SwingUtilities.getRoot(popupLabel).setSize(popupDimension);
        }
        
        private void _findNextMatch(int startIdx, boolean reverse) {
            int numItems = tableView.getTable().getCount();
            if (0 == numItems) {
                cancel();
                return;
            }
            
            // find the next item that matches the string
            startIdx %= numItems;
            final int increment = reverse ? numItems - 1 : 1;
            int stopIdx = (startIdx + numItems - increment) % numItems;
            String searchStr = str.toString();
            boolean found = false;
            for (int idx = startIdx;; idx = (idx + increment) % numItems) {
                if (StringUtils.containsIgnoreCase(tableView.getTable().getItemAtIndex(idx).getName(), searchStr)) {
                    tableView.getTable().setSelectedIndex(idx);
                    found = true;
                    break;
                }
                
                if (idx == stopIdx) {
                    break;
                }
            }
            
            if (searchStr.isEmpty()) {
                cancel();
                return;
            }
            
            // show a popup with the current search string, highlighted in red if not found
            popupLabel.setText(searchStr + " (hit Enter for next match, Esc to cancel)");
            if (found) {
                FSkin.get(popupLabel).setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
            else {
                FSkin.get(popupLabel).setForeground(new Color(255, 0, 0));
            }
            
            if (popupShowing) {
                _setPopupSize();
                popupTimer.restart();
            } else {
                PopupFactory factory = PopupFactory.getSharedInstance();
                Point tableLoc = tableView.getTable().getTable().getTableHeader().getLocationOnScreen();
                popup = factory.getPopup(null, popupLabel, tableLoc.x + 10, tableLoc.y + 10);
                FSkin.get(SwingUtilities.getRoot(popupLabel)).setBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE));
                
                popupTimer = new Timer(5000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        cancel();
                    }
                });
                popupTimer.setRepeats(false);
                
                popup.show();
                _setPopupSize();
                popupTimer.start();
                isFindingAsYouType = true;
                popupShowing = true;
            }
        }
        
        public void cancel() {
            str = new StringBuilder();
            popupShowing = false;
            if (null != popup) {
                popup.hide();
                popup = null;
            }
            if (null != popupTimer) {
                popupTimer.stop();
                popupTimer = null;
            }
            isFindingAsYouType = false;
        }
        
        @Override
        public void keyPressed(KeyEvent e) {
            if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                cancel();
            }
        }
        
        @Override
        public void keyTyped(KeyEvent e) {
            switch (e.getKeyChar()) {
            case KeyEvent.CHAR_UNDEFINED:
                return;
                
            case KeyEvent.VK_ENTER:
            case 13: // no KeyEvent constant for this, but this comes up on OSX for shift-enter
                if (!str.toString().isEmpty()) {
                    // no need to add (or subtract) 1 -- the table selection will already
                    // have been advanced by the (shift+) enter key
                    _findNextMatch(tableView.getTable().getSelectedIndex(), e.isShiftDown());
                }
                return;
                
            case KeyEvent.VK_BACK_SPACE:
                if (!str.toString().isEmpty()) {
                    str.deleteCharAt(str.toString().length() - 1);
                }
                break;
                
            case KeyEvent.VK_SPACE:
                // don't trigger if the first character is a space
                if (str.toString().isEmpty()) {
                    return;
                }
                // fall through
                
            default:
                // shift and/or alt-graph down is ok.  anything else is a hotkey (e.g. ctrl-f)
                if (okModifiers != (e.getModifiers() | okModifiers)
                 || !CharUtils.isAsciiPrintable(e.getKeyChar())) { // escape sneaks in here on Windows
                    return;
                }
                str.append(e.getKeyChar());
            }
            
            _findNextMatch(Math.max(0, tableView.getTable().getSelectedIndex()), false);
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        return new CDeckEditorUIMenus().getMenus();
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
        Singletons.getControl().getForgeMenu().setProvider(this);
        
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

