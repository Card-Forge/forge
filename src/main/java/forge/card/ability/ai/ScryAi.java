package forge.card.ability.ai;

import java.util.Random;

import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.util.MyRandom;

public class ScryAi extends SpellAiLogic {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (tgt != null) { // It doesn't appear that Scry ever targets
            // ability is targeted
            tgt.resetTargets();

            tgt.addTarget(ai);
        }

        return true;
    } // scryTargetAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        return doTriggerAINoCost(ai, sa, false);
    }


    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        // Card source = sa.getSourceCard();

        double chance = .4; // 40 percent chance of milling with instant speed
                            // stuff
        if (AbilityUtils.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will
                           // never activate EOT)
        }
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (SpellAiLogic.playReusable(ai, sa)) {
            randomReturn = true;
        }

        return randomReturn;
    }

}
