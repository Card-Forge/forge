package forge.screens.match.controllers;

import forge.gui.FThreads;
import forge.gui.framework.ICDoc;
import forge.screens.match.views.VZone;

/**
 * Controls a docked zone tab's refresh lifecycle.
 */
public class CZone implements ICDoc {
    private final VZone view;

    public CZone(final VZone view) {
        this.view = view;
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(view::refresh);
    }
}
