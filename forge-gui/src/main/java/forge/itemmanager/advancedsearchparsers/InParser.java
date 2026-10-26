package forge.itemmanager.advancedsearchparsers;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardEdition.EditionEntry;
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

        Set<String> obtainableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (EditionEntry entry : edition.getObtainableCards()) {
            obtainableNames.add(entry.name());
        }
        return card -> obtainableNames.contains(card.getName());
    }
}
