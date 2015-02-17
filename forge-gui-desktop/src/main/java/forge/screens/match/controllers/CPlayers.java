package forge.screens.match.controllers;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VPlayers;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public class CPlayers implements ICDoc {

    private final CMatchUI matchUI;
    private final VPlayers view;
    public CPlayers(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VPlayers(this);
    }

    public final CMatchUI getMatchUI() {
        return matchUI;
    }
    public final VPlayers getView() {
        return view;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
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
}
