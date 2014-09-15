package forge.screens.match.controllers;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.screens.match.views.VPlayers;
import forge.view.IGameView;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CPlayers implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    private IGameView game;

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
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
        VPlayers.SINGLETON_INSTANCE.update(game);
    }

    public void setModel(IGameView game) {
        this.game = game;
    }

}
