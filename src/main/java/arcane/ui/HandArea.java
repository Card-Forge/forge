package arcane.ui;

import java.awt.Frame;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;

import arcane.ui.util.CardPanelMouseListener;

/**
 * <p>
 * HandArea class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class HandArea extends CardArea {
    /** Constant <code>serialVersionUID=7488132628637407745L</code>. */
    private static final long serialVersionUID = 7488132628637407745L;

    /**
     * <p>
     * Constructor for HandArea.
     * </p>
     * 
     * @param scrollPane
     *            a {@link javax.swing.JScrollPane} object.
     * @param frame
     *            a {@link java.awt.Frame} object.
     */
    public HandArea(final JScrollPane scrollPane, final Frame frame) {
        super(scrollPane);

        this.setDragEnabled(true);
        this.setVertical(true);

        this.addCardPanelMouseListener(new CardPanelMouseListener() {
            @Override
            public void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
            }

            @Override
            public void mouseOver(final CardPanel panel, final MouseEvent evt) {
            }

            @Override
            public void mouseOut(final CardPanel panel, final MouseEvent evt) {
            }

            @Override
            public void mouseMiddleClicked(final CardPanel panel, final MouseEvent evt) {
            }

            @Override
            public void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {

            }

            @Override
            public void mouseDragged(final CardPanel dragPanel, final int dragOffsetX, final int dragOffsetY,
                    final MouseEvent evt) {
            }

            @Override
            public void mouseDragStart(final CardPanel dragPanel, final MouseEvent evt) {
            }

            @Override
            public void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
            }
        });
    }
}
