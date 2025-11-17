package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class EarthbendAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {

        CardCollection nonAnimatedLands = CardLists.filter(aiPlayer.getLandsInPlay(),
                CardPredicates.LANDS.and(CardPredicates.NON_CREATURES));

        if (nonAnimatedLands.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }

        Card bestToAnimate = ComputerUtilCard.getBestLandToAnimate(nonAnimatedLands);
        sa.getTargets().add(bestToAnimate);

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
