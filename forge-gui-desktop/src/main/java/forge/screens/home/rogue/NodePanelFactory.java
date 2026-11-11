package forge.screens.home.rogue;

import forge.gamemodes.rogue.path.*;

/**
 * Factory for creating the appropriate panel for each node type. Centralizes the mapping between
 * node types and their visual representations.
 */
public class NodePanelFactory {

  private NodePanelFactory() {
  }

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
    if (node instanceof NodePlanebound nodePlanebound) {
      return new NodePlaneboundPanel(nodePlanebound, isFaceDown, planeboundRowCount,
          animateReveal);
    } else if (node instanceof NodeSanctum nodeSanctum) {
      return new NodeSanctumPanel(nodeSanctum);
    } else if (node instanceof NodeBazaar nodeBazaar) {
      return new NodeBazaarPanel(nodeBazaar);
    } else if (node instanceof NodeChest nodeChest) {
      return new NodeChestPanel(nodeChest);
    } else if (node instanceof NodeEvent nodeEvent) {
      return new NodeEventPanel(nodeEvent);
    }

    // Fallback: Create a generic panel for unknown node types
    return new NodeGenericPanel(node);
  }
}
