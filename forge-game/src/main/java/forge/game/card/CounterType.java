package forge.game.card;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.Lists;

import forge.util.ITranslatable;

public interface CounterType extends Serializable, ITranslatable {

    static CounterType getType(String name) {
        if ("Any".equalsIgnoreCase(name)) {
            return null;
        }
        try {
            return CounterEnumType.getType(name);
        } catch (final IllegalArgumentException ex) {
            CounterType result = CounterListType.get(name);
            if (result != null) {
                return result;
            }
            return CounterKeywordType.get(name);
        }
    }
    static List<CounterType> getValues() {
        List<CounterType> result = Lists.newArrayList();
        result.addAll(List.of(CounterEnumType.values()));
        result.addAll(CounterListType.getValues());
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

    @Override
    default String getTranslationKey() {
        return toString();
    }
    @Override
    default String getUntranslatedName() {
        return toString();
    }
}
