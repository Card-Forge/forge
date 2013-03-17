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

import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.zone.ZoneType;

public class HumanPlayer extends Player {
    private PlayerControllerHuman controller;
    
    public HumanPlayer(final LobbyPlayer player, GameState game) {
        super(player, game);
        controller = new PlayerControllerHuman(game, this);
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

    @Override
    public PlayerType getType() {
        return PlayerType.HUMAN;
    }
    public PlayerController getController() {
        return controller;
    }

} // end HumanPlayer class
