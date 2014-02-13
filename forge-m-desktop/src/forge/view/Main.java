package forge.view;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;

import forge.ForgeGame;

public class Main {
    public static void main (String[] args) {
        new LwjglApplication(new ForgeGame(), "Forge", 320, 480, false);
    }
}
