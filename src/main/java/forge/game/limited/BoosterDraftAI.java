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
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Constant;
import forge.Constant.Preferences;
import forge.card.CardColor;
import forge.card.CardRulesPredicates;
import forge.card.CardRules;
import forge.deck.Deck;
import forge.deck.generate.GenerateDeckUtil;
import forge.item.CardPrinted;

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

        CardPrinted pickedCard = null;

        Predicate<CardPrinted> pred = Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, CardPrinted.FN_GET_RULES);
        Iterable<CardPrinted> aiPlayablesView = Iterables.filter(chooseFrom, pred );
        List<CardPrinted> aiPlayables = Lists.newArrayList(aiPlayablesView); 
        

        TreeMap<Double, CardPrinted> rankedCards = rankCards(chooseFrom);

        if (this.playerColors.get(player).getColor1().equals("none")
                && this.playerColors.get(player).getColor2().equals("none")) {
            // Generally the first pick of the draft, no colors selected yet.

            // Sort playable cards by rank
            TreeMap<Double, CardPrinted> rankedPlayableCards = rankCards(aiPlayables);

            pickedCard = pickCard(rankedCards, rankedPlayableCards);

            if (!pickedCard.getCard().getColor().isColorless() && aiPlayables.contains(pickedCard)) {
                CardColor color = pickedCard.getCard().getColor();
                if (color.isMonoColor()) {
                    this.playerColors.get(player).setColor1(color.toString());
                } else {
                    // Arbitrary ordering here...
                    if (color.hasWhite()) {
                        this.playerColors.get(player).setColor1(Constant.Color.WHITE);
                    }
                    if (color.hasBlue()) {
                        if (this.playerColors.get(player).getColor1().equals("none")) {
                            this.playerColors.get(player).setColor1(Constant.Color.BLUE);
                        } else {
                            this.playerColors.get(player).setColor2(Constant.Color.BLUE);
                        }
                    }
                    if (color.hasBlack()) {
                        if (this.playerColors.get(player).getColor1().equals("none")) {
                            this.playerColors.get(player).setColor1(Constant.Color.BLACK);
                        } else {
                            this.playerColors.get(player).setColor2(Constant.Color.BLACK);
                        }
                    }
                    if (color.hasRed()) {
                        if (this.playerColors.get(player).getColor1().equals("none")) {
                            this.playerColors.get(player).setColor1(Constant.Color.RED);
                        } else {
                            this.playerColors.get(player).setColor2(Constant.Color.RED);
                        }
                    }
                    if (color.hasGreen()) {
                        if (this.playerColors.get(player).getColor1().equals("none")) {
                            this.playerColors.get(player).setColor1(Constant.Color.GREEN);
                        } else {
                            this.playerColors.get(player).setColor2(Constant.Color.GREEN);
                        }
                    }
                }
                if (Preferences.DEV_MODE) {
                    System.out.println("Player[" + player + "] Color1: " + this.playerColors.get(player).getColor1());
                    if (!this.playerColors.get(player).getColor2().equals("none")) {
                        System.out.println("Player[" + player + "] Color2: "
                                + this.playerColors.get(player).getColor2());
                    }
                }
            }
        } else if (!this.playerColors.get(player).getColor1().equals("none")
                && this.playerColors.get(player).getColor2().equals("none")) {
            // Has already picked one color, but not the second.

            // Sort playable, on-color, or mono-colored, or colorless cards
            TreeMap<Double, CardPrinted> rankedPlayableCards = new TreeMap<Double, CardPrinted>();
            for (CardPrinted card : aiPlayables) {
                CardColor currentColor1 = CardColor.fromNames(this.playerColors.get(player).getColor1());
                CardColor color = card.getCard().getColor();
                if (color.isColorless() || color.sharesColorWith(currentColor1) || color.isMonoColor()) {
                    Double rkg = draftRankings.getRanking(card.getName(), card.getEdition());
                    if (rkg != null) {
                        rankedPlayableCards.put(rkg, card);
                    }
                }
            }

            pickedCard = pickCard(rankedCards, rankedPlayableCards);

            CardColor color = pickedCard.getCard().getColor();
            if (!color.isColorless() && aiPlayables.contains(pickedCard)) {
                CardColor currentColor1 = CardColor.fromNames(this.playerColors.get(player).getColor1());
                if (color.isMonoColor()) {
                    if (!color.sharesColorWith(currentColor1)) {
                        this.playerColors.get(player).setColor2(color.toString());
                    }
                } else {
                    // Arbitrary ordering...
                    if (color.hasWhite()) {
                        if (!currentColor1.isWhite()) {
                            this.playerColors.get(player).setColor2(Constant.Color.WHITE);
                        }
                    } else if (color.hasBlue()) {
                        if (!currentColor1.isBlue()) {
                            this.playerColors.get(player).setColor2(Constant.Color.BLUE);
                        }
                    } else if (color.hasBlack()) {
                        if (!currentColor1.isBlack()) {
                            this.playerColors.get(player).setColor2(Constant.Color.BLACK);
                        }
                    } else if (color.hasRed()) {
                        if (!currentColor1.isRed()) {
                            this.playerColors.get(player).setColor2(Constant.Color.RED);
                        }
                    } else if (color.hasGreen()) {
                        if (!currentColor1.isGreen()) {
                            this.playerColors.get(player).setColor2(Constant.Color.GREEN);
                        }
                    }
                }
                if (Preferences.DEV_MODE) {
                    System.out.println("Player[" + player + "] Color2: " + this.playerColors.get(player).getColor2());
                }
            }
        } else {
            // Has already picked both colors.
            DeckColors dckColors = this.playerColors.get(player);
            CardColor colors = CardColor.fromNames(dckColors.getColor1(), dckColors.getColor2());
            Predicate<CardRules> hasColor = Predicates.or(new GenerateDeckUtil.ContainsAllColorsFrom(colors),
                    GenerateDeckUtil.COLORLESS_CARDS);
            
            Iterable<CardPrinted> colorList = Iterables.filter(aiPlayables, Predicates.compose(hasColor, CardPrinted.FN_GET_RULES));

            // Sort playable, on-color cards by rank
            TreeMap<Double, CardPrinted> rankedPlayableCards = rankCards(colorList);

            pickedCard = pickCard(rankedCards, rankedPlayableCards);
        }

        if (pickedCard == null) {
            final Random r = new Random();
            pickedCard = chooseFrom.get(r.nextInt(chooseFrom.size()));
        }

        if (pickedCard != null) {
            chooseFrom.remove(pickedCard);
            this.deck.get(player).add(pickedCard);
        }

        return pickedCard;
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
    private TreeMap<Double, CardPrinted> rankCards(final Iterable<CardPrinted> chooseFrom) {
        TreeMap<Double, CardPrinted> rankedCards = new TreeMap<Double, CardPrinted>();
        for (CardPrinted card : chooseFrom) {
            Double rkg = draftRankings.getRanking(card.getName(), card.getEdition());
            if (rkg != null) {
                rankedCards.put(rkg, card);
            } else {
                System.out.println("Draft Rankings - Card Not Found: " + card.getName());
            }
        }
        return rankedCards;
    }

    /**
     * Pick a card.
     * 
     * @param rankedCards
     * @param rankedPlayableCards
     * @return CardPrinted
     */
    private CardPrinted pickCard(TreeMap<Double, CardPrinted> rankedCards,
            TreeMap<Double, CardPrinted> rankedPlayableCards) {
        CardPrinted pickedCard = null;
        Map.Entry<Double, CardPrinted> best = rankedCards.firstEntry();
        if (best != null) {
            if (rankedPlayableCards.containsValue(best.getValue())) {
                // If best card is playable, pick it.
                pickedCard = best.getValue();
                System.out.println("Chose Best: " + "[" + best.getKey() + "] " + pickedCard.getName() + " ("
                        + pickedCard.getCard().getManaCost() + ") " + pickedCard.getType().toString());
            } else {
                // If not, find the best card that is playable.
                Map.Entry<Double, CardPrinted> bestPlayable = rankedPlayableCards.firstEntry();
                if (bestPlayable == null) {
                    // Nothing is playable, so just take the best card.
                    pickedCard = best.getValue();
                    System.out.println("Nothing playable, chose Best: " + "[" + best.getKey() + "] "
                            + pickedCard.getName() + " (" + pickedCard.getCard().getManaCost() + ") "
                            + pickedCard.getType().toString());
                } else {
                    // If the best card is far better than the best playable,
                    // take the best. Otherwise, take the one that is playable.
                    if (best.getKey() + TAKE_BEST_THRESHOLD < bestPlayable.getKey()) {
                        pickedCard = best.getValue();
                        System.out.println("Best is much better than playable; chose Best: " + "[" + best.getKey()
                                + "] " + pickedCard.getName() + " (" + pickedCard.getCard().getManaCost() + ") "
                                + pickedCard.getType().toString());
                        System.out.println("Playable was: " + "[" + bestPlayable.getKey() + "] "
                                + bestPlayable.getValue().getName());
                    } else {
                        pickedCard = bestPlayable.getValue();
                        System.out.println("Chose Playable: " + "[" + bestPlayable.getKey() + "] "
                                + pickedCard.getName() + " (" + pickedCard.getCard().getManaCost() + ") "
                                + pickedCard.getType().toString());
                        System.out.println("Best was: " + "[" + best.getKey() + "] " + best.getValue().getName());
                    }
                }
            }
        }
        return pickedCard;
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

