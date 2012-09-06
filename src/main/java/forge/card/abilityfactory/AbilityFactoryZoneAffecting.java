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
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CardUtil;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostDiscard;
import forge.card.cost.CostPart;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_ZoneAffecting class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryZoneAffecting {

    // **********************************************************************
    // ******************************* DRAW *********************************
    // **********************************************************************
    /**
     * <p>
     * createAbilityDraw.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDraw(final AbilityFactory af) {
        class AbilityDraw extends AbilityActivated {
            public AbilityDraw(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityDraw(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5445572699000471299L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.drawStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.drawCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.drawResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryZoneAffecting.drawTrigger(af, this, mandatory);
            }
        }
        final SpellAbility abDraw = new AbilityDraw(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abDraw;
    }

    /**
     * <p>
     * createSpellDraw.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDraw(final AbilityFactory af) {
        final SpellAbility spDraw = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.drawStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.drawCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.drawResolve(af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryZoneAffecting.drawTriggerNoCost(af, this, mandatory);
                }
                return AbilityFactoryZoneAffecting.drawTrigger(af, this, mandatory);
            }

        };
        return spDraw;
    }

    /**
     * <p>
     * createDrawbackDraw.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackDraw(final AbilityFactory af) {
        class DrawbackDraw extends AbilitySub {
            public DrawbackDraw(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackDraw(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.drawStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.drawCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.drawResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryZoneAffecting.drawTargetAI(af, this, false, false);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryZoneAffecting.drawTrigger(af, this, mandatory);
            }
        }
        final SpellAbility dbDraw = new DrawbackDraw(af.getHostCard(), af.getAbTgt());

        return dbDraw;
    }

    /**
     * <p>
     * drawStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String drawStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

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
        if (!params.containsKey("Defined") && tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtPlayers.size() > 0) {
            final Iterator<Player> it = tgtPlayers.iterator();
            while (it.hasNext()) {
                sb.append(it.next().toString());
                if (it.hasNext()) {
                    sb.append(" and ");
                }
            }

            int numCards = 1;
            if (params.containsKey("NumCards")) {
                numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
            }

            if (tgtPlayers.size() > 1) {
                sb.append(" each");
            }
            sb.append(" draw");
            if (tgtPlayers.size() == 1) {
                sb.append("s");
            }
            sb.append(" (").append(numCards).append(")");

            if (params.containsKey("NextUpkeep")) {
                sb.append(" at the beginning of the next upkeep");
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
     * drawCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean drawCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final Cost abCost = sa.getPayCosts();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkCreatureSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                for (final CostPart part : abCost.getCostParts()) {
                    if (part instanceof CostDiscard) {
                        CostDiscard cd = (CostDiscard) part;
                        cd.decideAIPayment(sa, sa.getSourceCard(), null);
                        CardList discards = cd.getList();
                        for (Card discard : discards) {
                            if (!ComputerUtil.isWorseThanDraw(discard)) {
                                return false;
                            }
                        }
                    }
                }
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        final boolean bFlag = AbilityFactoryZoneAffecting.drawTargetAI(af, sa, true, false);

        if (!bFlag) {
            return false;
        }

        if (tgt != null) {
            final ArrayList<Player> players = tgt.getTargetPlayers();
            if ((players.size() > 0) && players.get(0).isHuman()) {
                return true;
            }
        }

        // Don't use draw abilities before main 2 if possible
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (AbilityFactory.waitForBlocking(sa)) {
            return false;
        }

        double chance = .4; // 40 percent chance of drawing with instant speed
                            // stuff
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);
        if (AbilityFactory.isSorcerySpeed(sa)) {
            randomReturn = true;
        }
        if ((Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.END_OF_TURN)
                && Singletons.getModel().getGameState().getPhaseHandler().isNextTurn(AllZone.getComputerPlayer()))) {
            randomReturn = true;
        }

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }
        return randomReturn;
    }

    /**
     * <p>
     * drawTargetAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param primarySA
     *            a boolean.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean drawTargetAI(final AbilityFactory af, final SpellAbility sa, final boolean primarySA,
            final boolean mandatory) {
        final Target tgt = sa.getTarget();
        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();

        int computerHandSize = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand).size();
        final int humanLibrarySize = AllZone.getHumanPlayer().getCardsIn(ZoneType.Library).size();
        final int computerLibrarySize = AllZone.getComputerPlayer().getCardsIn(ZoneType.Library).size();
        final int computerMaxHandSize = AllZone.getComputerPlayer().getMaxHandSize();

        //if a spell is used don't count the card
        if (sa.isSpell() && source.isInZone(ZoneType.Hand)) {
            computerHandSize -= 1;
        }

        int numCards = 1;
        if (params.containsKey("NumCards")) {
            numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
        }

        boolean xPaid = false;
        final String num = params.get("NumCards");
        if ((num != null) && num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            if (sa instanceof AbilitySub) {
                numCards = Integer.parseInt(source.getSVar("PayX"));
            } else {
                numCards = ComputerUtil.determineLeftoverMana(sa);
                source.setSVar("PayX", Integer.toString(numCards));
            }
            xPaid = true;
        }
        //if (n)

        // TODO: if xPaid and one of the below reasons would fail, instead of
        // bailing
        // reduce toPay amount to acceptable level

        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            final boolean canTgtHuman = sa.canTarget(AllZone.getHumanPlayer());
            final boolean canTgtComp = sa.canTarget(AllZone.getComputerPlayer());
            boolean tgtHuman = false;

            if (!canTgtHuman && !canTgtComp) {
                return false;
            }

            if (canTgtHuman && !AllZone.getHumanPlayer().cantLose() && (numCards >= humanLibrarySize)) {
                // Deck the Human? DO IT!
                tgt.addTarget(AllZone.getHumanPlayer());
                return true;
            }

            if (numCards >= computerLibrarySize) {
                if (xPaid) {
                    numCards = computerLibrarySize - 1;
                    source.setSVar("PayX", Integer.toString(numCards));
                } else {
                    // Don't deck your self
                    if (!mandatory) {
                        return false;
                    }
                    tgtHuman = true;
                }
            }

            if (((computerHandSize + numCards) > computerMaxHandSize)
                    && Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().isComputer()) {
                if (xPaid) {
                    numCards = computerMaxHandSize - computerHandSize;
                    source.setSVar("PayX", Integer.toString(numCards));
                } else {
                    // Don't draw too many cards and then risk discarding cards
                    // at EOT
                    if (!(params.containsKey("NextUpkeep") || (sa instanceof AbilitySub)) && !mandatory) {
                        return false;
                    }
                }
            }

            if (numCards == 0 && !mandatory) {
                return false;
            }

            if ((!tgtHuman || !canTgtHuman) && canTgtComp) {
                tgt.addTarget(AllZone.getComputerPlayer());
            } else if (mandatory) {
                tgt.addTarget(AllZone.getHumanPlayer());
            } else {
                return false;
            }
        } else {
            // TODO: consider if human is the defined player

            // ability is not targeted
            if (numCards >= computerLibrarySize) {
                // Don't deck yourself
                if (!mandatory) {
                    return false;
                }
            }

            if (numCards == 0 && !mandatory) {
                return false;
            }

            if (((computerHandSize + numCards) > computerMaxHandSize)
                    && Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().isComputer()
                    && !sa.isTrigger()) {
                // Don't draw too many cards and then risk discarding cards at
                // EOT
                if (!(params.containsKey("NextUpkeep") || (sa instanceof AbilitySub)) && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    } // drawTargetAI()

    /**
     * <p>
     * drawTrigger.
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
    private static boolean drawTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }
        return drawTriggerNoCost(af, sa, mandatory);
    }

    /**
     * <p>
     * drawTriggerNoCost.
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
    private static boolean drawTriggerNoCost(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {

        if (!AbilityFactoryZoneAffecting.drawTargetAI(af, sa, false, mandatory)) {
            return false;
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
     * drawResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void drawResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final Card source = sa.getSourceCard();
        int numCards = 1;
        if (params.containsKey("NumCards")) {
            numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (!params.containsKey("Defined") && tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(source, params.get("Defined"), sa);
        }

        final boolean optional = params.containsKey("OptionalDecider");
        final boolean slowDraw = params.containsKey("NextUpkeep");

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (optional) {
                    if (p.isComputer()) {
                        if (numCards >= p.getCardsIn(ZoneType.Library).size()) {
                            // AI shouldn't itself
                            continue;
                        }
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Do you want to draw ").append(numCards).append(" cards(s)");

                        if (slowDraw) {
                            sb.append(" next upkeep");
                        }

                        sb.append("?");

                        if (!GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString())) {
                            continue;
                        }
                    }
                }

                if (slowDraw) {
                    for (int i = 0; i < numCards; i++) {
                        p.addSlowtripList(source);
                    }
                } else {
                    final CardList drawn = p.drawCards(numCards);
                    if (params.containsKey("Reveal")) {
                        GuiUtils.chooseOne("Revealing drawn cards", drawn.toArray());
                    }
                    if (params.containsKey("RememberDrawn")) {
                        for (final Card c : drawn) {
                            source.addRemembered(c);
                        }
                    }

                }

            }
        }
    } // drawResolve()

    // **********************************************************************
    // ******************************* MILL *********************************
    // **********************************************************************

    /**
     * <p>
     * createAbilityMill.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityMill(final AbilityFactory af) {
        class AbilityMill extends AbilityActivated {
            public AbilityMill(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityMill(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5445572699000471299L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.millStackDescription(this, af);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.millCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.millResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryZoneAffecting.millTrigger(af, this, mandatory);
            }
        }
        final SpellAbility abMill = new AbilityMill(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abMill;
    }

    /**
     * <p>
     * createSpellMill.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellMill(final AbilityFactory af) {
        final SpellAbility spMill = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.millStackDescription(this, af);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.millCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.millResolve(af, this);
            }

        };
        return spMill;
    }

    /**
     * <p>
     * createDrawbackMill.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackMill(final AbilityFactory af) {
        class DrawbackMill extends AbilitySub {
            public DrawbackMill(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackMill(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.millStackDescription(this, af);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.millResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryZoneAffecting.millDrawback(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryZoneAffecting.millTrigger(af, this, mandatory);
            }
        }
        final SpellAbility dbMill = new DrawbackMill(af.getHostCard(), af.getAbTgt());

        return dbMill;
    }

    /**
     * <p>
     * millStackDescription.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link java.lang.String} object.
     */
    private static String millStackDescription(final SpellAbility sa, final AbilityFactory af) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        if (params.containsKey("StackDescription")) {
            if (params.get("StackDescription").equals("None")) {
                sb.append("");
            } else {
            sb.append(params.get("StackDescription"));
            }
        } else {
            for (final Player p : tgtPlayers) {
                sb.append(p.toString()).append(" ");
            }

            final ZoneType dest = ZoneType.smartValueOf(params.get("Destination"));
            if ((dest == null) || dest.equals(ZoneType.Graveyard)) {
                sb.append("mills ");
            } else if (dest.equals(ZoneType.Exile)) {
                sb.append("exiles ");
            } else if (dest.equals(ZoneType.Ante)) {
                sb.append("antes ");
            }
            sb.append(numCards);
            sb.append(" card");
            if (numCards != 1) {
                sb.append("s");
            }
            sb.append(" from the top of his or her library.");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * millCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean millCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final Card source = sa.getSourceCard();
        final Cost abCost = af.getAbCost();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
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

        if (!AbilityFactoryZoneAffecting.millTargetAI(af, sa, false)) {
            return false;
        }

        final Random r = MyRandom.getRandom();

        // Don't use draw abilities before main 2 if possible
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (AbilityFactory.waitForBlocking(sa)) {
            return false;
        }

        double chance = .4; // 40 percent chance of milling with instant speed
                            // stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed
        }

        if ((Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.END_OF_TURN) && Singletons.getModel().getGameState().getPhaseHandler().isNextTurn(
                AllZone.getComputerPlayer()))) {
            chance = .9; // 90% for end of opponents turn
        }

        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (params.get("NumCards").equals("X") && source.getSVar("X").startsWith("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int cardsToDiscard = Math.min(ComputerUtil.determineLeftoverMana(sa), AllZone.getHumanPlayer()
                    .getCardsIn(ZoneType.Library).size());
            source.setSVar("PayX", Integer.toString(cardsToDiscard));
            if (cardsToDiscard <= 0) {
                return false;
            }
        }

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
            // some other variables here, like deck size, and phase and other fun stuff
        }

        return randomReturn;
    }

    /**
     * <p>
     * millTargetAI.
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
    private static boolean millTargetAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final Target tgt = sa.getTarget();
        final HashMap<String, String> params = af.getMapParams();

        if (tgt != null) {
            tgt.resetTargets();
            if (!sa.canTarget(AllZone.getHumanPlayer())) {
                if (mandatory && sa.canTarget(AllZone.getComputerPlayer())) {
                    tgt.addTarget(AllZone.getComputerPlayer());
                    return true;
                }
                return false;
            }

            final int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);

            final CardList pLibrary = AllZone.getHumanPlayer().getCardsIn(ZoneType.Library);

            if (pLibrary.size() == 0) { // deck already empty, no need to mill
                if (!mandatory) {
                    return false;
                }

                tgt.addTarget(AllZone.getHumanPlayer());
                return true;
            }

            if (numCards >= pLibrary.size()) {
                // Can Mill out Human's deck? Do it!
                tgt.addTarget(AllZone.getHumanPlayer());
                return true;
            }

            // Obscure case when you know what your top card is so you might?
            // want to mill yourself here
            // if (AI wants to mill self)
            // tgt.addTarget(AllZone.getComputerPlayer());
            // else
            tgt.addTarget(AllZone.getHumanPlayer());
        }
        return true;
    }

    /**
     * <p>
     * millDrawback.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean millDrawback(final AbilityFactory af, final SpellAbility sa) {
        if (!AbilityFactoryZoneAffecting.millTargetAI(af, sa, true)) {
            return false;
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.chkAIDrawback();
        }

        return true;
    }

    private static boolean millTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (!AbilityFactoryZoneAffecting.millTargetAI(af, sa, mandatory)) {
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();

        final Card source = sa.getSourceCard();
        if (params.get("NumCards").equals("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int cardsToDiscard = Math.min(ComputerUtil.determineLeftoverMana(sa), AllZone.getHumanPlayer()
                    .getCardsIn(ZoneType.Library).size());
            source.setSVar("PayX", Integer.toString(cardsToDiscard));
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
     * millResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void millResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        final int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
        final boolean bottom = params.containsKey("FromBottom");

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        if (destination == null) {
            destination = ZoneType.Graveyard;
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final CardList milled = p.mill(numCards, destination, bottom);
                if (params.containsKey("RememberMilled")) {
                    for (final Card c : milled) {
                        source.addRemembered(c);
                    }
                }
                if (params.containsKey("Imprint")) {
                    for (final Card c : milled) {
                        source.addImprinted(c);
                    }
                }
            }
        }
    }

    // ////////////////////
    //
    // Discard stuff
    //
    // ////////////////////

    // NumCards - the number of cards to be discarded (may be integer or X)
    // Mode - the mode of discard - should match spDiscard
    // -Random
    // -TgtChoose
    // -RevealYouChoose
    // -RevealOppChoose
    // -RevealDiscardAll (defaults to Card if DiscardValid is missing)
    // -Hand
    // DiscardValid - a ValidCards syntax for acceptable cards to discard
    // UnlessType - a ValidCards expression for
    // "discard x unless you discard a ..."

    // Examples:
    // A:SP$Discard | Cost$B | Tgt$TgtP | NumCards$2 | Mode$Random |
    // SpellDescription$<...>
    // A:AB$Discard | Cost$U | ValidTgts$ Opponent | Mode$RevealYouChoose |
    // NumCards$X | SpellDescription$<...>

    /**
     * <p>
     * createAbilityDiscard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDiscard(final AbilityFactory af) {
        class AbilityDiscard extends AbilityActivated {
            public AbilityDiscard(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityDiscard(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 4348585353456736817L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.discardStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.discardCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.discardResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryZoneAffecting.discardTrigger(af, this, mandatory);
            }
        }

        final SpellAbility abDiscard = new AbilityDiscard(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abDiscard;
    }

    /**
     * <p>
     * createSpellDiscard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDiscard(final AbilityFactory af) {
        final SpellAbility spDiscard = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4348585353456736817L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.discardStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.discardCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.discardResolve(af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryZoneAffecting.discardTriggerNoCost(af, this, mandatory);
                }
                return AbilityFactoryZoneAffecting.discardTrigger(af, this, mandatory);
            }

        };
        return spDiscard;
    }

    /**
     * <p>
     * createDrawbackDiscard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackDiscard(final AbilityFactory af) {
        class DrawbackDiscard extends AbilitySub {
            public DrawbackDiscard(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackDiscard(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 4348585353456736817L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.discardStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.discardResolve(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.discardCanPlayAI(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryZoneAffecting.discardCheckDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryZoneAffecting.discardTrigger(af, this, mandatory);
            }
        }
        final SpellAbility dbDiscard = new DrawbackDiscard(af.getHostCard(), af.getAbTgt());

        return dbDiscard;
    }

    /**
     * <p>
     * discardResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void discardResolve(final AbilityFactory af, final SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final Card host = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();
        final String mode = params.get("Mode");

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        final CardList discarded = new CardList();

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (mode.equals("Defined")) {
                    final ArrayList<Card> toDiscard = AbilityFactory.getDefinedCards(host, params.get("DefinedCards"),
                            sa);
                    for (final Card c : toDiscard) {
                        discarded.addAll(p.discard(c, sa));
                    }
                    if (params.containsKey("RememberDiscarded")) {
                        for (final Card c : discarded) {
                            source.addRemembered(c);
                        }
                    }
                    continue;
                }

                if (mode.equals("Hand")) {
                    final CardList list = p.discardHand(sa);
                    if (params.containsKey("RememberDiscarded")) {
                        for (final Card c : list) {
                            source.addRemembered(c);
                        }
                    }
                    continue;
                }

                int numCards = 1;
                if (params.containsKey("NumCards")) {
                    numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
                    if (p.getCardsIn(ZoneType.Hand).size() > 0
                            && p.getCardsIn(ZoneType.Hand).size() < numCards) {
                        // System.out.println("Scale down discard from " + numCards + " to " + p.getCardsIn(ZoneType.Hand).size());
                        numCards = p.getCardsIn(ZoneType.Hand).size();
                    }
                }

                if (mode.equals("Random")) {
                    final String valid = params.containsKey("DiscardValid") ? params.get("DiscardValid") : "Card";
                    discarded.addAll(p.discardRandom(numCards, sa, valid));
                } else if (mode.equals("TgtChoose") && params.containsKey("UnlessType")) {
                    p.discardUnless(numCards, params.get("UnlessType"), sa);
                } else if (mode.equals("RevealDiscardAll")) {
                    // Reveal
                    final CardList dPHand = p.getCardsIn(ZoneType.Hand);

                    if (p.isHuman()) {
                        // "reveal to computer" for information gathering
                    } else {
                        GuiUtils.chooseOneOrNone("Revealed computer hand", dPHand.toArray());
                    }

                    String valid = params.get("DiscardValid");
                    if (valid == null) {
                        valid = "Card";
                    }

                    if (valid.contains("X")) {
                        valid = valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(source, "X", sa)));
                    }

                    final CardList dPChHand = dPHand.getValidCards(valid.split(","), source.getController(), source);
                    // Reveal cards that will be discarded?
                    for (final Card c : dPChHand) {
                        p.discard(c, sa);
                        discarded.add(c);
                    }
                } else if (mode.equals("RevealYouChoose") || mode.equals("RevealOppChoose") || mode.equals("TgtChoose")) {
                    // Is Reveal you choose right? I think the wrong player is
                    // being used?
                    CardList dPHand = p.getCardsIn(ZoneType.Hand);
                    if (dPHand.size() != 0) {
                        if (params.containsKey("RevealNumber")) {
                            String amountString = params.get("RevealNumber");
                            int amount = amountString.matches("[0-9][0-9]?") ? Integer.parseInt(amountString)
                                    : CardFactoryUtil.xCount(source, source.getSVar(amountString));
                            dPHand = AbilityFactoryReveal.getRevealedList(p, dPHand, amount);
                        }
                        CardList dPChHand = new CardList(dPHand);
                        String[] dValid = null;
                        if (params.containsKey("DiscardValid")) { // Restrict card choices
                            dValid = params.get("DiscardValid").split(",");
                            dPChHand = dPHand.getValidCards(dValid, source.getController(), source);
                        }
                        Player chooser = p;
                        if (mode.equals("RevealYouChoose")) {
                            chooser = source.getController();
                        } else if (mode.equals("RevealOppChoose")) {
                            chooser = source.getController().getOpponent();
                        }

                        if (chooser.isComputer()) {
                            // AI
                            if (p.isComputer()) { // discard AI cards
                                CardList list = ComputerUtil.discardNumTypeAI(numCards, dValid, sa);
                                if (mode.startsWith("Reveal")) {
                                    GuiUtils.chooseOneOrNone("Computer has chosen", list.toArray());
                                }
                                discarded.addAll(list);
                                for (Card card : list) {
                                    p.discard(card, sa);
                                }
                                continue;
                            }
                            // discard human cards
                            for (int i = 0; i < numCards; i++) {
                                if (dPChHand.size() > 0) {
                                    final CardList dChoices = new CardList();
                                    if (params.containsKey("DiscardValid")) {
                                        final String validString = params.get("DiscardValid");
                                        if (validString.contains("Creature") && !validString.contains("nonCreature")) {
                                            final Card c = CardFactoryUtil.getBestCreatureAI(dPChHand);
                                            if (c != null) {
                                                dChoices.add(CardFactoryUtil.getBestCreatureAI(dPChHand));
                                            }
                                        }
                                    }

                                    CardListUtil.sortByTextLen(dPChHand);
                                    dChoices.add(dPChHand.get(0));

                                    CardListUtil.sortCMC(dPChHand);
                                    dChoices.add(dPChHand.get(0));

                                    final Card dC = dChoices.get(CardUtil.getRandomIndex(dChoices));
                                    dPChHand.remove(dC);

                                    if (mode.startsWith("Reveal")) {
                                        final CardList dCs = new CardList();
                                        dCs.add(dC);
                                        GuiUtils.chooseOneOrNone("Computer has chosen", dCs.toArray());
                                    }
                                    discarded.add(dC);
                                    p.discard(dC, sa);
                                }
                            }
                        } else {
                            // human
                            if (mode.startsWith("Reveal")) {
                                GuiUtils.chooseOneOrNone("Revealed " + p + "  hand", dPHand.toArray());
                            }

                            for (int i = 0; i < numCards; i++) {
                                if (dPChHand.size() > 0) {
                                    Card dC = null;
                                    if (params.containsKey("Optional")) {
                                        dC = GuiUtils.chooseOneOrNone("Choose a card to be discarded",
                                                dPChHand.toArray());
                                    } else {
                                    dC = GuiUtils.chooseOne("Choose a card to be discarded",
                                            dPChHand.toArray());
                                    } if (dC != null) {
                                        dPChHand.remove(dC);
                                        discarded.add(dC);
                                        p.discard(dC, sa);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (params.containsKey("RememberDiscarded")) {
            for (final Card c : discarded) {
                source.addRemembered(c);
            }
        }

    } // discardResolve()

    /**
     * <p>
     * discardStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String discardStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final String mode = params.get("Mode");
        final StringBuilder sb = new StringBuilder();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        if (tgtPlayers.size() > 0) {

            for (final Player p : tgtPlayers) {
                sb.append(p.toString()).append(" ");
            }

            if (mode.equals("RevealYouChoose")) {
                sb.append("reveals his or her hand.").append("  You choose (");
            } else if (mode.equals("RevealDiscardAll")) {
                sb.append("reveals his or her hand. Discard (");
            } else {
                sb.append("discards (");
            }

            int numCards = 1;
            if (params.containsKey("NumCards")) {
                numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
            }

            if (mode.equals("Hand")) {
                sb.append("his or her hand");
            } else if (mode.equals("RevealDiscardAll")) {
                sb.append("All");
            } else {
                sb.append(numCards);
            }

            sb.append(")");

            if (mode.equals("RevealYouChoose")) {
                sb.append(" to discard");
            } else if (mode.equals("RevealDiscardAll")) {
                String valid = params.get("DiscardValid");
                if (valid == null) {
                    valid = "Card";
                }
                sb.append(" of type: ").append(valid);
            }

            if (mode.equals("Defined")) {
                sb.append(" defined cards");
            }

            if (mode.equals("Random")) {
                sb.append(" at random.");
            } else {
                sb.append(".");
            }
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // discardStackDescription()

    /**
     * <p>
     * discardCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean discardCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final Cost abCost = sa.getPayCosts();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        final boolean humanHasHand = AllZone.getHumanPlayer().getCardsIn(ZoneType.Hand).size() > 0;

        if (tgt != null) {
            if (!AbilityFactoryZoneAffecting.discardTargetAI(af, sa)) {
                return false;
            }
        } else {
            // TODO: Add appropriate restrictions
            final ArrayList<Player> players = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                    params.get("Defined"), sa);

            if (players.size() == 1) {
                if (players.get(0).isComputer()) {
                    // the ai should only be using something like this if he has
                    // few cards in hand,
                    // cards like this better have a good drawback to be in the
                    // AIs deck
                } else {
                    // defined to the human, so that's fine as long the human
                    // has cards
                    if (!humanHasHand) {
                        return false;
                    }
                }
            } else {
                // Both players discard, any restrictions?
            }
        }

        if (!params.get("Mode").equals("Hand") && !params.get("Mode").equals("RevealDiscardAll")) {
            if (params.get("NumCards").equals("X") && source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int cardsToDiscard = Math.min(ComputerUtil.determineLeftoverMana(sa), AllZone.getHumanPlayer()
                        .getCardsIn(ZoneType.Hand).size());
                source.setSVar("PayX", Integer.toString(cardsToDiscard));
            }
        }

        // Don't use draw abilities before main 2 if possible
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) 
                && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (AbilityFactory.waitForBlocking(sa) && !params.containsKey("ActivationPhases")) {
            return false;
        }

        double chance = .5; // 50 percent chance of discarding with instant
                            // speed stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .75; // 75% chance for sorcery speed
        }

        if ((Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.END_OF_TURN) && Singletons.getModel().getGameState().getPhaseHandler().isNextTurn(
                AllZone.getComputerPlayer()))) {
            chance = .9; // 90% for end of opponents turn
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

        // some other variables here, like handsize vs. maxHandSize

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }
        return randomReturn;
    } // discardCanPlayAI()

    /**
     * <p>
     * discardTargetAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean discardTargetAI(final AbilityFactory af, final SpellAbility sa) {
        final Target tgt = sa.getTarget();
        if (AllZone.getHumanPlayer().getCardsIn(ZoneType.Hand).size() < 1) {
            return false;
        }
        if (tgt != null) {
            if (sa.canTarget(AllZone.getHumanPlayer())) {
                tgt.addTarget(AllZone.getHumanPlayer());
                return true;
            }
        }
        return false;
    } // discardTargetAI()

    /**
     * <p>
     * discardTrigger.
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
    private static boolean discardTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }
        return discardTriggerNoCost(af, sa, mandatory);
    }

    /**
     * <p>
     * discardTriggerNoCost.
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
    private static boolean discardTriggerNoCost(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            if (!AbilityFactoryZoneAffecting.discardTargetAI(af, sa)) {
                if (mandatory && sa.canTarget(AllZone.getHumanPlayer())) {
                    tgt.addTarget(AllZone.getHumanPlayer());
                } else if (mandatory && sa.canTarget(AllZone.getComputerPlayer())) {
                    tgt.addTarget(AllZone.getComputerPlayer());
                } else {
                    return false;
                }
            }
        }

        return true;
    } // discardTrigger()

    /**
     * <p>
     * discardCheckDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param subAb
     *            a {@link forge.card.spellability.AbilitySub} object.
     * @return a boolean.
     */
    private static boolean discardCheckDrawbackAI(final AbilityFactory af, final AbilitySub subAb) {
        // Drawback AI improvements
        // if parent draws cards, make sure cards in hand + cards drawn > 0
        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            return AbilityFactoryZoneAffecting.discardTargetAI(af, subAb);
        }
        // TODO: check for some extra things
        return true;
    } // discardCheckDrawbackAI()

    // **********************************************************************
    // ******************************* Shuffle ******************************
    // **********************************************************************

    /**
     * <p>
     * createAbilityShuffle.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityShuffle(final AbilityFactory af) {
        class AbilityShuffle extends AbilityActivated {
            public AbilityShuffle(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityShuffle(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -1245185178904838198L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.shuffleStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.shuffleCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.shuffleResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryZoneAffecting.shuffleTrigger(af, this, mandatory);
            }
        }
        final SpellAbility abShuffle = new AbilityShuffle(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abShuffle;
    }

    /**
     * <p>
     * createSpellShuffle.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellShuffle(final AbilityFactory af) {
        final SpellAbility spShuffle = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 589035800601547559L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.shuffleStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryZoneAffecting.shuffleCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.shuffleResolve(af, this);
            }

        };
        return spShuffle;
    }

    /**
     * <p>
     * createDrawbackShuffle.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackShuffle(final AbilityFactory af) {
        class DrawbackShuffle extends AbilitySub {
            public DrawbackShuffle(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackShuffle(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5974307947494280639L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryZoneAffecting.shuffleStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryZoneAffecting.shuffleResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryZoneAffecting.shuffleTargetAI(af, this, false, false);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryZoneAffecting.shuffleTrigger(af, this, mandatory);
            }
        }
        final SpellAbility dbShuffle = new DrawbackShuffle(af.getHostCard(), af.getAbTgt());

        return dbShuffle;
    }

    /**
     * <p>
     * shuffleStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String shuffleStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

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

        if (tgtPlayers.size() > 0) {
            final Iterator<Player> it = tgtPlayers.iterator();
            while (it.hasNext()) {
                sb.append(it.next().getName());
                if (it.hasNext()) {
                    sb.append(" and ");
                }
            }
        } else {
            sb.append("Error - no target players for Shuffle. ");
        }
        sb.append(" shuffle");
        if (tgtPlayers.size() > 1) {
            sb.append(" their libraries");
        } else {
            sb.append("s his or her library");
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
     * shuffleCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean shuffleCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // not really sure when the compy would use this; maybe only after a
        // human
        // deliberately put a card on top of their library
        return false;
        /*
         * if (!ComputerUtil.canPayCost(sa)) return false;
         * 
         * Card source = sa.getSourceCard();
         * 
         * Random r = MyRandom.random; boolean randomReturn = r.nextFloat() <=
         * Math.pow(.667, sa.getActivationsThisTurn()+1);
         * 
         * if (AbilityFactory.playReusable(sa)) randomReturn = true;
         * 
         * Ability_Sub subAb = sa.getSubAbility(); if (subAb != null)
         * randomReturn &= subAb.chkAI_Drawback(); return randomReturn;
         */
    }

    /**
     * <p>
     * shuffleTargetAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param primarySA
     *            a boolean.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean shuffleTargetAI(final AbilityFactory af, final SpellAbility sa, final boolean primarySA,
            final boolean mandatory) {
        return false;
    } // shuffleTargetAI()

    /**
     * <p>
     * shuffleTrigger.
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
    private static boolean shuffleTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (!AbilityFactoryZoneAffecting.shuffleTargetAI(af, sa, false, mandatory)) {
            return false;
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
     * shuffleResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void shuffleResolve(final AbilityFactory af, final SpellAbility sa) {
        final Card host = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();
        final boolean optional = params.containsKey("Optional");

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (optional && sa.getActivatingPlayer().isHuman()
                        && !GameActionUtil.showYesNoDialog(host, "Have " + p + " shuffle?")) {
                } else {
                    p.shuffle();
                }
            }
        }
    }

} // end class AbilityFactory_ZoneAffecting
