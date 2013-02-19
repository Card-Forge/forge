package forge.card.ability.ai;

import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;

public class CloneAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        boolean useAbility = true;

//        if (card.getController().isComputer()) {
//            final List<Card> creatures = AllZoneUtil.getCreaturesInPlay();
//            if (!creatures.isEmpty()) {
//                cardToCopy = CardFactoryUtil.getBestCreatureAI(creatures);
//            }
//        }

        // TODO - add some kind of check to answer
        // "Am I going to attack with this?"
        // TODO - add some kind of check for during human turn to answer
        // "Can I use this to block something?"

        PhaseHandler phase = Singletons.getModel().getGame().getPhaseHandler();
        // don't use instant speed clone abilities outside computers
        // Combat_Begin step
        if (!phase.is(PhaseType.COMBAT_BEGIN)
                && phase.isPlayerTurn(ai) && !SpellAbilityAi.isSorcerySpeed(sa)
                && !sa.hasParam("ActivationPhases") && !sa.hasParam("Permanent")) {
            return false;
        }

        // don't use instant speed clone abilities outside humans
        // Combat_Declare_Attackers_InstantAbility step
        if ((!phase.is(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                || Singletons.getModel().getGame().getCombat().getAttackers().isEmpty())
                && !phase.isPlayerTurn(ai)) {
            return false;
        }

        // don't activate during main2 unless this effect is permanent
        if (phase.is(PhaseType.MAIN2) && !sa.hasParam("Permanent")) {
            return false;
        }

        if (null == tgt) {
            final List<Card> defined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= (!c.isCreature() && !c.isTapped() && !(c.getTurnInZone() == phase.getTurn()));

                // for creatures that could be improved (like Figure of Destiny)
                if (c.isCreature() && (sa.hasParam("Permanent") || (!c.isTapped() && !c.isSick()))) {
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

            if (!bFlag) { // All of the defined stuff is cloned, not very
                          // useful
                return false;
            }
        } else {
            tgt.resetTargets();
            useAbility &= cloneTgtAI(sa);
        }

        return useAbility;
    } // end cloneCanPlayAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer aiPlayer) {
        // AI should only activate this during Human's turn
        boolean chance = true;

        if (sa.getTarget() != null) {
            chance = cloneTgtAI(sa);
        }


        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {

        boolean chance = true;

        if (sa.getTarget() != null) {
            chance = cloneTgtAI(sa);
        }

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with
        // not mandatory as part of the checks to cast something

        return chance || mandatory;
    }

    /**
     * <p>
     * cloneTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean cloneTgtAI(final SpellAbility sa) {
        // This is reasonable for now. Kamahl, Fist of Krosa and a sorcery or
        // two are the only things
        // that clone a target. Those can just use SVar:RemAIDeck:True until
        // this can do a reasonably
        // good job of picking a good target
        return false;
    }

}
