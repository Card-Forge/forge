package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.Random;

import forge.Card;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.util.MyRandom;

public class UnattachAllAi extends SpellAiLogic {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        if (abCost != null) {
            // No Aura spells have Additional Costs
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= .9;

        // Attach spells always have a target
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
        }

        if (abCost != null && abCost.getTotalMana().countX() > 0 && source.getSVar("X").equals("Count$xPaid")) {
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);

            if (xPay == 0) {
                return false;
            }

            source.setSVar("PayX", Integer.toString(xPay));
        }

        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                && !"Curse".equals(sa.getParam("AILogic"))) {
            return false;
        }

        return chance;
    }


    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Card card = sa.getSourceCard();
        final Player opp = ai.getOpponent();
        // Check if there are any valid targets
        ArrayList<Object> targets = new ArrayList<Object>();
        final Target tgt = sa.getTarget();
        if (tgt == null) {
            targets = AbilityUtils.getDefinedObjects(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        if (!mandatory && card.isEquipment() && !targets.isEmpty()) {
            Card newTarget = (Card) targets.get(0);
            //don't equip human creatures
            if (newTarget.getController().equals(opp)) {
                return false;
            }

            //don't equip a worse creature
            if (card.isEquipping()) {
                Card oldTarget = card.getEquipping().get(0);
                if (CardFactoryUtil.evaluateCreature(oldTarget) > CardFactoryUtil.evaluateCreature(newTarget)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        // AI should only activate this during Human's turn
        return canPlayAI(ai, sa);
    }

}
