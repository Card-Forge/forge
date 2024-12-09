package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

import java.util.Map;
import java.util.function.Predicate;

public class MutateAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        CardCollectionView mutateTgts = CardLists.getTargetableCards(aiPlayer.getCreaturesInPlay(), sa);
        mutateTgts = ComputerUtil.getSafeTargets(aiPlayer, sa, mutateTgts);

        // Filter out some abilities that are useless
        // TODO: add other stuff useless for Mutate here
        mutateTgts = CardLists.filter(mutateTgts, Predicate.not(
                CardPredicates.hasKeyword(Keyword.DEFENDER)
                        .or(CardPredicates.hasKeyword("CARDNAME can't attack."))
                        .or(CardPredicates.hasKeyword("CARDNAME can't block."))
                        .or(card -> ComputerUtilCard.isUselessCreature(aiPlayer, card))
                )
        );

        if (mutateTgts.isEmpty()) {
            return false;
        }

        // Choose the best target
        // TODO: maybe, instead of the standard evaluator, this could inspect the abilities and decide
        // which are better in context, but that's a bit complicated for the time being (not sure if necessary?).
        Card mutateTgt = ComputerUtilCard.getBestCreatureAI(mutateTgts);
        sa.getTargets().add(mutateTgt);

        return true;
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        // Decide which card goes on top here. Pretty rudimentary, feel free to improve.
        Card choice = null;

        for (Card c : options) {
            if (choice == null || c.getBasePower() > choice.getBasePower() || c.getBaseToughness() > choice.getBaseToughness()) {
                choice = c;
            }
        }

        return choice;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
