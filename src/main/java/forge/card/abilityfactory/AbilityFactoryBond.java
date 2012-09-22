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
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.game.player.ComputerUtil;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;

/**
 * <p>
 * AbilityFactoryBond class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryBond.java 15090 2012-04-07 12:50:31Z Max mtg $
 */
public final class AbilityFactoryBond {

    private AbilityFactoryBond() {
        throw new AssertionError();
    }

    // **************************************************************
    // ************************** Bond ***************************
    // **************************************************************

    /**
     * <p>
     * createAbilityBond.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityBond(final AbilityFactory af) {
        class AbilityBond extends AbilityActivated {
            public AbilityBond(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityBond(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 1938171749867735256L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryBond.bondCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryBond.bondResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryBond.bondStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryBond.bondTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abBond = new AbilityBond(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abBond;
    }

//    /**
//     * <p>
//     * createSpellBond.
//     * </p>
//     * 
//     * @param af
//     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
//     * @return a {@link forge.card.spellability.SpellAbility} object.
//     */
//    public static SpellAbility createSpellBond(final AbilityFactory af) {
//        final SpellAbility spBond = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
//            private static final long serialVersionUID = -4047747186919390147L;
//
//            @Override
//            public boolean canPlayAI() {
//                return AbilityFactoryBond.bondCanPlayAI(af, this);
//            }
//
//            @Override
//            public void resolve() {
//                AbilityFactoryBond.bondResolve(af, this);
//            }
//
//            @Override
//            public String getStackDescription() {
//                return AbilityFactoryBond.bondStackDescription(af, this);
//            }
//        };
//        return spBond;
//    }
//
//    /**
//     * <p>
//     * createDrawbackBond.
//     * </p>
//     * 
//     * @param af
//     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
//     * @return a {@link forge.card.spellability.SpellAbility} object.
//     */
//    public static SpellAbility createDrawbackBond(final AbilityFactory af) {
//        final SpellAbility dbBond = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
//            private static final long serialVersionUID = -8659938411460952874L;
//
//            @Override
//            public void resolve() {
//                AbilityFactoryBond.bondResolve(af, this);
//            }
//
//            @Override
//            public boolean chkAIDrawback() {
//                return AbilityFactoryBond.bondPlayDrawbackAI(af, this);
//            }
//
//            @Override
//            public String getStackDescription() {
//                return AbilityFactoryBond.bondStackDescription(af, this);
//            }
//
//            @Override
//            public boolean doTrigger(final boolean mandatory) {
//                return AbilityFactoryBond.bondTriggerAI(af, this, mandatory);
//            }
//        };
//        return dbBond;
//    }

    /**
     * <p>
     * bondStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String bondStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        ArrayList<Card> tgts;
        tgts = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

        final StringBuilder sb = new StringBuilder();

        for (final Card c : tgts) {
            sb.append(c).append(" ");
        }
        sb.append("pairs with another unpaired creature you control.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // end bondStackDescription()

    /**
     * <p>
     * bondCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean bondCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

//        final HashMap<String, String> params = af.getMapParams();
//        final Target tgt = sa.getTarget();
//        final Card source = sa.getSourceCard();

        boolean chance = AbilityFactoryBond.bondTgtAI(af, sa);

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    } // end bondCanPlayAI()

//    /**
//     * <p>
//     * bondPlayDrawbackAI.
//     * </p>
//     * 
//     * @param af
//     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
//     * @param sa
//     *            a {@link forge.card.spellability.SpellAbility} object.
//     * @return a boolean.
//     */
//    private static boolean bondPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
//        // AI should only activate this during Human's turn
//        boolean chance = AbilityFactoryBond.bondTgtAI(af, sa);
//
//        // TODO - restrict the subAbility a bit
//
//        final AbilitySub subAb = sa.getSubAbility();
//        if (subAb != null) {
//            chance &= subAb.chkAIDrawback();
//        }
//
//        return chance;
//    }

    /**
     * <p>
     * bondTriggerAI.
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
    private static boolean bondTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) { // If there is a cost payment
            return false;
        }

        boolean chance = AbilityFactoryBond.bondTgtAI(af, sa);

         final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance || mandatory;
    }

    /**
     * <p>
     * bondTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean bondTgtAI(final AbilityFactory af, final SpellAbility sa) {
     // TODO - add some kind of check to if there good creature to Soulbond with
     //        initially AI will always use Soulbound if triggered
        return true;
    }

    /**
     * <p>
     * bondResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void bondResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
//        final Card source = sa.getSourceCard();
//        final Card host = af.getHostCard();
//        final Map<String, String> svars = host.getSVars();

        // find card that triggered pairing first
        ArrayList<Card> trigCards;
        trigCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

        // Check that this card hasn't already become paired by an earlier trigger
        if (trigCards.get(0).isPaired() || !trigCards.get(0).isInZone(ZoneType.Battlefield)) {
            return;
        }

        // find list of valid cards to pair with
        CardList cards = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        cards = AbilityFactory.filterListByType(cards, params.get("ValidCards"), sa);
        if (cards.isEmpty()) {
            return;
        }

        Card partner = null;
        // skip choice if only one card on list
        if (cards.size() == 1) {
            partner = cards.get(0);
        } else if (sa.getActivatingPlayer().isHuman()) {
            Object o = GuiUtils.chooseOne("Select a card to pair with", cards);

            if (o != null) {
                partner = (Card) o;
            }
        } else {
            // TODO - Pick best creature instead of just the first on the list
            partner = cards.get(0);
        }

        // pair choices together
        trigCards.get(0).setPairedWith(partner);
        partner.setPairedWith(trigCards.get(0));

    } // bondResolve

} // end class AbilityFactoryBond
