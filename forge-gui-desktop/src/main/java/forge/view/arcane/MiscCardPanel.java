package forge.view.arcane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.view.arcane.util.OutlinedLabel;


public class MiscCardPanel extends SkinnedPanel {
    private static final float ROT_CENTER_TO_TOP_CORNER = 1.0295630140987000315797369464196f;
    private static final float ROT_CENTER_TO_BOTTOM_CORNER = 0.7071067811865475244008443621048f;

    private final CMatchUI matchUI;
    private final String label;
    private final FLabel image;

    private OutlinedLabel titleText;
    private int cardXOffset, cardYOffset, cardWidth, cardHeight;

    public MiscCardPanel(final CMatchUI matchUI, final String label, final SkinImage image) {
        this.matchUI = matchUI;
        this.label = label;
        this.image = new FLabel.Builder().icon(image).build();

        setBackground(Color.black);
        setOpaque(true);

        add(this.image);
        createCardNameOverlay();
    }

    public CMatchUI getMatchUI() {
        return matchUI;
    }

    private void createCardNameOverlay() {
        titleText = new OutlinedLabel();
        titleText.setFont(getFont().deriveFont(Font.BOLD, 13f));
        titleText.setForeground(Color.white);
        titleText.setGlow(Color.black);
        titleText.setWrap(true);
        titleText.setText(label);
        add(titleText);
    }

    @Override
    public final void paint(final Graphics g) {
        if (!isValid()) {
            super.validate();
        }
        super.paint(g);
    }

    @Override
    public final void doLayout() {
        final Point imgPos = new Point(cardXOffset, cardYOffset);
        final Dimension imgSize = new Dimension(cardWidth, cardHeight);

        image.setLocation(imgPos);
        image.setSize(imgSize);

        displayCardNameOverlay(showCardNameOverlay(), imgSize, imgPos);
    }

    private void displayCardNameOverlay(final boolean isVisible, final Dimension imgSize, final Point imgPos) {
        if (isVisible) {
            final int titleX = Math.round(imgSize.width * (24f / 480));
            final int titleY = Math.round(imgSize.height * (54f / 640)) - 15;
            final int titleH = Math.round(imgSize.height * (360f / 640));
            titleText.setBounds(imgPos.x + titleX, imgPos.y + titleY + 2, imgSize.width - 2 * titleX, titleH - titleY);
        }
        titleText.setVisible(isVisible);
    }

    @Override
    public final String toString() {
        return label;
    }

    public final void setCardBounds(final int x, final int y, int width, int height) {
        cardWidth = width;
        cardHeight = height;
        final int rotCenterX = Math.round(width / 2f);
        final int rotCenterY = height - rotCenterX;
        final int rotCenterToTopCorner = Math.round(width * ROT_CENTER_TO_TOP_CORNER);
        final int rotCenterToBottomCorner = Math.round(width * ROT_CENTER_TO_BOTTOM_CORNER);
        final int xOffset = rotCenterX - rotCenterToBottomCorner;
        final int yOffset = rotCenterY - rotCenterToTopCorner;
        cardXOffset = -xOffset;
        cardYOffset = -yOffset;
        width = -xOffset + rotCenterX + rotCenterToTopCorner;
        height = -yOffset + rotCenterY + rotCenterToBottomCorner;
        setBounds(x + xOffset, y + yOffset, width, height);
    }

    @Override
    public final void repaint() {
        final Rectangle b = getBounds();
        final JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null) {
            return;
        }
        final Point p = SwingUtilities.convertPoint(getParent(), b.x, b.y, rootPane);
        rootPane.repaint(p.x, p.y, b.width, b.height);
    }

    private static boolean isPreferenceEnabled(final FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
    }

    private boolean isShowingOverlays() {
        return isPreferenceEnabled(FPref.UI_SHOW_CARD_OVERLAYS);
    }

    private boolean showCardNameOverlay() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_NAME);
    }

    public void repaintOverlays() {
        repaint();
        doLayout();
    }
}
