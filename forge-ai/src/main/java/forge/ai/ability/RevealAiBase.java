package forge.ai.ability;


import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public abstract class RevealAiBase extends SpellAbilityAi {

    protected  boolean revealHandTargetAI(final Player ai, final SpellAbility sa) {
        if (sa.usesTargeting()) {
            // ability is targeted
            sa.resetTargets();

            List<Player> opps = ai.getOpponents();
            opps = Lists.newArrayList(Iterables.filter(opps, PlayerPredicates.isTargetableBy(sa)));

            if (opps.isEmpty()) {
                return false;
            }

            Player p = Collections.max(opps, PlayerPredicates.compareByZoneSize(ZoneType.Hand));

            if (p.getCardsIn(ZoneType.Hand).isEmpty()) {
                return false;
            }
            sa.getTargets().add(p);
        } else {
            // if it's just defined, no big deal
        }

        return true;
    } // revealHandTargetAI()

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        revealHandTargetAI(ai, sa);
        return true;
    }
}
