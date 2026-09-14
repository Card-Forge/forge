package forge.adventure.scene;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Adventure;
import forge.adventure.data.AdventureEventData;
import forge.screens.FScreen;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class DeckEditScene extends ForgeScene {

    AdventureDeckEditor screen;
    AdventureEventData currentEvent;

    private DeckEditScene() {
    }

    private static DeckEditScene object;
    TextureRegion backDrop;

    public static DeckEditScene getInstance(TextureRegion backdrop) {
        if(object == null)
            object = new DeckEditScene();
        object.backDrop = backdrop;
        return object;
    }


    public void loadEvent(AdventureEventData event){
        currentEvent = event;
    }

    @Override
    public boolean leave() {
        Adventure.getInstance().renderTransitionScreen = true;
        return super.leave();
    }

    @Override
    public void enter() {
        screen = null;
        getScreen();
        screen.refresh();
        Adventure.getInstance().renderTransitionScreen = false;
        super.enter();
    }

    @Override
    public FScreen getScreen() {
        if (screen == null) {
            if (currentEvent == null) {
                screen = new AdventureDeckEditor(false, backDrop);
                screen.setEvent(null);
            }
            else {
                screen = new AdventureDeckEditor(currentEvent, backDrop);
            }
        }
        return screen;
    }
}
