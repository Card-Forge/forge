package forge.app;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;

import forge.Forge;
import forge.util.Utils;

public class Main {
    public static void main(String[] args) {
        new LwjglApplication(Forge.getApp(new LwjglClipboard(), "../forge-gui/"), "Forge", (int)Utils.BASE_WIDTH, (int)Utils.BASE_HEIGHT, true);
    }
}
