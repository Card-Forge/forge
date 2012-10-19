package forge.gui.match.controllers;

import forge.Command;
import forge.Singletons;
import forge.game.player.Player;
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
        for(Player p : Singletons.getModel().getGame().getPlayers())
            VPlayers.SINGLETON_INSTANCE.updatePlayerLabels(p);
        
        VPlayers.SINGLETON_INSTANCE.updateStormLabel();
    }

}
