package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.libgdxgui.Forge;
import forge.adventure.libgdxgui.Graphics;
import forge.adventure.libgdxgui.animation.ForgeAnimation;
import forge.adventure.libgdxgui.assets.ImageCache;
import forge.adventure.libgdxgui.screens.FScreen;
import forge.adventure.libgdxgui.toolbox.FDisplayObject;
import forge.adventure.libgdxgui.toolbox.FOverlay;
import forge.gamemodes.match.LobbySlotType;
import forge.interfaces.IUpdateable;

import java.util.List;

public abstract  class ForgeScene extends Scene implements IUpdateable {

    //GameLobby lobby;
    Graphics localGraphics;
    DuelInput input=new DuelInput(this);
    @Override
    public void dispose() {
    }
    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        /*
        Gdx.gl.glClearColor(0,1,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Stage.getBatch().begin();
        Stage.getBatch().end();
        Stage.act(Gdx.graphics.getDeltaTime());
        Stage.draw();
        */
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.
        if (getScreen() == null) {
            return;
        }


        localGraphics.begin(AdventureApplicationAdapter.instance.getCurrentWidth(), AdventureApplicationAdapter.instance.getCurrentHeight());
        getScreen().screenPos.setSize(AdventureApplicationAdapter.instance.getCurrentWidth(), AdventureApplicationAdapter.instance.getCurrentHeight());
        if (getScreen().getRotate180()) {
            localGraphics.startRotateTransform(AdventureApplicationAdapter.instance.getCurrentWidth() / 2, AdventureApplicationAdapter.instance.getCurrentHeight() / 2, 180);
        }
        getScreen().draw(localGraphics);
        if (getScreen().getRotate180()) {
            localGraphics.endTransform();
        }
        for (FOverlay overlay : FOverlay.getOverlays()) {
            if (overlay.isVisibleOnScreen(getScreen())) {
                overlay.screenPos.setSize(AdventureApplicationAdapter.instance.getCurrentWidth(), AdventureApplicationAdapter.instance.getCurrentHeight());
                overlay.setSize(AdventureApplicationAdapter.instance.getCurrentWidth(), AdventureApplicationAdapter.instance.getCurrentHeight()); //update overlay sizes as they're rendered
                if (overlay.getRotate180()) {
                    localGraphics.startRotateTransform(AdventureApplicationAdapter.instance.getCurrentHeight() / 2, AdventureApplicationAdapter.instance.getCurrentHeight() / 2, 180);
                }
                overlay.draw(localGraphics);
                if (overlay.getRotate180()) {
                    localGraphics.endTransform();
                }
            }
        }
        localGraphics.end();

        //Batch.end();
    }
    @Override
    public void act(float delta) {

        ImageCache.allowSingleLoad();
        ForgeAnimation.advanceAll();
    }


    @Override
    public void enter() {
        if(getScreen()!=null)
            getScreen().setSize(AdventureApplicationAdapter.instance.getCurrentWidth(), AdventureApplicationAdapter.instance.getCurrentHeight());

        Forge.openScreen(getScreen());
        Gdx.input.setInputProcessor(input);

    }
    public abstract FScreen getScreen();

    public void buildTouchListeners(int x, int y, List<FDisplayObject> potentialListeners) {
        if(getScreen()!=null)
            getScreen().buildTouchListeners(x, y, potentialListeners);
    }

    @Override
    public void resLoaded() {
        localGraphics = AdventureApplicationAdapter.instance.getGraphics();
    }


    @Override
    public void update(boolean fullUpdate) {

    }

    @Override
    public void update(int slot, LobbySlotType type) {

    }


}
