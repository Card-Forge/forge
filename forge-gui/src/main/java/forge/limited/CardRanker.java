package forge.limited;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.card.CardRulesPredicates;
import forge.card.DeckHints;
import forge.item.PaperCard;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CardRanker {

    /**
     * Rank cards.
     *
     * @param cards PaperCards to rank
     * @return List of beans with card rankings
     */
    public List<PaperCard> rankCards(final Iterable<PaperCard> cards) {
        List<Pair<Double, PaperCard>> cardScores = getScores(cards);

        Collections.sort(cardScores, Collections.reverseOrder(new CardRankingComparator()));

        List<PaperCard> rankedCards = new ArrayList<>(cardScores.size());
        for (Pair<Double, PaperCard> pair : cardScores) {
            System.out.println(pair.getKey().toString() + " " + pair.getValue().getName());
            rankedCards.add(pair.getValue());
        }

        return rankedCards;
    }

    private List<Pair<Double, PaperCard>> getScores(Iterable<PaperCard> cards) {
        List<Pair<Double, PaperCard>> cardScores = new ArrayList<>();

        List<PaperCard> cache = new ArrayList<>();
        for (PaperCard card : cards) {
            cache.add(card);
        }

        String customRankings = IBoosterDraft.CUSTOM_RANKINGS_FILE[0];
        for (int i = 0; i < cache.size(); i++) {
            final PaperCard card = cache.get(i);

            double score = getRawScore(card, customRankings);

            List<PaperCard> otherCards = getCardsExcept(cache, i);

            score += calculateSynergies(card, otherCards);

            cardScores.add(Pair.of(score, card));
        }

        return cardScores;
    }

    private double getRawScore(PaperCard card, String customRankings) {
        Double rkg;
        if (customRankings != null) {
            rkg = DraftRankCache.getCustomRanking(customRankings, card.getName());
            if (rkg == null) {
                // try the default rankings if custom rankings contain no entry
                rkg = DraftRankCache.getRanking(card.getName(), card.getEdition());
            }
        } else {
            rkg = DraftRankCache.getRanking(card.getName(), card.getEdition());
        }

        double rawScore;
        if (rkg != null) {
            rawScore = 100 - (100 * rkg);
        } else {
            rawScore = 0.0;
        }
        return rawScore;
    }

    private List<PaperCard> getCardsExcept(List<PaperCard> cache, int i) {
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
        synergyScore += getScoreForBuffedBy(card, otherCards);

        return synergyScore;
    }

    private double getScoreForDeckHints(PaperCard card, Iterable<PaperCard> otherCards) {
        double score = 0.0;
        final DeckHints hints = card.getRules().getAiHints().getDeckHints();
        if (hints != null && hints.getType() != DeckHints.Type.NONE) {
            final List<PaperCard> comboCards = hints.filter(otherCards);
            score = comboCards.size() * 10;
        }
        return score;
    }

    private double getScoreForBuffedBy(PaperCard card, Iterable<PaperCard> otherCards) {
        double matchBuffScore = 0.0;
        Iterable<Map.Entry<String, String>> vars = card.getRules().getMainPart().getVariables();
        for (Map.Entry<String, String> var : vars) {
            if (var.getKey().equals("BuffedBy")) {
                String buff = var.getValue();
                final Iterable<PaperCard> buffers = Iterables.filter(otherCards,
                        Predicates.compose(CardRulesPredicates.subType(buff), PaperCard.FN_GET_RULES));
                matchBuffScore = Iterables.size(buffers) * 3;
            }
        }
        return matchBuffScore;
    }
}
