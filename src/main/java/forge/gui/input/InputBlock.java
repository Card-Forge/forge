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
package forge.gui.input;

import java.util.ArrayList;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardUtil;
import forge.CombatUtil;
import forge.Command;
import forge.Constant;
import forge.GameActionUtil;
import forge.PlayerZone;
import forge.view.match.ViewTopLevel;

/**
 * <p>
 * Input_Block class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputBlock extends Input {
    /** Constant <code>serialVersionUID=6120743598368928128L</code>. */
    private static final long serialVersionUID = 6120743598368928128L;

    private Card currentAttacker = null;
    private final ArrayList<Card> allBlocking = new ArrayList<Card>();

    /**
     * <p>
     * removeFromAllBlocking.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void removeFromAllBlocking(final Card c) {
        this.allBlocking.remove(c);
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // for Castle Raptors, since it gets a bonus if untapped
        for (final String effect : AllZone.getStaticEffects().getStateBasedMap().keySet()) {
            final Command com = GameActionUtil.getCommands().get(effect);
            com.execute();
        }

        // could add "Reset Blockers" button
        ButtonUtil.enableOnlyOK();

        if (this.currentAttacker == null) {
            /*
             * //Lure CardList attackers = new
             * CardList(AllZone.getCombat().getAttackers()); for(Card
             * attacker:attackers) {
             * if(attacker.hasKeyword("All creatures able to block CARDNAME do so."
             * )) { CardList bls =
             * AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
             * for(Card bl:bls) { if(CombatUtil.canBlock(attacker, bl,
             * AllZone.getCombat())) { allBlocking.add(bl);
             * AllZone.getCombat().addBlocker(attacker, bl); } } } }
             */

            AllZone.getDisplay().showMessage("To Block, click on your Opponents attacker first, then your blocker(s)");
        } else {
            final String attackerName = this.currentAttacker.isFaceDown() ? "Morph" : this.currentAttacker.getName();
            AllZone.getDisplay()
                    .showMessage(
                            "Select a creature to block " + attackerName + " ("
                                    + this.currentAttacker.getUniqueNumber() + ") ");
        }

        CombatUtil.showCombat();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        if (CombatUtil.finishedMandatotyBlocks(AllZone.getCombat())) {
            // Done blocking
            ButtonUtil.reset();

            AllZone.getPhase().setNeedToNextPhase(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        // is attacking?
        if (CardUtil.toList(AllZone.getCombat().getAttackers()).contains(card)) {
            this.currentAttacker = card;
        } else if (zone.is(Constant.Zone.Battlefield, AllZone.getHumanPlayer()) && card.isCreature()
                && CombatUtil.canBlock(this.currentAttacker, card, AllZone.getCombat())) {
            if ((this.currentAttacker != null) && (!this.allBlocking.contains(card))) {
                this.allBlocking.add(card);
                AllZone.getCombat().addBlocker(this.currentAttacker, card);
            }
        }
        else if (!Constant.Runtime.OLDGUI[0]) {
            ((ViewTopLevel) AllZone.getDisplay()).getInputController().remind();
        }
        this.showMessage();
    } // selectCard()
}
