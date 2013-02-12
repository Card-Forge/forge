package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CounterType;
import forge.Singletons;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class CountersRemoveEffect extends SpellEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final String counterName = sa.getParam("CounterType");

        final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("CounterNum"), sa);

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

        final Card card = sa.getSourceCard();
        final String type = sa.getParam("CounterType");
        int counterAmount = 0;
        if (!sa.getParam("CounterNum").equals("All")) {
            counterAmount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("CounterNum"), sa);
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

        final Target tgt = sa.getTarget();

        boolean rememberRemoved = false;
        if (sa.hasParam("RememberRemoved")) {
            rememberRemoved = true;
        }
        for (final Card tgtCard : getTargetCards(sa)) {
            if ((tgt == null) || tgtCard.canBeTargetedBy(sa)) {
                final Zone zone = Singletons.getModel().getGame().getZoneOf(tgtCard);
                if (sa.getParam("CounterNum").equals("All")) {
                    counterAmount = tgtCard.getCounters(counterType);
                }

                if (type.matches("Any")) {
                    while (counterAmount > 0 && tgtCard.hasCounters()) {
                        final Map<CounterType, Integer> tgtCounters = tgtCard.getCounters();
                        CounterType chosenType = null;
                        int chosenAmount;
                        if (sa.getActivatingPlayer().isHuman()) {
                            final ArrayList<CounterType> typeChoices = new ArrayList<CounterType>();
                            // get types of counters
                            for (CounterType key : tgtCounters.keySet()) {
                                if (tgtCounters.get(key) > 0) {
                                    typeChoices.add(key);
                                }
                            }
                            if (typeChoices.size() > 1) {
                                String prompt = "Select type counters to remove";
                                chosenType = GuiChoose.one(prompt, typeChoices);
                            } else {
                                chosenType = typeChoices.get(0);
                            }
                            chosenAmount = tgtCounters.get(chosenType);
                            if (chosenAmount > counterAmount) {
                                chosenAmount = counterAmount;
                            }
                            // make list of amount choices

                            if (chosenAmount > 1) {
                                final List<Integer> choices = new ArrayList<Integer>();
                                for (int i = 1; i <= chosenAmount; i++) {
                                    choices.add(Integer.valueOf(i));
                                }
                                String prompt = "Select the number of " + chosenType.getName() + " counters to remove";
                                chosenAmount = GuiChoose.one(prompt, choices);
                            }
                        } else {
                            // TODO: ArsenalNut (06 Feb 12)computer needs
                            // better logic to pick a counter type and probably
                            // an initial target
                            // find first nonzero counter on target
                            for (Object key : tgtCounters.keySet()) {
                                if (tgtCounters.get(key) > 0) {
                                    chosenType = (CounterType) key;
                                    break;
                                }
                            }
                            // subtract all of selected type
                            chosenAmount = tgtCounters.get(chosenType);
                            if (chosenAmount > counterAmount) {
                                chosenAmount = counterAmount;
                            }
                        }
                        tgtCard.subtractCounter(chosenType, chosenAmount);
                        if (rememberRemoved) {
                            for (int i = 0; i < chosenAmount; i++) {
                                card.addRemembered(chosenType);
                            }
                        }
                        counterAmount -= chosenAmount;
                    }
                } else {
                    if (zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Exile)) {
                        if (sa.hasParam("UpTo") && sa.getActivatingPlayer().isHuman()) {
                            final ArrayList<String> choices = new ArrayList<String>();
                            for (int i = 0; i <= counterAmount; i++) {
                                choices.add("" + i);
                            }
                            final String prompt = "Select the number of " + type + " counters to remove";
                            final String o = GuiChoose.one(prompt, choices);
                            counterAmount = Integer.parseInt(o);
                        }
                    }
                    if (rememberRemoved) {
                        if (counterAmount > tgtCard.getCounters(counterType)) {
                            counterAmount = tgtCard.getCounters(counterType);
                        }
                        for (int i = 0; i < counterAmount; i++) {
                            card.addRemembered(counterType);
                        }
                    }
                    tgtCard.subtractCounter(counterType, counterAmount);
                }
            }
        }
    }

}
