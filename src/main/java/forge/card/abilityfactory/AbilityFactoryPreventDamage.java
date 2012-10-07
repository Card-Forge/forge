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
import java.util.List;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
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
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactory_PreventDamage class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryPreventDamage {

    // Ex: A:SP$ PreventDamage | Cost$ W | Tgt$ TgtC | Amount$ 3 |
    // SpellDescription$ Prevent the next 3 damage that would be dealt to ...
    // http://www.slightlymagic.net/wiki/Forge_AbilityFactory#PreventDamage

    /**
     * <p>
     * createAbilityPreventDamage.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityPreventDamage(final AbilityFactory af) {
        class AbilityPreventDamage extends AbilityActivated {
            public AbilityPreventDamage(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityPreventDamage(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -6581723619801399347L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPreventDamage.preventDamageCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPreventDamage.preventDamageResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPreventDamage.preventDamageStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPreventDamage.preventDamageDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abPrevent = new AbilityPreventDamage(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abPrevent;
    }

    /**
     * <p>
     * createSpellPreventDamage.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellPreventDamage(final AbilityFactory af) {

        final SpellAbility spPrevent = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -3899905398102316582L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPreventDamage.preventDamageCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPreventDamage.preventDamageResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPreventDamage.preventDamageStackDescription(af, this);
            }

        }; // Spell

        return spPrevent;
    }

    /**
     * <p>
     * createDrawbackPreventDamage.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPreventDamage(final AbilityFactory af) {
        class DrawbackPreventDamage extends AbilitySub {
            public DrawbackPreventDamage(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackPreventDamage(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -2295483806708528744L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPreventDamage.preventDamageStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPreventDamage.preventDamageResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPreventDamage.preventDamageDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbPrevent = new DrawbackPreventDamage(af.getHostCard(), af.getAbTgt());

        return dbPrevent;
    }

    /**
     * <p>
     * preventDamageStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String preventDamageStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();

        ArrayList<Object> tgts;
        if (sa.getTarget() == null) {
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);
        } else {
            tgts = sa.getTarget().getTargets();
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        sb.append("Prevent the next ");
        sb.append(params.get("Amount"));
        sb.append(" damage that would be dealt to ");
        for (int i = 0; i < tgts.size(); i++) {
            if (i != 0) {
                sb.append(" ");
            }

            final Object o = tgts.get(i);
            if (o instanceof Card) {
                final Card tgtC = (Card) o;
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }
            } else {
                sb.append(o.toString());
            }
        }

        if (af.getMapParams().containsKey("Radiance") && (sa.getTarget() != null)) {
            sb.append(" and each other ").append(af.getMapParams().get("ValidTgts"))
                    .append(" that shares a color with ");
            if (tgts.size() > 1) {
                sb.append("them");
            } else {
                sb.append("it");
            }
        }
        sb.append(" this turn.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * preventDamageCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean preventDamageCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        boolean chance = false;

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(cost, hostCard, 4, null)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(ai, cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkSacrificeCost(ai, cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of
            // them
            final ArrayList<Object> objects = AbilityFactory.getDefinedObjects(sa.getSourceCard(),
                    params.get("Defined"), sa);

            // react to threats on the stack
            if (AllZone.getStack().size() > 0) {
                final ArrayList<Object> threatenedObjects = AbilityFactory.predictThreatenedObjects(sa.getActivatingPlayer(), af);
                for (final Object o : objects) {
                    if (threatenedObjects.contains(o)) {
                        chance = true;
                    }
                }
            } else {
                PhaseHandler handler = Singletons.getModel().getGameState().getPhaseHandler();
                if (handler.is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    boolean flag = false;
                    for (final Object o : objects) {
                        if (o instanceof Card) {
                            final Card c = (Card) o;
                            flag |= CombatUtil.combatantWouldBeDestroyed(c);
                        } else if (o instanceof Player) {
                            // Don't need to worry about Combat Damage during AI's turn
                            final Player p = (Player) o;
                            if (!handler.isPlayerTurn(p)) {
                                flag |= (p.isComputer() && ((CombatUtil.wouldLoseLife(ai, AllZone.getCombat()) && sa
                                        .isAbility()) || CombatUtil.lifeInDanger(ai, AllZone.getCombat())));
                            }
                        }
                    }

                    chance = flag;
                } else { // if nothing on the stack, and it's not declare
                         // blockers. no need to prevent
                    return false;
                }
            }
        } // targeted

        // react to threats on the stack
        else if (AllZone.getStack().size() > 0) {
            tgt.resetTargets();
            // check stack for something on the stack will kill anything i
            // control
            final ArrayList<Object> objects = new ArrayList<Object>();
            // AbilityFactory.predictThreatenedObjects(af);

            if (objects.contains(ai)) {
                tgt.addTarget(ai);
            }

            final List<Card> threatenedTargets = new ArrayList<Card>();
            // filter AIs battlefield by what I can target
            List<Card> targetables = ai.getCardsIn(ZoneType.Battlefield);
            targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, hostCard);

            for (final Card c : targetables) {
                if (objects.contains(c)) {
                    threatenedTargets.add(c);
                }
            }

            if (!threatenedTargets.isEmpty()) {
                // Choose "best" of the remaining to save
                tgt.addTarget(CardFactoryUtil.getBestCreatureAI(threatenedTargets));
                chance = true;
            }

        } // Protect combatants
        else if (Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            if (sa.canTarget(ai) && CombatUtil.wouldLoseLife(ai, AllZone.getCombat())
                    && (CombatUtil.lifeInDanger(ai, AllZone.getCombat()) || sa.isAbility())
                    && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(ai.getOpponent())) {
                tgt.addTarget(ai);
                chance = true;
            } else {
                // filter AIs battlefield by what I can target
                List<Card> targetables = ai.getCardsIn(ZoneType.Battlefield);
                targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, hostCard);

                if (targetables.size() == 0) {
                    return false;
                }
                final List<Card> combatants = CardLists.filter(targetables, CardPredicates.Presets.CREATURES);
                CardLists.sortByEvaluateCreature(combatants);

                for (final Card c : combatants) {
                    if (CombatUtil.combatantWouldBeDestroyed(c)) {
                        tgt.addTarget(c);
                        chance = true;
                        break;
                    }
                }
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * preventDamageDoTriggerAI.
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
    private static boolean preventDamageDoTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        boolean chance = false;

        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = AbilityFactoryPreventDamage.preventDamageMandatoryTarget(ai, af, sa, mandatory);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * preventDamageMandatoryTarget.
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
    private static boolean preventDamageMandatoryTarget(final Player ai, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final Card hostCard = af.getHostCard();
        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        // filter AIs battlefield by what I can target
        List<Card> targetables = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, hostCard);
        final List<Card> compTargetables = CardLists.filterControlledBy(targetables, ai);

        if (targetables.size() == 0) {
            return false;
        }

        if (!mandatory && (compTargetables.size() == 0)) {
            return false;
        }

        if (compTargetables.size() > 0) {
            final List<Card> combatants = CardLists.filter(compTargetables, CardPredicates.Presets.CREATURES);
            CardLists.sortByEvaluateCreature(combatants);
            if (Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                for (final Card c : combatants) {
                    if (CombatUtil.combatantWouldBeDestroyed(c)) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something I
            // can target

            tgt.addTarget(combatants.get(0));
            return true;
        }

        tgt.addTarget(CardFactoryUtil.getCheapestPermanentAI(targetables, sa, true));
        return true;
    }

    /**
     * <p>
     * preventDamageResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void preventDamageResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final int numDam = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), sa);

        ArrayList<Object> tgts;
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
        if (sa.getTarget() == null) {
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);
        } else {
            tgts = sa.getTarget().getTargets();
        }

        if (params.containsKey("Radiance") && (sa.getTarget() != null)) {
            Card origin = null;
            for (int i = 0; i < tgts.size(); i++) {
                if (tgts.get(i) instanceof Card) {
                    origin = (Card) tgts.get(i);
                    break;
                }
            }
            if (origin != null) {
                // Can't radiate from a player
                for (final Card c : CardUtil.getRadiance(af.getHostCard(), origin, params.get("ValidTgts").split(","))) {
                    untargetedCards.add(c);
                }
            }
        }

        final boolean targeted = (sa.getTarget() != null);

        for (final Object o : tgts) {
            if (o instanceof Card) {
                final Card c = (Card) o;
                if (AllZoneUtil.isCardInPlay(c) && (!targeted || c.canBeTargetedBy(sa))) {
                    c.addPreventNextDamage(numDam);
                }

            } else if (o instanceof Player) {
                final Player p = (Player) o;
                if (!targeted || p.canBeTargetedBy(sa)) {
                    p.addPreventNextDamage(numDam);
                }
            }
        }

        for (final Card c : untargetedCards) {
            if (AllZoneUtil.isCardInPlay(c)) {
                c.addPreventNextDamage(numDam);
            }
        }
    } // preventDamageResolve

    // *************************************************************************
    // ************************* PreventDamageAll ******************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityPreventDamageAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityPreventDamageAll(final AbilityFactory af) {
        class AbilityPreventDamageAll extends AbilityActivated {
            public AbilityPreventDamageAll(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityPreventDamageAll(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5750726631110311462L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPreventDamage.preventDamageAllCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPreventDamage.preventDamageAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPreventDamage.preventDamageAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPreventDamage.preventDamageAllDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abPreventAll = new AbilityPreventDamageAll(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abPreventAll;
    }

    /**
     * <p>
     * createSpellPreventDamageAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellPreventDamageAll(final AbilityFactory af) {
        final SpellAbility spPreventAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 7232585188922284377L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPreventDamage.preventDamageAllCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPreventDamage.preventDamageAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPreventDamage.preventDamageAllStackDescription(af, this);
            }

        }; // Spell

        return spPreventAll;
    }

    /**
     * <p>
     * createDrawbackPreventDamageAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPreventDamageAll(final AbilityFactory af) {
        class DrawbackPreventDamageAll extends AbilitySub {
            public DrawbackPreventDamageAll(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackPreventDamageAll(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -655573137457133314L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPreventDamage.preventDamageAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPreventDamage.preventDamageAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPreventDamage.preventDamageAllDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbPreventAll = new DrawbackPreventDamageAll(af.getHostCard(), af.getAbTgt());

        return dbPreventAll;
    }

    private static String preventDamageAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        String desc = sa.getDescription();

        if (desc.contains(":")) {
            desc = desc.split(":")[1];
        }

        sb.append(desc);

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static boolean preventDamageAllCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        boolean chance = false;

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(cost, hostCard, 4, null)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(ai, cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkSacrificeCost(ai, cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        if (AllZone.getStack().size() > 0) {
            // TODO check stack for something on the stack will kill anything i
            // control

        } // Protect combatants
        else if (Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            // TODO
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    private static boolean preventDamageAllDoTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        boolean chance = false;

        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }
        chance = true;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    private static void preventDamageAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        final int numDam = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), sa);

        String players = "";
        List<Card> list = new ArrayList<Card>();

        if (params.containsKey("ValidPlayers")) {
            players = params.get("ValidPlayers");
        }

        if (params.containsKey("ValidCards")) {
            list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        }

        list = AbilityFactory.filterListByType(list, params.get("ValidCards"), sa);

        for (final Card c : list) {
            c.addPreventNextDamage(numDam);
        }

        if (!players.equals("")) {
            final ArrayList<Player> playerList = new ArrayList<Player>(AllZone.getPlayersInGame());
            for (final Player p : playerList) {
                if (p.isValid(players, source.getController(), source)) {
                    p.addPreventNextDamage(numDam);
                }
            }
        }
    } // preventDamageAllResolve

} // end class AbilityFactoryPreventDamage
