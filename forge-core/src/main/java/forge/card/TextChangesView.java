package forge.card;

import java.util.Map;

public record TextChangesView(Map<MagicColor.Color, MagicColor.Color> colorChanges, Map<String, String> typeChanges) implements ITextChanges {

    @Override
    public boolean isEmpty() {
        return colorChanges.isEmpty() && typeChanges.isEmpty();
    }
}
