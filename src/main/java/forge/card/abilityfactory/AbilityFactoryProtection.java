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

import javax.swing.JOptionPane;

import com.google.common.base.Predicate;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
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
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;


/**
 * <p>
 * AbilityFactory_Protection class.
 * </p>
 * 
 * @author dennis.r.friedrichsen (slapshot5 on slightlymagic.net)
 * @version $Id$
 */
public final class AbilityFactoryProtection {

    private AbilityFactoryProtection() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getSpellProtection.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellProtection(final AbilityFactory af) {
        final SpellAbility spProtect = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4678736312735724916L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectResolve(af, this);
            } // resolve
        }; // SpellAbility

        return spProtect;
    }

    /**
     * <p>
     * getAbilityProtection.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityProtection(final AbilityFactory af) {
        class AbilityProtection extends AbilityActivated {
            public AbilityProtection(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityProtection(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -5295298887428747473L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectResolve(af, this);
            } // resolve()

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryProtection.protectTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abProtect = new AbilityProtection(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abProtect;
    }

    /**
     * <p>
     * getDrawbackProtection.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackProtection(final AbilityFactory af) {
        class DrawbackProtection extends AbilitySub {
            public DrawbackProtection(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackProtection(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 8342800124705819366L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectResolve(af, this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryProtection.protectDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryProtection.protectTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbProtect = new DrawbackProtection(af.getHostCard(), af.getAbTgt()); // SpellAbility

        return dbProtect;
    }

    private static boolean hasProtectionFrom(final Card card, final String color) {
        final ArrayList<String> onlyColors = new ArrayList<String>(Arrays.asList(Constant.Color.ONLY_COLORS));

        // make sure we have a valid color
        if (!onlyColors.contains(color)) {
            return false;
        }

        final String protection = "Protection from " + color;

        return card.hasKeyword(protection);
    }

    private static boolean hasProtectionFromAny(final Card card, final ArrayList<String> colors) {
        boolean protect = false;
        for (final String color : colors) {
            protect |= AbilityFactoryProtection.hasProtectionFrom(card, color);
        }
        return protect;
    }

    private static boolean hasProtectionFromAll(final Card card, final ArrayList<String> colors) {
        boolean protect = true;
        if (colors.size() < 1) {
            return false;
        }

        for (final String color : colors) {
            protect &= AbilityFactoryProtection.hasProtectionFrom(card, color);
        }
        return protect;
    }

    /**
     * <p>
     * getProtectCreatures.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList getProtectCreatures(final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        final ArrayList<String> gains = AbilityFactoryProtection.getProtectionList(hostCard, af.getMapParams());

        CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
        list = CardListUtil.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!c.canBeTargetedBy(sa)) {
                    return false;
                }

                // Don't add duplicate protections
                if (AbilityFactoryProtection.hasProtectionFromAll(c, gains)) {
                    return false;
                }

                // will the creature attack (only relevant for sorcery speed)?
                if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(AllZone.getComputerPlayer())
                        && CardFactoryUtil.doesCreatureAttackAI(c)) {
                    return true;
                }

                // is the creature blocking and unable to destroy the attacker
                // or would be destroyed itself?
                if (c.isBlocking()
                        && (CombatUtil.blockerWouldBeDestroyed(c))) {
                    return true;
                }

                // is the creature in blocked and the blocker would survive
                if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && AllZone.getCombat().isAttacking(c) && AllZone.getCombat().isBlocked(c)
                        && CombatUtil.blockerWouldBeDestroyed(AllZone.getCombat().getBlockers(c).get(0))) {
                    return true;
                }

                return false;
            }
        });
        return list;
    } // getProtectCreatures()

    /**
     * <p>
     * protectCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean protectCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        // if there is no target and host card isn't in play, don't activate
        if ((sa.getTarget() == null) && !AllZoneUtil.isCardInPlay(hostCard)) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(cost, hostCard, 4, null)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkCreatureSacrificeCost(cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        // Phase Restrictions
        if ((AllZone.getStack().size() == 0) && Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE)) {
            // Instant-speed protections should not be cast outside of combat
            // when the stack is empty
            if (!AbilityFactory.isSorcerySpeed(sa)) {
                return false;
            }
        } else if (AllZone.getStack().size() > 0) {
            // TODO protection something only if the top thing on the stack will
            // kill it via damage or destroy
            return false;
        }

        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            final ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

            if (cards.size() == 0) {
                return false;
            }

            /*
             * // when this happens we need to expand AI to consider if its ok
             * for everything? for (Card card : cards) { // TODO if AI doesn't
             * control Card and Pump is a Curse, than maybe use?
             * 
             * }
             */
        } else {
            return AbilityFactoryProtection.protectTgtAI(af, sa, false);
        }

        return false;
    } // protectPlayAI()

    /**
     * <p>
     * protectTgtAI.
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
    private static boolean protectTgtAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!mandatory && Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            return false;
        }

        final Card source = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        CardList list = AbilityFactoryProtection.getProtectCreatures(af, sa);

        list = CardListUtil.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        /*
         * TODO - What this should probably do is if it's time for instants and
         * abilities after Human declares attackers, determine desired
         * protection before assigning blockers.
         * 
         * The other time we want protection is if I'm targeted by a damage or
         * destroy spell on the stack
         * 
         * Or, add protection (to make it unblockable) when Compy is attacking.
         */

        if (AllZone.getStack().size() == 0) {
            // If the cost is tapping, don't activate before declare
            // attack/block
            if ((sa.getPayCosts() != null) && sa.getPayCosts().getTap()) {
                if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(AllZone.getComputerPlayer())) {
                    list.remove(sa.getSourceCard());
                }
                if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(AllZone.getHumanPlayer())) {
                    list.remove(sa.getSourceCard());
                }
            }
        }

        if (list.isEmpty()) {
            return mandatory && AbilityFactoryProtection.protectMandatoryTarget(af, sa, mandatory);
        }

        // Don't target cards that will die.
        list = CardListUtil.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                System.out.println("Not Protecting");
                return !c.getSVar("Targeting").equals("Dies");
            }
        });

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) || (tgt.getNumTargeted() == 0)) {
                    if (mandatory) {
                        return AbilityFactoryProtection.protectMandatoryTarget(af, sa, mandatory);
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
    } // protectTgtAI()

    /**
     * <p>
     * protectMandatoryTarget.
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
    private static boolean protectMandatoryTarget(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        final Target tgt = sa.getTarget();
        list = CardListUtil.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        CardList pref = CardListUtil.filterControlledBy(list, AllZone.getComputerPlayer());
        pref = CardListUtil.filter(pref, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !AbilityFactoryProtection.hasProtectionFromAll(c,
                        AbilityFactoryProtection.getProtectionList(host, params));
            }
        });
        final CardList pref2 = CardListUtil.filterControlledBy(list, AllZone.getComputerPlayer());
        pref = CardListUtil.filter(pref, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !AbilityFactoryProtection.hasProtectionFromAny(c,
                        AbilityFactoryProtection.getProtectionList(host, params));
            }
        });
        final CardList forced = CardListUtil.filterControlledBy(list, AllZone.getHumanPlayer());
        final Card source = sa.getSourceCard();

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref.isEmpty()) {
                break;
            }

            Card c;
            if (CardListUtil.getNotType(pref, "Creature").size() == 0) {
                c = CardFactoryUtil.getBestCreatureAI(pref);
            } else {
                c = CardFactoryUtil.getMostExpensivePermanentAI(pref, sa, true);
            }

            pref.remove(c);

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref2.isEmpty()) {
                break;
            }

            Card c;
            if (CardListUtil.getNotType(pref2, "Creature").size() == 0) {
                c = CardFactoryUtil.getBestCreatureAI(pref2);
            } else {
                c = CardFactoryUtil.getMostExpensivePermanentAI(pref2, sa, true);
            }

            pref2.remove(c);

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) {
            if (forced.isEmpty()) {
                break;
            }

            Card c;
            if (CardListUtil.getNotType(forced, "Creature").size() == 0) {
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
    } // protectMandatoryTarget()

    /**
     * <p>
     * protectTriggerAI.
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
    private static boolean protectTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return AbilityFactoryProtection.protectTgtAI(af, sa, mandatory);
        }

        return true;
    } // protectTriggerAI

    /**
     * <p>
     * protectDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean protectDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        final Card host = af.getHostCard();

        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            if (host.isCreature()) {
                // TODO
            }
        } else {
            return AbilityFactoryProtection.protectTgtAI(af, sa, false);
        }

        return true;
    } // protectDrawbackAI()

    /**
     * <p>
     * protectStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String protectStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final ArrayList<String> gains = AbilityFactoryProtection.getProtectionList(host, params);
        final boolean choose = (params.containsKey("Choices")) ? true : false;
        final String joiner = choose ? "or" : "and";

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
                    sb.append(", ");
                }
            }

            if (af.getMapParams().containsKey("Radiance") && (sa.getTarget() != null)) {
                sb.append(" and each other ").append(af.getMapParams().get("ValidTgts"))
                        .append(" that shares a color with ");
                if (tgtCards.size() > 1) {
                    sb.append("them");
                } else {
                    sb.append("it");
                }
            }

            sb.append(" gain");
            if (tgtCards.size() == 1) {
                sb.append("s");
            }
            sb.append(" protection from ");

            if (choose) {
                sb.append("your choice of ");
            }

            for (int i = 0; i < gains.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }

                if (i == (gains.size() - 1)) {
                    sb.append(joiner).append(" ");
                }

                sb.append(gains.get(i));
            }

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
    } // protectStackDescription()

    /**
     * <p>
     * protectResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void protectResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final boolean isChoice = params.get("Gains").contains("Choice");
        final ArrayList<String> choices = AbilityFactoryProtection.getProtectionList(host, params);
        final ArrayList<String> gains = new ArrayList<String>();
        if (isChoice) {
            if (sa.getActivatingPlayer().isHuman()) {
                final String choice = GuiChoose.one("Choose a protection", choices);
                if (null == choice) {
                    return;
                }
                gains.add(choice);
            } else {
                String choice = choices.get(0);
                if (params.containsKey("AILogic")) {
                    final String logic = params.get("AILogic");
                    if (logic.equals("MostProminentHumanCreatures")) {
                        CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                        if (list.isEmpty()) {
                            list = CardListUtil.filterControlledBy(AllZoneUtil.getCardsInGame(), AllZone.getHumanPlayer());
                        }
                        if (!list.isEmpty()) {
                            choice = CardFactoryUtil.getMostProminentColor(list);
                        }
                    }
                }
                gains.add(choice);
                JOptionPane.showMessageDialog(null, "Computer chooses " + gains, "" + host, JOptionPane.PLAIN_MESSAGE);
            }
        } else {
            if (params.get("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColor()) {
                    gains.add(color.toLowerCase());
                }
            } else {
                gains.addAll(choices);
            }
        }

        ArrayList<Card> tgtCards;
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }

        if (params.containsKey("Radiance") && (tgt != null)) {
            for (final Card c : CardUtil.getRadiance(af.getHostCard(), tgtCards.get(0),
                    params.get("ValidTgts").split(","))) {
                untargetedCards.add(c);
            }
        }

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);

            // only pump things in play
            if (!AllZoneUtil.isCardInPlay(tgtC)) {
                continue;
            }

            // if this is a target, make sure we can still target now
            if ((tgt != null) && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            for (final String gain : gains) {
                tgtC.addExtrinsicKeyword("Protection from " + gain);
            }

            if (!params.containsKey("Permanent")) {
                // If not Permanent, remove protection at EOT
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = 7682700789217703789L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(tgtC)) {
                            for (final String gain : gains) {
                                tgtC.removeExtrinsicKeyword("Protection from " + gain);
                            }
                        }
                    }
                };
                if (params.containsKey("UntilEndOfCombat")) {
                    AllZone.getEndOfCombat().addUntil(untilEOT);
                } else {
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }
            }
        }

        for (final Card unTgtC : untargetedCards) {
            // only pump things in play
            if (!AllZoneUtil.isCardInPlay(unTgtC)) {
                continue;
            }

            for (final String gain : gains) {
                unTgtC.addExtrinsicKeyword("Protection from " + gain);
            }

            if (!params.containsKey("Permanent")) {
                // If not Permanent, remove protection at EOT
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = 7682700789217703789L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(unTgtC)) {
                            for (final String gain : gains) {
                                unTgtC.removeExtrinsicKeyword("Protection from " + gain);
                            }
                        }
                    }
                };
                if (params.containsKey("UntilEndOfCombat")) {
                    AllZone.getEndOfCombat().addUntil(untilEOT);
                } else {
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }
            }
        }
    } // protectResolve()

    private static ArrayList<String> getProtectionList(final Card host, final HashMap<String, String> params) {
        final ArrayList<String> gains = new ArrayList<String>();

        final String gainStr = params.get("Gains");
        if (gainStr.equals("Choice")) {
            String choices = params.get("Choices");

            // Replace AnyColor with the 5 colors
            if (choices.contains("AnyColor")) {
                gains.addAll(Arrays.asList(Constant.Color.ONLY_COLORS));
                choices = choices.replaceAll("AnyColor,?", "");
            }
            // Add any remaining choices
            if (choices.length() > 0) {
                gains.addAll(Arrays.asList(choices.split(",")));
            }
        } else {
            gains.addAll(Arrays.asList(gainStr.split(",")));
        }
        return gains;
    }

    // *************************************************************************
    // ************************** ProtectionAll ********************************
    // *************************************************************************
    /**
     * <p>
     * getSpellProtectionAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellProtectionAll(final AbilityFactory af) {
        final SpellAbility spProtectAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 7205636088393235571L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectAllCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectAllResolve(af, this);
            } // resolve
        }; // SpellAbility

        return spProtectAll;
    }

    /**
     * <p>
     * getAbilityProtectionAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityProtectionAll(final AbilityFactory af) {
        class AbilityProtectionAll extends AbilityActivated {
            public AbilityProtectionAll(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityProtectionAll(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -8491026929105907288L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectAllCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectAllResolve(af, this);
            } // resolve()

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryProtection.protectAllTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abProtectAll = new AbilityProtectionAll(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abProtectAll;
    }

    /**
     * <p>
     * getDrawbackProtectionAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackProtectionAll(final AbilityFactory af) {
        class DrawbackProtectionAll extends AbilitySub {
            public DrawbackProtectionAll(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackProtectionAll(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5096939345199247701L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectAllCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectAllResolve(af, this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryProtection.protectAllDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryProtection.protectAllTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbProtectAll = new DrawbackProtectionAll(af.getHostCard(), af.getAbTgt()); // SpellAbility

        return dbProtectAll;
    }

    /**
     * <p>
     * protectAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean protectAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        // if there is no target and host card isn't in play, don't activate
        if ((sa.getTarget() == null) && !AllZoneUtil.isCardInPlay(hostCard)) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(cost, hostCard, 4, null)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkSacrificeCost(cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        return false;
    } // protectAllCanPlayAI()

    /**
     * <p>
     * protectAllTriggerAI.
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
    private static boolean protectAllTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        return true;
    } // protectAllTriggerAI

    /**
     * <p>
     * protectAllDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean protectAllDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryProtection.protectAllTriggerAI(af, sa, false);
    } // protectAllDrawbackAI()

    /**
     * <p>
     * protectAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String protectAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

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

            if (params.containsKey("SpellDescription")) {
                sb.append(params.get("SpellDescription"));
            } else {
                sb.append("Valid card gain protection");
                if (!params.containsKey("Permanent")) {
                    sb.append(" until end of turn");
                }
                sb.append(".");
            }
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // protectStackDescription()

    /**
     * <p>
     * protectAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void protectAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final boolean isChoice = params.get("Gains").contains("Choice");
        final ArrayList<String> choices = AbilityFactoryProtection.getProtectionList(host, params);
        final ArrayList<String> gains = new ArrayList<String>();
        if (isChoice) {
            if (sa.getActivatingPlayer().isHuman()) {
                final String choice = GuiChoose.one("Choose a protection", choices);
                if (null == choice) {
                    return;
                }
                gains.add(choice);
            } else {
                // TODO - needs improvement
                final String choice = choices.get(0);
                gains.add(choice);
                JOptionPane.showMessageDialog(null, "Computer chooses " + gains, "" + host, JOptionPane.PLAIN_MESSAGE);
            }
        } else {
            if (params.get("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColor()) {
                    gains.add(color.toLowerCase());
                }
            } else {
                gains.addAll(choices);
            }
        }

        // Deal with permanents
        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }
        if (!valid.equals("")) {
            CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
            list = CardListUtil.getValidCards(list, valid, sa.getActivatingPlayer(), host);

            for (final Card tgtC : list) {
                if (AllZoneUtil.isCardInPlay(tgtC)) {
                    for (final String gain : gains) {
                        tgtC.addExtrinsicKeyword("Protection from " + gain);
                    }

                    if (!params.containsKey("Permanent")) {
                        // If not Permanent, remove protection at EOT
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -6573962672873853565L;

                            @Override
                            public void execute() {
                                if (AllZoneUtil.isCardInPlay(tgtC)) {
                                    for (final String gain : gains) {
                                        tgtC.removeExtrinsicKeyword("Protection from " + gain);
                                    }
                                }
                            }
                        };
                        if (params.containsKey("UntilEndOfCombat")) {
                            AllZone.getEndOfCombat().addUntil(untilEOT);
                        } else {
                            AllZone.getEndOfTurn().addUntil(untilEOT);
                        }
                    }
                }
            }
        }

        // Deal with Players
        String players = "";
        if (params.containsKey("ValidPlayers")) {
            players = params.get("ValidPlayers");
        }
        if (!players.equals("")) {
            final ArrayList<Player> playerList = AbilityFactory.getDefinedPlayers(host, players, sa);
            for (final Player player : playerList) {
                for (final String gain : gains) {
                    player.addKeyword("Protection from " + gain);
                }

                if (!params.containsKey("Permanent")) {
                    // If not Permanent, remove protection at EOT
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -6573962672873853565L;

                        @Override
                        public void execute() {
                            for (final String gain : gains) {
                                player.removeKeyword("Protection from " + gain);
                            }
                        }
                    };
                    if (params.containsKey("UntilEndOfCombat")) {
                        AllZone.getEndOfCombat().addUntil(untilEOT);
                    } else {
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }
            }
        }

    } // protectAllResolve()

} // end class AbilityFactory_Protection
