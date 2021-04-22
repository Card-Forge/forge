package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;

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
        final TargetChoices topTargets = topSa.getTargets();
        final Card topHost = topSa.getHostCard();

        if (sa.getTargets().size() != 0 && sa.isTrigger()) {
            // something was already chosen before (e.g. in response to a trigger - Mizzium Meddler), so just proceed
            return true;
        }

        if (!topSa.usesTargeting() || topTargets.getTargetCards().contains(sa.getHostCard())) {
            // if this does not target at all or already targets host, no need to redirect it again
            return false;
        }

        for (Card tgt : topTargets.getTargetCards()) {
            if (ComputerUtilAbility.getAbilitySourceName(sa).equals(tgt.getName()) && tgt.getController().equals(aiPlayer)) {
                // We are already targeting at least one card with the same name (e.g. in presence of 2+ Spellskites),
                // no need to retarget again to another one
                return false;
            }
        }

        if (topHost != null && !topHost.getController().isOpponentOf(aiPlayer)) {
            // make sure not to redirect our own abilities
            return false;
        }
        if (!topSa.canTarget(sa.getHostCard())) {
            // don't try targeting it if we can't legally target the host card with it in the first place
            return false;
        }
        if (!sa.canTarget(topSa)) {
            // don't try retargeting a spell that the current card can't legally retarget (e.g. Muck Drubb + Lightning Bolt to the face)
            return false;
        }

        if (sa.getPayCosts().getCostMana() != null && sa.getPayCosts().getCostMana().getMana().hasPhyrexian()) {
            ManaCost manaCost = sa.getPayCosts().getCostMana().getMana();
            int payDamage = manaCost.getPhyrexianCount() * 2;
            // e.g. Spellskite or a creature receiving its ability that requires Phyrexian mana P/U
            int potentialDmg = ComputerUtil.predictDamageFromSpell(topSa, aiPlayer);
            ManaCost normalizedMana = manaCost.getNormalizedMana();
            boolean canPay = ComputerUtilMana.canPayManaCost(new ManaCostBeingPaid(normalizedMana), sa, aiPlayer);
            if (potentialDmg != -1 && potentialDmg <= payDamage && !canPay
                    && topTargets.contains(aiPlayer)) {
                // do not pay Phyrexian mana if the spell is a damaging one but it deals less damage or the same damage as we'll pay life
                return false;
            }
        }

        Card firstCard = topTargets.getFirstTargetedCard();
        // if we're not the target don't intervene unless we can steal a buff
        if (firstCard != null && !aiPlayer.equals(firstCard.getController()) && !topHost.getController().equals(firstCard.getController()) && !topHost.getController().getAllies().contains(firstCard.getController())) {
            return false;
        }
        Player firstPlayer = topTargets.getFirstTargetedPlayer();
        if (firstPlayer != null && !aiPlayer.equals(firstPlayer)) {
            return false;
        }

        sa.resetTargets();
        sa.getTargets().add(topSa);
        return true;
    }
}
