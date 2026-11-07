/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.deck;

import java.util.Map;
import java.util.function.Predicate;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;

/**
 * {@code DeckRule:Copies:Limit$ <Unlimited|n> | Affected$ <branches>} - overrides the format's
 * max-copies limit for matching cards. Branch grammar: {@link CardRulesPredicates#restrictionBranch}
 * (comma-separated, OR'd), typically {@code namedCARDNAME} for this rule's own bearing card.
 */
public class DeckRuleCopies extends DeckRule {
    private static final String UNLIMITED = "Unlimited";
    private static final String SELF_NAME = "CARDNAME";

    private final Predicate<CardRules> affectedPredicate;
    private final boolean unlimited;
    private final int limit;

    DeckRuleCopies(final Map<String, String> params, final String ownerName) {
        super(params);
        affectedPredicate = parseAffected(params.get("Affected"), ownerName);
        final String limitParam = params.get("Limit");
        unlimited = UNLIMITED.equalsIgnoreCase(limitParam);
        limit = !unlimited && limitParam != null ? Integer.parseInt(limitParam.trim()) : 0;
    }

    /** Resolves each branch's own CARDNAME before parsing it, so a comma in the resolved name (e.g. "Vazal, the Compleat") can't be mistaken for another branch. */
    private static Predicate<CardRules> parseAffected(final String rawValue, final String ownerName) {
        if (rawValue == null) {
            return card -> false;
        }
        Predicate<CardRules> result = null;
        for (final String rawBranch : rawValue.split(",")) {
            final String trimmed = rawBranch.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final Predicate<CardRules> branch = CardRulesPredicates.restrictionBranch(trimmed.replace(SELF_NAME, ownerName));
            result = result == null ? branch : result.or(branch);
        }
        return result == null ? card -> false : result;
    }

    /** The max copies of {@code candidate} this rule allows, or null if it doesn't match {@code Affected$}. */
    public Integer getLimit(final CardRules candidate) {
        if (!affectedPredicate.test(candidate)) {
            return null;
        }
        return unlimited ? Integer.MAX_VALUE : limit;
    }
}
