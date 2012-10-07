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
import java.util.List;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.Command;
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
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactory_Regenerate class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryRegenerate {

    // Ex: A:SP$Regenerate | Cost$W | Tgt$TgtC | SpellDescription$Regenerate
    // target creature.
    // http://www.slightlymagic.net/wiki/Forge_AbilityFactory#Regenerate

    // **************************************************************
    // ********************* Regenerate ****************************
    // **************************************************************

    /**
     * <p>
     * getAbilityRegenerate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getAbilityRegenerate(final AbilityFactory af) {
        class AbilityRegenerate extends AbilityActivated {
            public AbilityRegenerate(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityRegenerate(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -6386981911243700037L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryRegenerate.regenerateCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRegenerate.regenerateResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryRegenerate.regenerateStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryRegenerate.doTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abRegenerate = new AbilityRegenerate(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abRegenerate;
    }

    /**
     * <p>
     * getSpellRegenerate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getSpellRegenerate(final AbilityFactory af) {

        final SpellAbility spRegenerate = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -3899905398102316582L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryRegenerate.regenerateCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRegenerate.regenerateResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryRegenerate.regenerateStackDescription(af, this);
            }

        }; // Spell

        return spRegenerate;
    }

    /**
     * <p>
     * createDrawbackRegenerate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getDrawbackRegenerate(final AbilityFactory af) {
        class DrawbackRegenerate extends AbilitySub {
            public DrawbackRegenerate(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackRegenerate(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -2295483806708528744L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryRegenerate.regenerateStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRegenerate.regenerateResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryRegenerate.doTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbRegen = new DrawbackRegenerate(af.getHostCard(), af.getAbTgt());

        return dbRegen;
    }

    /**
     * <p>
     * regenerateStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String regenerateStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();

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

            sb.append("Regenerate ");
            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                final Card tgtC = it.next();
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }

                if (it.hasNext()) {
                    sb.append(", ");
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

    /**
     * <p>
     * regenerateCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean regenerateCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        boolean chance = false;
        final Cost abCost = af.getAbCost();
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, hostCard, 4, null)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(abCost, hostCard)) {
                return false;
            }

            if (!CostUtil.checkCreatureSacrificeCost(abCost, hostCard)) {
                return false;
            }
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of
            // them
            final ArrayList<Card> list = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);

            if (AllZone.getStack().size() > 0) {
                final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(sa.getActivatingPlayer(),af);

                for (final Card c : list) {
                    if (objects.contains(c)) {
                        chance = true;
                    }
                }
            } else {
                if (Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    boolean flag = false;

                    for (final Card c : list) {
                        if (c.getShield() == 0) {
                            flag |= CombatUtil.combatantWouldBeDestroyed(c);
                        }
                    }

                    chance = flag;
                } else { // if nothing on the stack, and it's not declare
                         // blockers. no need to regen
                    return false;
                }
            }
        } else {
            tgt.resetTargets();
            // filter AIs battlefield by what I can target
            List<Card> targetables = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
            targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), AllZone.getComputerPlayer(), hostCard);

            if (targetables.size() == 0) {
                return false;
            }

            if (AllZone.getStack().size() > 0) {
                // check stack for something on the stack will kill anything i
                // control
                final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(sa.getActivatingPlayer(), af);

                final List<Card> threatenedTargets = new ArrayList<Card>();

                for (final Card c : targetables) {
                    if (objects.contains(c) && (c.getShield() == 0)) {
                        threatenedTargets.add(c);
                    }
                }

                if (!threatenedTargets.isEmpty()) {
                    // Choose "best" of the remaining to regenerate
                    tgt.addTarget(CardFactoryUtil.getBestCreatureAI(threatenedTargets));
                    chance = true;
                }
            } else {
                if (Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    final List<Card> combatants = CardLists.filter(targetables, CardPredicates.Presets.CREATURES);
                    CardLists.sortByEvaluateCreature(combatants);

                    for (final Card c : combatants) {
                        if ((c.getShield() == 0) && CombatUtil.combatantWouldBeDestroyed(c)) {
                            tgt.addTarget(c);
                            chance = true;
                            break;
                        }
                    }
                }
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    } // regenerateCanPlayAI

    /**
     * <p>
     * doTriggerAI.
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
    private static boolean doTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        boolean chance = false;

        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = AbilityFactoryRegenerate.regenMandatoryTarget(af, sa, mandatory);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * regenMandatoryTarget.
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
    private static boolean regenMandatoryTarget(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final Card hostCard = af.getHostCard();
        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        // filter AIs battlefield by what I can target
        List<Card> targetables = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), AllZone.getComputerPlayer(), hostCard);
        final List<Card> compTargetables = CardLists.filterControlledBy(targetables, AllZone.getComputerPlayer());

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
                    if ((c.getShield() == 0) && CombatUtil.combatantWouldBeDestroyed(c)) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something i
            // can target

            // choose my best X without regen
            if (CardLists.getNotType(compTargetables, "Creature").size() == 0) {
                for (final Card c : combatants) {
                    if (c.getShield() == 0) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
                tgt.addTarget(combatants.get(0));
                return true;
            } else {
                CardLists.sortByMostExpensive(compTargetables);
                for (final Card c : compTargetables) {
                    if (c.getShield() == 0) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
                tgt.addTarget(compTargetables.get(0));
                return true;
            }
        }

        tgt.addTarget(CardFactoryUtil.getCheapestPermanentAI(targetables, sa, true));
        return true;
    }

    /**
     * <p>
     * regenerateResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void regenerateResolve(final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
        }

        for (final Card tgtC : tgtCards) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 1922050611313909200L;

                @Override
                public void execute() {
                    tgtC.resetShield();
                }
            };

            if (AllZoneUtil.isCardInPlay(tgtC) && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                tgtC.addShield();
                AllZone.getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateResolve

    // **************************************************************
    // ********************* RegenerateAll *************************
    // **************************************************************

    /**
     * <p>
     * getAbilityRegenerateAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getAbilityRegenerateAll(final AbilityFactory af) {
        class AbilityRegenerateAll extends AbilityActivated {
            public AbilityRegenerateAll(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityRegenerateAll(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -3001272997209059394L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryRegenerate.regenerateAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRegenerate.regenerateAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryRegenerate.regenerateAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryRegenerate.regenerateAllDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abRegenerateAll = new AbilityRegenerateAll(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abRegenerateAll;
    }

    /**
     * <p>
     * getSpellRegenerateAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getSpellRegenerateAll(final AbilityFactory af) {

        final SpellAbility spRegenerateAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4185454527676705881L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryRegenerate.regenerateAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRegenerate.regenerateAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryRegenerate.regenerateAllStackDescription(af, this);
            }

        }; // Spell

        return spRegenerateAll;
    }

    /**
     * <p>
     * createDrawbackRegenerateAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getDrawbackRegenerateAll(final AbilityFactory af) {
        class DrawbackRegenerateAll extends AbilitySub {
            public DrawbackRegenerateAll(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackRegenerateAll(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 4777861790603705572L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryRegenerate.regenerateAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRegenerate.regenerateAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryRegenerate.regenerateAllDoTriggerAI(af, this, mandatory);
            }

        }
        final SpellAbility dbRegenAll = new DrawbackRegenerateAll(af.getHostCard(), af.getAbTgt());

        return dbRegenAll;
    }

    /**
     * <p>
     * regenerateAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String regenerateAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        String desc = "";
        if (params.containsKey("SpellDescription")) {
            desc = params.get("SpellDescription");
        } else {
            desc = "Regenerate all valid cards.";
        }

        sb.append(desc);

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * regenerateAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean regenerateAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        boolean chance = false;
        final Cost abCost = af.getAbCost();
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(abCost, hostCard)) {
                return false;
            }

            if (!CostUtil.checkCreatureSacrificeCost(abCost, hostCard)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(abCost, hostCard, 4, null)) {
                return false;
            }
        }

        // filter AIs battlefield by what I can target
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        List<Card> list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard);
        list = CardLists.filter(list, CardPredicates.isController(AllZone.getComputerPlayer()));

        if (list.size() == 0) {
            return false;
        }

        int numSaved = 0;
        if (AllZone.getStack().size() > 0) {
            final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(sa.getActivatingPlayer(),af);

            for (final Card c : list) {
                if (objects.contains(c) && c.getShield() == 0) {
                    numSaved++;
                }
            }
        } else {
            if (Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                final List<Card> combatants = CardLists.filter(list, CardPredicates.Presets.CREATURES);

                for (final Card c : combatants) {
                    if (c.getShield() == 0 && CombatUtil.combatantWouldBeDestroyed(c)) {
                        numSaved++;
                    }
                }
            }
        }

        if (numSaved > 1) {
            chance = true;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * regenerateAllDoTriggerAI.
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
    private static boolean regenerateAllDoTriggerAI(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        boolean chance = true;

        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * regenerateAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void regenerateAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        List<Card> list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard);

        for (final Card c : list) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 259368227093961103L;

                @Override
                public void execute() {
                    c.resetShield();
                }
            };

            if (AllZoneUtil.isCardInPlay(c)) {
                c.addShield();
                AllZone.getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateAllResolve

} // end class AbilityFactory_Regenerate
