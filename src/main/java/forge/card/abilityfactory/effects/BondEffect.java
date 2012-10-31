package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
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

        Card partner = cards.get(0);
        // skip choice if only one card on list
        if (cards.size() > 1)
            if ( sa.getActivatingPlayer().isHuman() ) {
                Card o = GuiChoose.one("Select a card to pair with", cards);
                if (o != null) {
                    partner = o;
                }
            } else {
                // TODO - Pick best creature instead of just the first on the list
                partner = CardFactoryUtil.getBestCreatureAI(cards);
            }

        // pair choices together
        trigCards.get(0).setPairedWith(partner);
        partner.setPairedWith(trigCards.get(0));
    }

    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        ArrayList<Card> tgts;
        tgts = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

        final StringBuilder sb = new StringBuilder();

        for (final Card c : tgts) {
            sb.append(c).append(" ");
        }
        sb.append("pairs with another unpaired creature you control.");
        return sb.toString();
    } // end bondStackDescription()

}