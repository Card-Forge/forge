package forge.screens.home.rogue;

import forge.gamemodes.rogue.RogueRun;
import forge.toolbox.FOptionPane;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
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
        "View Map",
        null,
        mapPanel,
        List.of("Back"),
        0);
    optionPane.setVisible(true);
    optionPane.dispose();
  }

  private static Dimension getDialogSize() {
    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration();
    Rectangle screenBounds = gc.getBounds();
    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    int usableWidth = screenBounds.width - screenInsets.left - screenInsets.right;
    int usableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;
    return new Dimension((int) (usableWidth * 0.9), (int) (usableHeight * 0.9) - 80);
  }
}
