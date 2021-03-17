package forge.itemmanager.filters;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import forge.gui.GuiUtils;
import forge.gui.UiCommand;
import forge.gui.interfaces.IButton;
import forge.item.InventoryItem;
import forge.itemmanager.AdvancedSearch;
import forge.itemmanager.ItemManager;
import forge.itemmanager.AdvancedSearch.IFilterControl;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTextField;
import forge.toolbox.LayoutHelper;
import forge.util.Localizer;

import javax.swing.*;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;


public class AdvancedSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    private final AdvancedSearch.Model<T> model;
    private FLabel label;
    private EditDialog editDialog;

    public AdvancedSearchFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
        model = new AdvancedSearch.Model<>();
    }

    @Override
    public final boolean isEmpty() {
        return model.isEmpty();
    }

    @Override
    public void reset() {
        model.reset();
        editDialog = null;
    }

    @Override
    public ItemFilter<T> createCopy() {
        return new AdvancedSearchFilter<>(itemManager);
    }

    @Override
    protected Predicate<T> buildPredicate() {
        return model.getPredicate();
    }

    @Override
    protected final void buildWidget(JPanel widget) {
        label = new FLabel.Builder().fontAlign(SwingConstants.LEFT).fontSize(12).build();
        label.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftDoubleClick(final MouseEvent e) {
                edit();
            }

            @Override
            public void onRightClick(final MouseEvent e) {
                final JPopupMenu menu = new JPopupMenu("AdvancedSearchContextMenu");

                boolean hasFilters = !isEmpty();
                if (hasFilters) {
                    //add a menu item for each filter to allow easily editing just that filter
                    for (final IFilterControl<T> control : model.getControls()) {
                        GuiUtils.addMenuItem(menu, FSkin.encodeSymbols(control.getFilter().toString(), false), null, new Runnable() {
                            @Override
                            public void run() {
                                model.editFilterControl(control, onFilterChange);
                            }
                        });
                    }
                    GuiUtils.addSeparator(menu);
                }

                GuiUtils.addMenuItem(menu, Localizer.getInstance().getMessage("lblEditExpression"), null, new Runnable() {
                    @Override
                    public void run() {
                        edit();
                    }
                });

                if (hasFilters) {
                    GuiUtils.addMenuItem(menu, Localizer.getInstance().getMessage("lblClearFilter"), null, new Runnable() {
                        @Override
                        public void run() {
                            reset();
                            itemManager.applyNewOrModifiedFilter(AdvancedSearchFilter.this);
                        }
                    });
                }

                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        model.setLabel(label);
        widget.add(label);
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        helper.fillLine(label, FTextField.HEIGHT);
    }

    public boolean edit() {
        if (editDialog == null) {
            editDialog = new EditDialog();
        }
        return editDialog.show();
    }

    @Override
    public boolean merge(ItemFilter<?> filter) {
        return false;
    }

    private final Runnable onFilterChange = new Runnable() {
        @Override
        public void run() {
            //update expression when edit screen closed or a single filter is changed
            model.updateExpression();
            itemManager.applyNewOrModifiedFilter(AdvancedSearchFilter.this);
        }
    };

    @SuppressWarnings("serial")
    private class EditDialog {
        private static final int WIDTH = 400;
        private static final int HEIGHT = 500;

        private final JPanel panel;
        private final FScrollPane scroller;
        private FOptionPane optionPane;

        private EditDialog() {
            panel = new JPanel(null) {
                @Override
                public void doLayout() {
                    int x = 0;
                    int y = 0;
                    int w = getWidth();
                    int h = 100;

                    for (Component child : getComponents()) {
                        child.setBounds(x, y, w, h);
                        y += h;
                    }
                }  
            };
            panel.setOpaque(false);
            scroller = new FScrollPane(panel, false, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroller.setMinimumSize(new Dimension(WIDTH, HEIGHT));

            Filter filter = new Filter();
            model.addFilterControl(filter);
            panel.add(filter);
        }

        private boolean show() {
            optionPane = new FOptionPane(null, Localizer.getInstance().getMessage("lblAdvancedSearch"), null, scroller, ImmutableList.of(Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel")), 0);
            optionPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    //automatically edit first filter if search is empty
                    if (model.isEmpty()) {
                        model.editFilterControl(Iterables.getFirst(model.getControls(), null), onFilterChange);
                    }
                }
            });
            scroller.revalidate();
            scroller.repaint();
            optionPane.setVisible(true);

            int result = optionPane.getResult();

            optionPane.dispose();
            if (result != 1) {
                onFilterChange.run();
                return true;
            }
            return false;
        }

        private void addNewFilter(Filter fromFilter) {
            if (panel.getComponent(panel.getComponentCount() - 1) == fromFilter) {
                Filter filter = new Filter();
                model.addFilterControl(filter);
                panel.add(filter);
                panel.revalidate();
                panel.repaint();
                scroller.scrollToBottom();
            }
        }

        @SuppressWarnings("unchecked")
        private void removeNextFilter(Filter fromFilter) {
            int index = ArrayUtils.indexOf(panel.getComponents(), fromFilter);
            if (index < panel.getComponentCount() - 1) {
                Filter nextFilter = (Filter)panel.getComponent(index + 1);
                model.removeFilterControl(nextFilter);
                panel.remove(nextFilter);
                panel.revalidate();
            }
        }

        private class Filter extends SkinnedPanel implements AdvancedSearch.IFilterControl<T> {
            private final FLabel btnNotBeforeParen, btnOpenParen, btnNotAfterParen;
            private final FLabel btnFilter;
            private final FLabel btnCloseParen, btnAnd, btnOr;
            private AdvancedSearch.Filter<T> filter;

            private Filter() {
                super(null);
                setOpaque(false);

                btnNotBeforeParen = new FLabel.Builder().fontAlign(SwingConstants.CENTER).text("NOT").hoverable().selectable().build();
                btnOpenParen = new FLabel.Builder().fontAlign(SwingConstants.CENTER).text("(").hoverable().selectable().build();
                btnNotAfterParen = new FLabel.Builder().fontAlign(SwingConstants.CENTER).text("NOT").hoverable().selectable().build();
                btnFilter = new FLabel.ButtonBuilder().build();
                btnCloseParen = new FLabel.Builder().fontAlign(SwingConstants.CENTER).hoverable().selectable().text(")").build();
                btnAnd = new FLabel.Builder().fontAlign(SwingConstants.CENTER).text("AND").hoverable().selectable().cmdClick(new UiCommand() {
                    @Override
                    public void run() {
                        if (btnAnd.isSelected()) {
                            btnOr.setSelected(false);
                            addNewFilter(Filter.this);
                        }
                        else {
                            removeNextFilter(Filter.this);
                        }
                    }
                }).build();
                btnOr = new FLabel.Builder().fontAlign(SwingConstants.CENTER).text("OR").hoverable().selectable().cmdClick(new UiCommand() {
                    @Override
                    public void run() {
                        if (btnOr.isSelected()) {
                            btnAnd.setSelected(false);
                            addNewFilter(Filter.this);
                        }
                        else {
                            removeNextFilter(Filter.this);
                        }
                    }
                }).build();

                add(btnNotBeforeParen);
                add(btnOpenParen);
                add(btnNotAfterParen);
                add(btnFilter);
                add(btnCloseParen);
                add(btnAnd);
                add(btnOr);
            }

            @Override
            public void doLayout() {
                int padding = 5;
                int width = getWidth();
                int height = getHeight();
                int buttonWidth = (width - padding * 4) / 3;
                int buttonHeight = (height - padding * 3) / 3;

                int x = padding;
                int y = padding;
                int dx = buttonWidth + padding;
                int dy = buttonHeight + padding;

                btnNotBeforeParen.setBounds(x, y, buttonWidth, buttonHeight);
                x += dx;
                btnOpenParen.setBounds(x, y, buttonWidth, buttonHeight);
                x += dx;
                btnNotAfterParen.setBounds(x, y, buttonWidth, buttonHeight);
                x = padding;
                y += dy;
                btnFilter.setBounds(x, y, width - 2 * padding, buttonHeight);
                y += dy;
                btnCloseParen.setBounds(x, y, buttonWidth, buttonHeight);
                x += dx;
                btnAnd.setBounds(x, y, buttonWidth, buttonHeight);
                x += dx;
                btnOr.setBounds(x, y, buttonWidth, buttonHeight);
            }

            @Override
            public IButton getBtnNotBeforeParen() {
                return btnNotBeforeParen;
            }
            @Override
            public IButton getBtnOpenParen() {
                return btnOpenParen;
            }
            @Override
            public IButton getBtnNotAfterParen() {
                return btnNotAfterParen;
            }
            @Override
            public IButton getBtnFilter() {
                return btnFilter;
            }
            @Override
            public IButton getBtnCloseParen() {
                return btnCloseParen;
            }
            @Override
            public IButton getBtnAnd() {
                return btnAnd;
            }
            @Override
            public IButton getBtnOr() {
                return btnOr;
            }
            @Override
            public AdvancedSearch.Filter<T> getFilter() {
                return filter;
            }
            @Override
            public void setFilter(AdvancedSearch.Filter<T> filter0) {
                filter = filter0;
            }
            @Override
            public Class<? super T> getGenericType() {
                return itemManager.getGenericType();
            }
        }
    }
}
