package forge.card.abilityfactory.ai;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.util.MyRandom;

public class CounterAi extends SpellAiLogic {
    
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        boolean toReturn = true;
        final Cost abCost = sa.getAbilityFactory().getAbCost();
        final Card source = sa.getSourceCard();
        if (Singletons.getModel().getGame().getStack().size() < 1) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }
            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }

        final Target tgt = sa.getTarget();
        if (tgt != null) {

            final SpellAbility topSA = Singletons.getModel().getGame().getStack().peekAbility();
            if (!CardFactoryUtil.isCounterableBy(topSA.getSourceCard(), sa) || topSA.getActivatingPlayer().isComputer()) {
                return false;
            }
            if (params.containsKey("AITgts") && (topSA.getSourceCard() == null
                    || !topSA.getSourceCard().isValid(params.get("AITgts"), sa.getActivatingPlayer(), source))) {
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
        
        String unlessCost = params.containsKey("UnlessCost") ? params.get("UnlessCost").trim() : null;

        if (unlessCost != null && !unlessCost.endsWith(">")) {
            // Is this Usable Mana Sources? Or Total Available Mana?
            final int usableManaSources = CardFactoryUtil.getUsableManaSources(ai.getOpponent());
            int toPay = 0;
            boolean setPayX = false;
            if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")) {
                setPayX = true;
                toPay = ComputerUtil.determineLeftoverMana(sa, ai);
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
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return doTriggerAINoCost(aiPlayer, params, sa, true);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        boolean toReturn = true;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
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

            String unlessCost = params.containsKey("UnlessCost") ? params.get("UnlessCost").trim() : null;
            
            final Card source = sa.getSourceCard();
            if (unlessCost != null) {
                // Is this Usable Mana Sources? Or Total Available Mana?
                final int usableManaSources = CardFactoryUtil.getUsableManaSources(ai.getOpponent());
                int toPay = 0;
                boolean setPayX = false;
                if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")) {
                    setPayX = true;
                    toPay = ComputerUtil.determineLeftoverMana(sa, ai);
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

        return toReturn;
    }

}