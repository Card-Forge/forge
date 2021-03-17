package forge.itemmanager.filters;

import com.badlogic.gdx.utils.Align;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.assets.TextRenderer;
import forge.gui.interfaces.IButton;
import forge.item.InventoryItem;
import forge.itemmanager.AdvancedSearch;
import forge.itemmanager.AdvancedSearch.IFilterControl;
import forge.itemmanager.ItemManager;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.menu.FTooltip;
import forge.screens.FScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.Callback;
import forge.util.Localizer;


public class AdvancedSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    private final AdvancedSearch.Model<T> model;

    private FiltersLabel label;
    private EditScreen editScreen;

    public AdvancedSearchFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
        model = new AdvancedSearch.Model<>();
    }

    @Override
    public ItemFilter<T> createCopy() {
        AdvancedSearchFilter<T> copy = new AdvancedSearchFilter<>(itemManager);
        return copy;
    }

    @Override
    protected final Predicate<T> buildPredicate() {
        return model.getPredicate();
    }

    public Predicate<? super T> getPredicate() {
        return model.getPredicate();
    }

    public void edit() {
        if (editScreen == null) {
            editScreen = new EditScreen();
        }
        Forge.openScreen(editScreen);
    }

    @Override
    public boolean isEmpty() {
        return model.isEmpty();
    }

    @Override
    public void reset() {
        model.reset();
        editScreen = null;
    }

    @Override
    protected void buildWidget(Widget widget) {
        label = new FiltersLabel();
        model.setLabel(label);
        widget.add(label);
        widget.setVisible(!isEmpty());
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        label.setSize(width, height);
    }

    private final Runnable onFilterChange = new Runnable() {
        @Override
        public void run() {
            //update expression when edit screen closed or a single filter is changed
            model.updateExpression();
            itemManager.applyNewOrModifiedFilter(AdvancedSearchFilter.this);
        }
    };

    private class FiltersLabel extends FLabel {
        private String toolTipText;

        private FiltersLabel() {
            super(new FLabel.Builder().align(Align.left).parseSymbols(true).font(ListLabelFilter.LABEL_FONT));
        }

        @Override
        public String getToolTipText() {
            return toolTipText;
        }
        @Override
        public void setToolTipText(String s0) {
            toolTipText = s0;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (count == 1) {
                FPopupMenu menu = new FPopupMenu() {
                    @Override
                    protected void buildMenu() {
                        //add a menu item for each filter to allow easily editing just that filter
                        for (final IFilterControl<T> control : model.getControls()) {
                            FMenuItem item = new FMenuItem(control.getFilter().toString(), Forge.hdbuttons ? FSkinImage.HDEDIT : FSkinImage.EDIT, new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    model.editFilterControl(control, onFilterChange);
                                }
                            });
                            item.setTextRenderer(new TextRenderer()); //ensure symbols are displayed
                            addItem(item);
                        }
                        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblEditExpression"), Forge.hdbuttons ? FSkinImage.HDEDIT : FSkinImage.EDIT, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                edit();
                            }
                        }));
                        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblRemoveFilter"), Forge.hdbuttons ? FSkinImage.HDDELETE : FSkinImage.DELETE, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                reset();
                                itemManager.applyNewOrModifiedFilter(AdvancedSearchFilter.this);
                            }
                        }));
                    }
                };
                menu.show(this, x, y);
            }
            else if (count == 2) {
                edit();
            }
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            FTooltip tooltip = new FTooltip(toolTipText);
            tooltip.show(this, x, getHeight());
            return true;
        }
    }

    private class EditScreen extends FScreen {
        private FScrollPane scroller = add(new FScrollPane() {
            @Override
            protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
                float x = 0;
                float y = 0;
                float w = visibleWidth;
                float h = (FTextField.getDefaultHeight() + FList.PADDING) * 3;

                for (FDisplayObject child : getChildren()) {
                    child.setBounds(x, y, w, h);
                    y += h;
                }

                return new ScrollBounds(visibleWidth, y);
            }
        });

        private EditScreen() {
            super(Localizer.getInstance().getMessage("lblAdvancedSearch"));
            Filter filter = new Filter();
            model.addFilterControl(filter);
            scroller.add(filter);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            //automatically edit first filter if search is empty
            if (model.isEmpty()) {
                model.editFilterControl(Iterables.getFirst(model.getControls(), null), onFilterChange);
            }
        }

        @Override
        public void onClose(Callback<Boolean> canCloseCallback) {
            onFilterChange.run();
            super.onClose(canCloseCallback);
        }

        @Override
        protected void doLayout(float startY, float width, float height) {
            scroller.setBounds(0, startY, width, height - startY);
        }

        private void addNewFilter(Filter fromFilter) {
            if (scroller.getChildAt(scroller.getChildCount() - 1) == fromFilter) {
                Filter filter = new Filter();
                model.addFilterControl(filter);
                scroller.add(filter);
                scroller.revalidate();
                scroller.scrollToBottom();
            }
        }

        @SuppressWarnings("unchecked")
        private void removeNextFilter(Filter fromFilter) {
            int index = scroller.indexOf(fromFilter);
            if (index < scroller.getChildCount() - 1) {
                Filter nextFilter = (Filter)scroller.getChildAt(index + 1);
                model.removeFilterControl(nextFilter);
                scroller.remove(nextFilter);
                scroller.revalidate();
            }
        }

        private class Filter extends FContainer implements AdvancedSearch.IFilterControl<T> {
            private final FLabel btnNotBeforeParen, btnOpenParen, btnNotAfterParen;
            private final FLabel btnFilter;
            private final FLabel btnCloseParen, btnAnd, btnOr;
            private AdvancedSearch.Filter<T> filter;

            private Filter() {
                btnNotBeforeParen = add(new FLabel.Builder().align(Align.center).text("NOT").selectable().build());
                btnOpenParen = add(new FLabel.Builder().align(Align.center).text("(").selectable().build());
                btnNotAfterParen = add(new FLabel.Builder().align(Align.center).text("NOT").selectable().build());
                btnFilter = add(new FLabel.ButtonBuilder().parseSymbols(true).build());
                btnCloseParen = add(new FLabel.Builder().align(Align.center).selectable().text(")").build());
                btnAnd = add(new FLabel.Builder().align(Align.center).text("AND").selectable().command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        if (btnAnd.isSelected()) {
                            btnOr.setSelected(false);
                            addNewFilter(Filter.this);
                        }
                        else {
                            removeNextFilter(Filter.this);
                        }
                    }
                }).build());
                btnOr = add(new FLabel.Builder().align(Align.center).text("OR").selectable().command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        if (btnOr.isSelected()) {
                            btnAnd.setSelected(false);
                            addNewFilter(Filter.this);
                        }
                        else {
                            removeNextFilter(Filter.this);
                        }
                    }
                }).build());
            }

            @Override
            protected void doLayout(float width, float height) {
                float padding = FList.PADDING;
                float buttonWidth = (width - padding * 4) / 3;
                float buttonHeight = (height - padding * 3) / 3;

                float x = padding;
                float y = padding;
                float dx = buttonWidth + padding;
                float dy = buttonHeight + padding;

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
