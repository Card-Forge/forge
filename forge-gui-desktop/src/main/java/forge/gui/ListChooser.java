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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.card.MagicColor;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FList;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextField;
import forge.util.ITranslatable;
import forge.util.Localizer;

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
 * <li>The dialog was "canceled" if Localizer.getInstance().getMessage("lblCancel") or "X" was clicked.</li>
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
    private final List<T> allItems;
    private List<T> displayedItems;
    private final int minChoices, maxChoices;
    private final Function<T, String> display;

    // Flag: was the dialog already shown?
    private boolean called;

    // initialized before; listeners may be added to it
    private final FList<T> lstChoices;
    private final FOptionPane optionPane;
    private final ChooserListModel listModel;

    public ListChooser(final String title, final int minChoices, final int maxChoices, final Collection<T> list, final Function<T, String> display) {
        FThreads.assertExecutedByEdt(true);
        this.minChoices = minChoices;
        this.maxChoices = maxChoices;
        this.display = display;
        this.allItems = list.getClass().isInstance(List.class) ? (List<T>)list : Lists.newArrayList(list);
        this.displayedItems = new ArrayList<>(this.allItems);
        this.listModel = new ChooserListModel();
        this.lstChoices = new FList<>(this.listModel);

        final ImmutableList<String> options;
        if (minChoices == 0) {
            options = ImmutableList.of(Localizer.getInstance().getMessage("lblOK"),Localizer.getInstance().getMessage("lblCancel"));
        } else {
            options = ImmutableList.of(Localizer.getInstance().getMessage("lblOK"));
        }

        if (maxChoices == 1 || minChoices == -1) {
            this.lstChoices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        this.lstChoices.setCellRenderer(new TransformedCellRenderer(display));

        final FScrollPane listScroller = new FScrollPane(this.lstChoices, true);
        int minWidth = this.lstChoices.getAutoSizeWidth();
        if (this.lstChoices.getModel().getSize() > this.lstChoices.getVisibleRowCount()) {
            minWidth += listScroller.getVerticalScrollBar().getPreferredSize().width;
        }
        listScroller.setMinimumSize(new Dimension(minWidth, listScroller.getMinimumSize().height));

        // Add search field for large lists (same threshold as mobile)
        if (allItems.size() > 25) {
            final FTextField searchField = new FTextField.Builder()
                    .ghostText(Localizer.getInstance().getMessage("lblSearch"))
                    .showGhostTextWithFocus()
                    .build();
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { applyFilter(searchField); }
                @Override public void removeUpdate(DocumentEvent e) { applyFilter(searchField); }
                @Override public void changedUpdate(DocumentEvent e) { applyFilter(searchField); }
            });
            searchField.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(final KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        ListChooser.this.commit();
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        lstChoices.requestFocusInWindow();
                    }
                }
            });

            final JPanel panel = new JPanel(new BorderLayout(0, 4));
            panel.setOpaque(false);
            panel.add(searchField, BorderLayout.NORTH);
            panel.add(listScroller, BorderLayout.CENTER);

            this.optionPane = new FOptionPane(null, title, null, panel, options, minChoices < 0 ? 0 : -1);
            if (minChoices != -1) {
                this.optionPane.setDefaultFocus(searchField);
            }
        } else {
            this.optionPane = new FOptionPane(null, title, null, listScroller, options, minChoices < 0 ? 0 : -1);
            if (minChoices != -1) {
                this.optionPane.setDefaultFocus(this.lstChoices);
            }
        }

        this.optionPane.setButtonEnabled(0, minChoices <= 0);

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
            @Override public void onLeftDoubleClick(final MouseEvent e) {
                    ListChooser.this.commit();
            }
        });
    }

    private void applyFilter(final FTextField searchField) {
        final String text = searchField.getText().toLowerCase();
        lstChoices.clearSelection();

        if (text.isEmpty()) {
            displayedItems = new ArrayList<>(allItems);
        } else {
            final List<T> startsWith = new ArrayList<>();
            final List<T> contains = new ArrayList<>();
            for (final T item : allItems) {
                final String name = getDisplayText(item).toLowerCase();
                if (name.startsWith(text)) {
                    startsWith.add(item);
                } else if (name.contains(text)) {
                    contains.add(item);
                }
            }
            startsWith.sort(Comparator.comparingInt(a -> getDisplayText(a).length()));
            displayedItems = new ArrayList<>(startsWith.size() + contains.size());
            displayedItems.addAll(startsWith);
            displayedItems.addAll(contains);
        }

        listModel.fireDataChanged();
        if (!displayedItems.isEmpty() && maxChoices > 0) {
            lstChoices.setSelectedIndex(0);
        }
    }

    private String getDisplayText(final T value) {
        if (display != null) {
            return display.apply(value);
        }
        if (value instanceof ITranslatable t) {
            return t.getTranslatedName();
        }
        return value != null ? value.toString() : "";
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
        return show(null);
    }

    /**
     * Shows the dialog and returns after the dialog was closed.
     */
    public boolean show(final Collection<T> item) {
        if (this.called) {
            throw new IllegalStateException("Already shown");
        }
        int result;
        do {
            //invoke later so selected item not set until dialog open
            SwingUtilities.invokeLater(() -> {
                if (item != null) {
                    int[] indices = item.stream()
                            .mapToInt(displayedItems::indexOf)
                            .filter(i -> i >= 0)
                            .toArray();
                    lstChoices.setSelectedIndices(indices);
                }
                else {
                    lstChoices.setSelectedIndex(0);
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
            return ListChooser.this.displayedItems.size();
        }

        @Override
        public T getElementAt(final int index) {
            return ListChooser.this.displayedItems.get(index);
        }

        void fireDataChanged() {
            fireContentsChanged(this, 0, Math.max(getSize() - 1, 0));
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
            Component result = defRenderer.getListCellRendererComponent(list, getLabel(value), index, isSelected, cellHasFocus);
            if (value instanceof MagicColor.Color c) {
                defRenderer.setIcon(fromSkinProp(FSkinProp.iconFromColor(c)));
            }
            if (value instanceof CardType.CoreType c) {
                defRenderer.setIcon(fromSkinProp(FSkinProp.iconFromCoreType(c)));
            }
            return result;
        }

        protected ImageIcon fromSkinProp(FSkinProp prop) {
            if (prop == null) {
                return null;
            }
            return FSkin.getImage(prop, 24, 24).getIcon();
        }

        protected String getLabel(final T value) {
            if (transformer != null) {
                return transformer.apply(value);
            }
            if (value instanceof ITranslatable t) {
                return t.getTranslatedName();
            }
            return value != null ? value.toString() : "";
        }
    }
}
