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
    return switch (node.getType()) {
      case PLANEBOUND -> new NodePlaneboundPanel((NodePlanebound) node, isFaceDown,
          planeboundRowCount, animateReveal);
      case SANCTUM -> new NodeSanctumPanel((NodeSanctum) node);
      case BAZAAR -> new NodeBazaarPanel((NodeBazaar) node);
      case CHEST -> new NodeChestPanel((NodeChest) node);
      case EVENT -> new NodeEventPanel((NodeEvent) node);
    };
  }
}
