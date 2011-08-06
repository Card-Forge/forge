
package arcane.ui.util;

import java.awt.event.MouseEvent;

import arcane.ui.CardPanel;

public interface CardPanelMouseListener {
	public void mouseOver (CardPanel panel, MouseEvent evt);

	public void mouseOut (CardPanel panel, MouseEvent evt);

	public void mouseLeftClicked (CardPanel panel, MouseEvent evt);

	public void mouseMiddleClicked (CardPanel panel, MouseEvent evt);

	public void mouseRightClicked (CardPanel panel, MouseEvent evt);

	public void mouseDragStart (CardPanel dragPanel, MouseEvent evt);

	public void mouseDragged (CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt);

	public void mouseDragEnd (CardPanel dragPanel, MouseEvent evt);
}
