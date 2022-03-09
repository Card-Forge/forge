package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public abstract class RevealAiBase extends SpellAbilityAi {

    protected  boolean revealHandTargetAI(final Player ai, final SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            // ability is targeted
            sa.resetTargets();

            PlayerCollection opps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));

            if (opps.isEmpty()) {
                if (mandatory && sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                    return true;
                }
                return false;
            }

            Player p = opps.max(PlayerPredicates.compareByZoneSize(ZoneType.Hand));

            if (!mandatory && p.getCardsIn(ZoneType.Hand).isEmpty()) {
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
        revealHandTargetAI(ai, sa, false);
        return true;
    }
}
