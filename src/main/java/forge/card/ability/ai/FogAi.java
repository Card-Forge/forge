package forge.card.ability.ai;


import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.ai.ComputerUtilCombat;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;

public class FogAi extends SpellAbilityAi {

    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final GameState game = ai.getGame();
        // AI should only activate this during Human's Declare Blockers phase
        if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())) {
            return false;
        }
        if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            return false;
        }

        // Only cast when Stack is empty, so Human uses spells/abilities first
        if (!game.getStack().isEmpty()) {
            return false;
        }

        // Don't cast it, if the effect is already in place
        if (game.getPhaseHandler().isPreventCombatDamageThisTurn()) {
            return false;
        }

        // Cast it if life is in danger
        return ComputerUtilCombat.lifeInDanger(ai, game.getCombat());
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        // AI should only activate this during Human's turn
        boolean chance;
        final GameState game = ai.getGame();

        // should really check if other player is attacking this player
        if (ai.isOpponentOf(game.getPhaseHandler().getPlayerTurn())) {
            chance = game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        } else {
            chance = game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        final GameState game = aiPlayer.getGame();
        boolean chance;
        if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer().getOpponent())) {
            chance = game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        } else {
            chance = game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
        }

        return chance;
    }
}
