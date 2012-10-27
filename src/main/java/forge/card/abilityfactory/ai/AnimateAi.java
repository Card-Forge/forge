package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactoryAnimate class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryAnimate.java 17608 2012-10-20 22:27:27Z Max mtg $
 */

public class AnimateAi extends SpellAiLogic {

    // **************************************************************
    // ************************** Animate ***************************
    // **************************************************************
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player aiPlayer, Map<String, String> params, SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
    
        boolean useAbility = true;
    
        // TODO - add some kind of check to answer
        // "Am I going to attack with this?"
        // TODO - add some kind of check for during human turn to answer
        // "Can I use this to block something?"
    
        // don't use instant speed animate abilities outside computers
        // Combat_Begin step
        if (!Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_BEGIN)
                && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(aiPlayer) 
                && !AbilityFactory.isSorcerySpeed(sa)
                && !params.containsKey("ActivationPhases") && !params.containsKey("Permanent")) {
            return false;
        }
    
        Player opponent = aiPlayer.getOpponent();
        // don't animate if the AI won't attack anyway
        if (Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(aiPlayer)
                && aiPlayer.getLife() < 6 
                && opponent.getLife() > 6
                && Iterables.any(opponent.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES)) {
            return false;
        }
    
        // don't use instant speed animate abilities outside humans
        // Combat_Declare_Attackers_InstantAbility step
        if ((!Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY) 
                || (Singletons.getModel().getGame().getCombat().getAttackers().isEmpty())) 
                && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(opponent)) {
            return false;
        }
    
        // don't activate during main2 unless this effect is permanent
        if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.MAIN2) && !params.containsKey("Permanent")) {
            return false;
        }
    
        if (null == tgt) {
            final ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
    
            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= (!c.isCreature() && !c.isTapped() && !(c.getTurnInZone() == Singletons.getModel().getGame().getPhaseHandler().getTurn()));
    
                // for creatures that could be improved (like Figure of Destiny)
                if (c.isCreature() && (params.containsKey("Permanent") || (!c.isTapped() && !c.isSick()))) {
                    int power = -5;
                    if (params.containsKey("Power")) {
                        power = AbilityFactory.calculateAmount(source, params.get("Power"), sa);
                    }
                    int toughness = -5;
                    if (params.containsKey("Toughness")) {
                        toughness = AbilityFactory.calculateAmount(source, params.get("Toughness"), sa);
                    }
                    if ((power + toughness) > (c.getCurrentPower() + c.getCurrentToughness())) {
                        bFlag = true;
                    }
                }
    
            }
    
            if (!bFlag) { // All of the defined stuff is animated, not very
                          // useful
                return false;
            }
        } else {
            tgt.resetTargets();
            useAbility &= animateTgtAI(params, sa);
        }
    
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            useAbility &= subAb.chkAIDrawback();
        }
    
        return useAbility;
    }

    // end animateCanPlayAI()
    
    
    @Override
    public boolean chkAIDrawback(Map<String, String> params, SpellAbility sa) {
        boolean chance = true;
    
        if (sa.getTarget() != null) {
            chance = animateTgtAI(params, sa);
        }
    
        return chance;
    }

    /**
     * <p>
     * animateTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    @Override
    public boolean doTriggerAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, aiPlayer)) { // If there is a cost payment
            return false;
        }
    
        boolean chance = true;
    
        if (sa.getTarget() != null) {
            chance = animateTgtAI(params, sa);
        }
    
        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice
    
        // Eventually, we can call the trigger of ETB abilities with
        // not mandatory as part of the checks to cast something
    
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }
    
        return chance || mandatory;
    }

    /**
     * <p>
     * animateTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean animateTgtAI(final Map<String, String> params, final SpellAbility sa) {
        // This is reasonable for now. Kamahl, Fist of Krosa and a sorcery or
        // two are the only things
        // that animate a target. Those can just use SVar:RemAIDeck:True until
        // this can do a reasonably
        // good job of picking a good target
        return false;
    }
    
}