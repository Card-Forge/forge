package forge.screens.match.views;

import forge.toolbox.FScrollPane;

public abstract class VDisplayArea extends FScrollPane {
    public abstract int getCount();
    public abstract void update();
}
