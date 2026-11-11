package forge.screens.home.rogue;

import forge.item.PaperCard;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTextArea;
import forge.toolbox.FTextPane;
import forge.view.arcane.CardPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.text.StyleConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Result panel for node effects that may involve card lists.
 * Shows a result message and optional named card sections with card images.
 */
public class NodeResultPanel extends SkinnedPanel {

    private static final int CARD_WIDTH = 180;
    private static final int CARD_HEIGHT = Math.round(CARD_WIDTH * CardPanel.ASPECT_RATIO);
    private static final int CARD_SPACING = 12;
    private static final int MAX_CARDS_PER_ROW = 4;
    private static final int DEFAULT_MIN_WIDTH = 650;
    private static final int PANEL_INSET = 10;
    private static final int TEXT_WRAP_SAFETY_MARGIN = 20;
    private static final int TEXT_RIGHT_PADDING = 8;

    private final List<ReadOnlyCardPanel> allCardPanels = new ArrayList<>();
    private final RogueUIHelper.TypewriterText typewriterText;
    private CardUtil zoomUtil;

    public record CardSection(String label, String text, List<PaperCard> cards) {
        public CardSection(String label, List<PaperCard> cards) {
            this(label, null, cards);
        }

        public CardSection(String label, String text) {
            this(label, text, List.of());
        }
    }

    public enum MessageAlignment {
        LEFT(StyleConstants.ALIGN_LEFT),
        CENTER(StyleConstants.ALIGN_CENTER);

        private final int styleConstant;

        MessageAlignment(int styleConstant) {
            this.styleConstant = styleConstant;
        }
    }

    public NodeResultPanel(String message, List<CardSection> sections) {
        this(message, sections, DEFAULT_MIN_WIDTH, 0, MessageAlignment.LEFT, false);
    }

    public NodeResultPanel(String message, List<CardSection> sections, int minWidth, int minHeight,
                           MessageAlignment messageAlignment, boolean typewriterMessage) {
        super(new MigLayout("insets 10, gap 0, wrap", "[grow, center]", ""));
        setOpaque(false);

        boolean hasCards = sections.stream().anyMatch(
                s -> s.cards() != null && !s.cards().isEmpty());
        int maxCardsInRow = sections.stream()
                .filter(s -> s.cards() != null)
                .mapToInt(s -> Math.min(s.cards().size(), MAX_CARDS_PER_ROW))
                .max().orElse(0);
        int desiredWidth = hasCards
                ? Math.max(DEFAULT_MIN_WIDTH, maxCardsInRow * (CARD_WIDTH + CARD_SPACING) - CARD_SPACING + 40)
                : DEFAULT_MIN_WIDTH;
        desiredWidth = Math.max(desiredWidth, minWidth);
        int textWidth = Math.max(1, desiredWidth - (PANEL_INSET * 2) - TEXT_WRAP_SAFETY_MARGIN);

        // Result text
        Component txtMessage;
        if (typewriterMessage && messageAlignment == MessageAlignment.LEFT) {
            FTextArea textArea = createWrappedTextArea(message, 14f, textWidth);
            typewriterText = RogueUIHelper.prepareTypewriterText(
                    textArea, this, List.of(message == null ? "" : message), textWidth);
            txtMessage = textArea;
        } else {
            typewriterText = null;
            txtMessage = createMessageComponent(message, 14f, textWidth, messageAlignment);
        }
        add(txtMessage, "w 100%!, ax center, gap 0 0 0 10px, wrap");

        int desiredHeight = PANEL_INSET * 2 + txtMessage.getPreferredSize().height + 10;

        // Result sections
        for (CardSection section : sections) {
            if (!hasContent(section)) continue;

            if (hasLabel(section)) {
                FLabel lblSection = new FLabel.Builder()
                        .text(section.label())
                        .fontSize(13)
                        .fontStyle(Font.BOLD)
                        .build();
                add(lblSection, "w 100%!, h 22px!, gap 5px 0 5px 3px, wrap");
                desiredHeight += 22 + 10;
            }

            if (section.text() != null && !section.text().isBlank()) {
                FTextArea txtSection = createWrappedTextArea(section.text(), 13f, textWidth);
                add(txtSection, "w 100%!, ax center, gap 0 0 0 5px, wrap");
                desiredHeight += txtSection.getPreferredSize().height + 5;
            }

            if (section.cards() != null && !section.cards().isEmpty()) {
                // Card row panel
                SkinnedPanel cardRow = new SkinnedPanel(
                        new MigLayout("insets 0, gap " + CARD_SPACING + "px, wrap " + MAX_CARDS_PER_ROW, "", ""));
                cardRow.setOpaque(false);

                for (PaperCard card : section.cards()) {
                    ReadOnlyCardPanel cardPanel = new ReadOnlyCardPanel(card, () -> zoomUtil);
                    allCardPanels.add(cardPanel);
                    cardRow.add(cardPanel, "w " + CARD_WIDTH + "!, h " + CARD_HEIGHT + "!");
                }

                add(cardRow, "ax center, wrap");

                int cardsPerRow = Math.min(section.cards().size(), MAX_CARDS_PER_ROW);
                int rowCount = (int) Math.ceil(section.cards().size() / (double) cardsPerRow);
                desiredHeight += rowCount * CARD_HEIGHT
                        + Math.max(0, rowCount - 1) * CARD_SPACING
                        + 10;
            }
        }
        desiredHeight = Math.max(desiredHeight, minHeight);
        Dimension size = new Dimension(desiredWidth, desiredHeight);
        setPreferredSize(size);
        setMinimumSize(size);
    }

    public void startTypewriter() {
        if (typewriterText != null) {
            typewriterText.start();
        }
    }

    public void stopTypewriter() {
        if (typewriterText != null) {
            typewriterText.stop();
        }
    }

    private static Component createMessageComponent(String text, float fontSize, int width,
                                                    MessageAlignment alignment) {
        if (alignment == MessageAlignment.CENTER) {
            FTextPane textPane = new FTextPane(text);
            textPane.setFont(textPane.getFont().deriveFont(fontSize));
            textPane.setTextAlignment(alignment.styleConstant);
            textPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, TEXT_RIGHT_PADDING));
            textPane.setSize(width, Short.MAX_VALUE);
            return textPane;
        }

        return createWrappedTextArea(text, fontSize, width);
    }

    private static FTextArea createWrappedTextArea(String text, float fontSize, int width) {
        FTextArea textArea = new FTextArea(text);
        textArea.setFont(textArea.getFont().deriveFont(fontSize));
        textArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, TEXT_RIGHT_PADDING));
        textArea.setSize(width, Short.MAX_VALUE);
        return textArea;
    }

    private static boolean hasLabel(CardSection section) {
        return section.label() != null && !section.label().isBlank();
    }

    private static boolean hasContent(CardSection section) {
        return section.text() != null && !section.text().isBlank()
            || section.cards() != null && !section.cards().isEmpty();
    }

    /**
     * Initialize zoom support. Must be called after the parent FOptionPane is created.
     */
    public void initZoom(Window parentWindow) {
        zoomUtil = new CardUtil(parentWindow);
        zoomUtil.setupZoomOverlay();
    }

    /**
     * Read-only card panel — starts face-up, no selection.
     */
    private static class ReadOnlyCardPanel extends SelectableCardPanelBase {

        ReadOnlyCardPanel(PaperCard card, Supplier<CardUtil> zoomSupplier) {
            super(card, zoomSupplier, false, ZoomActivation.MOUSE_WHEEL);
        }

        @Override
        protected void toggleSelection() {
            // No-op: display only
        }
    }
}
