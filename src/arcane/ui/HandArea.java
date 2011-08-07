package arcane.ui;

import arcane.ui.util.CardPanelMouseListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * <p>HandArea class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class HandArea extends CardArea {
    /** Constant <code>serialVersionUID=7488132628637407745L</code> */
    private static final long serialVersionUID = 7488132628637407745L;

    /**
     * <p>Constructor for HandArea.</p>
     *
     * @param scrollPane a {@link javax.swing.JScrollPane} object.
     * @param frame a {@link java.awt.Frame} object.
     */
    public HandArea(JScrollPane scrollPane, final Frame frame) {
        super(scrollPane);

        setDragEnabled(true);
        setVertical(true);

        addCardPanelMouseListener(new CardPanelMouseListener() {
            public void mouseRightClicked(CardPanel panel, MouseEvent evt) {
            }

            public void mouseOver(CardPanel panel, MouseEvent evt) {
            }

            public void mouseOut(CardPanel panel, MouseEvent evt) {
            }

            public void mouseMiddleClicked(CardPanel panel, MouseEvent evt) {
            }

            public void mouseLeftClicked(CardPanel panel, MouseEvent evt) {

            }

            public void mouseDragged(CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt) {
            }

            public void mouseDragStart(CardPanel dragPanel, MouseEvent evt) {
            }

            public void mouseDragEnd(CardPanel dragPanel, MouseEvent evt) {
            }
        });
    }
}
