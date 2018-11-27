package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilityActivated;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

import java.util.List;
import java.util.Map;

public class CopySpellAbilityAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        Game game = aiPlayer.getGame();
        int chance = ((PlayerControllerAi)aiPlayer.getController()).getAi().getIntProperty(AiProps.CHANCE_TO_COPY_OWN_SPELL_WHILE_ON_STACK);
        int diff = ((PlayerControllerAi)aiPlayer.getController()).getAi().getIntProperty(AiProps.ALWAYS_COPY_SPELL_IF_CMC_DIFF);

        if (game.getStack().isEmpty()) {
            return sa.isMandatory();
        }

        final SpellAbility top = game.getStack().peekAbility();
        if (top != null
                && top.getPayCosts() != null && top.getPayCosts().getCostMana() != null
                && sa.getPayCosts() != null && sa.getPayCosts().getCostMana() != null
                && top.getPayCosts().getCostMana().getMana().getCMC() >= sa.getPayCosts().getCostMana().getMana().getCMC() + diff) {
            // The copied spell has a significantly higher CMC than the copy spell, consider copying
            chance = 100;
        }

        if (!MyRandom.percentTrue(chance)
                && !"AlwaysIfViable".equals(sa.getParam("AILogic"))
                && !"OnceIfViable".equals(sa.getParam("AILogic"))) {
            return false;
        }

        if ("OnceIfViable".equals(sa.getParam("AILogic"))) {
            if (AiCardMemory.isRememberedCard(aiPlayer, sa.getHostCard(), AiCardMemory.MemorySet.ACTIVATED_THIS_TURN)) {
                return false;
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {

            // Filter AI-specific targets if provided
            if ("OnlyOwned".equals(sa.getParam("AITgts"))) {
                if (!top.getActivatingPlayer().equals(aiPlayer)) {
                    return false;
                }
            }

            if (top.isWrapper() || !(top instanceof SpellAbility || top instanceof AbilityActivated)) {
                // Should even try with triggered or wrapped abilities first, will crash
                return false;
            } else if (top.getApi() == ApiType.CopySpellAbility) {
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
                    AiCardMemory.rememberCard(aiPlayer, sa.getHostCard(), AiCardMemory.MemorySet.ACTIVATED_THIS_TURN);
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

