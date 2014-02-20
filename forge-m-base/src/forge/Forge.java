package forge;

import java.util.Stack;

import com.badlogic.gdx.Game;

import forge.assets.FSkin;
import forge.screens.home.HomeScreen;

public class Forge extends Game {
    private static Forge game;
    private static final Stack<FScreen> screens = new Stack<FScreen>();

    public Forge() {
        if (game != null) {
            throw new RuntimeException("Cannot initialize Forge more than once");
        }
        game = this;
    }

    @Override
    public void create() {
        //Gdx.graphics.setContinuousRendering(false); //save power consumption by disabling continuous rendering

        FSkin.loadLight("journeyman", true);
        FSkin.loadFull(true);
        openScreen(new HomeScreen());
    }

    public static void back() {
        if (screens.size() < 2) { return; } //don't allow going back from initial screen
        screens.pop().dispose();
        game.setScreen(screens.lastElement());
    }

    public static void openScreen(FScreen screen0) {
        if (game.getScreen() == screen0) { return; }
        screens.push(screen0);
        game.setScreen(screen0);
    }

    @Override
    public void render () {
        super.render();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause () {
    }

    @Override
    public void resume () {
    }

    @Override
    public void dispose () {
    }
}
