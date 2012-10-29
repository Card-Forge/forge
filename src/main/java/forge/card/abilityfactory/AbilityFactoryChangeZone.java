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
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardCharacteristicName;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Constant;
import forge.GameActionUtil;
import forge.GameEntity;
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
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.ComputerUtilBlock;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.MyRandom;


/**
 * <p>
 * AbilityFactory_ChangeZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryChangeZone {

    private AbilityFactoryChangeZone() {
        throw new AssertionError();
    }

    // Change Zone is going to work much differently than other AFs.
    // *NOTE* Please do not use this as a base for copying and creating your own
    // AF

    /**
     * <p>
     * createAbilityChangeZone.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityChangeZone(final AbilityFactory af) {
        class AbilityChangeZone extends AbilityActivated {
            public AbilityChangeZone(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityChangeZone(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                AbilityFactoryChangeZone.setMiscellaneous(af, res);
                return res;
            }

            private static final long serialVersionUID = 3728332812890211671L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChangeZone.changeZoneCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeZone.changeZoneResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeZone.changeZoneDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChangeZone.changeZoneTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abChangeZone = new AbilityChangeZone(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        AbilityFactoryChangeZone.setMiscellaneous(af, abChangeZone);
        return abChangeZone;
    }

    /**
     * <p>
     * createSpellChangeZone.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellChangeZone(final AbilityFactory af) {
        final SpellAbility spChangeZone = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3270484211099902059L;

            @Override
            public boolean canPlayAI() {
                
                return AbilityFactoryChangeZone.changeZoneCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeZone.changeZoneResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeZone.changeZoneDescription(af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryChangeZone.changeZoneTriggerAINoCost(getActivatingPlayer(), af, this, mandatory);
                }
                return AbilityFactoryChangeZone.changeZoneTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        };
        AbilityFactoryChangeZone.setMiscellaneous(af, spChangeZone);
        return spChangeZone;
    }

    /**
     * <p>
     * createDrawbackChangeZone.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackChangeZone(final AbilityFactory af) {
        class DrawbackChangeZone extends AbilitySub {
            public DrawbackChangeZone(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackChangeZone(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                AbilityFactoryChangeZone.setMiscellaneous(af, res);
                return res;
            }
            private static final long serialVersionUID = 3270484211099902059L;

            @Override
            public void resolve() {
                AbilityFactoryChangeZone.changeZoneResolve(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChangeZone.changeZoneCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryChangeZone.changeZonePlayDrawbackAI(getActivatingPlayer(), af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeZone.changeZoneDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChangeZone.changeZoneTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbChangeZone = new DrawbackChangeZone(af.getHostCard(), af.getAbTgt());

        AbilityFactoryChangeZone.setMiscellaneous(af, dbChangeZone);
        return dbChangeZone;
    }

    /**
     * <p>
     * isHidden.
     * </p>
     * 
     * @param origin
     *            a {@link java.lang.String} object.
     * @param hiddenOverride
     *            a boolean.
     * @return a boolean.
     */
    public static boolean isHidden(final String origin, final boolean hiddenOverride) {
        List<ZoneType> zone = ZoneType.listValueOf(origin);
        
        if (hiddenOverride || zone.isEmpty()) {
            return true;
        }
        
        for (ZoneType z : zone) {
            if (z.isHidden()) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * isKnown.
     * </p>
     * 
     * @param origin
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isKnown(final String origin) {
        return !isHidden(origin, false);
    }

    /**
     * <p>
     * setMiscellaneous.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void setMiscellaneous(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (params.containsKey("Origin")) {
            origin = ZoneType.listValueOf(params.get("Origin"));
        }

        final Target tgt = sa.getTarget();

        // Don't set the zone if it targets a player
        if ((tgt != null) && !tgt.canTgtPlayer()) {
            sa.getTarget().setZone(origin);
        }
    }

    /**
     * <p>
     * changeZoneCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeZoneCanPlayAI(final Player aiPlayer,final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        String origin = "";
        if (params.containsKey("Origin")) {
            origin = params.get("Origin");
        }

        if (AbilityFactoryChangeZone.isHidden(origin, params.containsKey("Hidden"))) {
            return AbilityFactoryChangeZone.changeHiddenOriginCanPlayAI(aiPlayer, af, sa);
        } else if (AbilityFactoryChangeZone.isKnown(origin)) {
            return AbilityFactoryChangeZone.changeKnownOriginCanPlayAI(aiPlayer, af, sa);
        }

        return false;
    }

    /**
     * <p>
     * changeZonePlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeZonePlayDrawbackAI(final Player aiPlayer, final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        String origin = "";
        if (params.containsKey("Origin")) {
            origin = params.get("Origin");
        }

        if (AbilityFactoryChangeZone.isHidden(origin, params.containsKey("Hidden"))) {
            return AbilityFactoryChangeZone.changeHiddenOriginPlayDrawbackAI(aiPlayer, af, sa);
        } else if (AbilityFactoryChangeZone.isKnown(origin)) {
            return AbilityFactoryChangeZone.changeKnownOriginPlayDrawbackAI(aiPlayer, af, sa);
        }

        return false;
    }

    /**
     * <p>
     * changeZoneTriggerAI.
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
    private static boolean changeZoneTriggerAI(final Player ai,final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
            return false;
        }

        return AbilityFactoryChangeZone.changeZoneTriggerAINoCost(ai, af, sa, mandatory);
    }

    /**
     * <p>
     * changeZoneTriggerAINoCost.
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
    private static boolean changeZoneTriggerAINoCost(final Player aiPlayer,final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final HashMap<String, String> params = af.getMapParams();
        String origin = "";
        if (params.containsKey("Origin")) {
            origin = params.get("Origin");
        }

        if (AbilityFactoryChangeZone.isHidden(origin, params.containsKey("Hidden"))) {
            return AbilityFactoryChangeZone.changeHiddenTriggerAI(aiPlayer, af, sa, mandatory);
        } else if (AbilityFactoryChangeZone.isKnown(origin)) {
            return AbilityFactoryChangeZone.changeKnownOriginTriggerAI(aiPlayer, af, sa, mandatory);
        }

        return false;
    }

    /**
     * <p>
     * changeZoneDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeZoneDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        String origin = "";
        if (params.containsKey("Origin")) {
            origin = params.get("Origin");
        }

        if (AbilityFactoryChangeZone.isHidden(origin, params.containsKey("Hidden"))) {
            return AbilityFactoryChangeZone.changeHiddenOriginStackDescription(af, sa);
        } else if (AbilityFactoryChangeZone.isKnown(origin)) {
            return AbilityFactoryChangeZone.changeKnownOriginStackDescription(af, sa);
        }

        return "";
    }

    /**
     * <p>
     * changeZoneResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeZoneResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        String origin = "";
        if (params.containsKey("Origin")) {
            origin = params.get("Origin");
        }

        if (AbilityFactoryChangeZone.isHidden(origin, params.containsKey("Hidden")) && !params.containsKey("Ninjutsu")) {
            AbilityFactoryChangeZone.changeHiddenOriginResolve(af, sa);
        } else {
            //else if (AbilityFactoryChangeZone.isKnown(origin) || params.containsKey("Ninjutsu")) {
            // Why is this an elseif and not just an else?
            AbilityFactoryChangeZone.changeKnownOriginResolve(af, sa);
        }
    }

    // *************************************************************************************
    // ************ Hidden Origin (Library/Hand/Sideboard/Non-targetd other)
    // ***************
    // ******* Hidden origin cards are chosen on the resolution of the spell
    // ***************
    // ******* It is possible for these to have Destination of Battlefield
    // *****************
    // ****** Example: Cavern Harpy where you don't choose the card until
    // resolution *******
    // *************************************************************************************

    /**
     * <p>
     * changeHiddenOriginCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeHiddenOriginCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        // Fetching should occur fairly often as it helps cast more spells, and
        // have access to more mana
        final Cost abCost = af.getAbCost();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        ZoneType origin = null;
        final Player opponent = ai.getOpponent();
        
        if (params.containsKey("Origin")) {
            origin = ZoneType.smartValueOf(params.get("Origin"));
        }
        final String destination = params.get("Destination");

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(ai, abCost, source)
                    && !(destination.equals("Battlefield") && !source.isLand())) {
                return false;
            }

            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                for (final CostPart part : abCost.getCostParts()) {
                    if (part instanceof CostDiscard) {
                        CostDiscard cd = (CostDiscard) part;
                        // this is mainly for typecycling
                        if (!cd.getThis() || !ComputerUtil.isWorseThanDraw(ai, source)) {
                            return false;
                        }
                    }
                }
            }
            
            //Ninjutsu
            if (params.containsKey("Ninjutsu")) {
                if (source.isType("Legendary") && !Singletons.getModel().getGame().isCardInPlay("Mirror Gallery")) {
                    final List<Card> list = ai.getCardsIn(ZoneType.Battlefield);
                    if (Iterables.any(list, CardPredicates.nameEquals(source.getName()))) {
                        return false;
                    }
                }
                if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE)) {
                    return false;
                }
                List<Card> attackers = new ArrayList<Card>();
                attackers.addAll(Singletons.getModel().getGame().getCombat().getUnblockedAttackers());
                boolean lowerCMC = false;
                for (Card attacker : attackers) {
                    if (attacker.getCMC() < source.getCMC()) {
                        lowerCMC = true;
                    }
                }
                if (!lowerCMC) {
                    return false;
                }
            }
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

        final Random r = MyRandom.getRandom();
        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        ArrayList<Player> pDefined = new ArrayList<Player>();
        pDefined.add(source.getController());
        final Target tgt = sa.getTarget();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            if (af.isCurse() && sa.canTarget(opponent)) {
                tgt.addTarget(opponent);
            } else if (!af.isCurse() && sa.canTarget(ai)) {
                tgt.addTarget(ai);
            }
            pDefined = tgt.getTargetPlayers();
        } else {
            if (params.containsKey("DefinedPlayer")) {
                pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("DefinedPlayer"), sa);
            } else {
                pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        }

        String type = params.get("ChangeType");
        if (type != null) {
            if (type.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(xPay));
                type = type.replace("X", Integer.toString(xPay));
            }
        }

        for (final Player p : pDefined) {
            List<Card> list = p.getCardsIn(origin);

            if ((type != null) && p.isComputer()) {
                // AI only "knows" about his information
                list = CardLists.getValidCards(list, type, source.getController(), source);
            }

            if (list.isEmpty()) {
                return false;
            }
        }

        // don't use fetching to top of library/graveyard before main2
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !params.containsKey("ActivationPhases")) {
            if (!destination.equals("Battlefield") && !destination.equals("Hand")) {
                return false;
            }
            // Only tutor something in main1 if hand is almost empty
            if (ai.getCardsIn(ZoneType.Hand).size() > 1 && destination.equals("Hand")) {
                return false;
            }
        }

        chance &= (r.nextFloat() < .8);

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * changeHiddenOriginPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeHiddenOriginPlayDrawbackAI(final Player aiPlayer, final AbilityFactory af, final SpellAbility sa) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:
        final Target tgt = sa.getTarget();
        final Player opp = aiPlayer.getOpponent();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            if (af.isCurse() && sa.canTarget(opp)) {
                tgt.addTarget(opp);
            } else if (!af.isCurse() && sa.canTarget(aiPlayer)) {
                tgt.addTarget(aiPlayer);
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * changeHiddenTriggerAI.
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
    private static boolean changeHiddenTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // Fetching should occur fairly often as it helps cast more spells, and
        // have access to more mana

        final Card source = sa.getSourceCard();

        final HashMap<String, String> params = af.getMapParams();

        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (params.containsKey("Origin")) {
            origin = ZoneType.listValueOf(params.get("Origin"));
        }

        // this works for hidden because the mana is paid first.
        final String type = params.get("ChangeType");
        if ((type != null) && type.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        ArrayList<Player> pDefined;
        final Target tgt = sa.getTarget();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            final Player opp = ai.getOpponent();

            if (af.isCurse()) {
                if (sa.canTarget(opp)) {
                    tgt.addTarget(opp);
                } else if (mandatory && sa.canTarget(ai)) {
                    tgt.addTarget(ai);
                }
            } else {
                if (sa.canTarget(ai)) {
                    tgt.addTarget(ai);
                } else if (mandatory && sa.canTarget(opp)) {
                    tgt.addTarget(opp);
                }
            }

            pDefined = tgt.getTargetPlayers();

            if (pDefined.isEmpty()) {
                return false;
            }

            if (mandatory) {
                return pDefined.size() > 0;
            }
        } else {
            if (mandatory) {
                return true;
            }
            pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : pDefined) {
            List<Card> list = p.getCardsIn(origin);

            // Computer should "know" his deck
            if (p.isComputer()) {
                list = AbilityFactory.filterListByType(list, params.get("ChangeType"), sa);
            }

            if (list.isEmpty()) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            return subAb.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * changeHiddenOriginStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeHiddenOriginStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are
        // added
        final HashMap<String, String> params = af.getMapParams();

        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        if (params.containsKey("StackDescription")) {
            String stackDesc = params.get("StackDescription");
            if (stackDesc.equals("None")) {
                // Intentionally blank to avoid double spaces, otherwise: sb.append("");
            } else if (stackDesc.equals("SpellDescription")) {
                sb.append(params.get("SpellDescription"));
            } else {
                sb.append(stackDesc);
            }
        } else {
            String origin = "";
            if (params.containsKey("Origin")) {
                origin = params.get("Origin");
            }
            final String destination = params.get("Destination");

            final String type = params.containsKey("ChangeType") ? params.get("ChangeType") : "Card";
            final int num = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(host,
                    params.get("ChangeNum"), sa) : 1;

            if (origin.equals("Library") && params.containsKey("Defined")) {
                // for now, just handle the Exile from top of library case, but
                // this can be expanded...
                if (destination.equals("Exile")) {
                    sb.append("Exile the top card of your library");
                    if (params.containsKey("ExileFaceDown")) {
                        sb.append(" face down");
                    }
                } else if (destination.equals("Ante")) {
                    sb.append("Add the top card of your library to the ante");
                }
                sb.append(".");
            } else if (origin.equals("Library")) {
                sb.append("Search your library for ").append(num).append(" ").append(type).append(" and ");

                if (num == 1) {
                    sb.append("put that card ");
                } else {
                    sb.append("put those cards ");
                }

                if (destination.equals("Battlefield")) {
                    sb.append("onto the battlefield");
                    if (params.containsKey("Tapped")) {
                        sb.append(" tapped");
                    }

                    sb.append(".");

                }
                if (destination.equals("Hand")) {
                    sb.append("into your hand.");
                }
                if (destination.equals("Graveyard")) {
                    sb.append("into your graveyard.");
                }

                sb.append(" Then shuffle your library.");
            } else if (origin.equals("Hand")) {
                sb.append("Put ").append(num).append(" ").append(type).append(" card(s) from your hand ");

                if (destination.equals("Battlefield")) {
                    sb.append("onto the battlefield.");
                }
                if (destination.equals("Library")) {
                    final int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                            .get("LibraryPosition")) : 0;

                    if (libraryPos == 0) {
                        sb.append("on top");
                    }
                    if (libraryPos == -1) {
                        sb.append("on bottom");
                    }

                    sb.append(" of your library.");
                }
            } else if (origin.equals("Battlefield")) {
                // TODO Expand on this Description as more cards use it
                // for the non-targeted SAs when you choose what is returned on
                // resolution
                sb.append("Return ").append(num).append(" ").append(type).append(" card(s) ");
                sb.append(" to your ").append(destination);
            }
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeHiddenOriginResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeHiddenOriginResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        ArrayList<Player> fetchers;

        if (params.containsKey("DefinedPlayer")) {
            fetchers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("DefinedPlayer"), sa);
        } else {
            fetchers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        // handle case when Defined is for a Card
        if (fetchers.isEmpty()) {
            fetchers.add(sa.getSourceCard().getController());
        }

        Player chooser = null;
        if (params.containsKey("Chooser")) {
            final String choose = params.get("Chooser");
            if (choose.equals("Targeted") && (sa.getTarget().getTargetPlayers() != null)) {
                chooser = sa.getTarget().getTargetPlayers().get(0);
            } else {
                chooser = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), choose, sa).get(0);
            }
        }

        for (final Player player : fetchers) {
            Player decider = chooser;
            if (decider == null) {
                decider = player;
            }
            if (decider.isComputer()) {
                AbilityFactoryChangeZone.changeHiddenOriginResolveAI(decider, af, sa, player);
            } else {
                AbilityFactoryChangeZone.changeHiddenOriginResolveHuman(af, sa, player);
            }
        }
    }

    /**
     * <p>
     * changeHiddenOriginResolveHuman.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    private static void changeHiddenOriginResolveHuman(final AbilityFactory af, final SpellAbility sa, Player player) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();
        final List<Card> movedCards = new ArrayList<Card>();
        final boolean defined = params.containsKey("Defined");

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final ArrayList<Player> players = tgt.getTargetPlayers();
            player = players.get(0);
            if (players.contains(player) && !player.canBeTargetedBy(sa)) {
                return;
            }
        }

        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (params.containsKey("Origin")) {
            origin = ZoneType.listValueOf(params.get("Origin"));
        }
        ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        // this needs to be zero indexed. Top = 0, Third = 2
        int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;

        if (params.containsKey("OriginChoice")) {
            // Currently only used for Mishra, but may be used by other things
            // Improve how this message reacts for other cards
            final List<ZoneType> alt = ZoneType.listValueOf(params.get("OriginAlternative"));
            List<Card> altFetchList = player.getCardsIn(alt);
            altFetchList = AbilityFactory.filterListByType(altFetchList, params.get("ChangeType"), sa);

            final StringBuilder sb = new StringBuilder();
            sb.append(params.get("AlternativeMessage")).append(" ");
            sb.append(altFetchList.size()).append(" cards match your searching type in Alternate Zones.");

            if (!GameActionUtil.showYesNoDialog(card, sb.toString())) {
                origin = alt;
            }
        }

        if (params.containsKey("DestinationAlternative")) {

            final StringBuilder sb = new StringBuilder();
            sb.append(params.get("AlternativeDestinationMessage"));

            if (!GameActionUtil.showYesNoDialog(card, sb.toString())) {
                destination = ZoneType.smartValueOf(params.get("DestinationAlternative"));
                libraryPos = params.containsKey("LibraryPositionAlternative") ? Integer.parseInt(params
                        .get("LibraryPositionAlternative")) : 0;
            }
        }

        int changeNum = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(card, params.get("ChangeNum"),
                sa) : 1;

        List<Card> fetchList;
        if (defined) {
            fetchList = new ArrayList<Card>(AbilityFactory.getDefinedCards(card, params.get("Defined"), sa));
            if (!params.containsKey("ChangeNum")) {
                changeNum = fetchList.size();
            }
        } else if (!origin.contains(ZoneType.Library) && !origin.contains(ZoneType.Hand)
                && !params.containsKey("DefinedPlayer")) {
            fetchList = Singletons.getModel().getGame().getCardsIn(origin);
        } else {
            fetchList = player.getCardsIn(origin);
        }

        if (!defined) {
            if (origin.contains(ZoneType.Library) && !defined && !params.containsKey("NoLooking")) { 
                // Look at whole library before moving onto choosing a card
                GuiChoose.oneOrNone(sa.getSourceCard().getName() + " - Looking at Library",
                        player.getCardsIn(ZoneType.Library));
            }

            // Look at opponents hand before moving onto choosing a card
            if (origin.contains(ZoneType.Hand) && player.isComputer()) {
                GuiChoose.oneOrNone(sa.getSourceCard().getName() + " - Looking at Opponent's Hand", player
                        .getCardsIn(ZoneType.Hand));
            }
            fetchList = AbilityFactory.filterListByType(fetchList, params.get("ChangeType"), sa);
        }

        final String remember = params.get("RememberChanged");
        final String forget = params.get("ForgetChanged");
        final String imprint = params.get("Imprint");
        final String selectPrompt = params.containsKey("SelectPrompt") ? params.get("SelectPrompt") : "Select a card";

        if (params.containsKey("Unimprint")) {
            card.clearImprinted();
        }

        for (int i = 0; i < changeNum; i++) {
            if (params.containsKey("DifferentNames")) {
                for (Card c : movedCards) {
                    fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.nameEquals(c.getName())));
                }
            }
            if ((fetchList.size() == 0) || (destination == null)) {
                break;
            }

            Object o;
            if (params.containsKey("AtRandom")) {
                o = CardUtil.getRandom(fetchList);
            } else if (params.containsKey("Mandatory")) {
                o = GuiChoose.one(selectPrompt, fetchList);
            } else if (params.containsKey("Defined")) {
                o = fetchList.get(0);
            } else {
                o = GuiChoose.oneOrNone(selectPrompt, fetchList);
            }

            if (o != null) {
                final Card c = (Card) o;
                fetchList.remove(c);
                Card movedCard = null;

                if (destination.equals(ZoneType.Library)) {
                    // do not shuffle the library once we have placed a fetched
                    // card on top.
                    if (origin.contains(ZoneType.Library) && (i < 1) && "False".equals(params.get("Shuffle"))) {
                        player.shuffle();
                    }
                    movedCard = Singletons.getModel().getGame().getAction().moveToLibrary(c, libraryPos);
                } else if (destination.equals(ZoneType.Battlefield)) {
                    if (params.containsKey("Tapped")) {
                        c.setTapped(true);
                    }
                    if (params.containsKey("GainControl")) {
                        c.addController(sa.getSourceCard());
                    }

                    if (params.containsKey("AttachedTo")) {
                        final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                                params.get("AttachedTo"), sa);
                        if (!list.isEmpty()) {
                            final Card attachedTo = list.get(0);
                            if (c.isEnchanting()) {
                                // If this Card is already Enchanting something
                                // Need to unenchant it, then clear out the
                                // commands
                                final GameEntity oldEnchanted = c.getEnchanting();
                                oldEnchanted.removeEnchantedBy(c);
                                c.removeEnchanting(oldEnchanted);
                                c.clearEnchantCommand();
                                c.clearUnEnchantCommand();
                            }
                            c.enchantEntity(attachedTo);
                        }
                    }

                    if (params.containsKey("Attacking")) {
                        Singletons.getModel().getGame().getCombat().addAttacker(c);
                    }

                    movedCard = Singletons.getModel().getGame().getAction().moveTo(c.getController().getZone(destination), c);
                    if (params.containsKey("Tapped")) {
                        movedCard.setTapped(true);
                    }
                } else if (destination.equals(ZoneType.Exile)) {
                    movedCard = Singletons.getModel().getGame().getAction().exile(c);
                    if (params.containsKey("ExileFaceDown")) {
                        movedCard.setState(CardCharacteristicName.FaceDown);
                    }
                } else {
                    movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, c);
                }
                movedCards.add(movedCard);

                if (remember != null) {
                    card.addRemembered(movedCard);
                }
                if (forget != null) {
                    sa.getSourceCard().getRemembered().remove(movedCard);
                }
                // for imprinted since this doesn't use Target
                if (imprint != null) {
                    card.addImprinted(movedCard);
                }

            } else {
                final StringBuilder sb = new StringBuilder();
                final int num = Math.min(fetchList.size(), changeNum - i);
                sb.append("Cancel Search? Up to ").append(num).append(" more cards can change zones.");

                if (((i + 1) == changeNum) || GameActionUtil.showYesNoDialog(card, sb.toString())) {
                    break;
                }
            }
        }
        if (params.containsKey("Reveal") && !movedCards.isEmpty()) {
            GuiChoose.one(card + " - Revealed card: ", movedCards.toArray());
        }

        if ((origin.contains(ZoneType.Library) && !destination.equals(ZoneType.Library) && !defined)
                || (params.containsKey("Shuffle") && "True".equals(params.get("Shuffle")))) {
            player.shuffle();
        }
    }

    /**
     * <p>
     * changeHiddenOriginResolveAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    private static void changeHiddenOriginResolveAI(final Player ai, final AbilityFactory af, final SpellAbility sa, Player player) {
        final HashMap<String, String> params = af.getMapParams();
        final Target tgt = sa.getTarget();
        final Card card = sa.getSourceCard();
        final boolean defined = params.containsKey("Defined");

        if (tgt != null) {
            if (!tgt.getTargetPlayers().isEmpty()) {
                player = tgt.getTargetPlayers().get(0);
                if (!player.canBeTargetedBy(sa)) {
                    return;
                }
            }
        }

        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (params.containsKey("Origin")) {
            origin = ZoneType.listValueOf(params.get("Origin"));
        }

        String type = params.get("ChangeType");
        if (type == null) {
            type = "Card";
        }

        int changeNum = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(card, params.get("ChangeNum"),
                sa) : 1;

        List<Card> fetchList;
        if (defined) {
            fetchList = new ArrayList<Card>(AbilityFactory.getDefinedCards(card, params.get("Defined"), sa));
            if (!params.containsKey("ChangeNum")) {
                changeNum = fetchList.size();
            }
        } else if (!origin.contains(ZoneType.Library) && !origin.contains(ZoneType.Hand)
                && !params.containsKey("DefinedPlayer")) {
            fetchList = Singletons.getModel().getGame().getCardsIn(origin);
            fetchList = AbilityFactory.filterListByType(fetchList, type, sa);
        } else {
            fetchList = player.getCardsIn(origin);
            fetchList = AbilityFactory.filterListByType(fetchList, type, sa);
        }

        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final List<Card> fetched = new ArrayList<Card>();
        final String remember = params.get("RememberChanged");
        final String forget = params.get("ForgetChanged");
        final String imprint = params.get("Imprint");

        if (params.containsKey("Unimprint")) {
            card.clearImprinted();
        }

        for (int i = 0; i < changeNum; i++) {
            if (params.containsKey("DifferentNames")) {
                for (Card c : fetched) {
                    fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.nameEquals(c.getName())));
                }
            }
            if ((fetchList.size() == 0) || (destination == null)) {
                break;
            }

            // Improve the AI for fetching.
            Card c = null;
            if (params.containsKey("AtRandom")) {
                c = CardUtil.getRandom(fetchList);
            } else if (defined) {
                c = fetchList.get(0);
            } else {
                CardLists.shuffle(fetchList);
                // Save a card as a default, in case we can't find anything suitable.
                Card first = fetchList.get(0);
                if (ZoneType.Battlefield.equals(destination)) {
                    fetchList = CardLists.filter(fetchList, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            if (c.isType("Legendary")) {
                                if (!ai.getCardsIn(ZoneType.Battlefield, c.getName()).isEmpty()) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    });
                    if (player.isHuman() && params.containsKey("GainControl")) {
                        fetchList = CardLists.filter(fetchList, new Predicate<Card>() {
                            @Override
                            public boolean apply(final Card c) {
                                if (!c.getSVar("RemAIDeck").equals("") || !c.getSVar("RemRandomDeck").equals("")) {
                                    return false;
                                }
                                return true;
                            }
                        });
                    }
                }
                if (ZoneType.Exile.equals(destination) || origin.contains(ZoneType.Battlefield)) {
                    // Exiling or bouncing stuff
                    if (player.isHuman()) {
                        c = CardFactoryUtil.getBestAI(fetchList);
                    } else {
                        c = CardFactoryUtil.getWorstAI(fetchList);
                    }
                } else if (origin.contains(ZoneType.Library)
                        && (type.contains("Basic") || AbilityFactoryChangeZone.areAllBasics(type))) {
                    c = AbilityFactoryChangeZone.basicManaFixing(ai, fetchList);
                } else if (ZoneType.Hand.equals(destination) && CardLists.getNotType(fetchList, "Creature").size() == 0) {
                    c = AbilityFactoryChangeZone.chooseCreature(ai, fetchList);
                } else if (ZoneType.Battlefield.equals(destination) || ZoneType.Graveyard.equals(destination)) {
                    c = CardFactoryUtil.getBestAI(fetchList);
                } else {
                    // Don't fetch another tutor with the same name
                    List<Card> sameNamed = CardLists.filter(fetchList, Predicates.not(CardPredicates.nameEquals(card.getName())));
                    if (origin.contains(ZoneType.Library) && !sameNamed.isEmpty()) {
                        fetchList = sameNamed;
                    }

                    // Does AI need a land?
                    List<Card> hand = ai.getCardsIn(ZoneType.Hand);
                    if (CardLists.filter(hand, Presets.LANDS).size() == 0 && CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Presets.LANDS).size() < 4) {
                        boolean canCastSomething = false;
                        for (Card cardInHand : hand) {
                            canCastSomething |= ComputerUtil.payManaCost(cardInHand.getFirstSpellAbility(), ai, true, 0, false);
                        }
                        if (!canCastSomething) {
                            System.out.println("Pulling a land as there are none in hand, less than 4 on the board, and nothing in hand is castable.");
                            c = basicManaFixing(ai, fetchList);
                        }
                    }
                    if (c == null) {
                        System.out.println("Don't need a land or none available; trying for a creature.");
                        fetchList = CardLists.getNotType(fetchList, "Land");
                        // Prefer to pull a creature, generally more useful for AI.
                        c = chooseCreature(ai, CardLists.filter(fetchList, CardPredicates.Presets.CREATURES));
                    }
                    if (c == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            System.out.println("5 Life or less, trying to find something castable.");
                            CardLists.sortByMostExpensive(fetchList);
                            for (Card potentialCard : fetchList) {
                               if (ComputerUtil.payManaCost(potentialCard.getFirstSpellAbility(), ai, true, 0, false)) {
                                   c = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            System.out.println("No creature and lots of life, finding something good.");
                            c = CardFactoryUtil.getBestAI(fetchList);
                        }
                    }
                }
                if (c == null) {
                    c = first;
                }
            }

            fetched.add(c);
            fetchList.remove(c);
        }

        if (origin.contains(ZoneType.Library) && !defined && !"False".equals(params.get("Shuffle"))) {
            player.shuffle();
        }

        for (final Card c : fetched) {
            Card movedCard = null;
            if (ZoneType.Library.equals(destination)) {
                final int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                        .get("LibraryPosition")) : 0;
                movedCard = Singletons.getModel().getGame().getAction().moveToLibrary(c, libraryPos);
            } else if (ZoneType.Battlefield.equals(destination)) {
                if (params.containsKey("Tapped")) {
                    c.setTapped(true);
                }
                if (params.containsKey("GainControl")) {
                    c.addController(sa.getSourceCard());
                }

                if (params.containsKey("AttachedTo")) {
                    final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                            params.get("AttachedTo"), sa);
                    if (!list.isEmpty()) {
                        final Card attachedTo = list.get(0);
                        if (c.isEnchanting()) {
                            // If this Card is already Enchanting something
                            // Need to unenchant it, then clear out the commands
                            final GameEntity oldEnchanted = c.getEnchanting();
                            c.removeEnchanting(oldEnchanted);
                            c.clearEnchantCommand();
                            c.clearUnEnchantCommand();
                        }
                        c.enchantEntity(attachedTo);
                    }
                }

                if (params.containsKey("Attacking")) {
                    Singletons.getModel().getGame().getCombat().addAttacker(c);
                }
                // Auras without Candidates stay in their current location
                if (c.isAura()) {
                    final SpellAbility saAura = AbilityFactoryAttach.getAttachSpellAbility(c);
                    if (!saAura.getTarget().hasCandidates(saAura, false)) {
                        continue;
                    }
                }

                movedCard = Singletons.getModel().getGame().getAction().moveTo(c.getController().getZone(destination), c);
                if (params.containsKey("Tapped")) {
                    movedCard.setTapped(true);
                }
            } else if (destination.equals(ZoneType.Exile)) {
                movedCard = Singletons.getModel().getGame().getAction().exile(c);
                if (params.containsKey("ExileFaceDown")) {
                    movedCard.setState(CardCharacteristicName.FaceDown);
                }
            } else {
                movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, c);
            }

            if (remember != null) {
                card.addRemembered(movedCard);
            }
            if (forget != null) {
                sa.getSourceCard().getRemembered().remove(movedCard);
            }
            // for imprinted since this doesn't use Target
            if (imprint != null) {
                card.addImprinted(movedCard);
            }
        }
        
        if ((origin.contains(ZoneType.Library) && !destination.equals(ZoneType.Library) && !defined)
                || (params.containsKey("Shuffle") && "True".equals(params.get("Shuffle")))) {
            player.shuffle();
        }

        if (!ZoneType.Battlefield.equals(destination) && !"Card".equals(type) && !defined) {
            final String picked = sa.getSourceCard().getName() + " - Computer picked:";
            if (fetched.size() > 0) {
                GuiChoose.one(picked, fetched);
            } else {
                GuiChoose.one(picked, new String[] { "<Nothing>" });
            }
        }
    } // end changeHiddenOriginResolveAI

    // *********** Utility functions for Hidden ********************
    /**
     * <p>
     * basicManaFixing.
     * </p>
     * @param ai 
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    private static Card basicManaFixing(final Player ai, final List<Card> list) { // Search for a
                                                               // Basic Land

        final List<Card> combined = new ArrayList<Card>(ai.getCardsIn(ZoneType.Battlefield));
        combined.addAll(ai.getCardsIn(ZoneType.Hand));

        final ArrayList<String> basics = new ArrayList<String>();

        // what types can I go get?
        for (final String name : Constant.Color.BASIC_LANDS) {
            if (!CardLists.getType(list, name).isEmpty()) {
                basics.add(name);
            }
        }

        // Which basic land is least available from hand and play, that I still
        // have in my deck
        int minSize = Integer.MAX_VALUE;
        String minType = null;

        for (int i = 0; i < basics.size(); i++) {
            final String b = basics.get(i);
            final int num = CardLists.getType(combined, b).size();
            if (num < minSize) {
                minType = b;
                minSize = num;
            }
        }

        List<Card> result = list;
        if (minType != null) {
            result = CardLists.getType(list, minType);
        }

        return result.get(0);
    }

    /**
     * <p>
     * areAllBasics.
     * </p>
     * 
     * @param types
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    private static boolean areAllBasics(final String types) {
        final String[] split = types.split(",");
        final String[] names = { "Plains", "Island", "Swamp", "Mountain", "Forest" };
        final boolean[] bBasic = new boolean[split.length];

        for (final String s : names) {
            for (int i = 0; i < split.length; i++) {
                bBasic[i] |= s.equals(split[i]);
            }
        }

        for (int i = 0; i < split.length; i++) {
            if (!bBasic[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Some logic for picking a creature card from a list.
     * @param list
     * @return Card
     */
    private static Card chooseCreature(final Player ai, List<Card> list) {
        Card card = null;
        Combat combat = new Combat();
        combat.initiatePossibleDefenders(ai);
        List<Card> attackers = ai.getOpponent().getCreaturesInPlay();
        for (Card att : attackers) {
            combat.addAttacker(att);
        }
        combat = ComputerUtilBlock.getBlockers(ai, combat, ai.getCreaturesInPlay());

        if (CombatUtil.lifeInDanger(ai, combat)) {
            // need something AI can cast now
            CardLists.sortByEvaluateCreature(list);
            for (Card c : list) {
               if (ComputerUtil.payManaCost(c.getFirstSpellAbility(), ai, true, 0, false)) {
                   card = c;
                   break;
               }
            }
        } else {
            // not urgent, get the largest creature possible
            card = CardFactoryUtil.getBestCreatureAI(list);
        }
        return card;
    }

    // *************************************************************************************
    // **************** Known Origin (Battlefield/Graveyard/Exile)
    // *************************
    // ******* Known origin cards are chosen during casting of the spell
    // (target) **********
    // *************************************************************************************

    /**
     * <p>
     * changeKnownOriginCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeKnownOriginCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        // Retrieve either this card, or target Cards in Graveyard
        final Cost abCost = af.getAbCost();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();

        final ZoneType origin = ZoneType.smartValueOf(params.get("Origin"));
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));

        final Random r = MyRandom.getRandom();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getRestrictions().getNumberTurnActivations());

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            if (!AbilityFactoryChangeZone.changeKnownPreferredTarget(ai, af, sa, false)) {
                return false;
            }
        } else {
            // non-targeted retrieval
            final List<Card> retrieval = AbilityFactoryChangeZone
                    .knownDetermineDefined(sa, params.get("Defined"), origin);

            if ((retrieval == null) || retrieval.isEmpty()) {
                return false;
            }

            // if (origin.equals("Graveyard")) {
            // return this card from graveyard: cards like Hammer of Bogardan
            // in general this is cool, but we should add some type of
            // restrictions

            // return this card from battlefield: cards like Blinking Spirit
            // in general this should only be used to protect from Imminent Harm
            // (dying or losing control of)
            if (origin.equals(ZoneType.Battlefield)) {
                if (Singletons.getModel().getGame().getStack().size() == 0) {
                    return false;
                }

                final AbilitySub abSub = sa.getSubAbility();
                String subAPI = "";
                if (abSub != null) {
                    subAPI = abSub.getAbilityFactory().getAPI();
                }

                // only use blink or bounce effects
                if (!(destination.equals(ZoneType.Exile) && (subAPI.equals("DelayedTrigger") || subAPI.equals("ChangeZone")))
                        && !destination.equals(ZoneType.Hand)) {
                    return false;
                }

                final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(ai, af);
                boolean contains = false;
                for (final Card c : retrieval) {
                    if (objects.contains(c)) {
                        contains = true;
                    }
                }
                if (!contains) {
                    return false;
                }
            }
        }
        // don't return something to your hand if your hand is full of good stuff
        if (destination.equals(ZoneType.Hand) && origin.equals(ZoneType.Graveyard)) {
            final int handSize = ai.getCardsIn(ZoneType.Hand).size();
            if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN1)) {
                return false;
            }
            if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && handSize > 1) {
                return false;
            }
            if (Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)
                    && handSize >= ai.getMaxHandSize()) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return (chance);
    }

    /**
     * <p>
     * changeKnownOriginPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeKnownOriginPlayDrawbackAI(final Player aiPlayer, final AbilityFactory af, final SpellAbility sa) {
        if (sa.getTarget() == null) {
            return true;
        }

        return AbilityFactoryChangeZone.changeKnownPreferredTarget(aiPlayer, af, sa, false);
    }

    /**
     * <p>
     * changeKnownPreferredTarget.
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
    private static boolean changeKnownPreferredTarget(final Player ai, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        final ZoneType origin = ZoneType.listValueOf(params.get("Origin")).get(0);
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final Target tgt = sa.getTarget();

        final AbilitySub abSub = sa.getSubAbility();
        String subAPI = "";
        String subAffected = "";
        HashMap<String, String> subParams = null;
        if (abSub != null) {
            subAPI = abSub.getAbilityFactory().getAPI();
            subParams = abSub.getAbilityFactory().getMapParams();
            if (subParams.containsKey("Defined")) {
                subAffected = subParams.get("Defined");
            }
        }

        tgt.resetTargets();
        List<Card> list = CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(origin), tgt.getValidTgts(), ai, source);
        if (params.containsKey("AITgts")) {
            list = CardLists.getValidCards(list, params.get("AITgts"), sa.getActivatingPlayer(), source);
        }
        if (source.isInZone(ZoneType.Hand)) {
            list = CardLists.filter(list, Predicates.not(CardPredicates.nameEquals(source.getName()))); // Don't get the same card back.
        }

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            return false;
        }

        // Narrow down the list:
        if (origin.equals(ZoneType.Battlefield)) {
            // filter out untargetables
            list = CardLists.getTargetableCards(list, sa);
            List<Card> aiPermanents = CardLists.filterControlledBy(list, ai);

            // Don't blink cards that will die.
            aiPermanents = CardLists.filter(aiPermanents, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.getSVar("Targeting").equals("Dies");
                }
            });

            // if it's blink or bounce, try to save my about to die stuff
            if ((destination.equals(ZoneType.Hand) || (destination.equals(ZoneType.Exile) && (subAPI.equals("DelayedTrigger") || (subAPI
                    .equals("ChangeZone") && subAffected.equals("Remembered")))))
                    && (tgt.getMinTargets(sa.getSourceCard(), sa) <= 1)) {

                // check stack for something on the stack that will kill
                // anything i control
                if (Singletons.getModel().getGame().getStack().size() > 0) {
                    final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(ai, af);

                    final List<Card> threatenedTargets = new ArrayList<Card>();

                    for (final Card c : aiPermanents) {
                        if (objects.contains(c)) {
                            threatenedTargets.add(c);
                        }
                    }

                    if (!threatenedTargets.isEmpty()) {
                        // Choose "best" of the remaining to save
                        tgt.addTarget(CardFactoryUtil.getBestCreatureAI(threatenedTargets));
                        return true;
                    }
                }
                // Save combatants
                else if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    final List<Card> combatants = CardLists.filter(aiPermanents, CardPredicates.Presets.CREATURES);
                    CardLists.sortByEvaluateCreature(combatants);

                    for (final Card c : combatants) {
                        if (c.getShield() == 0 && CombatUtil.combatantWouldBeDestroyed(c) && !c.getOwner().isHuman() && !c.isToken()) {
                            tgt.addTarget(c);
                            return true;
                        }
                    }
                }
                // Blink permanents with ETB triggers
                else if (sa.isAbility() && (sa.getPayCosts() != null) && AbilityFactory.playReusable(ai, sa)) {
                    aiPermanents = CardLists.filter(aiPermanents, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            if (c.getNumberOfCounters() > 0) {
                                return false; // don't blink something with
                            }
                            // counters TODO check good and
                            // bad counters
                            // checks only if there is a dangerous ETB effect
                            return SpellPermanent.checkETBEffects(c, ai, null);
                        }
                    });
                    if (!aiPermanents.isEmpty()) {
                        // Choose "best" of the remaining to save
                        tgt.addTarget(CardFactoryUtil.getBestAI(aiPermanents));
                        return true;
                    }
                }
            }

        } else if (origin.equals(ZoneType.Graveyard)) {
            if (destination.equals(ZoneType.Hand)) {
                // only retrieve cards from computer graveyard
                list = CardLists.filterControlledBy(list, ai);
                System.out.println("changeZone:" + list);
            }

        }

        // blink human targets only during combat
        if (origin.equals(ZoneType.Battlefield)
                && destination.equals(ZoneType.Exile)
                && (subAPI.equals("DelayedTrigger") || (subAPI.equals("ChangeZone") && subAffected.equals("Remembered")))
                && !(Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY) || sa
                        .isAbility())) {
            return false;
        }

        // Exile and bounce opponents stuff
        if (destination.equals(ZoneType.Exile) || origin.equals(ZoneType.Battlefield)) {

            // don't rush bouncing stuff when not going to attack
            if (!sa.isTrigger() && sa.getPayCosts() != null
                    && Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)
                    && ai.getCreaturesInPlay().isEmpty()) {
                return false;
            }
            list = CardLists.filterControlledBy(list, ai.getOpponent());
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (Card aura : c.getEnchantedBy()) {
                        if (c.getOwner().isHuman() && aura.getController().equals(ai)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        }

        // Only care about combatants during combat
        if (Singletons.getModel().getGame().getPhaseHandler().inCombat()) {
            CardLists.getValidCards(list, "Card.attacking,Card.blocking", null, null);
        }

        if (list.isEmpty()) {
            return false;
        }

        if (!mandatory && (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
            return false;
        }

        // target loop
        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                final Card mostExpensive = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, false);
                if (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield)) {
                    if (mostExpensive.isCreature()) {
                        // if a creature is most expensive take the best one
                        if (destination.equals(ZoneType.Exile)) {
                            // If Exiling things, don't give bonus to Tokens
                            choice = CardFactoryUtil.getBestCreatureAI(list);
                        } else {
                            choice = CardFactoryUtil.getBestCreatureToBounceAI(list);
                        }
                    } else {
                        choice = mostExpensive;
                    }
                } else if (destination.equals(ZoneType.Hand) || destination.equals(ZoneType.Library)) {
                    List<Card> nonLands = CardLists.getNotType(list, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    choice = chooseCreature(ai, CardLists.filter(nonLands, CardPredicates.Presets.CREATURES));
                    if (choice == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            System.out.println("5 Life or less, trying to find something castable.");
                            CardLists.sortByMostExpensive(nonLands);
                            for (Card potentialCard : nonLands) {
                               if (ComputerUtil.payManaCost(potentialCard.getFirstSpellAbility(), ai, true, 0, false)) {
                                   choice = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            System.out.println("No creature and lots of life, finding something good.");
                            choice = CardFactoryUtil.getBestAI(nonLands);
                        }
                    }
                    if (choice == null) {
                        // No creatures or spells?
                        CardLists.shuffle(list);
                        choice = list.get(0);
                    }
                } else {
                    choice = CardFactoryUtil.getBestAI(list);
                }
            }
            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() == 0) || (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            list.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * changeKnownUnpreferredTarget.
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
    private static boolean changeKnownUnpreferredTarget(final Player ai, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        if (!mandatory) {
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        final ZoneType origin = ZoneType.smartValueOf(params.get("Origin"));
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final Target tgt = sa.getTarget();

        List<Card> list = CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(origin), tgt.getValidTgts(), ai, source);

        // Narrow down the list:
        if (origin.equals(ZoneType.Battlefield)) {
            // filter out untargetables
            list = CardLists.getTargetableCards(list, sa);

            // if Destination is hand, either bounce opponents dangerous stuff
            // or save my about to die stuff

            // if Destination is exile, filter out my cards
        } else if (origin.equals(ZoneType.Graveyard)) {
            // Retrieve from Graveyard to:

        }

        for (final Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        if (list.isEmpty()) {
            return false;
        }

        // target loop
        while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                if (CardFactoryUtil.getMostExpensivePermanentAI(list, sa, false).isCreature()
                        && (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield))) {
                    // if a creature is most expensive take the best
                    choice = CardFactoryUtil.getBestCreatureToBounceAI(list);
                } else if (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield)) {
                    choice = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, false);
                } else if (destination.equals(ZoneType.Hand) || destination.equals(ZoneType.Library)) {
                    List<Card> nonLands = CardLists.getNotType(list, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    choice = chooseCreature(ai, CardLists.filter(nonLands, CardPredicates.Presets.CREATURES));
                    if (choice == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            System.out.println("5 Life or less, trying to find something castable.");
                            CardLists.sortByMostExpensive(nonLands);
                            for (Card potentialCard : nonLands) {
                               if (ComputerUtil.payManaCost(potentialCard.getFirstSpellAbility(), ai, true, 0, false)) {
                                   choice = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            System.out.println("No creature and lots of life, finding something good.");
                            choice = CardFactoryUtil.getBestAI(nonLands);
                        }
                    }
                    if (choice == null) {
                        // No creatures or spells?
                        CardLists.shuffle(list);
                        choice = list.get(0);
                    }
                } else {
                    choice = CardFactoryUtil.getBestAI(list);
                }
            }
            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() == 0) || (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
                    tgt.resetTargets();
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            list.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * changeKnownOriginTriggerAI.
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
    private static boolean changeKnownOriginTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final HashMap<String, String> params = af.getMapParams();
        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }

        if (sa.getTarget() == null) {
            // Just in case of Defined cases
            if (!mandatory && params.containsKey("AttachedTo")) {
                final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                        params.get("AttachedTo"), sa);
                if (!list.isEmpty()) {
                    final Card attachedTo = list.get(0);
                    // This code is for the Dragon auras
                    if (attachedTo.getController().isHuman()) {
                        return false;
                    }
                }
            }
        } else if (AbilityFactoryChangeZone.changeKnownPreferredTarget(ai, af, sa, mandatory)) {
            // do nothing
        } else if (!AbilityFactoryChangeZone.changeKnownUnpreferredTarget(ai, af, sa, mandatory)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            return subAb.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * changeKnownOriginStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeKnownOriginStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final ZoneType origin = ZoneType.smartValueOf(params.get("Origin"));

        final StringBuilder sbTargets = new StringBuilder();

        ArrayList<Card> tgts;
        if (sa.getTarget() != null) {
            tgts = sa.getTarget().getTargetCards();
        } else {
            // otherwise add self to list and go from there
            tgts = new ArrayList<Card>();
            for (final Card c : AbilityFactoryChangeZone.knownDetermineDefined(sa, params.get("Defined"), origin)) {
                tgts.add(c);
            }
        }

        for (final Card c : tgts) {
            sbTargets.append(" ").append(c);
        }

        final String targetname = sbTargets.toString();

        final String pronoun = tgts.size() > 1 ? " their " : " its ";

        final String fromGraveyard = " from the graveyard";

        if (destination.equals(ZoneType.Battlefield)) {
            sb.append("Put").append(targetname);
            if (origin.equals(ZoneType.Graveyard)) {
                sb.append(fromGraveyard);
            }

            sb.append(" onto the battlefield");
            if (params.containsKey("Tapped")) {
                sb.append(" tapped");
            }
            if (params.containsKey("GainControl")) {
                sb.append(" under your control");
            }
            sb.append(".");
        }

        if (destination.equals(ZoneType.Hand)) {
            sb.append("Return").append(targetname);
            if (origin.equals(ZoneType.Graveyard)) {
                sb.append(fromGraveyard);
            }
            sb.append(" to").append(pronoun).append("owners hand.");
        }

        if (destination.equals(ZoneType.Library)) {
            if (params.containsKey("Shuffle")) { // for things like Gaea's
                                                 // Blessing
                sb.append("Shuffle").append(targetname);

                sb.append(" into").append(pronoun).append("owner's library.");
            } else {
                sb.append("Put").append(targetname);
                if (origin.equals(ZoneType.Graveyard)) {
                    sb.append(fromGraveyard);
                }

                // this needs to be zero indexed. Top = 0, Third = 2, -1 =
                // Bottom
                final int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                        .get("LibraryPosition")) : 0;

                if (libraryPosition == -1) {
                    sb.append(" on the bottom of").append(pronoun).append("owner's library.");
                } else if (libraryPosition == 0) {
                    sb.append(" on top of").append(pronoun).append("owner's library.");
                } else {
                    sb.append(" ").append(libraryPosition + 1).append(" from the top of");
                    sb.append(pronoun).append("owner's library.");
                }
            }
        }

        if (destination.equals(ZoneType.Exile)) {
            sb.append("Exile").append(targetname);
            if (origin.equals(ZoneType.Graveyard)) {
                sb.append(fromGraveyard);
            }
            sb.append(".");
        }

        if (destination.equals(ZoneType.Ante)) {
            sb.append("Ante").append(targetname);
            sb.append(".");
        }

        if (destination.equals(ZoneType.Graveyard)) {
            sb.append("Put").append(targetname);
            sb.append(" from ").append(origin);
            sb.append(" into").append(pronoun).append("owner's graveyard.");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeKnownOriginResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeKnownOriginResolve(final AbilityFactory af, final SpellAbility sa) {
        ArrayList<Card> tgtCards;
        ArrayList<SpellAbility> sas;

        final HashMap<String, String> params = af.getMapParams();
        final Target tgt = sa.getTarget();
        final Player player = sa.getActivatingPlayer();
        final Card hostCard = sa.getSourceCard();

        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final List<ZoneType> origin = ZoneType.listValueOf(params.get("Origin"));

        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = new ArrayList<Card>();
            for(ZoneType o : origin) {
                for (final Card c : AbilityFactoryChangeZone.knownDetermineDefined(sa, params.get("Defined"), o)) {
                    tgtCards.add(c);
                }
            }
        }

        // changing zones for spells on the stack
        if (tgt != null) {
            sas = tgt.getTargetSAs();
        } else {
            sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final SpellAbility tgtSA : sas) {
            if (!tgtSA.isSpell()) { // Catch any abilities or triggers that slip through somehow
                continue;
            }

            final SpellAbilityStackInstance si = Singletons.getModel().getGame().getStack().getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                continue;
            }

            removeFromStack(tgtSA, sa, si);
        } // End of change from stack

        final String remember = params.get("RememberChanged");
        final String imprint = params.get("Imprint");

        if (params.containsKey("Unimprint")) {
            hostCard.clearImprinted();
        }

        if (params.containsKey("ForgetOtherRemembered")) {
            hostCard.clearRemembered();
        }

        boolean optional = params.containsKey("Optional");

        if (tgtCards.size() != 0) {
            for (final Card tgtC : tgtCards) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Do you want to move " + tgtC + " from " + origin + " to " + destination + "?");
                if (player.isHuman() && optional
                        && !GameActionUtil.showYesNoDialog(hostCard, sb.toString())) {
                    continue;
                }
                final Zone originZone = Singletons.getModel().getGame().getZoneOf(tgtC);

                // if Target isn't in the expected Zone, continue

                if (originZone == null || !origin.contains(originZone.getZoneType())) {
                    continue;
                }
                
                Card movedCard = null;

                if (destination.equals(ZoneType.Library)) {
                    // library position is zero indexed
                    final int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                            .get("LibraryPosition")) : 0;

                    movedCard = Singletons.getModel().getGame().getAction().moveToLibrary(tgtC, libraryPosition);

                    // for things like Gaea's Blessing
                    if (params.containsKey("Shuffle")) {
                        tgtC.getOwner().shuffle();
                    }
                } else {
                    if (destination.equals(ZoneType.Battlefield)) {
                        if (params.containsKey("Tapped") || params.containsKey("Ninjutsu")) {
                            tgtC.setTapped(true);
                        }
                        if (params.containsKey("GainControl")) {
                            tgtC.addController(sa.getSourceCard());
                        }
                        if (params.containsKey("AttachedTo")) {
                            final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                                    params.get("AttachedTo"), sa);
                            if (!list.isEmpty()) {
                                final Card attachedTo = list.get(0);
                                if (tgtC.isEnchanting()) {
                                    // If this Card is already Enchanting
                                    // something
                                    // Need to unenchant it, then clear out the
                                    // commands
                                    final GameEntity oldEnchanted = tgtC.getEnchanting();
                                    tgtC.removeEnchanting(oldEnchanted);
                                    tgtC.clearEnchantCommand();
                                    tgtC.clearUnEnchantCommand();
                                }
                                tgtC.enchantEntity(attachedTo);
                            }
                        }
                        // Auras without Candidates stay in their current
                        // location
                        if (tgtC.isAura()) {
                            final SpellAbility saAura = AbilityFactoryAttach.getAttachSpellAbility(tgtC);
                            if (!saAura.getTarget().hasCandidates(saAura, false)) {
                                continue;
                            }
                        }

                        movedCard = Singletons.getModel().getGame().getAction()
                                .moveTo(tgtC.getController().getZone(destination), tgtC);

                        if (params.containsKey("Ninjutsu") || params.containsKey("Attacking")) {
                            Singletons.getModel().getGame().getCombat().addAttacker(tgtC);
                            Singletons.getModel().getGame().getCombat().addUnblockedAttacker(tgtC);
                        }
                        if (params.containsKey("Tapped") || params.containsKey("Ninjutsu")) {
                            tgtC.setTapped(true);
                        }
                    } else {
                        movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, tgtC);
                        // If a card is Exiled from the stack, remove its spells from the stack
                        if (params.containsKey("Fizzle")) {
                            ArrayList<SpellAbility> spells = tgtC.getSpellAbilities();
                            for (SpellAbility spell : spells) {
                                if (tgtC.isInZone(ZoneType.Exile)) {
                                    final SpellAbilityStackInstance si = Singletons.getModel().getGame().getStack().getInstanceFromSpellAbility(spell);
                                    Singletons.getModel().getGame().getStack().remove(si);
                                }
                            }
                        }
                        if (params.containsKey("ExileFaceDown")) {
                            movedCard.setState(CardCharacteristicName.FaceDown);
                        }
                    }
                }
                if (remember != null) {
                    hostCard.addRemembered(movedCard);
                }
                if (imprint != null) {
                    hostCard.addImprinted(movedCard);
                }
            }
        }
    }

    // **************************** Known Utility
    // **************************************
    /**
     * <p>
     * knownDetermineDefined.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param defined
     *            a {@link java.lang.String} object.
     * @param origin
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> knownDetermineDefined(final SpellAbility sa, final String defined, final ZoneType origin) {
        final List<Card> ret = new ArrayList<Card>();
        final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(), defined, sa);

        for (final Card c : list) {
            final Card actualCard = Singletons.getModel().getGame().getCardState(c);
            ret.add(actualCard);
        }
        return ret;
    }

    /**
     * <p>
     * removeFromStack.
     * </p>
     *
     * @param tgtSA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param srcSA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param si
     *            a {@link forge.card.spellability.SpellAbilityStackInstance}
     *            object.
     */
    private static void removeFromStack(final SpellAbility tgtSA, final SpellAbility srcSA, final SpellAbilityStackInstance si) {
        Singletons.getModel().getGame().getStack().remove(si);

        final AbilityFactory af = srcSA.getAbilityFactory();
        final HashMap<String, String> params = af.getMapParams();

        if (params.containsKey("Destination")) {
            if (tgtSA.isAbility()) {
                // Shouldn't be able to target Abilities but leaving this in for now
            } else if (tgtSA.isFlashBackAbility())  {
                Singletons.getModel().getGame().getAction().exile(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("Graveyard")) {
                Singletons.getModel().getGame().getAction().moveToGraveyard(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("Exile")) {
                Singletons.getModel().getGame().getAction().exile(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("TopOfLibrary")) {
                Singletons.getModel().getGame().getAction().moveToLibrary(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("Hand")) {
                Singletons.getModel().getGame().getAction().moveToHand(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("BottomOfLibrary")) {
                Singletons.getModel().getGame().getAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("ShuffleIntoLibrary")) {
                Singletons.getModel().getGame().getAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
                tgtSA.getSourceCard().getController().shuffle();
            } else {
                throw new IllegalArgumentException("AbilityFactory_ChangeZone: Invalid Destination argument for card "
                        + srcSA.getSourceCard().getName());
            }

            if (!tgtSA.isAbility()) {
                System.out.println("Moving spell to " + params.get("Destination"));
            }
        }
    }

    // *************************************************************************************
    // ************************** ChangeZoneAll
    // ********************************************
    // ************ All is non-targeted and should occur similarly to Hidden
    // ***************
    // ******* Instead of choosing X of type on resolution, all on type go
    // *****************
    // *************************************************************************************
    /**
     * <p>
     * createAbilityChangeZoneAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityChangeZoneAll(final AbilityFactory af) {
        class AbilityChangeZoneAll extends AbilityActivated {
            public AbilityChangeZoneAll(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityChangeZoneAll(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                AbilityFactoryChangeZone.setMiscellaneous(af, res);
                return res;
            }

            private static final long serialVersionUID = 3728332812890211671L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChangeZone.changeZoneAllCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeZone.changeZoneAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeZone.changeZoneAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChangeZone.changeZoneAllDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abChangeZone = new AbilityChangeZoneAll(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        AbilityFactoryChangeZone.setMiscellaneous(af, abChangeZone);
        return abChangeZone;
    }

    /**
     * <p>
     * createSpellChangeZoneAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellChangeZoneAll(final AbilityFactory af) {
        final SpellAbility spChangeZone = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3270484211099902059L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChangeZone.changeZoneAllCanPlayAI(getActivatingPlayer(), af, this) && super.canPlayAI();
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeZone.changeZoneAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeZone.changeZoneAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryChangeZone.changeZoneAllTriggerAINoCost(getActivatingPlayer(), af, this, mandatory);
                }
                return AbilityFactoryChangeZone.changeZoneAllDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        };
        AbilityFactoryChangeZone.setMiscellaneous(af, spChangeZone);
        return spChangeZone;
    }

    /**
     * <p>
     * createDrawbackChangeZoneAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackChangeZoneAll(final AbilityFactory af) {
        class DrawbackChangeZoneAll extends AbilitySub {
            public DrawbackChangeZoneAll(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackChangeZoneAll(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                AbilityFactoryChangeZone.setMiscellaneous(af, res);
                return res;
            }

            private static final long serialVersionUID = 3270484211099902059L;

            @Override
            public void resolve() {
                AbilityFactoryChangeZone.changeZoneAllResolve(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChangeZone.changeZoneAllCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryChangeZone.changeZoneAllPlayDrawbackAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeZone.changeZoneAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChangeZone.changeZoneAllDoTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbChangeZone = new DrawbackChangeZoneAll(af.getHostCard(), af.getAbTgt());

        AbilityFactoryChangeZone.setMiscellaneous(af, dbChangeZone);
        return dbChangeZone;
    }

    /**
     * <p>
     * changeZoneAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeZoneAllCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        // Change Zone All, can be any type moving from one zone to another
        final Cost abCost = af.getAbCost();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final ZoneType origin = ZoneType.smartValueOf(params.get("Origin"));

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

        }

        final Random r = MyRandom.getRandom();
        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // TODO targeting with ChangeZoneAll
        // really two types of targeting.
        // Target Player has all their types change zones
        // or target permanent and do something relative to that permanent
        // ex. "Return all Auras attached to target"
        // ex. "Return all blocking/blocked by target creature"

        final Player opp = ai.getOpponent();
        final List<Card> humanType = AbilityFactory.filterListByType(opp.getCardsIn(origin), params.get("ChangeType"), sa);
        List<Card> computerType = ai.getCardsIn(origin);
        computerType = AbilityFactory.filterListByType(computerType, params.get("ChangeType"), sa);
        final Target tgt = sa.getTarget();

        // TODO improve restrictions on when the AI would want to use this
        // spBounceAll has some AI we can compare to.
        if (origin.equals(ZoneType.Hand) || origin.equals(ZoneType.Library)) {
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Hand).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                tgt.resetTargets();
                tgt.addTarget(opp);
            }
        } else if (origin.equals(ZoneType.Battlefield)) {
            // this statement is assuming the AI is trying to use this spell
            // offensively
            // if the AI is using it defensively, then something else needs to
            // occur
            // if only creatures are affected evaluate both lists and pass only
            // if human creatures are more valuable
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Hand).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                tgt.resetTargets();
                tgt.addTarget(opp);
                computerType.clear();
            }
            if ((CardLists.getNotType(humanType, "Creature").size() == 0) && (CardLists.getNotType(computerType, "Creature").size() == 0)) {
                if ((CardFactoryUtil.evaluateCreatureList(computerType) + 200) >= CardFactoryUtil
                        .evaluateCreatureList(humanType)) {
                    return false;
                }
            } // otherwise evaluate both lists by CMC and pass only if human
              // permanents are more valuable
            else if ((CardFactoryUtil.evaluatePermanentList(computerType) + 3) >= CardFactoryUtil
                    .evaluatePermanentList(humanType)) {
                return false;
            }

            // Don't cast during main1?
            if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.MAIN1, ai)) {
                return false;
            }
        } else if (origin.equals(ZoneType.Graveyard)) {
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Graveyard).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                tgt.resetTargets();
                tgt.addTarget(opp);
            }
        } else if (origin.equals(ZoneType.Exile)) {

        } else if (origin.equals(ZoneType.Stack)) {
            // time stop can do something like this:
            // Origin$ Stack | Destination$ Exile | SubAbility$ DBSkip
            // DBSKipToPhase | DB$SkipToPhase | Phase$ Cleanup
            // otherwise, this situation doesn't exist
            return false;
        }

        if (destination.equals(ZoneType.Battlefield)) {
            if (params.get("GainControl") != null) {
                // Check if the cards are valuable enough
                if ((CardLists.getNotType(humanType, "Creature").size() == 0) && (CardLists.getNotType(computerType, "Creature").size() == 0)) {
                    if ((CardFactoryUtil.evaluateCreatureList(computerType) + CardFactoryUtil
                            .evaluateCreatureList(humanType)) < 400) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if ((CardFactoryUtil.evaluatePermanentList(computerType) + CardFactoryUtil
                        .evaluatePermanentList(humanType)) < 6) {
                    return false;
                }
            } else {
                // don't activate if human gets more back than AI does
                if ((CardLists.getNotType(humanType, "Creature").size() == 0) && (CardLists.getNotType(computerType, "Creature").size() == 0)) {
                    if (CardFactoryUtil.evaluateCreatureList(computerType) <= (CardFactoryUtil
                            .evaluateCreatureList(humanType) + 100)) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if (CardFactoryUtil.evaluatePermanentList(computerType) <= (CardFactoryUtil
                        .evaluatePermanentList(humanType) + 2)) {
                    return false;
                }
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return (((r.nextFloat() < .8) || sa.isTrigger()) && chance);
    }

    /**
     * <p>
     * changeZoneAllPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeZoneAllPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:

        return true;
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
    public static boolean changeZoneAllDoTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
            // payment it's usually
            // not mandatory
            return false;
        }
        return changeZoneAllTriggerAINoCost(ai, af, sa, mandatory);
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
    public static boolean changeZoneAllTriggerAINoCost(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // Change Zone All, can be any type moving from one zone to another
        final HashMap<String, String> params = af.getMapParams();
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final ZoneType origin = ZoneType.smartValueOf(params.get("Origin"));

        final Player opp = ai.getOpponent();
        final List<Card> humanType = AbilityFactory.filterListByType(opp.getCardsIn(origin), params.get("ChangeType"), sa);
        List<Card> computerType = ai.getCardsIn(origin);
        computerType = AbilityFactory.filterListByType(computerType, params.get("ChangeType"), sa);

        // TODO improve restrictions on when the AI would want to use this
        // spBounceAll has some AI we can compare to.
        if (origin.equals(ZoneType.Hand) || origin.equals(ZoneType.Library)) {
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Hand).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                tgt.resetTargets();
                tgt.addTarget(opp);
            }
        } else if (origin.equals(ZoneType.Battlefield)) {
            // this statement is assuming the AI is trying to use this spell offensively
            // if the AI is using it defensively, then something else needs to occur
            // if only creatures are affected evaluate both lists and pass only
            // if human creatures are more valuable
            if ((CardLists.getNotType(humanType, "Creature").isEmpty()) && (CardLists.getNotType(computerType, "Creature").isEmpty())) {
                if (CardFactoryUtil.evaluateCreatureList(computerType) >= CardFactoryUtil
                        .evaluateCreatureList(humanType)) {
                    return false;
                }
            } // otherwise evaluate both lists by CMC and pass only if human
              // permanents are more valuable
            else if (CardFactoryUtil.evaluatePermanentList(computerType) >= CardFactoryUtil
                    .evaluatePermanentList(humanType)) {
                return false;
            }
        } else if (origin.equals(ZoneType.Graveyard)) {
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Graveyard).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                tgt.resetTargets();
                tgt.addTarget(opp);
            }
        } else if (origin.equals(ZoneType.Exile)) {

        } else if (origin.equals(ZoneType.Stack)) {
            // time stop can do something like this:
            // Origin$ Stack | Destination$ Exile | SubAbility$ DBSkip
            // DBSKipToPhase | DB$SkipToPhase | Phase$ Cleanup
            // otherwise, this situation doesn't exist
            return false;
        }

        if (destination.equals(ZoneType.Battlefield)) {
            if (params.get("GainControl") != null) {
                // Check if the cards are valuable enough
                if ((CardLists.getNotType(humanType, "Creature").size() == 0) && (CardLists.getNotType(computerType, "Creature").size() == 0)) {
                    if ((CardFactoryUtil.evaluateCreatureList(computerType) + CardFactoryUtil
                            .evaluateCreatureList(humanType)) < 1) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if ((CardFactoryUtil.evaluatePermanentList(computerType) + CardFactoryUtil
                        .evaluatePermanentList(humanType)) < 1) {
                    return false;
                }
            } else {
                // don't activate if human gets more back than AI does
                if ((CardLists.getNotType(humanType, "Creature").isEmpty()) && (CardLists.getNotType(computerType, "Creature").isEmpty())) {
                    if (CardFactoryUtil.evaluateCreatureList(computerType) <= CardFactoryUtil
                            .evaluateCreatureList(humanType)) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if (CardFactoryUtil.evaluatePermanentList(computerType) <= CardFactoryUtil
                        .evaluatePermanentList(humanType)) {
                    return false;
                }
            }
        }

        boolean chance = true;
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * changeZoneAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeZoneAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are
        // added
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        final String[] desc = sa.getDescription().split(":");

        if (desc.length > 1) {
            sb.append(desc[1]);
        } else {
            sb.append(desc[0]);
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeZoneAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeZoneAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final List<ZoneType> origin = ZoneType.listValueOf(params.get("Origin"));

        List<Card> cards = null;

        ArrayList<Player> tgtPlayers = null;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            cards = Singletons.getModel().getGame().getCardsIn(origin);
        } else {
            cards = tgtPlayers.get(0).getCardsIn(origin);
        }

        cards = AbilityFactory.filterListByType(cards, params.get("ChangeType"), sa);

        if (params.containsKey("ForgetOtherRemembered")) {
            sa.getSourceCard().clearRemembered();
        }

        final String remember = params.get("RememberChanged");

        // I don't know if library position is necessary. It's here if it is,
        // just in case
        final int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition"))
                : 0;
        for (final Card c : cards) {
            if (destination.equals(ZoneType.Battlefield)) {
                // Auras without Candidates stay in their current location
                if (c.isAura()) {
                    final SpellAbility saAura = AbilityFactoryAttach.getAttachSpellAbility(c);
                    if (!saAura.getTarget().hasCandidates(saAura, false)) {
                        continue;
                    }
                }
                if (params.containsKey("Tapped")) {
                    c.setTapped(true);
                }
            }

            if (params.containsKey("GainControl")) {
                c.addController(sa.getSourceCard());
                Singletons.getModel().getGame().getAction().moveToPlay(c, sa.getActivatingPlayer());
            } else {
                final Card movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, c, libraryPos);
                if (params.containsKey("ExileFaceDown")) {
                    movedCard.setState(CardCharacteristicName.FaceDown);
                }
                if (params.containsKey("Tapped")) {
                    movedCard.setTapped(true);
                }
            }

            if (remember != null) {
                Singletons.getModel().getGame().getCardState(sa.getSourceCard()).addRemembered(c);
            }
        }

        // if Shuffle parameter exists, and any amount of cards were owned by
        // that player, then shuffle that library
        if (params.containsKey("Shuffle")) {
            for( Player p : Singletons.getModel().getGame().getPlayers()) {
                if (Iterables.any(cards, CardPredicates.isOwner(p))) {
                    p.shuffle();
                }
            }
        }
    }

}
