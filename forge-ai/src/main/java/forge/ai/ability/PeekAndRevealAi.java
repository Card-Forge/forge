package forge.ai.ability;

import forge.ai.*;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PeekAndRevealAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParamOrDefault("AILogic", "");
        if ("Main2".equals(logic)) {
            if (aiPlayer.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        // So far this only appears on Triggers, but will expand
        // once things get converted from Dig + NoMove
        Player opp = AiAttackController.choosePreferredDefenderPlayer(aiPlayer);
        Player libraryOwner = aiPlayer;

        if (sa.usesTargeting()) {
            sa.resetTargets();
            //todo: evaluate valid targets
            if (!sa.canTarget(opp)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.getTargets().add(opp);
            libraryOwner = opp;
        }

        if (libraryOwner.getCardsIn(ZoneType.Library).isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if ("X".equals(sa.getParam("PeekAmount")) && sa.getSVar("X").equals("Count$xPaid")) {
            int xPay = ComputerUtilCost.getMaxXValue(sa, aiPlayer, sa.isTrigger());
            if (xPay == 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.getRootAbility().setXManaCostPaid(xPay);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if ("InstantOrSorcery".equals(sa.getParam("AILogic"))) {
            CardCollection revealed = (CardCollection) params.get("Revealed");
            for (Card c : revealed) {
                if (!c.isInstant() && !c.isSorcery()) {
                    return false;
                }
            }
        }

        AbilitySub subAb = sa.getSubAbility();
        return subAb != null && SpellApiToAi.Converter.get(subAb).chkDrawbackWithSubs(player, subAb).willingToPlay();
    }

}
