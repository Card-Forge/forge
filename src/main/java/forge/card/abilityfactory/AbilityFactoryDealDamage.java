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
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactoryDealDamage class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryDealDamage {
    private AbilityFactory abilityFactory = null;

    private final String damage;

    /**
     * <p>
     * Constructor for AbilityFactoryDealDamage.
     * </p>
     * 
     * @param newAF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public AbilityFactoryDealDamage(final AbilityFactory newAF) {
        this.abilityFactory = newAF;

        this.damage = this.abilityFactory.getMapParams().get("NumDmg");
    }

    // *************************************************************************
    // ***************************** DealDamage ********************************
    // *************************************************************************

    /**
     * <p>
     * getAbilitDealDamage.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityDealDamage() {
        final SpellAbility abDamage = new AbilityActivated(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -7560349014757367722L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDealDamage.this.dealDamageCanPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.dealDamageStackDescription(
                        AbilityFactoryDealDamage.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.dealDamageResolve(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDealDamage.this.dealDamageDoTriggerAI(
                        AbilityFactoryDealDamage.this.abilityFactory, this, mandatory);
            }
        }; // Ability_Activated

        return abDamage;
    }

    /**
     * <p>
     * getSpellDealDamage.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellDealDamage() {
        final SpellAbility spDealDamage = new Spell(this.abilityFactory.getHostCard(), this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 7239608350643325111L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDealDamage.this.dealDamageCanPlayAI(this);

            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.dealDamageStackDescription(
                        AbilityFactoryDealDamage.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.dealDamageResolve(this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryDealDamage.this.dealDamageDoTriggerAINoCost(
                            AbilityFactoryDealDamage.this.abilityFactory, this, mandatory);
                }
                return AbilityFactoryDealDamage.this.dealDamageDoTriggerAI(
                        AbilityFactoryDealDamage.this.abilityFactory, this, mandatory);
            }
        }; // Spell

        return spDealDamage;
    }

    /**
     * <p>
     * getDrawbackDealDamage.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackDealDamage() {
        final SpellAbility dbDealDamage = new AbilitySub(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 7239608350643325111L;

            @Override
            public boolean chkAIDrawback() {
                // Make sure there is a valid target
                return AbilityFactoryDealDamage.this.damageDrawback(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.dealDamageStackDescription(
                        AbilityFactoryDealDamage.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.dealDamageResolve(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDealDamage.this.dealDamageDoTriggerAI(
                        AbilityFactoryDealDamage.this.abilityFactory, this, mandatory);
            }

        }; // Drawback

        return dbDealDamage;
    }

    /**
     * <p>
     * dealDamageStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String dealDamageStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when damageStackDescription is called, just build exactly what is
        // happening
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final String name = af.getHostCard().toString();
        final int dmg = this.getNumDamage(sa);

        ArrayList<Object> tgts;
        if (sa.getTarget() == null) {
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        } else {
            tgts = sa.getTarget().getTargets();
        }

        if (tgts.size() > 0) {
            if (!(sa instanceof AbilitySub)) {
                sb.append(name).append(" -");
            }
            sb.append(" ");

            if (params.containsKey("StackDescription")) {
                sb.append(params.get("StackDescription"));
            }
            else {
                final String conditionDesc = af.getMapParams().get("ConditionDescription");
                if (conditionDesc != null) {
                    sb.append(conditionDesc).append(" ");
                }

                final ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(sa.getSourceCard(), af.getMapParams()
                        .get("DamageSource"), sa);
                final Card source = definedSources.get(0);

                if (source != sa.getSourceCard()) {
                    sb.append(source.toString()).append(" deals");
                } else {
                    sb.append("Deals");
                }

                sb.append(" ").append(dmg).append(" damage to ");

                for (int i = 0; i < tgts.size(); i++) {
                    if (i != 0) {
                        sb.append(" ");
                    }

                    final Object o = tgts.get(i);
                    if ((o instanceof Card) || (o instanceof Player)) {
                        sb.append(o.toString());
                    }
                }

                if (af.getMapParams().containsKey("Radiance")) {
                    sb.append(" and each other ").append(af.getMapParams().get("ValidTgts"))
                            .append(" that shares a color with ");
                    if (tgts.size() > 1) {
                        sb.append("them");
                    } else {
                        sb.append("it");
                    }
                }

                sb.append(". ");
            }
        }

        if (sa.getSubAbility() != null) {
            sb.append(sa.getSubAbility().getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * getNumDamage.
     * </p>
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    private int getNumDamage(final SpellAbility saMe) {
        return AbilityFactory.calculateAmount(saMe.getSourceCard(), this.damage, saMe);
    }

    /**
     * <p>
     * damageDrawback.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean damageDrawback(final SpellAbility sa) {
        final Card source = sa.getSourceCard();
        int dmg;
        if (this.damage.equals("X") && source.getSVar(this.damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(dmg));
        } else {
            dmg = this.getNumDamage(sa);
        }
        return this.damageTargetAI(sa, dmg);
    }

    /**
     * <p>
     * dealDamageCanPlayAI.
     * </p>
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean dealDamageCanPlayAI(final SpellAbility saMe) {

        final Cost abCost = this.abilityFactory.getAbCost();
        final Card source = saMe.getSourceCard();

        int dmg = 0;
        if (this.damage.equals("X") && source.getSVar(this.damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtil.determineLeftoverMana(saMe);
            source.setSVar("PayX", Integer.toString(dmg));
        } else {
            dmg = this.getNumDamage(saMe);
        }
        boolean rr = this.abilityFactory.isSpell();

        if (dmg <= 0) {
            return false;
        }

        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(abCost, source, 4)) {
            return false;
        }

        if (!CostUtil.checkSacrificeCost(abCost, source)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
            return false;
        }

        if (source.getName().equals("Stuffy Doll")) {
            // Now stuffy sits around for blocking
            // TODO(sol): this should also happen if Stuffy is going to die
            return Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.END_OF_TURN, AllZone.getHumanPlayer());
        }

        if (this.abilityFactory.isAbility()) {
            final Random r = MyRandom.getRandom(); // prevent run-away
                                                   // activations
            if (r.nextFloat() <= Math.pow(.9, saMe.getActivationsThisTurn())) {
                rr = true;
            }
        }

        final boolean bFlag = this.damageTargetAI(saMe, dmg);
        if (!bFlag) {
            return false;
        }

        if (this.damage.equals("X") && source.getSVar(this.damage).equals("Count$xPaid")) {
            // If I can kill my target by paying less mana, do it
            final Target tgt = saMe.getTarget();
            if (tgt != null) {
                int actualPay = 0;
                final boolean noPrevention = this.abilityFactory.getMapParams().containsKey("NoPrevention");
                final ArrayList<Card> cards = tgt.getTargetCards();
                for (final Card c : cards) {
                    final int adjDamage = c.getEnoughDamageToKill(dmg, source, false, noPrevention);
                    if ((adjDamage > actualPay) && (adjDamage <= dmg)) {
                        actualPay = adjDamage;
                    }
                }
                source.setSVar("PayX", Integer.toString(actualPay));
            }
        }

        final AbilitySub subAb = saMe.getSubAbility();
        if (subAb != null) {
            rr &= subAb.chkAIDrawback();
        }
        return rr;
    }

    /**
     * <p>
     * shouldTgtP.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param d
     *            a int.
     * @param noPrevention
     *            a boolean.
     * @return a boolean.
     */
    private boolean shouldTgtP(final SpellAbility sa, final int d, final boolean noPrevention) {
        int restDamage = d;
        final Player human = AllZone.getHumanPlayer();
        final Player comp = AllZone.getComputerPlayer();
        if (!sa.canTarget(human)) {
            return false;
        }

        if (!noPrevention) {
            restDamage = human.predictDamage(restDamage, this.abilityFactory.getHostCard(), false);
        } else {
            restDamage = human.staticReplaceDamage(restDamage, this.abilityFactory.getHostCard(), false);
        }

        if (restDamage == 0) {
            return false;
        }

        if (!human.canLoseLife()) {
            return false;
        }

        final CardList hand = comp.getCardsIn(ZoneType.Hand);

        if (this.abilityFactory.isSpell()) {
            // If this is a spell, cast it instead of discarding
            if ((Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.END_OF_TURN) || Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.MAIN2))
                    && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(comp) && (hand.size() > comp.getMaxHandSize())) {
                return true;
            }
        }

        if ((human.getLife() - restDamage) < 5) {
            // drop the human to less than 5
            // life
            return true;
        }

        return false;
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
    private Card dealDamageChooseTgtC(final SpellAbility saMe, final int d, final boolean noPrevention,
            final Player pl, final boolean mandatory) {
        final Target tgt = saMe.getTarget();
        final Card source = saMe.getSourceCard();
        final HashMap<String, String> params = this.abilityFactory.getMapParams();
        CardList hPlay = pl.getCardsIn(ZoneType.Battlefield);
        hPlay = hPlay.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), source);

        final ArrayList<Object> objects = tgt.getTargets();
        if (params.containsKey("TargetUnique")) {
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
        hPlay = hPlay.getTargetableCards(saMe);

        final CardList killables = hPlay.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return (c.getEnoughDamageToKill(d, source, false, noPrevention) <= d) && !ComputerUtil.canRegenerate(c)
                        && !(c.getSVar("SacMe").length() > 0);
            }
        });

        Card targetCard;
        if (pl.isHuman() && (killables.size() > 0)) {
            targetCard = CardFactoryUtil.getBestCreatureAI(killables);

            return targetCard;
        }

        if (!mandatory) {
            return null;
        }

        if (hPlay.size() > 0) {
            if (pl.isHuman()) {
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
    private boolean damageTargetAI(final SpellAbility saMe, final int dmg) {
        final Target tgt = saMe.getTarget();

        if (tgt == null) {
            return this.damageChooseNontargeted(saMe, dmg);
        }

        return this.damageChoosingTargets(saMe, tgt, dmg, false, false);
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
    private boolean damageChoosingTargets(final SpellAbility saMe, final Target tgt, final int dmg,
            final boolean isTrigger, final boolean mandatory) {
        final boolean noPrevention = this.abilityFactory.getMapParams().containsKey("NoPrevention");

        // target loop
        tgt.resetTargets();

        while (tgt.getNumTargeted() < tgt.getMaxTargets(saMe.getSourceCard(), saMe)) {
            // TODO: Consider targeting the planeswalker
            if (tgt.canTgtCreatureAndPlayer()) {

                if (this.shouldTgtP(saMe, dmg, noPrevention)) {
                    if (tgt.addTarget(AllZone.getHumanPlayer())) {
                        continue;
                    }
                }

                final Card c = this.dealDamageChooseTgtC(saMe, dmg, noPrevention, AllZone.getHumanPlayer(), false);
                if (c != null) {
                    tgt.addTarget(c);
                    continue;
                }

                // When giving priority to targeting Creatures for mandatory
                // triggers
                // feel free to add the Human after we run out of good targets

                // TODO: add check here if card is about to die from something
                // on the stack
                // or from taking combat damage
                final boolean freePing = isTrigger || saMe.getPayCosts() == null || tgt.getNumTargeted() > 0
                        || (Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.END_OF_TURN) && saMe.isAbility()
                                && Singletons.getModel().getGameState().getPhaseHandler().isNextTurn(AllZone.getComputerPlayer()));

                if (freePing && saMe.canTarget(AllZone.getHumanPlayer()) && tgt.addTarget(AllZone.getHumanPlayer())) {
                    continue;
                }
            } else if (tgt.canTgtCreature()) {
                final Card c = this.dealDamageChooseTgtC(saMe, dmg, noPrevention, AllZone.getHumanPlayer(), mandatory);
                if (c != null) {
                    tgt.addTarget(c);
                    continue;
                }
            }

            // TODO: Improve Damage, we shouldn't just target the player just
            // because we can
            else if (saMe.canTarget(AllZone.getHumanPlayer()) && ((Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.END_OF_TURN)
                    && Singletons.getModel().getGameState().getPhaseHandler().isNextTurn(AllZone.getComputerPlayer()))
                    || saMe.getPayCosts() == null || isTrigger)) {
                if (tgt.addTarget(AllZone.getHumanPlayer())) {
                    System.out.println(saMe.getSourceCard() + " - damageChoosingTargets");
                    continue;
                }
            }
            // fell through all the choices, no targets left?
            if (((tgt.getNumTargeted() < tgt.getMinTargets(saMe.getSourceCard(), saMe)) || (tgt.getNumTargeted() == 0))) {
                if (!mandatory) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // If the trigger is mandatory, gotta choose my own stuff
                    // now
                    return this.damageChooseRequiredTargets(saMe, tgt, dmg, mandatory);
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
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param dmg
     *            a int.
     * @return a boolean.
     */
    private boolean damageChooseNontargeted(final SpellAbility saMe, final int dmg) {
        // TODO: Improve circumstances where the Defined Damage is unwanted
        final ArrayList<Object> objects = AbilityFactory.getDefinedObjects(saMe.getSourceCard(), this.abilityFactory
                .getMapParams().get("Defined"), saMe);

        for (final Object o : objects) {
            if (o instanceof Card) {
                // Card c = (Card)o;
            } else if (o instanceof Player) {
                final Player p = (Player) o;
                final int restDamage = p.predictDamage(dmg, this.abilityFactory.getHostCard(), false);
                if (p.isComputer() && p.canLoseLife() && ((restDamage + 3) >= p.getLife()) && (restDamage > 0)) {
                    // from
                    // this
                    // spell
                    // will
                    // kill
                    // me
                    return false;
                }
                if (p.isHuman() && !p.canLoseLife()) {
                    return false;
                }
            }
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
    private boolean damageChooseRequiredTargets(final SpellAbility saMe, final Target tgt, final int dmg,
            final boolean mandatory) {
        // this is for Triggered targets that are mandatory
        final boolean noPrevention = this.abilityFactory.getMapParams().containsKey("NoPrevention");

        while (tgt.getNumTargeted() < tgt.getMinTargets(saMe.getSourceCard(), saMe)) {
            // TODO: Consider targeting the planeswalker
            if (tgt.canTgtCreature()) {
                final Card c = this.dealDamageChooseTgtC(saMe, dmg, noPrevention, AllZone.getComputerPlayer(),
                        mandatory);
                if (c != null) {
                    tgt.addTarget(c);
                    continue;
                }
            }

            if (saMe.canTarget(AllZone.getComputerPlayer())) {
                if (tgt.addTarget(AllZone.getComputerPlayer())) {
                    continue;
                }
            }

            // if we get here then there isn't enough targets, this is the only
            // time we can return false
            return false;
        }
        return true;
    }

    private boolean dealDamageDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }
        return dealDamageDoTriggerAINoCost(af, sa, mandatory);
    }

    /**
     * <p>
     * damageDoTriggerAINoCost.
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
    private boolean dealDamageDoTriggerAINoCost(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {

        final Card source = sa.getSourceCard();
        int dmg;
        if (this.damage.equals("X") && source.getSVar(this.damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(dmg));
        } else {
            dmg = this.getNumDamage(sa);
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // If it's not mandatory check a few things
            if (!mandatory && !this.damageChooseNontargeted(sa, dmg)) {
                return false;
            }
        } else {
            if (!this.damageChoosingTargets(sa, tgt, dmg, true, mandatory) && !mandatory) {
                return false;
            }

            if (this.damage.equals("X") && source.getSVar(this.damage).equals("Count$xPaid")) {
                // If I can kill my target by paying less mana, do it
                int actualPay = 0;
                final boolean noPrevention = this.abilityFactory.getMapParams().containsKey("NoPrevention");
                final ArrayList<Card> cards = tgt.getTargetCards();
                for (final Card c : cards) {
                    final int adjDamage = c.getEnoughDamageToKill(dmg, source, false, noPrevention);
                    if (adjDamage > actualPay) {
                        actualPay = adjDamage;
                    }
                }

                source.setSVar("PayX", Integer.toString(actualPay));
            }
        }

        if (sa.getSubAbility() != null) {
            return sa.getSubAbility().doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * dealDamageResolve.
     * </p>
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void dealDamageResolve(final SpellAbility saMe) {
        final HashMap<String, String> params = this.abilityFactory.getMapParams();

        final int dmg = this.getNumDamage(saMe);

        final boolean noPrevention = params.containsKey("NoPrevention");
        final boolean combatDmg = params.containsKey("CombatDamage");

        ArrayList<Object> tgts;
        if (saMe.getTarget() == null) {
            tgts = AbilityFactory.getDefinedObjects(saMe.getSourceCard(), params.get("Defined"), saMe);
        } else {
            tgts = saMe.getTarget().getTargets();
        }

        final boolean targeted = (saMe.getTarget() != null);

        if (params.containsKey("Radiance") && targeted) {
            Card origin = null;
            for (int i = 0; i < tgts.size(); i++) {
                if (tgts.get(i) instanceof Card) {
                    origin = (Card) tgts.get(i);
                    break;
                }
            }
            // Can't radiate from a player
            if (origin != null) {
                for (final Card c : CardUtil.getRadiance(this.abilityFactory.getHostCard(), origin,
                        params.get("ValidTgts").split(","))) {
                    tgts.add(c);
                }
            }
        }

        final ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(saMe.getSourceCard(),
                params.get("DamageSource"), saMe);
        final Card source = definedSources.get(0);

        for (final Object o : tgts) {
            if (o instanceof Card) {
                final Card c = (Card) o;
                if (AllZoneUtil.isCardInPlay(c) && (!targeted || c.canBeTargetedBy(saMe))) {
                    if (noPrevention) {
                        c.addDamageWithoutPrevention(dmg, source);
                    } else if (combatDmg) {
                        HashMap<Card, Integer> combatmap = new HashMap<Card, Integer>();
                        combatmap.put(source, dmg);
                        c.addCombatDamage(combatmap);
                    } else {
                        c.addDamage(dmg, source);
                    }
                }

            } else if (o instanceof Player) {
                final Player p = (Player) o;
                if (!targeted || p.canBeTargetedBy(saMe)) {
                    if (noPrevention) {
                        p.addDamageWithoutPrevention(dmg, source);
                    } else if (combatDmg) {
                        p.addCombatDamage(dmg, source);
                    } else {
                        p.addDamage(dmg, source);
                    }
                }
            }
        }
    }

    // *************************************************************************
    // ***************************** DamageAll *********************************
    // *************************************************************************
    /**
     * <p>
     * getAbilityDamageAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityDamageAll() {

        final SpellAbility abDamageAll = new AbilityActivated(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -1831356710492849854L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.damageAllStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDealDamage.this.damageAllCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.damageAllResolve(this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDealDamage.this.damageAllDoTriggerAI(AbilityFactoryDealDamage.this.abilityFactory,
                        this, mandatory);
            }

        };
        return abDamageAll;
    }

    /**
     * <p>
     * getSpellDamageAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellDamageAll() {
        final SpellAbility spDamageAll = new Spell(this.abilityFactory.getHostCard(), this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 8004957182752984818L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;
            private final HashMap<String, String> params = this.af.getMapParams();

            @Override
            public String getStackDescription() {
                if (this.params.containsKey("SpellDescription")) {
                    return AbilityFactoryDealDamage.this.abilityFactory.getHostCard().getName() + " - "
                            + this.params.get("SpellDescription");
                } else {
                    return AbilityFactoryDealDamage.this.damageAllStackDescription(this.af, this);
                }
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDealDamage.this.damageAllCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.damageAllResolve(this.af, this);
            }

        };
        return spDamageAll;
    }

    /**
     * <p>
     * getDrawbackDamageAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackDamageAll() {
        final SpellAbility dbDamageAll = new AbilitySub(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -6169562107675964474L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.damageAllStackDescription(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.damageAllResolve(this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                // check AI life before playing this drawback?
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDealDamage.this.damageAllDoTriggerAI(AbilityFactoryDealDamage.this.abilityFactory,
                        this, mandatory);
            }

        };
        return dbDamageAll;
    }

    /**
     * <p>
     * damageAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String damageAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String name = af.getHostCard().getName();
        final HashMap<String, String> params = af.getMapParams();
        String desc = "";
        if (params.containsKey("ValidDescription")) {
            desc = params.get("ValidDescription");
        }
        final int dmg = this.getNumDamage(sa);

        if (!(sa instanceof AbilitySub)) {
            sb.append(name).append(" -");
        }
        sb.append(" ");

        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        } else {
            final ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(sa.getSourceCard(), af.getMapParams()
                    .get("DamageSource"), sa);
            final Card source = definedSources.get(0);

            if (source != sa.getSourceCard()) {
                sb.append(source.toString()).append(" deals");
            } else {
                sb.append("Deals");
            }

            sb.append(" ").append(dmg).append(" damage to ").append(desc);
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * damageAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean damageAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();

        String validP = "";

        int dmg;
        if (this.damage.equals("X") && source.getSVar(this.damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(dmg));
        } else {
            dmg = this.getNumDamage(sa);
        }

        if (params.containsKey("ValidPlayers")) {
            validP = params.get("ValidPlayers");
        }

        final CardList humanList = this.getKillableCreatures(af, sa, AllZone.getHumanPlayer(), dmg);
        CardList computerList = this.getKillableCreatures(af, sa, AllZone.getComputerPlayer(), dmg);

        final Target tgt = sa.getTarget();
        if (tgt != null && sa.canTarget(AllZone.getHumanPlayer())) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getHumanPlayer());
            computerList = new CardList();
        }

        // abCost stuff that should probably be centralized...
        if (abCost != null) {
            // AI currently disabled for some costs
            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }
        }

        // TODO: if damage is dependant on mana paid, maybe have X be human's
        // max life
        // Don't kill yourself
        if (validP.contains("Each")
                && (AllZone.getComputerPlayer().getLife() <= AllZone.getComputerPlayer().predictDamage(dmg, source,
                        false))) {
            return false;
        }

        // if we can kill human, do it
        if ((validP.contains("Each") || validP.contains("EachOpponent"))
                && (AllZone.getHumanPlayer().getLife() <= AllZone.getHumanPlayer().predictDamage(dmg, source, false))) {
            return true;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        int minGain = 200; // The minimum gain in destroyed creatures
        if (sa.getPayCosts().isReusuableResource()) {
            minGain = 100;
        }
        // evaluate both lists and pass only if human creatures are more
        // valuable
        if ((CardFactoryUtil.evaluateCreatureList(computerList) + minGain) >= CardFactoryUtil
                .evaluateCreatureList(humanList)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return ((r.nextFloat() < .6667) && chance);
    }

    /**
     * <p>
     * getKillableCreatures.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param dmg
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    private CardList getKillableCreatures(final AbilityFactory af, final SpellAbility sa, final Player player,
            final int dmg) {
        final HashMap<String, String> params = af.getMapParams();
        final Card source = af.getHostCard();

        String validC = "";
        if (params.containsKey("ValidCards")) {
            validC = params.get("ValidCards");
        }

        // TODO: X may be something different than X paid
        CardList list = player.getCardsIn(ZoneType.Battlefield);
        list = list.getValidCards(validC.split(","), source.getController(), source);

        final CardListFilter filterKillable = new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return (c.predictDamage(dmg, source, false) >= c.getKillDamage());
            }
        };

        list = list.getNotKeyword("Indestructible");
        list = list.filter(filterKillable);

        return list;
    }

    /**
     * <p>
     * damageAllDoTriggerAI.
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
    private boolean damageAllDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        String validP = "";

        int dmg;
        if (this.damage.equals("X") && source.getSVar(this.damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(dmg));
        } else {
            dmg = this.getNumDamage(sa);
        }

        if (params.containsKey("ValidPlayers")) {
            validP = params.get("ValidPlayers");
        }

        final Target tgt = sa.getTarget();
        do { // A little trick to still check the SubAbilities, once we know we
             // want to play it
            if (tgt == null) {
                // If it's not mandatory check a few things
                if (mandatory) {
                    return true;
                } else {
                    // Don't get yourself killed
                    if (validP.contains("Each")
                            && (AllZone.getComputerPlayer().getLife() <= AllZone.getComputerPlayer().predictDamage(dmg,
                                    source, false))) {
                        return false;
                    }

                    // if we can kill human, do it
                    if ((validP.contains("Each") || validP.contains("EachOpponent") || validP.contains("Targeted"))
                            && (AllZone.getHumanPlayer().getLife() <= AllZone.getHumanPlayer().predictDamage(dmg,
                                    source, false))) {
                        break;
                    }

                    // Evaluate creatures getting killed
                    final CardList humanList = this.getKillableCreatures(af, sa, AllZone.getHumanPlayer(), dmg);
                    final CardList computerList = this.getKillableCreatures(af, sa, AllZone.getComputerPlayer(), dmg);
                    if ((CardFactoryUtil.evaluateCreatureList(computerList) + 50) >= CardFactoryUtil
                            .evaluateCreatureList(humanList)) {
                        return false;
                    }
                }
            } else {
                // DamageAll doesn't really target right now
            }
        } while (false);

        if (sa.getSubAbility() != null) {
            return sa.getSubAbility().doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * damageAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void damageAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                params.get("DamageSource"), sa);
        final Card card = definedSources.get(0);
        final Card source = sa.getSourceCard();

        final int dmg = this.getNumDamage(sa);

        final Target tgt = sa.getTarget();
        Player targetPlayer = null;
        if (tgt != null) {
            targetPlayer = tgt.getTargetPlayers().get(0);
        }

        String players = "";
        CardList list = new CardList();

        if (params.containsKey("ValidPlayers")) {
            players = params.get("ValidPlayers");
        }

        if (params.containsKey("ValidCards")) {
            list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        }

        if (targetPlayer != null) {
            list = list.getController(targetPlayer);
        }

        list = AbilityFactory.filterListByType(list, params.get("ValidCards"), sa);

        for (final Card c : list) {
            if (c.addDamage(dmg, card) && params.containsKey("RememberDamaged")) {
                source.addRemembered(c);
            }
        }

        if (!players.equals("")) {
            final ArrayList<Player> playerList = AbilityFactory.getDefinedPlayers(card, players, sa);
            for (final Player p : playerList) {
                if (p.addDamage(dmg, card) && params.containsKey("RememberDamaged")) {
                    source.addRemembered(p);
                }
            }
        }
    }

    // *************************************************************************
    // ***************************** EachDamage ********************************
    // *************************************************************************
    /**
     * <p>
     * getAbilityEachDamage.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityEachDamage() {

        final SpellAbility abEachDamage = new AbilityActivated(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -1831356710492849854L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.eachDamageStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDealDamage.this.eachDamageCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.eachDamageResolve(this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDealDamage.this.eachDamageDoTriggerAI(
                        AbilityFactoryDealDamage.this.abilityFactory, this, mandatory);
            }

        };
        return abEachDamage;
    }

    /**
     * <p>
     * getSpellEachDamage.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellEachDamage() {
        final SpellAbility spEachDamage = new Spell(this.abilityFactory.getHostCard(), this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 8004957182752984818L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;
            private final HashMap<String, String> params = this.af.getMapParams();

            @Override
            public String getStackDescription() {
                if (this.params.containsKey("SpellDescription")) {
                    return AbilityFactoryDealDamage.this.abilityFactory.getHostCard().getName() + " - "
                            + this.params.get("SpellDescription");
                } else {
                    return AbilityFactoryDealDamage.this.eachDamageStackDescription(this.af, this);
                }
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDealDamage.this.eachDamageCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.eachDamageResolve(this.af, this);
            }

        };
        return spEachDamage;
    }

    /**
     * <p>
     * getDrawbackEachDamage.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackEachDamage() {
        final SpellAbility dbEachDamage = new AbilitySub(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -6169562107675964474L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.eachDamageStackDescription(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.eachDamageResolve(this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                // check AI life before playing this drawback?
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDealDamage.this.eachDamageDoTriggerAI(
                        AbilityFactoryDealDamage.this.abilityFactory, this, mandatory);
            }

        };
        return dbEachDamage;
    }

    /**
     * <p>
     * eachDamageStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String eachDamageStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("DefinedPlayers"), sa);
        }

        String desc = params.get("ValidCards");
        if (params.containsKey("ValidDescription")) {
            desc = params.get("ValidDescription");
        }

        String dmg = "";
        if (params.containsKey("DamageDesc")) {
            dmg = params.get("DamageDesc");
        } else {
            dmg += this.getNumDamage(sa) + " damage";
        }

        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        } else {
            sb.append("Each ").append(desc).append(" deals ").append(dmg).append(" to ");
            for (final Player p : tgtPlayers) {
                sb.append(p);
            }
            if (params.containsKey("DefinedCards")) {
                if (params.get("DefinedCards").equals("Self")) {
                    sb.append(" itself");
                }
            }
        }
        sb.append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private boolean eachDamageCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

        final Target tgt = sa.getTarget();

        if (tgt != null && sa.canTarget(AllZone.getHumanPlayer())) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getHumanPlayer());
        }

        return this.shouldTgtP(sa, this.getNumDamage(sa), false);
    }

    private boolean eachDamageDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        if (sa.getSubAbility() != null) {
            return sa.getSubAbility().doTrigger(mandatory);
        }

        return this.eachDamageCanPlayAI(af, sa);
    }

    private void eachDamageResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();

        CardList sources = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        if (params.containsKey("ValidCards")) {
            sources = sources.getValidCards(params.get("ValidCards"), card.getController(), card);
        }

        ArrayList<Object> tgts = new ArrayList<Object>();
        if (sa.getTarget() == null) {
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("DefinedPlayers"), sa);
        } else {
            tgts = sa.getTarget().getTargets();
        }

        final boolean targeted = (sa.getTarget() != null);

        for (final Object o : tgts) {
            for (final Card source : sources) {
                final int dmg = CardFactoryUtil.xCount(source, card.getSVar("X"));
                // System.out.println(source+" deals "+dmg+" damage to "+o.toString());
                if (o instanceof Card) {
                    final Card c = (Card) o;
                    if (AllZoneUtil.isCardInPlay(c) && (!targeted || c.canBeTargetedBy(sa))) {
                        c.addDamage(dmg, source);
                    }

                } else if (o instanceof Player) {
                    final Player p = (Player) o;
                    if (!targeted || p.canBeTargetedBy(sa)) {
                        p.addDamage(dmg, source);
                    }
                }
            }
        }

        if (params.containsKey("DefinedCards")) {
            if (params.get("DefinedCards").equals("Self")) {
                for (final Card source : sources) {
                    final int dmg = CardFactoryUtil.xCount(source, card.getSVar("X"));
                    // System.out.println(source+" deals "+dmg+" damage to "+source);
                    source.addDamage(dmg, source);
                }
            }
            if (params.get("DefinedCards").equals("Remembered")) {
                for (final Card source : sources) {
                    final int dmg = CardFactoryUtil.xCount(source, card.getSVar("X"));
                    Card rememberedcard;
                    for (final Object o : sa.getSourceCard().getRemembered()) {
                        if (o instanceof Card) {
                            rememberedcard = (Card) o;
                            // System.out.println(source + " deals " + dmg + " damage to " + rememberedcard);
                            rememberedcard.addDamage(dmg, source);
                        }
                    }
                }
            }
        }
    }
    // *************************************************************************
    // ******************************* Fight ***********************************
    // *************************************************************************
    /**
     * <p>
     * getAbilityFight.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityFight() {

        final SpellAbility abFight = new AbilityActivated(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -1831356710492849854L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.fightStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDealDamage.this.fightCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.fightResolve(this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDealDamage.this.fightDoTriggerAI(
                        AbilityFactoryDealDamage.this.abilityFactory, this, mandatory);
            }

        };
        return abFight;
    }

    /**
     * <p>
     * getSpellFight.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellFight() {
        final SpellAbility spFight = new Spell(this.abilityFactory.getHostCard(), this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 8004957182752984818L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;
            private final HashMap<String, String> params = this.af.getMapParams();

            @Override
            public String getStackDescription() {
                if (this.params.containsKey("SpellDescription")) {
                    return AbilityFactoryDealDamage.this.abilityFactory.getHostCard().getName() + " - "
                            + this.params.get("SpellDescription");
                } else {
                    return AbilityFactoryDealDamage.this.fightStackDescription(this.af, this);
                }
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDealDamage.this.fightCanPlayAI(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.fightResolve(this.af, this);
            }

        };
        return spFight;
    }

    /**
     * <p>
     * getDrawbackFight.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackFight() {
        final SpellAbility dbFight = new AbilitySub(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -6169562107675964474L;
            private final AbilityFactory af = AbilityFactoryDealDamage.this.abilityFactory;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDealDamage.this.fightStackDescription(this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDealDamage.this.fightResolve(this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                // check AI life before playing this drawback?
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDealDamage.this.fightDoTriggerAI(
                        AbilityFactoryDealDamage.this.abilityFactory, this, mandatory);
            }

        };
        return dbFight;
    }

    /**
     * <p>
     * fightStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String fightStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();
        Card fighter1 = null;
        Card fighter2 = null;
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts = tgt.getTargetCards();
        if (tgts.size() > 0) {
            fighter1 = tgts.get(0);
        }
        if (params.containsKey("Defined")) {
            fighter2 = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa).get(0);
        } else if (tgts.size() > 1) {
            fighter2 = tgts.get(1);
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append(fighter1 + " fights " + fighter2);

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private boolean fightCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

        //final Target tgt = sa.getTarget();

        return false;
    }

    private boolean fightDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        return false;
    }

    private void fightResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        Card fighter1 = null;
        Card fighter2 = null;
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts = tgt.getTargetCards();
        if (tgts.size() > 0) {
            fighter1 = tgts.get(0);
        }
        if (params.containsKey("Defined")) {
            fighter2 = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa).get(0);
        } else if (tgts.size() > 1) {
            fighter2 = tgts.get(1);
        }

        if (fighter1 == null || fighter2 == null) {
            return;
        }

        int dmg2 = fighter2.getNetAttack();
        fighter2.addDamage(fighter1.getNetAttack(), fighter1);
        fighter1.addDamage(dmg2, fighter2);
    }

} // end class AbilityFactoryDealDamage
