package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
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

        if (sa.hasParam("AILogic")) {
            if ("SpellMagnet".equals(sa.getParam("AILogic"))) {
                // Cards like Spellskite that retarget spells to itself

                if (topSa == null) {
                    // nothing on stack, so nothing to target
                    return false;
                }
                if (!topSa.usesTargeting() || topSa.getTargets().getTargetCards().contains(sa.getHostCard())) {
                    // if this does not target at all or already targets host, no need to redirect it again
                    return false;
                }
                if (topSa.getHostCard() != null && !topSa.getHostCard().getController().isOpponentOf(aiPlayer)) {
                    // make sure not to redirect our own abilities
                    return false;
                }
                if (!topSa.canTarget(sa.getHostCard())) {
                    // don't try targeting it if we can't legally target Spellskite with it in the first place
                    return false;
                }

                if ("Spellskite".equals(sa.getHostCard().getName())) {
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

        // The AI can't otherwise play this ability, but should at least not miss mandatory activations (e.g. triggers).
        return sa.isMandatory();
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }
}
