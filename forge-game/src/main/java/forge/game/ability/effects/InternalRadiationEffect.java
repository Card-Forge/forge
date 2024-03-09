package forge.game.ability.effects;

import java.util.Map;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityGainLifeRadiation;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class InternalRadiationEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        
        int numRad = p.getCounters(CounterEnumType.RAD);
        
        // TODO move mill part to GameAction
        final CardZoneTable table = new CardZoneTable(game.getLastStateBattlefield(), game.getLastStateGraveyard());
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        AbilityKey.addCardZoneTableParams(moveParams, table);

        final CardCollectionView milled = p.mill(numRad, ZoneType.Graveyard, sa, moveParams);
        game.getAction().reveal(milled, p, false, Localizer.getInstance().getMessage("lblMilledCards", p), false);
        game.getGameLog().add(GameLogEntryType.ZONE_CHANGE, p + " milled " +
                Lang.joinHomogenous(milled) + ".");
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
