package forge.control;

import forge.view.home.HomeTopLevel;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlHomeUI {
    private HomeTopLevel view;

    /** @param v0 &emsp; HomeTopLevel */
    public ControlHomeUI(HomeTopLevel v0) {
        view = v0;
    }

    /** */
    public void exit() {
        System.exit(0);
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getView() {
        return view;
    }
}
