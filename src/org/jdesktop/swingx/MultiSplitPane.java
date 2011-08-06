/*
 * $Id: MultiSplitPane.java,v 1.15 2005/10/26 14:29:54 hansmuller Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import org.jdesktop.swingx.MultiSplitLayout.Divider;
import org.jdesktop.swingx.MultiSplitLayout.Node;

/**
 *
 * <p>
 * All properties in this class are bound: when a properties value
 * is changed, all PropertyChangeListeners are fired.
 * 
 * @author Hans Muller
 */

public class MultiSplitPane extends JPanel {

	private static final long serialVersionUID = 3379001167792141242L;

	private AccessibleContext accessibleContext = null;
    private boolean continuousLayout = true;
    private DividerPainter dividerPainter = new DefaultDividerPainter();

    /**
     * Creates a MultiSplitPane with it's LayoutManager set to 
     * to an empty MultiSplitLayout.
     */
    public MultiSplitPane() {
	super(new MultiSplitLayout());
	InputHandler inputHandler = new InputHandler();
	addMouseListener(inputHandler);
	addMouseMotionListener(inputHandler);
	addKeyListener(inputHandler);
	setFocusable(true);
    }

    /** 
     * A convenience method that returns the layout manager cast 
     * to MutliSplitLayout.
     * 
     * @return this MultiSplitPane's layout manager
     * @see java.awt.Container#getLayout
     * @see #setModel
     */
    public final MultiSplitLayout getMultiSplitLayout() {
	return (MultiSplitLayout)getLayout();
    }

    /** 
     * A convenience method that sets the MultiSplitLayout model.
     * Equivalent to <code>getMultiSplitLayout.setModel(model)</code>
     * 
     * @param model the root of the MultiSplitLayout model
     * @see #getMultiSplitLayout
     * @see MultiSplitLayout#setModel
     */
    public final void setModel(Node model) {
	getMultiSplitLayout().setModel(model);
    }

    /** 
     * A convenience method that sets the MultiSplitLayout dividerSize
     * property. Equivalent to 
     * <code>getMultiSplitLayout().setDividerSize(newDividerSize)</code>.
     * 
     * @param dividerSize the value of the dividerSize property
     * @see #getMultiSplitLayout
     * @see MultiSplitLayout#setDividerSize
     */
    public final void setDividerSize(int dividerSize) {
	getMultiSplitLayout().setDividerSize(dividerSize);
    }

    /**
     * Sets the value of the <code>continuousLayout</code> property.
     * If true, then the layout is revalidated continuously while
     * a divider is being moved.  The default value of this property
     * is true.
     *
     * @param continuousLayout value of the continuousLayout property
     * @see #isContinuousLayout
     */
    public void setContinuousLayout(boolean continuousLayout) {
        boolean oldContinuousLayout = continuousLayout;
        this.continuousLayout = continuousLayout;
        firePropertyChange("continuousLayout", oldContinuousLayout, continuousLayout);
    }

    /**
     * Returns true if dragging a divider only updates
     * the layout when the drag gesture ends (typically, when the 
     * mouse button is released).
     *
     * @return the value of the <code>continuousLayout</code> property
     * @see #setContinuousLayout
     */
    public boolean isContinuousLayout() {
        return continuousLayout;
    }

    /** 
     * Returns the Divider that's currently being moved, typically
     * because the user is dragging it, or null.
     * 
     * @return the Divider that's being moved or null.
     */
    public Divider activeDivider() {
	return dragDivider;
    }

    /**
     * Draws a single Divider.  Typically used to specialize the
     * way the active Divider is painted.  
     * 
     * @see #getDividerPainter
     * @see #setDividerPainter
     */
    public static abstract class DividerPainter {
	/**
	 * Paint a single Divider.       
	 * 
	 * @param g the Graphics object to paint with
	 * @param divider the Divider to paint
	 */
	public abstract void paint(Graphics g, Divider divider);
    }

    private class DefaultDividerPainter extends DividerPainter {
	public void paint(Graphics g, Divider divider) {
	    if ((divider == activeDivider()) && !isContinuousLayout()) {
		Graphics2D g2d = (Graphics2D)g;
		g2d.setColor(Color.black);
		g2d.fill(divider.getBounds());
	    }
	}
    }

    /** 
     * The DividerPainter that's used to paint Dividers on this MultiSplitPane.
     * This property may be null.
     * 
     * @return the value of the dividerPainter Property
     * @see #setDividerPainter
     */
    public DividerPainter getDividerPainter() {
	return dividerPainter;
    }

    /** 
     * Sets the DividerPainter that's used to paint Dividers on this 
     * MultiSplitPane.  The default DividerPainter only draws
     * the activeDivider (if there is one) and then, only if 
     * continuousLayout is false.  The value of this property is 
     * used by the paintChildren method: Dividers are painted after
     * the MultiSplitPane's children have been rendered so that 
     * the activeDivider can appear "on top of" the children.
     * 
     * @param dividerPainter the value of the dividerPainter property, can be null
     * @see #paintChildren
     * @see #activeDivider
     */
    public void setDividerPainter(DividerPainter dividerPainter) {
	this.dividerPainter = dividerPainter;
    }

    /**
     * Uses the DividerPainter (if any) to paint each Divider that
     * overlaps the clip Rectangle.  This is done after the call to
     * <code>super.paintChildren()</code> so that Dividers can be 
     * rendered "on top of" the children.
     * <p>
     * {@inheritDoc}
     */
    protected void paintChildren(Graphics g) {
	super.paintChildren(g);
	DividerPainter dp = getDividerPainter();
	Rectangle clipR = g.getClipBounds();
	if ((dp != null) && (clipR != null)) {
            Graphics dpg = g.create();
            try {
		MultiSplitLayout msl = getMultiSplitLayout();
		for(Divider divider : msl.dividersThatOverlap(clipR)) {
		    dp.paint(dpg, divider);
		}
            }
            finally {
		dpg.dispose();
            }
	}
    }

    private boolean dragUnderway = false;
    private MultiSplitLayout.Divider dragDivider = null;
    private Rectangle initialDividerBounds = null;
    private boolean oldFloatingDividers = true;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int dragMin = -1;
    private int dragMax = -1;
    
    private void startDrag(int mx, int my) {
	requestFocusInWindow();
	MultiSplitLayout msl = getMultiSplitLayout();
	MultiSplitLayout.Divider divider = msl.dividerAt(mx, my);
	if (divider != null) {
	    MultiSplitLayout.Node prevNode = divider.previousSibling();
	    MultiSplitLayout.Node nextNode = divider.nextSibling();
	    if ((prevNode == null) || (nextNode == null)) {
		dragUnderway = false;
	    }
	    else {
		initialDividerBounds = divider.getBounds();
		dragOffsetX = mx - initialDividerBounds.x;
		dragOffsetY = my - initialDividerBounds.y;
		dragDivider  = divider;
		Rectangle prevNodeBounds = prevNode.getBounds();
		Rectangle nextNodeBounds = nextNode.getBounds();
		if (dragDivider.isVertical()) {
		    dragMin = prevNodeBounds.x;
		    dragMax = nextNodeBounds.x + nextNodeBounds.width;
		    dragMax -= dragDivider.getBounds().width;
		}
		else {
		    dragMin = prevNodeBounds.y;
		    dragMax = nextNodeBounds.y + nextNodeBounds.height;
		    dragMax -= dragDivider.getBounds().height;
		}
		oldFloatingDividers = getMultiSplitLayout().getFloatingDividers();
		getMultiSplitLayout().setFloatingDividers(false);
		dragUnderway = true;
	    }
	}
	else {
	    dragUnderway = false;
	}
    }

    private void repaintDragLimits() {
	Rectangle damageR = dragDivider.getBounds();
	if (dragDivider.isVertical()) {
	    damageR.x = dragMin;
	    damageR.width = dragMax - dragMin;
	}
	else {
	    damageR.y = dragMin;
	    damageR.height = dragMax - dragMin;
	}
	repaint(damageR);
    }

    private void updateDrag(int mx, int my) {
	if (!dragUnderway) {
	    return;
	}
	Rectangle oldBounds = dragDivider.getBounds();
	Rectangle bounds = new Rectangle(oldBounds);
	if (dragDivider.isVertical()) {
	    bounds.x = mx - dragOffsetX;
	    bounds.x = Math.max(bounds.x, dragMin);
	    bounds.x = Math.min(bounds.x, dragMax);
	}
	else {
	    bounds.y = my - dragOffsetY;
	    bounds.y = Math.max(bounds.y, dragMin);
	    bounds.y = Math.min(bounds.y, dragMax);
	}
	dragDivider.setBounds(bounds);
	if (isContinuousLayout()) {
	    revalidate();
	    repaintDragLimits();
	}
	else {
	    repaint(oldBounds.union(bounds));
	}
    }

    private void clearDragState() {
	dragDivider = null;
	initialDividerBounds = null;
	oldFloatingDividers = true;
	dragOffsetX = dragOffsetY = 0;
	dragMin = dragMax = -1;
	dragUnderway = false;
    }

    private void finishDrag(int x, int y) {
	if (dragUnderway) {
	    clearDragState();
	    if (!isContinuousLayout()) {
		revalidate();
		repaint();
	    }
	}
    }
    
    private void cancelDrag() {       
	if (dragUnderway) {
	    dragDivider.setBounds(initialDividerBounds);
	    getMultiSplitLayout().setFloatingDividers(oldFloatingDividers);
	    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    repaint();
	    revalidate();
	    clearDragState();
	}
    }

    private void updateCursor(int x, int y, boolean show) {
	if (dragUnderway) {
	    return;
	}
	int cursorID = Cursor.DEFAULT_CURSOR;
	if (show) {
	    MultiSplitLayout.Divider divider = getMultiSplitLayout().dividerAt(x, y);
	    if (divider != null) {
		cursorID  = (divider.isVertical()) ? 
		    Cursor.E_RESIZE_CURSOR : 
		    Cursor.N_RESIZE_CURSOR;
	    }
	}
	setCursor(Cursor.getPredefinedCursor(cursorID));
    }


    private class InputHandler extends MouseInputAdapter implements KeyListener {

	public void mouseEntered(MouseEvent e) {
	    updateCursor(e.getX(), e.getY(), true);
	}

	public void mouseMoved(MouseEvent e) {
	    updateCursor(e.getX(), e.getY(), true);
	}

	public void mouseExited(MouseEvent e) {
	    updateCursor(e.getX(), e.getY(), false);
	}

	public void mousePressed(MouseEvent e) {
	    startDrag(e.getX(), e.getY());
	}
	public void mouseReleased(MouseEvent e) {
	    finishDrag(e.getX(), e.getY());
	}
	public void mouseDragged(MouseEvent e) {
	    updateDrag(e.getX(), e.getY());	    
	}
        public void keyPressed(KeyEvent e) { 
	    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
		cancelDrag();
	    }
	}
        public void keyReleased(KeyEvent e) { }
	public void keyTyped(KeyEvent e) { }
    }

    public AccessibleContext getAccessibleContext() {
        if( accessibleContext == null ) {
            accessibleContext = new AccessibleMultiSplitPane();
        }
        return accessibleContext;
    }
    
    protected class AccessibleMultiSplitPane extends AccessibleJPanel {

		private static final long serialVersionUID = 4177114112369591280L;

		public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SPLIT_PANE;
        }
    }
}
