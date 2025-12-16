package forge.screens.home.rogue;

import forge.gamemodes.rogue.RogueDeck;
import forge.gui.CardPicturePanel;
import forge.gui.util.CardZoomUtil;
import forge.item.PaperCard;
import forge.toolbox.FSkin.SkinnedPanel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Panel for displaying a commander card in the start view.
 * Shows the commander card and allows single selection.
 */
public class CommanderCardPanel extends SkinnedPanel {
    private final RogueDeck commander;
    private final PaperCard commanderCard;
    private final CardPicturePanel cardPicture;
    private CardZoomUtil zoomUtil; // Not final - can be set after construction
    private boolean selected;
    private Consumer<CommanderCardPanel> selectionCallback;

    public CommanderCardPanel(RogueDeck commander, CardZoomUtil zoomUtil) {
        super(null);
        this.commander = commander;
        // Get the commander card from the start deck
        this.commanderCard = commander.getStartDeck().getCommanders().get(0);
        this.zoomUtil = zoomUtil;
        this.selected = false;
        this.cardPicture = new CardPicturePanel();

        setOpaque(false);
        setLayout(null); // Manual layout

        // Display commander card
        cardPicture.setItem(commanderCard);
        cardPicture.setOpaque(false);
        add(cardPicture);

        // Add mouse listener for selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectionCallback != null) {
                    selectionCallback.accept(CommanderCardPanel.this);
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

        // Add mouse wheel listener for card zoom
        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0 && zoomUtil != null) { // Scroll up to zoom
                zoomUtil.showZoom(commanderCard);
            }
        });

        // Set tooltip with commander name
        setToolTipText(commander.getName());
    }

    public void setSelectionCallback(Consumer<CommanderCardPanel> callback) {
        this.selectionCallback = callback;
    }

    public void setZoomUtil(CardZoomUtil zoomUtil) {
        // Allow updating zoom utility after panel creation (for initialization timing issues)
        this.zoomUtil = zoomUtil;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    public RogueDeck getCommander() {
        return commander;
    }

    @Override
    public void doLayout() {
        // Make card picture fill the panel
        cardPicture.setBounds(0, 0, getWidth(), getHeight());
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        // Draw selection indicators if selected
        if (selected) {
            Graphics2D g2d = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();

            // Draw thick green border
            g2d.setColor(new Color(0, 255, 0, 200));
            g2d.setStroke(new BasicStroke(6));
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
}
