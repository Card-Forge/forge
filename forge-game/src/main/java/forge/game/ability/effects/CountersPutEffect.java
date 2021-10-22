package forge.game.ability.effects;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.event.GameEventRandomLog;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityAdapt;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

public class CountersPutEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility spellAbility) {
        final StringBuilder stringBuilder = new StringBuilder();
        final Card card = spellAbility.getHostCard();

        final int amount = AbilityUtils.calculateAmount(card, spellAbility.getParamOrDefault("CounterNum", "1"), spellAbility);
        //skip the StringBuilder if no targets are chosen ("up to" scenario)
        if (spellAbility.usesTargeting()) {
            final List<Card> targetCards = SpellAbilityEffect.getTargetCards(spellAbility);
            if (targetCards.size() == 0) {
                return stringBuilder.toString();
            }
        }
        if (spellAbility.hasParam("Bolster")) {
            stringBuilder.append("Bolster ").append(amount);
            return stringBuilder.toString();
        }
        boolean divAsChoose = spellAbility.isDividedAsYouChoose();
        if (divAsChoose) {
            stringBuilder.append("Distribute ");
        } else if (spellAbility.hasParam("DividedRandomly")){
            stringBuilder.append("Randomly distribute ");
        } else {
            stringBuilder.append("Put ");
        }
        if (spellAbility.hasParam("UpTo")) {
            stringBuilder.append("up to ");
        }

        String type = spellAbility.getParam("CounterType");
        if (amount == 1) {
            stringBuilder.append(Lang.startsWithVowel(type) ? "an " : "a ");
        } else {
            stringBuilder.append(Lang.getNumeral(amount)).append(" ");
        }

        stringBuilder.append(CounterType.getType(type).getName().toLowerCase()).append(" counter");
        stringBuilder.append(amount != 1 ? "s" : "").append(divAsChoose || spellAbility.hasParam("DividedRandomly")
                ? " among " : " on ");

        // if use targeting we show all targets and corresponding counters
        if(spellAbility.usesTargeting()) {
            final List<Card> targetCards = SpellAbilityEffect.getTargetCards(spellAbility);
            for(int i = 0; i < targetCards.size(); i++) {
                Card targetCard = targetCards.get(i);
                stringBuilder.append(targetCard);
                Integer v = spellAbility.getDividedValue(targetCard);
                if (v != null) // fix null counter stack description
                    stringBuilder.append(" (").append(v).append(" counter)");

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

    protected void resolvePerType(SpellAbility sa, final Player placer, CounterType counterType, int counterAmount, GameEntityCounterTable table) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();
        final boolean etbcounter = sa.hasParam("ETB");
        final boolean rememberCards = sa.hasParam("RememberCards");
        final int max = sa.hasParam("MaxFromEffect") ? Integer.parseInt(sa.getParam("MaxFromEffect")) : -1;

        boolean existingCounter = sa.hasParam("CounterType") && sa.getParam("CounterType").equals("ExistingCounter");
        boolean eachExistingCounter = sa.hasParam("EachExistingCounter");

        List<GameObject> tgtObjects = Lists.newArrayList();
        int divrem = 0;
        if (sa.hasParam("Bolster")) {
            CardCollection creatsYouCtrl = activator.getCreaturesInPlay();
            CardCollection leastToughness = new CardCollection(Aggregates.listWithMin(creatsYouCtrl, CardPredicates.Accessors.fnGetDefense));

            Map<String, Object> params = Maps.newHashMap();
            params.put("CounterType", counterType);

            Iterables.addAll(tgtObjects, activator.getController().chooseCardsForEffect(leastToughness, sa, Localizer.getInstance().getMessage("lblChooseACreatureWithLeastToughness"), 1, 1, false, params));
        } else if (sa.hasParam("Choices")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            Player chooser = activator;
            if (sa.hasParam("Chooser")) {
                List<Player> choosers = AbilityUtils.getDefinedPlayers(card, sa.getParam("Chooser"), sa);
                if (choosers.isEmpty()) {
                    return;
                }
                chooser = choosers.get(0);
            }

            int n = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("ChoiceAmount",
                    "1"), sa);
            int m = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("MinChoiceAmount",
                    sa.getParamOrDefault("ChoiceAmount", "1")), sa);

            // no choices allowed
            if (n <= 0) {
                return;
            }

            CardCollection choices = CardLists.getValidCards(game.getCardsIn(choiceZone), sa.getParam("Choices"), activator, card, sa);

            String title = Localizer.getInstance().getMessage("lblChooseaCard") + " ";
            if (sa.hasParam("ChoiceTitle")) {
                title = sa.getParam("ChoiceTitle");
                // TODO might use better message
                if (counterType != null) {
                    title += " (" + counterType.getName() + ")";
                }
            }

            Map<String, Object> params = Maps.newHashMap();
            params.put("CounterType", counterType);
            if (sa.hasParam("DividedRandomly")) {
                tgtObjects.addAll(choices);
            } else {
                Iterables.addAll(tgtObjects, chooser.getController().chooseCardsForEffect(choices, sa, title, m, n,
                        sa.hasParam("ChoiceOptional"), params));
            }
        } else {
            tgtObjects.addAll(getDefinedOrTargeted(sa, "Defined"));
        }

        if (sa.hasParam("Optional") && !pc.confirmAction
                (sa, null, Localizer.getInstance().getMessage("lblDoYouWantPutCounter"))) {
            return;
        }

        int counterRemain = counterAmount;
        if (sa.hasParam("DividedRandomly")) {
            CardCollection targets = new CardCollection();
            for (final GameObject obj : tgtObjects) { // check if each target is still OK
                if (obj instanceof Card) {
                    Card tgtCard = (Card) obj;
                    Card gameCard = game.getCardState(tgtCard, null);
                    if (gameCard == null || !tgtCard.equalsWithTimestamp(gameCard)) {
                        tgtObjects.remove(obj);
                    } else {
                        targets.add(gameCard);
                    }
                } else { // for now, we can remove non-card objects if they somehow got targeted
                    tgtObjects.remove(obj);
                }
            }
            if (tgtObjects.size() == 0) {
                return;
            }
            Map<Object, Integer> randomMap = Maps.newHashMap();
            for (int i=0; i<counterRemain; i++) {
                Card found = Aggregates.random(targets);
                found.addCounter(counterType, 1, placer, sa, true, table);
                if (randomMap.containsKey(found)) {
                    int oN = randomMap.get(found);
                    int nN = oN+1;
                    randomMap.replace(found, oN, nN);
                } else {
                    randomMap.put(found, 1);
                }
            }
            game.fireEvent(new GameEventRandomLog(logOutput(randomMap, card)));
        } else for (final GameObject obj : tgtObjects) {
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
                            ((Player) obj).addCounter(ct, counterAmount, placer, sa, true, table);
                        }
                        if (obj instanceof Card) {
                            gameCard.addCounter(ct, counterAmount, placer, sa, true, table);
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

            if (sa.hasParam("EachFromSource")) {
                for (Card c : AbilityUtils.getDefinedCards(card, sa.getParam("EachFromSource"), sa)) {
                    for (Entry<CounterType, Integer> cti : c.getCounters().entrySet()) {
                        if (gameCard != null && gameCard.canReceiveCounters(cti.getKey())) {
                            gameCard.addCounter(cti.getKey(), cti.getValue(), placer, sa, true, table);
                        }
                    }
                }
                continue;
            }

            if (obj instanceof Card) {
                boolean counterAdded = false;
                counterAmount = sa.usesTargeting() && sa.isDividedAsYouChoose() ? sa.getDividedValue(gameCard) : counterAmount;
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
                    if (sa.isDividedAsYouChoose() && !sa.usesTargeting()) {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Target", obj);
                        params.put("CounterType", counterType);
                        divrem++;
                        if (divrem == tgtObjects.size() || counterRemain == 1) { counterAmount = counterRemain; }
                        else {
                            counterAmount = pc.chooseNumber(sa, Localizer.getInstance().getMessage
                                    ("lblHowManyCountersThis", CardTranslation.getTranslatedName(gameCard.getName())),
                                    1, counterRemain, params);
                        }
                    }

                    // Adapt need extra logic
                    if (sa.hasParam("Adapt")) {
                        if (!(gameCard.getCounters(CounterEnumType.P1P1) == 0 || StaticAbilityAdapt.anyWithAdapt(sa, gameCard))) {
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

                        Map<String, Object> params = Maps.newHashMap();
                        params.put("CounterType", counterType);
                        params.put("Amount", counterAmount);
                        params.put("Target", gameCard);

                        String message = Localizer.getInstance().getMessage("lblDoYouWantPutTargetP1P1CountersOnCard", String.valueOf(counterAmount), CardTranslation.getTranslatedName(gameCard.getName()));
                        Player chooser = pc.chooseSingleEntityForEffect(activator.getOpponents(), sa, Localizer.getInstance().getMessage("lblChooseAnOpponent"), params);

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
                            int addedAmount = gameCard.addCounter(counterType, counterAmount, placer, sa, true, table);
                            if (addedAmount > 0) {
                                counterAdded = true;
                            }

                            if (sa.hasParam("RemovePhase")) {
                                addRemovePhaseTrigger(card, sa, sa.getParam("RemovePhase"), gameCard, counterType, addedAmount);
                            }
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
                            game.getTriggerHandler().runTrigger(TriggerType.Adapt, AbilityKey.mapFromCard(gameCard), false);
                        }
                    } else {
                        // adding counters to something like re-suspend cards
                        // etbcounter should apply multiplier
                        if (etbcounter) {
                            gameCard.addEtbCounter(counterType, counterAmount, placer);
                        } else {
                            if (gameCard.addCounter(counterType, counterAmount, placer, sa, false, table) > 0) {
                                counterAdded = true;
                            }
                        }
                    }
                    if (rememberCards && counterAdded) {
                        card.addRemembered(gameCard);
                    }
                    game.updateLastStateForCard(gameCard);
                    if (sa.isDividedAsYouChoose() && !sa.usesTargeting()) {
                        counterRemain = counterRemain - counterAmount;
                    }
                }
            } else if (obj instanceof Player) {
                // Add Counters to players!
                Player pl = (Player) obj;
                pl.addCounter(counterType, counterAmount, placer, sa, true, table);
            }
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();

        String amount = sa.getParamOrDefault("CounterNum", "1");
        boolean rememberAmount = sa.hasParam("RememberAmount");

        Player placer = activator;
        if (sa.hasParam("Placer")) {
            final String pstr = sa.getParam("Placer");
            placer = AbilityUtils.getDefinedPlayers(card, pstr, sa).get(0);
        }

        int counterAmount = AbilityUtils.calculateAmount(card, amount, sa);

        GameEntityCounterTable table = new GameEntityCounterTable();

        if (sa.hasParam("TriggeredCounterMap")) {
            @SuppressWarnings("unchecked")
            Map<CounterType, Integer> counterMap = (Map<CounterType, Integer>) sa.getTriggeringObject(AbilityKey.CounterMap);
            for (Map.Entry<CounterType, Integer> e : counterMap.entrySet()) {
                resolvePerType(sa, placer, e.getKey(), e.getValue(), table);
            }
        } else if (sa.hasParam("SharedKeywords")) {
            List<String> keywords = Arrays.asList(sa.getParam("SharedKeywords").split(" & "));
            List<ZoneType> zones =  ZoneType.listValueOf(sa.getParam("SharedKeywordsZone"));
            String[] restrictions = sa.hasParam("SharedRestrictions") ? sa.getParam("SharedRestrictions").split(",") : new String[]{"Card"};
            keywords = CardFactoryUtil.sharedKeywords(keywords, restrictions, zones, card, sa);
            for (String k : keywords) {
                resolvePerType(sa, placer, CounterType.getType(k), counterAmount, table);
            }
        } else {
            CounterType counterType = null;
            if (!sa.hasParam("EachExistingCounter") && !sa.hasParam("EachFromSource")) {
                try {
                    counterType = AbilityUtils.getCounterType(sa.getParam("CounterType"), sa);
                } catch (Exception e) {
                    System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                    return;
                }
            }
            resolvePerType(sa, placer, counterType, counterAmount, table);
        }

        int totalAdded = table.totalValues();

        if (totalAdded > 0 && rememberAmount) {
            // TODO use SpellAbility Remember later
            card.addRemembered(Integer.valueOf(totalAdded));
        }

        table.triggerCountersPutAll(game);
    }

    protected void addRemovePhaseTrigger(final Card host, final SpellAbility sa, String phase, Card tgt, CounterType ct, int added) {
        boolean intrinsic = sa.isIntrinsic();

        StringBuilder delTrig = new StringBuilder("Mode$ Phase | Phase$ ");
        delTrig.append(phase);
        delTrig.append(" | TriggerDescription$ For each ").append(ct.getName()).append(" counter you put on a creature this way, remove a ").append(ct.getName()).append(" counter from that creature at the beginning of the next");
        if ("Cleanup".equals(phase)) {
            delTrig.append("cleanup step");
        } else if ("End of Turn".equals(phase)) {
            delTrig.append("next end step");
        }

        String trigSA = new StringBuilder("DB$ RemoveCounter | Defined$ DelayTriggerRemembered | CounterNum$ 1 | CounterType$ ").append(ct).toString();

        // these trigger are one per counter
        for (int i = 0; i < added; i++) {
            final Trigger trig = TriggerHandler.parseTrigger(delTrig.toString(), sa.getHostCard(), intrinsic);
            trig.addRemembered(tgt);

            final SpellAbility newSa = AbilityFactory.getAbility(trigSA, sa.getHostCard());
            newSa.setIntrinsic(intrinsic);
            trig.setOverridingAbility(newSa);
            sa.getActivatingPlayer().getGame().getTriggerHandler().registerDelayedTrigger(trig);
        }
    }

    protected String logOutput(Map<Object, Integer> randomMap, Card card) {
        StringBuilder randomLog = new StringBuilder();
        randomLog.append(card.getName()).append(" randomly distributed ");
        if (randomMap.entrySet().size() == 0) {
            randomLog.append("no counters.");
        } else {
            randomLog.append("counters: ");
            int count = 0;
            for (Entry<Object, Integer> e : randomMap.entrySet()) {
                count++;
                randomLog.append(e.getKey()).append(" (").append(e.getValue()).append(" counter");
                randomLog.append(e.getValue() != 1 ? "s" : "").append(")");
                randomLog.append(count == randomMap.entrySet().size() ? "" : ", ");
            }
        }
        return randomLog.toString();
    }
}
