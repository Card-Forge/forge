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
        CardRarity rarity = RarityParser.ParseRarityFromStr(tokenValue);
        if (rarity != null) {
            return PaperCardPredicates.printedWithRarity(rarity);
        }

        CardEdition edition = StaticData.instance().getEditions().get(tokenValue);
        if (edition == null) {
            return card -> false;
        }

        return card -> edition.getObtainableCards().stream().anyMatch(e -> e.name().equals(card.getName()));
    }
}
