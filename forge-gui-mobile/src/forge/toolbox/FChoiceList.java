package forge.toolbox;

import static forge.card.CardRenderer.MANA_SYMBOL_SIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardFaceSymbols;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.card.CardZoom.ActivateHandler;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.AdvancedSearch.FilterOperator;
import forge.itemmanager.CardManager;
import forge.itemmanager.filters.AdvancedSearchFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.IHasSkinProp;
import forge.screens.match.MatchController;
import forge.screens.match.views.VAvatar;
import forge.screens.match.views.VStack;
import forge.util.TextUtil;
import forge.util.Utils;

public class FChoiceList<T> extends FList<T> implements ActivateHandler {
    public static final FSkinColor ITEM_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    public static final FSkinColor ALT_ITEM_COLOR = ITEM_COLOR.getContrastColor(-20);
    public static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
    public static final FSkinColor BORDER_COLOR = FList.FORE_COLOR;
    public static final float DEFAULT_ITEM_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.75f;

    protected final int minChoices, maxChoices;
    private final CompactModeHandler compactModeHandler = new CompactModeHandler();
    private final List<Integer> selectedIndices = new ArrayList<>();

    public FChoiceList(Iterable<? extends T> items) {
        this(items, null);
    }
    protected FChoiceList(Iterable<? extends T> items, T typeItem) {
        this(items, 0, 1, typeItem);
        if (getCount() > 0) {
            addSelectedIndex(0); //select first item by default
        }
    }
    public FChoiceList(Iterable<? extends T> items, int minChoices0, int maxChoices0) {
        this(items, minChoices0, maxChoices0, null);
    }
    protected FChoiceList(Iterable<? extends T> items, int minChoices0, int maxChoices0, T typeItem) {
        super(items);
        minChoices = minChoices0;
        maxChoices = maxChoices0;

        //determine renderer from first item's type if none passed
        T item = typeItem == null ? getItemAt(0) : typeItem;
        final ItemRenderer renderer;
        if (item instanceof PaperCard) {
            renderer = new PaperCardItemRenderer();
        }
        else if (item instanceof CardView) {
            renderer = new CardItemRenderer();
        }
        else if (item instanceof IHasCardView) {
            renderer = new IHasCardViewItemRenderer();
        }
        else if (item instanceof PlayerView) {
            renderer = new PlayerItemRenderer();
        }
        else if (item instanceof Integer || item == FilterOperator.EQUALS) { //allow numeric operators to be selected horizontally
            renderer = new NumberRenderer();
        }
        else if (item instanceof IHasSkinProp) {
            renderer = new IHasSkinPropRenderer();
        }
        else {
            renderer = new DefaultItemRenderer();
        }
        setListItemRenderer(new ListItemRenderer<T>() {
            private int prevTapIndex = -1;

            @Override
            public float getItemHeight() {
                return renderer.getItemHeight();
            }

            @Override
            public boolean layoutHorizontal() {
                return renderer.layoutHorizontal();
            }

            @Override
            public AdvancedSearchFilter<? extends InventoryItem> getAdvancedSearchFilter(ListChooser<T> listChooser) {
                return renderer.getAdvancedSearchFilter(listChooser);
            }

            @Override
            public boolean tap(Integer index, T value, float x, float y, int count) {
                boolean activate = (count == 2 && index == prevTapIndex);
                if (maxChoices > 1) {
                    if (selectedIndices.contains(index)) {
                        if (!activate) { //retain selected item if double tapped
                            selectedIndices.remove(index);
                            onSelectionChange();
                        }
                    }
                    else if (selectedIndices.size() < maxChoices) {
                        selectedIndices.add(index);
                        Collections.sort(selectedIndices); //ensure selected indices are sorted
                        onSelectionChange();
                    }
                }
                else if (maxChoices > 0) {
                    selectedIndices.clear();
                    selectedIndices.add(index);
                    onSelectionChange();
                }
                if (renderer.tap(index, value, x, y, count)) {
                    activate = false; //don't activate if renderer handles tap
                }
                if (activate) {
                    onItemActivate(index, value);
                }
                prevTapIndex = index;
                return true;
            }

            @Override
            public boolean showMenu(Integer index, T value, FDisplayObject owner, float x, float y) {
                return renderer.longPress(index, value, x, y);
            }

            @Override
            public void drawValue(Graphics g, Integer index, T value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                if (maxChoices > 1) {
                    if (pressed) { //if multi-select mode, draw SEL_COLOR when pressed
                        g.fillRect(SEL_COLOR, x - FList.PADDING, y - FList.PADDING, w + 2 * FList.PADDING, h + 2 * FList.PADDING);
                    }
                    //draw checkbox, with it checked based on whether item is selected
                    float checkBoxSize = h / 2;
                    float padding = checkBoxSize / 2;
                    w -= checkBoxSize + padding;
                    FCheckBox.drawCheckBox(g, selectedIndices.contains(index), x + w, y + padding, checkBoxSize, checkBoxSize);
                    w -= padding;
                }
                renderer.drawValue(g, value, font, foreColor, pressed, x, y, w, h);
            }
        });
        setFont(renderer.getDefaultFont());
    }

    public int getMinChoices() {
        return minChoices;
    }

    public int getMaxChoices() {
        return maxChoices;
    }

    public T getSelectedItem() {
        if (selectedIndices.size() > 0) {
            return getItemAt(selectedIndices.get(0));
        }
        return null;
    }

    public List<T> getSelectedItems() {
        List<T> choices = new ArrayList<>();
        for (int i : selectedIndices) {
            choices.add(getItemAt(i));
        }
        return choices;
    }

    public int getSelectedIndex() {
        if (selectedIndices.size() > 0) {
            return selectedIndices.get(0);
        }
        return -1;
    }

    public Iterable<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    public int getSelectionCount() {
        return selectedIndices.size();
    }

    @Override
    public void clear() {
        super.clear();
        clearSelection();
    }

    public void clearSelection() {
        selectedIndices.clear();
        onSelectionChange();
    }

    //remove any selected indices outside item range
    public void cleanUpSelections() {
        int count = getCount();
        for (int i = 0; i < selectedIndices.size(); i++) {
            if (selectedIndices.get(i) >= count) {
                selectedIndices.remove(i);
                i--;
            }
        }
        if (selectedIndices.isEmpty() && count > 0) {
            selectedIndices.add(count - 1); //select last item if nothing remains selected
        }
        onSelectionChange();
    }

    public void addSelectedIndex(int index) {
        selectedIndices.add(index);
        onSelectionChange();
    }

    public void addSelectedItem(T choice) {
        addSelectedIndex(getIndexOf(choice));
    }

    public void setSelectedIndex(int index) {
        selectedIndices.clear();
        selectedIndices.add(index);
        scrollIntoView(index);
        onSelectionChange();
    }

    public void setSelectedItem(T choice) {
        setSelectedIndex(getIndexOf(choice));
    }

    protected String getChoiceText(T choice) {
        return choice.toString();
    }

    protected void onItemActivate(Integer index, T value) {
    }

    protected void onSelectionChange() {
    }

    protected void onCompactModeChange() {
        revalidate(); //update scroll bounds by default when compact mode changes
    }

    protected boolean allowDefaultItemWrap() {
        return false;
    }

    @Override
    public boolean zoom(float x, float y, float amount) {
        if (compactModeHandler.update(amount)) {
            onCompactModeChange();
            if (selectedIndices.size() > 0) {
                scrollIntoView(selectedIndices.get(0)); //ensure selection remains in view
            }
        }
        return true;
    }

    @Override
    protected void drawBackground(Graphics g) {
        //draw no background
    }

    @Override
    public void drawOverlay(Graphics g) {
        super.drawOverlay(g);
        g.drawRect(1.5f, BORDER_COLOR, 0, 0, getWidth(), getHeight());
    }

    @Override
    protected FSkinColor getItemFillColor(int index) {
        if (maxChoices == 1 && selectedIndices.contains(index)) {
            return SEL_COLOR; //don't show SEL_COLOR if in multi-select mode
        }
        if (index % 2 == 1) {
            return ALT_ITEM_COLOR;
        }
        return ITEM_COLOR;
    }

    @Override
    protected boolean drawLineSeparators() {
        return false;
    }

    protected abstract class ItemRenderer {
        public abstract FSkinFont getDefaultFont();
        public abstract float getItemHeight();
        public abstract boolean tap(Integer index, T value, float x, float y, int count);
        public abstract boolean longPress(Integer index, T value, float x, float y);
        public abstract void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h);

        public boolean layoutHorizontal() {
            return false; //this doesn't need to be overridden to specify vertical layouts
        }

        public AdvancedSearchFilter<? extends InventoryItem> getAdvancedSearchFilter(ListChooser<T> listChooser) {
            return null; //allow overriding to support advanced search
        }
    }
    protected class DefaultItemRenderer extends ItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(12);
        }

        @Override
        public float getItemHeight() {
            if (allowDefaultItemWrap()) {
                return DEFAULT_ITEM_HEIGHT * 1.5f; //provide more height for wrapping
            }
            return DEFAULT_ITEM_HEIGHT;
        }

        @Override
        public boolean tap(Integer index, T value, float x, float y, int count) {
            return false;
        }

        @Override
        public boolean longPress(Integer index, T value, float x, float y) {
            return false;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            //update manacost text to draw symbols instead
            if (value.toString().contains(" {")){
                String[] values = value.toString().split(" ");
                String cost = TextUtil.fastReplace(values[1],"}{", " ");
                cost = TextUtil.fastReplace(TextUtil.fastReplace(cost,"{", ""),"}", "");
                ManaCost manaCost = new ManaCost(new ManaCostParser(cost));
                CardFaceSymbols.drawManaCost(g, manaCost, x + font.getBounds(values[0]+" ").width, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);
                g.drawText(values[0], font, foreColor, x, y, w, h, allowDefaultItemWrap(), Align.left, true);
            } else {
                g.drawText(getChoiceText(value), font, foreColor, x, y, w, h, allowDefaultItemWrap(), Align.left, true);
            }
        }
    }
    protected class NumberRenderer extends DefaultItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(14);
        }

        @Override
        public float getItemHeight() { //this is actually item width since laying out horizontally
            float width = Utils.AVG_FINGER_WIDTH;
            int itemCount = getCount();
            float totalWidth = width * itemCount;
            if (totalWidth < getWidth()) {
                width = getWidth() / itemCount; //make items wider to take up full width
            }
            return width;
        }

        @Override
        public boolean layoutHorizontal() {
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            g.drawText(getChoiceText(value), font, foreColor, x, y, w, h, false, Align.center, true);
        }
    }
    //simple check for cardview needed on some special renderer for cards
    private boolean showAlternate(CardView cardView, String value){
        if(cardView == null)
            return false;
        boolean showAlt = false;
        if(cardView.hasAlternateState()){
            if(cardView.hasBackSide())
                showAlt = value.contains(cardView.getBackSideName()) || cardView.getAlternateState().getAbilityText().contains(value);
            else if (cardView.isAdventureCard())
                showAlt = value.equals(cardView.getAlternateState().getAbilityText());
            else if (cardView.isSplitCard()) {
                //special case if aftermath cards can be cast from graveyard like yawgmoths will, you will have choices
                if (cardView.getAlternateState().getOracleText().contains("Aftermath"))
                    showAlt = cardView.getAlternateState().getOracleText().contains(value);
                else
                    showAlt = value.equals(cardView.getAlternateState().getAbilityText());
            }
        }
        return showAlt;
    }
    //special renderer for cards
    protected class PaperCardItemRenderer extends ItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(14);
        }

        @Override
        public float getItemHeight() {
            return CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode());
        }

        @Override
        public boolean tap(Integer index, T value, float x, float y, int count) {
            return CardRenderer.cardListItemTap(items, index, FChoiceList.this, x, y, count, compactModeHandler.isCompactMode());
        }

        @Override
        public boolean longPress(Integer index, T value, float x, float y) {
            CardZoom.show(items, index, FChoiceList.this);
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            CardRenderer.drawCardListItem(g, font, foreColor, (PaperCard)value, 0, null, x, y, w, h, compactModeHandler.isCompactMode());
        }

        @Override
        public AdvancedSearchFilter<? extends InventoryItem> getAdvancedSearchFilter(final ListChooser<T> listChooser) {
            //must create a fake CardManager in order to utilize advance search filter
            final CardManager manager = new CardManager(true) {
                @Override
                public void applyNewOrModifiedFilter(final ItemFilter<? extends PaperCard> filter) {
                    //handle update the visibility of the advanced search filter
                    boolean empty = filter.isEmpty();
                    ItemFilter<? extends PaperCard>.Widget widget = filter.getWidget();
                    if (widget.isVisible() == empty) {
                        widget.setVisible(!empty);
                        listChooser.revalidate();
                    }
                    listChooser.applyFilters();
                }

                @Override
                protected void addDefaultFilters() {
                    //avoid creating unneeded filters
                }
            };
            return CardManager.createAdvancedSearchFilter(manager);
        }
    }
    //special renderer for cards
    protected class CardItemRenderer extends ItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(14);
        }

        @Override
        public float getItemHeight() {
            return CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode());
        }

        @Override
        public boolean tap(Integer index, T value, float x, float y, int count) {
            return CardRenderer.cardListItemTap(items, index, FChoiceList.this, x, y, count, compactModeHandler.isCompactMode());
        }

        @Override
        public boolean longPress(Integer index, T value, float x, float y) {
            CardZoom.show(items, index, FChoiceList.this);
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            CardRenderer.drawCardListItem(g, font, foreColor, (CardView)value, 0, null, x, y, w, h, compactModeHandler.isCompactMode());
        }
    }
    //special renderer for SpellAbilities
    protected class IHasCardViewItemRenderer extends ItemRenderer {
        private final TextRenderer textRenderer = new TextRenderer(true);

        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(14);
        }

        @Override
        public float getItemHeight() {
            return VStack.CARD_HEIGHT + 2 * FList.PADDING;
        }

        @Override
        public boolean tap(Integer index, T value, float x, float y, int count) {
            if (x <= VStack.CARD_WIDTH + 2 * FList.PADDING) {
                CardView cv = ((IHasCardView)value).getCardView();
                CardZoom.show(cv, showAlternate(cv, value.toString()));
                return true;
            }
            return false;
        }

        @Override
        public boolean longPress(Integer index, T value, float x, float y) {
            CardView cv = ((IHasCardView)value).getCardView();
            CardZoom.show(cv, showAlternate(cv, value.toString()));
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            //should fix NPE ie Thief of Sanity, Gonti... etc
            CardView cv = ((IHasCardView)value).getCardView();
            if (cv != null) {
                CardView render = cv.isFaceDown() && cv.isInZone(EnumSet.of(ZoneType.Exile)) ? cv.getBackup() : cv;
                boolean showAlternate = showAlternate(render, value.toString());
                CardRenderer.drawCardWithOverlays(g, render, x, y, VStack.CARD_WIDTH, VStack.CARD_HEIGHT, CardStackPosition.Top, false, showAlternate, true);
            }

            float dx = VStack.CARD_WIDTH + FList.PADDING;
            x += dx;
            w -= dx;
            textRenderer.drawText(g, value.toString(), font, foreColor, x, y, w, h, y, h, true, Align.left, true);
        }
    }
    protected class PlayerItemRenderer extends ItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(18);
        }

        @Override
        public float getItemHeight() {
            return VAvatar.HEIGHT;
        }

        @Override
        public boolean tap(Integer index, T value, float x, float y, int count) {
            return false;
        }

        @Override
        public boolean longPress(Integer index, T value, float x, float y) {
            return false;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            PlayerView player = (PlayerView)value;
            g.drawImage(MatchController.getPlayerAvatar(player), x - FList.PADDING, y - FList.PADDING, VAvatar.WIDTH, VAvatar.HEIGHT);
            x += VAvatar.WIDTH;
            w -= VAvatar.WIDTH;
            g.drawText(player.getName() + " (" + player.getLife() + ")", font, foreColor, x, y, w, h, false, Align.left, true);
        }
    }
    protected class IHasSkinPropRenderer extends DefaultItemRenderer {
        private final TextRenderer textRenderer = new TextRenderer(true);

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            FSkinProp skinProp = ((IHasSkinProp)value).getSkinProp();
            if (skinProp != null) {
                float iconSize = h * 0.8f;
                float offset = (h - iconSize) / 2;

                g.drawImage(FSkin.getImages().get(skinProp), x + offset - 1, y + offset, iconSize, iconSize);

                float dx = iconSize + PADDING + 2 * offset - 1;
                x += dx;
                w -= dx;
            }
            textRenderer.drawText(g, value.toString(), font, foreColor, x, y, w, h, y, h, true, Align.left, true);
        }
    }

    @Override
    public String getActivateAction(int index) {
        if (maxChoices > 0) {
            return "select card";
        }
        return null;
    }

    @Override
    public void activate(int index) {
        setSelectedIndex(index);
    }
}
