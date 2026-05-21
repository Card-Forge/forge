package forge.card;

import java.util.Map;

public interface ResetChangedText extends IChangedText {

    @Override
    default void applyColorChanges(Map<MagicColor.Color, MagicColor.Color> result) {
        result.clear();
    }
    @Override
    default void applyTypeChanges(Map<String, String> result) {
        result.clear();
    }

}
