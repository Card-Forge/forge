package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiAttackController;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;

public class BidLifeAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (tgt.canTgtCreature()) {
                List<Card> list = CardLists.getTargetableCards(AiAttackController.choosePreferredDefenderPlayer(aiPlayer).getCardsIn(ZoneType.Battlefield), sa);
                if (list.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
                Card c = ComputerUtilCard.getBestCreatureAI(list);
                if (sa.canTarget(c)) {
                    sa.getTargets().add(c);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
            } else if (tgt.getZone().contains(ZoneType.Stack)) {
                if (game.getStack().isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                final SpellAbility topSA = game.getStack().peekAbility();
                if (!topSA.isCounterableBy(sa) || aiPlayer.equals(topSA.getActivatingPlayer())) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                if (sa.canTargetSpellAbility(topSA)) {
                    sa.getTargets().add(topSA);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

}
