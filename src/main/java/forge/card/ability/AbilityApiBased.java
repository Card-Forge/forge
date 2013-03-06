package forge.card.ability;

import java.util.Map;

import forge.Card;
import forge.card.ability.effects.ChangeZoneAllEffect;
import forge.card.ability.effects.ChangeZoneEffect;
import forge.card.ability.effects.ManaEffect;
import forge.card.ability.effects.ManaReflectedEffect;
import forge.card.cardfactory.CardFactory;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;

public class AbilityApiBased extends AbilityActivated {
    private final SpellAbilityEffect effect;
    private final SpellAbilityAi ai;

    private static final long serialVersionUID = -4183793555528531978L;

    public AbilityApiBased(ApiType api0, Card sourceCard, Cost abCost, Target tgt, Map<String, String> params0) {
        super(sourceCard, abCost, tgt);
        params = params0;
        api = api0;
        effect = api.getSpellEffect();
        ai = api.getAi();

        if (effect instanceof ManaEffect || effect instanceof ManaReflectedEffect) {

            this.setManaPart(new AbilityManaPart(sourceCard, params));
            this.setUndoable(true); // will try at least
        }

        if (effect instanceof ChangeZoneEffect || effect instanceof ChangeZoneAllEffect) {
            AbilityFactory.adjustChangeZoneTarget(params, this);
        }
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
        AbilityActivated res = new AbilityApiBased(api, getSourceCard(), getPayCosts(), tgt, params);
        CardFactory.copySpellAbility(this, res);
        return res;
    }

    /* (non-Javadoc)
     * @see forge.card.spellability.SpellAbility#resolve()
     */
    @Override
    public void resolve() {
        effect.resolve(this);
    }

    @Override
    public boolean canPlayAI() {
        return ai.canPlayAIWithSubs((AIPlayer)getActivatingPlayer(), this);
    }

    @Override
    public boolean doTrigger(final boolean mandatory, AIPlayer aiPlayer) {
        return ai.doTriggerAI(aiPlayer, this, mandatory);
    }
}
