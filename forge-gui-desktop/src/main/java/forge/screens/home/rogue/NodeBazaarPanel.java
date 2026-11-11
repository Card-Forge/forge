package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.NodeBazaar;
import java.awt.*;

/**
 * Visual representation of a Bazaar node in the Rogue Commander path. Bazaars allow the player to
 * buy cards and items. Displays as a small circular node with a coin icon.
 */
public class NodeBazaarPanel extends NodeCircularPanel {

  /**
   * Create a panel for displaying a bazaar node.
   *
   * @param node Node data to display
   */
  public NodeBazaarPanel(NodeBazaar node) {
    super(node);
    setToolTipText("Bazaar: Spend gold to buy cards for your deck.");
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    // Draw coin icon in the center
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Coin icon parameters
    int centerX = getWidth() / 2;
    int centerY = getHeight() / 2;
    int coinSize = 30;

    // Draw coin shape
    g2d.setColor(new Color(255, 215, 0)); // Gold color for coins
    drawCoin(g2d, centerX, centerY, coinSize);
  }

  /**
   * Draw a coin icon centered at the given position.
   */
  private void drawCoin(Graphics2D g2d, int centerX, int centerY, int size) {
    // Draw outer circle (gold coin)
    g2d.setColor(new Color(255, 215, 0));
    g2d.fillOval(centerX - size / 2, centerY - size / 2, size, size);

    // Draw inner circle (darker border)
    g2d.setColor(new Color(218, 165, 32));
    g2d.setStroke(new BasicStroke(3));
    g2d.drawOval(centerX - size / 2 + 5, centerY - size / 2 + 5, size - 10, size - 10);

    // Draw dollar sign or similar marking
    g2d.setColor(new Color(139, 90, 0));
    g2d.setFont(new Font("Arial", Font.BOLD, size / 2));
    FontMetrics fm = g2d.getFontMetrics();
    String symbol = "$";
    int textX = centerX - fm.stringWidth(symbol) / 2;
    int textY = centerY + fm.getAscent() / 2 - 2;
    g2d.drawString(symbol, textX, textY);
  }
}
