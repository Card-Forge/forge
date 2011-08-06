
package arcane.ui;

import java.awt.Frame;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;

import arcane.ui.CardArea;
import arcane.ui.CardPanel;
import arcane.ui.util.CardPanelMouseListener;

public class HandArea extends CardArea {
	private static final long serialVersionUID = 7488132628637407745L;

	public HandArea (JScrollPane scrollPane, final Frame frame) {
		super(scrollPane);

		setDragEnabled(true);
		setVertical(true);

		addCardPanelMouseListener(new CardPanelMouseListener() {
			public void mouseRightClicked (CardPanel panel, MouseEvent evt) {
			}

			public void mouseOver (CardPanel panel, MouseEvent evt) {
			}

			public void mouseOut (CardPanel panel, MouseEvent evt) {
			}

			public void mouseMiddleClicked (CardPanel panel, MouseEvent evt) {
			}

			public void mouseLeftClicked (CardPanel panel, MouseEvent evt) {
				
			}

			public void mouseDragged (CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt) {
			}

			public void mouseDragStart (CardPanel dragPanel, MouseEvent evt) {
			}

			public void mouseDragEnd (CardPanel dragPanel, MouseEvent evt) {
			}
		});
	}
}
