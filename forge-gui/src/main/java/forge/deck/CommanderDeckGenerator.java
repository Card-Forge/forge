package forge.deck;

import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.deck.generation.DeckGeneratorBase;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.model.FModel;
import forge.util.ItemPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by maustin on 09/05/2017.
 */
public class CommanderDeckGenerator extends DeckProxy implements Comparable<CommanderDeckGenerator> {
    public static List<DeckProxy> getCommanderDecks(final DeckFormat format, boolean isForAi, boolean isCardGen){
        if (format.equals(DeckFormat.Brawl)){
            return getBrawlDecks(format, isForAi, isCardGen);
        }
        ItemPool<PaperCard> uniqueCards;
        if (isCardGen){
            uniqueCards = new ItemPool<>(PaperCard.class);
            String matrixKey = (format.equals(DeckFormat.TinyLeaders) ? DeckFormat.Commander : format).toString(); //use Commander for Tiny Leaders
            HashMap<String, List<Map.Entry<PaperCard, Integer>>> matrixPool = CardRelationMatrixGenerator.cardPools.get(matrixKey);
            if (matrixPool != null) {
                Iterable<String> legendNames = matrixPool.keySet();
                for (String legendName : legendNames) {
                    uniqueCards.add(FModel.getMagicDb().getCommonCards().getUniqueByName(legendName));
                }
            }
        }
        else {
            uniqueCards = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getUniqueCards(), PaperCard.class);
        }
        Predicate<CardRules> canPlay = isForAi ? DeckGeneratorBase.AI_CAN_PLAY : CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS;
        return uniqueCards.toFlatList().stream()
                .filter(format.isLegalCommanderPredicate())
                .filter(PaperCardPredicates.fromRules(canPlay))
                .map(legend -> new CommanderDeckGenerator(legend, format, isForAi, isCardGen))
                .collect(Collectors.toList());
    }

    public static List<DeckProxy> getBrawlDecks(final DeckFormat format, boolean isForAi, boolean isCardGen){
        ItemPool<PaperCard> uniqueCards;
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
        Predicate<CardRules> canPlay = isForAi ? DeckGeneratorBase.AI_CAN_PLAY : CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS;
        return uniqueCards.toFlatList().stream()
                .filter(format.isLegalCardPredicate())
                .filter(PaperCardPredicates.fromRules(CardRulesPredicates.CAN_BE_BRAWL_COMMANDER.and(canPlay)))
                .map(legend -> new CommanderDeckGenerator(legend, format, isForAi, isCardGen))
                .collect(Collectors.toList());
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
