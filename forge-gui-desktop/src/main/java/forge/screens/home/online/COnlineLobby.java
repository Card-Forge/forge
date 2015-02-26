package forge.screens.home.online;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.screens.home.CLobby;

public enum COnlineLobby implements ICDoc {
    SINGLETON_INSTANCE;

    private final VOnlineLobby view = VOnlineLobby.SINGLETON_INSTANCE;
    private final CLobby lobby = new CLobby(view.getLobby());

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        lobby.update();
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        lobby.initialize();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

}
