package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.Counters;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class CountersRemoveEffect extends SpellEffect { 
    /**
     * <p>
     * removeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {

        final Card card = sa.getSourceCard();
        final String type = params.get("CounterType");
        int counterAmount = 0;
        if (!params.get("CounterNum").equals("All")) {
            counterAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("CounterNum"), sa);
        }

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        boolean rememberRemoved = false;
        if (params.containsKey("RememberRemoved")) {
            rememberRemoved = true;
        }
        for (final Card tgtCard : tgtCards) {
            if ((tgt == null) || tgtCard.canBeTargetedBy(sa)) {
                final Zone zone = Singletons.getModel().getGame().getZoneOf(tgtCard);
                if (params.get("CounterNum").equals("All")) {
                    counterAmount = tgtCard.getCounters(Counters.valueOf(type));
                }

                if (type.matches("Any")) {
                    while (counterAmount > 0 && tgtCard.getNumberOfCounters() > 0) {
                        final Map<Counters, Integer> tgtCounters = tgtCard.getCounters();
                        Counters chosenType = null;
                        int chosenAmount;
                        if (sa.getActivatingPlayer().isHuman()) {
                            final ArrayList<Counters> typeChoices = new ArrayList<Counters>();
                            // get types of counters
                            for (Counters key : tgtCounters.keySet()) {
                                if (tgtCounters.get(key) > 0) {
                                    typeChoices.add(key);
                                }
                            }
                            if (typeChoices.size() > 1) {
                                String prompt = "Select type counters to remove";
                                chosenType = GuiChoose.one(prompt, typeChoices);
                            }
                            else {
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
                        }
                        else {
                            // TODO: ArsenalNut (06 Feb 12) - computer needs better logic to pick a counter type and probably an initial target
                            // find first nonzero counter on target
                            for (Object key : tgtCounters.keySet()) {
                                if (tgtCounters.get(key) > 0) {
                                    chosenType = (Counters) key;
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
                }
                else {
                    if (zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Exile)) {
                        if (params.containsKey("UpTo") && sa.getActivatingPlayer().isHuman()) {
                            final ArrayList<String> choices = new ArrayList<String>();
                            for (int i = 0; i <= counterAmount; i++) {
                                choices.add("" + i);
                            }
                            final String prompt = "Select the number of " + type + " counters to remove";
                            final String o = GuiChoose.one(prompt, choices);
                            counterAmount = Integer.parseInt(o);
                        }
                    }
                    tgtCard.subtractCounter(Counters.valueOf(type), counterAmount);
                    if (rememberRemoved) {
                        for (int i = 0; i < counterAmount; i++) {
                            card.addRemembered(Counters.valueOf(type));
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * removeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(card).append(" - ");
        } else {
            sb.append(" ");
        }
    
        final String counterName = params.get("CounterType");
    
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("CounterNum"), sa);
    
        sb.append("Remove ");
        if (params.containsKey("UpTo")) {
            sb.append("up to ");
        }
        if ("Any".matches(counterName)) {
            if (amount == 1) {
                sb.append("a counter");
            }
            else {
                sb.append(amount).append(" ").append(" counter");
            }
        }
        else {
            sb.append(amount).append(" ").append(Counters.valueOf(counterName).getName()).append(" counter");
        }
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from");
    
        ArrayList<Card> tgtCards;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }
        for (final Card c : tgtCards) {
            sb.append(" ").append(c);
        }
    
        sb.append(".");
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    // *******************************************
    // ********** Proliferate ********************
    // *******************************************


    /**
     * <p>
     * proliferateShouldPlayAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    
}