package forge.screens.home.rogue;

import forge.ImageCache;
import forge.card.CardSplitType;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.miginfocom.swing.MigLayout;

/**
 * Utility class for card display functionality in Rogue Commander. Provides zoom overlay logic and
 * reusable flip animation for card panels.
 */
public class CardUtil {

  private JPanel zoomOverlay;
  private PaperCard currentZoomedCard;
  private final Window parentWindow;

  /**
   * Create a CardUtil for the given parent window.
   *
   * @param parentWindow The window (JDialog or JFrame) that will host the zoom overlay
   */
  public CardUtil(Window parentWindow) {
    this.parentWindow = parentWindow;
  }

  /**
   * Setup the zoom overlay on the parent window's glass pane.
   */
  public void setupZoomOverlay() {
    if (parentWindow == null) {
      return;
    }

    zoomOverlay = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        // Semi-transparent black background
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, getWidth(), getHeight());
      }
    };
    zoomOverlay.setOpaque(false);
    zoomOverlay.setLayout(new MigLayout("insets 0, wrap, ax center, ay center"));
    zoomOverlay.setVisible(false);

    // Add mouse listener to close zoom on click
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

    // Set as glass pane
    if (parentWindow instanceof JDialog) {
      ((JDialog) parentWindow).setGlassPane(zoomOverlay);
    } else if (parentWindow instanceof JFrame) {
      ((JFrame) parentWindow).setGlassPane(zoomOverlay);
    }
  }

  /**
   * Show zoomed view of a card.
   *
   * @param card The card to zoom
   */
  public void showZoom(PaperCard card) {
    showZoom(card, false);
  }

  /**
   * Show zoomed view of a card, optionally showing the alternate face.
   *
   * @param card        The card to zoom
   * @param showAltFace Whether to show the alternate face (for double-faced cards)
   */
  public void showZoom(PaperCard card, boolean showAltFace) {
    if (zoomOverlay == null) {
      return;
    }

    // Always set glass pane when showing zoom (multiple components may share the same window)
    setGlassPane();

    currentZoomedCard = card;
    zoomOverlay.removeAll();

    BufferedImage cardImage = null;

    // Get the appropriate face based on showAltFace
    if (showAltFace && card.hasBackFace()) {
      // Use ImageCache.getImage with altState=true for back face
      cardImage = ImageCache.getImage(card, -1, -1, true);
    } else {
      // Front face
      cardImage = ImageCache.getImage(card, -1, -1, false);
    }

    // Fallback to FImageUtil if ImageCache returned null or default
    if (cardImage == null || ImageCache.isDefaultImage(cardImage)) {
      Card gameCard = Card.getCardForUi(card);
      if (gameCard != null) {
        CardView cardView = CardView.get(gameCard);
        if (showAltFace && card.hasBackFace() && cardView.getAlternateState() != null) {
          cardImage = FImageUtil.getImageXlhq(cardView.getAlternateState());
          if (cardImage == null) {
            cardImage = FImageUtil.getImage(cardView.getAlternateState());
          }
        } else {
          cardImage = FImageUtil.getImageXlhq(cardView.getCurrentState());
          if (cardImage == null) {
            cardImage = FImageUtil.getImage(cardView.getCurrentState());
          }
        }
      }
    }

    if (cardImage != null) {
      int rotation = getCardRotation(card);
      FImagePanel imagePanel = new FImagePanel();
      imagePanel.setImage(cardImage, rotation, AutoSizeImageMode.SOURCE);
      zoomOverlay.add(imagePanel, "w 80%!, h 80%!");
    }

    zoomOverlay.setVisible(true);
    zoomOverlay.requestFocusInWindow();
    zoomOverlay.revalidate();
    zoomOverlay.repaint();
  }

  /**
   * Get the rotation needed for a card (split cards and planes display at 90 degrees).
   */
  private int getCardRotation(PaperCard card) {
    if (card.getRules().getSplitType() == CardSplitType.Split) {
      return 90;
    }
    if (card.getRules().getType().isPlane() || card.getRules().getType().isPhenomenon()) {
      return 90;
    }
    return 0;
  }

  /**
   * Show zoomed view of a pre-rendered card image. Useful for displaying rotated or modified card
   * images.
   *
   * @param cardImage The card image to zoom
   */
  public void showZoom(BufferedImage cardImage) {
    if (zoomOverlay == null || cardImage == null) {
      return;
    }

    // Always set glass pane when showing zoom (multiple components may share the same window)
    setGlassPane();

    currentZoomedCard = null; // No PaperCard associated
    zoomOverlay.removeAll();

    FImagePanel imagePanel = new FImagePanel();
    imagePanel.setImage(cardImage, 0, AutoSizeImageMode.SOURCE);
    zoomOverlay.add(imagePanel, "w 80%!, h 80%!");

    zoomOverlay.setVisible(true);
    zoomOverlay.requestFocusInWindow();
    zoomOverlay.revalidate();
    zoomOverlay.repaint();
  }

  /**
   * Set this overlay as the active glass pane on the parent window. Called every time zoom is shown
   * to ensure multiple components sharing the same window don't interfere with each other.
   */
  private void setGlassPane() {
    if (parentWindow == null || zoomOverlay == null) {
      return;
    }

    if (parentWindow instanceof JDialog) {
      ((JDialog) parentWindow).setGlassPane(zoomOverlay);
    } else if (parentWindow instanceof JFrame) {
      ((JFrame) parentWindow).setGlassPane(zoomOverlay);
    }
  }

  /**
   * Close the zoom overlay.
   */
  public void closeZoom() {
    if (zoomOverlay != null) {
      zoomOverlay.setVisible(false);
      zoomOverlay.removeAll();
      currentZoomedCard = null;
    }
  }

  /**
   * Get the current zoomed card.
   *
   * @return The currently zoomed card, or null if no card is zoomed
   */
  public PaperCard getCurrentZoomedCard() {
    return currentZoomedCard;
  }

  /**
   * Check if a card is currently being zoomed.
   *
   * @return true if a card is being displayed in zoom mode
   */
  public boolean isZooming() {
    return zoomOverlay != null && zoomOverlay.isVisible();
  }

  /**
   * Reusable flip animation state manager. Handles the Timer and elapsed time for a horizontal
   * card flip (scale 1->0->1). Panels query {@link #isAnimating()} and {@link #getScaleX()} in
   * their own paint() method to apply the transform directly — no lambda indirection.
   */
  public static class FlipAnimation {

    private static final int ANIMATION_DURATION = 300; // milliseconds
    private static final int FRAMES_PER_SECOND = 60;
    private static final int FRAME_DELAY = 1000 / FRAMES_PER_SECOND;

    private final JComponent target;
    private boolean animating;
    private double scaleX = 1.0;
    private Timer animationTimer;

    public FlipAnimation(JComponent target) {
      this.target = target;
    }

    /**
     * Start the flip animation with a custom action at the midpoint.
     *
     * @param midpointAction Action to run when the card is "edge-on" (scale = 0), e.g., swap image
     */
    public void start(Runnable midpointAction) {
      if (animating) {
        return;
      }
      animating = true;
      final long startNanos = System.nanoTime();
      final long durationNanos = ANIMATION_DURATION * 1_000_000L;
      final boolean[] actionExecuted = {false};

      animationTimer = new Timer(FRAME_DELAY, e -> {
        double progress = Math.min(1.0, (System.nanoTime() - startNanos) / (double) durationNanos);

        // Cosine easing: smooth ease-in-out (1.0 -> 0.0 -> 1.0)
        scaleX = Math.abs(Math.cos(progress * Math.PI));

        // Execute midpoint action and update display
        if (!actionExecuted[0] && progress >= 0.5) {
          scaleX = 0.0;
          midpointAction.run();
          actionExecuted[0] = true;
        }

        target.repaint();

        // End animation
        if (progress >= 1.0) {
          animationTimer.stop();
          animating = false;
          scaleX = 1.0;
          target.repaint();
        }
      });

      animationTimer.start();
    }

    public boolean isAnimating() {
      return animating;
    }

    public double getScaleX() {
      return scaleX;
    }
  }
}
