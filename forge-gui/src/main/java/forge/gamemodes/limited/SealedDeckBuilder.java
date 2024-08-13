package forge.gamemodes.limited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.item.PaperCard;
import forge.util.MyRandom;

/**
 * Deck builder for Sealed Deck Format.
 * 
 */
public class SealedDeckBuilder extends LimitedDeckBuilder {

    public SealedDeckBuilder(List<PaperCard> list) {
        super(list);
        this.setColors(chooseColors());
    }

    /**
     * Choose colors for the Sealed Deck.
     * 
     * @return DeckColors
     */
    private ColorSet chooseColors() {
        // choose colors based on top 33% of cards
        final List<PaperCard> colorChooserList = new ArrayList<>();
        // this is not exactly right, because the rankings here are taking into account deckhints
        // for the whole set of cards, when some of those cards could be in colors that won't
        // make it into the deck
        List<PaperCard> initialRanked = CardRanker.rankCardsInDeck(getAiPlayables());
        double limit = getAiPlayables().size() * .33;
        for (int i = 0; i < limit; i++) {
            PaperCard cp = initialRanked.get(i);
            colorChooserList.add(cp);
        }

        Iterable<CardRules> rules = Iterables.transform(colorChooserList, PaperCard::getRules);

        int white = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_WHITE));
        int blue = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_BLUE));
        int black = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_BLACK));
        int red = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_RED));
        int green = Iterables.size(Iterables.filter(rules, CardRulesPredicates.Presets.IS_GREEN));

        final int[] colorCounts = { white, blue, black, red, green };
        int[] countsCopy = Arrays.copyOf(colorCounts, 5);
        Arrays.sort(countsCopy);

        List<String> maxColors = new ArrayList<>();
        List<String> secondColors = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (countsCopy[4] == colorCounts[i]) {
                maxColors.add(MagicColor.Constant.ONLY_COLORS.get(i));
            } else if (countsCopy[3] == colorCounts[i]) {
                secondColors.add(MagicColor.Constant.ONLY_COLORS.get(i));
            }
        }

        String color1;
        String color2;
        if (maxColors.size() > 1) {
            int n = MyRandom.getRandom().nextInt(maxColors.size());
            color1 = maxColors.get(n);
            maxColors.remove(n);
            n = MyRandom.getRandom().nextInt(maxColors.size());
            color2 = maxColors.get(n);
        } else {
            color1 = maxColors.get(0);
            if (secondColors.size() > 1) {
                color2 = secondColors.get(MyRandom.getRandom().nextInt(secondColors.size()));
            } else {
                color2 = secondColors.get(0);
            }
        }

        if (logToConsole) {
            System.out.println("COLOR = " + color1);
            System.out.println("COLOR = " + color2);
        }
        return ColorSet.fromNames(color1, color2);
    }
}
