package forge.gui.toolbox;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;

import forge.gui.toolbox.FSkin.SkinnedList;

/** 
 * A JList object using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FList<E> extends SkinnedList<E> {
    public FList() {
        super();
        initialize();
    }
    /** 
     * A JList object using Forge skin properties.
     * This constructor assumes list contents are null and will be set later.
     * This constructor is used for applying a list model at instantiation.
     * @param model0 &emsp; {@link javax.swing.ListModel}
     */
    public FList(final ListModel<E> model0) {
        super(model0);
        initialize();
    }

    /**
     * A JList object using Forge skin properties.
     * This constructor may be passed an object array of list contents.
     * 
     * @param o0 {@link java.lang.Object}[]
     */
    public FList(E[] o0) {
        super(o0);
        initialize();
    }

    private void initialize() {
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setSelectionForeground(this.getSkin().getForeground());
        this.setFont(FSkin.getFont(12));
        this.setCellRenderer(new ComplexCellRenderer<E>());

        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
                updateSelectionBackground();
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                updateSelectionBackground();
            }
        });
        updateSelectionBackground();
    }

    private void updateSelectionBackground() {
        this.setSelectionBackground(FSkin.getColor(hasFocus() ? FSkin.Colors.CLR_ACTIVE : FSkin.Colors.CLR_INACTIVE));
    }

    private class ComplexCellRenderer<E1> implements ListCellRenderer<E1> {
        private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList<? extends E1> lst0, E1 val0, int i0,
            boolean isSelected, boolean cellHasFocus) {

            JLabel lblItem = (JLabel) defaultRenderer.getListCellRendererComponent(
                    lst0, val0, i0, isSelected, cellHasFocus);
            lblItem.setBorder(new EmptyBorder(4, 3, 4, 3));
            return lblItem;
        }
    }
}
