package forge.gui.toolbox;

import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class FComboBoxPanel<E> extends JPanel {
    
    private String comboBoxCaption = "";
    private final JComboBox<E> comboBox;
            
    public FComboBoxPanel(E[] comboBoxItems, String comboBoxCaption) {
        super();
        this.comboBox = new JComboBox<E>(comboBoxItems);
        this.comboBoxCaption = comboBoxCaption;
        applyLayoutAndSkin();
    }
        
    public final JComboBox<E> getJComboBox() {
        return this.comboBox;
    }

    private void applyLayoutAndSkin() {        
                
        FlowLayout panelLayout = new FlowLayout(FlowLayout.LEFT);
        panelLayout.setVgap(0);
        this.setLayout(panelLayout);
        this.setOpaque(false);

        if (this.comboBoxCaption != "") {
            JLabel comboLabel;
            comboLabel = new JLabel(this.comboBoxCaption);
            comboLabel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            comboLabel.setFont(FSkin.getBoldFont(12));
            this.add(comboLabel);            
        }
                
        this.comboBox.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        this.comboBox.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.comboBox.setFont(FSkin.getFont(12));
        this.comboBox.setEditable(false);
        this.comboBox.setFocusable(true);
        this.comboBox.setOpaque(true);       
        this.comboBox.setRenderer(new ComplexCellRenderer<E>());         
        this.add(this.comboBox);
        
    }
            
    private class ComplexCellRenderer<E1> implements ListCellRenderer<E1> {

        private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        /* (non-Javadoc)
         * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends E1> lst0, E1 val0, int i0,
                boolean isSelected, boolean cellHasFocus) {

            JLabel lblItem = (JLabel) defaultRenderer.getListCellRendererComponent(
                    lst0, val0, i0, isSelected, cellHasFocus);

            lblItem.setBorder(new EmptyBorder(4, 3, 4, 3));
            lblItem.setBackground(FSkin.getColor(hasFocus() ? FSkin.Colors.CLR_ACTIVE : FSkin.Colors.CLR_INACTIVE));
            lblItem.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            lblItem.setFont(FSkin.getFont(12));
            lblItem.setOpaque(isSelected);
            return lblItem;

        }

    }

}
