package forge.control.bazaar;

import forge.view.bazaar.ViewStall;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlStall {
    private final ViewStall view;

    /** @param v0 &emsp; {@link forge.view.bazaar.ViewStall} */
    public ControlStall(ViewStall v0) {
        this.view = v0;
    }

    /** @return {@link forge.view.bazaar.ViewStall} */
    public ViewStall getView() {
        return view;
    }
}
