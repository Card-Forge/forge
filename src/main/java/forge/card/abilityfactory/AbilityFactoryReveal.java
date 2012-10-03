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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardCharacteristicName;

import forge.CardListUtil;
import forge.CardUtil;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_Reveal class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryReveal {

    private AbilityFactoryReveal() {
        throw new AssertionError();
    }

    // *************************************************************************
    // ************************* Dig *******************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityDig.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDig(final AbilityFactory af) {
        class AbilityDig extends AbilityActivated {
            public AbilityDig(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityDig(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 4239474096624403497L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.digStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.digCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.digResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.digTriggerAI(af, this, mandatory);
            }

        }
        final SpellAbility abDig = new AbilityDig(af.getHostCard(), af.getAbCost(), af.getAbTgt());
        return abDig;
    }

    /**
     * <p>
     * createSpellDig.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDig(final AbilityFactory af) {
        final SpellAbility spDig = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3389143507816474146L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.digStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.digCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.digResolve(af, this);
            }

        };
        return spDig;
    }

    /**
     * <p>
     * createDrawbackDig.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackDig(final AbilityFactory af) {
        class DrawbackDig extends AbilitySub {
            public DrawbackDig(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackDig(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -3372788479421357024L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.digStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.digResolve(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.digCanPlayAI(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.digTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbDig = new DrawbackDig(af.getHostCard(), af.getAbTgt());

        return dbDig;
    }

    /**
     * <p>
     * digStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String digStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();
        final StringBuilder sb = new StringBuilder();
        final int numToDig = AbilityFactory.calculateAmount(af.getHostCard(), params.get("DigNum"), sa);

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
            return sb.toString();
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        sb.append(host.getController()).append(" looks at the top ").append(numToDig);
        sb.append(" card");
        if (numToDig != 1) {
            sb.append("s");
        }
        sb.append(" of ");
        if (tgtPlayers.contains(host.getController())) {
            sb.append("his or her ");
        } else {
            for (final Player p : tgtPlayers) {
                sb.append(p).append("'s ");
            }
        }
        sb.append("library.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * digCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean digCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        double chance = .4; // 40 percent chance with instant speed stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will
                           // never activate EOT)
        }
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        final Target tgt = sa.getTarget();
        Player libraryOwner = AllZone.getComputerPlayer();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (!AllZone.getHumanPlayer().canBeTargetedBy(sa)) {
                return false;
            } else {
                sa.getTarget().addTarget(AllZone.getHumanPlayer());
            }
            libraryOwner = AllZone.getHumanPlayer();
        }

        // return false if nothing to dig into
        if (libraryOwner.getCardsIn(ZoneType.Library).isEmpty()) {
            return false;
        }

        // Don't use draw abilities before main 2 if possible
        if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !params.containsKey("ActivationPhases")
                && !params.containsKey("DestinationZone")) {
            return false;
        }

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

        if (af.hasSubAbility()) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                return randomReturn && abSub.chkAIDrawback();
            }
        }

        return randomReturn;
    }

    /**
     * <p>
     * digTriggerAI.
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
    private static boolean digTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getComputerPlayer());
        }

        return true;
    }

    /**
     * <p>
     * digResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void digResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();
        final Player player = sa.getActivatingPlayer();
        Player choser = player;
        int numToDig = AbilityFactory.calculateAmount(af.getHostCard(), params.get("DigNum"), sa);
        final ZoneType destZone1 = params.containsKey("DestinationZone") ? ZoneType.smartValueOf(params.get("DestinationZone"))
                : ZoneType.Hand;
        final ZoneType destZone2 = params.containsKey("DestinationZone2") ? ZoneType.smartValueOf(params
                .get("DestinationZone2")) : ZoneType.Library;

        final int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                .get("LibraryPosition")) : -1;
        int destZone1ChangeNum = 1;
        final boolean mitosis = params.containsKey("Mitosis");
        String changeValid = params.containsKey("ChangeValid") ? params.get("ChangeValid") : "";
        //andOrValid is for cards with "creature card and/or a land card"
        String andOrValid = params.containsKey("AndOrValid") ? params.get("AndOrValid") : "";
        final boolean anyNumber = params.containsKey("AnyNumber");

        final int libraryPosition2 = params.containsKey("LibraryPosition2") ? Integer.parseInt(params
                .get("LibraryPosition2")) : -1;
        final boolean optional = params.containsKey("Optional");
        final boolean noMove = params.containsKey("NoMove");
        boolean changeAll = false;
        final ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }

        if (params.containsKey("ChangeNum")) {
            if (params.get("ChangeNum").equalsIgnoreCase("All")) {
                changeAll = true;
            } else {
                destZone1ChangeNum = Integer.parseInt(params.get("ChangeNum"));
            }
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (params.containsKey("Choser")) {
            final ArrayList<Player> chosers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                    params.get("Choser"), sa);
            if (!chosers.isEmpty()) {
                choser = chosers.get(0);
            }
        }

        for (final Player p : tgtPlayers) {
            if (tgt != null && !p.canBeTargetedBy(sa)) {
                continue;
            }
            final List<Card> top = new ArrayList<Card>();
            List<Card> valid = new ArrayList<Card>();
            final List<Card> rest = new ArrayList<Card>();
            final PlayerZone library = p.getZone(ZoneType.Library);

            numToDig = Math.min(numToDig, library.size());
            for (int i = 0; i < numToDig; i++) {
                top.add(library.get(i));
            }

            if (top.size() > 0) {
                final Card dummy = new Card();
                dummy.setName("[No valid cards]");

                if (params.containsKey("Reveal")) {
                    GuiChoose.one("Revealing cards from library", top);
                    // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    // - for when it exists
                } else if (params.containsKey("RevealOptional")) {
                    String question = "Reveal: ";
                    for (final Card c : top) {
                        question += c + " ";
                    }
                    if (p.isHuman() && GameActionUtil.showYesNoDialog(host, question)) {
                        GuiChoose.one(host + "Revealing cards from library", top);
                        // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    } else if (p.isComputer() && (top.get(0).isInstant() || top.get(0).isSorcery())) {
                        GuiChoose.one(host + "Revealing cards from library", top);
                    }
                } else if (params.containsKey("RevealValid")) {
                    final String revealValid = params.get("RevealValid");
                    final List<Card> toReveal = CardListUtil.getValidCards(top, revealValid, host.getController(), host);
                    if (!toReveal.isEmpty()) {
                        GuiChoose.one("Revealing cards from library", toReveal);
                        if (params.containsKey("RememberRevealed")) {
                            for (final Card one : toReveal) {
                                host.addRemembered(one);
                            }
                        }
                    }
                    // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    // - for when it exists
                } else if (choser.isHuman()) {
                    // show the user the revealed cards
                    GuiChoose.one("Looking at cards from library", top);
                }

                if ((params.containsKey("RememberRevealed")) && !params.containsKey("RevealValid")) {
                    for (final Card one : top) {
                        host.addRemembered(one);
                    }
                }

                if (!noMove) {
                    List<Card> movedCards = new ArrayList<Card>();
                    List<Card> andOrCards = new ArrayList<Card>();
                    for (final Card c : top) {
                        rest.add(c);
                    }
                    if (mitosis) {
                        valid = AbilityFactoryReveal.sharesNameWithCardOnBattlefield(top);
                    } else if (!changeValid.equals("")) {
                        if (changeValid.contains("ChosenType")) {
                            changeValid = changeValid.replace("ChosenType", host.getChosenType());
                        }
                        valid = CardListUtil.getValidCards(top, changeValid.split(","), host.getController(), host);
                        if (!andOrValid.equals("")) {
                            andOrCards = CardListUtil.getValidCards(top, andOrValid.split(","), host.getController(), host);
                            andOrCards.removeAll(valid);
                            valid.addAll(andOrCards);
                        }
                        if (valid.isEmpty() && choser.isHuman()) {
                            valid.add(dummy);
                        }
                    } else {
                        valid = top;
                    }

                    if (changeAll) {
                        movedCards.addAll(valid);
                    } else if (params.containsKey("RandomChange")) {
                        int numChanging = Math.min(destZone1ChangeNum, valid.size());
                        movedCards = CardListUtil.getRandomSubList(valid, numChanging);
                    } else {
                        int j = 0;
                        if (choser.isHuman()) {
                            while ((j < destZone1ChangeNum) || (anyNumber && (j < numToDig))) {
                                // let user get choice
                                if (valid.isEmpty()) {
                                    break;
                                }
                                Card chosen = null;
                                String prompt = "Choose a card to put into the ";
                                if (destZone1.equals(ZoneType.Library) && (libraryPosition == -1)) {
                                    prompt = "Chose a card to put on the bottom of the ";
                                }
                                if (destZone1.equals(ZoneType.Library) && (libraryPosition == 0)) {
                                    prompt = "Chose a card to put on top of the ";
                                }
                                if (anyNumber || optional) {
                                    chosen = GuiChoose.oneOrNone(prompt + destZone1, valid);
                                } else {
                                    chosen = GuiChoose.one(prompt + destZone1, valid);
                                }
                                if ((chosen == null) || chosen.getName().equals("[No valid cards]")) {
                                    break;
                                }
                                movedCards.add(chosen);
                                valid.remove(chosen);
                                if (!andOrValid.equals("")) {
                                    andOrCards.remove(chosen);
                                    if (!chosen.isValid(andOrValid.split(","), host.getController(), host)) {
                                        valid = new ArrayList<Card>(andOrCards);
                                    } else if (!chosen.isValid(changeValid.split(","), host.getController(), host)) {
                                        valid.removeAll(andOrCards);
                                    }
                                }
                                // Singletons.getModel().getGameAction().revealToComputer()
                                // - for when this exists
                                j++;
                            }
                        } // human
                        else { // computer
                            int changeNum = Math.min(destZone1ChangeNum, valid.size());
                            if (anyNumber) {
                                changeNum = valid.size(); // always take all
                            }
                            for (j = 0; j < changeNum; j++) {
                                Card chosen = CardFactoryUtil.getBestAI(valid);
                                if (sa.getActivatingPlayer().isHuman() && p.isHuman()) {
                                    chosen = CardFactoryUtil.getWorstAI(valid);
                                }
                                if (chosen == null) {
                                    break;
                                }
                                if (changeValid.length() > 0) {
                                    GuiChoose.one("Computer picked: ", chosen);
                                }
                                movedCards.add(chosen);
                                valid.remove(chosen);
                                if (!andOrValid.equals("")) {
                                    andOrCards.remove(chosen);
                                    if (!chosen.isValid(andOrValid.split(","), host.getController(), host)) {
                                        valid = andOrCards;
                                    } else if (!chosen.isValid(changeValid.split(","), host.getController(), host)) {
                                        valid.removeAll(andOrCards);
                                    }
                                }
                            }
                        }
                    }
                    if (params.containsKey("ForgetOtherRemembered")) {
                        host.clearRemembered();
                    }
                    Collections.reverse(movedCards);
                    for (Card c : movedCards) {
                        if (c.equals(dummy)) {
                            continue;
                        }
                        final PlayerZone zone = c.getOwner().getZone(destZone1);
                        if (zone.is(ZoneType.Library)) {
                            Singletons.getModel().getGameAction().moveToLibrary(c, libraryPosition);
                        } else {
                            c = Singletons.getModel().getGameAction().moveTo(zone, c);
                            if (destZone1.equals(ZoneType.Battlefield)) {
                                for (final String kw : keywords) {
                                    c.addExtrinsicKeyword(kw);
                                }
                                if (params.containsKey("Tapped")) {
                                    c.setTapped(true);
                                }
                            }
                            if (params.containsKey("ExileFaceDown")) {
                                c.setState(CardCharacteristicName.FaceDown);
                            }
                            if (params.containsKey("Imprint")) {
                                host.addImprinted(c);
                            }
                        }
                        if (params.containsKey("ForgetOtherRemembered")) {
                            host.clearRemembered();
                        }
                        if (params.containsKey("RememberChanged")) {
                            host.addRemembered(c);
                        }
                        rest.remove(c);
                    }
                    if (rest.contains(dummy)) {
                        rest.remove(dummy);
                    }

                    // now, move the rest to destZone2
                    if (destZone2.equals(ZoneType.Library)) {
                        if (choser.isHuman()) {
                            // put them in any order
                            while (rest.size() > 0) {
                                Card chosen;
                                if (rest.size() > 1) {
                                    String prompt = "Put the rest on top of the library in any order";
                                    if (libraryPosition2 == -1) {
                                        prompt = "Put the rest on the bottom of the library in any order";
                                    }
                                    chosen = GuiChoose.one(prompt, rest);
                                } else {
                                    chosen = rest.get(0);
                                }
                                Singletons.getModel().getGameAction().moveToLibrary(chosen, libraryPosition2);
                                rest.remove(chosen);
                            }
                        } else { // Computer
                            for (int i = 0; i < rest.size(); i++) {
                                Singletons.getModel().getGameAction().moveToLibrary(rest.get(i), libraryPosition2);
                            }
                        }
                    } else {
                        // just move them randomly
                        for (int i = 0; i < rest.size(); i++) {
                            Card c = rest.get(i);
                            final PlayerZone toZone = c.getOwner().getZone(destZone2);
                            c = Singletons.getModel().getGameAction().moveTo(toZone, c);
                            if (destZone2.equals(ZoneType.Battlefield) && !keywords.isEmpty()) {
                                for (final String kw : keywords) {
                                    c.addExtrinsicKeyword(kw);
                                }
                            }
                        }

                    }
                }
            }
        } // end foreach player
    } // end resolve

    // returns a List<Card> that is a subset of list with cards that share a name
    // with a permanent on the battlefield
    /**
     * <p>
     * sharesNameWithCardOnBattlefield.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> sharesNameWithCardOnBattlefield(final List<Card> list) {
        final List<Card> toReturn = new ArrayList<Card>();
        final List<Card> play = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        for (final Card c : list) {
            for (final Card p : play) {
                if (p.getName().equals(c.getName()) && !toReturn.contains(c)) {
                    toReturn.add(c);
                }
            }
        }
        return toReturn;
    }

    // **********************************************************************
    // ******************************* DigUntil ***************************
    // **********************************************************************

    /**
     * <p>
     * createAbilityDigUntil.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDigUntil(final AbilityFactory af) {

        class AbilityDigUntil extends AbilityActivated {
            public AbilityDigUntil(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityDigUntil(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 4239474096624403497L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.digUntilStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.digUntilCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.digUntilResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.digUntilTriggerAI(af, this, mandatory);
            }

        }
        final SpellAbility abDig = new AbilityDigUntil(af.getHostCard(), af.getAbCost(), af.getAbTgt());
        return abDig;
    }

    /**
     * <p>
     * createSpellDigUntil.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDigUntil(final AbilityFactory af) {
        final SpellAbility spDig = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3389143507816474146L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.digUntilStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.digUntilCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.digUntilResolve(af, this);
            }

        };
        return spDig;
    }

    /**
     * <p>
     * createDrawbackDigUntil.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackDigUntil(final AbilityFactory af) {
        class DrawbackDigUntil extends AbilitySub {
            public DrawbackDigUntil(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackDigUntil(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -3372788479421357024L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.digUntilStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.digUntilResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.digUntilTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbDig = new DrawbackDigUntil(af.getHostCard(), af.getAbTgt());

        return dbDig;
    }

    /**
     * <p>
     * digUntilStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String digUntilStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();

        String desc = "Card";
        if (params.containsKey("ValidDescription")) {
            desc = params.get("ValidDescription");
        }

        int untilAmount = 1;
        if (params.containsKey("Amount")) {
            untilAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), sa);
        }

        if (!(sa instanceof AbilitySub)) {
            sb.append(host).append(" - ");
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

        for (final Player pl : tgtPlayers) {
            sb.append(pl).append(" ");
        }

        sb.append("reveals cards from his or her library until revealing ");
        sb.append(untilAmount).append(" ").append(desc).append(" card");
        if (untilAmount != 1) {
            sb.append("s");
        }
        sb.append(". Put ");

        final ZoneType found = ZoneType.smartValueOf(params.get("FoundDestination"));
        final ZoneType revealed = ZoneType.smartValueOf(params.get("RevealedDestination"));
        if (found != null) {

            sb.append(untilAmount > 1 ? "those cards" : "that card");
            sb.append(" ");

            if (found.equals(ZoneType.Hand)) {
                sb.append("into his or her hand ");
            }

            if (revealed.equals(ZoneType.Graveyard)) {
                sb.append("and all other cards into his or her graveyard.");
            }
            if (revealed.equals(ZoneType.Exile)) {
                sb.append("and exile all other cards revealed this way.");
            }
        } else {
            if (revealed.equals(ZoneType.Hand)) {
                sb.append("all cards revealed this way into his or her hand");
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
     * digUntilCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean digUntilCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

        double chance = .4; // 40 percent chance with instant speed stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will
                           // never activate EOT)
        }
        final Random r = MyRandom.getRandom();
        final boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        final Target tgt = sa.getTarget();
        Player libraryOwner = AllZone.getComputerPlayer();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (!AllZone.getHumanPlayer().canBeTargetedBy(sa)) {
                return false;
            } else {
                sa.getTarget().addTarget(AllZone.getHumanPlayer());
            }
            libraryOwner = AllZone.getHumanPlayer();
        }

        // return false if nothing to dig into
        if (libraryOwner.getCardsIn(ZoneType.Library).isEmpty()) {
            return false;
        }

        if (af.hasSubAbility()) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                return randomReturn && abSub.chkAIDrawback();
            }
        }

        return randomReturn;
    }

    /**
     * <p>
     * digUntilTriggerAI.
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
    private static boolean digUntilTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getComputerPlayer());
        }

        return true;
    }

    /**
     * <p>
     * digUntilResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void digUntilResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();

        String type = "Card";
        if (params.containsKey("Valid")) {
            type = params.get("Valid");
        }

        int untilAmount = 1;
        if (params.containsKey("Amount")) {
            untilAmount = AbilityFactory.calculateAmount(host, params.get("Amount"), sa);
        }

        Integer maxRevealed = null;
        if (params.containsKey("MaxRevealed")) {
            maxRevealed = AbilityFactory.calculateAmount(host, params.get("MaxRevealed"), sa);
        }

        final boolean remember = params.containsKey("RememberFound");

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(host, params.get("Defined"), sa);
        }

        final ZoneType foundDest = ZoneType.smartValueOf(params.get("FoundDestination"));
        final int foundLibPos = AbilityFactory.calculateAmount(host, params.get("FoundLibraryPosition"), sa);
        final ZoneType revealedDest = ZoneType.smartValueOf(params.get("RevealedDestination"));
        final int revealedLibPos = AbilityFactory.calculateAmount(host, params.get("RevealedLibraryPosition"), sa);

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> found = new ArrayList<Card>();
                final List<Card> revealed = new ArrayList<Card>();

                final PlayerZone library = p.getZone(ZoneType.Library);

                final int maxToDig = maxRevealed != null ? maxRevealed : library.size();

                for (int i = 0; i < maxToDig; i++) {
                    final Card c = library.get(i);
                    revealed.add(c);
                    if (c.isValid(type, sa.getActivatingPlayer(), host)) {
                        found.add(c);
                        if (remember) {
                            host.addRemembered(c);
                        }
                        if (found.size() == untilAmount) {
                            break;
                        }
                    }
                }

                if (revealed.size() > 0) {
                    GuiChoose.one(p + " revealed: ", revealed);
                }

                // TODO Allow Human to choose the order
                if (foundDest != null) {
                    final Iterator<Card> itr = found.iterator();
                    while (itr.hasNext()) {
                        final Card c = itr.next();
                        if (params.containsKey("GainControl") && foundDest.equals(ZoneType.Battlefield)) {
                            c.addController(af.getHostCard());
                            Singletons.getModel().getGameAction().moveTo(c.getController().getZone(foundDest), c);
                        } else {
                            Singletons.getModel().getGameAction().moveTo(foundDest, c, foundLibPos);
                        }
                        revealed.remove(c);
                    }
                }

                if (params.containsKey("RememberRevealed")) {
                    for (final Card c : revealed) {
                        host.addRemembered(c);
                    }
                }

                final Iterator<Card> itr = revealed.iterator();
                while (itr.hasNext()) {
                    final Card c = itr.next();
                    Singletons.getModel().getGameAction().moveTo(revealedDest, c, revealedLibPos);
                }

                if (params.containsKey("Shuffle")) {
                    p.shuffle();
                }
            } // end foreach player
        }
    } // end resolve

    // **********************************************************************
    // ******************************* RevealHand ***************************
    // **********************************************************************

    /**
     * <p>
     * createAbilityRevealHand.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityRevealHand(final AbilityFactory af) {
        class AbilityRevealHand extends AbilityActivated {
            public AbilityRevealHand(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityRevealHand(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 2785654059206102004L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.revealHandStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.revealHandCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.revealHandResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.revealHandTrigger(af, this, mandatory);
            }
        }
        final SpellAbility abRevealHand = new AbilityRevealHand(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abRevealHand;
    }

    /**
     * <p>
     * createSpellRevealHand.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellRevealHand(final AbilityFactory af) {
        final SpellAbility spRevealHand = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -668943560971904791L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.revealHandStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.revealHandCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.revealHandResolve(af, this);
            }

        };
        return spRevealHand;
    }

    /**
     * <p>
     * createDrawbackRevealHand.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackRevealHand(final AbilityFactory af) {
        class DrawbackRevealHand extends AbilitySub {
            public DrawbackRevealHand(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackRevealHand(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -6079668770576878801L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryReveal.revealHandStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.revealHandResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryReveal.revealHandTargetAI(af, this, false, false);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.revealHandTrigger(af, this, mandatory);
            }
        }
        final SpellAbility dbRevealHand = new DrawbackRevealHand(af.getHostCard(), af.getAbTgt());

        return dbRevealHand;
    }

    /**
     * <p>
     * revealHandStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String revealHandStackDescription(final AbilityFactory af, final SpellAbility sa) {
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

        sb.append(sa.getActivatingPlayer()).append(" looks at ");

        if (tgtPlayers.size() > 0) {
            for (final Player p : tgtPlayers) {
                sb.append(p.toString()).append("'s ");
            }
        } else {
            sb.append("Error - no target players for RevealHand. ");
        }
        sb.append("hand.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * revealHandCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean revealHandCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 4, null)) {
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

        final boolean bFlag = AbilityFactoryReveal.revealHandTargetAI(af, sa, true, false);

        if (!bFlag) {
            return false;
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

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
     * revealHandTargetAI.
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
    private static boolean revealHandTargetAI(final AbilityFactory af, final SpellAbility sa, final boolean primarySA,
            final boolean mandatory) {
        final Target tgt = sa.getTarget();

        final int humanHandSize = AllZone.getHumanPlayer().getCardsIn(ZoneType.Hand).size();

        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            final boolean canTgtHuman = AllZone.getHumanPlayer().canBeTargetedBy(sa);

            if (!canTgtHuman || (humanHandSize == 0)) {
                return false;
            } else {
                tgt.addTarget(AllZone.getHumanPlayer());
            }
        } else {
            // if it's just defined, no big deal
        }

        return true;
    } // revealHandTargetAI()

    /**
     * <p>
     * revealHandTrigger.
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
    private static boolean revealHandTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (!AbilityFactoryReveal.revealHandTargetAI(af, sa, false, mandatory)) {
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
     * revealHandResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void revealHandResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> hand = p.getCardsIn(ZoneType.Hand);
                if (sa.getActivatingPlayer().isHuman()) {
                    if (hand.size() > 0) {
                        GuiChoose.one(p + "'s hand", hand);
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append(p).append("'s hand is empty!");
                        javax.swing.JOptionPane.showMessageDialog(null, sb.toString(), p + "'s hand",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    // reveal to Computer (when computer can keep track of seen
                    // cards...)
                }
                if (params.containsKey("RememberRevealed")) {
                    for (final Card c : hand) {
                        host.addRemembered(c);
                    }
                }
            }
        }
    }

    // **********************************************************************
    // ******************************* SCRY *********************************
    // **********************************************************************

    /**
     * <p>
     * createAbilityScry.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityScry(final AbilityFactory af) {
        class AbilityScry extends AbilityActivated {
            public AbilityScry(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityScry(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 2631175859655699419L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.scryStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.scryCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.scryResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.scryTriggerAI(af, this);
            }
        }
        final SpellAbility abScry = new AbilityScry(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abScry;
    }

    /**
     * <p>
     * createSpellScry.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellScry(final AbilityFactory af) {
        final SpellAbility spScry = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6273876397392154403L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.scryStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.scryCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.scryResolve(af, this);
            }

        };
        return spScry;
    }

    /**
     * <p>
     * createDrawbackScry.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackScry(final AbilityFactory af) {
        class DrawbackScry extends AbilitySub {
            public DrawbackScry(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackScry(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 7763043327497404630L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryReveal.scryStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.scryResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryReveal.scryTargetAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.scryTriggerAI(af, this);
            }
        }
        final SpellAbility dbScry = new DrawbackScry(af.getHostCard(), af.getAbTgt());

        return dbScry;
    }

    /**
     * <p>
     * scryResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void scryResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        int num = 1;
        if (params.containsKey("ScryNum")) {
            num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);
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
                p.scry(num);
            }
        }
    }

    /**
     * <p>
     * scryTargetAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean scryTargetAI(final AbilityFactory af, final SpellAbility sa) {
        final Target tgt = sa.getTarget();

        if (tgt != null) { // It doesn't appear that Scry ever targets
            // ability is targeted
            tgt.resetTargets();

            tgt.addTarget(AllZone.getComputerPlayer());
        }

        return true;
    } // scryTargetAI()

    /**
     * <p>
     * scryTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean scryTriggerAI(final AbilityFactory af, final SpellAbility sa) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        return AbilityFactoryReveal.scryTargetAI(af, sa);
    } // scryTargetAI()

    /**
     * <p>
     * scryStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String scryStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
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
            sb.append(p.toString()).append(" ");
        }

        int num = 1;
        if (params.containsKey("ScryNum")) {
            num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);
        }

        sb.append("scrys (").append(num).append(").");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * scryCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean scryCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // Card source = sa.getSourceCard();

        double chance = .4; // 40 percent chance of milling with instant speed
                            // stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will
                           // never activate EOT)
        }
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

        if (af.hasSubAbility()) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                return randomReturn && abSub.chkAIDrawback();
            }
        }
        return randomReturn;
    }

    // **********************************************************************
    // *********************** REARRANGETOPOFLIBRARY ************************
    // **********************************************************************

    /**
     * <p>
     * createRearrangeTopOfLibraryAbility.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityRearrangeTopOfLibrary(final AbilityFactory af) {
        class AbilityRearrangeTopOfLibrary extends AbilityActivated {
            public AbilityRearrangeTopOfLibrary(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityRearrangeTopOfLibrary(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -548494891203983219L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.rearrangeTopOfLibraryStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.rearrangeTopOfLibraryTrigger(af, this, mandatory);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.rearrangeTopOfLibraryResolve(af, this);
            }
        }
        final SpellAbility rtolAbility = new AbilityRearrangeTopOfLibrary(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return rtolAbility;
    }

    /**
     * <p>
     * createRearrangeTopOfLibrarySpell.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellRearrangeTopOfLibrary(final AbilityFactory af) {
        final SpellAbility rtolSpell = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6977502611509431864L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.rearrangeTopOfLibraryStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.rearrangeTopOfLibraryTrigger(af, this, mandatory);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.rearrangeTopOfLibraryResolve(af, this);
            }

        };

        return rtolSpell;
    }

    /**
     * <p>
     * createRearrangeTopOfLibraryDrawback.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackRearrangeTopOfLibrary(final AbilityFactory af) {
        class DrawbackRearrangeTopOfLibrary extends AbilitySub {
            public DrawbackRearrangeTopOfLibrary(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackRearrangeTopOfLibrary(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -777856059960750319L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryReveal.rearrangeTopOfLibraryStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.rearrangeTopOfLibraryResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return false;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.rearrangeTopOfLibraryTrigger(af, this, mandatory);
            }
        }
        final SpellAbility dbDraw = new DrawbackRearrangeTopOfLibrary(af.getHostCard(), af.getAbTgt());

        return dbDraw;
    }

    /**
     * <p>
     * rearrangeTopOfLibraryStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String rearrangeTopOfLibraryStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        int numCards = 0;
        ArrayList<Player> tgtPlayers;
        boolean shuffle = false;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        numCards = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumCards"), sa);
        shuffle = params.containsKey("MayShuffle");

        final StringBuilder ret = new StringBuilder();
        if (!(sa instanceof AbilitySub)) {
            ret.append(af.getHostCard().getName());
            ret.append(" - ");
        }
        ret.append("Look at the top ");
        ret.append(numCards);
        ret.append(" cards of ");
        for (final Player p : tgtPlayers) {
            ret.append(p.getName());
            ret.append("s");
            ret.append(" & ");
        }
        ret.delete(ret.length() - 3, ret.length());

        ret.append(" library. Then put them back in any order.");

        if (shuffle) {
            ret.append("You may have ");
            if (tgtPlayers.size() > 1) {
                ret.append("those");
            } else {
                ret.append("that");
            }

            ret.append(" player shuffle his or her library.");
        }

        return ret.toString();
    }

    /**
     * <p>
     * rearrangeTopOfLibraryTrigger.
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
    private static boolean rearrangeTopOfLibraryTrigger(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {

        final Target tgt = sa.getTarget();

        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            final boolean canTgtHuman = AllZone.getHumanPlayer().canBeTargetedBy(sa);

            if (!canTgtHuman) {
                return false;
            } else {
                tgt.addTarget(AllZone.getHumanPlayer());
            }
        } else {
            // if it's just defined, no big deal
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return false;
    }

    /**
     * <p>
     * rearrangeTopOfLibraryResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void rearrangeTopOfLibraryResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        int numCards = 0;
        ArrayList<Player> tgtPlayers = new ArrayList<Player>();
        boolean shuffle = false;

        if (sa.getActivatingPlayer().isHuman()) {
            final Target tgt = sa.getTarget();
            if ((tgt != null) && !params.containsKey("Defined")) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }

            numCards = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumCards"), sa);
            shuffle = params.containsKey("MayShuffle");

            for (final Player p : tgtPlayers) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    AbilityFactoryReveal.rearrangeTopOfLibrary(af.getHostCard(), p, numCards, shuffle);
                }
            }
        }
    }

    /**
     * use this when Human needs to rearrange the top X cards in a player's
     * library. You may also specify a shuffle when done
     * 
     * @param src
     *            the source card
     * @param player
     *            the player to target
     * @param numCards
     *            the number of cards from the top to rearrange
     * @param mayshuffle
     *            a boolean.
     */
    private static void rearrangeTopOfLibrary(final Card src, final Player player, final int numCards,
            final boolean mayshuffle) {
        final PlayerZone lib = player.getZone(ZoneType.Library);
        int maxCards = lib.size();
        // If library is smaller than N, only show that many cards
        maxCards = Math.min(maxCards, numCards);
        if (maxCards == 0) {
            return;
        }
        final List<Card> topCards = new ArrayList<Card>();
        // show top n cards:
        for (int j = 0; j < maxCards; j++) {
            topCards.add(lib.get(j));
        }

        List<Object> orderedCards = GuiChoose.getOrderChoices("Select order to Rearrange", "Top of Library", 0, topCards.toArray(), null);
        for (int i = maxCards - 1; i >= 0; i--) {
            Card next = (Card) orderedCards.get(i);
            Singletons.getModel().getGameAction().moveToLibrary(next, 0);
        }
        if (mayshuffle) {
            if (GameActionUtil.showYesNoDialog(src, "Do you want to shuffle the library?")) {
                player.shuffle();
            }
        }
    }

    // **********************************************************************
    // ******************************* Reveal *******************************
    // **********************************************************************

    /**
     * <p>
     * createAbilityReveal.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityReveal(final AbilityFactory af) {
        class AbilityReveal extends AbilityActivated {
            public AbilityReveal(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityReveal(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -4417404703197532765L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.revealStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.revealCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.revealResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.revealTrigger(af, this, mandatory);
            }
        }
        final SpellAbility abReveal = new AbilityReveal(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abReveal;
    }

    /**
     * <p>
     * createSpellReveal.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellReveal(final AbilityFactory af) {
        final SpellAbility spReveal = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -9015033247472453902L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryReveal.revealStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryReveal.revealCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.revealResolve(af, this);
            }

        };
        return spReveal;
    }

    /**
     * <p>
     * createDrawbackReveal.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackReveal(final AbilityFactory af) {
        class DrawbackReveal extends AbilitySub {
            public DrawbackReveal(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackReveal(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -8059731932417441449L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryReveal.revealStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryReveal.revealResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                // reuse code from RevealHand
                return AbilityFactoryReveal.revealHandTargetAI(af, this, false, false);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryReveal.revealTrigger(af, this, mandatory);
            }
        }
        final SpellAbility dbReveal = new DrawbackReveal(af.getHostCard(), af.getAbTgt());

        return dbReveal;
    }

    /**
     * <p>
     * revealStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String revealStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
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
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtPlayers.size() > 0) {
            sb.append(tgtPlayers.get(0)).append(" reveals ");
            if (params.containsKey("AnyNumber")) {
                sb.append("any number of cards ");
            } else {
                sb.append("a card ");
            }
            if (params.containsKey("Random")) {
                sb.append("at random ");
            }
            sb.append("from his or her hand.");
        } else {
            sb.append("Error - no target players for RevealHand. ");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * revealCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean revealCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 4, null)) {
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

        // we can reuse this function here...
        final boolean bFlag = AbilityFactoryReveal.revealHandTargetAI(af, sa, true, false);

        if (!bFlag) {
            return false;
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

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
     * revealTrigger.
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
    private static boolean revealTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (!AbilityFactoryReveal.revealHandTargetAI(af, sa, false, mandatory)) {
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
     * revealResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void revealResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();
        final boolean anyNumber = params.containsKey("AnyNumber");

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> handChoices = p.getCardsIn(ZoneType.Hand);
                if (handChoices.size() > 0) {
                    final List<Card> revealed = new ArrayList<Card>();
                    if (params.containsKey("Random")) {
                        revealed.add(CardUtil.getRandom(handChoices));
                        GuiChoose.oneOrNone("Revealed card(s)", revealed);
                    } else {
                        List<Card> valid = new ArrayList<Card>(handChoices);
                        int max = 1;
                        if (params.containsKey("RevealValid")) {
                            valid = CardListUtil.getValidCards(valid, params.get("RevealValid"), p, host);
                        }
                        if (params.containsKey("AnyNumber")) {
                            max = valid.size();
                        }
                        revealed.addAll(AbilityFactoryReveal.getRevealedList(sa.getActivatingPlayer(), valid, max, anyNumber));
                        if (sa.getActivatingPlayer().isComputer()) {
                            GuiChoose.oneOrNone("Revealed card(s)", revealed);
                        }
                    }

                    if (params.containsKey("RememberRevealed")) {
                        for (final Card rem : revealed) {
                            host.addRemembered(rem);
                        }
                    }

                }
            }
        }
    }

    /**
     * Gets the revealed list.
     *
     * @param player the player
     * @param valid the valid
     * @param max the max
     * @param anyNumber a boolean
     * @return the revealed list
     */
    public static List<Card> getRevealedList(final Player player, final List<Card> valid, final int max, boolean anyNumber) {
        final List<Card> chosen = new ArrayList<Card>();
        final int validamount = Math.min(valid.size(), max);

        if (anyNumber && player.isHuman() && validamount > 0) {
            final List<Object> selection = GuiChoose.getOrderChoices("Choose Which Cards to Reveal", "Revealed", -1, valid.toArray(), null);
            for (final Object o : selection) {
                if (o != null && o instanceof Card) {
                    chosen.add((Card) o);
                }
            }
        } else {
            for (int i = 0; i < validamount; i++) {
                if (player.isHuman()) {
                    final Card o = GuiChoose.one("Choose card(s) to reveal", valid);
                    if (o != null) {
                        chosen.add(o);
                        valid.remove(o);
                    } else {
                        break;
                    }
                } else { // Computer
                    chosen.add(valid.get(0));
                    valid.remove(valid.get(0));
                }
            }
        }
        return chosen;
    }

} // end class AbilityFactory_Reveal
