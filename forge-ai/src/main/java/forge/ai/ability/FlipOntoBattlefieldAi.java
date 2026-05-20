package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

public class FlipOntoBattlefieldAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        PhaseHandler ph = sa.getHostCard().getGame().getPhaseHandler();
        String logic = sa.getParamOrDefault("AILogic", "");

        if (!isSorcerySpeed(sa, aiPlayer) && sa.getPayCosts().hasManaCost()) {
            if (ph.is(PhaseType.END_OF_TURN)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.WaitForEndOfTurn);
            }
        }

        if ("DamageCreatures".equals(logic)) {
            int maxToughness = Integer.parseInt(sa.getSubAbility().getParam("NumDmg"));
            CardCollectionView rightToughness = CardLists.filter(aiPlayer.getOpponents().getCreaturesInPlay(), card -> card.getNetToughness() <= maxToughness && card.canBeDestroyed());

            if (rightToughness.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
            } else {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }

        if (!aiPlayer.getOpponents().getCardsIn(ZoneType.Battlefield).isEmpty()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return canPlay(aiPlayer, sa);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
