package forge.screens.home.rogue;

import forge.gamemodes.rogue.NodeSanctum;
import forge.toolbox.FSkin;

import javax.swing.*;
import java.awt.*;

/**
 * Visual representation of a Sanctum node in the Rogue Commander path.
 * Sanctums allow the player to heal and remove cards from their deck.
 * TODO: Add proper UI with heart icon, heal amount, and card removal UI.
 */
public class NodeSanctumPanel extends NodePanel {
    private final NodeSanctum sanctumNode;
    private final JLabel lblTitle;
    private final JLabel lblDetails;

    /**
     * Create a panel for displaying a sanctum node.
     *
     * @param node Node data to display
     * @param isCurrentNode Whether this is the player's current position
     */
    public NodeSanctumPanel(NodeSanctum node, boolean isCurrentNode) {
        super(node, isCurrentNode);
        this.sanctumNode = node;

        // Title label
        lblTitle = new JLabel("⛪ Sanctum");
        lblTitle.setFont(FSkin.getRelativeBoldFont(16).getBaseFont());
        lblTitle.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblTitle);

        // Details label
        String details = String.format("Heal %d | Remove up to %d",
            sanctumNode.getHealAmount(),
            sanctumNode.getFreeRemoves());
        lblDetails = new JLabel(details);
        lblDetails.setFont(FSkin.getRelativeFont(12).getBaseFont());
        lblDetails.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblDetails.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblDetails);
    }

    @Override
    public void doLayout() {
        int x = 10;
        int y = (PANEL_HEIGHT / 2) - 30;

        lblTitle.setBounds(x, y, PANEL_WIDTH - 20, 30);
        y += 35;

        lblDetails.setBounds(x, y, PANEL_WIDTH - 20, 25);
    }

    @Override
    protected void paintNodeBorder(Graphics g) {
        super.paintNodeBorder(g);

        // TODO: Could add healing theme visual elements here (hearts, glow, etc.)
    }
}
