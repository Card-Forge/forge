package forge.game.ability.effects;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.SpellAbility;

public class ReplaceCounterEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        // outside of Replacement Effect, unwanted result
        if (!sa.isReplacementAbility()) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> originalParams = (Map<AbilityKey, Object>) sa.getReplacingObject(AbilityKey.OriginalParams);
        @SuppressWarnings("unchecked")
        Map<Optional<Player>, Multiset<CounterType>> counterTable = (Map<Optional<Player>, Multiset<CounterType>>) sa.getReplacingObject(AbilityKey.CounterMap);

        if (counterTable.size() > 1 && sa.hasParam("ChooseCounter")) {
            // ChooseCounter is for ones that only adds one counter, when that is coming from multiple sources, the affected player needs to choose

            GameEntity ge = (GameEntity) sa.getReplacingObject(AbilityKey.Object);
            Player chooser = ge instanceof Player ? (Player) ge : ((Card) ge).getController();

            // for some effects, the Player -> CounterType Table needs to be flip into a CounterType -> [Player] list for the player to select
            Multimap<CounterType, Player> playerMap = HashMultimap.create();
            for (Map.Entry<Optional<Player>, Multiset<CounterType>> e : counterTable.entrySet()) {
                for (CounterType ct : e.getValue().elementSet()) {
                    playerMap.put(ct, e.getKey().orElse(null));
                }
            }

            // there shouldn't be a case where one of the players is null, and the other is not

            for (Map.Entry<CounterType, Collection<Player>> e : playerMap.asMap().entrySet()) {
                Optional<Player> p = Optional.ofNullable(chooser.getController().chooseSingleEntityForEffect(new PlayerCollection(e.getValue()), sa, "Choose Player for " + e.getKey().getName(), null));

                sa.setReplacingObject(AbilityKey.CounterNum, counterTable.get(p).count(e.getKey()));
                counterTable.get(p).setCount(e.getKey(), AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa));
            }
        } else {
            for (Map.Entry<Optional<Player>, Multiset<CounterType>> e : counterTable.entrySet()) {
                if (!sa.matchesValidParam("ValidSource", e.getKey().orElse(null))) {
                    continue;
                }

                if (sa.hasParam("ValidCounterType")) {
                    CounterType ct = CounterType.getType(sa.getParam("ValidCounterType"));
                    if (e.getValue().contains(ct)) {
                        sa.setReplacingObject(AbilityKey.CounterNum, e.getValue().count(ct));
                        e.getValue().setCount(ct, AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa));
                    }
                } else {
                    for (Multiset.Entry<CounterType> ec : Lists.newArrayList(e.getValue().entrySet())) {
                        sa.setReplacingObject(AbilityKey.CounterNum, ec.getCount());
                        e.getValue().setCount(ec.getElement(), AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa));
                    }
                }
            }
        }

        originalParams.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
    }
}
