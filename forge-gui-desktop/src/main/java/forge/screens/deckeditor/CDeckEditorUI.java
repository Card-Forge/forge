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
package forge.screens.deckeditor;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.Singletons;
import forge.UiCommand;
import forge.deck.DeckBase;
import forge.deck.DeckProxy;
import forge.deck.io.DeckPreferences;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CEditorConstructed;
import forge.screens.deckeditor.controllers.CProbabilities;
import forge.screens.deckeditor.controllers.CStatistics;
import forge.screens.deckeditor.controllers.DeckController;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VCardCatalog;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.match.controllers.CDetailPicture;
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
    private final CDetailPicture cDetailPicture;
    private final VAllDecks vAllDecks;

    private CDeckEditorUI() {
        screenChildControllers = new HashMap<FScreen, ACEditorBase<? extends InventoryItem, ? extends DeckBase>>();
        this.cDetailPicture = new CDetailPicture();
        this.vAllDecks = VAllDecks.SINGLETON_INSTANCE;
        this.vAllDecks.setCDetailPicture(cDetailPicture);
    }

    public CDetailPicture getCDetailPicture() {
        return cDetailPicture;
    }

    /**
     * Set Pack, for when Packs can be shown in the CardPicturePanel.
     * @param item
     */
    public void setCard(final InventoryItem item) {
        cDetailPicture.showItem(item);
    }

    public boolean hasChanges() {
        if (this.childController == null) { return false; }

        final DeckController<?> deckController = this.childController.getDeckController();
        if (deckController == null) { return false; }

        return !deckController.isSaved();
    }

    public boolean canSwitchAway(final boolean isClosing) {
        if (this.childController != null) {
            if (!this.childController.canSwitchAway(isClosing)) {
                return false;
            }
            this.childController.resetUIChanges();
            if (isClosing) {
                screenChildControllers.remove(this.childController.getScreen());
                this.childController = null;
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
    public void setEditorController(final ACEditorBase<? extends InventoryItem, ? extends DeckBase> childController0) {
        final FScreen screen = childController0.getScreen();
        screenChildControllers.put(screen, childController0);
        if (screen == Singletons.getControl().getCurrentScreen()) {
            setCurrentEditorController(childController0);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends InventoryItem> void incrementDeckQuantity(final T item, final int delta) {
        if (item == null || delta == 0) { return; }

        if (delta > 0) { //add items
            final int qty = Math.min(delta, ((ItemManager<T>)childController.getCatalogManager()).getItemCount(item));
            if (qty == 0) { return; }
            ((ACEditorBase<T, ?>)childController).addItem(item, qty);
        }
        else { //remove items
            final int qty = Math.min(-delta, ((ItemManager<T>)childController.getDeckManager()).getItemCount(item));
            if (qty == 0) { return; }
            ((ACEditorBase<T, ?>)childController).removeItem(item, qty);
        }

        CStatistics.SINGLETON_INSTANCE.update();
        CProbabilities.SINGLETON_INSTANCE.update();
    }

    private interface _MoveAction {
        public <T extends InventoryItem> void move(Iterable<Entry<T, Integer>> items);
    }

    private <T extends InventoryItem> void moveSelectedItems(final ItemManager<T> itemManager, final _MoveAction moveAction, final int maxQty) {
        if (maxQty == 0) { return; }

        final ItemPool<T> items = new ItemPool<T>(itemManager.getGenericType());
        for (final T item : itemManager.getSelectedItems()) {
            final int qty = Math.min(maxQty, itemManager.getItemCount(item));
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
    public void addSelectedCards(final boolean toAlternate, final int number) {
        moveSelectedItems(childController.getCatalogManager(), new _MoveAction() {
            @Override public <T extends InventoryItem> void move(final Iterable<Entry<T, Integer>> items) {
                ((ACEditorBase<T, ?>)childController).addItems(items, toAlternate);
            }
        }, number);
    }

    @SuppressWarnings("unchecked")
    public void removeSelectedCards(final boolean toAlternate, final int number) {
        moveSelectedItems(childController.getDeckManager(), new _MoveAction() {
            @Override public <T extends InventoryItem> void move(final Iterable<Entry<T, Integer>> items) {
                ((ACEditorBase<T, ?>)childController).removeItems(items, toAlternate);
            }
        }, number);
    }

    /**
     * Set current editor controller
     */
    @SuppressWarnings("serial")
    private void setCurrentEditorController(final ACEditorBase<? extends InventoryItem, ? extends DeckBase> childController0) {
        this.childController = childController0;
        Singletons.getControl().getForgeMenu().setProvider(childController0);

        if (childController == null) { return; }

        final ItemManager<? extends InventoryItem> catView  = childController.getCatalogManager();
        final ItemManager<? extends InventoryItem> deckView = childController.getDeckManager();

        VCardCatalog.SINGLETON_INSTANCE.setItemManager(catView);
        VCurrentDeck.SINGLETON_INSTANCE.setItemManager(deckView);

        if (!childController.listenersHooked) { //hook listeners the first time the controller is updated
            catView.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(final KeyEvent e) {
                    if (!catView.isIncrementalSearchActive() && KeyEvent.VK_SPACE == e.getKeyCode()) {
                        addSelectedCards(e.isControlDown() || e.isMetaDown(), e.isShiftDown() ? 4: 1);
                    }
                    else if (KeyEvent.VK_LEFT == e.getKeyCode() || KeyEvent.VK_RIGHT == e.getKeyCode()) {
                        deckView.focus();
                        e.consume(); //prevent losing selection
                    }
                }
            });

            deckView.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(final KeyEvent e) {
                    if (!catView.isIncrementalSearchActive() && KeyEvent.VK_SPACE == e.getKeyCode()) {
                        removeSelectedCards(e.isControlDown() || e.isMetaDown(), e.isShiftDown() ? 4: 1);
                    }
                    else if (KeyEvent.VK_LEFT == e.getKeyCode() || KeyEvent.VK_RIGHT == e.getKeyCode()) {
                        catView.focus();
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

            catView.setItemActivateCommand(new UiCommand() {
                @Override
                public void run() {
                    addSelectedCards(false, 1);
                }
            });
            deckView.setItemActivateCommand(new UiCommand() {
                @Override
                public void run() {
                    removeSelectedCards(false, 1);
                }
            });

            catView.setContextMenuBuilder(childController.createContextMenuBuilder(true));
            deckView.setContextMenuBuilder(childController.createContextMenuBuilder(false));

            //set card when selection changes
            catView.addSelectionListener(new ListSelectionListener() {
                @Override public void valueChanged(final ListSelectionEvent e) {
                    setCard(catView.getSelectedItem());
                }
            });

            deckView.addSelectionListener(new ListSelectionListener() {
                @Override public void valueChanged(final ListSelectionEvent e) {
                    setCard(deckView.getSelectedItem());
                }
            });

            catView.setAllowMultipleSelections(true);
            deckView.setAllowMultipleSelections(true);

            childController.listenersHooked = true;
        }

        childController.update();

        catView.applyFilters();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                catView.focus();
            }
        });
    }

    @Override
    public void register() {
        EDocID.CARD_PICTURE.setDoc(cDetailPicture.getCPicture().getView());
        EDocID.CARD_DETAIL.setDoc(cDetailPicture.getCDetail().getView());
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        //change to previously open child controller based on screen
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        final ACEditorBase<? extends InventoryItem, ? extends DeckBase> screenChildController = screenChildControllers.get(screen);
        if (screenChildController != null) {
            setCurrentEditorController(screenChildController);
        } else if (screen == FScreen.DECK_EDITOR_CONSTRUCTED) {
            setEditorController(new CEditorConstructed(cDetailPicture)); //ensure Constructed deck editor controller initialized

            final String currentDeckStr = DeckPreferences.getCurrentDeck();
            if (currentDeckStr != null) {
                final DeckProxy deck = vAllDecks.getLstDecks().stringToItem(currentDeckStr);
                if (deck != null) {
                    vAllDecks.getLstDecks().setSelectedItem(deck);
                    childController.getDeckController().load(deck.getPath(), deck.getName());
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() { }
}

