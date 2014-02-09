package forge.game.ability;

import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.ChangeZoneAllEffect;
import forge.game.ability.effects.ChangeZoneEffect;
import forge.game.ability.effects.ManaEffect;
import forge.game.ability.effects.ManaReflectedEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.Spell;
import forge.game.spellability.TargetRestrictions;

import java.util.Map;

public class SpellApiBased extends Spell {
    private static final long serialVersionUID = -6741797239508483250L;
    private final SpellAbilityEffect effect;
    private final SpellAbilityAi ai;

    public SpellApiBased(ApiType api0, Card sourceCard, Cost abCost, TargetRestrictions tgt, Map<String, String> params0) {
        super(sourceCard, abCost);
        this.setTargetRestrictions(tgt);
        
        params = params0;
        api = api0;
        effect = api.getSpellEffect();
        ai = api.getAi();

        if (effect instanceof ManaEffect || effect instanceof ManaReflectedEffect) {
            this.setManaPart(new AbilityManaPart(sourceCard, params));
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
     * @see forge.card.spellability.SpellAbility#resolve()
     */

    @Override
    public boolean canPlayAI(Player aiPlayer) {
        return ai.canPlayAIWithSubs(aiPlayer, this) && super.canPlayAI(aiPlayer);
    }

    @Override
    public void resolve() {
        effect.resolve(this);
    }

    @Override
    public boolean canPlayFromEffectAI(Player aiPlayer, final boolean mandatory, final boolean withOutManaCost) {
        boolean chance = false;
        if (withOutManaCost) {
            chance = ai.doTriggerNoCostWithSubs(aiPlayer, this, mandatory);
        } else {
            chance = ai.doTriggerAI(aiPlayer, this, mandatory);
        }
        return chance && super.canPlayAI(aiPlayer);
    }
}
