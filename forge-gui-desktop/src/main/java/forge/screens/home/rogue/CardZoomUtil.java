package forge.screens.home.rogue;

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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Utility class for card zoom functionality.
 * Provides shared zoom overlay logic for dialogs and panels that display cards.
 */
public class CardZoomUtil {
    private JPanel zoomOverlay;
    private PaperCard currentZoomedCard;
    private final Window parentWindow;

    /**
     * Create a CardZoomUtil for the given parent window.
     * @param parentWindow The window (JDialog or JFrame) that will host the zoom overlay
     */
    public CardZoomUtil(Window parentWindow) {
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
     * @param card The card to zoom
     */
    public void showZoom(PaperCard card) {
        if (zoomOverlay == null) {
            return;
        }

        // Always set glass pane when showing zoom (multiple components may share the same window)
        setGlassPane();

        currentZoomedCard = card;
        zoomOverlay.removeAll();

        // Get high-quality card image
        Card gameCard = Card.getCardForUi(card);
        if (gameCard != null) {
            CardView cardView = CardView.get(gameCard);
            BufferedImage cardImage = FImageUtil.getImageXlhq(cardView.getCurrentState());
            if (cardImage == null) {
                cardImage = FImageUtil.getImage(cardView.getCurrentState());
            }

            if (cardImage != null) {
                FImagePanel imagePanel = new FImagePanel();
                imagePanel.setImage(cardImage, 0, AutoSizeImageMode.SOURCE);
                zoomOverlay.add(imagePanel, "w 80%!, h 80%!");
            }
        }

        zoomOverlay.setVisible(true);
        zoomOverlay.requestFocusInWindow();
        zoomOverlay.revalidate();
        zoomOverlay.repaint();
    }

    /**
     * Show zoomed view of a pre-rendered card image.
     * Useful for displaying rotated or modified card images.
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
     * Set this overlay as the active glass pane on the parent window.
     * Called every time zoom is shown to ensure multiple components sharing
     * the same window don't interfere with each other.
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
     * @return The currently zoomed card, or null if no card is zoomed
     */
    public PaperCard getCurrentZoomedCard() {
        return currentZoomedCard;
    }

    /**
     * Check if a card is currently being zoomed.
     * @return true if a card is being displayed in zoom mode
     */
    public boolean isZooming() {
        return zoomOverlay != null && zoomOverlay.isVisible();
    }
}
