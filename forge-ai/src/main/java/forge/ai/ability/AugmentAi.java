package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class AugmentAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        CardCollectionView augmentTgts = CardLists.getTargetableCards(aiPlayer.getYourTeam().getCreaturesInPlay(), sa);
        augmentTgts.removeIf(Card::isAugmented);

        if (augmentTgts.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        Card augmentTgt = ComputerUtilCard.getBestCreatureAI(augmentTgts);
        sa.resetTargets();
        sa.getTargets().add(augmentTgt);

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        CardCollection filteredList = CardLists.filterControlledBy(options, ai.getYourTeam());
        filteredList.removeIf(Card::isAugmented);
        if (!filteredList.isEmpty()) {
            return ComputerUtilCard.getBestAI(filteredList);
        }
        if (isOptional) {
            return null;
        }
        return ComputerUtilCard.getWorstAI(options);
    }
}
