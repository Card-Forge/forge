package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.PlayerController;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Lang;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

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
            CounterType ctype = CounterType.valueOf(sa.getParam("CounterType"));
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
        final int counterAmount = AbilityUtils.calculateAmount(source, sa.getParam("CounterNum"), sa);
        
        CounterType ctype = null;
        if (sa.hasParam("CounterType")) {
            ctype = CounterType.valueOf(sa.getParam("CounterType"));
        }
        
        for (final Card tgtCard : getDefinedCardsOrTargeted(sa)) {
            if (!sa.usesTargeting() || tgtCard.canBeTargetedBy(sa)) {
                if (tgtCard.hasCounters()) {
                    if (sa.hasParam("EachExistingCounter")) {
                        for (CounterType listType : Lists.newArrayList(tgtCard.getCounters().keySet())) {
                            addOrRemoveCounter(sa, tgtCard, listType, counterAmount);
                        }
                    } else {
                        addOrRemoveCounter(sa, tgtCard, ctype, counterAmount);
                    }
                }
            }
        }
    }

    private void addOrRemoveCounter(final SpellAbility sa, final Card tgtCard, CounterType ctype,
            final int counterAmount) {
        PlayerController pc = sa.getActivatingPlayer().getController();
        final Card source = sa.getHostCard();

        Map<String, Object> params = Maps.newHashMap();
        params.put("Target", tgtCard);

        List<CounterType> list = Lists.newArrayList(tgtCard.getCounters().keySet());
        if (ctype != null) {
            list = Lists.newArrayList(ctype);
        }

        String prompt = "Select type of counters to add or remove";
        CounterType chosenType = pc.chooseCounterType(list, sa, prompt, params);

        params.put("CounterType", chosenType);
        prompt = "What to do with that '" + chosenType.getName() + "' counter ";
        Boolean putCounter = pc.chooseBinary(sa, prompt, BinaryChoiceType.AddOrRemove, params);

        if (putCounter) {
            // Put another of the chosen counter on card
            final Zone zone = tgtCard.getGame().getZoneOf(tgtCard);
            
            boolean apply = zone == null || zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Stack);

            tgtCard.addCounter(chosenType, counterAmount, source, apply);
        } else {
            tgtCard.subtractCounter(chosenType, counterAmount);
        }
    }
}
