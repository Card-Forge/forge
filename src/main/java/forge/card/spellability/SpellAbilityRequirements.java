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
package forge.card.spellability;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardCharacteristicName;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.cost.CostPayment;
import forge.game.zone.Zone;

/**
 * <p>
 * SpellAbility_Requirements class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellAbilityRequirements {
    private SpellAbility ability = null;
    private TargetSelection select = null;
    private CostPayment payment = null;
    private boolean isFree = false;
    private boolean skipStack = false;
    private boolean bCasting = false;
    private Zone fromZone = null;
    private Integer zonePosition = null;

 
    public final void setSkipStack(final boolean bSkip) {
        this.skipStack = bSkip;
    }
 
    public final void setFree(final boolean bFree) {
        this.isFree = bFree;
    }


    public SpellAbilityRequirements(final SpellAbility sa, final TargetSelection ts, final CostPayment cp) {
        this.ability = sa;
        this.select = ts;
        this.payment = cp;
    }

    public final void fillRequirements() {
        this.fillRequirements(false);
    }

    public final void fillRequirements(final boolean skipTargeting) {
        if ((this.ability instanceof Spell) && !this.bCasting) {
            // remove from hand
            this.bCasting = true;
            if (!this.ability.getSourceCard().isCopiedSpell()) {
                final Card c = this.ability.getSourceCard();

                this.fromZone = Singletons.getModel().getGame().getZoneOf(c);
                this.zonePosition = this.fromZone.getPosition(c);
                this.ability.setSourceCard(Singletons.getModel().getGame().getAction().moveToStack(c));
            }
        }

        // freeze Stack. No abilities should go onto the stack while I'm filling requirements.
        Singletons.getModel().getGame().getStack().freezeStack();

        // Announce things like how many times you want to Multikick or the value of X
        if (!this.announceRequirements()) {
            this.select.setCancel(true);
            rollbackAbility();
            return;
        }

        // Skip to paying if parent ability doesn't target and has no
        // subAbilities.
        // (or trigger case where its already targeted)
        if (!skipTargeting && (this.select.doesTarget() || (this.ability.getSubAbility() != null))) {
            this.select.setRequirements(this);
            this.select.clearTargets();
            this.select.chooseTargets();
            if (this.select.isCanceled()) {
                rollbackAbility();
                return;
            }
        }
        
        // Payment
        if (!this.isFree) {
            this.payment.setRequirements(this);
            this.payment.changeCost();
            this.payment.payCost();
        } 
    
        if (this.payment.isCanceled()) {
            rollbackAbility();
            return;
        }
        
        else if (this.isFree || this.payment.isAllPaid()) {
            if (this.skipStack) {
                AbilityUtils.resolve(this.ability, false);
            } else {

                this.enusureAbilityHasDescription(this.ability);
                this.ability.getActivatingPlayer().getManaPool().clearManaPaid(this.ability, false);
                Singletons.getModel().getGame().getStack().addAndUnfreeze(this.ability);
            }
    
            // Warning about this - resolution may come in another thread, and it would still need its targets
            this.select.clearTargets();
            Singletons.getModel().getGame().getAction().checkStateEffects();
        }
    }

    private void rollbackAbility() { 
        // cancel ability during target choosing
        final Card c = this.ability.getSourceCard();

        // split cards transform back to full form if targeting is canceled
        if (c.isSplitCard()) {
            c.setState(CardCharacteristicName.Original);
        }

        if (this.bCasting && !c.isCopiedSpell()) { // and not a copy
            // add back to where it came from
            Singletons.getModel().getGame().getAction().moveTo(this.fromZone, c, this.zonePosition);
        }

        if (this.select != null) {
            this.select.clearTargets();
        }

        this.ability.resetOnceResolved();
        this.payment.cancelPayment();
        Singletons.getModel().getGame().getStack().clearFrozen();
        // Singletons.getModel().getGame().getStack().removeFromFrozenStack(this.ability);
    }
    

    public boolean announceRequirements() {
        // Announcing Requirements like Choosing X or Multikicker
        // SA Params as comma delimited list
        String announce = ability.getParam("Announce");
        if (announce != null) {
            for(String aVar : announce.split(",")) {
                String value = ability.getActivatingPlayer().getController().announceRequirements(ability, aVar);
                 if (value == null || !StringUtils.isNumeric(value)) { 
                     return false;
                 } else if (ability.getPayCosts().getCostMana() != null && !ability.getPayCosts().getCostMana().canXbe0() 
                         && Integer.parseInt(value) == 0) {
                     return false;
                 }
                 ability.setSVar(aVar, "Number$" + value);
                 ability.getSourceCard().setSVar(aVar, "Number$" + value);
            }
        }
        return true;
    }

    private void enusureAbilityHasDescription(SpellAbility ability) {
        if (!StringUtils.isBlank(ability.getStackDescription())) 
            return;
            
        // For older abilities that don't setStackDescription set it here
        final StringBuilder sb = new StringBuilder();
        sb.append(ability.getSourceCard().getName());
        if (ability.getTarget() != null) {
            final ArrayList<Object> targets = ability.getTarget().getTargets();
            if (targets.size() > 0) {
                sb.append(" - Targeting ");
                for (final Object o : targets) {
                    sb.append(o.toString()).append(" ");
                }
            }
        }

        ability.setStackDescription(sb.toString());
    }
}
