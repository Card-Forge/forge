package forge.control;

import forge.control.home.ControlConstructed;
import forge.control.home.ControlDraft;
import forge.control.home.ControlQuest;
import forge.control.home.ControlSealed;
import forge.control.home.ControlUtilities;
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

    /** @return ControlConstructed */
    public ControlConstructed getControlConstructed() {
        return view.getViewConstructed().getControl();
    }

    /** @return ControlDraft */
    public ControlDraft getControlDraft() {
        return view.getViewDraft().getControl();
    }

    /** @return ControlSealed */
    public ControlSealed getControlSealed() {
        return view.getViewSealed().getControl();
    }

    /** @return ControlQuest */
    public ControlQuest getControlQuest() {
        return view.getViewQuest().getControl();
    }

    /** @return ControlUtilities */
    public ControlUtilities getControlUtilities() {
        return view.getViewUtilities().getControl();
    }
}
