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
package forge.card.cost;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.Player;
import forge.gui.GuiDialog;

/**
 * The Class CostPayLife.
 */
public class CostDraw extends CostPart {
    /**
     * CostDraw.
     * @param amount
     * @param playerSelector
     */
    public CostDraw(final String amount, final String playerSelector) {
        super(amount, playerSelector, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = this.convertAmount();
        sb.append("Draw ").append(Cost.convertAmountTypeToWords(i, this.getAmount(), "Card"));
        return sb.toString();
    }

    /** 
     * getPotentialPlayers.
     * @param payer
     * @param source
     */
    private List<Player> getPotentialPlayers(final Player payer, final SpellAbility  ability) {
        List<Player> res = new ArrayList<Player>();
        String type = this.getType();
        if ("Activator".equals(type)) {
            res.add(ability.getActivatingPlayer());
        } else {
            for (Player p : payer.getGame().getPlayers()) {
                if (p.isValid(type, payer, ability.getSourceCard())) {
                    res.add(p);
                }
            }
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability) {
        List<Player> potentials = getPotentialPlayers(ability.getActivatingPlayer(), ability);
        return !potentials.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final PaymentDecision decision, final Player ai, SpellAbility ability, Card source) {
        for (final Player p : getPotentialPlayers(ai, ability)) {
            p.drawCards(decision.c);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Game game) {
        final String amount = this.getAmount();
        final List<Player> players = getPotentialPlayers(ability.getActivatingPlayer(), ability);
        final Card source = ability.getSourceCard();

        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, amount, ability);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Draw ").append(c).append(" Card(s)?");

        if (GuiDialog.confirm(source, sb.toString())) {
            for (Player p : players) {
                p.drawCards(c);
            }
        } else {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final PaymentDecision decideAIPayment(final Player ai, final SpellAbility ability, final Card source) {
        Integer c = this.convertAmount();

        if (c == null) {
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        return new PaymentDecision(c);
    }
}
