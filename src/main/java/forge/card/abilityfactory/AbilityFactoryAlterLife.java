/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.Counters;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_AlterLife class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryAlterLife {
    // An AbilityFactory subclass for Gaining, Losing, or Setting Life totals.

    // *************************************************************************
    // ************************* GAIN LIFE *************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityGainLife.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityGainLife(final AbilityFactory abilityFactory) {
        class AbilityGainLife extends AbilityActivated {
            public AbilityGainLife(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityGainLife(getSourceCard(), getPayCosts(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 8869422603616247307L;

            private final AbilityFactory af = abilityFactory;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryAlterLife.gainLifeStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.gainLifeCanPlayAI(getActivatingPlayer(), this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.gainLifeResolve(this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.gainLifeDoTriggerAI(getActivatingPlayer(), this.af, this, mandatory);
            }

        }
        final SpellAbility abGainLife = new AbilityGainLife(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt());

        return abGainLife;
    }

    /**
     * <p>
     * createSpellGainLife.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellGainLife(final AbilityFactory abilityFactory) {
        final SpellAbility spGainLife = new Spell(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 6631124959690157874L;

            private final AbilityFactory af = abilityFactory;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryAlterLife.gainLifeStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.gainLifeCanPlayAI(getActivatingPlayer(), this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.gainLifeResolve(this.af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryAlterLife.gainLifeDoTriggerAINoCost(this.getActivatingPlayer(), af, this, mandatory);
                }
                return AbilityFactoryAlterLife.gainLifeDoTriggerAI(this.getActivatingPlayer(), af, this, mandatory);
            }

        };

        return spGainLife;
    }

    /**
     * <p>
     * createDrawbackGainLife.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackGainLife(final AbilityFactory abilityFactory) {
        class DrawbackGainLife extends AbilitySub {
            public DrawbackGainLife(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackGainLife(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 6631124959690157874L;

            private final AbilityFactory af = abilityFactory;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryAlterLife.gainLifeStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.gainLifeCanPlayAI(getActivatingPlayer(), this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.gainLifeResolve(this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.gainLifeDoTriggerAI(getActivatingPlayer(), this.af, this, mandatory);
            }
        }
        final SpellAbility dbGainLife = new DrawbackGainLife(abilityFactory.getHostCard(), abilityFactory.getAbTgt());

        return dbGainLife;
    }

    /**
     * <p>
     * gainLifeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String gainLifeStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        }
        else {
            final String conditionDesc = params.get("ConditionDescription");
            if (conditionDesc != null) {
                sb.append(conditionDesc).append(" ");
            }

            ArrayList<Player> tgtPlayers;

            final Target tgt = sa.getTarget();
            if (tgt != null && !params.containsKey("Defined")) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }

            for (final Player player : tgtPlayers) {
                sb.append(player).append(" ");
            }

            sb.append("gains ").append(amount).append(" life.");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * gainLifeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean gainLifeCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final HashMap<String, String> params = af.getMapParams();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final int life = ai.getLife();
        final String amountStr = params.get("LifeAmount");
        int lifeAmount = 0;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            lifeAmount = xPay;
        } else {
            lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);
        }

        // don't use it if no life to gain
        if (lifeAmount <= 0) {
            return false;
        }
        // don't play if the conditions aren't met, unless it would trigger a beneficial sub-condition
        if (!AbilityFactory.checkConditional(sa)) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null && !sa.isWrapper() && "True".equals(source.getSVar("AIPlayForSub"))) {
                if (!AbilityFactory.checkConditional(abSub)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                && !params.containsKey("ActivationPhases")) {
            return false;
        }
        boolean lifeCritical = life <= 5;
        lifeCritical |= (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DAMAGE)
                && CombatUtil.lifeInDanger(AllZone.getCombat()));

        if (abCost != null && !lifeCritical) {
            if (!CostUtil.checkSacrificeCost(ai, abCost, source, false)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        if (!ai.canGainLife()) {
            return false;
        }

        // Don't use lifegain before main 2 if possible
        if (!lifeCritical
                && Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) 
                && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (AbilityFactory.waitForBlocking(sa)) {
            return false;
        }

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        // final int amount = calculateAmount(af.getHostCard(), amountStr, sa);

        // prevent run-away activations - first time will always return true
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai)) {
                tgt.addTarget(ai);
            } else {
                return false;
            }
        }

        boolean randomReturn = r.nextFloat() <= .6667;
        if (lifeCritical || AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

        return (randomReturn && chance);
    }

    /**
     * <p>
     * gainLifeDoTriggerAI.
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
    public static boolean gainLifeDoTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
            // payment it's usually
            // not mandatory
            return false;
        }
        return gainLifeDoTriggerAINoCost(ai, af, sa, mandatory);
    }

    /**
     * <p>
     * gainLifeDoTriggerAINoCost.
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
    public static boolean gainLifeDoTriggerAINoCost(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {

        final HashMap<String, String> params = af.getMapParams();

        // If the Target is gaining life, target self.
        // if the Target is modifying how much life is gained, this needs to be
        // handled better
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai)) {
                tgt.addTarget(ai);
            } else if (mandatory && sa.canTarget(ai.getOpponent())) {
                tgt.addTarget(ai.getOpponent());
            } else {
                return false;
            }
        }

        final Card source = sa.getSourceCard();
        final String amountStr = params.get("LifeAmount");
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * gainLifeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void gainLifeResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final int lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);
        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.gainLife(lifeAmount, sa.getSourceCard());
            }
        }
    }

    // *************************************************************************
    // ************************* LOSE LIFE *************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityLoseLife.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityLoseLife(final AbilityFactory abilityFactory) {
        class AbilityLoseLife extends AbilityActivated {
            public AbilityLoseLife(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityLoseLife(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 1129762905315395160L;

            private final AbilityFactory af = abilityFactory;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryAlterLife.loseLifeStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.loseLifeCanPlayAI(getActivatingPlayer(), this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.loseLifeResolve(this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.loseLifeDoTriggerAI(getActivatingPlayer(), this.af, this, mandatory);
            }
        }
        final SpellAbility abLoseLife = new AbilityLoseLife(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt());

        return abLoseLife;
    }

    /**
     * <p>
     * createSpellLoseLife.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellLoseLife(final AbilityFactory abilityFactory) {
        final SpellAbility spLoseLife = new Spell(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -2966932725306192437L;

            private final AbilityFactory af = abilityFactory;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryAlterLife.loseLifeStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.loseLifeCanPlayAI(getActivatingPlayer(), this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.loseLifeResolve(this.af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryAlterLife.loseLifeDoTriggerAINoCost(getActivatingPlayer(), af, this, mandatory);
                }
                return AbilityFactoryAlterLife.loseLifeDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        };

        return spLoseLife;
    }

    /**
     * <p>
     * createDrawbackLoseLife.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackLoseLife(final AbilityFactory abilityFactory) {
        class DrawbackLoseLife extends AbilitySub {
            public DrawbackLoseLife(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackLoseLife(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -2966932725306192437L;

            private final AbilityFactory af = abilityFactory;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryAlterLife.loseLifeStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.loseLifeCanPlayAI(getActivatingPlayer(), this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.loseLifeResolve(this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return loseLifeDoTriggerAINoCost(getActivatingPlayer(), this.af, this, false);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.loseLifeDoTriggerAI(getActivatingPlayer(), this.af, this, mandatory);
            }
        }
        final SpellAbility dbLoseLife = new DrawbackLoseLife(abilityFactory.getHostCard(), abilityFactory.getAbTgt());

        return dbLoseLife;
    }

    /**
     * <p>
     * loseLifeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    static String loseLifeStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        ArrayList<Player> tgtPlayers;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player player : tgtPlayers) {
            sb.append(player).append(" ");
        }

        sb.append("loses ").append(amount).append(" life.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * loseLifeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean loseLifeCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        boolean priority = false;

        final String amountStr = params.get("LifeAmount");

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);

        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(amount));
        }

        if (amount <= 0) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, amount, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }
        
        Player opp = ai.getOpponent(); 

        if (!opp.canLoseLife()) {
            return false;
        }

        if (amount >= opp.getLife()) {
            priority = true; // killing the human should be done asap
        }

        // Don't use loselife before main 2 if possible
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !params.containsKey("ActivationPhases") && !priority) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (AbilityFactory.waitForBlocking(sa) && !priority) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (sa.canTarget(opp)) {
                sa.getTarget().addTarget(opp);
            } else {
                return false;
            }
        }

        boolean randomReturn = r.nextFloat() <= .6667;
        if (AbilityFactory.playReusable(sa) || priority) {
            randomReturn = true;
        }

        return (randomReturn && chance);
    }

    /**
     * <p>
     * loseLifeDoTriggerAI.
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
    public static boolean loseLifeDoTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
            return false;
        }
        return loseLifeDoTriggerAINoCost(ai, af, sa, mandatory);
    }

    /**
     * <p>
     * loseLifeDoTriggerAINoCost.
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
    public static boolean loseLifeDoTriggerAINoCost(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {

        final HashMap<String, String> params = af.getMapParams();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            if (sa.canTarget(ai.getOpponent())) {
                tgt.addTarget(ai.getOpponent());
            } else if (mandatory && sa.canTarget(ai)) {
                tgt.addTarget(ai);
            } else {
                return false;
            }
        }

        final Card source = sa.getSourceCard();
        final String amountStr = params.get("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityFactory.calculateAmount(source, amountStr, sa);
        }

        ArrayList<Player> tgtPlayers;
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (!mandatory && tgtPlayers.contains(ai)) {
            // For cards like Foul Imp, ETB you lose life
            if ((amount + 3) > ai.getLife()) {
                return false;
            }
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * loseLifeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void loseLifeResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        int lifeLost = 0;

        final int lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                lifeLost += p.loseLife(lifeAmount, sa.getSourceCard());
            }
        }
        sa.getSourceCard().setSVar("AFLifeLost", "Number$" + Integer.toString(lifeLost));
    }

    // *************************************************************************
    // ************************** Poison Counters ******************************
    // *************************************************************************
    //
    // Made more sense here than in AF_Counters since it affects players and
    // their health

    /**
     * <p>
     * createAbilityPoison.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityPoison(final AbilityFactory af) {
        class AbilityPoison extends AbilityActivated {
            public AbilityPoison(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityPoison(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 6598936088284756268L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryAlterLife.poisonStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.poisonCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.poisonResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.poisonDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abPoison = new AbilityPoison(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abPoison;
    }

    /**
     * <p>
     * createSpellPoison.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellPoison(final AbilityFactory af) {
        final SpellAbility spPoison = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -1495708415138457833L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.poisonStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.poisonCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.poisonResolve(af, this);
            }

        };

        return spPoison;
    }

    /**
     * <p>
     * createDrawbackPoison.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPoison(final AbilityFactory af) {
        class DrawbackPoison extends AbilitySub {
            public DrawbackPoison(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackPoison(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -1173479041548558016L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.poisonStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.poisonCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.poisonResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.poisonDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbPoison = new DrawbackPoison(af.getHostCard(), af.getAbTgt());

        return dbPoison;
    }

    /**
     * <p>
     * poisonDoTriggerAI.
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
    private static boolean poisonDoTriggerAI(final Player ai,final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
            // payment it's usually
            // not mandatory
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.addTarget(ai.getOpponent());
        } else {
            final ArrayList<Player> players = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                    params.get("Defined"), sa);
            for (final Player p : players) {
                if (!mandatory && p.isComputer() && (p.getPoisonCounters() > p.getOpponent().getPoisonCounters())) {
                    return false;
                }
            }
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * poisonResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void poisonResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("Num"), sa);

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.addPoisonCounters(amount, sa.getSourceCard());
            }
        }
    }

    /**
     * <p>
     * poisonStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String poisonStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("Num"), sa);

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtPlayers.size() > 0) {
            final Iterator<Player> it = tgtPlayers.iterator();
            while (it.hasNext()) {
                final Player p = it.next();
                sb.append(p);
                if (it.hasNext()) {
                    sb.append(", ");
                } else {
                    sb.append(" ");
                }
            }
        }

        sb.append("get");
        if (tgtPlayers.size() < 2) {
            sb.append("s");
        }
        sb.append(" ").append(amount).append(" poison counter");
        if (amount != 1) {
            sb.append("s.");
        } else {
            sb.append(".");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * poisonCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean poisonCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        // int humanPoison = AllZone.getHumanPlayer().getPoisonCounters();
        // int humanLife = AllZone.getHumanPlayer().getLife();
        // int aiPoison = AllZone.getComputerPlayer().getPoisonCounters();

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        // final int amount = AbilityFactory.calculateAmount(af.getHostCard(),
        // amountStr, sa);

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 1, null)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }
        }

        // Don't use poison before main 2 if possible
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (AbilityFactory.waitForBlocking(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai.getOpponent());
        }

        return true;
    }

    // *************************************************************************
    // ************************** SET LIFE *************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilitySetLife.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilitySetLife(final AbilityFactory af) {
        class AbilitySetLife extends AbilityActivated {
            public AbilitySetLife(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilitySetLife(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -7375434097541097668L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.setLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.setLifeCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.setLifeResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.setLifeDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abSetLife = new AbilitySetLife(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abSetLife;
    }

    /**
     * <p>
     * createSpellSetLife.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellSetLife(final AbilityFactory af) {
        final SpellAbility spSetLife = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -94657822256270222L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.setLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.setLifeCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.setLifeResolve(af, this);
            }

        };

        return spSetLife;
    }

    /**
     * <p>
     * createDrawbackSetLife.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackSetLife(final AbilityFactory af) {
        class DrawbackSetLife extends AbilitySub {
            public DrawbackSetLife(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackSetLife(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -7634729949893534023L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.setLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryAlterLife.setLifeCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.setLifeResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.setLifeDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbSetLife = new DrawbackSetLife(af.getHostCard(), af.getAbTgt());

        return dbSetLife;
    }

    /**
     * <p>
     * setLifeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String setLifeStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" -");
        } else {
            sb.append(" ");
        }

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player player : tgtPlayers) {
            sb.append(player).append(" ");
        }

        sb.append("life total becomes ").append(amount).append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * setLifeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean setLifeCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        // Ability_Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final int myLife = ai.getLife();
        final Player opponent = ai.getOpponent();
        final int hlife = opponent.getLife();
        final HashMap<String, String> params = af.getMapParams();
        final String amountStr = params.get("LifeAmount");

        if (!ai.canGainLife()) {
            return false;
        }

        // Don't use setLife before main 2 if possible
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        int amount;
        // we shouldn't have to worry too much about PayX for SetLife
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);
        }

        // prevent run-away activations - first time will always return true
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(opponent);
                // if we can only target the human, and the Human's life would
                // go up, don't play it.
                // possibly add a combo here for Magister Sphinx and Higedetsu's
                // (sp?) Second Rite
                if ((amount > hlife) || !opponent.canLoseLife()) {
                    return false;
                }
            } else {
                if ((amount > myLife) && (myLife <= 10)) {
                    tgt.addTarget(ai);
                } else if (hlife > amount) {
                    tgt.addTarget(opponent);
                } else if (amount > myLife) {
                    tgt.addTarget(ai);
                } else {
                    return false;
                }
            }
        } else {
            if (params.containsKey("Each") && params.get("Defined").equals("Each")) {
                if (amount == 0) {
                    return false;
                } else if (myLife > amount) { // will decrease computer's life
                    if ((myLife < 5) || ((myLife - amount) > (hlife - amount))) {
                        return false;
                    }
                }
            }
            if (amount < myLife) {
                return false;
            }
        }

        // if life is in danger, always activate
        if ((myLife < 3) && (amount > myLife)) {
            return true;
        }

        return ((r.nextFloat() < .6667) && chance);
    }

    /**
     * <p>
     * setLifeDoTriggerAI.
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
    private static boolean setLifeDoTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final int myLife = ai.getLife();
        final Player opponent = ai.getOpponent();
        final int hlife = opponent.getLife();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        final String amountStr = params.get("LifeAmount");

        // If there is a cost payment it's usually not mandatory
        if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
            return false;
        }

        int amount;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);
        }

        if (source.getName().equals("Eternity Vessel")
                && (AllZoneUtil.isCardInPlay("Vampire Hexmage", opponent) || (source
                        .getCounters(Counters.CHARGE) == 0))) {
            return false;
        }

        // If the Target is gaining life, target self.
        // if the Target is modifying how much life is gained, this needs to be
        // handled better
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(opponent);
            } else {
                if ((amount > myLife) && (myLife <= 10)) {
                    tgt.addTarget(ai);
                } else if (hlife > amount) {
                    tgt.addTarget(opponent);
                } else if (amount > myLife) {
                    tgt.addTarget(ai);
                } else {
                    return false;
                }
            }
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * setLifeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void setLifeResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final int lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);
        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.setLife(lifeAmount, sa.getSourceCard());
            }
        }
    }

    // *************************************************************************
    // ************************ EXCHANGE LIFE **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityExchangeLife.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityExchangeLife(final AbilityFactory af) {
        class AbilityExchangeLife extends AbilityActivated {
            public AbilityExchangeLife(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityExchangeLife(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 212548821691286311L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.exchangeLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.exchangeLifeCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.exchangeLifeResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.exchangeLifeDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abExLife = new AbilityExchangeLife(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abExLife;
    }

    /**
     * <p>
     * createSpellExchangeLife.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellExchangeLife(final AbilityFactory af) {
        final SpellAbility spExLife = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3512136004868367924L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.exchangeLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.exchangeLifeCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.exchangeLifeResolve(af, this);
            }

        };
        return spExLife;
    }

    /**
     * <p>
     * createDrawbackExchangeLife.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackExchangeLife(final AbilityFactory af) {
        class DrawbackExchangeLife extends AbilitySub {
            public DrawbackExchangeLife(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackExchangeLife(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 6951913863491173483L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.exchangeLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.exchangeLifeCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.exchangeLifeResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.exchangeLifeDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbExLife = new DrawbackExchangeLife(af.getHostCard(), af.getAbTgt());

        return dbExLife;
    }

    /**
     * <p>
     * exchangeLifeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String exchangeLifeStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final Player activatingPlayer = sa.getActivatingPlayer();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" -");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtPlayers.size() == 1) {
            sb.append(activatingPlayer).append(" exchanges life totals with ");
            sb.append(tgtPlayers.get(0));
        } else if (tgtPlayers.size() > 1) {
            sb.append(tgtPlayers.get(0)).append(" exchanges life totals with ");
            sb.append(tgtPlayers.get(1));
        }
        sb.append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * exchangeLifeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean exchangeLifeCanPlayAI(final Player aiPlayer, final AbilityFactory af, final SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final int myLife = aiPlayer.getLife();
        Player opponent = aiPlayer.getOpponent();
        final int hLife = opponent.getLife();

        if (!aiPlayer.canGainLife()) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        /*
         * TODO - There is one card that takes two targets (Soul Conduit) and
         * one card that has a conditional (Psychic Transfer) that are not
         * currently handled
         */
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (opponent.canBeTargetedBy(sa)) {
                // never target self, that would be silly for exchange
                tgt.addTarget(opponent);
                if (!opponent.canLoseLife()) {
                    return false;
                }
            }
        }

        // if life is in danger, always activate
        if ((myLife < 5) && (hLife > myLife)) {
            return true;
        }

        // cost includes sacrifice probably, so make sure it's worth it
        chance &= (hLife > (myLife + 8));

        return ((r.nextFloat() < .6667) && chance);
    }

    /**
     * <p>
     * exchangeLifeDoTriggerAI.
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
    private static boolean exchangeLifeDoTriggerAI(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        // this can pretty much return false for now since nothing of this type
        // triggers
        return false;
    }

    /**
     * <p>
     * exchangeLifeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void exchangeLifeResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        Player p1;
        Player p2;

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtPlayers.size() == 1) {
            p1 = sa.getActivatingPlayer();
            p2 = tgtPlayers.get(0);
        } else {
            p1 = tgtPlayers.get(0);
            p2 = tgtPlayers.get(1);
        }

        final int life1 = p1.getLife();
        final int life2 = p2.getLife();

        if ((life1 > life2) && p1.canLoseLife()) {
            final int diff = life1 - life2;
            p1.loseLife(diff, source);
            p2.gainLife(diff, source);
        } else if ((life2 > life1) && p2.canLoseLife()) {
            final int diff = life2 - life1;
            p2.loseLife(diff, source);
            p1.gainLife(diff, source);
        } else {
            // they are equal, so nothing to do
        }

    }

} // end class AbilityFactory_AlterLife
