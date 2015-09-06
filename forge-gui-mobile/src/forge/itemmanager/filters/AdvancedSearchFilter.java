package forge.itemmanager.filters;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.FThreads;
import forge.Forge;
import forge.item.InventoryItem;
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
import forge.util.Callback;


public class AdvancedSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    private final List<Object> expression = new ArrayList<Object>();

    private FiltersLabel label;
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
        return getPredicate();
    }

    public Predicate<T> getPredicate() {
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
                predPiece = ((AdvancedSearch.Filter<T>) piece).getPredicate();
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
    }

    @SuppressWarnings("unchecked")
    private void updateLabel() {
        StringBuilder builder = new StringBuilder();
        builder.append("Filter: ");
        if (expression.isEmpty()) {
            builder.append("(none)");
        }
        else {
            int prevFilterEndIdx = -1;
            AdvancedSearch.Filter<T> filter, prevFilter = null;
            for (Object piece : expression) {
                if (piece instanceof AdvancedSearch.Filter) {
                    filter = (AdvancedSearch.Filter<T>)piece;
                    if (filter.canMergeCaptionWith(prevFilter)) {
                        //convert boolean operators between filters to lowercase
                        builder.replace(prevFilterEndIdx, builder.length(), builder.substring(prevFilterEndIdx).toLowerCase());
                        //append only values for filter
                        builder.append(filter.extractValuesFromCaption());
                    }
                    else {
                        builder.append(filter);
                    }
                    prevFilter = filter;
                    prevFilterEndIdx = builder.length();
                }
                else {
                    if (piece.equals(Operator.OPEN_PAREN) || piece.equals(Operator.CLOSE_PAREN)) {
                        prevFilter = null; //prevent merging filters with parentheses in between
                    }
                    builder.append(piece);
                }
            }
        }
        label.setText(builder.toString());
    }

    private String getTooltip() {
        if (expression.isEmpty()) { return ""; }

        StringBuilder builder = new StringBuilder();
        builder.append("Filter:\n");

        String indent = "";

        for (Object piece : expression) {
            if (piece.equals(Operator.CLOSE_PAREN) && !indent.isEmpty()) {
                indent = indent.substring(2); //trim an indent level when a close paren is hit
            }
            builder.append("\n" + indent + piece.toString().trim());
            if (piece.equals(Operator.OPEN_PAREN)) {
                indent += "  "; //add an indent level when an open paren is hit
            }
        }
        return builder.toString();
    }

    @Override
    public boolean isEmpty() {
        return expression.isEmpty();
    }

    @Override
    public void reset() {
        expression.clear();
        editScreen = null;
        if (label != null) {
            updateLabel();
        }
    }

    @Override
    protected void buildWidget(Widget widget) {
        label = new FiltersLabel();
        updateLabel();
        widget.add(label);
        widget.setVisible(!isEmpty());
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        label.setSize(width, height);
    }

    private class FiltersLabel extends FLabel {
        private FiltersLabel() {
            super(new FLabel.Builder().align(HAlignment.LEFT).parseSymbols(true).font(ListLabelFilter.LABEL_FONT));
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
        @SuppressWarnings("unchecked")
        public void onClose(Callback<Boolean> canCloseCallback) {
            //build expression when closing screen
            expression.clear();

            for (FDisplayObject child : scroller.getChildren()) {
                Filter filter = (Filter)child;
                if (filter.filter == null) { continue; } //skip any blank filters

                if (filter.btnNotBeforeParen.isSelected()) {
                    expression.add(Operator.NOT);
                }
                if (filter.btnOpenParen.isSelected()) {
                    expression.add(Operator.OPEN_PAREN);
                }
                if (filter.btnNotAfterParen.isSelected()) {
                    expression.add(Operator.NOT);
                }

                expression.add(filter.filter);

                if (filter.btnCloseParen.isSelected()) {
                    expression.add(Operator.CLOSE_PAREN);
                }
                if (filter.btnAnd.isSelected()) {
                    expression.add(Operator.AND);
                }
                else if (filter.btnOr.isSelected()) {
                    expression.add(Operator.OR);
                }
            }

            if (label != null) {
                updateLabel();
            }

            itemManager.applyNewOrModifiedFilter(AdvancedSearchFilter.this);

            super.onClose(canCloseCallback);
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

        @SuppressWarnings("unchecked")
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
                btnFilter = add(new FLabel.ButtonBuilder().text(emptyFilterText).parseSymbols(true).command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        FThreads.invokeInBackgroundThread(new Runnable() {
                            @Override
                            public void run() {
                                final AdvancedSearch.Filter<T> newFilter = AdvancedSearch.getFilter(itemManager.getGenericType(), filter);
                                if (filter != newFilter) {
                                    FThreads.invokeInEdtLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            filter = newFilter;
                                            if (filter != null) {
                                                btnFilter.setText(filter.toString());
                                            }
                                            else {
                                                btnFilter.setText(emptyFilterText);
                                            }
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

    private enum Operator {
        AND(" AND "),
        OR(" OR "),
        NOT("NOT "),
        OPEN_PAREN("("),
        CLOSE_PAREN(")");

        private final String token;

        private Operator(String token0) {
            token = token0;
        }

        public String toString() {
            return token;
        }
    }
}
