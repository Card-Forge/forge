package forge.itemmanager.filters;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.FThreads;
import forge.Forge;
import forge.item.InventoryItem;
import forge.itemmanager.BooleanExpression.Operator;
import forge.itemmanager.AdvancedSearch;
import forge.itemmanager.ItemManager;
import forge.menu.FTooltip;
import forge.screens.FScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;


public class AdvancedSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    private final List<Object> expression = new ArrayList<Object>();

    private FiltersLabel label;
    private boolean isAdded;
    private EditScreen editScreen;

    public AdvancedSearchFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<T> createCopy() {
        AdvancedSearchFilter<T> copy = new AdvancedSearchFilter<T>(itemManager);
        return copy;
    }

    @Override
    protected final Predicate<T> buildPredicate() {
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
    private Predicate<T> getPredicatePiece(ExpressionIterator iterator) {
        Predicate<T> pred = null;
        Predicate<T> predPiece = null;
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
                predPiece = ((ItemFilter<T>) piece).buildPredicate();
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
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        label.setSize(width, height);
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

        private void removeNextFilter(Filter fromFilter) {
            int index = scroller.indexOf(fromFilter);
            if (index < scroller.getChildCount() - 1) {
                Filter nextFilter = (Filter)scroller.getChildAt(index + 1);
                fromFilter.btnAnd.setSelected(nextFilter.btnAnd.isSelected());
                fromFilter.btnOr.setSelected(nextFilter.btnOr.isSelected());
                scroller.remove(nextFilter);
                scroller.revalidate();
            }
        }

        private class Filter extends FContainer {
            private final FLabel btnNotBeforeParen, btnOpenParen, btnNotAfterParen;
            private final FLabel btnFilter;
            private final FLabel btnCloseParen, btnAnd, btnOr;
            private AdvancedSearch.Filter<T> filter;

            private Filter() {
                btnNotBeforeParen = add(new FLabel.Builder().align(HAlignment.CENTER).text("NOT").selectable().build());
                btnOpenParen = add(new FLabel.Builder().align(HAlignment.CENTER).text("(").selectable().build());
                btnNotAfterParen = add(new FLabel.Builder().align(HAlignment.CENTER).text("NOT").selectable().build());

                final String emptyFilterText = "Select Filter...";
                btnFilter = add(new FLabel.ButtonBuilder().text(emptyFilterText).command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        FThreads.invokeInBackgroundThread(new Runnable() {
                            @Override
                            public void run() {
                                final AdvancedSearch.Filter<T> newFilter = AdvancedSearch.getFilter(itemManager.getGenericType());
                                if (newFilter != null) {
                                    FThreads.invokeInEdtLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            filter = newFilter;
                                            btnFilter.setText(filter.toString());
                                        }
                                    });
                                }
                            }
                        });
                    }
                }).build());

                btnCloseParen = add(new FLabel.Builder().align(HAlignment.CENTER).selectable().text(")").build());
                btnAnd = add(new FLabel.Builder().align(HAlignment.CENTER).text("AND").selectable().command(new FEventHandler() {
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
                btnOr = add(new FLabel.Builder().align(HAlignment.CENTER).text("OR").selectable().command(new FEventHandler() {
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
        }
    }
}
