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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;



import forge.error.ErrorViewer;
import forge.game.GameType;

//reads and writeDeck Deck objects
/**
 * <p>
 * DeckManager class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class DeckManager {
    private Map<String, Deck> deckMap;
    private Map<String, Deck[]> draftMap;

    /**
     * <p>
     * Constructor for DeckManager.
     * </p>
     * 
     * @param deckDir
     *            a {@link java.io.File} object.
     */
    public DeckManager(final File deckDir) {
        if (deckDir == null) {
            throw new IllegalArgumentException("No deck directory specified");
        }
        try {
            if (deckDir.isFile()) {
                throw new IOException("Not a directory");
            } else {
                deckDir.mkdirs();
                if (!deckDir.isDirectory()) {
                    throw new IOException("Directory can't be created");
                }
                this.deckMap = DeckIO.readAllDecks(deckDir);
                this.draftMap = DeckIO.readAllDraftDecks(deckDir);
            }
        } catch (final IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckManager : writeDeck() error, " + ex.getMessage());
        }
    }

    /**
     * <p>
     * isUnique.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isUnique(final String deckName) {
        return !this.deckMap.containsKey(deckName);
    }

    /**
     * <p>
     * isUniqueDraft.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isUniqueDraft(final String deckName) {
        return !this.draftMap.keySet().contains(deckName);
    }

    /**
     * <p>
     * getDeck.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public final Deck getDeck(final String deckName) {
        return this.deckMap.get(deckName);
    }

    /**
     * <p>
     * addDeck.
     * </p>
     * 
     * @param deck
     *            a {@link forge.deck.Deck} object.
     */
    public final void addDeck(final Deck deck) {
        if (deck.getDeckType().equals(GameType.Draft)) {
            throw new RuntimeException("DeckManager : addDeck() error, deck type is Draft");
        }

        this.deckMap.put(deck.getName(), deck);
    }

    /**
     * <p>
     * deleteDeck.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     */
    public final void deleteDeck(final String deckName) {
        this.deckMap.remove(deckName);
    }

    /**
     * <p>
     * getDraftDeck.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     * @return an array of {@link forge.deck.Deck} objects.
     */
    public final Deck[] getDraftDeck(final String deckName) {
        if (!this.draftMap.containsKey(deckName)) {
            throw new RuntimeException("DeckManager : getDraftDeck() error, deck name not found - " + deckName);
        }

        return this.draftMap.get(deckName);
    }

    /**
     * <p>
     * addDraftDeck.
     * </p>
     * 
     * @param deck
     *            an array of {@link forge.deck.Deck} objects.
     */
    public final void addDraftDeck(final Deck[] deck) {
        this.checkDraftDeck(deck);

        this.draftMap.put(deck[0].toString(), deck);
    }

    /**
     * <p>
     * deleteDraftDeck.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     */
    public final void deleteDraftDeck(final String deckName) {
        if (!this.draftMap.containsKey(deckName)) {
            throw new RuntimeException("DeckManager : deleteDraftDeck() error, deck name not found - " + deckName);
        }

        this.draftMap.remove(deckName);
    }

    /**
     * <p>
     * checkDraftDeck.
     * </p>
     * 
     * @param deck
     *            an array of {@link forge.deck.Deck} objects.
     */
    private void checkDraftDeck(final Deck[] deck) {
        if ((deck == null) || (deck.length != 8) || deck[0].getName().equals("")
                || (!deck[0].getDeckType().equals(GameType.Draft))) {
            throw new RuntimeException("DeckManager : checkDraftDeck() error, invalid deck");
        }
    }

    /**
     * 
     * Get Decks.
     * 
     * @return a Collection<Deck>
     */
    public final Collection<Deck> getDecks() {
        return this.deckMap.values();
    }

    /**
     * 
     * Get Constructed Decks.
     * 
     * @return a Collection<Deck>
     */
    public final Collection<Deck> getConstructedDecks() {
        final ArrayList<Deck> list = new ArrayList<Deck>();
        for (final Deck l : this.deckMap.values()) {
            if (l.getDeckType().equals(GameType.Constructed) && !l.isCustomPool()) {
                list.add(l);
            }
        }
        Collections.sort(list);
        return list;
    }

    /**
     * 
     * Get draft decks.
     * 
     * @return a Map<String, Deck[]>
     */
    public final Map<String, Deck[]> getDraftDecks() {
        return new HashMap<String, Deck[]>(this.draftMap);
    }

    /**
     * 
     * Get names of decks.
     * 
     * @param deckType
     *            a GameType
     * @return a ArrayList<String>
     */
    public final ArrayList<String> getDeckNames(final GameType deckType) {
        final ArrayList<String> list = new ArrayList<String>();

        // only get decks according to the OldGuiNewGame screen option
        if (deckType.equals(GameType.Draft)) {
            for (final String s : this.getDraftDecks().keySet()) {
                list.add(s);
            }
        } else {
            for (final Deck deck : this.getDecks()) {
                if (deckType.equals(deck.getDeckType())) {
                    list.add(deck.toString());
                }
            }
        }

        Collections.sort(list);
        return list;
    }
}
