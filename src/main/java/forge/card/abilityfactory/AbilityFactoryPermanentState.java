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
import forge.CardList;
import forge.CardListFilter;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_PermanentState class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryPermanentState {

    // ****************************************
    // ************** Untap *******************
    // ****************************************

    /**
     * <p>
     * createAbilityUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityUntap(final AbilityFactory af) {
        final SpellAbility abUntap = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5445572699000471299L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.untapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.untapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.untapResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.untapTrigger(af, this, mandatory);
            }

        };
        return abUntap;
    }

    /**
     * <p>
     * createSpellUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellUntap(final AbilityFactory af) {
        final SpellAbility spUntap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.untapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.untapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.untapResolve(af, this);
            }

        };
        return spUntap;
    }

    /**
     * <p>
     * createDrawbackUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackUntap(final AbilityFactory af) {
        final SpellAbility dbUntap = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.untapStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.untapResolve(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.untapCanPlayAI(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryPermanentState.untapPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.untapTrigger(af, this, mandatory);
            }

        };
        return dbUntap;
    }

    /**
     * <p>
     * untapStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String untapStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = sa.getSourceCard();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append("Untap ");

        if (params.containsKey("UntapUpTo")) {
            sb.append("up to ").append(params.get("Amount")).append(" ");
            sb.append(params.get("UntapType")).append("s");
        } else {
            ArrayList<Card> tgtCards = new ArrayList<Card>();
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtCards = tgt.getTargetCards();
            } else {
                tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
            }

            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        sb.append(".");

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * untapCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean untapCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final Cost cost = sa.getPayCosts();
        final HashMap<String, String> params = af.getMapParams();

        if (!CostUtil.checkAddM1M1CounterCost(cost, source)) {
            return false;
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn() + 1);

        if (tgt == null) {
            final ArrayList<Card> pDefined = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"),
                    sa);
            if ((pDefined != null) && pDefined.get(0).isUntapped() && pDefined.get(0).getController().isComputer()) {
                return false;
            }
        } else {
            if (!AbilityFactoryPermanentState.untapPrefTargeting(tgt, af, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * untapTrigger.
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
    private static boolean untapTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final HashMap<String, String> params = af.getMapParams();
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine, if this is an unfavorable result
            final ArrayList<Card> pDefined = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"),
                    sa);
            if ((pDefined != null) && pDefined.get(0).isUntapped() && pDefined.get(0).getController().isComputer()) {
                return false;
            }

            return true;
        } else {
            if (AbilityFactoryPermanentState.untapPrefTargeting(tgt, af, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return AbilityFactoryPermanentState.untapUnpreferredTargeting(af, sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * untapPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean untapPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Target tgt = sa.getTarget();

        boolean randomReturn = true;

        if (tgt == null) {
            // who cares if its already untapped, it's only a subability?
        } else {
            if (!AbilityFactoryPermanentState.untapPrefTargeting(tgt, af, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * untapPrefTargeting.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean untapPrefTargeting(final Target tgt, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final Card source = sa.getSourceCard();

        Player targetController = AllZone.getComputerPlayer();

        if (af.isCurse()) {
            targetController = AllZone.getHumanPlayer();
        }

        CardList untapList = targetController.getCardsIn(ZoneType.Battlefield);
        untapList = untapList.getTargetableCards(sa);
        untapList = untapList.getValidCards(tgt.getValidTgts(), source.getController(), source);

        untapList = untapList.filter(CardListFilter.TAPPED);
        // filter out enchantments and planeswalkers, their tapped state doesn't
        // matter.
        final String[] tappablePermanents = { "Creature", "Land", "Artifact" };
        untapList = untapList.getValidCards(tappablePermanents, source.getController(), source);

        if (untapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card choice = null;

            if (untapList.size() == 0) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (untapList.getNotType("Creature").size() == 0) {
                choice = CardFactoryUtil.getBestCreatureAI(untapList); // if
                                                                       // only
                                                                       // creatures
                                                                       // take
                                                                       // the
                                                                       // best
            } else {
                choice = CardFactoryUtil.getMostExpensivePermanentAI(untapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            untapList.remove(choice);
            tgt.addTarget(choice);
        }
        return true;
    }

    /**
     * <p>
     * untapUnpreferredTargeting.
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
    private static boolean untapUnpreferredTargeting(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();

        CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);

        list = list.getValidCards(tgt.getValidTgts(), source.getController(), source);
        list = list.getTargetableCards(sa);

        // filter by enchantments and planeswalkers, their tapped state doesn't
        // matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        CardList tapList = list.getValidCards(tappablePermanents, source.getController(), source);

        if (AbilityFactoryPermanentState.untapTargetList(source, tgt, af, sa, mandatory, tapList)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = list.filter(CardListFilter.UNTAPPED);

        if (AbilityFactoryPermanentState.untapTargetList(source, tgt, af, sa, mandatory, tapList)) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        if (AbilityFactoryPermanentState.untapTargetList(source, tgt, af, sa, mandatory, tapList)) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * untapTargetList.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param tapList
     *            a {@link forge.CardList} object.
     * @return a boolean.
     */
    private static boolean untapTargetList(final Card source, final Target tgt, final AbilityFactory af,
            final SpellAbility sa, final boolean mandatory, final CardList tapList) {
        for (final Card c : tgt.getTargetCards()) {
            tapList.remove(c);
        }

        if (tapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (tapList.getNotType("Creature").size() == 0) {
                choice = CardFactoryUtil.getBestCreatureAI(tapList); // if only
                                                                     // creatures
                                                                     // take
                                                                     // the
                                                                     // best
            } else {
                choice = CardFactoryUtil.getMostExpensivePermanentAI(tapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * untapResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void untapResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgtCards = null;

        if (params.containsKey("UntapUpTo")) {
            AbilityFactoryPermanentState.untapChooseUpTo(af, sa, params);
        } else {
            if (tgt != null) {
                tgtCards = tgt.getTargetCards();
            } else {
                tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
            }

            for (final Card tgtC : tgtCards) {
                if (AllZoneUtil.isCardInPlay(tgtC) && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                    tgtC.untap();
                }
            }
        }
    }

    /**
     * <p>
     * untapChooseUpTo.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param params
     *            a {@link java.util.HashMap} object.
     */
    private static void untapChooseUpTo(final AbilityFactory af, final SpellAbility sa,
            final HashMap<String, String> params) {
        final int num = Integer.parseInt(params.get("Amount"));
        final String valid = params.get("UntapType");

        final ArrayList<Player> definedPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                params.get("Defined"), sa);

        for (final Player p : definedPlayers) {
            if (p.isHuman()) {
                AllZone.getInputControl().setInput(CardFactoryUtil.inputUntapUpToNType(num, valid));
            } else {
                CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                list = list.getType(valid);
                list = list.filter(CardListFilter.TAPPED);

                int count = 0;
                while ((list.size() != 0) && (count < num)) {
                    for (int i = 0; (i < list.size()) && (count < num); i++) {

                        final Card c = CardFactoryUtil.getBestLandAI(list);
                        c.untap();
                        list.remove(c);
                        count++;
                    }
                }
            }
        }
    }

    // ****************************************
    // ************** Tap *********************
    // ****************************************

    /**
     * <p>
     * createAbilityTap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityTap(final AbilityFactory af) {
        final SpellAbility abTap = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5445572699000471299L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.tapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.tapTrigger(af, this, mandatory);
            }

        };
        return abTap;
    }

    /**
     * <p>
     * createSpellTap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellTap(final AbilityFactory af) {
        final SpellAbility spTap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.tapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapResolve(af, this);
            }

        };
        return spTap;
    }

    /**
     * <p>
     * createDrawbackTap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackTap(final AbilityFactory af) {
        final SpellAbility dbTap = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapResolve(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.tapCanPlayAI(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryPermanentState.tapPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.tapTrigger(af, this, mandatory);
            }

        };
        return dbTap;
    }

    /**
     * <p>
     * tapStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String tapStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = sa.getSourceCard();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append("Tap ");

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(".");

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * tapCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn

        final HashMap<String, String> params = af.getMapParams();
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final PhaseHandler phase = Singletons.getModel().getGameState().getPhaseHandler();
        final Player turn = phase.getPlayerTurn();

        if (turn.isHuman()) {
            // Tap things down if it's Human's turn
        } else if (phase.inCombat() && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            // TODO Tap creatures down if in combat
        } else {
            // Generally don't want to tap things during AI turn outside of
            // combat
            return false;
        }

        if (tgt == null) {
            final ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);

            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= c.isUntapped();
            }

            if (!bFlag) {
                return false;
            }
        } else {
            tgt.resetTargets();
            if (!AbilityFactoryPermanentState.tapPrefTargeting(source, tgt, af, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * tapTrigger.
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
    private static boolean tapTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine, if this is an unfavorable result

            return true;
        } else {
            if (AbilityFactoryPermanentState.tapPrefTargeting(source, tgt, af, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return AbilityFactoryPermanentState.tapUnpreferredTargeting(af, sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * tapPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        boolean randomReturn = true;

        if (tgt == null) {
            // either self or defined, either way should be fine
        } else {
            // target section, maybe pull this out?
            tgt.resetTargets();
            if (!AbilityFactoryPermanentState.tapPrefTargeting(source, tgt, af, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * tapPrefTargeting.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean tapPrefTargeting(final Card source, final Target tgt, final AbilityFactory af,
            final SpellAbility sa, final boolean mandatory) {
        CardList tapList = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
        tapList = tapList.filter(CardListFilter.UNTAPPED);
        tapList = tapList.getValidCards(tgt.getValidTgts(), source.getController(), source);
        // filter out enchantments and planeswalkers, their tapped state doesn't
        // matter.
        final String[] tappablePermanents = { "Creature", "Land", "Artifact" };
        tapList = tapList.getValidCards(tappablePermanents, source.getController(), source);
        tapList = tapList.getTargetableCards(sa);

        if (tapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (tapList.getNotType("Creature").size() == 0) {
                choice = CardFactoryUtil.getBestCreatureAI(tapList); // if only
                                                                     // creatures
                                                                     // take
                                                                     // the
                                                                     // best
            } else {
                choice = CardFactoryUtil.getMostExpensivePermanentAI(tapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * tapUnpreferredTargeting.
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
    private static boolean tapUnpreferredTargeting(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();

        CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        list = list.getValidCards(tgt.getValidTgts(), source.getController(), source);
        list = list.getTargetableCards(sa);

        // filter by enchantments and planeswalkers, their tapped state doesn't
        // matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        CardList tapList = list.getValidCards(tappablePermanents, source.getController(), source);

        if (AbilityFactoryPermanentState.tapTargetList(af, sa, tapList, mandatory)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = list.filter(CardListFilter.TAPPED);

        if (AbilityFactoryPermanentState.tapTargetList(af, sa, tapList, mandatory)) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        if (AbilityFactoryPermanentState.tapTargetList(af, sa, tapList, mandatory)) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * tapTargetList.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param tapList
     *            a {@link forge.CardList} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean tapTargetList(final AbilityFactory af, final SpellAbility sa, final CardList tapList,
            final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();

        for (final Card c : tgt.getTargetCards()) {
            tapList.remove(c);
        }

        if (tapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (tapList.getNotType("Creature").size() == 0) {
                choice = CardFactoryUtil.getBestCreatureAI(tapList); // if only
                                                                     // creatures
                                                                     // take
                                                                     // the
                                                                     // best
            } else {
                choice = CardFactoryUtil.getMostExpensivePermanentAI(tapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * tapResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void tapResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();
        final boolean remTapped = params.containsKey("RememberTapped");
        if (remTapped) {
            card.clearRemembered();
        }

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (final Card tgtC : tgtCards) {
            if (AllZoneUtil.isCardInPlay(tgtC) && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                if (tgtC.isUntapped() && (remTapped)) {
                    card.addRemembered(tgtC);
                }
                tgtC.tap();
            }
        }
    }

    // ****************************************
    // ************** UntapAll *****************
    // ****************************************
    /**
     * <p>
     * createAbilityUntapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityUntapAll(final AbilityFactory af) {
        final SpellAbility abUntap = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 8914852730903389831L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.untapAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.untapAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.untapAllResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.untapAllTrigger(af, this, mandatory);
            }

        };
        return abUntap;
    }

    /**
     * <p>
     * createSpellUntapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellUntapAll(final AbilityFactory af) {
        final SpellAbility spUntap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5713174052551899363L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.untapAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.untapAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.untapAllResolve(af, this);
            }

        };
        return spUntap;
    }

    /**
     * <p>
     * createDrawbackUntapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackUntapAll(final AbilityFactory af) {
        final SpellAbility dbUntapAll = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -5187900994680626766L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.untapAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.untapAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryPermanentState.untapAllPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.untapAllPlayDrawbackAI(af, this);
            }

        };
        return dbUntapAll;
    }

    /**
     * <p>
     * untapAllPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean untapAllPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return true;
    }

    /**
     * <p>
     * untapAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void untapAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();

        String valid = "";
        CardList list = null;

        ArrayList<Player> tgtPlayers = null;

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            // use it
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        } else {
            list = tgtPlayers.get(0).getCardsIn(ZoneType.Battlefield);
        }
        list = list.getValidCards(valid.split(","), card.getController(), card);

        for (int i = 0; i < list.size(); i++) {
            list.get(i).untap();
        }
    }

    /**
     * <p>
     * untapAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean untapAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null && abSub.chkAIDrawback()) {
            return true;
        }
        return false;
    }

    /**
     * <p>
     * untapAllTrigger.
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
    private static boolean untapAllTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (mandatory) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * untapAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String untapAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
            sb.append("Untap all valid cards.");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
            sb.append(params.get("SpellDescription"));
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    // ****************************************
    // ************** TapAll *****************
    // ****************************************
    /**
     * <p>
     * createAbilityTapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityTapAll(final AbilityFactory af) {
        final SpellAbility abUntap = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -2095140656782946737L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.tapAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapAllResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.tapAllTrigger(af, this, mandatory);
            }

        };
        return abUntap;
    }

    /**
     * <p>
     * createSpellTapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellTapAll(final AbilityFactory af) {
        final SpellAbility spUntap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -62401571838950166L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.tapAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapAllResolve(af, this);
            }

        };
        return spUntap;
    }

    /**
     * <p>
     * createDrawbackTapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackTapAll(final AbilityFactory af) {
        final SpellAbility dbTap = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapAllResolve(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.tapAllCanPlayAI(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryPermanentState.tapAllPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.tapAllPlayDrawbackAI(af, this);
            }

        };
        return dbTap;
    }

    /**
     * <p>
     * tapAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void tapAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();
        final boolean remTapped = params.containsKey("RememberTapped");
        if (remTapped) {
            card.clearRemembered();
        }

        CardList cards = null;

        ArrayList<Player> tgtPlayers = null;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            // use it
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            cards = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        } else {
            cards = tgtPlayers.get(0).getCardsIn(ZoneType.Battlefield);
        }

        cards = AbilityFactory.filterListByType(cards, params.get("ValidCards"), sa);

        for (final Card c : cards) {
            if (remTapped) {
            card.addRemembered(c);
            }
            c.tap();
        }
    }

    /**
     * <p>
     * tapAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // If tapping all creatures do it either during declare attackers of AIs
        // turn
        // or during upkeep/begin combat?

        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();

        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_BEGIN)) {
            return false;
        }

        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList validTappables = AllZoneUtil.getCardsIn(ZoneType.Battlefield);

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            tgt.addTarget(AllZone.getHumanPlayer());
            validTappables = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
        }

        validTappables = validTappables.getValidCards(valid, source.getController(), source);
        validTappables = validTappables.filter(CardListFilter.UNTAPPED);

        final Random r = MyRandom.getRandom();
        boolean rr = false;
        if (r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn())) {
            rr = true;
        }

        if (validTappables.size() > 0) {
            final CardList human = validTappables.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getController().isHuman();
                }
            });
            final CardList compy = validTappables.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getController().isComputer();
                }
            });
            if (human.size() > compy.size()) {
                return rr;
            }
        }
        return false;
    }

    /**
     * <p>
     * getTapAllTargets.
     * </p>
     * 
     * @param valid
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList getTapAllTargets(final String valid, final Card source) {
        CardList tmpList = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        tmpList = tmpList.getValidCards(valid, source.getController(), source);
        tmpList = tmpList.filter(CardListFilter.UNTAPPED);
        return tmpList;
    }

    /**
     * <p>
     * tapAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String tapAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
            sb.append("Tap all valid cards.");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
            sb.append(params.get("SpellDescription"));
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * tapAllTrigger.
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
    private static boolean tapAllTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();

        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList validTappables = AbilityFactoryPermanentState.getTapAllTargets(valid, source);

        final Target tgt = sa.getTarget();

        if (tgt != null) {
            tgt.resetTargets();
            tgt.addTarget(AllZone.getHumanPlayer());
            validTappables = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
        }

        if (mandatory) {
            return true;
        }

        final Random r = MyRandom.getRandom();
        boolean rr = false;
        if (r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn())) {
            rr = true;
        }

        if (validTappables.size() > 0) {
            final CardList human = validTappables.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getController().isHuman();
                }
            });
            final CardList compy = validTappables.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getController().isComputer();
                }
            });
            if (human.size() > compy.size()) {
                return rr;
            }
        }

        return false;
    }

    /**
     * <p>
     * tapAllPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapAllPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return true;
    }

    // ****************************************
    // ************** Tap or Untap ************
    // ****************************************

    /**
     * <p>
     * createAbilityTapOrUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityTapOrUntap(final AbilityFactory af) {
        final SpellAbility abTapOrUntap = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4713183763302932079L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapOrUntapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.tapOrUntapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapOrUntapResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.tapOrUntapTrigger(af, this, mandatory);
            }

        };
        return abTapOrUntap;
    }

    /**
     * <p>
     * createSpellTapOrUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellTapOrUntap(final AbilityFactory af) {
        final SpellAbility spTapOrUntap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -8870476840484788521L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapOrUntapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.tapOrUntapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapOrUntapResolve(af, this);
            }

        };
        return spTapOrUntap;
    }

    /**
     * <p>
     * createDrawbackTapOrUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackTapOrUntap(final AbilityFactory af) {
        final SpellAbility dbTapOrUntap = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -8282868583712773337L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.tapOrUntapStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.tapOrUntapResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryPermanentState.tapOrUntapPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.tapOrUntapTrigger(af, this, mandatory);
            }

        };
        return dbTapOrUntap;
    }

    /**
     * <p>
     * tapOrUntapStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String tapOrUntapStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();

        final HashMap<String, String> params = af.getMapParams();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append("Tap or untap ");

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(".");

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * tapOrUntapCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapOrUntapCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn

        final HashMap<String, String> params = af.getMapParams();
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (tgt == null) {
            // assume we are looking to tap human's stuff
            // TODO - check for things with untap abilities, and don't tap
            // those.
            final ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);

            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= c.isUntapped();
            }

            if (!bFlag) {
                return false;
            }
        } else {
            tgt.resetTargets();
            if (!AbilityFactoryPermanentState.tapPrefTargeting(source, tgt, af, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * tapOrUntapTrigger.
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
    private static boolean tapOrUntapTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine if this is an unfavorable result

            return true;
        } else {
            if (AbilityFactoryPermanentState.tapPrefTargeting(source, tgt, af, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return AbilityFactoryPermanentState.tapUnpreferredTargeting(af, sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * tapOrUntapPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapOrUntapPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        boolean randomReturn = true;

        if (tgt == null) {
            // either self or defined, either way should be fine
        } else {
            // target section, maybe pull this out?
            tgt.resetTargets();
            if (!AbilityFactoryPermanentState.tapPrefTargeting(source, tgt, af, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * tapOrUntapResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void tapOrUntapResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (final Card tgtC : tgtCards) {
            if (AllZoneUtil.isCardInPlay(tgtC) && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                if (sa.getActivatingPlayer().isHuman()) {
                    final String[] tapOrUntap = new String[] { "Tap", "Untap" };
                    final Object z = GuiUtils.chooseOneOrNone("Tap or Untap " + tgtC + "?", tapOrUntap);
                    if (null == z) {
                        continue;
                    }
                    final boolean tap = (z.equals("Tap")) ? true : false;

                    if (tap) {
                        tgtC.tap();
                    } else {
                        tgtC.untap();
                    }
                } else {
                    // computer
                    tgtC.tap();
                }
            }
        }
    }

    // ******************************************
    // ************** Phases ********************
    // ******************************************
    // Phases generally Phase Out. Time and Tide is the only card that can force
    // Phased Out cards in.

    /**
     * <p>
     * createAbilityPhases.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityPhases(final AbilityFactory af) {
        final SpellAbility abPhases = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5445572699000471299L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.phasesStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.phasesCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.phasesResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.phasesTrigger(af, this, mandatory);
            }

        };
        return abPhases;
    }

    /**
     * <p>
     * createSpellPhases.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellPhases(final AbilityFactory af) {
        final SpellAbility spPhases = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.phasesStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPermanentState.phasesCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.phasesResolve(af, this);
            }

        };
        return spPhases;
    }

    /**
     * <p>
     * createDrawbackPhases.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPhases(final AbilityFactory af) {
        final SpellAbility dbPhases = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPermanentState.phasesStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPermanentState.phasesResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryPermanentState.phasesPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPermanentState.phasesTrigger(af, this, mandatory);
            }

        };
        return dbPhases;
    }

    /**
     * <p>
     * phasesStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String phasesStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(" Phases Out.");

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * phasesCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean phasesCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // This still needs to be fleshed out
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn() + 1);

        ArrayList<Card> tgtCards;
        if (tgt == null) {
            tgtCards = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
            if (tgtCards.contains(source)) {
                // Protect it from something
            } else {
                // Card def = tgtCards.get(0);
                // Phase this out if it might attack me, or before it can be
                // declared as a blocker
            }

            return false;
        } else {
            if (!AbilityFactoryPermanentState.phasesPrefTargeting(tgt, af, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * phasesTrigger.
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
    private static boolean phasesTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            return false;
        } else {
            if (AbilityFactoryPermanentState.phasesPrefTargeting(tgt, af, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return AbilityFactoryPermanentState.phasesUnpreferredTargeting(af, sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * phasesPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean phasesPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Target tgt = sa.getTarget();

        boolean randomReturn = true;

        if (tgt == null) {

        } else {
            if (!AbilityFactoryPermanentState.phasesPrefTargeting(tgt, af, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * phasesPrefTargeting.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean phasesPrefTargeting(final Target tgt, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        // Card source = sa.getSourceCard();

        // CardList phaseList =
        // AllZoneUtil.getCardsIn(Zone.Battlefield).getTargetableCards(source)
        // .getValidCards(tgt.getValidTgts(), source.getController(), source);

        // CardList aiPhaseList =
        // phaseList.getController(AllZone.getComputerPlayer());

        // If Something in the Phase List might die from a bad combat, or a
        // spell on the stack save it

        // CardList humanPhaseList =
        // phaseList.getController(AllZone.getHumanPlayer());

        // If something in the Human List is causing issues, phase it out

        return false;
    }

    /**
     * <p>
     * phasesUnpreferredTargeting.
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
    private static boolean phasesUnpreferredTargeting(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();

        CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        list = list.getValidCards(tgt.getValidTgts(), source.getController(), source).getTargetableCards(sa);

        return false;
    }

    /**
     * <p>
     * phasesResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void phasesResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgtCards = null;

        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (final Card tgtC : tgtCards) {
            if (!tgtC.isPhasedOut()) {
                tgtC.phase();
            }
        }
    }
} // end of AbilityFactory_PermanentState class
