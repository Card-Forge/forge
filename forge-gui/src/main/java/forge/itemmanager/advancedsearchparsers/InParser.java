package forge.itemmanager.advancedsearchparsers;

import java.util.function.Predicate;

import forge.card.CardRarity;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;

public abstract class InParser {
    /**
     * Handles both if a card was printed in a set, or in a rarity.
     * @param tokenValue Token value
     * @return Predicate
     */
    public static Predicate<PaperCard> handle(String tokenValue) {
        Predicate<PaperCard> predicate = null;
        CardRarity rarity = RarityParser.ParseRarityFromStr(tokenValue);

        if (rarity == null) {
             // Not a rarity. Assume it is a set
            predicate = PaperCardPredicates.printedInSet(tokenValue);
        } else {
            predicate = PaperCardPredicates.printedWithRarity(rarity);
        }
        
        return predicate;
    }
}
