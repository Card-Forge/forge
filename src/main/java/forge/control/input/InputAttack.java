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
package forge.control.input;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Constant.Zone;
import forge.game.phase.CombatUtil;
import forge.game.player.PlayerZone;
import forge.Singletons;

/**
 * <p>
 * InputAttack class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputAttack extends Input {
    /** Constant <code>serialVersionUID=7849903731842214245L</code>. */
    private static final long serialVersionUID = 7849903731842214245L;

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // TODO still seems to have some issues with multiple planeswalkers

        ButtonUtil.enableOnlyOK();

        final Object o = AllZone.getCombat().nextDefender();
        if (o == null) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Declare Attackers: Select Creatures to Attack ");
        sb.append(o.toString());

        Singletons.getControl().getControlMatch().showMessage(sb.toString());

        if (AllZone.getCombat().getRemainingDefenders() == 0) {
            // Nothing left to attack, has to attack this defender
            CardList possibleAttackers = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
            possibleAttackers = possibleAttackers.getType("Creature");
            for (int i = 0; i < possibleAttackers.size(); i++) {
                final Card c = possibleAttackers.get(i);
                if (c.hasKeyword("CARDNAME attacks each turn if able.") && CombatUtil.canAttack(c, AllZone.getCombat())
                        && !c.isAttacking()) {
                    AllZone.getCombat().addAttacker(c);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        if (!AllZone.getCombat().getAttackers().isEmpty()) {
            Singletons.getModel().getGameState().getPhaseHandler().setCombat(true);
        }

        if (AllZone.getCombat().getRemainingDefenders() != 0) {
            Singletons.getModel().getGameState().getPhaseHandler().repeatPhase();
        }

        Singletons.getModel().getGameState().getPhaseHandler().setNeedToNextPhase(true);
        AllZone.getInputControl().resetInput();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        if (card.isAttacking() || card.getController().isComputer()) {
            return;
        }

        if (zone.is(Constant.Zone.Battlefield, AllZone.getHumanPlayer())
                && CombatUtil.canAttack(card, AllZone.getCombat())) {

            // TODO add the propaganda code here and remove it in
            // Phase.nextPhase()
            // if (!CombatUtil.checkPropagandaEffects(card))
            // return;

            AllZone.getCombat().addAttacker(card);

            // just to make sure the attack symbol is marked
            AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
            CombatUtil.showCombat();
        }
        else {
            Singletons.getControl().getControlMatch().getMessageControl().remind();
        }
    } // selectCard()

    /**
     * <p>
     * unselectCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param zone
     *            a {@link forge.game.player.PlayerZone} object.
     */
    public void unselectCard(final Card card, final PlayerZone zone) {

    }
}
