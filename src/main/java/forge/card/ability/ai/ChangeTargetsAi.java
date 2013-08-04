package forge.card.ability.ai;

import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class ChangeTargetsAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
     * (forge.game.player.Player, java.util.Map,
     * forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }
}
