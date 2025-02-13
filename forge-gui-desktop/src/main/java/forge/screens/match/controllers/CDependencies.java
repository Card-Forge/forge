package forge.screens.match.controllers;

import forge.game.GameView;
import forge.gui.framework.ICDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VDependencies;

/**
 * Controls the combat panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public class CDependencies implements ICDoc {

    private final CMatchUI matchUI;
    private final VDependencies view;
    public CDependencies(CMatchUI cMatchUI) {
        view = new VDependencies(this);
        matchUI = cMatchUI;
    }

    public VDependencies getView() {
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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        GameView game = matchUI.getGameView();
        if (game == null || game.getDependencies() == null) {
            return;
        }
        String dependencies = game.getDependencies();
        view.updateDependencies(dependencies.lines().count(), dependencies);
    }

}
