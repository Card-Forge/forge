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
import forge.menu.FTooltip;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;


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
        label.setText("Filters: ");
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

    private class EditScreen extends FScreen {
        private FScrollPane scroller = add(new FScrollPane() {
            @Override
            protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
                float x = PADDING;
                float y = PADDING;
                float w = visibleWidth - 2 * PADDING;
                float fieldHeight = FTextField.getDefaultHeight();
                float dy = fieldHeight + PADDING;

                for (FDisplayObject child : getChildren()) {
                    if (child.isVisible()) {
                        child.setBounds(x, y, w, fieldHeight);
                        y += dy;
                    }
                }

                return new ScrollBounds(visibleWidth, y);
            }
        });

        private EditScreen() {
            super("Advanced Search");
        }

        @Override
        protected void doLayout(float startY, float width, float height) {
            scroller.setBounds(0, startY, width, height - startY);
        }
    }
}
