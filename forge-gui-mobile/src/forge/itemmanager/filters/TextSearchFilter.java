package forge.itemmanager.filters;

import com.badlogic.gdx.utils.Align;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.Graphics;
import forge.assets.FSkinFont;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FTextField;
import forge.util.Localizer;


public class TextSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    private static final FSkinFont FONT = FSkinFont.get(12);
    protected SearchField txtSearch;

    public TextSearchFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<T> createCopy() {
        TextSearchFilter<T> copy = new TextSearchFilter<>(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        return copy;
    }

    @Override
    public boolean isEmpty() {
        return txtSearch.isEmpty();
    }

    @Override
    public void reset() {
        txtSearch.setText("");
    }

    @Override
    public FDisplayObject getMainComponent() {
        return txtSearch;
    }

    @Override
    protected void buildWidget(Widget widget) {
        txtSearch = new SearchField();
        widget.add(txtSearch);

        txtSearch.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                applyChange();
            }
        });
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        txtSearch.setSize(width, height);
    }

    @Override
    protected Predicate<T> buildPredicate() {
        String text = txtSearch.getText();
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }
        return SFilterUtil.buildItemTextFilter(text);
    }

    public void setRatio(String ratio0) {
        txtSearch.ratio = ratio0;
    }

    public String getCaption() {
        return txtSearch.getGhostText().substring((Localizer.getInstance().getMessage("lblSearch") + " ").length());
    }
    public void setCaption(String caption0) {
        txtSearch.setGhostText(Localizer.getInstance().getMessage("lblSearch") + " " + caption0);
    }

    protected class SearchField extends FTextField {
        private String ratio = "(0 / 0)";

        private SearchField() {
            setFont(FONT);
            setGhostText(Localizer.getInstance().getMessage("lblSearch"));
            setHeight(getDefaultHeight(DEFAULT_FONT)); //set height based on default filter font
        }

        @Override
        protected float getRightPadding() {
            return renderedFont.getBounds(ratio).width + 2 * PADDING;
        }

        @Override
        public void draw(Graphics g) {
            super.draw(g);
            g.drawText(ratio, renderedFont, GHOST_TEXT_COLOR, 0, 0, getWidth() - PADDING, getHeight(), false, Align.right, true);
        }
    }
}
