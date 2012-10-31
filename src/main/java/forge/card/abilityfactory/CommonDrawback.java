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
package forge.card.abilityfactory;

import java.util.Map;

import forge.Card;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Target;

class CommonDrawback extends AbilitySub {
        private final SpellEffect effect;
        private final SpellAiLogic ai;
        private final Map<String, String> params;
        private static final long serialVersionUID = 6631124959690157874L;
        
        public CommonDrawback(final Card ca, final Target t, Map<String, String> params0, SpellEffect effect0, SpellAiLogic ai0) {
            super(ca, t);
            params = params0;
            ai = ai0;
            effect = effect0;
        }
        @Override
        public AbilitySub getCopy() {
            Target t = getTarget() == null ? null : new Target(getTarget()); 
            AbilitySub res = new CommonDrawback(getSourceCard(),t, params, effect, ai);
            CardFactoryUtil.copySpellAbility(this, res);
            return res;
        }
    
        
    
        @Override
        public String getStackDescription() {
            // when getStackDesc is called, just build exactly what is happening
            return effect.getStackDescriptionWithSubs(params, this);
        }
    
        @Override
        public boolean canPlayAI() {
            // if X depends on abCost, the AI needs to choose which card he would sacrifice first
            // then call xCount with that card to properly calculate the amount
            // Or choosing how many to sacrifice
            return ai.canPlayAI(getActivatingPlayer(), params, this);
        }
    
        @Override
        public void resolve() {
            effect.resolve(params, this);
        }
    
        @Override
        public boolean chkAIDrawback() {
            boolean chance = ai.chkAIDrawback(params, this, getActivatingPlayer());
            final AbilitySub subAb = getSubAbility();
            if (subAb != null) {
                chance &= subAb.chkAIDrawback();
            }
            return chance;
        }
    
        @Override
        public boolean doTrigger(final boolean mandatory) {
            return ai.doTriggerAI(getActivatingPlayer(), params, this, mandatory);
        }
    }

    


 // end class AbilityFactory_AlterLife
