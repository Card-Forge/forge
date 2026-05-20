package forge.util;

import forge.toolbox.FComboBox;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FPrefsBinder;

public class GuiPrefBinders {
  public static final class ComboBox extends FPrefsBinder<FComboBox<String>, String> {
    public ComboBox(ForgePreferences.FPref key, FComboBox<String> box) {
      super(
        key,
        box,
        b -> (String) b.getSelectedItem(),
        (b, s) -> b.setSelectedItem(s),
        s -> s,
        s -> s);

      box.setChangedHandler(e -> {
          this.save();
      });
    }
  }
}
