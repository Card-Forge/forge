package forge.trackable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import forge.card.CardType;
import forge.card.CardTypeView;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.combat.CombatView;
import forge.game.keyword.KeywordCollectionView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.item.IPaperCard;

public class TrackableTypes {
    public static class TrackableType<T> {
        private T defaultValue;
        private TrackableType(T defaultValue) {
            this.defaultValue = defaultValue;
        }

        protected void updateObjLookup(Tracker tracker, T newObj) {
        }

        protected T getDefaultValue() {
            return defaultValue;
        }
    }

    public static class TrackableObjectType<T extends TrackableObject> extends TrackableType<T> {
        private TrackableObjectType() {
            super(null);
        }

        public T lookup(T from) {
            if (from == null) { return null; }
            T to = from.getTracker().getObj(this, from.getId());
            if (to == null) {
                from.getTracker().putObj(this, from.getId(), from);
                return from;
            }
            return to;
        }

        @Override
        protected void updateObjLookup(Tracker tracker, T newObj) {
            if (tracker == null) { return; }
            if (newObj != null && !tracker.hasObj(this, newObj.getId())) {
                tracker.putObj(this, newObj.getId(), newObj);
                newObj.updateObjLookup();
            }
        }
    }

    private static class TrackableCollectionType<T extends TrackableObject> extends TrackableType<TrackableCollection<T>> {
        private final TrackableObjectType<T> itemType;

        private TrackableCollectionType(TrackableObjectType<T> itemType0) {
            super(null);
            itemType = itemType0;
        }

        @Override
        protected void updateObjLookup(Tracker tracker, TrackableCollection<T> newCollection) {
            if (newCollection != null) {
                for (T newObj : newCollection) {
                    if (newObj != null) {
                        itemType.updateObjLookup(tracker, newObj);
                    }
                }
            }
        }
    }

    public static final TrackableType<Boolean> BooleanType = new TrackableType<Boolean>(false);
    public static final TrackableType<Integer> IntegerType = new TrackableType<Integer>(0);
    public static final TrackableType<Float> FloatType = new TrackableType<Float>(0f);
    public static final TrackableType<String> StringType = new TrackableType<String>("");
    public static final TrackableType<Object> ObjectType = new TrackableType<Object>(null);

    //make this quicker than having to define a new class for every single enum
    private static Map<Class<? extends Enum<?>>, TrackableType<?>> enumTypes = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> TrackableType<E> EnumType(final Class<E> enumType) {
        return (TrackableType<E>)enumTypes.computeIfAbsent(enumType, (t) -> new TrackableType<E>(null));
    }

    public static final TrackableObjectType<CardView> CardViewType = new TrackableObjectType<CardView>();

    public static final TrackableType<IPaperCard> IPaperCardType = new TrackableType<IPaperCard>(null);

    public static final TrackableCollectionType<CardView> CardViewCollectionType = new TrackableCollectionType<CardView>(CardViewType);
    public static final TrackableObjectType<CardStateView> CardStateViewType = new TrackableObjectType<>();
    public static final TrackableType<CardTypeView> CardTypeViewType = new TrackableType<>(CardType.EMPTY);
    public static final TrackableObjectType<PlayerView> PlayerViewType = new TrackableObjectType<>();
    public static final TrackableCollectionType<PlayerView> PlayerViewCollectionType = new TrackableCollectionType<>(PlayerViewType);
    public static final TrackableObjectType<GameEntityView> GameEntityViewType = new TrackableObjectType<>();
    public static final TrackableObjectType<StackItemView> StackItemViewType = new TrackableObjectType<>();
    public static final TrackableCollectionType<StackItemView> StackItemViewListType = new TrackableCollectionType<>(StackItemViewType);
    public static final TrackableObjectType<CombatView> CombatViewType = new TrackableObjectType<>();
    public static final TrackableType<ManaCost> ManaCostType = new TrackableType<>(ManaCost.NO_COST);
    public static final TrackableType<ColorSet> ColorSetType = new TrackableType<>(ColorSet.C);
    public static final TrackableType<List<String>> StringListType = new TrackableType<>(null);
    public static final TrackableType<Set<String>> StringSetType = new TrackableType<>(null);
    public static final TrackableType<Map<String, String>> StringMapType = new TrackableType<>(null);

    public static final TrackableType<Set<Integer>> IntegerSetType = new TrackableType<>(null);
    public static final TrackableType<Map<Integer, Integer>> IntegerMapType = new TrackableType<>(null);
    public static final TrackableType<Map<Byte, Integer>> ManaMapType = new TrackableType<>(null);
    public static final TrackableType<Multiset<CounterType>> CounterMapType = new TrackableType<>(null);
    public static final TrackableType<Map<Object, Object>> GenericMapType = new TrackableType<>(null);
    public static final TrackableType<KeywordCollectionView> KeywordCollectionViewType = new TrackableType<>(KeywordCollectionView.EMPTY);
}
