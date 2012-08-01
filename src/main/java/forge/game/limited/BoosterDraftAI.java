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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import forge.Constant;
import forge.card.CardColor;
import forge.card.CardRules;
import forge.deck.Deck;
import forge.deck.generate.GenerateDeckUtil;
import forge.item.CardPrinted;
import forge.util.MyRandom;
import forge.util.closures.Predicate;

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
    private final String[][] deckColor = new String[BoosterDraftAI.N_DECKS][];

    /**
     * Constant <code>colorToLand.</code>
     */
    private static Map<String, String> colorToLand = new TreeMap<String, String>();

    // picks one Card from in_choose, removes that card, and returns the list
    // returns the cards not picked

    /**
     * <p>
     * choose.
     * </p>
     * 
     * @param chooseFrom
     *            List of CardPrinted
     * @param player
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public CardPrinted choose(final List<CardPrinted> chooseFrom, final int player) {
        if (Constant.Runtime.DEV_MODE[0]) {
            System.out.println("Player[" + player + "] pack: " + chooseFrom.toString());
        }

        CardPrinted pickedCard = null;

        List<CardPrinted> aiPlayables = CardRules.Predicates.IS_KEPT_IN_AI_DECKS.select(chooseFrom,
                CardPrinted.FN_GET_RULES);

        // Sort cards by rank.
        // Note that if pack has cards from different editions, they could have
        // the same Integer rank.
        // In that (hopefully rare) case, only one will end up in the Map.
        TreeMap<Integer, CardPrinted> rankedCards = new TreeMap<Integer, CardPrinted>();
        for (CardPrinted card : chooseFrom) {
            Integer rkg = draftRankings.getRanking(card.getName(), card.getEdition());
            if (rkg != null) {
                rankedCards.put(rkg, card);
            } else {
                System.out.println("Draft Rankings - Card Not Found: " + card.getName());
            }
        }

        if (this.playerColors.get(player).getColor1().equals("none")
                && this.playerColors.get(player).getColor2().equals("none")) {
            // Generally the first pick of the draft, no colors selected yet.

            // Sort playable cards by rank
            TreeMap<Integer, CardPrinted> rankedPlayableCards = new TreeMap<Integer, CardPrinted>();
            for (CardPrinted card : aiPlayables) {
                Integer rkg = draftRankings.getRanking(card.getName(), card.getEdition());
                if (rkg != null) {
                    rankedPlayableCards.put(rkg, card);
                }
            }

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
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Player[" + player + "] Color1: " + this.playerColors.get(player).getColor1());
                    if (!this.playerColors.get(player).getColor2().equals("none")) {
                        System.out.println("Player[" + player + "] Color2: "
                                + this.playerColors.get(player).getColor2());
                    }
                }
                this.playerColors.get(player).setMana1(
                        this.playerColors.get(player).colorToMana(this.playerColors.get(player).getColor1()));
                if (!this.playerColors.get(player).getColor2().equals("none")) {
                    this.playerColors.get(player).setMana2(
                            this.playerColors.get(player).colorToMana(this.playerColors.get(player).getColor2()));
                }
            }
        } else if (!this.playerColors.get(player).getColor1().equals("none")
                && this.playerColors.get(player).getColor2().equals("none")) {
            // Has already picked one color, but not the second.

            // Sort playable, on-color, or mono-colored, or colorless cards
            TreeMap<Integer, CardPrinted> rankedPlayableCards = new TreeMap<Integer, CardPrinted>();
            for (CardPrinted card : aiPlayables) {
                CardColor currentColor1 = CardColor.fromNames(this.playerColors.get(player).getColor1());
                CardColor color = card.getCard().getColor();
                if (color.isColorless() || color.sharesColorWith(currentColor1) || color.isMonoColor()) {
                    Integer rkg = draftRankings.getRanking(card.getName(), card.getEdition());
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
                this.playerColors.get(player).setMana2(
                        this.playerColors.get(player).colorToMana(this.playerColors.get(player).getColor2()));
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Player[" + player + "] Color2: " + this.playerColors.get(player).getColor2());
                }
            }
        } else {
            // Has already picked both colors.
            CardColor colors = CardColor.fromNames(this.playerColors.get(player).getColor1(),
                    this.playerColors.get(player).getColor2());
            Predicate<CardRules> hasColor = Predicate.or(new GenerateDeckUtil.ContainsAllColorsFrom(colors),
                    GenerateDeckUtil.COLORLESS_CARDS);
            List<CardPrinted> colorList = hasColor.select(aiPlayables, CardPrinted.FN_GET_RULES);

            // Sort playable, on-color cards by rank
            TreeMap<Integer, CardPrinted> rankedPlayableCards = new TreeMap<Integer, CardPrinted>();
            for (CardPrinted card : colorList) {
                Integer rkg = draftRankings.getRanking(card.getName(), card.getEdition());
                if (rkg != null) {
                    rankedPlayableCards.put(rkg, card);
                }
            }

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
     * Pick a card.
     * 
     * @param rankedCards
     * @param rankedPlayableCards
     * @return CardPrinted
     */
    private CardPrinted pickCard(TreeMap<Integer, CardPrinted> rankedCards,
            TreeMap<Integer, CardPrinted> rankedPlayableCards) {
        CardPrinted pickedCard = null;
        Map.Entry<Integer, CardPrinted> best = rankedCards.firstEntry();
        if (best != null) {
            if (rankedPlayableCards.containsValue(best.getValue())) {
                // If best card is playable, pick it.
                pickedCard = best.getValue();
                System.out.println("Chose Best: " + "[" + best.getKey() + "] " + pickedCard.getName() + " ("
                        + pickedCard.getCard().getManaCost() + ") " + pickedCard.getType().toString());
            } else {
                // If not, find the best card that is playable.
                Map.Entry<Integer, CardPrinted> bestPlayable = rankedPlayableCards.firstEntry();
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
     * testColors.
     * </p>
     * 
     * @param n
     *            an array of int.
     */
    private void testColors(final int[] n) {
        if (n.length != BoosterDraftAI.N_DECKS) {
            throw new RuntimeException("BoosterDraftAI : testColors error, numbers array length does not equal 7");
        }

        final Set<Integer> set = new TreeSet<Integer>();
        for (int i = 0; i < BoosterDraftAI.N_DECKS; i++) {
            set.add(Integer.valueOf(n[i]));
        }

        if (set.size() != BoosterDraftAI.N_DECKS) {
            throw new RuntimeException("BoosterDraftAI : testColors error, numbers not unique");
        }

        for (int i = 0; i < BoosterDraftAI.N_DECKS; i++) {
            if ((n[i] < 0) || (this.deckColorChoices.length <= n[i])) {
                throw new RuntimeException("BoosterDraftAI : testColors error, index out of range - " + n[i]);
            }
        }
    } // testColors()

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
            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Deck[" + i + "]");
            }

            CardColor cc = CardColor.fromNames(this.playerColors.get(i).getColor1(), this.playerColors.get(i).getColor2());
            out[i] = new BoosterDeck(this.deck.get(i), cc);
        }
        return out;
    } // getDecks()

    // returns 7 different ints, within the range of 0-9

    /**
     * <p>
     * getDeckColors.
     * </p>
     * 
     * @return an array of int.
     */
    private int[] getDeckColors() {
        final int[] out = new int[BoosterDraftAI.N_DECKS];
        int start = MyRandom.getRandom().nextInt(10);

        for (int i = 0; i < out.length; i++) {
            // % to get an index between 0 and deckColorChoices.length
            out[i] = start % this.deckColorChoices.length;
            start++;
        }
        this.testColors(out);

        return out;
    } // getDeckColors()

    /**
     * <p>
     * Constructor for BoosterDraftAI.
     * </p>
     */
    public BoosterDraftAI() {
        // choose colors for decks
        final int[] n = this.getDeckColors();
        for (int i = 0; i < n.length; i++) {
            this.deckColor[i] = this.deckColorChoices[n[i]];
        }

        // initialize color map
        BoosterDraftAI.colorToLand.put(Constant.Color.BLACK, "Swamp");
        BoosterDraftAI.colorToLand.put(Constant.Color.BLUE, "Island");
        BoosterDraftAI.colorToLand.put(Constant.Color.GREEN, "Forest");
        BoosterDraftAI.colorToLand.put(Constant.Color.RED, "Mountain");
        BoosterDraftAI.colorToLand.put(Constant.Color.WHITE, "Plains");

        // Initialize deck array and playerColors list
        for (int i = 0; i < this.deck.size(); i++) {
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

    private final ArrayList<DeckColors> playerColors = new ArrayList<DeckColors>();

    private ReadDraftRankings draftRankings;
    private static final int TAKE_BEST_THRESHOLD = 50;

    // all 10 two color combinations
    private final String[][] deckColorChoices = { { Constant.Color.BLACK, Constant.Color.BLUE },
            { Constant.Color.BLACK, Constant.Color.GREEN }, { Constant.Color.BLACK, Constant.Color.RED },
            { Constant.Color.BLACK, Constant.Color.WHITE },

            { Constant.Color.BLUE, Constant.Color.GREEN }, { Constant.Color.BLUE, Constant.Color.RED },
            { Constant.Color.BLUE, Constant.Color.WHITE },

            { Constant.Color.GREEN, Constant.Color.RED }, { Constant.Color.GREEN, Constant.Color.WHITE },

            { Constant.Color.RED, Constant.Color.WHITE } };

} // BoosterDraftAI()

