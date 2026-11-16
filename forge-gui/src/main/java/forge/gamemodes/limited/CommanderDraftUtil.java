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
package forge.gamemodes.limited;

import forge.card.ColorSet;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;

/**
 * Shared utility methods for Commander Draft deck-building.
 *
 * <p>Contains helper predicates used by both the desktop and mobile UIs.</p>
 */
public final class CommanderDraftUtil {

    private static final String DECK_TAG = "CommanderDraft";

    private CommanderDraftUtil() {}

    public static void markCommanderDraftDeck(final Deck deck) {
        deck.getTags().add(DECK_TAG);
    }

    /** Also accepts untagged decks that have a commander, since older mobile saves carry no tag. */
    public static boolean isCommanderDraftDeck(final Deck deck) {
        return deck.getTags().contains(DECK_TAG) || deck.has(DeckSection.Commander);
    }

    /**
     * Return the combined color identity of all supplied commander cards.
     */
    public static ColorSet getCommanderColorIdentity(final Iterable<PaperCard> commanders) {
        byte mask = 0;
        for (PaperCard cmd : commanders) {
            mask |= cmd.getRules().getColorIdentity().getColor();
        }
        return ColorSet.fromMask(mask);
    }
}

