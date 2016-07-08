package forge.limited;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.CardRulesPredicates;
import forge.card.DeckHints;
import forge.item.PaperCard;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.print.Paper;
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
    public List<Pair<Double, PaperCard>> rankCards(final Iterable<PaperCard> cards) {
        final List<Pair<Double, PaperCard>> ranked = new ArrayList<Pair<Double, PaperCard>>();

        getInitialRankings(cards, ranked);

        Collections.sort(ranked, Collections.reverseOrder(new CardRankingComparator()));
        return ranked;
    }

    private void getInitialRankings(Iterable<PaperCard> cards, List<Pair<Double, PaperCard>> ranked) {
        List<PaperCard> cache = new ArrayList<>();
        for (PaperCard card : cards) {
            cache.add(card);
        }

        String customRankings = IBoosterDraft.CUSTOM_RANKINGS_FILE[0];
        for (int i = 0; i < cache.size(); i++) {
            final PaperCard card = cache.get(i);

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

            double score = rawScore;

            List<PaperCard> otherCards = new ArrayList<>();
            otherCards.addAll(cache.subList(0, i));
            if (i + 1 < cache.size()) {
                otherCards.addAll(cache.subList(i + 1, cache.size()));
            }

            score += calculateSynergies(card, otherCards);

            ranked.add(Pair.of(score, card));
        }
    }

    private double calculateSynergies(PaperCard card, Iterable<PaperCard> otherCards) {
        double synergyScore = 0.0;

        synergyScore += getScoreForDeckHints(card, otherCards);
        synergyScore += getScoreForBuffedBy(card, otherCards);

        return synergyScore;
    }

    private double getScoreForBuffedBy(PaperCard card, Iterable<PaperCard> otherCards) {
        double matchBuffScore = 0.0;
        Iterable<Map.Entry<String, String>> vars = card.getRules().getMainPart().getVariables();
        for (Map.Entry<String, String> var : vars) {
            if (var.getKey().equals("BuffedBy")) {
                String buff = var.getValue();
                final Iterable<PaperCard> buffers = Iterables.filter(otherCards,
                        Predicates.compose(CardRulesPredicates.subType(buff), PaperCard.FN_GET_RULES));
                System.out.println(Iterables.size(buffers));
                matchBuffScore = Iterables.size(buffers) * 5;
            }
        }
        return matchBuffScore;
    }

    private double getScoreForDeckHints(PaperCard card, Iterable<PaperCard> otherCards) {
        double score = 0.0;
        final DeckHints hints = card.getRules().getAiHints().getDeckHints();
        if (hints != null && hints.getType() != DeckHints.Type.NONE) {
            final List<PaperCard> comboCards = hints.filter(otherCards);
            score = comboCards.size() * 3;
        }
        return score;
    }
}
