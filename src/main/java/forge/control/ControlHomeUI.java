package forge.control;

import forge.view.ViewHomeUI;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlHomeUI {
    private ViewHomeUI view;

    /** @param v0 &emsp; ViewHomeUI */
    public ControlHomeUI(ViewHomeUI v0) {
        view = v0;
    }

    /** */
    public void exit() {
        System.exit(0);
    }

    /** @return ViewHomeUI */
    public ViewHomeUI getView() {
        return view;
    }
}
