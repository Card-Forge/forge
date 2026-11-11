package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.RoguePathNode;
import forge.toolbox.FSkin.SkinnedPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Abstract base class for all node panel visualizations. Each node type (Planebound, Sanctum,
 * Bazaar, etc.) has its own concrete panel class.
 */
public abstract class NodePanel extends SkinnedPanel {

  protected static final int PANEL_WIDTH = 270;
  protected static final int PANEL_HEIGHT = 260;

  protected final RoguePathNode node;
  protected final boolean isCompleted;
  protected boolean isSelected = false;
  protected boolean isHovered = false;
  protected NodeClickHandler clickHandler;

  /**
   * Create a panel for displaying a path node.
   *
   * @param node Node data to display
   */
  protected NodePanel(RoguePathNode node) {
    this.node = node;
    this.isCompleted = node.isCompleted();

    setLayout(null);
    setOpaque(false);
    setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
    setMinimumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
    setMaximumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

    // Add mouse listener for click and hover handling
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (clickHandler != null) {
          clickHandler.onNodeClicked(NodePanel.this);
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        isHovered = true;
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        isHovered = false;
        setCursor(java.awt.Cursor.getDefaultCursor());
        repaint();
      }
    });
  }

  /**
   * Set the click handler for this panel.
   */
  public void setClickHandler(NodeClickHandler handler) {
    this.clickHandler = handler;
  }

  /**
   * Set whether this node is selected.
   */
  public void setSelected(boolean selected) {
    this.isSelected = selected;
    repaint();
  }

  /**
   * Check if this node is selected.
   */
  public boolean isSelected() {
    return isSelected;
  }

  @Override
  public void paint(Graphics g) {
    // First paint everything (component + children)
    super.paint(g);

    // Then paint border and checkmark ON TOP of children
    paintNodeBorder(g);
  }

  /**
   * Paint the border and status indicators for this node. Subclasses can override to customize
   * border appearance.
   */
  protected void paintNodeBorder(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Draw border based on node status
    Color borderColor;
    int borderWidth;

    if (isHovered && !isSelected) {
      // Hovered node (not selected): yellow/gold border to show interactivity
      borderColor = new Color(255, 215, 0);
      borderWidth = 3;
    } else if (isSelected) {
      // Selected node: thick bright gold border (used for all node types)
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

    g2d.setColor(borderColor);
    g2d.setStroke(new BasicStroke(borderWidth));
    g2d.drawRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 10, 10);

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

  public RoguePathNode getNode() {
    return node;
  }

  /**
   * Interface for handling node click events.
   */
  public interface NodeClickHandler {

    void onNodeClicked(NodePanel panel);
  }
}
