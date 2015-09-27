package forge.toolbox;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.google.common.collect.ImmutableList;

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
    private int flowLayout;

    public FComboBoxPanel(final String comboBoxCaption0) {
        this(comboBoxCaption0, FlowLayout.LEFT);
    }
    public FComboBoxPanel(final String comboBoxCaption0, int flowLayout0) {
        super();
        comboBoxCaption = comboBoxCaption0;
        flowLayout = flowLayout0;
        applyLayoutAndSkin();
        allPanels.add(this);
    }

    public FComboBoxPanel(final String comboBoxCaption0, Iterable<E> items) {
        this(comboBoxCaption0, FlowLayout.LEFT, items);
    }
    public FComboBoxPanel(final String comboBoxCaption0, int flowLayout0, Iterable<E> items) {
        this(comboBoxCaption0, flowLayout0);

        List<E> list = ImmutableList.copyOf(items);
        setComboBox(new FComboBox<E>(list), list.get(0));
    }

    public void setComboBox(final FComboBox<E> comboBox0, final E selectedItem) {
        removeExistingComboBox();
        comboBox = comboBox0;
        comboBox.setSelectedItem(selectedItem);
        setComboBoxLayout();
    }

    private void removeExistingComboBox() {
        if (comboBox != null) {
            remove(comboBox);
            comboBox = null;
        }
    }

    private void applyLayoutAndSkin() {
        setPanelLayout();
        setLabelLayout();
        setComboBoxLayout();
    }

    private void setPanelLayout() {
        final FlowLayout panelLayout = new FlowLayout(flowLayout);
        panelLayout.setVgap(0);
        setLayout(panelLayout);
        setOpaque(false);
    }

    private void setLabelLayout() {
        if (comboBoxCaption != null && !comboBoxCaption.isEmpty()) {
            final SkinnedLabel comboLabel = new SkinnedLabel(comboBoxCaption);
            comboLabel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            comboLabel.setFont(FSkin.getBoldFont(12));
            add(comboLabel);
        }
    }

    private void setComboBoxLayout() {
        if (comboBox != null) {
            comboBox.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
            comboBox.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            comboBox.setFont(FSkin.getFont(12));
            comboBox.setEditable(false);
            comboBox.setFocusable(true);
            comboBox.setOpaque(true);
            add(comboBox);
        }
    }

    public void addActionListener(final ActionListener l) {
        comboBox.addActionListener(l);
    }

    public void setSelectedItem(final Object item) {
        comboBox.setSelectedItem(item);
    }

    public E getSelectedItem() {
        return comboBox.getSelectedItem();
    }

    private void refreshSkin() {
        comboBox = FComboBoxWrapper.refreshComboBoxSkin(comboBox);
    }
}
