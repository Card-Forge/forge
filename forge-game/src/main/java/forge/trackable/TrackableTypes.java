package forge.trackable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import forge.card.CardType;
import forge.card.CardTypeView;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.item.IPaperCard;

public class TrackableTypes {
    public static abstract class TrackableType<T> {
        private TrackableType() {
        }

        protected void updateObjLookup(Tracker tracker, T newObj) {
        }
        protected void copyChangedProps(TrackableObject from, TrackableObject to, TrackableProperty prop) {
            to.set(prop, from.get(prop));
        }
        protected abstract T getDefaultValue();
    }

    public static abstract class TrackableObjectType<T extends TrackableObject> extends TrackableType<T> {
        private TrackableObjectType() {
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

        @Override
        protected void copyChangedProps(TrackableObject from, TrackableObject to, TrackableProperty prop) {
            T newObj = from.get(prop);
            if (newObj != null) {
                T existingObj = newObj.getTracker().getObj(this, newObj.getId());
                if (existingObj != null) { //if object exists already, update its changed properties
                    existingObj.copyChangedProps(newObj);
                    newObj = existingObj;
                }
                else { //if object is new, cache in object lookup
                    newObj.getTracker().putObj(this, newObj.getId(), newObj);
                }
            }
            to.set(prop, newObj);
        }
    }

    private static abstract class TrackableCollectionType<T extends TrackableObject> extends TrackableType<TrackableCollection<T>> {
        private final TrackableObjectType<T> itemType;

        private TrackableCollectionType(TrackableObjectType<T> itemType0) {
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

        @Override
        protected void copyChangedProps(TrackableObject from, TrackableObject to, TrackableProperty prop) {
            TrackableCollection<T> newCollection = from.get(prop);
            if (newCollection != null) {
                // Snapshot via toArray: the loop below clears and rebuilds the collection,
                // so direct iteration would throw ConcurrentModificationException
                @SuppressWarnings("unchecked")
                T[] items = (T[]) newCollection.toArray(new TrackableObject[0]);
                newCollection.clear();
                for (T newObj : items) {
                    if (newObj != null) {
                        T existingObj = from.getTracker().getObj(itemType, newObj.getId());
                        if (existingObj != null) {
                            // Skip CardView collections — cross-zone refs like Commander hold stale copies
                            if (prop.getType() != TrackableTypes.CardViewCollectionType &&
                                    prop.getType() != TrackableTypes.StackItemViewListType) {
                                existingObj.copyChangedProps(newObj);
                            }
                            newCollection.add(existingObj);
                        } else {
                            from.getTracker().putObj(itemType, newObj.getId(), newObj);
                            newCollection.add(newObj);
                        }
                    }
                }
            }
            to.set(prop, newCollection);
        }
    }

    public static final TrackableType<Boolean> BooleanType = new TrackableType<Boolean>() {
        @Override
        public Boolean getDefaultValue() {
            return false;
        }
    };
    public static final TrackableType<Integer> IntegerType = new TrackableType<Integer>() {
        @Override
        public Integer getDefaultValue() {
            return 0;
        }
    };
    public static final TrackableType<Float> FloatType = new TrackableType<Float>() {
        @Override
        public Float getDefaultValue() {
            return 0f;
        }
    };
    public static final TrackableType<String> StringType = new TrackableType<String>() {
        @Override
        public String getDefaultValue() {
            return "";
        }
    };

    //make this quicker than having to define a new class for every single enum
    private static Map<Class<? extends Enum<?>>, TrackableType<?>> enumTypes = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> TrackableType<E> EnumType(final Class<E> enumType) {
        TrackableType<E> type = (TrackableType<E>)enumTypes.get(enumType);
        if (type == null) {
            type = new TrackableType<E>() {
                @Override
                public E getDefaultValue() {
                    return null;
                }
            };
            enumTypes.put(enumType, type);
        }
        return type;
    }

    public static final TrackableObjectType<CardView> CardViewType = new TrackableObjectType<CardView>() {
        @Override
        protected CardView getDefaultValue() {
            return null;
        }
    };

    public static final TrackableType<IPaperCard> IPaperCardType = new TrackableType<IPaperCard>() {
        @Override
        protected IPaperCard getDefaultValue() {
            return null;
        }
    };

    public static final TrackableCollectionType<CardView> CardViewCollectionType = new TrackableCollectionType<CardView>(CardViewType) {
        @Override
        protected TrackableCollection<CardView> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableObjectType<CardStateView> CardStateViewType = new TrackableObjectType<CardStateView>() {
        @Override
        protected CardStateView getDefaultValue() {
            return null;
        }
        @Override
        protected void copyChangedProps(TrackableObject from, TrackableObject to, TrackableProperty prop) {
            // CardStateViews share their parent CardView's ID, so multiple states
            // (CurrentState, AlternateState) have the same (type, id) key. The base
            // implementation uses tracker.getObj(type, id) which returns the wrong
            // state. Instead, look up the existing state directly via the property.
            CardStateView newCsv = from.get(prop);
            CardStateView existingCsv = to.get(prop);
            if (newCsv != null && existingCsv != null) {
                existingCsv.copyChangedProps(newCsv);
                to.set(prop, existingCsv);
            } else {
                to.set(prop, newCsv);
            }
        }
    };
    public static final TrackableType<CardTypeView> CardTypeViewType = new TrackableType<CardTypeView>() {
        @Override
        protected CardTypeView getDefaultValue() {
            return CardType.EMPTY;
        }
    };
    public static final TrackableObjectType<PlayerView> PlayerViewType = new TrackableObjectType<PlayerView>() {
        @Override
        protected PlayerView getDefaultValue() {
            return null;
        }
    };
    public static final TrackableCollectionType<PlayerView> PlayerViewCollectionType = new TrackableCollectionType<PlayerView>(PlayerViewType) {
        @Override
        protected TrackableCollection<PlayerView> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableObjectType<GameEntityView> GameEntityViewType = new TrackableObjectType<GameEntityView>() {
        @Override
        protected GameEntityView getDefaultValue() {
            return null;
        }
    };
    public static final TrackableObjectType<StackItemView> StackItemViewType = new TrackableObjectType<StackItemView>() {
        @Override
        protected StackItemView getDefaultValue() {
            return null;
        }
    };
    public static final TrackableCollectionType<StackItemView> StackItemViewListType = new TrackableCollectionType<StackItemView>(StackItemViewType) {
        @Override
        protected TrackableCollection<StackItemView> getDefaultValue() {
            return new TrackableCollection<>();
        }
    };
    public static final TrackableType<ManaCost> ManaCostType = new TrackableType<ManaCost>() {
        @Override
        public ManaCost getDefaultValue() {
            return ManaCost.NO_COST;
        }
    };
    public static final TrackableType<ColorSet> ColorSetType = new TrackableType<ColorSet>() {
        @Override
        public ColorSet getDefaultValue() {
            return ColorSet.C;
        }
    };
    public static final TrackableType<List<String>> StringListType = new TrackableType<List<String>>() {
        @Override
        public List<String> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableType<Set<String>> StringSetType = new TrackableType<Set<String>>() {
        @Override
        public Set<String> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableType<Map<String, String>> StringMapType = new TrackableType<Map<String, String>>() {
        @Override
        public Map<String, String> getDefaultValue() {
            return null;
        }
    };

    public static final TrackableType<Set<Integer>> IntegerSetType = new TrackableType<Set<Integer>>() {
        @Override
        public Set<Integer> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableType<Map<Integer, Integer>> IntegerMapType = new TrackableType<Map<Integer, Integer>>() {
        @Override
        public Map<Integer, Integer> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableType<Map<Byte, Integer>> ManaMapType = new TrackableType<Map<Byte, Integer>>() {
        @Override
        public Map<Byte, Integer> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableType<Map<CounterType, Integer>> CounterMapType = new TrackableType<Map<CounterType, Integer>>() {
        @Override
        public Map<CounterType, Integer> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableType<Map<Object, Object>> GenericMapType = new TrackableType<Map<Object, Object>>() {
        @Override
        public Map<Object, Object> getDefaultValue() {
            return null;
        }
    };
    public static final TrackableObjectType<CombatView> CombatViewType = new TrackableObjectType<CombatView>() {
        @Override
        protected CombatView getDefaultValue() {
            return null;
        }
    };
}
