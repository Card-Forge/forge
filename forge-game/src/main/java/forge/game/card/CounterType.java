package forge.game.card;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

public class CounterType implements Comparable<CounterType>, Serializable {
    private static final long serialVersionUID = -7575835723159144478L;

    private CounterEnumType eVal = null;
    private String sVal = null;

    // Rule 122.1b
    static ImmutableList<String> keywordCounter = ImmutableList.of(
            "Flying", "First Strike", "Double Strike", "Deathtouch", "Haste", "Hexproof",
            "Indestructible", "Lifelink", "Menace", "Reach", "Trample", "Vigilance");

    private static Map<CounterEnumType, CounterType> eMap = Maps.newEnumMap(CounterEnumType.class);
    private static Map<String, CounterType> sMap = Maps.newHashMap();

    private CounterType(CounterEnumType e, String s) {
        this.eVal = e;
        this.sVal = s;
    }

    public static CounterType get(CounterEnumType e) {
        if (!eMap.containsKey(e)) {
            eMap.put(e, new CounterType(e, null));
        }
        return eMap.get(e);
    }

    public static CounterType get(String s) {
        if (!sMap.containsKey(s)) {
            sMap.put(s, new CounterType(null, s));
        }
        return sMap.get(s);
    }

    public static CounterType getType(String name) {
        if ("Any".equalsIgnoreCase(name)) {
            return null;
        }
        try {
            return get(CounterEnumType.getType(name));
        } catch (final IllegalArgumentException ex) {
            return get(name);
        }
    }


    @Override
    public int hashCode() {
        return Objects.hash(eVal, sVal);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        CounterType rhs = (CounterType) obj;
        return new EqualsBuilder()
                .append(eVal, rhs.eVal)
                .append(sVal, rhs.sVal)
                .isEquals();
    }

    @Override
    public String toString() {
        return eVal != null ? eVal.toString() : sVal;
    }

    public String getName() {
        return eVal != null ? eVal.getName() : getKeywordDescription();
    }

    public String getCounterOnCardDisplayName() {
        return eVal != null ? eVal.getCounterOnCardDisplayName() : getKeywordDescription();
    }

    private String getKeywordDescription() {
        if (sVal.startsWith("Hexproof:")) {
            final String[] k = sVal.split(":");
            return "Hexproof from " + k[2];
        }
        if (sVal.startsWith("Trample:")) {
            return "Trample over Planeswalkers";
        }
        return sVal;
    }

    @Override
    public int compareTo(CounterType o) {
        return ComparisonChain.start()
                .compare(eVal, o.eVal, Ordering.natural().nullsLast())
                .compare(sVal, o.sVal, Ordering.natural().nullsLast())
                .result();
    }

    public boolean is(CounterEnumType eType) {
        return eVal == eType;
    }

    public boolean isKeywordCounter() {
        if (eVal != null) {
            return false;
        }
        if (sVal.startsWith("Hexproof:")) {
            return true;
        }
        if (sVal.startsWith("Trample:")) {
            return true;
        }
        return keywordCounter.contains(sVal);
    }

    public int getRed() {
        return eVal != null ? eVal.getRed() : 255;
    }

    public int getGreen() {
        return eVal != null ? eVal.getGreen() : 255;
    }

    public int getBlue() {
        return eVal != null ? eVal.getBlue() : 255;
    }
}
