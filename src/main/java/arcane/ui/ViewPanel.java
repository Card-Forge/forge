package arcane.ui;

import javax.swing.*;
import java.awt.*;

/**
 * <p>ViewPanel class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ViewPanel extends JPanel {
    /** Constant <code>serialVersionUID=7016597023142963068L</code> */
    private static final long serialVersionUID = 7016597023142963068L;

    /**
     * <p>doLayout.</p>
     *
     * @since 1.0.15
     */
    public void doLayout() {
        if (getComponentCount() == 0) return;
        CardPanel panel = (CardPanel) getComponent(0);
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int srcWidth = viewWidth;
        int srcHeight = Math.round(viewWidth * CardPanel.ASPECT_RATIO);
        int targetWidth = Math.round(viewHeight * (srcWidth / (float) srcHeight));
        int targetHeight;
        if (targetWidth > viewWidth) {
            targetHeight = Math.round(viewWidth * (srcHeight / (float) srcWidth));
            targetWidth = viewWidth;
        } else
            targetHeight = viewHeight;
        int x = viewWidth / 2 - targetWidth / 2;
        int y = viewHeight / 2 - targetHeight / 2;
        panel.setCardBounds(x, y, targetWidth, targetHeight);
    }

    /**
     * <p>setCardPanel.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     */
    public void setCardPanel(CardPanel panel) {
        //CardPanel newPanel = new CardPanel(panel.gameCard);
        //newPanel.setImage(panel);
        removeAll();
        add(panel, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
    }
}
