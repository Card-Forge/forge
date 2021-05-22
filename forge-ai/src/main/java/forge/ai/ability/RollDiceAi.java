package forge.ai.ability;


import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

public class RollDiceAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        Card source = sa.getHostCard();
        Game game = aiPlayer.getGame();
        PhaseHandler ph = game.getPhaseHandler();
        Cost cost = sa.getPayCosts();
        String logic = sa.getParamOrDefault("AILogic", "");

        if (logic.equals("Combat")) {
            return game.getCombat() != null && ((game.getCombat().isAttacking(source) && game.getCombat().isUnblocked(source)) || game.getCombat().isBlocking(source));
        } else if (logic.equals("CombatEarly")) {
            return game.getCombat() != null && (game.getCombat().isAttacking(source) || game.getCombat().isBlocking(source));
        } else if (logic.equals("Main2")) {
            return ph.is(PhaseType.MAIN2, aiPlayer);
        } else if (logic.equals("AtOppEOT")) {
            return ph.getNextTurn() == aiPlayer && ph.is(PhaseType.END_OF_TURN);
        }

        if (cost != null && (sa.getPayCosts().hasManaCost() || sa.getPayCosts().hasTapCost())) {
            return ph.getNextTurn() == aiPlayer && ph.is(PhaseType.END_OF_TURN);
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
}
