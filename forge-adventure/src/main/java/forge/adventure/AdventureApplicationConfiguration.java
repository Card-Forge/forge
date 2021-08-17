package forge.adventure;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;


public class AdventureApplicationConfiguration extends Lwjgl3ApplicationConfiguration {
    public String Plane;

    public AdventureApplicationConfiguration() {
        setResizable(false);

    }

    public void SetPlane(String plane) {
        Plane = plane;
    }

    public void setFullScreen(boolean fullS) {
        if (fullS) {
            setFullscreenMode(getDisplayMode());
        } else
            setWindowedMode((int) (1920 / 1.5), (int) (1080 / 1.5));
    }
}
