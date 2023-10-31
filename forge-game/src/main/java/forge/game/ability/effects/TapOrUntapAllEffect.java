package forge.game.ability.effects;


import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.Map;

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

        CardCollection tapped = new CardCollection();
        for (final Card tgtC : validCards) {
            if (!tgtC.isInPlay()) {
                continue;
            }
            if (toTap) {
                if (tgtC.tap(true, sa, activator)) tapped.add(tgtC);
            } else {
                tgtC.untap(true);
            }
        }
        if (!tapped.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, tapped);
            game.getTriggerHandler().runTrigger(TriggerType.TapAll, runParams, false);
        }
    }

}
