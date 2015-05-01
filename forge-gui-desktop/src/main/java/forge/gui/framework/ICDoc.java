package forge.gui.framework;

/**
 * Dictates methods required for any controller of an
 * {@link forge.gui.framework.IVDoc}.
 *
 * <br>
 * <br>
 * <i>(I at beginning of class name denotes an interface.)</i><br>
 * <i>(C at beginning of class name denotes a controller class.)</i>
 */
public interface ICDoc {

    /**
     * Asks this controller to register its docs, so that a layout can be
     * applied to them.
     */
    void register();

    /**
     * This method is called every time the user switches to the tab containing
     * this item.
     */
    void initialize();

    /**
     * Update whatever content is in the panel.
     */
    void update();

}
