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
        switch (tokenValue) {
            case "c":
            case "common":
                predicate = PaperCardPredicates.printedWithRarity(CardRarity.Common)
                    .or(PaperCardPredicates.printedWithRarity(CardRarity.BasicLand));
                break;

            case "u":
            case "uncommon":
                predicate = PaperCardPredicates.printedWithRarity(CardRarity.Uncommon);
                break;

            case "r":
            case "rare":
                predicate = PaperCardPredicates.printedWithRarity(CardRarity.Rare);
                break;

            case "m":
            case "mythic":
                predicate = PaperCardPredicates.printedWithRarity(CardRarity.MythicRare);
                break;

            case "s":
            case "special":
                predicate = PaperCardPredicates.printedWithRarity(CardRarity.Special);
                break;

            // Not a rarity. Assume it is a set
            default:
                predicate = c -> {
                    CardEdition e = StaticData.instance().getEditions().get(tokenValue);
                    
                    if (e == null) {
                        return false;
                    }

                    return !e.getCardInSet(c.getName()).isEmpty();
                };
        }
        
        return predicate;
    }
}
