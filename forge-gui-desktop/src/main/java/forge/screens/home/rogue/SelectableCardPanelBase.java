package forge.screens.home.rogue;

import forge.ImageCache;
import forge.ImageKeys;
import forge.game.card.CardView;
import forge.gui.CardPicturePanel;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.ImageFetcher;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Base class for selectable card panels with flip animation. Provides common functionality for card
 * selection dialogs.
 */
public abstract class SelectableCardPanelBase extends SkinnedPanel implements
    ImageFetcher.Callback {

  protected enum ZoomActivation {
    MOUSE_WHEEL,
    MIDDLE_MOUSE_BUTTON
  }

  // Flip icon dimensions (2:3 aspect ratio matching FSkinProp)
  private static final int FLIP_ICON_WIDTH = 44;
  private static final int FLIP_ICON_HEIGHT = 66;

  protected final PaperCard card;
  protected final CardPicturePanel cardPicture;
  protected boolean selected;
  protected boolean hovered;
  protected boolean faceDown;
  private final CardUtil.FlipAnimation flipAnimation;
  private final Supplier<CardUtil> zoomUtilSupplier;
  private final ZoomActivation zoomActivation;

  // Double-faced card support
  protected final boolean hasBackFace;
  protected boolean showingAltFace;

  /**
   * Create a selectable card panel.
   *
   * @param card             The card to display
   * @param zoomUtilSupplier Supplier for zoom utility (accessed at runtime, can return null)
   * @param faceDown         Whether the card starts face-down (for flip animation reveal)
   * @param zoomActivation   Mouse action used to zoom the card
   */
  public SelectableCardPanelBase(PaperCard card, Supplier<CardUtil> zoomUtilSupplier,
      boolean faceDown, ZoomActivation zoomActivation) {
    super(null);
    this.card = card;
    this.selected = false;
    this.faceDown = faceDown;
    this.flipAnimation = new CardUtil.FlipAnimation(this);
    this.cardPicture = new CardPicturePanel();
    this.zoomUtilSupplier = zoomUtilSupplier;
    this.zoomActivation = zoomActivation;

    // Check if card has a back face (transform, flip, meld, modal)
    this.hasBackFace = card.hasBackFace();
    this.showingAltFace = false;

    setOpaque(false);
    setLayout(null); // Manual layout

    updateCardDisplay();
    cardPicture.setOpaque(false);
    add(cardPicture);

    // Add mouse listener for selection and flip button clicks
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (zoomActivation == ZoomActivation.MIDDLE_MOUSE_BUTTON
            && e.getButton() == MouseEvent.BUTTON2) {
          return;
        }
        if (!SelectableCardPanelBase.this.faceDown) {
          // Check if click is on flip icon area
          if (hasBackFace && isClickOnFlipIcon(e)) {
            if (!flipAnimation.isAnimating()) {
              SoundSystem.instance.play(SoundEffectType.FlipCard,
                  SoundEffectType.FlipCard.isSynced());
              flipToOtherFace();
            }
          } else {
            toggleSelection();
          }
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        if (zoomActivation == ZoomActivation.MIDDLE_MOUSE_BUTTON
            && e.getButton() == MouseEvent.BUTTON2
            && !SelectableCardPanelBase.this.faceDown) {
          CardUtil zoomUtil = zoomUtilSupplier != null ? zoomUtilSupplier.get() : null;
          if (zoomUtil != null) {
            zoomUtil.showZoom(card, showingAltFace);
          }
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (zoomActivation == ZoomActivation.MIDDLE_MOUSE_BUTTON
            && e.getButton() == MouseEvent.BUTTON2) {
          CardUtil zoomUtil = zoomUtilSupplier != null ? zoomUtilSupplier.get() : null;
          if (zoomUtil != null) {
            zoomUtil.closeZoom();
          }
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        hovered = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hovered = false;
        setCursor(Cursor.getDefaultCursor());
        repaint();
      }
    });

    if (zoomActivation == ZoomActivation.MOUSE_WHEEL) {
      // Add mouse wheel listener for card zoom (only when revealed)
      addMouseWheelListener(e -> {
        if (!SelectableCardPanelBase.this.faceDown && e.getWheelRotation() < 0) {
          CardUtil zoomUtil = zoomUtilSupplier != null ? zoomUtilSupplier.get() : null;
          if (zoomUtil != null) {
            // Zoom the currently displayed face
            zoomUtil.showZoom(card, showingAltFace);
          }
        }
      });
    }
  }

  /**
   * Check if the click was on the flip icon area. Icon is positioned in upper third, right side.
   */
  private boolean isClickOnFlipIcon(MouseEvent e) {
    int iconX = (int) (getWidth() * 0.72) - FLIP_ICON_WIDTH / 2;
    int iconY = (int) (getHeight() * 0.18);
    int padding = 4;
    int x = e.getX();
    int y = e.getY();
    return x >= iconX - padding && x <= iconX + FLIP_ICON_WIDTH + padding
        && y >= iconY - padding && y <= iconY + FLIP_ICON_HEIGHT + padding;
  }

  /**
   * Update the card display (face-up or face-down, front or back face).
   */
  protected void updateCardDisplay() {
    if (faceDown) {
      cardPicture.setCard(CardView.getCardForUi(card).getCurrentState(), false);
      GuiBase.getInterface().getImageFetcher().fetchImage(
          ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), cardPicture);
    } else if (showingAltFace && hasBackFace) {
      // Show alternate face - check if we need to fetch the image
      String altImageKey = card.getImageKey(true);
      Pair<BufferedImage, Boolean> imageInfo = ImageCache.getCardOriginalImageInfo(altImageKey,
          true);
      BufferedImage altImage = imageInfo.getLeft();
      boolean isPlaceholder = imageInfo.getRight();

      // Trigger fetch if image is missing or placeholder
      if (ImageCache.isDefaultImage(altImage) || isPlaceholder) {
        GuiBase.getInterface().getImageFetcher().fetchImage(altImageKey, this);
      }

      if (altImage != null) {
        cardPicture.setItem(altImage);
      } else {
        cardPicture.setItem(card);
      }
    } else {
      // Show front face
      cardPicture.setItem(card);
    }
  }

  /**
   * Callback from ImageFetcher when a card image has been downloaded.
   */
  @Override
  public void onImageFetched() {
    // Refresh display with newly downloaded image
    updateCardDisplay();
    repaint();
  }

  /**
   * Start the flip animation to reveal the card.
   */
  public void flip() {
    if (flipAnimation.isAnimating() || !faceDown) {
      return; // Already revealed or animating
    }
    flipAnimation.start(() -> {
      faceDown = false;
      updateCardDisplay();
    });
  }

  /**
   * Start the flip animation to show the other face of a double-faced card.
   */
  public void flipToOtherFace() {
    if (flipAnimation.isAnimating() || faceDown || !hasBackFace) {
      return; // Can't flip if animating, face-down, or no back face
    }
    flipAnimation.start(() -> {
      showingAltFace = !showingAltFace;
      updateCardDisplay();
    });
  }

  /**
   * Set the selection state.
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
    repaint();
  }

  /**
   * Toggle the selection state. Subclasses can override to add custom logic (e.g., budget
   * checking).
   */
  protected abstract void toggleSelection();

  @Override
  public void doLayout() {
    // Make card picture fill the panel
    cardPicture.setBounds(0, 0, getWidth(), getHeight());
  }

  @Override
  public void paint(Graphics g) {
    final Graphics2D g2d = (Graphics2D) g;
    final int width = getWidth();
    final int height = getHeight();

    // Apply horizontal scale transformation if animating
    if (flipAnimation.isAnimating() && flipAnimation.getScaleX() != 1.0) {
      // Save the original transform
      AffineTransform originalTransform = g2d.getTransform();

      // Smoother scaling during animation
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      // Create scale transform centered on the card
      AffineTransform scaleTransform = new AffineTransform();
      scaleTransform.translate(width / 2.0, 0); // Move origin to center
      scaleTransform.scale(Math.max(0.01, flipAnimation.getScaleX()),
          1.0); // Scale horizontally (min 0.01 to avoid zero)
      scaleTransform.translate(-width / 2.0, 0); // Move origin back

      g2d.transform(scaleTransform);
      super.paint(g);
      g2d.setTransform(originalTransform);
    } else {
      super.paint(g);
    }

    // Draw hover/selection indicators ON TOP of everything (only if not animating)
    if (!flipAnimation.isAnimating()) {
      if (selected) {
        drawSelectionHighlight(g2d, width, height);
      } else if (hovered && !faceDown) {
        // Draw yellow/gold hover border when not selected and card is revealed
        drawHoverHighlight(g2d, width, height);
      }

      // Draw flip icon for double-faced cards (when revealed)
      if (hasBackFace && !faceDown) {
        drawFlipIcon(g2d);
      }
    }
  }

  /**
   * Draw the flip icon in upper third, right side (Moxfield style).
   */
  private void drawFlipIcon(Graphics2D g2d) {
    // Position: upper third vertically, right side horizontally
    int iconX = (int) (getWidth() * 0.72) - FLIP_ICON_WIDTH / 2;
    int iconY = (int) (getHeight() * 0.18);

    // Draw nearly opaque black background with oval/pill shape for visibility
    int padding = 5;
    int bgWidth = FLIP_ICON_WIDTH + padding * 2;
    int bgHeight = FLIP_ICON_HEIGHT + padding * 2;
    int cornerRadius = bgWidth; // Full width = oval/pill shape
    g2d.setColor(new Color(0, 0, 0, 230)); // Tiny bit of transparency
    g2d.fillRoundRect(iconX - padding, iconY - padding, bgWidth, bgHeight, cornerRadius,
        cornerRadius);

    // Draw the icon
    FSkin.drawImage(g2d, FSkin.getIcon(FSkinProp.ICO_FLIPCARD),
        iconX, iconY, FLIP_ICON_WIDTH, FLIP_ICON_HEIGHT);
  }

  /**
   * Draw the hover highlight (yellow/gold border).
   */
  protected void drawHoverHighlight(Graphics2D g2d, int width, int height) {
    g2d.setColor(new Color(255, 215, 0));  // Gold color
    g2d.setStroke(new BasicStroke(4));
    g2d.drawRect(3, 3, width - 6, height - 6);
  }

  /**
   * Draw the selection highlight (green border + checkmark).
   */
  protected void drawSelectionHighlight(Graphics2D g2d, int width, int height) {
    // Draw thick border
    g2d.setColor(new Color(0, 255, 0, 200)); // Green with transparency
    g2d.setStroke(new BasicStroke(6)); // Thicker border
    g2d.drawRect(3, 3, width - 6, height - 6);

    // Draw checkmark in top-right corner
    int checkSize = 30;
    int checkX = width - checkSize - 8;
    int checkY = 8;

    // Draw circle background
    g2d.setColor(new Color(0, 200, 0, 230));
    g2d.fillOval(checkX, checkY, checkSize, checkSize);

    // Draw checkmark
    g2d.setColor(Color.WHITE);
    g2d.setStroke(new BasicStroke(3));
    int[] xPoints = {checkX + 7, checkX + 12, checkX + 23};
    int[] yPoints = {checkY + 15, checkY + 20, checkY + 10};
    g2d.drawPolyline(xPoints, yPoints, 3);
  }
}
