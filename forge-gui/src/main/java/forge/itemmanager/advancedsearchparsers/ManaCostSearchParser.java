package forge.itemmanager.advancedsearchparsers;

import java.util.function.Predicate;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.mana.ManaCost;

/**
 * Handles {@code m:} / {@code mana:} Advanced Search tokens for mana cost symbols.
 */
public abstract class ManaCostSearchParser {

    /**
     * @param opUsed   operator from the search token ({@code :}, {@code =}, {@code >}, …)
     * @param valueStr cost symbol string (may be lowercased by the caller)
     * @return predicate matching cards by mana cost; {@code card -> false} if unparseable
     */
    public static Predicate<CardRules> handle(final String opUsed, final String valueStr) {
        final ManaCost query = ManaCost.parseCostSymbols(valueStr);
        if (query == null) {
            return card -> false;
        }
        final Predicate<CardRules> predicate = CardRulesPredicates.manaCost(opUsed, query);
        return predicate != null ? predicate : card -> false;
    }
}
