package forge.card.abilityfactory.ai;

import java.util.Random;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.util.MyRandom;

public class ScryAi extends SpellAiLogic {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (tgt != null) { // It doesn't appear that Scry ever targets
            // ability is targeted
            tgt.resetTargets();

            tgt.addTarget(ai);
        }

        return true;
    } // scryTargetAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return doTriggerAINoCost(ai, sa, false);
    }
    
    
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
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

        return randomReturn;
    }

}