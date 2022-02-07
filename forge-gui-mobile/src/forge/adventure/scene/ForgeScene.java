package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import forge.Forge;
import forge.animation.ForgeAnimation;
import forge.assets.ImageCache;
import forge.gamemodes.match.LobbySlotType;
import forge.interfaces.IUpdateable;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;

import java.util.List;

/**
 * base class to render base forge screens like the deck editor and matches
 */
public abstract  class ForgeScene extends Scene implements IUpdateable {

    //GameLobby lobby;
    ForgeInput input=new ForgeInput(this);
    @Override
    public void dispose() {
    }
    @Override
    public void render() {

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

    }


    @Override
    public void update(boolean fullUpdate) {

    }

    @Override
    public void update(int slot, LobbySlotType type) {

    }


}
