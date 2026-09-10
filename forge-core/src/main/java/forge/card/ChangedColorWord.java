package forge.card;

import java.util.Map;

public record ChangedColorWord(MagicColor.Color oldColor, MagicColor.Color newColor) implements IChangedText {

    @Override
    public void applyColorChanges(Map<MagicColor.Color, MagicColor.Color> result) {
        if (oldColor == null) {
            for (MagicColor.Color old : ColorSet.WUBRG) {
                result.put(old, newColor);
            }
        } else {
            for (Map.Entry<MagicColor.Color, MagicColor.Color> e : result.entrySet()) {
                if (e.getValue().equals(oldColor)) {
                    e.setValue(newColor);
                }
            }
            result.put(oldColor, newColor);
        }
        
    }
}
