package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SpellShopManager;
import forge.itemmanager.SItemManagerUtil.StatTypes;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;


public class CardTypeFilter extends StatTypeFilter<PaperCard> {
    public CardTypeFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardTypeFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
        if (itemManager instanceof SpellShopManager) {
            addToggleButton(widget, StatTypes.PACK_OR_DECK);
        }
        addToggleButton(widget, StatTypes.LAND);
        addToggleButton(widget, StatTypes.ARTIFACT);
        addToggleButton(widget, StatTypes.CREATURE);
        addToggleButton(widget, StatTypes.ENCHANTMENT);
        addToggleButton(widget, StatTypes.PLANESWALKER);
        addToggleButton(widget, StatTypes.INSTANT);
        addToggleButton(widget, StatTypes.SORCERY);
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        final List<Predicate<CardRules>> types = new ArrayList<Predicate<CardRules>>();

        for (StatTypes s : buttonMap.keySet()) {
            if (s.predicate != null && buttonMap.get(s).isSelected()) {
                types.add(s.predicate);
            }
        }

        if (types.size() == buttonMap.size()) {
            return new Predicate<PaperCard>() { //use custom return true delegate to validate the item is a card
                @Override
                public boolean apply(PaperCard card) {
                    return true;
                }
            };
        }
        return Predicates.compose(Predicates.or(types), PaperCard.FN_GET_RULES);
    }
}
