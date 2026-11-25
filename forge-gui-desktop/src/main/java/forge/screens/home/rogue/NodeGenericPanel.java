package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.RoguePathNode;
import forge.toolbox.FSkin;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Generic fallback panel for unknown or unimplemented node types. Displays the node's toString()
 * representation.
 */
public class NodeGenericPanel extends NodePanel {

  private final JLabel lblTitle;

  /**
   * Create a generic panel for displaying any node type.
   *
   * @param node Node data to display
   */
  public NodeGenericPanel(RoguePathNode node) {
    super(node);

    // Display node's toString() representation
    lblTitle = new JLabel(node.toString());
    lblTitle.setFont(FSkin.getRelativeBoldFont(14).getBaseFont());
    lblTitle.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
    lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
    add(lblTitle);
  }

  @Override
  public void doLayout() {
    int x = 10;
    int y = (PANEL_HEIGHT / 2) - 15;

    lblTitle.setBounds(x, y, PANEL_WIDTH - 20, 30);
  }
}
