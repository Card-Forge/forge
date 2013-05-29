package forge.card.ability.ai;

import java.util.List;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardPredicates;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.Game;
import forge.game.phase.PhaseType;
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

public class AnimateAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final Game game = aiPlayer.getGame();
        
        // TODO - add some kind of check to answer
        // "Am I going to attack with this?"
        // TODO - add some kind of check for during human turn to answer
        // "Can I use this to block something?"

        // don't use instant speed animate abilities outside computers
        // Combat_Begin step
        if (!game.getPhaseHandler().is(PhaseType.COMBAT_BEGIN)
                && game.getPhaseHandler().isPlayerTurn(aiPlayer)
                && !SpellAbilityAi.isSorcerySpeed(sa)
                && !sa.hasParam("ActivationPhases") && !sa.hasParam("Permanent")) {
            return false;
        }

        Player opponent = aiPlayer.getWeakestOpponent();
        // don't animate if the AI won't attack anyway
        if (game.getPhaseHandler().isPlayerTurn(aiPlayer)
                && aiPlayer.getLife() < 6
                && opponent.getLife() > 6
                && Iterables.any(opponent.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES)) {
            return false;
        }

        // don't use instant speed animate abilities outside humans
        // Combat_Declare_Attackers_InstantAbility step
        if ((!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                || (game.getCombat().getAttackers().isEmpty()))
                && game.getPhaseHandler().isPlayerTurn(opponent)) {
            return false;
        }

        // don't activate during main2 unless this effect is permanent
        if (game.getPhaseHandler().is(PhaseType.MAIN2) && !sa.hasParam("Permanent") && !sa.hasParam("UntilYourNextTurn")) {
            return false;
        }

        if (null == tgt) {
            final List<Card> defined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

            boolean bFlag = false;
            if (sa.hasParam("AILogic")) {
                if ("EOT".equals(sa.getParam("AILogic"))) {
                    if (game.getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                        return false;
                    } else {
                        bFlag = true;
                    }
                } if ("Never".equals(sa.getParam("AILogic"))) {
                    return false;
                }
            } else for (final Card c : defined) {
                bFlag |= !c.isCreature() && !c.isTapped()
                        && !(c.getTurnInZone() == game.getPhaseHandler().getTurn())
                        && !c.isEquipping();

                // for creatures that could be improved (like Figure of Destiny)
                if (!bFlag && c.isCreature() && (sa.hasParam("Permanent") || (!c.isTapped() && !c.isSick()))) {
                    int power = -5;
                    if (sa.hasParam("Power")) {
                        power = AbilityUtils.calculateAmount(source, sa.getParam("Power"), sa);
                    }
                    int toughness = -5;
                    if (sa.hasParam("Toughness")) {
                        toughness = AbilityUtils.calculateAmount(source, sa.getParam("Toughness"), sa);
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
            if (!animateTgtAI(sa)) {
                return false;
            }
        }

        return true;
    }

    // end animateCanPlayAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        if (sa.getTarget() != null) {
            sa.getTarget().resetTargets();
            if (!animateTgtAI(sa)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * animateTriggerAI.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {

        if (sa.getTarget() != null && !animateTgtAI(sa) && !mandatory) {
            return false;
        }

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with
        // not mandatory as part of the checks to cast something

        return true;
    }

    /**
     * <p>
     * animateTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean animateTgtAI(final SpellAbility sa) {
        // This is reasonable for now. Kamahl, Fist of Krosa and a sorcery or
        // two are the only things
        // that animate a target. Those can just use SVar:RemAIDeck:True until
        // this can do a reasonably
        // good job of picking a good target
        return false;
    }

}
