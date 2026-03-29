package forge.game.card;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;


public record CounterListType(String name, String desc, int red, int green, int blue) implements CounterType {
    private static Map<String, CounterListType> sMap = Maps.newHashMap();

    public static CounterListType get(String s) {
        return sMap.get(s);
    }

    public static Collection<CounterListType> getValues() {
        return sMap.values();
    }

    public static void add(String name, String desc, int red, int green, int blue) {
        sMap.put(desc, new CounterListType(StringUtils.capitalize(name), desc, red, green, blue));
    }

    public static final void parseTypes(List<String> content) {
        for (String line : content) {
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            // Name=Description,red,green,blue
            String k[] = line.split("=");
            String l[] = k[1].split(",");
            add(k[0], l[0], Integer.valueOf(l[1]), Integer.valueOf(l[2]), Integer.valueOf(l[3]));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCounterOnCardDisplayName() {
        return desc;
    }

    @Override
    public boolean is(CounterEnumType eType) {
        return false;
    }

    @Override
    public boolean isKeywordCounter() {
        return false;
    }

    @Override
    public int getRed() {
        return this.red;
    }

    @Override
    public int getGreen() {
        return this.green;
    }

    @Override
    public int getBlue() {
        return this.blue;
    }

}
