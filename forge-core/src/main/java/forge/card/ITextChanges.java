package forge.card;

import java.util.Map;

public interface ITextChanges {
    Map<MagicColor.Color, MagicColor.Color> colorChanges();
    Map<String, String> typeChanges();

    boolean isEmpty();

    ITextChanges combine(ITextChanges output);
    default ITextChanges getView() { return this; }
}
