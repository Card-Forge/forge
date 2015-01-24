package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Created by friarsol on 1/23/15.
 */
public class ManifestAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        final Game game = ai.getGame();

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (sa.hasParam("AILogic")) {
            if ("Never".equals(sa.getParam("AILogic"))) {
                return false;
            }
        }

        PhaseHandler ph = game.getPhaseHandler();
        // Only manifest things on your turn if sorcery speed, or would pump one of my creatures
        if (ph.isPlayerTurn(ai)) {
            if (ph.getPhase().isBefore(PhaseType.MAIN2)
                    && !sa.hasParam("ActivationPhases")
                    && !ComputerUtil.castSpellInMain1(ai, sa)) {
                boolean buff = false;
                for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                    if ("Creature".equals(c.getSVar("BuffedBy"))) {
                        buff = true;
                    }
                }
                if (!buff) {
                    return false;
                }
            } else if (!SpellAbilityAi.isSorcerySpeed(sa)) {
                return false;
            }
        } else {
        	// try to ambush attackers
        	if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
        		return false;
        	}
        }

        final Card source = sa.getHostCard();

        if (cost != null) {
            // Sacrifice is the only cost Manifest actually has, but i'll leave these others for now
            if (!ComputerUtilCost.checkLifeCost(ai, cost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, cost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, cost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(cost, source)) {
                return false;
            }
        }

        if (source.getSVar("X").equals("Count$xPaid")) {
            // Handle either Manifest X cards, or Manifest 1 card and give it X P1P1s
            // Set PayX here to maximum value.
            int x = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(x));
            if (x <= 0) {
                return false;
            }
        }

		// Probably should be a little more discerning on playing during OPPs turn
        if (SpellAbilityAi.playReusable(ai, sa)) {
            return true;
        }
        if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            // Add blockers?
            return true;
        }
        if (sa.isAbility()) {
            return true;
        }

        return MyRandom.getRandom().nextFloat() < .8;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // Manifest doesn't have any "Pay X to manifest X triggers"

        return true;
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }

}
