package forge.card.ability.ai;


import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

public class ManaEffectAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        if (ai.getGame().getPhaseHandler().is(PhaseType.MAIN2) && ComputerUtil.ActivateForSacCost(sa, ai)) {
            return true;
        }
        return false;
    }

    /**
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }

}
