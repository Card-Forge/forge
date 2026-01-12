package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class BecomesBlockedAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Game game = aiPlayer.getGame();

        if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                || !game.getPhaseHandler().getPlayerTurn().isOpponentOf(aiPlayer)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (tgt != null) {
            sa.resetTargets();
            CardCollection list = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), aiPlayer.getOpponents());
            list = CardLists.getTargetableCards(list, sa);
            list = CardLists.getNotKeyword(list, Keyword.TRAMPLE);

            while (sa.canAddMoreTarget()) {
                Card choice = null;

                if (list.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                choice = ComputerUtilCard.getBestCreatureAI(list);

                if (choice == null) { // can't find anything left
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                list.remove(choice);
                sa.getTargets().add(choice);
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        // TODO - implement AI
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        // TODO - implement AI
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
