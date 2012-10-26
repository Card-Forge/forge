package forge.card.abilityfactory;

import java.util.Map;

import forge.Card;
import forge.card.cost.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.Target;

public class UniversalSpell extends Spell {
    private static final long serialVersionUID = -6741797239508483250L;
    private final SpellEffect effect;
    private final Map<String,String> params;
    private final SpellAiLogic ai;

    public UniversalSpell(Card sourceCard, Cost abCost, Target tgt, Map<String,String> params0, SpellEffect effect0, SpellAiLogic ai0) {
        super(sourceCard, abCost, tgt);
        params = params0;
        effect = effect0;
        ai = ai0;
    }
    
    @Override
    public String getStackDescription() {
        return effect.getStackDescription(params, this);
    }
    
    /* (non-Javadoc)
     * @see forge.card.spellability.SpellAbility#resolve()
     */
    
    @Override
    public boolean canPlayAI() {
        return ai.canPlayAI(getActivatingPlayer(), params, this);
    }    
    
    @Override
    public void resolve() {
        effect.resolve(params, this);
    }
    
    @Override
    public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
        if (withOutManaCost) {
            return ai.doTriggerAINoCost(this.getActivatingPlayer(), params, this, mandatory);
        }
        return ai.doTriggerAI(this.getActivatingPlayer(), params, this, mandatory);
    }
}