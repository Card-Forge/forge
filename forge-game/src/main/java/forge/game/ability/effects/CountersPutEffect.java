package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
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


        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("CounterNum", "1"), sa);
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

        Player placer = activator;
        if (sa.hasParam("Placer")) {
            final String pstr = sa.getParam("Placer");
            placer = AbilityUtils.getDefinedPlayers(sa.getHostCard(), pstr, sa).get(0);
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

        GameEntityCounterTable table = new GameEntityCounterTable();

        for (final GameObject obj : tgtObjects) {
            // check if the object is still in game or if it was moved
            Card gameCard = null;
            if (obj instanceof Card) {
                Card tgtCard = (Card) obj;
                gameCard = game.getCardState(tgtCard, null);
                // gameCard is LKI in that case, the card is not in game anymore
                // or the timestamp did change
                // this should check Self too
                if (gameCard == null || !tgtCard.equalsWithTimestamp(gameCard)) {
                    continue;
                }
            }

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
                            ((Player) obj).addCounter(ct, counterAmount, placer, true, table);
                        }
                        if (obj instanceof Card) {
                            gameCard.addCounter(ct, counterAmount, placer, true, table);
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
                Card tgtCard = gameCard;
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

                    // Adapt need extra logic
                    if (sa.hasParam("Adapt")) {
                        if (!(tgtCard.getCounters(CounterType.P1P1) == 0
                                || tgtCard.hasKeyword("CARDNAME adapts as though it had no +1/+1 counters"))) {
                            continue;
                        }
                    }

                    if (sa.hasParam("Tribute")) {
                        // make a copy to check if it would be on the battlefield 
                        Card noTributeLKI = CardUtil.getLKICopy(tgtCard);
                        // this check needs to check if this card would be on the battlefield
                        noTributeLKI.setLastKnownZone(activator.getZone(ZoneType.Battlefield));

                        // double freeze tracker, so it doesn't update view
                        game.getTracker().freeze();

                        CardCollection preList = new CardCollection(noTributeLKI);
                        game.getAction().checkStaticAbilities(false, Sets.newHashSet(noTributeLKI), preList);

                        boolean abort = !noTributeLKI.canReceiveCounters(counterType);

                        game.getAction().checkStaticAbilities(false);
                        // clear delayed changes, this check should not have updated the view
                        game.getTracker().clearDelayed();
                        // need to unfreeze tracker
                        game.getTracker().unfreeze();

                        // check if it can recive the Tribute
                        if (abort) {
                            continue;
                        }

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
                            tgtCard.addEtbCounter(counterType, counterAmount, placer);
                        } else {
                            tgtCard.addCounter(counterType, counterAmount, placer, true, table);
                        }
                        if (remember) {
                            final int value = tgtCard.getTotalCountersToAdd();
                            tgtCard.addCountersAddedBy(card, counterType, value);
                        }

                        if (sa.hasParam("Evolve")) {
                            final Map<String, Object> runParams = Maps.newHashMap();
                            runParams.put("Card", tgtCard);
                            game.getTriggerHandler().runTriggerOld(TriggerType.Evolved, runParams, false);
                        }
                        if (sa.hasParam("Monstrosity")) {
                            tgtCard.setMonstrous(true);
                            final Map<String, Object> runParams = Maps.newHashMap();
                            runParams.put("Card", tgtCard);
                            runParams.put("MonstrosityAmount", counterAmount);
                            game.getTriggerHandler().runTriggerOld(TriggerType.BecomeMonstrous, runParams, false);
                        }
                        if (sa.hasParam("Renown")) {
                            tgtCard.setRenowned(true);
                            final Map<String, Object> runParams = Maps.newHashMap();
                            runParams.put("Card", tgtCard);
                            game.getTriggerHandler().runTriggerOld(TriggerType.BecomeRenowned, runParams, false);
                        }
                        if (sa.hasParam("Adapt")) {
                            // need to remove special keyword
                            tgtCard.removeHiddenExtrinsicKeyword("CARDNAME adapts as though it had no +1/+1 counters");
                            final Map<String, Object> runParams = Maps.newHashMap();
                            runParams.put("Card", tgtCard);
                            game.getTriggerHandler().runTriggerOld(TriggerType.Adapt, runParams, false);
                        }
                    } else {
                        // adding counters to something like re-suspend cards
                        // etbcounter should apply multiplier
                        if (etbcounter) {
                            tgtCard.addEtbCounter(counterType, counterAmount, placer);
                        } else {
                            tgtCard.addCounter(counterType, counterAmount, placer, false, table);
                        }
                    }
                    game.updateLastStateForCard(tgtCard);
                }
            } else if (obj instanceof Player) {
                // Add Counters to players!
                Player pl = (Player) obj;
                pl.addCounter(counterType, counterAmount, placer, true, table);
            }
        }
        table.triggerCountersPutAll(game);
    }

}
