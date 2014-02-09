package forge.game.ability;

import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.ChangeZoneAllEffect;
import forge.game.ability.effects.ChangeZoneEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.TargetRestrictions;

import java.util.Map;

public class StaticAbilityApiBased extends AbilityStatic {

    private final SpellAbilityEffect effect;
    private final SpellAbilityAi ai;

    public StaticAbilityApiBased(ApiType api0, Card sourceCard, Cost abCost, TargetRestrictions tgt, Map<String, String> params0) {
        super(sourceCard, abCost, tgt);
        params = params0;
        api = api0;
        effect = api.getSpellEffect();
        ai = api.getAi();

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
    public void resolve() {
        effect.resolve(this);
    }

    @Override
    public boolean canPlayAI(Player aiPlayer) {
        return ai.canPlayAIWithSubs(aiPlayer, this);
    }

    @Override
    public boolean doTrigger(final boolean mandatory, Player aiPlayer) {
        return ai.doTriggerAI(aiPlayer, this, mandatory);
    }
}
