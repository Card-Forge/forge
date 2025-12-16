package forge.screens.home.rogue;

import forge.gamemodes.rogue.NodeEvent;
import forge.toolbox.FSkin;

import javax.swing.*;
import java.awt.*;

/**
 * Visual representation of an Event node in the Rogue Commander path.
 * Events trigger random occurrences or choices for the player.
 * TODO: Add proper UI with event icon and choice preview.
 */
public class NodeEventPanel extends NodePanel {
    private final NodeEvent eventNode;
    private final JLabel lblTitle;
    private final JLabel lblSubtitle;

    /**
     * Create a panel for displaying an event node.
     *
     * @param node Node data to display
     * @param isCurrentNode Whether this is the player's current position
     */
    public NodeEventPanel(NodeEvent node, boolean isCurrentNode) {
        super(node, isCurrentNode);
        this.eventNode = node;

        // Title label
        lblTitle = new JLabel("❓ Event");
        lblTitle.setFont(FSkin.getRelativeBoldFont(16).getBaseFont());
        lblTitle.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblTitle);

        // Subtitle label
        lblSubtitle = new JLabel("(???)");
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

        // TODO: Could add mystery theme visual elements here (question marks, random symbols, etc.)
    }
}
