package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

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

public class ManifestEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();
        // Usually a number leaving possibility for X, Sacrifice X land: Manifest X creatures.
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(source,
                sa.getParam("Amount"), sa) : 1;
        // Most commonly "defined" is Top of Library
        final String defined = sa.getParamOrDefault("Defined", "TopOfLibrary");

        CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
        CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

        Map<AbilityKey, Object> moveParams = Maps.newEnumMap(AbilityKey.class);
        moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
        moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);

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

                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseCardToManifest") + " ";

                tgtCards = new CardCollection(activator.getController().chooseCardsForEffect(choices, sa, title, amount, amount, false, null));
            } else if ("TopOfLibrary".equals(defined)) {
                tgtCards = p.getTopXCardsFromLibrary(amount);
            } else {
                tgtCards = getTargetCards(sa);
            }

            if (sa.hasParam("Shuffle")) {
                CardLists.shuffle(tgtCards);
            }

            for (Card tgtC : tgtCards) {
                // check if the object is still in game or if it was moved
                Card gameCard = game.getCardState(tgtC, null);
                // gameCard is LKI in that case, the card is not in game anymore
                // or the timestamp did change
                // this should check Self too
                if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                    continue;
                }
                CardZoneTable triggerList = new CardZoneTable();
                ZoneType origin = gameCard.getZone().getZoneType();
                Card rem = gameCard.manifest(p, sa, moveParams);
                if (rem != null) {
                    if (sa.hasParam("RememberManifested") && rem.isManifested()) {
                        source.addRemembered(rem);
                    }
                    // 701.34d. If an effect instructs a player to manifest multiple cards from their library,
                    // those cards are manifested one at a time.
                    triggerList.put(origin, ZoneType.Battlefield, rem);
                    triggerList.triggerChangesZoneAll(game, sa);
                }
            }
        }
    }
}
