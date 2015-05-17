package forge.toolbox;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import forge.toolbox.FSkin.SkinnedLabel;

/**
 * Panel with combo box and caption (either FComboBoxWrapper or FComboBoxPanel should be used instead of FComboBox so skinning works)
 *
 */
@SuppressWarnings("serial")
public class FComboBoxPanel<E> extends JPanel {

    private static final List<FComboBoxPanel<?>> allPanels = new ArrayList<FComboBoxPanel<?>>();

    public static void refreshAllSkins() {
        for (final FComboBoxPanel<?> panel : allPanels) {
            panel.refreshSkin();
        }
    }

    private String comboBoxCaption = "";
    private FComboBox<E> comboBox = null;

    public FComboBoxPanel(final String comboBoxCaption) {
        super();
        this.comboBoxCaption = comboBoxCaption;
        applyLayoutAndSkin();
        allPanels.add(this);
    }

    public void setComboBox(final FComboBox<E> comboBox, final E selectedItem) {
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
        final FlowLayout panelLayout = new FlowLayout(FlowLayout.LEFT);
        panelLayout.setVgap(0);
        this.setLayout(panelLayout);
        this.setOpaque(false);
    }

    private void setLabelLayout() {
        if (this.comboBoxCaption != null && !this.comboBoxCaption.isEmpty()) {
            final SkinnedLabel comboLabel = new SkinnedLabel(this.comboBoxCaption);
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

    public void setSelectedItem(final Object item) {
        this.comboBox.setSelectedItem(item);
    }

    public Object getSelectedItem() {
        return this.comboBox.getSelectedItem();
    }

    private void refreshSkin() {
        this.comboBox = FComboBoxWrapper.refreshComboBoxSkin(this.comboBox);
    }
}
