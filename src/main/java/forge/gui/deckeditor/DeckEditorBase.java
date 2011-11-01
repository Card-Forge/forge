package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.slightlymagic.maxmtg.Predicate;
import forge.deck.Deck;
import forge.game.GameType;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * The Class DeckEditorBase.
 */
public abstract class DeckEditorBase extends JFrame implements DeckDisplay {
    private static final long serialVersionUID = -401223933343539977L;

    /** The filter boxes. */
    private FilterCheckBoxes filterBoxes;
    // set this to false when resetting filter from code (like
    // "clearFiltersPressed"), reset when done.
    /** The is filters change firing update. */
    private boolean isFiltersChangeFiringUpdate = true;

    /** The card view. */
    private CardPanelBase cardView;

    // CardPools and Table data for top and bottom lists
    /** The top. */
    private TableWithCards topTableWithCards;

    /** The bottom. */
    private TableWithCards bottomTableWithCards;

    private GameType gameType;

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#getGameType()
     */
    @Override
    public final GameType getGameType() {
        return this.gameType;
    }

    // top shows available card pool
    // if constructed, top shows all cards
    // if sealed, top shows N booster packs
    // if draft, top shows cards that were chosen
    /**
     * Gets the top table model.
     * 
     * @return the top table model
     */
    public final TableWithCards getTopTableModel() {
        return this.getTopTableWithCards();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#getTop()
     */
    @Override
    public final ItemPoolView<InventoryItem> getTop() {
        return this.getTopTableWithCards().getCards();
    }

    // bottom shows player's choice - be it deck or draft
    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#getBottom()
     */
    @Override
    public final ItemPoolView<InventoryItem> getBottom() {
        return this.getBottomTableWithCards().getCards();
    }

    // THIS IS HERE FOR OVERLOADING!!!1
    // or may be return abstract getFilter from derived class + this filter ...
    // virtual protected member, but later
    /**
     * Builds the filter.
     * 
     * @return the predicate
     */
    protected abstract Predicate<InventoryItem> buildFilter();

    /**
     * Analysis button_action performed.
     * 
     * @param e
     *            the e
     */
    final void analysisButtonActionPerformed(final ActionEvent e) {
        final ItemPoolView<CardPrinted> deck = ItemPool.createFrom(this.getBottomTableWithCards().getCards(),
                CardPrinted.class);
        if (deck.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Cards in deck not found.", "Analysis Deck",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            final DeckEditorBase g = DeckEditorBase.this;
            final DeckAnalysis dAnalysis = new DeckAnalysis(g, deck);
            dAnalysis.setVisible(true);
            g.setEnabled(false);
        }
    }

    /**
     * Instantiates a new deck editor base.
     * 
     * @param gametype
     *            the gametype
     */
    public DeckEditorBase(final GameType gametype) {
        this.gameType = gametype;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#setDeck(forge.item.ItemPoolView,
     * forge.item.ItemPoolView, forge.game.GameType)
     */
    @Override
    public void setDeck(final ItemPoolView<CardPrinted> topParam, final ItemPoolView<CardPrinted> bottomParam,
            final GameType gt) {
        this.gameType = gt;
        this.getTopTableWithCards().setDeck(topParam);
        this.getBottomTableWithCards().setDeck(bottomParam);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#setItems(forge.item.ItemPoolView,
     * forge.item.ItemPoolView, forge.game.GameType)
     */
    @Override
    public final <T extends InventoryItem> void setItems(final ItemPoolView<T> topParam,
            final ItemPoolView<T> bottomParam, final GameType gt) {
        this.gameType = gt;
        this.getTopTableWithCards().setDeck(topParam);
        this.getBottomTableWithCards().setDeck(bottomParam);
    }

    /**
     * Update display.
     */
    public final void updateDisplay() {
        this.getTopTableWithCards().setFilter(this.buildFilter());
    }

    /** The item listener updates display. */
    private ItemListener itemListenerUpdatesDisplay = new ItemListener() {
        @Override
        public void itemStateChanged(final ItemEvent e) {
            if (DeckEditorBase.this.isFiltersChangeFiringUpdate()) {
                DeckEditorBase.this.updateDisplay();
            }
        }
    };

    /**
     * This class is used for a feature: when you start typing card name, the
     * list gets auto-filtered.
     */
    protected class OnChangeTextUpdateDisplay implements DocumentListener {
        private void onChange() {
            if (DeckEditorBase.this.isFiltersChangeFiringUpdate()) {
                DeckEditorBase.this.updateDisplay();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#insertUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public final void insertUpdate(final DocumentEvent e) {
            this.onChange();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#removeUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public final void removeUpdate(final DocumentEvent e) {
            this.onChange();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#changedUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public void changedUpdate(final DocumentEvent e) {
        } // Happend only on ENTER pressed
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#getDeck()
     */
    @Override
    public final Deck getDeck() {
        final Deck deck = new Deck(this.gameType);
        deck.addMain(ItemPool.createFrom(this.getBottom(), CardPrinted.class));

        // if sealed or draft, move "top" to sideboard
        if (this.gameType.isLimited() && (this.gameType != GameType.Quest)) {
            deck.addSideboard(ItemPool.createFrom(this.getTop(), CardPrinted.class));
        }
        return deck;
    } // getDeck()

    /**
     * @return the itemListenerUpdatesDisplay
     */
    public ItemListener getItemListenerUpdatesDisplay() {
        return itemListenerUpdatesDisplay;
    }

    /**
     * @param itemListenerUpdatesDisplay
     *            the itemListenerUpdatesDisplay to set
     */
    public void setItemListenerUpdatesDisplay(ItemListener itemListenerUpdatesDisplay) {
        this.itemListenerUpdatesDisplay = itemListenerUpdatesDisplay; // TODO:
                                                                      // Add 0
                                                                      // to
                                                                      // parameter's
                                                                      // name.
    }

    /**
     * @return the isFiltersChangeFiringUpdate
     */
    public boolean isFiltersChangeFiringUpdate() {
        return isFiltersChangeFiringUpdate;
    }

    /**
     * @param isFiltersChangeFiringUpdate
     *            the isFiltersChangeFiringUpdate to set
     */
    public void setFiltersChangeFiringUpdate(boolean isFiltersChangeFiringUpdate) {
        this.isFiltersChangeFiringUpdate = isFiltersChangeFiringUpdate; // TODO:
                                                                        // Add 0
                                                                        // to
                                                                        // parameter's
                                                                        // name.
    }

    /**
     * @return the cardView
     */
    public CardPanelBase getCardView() {
        return cardView;
    }

    /**
     * @param cardView
     *            the cardView to set
     */
    public void setCardView(CardPanelBase cardView) {
        this.cardView = cardView; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the filterBoxes
     */
    public FilterCheckBoxes getFilterBoxes() {
        return filterBoxes;
    }

    /**
     * @param filterBoxes
     *            the filterBoxes to set
     */
    public void setFilterBoxes(FilterCheckBoxes filterBoxes) {
        this.filterBoxes = filterBoxes; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bottomTableWithCards
     */
    public TableWithCards getBottomTableWithCards() {
        return bottomTableWithCards;
    }

    /**
     * @param bottomTableWithCards
     *            the bottomTableWithCards to set
     */
    public void setBottomTableWithCards(TableWithCards bottomTableWithCards) {
        this.bottomTableWithCards = bottomTableWithCards; // TODO: Add 0 to
                                                          // parameter's name.
    }

    /**
     * @return the topTableWithCards
     */
    public TableWithCards getTopTableWithCards() {
        return topTableWithCards;
    }

    /**
     * @param topTableWithCards
     *            the topTableWithCards to set
     */
    public void setTopTableWithCards(TableWithCards topTableWithCards) {
        this.topTableWithCards = topTableWithCards; // TODO: Add 0 to
                                                    // parameter's name.
    }

}
