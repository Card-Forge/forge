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

    private static Boolean lastAppliedEnglishState = null;

    @Override
    public void dispose() {
    }

    @Override
    public void render() {
    }

    @Override
    public void act(float delta) {
    }

    @Override
    public void enter() {
        FOverlay.hideAll();

        final FScreen activeScreen = getScreen();
        if (activeScreen != null) {
            activeScreen.setSize(Forge.getScreenWidth(), Forge.getScreenHeight());
        }

        final boolean currentForcedState = Forge.forcedEnglishonCJKMissing;
        if (lastAppliedEnglishState == null || lastAppliedEnglishState != currentForcedState) {
            lastAppliedEnglishState = currentForcedState;
            Forge.getLocalizer().setEnglish(currentForcedState);
        }

        Forge.openScreen(activeScreen);
        Gdx.input.setInputProcessor(Forge.getInputProcessor());
    }

    public abstract FScreen getScreen();

    public void buildTouchListeners(int x, int y, List<FDisplayObject> potentialListeners) {
        final FScreen activeScreen = getScreen();
        if (activeScreen != null) {
            activeScreen.buildTouchListeners(x, y, potentialListeners);
        }
    }

    @Override
    public boolean leave() {
        final boolean currentForcedState = Forge.forcedEnglishonCJKMissing;
        if (lastAppliedEnglishState == null || lastAppliedEnglishState != currentForcedState) {
            lastAppliedEnglishState = currentForcedState;
            Forge.getLocalizer().setEnglish(currentForcedState);
        }
        return super.leave();
    }

    @Override
    public void update(boolean fullUpdate) {
    }

    @Override
    public void update(int slot, LobbySlotType type) {
    }
}
