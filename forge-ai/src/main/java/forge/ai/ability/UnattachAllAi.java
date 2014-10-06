package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UnattachAllAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();

        if (abCost != null) {
            // No Aura spells have Additional Costs
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= .9;

        // Attach spells always have a target
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
        }

        if (abCost != null && abCost.getTotalMana().countX() > 0 && source.getSVar("X").equals("Count$xPaid")) {
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);

            if (xPay == 0) {
                return false;
            }

            source.setSVar("PayX", Integer.toString(xPay));
        }

        if (ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                && !"Curse".equals(sa.getParam("AILogic"))) {
            return false;
        }

        return chance;
    }


    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card card = sa.getHostCard();
        final Player opp = ai.getOpponent();
        // Check if there are any valid targets
        List<GameObject> targets = new ArrayList<GameObject>();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            targets = AbilityUtils.getDefinedObjects(sa.getHostCard(), sa.getParam("Defined"), sa);
        }

        if (!mandatory && card.isEquipment() && !targets.isEmpty()) {
            Card newTarget = (Card) targets.get(0);
            //don't equip human creatures
            if (newTarget.getController().equals(opp)) {
                return false;
            }

            //don't equip a worse creature
            if (card.isEquipping()) {
                Card oldTarget = card.getEquipping();
                if (ComputerUtilCard.evaluateCreature(oldTarget) > ComputerUtilCard.evaluateCreature(newTarget)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        // AI should only activate this during Human's turn
        return canPlayAI(ai, sa);
    }

}
