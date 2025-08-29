package forge.game.card;

import java.io.Serializable;

public interface CounterType extends Serializable {

    public static CounterType getType(String name) {
        if ("Any".equalsIgnoreCase(name)) {
            return null;
        }
        try {
            return CounterEnumType.getType(name);
        } catch (final IllegalArgumentException ex) {
            return CounterKeywordType.get(name);
        }
    }

    public String getName();

    public String getCounterOnCardDisplayName();

    public boolean is(CounterEnumType eType);

    public boolean isKeywordCounter();

    public int getRed();

    public int getGreen();

    public int getBlue();
}
