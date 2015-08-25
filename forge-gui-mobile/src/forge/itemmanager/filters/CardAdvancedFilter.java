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
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FList;
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

    private class EditScreen extends FScreen {
        private FScrollPane scroller = add(new FScrollPane() {
            @Override
            protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
                float x = 0;
                float y = FList.PADDING;
                float w = visibleWidth;
                float h = FTextField.getDefaultHeight();
                float dy = h + FList.PADDING;

                for (FDisplayObject child : getChildren()) {
                    child.setBounds(x, y, w, h);
                    y += dy;
                }

                return new ScrollBounds(visibleWidth, y);
            }
        });

        private EditScreen() {
            super("Advanced Search");

            scroller.add(new OptionRow(false));
        }

        @Override
        protected void doLayout(float startY, float width, float height) {
            scroller.setBounds(0, startY, width, height - startY);
        }
        
        private void addFilter(ItemFilter<PaperCard> filter, OptionRow belowRow) {
            if (scroller.getChildAt(scroller.getChildCount() - 1) == belowRow) {
                scroller.add(new OptionRow(filter));
                scroller.add(new OptionRow(true));
                scroller.revalidate();
                scroller.scrollToBottom();
            }
            else { //support swapping out one filter for another
                OptionRow filterRow = (OptionRow)scroller.getChildAt(scroller.indexOf(belowRow) + 1);
                filterRow.filter = filter;
                filterRow.clear();
                filterRow.add(filter.getWidget());
                filterRow.revalidate();
            }
        }

        private void addFilterSelectRow(OptionRow belowRow) {
            if (scroller.getChildAt(scroller.getChildCount() - 1) == belowRow) {
                scroller.add(new OptionRow(false));
                scroller.revalidate();
                scroller.scrollToBottom();
            }
        }

        private class OptionRow extends FContainer {
            private FLabel btnOpenParen, btnCloseParen, btnAnd, btnOr, btnNot;
            private ItemFilter<PaperCard> filter;

            private OptionRow(ItemFilter<PaperCard> filter0) {
                filter = filter0;
                add(filter.getWidget());
            }
            private OptionRow(boolean includeOperators) {
                if (includeOperators) {
                    btnCloseParen = add(new FLabel.Builder().align(HAlignment.CENTER).selectable().text(")").build());
                    btnAnd = add(new FLabel.Builder().align(HAlignment.CENTER).text("AND").selectable().command(new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            btnOr.setSelected(false);
                            addFilterSelectRow(OptionRow.this);
                        }
                    }).build());
                    btnOr = add(new FLabel.Builder().align(HAlignment.CENTER).text("OR").selectable().command(new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            btnAnd.setSelected(false);
                            addFilterSelectRow(OptionRow.this);
                        }
                    }).build());
                }
                else {
                    btnOpenParen = add(new FLabel.Builder().align(HAlignment.CENTER).text("(").selectable().build());
                    btnNot = add(new FLabel.Builder().align(HAlignment.CENTER).text("NOT").selectable().build());
                    add(new FLabel.ButtonBuilder().text("...").command(new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            FPopupMenu menu = new FPopupMenu() {
                                @Override
                                protected void buildMenu() {
                                    addItem(new FMenuItem("CMC", new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            addFilter(new CardCMCFilter(itemManager), OptionRow.this);
                                        }
                                    }));
                                    addItem(new FMenuItem("Colorless Cost", new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            addFilter(new CardColorlessCostFilter(itemManager), OptionRow.this);
                                        }
                                    }));
                                    addItem(new FMenuItem("Power", new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            addFilter(new CardPowerFilter(itemManager), OptionRow.this);
                                        }
                                    }));
                                    addItem(new FMenuItem("Toughness", new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            addFilter(new CardToughnessFilter(itemManager), OptionRow.this);
                                        }
                                    }));
                                    addItem(new FMenuItem("Name", new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            addFilter(new CardSearchFilter(itemManager, true, false, false, false), OptionRow.this);
                                        }
                                    }));
                                    addItem(new FMenuItem("Type", new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            addFilter(new CardSearchFilter(itemManager, false, true, false, false), OptionRow.this);
                                        }
                                    }));
                                    addItem(new FMenuItem("Rules Text", new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            addFilter(new CardSearchFilter(itemManager, false, false, true, false), OptionRow.this);
                                        }
                                    }));
                                    addItem(new FMenuItem("Mana Cost", new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            addFilter(new CardSearchFilter(itemManager, false, false, false, true), OptionRow.this);
                                        }
                                    }));
                                }
                            };
                            FDisplayObject button = e.getSource();
                            menu.show(button, 0, button.getHeight());
                        }
                    }).build());
                }
            }

            @Override
            protected void doLayout(float width, float height) {
                float padding = FList.PADDING;
                float buttonCount = getChildCount();
                float buttonWidth = (width - padding * (buttonCount + 1)) / buttonCount;

                float x = padding;
                for (FDisplayObject button : getChildren()) {
                    button.setBounds(x, 0, buttonWidth, height);
                    x += buttonWidth + padding;
                }
            }
        }
    }
}
