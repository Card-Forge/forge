package forge.screens.home.rogue;

import forge.StaticData;
import forge.gamemodes.rogue.NodeData;
import forge.gui.CardPicturePanel;
import forge.item.PaperCard;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Visual representation of a single node in the Rogue Commander path.
 * Displays the plane card image, planebound name, and life total.
 */
public class PathNodePanel extends SkinnedPanel {
    private static final int CARD_WIDTH = 180;
    private static final int CARD_HEIGHT = 250;
    private static final int PANEL_WIDTH = CARD_WIDTH + 20;
    private static final int PANEL_HEIGHT = CARD_HEIGHT + 80;

    private final NodeData node;
    private final boolean isCurrentNode;
    private final boolean isCompleted;
    private final CardPicturePanel cardImage;
    private final JLabel lblPlaneboundName;
    private final JLabel lblLifeTotal;

    /**
     * Create a panel for displaying a path node.
     *
     * @param node Node data to display
     * @param isCurrentNode Whether this is the player's current position
     */
    public PathNodePanel(NodeData node, boolean isCurrentNode) {
        this.node = node;
        this.isCurrentNode = isCurrentNode;
        this.isCompleted = node.isCompleted();

        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMinimumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMaximumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        // Card image (plane card)
        cardImage = new CardPicturePanel();
        cardImage.setOpaque(false);
        PaperCard planeCard = getPlaneCard(node.getPlaneName());
        if (planeCard != null) {
            cardImage.setItem(planeCard);
        }
        add(cardImage);

        // Planebound name label
        lblPlaneboundName = new JLabel(node.getPlaneboundName());
        lblPlaneboundName.setFont(FSkin.getRelativeFont(12).getBaseFont());
        lblPlaneboundName.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblPlaneboundName.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblPlaneboundName);

        // Life total label
        int planeboundLife = 5 + (5 * node.getRowIndex());
        lblLifeTotal = new JLabel("Life: " + planeboundLife);
        lblLifeTotal.setFont(FSkin.getRelativeBoldFont(14).getBaseFont());
        lblLifeTotal.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblLifeTotal.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblLifeTotal);
    }

    @Override
    public void doLayout() {
        int x = 10;
        int y = 10;

        // Card image
        cardImage.setBounds(x, y, CARD_WIDTH, CARD_HEIGHT);
        y += CARD_HEIGHT + 5;

        // Planebound name
        lblPlaneboundName.setBounds(x, y, CARD_WIDTH, 20);
        y += 25;

        // Life total
        lblLifeTotal.setBounds(x, y, CARD_WIDTH, 20);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw border based on node status
        Color borderColor;
        int borderWidth;

        if (isCurrentNode) {
            // Current node: thick gold border
            borderColor = new Color(255, 215, 0);
            borderWidth = 4;
        } else if (isCompleted) {
            // Completed node: thin green border
            borderColor = new Color(0, 200, 0);
            borderWidth = 2;
        } else {
            // Uncompleted node: thin gray border
            borderColor = new Color(100, 100, 100);
            borderWidth = 2;
        }

        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.drawRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 10, 10);

        // Draw checkmark for completed nodes
        if (isCompleted && !isCurrentNode) {
            g2d.setColor(new Color(0, 200, 0, 230));
            g2d.fillOval(getWidth() - 35, 10, 25, 25);

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            int checkX = getWidth() - 32;
            int checkY = 13;
            int[] xPoints = {checkX + 5, checkX + 9, checkX + 18};
            int[] yPoints = {checkY + 11, checkY + 15, checkY + 7};
            g2d.drawPolyline(xPoints, yPoints, 3);
        }
    }

    /**
     * Get the plane card by name from the database.
     */
    private PaperCard getPlaneCard(String planeName) {
        try {
            return StaticData.instance().getCommonCards().getCard(planeName);
        } catch (Exception e) {
            System.err.println("Warning: Could not find plane card: " + planeName);
            return null;
        }
    }

    public NodeData getNode() {
        return node;
    }
}
