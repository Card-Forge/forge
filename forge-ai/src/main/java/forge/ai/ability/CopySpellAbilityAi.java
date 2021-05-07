package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.AiCardMemory;
import forge.ai.AiPlayDecision;
import forge.ai.AiProps;
import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

public class CopySpellAbilityAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        Game game = aiPlayer.getGame();
        int chance = ((PlayerControllerAi)aiPlayer.getController()).getAi().getIntProperty(AiProps.CHANCE_TO_COPY_OWN_SPELL_WHILE_ON_STACK);
        int diff = ((PlayerControllerAi)aiPlayer.getController()).getAi().getIntProperty(AiProps.ALWAYS_COPY_SPELL_IF_CMC_DIFF);
        String logic = sa.getParamOrDefault("AILogic", "");

        if (game.getStack().isEmpty()) {
            return sa.isMandatory();
        }

        final SpellAbility top = game.getStack().peekAbility();
        if (top != null
                && top.getPayCosts().getCostMana() != null
                && sa.getPayCosts().getCostMana() != null
                && top.getPayCosts().getCostMana().getMana().getCMC() >= sa.getPayCosts().getCostMana().getMana().getCMC() + diff) {
            // The copied spell has a significantly higher CMC than the copy spell, consider copying
            chance = 100;
        }

        if (top.getActivatingPlayer().isOpponentOf(aiPlayer)) {
            chance = 100; // currently the AI will always copy the opponent's spell if viable
        }

        if (!MyRandom.percentTrue(chance)
                && !"AlwaysIfViable".equals(logic)
                && !"OnceIfViable".equals(logic)
                && !"AlwaysCopyActivatedAbilities".equals(logic)) {
            return false;
        }

        if ("OnceIfViable".equals(logic)) {
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

            if (top.isWrapper() || top.isActivatedAbility()) {
                // Shouldn't even try with triggered or wrapped abilities at this time, will crash
                return false;
            } else if (top.getApi() == ApiType.CopySpellAbility) {
                // Don't try to copy a copy ability, too complex for the AI to handle
                return false;
            } else if (top.getApi() == ApiType.DestroyAll || top.getApi() == ApiType.SacrificeAll || top.getApi() == ApiType.ChangeZoneAll || top.getApi() == ApiType.TapAll || top.getApi() == ApiType.UnattachAll) {
                if (!top.usesTargeting() || top.getActivatingPlayer().equals(aiPlayer)) {
                    // If we activated a mass removal / mass tap / mass bounce / etc. spell, or if the opponent activated it but
                    // it can't be retargeted, no reason to copy this spell since it'll probably do the same thing and is useless as a copy
                    return false;
                }
            } else if (top.hasParam("ConditionManaSpent")) {
                // Mana spent is not copied, so these spells generally do nothing when copied.
                return false;
            } else if (ComputerUtilCard.isCardRemAIDeck(top.getHostCard())) {
                // Don't try to copy anything you can't understand how to handle
                return false;
            }

            // A copy is necessary to properly test the SA before targeting the copied spell, otherwise the copy SA will fizzle.
            final SpellAbility topCopy = top.copy(aiPlayer);
            topCopy.resetTargets();

            if (top.canBeTargetedBy(sa)) {
                AiPlayDecision decision = AiPlayDecision.CantPlaySa;
                if (top instanceof Spell) {
                    decision = ((PlayerControllerAi) aiPlayer.getController()).getAi().canPlayFromEffectAI((Spell) topCopy, true, true);
                } else if (top.isActivatedAbility() && top.getActivatingPlayer().equals(aiPlayer)
                        && logic.contains("CopyActivatedAbilities")) {
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
        return sa.isMandatory() || "Always".equals(logic);
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        // the AI should not miss mandatory activations (e.g. Precursor Golem trigger)
        String logic = sa.getParamOrDefault("AILogic", "");
        return mandatory || logic.contains("Always"); // this includes logic like AlwaysIfViable
    }

    @Override
    public boolean chkAIDrawback(final SpellAbility sa, final Player aiPlayer) {
        if ("ChainOfSmog".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfSmog.consider(aiPlayer, sa);
        } else if ("ChainOfAcid".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfAcid.consider(aiPlayer, sa);
        }

        return canPlayAI(aiPlayer, sa) || (sa.isMandatory() && super.chkAIDrawback(sa, aiPlayer));
    }

    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells,
            Map<String, Object> params) {
        return spells.get(0);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        // Chain of Acid requires special attention here since otherwise the AI will confirm the copy and then
        // run into the necessity of confirming a mandatory Destroy, thus destroying all of its own permanents.
        if ("ChainOfAcid".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfAcid.consider(player, sa);
        }

        return true;
    }

}

