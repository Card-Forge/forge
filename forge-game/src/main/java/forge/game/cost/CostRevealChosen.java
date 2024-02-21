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
package forge.game.cost;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class CostRevealChosen extends CostPart {

    private static final long serialVersionUID = 1L;

    public CostRevealChosen(final String type, final String desc) {
        super("1", type, desc);
    }

    @Override
    public int paymentOrder() { return 20; }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        if (getType().equals("Player")) {
            return "Reveal the player you chose";
        } else if (getType().equals("Type")) {
            return "Reveal the chosen " + getDescriptiveType().toLowerCase();
        }
        return "Update CostRevealChosen.java";
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player activator, final boolean effect) {
        final Card source = ability.getHostCard();

        if (getType().equals("Player")) {
            return source.hasChosenPlayer() && source.getTurnInController().equals(activator);
        } else if (getType().equals("Type")) {
            return source.hasChosenType() && source.getTurnInController().equals(activator);
        }
        return false;
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability, final boolean effect) {
        Card host = ability.getHostCard();
        String o = "";
        if (getType().equals("Player")) {
            o = host.getChosenPlayer().toString();
            host.revealChosenPlayer();
        } else if (getType().equals("Type")) {
            o = host.getChosenType();
            host.revealChosenType();
        }
        final String message = Localizer.getInstance().getMessage("lblPlayerReveals", ai, o);
        ai.getGame().getAction().notifyOfValue(ability, host, message, ai);
        return true;
    }

    // Inputs
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
