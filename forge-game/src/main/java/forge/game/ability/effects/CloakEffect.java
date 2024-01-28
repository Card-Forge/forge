package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class CloakEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(source,
                sa.getParam("Amount"), sa) : 1;

        CardZoneTable triggerList = new CardZoneTable(game.copyLastStateBattlefield(), game.copyLastStateGraveyard());
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, triggerList.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, triggerList.getLastStateGraveyard());
        moveParams.put(AbilityKey.InternalTriggerTable, triggerList);

        for (final Player p : getTargetPlayers(sa, "DefinedPlayer")) {
            CardCollection tgtCards;
            if (sa.hasParam("Choices") || sa.hasParam("ChoiceZone")) {
                ZoneType choiceZone = ZoneType.Hand;
                if (sa.hasParam("ChoiceZone")) {
                    choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
                }
                CardCollectionView choices = game.getCardsIn(choiceZone);
                if (sa.hasParam("Choices")) {
                    choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, source, sa);
                }
                if (choices.isEmpty()) {
                    continue;
                }

                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseCards") + " ";

                tgtCards = new CardCollection(activator.getController().chooseCardsForEffect(choices, sa, title, amount, amount, false, null));
            } else {
                tgtCards = getTargetCards(sa);
            }

            if (sa.hasParam("Shuffle")) {
                CardLists.shuffle(tgtCards);
            }

            for (Card c : tgtCards) {
                Card rem = c.cloak(p, sa, moveParams);
                if (rem != null && sa.hasParam("RememberCloaked") && rem.isCloaked()) {
                    source.addRemembered(rem);
                }
            }
        }

        triggerList.triggerChangesZoneAll(game, sa);
    }
}
