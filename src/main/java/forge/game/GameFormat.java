package forge.game;

import java.util.Collections;
import java.util.List;

import net.slightlymagic.maxmtg.Predicate;

import forge.card.CardRules;
import forge.item.CardPrinted;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public final class GameFormat {
    
    private final String name;
    // contains allowed sets, when empty allows all sets
    private final List<String> allowedSetCodes; 
    private final List<String> bannedCardNames; 
    
    private final Predicate<CardPrinted> filterRules;
    private final Predicate<CardPrinted> filterPrinted;
    
    
    public GameFormat(final String fName, final List<String> sets, final List<String> bannedCards)
    {
        name = fName;
        allowedSetCodes = Collections.unmodifiableList(sets);
        bannedCardNames = Collections.unmodifiableList(bannedCards);
        filterRules = buildFilterRules();
        filterPrinted = buildFilterPritned();
    }
    

    private Predicate<CardPrinted> buildFilterPritned() {
        Predicate<CardPrinted> banNames = CardPrinted.Predicates.namesExcept(bannedCardNames);
        Predicate<CardPrinted> allowSets = allowedSetCodes == null || allowedSetCodes.isEmpty() 
            ? CardPrinted.Predicates.Presets.isTrue
            : CardPrinted.Predicates.printedInSets(allowedSetCodes, true); 
        return Predicate.and(banNames, allowSets);
    }


    private Predicate<CardPrinted> buildFilterRules() {
        Predicate<CardPrinted> banNames = CardPrinted.Predicates.namesExcept(bannedCardNames);
        Predicate<CardPrinted> allowSets = allowedSetCodes == null || allowedSetCodes.isEmpty() 
            ? CardPrinted.Predicates.Presets.isTrue
            : Predicate.brigde(CardRules.Predicates.wasPrintedInSets(allowedSetCodes), CardPrinted.fnGetRules); 
        return Predicate.and(banNames, allowSets);
    }

    public String getName() { return name; } 
    public Predicate<CardPrinted> getFilterRules() { return filterRules; }
    public Predicate<CardPrinted> getFilterPrinted() { return filterPrinted; }
    
    @Override
    public String toString()
    {
        return name + " (format)";
    }
    
}
