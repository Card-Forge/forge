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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import forge.AllZone;
import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Constant;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.cost.Cost;
import forge.game.GameState;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.ListChooser;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.util.MyRandom;


/**
 * <p>
 * AbilityFactoryChoose class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryChoose {

    private AbilityFactoryChoose() {
        throw new AssertionError();
    }

    // *************************************************************************
    // ************************* ChooseType ************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChooseType.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityChooseType(final AbilityFactory af) {
        class AbilityChooseType extends AbilityActivated {
            public AbilityChooseType(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityChooseType(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -7734286034988741837L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseTypeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseTypeCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseTypeResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseTypeTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abChooseType = new AbilityChooseType(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abChooseType;
    }

    /**
     * <p>
     * createSpellChooseType.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellChooseType(final AbilityFactory af) {
        final SpellAbility spChooseType = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3395765985146644736L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseTypeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseTypeCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseTypeResolve(af, this);
            }

        };
        return spChooseType;
    }

    /**
     * <p>
     * createDrawbackChooseType.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackChooseType(final AbilityFactory af) {
        class DrawbackChooseType extends AbilitySub {
            public DrawbackChooseType(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackChooseType(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5555184803257696143L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseTypeStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseTypeResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseTypeTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbChooseType = new DrawbackChooseType(af.getHostCard(), af.getAbTgt());

        return dbChooseType;
    }

    /**
     * <p>
     * chooseTypeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseTypeStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a type.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * chooseTypeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseTypeCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        if (!params.containsKey("AILogic")) {
            return false;
        }

        return AbilityFactoryChoose.chooseTypeTriggerAI(ai, af, sa, false);
    }

    /**
     * <p>
     * chooseTypeTriggerAI.
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
    private static boolean chooseTypeTriggerAI(final Player ai,final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai);
        } else {
            final ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams()
                    .get("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isHuman() && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * chooseTypeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseTypeResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();
        final String type = params.get("Type");
        final ArrayList<String> invalidTypes = new ArrayList<String>();
        if (params.containsKey("InvalidTypes")) {
            invalidTypes.addAll(Arrays.asList(params.get("InvalidTypes").split(",")));
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {

                if (type.equals("Card")) {
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            final Object o = GuiChoose.one("Choose a card type", Constant.CardTypes.CARD_TYPES);
                            if (null == o) {
                                return;
                            }
                            final String choice = (String) o;
                            if (CardUtil.isACardType(choice) && !invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            // TODO
                            // computer will need to choose a type
                            // based on whether it needs a creature or land,
                            // otherwise, lib search for most common type left
                            // then, reveal chosenType to Human
                        }
                    }
                } else if (type.equals("Creature")) {
                    String chosenType = "";
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            final ArrayList<String> validChoices = CardUtil.getCreatureTypes();
                            for (final String s : invalidTypes) {
                                validChoices.remove(s);
                            }
                            final Object o = GuiChoose.one("Choose a creature type", validChoices);
                            if (null == o) {
                                return;
                            }
                            final String choice = (String) o;
                            if (CardUtil.isACreatureType(choice) && !invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            Player ai = sa.getActivatingPlayer();
                            Player opp = ai.getOpponent();
                            String chosen = "";
                            if (params.containsKey("AILogic")) {
                                final String logic = params.get("AILogic");
                                if (logic.equals("MostProminentOnBattlefield")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(GameState
                                            .getCardsIn(ZoneType.Battlefield));
                                }
                                if (logic.equals("MostProminentComputerControls")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(ai.getCardsIn(ZoneType.Battlefield));
                                }
                                if (logic.equals("MostProminentHumanControls")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(opp.getCardsIn(ZoneType.Battlefield));
                                    if (!CardUtil.isACreatureType(chosen) || invalidTypes.contains(chosen)) {
                                        chosen = CardFactoryUtil.getMostProminentCreatureType(CardLists.filterControlledBy(GameState.getCardsInGame(), opp));
                                    }
                                }
                                if (logic.equals("MostProminentInComputerDeck")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(CardLists.filterControlledBy(GameState.getCardsInGame(), ai));
                                }
                                if (logic.equals("MostProminentInComputerGraveyard")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(ai.getCardsIn(ZoneType.Graveyard));
                                }
                            }
                            if (!CardUtil.isACreatureType(chosen) || invalidTypes.contains(chosen)) {
                                chosen = "Sliver";
                            }
                            GuiChoose.one("Computer picked: ", new String[]{chosen});
                            chosenType = chosen;
                        }
                        if (CardUtil.isACreatureType(chosenType) && !invalidTypes.contains(chosenType)) {
                            valid = true;
                            card.setChosenType(chosenType);
                        }
                    }
                } else if (type.equals("Basic Land")) {
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            final String choice = GuiChoose.one("Choose a basic land type", CardUtil.getBasicTypes());
                            if (null == choice) {
                                return;
                            }
                            if (CardUtil.isABasicLandType(choice) && !invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            // TODO
                            // computer will need to choose a type
                        }
                    }
                } else if (type.equals("Land")) {
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            final String choice = GuiChoose
                                    .one("Choose a land type", CardUtil.getLandTypes());
                            if (null == choice) {
                                return;
                            }
                            if (!invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            // TODO
                            // computer will need to choose a type
                        }
                    }
                } // end if-else if
            }
        }
    }

    // *************************************************************************
    // ************************* ChooseColor ***********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChooseColor.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilityChooseColor(final AbilityFactory af) {
        class AbilityChooseColor extends AbilityActivated {
            public AbilityChooseColor(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityChooseColor(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 7069068165774633355L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseColorStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseColorCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseColorResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseColorTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abChooseColor = new AbilityChooseColor(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abChooseColor;
    }

    /**
     * <p>
     * createSpellChooseColor.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellChooseColor(final AbilityFactory af) {
        final SpellAbility spChooseColor = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5627273779759130247L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseColorStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseColorCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseColorResolve(af, this);
            }

        };
        return spChooseColor;
    }

    /**
     * <p>
     * createDrawbackChooseColor.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackChooseColor(final AbilityFactory af) {
        class DrawbackChooseColor extends AbilitySub {
            public DrawbackChooseColor(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackChooseColor(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 6969618586164278998L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseColorStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseColorResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseColorTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbChooseColor = new DrawbackChooseColor(af.getHostCard(), af.getAbTgt());

        return dbChooseColor;
    }

    /**
     * <p>
     * chooseColorStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseColorStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a color");
        if (params.containsKey("OrColors")) {
            sb.append(" or colors");
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
     * chooseColorCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseColorCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        if (!params.containsKey("AILogic")) {
            return false;
        }
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }
        return chance;
    }

    /**
     * <p>
     * chooseColorTriggerAI.
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
    private static boolean chooseColorTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        return mandatory || chooseColorCanPlayAI(af, sa);
    }

    /**
     * <p>
     * chooseColorResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseColorResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    if (params.containsKey("OrColors")) {
                        final List<String> o = GuiChoose.oneOrMany("Choose a color or colors",
                                Constant.Color.ONLY_COLORS);
                        card.setChosenColor(new ArrayList<String>(o));
                    } else if (params.containsKey("TwoColors")) {
                        final List<String> o = GuiChoose.amount("Choose two colors", Constant.Color.ONLY_COLORS, 2);
                        card.setChosenColor(new ArrayList<String>(o));
                    } else {
                        final Object o = GuiChoose.one("Choose a color", Constant.Color.ONLY_COLORS);
                        if (null == o) {
                            return;
                        }
                        final String choice = (String) o;
                        final ArrayList<String> tmpColors = new ArrayList<String>();
                        tmpColors.add(choice);
                        card.setChosenColor(tmpColors);
                    }
                } else {
                    List<String> chosen = new ArrayList<String>();
                    Player ai = sa.getActivatingPlayer();
                    Player opp = ai.getOpponent();
                    if (params.containsKey("AILogic")) {
                        final String logic = params.get("AILogic");
                        if (logic.equals("MostProminentInHumanDeck")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(CardLists.filterControlledBy(GameState.getCardsInGame(), opp)));
                        } else if (logic.equals("MostProminentInComputerDeck")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(CardLists.filterControlledBy(GameState.getCardsInGame(), ai)));
                        } else if (logic.equals("MostProminentDualInComputerDeck")) {
                            List<String> prominence = CardFactoryUtil.getColorByProminence(CardLists.filterControlledBy(GameState.getCardsInGame(), ai));
                            chosen.add(prominence.get(0));
                            chosen.add(prominence.get(1));
                        }
                        else if (logic.equals("MostProminentInGame")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(GameState.getCardsInGame()));
                        }
                        else if (logic.equals("MostProminentHumanCreatures")) {
                            List<Card> list = GameState.getCreaturesInPlay(opp);
                            if (list.isEmpty()) {
                                list = CardLists.filter(CardLists.filterControlledBy(GameState.getCardsInGame(), opp), CardPredicates.Presets.CREATURES);
                            }
                            chosen.add(CardFactoryUtil.getMostProminentColor(list));
                        }
                        else if (logic.equals("MostProminentComputerControls")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(ai.getCardsIn(ZoneType.Battlefield)));
                        }
                        else if (logic.equals("MostProminentPermanent")) {
                            final List<Card> list = GameState.getCardsIn(ZoneType.Battlefield);
                            chosen.add(CardFactoryUtil.getMostProminentColor(list));
                        }
                        else if (logic.equals("MostProminentAttackers")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(AllZone.getCombat()
                                    .getAttackerList()));
                        }
                    }
                    if (chosen.size() == 0) {
                        chosen.add(Constant.Color.GREEN);
                    }
                    GuiChoose.one("Computer picked: ", chosen);
                    final ArrayList<String> colorTemp = new ArrayList<String>();
                    colorTemp.addAll(chosen);
                    card.setChosenColor(colorTemp);
                }
            }
        }
    }

    // *************************************************************************
    // ************************* ChooseNumber **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChooseNumber.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createAbilityChooseNumber(final AbilityFactory af) {
        class AbilityChooseNumber extends AbilityActivated {
            public AbilityChooseNumber(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityChooseNumber(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -8268155210011368749L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseNumberStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseNumberCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseNumberResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseNumberTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abChooseNumber = new AbilityChooseNumber(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abChooseNumber;
    }

    /**
     * <p>
     * createSpellChooseNumber.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createSpellChooseNumber(final AbilityFactory af) {
        final SpellAbility spChooseNumber = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6397887501014311392L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseNumberStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseNumberCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseNumberResolve(af, this);
            }

        };
        return spChooseNumber;
    }

    /**
     * <p>
     * createDrawbackChooseNumber.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createDrawbackChooseNumber(final AbilityFactory af) {
        class DrawbackChooseNumber extends AbilitySub {
            public DrawbackChooseNumber(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackChooseNumber(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -1339609900364066904L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseNumberStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseNumberResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseNumberTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbChooseNumber = new DrawbackChooseNumber(af.getHostCard(), af.getAbTgt());

        return dbChooseNumber;
    }

    /**
     * <p>
     * chooseNumberStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseNumberStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

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
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a number.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * chooseNumberCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseNumberCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryChoose.chooseNumberTriggerAI(af, sa, false);
    }

    /**
     * <p>
     * chooseNumberTriggerAI.
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
    private static boolean chooseNumberTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        return false;
    }

    /**
     * <p>
     * chooseNumberResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseNumberResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();
        //final int min = params.containsKey("Min") ? Integer.parseInt(params.get("Min")) : 0;
        //final int max = params.containsKey("Max") ? Integer.parseInt(params.get("Max")) : 99;
        final boolean random = params.containsKey("Random");

        final int min;
        if (!params.containsKey("Min")) {
            min = Integer.parseInt("0");
        } else if (params.get("Min").matches("[0-9][0-9]?")) {
            min = Integer.parseInt(params.get("Min"));
        } else {
            min = CardFactoryUtil.xCount(card, card.getSVar(params.get("Min")));
        } // Allow variables for Min

        final int max;
        if (!params.containsKey("Max")) {
            max = Integer.parseInt("99");
        } else if (params.get("Max").matches("[0-9][0-9]?")) {
            max = Integer.parseInt(params.get("Max"));
        } else {
            max = CardFactoryUtil.xCount(card, card.getSVar(params.get("Max")));
        } // Allow variables for Max

        final String[] choices = new String[max + 1];
        if (!random) {
            // initialize the array
            for (int i = min; i <= max; i++) {
                choices[i] = Integer.toString(i);
            }
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    int chosen;
                    if (random) {
                        final Random randomGen = new Random();
                        chosen = randomGen.nextInt(max - min) + min;
                        final String message = "Randomly chosen number: " + chosen;
                        JOptionPane.showMessageDialog(null, message, "" + card, JOptionPane.PLAIN_MESSAGE);
                    } else if (params.containsKey("ListTitle")) {
                        final Object o = GuiChoose.one(params.get("ListTitle"), choices);
                        if (null == o) {
                            return;
                        }
                        chosen = Integer.parseInt((String) o);
                    } else {
                        final Object o = GuiChoose.one("Choose a number", choices);
                        if (null == o) {
                            return;
                        }
                        chosen = Integer.parseInt((String) o);
                    }
                    card.setChosenNumber(chosen);

                } else {
                    // TODO - not implemented
                }
            }
        }
    }

    // *************************************************************************
    // ************************* ChoosePlayer **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChoosePlayer.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createAbilityChoosePlayer(final AbilityFactory af) {
        class AbilityChoosePlayer extends AbilityActivated {
            public AbilityChoosePlayer(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityChoosePlayer(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 7502903475594562552L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.choosePlayerStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.choosePlayerCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.choosePlayerResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.choosePlayerTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abChoosePlayer = new AbilityChoosePlayer(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abChoosePlayer;
    }

    /**
     * <p>
     * createSpellChoosePlayer.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createSpellChoosePlayer(final AbilityFactory af) {
        final SpellAbility spChoosePlayer = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {

            private static final long serialVersionUID = -7684507578494661495L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.choosePlayerStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.choosePlayerCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.choosePlayerResolve(af, this);
            }

        };
        return spChoosePlayer;
    }

    /**
     * <p>
     * createDrawbackChoosePlayer.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createDrawbackChoosePlayer(final AbilityFactory af) {
        class DrawbackChoosePlayer extends AbilitySub {
            public DrawbackChoosePlayer(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackChoosePlayer(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -766158106632103029L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.choosePlayerStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.choosePlayerResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.choosePlayerTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbChoosePlayer = new DrawbackChoosePlayer(af.getHostCard(), af.getAbTgt());

        return dbChoosePlayer;
    }

    /**
     * <p>
     * choosePlayerStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String choosePlayerStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

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
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a player.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * choosePlayerCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean choosePlayerCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryChoose.choosePlayerTriggerAI(af, sa, false);
    }

    /**
     * <p>
     * choosePlayerTriggerAI.
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
    private static boolean choosePlayerTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        return false;
    }

    /**
     * <p>
     * choosePlayerResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void choosePlayerResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        final ArrayList<Player> choices = params.containsKey("Choices") ? AbilityFactory.getDefinedPlayers(
                sa.getSourceCard(), params.get("Choices"), sa) : new ArrayList<Player>(Singletons.getModel().getGameState().getPlayers());

        final String choiceDesc = params.containsKey("ChoiceTitle") ? params.get("ChoiceTitle") : "Choose a player";

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (p.isHuman()) {
                    // Was if (sa.getActivatingPlayer().isHuman()) but defined player was being
                    // overwritten by activatingPlayer (or controller if no activator was set).
                    // Revert if it causes issues and remove Goblin Festival from card database.
                    final Object o = GuiChoose.one(choiceDesc, choices);
                    if (null == o) {
                        return;
                    }
                    final Player chosen = (Player) o;
                    card.setChosenPlayer(chosen);

                } else {
                    if (params.containsKey("AILogic")) {
                        if (params.get("AILogic").equals("Curse")) {
                            card.setChosenPlayer(p.getOpponent());
                        } else {
                            card.setChosenPlayer(p);
                        }
                    } else {
                        card.setChosenPlayer(p);
                    }
                }
            }
        }
    }

    // *************************************************************************
    // ***************************** NameCard **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityNameCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createAbilityNameCard(final AbilityFactory af) {
        class AbilityNameCard extends AbilityActivated {
            public AbilityNameCard(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityNameCard(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 1748714246609515354L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.nameCardStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.nameCardCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.nameCardResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.nameCardTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abNameCard = new AbilityNameCard(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abNameCard;
    }

    /**
     * <p>
     * createSpellNameCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createSpellNameCard(final AbilityFactory af) {
        final SpellAbility spNameCard = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 209265128022008897L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.nameCardStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.nameCardCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.nameCardResolve(af, this);
            }

        };
        return spNameCard;
    }

    /**
     * <p>
     * createDrawbackNameCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createDrawbackNameCard(final AbilityFactory af) {
        class DrawbackNameCard extends AbilitySub {
            public DrawbackNameCard(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackNameCard(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -7647726271751061495L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.nameCardStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.nameCardResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.nameCardTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbNameCard = new DrawbackNameCard(af.getHostCard(), af.getAbTgt());

        return dbNameCard;
    }

    /**
     * <p>
     * nameCardStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String nameCardStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

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
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("names a card.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * nameCardCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean nameCardCanPlayAI(final Player ai,final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        if (params.containsKey("AILogic")) {
            // Don't tap creatures that may be able to block
            if (AbilityFactory.waitForBlocking(sa)) {
                return false;
            }

            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgt.resetTargets();
                if (tgt.canOnlyTgtOpponent()) {
                    tgt.addTarget(ai.getOpponent());
                } else {
                    tgt.addTarget(ai);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * <p>
     * nameCardTriggerAI.
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
    private static boolean nameCardTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // TODO - there is no AILogic implemented yet
        return false;
    }

    /**
     * <p>
     * nameCardResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void nameCardResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        String valid = "Card";
        String validDesc = "card";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
            validDesc = params.get("ValidDesc");
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                boolean ok = false;
                String name = null;
                while (!ok) {
                    if (p.isHuman()) {
                        final String message = validDesc.equals("card") ? "Name a card" : "Name a " + validDesc
                                + " card. (Case sensitive)";
                        /*
                         * name = JOptionPane.showInputDialog(null, message,
                         * host.getName(), JOptionPane.QUESTION_MESSAGE); if
                         * (!valid.equals("Card") && !(null == name)) { try {
                         * final Card temp =
                         * AllZone.getCardFactory().getCard(name, p); ok =
                         * temp.isValid(valid, host.getController(), host); }
                         * catch (final Exception ignored) { ok = false; } }
                         * else { ok = true; } if (ok) { host.setNamedCard(null
                         * == name ? "" : name); }
                         */
                        final List<String> cards = new ArrayList<String>();
                        for (final CardPrinted c : CardDb.instance().getAllUniqueCards()) {
                            cards.add(c.getName());
                        }
                        Collections.sort(cards);

                        // use standard forge's list selection dialog
                        final ListChooser<String> choice = new ListChooser<String>(message, 1, 1, cards);
                        choice.show();
                        // still missing a listener to display the card preview
                        // in the right
                        name = choice.getSelectedValue();
                        if (AllZone.getCardFactory().getCard(CardDb.instance().getCard(name), p).isValid(valid, host.getController(), host)) {
                            host.setNamedCard(choice.getSelectedValue());
                            ok = true;
                        }
                    } else {
                        String chosen = "";
                        if (params.containsKey("AILogic")) {
                            final String logic = params.get("AILogic");
                            if (logic.equals("MostProminentInComputerDeck")) {
                                chosen = CardFactoryUtil.getMostProminentCardName(p.getCardsIn(ZoneType.Library));
                            } else if (logic.equals("MostProminentInHumanDeck")) {
                                chosen = CardFactoryUtil.getMostProminentCardName(p.getOpponent().getCardsIn(ZoneType.Library));
                            }
                        } else {
                            List<Card> list = CardLists.filterControlledBy(GameState.getCardsInGame(), p.getOpponent());
                            list = CardLists.filter(list, Predicates.not(Presets.LANDS));
                            if (!list.isEmpty()) {
                                chosen = list.get(0).getName();
                            }
                        }
                        if (chosen.equals("")) {
                            chosen = "Morphling";
                        }
                        GuiChoose.one("Computer picked: ", new String[]{chosen});
                        host.setNamedCard(chosen);
                        ok = true;
                    }
                }
            }
        }
    }

    // *************************************************************************
    // *************************** ChooseCard **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChooseCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityChooseCard(final AbilityFactory af) {
        class AbilityChooseCard extends AbilityActivated {
            public AbilityChooseCard(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityChooseCard(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 2399435577106102311L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseCardStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseCardCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseCardResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseCardTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abChooseCard = new AbilityChooseCard(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abChooseCard;
    }

    /**
     * <p>
     * createSpellChooseCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.7
     */
    public static SpellAbility createSpellChooseCard(final AbilityFactory af) {
        final SpellAbility spChooseCard = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 1425536663625668893L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseCardStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseCardCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseCardResolve(af, this);
            }

        };
        return spChooseCard;
    }

    /**
     * <p>
     * createDrawbackChooseCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackChooseCard(final AbilityFactory af) {
        class DrawbackChooseCard extends AbilitySub {
            public DrawbackChooseCard(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackChooseCard(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -3255569671897226555L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseCardStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseCardResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryChoose.chooseCardCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseCardTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbChooseCard = new DrawbackChooseCard(af.getHostCard(), af.getAbTgt());

        return dbChooseCard;
    }

    /**
     * <p>
     * chooseCardStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseCardStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

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
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a card.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * chooseCardCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseCardCanPlayAI(final Player ai,final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai.getOpponent())) {
                tgt.addTarget(ai.getOpponent());
            } else {
                return false;
            }
        }
        if (params.containsKey("AILogic")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (params.containsKey("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(params.get("ChoiceZone"));
            }
            List<Card> choices = GameState.getCardsIn(choiceZone);
            if (params.containsKey("Choices")) {
                choices = CardLists.getValidCards(choices, params.get("Choices"), host.getController(), host);
            }
            if (params.containsKey("TargetControls")) {
                choices = CardLists.filterControlledBy(choices, ai.getOpponent());
            }
            if (params.get("AILogic").equals("AtLeast1")) {
                if (choices.size() < 1) {
                    return false;
                }
            } else if (params.get("AILogic").equals("AtLeast2") || params.get("AILogic").equals("BestBlocker")) {
                if (choices.size() < 2) {
                    return false;
                }
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback()) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * chooseCardTriggerAI.
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
    private static boolean chooseCardTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        return chooseCardCanPlayAI(ai, af, sa);
    }

    /**
     * <p>
     * chooseCardResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseCardResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();
        final ArrayList<Card> chosen = new ArrayList<Card>();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
        ZoneType choiceZone = ZoneType.Battlefield;
        if (params.containsKey("ChoiceZone")) {
            choiceZone = ZoneType.smartValueOf(params.get("ChoiceZone"));
        }
        List<Card> choices = GameState.getCardsIn(choiceZone);
        if (params.containsKey("Choices")) {
            choices = CardLists.getValidCards(choices, params.get("Choices"), host.getController(), host);
        }
        if (params.containsKey("TargetControls")) {
            choices = CardLists.filterControlledBy(choices, tgtPlayers.get(0));
        }

        final String numericAmount = params.containsKey("Amount") ? params.get("Amount") : "1";
        final int validAmount = !numericAmount.matches("[0-9][0-9]?")
                ? CardFactoryUtil.xCount(host, host.getSVar(params.get("Amount"))) : Integer.parseInt(numericAmount);

        if (params.containsKey("SunderingTitan")) {
            final List<Card> land = GameState.getLandsInPlay();
            final ArrayList<String> basic = CardUtil.getBasicTypes();

            for (final String type : basic) {
                final List<Card> cl = CardLists.getType(land, type);
                if (cl.size() > 0) {
                    final String prompt = "Choose a" + (type.equals("Island") ? "n " : " ") + type;
                    final Object o = GuiChoose.one(prompt, cl);
                    if (null != o) {
                        final Card c = (Card) o;
                        chosen.add(c);
                    }
                }
            }
            host.setChosenCard(chosen);
            return;
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < validAmount; i++) {
                    if (p.isHuman()) {
                        final String choiceTitle = params.containsKey("ChoiceTitle") ? params.get("ChoiceTitle") : "Choose a card ";
                        final Card o = GuiChoose.oneOrNone(choiceTitle, choices);
                        if (o != null) {
                            chosen.add(o);
                            choices.remove(o);
                        } else {
                            break;
                        }
                    } else { // Computer
                        if (params.containsKey("AILogic") && params.get("AILogic").equals("BestBlocker")) {
                            if (CardLists.filter(choices, Presets.UNTAPPED).isEmpty()) {
                                choices = CardLists.filter(choices, Presets.UNTAPPED);
                            }
                            chosen.add(CardFactoryUtil.getBestCreatureAI(choices));
                        } else {
                            chosen.add(CardFactoryUtil.getBestAI(choices));
                        }
                    }
                }
                host.setChosenCard(chosen);
                if (params.containsKey("RememberChosen")) {
                    for (final Card rem : chosen) {
                        host.addRemembered(rem);
                    }
                }
            }
        }
    }

    // *************************************************************************
    // ************************* ChooseGeneric *********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChooseGeneric.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * 
     * @since 1.2.4
     */
    public static SpellAbility createAbilityChooseGeneric(final AbilityFactory af) {
        class AbilityChooseGeneric extends AbilityActivated {
            public AbilityChooseGeneric(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityChooseGeneric(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -459173435583208151L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseGenericStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseGenericCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseGenericResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseGenericTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abChooseGeneric = new AbilityChooseGeneric(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abChooseGeneric;
    }

    /**
     * <p>
     * createSpellChooseGeneric.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * 
     * @since 1.2.4
     */
    public static SpellAbility createSpellChooseGeneric(final AbilityFactory af) {
        final SpellAbility spChooseGeneric = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4187094641157371974L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseGenericStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseGenericCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseGenericResolve(af, this);
            }

        };
        return spChooseGeneric;
    }

    /**
     * <p>
     * createDrawbackChooseGeneric.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * 
     * @since 1.2.4
     */
    public static SpellAbility createDrawbackChooseGeneric(final AbilityFactory af) {
        class DrawbackChooseGeneric extends AbilitySub {
            public DrawbackChooseGeneric(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackChooseGeneric(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 1586980855969921641L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseGenericStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseGenericResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseGenericTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbChooseGeneric = new DrawbackChooseGeneric(af.getHostCard(), af.getAbTgt());

        return dbChooseGeneric;
    }

    private static String chooseGenericStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        }
        else {
            ArrayList<Player> tgtPlayers;

            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }

            for (final Player p : tgtPlayers) {
                sb.append(p).append(" ");
            }
            sb.append("chooses from a list.");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static boolean chooseGenericCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        if (!params.containsKey("AILogic")) {
            return false;
        }

        return AbilityFactoryChoose.chooseGenericTriggerAI(ai, af, sa, false);
    }

    /**
     * <p>
     * chooseTypeTriggerAI.
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
    private static boolean chooseGenericTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }

        return false;
    }

    private static void chooseGenericResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();
        final BiMap<String, String> choices = HashBiMap.create();
        for (String s : Arrays.asList(params.get("Choices").split(","))) {
            final HashMap<String, String> theseParams = AbilityFactory.getMapParams(host.getSVar(s), host);
            choices.put(s, theseParams.get("ChoiceDescription"));
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (!params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if (tgt != null && !p.canBeTargetedBy(sa)) {
                continue;
            }
            SpellAbility chosenSA = null;
            AbilityFactory afChoice = new AbilityFactory();
            if (p.isHuman()) {
                String choice = GuiChoose.one("Choose one", choices.values());
                chosenSA = afChoice.getAbility(host.getSVar(choices.inverse().get(choice)), host);
            } else { //Computer AI
                chosenSA = afChoice.getAbility(host.getSVar(params.get("Choices").split(",")[0]), host);
            }
            chosenSA.setActivatingPlayer(sa.getSourceCard().getController());
            ((AbilitySub) chosenSA).setParent(sa);
            AbilityFactory.resolve(chosenSA, false);
        }
    }

} // end class AbilityFactoryChoose
