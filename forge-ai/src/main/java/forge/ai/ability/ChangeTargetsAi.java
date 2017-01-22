package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ChangeTargetsAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
     * (forge.game.player.Player, java.util.Map,
     * forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        final SpellAbility topSa = game.getStack().isEmpty() ? null : ComputerUtilAbility.getTopSpellAbilityOnStack(game, sa);

        if ("Self".equals(sa.getParam("DefinedMagnet"))) {
            return doSpellMagnet(sa, topSa, aiPlayer);
        }

        // The AI can't otherwise play this ability, but should at least not miss mandatory activations (e.g. triggers).
        return sa.isMandatory();
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final Game game = sa.getHostCard().getGame();
        final SpellAbility topSa = game.getStack().isEmpty() ? null : ComputerUtilAbility.getTopSpellAbilityOnStack(game, sa);

        if ("Self".equals(sa.getParam("DefinedMagnet"))) {
            return doSpellMagnet(sa, topSa, aiPlayer);
        }

        return true;
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

        if (!topSa.usesTargeting() || topSa.getTargets().getTargetCards().contains(sa.getHostCard())) {
            // if this does not target at all or already targets host, no need to redirect it again

            // TODO: currently the AI does not know how to change several targets to the same object (e.g.
            // Cone Flame) and will stupidly keep retargeting the first available target unless stopped here.
            // Needs investigation and improvement.
            return false;
        }
        if (topSa.getTargets().getNumTargeted() > 1 && topSa.hasParam("DividedAsYouChoose")) {
            // TODO: currently the AI will crash the game if it tries to change the target of a SA with a
            // divided allocation map. May need improvement.
            return false;
        }

        if (topSa.getHostCard() != null && !topSa.getHostCard().getController().isOpponentOf(aiPlayer)) {
            // make sure not to redirect our own abilities
            return false;
        }
        if (!topSa.canTarget(sa.getHostCard())) {
            // don't try targeting it if we can't legally target the host card with it in the first place
            return false;
        }
        
        if (sa.getPayCosts().getCostMana() != null && "{P/U}".equals(sa.getPayCosts().getCostMana().getMana().getShortString())) {
            // e.g. Spellskite or a creature receiving its ability that requires Phyrexian mana P/U
            int potentialDmg = ComputerUtil.predictDamageFromSpell(topSa, aiPlayer);
            boolean canPayBlue = ComputerUtilMana.canPayManaCost(new ManaCostBeingPaid(new ManaCost(new ManaCostParser("U"))), sa, aiPlayer);
            if (potentialDmg != -1 && potentialDmg <= 2 && !canPayBlue && topSa.getTargets().getTargets().contains(aiPlayer)) {
                // do not pay Phyrexian mana if the spell is a damaging one but it deals less damage or the same damage as we'll pay life
                return false;
            }
        }

        sa.resetTargets();
        sa.getTargets().add(topSa);
        return true;
    }
}
