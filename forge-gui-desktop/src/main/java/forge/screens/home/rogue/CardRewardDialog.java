package forge.screens.home.rogue;

import com.google.common.collect.ImmutableList;
import forge.ImageCache;
import forge.ImageKeys;
import forge.gui.CardPicturePanel;
import forge.item.PaperCard;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;
import forge.view.arcane.CardPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
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
    private final int goldReward;
    private final int echoReward;
    private final Set<PaperCard> selectedCards;
    private final List<SelectableCardPanel> cardPanels;
    private final MainPanel panel;
    private final FLabel lblInfo;
    private final FLabel lblRewards;
    private final JButton btnRevealAll;
    private FOptionPane optionPane;
    private CardZoomUtil zoomUtil;

    /**
     * Create a card reward selection dialog.
     * @param title Dialog title
     * @param cards List of cards to choose from
     * @param minSelections Minimum number of cards to select (0 for optional)
     * @param maxSelections Maximum number of cards to select
     * @param goldReward Amount of gold earned
     * @param echoReward Amount of echoes earned
     */
    public CardRewardDialog(String title, List<PaperCard> cards, int minSelections, int maxSelections, int goldReward, int echoReward) {
        this.cards = cards;
        this.maxSelections = maxSelections;
        this.goldReward = goldReward;
        this.echoReward = echoReward;
        this.selectedCards = new HashSet<>();
        this.cardPanels = new ArrayList<>();

        // Create rewards label
        lblRewards = new FLabel.Builder()
                .text(getRewardsText())
                .fontSize(16)
                .fontStyle(Font.BOLD)
                .fontAlign(SwingConstants.CENTER)
                .build();

        // Create info label
        lblInfo = new FLabel.Builder()
                .text(getInfoText())
                .fontSize(14)
                .fontAlign(SwingConstants.CENTER)
                .build();

        // Create reveal all button
        btnRevealAll = new JButton("Reveal All");
        btnRevealAll.setFont(new Font("Arial", Font.BOLD, 14));
        btnRevealAll.addActionListener(e -> revealAllCards());

        // Create main panel
        panel = new MainPanel();
        panel.add(lblRewards);
        panel.add(lblInfo);
        panel.add(btnRevealAll);

        // Create card panels
        for (PaperCard card : cards) {
            SelectableCardPanel cardPanel = new SelectableCardPanel(card);
            cardPanels.add(cardPanel);
            panel.add(cardPanel);
        }

        // Calculate dialog size (max 4 cards per row)
        int cardsPerRow = Math.min(cards.size(), 4);
        int numRows = (int) Math.ceil(cards.size() / 4.0);

        int dialogWidth = cardsPerRow * (CARD_WIDTH + CARD_SPACING) - CARD_SPACING + 2 * PADDING;
        int dialogHeight = numRows * (CARD_HEIGHT + CARD_SPACING) - CARD_SPACING + 185 + 2 * PADDING; // 185px for labels + button

        Dimension dialogSize = new Dimension(dialogWidth, dialogHeight);
        panel.setPreferredSize(dialogSize);
        panel.setMinimumSize(dialogSize);
    }

    private void revealAllCards() {
        for (SelectableCardPanel cardPanel : cardPanels) {
            cardPanel.flip();
        }
        // Optionally hide the button after revealing all cards
        btnRevealAll.setVisible(false);
        panel.revalidate();
        panel.repaint();
    }

    private String getRewardsText() {
        if (goldReward > 0 || echoReward > 0) {
            return String.format("Victory Rewards: %d Gold, %d Echoes", goldReward, echoReward);
        }
        return "Victory!";
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

        // Setup zoom utility
        zoomUtil = new CardZoomUtil(optionPane);
        zoomUtil.setupZoomOverlay();

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

            // Layout rewards label
            lblRewards.setBounds(PADDING, y, totalWidth - 2 * PADDING, 35);
            y += 35 + 5;

            // Layout info label
            lblInfo.setBounds(PADDING, y, totalWidth - 2 * PADDING, 30);
            y += 30 + 5;

            // Layout reveal all button (centered)
            int buttonWidth = 120;
            int buttonHeight = 35;
            btnRevealAll.setBounds((totalWidth - buttonWidth) / 2, y, buttonWidth, buttonHeight);
            y += buttonHeight + 10;

            // Layout card panels in rows of up to 4 cards
            int cardsPerRow = 4;
            int cardIndex = 0;

            for (int row = 0; cardIndex < cardPanels.size(); row++) {
                // Calculate how many cards in this row
                int cardsInThisRow = Math.min(cardsPerRow, cardPanels.size() - cardIndex);
                int rowWidth = cardsInThisRow * CARD_WIDTH + (cardsInThisRow - 1) * CARD_SPACING;
                int startX = (totalWidth - rowWidth) / 2;

                // Position cards in this row
                int x = startX;
                for (int col = 0; col < cardsInThisRow; col++) {
                    SelectableCardPanel cardPanel = cardPanels.get(cardIndex);
                    cardPanel.setBounds(x, y, CARD_WIDTH, CARD_HEIGHT);
                    x += CARD_WIDTH + CARD_SPACING;
                    cardIndex++;
                }

                // Move to next row
                y += CARD_HEIGHT + CARD_SPACING;
            }
        }
    }

    private class SelectableCardPanel extends SkinnedPanel {
        private final PaperCard card;
        private final CardPicturePanel cardPicture;
        private boolean selected;
        private boolean faceDown;
        private boolean animating;
        private double scaleX;
        private Timer animationTimer;

        private SelectableCardPanel(PaperCard card) {
            super(null);
            this.card = card;
            this.selected = false;
            this.faceDown = true; // Start face-down
            this.animating = false;
            this.scaleX = 1.0;
            this.cardPicture = new CardPicturePanel();

            setOpaque(false);
            setLayout(null); // Manual layout

            // Set the card to display (start with card back)
            updateCardDisplay();
            cardPicture.setOpaque(false);
            add(cardPicture);

            // Add mouse listener for revealing and selection
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (faceDown) {
                        // First click: flip the card face-up
                        flip();
                    } else {
                        // Subsequent clicks: toggle selection
                        toggleCardSelection(SelectableCardPanel.this);
                    }
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

            // Add mouse wheel listener for card zoom (only when revealed)
            addMouseWheelListener(e -> {
                if (!faceDown && e.getWheelRotation() < 0 && CardRewardDialog.this.zoomUtil != null) { // Scroll up to zoom (only if face-up)
                    CardRewardDialog.this.zoomUtil.showZoom(card);
                }
            });

            // Set tooltip with card name (but don't reveal card name when face-down)
            updateTooltip();
        }

        private void updateCardDisplay() {
            if (faceDown) {
                // Show card back
                BufferedImage cardBack = ImageCache.getOriginalImage(
                    ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), true, null);
                cardPicture.setItem(cardBack);
            } else {
                // Show actual card
                cardPicture.setItem(card);
            }
        }

        private void updateTooltip() {
            setToolTipText(faceDown ? "???" : card.getName());
        }

        public void flip() {
            if (animating || !faceDown) {
                return; // Already revealed or animating
            }

            animating = true;
            final int animationDuration = 300; // milliseconds
            final int framesPerSecond = 60;
            final int frameDelay = 1000 / framesPerSecond;
            final int totalFrames = animationDuration / frameDelay;
            final double scaleStep = 2.0 / totalFrames; // Scale from 1.0 to -1.0 to 1.0

            final int[] currentFrame = {0};
            final boolean[] imageFlipped = {false};

            animationTimer = new Timer(frameDelay, e -> {
                currentFrame[0]++;

                // Calculate scale (1.0 -> 0.0 -> 1.0)
                if (currentFrame[0] <= totalFrames / 2) {
                    // First half: shrink from 1.0 to 0.0
                    scaleX = 1.0 - (currentFrame[0] * scaleStep);
                } else {
                    // Second half: expand from 0.0 to 1.0
                    scaleX = (currentFrame[0] - totalFrames / 2) * scaleStep;
                }

                // Flip the card image at the middle of the animation
                if (!imageFlipped[0] && currentFrame[0] >= totalFrames / 2) {
                    faceDown = false;
                    updateCardDisplay();
                    updateTooltip();
                    imageFlipped[0] = true;
                }

                repaint();

                // End animation
                if (currentFrame[0] >= totalFrames) {
                    animationTimer.stop();
                    animating = false;
                    scaleX = 1.0;
                    repaint();
                }
            });

            animationTimer.start();
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
            final Graphics2D g2d = (Graphics2D) g;
            final int width = getWidth();
            final int height = getHeight();

            // Apply horizontal scale transformation if animating
            if (animating && scaleX != 1.0) {
                // Save the original transform
                AffineTransform originalTransform = g2d.getTransform();

                // Create scale transform centered on the card
                AffineTransform scaleTransform = new AffineTransform();
                scaleTransform.translate(width / 2.0, 0); // Move origin to center
                scaleTransform.scale(Math.max(0.01, scaleX), 1.0); // Scale horizontally (min 0.01 to avoid zero)
                scaleTransform.translate(-width / 2.0, 0); // Move origin back

                g2d.transform(scaleTransform);
                super.paint(g);
                g2d.setTransform(originalTransform);
            } else {
                super.paint(g);
            }

            // Then draw selection indicators ON TOP of everything (only if not animating)
            if (selected && !animating) {
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
