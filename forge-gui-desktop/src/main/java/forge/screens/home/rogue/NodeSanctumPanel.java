package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.NodeSanctum;
import java.awt.*;

/**
 * Visual representation of a Sanctum node in the Rogue Commander path. Sanctums allow the player to
 * heal, cook a carry item, or reflect for removal credits. Displays as a small circular node with
 * a heart icon.
 */
public class NodeSanctumPanel extends NodeCircularPanel {

  private final NodeSanctum sanctumNode;

  /**
   * Create a panel for displaying a sanctum node.
   *
   * @param node Node data to display
   */
  public NodeSanctumPanel(NodeSanctum node) {
    super(node);
    this.sanctumNode = node;
    setToolTipText(
        "Sanctum: Gain life and cure all wounds, craft a random Food item, or gain 3 Removal Credits.");
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    // Draw heart icon in the center
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Heart icon parameters
    int centerX = getWidth() / 2;
    int centerY = getHeight() / 2 + 3;
    int heartSize = 35;

    // Draw heart shape
    g2d.setColor(new Color(220, 53, 69)); // Red color for healing
    drawHeart(g2d, centerX, centerY, heartSize);
  }

  /**
   * Draw a heart icon centered at the given position.
   */
  private void drawHeart(Graphics2D g2d, int centerX, int centerY, int size) {
    // Use heart character for clear, recognizable heart symbol
    g2d.setFont(new Font("Serif", Font.PLAIN, size));
    FontMetrics fm = g2d.getFontMetrics();
    String heart = "♥";
    int textX = centerX - fm.stringWidth(heart) / 2;
    int textY = centerY + fm.getAscent() / 2 - fm.getDescent();
    g2d.drawString(heart, textX, textY);
  }
}
