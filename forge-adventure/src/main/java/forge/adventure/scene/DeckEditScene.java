package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import forge.adventure.libgdxgui.Forge;
import forge.adventure.libgdxgui.Graphics;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.world.WorldSave;
import forge.adventure.libgdxgui.animation.ForgeAnimation;
import forge.adventure.libgdxgui.assets.ImageCache;
import forge.deck.Deck;
import forge.gamemodes.quest.QuestMode;
import forge.gamemodes.quest.data.DeckConstructionRules;
import forge.gamemodes.quest.data.QuestData;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.adventure.libgdxgui.screens.FScreen;
import forge.adventure.libgdxgui.screens.quest.QuestDeckEditor;
import forge.adventure.libgdxgui.toolbox.FDisplayObject;
import forge.adventure.libgdxgui.toolbox.FOverlay;

import java.util.List;

public class DeckEditScene extends Scene {
    QuestDeckEditor screen;
    Graphics localGraphics;
    Stage stage;
    private DuelInput duelInput;

    public DeckEditScene() {

    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

    @Override
    public void render() {

        ImageCache.allowSingleLoad();
        ForgeAnimation.advanceAll();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.
        if (screen == null) {
            return;
        }


        localGraphics.begin(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());
        screen.screenPos.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());
        if (screen.getRotate180()) {
            localGraphics.startRotateTransform(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth() / 2, AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight() / 2, 180);
        }
        screen.draw(localGraphics);
        if (screen.getRotate180()) {
            localGraphics.endTransform();
        }
        for (FOverlay overlay : FOverlay.getOverlays()) {
            if (overlay.isVisibleOnScreen(screen)) {
                overlay.screenPos.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());
                overlay.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight()); //update overlay sizes as they're rendered
                if (overlay.getRotate180()) {
                    localGraphics.startRotateTransform(AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight() / 2, AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight() / 2, 180);
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

    public void buildTouchListeners(int x, int y, List<FDisplayObject> potentialListeners) {
        screen.buildTouchListeners(x, y, potentialListeners);
    }

    @Override
    public void Enter() {
        QuestData data = new QuestData("", 0, QuestMode.Classic, null, false, "", DeckConstructionRules.Commander);
        FModel.getQuest().load(data);
        Deck deck = WorldSave.getCurrentSave().player.getDeck();
        FModel.getQuest().getCards().getCardpool().clear();


        for (PaperCard card : deck.getAllCardsInASinglePool().toFlatList())
            FModel.getQuest().getCards().addSingleCard(card, 1);

        screen = new QuestDeckEditor(false);
        screen.getEditorType().getController().setDeck(deck);
        screen.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());

        Gdx.input.setInputProcessor(duelInput);
        Forge.openScreen(screen);

    }

    @Override
    public FScreen forgeScreen() {
        return screen;
    }

    @Override
    public void ResLoaded() {
        duelInput = new DuelInput();
        localGraphics = AdventureApplicationAdapter.CurrentAdapter.getGraphics();
        //lobby = new LocalLobby();
        //initLobby(lobby);


    }
}
