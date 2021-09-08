package forge.adventure;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;


public class AdventureApplicationConfiguration extends Lwjgl3ApplicationConfiguration {
    public AdventureApplicationConfiguration() {
        setResizable(false);

    }
    public void setFullScreen(boolean fullS,int width,int height) {
        if (fullS) {
            setFullscreenMode(getDisplayMode());
        } else
            setWindowedMode(width,height);
    }
}
