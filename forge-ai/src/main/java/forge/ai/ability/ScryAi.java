package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

import java.util.Random;

public class ScryAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt != null) { // It doesn't appear that Scry ever targets
            // ability is targeted
            sa.resetTargets();

            sa.getTargets().add(ai);
        }

        return true;
    } // scryTargetAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return doTriggerAINoCost(ai, sa, false);
    }


    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // Card source = sa.getHostCard();

        double chance = .4; // 40 percent chance of milling with instant speed
                            // stuff
        if (SpellAbilityAi.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will
                           // never activate EOT)
        }
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (SpellAbilityAi.playReusable(ai, sa)) {
            randomReturn = true;
        }

        return randomReturn;
    }

}
