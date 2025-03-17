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
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public abstract class ManifestBaseEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        // Usually a number leaving possibility for X, Sacrifice X land: Manifest X creatures.
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(source, sa.getParam("Amount"), sa) : 1;

        for (final Player p : getTargetPlayers(sa, "DefinedPlayer")) {
            manifestLoop(sa, p, amount);
        }
    }

    protected void manifestLoop(SpellAbility sa, Player p, final int amount) {
        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();

        CardCollection tgtCards;
        boolean fromLibrary = false;
        if (sa.hasParam("Choices") || sa.hasParam("ChoiceZone")) {
            ZoneType choiceZone = ZoneType.Hand;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
                fromLibrary = choiceZone.equals(ZoneType.Library);
            }
            CardCollectionView choices = p.getCardsIn(choiceZone);
            if (sa.hasParam("Choices")) {
                choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, source, sa);
            }
            if (choices.isEmpty()) {
                return;
            }

            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : getDefaultMessage() + " ";

            tgtCards = new CardCollection(p.getController().chooseCardsForEffect(choices, sa, title, amount, amount, false, null));
        } else if ("TopOfLibrary".equals(sa.getParamOrDefault("Defined", "TopOfLibrary"))) {
            tgtCards = p.getTopXCardsFromLibrary(amount);
            fromLibrary = true;
        } else {
            tgtCards = getTargetCards(sa);
            if (tgtCards.allMatch(CardPredicates.inZone(ZoneType.Library))) {
                fromLibrary = true;
            }
        }

        if (sa.hasParam("Shuffle")) {
            CardLists.shuffle(tgtCards);
        }

        if (fromLibrary) {
            for (Card c : tgtCards) {
                Card gameCard = game.getCardState(c, null);
                // gameCard is LKI in that case, the card is not in game anymore
                // or the timestamp did change
                // this should check Self too
                if (gameCard == null || !c.equalsWithGameTimestamp(gameCard)) {
                    continue;
                }
 
                // CR 701.34d If an effect instructs a player to manifest multiple cards from their library, those cards are manifested one at a time.
                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                CardZoneTable triggerList = AbilityKey.addCardZoneTableParams(moveParams, sa);
                internalEffect(gameCard, p, sa, moveParams);
                triggerList.triggerChangesZoneAll(game, sa);
            }
        } else {
            // manifest from other zones should be done at the same time
            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            CardZoneTable triggerList = AbilityKey.addCardZoneTableParams(moveParams, sa);
            for (Card c : tgtCards) {
                Card gameCard = game.getCardState(c, null);
                // gameCard is LKI in that case, the card is not in game anymore
                // or the timestamp did change
                // this should check Self too
                if (gameCard == null || !c.equalsWithGameTimestamp(gameCard)) {
                    continue;
                }
                internalEffect(gameCard, p, sa, moveParams);
            }
            triggerList.triggerChangesZoneAll(game, sa);
        }
    }

    abstract protected String getDefaultMessage();

    abstract protected Card internalEffect(Card c, Player p, SpellAbility sa, Map<AbilityKey, Object> moveParams);
}
