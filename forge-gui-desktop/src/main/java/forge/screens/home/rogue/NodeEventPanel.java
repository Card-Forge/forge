package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.NodeEvent;
import java.awt.*;

/**
 * Visual representation of an Event node in the Rogue Commander path. Events trigger random
 * occurrences or choices for the player. Displays as a small circular node with a scroll/event
 * icon.
 */
public class NodeEventPanel extends NodeCircularPanel {

  private final NodeEvent eventNode;

  /**
   * Create a panel for displaying an event node.
   *
   * @param node Node data to display
   */
  public NodeEventPanel(NodeEvent node) {
    super(node);
    this.eventNode = node;
    setToolTipText("Event: A random encounter.");
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    // Draw scroll icon in the center
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Scroll icon parameters
    int centerX = getWidth() / 2;
    int centerY = getHeight() / 2;
    int scrollSize = 32;

    // Draw scroll shape
    drawScroll(g2d, centerX, centerY, scrollSize);
  }

  /**
   * Draw a scroll/parchment icon centered at the given position.
   */
  private void drawScroll(Graphics2D g2d, int centerX, int centerY, int size) {
    // Main parchment (beige/tan rectangle)
    g2d.setColor(new Color(245, 222, 179));
    int scrollWidth = size;
    int scrollHeight = (int) (size * 0.8);
    g2d.fillRoundRect(centerX - scrollWidth / 2, centerY - scrollHeight / 2, scrollWidth,
        scrollHeight, 5, 5);

    // Top and bottom rolls (darker tan circles)
    g2d.setColor(new Color(210, 180, 140));
    g2d.fillRect(centerX - scrollWidth / 2 - 2, centerY - scrollHeight / 2 - 5, scrollWidth + 4, 8);
    g2d.fillRect(centerX - scrollWidth / 2 - 2, centerY + scrollHeight / 2 - 3, scrollWidth + 4, 8);

    // Question mark symbol (mysterious event)
    g2d.setColor(new Color(139, 69, 19));
    g2d.setFont(new Font("Arial", Font.BOLD, (int) (size * 0.7)));
    FontMetrics fm = g2d.getFontMetrics();
    String symbol = "?";
    int textX = centerX - fm.stringWidth(symbol) / 2;
    int textY = centerY + fm.getAscent() / 2 - 2;
    g2d.drawString(symbol, textX, textY);

    // Border for parchment
    g2d.setColor(new Color(210, 180, 140));
    g2d.setStroke(new BasicStroke(2));
    g2d.drawRoundRect(centerX - scrollWidth / 2, centerY - scrollHeight / 2, scrollWidth,
        scrollHeight, 5, 5);
  }
}
