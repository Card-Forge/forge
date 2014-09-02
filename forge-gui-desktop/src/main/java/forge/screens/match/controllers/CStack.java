package forge.screens.match.controllers;

import forge.UiCommand;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.screens.match.views.VStack;
import forge.view.IGameView;
import forge.view.PlayerView;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CStack implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private IGameView model;
    private PlayerView localPlayer;

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

    public void setModel(final IGameView game0, final PlayerView localPlayer) { 
        model = game0;
        this.localPlayer = localPlayer;
    }

}
