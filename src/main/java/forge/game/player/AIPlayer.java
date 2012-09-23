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

import java.util.Random;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;
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

    /**
     * <p>
     * getOpponent.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    @Override
    public final Player getOpponent() {
        return AllZone.getHumanPlayer();
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
        final CardList dredgers = this.getDredge();
        final Random random = MyRandom.getRandom();

        // use dredge if there are more than one of them in your graveyard
        if ((dredgers.size() > 1) || ((dredgers.size() == 1) && random.nextBoolean())) {
            dredgers.shuffle();
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
    public final CardList discard(final int num, final SpellAbility sa, final boolean duringResolution) {
        int max = this.getCardsIn(ZoneType.Hand).size();
        max = Math.min(max, num);
        final CardList discarded = ComputerUtil.discardNumTypeAI(max, null, sa);
        for (int i = 0; i < discarded.size(); i++) {
            this.doDiscard(discarded.get(i), sa);
        }

        return discarded;
    } // end discard

    /** {@inheritDoc} */
    @Override
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        final CardList hand = this.getCardsIn(ZoneType.Hand);
        final CardList tHand = hand.getType(uType);

        if (tHand.size() > 0) {
            CardListUtil.sortCMC(tHand);
            tHand.reverse();
            tHand.get(0).getController().discard(tHand.get(0), sa); // this got
                                                                    // changed
                                                                    // to
                                                                    // doDiscard
                                                                    // basically
            return;
        }
        AllZone.getComputerPlayer().discard(num, sa, false);
    }

    // /////////////////////////

    /** {@inheritDoc} */
    @Override
    protected final void doScry(final CardList topN, final int n) {
        int num = n;
        for (int i = 0; i < num; i++) {
            boolean bottom = false;
            if (topN.get(i).isBasicLand()) {
                CardList bl = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                bl = bl.filter(CardPredicates.Presets.BASIC_LANDS);

                bottom = bl.size() > 5; // if control more than 5 Basic land,
                                        // probably don't need more
            } else if (topN.get(i).isCreature()) {
                CardList cl = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                cl = cl.filter(CardPredicates.Presets.CREATURES);
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
    public final void sacrificePermanent(final String prompt, final CardList choices) {
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

} // end AIPlayer class
