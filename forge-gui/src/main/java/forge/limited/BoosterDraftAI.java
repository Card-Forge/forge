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
package forge.limited;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.properties.ForgePreferences;
import forge.util.Aggregates;

/**
 * <p>
 * BoosterDraftAI class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class BoosterDraftAI {

    /** The bd. */
    private IBoosterDraft bd = null;

    /**
     * Constant <code>nDecks=7.</code>
     */
    protected static final int N_DECKS = 7;

    // holds all the cards for each of the computer's decks
    protected final List<List<PaperCard>> decks = new ArrayList<List<PaperCard>>();
    protected final List<DeckColors> playerColors = new ArrayList<DeckColors>();
    protected CardRanker ranker = new CardRanker();

    /**
     * <p>
     * Choose a CardPrinted from the list given.
     * </p>
     *
     * @param chooseFrom
     *            List of CardPrinted
     * @param player
     *            a int.
     * @return a {@link forge.item.PaperCard} object.
     */
    public PaperCard choose(final List<PaperCard> chooseFrom, final int player) {
        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + player + "] pack: " + chooseFrom.toString());
        }

        final List<PaperCard> deck = decks.get(player);
        final DeckColors deckCols = this.playerColors.get(player);
        final ColorSet chosenColors = deckCols.getChosenColors();
        final boolean canAddMoreColors = deckCols.canChoseMoreColors();

        List<PaperCard> rankedCards = ranker.rankCardsInPack(chooseFrom, deck, chosenColors, canAddMoreColors);
        PaperCard bestPick = rankedCards.get(0);

        if (canAddMoreColors) {
            deckCols.addColorsOf(bestPick);
        }

        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + player + "] picked: " + bestPick);
        }
        this.decks.get(player).add(bestPick);

        return bestPick;
    }

    /**
     * <p>
     * getDecks.
     * </p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    public Deck[] getDecks() {
        final Deck[] out = new Deck[this.decks.size()];

        for (int i = 0; i < this.decks.size(); i++) {
            if (ForgePreferences.DEV_MODE) {
                System.out.println("Deck[" + i + "]");
            }

            out[i] = new BoosterDeckBuilder(this.decks.get(i), this.playerColors.get(i)).buildDeck();
        }
        return out;
    } // getDecks()

    /**
     * <p>
     * Constructor for BoosterDraftAI.
     * </p>
     */
    public BoosterDraftAI() {
        // Initialize deck array and playerColors list
        for (int i = 0; i < N_DECKS; i++) {
            this.decks.add(new ArrayList<PaperCard>());
            this.playerColors.add(new DeckColors());
        }
    } // BoosterDraftAI()

    /**
     * Gets the bd.
     *
     * @return the bd
     */
    public IBoosterDraft getBd() {
        return this.bd;
    }

    /**
     * Sets the bd.
     *
     * @param bd0
     *            the bd to set
     */
    public void setBd(final IBoosterDraft bd0) {
        this.bd = bd0;
    }

} // BoosterDraftAI()

