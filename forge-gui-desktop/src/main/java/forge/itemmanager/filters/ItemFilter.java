package forge.itemmanager.filters;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.base.Predicate;

import forge.gui.framework.ILocalRepaint;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedCheckBox;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTextField;
import forge.toolbox.LayoutHelper;
import forge.util.Localizer;


public abstract class ItemFilter<T extends InventoryItem> {
    public final static int PANEL_HEIGHT = 28;

    public static void layoutCheckbox(SkinnedCheckBox cb) {
        cb.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        cb.setFont(FSkin.getFont());
        cb.setOpaque(false);
        cb.setFocusable(false);
    }

    protected final ItemManager<? super T> itemManager;
    private FilterPanel panel;
    private Widget widget;
    private final SkinnedCheckBox chkEnable = new SkinnedCheckBox();
    private final RemoveButton btnRemove = new RemoveButton();

    protected ItemFilter(ItemManager<? super T> itemManager0) {
        this.itemManager = itemManager0;
        this.chkEnable.setSelected(true); //enable by default
    }

    public JPanel getPanel() {
        if (this.panel == null) {
            this.panel = new FilterPanel();

            layoutCheckbox(this.chkEnable);
            this.chkEnable.addItemListener(arg0 -> {
                updateEnabled();
                applyChange();
            });
            this.panel.add(this.chkEnable);

            getWidget(); //initialize widget
            if (!isEnabled()) {
                updateEnabled();
            }
            this.panel.add(this.widget);
            this.panel.add(this.btnRemove);
        }
        return this.panel;
    }

    public JPanel getWidget() {
        if (this.widget == null) {
            this.widget = new Widget();
            this.buildWidget(this.widget);
        }
        return this.widget;
    }

    public void refreshWidget() {
        if (this.widget == null) { return; }
        this.widget.removeAll();
        this.buildWidget(this.widget);
    }

    public Component getMainComponent() {
        return getWidget();
    }

    public boolean isEnabled() {
        return this.chkEnable.isSelected();
    }

    public void setEnabled(boolean enabled0) {
        this.chkEnable.setSelected(enabled0);
    }

    public boolean allowRemove() {
        return this.btnRemove.isVisible();
    }
    public void setAllowRemove(boolean b0) {
        this.btnRemove.setVisible(b0);
    }

    public void updateEnabled() {
        boolean enabled = this.isEnabled();
        for (Component comp : this.widget.getComponents()) {
            comp.setEnabled(enabled);
        }
    }

    protected void applyChange() {
        this.itemManager.applyFilters();
    }

    public final <U extends InventoryItem> Predicate<U> buildPredicate(Class<U> genericType) {
        final Predicate<T> predicate = this.buildPredicate();
        return item -> {
            try {
                //noinspection unchecked
                return predicate.apply((T)item);
            }
            catch (Exception ex) {
                return showUnsupportedItem(item); //if can't cast U to T, filter item out unless derived class can handle it
            }
        };
    }

    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        return false; //don't show unsupported items by default
    }

    public abstract ItemFilter<T> createCopy();
    public abstract boolean isEmpty();
    public abstract void reset();
    public void afterFiltersApplied() {
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    public abstract boolean merge(ItemFilter<?> filter);

    protected abstract void buildWidget(JPanel widget);
    protected abstract void doWidgetLayout(LayoutHelper helper);
    protected abstract Predicate<T> buildPredicate();

    @SuppressWarnings("serial")
    private class FilterPanel extends SkinnedPanel {
        private FilterPanel() {
            setLayout(null);
            setOpaque(false);
            this.setBorder(new FSkin.MatteSkinBorder(0, 0, 1, 0, FSkin.getColor(Colors.CLR_TEXT)));
        }

        @Override
        public void doLayout() {
            LayoutHelper helper = new LayoutHelper(this);
            int removeButtonSize = allowRemove() ? 17 : 0;
            helper.include(chkEnable, 23, FTextField.HEIGHT);
            helper.offset(-3, 0); //avoid extra padding between checkbox and widget
            helper.fillLine(widget, PANEL_HEIGHT, removeButtonSize); //leave room for remove button
            if (removeButtonSize > 0) {
                helper.offset(-3, (PANEL_HEIGHT - removeButtonSize) / 2 - 1); //shift position of remove button
                helper.include(btnRemove, removeButtonSize, removeButtonSize);
            }
        }
    }

    @SuppressWarnings("serial")
    private class Widget extends JPanel {
        private Widget() {
            setLayout(null);
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            LayoutHelper helper = new LayoutHelper(this);
            ItemFilter.this.doWidgetLayout(helper);
        }
    }

    @SuppressWarnings("serial")
    private class RemoveButton extends JLabel implements ILocalRepaint {
        private final SkinColor iconColor = FSkin.getColor(Colors.CLR_TEXT);
        private boolean pressed, hovered;

        private RemoveButton() {
            setToolTipText(Localizer.getInstance().getMessage("lblRemoveFilter"));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!RemoveButton.this.isEnabled()) { return; }
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        pressed = true;
                        repaintSelf();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (pressed && SwingUtilities.isLeftMouseButton(e)) {
                        pressed = false;
                        if (hovered) { //only handle click if mouse released over button
                            repaintSelf();
                            itemManager.focus();
                            itemManager.removeFilter(ItemFilter.this);
                        }
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!RemoveButton.this.isEnabled()) { return; }
                    hovered = true;
                    repaintSelf();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hovered) {
                        hovered = false;
                        repaintSelf();
                    }
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled0) {
            if (!enabled0 && hovered) {
                hovered = false; //ensure hovered reset if disabled
            }
            super.setEnabled(enabled0);
        }

        @Override
        public void repaintSelf() {
            final Dimension d = this.getSize();
            repaint(0, 0, d.width, d.height);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int thickness = 2;
            int offset = 4;
            int x1 = offset;
            int y1 = offset;
            int x2 = getWidth() - offset - 1;
            int y2 = getHeight() - offset - 1;

            Graphics2D g2d = (Graphics2D) g;
            if (hovered) {
                if (pressed) {
                    g.translate(1, 1); //translate icon to give pressed button look
                }
                FSkin.setGraphicsColor(g2d, iconColor);
            }
            else {
                FSkin.setGraphicsColor(g2d, iconColor.alphaColor(150));
            }
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setStroke(new BasicStroke(thickness));
            g2d.drawLine(x1, y1, x2, y2);
            g2d.drawLine(x2, y1, x1, y2);
        }
    }
}
