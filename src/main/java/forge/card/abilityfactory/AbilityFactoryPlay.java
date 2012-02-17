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
import forge.ComputerUtil;
import forge.GameActionUtil;
import forge.Player;
import forge.Constant.Zone;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostMana;
import forge.card.cost.CostPart;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
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

        final SpellAbility abCopySpell = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5232548517225345052L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPlay.playStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPlay.playCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPlay.playResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPlay.playTriggerAI(af, this, mandatory);
            }

        };
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
                return AbilityFactoryPlay.playCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPlay.playResolve(af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return true;
                }
                return AbilityFactoryPlay.playTriggerAI(af, this, mandatory);
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
        final SpellAbility dbCopySpell = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
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
                return AbilityFactoryPlay.playTriggerAI(af, this, mandatory);
            }

        };
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
        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        sb.append("Cast ");
        // TODO Someone fix this Description when Copying Charms
        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        if (params.containsKey("WithoutManaCost")) {
            sb.append(" without paying its mana cost");
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
    private static boolean playCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Cost abCost = af.getAbCost();
        final Card source = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();
        final Random r = MyRandom.getRandom();

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

        // don't use this as a response
        if (AllZone.getStack().size() != 0) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getRestrictions().getNumberTurnActivations());

        CardList cards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            Zone zone = tgt.getZone().get(0);
            cards = AllZoneUtil.getCardsIn(zone);
            cards = cards.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), source);
            if (cards.isEmpty()) {
                return false;
            }
            tgt.addTarget(CardFactoryUtil.getBestAI(cards));
        } else {
            cards = new CardList(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa));
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
    private static boolean playTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {

        return false;
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
        final Card card = af.getHostCard();
        Player controller = sa.getActivatingPlayer();

        if (params.containsKey("Controller")) {
            controller = AbilityFactory.getDefinedPlayers(card, params.get("Controller"), sa).get(0);
        }

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtCards.isEmpty()) {
            return;
        }

        Card tgtCard = tgtCards.get(0);
        final StringBuilder sb = new StringBuilder();
        sb.append("Do you want to play " + tgtCard + "?");
        if (controller.isHuman() && params.containsKey("Optional") && !GameActionUtil.showYesNoDialog(card, sb.toString())) {
            return;
        }
        if (tgtCard.isLand()) {
            controller.playLand(tgtCard);
            return;
        }

        ArrayList<SpellAbility> sas = tgtCard.getBasicSpells();
        if (sas.isEmpty()) {
            return;
        }
        SpellAbility tgtSA = sas.get(0);

        if (params.containsKey("WithoutManaCost")) {
            if (controller.isHuman()) {
                final SpellAbility newSA = tgtSA.copy();
                final Cost cost = new Cost("", tgtCard.getName(), false);
                for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                    if (!(part instanceof CostMana)) {
                        cost.getCostParts().add(part);
                    }
                }
                cost.setNoManaCostChange(true);
                newSA.setPayCosts(cost);
                newSA.setManaCost("");
                newSA.setDescription(sa.getDescription() + " (without paying its mana cost)");
                AllZone.getGameAction().playSpellAbility(newSA);
            } else {
                if (tgtSA instanceof Spell) {
                    Spell spell = (Spell) tgtSA;
                    if (spell.canPlayFromEffectAI(false, true)) {
                        //System.out.println("canPlayFromEffectAI: " + this.getHostCard());
                        ComputerUtil.playSpellAbilityWithoutPayingManaCost(tgtSA);
                    }
                }
            }
        } else {
            if (controller.isHuman()) {
                AllZone.getGameAction().playSpellAbility(tgtSA);
            } else if (tgtSA.canPlayAI()) {
                ComputerUtil.playStack(tgtSA);
            }
        }
    } // end resolve

} // end class AbilityFactory_Copy
