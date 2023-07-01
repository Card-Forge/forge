package forge.deck;

import java.util.ArrayList;
import java.util.List;

import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Created by maustin on 09/05/2017.
 */
public class CardThemedDeckGenerator extends DeckProxy implements Comparable<CardThemedDeckGenerator> {
    public static List<DeckProxy> getMatrixDecks(GameFormat format, boolean isForAi){
        final List<DeckProxy> decks = new ArrayList<>();
            for (String card: CardArchetypeLDAGenerator.ldaPools.get(format.getName()).keySet()) {
                //exclude non AI playables as keycards for AI decks
                if (isForAi&&FModel.getMagicDb().getCommonCards().getUniqueByName(card).getRules().getAiHints().getRemAIDecks()) {
                    continue;
                }
                decks.add(new CardThemedDeckGenerator(card, format, isForAi));
            }

        return decks;
    }
    private final String name;
    private final int index;
    private final GameFormat format;
    private final boolean isForAi;


    private CardThemedDeckGenerator(String cardName, GameFormat format0, boolean isForAi0) {
        super();
        name = cardName;
        index = 0;
        format=format0;
        isForAi=isForAi0;
    }

    public CardEdition getEdition() {
        return CardEdition.UNKNOWN;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(final CardThemedDeckGenerator d) {
        return name.compareTo(d.getName());
    }

    @Override
    public Deck getDeck() {
        return DeckgenUtil.buildCardGenDeck(name,format,isForAi);
    }

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }

    public String getImageKey(boolean altState) {
/*        Predicate<PaperCard> cardFilter = Predicates.and(format.getFilterPrinted(),PaperCard.Predicates.name(name));
        List<PaperCard> cards=FModel.getMagicDb().getCommonCards().getAllCards(cardFilter);
        return cards.get(cards.size()-1).getImageKey(altState);*/
        return FModel.getMagicDb().getCommonCards().getUniqueByName(name).getImageKey(altState);
    }

    public PaperCard getPaperCard(){
        return FModel.getMagicDb().getCommonCards().getUniqueByName(name);
    }
}
