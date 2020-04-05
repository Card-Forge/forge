package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
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
import forge.util.Localizer;
import forge.util.CardTranslation;

import java.util.Map;
import java.util.Iterator;
import java.util.List;

public class CountersPutEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility spellAbility) {
        final StringBuilder stringBuilder = new StringBuilder();
        final Card card = spellAbility.getHostCard();
        final boolean dividedAsYouChoose = spellAbility.hasParam("DividedAsYouChoose");

        final int amount = AbilityUtils.calculateAmount(card, spellAbility.getParamOrDefault("CounterNum", "1"), spellAbility);
        if (spellAbility.hasParam("Bolster")) {
            stringBuilder.append("Bolster ").append(amount);
            return stringBuilder.toString();
        }
        if (dividedAsYouChoose) {
            stringBuilder.append("Distribute ");
        } else {
            stringBuilder.append("Put ");
        }
        if (spellAbility.hasParam("UpTo")) {
            stringBuilder.append("up to ");
        }

        stringBuilder.append(amount).append(" ");

        String type = spellAbility.getParam("CounterType");
        if (type.equals("ExistingCounter")) {
            stringBuilder.append("of an existing counter");
        } else {
            stringBuilder.append(CounterType.valueOf(type).getName()).append(" counter");
        }

        if (amount != 1) {
            stringBuilder.append("s");
        }

        if (dividedAsYouChoose) {
            stringBuilder.append(" among ");
        } else {
            stringBuilder.append(" on ");
        }

        // if use targeting we show all targets and corresponding counters
        if(spellAbility.usesTargeting()) {
            final List<Card> targetCards = SpellAbilityEffect.getTargetCards(spellAbility);
            for(int i = 0; i < targetCards.size(); i++) {
                Card targetCard = targetCards.get(i);
                stringBuilder.append(targetCard);
                if (spellAbility.getTargetRestrictions().getDividedMap().get(targetCard) != null) // fix null counter stack description
                    stringBuilder.append(" (").append(spellAbility.getTargetRestrictions().getDividedMap().get(targetCard)).append(" counter)");

                if(i == targetCards.size() - 2) {
                    stringBuilder.append(" and ");
                }
                else if(i + 1 < targetCards.size()) {
                    stringBuilder.append(", ");
                }
            }
        } else {
            final List<Card> targetCards = SpellAbilityEffect.getTargetCards(spellAbility);
            final Iterator<Card> it = targetCards.iterator();
            while (it.hasNext()) {
                final Card targetCard = it.next();
                if (targetCard.isFaceDown()) {
                    stringBuilder.append("Morph");
                } else {
                    stringBuilder.append(targetCard);
                }

                if (it.hasNext()) {
                    stringBuilder.append(", ");
                }
            }
        }
        stringBuilder.append(".");
        return stringBuilder.toString();
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
            tgtCards.addAll(pc.chooseCardsForEffect(leastToughness, sa, Localizer.getInstance().getMessage("lblChooseACreatureWithLeastToughness"), 1, 1, false));
            tgtObjects.addAll(tgtCards);
        } else if (sa.hasParam("Choices")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            CardCollection choices = new CardCollection(game.getCardsIn(choiceZone));

            int n = sa.hasParam("ChoiceAmount") ? Integer.parseInt(sa.getParam("ChoiceAmount")) : 1;

            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, card);

            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseaCard") + " ";
            tgtObjects.addAll(new CardCollection(pc.chooseCardsForEffect(choices, sa, title, n, n, !sa.hasParam("ChoiceOptional"))));
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
                    GameEntity entity = (GameEntity) obj;
                    // get types of counters
                    for (CounterType ct : entity.getCounters().keySet()) {
                        if (entity.canReceiveCounters(ct)) {
                            choices.add(ct);
                        }
                    }
                }

                if (eachExistingCounter) {
                    for (CounterType ct : choices) {
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
                    sb.append(Localizer.getInstance().getMessage("lblSelectCounterTypeAddTo") + " ");
                    sb.append(obj);
                    counterType = pc.chooseCounterType(choices, sa, sb.toString(), params);
                }
            }

            if (obj instanceof Card) {
                boolean counterAdded = false;
                counterAmount = sa.usesTargeting() && sa.hasParam("DividedAsYouChoose") ? sa.getTargetRestrictions().getDividedValue(gameCard) : counterAmount;
                if (!sa.usesTargeting() || gameCard.canBeTargetedBy(sa)) {
                    if (max != -1) {
                        counterAmount = Math.max(Math.min(max - gameCard.getCounters(counterType), counterAmount), 0);
                    }
                    if (sa.hasParam("UpTo")) {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Target", obj);
                        params.put("CounterType", counterType);
                        counterAmount = pc.chooseNumber(sa, Localizer.getInstance().getMessage("lblHowManyCounters"), 0, counterAmount, params);
                    }

                    // Adapt need extra logic
                    if (sa.hasParam("Adapt")) {
                        if (!(gameCard.getCounters(CounterType.P1P1) == 0
                                || gameCard.hasKeyword("CARDNAME adapts as though it had no +1/+1 counters"))) {
                            continue;
                        }
                    }

                    if (sa.hasParam("Tribute")) {
                        // make a copy to check if it would be on the battlefield 
                        Card noTributeLKI = CardUtil.getLKICopy(gameCard);
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

                        String message = Localizer.getInstance().getMessage("lblDoYouWantPutTargetP1P1CountersOnCard", String.valueOf(counterAmount), CardTranslation.getTranslatedName(gameCard.getName()));
                        Player chooser = pc.chooseSingleEntityForEffect(activator.getOpponents(), sa, Localizer.getInstance().getMessage("lblChooseAnOpponent"));

                        if (chooser.getController().confirmAction(sa, PlayerActionConfirmMode.Tribute, message)) {
                            gameCard.setTributed(true);
                        } else {
                            continue;
                        }
                    }
                    final Zone zone = gameCard.getGame().getZoneOf(gameCard);
                    if (zone == null || zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Stack)) {
                        if (etbcounter) {
                            gameCard.addEtbCounter(counterType, counterAmount, placer);
                        } else {
                            if (gameCard.addCounter(counterType, counterAmount, placer, true, table) > 0) {
                                counterAdded = true;
                            }
                        }
                        if (remember) {
                            final int value = gameCard.getTotalCountersToAdd();
                            gameCard.addCountersAddedBy(card, counterType, value);
                        }

                        if (sa.hasParam("Evolve")) {
                            game.getTriggerHandler().runTrigger(TriggerType.Evolved, AbilityKey.mapFromCard(gameCard), false);
                        }
                        if (sa.hasParam("Monstrosity")) {
                            gameCard.setMonstrous(true);
                            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(gameCard);
                            runParams.put(AbilityKey.MonstrosityAmount, counterAmount);
                            game.getTriggerHandler().runTrigger(TriggerType.BecomeMonstrous, runParams, false);
                        }
                        if (sa.hasParam("Renown")) {
                            gameCard.setRenowned(true);
                            game.getTriggerHandler().runTrigger(TriggerType.BecomeRenowned, AbilityKey.mapFromCard(gameCard), false);
                        }
                        if (sa.hasParam("Adapt")) {
                            // need to remove special keyword
                            gameCard.removeHiddenExtrinsicKeyword("CARDNAME adapts as though it had no +1/+1 counters");
                            game.getTriggerHandler().runTrigger(TriggerType.Adapt, AbilityKey.mapFromCard(gameCard), false);
                        }
                    } else {
                        // adding counters to something like re-suspend cards
                        // etbcounter should apply multiplier
                        if (etbcounter) {
                            gameCard.addEtbCounter(counterType, counterAmount, placer);
                        } else {
                            if (gameCard.addCounter(counterType, counterAmount, placer, false, table) > 0) {
                                counterAdded = true;
                            }
                        }
                    }
                    if (rememberCards && counterAdded) {
                        card.addRemembered(gameCard);
                    }
                    game.updateLastStateForCard(gameCard);
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
