package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;

public class CountersRemoveEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final String counterName = sa.getParam("CounterType");

        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CounterNum"), sa);

        sb.append("Remove ");
        if (sa.hasParam("UpTo")) {
            sb.append("up to ");
        }
        if ("Any".matches(counterName)) {
            if (amount == 1) {
                sb.append("a counter");
            } else {
                sb.append(amount).append(" ").append(" counter");
            }
        } else {
            sb.append(amount).append(" ").append(CounterType.valueOf(counterName).getName()).append(" counter");
        }
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from");

        for (final Card c : getTargetCards(sa)) {
            sb.append(" ").append(c);
        }

        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {

        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final String type = sa.getParam("CounterType");
        int cntToRemove = 0;
        if (!sa.getParam("CounterNum").equals("All") && !sa.getParam("CounterNum").equals("Remembered")) {
            cntToRemove = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CounterNum"), sa);
        }

        CounterType counterType = null;

        try {
            counterType = AbilityUtils.getCounterType(type, sa);
        } catch (Exception e) {
            if (!type.matches("Any")) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return;
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        boolean rememberRemoved = false;
        if (sa.hasParam("RememberRemoved")) {
            rememberRemoved = true;
        }
        for (final Card tgtCard : getTargetCards(sa)) {
            if ((tgt == null) || tgtCard.canBeTargetedBy(sa)) {
                final Zone zone = game.getZoneOf(tgtCard);
                if (sa.getParam("CounterNum").equals("All")) {
                    cntToRemove = tgtCard.getCounters(counterType);
                } else if (sa.getParam("CounterNum").equals("Remembered")) {
                    cntToRemove = tgtCard.getCountersAddedBy(card, counterType);
                }
                
                PlayerController pc = sa.getActivatingPlayer().getController();
                
                if (type.matches("Any")) {
                    while (cntToRemove > 0 && tgtCard.hasCounters()) {
                        final Map<CounterType, Integer> tgtCounters = tgtCard.getCounters();

                        CounterType chosenType = pc.chooseCounterType(ImmutableList.copyOf(tgtCounters.keySet()), sa, "Select type of counters to remove");
                        String prompt = "Select the number of " + chosenType.getName() + " counters to remove";
                        int chosenAmount = pc.chooseNumber(sa, prompt, 1, Math.min(cntToRemove, tgtCounters.get(chosenType)));

                        tgtCard.subtractCounter(chosenType, chosenAmount);
                        if (rememberRemoved) {
                            for (int i = 0; i < chosenAmount; i++) {
                                card.addRemembered(Pair.of(chosenType, i));
                            }
                        }
                        cntToRemove -= chosenAmount;
                    }
                } else {
                    if (zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Exile)) {
                        if (sa.hasParam("UpTo")) 
                            cntToRemove = pc.chooseNumber(sa, "Select the number of " + type + " counters to remove", 0, cntToRemove);
                    }
                    if (rememberRemoved) {
                        if (cntToRemove > tgtCard.getCounters(counterType)) {
                            cntToRemove = tgtCard.getCounters(counterType);
                        }
                        for (int i = 0; i < cntToRemove; i++) {
                            card.addRemembered(Pair.of(counterType, i));
                        }
                    }
                    tgtCard.subtractCounter(counterType, cntToRemove);
                }
            }
        }
    }

}
