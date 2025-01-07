package forge.ai.ability;

import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class AssembleContraptionAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        //Pulls double duty as the OpenAttraction API. Same logic; usually good to do as long as we have the appropriate cards.
        CardCollectionView deck = getDeck(ai, sa);

        if(deck.isEmpty())
            return false;

        if(!super.canPlayAI(ai, sa))
            return false;

        if ("X".equals(sa.getParam("Amount")) && sa.getSVar("X").equals("Count$xPaid")) {
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            xPay = Math.max(xPay, deck.size());
            if (xPay == 0) {
                return false;
            }
            sa.getRootAbility().setXManaCostPaid(xPay);
        }

        if(sa.hasParam("DefinedContraption") && sa.usesTargeting()) {
            return getGoodReassembleTarget(ai, sa) != null;
        }

        return true;
    }

    private static CardCollectionView getDeck(Player ai, SpellAbility sa) {
        return ai.getCardsIn(sa.getApi() == ApiType.OpenAttraction ?
                ZoneType.AttractionDeck : ZoneType.ContraptionDeck);
    }

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        if ("X".equals(sa.getParam("Amount")) && sa.getSVar("X").equals("Count$xPaid")) {
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            if (xPay == 0) {
                return false;
            }
            sa.getRootAbility().setXManaCostPaid(xPay);
        }

        if(sa.hasParam("DefinedContraption") && sa.usesTargeting()) {
            Card target = getGoodReassembleTarget(ai, sa);
            if(target != null)
                sa.getTargets().add(target);
            else
                return false;
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
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph, String logic) {
        if(logic.equals("AtOppEOT"))
            return ph.getNextTurn() == ai && ph.is(PhaseType.END_OF_TURN);

        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        if(getDeck(aiPlayer, sa).isEmpty())
            return false;

        return super.chkAIDrawback(sa, aiPlayer);
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if(!mandatory && getDeck(aiPlayer, sa).isEmpty())
            return false;

        return super.doTriggerAINoCost(aiPlayer, sa, mandatory);
    }
}
