package forge.gui.framework;

import forge.Command;

/**
 * Dictates methods required for any controller
 * of an {@link forge.gui.framework.IVDoc}.
 *
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * <br><i>(C at beginning of class name denotes a controller class.)</i>
 */
public interface ICDoc {
    /**
     * Fires when this controller's view tab is selected.
     * Since this method is fired when all tabs are first
     * initialized, be wary of NPEs created by referring to
     * non-existent components.
     * 
     * @return {@link forge.Command} */
    Command getCommandOnSelect();

    /**
     * Call this method after the view singleton has been fully realized
     * for the first time. It should execute operations which should only
     * be done once, but require non-null view components.<br><br>
     * 
     * This method should only be called once, in FView, after singletons are populated.
     */
    void initialize();

    /**
     * Update whatever content is in the panel.
     */
    void update();
}
