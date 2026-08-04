package forge.gui.interfaces;

import forge.interfaces.IPlayerChangeListener;
import forge.interfaces.IUpdateable;

public interface ILobbyView extends IUpdateable {
    void setPlayerChangeListener(IPlayerChangeListener iPlayerChangeListener);

    /** Implementations that participate in network draft/sealed return their handler; others return null. */
    default IDraftEventHandler getDraftHandler() { return null; }
}
