package forge.gui.toolbox;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;

/** 
 * A JList object using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FList<E> extends JList<E> {

    public FList() {
        super();
        applySkin();
    }
    /** 
     * A JList object using Forge skin properties.
     * This constructor assumes list contents are null and will be set later.
     * This constructor is used for applying a list model at instantiation.
     * @param model0 &emsp; {@link javax.swing.ListModel}
     */
    public FList(final ListModel<E> model0) {
        super(model0);
        applySkin();
    }

    /**
     * A JList object using Forge skin properties.
     * This constructor may be passed an object array of list contents.
     * 
     * @param o0 {@link java.lang.Object}[]
     */
    public FList(E[] o0) {
        super(o0);
        applySkin();
    }

    /**
     * TODO: Write javadoc for this method.
     */
    private void applySkin() {
        setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        ListCellRenderer<E> renderer = new ComplexCellRenderer<E>();
        setCellRenderer(renderer);
    }

    private class ComplexCellRenderer<E1> implements ListCellRenderer<E1> {
        private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList<? extends E1> lst0, E1 val0, int i0,
            boolean isSelected, boolean cellHasFocus) {

            JLabel lblItem = (JLabel) defaultRenderer.getListCellRendererComponent(
                    lst0, val0, i0, isSelected, cellHasFocus);

            lblItem.setBorder(new EmptyBorder(4, 3, 4, 3));
            lblItem.setBackground(FSkin.getColor(hasFocus() ? FSkin.Colors.CLR_ACTIVE : FSkin.Colors.CLR_INACTIVE));
            lblItem.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            lblItem.setFont(FSkin.getFont(13));
            lblItem.setOpaque(isSelected);
            return lblItem;
        }
    }
}
