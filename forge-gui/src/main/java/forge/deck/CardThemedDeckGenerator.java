package forge.deck;

import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maustin on 09/05/2017.
 */
public class CardThemedDeckGenerator extends GeneratedDeckProxy implements Comparable<CardThemedDeckGenerator> {
    public static List<DeckProxy> getMatrixDecks(GameFormat format, boolean isForAi){
        final List<DeckProxy> decks = new ArrayList<>();
            for (String card: CardArchetypeLDAGenerator.ldaPools.get(format.getName()).keySet()) {
                //exclude non AI playables as keycards for AI decks
                if (isForAi&&FModel.getMagicDb().getCommonCards().getRules(card, true).getAiHints().getRemAIDecks()) {
                    continue;
                }
                decks.add(new CardThemedDeckGenerator(card, format, isForAi));
            }

        return decks;
    }
    private final GameFormat format;
    private final boolean isForAi;


    private CardThemedDeckGenerator(String cardName, GameFormat format0, boolean isForAi0) {
        super(cardName, 60);
        format=format0;
        isForAi=isForAi0;
    }

    @Override
    public int compareTo(final CardThemedDeckGenerator d) {
        return getName().compareTo(d.getName());
    }

    @Override
    public Deck getDeck() {
        return DeckgenUtil.buildCardGenDeck(getName(),format,isForAi);
    }

    public String getImageKey(boolean altState) {
/*        Predicate<PaperCard> cardFilter = Predicates.and(format.getFilterPrinted(),PaperCard.Predicates.name(getName()));
        List<PaperCard> cards=FModel.getMagicDb().getCommonCards().getAllCards(cardFilter);
        return cards.get(cards.size()-1).getImageKey(altState);*/
        return FModel.getMagicDb().getCommonCards().getUniqueByName(getName()).getImageKey(altState);
    }

    public PaperCard getPaperCard(){
        return FModel.getMagicDb().getCommonCards().getUniqueByName(getName());
    }
}
