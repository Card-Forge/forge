package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.ImageCache;
import forge.gamemodes.match.LobbySlotType;
import forge.interfaces.IUpdateable;
import forge.screens.FScreen;
import forge.screens.TransitionScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;

import java.util.List;

/**
 * base class to render base forge screens like the deck editor and matches
 */
public abstract  class ForgeScene extends Scene implements IUpdateable {

    //GameLobby lobby;
    Graphics localGraphics;
    ForgeInput input=new ForgeInput(this);

    @Override
    public void dispose() {
    }
    @Override
    public void render() {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.
        if (getScreen() == null) {
            return;
        }


        localGraphics.begin(Forge.getScreenWidth(), Forge.getScreenHeight());
        getScreen().screenPos.setSize(Forge.getScreenWidth(), Forge.getScreenHeight());
        if (getScreen().getRotate180()) {
            localGraphics.startRotateTransform( Forge.getScreenWidth() / 2f, Forge.getScreenHeight() / 2f, 180);
        }
        getScreen().draw(localGraphics);
        if (getScreen().getRotate180()) {
            localGraphics.endTransform();
        }
        for (FOverlay overlay : FOverlay.getOverlays()) {
            if (overlay.isVisibleOnScreen(getScreen())) {
                overlay.screenPos.setSize(Forge.getScreenWidth(), Forge.getScreenHeight());
                overlay.setSize(Forge.getScreenWidth(), Forge.getScreenHeight()); //update overlay sizes as they're rendered
                if (overlay.getRotate180()) {
                    localGraphics.startRotateTransform(Forge.getScreenWidth() / 2f, Forge.getScreenHeight() / 2f, 180);
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
        FOverlay.hideAll();
        if(getScreen()!=null)
            getScreen().setSize(Forge.getScreenWidth(), Forge.getScreenHeight());

        if (this instanceof DuelScene) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Forge.clearTransitionScreen();
                    Forge.openScreen(getScreen());
                    Gdx.input.setInputProcessor(input);
                }
            };
            Forge.setTransitionScreen(new TransitionScreen(runnable, ScreenUtils.getFrameBufferTexture(), true));
        } else {
            Forge.openScreen(getScreen());
            Gdx.input.setInputProcessor(input);
        }
    }
    public abstract FScreen getScreen();

    public void buildTouchListeners(int x, int y, List<FDisplayObject> potentialListeners) {
        if(getScreen()!=null)
            getScreen().buildTouchListeners(x, y, potentialListeners);
    }

    @Override
    public void resLoaded() {
        localGraphics = Forge.getGraphics();
    }


    @Override
    public void update(boolean fullUpdate) {

    }

    @Override
    public void update(int slot, LobbySlotType type) {

    }


}
