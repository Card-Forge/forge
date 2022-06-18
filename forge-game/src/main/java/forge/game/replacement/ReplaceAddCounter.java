package forge.game.replacement;

import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Optional;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceAddCounter extends ReplacementEffect {

    /**
     *
     * ReplaceProduceMana.
     * @param mapParams &emsp; HashMap<String, String>
     * @param host &emsp; Card
     */
    public ReplaceAddCounter(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        if (hasParam("EffectOnly")) {
            final Boolean effectOnly = (Boolean) runParams.get(AbilityKey.EffectOnly);
            if (!effectOnly) {
                return false;
            }
        }

        if (!matchesValidParam("ValidCard", runParams.get(AbilityKey.Affected))) {
            return false;
        }
        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Affected))) {
            return false;
        }
        if (!matchesValidParam("ValidObject", runParams.get(AbilityKey.Affected))) {
            return false;
        }

        if (!matchesValidParam("ValidCause", runParams.get(AbilityKey.Cause))) {
            return false;
        }

        if (!hasAnyInCounterMap(runParams)) {
            return false;
        }

        if (runParams.containsKey(AbilityKey.ETB)) {
            // if Card does affect something other than itself
            if (!hasParam("ValidCard") || !getParam("ValidCard").equals("Card.Self")) {
                // and it self is entering, skip
                if (getHostCard().equals(runParams.get(AbilityKey.Affected))) {
                    return false;
                }
                // and it wasn't already on the field, skip
                if (getActiveZone().contains(ZoneType.Battlefield) && runParams.containsKey(AbilityKey.LastStateBattlefield)) {
                    CardCollectionView lastBattlefield = (CardCollectionView) runParams.get(AbilityKey.LastStateBattlefield);
                    if (!lastBattlefield.contains(getHostCard())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.CounterMap, runParams.get(AbilityKey.CounterMap));
        Object o = runParams.get(AbilityKey.Affected);
        if (o instanceof Card) {
            sa.setReplacingObject(AbilityKey.Card, o);
        } else if (o instanceof Player) {
            sa.setReplacingObject(AbilityKey.Player, o);
        }
        sa.setReplacingObject(AbilityKey.Object, o);
    }

    public boolean hasAnyInCounterMap(Map<AbilityKey, Object> runParams) {
        @SuppressWarnings("unchecked")
        Map<Optional<Player>, Map<CounterType, Integer>> counterMap = (Map<Optional<Player>, Map<CounterType, Integer>>) runParams.get(AbilityKey.CounterMap);

        for (Map.Entry<Optional<Player>, Map<CounterType, Integer>> e : counterMap.entrySet()) {
            if (!matchesValidParam("ValidSource", e.getKey().orNull())) {
                continue;
            }
            if (hasParam("ValidCounterType")) {
                CounterType ct = CounterType.getType(getParam("ValidCounterType"));
                if (!e.getValue().containsKey(ct)) {
                    continue;
                }
                if (0 >= ObjectUtils.defaultIfNull(e.getValue().get(ct), 0)) {
                    continue;
                }
            }
            return true;
        }

        return false;
    }
}
