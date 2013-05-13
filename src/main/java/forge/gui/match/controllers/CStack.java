package forge.gui.match.controllers;

import java.util.Observable;
import java.util.Observer;

import forge.Command;
import forge.FThreads;
import forge.game.player.Player;
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
public enum CStack implements ICDoc, Observer {
    /** */
    SINGLETON_INSTANCE;
    
    private MagicStack model;
    private Player viewer;

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

    private final Runnable upd = new Runnable() { @Override public void run() {
        SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
        VStack.SINGLETON_INSTANCE.updateStack(model, viewer); 
    } };

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(final Observable arg0, Object arg1) {
        update();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(upd);
    }
    
    public void setModel(MagicStack model, Player guiPlayer) { 
        this.model = model; 
        this.viewer = guiPlayer;
    }

}
