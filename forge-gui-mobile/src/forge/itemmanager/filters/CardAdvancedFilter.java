package forge.itemmanager.filters;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.item.PaperCard;
import forge.itemmanager.BooleanExpression.Operator;
import forge.itemmanager.ItemManager;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.menu.FTooltip;
import forge.screens.FScreen;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.ComparableOp;


public class CardAdvancedFilter extends ItemFilter<PaperCard> {
    private final List<Object> expression = new ArrayList<Object>();

    private FiltersLabel label;
    private FLabel btnEdit;
    private boolean isAdded;
    private EditScreen editScreen;

    public CardAdvancedFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardAdvancedFilter copy = new CardAdvancedFilter(itemManager);
        return copy;
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        if (expression.isEmpty()) {
            return Predicates.alwaysTrue();
        }
        return getPredicatePiece(new ExpressionIterator());
    }
    
    private class ExpressionIterator {
        private int index;
        private boolean hasNext() {
            return index < expression.size();
        }
        private ExpressionIterator next() {
            index++;
            return this;
        }
        private Object get() {
            return expression.get(index);
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate<PaperCard> getPredicatePiece(ExpressionIterator iterator) {
        Predicate<PaperCard> pred = null;
        Predicate<PaperCard> predPiece = null;
        Operator operator = null;
        boolean applyNot = false;

        for (; iterator.hasNext(); iterator.next()) {
            Object piece = iterator.get();
            if (piece.equals(Operator.OPEN_PAREN)) {
                predPiece = getPredicatePiece(iterator.next());
            }
            else if (piece.equals(Operator.CLOSE_PAREN)) {
                return pred;
            }
            else if (piece.equals(Operator.AND)) {
                operator = Operator.AND;
                continue;
            }
            else if (piece.equals(Operator.OR)) {
                operator = Operator.OR;
                continue;
            }
            else if (piece.equals(Operator.NOT)) {
                applyNot = !applyNot;
                continue;
            }
            else {
                predPiece = ((ItemFilter<PaperCard>) piece).buildPredicate();
            }
            if (applyNot) {
                predPiece = Predicates.not(predPiece);
                applyNot = false;
            }
            if (pred == null) {
                pred = predPiece;
            }
            else if (operator == Operator.AND) {
                pred = Predicates.and(pred, predPiece);
            }
            else if (operator == Operator.OR) {
                pred = Predicates.or(pred, predPiece);
            }
            operator = null;
        }
        return pred;
    }

    public void edit() {
        if (editScreen == null) {
            editScreen = new EditScreen();
        }
        Forge.openScreen(editScreen);
        /*if (!isAdded) {
            isAdded = true;
            itemManager.addFilter(0, this);
        }
        else {
            itemManager.removeFilter(this);
            isAdded = false;
        }*/
    }

    private void updateLabel() {
        StringBuilder builder = new StringBuilder();
        builder.append("Filters: ");
        if (expression.isEmpty()) {
            builder.append("(none)");
        }
        else {
            builder.append(expression.get(0));
            for (int i = 1; i < expression.size(); i++) {
                builder.append(", " + expression.get(i));
            }
        }
        label.setText(builder.toString());
    }

    private String getTooltip() {
        if (expression.isEmpty()) { return ""; }

        StringBuilder builder = new StringBuilder();
        builder.append(expression.get(0));

        for (int i = 1; i < expression.size(); i++) {
            builder.append("\n" + expression.get(i));
        }
        return builder.toString();
    }

    @Override
    public boolean isEmpty() {
        return expression.isEmpty();
    }

    @Override
    public void reset() {
    }

    @Override
    protected void buildWidget(Widget widget) {
        label = new FiltersLabel();
        updateLabel();
        widget.add(label);

        btnEdit = new FLabel.ButtonBuilder().icon(FSkinImage.SEARCH).iconScaleFactor(0.9f).command(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                edit();
            }
        }).build();
        widget.add(btnEdit);
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        float buttonWidth = height;
        float buttonHeight = height;
        btnEdit.setBounds(width - buttonWidth, (height - buttonHeight) / 2, buttonWidth, buttonHeight);
        label.setSize(btnEdit.getLeft() - ItemFilter.PADDING, height);
    }

    private class FiltersLabel extends FLabel {
        private FiltersLabel() {
            super(new FLabel.Builder().align(HAlignment.LEFT).font(ListLabelFilter.LABEL_FONT));
        }

        @Override
        public boolean tap(float x, float y, int count) {
            FTooltip tooltip = new FTooltip(getTooltip());
            tooltip.show(this, x, getHeight());
            return true;
        }
    }

    private static class EditScreen extends FScreen {
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
            super("Advanced Search");
            scroller.add(new Filter());
        }

        @Override
        protected void doLayout(float startY, float width, float height) {
            scroller.setBounds(0, startY, width, height - startY);
        }

        private void addNewFilter(Filter fromFilter) {
            if (scroller.getChildAt(scroller.getChildCount() - 1) == fromFilter) {
                scroller.add(new Filter());
                scroller.revalidate();
                scroller.scrollToBottom();
            }
        }

        private enum FilterOption {
            NONE("Filter...", null, null, -1, -1),
            CMC("CMC", ComparableOp.NUMBER_OPS, ComparableOp.EQUALS, 0, 20),
            COLORLESS_COST("Colorless Cost", ComparableOp.NUMBER_OPS, ComparableOp.EQUALS, 0, 20),
            POWER("Power", ComparableOp.NUMBER_OPS, ComparableOp.EQUALS, 0, 20),
            TOUGHNESS("Toughness", ComparableOp.NUMBER_OPS, ComparableOp.EQUALS, 0, 20),
            NAME("Name", ComparableOp.STRING_OPS, ComparableOp.CONTAINS, -1, -1),
            TYPE("Type", ComparableOp.STRING_OPS, ComparableOp.CONTAINS, -1, -1),
            RULES_TEXT("Rules Text", ComparableOp.STRING_OPS, ComparableOp.CONTAINS, -1, -1),
            MANA_COST("Mana Cost", ComparableOp.STRING_OPS, ComparableOp.EQUALS, -1, -1);

            private final String name;
            private final ComparableOp[] availableOps;
            private final ComparableOp defaultOp;
            private final int min, max;

            private FilterOption(String name0, ComparableOp[] availableOps0, ComparableOp defaultOp0, int min0, int max0) {
                name = name0;
                availableOps = availableOps0;
                defaultOp = defaultOp0;
                min = min0;
                max = max0;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        private class Filter extends FContainer {
            private final FLabel btnNotBeforeParen, btnOpenParen, btnNotAfterParen;
            private final FComboBox<FilterOption> cbFilter;
            private final FComboBox<ComparableOp> cbFilterOperator;
            private final FTextField txtFilterValue;
            private final FLabel btnCloseParen, btnAnd, btnOr;

            private Filter() {
                btnNotBeforeParen = add(new FLabel.Builder().align(HAlignment.CENTER).text("NOT").selectable().build());
                btnOpenParen = add(new FLabel.Builder().align(HAlignment.CENTER).text("(").selectable().build());
                btnNotAfterParen = add(new FLabel.Builder().align(HAlignment.CENTER).text("NOT").selectable().build());

                cbFilter = add(new FComboBox<FilterOption>(FilterOption.values()));
                cbFilter.setChangedHandler(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        FilterOption filterOption = cbFilter.getSelectedItem();
                        cbFilterOperator.setItems(filterOption.availableOps, filterOption.defaultOp);
                        txtFilterValue.setText(filterOption.min == -1 ? "" : String.valueOf(filterOption.min));
                    }
                });
                cbFilterOperator = add(new FComboBox<ComparableOp>());
                txtFilterValue = add(new FTextField());

                btnCloseParen = add(new FLabel.Builder().align(HAlignment.CENTER).selectable().text(")").build());
                btnAnd = add(new FLabel.Builder().align(HAlignment.CENTER).text("AND").selectable().command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        btnOr.setSelected(false);
                        addNewFilter(Filter.this);
                    }
                }).build());
                btnOr = add(new FLabel.Builder().align(HAlignment.CENTER).text("OR").selectable().command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        btnAnd.setSelected(false);
                        addNewFilter(Filter.this);
                    }
                }).build());
            }

            @Override
            protected void doLayout(float width, float height) {
                float padding = FList.PADDING;
                float controlWidth = (width - padding * 4) / 3;
                float controlHeight = (height - padding * 3) / 3;

                float x = padding;
                float y = padding;
                for (FDisplayObject obj : getChildren()) {
                    obj.setBounds(x, y, controlWidth, controlHeight);
                    x += controlWidth + padding;
                    if (x > width - controlWidth) {
                        x = padding;
                        y += controlHeight + padding;
                    }
                }
            }
        }
    }
}
