/**
 * ListChooser.java
 *
 * Created on 31.08.2009
 */

package forge.gui;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.AbstractList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static javax.swing.JOptionPane.*;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;


/**
 * A simple class that shows a list of choices in a dialog. Two properties influence the behavior of a list
 * chooser: minSelection and maxSelection. These two give the allowed number of selected items for the dialog to be
 * closed.
 * <ul>
 * <li>If minSelection is 0, there will be a Cancel button.</li>
 * <li>If minSelection is 0 or 1, double-clicking a choice will also close the dialog.</li>
 * <li>If the number of selections is out of bounds, the "OK" button is disabled.</li>
 * <li>The dialog was "committed" if "OK" was clicked or a choice was double clicked.</li>
 * <li>The dialog was "canceled" if "Cancel" or "X" was clicked.</li>
 * <li>If the dialog was canceled, the selection will be empty.</li>
 * <li>
 * </ul>
 *
 * @author Forge
 * @version $Id$
 */
public class ListChooser<T> {

    //Data and number of choices for the list
    private List<T> list;
    private int minChoices, maxChoices;

    //Decoration
    private String title;

    //Flag: was the dialog already shown?
    private boolean called;
    //initialized before; listeners may be added to it
    private JList jList;
    //Temporarily stored for event handlers during show
    private JDialog d;
    private JOptionPane p;
    private Action ok, cancel;

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param list a T object.
     */
    public ListChooser(String title, T... list) {
        this(title, 1, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param numChoices a int.
     * @param list a T object.
     */
    public ListChooser(String title, int numChoices, T... list) {
        this(title, numChoices, numChoices, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param minChoices a int.
     * @param maxChoices a int.
     * @param list a T object.
     */
    public ListChooser(String title, int minChoices, int maxChoices, T... list) {
        this(title, null, minChoices, maxChoices, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param list a T object.
     */
    public ListChooser(String title, String message, T... list) {
        this(title, message, 1, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param numChoices a int.
     * @param list a T object.
     */
    public ListChooser(String title, String message, int numChoices, T... list) {
        this(title, message, numChoices, numChoices, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param minChoices a int.
     * @param maxChoices a int.
     * @param list a T object.
     */
    public ListChooser(String title, String message, int minChoices, int maxChoices, T... list) {
        this(title, message, minChoices, maxChoices, asList(list));
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param list a {@link java.util.List} object.
     */
    public ListChooser(String title, List<T> list) {
        this(title, 1, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param numChoices a int.
     * @param list a {@link java.util.List} object.
     */
    public ListChooser(String title, int numChoices, List<T> list) {
        this(title, numChoices, numChoices, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param minChoices a int.
     * @param maxChoices a int.
     * @param list a {@link java.util.List} object.
     */
    public ListChooser(String title, int minChoices, int maxChoices, List<T> list) {
        this(title, null, minChoices, maxChoices, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param list a {@link java.util.List} object.
     */
    public ListChooser(String title, String message, List<T> list) {
        this(title, message, 1, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param numChoices a int.
     * @param list a {@link java.util.List} object.
     */
    public ListChooser(String title, String message, int numChoices, List<T> list) {
        this(title, message, numChoices, numChoices, list);
    }

    /**
     * <p>Constructor for ListChooser.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param minChoices a int.
     * @param maxChoices a int.
     * @param list a {@link java.util.List} object.
     */
    public ListChooser(String title, String message, int minChoices, int maxChoices, List<T> list) {
        this.title = title;
        this.minChoices = minChoices;
        this.maxChoices = maxChoices;
        this.list = unmodifiableList(list);
        jList = new JList(new ChooserListModel());
        ok = new CloseAction(OK_OPTION, "OK");
        ok.setEnabled(minChoices == 0);
        cancel = new CloseAction(CANCEL_OPTION, "Cancel");

        Object[] options;
        if (minChoices == 0) options = new Object[]{new JButton(ok), new JButton(cancel)};
        else options = new Object[]{new JButton(ok)};
        if (maxChoices == 1) { jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); }

        p = new JOptionPane(new Object[]{message, new JScrollPane(jList)}, QUESTION_MESSAGE, DEFAULT_OPTION,
                null, options, options[0]);
        jList.getSelectionModel().addListSelectionListener(new SelListener());
        jList.addMouseListener(new DblListener());
    }

    /**
     * <p>getChoices.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<T> getChoices() {
        return list;
    }

    /**
     * Returns the JList used in the list chooser. this is useful for registering listeners before showing the
     * dialog.
     *
     * @return a {@link javax.swing.JList} object.
     */
    public JList getJList() {
        return jList;
    }

    /**
     * Shows the dialog and returns after the dialog was closed.
     *
     * @return a boolean.
     */
    public synchronized boolean show() {
        if (called) throw new IllegalStateException("Already shown");
        Integer value;
        do {
            d = p.createDialog(p.getParent(), title);
            if (minChoices != 0) d.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            jList.setSelectedIndex(0);
            d.addWindowFocusListener(new WindowFocusListener() {
                @Override public void windowGainedFocus(final WindowEvent e) { jList.grabFocus(); }
                @Override public void windowLostFocus(final WindowEvent e) { }
            });
            d.setVisible(true);
            d.dispose();
            value = (Integer) p.getValue();
            if (value == null || value != OK_OPTION) jList.clearSelection();
            //can't stop closing by ESC, so repeat if cancelled
        } while (minChoices != 0 && value != OK_OPTION);
        //this assert checks if we really don't return on a cancel if input is mandatory
        assert minChoices == 0 || value == OK_OPTION;
        called = true;
        return value != null && value == OK_OPTION;
    }

    /**
     * Returns if the dialog was closed by pressing "OK" or double clicking an option the last time
     *
     * @return a boolean.
     */
    public boolean isCommitted() {
        if (!called) throw new IllegalStateException("not yet shown");
        return (Integer) p.getValue() == OK_OPTION;
    }

    /**
     * Returns the selected indices as a list of integers
     *
     * @return a {@link java.util.List} object.
     */
    public List<Integer> getSelectedIndices() {
        if (!called) throw new IllegalStateException("not yet shown");
        final int[] indices = jList.getSelectedIndices();
        return new AbstractList<Integer>() {
            @Override
            public int size() {
                return indices.length;
            }

            @Override
            public Integer get(int index) {
                return indices[index];
            }
        };
    }

    /**
     * Returns the selected values as a list of objects. no casts are necessary when retrieving the objects.
     *
     * @return a {@link java.util.List} object.
     */
    public List<T> getSelectedValues() {
        if (!called) throw new IllegalStateException("not yet shown");
        final Object[] selected = jList.getSelectedValues();
        return new AbstractList<T>() {
            @Override
            public int size() {
                return selected.length;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T get(int index) {
                return (T) selected[index];
            }
        };
    }

    /**
     * Returns the (minimum) selected index, or -1
     *
     * @return a int.
     */
    public int getSelectedIndex() {
        if (!called) throw new IllegalStateException("not yet shown");
        return jList.getSelectedIndex();
    }

    /**
     * Returns the (first) selected value, or null
     *
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    public T getSelectedValue() {
        if (!called) throw new IllegalStateException("not yet shown");
        return (T) jList.getSelectedValue();
    }

    /**
     * <p>commit.</p>
     */
    private void commit() {
        if (ok.isEnabled()) p.setValue(OK_OPTION);
    }

    private class ChooserListModel extends AbstractListModel {

        private static final long serialVersionUID = 3871965346333840556L;

        public int getSize() {
            return list.size();
        }


        public Object getElementAt(int index) {
            return list.get(index);
        }
    }

    private class CloseAction extends AbstractAction {

        private static final long serialVersionUID = -8426767786083886936L;
        private int value;

        public CloseAction(int value, String label) {
            super(label);
            this.value = value;
        }


        public void actionPerformed(ActionEvent e) {
            p.setValue(value);
        }
    }

    private class SelListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            int num = jList.getSelectedIndices().length;
            ok.setEnabled(num >= minChoices && num <= maxChoices);
        }
    }

    private class DblListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) commit();
        }
    }
}
