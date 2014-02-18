package forge;

import com.badlogic.gdx.ApplicationListener;

import forge.gui.toolbox.FSkin;

public class ForgeGame implements ApplicationListener {
    @Override
    public void create () {
        FSkin.loadLight("default", true);
        //FSkin.loadFull(true);
    }

    @Override
    public void render () {
    }

    @Override
    public void resize (int width, int height) {
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
