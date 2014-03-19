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
package forge.screens.match.input;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.toolbox.FCardZoom;
import forge.toolbox.FCardZoom.ZoomController;

import java.util.List;

/**
 * <p>
 * Input_PassPriority class.
 * </p>
 * 
 * @author Forge
 * @version $Id: InputPassPriority.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputPassPriority extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=-581477682214137181L</code>. */
    private static final long serialVersionUID = -581477682214137181L;
    private final Player player;
    private SpellAbility chosenSa;
    
    public InputPassPriority(Player human) {
        player = human;
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        showMessage(getTurnPhasePriorityMessage(player.getGame()));
        chosenSa = null;
        ButtonUtil.enableOnlyOk();
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        stop();
    }
    
    public SpellAbility getChosenSa() {
        return chosenSa;
    }

    @Override
    protected void onCardSelected(final Card card, final List<Card> orderedCardOptions) {
        FCardZoom.show("Select a spell/ability", card, orderedCardOptions, new ZoomController<SpellAbility>() {
            @Override
            public List<SpellAbility> getOptions(final Card card) {
                return card.getAllPossibleAbilities(player, true);
            }

            @Override
            public boolean selectOption(final Card card, final SpellAbility option) {
                selectAbility(option);
                return true; //TODO: Avoid hiding card zoom when selecting mana abilities
            }
        });
    }

    @Override
    public void selectAbility(final SpellAbility ab) {
    	if (ab != null) {
            chosenSa = ab;
            stop();
        }
    }
}
