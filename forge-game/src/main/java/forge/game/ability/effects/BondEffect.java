package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.Localizer;

public class BondEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        Player p = sa.getActivatingPlayer();
        Game game = source.getGame();
        for (Card tgtC : getTargetCards(sa)) {
            Card gameCard = game.getCardState(tgtC, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                continue;
            }
            if (gameCard.isPaired() || !gameCard.isCreature() || !gameCard.isInPlay() || gameCard.getController() != p) {
                continue;
            }

            // find list of valid cards to pair with
            CardCollectionView cards = CardLists.getValidCards(p.getCreaturesInPlay(), sa.getParam("ValidCards"), p, source, sa);
            if (cards.isEmpty()) {
                continue;
            }

            Card partner = p.getController().chooseSingleEntityForEffect(cards, sa, Localizer.getInstance().getMessage("lblSelectACardPair"), true, null);

            if (partner != null) {
                // pair choices together
                gameCard.setPairedWith(partner);
                partner.setPairedWith(gameCard);
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(Lang.joinHomogenous(getTargetCards(sa)));

        sb.append(" pairs with another unpaired creature you control.");
        return sb.toString();
    } // end bondStackDescription()

}
