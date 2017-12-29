package forge.screens.match.views;

import forge.toolbox.FScrollPane;

public abstract class VDisplayArea extends FScrollPane {
    public VDisplayArea() {
        setVisible(false); //hide by default
    }
    public abstract int getCount();
    public abstract void update();
}
