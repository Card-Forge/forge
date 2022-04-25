package forge.deck;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.deck.generation.DeckGeneratorBase;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.ItemPool;

/**
 * Created by maustin on 09/05/2017.
 */
public class CommanderDeckGenerator extends DeckProxy implements Comparable<CommanderDeckGenerator> {
    public static List<DeckProxy> getCommanderDecks(final DeckFormat format, boolean isForAi, boolean isCardGen){
        if (format.equals(DeckFormat.Brawl)){
            return getBrawlDecks(format, isForAi, isCardGen);
        }
        ItemPool uniqueCards;
        if (isCardGen){
            uniqueCards = new ItemPool<>(PaperCard.class);
            String matrixKey = (format.equals(DeckFormat.TinyLeaders) ? DeckFormat.Commander : format).toString(); //use Commander for Tiny Leaders
            Iterable<String> legendNames = CardRelationMatrixGenerator.cardPools.get(matrixKey).keySet();
            for (String legendName : legendNames) {
                uniqueCards.add(FModel.getMagicDb().getCommonCards().getUniqueByName(legendName));
            }
        }
        else {
            uniqueCards = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getUniqueCards(), PaperCard.class);
        }
        Predicate<CardRules> canPlay = isForAi ? DeckGeneratorBase.AI_CAN_PLAY : DeckGeneratorBase.HUMAN_CAN_PLAY;
        @SuppressWarnings("unchecked")
        Iterable<PaperCard> legends = Iterables.filter(uniqueCards.toFlatList(), Predicates.and(format.isLegalCommanderPredicate(), Predicates.compose(
                    canPlay, PaperCard.FN_GET_RULES)));
        final List<DeckProxy> decks = new ArrayList<>();
        for (PaperCard legend: legends) {
            decks.add(new CommanderDeckGenerator(legend, format, isForAi, isCardGen));
        }
        return decks;
    }

    public static List<DeckProxy> getBrawlDecks(final DeckFormat format, boolean isForAi, boolean isCardGen){
        ItemPool uniqueCards;
        if (isCardGen){
            uniqueCards = new ItemPool<>(PaperCard.class);
            //TODO: update to actual Brawl model from real Brawl decks
            Iterable<String> legendNames=CardArchetypeLDAGenerator.ldaPools.get(FModel.getFormats().getStandard().getName()).keySet();
            for (String legendName : legendNames) {
                uniqueCards.add(FModel.getMagicDb().getCommonCards().getUniqueByName(legendName));
            }
        }
        else {
            uniqueCards = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getUniqueCards(), PaperCard.class);
        }
        Predicate<CardRules> canPlay = isForAi ? DeckGeneratorBase.AI_CAN_PLAY : DeckGeneratorBase.HUMAN_CAN_PLAY;
        @SuppressWarnings("unchecked")
        Iterable<PaperCard> legends = Iterables.filter(uniqueCards.toFlatList(), Predicates.and(format.isLegalCardPredicate(),
                Predicates.compose(Predicates.and(
                CardRulesPredicates.Presets.CAN_BE_BRAWL_COMMANDER,
                canPlay), PaperCard.FN_GET_RULES)));
        final List<DeckProxy> decks = new ArrayList<>();
        for (PaperCard legend: legends) {
            decks.add(new CommanderDeckGenerator(legend, format, isForAi, isCardGen));
        }
        return decks;
    }

    private final PaperCard legend;
    private final int index;
    private final DeckFormat format;
    private final boolean isForAi;
    private final boolean isCardgen;

    private CommanderDeckGenerator(PaperCard legend0, DeckFormat format0, boolean isForAi0, boolean isCardgen0) {
        super();
        legend = legend0;
        index = 0;
        isForAi=isForAi0;
        format=format0;
        isCardgen=isCardgen0;
    }

    public CardEdition getEdition() {
        return CardEdition.UNKNOWN;
    }

    @Override
    public String getName() {
        return legend.getName();
    }

    @Override
    public String toString() {
        return legend.getName();
    }

    @Override
    public int compareTo(final CommanderDeckGenerator d) {
        return this.getName().compareTo(d.getName());
    }

    @Override
    public Deck getDeck() {
        return DeckgenUtil.generateRandomCommanderDeck(legend, format,isForAi, isCardgen);
    }

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }

    public String getImageKey(boolean altState) {
        return legend.getImageKey(altState);
    }

    public PaperCard getPaperCard(){
        return legend;
    }
}
