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
    protected FilterCheckBoxes filterBoxes;
    // set this to false when resetting filter from code (like
    // "clearFiltersPressed"), reset when done.
    /** The is filters change firing update. */
    protected boolean isFiltersChangeFiringUpdate = true;

    /** The card view. */
    protected CardPanelBase cardView;

    // CardPools and Table data for top and bottom lists
    /** The top. */
    protected TableWithCards top;

    /** The bottom. */
    protected TableWithCards bottom;

    private GameType gameType;

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#getGameType()
     */
    public final GameType getGameType() {
        return gameType;
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
        return top;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#getTop()
     */
    public final ItemPoolView<InventoryItem> getTop() {
        return top.getCards();
    }

    // bottom shows player's choice - be it deck or draft
    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#getBottom()
     */
    public final ItemPoolView<InventoryItem> getBottom() {
        return bottom.getCards();
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
    final void analysisButton_actionPerformed(final ActionEvent e) {
        ItemPoolView<CardPrinted> deck = ItemPool.createFrom(bottom.getCards(), CardPrinted.class);
        if (deck.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Cards in deck not found.", "Analysis Deck",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            DeckEditorBase g = DeckEditorBase.this;
            DeckAnalysis dAnalysis = new DeckAnalysis(g, deck);
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
        gameType = gametype;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#setDeck(forge.item.ItemPoolView,
     * forge.item.ItemPoolView, forge.game.GameType)
     */
    public void setDeck(final ItemPoolView<CardPrinted> topParam, final ItemPoolView<CardPrinted> bottomParam,
            final GameType gt) {
        gameType = gt;
        top.setDeck(topParam);
        bottom.setDeck(bottomParam);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckDisplay#setItems(forge.item.ItemPoolView,
     * forge.item.ItemPoolView, forge.game.GameType)
     */
    public final <T extends InventoryItem> void setItems(final ItemPoolView<T> topParam,
            final ItemPoolView<T> bottomParam, final GameType gt) {
        gameType = gt;
        top.setDeck(topParam);
        bottom.setDeck(bottomParam);
    }

    /**
     * Update display.
     */
    public final void updateDisplay() {
        top.setFilter(buildFilter());
    }

    /** The item listener updates display. */
    protected ItemListener itemListenerUpdatesDisplay = new ItemListener() {
        public void itemStateChanged(final ItemEvent e) {
            if (isFiltersChangeFiringUpdate) {
                updateDisplay();
            }
        }
    };

    /**
     * This class is used for a feature: when you start typing card name, the
     * list gets auto-filtered.
     */
    protected class OnChangeTextUpdateDisplay implements DocumentListener {
        private void onChange() {
            if (isFiltersChangeFiringUpdate) {
                updateDisplay();
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
            onChange();
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
            onChange();
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
    public final Deck getDeck() {
        Deck deck = new Deck(gameType);
        deck.addMain(ItemPool.createFrom(getBottom(), CardPrinted.class));

        // if sealed or draft, move "top" to sideboard
        if (gameType.isLimited() && gameType != GameType.Quest) {
            deck.addSideboard(ItemPool.createFrom(getTop(), CardPrinted.class));
        }
        return deck;
    }// getDeck()

}
