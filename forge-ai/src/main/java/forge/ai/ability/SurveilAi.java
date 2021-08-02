package forge.ai.ability;

import forge.ai.AiCardMemory;
import forge.ai.AiProps;
import forge.ai.ComputerUtilCost;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.cost.CostPayLife;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class SurveilAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * @see forge.ai.SpellAbilityAi#doTriggerAINoCost(forge.game.player.Player, forge.game.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) { // TODO: It doesn't appear that Surveil ever targets, is this necessary?
            sa.resetTargets();
            sa.getTargets().add(ai);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see forge.ai.SpellAbilityAi#chkAIDrawback(forge.game.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return doTriggerAINoCost(ai, sa, false);
    }

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        // if the Surveil ability requires tapping and has a mana cost, it's best done at the end of opponent's turn
        // and right before the beginning of AI's turn, if possible, to avoid mana locking the AI and also to
        // try to scry right before drawing a card. Also, avoid tapping creatures in the AI's turn, if possible,
        // even if there's no mana cost.
        if (sa.getPayCosts().hasTapCost()
                && (sa.getPayCosts().hasManaCost() || (sa.getHostCard() != null && sa.getHostCard().isCreature()))
                && !SpellAbilityAi.isSorcerySpeed(sa)) {
            return ph.getNextTurn() == ai && ph.is(PhaseType.END_OF_TURN);
        }

        // in the player's turn Surveil should only be done in Main1 or in Upkeep if able
        if (ph.isPlayerTurn(ai)) {
            if (SpellAbilityAi.isSorcerySpeed(sa)) {
                return ph.is(PhaseType.MAIN1) || sa.isPwAbility();
            } else {
                return ph.is(PhaseType.UPKEEP);
            }
        }
        return true;
    }

    /**
     * Checks if the AI will play a SpellAbility with the specified AiLogic
     */
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        final Card source = sa.getHostCard();

        if ("Never".equals(aiLogic)) {
            return false;
        } else if ("Once".equals(aiLogic)) {
            return !AiCardMemory.isRememberedCard(ai, source, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN);
        }

        // TODO: add card-specific Surveil AI logic here when/if necessary

        return true;
    }

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        // Makes no sense to do Surveil when there's nothing in the library
        if (ai.getCardsIn(ZoneType.Library).isEmpty()) {
            return false;
        }

        // Only Surveil for life when at decent amount of life remaining
        final Cost cost = sa.getPayCosts();
        if (cost != null && cost.hasSpecificCostType(CostPayLife.class)) {
            final int maxLife = ((PlayerControllerAi)ai.getController()).getAi().getIntProperty(AiProps.SURVEIL_LIFEPERC_AFTER_PAYING_LIFE);
            if (!ComputerUtilCost.checkLifeCost(ai, cost, sa.getHostCard(), ai.getStartingLife() * maxLife / 100, sa)) {
                return false;
            }
        }

        double chance = .4; // 40 percent chance for instant speed
        if (SpellAbilityAi.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will never activate EOT)
        }

        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);
        if (SpellAbilityAi.playReusable(ai, sa)) {
            randomReturn = true;
        }

        if (randomReturn) {
            AiCardMemory.rememberCard(ai, sa.getHostCard(), AiCardMemory.MemorySet.ACTIVATED_THIS_TURN);
        }

        return randomReturn;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
}
