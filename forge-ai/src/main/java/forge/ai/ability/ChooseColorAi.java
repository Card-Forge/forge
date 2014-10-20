package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

public class ChooseColorAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();
        
        if (!sa.hasParam("AILogic")) {
            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if ("Oona, Queen of the Fae".equals(sa.getHostCard().getName())) {
            PhaseHandler ph = game.getPhaseHandler();
        	if (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
        		return false;
        	}
            // Set PayX here to maximum value.
            int x = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(x));
            return true;
        }
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(ai, sa);
    }

}
