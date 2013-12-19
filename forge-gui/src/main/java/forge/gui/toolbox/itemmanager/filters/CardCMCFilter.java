package forge.gui.toolbox.itemmanager.filters;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardCMCFilter extends StatTypeFilter<PaperCard> {
    public CardCMCFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardCMCFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
        addToggleButton(widget, StatTypes.CMC_0);
        addToggleButton(widget, StatTypes.CMC_1);
        addToggleButton(widget, StatTypes.CMC_2);
        addToggleButton(widget, StatTypes.CMC_3);
        addToggleButton(widget, StatTypes.CMC_4);
        addToggleButton(widget, StatTypes.CMC_5);
        addToggleButton(widget, StatTypes.CMC_6);
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        final List<Predicate<CardRules>> cmcs = new ArrayList<Predicate<CardRules>>();

        for (SItemManagerUtil.StatTypes s : buttonMap.keySet()) {
            if (buttonMap.get(s).getSelected()) {
                cmcs.add(s.predicate);
            }
        }

        if (cmcs.size() == 7) {
            return Predicates.alwaysTrue();
        }
        return Predicates.compose(Predicates.or(cmcs), PaperCard.FN_GET_RULES);
    }
}
