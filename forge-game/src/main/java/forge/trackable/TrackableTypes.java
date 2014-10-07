package forge.trackable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.card.CounterType;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;

public class TrackableTypes {
    public static abstract class TrackableType<T> {
        private TrackableType() {
        }

        protected abstract T getDefaultValue();
        protected abstract T loadFromXml(Element el, String name, T oldValue);
        protected abstract void saveToXml(Element el, String name, T value);
    }

    public static final TrackableType<Boolean> BooleanType = new TrackableType<Boolean>() {
        @Override
        public Boolean getDefaultValue() {
            return false;
        }

        @Override
        public Boolean loadFromXml(Element el, String name, Boolean oldValue) {
            String value = el.getAttribute(name);
            if (value.equals("1")) {
                return true;
            }
            if (value.equals("0")) {
                return false;
            }
            return oldValue;
        }

        @Override
        public void saveToXml(Element el, String name, Boolean value) {
            el.setAttribute(name, value ? "1" : "0");
        }
    };
    public static final TrackableType<Integer> IntegerType = new TrackableType<Integer>() {
        @Override
        public Integer getDefaultValue() {
            return 0;
        }

        @Override
        public Integer loadFromXml(Element el, String name, Integer oldValue) {
            String value = el.getAttribute(name);
            if (value.length() > 0) {
                try {
                    return Integer.parseInt(value);
                }
                catch (Exception ex) {}
            }
            return oldValue;
        }

        @Override
        public void saveToXml(Element el, String name, Integer value) {
            el.setAttribute(name, value.toString());
        }
    };
    public static final TrackableType<String> StringType = new TrackableType<String>() {
        @Override
        public String getDefaultValue() {
            return "";
        }

        @Override
        public String loadFromXml(Element el, String name, String oldValue) {
            return el.getAttribute(name);
        }

        @Override
        public void saveToXml(Element el, String name, String value) {
            el.setAttribute(name, value);
        }
    };

    //make this quicker than having to define a new class for every single enum
    private static HashMap<Class<? extends Enum<?>>, TrackableType<?>> enumTypes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> TrackableType<E> EnumType(final Class<E> enumType) {
        TrackableType<E> type = (TrackableType<E>)enumTypes.get(enumType);
        if (type == null) {
            type = new TrackableType<E>() {
                @Override
                public E getDefaultValue() {
                    return null;
                }

                @Override
                public E loadFromXml(Element el, String name, E oldValue) {
                    try {
                        return Enum.valueOf(enumType, el.getAttribute(name));
                    }
                    catch (Exception e) {
                        return oldValue;
                    }
                }

                @Override
                public void saveToXml(Element el, String name, E value) {
                    el.setAttribute(name, value.name());
                }
            };
            enumTypes.put(enumType, type);
        }
        return type;
    }

    public static final TrackableType<CardView> CardViewType = new TrackableType<CardView>() {
        @Override
        protected CardView getDefaultValue() {
            return null;
        }

        @Override
        protected CardView loadFromXml(Element el, String name, CardView oldValue) {
            //TODO
            return oldValue;
        }

        @Override
        protected void saveToXml(Element el, String name, CardView value) {
            //TODO
        }
    };
    public static final TrackableType<TrackableCollection<CardView>> CardViewCollectionType = new TrackableType<TrackableCollection<CardView>>() {
        @Override
        protected TrackableCollection<CardView> getDefaultValue() {
            return null;
        }

        @Override
        protected TrackableCollection<CardView> loadFromXml(Element el, String name, TrackableCollection<CardView> oldValue) {
            //TODO
            return oldValue;
        }

        @Override
        protected void saveToXml(Element el, String name, TrackableCollection<CardView> value) {
            //TODO
        }
    };
    public static final TrackableType<CardStateView> CardStateViewType = new TrackableType<CardStateView>() {
        @Override
        protected CardStateView getDefaultValue() {
            return null;
        }

        @Override
        protected CardStateView loadFromXml(Element el, String name, CardStateView oldValue) {
            //TODO
            return oldValue;
        }

        @Override
        protected void saveToXml(Element el, String name, CardStateView value) {
            //TODO
        }
    };
    public static final TrackableType<PlayerView> PlayerViewType = new TrackableType<PlayerView>() {
        @Override
        protected PlayerView getDefaultValue() {
            return null;
        }

        @Override
        protected PlayerView loadFromXml(Element el, String name, PlayerView oldValue) {
            //TODO
            return oldValue;
        }

        @Override
        protected void saveToXml(Element el, String name, PlayerView value) {
            //TODO
        }
    };
    public static final TrackableType<TrackableCollection<PlayerView>> PlayerViewCollectionType = new TrackableType<TrackableCollection<PlayerView>>() {
        @Override
        protected TrackableCollection<PlayerView> getDefaultValue() {
            return null;
        }

        @Override
        protected TrackableCollection<PlayerView> loadFromXml(Element el, String name, TrackableCollection<PlayerView> oldValue) {
            //TODO
            return oldValue;
        }

        @Override
        protected void saveToXml(Element el, String name, TrackableCollection<PlayerView> value) {
            //TODO
        }
    };
    public static final TrackableType<GameEntityView> GameEntityViewType = new TrackableType<GameEntityView>() {
        @Override
        protected GameEntityView getDefaultValue() {
            return null;
        }

        @Override
        protected GameEntityView loadFromXml(Element el, String name, GameEntityView oldValue) {
            //TODO
            return oldValue;
        }

        @Override
        protected void saveToXml(Element el, String name, GameEntityView value) {
            //TODO
        }
    };
    public static final TrackableType<StackItemView> StackItemViewType = new TrackableType<StackItemView>() {
        @Override
        protected StackItemView getDefaultValue() {
            return null;
        }

        @Override
        protected StackItemView loadFromXml(Element el, String name, StackItemView oldValue) {
            //TODO
            return oldValue;
        }

        @Override
        protected void saveToXml(Element el, String name, StackItemView value) {
            //TODO
        }
    };
    public static final TrackableType<ManaCost> ManaCostType = new TrackableType<ManaCost>() {
        @Override
        public ManaCost getDefaultValue() {
            return ManaCost.NO_COST;
        }

        @Override
        public ManaCost loadFromXml(Element el, String name, ManaCost oldValue) {
            String value = el.getAttribute(name);
            if (value.length() > 0) {
                try {
                    return ManaCost.deserialize(value);
                }
                catch (Exception ex) {}
            }
            return oldValue;
        }

        @Override
        public void saveToXml(Element el, String name, ManaCost value) {
            el.setAttribute(name, ManaCost.serialize(value));
        }
    };
    public static final TrackableType<ColorSet> ColorSetType = new TrackableType<ColorSet>() {
        @Override
        public ColorSet getDefaultValue() {
            return ColorSet.getNullColor();
        }

        @Override
        public ColorSet loadFromXml(Element el, String name, ColorSet oldValue) {
            String value = el.getAttribute(name);
            if (value.length() > 0) {
                try {
                    return ColorSet.fromMask(Byte.parseByte(value));
                }
                catch (Exception ex) {}
            }
            return oldValue;
        }

        @Override
        public void saveToXml(Element el, String name, ColorSet value) {
            el.setAttribute(name, Byte.toString(value.getColor()));
        }
    };
    public static final TrackableType<Set<String>> StringSetType = new TrackableType<Set<String>>() {
        private static final char DELIM = (char)5;

        @Override
        public Set<String> getDefaultValue() {
            return null;
        }

        @Override
        public Set<String> loadFromXml(Element el, String name, Set<String> oldValue) {
            String value = el.getAttribute(name);
            if (value.length() > 0) {
                return Sets.newHashSet(StringUtils.split(value, DELIM));
            }
            return null;
        }

        @Override
        public void saveToXml(Element el, String name, Set<String> value) {
            el.setAttribute(name, StringUtils.join(value, DELIM));
        }
    };
    public static final TrackableType<Map<String, String>> StringMapType = new TrackableType<Map<String, String>>() {
        private static final char DELIM_1 = (char)5;
        private static final char DELIM_2 = (char)6;

        @Override
        public Map<String, String> getDefaultValue() {
            return null;
        }

        @Override
        public Map<String, String> loadFromXml(Element el, String name, Map<String, String> oldValue) {
            String value = el.getAttribute(name);
            if (value.length() > 0) {
                Map<String, String> map = ImmutableMap.of();
                String[] entries = StringUtils.split(value, DELIM_1);
                for (String entry : entries) {
                    int idx = entry.indexOf(DELIM_2);
                    map.put(entry.substring(0, idx), entry.substring(idx + 1));
                }
                return map;
            }
            return null;
        }

        @Override
        public void saveToXml(Element el, String name, Map<String, String> value) {
            StringBuilder builder = new StringBuilder();
            for (Entry<String, String> entry : value.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(DELIM_1);
                }
                builder.append(entry.getKey() + DELIM_2 + entry.getValue());
            }
            el.setAttribute(name, builder.toString());
        }
    };
    public static final TrackableType<Map<Byte, Integer>> ManaMapType = new TrackableType<Map<Byte, Integer>>() {
        private static final char DELIM_1 = (char)5;
        private static final char DELIM_2 = (char)6;

        @Override
        public Map<Byte, Integer> getDefaultValue() {
            return null;
        }

        @Override
        public Map<Byte, Integer> loadFromXml(Element el, String name, Map<Byte, Integer> oldValue) {
            String value = el.getAttribute(name);
            if (value.length() > 0) {
                Map<Byte, Integer> map = Maps.newHashMapWithExpectedSize(MagicColor.NUMBER_OR_COLORS + 1);
                String[] entries = StringUtils.split(value, DELIM_1);
                for (String entry : entries) {
                    int idx = entry.indexOf(DELIM_2);
                    map.put(Byte.valueOf(entry.substring(0, idx)), Integer.valueOf(entry.substring(idx + 1)));
                }
                return map;
            }
            return null;
        }

        @Override
        public void saveToXml(Element el, String name, Map<Byte, Integer> value) {
            StringBuilder builder = new StringBuilder();
            for (Entry<Byte, Integer> entry : value.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(DELIM_1);
                }
                builder.append(Byte.toString(entry.getKey()) + DELIM_2 + Integer.toString(entry.getValue()));
            }
            el.setAttribute(name, builder.toString());
        }
    };
    public static final TrackableType<Map<CounterType, Integer>> CounterMapType = new TrackableType<Map<CounterType, Integer>>() {
        private static final char DELIM_1 = (char)5;
        private static final char DELIM_2 = (char)6;

        @Override
        public Map<CounterType, Integer> getDefaultValue() {
            return null;
        }

        @Override
        public Map<CounterType, Integer> loadFromXml(Element el, String name, Map<CounterType, Integer> oldValue) {
            String value = el.getAttribute(name);
            if (value.length() > 0) {
                Map<CounterType, Integer> map = new TreeMap<CounterType, Integer>();
                String[] entries = StringUtils.split(value, DELIM_1);
                for (String entry : entries) {
                    int idx = entry.indexOf(DELIM_2);
                    map.put(CounterType.valueOf(entry.substring(0, idx)), Integer.valueOf(entry.substring(idx + 1)));
                }
                return map;
            }
            return null;
        }

        @Override
        public void saveToXml(Element el, String name, Map<CounterType, Integer> value) {
            StringBuilder builder = new StringBuilder();
            for (Entry<CounterType, Integer> entry : value.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(DELIM_1);
                }
                builder.append(entry.getKey().name() + DELIM_2 + Integer.toString(entry.getValue()));
            }
            el.setAttribute(name, builder.toString());
        }
    };
}
