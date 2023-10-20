package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class TapOrUntapAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
        sb.append("Tap or untap ");

        if (sa.hasParam("ValidMessage")) {
            sb.append(sa.getParam("ValidMessage"));
        } else {
            sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        CardCollectionView validCards;
        if (sa.hasParam("ValidCards")) {
            validCards = AbilityUtils.filterListByType(game.getCardsIn(ZoneType.Battlefield), sa.getParam("ValidCards"), sa);
        } else {
            validCards = getTargetCards(sa);
        }

        if (sa.usesTargeting() || sa.hasParam("Defined")) {
            validCards = CardLists.filterControlledBy(validCards, getTargetPlayers(sa));
        }

        // Default to tapping for AI
        boolean toTap = true;

        StringBuilder sb = new StringBuilder(Localizer.getInstance().getMessage("lblTapOrUntapTarget") + " ");
        if (sa.hasParam("ValidMessage")) {
            sb.append(sa.getParam("ValidMessage"));
        } else {
            sb.append(Localizer.getInstance().getMessage("lblPermanents"));
        }
        sb.append("?");

        toTap = sa.getActivatingPlayer().getController().chooseBinary(sa, sb.toString(), PlayerController.BinaryChoiceType.TapOrUntap);

        for (final Card tgtC : validCards) {
            if (!tgtC.isInPlay()) {
                continue;
            }

            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(tgtC, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                continue;
            }
            if (toTap) {
                gameCard.tap(true, sa, activator);
            } else {
                gameCard.untap(true);
            }
        }
    }

}
