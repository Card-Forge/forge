package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.*;

/**
 * Factory for creating the appropriate panel for each node type. Centralizes the mapping between
 * node types and their visual representations.
 */
public class NodePanelFactory {

  /**
   * Create the appropriate panel for the given node type.
   *
   * @param node               The node to create a panel for
   * @param isFaceDown         Whether this node should be displayed face-down
   * @param planeboundRowCount Number of Planebound rows up to this node (for life calculation)
   * @param animateReveal      Whether to animate the reveal of this node (planebound only)
   * @return A panel instance for displaying the node
   */
  public static NodePanel createPanel(RoguePathNode node, boolean isFaceDown,
      int planeboundRowCount, boolean animateReveal) {
    if (node instanceof NodePlanebound) {
      return new NodePlaneboundPanel((NodePlanebound) node, isFaceDown, planeboundRowCount,
          animateReveal);
    } else if (node instanceof NodeSanctum) {
      return new NodeSanctumPanel((NodeSanctum) node);
    } else if (node instanceof NodeBazaar) {
      return new NodeBazaarPanel((NodeBazaar) node);
    } else if (node instanceof NodeChest) {
      return new NodeChestPanel((NodeChest) node);
    } else if (node instanceof NodeEvent) {
      return new NodeEventPanel((NodeEvent) node);
    }

    // Fallback: Create a generic panel for unknown node types
    return new NodeGenericPanel(node);
  }
}
