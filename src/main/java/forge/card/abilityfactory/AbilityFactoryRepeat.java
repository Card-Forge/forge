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

import forge.AllZoneUtil;
import forge.Card;
import forge.CardLists;
import forge.card.cost.Cost;

import forge.GameActionUtil;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactory_Repeat class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryRepeat.java 15090 2012-04-07 12:50:31Z Max mtg $
 */
public final class AbilityFactoryRepeat {

    private AbilityFactoryRepeat() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getAbilityRepeat.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilityRepeat(final AbilityFactory af) {
        class AbilityRepeat extends AbilityActivated {
            public AbilityRepeat(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityRepeat(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -8019637116128196482L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryRepeat.repeatCanPlayAI(getActivatingPlayer(), this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryRepeat.repeatCanPlayAI(getActivatingPlayer(), this) || mandatory;
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryRepeat.repeatStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRepeat.repeatResolve(af, this);
            }
        }
        final SpellAbility abRepeat = new AbilityRepeat(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abRepeat;
    }

    /**
     * <p>
     * createSpellRepeat.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellRepeat(final AbilityFactory af) {
        final SpellAbility spRepeat = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4991665176268317217L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryRepeat.repeatCanPlayAI(getActivatingPlayer(), this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryRepeat.repeatCanPlayAI(getActivatingPlayer(), this) || mandatory;
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryRepeat.repeatStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRepeat.repeatResolve(af, this);
            }
        };

        return spRepeat;
    }

    /**
     * <p>
     * createDrawbackRepeat.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackRepeat(final AbilityFactory af) {
        class DrawbackRepeat extends AbilitySub {
            public DrawbackRepeat(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackRepeat(getSourceCard(), new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -3850086157052881036L;

            @Override
            public boolean canPlayAI() {
                return true;
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryRepeat.repeatCanPlayAI(getActivatingPlayer(), this) || mandatory;
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryRepeat.repeatStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryRepeat.repeatResolve(af, this);
            }
        }
        final SpellAbility dbRepeat = new DrawbackRepeat(af.getHostCard(), af.getAbTgt());

        return dbRepeat;
    }

    private static boolean repeatCanPlayAI(final Player ai, final SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Player opp = ai.getOpponent();
        if (tgt != null) {
            if (!opp.canBeTargetedBy(sa)) {
                return false;
            }
            tgt.resetTargets();
            tgt.addTarget(opp);
        }
        return true;
    }

    /**
     * <p>
     * repeatResolve.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void repeatResolve(final AbilityFactory af, final SpellAbility sa) {
        final AbilityFactory afRepeat = new AbilityFactory();
        final HashMap<String, String> params = af.getMapParams();
        Card source = sa.getSourceCard();

        // setup subability to repeat
        final SpellAbility repeat = afRepeat.getAbility(
                af.getHostCard().getSVar(params.get("RepeatSubAbility")), source);
        repeat.setActivatingPlayer(sa.getActivatingPlayer());
        ((AbilitySub) repeat).setParent(sa);

        Integer maxRepeat = null;
        if (params.containsKey("MaxRepeat")) {
            maxRepeat = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("MaxRepeat"), sa);
        }
        
        //execute repeat ability at least once
        int count = 0;
        do {
             AbilityFactory.resolve(repeat, false);
             count++;
             if (maxRepeat != null && maxRepeat <= count) {
                 // TODO Replace Infinite Loop Break with a game draw. Here are the scenarios that can cause this:
                 // Helm of Obedience vs Graveyard to Library replacement effect
                 StringBuilder infLoop = new StringBuilder(sa.getSourceCard().toString());
                 infLoop.append(" - To avoid an infinite loop, this repeat has been broken ");
                 infLoop.append(" and the game will now continue in the current state, ending the loop early. ");
                 infLoop.append("Once Draws are available this probably should change to a Draw.");
                 System.out.println(infLoop.toString());
                 break;
             }
       } while (checkRepeatConditions(af, sa));

    }

    /**
     * <p>
     * checkRepeatConditions.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static boolean checkRepeatConditions(final AbilityFactory af, final SpellAbility sa) {
        //boolean doAgain = false;
        final HashMap<String, String> params = af.getMapParams();

        if (params.containsKey("RepeatPresent")) {
            final String repeatPresent = params.get("RepeatPresent");
            List<Card> list = new ArrayList<Card>();

            String repeatCompare = "GE1";
            if (params.containsKey("RepeatCompare")) {
                repeatCompare = params.get("RepeatCompare");
            }

            if (params.containsKey("RepeatDefined")) {
                list.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("RepeatDefined"), sa));
            } else {
                list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
            }

            list = CardLists.getValidCards(list, repeatPresent.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            int right;
            final String rightString = repeatCompare.substring(2);
            try { // If this is an Integer, just parse it
                right = Integer.parseInt(rightString);
            } catch (final NumberFormatException e) { // Otherwise, grab it from
                                                      // the
                // SVar
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar(rightString));
            }

            final int left = list.size();

            if (!AllZoneUtil.compare(left, repeatCompare, right)) {
                return false;
            }
        }

        if (params.containsKey("RepeatCheckSVar")) {
            String sVarOperator = "GE";
            String sVarOperand = "1";
            if (params.containsKey("RepeatSVarCompare")) {
                sVarOperator = params.get("RepeatSVarCompare").substring(0, 2);
                sVarOperand = params.get("RepeatSVarCompare").substring(2);
            }
            final int svarValue = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("RepeatCheckSVar"), sa);
            final int operandValue = AbilityFactory.calculateAmount(sa.getSourceCard(), sVarOperand, sa);

            if (!AllZoneUtil.compare(svarValue, sVarOperator, operandValue)) {
                return false;
            }
        }

        if (params.containsKey("RepeatOptional")) {
            if (sa.getActivatingPlayer().isComputer()) {
                //TODO add logic to have computer make better choice (ArsenalNut)
                return false;
            } else {
                final StringBuilder sb = new StringBuilder();
                sb.append("Do you want to repeat this process again?");
                if (!GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * repeatStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String repeatStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        if (params.containsKey("StackDescription")) {
            final String desc = params.get("StackDescription");
            if (!desc.equals("None")) {
                sb.append(params.get("StackDescription"));
            }
        } else {
            sb.append("Repeat something. Somebody should really write a better StackDescription!");
        }

        return sb.toString();
    } // end repeatStackDescription()
} // end class AbilityFactory_Repeat
