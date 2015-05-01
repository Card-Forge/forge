package forge.screens.match.controllers;

import forge.gui.framework.ICDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VAntes;

/**
 * Controls the ante panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public class CAntes implements ICDoc {
    private final CMatchUI matchUI;
    private final VAntes view;
    public CAntes(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VAntes(this);
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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        view.update();
    }

    public CMatchUI getMatchUI() {
        return matchUI;
    }

    public VAntes getView() {
        return view;
    }
}
