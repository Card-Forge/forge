package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Stage;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.world.AdventurePlayer;
import forge.deck.Deck;
import forge.deck.FDeckEditor;
import forge.gamemodes.quest.QuestMode;
import forge.gamemodes.quest.QuestSpellShop;
import forge.gamemodes.quest.data.DeckConstructionRules;
import forge.gamemodes.quest.data.QuestData;
import forge.item.PaperCard;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class DeckEditScene extends ForgeScene {
    public class AdventureDeckEditor extends FDeckEditor {
        public AdventureDeckEditor(boolean commander) {
            super(commander ? EditorType.QuestCommander : EditorType.Quest, "", false, new FEvent.FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    AdventureApplicationAdapter.instance.switchToLast();
                }
            });
        }
        @Override
        public void onActivate() {
            super.onActivate();
            QuestSpellShop.updateDecksForEachCard();
        }

        @Override
        protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
            Map<ColumnDef, ItemColumn> colOverrides = new HashMap<>();
            switch (config) {
                case QUEST_EDITOR_POOL:
                    ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
                    break;
                case QUEST_DECK_EDITOR:
                    ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
                    ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS, QuestSpellShop.fnDeckCompare, QuestSpellShop.fnDeckGet);
                    break;
                default:
                    colOverrides = null; //shouldn't happen
                    break;
            }
            return colOverrides;
        }

        public void refresh() {
            for(TabPage page:tabPages)
            {
                if(page instanceof CardManagerPage)
                    ((CardManagerPage)page).refresh();
            }
        }
    }

    AdventureDeckEditor screen;
    Stage stage;

    public DeckEditScene() {

    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }



    @Override
    public void enter() {
        QuestData data = new QuestData("", 0, QuestMode.Classic, null, false, "", DeckConstructionRules.Commander);
        FModel.getQuest().load(data);


        FModel.getQuest().getCards().getCardpool().clear();



        for (Map.Entry<PaperCard, Integer> card : AdventurePlayer.current().getCards())
            FModel.getQuest().getCards().addSingleCard(card.getKey(), card.getValue());


        Deck deck = AdventurePlayer.current().getDeck();
        getScreen();
        screen.getEditorType().getController().setDeck(deck);
        screen.refresh();





        super.enter();

    }


    @Override
    public FScreen getScreen() {
        return screen==null?screen = new AdventureDeckEditor(false):screen;
    }

}
