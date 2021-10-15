package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class PermanentEffect extends SpellAbilityEffect {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.
     * SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        CardZoneTable table = new CardZoneTable();
        ZoneType previousZone = host.getZone().getZoneType();

        host.setController(sa.getActivatingPlayer(), 0);

        CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
        CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

        Map<AbilityKey, Object> moveParams = Maps.newEnumMap(AbilityKey.class);
        moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
        moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);

        final Card c = game.getAction().moveToPlay(host, host.getController(), sa, moveParams);
        sa.setHostCard(c);

        // some extra for Dashing
        if (sa.isDash() && c.isInPlay()) {
            c.setSVar("EndOfTurnLeavePlay", "Dash");
            registerDelayedTrigger(sa, "Hand", Lists.newArrayList(c));
        }

        ZoneType newZone = c.getZone().getZoneType();
        if (newZone != previousZone) {
            table.put(previousZone, newZone, c);
        }
        table.triggerChangesZoneAll(game, sa);
    }
}
