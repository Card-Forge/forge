package forge.ai;

import forge.card.CardRules;
import forge.card.CardType;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AIDeckStatistics {

    public float averageCMC = 0;
    // TODO implement this. Use a numerically stable algorithm from
    // https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Weighted_incremental_algorithm
    public float stddevCMC = 0;
    public int maxCost = 0;
    public int maxColoredCost = 0;

    // in WUBRGC order from ManaCost.getColorShardCounts()
    public int[] maxPips = null;
//    public int[] numSources = new int[6];
    public int numLands = 0;
    public AIDeckStatistics(float averageCMC, float stddevCMC, int maxCost, int maxColoredCost, int[] maxPips, int numLands) {
        this.averageCMC = averageCMC;
        this.stddevCMC = stddevCMC;
        this.maxCost = maxCost;
        this.maxColoredCost = maxColoredCost;
        this.maxPips = maxPips;
        this.numLands = numLands;
    }

    public static AIDeckStatistics fromCards(List<Card> cards) {
        int totalCMC = 0;
        int totalCount = 0;
        int numLands = 0;
        int maxCost = 0;
        int[] maxPips = new int[6];
        int maxColoredCost = 0;
        for (Card c : cards) {
            CardRules rules = c.getRules();
            if (rules == null) {
                System.err.println(c + " CardRules is null" + (c.isToken() ? "/token" : "."));
                continue;
            }
            CardType type = rules.getType();
            if (type.isLand()) {
                numLands += 1;
            } else {
                int cost = rules.getManaCost().getCMC();
                // TODO use alternate casting costs for this, free spells will usually be cast for free
                maxCost = Math.max(maxCost, cost);
                totalCMC += cost;
                totalCount++;
                int[] pips = rules.getManaCost().getColorShardCounts();
                int colored_pips = 0;
                for (int i = 0; i < pips.length; i++) {
                    maxPips[i] = Math.max(maxPips[i], pips[i]);
                    if (i < 5) {
                        colored_pips += pips[i];
                    }
                }
                maxColoredCost = Math.max(maxColoredCost, colored_pips);
            }

            // TODO implement the number of mana sources
            // find the sources
            // What about non-mana-ability mana sources?
            // fetchlands, ramp spells, etc

        }

        return new AIDeckStatistics(totalCount == 0 ? 0 : totalCMC / (float)totalCount,
                0, // TODO use https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
                maxCost,
                maxColoredCost,
                maxPips,
                numLands
                );
    }


    public static AIDeckStatistics fromDeck(Deck deck, Player player) {
        List<Card> cardlist = new ArrayList<>();
        for (final Map.Entry<DeckSection, CardPool> deckEntry : deck) {
            switch (deckEntry.getKey()) {
                case Main:
                case Commander:
                    for (final Map.Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                        Card card = Card.fromPaperCard(poolEntry.getKey(), player);
                        cardlist.add(card);
                    }
                    break;
                default:
                    break; //ignore other sections
            }
        }

        return fromCards(cardlist);
    }

    public static AIDeckStatistics fromPlayer(Player player) {
        Deck deck = player.getRegisteredPlayer().getDeck();
        if (deck.isEmpty()) {
            // we're in a test or some weird match, search through the hand and library and build the decklist
            List<Card> cardlist = new ArrayList<>();
            for (Card c : player.getAllCards()) {
                if (c.getPaperCard() == null) {
                    continue;
                }
                cardlist.add(c);
            }

            return fromCards(cardlist);
        }

        return fromDeck(deck, player);

    }

}
