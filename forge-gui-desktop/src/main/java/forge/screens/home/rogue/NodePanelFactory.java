package forge.screens.home.rogue;

import forge.gamemodes.rogue.*;

/**
 * Factory for creating the appropriate panel for each node type.
 * Centralizes the mapping between node types and their visual representations.
 */
public class NodePanelFactory {

    /**
     * Create the appropriate panel for the given node type.
     *
     * @param node The node to create a panel for
     * @param isCurrentNode Whether this is the player's current position
     * @return A panel instance for displaying the node
     */
    public static NodePanel createPanel(RoguePathNode node, boolean isCurrentNode) {
        if (node instanceof NodePlanebound) {
            return new NodePlaneboundPanel((NodePlanebound) node, isCurrentNode);
        } else if (node instanceof NodeSanctum) {
            return new NodeSanctumPanel((NodeSanctum) node, isCurrentNode);
        } else if (node instanceof NodeBazaar) {
            return new NodeBazaarPanel((NodeBazaar) node, isCurrentNode);
        } else if (node instanceof NodeChest) {
            return new NodeChestPanel((NodeChest) node, isCurrentNode);
        } else if (node instanceof NodeEvent) {
            return new NodeEventPanel((NodeEvent) node, isCurrentNode);
        }

        // Fallback: Create a generic panel for unknown node types
        return new NodeGenericPanel(node, isCurrentNode);
    }
}
