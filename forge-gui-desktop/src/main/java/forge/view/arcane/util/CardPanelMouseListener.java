/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view.arcane.util;


import forge.view.arcane.CardPanel;

import java.awt.event.MouseEvent;

/**
 * <p>CardPanelMouseListener interface.</p>
 *
 * @author Forge
 * @version $Id: CardPanelMouseListener.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public interface CardPanelMouseListener {
    /**
     * <p>mouseOver.</p>
     *
     * @param panel a {@link forge.view.arcane.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseOver(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseOut.</p>
     *
     * @param panel a {@link forge.view.arcane.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseOut(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseLeftClicked.</p>
     *
     * @param panel a {@link forge.view.arcane.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseLeftClicked(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseRightClicked.</p>
     *
     * @param panel a {@link forge.view.arcane.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseRightClicked(CardPanel panel, MouseEvent evt);

    /**
     * <p>mouseDragStart.</p>
     *
     * @param dragPanel a {@link forge.view.arcane.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseDragStart(CardPanel dragPanel, MouseEvent evt);

    /**
     * <p>mouseDragged.</p>
     *
     * @param dragPanel a {@link forge.view.arcane.CardPanel} object.
     * @param dragOffsetX a int.
     * @param dragOffsetY a int.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseDragged(CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt);

    /**
     * <p>mouseDragEnd.</p>
     *
     * @param dragPanel a {@link forge.view.arcane.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    void mouseDragEnd(CardPanel dragPanel, MouseEvent evt);
}
