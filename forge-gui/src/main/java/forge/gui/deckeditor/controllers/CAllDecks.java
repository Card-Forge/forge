package forge.gui.deckeditor.controllers;

import forge.Command;
import forge.Singletons;
import forge.gui.deckeditor.views.VAllDecks;
import forge.gui.framework.ICDoc;

/** 
 * Controls the "all decks" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CAllDecks implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        VAllDecks.SINGLETON_INSTANCE.getLstDecks().setDecks(
                Singletons.getModel().getDecks().getConstructed());
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

}
