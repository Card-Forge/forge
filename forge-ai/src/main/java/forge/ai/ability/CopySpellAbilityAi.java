package forge.ai.ability;

import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilityActivated;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.List;
import java.util.Map;

public class CopySpellAbilityAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        Game game = aiPlayer.getGame();
        if (game.getStack().isEmpty()) {
            return sa.isMandatory(); // FIXME: Are mandatory activations possible in the canPlayAI code path?
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            final SpellAbility top = game.getStack().peekAbility();
            if (top.getApi() == ApiType.CopySpellAbility) {
                // Don't try to copy a copy ability, too complex for the AI to handle
                return false;
            }

            // A copy is necessary to properly test the SA before targeting the copied spell, otherwise the copy SA will fizzle.
            final SpellAbility topCopy = top.copy(aiPlayer);
            topCopy.resetTargets();

            if (top.canBeTargetedBy(sa)) {
                AiPlayDecision decision = AiPlayDecision.CantPlaySa;
                if (top instanceof Spell) {
                    decision = ((PlayerControllerAi) aiPlayer.getController()).getAi().canPlayFromEffectAI((Spell) topCopy, true, true);
                } else if (top instanceof AbilityActivated && top.getActivatingPlayer().equals(aiPlayer)
                        && "CopyActivatedAbilities".equals(sa.getParam("AILogic"))) {
                    decision = AiPlayDecision.WillPlay; // FIXME: we activated it once, why not again? Or bad idea?
                }
                if (decision == AiPlayDecision.WillPlay) {
                    sa.getTargets().add(top);
                    return true;
                }
            }
        }

        // the AI should not miss mandatory activations
        return sa.isMandatory() || "Always".equals(sa.getParam("AILogic"));
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        // the AI should not miss mandatory activations (e.g. Precursor Golem trigger)
        return mandatory || "Always".equals(sa.getParam("AILogic"));
    }

    @Override
    public boolean chkAIDrawback(final SpellAbility sa, final Player aiPlayer) {
        if ("ChainOfSmog".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfSmog.consider(aiPlayer, sa);
        } else if ("ChainOfAcid".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfAcid.consider(aiPlayer, sa);
        }

        return canPlayAI(aiPlayer, sa);
    }

    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells,
            Map<String, Object> params) {
        return spells.get(0);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // Chain of Acid requires special attention here since otherwise the AI will confirm the copy and then
        // run into the necessity of confirming a mandatory Destroy, thus destroying all of its own permanents.
        if ("ChainOfAcid".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfAcid.consider(player, sa);
        }

        return true;
    }

}

