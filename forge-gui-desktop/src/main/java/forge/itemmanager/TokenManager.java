package forge.itemmanager;

import forge.item.PaperToken;
import forge.itemmanager.filters.ItemFilter;
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
        return null;
    }

    @Override
    protected void buildAddFilterMenu(JMenu menu) {

    }
}
