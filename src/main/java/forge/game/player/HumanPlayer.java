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
import forge.FThreads;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputSelectCards;
import forge.control.input.InputSelectCardsFromList;
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
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        final List<Card> hand = getCardsIn(ZoneType.Hand);
        final InputSelectCards target = new InputSelectCardsFromList(num, num, hand) {
            private static final long serialVersionUID = -5774108410928795591L;

            @Override
            protected boolean hasAllTargets() {
                for(Card c : selected) {
                    if (c.isType(uType))
                        return true;
                }
                return super.hasAllTargets();
            }
        };
        target.setMessage("Select %d cards to discard, unless you discard a " + uType + ".");
        FThreads.setInputAndWait(target);
        for(Card c : target.getSelected())
            c.getController().discard(c, sa);
    } // input_discardNumUnless
    
    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param ab
     */
    public void playSpellAbility(Card c, SpellAbility ab) {
        if (ab == Ability.PLAY_LAND_SURROGATE)
            this.playLand(c);
        else {
            game.getActionPlay().playSpellAbility(ab, this);
        }
        game.getPhaseHandler().setPriority(this);
    }

    @Override
    public PlayerType getType() {
        return PlayerType.HUMAN;
    }
    public PlayerController getController() {
        return controller;
    }

} // end HumanPlayer class
