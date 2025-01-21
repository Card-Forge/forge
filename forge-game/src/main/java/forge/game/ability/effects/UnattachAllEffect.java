package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.spellability.SpellAbility;

import forge.util.Lang;

public class UnattachAllEffect extends SpellAbilityEffect {
    private static void handleUnattachment(final GameEntity o, final Card cardToUnattach) {
        if (cardToUnattach.isAttachment() && o.hasCardAttachment(cardToUnattach)) {
            cardToUnattach.unattachFromEntity(cardToUnattach.getEntityAttachedTo());
        }
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Unattach all valid Equipment and Auras from ");
        sb.append(Lang.joinHomogenous(getTargetEntities(sa)));
        return sb.toString();
    }

    @Override
    public void resolve(final SpellAbility sa) {
        Card source = sa.getHostCard();
        final Game game = source.getGame();
        String valid = sa.getParam("UnattachValid");

        // If Cast Targets will be checked on the Stack
        for (GameEntity ge : getTargetEntities(sa)) {
            if (ge instanceof Card) {
                Card gc = (Card) ge;
                // check if the object is still in game or if it was moved
                Card gameCard = game.getCardState(gc, null);
                // gameCard is LKI in that case, the card is not in game anymore
                // or the timestamp did change
                // this should check Self too
                if (gameCard == null || !gc.equalsWithGameTimestamp(gameCard)) {
                    continue;
                }
                ge = gameCard;
            }
            for (final Card c : CardLists.getValidCards(ge.getAttachedCards(), valid, source.getController(), source, sa)) {
                handleUnattachment(ge, c);
            }
        }
    }
}
