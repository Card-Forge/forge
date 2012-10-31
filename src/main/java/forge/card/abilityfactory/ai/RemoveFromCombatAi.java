package forge.card.abilityfactory.ai;

import java.util.Map;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class RemoveFromCombatAi extends SpellAiLogic {
    
    @Override
    public boolean canPlayAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa) {
        // disabled for the AI for now. Only for Gideon Jura at this time.
        return false;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        // AI should only activate this during Human's turn

        // TODO - implement AI
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    public boolean doTriggerAINoCost(Player aiPlayer, Map<String, String> params, SpellAbility sa, boolean mandatory) {
        boolean chance;

        // TODO - implement AI
        chance = false;

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return chance && abSub.doTrigger(mandatory);
        }

        return chance;
    }
}