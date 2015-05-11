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

package forge.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.FThreads;
import forge.toolbox.FList;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;

/**
 * A simple class that shows a list of choices in a dialog. Two properties
 * influence the behavior of a list chooser: minSelection and maxSelection.
 * These two give the allowed number of selected items for the dialog to be
 * closed. A negative value for minSelection suggests that the list is revealed
 * and the choice doesn't matter.
 * <ul>
 * <li>If minSelection is 0, there will be a Cancel button.</li>
 * <li>If minSelection is -1, 0 or 1, double-clicking a choice will also close
 * the dialog.</li>
 * <li>If the number of selections is out of bounds, the "OK" button is
 * disabled.</li>
 * <li>The dialog was "committed" if "OK" was clicked or a choice was double
 * clicked.</li>
 * <li>The dialog was "canceled" if "Cancel" or "X" was clicked.</li>
 * <li>If the dialog was canceled, the selection will be empty.</li>
 * <li>
 * </ul>
 *
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id: ListChooser.java 25183 2014-03-14 23:09:45Z drdev $
 */
public class ListChooser<T> {
    // Data and number of choices for the list
    private final List<T> list;
    private final int minChoices, maxChoices;

    // Flag: was the dialog already shown?
    private boolean called;

    // initialized before; listeners may be added to it
    private final FList<T> lstChoices;
    private final FOptionPane optionPane;

    public ListChooser(final String title, final int minChoices, final int maxChoices, final Collection<T> list, final Function<T, String> display) {
        FThreads.assertExecutedByEdt(true);
        this.minChoices = minChoices;
        this.maxChoices = maxChoices;
        this.list = list.getClass().isInstance(List.class) ? (List<T>)list : Lists.newArrayList(list);
        this.lstChoices = new FList<T>(new ChooserListModel());

        final ImmutableList<String> options;
        if (minChoices == 0) {
            options = ImmutableList.of("OK","Cancel");
        } else {
            options = ImmutableList.of("OK");
        }

        if (maxChoices == 1 || minChoices == -1) {
            this.lstChoices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        if (display != null) {
            this.lstChoices.setCellRenderer(new TransformedCellRenderer(display));
        }

        final FScrollPane listScroller = new FScrollPane(this.lstChoices, true);
        int minWidth = this.lstChoices.getAutoSizeWidth();
        if (this.lstChoices.getModel().getSize() > this.lstChoices.getVisibleRowCount()) {
            minWidth += listScroller.getVerticalScrollBar().getPreferredSize().width;
        }
        listScroller.setMinimumSize(new Dimension(minWidth, listScroller.getMinimumSize().height));

        this.optionPane = new FOptionPane(null, title, null, listScroller, options, minChoices < 0 ? 0 : -1);
        this.optionPane.setButtonEnabled(0, minChoices <= 0);

        if (minChoices != -1) {
            this.optionPane.setDefaultFocus(this.lstChoices);
        }

        if (minChoices > 0) {
            this.optionPane.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }

        if (minChoices != -1) {
            this.lstChoices.getSelectionModel().addListSelectionListener(new SelListener());
        }

        this.lstChoices.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ListChooser.this.commit();
                }
            }
        });
        this.lstChoices.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ListChooser.this.commit();
                }
            }
        });
    }

    /**
     * Returns the FList used in the list chooser. this is useful for
     * registering listeners before showing the dialog.
     *
     * @return a {@link javax.swing.JList} object.
     */
    public FList<T> getLstChoices() {
        return this.lstChoices;
    }

    /** @return boolean */
    public boolean show() {
        return show(list.get(0));
    }

    /**
     * Shows the dialog and returns after the dialog was closed.
     *
     * @param index0 index to select when shown
     * @return a boolean.
     */
    public boolean show(final T item) {
        if (this.called) {
            throw new IllegalStateException("Already shown");
        }
        int result;
        do {
            SwingUtilities.invokeLater(new Runnable() { //invoke later so selected item not set until dialog open
                @Override
                public void run() {
                    if (list.contains(item)) {
                        lstChoices.setSelectedValue(item, true);
                    }
                    else {
                        lstChoices.setSelectedIndex(0);
                    }
                }
            });
            this.optionPane.setVisible(true);
            result = this.optionPane.getResult();
            if (result != 0) {
                this.lstChoices.clearSelection();
            }
            // can't stop closing by ESC, so repeat if cancelled
        } while (this.minChoices > 0 && result != 0);

        this.optionPane.dispose();

        // this assert checks if we really don't return on a cancel if input is mandatory
        assert (this.minChoices <= 0) || (result == 0);
        this.called = true;
        return (result == 0);
    }

    /**
     * Returns if the dialog was closed by pressing "OK" or double clicking an
     * option the last time.
     *
     * @return a boolean.
     */
    public boolean isCommitted() {
        if (!this.called) {
            throw new IllegalStateException("not yet shown");
        }
        return (this.optionPane.getResult() == 0);
    }

    /**
     * Returns the selected indices as a list of integers.
     *
     * @return a {@link java.util.List} object.
     */
    public int[] getSelectedIndices() {
        if (!this.called) {
            throw new IllegalStateException("not yet shown");
        }
        return this.lstChoices.getSelectedIndices();
    }

    /**
     * Returns the selected values as a list of objects. no casts are necessary
     * when retrieving the objects.
     *
     * @return a {@link java.util.List} object.
     */
    public List<T> getSelectedValues() {
        if (!this.called) {
            throw new IllegalStateException("not yet shown");
        }
        return this.lstChoices.getSelectedValuesList();
    }

    /**
     * Returns the (minimum) selected index, or -1.
     *
     * @return a int.
     */
    public int getSelectedIndex() {
        if (!this.called) {
            throw new IllegalStateException("not yet shown");
        }
        return this.lstChoices.getSelectedIndex();
    }

    /**
     * Returns the (first) selected value, or null.
     *
     * @return a T object.
     */
    public T getSelectedValue() {
        if (!this.called) {
            throw new IllegalStateException("not yet shown");
        }
        return this.lstChoices.getSelectedValue();
    }

    /**
     * <p>
     * commit.
     * </p>
     */
    private void commit() {
        if (this.optionPane.isButtonEnabled(0)) {
            optionPane.setResult(0);
        }
    }

    private class ChooserListModel extends AbstractListModel<T> {
        private static final long serialVersionUID = 3871965346333840556L;

        @Override
        public int getSize() {
            return ListChooser.this.list.size();
        }

        @Override
        public T getElementAt(final int index) {
            return ListChooser.this.list.get(index);
        }
    }

    private class SelListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) {
            final int num = ListChooser.this.lstChoices.getSelectedIndices().length;
            ListChooser.this.optionPane.setButtonEnabled(0, (num >= ListChooser.this.minChoices) && (num <= ListChooser.this.maxChoices));
        }
    }

    private class TransformedCellRenderer implements ListCellRenderer<T> {
        public final Function<T, String> transformer;
        public final DefaultListCellRenderer defRenderer;

        /**
         * TODO: Write javadoc for Constructor.
         */
        public TransformedCellRenderer(final Function<T, String> t1) {
            transformer = t1;
            defRenderer = new DefaultListCellRenderer();
        }

        /* (non-Javadoc)
         * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        @Override
        public Component getListCellRendererComponent(final JList<? extends T> list, final T value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            // TODO Auto-generated method stub
            return defRenderer.getListCellRendererComponent(list, transformer.apply(value), index, isSelected, cellHasFocus);
        }
    }
}
