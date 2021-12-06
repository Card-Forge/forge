package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class BondEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        // find card that triggered pairing first
        CardCollectionView trigCards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);

        // Check that this card hasn't already become paired by an earlier trigger
        if (trigCards.getFirst().isPaired() || !trigCards.getFirst().isInPlay()) {
            return;
        }

        // find list of valid cards to pair with
        CardCollectionView cards = sa.getActivatingPlayer().getGame().getCardsIn(ZoneType.Battlefield);
        cards = AbilityUtils.filterListByType(cards, sa.getParam("ValidCards"), sa);
        if (cards.isEmpty()) {
            return;
        }

        Card partner = cards.getFirst();
        // skip choice if only one card on list
        if (cards.size() > 1) {
            partner = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(cards, sa, Localizer.getInstance().getMessage("lblSelectACardPair"), null);
        }

        // pair choices together
        trigCards.getFirst().setPairedWith(partner);
        partner.setPairedWith(trigCards.getFirst());
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        List<Card> tgts = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);

        final StringBuilder sb = new StringBuilder();

        for (final Card c : tgts) {
            sb.append(c).append(" ");
        }
        sb.append("pairs with another unpaired creature you control.");
        return sb.toString();
    } // end bondStackDescription()

}
