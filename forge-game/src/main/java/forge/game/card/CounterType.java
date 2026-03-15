package forge.game.card;

import java.io.Serializable;

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

    String getName();

    String getCounterOnCardDisplayName();

    boolean is(CounterEnumType eType);

    boolean isKeywordCounter();

    int getRed();

    int getGreen();

    int getBlue();
}
