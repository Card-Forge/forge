package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;

public class PoisonAi extends SpellAiLogic {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
     * (forge.game.player.Player, java.util.Map,
     * forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        // int humanPoison = AllZone.getHumanPlayer().getPoisonCounters();
        // int humanLife = AllZone.getHumanPlayer().getLife();
        // int aiPoison = AllZone.getComputerPlayer().getPoisonCounters();

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        // final int amount =
        // AbilityFactory.calculateAmount(af.getHostCard(),
        // amountStr, sa);

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(ai, abCost, source, 1, null)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }
        }

        // Don't use poison before main 2 if possible
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai.getOpponent());
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.addTarget(ai.getOpponent());
        } else {
            final ArrayList<Player> players = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                    params.get("Defined"), sa);
            for (final Player p : players) {
                if (!mandatory && p.isComputer() && (p.getPoisonCounters() > p.getOpponent().getPoisonCounters())) {
                    return false;
                }
            }
        }

        return true;
    }
}