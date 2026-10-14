package forge.card;

import java.util.Map;

public interface IChangedText {
    default void applyColorChanges(Map<MagicColor.Color, MagicColor.Color> result) { }
    default void applyTypeChanges(Map<String, String> result) { }
}
