package forge.ai.ability;


import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpecialAiLogic;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class BranchAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        if ("GrislySigil".equals(aiLogic)) {
            boolean result = SpecialCardAi.GrislySigil.consider(aiPlayer, sa);
            return new AiAbilityDecision(result ? 100 : 0, result ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        } else if ("BranchCounter".equals(aiLogic)) {
            boolean result = SpecialAiLogic.doBranchCounterspellLogic(aiPlayer, sa);
            return new AiAbilityDecision(result ? 100 : 0, result ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        } else if ("TgtAttacker".equals(aiLogic)) {
            final Combat combat = aiPlayer.getGame().getCombat();
            if (combat == null || combat.getAttackingPlayer() != aiPlayer) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            final CardCollection attackers = combat.getAttackers();
            final CardCollection attackingBattle = CardLists.filter(attackers, card -> {
                final GameEntity def = combat.getDefenderByAttacker(combat.getBandOfAttacker(card));
                return def instanceof Card && ((Card)def).isBattle();
            });

            if (!attackingBattle.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(attackingBattle));
            } else {
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(attackers));
            }

            return new AiAbilityDecision(sa.isTargetNumberValid() ? 100 : 0, sa.isTargetNumberValid() ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        }

        // TODO: expand for other cases where the AI is needed to make a decision on a branch
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        AiAbilityDecision decision = canPlay(aiPlayer, sa);
        if (decision.willingToPlay() || mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
