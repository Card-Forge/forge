package forge.limited;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.card.*;
import forge.item.PaperCard;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CardRanker {

    private static final double SCORE_UNPICKABLE = -100.0;

    /**
     * Rank cards.
     *
     * @param cards PaperCards to rank
     * @return List of beans with card rankings
     */
    public List<PaperCard> rankCardsInDeck(final Iterable<PaperCard> cards) {
        List<Pair<Double, PaperCard>> cardScores = getScores(cards);

        return sortAndCreateList(cardScores);
    }

    public List<PaperCard> rankCardsInPack(
            final Iterable<PaperCard> cardsInPack,
            final List<PaperCard> deck,
            ColorSet chosenColors,
            boolean canAddMoreColors
    ) {
        List<Pair<Double, PaperCard>> cardScores = getScoresForPack(cardsInPack, deck, chosenColors, canAddMoreColors);

        return sortAndCreateList(cardScores);
    }

    private List<Pair<Double, PaperCard>> getScores(Iterable<PaperCard> cards) {
        List<Pair<Double, PaperCard>> cardScores = new ArrayList<>();

        List<PaperCard> cache = new ArrayList<>();
        for (PaperCard card : cards) {
            cache.add(card);
        }

        for (int i = 0; i < cache.size(); i++) {
            final PaperCard card = cache.get(i);

            double score = getRawScore(card);
            if (card.getRules().getAiHints().getRemAIDecks()) {
                score -= 20.0;
            }

            List<PaperCard> otherCards = getCardsExceptOne(cache, i);
            score += calculateSynergies(card, otherCards);

            cardScores.add(Pair.of(score, card));
        }

        return cardScores;
    }

    private List<Pair<Double, PaperCard>> getScoresForPack(
            Iterable<PaperCard> cardsInPack,
            List<PaperCard> deck,
            ColorSet chosenColors,
            boolean canAddMoreColors
    ) {
        List<Pair<Double, PaperCard>> cardScores = new ArrayList<>();

        for (PaperCard card : cardsInPack) {
            double score = getRawScore(card);
            if (card.getRules().getAiHints().getRemAIDecks()) {
                score -= 20.0;
            }
            if( !canAddMoreColors && !card.getRules().getManaCost().canBePaidWithAvaliable(chosenColors.getColor())) {
                score -= 50.0;
            }

            score += calculateSynergies(card, deck);

            cardScores.add(Pair.of(score, card));
        }

        return cardScores;
    }

    private double getRawScore(PaperCard card) {
        Double rawScore;
        if (MagicColor.Constant.BASIC_LANDS.contains(card.getName())) {
            rawScore = SCORE_UNPICKABLE;
        } else {
            Double rkg;
            String customRankings = IBoosterDraft.CUSTOM_RANKINGS_FILE[0];
            if (customRankings != null) {
                rkg = DraftRankCache.getCustomRanking(customRankings, card.getName());
                if (rkg == null) {
                    // try the default rankings if custom rankings contain no entry
                    rkg = DraftRankCache.getRanking(card.getName(), card.getEdition());
                }
            } else {
                rkg = DraftRankCache.getRanking(card.getName(), card.getEdition());
            }

            if (rkg != null) {
                rawScore = 100 - (100 * rkg);
            } else {
                rawScore = SCORE_UNPICKABLE;
            }
        }
        return rawScore;
    }

    private List<PaperCard> getCardsExceptOne(List<PaperCard> cache, int i) {
        List<PaperCard> otherCards = new ArrayList<>();
        otherCards.addAll(cache.subList(0, i));
        if (i + 1 < cache.size()) {
            otherCards.addAll(cache.subList(i + 1, cache.size()));
        }
        return otherCards;
    }

    private double calculateSynergies(PaperCard card, Iterable<PaperCard> otherCards) {
        double synergyScore = 0.0;

        synergyScore += getScoreForDeckHints(card, otherCards);

        return synergyScore;
    }

    private double getScoreForDeckHints(PaperCard card, Iterable<PaperCard> otherCards) {
        double score = 0.0;
        final DeckHints hints = card.getRules().getAiHints().getDeckHints();
        if (hints != null && hints.isValid()) {
            final List<PaperCard> comboCards = hints.filter(otherCards);
            if (comboCards.size() > 0) {
                score = comboCards.size() * 10;
            }
        }
        return score;
    }

    private List<PaperCard> sortAndCreateList(List<Pair<Double, PaperCard>> cardScores) {
        Collections.sort(cardScores, Collections.reverseOrder(new CardRankingComparator()));

        List<PaperCard> rankedCards = new ArrayList<>(cardScores.size());
        for (Pair<Double, PaperCard> pair : cardScores) {
            rankedCards.add(pair.getValue());
        }

        return rankedCards;
    }
}
