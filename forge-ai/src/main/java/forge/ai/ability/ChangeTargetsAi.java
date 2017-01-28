package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ChangeTargetsAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        final SpellAbility topSa = game.getStack().isEmpty() ? null
                : ComputerUtilAbility.getTopSpellAbilityOnStack(game, sa);

        if ("Self".equals(sa.getParam("DefinedMagnet"))) {
            return doSpellMagnet(sa, topSa, ai);
        }

        // The AI can't otherwise play this ability, but should at least not
        // miss mandatory activations (e.g. triggers).
        return sa.isMandatory();
    }

    private boolean doSpellMagnet(SpellAbility sa, SpellAbility topSa, Player aiPlayer) {
        // For cards like Spellskite that retarget spells to itself
        if (topSa == null) {
            // nothing on stack, so nothing to target
            return false;
        }

        if (sa.getTargets().getNumTargeted() != 0) {
            // something was already chosen before (e.g. in response to a trigger - Mizzium Meddler), so just proceed
            return true;
        }

        if (!topSa.usesTargeting()) {
            // if this does not target at all, nothing to do
            return false;
        }
        if (topSa.getHostCard() != null && !topSa.getHostCard().getController().isOpponentOf(aiPlayer)) {
            // make sure not to redirect our own abilities
            return false;
        }
        if (!sa.canTarget(topSa)) {
            // don't try retargeting a spell that the current card can't legally retarget (e.g. Muck Drubb + Lightning Bolt to the face)
            return false;
        }
        
        // ensure it's a legitimate retarget and not a duplicate
        SpellAbility testSa = topSa;
        boolean canTargetHost = false;
        boolean hasOtherTargets = false;
        boolean isTargetingHost = false;
        boolean hasUniqueTargets = false;
        int numTargeted = 0;
        while (testSa != null) {
            for (GameObject o : testSa.getTargets().getTargets()) {
                numTargeted++;
                if (!o.equals(sa.getHostCard())) {
                    hasOtherTargets = true;
                } else {
                    isTargetingHost = true;
                }
            }
            if (testSa.canTarget(sa.getHostCard())) {
                canTargetHost = true;
            }
            if (testSa.getTargetRestrictions().isUniqueTargets()) {
                hasUniqueTargets = true;
            }
            testSa = testSa.getSubAbility();
        }
        if (!(canTargetHost && hasOtherTargets && (!isTargetingHost || !hasUniqueTargets))) {
            return false;
        }

        if (sa.getPayCosts().getCostMana() != null && sa.getPayCosts().getCostMana().getMana().hasPhyrexian()) {
            ManaCost manaCost = sa.getPayCosts().getCostMana().getMana();
            int payDamage = manaCost.getPhyrexianCount() * 2;
            // e.g. Spellskite or a creature receiving its ability that requires Phyrexian mana P/U
            int potentialDmg = ComputerUtil.predictDamageFromSpell(topSa, aiPlayer);
            ManaCost normalizedMana = manaCost.getNormalizedMana();
            boolean canPay = ComputerUtilMana.canPayManaCost(new ManaCostBeingPaid(normalizedMana), sa, aiPlayer);
            if (potentialDmg > 0 && numTargeted > 1 && !canPay) {
                return false; // AI is not very good at evaluating multitargeted spells and will overpay life, so don't do it for now
            }
            if (potentialDmg != -1 && potentialDmg <= payDamage && !canPay
                    && topSa.getTargets().getTargets().contains(aiPlayer)) {
                // do not pay Phyrexian mana if the spell is a damaging one but it deals less damage or the same damage as we'll pay life
                return false;
            }
        }

        sa.resetTargets();
        sa.getTargets().add(topSa);
        return true;
    }
}
