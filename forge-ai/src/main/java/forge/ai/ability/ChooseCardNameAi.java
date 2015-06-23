package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class ChooseCardNameAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        Card source = sa.getHostCard();
        if (sa.hasParam("AILogic")) {
            // Don't tap creatures that may be able to block
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
            }

            String logic = sa.getParam("AILogic");
            if (logic.equals("MomirAvatar")) {
                if (source.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN1)) {
                    return false;
                }
                // Set PayX here to maximum value.
                int tokenSize = ComputerUtilMana.determineLeftoverMana(sa, ai);
                
             // Some basic strategy for Momir
                if (tokenSize < 2) {
                    return false;
                }

                if (tokenSize > 11) {
                    tokenSize = 11;
                }

                source.setSVar("PayX", Integer.toString(tokenSize));
            }

            final TargetRestrictions tgt = sa.getTargetRestrictions();
            if (tgt != null) {
                sa.resetTargets();
                if (tgt.canOnlyTgtOpponent()) {
                    sa.getTargets().add(ai.getOpponent());
                } else {
                    sa.getTargets().add(ai);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        // TODO - there is no AILogic implemented yet
        return false;
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer) {
        return ComputerUtilCard.getBestAI(options);
    }

}
