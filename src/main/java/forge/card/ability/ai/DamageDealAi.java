package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class DamageDealAi extends DamageAiBase {
    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getSourceCard(), damage, sa);

        final Card source = sa.getSourceCard();

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }
        if (!this.damageTargetAI(ai, sa, dmg)) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {

        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getSourceCard(), damage, sa);

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }

        if (dmg <= 0) {
            return false;
        }

        // temporarily disabled until better AI
        if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
            return false;
        }

        if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
            return false;
        }

        if (source.getName().equals("Stuffy Doll")) {
            // Now stuffy sits around for blocking
            // TODO(sol): this should also happen if Stuffy is going to die
            return Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.END_OF_TURN, ai.getOpponent());
        }

        if (sa.isAbility()) {
            final Random r = MyRandom.getRandom(); // prevent run-away
                                                   // activations
            if (r.nextFloat() > Math.pow(.9, sa.getActivationsThisTurn())) {
                return false;
            }
        }

        if (!this.damageTargetAI(ai, sa, dmg)) {
            return false;
        }

        if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")) {
            // If I can kill my target by paying less mana, do it
            final Target tgt = sa.getTarget();
            if (tgt != null && tgt.getTargetPlayers().isEmpty() && !sa.hasParam("DividedAsYouChoose")) {
                int actualPay = 0;
                final boolean noPrevention = sa.hasParam("NoPrevention");
                final ArrayList<Card> cards = tgt.getTargetCards();
                for (final Card c : cards) {
                    final int adjDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                    if ((adjDamage > actualPay) && (adjDamage <= dmg)) {
                        actualPay = adjDamage;
                    }
                }
                source.setSVar("PayX", Integer.toString(actualPay));
            }
        }
        return true;
    }

    /**
     * <p>
     * dealDamageChooseTgtC.
     * </p>
     * 
     * @param d
     *            a int.
     * @param noPrevention
     *            a boolean.
     * @param pl
     *            a {@link forge.game.player.Player} object.
     * @param mandatory
     *            a boolean.
     * @return a {@link forge.Card} object.
     */
    private Card dealDamageChooseTgtC(final Player ai, final SpellAbility saMe, final int d, final boolean noPrevention,
            final Player pl, final boolean mandatory) {

        // wait until stack is empty (prevents duplicate kills)
        if (!saMe.isTrigger() && !Singletons.getModel().getGame().getStack().isEmpty()) {
            return null;
        }
        final Target tgt = saMe.getTarget();
        final Card source = saMe.getSourceCard();
        List<Card> hPlay = CardLists.getValidCards(pl.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, source);

        final ArrayList<Object> objects = tgt.getTargets();
        if (saMe.hasParam("TargetUnique")) {
            objects.addAll(TargetSelection.getUniqueTargets(saMe));
        }
        for (final Object o : objects) {
            if (o instanceof Card) {
                final Card c = (Card) o;
                if (hPlay.contains(c)) {
                    hPlay.remove(c);
                }
            }
        }
        hPlay = CardLists.getTargetableCards(hPlay, saMe);

        final List<Card> killables = CardLists.filter(hPlay, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return (ComputerUtilCombat.getEnoughDamageToKill(c, d, source, false, noPrevention) <= d) && !ComputerUtil.canRegenerate(ai, c)
                        && !(c.getSVar("SacMe").length() > 0);
            }
        });

        Card targetCard;
        if (pl.isOpponentOf(ai) && (killables.size() > 0)) {
            targetCard = CardFactoryUtil.getBestCreatureAI(killables);

            return targetCard;
        }

        if (!mandatory) {
            return null;
        }

        if (hPlay.size() > 0) {
            if (pl.isOpponentOf(ai)) {
                targetCard = CardFactoryUtil.getBestCreatureAI(hPlay);
            } else {
                targetCard = CardFactoryUtil.getWorstCreatureAI(hPlay);
            }

            return targetCard;
        }

        return null;
    }

    /**
     * <p>
     * damageTargetAI.
     * </p>
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param dmg
     *            a int.
     * @return a boolean.
     */
    private boolean damageTargetAI(final AIPlayer ai, final SpellAbility saMe, final int dmg) {
        final Target tgt = saMe.getTarget();

        if (tgt == null) {
            return this.damageChooseNontargeted(ai, saMe, dmg);
        }

        return this.damageChoosingTargets(ai, saMe, tgt, dmg, false, false);
    }

    /**
     * <p>
     * damageChoosingTargets.
     * </p>
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param dmg
     *            a int.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean damageChoosingTargets(final Player ai, final SpellAbility saMe, final Target tgt, int dmg,
            final boolean isTrigger, final boolean mandatory) {
        final Card source = saMe.getSourceCard();
        final boolean noPrevention = saMe.hasParam("NoPrevention");
        final PhaseHandler phase = Singletons.getModel().getGame().getPhaseHandler();
        final boolean divided = saMe.hasParam("DividedAsYouChoose");

        // target loop
        tgt.resetTargets();
        Player enemy = ai.getOpponent();
        
        if (tgt.getMaxTargets(source, saMe) <= 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, saMe)) {

            if (tgt.canTgtCreatureAndPlayer()) {

                if (this.shouldTgtP(ai, saMe, dmg, noPrevention)) {
                    tgt.addTarget(enemy);
                    if (divided) {
                        tgt.addDividedAllocation(enemy, dmg);
                        break;
                    }
                    continue;
                }

                final Card c = this.dealDamageChooseTgtC(ai, saMe, dmg, noPrevention, enemy, false);
                if (c != null) {
                    tgt.addTarget(c);
                    if (divided) {
                        final int assignedDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                        if (assignedDamage <= dmg) {
                            tgt.addDividedAllocation(c, assignedDamage);
                        }
                        dmg = dmg - assignedDamage;
                        if (dmg <= 0) {
                            break;
                        }
                    }
                    continue;
                }

                // When giving priority to targeting Creatures for mandatory
                // triggers
                // feel free to add the Human after we run out of good targets

                // TODO: add check here if card is about to die from something
                // on the stack
                // or from taking combat damage
                final boolean freePing = isTrigger || saMe.getPayCosts() == null || tgt.getNumTargeted() > 0
                        || (phase.is(PhaseType.END_OF_TURN) && saMe.isAbility() && phase.getNextTurn().equals(ai))
                            || (phase.is(PhaseType.MAIN2) && saMe.getRestrictions().getPlaneswalker());

                if (freePing && saMe.canTarget(enemy)) {
                    tgt.addTarget(enemy);
                    if (divided) {
                        tgt.addDividedAllocation(enemy, dmg);
                        break;
                    }
                }
            } else if (tgt.canTgtCreature()) {
                final Card c = this.dealDamageChooseTgtC(ai, saMe, dmg, noPrevention, enemy, mandatory);
                if (c != null) {
                    tgt.addTarget(c);
                    if (divided) {
                        final int assignedDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                        if (assignedDamage <= dmg) {
                            tgt.addDividedAllocation(c, assignedDamage);
                        }
                        dmg = dmg - assignedDamage;
                        if (dmg <= 0) {
                            break;
                        }
                    }
                    continue;
                }
            }

            // TODO: Improve Damage, we shouldn't just target the player just
            // because we can
            else if (saMe.canTarget(enemy)) {
                if ((phase.is(PhaseType.END_OF_TURN) && phase.getNextTurn().equals(ai))
                        || (SpellAbilityAi.isSorcerySpeed(saMe) && phase.is(PhaseType.MAIN2))
                        || saMe.getPayCosts() == null || isTrigger
                        || this.shouldTgtP(ai, saMe, dmg, noPrevention)) {
                    tgt.addTarget(enemy);
                    if (divided) {
                        tgt.addDividedAllocation(enemy, dmg);
                        break;
                    }
                    continue;
                }
            }
            // fell through all the choices, no targets left?
            if (((tgt.getNumTargeted() < tgt.getMinTargets(source, saMe)) || (tgt.getNumTargeted() == 0))) {
                if (!mandatory) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // If the trigger is mandatory, gotta choose my own stuff now
                    return this.damageChooseRequiredTargets(ai, saMe, tgt, dmg, mandatory);
                }
            } else {
                // TODO is this good enough? for up to amounts?
                break;
            }
        }
        return true;
    }

    /**
     * <p>
     * damageChooseNontargeted.
     * </p>
     * @param ai 
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param dmg
     *            a int.
     * @return a boolean.
     */
    private boolean damageChooseNontargeted(AIPlayer ai, final SpellAbility saMe, final int dmg) {
        // TODO: Improve circumstances where the Defined Damage is unwanted
        final ArrayList<Object> objects = AbilityUtils.getDefinedObjects(saMe.getSourceCard(), saMe.getParam("Defined"), saMe);
        boolean urgent = false; // can it wait?
        boolean positive = false;

        for (final Object o : objects) {
            if (o instanceof Card) {
                Card c = (Card) o;
                final int restDamage = ComputerUtilCombat.predictDamageTo(c, dmg, saMe.getSourceCard(), false);
                if (c.getLethalDamage() <= restDamage) {
                    if (c.getController().equals(ai)) {
                        return false;
                    } else {
                        urgent = true;
                    }
                }
                if (c.getController().isOpponentOf(ai)) {
                    positive = true;
                }
            } else if (o instanceof Player) {
                final Player p = (Player) o;
                final int restDamage = ComputerUtilCombat.predictDamageTo(p, dmg, saMe.getSourceCard(), false);
                if (!p.isOpponentOf(ai) && p.canLoseLife() && restDamage + 3 >= p.getLife() && restDamage > 0) {
                    // from this spell will kill me
                    return false;
                }
                if (p.isOpponentOf(ai) && p.canLoseLife()) {
                    positive = true;
                    if (p.getLife() + 3 <= restDamage) {
                        urgent = true;
                    }
                }
            }
        }
        if (!positive) {
            return false;
        }
        if (!urgent && !SpellAbilityAi.playReusable(ai, saMe)) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * damageChooseRequiredTargets.
     * </p>
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param dmg
     *            a int.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean damageChooseRequiredTargets(final Player ai, final SpellAbility saMe, final Target tgt, final int dmg,
            final boolean mandatory) {
        // this is for Triggered targets that are mandatory
        final boolean noPrevention = saMe.hasParam("NoPrevention");
        final boolean divided = saMe.hasParam("DividedAsYouChoose");

        while (tgt.getNumTargeted() < tgt.getMinTargets(saMe.getSourceCard(), saMe)) {
            // TODO: Consider targeting the planeswalker
            if (tgt.canTgtCreature()) {
                final Card c = this.dealDamageChooseTgtC(ai, saMe, dmg, noPrevention, ai, mandatory);
                if (c != null) {
                    tgt.addTarget(c);
                    if (divided) {
                        tgt.addDividedAllocation(c, dmg);
                        break;
                    }
                    continue;
                }
            }

            if (saMe.canTarget(ai)) {
                if (tgt.addTarget(ai)) {
                    if (divided) {
                        tgt.addDividedAllocation(ai, dmg);
                        break;
                    }
                    continue;
                }
            }

            // if we get here then there isn't enough targets, this is the only
            // time we can return false
            return false;
        }
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {

        final Card source = sa.getSourceCard();
        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getSourceCard(), damage, sa);

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // If it's not mandatory check a few things
            if (!mandatory && !this.damageChooseNontargeted(ai, sa, dmg)) {
                return false;
            }
        } else {
            if (!this.damageChoosingTargets(ai, sa, tgt, dmg, true, mandatory) && !mandatory) {
                return false;
            }

            if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid") && !sa.hasParam("DividedAsYouChoose")) {
                // If I can kill my target by paying less mana, do it
                int actualPay = 0;
                final boolean noPrevention = sa.hasParam("NoPrevention");
                final ArrayList<Card> cards = tgt.getTargetCards();
                //target is a player
                if (cards.isEmpty()) {
                    actualPay = dmg;
                }
                for (final Card c : cards) {
                    final int adjDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                    if (adjDamage > actualPay) {
                        actualPay = adjDamage;
                    }
                }

                source.setSVar("PayX", Integer.toString(actualPay));
            }
        }

        return true;
    }

}
