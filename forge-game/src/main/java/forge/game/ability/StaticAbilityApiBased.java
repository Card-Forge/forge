package forge.game.ability;

import forge.game.ability.effects.ChangeZoneAllEffect;
import forge.game.ability.effects.ChangeZoneEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.TargetRestrictions;

import java.util.Map;

public class StaticAbilityApiBased extends AbilityStatic {

    private final SpellAbilityEffect effect;

    public StaticAbilityApiBased(ApiType api0, Card sourceCard, Cost abCost, TargetRestrictions tgt, Map<String, String> params0) {
        super(sourceCard, abCost, tgt);
        originalMapParams.putAll(params0);
        mapParams.putAll(params0);
        api = api0;
        effect = api.getSpellEffect();

        if (effect instanceof ChangeZoneEffect || effect instanceof ChangeZoneAllEffect) {
            AbilityFactory.adjustChangeZoneTarget(mapParams, this);
        }
    }

    @Override
    public String getStackDescription() {
        return effect.getStackDescriptionWithSubs(mapParams, this);
    }


    /* (non-Javadoc)
     * @see forge.card.spellability.SpellAbility#resolve()
     */
    @Override
    public void resolve() {
        effect.resolve(this);
    }
}
