package forge.planarconquestgenerate;


import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.deck.CardRelationMatrixGenerator;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.limited.CardRanker;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Test
public class PlanarConquestGeneratorGATest {


    @Test
    public void test(){

        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });


        PlanarConquestGeneraterGA ga = new PlanarConquestGeneraterGA();
        ga.initializeCards(40);
        ga.run();
        List<Deck> winners = ga.listFinalPopulation();

        DeckStorage storage = new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR);

        for(Deck deck:winners){
            storage.save(deck);
        }
    }
}
