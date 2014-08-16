package forge.screens.match.controllers;

import forge.UiCommand;
import forge.game.player.Player;
import forge.game.zone.MagicStack;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.screens.match.views.VStack;

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
    private Player localPlayer;

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

    @Override
    public void update() {
        SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
        VStack.SINGLETON_INSTANCE.updateStack(model, localPlayer);
    }

    public void setModel(MagicStack model0, Player localPlayer0) { 
        model = model0;
        localPlayer = localPlayer0;
    }
}
