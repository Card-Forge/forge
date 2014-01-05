package forge.gui.toolbox.itemmanager.filters;

import java.util.Map;

import javax.swing.JPanel;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SFilterUtil;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.item.DeckBox;
import forge.item.PaperCard;
import forge.util.ItemPoolView;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckColorFilter extends StatTypeFilter<DeckBox> {
    public DeckColorFilter(ItemManager<? super DeckBox> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<DeckBox> createCopy() {
        return new DeckColorFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
        addColorButtons(widget);
    }

    @Override
    protected final Predicate<DeckBox> buildPredicate() {
        return DeckBox.createPredicate(SFilterUtil.buildColorFilter(buttonMap));
    }

    @Override
    public void afterFiltersApplied() {
        final ItemPoolView<? super DeckBox> items = itemManager.getFilteredItems();

        for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
            if (btn.getKey().predicate != null) {
                int count = items.countAll(DeckBox.createPredicate(Predicates.compose(btn.getKey().predicate,
                        PaperCard.FN_GET_RULES)), DeckBox.class);
                btn.getValue().setText(String.valueOf(count));
            }
        }
        getWidget().revalidate();
    }
}
