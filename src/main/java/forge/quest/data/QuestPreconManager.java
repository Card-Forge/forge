/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.quest.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import forge.deck.DeckIO;
import forge.item.PreconDeck;

/**
 * Very simple function - store all precons.
 * 
 */
public class QuestPreconManager {

    /** The decks. */
    private final List<PreconDeck> decks = new ArrayList<PreconDeck>();

    /**
     * Instantiates a new quest precon manager.
     *
     * @param deckDir the deck dir
     */
    public QuestPreconManager(final File deckDir) {
        final List<String> decksThatFailedToLoad = new ArrayList<String>();
        final File[] files = deckDir.listFiles(DeckIO.DCK_FILE_FILTER);
        for (final File file : files) {
            try {
                this.decks.add(new PreconDeck(file));
            } catch (final NoSuchElementException ex) {
                final String message = String.format("%s failed to load because ---- %s", file.getName(),
                        ex.getMessage());
                decksThatFailedToLoad.add(message);
            }
        }

        if (!decksThatFailedToLoad.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    StringUtils.join(decksThatFailedToLoad, System.getProperty("line.separator")),
                    "Some of your decks were not loaded.", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @param q the q
     * @return the decks for current
     */
    public List<PreconDeck> getDecksForCurrent(final QuestData q) {
        final List<PreconDeck> meetRequirements = new ArrayList<PreconDeck>();
        for (final PreconDeck deck : this.decks) {
            if (deck.getRecommendedDeals().meetsRequiremnts(q)) {
                meetRequirements.add(deck);
            }
        }
        return meetRequirements;
    }

    /**
     * Gets the decks.
     *
     * @return the decks
     */
    public final List<PreconDeck> getDecks() {
        return this.decks;
    }

}
