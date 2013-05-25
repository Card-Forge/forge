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

import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.game.phase.PhaseHandler;
import forge.game.player.HumanPlay;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.view.ButtonUtil;

/**
 * <p>
 * Input_PassPriority class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputPassPriority extends InputPassPriorityBase {
    /** Constant <code>serialVersionUID=-581477682214137181L</code>. */
    private static final long serialVersionUID = -581477682214137181L;
    
    /**
     * TODO: Write javadoc for Constructor.
     * @param player
     */
    public InputPassPriority(Player human) {
        super(human);
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        for (Player p : player.getGame().getRegisteredPlayers()) {
            p.getZone(ZoneType.Battlefield).updateObservers();
        }
        ButtonUtil.enableOnlyOk();

        final PhaseHandler ph = player.getGame().getPhaseHandler();
        final StringBuilder sb = new StringBuilder();

        sb.append("Priority: ").append(player).append("\n").append("\n");
        sb.append("Turn : ").append(ph.getPlayerTurn()).append("\n");
        sb.append("Phase: ").append(ph.getPhase().Name).append("\n");
        sb.append("Stack: ");
        if (!player.getGame().getStack().isEmpty()) {
            sb.append(player.getGame().getStack().size()).append(" to Resolve.");
        } else {
            sb.append("Empty");
        }
        sb.append("\n");
        

        showMessage(sb.toString());
    }


    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        if( isFinished() ) return;
        pass();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, boolean isMetaDown) {
        final SpellAbility ab = player.getController().getAbilityToPlay(player.getGame().getAbilitesOfCard(card, player));
        if ( null != ab) {
            Runnable execAbility = new Runnable() {
                @Override
                public void run() {
                    HumanPlay.playSpellAbility(player, card, ab);
                }
            };
            
            player.getGame().getInputQueue().LockAndInvokeGameAction(execAbility);
        }
        else {
            flashIncorrectAction();
        }
    } // selectCard()
}
