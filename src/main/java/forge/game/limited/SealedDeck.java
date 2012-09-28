package forge.game.limited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterables;

import forge.Constant;
import forge.Constant.Color;
import forge.card.CardColor;
import forge.card.CardRulesPredicates;
import forge.card.CardRules;
import forge.item.CardPrinted;
import forge.util.MyRandom;

/**
 * Deck builder for Sealed Deck Format.
 * 
 */
public class SealedDeck extends LimitedDeck {

    /**
     * Constructor.
     * 
     * @param list
     *            list of cards available
     */
    public SealedDeck(List<CardPrinted> list) {
        super(list);
        this.setColors(chooseColors());
    }

    /**
     * Choose colors for the Sealed Deck.
     * 
     * @return DeckColors
     */
    private CardColor chooseColors() {
        List<Pair<Double, CardPrinted>> rankedCards = rankCards(getAiPlayables());

        // choose colors based on top 33% of cards
        final List<CardPrinted> colorChooserList = new ArrayList<CardPrinted>();
        double limit = rankedCards.size() * .33;
        for (int i = 0; i < limit; i++) {
            CardPrinted cp = rankedCards.get(i).getValue();
            colorChooserList.add(cp);
            System.out.println(cp.getName() + " " + cp.getCard().getManaCost().toString());
        }

        Iterable<CardRules> rules = Iterables.transform(colorChooserList, CardPrinted.FN_GET_RULES);
        
        int white = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_WHITE));
        int blue = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_BLUE));
        int black = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_BLACK));
        int red = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_RED));
        int green = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_GREEN));

        final int[] colorCounts = { white, blue, black, red, green };
        final String[] colors = Constant.Color.ONLY_COLORS;
        int[] countsCopy = Arrays.copyOf(colorCounts, 5);
        Arrays.sort(countsCopy);

        List<String> maxColors = new ArrayList<String>();
        List<String> secondColors = new ArrayList<String>();
        for (int i = 0; i < 5; i++) {
            if (countsCopy[4] == colorCounts[i]) {
                maxColors.add(colors[i]);
            } else if (countsCopy[3] == colorCounts[i]) {
                secondColors.add(colors[i]);
            }
        }

        String color1 = Color.GREEN;
        String color2 = Color.BLACK;
        final Random r = MyRandom.getRandom();
        if (maxColors.size() > 1) {
            int n = r.nextInt(maxColors.size());
            color1 = maxColors.get(n);
            maxColors.remove(n);
            n = r.nextInt(maxColors.size());
            color2 = maxColors.get(n);
        } else {
            color1 = maxColors.get(0);
            if (secondColors.size() > 1) {
                color2 = secondColors.get(r.nextInt(secondColors.size()));
            } else {
                color2 = secondColors.get(0);
            }
        }

        System.out.println("COLOR = " + color1);
        System.out.println("COLOR = " + color2);
        return CardColor.fromNames(color1, color2);
    }
}
