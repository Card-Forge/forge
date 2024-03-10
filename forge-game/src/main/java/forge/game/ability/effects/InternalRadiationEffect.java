package forge.game.ability.effects;

import java.util.Map;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityGainLifeRadiation;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class InternalRadiationEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();

        int numRad = p.getCounters(CounterEnumType.RAD);

        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        final CardZoneTable table = AbilityKey.addCardZoneTableParams(moveParams, sa);

        final CardCollectionView milled = game.getAction().mill(new PlayerCollection(p), numRad, ZoneType.Graveyard, sa, moveParams);
        table.triggerChangesZoneAll(game, sa);
        int n = CardLists.count(milled, Predicates.not(CardPredicates.Presets.LANDS));
        
        if (StaticAbilityGainLifeRadiation.gainLifeRadiation(p)) {
            p.gainLife(n, sa.getHostCard(), sa);
        } else {
            final Map<Player, Integer> lossMap = Maps.newHashMap();
            final int lost = p.loseLife(n, false, false);
            if (lost > 0) {
                lossMap.put(p, lost);
            }
            if (!lossMap.isEmpty()) { // Run triggers if any player actually lost life
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPIMap(lossMap);
                game.getTriggerHandler().runTrigger(TriggerType.LifeLostAll, runParams, false);
            }
        }
        
        // and remove n rad counter
        p.removeRadCounters(n);
    }

}
