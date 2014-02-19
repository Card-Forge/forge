package forge.view;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;

import forge.Forge;

public class Main {
    public static void main(String[] args) {
        new LwjglApplication(new Forge(), "Forge", 320, 480, true);
    }
}
