package forge.itemmanager.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.JPanel;

import forge.card.CardRules;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.itemmanager.SpellShopManager;
import forge.util.IterableUtil;

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
        if (itemManager instanceof SpellShopManager) {
            addToggleButton(widget, StatTypes.PACK_OR_DECK);
        }
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
        final List<Predicate<CardRules>> cmcs = new ArrayList<>();

        for (StatTypes s : buttonMap.keySet()) {
            if (s.predicate != null && buttonMap.get(s).isSelected()) {
                cmcs.add(s.predicate);
            }
        }

        if (cmcs.size() == buttonMap.size()) {
            //use custom return true delegate to validate the item is a card
            return card -> true;
        }
        return PaperCardPredicates.fromRules(IterableUtil.or(cmcs));
    }
}
