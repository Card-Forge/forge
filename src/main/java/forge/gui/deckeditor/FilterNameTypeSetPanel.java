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
import forge.card.CardPrinted;
import forge.card.CardRules;
import forge.card.CardSet;
import forge.game.GameFormat;

/** 
 * A panel that holds Name, Type, Rules text fields aligned horizontally together with set filter
 */
public class FilterNameTypeSetPanel extends JComponent{

    
    private static final long serialVersionUID = -6409564625432765430L;
    public final JLabel labelFilterName = new JLabel();
    public final JLabel labelFilterType = new JLabel();
    public final JLabel labelFilterRules = new JLabel();

    public final JTextField txtCardName = new JTextField();
    public final JTextField txtCardType = new JTextField();
    public final JTextField txtCardRules = new JTextField();
    public final JComboBox searchSetCombo = new JComboBox();
    
    
    public FilterNameTypeSetPanel()
    {
        this.setLayout(new MigLayout("fill, ins 0"));
        
        labelFilterName.setText("Name:");
        labelFilterName.setToolTipText("Card names must include the text in this field");
        this.add(labelFilterName, "cell 0 1, split 7");
        this.add(txtCardName, "wmin 100, grow");

        labelFilterType.setText("Type:");
        labelFilterType.setToolTipText("Card types must include the text in this field");
        this.add(labelFilterType, "");
        this.add(txtCardType, "wmin 100, grow");
        
        labelFilterRules.setText("Text:");
        labelFilterRules.setToolTipText("Card descriptions must include the text in this field");
        this.add(labelFilterRules, "");
        this.add(txtCardRules, "wmin 200, grow");


        searchSetCombo.removeAllItems();
        searchSetCombo.addItem("(all sets and formats)");
        for (GameFormat s : SetUtils.getFormats()) {
            searchSetCombo.addItem(s);
        }        
        for (CardSet s : SetUtils.getAllSets()) {
            searchSetCombo.addItem(s);
        }

        this.add(searchSetCombo, "wmin 150, grow");
    }

    public void setListeners(DocumentListener onTextChange, ItemListener onComboChange) 
    {
        txtCardType.getDocument().addDocumentListener(onTextChange);
        txtCardRules.getDocument().addDocumentListener(onTextChange);
        txtCardName.getDocument().addDocumentListener(onTextChange);
        searchSetCombo.addItemListener(onComboChange);
    }
    
    public Predicate<CardPrinted> buildFilter() {
        List<Predicate<CardPrinted>> rules = new ArrayList<Predicate<CardPrinted>>(4);
        if (StringUtils.isNotBlank(txtCardName.getText())) {
            rules.add(CardPrinted.Predicates.name(StringOp.CONTAINS, txtCardName.getText()));
        }

        if (StringUtils.isNotBlank(txtCardType.getText())) {
            rules.add(Predicate.brigde(CardRules.Predicates.joinedType(StringOp.CONTAINS, txtCardType.getText()), CardPrinted.fnGetRules));
        }
        
        if (StringUtils.isNotBlank(txtCardRules.getText())) {
            rules.add(Predicate.brigde(CardRules.Predicates.rules(StringOp.CONTAINS, txtCardRules.getText()), CardPrinted.fnGetRules));
        }
        
        if (searchSetCombo.getSelectedIndex() != 0) {
            Object selected = searchSetCombo.getSelectedItem();
            if (selected instanceof CardSet) {
                rules.add(CardPrinted.Predicates.printedInSets(((CardSet) selected).getCode()));
            } else if (selected instanceof GameFormat) {
                rules.add(((GameFormat) selected).getFilter());
            }
        }

        switch (rules.size()){
            case 0: return Predicate.getTrue(CardPrinted.class);
            case 1: return rules.get(0);
            case 2: return Predicate.and(rules.get(0), rules.get(1));
            default: return Predicate.and(rules);
        }
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void clearFilters() {
        txtCardName.setText("");
        txtCardType.setText("");
        txtCardRules.setText("");
        searchSetCombo.setSelectedIndex(0);
    }
}
