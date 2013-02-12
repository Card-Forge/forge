package forge.card.ability.ai;

import forge.card.ability.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;

public class UntapAllAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory;
    }

}
