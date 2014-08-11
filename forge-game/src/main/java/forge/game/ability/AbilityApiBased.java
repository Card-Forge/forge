package forge.game.ability;

import forge.game.ability.effects.ChangeZoneAllEffect;
import forge.game.ability.effects.ChangeZoneEffect;
import forge.game.ability.effects.ManaEffect;
import forge.game.ability.effects.ManaReflectedEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.cost.Cost;
import forge.game.spellability.AbilityActivated;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.TargetRestrictions;

import java.util.Map;

public class AbilityApiBased extends AbilityActivated {
    private final SpellAbilityEffect effect;

    private static final long serialVersionUID = -4183793555528531978L;

    public AbilityApiBased(ApiType api0, Card sourceCard, Cost abCost, TargetRestrictions tgt, Map<String, String> params0) {
        super(sourceCard, abCost, tgt);
        originalMapParams.putAll(params0);
        mapParams.putAll(params0);
        api = api0;
        effect = api.getSpellEffect();

        if (effect instanceof ManaEffect || effect instanceof ManaReflectedEffect) {

            this.setManaPart(new AbilityManaPart(sourceCard, mapParams));
            this.setUndoable(true); // will try at least
        }

        if (effect instanceof ChangeZoneEffect || effect instanceof ChangeZoneAllEffect) {
            AbilityFactory.adjustChangeZoneTarget(mapParams, this);
        }
    }

    @Override
    public String getStackDescription() {
        return effect.getStackDescriptionWithSubs(mapParams, this);
    }

    /* (non-Javadoc)
     * @see forge.card.spellability.AbilityActivated#getCopy()
     */
    @Override
    public AbilityActivated getCopy() {
        TargetRestrictions tgt = getTargetRestrictions() == null ? null : new TargetRestrictions(getTargetRestrictions());
        AbilityActivated res = new AbilityApiBased(api, getHostCard(), getPayCosts(), tgt, mapParams);
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
}
