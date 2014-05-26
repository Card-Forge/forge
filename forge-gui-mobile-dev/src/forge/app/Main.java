package forge.app;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;

import forge.Forge;
import forge.util.Utils;

public class Main {
    public static void main(String[] args) {
        new ForgeApplication(new Forge());
    }

    private static class ForgeApplication extends LwjglApplication {
        private ForgeApplication(Forge app) {
            super(app, "Forge", (int)Utils.BASE_WIDTH, (int)Utils.BASE_HEIGHT, true);
            app.initialize(mainLoopThread, new LwjglClipboard(), "../forge-gui/");
        }
    }
}
