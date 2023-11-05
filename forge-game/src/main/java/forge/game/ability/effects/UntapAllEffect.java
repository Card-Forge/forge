package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.Map;

public class UntapAllEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa instanceof AbilitySub) {
            return "Untap all valid cards.";
        }
        return sa.getParam("SpellDescription");
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        Map<Player, CardCollection> untapMap = Maps.newHashMap();

        CardCollectionView list = !sa.usesTargeting() && !sa.hasParam("Defined") ?
                game.getCardsIn(ZoneType.Battlefield) :
                getDefinedPlayersOrTargeted(sa).getCardsIn(ZoneType.Battlefield);

        list = AbilityUtils.filterListByType(list, sa.getParamOrDefault("ValidCards", ""), sa);

        Player untapper = activator;

        for (Card c : list) {
            if (sa.hasParam("ControllerUntaps")) {
                untapper = c.getController();
            }
            if (c.untap(true))  {
                    untapMap.computeIfAbsent(untapper, i -> new CardCollection()).add(c);
                    if (sa.hasParam("RememberUntapped")) card.addRemembered(c);

            }
        }

        if (!untapMap.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Map, untapMap);
            game.getTriggerHandler().runTrigger(TriggerType.UntapAll, runParams, false);
        }
    }
}
