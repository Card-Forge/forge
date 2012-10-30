package forge.card.abilityfactory.ai;

import java.util.ArrayList;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class ChooseTypeAi extends SpellAiLogic {
    /**
     * <p>
     * chooseTypeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    @Override
    public boolean canPlayAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa) {
        if (!params.containsKey("AILogic")) {
            return false;
        }

        return doTriggerAINoCost(aiPlayer, params, sa, false);
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }

    /**
     * <p>
     * chooseTypeTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    @Override
    public boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai);
        } else {
            final ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isHuman() && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * chooseTypeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
}