package forge.ai.ability;

import forge.ai.*;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class AirbendAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        // Check own cards that need saving, non-token, above CMC 2 so that it's hopefully worth saving this one
        final Combat combat = aiPlayer.getGame().getCombat();
        final CardCollection threatenedTgts = CardLists.filter(CardLists.getTargetableCards(aiPlayer.getCreaturesInPlay(), sa),
                card -> !card.isToken() && card.getCMC() > 2 &&
                        (ComputerUtil.predictThreatenedObjects(aiPlayer, null, true).contains(card)
                        || (combat != null && ComputerUtilCombat.combatantWouldBeDestroyed(aiPlayer, card, combat))));
        if (!threatenedTgts.isEmpty()) {
            Card bestSaved = ComputerUtilCard.getBestAI(threatenedTgts);
            sa.resetTargets();
            sa.getTargets().add(bestSaved);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        // Check opponent's cards that need bouncing (only in the AI's own turn, main phase 1, or at the end of opponent's
        // turn, to get rid of potential blockers)
        PhaseHandler ph = aiPlayer.getGame().getPhaseHandler();
        if (ph.is(PhaseType.MAIN1, aiPlayer) || (ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == aiPlayer)) {
            final CardCollection opposingThreats = CardLists.getTargetableCards(aiPlayer.getOpponents().getCreaturesInPlay(), sa);
            if (!opposingThreats.isEmpty()) {
                sa.resetTargets();
                sa.getTargets().add(ComputerUtilCard.getBestAI(opposingThreats));
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }

        // TODO: add logic to use it to remove threatening spells when the ability allows to target spells?

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        AiAbilityDecision decision = canPlay(aiPlayer, sa);
        if (decision.willingToPlay() || mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

}
