package forge.toolbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.assets.FSkinColor.Colors;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;
import forge.screens.match.MatchController;
import forge.screens.match.views.VAvatar;
import forge.screens.match.views.VStack;
import forge.util.Callback;
import forge.util.Utils;

public class FChoiceList<T> extends FList<T> {
    public static final FSkinColor ITEM_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    public static final FSkinColor ALT_ITEM_COLOR = ITEM_COLOR.getContrastColor(-20);
    public static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
    public static final FSkinColor BORDER_COLOR = FList.FORE_COLOR;
    public static final float DEFAULT_ITEM_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.75f;

    protected final int minChoices, maxChoices;
    private final CompactModeHandler compactModeHandler = new CompactModeHandler();
    private final List<Integer> selectedIndices = new ArrayList<Integer>();

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
        else if (item instanceof SpellAbility) {
            renderer = new SpellAbilityItemRenderer();
        }
        else if (item instanceof PlayerView) {
            renderer = new PlayerItemRenderer();
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
            public boolean tap(Integer index, T value, float x, float y, int count) {
                if (maxChoices > 1) {
                    if (selectedIndices.contains(index)) {
                        selectedIndices.remove(index);
                        onSelectionChange();
                    }
                    else if (selectedIndices.size() < maxChoices) {
                        selectedIndices.add(index);
                        Collections.sort(selectedIndices); //ensure selected indices are sorted
                        onSelectionChange();
                    }
                }
                else if (maxChoices > 0 && !selectedIndices.contains(index)) {
                    selectedIndices.clear();
                    selectedIndices.add(index);
                    onSelectionChange();
                }
                if (renderer.tap(index, value, x, y, count)) {
                    prevTapIndex = index;
                    return true; //don't activate if renderer handles tap
                }
                if (count == 2 && index == prevTapIndex) {
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
        List<T> choices = new ArrayList<T>();
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
            g.drawText(getChoiceText(value), font, foreColor, x, y, w, h, allowDefaultItemWrap(), HAlignment.LEFT, true);
        }
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
            return CardRenderer.cardListItemTap(items, index, new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    setSelectedIndex(result);
                }
            }, x, y, count, compactModeHandler.isCompactMode());
        }

        @Override
        public boolean longPress(Integer index, T value, float x, float y) {
            CardZoom.show(items, index, new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    setSelectedIndex(result);
                }
            });
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            CardRenderer.drawCardListItem(g, font, foreColor, (PaperCard)value, 0, null, x, y, w, h, compactModeHandler.isCompactMode());
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
            return CardRenderer.cardListItemTap(items, index, new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    setSelectedIndex(result);
                }
            }, x, y, count, compactModeHandler.isCompactMode());
        }

        @Override
        public boolean longPress(Integer index, T value, float x, float y) {
            CardZoom.show(items, index, new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    setSelectedIndex(result);
                }
            });
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            CardRenderer.drawCardListItem(g, font, foreColor, (CardView)value, 0, null, x, y, w, h, compactModeHandler.isCompactMode());
        }
    }
    //special renderer for SpellAbilities
    protected class SpellAbilityItemRenderer extends ItemRenderer {
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
                CardZoom.show(((SpellAbility)value).getView().getHostCard());
                return true;
            }
            return false;
        }

        @Override
        public boolean longPress(Integer index, T value, float x, float y) {
            CardZoom.show(((SpellAbility)value).getView().getHostCard());
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            SpellAbility spellAbility = (SpellAbility)value;
            CardRenderer.drawCardWithOverlays(g, spellAbility.getView().getHostCard(), x, y, VStack.CARD_WIDTH, VStack.CARD_HEIGHT, CardStackPosition.Top);

            float dx = VStack.CARD_WIDTH + FList.PADDING;
            x += dx;
            w -= dx;
            textRenderer.drawText(g, spellAbility.toString(), font, foreColor, x, y, w, h, y, h, true, HAlignment.LEFT, true);
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
            g.drawText(player.getName() + " (" + player.getLife() + ")", font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);
        }
    }
}
