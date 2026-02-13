package forge.gamemodes.limited;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.ColorSet;
import forge.card.DeckHints;
import forge.card.MagicColor;
import forge.item.PaperCard;
import forge.model.FModel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CardRanker {

    private static final double SCORE_UNPICKABLE = -100.0;
    // These factors determine the amount of boost given to a card's ranking for each other
    // card that matches it's deckhints.
    private static final Map<DeckHints.Type, Integer> typeFactors = ImmutableMap.<DeckHints.Type, Integer>builder()
            .put(DeckHints.Type.ABILITY, 3)
            .put(DeckHints.Type.COLOR, 1)
            .put(DeckHints.Type.KEYWORD, 3)
            .put(DeckHints.Type.NAME, 10)
            .put(DeckHints.Type.TYPE, 3)
            .build();
    private static final Map<DeckHints.Type, Integer> typeThresholds = ImmutableMap.<DeckHints.Type, Integer>builder()
            .put(DeckHints.Type.ABILITY, 5)
            .put(DeckHints.Type.COLOR, 10)
            .put(DeckHints.Type.KEYWORD, 8)
            .put(DeckHints.Type.NAME, 2)
            .put(DeckHints.Type.TYPE, 8)
            .build();
    private static boolean logToConsole = false;

    /**
     * Rank cards.
     *
     * @param cards PaperCards to rank
     * @return List of ranked cards
     */
    public static List<PaperCard> rankCardsInDeck(final Iterable<PaperCard> cards) {
        List<Pair<Double, PaperCard>> cardScores = getScores(cards);

        return sortAndCreateList(cardScores);
    }

    /**
     * Rank cards in pack comparing to existing cards in deck.
     * @param cardsInPack PaperCards to rank
     * @param deck existing deck
     * @param chosenColors colors of deck
     * @param canAddMoreColors can deck add more colors
     * @return sorted List of ranked cards
     */
    public static List<PaperCard> rankCardsInPack(
            final Iterable<PaperCard> cardsInPack,
            final List<PaperCard> deck,
            ColorSet chosenColors,
            boolean canAddMoreColors
    ) {
        List<Pair<Double, PaperCard>> cardScores = getScoresForPack(cardsInPack, deck, chosenColors, canAddMoreColors);

        return sortAndCreateList(cardScores);
    }

    public static List<Pair<Double, PaperCard>> getScores(Iterable<PaperCard> cards) {
        List<Pair<Double, PaperCard>> cardScores = new ArrayList<>();

        List<PaperCard> cache = Lists.newArrayList(cards);

        for (int i = 0; i < cache.size(); i++) {
            final PaperCard card = cache.get(i);

            double score = getRawScore(card);
            if (card.getRules().getAiHints().getRemAIDecks()) {
                score -= 20.0;
            }

            List<PaperCard> otherCards = getCardsExceptOne(cache, i);
            score += getScoreForDeckHints(card, otherCards);

            cardScores.add(Pair.of(score, card));
        }

        return cardScores;
    }

    private static List<Pair<Double, PaperCard>> getScoresForPack(
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
            if (!canAddMoreColors && !card.getRules().getDeckbuildingColors().hasNoColorsExcept(chosenColors)) {
                score -= 50.0;
            }

            score += getScoreForDeckHints(card, deck);

            cardScores.add(Pair.of(score, card));
        }

        return cardScores;
    }

    public static List<PaperCard> getOrderedRawScores(List<PaperCard> cards) {
        List<Pair<Double, PaperCard>> cardScores = Lists.newArrayList();
        for(PaperCard card : cards) {
            cardScores.add(Pair.of(getRawScore(card), card));
        }

        cardScores.sort(Collections.reverseOrder(new CardRankingComparator()));

        List<PaperCard> rankedCards = new ArrayList<>(cardScores.size());
        for (Pair<Double, PaperCard> pair : cardScores) {
            rankedCards.add(pair.getValue());
        }

        return rankedCards;
    }

    public static double getRawScore(PaperCard card) {
        double rawScore;
        if (MagicColor.Constant.BASIC_LANDS.contains(card.getName())) {
            rawScore = SCORE_UNPICKABLE;
        } else {
            Double rkg;
            String customRankings = IBoosterDraft.CUSTOM_RANKINGS_FILE[0];
            if (customRankings != null) {
                rkg = DraftRankCache.getCustomRanking(customRankings, card.getName());
                if (rkg == null) {
                    // try the default rankings if custom rankings contain no entry, but penalize missing cards
                    rkg = DraftRankCache.getRanking(card.getName(), card.getEdition());
                    if (rkg != null) {
                        rkg = rkg + 1;
                    }
                }
                if (rkg != null) {
                    rkg = rkg / 2;
                }
            } else {
                rkg = DraftRankCache.getRanking(card.getName(), card.getEdition());
                if(rkg == null){
                    List<PaperCard> cardList = FModel.getMagicDb().getCommonCards().getAllCards(card.getName());
                    for(PaperCard currentCard : cardList){
                        rkg = DraftRankCache.getRanking(currentCard.getName(), currentCard.getEdition());
                        if(rkg != null){
                            break;
                        }
                    }
                }
            }

            // Convert to a score from 0-100 where higher is better.
            // Makes it easier to think about and do math with the scores.
            if (rkg != null) {
                rawScore = 100 - (100 * rkg);
            } else {
                rawScore = SCORE_UNPICKABLE;
            }
        }
        return rawScore;
    }

    private static List<PaperCard> getCardsExceptOne(List<PaperCard> cache, int i) {
        List<PaperCard> otherCards = new ArrayList<>(cache.subList(0, i));
        if (i + 1 < cache.size()) {
            otherCards.addAll(cache.subList(i + 1, cache.size()));
        }
        return otherCards;
    }

    private static double getScoreForDeckHints(PaperCard card, Iterable<PaperCard> otherCards) {
        double score = 0.0;

        List<PaperCard> toBeRanked = Lists.newArrayList(card);
        for (PaperCard other : otherCards) {
            final DeckHints hints = other.getRules().getAiHints().getDeckHints();
            if (hints != null && hints.isValid()) {
                final Map<DeckHints.Type, Iterable<PaperCard>> cardsByType = hints.filterByType(toBeRanked);
                for (DeckHints.Type type : cardsByType.keySet()) {
                    Iterable<PaperCard> cards = cardsByType.get(type);
                    score += Iterables.size(cards) * typeFactors.get(type);
                    if (logToConsole && Iterables.size(cards) > 0) {
                        System.out.println(" - " + card.getName() + ": Found " + Iterables.size(cards) + " cards for " + type);
                    }
                }
            }
        }

        final DeckHints needs = card.getRules().getAiHints().getDeckNeeds();
        if (needs != null && needs.isValid()) {
            final Map<DeckHints.Type, Iterable<PaperCard>> cardsByType = needs.filterByType(otherCards);
            for (DeckHints.Type type : cardsByType.keySet()) {
                Iterable<PaperCard> cards = cardsByType.get(type);
                score -= (Math.max(typeThresholds.get(type) - Iterables.size(cards), 0) / (double) typeThresholds.get(type)) * typeFactors.get(type);
                if (logToConsole && Iterables.size(cards) > 0) {
                    System.out.println(" - " + card.getName() + ": Found " + Iterables.size(cards) + " cards for " + type);
                }
            }
        }

        return score;
    }

    private static List<PaperCard> sortAndCreateList(List<Pair<Double, PaperCard>> cardScores) {
        // even if some cards might be assigned the same rank we don't need randomization here
        // as the limited variant is responsible for that during generation
        cardScores.sort(Collections.reverseOrder(new CardRankingComparator()));

        List<PaperCard> rankedCards = new ArrayList<>(cardScores.size());
        for (Pair<Double, PaperCard> pair : cardScores) {
            rankedCards.add(pair.getValue());
            if (logToConsole) {
                System.out.println(pair.getValue().getName() + "[" + pair.getKey().toString() + "]");
            }
        }

        return rankedCards;
    }
}
