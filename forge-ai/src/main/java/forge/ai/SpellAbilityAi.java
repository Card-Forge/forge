package forge.ai;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Iterables;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

/**
 * Base class for API-specific AI logic
 * <p>
 * The three main methods are canPlayAI(), chkAIDrawback and doTriggerAINoCost.
 */
public abstract class SpellAbilityAi {

    public final boolean canPlayAIWithSubs(final Player aiPlayer, final SpellAbility sa) {
        if (!canPlayAI(aiPlayer, sa)) {
            return false;
        }
        final AbilitySub subAb = sa.getSubAbility();
        return subAb == null || chkDrawbackWithSubs(aiPlayer,  subAb);
    }

    /**
     * Handles the AI decision to play a "main" SpellAbility
     */
    protected boolean canPlayAI(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Cost cost = sa.getPayCosts();
        if (sa.hasParam("AILogic") && !checkAiLogic(ai, sa, sa.getParam("AILogic"))) {
            return false;
        }
        if (cost != null && !willPayCosts(ai, sa, cost, source)) {
            return false;
        }
        if (!checkPhaseRestrictions(ai, sa, ai.getGame().getPhaseHandler())) {
            return false;
        }
        return checkApiLogic(ai, sa);
    }

    /**
     * Checks if the AI will play a SpellAbility with the specified AiLogic
     */
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        return !("Never".equals(aiLogic));
    }

    /**
     * Checks if the AI is willing to pay for additional costs
     * <p>
     * Evaluated costs are: life, discard, sacrifice and counter-removal
     */
    protected boolean willPayCosts(final Player ai, final SpellAbility sa, final Cost cost, final Card source) {
        if (!ComputerUtilCost.checkLifeCost(ai, cost, source, 4, null)) {
            return false;
        }
        if (!ComputerUtilCost.checkDiscardCost(ai, cost, source)) {
            return false;
        }
        if (!ComputerUtilCost.checkSacrificeCost(ai, cost, source)) {
            return false;
        }
        if (!ComputerUtilCost.checkRemoveCounterCost(cost, source)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        return true;
    }
    
    /**
     * The rest of the logic not covered by the canPlayAI template is defined here
     */
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;   // prevent infinite loop
        }
        return MyRandom.getRandom().nextFloat() < .8f;   // random success
    }
    
    public final boolean doTriggerAI(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtilCost.canPayCost(sa, aiPlayer) && !mandatory) {
            return false;
        }

        return doTriggerNoCostWithSubs(aiPlayer, sa, mandatory);
    }

    public final boolean doTriggerNoCostWithSubs(final Player aiPlayer, final SpellAbility sa, final boolean mandatory)
    {
        if (!doTriggerAINoCost(aiPlayer, sa, mandatory)) {
            return false;
        }
        final AbilitySub subAb = sa.getSubAbility();
        return subAb == null || chkDrawbackWithSubs(aiPlayer,  subAb) || mandatory;
    }

    /**
     * Handles the AI decision to play a triggered SpellAbility
     */
    protected boolean doTriggerAINoCost(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        if (canPlayAI(aiPlayer, sa)) {
            return true;
        }
        if (mandatory) {
            final TargetRestrictions tgt = sa.getTargetRestrictions();
            final Player opp = aiPlayer.getOpponent();
            if (tgt != null) {
                if (opp.canBeTargetedBy(sa)) {
                    sa.resetTargets();
                    sa.getTargets().add(opp);
                } else if (aiPlayer.canBeTargetedBy(sa)) {
                	sa.resetTargets();
                    sa.getTargets().add(aiPlayer);
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Handles the AI decision to play a sub-SpellAbility
     */
    public boolean chkAIDrawback(final SpellAbility sa, final Player aiPlayer) {
        return true;
    }

    /**
     * <p>
     * isSorcerySpeed.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    protected static boolean isSorcerySpeed(final SpellAbility sa) {
        return (sa.isSpell() && sa.getHostCard().isSorcery())
            || (sa.isAbility() && sa.getRestrictions().isSorcerySpeed())
            || (sa.getRestrictions().isPwAbility() && !sa.getHostCard().hasKeyword("CARDNAME's loyalty abilities can be activated at instant speed."));
    }

    /**
     * <p>
     * playReusable.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    protected static boolean playReusable(final Player ai, final SpellAbility sa) {
        // TODO probably also consider if winter orb or similar are out

        if (sa.getPayCosts() == null || sa instanceof AbilitySub) {
            return true; // This is only true for Drawbacks and triggers
        }
        
        if (!sa.getPayCosts().isReusuableResource()) {
            return false;
        }
        
        if (ComputerUtil.playImmediately(ai, sa)) {
            return true;
        }
    
        if (sa.getRestrictions().isPwAbility() && ai.getGame().getPhaseHandler().is(PhaseType.MAIN2)) {
            return true;
        }
        if (sa.isSpell() && !sa.isBuyBackAbility()) {
            return false;
        }
        
        PhaseHandler phase = ai.getGame().getPhaseHandler();
        return phase.is(PhaseType.END_OF_TURN) && phase.getNextTurn().equals(ai);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param aiPlayer
     * @param ab
     * @return
     */
    public boolean chkDrawbackWithSubs(Player aiPlayer, AbilitySub ab) {
        final AbilitySub subAb = ab.getSubAbility();
        return SpellApiToAi.Converter.get(ab.getApi()).chkAIDrawback(ab, aiPlayer) && (subAb == null || chkDrawbackWithSubs(aiPlayer, subAb));  
    }
    
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of confirmAction is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T extends GameEntity> T chooseSingleEntity(Player ai, SpellAbility sa, Collection<T> options, boolean isOptional, Player targetedPlayer) {
        T firstOption = Iterables.getFirst(options, null);
        
        if (firstOption instanceof Card) {
            return (T) chooseSingleCard(ai, sa, (Collection<Card>) options, isOptional, targetedPlayer);
        }
        if (firstOption instanceof Player) {
            return (T) chooseSinglePlayer(ai, sa, (Collection<Player>) options);
        }
        return null;
    }

    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSingleSpellAbility is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return spells.get(0);
    }

    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSingleEntity is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return Iterables.getFirst(options, null);

    }
    
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSingleEntity is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return Iterables.getFirst(options, null);
    }


    protected static boolean isUselessCreature(Player ai, Card c) {
        if (c == null) {
            return true;
        }

        if (c.hasKeyword("CARDNAME can't attack or block.")
                || (c.hasKeyword("CARDNAME doesn't untap during your untap step.") && c.isTapped())
                || (c.getOwner() == ai && ai.getOpponents().contains(c.getController()))) {
            return true;
        }

        return false;
    }
}
