package forge.view.toolbox;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

import forge.AllZone;
/** 
 * A JList object using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FList extends JList {
    private FSkin skin;
    /** 
     * A JList object using Forge skin properties.
     * This constructor assumes list contents are null and will be set later.
     *
     */
    public FList() {
        this(null);
    }

    /**
     * A JList object using Forge skin properties.
     * This constructor may be passed an object array of list contents.
     * 
     * @param o0 {@link java.lang.Object}[]
     */
    public FList(Object[] o0) {
        super(o0);
        skin = AllZone.getSkin();

        setOpaque(false);

        ListCellRenderer renderer = new ComplexCellRenderer();
        setCellRenderer(renderer);
    }

    private class ComplexCellRenderer implements ListCellRenderer {
        private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        public Component getListCellRendererComponent(JList lst0, Object val0, int i0,
            boolean isSelected, boolean cellHasFocus) {

            JLabel lblItem = (JLabel) defaultRenderer.getListCellRendererComponent(
                    lst0, val0, i0, isSelected, cellHasFocus);

            lblItem.setBorder(new EmptyBorder(4, 3, 4, 3));
            lblItem.setBackground(skin.getColor("active"));
            lblItem.setForeground(skin.getColor("text"));
            lblItem.setFont(skin.getFont1().deriveFont(Font.PLAIN, 13));
            lblItem.setOpaque(isSelected);
            return lblItem;
        }
    }
}
