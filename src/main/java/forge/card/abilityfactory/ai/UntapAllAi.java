package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class UntapAllAi extends SpellAiLogic {
    
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return true;
    }

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory;
    }

}