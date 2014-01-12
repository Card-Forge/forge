package forge.gui.toolbox.itemmanager.filters;

import java.util.Map;

import javax.swing.JPanel;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.deck.Deck;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SFilterUtil;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.item.PaperCard;
import forge.util.ItemPoolView;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckColorFilter extends StatTypeFilter<Deck> {
    public DeckColorFilter(ItemManager<? super Deck> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<Deck> createCopy() {
        return new DeckColorFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
        addColorButtons(widget);
    }

    @Override
    protected final Predicate<Deck> buildPredicate() {
        return Deck.createPredicate(SFilterUtil.buildColorFilter(buttonMap));
    }

    @Override
    public void afterFiltersApplied() {
        final ItemPoolView<? super Deck> items = itemManager.getFilteredItems();

        for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
            if (btn.getKey().predicate != null) {
                int count = items.countAll(Deck.createPredicate(Predicates.compose(btn.getKey().predicate,
                        PaperCard.FN_GET_RULES)), Deck.class);
                btn.getValue().setText(String.valueOf(count));
            }
        }
        getWidget().revalidate();
    }
}
