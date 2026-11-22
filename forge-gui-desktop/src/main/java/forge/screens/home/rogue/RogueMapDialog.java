package forge.screens.home.rogue;

import forge.gamemodes.rogue.RogueRun;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;
import java.awt.Dimension;
import java.util.List;

/** Displays the current Rogue Commander path without allowing path interaction. */
final class RogueMapDialog {

  private RogueMapDialog() {
  }

  static void show(RogueRun run) {
    if (run == null) {
      return;
    }

    RogueMapPanel mapPanel = new RogueMapPanel(null);
    mapPanel.updatePath(run);

    Dimension dialogSize = getDialogSize();
    mapPanel.setPreferredSize(dialogSize);
    mapPanel.setMinimumSize(dialogSize);

    FOptionPane optionPane = new FOptionPane(
        null,
        Localizer.getInstance().getMessage("lblRogueViewMap"),
        null,
        mapPanel,
        List.of(Localizer.getInstance().getMessage("lblBack")),
        0);
    optionPane.setVisible(true);
    optionPane.dispose();
  }

  private static Dimension getDialogSize() {
    Dimension screenSize = RogueUIHelper.getUsableDefaultScreenSize();
    return new Dimension((int) (screenSize.width * 0.9), (int) (screenSize.height * 0.9) - 80);
  }
}
