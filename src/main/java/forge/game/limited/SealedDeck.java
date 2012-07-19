package forge.game.limited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import forge.CardList;
import forge.CardListUtil;
import forge.Constant;
import forge.Constant.Color;
import forge.util.MyRandom;

/**
 * Deck builder for Sealed Deck Format.
 * 
 */
public class SealedDeck extends LimitedDeck {

    private static final long serialVersionUID = 8707786521610288811L;

    /**
     * Constructor.
     * 
     * @param list
     *            list of cards available
     */
    public SealedDeck(CardList list) {
        super(list);
        this.setColors(chooseColors());
        buildDeck();
    }

    /**
     * Choose colors for the Sealed Deck.
     * 
     * @return DeckColors
     */
    private DeckColors chooseColors() {
        CardList creatures = getAiPlayables().getType("Creature");
        CardListUtil.sortByEvaluateCreature(creatures);

        // choose colors based on top 33% of creatures and planeswalkers
        final CardList colorChooserList = new CardList();
        colorChooserList.addAll(getAiPlayables().getType("Planeswalker"));
        for (int i = 0; i < (creatures.size() * .33); i++) {
            colorChooserList.add(creatures.get(i));
            System.out.println(creatures.get(i).toString() + " " + creatures.get(i).getManaCost().toString());
        }

        final int[] colorCounts = { 0, 0, 0, 0, 0 };
        final String[] colors = Constant.Color.ONLY_COLORS;
        for (int i = 0; i < colors.length; i++) {
            colorCounts[i] = colorChooserList.getColorByManaCost(colors[i]).size();
        }
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
            color1 = maxColors.get(r.nextInt(maxColors.size()));
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
        final DeckColors dcAI = new DeckColors();
        dcAI.setColor1(color1);
        dcAI.setColor2(color2);
        // dcAI.splash = colors[2];
        dcAI.setMana1(dcAI.colorToMana(color1));
        dcAI.setMana2(dcAI.colorToMana(color2));
        // dcAI.manaS = dcAI.colorToMana(colors[2]);

        return dcAI;
    }
}
