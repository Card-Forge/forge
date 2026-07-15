package forge.game.card;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

import com.google.common.collect.Maps;

public record CounterCustomType(String keyword) implements CounterType {
    private static Map<String, CounterCustomType> sMap = Maps.newHashMap();

    public static CounterCustomType get(String s) {
        if (!sMap.containsKey(s)) {
            sMap.put(s, new CounterCustomType(s));
        }
        return sMap.get(s);
    }

    public static Set<CounterType> getValues() {
        return new LinkedHashSet<CounterType>(sMap.values());
    }
    
    @Override
    public String toString() {
        return keyword;
    }

    public String getName() {
        return keyword;
    }
}
