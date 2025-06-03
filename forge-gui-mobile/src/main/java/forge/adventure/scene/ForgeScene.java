package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import forge.Forge;
import forge.gamemodes.match.LobbySlotType;
import forge.interfaces.IUpdateable;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;

import java.util.List;

/**
 * base class to render base forge screens like the deck editor and matches
 */
public abstract class ForgeScene extends Scene implements IUpdateable {

    @Override
    public void dispose() {
    }

    @Override
    public void render() {
        /*Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.
        if (getScreen() == null) {
            return;
        }

        Forge.getGraphics().begin(Forge.getScreenWidth(), Forge.getScreenHeight());
        getScreen().screenPos.setSize(Forge.getScreenWidth(), Forge.getScreenHeight());
        if (getScreen().getRotate180()) {
            Forge.getGraphics().startRotateTransform( Forge.getScreenWidth() / 2f, Forge.getScreenHeight() / 2f, 180);
        }
        getScreen().draw(Forge.getGraphics());
        if (getScreen().getRotate180()) {
            Forge.getGraphics().endTransform();
        }
        for (FOverlay overlay : FOverlay.getOverlays()) {
            if (overlay.isVisibleOnScreen(getScreen())) {
                overlay.screenPos.setSize(Forge.getScreenWidth(), Forge.getScreenHeight());
                overlay.setSize(Forge.getScreenWidth(), Forge.getScreenHeight()); //update overlay sizes as they're rendered
                if (overlay.getRotate180()) {
                    Forge.getGraphics().startRotateTransform(Forge.getScreenWidth() / 2f, Forge.getScreenHeight() / 2f, 180);
                }
                overlay.draw(Forge.getGraphics());
                if (overlay.getRotate180()) {
                    Forge.getGraphics().endTransform();
                }
            }
        }
        Forge.getGraphics().end();*/
    }

    @Override
    public void act(float delta) {
        /*ImageCache.allowSingleLoad();
        ForgeAnimation.advanceAll();*/
    }


    @Override
    public void enter() {
        FOverlay.hideAll();
        if (getScreen() != null)
            getScreen().setSize(Forge.getScreenWidth(), Forge.getScreenHeight());
        //update language for ForgeScene
        Forge.getLocalizer().setEnglish(Forge.forcedEnglishonCJKMissing);
        Forge.openScreen(getScreen());
        Gdx.input.setInputProcessor(Forge.getInputProcessor());
    }

    public abstract FScreen getScreen();

    public void buildTouchListeners(int x, int y, List<FDisplayObject> potentialListeners) {
        if (getScreen() != null)
            getScreen().buildTouchListeners(x, y, potentialListeners);
    }


    @Override
    public boolean leave() {
        //non ForgeScene is english atm...
        Forge.getLocalizer().setEnglish(Forge.forcedEnglishonCJKMissing);
        return super.leave();
    }

    @Override
    public void update(boolean fullUpdate) {

    }

    @Override
    public void update(int slot, LobbySlotType type) {

    }


}
