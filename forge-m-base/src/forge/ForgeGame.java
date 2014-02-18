package forge;

import java.util.Stack;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;

import forge.gui.home.HomeScreen;
import forge.gui.toolbox.FSkin;

public class ForgeGame implements ApplicationListener {
    private static final Stack<ForgeScreen> screens = new Stack<ForgeScreen>();
    private static ForgeScreen currentScreen;

    @Override
    public void create () {
        Gdx.graphics.setContinuousRendering(false); //save power consumption by disabling continuous rendering

        FSkin.loadLight("journeyman", true);
        FSkin.loadFull(true);
        pushScreen(new HomeScreen());
    }

    public static void popScreen() {
        if (screens.size() < 2) { return; } //don't allow popping final screen
        screens.pop().dispose();
        setCurrentScreen(screens.lastElement());
    }

    public static void pushScreen(ForgeScreen screen0) {
        if (currentScreen == screen0) { return; }
        screens.push(screen0);
        setCurrentScreen(screen0);
    }

    private static void setCurrentScreen(ForgeScreen screen0) {
        currentScreen = screen0;
        Gdx.input.setInputProcessor(currentScreen);
        Gdx.graphics.requestRendering();
    }

    @Override
    public void render () {
        Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        currentScreen.act(Gdx.graphics.getDeltaTime());
        currentScreen.draw();
    }

    @Override
    public void resize(int width, int height) {
        currentScreen.setViewport(width, height);
    }

    @Override
    public void pause () {
    }

    @Override
    public void resume () {
    }

    @Override
    public void dispose () {
        currentScreen.dispose();
    }
}
