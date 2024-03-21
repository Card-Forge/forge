package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class RegenerateEffect extends RegenerateBaseEffect {

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        if (!tgtCards.isEmpty()) {
            sb.append("Regenerate ");
            sb.append(Lang.joinHomogenous(tgtCards));
            sb.append(".");
        }

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        CardCollection result = new CardCollection();

        for (Card c : getDefinedCardsOrTargeted(sa)) {
            if (!c.isInPlay()) {
                continue;
            }

            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(c, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !c.equalsWithGameTimestamp(gameCard)) {
                continue;
            }
            result.add(gameCard);
        }
        // create Effect for Regeneration
        createRegenerationEffect(sa, result);
    }

}
