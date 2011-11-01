package forge.gui.deckeditor;

import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;
import net.slightlymagic.maxmtg.Predicate;
import net.slightlymagic.maxmtg.PredicateString.StringOp;

import org.apache.commons.lang3.StringUtils;

import forge.SetUtils;
import forge.card.CardRules;
import forge.card.CardSet;
import forge.game.GameFormat;
import forge.item.CardPrinted;

/**
 * A panel that holds Name, Type, Rules text fields aligned horizontally
 * together with set filter.
 */
public class FilterNameTypeSetPanel extends JComponent {

    private static final long serialVersionUID = -6409564625432765430L;

    /** The label filter name. */
    private final JLabel labelFilterName = new JLabel();

    /** The label filter type. */
    private final JLabel labelFilterType = new JLabel();

    /** The label filter rules. */
    private final JLabel labelFilterRules = new JLabel();

    /** The txt card name. */
    private final JTextField txtCardName = new JTextField();

    /** The txt card type. */
    private final JTextField txtCardType = new JTextField();

    /** The txt card rules. */
    private final JTextField txtCardRules = new JTextField();

    /** The search set combo. */
    private final JComboBox searchSetCombo = new JComboBox();

    /**
     * Instantiates a new filter name type set panel.
     */
    public FilterNameTypeSetPanel() {
        this.setLayout(new MigLayout("fill, ins 0"));

        this.labelFilterName.setText("Name:");
        this.labelFilterName.setToolTipText("Card names must include the text in this field");
        this.add(this.labelFilterName, "cell 0 1, split 7");
        this.add(this.txtCardName, "wmin 100, grow");

        this.labelFilterType.setText("Type:");
        this.labelFilterType.setToolTipText("Card types must include the text in this field");
        this.add(this.labelFilterType, "");
        this.add(this.txtCardType, "wmin 100, grow");

        this.labelFilterRules.setText("Text:");
        this.labelFilterRules.setToolTipText("Card descriptions must include the text in this field");
        this.add(this.labelFilterRules, "");
        this.add(this.txtCardRules, "wmin 200, grow");

        this.searchSetCombo.removeAllItems();
        this.searchSetCombo.addItem("(all sets and formats)");
        for (final GameFormat s : SetUtils.getFormats()) {
            this.searchSetCombo.addItem(s);
        }
        for (final CardSet s : SetUtils.getAllSets()) {
            this.searchSetCombo.addItem(s);
        }

        this.add(this.searchSetCombo, "wmin 150, grow");
    }

    /**
     * Sets the listeners.
     * 
     * @param onTextChange
     *            the on text change
     * @param onComboChange
     *            the on combo change
     */
    public final void setListeners(final DocumentListener onTextChange, final ItemListener onComboChange) {
        this.txtCardType.getDocument().addDocumentListener(onTextChange);
        this.txtCardRules.getDocument().addDocumentListener(onTextChange);
        this.txtCardName.getDocument().addDocumentListener(onTextChange);
        this.searchSetCombo.addItemListener(onComboChange);
    }

    /**
     * Builds the filter.
     * 
     * @return the predicate
     */
    public final Predicate<CardPrinted> buildFilter() {
        final List<Predicate<CardPrinted>> rules = new ArrayList<Predicate<CardPrinted>>(4);
        if (StringUtils.isNotBlank(this.txtCardName.getText())) {
            rules.add(CardPrinted.Predicates.name(StringOp.CONTAINS, this.txtCardName.getText()));
        }

        if (StringUtils.isNotBlank(this.txtCardType.getText())) {
            rules.add(Predicate.brigde(CardRules.Predicates.joinedType(StringOp.CONTAINS, this.txtCardType.getText()),
                    CardPrinted.FN_GET_RULES));
        }

        if (StringUtils.isNotBlank(this.txtCardRules.getText())) {
            rules.add(Predicate.brigde(CardRules.Predicates.rules(StringOp.CONTAINS, this.txtCardRules.getText()),
                    CardPrinted.FN_GET_RULES));
        }

        if (this.searchSetCombo.getSelectedIndex() != 0) {
            final Object selected = this.searchSetCombo.getSelectedItem();
            if (selected instanceof CardSet) {
                rules.add(CardPrinted.Predicates.printedInSets(((CardSet) selected).getCode()));
            } else if (selected instanceof GameFormat) {
                rules.add(((GameFormat) selected).getFilterRules());
            }
        }

        switch (rules.size()) {
        case 0:
            return Predicate.getTrue(CardPrinted.class);
        case 1:
            return rules.get(0);
        case 2:
            return Predicate.and(rules.get(0), rules.get(1));
        default:
            return Predicate.and(rules);
        }
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public final void clearFilters() {
        this.txtCardName.setText("");
        this.txtCardType.setText("");
        this.txtCardRules.setText("");
        this.searchSetCombo.setSelectedIndex(0);
    }
}
