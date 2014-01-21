package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import com.google.common.base.Predicate;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.gui.deckeditor.DeckProxy;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.util.BinaryUtil;
import forge.util.ItemPool;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckColorFilter extends StatTypeFilter<DeckProxy> {
    public DeckColorFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        return new DeckColorFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
        addToggleButton(widget, StatTypes.DECK_WHITE);
        addToggleButton(widget, StatTypes.DECK_BLUE);
        addToggleButton(widget, StatTypes.DECK_BLACK);
        addToggleButton(widget, StatTypes.DECK_RED);
        addToggleButton(widget, StatTypes.DECK_GREEN);
        addToggleButton(widget, StatTypes.DECK_COLORLESS);
        addToggleButton(widget, StatTypes.DECK_MULTICOLOR);
    }

    @Override
    protected final Predicate<DeckProxy> buildPredicate() {
        return new Predicate<DeckProxy>() {
            @Override
            public boolean apply(DeckProxy input) {
                byte colorProfile = input.getColor().getColor();
                if (colorProfile == 0) {
                    return buttonMap.get(StatTypes.DECK_COLORLESS).getSelected();
                }

                boolean wantMulticolor = buttonMap.get(StatTypes.DECK_MULTICOLOR).getSelected();
                if (!wantMulticolor && BinaryUtil.bitCount(colorProfile) > 1) {
                    return false;
                }

                byte colors = 0;
                if (buttonMap.get(StatTypes.DECK_WHITE).getSelected()) {
                    colors |= MagicColor.WHITE;
                }
                if (buttonMap.get(StatTypes.DECK_BLUE).getSelected()) {
                    colors |= MagicColor.BLUE;
                }
                if (buttonMap.get(StatTypes.DECK_BLACK).getSelected()) {
                    colors |= MagicColor.BLACK;
                }
                if (buttonMap.get(StatTypes.DECK_RED).getSelected()) {
                    colors |= MagicColor.RED;
                }
                if (buttonMap.get(StatTypes.DECK_GREEN).getSelected()) {
                    colors |= MagicColor.GREEN;
                }

                if (colors == 0 && wantMulticolor && BinaryUtil.bitCount(colorProfile) > 1) {
                    return true;
                }

                return (colorProfile & colors) == colorProfile;
            }
        };
    }

    private static final Predicate<Deck> IS_WHITE = new Predicate<Deck>() {
        @Override
        public boolean apply(final Deck deck) {
            return deck.getColor().hasAnyColor(MagicColor.WHITE);
        }
    };
    private static final Predicate<Deck> IS_BLUE = new Predicate<Deck>() {
        @Override
        public boolean apply(final Deck deck) {
            return deck.getColor().hasAnyColor(MagicColor.BLUE);
        }
    };
    public static final Predicate<Deck> IS_BLACK = new Predicate<Deck>() {
        @Override
        public boolean apply(final Deck deck) {
            return deck.getColor().hasAnyColor(MagicColor.BLACK);
        }
    };
    public static final Predicate<Deck> IS_RED = new Predicate<Deck>() {
        @Override
        public boolean apply(final Deck deck) {
            return deck.getColor().hasAnyColor(MagicColor.RED);
        }
    };
    public static final Predicate<Deck> IS_GREEN = new Predicate<Deck>() {
        @Override
        public boolean apply(final Deck deck) {
            return deck.getColor().hasAnyColor(MagicColor.GREEN);
        }
    };
    private static final Predicate<Deck> IS_COLORLESS = new Predicate<Deck>() {
        @Override
        public boolean apply(final Deck deck) {
            return deck.getColor().getColor() == 0;
        }
    };
    private static final Predicate<Deck> IS_MULTICOLOR = new Predicate<Deck>() {
        @Override
        public boolean apply(final Deck deck) {
            return BinaryUtil.bitCount(deck.getColor().getColor()) > 1;
        }
    };

    @Override
    public void afterFiltersApplied() {
        final ItemPool<? super DeckProxy> items = itemManager.getFilteredItems();

        buttonMap.get(StatTypes.DECK_WHITE).setText(String.valueOf(items.countAll(IS_WHITE, Deck.class)));
        buttonMap.get(StatTypes.DECK_BLUE).setText(String.valueOf(items.countAll(IS_BLUE, Deck.class)));
        buttonMap.get(StatTypes.DECK_BLACK).setText(String.valueOf(items.countAll(IS_BLACK, Deck.class)));
        buttonMap.get(StatTypes.DECK_RED).setText(String.valueOf(items.countAll(IS_RED, Deck.class)));
        buttonMap.get(StatTypes.DECK_GREEN).setText(String.valueOf(items.countAll(IS_GREEN, Deck.class)));
        buttonMap.get(StatTypes.DECK_COLORLESS).setText(String.valueOf(items.countAll(IS_COLORLESS, Deck.class)));
        buttonMap.get(StatTypes.DECK_MULTICOLOR).setText(String.valueOf(items.countAll(IS_MULTICOLOR, Deck.class)));

        getWidget().revalidate();
    }
}
