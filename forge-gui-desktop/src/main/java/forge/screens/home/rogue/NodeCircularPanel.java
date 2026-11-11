package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.RoguePathNode;
import java.awt.*;

/**
 * Base class for small circular node panels (Sanctum, Bazaar, Chest, Event). Provides smaller
 * dimensions and circular rendering.
 */
public abstract class NodeCircularPanel extends NodePanel {

  // Smaller dimensions for non-plane nodes
  protected static final int CIRCULAR_SIZE = 75;

  protected NodeCircularPanel(RoguePathNode node) {
    super(node);

    // Override size from parent
    setPreferredSize(new Dimension(CIRCULAR_SIZE, CIRCULAR_SIZE));
    setMinimumSize(new Dimension(CIRCULAR_SIZE, CIRCULAR_SIZE));
    setMaximumSize(new Dimension(CIRCULAR_SIZE, CIRCULAR_SIZE));
  }

  @Override
  protected void paintNodeBorder(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Draw circular border based on node status
    Color borderColor;
    int borderWidth;

    if (isHovered && !isSelected) {
      // Hovered node (not selected): yellow/gold border to show interactivity
      borderColor = new Color(255, 215, 0);
      borderWidth = 3;
    } else if (isSelected) {
      // Selected node: thick bright gold border
      borderColor = new Color(255, 215, 0);
      borderWidth = 4;
    } else if (isCompleted) {
      // Completed node: thin green border
      borderColor = new Color(0, 200, 0);
      borderWidth = 2;
    } else {
      // Uncompleted node: thin gray border
      borderColor = new Color(100, 100, 100);
      borderWidth = 2;
    }

    // Draw circular border
    g2d.setColor(borderColor);
    g2d.setStroke(new BasicStroke(borderWidth));
    int margin = 5;
    g2d.drawOval(margin, margin, getWidth() - (margin * 2), getHeight() - (margin * 2));

    // Draw checkmark for completed nodes
    if (isCompleted) {
      g2d.setColor(new Color(0, 200, 0, 230));
      g2d.fillOval(getWidth() - 35, 10, 25, 25);

      g2d.setColor(Color.WHITE);
      g2d.setStroke(new BasicStroke(2));
      int checkX = getWidth() - 32;
      int checkY = 13;
      int[] xPoints = {checkX + 5, checkX + 9, checkX + 18};
      int[] yPoints = {checkY + 11, checkY + 15, checkY + 7};
      g2d.drawPolyline(xPoints, yPoints, 3);
    }
  }
}
