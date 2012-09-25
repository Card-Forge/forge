package forge.control.bazaar;

import forge.gui.bazaar.ViewStall;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlStall {
    private final ViewStall view;

    /** @param v0 &emsp; {@link forge.gui.bazaar.ViewStall} */
    public ControlStall(ViewStall v0) {
        this.view = v0;
    }

    /** @return {@link forge.gui.bazaar.ViewStall} */
    public ViewStall getView() {
        return view;
    }
}
