package forge.game.ability;

import java.util.Map;

import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.spellability.AbilityActivated;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.TargetRestrictions;

public class AbilityApiBased extends AbilityActivated {
    private final SpellAbilityEffect effect;

    public AbilityApiBased(ApiType api0, Card sourceCard, Cost abCost, TargetRestrictions tgt, Map<String, String> params0) {
        super(sourceCard, abCost, tgt);
        originalMapParams.putAll(params0);
        mapParams.putAll(params0);
        api = api0;
        effect = api.getSpellEffect();

        if (api.equals(ApiType.Mana) || api.equals(ApiType.ManaReflected)) {
            this.setManaPart(new AbilityManaPart(sourceCard, this, mapParams));
            this.setUndoable(true); // will try at least
        }

        if (api.equals(ApiType.ChangeZone) || api.equals(ApiType.ChangeZoneAll)) {
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
