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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.primitives.Ints;

import forge.Card;
import forge.deck.DeckBase;
import forge.gui.CardContainer;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.SEditorIO.EditorPreference;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CCardCatalog;
import forge.gui.deckeditor.controllers.CProbabilities;
import forge.gui.deckeditor.controllers.CStatistics;
import forge.gui.deckeditor.tables.EditorTableModel;
import forge.gui.deckeditor.tables.EditorTableView;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CPicture;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.item.InventoryItem;

/**
 * Constructs instance of deck editor UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CDeckEditorUI implements CardContainer {
    /** */
    SINGLETON_INSTANCE;

    private ACEditorBase<? extends InventoryItem, ? extends DeckBase> childController;
    private boolean isFindingAsYouType = false;

    private CDeckEditorUI() {
    }

    //========== Overridden from CardContainer

    @Override
    public void setCard(final Card c) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showCard(c);
    }

    @Override
    public Card getCard() {
        return CDetail.SINGLETON_INSTANCE.getCurrentCard();
    }

    /**
     * Set Pack, for when Packs can be shown in the CardPicturePanel.
     * @param item
     */
    public void setCard(final InventoryItem item) {
        CDetail.SINGLETON_INSTANCE.showCard(item);
        CPicture.SINGLETON_INSTANCE.showCard(item);
    }

    //========= Accessor/mutator methods
    /**
     * @return ACEditorBase<?, ?>
     */
    public ACEditorBase<? extends InventoryItem, ? extends DeckBase> getCurrentEditorController() {
        return childController;
    }

    /**
     * Set controller for current configuration of editor.
     * 
     * @param editor0 &emsp; {@link forge.gui.deckeditor.controllers.ACEditorBase}<?, ?>
     */
    public void setCurrentEditorController(ACEditorBase<? extends InventoryItem, ? extends DeckBase> editor0) {
        this.childController = editor0;
        updateController();
        if (childController != null) {
            boolean wantElastic = SEditorIO.getPref(EditorPreference.elastic_columns);
            boolean wantUnique = SEditorIO.getPref(EditorPreference.display_unique_only);
            childController.getTableCatalog().setWantElasticColumns(wantElastic);
            childController.getTableDeck().setWantElasticColumns(wantElastic);
            childController.getTableCatalog().setWantUnique(wantUnique);
            childController.getTableDeck().setWantUnique(wantUnique);
            CCardCatalog.SINGLETON_INSTANCE.applyCurrentFilter();
        }
    }
    
    private interface _MoveAction {
        void move(InventoryItem item, int qty);
    }

    private void moveSelectedCards(EditorTableView<InventoryItem> table, _MoveAction moveAction, int maxQty) {
        List<InventoryItem> items = table.getSelectedCards();
        if (items.isEmpty()) {
            return;
        }
        
        for (InventoryItem item : items) {
            int toMove = Math.min(maxQty, table.getCardCount(item));
            moveAction.move(item, toMove);
        }

        CStatistics.SINGLETON_INSTANCE.update();
        CProbabilities.SINGLETON_INSTANCE.update();
    }

    @SuppressWarnings("unchecked")
    public void addSelectedCards(final boolean toAlternate, int number) {
        moveSelectedCards((EditorTableView<InventoryItem>)childController.getTableCatalog(),
                new _MoveAction() {
            @Override
            public void move(InventoryItem item, int qty) {
                childController.addCard(item, toAlternate, qty);
            }
        }, number);
    }

    @SuppressWarnings("unchecked")
    public void removeSelectedCards(final boolean toAlternate, int number) {
        moveSelectedCards((EditorTableView<InventoryItem>)childController.getTableDeck(),
                new _MoveAction() {
            @Override
            public void move(InventoryItem item, int qty) {
                childController.removeCard(item, toAlternate, qty);
            }
        }, number);
    }

    @SuppressWarnings("unchecked")
    public void removeAllCards(final boolean toAlternate) {
        EditorTableView<InventoryItem> v = (EditorTableView<InventoryItem>)childController.getTableDeck();
        v.getTable().selectAll();
        moveSelectedCards(v, new _MoveAction() {
            @Override
            public void move(InventoryItem item, int qty) {
                childController.removeCard(item, toAlternate, qty);
            }
        }, Integer.MAX_VALUE);
    }
    
    //========== Other methods
    private interface _MoveCard {
        void moveCard(boolean toAlternate, int qty);
    }
    
    private class _ContextMenuBuilder implements ACEditorBase.ContextMenuBuilder {
        private final MouseEvent _e;
        private final JTable     _nextTable;
        private final _MoveCard  _onMove;
        private final int        _numSelected;
        private final JPopupMenu _menu = new JPopupMenu("TableContextMenu");
        private boolean          _showTextFilterItem = false;
        
        public _ContextMenuBuilder(MouseEvent e, JTable table, JTable nextTable, _MoveCard onMove) {
            _e         = e;
            _nextTable = nextTable;
            _onMove    = onMove;
            
            // ensure the table has focus
            if (!table.hasFocus()) {
                table.requestFocusInWindow();
            }
            
            // if item under the cursor is not selected, select it
            int row = table.rowAtPoint(e.getPoint());
            if (!Ints.contains(table.getSelectedRows(), row)) {
                table.setRowSelectionInterval(row, row);
            }
            
            // record selection count
            _numSelected = table.getSelectedRowCount();
        }
        
        private void show() {
            _menu.addSeparator();
            
            GuiUtils.addMenuItem(_menu, "Jump to previous table",
                    KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), new Runnable() {
                @Override public void run() { _nextTable.requestFocusInWindow(); }
            });
            GuiUtils.addMenuItem(_menu, "Jump to next table",
                    KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), new Runnable() {
                @Override public void run() { _nextTable.requestFocusInWindow(); }
            });
            
            if (_showTextFilterItem) {
                GuiUtils.addMenuItem(_menu, "Jump to text filter",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                        new Runnable() {
                    @Override public void run() {
                        VCardCatalog.SINGLETON_INSTANCE.getTxfSearch().requestFocusInWindow();
                    }
                });
            }

            _menu.show(_e.getComponent(), _e.getX(), _e.getY());
        }

        private String _doNoun(String nounSingular, String nounPlural) {
            if (1 == _numSelected) {
                return nounSingular;
            }
            return String.format("%d %s", _numSelected, nounPlural);
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
        
        @Override
        public void addTextFilterItem() {
            _showTextFilterItem = true;
        }
    }
    
    /**
     * Updates listeners for current controller.
     */
    private void updateController() {
        EditorTableView<? extends InventoryItem> catView  = childController.getTableCatalog();
        EditorTableView<? extends InventoryItem> deckView = childController.getTableDeck();
        final JTable catTable  = catView.getTable();
        final JTable deckTable = deckView.getTable();
        final _FindAsYouType catFind  = new _FindAsYouType(catView);
        final _FindAsYouType deckFind = new _FindAsYouType(deckView);

        catTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isFindingAsYouType && KeyEvent.VK_SPACE == e.getKeyCode()) {
                    addSelectedCards(e.isControlDown() || e.isMetaDown(), e.isShiftDown() ? 4: 1);
                } else if (KeyEvent.VK_LEFT == e.getKeyCode() || KeyEvent.VK_RIGHT == e.getKeyCode()) {
                    deckTable.requestFocusInWindow();
                } else if (KeyEvent.VK_F == e.getKeyCode()) {
                    // let ctrl/cmd-F set focus to the text filter box
                    if (e.isControlDown() || e.isMetaDown()) {
                        VCardCatalog.SINGLETON_INSTANCE.getTxfSearch().requestFocusInWindow();
                    }
                }
            }
        });

        deckTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isFindingAsYouType && KeyEvent.VK_SPACE == e.getKeyCode()) {
                    removeSelectedCards(e.isControlDown() || e.isMetaDown(), e.isShiftDown() ? 4: 1);
                } else if (KeyEvent.VK_LEFT == e.getKeyCode() || KeyEvent.VK_RIGHT == e.getKeyCode()) {
                    catTable.requestFocusInWindow();
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
        
        catTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) { addSelectedCards(false, 1); }
                else if (MouseEvent.BUTTON3 == e.getButton()) {
                    _ContextMenuBuilder cmb = new _ContextMenuBuilder(e, catTable, deckTable, onAdd);
                    childController.buildAddContextMenu(cmb);
                    cmb.show();
                }
            }
        });

        deckTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) { removeSelectedCards(false, 1); }
                else if (MouseEvent.BUTTON3 == e.getButton()) {
                    _ContextMenuBuilder cmb = new _ContextMenuBuilder(e, deckTable, catTable, onRemove);
                    childController.buildRemoveContextMenu(cmb);
                    cmb.show();
                }
            }
        });

        catTable.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent arg0) {
                catFind.cancel();
            }
        });
        deckTable.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent arg0) {
                deckFind.cancel();
            }
        });
        
        // highlight items as the user types a portion of their names
        catTable.addKeyListener(catFind);
        deckTable.addKeyListener(deckFind);
        
        childController.init();
    }
    
    private class _FindAsYouType extends KeyAdapter {
        private StringBuilder str = new StringBuilder();
        private final FLabel popupLabel = new FLabel.Builder().fontAlign(SwingConstants.LEFT).opaque().build();
        private boolean popupShowing = false;
        private Popup popup;
        private Timer popupTimer;
        private final EditorTableView<? extends InventoryItem> tableView;
        static final int okModifiers = KeyEvent.SHIFT_MASK | KeyEvent.ALT_GRAPH_MASK;
        
        public _FindAsYouType(EditorTableView<? extends InventoryItem> tableView) {
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
            int numItems = tableView.getTable().getRowCount();
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
                @SuppressWarnings("unchecked")
                EditorTableModel<? extends InventoryItem> tableModel =
                        (EditorTableModel<? extends InventoryItem>)tableView.getTable().getModel();
                if (StringUtils.containsIgnoreCase(tableModel.rowToCard(idx).getKey().getName(), searchStr)) {
                    tableView.selectAndScrollTo(idx);
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
            popupLabel.setForeground(found ? FSkin.getColor(FSkin.Colors.CLR_TEXT) : new Color(255, 0, 0));
            
            if (popupShowing) {
                _setPopupSize();
                popupTimer.restart();
            } else {
                PopupFactory factory = PopupFactory.getSharedInstance();
                Point tableLoc = tableView.getTable().getTableHeader().getLocationOnScreen();
                popup = factory.getPopup(null, popupLabel, tableLoc.x + 10, tableLoc.y + 10);
                SwingUtilities.getRoot(popupLabel).setBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE));
                
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
                    _findNextMatch(tableView.getTable().getSelectedRow(), e.isShiftDown());
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
            
            _findNextMatch(Math.max(0, tableView.getTable().getSelectedRow()), false);
        }
    }
}

