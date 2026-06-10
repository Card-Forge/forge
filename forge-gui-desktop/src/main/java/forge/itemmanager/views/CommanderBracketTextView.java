package forge.itemmanager.views;

import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
abstract class CommanderBracketTextView<T extends InventoryItem> extends ItemView<T> {
    private static final String ATTRIBUTION_URL = "https://commanderbracket.app/?ref=forge";
    private static final String LINK_COLOR = "#1f66cc";
    private final FPanel panel = new BracketPanel();
    private final JTextArea textArea = new JTextArea();
    private final JLabel attributionLabel = new JLabel();
    private final Timer refreshTimer = new Timer(1500, e -> updateText());
    private int selectedIndex = -1;

    CommanderBracketTextView(final ItemManager<T> itemManager0, final ItemManagerModel<T> model0) {
        super(itemManager0, model0);
        this.getScroller().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
        this.attributionLabel.setFont(FSkin.getFont(13).getBaseFont());
        this.attributionLabel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        this.attributionLabel.setText(getAttributionHtml());
        this.attributionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.attributionLabel.setBorder(new EmptyBorder(0, 8, 8, 8));
        this.attributionLabel.setToolTipText("<html>" + localizer.getMessage("lblCommanderBracketExplore")
                + "<br>" + ATTRIBUTION_URL + "</html>");
        this.attributionLabel.setVisible(false);
        this.attributionLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                MenuUtil.openUrlInBrowser(ATTRIBUTION_URL);
            }
        });
        this.refreshTimer.setRepeats(true);
        this.panel.add(textArea, BorderLayout.CENTER);
        this.panel.add(attributionLabel, BorderLayout.SOUTH);
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
        panel.revalidate();
    }

    @Override
    protected void onRefresh() {
        if (selectedIndex >= getCount()) {
            selectedIndex = getCount() - 1;
        }
        updateText();
    }

    protected final void updateText() {
        final String text = getText();
        final boolean hasAttribution = text.contains(getAttributionLabel());
        textArea.setText(stripAttribution(text));
        attributionLabel.setVisible(hasAttribution);
        textArea.setCaretPosition(0);
        updateRefreshTimer();
    }

    protected abstract String getText();

    protected boolean isRefreshPending() {
        return false;
    }

    private final class BracketPanel extends FPanel implements Scrollable {
        private BracketPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredSize() {
            final Dimension preferredSize = super.getPreferredSize();
            final int viewportWidth = getScroller().getViewport().getWidth();
            if (viewportWidth > 0) {
                preferredSize.width = viewportWidth;
            }
            return preferredSize;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            final int viewportHeight = getScroller().getViewport().getHeight();
            return viewportHeight > 0 && getPreferredSize().height < viewportHeight;
        }
    }

    private void updateRefreshTimer() {
        if (isRefreshPending()) {
            if (!refreshTimer.isRunning()) {
                refreshTimer.start();
            }
        }
        else if (refreshTimer.isRunning()) {
            refreshTimer.stop();
        }
    }

    private String getAttributionLabel() {
        return localizer.getMessage("lblCommanderBracketAttribution");
    }

    private String getAttributionHtml() {
        return "<html>" + localizer.getMessage("lblCommanderBracketPoweredBy")
                + " <font color=\"" + LINK_COLOR + "\"><u>"
                + localizer.getMessage("lblCommanderBracketSiteName")
                + "</u></font></html>";
    }

    private String stripAttribution(final String text) {
        final StringBuilder result = new StringBuilder();
        final String[] lines = text.split("\\R", -1);
        for (final String line : lines) {
            if (getAttributionLabel().equals(line.trim())) {
                continue;
            }
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(line);
        }
        return result.toString();
    }
}
