package forge.gui.match.controllers;

import forge.AllZone;
import forge.Command;
import forge.gui.framework.ICDoc;
import forge.gui.match.views.VPlayers;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CPlayers implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
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
        VPlayers.SINGLETON_INSTANCE.updatePlayerLabels(AllZone.getComputerPlayer());
        VPlayers.SINGLETON_INSTANCE.updatePlayerLabels(AllZone.getHumanPlayer());
    }

}
