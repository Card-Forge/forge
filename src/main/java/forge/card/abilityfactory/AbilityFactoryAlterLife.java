package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.ComputerUtil;
import forge.Constant;
import forge.Counters;
import forge.MyRandom;
import forge.Player;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

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

        final SpellAbility abGainLife = new AbilityActivated(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
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
                return AbilityFactoryAlterLife.gainLifeCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.gainLifeResolve(this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.gainLifeDoTriggerAI(this.af, this, mandatory);
            }

        };
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
                return AbilityFactoryAlterLife.gainLifeCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.gainLifeResolve(this.af, this);
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
        final SpellAbility dbGainLife = new AbilitySub(abilityFactory.getHostCard(), abilityFactory.getAbTgt()) {
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
                return AbilityFactoryAlterLife.gainLifeCanPlayAI(this.af, this);
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
                return AbilityFactoryAlterLife.gainLifeDoTriggerAI(this.af, this, mandatory);
            }

        };
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
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);

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

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player player : tgtPlayers) {
            sb.append(player).append(" ");
        }

        sb.append("gains ").append(amount).append(" life.");

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
    public static boolean gainLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final HashMap<String, String> params = af.getMapParams();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final int life = AllZone.getComputerPlayer().getLife();
        final int lifeAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);
        final String amountStr = params.get("LifeAmount");

        // don't use it if no life to gain
        if (lifeAmount <= 0) {
            return false;
        }

        if (abCost != null) {
            if (life > 5) {
                if (!CostUtil.checkSacrificeCost(abCost, source)) {
                    return false;
                }

                if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                    return false;
                }

                if (!CostUtil.checkDiscardCost(abCost, source)) {
                    return false;
                }
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        if (!AllZone.getComputerPlayer().canGainLife()) {
            return false;
        }

        // Don't use lifegain before main 2 if possible
        if (AllZone.getPhase().isBefore(Constant.Phase.MAIN2) && !params.containsKey("ActivationPhases")) {
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
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(AllZone.getHumanPlayer());
            } else {
                tgt.addTarget(AllZone.getComputerPlayer());
            }
        }

        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        boolean randomReturn = r.nextFloat() <= .6667;
        if (AbilityFactory.playReusable(sa)) {
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
    public static boolean gainLifeDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            // payment it's usually
            // not mandatory
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();

        // If the Target is gaining life, target self.
        // if the Target is modifying how much life is gained, this needs to be
        // handled better
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(AllZone.getHumanPlayer());
            } else {
                tgt.addTarget(AllZone.getComputerPlayer());
            }
        }

        final Card source = sa.getSourceCard();
        final String amountStr = params.get("LifeAmount");
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
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

        final int lifeAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);
        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
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
        final SpellAbility abLoseLife = new AbilityActivated(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
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
                return AbilityFactoryAlterLife.loseLifeCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.loseLifeResolve(this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.loseLifeDoTriggerAI(this.af, this, mandatory);
            }
        };
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
                return AbilityFactoryAlterLife.loseLifeCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.loseLifeResolve(this.af, this);
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
        final SpellAbility dbLoseLife = new AbilitySub(abilityFactory.getHostCard(), abilityFactory.getAbTgt()) {
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
                return AbilityFactoryAlterLife.loseLifeCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.loseLifeResolve(this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.loseLifeDoTriggerAI(this.af, this, mandatory);
            }
        };
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
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);

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
        final Target tgt = af.getAbTgt();
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
    public static boolean loseLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        boolean priority = false;

        final String amountStr = params.get("LifeAmount");

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, amount)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        if (!AllZone.getHumanPlayer().canLoseLife()) {
            return false;
        }

        if (amount >= AllZone.getHumanPlayer().getLife()) {
            priority = true; // killing the human should be done asap
        }

        // Don't use loselife before main 2 if possible
        if (AllZone.getPhase().isBefore(Constant.Phase.MAIN2) && !params.containsKey("ActivationPhases") && !priority) {
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
            sa.getTarget().addTarget(AllZone.getHumanPlayer());
        }

        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
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
    public static boolean loseLifeDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            // payment it's usually
            // not mandatory
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.addTarget(AllZone.getHumanPlayer());
        }

        final Card source = sa.getSourceCard();
        final String amountStr = params.get("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
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

        if (tgtPlayers.contains(AllZone.getComputerPlayer())) {
            // For cards like Foul Imp, ETB you lose life
            if ((amount + 3) > AllZone.getComputerPlayer().getLife()) {
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

        final int lifeAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.loseLife(lifeAmount, sa.getSourceCard());
            }
        }
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

        final SpellAbility abPoison = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6598936088284756268L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryAlterLife.poisonStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.poisonCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.poisonResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.poisonDoTriggerAI(af, this, mandatory);
            }

        };
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
                return AbilityFactoryAlterLife.poisonCanPlayAI(af, this);
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
        final SpellAbility dbPoison = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
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
                return AbilityFactoryAlterLife.poisonCanPlayAI(af, this);
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
                return AbilityFactoryAlterLife.poisonDoTriggerAI(af, this, mandatory);
            }

        };
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
    private static boolean poisonDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            // payment it's usually
            // not mandatory
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.addTarget(AllZone.getHumanPlayer());
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
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Num"), sa);

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.addPoisonCounters(amount);
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
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Num"), sa);

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

        final Target tgt = af.getAbTgt();
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
    private static boolean poisonCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Cost abCost = sa.getPayCosts();
        final Card source = af.getHostCard();
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
            if (!CostUtil.checkLifeCost(abCost, source, 1)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }
        }

        // Don't use poison before main 2 if possible
        if (AllZone.getPhase().isBefore(Constant.Phase.MAIN2) && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (AbilityFactory.waitForBlocking(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getHumanPlayer());
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
        final SpellAbility abSetLife = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -7375434097541097668L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.setLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.setLifeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.setLifeResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.setLifeDoTriggerAI(af, this, mandatory);
            }

        };
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
                return AbilityFactoryAlterLife.setLifeCanPlayAI(af, this);
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
        final SpellAbility dbSetLife = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
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
                return AbilityFactoryAlterLife.setLifeCanPlayAI(af, this);
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
                return AbilityFactoryAlterLife.setLifeDoTriggerAI(af, this, mandatory);
            }

        };
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
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);

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

        final Target tgt = af.getAbTgt();
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
    private static boolean setLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        // Ability_Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final int life = AllZone.getComputerPlayer().getLife();
        final int hlife = AllZone.getHumanPlayer().getLife();
        final HashMap<String, String> params = af.getMapParams();
        final String amountStr = params.get("LifeAmount");

        if (!AllZone.getComputerPlayer().canGainLife()) {
            return false;
        }

        // Don't use setLife before main 2 if possible
        if (AllZone.getPhase().isBefore(Constant.Phase.MAIN2) && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        int amount;
        // we shouldn't have to worry too much about PayX for SetLife
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);
        }

        // prevent run-away activations - first time will always return true
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(AllZone.getHumanPlayer());
                // if we can only target the human, and the Human's life would
                // go up, don't play it.
                // possibly add a combo here for Magister Sphinx and Higedetsu's
                // (sp?) Second Rite
                if ((amount > hlife) || !AllZone.getHumanPlayer().canLoseLife()) {
                    return false;
                }
            } else {
                if ((amount > life) && (life <= 10)) {
                    tgt.addTarget(AllZone.getComputerPlayer());
                } else if (hlife > amount) {
                    tgt.addTarget(AllZone.getHumanPlayer());
                } else if (amount > life) {
                    tgt.addTarget(AllZone.getComputerPlayer());
                } else {
                    return false;
                }
            }
        } else {
            if (params.containsKey("Each") && params.get("Defined").equals("Each")) {
                if (amount == 0) {
                    return false;
                } else if (life > amount) { // will decrease computer's life
                    if ((life < 5) || ((life - amount) > (hlife - amount))) {
                        return false;
                    }
                }
            }
            if (amount < life) {
                return false;
            }
        }

        // if life is in danger, always activate
        if ((life < 3) && (amount > life)) {
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
    private static boolean setLifeDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final int life = AllZone.getComputerPlayer().getLife();
        final int hlife = AllZone.getHumanPlayer().getLife();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        final String amountStr = params.get("LifeAmount");

        // If there is a cost payment it's usually not mandatory
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        int amount;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);
        }

        if (source.getName().equals("Eternity Vessel")
                && (AllZoneUtil.isCardInPlay("Vampire Hexmage", AllZone.getHumanPlayer()) || (source
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
                tgt.addTarget(AllZone.getHumanPlayer());
            } else {
                if ((amount > life) && (life <= 10)) {
                    tgt.addTarget(AllZone.getComputerPlayer());
                } else if (hlife > amount) {
                    tgt.addTarget(AllZone.getHumanPlayer());
                } else if (amount > life) {
                    tgt.addTarget(AllZone.getComputerPlayer());
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

        final int lifeAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);
        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
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
        final SpellAbility abExLife = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 212548821691286311L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.exchangeLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.exchangeLifeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryAlterLife.exchangeLifeResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryAlterLife.exchangeLifeDoTriggerAI(af, this, mandatory);
            }

        };
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
                return AbilityFactoryAlterLife.exchangeLifeCanPlayAI(af, this);
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
        final SpellAbility dbExLife = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 6951913863491173483L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryAlterLife.exchangeLifeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryAlterLife.exchangeLifeCanPlayAI(af, this);
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

        };
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

        final Target tgt = af.getAbTgt();
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
    private static boolean exchangeLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final int life = AllZone.getComputerPlayer().getLife();
        final int hLife = AllZone.getHumanPlayer().getLife();

        if (!AllZone.getComputerPlayer().canGainLife()) {
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
            if (AllZone.getHumanPlayer().canBeTargetedBy(sa)) {
                // never target self, that would be silly for exchange
                tgt.addTarget(AllZone.getHumanPlayer());
                if (!AllZone.getHumanPlayer().canLoseLife()) {
                    return false;
                }
            }
        }

        // if life is in danger, always activate
        if ((life < 5) && (hLife > life)) {
            return true;
        }

        // cost includes sacrifice probably, so make sure it's worth it
        chance &= (hLife > (life + 8));

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

        final Target tgt = af.getAbTgt();
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
