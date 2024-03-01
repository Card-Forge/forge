package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Iterables;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public abstract class ManifestBaseEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();
        // Usually a number leaving possibility for X, Sacrifice X land: Manifest X creatures.
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(source,
                sa.getParam("Amount"), sa) : 1;

        for (final Player p : getTargetPlayers(sa, "DefinedPlayer")) {
            CardCollection tgtCards;
            boolean fromLibrary = false;
            if (sa.hasParam("Choices") || sa.hasParam("ChoiceZone")) {
                ZoneType choiceZone = ZoneType.Hand;
                if (sa.hasParam("ChoiceZone")) {
                    choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
                    fromLibrary = choiceZone.equals(ZoneType.Library);
                }
                CardCollectionView choices = game.getCardsIn(choiceZone);
                if (sa.hasParam("Choices")) {
                    choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, source, sa);
                }
                if (choices.isEmpty()) {
                    continue;
                }

                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : getDefaultMessage() + " ";

                tgtCards = new CardCollection(activator.getController().chooseCardsForEffect(choices, sa, title, amount, amount, false, null));
            } else if ("TopOfLibrary".equals(sa.getParamOrDefault("Defined", "TopOfLibrary"))) {
                tgtCards = p.getTopXCardsFromLibrary(amount);
                fromLibrary = true;
            } else {
                tgtCards = getTargetCards(sa);
                if (Iterables.all(tgtCards, CardPredicates.inZone(ZoneType.Library))) {
                    fromLibrary = true;
                }
            }

            if (sa.hasParam("Shuffle")) {
                CardLists.shuffle(tgtCards);
            }

            if (fromLibrary) {
                for (Card c : tgtCards) {
                    // CR 701.34d If an effect instructs a player to manifest multiple cards from their library, those cards are manifested one at a time.
                    Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                    CardZoneTable triggerList = AbilityKey.addCardZoneTableParams(moveParams, sa);
                    internalEffect(c, p, sa, moveParams);
                    triggerList.triggerChangesZoneAll(game, sa);
                }
            } else {
                // manifest from other zones should be done at the same time
                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                CardZoneTable triggerList = AbilityKey.addCardZoneTableParams(moveParams, sa);
                for (Card c : tgtCards) {
                    internalEffect(c, p, sa, moveParams);
                }
                triggerList.triggerChangesZoneAll(game, sa);
            }
        }
    }

    abstract protected String getDefaultMessage();

    abstract protected Card internalEffect(Card c, Player p, SpellAbility sa, Map<AbilityKey, Object> moveParams);
}
