package forge.card;

import java.util.Map;

public record ResetChangedText() implements IChangedText {

    @Override
    public void applyColorChanges(Map<MagicColor.Color, MagicColor.Color> result) {
        result.clear();
    }
    @Override
    public void applyTypeChanges(Map<String, String> result) {
        result.clear();
    }

}
