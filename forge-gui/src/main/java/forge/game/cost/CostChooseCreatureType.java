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

import java.util.ArrayList;
import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.gui.GuiDialog;

/**
 * The Class CostChooseCreatureType.
 */
public class CostChooseCreatureType extends CostPart {

    /**
     * Instantiates a new cost mill.
     * 
     * @param amount
     *            the amount
     */
    public CostChooseCreatureType(final String amount) {
        this.setAmount(amount);
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
        return true;
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
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getSourceCard();

        final StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Choose a creature type?");

        if (GuiDialog.confirm(source, sb.toString())) {
        	String choice = activator.getController().chooseSomeType("Creature", ability, new ArrayList<String>(CardType.getCreatureTypes()), new ArrayList<String>());
    		source.setChosenType(choice);
        } else {
            return false;
        }
        return true;
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
        sb.append("Choose ");
        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), "creature type"));
        return sb.toString();
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
                return null; // cannot pay
            } else {
                c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
            }
        }
        return new PaymentDecision(c);
    }

	@Override
	public boolean payAI(PaymentDecision decision, Player ai,
			SpellAbility ability, Card source) {
		String choice = ai.getController().chooseSomeType("Creature", ability, new ArrayList<String>(CardType.getCreatureTypes()), new ArrayList<String>());
		source.setChosenType(choice);
		return true;
	}

}
