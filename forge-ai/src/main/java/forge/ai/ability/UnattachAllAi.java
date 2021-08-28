package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

public class UnattachAllAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // prevent run-away activations - first time will always return true
        boolean chance = MyRandom.getRandom().nextFloat() <= .9;

        // Attach spells always have a target
        if (sa.usesTargeting()) {
            sa.resetTargets();
        }

        if (sa.getSVar("X").equals("Count$xPaid")) {
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);

            if (xPay == 0) {
                return false;
            }

            sa.setXManaCostPaid(xPay);
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
        // Check if there are any valid targets
        List<GameObject> targets = new ArrayList<>();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            targets = AbilityUtils.getDefinedObjects(sa.getHostCard(), sa.getParam("Defined"), sa);
        }

        if (!mandatory && card.isEquipment() && !targets.isEmpty()) {
            Card newTarget = (Card) targets.get(0);
            //don't equip opponent creatures
            if (!newTarget.getController().equals(ai)) {
                return false;
            }

            //don't equip a worse creature
            if (card.isEquipping()) {
                Card oldTarget = card.getEquipping();
                return ComputerUtilCard.evaluateCreature(oldTarget) <= ComputerUtilCard.evaluateCreature(newTarget);
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
