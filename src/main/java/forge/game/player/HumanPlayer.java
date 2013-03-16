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

import forge.Card;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;

/**
 * <p>
 * HumanPlayer class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class HumanPlayer extends Player {
    private PlayerControllerHuman controller;
    
    /**
     * <p>
     * Constructor for HumanPlayer.
     * </p>
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     */
    public HumanPlayer(final LobbyPlayer player, GameState game) {
        super(player, game);
        
        controller = new PlayerControllerHuman(game, this);
    }

    /**
     * <p>
     * dredge.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean dredge() {
        boolean dredged = false;
        final boolean wantDredge = GuiDialog.confirm(null, "Do you want to dredge?");
        if (wantDredge) {
            final Card c = GuiChoose.one("Select card to dredge", this.getDredge());
            // rule 702.49a
            if (this.getDredgeNumber(c) <= getZone(ZoneType.Library).size()) {

                // might have to make this more sophisticated
                // dredge library, put card in hand
                game.getAction().moveToHand(c);

                for (int i = 0; i < this.getDredgeNumber(c); i++) {
                    final Card c2 = getZone(ZoneType.Library).get(0);
                    game.getAction().moveToGraveyard(c2);
                }
                dredged = true;
            } else {
                dredged = false;
            }
        }
        return dredged;
    }

    /** {@inheritDoc} */
    @Override
    public final void discard(final int num, final SpellAbility sa) {
        Singletons.getModel().getMatch().getInput().setInput(PlayerUtil.inputDiscard(num, sa));
    }

    /** {@inheritDoc} */
    @Override
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        if (this.getCardsIn(ZoneType.Hand).size() > 0) {
            Singletons.getModel().getMatch().getInput().setInput(PlayerUtil.inputDiscardNumUnless(num, uType, sa));
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void sacrificePermanent(final String prompt, final List<Card> choices) {
        final Input in = PlayerUtil.inputSacrificePermanent(choices, prompt);
        Singletons.getModel().getMatch().getInput().setInput(in);
    }

    /* (non-Javadoc)
     * @see forge.game.player.Player#getType()
     */
    @Override
    public PlayerType getType() {
        return PlayerType.HUMAN;
    }

    /* (non-Javadoc)
     * @see forge.game.player.Player#getController()
     */
    @Override
    public PlayerController getController() {
        return controller;
    }
} // end HumanPlayer class
