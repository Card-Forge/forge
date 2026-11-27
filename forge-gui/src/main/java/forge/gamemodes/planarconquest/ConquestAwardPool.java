package forge.gamemodes.planarconquest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import forge.item.PaperCard;

public class ConquestAwardPool {
    public final List<PaperCard> commons, uncommons, rares, mythics;
    private final Map<PaperCard, PaperCard> standsInFor;

    /** Builds the pool from the print the player is given for each card, or leaves the card out when that is null. */
    public static ConquestAwardPool of(Iterable<PaperCard> cards, UnaryOperator<PaperCard> toAwardable) {
        Set<PaperCard> awardable = new LinkedHashSet<>();
        Map<PaperCard, PaperCard> standsInFor = new HashMap<>();
        for (PaperCard card : cards) {
            PaperCard print = toAwardable.apply(card);
            if (print == null) {
                continue;
            }
            if (!print.equals(card)) {
                standsInFor.putIfAbsent(print, card);
            }
            awardable.add(print);
        }
        return new ConquestAwardPool(awardable, standsInFor);
    }

    /** Whether the player owns the card, or the excluded print it was given in place of. */
    public boolean isOwned(PaperCard card, ConquestData model) {
        return model.hasUnlockedCard(card) || standsInFor.containsKey(card) && model.hasUnlockedCard(standsInFor.get(card));
    }

    private ConquestAwardPool(Iterable<PaperCard> cards, Map<PaperCard, PaperCard> standsInFor0) {
        standsInFor = standsInFor0;
        commons = new ArrayList<>();
        uncommons = new ArrayList<>();
        rares = new ArrayList<>();
        mythics = new ArrayList<>();

        for (PaperCard c : cards) {
            switch (c.getRarity()) {
            case Common:
                commons.add(c);
                break;
            case Uncommon:
                uncommons.add(c);
                break;
            case Rare:
            case Special: //lump special cards in with rares for simplicity
                rares.add(c);
                break;
            case MythicRare:
                mythics.add(c);
                break;
            default:
                break;
            }
        }
    }
}