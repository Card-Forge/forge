package forge.planarconquestgenerate;

import java.io.File;
import java.util.List;

import forge.GuiDesktop;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckgenUtil;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

public class PlanarConquestGeneraterGAModern extends PlanarConquestGeneraterGA {

    private int deckCount = 0;

    public static void main(String[] args){
        test();
    }

    public static void test(){

        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, preferences -> {
            preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
            return null;
        });

        PlanarConquestGeneraterGA ga = new PlanarConquestGeneraterGAModern(new GameRules(GameType.Constructed),
                FModel.getFormats().getModern(),
                DeckFormat.Constructed,
                80,
                2,
                20);
        ga.run();
        List<Deck> winners = ga.listFinalPopulation();

        DeckStorage storage = new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR);

        int i=1;
        for(Deck deck:winners){
            storage.save(new Deck(deck,"GAS_"+i+"_"+deck.getName()));
            i++;
        }
    }

    public PlanarConquestGeneraterGAModern(GameRules rules, GameFormat gameFormat, DeckFormat deckFormat, int cardsToUse, int decksPerCard, int generations){
        super(rules,gameFormat,deckFormat,cardsToUse,decksPerCard,generations);
    }

    protected Deck getDeckForCard(PaperCard card){
        Deck genDeck =  DeckgenUtil.buildCardGenDeck(card, gameFormat, true);
        Deck d = new Deck(genDeck,genDeck.getName()+"_"+deckCount+"_"+generationCount);
        deckCount++;
        return d;
    }

    protected Deck getDeckForCard(PaperCard card, PaperCard card2){
        Deck genDeck = DeckgenUtil.buildCardGenDeck(card, card2, gameFormat, true);
        Deck d = new Deck(genDeck,genDeck.getName()+"_"+deckCount+"_"+generationCount);
        deckCount++;
        return d;
    }





}
