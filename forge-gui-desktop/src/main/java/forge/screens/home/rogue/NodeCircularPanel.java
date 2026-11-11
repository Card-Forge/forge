package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.RoguePathNode;
import java.awt.Dimension;
import java.awt.Graphics2D;

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
  protected void drawNodeBorder(Graphics2D g2d) {
    int margin = 5;
    g2d.drawOval(margin, margin, getWidth() - (margin * 2), getHeight() - (margin * 2));
  }
}
