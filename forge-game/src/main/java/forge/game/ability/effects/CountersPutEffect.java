package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.Map;
import java.util.Iterator;
import java.util.List;

public class CountersPutEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card card = sa.getHostCard();
        final boolean dividedAsYouChoose = sa.hasParam("DividedAsYouChoose");


        final int amount = AbilityUtils.calculateAmount(card, sa.getParam("CounterNum"), sa);
        if (sa.hasParam("Bolster")) {
            sb.append("Bolster ").append(amount);
            return sb.toString();
        }
        if (dividedAsYouChoose) {
            sb.append("Distribute ");
        } else {
            sb.append("Put ");
        }
        if (sa.hasParam("UpTo")) {
            sb.append("up to ");
        }

        sb.append(amount).append(" ");

        String type = sa.getParam("CounterType");
        if (type.equals("ExistingCounter")) {
            sb.append("of an existing counter");
        } else {

            sb.append( CounterType.valueOf(type).getName()).append(" counter");
        }
        if (amount != 1) {
            sb.append("s");
        }
        if (dividedAsYouChoose) {
            sb.append(" among ");
        } else {
            sb.append(" on ");
        }
        final List<Card> tgtCards = getTargetCards(sa);

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            final Card tgtC = it.next();
            if (tgtC.isFaceDown()) {
                sb.append("Morph");
            } else {
                sb.append(tgtC);
            }

            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();

        String strTyp = sa.getParam("CounterType");
        CounterType counterType = null;
        boolean existingCounter = strTyp.equals("ExistingCounter");
        boolean eachExistingCounter = sa.hasParam("EachExistingCounter");
        String amount = sa.getParamOrDefault("CounterNum", "1");

        if (!existingCounter) {
            try {
                counterType = AbilityUtils.getCounterType(strTyp, sa);
            } catch (Exception e) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return;
            }
        }

        final boolean etbcounter = sa.hasParam("ETB");
        final boolean remember = sa.hasParam("RememberCounters");
        final boolean rememberCards = sa.hasParam("RememberCards");
        int counterAmount = AbilityUtils.calculateAmount(sa.getHostCard(), amount, sa);
        final int max = sa.hasParam("MaxFromEffect") ? Integer.parseInt(sa.getParam("MaxFromEffect")) : -1;

        CardCollection tgtCards = new CardCollection();
        List<GameObject> tgtObjects = Lists.newArrayList();
        if (sa.hasParam("Bolster")) {
            CardCollection creatsYouCtrl = CardLists.filter(activator.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            CardCollection leastToughness = new CardCollection(Aggregates.listWithMin(creatsYouCtrl, CardPredicates.Accessors.fnGetDefense));
            tgtCards.addAll(pc.chooseCardsForEffect(leastToughness, sa, "Choose a creature with the least toughness", 1, 1, false));
            tgtObjects.addAll(tgtCards);
        } else {
            tgtObjects.addAll(getDefinedOrTargeted(sa, "Defined"));
        }

        for (final GameObject obj : tgtObjects) {
            if (existingCounter) {
                final List<CounterType> choices = Lists.newArrayList();
                if (obj instanceof GameEntity) {
                    GameEntity entity = (GameEntity)obj;
                    // get types of counters
                    for (CounterType ct : entity.getCounters().keySet()) {
                        if (entity.canReceiveCounters(ct)) {
                            choices.add(ct);
                        }
                    }
                }

                if (eachExistingCounter) {
                    for(CounterType ct : choices) {
                        if (obj instanceof Player) {
                            ((Player) obj).addCounter(ct, counterAmount, card, true);
                        }
                        if (obj instanceof Card) {
                            ((Card) obj).addCounter(ct, counterAmount, card, true);
                        }
                    }
                    continue;
                }

                if (choices.isEmpty()) {
                    continue;
                } else if (choices.size() == 1) {
                    counterType = choices.get(0);
                } else {
                    Map<String, Object> params = Maps.newHashMap();
                    params.put("Target", obj);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Select counter type to add to ");
                    sb.append(obj);
                    counterType = pc.chooseCounterType(choices, sa, sb.toString(), params);
                }
            }

            if (obj instanceof Card) {
                Card tgtCard = (Card) obj;
                counterAmount = sa.usesTargeting() && sa.hasParam("DividedAsYouChoose") ? sa.getTargetRestrictions().getDividedValue(tgtCard) : counterAmount;
                if (!sa.usesTargeting() || tgtCard.canBeTargetedBy(sa)) {
                    if (max != -1) {
                        counterAmount = Math.max(Math.min(max - tgtCard.getCounters(counterType), counterAmount), 0);
                    }
                    if (sa.hasParam("UpTo")) {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Target", obj);
                        params.put("CounterType", counterType);
                        counterAmount = pc.chooseNumber(sa, "How many counters?", 0, counterAmount, params);
                    }

                    if (sa.hasParam("Tribute")) {
                        String message = "Do you want to put " + counterAmount + " +1/+1 counters on " + tgtCard + " ?";
                        Player chooser = pc.chooseSingleEntityForEffect(activator.getOpponents(), sa, "Choose an opponent");
                        if (chooser.getController().confirmAction(sa, PlayerActionConfirmMode.Tribute, message)) {
                            tgtCard.setTributed(true);
                        } else {
                            continue;
                        }
                    }
                    if (rememberCards) {
                        card.addRemembered(tgtCard);
                    }
                    final Zone zone = tgtCard.getGame().getZoneOf(tgtCard);
                    if (zone == null || zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Stack)) {
                        if (etbcounter) {
                            tgtCard.addEtbCounter(counterType, counterAmount, card);
                        } else {
                            tgtCard.addCounter(counterType, counterAmount, card, true);
                        }
                        if (remember) {
                            final int value = tgtCard.getTotalCountersToAdd();
                            tgtCard.addCountersAddedBy(card, counterType, value);
                        }

                        if (sa.hasParam("Evolve")) {
                            final Map<String, Object> runParams = Maps.newHashMap();
                            runParams.put("Card", tgtCard);
                            game.getTriggerHandler().runTrigger(TriggerType.Evolved, runParams, false);
                        }
                        if (sa.hasParam("Monstrosity")) {
                            tgtCard.setMonstrous(true);
                            tgtCard.setMonstrosityNum(counterAmount);
                            final Map<String, Object> runParams = Maps.newHashMap();
                            runParams.put("Card", tgtCard);
                            game.getTriggerHandler().runTrigger(TriggerType.BecomeMonstrous, runParams, false);
                        }
                        if (sa.hasParam("Renown")) {
                            tgtCard.setRenowned(true);
                            final Map<String, Object> runParams = Maps.newHashMap();
                            runParams.put("Card", tgtCard);
                            game.getTriggerHandler().runTrigger(TriggerType.BecomeRenowned, runParams, false);
                        }
                    } else {
                        // adding counters to something like re-suspend cards
                        // etbcounter should apply multiplier
                        if (etbcounter) {
                            tgtCard.addEtbCounter(counterType, counterAmount, card);
                        } else {
                            tgtCard.addCounter(counterType, counterAmount, card, false);
                        }
                    }
                }
            } else if (obj instanceof Player) {
                // Add Counters to players!
                Player pl = (Player) obj;
                pl.addCounter(counterType, counterAmount, card, true);
            }
        }
    }

}
