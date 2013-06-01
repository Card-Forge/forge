package forge.gui.match.controllers;

import forge.Command;
import forge.game.player.LobbyPlayer;
import forge.game.zone.MagicStack;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.views.VStack;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CStack implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    
    private MagicStack model;
    private LobbyPlayer viewer;

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

    @Override
    public void update() {
        SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
        VStack.SINGLETON_INSTANCE.updateStack(model, viewer); 
    }
    
    public void setModel(MagicStack model, LobbyPlayer guiPlayer) { 
        this.model = model; 
        this.viewer = guiPlayer;
    }

}
