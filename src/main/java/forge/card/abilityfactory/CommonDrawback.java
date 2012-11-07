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
import forge.card.abilityfactory.effects.ChangeZoneAllEffect;
import forge.card.abilityfactory.effects.ChangeZoneEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Target;

public class CommonDrawback extends AbilitySub {
        private final SpellEffect effect;
        private final SpellAiLogic ai;
        private final Map<String, String> params;
        private static final long serialVersionUID = 6631124959690157874L;
        
        public CommonDrawback(final Card ca, final Target t, Map<String, String> params0, SpellEffect effect0, SpellAiLogic ai0) {
            super(ca, t);
            params = params0;
            ai = ai0;
            effect = effect0;
            
            if ( effect0 instanceof ChangeZoneEffect || effect0 instanceof ChangeZoneAllEffect )
                AbilityFactory.adjustChangeZoneTarget(params, this);            
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
            return effect.getStackDescriptionWithSubs(params, this);
        }
    
        @Override
        public boolean canPlayAI() {
            return ai.canPlayAIWithSubs(getActivatingPlayer(), params, this);
        }
    
        @Override
        public void resolve() {
            effect.resolve(params, this);
        }
    
        @Override
        public boolean chkAIDrawback() {
            if (!ai.chkAIDrawback(params, this, getActivatingPlayer())) {
                return false;
            }
            final AbilitySub subAb = getSubAbility();
            if (subAb != null && !subAb.chkAIDrawback()) {
                return false;
            }
            return true;
        }
    
        @Override
        public boolean doTrigger(final boolean mandatory) {
            return ai.doTriggerAI(getActivatingPlayer(), params, this, mandatory);
        }
    }

    


 // end class AbilityFactory_AlterLife
