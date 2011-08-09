package forge;

/**
 * <p>ButtonUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ButtonUtil {
    /**
     * <p>reset.</p>
     */
    public static void reset() {
        getOK().setText("OK");
        getCancel().setText("Cancel");

        getOK().setSelectable(false);
        getCancel().setSelectable(false);
    }

    /**
     * <p>enableOnlyOK.</p>
     */
    public static void enableOnlyOK() {
        getOK().setSelectable(true);
        getCancel().setSelectable(false);
    }

    /**
     * <p>enableOnlyCancel.</p>
     */
    public static void enableOnlyCancel() {
        getOK().setSelectable(false);
        getCancel().setSelectable(true);
    }

    /**
     * <p>disableAll.</p>
     */
    public static void disableAll() {
        getOK().setSelectable(false);
        getCancel().setSelectable(false);
    }

    /**
     * <p>enableAll.</p>
     */
    public static void enableAll() {
        getOK().setSelectable(true);
        getCancel().setSelectable(true);
    }

    /**
     * <p>disableOK.</p>
     */
    public static void disableOK() {
        getOK().setSelectable(false);
    }

    /**
     * <p>disableCancel.</p>
     */
    public static void disableCancel() {
        getCancel().setSelectable(false);
    }

    /**
     * <p>getOK.</p>
     *
     * @return a {@link forge.MyButton} object.
     */
    private static MyButton getOK() {
        return AllZone.getDisplay().getButtonOK();
    }

    /**
     * <p>getCancel.</p>
     *
     * @return a {@link forge.MyButton} object.
     */
    private static MyButton getCancel() {
        return AllZone.getDisplay().getButtonCancel();
    }
}
