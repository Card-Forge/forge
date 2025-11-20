package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;

import java.util.List;
import java.util.Map;

public class CopySpellAbilityAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player aiPlayer, SpellAbility sa) {
        Game game = aiPlayer.getGame();
        int chance = AiProfileUtil.getIntProperty(aiPlayer, AiProps.CHANCE_TO_COPY_OWN_SPELL_WHILE_ON_STACK);
        int diff = AiProfileUtil.getIntProperty(aiPlayer, AiProps.ALWAYS_COPY_SPELL_IF_CMC_DIFF);
        String logic = sa.getParamOrDefault("AILogic", "");

        if (game.getStack().isEmpty()) {
            boolean result = sa.isMandatory() || "Always".equals(logic);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
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
                && !"Always".equals(logic)
                && !"AlwaysCopyActivatedAbilities".equals(logic)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (sa.usesTargeting()) {
            // Filter AI-specific targets if provided
            if ("OnlyOwned".equals(sa.getParam("AITgts"))) {
                if (!top.getActivatingPlayer().equals(aiPlayer)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }

            if (top.isWrapper() || top.isActivatedAbility()) {
                // Shouldn't even try with triggered or wrapped abilities at this time, will crash
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (top.getApi() == ApiType.CopySpellAbility) {
                // Don't try to copy a copy ability, too complex for the AI to handle
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (top.getApi() == ApiType.Mana) {
                // would lead to Stack Overflow by trying to play this again
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (top.getApi() == ApiType.DestroyAll || top.getApi() == ApiType.SacrificeAll || top.getApi() == ApiType.ChangeZoneAll || top.getApi() == ApiType.TapAll) {
                if (!top.usesTargeting() || top.getActivatingPlayer().equals(aiPlayer)) {
                    // If we activated a mass removal / mass tap / mass bounce / etc. spell, or if the opponent activated it but
                    // it can't be retargeted, no reason to copy this spell since it'll probably do the same thing and is useless as a copy
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else if (top.hasParam("ConditionManaSpent") || top.getHostCard().hasSVar("AINoCopy")) {
                // Mana spent is not copied, so these spells generally do nothing when copied.
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (SpellApiToAi.Converter.get(top.getApi()) instanceof CannotPlayAi || ComputerUtilCard.isCardRemAIDeck(top.getHostCard())) {
                // Don't try to copy anything you can't understand how to handle
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            // A copy is necessary to properly test the SA before targeting the copied spell, otherwise the copy SA will fizzle.
            final SpellAbility topCopy = top.copy(aiPlayer);
            topCopy.clearManaPaid();
            topCopy.resetTargets();

            if (top.canBeTargetedBy(sa)) {
                AiPlayDecision decision = AiPlayDecision.CantPlaySa;
                if (top instanceof Spell) {
                    decision = ((PlayerControllerAi) aiPlayer.getController()).getAi().canPlayFromEffectAI((Spell) topCopy, false, true);
                } else if (top.isActivatedAbility() && top.getActivatingPlayer().equals(aiPlayer)
                        && logic.contains("CopyActivatedAbilities")) {
                    decision = AiPlayDecision.WillPlay; // FIXME: we activated it once, why not again? Or bad idea?
                }
                if (decision == AiPlayDecision.WillPlay) {
                    sa.getTargets().add(top);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, decision);
            }
        }

        // the AI should not miss mandatory activations
        boolean result = sa.isMandatory() || "Always".equals(logic);
        return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        // the AI should not miss mandatory activations (e.g. Precursor Golem trigger)
        String logic = sa.getParamOrDefault("AILogic", "");

        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if (logic.contains("Always")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player aiPlayer, final SpellAbility sa) {
        if ("ChainOfSmog".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfSmog.consider(aiPlayer, sa);
        }
        if ("ChainOfAcid".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfAcid.consider(aiPlayer, sa);
        }

        AiAbilityDecision decision = canPlay(aiPlayer, sa);
        if (!decision.willingToPlay()) {
            if (sa.isMandatory()) {
                return super.chkDrawback(aiPlayer, sa);
            }
        }
        return decision;
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
            return SpecialCardAi.ChainOfAcid.consider(player, sa).willingToPlay();
        }

        return true;
    }

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final String aiLogic = sa.getParam("UnlessAI");
        if ("Never".equals(aiLogic)) { return false; }

        if (sa.hasParam("UnlessSwitched")) {
            // TODO try without AI Logic flag
            if ("ChainOfVapor".equals(aiLogic)) {
                if (payer.getLandsInPlay().size() < 3) {
                    return false;
                }
                // TODO make better logic in to pick which opponent
                if (payer.getOpponents().getCreaturesInPlay().size() < 0) {
                    return false;
                }
            }
        }
        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }
}
