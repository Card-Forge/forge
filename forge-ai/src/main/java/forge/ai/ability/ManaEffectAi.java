package forge.ai.ability;


import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ManaEffectAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        if (ai.getGame().getPhaseHandler().is(PhaseType.MAIN2) && ComputerUtil.activateForCost(sa, ai)) {
            return true;
        }
        if (ComputerUtil.playImmediately(ai, sa) && sa.getPayCosts() != null && sa.getPayCosts().hasNoManaCost() 
        		&& sa.getPayCosts().isReusuableResource() 
        		&& sa.getSubAbility() == null) {
        	return true;
        }
        return false;
    }

    /**
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }

}
