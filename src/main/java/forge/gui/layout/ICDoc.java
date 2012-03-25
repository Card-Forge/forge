package forge.gui.layout;

import forge.Command;

/**
 * Dictates methods required for any controller
 * of an {@link forge.gui.layout.IVDoc}.
 *
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * <br><i>(C at beginning of class name denotes a controller class.)</i>
 */
public interface ICDoc {
    /**
     * Fires when this controller's view tab is selected.
     * Since this method is fired when all tabs are first
     * initialized, be wary of NPEs created by referring to
     * non-existing components.
     * 
     * @return {@link forge.Command} */
    Command getCommandOnSelect();

    /**
     * Call this method after the view singleton has been fully realized
     * for the first time. This method should ideally only be called once.
     */
    void initialize();

    /**
     * Update whatever content is in the panel.
     */
    void update();
}
