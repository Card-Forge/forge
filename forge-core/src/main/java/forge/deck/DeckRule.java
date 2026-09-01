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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import forge.card.CardRules;
import forge.item.PaperCard;

/**
 * A deckbuilding-legality rule from a card's own {@code DeckRule:} line, e.g.
 * {@code DeckRule:ColorIdentity:Exempt$ Type:Artifact.Creature | ActiveSection$ Commander}.
 */
public abstract class DeckRule {
    private final DeckSection activeSection;
    private final String description;

    protected DeckRule(final Map<String, String> params) {
        DeckSection parsed = null;
        final String sectionName = params.get("ActiveSection");
        if (sectionName != null) {
            try {
                parsed = DeckSection.valueOf(sectionName);
            } catch (IllegalArgumentException ignored) {
                // unrecognized section name - treat as always-active
            }
        }
        activeSection = parsed;
        description = params.getOrDefault("Description", "");
    }

    /** Null means this rule is always active, regardless of which section its bearing card sits in. */
    public final DeckSection getActiveSection() {
        return activeSection;
    }

    /** True if this rule should be applied given the section its bearing card currently occupies. */
    public final boolean isActiveFor(final DeckSection cardSection) {
        return activeSection == null || activeSection == cardSection;
    }

    /** The human-readable clause text, e.g. for display alongside the card's printed ability. */
    public final String getDescription() {
        return description;
    }

    /** Parses every {@code DeckRule:} line on the given card into typed rule objects (also picks up its marked colors for AllowedAdditionalColor$). */
    public static List<DeckRule> parseAll(final PaperCard card) {
        final CardRules rules = card.getRules();
        if (rules == null) {
            return new ArrayList<>();
        }
        final byte chosenAdditionalColors = card.getMarkedColors() != null ? card.getMarkedColors().getColor() : 0;
        return parseAll(rules.getDeckRules(), chosenAdditionalColors);
    }

    /** Parses a card face's raw {@code DeckRule:} line values (see {@link #parseAll(PaperCard)}). */
    public static List<DeckRule> parseAll(final Iterable<String> rawDeckRuleLines) {
        return parseAll(rawDeckRuleLines, (byte) 0);
    }

    private static List<DeckRule> parseAll(final Iterable<String> rawDeckRuleLines, final byte chosenAdditionalColors) {
        final List<DeckRule> result = new ArrayList<>();
        for (final String raw : rawDeckRuleLines) {
            final int colonPos = raw.indexOf(':');
            final String ruleClass = colonPos > 0 ? raw.substring(0, colonPos) : raw;
            final String rest = colonPos > 0 ? raw.substring(colonPos + 1) : "";
            final Map<String, String> params = parseParams(rest);
            switch (ruleClass) {
                case "ColorIdentity":
                    result.add(new DeckRuleColorIdentity(params, chosenAdditionalColors));
                    break;
                case "Size":
                    result.add(new DeckRuleSize(params));
                    break;
                default:
                    break; // unrecognized rule class - ignore
            }
        }
        return result;
    }

    /** Splits a {@code Key$ Value | Key2$ Value2} parameter string. */
    protected static Map<String, String> parseParams(final String rest) {
        final Map<String, String> params = new LinkedHashMap<>();
        for (final String piece : rest.split("\\|")) {
            final String trimmed = piece.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final int dollarPos = trimmed.indexOf('$');
            if (dollarPos < 0) {
                continue;
            }
            final String key = trimmed.substring(0, dollarPos).trim();
            final String value = trimmed.substring(dollarPos + 1).trim();
            params.put(key, value);
        }
        return params;
    }
}
