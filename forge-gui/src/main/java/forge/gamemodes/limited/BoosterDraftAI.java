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

import java.util.ArrayList;
import java.util.List;

import forge.card.ColorSet;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;

/**
 * <p>
 * BoosterDraftAI class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class BoosterDraftAI {

    // TODO When WinstonDraft gets related changes that BoosterDraft gets, this can be deleted
    private IBoosterDraft bd = null;
    protected static final int N_DECKS = 7;

    // holds all the cards for each of the computer's decks
    protected final List<List<PaperCard>> decks = new ArrayList<>();
    protected final List<DeckColors> playerColors = new ArrayList<>();

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

        List<PaperCard> rankedCards = CardRanker.rankCardsInPack(chooseFrom, deck, chosenColors, canAddMoreColors);
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

    public BoosterDraftAI() {
        // Initialize deck array and playerColors list
        for (int i = 0; i < N_DECKS; i++) {
            this.decks.add(new ArrayList<>());
            this.playerColors.add(new DeckColors());
        }
    } // BoosterDraftAI()

    public IBoosterDraft getBd() {
        return this.bd;
    }
    public void setBd(final IBoosterDraft bd0) {
        this.bd = bd0;
    }

} // BoosterDraftAI()
