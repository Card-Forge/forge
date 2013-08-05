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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import forge.gui.framework.ILocalRepaint;

/** 
 * An extension of JScrollPane that can be used as a panel and supports using arrow buttons to scroll instead of scrollbars
 *
 */
@SuppressWarnings("serial")
public class FScrollPanel extends JScrollPane {
    private final ArrowButton[] arrowButtons = new ArrowButton[4];
    private final JPanel innerPanel;
    private final boolean useArrowButtons;
    
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
     * @param useArrowButtons &emsp; True to use arrow buttons to scroll, false to use scrollbars
     * @param vertical0 &emsp; Vertical scroll bar policy
     * @param horizontal0 &emsp; Horizontal scroll bar policy
     */
    public FScrollPanel(final LayoutManager layout, boolean useArrowButtons0, final int vertical0, final int horizontal0) {
        super(new JPanel(layout), vertical0, horizontal0);

        innerPanel = (JPanel)getViewport().getView();
        useArrowButtons = useArrowButtons0;

        getViewport().setOpaque(false);
        innerPanel.setOpaque(false);
        setOpaque(false);
        setBorder(null);
        getHorizontalScrollBar().setUnitIncrement(16);
        getVerticalScrollBar().setUnitIncrement(16);
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
    
    private abstract class ArrowButton extends JLabel implements ILocalRepaint {
        private final Color clrFore = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        private final Color clrBack = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
        private final Color d50 = FSkin.stepColor(clrBack, -50);
        private final Color d10 = FSkin.stepColor(clrBack, -10);
        private final Color l10 = FSkin.stepColor(clrBack, 10);
        private final Color l20 = FSkin.stepColor(clrBack, 20);
        private final AlphaComposite alphaDefault = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f);
        private final AlphaComposite alphaHovered = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f);
        protected final int arrowSize = 6;
        private final JScrollBar scrollBar;
        private final int incrementDirection;
        private boolean hovered;

        protected ArrowButton(final JScrollBar scrollBar0, final int incrementDirection0) {
            super("");
            scrollBar = scrollBar0;
            incrementDirection = incrementDirection0;
            timer.setInitialDelay(500); //wait half a second after mouse down before starting timer
            addMouseListener(madEvents);
        }
        
        @Override
        public void repaintSelf() {
            final Dimension d = getSize();
            repaint(0, 0, d.width, d.height);
        }
        
        @Override
        public void paintComponent(final Graphics g) {
            Graphics2D g2d = (Graphics2D)g;

            int w = getWidth();
            int h = getHeight();

            g.setColor(Color.white); //draw white background before composite so not semi-transparent
            g.fillRect(0, 0, w, h);

            Composite oldComp = g2d.getComposite();
            g2d.setComposite(hovered ? alphaHovered : alphaDefault);

            GradientPaint gradient = new GradientPaint(0, h, d10, 0, 0, l20);
            g2d.setPaint(gradient);
            g.fillRect(0, 0, w, h);

            g.setColor(d50);
            g.drawRect(0, 0, w - 1, h - 1);
            g.setColor(l10);
            g.drawRect(1, 1, w - 3, h - 3);

            g.setColor(clrFore);
            drawArrow(g);

            super.paintComponent(g);

            g2d.setComposite(oldComp);
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

        private final MouseAdapter madEvents = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                scrollBar.setValue(scrollBar.getValue() + scrollBar.getUnitIncrement() * incrementDirection);
                timer.start();
            }
            
            @Override
            public void mouseReleased(final MouseEvent e) {
                timer.stop();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaintSelf();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaintSelf();
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!hovered) {
                    hovered = true;
                    repaintSelf();
                }
            }
        };
    }
    
    private class LeftArrowButton extends ArrowButton {
        public LeftArrowButton(final JScrollBar horzScrollbar) {
            super(horzScrollbar, -1);
        }
        
        @Override
        protected void drawArrow(final Graphics g) {
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int halfSize = arrowSize / 2;
            g.fillPolygon(new int[] { centerX - halfSize, centerX + halfSize, centerX + halfSize },
                    new int[] { centerY, centerY + arrowSize, centerY - arrowSize }, 3);
        }
    }
    
    private class RightArrowButton extends ArrowButton {
        public RightArrowButton(final JScrollBar horzScrollbar) {
            super(horzScrollbar, 1);
        }
        
        @Override
        protected void drawArrow(final Graphics g) {
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int halfSize = arrowSize / 2;
            g.fillPolygon(new int[] { centerX + halfSize, centerX - halfSize, centerX - halfSize },
                    new int[] { centerY, centerY + arrowSize, centerY - arrowSize }, 3);
        }
    }
    
    private class TopArrowButton extends ArrowButton {
        public TopArrowButton(final JScrollBar vertScrollbar) {
            super(vertScrollbar, -1);
        }
        
        @Override
        protected void drawArrow(final Graphics g) {
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int halfSize = arrowSize / 2;
            g.fillPolygon(new int[] { centerX, centerX + arrowSize, centerX - arrowSize },
                    new int[] { centerY - halfSize, centerY + halfSize, centerY + halfSize }, 3);
        }
    }
    
    private class BottomArrowButton extends ArrowButton {
        public BottomArrowButton(final JScrollBar vertScrollbar) {
            super(vertScrollbar, 1);
        }
        
        @Override
        protected void drawArrow(final Graphics g) {
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int halfSize = arrowSize / 2;
            g.fillPolygon(new int[] { centerX, centerX + arrowSize, centerX - arrowSize },
                    new int[] { centerY + halfSize, centerY - halfSize, centerY - halfSize }, 3);
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
