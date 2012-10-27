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

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardCharacteristicName;

import forge.CardLists;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostMana;
import forge.card.cost.CostPart;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.item.CardDb;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_Copy class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryCopy.java 13784 2012-02-03 16:29:28Z Sloth $
 */
public final class AbilityFactoryPlay {

    // *************************************************************************
    // ************************* Play *************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityPlay.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityPlay(final AbilityFactory af) {
        class AbilityPlay extends AbilityActivated {
            public AbilityPlay(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityPlay(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5232548517225345052L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPlay.playStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPlay.playCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPlay.playResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPlay.playTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abCopySpell = new AbilityPlay(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abCopySpell;
    }

    /**
     * <p>
     * createSpellPlay.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellPlay(final AbilityFactory af) {
        final SpellAbility spCopySpell = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 1878946074608916745L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPlay.playStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPlay.playCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPlay.playResolve(af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                return AbilityFactoryPlay.playTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }

        };
        return spCopySpell;
    }

    /**
     * <p>
     * createDrawbackPlay.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPlay(final AbilityFactory af) {
        class DrawbackPlay extends AbilitySub {
            public DrawbackPlay(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackPlay(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 1927508119173644632L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPlay.playStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPlay.playResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPlay.playTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbCopySpell = new DrawbackPlay(af.getHostCard(), af.getAbTgt());

        return dbCopySpell;
    }

    /**
     * <p>
     * playStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String playStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }
        sb.append("Play ");
        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (params.containsKey("Valid")) {
            sb.append("cards");
        } else {
            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        if (params.containsKey("WithoutManaCost")) {
            sb.append(" without paying the mana cost");
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
     * playCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean playCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final Cost abCost = af.getAbCost();
        final Card source = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();
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

        // don't use this as a response
        if (Singletons.getModel().getGame().getStack().size() != 0) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getRestrictions().getNumberTurnActivations());

        List<Card> cards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            ZoneType zone = tgt.getZone().get(0);
            cards = CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(zone), tgt.getValidTgts(), ai, source);
            if (cards.isEmpty()) {
                return false;
            }
            tgt.addTarget(CardFactoryUtil.getBestAI(cards));
        } else if (!params.containsKey("Valid")) {
            cards = new ArrayList<Card>(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa));
            if (cards.isEmpty()) {
                return false;
            }
        }
        return chance;
    }

    /**
     * <p>
     * playTriggerAI.
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
    private static boolean playTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {

        if (mandatory) {
            return true;
        }

        return playCanPlayAI(ai, af, sa);
    }

    /**
     * <p>
     * playResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void playResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        Player activator = sa.getActivatingPlayer();
        boolean optional = params.containsKey("Optional");
        boolean remember = params.containsKey("RememberPlayed");
        boolean wasFaceDown = false;
        int amount = 1;
        if (params.containsKey("Amount") && !params.get("Amount").equals("All")) {
            amount = AbilityFactory.calculateAmount(source, params.get("Amount"), sa);
        }

        if (params.containsKey("Controller")) {
            activator = AbilityFactory.getDefinedPlayers(source, params.get("Controller"), sa).get(0);
        }

        final Player controller = activator;
        List<Card> tgtCards = new ArrayList<Card>();

        final Target tgt = sa.getTarget();
        if (params.containsKey("Valid")) {
            ZoneType zone = ZoneType.Hand;
            if (params.containsKey("ValidZone")) {
                zone = ZoneType.smartValueOf(params.get("ValidZone"));
            }
            tgtCards = Singletons.getModel().getGame().getCardsIn(zone);
            tgtCards = AbilityFactory.filterListByType(tgtCards, params.get("Valid"), sa);
        } else if (params.containsKey("Defined")) {
            tgtCards = new ArrayList<Card>(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa));
        } else if (tgt != null) {
            tgtCards = new ArrayList<Card>(tgt.getTargetCards());
        }

        if (tgtCards.isEmpty()) {
            return;
        }

        if (params.containsKey("Amount") && params.get("Amount").equals("All")) {
            amount = tgtCards.size();
        }

        for (int i = 0; i < amount; i++) {
            if (tgtCards.isEmpty()) {
                return;
            }
            Card tgtCard = tgtCards.get(0);
            if (tgtCards.size() > 1) {
                if (controller.isHuman()) {
                    tgtCard = GuiChoose.one("Select a card to play", tgtCards);
                } else {
                    // AI
                    tgtCards = CardLists.filter(tgtCards, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            ArrayList<SpellAbility> spellAbilities = c.getBasicSpells();
                            ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
                            for (SpellAbility s : spellAbilities) {
                                Spell spell = (Spell) s;
                                s.setActivatingPlayer(controller);
                                SpellAbilityRestriction res = s.getRestrictions();
                                // timing restrictions still apply
                                if (res.checkTimingRestrictions(c, s) && spell.canPlayFromEffectAI(false, true)) {
                                    sas.add(s);
                                }
                            }
                            if (sas.isEmpty()) {
                                return false;
                            }
                            return true;
                        }
                    });
                    tgtCard = CardFactoryUtil.getBestAI(tgtCards);
                    if (tgtCard == null) {
                        return;
                    }
                }
            }
            if (tgtCard.isFaceDown()) {
                tgtCard.setState(CardCharacteristicName.Original);
                wasFaceDown = true;
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("Do you want to play " + tgtCard + "?");
            if (controller.isHuman() && optional
                    && !GameActionUtil.showYesNoDialog(source, sb.toString())) {
                // i--;  // This causes an infinite loop (ArsenalNut)
                if (wasFaceDown) {
                    tgtCard.setState(CardCharacteristicName.FaceDown);
                }
                continue;
            }
            if (params.containsKey("ForgetRemembered")) {
                source.clearRemembered();
            }
            if (params.containsKey("CopyCard")) {
                tgtCard = Singletons.getModel().getCardFactory().getCard(CardDb.instance().getCard(tgtCard), sa.getActivatingPlayer());
                // when copying something stolen:
                tgtCard.addController(sa.getActivatingPlayer());

                tgtCard.setToken(true);
                tgtCard.setCopiedSpell(true);
            }
            // lands will be played
            if (tgtCard.isLand()) {
                controller.playLand(tgtCard);
                if (remember && controller.canPlayLand()) {
                    source.addRemembered(tgtCard);
                }
                tgtCards.remove(tgtCard);
                continue;
            }

            // get basic spells (no flashback, etc.)
            ArrayList<SpellAbility> spellAbilities = tgtCard.getBasicSpells();
            ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
            for (SpellAbility s : spellAbilities) {
                final SpellAbility newSA = s.copy();
                newSA.setActivatingPlayer(controller);
                SpellAbilityRestriction res = new SpellAbilityRestriction();
                // timing restrictions still apply
                res.setPlayerTurn(s.getRestrictions().getPlayerTurn());
                res.setOpponentTurn(s.getRestrictions().getOpponentTurn());
                res.setPhases(s.getRestrictions().getPhases());
                res.setZone(null);
                newSA.setRestrictions(res);
                // timing restrictions still apply
                if (res.checkTimingRestrictions(tgtCard, newSA)) {
                    sas.add(newSA);
                }
            }
            if (sas.isEmpty()) {
                return;
            }
            tgtCards.remove(tgtCard);
            SpellAbility tgtSA = null;
            // only one mode can be used
            if (sas.size() == 1) {
                tgtSA = sas.get(0);
            } else if (sa.getActivatingPlayer().isHuman()) {
                tgtSA = GuiChoose.one("Select a spell to cast", sas);
            } else {
                tgtSA = sas.get(0);
            }

            if (tgtSA.getTarget() != null && !optional) {
                tgtSA.getTarget().setMandatory(true);
            }

            if (params.containsKey("WithoutManaCost")) {
                if (controller.isHuman()) {
                    final SpellAbility newSA = tgtSA.copy();
                    final Cost cost = new Cost(tgtCard, "", false);
                    if (newSA.getPayCosts() != null) {
                        for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                            if (!(part instanceof CostMana)) {
                                cost.getCostParts().add(part);
                            }
                        }
                    }
                    newSA.setPayCosts(cost);
                    newSA.setManaCost("");
                    newSA.setDescription(newSA.getDescription() + " (without paying its mana cost)");
                    Singletons.getModel().getGame().getAction().playSpellAbility(newSA);
                    if (remember) {
                        source.addRemembered(tgtSA.getSourceCard());
                    }
                } else {
                    if (tgtSA instanceof Spell) {
                        Spell spell = (Spell) tgtSA;
                        if (spell.canPlayFromEffectAI(!optional, true) || !optional) {
                            ComputerUtil.playSpellAbilityWithoutPayingManaCost(controller, tgtSA);
                            if (remember) {
                                source.addRemembered(tgtSA.getSourceCard());
                            }
                        }
                    }
                }
            } else {
                if (controller.isHuman()) {
                    Singletons.getModel().getGame().getAction().playSpellAbility(tgtSA);
                    if (remember) {
                        source.addRemembered(tgtSA.getSourceCard());
                    }
                } else {
                    if (tgtSA instanceof Spell) {
                        Spell spell = (Spell) tgtSA;
                        if (spell.canPlayFromEffectAI(!optional, false) || !optional) {
                            ComputerUtil.playStack(tgtSA, controller);
                            if (remember) {
                                source.addRemembered(tgtSA.getSourceCard());
                            }
                        }
                    }
                }
            }
        }
    } // end resolve

} // end class AbilityFactory_Copy
