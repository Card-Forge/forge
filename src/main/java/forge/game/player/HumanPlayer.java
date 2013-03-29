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

import forge.Card;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.control.input.InputBase;
import forge.game.GameState;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

public class HumanPlayer extends Player {
    private PlayerControllerHuman controller;
    
    public HumanPlayer(final LobbyPlayer player, GameState game) {
        super(player, game);
        controller = new PlayerControllerHuman(game, this);
    }

    /** {@inheritDoc} */
    @Override
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        if (this.getCardsIn(ZoneType.Hand).size() > 0) {
            Singletons.getModel().getMatch().getInput().setInput(inputDiscardNumUnless(num, uType, sa));
        }
    }
    
    private static Input inputDiscardNumUnless(final int nCards, final String uType, final SpellAbility sa) {
        final SpellAbility sp = sa;
        final Input target = new InputBase() {
            private static final long serialVersionUID = 8822292413831640944L;

            private int n = 0;

            @Override
            public void showMessage() {
                if (Singletons.getControl().getPlayer().getZone(ZoneType.Hand).size() == 0) {
                    this.stop();
                }
                CMatchUI.SINGLETON_INSTANCE.showMessage(
                        "Select " + (nCards - this.n) + " cards to discard, unless you discard a " + uType + ".");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(card);
                if (zone.is(ZoneType.Hand)) {
                    card.getController().discard(card, sp);
                    this.n++;

                    if (card.isType(uType.toString())) {
                        this.stop();
                    } else {
                        if ((this.n == nCards) || (Singletons.getControl().getPlayer().getZone(ZoneType.Hand).size() == 0)) {
                            this.stop();
                        } else {
                            this.showMessage();
                        }
                    }
                }
            }
        };

        return target;
    } // input_discardNumUnless
    

    @Override
    public PlayerType getType() {
        return PlayerType.HUMAN;
    }
    public PlayerController getController() {
        return controller;
    }

} // end HumanPlayer class
