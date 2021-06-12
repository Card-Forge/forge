package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

/** 
 * API for adding to or subtracting from existing counters on a target.
 *
 */
public class CountersPutOrRemoveEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append(sa.getActivatingPlayer().getName());

        if (sa.hasParam("CounterType")) {
            CounterType ctype = CounterType.getType(sa.getParam("CounterType"));
            sb.append(" removes a ").append(ctype.getName());
            sb.append(" counter from or put another ").append(ctype.getName()).append(" counter on ");
        } else {
            sb.append(" removes a counter from or puts another of those counters on ");
        }

        sb.append(Lang.joinHomogenous(getTargets(sa)));

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final int counterAmount = AbilityUtils.calculateAmount(source, sa.getParam("CounterNum"), sa);

        if (counterAmount <= 0) {
            return;
        }

        CounterType ctype = null;
        if (sa.hasParam("CounterType")) {
            ctype = CounterType.getType(sa.getParam("CounterType"));
        }
        
        GameEntityCounterTable table = new GameEntityCounterTable();

        for (final Card tgtCard : getDefinedCardsOrTargeted(sa)) {
            Card gameCard = game.getCardState(tgtCard, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtCard.equalsWithTimestamp(gameCard)) {
                continue;
            }
            if (!sa.usesTargeting() || gameCard.canBeTargetedBy(sa)) {
                if (gameCard.hasCounters()) {
                    if (sa.hasParam("EachExistingCounter")) {
                        for (CounterType listType : Lists.newArrayList(gameCard.getCounters().keySet())) {
                            addOrRemoveCounter(sa, gameCard, listType, counterAmount, table);
                        }
                    } else {
                        addOrRemoveCounter(sa, gameCard, ctype, counterAmount, table);
                    }
                    game.updateLastStateForCard(gameCard);
                }
            }
        }
        table.triggerCountersPutAll(game);
    }

    private void addOrRemoveCounter(final SpellAbility sa, final Card tgtCard, CounterType ctype,
            final int counterAmount, GameEntityCounterTable table) {
        final Player pl = sa.getActivatingPlayer();
        final PlayerController pc = pl.getController();

        Map<String, Object> params = Maps.newHashMap();
        params.put("Target", tgtCard);

        List<CounterType> list = Lists.newArrayList(tgtCard.getCounters().keySet());
        if (ctype != null) {
            list = Lists.newArrayList(ctype);
        }

        String prompt = Localizer.getInstance().getMessage("lblSelectCounterTypeToAddOrRemove");
        CounterType chosenType = pc.chooseCounterType(list, sa, prompt, params);

        params.put("CounterType", chosenType);
        prompt = Localizer.getInstance().getMessage("lblWhatToDoWithTargetCounter",  chosenType.getName()) + " ";
        Boolean putCounter = pc.chooseBinary(sa, prompt, BinaryChoiceType.AddOrRemove, params);

        if (putCounter) {
            // Put another of the chosen counter on card
            final Zone zone = tgtCard.getGame().getZoneOf(tgtCard);
            
            boolean apply = zone == null || zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Stack);

            tgtCard.addCounter(chosenType, counterAmount, pl, sa, apply, table);
        } else {
            tgtCard.subtractCounter(chosenType, counterAmount);
        }
    }
}
