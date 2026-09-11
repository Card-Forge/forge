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
import java.util.List;
import java.util.Map;

import forge.card.DeckRuleLine;
import forge.card.ICardFace;
import forge.item.IPaperCard;

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

    /** Assembles every {@code DeckRule:} line across every face of the given card into typed rule objects (also picks up its marked colors for AllowedAdditionalColor$, and each face's own name for Copies' CARDNAME resolution). The expensive per-line tokenizing is cached per {@code CardFace} (shared across every printing); call {@code card.getDeckRuleList()} instead of this directly. */
    public static List<DeckRule> parseAll(final IPaperCard card) {
        if (card.getRules() == null) {
            return new ArrayList<>();
        }
        final byte chosenAdditionalColors = card.getMarkedColors() != null ? card.getMarkedColors().getColor() : 0;
        final List<DeckRule> result = new ArrayList<>();
        for (final ICardFace face : card.getAllFaces()) {
            for (final DeckRuleLine line : face.getTokenizedDeckRules()) {
                switch (line.getRuleClass()) {
                    case "ColorIdentity":
                        result.add(new DeckRuleColorIdentity(line.getParams(), chosenAdditionalColors));
                        break;
                    case "Size":
                        result.add(new DeckRuleSize(line.getParams()));
                        break;
                    case "Copies":
                        result.add(new DeckRuleCopies(line.getParams(), face.getName()));
                        break;
                    default:
                        break; // unrecognized rule class - ignore
                }
            }
        }
        return result;
    }
}
