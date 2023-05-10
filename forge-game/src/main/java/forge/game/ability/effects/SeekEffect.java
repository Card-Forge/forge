package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
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
        final Game game = source.getGame();

        List<String> seekTypes = Lists.newArrayList();
        if (sa.hasParam("Types")) {
            seekTypes.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        } else {
            seekTypes.add(sa.getParamOrDefault("Type", "Card"));
        }

        int seekNum = AbilityUtils.calculateAmount(source, sa.getParamOrDefault("Num", "1"), sa);
        if (seekNum <= 0) {
            return;
        }

        final CardZoneTable triggerList = new CardZoneTable();
        CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
        CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

        for (Player seeker : getTargetPlayers(sa)) {
            if (!seeker.isInGame()) {
                continue;
            }

            CardCollection soughtCards = new CardCollection();

            for (String seekType : seekTypes) {
                CardCollection pool;
                if (sa.hasParam("DefinedCards")) {
                    pool = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
                } else {
                    pool = new CardCollection(seeker.getCardsIn(ZoneType.Library));
                }
                if (!seekType.equals("Card")) {
                    pool = CardLists.getValidCards(pool, seekType, source.getController(), source, sa);
                }
                if (pool.isEmpty()) {
                    continue; // can't find if nothing to seek
                }

                for (final Card c : Aggregates.random(pool, seekNum)) {

                    Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                    moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
                    moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);
                    Card movedCard = game.getAction().moveToHand(c, sa, moveParams);
                    ZoneType resultZone = movedCard.getZone().getZoneType();
                    if (!resultZone.equals(ZoneType.Library)) { // as long as it moved we add to triggerList
                        triggerList.put(ZoneType.Library, movedCard.getZone().getZoneType(), movedCard);
                    }
                    if (resultZone.equals(ZoneType.Hand)) { // if it went to hand as planned, consider it "sought"
                        soughtCards.add(movedCard);
                    }

                }
            }
            if (!soughtCards.isEmpty()) {
                if (sa.hasParam("RememberFound")) {
                    source.addRemembered(soughtCards);
                }
                if (sa.hasParam("ImprintFound")) {
                    source.addImprintedCards(soughtCards);
                }
                game.getTriggerHandler().runTrigger(TriggerType.SeekAll, AbilityKey.mapFromPlayer(seeker), false);
            }
        }
        triggerList.triggerChangesZoneAll(game, sa);
    }
}
