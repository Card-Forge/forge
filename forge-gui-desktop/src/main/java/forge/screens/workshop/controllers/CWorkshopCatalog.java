package forge.screens.workshop.controllers;

import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.workshop.views.VWorkshopCatalog;

/**
 * Controls the "card catalog" panel in the workshop UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CWorkshopCatalog implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private CWorkshopCatalog() {
    }

    //========== Overridden methods

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().setup(ItemManagerConfig.WORKSHOP_CATALOG);
        //TODO: Restore previously selected card
    }
}
