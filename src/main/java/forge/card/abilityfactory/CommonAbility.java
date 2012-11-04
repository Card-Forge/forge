package forge.card.abilityfactory;

import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.effects.ManaEffect;
import forge.card.abilityfactory.effects.ManaReflectedEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.Target;

public class CommonAbility extends AbilityActivated {
    private final SpellEffect effect;
    private final Map<String,String> params;
    private final SpellAiLogic ai;
    
    private static final long serialVersionUID = -4183793555528531978L;

    public CommonAbility(Card sourceCard, Cost abCost, Target tgt, Map<String,String> params0, SpellEffect effect0, SpellAiLogic ai0) {
        super(sourceCard, abCost, tgt);
        params = params0;
        effect = effect0;
        ai = ai0;
        
        if ( effect0 instanceof ManaEffect )
            this.setManaPart(new AbilityManaPart(sourceCard, new Cost(sourceCard, "0", false), params));
        if ( effect0 instanceof ManaReflectedEffect )
            this.setManaPart(new AbilityManaPart(sourceCard, new Cost(sourceCard, "0", false), params));
        
    }
    
    @Override
    public String getStackDescription() {
        return effect.getStackDescriptionWithSubs(params, this);
    }
    
    /* (non-Javadoc)
     * @see forge.card.spellability.AbilityActivated#getCopy()
     */
    @Override
    public AbilityActivated getCopy() {
        Target tgt = getTarget() == null ? null : new Target(getTarget());
        AbilityActivated res = new CommonAbility(getSourceCard(), getPayCosts(), tgt, params, effect, ai);
        CardFactoryUtil.copySpellAbility(this, res);
        return res;
    }

    /* (non-Javadoc)
     * @see forge.card.spellability.SpellAbility#resolve()
     */
    @Override
    public void resolve() {
        effect.resolve(params, this);
    }
    
    @Override
    public boolean canPlayAI() {
        return ai.canPlayAI(getActivatingPlayer(), params, this);
    }

    @Override
    public boolean doTrigger(final boolean mandatory) {
        return ai.doTriggerAI(this.getActivatingPlayer(), params, this, mandatory);
    }
}