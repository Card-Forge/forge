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
        if (CounterKeywordType.isKeywordCounter(name)) {
            return CounterKeywordType.get(name);
        }
        try {
            return CounterEnumType.getType(name);
        } catch (final IllegalArgumentException ex) {
            return CounterCustomType.get(name);
        }
    }
    static List<CounterType> getValues() {
        List<CounterType> result = Lists.newArrayList();
        result.addAll(List.of(CounterEnumType.values()));
        result.addAll(CounterKeywordType.getValues());
        result.addAll(CounterCustomType.getValues());
        return result;
    }

    String getName();

    default String getCounterOnCardDisplayName() {
        return getName();
    }

    default boolean is(CounterEnumType eType) {
        return false;
    }

    default boolean isKeywordCounter() {
        return false;
    }

    default int getRed() {
        return 255;
    }

    default int getGreen() {
        return 255;
    }

    default int getBlue() {
        return 255;
    }

    @Override
    default String getTranslationKey() {
        return toString();
    }
    @Override
    default String getUntranslatedName() {
        return toString();
    }
}
