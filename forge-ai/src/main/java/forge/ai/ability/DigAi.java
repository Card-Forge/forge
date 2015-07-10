package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;


public class DigAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        Player opp = ai.getOpponent();
        final Card host = sa.getHostCard();
        Player libraryOwner = ai;

        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (!opp.canBeTargetedBy(sa)) {
                return false;
            } else {
                sa.getTargets().add(opp);
            }
            libraryOwner = opp;
        }

        // return false if nothing to dig into
        if (libraryOwner.getCardsIn(ZoneType.Library).isEmpty()) {
            return false;
        }

        if ("Never".equals(sa.getParam("AILogic"))) {
            return false;
        }

        // don't deck yourself
        if (sa.hasParam("DestinationZone2") && !"Library".equals(sa.getParam("DestinationZone2"))) {
            int numToDig = AbilityUtils.calculateAmount(host, sa.getParam("DigNum"), sa);
            if (libraryOwner == ai && ai.getCardsIn(ZoneType.Library).size() <= numToDig + 2) {
                return false;
            }
        }

        // Don't use draw abilities before main 2 if possible
        if (game.getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases")
                && !sa.hasParam("DestinationZone") && !ComputerUtil.castSpellInMain1(ai, sa)) {
            return false;
        }

        final String num = sa.getParam("DigNum");
        if (num != null && num.equals("X") && host.getSVar(num).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            if (!(sa instanceof AbilitySub) || host.getSVar("PayX").equals("")) {
                int numCards = ComputerUtilMana.determineLeftoverMana(sa, ai);
                if (numCards <= 0) {
                    return false;
                }
                host.setSVar("PayX", Integer.toString(numCards));
            }
        }

        if (SpellAbilityAi.playReusable(ai, sa)) {
            return true;
        }

        if ((!game.getPhaseHandler().getNextTurn().equals(ai)
                || game.getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN))
            && !sa.hasParam("PlayerTurn") && !SpellAbilityAi.isSorcerySpeed(sa)
            && (ai.getCardsIn(ZoneType.Hand).size() > 1 || game.getPhaseHandler().getPhase().isBefore(PhaseType.DRAW))
            && !ComputerUtil.activateForCost(sa, ai)) {
        	return false;
        }

        return !ComputerUtil.preventRunAwayActivations(sa);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Player opp = ai.getOpponent();
        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (mandatory && sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else if (mandatory && sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            }
        }

        return true;
    }
    
    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> valid, boolean isOptional, Player relatedPlayer) {
        Card chosen = ComputerUtilCard.getBestAI(valid);
        if (sa.getActivatingPlayer().isOpponentOf(ai) && relatedPlayer.isOpponentOf(ai)) {
            return ComputerUtilCard.getWorstAI(valid);
        }
        return chosen;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // looks like perfect code for Delver of Secrets, but what about other cards? 
        Card topc = player.getZone(ZoneType.Library).get(0);
        return topc.isInstant() || topc.isSorcery();
    }
}
