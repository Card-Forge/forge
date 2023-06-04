package forge.adventure.scene;

import forge.adventure.data.AdventureEventData;
import forge.adventure.stage.GameHUD;
import forge.screens.FScreen;
import forge.sound.SoundSystem;

/**
 * DraftScene
 * scene class that contains the Deck editor used for draft events
 */
public class DraftScene extends ForgeScene {
    private static DraftScene object;

    public static DraftScene instance() {
        if(object==null)
            object=new DraftScene();
        return object;
    }

    AdventureDeckEditor screen;
    AdventureEventData currentEvent;

    private DraftScene() {

    }

    @Override
    public void dispose() {
    }

    @Override
    public void enter() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        SoundSystem.instance.pause();
        screen = null;
        getScreen();
        screen.refresh();
        super.enter();
    } 
    @Override
    public FScreen getScreen() {
        return screen==null?screen = new AdventureDeckEditor(false, currentEvent):screen;
    }

    public void loadEvent(AdventureEventData event) {
        this.currentEvent = event;
    }
}
