package forge.planarconquestgenerate;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.LobbyPlayer;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.deck.*;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import forge.limited.CardRanker;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.tournament.system.AbstractTournament;
import forge.tournament.system.TournamentPairing;
import forge.tournament.system.TournamentPlayer;
import forge.tournament.system.TournamentSwiss;
import forge.util.AbstractGeneticAlgorithm;
import forge.util.MyRandom;
import forge.util.TextUtil;
import forge.view.SimulateMatch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanarConquestGeneraterGAStandard extends PlanarConquestGeneraterGA {

    private int deckCount = 0;

    public static void main(String[] args){
        test();
    }

    public static void test(){

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

    public PlanarConquestGeneraterGAStandard(GameRules rules, GameFormat gameFormat, DeckFormat deckFormat, int cardsToUse, int decksPerCard, int generations){
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
