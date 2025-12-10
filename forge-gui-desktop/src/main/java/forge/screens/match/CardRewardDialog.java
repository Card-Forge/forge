package forge.screens.match;

import com.google.common.collect.ImmutableList;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gui.CardPicturePanel;
import forge.item.PaperCard;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;
import forge.util.Localizer;
import forge.view.arcane.CardPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog for selecting reward cards visually.
 * Displays cards as images and allows selecting up to a maximum number.
 */
public class CardRewardDialog {
    private static final int CARD_WIDTH = 223;  // Larger cards for readability
    private static final int CARD_HEIGHT = Math.round(CARD_WIDTH * CardPanel.ASPECT_RATIO);
    private static final int CARD_SPACING = 15;
    private static final int PADDING = 30;

    private final List<PaperCard> cards;
    private final int maxSelections;
    private final Set<PaperCard> selectedCards;
    private final List<SelectableCardPanel> cardPanels;
    private final MainPanel panel;
    private final FLabel lblInfo;
    private FOptionPane optionPane;
    private JPanel zoomOverlay;
    private PaperCard currentZoomedCard;

    /**
     * Create a card reward selection dialog.
     * @param title Dialog title
     * @param cards List of cards to choose from
     * @param minSelections Minimum number of cards to select (0 for optional)
     * @param maxSelections Maximum number of cards to select
     */
    public CardRewardDialog(String title, List<PaperCard> cards, int minSelections, int maxSelections) {
        this.cards = cards;
        this.maxSelections = maxSelections;
        this.selectedCards = new HashSet<>();
        this.cardPanels = new ArrayList<>();

        // Create info label
        lblInfo = new FLabel.Builder()
                .text(getInfoText())
                .fontSize(14)
                .fontAlign(SwingConstants.CENTER)
                .build();

        // Create main panel
        panel = new MainPanel();
        panel.add(lblInfo);

        // Create card panels
        for (PaperCard card : cards) {
            SelectableCardPanel cardPanel = new SelectableCardPanel(card);
            cardPanels.add(cardPanel);
            panel.add(cardPanel);
        }

        // Calculate dialog size (7 cards horizontally with proper spacing)
        int numCardsToShow = Math.min(cards.size(), 7);
        int dialogWidth = numCardsToShow * (CARD_WIDTH + CARD_SPACING) - CARD_SPACING + 2 * PADDING;
        int dialogHeight = CARD_HEIGHT + 100 + 2 * PADDING; // 100px for info label and spacing

        Dimension dialogSize = new Dimension(dialogWidth, dialogHeight);
        panel.setPreferredSize(dialogSize);
        panel.setMinimumSize(dialogSize);
    }

    /**
     * Show the dialog and return the selected cards.
     * @return List of selected cards, or empty list if canceled
     */
    public List<PaperCard> show() {
        final Localizer localizer = Localizer.getInstance();
        optionPane = new FOptionPane(
                null,
                "Card Rewards",
                null,
                panel,
                ImmutableList.of(localizer.getMessage("lblOK"), localizer.getMessage("lblCancel")),
                0
        );

        // Setup zoom overlay on the dialog's glass pane
        setupZoomOverlay();

        panel.revalidate();
        panel.repaint();
        optionPane.setVisible(true);

        int result = optionPane.getResult();
        optionPane.dispose();

        if (result == 0) {
            return new ArrayList<>(selectedCards);
        }
        return new ArrayList<>();
    }

    /**
     * Setup a zoom overlay that appears on top of the dialog when a card is zoomed.
     */
    private void setupZoomOverlay() {
        // Get the dialog's layered pane to add overlay on top
        JDialog dialog = optionPane;
        if (dialog != null) {
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

            // Add overlay to dialog's layered pane at highest layer
            dialog.setGlassPane(zoomOverlay);
        }
    }

    /**
     * Show zoomed view of a card.
     */
    private void showZoom(PaperCard card) {
        if (zoomOverlay == null) {
            return;
        }

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
     * Close the zoom overlay.
     */
    private void closeZoom() {
        if (zoomOverlay != null) {
            zoomOverlay.setVisible(false);
            zoomOverlay.removeAll();
            currentZoomedCard = null;
        }
    }

    private void updateInfoLabel() {
        lblInfo.setText(getInfoText());
    }

    private String getInfoText() {
        return String.format("Select up to %d cards (%d selected)", maxSelections, selectedCards.size());
    }

    private void toggleCardSelection(SelectableCardPanel cardPanel) {
        PaperCard card = cardPanel.card;

        if (selectedCards.contains(card)) {
            // Deselect
            selectedCards.remove(card);
            cardPanel.setSelected(false);
        } else if (selectedCards.size() < maxSelections) {
            // Select (if under limit)
            selectedCards.add(card);
            cardPanel.setSelected(true);
        }

        updateInfoLabel();
    }

    private class MainPanel extends SkinnedPanel {
        private MainPanel() {
            super(null);
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            int y = PADDING;
            int totalWidth = getWidth();

            // Layout info label
            lblInfo.setBounds(PADDING, y, totalWidth - 2 * PADDING, 30);
            y += 30 + 10;

            // Layout card panels in a horizontal row
            int cardsPerRow = Math.min(cards.size(), 7);
            int totalCardsWidth = cardsPerRow * CARD_WIDTH + (cardsPerRow - 1) * CARD_SPACING;
            int startX = (totalWidth - totalCardsWidth) / 2;

            int x = startX;
            for (SelectableCardPanel cardPanel : cardPanels) {
                cardPanel.setBounds(x, y, CARD_WIDTH, CARD_HEIGHT);
                x += CARD_WIDTH + CARD_SPACING;
            }
        }
    }

    private class SelectableCardPanel extends SkinnedPanel {
        private final PaperCard card;
        private final CardPicturePanel cardPicture;
        private boolean selected;

        private SelectableCardPanel(PaperCard card) {
            super(null);
            this.card = card;
            this.selected = false;
            this.cardPicture = new CardPicturePanel();

            setOpaque(false);
            setLayout(null); // Manual layout

            // Set the card to display
            cardPicture.setItem(card);
            cardPicture.setOpaque(false);
            add(cardPicture);

            // Add mouse listener for selection
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleCardSelection(SelectableCardPanel.this);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setCursor(Cursor.getDefaultCursor());
                }
            });

            // Add mouse wheel listener for card zoom
            addMouseWheelListener(e -> {
                if (e.getWheelRotation() < 0) { // Scroll up to zoom
                    CardRewardDialog.this.showZoom(card);
                }
            });

            // Set tooltip with card name
            setToolTipText(card.getName());
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        public void doLayout() {
            // Make card picture fill the panel
            cardPicture.setBounds(0, 0, getWidth(), getHeight());
        }

        @Override
        public void paint(Graphics g) {
            // First paint the component and all children (including CardPicturePanel)
            super.paint(g);

            // Then draw selection indicators ON TOP of everything
            if (selected) {
                final Graphics2D g2d = (Graphics2D) g;
                final int width = getWidth();
                final int height = getHeight();

                // Draw thick border
                g2d.setColor(new Color(0, 255, 0, 200)); // Green with transparency
                g2d.setStroke(new BasicStroke(6)); // Thicker border
                g2d.drawRect(3, 3, width - 6, height - 6);

                // Draw checkmark in top-right corner
                int checkSize = 30; // Larger checkmark
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
    }
}
