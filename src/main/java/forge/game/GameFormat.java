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

    /**
     * Instantiates a new game format.
     *
     * @param fName the f name
     * @param sets the sets
     * @param bannedCards the banned cards
     */
    public GameFormat(final String fName, final List<String> sets, final List<String> bannedCards) {
        this.name = fName;
        this.allowedSetCodes = Collections.unmodifiableList(sets);
        this.bannedCardNames = Collections.unmodifiableList(bannedCards);
        this.filterRules = this.buildFilterRules();
        this.filterPrinted = this.buildFilterPritned();
    }

    private Predicate<CardPrinted> buildFilterPritned() {
        final Predicate<CardPrinted> banNames = CardPrinted.Predicates.namesExcept(this.bannedCardNames);
        final Predicate<CardPrinted> allowSets = (this.allowedSetCodes == null) || this.allowedSetCodes.isEmpty() ? CardPrinted.Predicates.Presets.isTrue
                : CardPrinted.Predicates.printedInSets(this.allowedSetCodes, true);
        return Predicate.and(banNames, allowSets);
    }

    private Predicate<CardPrinted> buildFilterRules() {
        final Predicate<CardPrinted> banNames = CardPrinted.Predicates.namesExcept(this.bannedCardNames);
        final Predicate<CardPrinted> allowSets = (this.allowedSetCodes == null) || this.allowedSetCodes.isEmpty() ? CardPrinted.Predicates.Presets.isTrue
                : Predicate.brigde(CardRules.Predicates.wasPrintedInSets(this.allowedSetCodes), CardPrinted.fnGetRules);
        return Predicate.and(banNames, allowSets);
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the filter rules.
     *
     * @return the filter rules
     */
    public Predicate<CardPrinted> getFilterRules() {
        return this.filterRules;
    }

    /**
     * Gets the filter printed.
     *
     * @return the filter printed
     */
    public Predicate<CardPrinted> getFilterPrinted() {
        return this.filterPrinted;
    }

    /**
     * Checks if is sets the legal.
     *
     * @param setCode the set code
     * @return true, if is sets the legal
     */
    public boolean isSetLegal(final String setCode) {
        return this.allowedSetCodes.isEmpty() || this.allowedSetCodes.contains(setCode);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name + " (format)";
    }

}
