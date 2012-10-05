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

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;

import forge.CardLists;
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
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_Sacrifice class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactorySacrifice {
    // **************************************************************
    // *************************** Sacrifice ***********************
    // **************************************************************

    /**
     * <p>
     * createAbilitySacrifice.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilitySacrifice(final AbilityFactory af) {
        class AbilitySacrifice extends AbilityActivated {
            public AbilitySacrifice(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilitySacrifice(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -1933592438783630254L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactorySacrifice.sacrificeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactorySacrifice.sacrificeResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactorySacrifice.sacrificeDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactorySacrifice.sacrificeTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abSacrifice = new AbilitySacrifice(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abSacrifice;
    }

    /**
     * <p>
     * createSpellSacrifice.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellSacrifice(final AbilityFactory af) {
        final SpellAbility spSacrifice = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactorySacrifice.sacrificeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactorySacrifice.sacrificeResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactorySacrifice.sacrificeDescription(af, this);
            }
        };
        return spSacrifice;
    }

    /**
     * <p>
     * createDrawbackSacrifice.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackSacrifice(final AbilityFactory af) {
        class DrawbackSacrifice extends AbilitySub {
            public DrawbackSacrifice(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackSacrifice(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public void resolve() {
                AbilityFactorySacrifice.sacrificeResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactorySacrifice.sacrificePlayDrawbackAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactorySacrifice.sacrificeDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactorySacrifice.sacrificeTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbSacrifice = new DrawbackSacrifice(af.getHostCard(), af.getAbTgt());

        return dbSacrifice;
    }

    /**
     * <p>
     * sacrificeDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String sacrificeDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        }

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        final Target tgt = sa.getTarget();
        ArrayList<Player> tgts;
        if (tgt != null) {
            tgts = tgt.getTargetPlayers();
        } else {
            tgts = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        String valid = params.get("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String num = params.get("Amount");
        num = (num == null) ? "1" : num;
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), num, sa);

        if (valid.equals("Self")) {
            sb.append("Sacrifice ").append(sa.getSourceCard().toString());
        } else if (valid.equals("Card.AttachedBy")) {
            final Card toSac = sa.getSourceCard().getEnchantingCard();
            sb.append(toSac.getController()).append(" sacrifices ").append(toSac).append(".");
        } else {
            for (final Player p : tgts) {
                sb.append(p.getName()).append(" ");
            }

            String msg = params.get("SacMessage");
            if (msg == null) {
                msg = valid;
            }

            if (params.containsKey("Destroy")) {
                sb.append("Destroys ");
            } else {
                sb.append("Sacrifices ");
            }
            sb.append(amount).append(" ").append(msg).append(".");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * sacrificeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean sacrificeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

        final HashMap<String, String> params = af.getMapParams();
        boolean chance = AbilityFactorySacrifice.sacrificeTgtAI(af, sa);

        // Some additional checks based on what is being sacrificed, and who is
        // sacrificing
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final String valid = params.get("SacValid");
            String num = params.get("Amount");
            num = (num == null) ? "1" : num;
            final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), num, sa);

            List<Card> list = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
            list = CardLists.getValidCards(list, valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            if (list.size() == 0) {
                return false;
            }

            final Card source = sa.getSourceCard();
            if (num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int xPay = Math.min(ComputerUtil.determineLeftoverMana(sa), amount);
                source.setSVar("PayX", Integer.toString(xPay));
            }

            final int half = (amount / 2) + (amount % 2); // Half of amount
                                                          // rounded up

            // If the Human has at least half rounded up of the amount to be
            // sacrificed, cast the spell
            if (list.size() < half) {
                return false;
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
     * sacrificePlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean sacrificePlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance = AbilityFactorySacrifice.sacrificeTgtAI(af, sa);

        // TODO: restrict the subAbility a bit

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * sacrificeTriggerAI.
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
    public static boolean sacrificeTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        // AI should only activate this during Human's turn
        boolean chance = AbilityFactorySacrifice.sacrificeTgtAI(af, sa);

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with not
        // mandatory as part of the checks to cast something

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance || mandatory;
    }

    /**
     * <p>
     * sacrificeTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean sacrificeTgtAI(final AbilityFactory af, final SpellAbility sa) {

        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();
        final Target tgt = sa.getTarget();

        if (tgt != null) {
            tgt.resetTargets();
            if (AllZone.getHumanPlayer().canBeTargetedBy(sa)) {
                tgt.addTarget(AllZone.getHumanPlayer());
                return true;
            } else {
                return false;
            }
        }

        final String defined = params.get("Defined");
        if (defined == null) {
            // Self Sacrifice.
        } else if (defined.equals("Each")
                || (defined.equals("Opponent") && !sa.isTrigger())) {
            // If Sacrifice hits both players:
            // Only cast it if Human has the full amount of valid
            // Only cast it if AI doesn't have the full amount of Valid
            // TODO: Cast if the type is favorable: my "worst" valid is
            // worse than his "worst" valid
            final String valid = params.get("SacValid");
            final String num = params.containsKey("Amount") ? params.get("Amount") : "1";
            int amount = AbilityFactory.calculateAmount(card, num, sa);

            final Card source = sa.getSourceCard();
            if (num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                amount = Math.min(ComputerUtil.determineLeftoverMana(sa), amount);
            }

            List<Card> humanList = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
            humanList = CardLists.getValidCards(humanList, valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());
            List<Card> computerList = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
            if (defined.equals("Opponent")) {
                computerList = new ArrayList<Card>();
            }
            computerList = CardLists.getValidCards(computerList, valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            // Since all of the cards have remAIDeck:True, I enabled 1 for 1
            // (or X for X) trades for special decks
            if (humanList.size() < amount) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * sacrificeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void sacrificeResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = sa.getSourceCard();

        // Expand Sacrifice keyword here depending on what we need out of it.
        final String num = params.containsKey("Amount") ? params.get("Amount") : "1";
        final int amount = AbilityFactory.calculateAmount(card, num, sa);

        final Target tgt = sa.getTarget();
        ArrayList<Player> tgts;
        if (tgt != null) {
            tgts = tgt.getTargetPlayers();
        } else {
            tgts = AbilityFactory.getDefinedPlayers(card, params.get("Defined"), sa);
        }

        String valid = params.get("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String msg = params.get("SacMessage");
        if (msg == null) {
            msg = valid;
        }

        msg = "Sacrifice a " + msg;

        final boolean destroy = params.containsKey("Destroy");
        final boolean remSacrificed = params.containsKey("RememberSacrificed");

        if (valid.equals("Self")) {
            if (AllZone.getZoneOf(card).is(ZoneType.Battlefield)) {
                if (Singletons.getModel().getGameAction().sacrifice(card, sa) && remSacrificed) {
                    card.addRemembered(card);
                }
            }
        }
        else {
            List<Card> sacList = null;
            for (final Player p : tgts) {
                if (params.containsKey("Random")) {
                    sacList = AbilityFactorySacrifice.sacrificeRandom(p, amount, valid, sa, destroy);
                } else if (p.isComputer()) {
                    if (params.containsKey("Optional") && sa.getActivatingPlayer().isHuman()) {
                        continue;
                    }
                    sacList = AbilityFactorySacrifice.sacrificeAI(p, amount, valid, sa, destroy);
                } else {
                    sacList = AbilityFactorySacrifice.sacrificeHuman(p, amount, valid, sa, destroy,
                            params.containsKey("Optional"));
                }
                if (remSacrificed) {
                    for (int i = 0; i < sacList.size(); i++) {
                        card.addRemembered(sacList.get(i));
                    }
                }
            }

        }
    }

    /**
     * <p>
     * sacrificeAI.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @param amount
     *            a int.
     * @param valid
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static List<Card> sacrificeAI(final Player p, final int amount, final String valid, final SpellAbility sa,
            final boolean destroy) {
        List<Card> battlefield = p.getCardsIn(ZoneType.Battlefield);
        List<Card> sacList = AbilityFactory.filterListByType(battlefield, valid, sa);
        sacList = ComputerUtil.sacrificePermanents(amount, sacList, destroy, sa);

        return sacList;
    }

    /**
     * <p>
     * sacrificeHuman.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @param amount
     *            a int.
     * @param valid
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static List<Card> sacrificeHuman(final Player p, final int amount, final String valid, final SpellAbility sa,
            final boolean destroy, final boolean optional) {
        List<Card> battlefield = p.getCardsIn(ZoneType.Battlefield);
        List<Card> list = AbilityFactory.filterListByType(battlefield, valid, sa);
        List<Card> sacList = new ArrayList<Card>();

        for (int i = 0; i < amount; i++) {
            if (list.isEmpty()) {
                break;
            }
            Object o;
            if (optional) {
                o = GuiChoose.oneOrNone("Select a card to sacrifice", list);
            } else {
                o = GuiChoose.one("Select a card to sacrifice", list);
            }
            if (o != null) {
                final Card c = (Card) o;

                if (destroy) {
                    if (Singletons.getModel().getGameAction().destroy(c)) {
                        sacList.add(c);
                    }
                } else {
                    if (Singletons.getModel().getGameAction().sacrifice(c, sa)) {
                        sacList.add(c);
                    }
                }

                list.remove(c);

            } else {
                return sacList;
            }
        }
        return sacList;
    }

    /**
     * <p>
     * sacrificeRandom.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @param amount
     *            a int.
     * @param valid
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static List<Card> sacrificeRandom(final Player p, final int amount, final String valid, final SpellAbility sa,
            final boolean destroy) {
        List<Card> sacList = new ArrayList<Card>();
        for (int i = 0; i < amount; i++) {
            List<Card> battlefield = p.getCardsIn(ZoneType.Battlefield);
            List<Card> list = AbilityFactory.filterListByType(battlefield, valid, sa);
            if (list.size() != 0) {
                final Card sac = CardUtil.getRandom(list);
                if (destroy) {
                    if (Singletons.getModel().getGameAction().destroy(sac)) {
                        sacList.add(sac);
                    }
                } else {
                    if (Singletons.getModel().getGameAction().sacrifice(sac, sa)) {
                        sacList.add(sac);
                    }
                }
            }
        }
        return sacList;
    }

    // **************************************************************
    // *********************** SacrificeAll *************************
    // **************************************************************

    /**
     * <p>
     * createAbilitySacrificeAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilitySacrificeAll(final AbilityFactory af) {
        class AbilitySacrificeAll extends AbilityActivated {
            public AbilitySacrificeAll(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilitySacrificeAll(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -1933592438783630254L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactorySacrifice.sacrificeAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactorySacrifice.sacrificeAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactorySacrifice.sacrificeAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactorySacrifice.sacrificeAllCanPlayAI(af, this);
            }
        }
        final SpellAbility abSacrifice = new AbilitySacrificeAll(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abSacrifice;
    }

    /**
     * <p>
     * createSpellSacrificeAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellSacrificeAll(final AbilityFactory af) {
        final SpellAbility spSacrifice = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactorySacrifice.sacrificeAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactorySacrifice.sacrificeAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactorySacrifice.sacrificeAllStackDescription(af, this);
            }
        };
        return spSacrifice;
    }

    /**
     * <p>
     * createDrawbackSacrificeAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackSacrificeAll(final AbilityFactory af) {
        class DrawbackSacrificeAll extends AbilitySub {
            public DrawbackSacrificeAll(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackSacrificeAll(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public void resolve() {
                AbilityFactorySacrifice.sacrificeAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public String getStackDescription() {
                return AbilityFactorySacrifice.sacrificeAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactorySacrifice.sacrificeAllCanPlayAI(af, this);
            }
        }
        final SpellAbility dbSacrifice = new DrawbackSacrificeAll(af.getHostCard(), af.getAbTgt());

        return dbSacrifice;
    }

    /**
     * <p>
     * sacrificeAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     * @since 1.0.15
     */
    public static String sacrificeAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening

        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        /*
         * This is not currently targeted ArrayList<Player> tgtPlayers;
         * 
         * Target tgt = af.getAbTgt(); if (tgt != null) tgtPlayers =
         * tgt.getTargetPlayers(); else tgtPlayers =
         * AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
         * params.get("Defined"), sa);
         */

        sb.append("Sacrifice permanents.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * sacrificeAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean sacrificeAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        if (valid.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            valid = valid.replace("X", Integer.toString(xPay));
        }

        List<Card> humanlist = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
        List<Card> computerlist = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);

        humanlist = CardLists.getValidCards(humanlist, valid.split(","), source.getController(), source);
        computerlist = CardLists.getValidCards(computerlist, valid.split(","), source.getController(), source);

        if (abCost != null) {
            // AI currently disabled for some costs
            if (!CostUtil.checkLifeCost(abCost, source, 4, null)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // if only creatures are affected evaluate both lists and pass only if
        // human creatures are more valuable
        if ((CardLists.getNotType(humanlist, "Creature").size() == 0) && (CardLists.getNotType(computerlist, "Creature").size() == 0)) {
            if ((CardFactoryUtil.evaluateCreatureList(computerlist) + 200) >= CardFactoryUtil
                    .evaluateCreatureList(humanlist)) {
                return false;
            }
        } // only lands involved
        else if ((CardLists.getNotType(humanlist, "Land").size() == 0) && (CardLists.getNotType(computerlist, "Land").size() == 0)) {
            if ((CardFactoryUtil.evaluatePermanentList(computerlist) + 1) >= CardFactoryUtil
                    .evaluatePermanentList(humanlist)) {
                return false;
            }
        } // otherwise evaluate both lists by CMC and pass only if human
          // permanents are more valuable
        else if ((CardFactoryUtil.evaluatePermanentList(computerlist) + 3) >= CardFactoryUtil
                .evaluatePermanentList(humanlist)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return ((r.nextFloat() < .9667) && chance);
    }

    /**
     * <p>
     * sacrificeAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static void sacrificeAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final Card card = sa.getSourceCard();

        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
        // to use the X variable
        // We really need a better solution to this
        if (valid.contains("X")) {
            valid = valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(card, "X", sa)));
        }

        List<Card> list;
        if (params.containsKey("Defined")) {
            list = new ArrayList<Card>(AbilityFactory.getDefinedCards(af.getHostCard(), params.get("Defined"), sa));
        } else {
            list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        }

        final boolean remSacrificed = params.containsKey("RememberSacrificed");
        if (remSacrificed) {
            card.clearRemembered();
        }

        list = AbilityFactory.filterListByType(list, valid, sa);

        for (int i = 0; i < list.size(); i++) {
            if (Singletons.getModel().getGameAction().sacrifice(list.get(i), sa) && remSacrificed) {
                card.addRemembered(list.get(i));
            }
        }
    }

} // end class AbilityFactory_Sacrifice
