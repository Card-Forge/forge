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
import java.util.Random;

import com.google.common.collect.Iterables;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.Constant;
import forge.Counters;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.InputPayManaCostUtil;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_Mana class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryMana {
    // ****************************** MANA ************************
    /**
     * <p>
     * createAbilityMana.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param produced
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityMana(final AbilityFactory abilityFactory, final String produced) {
        final String restrictions = abilityFactory.getMapParams().get("RestrictValid");
        class abFactoryMana extends AbilityMana {
            private static final long serialVersionUID = -1933592438783630254L;

            public abFactoryMana(Card ca, Cost co, String s, String s2) {
                super(ca, co, s, s2);
            }

            private final AbilityFactory af = abilityFactory;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryMana.manaCanPlayAI(this.af);
            }

            @Override
            public void resolve() {
                AbilityFactoryMana.manaResolve(this, this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public String getManaProduced() {
                return manaGenerated(this, this.af, this);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new abFactoryMana(getSourceCard(), getPayCosts(), this.getOrigProduced(), getManaRestrictions());
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }
        }
        final AbilityMana abMana = new abFactoryMana(abilityFactory.getHostCard(), abilityFactory.getAbCost(), produced, restrictions);
        return abMana;
    }

    /**
     * <p>
     * createSpellMana.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param produced
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellMana(final AbilityFactory abilityFactory, final String produced) {
        final String restrictions = abilityFactory.getMapParams().get("RestrictValid");
        final SpellAbility spMana = new Spell(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            private final AbilityFactory af = abilityFactory;
            // To get the mana to resolve properly, we need the spell to contain an AbilityMana
            private final Cost tmp = new Cost(abilityFactory.getHostCard(), "0", false);
            private final AbilityMana tmpMana = new AbilityMana(abilityFactory.getHostCard(), this.tmp, produced, restrictions) {
                private static final long serialVersionUID = 1454043766057140491L;

                @Override
                public boolean doTrigger(final boolean mandatory) {
                    // TODO Auto-generated method stub
                    return false;
                }

            };

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryMana.manaCanPlayAI(this.af);
            }

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryMana.manaStackDescription(this.tmpMana, this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryMana.manaResolve(this.tmpMana, this.af, this);
            }

        };
        return spMana;
    }

    // Mana never really appears as a Drawback
    /**
     * <p>
     * createDrawbackMana.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param produced
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.AbilitySub} object.
     */
    public static AbilitySub createDrawbackMana(final AbilityFactory abilityFactory, final String produced) {
        final String restrictions = abilityFactory.getMapParams().get("RestrictValid");
        class DrawbackMana extends AbilitySub {
            public DrawbackMana(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackMana(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                ((DrawbackMana) res).setTmp((AbilityMana) tmpMana.getCopy());
                return res;
            }

            private static final long serialVersionUID = -5141246507533353605L;

            private final AbilityFactory af = abilityFactory;
            // To get the mana to resolve properly, we need the spell to contain an AbilityMana
            private final Cost tmp = new Cost(abilityFactory.getHostCard(), "0", false);
            private AbilityMana tmpMana = new AbilityMana(abilityFactory.getHostCard(), this.tmp, produced, restrictions) {
                private static final long serialVersionUID = 1454043766057140491L;

                @Override
                public boolean doTrigger(final boolean mandatory) {
                    // TODO Auto-generated method stub
                    return false;
                }

            };

            public void setTmp(AbilityMana newTmp) {
                tmpMana = newTmp;
            }

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryMana.manaStackDescription(this.tmpMana, this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryMana.manaResolve(this.tmpMana, this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                // TODO Auto-generated method stub
                return false;
            }
        }
        final AbilitySub dbMana = new DrawbackMana(abilityFactory.getHostCard(), abilityFactory.getAbTgt());

        return dbMana;
    }

    /**
     * <p>
     * manaCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a boolean.
     */
    public static boolean manaCanPlayAI(final AbilityFactory af) {
        // AI cannot use this properly until he has a ManaPool
        return false;
    }

    /**
     * <p>
     * manaStackDescription.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String manaStackDescription(final AbilityMana abMana, final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(af.getHostCard()).append(" - ");
        }

        sb.append("Add ").append(AbilityFactoryMana.generatedMana(abMana, af, sa)).append(" to your mana pool.");

        if (abMana.getSubAbility() != null) {
            sb.append(abMana.getSubAbility().getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>manaGenerated.</p>
     *
     * @param abMana a {@link forge.card.spellability.AbilityMana} object.
     * @param af a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String manaGenerated(final AbilityMana abMana, final AbilityFactory af, final SpellAbility sa) {
        return generatedMana(abMana, af, sa);
    }

    /**
     * <p>
     * manaResolve.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void manaResolve(final AbilityMana abMana, final AbilityFactory af, final SpellAbility sa) {

        final HashMap<String, String> params = af.getMapParams();
        final Card card = af.getHostCard();

        if (!AbilityFactory.checkConditional(sa)) {
            AbilityFactoryMana.doDrawback(af, abMana, card);
            return;
        }

        // Spells are not undoable
        abMana.setUndoable(af.isAbility() && abMana.isUndoable());


        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (abMana.isComboMana()) {
            for (Player p : tgtPlayers) {
                int amount = params.containsKey("Amount") ? AbilityFactory.calculateAmount(af.getHostCard(),
                        params.get("Amount"), sa) : 1;
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    Player activator = sa.getActivatingPlayer(); 
                    // AI color choice is set in ComputerUtils so only human players need to make a choice
                    if (activator.isHuman()) {
                        //String colorsNeeded = abMana.getExpressChoice();
                        String[] colorsProduced = abMana.getComboColors().split(" ");
                        final StringBuilder choiceString = new StringBuilder();
                        String[] colorMenu = null;
                        if (!abMana.isAnyMana()) {
                            colorMenu = new String[colorsProduced.length];
                            //loop through colors to make menu
                            for (int nColor = 0; nColor < colorsProduced.length; nColor++) {
                                colorMenu[nColor] = InputPayManaCostUtil.getLongColorString(colorsProduced[nColor]);
                            }
                        }
                        else {
                            colorMenu = Constant.Color.ONLY_COLORS;
                        }
                        for (int nMana = 1; nMana <= amount; nMana++) {
                            String choice = "";
                                Object o = GuiChoose.one("Select Mana to Produce", colorMenu);
                                if (o == null) {
                                    final StringBuilder sb = new StringBuilder();
                                    sb.append("AbilityFactoryMana::manaResolve() - Human color mana choice is empty for ");
                                    sb.append(sa.getSourceCard().getName());
                                    throw new RuntimeException(sb.toString());
                                } else {
                                    choice = InputPayManaCostUtil.getShortColorString((String) o);
                                    if (nMana != 1) {
                                        choiceString.append(" ");
                                    }
                                    choiceString.append(choice);
                                }
                        }
                        abMana.setExpressChoice(choiceString.toString());
                    }
                    else {
                        // TODO: Add some logic for AI choice (ArsenalNut 2012/09/16)
                        if (params.containsKey("AILogic")) {
                            final String logic = params.get("AILogic");
                            String chosen = Constant.Color.BLACK;
                            if (logic.equals("MostProminentInComputerHand")) {
                                chosen = CardFactoryUtil.getMostProminentColor(activator.getCardsIn(
                                        ZoneType.Hand));
                            }
                            if (chosen.equals("")) {
                                chosen = Constant.Color.BLACK;
                            }
                            GuiChoose.one("Computer picked: ", new String[]{chosen});
                            abMana.setExpressChoice(InputPayManaCostUtil.getShortColorString(chosen));
                        }
                        if (abMana.getExpressChoice().isEmpty()) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("AbilityFactoryMana::manaResolve() - combo mana color choice is empty for ");
                            sb.append(sa.getSourceCard().getName());
                            throw new RuntimeException(sb.toString());
                        }
                    }
                }
            }
        }
        else if (abMana.isAnyMana()) {
            for (Player p : tgtPlayers) {
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    Player act = sa.getActivatingPlayer(); 
                    // AI color choice is set in ComputerUtils so only human players need to make a choice
                    if (act.isHuman()) {
                        String colorsNeeded = abMana.getExpressChoice();
                        String choice = "";
                        if (colorsNeeded.length() == 1) {
                            choice = colorsNeeded;
                        }
                        else {
                            String[] colorMenu = null;
                            if (colorsNeeded.length() > 1 && colorsNeeded.length() < 5) {
                                colorMenu = new String[colorsNeeded.length()];
                                //loop through colors to make menu
                                for (int nChar = 0; nChar < colorsNeeded.length(); nChar++) {
                                    colorMenu[nChar] = InputPayManaCostUtil.getLongColorString(colorsNeeded.substring(nChar, nChar + 1));
                                }
                            }
                            else {
                                colorMenu = Constant.Color.ONLY_COLORS;
                            }
                            String s = GuiChoose.one("Select Mana to Produce", colorMenu);
                            if (s == null) {
                                final StringBuilder sb = new StringBuilder();
                                sb.append("AbilityFactoryMana::manaResolve() - Human color mana choice is empty for ");
                                sb.append(sa.getSourceCard().getName());
                                throw new RuntimeException(sb.toString());
                            } else {
                                choice = InputPayManaCostUtil.getShortColorString(s);
                            }
                        }
                        abMana.setExpressChoice(choice);
                    }
                    else {
                        if (params.containsKey("AILogic")) {
                            final String logic = params.get("AILogic");
                            String chosen = Constant.Color.BLACK;
                            if (logic.equals("MostProminentInComputerHand")) {
                                chosen = CardFactoryUtil.getMostProminentColor(act.getCardsIn(ZoneType.Hand));
                            }
                            GuiChoose.one("Computer picked: ", new String[]{chosen});
                            abMana.setExpressChoice(InputPayManaCostUtil.getShortColorString(chosen));
                        }
                        if (abMana.getExpressChoice().isEmpty()) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("AbilityFactoryMana::manaResolve() - any color mana choice is empty for ");
                            sb.append(sa.getSourceCard().getName());
                            throw new RuntimeException(sb.toString());
                        }
                    }
                }
            }
        }

        for (final Player player : tgtPlayers) {
            abMana.produceMana(AbilityFactoryMana.generatedMana(abMana, af, sa), player);
        }

        // Only clear express choice after mana has been produced
        abMana.clearExpressChoice();

        // convert these to SubAbilities when appropriate
        if (params.containsKey("Stuck")) {
            abMana.setUndoable(false);
            card.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
        }

        final String deplete = params.get("Deplete");
        if (deplete != null) {
            final int num = card.getCounters(Counters.getType(deplete));
            if (num == 0) {
                abMana.setUndoable(false);
                Singletons.getModel().getGame().getAction().sacrifice(card, null);
            }
        }

        AbilityFactoryMana.doDrawback(af, abMana, card);
    }

    /**
     * <p>
     * generatedMana.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String generatedMana(final AbilityMana abMana, final AbilityFactory af, final SpellAbility sa) {
        // Calculate generated mana here for stack description and resolving
        final HashMap<String, String> params = af.getMapParams();
        int amount = params.containsKey("Amount") ? AbilityFactory.calculateAmount(af.getHostCard(),
                params.get("Amount"), sa) : 1;

        String baseMana;
        if (abMana.isComboMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = abMana.getOrigProduced();
            }
        }
        else if (abMana.isAnyMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = "Any";
            }
        }
        else {
            baseMana = abMana.mana();
        }

        if (params.containsKey("Bonus")) {
            // For mana abilities that get a bonus
            // Bonus currently MULTIPLIES the base amount. Base Amounts should
            // ALWAYS be Base
            int bonus = 0;
            if (params.get("Bonus").equals("UrzaLands")) {
                if (AbilityFactoryMana.hasUrzaLands(abMana.getActivatingPlayer())) {
                    bonus = Integer.parseInt(params.get("BonusProduced"));
                }
            }

            amount += bonus;
        }

        try {
            if ((params.get("Amount") != null) && (amount != Integer.parseInt(params.get("Amount")))) {
                abMana.setUndoable(false);
            }
        } catch (final NumberFormatException n) {
            abMana.setUndoable(false);
        }

        final StringBuilder sb = new StringBuilder();
        if (amount == 0) {
            sb.append("0");
        }
        else if (abMana.isComboMana()) {
            // amount is already taken care of in resolve method for combination mana, just append baseMana
            sb.append(baseMana);
        }
        else {
            try {
                // if baseMana is an integer(colorless), just multiply amount
                // and baseMana
                final int base = Integer.parseInt(baseMana);
                sb.append(base * amount);
            } catch (final NumberFormatException e) {
                for (int i = 0; i < amount; i++) {
                    if (i != 0) {
                        sb.append(" ");
                    }
                    sb.append(baseMana);
                }
            }
        }
        return sb.toString();
    }

    // ****************************** MANAREFLECTED ************************
    /**
     * <p>
     * createAbilityManaReflected.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param produced
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityManaReflected(final AbilityFactory abilityFactory, final String produced) {
        class abFactoryReflectedMana extends AbilityMana {
            private static final long serialVersionUID = -1933592438783630254L;

            public abFactoryReflectedMana(Card ca, Cost co, String s) {
                super(ca, co, s);
            }

            private final AbilityFactory af = abilityFactory;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryMana.manaReflectedCanPlayAI(this.af);
            }

            @Override
            public void resolve() {
                AbilityFactoryMana.manaReflectedResolve(this, this.af);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new abFactoryReflectedMana(getSourceCard(), getPayCosts(), getManaProduced());
                CardFactoryUtil.copySpellAbility(this, res);
                ((AbilityMana) res).setReflectedMana(true);
                return res;
            }
        }
        final AbilityMana abMana = new abFactoryReflectedMana(abilityFactory.getHostCard(), abilityFactory.getAbCost(), produced);
        abMana.setReflectedMana(true);
        return abMana;
    }

    /**
     * <p>
     * createSpellManaReflected.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param produced
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellManaReflected(final AbilityFactory abilityFactory, final String produced) {
        // No Spell has Reflected Mana, but might as well put it in for the
        // future
        final SpellAbility spMana = new Spell(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            private final AbilityFactory af = abilityFactory;
            // To get the mana to resolve properly, we need the spell to contain an AbilityMana
            private final Cost tmp = new Cost(abilityFactory.getHostCard(), "0", false);
            private final AbilityMana tmpMana = new AbilityMana(abilityFactory.getHostCard(), this.tmp, produced) {
                private static final long serialVersionUID = 1454043766057140491L;

                @Override
                public boolean doTrigger(final boolean mandatory) {
                    // TODO Auto-generated method stub
                    return false;
                }

                // TODO: maybe add can produce here, so old AI code can use
                // reflected mana?
            };

            // tmpMana.setReflectedMana(true);

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryMana.manaReflectedCanPlayAI(this.af);
            }

            @Override
            public void resolve() {
                AbilityFactoryMana.manaReflectedResolve(this.tmpMana, this.af);
            }

        };
        return spMana;
    }

    /**
     * <p>
     * manaReflectedCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a boolean.
     */
    public static boolean manaReflectedCanPlayAI(final AbilityFactory af) {
        // AI cannot use this properly until he has a ManaPool
        return false;
    }

    /**
     * <p>
     * manaReflectedResolve.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public static void manaReflectedResolve(final AbilityMana abMana, final AbilityFactory af) {
        // Spells are not undoable
        final HashMap<String, String> params = af.getMapParams();
        abMana.setUndoable(af.isAbility() && abMana.isUndoable());

        final Card card = af.getHostCard();

        final ArrayList<String> colors = AbilityFactoryMana.reflectableMana(abMana, af, new ArrayList<String>(),
                new ArrayList<Card>());

        ArrayList<Player> tgtPlayers;

        final Target tgt = abMana.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(abMana.getSourceCard(), params.get("Defined"), abMana);
        }

        for (final Player player : tgtPlayers) {
            final String generated = AbilityFactoryMana.generatedReflectedMana(abMana, af, colors, player);

            if (abMana.getCanceled()) {
                abMana.undo();
                abMana.setCanceled(false);
                return;
            }

            abMana.produceMana(generated, player);
        }

        AbilityFactoryMana.doDrawback(af, abMana, card);
    }

    // add Colors and
    /**
     * <p>
     * reflectableMana.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @param parents
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> reflectableMana(final AbilityMana abMana, final AbilityFactory af,
            ArrayList<String> colors, final ArrayList<Card> parents) {
        // Here's the problem with reflectable Mana. If more than one is out,
        // they need to Reflect each other,
        // so we basically need to have a recursive list that send the parents
        // so we don't infinite recurse.
        final HashMap<String, String> params = af.getMapParams();
        final Card card = af.getHostCard();

        if (!parents.contains(card)) {
            parents.add(card);
        }

        final String colorOrType = params.get("ColorOrType"); // currently Color
                                                              // or
        // Type, Type is colors
        // + colorless
        final String validCard = params.get("Valid");
        final String reflectProperty = params.get("ReflectProperty"); // Produce
        // (Reflecting
        // Pool) or Is
        // (Meteor
        // Crater)

        int maxChoices = 5; // Color is the default colorOrType
        if (colorOrType.equals("Type")) {
            maxChoices++;
        }

        List<Card> cards = null;

        // Reuse AF_Defined in a slightly different way
        if (validCard.startsWith("Defined.")) {
            cards = new ArrayList<Card>();
            for (final Card c : AbilityFactory.getDefinedCards(card, validCard.replace("Defined.", ""), abMana)) {
                cards.add(c);
            }
        } else {
            cards = CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), validCard, abMana.getActivatingPlayer(), card);
        }

        // remove anything cards that is already in parents
        for (final Card p : parents) {
            if (cards.contains(p)) {
                cards.remove(p);
            }
        }

        if ((cards.size() == 0) && !reflectProperty.equals("Produced")) {
            return colors;
        }

        if (reflectProperty.equals("Is")) { // Meteor Crater
            colors = AbilityFactoryMana.hasProperty(maxChoices, cards, colors);
        } else if (reflectProperty.equals("Produced")) {
            final String producedColors = (String) abMana.getTriggeringObject("Produced");
            for (final String col : Constant.Color.ONLY_COLORS) {
                final String s = InputPayManaCostUtil.getShortColorString(col);
                if (producedColors.contains(s) && !colors.contains(col)) {
                    colors.add(col);
                }
            }
            if ((maxChoices == 6) && producedColors.contains("1") && !colors.contains(Constant.Color.COLORLESS)) {
                colors.add(Constant.Color.COLORLESS);
            }
        } else if (reflectProperty.equals("Produce")) {
            final ArrayList<AbilityMana> abilities = new ArrayList<AbilityMana>();
            for (final Card c : cards) {
                abilities.addAll(c.getManaAbility());
            }
            // currently reflected mana will ignore other reflected mana
            // abilities

            final ArrayList<AbilityMana> reflectAbilities = new ArrayList<AbilityMana>();

            for (final AbilityMana ab : abilities) {
                if (maxChoices == colors.size()) {
                    break;
                }

                if (ab.isReflectedMana()) {
                    if (!parents.contains(ab.getSourceCard())) {
                        // Recursion!
                        reflectAbilities.add(ab);
                        parents.add(ab.getSourceCard());
                    }
                    continue;
                }
                colors = AbilityFactoryMana.canProduce(maxChoices, ab, colors);
                if (!parents.contains(ab.getSourceCard())) {
                    parents.add(ab.getSourceCard());
                }
            }

            for (final AbilityMana ab : reflectAbilities) {
                if (maxChoices == colors.size()) {
                    break;
                }

                colors = AbilityFactoryMana.reflectableMana(ab, ab.getAbilityFactory(), colors, parents);
            }
        }

        return colors;
    }

    /**
     * <p>
     * hasProperty.
     * </p>
     * 
     * @param maxChoices
     *            a int.
     * @param cards
     *            a {@link forge.CardList} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private static ArrayList<String> hasProperty(final int maxChoices, final List<Card> cards,
            final ArrayList<String> colors) {
        for (final Card c : cards) {
            // For each card, go through all the colors and if the card is that
            // color, add
            for (final String col : Constant.Color.ONLY_COLORS) {
                if (c.isColor(col) && !colors.contains(col)) {
                    colors.add(col);
                    if (colors.size() == maxChoices) {
                        break;
                    }
                }
            }
        }
        return colors;
    }

    /**
     * <p>
     * canProduce.
     * </p>
     * 
     * @param maxChoices
     *            a int.
     * @param ab
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private static ArrayList<String> canProduce(final int maxChoices, final AbilityMana ab,
            final ArrayList<String> colors) {
        for (final String col : Constant.Color.ONLY_COLORS) {
            final String s = InputPayManaCostUtil.getShortColorString(col);
            if (ab.canProduce(s) && !colors.contains(col)) {
                colors.add(col);
            }
        }

        if ((maxChoices == 6) && ab.canProduce("1") && !colors.contains(Constant.Color.COLORLESS)) {
            colors.add(Constant.Color.COLORLESS);
        }

        return colors;
    }

    /**
     * <p>
     * generatedReflectedMana.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a {@link java.lang.String} object.
     */
    private static String generatedReflectedMana(final AbilityMana abMana, final AbilityFactory af,
            final ArrayList<String> colors, final Player player) {
        // Calculate generated mana here for stack description and resolving
        final HashMap<String, String> params = af.getMapParams();
        final int amount = params.containsKey("Amount") ? AbilityFactory.calculateAmount(af.getHostCard(),
                params.get("Amount"), abMana) : 1;

        String baseMana = "";

        if (colors.size() == 0) {
            return "0";
        } else if (colors.size() == 1) {
            baseMana = InputPayManaCostUtil.getShortColorString(colors.get(0));
        } else {
            if (player.isHuman()) {
                final Object o = GuiChoose.oneOrNone("Select Mana to Produce", colors);
                if (o == null) {
                    // User hit cancel
                    abMana.setCanceled(true);
                    return "";
                } else {
                    baseMana = InputPayManaCostUtil.getShortColorString((String) o);
                }
            } else {
                // AI doesn't really have anything here yet
                baseMana = InputPayManaCostUtil.getShortColorString(colors.get(0));
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (amount == 0) {
            sb.append("0");
        } else {
            try {
                // if baseMana is an integer(colorless), just multiply amount
                // and baseMana
                final int base = Integer.parseInt(baseMana);
                sb.append(base * amount);
            } catch (final NumberFormatException e) {
                for (int i = 0; i < amount; i++) {
                    if (i != 0) {
                        sb.append(" ");
                    }
                    sb.append(baseMana);
                }
            }
        }
        return sb.toString();
    }

    // *************** Utility Functions **********************

    /**
     * <p>
     * doDrawback.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param card
     *            a {@link forge.Card} object.
     */
    public static void doDrawback(final AbilityFactory af, final AbilityMana abMana, final Card card) {

        // if mana production has any type of SubAbility, undoable=false
        if (af.hasSubAbility()) {
            abMana.setUndoable(false);
            final AbilitySub abSub = abMana.getSubAbility();
            AbilityFactory.resolve(abSub, false);
        }
    }

    /**
     * <p>
     * hasUrzaLands.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    private static boolean hasUrzaLands(final Player p) {
        final List<Card> landsControlled = p.getCardsIn(ZoneType.Battlefield);
        return Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Mine")) &&  
                Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Tower")) && 
                Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Power Plant"));
    }

    // ****************************************
    // ************** DrainMana ***************
    // ****************************************

    /**
     * <p>
     * createAbilityDrainMana.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilityDrainMana(final AbilityFactory af) {
        class AbilityDrainMana extends AbilityActivated {
            public AbilityDrainMana(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityDrainMana(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5669367387381350104L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryMana.drainManaStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryMana.drainManaCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryMana.drainManaResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryMana.drainManaTrigger(getActivatingPlayer(), af, this, mandatory);
            }
        }

        final SpellAbility abDrainMana = new AbilityDrainMana(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abDrainMana;
    }

    /**
     * <p>
     * createSpellDrainMana.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellDrainMana(final AbilityFactory af) {
        final SpellAbility spDrainMana = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4294474468024747680L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryMana.drainManaStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryMana.drainManaCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryMana.drainManaResolve(af, this);
            }

        };
        return spDrainMana;
    }

    /**
     * <p>
     * createDrawbackDrainMana.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackDrainMana(final AbilityFactory af) {
        class DrawbackDrainMana extends AbilitySub {
            public DrawbackDrainMana(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackDrainMana(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 1458568386420831420L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryMana.drainManaStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryMana.drainManaResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryMana.drainManaPlayDrawbackAI(getActivatingPlayer(), af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryMana.drainManaTrigger(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbDrainMana = new DrawbackDrainMana(af.getHostCard(), af.getAbTgt());

        return dbDrainMana;
    }

    /**
     * <p>
     * drainManaStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String drainManaStackDescription(final AbilityFactory af, final SpellAbility sa) {
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
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        final Iterator<Player> it = tgtPlayers.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(" empties his or her mana pool.");

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * drainManaCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean drainManaCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn

        final HashMap<String, String> params = af.getMapParams();
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final Player opp = ai.getOpponent();
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (tgt == null) {
            // assume we are looking to tap human's stuff
            // TODO - check for things with untap abilities, and don't tap
            // those.
            final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, params.get("Defined"), sa);

            if (!defined.contains(opp)) {
                return false;
            }
        } else {
            tgt.resetTargets();
            tgt.addTarget(opp);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * drainManaTrigger.
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
    private static boolean drainManaTrigger(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }
        final Player opp = ai.getOpponent();

        final HashMap<String, String> params = af.getMapParams();
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        if (null == tgt) {
            if (mandatory) {
                return true;
            } else {
                final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, params.get("Defined"), sa);

                if (!defined.contains(opp)) {
                    return false;
                }
            }

            return true;
        } else {
            tgt.resetTargets();
            tgt.addTarget(opp);
        }

        return true;
    }

    /**
     * <p>
     * drainManaPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean drainManaPlayDrawbackAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final HashMap<String, String> params = af.getMapParams();
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        boolean randomReturn = true;

        if (tgt == null) {
            final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, params.get("Defined"), sa);

            if (defined.contains(ai)) {
                return false;
            }
        } else {
            tgt.resetTargets();
            tgt.addTarget(ai.getOpponent());
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * drainManaResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void drainManaResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();

        ArrayList<Player> tgtPlayers;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(card, params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.getManaPool().clearPool(false);
            }
        }
    }

} // end class AbilityFactory_Mana
