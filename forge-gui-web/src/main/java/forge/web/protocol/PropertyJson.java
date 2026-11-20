package forge.web.protocol;

import com.google.common.collect.Multiset;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import forge.card.CardTypeView;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.card.CounterType;
import forge.game.keyword.KeywordCollectionView;
import forge.game.keyword.KeywordView;
import forge.gamemodes.net.DeltaPacket;
import forge.item.IPaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Writes netplay's changed properties as JSON for the browser. A property that refers to another object is written as {@code {"ref": key}}.
 * Gson's reflection cannot do this. A packet holds a bare id where a property refers to an object, and only the property's type says
 * what kind of object it is. Reflection would also write classes such as ManaCost as their private fields. Each form written here has
 * a TypeScript type in {@link ProtocolTypes}.
 */
public final class PropertyJson {
    private PropertyJson() {}

    public static JsonObject ref(final int type, final int id) {
        final JsonObject r = new JsonObject();
        r.addProperty("ref", DeltaPacket.makeDeltaKey(type, id));
        return r;
    }

    public static Map<Integer, JsonObject> encodeAll(final Map<Integer, Map<TrackableProperty, Object>> objects, final Consumer<String> onSkip) {
        final Map<Integer, JsonObject> out = new LinkedHashMap<>();
        for (final Map.Entry<Integer, Map<TrackableProperty, Object>> e : objects.entrySet()) {
            out.put(e.getKey(), encodeProps(e.getKey(), e.getValue(), onSkip));
        }
        return out;
    }

    public static JsonObject encodeProps(final int deltaKey, final Map<TrackableProperty, Object> props, final Consumer<String> onSkip) {
        final JsonObject out = new JsonObject();
        final int ownerId = DeltaPacket.getIdFromDeltaKey(deltaKey);
        for (final Map.Entry<TrackableProperty, Object> e : props.entrySet()) {
            final JsonElement v = encodeValue(e.getKey(), e.getValue(), ownerId);
            if (v == null) {
                onSkip.accept(e.getKey().name());
            } else {
                out.add(e.getKey().name(), v);
            }
        }
        return out;
    }

    // ProtocolTypes writes the TypeScript type of each of these forms, so a new form needs a type there too
    @SuppressWarnings("unchecked")
    static JsonElement encodeValue(final TrackableProperty prop, final Object value, final int ownerId) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        final TrackableType<?> type = prop.getType();
        if (type == TrackableTypes.CardViewType) {
            return ref(DeltaPacket.TYPE_CARD_VIEW, (Integer) value);
        }
        if (type == TrackableTypes.PlayerViewType) {
            return ref(DeltaPacket.TYPE_PLAYER_VIEW, (Integer) value);
        }
        if (type == TrackableTypes.StackItemViewType) {
            return ref(DeltaPacket.TYPE_STACK_ITEM_VIEW, (Integer) value);
        }
        if (type == TrackableTypes.CardViewCollectionType) {
            return refs(DeltaPacket.TYPE_CARD_VIEW, (List<Integer>) value);
        }
        if (type == TrackableTypes.PlayerViewCollectionType) {
            return refs(DeltaPacket.TYPE_PLAYER_VIEW, (List<Integer>) value);
        }
        if (type == TrackableTypes.StackItemViewListType) {
            return refs(DeltaPacket.TYPE_STACK_ITEM_VIEW, (List<Integer>) value);
        }
        if (type == TrackableTypes.GameEntityViewType) {
            return entity((int[]) value);
        }
        if (type == TrackableTypes.CardStateViewType) {
            // The packet holds only the CardStateName ordinal, and the state object's key is derived from its card
            return ref(DeltaPacket.TYPE_CSV, ownerId * 16 + (Integer) value);
        }
        if (type == TrackableTypes.CombatViewType) {
            return combat((DeltaPacket.CombatData) value);
        }
        if (prop == TrackableProperty.CommanderDamage || prop == TrackableProperty.CommanderCast) {
            return byCommander((Map<Integer, Integer>) value);
        }
        return plain(value);
    }

    // These maps are keyed by the commander's card id, which the browser can look up only as a reference
    private static JsonElement byCommander(final Map<Integer, Integer> values) {
        final JsonArray out = new JsonArray();
        for (final Map.Entry<Integer, Integer> e : values.entrySet()) {
            final JsonObject o = new JsonObject();
            o.add("card", ref(DeltaPacket.TYPE_CARD_VIEW, e.getKey()));
            o.addProperty("value", e.getValue());
            out.add(o);
        }
        return out;
    }

    private static JsonElement entity(final int[] pair) {
        return pair == null ? JsonNull.INSTANCE : ref(pair[0] == 0 ? DeltaPacket.TYPE_CARD_VIEW : DeltaPacket.TYPE_PLAYER_VIEW, pair[1]);
    }

    private static JsonElement refs(final int type, final List<Integer> ids) {
        if (ids == null) {
            return JsonNull.INSTANCE;
        }
        final JsonArray a = new JsonArray();
        for (final Integer id : ids) {
            a.add(id == null || id == -1 ? JsonNull.INSTANCE : ref(type, id));
        }
        return a;
    }

    private static JsonElement combat(final DeltaPacket.CombatData data) {
        final JsonArray bands = new JsonArray();
        for (int i = 0; i < data.bandAttackerIds.size(); i++) {
            final JsonObject band = new JsonObject();
            band.add("attackers", refs(DeltaPacket.TYPE_CARD_VIEW, data.bandAttackerIds.get(i)));
            band.add("defender", entity(data.bandDefenderRefs.get(i)));
            band.add("blockers", refs(DeltaPacket.TYPE_CARD_VIEW, data.bandBlockerIds.get(i)));
            band.add("plannedBlockers", refs(DeltaPacket.TYPE_CARD_VIEW, data.bandPlannedBlockerIds.get(i)));
            bands.add(band);
        }
        return bands;
    }

    static JsonElement plain(final Object v) {
        if (v instanceof Boolean b) {
            return new JsonPrimitive(b);
        }
        if (v instanceof Number n) {
            return new JsonPrimitive(n);
        }
        if (v instanceof String s) {
            return new JsonPrimitive(s);
        }
        if (v instanceof ColorSet c) {
            // ColorSet is an enum, and the UI needs its colour mask
            return new JsonPrimitive(c.getColor());
        }
        if (v instanceof Enum<?> e) {
            return new JsonPrimitive(e.name());
        }
        if (v instanceof ManaCost m) {
            return new JsonPrimitive(manaCost(m));
        }
        if (v instanceof CardTypeView t) {
            return new JsonPrimitive(t.toString());
        }
        if (v instanceof KeywordCollectionView k) {
            final JsonArray a = new JsonArray();
            for (final KeywordView kv : k.getValues()) {
                final JsonObject o = new JsonObject();
                o.addProperty("title", kv.title());
                o.addProperty("reminder", kv.reminderText());
                o.addProperty("keyword", kv.keyword().name());
                // The icon is picked here rather than in the browser so both clients read the same table
                final FSkinProp icon = FSkinProp.iconFromKeyword(kv);
                if (icon != null) {
                    o.addProperty("icon", icon.name());
                }
                a.add(o);
            }
            return a;
        }
        if (v instanceof Multiset<?> m) {
            final JsonObject o = new JsonObject();
            for (final Multiset.Entry<?> e : m.entrySet()) {
                final Object key = e.getElement();
                o.addProperty(key instanceof CounterType ct ? ct.getName() : String.valueOf(key), e.getCount());
            }
            return o;
        }
        if (v instanceof IPaperCard p) {
            final JsonObject o = new JsonObject();
            o.addProperty("name", p.getName());
            o.addProperty("imageKey", p.getCardImageKey());
            return o;
        }
        if (v instanceof Map<?, ?> m) {
            final JsonObject o = new JsonObject();
            for (final Map.Entry<?, ?> e : m.entrySet()) {
                final JsonElement inner = e.getValue() == null ? JsonNull.INSTANCE : plain(e.getValue());
                if (inner == null) {
                    return null;
                }
                o.add(String.valueOf(e.getKey()), inner);
            }
            return o;
        }
        if (v instanceof Collection<?> c) {
            final JsonArray a = new JsonArray();
            for (final Object item : c) {
                final JsonElement inner = item == null ? JsonNull.INSTANCE : plain(item);
                if (inner == null) {
                    return null;
                }
                a.add(inner);
            }
            return a;
        }
        return null;
    }

    // Lands and tokens have no cost at all, which is different from a cost of {0}
    public static String manaCost(final ManaCost cost) {
        return cost == null || cost.isNoCost() ? "" : cost.getSimpleString();
    }
}
