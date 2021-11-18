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
        public AdventureDeckEditor( ) {
            super( EditorType.Quest, "", false, new FEvent.FEventHandler() {
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
        protected boolean allowDelete() {
            return false;
        }
        @Override
        protected boolean allowsSave() {
            return false;
        }
        @Override
        protected boolean allowsAddBasic() {
            return false;
        }
        @Override
        protected boolean allowRename() {
            return false;
        }
        @Override
        protected boolean isLimitedEditor() {
            return true;
        }

        @Override
        protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
            Map<ColumnDef, ItemColumn> colOverrides = new HashMap<>();
            ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
            ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS, QuestSpellShop.fnDeckCompare, QuestSpellShop.fnDeckGet);
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


        Deck deck = AdventurePlayer.current().getSelectedDeck();
        getScreen();
        screen.getEditorType().getController().setDeck(deck);
        screen.refresh();





        super.enter();

    }


    @Override
    public FScreen getScreen() {
        return screen==null?screen = new AdventureDeckEditor():screen;
    }

}
