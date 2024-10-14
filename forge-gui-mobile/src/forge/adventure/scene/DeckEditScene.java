package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Stage;
import forge.adventure.data.AdventureEventData;
import forge.adventure.player.AdventurePlayer;
import forge.item.PaperCard;
import forge.screens.FScreen;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class DeckEditScene extends ForgeScene {

    AdventureDeckEditor screen;
    Stage stage;
    AdventureEventData currentEvent;

    private DeckEditScene() {

    }

    private static DeckEditScene object;

    public static DeckEditScene getInstance() {
        if(object==null)
            object=new DeckEditScene();
        return object;
    }


    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
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
        if (screen==null){
            if (currentEvent == null){
                screen = new AdventureDeckEditor(false);
                screen.setEvent(null);
            }
            else {
                screen = new AdventureDeckEditor(currentEvent);
            }
        }
        return screen;
    }

    public boolean isAutoSell(PaperCard pc) {
        return AdventurePlayer.current().getAutoSellCards().contains(pc);
    }

    public boolean isNoSell(PaperCard pc) {
        return AdventurePlayer.current().getNoSellCards().contains(pc);
    }

    public int getCardPrice(PaperCard pc) {
        return AdventurePlayer.current().cardSellPrice(pc);
    }
}
