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


import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.ai.AiController;
import forge.game.ai.ComputerUtilCard;
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

    private final PlayerControllerAi controller;
    /**
     * <p>
     * Constructor for AIPlayer.
     * </p>
     * @param computerAIGeneral
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     */
    public AIPlayer(final LobbyPlayer player, final GameState game) {
        super(player, game);
        controller = new PlayerControllerAi(game, this);
    }

    public AiController getAi() { 
        return controller.getAi();
    }
    


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
                game.getAction().moveToHand(c);
                // put dredge number in graveyard
                for (int i = 0; i < this.getDredgeNumber(c); i++) {
                    final Card c2 = this.getCardsIn(ZoneType.Library).get(0);
                    game.getAction().moveToGraveyard(c2);
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
    public final void discard(final int num, final SpellAbility sa) {
        int max = this.getCardsIn(ZoneType.Hand).size();
        max = Math.min(max, num);
        final List<Card> toDiscard = this.getAi().getCardsToDiscard(max, null, sa);
        for (int i = 0; i < toDiscard.size(); i++) {
            this.doDiscard(toDiscard.get(i), sa);
        }
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
        this.discard(num, sa);
    }

    // /////////////////////////

    /** {@inheritDoc} */
    @Override
    public final void sacrificePermanent(final String prompt, final List<Card> choices) {
        if (choices.size() > 0) {
            // TODO - this could probably use better AI
            final Card c = ComputerUtilCard.getWorstPermanentAI(choices, false, false, false, false);
            game.getAction().sacrificeDestroy(c);
        }
    }

    /* (non-Javadoc)
     * @see forge.game.player.Player#getType()
     */
    @Override
    public PlayerType getType() {
        return PlayerType.COMPUTER;
    }


    /* (non-Javadoc)
     * @see forge.game.player.Player#getController()
     */
    @Override
    public PlayerController getController() {
        return controller;
    }
} // end AIPlayer class
