package forge.game.ability.effects;

import com.google.common.collect.Lists;
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
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SeekEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getDescription();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        List<Player> seekers;

        if (sa.hasParam("Defined")) {
            seekers = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);
        } else {
            seekers = Lists.newArrayList(sa.getActivatingPlayer());
        }

        final Game game = source.getGame();
        List<String> seekTypes = Lists.newArrayList();
        if (sa.hasParam("Types")) {
            seekTypes.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        } else {
            seekTypes.add(sa.getParamOrDefault("Type", "Card"));
        }

        int seekNum = sa.hasParam("Num") ? AbilityUtils.calculateAmount(source, sa.getParam("Num"), sa) : 1;

        for (Player seeker : seekers) {
            final CardZoneTable triggerList = new CardZoneTable();
            CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
            CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();
            CardCollection soughtCards = new CardCollection();
            for (String seekType : seekTypes) {
                CardCollection pool;
                if (sa.hasParam("DefinedCards")) {
                    pool = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
                } else {
                    pool = new CardCollection(seeker.getCardsIn(ZoneType.Library));
                }
                pool = CardLists.getValidCards(pool, seekType, source.getController(), source, sa);

                if (pool.isEmpty()) {
                    continue; // can't find if nothing to seek
                }

                CardCollection found = new CardCollection();
                for (int i = 0; i < seekNum; i++) {
                    Card c = Aggregates.random(pool);
                    pool.remove(c);
                    found.add(c);
                }

                for (final Card c : found) {
                    Card movedCard = null;
                    final Zone originZone = game.getZoneOf(c);
                    Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                    moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
                    moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);
                    movedCard = game.getAction().moveTo(ZoneType.Hand, c, 0, sa, moveParams);
                    soughtCards.add(movedCard);
                    if (originZone != null) {
                        triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);
                    }
                    if (sa.hasParam("RememberFound")) {
                        source.addRemembered(movedCard);
                    }
                }
            }
            if (!soughtCards.isEmpty()) {
                game.getTriggerHandler().runTrigger(TriggerType.SeekAll, AbilityKey.mapFromPlayer(seeker), false);
            }
            triggerList.triggerChangesZoneAll(game, sa);
        }
    }
}