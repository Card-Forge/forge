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

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.Player;
import forge.gui.GuiDialog;

/**
 * The Class CostPayLife.
 */
public class CostPayLife extends CostPart {
    int paidAmount = 0;
    
    /**
     * Instantiates a new cost pay life.
     * 
     * @param amount
     *            the amount
     */
    public CostPayLife(final String amount) {
        this.setAmount(amount);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Pay ").append(this.getAmount()).append(" Life");
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public final void refund(final Card source) {
        // Really should be activating player
        source.getController().payLife(this.paidAmount * -1, null);
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
        Integer amount = this.convertAmount();
        Player activator = ability.getActivatingPlayer();
        if(amount == null) { // try to calculate when it's defined.
            String sAmount = getAmount();
            String sVar = ability.getSVar(sAmount);
            if(!sVar.startsWith("XChoice")) {
                amount = AbilityUtils.calculateAmount(ability.getSourceCard(), getAmount(), ability);
            }
        }
        
        if ((amount != null) && !activator.canPayLife(amount)) {
            return false;
        }

        if (activator.hasKeyword("You can't pay life to cast spells or activate abilities.")) {
            return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#payAI(forge.card.cost.PaymentDecision, forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public void payAI(PaymentDecision decision, Player ai, SpellAbility ability, Card source) {
        // TODO Auto-generated method stub
        paidAmount = decision.c;
        ai.payLife(paidAmount, null);
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
        final Card source = ability.getSourceCard();
        final String amount = this.getAmount();
        final Player activator = ability.getActivatingPlayer();
        final int life = activator.getLife();

        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.startsWith("XChoice")) {
                int limit = life;
                if (sVar.contains("LimitMax")) {
                    limit = AbilityUtils.calculateAmount(source, sVar.split("LimitMax.")[1], ability);
                }
                int maxLifePayment = limit < life ? limit : life;
                c = Cost.chooseXValue(source, ability, maxLifePayment);
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Pay ").append(c).append(" Life?");

        if (activator.canPayLife(c) && GuiDialog.confirm(source, sb.toString())) {
            activator.payLife(c, null);
        } else {
            return false;
        }
        paidAmount = c;
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(this.getAmount());
            // Generalize this
            if (sVar.equals("XChoice")) {
                return null;
            } else {
                c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
            }
        }
        if (!ai.canPayLife(c)) {
            return null;
        }
        // activator.payLife(c, null);
        return new PaymentDecision(c);
    }
}
