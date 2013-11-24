package forge.game.ability;

import java.util.Map;

import forge.card.cardfactory.CardFactory;
import forge.game.ability.effects.ChangeZoneAllEffect;
import forge.game.ability.effects.ChangeZoneEffect;
import forge.game.ability.effects.ManaEffect;
import forge.game.ability.effects.ManaReflectedEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.AbilityActivated;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.TargetRestrictions;

public class AbilityApiBased extends AbilityActivated {
    private final SpellAbilityEffect effect;
    private final SpellAbilityAi ai;

    private static final long serialVersionUID = -4183793555528531978L;

    public AbilityApiBased(ApiType api0, Card sourceCard, Cost abCost, TargetRestrictions tgt, Map<String, String> params0) {
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
        TargetRestrictions tgt = getTargetRestrictions() == null ? null : new TargetRestrictions(getTargetRestrictions());
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
        return ai.canPlayAIWithSubs(getActivatingPlayer(), this);
    }

    @Override
    public boolean doTrigger(final boolean mandatory, Player aiPlayer) {
        return ai.doTriggerAI(aiPlayer, this, mandatory);
    }
}
