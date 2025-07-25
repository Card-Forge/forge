package forge.itemmanager.advancedsearchparsers;

import java.util.function.Predicate;

import forge.StaticData;
import forge.card.CardEdition;
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
            predicate = c -> {
                    CardEdition e = StaticData.instance().getEditions().get(tokenValue);
                    
                    if (e == null) {
                        return false;
                    }

                    return e.isCardObtainable(c.getName());
                };
        } else {
            predicate = PaperCardPredicates.printedWithRarity(rarity);
        }
        
        return predicate;
    }
}
