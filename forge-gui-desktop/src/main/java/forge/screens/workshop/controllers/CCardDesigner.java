package forge.screens.workshop.controllers;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.screens.workshop.views.VCardDesigner;


/** 
 * Controls the "card designer" panel in the workshop UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCardDesigner implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private CCardDesigner() {
    	VCardDesigner.SINGLETON_INSTANCE.getBtnSaveCard().setCommand(new Runnable() {
			@Override
			public void run() {
				CCardScript.SINGLETON_INSTANCE.saveChanges();
			}
		});
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

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
    }
}
