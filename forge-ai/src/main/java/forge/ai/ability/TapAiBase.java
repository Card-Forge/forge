package forge.ai.ability;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.AiAttackController;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public abstract class TapAiBase extends SpellAbilityAi {

    /**
     * <p>
     * tapTargetList.
     * </p>
     * 

     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param tapList
     *            a CardCollection object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean tapTargetList(final Player ai, final SpellAbility sa, final CardCollection tapList, final boolean mandatory) {
        final Card source = sa.getHostCard();

        tapList.removeAll(sa.getTargets().getTargetCards());

        if (tapList.isEmpty()) {
            return false;
        }

        while (sa.canAddMoreTarget()) {
            Card choice = null;

            if (tapList.isEmpty()) {
                if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            choice = ComputerUtilCard.getBestAI(tapList);

            if (choice == null) { // can't find anything left
                if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            tapList.remove(choice);
            sa.getTargets().add(choice);
        }

        return true;
    }

    /**
     * <p>
     * tapPrefTargeting.
     * </p>
     * 
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    protected boolean tapPrefTargeting(final Player ai, final Card source, final SpellAbility sa, final boolean mandatory) {
        final Game game = ai.getGame();
        CardCollection tapList = CardLists.getTargetableCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), sa);
        tapList = CardLists.filter(tapList, Presets.CAN_TAP);
        tapList = CardLists.filter(tapList, c -> {
            if (c.isCreature()) {
                return true;
            }

            for (final SpellAbility sa1 : c.getSpellAbilities()) {
                if (sa1.isAbility() && sa1.getPayCosts().hasTapCost()) {
                    return true;
                }
            }
            return false;
        });

        //use broader approach when the cost is a positive thing
        if (tapList.isEmpty() && ComputerUtil.activateForCost(sa, ai)) { 
            tapList = CardLists.getTargetableCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), sa);
            tapList = CardLists.filter(tapList, c -> {
                if (c.isCreature()) {
                    return true;
                }

                for (final SpellAbility sa12 : c.getSpellAbilities()) {
                    if (sa12.isAbility() && sa12.getPayCosts().hasTapCost()) {
                        return true;
                    }
                }
                return false;
            });
        }

        //try to exclude things that will already be tapped due to something on stack or because something is
        //already targeted in a parent or sub SA
        if (!sa.isTrigger() || mandatory) { // but if just confirming trigger no need to look for other targets and might still help anyway
            CardCollection toExclude = ComputerUtilAbility.getCardsTargetedWithApi(ai, tapList, sa, ApiType.Tap);
            tapList.removeAll(toExclude);
        }

        if (tapList.isEmpty()) {
            return false;
        }

        boolean goodTargets = false;
        while (sa.canAddMoreTarget()) {
            Card choice = null;

            if (tapList.isEmpty()) {
                if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    if (!goodTargets && !ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            PhaseHandler phase = game.getPhaseHandler();
            final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
            Card primeTarget = ComputerUtil.getKilledByTargeting(sa, tapList);
            if (primeTarget != null) {
                choice = primeTarget;
                goodTargets = true;
            } else if (phase.isPlayerTurn(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                // Tap creatures possible blockers before combat during AI's turn.
                List<Card> attackers;
                if (phase.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                    //Combat has already started
                    attackers = game.getCombat().getAttackers();
                } else {
                    attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> CombatUtil.canAttack(c, opp));
                    attackers.remove(source);
                }
                Predicate<Card> findBlockers = CardPredicates.possibleBlockerForAtLeastOne(attackers);
                List<Card> creatureList = CardLists.filter(tapList, findBlockers);

                // TODO check if own creature would be forced to attack and we want to keep it alive

                if (!attackers.isEmpty() && !creatureList.isEmpty()) {
                    choice = ComputerUtilCard.getBestCreatureAI(creatureList);
                } else if (sa.isTrigger() || ComputerUtil.castSpellInMain1(ai, sa)) {
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList);
                }
            } else if (phase.isPlayerTurn(opp)
                    && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                // Tap creatures possible blockers before combat during AI's turn.
                if (Iterables.any(tapList, CardPredicates.Presets.CREATURES)) {
                    List<Card> creatureList = CardLists.filter(tapList, c -> c.isCreature() && CombatUtil.canAttack(c, opp));
                    choice = ComputerUtilCard.getBestCreatureAI(creatureList);
                } else { // no creatures available
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList);
                }
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList);
            }

            if (choice == null) { // can't find anything left
                if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            tapList.remove(choice);
            sa.getTargets().add(choice);
        }

        // Nothing was ever targeted, so we need to bail.
        return sa.getTargets().size() != 0;
    }

    /**
     * <p>
     * tapUnpreferredTargeting.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    protected  boolean tapUnpreferredTargeting(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();

        CardCollection list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);

        // try to tap anything controlled by the computer
        CardCollection tapList = CardLists.filterControlledBy(list, ai.getOpponents());
        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }
        
        if (sa.isMinTargetChosen()) {
            return true;
        }

        // filter by enchantments and planeswalkers, their tapped state (usually) doesn't matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        tapList = CardLists.getValidCards(list, tappablePermanents, source.getController(), source, sa);

        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = CardLists.filter(list, Presets.TAPPED);

        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }
        
        if (sa.isMinTargetChosen()) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        return tapTargetList(ai, sa, tapList, mandatory);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();

        if (!sa.usesTargeting()) {
            if (mandatory) {
                return true;
            }

            final List<Card> pDefined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            // might be from ETBreplacement
            return pDefined.isEmpty() || !pDefined.get(0).isInPlay() || (pDefined.get(0).isUntapped() && pDefined.get(0).getController() != ai);
        } else {
            sa.resetTargets();
            if (tapPrefTargeting(ai, source, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return tapUnpreferredTargeting(ai, sa, mandatory);
            }
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final Card source = sa.getHostCard();
        final boolean oppTargetsChoice = sa.hasParam("TargetingPlayer");

        if (oppTargetsChoice && sa.getActivatingPlayer().equals(ai) && !sa.isTrigger()) {
            // canPlayAI (sa activated by ai)
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            sa.getTargets().clear();
            return targetingPlayer.getController().chooseTargetsFor(sa);
        }

        boolean randomReturn = true;

        if (sa.usesTargeting()) {
            // target section, maybe pull this out?
            sa.resetTargets();
            if (!tapPrefTargeting(ai, source, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

}
