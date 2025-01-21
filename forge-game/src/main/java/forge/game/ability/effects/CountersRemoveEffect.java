package forge.game.ability.effects;

import java.util.Map;

import forge.game.card.*;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;

public class CountersRemoveEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final String counterName = sa.getParam("CounterType");
        final String num = sa.getParamOrDefault("CounterNum", "1");

        int amount = 0;
        if (!num.equals("All") && !num.equals("Any")) {
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), num, sa);
        }

        sb.append("Remove ");
        if (sa.hasParam("UpTo")) {
            sb.append("up to ");
        }
        if ("All".matches(counterName)) {
            sb.append("all counter");
        } else if ("Any".matches(counterName)) {
            if (amount == 1) {
                sb.append("a counter");
            } else {
                sb.append(amount).append(" ").append(" counter");
            }
        } else {
            sb.append(amount).append(" ").append(CounterType.getType(counterName).getName()).append(" counter");
        }
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from");

        sb.append(Lang.joinHomogenous(getTargetCards(sa)));

        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));

        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();

        PlayerController pc = activator.getController();
        final String type = sa.getParam("CounterType");
        final String num = sa.getParamOrDefault("CounterNum", "1");

        int cntToRemove = 0;
        if (!num.equals("All") && !num.equals("Any")) {
            cntToRemove = AbilityUtils.calculateAmount(card, num, sa);
        }

        if (sa.hasParam("Optional")) {
            String ctrs = cntToRemove > 1 ? Localizer.getInstance().getMessage("lblCounters") : num.equals("All") ? Localizer.getInstance().getMessage("lblAllCounters") : Localizer.getInstance().getMessage("lblACounters");
            if (!pc.confirmAction(sa, null, Localizer.getInstance().getMessage("lblRemove") + " " + ctrs + "?", null)) {
                return;
            }
        }

        CounterType counterType = null;

        if (!type.equals("Any") && !type.equals("All")) {
            try {
                counterType = CounterType.getType(type);
            } catch (Exception e) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return;
            }
        }

        boolean rememberRemoved = sa.hasParam("RememberRemoved");
        boolean rememberAmount = sa.hasParam("RememberAmount");

        int totalRemoved = 0;

        for (final Player tgtPlayer : getTargetPlayers(sa)) {
            if (!tgtPlayer.isInGame()) {
                continue;
            }
            // Removing energy
            if (type.equals("All")) {
                for (Map.Entry<CounterType, Integer> e : Lists.newArrayList(tgtPlayer.getCounters().entrySet())) {
                    totalRemoved += tgtPlayer.subtractCounter(e.getKey(), e.getValue(), activator);
                }
            } else {
                if (num.equals("All")) {
                    cntToRemove = tgtPlayer.getCounters(counterType);
                }
                if (type.equals("Any")) {
                    totalRemoved += removeAnyType(tgtPlayer, cntToRemove, sa);
                } else {
                    totalRemoved += tgtPlayer.subtractCounter(counterType, cntToRemove, activator);
                }
            }
        }

        CardCollectionView srcCards;

        String typeforPrompt = counterType == null ? "" : counterType.getName();
        String title = Localizer.getInstance().getMessage("lblChooseCardsToTakeTargetCounters", typeforPrompt);
        title = title.replace("  ", " ");
        if (sa.hasParam("ValidSource")) {
            srcCards = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("ValidSource"), activator, card, sa);
            if (num.equals("Any")) {
                Map<String, Object> params = Maps.newHashMap();
                params.put("CounterType", counterType);
                srcCards = pc.chooseCardsForEffect(srcCards, sa, title, 0, srcCards.size(), true, params);
            }
        } else if (sa.hasParam("Choices") && counterType != null) {
            ZoneType choiceZone = sa.hasParam("ChoiceZone") ? ZoneType.smartValueOf(sa.getParam("ChoiceZone"))
                    : ZoneType.Battlefield;

            CardCollection choices = CardLists.getValidCards(game.getCardsIn(choiceZone), sa.getParam("Choices"),
                    activator, card, sa);

            int min = 1;
            int max = 1;
            if (sa.hasParam("ChoiceOptional")) {
                min = 0;
                max = choices.size();
            }
            srcCards = pc.chooseCardsForEffect(choices, sa, title, min, max, min == 0, null);
        } else {
            srcCards = getTargetCards(sa);
        }

        for (final Card tgtCard : srcCards) {
            Card gameCard = game.getCardState(tgtCard, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtCard.equalsWithGameTimestamp(gameCard)) {
                continue;
            }

            final Zone zone = game.getZoneOf(gameCard);
            if (type.equals("All")) {
                for (Map.Entry<CounterType, Integer> e : Lists.newArrayList(gameCard.getCounters().entrySet())) {
                    totalRemoved += gameCard.subtractCounter(e.getKey(), e.getValue(), activator);
                }
                game.updateLastStateForCard(gameCard);
                continue;
            } else if (num.equals("All") || num.equals("Any")) {
                cntToRemove = gameCard.getCounters(counterType);
            }

            if (type.equals("Any")) {
                totalRemoved += removeAnyType(gameCard, cntToRemove, sa);
            } else {
                if (!tgtCard.canRemoveCounters(counterType)) {
                    continue;
                }
                cntToRemove = Math.min(cntToRemove, gameCard.getCounters(counterType));

                if (zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Exile)) {
                    if (sa.hasParam("UpTo") || num.equals("Any")) {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Target", gameCard);
                        params.put("CounterType", counterType);
                        title = Localizer.getInstance().getMessage("lblSelectRemoveCountersNumberOfTarget", type);
                        cntToRemove = pc.chooseNumber(sa, title, 0, cntToRemove, params);
                    }
                }
                if (cntToRemove > 0) {
                    gameCard.subtractCounter(counterType, cntToRemove, activator);
                    if (rememberRemoved) {
                        for (int i = 0; i < cntToRemove; i++) {
                            // TODO might need to be more specific
                            card.addRemembered(Pair.of(counterType, i));
                        }
                    }
                    game.updateLastStateForCard(gameCard);

                    totalRemoved += cntToRemove;
                }
            }
        }

        if (totalRemoved > 0 && rememberAmount) {
            // TODO use SpellAbility Remember later
            card.addRemembered(totalRemoved);
        }
    }

    protected int removeAnyType(GameEntity entity, int cntToRemove, SpellAbility sa) {
        boolean rememberRemoved = sa.hasParam("RememberRemoved");
        int removed = 0;

        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();
        final Map<CounterType, Integer> tgtCounters = Maps.newHashMap(entity.getCounters());
        for (CounterType ct : ImmutableList.copyOf(tgtCounters.keySet())) {
            if (!entity.canRemoveCounters(ct)) {
                tgtCounters.remove(ct);
            }
        }

        while (cntToRemove > 0 && !tgtCounters.isEmpty()) {
            Map<String, Object> params = Maps.newHashMap();
            params.put("Target", entity);

            String prompt = Localizer.getInstance().getMessage("lblSelectCountersTypeToRemove");
            CounterType chosenType = pc.chooseCounterType(ImmutableList.copyOf(tgtCounters.keySet()), sa, prompt, params);

            int max = Math.min(cntToRemove, tgtCounters.get(chosenType));
            // remove selection so player can't cheat additional trigger by choosing the same type multiple times
            tgtCounters.remove(chosenType);
            int remaining = Aggregates.sum(tgtCounters.values());
            // player must choose enough so he can still reach the amount with other types
            int min = sa.hasParam("UpTo") ? 0 : Math.max(1, max - remaining);
            prompt = Localizer.getInstance().getMessage("lblSelectRemoveCountersNumberOfTarget", chosenType.getName());
            params = Maps.newHashMap();
            params.put("Target", entity);
            params.put("CounterType", chosenType);
            int chosenAmount = pc.chooseNumber(sa, prompt, min, max, params);

            if (chosenAmount > 0) {
                removed += chosenAmount;
                entity.subtractCounter(chosenType, chosenAmount, activator);
                if (entity instanceof Card) {
                    Card gameCard = (Card) entity;
                    game.updateLastStateForCard(gameCard);
                }

                if (rememberRemoved) {
                    for (int i = 0; i < chosenAmount; i++) {
                        card.addRemembered(Pair.of(chosenType, i));
                    }
                }
                cntToRemove -= chosenAmount;
            } else if (sa.hasParam("UpTo")) {
                break;
            }
        }
        return removed;
    }
}
