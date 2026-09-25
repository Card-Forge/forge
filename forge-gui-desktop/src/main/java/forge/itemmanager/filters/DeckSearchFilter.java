package forge.itemmanager.filters;

import forge.deck.DeckProxy;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Predicate;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckSearchFilter extends TextSearchFilter<DeckProxy> {
    public DeckSearchFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        DeckSearchFilter copy = new DeckSearchFilter(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        return copy;
    }

    @Override
    protected void applyChange() {
        if (itemManager instanceof DeckManager) {
            ((DeckManager) itemManager).notifySearchChanged(isEnabled() ? getSearchText() : "");
        }
        super.applyChange();
    }

    @Override
    protected boolean applyOnTextChange() {
        return false;
    }

    @Override
    protected Predicate<DeckProxy> buildPredicate() {
        final String text = txtSearch.getText();
        if (text.trim().isEmpty()) {
            return x -> true;
        }

        final Predicate<DeckProxy> namePredicate = SFilterUtil.buildItemTextFilter(text);
        final String[] tokens = StringUtils.split(text.toLowerCase());
        return deck -> namePredicate.test(deck) || deckBrowserTextMatches(deck, tokens);
    }

    private boolean deckBrowserTextMatches(final DeckProxy deck, final String[] tokens) {
        final String searchable = StringUtils.defaultString(deck.getSourceFileName()).toLowerCase();
        for (final String token : tokens) {
            if (searchable.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
