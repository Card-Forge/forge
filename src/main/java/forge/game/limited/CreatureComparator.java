package forge.game.limited;

import java.util.Comparator;

import forge.Card;
import forge.card.cardfactory.CardFactoryUtil;

/**
 * Sorts creatures, best first.
 * 
 */
public class CreatureComparator implements Comparator<Card> {
    @Override
    public int compare(final Card a, final Card b) {
        int cmcA = a.getCMC();
        cmcA *= 30; // average creature from evaluateCreature is about 30 * CMC

        int cmcB = b.getCMC();
        cmcB *= 30;

        // evaluateCreature starts at 100
        int evalA = CardFactoryUtil.evaluateCreature(a) - 100;
        int evalB = CardFactoryUtil.evaluateCreature(b) - 100;

        int rarA = 0;
        int rarB = 0;

        if (a.getCurSetRarity().equals("Common")) {
            rarA = 1;
        } else if (a.getCurSetRarity().equals("Uncommon")) {
            rarA = 2;
        } else if (a.getCurSetRarity().equals("Rare")) {
            rarA = 4;
        } else if (a.getCurSetRarity().equals("Mythic")) {
            rarA = 8;
        }

        if (b.getCurSetRarity().equals("Common")) {
            rarB = 1;
        } else if (b.getCurSetRarity().equals("Uncommon")) {
            rarB = 2;
        } else if (b.getCurSetRarity().equals("Rare")) {
            rarB = 4;
        } else if (b.getCurSetRarity().equals("Mythic")) {
            rarB = 8;
        }

        final int scoreA = evalA - cmcA + rarA;
        final int scoreB = evalB - cmcB + rarB;

        return scoreB - scoreA;
    }
}
