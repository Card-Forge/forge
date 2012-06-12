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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.Command;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_Debuff class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryDebuff {

    private AbilityFactoryDebuff() {
        throw new AssertionError();
    }

    // *************************************************************************
    // ***************************** Debuff ************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityDebuff.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDebuff(final AbilityFactory af) {
        class AbilityDebuff extends AbilityActivated {
            public AbilityDebuff(final Card ca,final Cost co,final Target t) {
                super(ca,co,t);
            }
            
            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityDebuff(getSourceCard(),getPayCosts(),getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;                        
            }
            
            private static final long serialVersionUID = 3536198601841771383L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDebuff.debuffStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDebuff.debuffCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDebuff.debuffResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDebuff.debuffTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abDebuff = new AbilityDebuff(af.getHostCard(), af.getAbCost(), af.getAbTgt());
        
        return abDebuff;
    }

    /**
     * <p>
     * createSpellDebuff.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDebuff(final AbilityFactory af) {
        final SpellAbility spDebuff = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -54573740774322697L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDebuff.debuffStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDebuff.debuffCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDebuff.debuffResolve(af, this);
            }

        };
        return spDebuff;
    }

    /**
     * <p>
     * createDrawbackDebuff.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackDebuff(final AbilityFactory af) {
        class DrawbackDebuff extends AbilitySub {
            public DrawbackDebuff(final Card ca,final Target t) {
                super(ca,t);
            }
            
            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackDebuff(getSourceCard(),getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this,res);
                return res;
            }
            
            private static final long serialVersionUID = -4728590185604233229L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDebuff.debuffStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDebuff.debuffResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryDebuff.debuffDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDebuff.debuffTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbDebuff = new DrawbackDebuff(af.getHostCard(), af.getAbTgt());
        
        return dbDebuff;
    }

    /**
     * <p>
     * getKeywords.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private static ArrayList<String> getKeywords(final HashMap<String, String> params) {
        final ArrayList<String> kws = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            kws.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }
        return kws;
    }

    /**
     * <p>
     * debuffStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String debuffStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();
        final ArrayList<String> kws = AbilityFactoryDebuff.getKeywords(params);
        final StringBuilder sb = new StringBuilder();

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtCards.size() > 0) {
            if (sa instanceof AbilitySub) {
                sb.append(" ");
            } else {
                sb.append(host).append(" - ");
            }

            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                final Card tgtC = it.next();
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }

                if (it.hasNext()) {
                    sb.append(" ");
                }
            }
            sb.append(" loses ");
            /*
             * Iterator<String> kwit = kws.iterator(); while(it.hasNext()) {
             * String kw = kwit.next(); sb.append(kw); if(it.hasNext())
             * sb.append(" "); }
             */
            sb.append(kws);
            if (!params.containsKey("Permanent")) {
                sb.append(" until end of turn");
            }
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
     * debuffCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean debuffCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // if there is no target and host card isn't in play, don't activate
        final Card source = sa.getSourceCard();
        if ((sa.getTarget() == null) && !AllZoneUtil.isCardInPlay(source)) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until AI is improved
        if (!CostUtil.checkCreatureSacrificeCost(cost, source)) {
            return false;
        }

        if (!CostUtil.checkLifeCost(cost, source, 40)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, source)) {
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();
        final SpellAbilityRestriction restrict = sa.getRestrictions();

        // Phase Restrictions
        if ((AllZone.getStack().size() == 0) && Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_BEGIN)) {
            // Instant-speed pumps should not be cast outside of combat when the
            // stack is empty
            if (!AbilityFactory.isSorcerySpeed(sa)) {
                return false;
            }
        }

        final int activations = restrict.getNumberTurnActivations();
        final int sacActivations = restrict.getActivationNumberSacrifice();
        // don't risk sacrificing a creature just to pump it
        if ((sacActivations != -1) && (activations >= (sacActivations - 1))) {
            return false;
        }

        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            final ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
            if (cards.size() == 0) {
                return false;
            }
        } else {
            return AbilityFactoryDebuff.debuffTgtAI(af, sa, AbilityFactoryDebuff.getKeywords(params), false);
        }

        return false;
    }

    /**
     * <p>
     * debuffDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean debuffDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            // TODO - copied from AF_Pump.pumpDrawbackAI() - what should be
            // here?
        } else {
            return AbilityFactoryDebuff.debuffTgtAI(af, sa, AbilityFactoryDebuff.getKeywords(params), false);
        }

        return true;
    } // debuffDrawbackAI()

    /**
     * <p>
     * debuffTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param kws
     *            a {@link java.util.ArrayList} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean debuffTgtAI(final AbilityFactory af, final SpellAbility sa, final ArrayList<String> kws,
            final boolean mandatory) {
        // this would be for evasive things like Flying, Unblockable, etc
        if (!mandatory && Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        CardList list = AbilityFactoryDebuff.getCurseCreatures(af, sa, kws);
        list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        // several uses here:
        // 1. make human creatures lose evasion when they are attacking
        // 2. make human creatures lose Flying/Horsemanship/Shadow/etc. when
        // Comp is attacking
        // 3. remove Indestructible keyword so it can be destroyed?
        // 3a. remove Persist?

        if (list.isEmpty()) {
            return mandatory && AbilityFactoryDebuff.debuffMandatoryTarget(af, sa, mandatory);
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (mandatory) {
                        return AbilityFactoryDebuff.debuffMandatoryTarget(af, sa, mandatory);
                    }

                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = CardFactoryUtil.getBestCreatureAI(list);
            tgt.addTarget(t);
            list.remove(t);
        }

        return true;
    } // pumpTgtAI()

    /**
     * <p>
     * getCurseCreatures.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param kws
     *            a {@link java.util.ArrayList} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList getCurseCreatures(final AbilityFactory af, final SpellAbility sa,
            final ArrayList<String> kws) {
        CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
        list = list.getTargetableCards(sa);

        if (!list.isEmpty()) {
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.hasAnyKeyword(kws); // don't add duplicate negative
                                                 // keywords
                }
            });
        }

        return list;
    } // getCurseCreatures()

    /**
     * <p>
     * debuffMandatoryTarget.
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
    private static boolean debuffMandatoryTarget(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        final Target tgt = sa.getTarget();
        list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        final CardList pref = list.getController(AllZone.getHumanPlayer());
        final CardList forced = list.getController(AllZone.getComputerPlayer());
        final Card source = sa.getSourceCard();

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref.isEmpty()) {
                break;
            }

            Card c;
            if (pref.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.getBestCreatureAI(pref);
            } else {
                c = CardFactoryUtil.getMostExpensivePermanentAI(pref, sa, true);
            }

            pref.remove(c);

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            if (forced.isEmpty()) {
                break;
            }

            // TODO - if forced targeting, just pick something without the given
            // keyword
            Card c;
            if (forced.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.getWorstCreatureAI(forced);
            } else {
                c = CardFactoryUtil.getCheapestPermanentAI(forced, sa, true);
            }

            forced.remove(c);

            tgt.addTarget(c);
        }

        if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        return true;
    } // pumpMandatoryTarget()

    /**
     * <p>
     * debuffTriggerAI.
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
    private static boolean debuffTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();

        final ArrayList<String> kws = AbilityFactoryDebuff.getKeywords(params);

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return AbilityFactoryDebuff.debuffTgtAI(af, sa, kws, mandatory);
        }

        return true;
    }

    /**
     * <p>
     * debuffResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void debuffResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final ArrayList<String> kws = AbilityFactoryDebuff.getKeywords(params);

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }

        for (final Card tgtC : tgtCards) {
            final ArrayList<String> hadIntrinsic = new ArrayList<String>();
            if (AllZoneUtil.isCardInPlay(tgtC) && tgtC.canBeTargetedBy(sa)) {
                for (final String kw : kws) {
                    if (tgtC.getIntrinsicKeyword().contains(kw)) {
                        hadIntrinsic.add(kw);
                    }
                    tgtC.removeIntrinsicKeyword(kw);
                    tgtC.removeAllExtrinsicKeyword(kw);
                }
            }
            if (!params.containsKey("Permanent")) {
                AllZone.getEndOfTurn().addUntil(new Command() {
                    private static final long serialVersionUID = 5387486776282932314L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(tgtC)) {
                            for (final String kw : hadIntrinsic) {
                                tgtC.addIntrinsicKeyword(kw);
                            }
                        }
                    }
                });
            }
        }

    } // debuffResolve

    // *************************************************************************
    // ***************************** DebuffAll *********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityDebuffAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilityDebuffAll(final AbilityFactory af) {
        class AbilityDebuffAll extends AbilityActivated {
            public AbilityDebuffAll(final Card ca,final Cost co,final Target t) {
                super(ca,co,t);
            }
            
            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityDebuffAll(getSourceCard(),getPayCosts(),getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }
            
            private static final long serialVersionUID = -1977027530713097149L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDebuff.debuffAllCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryDebuff.debuffAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDebuff.debuffAllResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDebuff.debuffAllTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abDebuffAll = new AbilityDebuffAll(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abDebuffAll;
    }

    /**
     * <p>
     * createSpellDebuffAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellDebuffAll(final AbilityFactory af) {
        final SpellAbility spDebuffAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 399707924254248213L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDebuff.debuffAllCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryDebuff.debuffAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDebuff.debuffAllResolve(af, this);
            }
        }; // SpellAbility

        return spDebuffAll;
    }

    /**
     * <p>
     * createDrawbackDebuffAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackDebuffAll(final AbilityFactory af) {
        class DrawbackDebuffAll extends AbilitySub {
            public DrawbackDebuffAll(final Card ca,final Target t) {
                super(ca,t);
            }
            
            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackDebuffAll(getSourceCard(),getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this,res);
                return res;
            }
            
            private static final long serialVersionUID = 3262199296469706708L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDebuff.debuffAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDebuff.debuffAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryDebuff.debuffAllChkDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDebuff.debuffAllTriggerAI(af, this, mandatory);
            }
        }
        
        final SpellAbility dbDebuffAll = new DrawbackDebuffAll(af.getHostCard(), af.getAbTgt()); // SpellAbility
        
        return dbDebuffAll;
    }

    /**
     * <p>
     * debuffAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean debuffAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        String valid = "";
        final Random r = MyRandom.getRandom();
        // final Card source = sa.getSourceCard();
        final Card hostCard = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();

        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn()); // to
        // prevent
        // runaway
        // activations

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList comp = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
        comp = comp.getValidCards(valid, hostCard.getController(), hostCard);
        CardList human = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
        human = human.getValidCards(valid, hostCard.getController(), hostCard);

        // TODO - add blocking situations here also

        // only count creatures that can attack
        human = human.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return CombatUtil.canAttack(c);
            }
        });

        // don't use DebuffAll after Combat_Begin until AI is improved
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_BEGIN)) {
            return false;
        }

        if (comp.size() > human.size()) {
            return false;
        }

        return (r.nextFloat() < .6667) && chance;
    } // debuffAllCanPlayAI()

    /**
     * <p>
     * debuffAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void debuffAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        final ArrayList<String> kws = AbilityFactoryDebuff.getKeywords(params);
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        list = list.getValidCards(valid.split(","), hostCard.getController(), hostCard);

        for (final Card tgtC : list) {
            final ArrayList<String> hadIntrinsic = new ArrayList<String>();
            if (AllZoneUtil.isCardInPlay(tgtC) && tgtC.canBeTargetedBy(sa)) {
                for (final String kw : kws) {
                    if (tgtC.getIntrinsicKeyword().contains(kw)) {
                        hadIntrinsic.add(kw);
                    }
                    tgtC.removeIntrinsicKeyword(kw);
                    tgtC.removeExtrinsicKeyword(kw);
                }
            }
            if (!params.containsKey("Permanent")) {
                AllZone.getEndOfTurn().addUntil(new Command() {
                    private static final long serialVersionUID = 7486231071095628674L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(tgtC)) {
                            for (final String kw : hadIntrinsic) {
                                tgtC.addIntrinsicKeyword(kw);
                            }
                        }
                    }
                });
            }
        }
    } // debuffAllResolve()

    /**
     * <p>
     * debuffAllTriggerAI.
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
    private static boolean debuffAllTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * debuffAllChkDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean debuffAllChkDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return true;
    }

    /**
     * <p>
     * debuffAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String debuffAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (params.containsKey("SpellDescription")) {
            desc = params.get("SpellDescription");
        } else if (params.containsKey("DebuffAllDescription")) {
            desc = params.get("DebuffAllDescription");
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append(desc);

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // debuffAllStackDescription()

} // end class AbilityFactory_Debuff
