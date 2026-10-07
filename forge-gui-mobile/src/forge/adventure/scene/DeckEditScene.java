package forge.adventure.scene;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Adventure;
import forge.adventure.data.AdventureEventData;
import forge.screens.FScreen;

/**
 * DeckEditScene
 * Scene class that contains the Deck editor layout
 */
public class DeckEditScene extends ForgeScene {

    AdventureEventData currentEvent;

    private DeckEditScene() {}

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
        if (currentEvent == null)
            ((AdventureDeckEditor) getScreen()).setEvent(null);
        ((AdventureDeckEditor) getScreen()).refresh();
        super.enter();
    }

    @Override
    public FScreen getScreen() {
        return currentEvent == null
            ? new AdventureDeckEditor(false, backDrop)
            :  new AdventureDeckEditor(currentEvent, backDrop);
    }
}
