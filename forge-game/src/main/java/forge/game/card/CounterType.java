package forge.game.card;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.collect.Lists;

public interface CounterType extends Serializable {

    static CounterType getType(String name) {
        if ("Any".equalsIgnoreCase(name)) {
            return null;
        }
        try {
            return CounterEnumType.getType(name);
        } catch (final IllegalArgumentException ex) {
            return CounterKeywordType.get(name);
        }
    }
    static Collection<CounterType> getValues() {
        List<CounterType> result = Lists.newArrayList();
        result.addAll(CounterEnumType.getValues());
        result.addAll(CounterKeywordType.getValues());
        return result;
    }

    String getName();

    String getCounterOnCardDisplayName();

    boolean is(CounterEnumType eType);

    boolean isKeywordCounter();

    int getRed();

    int getGreen();

    int getBlue();
}
