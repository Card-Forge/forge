package forge.card.abilityfactory.ai;

import java.util.Map;
import java.util.Random;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.util.MyRandom;

public class ScryAi extends SpellAiLogic {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    public boolean doTriggerAINoCost(Player ai, Map<String, String> params, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (tgt != null) { // It doesn't appear that Scry ever targets
            // ability is targeted
            tgt.resetTargets();

            tgt.addTarget(ai);
        }

        return true;
    } // scryTargetAI()

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player ai) {
        return doTriggerAINoCost(ai, params, sa, false);
    }
    
    
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        // Card source = sa.getSourceCard();
    
        double chance = .4; // 40 percent chance of milling with instant speed
                            // stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will
                           // never activate EOT)
        }
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);
    
        if (AbilityFactory.playReusable(ai, sa)) {
            randomReturn = true;
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return randomReturn && abSub.chkAIDrawback();
        }
        return randomReturn;
    }

}