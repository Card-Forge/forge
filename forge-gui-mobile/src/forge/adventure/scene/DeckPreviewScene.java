package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Stage;
import forge.Adventure;
import forge.deck.Deck;
import forge.screens.FScreen;

/**
 * DeckPreviewScene
 * Scene class that contains the Deck editor layout
 */
public class DeckPreviewScene extends ForgeScene {

    AdventureDeckEditor screen;
    Stage stage;
    Deck deckToPreview;

    private Deck lastPreviewedDeckRef = null;

    private DeckPreviewScene() {

    }

    private static DeckPreviewScene object;

    public static DeckPreviewScene getInstance(Deck deckToPreview) {
        if(object == null)
            object = new DeckPreviewScene();

        object.deckToPreview = deckToPreview;

        return object;
    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

    @Override
    public void enter() {
        Adventure.getInstance().renderTransitionScreen = false;
        if (lastPreviewedDeckRef != deckToPreview) {
            screen = null;
            lastPreviewedDeckRef = deckToPreview;
        }
        getScreen();
        screen.refresh();

        super.enter();
    }

    @Override
    public FScreen getScreen() {
        if (screen == null) {
            screen = new AdventureDeckEditor(deckToPreview);
        }
        screen.setEvent(null);
        return screen;
    }

    @Override
    public boolean leave() {
        Adventure.getInstance().renderTransitionScreen = true;
        return super.leave();
    }
}
