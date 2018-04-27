package forge.planarconquestgenerate;


import com.google.common.base.Function;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Test
public class PlanarConquestGeneratorGAStandardTest {


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

        PlanarConquestGeneraterGA ga = new PlanarConquestGeneraterGAStandard(new GameRules(GameType.Constructed),
                FModel.getFormats().getStandard(),
                DeckFormat.Constructed,
                40,
                4,
                10);
        ga.run();
        List<Deck> winners = ga.listFinalPopulation();

        DeckStorage storage = new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR);

        for(Deck deck:winners){
            storage.save(deck);
        }
    }
}
