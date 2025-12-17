package forge.screens.home.rogue;

import forge.gamemodes.rogue.NodeChest;
import forge.toolbox.FSkin;
import java.awt.Graphics;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Visual representation of a Chest node in the Rogue Commander path.
 * Chests provide rewards without combat.
 * TODO: Add proper UI with chest icon and reward preview.
 */
public class NodeChestPanel extends NodePanel {
    private final NodeChest chestNode;
    private final JLabel lblTitle;
    private final JLabel lblSubtitle;

    /**
     * Create a panel for displaying a chest node.
     *
     * @param node Node data to display
     * @param isCurrentNode Whether this is the player's current position
     */
    public NodeChestPanel(NodeChest node, boolean isCurrentNode) {
        super(node, isCurrentNode);
        this.chestNode = node;

        // Title label
        lblTitle = new JLabel("📦 Treasure");
        lblTitle.setFont(FSkin.getRelativeBoldFont(16).getBaseFont());
        lblTitle.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblTitle);

        // Subtitle label
        lblSubtitle = new JLabel("(Free Loot)");
        lblSubtitle.setFont(FSkin.getRelativeFont(12).getBaseFont());
        lblSubtitle.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblSubtitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblSubtitle);
    }

    @Override
    public void doLayout() {
        int x = 10;
        int y = (PANEL_HEIGHT / 2) - 30;

        lblTitle.setBounds(x, y, PANEL_WIDTH - 20, 30);
        y += 35;

        lblSubtitle.setBounds(x, y, PANEL_WIDTH - 20, 25);
    }

    @Override
    protected void paintNodeBorder(Graphics g) {
        super.paintNodeBorder(g);

        // TODO: Could add treasure theme visual elements here (sparkles, gold border, etc.)
    }
}
