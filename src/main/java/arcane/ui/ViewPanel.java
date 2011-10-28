package arcane.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

/**
 * <p>
 * ViewPanel class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ViewPanel extends JPanel {
    /** Constant <code>serialVersionUID=7016597023142963068L</code>. */
    private static final long serialVersionUID = 7016597023142963068L;

    /**
     * <p>
     * doLayout.
     * </p>
     * 
     * @since 1.0.15
     */
    @Override
    public final void doLayout() {
        if (this.getComponentCount() == 0) {
            return;
        }
        final CardPanel panel = (CardPanel) this.getComponent(0);
        final int viewWidth = this.getWidth();
        final int viewHeight = this.getHeight();
        final int srcWidth = viewWidth;
        final int srcHeight = Math.round(viewWidth * CardPanel.ASPECT_RATIO);
        int targetWidth = Math.round(viewHeight * (srcWidth / (float) srcHeight));
        int targetHeight;
        if (targetWidth > viewWidth) {
            targetHeight = Math.round(viewWidth * (srcHeight / (float) srcWidth));
            targetWidth = viewWidth;
        } else {
            targetHeight = viewHeight;
        }
        final int x = (viewWidth / 2) - (targetWidth / 2);
        final int y = (viewHeight / 2) - (targetHeight / 2);
        panel.setCardBounds(x, y, targetWidth, targetHeight);
    }

    /**
     * <p>
     * setCardPanel.
     * </p>
     * 
     * @param panel
     *            a {@link arcane.ui.CardPanel} object.
     */
    public final void setCardPanel(final CardPanel panel) {
        // CardPanel newPanel = new CardPanel(panel.gameCard);
        // newPanel.setImage(panel);
        this.removeAll();
        this.add(panel, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
    }
}
