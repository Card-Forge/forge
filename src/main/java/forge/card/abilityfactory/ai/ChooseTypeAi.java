package forge.card.abilityfactory.ai;

import java.util.ArrayList;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class ChooseTypeAi extends SpellAiLogic {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        if (!sa.hasParam("AILogic")) {
            return false;
        }

        return doTriggerAINoCost(aiPlayer, sa, false);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai);
        } else {
            final ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isHuman() && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

}