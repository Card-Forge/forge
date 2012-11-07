package forge.card.abilityfactory;

import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.effects.ChangeZoneAllEffect;
import forge.card.abilityfactory.effects.ChangeZoneEffect;
import forge.card.cost.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.Target;

public class CommonSpell extends Spell {
    private static final long serialVersionUID = -6741797239508483250L;
    private final SpellEffect effect;
    private final Map<String,String> params;
    private final SpellAiLogic ai;

    public CommonSpell(Card sourceCard, Cost abCost, Target tgt, Map<String,String> params0, SpellEffect effect0, SpellAiLogic ai0) {
        super(sourceCard, abCost, tgt);
        params = params0;
        effect = effect0;
        ai = ai0;
        
        if ( effect0 instanceof ChangeZoneEffect || effect0 instanceof ChangeZoneAllEffect )
            AbilityFactory.adjustChangeZoneTarget(params, this);        
    }
    
    @Override
    public String getStackDescription() {
        return effect.getStackDescriptionWithSubs(params, this);
    }
    
    /* (non-Javadoc)
     * @see forge.card.spellability.SpellAbility#resolve()
     */
    
    @Override
    public boolean canPlayAI() {
        return ai.canPlayAIWithSubs(getActivatingPlayer(), params, this) && super.canPlayAI();
    }    
    
    @Override
    public void resolve() {
        effect.resolve(params, this);
    }
    
    @Override
    public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
        boolean chance = false;
        if (withOutManaCost) {
            chance = ai.doTriggerNoCostWithSubs(this.getActivatingPlayer(), params, this, mandatory);
        }
        chance = ai.doTriggerAI(this.getActivatingPlayer(), params, this, mandatory);
        return chance;
    }
}