/**
 * ListChooser.java
 *
 * Created on 31.08.2009
 */

package forge.gui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A simple class that shows a list of choices in a dialog. Two properties
 * influence the behavior of a list chooser: minSelection and maxSelection.
 * These two give the allowed number of selected items for the dialog to be
 * closed.
 * <ul>
 * <li>If minSelection is 0, there will be a Cancel button.</li>
 * <li>If minSelection is 0 or 1, double-clicking a choice will also close the
 * dialog.</li>
 * <li>If the number of selections is out of bounds, the "OK" button is
 * disabled.</li>
 * <li>The dialog was "committed" if "OK" was clicked or a choice was double
 * clicked.</li>
 * <li>The dialog was "canceled" if "Cancel" or "X" was clicked.</li>
 * <li>If the dialog was canceled, the selection will be empty.</li>
 * <li>
 * </ul>
 *
 * @param <T> the generic type
 * @author Forge
 * @version $Id$
 */
public class ListChooser<T> {

    // Data and number of choices for the list
    private List<T> list;
    private int minChoices, maxChoices;

    // Decoration
    private String title;

    // Flag: was the dialog already shown?
    private boolean called;
    // initialized before; listeners may be added to it
    private JList jList;
    // Temporarily stored for event handlers during show
    private JDialog d;
    private JOptionPane p;
    private Action ok, cancel;

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param list
     *            a T object.
     */
    public ListChooser(final String title, final T... list) {
        this(title, 1, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param numChoices
     *            a int.
     * @param list
     *            a T object.
     */
    public ListChooser(final String title, final int numChoices, final T... list) {
        this(title, numChoices, numChoices, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param minChoices
     *            a int.
     * @param maxChoices
     *            a int.
     * @param list
     *            a T object.
     */
    public ListChooser(final String title, final int minChoices, final int maxChoices, final T... list) {
        this(title, null, minChoices, maxChoices, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param list
     *            a T object.
     */
    public ListChooser(final String title, final String message, final T... list) {
        this(title, message, 1, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param numChoices
     *            a int.
     * @param list
     *            a T object.
     */
    public ListChooser(final String title, final String message, final int numChoices, final T... list) {
        this(title, message, numChoices, numChoices, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param minChoices
     *            a int.
     * @param maxChoices
     *            a int.
     * @param list
     *            a T object.
     */
    public ListChooser(final String title, final String message, final int minChoices, final int maxChoices,
            final T... list) {
        this(title, message, minChoices, maxChoices, Arrays.asList(list));
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param list
     *            a {@link java.util.List} object.
     */
    public ListChooser(final String title, final List<T> list) {
        this(title, 1, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param numChoices
     *            a int.
     * @param list
     *            a {@link java.util.List} object.
     */
    public ListChooser(final String title, final int numChoices, final List<T> list) {
        this(title, numChoices, numChoices, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param minChoices
     *            a int.
     * @param maxChoices
     *            a int.
     * @param list
     *            a {@link java.util.List} object.
     */
    public ListChooser(final String title, final int minChoices, final int maxChoices, final List<T> list) {
        this(title, null, minChoices, maxChoices, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param list
     *            a {@link java.util.List} object.
     */
    public ListChooser(final String title, final String message, final List<T> list) {
        this(title, message, 1, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param numChoices
     *            a int.
     * @param list
     *            a {@link java.util.List} object.
     */
    public ListChooser(final String title, final String message, final int numChoices, final List<T> list) {
        this(title, message, numChoices, numChoices, list);
    }

    /**
     * <p>
     * Constructor for ListChooser.
     * </p>
     * 
     * @param title
     *            a {@link java.lang.String} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param minChoices
     *            a int.
     * @param maxChoices
     *            a int.
     * @param list
     *            a {@link java.util.List} object.
     */
    public ListChooser(final String title, final String message, final int minChoices, final int maxChoices,
            final List<T> list) {
        this.title = title;
        this.minChoices = minChoices;
        this.maxChoices = maxChoices;
        this.list = Collections.unmodifiableList(list);
        this.jList = new JList(new ChooserListModel());
        this.ok = new CloseAction(JOptionPane.OK_OPTION, "OK");
        this.ok.setEnabled(minChoices == 0);
        this.cancel = new CloseAction(JOptionPane.CANCEL_OPTION, "Cancel");

        Object[] options;
        if (minChoices == 0) {
            options = new Object[] { new JButton(this.ok), new JButton(this.cancel) };
        } else {
            options = new Object[] { new JButton(this.ok) };
        }
        if (maxChoices == 1) {
            this.jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        this.p = new JOptionPane(new Object[] { message, new JScrollPane(this.jList) }, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, options[0]);
        this.jList.getSelectionModel().addListSelectionListener(new SelListener());
        this.jList.addMouseListener(new DblListener());
    }

    /**
     * <p>
     * getChoices.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public List<T> getChoices() {
        return this.list;
    }

    /**
     * Returns the JList used in the list chooser. this is useful for
     * registering listeners before showing the dialog.
     * 
     * @return a {@link javax.swing.JList} object.
     */
    public JList getJList() {
        return this.jList;
    }

    /**
     * Shows the dialog and returns after the dialog was closed.
     * 
     * @return a boolean.
     */
    public synchronized boolean show() {
        if (this.called) {
            throw new IllegalStateException("Already shown");
        }
        Integer value;
        do {
            this.d = this.p.createDialog(this.p.getParent(), this.title);
            if (this.minChoices != 0) {
                this.d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            }
            this.jList.setSelectedIndex(0);
            this.d.addWindowFocusListener(new WindowFocusListener() {
                @Override
                public void windowGainedFocus(final WindowEvent e) {
                    ListChooser.this.jList.grabFocus();
                }

                @Override
                public void windowLostFocus(final WindowEvent e) {
                }
            });
            this.d.setVisible(true);
            this.d.dispose();
            value = (Integer) this.p.getValue();
            if ((value == null) || (value != JOptionPane.OK_OPTION)) {
                this.jList.clearSelection();
                // can't stop closing by ESC, so repeat if cancelled
            }
        } while ((this.minChoices != 0) && (value != JOptionPane.OK_OPTION));
        // this assert checks if we really don't return on a cancel if input is
        // mandatory
        assert (this.minChoices == 0) || (value == JOptionPane.OK_OPTION);
        this.called = true;
        return (value != null) && (value == JOptionPane.OK_OPTION);
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
        return (Integer) this.p.getValue() == JOptionPane.OK_OPTION;
    }

    /**
     * Returns the selected indices as a list of integers.
     *
     * @return a {@link java.util.List} object.
     */
    public List<Integer> getSelectedIndices() {
        if (!this.called) {
            throw new IllegalStateException("not yet shown");
        }
        final int[] indices = this.jList.getSelectedIndices();
        return new AbstractList<Integer>() {
            @Override
            public int size() {
                return indices.length;
            }

            @Override
            public Integer get(final int index) {
                return indices[index];
            }
        };
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
        final Object[] selected = this.jList.getSelectedValues();
        return new AbstractList<T>() {
            @Override
            public int size() {
                return selected.length;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T get(final int index) {
                return (T) selected[index];
            }
        };
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
        return this.jList.getSelectedIndex();
    }

    /**
     * Returns the (first) selected value, or null.
     *
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    public T getSelectedValue() {
        if (!this.called) {
            throw new IllegalStateException("not yet shown");
        }
        return (T) this.jList.getSelectedValue();
    }

    /**
     * <p>
     * commit.
     * </p>
     */
    private void commit() {
        if (this.ok.isEnabled()) {
            this.p.setValue(JOptionPane.OK_OPTION);
        }
    }

    private class ChooserListModel extends AbstractListModel {

        private static final long serialVersionUID = 3871965346333840556L;

        @Override
        public int getSize() {
            return ListChooser.this.list.size();
        }

        @Override
        public Object getElementAt(final int index) {
            return ListChooser.this.list.get(index);
        }
    }

    private class CloseAction extends AbstractAction {

        private static final long serialVersionUID = -8426767786083886936L;
        private final int value;

        public CloseAction(final int value, final String label) {
            super(label);
            this.value = value;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            ListChooser.this.p.setValue(this.value);
        }
    }

    private class SelListener implements ListSelectionListener {

        @Override
        public void valueChanged(final ListSelectionEvent e) {
            final int num = ListChooser.this.jList.getSelectedIndices().length;
            ListChooser.this.ok
                    .setEnabled((num >= ListChooser.this.minChoices) && (num <= ListChooser.this.maxChoices));
        }
    }

    private class DblListener extends MouseAdapter {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
                ListChooser.this.commit();
            }
        }
    }
}
