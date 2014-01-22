/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
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
package forge.gui.toolbox;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.border.Border;

import forge.gui.toolbox.FSkin.SkinColor;

/** 
 * An extension of JScrollPane that can be used as a panel and supports using arrow buttons to scroll instead of scrollbars
 *
 */
@SuppressWarnings("serial")
public class FScrollPanel extends FScrollPane {
    private static final SkinColor arrowColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);
    private final boolean useArrowButtons;
    private final ArrowButton[] arrowButtons = new ArrowButton[4];
    private JPanel innerPanel;

    /**
     * An extension of JScrollPane that can be used as a panel and supports using arrow buttons to scroll instead of scrollbars
     * This constructor assumes no layout, assumes using scrollbars to scroll, and "as needed" for horizontal and vertical scroll policies.
     * 
     */
    public FScrollPanel() {
        this(null);
    }

    /**
     * An extension of JScrollPane that can be used as a panel and supports using arrow buttons to scroll instead of scrollbars
     * This constructor assumes using scrollbars to scroll and "as needed" for horizontal and vertical scroll policies.
     * 
     * @param layout &emsp; Layout for panel.
     */
    public FScrollPanel(final LayoutManager layout) {
        this(layout, false);
    }

    /**
     * An extension of JScrollPane that can be used as a panel and supports using arrow buttons to scroll instead of scrollbars
     * This constructor assumes "as needed" for horizontal and vertical scroll policies.
     * 
     * @param layout &emsp; Layout for panel.
     * @param useArrowButtons &emsp; True to use arrow buttons to scroll, false to use scrollbars
     */
    public FScrollPanel(final LayoutManager layout, boolean useArrowButtons) {
        this(layout, useArrowButtons, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * An extension of JScrollPane that can be used as a panel and supports using arrow buttons to scroll instead of scrollbars
     * 
     * @param layout &emsp; Layout for panel.
     * @param useArrowButtons0 &emsp; True to use arrow buttons to scroll, false to use scrollbars
     * @param vertical0 &emsp; Vertical scroll bar policy
     * @param horizontal0 &emsp; Horizontal scroll bar policy
     */
    public FScrollPanel(final LayoutManager layout, boolean useArrowButtons0, final int vertical0, final int horizontal0) {
        this(new JPanel(layout), useArrowButtons0, vertical0, horizontal0);
        innerPanel = (JPanel)getViewport().getView();
        innerPanel.setOpaque(false);
    }

    /**
     * An extension of JScrollPane that can be used as a panel and supports using arrow buttons to scroll instead of scrollbars
     * 
     * @param view &emsp; Scrollable view component.
     * @param useArrowButtons0 &emsp; True to use arrow buttons to scroll, false to use scrollbars
     */
    public FScrollPanel(final Component view, boolean useArrowButtons0) {
        this(view, useArrowButtons0, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * An extension of JScrollPane that can be used as a panel and supports using arrow buttons to scroll instead of scrollbars
     * 
     * @param view &emsp; Scrollable view component.
     * @param useArrowButtons0 &emsp; True to use arrow buttons to scroll, false to use scrollbars
     * @param vertical0 &emsp; Vertical scroll bar policy
     * @param horizontal0 &emsp; Horizontal scroll bar policy
     */
    public FScrollPanel(final Component view, boolean useArrowButtons0, final int vertical0, final int horizontal0) {
        super(view, vertical0, horizontal0);

        useArrowButtons = useArrowButtons0;
        setBorder((Border)null);
        if (useArrowButtons) {
            //ensure scrollbar aren't shown
            getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
            getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (useArrowButtons) {
            //determine which buttons should be visible
            boolean[] visible = new boolean[] { false, false, false, false };
            final JScrollBar horzScrollBar = this.getHorizontalScrollBar();
            if (horzScrollBar.isVisible()) { //NOTE: scrollbar wouldn't actually be visible since size set to 0 to hide it
                visible[0] = horzScrollBar.getValue() > 0;
                visible[1] = horzScrollBar.getValue() < horzScrollBar.getMaximum()  - horzScrollBar.getModel().getExtent();
            }
            final JScrollBar vertScrollBar = this.getVerticalScrollBar();
            if (vertScrollBar.isVisible()) {
                visible[2] = vertScrollBar.getValue() > 0;
                visible[3] = vertScrollBar.getValue() < vertScrollBar.getMaximum() - vertScrollBar.getModel().getExtent();
            }
            for (int dir = 0; dir < 4; dir++) {
                updateArrowButton(dir, visible);
            }
        }
    }

    private void updateArrowButton(int dir, boolean[] visible) {
        ArrowButton arrowButton = arrowButtons[dir];
        if (!visible[dir]) {
            if (arrowButton != null) {
                arrowButton.setVisible(false);
            }
            return;
        }

        //determine bounds of button
        int x, y, w, h;
        final int panelWidth = getWidth();
        final int panelHeight = getHeight();
        final int arrowButtonSize = 18;
        final int cornerSize = arrowButtonSize - 1; //make borders line up

        if (dir < 2) { //if button for horizontal scrolling
            y = 0;
            h = panelHeight;
            if (visible[2]) {
                y += cornerSize;
                h -= cornerSize;
            }
            if (visible[3]) {
                h -= cornerSize;
            }
            x = (dir == 0 ? 0 : panelWidth - arrowButtonSize);
            w = arrowButtonSize;
        }
        else { //if button for vertical scrolling
            x = 0;
            w = panelWidth;
            if (visible[0]) {
                x += cornerSize;
                w -= cornerSize;
            }
            if (visible[1]) {
                w -= cornerSize;
            }
            y = (dir == 2 ? 0 : panelHeight - arrowButtonSize);
            h = arrowButtonSize;
        }

        if (arrowButton == null) {
            switch (dir) {
                case 0:
                    arrowButton = new LeftArrowButton(getHorizontalScrollBar());
                    break;
                case 1:
                    arrowButton = new RightArrowButton(getHorizontalScrollBar());
                    break;
                case 2:
                    arrowButton = new TopArrowButton(getVerticalScrollBar());
                    break;
                default:
                    arrowButton = new BottomArrowButton(getVerticalScrollBar());
                    break;
            }
            arrowButtons[dir] = arrowButton;
        }
        //absolutely position button in front of scroll panel
        arrowButton.setSize(w, h);
        FAbsolutePositioner.SINGLETON_INSTANCE.show(arrowButton, this, x, y);
    }

    private abstract class ArrowButton extends FLabel {
        protected final int arrowSize = 6;
        private final JScrollBar scrollBar;
        private final int incrementDirection;

        protected ArrowButton(final JScrollBar scrollBar0, final int incrementDirection0) {
            super(new FLabel.ButtonBuilder());
            scrollBar = scrollBar0;
            incrementDirection = incrementDirection0;
            timer.setInitialDelay(500); //wait half a second after mouse down before starting timer
        }

        @Override
        protected void setPressed(boolean pressed0) {
            super.setPressed(pressed0);
            if (pressed0) {
                scrollBar.setValue(scrollBar.getValue() + scrollBar.getUnitIncrement() * incrementDirection);
                timer.start();
            }
            else {
                timer.stop();
            }
        }

        @Override
        protected void paintContent(final Graphics2D g, int w, int h, final boolean paintPressedState) {
            FSkin.setGraphicsColor(g, arrowColor);
            drawArrow(g);
        }

        protected abstract void drawArrow(final Graphics g);

        //timer to continue scrollling while mouse remains down
        final Timer timer = new Timer(50, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isVisible()) {
                    //ensure timer stops if button hidden from scrolling to beginning/end (based on incrementDirection)
                    ((Timer)e.getSource()).stop();
                    return;
                }
                scrollBar.setValue(scrollBar.getValue() + scrollBar.getUnitIncrement() * incrementDirection);
            }
        });
    }

    private class LeftArrowButton extends ArrowButton {
        public LeftArrowButton(final JScrollBar horzScrollbar) {
            super(horzScrollbar, -1);
        }

        @Override
        protected void drawArrow(final Graphics g) {
            int x = (getWidth() - arrowSize) / 2;
            int y2 = getHeight() / 2;
            int y1 = y2 - 1;
            for (int i = 0; i < arrowSize; i++) {
                g.drawLine(x, y1, x, y2);
                x++;
                y1--;
                y2++;
            }
        }
    }

    private class RightArrowButton extends ArrowButton {
        public RightArrowButton(final JScrollBar horzScrollbar) {
            super(horzScrollbar, 1);
        }

        @Override
        protected void drawArrow(final Graphics g) {
            int x = (getWidth() + arrowSize) / 2;
            int y2 = getHeight() / 2;
            int y1 = y2 - 1;
            for (int i = 0; i < arrowSize; i++) {
                g.drawLine(x, y1, x, y2);
                x--;
                y1--;
                y2++;
            }
        }
    }

    private class TopArrowButton extends ArrowButton {
        public TopArrowButton(final JScrollBar vertScrollbar) {
            super(vertScrollbar, -1);
        }

        @Override
        protected void drawArrow(final Graphics g) {
            int x2 = getWidth() / 2;
            int x1 = x2 - 1;
            int y = (getHeight() - arrowSize) / 2;
            for (int i = 0; i < arrowSize; i++) {
                g.drawLine(x1, y, x2, y);
                x1--;
                x2++;
                y++;
            }
        }
    }

    private class BottomArrowButton extends ArrowButton {
        public BottomArrowButton(final JScrollBar vertScrollbar) {
            super(vertScrollbar, 1);
        }

        @Override
        protected void drawArrow(final Graphics g) {
            int x2 = getWidth() / 2;
            int x1 = x2 - 1;
            int y = (getHeight() + arrowSize) / 2;
            for (int i = 0; i < arrowSize; i++) {
                g.drawLine(x1, y, x2, y);
                x1--;
                x2++;
                y--;
            }
        }
    }

    //relay certain methods to the inner panel if it has been initialized
    @Override
    public Component add(Component comp) {
        if (innerPanel != null) {
            return innerPanel.add(comp);
        }
        return super.add(comp);
    }

    @Override
    public void add(PopupMenu popup) {
        if (innerPanel != null) {
            innerPanel.add(popup);
            return;
        }
        super.add(popup);
    }

    @Override
    public void add(Component comp, Object constraints) {
        if (innerPanel != null) {
            innerPanel.add(comp, constraints);
            return;
        }
        super.add(comp, constraints);
    }

    @Override
    public Component add(Component comp, int index) {
        if (innerPanel != null) {
            return innerPanel.add(comp, index);
        }
        return super.add(comp, index);
    }

    @Override
    public void add(Component comp, Object constraints, int index) {
        if (innerPanel != null) {
            innerPanel.add(comp, constraints, index);
            return;
        }
        super.add(comp, constraints, index);
    }

    @Override
    public Component add(String name, Component comp) {
        if (innerPanel != null) {
            return innerPanel.add(name, comp);
        }
        return super.add(name, comp);
    }
}
