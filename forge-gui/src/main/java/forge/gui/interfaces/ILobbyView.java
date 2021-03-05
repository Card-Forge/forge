package forge.gui.interfaces;

import forge.interfaces.IPlayerChangeListener;
import forge.interfaces.IUpdateable;

public interface ILobbyView extends IUpdateable {
    void setPlayerChangeListener(IPlayerChangeListener iPlayerChangeListener);
}
