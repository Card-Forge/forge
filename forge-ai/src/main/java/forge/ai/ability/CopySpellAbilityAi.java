package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class CopySpellAbilityAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return false;
    }

    @Override
    public boolean chkAIDrawback(final SpellAbility sa, final Player aiPlayer) {
        // NOTE: Other SAs that use CopySpellAbilityAi (e.g. Chain Lightning) are currently routed through
        // generic method SpellAbilityAi#chkDrawbackWithSubs and are handled there.

        if ("ChainOfSmog".equals(sa.getParam("AILogic"))) {
            if (aiPlayer.getCardsIn(ZoneType.Hand).size() == 0) {
                // avoid failure to add to stack by providing a legal target
                // TODO: this makes the AI target opponents with 0 cards in hand, but bailing from here causes a
                // "failed to add to stack" error, needs investigation and improvement.
                Player targOpp = aiPlayer.getOpponent(); 

                for (Player opp : aiPlayer.getOpponents()) {
                    if (opp.getCardsIn(ZoneType.Hand).size() > 0) {
                        targOpp = opp;
                        break;
                    }
                }

                sa.getParent().resetTargets();
                sa.getParent().getTargets().add(targOpp);
                return true;
            }
        }

        return super.chkAIDrawback(sa, aiPlayer);
    }

    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells) {
        return spells.get(0);
    }
}

