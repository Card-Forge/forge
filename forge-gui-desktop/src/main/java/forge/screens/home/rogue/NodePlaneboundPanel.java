package forge.screens.home.rogue;

import forge.ImageCache;
import forge.ImageKeys;
import forge.deck.CardPool;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gamemodes.rogue.NodePlanebound;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.gui.CardPicturePanel;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin;
import forge.toolbox.imaging.FImageUtil;
import forge.util.ImageFetcher;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Visual representation of a Planebound node in the Rogue Commander path.
 * Displays the plane card image.
 */
public class NodePlaneboundPanel extends NodePanel implements ImageFetcher.Callback {
    // Plane cards are horizontal, so width > height (rotated 90 degrees from normal cards)
    private static final int CARD_WIDTH = 250;  // Wider (was height)
    private static final int CARD_HEIGHT = 180; // Shorter (was width)

    private final CardPicturePanel cardImage;
    private final JLabel lblPlaneboundName;
    private final JLabel lblLifeTotal;
    private final PaperCard currentPlaneCard;
    private final boolean isFaceDown;

    // Zoom utility
    private CardZoomUtil zoomUtil; // Lazily initialized on first zoom
    private BufferedImage cachedRotatedImage; // Cache rotated image to avoid recreating

    /**
     * Create a panel for displaying a planebound node.
     *
     * @param node Node data to display
     * @param isCurrentNode Whether this is the player's current position
     * @param isFaceDown Whether to display the card face-down
     */
    public NodePlaneboundPanel(NodePlanebound node, boolean isCurrentNode, boolean isFaceDown) {
        super(node, isCurrentNode);
        this.isFaceDown = isFaceDown;

      // Card image (plane card) - rotated 90 degrees clockwise for horizontal display
        cardImage = new CardPicturePanel();
        cardImage.setOpaque(false);
        cardImage.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));

        String planeName = node.getRoguePlanebound().planeName();
        PaperCard planeCard = getPlaneCard(planeName);

        if (isFaceDown) {
            // Show card back for face-down planes
            BufferedImage cardBack = ImageCache.getOriginalImage(
                ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), true, null);
            if (cardBack != null) {
                BufferedImage rotatedCardBack = rotateImage90Clockwise(cardBack);
                cardImage.setItem(rotatedCardBack);
            }
        } else if (planeCard != null) {
            // Check if we need to fetch the image
            Pair<BufferedImage, Boolean> imageInfo = ImageCache.getCardOriginalImageInfo(
                planeCard.getImageKey(false), true);
            BufferedImage originalImage = imageInfo.getLeft();
            boolean isPlaceholder = imageInfo.getRight();

            // If image is missing or placeholder, trigger download
            if (ImageCache.isDefaultImage(originalImage) || isPlaceholder) {
                System.out.println("Triggering image fetch for: " + planeCard.getName());
                GuiBase.getInterface().getImageFetcher().fetchImage(planeCard.getImageKey(false), this);
            }

            // Display current image (even if placeholder) while real image downloads
            if (originalImage != null) {
                BufferedImage rotatedImage = rotateImage90Clockwise(originalImage);
                cardImage.setItem(rotatedImage);
            } else {
                // Fallback to original if image not available
                System.out.println("Using fallback rendering for: " + planeCard.getName());
                cardImage.setItem(planeCard);
            }

            cardImage.revalidate();
            cardImage.repaint();
        } else {
            System.out.println("ERROR: Plane card not found in database!");
        }

        add(cardImage);

        // Store the plane card for zoom functionality
        currentPlaneCard = planeCard;

        // Add mouse wheel listener for zoom (only for face-up cards)
        addMouseWheelListener(e -> {
            if (!isFaceDown && e.getWheelRotation() < 0 && currentPlaneCard != null) { // Scroll up to zoom
                showZoom();
            }
        });

        // Planebound name label with icon for Elite/Boss
        String planeboundName = isFaceDown ? "???" : node.getRoguePlanebound().planeboundName();
        lblPlaneboundName = new JLabel(planeboundName);
        lblPlaneboundName.setFont(FSkin.getRelativeFont(12).getBaseFont());
        lblPlaneboundName.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lblPlaneboundName.setHorizontalAlignment(SwingConstants.CENTER);

        // Add icon based on planebound type (always shown)
        RoguePlaneboundType type = node.getPlaneboundType();
        if (type == RoguePlaneboundType.ELITE) {
            // Elite gets a filled star icon
            lblPlaneboundName.setIcon(FSkin.getImage(FSkinProp.IMG_STAR_FILLED).resize(16, 16).getIcon());
            lblPlaneboundName.setIconTextGap(5);
        } else if (type == RoguePlaneboundType.BOSS) {
            // Boss gets an attack/sword icon
            lblPlaneboundName.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_BIG_AXE).resize(18, 18).getIcon());
            lblPlaneboundName.setIconTextGap(5);
        }

        add(lblPlaneboundName);

        // Life total label (always shown - it's a known rule that life scales by row)
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

    /**
     * Show zoomed plane card overlay.
     */
    private void showZoom() {
        if (currentPlaneCard == null) {
            return;
        }

        // Lazily initialize zoom utility
        if (zoomUtil == null) {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                zoomUtil = new CardZoomUtil(window);
                zoomUtil.setupZoomOverlay();
            }
        }

        if (zoomUtil == null) {
            return;
        }

        // Use cached rotated image if available, otherwise create and cache it
        if (cachedRotatedImage == null) {
            BufferedImage originalImage = getPlaneCardImage(currentPlaneCard);
            if (originalImage != null) {
                cachedRotatedImage = rotateImage90Clockwise(originalImage);
            }
        }

        if (cachedRotatedImage != null) {
            zoomUtil.showZoom(cachedRotatedImage);
        }
    }

    /**
     * Close the zoom overlay.
     */
    private void closeZoom() {
        if (zoomUtil != null) {
            zoomUtil.closeZoom();
        }
    }

    /**
     * Callback from ImageFetcher when a card image has been downloaded.
     * Updates the display with the newly downloaded image.
     */
    @Override
    public void onImageFetched() {
        if (currentPlaneCard == null) {
            return;
        }

        System.out.println("=== Image fetched for: " + currentPlaneCard.getName() + " ===");

        // Clear cached rotated image so it gets regenerated with the new downloaded image
        cachedRotatedImage = null;

        // Get the newly downloaded image
        BufferedImage originalImage = getPlaneCardImage(currentPlaneCard);
        if (originalImage != null) {
            System.out.println("New image size: " + originalImage.getWidth() + "x" + originalImage.getHeight());
            BufferedImage rotatedImage = rotateImage90Clockwise(originalImage);
            cardImage.setItem(rotatedImage);
            cardImage.revalidate();
            cardImage.repaint();
        }
    }
}
