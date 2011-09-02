package forge.gui.deckeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.slightlymagic.maxmtg.Predicate;

import forge.Constant;
import forge.GUI_DeckAnalysis;
import forge.card.CardPool;
import forge.card.CardPrinted;
import forge.card.CardRules;
import forge.card.CardPoolView;

public abstract class DeckEditorBase extends JFrame implements DeckDisplay  {
    private static final long serialVersionUID = -401223933343539977L;

    protected FilterCheckBoxes filterBoxes;
    // set this to false when resetting filter from code (like "clearFiltersPressed"), reset when done.
    protected boolean isFiltersChangeFiringUpdate = true;
 

    protected CardPanelBase cardView;
    
    // CardPools and Table data for top and bottom lists
    protected TableWithCards top;
    protected TableWithCards bottom;
    
    // top shows available card pool
    // if constructed, top shows all cards
    // if sealed, top shows N booster packs
    // if draft, top shows cards that were chosen
    public final TableWithCards getTopTableModel() { return top; }
    public final CardPoolView getTop() { return top.getCards(); }
    // bottom shows player's choice - be it deck or draft
    public final CardPoolView getBottom() { return bottom.getCards(); }

    // THIS IS HERE FOR OVERLOADING!!!1
    // or may be return abstract getFilter from derived class + this filter ... virtual protected member, but later
    protected Predicate<CardRules> buildFilter() {
        if (null == filterBoxes) {
            return Predicate.getTrue(CardRules.class);
        }
        return filterBoxes.buildFilter();
    }

    void analysisButton_actionPerformed(ActionEvent e) {
        CardPoolView deck = bottom.getCards(); 
        if (deck.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Cards in deck not found.", "Analysis Deck",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            DeckEditorBase g = DeckEditorBase.this;
            GUI_DeckAnalysis dAnalysis = new GUI_DeckAnalysis(g, deck);
            dAnalysis.setVisible(true);
            g.setEnabled(false);
        }
    }

    protected ItemListener itemListenerUpdatesDisplay = new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (isFiltersChangeFiringUpdate) { updateDisplay(); }
        }
    };
    
    public void setDecks(CardPoolView topParam, CardPoolView bottomParam) {
        top.setDeck(topParam);
        bottom.setDeck(bottomParam);
    }

    public void updateDisplay() {
        top.setFilter(buildFilter());
    }

}
