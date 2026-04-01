package forge.adventure.scene;

import forge.adventure.data.AdventureEventData;
import forge.screens.FScreen;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class DeckEditScene extends ForgeScene {

    AdventureDeckEditor screen;
    AdventureEventData currentEvent;

    private DeckEditScene() {}

    private static DeckEditScene object;

    public static DeckEditScene getInstance() {
        if(object == null)
            object = new DeckEditScene();
        return object;
    }


    public void loadEvent(AdventureEventData event){
        currentEvent = event;
    }

    @Override
    public void enter() {
        screen = null;
        getScreen();
        screen.refresh();
        super.enter();
    }

    @Override
    public FScreen getScreen() {
        if (screen == null) {
            if (currentEvent == null) {
                screen = new AdventureDeckEditor(false);
                screen.setEvent(null);
            }
            else {
                screen = new AdventureDeckEditor(currentEvent);
            }
        }
        return screen;
    }
}
