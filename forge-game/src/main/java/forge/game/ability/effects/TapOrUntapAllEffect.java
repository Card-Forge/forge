package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
import forge.util.Localizer;
import forge.util.collect.FCollection;


public class TapOrUntapAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
        sb.append("Tap or untap ");

        if (sa.hasParam("ValidMessage")) {
            sb.append(sa.getParam("ValidMessage"));
        }
        else {
            final List<Card> tgtCards = getTargetCards(sa);
            sb.append(StringUtils.join(tgtCards, ", "));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        CardCollectionView validCards = getTargetCards(sa);
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        FCollection<Player> targetedPlayers = getTargetPlayers(sa);

        if (sa.hasParam("ValidCards")) {
            validCards = game.getCardsIn(ZoneType.Battlefield);
            validCards = AbilityUtils.filterListByType(validCards, sa.getParam("ValidCards"), sa);
        }
        
        if (sa.usesTargeting() || sa.hasParam("Defined")) {
            validCards = CardLists.filterControlledBy(validCards, targetedPlayers);
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


        for (final Card cad : validCards) {
            if (cad.isInPlay()) {
                if (toTap) {
                    cad.tap(true);
                } else {
                    cad.untap(true);
                }
            }
        }
    }

}
