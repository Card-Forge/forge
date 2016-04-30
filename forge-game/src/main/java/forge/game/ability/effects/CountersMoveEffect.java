package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CountersMoveEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        Card source = null;
        List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");
        
        if (srcCards.size() > 0) {
            source = srcCards.get(0);
        }
        final List<Card> tgtCards = getDefinedCardsOrTargeted(sa);
        final String countername = sa.getParam("CounterType");
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CounterNum"), sa);

        sb.append("Move ");
        if ("Any".matches(countername)) {
            if (amount == 1) {
                sb.append("a counter");
            } else {
                sb.append(amount).append(" ").append(" counter");
            }
        } else {   
            sb.append(amount).append(" ").append(countername).append(" counter");
        }
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from ").append(source).append(" to ");
        try{
            sb.append(tgtCards.get(0));
        } catch(final IndexOutOfBoundsException exception) {
            System.out.println(String.format("Somehow this is missing targets? %s", source.toString()));
        }

        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final String counterName = sa.getParam("CounterType");
        final Game game = host.getGame();
        int cntToMove = 0;
        if (!sa.getParam("CounterNum").equals("All")) {
            cntToMove = AbilityUtils.calculateAmount(host, sa.getParam("CounterNum"), sa);
        }
        
        CounterType cType = null;
        try {
            cType = AbilityUtils.getCounterType(counterName, sa);
        } catch (Exception e) {
            if (!counterName.matches("Any")) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return;
            }
        }

        Card source = null;
        List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");
        if (srcCards.size() > 0) {
            source = srcCards.get(0);
        }
        if (sa.getParam("CounterNum").equals("All")) {
            cntToMove = source.getCounters(cType);
        }
        List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        for (final Card dest : tgtCards) {
            if (null != source && null != dest) {
                // rule 121.5: If the first and second objects are the same object, nothing happens
                if (source.equals(dest)) {
                    continue;
                }
                Card cur = game.getCardState(dest);
                if (cur.getTimestamp() != dest.getTimestamp()) {
                    // Test to see if the card we're trying to add is in the expected state
                    continue;
                }

                if (!"Any".matches(counterName)) {
                    if (dest.canReceiveCounters(cType)
                            && source.getCounters(cType) >= cntToMove) {
                        dest.addCounter(cType, cntToMove, true);
                        source.subtractCounter(cType, cntToMove);
                    }
                } else {
                    if (dest.hasKeyword("CARDNAME can't have counters placed on it.")) {
                        return;
                    }
                    boolean canPlaceM1M1Counters = true;
                    for (final Card c : dest.getController().getCreaturesInPlay()) {//Melira, Sylvok Outcast
                        if (c.hasKeyword("Creatures you control can't have -1/-1 counters placed on them.")) {
                            canPlaceM1M1Counters = false;
                        }
                    }
                    while (cntToMove > 0 && source.hasCounters()) {
                        final Map<CounterType, Integer> tgtCounters = source.getCounters();
                        
                        final List<CounterType> typeChoices = new ArrayList<CounterType>();
                        // get types of counters
                        for (CounterType ct : tgtCounters.keySet()) {
                            if (ct != CounterType.M1M1 || canPlaceM1M1Counters) {
                                typeChoices.add(ct);
                            }
                        }
                        if (typeChoices.isEmpty()) {
                            return;
                        }
                        
                        PlayerController pc = sa.getActivatingPlayer().getController();
                        CounterType chosenType = pc.chooseCounterType(typeChoices, sa, "Select type counters to remove");

                        String prompt = "Select the number of " + chosenType.getName() + " counters to remove";
                        int chosenAmount = pc.chooseNumber(sa, prompt, 1, Math.min(tgtCounters.get(chosenType), cntToMove));
                        dest.addCounter(chosenType, chosenAmount, true);
                        source.subtractCounter(chosenType, chosenAmount);
                        cntToMove -= chosenAmount;
                    }
                }
            }
        }
    } // moveCounterResolve
}
