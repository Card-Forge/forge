package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.MagicColor;
import forge.card.CardRulesPredicates.Presets;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SFilterUtil;
import forge.gui.toolbox.itemmanager.SpellShopManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardColorFilter extends StatTypeFilter<PaperCard> {
    public CardColorFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardColorFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
        if (itemManager instanceof SpellShopManager) {
            addToggleButton(widget, StatTypes.PACK_OR_DECK);
        }
        addToggleButton(widget, StatTypes.WHITE);
        addToggleButton(widget, StatTypes.BLUE);
        addToggleButton(widget, StatTypes.BLACK);
        addToggleButton(widget, StatTypes.RED);
        addToggleButton(widget, StatTypes.GREEN);
        addToggleButton(widget, StatTypes.COLORLESS);
        addToggleButton(widget, StatTypes.MULTICOLOR);
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        byte colors = 0;

        if (buttonMap.get(StatTypes.WHITE).getSelected()) {
            colors |= MagicColor.WHITE;
        }
        if (buttonMap.get(StatTypes.BLUE).getSelected()) {
            colors |= MagicColor.BLUE;
        }
        if (buttonMap.get(StatTypes.BLACK).getSelected()) {
            colors |= MagicColor.BLACK;
        }
        if (buttonMap.get(StatTypes.RED).getSelected()) {
            colors |= MagicColor.RED;
        }
        if (buttonMap.get(StatTypes.GREEN).getSelected()) {
            colors |= MagicColor.GREEN;
        }

        boolean wantColorless = buttonMap.get(StatTypes.COLORLESS).getSelected();
        boolean wantMulticolor = buttonMap.get(StatTypes.MULTICOLOR).getSelected();

        Predicate<CardRules> preFinal = null;
        if (wantMulticolor) {
            if (colors == 0) { //handle showing all multi-color cards if all 5 colors are filtered
                preFinal = Presets.IS_MULTICOLOR;
                if (wantColorless) {
                    preFinal = Predicates.or(preFinal, Presets.IS_COLORLESS);
                }
            }
            else if (colors != MagicColor.ALL_COLORS) {
                preFinal = CardRulesPredicates.canCastWithAvailable(colors);
            }
        }
        else {
            preFinal = Predicates.not(Presets.IS_MULTICOLOR);
            if (colors != MagicColor.ALL_COLORS) {
                preFinal = Predicates.and(CardRulesPredicates.canCastWithAvailable(colors), preFinal);
            }
        }
        if (!wantColorless) {
            if (colors != 0 && colors != MagicColor.ALL_COLORS) {
                //if colorless filtered out ensure phyrexian cards don't appear
                //unless at least one of their colors is selected
                preFinal = Predicates.and(preFinal, CardRulesPredicates.isColor(colors));
            }
            preFinal = SFilterUtil.optimizedAnd(preFinal, Predicates.not(Presets.IS_COLORLESS));
        }

        if (preFinal == null) {
            return new Predicate<PaperCard>() { //use custom return true delegate to validate the item is a card
                @Override
                public boolean apply(PaperCard card) {
                    return true;
                }
            };
        }
        return Predicates.compose(preFinal, PaperCard.FN_GET_RULES);
    }
}
