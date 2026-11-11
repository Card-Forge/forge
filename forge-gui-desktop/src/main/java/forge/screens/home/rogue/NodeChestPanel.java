package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.NodeChest;
import java.awt.*;

/**
 * Visual representation of a Chest node in the Rogue Commander path. Chests provide rewards without
 * combat. Displays as a small circular node with a chest icon.
 */
public class NodeChestPanel extends NodeCircularPanel {

  private final NodeChest chestNode;

  /**
   * Create a panel for displaying a chest node.
   *
   * @param node Node data to display
   */
  public NodeChestPanel(NodeChest node) {
    super(node);
    this.chestNode = node;
    setToolTipText("Chest: Open for a random reward.");
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    // Draw chest icon in the center
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Chest icon parameters
    int centerX = getWidth() / 2;
    int centerY = getHeight() / 2;
    int chestSize = 32;

    // Draw chest shape
    drawChest(g2d, centerX, centerY, chestSize);
  }

  /**
   * Draw a treasure chest icon centered at the given position.
   */
  private void drawChest(Graphics2D g2d, int centerX, int centerY, int size) {
    // Chest body (brown rectangle)
    g2d.setColor(new Color(139, 90, 43));
    int bodyWidth = size;
    int bodyHeight = (int) (size * 0.7);
    g2d.fillRoundRect(centerX - bodyWidth / 2, centerY - bodyHeight / 2, bodyWidth, bodyHeight, 8,
        8);

    // Lock/latch (gold rectangle in center)
    g2d.setColor(new Color(255, 215, 0));
    int latchWidth = 12;
    int latchHeight = 18;
    g2d.fillRect(centerX - latchWidth / 2, centerY - latchHeight / 2, latchWidth, latchHeight);

    // Keyhole (black circle)
    g2d.setColor(Color.BLACK);
    g2d.fillOval(centerX - 3, centerY - 2, 6, 6);

    // Decorative bands (darker brown stripes)
    g2d.setColor(new Color(80, 50, 20));
    g2d.setStroke(new BasicStroke(2));
    g2d.drawLine(centerX - bodyWidth / 2, centerY, centerX + bodyWidth / 2, centerY);
  }
}
