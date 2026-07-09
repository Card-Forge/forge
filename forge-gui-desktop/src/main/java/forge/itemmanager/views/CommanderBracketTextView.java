package forge.itemmanager.views;

import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
abstract class CommanderBracketTextView<T extends InventoryItem> extends ItemView<T> {
    private final FPanel panel = new FPanel(new BorderLayout());
    private final JTextArea textArea = new JTextArea();
    private int selectedIndex = -1;

    CommanderBracketTextView(final ItemManager<T> itemManager0, final ItemManagerModel<T> model0) {
        super(itemManager0, model0);
        this.panel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));
        this.panel.setBorderToggle(false);
        this.textArea.setEditable(false);
        this.textArea.setLineWrap(true);
        this.textArea.setWrapStyleWord(true);
        this.textArea.setOpaque(false);
        this.textArea.setFont(FSkin.getFont(13).getBaseFont());
        this.textArea.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        this.textArea.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        this.textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        this.panel.add(textArea, BorderLayout.CENTER);
        this.getButton().setBorder(new EmptyBorder(4, 0, 0, 0));
        this.getPnlOptions().setVisible(false);
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void setup(final ItemManagerConfig config, final Map<ColumnDef, ItemTableColumn> colOverrides) {
    }

    @Override
    public void setAllowMultipleSelections(final boolean allowMultipleSelections) {
    }

    @Override
    public T getItemAtIndex(final int index) {
        final List<Map.Entry<T, Integer>> items = model.getOrderedList();
        if (index < 0 || index >= items.size()) {
            return null;
        }
        return items.get(index).getKey();
    }

    @Override
    public int getIndexOfItem(final T item) {
        final List<Map.Entry<T, Integer>> items = model.getOrderedList();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getKey().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override
    public Iterable<Integer> getSelectedIndices() {
        return selectedIndex < 0 ? Collections.emptyList() : Collections.singletonList(selectedIndex);
    }

    @Override
    public void selectAll() {
    }

    @Override
    public int getCount() {
        return model.getOrderedList().size();
    }

    @Override
    public int getSelectionCount() {
        return selectedIndex < 0 ? 0 : 1;
    }

    @Override
    public int getIndexAtPoint(final Point p) {
        return selectedIndex;
    }

    @Override
    protected FSkin.SkinImage getIcon() {
        return null;
    }

    @Override
    protected String getButtonText() {
        return "B";
    }

    @Override
    protected void configureTextButton(final FLabel.Builder buttonBuilder) {
        buttonBuilder.fontStyle(Font.BOLD).fontSize(18);
    }

    @Override
    protected String getCaption() {
        return localizer.getMessage("lblBracketView");
    }

    @Override
    protected void onSetSelectedIndex(final int index) {
        selectedIndex = index;
        updateText();
        onSelectionChange();
    }

    @Override
    protected void onSetSelectedIndices(final Iterable<Integer> indices) {
        final List<Integer> indexList = new ArrayList<>();
        for (final Integer index : indices) {
            indexList.add(index);
        }
        selectedIndex = indexList.isEmpty() ? -1 : indexList.get(0);
        updateText();
        onSelectionChange();
    }

    @Override
    protected void onScrollSelectionIntoView(final JViewport viewport) {
    }

    @Override
    protected void onResize() {
    }

    @Override
    protected void onRefresh() {
        if (selectedIndex >= getCount()) {
            selectedIndex = getCount() - 1;
        }
        updateText();
    }

    protected final void updateText() {
        textArea.setText(getText());
        textArea.setCaretPosition(0);
    }

    protected abstract String getText();
}
