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

import java.util.List;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardPredicates;
import forge.Singletons;
import forge.game.GameState;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.views.VMessage;
import forge.view.ButtonUtil;

/**
 * <p>
 * InputAttack class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputAttack extends InputBase {
    /** Constant <code>serialVersionUID=7849903731842214245L</code>. */
    private static final long serialVersionUID = 7849903731842214245L;

    private final GameState game;
    public InputAttack(Player human) {
        super(human);
        game = human.getGame();
    }
    
    
    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // TODO still seems to have some issues with multiple planeswalkers

        ButtonUtil.enableOnlyOk();

        final Object o = game.getCombat().nextDefender();
        if (o == null) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Declare Attackers: Select Creatures to Attack ");
        sb.append(o.toString());

        CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());

        if (game.getCombat().getRemainingDefenders() == 0) {
            // Nothing left to attack, has to attack this defender
            List<Card> possibleAttackers = Singletons.getControl().getPlayer().getCardsIn(ZoneType.Battlefield);
            for (Card c : Iterables.filter(possibleAttackers, CardPredicates.Presets.CREATURES)) {
                if (c.hasKeyword("CARDNAME attacks each turn if able.") && CombatUtil.canAttack(c, game.getCombat())
                        && !c.isAttacking()) {
                    game.getCombat().addAttacker(c);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        if (!game.getCombat().getAttackers().isEmpty()) {
            game.getPhaseHandler().setCombat(true);
        }

        if (game.getCombat().getRemainingDefenders() != 0) {
            game.getPhaseHandler().repeatPhase();
        }

        game.getPhaseHandler().setPlayersPriorityPermission(false);
        Singletons.getModel().getMatch().getInput().updateObservers();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card) {
        if (card.isAttacking() || card.getController() != Singletons.getControl().getPlayer()) {
            return;
        }

        final Player human = Singletons.getControl().getPlayer();
        Zone zone = game.getZoneOf(card);
        if (zone.is(ZoneType.Battlefield, human)
                && CombatUtil.canAttack(card, game.getCombat())) {

            // TODO add the propaganda code here and remove it in
            // Phase.nextPhase()
            // if (!CombatUtil.checkPropagandaEffects(card))
            // return;

            game.getCombat().addAttacker(card);

            // just to make sure the attack symbol is marked
            human.getZone(ZoneType.Battlefield).updateObservers();
            CombatUtil.showCombat();
        }
        else {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
        }
    } // selectCard()
}
