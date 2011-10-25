package arcane.ui.util;

import arcane.ui.CardPanel;

import java.awt.event.MouseEvent;

/**
 * <p>CardPanelMouseListener interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
public interface CardPanelMouseListener {
    /**
     * <p>mouseOver.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseOver(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseOut.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseOut(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseLeftClicked.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseLeftClicked(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseMiddleClicked.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseMiddleClicked(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseRightClicked.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseRightClicked(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseDragStart.</p>
     *
     * @param dragPanel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseDragStart(CardPanel dragPanel, MouseEvent evt);

    /**
     * <p>mouseDragged.</p>
     *
     * @param dragPanel a {@link arcane.ui.CardPanel} object.
     * @param dragOffsetX a int.
     * @param dragOffsetY a int.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseDragged(CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt);

    /**
     * <p>mouseDragEnd.</p>
     *
     * @param dragPanel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseDragEnd(CardPanel dragPanel, MouseEvent evt);
}
