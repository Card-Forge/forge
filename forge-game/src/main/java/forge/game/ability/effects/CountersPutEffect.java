package forge.game.ability.effects;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.base.Optional;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.event.GameEventRandomLog;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityAdapt;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

public class CountersPutEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder stringBuilder = new StringBuilder();
        final Card card = sa.getHostCard();
        final String who = sa.getActivatingPlayer().getName();
        boolean pronoun = false;

        if (sa.hasParam("IfDesc")) {
            final String ifD = sa.getParam("IfDesc");
            if (ifD.equals("True")) {
                String ifDesc = sa.getDescription();
                if (ifDesc.contains(",")) {
                    if (ifDesc.contains(" you ")) {
                        ifDesc = ifDesc.replaceFirst(" you ", " " + who + " ");
                        pronoun = true;
                        if (ifDesc.contains(" you ")) {
                            ifDesc = ifDesc.replaceAll(" you ", " they ");
                        }
                        if (ifDesc.contains(" your ")) {
                            ifDesc = ifDesc.replaceAll(" your ", " their ");
                        }
                    }
                    stringBuilder.append(ifDesc, 0, ifDesc.indexOf(",") + 1);
                } else {
                    stringBuilder.append("[CountersPutEffect IfDesc parsing error]");
                }
            } else {
                stringBuilder.append(ifD);
            }
            stringBuilder.append(" ");
        }

        stringBuilder.append(pronoun ? "they" : who).append(" ");
        final String typeName = sa.hasParam("CounterType") ? CounterType.getType(sa.getParam("CounterType")).getName().toLowerCase() : "";

        final List<String> playerCounters = Arrays.asList("energy", "experience", "poison", "ticket");
        if (playerCounters.contains(typeName)) {
            stringBuilder.append(pronoun ? "get " : "gets ");
            stringBuilder.append(Lang.nounWithNumeralExceptOne(AbilityUtils.calculateAmount(card,
                    sa.getParamOrDefault("CounterNum", "1"), sa), typeName + " counter"));
            stringBuilder.append(".");
            return stringBuilder.toString();
        }

        String desc = sa.getDescription();
        boolean forEach = desc.contains("for each");
        if (sa.hasParam("CounterTypes")) {
            if (desc.contains("Put ") && desc.contains(" on ")) {
                desc = desc.substring(desc.indexOf("Put "), desc.indexOf(" on ") + 4)
                        .replaceFirst("Put ", "puts ");
            }
            stringBuilder.append(desc).append(Lang.joinHomogenous(getTargets(sa))).append(".");
            return stringBuilder.toString();
        }
        // skip the StringBuilder if no targets are chosen ("up to" scenario)
        if (sa.usesTargeting()) {
            final List<Card> targetCards = SpellAbilityEffect.getTargetCards(sa);
            if (targetCards.size() == 0) {
                return stringBuilder.toString();
            }
        }

        final String key = forEach ? "ForEachNum" : "CounterNum";
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault(key, "1"), sa);

        if (sa.hasParam("Bolster")) {
            stringBuilder.append("bolsters ").append(amount).append(".");
            return stringBuilder.toString();
        }
        boolean divAsChoose = sa.isDividedAsYouChoose();
        final boolean divRandom = sa.hasParam("DividedRandomly");
        if (divAsChoose) {
            stringBuilder.append(pronoun ? "distribute " : "distributes ");
        } else if (divRandom) {
            stringBuilder.append(pronoun ? "randomly distribute " : "randomly distributes ");
        } else {
            stringBuilder.append(pronoun ? "put " : "puts ");
        }
        if (sa.hasParam("UpTo")) {
            stringBuilder.append("up to ");
        }

        stringBuilder.append(Lang.nounWithNumeralExceptOne(amount, typeName + " counter"));
        stringBuilder.append(divAsChoose || divRandom ? " among " : " on ");

        // special handling for multiple Defined
        if (sa.hasParam("Defined") && sa.getParam("Defined").contains(" & ")) {
            String[] def = sa.getParam("Defined").split(" & ");
            for (int i = 0; i < def.length; i++) {
                stringBuilder.append(AbilityUtils.getDefinedEntities(card, def[i], sa).toString()
                        .replaceAll("[\\[\\]]", ""));
                if (i + 1 < def.length) {
                    stringBuilder.append(" and ");
                    stringBuilder.append(Lang.nounWithNumeralExceptOne(amount, typeName + " counter")).append(" on ");
                }
            }
            // if use targeting we show all targets and corresponding counters
        } else if (sa.usesTargeting()) {
            final List<Card> targetCards = SpellAbilityEffect.getTargetCards(sa);
            for (int i = 0; i < targetCards.size(); i++) {
                Card targetCard = targetCards.get(i);
                stringBuilder.append(targetCard);
                Integer v = sa.getDividedValue(targetCard);
                if (v != null) // fix null counter stack description
                    stringBuilder.append(" (").append(v).append(v == 1 ? " counter)" : " counters)");

                if (i == targetCards.size() - 2) {
                    stringBuilder.append(" and ");
                } else if (i + 1 < targetCards.size()) {
                    stringBuilder.append(", ");
                }
            }
        } else if (sa.hasParam("Choices")) {
            int n = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("ChoiceAmount", "1"), sa);
            String what = (sa.getParamOrDefault("ChoicesDesc", sa.getParam("Choices")));
            stringBuilder.append(Lang.nounWithNumeralExceptOne(n, what));
        } else {
            final List<Card> targetCards = SpellAbilityEffect.getTargetCards(sa);
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
        stringBuilder.append(forEach ? desc.substring(desc.indexOf(" for each")) : ".");
        return stringBuilder.toString();
    }

    protected void resolvePerType(SpellAbility sa, Player placer, CounterType counterType, int counterAmount,
                                  GameEntityCounterTable table, boolean stopForTypes) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();
        final boolean etbcounter = sa.hasParam("ETB");

        boolean existingCounter = sa.hasParam("CounterType") && sa.getParam("CounterType").equals("ExistingCounter");
        boolean eachExistingCounter = sa.hasParam("EachExistingCounter");
        boolean putOnEachOther = sa.hasParam("PutOnEachOther");
        boolean putOnDefined = sa.hasParam("PutOnDefined");

        if (sa.hasParam("Optional") && !pc.confirmAction
                (sa, null, Localizer.getInstance().getMessage("lblDoYouWantPutCounter"), null)) {
            return;
        }

        List<GameEntity> tgtObjects = Lists.newArrayList();
        int divrem = 0;
        if (sa.hasParam("Bolster")) {
            CardCollection creatsYouCtrl = activator.getCreaturesInPlay();
            CardCollection leastToughness = new CardCollection(
                    Aggregates.listWithMin(creatsYouCtrl, CardPredicates.Accessors.fnGetNetToughness));

            Map<String, Object> params = Maps.newHashMap();
            params.put("CounterType", counterType);

            Iterables.addAll(tgtObjects, activator.getController().chooseCardsForEffect(leastToughness, sa,
                    Localizer.getInstance().getMessage("lblChooseACreatureWithLeastToughness"), 1, 1, false, params));
        } else if (sa.hasParam("Choices") && (counterType != null || putOnEachOther || putOnDefined)) {
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

            int n = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("ChoiceAmount", "1"), sa);
            int m = AbilityUtils.calculateAmount(card,
                    sa.getParamOrDefault("MinChoiceAmount", sa.getParamOrDefault("ChoiceAmount", "1")), sa);

            // no choices allowed
            if (n <= 0) {
                return;
            }

            CardCollection choices = CardLists.getValidCards(game.getCardsIn(choiceZone), sa.getParam("Choices"),
                    activator, card, sa);

            // TODO might use better message
            String title = Localizer.getInstance().getMessage("lblChooseaCard") + " ";
            if (sa.hasParam("ChoiceTitle")) {
                title = sa.getParam("ChoiceTitle");
            }
            if ((sa.hasParam("ChoiceTitle") || sa.hasParam("SpecifyCounter")) && counterType != null) {
                title += " (" + counterType.getName() + ")";
            } else if (putOnEachOther || putOnDefined) {
                title += Localizer.getInstance().getMessage("lblWithKindCounter");
                if (putOnEachOther) {
                    title += " " + Localizer.getInstance().getMessage("lblEachOther");
                }
            }

            Map<String, Object> params = Maps.newHashMap();
            if (counterType != null) {
                params.put("CounterType", counterType);
            }
            if (sa.hasParam("DividedRandomly")) {
                tgtObjects.addAll(choices);
            } else {
                Iterables.addAll(tgtObjects, chooser.getController().chooseCardsForEffect(choices, sa, title, m, n,
                        sa.hasParam("ChoiceOptional"), params));
            }
        } else {
            if (sa.hasParam("Defined") && sa.getParam("Defined").contains(" & ")) {
                tgtObjects.addAll(AbilityUtils.getDefinedEntities(card, sa.getParam("Defined").split(" & "), sa));
            } else {
                tgtObjects.addAll(getDefinedEntitiesOrTargeted(sa, "Defined"));
            }
        }

        int counterRemain = counterAmount;
        if (sa.hasParam("DividedRandomly")) {
            CardCollection targets = new CardCollection();
            for (final GameEntity obj : tgtObjects) { // check if each target is still OK
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
            for (int i = 0; i < counterRemain; i++) {
                Card found = Aggregates.random(targets);
                found.addCounter(counterType, 1, placer, table);
                if (randomMap.containsKey(found)) {
                    int oN = randomMap.get(found);
                    int nN = oN + 1;
                    randomMap.replace(found, oN, nN);
                } else {
                    randomMap.put(found, 1);
                }
            }
            game.fireEvent(new GameEventRandomLog(logOutput(randomMap, card)));
        } else {
            for (final GameEntity obj : tgtObjects) {
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

                if (sa.hasParam("ChooseDifferent")) {
                    final int num = Integer.parseInt(sa.getParam("ChooseDifferent"));
                    final List<CounterType> typesToAdd = Lists.newArrayList();
                    String options = sa.getParam("CounterType");
                    for (int i = 0; i < num; i++) {
                        CounterType ct = chooseTypeFromList(sa, options, obj, pc);
                        typesToAdd.add(ct);
                        options = options.replace(ct.getName(),"");
                    }
                    for (CounterType ct : typesToAdd) {
                        if (obj instanceof Player) {
                            ((Player) obj).addCounter(ct, counterAmount, placer, table);
                        }
                        if (obj instanceof Card) {
                            if (etbcounter) {
                                gameCard.addEtbCounter(ct, counterAmount, placer);
                            } else {
                                gameCard.addCounter(ct, counterAmount, placer, table);
                            }
                        }
                    }
                    continue;
                }

                if (stopForTypes && sa.hasParam("CounterTypes")) {
                    final List<CounterType> typesToAdd = Lists.newArrayList();
                    String types = sa.getParam("CounterTypes");
                    if (types.contains("ChosenFromList")) {
                        typesToAdd.add(chooseTypeFromList(sa, sa.getParam("TypeList"), obj, pc));
                        types = types.replace("ChosenFromList", "");
                    }
                    for (String type : types.split(",")) {
                        if (type.contains("EachType")) {
                            CardCollectionView counterCards =
                                    CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                                            type.split("_")[1], activator, card, sa);
                            List <CounterType> counterTypes = Lists.newArrayList();
                            for (Card c : counterCards) {
                                for (final Map.Entry<CounterType, Integer> map : c.getCounters().entrySet()) {
                                    if (!counterTypes.contains(map.getKey())) {
                                        counterTypes.add(map.getKey());
                                    }
                                }
                            }
                            for (CounterType ct : counterTypes) {
                                if (sa.hasParam("AltChoiceForEach")) {
                                    String typeChoices = sa.getParam("AltChoiceForEach") + "," + ct.toString();
                                    ct = chooseTypeFromList(sa, typeChoices, obj, pc);
                                }
                                resolvePerType(sa, placer, ct, counterAmount, table, false);
                            }
                        } else {
                            typesToAdd.add(CounterType.getType(type));
                        }
                    }
                    for (CounterType ct : typesToAdd) {
                        if (obj instanceof Player) {
                            ((Player) obj).addCounter(ct, counterAmount, placer, table);
                        }
                        if (obj instanceof Card) {
                            if (etbcounter) {
                                gameCard.addEtbCounter(ct, counterAmount, placer);
                            } else {
                                gameCard.addCounter(ct, counterAmount, placer, table);
                            }
                        }
                    }
                    continue;
                }

                if (existingCounter) {
                    final List<CounterType> choices = Lists.newArrayList();
                    // get types of counters
                    for (CounterType ct : obj.getCounters().keySet()) {
                        if (obj.canReceiveCounters(ct) || putOnEachOther) {
                            choices.add(ct);
                        }
                    }

                    if (eachExistingCounter) {
                        for (CounterType ct : choices) {
                            if (obj instanceof Player) {
                                ((Player) obj).addCounter(ct, counterAmount, placer, table);
                            }
                            if (obj instanceof Card) {
                                gameCard.addCounter(ct, counterAmount, placer, table);
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
                        String sb = Localizer.getInstance().getMessage("lblSelectCounterTypeAddTo") +
                                " " + (putOnEachOther ? Localizer.getInstance().getMessage("lblEachOther") : obj);
                        counterType = pc.chooseCounterType(choices, sa, sb, params);
                    }
                    if (putOnEachOther) {
                        List<Card> others = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                                sa.getParam("PutOnEachOther"), activator, card, sa);
                        for (Card other : others) {
                            if (other.equals(obj)) {
                                continue;
                            }
                            Card otherGCard = game.getCardState(other, null);
                            otherGCard.addCounter(counterType, counterAmount, placer, table);
                        }
                        continue;
                    } else if (putOnDefined) {
                        List<Card> defs = AbilityUtils.getDefinedCards(card, sa.getParam("PutOnDefined"), sa);
                        for (Card c : defs) {
                            Card gCard = game.getCardState(c, null);
                            if (!sa.hasParam("OnlyNewKind") || gCard.getCounters(counterType) < 1) {
                                gCard.addCounter(counterType, counterAmount, placer, table);
                            }
                        }
                        continue;
                    }
                }

                if (sa.hasParam("EachFromSource")) {
                    for (Card c : AbilityUtils.getDefinedCards(card, sa.getParam("EachFromSource"), sa)) {
                        for (Entry<CounterType, Integer> cti : c.getCounters().entrySet()) {
                            if (gameCard != null) {
                                gameCard.addCounter(cti.getKey(), cti.getValue(), placer, table);
                            }
                        }
                    }
                    continue;
                }
                if (sa.hasParam("CounterTypePerDefined") || sa.hasParam("UniqueType")) {
                    counterType = chooseTypeFromList(sa, sa.getParam("CounterType"), obj, pc);
                    if (counterType == null) continue;
                }

                if (obj instanceof Card) {
                    if (sa.hasParam("CounterNumPerDefined")) {
                        counterAmount = AbilityUtils.calculateAmount(gameCard, sa.getParam("CounterNumPerDefined"), sa);
                    }
                    counterAmount = sa.usesTargeting() && sa.isDividedAsYouChoose() ? sa.getDividedValue(gameCard)
                            : counterAmount;
                    if (sa.hasParam("UpTo")) {
                        int min = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("UpToMin", "0"), sa);
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Target", obj);
                        params.put("CounterType", counterType);
                        counterAmount = pc.chooseNumber(sa,
                                Localizer.getInstance().getMessage("lblHowManyCounters"), min, counterAmount, params);
                    }
                    if (sa.isDividedAsYouChoose() && !sa.usesTargeting()) {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Target", obj);
                        params.put("CounterType", counterType);
                        divrem++;
                        if (divrem == tgtObjects.size() || counterRemain == 1) {
                            counterAmount = counterRemain;
                        } else {
                            counterAmount = pc.chooseNumber(sa,
                                    Localizer.getInstance().getMessage("lblHowManyCountersThis",
                                            CardTranslation.getTranslatedName(gameCard.getName())),
                                    1, counterRemain, params);
                        }
                    }

                    // Adapt need extra logic
                    if (sa.hasParam("Adapt")) {
                        if (!(gameCard.getCounters(CounterEnumType.P1P1) == 0
                                || StaticAbilityAdapt.anyWithAdapt(sa, gameCard))) {
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

                        // check if it can receive the Tribute
                        if (abort) {
                            continue;
                        }

                        Map<String, Object> params = Maps.newHashMap();
                        params.put("CounterType", counterType);
                        params.put("Amount", counterAmount);
                        params.put("Target", gameCard);

                        String message = Localizer.getInstance().getMessage(
                                "lblDoYouWantPutTargetP1P1CountersOnCard", String.valueOf(counterAmount),
                                CardTranslation.getTranslatedName(gameCard.getName()));
                        placer = pc.chooseSingleEntityForEffect(activator.getOpponents(), sa,
                                Localizer.getInstance().getMessage("lblChooseAnOpponent"), params);

                        if (placer.getController().confirmAction(sa, PlayerActionConfirmMode.Tribute, message, null)) {
                            gameCard.setTributed(true);
                        } else {
                            continue;
                        }
                    }

                    if (etbcounter) {
                        gameCard.addEtbCounter(counterType, counterAmount, placer);
                    } else {
                        gameCard.addCounter(counterType, counterAmount, placer, table);
                    }

                    if (sa.hasParam("Evolve")) {
                        game.getTriggerHandler().runTrigger(TriggerType.Evolved, AbilityKey.mapFromCard(gameCard),
                                false);
                    }
                    if (sa.hasParam("Monstrosity")) {
                        gameCard.setMonstrous(true);
                        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(gameCard);
                        runParams.put(AbilityKey.MonstrosityAmount, counterAmount);
                        game.getTriggerHandler().runTrigger(TriggerType.BecomeMonstrous, runParams, false);
                    }
                    if (sa.hasParam("Renown")) {
                        gameCard.setRenowned(true);
                        game.getTriggerHandler().runTrigger(TriggerType.BecomeRenowned,
                                AbilityKey.mapFromCard(gameCard), false);
                    }
                    if (sa.hasParam("Adapt")) {
                        game.getTriggerHandler().runTrigger(TriggerType.Adapt, AbilityKey.mapFromCard(gameCard),
                                false);
                    }
                    if (sa.hasParam("Training")) {
                        game.getTriggerHandler().runTrigger(TriggerType.Trains, AbilityKey.mapFromCard(gameCard),
                                false);
                    }

                    game.updateLastStateForCard(gameCard);
                    if (sa.isDividedAsYouChoose() && !sa.usesTargeting()) {
                        counterRemain = counterRemain - counterAmount;
                    }
                } else if (obj instanceof Player) {
                    // Add Counters to players!
                    Player pl = (Player) obj;
                    pl.addCounter(counterType, counterAmount, placer, table);
                }
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
            Integer counterMapValue = null;
            if (sa.hasParam("CounterMapValues")) {
                counterMapValue = Integer.valueOf(sa.getParam("CounterMapValues"));
            }
            @SuppressWarnings("unchecked")
            Map<CounterType, Integer> counterMap = (Map<CounterType, Integer>) sa
                    .getTriggeringObject(AbilityKey.CounterMap);
            for (Map.Entry<CounterType, Integer> e : counterMap.entrySet()) {
                resolvePerType(sa, placer, e.getKey(), counterMapValue == null ? e.getValue() : counterMapValue, table, false);
            }
        } else if (sa.hasParam("SharedKeywords")) {
            List<String> keywords = Arrays.asList(sa.getParam("SharedKeywords").split(" & "));
            if (sa.hasParam("SharedKeywordsDefined")) {
                CardCollection def = getDefinedCardsOrTargeted(sa, "SharedKeywordsDefined");
                keywords = CardFactoryUtil.getSharedKeywords(keywords, def);
            } else {
                List<ZoneType> zones = ZoneType.listValueOf(sa.getParam("SharedKeywordsZone"));
                String[] restrictions = sa.hasParam("SharedRestrictions") ? sa.getParam("SharedRestrictions").split(",")
                        : new String[] { "Card" };
                keywords = CardFactoryUtil.sharedKeywords(keywords, restrictions, zones, card, sa);
            }
            for (String k : keywords) {
                resolvePerType(sa, placer, CounterType.getType(k), counterAmount, table, false);
            }
        } else {
            CounterType counterType = null;
            if (!sa.hasParam("EachExistingCounter") && !sa.hasParam("EachFromSource")
                    && !sa.hasParam("UniqueType") && !sa.hasParam("CounterTypePerDefined")
                    && !sa.hasParam("CounterTypes") && !sa.hasParam("ChooseDifferent")
                    && !sa.hasParam("PutOnEachOther") && !sa.hasParam("PutOnDefined")) {
                try {
                    counterType = chooseTypeFromList(sa, sa.getParam("CounterType"), null, placer.getController());
                } catch (Exception e) {
                    System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                    return;
                }
            }
            resolvePerType(sa, placer, counterType, counterAmount, table, true);
        }

        int totalAdded = table.totalValues();

        if (totalAdded > 0 && rememberAmount) {
            // TODO use SpellAbility Remember later
            card.addRemembered(Integer.valueOf(totalAdded));
        }

        table.replaceCounterEffect(game, sa, true);

        if (sa.hasParam("RemovePhase")) {
            for (Map.Entry<GameEntity, Map<CounterType, Integer>> e : table.row(Optional.of(placer)).entrySet()) {
                for (Map.Entry<CounterType, Integer> ce : e.getValue().entrySet()) {
                    addRemovePhaseTrigger(card, sa, sa.getParam("RemovePhase"), e.getKey(), ce.getKey(), ce.getValue());
                }
            }
        }
        //for cards like Agitator Ant/Spectacular Showdown that care if counters were actually put on,
        // instead use "RememberPut" – this checks after replacement
        if (sa.hasParam("RememberCards")) { // remembers whether counters actually placed or not
            card.addRemembered(table.columnKeySet());
        }
    }

    protected void addRemovePhaseTrigger(final Card host, final SpellAbility sa, String phase, GameEntity tgt,
            CounterType ct, int added) {
        boolean intrinsic = sa.isIntrinsic();

        StringBuilder delTrig = new StringBuilder("Mode$ Phase | Phase$ ");
        delTrig.append(phase);
        delTrig.append(" | TriggerDescription$ For each ").append(ct.getName())
                .append(" counter you put on a creature this way, remove a ").append(ct.getName())
                .append(" counter from that creature at the beginning of the next");
        if ("Cleanup".equals(phase)) {
            delTrig.append("cleanup step");
        } else if ("End of Turn".equals(phase)) {
            delTrig.append("next end step");
        }

        String trigSA = new StringBuilder(
                "DB$ RemoveCounter | Defined$ DelayTriggerRemembered | CounterNum$ 1 | CounterType$ ").append(ct)
                        .toString();

        // these trigger are one per counter
        for (int i = 0; i < added; i++) {
            final Trigger trig = TriggerHandler.parseTrigger(delTrig.toString(), host, intrinsic);
            trig.addRemembered(tgt);

            final SpellAbility newSa = AbilityFactory.getAbility(trigSA, host);
            newSa.setIntrinsic(intrinsic);
            trig.setOverridingAbility(newSa);
            trig.setSpawningAbility(sa.copy(host, true));
            sa.getActivatingPlayer().getGame().getTriggerHandler().registerDelayedTrigger(trig);
        }
    }

    protected CounterType chooseTypeFromList(SpellAbility sa, String list, GameEntity obj, PlayerController pc) {
        List<CounterType> choices = Lists.newArrayList();
        for (String s : list.split(",")) {
            if (!s.equals("") && (!sa.hasParam("UniqueType") || obj.getCounters(CounterType.getType(s)) == 0)) {
                CounterType type = CounterType.getType(s);
                if (!choices.contains(type)) {
                    choices.add(type);
                }
            }
        }
        if (sa.hasParam("RandomType")) {
            return Aggregates.random(choices);
        }
        Map<String, Object> params = Maps.newHashMap();
        params.put("Target", obj);
        StringBuilder sb = new StringBuilder();
        if (obj != null) {
            sb.append(Localizer.getInstance().getMessage("lblSelectCounterTypeAddTo")).append(" ").append(obj);
        } else {
            sb.append(Localizer.getInstance().getMessage("lblSelectCounterType"));
        }
        return pc.chooseCounterType(choices, sa, sb.toString(), params);
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
