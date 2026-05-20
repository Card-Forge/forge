package forge.gui;

import javax.swing.*;
import java.awt.event.ItemEvent;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FPrefsBinder;

public class SwingPrefBinders {
  public static final class ComboBox extends FPrefsBinder<JComboBox<String>, String> {
    public ComboBox(ForgePreferences.FPref key, JComboBox<String> box) {
      super(
        key,
        box,
        b -> (String) b.getSelectedItem(),
        (b, s) -> b.setSelectedItem(s),
        s -> s,
        s -> s);

      box.addItemListener(e -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          this.save();
        }
      });
    }
  }
}
