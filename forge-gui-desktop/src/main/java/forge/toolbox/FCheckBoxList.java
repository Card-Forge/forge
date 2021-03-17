package forge.toolbox;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * A list of FCheckBox items using Forge skin properties.
 * Call setListData() with an array of FCheckBox items to populate.
 * <p>
 * based on code at http://www.devx.com/tips/Tip/5342
 */
@SuppressWarnings("serial")
public class FCheckBoxList<E> extends JList<E> {

	private static final Border NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

	private int lastClickedIndex = 0;
	private int currentShiftSelectionMinIndex = Integer.MAX_VALUE;
	private int currentShiftSelectionMaxIndex = -1;
	private int currentHighlightMinIndex = Integer.MAX_VALUE;
	private int currentHighlightMaxIndex = -1;
	private boolean shiftSelectShouldCheckBox = true;

	public FCheckBoxList(final boolean keepSelectionWhenFocusLost) {

		setCellRenderer(new CellRenderer<>());

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {

				final int index = locationToIndex(e.getPoint());

				if (index != -1) {

					if (e.isShiftDown()) {

						int min = Math.min(lastClickedIndex, index);
						int max = Math.max(lastClickedIndex, index);

						if (index == lastClickedIndex) {
							currentHighlightMinIndex = lastClickedIndex;
							currentHighlightMaxIndex = lastClickedIndex;
						} else if (index > lastClickedIndex) {
							currentHighlightMinIndex = lastClickedIndex;
							currentHighlightMaxIndex = index;
						} else if (index < lastClickedIndex) {
							currentHighlightMinIndex = index;
							currentHighlightMaxIndex = lastClickedIndex;
						}

						currentShiftSelectionMinIndex = Math.min(min, currentShiftSelectionMinIndex);
						currentShiftSelectionMaxIndex = Math.max(max, currentShiftSelectionMaxIndex);

						for (int i = currentShiftSelectionMinIndex; i <= currentShiftSelectionMaxIndex; i++) {
							final FCheckBox checkbox = (FCheckBox) getModel().getElementAt(i);
							if (shiftSelectShouldCheckBox) {
								checkbox.setSelected(!shiftSelectShouldCheckBox);
							}
						}

						for (int i = min; i <= max; i++) {
							final FCheckBox checkbox = (FCheckBox) getModel().getElementAt(i);
							checkbox.setSelected(shiftSelectShouldCheckBox);
						}

					} else {

						final FCheckBox checkbox = (FCheckBox) getModel().getElementAt(index);

						if (checkbox.isEnabled()) {
							checkbox.setSelected(!checkbox.isSelected());
							shiftSelectShouldCheckBox = checkbox.isSelected();
							lastClickedIndex = index;
							currentShiftSelectionMinIndex = currentHighlightMinIndex = Integer.MAX_VALUE;
							currentShiftSelectionMaxIndex = currentHighlightMaxIndex = -1;
						}

					}

					repaint();

				}

			}
		});

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyChar() == ' ') {
					final FCheckBox item = (FCheckBox) getSelectedValue();
					if (null == item || !item.isEnabled()) {
						return;
					}

					item.setSelected(!item.isSelected());
					repaint();
				}
			}
		});

		if (!keepSelectionWhenFocusLost) {
			addFocusListener(new FocusListener() {
				int lastSelectedIdx;

				@Override
				public void focusLost(final FocusEvent arg0) {
					lastSelectedIdx = Math.max(0, getSelectedIndex());
					clearSelection();
				}

				@Override
				public void focusGained(final FocusEvent arg0) {
					if (getSelectedIndex() == -1) {
						setSelectedIndex(lastSelectedIdx);
					}
				}
			});
		}

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	}

	protected class CellRenderer<E1> implements ListCellRenderer<E1> {
		@Override
		public Component getListCellRendererComponent(final JList<? extends E1> list, final E1 value, final int index, final boolean isSelected, final boolean cellHasFocus) {

			final FCheckBox checkbox = (FCheckBox) value;

			if (index >= currentHighlightMinIndex && index <= currentHighlightMaxIndex) {
				checkbox.setOpaque(true);
			} else {
				checkbox.setOpaque(false);
			}

			checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
			checkbox.setBackground(getSelectionBackground());
			checkbox.setFont(getFont());
			checkbox.setFocusPainted(false);
			checkbox.setBorderPainted(true);
			checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : NO_FOCUS_BORDER);

			return checkbox;

		}
	}

}
