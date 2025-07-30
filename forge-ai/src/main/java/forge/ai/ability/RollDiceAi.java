package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class RollDiceAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision checkApiLogic(Player aiPlayer, SpellAbility sa) {
        Card source = sa.getHostCard();
        Game game = aiPlayer.getGame();
        PhaseHandler ph = game.getPhaseHandler();
        Cost cost = sa.getPayCosts();
        String logic = sa.getParamOrDefault("AILogic", "");

        if (logic.equals("Combat")) {
            boolean result = ph.inCombat() && ((game.getCombat().isAttacking(source) && game.getCombat().isUnblocked(source)) || game.getCombat().isBlocking(source));
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else if (logic.equals("CombatEarly")) {
            boolean result = ph.inCombat() && (game.getCombat().isAttacking(source) || game.getCombat().isBlocking(source));
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else if (logic.equals("Main2")) {
            boolean result = ph.is(PhaseType.MAIN2, aiPlayer);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (cost != null && (sa.getPayCosts().hasManaCost() || sa.getPayCosts().hasTapCost())) {
            boolean result = ph.getNextTurn() == aiPlayer && ph.is(PhaseType.END_OF_TURN);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
