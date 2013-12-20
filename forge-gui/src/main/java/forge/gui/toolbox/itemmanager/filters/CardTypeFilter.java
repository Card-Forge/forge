package forge.gui.toolbox.itemmanager.filters;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.gui.toolbox.itemmanager.SpellShopManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.item.InventoryItem;
import forge.item.ItemPredicate;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
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

        for (SItemManagerUtil.StatTypes s : buttonMap.keySet()) {
            if (s.predicate != null && buttonMap.get(s).getSelected()) {
                types.add(s.predicate);
            }
        }

        if (types.size() == buttonMap.size()) {
            return Predicates.alwaysTrue();
        }
        return Predicates.compose(Predicates.or(types), PaperCard.FN_GET_RULES);
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        FLabel btnPack = buttonMap.get(StatTypes.PACK_OR_DECK);
        if (btnPack != null && btnPack.getSelected()) {
            return ItemPredicate.Presets.IS_PACK_OR_DECK.apply(item);
        }
        return false;
    }

    @Override
    public void afterFiltersApplied() {
        FLabel btnPack = buttonMap.get(StatTypes.PACK_OR_DECK);
        if (btnPack != null) {
            int count = itemManager.getFilteredItems().countAll(ItemPredicate.Presets.IS_PACK_OR_DECK, InventoryItem.class);
            btnPack.setText(String.valueOf(count));
        }
        super.afterFiltersApplied();
    }
}
