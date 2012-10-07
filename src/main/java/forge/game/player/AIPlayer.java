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
package forge.game.player;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Iterables;
import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * <p>
 * AIPlayer class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AIPlayer extends Player {

    /**
     * <p>
     * Constructor for AIPlayer.
     * </p>
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     */
    public AIPlayer(final String myName) {
        this(myName, 20, 0);
    }

    /**
     * <p>
     * Constructor for AIPlayer.
     * </p>
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     * @param myLife
     *            a int.
     * @param myPoisonCounters
     *            a int.
     */
    public AIPlayer(final String myName, final int myLife, final int myPoisonCounters) {
        super(myName, myLife, myPoisonCounters);
    }

    
    // //////////////
    // /
    // / Methods to ease transition to Abstract Player class
    // /
    // /////////////
    /**
     * <p>
     * isHuman.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean isHuman() {
        return false;
    }

    
    /**
     * <p>
     * isComputer.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean isComputer() {
        return true;
    }

    // /////////////
    // /
    // / End transition methods
    // /
    // /////////////

    // //////////////////////////////
    // /
    // / replaces Singletons.getModel().getGameAction().draw* methods
    // /
    // //////////////////////////////

    /**
     * <p>
     * dredge.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean dredge() {
        final List<Card> dredgers = this.getDredge();
        final Random random = MyRandom.getRandom();

        // use dredge if there are more than one of them in your graveyard
        if ((dredgers.size() > 1) || ((dredgers.size() == 1) && random.nextBoolean())) {
            CardLists.shuffle(dredgers);
            final Card c = dredgers.get(0);
            // rule 702.49a
            if (this.getDredgeNumber(c) <= this.getCardsIn(ZoneType.Library).size()) {
                // dredge library, put card in hand
                Singletons.getModel().getGameAction().moveToHand(c);
                // put dredge number in graveyard
                for (int i = 0; i < this.getDredgeNumber(c); i++) {
                    final Card c2 = this.getCardsIn(ZoneType.Library).get(0);
                    Singletons.getModel().getGameAction().moveToGraveyard(c2);
                }
                return true;
            }
        }
        return false;
    }

    // //////////////////////////////
    // /
    // / replaces Singletons.getModel().getGameAction().discard* methods
    // /
    // //////////////////////////////

    /** {@inheritDoc} */
    @Override
    public final List<Card> discard(final int num, final SpellAbility sa, final boolean duringResolution) {
        int max = this.getCardsIn(ZoneType.Hand).size();
        max = Math.min(max, num);
        final List<Card> discarded = ComputerUtil.discardNumTypeAI(max, null, sa);
        for (int i = 0; i < discarded.size(); i++) {
            this.doDiscard(discarded.get(i), sa);
        }

        return discarded;
    } // end discard

    /** {@inheritDoc} */
    @Override
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        final List<Card> hand = this.getCardsIn(ZoneType.Hand);
        final List<Card> tHand = CardLists.getType(hand, uType);

        if (tHand.size() > 0) {
            Card toDiscard = Aggregates.itemWithMin(tHand, CardPredicates.Accessors.fnGetCmc);
            toDiscard.getController().discard(toDiscard, sa); // this got changed
                                                              // to doDiscard basically
            return;
        }
        this.discard(num, sa, false);
    }

    // /////////////////////////

    /** {@inheritDoc} */
    @Override
    protected final void doScry(final List<Card> topN, final int n) {
        int num = n;
        for (int i = 0; i < num; i++) {
            boolean bottom = false;
            if (topN.get(i).isBasicLand()) {
                List<Card> bl = this.getCardsIn(ZoneType.Battlefield);
                int nBasicLands = Iterables.size(Iterables.filter(bl, CardPredicates.Presets.BASIC_LANDS));

                bottom = nBasicLands > 5; // if control more than 5 Basic land,
                                        // probably don't need more
            } else if (topN.get(i).isCreature()) {
                List<Card> cl = this.getCardsIn(ZoneType.Battlefield);
                cl = CardLists.filter(cl, CardPredicates.Presets.CREATURES);
                bottom = cl.size() > 5; // if control more than 5 Creatures,
                                        // probably don't need more
            }
            if (bottom) {
                final Card c = topN.get(i);
                Singletons.getModel().getGameAction().moveToBottomOfLibrary(c);
                // topN.remove(c);
            }
        }
        num = topN.size();
        // put the rest on top in random order
        for (int i = 0; i < num; i++) {
            final Random rndm = MyRandom.getRandom();
            final int r = rndm.nextInt(topN.size());
            final Card c = topN.get(r);
            Singletons.getModel().getGameAction().moveToLibrary(c);
            topN.remove(r);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void sacrificePermanent(final String prompt, final List<Card> choices) {
        if (choices.size() > 0) {
            // TODO - this could probably use better AI
            final Card c = CardFactoryUtil.getWorstPermanentAI(choices, false, false, false, false);
            Singletons.getModel().getGameAction().sacrificeDestroy(c);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void clashMoveToTopOrBottom(final Card c) {
        // computer just puts the card back until such time it can make a
        // smarter decision
        Singletons.getModel().getGameAction().moveToLibrary(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Player#discard_Chains_of_Mephistopheles()
     */
    @Override
    protected final void discardChainsOfMephistopheles() {
        this.discard(null);
        this.drawCard();
    }

    /* (non-Javadoc)
     * @see forge.game.player.Player#getType()
     */
    @Override
    public PlayerType getType() {
        return PlayerType.COMPUTER;
    }
} // end AIPlayer class
