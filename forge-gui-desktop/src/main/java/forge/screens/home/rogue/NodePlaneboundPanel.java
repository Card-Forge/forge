package forge.screens.home.rogue;

import forge.ImageCache;
import forge.ImageKeys;
import forge.deck.CardPool;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gui.CardPicturePanel;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FSkin;
import forge.toolbox.imaging.FImageUtil;
import forge.util.ImageFetcher;
import forge.util.Localizer;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.*;
import org.tinylog.Logger;

/**
 * Visual representation of a Planebound node in the Rogue Commander path. Displays the plane card
 * image.
 */
public class NodePlaneboundPanel extends NodePanel implements ImageFetcher.Callback {

  // Plane cards are horizontal, so width > height (rotated 90 degrees from normal cards)
  private static final int CARD_WIDTH = 250;  // Wider (was height)
  private static final int CARD_HEIGHT = 180; // Shorter (was width)
  private static final int FACE_DOWN_PLANE_SLEEVE_INDEX = 18;

  private static final ImageIcon FLAME_ICON = createFlameIcon(14, 18);
  private static final ImageIcon PENTAGRAM_ICON = createPentagramIcon(14, 18);

  private final CardPicturePanel cardImage;
  private final JPanel pnlNameRow;
  private final JPanel pnlLifeRow;
  private final JLabel lblPlaneboundName;
  private final JLabel lblLifeTotal;
  private final PaperCard currentPlaneCard;
  private boolean awaitingFlip;

  // Zoom utility
  private transient CardUtil zoomUtil; // Lazily initialized on first zoom
  private transient BufferedImage cachedRotatedImage; // Cache rotated image to avoid recreating

  // Flip animation
  private final transient CardUtil.FlipAnimation flipAnimation;
  private transient BufferedImage revealImage; // The face-up image to show after flip

  /**
   * Create a panel for displaying a planebound node.
   *
   * @param node               Node data to display
   * @param isFaceDown         Whether to display the card face-down
   * @param planeboundRowCount Number of Planebound rows up to this node (for life calculation)
   * @param animateReveal      Whether to start face-down and animate to face-up
   */
  public NodePlaneboundPanel(NodePlanebound node, boolean isFaceDown, int planeboundRowCount,
      boolean animateReveal) {
    super(node);
    this.awaitingFlip = animateReveal;
    this.flipAnimation = new CardUtil.FlipAnimation(this);

    // Card image (plane card) - rotated 90 degrees clockwise for horizontal display
    cardImage = new CardPicturePanel();
    cardImage.setOpaque(false);
    cardImage.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));

    String planeName = node.getRoguePlanebound().planeName();
    PaperCard planeCard = getPlaneCard(planeName);
    boolean showFaceDown = isFaceDown || animateReveal;

    if (showFaceDown) {
      // Show card back
      BufferedImage cardBack = getFaceDownPlaneBackImage();
      GuiBase.getInterface().getImageFetcher().fetchImage(
          ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), () -> setHiddenCardBackImage(getFaceDownPlaneBackImage()));
      setHiddenCardBackImage(cardBack);
    }

    if (!isFaceDown) {
      loadFaceUpImage(planeCard, planeName, animateReveal);
    }

    add(cardImage);

    // Store the plane card for zoom functionality
    currentPlaneCard = planeCard;

    // Add mouse wheel listener for zoom (only for face-up cards)
    addMouseWheelListener(e -> {
      if (!isFaceDown && e.getWheelRotation() < 0
          && currentPlaneCard != null) { // Scroll up to zoom
        showZoom();
      }
    });

    // Name row: elite/boss marker + planebound name in a centered flow layout
    pnlNameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
    pnlNameRow.setOpaque(false);

    String planeboundName = showFaceDown ? "???" : node.getRoguePlanebound().planeboundName();
    lblPlaneboundName = new JLabel(planeboundName);
    lblPlaneboundName.setFont(FSkin.getRelativeFont(13).getBaseFont());
    lblPlaneboundName.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());

    RoguePlaneboundType type = node.getPlaneboundType();
    if (type == RoguePlaneboundType.ELITE) {
      JLabel star = new JLabel(FSkin.getImage(FSkinProp.IMG_STAR_FILLED).resize(16, 16).getIcon());
      star.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 5, 0));
      star.setToolTipText(Localizer.getInstance().getMessage("lblRogueElitePlanebound"));
      pnlNameRow.add(star);
    } else if (type == RoguePlaneboundType.BOSS) {
      JLabel skull = new JLabel(FSkin.getImage(FSkinProp.IMG_POISON).resize(18, 18).getIcon());
      skull.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 5, 0));
      skull.setToolTipText(Localizer.getInstance().getMessage("lblRogueBossPlanebound"));
      pnlNameRow.add(skull);
    }

    pnlNameRow.add(lblPlaneboundName);
    add(pnlNameRow);

    // Lower stat row: wrathful/cursed markers + life total
    pnlLifeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
    pnlLifeRow.setOpaque(false);

    for (int i = 0; i < node.getWrathfulCount(); i++) {
      JLabel flame = new JLabel(FLAME_ICON);
      flame.setToolTipText(Localizer.getInstance().getMessage("lblRogueWrathfulTooltip"));
      pnlLifeRow.add(flame);
    }

    for (int i = 0; i < node.getCursedCount(); i++) {
      JLabel pentagram = new JLabel(PENTAGRAM_ICON);
      pentagram.setToolTipText(Localizer.getInstance().getMessage("lblRogueCursedTooltip"));
      pnlLifeRow.add(pentagram);
    }

    int planeboundLife = node.getPlaneboundLife(planeboundRowCount);
    lblLifeTotal = new JLabel("\u2665 " + planeboundLife);
    lblLifeTotal.setFont(FSkin.getRelativeBoldFont(14).getBaseFont());
    lblLifeTotal.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
    lblLifeTotal.setHorizontalAlignment(SwingConstants.CENTER);
    lblLifeTotal.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 6, 0, 0));
    lblLifeTotal.setToolTipText(Localizer.getInstance().getMessage("lblRoguePlaneboundStartingLife", planeboundLife));
    pnlLifeRow.add(lblLifeTotal);
    add(pnlLifeRow);
  }

  private void loadFaceUpImage(PaperCard planeCard, String planeName, boolean animateReveal) {
    if (planeCard == null) {
      Logger.warn("Plane card not found in database: {}", planeName);
      return;
    }

    BufferedImage originalImage = CardUtil.getCardImageAndFetchIfNeeded(planeCard.getImageKey(false), this);
    if (originalImage != null) {
      revealImage = rotateImage90Clockwise(originalImage);
      if (!animateReveal) {
        cardImage.setItem(revealImage);
      }
    }
  }

  /**
   * Animate flipping from face-down to face-up.
   */
  public void flipToReveal() {
    if (flipAnimation.isAnimating() || revealImage == null) {
      return;
    }
    SoundSystem.instance.play(SoundEffectType.FlipCard,
        SoundEffectType.FlipCard.isSynced());
    awaitingFlip = false;
    flipAnimation.start(() -> {
      cardImage.setItem(revealImage);
      lblPlaneboundName.setText(((NodePlanebound) node).getRoguePlanebound().planeboundName());
    });
  }

  @Override
  public void paint(Graphics g) {
    if (flipAnimation.isAnimating() && flipAnimation.getScaleX() != 1.0) {
      Graphics2D g2d = (Graphics2D) g;
      AffineTransform originalTransform = g2d.getTransform();

      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      AffineTransform scaleTransform = new AffineTransform();
      scaleTransform.translate(getWidth() / 2.0, 0);
      scaleTransform.scale(Math.max(0.01, flipAnimation.getScaleX()), 1.0);
      scaleTransform.translate(-getWidth() / 2.0, 0);

      g2d.transform(scaleTransform);
      super.paint(g);
      g2d.setTransform(originalTransform);
    } else {
      super.paint(g);
    }
  }

  @Override
  public void doLayout() {
    int x = 10;
    int y = 10;

    // Card image
    cardImage.setBounds(x, y, CARD_WIDTH, CARD_HEIGHT);
    y += CARD_HEIGHT + 5;

    // Name row
    pnlNameRow.setBounds(x, y, CARD_WIDTH, 20);
    y += 25;

    // Lower stat row
    pnlLifeRow.setBounds(x, y, CARD_WIDTH, 20);
  }

  /**
   * Get the BufferedImage for a plane card. Try to get high-quality artwork first, fall back to
   * regular image if not available.
   */
  private BufferedImage getPlaneCardImage(PaperCard planeCard) {
    try {
      Card gameCard = Card.getCardForUi(planeCard);
      CardView cardView = CardView.get(gameCard);

      // Try high-quality image first (actual artwork)
      BufferedImage image = FImageUtil.getImageXlhq(cardView.getCurrentState());
      if (image != null) {
        return image;
      }

      // Fall back to regular image
      image = FImageUtil.getImage(cardView.getCurrentState());
      return image;
    } catch (Exception e) {
      Logger.warn(e, "Could not get image for plane card: {}", planeCard.getName());
    }
    return null;
  }

  /**
   * Rotate a BufferedImage 90 degrees clockwise. Plane cards are horizontal, so we need to rotate
   * them from their default vertical display.
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

  private void setHiddenCardBackImage(BufferedImage cardBack) {
    if (cardBack == null) {
      return;
    }
    cardImage.setItem(rotateImage90Clockwise(cardBack));
    cardImage.revalidate();
    cardImage.repaint();
  }

  private static BufferedImage getFaceDownPlaneBackImage() {
    BufferedImage sleeveBack = FSkin.getSleeveImage(FACE_DOWN_PLANE_SLEEVE_INDEX);
    if (sleeveBack != null) {
      return sleeveBack;
    }
    return ImageCache.getOriginalImage(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), true, null);
  }

  /**
   * Get the plane card by name from the variant cards collection. Uses the centralized
   * RogueConfig.getAllPlanes() method.
   */
  private static PaperCard getPlaneCard(String planeName) {
    try {
      // Get all plane cards from the centralized cache
      CardPool allPlanes = RogueConfig.getAllPlanes();

      // Find the plane card by name
      for (PaperCard card : allPlanes.toFlatList()) {
        if (card.getName().equalsIgnoreCase(planeName)) {
          return card;
        }
      }
      return null;
    } catch (Exception e) {
      Logger.warn(e, "Error loading plane card: {}", planeName);
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
        zoomUtil = new CardUtil(window);
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
   * Callback from ImageFetcher when a card image has been downloaded. Updates the display with the
   * newly downloaded image.
   */
  @Override
  public void onImageFetched() {
    if (currentPlaneCard == null) {
      return;
    }

    Logger.debug("Image fetched for plane card: {}", currentPlaneCard.getName());

    // Clear cached rotated image so it gets regenerated with the new downloaded image
    cachedRotatedImage = null;

    // Get the newly downloaded image
    BufferedImage originalImage = getPlaneCardImage(currentPlaneCard);
    if (originalImage != null) {
      Logger.debug("Plane card image size: {}x{}", originalImage.getWidth(), originalImage.getHeight());
      BufferedImage rotatedImage = rotateImage90Clockwise(originalImage);
      // Update revealImage so the flip animation uses the real image, not the fallback
      revealImage = rotatedImage;
      // Only update cardImage if the card is already face-up (not waiting for flip animation)
      if (!awaitingFlip) {
        cardImage.setItem(rotatedImage);
        cardImage.revalidate();
        cardImage.repaint();
      }
    }
  }

  static ImageIcon createPentagramIcon(int w, int h) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    double cx = w / 2.0;
    double cy = h / 2.0;
    double r = h / 2.0 - 1;
    // 5 vertices, connect every other (0->2->4->1->3) for pentagram shape
    double[] px = new double[5];
    double[] py = new double[5];
    for (int i = 0; i < 5; i++) {
      double angle = Math.PI / 2 + i * 2 * Math.PI / 5;
      px[i] = cx + r * Math.cos(angle);
      py[i] = cy - r * Math.sin(angle);
    }
    // Solid fill: use WIND_NON_ZERO so the center is filled too
    Path2D pentagram = new Path2D.Double();
    int[] order = {0, 2, 4, 1, 3};
    pentagram.moveTo(px[order[0]], py[order[0]]);
    for (int i = 1; i < 5; i++) pentagram.lineTo(px[order[i]], py[order[i]]);
    pentagram.closePath();
    g.setColor(new Color(80, 10, 120));
    g.fill(pentagram);
    // Draw crossing lines in lighter purple to show pentagram structure
    g.setStroke(new BasicStroke(1.5f));
    g.setColor(new Color(200, 130, 255));
    for (int i = 0; i < 5; i++) {
      int j = (i + 2) % 5;
      g.drawLine((int) px[i], (int) py[i], (int) px[j], (int) py[j]);
    }
    g.dispose();
    return new ImageIcon(img);
  }

  static ImageIcon createFlameIcon(int w, int h) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    double cx = w / 2.0;
    // Outer flame: narrow tip at top, wide bulge in lower half
    Path2D outer = new Path2D.Double();
    outer.moveTo(cx, 0);
    outer.curveTo(cx + w * 0.1, h * 0.2, cx + w * 0.55, h * 0.4, cx + w * 0.45, h * 0.7);
    outer.curveTo(cx + w * 0.35, h * 0.9, cx + w * 0.1, h, cx, h);
    outer.curveTo(cx - w * 0.1, h, cx - w * 0.35, h * 0.9, cx - w * 0.45, h * 0.7);
    outer.curveTo(cx - w * 0.55, h * 0.4, cx - w * 0.1, h * 0.2, cx, 0);
    g.setColor(new Color(255, 80, 0));
    g.fill(outer);
    // Inner core: smaller, yellow, offset downward
    Path2D inner = new Path2D.Double();
    inner.moveTo(cx, h * 0.3);
    inner.curveTo(cx + w * 0.05, h * 0.45, cx + w * 0.25, h * 0.55, cx + w * 0.2, h * 0.75);
    inner.curveTo(cx + w * 0.1, h * 0.9, cx, h * 0.95, cx, h * 0.95);
    inner.curveTo(cx, h * 0.95, cx - w * 0.1, h * 0.9, cx - w * 0.2, h * 0.75);
    inner.curveTo(cx - w * 0.25, h * 0.55, cx - w * 0.05, h * 0.45, cx, h * 0.3);
    g.setColor(new Color(255, 200, 50));
    g.fill(inner);
    g.dispose();
    return new ImageIcon(img);
  }
}
