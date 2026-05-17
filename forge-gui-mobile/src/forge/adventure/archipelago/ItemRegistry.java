package forge.adventure.archipelago;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ItemRegistry {
    private static final Map<Long, String> DEFAULT_ITEMS = ImmutableMap.<Long, String>builder()
            .put(1L, "White rune")
            .put(2L, "Blue rune")
            .put(3L, "Black rune")
            .put(4L, "Red rune")
            .put(5L, "Green rune")
            .put(6L, "Mana crystals")
            .put(7L, "Gold")
            .put(8L, "Gold Challenge Coin")
            .put(9L, "Silver Challenge Coin")
            .put(10L, "Bronze Challenge Coin")
            .put(11L, "Set Unlock")
            .build();

    public static String getItem(Long input) {
        return DEFAULT_ITEMS.get(input);
    }
}
