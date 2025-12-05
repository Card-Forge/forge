package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class AssembleContraptionAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        CardCollectionView deck = getDeck(ai, sa);

        if(deck.isEmpty())
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);

        AiAbilityDecision superDecision = super.canPlay(ai, sa);
        if (!superDecision.willingToPlay())
            return superDecision;

        if ("X".equals(sa.getParam("Amount")) && sa.getSVar("X").equals("Count$xPaid")) {
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            xPay = Math.max(xPay, deck.size());
            if (xPay == 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantAffordX);
            }
            sa.getRootAbility().setXManaCostPaid(xPay);
        }

        if(sa.hasParam("DefinedContraption") && sa.usesTargeting()) {
            if (getGoodReassembleTarget(ai, sa) == null) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static CardCollectionView getDeck(Player ai, SpellAbility sa) {
        return ai.getCardsIn(sa.getApi() == ApiType.OpenAttraction ?
                ZoneType.AttractionDeck : ZoneType.ContraptionDeck);
    }

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        if ("X".equals(sa.getParam("Amount")) && sa.getSVar("X").equals("Count$xPaid")) {
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            if (xPay == 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.getRootAbility().setXManaCostPaid(xPay);
        }

        if(sa.hasParam("DefinedContraption") && sa.usesTargeting()) {
            Card target = getGoodReassembleTarget(ai, sa);
            if(target != null)
                sa.getTargets().add(target);
            else
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return super.checkApiLogic(ai, sa);
    }

    private Card getGoodReassembleTarget(Player ai, SpellAbility sa) {
        List<GameEntity> targets = sa.getTargetRestrictions().getAllCandidates(sa, true);
        int nextSprocket = (ai.getCrankCounter() % 3) + 1;
        return targets.stream()
                .filter(e -> {
                    if(!(e instanceof Card))
                        return false;
                    Card c = (Card) e;
                    if(c.getController().isOpponentOf(ai))
                        return true;
                    return c.isContraption() && c.getSprocket() != nextSprocket;
                }).map(c -> (Card) c)
                .findFirst().orElse(null);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if(!mandatory && getDeck(aiPlayer, sa).isEmpty())
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        return super.doTriggerNoCost(aiPlayer, sa, mandatory);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        if(getDeck(aiPlayer, sa).isEmpty())
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        return super.chkDrawback(aiPlayer, sa);
    }
}
