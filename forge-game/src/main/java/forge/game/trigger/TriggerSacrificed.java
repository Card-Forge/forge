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
package forge.game.trigger;

import forge.game.card.Card;
import forge.game.cost.IndividualCostPaymentInstance;
import forge.game.spellability.SpellAbility;
import forge.game.zone.CostPaymentStack;

/**
 * <p>
 * Trigger_Sacrificed class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerSacrificed extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Sacrificed.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerSacrificed(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final Card sac = (Card) runParams2.get("Card");
        final SpellAbility sourceSA = (SpellAbility) runParams2.get("Cause");
        if (this.mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(sac.getController(), this.mapParams.get("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        if (this.mapParams.containsKey("ValidCard")) {
            if (!sac.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }
        if (this.mapParams.containsKey("ValidSourceController")) {
        	if (sourceSA == null || !sourceSA.getActivatingPlayer().isValid(this.mapParams.get("ValidSourceController"),
        			this.getHostCard().getController(), this.getHostCard(), null)) {
        		return false;
        	}
        }

        if (this.mapParams.containsKey("WhileKeyword")) {
        	final String keyword = this.mapParams.get("WhileKeyword");
        	boolean withKeyword = false;
        	
        	// When cast with Emerge, the cost instance is there
        	IndividualCostPaymentInstance currentPayment = (IndividualCostPaymentInstance) runParams2.get("IndividualCostPaymentInstance");
        	SpellAbility sa = currentPayment.getPayment().getAbility();

    		if (sa != null && sa.getHostCard() != null) {
	    		if (sa.isSpell() && sa.getHostCard().hasStartOfUnHiddenKeyword(keyword)) {
	    			withKeyword = true;
	    		}
    		}
    		if (!withKeyword) {
    			// When done for another card to get the Mana, the Emerge card is there
	    		CostPaymentStack stack = (CostPaymentStack) runParams2.get("CostStack");

	        	for (IndividualCostPaymentInstance individual : stack) {
	        		sa = individual.getPayment().getAbility();
	
	        		if (sa == null ||  sa.getHostCard() == null)
	        			continue;
	
	        		if (sa.isSpell() && sa.getHostCard().hasStartOfUnHiddenKeyword(keyword)) {
	        			withKeyword = true;
	        			break;
	        		}
	        	}
    		}
    		
    		if (!withKeyword)
    			return false;
        }

        return true;
    }


    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sacrificed: ").append(sa.getTriggeringObject("Card"));
        return sb.toString();
    }

}
