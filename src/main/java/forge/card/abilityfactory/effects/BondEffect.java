package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class BondEffect extends SpellEffect {
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
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
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
        List<Card> cards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        cards = AbilityFactory.filterListByType(cards, params.get("ValidCards"), sa);
        if (cards.isEmpty()) {
            return;
        }

        Card partner = null;
        // skip choice if only one card on list
        if (cards.size() == 1) {
            partner = cards.get(0);
        } else if (sa.getActivatingPlayer().isHuman()) {
            Object o = GuiChoose.one("Select a card to pair with", cards);

            if (o != null) {
                partner = (Card) o;
            }
        } else {
            // TODO - Pick best creature instead of just the first on the list
            partner = CardFactoryUtil.getBestCreatureAI(cards);
        }

        // pair choices together
        trigCards.get(0).setPairedWith(partner);
        partner.setPairedWith(trigCards.get(0));

    } // bondResolve

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
    @Override
    public String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
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

} // end class AbilityFactoryBond