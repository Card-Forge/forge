package forge.screens.match.controllers;

import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VStack;

/**
 * Controls the combat panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public class CStack implements ICDoc {

    private final CMatchUI matchUI;
    private final VStack view;
    public CStack(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VStack(this);
    }

    public final CMatchUI getMatchUI() {
        return matchUI;
    }
    public final VStack getView() {
        return view;
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

    @Override
    public void update() {
        SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
        view.updateStack();
    }
}
