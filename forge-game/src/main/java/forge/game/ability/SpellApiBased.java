package forge.game.ability;

import forge.game.ability.effects.ChangeZoneAllEffect;
import forge.game.ability.effects.ChangeZoneEffect;
import forge.game.ability.effects.ManaEffect;
import forge.game.ability.effects.ManaReflectedEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.Spell;
import forge.game.spellability.TargetRestrictions;

import java.util.Map;

public class SpellApiBased extends Spell {
    private static final long serialVersionUID = -6741797239508483250L;
    private final SpellAbilityEffect effect;
    
    public SpellApiBased(ApiType api0, Card sourceCard, Cost abCost, TargetRestrictions tgt, Map<String, String> params0) {
        super(sourceCard, abCost);
        this.setTargetRestrictions(tgt);

        originalMapParams.putAll(params0);
        mapParams.putAll(params0);
        api = api0;
        effect = api.getSpellEffect();

        // A spell is always intrinsic
        this.setIntrinsic(true);

        if (effect instanceof ManaEffect || effect instanceof ManaReflectedEffect) {
            this.setManaPart(new AbilityManaPart(sourceCard, mapParams));
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
     * @see forge.card.spellability.SpellAbility#resolve()
     */
    @Override
    public void resolve() {
        effect.resolve(this);
        getActivatingPlayer().getAchievementTracker().onSpellResolve(this);
    }
}
