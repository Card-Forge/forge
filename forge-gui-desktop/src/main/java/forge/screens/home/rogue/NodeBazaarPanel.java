package forge.screens.home.rogue;

import forge.gamemodes.rogue.NodeBazaar;
import forge.toolbox.FSkin;
import java.awt.Graphics;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Visual representation of a Bazaar node in the Rogue Commander path.
 * Bazaars allow the player to buy cards and items.
 * TODO: Add proper UI with shop icon, coin display, and item preview.
 */
public class NodeBazaarPanel extends NodePanel {
    private final NodeBazaar bazaarNode;
    private final JLabel lblTitle;
    private final JLabel lblSubtitle;

    /**
     * Create a panel for displaying a bazaar node.
     *
     * @param node Node data to display
     * @param isCurrentNode Whether this is the player's current position
     */
    public NodeBazaarPanel(NodeBazaar node, boolean isCurrentNode) {
        super(node, isCurrentNode);
        this.bazaarNode = node;

        // Title label
        lblTitle = new JLabel("🏪 Bazaar");
        lblTitle.setFont(FSkin.getRelativeBoldFont(16).getBaseFont());
        lblTitle.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblTitle);

        // Subtitle label
        lblSubtitle = new JLabel("(Shop)");
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

        // TODO: Could add bazaar theme visual elements here (coins, merchant tent, etc.)
    }
}
