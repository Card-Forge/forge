package forge.gui.toolbox.itemmanager.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;

import forge.ImageCache;
import forge.gui.deckeditor.DeckProxy;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CPicture;
import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinImage;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.ItemManagerModel;
import forge.item.InventoryItem;
import forge.view.arcane.CardArea;
import forge.view.arcane.CardPanel;

public class ImageView<T extends InventoryItem> extends ItemView<T> {
    private static final float GAP_SCALE_FACTOR = 0.04f;

    public enum LayoutType {
        Spreadsheet,
        Piles
    }

    private final CardViewDisplay display;
    private List<Integer> selectedIndices = new ArrayList<Integer>();
    private int imageScaleFactor = 3;
    private boolean allowMultipleSelections;
    private LayoutType layoutType = LayoutType.Spreadsheet;

    public ImageView(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        super(itemManager0, model0);
        this.display = new CardViewDisplay();
        this.display.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftMouseDown(MouseEvent e) {
                selectItem(e);
            }

            @Override
            public void onLeftDoubleClick(MouseEvent e) {
                itemManager.activateSelectedItems();
            }

            @Override
            public void onRightClick(MouseEvent e) {
                selectItem(e);
                itemManager.showContextMenu(e);
            }

            private void selectItem(MouseEvent e) {
                focus();

                ItemInfo item = display.getItemAtPoint(e.getPoint());
                if (item == null) { return; }

                if (item.selected) {
                    //toggle selection off item if Control down and left mouse down, otherwise do nothing
                    if (e.getButton() != 1) {
                        return;
                    }
                    if (e.isControlDown() && allowMultipleSelections) {
                        item.selected = false;
                        selectedIndices.remove(item.index);
                        onSelectionChange();
                        item.scrollIntoView();
                        return;
                    }
                }
                if (!allowMultipleSelections || (!e.isControlDown() && !e.isShiftDown())) {
                    clearSelection();
                }
                selectedIndices.add(0, item.index);
                item.selected = true;
                onSelectionChange();
                item.scrollIntoView();
            }

            @Override
            public void onMouseExit(MouseEvent e) {
                if (display.updateHoveredItem(null, null)) {
                    display.repaint();
                }
            }
        });
        display.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                FScrollPane scroller = ImageView.this.getScroller();
                Point hoverScrollPos = new Point(scroller.getHorizontalScrollBar().getValue(), scroller.getVerticalScrollBar().getValue());
                if (display.updateHoveredItem(e.getPoint(), hoverScrollPos)) {
                    display.repaint();
                }
            }
        });
    }

    @Override
    protected void onResize() {
        if (this.layoutType == LayoutType.Spreadsheet) {
            display.refresh(); //need to refresh to adjust wrapping of items
        }
    }

    @Override
    protected void onRefresh() {
        display.refresh();
    }

    @Override
    public JComponent getComponent() {
        return display;
    }

    @Override
    public void setAllowMultipleSelections(boolean allowMultipleSelections0) {
        this.allowMultipleSelections = allowMultipleSelections0;
    }

    @Override
    public T getItemAtIndex(int index) {
        if (index >= 0 && index < getCount()) {
            return display.items.get(index).item;
        }
        return null;
    }

    @Override
    public int getIndexOfItem(T item) {
        for (int i = getCount() - 1; i >= 0; i--) {
            ItemInfo itemInfo = display.items.get(i);
            if (itemInfo.item == item) {
                return itemInfo.index;
            }
        }
        return 0;
    }

    @Override
    public int getSelectedIndex() {
        return selectedIndices.isEmpty() ? -1 : selectedIndices.get(0);
    }

    @Override
    public Iterable<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    @Override
    public int getCount() {
        return display.items.size();
    }

    @Override
    public int getSelectionCount() {
        return selectedIndices.size();
    }

    @Override
    public int getIndexAtPoint(Point p) {
        ItemInfo item = display.getItemAtPoint(p);
        if (item != null) {
            return item.index;
        }
        return -1;
    }

    @Override
    protected SkinImage getIcon() {
        if (itemManager.getGenericType().equals(DeckProxy.class)) {
            return FSkin.getImage(FSkin.EditorImages.IMG_PACK).resize(18, 18);
        }
        return FSkin.getIcon(FSkin.InterfaceIcons.ICO_CARD_IMAGE);
    }

    @Override
    protected String getCaption() {
        return "Image View";
    }

    @Override
    public void selectAll() {
        clearSelection();
        for (Integer i = 0; i < display.items.size(); i++) {
            selectedIndices.add(i);
        }
        updateSelection();
    }

    @Override
    protected void onSetSelectedIndex(int index) {
        clearSelection();
        selectedIndices.add(index);
        updateSelection();
    }

    @Override
    protected void onSetSelectedIndices(Iterable<Integer> indices) {
        clearSelection();
        for (Integer index : indices) {
            selectedIndices.add(index);
        }
        updateSelection();
    }

    private void clearSelection() {
        int count = getCount();
        for (Integer i : selectedIndices) {
            if (i < count) {
                display.items.get(i).selected = false;
            }
        }
        selectedIndices.clear();
    }

    private void updateSelection() {
        for (Integer i : selectedIndices) {
            display.items.get(i).selected = true;
        }
        onSelectionChange();
    }

    @Override
    protected void onSelectionChange() {
        super.onSelectionChange();
        display.repaint();
    }

    @Override
    protected void onScrollSelectionIntoView(JViewport viewport) {
        if (selectedIndices.isEmpty()) { return; }

        ItemInfo itemInfo = display.items.get(selectedIndices.get(0));
        itemInfo.scrollIntoView();
    }

    private class DisplayArea {
        private final Rectangle bounds = new Rectangle();

        public Rectangle getBounds() {
            return this.bounds;
        }
        public void setBounds(int x, int y, int width, int height) {
            this.bounds.x = x;
            this.bounds.y = y;
            this.bounds.width = width;
            this.bounds.height = height;
        }
        public void scrollIntoView() {
            int x = this.bounds.x - CardArea.GUTTER_X;
            int y = this.bounds.y - CardArea.GUTTER_Y;
            int width = this.bounds.width + 2 * CardArea.GUTTER_Y;
            int height = this.bounds.height + 2 * CardArea.GUTTER_Y;
            display.scrollRectToVisible(new Rectangle(x, y, width, height));
        }
    }
    private class Section extends DisplayArea {
        private final List<Pile> piles = new ArrayList<Pile>();
        private boolean isCollapsed;
    }
    private class Pile extends DisplayArea {
        private final List<ItemInfo> items = new ArrayList<ItemInfo>();
    }
    private class ItemInfo extends DisplayArea {
        private final T item;
        private Integer index;
        private boolean selected;

        private ItemInfo(T item0, int index0) {
            this.item = item0;
            this.index = index0;
        }

        @Override
        public String toString() {
            return this.item.toString();
        }
    }

    @SuppressWarnings("serial")
    private class CardViewDisplay extends JPanel {
        private Point hoverPoint;
        private Point hoverScrollPos;
        private ItemInfo hoveredItem;
        private List<ItemInfo> items = new ArrayList<ItemInfo>();
        private List<Section> sections = new ArrayList<Section>();

        private CardViewDisplay() {
            this.setOpaque(false);
            this.setFocusable(true);
        }

        private void refresh() {
            int index = 0;
            this.items.clear();
            for (Entry<T, Integer> itemEntry : model.getOrderedList()) {
                for (int i = 0; i < itemEntry.getValue(); i++) {
                    this.items.add(new ItemInfo(itemEntry.getKey(), index++));
                }
            }
            this.refreshSections();
        }

        private void refreshSections() {
            this.sections.clear();

            if (!this.items.isEmpty()) {
                switch (ImageView.this.layoutType) {
                case Spreadsheet:
                    buildSpreadsheet();
                    break;
                case Piles:
                    buildPiles();
                    break;
                }
            }

            this.revalidate();
            this.repaint();
        }

        private ItemInfo getItemAtPoint(Point p) {
            for (int i = this.sections.size() - 1; i >= 0; i--) {
                Section section = this.sections.get(i);
                if (!section.isCollapsed && section.getBounds().contains(p)) {
                    for (int j = section.piles.size() - 1; j >= 0; j--) {
                        Pile pile = section.piles.get(j);
                        if (pile.getBounds().contains(p)) {
                            for (int k = pile.items.size() - 1; k >= 0; k--) {
                                ItemInfo item = pile.items.get(k);
                                if (item.getBounds().contains(p)) {
                                    return item;
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        private Dimension getVisibleSize() {
            FScrollPane scroller = ImageView.this.getScroller();
            Dimension size = ImageView.this.getScroller().getSize();
            Insets insets = ImageView.this.getScroller().getInsets();
            size =  new Dimension(size.width - insets.left - insets.right,
                    size.height - insets.top - insets.bottom);
            if (scroller.getVerticalScrollBarPolicy() != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
                size.width -= scroller.getVerticalScrollBar().getWidth();
            }
            if (scroller.getHorizontalScrollBarPolicy() != ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
                size.height -= scroller.getHorizontalScrollBar().getHeight();
            }
            return size;
        }

        private void buildSpreadsheet() {
            ImageView.this.getScroller().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            Section section = new Section();

            final int itemAreaWidth = getVisibleSize().width;
            int itemWidth = 50 * imageScaleFactor;
            int gap = Math.round(itemWidth * GAP_SCALE_FACTOR);
            int dx = itemWidth + gap;
            int itemsPerRow = (itemAreaWidth - 2 * CardArea.GUTTER_X + gap) / dx;
            if (itemsPerRow == 0) {
                itemsPerRow = 1;
                itemWidth = itemAreaWidth - 2 * CardArea.GUTTER_X;
            }
            int itemHeight = Math.round(itemWidth * CardPanel.ASPECT_RATIO);
            int dy = itemHeight + gap;

            Pile pile = new Pile(); //use a pile for each row
            int x = CardArea.GUTTER_X;
            int y = CardArea.GUTTER_Y;

            for (ItemInfo itemInfo : this.items) {
                if (pile.items.size() == itemsPerRow) {
                    pile = new Pile();
                    x = CardArea.GUTTER_X;
                    y += dy;
                }

                itemInfo.setBounds(x, y, itemWidth, itemHeight);

                if (pile.items.size() == 0) {
                    pile.setBounds(0, y, itemAreaWidth, dy);
                    section.piles.add(pile);
                }
                pile.items.add(itemInfo);
                x += dx;
            }

            section.setBounds(0, 0, itemAreaWidth, y + itemHeight + CardArea.GUTTER_Y);
            this.setPreferredSize(section.getBounds().getSize());

            this.sections.add(section);
        }

        private void buildPiles() {

        }

        private boolean updateHoveredItem(Point hoverPoint0, Point hoverScrollPos0) {
            this.hoverPoint = hoverPoint0;
            this.hoverScrollPos = hoverScrollPos0;

            ItemInfo item = null;
            FScrollPane scroller = ImageView.this.getScroller();
            if (hoverPoint0 != null) {
                Point displayPoint = new Point(hoverPoint0);
                //account for change in scroll positions since mouse last moved
                displayPoint.x += scroller.getHorizontalScrollBar().getValue() - hoverScrollPos0.x;
                displayPoint.y += scroller.getVerticalScrollBar().getValue() - hoverScrollPos0.y;
                item = this.getItemAtPoint(displayPoint);
            }

            if (this.hoveredItem == item) { return false; }
            this.hoveredItem = item;
            if (item != null) {
                CDetail.SINGLETON_INSTANCE.showCard(item.item);
                CPicture.SINGLETON_INSTANCE.showImage(item.item);
            }
            return true;
        }

        @Override
        public final void paintComponent(final Graphics g) {
            if (this.items.isEmpty()) { return; }

            updateHoveredItem(this.hoverPoint, this.hoverScrollPos); //ensure hovered item up to date

            final Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int sectionIdx = 0, pileIdx = 0;
            final int scrollTop = ImageView.this.getScroller().getVerticalScrollBar().getValue();
            final int scrollBottom = scrollTop + getVisibleSize().height;
            switch (ImageView.this.layoutType) {
            case Spreadsheet:
                pileIdx = scrollTop / this.sections.get(0).piles.get(0).getBounds().height;
                break;
            case Piles:
                break;
            }
            for (; sectionIdx < this.sections.size(); sectionIdx++, pileIdx = 0) {
                Section section = this.sections.get(sectionIdx);
                if (section.getBounds().y >= scrollBottom) {
                    break;
                }
                if (this.sections.size() > 1) {
                    //TODO: Draw section name/border
                    if (section.isCollapsed) {
                        continue;
                    }
                }
                for (; pileIdx < section.piles.size(); pileIdx++) {
                    Pile pile = section.piles.get(pileIdx);
                    if (pile.getBounds().y >= scrollBottom) {
                        break;
                    }
                    for (ItemInfo itemInfo : pile.items) {
                        if (itemInfo != this.hoveredItem) { //save hovered item for last
                            drawItemImage(g2d, itemInfo);
                        }
                    }
                }
            }
            if (this.hoveredItem != null) { //draw hovered item on top
                drawItemImage(g2d, this.hoveredItem);
            }
        }

        private void drawItemImage(Graphics2D g, ItemInfo itemInfo) {
            Rectangle bounds = itemInfo.getBounds();
            final int itemWidth = bounds.width;
            final int selBorderSize = Math.max(1, Math.round(itemWidth * GAP_SCALE_FACTOR / 2) - 1);
            final int borderSize = Math.round(itemWidth * CardPanel.BLACK_BORDER_SIZE);
            final int cornerSize = Math.max(4, Math.round(itemWidth * CardPanel.ROUNDED_CORNER_SIZE));

            if (itemInfo.selected) {
                g.setColor(Color.green);
                g.fillRoundRect(bounds.x - selBorderSize, bounds.y - selBorderSize,
                        bounds.width + 2 * selBorderSize, bounds.height + 2 * selBorderSize,
                        cornerSize + selBorderSize, cornerSize + selBorderSize);
            }
            else if (itemInfo == this.hoveredItem) {
                int hoverBorderSize = Math.max(1, selBorderSize / 2);
                g.setColor(Color.green);
                g.fillRoundRect(bounds.x - hoverBorderSize, bounds.y - hoverBorderSize,
                        bounds.width + 2 * hoverBorderSize, bounds.height + 2 * hoverBorderSize,
                        cornerSize + hoverBorderSize, cornerSize + hoverBorderSize);
            }

            g.setColor(Color.black);
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, cornerSize, cornerSize);

            BufferedImage img = ImageCache.getImage(itemInfo.item, bounds.width - 2 * borderSize, bounds.height - 2 * borderSize);
            if (img != null) {
                g.drawImage(img, null, bounds.x + borderSize, bounds.y + borderSize);
            }
            else {
                g.setColor(Color.white);
                Shape clip = g.getClip();
                g.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
                g.drawString(itemInfo.item.getName(), bounds.x + 10, bounds.y + 20);
                g.setClip(clip);
            }
        }
    }
}
