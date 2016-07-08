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
    protected final List<List<PaperCard>> deck = new ArrayList<List<PaperCard>>();
    protected final List<DeckColors> playerColors = new ArrayList<DeckColors>();

    // roughly equivalent to 25 ranks in a core set, or 15 ranks in a small set
    private static final double TAKE_BEST_THRESHOLD = 0.1;

    // rank worse than any other card available to draft
    private static final double RANK_UNPICKABLE = 999.0;

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

        final DeckColors deckCols = this.playerColors.get(player);
        final ColorSet currentChoice = deckCols.getChosenColors();
        final boolean canAddMoreColors = deckCols.canChoseMoreColors();

        final List<Pair<PaperCard, Double>> rankedCards = rankCards(chooseFrom, IBoosterDraft.CUSTOM_RANKINGS_FILE[0]);

        for (final Pair<PaperCard, Double> p : rankedCards) {
            double valueBoost = 0;

            // If a card is not ai playable, somewhat decrease its rating
            if( p.getKey().getRules().getAiHints().getRemAIDecks() ) {
                valueBoost = TAKE_BEST_THRESHOLD;
            }

            // if I cannot choose more colors, and the card cannot be played with chosen colors, decrease its rating.
            if( !canAddMoreColors && !p.getKey().getRules().getManaCost().canBePaidWithAvaliable(currentChoice.getColor())) {
                valueBoost = TAKE_BEST_THRESHOLD * 3;
            }

            if (valueBoost > 0) {
                p.setValue(p.getValue() + valueBoost);
                //System.out.println(p.getKey() + " is now " + p.getValue());
            }
        }

        double bestRanking = Double.MAX_VALUE;
        PaperCard bestPick = null;
        final List<PaperCard> possiblePick = new ArrayList<PaperCard>();
        for (final Pair<PaperCard, Double> p : rankedCards) {
            final double rating = p.getValue();
            if(rating <= bestRanking + .01) {
                if (rating < bestRanking) {
                    // found a better card start a new list
                    possiblePick.clear();
                    bestRanking = rating;
                }
                possiblePick.add(p.getKey());
            }
        }

        bestPick = Aggregates.random(possiblePick);

        if (canAddMoreColors) {
            deckCols.addColorsOf(bestPick);
        }

        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + player + "] picked: " + bestPick + " ranking of " + bestRanking);
        }
        this.deck.get(player).add(bestPick);

        return bestPick;
    }

    /**
     * Sort cards by rank. Note that if pack has cards from different editions,
     * they could have the same rank. Basic lands and unrecognised cards are
     * rated worse than all other possible picks.
     *
     * @param chooseFrom
     *            List of cards
     * @return map of rankings
     */
    private static List<Pair<PaperCard, Double>> rankCards(final Iterable<PaperCard> chooseFrom, String customRankings) {
        final List<Pair<PaperCard, Double>> rankedCards = new ArrayList<Pair<PaperCard,Double>>();
        for (final PaperCard card : chooseFrom) {
            Double rank;
            if (MagicColor.Constant.BASIC_LANDS.contains(card.getName())) {
                rank = RANK_UNPICKABLE;
            } else {
                if (customRankings != null) {
                    rank = DraftRankCache.getCustomRanking(customRankings, card.getName());
                    if (rank == null) {
                        // try the default draft rankings if there's no entry in the custom rankings file
                        rank = DraftRankCache.getRanking(card.getName(), card.getEdition());
                    }
                } else {
                    rank = DraftRankCache.getRanking(card.getName(), card.getEdition());
                }

                if (rank == null) {
                    if (ForgePreferences.DEV_MODE) {
                        System.out.println("Draft Rankings - Card Not Found: " + card.getName());
                    }
                    rank = RANK_UNPICKABLE;
                }
            }

            rankedCards.add(MutablePair.of(card, rank));
        }
        return rankedCards;
    }

    /**
     * <p>
     * getDecks.
     * </p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    public Deck[] getDecks() {
        final Deck[] out = new Deck[this.deck.size()];

        for (int i = 0; i < this.deck.size(); i++) {
            if (ForgePreferences.DEV_MODE) {
                System.out.println("Deck[" + i + "]");
            }

            out[i] = new BoosterDeckBuilder(this.deck.get(i), this.playerColors.get(i)).buildDeck();
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
            this.deck.add(new ArrayList<PaperCard>());
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

