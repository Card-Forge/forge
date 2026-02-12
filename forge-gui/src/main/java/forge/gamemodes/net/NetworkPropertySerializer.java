package forge.gamemodes.net;

import forge.card.CardStateName;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.player.PlayerView;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;

import java.io.*;
import java.util.*;

/**
 * Handles type-specific serialization and deserialization for delta sync.
 * Maps TrackableProperty types to their compact binary representation.
 *
 * Key optimizations:
 * - Object references (CardView, PlayerView) are written as 4-byte IDs only
 * - Collections are written as size + list of IDs (not full objects)
 * - CardStateView/StackItemView are serialized inline with their properties
 * - Enums are written as string names for compatibility
 */
public final class NetworkPropertySerializer {

    // Marker values for serialization
    private static final int MARKER_NULL = -1;
    private static final int MARKER_PRESENT = 1;
    private static final int MARKER_INLINE_OBJECT = 2;
    private static final int MARKER_SKIP = 3;

    /**
     * Data holder for deserialized CardStateView.
     * Used to pass CardStateView property data to the caller for application.
     */
    public static class CardStateViewData {
        public final int id;
        public final CardStateName state;
        public final Map<TrackableProperty, Object> properties;

        public CardStateViewData(int id, CardStateName state, Map<TrackableProperty, Object> properties) {
            this.id = id;
            this.state = state;
            this.properties = properties;
        }
    }

    private NetworkPropertySerializer() {
        // Utility class
    }

    /**
     * Serialize a property value to the network serializer.
     * @param nts the network serializer
     * @param prop the property being serialized
     * @param value the value to serialize
     */
    @SuppressWarnings("unchecked")
    public static void serialize(NetworkTrackableSerializer nts, TrackableProperty prop, Object value) throws IOException {
        TrackableType<?> type = prop.getType();

        // Handle null values
        if (value == null) {
            nts.write(MARKER_NULL);
            return;
        }

        // Boolean
        if (type == TrackableTypes.BooleanType) {
            nts.write(MARKER_PRESENT);
            nts.write((Boolean) value);
        }
        // Integer
        else if (type == TrackableTypes.IntegerType) {
            nts.write(MARKER_PRESENT);
            nts.write((Integer) value);
        }
        // Float
        else if (type == TrackableTypes.FloatType) {
            nts.write(MARKER_PRESENT);
            nts.write((Float) value);
        }
        // String
        else if (type == TrackableTypes.StringType) {
            nts.write(MARKER_PRESENT);
            nts.write((String) value);
        }
        // CardView - write ID only
        else if (type == TrackableTypes.CardViewType) {
            nts.write(MARKER_PRESENT);
            CardView card = (CardView) value;
            nts.write(card.getId());
        }
        // PlayerView - write ID only
        else if (type == TrackableTypes.PlayerViewType) {
            nts.write(MARKER_PRESENT);
            PlayerView player = (PlayerView) value;
            nts.write(player.getId());
        }
        // GameEntityView - polymorphic (CardView or PlayerView)
        else if (type == TrackableTypes.GameEntityViewType) {
            nts.write(MARKER_PRESENT);
            GameEntityView entity = (GameEntityView) value;
            if (entity instanceof CardView) {
                nts.write(0); // Type marker for CardView
                nts.write(entity.getId());
            } else if (entity instanceof PlayerView) {
                nts.write(1); // Type marker for PlayerView
                nts.write(entity.getId());
            } else {
                nts.write(-1); // Unknown type
            }
        }
        // CardView Collection - write IDs only
        else if (type == TrackableTypes.CardViewCollectionType) {
            nts.write(MARKER_PRESENT);
            nts.write((TrackableCollection<CardView>) value);
        }
        // PlayerView Collection - write IDs only
        else if (type == TrackableTypes.PlayerViewCollectionType) {
            nts.write(MARKER_PRESENT);
            nts.write((TrackableCollection<PlayerView>) value);
        }
        // ColorSet - write as int mask (cast byte to int to ensure 4 bytes written)
        else if (type == TrackableTypes.ColorSetType) {
            nts.write(MARKER_PRESENT);
            ColorSet colors = (ColorSet) value;
            nts.write((int) colors.getColor());
        }
        // ManaCost - write as string
        else if (type == TrackableTypes.ManaCostType) {
            nts.write(MARKER_PRESENT);
            ManaCost cost = (ManaCost) value;
            nts.write(ManaCost.serialize(cost));
        }
        // String List
        else if (type == TrackableTypes.StringListType) {
            nts.write(MARKER_PRESENT);
            List<String> list = (List<String>) value;
            nts.write(list.size());
            for (String s : list) {
                nts.write(s);
            }
        }
        // String Set
        else if (type == TrackableTypes.StringSetType) {
            nts.write(MARKER_PRESENT);
            Set<String> set = (Set<String>) value;
            nts.write(set.size());
            for (String s : set) {
                nts.write(s);
            }
        }
        // String Map
        else if (type == TrackableTypes.StringMapType) {
            nts.write(MARKER_PRESENT);
            Map<String, String> map = (Map<String, String>) value;
            nts.write(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                nts.write(entry.getKey());
                nts.write(entry.getValue());
            }
        }
        // Integer Set
        else if (type == TrackableTypes.IntegerSetType) {
            nts.write(MARKER_PRESENT);
            Set<Integer> set = (Set<Integer>) value;
            nts.write(set.size());
            for (Integer i : set) {
                nts.write(i);
            }
        }
        // Integer Map
        else if (type == TrackableTypes.IntegerMapType) {
            nts.write(MARKER_PRESENT);
            Map<Integer, Integer> map = (Map<Integer, Integer>) value;
            nts.write(map.size());
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                nts.write(entry.getKey());
                nts.write(entry.getValue());
            }
        }
        // Mana Map (Byte -> Integer)
        else if (type == TrackableTypes.ManaMapType) {
            nts.write(MARKER_PRESENT);
            Map<Byte, Integer> map = (Map<Byte, Integer>) value;
            nts.write(map.size());
            for (Map.Entry<Byte, Integer> entry : map.entrySet()) {
                nts.write(entry.getKey());
                nts.write(entry.getValue());
            }
        }
        // Counter Map (CounterType -> Integer)
        else if (type == TrackableTypes.CounterMapType) {
            nts.write(MARKER_PRESENT);
            Map<CounterType, Integer> map = (Map<CounterType, Integer>) value;
            nts.write(map.size());
            for (Map.Entry<CounterType, Integer> entry : map.entrySet()) {
                nts.write(entry.getKey().toString());
                nts.write(entry.getValue());
            }
        }
        // CardStateView - serialize inline with all properties
        else if (type == TrackableTypes.CardStateViewType) {
            int startPos = nts.getBytesWritten();
            nts.write(MARKER_INLINE_OBJECT);
            CardStateView csv = (CardStateView) value;
            nts.write(csv.getId());
            String stateName = csv.getState().name();
            nts.write(stateName);  // CardStateName as string
            // Serialize all properties of the CardStateView
            @SuppressWarnings("unchecked")
            Map<TrackableProperty, Object> csvProps = csv.getProps();
            if (csvProps != null && !csvProps.isEmpty()) {
                nts.write(csvProps.size());
                // Collect property names for summary logging
                StringBuilder propNames = new StringBuilder();
                int propCount = 0;
                for (Map.Entry<TrackableProperty, Object> entry : csvProps.entrySet()) {
                    nts.write(entry.getKey().ordinal());
                    serialize(nts, entry.getKey(), entry.getValue());  // recursive
                    if (propCount < 5) {  // Show first 5 property names
                        if (propCount > 0) propNames.append(", ");
                        propNames.append(entry.getKey().name());
                    }
                    propCount++;
                }
                if (propCount > 5) {
                    propNames.append(", ...");
                }
                int totalBytes = nts.getBytesWritten() - startPos;
                NetworkDebugLogger.trace("[CSV-Serialize] CardStateView ID=%d state=%s: %d props (%s), %d bytes",
                        csv.getId(), stateName, csvProps.size(), propNames, totalBytes);
            } else {
                nts.write(0);
                NetworkDebugLogger.trace("[CSV-Serialize] CardStateView ID=%d state=%s: 0 props",
                        csv.getId(), stateName);
            }
        }
        // StackItemView - skip, will be synced via full state
        else if (type == TrackableTypes.StackItemViewType) {
            nts.write(MARKER_SKIP);
        }
        // StackItemView List - skip, will be synced via full state
        else if (type == TrackableTypes.StackItemViewListType) {
            nts.write(MARKER_SKIP);
        }
        // Enum types - write as string name (must use same check as deserialize)
        else if (isEnumType(prop)) {
            nts.write(MARKER_PRESENT);
            nts.write(((Enum<?>) value).name());
        }
        // Skip complex types that would cause large serialization
        // CombatView, KeywordCollectionView, CardTypeView, IPaperCard, GenericMap
        else if (type == TrackableTypes.CombatViewType ||
                 type == TrackableTypes.KeywordCollectionViewType ||
                 type == TrackableTypes.CardTypeViewType ||
                 type == TrackableTypes.IPaperCardType ||
                 type == TrackableTypes.GenericMapType) {
            // Skip these - they will be synced via full state
            nts.write(MARKER_SKIP);
        }
        // Fallback for any other types - skip to avoid large serialization
        else {
            nts.write(MARKER_SKIP);
        }
    }

    /**
     * Deserialize a property value from the network deserializer.
     * @param ntd the network deserializer
     * @param prop the property being deserialized
     * @param oldValue the previous value (for collections that may need lookup)
     * @return the deserialized value
     */
    @SuppressWarnings("unchecked")
    public static Object deserialize(NetworkTrackableDeserializer ntd, TrackableProperty prop, Object oldValue) throws IOException {
        TrackableType<?> type = prop.getType();

        // Check marker
        int markerPos = ntd.getBytesRead();
        int marker = ntd.readInt();

        // Validate marker value
        if (marker != MARKER_NULL && marker != MARKER_PRESENT && marker != MARKER_INLINE_OBJECT && marker != MARKER_SKIP) {
            NetworkDebugLogger.error("[Deserialize] WARNING: Unexpected marker %d (0x%08X) at byte %d for property %s (type=%s)",
                    marker, marker, markerPos, prop, type);
            NetworkDebugLogger.error("[Deserialize] Expected markers: NULL=-1, PRESENT=1, INLINE_OBJECT=2, SKIP=3");
            throw new IOException(String.format("Invalid marker %d (0x%08X) at byte %d for property %s",
                    marker, marker, markerPos, prop));
        }

        if (marker == MARKER_NULL) {
            return null;
        }
        if (marker == MARKER_SKIP) {
            // Property was skipped - keep old value
            return oldValue;
        }

        // Boolean
        if (type == TrackableTypes.BooleanType) {
            return ntd.readBoolean();
        }
        // Integer
        else if (type == TrackableTypes.IntegerType) {
            return ntd.readInt();
        }
        // Float
        else if (type == TrackableTypes.FloatType) {
            return ntd.readFloat();
        }
        // String
        else if (type == TrackableTypes.StringType) {
            return ntd.readString();
        }
        // CardView - lookup by ID
        else if (type == TrackableTypes.CardViewType) {
            return ntd.readCardView();
        }
        // PlayerView - lookup by ID
        else if (type == TrackableTypes.PlayerViewType) {
            return ntd.readPlayerView();
        }
        // GameEntityView - polymorphic lookup
        else if (type == TrackableTypes.GameEntityViewType) {
            int entityType = ntd.readInt();
            if (entityType == 0) {
                return ntd.readCardView();
            } else if (entityType == 1) {
                return ntd.readPlayerView();
            }
            return null;
        }
        // CardView Collection
        else if (type == TrackableTypes.CardViewCollectionType) {
            return ntd.readCardViewCollection((TrackableCollection<CardView>) oldValue);
        }
        // PlayerView Collection
        else if (type == TrackableTypes.PlayerViewCollectionType) {
            return ntd.readPlayerViewCollection((TrackableCollection<PlayerView>) oldValue);
        }
        // ColorSet
        else if (type == TrackableTypes.ColorSetType) {
            return ColorSet.fromMask(ntd.readInt());
        }
        // ManaCost
        else if (type == TrackableTypes.ManaCostType) {
            String costString = ntd.readString();
            if (costString != null && !costString.isEmpty()) {
                return ManaCost.deserialize(costString);
            }
            return ManaCost.NO_COST;
        }
        // String List
        else if (type == TrackableTypes.StringListType) {
            int size = ntd.readInt();
            if (size > 0) {
                List<String> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(ntd.readString());
                }
                return list;
            }
            return null;
        }
        // String Set
        else if (type == TrackableTypes.StringSetType) {
            int size = ntd.readInt();
            if (size > 0) {
                Set<String> set = new HashSet<>(size);
                for (int i = 0; i < size; i++) {
                    set.add(ntd.readString());
                }
                return set;
            }
            return null;
        }
        // String Map
        else if (type == TrackableTypes.StringMapType) {
            int size = ntd.readInt();
            if (size > 0) {
                Map<String, String> map = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    map.put(ntd.readString(), ntd.readString());
                }
                return map;
            }
            return null;
        }
        // Integer Set
        else if (type == TrackableTypes.IntegerSetType) {
            int size = ntd.readInt();
            if (size > 0) {
                Set<Integer> set = new HashSet<>(size);
                for (int i = 0; i < size; i++) {
                    set.add(ntd.readInt());
                }
                return set;
            }
            return null;
        }
        // Integer Map
        else if (type == TrackableTypes.IntegerMapType) {
            int size = ntd.readInt();
            if (size > 0) {
                Map<Integer, Integer> map = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    map.put(ntd.readInt(), ntd.readInt());
                }
                return map;
            }
            return null;
        }
        // Mana Map
        else if (type == TrackableTypes.ManaMapType) {
            int size = ntd.readInt();
            if (size > 0) {
                Map<Byte, Integer> map = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    map.put(ntd.readByte(), ntd.readInt());
                }
                return map;
            }
            return null;
        }
        // Counter Map
        else if (type == TrackableTypes.CounterMapType) {
            int size = ntd.readInt();
            if (size > 0) {
                Map<CounterType, Integer> map = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    map.put(CounterType.getType(ntd.readString()), ntd.readInt());
                }
                return map;
            }
            return null;
        }
        // CardStateView - read inline data
        else if (type == TrackableTypes.CardStateViewType) {
            int startPos = ntd.getBytesRead();
            int csvId = ntd.readInt();
            String stateName = ntd.readString();
            CardStateName csvState = CardStateName.valueOf(stateName);
            int propCount = ntd.readInt();

            Map<TrackableProperty, Object> props = new HashMap<>();
            StringBuilder propNames = new StringBuilder();
            int propsLogged = 0;

            for (int i = 0; i < propCount; i++) {
                int propStartPos = ntd.getBytesRead();
                int ordinal = ntd.readInt();

                // Validate ordinal before deserializing
                if (ordinal < 0 || ordinal >= TrackableProperty.values().length) {
                    NetworkDebugLogger.error("[CSV-Deserialize] ERROR: Invalid ordinal %d (0x%08X) at byte %d for prop %d/%d in CSV ID=%d",
                            ordinal, ordinal, propStartPos, i + 1, propCount, csvId);
                    NetworkDebugLogger.error("[CSV-Deserialize] Valid ordinal range: 0-%d",
                            TrackableProperty.values().length - 1);
                    throw new IOException("Invalid TrackableProperty ordinal: " + ordinal +
                            " at byte position " + propStartPos);
                }

                TrackableProperty csvProp = TrackableProperty.deserialize(ordinal);
                // Recursively deserialize the property value
                Object propValue = deserialize(ntd, csvProp, null);
                props.put(csvProp, propValue);

                // Collect property names for summary (first 5 only)
                if (propsLogged < 5) {
                    if (propsLogged > 0) propNames.append(", ");
                    propNames.append(csvProp.name());
                    propsLogged++;
                }
            }

            if (propCount > 5) {
                propNames.append(", ...");
            }
            int totalBytes = ntd.getBytesRead() - startPos;
            NetworkDebugLogger.trace("[CSV-Deserialize] CardStateView ID=%d state=%s: %d props (%s), %d bytes",
                    csvId, stateName, propCount, propNames, totalBytes);

            // Return as CardStateViewData to be handled by applyDeltaToObject
            return new CardStateViewData(csvId, csvState, props);
        }
        // StackItemView - skipped, keep old value
        else if (type == TrackableTypes.StackItemViewType) {
            return oldValue;
        }
        // StackItemView List - skipped, keep old value
        else if (type == TrackableTypes.StackItemViewListType) {
            return oldValue;
        }
        // Enum types - read string name and convert
        else if (isEnumType(prop)) {
            String name = ntd.readString();
            return deserializeEnum(prop, name, oldValue);
        }
        // For skipped/complex types, keep old value
        else {
            return oldValue;
        }
    }

    /**
     * Check if a property is an enum type.
     */
    private static boolean isEnumType(TrackableProperty prop) {
        // Check if the property's default value is an enum
        Object defaultValue = prop.getDefaultValue();
        if (defaultValue != null && defaultValue instanceof Enum) {
            return true;
        }
        // Known enum properties
        switch (prop) {
            case Zone:
            case GamePieceType:
            case ChosenDirection:
            case ChosenEvenOdd:
            case Rarity:
            case GameType:
            case Phase:
                return true;
            default:
                return false;
        }
    }

    /**
     * Deserialize an enum value from its name.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object deserializeEnum(TrackableProperty prop, String name, Object oldValue) {
        if (name == null || name.isEmpty()) {
            return oldValue;
        }
        try {
            // Get the enum class from the old value or property default
            Class<? extends Enum> enumClass = null;
            if (oldValue != null && oldValue instanceof Enum) {
                enumClass = ((Enum<?>) oldValue).getClass();
            }
            if (enumClass == null) {
                // Try to get from property
                switch (prop) {
                    case Zone:
                        enumClass = forge.game.zone.ZoneType.class;
                        break;
                    case GamePieceType:
                        enumClass = forge.card.GamePieceType.class;
                        break;
                    case ChosenDirection:
                        enumClass = forge.game.Direction.class;
                        break;
                    case ChosenEvenOdd:
                        enumClass = forge.game.EvenOdd.class;
                        break;
                    case Rarity:
                        enumClass = forge.card.CardRarity.class;
                        break;
                    case GameType:
                        enumClass = forge.game.GameType.class;
                        break;
                    case Phase:
                        enumClass = forge.game.phase.PhaseType.class;
                        break;
                    default:
                        return oldValue;
                }
            }
            return Enum.valueOf(enumClass, name);
        } catch (Exception e) {
            return oldValue;
        }
    }
}
