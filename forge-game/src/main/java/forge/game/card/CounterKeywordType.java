package forge.game.card;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import forge.game.keyword.Keyword;

public record CounterKeywordType(String keyword, String desc) implements CounterType {

    // Rule 122.1b
    static ImmutableList<String> keywordCounter = ImmutableList.of(
            "Flying", "First Strike", "Double Strike", "Deathtouch", "Decayed", "Exalted", "Haste", "Hexproof",
            "Indestructible", "Lifelink", "Menace", "Reach", "Shadow", "Trample", "Vigilance");
    private static Map<String, CounterKeywordType> sMap = Maps.newHashMap();

    public static CounterKeywordType get(String s) {
        if (!sMap.containsKey(s)) {
            sMap.put(s, new CounterKeywordType(s, isKeywordCounter(s) ? Keyword.getInstance(s).getTitle() : null));
        }
        return sMap.get(s);
    }

    public static Set<CounterType> getValues() {
        // add fixed first
        Set<CounterType> result = keywordCounter.stream().map(CounterKeywordType::get).collect(Collectors.toCollection(LinkedHashSet::new));
        // add variable ones later
        result.addAll(sMap.values());
        return result;
    }
    
    @Override
    public String toString() {
        return keyword;
    }

    public String getName() {
        return getKeywordDescription();
    }

    public String getCounterOnCardDisplayName() {
        return getKeywordDescription();
    }

    private String getKeywordDescription() {
        return desc != null ? desc : keyword;
    }

    public boolean is(CounterEnumType eType) {
        return false;
    }

    public boolean isKeywordCounter() {
        return isKeywordCounter(keyword);
    }
    public static boolean isKeywordCounter(String keyword) {
        if (keyword.startsWith("Hexproof:")) {
            return true;
        }
        if (keyword.startsWith("Trample:")) {
            return true;
        }
        return keywordCounter.contains(keyword);
    }

    public int getRed() {
        return 255;
    }

    public int getGreen() {
        return 255;
    }

    public int getBlue() {
        return 255;
    }
}
