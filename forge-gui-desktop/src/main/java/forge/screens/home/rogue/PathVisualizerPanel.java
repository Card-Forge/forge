package forge.screens.home.rogue;

import forge.gamemodes.rogue.RoguePathNode;
import forge.gamemodes.rogue.RoguePath;
import forge.gamemodes.rogue.RogueRun;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the entire path for a Rogue Commander run.
 * Shows all nodes in a vertical linear progression with connection lines.
 */
public class PathVisualizerPanel extends SkinnedPanel {
    private static final int NODE_SPACING = 40;  // Vertical space between nodes
    private static final int PATH_LINE_WIDTH = 4;

    private List<NodePanel> nodePanels;
    private int currentNodeIndex;

    public PathVisualizerPanel() {
        setLayout(null);
        setOpaque(false);
        this.nodePanels = new ArrayList<>();
        this.currentNodeIndex = -1;
    }

    /**
     * Update the display with a new run's path.
     *
     * @param run The current run data
     */
    public void updatePath(RogueRun run) {
        if (run == null) {
            clearPath();
            return;
        }

        RoguePath path = run.getPath();
        if (path == null || path.getNodes().isEmpty()) {
            clearPath();
            return;
        }

        // Clear existing panels
        removeAll();
        nodePanels.clear();

        // Find current node index
        RoguePathNode currentNode = run.getCurrentNode();
        currentNodeIndex = path.getNodes().indexOf(currentNode);

        // Create panels for each node
        List<RoguePathNode> nodes = path.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            RoguePathNode node = nodes.get(i);
            boolean isCurrent = (i == currentNodeIndex);

            NodePanel nodePanel = NodePanelFactory.createPanel(node, isCurrent);
            nodePanels.add(nodePanel);
            add(nodePanel);
        }

        calculatePreferredSize();
        revalidate();
        repaint();
    }

    /**
     * Clear all nodes from the display.
     */
    public void clearPath() {
        removeAll();
        nodePanels.clear();
        currentNodeIndex = -1;
        revalidate();
        repaint();
    }

    /**
     * Calculate the preferred size based on number of nodes.
     */
    private void calculatePreferredSize() {
        if (nodePanels.isEmpty()) {
            setPreferredSize(new Dimension(0, 0));
            return;
        }

        NodePanel firstPanel = nodePanels.get(0);
        int panelWidth = firstPanel.getPreferredSize().width;
        int panelHeight = firstPanel.getPreferredSize().height;

        int totalHeight = (nodePanels.size() * panelHeight) +
                ((nodePanels.size() - 1) * NODE_SPACING) + 40; // Extra padding

        setPreferredSize(new Dimension(panelWidth + 40, totalHeight));
    }

    @Override
    public void doLayout() {
        if (nodePanels.isEmpty()) {
            return;
        }

        // Center nodes horizontally, stack vertically with spacing
        int panelWidth = nodePanels.get(0).getPreferredSize().width;
        int panelHeight = nodePanels.get(0).getPreferredSize().height;

        int startX = (getWidth() - panelWidth) / 2;
        int y = 20;

        for (NodePanel nodePanel : nodePanels) {
            nodePanel.setBounds(startX, y, panelWidth, panelHeight);
            y += panelHeight + NODE_SPACING;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (nodePanels.size() < 2) {
            return; // No lines to draw if less than 2 nodes
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw connection lines between nodes
        g2d.setColor(FSkin.getColor(FSkin.Colors.CLR_BORDERS).getColor());
        g2d.setStroke(new BasicStroke(PATH_LINE_WIDTH));

        for (int i = 0; i < nodePanels.size() - 1; i++) {
            NodePanel currentPanel = nodePanels.get(i);
            NodePanel nextPanel = nodePanels.get(i + 1);

            // Calculate line positions (center bottom of current to center top of next)
            int x1 = currentPanel.getX() + (currentPanel.getWidth() / 2);
            int y1 = currentPanel.getY() + currentPanel.getHeight();
            int x2 = nextPanel.getX() + (nextPanel.getWidth() / 2);
            int y2 = nextPanel.getY();

            // Draw the connecting line
            g2d.drawLine(x1, y1, x2, y2);

            // Draw arrow at the end
            drawArrow(g2d, x1, y1, x2, y2);
        }
    }

    /**
     * Draw an arrow pointing from (x1, y1) to (x2, y2).
     */
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        int arrowSize = 10;

        // Calculate angle
        double angle = Math.atan2(y2 - y1, x2 - x1);

        // Arrow head points
        int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

        // Draw arrow head
        g2d.drawLine(x2, y2, x3, y3);
        g2d.drawLine(x2, y2, x4, y4);
    }

    /**
     * Get all node panels.
     */
    public List<NodePanel> getNodePanels() {
        return nodePanels;
    }

    /**
     * Get the current node index.
     */
    public int getCurrentNodeIndex() {
        return currentNodeIndex;
    }
}
