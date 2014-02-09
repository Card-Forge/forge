package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.List;

public class CloneAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final Game game = source.getGame();

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

        PhaseHandler phase = game.getPhaseHandler();
        // don't use instant speed clone abilities outside computers
        // Combat_Begin step
        if (!phase.is(PhaseType.COMBAT_BEGIN)
                && phase.isPlayerTurn(ai) && !SpellAbilityAi.isSorcerySpeed(sa)
                && !sa.hasParam("ActivationPhases") && !sa.hasParam("Permanent")) {
            return false;
        }

        // don't use instant speed clone abilities outside humans
        // Combat_Declare_Attackers_InstantAbility step
        if (!phase.is(PhaseType.COMBAT_DECLARE_ATTACKERS) || phase.isPlayerTurn(ai) || game.getCombat().getAttackers().isEmpty()) {
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
            sa.resetTargets();
            useAbility &= cloneTgtAI(sa);
        }

        return useAbility;
    } // end cloneCanPlayAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // AI should only activate this during Human's turn
        boolean chance = true;

        if (sa.usesTargeting()) {
            chance = cloneTgtAI(sa);
        }


        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {

        boolean chance = true;

        if (sa.usesTargeting()) {
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
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
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
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // Didn't confirm in the original code
        return false;
    }

}
