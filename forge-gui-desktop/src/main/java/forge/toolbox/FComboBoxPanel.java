package forge.toolbox;

import forge.toolbox.FSkin.SkinnedLabel;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;

/** 
 * Panel with combo box and caption (either FComboBoxWrapper or FComboBoxPanel should be used instead of FComboBox so skinning works)
 *
 */
@SuppressWarnings("serial")
public class FComboBoxPanel<E> extends JPanel {

    private static final ArrayList<FComboBoxPanel<?>> allPanels = new ArrayList<FComboBoxPanel<?>>();

    public static void refreshAllSkins() {
        for (FComboBoxPanel<?> panel : allPanels) {
            panel.refreshSkin();
        }
    }

    private String comboBoxCaption = "";
    private FComboBox<E> comboBox = null;
    
    public FComboBoxPanel(String comboBoxCaption) {
        super();
        this.comboBoxCaption = comboBoxCaption;
        applyLayoutAndSkin();
        allPanels.add(this);
    }
            
    public void setComboBox(FComboBox<E> comboBox, E selectedItem) {
        removeExistingComboBox();
        this.comboBox = comboBox;
        this.comboBox.setSelectedItem(selectedItem);        
        setComboBoxLayout();      
    }
    
    private void removeExistingComboBox() {
        if (this.comboBox != null) {
            this.remove(this.comboBox);
            this.comboBox = null;
        }        
    }
    
    private void applyLayoutAndSkin() {        
        setPanelLayout();
        setLabelLayout();
        setComboBoxLayout();
    }
    
    private void setPanelLayout() {
        FlowLayout panelLayout = new FlowLayout(FlowLayout.LEFT);
        panelLayout.setVgap(0);
        this.setLayout(panelLayout);
        this.setOpaque(false);        
    }
    
    private void setLabelLayout() {
        if (this.comboBoxCaption != null && !this.comboBoxCaption.isEmpty()) {
            SkinnedLabel comboLabel = new SkinnedLabel(this.comboBoxCaption);
            comboLabel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            comboLabel.setFont(FSkin.getBoldFont(12));
            this.add(comboLabel);            
        }
    }
    
    private void setComboBoxLayout() {
        if (this.comboBox != null) {
            this.comboBox.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
            this.comboBox.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            this.comboBox.setFont(FSkin.getFont(12));
            this.comboBox.setEditable(false);
            this.comboBox.setFocusable(true);
            this.comboBox.setOpaque(true);                
            this.add(this.comboBox);
        }
    }
    
    public void setSelectedItem(Object item) {
        this.comboBox.setSelectedItem(item);
    }
    
    public Object getSelectedItem() {
        return this.comboBox.getSelectedItem();
    }
    
    private void refreshSkin() {
        this.comboBox = FComboBoxWrapper.refreshComboBoxSkin(this.comboBox);
    }
}
