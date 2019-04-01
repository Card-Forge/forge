package forge.itemmanager;

import forge.item.PaperToken;
import forge.itemmanager.filters.ItemFilter;
import forge.itemmanager.filters.TokenSearchFilter;
import forge.screens.match.controllers.CDetailPicture;

import javax.swing.*;

public class TokenManager extends ItemManager<PaperToken> {
    public TokenManager(final CDetailPicture cDetailPicture, final boolean wantUnique0) {
        super(PaperToken.class, cDetailPicture, wantUnique0);
    }

    @Override
    protected void addDefaultFilters() {

    }

    @Override
    protected ItemFilter<PaperToken> createSearchFilter() {
        return createSearchFilter(this);
    }

    public static ItemFilter<PaperToken> createSearchFilter(final ItemManager<? super PaperToken> itemManager) {
        return new TokenSearchFilter(itemManager);
    }

    @Override
    protected void buildAddFilterMenu(JMenu menu) {

    }
}
