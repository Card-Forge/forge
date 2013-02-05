package forge.card.abilityfactory.ai;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.player.AIPlayer;
import forge.util.MyRandom;

public class CounterAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        boolean toReturn = true;
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        if (Singletons.getModel().getGame().getStack().isEmpty()) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }

        final Target tgt = sa.getTarget();
        if (tgt != null) {

            final SpellAbility topSA = Singletons.getModel().getGame().getStack().peekAbility();
            if (!CardFactoryUtil.isCounterableBy(topSA.getSourceCard(), sa) || topSA.getActivatingPlayer().isComputer()) {
                return false;
            }
            if (sa.hasParam("AITgts") && (topSA.getSourceCard() == null
                    || !topSA.getSourceCard().isValid(sa.getParam("AITgts"), sa.getActivatingPlayer(), source))) {
                return false;
            }

            tgt.resetTargets();
            if (TargetSelection.matchSpellAbility(sa, topSA, tgt)) {
                tgt.addTarget(topSA);
            } else {
                return false;
            }
        } else {
            return false;
        }

        String unlessCost = sa.hasParam("UnlessCost") ? sa.getParam("UnlessCost").trim() : null;

        if (unlessCost != null && !unlessCost.endsWith(">")) {
            // Is this Usable Mana Sources? Or Total Available Mana?
            final int usableManaSources = CardFactoryUtil.getUsableManaSources(ai.getOpponent());
            int toPay = 0;
            boolean setPayX = false;
            if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")) {
                setPayX = true;
                toPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            } else {
                toPay = AbilityFactory.calculateAmount(source, unlessCost, sa);
            }

            if (toPay == 0) {
                return false;
            }

            if (toPay <= usableManaSources) {
                // If this is a reusable Resource, feel free to play it most of
                // the time
                if (!sa.getPayCosts().isReusuableResource() || sa.isSpell()) {
                    return false;
                }
            }

            if (setPayX) {
                source.setSVar("PayX", Integer.toString(toPay));
            }
        }

        // TODO Improve AI

        // Will return true if this spell can counter (or is Reusable and can
        // force the Human into making decisions)

        // But really it should be more picky about how it counters things


        return toReturn;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer aiPlayer) {
        return doTriggerAINoCost(aiPlayer, sa, true);
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            if (Singletons.getModel().getGame().getStack().isEmpty()) {
                return false;
            }
            final SpellAbility topSA = Singletons.getModel().getGame().getStack().peekAbility();
            if (!CardFactoryUtil.isCounterableBy(topSA.getSourceCard(), sa) || topSA.getActivatingPlayer().isComputer()) {
                return false;
            }

            tgt.resetTargets();
            if (TargetSelection.matchSpellAbility(sa, topSA, tgt)) {
                tgt.addTarget(topSA);
            } else {
                return false;
            }

            String unlessCost = sa.hasParam("UnlessCost") ? sa.getParam("UnlessCost").trim() : null;

            final Card source = sa.getSourceCard();
            if (unlessCost != null) {
                // Is this Usable Mana Sources? Or Total Available Mana?
                final int usableManaSources = CardFactoryUtil.getUsableManaSources(ai.getOpponent());
                int toPay = 0;
                boolean setPayX = false;
                if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")) {
                    setPayX = true;
                    toPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
                } else {
                    toPay = AbilityFactory.calculateAmount(source, unlessCost, sa);
                }

                if (toPay == 0) {
                    return false;
                }

                if (toPay <= usableManaSources) {
                    // If this is a reusable Resource, feel free to play it most
                    // of the time
                    if (!sa.getPayCosts().isReusuableResource() || (MyRandom.getRandom().nextFloat() < .4)) {
                        return false;
                    }
                }

                if (setPayX) {
                    source.setSVar("PayX", Integer.toString(toPay));
                }
            }
        }

        // TODO Improve AI

        // Will return true if this spell can counter (or is Reusable and can
        // force the Human into making decisions)

        // But really it should be more picky about how it counters things

        return true;
    }

}
