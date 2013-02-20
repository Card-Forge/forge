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
package forge.game.limited;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import forge.Constant.Preferences;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.item.CardPrinted;
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
    private static final int N_DECKS = 7;

    // holds all the cards for each of the computer's decks
    private final List<List<CardPrinted>> deck = new ArrayList<List<CardPrinted>>();
    private final ArrayList<DeckColors> playerColors = new ArrayList<DeckColors>();

    private ReadDraftRankings draftRankings;
    // roughly equivalent to 75 ranks in a core set, or 50 ranks in a small set
    private static final double TAKE_BEST_THRESHOLD = 0.3;

    /**
     * <p>
     * Choose a CardPrinted from the list given.
     * </p>
     * 
     * @param chooseFrom
     *            List of CardPrinted
     * @param player
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public CardPrinted choose(final List<CardPrinted> chooseFrom, final int player) {
        if (Preferences.DEV_MODE) {
            System.out.println("Player[" + player + "] pack: " + chooseFrom.toString());
        }

        DeckColors deckCols = this.playerColors.get(player);
        ColorSet currentChoice = deckCols.getChosenColors();
        boolean canAddMoreColors = deckCols.canChoseMoreColors();
        
        List<Pair<CardPrinted, Double>> rankedCards = rankCards(chooseFrom);
        
        for(Pair<CardPrinted, Double> p : rankedCards) {
            // If a card is not ai playable, somewhat decrease its rating
            if( p.getKey().getRules().getAiHints().getRemAIDecks() )
                p.setValue(p.getValue() + TAKE_BEST_THRESHOLD);

            // if I cannot choose more colors, and the card cannot be played with chosen colors, decrease its rating.
            if( !canAddMoreColors && !p.getKey().getRules().getManaCost().canBePaidWithAvaliable(currentChoice))
                p.setValue(p.getValue() + 10);
        }

        int cntBestCards = 0;
        double bestRanking = Double.MAX_VALUE;
        CardPrinted bestPick = null;
        for(Pair<CardPrinted, Double> p : rankedCards) { 
            double rating = p.getValue();
            if( rating < bestRanking )
            {
                bestRanking = rating;
                bestPick = p.getKey();
                cntBestCards = 1;
            } else if ( rating == bestRanking ) {
                cntBestCards++;
            }
        }

        if (cntBestCards > 1) {
            final List<CardPrinted> possiblePick = new ArrayList<CardPrinted>();
            for(Pair<CardPrinted, Double> p : rankedCards) {
                if ( p.getValue() == bestRanking )
                    possiblePick.add(p.getKey());
            }
            bestPick = Aggregates.random(possiblePick);
        }

        if (canAddMoreColors)
            deckCols.addColorsOf(bestPick);
        
        System.out.println("Player[" + player + "] picked: " + bestPick);
        this.deck.get(player).add(bestPick);
        
        return bestPick;
    }

    /**
     * Sort cards by rank. Note that if pack has cards from different editions,
     * they could have the same rank. In that (hopefully rare) case, only one
     * will end up in the Map.
     * 
     * @param chooseFrom
     *            List of cards
     * @return map of rankings
     */
    private List<Pair<CardPrinted, Double>> rankCards(final Iterable<CardPrinted> chooseFrom) {
        List<Pair<CardPrinted, Double>> rankedCards = new ArrayList<Pair<CardPrinted,Double>>();
        for (CardPrinted card : chooseFrom) {
            Double rkg = draftRankings.getRanking(card.getName(), card.getEdition());
            if (rkg != null) {
                rankedCards.add(MutablePair.of(card, rkg));
            } else {
                System.out.println("Draft Rankings - Card Not Found: " + card.getName());
                rankedCards.add(MutablePair.of(card, 0.0));
            }
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
            if (Preferences.DEV_MODE) {
                System.out.println("Deck[" + i + "]");
            }

            out[i] = new BoosterDeck(this.deck.get(i), this.playerColors.get(i)).buildDeck();
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
            this.deck.add(new ArrayList<CardPrinted>());
            this.playerColors.add(new DeckColors());
        }

        // Initialize card rankings
        this.draftRankings = new ReadDraftRankings();
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

