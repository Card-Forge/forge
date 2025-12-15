package forge.screens.home.rogue;

import forge.deck.CardPool;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gamemodes.rogue.NodeData;
import forge.gui.CardPicturePanel;
import forge.item.PaperCard;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Visual representation of a single node in the Rogue Commander path.
 * Displays the plane card image, planebound name, and life total.
 */
public class PathNodePanel extends SkinnedPanel {
    // Plane cards are horizontal, so width > height (rotated 90 degrees from normal cards)
    private static final int CARD_WIDTH = 250;  // Wider (was height)
    private static final int CARD_HEIGHT = 180; // Shorter (was width)
    private static final int PANEL_WIDTH = CARD_WIDTH + 20;
    private static final int PANEL_HEIGHT = CARD_HEIGHT + 80;

    private final NodeData node;
    private final boolean isCurrentNode;
    private final boolean isCompleted;
    private final CardPicturePanel cardImage;
    private final JLabel lblPlaneboundName;
    private final JLabel lblLifeTotal;

    // Zoom overlay
    private JPanel zoomOverlay;
    private PaperCard currentPlaneCard;
    private BufferedImage cachedRotatedImage; // Cache rotated image to avoid recreating

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

        // Card image (plane card) - rotated 90 degrees clockwise for horizontal display
        cardImage = new CardPicturePanel();
        cardImage.setOpaque(false);
        cardImage.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        PaperCard planeCard = getPlaneCard(node.getPlaneBoundConfig().planeName());
        if (planeCard != null) {
            // Get the card image and rotate it 90 degrees clockwise
            BufferedImage originalImage = getPlaneCardImage(planeCard);
            if (originalImage != null) {
                BufferedImage rotatedImage = rotateImage90Clockwise(originalImage);
                cardImage.setItem(rotatedImage);
            } else {
                // Fallback to original if image not available
                cardImage.setItem(planeCard);
            }

            cardImage.revalidate();
            cardImage.repaint();
        }
        add(cardImage);

        // Store the plane card for zoom functionality
        currentPlaneCard = planeCard;

        // Add mouse wheel listener for zoom
        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0 && currentPlaneCard != null) { // Scroll up to zoom
                showZoom();
            }
        });

        // Planebound name label
        lblPlaneboundName = new JLabel(node.getPlaneBoundConfig().planeboundName());
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
    public void paint(Graphics g) {
        // First paint everything (component + children)
        super.paint(g);

        // Then paint border and checkmark ON TOP of children
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
     * Get the BufferedImage for a plane card.
     * Try to get high-quality artwork first, fall back to regular image if not available.
     */
    private BufferedImage getPlaneCardImage(PaperCard planeCard) {
        try {
            Card gameCard = Card.getCardForUi(planeCard);
            if (gameCard != null) {
                CardView cardView = CardView.get(gameCard);

                // Try high-quality image first (actual artwork)
                BufferedImage image = FImageUtil.getImageXlhq(cardView.getCurrentState());
                if (image != null) {
                    return image;
                }

                // Fall back to regular image
                image = FImageUtil.getImage(cardView.getCurrentState());
                return image;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not get image for plane card: " + planeCard.getName());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Rotate a BufferedImage 90 degrees clockwise.
     * Plane cards are horizontal, so we need to rotate them from their default vertical display.
     */
    private BufferedImage rotateImage90Clockwise(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Create new image with swapped dimensions (width becomes height, height becomes width)
        BufferedImage rotated = new BufferedImage(height, width, image.getType());

        // Graphics2D to draw the rotated image
        Graphics2D g2d = rotated.createGraphics();

        // Rotate 90 degrees clockwise around the center
        AffineTransform transform = new AffineTransform();
        transform.translate(height / 2.0, width / 2.0);
        transform.rotate(Math.toRadians(90));
        transform.translate(-width / 2.0, -height / 2.0);

        // Draw the original image onto the rotated canvas
        g2d.drawImage(image, transform, null);
        g2d.dispose();

        return rotated;
    }

    /**
     * Get the plane card by name from the variant cards collection.
     * Uses the centralized RogueConfig.getAllPlanes() method.
     */
    private static PaperCard getPlaneCard(String planeName) {
        try {
            // Get all plane cards from the centralized cache
            CardPool allPlanes = forge.gamemodes.rogue.RogueConfig.getAllPlanes();

            // Find the plane card by name
            for (PaperCard card : allPlanes.toFlatList()) {
                if (card.getName().equalsIgnoreCase(planeName)) {
                    return card;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Warning: Error loading plane card: " + planeName + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public NodeData getNode() {
        return node;
    }

    /**
     * Show zoomed plane card overlay.
     */
    private void showZoom() {
        if (currentPlaneCard == null) {
            return;
        }

        // Find the root frame to attach the overlay
        Window window = SwingUtilities.getWindowAncestor(this);
        if (!(window instanceof JFrame)) {
            return;
        }

        JFrame frame = (JFrame) window;

        // Create zoom overlay if it doesn't exist
        if (zoomOverlay == null) {
            zoomOverlay = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    // Semi-transparent dark background
                    g.setColor(new Color(0, 0, 0, 200));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            zoomOverlay.setLayout(new MigLayout("insets 0, wrap, ax center, ay center"));
            zoomOverlay.setOpaque(false);
            zoomOverlay.setVisible(false);

            // Add click listener to close zoom
            zoomOverlay.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    closeZoom();
                }
            });

            // Add mouse wheel listener to close zoom on scroll down
            zoomOverlay.addMouseWheelListener(e -> {
                if (e.getWheelRotation() > 0) { // Scroll down
                    closeZoom();
                }
            });

            // Add key listener for ESC to close
            zoomOverlay.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        closeZoom();
                    }
                }
            });
            zoomOverlay.setFocusable(true);
        }

        // Always set glass pane (multiple PathNodePanels share the same frame)
        // Each panel needs to set its own overlay as the active glass pane when zooming
        frame.setGlassPane(zoomOverlay);

        // Clear previous content
        zoomOverlay.removeAll();

        // Use cached rotated image if available, otherwise create and cache it
        if (cachedRotatedImage == null) {
            BufferedImage originalImage = getPlaneCardImage(currentPlaneCard);
            if (originalImage != null) {
                cachedRotatedImage = rotateImage90Clockwise(originalImage);
            }
        }

        if (cachedRotatedImage != null) {
            // Create image panel for display at 80% of overlay size
            FImagePanel imagePanel = new FImagePanel();
            imagePanel.setImage(cachedRotatedImage, 0, AutoSizeImageMode.SOURCE);

            // Add to overlay with size constraints (80% of overlay size)
            zoomOverlay.add(imagePanel, "w 80%!, h 80%!");
        }

        zoomOverlay.setVisible(true);
        zoomOverlay.requestFocusInWindow();
        zoomOverlay.revalidate();
        zoomOverlay.repaint();
    }

    /**
     * Close the zoom overlay.
     */
    private void closeZoom() {
        if (zoomOverlay != null) {
            zoomOverlay.setVisible(false);
            zoomOverlay.removeAll();
        }
    }
}
